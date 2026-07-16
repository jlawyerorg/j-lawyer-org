# email-rest-api delta

## ADDED Requirements

### Requirement: Folder Visibility Management
The REST API SHALL provide endpoints to hide and unhide individual folders per mailbox, to
list the hidden folders, and to fetch message counts for a single folder on demand. Hidden
state SHALL be stored per mailbox and SHALL be interoperable with the desktop client.

#### Scenario: User hides a folder
- **WHEN** a PUT request is made to `/rest/v7/email/mailboxes/{mailboxId}/folders/{folderId}/hidden` with `{"hidden": true}`
- **THEN** the folder SHALL be recorded as hidden for that mailbox
- **AND** its descendant folders SHALL be treated as hidden as well
- **AND** the response SHALL return HTTP 200

#### Scenario: User unhides a folder
- **WHEN** a PUT request is made to `/rest/v7/email/mailboxes/{mailboxId}/folders/{folderId}/hidden` with `{"hidden": false}`
- **THEN** the folder SHALL no longer be recorded as hidden for that mailbox

#### Scenario: User lists hidden folders
- **WHEN** a GET request is made to `/rest/v7/email/mailboxes/{mailboxId}/hidden-folders`
- **THEN** the response SHALL contain a JSON array of the folders the user has hidden for that mailbox

#### Scenario: Client fetches counts for a single folder
- **WHEN** a GET request is made to `/rest/v7/email/mailboxes/{mailboxId}/folders/{folderId}/counts`
- **THEN** the response SHALL contain `folderId`, `unreadCount`, and `totalCount` for that folder

#### Scenario: Hidden-folder state interoperates with the desktop client
- **WHEN** folders were hidden via the desktop client (or vice versa)
- **THEN** both clients SHALL interpret the same per-mailbox hidden-folder state

## MODIFIED Requirements

### Requirement: Folder Listing
The REST API SHALL provide an endpoint to list all mail folders for a given mailbox,
including folder hierarchy and message counts. The endpoint SHALL accept optional
`includeHidden` and `includeCounts` query parameters (both defaulting to `true`) so that
clients can obtain a fast, lightweight folder tree and defer per-folder counts.

#### Scenario: User lists folders for a mailbox
- **WHEN** a GET request is made to `/rest/v7/email/mailboxes/{mailboxId}/folders`
- **THEN** the response SHALL contain a JSON array of folder objects
- **AND** each folder SHALL include `folderId`, `parentFolderId`, `displayName`, `wellKnownName`, `unreadCount`, and `totalCount`

#### Scenario: Client excludes hidden folders
- **WHEN** the query parameter `includeHidden=false` is provided
- **THEN** folders the user has hidden for that mailbox (and their descendants) SHALL be omitted from the response
- **AND** with `includeHidden=true` or the parameter absent, all folders SHALL be returned (unchanged default behavior)

#### Scenario: Client defers per-folder counts for fast rendering
- **WHEN** the query parameter `includeCounts=false` is provided
- **THEN** the server SHALL NOT open each folder to compute counts, and SHALL return `unreadCount` and `totalCount` as `-1` (count-unknown) sentinels
- **AND** with `includeCounts=true` or the parameter absent, real counts SHALL be returned (unchanged default behavior)

#### Scenario: User requests folders for unauthorized mailbox
- **WHEN** a GET request is made for a mailbox the user does not have access to
- **THEN** the response SHALL return HTTP 403 Forbidden

### Requirement: Message Listing with Pagination and Filtering
The REST API SHALL provide an endpoint to list messages in a folder with pagination, date
filtering, unread filtering, and search support. The `offset` parameter SHALL page through
results consistently whether or not date/unread/search filters are applied, enabling
infinite-scroll clients.

#### Scenario: User lists messages with pagination
- **WHEN** a GET request is made to `/rest/v7/email/mailboxes/{mailboxId}/folders/{folderId}/messages` with query parameters `top` and `offset`
- **THEN** the response SHALL contain a JSON array of message summary objects limited by `top` and starting at `offset`, ordered newest first
- **AND** each message summary SHALL include `messageRef`, `messageId`, `subject`, `from`, `to`, `cc`, `date`, `read`, and `hasAttachments`

#### Scenario: User filters messages by date and unread status
- **WHEN** query parameters `sinceDate` (ISO 8601 format) and `unreadOnly=true` are provided
- **THEN** only unread messages received after the given date SHALL be returned

#### Scenario: User searches messages
- **WHEN** a query parameter `search` is provided
- **THEN** only messages matching the search term in subject, from, to, or body SHALL be returned

#### Scenario: Pagination applies within filtered results
- **WHEN** a filter (`search`, `unreadOnly`, or `sinceDate`) is combined with a non-zero `offset`
- **THEN** the response SHALL return the page of filtered results at that offset, windowed from the newest match, without overlap or gaps between consecutive pages
- **AND** for `offset=0` the result SHALL be identical to the previous behavior (the newest `top` matches), so existing clients are unaffected
