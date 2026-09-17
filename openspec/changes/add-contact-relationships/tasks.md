## 1. Data model

- [x] 1.1 Add Flyway migration
      `j-lawyer-server-entities/src/main/resources/db/migration/V3_6_0_28__ContactRelations.sql`
      (re-check the highest existing version first; `V3_6_0_27` is the latest today):
      - `contact_relation_types` (`id` VARCHAR(50) BINARY PK, `name` VARCHAR(100) BINARY NOT
        NULL, `label_from` VARCHAR(100) BINARY NOT NULL, `label_to` VARCHAR(100) BINARY NOT
        NULL, `symmetric` TINYINT(1) DEFAULT 0 NOT NULL, `category` VARCHAR(50) BINARY,
        `color` INTEGER DEFAULT 0, `sequence_no` INTEGER DEFAULT 0, `active` TINYINT(1)
        DEFAULT 1 NOT NULL), unique index on `name`
      - `contact_relations` (`id`, `from_contact_id`, `to_contact_id`, `type_id`, `note`
        VARCHAR(250) BINARY, `date_created` DATETIME, `created_by` VARCHAR(50) BINARY), FKs
        on `contacts(id)` `ON DELETE CASCADE` for both contacts and on
        `contact_relation_types(id)` `ON DELETE RESTRICT`,
        `CHECK (from_contact_id <> to_contact_id)`, unique index
        `UQ_CONTACTRELATIONS_TRIPLE (from_contact_id, to_contact_id, type_id)`, index on
        `to_contact_id`
      - leading comment block explaining direction-as-data and the symmetric normalisation,
        plus the `server_settings` version bump and `commit;` — as in
        `V3_6_0_27__CaseLinks.sql`
- [x] 1.2 Add entity
      `j-lawyer-server-entities/.../persistence/ContactRelationType.java` (`@Table(name =
      "contact_relation_types")`, named queries incl. `findAllInSequence` and
      `findActiveInSequence`, style of `PartyTypeBean`/`DunningStage`)
- [x] 1.3 Add entity `j-lawyer-server-entities/.../persistence/ContactRelation.java`
      (`@Table(name = "contact_relations")`, `@ManyToOne(fetch = FetchType.LAZY)` for both
      contacts and for the type; named queries `findByContact`, `findByTriple`, `findByType`).
      `AddressBean` gets **no** inverse collection: relationships form a graph, and a mapped
      collection would let one contact load walk outward from contact to contact. Verify after
      the change that `AddressBean` still has exactly its two existing `@OneToMany` collections
      (`AddressBean.java:725,887`) and that `getAddress` is untouched
- [x] 1.4 Add `ContactRelation.normalisePair(...)` for symmetric types (same idea as
      `CaseLink.normalisePair`, rejecting null and equal ids) and a `getOtherContact(id)`
      helper
- [x] 1.5 Register **both** new entities in **both** `persistence.xml` files
      (`j-lawyer-server-entities/src/main/resources/META-INF/` and
      `j-lawyer-server/j-lawyer-server-ejb/src/main/resources/META-INF/`)
- [x] 1.6 Add `ContactRelationDTO` to `j-lawyer-server-api/.../services/` (relationId, label,
      typeId, typeName, symmetric, color, note, dateCreated, createdBy, otherContactId,
      otherContactDisplayName, otherContactCompany, otherContactCity)
- [x] 1.7 Extract the display-name composition of `AddressBean.toDisplayName()`
      (`AddressBean.java:1292-1314`) into a static helper so the projection can compose the
      same string from columns (as `ArchiveFileBean.composeFileNumber` does)
- [x] 1.8 Add facades `ContactRelationTypeFacade(Local)` and `ContactRelationFacade(Local)`
      under `j-lawyer-server/j-lawyer-server-ejb/.../persistence/`, the latter with a
      projection query per side filling `ContactRelationDTO` (scalars + type labels only, no
      `AddressBean`, no `ContactRelationType` instantiated)
