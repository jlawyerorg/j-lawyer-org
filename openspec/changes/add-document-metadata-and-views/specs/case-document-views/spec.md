## ADDED Requirements

### Requirement: Two Switchable Document Views
The documents area of a case in the desktop client SHALL offer two views: (1) "Liste +
Vorschau" with folders on the left, a document list in the middle and a preview area on the
right, and (2) "Tabelle" with folders on the left and a table with one row per document in
the remaining area. A toggle in the documents toolbar SHALL switch between the views at any
time. Switching SHALL preserve the selected folders, the selected documents, the sort order
and the active filter. The chosen view SHALL be stored as a user setting on the server,
so that it applies on every workstation the user logs in to, and SHALL be restored when a
case is opened.

#### Scenario: Switching keeps the selection
- **WHEN** a user has selected three documents in the list view and switches to the table view
- **THEN** the same three documents are selected in the table and the folder selection is unchanged

#### Scenario: View remembered
- **WHEN** a user switches to the table view, closes the case and opens another case
- **THEN** the documents are shown in the table view

#### Scenario: View follows the user
- **WHEN** a user switches to the table view on one workstation and later logs in on another workstation
- **THEN** the documents are shown in the table view there as well

### Requirement: All Metadata Visible in Both Views
Both views SHALL make every document attribute visible: title (or file name), file name,
file type, favorite, correspondent with direction, received date, creation date, change
date, keywords, folder, size, dictation sign, tags, highlights, lock state, linked invoice,
number of linked instant messages and number of attachments. In the list view, attributes
MAY be reduced to an icon indicator (e.g. a message icon with count); in the table view,
each attribute SHALL be available as a column.

#### Scenario: Message indicator in list view
- **WHEN** a document has two linked instant messages
- **THEN** its list entry shows a message icon with the number 2
- **AND** a document without messages shows no message icon

#### Scenario: Correspondent direction shown
- **WHEN** a document has an incoming correspondent "RA Müller"
- **THEN** the list view and the table view show "↘ RA Müller", and an outgoing correspondent is shown with "↗"

### Requirement: List View Layout
The list view SHALL show each document as a two-line entry: the first line with selection
checkbox, favorite, file type icon, title (file name if no title) and status icons; the
second line, visually subdued, with correspondent, received date, keywords, file name and
size. The preview area SHALL provide the tabs "Vorschau", "Details" (all metadata, editable
with write permission) and "Nachrichten" (linked instant messages and creating a new
message for the document). Clicking the message indicator SHALL open the "Nachrichten" tab.

#### Scenario: Details tab
- **WHEN** a user selects a document and opens the "Details" tab
- **THEN** title, keywords, received date, correspondent, parent and tags are shown and can be changed and saved

#### Scenario: Message indicator opens messages
- **WHEN** a user clicks the message indicator of a document
- **THEN** the document is selected and the "Nachrichten" tab shows its linked messages

### Requirement: Table View Layout
The table view SHALL show one row per document in a table whose appearance follows the
list view (same fonts, icons and row height, no grid lines, subtle row separators, hover
highlight, highlight colors as row background). Users SHALL be able to show, hide, reorder
and resize columns; the column configuration (visibility, order and width of each column)
SHALL be stored as a user setting on the server. Columns added in later versions and not
contained in a stored configuration SHALL be shown with their default settings. Clicking a column
header SHALL sort by that column. Title and keywords SHALL be editable inline with write
permission. Double-clicking a row SHALL open the document as in the list view. Clicking the
message indicator SHALL show the linked messages in a popup. The preview area SHALL be
hidden in the table view and restored when switching back. A preview column next to the file type SHALL
open the internal document viewer in a modal dialog, so users can choose between the internal
viewer and the external application (double click).

#### Scenario: Sort by received date
- **WHEN** a user clicks the column header "Eingang"
- **THEN** the documents are sorted by received date, and a second click reverses the order

#### Scenario: Hide a column
- **WHEN** a user hides the column "Größe" via the header context menu and reopens the case later
- **THEN** the column "Größe" is still hidden

