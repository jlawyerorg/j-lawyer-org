## ADDED Requirements

### Requirement: Relationship Type Catalogue

The system SHALL hold an administrable catalogue of relationship types between contacts.
Each type SHALL carry a catalogue name, a label for each of its two directions (e.g.
"ist Mutter von" and "ist Kind von"), a flag marking it as symmetric, a category for
grouping, a colour, a sequence number and an active flag.

A symmetric type SHALL read the same in both directions; the system SHALL keep its two
labels identical rather than requiring the administrator to enter the same text twice.

Catalogue names SHALL be unique and SHALL NOT be empty. An inactive type SHALL NOT be
offered when a new relationship is created, but relationships already using it SHALL keep
working and keep their wording. Deleting a type SHALL be refused while relationships still
reference it, and the refusal SHALL say how many.

#### Scenario: Directed type with two labels

- **WHEN** an administrator creates the type "Mutter – Kind" with the labels "ist Mutter von"
  and "ist Kind von"
- **THEN** both labels are stored with the type and offered when a relationship is created

#### Scenario: Symmetric type

- **WHEN** an administrator marks a type as symmetric and enters "ist Ehepartner von"
- **THEN** the same text applies to both directions without being entered twice

#### Scenario: Duplicate catalogue name rejected

- **WHEN** an administrator creates a type whose name an existing type already has
- **THEN** the type is not created and the administrator is told why

#### Scenario: Type in use cannot be deleted

- **GIVEN** three relationships use the type "Mutter – Kind"
- **WHEN** an administrator deletes that type
- **THEN** the deletion is refused, naming the number of relationships that use it

#### Scenario: Deactivated type disappears from the pickers only

- **GIVEN** a type is set inactive
- **WHEN** a user creates a new relationship
- **THEN** that type is not offered
- **AND** existing relationships of that type are still shown with their label

### Requirement: Seeded Default Catalogue

The system SHALL ship a substantial default catalogue of German relationship types, grouped
by category, so relationships are usable without configuration. It SHALL cover at least
family and partnership, legal representation and custodianship, company roles, contractual
and insurance relationships, and a residual category.

The defaults SHALL be seeded idempotently with fixed identifiers, so that re-running the
seed cannot create duplicates, and SHALL NOT overwrite a type a firm has edited.

#### Scenario: Usable right after an upgrade

- **WHEN** an existing installation is upgraded
- **THEN** the catalogue contains the default types, grouped by category
- **AND** a user can record "ist Mutter von" without an administrator configuring anything

#### Scenario: Firm edits survive

- **GIVEN** a firm renamed a seeded type's labels
- **WHEN** the installation is upgraded again
- **THEN** the firm's wording is still in place and no second copy of the type appears

### Requirement: Directed Contact Relationship Model

The system SHALL store a relationship between two contacts once, as a directed statement:
one contact on the `from` side, the other on the `to` side, with a relationship type and an
optional free-text note. Which side a contact sits on SHALL determine which of the type's
two labels describes it.

A contact SHALL NOT be relatable to itself. The same statement — same pair, same direction,
same type — SHALL NOT be storable twice. For a symmetric type the two directions are the
same statement, so the system SHALL normalise the order of the two contacts before storing
and SHALL then reject the mirrored duplicate as well.

Deleting a contact SHALL remove its relationships and SHALL NOT leave a relationship
pointing at a contact that no longer exists.

#### Scenario: Directed relationship recorded once

- **WHEN** a user records that contact A is the mother of contact B
- **THEN** one relationship is stored with A on the from side, B on the to side and the type
  "Mutter – Kind"

#### Scenario: Duplicate statement rejected

- **GIVEN** A is already recorded as the mother of B
- **WHEN** the same statement is recorded again
- **THEN** it is rejected and the user is told that the relationship already exists

#### Scenario: Opposite direction of a directed type is a different statement

- **GIVEN** A is recorded as the mother of B
- **WHEN** a user records that B is the mother of A
- **THEN** that is stored as its own relationship, because the direction is what carries the
  meaning

#### Scenario: Mirrored symmetric relationship rejected

