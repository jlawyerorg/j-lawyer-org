## 1. Data model

- [x] 1.1 Add Flyway migration
      `j-lawyer-server-entities/src/main/resources/db/migration/V3_6_0_27__CaseLinks.sql`:
      table `case_links` (`id` VARCHAR(50) BINARY PK, `case_id_a` / `case_id_b` VARCHAR(50)
      BINARY NOT NULL, `description` VARCHAR(250) BINARY, `date_created` DATETIME,
      `created_by` VARCHAR(50) BINARY), both FKs on `cases(id)` `ON DELETE CASCADE`,
      `CHECK (case_id_a <> case_id_b)`, unique index `UQ_CASELINKS_PAIR (case_id_a,
      case_id_b)`, index `IDX_CASELINKS_B (case_id_b)`, plus the
      `server_settings`/`jlawyer.server.database.version` upsert ('3.6.0.27') and `commit;`
      — with a leading comment block explaining the normalised pair, as in
      `V3_6_0_20__DunningCaseDeadlines.sql`. `V3_6_0_26` is the latest today — re-check the
      highest existing version at implementation time, since other changes are in flight
- [x] 1.2 Add entity
      `j-lawyer-server-entities/src/main/java/com/jdimension/jlawyer/persistence/CaseLink.java`:
      `@Table(name = "case_links")`, `@Id String id`, two
      `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "case_id_a"/"case_id_b",
      referencedColumnName = "id") ArchiveFileBean caseA/caseB` — **LAZY on purpose**, an
      eager `ArchiveFileBean` drags its EAGER `group` and `rootFolder` and with it the whole
      recursive `CaseFolder.children` tree — plus `description`, `dateCreated`, `createdBy`,
      `Serializable`, `equals`/`hashCode`/`toString` in the style of `DunningCaseDeadline`
- [x] 1.3 Add `CaseLinkDTO` to `j-lawyer-server-api/src/main/java/com/jdimension/jlawyer/services/`
      (next to `MailMessageDTO`): `linkId`, `description`, `dateCreated`, `createdBy`,
      `otherCaseId`, `otherCaseFileNumber`, `otherCaseName`, `otherCaseReason`,
      `otherCaseArchived`; plain `Serializable` POJO
- [x] 1.4 Add two projection queries with a constructor expression filling `CaseLinkDTO` from
      scalar columns of the other case — one for `caseA = :caseKey`, one for
      `caseB = :caseKey` — so no `ArchiveFileBean`, `Group` or `CaseFolder` is instantiated;
      compose the display file number from `fileNumberMain` + `fileNumberExtension` with the
      same logic as `ArchiveFileBean.getFileNumber()`, extracted into one shared helper
      (`ArchiveFileBean.composeFileNumber`). The two queries live in `CaseLinkFacade` as typed
      `createQuery` constants rather than as named queries on the entity: a named query naming a
      class from `j-lawyer-server-api` would only resolve at runtime and fail the persistence
      unit's boot-time validation if it did not
- [x] 1.5 Add facades
      `j-lawyer-server/j-lawyer-server-ejb/src/main/java/com/jdimension/jlawyer/persistence/CaseLinkFacade.java`
      and `CaseLinkFacadeLocal.java` (`findByCase(String)`) following
      `DunningCaseDeadlineFacade`

## 2. EJB service

- [x] 2.1 Inject `CaseLinkFacadeLocal` into `ArchiveFileService`
- [x] 2.2 Implement `List<CaseLinkDTO> getCaseLinks(String caseId)`: `checkGroupsForCase` on
      the requested case, then run the two projection queries and drop links whose other case
      the caller may not see
- [x] 2.3 Add `SecurityUtils.filterAllowedCases(String principalId, Collection<String> caseIds,
      SecurityServiceLocal)` — the union SQL of `getAllowedCasesForUser`
      (`SecurityUtils.java:758`) restricted by `id IN (:caseIds)`, one indexed query instead
      of enumerating every case the user may see. Follow that method's visibility rule, not
      the diverging one of `checkGroupsForCase(List<Group>, …)` (`SecurityUtils.java:730`),
      so links and search results agree on what is visible
- [x] 2.4 Implement `CaseLinkDTO linkCases(String caseId, String otherCaseId, String description)`:
      write access on `caseId`, read access on `otherCaseId`, reject self-link, normalise the
      id order, reject an existing pair with a distinguishable exception/message, id from
      `StringGenerator`, `date_created` + `created_by` from the caller principal
- [x] 2.5 Implement `void updateCaseLinkDescription(String linkId, String description)` and
      `void unlinkCases(String linkId)` (write access on at least one of the two cases)