#### Scenario: Column configuration follows the user
- **WHEN** a user moves the column "Von/An" to the first position on one workstation and later opens a case on another workstation
- **THEN** the column "Von/An" is the first column there as well

#### Scenario: Internal preview from the table
- **WHEN** a user clicks the preview icon of a row in the table view
- **THEN** the document is shown with the same viewer as in the preview area of the list view, in a modal dialog

#### Scenario: Inline edit of keywords
- **WHEN** a user edits the keywords cell of a row and confirms
- **THEN** the keywords are saved and normalized

### Requirement: Hierarchical Display
Both views SHALL show child documents indented below their parent and allow collapsing and
expanding a parent. A parent SHALL show the number of its attachments. A child whose parent
is not visible under the current folder selection SHALL be shown at top level with a hint
naming its parent. Sorting SHALL apply to top-level entries and, separately, to the children
of each parent.

#### Scenario: Collapse attachments
- **WHEN** a user collapses an e-mail document with three attachments
- **THEN** the attachments are hidden and the e-mail shows an attachment indicator with 3

#### Scenario: Child in another folder
- **WHEN** an attachment lies in folder "Beweise" while its e-mail lies in "Korrespondenz" and only "Beweise" is selected
- **THEN** the attachment is shown at top level with the hint "Anlage zu <Bezeichnung der E-Mail>"

### Requirement: Identical Behavior Across Views
Both views SHALL behave identically for context menu, drag and drop (into folders, to
other applications, onto other cases), multi-selection, keyboard shortcuts, read-only
mode, document locking indication and the quick filter. The quick filter SHALL match title,
file name, keywords and correspondent name.

#### Scenario: Context menu in table view
- **WHEN** a user right-clicks a row in the table view
- **THEN** the same document context menu as in the list view is shown

#### Scenario: Quick filter matches keywords
- **WHEN** a user types `Frist` into the quick filter
- **THEN** documents whose title, file name, keywords or correspondent name contain `Frist` are shown in either view

### Requirement: Document Properties Dialog
The document context menu SHALL offer "Eigenschaften…", which opens a dialog to edit the
metadata of the selected document, or, for several selected documents, a bulk edit where
each field is only changed if the user activates it and keywords can be set, added or
removed.

#### Scenario: Bulk edit from the context menu
- **WHEN** a user selects four documents, opens "Eigenschaften…", activates "Von/An" and chooses the contact "Mandant Meier" as incoming
- **THEN** all four documents get "Von" = "Mandant Meier" and all other fields remain unchanged

### Requirement: Web Client Document Metadata
The web client's documents tab of a case SHALL display title (file name as fallback),
correspondent with direction, received date, keyword chips, message and attachment
indicators and the document hierarchy with expandable parents. The single-document actions
and the bulk action bar SHALL allow editing the metadata with the same semantics as the
desktop client. Linked instant messages of a document SHALL be viewable.

#### Scenario: Web list shows metadata
- **WHEN** a user opens the documents tab of a case in the web client
- **THEN** each document shows its title, correspondent, received date and keywords, and parents can be expanded to show their attachments

#### Scenario: Web bulk keyword edit
- **WHEN** a user selects several documents in the web client and adds a keyword via the bulk bar
- **THEN** each selected document additionally carries that keyword

### Requirement: AI Keyword Suggestions
The desktop client SHALL provide an AI button wherever keywords can be edited — the
"Details" tab, the inline keyword editor of the table view and the properties dialog for
one or several documents. The AI button SHALL offer
all configured assistant prompts of the request types "chat" and "extract". Selecting a prompt SHALL run
it with the document's extracted text, title and current keywords as input and SHALL
present the keywords parsed from the answer as suggestions. Suggestions SHALL only be added
to the keyword input after the user accepts them, and SHALL NOT be saved without an
explicit save by the user. For several documents the prompt SHALL be run per document and
accepted suggestions SHALL be added per document. If no assistant or no prompt of these
types is configured, the AI button SHALL be disabled with an explanatory tooltip.
The desktop client SHALL also provide such an AI button next to the title of a single
document; it SHALL offer the same prompts and propose one title taken from the answer, which
is only put into the title input after the user accepts it.

