## Context

`ToolRegistry.java` (4893 lines, `j-lawyer-client`) defines and executes 62 AI tools. It runs
*inside the Swing client*: it obtains `*Remote` EJB stubs through `JLawyerServiceLocator`, so
the container identity is whatever the desktop user logged in as, and the tool-approval UI is
a Swing dialog. That design is correct for Ingo and useless for anything else -- no external
AI product can reach it.

This change adds a second, independent consumer of the same business operations: an MCP
endpoint inside `j-lawyer-server`, reachable over HTTPS by third-party AI hosts.

Three facts constrain everything below.

**Fact 1 -- identity must be established by the container.** The EJB tier does not accept an
"acting user" parameter. `ArchiveFileService` alone carries ~180 `@RolesAllowed` annotations,
and `context.getCallerPrincipal().getName()` drives case visibility
(`SecurityUtils.getAllowedCasesForUser`), group checks (`SecurityUtils.checkGroupsForCase`),
document locking, `setCreatedBy`, and `archiveFileService.addCaseHistory(...)`. The
`add-web-client` change already litigated this and revised its own Decision 5 accordingly: a
purely application-level JAX-RS `SecurityContext` does **not** propagate to EJBs, so
`getCallerPrincipal()` would return `anonymous` and both authorization and audit would break.
The MCP server must reuse the Elytron path, not invent a second one.

**Fact 2 -- the target hosts speak the legacy protocol era.** MCP revision `2026-07-28` removed
protocol-level sessions and the GET stream in favour of per-request `_meta`. Claude.ai and
Claude Cowork custom connectors, as of this writing, speak the `initialize`-handshake era
(auth spec `2025-03-26` / `2025-06-18`) over Streamable HTTP or the deprecated HTTP+SSE
transport. A server that implements only the modern revision is, per the MCP compatibility
matrix, a **"Legacy client / Modern server → Fails"** cell. Dual-era is a hard requirement of
"works with Claude Cowork", not gold-plating.

**Fact 3 -- claude.ai connectors have no static-token field.** The connector UI performs an
OAuth flow and exposes only optional client ID / client secret. There is no header or bearer
field. OAuth 2.1 is therefore mandatory for the primary use case; personal access tokens
serve Claude Code, n8n and scripts.

## Goals / Non-Goals

**Goals**

- Functional parity with the 60 `ToolRegistry` tools that operate on j-lawyer data, over
  MCP, server-side.
- Work out of the box with Claude Cowork / claude.ai custom connectors, Claude Code, Claude
  Desktop, and generic MCP hosts (n8n, Open WebUI, MCP Inspector).
- Every operation runs with the real user's j-lawyer identity, permissions and group
  memberships; write operations name that user in the case history.
- Safe to expose on the public internet.

**Non-Goals**

- Reusing, refactoring or extracting `ToolRegistry`. The client keeps its copy; duplication
  is accepted deliberately (Decision 3).
- MCP `resources` and `prompts` capabilities. Out of scope; this change builds the transport
  and authorization foundation they would sit on.
- Exposing `web_search` and `fetch_url` over MCP (Decision 3a). They stay in the client.
- Replacing HTTP Basic auth on `/j-lawyer-io`, the EJB remoting path, or the Ingo in-client
  tool-calling loop. All remain untouched.
- Acting as a general-purpose OAuth provider for other j-lawyer surfaces. The authorization
  server built here is scoped to the MCP resource.

## Decisions

### Decision 1 -- Separate WAR `j-lawyer-mcp` inside the existing EAR

The MCP endpoint ships as its own web module with context root `/j-lawyer-mcp`, packaged into
`j-lawyer-server.ear` next to `j-lawyer-io`.

- Its own `web.xml` can leave `/mcp` behind a bearer-only constraint while `/.well-known/*`
  and `/oauth/*` are public -- mirroring how `add-web-client` exempted `/rest/v8/auth/*`.
  Sharing `j-lawyer-io`'s single `<security-constraint>` would force awkward exemptions into
  the REST war.
- MCP versioning (`YYYY-MM-DD` protocol revisions) and REST API versioning (`v1..v8`) are
  independent axes. Coupling them means every MCP revision bump looks like a REST API change.
- It can be undeployed or disabled without touching the REST API -- valuable for a
  public-facing surface that a firm may want off.
- Same-EAR deployment keeps `java:global/j-lawyer-server/j-lawyer-server-ejb/...` local
  lookups available, so tools call `*Local` interfaces with no remoting overhead.

