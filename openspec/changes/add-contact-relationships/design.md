## Context

Contacts (`AddressBean`, table `contacts`, 77 columns) have tags and documents as children,
and are connected to cases through `ArchiveFileAddressesBean` (table `case_contacts`), which
carries the contact, the case and the role in that case (`referenceType` →
`PartyTypeBean`, `ArchiveFileAddressesBean.java:690-698`). Between two contacts there is
nothing at all: a search over entities, migrations and the client for `contact_relation`,
`ContactRelation`, `relatedContact`, `Beziehung` returns zero hits.

Two existing mechanisms decide how a *configurable* catalogue is built:

- `AppOptionGroupBean` (table `server_options`) holds exactly one string per row — `id`,
  `optionGroup`, `value` (`AppOptionGroupBean.java:674-691`), with no update operation
  (rename = remove + create, `OptionGroupConfigurationDialog.java:919-966`). Where a second
  dimension was needed, the project encoded it into the group *name*
  (`OptionConstants.OPTIONGROUP_*_MV_PREFIX`).
- `PartyTypeBean` (table `party_types`) is a richer administrable entity with its own table,
  name/placeholder/colour/sequence, CRUD on `SystemManagementRemote.java:816-824` and an
  admin dialog (`PartyTypesDialog`, opened from `JKanzleiGUI.java:2797-2803`). Its
  `removePartyType` refuses deletion while the type is still referenced
  (`SystemManagement.java:2226-2230`).

The immediate predecessor of this change is `case-linking`: symmetric links between cases,
stored once, read as a flat DTO, maintained in the case header. This feature is the *typed
and directed* counterpart of it for contacts.

## Goals / Non-Goals

- Goals
  - Directed relationships whose two directions read correctly without the user entering
    anything twice.
  - A catalogue a firm can extend and rename, delivered with enough content to be useful on
    day one.
  - Both sides of a relationship see it, with the right wording, immediately.
  - Nobody deletes a contact without being told what is attached to it.
  - A picture of the network — around a contact, and around a case including its cases.
- Non-Goals
  - Validity periods or relationship history; a relationship exists or it does not.
  - Deriving relationships from case parties automatically.
  - Reasoning over relationships (conflict-of-interest checks, inheritance chains).
  - The graph in the web client.

## Decisions

### Decision: the type catalogue gets its own entity, not an option group

A relationship type needs at least two labels (one per direction) plus a symmetry flag,
a category and an ordering. `AppOptionGroupBean` can hold one string per row, and the only
established way around that is encoding structure into the group name — which would turn
"ist Mutter von / ist Kind von" into parsing. So `ContactRelationType` gets its own table,
modelled on `PartyTypeBean` with the additions `DunningStage` established (an `active` flag
and seeded defaults):

`contact_relation_types(id, name, label_from, label_to, symmetric, category, color,
sequence_no, active)`

- `name` is the catalogue name shown in administration and used in error messages
  ("Mutter – Kind"); unique.
