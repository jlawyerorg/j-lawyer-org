## ADDED Requirements

### Requirement: Send E-Mail Tool
The client-side `ToolRegistry` SHALL provide `send_email`, a writing tool that sends a new e-mail
from one of the user's mailboxes and, when a case id is given, files the sent message as an `.eml`
document in that case. It SHALL be registered with risk level `RISK_MEDIUM` and therefore SHALL
require user approval; the approval dialog shows the arguments, so the user sees recipients,
subject and text before anything is sent. The mailbox SHALL be resolved through the mailbox list of
the logged-in user, as for the other e-mail tools. The tool SHALL send through
`EmailServiceRemote.sendMailReturningEml` and file the message it returns.

Parameters: `mailboxId`, `to`, `subject` and `body` are required; `cc`, `bcc`, `html`,
`useSignature`, `attachmentDocumentIds`, `caseId`, `folderId`, `tags` and `keywords` are optional.

#### Scenario: Send without filing
- **WHEN** the LLM calls `send_email` with a mailbox id, recipients, subject and body and no case id
- **THEN** the client SHALL send the message from that mailbox
- **AND** return `sent: true` with the recipients and subject

#### Scenario: Send and file into a case
- **WHEN** the call carries a case id
- **THEN** the client SHALL send the message and store the returned `.eml` in that case, named like
  `save_email_to_case` names a message, including the name clash suffix
- **AND** the document SHALL carry the subject as title, the sending time as received date and the
  first recipient as outgoing correspondent, resolved to a contact where possible, with the number
  of further recipients
- **AND** the optional folder, tags and keywords SHALL be applied
- **AND** the case history SHALL record "E-Mail gesendet an <Empfänger>: <Betreff>"
- **AND** a document-added event SHALL be published

#### Scenario: Signature and format
- **WHEN** `useSignature` is not `false`
- **THEN** the mailbox signature SHALL be appended - the HTML signature when `html` is `true`, the
  text signature otherwise
- **AND** the message SHALL be sent as `text/html` when `html` is `true`, as `text/plain` otherwise

#### Scenario: Case documents as attachments
- **WHEN** the call carries `attachmentDocumentIds`
- **THEN** the content of those documents SHALL be attached under their file names
- **AND** documents without a correspondent SHALL be marked as sent to the first recipient, as the
  send dialog does

#### Scenario: Everything checked before sending
- **WHEN** the mailbox is not accessible, an address is invalid, there are more than 20 recipients
  in total, an attachment is unknown or the attachments exceed 20 MB together, or - with a case id -
  the case does not exist or is archived, or a folder or tag is invalid, or folder, tags or
  keywords are given without a case id
- **THEN** the client SHALL return an error naming the problem
- **AND** SHALL NOT send anything and SHALL NOT store anything

#### Scenario: Sent but not filed
- **WHEN** the message was sent and storing it in the case fails
- **THEN** the client SHALL return `sent: true`, `savedToCase: false` and the reason
- **AND** SHALL NOT report the call as failed, so that the LLM does not send the message again

#### Scenario: Approval denied
- **WHEN** the user denies the tool call in the approval dialog
- **THEN** nothing SHALL be sent or stored
