# case-linking Specification

## Purpose
Cases that belong together but must stay separate files - the criminal matter and the civil
damages claim out of one accident, a case per family member, the case a title is enforced
from, a follow-up instruction - can be linked to each other with an optional free-text
description. A link is symmetric and navigable from either case, is shown in the case header
of the desktop client and on the case overview in the web client, and is exposed over the
REST API. Creating a new case from an existing one and linking the two in one step is part of
this capability.
## Requirements
### Requirement: Case Link Model

The system SHALL be able to link two cases (Akten) to each other. A link SHALL be stored
exactly once per pair of cases in a dedicated table (`case_links`, entity `CaseLink`)
and SHALL carry an optional free-text description (e.g. "Gegenakte", "Folgesache zu 12/24"),
the creation timestamp and the user who created it.

A link SHALL be symmetric: it SHALL NOT distinguish a source from a target case, and the
same two cases SHALL NOT be linkable twice, regardless of which of them the user starts
from. To make that enforceable in the database, the service SHALL normalise the column
order of the two case references before insert, and a unique database index SHALL cover the
normalised pair. A case SHALL NOT be linkable to itself.

Both case references SHALL be foreign keys on `cases(id)` with `ON DELETE CASCADE`, so
deleting a case removes its links and never leaves a link pointing at a case that no longer
exists.

#### Scenario: Link two cases

- **WHEN** a user links case A to case B with the description "Gegenakte"
- **THEN** one row is stored referencing both cases, with the description, the creation
  timestamp and the acting user

#### Scenario: Duplicate link rejected from either side

- **GIVEN** case A and case B are already linked
- **WHEN** a user tries to link A to B, or B to A
- **THEN** no second link is created
- **AND** the caller is told that the two cases are already linked

#### Scenario: Self-link rejected

- **WHEN** a user tries to link case A to case A
- **THEN** no link is created and the caller is told that a case cannot be linked to itself

#### Scenario: Deleting a case removes its links

- **GIVEN** case A is linked to case B and to case C
- **WHEN** case A is deleted
- **THEN** both links are gone
- **AND** cases B and C still exist and each show one link less

### Requirement: Bidirectional Link Navigation

Reading the links of a case SHALL return, for every link, the *other* case of that link,
identified by at least its id, file number (Aktenzeichen), short name (Kurzrubrum) and
archived flag, together with the link's description. A client SHALL NOT have to determine
which of two stored case references is the other one.

Consequently a link created from case A SHALL be visible when reading case B, with case A
as the other case, without any additional write.

#### Scenario: Link is visible from both cases

- **GIVEN** a user linked case A to case B
- **WHEN** the links of case B are read
- **THEN** the result contains one link whose other case is A, with A's file number, short
  name and archived flag and the link's description

#### Scenario: Archived linked case is marked

- **GIVEN** case A is linked to the archived case B
- **WHEN** the links of case A are read
- **THEN** the entry for B is flagged as archived

### Requirement: Reading Links Is Cheap Enough For Every Case Load

Because the links are shown whenever a case is opened, reading them SHALL be cheap in both
payload and queries. Reading the links of a case SHALL transport only the fields the display
needs — the link's own data plus the other case's id, file number, short name, reason and
archived flag — and SHALL NOT transport case entities, their folder trees, their documents,
parties, deadlines or any other of their child collections.

Reading links SHALL cost at most a constant number of database queries, independent of the
number of links, and SHALL NOT require enumerating the cases a user is allowed to see in
order to filter the result. In the desktop client the links SHALL be loaded together with
the other case details in the existing parallel load, so that opening a case needs no
additional sequential round-trip, and the case editor SHALL become usable regardless of how
long the link query takes.

#### Scenario: Payload stays flat

- **WHEN** the links of a case with five links are read
- **THEN** the result contains only the link data and the five other cases' identifying
  fields
- **AND** no case folder tree, document list, party list or deadline list is part of the
  result

#### Scenario: Query count independent of link count

- **WHEN** the links of a case with one link and of a case with fifty links are read
- **THEN** both reads issue the same number of database queries

#### Scenario: No extra round-trip when opening a case

- **WHEN** a case is opened in the desktop client
- **THEN** the links are fetched in parallel with the other case details
- **AND** opening the case is not measurably slower than before this change

### Requirement: Link Visibility Follows Case Permissions

