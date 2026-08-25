## ADDED Requirements

### Requirement: MCP Module Deployment

The system SHALL provide the MCP server as a dedicated web module `j-lawyer-mcp`, packaged
into `j-lawyer-server.ear` with context root `/j-lawyer-mcp`. The MCP endpoint SHALL be
`POST /j-lawyer-mcp/mcp`. The module SHALL NOT alter the behaviour of `j-lawyer-io`, the EJB
remoting path, or any existing REST API version.

The MCP server SHALL be disabled by default. While disabled, all MCP, OAuth and discovery
paths SHALL respond with HTTP `503 Service Unavailable` and a body that names the required
administrative action, and SHALL NOT execute any tool.

#### Scenario: Fresh installation has MCP disabled
- **WHEN** an installation is upgraded to a version containing `j-lawyer-mcp` and no
  administrator has enabled the MCP server
- **THEN** `POST /j-lawyer-mcp/mcp` SHALL return HTTP `503`
- **AND** no tool SHALL be executed and no case data SHALL be returned

#### Scenario: Administrator enables the MCP server
- **WHEN** an administrator enables the MCP server and configures the public base URL and the
  allowed origins
- **THEN** `POST /j-lawyer-mcp/mcp` SHALL accept authorized requests
- **AND** the discovery documents SHALL be served under the configured public base URL

#### Scenario: Existing REST clients are unaffected
- **WHEN** the MCP server is enabled
- **THEN** HTTP Basic authenticated requests to `/j-lawyer-io/rest/v1` through `/rest/v8` and
  EJB remote invocations from the desktop client SHALL behave exactly as before

### Requirement: Dual-Era Protocol Support

The system SHALL support both MCP protocol eras on the same endpoint: the modern
per-request-metadata era (revision `2026-07-28`) and the legacy `initialize`-handshake era
(revisions `2025-11-25`, `2025-06-18`, `2025-03-26`). The server SHALL select the era from how
the client opens the conversation.

The system SHALL NOT implement the deprecated HTTP+SSE transport of revision `2024-11-05`.

#### Scenario: Modern client is served statelessly
- **WHEN** a client sends a request carrying `_meta.io.modelcontextprotocol/protocolVersion`
  and the required request-metadata headers
- **THEN** the server SHALL serve the request under revision `2026-07-28` semantics
- **AND** SHALL NOT mint or expect a session identifier

#### Scenario: Legacy client is served with a session
- **WHEN** a client sends an `initialize` request
- **THEN** the server SHALL complete the handshake under the negotiated legacy revision
- **AND** SHALL return an `Mcp-Session-Id` header that the client uses on subsequent requests

#### Scenario: Claude Cowork connects successfully
- **WHEN** a Claude Cowork or claude.ai custom connector connects to the configured MCP URL
  after completing the OAuth flow
- **THEN** the connection SHALL succeed under the legacy era
- **AND** the connector SHALL be able to list and call tools

### Requirement: Protocol Version Negotiation

The system SHALL implement the `server/discover` RPC, returning its supported protocol
versions, its capabilities and its server identity in a single response.

When a request declares a protocol version the server does not implement, the system SHALL
respond with HTTP `400 Bad Request` and an `UnsupportedProtocolVersionError` (JSON-RPC code
`-32022`) whose `data` contains the `supported` version list and the `requested` version.

When a request names an RPC method the server does not implement, the system SHALL respond
with HTTP `404 Not Found` and a JSON-RPC error with code `-32601`.

#### Scenario: Client discovers supported versions
- **WHEN** a client calls `server/discover`
- **THEN** the response SHALL list `2026-07-28`, `2025-11-25`, `2025-06-18` and `2025-03-26`
  as supported versions
- **AND** SHALL declare the `tools` capability

#### Scenario: Unsupported version is rejected with guidance
- **WHEN** a client sends a request declaring protocol version `1900-01-01`
- **THEN** the server SHALL respond with HTTP `400` and JSON-RPC error code `-32022`
- **AND** the error `data.supported` SHALL list the versions the server does implement

#### Scenario: Unknown method is distinguishable from a wrong endpoint
- **WHEN** a client calls a method the server does not implement
- **THEN** the server SHALL respond with HTTP `404` and a JSON-RPC error body with code `-32601`

### Requirement: Modern Streamable HTTP Transport

