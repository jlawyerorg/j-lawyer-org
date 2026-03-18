## Context
j-lawyer.org's mail architecture currently has the client connecting directly to IMAP/SMTP servers. The client holds live `javax.mail.Message` objects in memory as references to messages. This makes it impossible to cleanly add a second mail backend (Graph API) without splitting client logic into two paths. Additionally, `javax.mail.Message` objects become stale when connections drop, causing operational issues.

This change moves all mail operations to the server behind a unified abstraction. Both IMAP/SMTP and Graph API are server-side backends that implement the same interface. The client works exclusively with DTOs and opaque message references.

## Goals / Non-Goals
- Goals:
  - Unified server-side mail service: single client API for all mailbox types
  - Client works with DTOs only — no `javax.mail.Message`, no IMAP/SMTP connections
  - Enable O365 Graph API with Client Credentials Flow (no user interaction, no MFA)
  - Opaque message identification: client does not know if backend is IMAP or Graph API
  - Reuse existing `msExchange`, `clientId`, `clientSecret`, `tenantId` fields on `MailboxSetup`
- Non-Goals:
  - Maintaining backward compatibility with Device Code Flow
  - Graph API Webhooks/Subscriptions (polling is sufficient)
  - Persistent IMAP connections on the server (on-demand is sufficient for initial implementation)
  - Supporting delegated (user-context) Graph API flows

## Decisions

### Unified mail service abstraction
- **Decision**: `EmailService` exposes backend-agnostic methods: `listFolders()`, `listMessages()`, `getMessage()`, `getAttachments()`, `sendMail()`, `moveMessage()`, `deleteMessage()`. Internally it delegates to IMAP/SMTP or Graph API based on `msExchange` flag.
- **Alternatives considered**:
  - Two separate services (ImapService, GraphService) → client must choose, defeats the purpose
  - Client-side abstraction → still needs two connection mechanisms in the client
- **Rationale**: Single service, single client code path, backend selection is a server concern.

### Opaque message references
- **Decision**: Messages are identified by a `messageRef` string that is opaque to the client. The server creates and interprets these references:
  - IMAP: encodes folder path + IMAP UID (e.g. `"imap://INBOX/12345"`)
  - Graph API: uses Microsoft's message ID directly
- **Alternatives considered**:
  - Typed identifier class with separate fields → leaks backend details to client
  - Message-ID header → not unique across folders, some messages lack it
- **Rationale**: Opaque string is the simplest contract. The client passes it back to the server for any operation.

### IMAP UID stability
- **Decision**: Use IMAP UIDs (`UIDFolder.getUID()`) as stable message identifiers. UIDs are unique within a folder and persist across sessions as long as `UIDVALIDITY` does not change.
- **Risk**: If `UIDVALIDITY` changes (rare — folder rebuild, server migration), cached UIDs become invalid. Mitigation: include UIDVALIDITY in the message reference; server detects mismatch and returns error.
- **Rationale**: The codebase already uses UIDs in `FolderContainer` for caching. This extends that pattern to the server.

### IMAP connection management on server
- **Decision**: Open IMAP connections on-demand per operation, then close them. No persistent connection pool in the initial implementation.
- **Alternatives considered**:
  - Persistent connection pool → complex lifecycle management, idle connection timeouts, resource leaks
  - Connection-per-user session → EJBs are stateless, no session concept
- **Rationale**: On-demand is simpler and sufficient. IMAP connection setup is fast (~100ms). Can be optimized later if needed.

### Authentication: Client Credentials Flow for O365
- **Decision**: Use OAuth 2.0 Client Credentials Flow (`grant_type=client_credentials`) with `scope=https://graph.microsoft.com/.default`
- **Rationale**: No user interaction, no MFA, no Conditional Access conflicts, simplest admin setup.