*Rejected:* endpoints under `/j-lawyer-io/rest/v8/mcp`. Cheaper to build, but couples the two
version axes, shares one security constraint, and would place `/.well-known/oauth-*` inside
the REST war where the SPA also lives.

### Decision 1a -- The reverse proxy is part of the design, not an afterthought

j-lawyer installations run WildFly behind nginx, and **WildFly never terminates TLS** -- that
is always the proxy's job. The MCP server is specified for that topology rather than for a
directly exposed container, because three of its requirements break silently otherwise.

**Effective scheme, not connector scheme.** OAuth 2.1 requires HTTPS for authorization flows.
Behind a terminating proxy the container sees plaintext HTTP on every request, so a check
written against the connector (`SecurityContext.isSecure()`) reports `false` always and would
refuse every token -- the MCP server would simply not work. The check must therefore run
against the *forwarded* scheme, which means `proxy-address-forwarding="true"` on the
`http-listener`. The committed `docker/wildfly/standalone.xml:586` does not set it today.

That trust has a precondition: forwarded headers are supplied by whoever connects, so a
directly reachable plaintext listener lets a client assert `X-Forwarded-Proto: https` itself.
The listener must not be reachable other than through the proxy. This is a network-level
guarantee, not something the application can verify, so it is stated in the administration
interface and probed by the activation self-test rather than assumed.

*Note for a separate change:* `AuthenticationEndpointV8.java:196` decides the `Secure` flag of
the web client's refresh cookie from the same `isSecure()`. Behind nginx without
`proxy-address-forwarding`, that cookie is issued **without** `Secure` today. Same root cause,
pre-existing, and not fixed here.

**The metadata document does not live where the module does.** RFC 9728 inserts the well-known
suffix *between host and path*: the metadata for `https://host/j-lawyer-mcp/mcp` belongs at
`https://host/.well-known/oauth-protected-resource/j-lawyer-mcp/mcp`. A WAR at context root
`/j-lawyer-mcp` cannot serve a host-root path. Two mechanisms together resolve this: the
`resource_metadata` parameter of the `WWW-Authenticate` challenge may point anywhere and is
authoritative for conforming MCP clients, and an nginx `location` maps the canonical path to
the module-local one for clients that probe directly. Relying on the header alone would be
brittle.

**SSE needs proxy settings that are not the defaults.** Tool results may be `text/event-stream`,
and the legacy era additionally has a long-lived GET stream. nginx buffers responses and times
reads out after 60s by default; both break MCP. The server emits `X-Accel-Buffering: no`, and
the reference configuration adds `proxy_buffering off`, `proxy_http_version 1.1`,
`proxy_set_header Connection ""` and a raised `proxy_read_timeout`.

All externally visible URLs -- `resource`, `issuer`, endpoint URLs in the discovery documents
-- are built from the administrator-configured public base URL, never from the `Host` header
or the container's view of the scheme. This is why that setting is mandatory at activation.

*Rejected:* terminating TLS in WildFly for the MCP context only. It contradicts how these
installations are operated, would need a second certificate lifecycle next to the one nginx
already manages, and buys nothing the forwarded-scheme check does not.

### Decision 2 -- Hand-rolled protocol layer on `json-simple`, not the MCP Java SDK

The official `io.modelcontextprotocol.sdk` core builds its HTTP transport on
`jakarta.servlet`. This server targets WildFly 26.1.3 / Jakarta EE 8, which is the `javax.*`
namespace. Adopting the SDK would mean either shading and bytecode-rewriting it or moving the
whole server to Jakarta EE 9+ -- a far larger change than the MCP server itself.

What the SDK would actually save is modest: MCP over HTTP is JSON-RPC 2.0 request/response
plus SSE framing plus header validation. The tool layer -- the genuinely large part -- would
be hand-written either way.

`json-simple` is already on the EAR classpath and is what `ToolRegistry` and `AssistantAPI`
already use, so no new dependency enters the supply chain for a public-facing component.

*Rejected:* MCP Java SDK (namespace mismatch); Jackson-based custom layer (adds a dependency
where an equivalent one is already present).

### Decision 3 -- Independent tool implementations, no sharing with `ToolRegistry`

The 60 data tools are re-implemented in `j-lawyer-mcp` against `*Local` EJB interfaces.

