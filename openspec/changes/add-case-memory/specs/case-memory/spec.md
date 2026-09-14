## ADDED Requirements

### Requirement: Server-Wide Enablement and Configuration
The system SHALL provide a server-wide case memory feature that is disabled by default and
configured through server settings. The configuration SHALL consist of:
- a global on/off switch,
- the id of the `AssistantConfig` (Ingo connection) to use,
- request type, action id and optional model, separately for card generation and for dossier
  generation,
- the debounce interval in minutes (default 10),
- the card input budget in characters (default 120000),
- the dossier input budget in characters (default 200000),
- the maximum number of LLM calls per background run (default 20).

Only users with `adminRole` SHALL be able to change these settings.

#### Scenario: Feature disabled by default
- **WHEN** a server is installed or upgraded to this version
- **THEN** the global switch SHALL be off
- **AND** no case SHALL be processed and no LLM call SHALL be made for the case memory

#### Scenario: Administrator configures the feature
- **WHEN** an administrator selects an assistant configuration, the actions and models for cards and dossier, and switches the feature on
- **THEN** the settings SHALL be persisted in the server settings
- **AND** users SHALL be able to enable the case memory for individual cases

#### Scenario: Global switch turned off
- **WHEN** an administrator turns the global switch off
- **THEN** background processing SHALL stop for all cases
- **AND** already generated cards, dossiers and notes SHALL remain readable

### Requirement: Per-Case Opt-In
The case memory SHALL be processed only for cases on which it has been explicitly enabled.
Enabling SHALL require `writeArchiveFileRole`, `aiAgentRole`, access to the case under the case
group permissions, and the global switch being on. Enabling and disabling SHALL each be recorded
in the case history with the acting user.

#### Scenario: User enables the case memory
- **WHEN** a user with `writeArchiveFileRole` and `aiAgentRole` enables the case memory for a case they may access
- **THEN** the case SHALL be marked as enabled with the user and timestamp
- **AND** a case history entry SHALL state that the case memory was enabled
- **AND** no document of the case SHALL be included until it is opted in individually

#### Scenario: User without aiAgentRole tries to enable
- **WHEN** a user without `aiAgentRole` tries to enable the case memory
- **THEN** the request SHALL be rejected
- **AND** the case SHALL remain disabled

#### Scenario: Case not enabled
- **WHEN** a document is added to a case whose memory is not enabled
- **THEN** no card SHALL be created and no document content SHALL be sent to the AI service

### Requirement: Disabling Deletes Generated Content
Disabling the case memory of a case SHALL stop its processing and delete all document
inclusions, all generated cards and the dossier of that case. The lawyer's notes SHALL be
preserved. Deleting a case SHALL delete its case memory entirely, including notes.

#### Scenario: User disables the case memory
- **WHEN** a permitted user disables the case memory of a case
- **THEN** all document inclusions, cards and the dossier of that case SHALL be deleted
- **AND** the notes SHALL be kept
- **AND** after re-enabling, every document SHALL again require an individual opt-in
- **AND** a case history entry SHALL state that the case memory was disabled

#### Scenario: Case is deleted
- **WHEN** a case with an enabled case memory is deleted
- **THEN** its case memory record, cards and notes SHALL be deleted with it

### Requirement: Per-Document Opt-In
Within an enabled case, a document SHALL be processed only after it has been explicitly included
in the case memory. The inclusion SHALL be recorded with the including user and a timestamp.
Documents SHALL NOT be included automatically by any path, including server-side filing
(mailbox scanner, Dropscan), AI tools, REST uploads and document copies or moves between cases.
Including and excluding documents SHALL require `writeArchiveFileRole`, `aiAgentRole` and case
access, and each include or exclude action SHALL be recorded in the case history with the
affected document names. Excluding a document SHALL delete its card and mark the case as changed
for a dossier rebuild.

