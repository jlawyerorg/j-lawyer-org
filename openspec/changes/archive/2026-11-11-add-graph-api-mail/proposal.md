# Change: Unify mail access on server with Graph API + Client Credentials for O365

## Why
The current mail architecture splits responsibilities: the desktop client connects directly to IMAP/SMTP servers and holds `javax.mail.Message` objects in memory, while the server only manages OAuth tokens. This creates two problems: (1) Office 365 integration via Device Code Flow is fragile (blocked by Conditional Access/MFA, requires periodic user re-authentication), and (2) adding Graph API as a second backend would force the client to distinguish between two completely different mail access paths, distributing logic across client and server.

By moving all mail operations to the server behind a unified abstraction, the client works with a single set of DTOs and EJB methods regardless of backend (IMAP/SMTP or Graph API). This also solves the `javax.mail.Message` in-memory reference problem — messages are identified by opaque references (IMAP UID or Graph API message ID) instead of live IMAP objects.

## What Changes

### New capability: `email-service` — Unified server-side mail service
- Server-side abstraction layer for all mail operations (list folders, list/get/send/move/delete messages)
- Two backend implementations: IMAP/SMTP (for standard mailboxes) and Graph API (for O365 mailboxes)
- Unified DTOs (`MailFolderDTO`, `MailMessageDTO`, `MailAttachmentDTO`) shared between server and client
- Opaque message references: IMAP UID-based for IMAP, Microsoft message ID for Graph API
- Server manages IMAP connections (on-demand, not persistent)
- Server manages Graph API tokens via Client Credentials Flow

### New capability: `email-o365-graph` — Graph API backend
- Client Credentials Flow authentication (no Device Code, no user interaction)
- Graph API REST calls for all mail operations
- Replaces Device Code Flow entirely — `requestDeviceCode()`, `pollForTokens()`, `O365OAuthCouplingDialog` are removed

### Modified capability: `email-client` — Client uses unified server API
- Client no longer connects to IMAP/SMTP directly — all mail operations go through `EmailServiceRemote`
- Client works exclusively with DTOs, no `javax.mail.Message` objects
- `MessageContainer` wraps DTO instead of `javax.mail.Message`
- `LoadFolderThread` calls server EJB instead of opening IMAP connections
- `SendAction` submits emails to server instead of SMTP
- Single code path for all mailbox types

### Entity & database
- Remove `refreshToken` field from `MailboxSetup` entity and `token_refresh` column (no longer needed)
- Add `clientSecretExpiry` (Date) field to `MailboxSetup` for expiry warning (manually entered by admin)
- Reuse existing `msExchange`, `clientId`, `clientSecret`, `tenantId` fields

## Impact
- Affected specs: `email-client`, new `email-service`, new `email-o365-graph`
- Affected code:
  - `EmailService.java` / `EmailServiceRemote.java` / `EmailServiceLocal.java` (server — major expansion)
  - `MailboxSetup.java` (entity — remove `refreshToken`)
  - `EmailInboxPanel.java` (client — switch from IMAP to server EJB calls)
  - `SendAction.java` (client — switch from SMTP to server EJB calls)
  - `LoadFolderThread.java` (client — switch from IMAP to server EJB calls)
  - `FolderContainer.java` (client — adapt to DTOs)
  - `MessageContainer.java` (client — wrap DTO instead of `javax.mail.Message`)
  - `MoveAction.java` (client — use server EJB for move)
  - `O365OAuthCouplingDialog.java` (client — removed)
  - `OutgoingMailProcessor.java` (server — use unified service for sending)
  - SQL migration script
- `MailboxScannerTask.java` (server — adapt to unified service)
- **Additional scope**: mark as read/unread, pagination, reply/forward threading headers (In-Reply-To, References), inline image CID handling, BCC for sending, Client Secret expiry warning, mailbox scanner migration
- **BREAKING**: Existing O365 mailboxes must be reconfigured (Application Permissions + new Client-Secret)
