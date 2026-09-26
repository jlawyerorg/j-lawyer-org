## ADDED Requirements

### Requirement: Document Title
The system SHALL store an optional human-readable title (Bezeichnung) for each case
document, independent of its file name. Renaming the file SHALL NOT change the title, and
changing the title SHALL NOT change the file name. Wherever a document is listed, the title
SHALL be displayed when set and the file name SHALL be displayed otherwise.

#### Scenario: Title set independently of the file name
- **WHEN** a user sets the title "Klageerwiderung Beklagter" on the document `2026-09-12_scan_0042.pdf`
- **THEN** the document is listed as "Klageerwiderung Beklagter"
- **AND** the file name remains `2026-09-12_scan_0042.pdf`

#### Scenario: No title set
- **WHEN** a document has no title
- **THEN** its file name is displayed in its place

### Requirement: Document Keywords
The system SHALL store free-text keywords (Schlagworte) for each case document in a single
field as a comma-separated list of words or word groups. On save the system SHALL trim each
keyword, drop empty entries, remove case-insensitive duplicates and store the list
separated by `, `. Keywords SHALL be independent of the existing document tags (Etiketten),
which remain unchanged.

#### Scenario: Keywords are normalized
- **WHEN** a user enters ` Frist ,Kosten,, frist , Vergleich angeboten`
- **THEN** the stored keywords are `Frist, Kosten, Vergleich angeboten`

#### Scenario: Keyword suggestions per case
- **WHEN** a user edits keywords of a document in a case whose other documents use the keywords `Frist` and `Kosten`
- **THEN** `Frist` and `Kosten` are offered as suggestions

### Requirement: Document Received Date
The system SHALL store an optional received date (Eingangsdatum) with date and time for
each case document. It SHALL be editable by the user and SHALL be set automatically when a
document is created from an incoming message (see Automatic Metadata on Receipt).

#### Scenario: Manual received date
- **WHEN** a user sets the received date of a scanned letter to 15.09.2026
- **THEN** the document shows "Eingang 15.09.2026" and can be sorted by received date

### Requirement: Document Correspondent
The system SHALL store for each case document at most one correspondent (Von/An)
consisting of an optional reference to a contact, a display name and a direction
(none, incoming = "Von", outgoing = "An"). If no matching contact exists, the display name
alone SHALL be stored as free text. The display name SHALL be kept when the referenced
contact is deleted.

#### Scenario: Correspondent referencing a contact
- **WHEN** a user sets "Von" to the contact "RA Müller"
- **THEN** the document stores the contact reference, the name "RA Müller" and direction incoming
- **AND** the list shows "↘ RA Müller"

#### Scenario: Free-text correspondent
- **WHEN** a document is received from `info@unbekannt.example` which matches no contact
- **THEN** the document stores no contact reference and the name `info@unbekannt.example` with direction incoming

#### Scenario: Referenced contact deleted
- **WHEN** the contact referenced as correspondent is deleted
- **THEN** the contact reference is cleared and the display name remains

### Requirement: Document Hierarchy
The system SHALL allow a case document to reference a parent document of the same case
(e.g. attachments of an e-mail). The system SHALL reject a parent from a different case and
any assignment that would create a cycle. A document MAY have any number of children.

#### Scenario: Attachments below their e-mail
- **WHEN** the documents `Anlage K1.pdf` and `Anlage K2.pdf` have the e-mail document `AW Vergleichsangebot.eml` as parent
- **THEN** the e-mail document reports two attachments and both are shown below it

#### Scenario: Cycle rejected
- **WHEN** a user tries to set document A as parent of document B while B is already an ancestor of A
- **THEN** the system rejects the change with an error message

#### Scenario: Parent from another case rejected
- **WHEN** a parent from a different case is assigned
- **THEN** the system rejects the change

### Requirement: Children Become Independent
The system SHALL keep children as independent documents when their parent is deleted or
moved to another case without them: the children SHALL remain in their case as independent
documents. Moving documents between folders of the same case SHALL NOT change any
relation. Restoring
a deleted parent from the recycle bin SHALL restore its relations to children that still
exist in the case.

#### Scenario: Parent deleted
- **WHEN** a user deletes an e-mail document that has two attachments
- **THEN** both attachments remain in the case and are shown as top-level documents

#### Scenario: Only the parent moved
- **WHEN** a user moves only the e-mail document to another case
- **THEN** its attachments stay in the source case as independent documents

#### Scenario: Parent restored from recycle bin
- **WHEN** a deleted e-mail document is restored while its attachments are still in the case
- **THEN** the attachments are shown below it again

### Requirement: Copying and Moving Documents Between Cases
The system SHALL copy and move a set of documents to another case in one server-side
operation. The copies SHALL carry content, file name, dictation sign, tags, favorite,
highlights, document type and all document metadata (title, keywords, received date,
correspondent). Parent-child relations between documents of the same set SHALL be preserved
in the copies, referring to the new documents; relations to documents outside the set SHALL
NOT be carried over. Moving SHALL additionally put the source documents into the recycle bin
of the source case. All client paths that copy or move documents between cases (context
menu, drag and drop, assistant tools, REST) SHALL use this operation.