Case links SHALL NOT widen access to cases. Reading the links of a case SHALL require the
caller's usual read access to that case and SHALL fail the same way as any other read on it
when access is missing. Links whose other case the caller is not allowed to see SHALL be
omitted from the result, and SHALL NOT be reported as an existing-but-hidden entry, so that
neither the existence nor the file number of a restricted case is disclosed.

Creating a link SHALL require write access to the case the user acts on and read access to
the other case. Removing a link SHALL require write access to at least one of the two cases.

#### Scenario: Restricted other case is omitted

- **GIVEN** case A is linked to case B, and the calling user is not a member of any group
  allowed on case B
- **WHEN** the links of case A are read
- **THEN** the link to B is not contained in the result and nothing indicates that it exists

#### Scenario: Linking to an invisible case is refused

- **WHEN** a user who may not read case B tries to link case A to case B
- **THEN** no link is created and the operation is refused

#### Scenario: Read-only user cannot link

- **WHEN** a user without write permission on case A tries to link or unlink case A
- **THEN** the operation is refused and no link is created or removed

### Requirement: Case Link Service API

`ArchiveFileService` SHALL expose case links through its Remote and Local interfaces:
reading all links of a case, creating a link between two cases with an optional
description, updating a link's description, and removing a link. The remote interface
methods SHALL carry English JavaDoc, and the methods SHALL be guarded by the same roles as
the comparable existing case operations (read role for reading, write role for creating,
updating and removing).

The remote signatures SHALL expose links as a flat data transfer object and SHALL NOT expose
the link entity, so that no caller can pull case entities and their eagerly loaded folder
trees through a link read.

Creating and removing a link SHALL write a case history entry ("Historie") on **both**
cases, naming the other case, so the change is traceable from either side and both cases
count as changed. Updating only the description SHALL NOT write a history entry.

#### Scenario: Linking writes history on both cases

- **WHEN** case A is linked to case B
- **THEN** case A's history contains an entry naming B
- **AND** case B's history contains an entry naming A
- **AND** both cases' "last changed" timestamps are updated

#### Scenario: Unlinking writes history on both cases

- **WHEN** the link between case A and case B is removed
- **THEN** both cases have a history entry recording the removal

#### Scenario: Description update is silent

- **WHEN** the description of an existing link is changed
- **THEN** the new description is stored
- **AND** no history entry is written

### Requirement: Linked Cases In The Case Header

The desktop case editor SHALL show the links of the open case in the case header
("Aktenkopf") on its first tab ("Allgemeine Daten"), in a row below the subject field
("Sachgebiet"). Each link SHALL be rendered as one compact entry carrying the other case's
file number, an archived marker and a short label of at most 20 characters. That label SHALL
be what the other case is about ("wegen"), because it says more about the other case than the
link's own description does; only where that subject is empty SHALL the link description take
its place. The other case's short name (Kurzrubrum) and the full, untruncated description
SHALL be reachable without opening the case, at least as a tooltip.

The entries SHALL be laid out flowing, so that several of them stand next to each other and
wrap to a further line only when the available width is exhausted — the layout the case tag
panel ("Akten-Etiketten") already uses. The row SHALL grow only with the number of lines
actually needed, and a case without links SHALL consume next to no vertical space: no
permanent heading, border or empty-state text SHALL be introduced for it, and the case
header SHALL NOT become taller than it is today for a case whose links fit on one line.

The row SHALL be present in both the editable and the read-only case editor. Loading the
links SHALL NOT block the case editor from becoming usable, following the way the other
parts of that tab are populated.

Navigating to a linked case SHALL first settle pending changes of the case shown at that
moment - offering to save them, to discard them or to stay - because the editor reuses one
instance for every case and would otherwise overwrite them silently. The same SHALL apply
before a new linked case is created from the open one.

#### Scenario: Links shown in the case header

- **WHEN** a case with two links is opened in the desktop client
- **THEN** the case header shows two entries below the subject field, each with the other
  case's file number and what that case is about, side by side

#### Scenario: Entry label falls back to the link description

- **GIVEN** a linked case whose subject ("wegen") is empty
- **WHEN** the links are shown
- **THEN** that entry shows the link's description instead
- **AND** a label longer than 20 characters is truncated, with the full text in the tooltip

#### Scenario: Many links wrap instead of widening

- **GIVEN** a case with more links than fit the available width
- **WHEN** the case is opened, and when the window is later narrowed
- **THEN** the entries wrap onto further lines and all of them stay visible and clickable