- [x] 1.9 Add seed migration `V3_6_0_29__ContactRelationTypesSeed.sql`: ~60 German types with
      fixed `seed-crt-*` ids, `INSERT ... SELECT ... WHERE NOT EXISTS`, grouped by category
      (Familie, Vertretung, Unternehmen, Vertrag, Sonstige), symmetric flag set where the
      relation reads the same both ways, sensible `sequence_no`, plus the version bump.
      Cover at least: Elternteil/Mutter/Vater–Kind, Ehepartner, eingetragener Lebenspartner,
      Lebensgefährte, geschiedener Ehepartner, Geschwister, Halbgeschwister,
      Großelternteil–Enkel, Onkel/Tante–Nichte/Neffe, Cousin/Cousine,
      Stief-/Adoptiv-/Pflegeelternteil–Kind, Schwiegerelternteil–Schwiegerkind,
      Schwager/Schwägerin; gesetzlicher Vertreter, Vormund–Mündel, Betreuer–Betreuter,
      Bevollmächtigter–Vollmachtgeber, Verfahrensbeistand, Testamentsvollstrecker–Erblasser,
      Erbe–Erblasser, Nachlasspfleger, Insolvenzverwalter–Schuldner,
      Rechtsanwalt–Mandant, Steuerberater–Mandant, Notar; Geschäftsführer, Vorstand,
      Gesellschafter, Prokurist, Inhaber, Ansprechpartner, Arbeitgeber–Arbeitnehmer,
      Muttergesellschaft–Tochtergesellschaft, verbundenes Unternehmen,
      Zweigstelle–Hauptniederlassung, Rechtsvorgänger–Rechtsnachfolger;
      Versicherer–Versicherungsnehmer, Versicherungsnehmer–versicherte Person,
      Vermieter–Mieter, Verpächter–Pächter, Darlehensgeber–Darlehensnehmer, Bürge,
      Gläubiger–Schuldner, Vertragspartner; Nachbar, Kontaktperson, Hausarzt–Patient

## 2. Server services

- [x] 2.1 `SystemManagement` (+ Local/Remote, JavaDoc on the remote): `getContactRelationTypes()`,
      `getActiveContactRelationTypes()`, `addContactRelationType`, `updateContactRelationType`,
      `removeContactRelationType` — with the uniqueness/blank checks and the
      "refuse while referenced" rule of `removePartyType` (`SystemManagement.java:2226-2230`),
      keeping `label_to` equal to `label_from` for symmetric types
- [x] 2.2 `AddressService` (+ Local/Remote, JavaDoc on the remote):
      `List<ContactRelationDTO> getRelations(String contactId)` — read role, label resolved
      for the requested side
- [x] 2.3 `ContactRelationDTO addRelation(String fromContactId, String toContactId, String typeId, String note)`
      — write role, reject self-relation, reject unknown type, normalise the pair for
      symmetric types, reject the duplicate triple with a distinguishable exception
      (`ContactRelationExistsException` in `j-lawyer-server-api`)
- [x] 2.4 `void updateRelationNote(String relationId, String note)` and
      `void removeRelation(String relationId)`
- [x] 2.5 `Map<String, Integer> getRelationCounts(List<String> contactIds)` for the delete
      warning — one query, not one per contact
- [x] 2.6 Leave `removeAddress`'s existing blocking checks untouched; verify by reading that
      relationships are removed by the FK cascade and that nothing in
      `AddressService.removeAddress` needs to change

## 3. REST API v8

- [x] 3.1 New pojos under `j-lawyer-server/j-lawyer-io/.../rest/v8/pojo/`:
      `RestfulContactRelationV8`, `RestfulContactRelationRequestV8`,
      `RestfulContactRelationTypeV8` with `fromDTO`/`fromBean` factories and swagger
      annotations in the style of the other v8 pojos
- [x] 3.2 `GET /v8/contacts/{id}/relations` in `ContactsEndpointV8` (+ local twin)
- [x] 3.3 `PUT /v8/contacts/{id}/relations` (body: other contact id, type id, note) returning
      the created relationship; already-exists / self-relation / unknown type reported through
      the uniform error envelope with distinguishable messages
- [x] 3.4 `PUT /v8/contacts/{id}/relations/{relationId}` (note) and
      `DELETE /v8/contacts/{id}/relations/{relationId}`, both verifying the relationship
      belongs to `{id}`