#### Scenario: Metadata copied
- **WHEN** a user copies a document with title "Klageerwiderung", keywords "Frist" and "Von" = "RA Müller" into another case
- **THEN** the copy in the target case has the same title, keywords and correspondent

#### Scenario: Parent and children moved together
- **WHEN** a user selects an e-mail document and both of its attachments and moves them to another case
- **THEN** in the target case both attachment copies are children of the e-mail copy
- **AND** the source documents are in the recycle bin of the source case

#### Scenario: Parent and children copied together
- **WHEN** a user copies an e-mail document and one of its two attachments into another case
- **THEN** in the target case the attachment copy is a child of the e-mail copy
- **AND** the documents in the source case keep their relations unchanged

#### Scenario: Child copied without its parent
- **WHEN** a user copies only an attachment into another case
- **THEN** the copy has no parent

### Requirement: Creating a Document With Metadata
The system SHALL allow creating a case document together with its metadata (title,
keywords, received date, correspondent, parent) in a single transaction, and SHALL index
the new document in the full-text search once, including its metadata. Automatic metadata
population and copying or moving between cases SHALL use this operation. The existing
operation for creating a document without metadata SHALL remain available.

#### Scenario: E-mail attachment created with metadata
- **WHEN** an attachment of an incoming e-mail is saved to a case
- **THEN** the attachment is created with its parent, received date and correspondent in one step
- **AND** a search for `von:<sender>` finds it without a further index update

### Requirement: Instant Messages Linked to a Document
The system SHALL provide, per case, the number of instant messages linked to each document
in a single request, and SHALL provide the list of instant messages linked to a given
document. Users SHALL be able to create a new instant message linked to a document directly
from that document.

#### Scenario: Message count
- **WHEN** two instant messages reference a document
- **THEN** the document list reports a message count of 2 for that document

#### Scenario: New message from a document
- **WHEN** a user starts "Nachricht zu diesem Dokument" on a document
- **THEN** the message dialog opens with the case and the document preset as context

### Requirement: Metadata Editing for One or Many Documents
The system SHALL allow editing title, keywords, received date, correspondent and parent of
a single document, and SHALL allow editing keywords (set, add, remove), received date and
correspondent of multiple documents in one operation. Fields not touched in a bulk edit
SHALL remain unchanged per document. Metadata edits SHALL NOT create a new document version
and SHALL NOT change the document's change date; they SHALL create a case history entry
(one entry per bulk operation). Metadata edits SHALL require write permission on the case
and SHALL be allowed on locked documents.

#### Scenario: Bulk add keyword
- **WHEN** a user selects three documents with different keywords and adds the keyword `Beweis`
- **THEN** each document keeps its previous keywords and additionally has `Beweis`

#### Scenario: Bulk edit leaves untouched fields
- **WHEN** a user sets only the received date for five selected documents
- **THEN** the titles, keywords and correspondents of the five documents are unchanged

#### Scenario: No new version
- **WHEN** a user changes the title of a document with version 3
- **THEN** the document still has version 3 and its change date is unchanged

### Requirement: Automatic Metadata on Receipt
The system SHALL set metadata automatically when an e-mail or beA message is saved to a
case, whether by a user or by an automated server process. On the document of the message
it SHALL set: the received date to
the message's receipt time, the correspondent to the sender (direction incoming, contact
resolved first among the case parties and then globally by e-mail address or beA SafeId),
and the title to the message subject. Attachments saved together with the message SHALL get
the message document as parent and the same received date and correspondent. When only
attachments are saved, they SHALL get received date and correspondent but no parent.

#### Scenario: E-mail with attachments saved
- **WHEN** a user saves an e-mail from `mueller@kanzlei.example` received on 12.09.2026 10:14 with subject "Klageerwiderung" and two attachments into a case, as message plus separate attachments
- **THEN** the message document has title "Klageerwiderung", received date 12.09.2026 10:14 and "Von" set to the contact with that e-mail address
- **AND** both attachment documents have the message document as parent and the same received date and correspondent

#### Scenario: beA message saved
- **WHEN** a beA message is saved to a case
- **THEN** the sender is resolved via its SafeId and set as "Von", and the received date is the beA receipt time

### Requirement: Automatic Metadata on Sending
When case documents are sent by e-mail, beA, fax or ePost, the system SHALL set the
correspondent of each sent case document to the first recipient with direction outgoing,
unless the document already has a correspondent. When the sent message itself is stored as
a case document, it SHALL get the first recipient as outgoing correspondent and the subject
as title. With more than one recipient, the display name SHALL be the first recipient
followed by " +N" where N is the number of further recipients. Documents that already
exist in the case SHALL NOT be re-parented by sending.