- **GIVEN** A is recorded as the spouse of B with a symmetric type
- **WHEN** a user records that B is the spouse of A
- **THEN** it is rejected as already existing, and no second row is stored

#### Scenario: Self-relationship rejected

- **WHEN** a user tries to relate a contact to itself
- **THEN** no relationship is created and the user is told why

#### Scenario: Deleting a contact removes its relationships

- **GIVEN** contact A has relationships to B and C
- **WHEN** A is deleted
- **THEN** both relationships are gone and B and C still exist

### Requirement: Relationships Are Readable From Either Contact

Reading the relationships of a contact SHALL return, for each of them, the label that applies
as seen from that contact, the other contact identified by at least its id and display name,
the type and the note. A client SHALL NOT have to decide which end it is looking at, nor
which of the type's two labels applies.

A relationship recorded on one contact SHALL therefore be visible on the other one, with the
opposite label, without any additional write.

#### Scenario: Both sides read correctly

- **GIVEN** A is recorded as the mother of B
- **WHEN** the relationships of A are read
- **THEN** the entry reads "ist Mutter von" and names B
- **AND** reading the relationships of B yields "ist Kind von" and names A

#### Scenario: Symmetric relationship reads the same both ways

- **GIVEN** A and B are spouses
- **WHEN** the relationships of either contact are read
- **THEN** both read "ist Ehepartner von" and name the other contact

#### Scenario: Reading stays flat

- **WHEN** the relationships of a contact are read
- **THEN** only the fields the display needs are transported
- **AND** no contact entities with their tags, documents or case participations are part of
  the result

### Requirement: Relationships Are Never Loaded With A Contact

Relationships SHALL NOT be part of the contact itself. Loading a contact SHALL NOT load its
relationships, and the contact entity SHALL NOT expose them as a mapped collection, so that
no access path can walk from one contact into the surrounding network.

Relationships SHALL only be obtainable through their own read, for one contact at a time.
Every consumer SHALL follow that rule: the contact editor SHALL read them when its
relationship section is first opened rather than on every contact load, the graph SHALL load
one ring at a time and only expand further on an explicit request, and the delete warning
SHALL read counts rather than the relationships themselves. Creating a relationship SHALL
take the two contacts by their identifiers, not as loaded contacts.

#### Scenario: Opening a contact loads no relationships

- **WHEN** a contact is opened in the desktop client
- **THEN** no relationship of that contact is read
- **AND** the relationships are read only once the relationship section is opened

#### Scenario: No traversal through the contact

- **WHEN** a contact is read through any interface
- **THEN** the result contains no relationships and therefore no related contacts
- **AND** no lazy access on it can reach the network

#### Scenario: Graph grows ring by ring

- **WHEN** the network view is opened for a contact
- **THEN** only that contact's own relationships are read
- **AND** a further ring is read only when the user expands a node

#### Scenario: Delete warning stays cheap

- **WHEN** several contacts are selected for deletion
- **THEN** one read returns the number of relationships per contact
- **AND** no relationships or related contacts are transported for it

### Requirement: Adding A Related Contact As A Case Party

A party of a case SHALL offer adding one of its related contacts to the same case as a
further party — the managing director of a party company, the legal representative of a
party, the spouse in a family matter. The offer SHALL be reachable from the party's own
actions in the case's party list, SHALL name each candidate with the direction-correct label
and the contact's name, and SHALL let the user pick the role (Beteiligtentyp) the new party
takes in the case.

The offer SHALL exist in every client that can add a party to a case, so the shortcut is not
a desktop privilege.

Adding SHALL use the same path as adding any other party, so the new party appears in the
list immediately and is stored the same way. A related contact that is already a party of
that case SHALL be shown as such and SHALL NOT be addable twice. Where the party has no
relationships, the offer SHALL be visibly unavailable rather than silently missing.

Determining the candidates SHALL NOT happen while the case is being loaded; it SHALL happen
when the user asks for them.

This offer SHALL be the only place relationships appear in the case's party list: the list
SHALL NOT show a party's relationships permanently, neither as an additional line nor as a
marker on another party. Seeing the whole picture is what the contact's relationship section
and the network views are for.

#### Scenario: Managing director added from the company