- [x] 3.5 Catalogue endpoints under the API's master-data path, following
      `/v7/configuration/party-types` (the web settings services' pattern): `GET` (all incl.
      inactive for administration, plus an active-only read for the pickers), `PUT` (update),
      `POST` (create), `DELETE` on `/v8/configuration/contact-relation-types`, with the
      service's uniqueness and "refuse while referenced" rules surfaced as distinguishable
      errors
- [x] 3.6 Annotate all endpoints so the generated swagger picks them up (no manual swagger
      edit)

## 4. Desktop: relationships at the contact

- [x] 4.1 Add `ContactRelationEntryPanel.java` + `.form` under
      `j-lawyer-client/.../editors/addresses/`, modelled on `CaseForContactEntryPanel`:
      label, other contact, note, edit/remove actions; click opens the other contact via
      `getAddress(id)` + `EditorsRegistry` (idiom at `ArchiveFilePanel.java:10097-10135`)
- [x] 4.2 Add a "Beziehungen" tab as the **last** tab of `jTabbedPane1` in `AddressPanel.form`
      + `.java` (scroll pane over `pnlRelationsForContact`, `GridLayout`, like
      `pnlCasesForContact` at `AddressPanel.java:3264-3275`), keeping both files consistent
      for the GUI builder
- [x] 4.3 Load it lazily in `jTabbedPane1StateChanged` (`AddressPanel.java:3542`) for the new
      index, clear it in `setAddressDTO` and in the clear/reset path (as
      `pnlCasesForContact` is cleared at `:1040` and `:1410`), empty-state text
- [x] 4.4 Add `ContactRelationDialog.java` + `.form`: label picker grouped by category
      (picking a label fixes type **and** direction), contact picker for the other contact,
      note; used for adding and for editing the note
- [x] 4.5 Wire add / edit note / remove (with confirmation) against the new service methods;
      show `ContactRelationExistsException` as a notice, not as an error
- [x] 4.6 Disable the editing actions where the contact is shown read-only
      (`ViewAddressDetailsPanel`), keeping the list navigable

## 5. Desktop: add a related contact as a case party

- [x] 5.1 Extract the party append path of
      `ArchiveFilePanel.cmdSearchClientActionPerformed` (`:6069-6110`: `addAddressToCase`,
      build `InvolvedPartyEntryPanel`, add to `pnlInvolvedParties`, revalidate) into a public
      method `addPartyToCase(AddressBean contact, PartyTypeBean type)` and rewire the existing
      action to it, so both entry points share one implementation
