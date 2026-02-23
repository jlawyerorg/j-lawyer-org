## ADDED Requirements

### Requirement: beA REST API Authentication Endpoints
The system SHALL expose beA authentication operations as REST endpoints under `/rest/v8/bea/`. The endpoints SHALL delegate to `BeaServiceLocal` and require `loginRole` authorization. The EJB security context SHALL propagate the HTTP Basic Auth principal to `BeaService`.

#### Scenario: Login via REST
- **WHEN** a client sends `POST /rest/v8/bea/login`
- **THEN** the server calls `BeaServiceLocal.login()` using the HTTP Basic Auth principal
- **AND** returns HTTP 200 with a `BeaLoginResult` JSON containing available postboxes

#### Scenario: Logout via REST
- **WHEN** a client sends `POST /rest/v8/bea/logout`
- **THEN** the server calls `BeaServiceLocal.logout()` and returns HTTP 200

#### Scenario: Get beAstie version via REST
- **WHEN** a client sends `GET /rest/v8/bea/version`
- **THEN** the server returns the beAstie version string as JSON

#### Scenario: Get certificate information via REST
- **WHEN** a client sends `POST /rest/v8/bea/certificate-info` with a `BeaCertificateInfoRequest` JSON body
- **THEN** the server returns a JSON map of certificate attribute names to values

#### Scenario: REST beA call fails
- **WHEN** any beA REST endpoint call throws an exception from BeaServiceLocal
- **THEN** the server returns HTTP 500 with an error response

### Requirement: beA REST API Postbox Endpoints
The system SHALL expose beA postbox operations as REST endpoints under `/rest/v8/bea/postboxes/`.

#### Scenario: List postboxes via REST
- **WHEN** a client sends `GET /rest/v8/bea/postboxes`
- **THEN** the server returns a JSON array of `BeaPostbox` objects

#### Scenario: Get postbox details via REST
- **WHEN** a client sends `GET /rest/v8/bea/postboxes/{safeId}`
- **THEN** the server returns a single `BeaPostbox` JSON object

#### Scenario: Check outbox empty via REST
- **WHEN** a client sends `GET /rest/v8/bea/postboxes/{safeId}/outbox/empty`
- **THEN** the server returns a JSON boolean

#### Scenario: Check EGVP postbox via REST
- **WHEN** a client sends `GET /rest/v8/bea/postboxes/{safeId}/egvp` with optional query parameter `userName`
- **THEN** the server returns a JSON boolean indicating whether the identity is an EGVP postbox

### Requirement: beA REST API Folder Endpoints
The system SHALL expose beA folder operations as REST endpoints under `/rest/v8/bea/postboxes/{safeId}/folders/`.

#### Scenario: List folders via REST
- **WHEN** a client sends `GET /rest/v8/bea/postboxes/{safeId}/folders`
- **THEN** the server returns a JSON array of `BeaFolder` objects

#### Scenario: Get folder details via REST
- **WHEN** a client sends `GET /rest/v8/bea/postboxes/{safeId}/folders/{folderId}`
- **THEN** the server returns a single `BeaFolder` JSON object

#### Scenario: Create folder via REST
- **WHEN** a client sends `POST /rest/v8/bea/postboxes/{safeId}/folders` with JSON body containing `name` and optional `parentFolderId`
- **THEN** the server creates the folder and returns the created `BeaFolder` JSON object

#### Scenario: Delete folder via REST
- **WHEN** a client sends `DELETE /rest/v8/bea/postboxes/{safeId}/folders/{folderId}`
- **THEN** the server deletes the folder and returns HTTP 200

### Requirement: beA REST API Message Endpoints
The system SHALL expose beA message operations as REST endpoints. Message listing and search SHALL be scoped under folders; individual message operations SHALL be scoped under postboxes.

#### Scenario: List messages in folder via REST
- **WHEN** a client sends `GET /rest/v8/bea/postboxes/{safeId}/folders/{folderId}/messages`
- **THEN** the server returns a JSON array of `BeaMessageHeader` objects

#### Scenario: Search messages in folder via REST
- **WHEN** a client sends `POST /rest/v8/bea/postboxes/{safeId}/folders/{folderId}/messages/search` with a `BeaMessageFilter` JSON body
- **THEN** the server returns a JSON array of matching `BeaMessageHeader` objects

#### Scenario: List message IDs in folder via REST
- **WHEN** a client sends `GET /rest/v8/bea/postboxes/{safeId}/folders/{folderId}/messageids`
- **THEN** the server returns a JSON array of message ID strings

#### Scenario: Search message IDs in folder via REST
- **WHEN** a client sends `POST /rest/v8/bea/postboxes/{safeId}/folders/{folderId}/messageids/search` with a `BeaMessageFilter` JSON body
- **THEN** the server returns a JSON array of matching message ID strings

#### Scenario: Get full message via REST
- **WHEN** a client sends `GET /rest/v8/bea/postboxes/{safeId}/messages/{messageId}`
- **THEN** the server returns a full `BeaMessage` JSON object including body and attachments

#### Scenario: Get message header via REST
- **WHEN** a client sends `GET /rest/v8/bea/postboxes/{safeId}/messages/{messageId}/header`
- **THEN** the server returns a `BeaMessageHeader` JSON object without attachments

#### Scenario: Send message via REST
- **WHEN** a client sends `POST /rest/v8/bea/postboxes/{safeId}/messages` with a `BeaSendMessageRequest` JSON body
- **THEN** the server sends the message and returns the sent `BeaMessage` JSON object

#### Scenario: Save draft via REST
- **WHEN** a client sends `POST /rest/v8/bea/postboxes/{safeId}/messages/draft` with a `BeaSaveDraftRequest` JSON body
- **THEN** the server saves the draft and returns the draft message ID as JSON string