#### Scenario: Document included by the user
- **WHEN** a permitted user includes a document of an enabled case
- **THEN** the inclusion SHALL be stored with user and timestamp
- **AND** the document SHALL receive a card in a subsequent background run
- **AND** a case history entry SHALL name the included document

#### Scenario: Document filed automatically
- **WHEN** the mailbox scanner files an e-mail into a case with an enabled case memory
- **THEN** the new document SHALL NOT be included
- **AND** no content of it SHALL be sent to the AI service

#### Scenario: Document excluded by the user
- **WHEN** a permitted user excludes a previously included document
- **THEN** its card SHALL be deleted
- **AND** the next dossier rebuild SHALL no longer use it

#### Scenario: Case memory not enabled
- **WHEN** a user tries to include a document of a case whose memory is not enabled
- **THEN** the request SHALL be rejected

### Requirement: Opt-In at Document Assignment
The desktop client SHALL offer a checkbox "Zum Aktenwissen hinzufügen" at the moment a user
stores one or more documents into a case. It SHALL do so at least in these shared dialogs, so
that every path using them is covered:
- the bulk save dialog (e-mail inbox, beA inbox, scans, Dropscan),
- the file naming prompt, whenever it is shown for storing a document into a case (for example
  e-mail and beA attachments, eEB responses, copying or moving documents to another case,
  derived documents such as PDF conversion, split, stamp, anonymisation or page reordering,
  E-POST letters, fax and E-POST reports, AI results),
- the new-document-from-template dialog (templates, invoices, calculations, form plugins, AI
  generation),
- the e-mail and beA send windows when "als Dokument speichern" is selected,
- the note dialog.

The checkbox SHALL be offered only when the target case has an enabled case memory and the user
holds the roles required to include documents. It SHALL be unchecked by default and SHALL NOT
remember a previous choice. When checked, all documents stored by that action SHALL be included
right after they have been stored. When the target case is chosen inside the dialog, the
availability of the checkbox SHALL follow the currently selected case. A failed inclusion SHALL
NOT undo the stored documents, and the user SHALL be informed about it.

Uploads without a dialog (file chooser, drag and drop into the case or a folder) SHALL, when the
target case has an enabled case memory, show a non-blocking notice after the upload. The notice
SHALL offer to include the uploaded documents with one click, and SHALL leave them not included
when it is dismissed or ignored. Paths without user interaction SHALL NOT include documents:
draft autosave, documents created by the server (for example claim statements and dunning
exports), form data saved by form plugins, and saving the case history as a document. Documents
from these paths SHALL be includable later from the case document list or the case memory tab.

#### Scenario: Saving an e-mail with opt-in
- **WHEN** a user saves an e-mail to a case with an enabled case memory and checks "Zum Aktenwissen hinzufügen"
- **THEN** the stored document SHALL be included in the case memory

#### Scenario: Saving without opt-in
- **WHEN** a user assigns a document to a case with an enabled case memory and leaves the checkbox unchecked
- **THEN** the document SHALL be stored but not included

#### Scenario: Target case without case memory
- **WHEN** the selected target case has no enabled case memory
- **THEN** the checkbox SHALL NOT be offered

#### Scenario: Switching the target case in the dialog
- **WHEN** the user changes the target case in an assignment dialog from a case with an enabled case memory to one without
- **THEN** the checkbox SHALL become unavailable and unchecked

#### Scenario: Bulk saving scans
- **WHEN** a user saves four scans into a case with an enabled case memory through the bulk save dialog and checks "Zum Aktenwissen hinzufügen"
- **THEN** all four stored documents SHALL be included

#### Scenario: Moving a document with opt-in
- **WHEN** a user moves a document to another case with an enabled case memory and checks the box in the naming prompt
- **THEN** the new document in the target case SHALL be included

#### Scenario: Drag and drop upload
- **WHEN** a user drops three files onto a case with an enabled case memory
- **THEN** the documents SHALL be stored and a notice SHALL offer to include them
- **AND** they SHALL be included only if the user confirms the notice

