## ADDED Requirements

### Requirement: Server-Side beA Authentication
The system SHALL provide server-side beA authentication via the `BeaService` EJB. The server SHALL identify the calling user via the EJB principal (`SessionContext.getCallerPrincipal()`), look up the user's beA certificate and password from `AppUserBean`, and forward these credentials to the beAstie REST API. The client SHALL NOT send any credentials - it simply calls `login()` with no parameters.

#### Scenario: Successful beA login
- **WHEN** the client calls `BeaServiceRemote.login()` and the current user has a valid beA certificate stored in `AppUserBean`
- **THEN** the server reads `beaCertificate` and `beaCertificatePassword` from the user's `AppUserBean`
- **AND** decrypts the password via `Crypto.decrypt()`
- **AND** authenticates against beAstie POST `/api/v1/auth/login`
- **AND** stores the beAstie bearer token in the `BeaSessionRegistry` keyed by the principal name
- **AND** returns a `BeaLoginResult` containing a list of available postboxes

#### Scenario: Failed beA login - no certificate configured
- **WHEN** the client calls `BeaServiceRemote.login()` and the current user has no beA certificate in `AppUserBean`
- **THEN** the server throws an appropriate error indicating no beA certificate is configured for the user

#### Scenario: Failed beA login - invalid certificate
- **WHEN** the client calls `BeaServiceRemote.login()` and the stored certificate is invalid or the password is wrong
- **THEN** the server returns an appropriate error indicating authentication failure from beAstie

#### Scenario: beAstie unreachable
- **WHEN** the client calls any BeaServiceRemote method and beAstie is not reachable
- **THEN** the server throws an exception with a clear error message indicating the beAstie service is unavailable

#### Scenario: Transparent session re-login on expiry
- **WHEN** a BeaServiceRemote method call results in a 401 response from beAstie (session expired)
- **THEN** the server SHALL automatically re-read the certificate and password from `AppUserBean`
- **AND** re-authenticate against beAstie
- **AND** retry the original request transparently
- **AND** the client SHALL NOT receive a session expiry error

### Requirement: Server-Side Postbox Operations
The system SHALL provide server-side access to beA postboxes. All postbox queries SHALL be delegated from `BeaService` to the beAstie REST API.

#### Scenario: List postboxes
- **WHEN** the client calls `BeaServiceRemote.getPostboxes()`
- **THEN** the server queries beAstie GET `/api/v1/postboxes` and returns a list of `BeaPostbox` DTOs

#### Scenario: Get specific postbox
- **WHEN** the client calls `BeaServiceRemote.getPostbox(safeId)`
- **THEN** the server queries beAstie GET `/api/v1/postboxes/{safeId}` and returns a `BeaPostbox` DTO

### Requirement: Server-Side Folder Operations
The system SHALL provide server-side access to beA folders including listing, creating, and deleting folders.

#### Scenario: List folders for a postbox
- **WHEN** the client calls `BeaServiceRemote.getFolders(safeId)`
- **THEN** the server queries beAstie GET `/api/v1/postboxes/{safeId}/folders` and returns a list of `BeaFolder` DTOs

#### Scenario: Create a new folder
- **WHEN** the client calls `BeaServiceRemote.createFolder(safeId, name, parentFolderId)`
- **THEN** the server creates the folder via beAstie POST `/api/v1/postboxes/{safeId}/folders`

#### Scenario: Delete a folder
- **WHEN** the client calls `BeaServiceRemote.deleteFolder(safeId, folderId)`
- **THEN** the server deletes the folder via beAstie DELETE `/api/v1/postboxes/{safeId}/folders/{folderId}`

### Requirement: Server-Side Message Operations
The system SHALL provide server-side access to beA messages including listing, reading, sending, deleting, moving, and draft management.

#### Scenario: List messages in a folder
- **WHEN** the client calls `BeaServiceRemote.getMessages(safeId, folderId)`
- **THEN** the server queries beAstie and returns a list of `BeaMessageHeader` DTOs

#### Scenario: Search messages with filter
- **WHEN** the client calls `BeaServiceRemote.searchMessages(safeId, folderId, filter)`
- **THEN** the server forwards the `BeaMessageFilter` (with paging, sorting, date ranges, name filters) to beAstie POST `.../messages/search` and returns matching `BeaMessageHeader` DTOs

#### Scenario: Get full message with attachments
- **WHEN** the client calls `BeaServiceRemote.getMessage(safeId, messageId)`
- **THEN** the server retrieves the full message via beAstie and returns a `BeaMessage` DTO including body, attachments, VHN attachments, and journal entries