#### Scenario: Delete message via REST
- **WHEN** a client sends `DELETE /rest/v8/bea/postboxes/{safeId}/messages/{messageId}`
- **THEN** the server deletes (trashes) the message and returns HTTP 200

#### Scenario: Restore message via REST
- **WHEN** a client sends `POST /rest/v8/bea/postboxes/{safeId}/messages/{messageId}/restore`
- **THEN** the server restores the message from trash and returns a JSON boolean

#### Scenario: Move message via REST
- **WHEN** a client sends `PUT /rest/v8/bea/postboxes/{safeId}/messages/{messageId}/move` with a `BeaMoveMessageRequest` JSON body containing `targetFolderId`
- **THEN** the server moves the message and returns a JSON boolean

#### Scenario: Mark message as read via REST
- **WHEN** a client sends `PUT /rest/v8/bea/postboxes/{safeId}/messages/{messageId}/read`
- **THEN** the server marks the message as read and returns a JSON boolean

#### Scenario: Check if message read by identity via REST
- **WHEN** a client sends `GET /rest/v8/bea/postboxes/{safeId}/messages/{messageId}/read/{targetSafeId}`
- **THEN** the server returns a JSON boolean

#### Scenario: Get message journal via REST
- **WHEN** a client sends `GET /rest/v8/bea/postboxes/{safeId}/messages/{messageId}/journal`
- **THEN** the server returns a JSON array of `BeaMessageJournalEntry` objects

#### Scenario: Get process cards via REST
- **WHEN** a client sends `GET /rest/v8/bea/postboxes/{safeId}/messages/{messageId}/processcards`
- **THEN** the server returns a JSON array of `BeaProcessCard` objects

#### Scenario: Verify message via REST
- **WHEN** a client sends `GET /rest/v8/bea/postboxes/{safeId}/messages/{messageId}/verify`
- **THEN** the server returns a `BeaVerificationResult` JSON object

### Requirement: beA REST API Identity Endpoints
The system SHALL expose beA identity lookup and search as REST endpoints under `/rest/v8/bea/identities/`.

#### Scenario: Get identity by Safe-ID via REST
- **WHEN** a client sends `GET /rest/v8/bea/identities/{safeId}`
- **THEN** the server returns a `BeaIdentity` JSON object

#### Scenario: Get identity with ZIP code filter via REST
- **WHEN** a client sends `GET /rest/v8/bea/identities/{safeId}?zipCode={zipCode}`
- **THEN** the server returns a `BeaIdentity` JSON object filtered by ZIP code

#### Scenario: Search identities via REST
- **WHEN** a client sends `POST /rest/v8/bea/identities/search` with a `BeaIdentitySearchRequest` JSON body
- **THEN** the server returns a JSON array of matching `BeaIdentity` objects

### Requirement: beA REST API Reference Data Endpoints
The system SHALL expose beA reference data (legal authorities, message priorities) as REST endpoints.

#### Scenario: List legal authorities via REST
- **WHEN** a client sends `GET /rest/v8/bea/legal-authorities`
- **THEN** the server returns a JSON array of `BeaListItem` objects

#### Scenario: Get default legal authority via REST
- **WHEN** a client sends `GET /rest/v8/bea/legal-authorities/default`
- **THEN** the server returns a single `BeaListItem` JSON object

#### Scenario: List message priorities via REST
- **WHEN** a client sends `GET /rest/v8/bea/message-priorities`
- **THEN** the server returns a JSON array of `BeaListItem` objects

### Requirement: beA REST API eEB Endpoints
The system SHALL expose eEB (elektronisches Empfangsbekenntnis) operations as REST endpoints.

#### Scenario: Check eEB request via REST
- **WHEN** a client sends `POST /rest/v8/bea/eeb/check-request` with XML string in JSON body
- **THEN** the server returns a JSON boolean

#### Scenario: Check eEB response via REST
- **WHEN** a client sends `POST /rest/v8/bea/eeb/check-response` with XML string in JSON body
- **THEN** the server returns a JSON boolean

#### Scenario: Get eEB request attributes via REST
- **WHEN** a client sends `POST /rest/v8/bea/eeb/request-attributes` with XML string in JSON body
- **THEN** the server returns a `BeaEebRequestAttributes` JSON object

#### Scenario: Get eEB response attributes via REST
- **WHEN** a client sends `POST /rest/v8/bea/eeb/response-attributes` with XML string in JSON body
- **THEN** the server returns a `BeaEebResponseAttributes` JSON object

#### Scenario: Render eEB as HTML via REST
- **WHEN** a client sends `POST /rest/v8/bea/eeb/render-html` with JSON body containing `xmlRequest` and `xmlResponse` strings
- **THEN** the server returns the rendered HTML string

#### Scenario: Get eEB rejection reasons via REST
- **WHEN** a client sends `GET /rest/v8/bea/eeb/rejection-reasons`
- **THEN** the server returns a JSON array of `BeaListItem` objects

#### Scenario: Send eEB confirmation via REST
- **WHEN** a client sends `POST /rest/v8/bea/postboxes/{safeId}/messages/{messageId}/eeb/confirm` with JSON body containing `senderSafeId`, `recipientSafeId`, and `abgabeDate`
- **THEN** the server sends the eEB confirmation and returns the resulting `BeaMessage` JSON object

#### Scenario: Send eEB rejection via REST
- **WHEN** a client sends `POST /rest/v8/bea/postboxes/{safeId}/messages/{messageId}/eeb/reject` with JSON body containing `senderSafeId`, `recipientSafeId`, `code`, and optional `comment`
- **THEN** the server sends the eEB rejection and returns the resulting `BeaMessage` JSON object