- **GIVEN** a GmbH is a party of the case and has a related contact labelled
  "ist Geschäftsführer von"
- **WHEN** the user opens that party's actions and chooses the related contact
- **THEN** the user is asked for the role the new party takes in the case
- **AND** the contact is added as a party of the case and appears in the party list at once

#### Scenario: Already a party

- **GIVEN** a related contact of a party is itself already a party of the case
- **WHEN** the user opens the offer
- **THEN** that contact is marked as already involved and cannot be added again

#### Scenario: Party without relationships

- **WHEN** the user opens the actions of a party that has no relationships
- **THEN** the offer is shown as unavailable

#### Scenario: Shortcut in the web client

- **GIVEN** a party of the case has related contacts
- **WHEN** the user adds a party in the web client
- **THEN** that party's related contacts are offered with their label as a shortcut
- **AND** choosing one pre-fills the contact so only the role has to be picked

#### Scenario: Case load stays unaffected

- **WHEN** a case with ten parties is opened
- **THEN** no party's relationships are read
- **AND** they are read only for the party whose offer the user opens

#### Scenario: Party list stays unchanged otherwise

- **WHEN** a case whose parties have relationships to each other is opened
- **THEN** the party list looks as it does without relationships
- **AND** the relationships are visible only after opening a party's actions

### Requirement: Relationship Maintenance At A Contact

The desktop contact editor SHALL show the relationships of the open contact in a dedicated
section, listing one row per relationship with the direction-correct label, the other
contact and the note. The section SHALL be loaded when it is first opened rather than on
every contact load, following the way the contact's cases are loaded today, and SHALL state
when there are none.

Clicking a row SHALL open the other contact in the editor, using the same mechanism as every
other "open a contact" action in the client, so the user can walk the network. The section
SHALL offer adding a relationship — picking the label (which fixes type and direction),
the other contact and an optional note — as well as editing the note and removing a
relationship after a confirmation. Where the contact is shown read-only, the relationships
SHALL be listed and navigable but not editable.

#### Scenario: Relationships shown at the contact

- **WHEN** a contact with two relationships is opened and the section is selected
- **THEN** both are listed with their label, the other contact and the note

#### Scenario: Adding from either side

- **WHEN** the user adds "ist Kind von" pointing at contact P on contact C
- **THEN** the relationship is stored
- **AND** opening P shows the same relationship as "ist Mutter von" pointing at C, if that is
  the type's opposite label

#### Scenario: Navigating the network

- **GIVEN** contact A lists a relationship to B
- **WHEN** the user clicks that row
- **THEN** contact B is opened, and its own relationships list the one back to A

#### Scenario: Removing a relationship

- **WHEN** the user removes a row and confirms
- **THEN** the relationship is deleted and the row disappears without reloading the contact

#### Scenario: Read-only contact

- **WHEN** a contact is opened read-only
- **THEN** its relationships are listed and navigable
- **AND** no action to add, edit or remove one is offered

### Requirement: Warning Before Deleting A Referenced Contact

Deleting a contact SHALL warn the user beforehand when that contact still carries at least
one relationship, and the warning SHALL say which contacts are affected and how many
relationships each of them has. The warning SHALL be shown before anything is deleted, and
the user SHALL be able to abort.

This SHALL hold in **every** client that can delete a contact, not only in the desktop
client: a safety warning that exists in one client and not in the other is no warning. The
count SHALL come from the server, so both clients ask the same question of the same data.

Relationships SHALL NOT prevent the deletion: confirming SHALL delete the contact and its
relationships. The existing reasons that *do* prevent a deletion — the contact being a party
in a case, an invoice recipient or a payment counterparty — SHALL keep working unchanged.

When several contacts are deleted at once, the warning SHALL cover all of them in one
dialog.

#### Scenario: Warning for a related contact

- **GIVEN** a contact has three relationships
- **WHEN** a user deletes it
- **THEN** a warning names the contact and its three relationships before anything happens
- **AND** aborting leaves the contact and its relationships untouched

#### Scenario: Confirming deletes both

- **WHEN** the user confirms the warning
- **THEN** the contact is deleted and its relationships with it

#### Scenario: Contact without relationships