#### Scenario: Send a message
- **WHEN** the client calls `BeaServiceRemote.sendMessage(safeId, request)` with a `BeaSendMessageRequest` containing recipientSafeId, subject, body, attachments, legalAuthorityCode, priorityCode, eebRequested, and confidential flags
- **THEN** the server sends the message via beAstie POST `/api/v1/postboxes/{safeId}/messages` and returns the sent `BeaMessage`

#### Scenario: Save a draft
- **WHEN** the client calls `BeaServiceRemote.saveDraft(safeId, request)` with a `BeaSaveDraftRequest`
- **THEN** the server saves the draft via beAstie and returns the draft message ID

#### Scenario: Delete a message (move to trash)
- **WHEN** the client calls `BeaServiceRemote.deleteMessage(safeId, messageId)`
- **THEN** the server moves the message to trash via beAstie DELETE

#### Scenario: Restore a message from trash
- **WHEN** the client calls `BeaServiceRemote.restoreMessage(safeId, messageId)`
- **THEN** the server restores the message via beAstie POST `.../restore`

#### Scenario: Move message to folder
- **WHEN** the client calls `BeaServiceRemote.moveMessage(safeId, messageId, targetFolderId)`
- **THEN** the server moves the message via beAstie PUT `.../messages/{messageId}/move` and returns success status

#### Scenario: Mark message as read
- **WHEN** the client calls `BeaServiceRemote.markMessageRead(safeId, messageId)`
- **THEN** the server marks the message as read via beAstie PUT `.../read`

#### Scenario: Check if message was read by specific identity
- **WHEN** the client calls `BeaServiceRemote.isMessageReadByIdentity(safeId, messageId, targetSafeId)`
- **THEN** the server queries beAstie GET `.../read/{targetSafeId}` and returns true or false

#### Scenario: Get message journal
- **WHEN** the client calls `BeaServiceRemote.getMessageJournal(safeId, messageId)`
- **THEN** the server queries beAstie GET `.../journal` and returns a list of `BeaMessageJournalEntry` DTOs

### Requirement: Server-Side Process Cards
The system SHALL provide server-side access to beA process cards (Laufzettel).

#### Scenario: Get process cards for a message
- **WHEN** the client calls `BeaServiceRemote.getProcessCards(safeId, messageId)`
- **THEN** the server queries beAstie GET `.../processcards` and returns a list of `BeaProcessCard` DTOs containing entries, OSCI message, and success status

### Requirement: Server-Side Message Export and Verification
The system SHALL provide server-side message export (XML) and signature verification.

#### Scenario: Export message as XML with process card wait
- **WHEN** the client calls `BeaServiceRemote.exportMessage(safeId, messageId, maxWaitForProcessCard)`
- **THEN** the server queries beAstie GET `.../export?maxWaitForProcessCard=...` and returns a `BeaMessageExport` containing the XML file

#### Scenario: Verify message signature
- **WHEN** the client calls `BeaServiceRemote.verifyMessage(safeId, messageId)`
- **THEN** the server returns a `BeaVerificationResult` with the verification status

### Requirement: Server-Side Identity Lookup and Search
The system SHALL provide server-side beA identity lookups by Safe-ID and multi-criteria search for finding recipients.

#### Scenario: Look up identity by Safe-ID
- **WHEN** the client calls `BeaServiceRemote.getIdentity(safeId)`
- **THEN** the server queries beAstie GET `/api/v1/identities/{safeId}` and returns a `BeaIdentity` DTO

#### Scenario: Look up identity with ZIP code filter
- **WHEN** the client calls `BeaServiceRemote.getIdentity(safeId, zipCode)`
- **THEN** the server queries beAstie GET `/api/v1/identities/{safeId}?zipCode=...` and returns a `BeaIdentity` DTO

#### Scenario: Search identities by criteria
- **WHEN** the client calls `BeaServiceRemote.searchIdentity(request)` with a `BeaIdentitySearchRequest` containing firstName, surName, userName, city, zipCode, officeName
- **THEN** the server queries beAstie POST `/api/v1/identities/search` and returns a list of matching `BeaIdentity` DTOs

### Requirement: Server-Side Legal Authority and Priority Lookup
The system SHALL provide server-side access to lists of legal authorities and message priorities for the send message dialog.

#### Scenario: List legal authorities
- **WHEN** the client calls `BeaServiceRemote.getLegalAuthorities(safeId)`
- **THEN** the server queries beAstie GET `/api/v1/postboxes/{safeId}/legal-authorities` and returns a list of `BeaListItem` DTOs