- `label_from` is what the contact on the `from` side *is* to the other one ("ist Mutter
  von"), `label_to` the reverse ("ist Kind von"). This is the whole point of the entity: the
  user picks a label, and that choice fixes both the type and the direction.
- `symmetric` marks types that read the same in both directions ("ist Ehepartner von"). For
  those, `label_to` is kept equal to `label_from` by the service, so the UI needs no special
  case when rendering.
- `category` groups the pickers (Familie, Vertretung, Unternehmen, Vertrag, Sonstige) — with
  ~60 seeded types a flat dropdown would be unusable.
- `color` (int, like `PartyTypeBean.color`) is what the graph draws edges in.
- `active` hides a type from the pickers without deleting it, so existing relationships keep
  their wording. Deletion is refused while the type is referenced, mirroring
  `removePartyType`.

Administration lives in `SystemManagement`, next to the party types, and in a new
`ContactRelationTypesDialog` following `PartyTypesDialog` (table + detail fields + colour
picker + sequence up/down) with a menu entry next to "Beteiligtentypen".

### Decision: one row per relationship, direction is data

`contact_relations(id, from_contact_id, to_contact_id, type_id, note, date_created,
created_by)`. Unlike `case_links`, the order of the two ends is **not** normalised away: it
carries the meaning. `(A, B, Mutter–Kind)` says A is B's mother; `(B, A, Mutter–Kind)` says
the opposite, and both are storable — the model does not pretend to detect a factual
contradiction.

Three constraints:
- `from_contact_id <> to_contact_id`, checked in the service and as a `CHECK` (enforced by
  MariaDB ≥ 10.2.1 / MySQL ≥ 8.0.16, parsed and ignored by older versions — as in
  `V3_6_0_27__CaseLinks.sql`).
- unique index on `(from_contact_id, to_contact_id, type_id)`, so the same statement cannot
  be recorded twice.
- **for symmetric types only**, the service normalises the two ids into the order the
  lexicographically smaller id first, before insert and before the duplicate lookup. Without
  that, "A ist Ehepartner von B" and "B ist Ehepartner von A" would be two rows for one
  fact, and the unique index could not see it. This reuses `CaseLink.normalisePair(...)`'s
  idea; the shared helper goes onto `ContactRelation` and is unit-tested there.

Both foreign keys are `ON DELETE CASCADE`, the type is `ON DELETE RESTRICT` (deletion is
refused in the service anyway; the constraint keeps a direct SQL edit honest).

### Decision: relationships are read as a flat DTO, oriented from the requested contact

`getRelations(contactId)` returns `List<ContactRelationDTO>` with `relationId`, `label` (the
label that applies *as seen from the requested contact*), `typeId`, `typeName`, `symmetric`,
`color`, `note`, and the other contact's `id`, display name, company and city. The caller
never has to work out which end it is looking at, nor which of the two labels applies —
exactly the contract `CaseLinkDTO` established.

`AddressBean` is far less dangerous to serialise than `ArchiveFileBean` (no eager
`@OneToOne`, only two lazy `@OneToMany` collections, `AddressBean.java:725,887`), so this is
less about payload than about the orientation: the label must be resolved server-side, where
the type is known. The projection query selects scalars only and joins the type, so neither
`AddressBean` nor `ContactRelationType` is instantiated.

The display name is composed by `AddressBean.toDisplayName()` (`AddressBean.java:1292-1314`).
Since the projection has no bean to call it on, that logic is extracted into a static helper
on `AddressBean` and used by both, the same way `composeFileNumber` was extracted for
`CaseLinkDTO`.

### Decision: relationships are never part of loading a contact

`AddressBean` gets **no** collection of relationships, and `getAddress(id)` returns exactly
what it returns today. Relationships are only ever read by their own call, for one contact at
a time:

- the contact editor's "Beziehungen" tab loads them when it is first selected, the way the
  "Akten" tab loads case involvements (`AddressPanel.java:3542-3564`) - not on every contact
  load,
- the graph loads one ring at a time and only on explicit expansion,
- the delete warning reads counts, not relationships.

The reason is the shape of the data, not the size of a row: relationships form a graph.
A mapped collection on `AddressBean` would put the first ring into every contact load, and
because each related contact is itself an `AddressBean` with the same mapping, a lazy
collection touched anywhere - or an eager one - walks outward. One badly placed access would
pull a family, its company, that company's directors and their other companies through a
single `getAddress`. Keeping the relationship out of the entity that gets loaded everywhere
is what makes the blast radius impossible rather than merely unlikely.

The same rule applies on the write side: `addRelation` takes two ids, not two beans.

### Decision: relationships warn before a delete, they do not block it

The server already refuses to delete a contact that is a case party, an invoice recipient or
a payment counterparty (`AddressService.java:808-851`, with `describeReferences(...)` listing
up to 20). Relationships are deliberately **not** added to that list: they are descriptive
metadata, and a firm that has recorded "ist Nachbar von" must still be able to clean up its
address book. They are removed with the contact by the FK cascade.

What is added is the warning the request asks for, and it has to happen in the client
*before* the call, because the existing batch path (`removeAddresses`) swallows the reason
per contact and reports one lumped sentence (`QuickAddressSearchPanel.java:1025`). So a new
read `getRelationCounts(List<String> contactIds)` returns a map contact → count, and the
delete confirmation names the affected contacts and their counts before anything is removed.
One extra round-trip on a destructive action is a good trade.

### Decision: a related contact can be added to the case as a party in one step

The relationships pay off most where the work happens: a GmbH is a party of the case and the
managing director has to be added too. So `InvolvedPartyEntryPanel`'s existing actions popup
(`partiesPopup`, opened from `cmdActions`, `InvolvedPartyEntryPanel.java:918-938`, `:1475`)
gets a submenu listing the party's related contacts with their direction-correct label
("ist Geschäftsführer von - Max Müller"). Choosing one asks for the party type
(Beteiligtentyp) and adds that contact to the case.

Two consequences for the implementation:
- The append path in `cmdSearchClientActionPerformed`
  (`ArchiveFilePanel.java:6069-6110`: `addAddressToCase`, then build an
  `InvolvedPartyEntryPanel`, add it to `pnlInvolvedParties`, revalidate) is extracted into a
  public method on `ArchiveFilePanel` so both entry points share it. Duplicating it would be
  the third copy of that ritual in the class.
- The submenu is built when the popup opens, not when the panel is created: the party's
  relationships are one server call, and paying for it per party on every case load would
  undo the decision above. Contacts that are already parties of the case are shown as such
  and cannot be added twice.

### Decision: both clients get everything except the graph

The feature is split by what a client can reasonably do, not by which client was easier to
extend. The web client already has every building block this needs, so leaving it out would
create silent divergence rather than saving work:

- it deletes contacts today (`contacts/kontakte.component.ts:439` `confirmDelete()`, a plain
  `confirm()`), so without the warning a web user would delete a related contact silently
  while a desktop user is warned — a safety promise that holds in one client only is not a
  promise,
- it adds parties to a case (`akten/party-add.component.ts`, party types already loaded),
  so the "add a related contact as a party" shortcut fits straight into an existing dialog,
- it maintains administrable catalogues, including the closest possible precedent
  (`settings/party-types.component.ts` + `party-type.service.ts` + the `partyTypes` entry in
  `settings/section-registry.ts:94`), so the type catalogue is a new section of the same
  shape.

**The graph stays desktop-only**, and that is a decision about cost, not about importance:
the Swing component is hand-painted (see below), so nothing of it can be reused in a browser
— the web would need its own implementation, and the one thing this repo has no library for
is drawing graphs. A later change can add it to the web against the same REST reads; the data
side is deliberately client-neutral.

Consequence for the REST layer: the type catalogue is not only readable but maintainable over
the API, and it lives where the web client's settings services already look for master data
(`/v7/configuration/party-types` is the pattern, so the new one is
`/v8/configuration/contact-relation-types`) rather than hanging off `/v8/contacts`.

### Decision: the graph is a custom-painted Swing component

`RelationshipGraphPanel extends JPanel` with a hand-rolled spring embedder, painted in
`paintComponent`, following `WaveformPanel` (`viewer/WaveformPanel.java:128`) — the only
existing component in the repo that does real `Graphics2D` work with mouse interaction.

Why not the WebView route, which the project uses for the HTML editor:
- No graph library exists anywhere — not in `j-lawyer-client/pom.xml`, not in the root pom,
  not in `lib/`, not in the in-project `maven-repo/`. The only chart library is
  `org.knowm.xchart:xchart:3.8.2` (root `pom.xml:822-824`), which has no node-link chart
  type. So the WebView path means vendoring a new ~1 MB asset.
- The WebView integration loads everything as base64 `data:` URLs into `loadContent` for
  offline operation (`WebViewHtmlEditorPanel.java:505-545`) — a graph library would have to
  follow that route, and the panel additionally consumes `DRAG_DETECTED` globally
  (`:487-490`) as a crash workaround, which is precisely the gesture a draggable graph wants.
- A node-link view of a few dozen nodes is a modest amount of geometry; Swing draws it
  without a dependency, in the same look as the rest of the client.

Component contract:
- Nodes: contacts (rounded rect with the display name, like `TagToggleRectButton`'s visual
  language) and, in the extended mode, cases (a different shape/colour).
- Edges: relationships (labelled with the direction-correct label, coloured by type) and, in
  the extended mode, case participations (labelled with the party role, coloured by
  `PartyTypeBean.color`).
- Interaction: drag a node to fix it, click a node to expand its neighbours (loaded on
  demand), double-click to open that contact or case in the editor, wheel to zoom, tooltip
  with the full details.
- Bounds: the layout starts from the selected node, expands two levels by default, and stops
  adding nodes at a configurable cap (150). A hub contact would otherwise turn the picture
  into a hairball and the layout into a CPU sink. Expansion beyond that is a click, not a
  default.
- The layout runs in a Swing `Timer` and stops once movement falls below a threshold, so an
  open dialog does not burn a core.

### Decision: the graph opens as a dialog, not as a tab

`RelationshipGraphDialog` hosts the panel, in two modes:
- from the contact editor's new "Beziehungen" tab: contacts only
- from the case editor: contacts **and** cases

The case editor deliberately gets a toolbar button (next to `cmdIngoChat`) rather than a
tenth tab: `tabPaneArchiveFileStateChanged` dispatches on hardcoded tab indices
(`ArchiveFilePanel.java:6269-6308`, plus literals at `:2468`, `:2551-2561`, `:6172`), so a
tab inserted anywhere but at the end would silently break the lazy loading of other tabs. A
dialog also gives the graph the space it needs.

### Decision: the case graph is assembled from existing reads

The extended graph needs, per case: its parties (`getAddressesForCase(caseId)`), per party
its other cases (`getArchiveFileAddressesForAddress(contactId)`,
`ArchiveFileServiceRemote.java:727` — already used by the contact editor's "Akten" tab) and
its relationships (the new read). No new aggregate endpoint in this change.

Trade-off: that is 1 + 2n calls for n parties. Acceptable because the dialog is explicitly
opened, shows a progress indicator, and expansion beyond the first ring is on demand — and
the alternative, a server-side graph endpoint, would fix a shape we have not validated yet.
Noted as the first thing to revisit if the dialog feels slow.

### Decision: the contact editor gets a new tab

"Beziehungen" is added after "Akten" in `AddressPanel`'s `jTabbedPane1` — the structural
twin of what is being built (a lazily loaded list of clickable related entries, populated in
`jTabbedPane1StateChanged` on index 8 today, `AddressPanel.java:3542-3564`). Rows are
`ContactRelationEntryPanel`, modelled on `CaseForContactEntryPanel`, in a `GridLayout`
container inside a scroll pane like `pnlCasesForContact`
(`AddressPanel.java:4599-4647`). Appending at the end keeps the index dispatch trivial.

### Decision: no client-side caching of the catalogue

Party types are fetched from the server on every use rather than cached in `ClientSettings`
(which is only filled at login by `SplashThread.java:813-917` and never invalidated). The
relationship types follow that: fetched when a picker or the graph opens, so an
administrator's change is effective without a re-login.

### Decision: in the case party list, relationships surface only through the action

**Decided (jens@office-42.de): the submenu is the display.** The add-party submenu already
lists a party's related contacts with their direction-correct label, so a user who wants to
know who belongs to this party opens the actions and reads them there — implicitly, at the
moment the question actually comes up. Beyond that, the case party list shows no
relationships: no extra line per party, no badge, no "Mutter von Max Müller, ebenfalls
Beteiligter".

That keeps two things out of the way. The party list stays as dense as it is today — every
party is one `InvolvedPartyEntryPanel` row, and an extra relationship line per row would grow
the tab by the number of parties. And it avoids inventing a relevance rule: which of a
party's relationships deserve a permanent line is a question nobody can answer before the
graph has shown what people look for. The graph and the contact's "Beziehungen" tab remain
the places that show the full picture deliberately.

### Decision: the catalogue is German only, with no second language

**Decided (jens@office-42.de): no English labels for now.** The catalogue is administrable
master data a firm edits itself, exactly like party types and dunning stages, and those carry
a single name too. A second label column would have to be filled for ~60 seeded types, kept
in step by every administrator who adds or renames one, and picked per user in every picker,
graph edge and REST response — for a client base that works in German.

The consequence to accept knowingly: the web client's relationship section shows German
labels even when its UI language is English. Only the *surrounding* texts (section title,
empty state, actions) come from the translation bundles — the relationship labels are data,
not UI text, and the web requirement is worded that way. If bilingual labels are ever wanted,
the honest way in is a `label_from_en`/`label_to_en` pair plus a fallback to the German
label, not an inference at display time.

## Risks / Trade-offs

- **Hairball graphs** at hub contacts (a big insurer, a housing company) → depth limit, node
  cap, expansion on demand; the cap is stated in the UI when it bites.
- **Layout CPU** in a hand-rolled embedder → timer-driven with a movement threshold and a
  hard iteration ceiling; nodes the user dragged stay pinned, which also stabilises it.
- **N+1 reads for the case graph** → bounded by the first ring plus on-demand expansion; a
  server-side aggregate is the documented next step.
- **A seeded catalogue is opinionated.** ~60 German relationship types will not match every
  firm. Mitigated by the `active` flag, free renaming, and the seed's fixed ids +
  `WHERE NOT EXISTS` so edits survive upgrades.
- **Symmetric types entered before someone flips the flag**: turning a directed type
  symmetric later does not retro-normalise existing rows, so a duplicate pair could survive
  the change. The administration warns when the flag is changed on a type that is in use.
- **Deleting a contact silently removes its relationships** (by design) → that is exactly
  what the new warning is for; the warning is the feature, not a nicety.
- **The party submenu costs a server call when it opens.** Deliberate: the alternative is
  loading every party's relationships on every case load. If the popup ever feels slow, the
  call is cached per party for as long as the case stays open - not moved into the load.

## Migration Plan

Two additive Flyway migrations, DDL and seed kept separate as the project does for
`dunning_stages`:
- `V3_6_0_28__ContactRelations.sql` — `contact_relation_types` and `contact_relations`,
  indexes, FKs, `CHECK`, version bump.
- `V3_6_0_29__ContactRelationTypesSeed.sql` — the default catalogue, fixed `seed-crt-*` ids,
  `INSERT ... SELECT ... WHERE NOT EXISTS`, version bump.

Both entities must be registered in **both** `persistence.xml` files
(`j-lawyer-server-entities` and `j-lawyer-server/j-lawyer-server-ejb`). New feature tables
are no longer added to `j-lawyer-server/setup/create_database.sql`; Flyway's baseline
(1.13.0.16) applies them to fresh and existing databases alike.

No rollback script: dropping the two tables is the rollback and loses only what was recorded
after the upgrade.

## Open Questions

None — the two that were open are decided above: German-only labels, and no relationship
display in the case party list beyond the add-party submenu.
