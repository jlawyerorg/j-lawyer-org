## ADDED Requirements

### Requirement: Mailbox Discovery
The REST API SHALL provide an endpoint to list all mailboxes accessible to the authenticated user.

#### Scenario: User lists their mailboxes
- **WHEN** a GET request is made to `/rest/v7/email/mailboxes`
- **THEN** the response SHALL contain a JSON array of mailbox objects
- **AND** each mailbox object SHALL include `id`, `displayName`, `emailAddress`, and `type` (IMAP or Exchange)
- **AND** only mailboxes the authenticated user has access to SHALL be returned

#### Scenario: User with no mailbox access
- **WHEN** a GET request is made to `/rest/v7/email/mailboxes` by a user with no mailbox access
- **THEN** the response SHALL return an empty JSON array

### Requirement: Folder Listing
The REST API SHALL provide an endpoint to list all mail folders for a given mailbox, including folder hierarchy and message counts.

#### Scenario: User lists folders for a mailbox
- **WHEN** a GET request is made to `/rest/v7/email/mailboxes/{mailboxId}/folders`
- **THEN** the response SHALL contain a JSON array of folder objects
- **AND** each folder SHALL include `folderId`, `parentFolderId`, `displayName`, `wellKnownName`, `unreadCount`, and `totalCount`

#### Scenario: User requests folders for unauthorized mailbox
- **WHEN** a GET request is made for a mailbox the user does not have access to
- **THEN** the response SHALL return HTTP 403 Forbidden

### Requirement: Folder Management
The REST API SHALL provide endpoints to create and delete mail folders, and to empty the trash folder.

#### Scenario: User creates a subfolder
- **WHEN** a POST request is made to `/rest/v7/email/mailboxes/{mailboxId}/folders` with `parentFolderId` and `folderName`
- **THEN** the response SHALL return the created folder as a JSON object with its assigned `folderId`

#### Scenario: User deletes a folder
- **WHEN** a DELETE request is made to `/rest/v7/email/mailboxes/{mailboxId}/folders/{folderId}`
- **THEN** the folder SHALL be deleted
- **AND** the response SHALL return HTTP 200

#### Scenario: User empties trash
- **WHEN** a DELETE request is made to `/rest/v7/email/mailboxes/{mailboxId}/folders/{folderId}/trash`
- **THEN** all messages in the trash folder SHALL be permanently deleted

### Requirement: Message Listing with Pagination and Filtering
The REST API SHALL provide an endpoint to list messages in a folder with pagination, date filtering, unread filtering, and search support.

#### Scenario: User lists messages with pagination
- **WHEN** a GET request is made to `/rest/v7/email/mailboxes/{mailboxId}/folders/{folderId}/messages` with query parameters `top` and `offset`
- **THEN** the response SHALL contain a JSON array of message summary objects limited by `top` and starting at `offset`
- **AND** each message summary SHALL include `messageRef`, `messageId`, `subject`, `from`, `to`, `cc`, `date`, `read`, and `hasAttachments`

#### Scenario: User filters messages by date and unread status
- **WHEN** query parameters `sinceDate` (ISO 8601 format) and `unreadOnly=true` are provided
- **THEN** only unread messages received after the given date SHALL be returned

#### Scenario: User searches messages
- **WHEN** a query parameter `search` is provided
- **THEN** only messages matching the search term in subject, from, to, or body SHALL be returned

### Requirement: Message Retrieval
The REST API SHALL provide an endpoint to retrieve the full content of a single message, including body and optional attachment metadata.

#### Scenario: User retrieves a message
- **WHEN** a GET request is made to `/rest/v7/email/mailboxes/{mailboxId}/messages/{messageRef}`
- **THEN** the response SHALL contain the full message as a JSON object
- **AND** the response SHALL include `body`, `bodyContentType`, and all header fields

#### Scenario: User retrieves a message with attachment metadata
- **WHEN** the query parameter `includeAttachments=true` is provided
- **THEN** the response SHALL include an `attachments` array with metadata for each attachment (`attachmentId`, `name`, `contentType`, `size`, `inline`)
- **AND** inline attachment content (CID images, calendar files) SHALL be included as Base64

### Requirement: Message Management
The REST API SHALL provide endpoints to move, delete, and update read status of messages.

#### Scenario: User moves a message to another folder
- **WHEN** a PUT request is made to `/rest/v7/email/mailboxes/{mailboxId}/messages/{messageRef}/move` with `targetFolderId` in the request body
- **THEN** the message SHALL be moved to the target folder

#### Scenario: User deletes a message
- **WHEN** a DELETE request is made to `/rest/v7/email/mailboxes/{mailboxId}/messages/{messageRef}`
- **THEN** the message SHALL be deleted

