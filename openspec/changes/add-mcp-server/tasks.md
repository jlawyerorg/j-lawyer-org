## 1. Module scaffolding and build integration

- [ ] 1.1 Create Maven module `j-lawyer-server/j-lawyer-mcp` (packaging `war`, Java 17,
      `javax.*` / Jakarta EE 8), depending on `j-lawyer-server-api`, `j-lawyer-server-entities`,
      `j-lawyer-server-common` and `json-simple`
- [ ] 1.2 Register the module in `j-lawyer-server/pom.xml`
- [ ] 1.3 Add a `webModule` entry with `contextRoot` `/j-lawyer-mcp` to
      `j-lawyer-server/j-lawyer-server-ear/pom.xml`
- [ ] 1.4 Create `src/main/webapp/WEB-INF/web.xml`: `/mcp/*` behind the `loginRole`
      constraint, `/.well-known/*` and `/oauth/*` public (no `auth-constraint`), `BASIC`
      login-config kept off these paths so no browser Basic dialog can fire
- [ ] 1.5 Create `jboss-web.xml` binding the war to `jlawyer-application-security-domain`
- [ ] 1.6 Add `ejb-local-ref` entries for every EJB service the tool layer needs
- [ ] 1.7 Verify `./build-fast.sh` produces an EAR containing `j-lawyer-mcp.war`

## 2. Persistence

- [ ] 2.1 Create JPA entity `McpOAuthClient` (`client_id`, registration kind, name,
      redirect URIs, hashed secret, metadata document URI + cached document, timestamps)
- [ ] 2.2 Create JPA entity `McpAuthorizationCode` (hashed code, client, principal, scopes,
      `code_challenge` + method, redirect URI, resource, expiry, single-use marker)
- [ ] 2.3 Create JPA entity `McpAccessGrant` (`jti`, kind `ACCESS`/`REFRESH`/`PAT`, principal,
      client, scopes, issued/expires, `revoked_at`, `last_used_at`, label, rotation lineage)
- [ ] 2.4 Create JPA entity `McpConsent` (principal × client × scopes, granted/revoked)
- [ ] 2.5 Create JPA entity `McpSession` (session ID, grant `jti`, principal, created,
      last-seen, expiry) for the legacy transport era
- [ ] 2.6 Create the corresponding facade beans following the existing facade pattern
- [ ] 2.7 Write the Flyway migration(s) creating the five tables with indexes on `jti`,
      `client_id`, `principal_id` and expiry columns — next free numbers after `V3_6_0_6`;
      coordinate with `add-two-factor-auth` and `add-dunning-and-enforcement`
- [ ] 2.8 Add the new entities to `persistence.xml`

## 3. Server configuration and identity plumbing

- [ ] 3.1 Add the `j-lawyer-mcp` audience to the Elytron `token-realm` in
      `docker/wildfly/standalone.xml`
- [ ] 3.2 Extend `j-lawyer-server/j-lawyer-io/AUTH-SETUP.md` (or add an MCP-specific
      `MCP-SETUP.md`) documenting the audience addition and the keystore requirement
- [ ] 3.3 Implement `McpJwtIssuer` on top of `JwtService` / `JwtKeyProvider`, issuing RS256
      tokens with `aud=j-lawyer-mcp`, subject, `roles`, `scope`, `jti` and bounded `exp`
- [ ] 3.4 Implement the request filter that runs after container authentication and, before
      any tool executes, validates audience, checks `jti` against `McpAccessGrant`, verifies
      `aiAgentRole`, and updates `last_used_at`
- [ ] 3.5 Verify end-to-end that `getCallerPrincipal()` inside an invoked EJB returns the
      MCP user

## 4. Authorization server

- [ ] 4.1 Implement the protected resource metadata endpoint (RFC 9728) with `resource`,
      `authorization_servers`, `scopes_supported`, `bearer_methods_supported`
- [ ] 4.2 Implement the authorization server metadata endpoint (RFC 8414) with `issuer`,
      endpoints, `code_challenge_methods_supported: ["S256"]`, grant/response types, scopes
      and `authorization_response_iss_parameter_supported: true`