#### Scenario: Sending an e-mail and saving it as a document
- **WHEN** a user sends an e-mail with "als Dokument speichern" to a case with an enabled case memory and checks "Zum Aktenwissen hinzufügen" in the send window
- **THEN** the stored e-mail document SHALL be included, regardless of whether the naming prompt was shown

### Requirement: Document Inclusion in the Case Document List
When the case memory of a case is enabled, the case document list in the desktop client SHALL
mark included documents visibly. For one or more selected documents, it SHALL offer the actions
"Zum Aktenwissen hinzufügen" and "Aus dem Aktenwissen entfernen", subject to the inclusion
permissions.

#### Scenario: Including several documents at once
- **WHEN** a user selects five documents in the case document list and chooses "Zum Aktenwissen hinzufügen"
- **THEN** all five documents SHALL be included and marked in the list
- **AND** a single case history entry SHALL name the five documents

#### Scenario: Case memory disabled
- **WHEN** the case memory of the case is not enabled
- **THEN** the document list SHALL show neither inclusion marks nor the include and exclude actions

### Requirement: Document Card Generation
For every included document of an enabled case that is not in the recycle bin, the system SHALL
maintain exactly one document card. The card SHALL be generated in the background from the document's
existing text preview. It SHALL consist of a header written by the system, containing document
id, name, document date and folder, followed by an LLM-generated body with the sections
KURZFASSUNG, DOKUMENTART, URHEBER/ABSENDER, DATUM, KERNAUSSAGEN, BETRÄGE and FRISTEN/TERMINE.
Statements in KERNAUSSAGEN SHALL be attributed to their originator. The KURZFASSUNG SHALL be
stored separately as the card abstract. When that line is missing, the first 300 characters of
the body SHALL be used instead.

#### Scenario: Card for a newly included document
- **WHEN** a document with extractable text is included in an enabled case memory
- **THEN** a card SHALL be generated for it by a background run
- **AND** the card SHALL store the model, prompt version, document version and SHA-256 fingerprint of the text it was generated from

#### Scenario: Document without extractable text
- **WHEN** the text preview of a document is empty or contains only whitespace
- **THEN** a card with status `NO_TEXT` containing only the system header SHALL be stored
- **AND** no LLM call SHALL be made

#### Scenario: Very long document
- **WHEN** the text preview of a document exceeds the card input budget
- **THEN** only the first characters up to the budget SHALL be sent to the LLM
- **AND** the card SHALL be flagged as truncated

### Requirement: Change Detection Without Unnecessary LLM Calls
The system SHALL detect document changes by comparing the document version with the version
stored on the card. When the version differs, it SHALL compare the SHA-256 fingerprint of the
current text preview with the card's fingerprint. A card SHALL be regenerated only when the
fingerprint differs.

#### Scenario: Document content changed
- **WHEN** the content of an included document is replaced and its text changes
- **THEN** the card SHALL be regenerated
- **AND** the case SHALL be marked as changed for a dossier rebuild

#### Scenario: Document renamed or metadata changed
- **WHEN** a document is renamed, moved to another folder, re-dated or re-tagged without a text change
- **THEN** no LLM call SHALL be made
- **AND** only the card's stored document version SHALL be updated

#### Scenario: OCR adds text to a scanned document
- **WHEN** OCR is performed on a document that previously had a `NO_TEXT` card
- **THEN** the changed fingerprint SHALL cause a regular card to be generated

### Requirement: Document Removal, Restore and Move
While an included document is in the recycle bin, it SHALL NOT be used for the dossier, but its
inclusion and card SHALL be kept. A card and inclusion SHALL be deleted when the document is
permanently deleted. A document moved to another case SHALL leave the source case's memory. In
the target case it SHALL be included only if the user opts in during the move, as for any other
document assignment.