- **WHEN** a contact without relationships is deleted
- **THEN** no additional warning about relationships is shown

#### Scenario: Several contacts at once

- **WHEN** five contacts are selected for deletion and two of them have relationships
- **THEN** one warning names those two with their counts

#### Scenario: Same warning in the web client

- **GIVEN** a contact with relationships
- **WHEN** it is deleted in the web client
- **THEN** the same warning is shown before anything is deleted, naming the relationships
- **AND** aborting leaves the contact untouched

#### Scenario: Blocking references still block

- **GIVEN** a contact is a party in a case
- **WHEN** it is deleted
- **THEN** the deletion is still refused with the existing reason, regardless of any
  relationships

### Requirement: Relationship Network Visualisation At A Contact

The graph views are the one part of this capability that SHALL exist in the desktop client
only; the web client SHALL NOT be required to draw a network. Everything else — maintenance,
the delete warning, the case-party shortcut and the administration — SHALL exist in both.

The desktop client SHALL be able to show the relationships around a contact as a graph:
contacts as nodes, relationships as edges labelled with the direction-correct label. The
graph SHALL be reachable from the contact's relationship section and SHALL be restricted to
contacts — cases SHALL NOT appear in it.

The graph SHALL start from the selected contact and SHALL show its neighbourhood up to a
limited depth rather than the whole address book, and SHALL stop adding nodes at a defined
upper bound, saying so when that bound is reached. Further nodes SHALL be reachable by
expanding a node explicitly.

A user SHALL be able to rearrange the graph by dragging nodes, SHALL be able to open a
contact from its node, and SHALL see the full details of a node or edge without opening it.
The layout SHALL come to rest instead of running continuously.

#### Scenario: Network around a contact

- **WHEN** the user opens the network view for a contact with four relationships
- **THEN** the contact and its four related contacts are drawn, connected by edges labelled
  with the relationship labels

#### Scenario: Only contacts

- **GIVEN** the contact is a party in three cases
- **WHEN** the contact network is shown
- **THEN** no case appears in the graph

#### Scenario: Expanding a neighbour

- **WHEN** the user expands a related contact's node
- **THEN** that contact's own relationships are loaded and added to the graph

#### Scenario: Large network stays readable

- **GIVEN** a contact whose network exceeds the node limit
- **WHEN** the network view is opened
- **THEN** the graph is limited to the bound and the user is told that it was truncated

#### Scenario: Opening a contact from the graph

- **WHEN** the user opens a node
- **THEN** that contact is opened in the contact editor

### Requirement: Extended Network Visualisation At A Case

The desktop client SHALL be able to show an extended graph from a case that, in addition to
contacts and their relationships, includes cases and each contact's participation in them.
A participation edge SHALL be labelled with the contact's role in that case (Beteiligtentyp)
and SHALL be visually distinguishable from a relationship edge; a case node SHALL be visually
distinguishable from a contact node.

The graph SHALL start from the open case and its parties, and SHALL apply the same depth and
node limits as the contact network. Opening a case node SHALL open that case, opening a
contact node SHALL open that contact.

It SHALL be reachable from the case editor without changing the order of the editor's tabs.

#### Scenario: Case, parties and their relationships

- **WHEN** the extended network is opened from a case with three parties
- **THEN** the case, its three parties and the relationships between those parties are drawn
- **AND** each party is connected to the case by an edge labelled with its role in the case

#### Scenario: Another case of a party becomes visible

- **GIVEN** one party of the case is also a party in two other cases
- **WHEN** the extended network is opened
- **THEN** those cases appear as case nodes connected to that party with its role there

#### Scenario: Navigating from the graph

- **WHEN** the user opens a case node
- **THEN** that case is opened in the case editor

#### Scenario: Tab order untouched

- **WHEN** the extended network is made reachable from the case editor
- **THEN** the editor's existing tabs keep their order and their lazy loading keeps working

### Requirement: Contact Relationships REST API

The REST API SHALL expose, in its current version (v8), the relationship type catalogue as
well as the relationships of a contact: reading the catalogue, **maintaining** the catalogue
(create, change, delete), reading a contact's relationships, creating a relationship between
two contacts with a type and an optional note, updating a relationship's note, and deleting a
relationship. The catalogue SHALL sit where the API keeps its other administrable master
data, so a client administering it finds it next to the party types.

