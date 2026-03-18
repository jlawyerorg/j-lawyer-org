## MODIFIED Requirements

### Requirement: Email Priority Selection
The email send dialog SHALL provide a priority selection control allowing users to set the importance level of outgoing emails.

#### Scenario: User selects high priority for urgent email
**Given** a user is composing an email in the SendEmailFrame dialog
**When** the user selects "Hoch (High)" from the priority dropdown
**And** the user sends the email
**Then** the outgoing email SHALL be submitted to the server with priority `high`
**And** the server SHALL apply appropriate priority headers or fields depending on the backend

#### Scenario: User sends email with normal priority (default)
**Given** a user is composing an email in the SendEmailFrame dialog
**When** the user has not changed the priority selection (defaults to "Normal")
**And** the user sends the email
**Then** the outgoing email SHALL be submitted to the server with priority `normal`

#### Scenario: User selects low priority for non-urgent email
**Given** a user is composing an email in the SendEmailFrame dialog
**When** the user selects "Niedrig (Low)" from the priority dropdown
**And** the user sends the email
**Then** the outgoing email SHALL be submitted to the server with priority `low`

## ADDED Requirements

### Requirement: O365 Mailbox Configuration UI
The mailbox configuration dialog SHALL provide a simplified setup for O365 mailboxes requiring only Tenant-ID, Client-ID, and Client-Secret.

#### Scenario: Administrator configures O365 mailbox
**Given** an administrator opens the mailbox configuration dialog
**When** the administrator enables O365/Exchange mode (`msExchange`)
**Then** the dialog SHALL display input fields for Tenant-ID, Client-ID, and Client-Secret
**And** the dialog SHALL NOT display a Device Code authentication flow
**And** the dialog SHALL NOT require IMAP/SMTP server settings for this mailbox

### Requirement: Unified Client Mail Access
The desktop client SHALL use the server-side unified mail service (`EmailServiceRemote`) for all mail operations, regardless of mailbox type. The client SHALL NOT establish direct IMAP or SMTP connections.

#### Scenario: Client loads folder list from server
**Given** a user selects any mailbox (IMAP or O365)
**When** the email inbox panel loads
**Then** the client SHALL call `listFolders(mailboxId)` via EJB remote call
**And** the client SHALL display the returned `MailFolderDTO` list
**And** the client SHALL NOT open an IMAP connection

#### Scenario: Client loads messages from server
**Given** a user selects a folder in any mailbox
**When** the folder contents are loaded
**Then** the client SHALL call `listMessages(mailboxId, folderId, top)` via EJB remote call
**And** the client SHALL display the returned `MailMessageDTO` list
**And** `MessageContainer` SHALL wrap `MailMessageDTO` instead of `javax.mail.Message`

#### Scenario: Client sends email via server
**Given** a user sends an email from any mailbox
**When** the send action is triggered
**Then** the client SHALL call `sendMail(mailboxId, ...)` via EJB remote call
**And** the client SHALL NOT establish a direct SMTP connection

#### Scenario: Client deletes a message via server
**Given** a user deletes a message in any mailbox
**When** the delete action is triggered
**Then** the client SHALL call `deleteMessage(mailboxId, messageRef)` via EJB remote call using the opaque message reference from `MailMessageDTO`

#### Scenario: Client moves a message via server
**Given** a user moves a message to another folder in any mailbox
**When** the move action is triggered
**Then** the client SHALL call `moveMessage(mailboxId, messageRef, targetFolderId)` via EJB remote call

#### Scenario: Client marks a message as read or unread via server
**Given** a user opens or marks a message in any mailbox
**When** the read status changes
**Then** the client SHALL call `markAsRead(mailboxId, messageRef, read)` via EJB remote call

### Requirement: Reply and Forward with Threading Headers
The client SHALL set `In-Reply-To` and `References` headers when replying to or forwarding messages, using the `messageId`, `inReplyTo`, and `references` fields from `MailMessageDTO`.

#### Scenario: User replies to a message
**Given** a user replies to a message
**When** the reply is sent via `sendMail()`
**Then** the client SHALL pass the original message's `messageId` as the `inReplyTo` parameter
**And** the client SHALL pass the original message's `references` concatenated with its `messageId` as the `references` parameter

#### Scenario: User forwards a message
**Given** a user forwards a message
**When** the forward is sent via `sendMail()`
**Then** the client SHALL pass the original message's `messageId` as the `inReplyTo` parameter

### Requirement: Inline Image Display
The client SHALL resolve CID-referenced inline images in HTML email bodies using the `contentId` field from `MailAttachmentDTO`.

#### Scenario: HTML email body contains CID references
**Given** a user views an HTML email with inline images
**When** the message body and attachments are loaded from the server
**Then** the client SHALL match `cid:` references in the HTML body to `MailAttachmentDTO` entries with matching `contentId`
**And** the client SHALL render the inline images in the HTML view
