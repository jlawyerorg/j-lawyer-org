## ADDED Requirements

### Requirement: Graph API Client Credentials Authentication
The server SHALL authenticate with Microsoft Graph API using OAuth 2.0 Client Credentials Flow, acquiring access tokens without user interaction using the existing `clientId`, `clientSecret`, and `tenantId` fields on `MailboxSetup`.

#### Scenario: Server acquires access token with valid credentials
- **WHEN** a mailbox has `msExchange=true` and valid `tenantId`, `clientId`, and `clientSecret`
- **THEN** the server SHALL request an access token from `https://login.microsoftonline.com/{tenantId}/oauth2/v2.0/token` with `grant_type=client_credentials` and `scope=https://graph.microsoft.com/.default`
- **AND** the server SHALL cache the access token in `authToken` with expiry in `tokenExpiry`

#### Scenario: Server refreshes token before expiry
- **WHEN** a cached access token is within 6 minutes of expiry
- **THEN** the server SHALL request a new access token using Client Credentials Flow
- **AND** the server SHALL NOT require a refresh token or user interaction

#### Scenario: Authentication fails with invalid credentials
- **WHEN** the `tenantId`, `clientId`, or `clientSecret` is invalid
- **THEN** the server SHALL log the error with the affected mailbox address
- **AND** the server SHALL NOT retry with the same credentials until they are updated

### Requirement: Graph API Mail Operations
The server SHALL implement the unified mail service interface for O365 mailboxes using Microsoft Graph API REST calls.

#### Scenario: List folders via Graph API
- **WHEN** the unified service delegates a `listFolders()` call for an O365 mailbox
- **THEN** the server SHALL call `GET /users/{email}/mailFolders` with the cached access token

#### Scenario: List messages via Graph API
- **WHEN** the unified service delegates a `listMessages()` call for an O365 mailbox
- **THEN** the server SHALL call `GET /users/{email}/mailFolders/{folderId}/messages` with appropriate `$select` and `$top` parameters

#### Scenario: Get message via Graph API
- **WHEN** the unified service delegates a `getMessage()` call for an O365 mailbox
- **THEN** the server SHALL call `GET /users/{email}/messages/{messageId}` and return the full message body and headers

#### Scenario: Get attachments via Graph API
- **WHEN** the unified service delegates a `getAttachments()` call for an O365 mailbox
- **THEN** the server SHALL call `GET /users/{email}/messages/{messageId}/attachments`
- **AND** for attachments larger than 3 MB, the server SHALL handle the download in chunks

#### Scenario: Send mail via Graph API
- **WHEN** the unified service delegates a `sendMail()` call for an O365 mailbox
- **THEN** the server SHALL call `POST /users/{email}/sendMail` with the message payload
- **AND** for attachments larger than 3 MB, the server SHALL use an upload session

#### Scenario: Send mail with priority via Graph API
- **WHEN** the unified service delegates a `sendMail()` call with a priority level
- **THEN** the server SHALL set the `importance` field in the Graph API request accordingly (`high`, `normal`, `low`)

#### Scenario: Move message via Graph API
- **WHEN** the unified service delegates a `moveMessage()` call for an O365 mailbox
- **THEN** the server SHALL call `POST /users/{email}/messages/{messageId}/move` with the destination folder ID

#### Scenario: Delete message via Graph API
- **WHEN** the unified service delegates a `deleteMessage()` call for an O365 mailbox
- **THEN** the server SHALL call `DELETE /users/{email}/messages/{messageId}`

#### Scenario: Mark message as read/unread via Graph API
- **WHEN** the unified service delegates a `markAsRead()` call for an O365 mailbox
- **THEN** the server SHALL call `PATCH /users/{email}/messages/{messageId}` with `{"isRead": true/false}`

### Requirement: Client Secret Expiry Warning
The system SHALL warn administrators when the Azure Client Secret is approaching expiry. Since the server cannot query Azure for the expiry date, the administrator SHALL enter the expiry date manually during mailbox configuration.

#### Scenario: Administrator enters Client Secret expiry date
- **WHEN** an administrator configures an O365 mailbox
- **THEN** the configuration dialog SHALL provide a date field for the Client Secret expiry date
- **AND** the expiry date SHALL be stored in `MailboxSetup`

#### Scenario: Client Secret expires within 30 days
- **WHEN** an administrator opens the mailbox configuration or the server detects an O365 mailbox whose stored Client Secret expiry date is within 30 days
- **THEN** the system SHALL display a warning indicating the upcoming expiry date
- **AND** the warning SHALL recommend renewing the Client Secret in the Azure portal

#### Scenario: Client Secret has expired
- **WHEN** a token acquisition fails for an O365 mailbox
- **THEN** the server SHALL log the error and indicate that the Client Secret may have expired