- [ ] 4.3 Implement the authorization endpoint: parameter validation, exact `redirect_uri`
      matching, mandatory `S256` PKCE, `resource` handling, `state` pass-through, `iss` in
      responses (RFC 9207)
- [ ] 4.4 Implement the login page served by the authorization endpoint, authenticating via
      `SecurityService.authenticateAndGetRoles`
- [ ] 4.5 Wire the two-factor challenge into the login page (coordinate with
      `add-two-factor-auth`; leave the hook in place if that change has not landed)
- [ ] 4.6 Implement the consent page: client name, requested scopes, per-scope selection,
      logged-in identity, and persistence of the granted consent
- [ ] 4.7 Implement consent skip for a returning client requesting the same or a narrower
      scope set, and forced re-consent for a wider set
- [ ] 4.8 Implement the token endpoint: `authorization_code` and `refresh_token` grants,
      single-use codes, `code_verifier` verification, code-replay detection that revokes
      tokens issued from the reused code
- [ ] 4.9 Implement refresh token rotation with lineage tracking and reuse detection that
      revokes the whole lineage
- [ ] 4.10 Implement the revocation endpoint (RFC 7009)
- [ ] 4.11 Implement Dynamic Client Registration (RFC 7591) with an administrator kill switch
- [ ] 4.12 Implement Client ID Metadata Document resolution: HTTPS fetch, `client_id`/URL
      match validation, `redirect_uris` validation, caching, and the SSRF guard from task 12.3
- [ ] 4.13 Implement administrator pre-registration of clients
- [ ] 4.14 Enforce HTTPS on all authorization flows; reject non-HTTPS, non-loopback redirect
      URIs at registration time
- [ ] 4.15 Implement rate limiting, failed-login throttling and non-disclosing error responses
      on the authorization, token, registration and revocation endpoints

## 5. Personal access tokens

- [ ] 5.1 Implement PAT issuance as long-lived RS256 JWTs registered in `McpAccessGrant`
      with kind `PAT`, a label, a selected scope set and a mandatory capped expiry
- [ ] 5.2 Add the remote service methods for creating, listing and revoking PATs
      (JavaDoc in English on the `*Remote` interface, per project convention)
- [ ] 5.3 Enforce the administrator-level feature switch for PATs
- [ ] 5.4 Ensure PATs pass through exactly the same audience, `aiAgentRole`, scope and
      permission checks as OAuth-issued tokens

## 6. Scope model

- [ ] 6.1 Define `jlawyer.read`, `jlawyer.write`, `jlawyer.destructive` and the implication
      hierarchy
- [ ] 6.2 Annotate every tool with its required scope, derived from the `ToolDefinition.RISK_*`
      classification (36 read / 22 write / 2 destructive)
- [ ] 6.3 Filter `tools/list` by granted scope
- [ ] 6.4 Implement the `403` + `WWW-Authenticate: Bearer error="insufficient_scope"` step-up
      challenge, naming all required scopes in a single response

## 7. MCP transport — modern era (2026-07-28)

- [ ] 7.1 Implement JSON-RPC 2.0 request parsing, dispatch and error mapping on `json-simple`
- [ ] 7.2 Implement `_meta` handling (`io.modelcontextprotocol/protocolVersion`, `clientInfo`,
      `clientCapabilities`)
- [ ] 7.3 Implement the request-metadata headers `MCP-Protocol-Version`, `Mcp-Method`,
      `Mcp-Name`, including `=?base64?…?=` sentinel decoding
- [ ] 7.4 Implement header-vs-body validation returning `400` + `-32020` `HeaderMismatch`
- [ ] 7.5 Implement `202 Accepted` for accepted notifications
- [ ] 7.6 Implement the SSE response stream with `X-Accel-Buffering: no` and stream-close
      cancellation semantics
- [ ] 7.7 Return `405 Method Not Allowed` for `GET`/`DELETE`; ignore `Mcp-Session-Id` and
      `Last-Event-ID`
- [ ] 7.8 Implement `server/discover`
- [ ] 7.9 Implement `UnsupportedProtocolVersionError` (`-32022`) with the `supported` list, and
      `404` + `-32601` for unknown methods

## 8. MCP transport — legacy era (2025-11-25 / 2025-06-18 / 2025-03-26)

