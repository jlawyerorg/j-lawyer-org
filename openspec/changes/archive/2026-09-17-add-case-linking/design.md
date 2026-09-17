## Context

Cases (`ArchiveFileBean`, table `cases`) have many child entities (documents, parties,
tags, reviews, invoices) but no relation to each other. Ids are application-generated
strings (`StringGenerator.getID()`, ~33 hex chars), not database sequences, so both sides
of a link are plain `VARCHAR` foreign keys.

Case visibility is group-based: `SecurityUtils.checkGroupsForCase(principal, aFile,
securityFacade, allowedGroups)` throws for a case the caller may not see, and
`SecurityUtils.getAllowedCasesForUser(...)` is used where lists are filtered
(`ArchiveFileService.java:1093`, `:1166`). A link therefore joins two objects whose
visibility is decided independently — the interesting design question of this change.

The desktop case editor is a single shared instance swapped into the main editor pane
(`EditorsRegistry.setMainEditorsPaneView`); there are no per-case windows. The first tab
("Allgemeine Daten", `ArchiveFilePanel.java:3240`) holds the Aktenkopf panel and a
`JSplitPane` `splitMessages` whose left half is `splitNotes` (tags on top, Notizen below)
and whose right half is the message stream.

## Goals / Non-Goals

- Goals
  - One symmetric relation, readable and maintainable from either case, with an optional
    free-text description.
  - Navigation in both directions with one UI component, reusing the established
    "open a case by id" idiom.
  - "Neue verknüpfte Akte" must not fork the duplication logic — one implementation for
    both entry points.
  - Additive server API; older desktop clients keep working.
- Non-Goals
  - Directed link types with a counterpart label per direction, link type administration.
  - Link graphs/transitive navigation (A–B, B–C does not make A–C a link).
  - Copying documents, deadlines, invoices or timesheets into a new linked case.

## Decisions

### Decision: one row per pair with normalised column order

`case_links(id, case_id_a, case_id_b, description, date_created, created_by)` stores a
link once. On insert the service orders the two ids so that `case_id_a.compareTo(case_id_b)
< 0`, and a unique index `UQ_CASELINKS_PAIR (case_id_a, case_id_b)` then rejects a duplicate
regardless of which side the user linked from. Reading uses
`WHERE c.caseA = :caseKey OR c.caseB = :caseKey`, so "navigation in both directions" is a
property of the data, not of two rows that could drift apart.

Alternatives considered:
- *Two rows, one per direction*: every write becomes two writes and every delete two
  deletes; a half-failed operation leaves a one-way link that the UI shows on one case only.
- *Unordered single row without normalisation*: duplicates in the opposite direction cannot
  be prevented by a database constraint, only by a read-then-write check that races.
- *`ArchiveFileBean.linkedCases` as a JPA `@ManyToMany` with a join table*: would need an
  owning side, which reintroduces direction, and would pull the relation into every
  `ArchiveFileBean` serialisation towards remote clients.

### Decision: no self-link, enforced twice

`case_id_a <> case_id_b` is checked in the service (fast, with a readable error) and, because
normalisation makes a self-link the pair `(X, X)`, the unique index alone would still allow
exactly one such row — so the service check is the real guard and a `CHECK` constraint is
added for the database to refuse it as well (MySQL 8 enforces `CHECK`; on 5.7 it is parsed
and ignored, which is acceptable because the service check runs first).

### Decision: links are read as a flat DTO, never as case entities

`getCaseLinks(caseId)` SHALL return `List<CaseLinkDTO>` — a plain serializable POJO next to
the existing `MailMessageDTO`/`MailFolderDTO` in `j-lawyer-server-api` — with `linkId`,
`description`, `dateCreated`, `createdBy`, `otherCaseId`, `otherCaseFileNumber`,
`otherCaseName`, `otherCaseReason` and `otherCaseArchived`. Roughly 200 bytes per link.

Returning `CaseLink` with its two `ArchiveFileBean` references would have been the
cheaper code and is what the first version of this design proposed. It is the wrong call,
because an `ArchiveFileBean` is not a small object:

- `ArchiveFileBean.group` is `@OneToOne(fetch = EAGER)` and `ArchiveFileBean.rootFolder` is
  `@OneToOne` **without** a fetch type — i.e. EAGER by JPA default
  (`ArchiveFileBean.java:757-763`).
- `CaseFolder.children` is `@OneToMany(..., fetch = FetchType.EAGER)` and **recursive**
  (`CaseFolder.java:699-700`). Loading one case therefore materialises and serialises its
  **entire folder tree**; a case created from a folder template routinely has 15–30 folders.
