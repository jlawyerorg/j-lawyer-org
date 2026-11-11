## ADDED Requirements

### Requirement: Unified Mail Service Interface
The server SHALL expose a unified mail service via `EmailServiceRemote` that abstracts over IMAP/SMTP and Graph API backends. The client SHALL use this service for all mail operations regardless of mailbox type.

#### Scenario: Client lists folders for any mailbox type
- **WHEN** a client calls `listFolders(mailboxId)`
- **THEN** the server SHALL return a `List<MailFolderDTO>` with folder ID, display name, unread count, and total count
- **AND** the server SHALL internally use IMAP for standard mailboxes and Graph API for O365 mailboxes (`msExchange=true`)
- **AND** the client SHALL NOT know which backend was used

#### Scenario: Client lists messages in a folder for any mailbox type
- **WHEN** a client calls `listMessages(mailboxId, folderId, top)`
- **THEN** the server SHALL return a `List<MailMessageDTO>` with message metadata
- **AND** each `MailMessageDTO` SHALL contain an opaque `messageRef` string for subsequent operations

#### Scenario: Client retrieves a full message by reference
- **WHEN** a client calls `getMessage(mailboxId, messageRef)`
- **THEN** the server SHALL return a `MailMessageDTO` with full body content and headers

#### Scenario: Client retrieves attachments for a message
- **WHEN** a client calls `getAttachments(mailboxId, messageRef)`
- **THEN** the server SHALL return a `List<MailAttachmentDTO>` with attachment metadata and content

#### Scenario: Client sends an email through unified service
- **WHEN** a client calls `sendMail(mailboxId, ...)` with to, cc, bcc, subject, body, attachments, priority, readReceipt flag, and optionally inReplyTo and references headers
- **THEN** the server SHALL send the email using the appropriate backend (SMTP or Graph API)
- **AND** the server SHALL set `In-Reply-To` and `References` headers if provided
- **AND** the server SHALL set `Disposition-Notification-To` and `Return-Receipt-To` headers if readReceipt is true
- **AND** the client SHALL NOT establish any direct SMTP connection

#### Scenario: Client moves a message to another folder
- **WHEN** a client calls `moveMessage(mailboxId, messageRef, targetFolderId)`
- **THEN** the server SHALL move the message using the appropriate backend

#### Scenario: Client deletes a message
- **WHEN** a client calls `deleteMessage(mailboxId, messageRef)`
- **THEN** the server SHALL delete the message using the appropriate backend

#### Scenario: Client marks a message as read or unread
- **WHEN** a client calls `markAsRead(mailboxId, messageRef, boolean read)`
- **THEN** the server SHALL update the read status using the appropriate backend (IMAP SEEN flag or Graph API PATCH)

#### Scenario: Client lists messages with pagination
- **WHEN** a client calls `listMessages(mailboxId, folderId, top, offset)`
- **THEN** the server SHALL return up to `top` messages starting from `offset`
- **AND** the server SHALL use IMAP message ranges or Graph API `$skip`/`$top` accordingly

### Requirement: Opaque Message References
The server SHALL identify messages using opaque reference strings that are stable across client sessions and independent of the mail backend.

#### Scenario: IMAP message reference uses UID
- **WHEN** the server returns messages from an IMAP mailbox
- **THEN** each `messageRef` SHALL encode the IMAP UID and UIDVALIDITY for the folder
- **AND** the client SHALL treat the `messageRef` as an opaque string

#### Scenario: Graph API message reference uses Microsoft message ID
- **WHEN** the server returns messages from an O365 mailbox
- **THEN** each `messageRef` SHALL contain the Microsoft Graph message ID
- **AND** the client SHALL treat the `messageRef` as an opaque string

#### Scenario: Server detects stale IMAP UID
- **WHEN** a client passes a `messageRef` whose UIDVALIDITY no longer matches the folder
- **THEN** the server SHALL return an error indicating the reference is stale
- **AND** the client SHALL refresh the folder listing

### Requirement: Mail DTOs
The server SHALL provide serializable DTO classes in `j-lawyer-server-api` for mail data exchange between server and client.

#### Scenario: MailFolderDTO contains folder metadata
- **WHEN** the server returns folder data
- **THEN** `MailFolderDTO` SHALL contain `folderId` (String), `displayName` (String), `unreadCount` (int), and `totalCount` (int)

#### Scenario: MailMessageDTO contains message metadata
- **WHEN** the server returns message data
- **THEN** `MailMessageDTO` SHALL contain `messageRef` (String), `messageId` (String — Message-ID header for threading), `subject` (String), `from` (String), `to` (String[]), `cc` (String[]), `date` (Date), `read` (boolean), `hasAttachments` (boolean), and optionally `body` (String), `bodyContentType` (String), `inReplyTo` (String), and `references` (String)

