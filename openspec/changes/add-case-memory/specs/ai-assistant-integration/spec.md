## ADDED Requirements

### Requirement: Case Memory Tools
The client-side `ToolRegistry` SHALL provide two read-only tools with risk level `RISK_LOW`:
- `get_case_dossier(caseId)` SHALL return the case memory context package: status,
  not-incorporated documents, master data, lawyer's notes, dossier and card abstracts.
- `get_document_cards(caseId, page)` SHALL return the full cards of a case, 20 per page.

The tool descriptions SHALL instruct the model to read the dossier first and to fetch
individual document text with `get_document_text` only for details, or for documents listed as
not incorporated. The tools SHALL be subject to the existing `aiAgentRole` gating and to the
server-side case access checks. No AI tool SHALL include documents in the case memory; this
includes `save_email_to_case`, which SHALL store documents without including them.

#### Scenario: Dossier available
- **WHEN** the LLM calls `get_case_dossier` for a case with an enabled case memory
- **THEN** the client SHALL return the context package without an approval dialog

#### Scenario: Case memory not enabled
- **WHEN** the LLM calls `get_case_dossier` or `get_document_cards` for a case whose memory is not enabled
- **THEN** the tool SHALL return a result stating that no case memory exists for the case and that `list_case_documents` and `get_document_text` can be used instead
- **AND** it SHALL NOT be reported as an error

#### Scenario: E-mail filed by the assistant
- **WHEN** the LLM files an e-mail into a case with an enabled case memory via `save_email_to_case`
- **THEN** the stored document SHALL NOT be included in the case memory

#### Scenario: Paging through cards
- **WHEN** the LLM calls `get_document_cards` for a case with 45 cards and page 3
- **THEN** the tool SHALL return cards 41 to 45 and indicate that no further page exists

### Requirement: Case Memory Notice in Case Chat
The client SHALL include a short notice that a case memory exists in the chat context when an
assistant chat is started from a case whose case memory is enabled. The notice SHALL state the
time of the last dossier build, the number of included documents not yet incorporated and the
number of case documents that are not included, and SHALL recommend calling `get_case_dossier` before reading individual documents. The dossier itself SHALL NOT be injected
into the chat context automatically.

#### Scenario: Chat from an enabled case
- **WHEN** the user starts the Ingo chat from a case with an enabled case memory
- **THEN** the chat context SHALL contain the case memory notice in addition to the case id and file number

#### Scenario: Chat from a case without case memory
- **WHEN** the user starts the Ingo chat from a case whose memory is not enabled
- **THEN** the chat context SHALL be unchanged compared to today