Sharing is unattractive in both directions. `ToolRegistry` is a client class: it depends on
`UserSettings`, `EventBroker`, `JLawyerServiceLocator` and `*Remote` stubs, and it returns
German prose strings assembled for an LLM chat context. Lifting it into a shared module would
drag client concerns into the server or force an abstraction layer over both -- and would
couple the client's approval-dialog semantics to a protocol where approval happens in a
third-party host.

The two also need to diverge. MCP tools want JSON Schema `inputSchema`, `outputSchema` with
`structuredContent`, `annotations`, and `resource_link` results; `ToolRegistry` has a flat
`ToolParameter` list with everything typed as `string`. Server-side tools additionally need
argument validation and output size caps that a trusted in-process client never needed.

The cost is real -- 60 tools exist twice, and a behaviour fix may need applying twice. It is
accepted, and the duplication is confined to the tool bodies: both sides call the same EJB
services, so the actual business logic is shared where it matters.

*Rejected:* extracting a shared `j-lawyer-tools` module (couples client and server release
cycles, forces a lowest-common-denominator tool model); having the MCP server call the client
(nonsensical).

### Decision 3a -- `web_search` and `fetch_url` are not exposed over MCP

The MCP catalogue is the 60 tools that read or write j-lawyer data. The two web tools stay
in the client only.

They are the only tools in the registry that would make **j-lawyer-server** originate an
outbound HTTP request with a caller-supplied URL. On a public endpoint that is an SSRF and
egress surface -- a reachable request forger sitting inside the firm's network perimeter --
and it would have to be defended with an address-range denylist that is notoriously easy to
get subtly wrong (DNS rebinding between check and connect, IPv6-mapped IPv4, redirect
chains, `0.0.0.0`, decimal-encoded addresses).

What that risk would buy is close to nothing. Every target host -- Claude Cowork, Claude
Desktop, Claude Code, ChatGPT -- already has first-class web search and fetching, better
than a Jsoup scrape, and will use its own rather than a tool proxied through a law firm's
server. Adding a fourth scope (`jlawyer.web`) and an administrator toggle to manage a
capability the caller already has is complexity spent against itself.

The distinction that matters is *who originates the request*. In the desktop client the
request comes from the user's own machine on the user's own network; over MCP it would come
from the server that holds every case file. The same tool is a reasonable thing in the first
position and a poor one in the second, so `ToolRegistry` keeps both tools unchanged and the
MCP catalogue simply does not carry them.

One consequence for the parity test (Decision 3): it asserts that the MCP tool set equals
`ToolRegistry`'s **minus exactly this documented pair**, so adding a new tool on either side
still fails the test, but this deliberate omission does not.

The catalogue has a second inward-only property worth stating, because it is easy to assume
otherwise: **no MCP tool dispatches anything out of the firm.** Every write tool terminates in
the database or the document store -- `create_invoice` calls `addInvoice`,
`create_document_from_template` calls `addDocumentFromTemplate`, `create_instant_message`
addresses j-lawyer's internal messenger. There is no send-email, send-beA, send-fax, print or
export tool in the registry, so nothing an AI host triggers can reach a client, a court or an
opposing party without a human afterwards opening the record and sending it. Every write is
therefore reviewable before it has external effect. Should a dispatching tool ever be added,
that is a fresh decision about scope and confirmation, not an extension of `jlawyer.write`.

Outbound HTTP is not eliminated from the module entirely -- resolving a Client ID Metadata
Document during OAuth client registration requires fetching an HTTPS URL. That request is
narrow (a registration-time metadata document, not arbitrary caller-supplied browsing), and
it still carries the full guard: scheme allowlist, blocked loopback/private/link-local/
multicast/metadata ranges re-checked after every redirect, a redirect cap, a timeout and a
response-size cap.

*Rejected:* shipping them behind a `jlawyer.web` scope and an admin toggle. Off-by-default
still means the SSRF guard must be written, maintained and trusted, and a firm that switches
it on gets a capability its AI host already had.

### Decision 4 -- Dual-era protocol support on one endpoint

`POST /j-lawyer-mcp/mcp` serves both eras, selecting from how the client opens:

| Client opens with | Server behaviour |
|---|---|
| Request carrying `_meta.io.modelcontextprotocol/protocolVersion` + `MCP-Protocol-Version`, `Mcp-Method`, `Mcp-Name` headers | Modern (`2026-07-28`): stateless, header/body validation, no session |
| `initialize` request | Legacy: mint `Mcp-Session-Id`, honour `GET` SSE stream and `DELETE` termination, per the negotiated revision |

