## 1. Server-Side DTOs
- [ ] 1.1 Create serializable DTO classes in j-lawyer-server-api under `com.jdimension.jlawyer.services.bea.rest`:
  - `BeaLoginResult` (postboxes list)
  - `BeaPostbox` (safeId, userName, displayName, firstName, surName, title, zipCode, city, street, streetNumber, federalState, country, email, phone, mobile, fax, status, chamber, chamberMemberShipId, type, organization, organizationExtension, officeName)
  - `BeaFolder` (id, parentId, name, type, unreadMessageCount, safeId)
  - `BeaMessageHeader` (id, subject, sender, receptionTime, sentTime, permanentDeletion, referenceJustice, referenceNumber, confidential, signed, postBoxSafeId, recipientName, recipientSafeId, read, folderId)
  - `BeaMessage` (id, subject, body, receptionTime, createdTime, senderSafeId, senderName, referenceJustice, referenceNumber, confidential, signed, read, eebRequested, eebId, eebForeignId, messageType, recipients, attachments, vhnAttachments, journal, verificationHtml, verificationXml, verificationStatus, sentStatus, sentStatusDescription)
  - `BeaAttachment` (name, alias, type, size, content as byte[], technicalAttachment)
  - `BeaRecipient` (name, safeId)
  - `BeaIdentity` (safeId, firstName, surName, userName, zipCode, city, status, chamber, chamberMemberShipId, type, title, street, streetNumber, federalState, country, email, phone, mobile, fax, organization, organizationExtension, officeName, displayName)
  - `BeaMessageJournalEntry` (journalType, eventType, timestamp, messageId, fromSafeId, fromUsername, fromSurnameFirstname, postBoxSafeId, toSafeId, toUsername, toSurnameFirstname, attachmentReference)
  - `BeaVerificationResult` (html, xml, status)
  - `BeaMessageExport` (fileName, content as byte[])
  - `BeaProcessCard` (messageId, entries as List<BeaProcessCardEntry>, osciMessage, exceptionMessage, success)
  - `BeaProcessCardEntry` (code, text)
  - `BeaEebRequestAttributes` (sender, recipient, reference, documentNames)
  - `BeaEebResponseAttributes` (code, description, messageIds, dates)
  - `BeaListItem` (code, name)
  - `BeaSendMessageRequest` (recipientSafeId, subject, body, caseReference, attachments, legalAuthorityCode, priorityCode, eebRequested, confidential)
  - `BeaSaveDraftRequest` (subject, body, referenceNumber, referenceJustice, recipientSafeId, attachments, eebRequested, confidential, legalAuthorityCode, priorityCode, messageType)
  - `BeaMessageFilter` (onlyNew, limit, offset, sortDirection, sortCriterion, sentFrom/To, receivedFrom/To, deliveredFrom/To, senderNameContains, recipientNameContains)
  - `BeaMoveMessageRequest` (targetFolderId)
  - `BeaIdentitySearchRequest` (firstName, surName, userName, city, zipCode, officeName)
  - `BeaCertificateInfoRequest` (certificate as byte[], password)

