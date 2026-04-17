## MODIFIED Requirements

### Requirement: Attachment Download
The REST API SHALL provide an endpoint to download individual attachment content, supporting both JSON (Base64) and raw binary responses. For IMAP mailboxes, the `attachmentId` SHALL be a unique positional index (zero-based string) assigned during MIME tree traversal, ensuring each attachment is individually addressable even when multiple attachments share the same filename.

#### Scenario: User downloads attachment as JSON
- **WHEN** a GET request is made to `/rest/v7/email/mailboxes/{mailboxId}/messages/{messageRef}/attachments/{attachmentId}` with `Accept: application/json`
- **THEN** the response SHALL return the attachment content as Base64 with metadata

#### Scenario: User downloads attachment as raw binary
- **WHEN** a GET request is made with `Accept: application/octet-stream`
- **THEN** the response SHALL return the raw attachment bytes with appropriate `Content-Type` and `Content-Disposition` headers

#### Scenario: Downloading one of multiple same-name IMAP attachments
- **WHEN** an IMAP message contains two attachments both named "scan.pdf"
- **AND** a GET request is made with `attachmentId` set to "1" (the second attachment)
- **THEN** the response SHALL return the content of the second attachment, not the first