#### Scenario: Document moved to the recycle bin
- **WHEN** an included document is moved to the recycle bin
- **THEN** its card SHALL no longer be used for the dossier
- **AND** the case SHALL be marked as changed for a dossier rebuild

#### Scenario: Document restored with unchanged text
- **WHEN** a document is restored from the recycle bin and its text fingerprint equals the card's fingerprint
- **THEN** the existing card SHALL be reused without an LLM call

#### Scenario: Document permanently deleted
- **WHEN** a document is permanently deleted
- **THEN** its card SHALL be deleted

#### Scenario: Document moved to another case
- **WHEN** an included document is moved from an enabled case to another case with an enabled case memory, without opting in during the move
- **THEN** it SHALL no longer contribute to the source case's dossier
- **AND** it SHALL NOT be included in the target case

### Requirement: Dossier Rebuild From Cards
The system SHALL generate the case dossier from the case's cards, the case master data and the
lawyer's notes. It SHALL NOT derive the dossier from a previous dossier. A rebuild SHALL start
only once the case has had no relevant change for the configured debounce interval and no card
of the case is pending. Failed cards SHALL NOT block a rebuild. A relevant change is a card
added or regenerated, a document excluded or moved to the recycle bin, or a notes change.

#### Scenario: Bulk import
- **WHEN** 200 documents of an enabled case are included within a few minutes
- **THEN** cards SHALL be generated for all of them
- **AND** the dossier SHALL be rebuilt once, after the debounce interval has elapsed since the last change and all cards are no longer pending

#### Scenario: A card failed permanently
- **WHEN** one card of a case is in status `FAILED` and all other cards are complete
- **THEN** the dossier SHALL be rebuilt without it
- **AND** the document SHALL be listed as not incorporated

#### Scenario: Rebuild fails
- **WHEN** the dossier generation fails
- **THEN** the previous dossier SHALL be kept
- **AND** the error SHALL be recorded and shown in the case memory status

### Requirement: Dossier Content and Attribution
The dossier SHALL be Markdown with the sections Sachstand, Beteiligte und Positionen,
Chronologie, Streitpunkte, Beweismittel, Offene Fragen and Widersprüche. Every factual statement
SHALL cite its source document as `[D:<documentId>]`. Statements made by a party SHALL be
attributed to that party and SHALL NOT be presented as established fact. The lawyer's notes SHALL
be treated as context that takes precedence, and SHALL NOT be copied into the dossier.

#### Scenario: Disputed statement
- **WHEN** a pleading of the opposing party asserts a fact that the client disputes in another document
- **THEN** the dossier SHALL state who asserts and who disputes it, each with its `[D:<id>]` citation
- **AND** the point SHALL appear under Streitpunkte

### Requirement: Citation Validation
After each dossier generation, the system SHALL check every `[D:<id>]` token against the set of
cards provided as input. It SHALL remove tokens that reference any other id and record their
number in the dossier build metadata. Citations SHALL be stored by document id and rendered to
the current document name at display time.

#### Scenario: Hallucinated citation
- **WHEN** the generated dossier contains `[D:x]` and no card with id `x` was part of the input
- **THEN** the token SHALL be removed from the stored dossier
- **AND** the build metadata SHALL report one invalid citation

#### Scenario: Cited document renamed
- **WHEN** a document cited in the dossier is renamed
- **THEN** the dossier SHALL show the new name without being rebuilt

### Requirement: Dossier Input Budget
When the combined size of all full cards exceeds the dossier input budget, the system SHALL
replace the full cards of the oldest documents, by document date, with their abstracts until the
input fits. It SHALL record in the build metadata that this fallback was used.

#### Scenario: Very large case
- **WHEN** a case has so many cards that their full text exceeds the dossier input budget
- **THEN** the newest cards SHALL be sent in full and the oldest as abstracts only
- **AND** the build metadata SHALL indicate the fallback and the number of abstract-only cards