## 2. Server-Side Interfaces
- [ ] 2.1 Define `BeaServiceRemote` interface with all beA operations (no sessionToken params - server resolves user via EJB principal and manages beAstie tokens internally):
  **Auth & Session:**
  - `BeaLoginResult login()` - authenticate current user with beAstie (reads certificate/password from AppUserBean via principal)
  - `void logout()` - invalidate current user's beAstie session
  - `String getBeaWrapperVersion()` - get BeaWrapper version info
  - `Map<String, String> getCertificateInformation(byte[] certificate, String password)` - get certificate attributes
  **Postboxes:**
  - `List<BeaPostbox> getPostboxes()` - list postboxes
  - `BeaPostbox getPostbox(String safeId)` - get postbox details
  **Folders:**
  - `List<BeaFolder> getFolders(String safeId)` - list folders
  - `BeaFolder getFolder(String safeId, long folderId)` - get folder details
  - `BeaFolder createFolder(String safeId, String name, Long parentFolderId)` - create folder
  - `void deleteFolder(String safeId, long folderId)` - delete folder
  - `boolean isOutboxEmpty(String safeId)` - check outbox
  **Messages:**
  - `List<BeaMessageHeader> getMessages(String safeId, long folderId)` - list messages in folder
  - `List<BeaMessageHeader> searchMessages(String safeId, long folderId, BeaMessageFilter filter)` - search/filter messages with paging
  - `BeaMessage getMessage(String safeId, String messageId)` - get full message with attachments
  - `BeaMessage sendMessage(String safeId, BeaSendMessageRequest request)` - send message
  - `String saveDraft(String safeId, BeaSaveDraftRequest request)` - save draft
  - `void deleteMessage(String safeId, String messageId)` - move to trash
  - `boolean restoreMessage(String safeId, String messageId)` - restore from trash
  - `boolean moveMessage(String safeId, String messageId, long targetFolderId)` - move message to folder
  - `boolean markMessageRead(String safeId, String messageId)` - mark as read
  - `boolean isMessageReadByIdentity(String safeId, String messageId, String targetSafeId)` - check if read by identity
  - `List<BeaMessageJournalEntry> getMessageJournal(String safeId, String messageId)` - get message journal
  - `List<BeaProcessCard> getProcessCards(String safeId, String messageId)` - get process cards (Laufzettel)
  - `BeaMessageExport exportMessage(String safeId, String messageId, long maxWaitForProcessCard)` - export as XML with process card wait
  - `BeaVerificationResult verifyMessage(String safeId, String messageId)` - verify signature
  **Identity:**
  - `BeaIdentity getIdentity(String safeId)` - get identity by Safe-ID
  - `BeaIdentity getIdentity(String safeId, String zipCode)` - get identity with ZIP filter
  - `List<BeaIdentity> searchIdentity(BeaIdentitySearchRequest request)` - search identities by criteria
  **Legal Authorities & Priorities:**
  - `List<BeaListItem> getLegalAuthorities(String safeId)` - list legal authorities for send dialog
  - `List<BeaListItem> getMessagePriorities(String safeId)` - list message priorities for send dialog
  **eEB:**
  - `boolean isEebRequest(String xml)` - check eEB request
  - `boolean isEebResponse(String xml)` - check eEB response
  - `BeaEebRequestAttributes getEebRequestAttributes(String xml)` - extract eEB request attrs
  - `BeaEebResponseAttributes getEebResponseAttributes(String xml)` - extract eEB response attrs
  - `String renderEebHtml(String xmlRequest, String xmlResponse)` - render eEB HTML
  - `List<BeaListItem> getEebRejectionReasons()` - rejection reasons
  - `BeaMessage sendEebConfirmation(String safeId, long messageId, String senderSafeId, String recipientSafeId, String abgabeDate)` - send eEB confirmation
  - `BeaMessage sendEebRejection(String safeId, long messageId, String senderSafeId, String recipientSafeId, String code, String comment)` - send eEB rejection
- [ ] 2.2 Define `BeaServiceLocal` interface (mirror of Remote for inter-bean calls)

## 3. Server-Side Implementation
- [ ] 3.1 Repurpose existing `ServerSettings.SERVERCONF_BEAENDPOINT` to hold the beAstie base URL (default: `http://localhost:7080`). No new setting key needed.
- [ ] 3.2 Implement `BeaService` EJB:
  - `@Resource SessionContext context` for identifying the calling user via `context.getCallerPrincipal().getName()`
  - Inject `AppUserBeanFacadeLocal` to look up `AppUserBean` and read `beaCertificate` + `beaCertificatePassword`
  - Decrypt `beaCertificatePassword` via `Crypto.decrypt()` before sending to beAstie
  - HTTP client setup (java.net.http.HttpClient)
  - JSON serialization/deserialization (Jackson via WildFly)
  - Helper methods for GET/POST/PUT/DELETE to beAstie
  - Authentication header management (Bearer token)
  - X-Client-Id header (value: "j-lawyer-org")
  - Error handling: map beAstie error responses to meaningful exceptions
  - **Transparent auto-re-login**: On 401 response, re-read certificate/password from AppUserBean, call `/api/v1/auth/login` again, and retry the failed request