- The six `@OneToMany` lists (documents, parties, reviews, history, forms, form entries) are
  lazy, but a detached uninitialised Hibernate collection still travels in the stream and
  blows up in the client the moment anything touches it.

Unlike `getTags`, where Java serialisation writes the one shared case bean exactly once for
all tags, every link points at a **different** case — so N links would mean N case beans and
N folder trees, tens of KB and N extra folder queries for a list that needs four strings and
a boolean. That is exactly the payload growth to avoid.

The DTOs are filled by a JPQL constructor-expression projection over the association, run
once per side (`caseA = :id`, `caseB = :id`), selecting only the scalar columns of the other
case. No `ArchiveFileBean`, no `Group`, no `CaseFolder` is instantiated. The display file
number is composed from `fileNumberMain` + `fileNumberExtension` with the same logic
`ArchiveFileBean.getFileNumber()` uses, kept in one helper so the two cannot drift.

The entity keeps proper associations for the server side, but explicitly lazy:
`@ManyToOne(fetch = FetchType.LAZY) ArchiveFileBean caseA/caseB`. `CaseLink` SHALL NOT
appear in any remote interface signature, so the eager-loading trap cannot be re-introduced
by a later caller.

Opening a linked case goes through `getArchiveFile(id)` as everywhere else — the full bean
is fetched when, and only when, the user actually navigates.

### Decision: permissions are checked for the few candidate ids, not by listing all cases

`SecurityUtils.getAllowedCasesForUser(principal, securityFacade)`
(`SecurityUtils.java:758`) returns the ids of **every** case the user may see; running it on
each case load would be a full-table union query for a filter over a handful of ids. Instead
a new helper `SecurityUtils.filterAllowedCases(principalId, Collection<String> caseIds,
SecurityServiceLocal)` runs the same union SQL restricted by `id IN (:caseIds)` — one
indexed query over at most a few dozen ids.

Note for implementation: the two existing helpers disagree about one constellation. For a
case that *has* an owner group but *no* `case_groups` rows,
`getAllowedCasesForUser` treats it as visible (its last union branch: "cases where there is
restriction although there is an owner") while
`checkGroupsForCase(List<Group>, …)` (`SecurityUtils.java:730`) returns `false`. The link
filter SHALL follow the `getAllowedCasesForUser` rule, because that is what decides which
cases the user sees in search results (`ArchiveFileService.java:1093`) — a link must not
show a case the search would hide, nor hide one the search shows.

### Decision: loaded in the existing parallel loader, as one more call

The desktop case load already fans 16 independent remote calls out over an
`Executors.newFixedThreadPool(8)` and joins them
(`ArchiveFileDetailLoadAction.java:818-905`). `getCaseLinks` is submitted there as one more
`Future`, so it costs no extra round-trip latency on the critical path — with one caveat: 16
tasks on 8 threads are two waves, and a 17th task starts a third wave. The pool size SHALL
be raised alongside (these tasks are I/O bound, not CPU bound), otherwise the cheapest call
of the set could add a full wave to the case load. `ArchiveFilePanel.setArchiveFileDTO` keeps
doing no remote call of its own.

### Decision: visibility of the *other* case is filtered, not thrown

`getCaseLinks(caseId)` checks the caller's access to the case being read (throws, like every
other read on that case) and then **omits** links whose other case the caller may not see,
using the allowed-cases/groups check already in `ArchiveFileService`. A restricted link is
not rendered as a placeholder either: showing "keine Berechtigung" next to a file number
would leak that a case exists and how it is numbered. `linkCases` requires write access to
the case the user acts on and read access to the other case, so a link cannot be used to
probe for cases.

Trade-off: a user with narrower group membership sees fewer links than a colleague and can
re-create a link that already exists; the unique index turns that into a clean
"already linked" message rather than a duplicate row.

### Decision: history entries on both cases

`linkCases` and `unlinkCases` call `addCaseHistory(newId, dto, description)`
(`ArchiveFileService.java:1441`) once per case ("Akte verknüpft mit 12/24 (Mandant Müller)"
/ "Verknüpfung mit 12/24 entfernt"). That also bumps `cases.date_changed` on both sides via
the raw JDBC update the method already does, so the "zuletzt geändert" lists surface the
change. A description edit writes no history entry — it is a free-text annotation, and the
noise is not worth it.

### Decision: shared duplication helper, dialog instead of a second menu variant