#### Scenario: List message priorities
- **WHEN** the client calls `BeaServiceRemote.getMessagePriorities(safeId)`
- **THEN** the server queries beAstie GET `/api/v1/postboxes/{safeId}/message-priorities` and returns a list of `BeaListItem` DTOs

### Requirement: Server-Side Certificate Information
The system SHALL provide the ability to retrieve certificate attributes from a beA certificate.

#### Scenario: Get certificate information
- **WHEN** the client calls `BeaServiceRemote.getCertificateInformation(certificate, password)`
- **THEN** the server queries beAstie POST `/api/v1/auth/certificate-info` and returns a Map of certificate attributes

### Requirement: Server-Side eEB (Elektronisches Empfangsbekenntnis) Operations
The system SHALL provide server-side eEB operations including checking, rendering, confirming, and rejecting eEB messages.

#### Scenario: Check if XML is eEB request
- **WHEN** the client calls `BeaServiceRemote.isEebRequest(xml)`
- **THEN** the server checks via beAstie and returns true or false

#### Scenario: Check if XML is eEB response
- **WHEN** the client calls `BeaServiceRemote.isEebResponse(xml)`
- **THEN** the server checks via beAstie and returns true or false

#### Scenario: Extract eEB request attributes
- **WHEN** the client calls `BeaServiceRemote.getEebRequestAttributes(xml)`
- **THEN** the server extracts attributes via beAstie and returns `BeaEebRequestAttributes`

#### Scenario: Render eEB as HTML
- **WHEN** the client calls `BeaServiceRemote.renderEebHtml(xmlRequest, xmlResponse)`
- **THEN** the server renders via beAstie and returns the HTML string

#### Scenario: Get eEB rejection reasons
- **WHEN** the client calls `BeaServiceRemote.getEebRejectionReasons()`
- **THEN** the server retrieves rejection reasons from beAstie and returns a list of `BeaListItem`

#### Scenario: Send eEB confirmation
- **WHEN** the client calls `BeaServiceRemote.sendEebConfirmation(safeId, ...)`
- **THEN** the server sends the confirmation via beAstie and returns the resulting `BeaMessage`

#### Scenario: Send eEB rejection
- **WHEN** the client calls `BeaServiceRemote.sendEebRejection(safeId, ...)`
- **THEN** the server sends the rejection via beAstie and returns the resulting `BeaMessage`

### Requirement: beAstie Connection Configuration
The system SHALL allow configuration of the beAstie REST API base URL via the existing `BeaConfigurationDialog` and `SERVERCONF_BEAENDPOINT` server setting. The existing endpoint text field SHALL be repurposed to hold the beAstie base URL.

#### Scenario: Configure beAstie URL via dialog
- **WHEN** the administrator opens `BeaConfigurationDialog` and enters a beAstie URL in the endpoint text field
- **THEN** the URL is saved to `ServerSettings.SERVERCONF_BEAENDPOINT`
- **AND** `BeaService` SHALL use this URL as the base for all beAstie REST API calls

#### Scenario: beAstie URL not configured
- **WHEN** the server setting `SERVERCONF_BEAENDPOINT` is not set
- **THEN** the `BeaService` SHALL use the default URL `http://localhost:7080`

### Requirement: Client-Side beA Delegation to Server
The j-lawyer desktop client SHALL delegate all beA operations to the server-side `BeaService` instead of calling the beA API directly. The `BeaAccess` class SHALL be refactored from using `BeaWrapper` locally to calling `BeaServiceRemote` via `JLawyerServiceLocator`. There SHALL be no fallback to direct BeaWrapper usage; the BeaWrapper and EHCache dependencies SHALL be removed from the client entirely.

#### Scenario: Client beA login delegates to server
- **WHEN** the user initiates a beA login in the desktop client
- **THEN** the client calls `BeaServiceRemote.login()` with no parameters
- **AND** the server resolves the user's certificate and password from `AppUserBean` via the EJB principal
- **AND** the client receives a `BeaLoginResult` with available postboxes

#### Scenario: Client beA operations use server
- **WHEN** the client performs any beA operation (list messages, send, etc.)
- **THEN** the operation is executed via `BeaServiceRemote` method calls
- **AND** the client no longer makes direct beA API calls via BeaWrapper

### Requirement: BeaWrapper Version Query
The system SHALL provide the ability to query the BeaWrapper library version from the server.

#### Scenario: Get BeaWrapper version
- **WHEN** the client calls `BeaServiceRemote.getBeaWrapperVersion()`
- **THEN** the server queries beAstie GET `/api/v1/auth/version` and returns the version string