#### Scenario: No links costs no space

- **WHEN** a case without links is opened
- **THEN** no heading, border or placeholder text for links is shown
- **AND** the case header is no taller than for the same case before this change

#### Scenario: Navigating to a linked case and back

- **GIVEN** case A shows an entry for case B
- **WHEN** the user clicks the entry for B
- **THEN** case B is opened in the main editor pane
- **AND** case B's case header shows an entry back to case A

#### Scenario: Navigating away from a case with unsaved changes

- **GIVEN** the user has edited the open case without saving
- **WHEN** they click a link entry
- **THEN** they are asked whether to save before leaving
- **AND** saving navigates to the linked case, discarding navigates without saving, and
  cancelling keeps the edited case open with its changes intact

#### Scenario: Archived linked case is recognisable

- **GIVEN** case A is linked to the archived case B
- **WHEN** case A is opened
- **THEN** the entry for B is visually marked as archived

### Requirement: Link Actions In The Case Header

Each link entry SHALL offer, in addition to opening the linked case, editing the link's
description and removing the link after a confirmation. Adding SHALL be offered by one
permanent control at the end of the row that leads to both creation actions — linking an
existing case and creating a new linked case — so that no separate toolbar or heading is
needed.

Opening a linked case SHALL be the entry's primary gesture. The maintenance actions (edit
description, remove) MAY be placed in a per-entry context menu, but SHALL be reachable in
one gesture from the entry and SHALL follow the way the other lists of the case editor offer
their per-row actions.

In the read-only case editor and while the case is new and unsaved, the control for adding
SHALL NOT be shown and no action that changes a link SHALL be offered; opening a linked case
SHALL remain possible.

#### Scenario: Both creation actions behind one control

- **WHEN** the user activates the add control at the end of the links row
- **THEN** exactly two actions are offered: linking an existing case and creating a new
  linked case

#### Scenario: Remove a link

- **WHEN** the user chooses to remove a link entry and confirms
- **THEN** the link is deleted and the entry disappears without reloading the case

#### Scenario: Edit a description

- **WHEN** the user edits the description of a link entry
- **THEN** the new description is stored and the entry shows it immediately

#### Scenario: Read-only editor

- **WHEN** a case is opened in the read-only editor
- **THEN** the links are shown and navigable
- **AND** neither the add control nor an action that changes a link is offered

#### Scenario: Unsaved new case

- **WHEN** a new case has not been saved yet
- **THEN** no add control is offered, so no link can be created for a case that does not
  exist yet

### Requirement: Link An Existing Case From The Case

The desktop case editor SHALL offer an action "bestehende Akte verknüpfen" from the case
header's links row. The action SHALL let the user find and pick a case with the client's
existing case picker, SHALL let the user enter an optional description, and SHALL then
create the link and add it to the row without reloading the case.

If the picked case is the open case itself, or is already linked to it, the action SHALL say
so and create nothing.

#### Scenario: Link an existing case

- **WHEN** the user triggers "bestehende Akte verknüpfen", picks case B and enters
  "Gegenakte"
- **THEN** the link is created with that description
- **AND** the new entry appears in the links row immediately

#### Scenario: Picker cancelled

- **WHEN** the user closes the case picker without picking a case
- **THEN** nothing is created and the links row is unchanged

#### Scenario: Already linked case picked

- **GIVEN** case A is already linked to case B
- **WHEN** the user triggers "bestehende Akte verknüpfen" on A and picks B
- **THEN** the user is told that the cases are already linked and no second link is created

#### Scenario: Own case picked

- **WHEN** the user picks the case that is currently open
- **THEN** the user is told that a case cannot be linked to itself and nothing is created

### Requirement: Create A New Linked Case

The desktop case editor SHALL offer an action "neue verknüpfte Akte erstellen" from the
case header's links row. The action SHALL create a new case from the open one with the
same copy scope as the existing "duplizieren" action, SHALL link the new case to the open
one, and SHALL then open the new case in the editor so the user can continue working in it.

Before creating, the action SHALL let the user adjust the new case's short name
(pre-filled from the open case), choose whether the case data (Falldaten/forms) are copied
as well, and enter the link description. The new case SHALL receive its own file number from
the server's numbering, and SHALL NOT inherit the source's claim number, claim value,
external id, archived state, documents, deadlines, invoices, timesheets or history.