- [x] 2.6 Write `addCaseHistory` entries on both cases in `linkCases` and `unlinkCases`
      (naming the other case's file number and short name); none on description update
- [x] 2.7 Add the four methods to `ArchiveFileServiceLocal`
- [x] 2.8 Add the four methods to `ArchiveFileServiceRemote` **with English JavaDoc**
      (params, return, thrown exception, permission requirements)

## 3. REST API v8

- [x] 3.1 Add `j-lawyer-server/j-lawyer-io/src/main/java/org/jlawyer/io/rest/v8/pojo/RestfulCaseLinkV8.java`
      (`id`, `description`, `creationDate`, `createdBy`, `linkedCaseId`,
      `linkedCaseFileNumber`, `linkedCaseName`, `linkedCaseArchived`) with a
      `fromCaseLinkDTO(CaseLinkDTO)` factory (the DTO is already flat and oriented from
      the requested case) and swagger annotations
- [x] 3.2 `GET /v8/cases/{id}/links` in `CasesEndpointV8` (+ `CasesEndpointLocalV8`)
- [x] 3.3 `PUT /v8/cases/{id}/links` (body `linkedCaseId`, `description`) returning the
      created link; duplicate pair → error envelope with a clear message
- [x] 3.4 `PUT /v8/cases/{id}/links/{linkId}` (description update) and
      `DELETE /v8/cases/{id}/links/{linkId}`, both verifying the link belongs to `{id}`
- [x] 3.5 Annotate all four with `@Operation`/`@ApiResponse` so the generated swagger.json
      documents them (no manual swagger edit)

## 4. Desktop: duplication refactor

- [x] 4.1 Move the duplication logic from
      `QuickArchiveFileSearchPanel.duplicateSelectedArchiveFiles`
      (`QuickArchiveFileSearchPanel.java:1277`) into
      `com.jdimension.jlawyer.client.utils.CaseUtils.duplicateCase(ArchiveFileBean source,
      String newName, boolean includeForms)` returning the created `ArchiveFileBean`
      (master data reset, parties, tags, allowed groups, optional forms — behaviour
      unchanged)
- [x] 4.2 Rewire both popup items (`mnuDuplicateSelectedArchiveFiles`,
      `mnuDuplicateSelectedArchiveFilesWithForms`) to the helper, keeping the
      " (Kopie)" suffix, the multi-selection confirmation and the `CasesChangedEvent`
- [x] 4.3 Verify by hand that duplicating one and several cases still behaves as before

## 5. Desktop: links row in the Aktenkopf

- [x] 5.1 Add `LinkedCaseChip.java` under
      `j-lawyer-client/src/main/java/com/jdimension/jlawyer/ui/tagging/` (no `.form`; a
      hand-written `JButton` like `TagToggleButton`): link icon
      `/icons16/material/link_24dp_0E72B5_FILL0_wght400_GRAD0_opsz24.png`, text = file
      number + truncated description, greyed/"(abgelegt)" for an archived case, tooltip with
      Kurzrubrum + full description; left click opens the case via `getArchiveFile(id)` +
      `EditorsRegistry` (the idiom at `CaseForContactEntryPanel.java:893`)
- [x] 5.2 Add `pnlLinkedCases` to `ArchiveFilePanel.form` + `.java` inside `jPanel1`
      ("Aktenkopf"): horizontally a third entry of the parallel group holding the
      Aktenzeichen row and the labels/fields group (resizable to `Short.MAX_VALUE`),
      vertically appended after the `jLabel9`/`cmbSubjectField` baseline group as
      `PREFERRED_SIZE / DEFAULT_SIZE / PREFERRED_SIZE`; no border, no label. Keep both files
      consistent for the GUI Builder; do **not** add a split pane
- [x] 5.3 Replace the generated layout at runtime with
      `this.pnlLinkedCases.setLayout(new WrapLayout(FlowLayout.LEFT, 4, 2))` next to the
      existing `tagPanel`/`documentTagPanel` calls (`ArchiveFilePanel.java:1007`)
- [x] 5.4 Add a `ComponentListener` on `pnlLinkedCases` that calls `revalidate()` when its
      **width** changed, and `revalidate()` + `repaint()` after every chip add/remove — the
      `WrapLayout` preferred height depends on the current width and there is no scroll pane
      to supply one
- [x] 5.5 Load the links as one more `Future` in the parallel block of
      `ArchiveFileDetailLoadAction.java:818-905` and populate the row from it (in the style of
      the tag panel population at `:1093-1182`); clear it in the reset path. Raise the
      `Executors.newFixedThreadPool(8)` size so the extra I/O-bound task does not start a
      third wave
- [x] 5.6 Add the permanent `cmdAddLink` chip as the last component of the row
      (`/icons/edit_add.png`, tooltip "Akte verknüpfen"); left click shows a `JPopupMenu`
      with "bestehende Akte verknüpfen…" and "neue verknüpfte Akte erstellen…", positioned
      like the popup of `cmdIngoChat` (`ArchiveFilePanel.java:9486`)
- [x] 5.7 Add the per-chip context menu `popLinkedCase` ("öffnen", "Beschreibung
      bearbeiten…", "Verknüpfung entfernen") with the client's icon conventions
- [x] 5.8 Hide `cmdAddLink` and reduce the context menu to "öffnen" in the read-only editor
      (`ViewArchiveFileDetailsPanel`) and for an unsaved new case
- [x] 5.9 Add `getCaseLinks`/`linkCases`/`unlinkCases`/`updateCaseLinkDescription` usage via
      `JLawyerServiceLocator.lookupArchiveFileServiceRemote()`
- [x] 5.10 Check the empty and one-row state against the height budget: `jPanel1`'s height is
      `max(left column, tabPrivileges)` — confirm by hand that the Aktenkopf does not grow
      for a case without links and for one whose chips fit on a single line

## 6. Desktop: the two actions

- [x] 6.1 "bestehende Akte verknüpfen…": open `SearchAndAssignDialog`
      (`editors/documents/SearchAndAssignDialog.java:737`), read `getCaseSelection()`,
      reject self-link and already-linked with a message, ask for the description, create
      the link and append the chip
- [x] 6.2 Add `NewLinkedCaseDialog.java` + `.form`: short name (pre-filled, without the
      "(Kopie)" suffix), checkbox "Falldaten übernehmen" (default off), link description,
      OK/Cancel
- [x] 6.3 "neue verknüpfte Akte erstellen…": `CaseUtils.duplicateCase(...)` → `linkCases(...)` → publish
      `CasesChangedEvent` → open the new case in the editor; if the link fails, keep the
      case and inform the user
- [x] 6.4 Confirm before removing a link; edit the description in place and refresh the row
      without reloading the case

## 7. Web client

- [x] 7.1 Add `CaseLink` to
      `j-lawyer-web/frontend/src/app/akten/case.models.ts`
- [x] 7.2 Add `caseLinks(id)`, `createCaseLink(id, linkedCaseId, description)`,
      `updateCaseLink(...)`, `deleteCaseLink(id, linkId)` to `cases.service.ts` against
      `/v8/cases/{id}/links`
- [x] 7.3 Add the "verknüpfte Akten" card to the overview tab in `akten.component.ts`
      (lazy-loaded per case, empty state, archived marker), navigation via the `/cases/:id`
      route, add via the existing case picker used for party/case lookups, remove with
      confirmation
- [x] 7.4 Add the i18n keys under `akten.links.*` to
      `j-lawyer-web/frontend/public/i18n/de.json` and `en.json`

## 8. Verification

- [x] 8.1 Server unit test for the normalisation + duplicate/self-link rejection
      (`*Test.java` in the ejb module's `src/test/java`)
- [x] 8.2 Verify the read path stays flat: with SQL logging on, confirm that reading the links
      of a case issues a constant number of queries and loads no `CaseFolder` rows, and that
      opening a case with several links is not slower than before
- [x] 8.3 Manual REST check against the Docker environment (admin:a): GET/PUT/PUT/DELETE on
      `/v8/cases/{id}/links`, including duplicate and unknown-id responses, and confirm the
      generated `swagger.json` contains the four operations
- [x] 8.4 Manual desktop check: link, navigate A→B→A, edit description, remove, read-only
      editor, "neue verknüpfte Akte" with and without Falldaten, case deletion removes
      links; plus the layout cases — no links, one chip row, several chip rows, window
      narrowed until the chips wrap (no clipping), and the Aktenkopf height unchanged in the
      empty state
- [x] 8.5 Manual web check: card, navigation, add, remove, both languages
- [x] 8.6 `openspec validate add-case-linking --strict`

## Notes

4.3, 5.10 and 8.2-8.5 needed a build and a running server, so they were verified by the user
(jens@office-42.de) after the implementation was handed over: tested successfully.

Deviations and additions made while implementing:
- `CaseLinkExistsException` (new, `j-lawyer-server-api`) so a client can tell "already linked"
  from a real failure; the desktop shows it as a notice, REST returns it in the error envelope
  with `error: "CaseLinkExistsException"` (status 500 - the envelope helper only emits 500).
- `CaseLink.normalisePair(...)` holds the pair normalisation, so the property "either
  direction yields the same row" is unit-testable (`CaseLinkPairTest`).
- `ArchiveFileBean.composeFileNumber(...)` extracted from `getFileNumber()` for the projection.
- `SecurityUtils.filterAllowedCases(...)` added as planned; it follows the visibility rule of
  `getAllowedCasesForUser`, not the narrower one of `checkGroupsForCase(List<Group>, ...)`.
- `ArchiveFilePanel.isReadOnlyEditor()` (overridden in `ViewArchiveFileDetailsPanel`) had to be
  introduced for the read-only gating: the existing `readOnly` field is never assigned - the
  assignment in `setReadOnly` is commented out - so it reports "editable" everywhere.
- `RestfulCaseLinkRequestV8` added as the write payload, following the other v8 write endpoints.
- The links row is loaded as a 17th parallel call in `ArchiveFileDetailLoadAction`; the thread
  pool was raised from 8 to 17 so the added I/O-bound call does not start another wave.
