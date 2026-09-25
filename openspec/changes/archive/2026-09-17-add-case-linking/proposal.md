# Change: Link cases to each other (Aktenverknüpfung)

## Why

Law firms routinely work on matters that belong together but must stay separate cases:
the criminal matter and the civil damages claim out of the same accident, a firm's case
per family member, the Mahnverfahren case and the case its title is enforced from, a
follow-up instruction (Folgesache) after the first matter was closed, the opposing party's
counter-suit. Today the only way to record that relationship is a sentence in "Notizen" or
a naming convention in the short name (Kurzrubrum) — neither is navigable, neither survives
a rename, and neither shows up when a colleague opens the *other* case.

The desktop client already offers "duplizieren" for the common case of starting a new
matter from an existing one (`QuickArchiveFileSearchPanel.duplicateSelectedArchiveFiles`,
`QuickArchiveFileSearchPanel.java:1277`), but the copy and the original end up with no
recorded relationship at all, and the action is only reachable from the search result list,
not from the case that is open in front of the user.

## What Changes

- **New capability `case-linking`**: a symmetric, navigable link between two cases with an
  optional free-text description ("Gegenakte", "Folgesache zu 12/24", …). Link types are
  deliberately *not* a configurable option list — a free-text field keeps the model
  symmetric and needs no administration.
- **Data model**: new table `case_links` (Flyway `V3_6_0_27__CaseLinks.sql`) and JPA entity
  `CaseLink` with two `cases` foreign keys, `ON DELETE CASCADE` on both, plus a
  normalised unique key so the same pair cannot be linked twice in either direction and a
  case cannot be linked to itself.
- **EJB service**: `getCaseLinks`, `linkCases`, `updateCaseLinkDescription`, `unlinkCases` on
  `ArchiveFileService` with Remote + Local interfaces. Reads return a flat `CaseLinkDTO`
  (never the entity): an `ArchiveFileBean` eagerly pulls its `group` and its `rootFolder`,
  and `CaseFolder.children` is EAGER and recursive, so returning case beans per link would
  ship a whole folder tree per linked case into every case load. Reading follows the existing
  group-based case visibility, filtered for the handful of linked ids rather than by
  enumerating all cases the user may see; linking and unlinking write a case history entry
  ("Historie") on **both** cases.
- **Desktop client**: the linked cases appear in the "Aktenkopf" panel of the first tab
  ("Allgemeine Daten"), in a new row below "Sachgebiet:", as chips in a wrapping
  (`WrapLayout`) row — the same layout the "Akten-Etiketten" panel already uses, so entries
  sit next to each other and the row costs practically no space when a case has no links.
  No further split pane is added. Clicking a chip opens that case in the main editor pane —
  this is what makes navigation work in both directions, because the same row is shown on
  the other case.
- **Desktop actions**: a permanent "+" chip at the end of the row opens a small menu with
  "bestehende Akte verknüpfen…" (using the existing case picker
  `SearchAndAssignDialog`, `editors/documents/SearchAndAssignDialog.java:704`) and "neue
  verknüpfte Akte erstellen…". Editing a description and removing a link live in the
  per-chip context menu, next to "öffnen".
- **"Neue verknüpfte Akte"**: creates a new case from the currently open one
  with the same copy scope as "duplizieren" (master data, parties, tags, allowed groups,
  optionally Falldaten/forms), links the two cases and opens the new case. The duplication
  logic is extracted from `QuickArchiveFileSearchPanel` into a reusable helper so both
  entry points share one implementation; the existing popup action keeps its behaviour.
- **REST API v8** (`/v8/cases/{id}/links`, GET/PUT/DELETE) so integrations and the web
  client can read and maintain links. Note: the latest API version in the code is **v8**,
  not v7 as `CLAUDE.md` still states — additive, no existing version changes.
- **Web client**: a "verknüpfte Akten" card on the case overview tab with navigation via the
  existing `/cases/:id` deep link, plus add/remove and `de`/`en` i18n keys.

Out of scope: link types/roles with a directed counterpart (e.g. "Vorakte"/"Folgeakte"),
linking cases across installations, copying documents into a newly created linked case,
MCP tools for links, and showing links in the Lucene full-text search index.

## Impact

- Affected specs: `case-linking` (new capability). Desktop, REST and web requirements are
  all kept inside this capability on purpose, so this change does not collide with the
  pending `add-web-client` change, whose `web-client` capability is not in `openspec/specs/`
  yet.
- Affected code:
  - `j-lawyer-server-entities/src/main/java/com/jdimension/jlawyer/persistence/CaseLink.java` (new)
  - `j-lawyer-server-entities/src/main/resources/db/migration/V3_6_0_27__CaseLinks.sql` (new)
  - `j-lawyer-server/j-lawyer-server-ejb/.../persistence/CaseLinkFacade.java` + `CaseLinkFacadeLocal.java` (new)
  - `j-lawyer-server/j-lawyer-server-ejb/.../services/ArchiveFileService.java` (+ `ArchiveFileServiceLocal.java`)
  - `j-lawyer-server-api/.../services/ArchiveFileServiceRemote.java` (JavaDoc required),
    new `j-lawyer-server-api/.../services/CaseLinkDTO.java`
  - `j-lawyer-server/j-lawyer-server-ejb/.../server/utils/SecurityUtils.java` (new
    `filterAllowedCases` helper)
  - `j-lawyer-server/j-lawyer-io/.../rest/v8/CasesEndpointV8.java` (+ `CasesEndpointLocalV8.java`,
    new `rest/v8/pojo/RestfulCaseLinkV8.java`)
  - `j-lawyer-client/.../editors/files/ArchiveFilePanel.java` + `ArchiveFilePanel.form`,
    `ArchiveFileDetailLoadAction.java` (one more parallel fetch)
  - `j-lawyer-client/.../ui/tagging/LinkedCaseChip.java` (new),
    `j-lawyer-client/.../editors/files/NewLinkedCaseDialog.java` + `.form` (new)
  - `j-lawyer-client/.../editors/files/QuickArchiveFileSearchPanel.java` (duplication logic
    moved to `com.jdimension.jlawyer.client.utils.CaseUtils`)
  - `j-lawyer-web/frontend/src/app/akten/akten.component.ts`, `cases.service.ts`,
    `case.models.ts`, `j-lawyer-web/frontend/public/i18n/{de,en}.json`
- No swagger step: `swagger.json` is generated from the annotations on every build.