- [x] 5.2 Add a submenu `mnuAddRelatedParty` ("verknüpften Kontakt als Beteiligten
      hinzufügen") to `InvolvedPartyEntryPanel`'s `partiesPopup` (`:918-938`), filled **when
      the popup opens** (`cmdActionsMousePressed`, `:1475`) — not in the constructor and not
      during the case load — from `getRelations(<party contact id>)`
- [x] 5.3 One item per related contact, labelled "<Richtungsbezeichnung> – <Anzeigename>";
      contacts that are already parties of the case are marked as such and disabled; the whole
      submenu is disabled with a hint when the party has no relationships
- [x] 5.4 Choosing an item asks for the Beteiligtentyp and then calls `addPartyToCase(...)`;
      errors are reported the way the existing add-party action reports them
- [x] 5.5 Cache the party's relationships per `InvolvedPartyEntryPanel` instance for as long
      as the case stays open, so reopening the popup does not re-read

## 6. Desktop: delete warning

- [x] 6.1 In `QuickAddressSearchPanel.mnuDeleteSelectedAddressesActionPerformed`
      (`:994-1030`), call `getRelationCounts(ids)` before the existing confirmation
- [x] 6.2 When at least one selected contact has relationships, show one dialog naming those
      contacts and their counts, with abort as the default answer; on confirmation continue
      into the existing `RemoveAddressesAction` path unchanged
- [x] 6.3 Verify by hand that a contact that is a case party is still refused by the server
      with its existing message

## 7. Desktop: graph component

- [x] 7.1 Add `j-lawyer-client/.../ui/graph/GraphNode.java` / `GraphEdge.java` (plain model:
      id, kind CONTACT|CASE, label, tooltip, colour, pinned position)
- [x] 7.2 Add `RelationshipGraphPanel.java` (no `.form`; hand-written `JPanel` with
      `paintComponent`, following `viewer/WaveformPanel.java:128`): spring-embedder layout in
      a Swing `Timer` that stops below a movement threshold and after a hard iteration
      ceiling, antialiased edges with labels, rounded-rect contact nodes and a distinct case
      node shape, zoom on wheel, pan on background drag, node drag pins a node, tooltips
- [x] 7.3 Add `RelationshipGraphDialog.java` + `.form` hosting the panel with a toolbar
      (re-layout, zoom to fit, depth selector) and a progress indicator while data loads
- [x] 7.4 Data loading for the contact mode: relationships of the start contact, expansion of
      a node on click, depth limit (default 2) and node cap (150) with a visible note when the
      cap truncates the graph
- [x] 7.5 Open a contact on double-click / context menu from a node, reusing the editor idiom
- [x] 7.6 Add the entry point on the contact editor's "Beziehungen" tab (button "Netz
      anzeigen")

## 8. Desktop: extended graph at the case

- [x] 8.1 Extend `RelationshipGraphPanel` data loading with the case mode: the open case, its
      parties (`getAddressesForCase`), the relationships between those parties, and each
      party's other cases (`getArchiveFileAddressesForAddress`,
      `ArchiveFileServiceRemote.java:727`) with the role as the edge label and
      `PartyTypeBean.getColor()` as its colour
- [x] 8.2 Add a toolbar button `cmdCaseNetwork` to `ArchiveFilePanel` + `.form` (next to
      `cmdIngoChat`) opening the dialog in case mode — **do not** add a tab: the lazy loading
      in `tabPaneArchiveFileStateChanged` (`:6269-6308`) dispatches on hardcoded indices
- [x] 8.3 Open a case on double-click of a case node (the `openCaseById` idiom already in
      `ArchiveFilePanel`), a contact on double-click of a contact node
- [x] 8.4 Apply the same depth and node limits as the contact mode

## 9. Desktop: type administration

- [x] 9.1 Add `ContactRelationTypesDialog.java` + `.form` under
      `j-lawyer-client/.../configuration/`, modelled on `PartyTypesDialog` (table + detail
      fields, colour picker, sequence up/down) plus the symmetry flag, category and active
      flag; warn when symmetry is switched on for a type that is in use
- [x] 9.2 Add the menu entry in `JKanzleiGUI.java` + `.form` next to `mnuPartyTypes`
      ("Beteiligtentypen"), guarded by `checkAdmin()`
- [x] 9.3 Fetch the catalogue on demand (like party types), not via `ClientSettings`, so an
      administrator's change needs no re-login

## 10. Web client

- [x] 10.1 Add `ContactRelation` and `ContactRelationType` to the contacts module's models
- [x] 10.2 Add service methods against `/v8/contacts/{id}/relations` and
      `/v8/configuration/contact-relation-types`, following the error conventions of the
      neighbouring methods (pass write errors through)
- [x] 10.3 Add the relationships section to the web contact view: list with label, other
      contact and note, empty state, navigation to the other contact via the existing
      contact deep link, add / edit note / remove with confirmation, server messages surfaced
- [x] 10.4 Delete warning parity: `contacts/kontakte.component.ts:439` (`confirmDelete()`)
      reads the relationship count first and, when there is one, asks the same question the
      desktop asks before deleting — a warning that exists in one client only is not a
      warning
- [x] 10.5 Case-party shortcut parity: in `akten/party-add.component.ts` offer the related
      contacts of the case's existing parties (label + name) as a shortcut that pre-fills the
      contact, so only the role is left to pick; candidates are loaded when the dialog opens
- [x] 10.6 Administration parity: new settings section for the type catalogue, modelled on
      `settings/party-types.component.ts` + `settings/party-type.service.ts`, registered in
      `settings/section-registry.ts` (next to the `partyTypes` entry at `:94`) and rendered in
      `settings/settings-screen.component.ts` (next to `@case ('partyTypes')` at `:106`),
      admin-only like the other sections