`duplicateSelectedArchiveFiles` is moved (unchanged in behaviour) into
`com.jdimension.jlawyer.client.utils.CaseUtils.duplicateCase(ArchiveFileBean source,
String newName, boolean includeForms)`, returning the created case.
`QuickArchiveFileSearchPanel` keeps both of its popup items and calls the helper per selected
row; the new "neue verknüpfte Akte" action calls the same helper once and then `linkCases`.
The new action opens a small `NewLinkedCaseDialog` with a pre-filled short name (the source
name **without** the " (Kopie)" suffix the popup action adds), a checkbox
"Falldaten übernehmen" (default off, matching plain "duplizieren") and the link description —
one dialog instead of a second "… (inklusive Falldaten)" menu entry.

### Decision: placement in the Aktenkopf as a wrapping chip row

The links get a new row inside the "Aktenkopf" panel (`jPanel1`) of the first tab, directly
below "Sachgebiet:". No split pane is added: the first tab already nests `splitMessages`,
`splitNotes`, `splitDocuments`, `splitDocumentsMain` and the `tabPrivileges` tabbed pane, and
a further divider would make the tab harder to read, not richer.

`pnlLinkedCases` is a plain `JPanel` declared in `ArchiveFilePanel.form` (with the
GroupLayout the GUI Builder generates) whose layout is replaced at runtime with
`new WrapLayout(FlowLayout.LEFT, 4, 2)` — literally the pattern already used for the tag
panels (`ArchiveFilePanel.java:1007`: `this.tagPanel.setLayout(new WrapLayout())`). Entries
therefore flow horizontally and wrap into a second row only when the width runs out.

**The empty state really is free.** `WrapLayout.layoutSize` returns `insets + 2 * vgap` for a
container without visible children, so an empty row is ~4 px. More importantly, `jPanel1`'s
height is `max(left column, tabPrivileges)`: the two are in one parallel group, bottom
aligned (`ArchiveFilePanel.form:806-847`), and `tabPrivileges` asks for ~180 px (its inner
scroll pane alone is `pref="159"`, `ArchiveFilePanel.form:1094`) while the five existing
rows of the left column come to ~155 px. The first chip row fits into that existing slack,
so the Aktenkopf does not grow at all for a case with a handful of links; only a second row
adds height, and then by ~24 px.

GroupLayout wiring (both `.java` and `.form`, kept consistent for the GUI Builder):
- horizontal — `pnlLinkedCases` becomes a third entry of the parallel group that already
  holds the Aktenzeichen row and the labels/fields group
  (`ArchiveFilePanel.java:3046-3079`), resizable to `Short.MAX_VALUE`, so the row spans the
  label *and* field columns and can hold more chips per line than the field column alone.
- vertical — appended to the left sequential group after the Sachgebiet baseline group
  (`ArchiveFilePanel.java:3117-3120`) as
  `.add(pnlLinkedCases, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)`, i.e. exactly as tall
  as its content.

One pitfall to handle: a `WrapLayout`'s preferred height depends on the container's current
width, which GroupLayout only knows after a layout pass — both existing tag panels dodge
this by sitting in a `JScrollPane`. Here the panel asks for the second pass itself: a
`ComponentListener` on `pnlLinkedCases` calls `revalidate()` when its **width** changed, and
the chip add/remove code calls `revalidate()` + `repaint()` as well. This converges, because
only the width feeds the computed height.

No titled border and no static label ("Verknüpfungen:") — either would cost a permanent row
in the empty state and defeat the point of the placement. The row identifies itself through
the link icon each chip carries and through the tooltip of the "+" chip.

Alternatives considered:
- *A titled "verknüpfte Akten" section in a nested split pane* (the first version of this
  proposal): rejected — more dividers on an already divided tab, and a permanent titled
  border even for the majority of cases that have no links.
- *An own tab*: buries the relation and contradicts the requirement to see it on the first
  tab.
- *A `JTable` or `BoxLayout` list in the Aktenkopf*: cannot collapse to nothing, wastes the
  horizontal space that short file numbers leave, and a one-row-per-link list of 6 links
  would push the Aktenkopf ~100 px taller.

### Decision: navigation on the chip, maintenance in its context menu

Each link renders as `LinkedCaseChip extends JButton`, styled like the `TagToggleButton`
chips so the row reads as one visual family with the Etiketten: link icon
(`/icons16/material/link_24dp_0E72B5_FILL0_wght400_GRAD0_opsz24.png`), text = file number
plus the description when set (truncated), archived cases greyed out with an "(abgelegt)"
suffix, and a tooltip carrying Kurzrubrum, full description and archived state.

- **Left click = open the linked case** — the primary, most frequent action gets the
  primary gesture (`getArchiveFile(id)` + `EditorsRegistry`, the idiom at
  `CaseForContactEntryPanel.java:893`).