#### Scenario: User marks a message as read
- **WHEN** a PUT request is made to `/rest/v7/email/mailboxes/{mailboxId}/messages/{messageRef}/read` with `{"read": true}`
- **THEN** the message SHALL be marked as read

#### Scenario: User marks a message as unread
- **WHEN** a PUT request is made to `/rest/v7/email/mailboxes/{mailboxId}/messages/{messageRef}/read` with `{"read": false}`
- **THEN** the message SHALL be marked as unread

### Requirement: Attachment Download
The REST API SHALL provide an endpoint to download individual attachment content, supporting both JSON (Base64) and raw binary responses.

#### Scenario: User downloads attachment as JSON
- **WHEN** a GET request is made to `/rest/v7/email/mailboxes/{mailboxId}/messages/{messageRef}/attachments/{attachmentId}` with `Accept: application/json`
- **THEN** the response SHALL contain a JSON object with `name`, `contentType`, `size`, and `contentBase64` (Base64-encoded content)

#### Scenario: User downloads attachment as raw binary
- **WHEN** a GET request is made with `Accept: application/octet-stream` (or no specific Accept header)
- **THEN** the response SHALL return the raw attachment bytes with appropriate `Content-Type` and `Content-Disposition` headers

### Requirement: Send Email
The REST API SHALL provide an endpoint to compose and send an email through the mailbox's configured backend.

#### Scenario: User sends a plain text email
- **WHEN** a POST request is made to `/rest/v7/email/mailboxes/{mailboxId}/send` with `to`, `subject`, `body`, and `contentType: "text/plain"`
- **THEN** the email SHALL be sent via the mailbox's backend (SMTP or Graph API)
- **AND** the response SHALL return HTTP 200

#### Scenario: User sends an HTML email with attachments
- **WHEN** the request includes `contentType: "text/html"`, CC/BCC recipients, and an `attachments` array with Base64-encoded content
- **THEN** the email SHALL be sent with all specified recipients and attachments

#### Scenario: User sends a reply with threading headers
- **WHEN** the request includes `inReplyTo` and `references` fields
- **THEN** the sent email SHALL include proper `In-Reply-To` and `References` headers for threading

#### Scenario: User sends with priority and read receipt
- **WHEN** the request includes `priority: "high"` and `readReceipt: true`
- **THEN** the email SHALL be sent with priority headers and a read receipt request

### Requirement: Append to Folder
The REST API SHALL provide an endpoint to append a composed message to a specific folder (e.g. Sent, Drafts) without sending it.

#### Scenario: User appends a draft message
- **WHEN** a POST request is made to `/rest/v7/email/mailboxes/{mailboxId}/folders/{folderId}/append` with message data
- **THEN** the message SHALL be created in the specified folder
- **AND** the message read status SHALL match the `markAsRead` flag in the request

### Requirement: EML Export
The REST API SHALL provide an endpoint to download a message as a complete RFC 822 EML file.

#### Scenario: User downloads message as EML
- **WHEN** a GET request is made to `/rest/v7/email/mailboxes/{mailboxId}/messages/{messageRef}/eml`
- **THEN** the response SHALL return the EML bytes with `Content-Type: message/rfc822` and `Content-Disposition: attachment`

### Requirement: Connection Test
The REST API SHALL provide an endpoint to test the connection to a mailbox.

#### Scenario: User tests a working mailbox connection
- **WHEN** a GET request is made to `/rest/v7/email/mailboxes/{mailboxId}/test`
- **AND** the mailbox connection succeeds
- **THEN** the response SHALL return HTTP 200 with `{"success": true}`

#### Scenario: User tests a failing mailbox connection
- **WHEN** a GET request is made to `/rest/v7/email/mailboxes/{mailboxId}/test`
- **AND** the mailbox connection fails
- **THEN** the response SHALL return HTTP 200 with `{"success": false, "error": "descriptive error message"}`

### Requirement: Cache Management
The REST API SHALL provide an endpoint to invalidate server-side message caches for a mailbox.

#### Scenario: User invalidates mailbox cache
- **WHEN** a DELETE request is made to `/rest/v7/email/mailboxes/{mailboxId}/cache`
- **THEN** all server-side caches for the mailbox SHALL be invalidated
- **AND** the response SHALL return HTTP 200

### Requirement: Mailbox Access Authorization
The REST API SHALL verify that the authenticated user has access to the requested mailbox before executing any operation.

#### Scenario: Authorized user accesses mailbox
- **WHEN** a request is made for a mailbox the authenticated user has access to
- **THEN** the request SHALL be processed normally

#### Scenario: Unauthorized user accesses mailbox
- **WHEN** a request is made for a mailbox the authenticated user does NOT have access to
- **THEN** the response SHALL return HTTP 403 Forbidden
- **AND** no mailbox operation SHALL be executed