Supported versions: `2026-07-28`, `2025-11-25`, `2025-06-18`, `2025-03-26`. A request for
anything else gets `400` + `UnsupportedProtocolVersionError` (`-32022`) with the `supported`
list. `server/discover` is implemented (mandatory in the modern era).

The deprecated 2024-11-05 HTTP+SSE transport (separate SSE + POST endpoints) is **not**
implemented. It is deprecated in the spec and eligible for removal; Claude supports Streamable
HTTP, and shipping a second transport shape on a public endpoint is avoidable attack surface.

Legacy sessions are server state: `Mcp-Session-Id` is bound to the token's `sub` and `jti` at
creation, validated on every subsequent request, expires after an idle timeout, and is
destroyed when the underlying grant is revoked. A session ID alone is never a credential --
the `Authorization` header is required on every request in both eras.

### Decision 5 -- Reuse the existing Elytron JWT path; add an audience

`add-web-client` already built exactly the mechanism this needs: `JwtService` /
`JwtClaims` in `j-lawyer-server-common`, `JwtKeyProvider` reading
`${jboss.server.config.dir}/j-lawyer-jwt.p12`, an Elytron `token-realm` verifying RS256
against the same keystore, and a `BEARER_TOKEN` + `BASIC` http-authentication-factory bound to
`jlawyer-security-domain`.

The MCP server issues tokens through the same `JwtService` and the same key, with
`aud=j-lawyer-mcp` instead of `j-lawyer-web`. The only server-config change is adding
`j-lawyer-mcp` to the `token-realm`'s audience list. Elytron then establishes the
`SecurityIdentity`, it propagates to the EJB tier, `@RolesAllowed` and `getCallerPrincipal()`
work unchanged -- and the user's name lands in `addCaseHistory` for free.

Consequence worth stating plainly: **a token minted for the web SPA and a token minted for MCP
are distinguished only by audience.** Elytron accepts both audiences on the same domain, so
audience enforcement for the MCP resource is done in the application layer, in the MCP
endpoint's own request filter, before any tool runs. A `j-lawyer-web` token presented at
`/j-lawyer-mcp/mcp` is rejected with `401`.

### Decision 6 -- Personal access tokens are long-lived JWTs with a revocation registry

An opaque random PAT cannot be validated by Elytron's `token-realm`, which only understands
JWTs. Rather than bolt a second authentication mechanism onto a public endpoint, a PAT **is**
an RS256 JWT: same issuer, same key, `aud=j-lawyer-mcp`, a long `exp` (user-chosen, capped),
a `scope` claim, and a `jti` registered in `mcp_access_grant`.

Elytron validates signature, issuer, audience and expiry; the MCP request filter then checks
`jti` against the grant registry and rejects revoked or unknown grants. The same `jti` check
covers OAuth access tokens, so revocation is one uniform mechanism rather than two.

Trade-off: the token is a bearer credential valid until revoked, and it is displayed once at
creation. Mitigations -- mandatory expiry (default 90 days, hard cap 1 year), scope selection
at creation, `last_used_at` tracking, one-click revocation, and PATs being unavailable unless
an administrator has enabled them.

*Rejected:* opaque tokens exchanged at a `/oauth/token` endpoint for a short-lived JWT. It
works, but adds a round trip and a second token store for no security gain over a registered,
revocable `jti`.

### Decision 7 -- Three scopes, with a hierarchy

| Scope | Grants | Tools |
|---|---|---|
| `jlawyer.read` | read-only access to everything the user may already see | 36 |
| `jlawyer.write` | implies `read`; create and modify | +22 |
| `jlawyer.destructive` | implies `write`; delete and cross-case moves | +2 (`delete_document`, `move_document_to_case`) |

MCP requires servers to honour scope hierarchies, so `jlawyer.destructive` alone satisfies a
`jlawyer.read` requirement. `scopes_supported` in the protected resource metadata advertises
the minimal useful set (`jlawyer.read`), with the rest reached through step-up authorization:
an under-scoped `tools/call` returns `403` with
`WWW-Authenticate: Bearer error="insufficient_scope", scope="jlawyer.write", resource_metadata="..."`,
which is the signal a conforming host uses to re-authorize.

Scope is a ceiling, never a floor. The evaluation order for every tool call is:
`MCP enabled` → `token valid, audience correct, jti not revoked` → `aiAgentRole` → `scope
covers the tool` → `EJB @RolesAllowed` → `group/case permissions`. A scope can only take
permissions away from what the user already has.