### Requirement: Lawyer's Notes
Each case memory SHALL have a manually maintained notes text. Users with `writeArchiveFileRole`
and case access SHALL be able to edit it. Background processing SHALL never modify it. Every
notes change SHALL be recorded in the case history and SHALL mark the case as changed for a
dossier rebuild.

#### Scenario: Lawyer corrects an interpretation
- **WHEN** a user saves notes stating that an expert opinion was misread
- **THEN** the notes SHALL be stored unchanged with user and timestamp
- **AND** the next dossier rebuild SHALL receive the notes as input

#### Scenario: Background run after notes change
- **WHEN** a background run processes a case with notes
- **THEN** the stored notes SHALL remain byte-identical

### Requirement: Context Package
Every read of a case memory SHALL return a context package consisting of:
- **status:** enabled flag, time of the last dossier build, the counts of included documents
  incorporated, pending, failed, without text and truncated, and the count of case documents
  that are not included,
- **not incorporated:** the ids and names of all included documents that are not incorporated in
  the current dossier. Documents that are not included SHALL be reported only as a count, without
  ids, names or content,
- **master data** rendered from the database at read time: file number, name, reason, subject
  field, claim value, responsible lawyer, parties with party type, and open follow-ups,
  deadlines and appointments,
- the lawyer's notes,
- the dossier,
- the list of card abstracts with document ids.

Master data SHALL NOT be taken from LLM output.

#### Scenario: Included document not yet processed
- **WHEN** a context package is read while a newly included document has no card yet
- **THEN** the package SHALL list that document's id and name as not incorporated

#### Scenario: Documents not included
- **WHEN** a case has 40 documents of which 25 are included
- **THEN** the package SHALL report 15 documents as not included
- **AND** it SHALL NOT list their ids, names or content

#### Scenario: Party added after the last build
- **WHEN** a party is added to the case after the last dossier build
- **THEN** the master data in the next context package SHALL contain the new party without a rebuild

### Requirement: Background Processing Robustness
Background processing SHALL be driven by a periodic reconciliation of documents against cards,
so that it recovers after a server restart without losing work. LLM calls SHALL be made outside
database transactions. Each run SHALL make at most the configured number of LLM calls, cards
first. Overlapping runs SHALL be skipped. When the AI service cannot be reached, processing
SHALL pause globally for a back-off period without counting a failed attempt against individual
cards. A card whose generation fails SHALL be retried at most three times and then remain
`FAILED` until its document changes or a full rebuild is requested.

#### Scenario: Server restarts during processing
- **WHEN** the server restarts while cards of an enabled case are still missing
- **THEN** the next run after startup SHALL continue creating the missing cards

#### Scenario: AI service unreachable
- **WHEN** the configured Ingo instance cannot be reached
- **THEN** the run SHALL stop and the next attempt SHALL be delayed by the back-off period
- **AND** the attempt counters of the affected cards SHALL NOT increase

#### Scenario: Per-run limit
- **WHEN** 500 cards are pending and the per-run limit is 20
- **THEN** a single run SHALL make at most 20 LLM calls

### Requirement: Manual Rebuild
Users with `writeArchiveFileRole`, `aiAgentRole` and case access SHALL be able to request a
dossier rebuild, or a full rebuild that regenerates every card of the case. Requests SHALL be
executed asynchronously by the background processing, SHALL NOT wait for the debounce interval,
and SHALL reset failed cards.

#### Scenario: Dossier rebuild requested
- **WHEN** a user requests a dossier rebuild
- **THEN** the next background run SHALL rebuild the dossier from the existing cards without waiting for the debounce interval

#### Scenario: Full rebuild requested after a prompt update
- **WHEN** a user requests a full rebuild for a case whose cards were built with an older prompt version
- **THEN** all cards of the case SHALL be regenerated and the dossier rebuilt afterwards

