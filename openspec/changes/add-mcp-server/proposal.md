# Change: Add a Model Context Protocol (MCP) server to j-lawyer-server

## Why

The 62 AI tools implemented in `ToolRegistry.java` (`j-lawyer-client`) are only reachable
from inside the Swing client's Ingo chat panel. Every other AI product a firm might already
use -- Claude Cowork, Claude Desktop, Claude Code, ChatGPT connectors, n8n, Open WebUI --
cannot see j-lawyer data at all.

MCP is the de-facto interoperability standard for exactly this. Exposing a *server-side*
MCP endpoint makes the same case, document, contact, calendar, invoicing and timesheet
operations available to any MCP-capable host, without shipping a desktop client and without
each vendor needing a bespoke integration.

Two constraints shape the design:

1. **The user's identity must reach the EJB tier.** j-lawyer authorization is not a thin
   layer -- `ArchiveFileService` alone has ~180 `@RolesAllowed` annotations and derives case
   visibility, group checks, locking and audit fields (`setCreatedBy`, `addCaseHistory`)
   from `context.getCallerPrincipal()`. An MCP request must therefore run under a *container-
   established* identity, not under a service account with an application-level "acting user"
   field. Anything less produces history entries that do not name the real user.
2. **The endpoint is publicly exposed.** Unlike the Swing client (EJB remoting, usually
   inside the firm network), an MCP server has to be reachable by a cloud host such as
   Claude Cowork. It is an internet-facing door into the firm's entire case file.

## What Changes

### New module `j-lawyer-mcp` (WAR inside `j-lawyer-server.ear`)

- New Maven module `j-lawyer-server/j-lawyer-mcp`, packaged into the existing EAR with
  context root `/j-lawyer-mcp`, alongside `j-lawyer-io`. Own `web.xml`, own security
  constraints, independently disableable.
- MCP endpoint at `POST /j-lawyer-mcp/mcp`; OAuth endpoints under `/j-lawyer-mcp/oauth/*`.
  The discovery documents are served module-locally, but RFC 9728 places the protected
  resource metadata at a **host-root** path
  (`/.well-known/oauth-protected-resource/j-lawyer-mcp/mcp`) that a module under
  `/j-lawyer-mcp` cannot serve itself -- the reverse proxy maps it (see `design.md`,
  Decision 1a).
- **Disabled by default.** An administrator must enable the MCP server explicitly and
  configure the public base URL and allowed origins before it serves any request.

### MCP protocol implementation (self-contained, dual-era)

- **Modern era** (protocol revision `2026-07-28`): stateless per-request metadata, single
  POST endpoint, `MCP-Protocol-Version` / `Mcp-Method` / `Mcp-Name` header mirroring with
  header-vs-body validation, mandatory `server/discover` RPC, SSE response streams, no
  protocol-level sessions.
- **Legacy era** (`2025-11-25`, `2025-06-18`, `2025-03-26`): `initialize` handshake,
  `Mcp-Session-Id`, GET SSE stream, DELETE session termination. **This is what Claude Cowork
  and claude.ai custom connectors speak today**, so it is not optional.
- The era is selected from how the client opens the conversation, per the MCP compatibility
  matrix. Unsupported versions produce `UnsupportedProtocolVersionError` (`-32022`) listing
  what the server does support.
- Implemented against the raw JSON-RPC wire format using `json-simple` (already on the
  classpath). The official MCP Java SDK is **not** usable here: its servlet transport is
  built on `jakarta.servlet`, while this server targets WildFly 26.1.3 / Jakarta EE 8
  (`javax.*`). See `design.md`, Decision 2.

### Tool layer -- functional parity with `ToolRegistry`, independent codebase

- **60** of the 62 tools from `ToolRegistry.java` are re-implemented server-side against the
  **Local** EJB interfaces. `ToolRegistry` itself is not reused, moved or refactored; the
  client keeps its own copy (including the two web tools) and the two evolve independently.
- `web_search` and `fetch_url` are **deliberately not exposed over MCP**. Every target host
  already has its own, better web search, and they are the only tools that would make
  j-lawyer-server originate outbound HTTP on behalf of a caller -- an egress and SSRF surface
  on a public endpoint that buys nothing. They remain available to the in-client Ingo
  assistant, where the request originates from the desktop rather than from the server.
- Breakdown by write impact (derived from the existing `ToolDefinition.RISK_*` levels):
  - 36 read-only tools (`RISK_LOW` / unset)
  - 22 write tools (`RISK_MEDIUM`)
  - 2 destructive tools (`RISK_HIGH`): `delete_document`, `move_document_to_case`
- Tools gain proper MCP shape that the client-side registry never had: JSON Schema
  `inputSchema`, `outputSchema` + `structuredContent`, tool `annotations`
  (`readOnlyHint` / `destructiveHint` / `idempotentHint`) derived from the risk level, and
  `resource_link` / embedded-resource results for document content.

### Authorization -- OAuth 2.1 plus personal access tokens