- [x] 10.7 Add the i18n keys under `kontakte.relations.*` and
      `settings.section.contactRelationTypes` to both
      `j-lawyer-web/frontend/public/i18n/de.json` and `en.json` — note that the relationship
      **labels** are data and stay as configured in both languages
- [x] 10.8 No graph in the web client: that stays desktop-only in this change

## 11. Verification

- [x] 11.1 Server unit test for `ContactRelation.normalisePair` and the display-name helper
      (`*Test.java` in the ejb module's `src/test/java`)
- [ ] 11.2 Verify with SQL logging that reading a contact's relationships is a constant
      number of queries and loads no `contacts` or `contact_relation_types` entities
- [ ] 11.3 Verify with SQL logging that opening a contact and opening a case with ten parties
      read **no** relationships at all, and that the party submenu reads them only when it is
      opened
- [ ] 11.4 Manual REST check against Docker (admin:a): catalogue, read/create/update/delete,
      plus the duplicate, self-relation and unknown-type rejections; confirm the generated
      `swagger.json` contains the new operations
- [ ] 11.5 Manual desktop check: add from either side and see both labels, symmetric type
      entered twice is refused, navigate A→B→A, edit note, remove, read-only contact,
      delete warning (single and multi-select), blocked delete still blocked, type
      administration incl. deletion refusal
- [ ] 11.6 Manual graph check: contact network (expansion, drag, zoom, truncation note,
      layout comes to rest), extended case network (case nodes, role labels, other cases of a
      party, navigation), and a hub contact with many relationships
- [ ] 11.7 Manual web check: list, navigation, add, remove, rejection message, both
      languages, delete warning (and that a blocked delete is still blocked), the party-add
      shortcut, and the type administration section incl. the deletion refusal
- [x] 11.8 `openspec validate add-contact-relationships --strict`

## Notes

Implemented; not built and not committed (the project builds manually). Open items are the ones
that need a build and a running server: 11.2 - 11.8.

Deviations and additions made while implementing:
- `ContactRelationExistsException` (new, `j-lawyer-server-api`) so "already recorded" is
  distinguishable; the desktop shows it as a notice, REST returns it in the v8 error envelope with
  `error: "ContactRelationExistsException"` (status 500 - the envelope helper only emits 500).
- The catalogue endpoints live in a **new** `ConfigurationEndpointV8` (`/v8/configuration`), which
  did not exist yet, and had to be registered in `EndpointServiceLocator` because the JAX-RS
  `Application` enumerates its classes explicitly. Active-only read is
  `?activeOnly=true`. **POST creates, PUT updates** - the conventional mapping, deliberately the
  opposite of `/v7/configuration/party-types`, which has them swapped; the web client follows the
  v8 shape.
- `AddressBean.composeDisplayName(...)` extracted from `toDisplayName()` for the projection.
- `ContactRelation.normalisePair(...)` holds the symmetric-pair normalisation and is unit-tested
  (`ContactRelationPairTest`).
- `ArchiveFilePanel.addPartyToCase(AddressBean, ArchiveFileAddressesBean)` extracted from
  `cmdSearchClientActionPerformed`, plus `getInvolvedContactIds()` so the new submenu can mark
  contacts that are already parties.
- `InvolvedPartyEntryPanel.getAddress()` added (there was no accessor for the row's contact).
- The graph's case mode uses `getInvolvementDetailsForCase(caseId, false)` for the parties rather
  than `getAddressesForCase`, because it needs the party role, not just the contact.
- The web relationship count is derived from the relations read: `getRelationCounts` is an EJB
  method with no REST endpoint, and adding one was not needed for a per-contact delete.
- The contact picker in `ContactRelationDialog` is built in-dialog: `AddAddressSearchDialog` is
  coupled to cases (needs a case id, loads party types, runs conflict checks) and
  `MultiAddressSearchDialog` is multi-select, so neither was reusable for picking one contact.
- Desktop administration sits under "Einstellungen -> Adressen" next to "Beteiligtentypen"; the web
  settings section is registered in the same group.