- **Right click = `popLinkedCase`** with "öffnen", "Beschreibung bearbeiten…" and
  "Verknüpfung entfernen" (with confirmation). Popup menus are how every other list in this
  editor (documents, reviews, invoices) offers its per-row actions, so this needs no new
  vocabulary, and it keeps a dense chip row readable.
- **Adding** happens through one permanent `cmdAddLink` chip rendered last in the flow
  (`/icons/edit_add.png`, tooltip "Akte verknüpfen"). A left click opens a small
  `JPopupMenu` with exactly two items — "bestehende Akte verknüpfen…" and "neue verknüpfte
  Akte erstellen…" — following `cmdIngoChat`, which already shows a popup below its button
  (`ArchiveFilePanel.java:9486-9487`). Two actions behind one 22 px chip instead of a
  toolbar row.

In the read-only editor (`ViewArchiveFileDetailsPanel`) and for an unsaved new case the "+"
chip is **hidden** (not merely disabled) and the context menu offers only "öffnen", so a
read-only case header shows nothing but the links themselves.

Rejected: a small "✕" inside every chip — at chip size its hit area competes with the
navigation click, and it adds ~14 px per chip to a row whose whole point is compactness.
Removal stays one gesture away in the context menu.

Naming note: the action is called "neue verknüpfte Akte erstellen", not "neue Unterakte" —
the model is deliberately symmetric, so no chip is a parent or a child of another. If a
parent/child wording is wanted in the UI, it needs the directed link types this change
lists as out of scope.

### Decision: REST shape

`GET /v8/cases/{id}/links` → `RestfulCaseLinkV8[]` with `id`, `description`, `creationDate`,
`createdBy` and a flattened `linkedCaseId` / `linkedCaseFileNumber` / `linkedCaseName` /
`linkedCaseArchived` — the linked case as seen *from the requested case*, so a client never
has to work out which of two ids is "the other one".
`PUT /v8/cases/{id}/links` creates (body: `linkedCaseId`, `description`),
`PUT /v8/cases/{id}/links/{linkId}` updates the description,
`DELETE /v8/cases/{id}/links/{linkId}` removes. PUT-for-create matches the existing v8 case
sub-resources (payments, tags). Errors use the uniform error envelope `SwaggerFinalizer`
already documents.

## Risks / Trade-offs

- **Payload growth on case load** → addressed by the DTO decision above. The remaining cost
  of a case load is one additional parallel remote call returning ~200 bytes per link.
  Worth knowing while implementing: `getTags` (and every other read returning a bean with an
  `@ManyToOne ArchiveFileBean`, which defaults to EAGER — `ArchiveFileTagsBean.java:691-693`)
  already ships a full case bean including the eagerly loaded folder tree. That is an
  existing inefficiency, mitigated only by Java serialisation sharing the one identical case
  object; cleaning it up is out of scope here, but this change must not add to it.
- **A case with many links widens the Aktenkopf row** → chips wrap, and every additional
  row costs ~24 px of the first tab. Mitigation: the chip text is the file number plus a
  truncated description, not the full Kurzrubrum, so a typical chip stays ~120 px wide and
  6–8 links fit in one row; the details are in the tooltip. If a firm really links dozens of
  cases, the row grows — accepted, because the alternative (a fixed-height scrolling list)
  costs that space in *every* case.
- **`WrapLayout` outside a scroll pane** → the width/height feedback described above needs
  the explicit `revalidate()` on width change; without it a wrapped second row would be
  clipped. To be verified by hand at narrow window widths and after adding a chip.
- **Cases deleted while a link exists** → `ON DELETE CASCADE` on both foreign keys removes
  the link row; no orphan handling needed, and the surviving case simply shows one entry
  less.
- **Link count on a "hub" case** (one case linked to dozens) → list is scrollable, `GET`
  returns all links; no paging, since the realistic upper bound is tens of rows and the
  existing parties/messages lists make the same assumption.
- **Old desktop clients** against a new server keep working (new methods are additive); a
  new client against an old server fails on lookup of the new methods — same as every
  previous service addition, no extra mitigation.

## Migration Plan

Additive Flyway migration `V3_6_0_27__CaseLinks.sql` (create table, indexes, version upsert
into `server_settings`), run by `DatabaseMigrator` when Hibernate boots the persistence
unit. No data backfill, no rollback script: dropping the table would be the rollback and
loses only links created after the upgrade.

## Open Questions

- Should a link description be shown in the Aktenkopf HTML export / Deckblatt print? Left
  out for now; can be added once the section has proven itself.
- Should "neue verknüpfte Akte" also be offered from the search result popup (next to
  "duplizieren")? Not in this change — the request is explicitly an action inside the case.
