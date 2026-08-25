## ADDED Requirements

### Requirement: OAuth 2.1 Resource Server Behaviour

The MCP server SHALL act as an OAuth 2.1 resource server. It SHALL accept access tokens only
in the `Authorization: Bearer` request header, SHALL reject tokens supplied in a URI query
string or any other location, and SHALL require authorization on every request.

On a missing, malformed, expired or otherwise invalid token the system SHALL respond with HTTP
`401 Unauthorized` and a `WWW-Authenticate: Bearer` header carrying a `resource_metadata`
parameter pointing at the protected resource metadata document, and a `scope` parameter naming
the scopes required for the attempted operation.

The system SHALL NOT log token values and SHALL NOT include them in error responses.

#### Scenario: Unauthenticated request is challenged with discovery information
- **WHEN** a client sends an MCP request without an `Authorization` header
- **THEN** the server SHALL respond with HTTP `401`
- **AND** the `WWW-Authenticate` header SHALL contain `resource_metadata` and `scope`

#### Scenario: Token in query string is refused
- **WHEN** a client passes an access token as a URI query parameter
- **THEN** the server SHALL respond with HTTP `401`
- **AND** SHALL NOT treat the query parameter as a credential

#### Scenario: Expired token is refused
- **WHEN** a client presents a syntactically valid but expired access token
- **THEN** the server SHALL respond with HTTP `401` with a `WWW-Authenticate` challenge

### Requirement: Protected Resource Metadata

The system SHALL publish an OAuth 2.0 Protected Resource Metadata document (RFC 9728) at a
well-known location under the configured public base URL, without requiring authentication.

The document SHALL declare the canonical `resource` URI of the MCP server, the
`authorization_servers` the client is to use, `bearer_methods_supported` of `header`, and
`scopes_supported` containing the minimal scope needed for basic functionality.

#### Scenario: Client discovers the authorization server
- **WHEN** a client fetches the `resource_metadata` URI named in a `401` challenge
- **THEN** the document SHALL be returned without authentication
- **AND** SHALL name the canonical resource URI and the authorization server

#### Scenario: Minimal scope is advertised
- **WHEN** the protected resource metadata is fetched
- **THEN** `scopes_supported` SHALL advertise `jlawyer.read` as the minimal useful scope
- **AND** SHALL NOT advertise `offline_access`

### Requirement: Authorization Server Metadata

The system SHALL publish an OAuth 2.0 Authorization Server Metadata document (RFC 8414) at a
well-known location, without requiring authentication.

The document SHALL declare the `issuer`, the authorization, token, registration and revocation
endpoints, `code_challenge_methods_supported` containing `S256`, the supported grant types and
response types, the supported scopes, and `authorization_response_iss_parameter_supported`
set to `true`.

#### Scenario: Metadata declares PKCE support
- **WHEN** a client fetches the authorization server metadata
- **THEN** `code_challenge_methods_supported` SHALL contain `S256`
- **AND** SHALL NOT contain `plain`

#### Scenario: Metadata declares issuer identification
- **WHEN** a client fetches the authorization server metadata
- **THEN** `authorization_response_iss_parameter_supported` SHALL be `true`

### Requirement: Client Registration

The system SHALL support three client registration mechanisms: Client ID Metadata Documents,
where an HTTPS URL is used as the `client_id` and the metadata document is fetched and
validated from it; OAuth 2.0 Dynamic Client Registration (RFC 7591); and administrator
pre-registration through the administration interface.

When resolving a Client ID Metadata Document, the system SHALL fetch it over HTTPS, SHALL
validate that the document's declared `client_id` matches the URL it was fetched from, SHALL
validate the declared `redirect_uris`, and SHALL apply the outbound-request restrictions
defined by the `mcp-server` capability (scheme allowlist, blocked internal address ranges
re-checked after every redirect, redirect cap, timeout and response-size cap).

The system SHALL allow an administrator to disable Dynamic Client Registration, and SHALL
allow an administrator to review and delete registered clients.

#### Scenario: Claude registers dynamically
- **WHEN** a Claude custom connector posts a Dynamic Client Registration request and dynamic
  registration is enabled
- **THEN** the system SHALL register the client and return client credentials
- **AND** the client SHALL appear in the administration interface

#### Scenario: Client ID Metadata Document is validated
- **WHEN** a client presents an HTTPS URL as its `client_id`
- **THEN** the system SHALL fetch and validate the metadata document
- **AND** SHALL reject the client if the document's `client_id` does not match the fetch URL