- [ ] 8.1 Implement era detection at the front of the request pipeline
- [ ] 8.2 Implement `initialize` / `notifications/initialized` with per-revision capability
      negotiation
- [ ] 8.3 Implement `Mcp-Session-Id` minting, binding to principal + grant `jti`, validation,
      idle expiry and `DELETE` termination
- [ ] 8.4 Implement the standalone `GET` SSE stream
- [ ] 8.5 Destroy sessions when the underlying grant is revoked or `aiAgentRole` is withdrawn
- [ ] 8.6 Require the `Authorization` header on every legacy-era request

## 9. Transport hardening

- [ ] 9.1 Implement `Origin` validation against the configured allowlist, returning `403`
- [ ] 9.2 Implement per-principal and per-client tool-call rate limiting with `429` +
      `Retry-After`
- [ ] 9.3 Implement request and response size limits
- [ ] 9.4 Ensure no token, secret or stack trace can reach a response body or a log line

## 10. Tool layer — read tools (36)

- [ ] 10.1 Build the tool framework: registration, JSON Schema `inputSchema`/`outputSchema`,
      annotations, argument validation, result assembly (`content` + `structuredContent`),
      and uniform mapping of failures to `isError: true`
- [ ] 10.2 Cases: `search_cases`, `get_case`, `get_case_by_id`, `get_history_for_case`,
      `get_parties_for_case`
- [ ] 10.3 Contacts: `search_contacts`
- [ ] 10.4 Documents: `list_case_documents`, `list_case_documents_by_date`,
      `search_case_documents`, `get_document_text`, `get_document_content`
- [ ] 10.5 Folders: `list_case_folders`, `list_folder_templates`
- [ ] 10.6 Calendar: `get_events_for_case`, `get_all_open_events`,
      `get_all_open_events_between_dates`, `list_event_types`, `find_free_slots`,
      `list_calendars`
- [ ] 10.7 Invoicing: `get_all_open_invoices`, `search_invoices`, `search_invoices_by_date`,
      `list_invoice_pools`
- [ ] 10.8 Timesheets: `get_all_open_timesheets`, `get_open_timesheets_for_case`,
      `get_timesheet_positions`
- [ ] 10.9 Tags: `list_document_tags`, `list_case_tags`, `list_contact_tags`
- [ ] 10.10 Templates and forms: `search_templates`, `list_letter_heads`, `list_form_types`
- [ ] 10.11 Messaging: `search_instant_messages`
- [ ] 10.12 Users and groups: `list_users`, `get_my_groups`
- [ ] 10.13 Misc: `get_current_date_time`
- [ ] 10.14 Implement `get_document_text` truncation with an explicit truncation notice, and
      `get_document_content` refusal above the configured maximum size

## 11. Tool layer — write tools (22)

- [ ] 11.1 Cases: `create_case`, `update_case`
- [ ] 11.2 Contacts: `create_contact`, `create_or_get_contact`, `update_contact`,
      `add_party_to_case`
- [ ] 11.3 Documents: `rename_document`, `create_note`, `move_document_to_folder`,
      `create_document_from_template`
- [ ] 11.4 Folders: `create_case_folder`, `apply_folder_template`
- [ ] 11.5 Calendar: `create_event`, `update_event`
- [ ] 11.6 Invoicing: `create_invoice`, `create_invoice_position`
- [ ] 11.7 Timesheets: `create_timesheet_position`
- [ ] 11.8 Tags: `set_document_tag`, `set_case_tag`, `set_contact_tag`
- [ ] 11.9 Messaging: `create_instant_message`
- [ ] 11.10 Forms: `create_case_form`
- [ ] 11.11 Verify for each write tool that the generated case history entry names the
      authenticated MCP user

## 12. Tool layer — destructive tools, and outbound-request guard

- [ ] 12.1 `delete_document` and `move_document_to_case` with `destructiveHint: true` and the
      `jlawyer.destructive` scope requirement
- [ ] 12.2 Confirm `web_search` and `fetch_url` are absent from the MCP catalogue and that no
      MCP tool accepts a caller-supplied URL, hostname or address to connect to; leave both
      tools untouched in `ToolRegistry`