If creating the case succeeds but linking fails, the new case SHALL be kept and the user
SHALL be told that the link was not created.

#### Scenario: New linked case created

- **WHEN** the user triggers "neue verknüpfte Akte erstellen" on case A, confirms the
  pre-filled short name and enters "Folgesache"
- **THEN** a new case is created with its own file number, carrying A's master data,
  parties, tags and allowed groups
- **AND** the two cases are linked with the description "Folgesache"
- **AND** the new case is opened in the editor and shows the link back to A

#### Scenario: Case data included on request

- **WHEN** the user ticks the option to copy the case data (Falldaten)
- **THEN** the new case also contains the source's forms and their values

#### Scenario: Nothing copied that must stay unique

- **WHEN** a new linked case is created from a case that is archived and has a claim number,
  a claim value, an external id and documents
- **THEN** the new case is not archived and has none of those values and no documents

#### Scenario: Dialog cancelled

- **WHEN** the user cancels the dialog
- **THEN** no case is created and no link is created

### Requirement: Shared Case Duplication Implementation

Case duplication SHALL exist once in the desktop client and SHALL be used both by the
existing "duplizieren" actions in the case search result list and by "neue verknüpfte Akte
erstellen". The existing popup actions SHALL keep their current behaviour, including
appending a copy marker to the short name and the separate variant that includes the case
data (Falldaten).

#### Scenario: Popup duplication unchanged

- **WHEN** the user duplicates two selected cases from the search result popup
- **THEN** two new cases are created exactly as before, with the copy marker in the short
  name, and no links are created

#### Scenario: One implementation

- **WHEN** the copy scope of duplication changes
- **THEN** both the popup actions and "neue verknüpfte Akte erstellen" reflect the change
  without a second implementation being edited

### Requirement: Case Links REST API

The REST API SHALL expose case links in its current version (v8) as a sub-resource of a
case: reading all links of a case, creating a link to another case with an optional
description, updating a link's description, and deleting a link. The returned
representation SHALL describe the other case as seen from the requested case (id, file
number, short name, archived flag) together with the link's id, description, creation
timestamp and creator.

The endpoints SHALL use the API's existing authentication, SHALL report errors through the
uniform error envelope, SHALL enforce the same permission rules as the service layer, and
SHALL be additive — no existing API version changes behaviour.

#### Scenario: Read links of a case

- **WHEN** a client requests the links of a case
- **THEN** the response lists each link with its description and the other case's id, file
  number, short name and archived flag

#### Scenario: Create a link

- **WHEN** a client sends a link request for a case naming another case id and a description
- **THEN** the link is created and the created link is returned

#### Scenario: Create a duplicate link

- **GIVEN** the two cases are already linked
- **WHEN** a client sends the same link request again
- **THEN** the request is rejected with an error stating that the link already exists, and
  no second link is created

#### Scenario: Delete a link

- **WHEN** a client deletes a link of a case
- **THEN** the link is removed and reading the links of either case no longer returns it

#### Scenario: Unknown case or link

- **WHEN** a client requests links for a case id that does not exist, or deletes a link id
  that does not belong to the given case
- **THEN** the request fails with a not-found error in the uniform error envelope

### Requirement: Linked Cases In The Web Client

The web client SHALL show the links of the selected case as a "verknüpfte Akten" card on the
case overview tab, in the style of the existing cards on that tab, with one entry per link
showing the other case's file number, short name, archived marker and the link description,
and an empty-state text when there are none.

Selecting an entry SHALL navigate to that case using the existing single-case deep link, so
the user can follow links in both directions. The card SHALL allow adding a link by picking
another case and removing a link after a confirmation. All texts SHALL come from the
translation bundles for German and English.

#### Scenario: Links on the overview tab

- **WHEN** a case with links is selected in the web client
- **THEN** the overview tab shows a "verknüpfte Akten" card listing them

#### Scenario: Navigate to a linked case

- **WHEN** the user selects a linked case in the card
- **THEN** the application navigates to that case's detail view, whose overview tab shows
  the link back

#### Scenario: Add and remove from the web client

- **WHEN** the user picks another case in the card's add control
- **THEN** the link is created and appears in the card
- **AND** removing it after confirmation deletes it and updates the card

#### Scenario: Both languages

- **WHEN** the client language is English
- **THEN** the card's title, empty state and actions are shown in English