For requests served under revision `2026-07-28`, the system SHALL implement the Streamable
HTTP transport as specified: a single POST endpoint; a response of either
`Content-Type: application/json` or `Content-Type: text/event-stream`; HTTP `202 Accepted`
with no body for accepted JSON-RPC notifications; `X-Accel-Buffering: no` on SSE responses;
and treatment of a closed SSE response stream as cancellation of that request.

The system SHALL require the `MCP-Protocol-Version`, `Mcp-Method` and (for `tools/call`)
`Mcp-Name` headers, and SHALL validate that each header value matches the corresponding value
in the request body, decoding the `=?base64?…?=` sentinel encoding before comparison. On a
missing, malformed or mismatched header the system SHALL respond with HTTP `400 Bad Request`
and JSON-RPC error code `-32020` (`HeaderMismatch`).

Under this revision the system SHALL respond to `GET` and `DELETE` on the MCP endpoint with
HTTP `405 Method Not Allowed`, SHALL ignore any `Mcp-Session-Id` header, and SHALL ignore any
`Last-Event-ID` header.

#### Scenario: Header and body agree
- **WHEN** a modern client POSTs a `tools/call` for `search_cases` with
  `Mcp-Method: tools/call` and `Mcp-Name: search_cases`
- **THEN** the server SHALL execute the tool and return the result

#### Scenario: Header contradicts body
- **WHEN** a client POSTs a `tools/call` whose body names `delete_document` but whose
  `Mcp-Name` header reads `search_cases`
- **THEN** the server SHALL respond with HTTP `400` and JSON-RPC error code `-32020`
- **AND** SHALL NOT execute either tool

#### Scenario: Base64-encoded header value is decoded before comparison
- **WHEN** a client sends an `Mcp-Name` header using the `=?base64?…?=` sentinel encoding whose
  decoded value matches the body
- **THEN** the server SHALL accept the request

#### Scenario: Modern era rejects legacy transport verbs
- **WHEN** a client issues `GET` or `DELETE` against the MCP endpoint under revision
  `2026-07-28`
- **THEN** the server SHALL respond with HTTP `405 Method Not Allowed`

### Requirement: Legacy Session Transport

For clients that open with `initialize`, the system SHALL implement the negotiated legacy
revision's transport: an `Mcp-Session-Id` header returned on initialization and required on
subsequent requests, a standalone SSE stream opened with `GET`, and session termination with
`DELETE`.

Each session SHALL be bound at creation to the authenticated principal and to the `jti` of the
presented token. The system SHALL reject a request whose session identifier is unknown,
expired, or bound to a different principal or grant, with HTTP `404 Not Found`. A session
identifier SHALL NOT by itself authorize a request; a valid `Authorization` header SHALL be
required on every request in both eras.

Sessions SHALL expire after a configurable idle timeout and SHALL be destroyed immediately
when the underlying grant is revoked.

#### Scenario: Session is issued and reused
- **WHEN** a legacy client completes `initialize` with a valid token
- **THEN** the server SHALL return an `Mcp-Session-Id`
- **AND** SHALL accept subsequent requests that present both that session ID and the token

#### Scenario: Session identifier alone is not a credential
- **WHEN** a request presents a valid `Mcp-Session-Id` but no `Authorization` header
- **THEN** the server SHALL respond with HTTP `401 Unauthorized`
- **AND** SHALL NOT execute any tool

#### Scenario: Session bound to another principal is rejected
- **WHEN** a request presents a session ID created for a different principal
- **THEN** the server SHALL respond with HTTP `404 Not Found`

#### Scenario: Revoking a grant kills its sessions
- **WHEN** a user or administrator revokes a grant that has open MCP sessions
- **THEN** those sessions SHALL be destroyed
- **AND** subsequent requests using them SHALL be rejected

### Requirement: Origin Validation

The system SHALL validate the `Origin` header on all incoming connections to the MCP endpoint.
When an `Origin` header is present and is not in the administrator-configured allowlist, the
system SHALL respond with HTTP `403 Forbidden`.

#### Scenario: Disallowed origin is refused
- **WHEN** a request arrives with `Origin: https://evil.example.com`, which is not in the
  configured allowlist
- **THEN** the server SHALL respond with HTTP `403 Forbidden`
- **AND** SHALL NOT execute any tool

#### Scenario: Allowed origin passes
- **WHEN** a request arrives with an `Origin` that the administrator has allowlisted
- **THEN** the server SHALL process the request normally

### Requirement: Tool Catalogue Parity