#### Scenario: Document sent by beA
- **WHEN** a user sends a brief without correspondent via beA to the court "AG Musterstadt"
- **THEN** the brief gets "An" = "AG Musterstadt"

#### Scenario: Forwarded incoming document keeps its sender
- **WHEN** a user forwards a document with "Von" = "RA Müller" by e-mail to the client
- **THEN** the document keeps "Von" = "RA Müller"

#### Scenario: Several recipients
- **WHEN** a sent e-mail with three recipients is saved to the case
- **THEN** its correspondent name is the first recipient followed by " +2"

### Requirement: Existing REST Endpoints Remain Compatible
All existing REST endpoints (v1 to v8) SHALL continue to work unchanged: their paths,
request formats and response formats SHALL NOT change. Existing endpoints that create or
modify a case document SHALL NOT clear or overwrite the new metadata (title, keywords,
received date, correspondent, parent) unless they explicitly set it.

#### Scenario: Rename via an existing endpoint keeps metadata
- **WHEN** a client renames a document with title "Klageerwiderung" and keywords "Frist" via an existing v1 endpoint
- **THEN** the file name is changed and the title and keywords are unchanged

#### Scenario: Existing document list unchanged
- **WHEN** a client calls `GET /v1/cases/{id}/documents`
- **THEN** the response has the same structure as before this change

### Requirement: Document Metadata via REST API
The REST API version 8 SHALL additively make all new document attributes available, and
SHALL provide: a case document list including title,
keywords, received date, correspondent (id, name, direction), parent id, message count and
tags; a single document with all metadata; updating the metadata of one document; setting
or removing the parent of a document; bulk updating the metadata of several
documents with the same semantics as the desktop bulk edit; keyword suggestions per case;
and the instant messages linked to a document. Access SHALL be checked against the case permissions of the caller.

#### Scenario: List documents with metadata
- **WHEN** a client calls `GET /v8/cases/{id}/documents`
- **THEN** each document contains title, keywords, receivedDate, correspondentId, correspondentName, correspondentDirection, parentId and messageCount

#### Scenario: Update without permission
- **WHEN** a user without write access to the case calls `PUT /v8/cases/documents/{id}/metadata`
- **THEN** the request is rejected and no data is changed

### Requirement: Metadata in the Save-to-Case Dialog
The dialog that saves e-mails, beA messages, their attachments and scans to a case SHALL show,
per file, the title, keywords, received date and sender / recipient the document will be saved
with, prefilled from where the document comes from, and SHALL allow changing them. An
attachment of a message SHALL show that it is saved as attachment of the message and SHALL
allow saving it as an independent document instead. A sender / recipient that is not changed
SHALL be resolved to a contact as described in Automatic Metadata on Receipt. The dialog
SHALL offer to add keywords to all files and to set the sender / recipient of all files at once.

#### Scenario: Prefilled from the e-mail
- **WHEN** a user saves an e-mail with subject "Klageerwiderung" and two attachments, as message plus separate attachments
- **THEN** the message file shows the title "Klageerwiderung", the received date and the sender, and both attachments show that they are saved as attachments of the message

#### Scenario: Attachment saved independently
- **WHEN** the user unticks "als Anlage speichern" for one attachment
- **THEN** that attachment is saved without parent

#### Scenario: Keywords for all files
- **WHEN** the user adds the keyword "Beweis" for all files
- **THEN** every file that is saved gets the keyword "Beweis" in addition to its own keywords

### Requirement: Assistant Suggestions Before Saving
The AI buttons for title and keywords SHALL also be available in the save-to-case dialog,
per file and as actions for all files; the actions for all files SHALL enter the suggested
keywords and titles directly, without asking per file. The text of a file that is not saved yet
SHALL be obtained without uploading it where possible: the subject and body of the message
itself, the text of a PDF read locally, and plain text formats read locally. Other formats
SHALL be sent to the server for text extraction without being saved, only up to a maximum file
size stored as a server setting and configurable in the assistant settings (default 3 MB).
The text SHALL be obtained at most once per file. Files without recognizable text or above the
size limit SHALL get no suggestions, with a hint that suggestions are possible after saving.

#### Scenario: Suggestions for a PDF attachment
- **WHEN** a user asks for keyword suggestions for a 20 MB PDF attachment with a text layer in the save dialog
- **THEN** the text is read locally, nothing is uploaded and the suggestions are shown

#### Scenario: Office document above the limit
- **WHEN** the size limit is 3 MB and a user asks for suggestions for a 5 MB DOCX file in the save dialog
- **THEN** no suggestions are made and a hint explains that they are possible after saving

#### Scenario: Keywords for many files without dialogs
- **WHEN** a user runs "Schlagworte eintragen" for 12 files in the save dialog
- **THEN** the suggested keywords are added to each file without a dialog per file, and skipped files are listed with their reason

#### Scenario: Scanned PDF
- **WHEN** a user asks for suggestions for a PDF without text layer in the save dialog
- **THEN** nothing is uploaded and a hint explains that suggestions are possible after saving and text recognition
