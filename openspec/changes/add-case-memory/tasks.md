## 1. Persistence

- [ ] 1.1 Create JPA entity `CaseMemoryBean` (table `case_memory`, PK `case_id`): enabled,
      enabled_by, enabled_date, dossier (MEDIUMTEXT), dossier_built, dossier_meta (TEXT/JSON),
      notes (MEDIUMTEXT), notes_changed_by, notes_changed, dirty_since, rebuild_requested
      (NONE/DOSSIER/FULL), status, last_error, prompt_version
- [ ] 1.2 Create JPA entity `CaseMemoryCardBean` (table `case_memory_cards`, PK
      `document_id`, one row per included document): case_id, included_by, included_date,
      abstract (TEXT), card (MEDIUMTEXT), document_version,
      text_sha256, status (PENDING/OK/NO_TEXT/FAILED), truncated, attempts, next_attempt,
      last_error, model, prompt_version, created
- [ ] 1.3 Create facades `CaseMemoryBeanFacade(Local)` and `CaseMemoryCardBeanFacade(Local)`
      following the existing facade pattern, incl. queries "enabled cases", "cards by case"
- [ ] 1.4 Write the Flyway migration (next free number at implementation time, currently
      `V3_6_0_25`; coordinate with `add-dunning-and-enforcement`): both tables,
      `case_memory.case_id` FK → `cases.id` ON DELETE CASCADE,
      `case_memory_cards.document_id` FK → `case_documents.id` ON DELETE CASCADE, index on
      `case_memory_cards.case_id`; column types matching `cases.id` / `case_documents.id`
- [ ] 1.5 Register the entities in the persistence unit if required by the current setup

## 2. Configuration

- [ ] 2.1 Add keys to `ServerSettingsKeys`: `SERVERCONF_CASEMEMORY_ENABLED`,
      `..._ASSISTANT_CONFIG_ID`, `..._CARD_REQUESTTYPE`, `..._CARD_ACTIONID`, `..._CARD_MODEL`,
      `..._DOSSIER_REQUESTTYPE`, `..._DOSSIER_ACTIONID`, `..._DOSSIER_MODEL`,
      `..._DEBOUNCE_MINUTES`, `..._CARD_MAXINPUTCHARS`, `..._DOSSIER_MAXINPUTCHARS`,
      `..._MAXCALLSPERRUN`
- [ ] 2.2 Create a small settings reader (defaults 10 / 120000 / 200000 / 20, toggle parsing
      `on|1|true` as in `ContactSyncService`)

## 3. Prompts and pure helpers (unit-testable, no EJB dependencies)

- [ ] 3.1 Add prompt resources under `j-lawyer-server-ejb/src/main/resources/casememory/`:
      card system prompt + card prompt, dossier system prompt + dossier prompt (German; card
      skeleton per design D5, dossier sections, citation and attribution rules per D6), plus a
      `PROMPT_VERSION` constant
- [ ] 3.2 `CaseMemoryCardFormat`: build the system header, parse `KURZFASSUNG` into the
      abstract (fallback: first 300 chars), truncate input to budget and report truncation
- [ ] 3.3 `CaseMemoryFingerprint`: SHA-256 over the normalized text preview; empty/whitespace
      → NO_TEXT
- [ ] 3.4 `CaseMemoryDossierInput`: assemble cards in document-date order + master data +
      notes, apply the abstract fallback for the oldest cards when over budget, report
      fallback metadata
- [ ] 3.5 `CaseMemoryCitations`: validate `[D:<id>]` tokens against the input card ids,
      strip unknown ones, count them; render tokens to current document names for display
- [ ] 3.6 `CaseMemoryReconciler`: given the card rows of included documents and the current
      document state (version, deleted, text hash supplier), return the actions (generate /
      regenerate / bump version / exclude from dossier / reuse) and whether the case became
      dirty — pure decision logic; documents without a card row are never considered
- [ ] 3.7 `CaseMemoryMasterData`: render the "Stammdaten" block (file number, name, reason,
      subject field, claim value, lawyer, parties with party type, open `case_events`)

## 4. Ingo access

- [ ] 4.1 Extract the assistant-config lookup + password decryption from
      `IntegrationService.submitAssistantRequest` (≈:1580-1590) into a shared helper and use
      it from both `IntegrationService` and the case memory processor (no behaviour change for
      `IntegrationService`)
- [ ] 4.2 `CaseMemoryLlmClient`: build a fresh `AssistantAPI` per call, submit async with
      request type / action / model from settings and j-lawyer prompts, poll
      `getRequestStatus` every 2 s up to 15 min, distinguish connection errors (global
      back-off) from model errors (per-item failure)

## 5. Server service and background processing