The system SHALL expose over MCP the 60 data tools defined in `ToolRegistry.java`, covering
cases, contacts, documents, case folders, calendar, invoicing, timesheets, tags, templates
and forms, instant messages, and users and groups.

The system SHALL NOT expose the `web_search` and `fetch_url` tools over MCP. No MCP tool
SHALL cause the server to issue an outbound HTTP request with a caller-supplied target.

The MCP tool implementations SHALL be an independent server-side codebase. The system SHALL
NOT depend on `ToolRegistry` or any other `j-lawyer-client` class, and `ToolRegistry` SHALL
remain unchanged and fully functional for the in-client Ingo assistant.

Tool implementations SHALL invoke business logic through the **Local** EJB interfaces of the
same services the client-side tools use.

#### Scenario: Every client data tool has an MCP counterpart
- **WHEN** the MCP tool catalogue is compared against the tool identifiers registered in
  `ToolRegistry`
- **THEN** every identifier present in `ToolRegistry` other than `web_search` and `fetch_url`
  SHALL be present in the MCP catalogue
- **AND** an automated test SHALL fail if the two sets diverge by anything other than that
  documented pair

#### Scenario: Web tools are absent from the MCP catalogue
- **WHEN** a client calls `tools/list` with any granted scope
- **THEN** neither `web_search` nor `fetch_url` SHALL appear
- **AND** calling either SHALL return a JSON-RPC error for an unknown tool

#### Scenario: Web tools remain available in the desktop client
- **WHEN** a user runs an Ingo chat with tool calling in the desktop client
- **THEN** `web_search` and `fetch_url` SHALL remain available there unchanged

#### Scenario: Ingo assistant is unaffected
- **WHEN** the MCP server is enabled and a user runs an Ingo chat with tool calling in the
  desktop client
- **THEN** the client SHALL continue to use `ToolRegistry` and its own approval dialog
  unchanged

### Requirement: Tool Discovery

The system SHALL declare the `tools` capability and SHALL answer `tools/list` with the tools
available to the presented credential, in a deterministic order, with pagination support.

Each tool SHALL carry a `name`, a human-readable `title`, a `description`, and a valid JSON
Schema `inputSchema`. Tools with no parameters SHALL declare
`{ "type": "object", "additionalProperties": false }`. Where a tool returns structured data,
it SHALL additionally declare an `outputSchema`.

Each tool SHALL carry `annotations` derived from its write impact: `readOnlyHint: true` for
the 36 read-only tools; `destructiveHint: true` for `delete_document` and
`move_document_to_case`; and `idempotentHint` where repeating the call with identical
arguments has no additional effect. No MCP tool interacts with an open world, so
`openWorldHint` SHALL be `false` for every tool.

The advertised tool set SHALL vary only by the authorization presented on the request, and
SHALL NOT vary as a side effect of other requests on the same connection.

#### Scenario: Read-only credential sees only read tools
- **WHEN** a client calls `tools/list` with a token granting only `jlawyer.read`
- **THEN** the response SHALL contain the 36 read-only tools
- **AND** SHALL NOT contain any tool that creates, modifies or deletes data

#### Scenario: Destructive tools are annotated
- **WHEN** a client lists tools with a token granting `jlawyer.destructive`
- **THEN** `delete_document` and `move_document_to_case` SHALL carry `destructiveHint: true`
- **AND** every read-only tool SHALL carry `readOnlyHint: true`

#### Scenario: Parameterless tool declares a valid schema
- **WHEN** `list_users` or `get_current_date_time` appears in `tools/list`
- **THEN** its `inputSchema` SHALL be `{ "type": "object", "additionalProperties": false }`

#### Scenario: Tool order is stable
- **WHEN** a client calls `tools/list` twice without any change to the underlying tool set
- **THEN** the tools SHALL be returned in the same order both times

### Requirement: Tool Invocation and Result Shape

The system SHALL answer `tools/call` with a result containing a `content` array. Where a tool
produces structured data, the system SHALL populate `structuredContent` conforming to the
tool's `outputSchema` and SHALL also include the serialized JSON as a text content block.

The system SHALL distinguish the two MCP error mechanisms: an unknown tool name or a
malformed request SHALL be returned as a JSON-RPC error; a failure during execution --
invalid argument values, a business rule violation, an unavailable record, or a permission
denial -- SHALL be returned as a normal result with `isError: true` and a message the model
can act on.

The system SHALL validate every tool argument against the tool's `inputSchema` before
executing it, and SHALL reject arguments that do not conform.