### Requirement: Prompt Versioning
Cards and dossiers SHALL record the prompt version they were generated with. A new prompt
version SHALL NOT trigger automatic regeneration. The case memory status SHALL indicate when
cards or the dossier were built with an older prompt version.

#### Scenario: Server updated with new prompts
- **WHEN** the server is updated to a version with a higher prompt version
- **THEN** existing cards and dossiers SHALL remain in use
- **AND** the status SHALL indicate that they were built with an older prompt version

### Requirement: Case Memory Access Control
Reading the case memory status, context package and cards SHALL require `readArchiveFileRole`
and access to the case under the case group permissions. Editing notes SHALL require
`writeArchiveFileRole` and case access. Enabling, disabling, including and excluding documents,
and requesting rebuilds SHALL require `writeArchiveFileRole`, `aiAgentRole` and case access.

#### Scenario: User outside the case groups
- **WHEN** a user who is not a member of the case's owner group or allowed groups requests the context package
- **THEN** the request SHALL be rejected like any other access to that case

### Requirement: Case Memory REST API
The REST API v8 SHALL provide read-only endpoints `GET /v8/cases/{id}/memory`, returning the
context package, and `GET /v8/cases/{id}/memory/cards`, returning the cards with document id,
name, status, abstract, body, truncated flag and prompt version. Both SHALL require
`readArchiveFileRole` and case access. For a case whose memory is not enabled, both SHALL
respond with the enabled flag set to false and no generated content.

#### Scenario: Read the context package over REST
- **WHEN** an authorized user calls `GET /v8/cases/{id}/memory` for an enabled case
- **THEN** the response SHALL contain status, not-incorporated documents, master data, notes, dossier and card abstracts

#### Scenario: Endpoint documented
- **WHEN** the generated `swagger.json` is inspected
- **THEN** both operations SHALL appear under the cases endpoints

### Requirement: Case Memory Tab in the Case View
The desktop client SHALL provide an "Aktengedächtnis" tab in the case view. The tab SHALL show:
- the status and processing progress,
- the dossier rendered as Markdown, with citations displayed as the current document names and
  opening the document on click,
- an editor for the lawyer's notes,
- the list of cards with document name, status and abstract,
- the list of case documents that are not included, with the option to include several of them
  at once,
- actions for enabling, disabling, dossier rebuild and full rebuild, according to the user's
  permissions.

Enabling SHALL require confirmation in a dialog that names the configured AI service and model,
states whether the model is local, and explains that included documents will be sent to that
service automatically from then on, and that each document must be included individually.

#### Scenario: Enabling with a non-local model
- **WHEN** a user enables the case memory and the configured model is not local
- **THEN** the confirmation dialog SHALL state that document contents will be transmitted to an external AI service
- **AND** the case memory SHALL only be enabled after the user confirms

#### Scenario: Processing in progress
- **WHEN** the tab is opened for a case whose cards are being generated
- **THEN** it SHALL show the number of processed and total included documents

#### Scenario: Automatically filed documents awaiting a decision
- **WHEN** the mailbox scanner has filed three e-mails into the case since the last visit
- **THEN** the tab SHALL list them among the documents that are not included
- **AND** the user SHALL be able to include them with one action

#### Scenario: Clicking a citation
- **WHEN** the user clicks a document citation in the rendered dossier
- **THEN** the cited document SHALL be opened

### Requirement: Case Memory Administration Dialog
The desktop client SHALL provide an administration dialog for the server-wide case memory
settings. In it, the assistant configuration, actions and models are chosen from those reported
by the selected Ingo instance, and the dialog SHALL show whether each selected model is local.
It SHALL also show the number of enabled cases and of pending and failed cards.

#### Scenario: Administrator selects a model
- **WHEN** an administrator opens the dialog and selects an assistant configuration
- **THEN** the dialog SHALL list the actions and models reported by that Ingo instance, each model with its local flag