- [ ] 3.3 Implement `@Singleton BeaSessionRegistry` bean:
  - Maps j-lawyer principal name -> beAstie bearer token
  - Provides `getToken(String principal)`, `setToken(String principal, String token)`, `removeToken(String principal)`
  - Thread-safe (ConcurrentHashMap)
- [ ] 3.4 Implement all interface methods in `BeaService`, each mapping to corresponding beAstie endpoint:
  **Auth & Session:**
  - `login()` -> POST `/api/v1/auth/login`
  - `logout()` -> POST `/api/v1/auth/logout`
  - `getBeaWrapperVersion()` -> GET `/api/v1/auth/version`
  - `getCertificateInformation()` -> POST `/api/v1/auth/certificate-info`
  **Postboxes:**
  - `getPostboxes()` -> GET `/api/v1/postboxes`
  - `getPostbox()` -> GET `/api/v1/postboxes/{safeId}`
  **Folders:**
  - `getFolders()` -> GET `/api/v1/postboxes/{safeId}/folders`
  - `getFolder()` -> GET `/api/v1/postboxes/{safeId}/folders/{folderId}`
  - `createFolder()` -> POST `/api/v1/postboxes/{safeId}/folders`
  - `deleteFolder()` -> DELETE `/api/v1/postboxes/{safeId}/folders/{folderId}`
  - `isOutboxEmpty()` -> GET `/api/v1/postboxes/{safeId}/folders/outbox/empty`
  **Messages:**
  - `getMessages()` -> GET `/api/v1/postboxes/{safeId}/folders/{folderId}/messages`
  - `searchMessages()` -> POST `/api/v1/postboxes/{safeId}/folders/{folderId}/messages/search`
  - `getMessage()` -> GET `/api/v1/postboxes/{safeId}/messages/{messageId}`
  - `sendMessage()` -> POST `/api/v1/postboxes/{safeId}/messages`
  - `saveDraft()` -> POST `/api/v1/postboxes/{safeId}/drafts`
  - `deleteMessage()` -> DELETE `/api/v1/postboxes/{safeId}/messages/{messageId}`
  - `restoreMessage()` -> POST `/api/v1/postboxes/{safeId}/messages/{messageId}/restore`
  - `moveMessage()` -> PUT `/api/v1/postboxes/{safeId}/messages/{messageId}/move`
  - `markMessageRead()` -> PUT `/api/v1/postboxes/{safeId}/messages/{messageId}/read`
  - `isMessageReadByIdentity()` -> GET `/api/v1/postboxes/{safeId}/messages/{messageId}/read/{targetSafeId}`
  - `getMessageJournal()` -> GET `/api/v1/postboxes/{safeId}/messages/{messageId}/journal`
  - `getProcessCards()` -> GET `/api/v1/postboxes/{safeId}/messages/{messageId}/processcards`
  - `exportMessage()` -> GET `/api/v1/postboxes/{safeId}/messages/{messageId}/export?maxWaitForProcessCard=...`
  - `verifyMessage()` -> GET `/api/v1/postboxes/{safeId}/messages/{messageId}/verify`
  **Identity:**
  - `getIdentity()` -> GET `/api/v1/identities/{safeId}` (optional query: `?zipCode=...`)
  - `searchIdentity()` -> POST `/api/v1/identities/search`
  **Legal Authorities & Priorities:**
  - `getLegalAuthorities()` -> GET `/api/v1/postboxes/{safeId}/legal-authorities`
  - `getMessagePriorities()` -> GET `/api/v1/postboxes/{safeId}/message-priorities`
  **eEB:**
  - `isEebRequest()` -> POST `/api/v1/eeb/check-request`
  - `isEebResponse()` -> POST `/api/v1/eeb/check-response`
  - `getEebRequestAttributes()` -> POST `/api/v1/eeb/request-attributes`
  - `getEebResponseAttributes()` -> POST `/api/v1/eeb/response-attributes`
  - `renderEebHtml()` -> POST `/api/v1/eeb/html`
  - `getEebRejectionReasons()` -> GET `/api/v1/eeb/rejection-reasons`
  - `sendEebConfirmation()` -> POST `/api/v1/postboxes/{safeId}/eeb/confirm`
  - `sendEebRejection()` -> POST `/api/v1/postboxes/{safeId}/eeb/reject`