### Decision 7a -- No per-user scope cap; `aiAgentRole` plus the permission model is enough

Three distinct layers are easy to conflate, so they are named here explicitly:

| Layer | Who controls it | What it does |
|---|---|---|
| `aiAgentRole` | administrator | whether this user may use MCP at all |
| Scope / consent | the user, at authorization time | which *kinds* of operation this one client may perform on their behalf |
| j-lawyer permission model | the data itself | which cases, documents and calendars this user may touch — **always applied, never negotiable** |

The third layer is the one that carries the user's actual permissions, and it is not part of
consent at all. It runs on every tool call regardless of scope, regardless of client,
regardless of what the user consented to: EJB `@RolesAllowed`, `getAllowedCasesForUser`,
`checkGroupsForCase`. It cannot be widened by any token, and it is what makes an MCP
connection see exactly the same slice of the firm as the user's own desktop client.

Given that, an administrator-side scope cap adds little. It could only restrict a user from
granting *themselves* a broader operation class over MCP than they would like — it could never
expose data the permission model conceals, because scope only subtracts. The administrator
already holds the decisive lever in `aiAgentRole`: withdraw it and the user's MCP access ends
entirely, including existing grants.

**Decision: no per-user scope cap.** `aiAgentRole` gates access; the user chooses the scope
for each client at consent time; the j-lawyer permission model bounds the data unconditionally.

The residual this accepts, stated plainly: a user with `aiAgentRole` may grant a host
`jlawyer.destructive` without asking anyone, and that host can then delete documents *the user
could already have deleted in the client anyway*. The blast radius is the user's own
permissions, never more. If a firm later finds it needs a narrower lever, the natural place is
a second role (`mcpWriteRole`) rather than a per-user scope matrix — additive, and consistent
with how `aiAgentRole` itself works.

### Decision 8 -- Human-in-the-loop stays with the host, guarded by scope and annotations

`ToolRegistry` shows the user a four-option Swing approval dialog (allow / deny / always /
this session). Over MCP that dialog belongs to the host application, and j-lawyer cannot see
or trust it.