- j-lawyer-server acts as **both** the OAuth 2.1 resource server and its own authorization
  server: RFC 9728 protected resource metadata, RFC 8414 authorization server metadata,
  authorization code + PKCE (S256), RFC 8707 resource indicators, RFC 9207 `iss`,
  refresh token rotation, RFC 7009 revocation.
- **Client registration**: Client ID Metadata Documents (the current default) *and*
  RFC 7591 Dynamic Client Registration (deprecated by MCP but still what Claude uses),
  *and* admin pre-registration.
- **Interactive login and consent** are served by j-lawyer itself: the authorization
  endpoint renders a j-lawyer login form, authenticates via
  `SecurityService.authenticateAndGetRoles` (the same store as Basic auth and the Swing
  client), and shows a consent screen on which the user picks the scopes to grant.
- **Personal access tokens (PATs)** for hosts without a browser flow (Claude Code, n8n,
  scripts): long-lived RS256 JWTs issued from the Swing client's user settings, revocable
  by `jti`. Note that claude.ai / Claude Cowork custom connectors accept **only** OAuth --
  they offer no field for a static bearer token -- so PATs are strictly the secondary path.
- Both paths mint RS256 JWTs with `aud=j-lawyer-mcp`, verified by the **existing** WildFly
  Elytron `token-realm` + `BEARER_TOKEN` mechanism introduced by `add-web-client`. Identity
  therefore propagates to the EJB tier and `getCallerPrincipal()` names the real user --
  which is what puts that user into the auto-generated case history entries.
- **Scopes**: `jlawyer.read` ⊂ `jlawyer.write` ⊂ `jlawyer.destructive`. `tools/list` is
  filtered by granted scope; an under-scoped `tools/call` returns `403` +
  `WWW-Authenticate: Bearer error="insufficient_scope"` so the host can run MCP step-up
  authorization.
- Scope is an **additional** restriction on top of, never a replacement for, the existing
  `aiAgentRole` check and the full j-lawyer group/case permission model.

### Hardening for public exposure

- Mandatory `Origin` validation (403 on mismatch); HTTPS enforced on the **effective external**
  scheme reported by the reverse proxy, never on the container's own plaintext connector;
  tokens never in URLs or logs.
- Audience binding: tokens not issued for this MCP server's canonical URI are rejected and
  never forwarded upstream (confused-deputy / token-passthrough prevention).
- Per-principal and per-client rate limiting on tool calls; brute-force protection and
  fixed-cost responses on the authorization and token endpoints.
- The MCP server never originates outbound HTTP on behalf of a caller. The one outbound
  request it makes at all -- fetching a Client ID Metadata Document during client
  registration -- is guarded by a scheme allowlist, blocked loopback / private / link-local /
  metadata address ranges, and redirect and response-size caps.
- A dedicated audit log of every MCP tool invocation (principal, client, tool, outcome,
  duration), separate from the case history.

### Administration and visibility

- New admin panel: enable/disable the MCP server, public base URL, allowed origins,
  rate limits, registered OAuth clients, active connections and grants, with per-grant
  revocation.
- New user panel: create/label/revoke personal access tokens, review which MCP clients the
  user has authorized and with which scopes.

## Impact

- **Affected specs**: `mcp-server` (new), `mcp-authorization` (new)
- **Affected code (new)**:
  - `j-lawyer-server/j-lawyer-mcp/` -- new WAR module (protocol, tool layer, OAuth endpoints)
  - `j-lawyer-server-entities` -- new JPA entities for OAuth clients, authorization codes,
    grants/tokens, consents, plus Flyway migrations (next free numbers after `V3_6_0_6`;
    coordinate with `add-two-factor-auth` and `add-dunning-and-enforcement`)
  - `j-lawyer-client` -- admin panel and PAT management panel (with `.form` files)
- **Affected code (modified)**:
  - `j-lawyer-server/pom.xml`, `j-lawyer-server/j-lawyer-server-ear/pom.xml` -- new module
    and `webModule` entry
  - `docker/wildfly/standalone.xml` -- add `j-lawyer-mcp` to the Elytron `token-realm`
    audience list, and set `proxy-address-forwarding="true"` on the `http-listener` so the
    forwarded scheme is honoured behind the reverse proxy
  - `j-lawyer-server/j-lawyer-io/AUTH-SETUP.md` -- document both, plus the reference reverse
    proxy configuration
  - `SecurityServiceRemote` / `SystemManagementRemote` -- new remote methods for PAT and
    MCP administration (JavaDoc in English, per project convention)
- **Not affected**: `ToolRegistry.java` and the client-side Ingo tool-calling loop stay
  exactly as they are; existing REST API versions v1-v8, HTTP Basic auth and the EJB remoting
  path are untouched.
- **No breaking changes.** The MCP server is additive and ships disabled.

## Open questions

- Whether to also expose MCP **resources** (cases and documents as addressable
  `jlawyer://` URIs) and **prompts** (firm-standard drafting instructions). Deliberately
  out of scope for this change; the transport and authorization work here is the
  prerequisite for both.
