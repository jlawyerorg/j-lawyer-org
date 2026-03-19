# Change: Expose EmailService as REST API for external clients

## Why
The unified server-side EmailService (IMAP/SMTP + Graph API) is currently only accessible via EJB remote invocation from the desktop client. External clients — such as mobile apps, web frontends, or third-party integrations — cannot access mailbox functionality. Exposing email operations as REST endpoints enables these clients to list mailboxes, browse folder structures, read/search/manage messages, download attachments, and send emails through the same backend-agnostic abstraction the desktop client uses.

## What Changes

### New capability: `email-rest-api` — REST endpoints for email operations
- New JAX-RS endpoint class `EmailEndpointV7` under `/rest/v7/email`
- REST POJOs for request/response serialization (separate from EJB DTOs where needed)
- Endpoints for:
  - **Mailbox discovery**: List mailboxes accessible to the authenticated user
  - **Folder operations**: List folders, create folder, delete folder, empty trash
  - **Message listing**: Paginated message list with filtering (date, unread, search)
  - **Message retrieval**: Get full message with body and attachment metadata
  - **Message management**: Move, delete, mark as read/unread
  - **Attachment download**: Fetch individual attachment content (Base64-encoded in JSON, or raw binary)
  - **Send email**: Compose and send with attachments, CC/BCC, priority, threading headers
  - **Append to folder**: Save messages to Sent/Drafts folders
  - **EML export**: Download message as RFC 822 EML file
  - **Connection test**: Verify mailbox connectivity
  - **Cache management**: Invalidate server-side caches

## Impact
- Affected specs: new `email-rest-api`
- Affected code:
  - New: `j-lawyer-server/j-lawyer-io/src/java/org/jlawyer/io/rest/v7/EmailEndpointV7.java`
  - New: `j-lawyer-server/j-lawyer-io/src/java/org/jlawyer/io/rest/v7/EmailEndpointLocalV7.java` (local interface)
  - New: REST POJOs in `j-lawyer-server/j-lawyer-io/src/java/org/jlawyer/io/rest/v7/pojo/` (e.g. `RestfulMailboxV7.java`, `RestfulMailFolderV7.java`, `RestfulMailMessageV7.java`, `RestfulMailAttachmentV7.java`, `RestfulSendMailRequestV7.java`)
  - Modified: `j-lawyer-server/j-lawyer-io/src/java/org/jlawyer/io/rest/v1/EndpointServiceLocator.java` (register new endpoint)
  - Depends on: `EmailServiceLocal`, `SecurityServiceLocal` (both accessed via JNDI)
