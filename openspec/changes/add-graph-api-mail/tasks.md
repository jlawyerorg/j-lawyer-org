## 1. Entity & Database

- [x] 1.1 Remove `refreshToken` field from `MailboxSetup.java` entity
- [x] 1.2 Add `clientSecretExpiry` (Date) field to `MailboxSetup.java` entity
- [x] 1.3 Create SQL migration script `V3_5_0_13__AddMailboxClientSecretExpiry.sql`
- [ ] 1.4 Create SQL migration script to drop `token_refresh` column from `mailbox_setup`

## 2. Data Transfer Objects

- [x] 2.1 Create `MailFolderDTO` in `j-lawyer-server-api`
- [x] 2.2 Create `MailMessageDTO` in `j-lawyer-server-api`
- [x] 2.3 Create `MailAttachmentDTO` in `j-lawyer-server-api`

## 3. Unified Mail Service Interface

- [x] 3.1–3.12 All methods added to `EmailServiceRemote` and `EmailServiceLocal`, Device Code Flow methods removed

## 4. Server-Side IMAP/SMTP Backend

- [x] 4.1–4.9 All IMAP/SMTP backend methods implemented in `EmailService.java`

## 5. Server-Side Graph API Backend

- [x] 5.1–5.12 All Graph API backend methods implemented, Client Credentials Flow replaces Device Code Flow

## 6. Server-Side Backend Dispatch

- [x] 6.1 Dispatch logic in `EmailService`: delegates to IMAP or Graph API based on `msExchange` flag
- [x] 6.2 `OutgoingMailProcessor` — not relevant (uses system-level SMTP, not per-user mailbox)

## 7. Mailbox Scanner Migration

- [x] 7.1 `MailboxScannerTask` — migrated to `processMailboxViaService()` using unified EmailService (listMessages, getMessage, getMessageAsEml, getAttachments, moveMessage, createFolder). Works for both IMAP and O365. Legacy `processMailbox()` retained as reference.

## 8. Client-Side Migration

- [x] 8.1 `MessageContainer` — backward-compatible: supports both DTO (`isServerBased()`) and legacy `Message`
- [x] 8.2 `LoadFolderThread` — server-based path via `runServerBased()` using `listMessages()` EJB
- [x] 8.3 `LoadFolderAction` — server-based path via `executeServerBased()` using `listMessages()` EJB
- [x] 8.4 `EmailInboxPanel` delete — server-based path using `deleteMessage()` EJB
- [x] 8.5 `EmailInboxPanel` mark-as-read — `MessageContainer.setRead()` handles server path internally
- [x] 8.6 `MoveAction` — server-based path using `moveMessage()` EJB
- [x] 8.7 `SendAction` — server-based path via `executeServerBased()` for `msExchange` mailboxes
- [x] 8.8 `EmailUtils` — DTO-based `reply()` overload with threading headers (`inReplyTo`, `references`); `SendEmailFrame` extended with `setThreadingHeaders()`/`getThreadingHeaders()`
- [x] 8.9 `MailContentUI` — server-based `setMessageServerBased()` populates CID cache from `MailAttachmentDTO` and displays message body
- [x] 8.10 `FolderContainer` — backward-compatible: supports both DTO and legacy `Folder`
- [x] 8.11 Client-side polling — `serverPollingTimer` in `EmailInboxPanel` calls `hasNewMessages()` every 30 seconds, repaints folder tree on new messages
- [ ] 8.12 Remove direct IMAP/SMTP connection code — **DEFERRED (legacy paths retained for non-server-based fallback; will be removed when all callers are migrated)**
- [x] 8.13 `O365OAuthCouplingDialog` — button action replaced with info message
- [x] 8.14 Mailbox config panel — `txtSecretExpiry` + `cmdSecretExpiry` with `MultiCalDialog`, load/save/reset/duplicate logic

## 12. Remaining Client-Side Direct Mail Access

- [x] 12.1 Migrate `SendEncryptedAction` — SMTP send retained (client-side PDF encryption), but Copy-to-Sent now uses `appendToFolder()` EJB instead of direct IMAP
- [x] 12.2 Migrate `SendEmailFrame` "Copy to Sent" — handled via `appendToFolder()` EJB (new server method)
- [x] 12.3 Migrate `SendEmailFrame` Draft saving — now uses `appendToFolder()` EJB to save drafts to server-side Drafts folder instead of direct IMAP
- [x] 12.4 Migrate `EmailUtils.sendReceipt()` — replaced 100+ lines of direct SMTP code with single `sendMail()` EJB call
- [x] 12.5 Remove dead code: `connect()` and `traverseFolders()` removed from `EmailInboxPanel`. `reconnectSelectedFolder()` retained — still referenced in legacy else-block
- [ ] 12.6 Remove dead code: Legacy IMAP paths in `LoadFolderThread.run()`, `LoadFolderAction.execute()`, `LoadEmailAction` — **DEFERRED (coupled with 12.7, requires coordinated removal)**
- [ ] 12.7 Remove dead code: Legacy `getMessage()`/`getFolder()` in `MessageContainer`/`FolderContainer` — **DEFERRED (still referenced at 50+ call sites in legacy else-blocks; requires big-bang removal of all else-blocks)**
- [x] 12.8 `MailSettingsTestAction` — confirmed no longer instantiated, can be deleted
- [ ] 12.9 Clean up `javax.mail` imports — **DEFERRED (all imports still needed by legacy else-blocks; removable only after 12.6+12.7)**

**Note:** Tasks 12.6, 12.7, 12.9 form a dependency chain — legacy else-blocks, deprecated methods, and javax.mail imports can only be removed together in a single coordinated pass after the server-based path is confirmed stable in production.

## 9. Server-Side Polling & New-Message State

- [x] 9.1–9.4 All polling and new-message state implemented in `EmailService.java`

## 10. Test Connection & Warnings

- [x] 10.1 Test connection button calls `testConnection()` via EJB for O365 mailboxes
- [x] 10.2 Client Secret expiry warning — shows warning in `MailboxSetupDialog` when within 30 days or expired

## 11. Validation & Testing

- [ ] 11.1–11.9 Manual testing required
