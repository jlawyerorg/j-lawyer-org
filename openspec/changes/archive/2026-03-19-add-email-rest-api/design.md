## Context
The EmailService already provides a unified server-side abstraction over IMAP/SMTP and Microsoft Graph API backends. The desktop client uses it via EJB remote interface. This change wraps those same operations as REST endpoints so external HTTP clients (mobile apps, web UIs, integrations) can access email functionality.

The existing REST API (v1–v8) uses a consistent pattern: `@Stateless` EJBs with JAX-RS annotations, JNDI lookups for local service references, HTTP Basic Auth, and JSON request/response bodies.

## Goals / Non-Goals
- Goals:
  - Expose all EmailService operations as REST endpoints under `/rest/v7/email`
  - Follow existing REST API patterns (auth, JNDI lookup, response structure, error handling)
  - Provide clean JSON serialization via dedicated REST POJOs
  - Support pagination, filtering, and search for message listing
  - Enable binary attachment download (both Base64 JSON and raw `application/octet-stream`)
- Non-Goals:
  - WebSocket/push notifications for new mail (out of scope)
  - Mailbox administration (create/update/delete mailbox configurations — already in SecurityService)
  - OAuth token management endpoints (internal server concern)
  - MIME-level manipulation or raw SMTP access

## Decisions

### URL Structure
Decision: Flat resource-oriented paths under `/v7/email/`.
```
GET    /v7/email/mailboxes                                          → list mailboxes
GET    /v7/email/mailboxes/{mailboxId}/folders                      → list folders
POST   /v7/email/mailboxes/{mailboxId}/folders                      → create folder
DELETE /v7/email/mailboxes/{mailboxId}/folders/{folderId}            → delete folder
DELETE /v7/email/mailboxes/{mailboxId}/folders/{folderId}/trash      → empty trash
GET    /v7/email/mailboxes/{mailboxId}/folders/{folderId}/messages   → list messages (paginated)
GET    /v7/email/mailboxes/{mailboxId}/messages/{messageRef}         → get message
DELETE /v7/email/mailboxes/{mailboxId}/messages/{messageRef}         → delete message
PUT    /v7/email/mailboxes/{mailboxId}/messages/{messageRef}/move    → move message
PUT    /v7/email/mailboxes/{mailboxId}/messages/{messageRef}/read    → mark read/unread
GET    /v7/email/mailboxes/{mailboxId}/messages/{messageRef}/eml     → download EML
GET    /v7/email/mailboxes/{mailboxId}/messages/{messageRef}/attachments/{attachmentId} → get attachment
POST   /v7/email/mailboxes/{mailboxId}/send                         → send email
POST   /v7/email/mailboxes/{mailboxId}/folders/{folderId}/append     → append to folder
GET    /v7/email/mailboxes/{mailboxId}/test                          → test connection
DELETE /v7/email/mailboxes/{mailboxId}/cache                         → invalidate caches
```
Why: Resource-oriented URLs are intuitive for external consumers. Nesting under `mailboxes/{mailboxId}` makes authorization scoping clear.
Alternatives considered: Using query parameters for mailboxId — rejected because path parameters are more RESTful and cacheable.

### Authentication & Authorization
Decision: Use existing HTTP Basic Auth + `@RolesAllowed("loginRole")` — same as all other v7 endpoints. Mailbox-level access is verified server-side via `SecurityServiceLocal.getMailboxesForUser()`: the endpoint rejects requests if the authenticated user does not have access to the requested mailbox.
Why: Consistent with existing API security model. No new auth mechanism needed.

### POJO Strategy
Decision: Create dedicated REST POJOs rather than serializing EJB DTOs directly.
Why:
- EJB DTOs (`MailMessageDTO`, etc.) are `Serializable` Java objects with potential circular references and fields not suitable for JSON.
- REST POJOs give control over JSON field naming, null handling, and can flatten/simplify structures.
- Follows the pattern established by other v7 endpoints (e.g. `RestfulInstantMessageV7`).

### Attachment Content Delivery
Decision: Two modes for attachment content:
1. JSON response with Base64-encoded content via `GET .../attachments/{attachmentId}` with `Accept: application/json`
2. Raw binary stream via `GET .../attachments/{attachmentId}` with `Accept: application/octet-stream` (or default)
Why: JSON mode is convenient for web/mobile clients. Raw binary is more efficient for large attachments and allows direct download.

### Path Parameter Encoding for messageRef
Decision: Use URL-safe Base64 encoding for `messageRef` path parameters where necessary (IMAP UIDs are numeric, Graph API IDs may contain special characters).
Why: Ensures valid URLs regardless of backend. The endpoint decodes before passing to EmailServiceLocal.

## Risks / Trade-offs
- **Performance**: Large attachment downloads via REST add HTTP overhead vs. EJB binary serialization → Mitigated by streaming response for raw binary mode.
- **Caching**: REST responses are not cached (server-side EmailService caching still applies) → Acceptable for initial version; HTTP cache headers can be added later.
- **Mailbox access check overhead**: Each request verifies mailbox access via SecurityService → One extra JNDI lookup per request; negligible compared to IMAP/Graph latency.

## Open Questions
None — all decisions resolved.