- [ ] 12.3 Implement the SSRF guard used by Client ID Metadata Document resolution (task
      4.12): `https`-only, blocked loopback/private/link-local/multicast/metadata ranges,
      re-checked after every redirect, redirect cap, connection timeout and response size cap

## 13. Auditing

- [ ] 13.1 Implement the MCP tool invocation audit log (timestamp, principal, client, grant
      `jti`, tool, outcome, duration)
- [ ] 13.2 Audit denials: insufficient scope, missing `aiAgentRole`, permission denial, rate
      limit
- [ ] 13.3 Audit failed authorization attempts without credential values
- [ ] 13.4 Verify no credential value can appear in the audit log

## 14. Administration and user UI (j-lawyer-client)

- [ ] 14.1 Admin panel: enable/disable MCP, public base URL, allowed origins, PAT toggle,
      dynamic-registration toggle, rate limits, maximum response size (update the `.form`
      file alongside the `.java`)
- [ ] 14.2 Admin panel: display the activation warning that enabling MCP exposes case data to
      an external AI host
- [ ] 14.3 Admin panel: activation self-test that mints a token, calls the MCP endpoint and
      reports the exact `standalone.xml` remediation when the audience is missing
- [ ] 14.4 Admin panel: registered clients list with delete
- [ ] 14.5 Admin panel: all grants across all users, with revoke
- [ ] 14.6 User panel: create/label/revoke personal access tokens, value shown once
- [ ] 14.7 User panel: authorized MCP clients with scopes, last use and revoke
- [ ] 14.8 Add the required remote service methods and update `JLawyerServiceLocator`
      (JavaDoc in English on all `*Remote` interface changes)

## 15. Tests

- [ ] 15.1 Parity test asserting the MCP tool-name set equals `ToolRegistry`'s minus exactly
      `web_search` and `fetch_url`, so any other divergence fails
- [ ] 15.2 JSON Schema validity test for every `inputSchema` and `outputSchema`
- [ ] 15.3 Transport tests: header/body mismatch → `-32020`; unsupported version → `-32022`;
      unknown method → `404`/`-32601`; notification → `202`; `GET`/`DELETE` → `405` in the
      modern era
- [ ] 15.4 Legacy-era tests: `initialize` handshake, session binding, session rejected for a
      foreign principal, session without `Authorization` rejected
- [ ] 15.5 Authorization tests: PKCE required, wrong verifier refused, code replay revokes,
      exact redirect URI matching, refresh rotation and reuse detection
- [ ] 15.6 Audience tests: a `j-lawyer-web` token is refused at the MCP endpoint
- [ ] 15.7 Scope tests: hierarchy honoured, `tools/list` filtered, `insufficient_scope`
      challenge shape
- [ ] 15.8 Permission tests: case outside the user's groups invisible to `get_case` and
      `search_cases`; denied write creates no record and no history entry
- [ ] 15.9 Attribution test: a write through MCP produces a history entry naming the MCP user,
      identical to the desktop-client path
- [ ] 15.10 SSRF tests on Client ID Metadata Document resolution: loopback, RFC 1918,
      link-local, metadata address, non-HTTPS scheme and redirect-into-internal all refused
- [ ] 15.11 Error hygiene test: no stack trace, SQL or internal path in any response body

## 16. Interoperability verification

- [ ] 16.1 MCP Inspector: connect, complete OAuth, list and call tools
- [ ] 16.2 Claude Cowork / claude.ai custom connector: complete OAuth, list and call tools
- [ ] 16.3 Claude Code: connect with a personal access token, list and call tools
- [ ] 16.4 One non-Anthropic host (n8n or Open WebUI): connect and call tools
- [ ] 16.5 Record the verified protocol revision and auth mechanism per host in the setup
      documentation

## 17. Documentation

- [ ] 17.1 Write `j-lawyer-server/j-lawyer-mcp/README.md`: architecture, endpoints, protocol
      revisions supported, scope model, tool catalogue
- [ ] 17.2 Write the administrator setup guide: keystore, Elytron audience, activation, base
      URL, origins, self-test
- [ ] 17.3 Write the end-user connection guide per host (Claude Cowork, Claude Code, generic)
- [ ] 17.4 Document the security model and its residual risks, including prompt injection
      reaching write tools