#### Scenario: MailAttachmentDTO contains attachment data
- **WHEN** the server returns attachment data
- **THEN** `MailAttachmentDTO` SHALL contain `attachmentId` (String), `name` (String), `contentType` (String), `size` (long), `content` (byte[]), `inline` (boolean), and `contentId` (String — for CID-referenced inline images)

### Requirement: Server-Side IMAP Backend
The server SHALL access IMAP mailboxes on behalf of the client using credentials stored in `MailboxSetup`.

#### Scenario: Server connects to IMAP on demand
- **WHEN** a mail operation is requested for a standard mailbox (`msExchange=false`)
- **THEN** the server SHALL open an IMAP connection using the stored credentials
- **AND** the server SHALL close the connection after the operation completes

#### Scenario: Server uses IMAP UID for message operations
- **WHEN** a mail operation references a specific message in an IMAP mailbox
- **THEN** the server SHALL use `UIDFolder.getMessageByUID()` to locate the message
- **AND** the server SHALL perform the operation (delete, move, mark read) on the located message

#### Scenario: Server sends email via SMTP
- **WHEN** a send operation is requested for a standard mailbox
- **THEN** the server SHALL connect to the configured SMTP server and send the email using stored credentials

### Requirement: Inline Image Handling
The server SHALL extract inline images (CID-referenced) from messages and deliver them as part of the attachment list with their Content-ID, so the client can resolve `cid:` references in HTML bodies.

#### Scenario: Message contains inline images with CID references
- **WHEN** a client calls `getAttachments(mailboxId, messageRef)` for a message with inline images
- **THEN** the returned `MailAttachmentDTO` list SHALL include inline images with `inline=true` and `contentId` set to the Content-ID value
- **AND** the client SHALL use the `contentId` to resolve `cid:` references in the HTML body

#### Scenario: Message contains only regular attachments
- **WHEN** a client calls `getAttachments(mailboxId, messageRef)` for a message without inline images
- **THEN** all returned `MailAttachmentDTO` entries SHALL have `inline=false` and `contentId=null`

### Requirement: Mailbox Scanner Integration
The existing server-side mailbox scanner (`MailboxScannerTask`) SHALL use the unified mail service for mail access instead of connecting directly via IMAP.

#### Scenario: Mailbox scanner reads messages via unified service
- **WHEN** the scheduled mailbox scanner processes a mailbox with `scanInbox=true`
- **THEN** the scanner SHALL use the unified mail service methods (`listMessages`, `getMessage`, `getAttachments`) instead of direct IMAP connections
- **AND** the scanner SHALL work identically for both IMAP and O365 mailboxes

#### Scenario: Mailbox scanner moves processed messages
- **WHEN** the scanner has successfully imported a message into a case
- **THEN** the scanner SHALL use `moveMessage(mailboxId, messageRef, targetFolderId)` to move the message to the "in Akte importiert" folder

### Requirement: Server-Side Mail Polling
The server SHALL periodically check all mailboxes for new messages and maintain per-mailbox state in RAM indicating whether new messages are available.

#### Scenario: Server polls mailboxes every 30 seconds
- **WHEN** 30 seconds have elapsed since the last poll
- **THEN** the server SHALL check each configured mailbox for new messages since the last poll
- **AND** the server SHALL use the appropriate backend (IMAP or Graph API) for each mailbox
- **AND** the server SHALL update an in-memory flag per mailbox indicating whether new messages were found

#### Scenario: Server resets new-message flag after client retrieval
- **WHEN** a client calls `listMessages()` for a mailbox
- **THEN** the server SHALL reset the new-message flag for that mailbox

### Requirement: Lightweight New-Message Check
The server SHALL provide a lightweight method for the client to check whether a mailbox has new messages, without fetching the messages themselves.

#### Scenario: Client checks for new messages
- **WHEN** a client calls `hasNewMessages(String mailboxId)`
- **THEN** the server SHALL return `true` if the server-side polling has detected new messages since the last `listMessages()` call for that mailbox, or `false` otherwise
- **AND** the method SHALL NOT trigger any IMAP or Graph API calls (reads from RAM only)

#### Scenario: Client uses new-message check to decide whether to refresh
- **WHEN** the client periodically calls `hasNewMessages(mailboxId)` and receives `true`
- **THEN** the client SHALL call `listMessages()` to refresh the folder view
- **WHEN** the client receives `false`
- **THEN** the client SHALL NOT call `listMessages()`

### Requirement: Test Connection
The server SHALL provide a method to verify that mail access works for a given mailbox configuration.

#### Scenario: Test connection for IMAP mailbox
- **WHEN** an administrator triggers a connection test for a standard mailbox
- **THEN** the server SHALL attempt to connect to the IMAP server with the stored credentials
- **AND** the server SHALL return success or a descriptive error message

#### Scenario: Test connection for O365 mailbox
- **WHEN** an administrator triggers a connection test for an O365 mailbox
- **THEN** the server SHALL attempt to acquire a token via Client Credentials Flow and list folders via Graph API
- **AND** the server SHALL return success or a descriptive error message