#### Scenario: Metadata document URL cannot target an internal address
- **WHEN** a `client_id` URL resolves to a loopback, private or link-local address
- **THEN** the system SHALL refuse the registration and SHALL NOT issue the request

#### Scenario: Dynamic registration can be switched off
- **WHEN** an administrator has disabled dynamic registration and a client posts a
  registration request
- **THEN** the system SHALL reject it
- **AND** clients that were pre-registered by the administrator SHALL continue to work

### Requirement: Authorization Code Flow with PKCE

The system SHALL implement the OAuth 2.1 authorization code grant with PKCE. It SHALL require
a `code_challenge` with method `S256` on the authorization request and SHALL verify the
`code_verifier` on the token request. It SHALL reject the `plain` challenge method and SHALL
reject authorization requests that omit PKCE parameters.

Authorization codes SHALL be single-use, SHALL expire within a short bounded lifetime, SHALL
be bound to the client, the redirect URI, the granted scopes and the resource, and SHALL be
stored hashed. The system SHALL match `redirect_uri` values exactly against the registered
values, without pattern or prefix matching.

The system SHALL include the `iss` parameter (RFC 9207) in authorization responses, including
error responses.

#### Scenario: Authorization code is exchanged with a valid verifier
- **WHEN** a client completes the flow and presents the matching `code_verifier`
- **THEN** the system SHALL issue an access token and a refresh token

#### Scenario: Wrong verifier is refused
- **WHEN** a client presents an authorization code with a `code_verifier` that does not match
  the recorded `code_challenge`
- **THEN** the system SHALL refuse the token request
- **AND** SHALL invalidate the authorization code

#### Scenario: Authorization code cannot be replayed
- **WHEN** an authorization code that has already been redeemed is presented again
- **THEN** the system SHALL refuse the token request
- **AND** SHALL revoke the tokens previously issued from that code

#### Scenario: Redirect URI must match exactly
- **WHEN** a token request presents a `redirect_uri` that differs from the registered value by
  a path suffix, a query parameter or a trailing slash
- **THEN** the system SHALL refuse the request

#### Scenario: Missing PKCE is refused
- **WHEN** an authorization request omits `code_challenge`
- **THEN** the system SHALL refuse the request

### Requirement: Interactive Login and Consent

The authorization endpoint SHALL authenticate the resource owner using j-lawyer's own
credential store, through the same authentication path used by HTTP Basic and the desktop
client. The system SHALL NOT introduce a separate MCP user store.

The system SHALL present a consent screen naming the requesting client, the scopes it
requests, and the identity of the logged-in user, and SHALL let the user grant a narrower set
of scopes than requested. The system SHALL record the granted consent so a returning client
requesting the same or a narrower scope set may skip the screen, while a request for wider
scopes SHALL always require explicit consent.

Where two-factor authentication is configured for a user, it SHALL be enforced at this
authorization endpoint.

#### Scenario: User authenticates with j-lawyer credentials
- **WHEN** a user reaches the authorization endpoint
- **THEN** they SHALL be able to log in with their existing j-lawyer username and password
- **AND** no separate MCP account SHALL be required

#### Scenario: User narrows the requested scopes
- **WHEN** a client requests `jlawyer.destructive` and the user grants only `jlawyer.read`
- **THEN** the issued token SHALL carry only `jlawyer.read`
- **AND** write and destructive tools SHALL NOT be available to that token

#### Scenario: Wider request re-prompts
- **WHEN** a client that was previously granted `jlawyer.read` requests `jlawyer.write`
- **THEN** the consent screen SHALL be shown again

#### Scenario: Invalid credentials are refused uniformly
- **WHEN** a login attempt at the authorization endpoint uses invalid credentials
- **THEN** the system SHALL refuse it without disclosing whether the username exists

### Requirement: Resource Indicators and Audience Binding

The system SHALL accept the `resource` parameter (RFC 8707) on authorization and token
requests, and SHALL bind issued tokens to the canonical URI of the MCP server as their
audience.

The system SHALL validate on every MCP request that the presented token was issued for this
MCP server as its intended audience, and SHALL reject with HTTP `401` any token carrying a
different audience -- including tokens issued by this same installation for another purpose.
The system SHALL NOT accept, forward or otherwise transit tokens issued by third parties.

#### Scenario: Token for another audience is refused
- **WHEN** a client presents a token issued by this installation for the web client audience
- **THEN** the MCP endpoint SHALL respond with HTTP `401`
- **AND** SHALL NOT execute any tool

#### Scenario: Third-party token is never forwarded
- **WHEN** a request carries a token issued by an external identity provider
- **THEN** the system SHALL reject it
- **AND** SHALL NOT pass it to any downstream service