- [ ] 5.1 Create `CaseMemoryServiceRemote` in `j-lawyer-server-api` with English JavaDoc:
      `getStatus(caseId)`, `getContextPackage(caseId)`, `getCards(caseId)`,
      `enable(caseId)`, `disable(caseId)`, `includeDocuments(caseId, documentIds)`,
      `excludeDocuments(caseId, documentIds)`, `getIncludedDocumentIds(caseId)`,
      `setNotes(caseId, notes)`, `requestRebuild(caseId, boolean full)`,
      `getAdminOverview()`; plus DTOs (serializable) for status, context package and card
- [ ] 5.2 Create `CaseMemoryServiceLocal` and `CaseMemoryService` (`@Stateless`) with the role
      and `SecurityUtils.checkGroupsForCase` checks per design D9; `enable` includes no
      documents; `includeDocuments` validates that every document belongs to the case, is not
      deleted and the case memory is enabled, creates `PENDING` rows (idempotent for already
      included ones); `excludeDocuments` deletes the rows and marks the case dirty; case
      history entries for enable, disable, each include/exclude action (names capped at 20)
      and notes change; `disable` deletes all rows and the dossier, keeps notes
- [ ] 5.3 Add `REQUIRES_NEW` persistence methods on the Local interface used by the
      processor (store card, store dossier + meta, mark dirty, record errors, update status)
- [ ] 5.4 Create `CaseMemoryProcessor` (`@Singleton @Startup`, `@Schedule(minute="*",
      persistent=false)`, `NOT_SUPPORTED`, `AtomicBoolean` overlap guard): skip when globally
      off or in back-off; reconcile enabled cases via `CaseMemoryReconciler`; generate cards
      (LLM outside transactions) within the per-run budget; then rebuild due dossiers
      (debounce elapsed or rebuild requested, no pending cards); retry/back-off per design D4
- [ ] 5.5 Read document text via `ArchiveFileServiceLocal.getDocumentPreview(id, TYPE_TEXT)`
      (unrestricted path for the principal-less timer) and master data via local services
- [ ] 5.6 Status counts: incorporated / pending / failed / no text / truncated / stale
      prompt version / not included; "not incorporated" list (included documents only) for the
      context package; not-included documents as a count only

## 6. REST API v8

- [ ] 6.1 Create `RestfulCaseMemoryV8` (context package) and `RestfulCaseMemoryCardV8`
      POJOs in `v8/pojo/` with swagger annotations
- [ ] 6.2 Add `GET /v8/cases/{id}/memory` and `GET /v8/cases/{id}/memory/cards` to
      `CasesEndpointV8` (`@RolesAllowed readArchiveFileRole`, JNDI lookup of
      `CaseMemoryServiceLocal`, `RestErrorResponses` on failure, `enabled=false` for
      non-enabled cases)
- [ ] 6.3 Rebuild and verify both operations appear in the generated `swagger.json`

## 7. Desktop client

- [ ] 7.1 Create `CaseMemoryPanel` (+ `.form`): status/progress line, enable/disable toggle,
      Markdown dossier view (reuse the existing commonmark-based Markdown rendering, e.g.
      `MarkdownPanel`), citations rendered as clickable document names that open the document,
      notes editor with save, card list (name, status, abstract), list of not-included
      documents with multi-select "Aufnehmen", buttons "Dossier neu aufbauen" and "Alle Karten
      neu erzeugen" (with confirmation), enabled/disabled by user roles
- [ ] 7.2 Enable confirmation dialog: names assistant config, model and the `local` flag;
      explicit warning text for non-local models; states that no document is included yet and
      each document must be included individually
- [ ] 7.3 Add the "Aktengedächtnis" tab to `ArchiveFilePanel` (update `ArchiveFilePanel.form`
      consistently); load lazily when the tab is selected; periodic refresh while processing
- [ ] 7.4 Case document list in `ArchiveFilePanel`: load `getIncludedDocumentIds(caseId)`
      when the case memory is enabled, show an inclusion mark per document, add context menu
      entries "Zum Aktenwissen hinzufügen" / "Aus dem Aktenwissen entfernen" for the current
      multi-selection (hidden when the case memory is disabled or roles are missing); refresh
      marks on the client-side document added/removed events
- [ ] 7.5 Create `CaseMemorySetupDialog` (+ `.form`) for admins: global switch, assistant
      config, request type/action/model for cards and dossier (from `getAssistantCapabilities`
      / `getAssistantModels`, local flag shown), debounce, budgets, max calls per run;
      overview counts; persist via `SystemManagement.setSetting`
- [ ] 7.6 Add the dialog entry to the client configuration menu next to the assistant setup
- [ ] 7.7 Register `CaseMemoryServiceRemote` lookup in `JLawyerServiceLocator`

## 8. Opt-in at document assignment (client)

- [ ] 8.1 Create `CaseMemoryOptInPanel` (+ `.form`, GUI Builder palette compatible): checkbox
      "Zum Aktenwissen hinzufügen", `setTargetCase(caseId)` (visible/enabled only for an
      enabled case memory + `writeArchiveFileRole` + `aiAgentRole`, per-case status cached
      ~60 s), resets to unchecked on case change, never persists the choice,
      `includeIfSelected(caseId, documentIds)` with a user-facing message on failure (documents
      stay stored)