j-lawyer therefore controls what it *can* control: the granted scope (chosen by the user at
consent time, in j-lawyer's own UI), and accurate `annotations` on every tool so a host can
render meaningful warnings -- `readOnlyHint: true` on all 36 read tools, `destructiveHint:
true` on the two `RISK_HIGH` tools, and `idempotentHint` where re-execution is safe. With
the web tools gone, no MCP tool reaches outside the installation, so `openWorldHint` is
`false` throughout.

Server-driven confirmation via MRTR (`InputRequiredResult` + `elicitation/create`) exists in
the modern era and would let j-lawyer demand an explicit confirmation before a destructive
call. It is deliberately **not** used in this change: it is unavailable in the legacy era that
the primary target hosts speak, so it would produce inconsistent behaviour across clients. It
is the natural follow-up once hosts move to `2026-07-28`.

### Decision 9 -- Persistence

New tables (Flyway, next free numbers after `V3_6_0_6`; coordinate with `add-two-factor-auth`
and `add-dunning-and-enforcement`):

- `mcp_oauth_client` -- `client_id`, registration kind (`CIMD` / `DCR` / `PREREGISTERED`),
  client name, redirect URIs, hashed client secret (confidential clients), metadata document
  URI and cached document, timestamps.
- `mcp_authorization_code` -- hashed code, client, principal, granted scopes, PKCE
  `code_challenge` + method, `redirect_uri`, `resource`, expiry, single-use marker.
- `mcp_access_grant` -- `jti`, kind (`ACCESS` / `REFRESH` / `PAT`), principal, client, scopes,
  issue/expiry, `revoked_at`, `last_used_at`, human label, refresh-rotation lineage.
- `mcp_consent` -- principal × client × scopes, granted/revoked timestamps, so a returning
  client with unchanged scopes can skip the consent screen but a widened request cannot.
  This is the designated extension point should per-connection data extent be added later
  (see Open Questions): a groups dimension goes here, not into the resource identity.
- `mcp_session` -- legacy-era `Mcp-Session-Id` ↔ grant binding with idle expiry.

Secrets are stored hashed; tokens themselves are never persisted, only their `jti`.

## Risks / Trade-offs

- **A public endpoint into the entire case file.** Mitigated by: disabled by default, explicit
  admin activation with a configured base URL and origin allowlist, HTTPS-only, audience
  binding, `aiAgentRole` gating, scope ceilings, unchanged group/case permission enforcement,
  rate limiting, and a dedicated audit log. Residual risk is inherent to the feature; it is a
  firm-level decision, which is why activation is explicit.
- **Prompt injection reaching write tools.** A document or an email in a case file can contain
  instructions aimed at the model. The host, not j-lawyer, decides whether to act on them.
  Mitigations: default consent is `jlawyer.read`; `jlawyer.destructive` is separate and must be
  chosen deliberately; every write is attributed and audited, so damage is at least
  reconstructable. This risk cannot be eliminated server-side and should be stated plainly in
  the admin UI at activation time.
- **60 tools implemented twice.** Accepted (Decision 3). Mitigation: a test that asserts the
  MCP tool-name set matches `ToolRegistry`'s minus the two documented web-tool exclusions, so
  a tool added on one side surfaces as a failing test rather than silent drift.
- **Dual-era support doubles the transport surface.** Two request-handling shapes on the same
  public endpoint. Mitigated by handling era selection once, at the front of the request
  pipeline, and sharing everything downstream of JSON-RPC dispatch. The legacy path can be
  deleted once hosts have moved on.
- **Building an OAuth authorization server is a security-sensitive undertaking.** Mitigated by
  implementing exactly the subset MCP requires, reusing the audited `JwtService`, and keeping
  the AS scoped to the MCP resource only. External review of the authorization endpoints
  before the first release is strongly advised.
- **Server and proxy configuration drift.** Three settings live outside the deployment and
  must be applied per installation: the Elytron audience, `proxy-address-forwarding`, and the
  nginx location blocks. Each fails in a different and initially confusing way -- `401` on
  every request, refusal to issue any token, and clients that cannot discover the
  authorization server. Mitigation: the activation self-test probes all three through the
  public base URL and names the specific missing setting rather than reporting a generic
  transport error.
- **Long-lived PATs.** See Decision 6. Capped expiry, revocation, `last_used_at`, and an
  admin-level kill switch.

## Migration Plan

1. Ship the module disabled. No behaviour change on upgrade; the WAR deploys and answers
   `503` with a clear message until an administrator enables it.
2. Administrator generates or reuses `j-lawyer-jwt.p12` (already required by
   `add-web-client`), adds `j-lawyer-mcp` to the Elytron `token-realm` audience list, sets
   `proxy-address-forwarding="true"` on the `http-listener`, and restarts.
3. Administrator applies the reference nginx configuration: the `/j-lawyer-mcp/` location with
   SSE-compatible buffering and timeouts, and the `location =` mapping for the RFC 9728
   canonical metadata path. Where a catch-all `/.well-known/` block exists for ACME, the more
   specific MCP block must precede it.
4. Administrator enables MCP in the admin panel, sets the public base URL and origin
   allowlist, and runs the built-in self-test, which verifies audience, forwarded scheme and
   canonical metadata reachability.
5. Users with `aiAgentRole` connect a host: Claude Cowork / claude.ai via the OAuth flow,
   Claude Code / n8n via a PAT.
6. Rollback: disable in the admin panel (immediate; all grants stop working), or remove the
   `webModule` entry from the EAR and redeploy. No data migration is reversed -- the new
   tables are additive and unused when the feature is off.

## Open Questions

- **Per-connection data extent, deferred past v1.** The design bounds *whether* a user may
  use MCP (`aiAgentRole`) and *what kind* of operation a client may perform (scope), but not
  *which data* a given connection may reach. Case extent is bound to the user, so connecting
  a host grants it everything that user can see. A firm that wants AI help on routine traffic
  files but nothing near its criminal-defence mandates currently has only one answer: leave
  MCP off entirely. The workaround of connecting as a narrow technical user is not open to
  us -- it would put that account, not the human, into the case history, defeating the
  requirement the whole Elytron construction exists to satisfy.

  When this is picked up, the intended shape is a **case-group selection on the consent
  screen** (`mcp_consent` grows a groups dimension, enforced as an additional subtractive
  filter above `checkGroupsForCase`), *not* per-profile endpoints. `get_my_groups` already
  exists, so groups are a concept the user meets at consent time anyway. One endpoint, one
  discovery document, one canonical resource URI.

  Deferring costs nothing structurally, with one caveat worth recording: because tokens are
  minted flat with `aud=j-lawyer-mcp` (Decision 5), a later switch to *per-profile endpoints*
  would need additional audiences, whereas the consent-field route stays inside the grant
  claims. That asymmetry is the reason to prefer the consent route if this is ever built.