- [ ] 3.5 Register BeaService in JLawyerServiceLocator for client access

## 4. Client-Side Refactoring
- [ ] 4.1 Add BeaServiceRemote lookup to `JLawyerServiceLocator`
- [ ] 4.2 Refactor `BeaAccess` to delegate all operations to `BeaServiceRemote`:
  - Replace BeaWrapper calls with BeaServiceRemote calls
  - `login()` takes no parameters - server resolves credentials from the authenticated EJB principal
  - Remove all local certificate/password handling
  - Remove EHCache usage
  - Keep the singleton pattern for UI convenience
- [ ] 4.3 Adapt `BeaLoginDialog` / `BeaLoginPanel`:
  - Login simply calls `BeaServiceRemote.login()` (no certificate/password to pass)
  - Remove local BeaWrapper instantiation and certificate loading
- [ ] 4.4 Adapt `SendBeaMessageAction`, `SaveBeaMessageAction`:
  - Use DTOs instead of BeaWrapper model classes
- [ ] 4.5 Adapt `LoadBeaFolderAction`, `BeaInboxPanel`:
  - Use DTOs instead of BeaWrapper model classes
- [ ] 4.6 Adapt `ViewBeaDialog`, `BeaMessageContentUI`:
  - Use DTOs for message display
- [ ] 4.7 Adapt `BeaCheckTimerTask`:
  - Poll via BeaServiceRemote instead of local BeaWrapper
- [ ] 4.8 Adapt `BeaIdentitySearchDialog`, `BeaIdentitySearchThread`:
  - Use BeaServiceRemote for identity lookups
- [ ] 4.9 Adapt `BeaEebReplyPanel`, `BeaEebDisplayDialog`, `EebRejectDialog`:
  - Use DTOs for eEB operations
- [ ] 4.10 Adapt `BeaSignaturesVerificationDialog`:
  - Use BeaServiceRemote for signature verification
- [ ] 4.11 Adapt `BeaEntryProcessor` (case import):
  - Use DTOs instead of BeaWrapper model classes
- [ ] 4.12 Remove BeaWrapper and EHCache from client dependencies (build.xml / lib)

## 5. Configuration
- [ ] 5.1 Update `BeaConfigurationDialog.java` and `.form`: repurpose existing `txtEndpoint` field for beAstie URL, update label text (e.g., "beAstie URL:" instead of "API-Endpunkt:")
- [ ] 5.2 Update default value for `SERVERCONF_BEAENDPOINT` to `http://localhost:7080`
- [ ] 5.3 Document beAstie deployment alongside j-lawyer-server

## 6. Testing
- [ ] 6.1 Test BeaService with running beAstie instance (login, list postboxes, list folders, get messages)
- [ ] 6.2 Test client end-to-end: login, inbox, send message, eEB operations
- [ ] 6.3 Test error scenarios: beAstie unreachable, invalid certificate, session expiry with auto-re-login