- [ ] 8.2 `BulkSaveDialog` (+ `.form`): add the panel dialog-wide, driven by the selected case;
      collect the stored document ids from `afs.addDocument` (:1335) and from entry processors
      (`ScanEntryProcessor` → `assignObservedFile` already returns the id; extend the
      processor `save()` contract to return the created ids); include after the save loop.
      Covers e-mail inbox, beA inbox, scans and Dropscan
- [ ] 8.3 `NewFilenameOptionPanel` (+ `.form`) and `FileUtils.getNewFileName(...)` (:823-:991):
      add the panel when the prompt is used for a case; add an overload returning a result
      object (name + opt-in) and keep the String-returning methods unchanged for other callers
- [ ] 8.4 Migrate the naming-prompt callers that store into a case to the new overload and
      include afterwards: `MailContentUI` (:2294/:2299, :2440), `BeaMessageContentUI` (:1657,
      :2075), `ArchiveFilePanel` copy (:6832), move (:8660), duplicate (:6301, :7702), convert
      to PDF (:6958/:7048), split (:9040), anonymise (:9119), stamp (:9186), reorder (:9272), AI
      result (:10110), `ExportAsPdfMergeStep` (:789/:822), `EpostLetterSendStatus`
      (:897/:920), `MailingStatusPanel` fax/E-POST reports (:1297/:1318), `InvoiceDialog`
      e-invoice XML (:2890/:2900)
- [ ] 8.5 `AddDocumentFromTemplateDialog` (+ `.form`): add the panel; include the created
      document (:1587) and, when "convert to PDF" is chosen, the resulting PDF (:1660)
- [ ] 8.6 `SendEmailFrame` and `SendBeaMessageFrame` (+ `.form`): add the panel next to
      `chkSaveAsDocument`, enabled only while that box is checked and a case is selected; pass
      the choice through `SendAction` (SMTP :1036 and server-based :1269),
      `SendEncryptedAction` (:915), `SendBeaMessageAction` (:916) and `SaveBeaMessageAction`
      (:864); exclude the draft autosave (`SendEmailFrame.performAutoSave`)
- [ ] 8.7 `AddNoteFrame` (+ `.form`): add the panel; include the note document (:1325)
- [ ] 8.8 `UploadDocumentsAction` (:777): after an upload into a case with an enabled case
      memory, show a non-blocking notice "N Dokumente hochgeladen – zum Aktenwissen
      hinzufügen?" with a one-click include; dismissing leaves them not included
- [ ] 8.9 Verify that `ToolRegistry` tools (`save_email_to_case`, move document, note, from
      template) and all server-side paths do not include documents

## 9. Ingo chat integration

- [ ] 9.1 Add `get_case_dossier` and `get_document_cards` (20 per page) to `ToolRegistry`
      with `RISK_LOW`, descriptions per the spec, "not enabled" as a non-error result
- [ ] 9.2 Extend the `get_document_text` / `list_case_documents` descriptions with a hint to
      prefer the dossier where available
- [ ] 9.3 Add the case memory notice (last build, not-incorporated count, recommendation) to
      the chat context when the chat is started from an enabled case (`ArchiveFilePanel`
      chat button, ≈:9440-9481)

## 10. Tests

- [ ] 10.1 Unit tests for `CaseMemoryCardFormat` (abstract parsing, fallback, truncation,
      header)
- [ ] 10.2 Unit tests for `CaseMemoryFingerprint` (NO_TEXT detection, stable hash)
- [ ] 10.3 Unit tests for `CaseMemoryReconciler` (new doc, content change, rename-only,
      recycle bin, restore with same hash, moved document, OCR adds text)
- [ ] 10.4 Unit tests for `CaseMemoryDossierInput` (ordering, budget fallback, metadata)
- [ ] 10.5 Unit tests for `CaseMemoryCitations` (valid, unknown id stripped, rendering after
      rename)
- [ ] 10.6 Manual end-to-end check against a test Ingo instance: enable a case with ~20
      documents and verify nothing is processed; include 10 via multi-select, observe cards +
      dossier; save an e-mail and a scan with and without the opt-in checkbox; let the mailbox
      scanner file a mail and verify it is not included; rename a document, move one to the
      bin, exclude one, add notes, disable; verify case history entries and REST output

## 11. Documentation and optional backend work

- [ ] 11.1 Document the feature for administrators (confidentiality implications, token
      consumption, residual risk: DB content not covered by encryption at rest)
- [ ] 11.2 (Optional, sibling repo `j-lawyer-ai`) Add `case-memory-card` and
      `case-memory-dossier` actions with `usage-types="automated"` and suitable `max-tokens`
      to `j-lawyer-ai.xml`, if the open question on usage-type enforcement requires it