The returned relationship SHALL be described as seen from the requested contact, with the
applicable label and the other contact's identifying fields. The endpoints SHALL use the
API's existing authentication, SHALL report errors through the uniform error envelope, SHALL
enforce the same rules as the service layer, and SHALL be additive — no existing API version
changes behaviour.

#### Scenario: Read a contact's relationships

- **WHEN** a client requests the relationships of a contact
- **THEN** each entry carries the applicable label, the type and the other contact's id and
  display name

#### Scenario: Create a relationship

- **WHEN** a client posts a relationship naming the other contact and a type
- **THEN** it is created and returned as seen from the requested contact

#### Scenario: Rejections are reported

- **WHEN** a client tries to create a relationship that already exists, or one from a contact
  to itself, or one with an unknown type
- **THEN** the request fails with an error that says which of those it is

#### Scenario: Read the catalogue

- **WHEN** a client requests the relationship types
- **THEN** the active types are returned with both labels, the symmetry flag and the category

#### Scenario: Maintain the catalogue

- **WHEN** an administrating client creates, changes or deletes a relationship type
- **THEN** it is applied with the same rules as in the desktop administration, and a type
  still in use is refused

### Requirement: Relationship Maintenance In The Web Client

The web client SHALL show the relationships of the selected contact in its contact module,
one entry per relationship with the direction-correct label, the other contact and the note,
with an empty state when there are none. Selecting an entry SHALL navigate to that contact.

The web client SHALL allow adding a relationship by picking a label and another contact,
editing the note, and removing a relationship after a confirmation, and SHALL surface the
server's rejection messages rather than swallowing them. All texts SHALL come from the
translation bundles for German and English. The relationship labels themselves SHALL NOT be
translated: they are administrable master data with a single wording, so they SHALL be shown
as configured regardless of the client language.

#### Scenario: Relationships in the web contact view

- **WHEN** a contact with relationships is selected in the web client
- **THEN** they are listed with their labels and the other contacts

#### Scenario: Navigate in the web client

- **WHEN** the user selects a related contact
- **THEN** the application navigates to that contact, whose relationships list the one back

#### Scenario: Rejection is visible

- **WHEN** the user tries to add a relationship that already exists
- **THEN** the server's message is shown instead of a generic failure

#### Scenario: Both languages

- **WHEN** the client language is English
- **THEN** the section's title, empty state and actions are shown in English
- **AND** the relationship labels are still shown as configured, not translated

### Requirement: Relationship Type Administration

An administrator SHALL be able to maintain the relationship type catalogue: listing the types
in their configured order, creating a type with both labels, the symmetry flag, category,
colour and sequence, changing it, activating or deactivating it, and deleting one that is not
in use. The administration SHALL be restricted to administrators.

It SHALL be available in the desktop client and in the web client, in each case where that
client keeps its other catalogues of administrable master data, so a firm does not have to
switch clients to configure the feature it is using.

Changing a type's labels SHALL take effect wherever relationships of that type are shown,
without a client restart. Turning an existing type symmetric SHALL warn that already
recorded relationships of that type are not reconciled by the change.

#### Scenario: Administrator creates a type

- **WHEN** an administrator adds "Vermieter – Mieter" with both labels and a category
- **THEN** the type is offered when a relationship is created, in that category

#### Scenario: Non-administrator has no access

- **WHEN** a user without administrator rights opens the administration
- **THEN** the relationship type catalogue is not editable for them

#### Scenario: Administration in the web client

- **WHEN** an administrator opens the settings section for relationship types in the web
  client
- **THEN** the catalogue can be listed, created, changed, (de)activated and deleted there with
  the same rules as in the desktop client

#### Scenario: Renaming a label

- **WHEN** an administrator changes a type's label
- **THEN** existing relationships of that type are shown with the new label

#### Scenario: Warning when a type becomes symmetric

- **GIVEN** a directed type is in use
- **WHEN** an administrator marks it symmetric
- **THEN** the administrator is warned that existing relationships of that type keep their
  stored direction and may now contain mirrored duplicates
