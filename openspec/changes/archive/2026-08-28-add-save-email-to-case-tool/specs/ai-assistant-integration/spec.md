## ADDED Requirements

### Requirement: File E-Mail Into Case Tool
The client-side `ToolRegistry` SHALL provide a writing tool that stores a mailbox message as an `.eml` document in a case. It SHALL be registered with risk level `RISK_MEDIUM` and therefore SHALL require user approval. The mailbox SHALL be resolved through the mailbox list of the logged-in user, so that the per-user mailbox ACL applies as it does for the read-only e-mail tools.

#### Scenario: File a message into a case
- **WHEN** the LLM calls `save_email_to_case` with a mailbox id, a message reference and a case id
- **THEN** the client SHALL retrieve the message as `.eml` and store it as a document in that case
- **AND** publish a document-added event so the open case refreshes without user interaction
- **AND** return the new document id together with its name, so that the chat panel offers an "open document" button

#### Scenario: Document name follows the configured template
- **WHEN** a message is filed and a default document name template is configured
- **THEN** the name SHALL be built from the subject with an `.eml` extension and then run through that template including placeholder replacement, matching the manual filing dialog
- **AND** when no template is configured, the sanitized subject SHALL be used
- **AND** when the message has no subject, `E-Mail ohne Betreff` SHALL be used
- **AND** the resulting name SHALL be capped at 250 characters while keeping the `.eml` extension

#### Scenario: A document of that name already exists
- **WHEN** the target case already contains a document with the computed name
- **THEN** the client SHALL append ` (2)` through ` (5)` until the name is free
- **AND** report an error naming the conflict if all attempts are taken
- **AND** report a readable error instead of a raw exception when the name is taken by a document in the recycle bin, which the existence check does not cover

#### Scenario: Optional folder and tags
- **WHEN** the call carries a folder id
- **THEN** the stored document SHALL be moved into that folder, otherwise it stays in the case root
- **AND** when the call carries a comma-separated tag list, every tag SHALL be validated against the configured document tags before the document is written, and an invalid tag SHALL abort the call without storing anything

#### Scenario: Case or mailbox not usable
- **WHEN** the case does not exist, is archived, or the mailbox is not accessible to the logged-in user
- **THEN** the client SHALL return an error explaining which precondition failed
- **AND** SHALL NOT store a document

### Requirement: Mail Context For The Assistant Chat
When an assistant chat is started from an opened e-mail, the client SHALL pass the message header data as part of the chat context, and for server-based mailboxes SHALL include the mailbox id and the message reference. Without those identifiers the assistant cannot address the opened message with its e-mail tools. AI actions other than chat SHALL keep receiving the plain message text.

#### Scenario: Chat started from an opened message
- **WHEN** the user starts a chat action from an opened e-mail
- **THEN** the chat context SHALL contain sender, recipients, copy recipients, date, subject and attachment names
- **AND** for a server-based mailbox it SHALL also contain the mailbox id and the message reference, labelled so that they map onto the `mailboxId` and `messageRef` tool parameters
- **AND** the assistant SHALL be able to file that message into a case without searching for it again

#### Scenario: Non-chat AI action from an opened message
- **WHEN** the user starts a translate, summarize, explain or generate action from an opened e-mail
- **THEN** the input SHALL remain the selected text or message body without the header block
