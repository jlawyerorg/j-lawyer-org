# Change: Relationships between contacts (Kontaktbeziehungen)

## Why

A law firm's address book is full of people whose relationships to each other are the very
substance of the matter: mother and child in family law, policy holder and insured person,
managing director and company, guardian and ward, landlord and tenant, legal representative
and represented party, heir and testator. Today j-lawyer stores each contact in isolation.
The relationship lives in a colleague's head, in a note, or is reconstructed from the parties
of a case every time somebody needs it — and it is invisible from the *other* contact.

Two consequences hurt daily work:

- **Nothing warns before damage.** Deleting a contact today is refused only when it is a
  party in a case, an invoice recipient or a payment counterparty
  (`AddressService.java:808-851`). Anything else disappears without a word, and the batch
  delete in the contact search even loses the per-contact reason
  (`QuickAddressSearchPanel.java:1025` reports a single lumped sentence).
- **The picture is never visible as a whole.** Who belongs to whom around this client, and
  which of our cases do those people appear in — that is a question a partner asks in
  seconds and that today takes minutes of clicking through cases.

## What Changes

- **New capability `contact-relationships`**: directed, typed relationships between two
  contacts, plus an administrable catalogue of relationship types.
- **Configurable relationship types** in their own table (entity `ContactRelationType`,
  modelled on `PartyTypeBean`): a label per direction (`ist Mutter von` /
  `ist Kind von`), a `symmetric` flag for relationships that read the same both ways
  (`ist Ehepartner von`), a category for grouping in the pickers (Familie, Vertretung,
  Unternehmen, Vertrag, Sonstige), a colour, a sequence number and an active flag.
  Administered in a new `ContactRelationTypesDialog`, reachable from the administration menu
  next to "Beteiligtentypen" (`JKanzleiGUI.mnuPartyTypes`).
- **An extensive seeded catalogue** (~60 types) via Flyway, with fixed ids and
  `WHERE NOT EXISTS`, so a firm's own edits survive every upgrade — the pattern of
  `V3_6_0_14__DunningStagesSeed.sql`.
- **Relationships stored once per direction** (entity `ContactRelation`, table
  `contact_relations`): `from_contact`, `to_contact`, the type, an optional note. Read from
  either side; the side a contact sits on decides which of the type's two labels is shown.
  Both foreign keys `ON DELETE CASCADE`.
- **Never loaded with a contact**: `AddressBean` gets no relationship collection and
  `getAddress` stays as it is. Relationships form a graph, so a mapped collection would pull
  the first ring into every contact load — and since each related contact carries the same
  mapping, one stray access would walk outward from there. They are read only by their own
  call, one contact at a time, the graph one ring at a time.
- **Adding a related contact as a case party**: a party's actions in the case's party list
  offer its related contacts ("ist Geschäftsführer von – Max Müller"); choosing one asks for
  the Beteiligtentyp and adds that contact to the case through the existing add-party path.
  The candidates are read when the menu opens, never during the case load.
- **New "Beziehungen" tab in the contact editor** (`AddressPanel`), built like the existing
  "Akten" tab: a lazily loaded list of rows showing label, the other contact and the note,
  clicking a row opens that contact, plus add / edit / remove. A relationship entered on
  either contact is immediately visible on the other.
- **A hint before deleting a referenced contact**: the contact delete in
  `QuickAddressSearchPanel` asks up front when a contact still carries relationships, naming
  them. Relationships **do not block** the deletion (unlike case parties or invoices) — they
  are removed with the contact; the user only must not be surprised.
- **Graphical network at a contact**: a custom-painted Swing panel (no new dependency)
  showing the selected contact and its relationships as a node-link graph, expandable by
  clicking a node, restricted to contacts.
- **Extended graph at a case**: the same component in a mode that also draws the cases the
  contacts appear in and each contact's role in them (`ArchiveFileAddressesBean.referenceType`),
  so a case's parties, their private relationships and the other cases they turn up in are
  one picture. Opened from a toolbar button of the case editor, so no tab index shifts.