#### Scenario: Prompts offered
- **WHEN** the prompts "Schlagworte vorschlagen" (extract) and "Rechtsgebiet bestimmen" (chat) and a summarize prompt are configured and a user clicks the AI button next to the keywords
- **THEN** the menu offers "Schlagworte vorschlagen" and "Rechtsgebiet bestimmen" and does not offer the summarize prompt

#### Scenario: Suggestions accepted
- **WHEN** the selected prompt answers "Frist; Kostenfestsetzung\n- Vergleich" and the user accepts "Frist" and "Vergleich"
- **THEN** "Frist" and "Vergleich" are added to the keyword input and nothing is saved until the user saves

#### Scenario: Bulk suggestions
- **WHEN** a user runs a prompt from the properties dialog with three documents selected
- **THEN** the prompt is run for each of the three documents and the suggestions are shown per document

#### Scenario: No assistant configured
- **WHEN** no assistant prompt of type chat or extract is configured
- **THEN** the AI button is disabled and its tooltip explains why

#### Scenario: Title suggested
- **WHEN** a user clicks the AI button next to the title in the "Details" tab and picks a prompt
- **THEN** the first line of the answer is offered as title for review and, when accepted, put into the title input without saving it

### Requirement: Active Tags Shown on Documents
The separate "Dokument-Etiketten" panel below the document list SHALL be removed. The
active tags of each document SHALL be shown directly with the document: in the list view as
compact chips (boolean tags by name, multi-value tags as "name: value", overflow as "+N"
with a tooltip), in the table view in a "Etiketten" column that is visible by default.
Inactive tags SHALL NOT be shown. Tags SHALL be edited together with the other metadata in
the "Details" tab and in the properties dialog (for one or several documents); the tag
entries of the document context menu SHALL remain available. The tags for the document
list SHALL be loaded in one request per case.

#### Scenario: Active tags in the list
- **WHEN** a document has the boolean tag "Wichtig" active and the multi-value tag "Dokumentstatus" set to "Geprüft"
- **THEN** its list entry shows the chips "Wichtig" and "Dokumentstatus: Geprüft" and no chips for inactive tags

#### Scenario: Edit tags with metadata
- **WHEN** a user opens the "Details" tab of a document and activates the tag "Wichtig"
- **THEN** the tag is set on the document and the chip "Wichtig" appears in the list entry

#### Scenario: Bulk tag edit
- **WHEN** a user selects five documents, opens "Eigenschaften…" and activates the tag "Beweismittel"
- **THEN** all five documents carry the tag "Beweismittel"

#### Scenario: More vertical space
- **WHEN** a user opens the documents of a case
- **THEN** no separate tag panel is shown below the document list

### Requirement: Attaching Documents by Drag and Drop
Both views SHALL allow dragging one or several documents onto another document of the same
case; the dragged documents SHALL then become attachments (children) of the document they are
dropped on. The document under the cursor SHALL be highlighted while dragging. Drops that would
create a cycle or come from another case SHALL be refused with a hint. Dragging documents onto
folders, dragging them out of the client and dropping files from outside the client SHALL keep
working unchanged; files dropped on a document SHALL be uploaded as before.

#### Scenario: Attach two documents
- **WHEN** a user selects two documents and drops them on the e-mail document "AW: Vergleichsangebot"
- **THEN** both documents are shown as attachments below the e-mail document

#### Scenario: Cycle refused
- **WHEN** a user drops an e-mail document on one of its own attachments
- **THEN** nothing is changed and a hint explains why

#### Scenario: Other drop targets unchanged
- **WHEN** a user drops documents on a folder, drags them into a file manager or drops a file from the desktop on a document
- **THEN** the documents are moved to the folder, exported as files or the file is uploaded, as before