#### Scenario: Resource parameter is honoured
- **WHEN** a client includes the MCP server's canonical URI as the `resource` parameter
- **THEN** the issued token's audience SHALL be that canonical URI

### Requirement: Token Issuance and Identity Propagation

The system SHALL issue access tokens as RS256 JWTs signed with the installation's existing
signing key, carrying the subject, the user's j-lawyer roles, the granted scopes, a unique
`jti`, the MCP audience and a bounded expiry. Access tokens SHALL be short-lived.

Token verification SHALL be performed by the container's bearer-token mechanism, so that the
authenticated identity propagates to the EJB tier and `getCallerPrincipal()` returns the real
j-lawyer user for every tool invocation.

#### Scenario: Caller principal is the real user
- **WHEN** a tool is executed on behalf of the user `mmueller`
- **THEN** `getCallerPrincipal().getName()` inside the invoked EJB SHALL return `mmueller`

#### Scenario: Roles from the token drive EJB authorization
- **WHEN** a user without a role required by an EJB method invokes a tool that calls it
- **THEN** the EJB `@RolesAllowed` check SHALL deny the call
- **AND** the tool SHALL return a tool execution error

#### Scenario: Access tokens are short-lived
- **WHEN** an access token is issued through the authorization code flow
- **THEN** its lifetime SHALL be bounded and short
- **AND** continued access SHALL require refresh

### Requirement: Refresh Token Rotation and Revocation

The system SHALL rotate refresh tokens on each use, invalidating the previous one. On
detecting reuse of an already-rotated refresh token, the system SHALL revoke the entire grant
lineage.

The system SHALL implement a token revocation endpoint (RFC 7009) and SHALL maintain a
server-side registry of issued grants keyed by `jti`, checked on every MCP request, so that
revocation takes effect immediately rather than at token expiry.

#### Scenario: Refresh token rotates
- **WHEN** a client redeems a refresh token
- **THEN** a new refresh token SHALL be issued and the previous one SHALL become invalid

#### Scenario: Refresh token reuse revokes the lineage
- **WHEN** a refresh token that has already been rotated is presented again
- **THEN** the system SHALL refuse the request
- **AND** SHALL revoke all tokens descending from that grant

#### Scenario: Revocation takes effect immediately
- **WHEN** a user revokes a grant whose access token has not yet expired
- **THEN** the next MCP request presenting that access token SHALL be rejected with HTTP `401`

### Requirement: Scope Model and Enforcement

The system SHALL define the scopes `jlawyer.read`, `jlawyer.write` and `jlawyer.destructive`.
`jlawyer.write` SHALL imply `jlawyer.read` and `jlawyer.destructive` SHALL imply
`jlawyer.write`; the system SHALL honour this hierarchy when deciding whether a token is
sufficient for an operation.

`tools/list` SHALL return only the tools covered by the granted scopes. A `tools/call` for a
tool the granted scopes do not cover SHALL be answered with HTTP `403 Forbidden` and a
`WWW-Authenticate: Bearer` header carrying `error="insufficient_scope"`, a `scope` parameter
naming all scopes required for the operation, and `resource_metadata`.

Scope SHALL only restrict. It SHALL NOT grant any access the user does not already hold under
the j-lawyer permission model.

#### Scenario: Broader scope satisfies a narrower requirement
- **WHEN** a token granting `jlawyer.destructive` calls a read-only tool
- **THEN** the call SHALL be permitted

#### Scenario: Insufficient scope triggers a step-up challenge
- **WHEN** a token granting only `jlawyer.read` calls `create_event`
- **THEN** the server SHALL respond with HTTP `403`
- **AND** `WWW-Authenticate` SHALL carry `error="insufficient_scope"` and
  `scope="jlawyer.write"`

#### Scenario: All required scopes are challenged at once
- **WHEN** an operation requires more than one scope the token lacks
- **THEN** the `scope` parameter of the challenge SHALL name all of them in a single response

#### Scenario: Scope cannot widen permissions
- **WHEN** a token granting `jlawyer.destructive` calls `delete_document` for a document in a
  case the user's groups exclude
- **THEN** the call SHALL be denied by the permission model
- **AND** the document SHALL NOT be deleted

### Requirement: AI Agent Role Gating

The system SHALL require the `aiAgentRole` for all MCP access. A user without that role SHALL
NOT be able to complete the authorization flow, SHALL NOT be able to create a personal access
token, and SHALL receive no tools if a previously issued token is presented after the role has
been revoked.