- **REST API v8** for the relationships and for the type catalogue, the latter readable
  *and* maintainable and placed where the API keeps its other master data
  (`/v8/configuration/contact-relation-types`, following `/v7/configuration/party-types`).
- **Web client at parity, except the graph.** The web client already deletes contacts
  (`contacts/kontakte.component.ts:439`), adds case parties
  (`akten/party-add.component.ts`) and maintains catalogues
  (`settings/party-types.component.ts`), so it gets all of it: relationship maintenance, the
  **delete warning**, the **case-party shortcut** and the **type administration**. Only the
  graph stays desktop-only — the Swing component is hand-painted, so nothing of it is
  reusable in a browser, and a web graph would be its own implementation. A later change can
  add it against the same REST reads.

Out of scope: validity periods / relationship history (a relationship exists or it does
not), relationships between a contact and a case (that is what case parties already are),
inferring relationships automatically from case parties, the graph in the web client,
relationship-aware conflict-of-interest checking, English labels for the catalogue, any
permanent display of relationships in the case's party list beyond the add-party submenu, and
the network views in the web client.

## Impact

- Affected specs: `contact-relationships` (new capability). Desktop, REST and web
  requirements are kept inside it, as `add-case-linking` did, so this does not collide with
  the pending `add-web-client` change.
- Overlap to coordinate: the pending `add-contact-insurant-fields` change also edits
  `AddressPanel.java`/`.form`, but in the "Bank / Versicherung" tab's fields (its
  implementation is done, only manual verification is open). This change only appends a new
  last tab, so the two touch different regions of both files — whichever lands second just
  has to keep the `.form` consistent.
- Affected code:
  - `j-lawyer-server-entities/.../persistence/ContactRelationType.java`, `ContactRelation.java` (new)
  - `j-lawyer-server-entities/src/main/resources/db/migration/V3_6_0_28__ContactRelations.sql`,
    `V3_6_0_29__ContactRelationTypesSeed.sql` (new; re-check the highest version at
    implementation time)
  - `j-lawyer-server/j-lawyer-server-ejb/.../persistence/ContactRelationFacade(Local).java`,
    `ContactRelationTypeFacade(Local).java` (new)
  - `j-lawyer-server/j-lawyer-server-ejb/.../services/AddressService.java` (relationships,
    delete hint data) and `SystemManagement.java` (type catalogue), plus both interfaces
  - `j-lawyer-server-api/.../services/AddressServiceRemote.java`,
    `SystemManagementRemote.java` (JavaDoc required), new `ContactRelationDTO`
  - `j-lawyer-server/j-lawyer-io/.../rest/v8/ContactsEndpointV8.java` (+ local twin, new
    pojos)
  - `j-lawyer-client/.../editors/addresses/AddressPanel.java` + `.form` (new tab),
    new `ContactRelationEntryPanel.java` + `.form`, `ContactRelationDialog.java` + `.form`
  - `j-lawyer-client/.../editors/addresses/QuickAddressSearchPanel.java` (delete hint)
  - `j-lawyer-client/.../ui/graph/RelationshipGraphPanel.java`, `RelationshipGraphDialog.java` (new)
  - `j-lawyer-client/.../editors/files/ArchiveFilePanel.java` + `.form` (toolbar button,
    extracted `addPartyToCase(...)`),
    `j-lawyer-client/.../editors/files/InvolvedPartyEntryPanel.java` + `.form` (submenu for
    adding a related contact as a party)
  - `j-lawyer-client/.../configuration/ContactRelationTypesDialog.java` + `.form`,
    `JKanzleiGUI.java` + `.form` (menu entry)
  - `j-lawyer-web/frontend/src/app/contacts/*` (relationships section, delete warning),
    `app/akten/party-add.component.ts` (related-contact shortcut),
    `app/settings/` (new catalogue section + service, registered in `section-registry.ts` and
    `settings-screen.component.ts`), `public/i18n/{de,en}.json`
- No swagger step: `swagger.json` is generated from the annotations on every build.
