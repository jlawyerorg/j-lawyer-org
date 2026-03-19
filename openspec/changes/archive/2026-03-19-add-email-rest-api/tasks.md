## 1. REST POJOs
- [x] 1.1 Create `RestfulMailboxV7.java` (id, displayName, emailAddress, type)
- [x] 1.2 Create `RestfulMailFolderV7.java` (folderId, parentFolderId, displayName, wellKnownName, unreadCount, totalCount)
- [x] 1.3 Create `RestfulMailMessageV7.java` (messageRef, messageId, subject, from, to, cc, date, read, hasAttachments, body, bodyContentType, inReplyTo, references, attachments list)
- [x] 1.4 Create `RestfulMailAttachmentV7.java` (attachmentId, name, contentType, size, inline, contentId, contentBase64)
- [x] 1.5 Create `RestfulSendMailRequestV7.java` (to, cc, bcc, subject, body, contentType, attachments, priority, readReceipt, inReplyTo, references)
- [x] 1.6 Create `RestfulAppendMailRequestV7.java` (to, cc, bcc, subject, body, contentType, attachments, markAsRead)
- [x] 1.7 Create `RestfulCreateFolderRequestV7.java` (parentFolderId, folderName)

## 2. Endpoint Implementation
- [x] 2.1 Create `EmailEndpointLocalV7.java` local interface
- [x] 2.2 Create `EmailEndpointV7.java` implementing the local interface
- [x] 2.3 Implement mailbox access verification helper (JNDI lookup SecurityServiceLocal, check user access)
- [x] 2.4 Implement `GET /mailboxes` — list mailboxes for authenticated user
- [x] 2.5 Implement `GET /mailboxes/{mailboxId}/folders` — list folders
- [x] 2.6 Implement `POST /mailboxes/{mailboxId}/folders` — create folder
- [x] 2.7 Implement `DELETE /mailboxes/{mailboxId}/folders/{folderId}` — delete folder
- [x] 2.8 Implement `DELETE /mailboxes/{mailboxId}/folders/{folderId}/trash` — empty trash
- [x] 2.9 Implement `GET /mailboxes/{mailboxId}/folders/{folderId}/messages` — list messages with pagination & filter query params
- [x] 2.10 Implement `GET /mailboxes/{mailboxId}/messages/{messageRef}` — get full message
- [x] 2.11 Implement `DELETE /mailboxes/{mailboxId}/messages/{messageRef}` — delete message
- [x] 2.12 Implement `PUT /mailboxes/{mailboxId}/messages/{messageRef}/move` — move message
- [x] 2.13 Implement `PUT /mailboxes/{mailboxId}/messages/{messageRef}/read` — mark read/unread
- [x] 2.14 Implement `GET /mailboxes/{mailboxId}/messages/{messageRef}/eml` — download EML
- [x] 2.15 Implement `GET /mailboxes/{mailboxId}/messages/{messageRef}/attachments/{attachmentId}` — get attachment (JSON + binary)
- [x] 2.16 Implement `POST /mailboxes/{mailboxId}/send` — send email
- [x] 2.17 Implement `POST /mailboxes/{mailboxId}/folders/{folderId}/append` — append to folder
- [x] 2.18 Implement `GET /mailboxes/{mailboxId}/test` — test connection
- [x] 2.19 Implement `DELETE /mailboxes/{mailboxId}/cache` — invalidate caches

## 3. Registration & Integration
- [x] 3.1 Register `EmailEndpointV7` in `EndpointServiceLocator.getClasses()`

## 4. Testing
- [x] 4.1 Manual test: list mailboxes via curl/Swagger
- [x] 4.2 Manual test: list folders, list messages, get message
- [ ] 4.3 Manual test: send email, download attachment, download EML
- [ ] 4.4 Manual test: move/delete message, mark read, create/delete folder
- [ ] 4.5 Verify authorization: user without mailbox access gets 403