#### Scenario: User without the role cannot authorize
- **WHEN** a user without `aiAgentRole` logs in at the authorization endpoint
- **THEN** the system SHALL refuse to issue a token and SHALL explain that the AI agent
  permission is missing

#### Scenario: Revoking the role disables an existing connection
- **WHEN** an administrator revokes `aiAgentRole` from a user who has an active MCP grant
- **THEN** subsequent MCP requests under that grant SHALL be refused
- **AND** `tools/list` SHALL NOT return any tool

### Requirement: Personal Access Tokens

The system SHALL allow a user holding `aiAgentRole` to create personal access tokens for MCP
hosts that cannot perform a browser-based authorization flow, when an administrator has
enabled the feature.

Each personal access token SHALL carry a user-supplied label, a user-selected scope set, and a
mandatory expiry with a configurable default and a hard upper bound. The token value SHALL be
displayed exactly once at creation. The system SHALL record `last_used_at` and SHALL allow the
user and any administrator to revoke it, taking effect immediately.

Personal access tokens SHALL be subject to the same audience validation, `aiAgentRole` check,
scope enforcement and j-lawyer permission model as tokens issued through the authorization
code flow.

#### Scenario: Token is shown once
- **WHEN** a user creates a personal access token
- **THEN** the value SHALL be displayed once
- **AND** SHALL NOT be retrievable afterwards through any interface

#### Scenario: Expiry is mandatory and capped
- **WHEN** a user attempts to create a personal access token with no expiry or with an expiry
  beyond the configured maximum
- **THEN** the system SHALL refuse and SHALL name the permitted maximum

#### Scenario: Revocation is immediate
- **WHEN** a user revokes a personal access token
- **THEN** the next MCP request presenting it SHALL be rejected with HTTP `401`

#### Scenario: Personal access token obeys its scopes
- **WHEN** a personal access token created with only `jlawyer.read` is used to call a write
  tool
- **THEN** the server SHALL respond with HTTP `403` and an `insufficient_scope` challenge

#### Scenario: Feature can be disabled centrally
- **WHEN** an administrator disables personal access tokens
- **THEN** users SHALL NOT be able to create new ones
- **AND** existing ones SHALL stop being accepted

### Requirement: Connection Management

The system SHALL provide each user with a view of the MCP clients they have authorized,
showing the client, the granted scopes, the time of authorization and the time of last use,
with the ability to revoke any of them. Administrators SHALL have the equivalent view across
all users.

#### Scenario: User reviews and revokes a connection
- **WHEN** a user opens their MCP connections view after authorizing a client
- **THEN** they SHALL see the client name, the granted scopes and the last use timestamp
- **AND** revoking it SHALL immediately end that client's access

#### Scenario: Administrator sees all connections
- **WHEN** an administrator opens the MCP administration view
- **THEN** they SHALL see the grants of all users and SHALL be able to revoke any of them

### Requirement: Transport Security of Authorization Endpoints

The system SHALL serve the MCP, OAuth and discovery endpoints over HTTPS. Where the
installation is reachable over plaintext HTTP, the system SHALL refuse to complete
authorization flows and SHALL NOT issue tokens.

Redirect URIs SHALL be required to use HTTPS, except for loopback redirect URIs used by
locally installed clients.

#### Scenario: Plaintext request does not yield a token
- **WHEN** an authorization or token request arrives over plaintext HTTP
- **THEN** the system SHALL refuse it and SHALL NOT issue a token

#### Scenario: Non-HTTPS redirect URI is rejected at registration
- **WHEN** a client registers a redirect URI with a non-HTTPS, non-loopback scheme
- **THEN** the system SHALL reject the registration

### Requirement: Abuse Protection on Authorization Endpoints

The system SHALL rate limit the authorization, token, registration and revocation endpoints
per client and per source, SHALL apply throttling and lockout to repeated failed login
attempts at the authorization endpoint consistent with the rest of j-lawyer, and SHALL respond
to failed credential and token requests in a way that does not disclose whether a username,
client or token exists.

Failed authorization attempts SHALL be recorded in the audit log without credential values.

#### Scenario: Repeated failed logins are throttled
- **WHEN** an attacker submits many failed login attempts at the authorization endpoint
- **THEN** the system SHALL throttle or lock further attempts
- **AND** SHALL record the attempts in the audit log

#### Scenario: Token endpoint does not disclose which input was wrong
- **WHEN** a token request fails because of an unknown client, an invalid code or a wrong
  verifier
- **THEN** the returned error SHALL NOT distinguish which of these was the cause