### Entity cleanup
- **Decision**: Remove `refreshToken` field and `token_refresh` column. Reuse `msExchange`, `clientId`, `clientSecret`, `tenantId`. Add `clientSecretExpiry` (Date) for expiry warning.
- **Rationale**: Dead fields should be removed. Expiry date must be stored manually because the server cannot query Azure for it.

### Remove Device Code Flow
- **Decision**: Remove `requestDeviceCode()`, `pollForTokens()` from `EmailServiceRemote` and `O365OAuthCouplingDialog` from client.
- **Rationale**: Replaced entirely by Client Credentials Flow. Users must reconfigure anyway.

### DTO model
- **Decision**: Three DTOs in `j-lawyer-server-api` (serializable, shared between server and client):
  - `MailFolderDTO`: folderId, displayName, unreadCount, totalCount
  - `MailMessageDTO`: messageRef, messageId (Message-ID header), subject, from, to, cc, date, read, hasAttachments, body, bodyContentType, inReplyTo, references
  - `MailAttachmentDTO`: attachmentId, name, contentType, size, content (byte[]), inline, contentId (for CID-referenced inline images)
- **Rationale**: Includes threading headers (`messageId`, `inReplyTo`, `references`) for proper reply/forward support and CID metadata for inline image rendering. BCC is a send-only parameter, not stored in DTOs.

### Graph API endpoints used
- List folders: `GET /users/{email}/mailFolders`
- List messages: `GET /users/{email}/mailFolders/{folderId}/messages`
- Get message: `GET /users/{email}/messages/{messageId}`
- Get attachment: `GET /users/{email}/messages/{messageId}/attachments/{attachmentId}`
- Send mail: `POST /users/{email}/sendMail`
- Move message: `POST /users/{email}/messages/{messageId}/move`
- Delete message: `DELETE /users/{email}/messages/{messageId}`

### Required Azure Application Permissions
- `Mail.ReadWrite` — read and manage mail in all mailboxes
- `Mail.Send` — send mail as any user
- Admin Consent required (one-time, by Global Admin)
- Optional: Application Access Policy to restrict to specific mailboxes

## Risks / Trade-offs

- **IMAP connection overhead**: Opening connections on-demand adds ~100ms per operation. Mitigation: acceptable for user-triggered actions; can add connection pooling later.
- **No IMAP IDLE on server**: Client currently uses IDLE for real-time updates. Moving to server means polling. Mitigation: scheduled polling (e.g. every 60 seconds) for all mailbox types.
- **Large refactoring scope**: All client-side IMAP/SMTP code must be rerouted through server EJB calls. Mitigation: phased approach — DTOs and server service first, then client migration.
- **Rate limiting (Graph API)**: ~10,000 requests per 10 minutes per app/tenant. Mitigation: `$select`, batch requests, cache folder listings.
- **Attachment size (Graph API)**: >3 MB requires upload sessions. Mitigation: handle transparently in server implementation.
- **Client Secret management**: Secrets expire (max 2 years). Mitigation: document in UI.
- **Breaking change for O365 users**: Must reconfigure mailboxes and Azure App Registration. Mitigation: document migration steps.

## Migration Plan (for existing O365 users)
1. Update Azure App Registration: remove Delegated Permissions, add Application Permissions (`Mail.ReadWrite`, `Mail.Send`), grant Admin Consent
2. Optionally: remove "Allow public client flows" setting
3. Optionally: create Application Access Policy to restrict mailbox access
4. In j-lawyer: re-enter Client-ID and Client-Secret in mailbox settings

## Resolved Questions
- **Client Secret expiry warning**: Yes — the UI SHALL warn administrators when the Client Secret is approaching expiry.
- **Test connection button**: Yes — the existing "test connection" button in `MailboxSetupDialog` SHALL be adapted to verify Graph API access for O365 mailboxes and IMAP/SMTP access for standard mailboxes, both via the server.
- **Polling interval**: 30 seconds.
- **IMAP connection pooling**: Deferred — on-demand connections are sufficient for the initial implementation.