#### Scenario: Successful call returns structured and text content
- **WHEN** a client calls `search_cases` with a query that matches cases the user may see
- **THEN** the result SHALL contain `structuredContent` conforming to the tool's `outputSchema`
- **AND** SHALL contain a text content block with the same data serialized

#### Scenario: Unknown tool is a protocol error
- **WHEN** a client calls a tool name that does not exist
- **THEN** the server SHALL return a JSON-RPC error
- **AND** SHALL NOT return a result with `isError`

#### Scenario: Business failure is a tool execution error
- **WHEN** a client calls `get_case` with a file number that does not exist
- **THEN** the server SHALL return a result with `isError: true` and an explanatory message
- **AND** SHALL NOT return a JSON-RPC error

#### Scenario: Malformed argument is rejected before execution
- **WHEN** a client calls `list_case_documents_by_date` with `fromDate` in a format other than
  `yyyy-MM-dd`
- **THEN** the server SHALL return a result with `isError: true` naming the expected format
- **AND** SHALL NOT invoke the underlying EJB service

#### Scenario: Internal failure does not leak implementation detail
- **WHEN** a tool invocation fails with an unexpected server-side exception
- **THEN** the server SHALL return a result with `isError: true` and a generic message
- **AND** SHALL log the full exception server-side
- **AND** SHALL NOT include a stack trace, SQL statement or internal path in the response

### Requirement: Document Content Delivery

The system SHALL return document text through `get_document_text`, honouring the `maxChars`
parameter with a default and hard upper bound, and SHALL indicate in the result when content
was truncated.

The system SHALL return binary document content through `get_document_content` as
base64-encoded data. The system SHALL enforce an administrator-configurable maximum response
size and SHALL return a tool execution error naming the actual and permitted size when a
document exceeds it, rather than a truncated or partial document.

#### Scenario: Text extraction is truncated with notice
- **WHEN** a client calls `get_document_text` for a document longer than the effective
  character limit
- **THEN** the server SHALL return the leading portion up to the limit
- **AND** the result SHALL state that the content was truncated and give the total length

#### Scenario: Oversized binary is refused, not truncated
- **WHEN** a client calls `get_document_content` for a document larger than the configured
  maximum response size
- **THEN** the server SHALL return a result with `isError: true` naming both sizes
- **AND** SHALL NOT return partial document data

### Requirement: Permission Enforcement in Tool Execution

Every tool invocation SHALL execute under the container-established identity of the
authenticated MCP user. The system SHALL NOT execute tools under a service account, a shared
technical user, or any identity supplied as a request parameter.

All existing j-lawyer authorization SHALL continue to apply unchanged: EJB `@RolesAllowed`
checks, case visibility via the user's allowed cases, and group permission checks on cases,
documents and calendar entries. The MCP server SHALL NOT bypass, widen or pre-empt any of
them.

When an operation is denied by the permission model, the system SHALL return a tool execution
error stating that the user lacks permission, and SHALL NOT disclose the existence or content
of the record where the permission model would otherwise conceal it.

#### Scenario: Case outside the user's groups is not readable
- **WHEN** a user calls `get_case` for a case whose group permissions exclude them
- **THEN** the server SHALL return a result with `isError: true`
- **AND** SHALL NOT return the case's parties, notes or documents

#### Scenario: Search results respect case visibility
- **WHEN** a user calls `search_cases` with a query that would match cases they may not see
- **THEN** the result SHALL contain only cases the user is permitted to see

#### Scenario: Write denied by group permissions
- **WHEN** a user calls `create_event` on a case they may not modify
- **THEN** the server SHALL return a result with `isError: true`
- **AND** no calendar entry and no history entry SHALL be created

### Requirement: Attribution of Write Operations

Every write operation performed through an MCP tool SHALL be attributed in j-lawyer to the
authenticated MCP user, exactly as if the same operation had been performed in the desktop
client. Automatically generated case history entries, `createdBy` fields and assignee defaults
SHALL name that user.

#### Scenario: Case history names the MCP user
- **WHEN** the user `mmueller` creates a calendar event through MCP with `create_event`
- **THEN** the automatically generated case history entry SHALL record `mmueller` as the
  acting user

#### Scenario: Created records carry the MCP user
- **WHEN** a user creates a note, an invoice, a contact or a timesheet position through MCP
- **THEN** the persisted record's creator/author field SHALL name that user

