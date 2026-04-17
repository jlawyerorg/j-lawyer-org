# Change: Fix duplicate attachment filename handling

## Why
When an IMAP message contains multiple attachments with the same filename, opening or saving any of them always returns the content of the first attachment. This is because the IMAP `attachmentId` is set to the filename (not a unique identifier), and the client looks up attachments by name only.

## What Changes
- Server: Generate unique positional `attachmentId` values for IMAP attachments (index-based instead of filename-based)
- Client: Track attachments in the UI by `attachmentId`/index instead of by display name, so duplicate filenames resolve to the correct attachment content

## Impact
- Affected specs: `email-client`, `email-rest-api`
- Affected code:
  - `j-lawyer-server/j-lawyer-server-ejb/src/java/com/jdimension/jlawyer/services/EmailService.java` (IMAP attachment ID generation)
  - `j-lawyer-client/src/com/jdimension/jlawyer/client/mail/MailContentUI.java` (attachment list model and all consumer actions)
  - `j-lawyer-client/src/com/jdimension/jlawyer/client/mail/EmailUtils.java` (index-based attachment retrieval for local EML)