#### Scenario: Attribution is identical across channels
- **WHEN** the same operation is performed once through the desktop client and once through
  MCP by the same user
- **THEN** the resulting history entries SHALL name the same user in both cases

### Requirement: No Caller-Directed Outbound Requests

The system SHALL NOT provide any MCP tool that issues an outbound network request to a target
supplied by the caller.

The only outbound request the module makes at all is the resolution of a Client ID Metadata
Document during OAuth client registration. That request SHALL be restricted to the `https`
scheme, SHALL be refused when the target resolves to a loopback, link-local, private,
multicast or cloud-metadata address range, SHALL re-apply that check after every redirect,
and SHALL be subject to a redirect cap, a connection timeout and a maximum response size.

#### Scenario: No tool accepts a URL to fetch
- **WHEN** the full MCP tool catalogue is inspected
- **THEN** no tool SHALL accept a URL, hostname or address as a parameter that the server
  would connect to

#### Scenario: Metadata document fetch cannot reach an internal address
- **WHEN** client registration presents a `client_id` URL that resolves to `127.0.0.1`,
  `169.254.169.254` or a private RFC 1918 address
- **THEN** the system SHALL refuse the registration
- **AND** SHALL NOT issue the outbound request

#### Scenario: Metadata document fetch cannot be redirected inward
- **WHEN** a `client_id` URL is public but redirects to an internal address
- **THEN** the system SHALL stop following the redirect and refuse the registration

### Requirement: Rate Limiting

The system SHALL rate limit tool invocations per authenticated principal and per registered
client, with administrator-configurable limits. When a limit is exceeded, the system SHALL
respond with HTTP `429 Too Many Requests` and a `Retry-After` header.

#### Scenario: Excessive tool calls are throttled
- **WHEN** a client exceeds the configured tool invocation rate for its principal
- **THEN** the server SHALL respond with HTTP `429` and a `Retry-After` header
- **AND** SHALL NOT execute the tool

#### Scenario: One client's throttling does not affect another
- **WHEN** one registered client is being throttled
- **THEN** another client authorized by a different principal SHALL continue to be served

### Requirement: Tool Invocation Auditing

The system SHALL record every MCP tool invocation in a server-side audit log, capturing at
minimum the timestamp, the authenticated principal, the registered client, the grant `jti`,
the tool name, the outcome (success, tool execution error, permission denial, or rate limit),
and the duration.

The audit log SHALL NOT contain access tokens, refresh tokens, personal access tokens or
client secrets. The audit log SHALL be independent of, and SHALL NOT replace, the case history
entries generated by write operations.

#### Scenario: Successful call is audited
- **WHEN** a user successfully calls `create_note` through MCP
- **THEN** an audit record SHALL be written naming the principal, client, tool and outcome
- **AND** a separate case history entry SHALL be created by the business logic as usual

#### Scenario: Denied call is audited
- **WHEN** a tool call is refused because of insufficient scope or missing case permissions
- **THEN** an audit record SHALL be written recording the denial

#### Scenario: Audit log holds no credentials
- **WHEN** the audit log is inspected after any sequence of MCP requests
- **THEN** it SHALL contain no token, secret or credential value

### Requirement: MCP Administration

The system SHALL provide an administration user interface to enable or disable the MCP server,
configure the public base URL, the allowed origins, rate limits and maximum response sizes, review registered OAuth clients and active grants, and revoke any
grant.

On activation the system SHALL run a self-test that issues a token and verifies that it
authenticates against the MCP endpoint, and SHALL report a specific remediation message when
the server-level Elytron configuration does not yet accept the MCP token audience.

The administration interface SHALL state, at the point of activation, that enabling MCP
exposes case data to an external AI host.

#### Scenario: Self-test detects a missing audience configuration
- **WHEN** an administrator enables MCP on a server whose Elytron `token-realm` does not list
  the MCP token audience
- **THEN** the self-test SHALL fail
- **AND** the interface SHALL name the required `standalone.xml` change

#### Scenario: Administrator revokes an active grant
- **WHEN** an administrator revokes a grant listed in the administration interface
- **THEN** subsequent MCP requests presenting that grant SHALL be rejected with HTTP `401`
- **AND** any MCP session bound to that grant SHALL be destroyed

#### Scenario: Disabling stops all access immediately
- **WHEN** an administrator disables the MCP server
- **THEN** all subsequent MCP requests SHALL receive HTTP `503`
- **AND** previously issued tokens SHALL grant no access while it remains disabled
