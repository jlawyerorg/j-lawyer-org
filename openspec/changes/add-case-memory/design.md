## Context

- The Ingo backend (`j-lawyer-ai`) is reached through `AssistantAPI` (`j-lawyer-fax`, no UI
  dependencies, already a dependency of `j-lawyer-server-ejb`). Each `AssistantAPI` instance
  performs one call (`restClient.close()` in `finally`). Async requests are polled via
  `getRequestStatus`, and the stored response is deleted after the first non-`EXECUTING` read.
- `IntegrationService.submitAssistantRequest` is `@RolesAllowed(loginRole)` and not on the
  Local interface. A timer has no caller principal, so it cannot use this path.
- Text extraction already happens once per document: `PreviewGenerator` runs Tika on
  add/change and caches the text as `archivefiles-preview/<caseId>/<docId>.txt`
  (`ArchiveFileService.getDocumentPreview(id, TYPE_TEXT)`, on Local).
- Document lifecycle: add/restore fire `DocumentCreatedEvent`; content change, rename and
  metadata fire `DocumentUpdatedEvent`; moving to the bin fires `DocumentRemovedEvent`.
  Permanent deletion and folder moves fire no event. There is **no server-side "move to other
  case"**: the client copies and deletes. `case_documents.version` is a plain counter bumped on
  changes. There is no content hash.
- Background infrastructure: `@Singleton @Startup` beans with `@Schedule(persistent=false)`
  (`ScheduledTasksService`), async CDI observers (`PdfPreviewGenerationService`), and one MDB on
  `searchIndexProcessorQueue`. That MDB drops messages on error and does not retry.
- Case access is checked with `SecurityUtils.checkGroupsForCase`. Server-internal callers use
  the `*Unrestricted` methods.

## Goals / Non-Goals

**Goals**
- Pre-synthesised, compact, source-attributed context per case that AI consumers can use
  instead of raw document text.
- Processing only after an explicit two-level opt-in: per case, and then per document, with the
  document opt-in available with one click wherever a document is assigned to a case.
- Robust against restarts, failures, deletions, moves and restores, without manual cleanup.
- Hard facts (dates, deadlines, parties, amounts from master data) never come from LLM output.
- No changes required in `j-lawyer-ai`.

**Non-Goals**
- Incremental, in-place editing of the dossier (the full Karpathy wiki, "V3"). The dossier is
  always rebuilt from the cards.
- Cross-case knowledge (for example about the same opposing party across cases).
- Chunked map-reduce for very long single documents: a card is created from a size-capped text
  and flagged as truncated.
- Page-level citations. The text preview carries no reliable page markers, so citations point to
  documents.
- Dossier version history. Only the current dossier is kept.
- Embedding/vector retrieval.
- Tag-, folder- or rule-based automatic inclusion, and a per-case "include new documents
  automatically" switch. Every inclusion is an explicit user decision.
- REST write operations (enable, include/exclude, notes) and MCP exposure. Assignment paths in
  the REST API and the web client therefore store documents without including them. The latter follows once `add-mcp-server` lands and can
  reuse `CaseMemoryServiceLocal`.
- User-editable prompts.

## Decisions

### D1: Two layers, and the dossier is always rebuilt from the cards
**What:** Cards are the durable unit and are regenerated only when a document's text changes.
The dossier is a pure function of (cards, master data, notes, prompt version) and is recomputed
whole.

**Why:**
- Rewriting a dossier incrementally is like a game of telephone: every rewrite can drop or
  distort facts, and errors accumulate.
- A dossier that is only ever extended cannot forget a document that was deleted or moved.
- Rebuilding from cards is cheap because the cards are already small.

**Alternatives:**
- A single rolling summary file: rejected for drift and deletion reasons.
- Rebuilding the dossier from raw document text: rejected, as that is exactly the cost this
  change removes.

### D2: Reconciliation timer instead of event-driven processing
**What:** `CaseMemoryProcessor` (`@Singleton @Startup`, `@Schedule(minute="*", persistent=false)`)
runs every minute and, per enabled case, checks the **included** documents, i.e. the card rows
(see D11), against the current document state:
- card row `PENDING` (freshly included) → generate
- `document.version != card.documentVersion` → recompute the SHA-256 of the text preview. If the
  fingerprint is unchanged (rename, metadata, tag change), only update `documentVersion` and make
  no LLM call. Otherwise regenerate.
- document in the recycle bin → keep the card but exclude it from the dossier. A restore with an
  unchanged fingerprint reuses the card at no cost.
- document deleted permanently → the card goes away via `ON DELETE CASCADE`.

Every change to the set of cards used for the dossier sets `case_memory.dirtySince`. The dossier
is rebuilt when `now - lastChange ≥ debounce` and no card is pending (failed cards do not block).

**Why:**
- The database state is the queue, so there is nothing to lose on restart and no dropped JMS
  messages.
- There is no gap where no event fires: permanent deletion, folder moves and the client-side
  "move to other case" (add + remove) are all covered.
- Documents that are not included are never looked at, so the reconciliation cost scales with
  the number of included documents, not with the size of the case.
- A bulk import of 200 documents produces a single dossier rebuild.

**Alternatives:**
- Async CDI observers on the existing `Document*Event`s: lower latency, but they miss permanent
  deletion and need their own retry and persistence logic. May be added later as a pure
  "wake-up" optimisation.
- A new JMS queue: requires a `standalone.xml` change and has no retry.

**Cost:** one indexed query per enabled case per minute. This is acceptable because processing is
opt-in and the number of enabled cases stays small.

### D3: Transactions and concurrency
**What:**
- The timer method is `@TransactionAttribute(NOT_SUPPORTED)` and guarded by an `AtomicBoolean`,
  so overlapping runs are skipped instead of queued.
- LLM calls happen **outside** any transaction.
- Each persistence step (store a card, store the dossier, update status) runs in its own short
  `REQUIRES_NEW` transaction in `CaseMemoryService`.
- A per-run budget (`maxCallsPerRun`, default 20) caps LLM calls. Cards are processed before
  dossiers.

User actions (enable, rebuild, notes) never call the LLM synchronously. They only change state
that the timer picks up.

### D4: Calling Ingo directly via `AssistantAPI`
**What:**
- The assistant-config lookup and password decryption now in `IntegrationService` (≈:1580-1590)
  move into a small shared helper.
- The processor builds a fresh `AssistantAPI` per call, submits with `asyncRecommended=true`,
  and polls `getRequestStatus` every 2 s up to a 15-minute cap.
- Request type, `actionId` and `model` come from server settings, separately for cards and for
  the dossier.
- `prompt` and `systemPrompt` come from j-lawyer's prompt resources; the document text is passed
  as a STRING input.

**Why:**
- Avoids a principal-less call into `@RolesAllowed` methods. There is no `@RunAs` precedent in
  the code base.
- Keeps `j-lawyer-ai` untouched: it already honours client-supplied prompt and system prompt
  (`V2PipelineRunner`).

**Error handling:**
- Connection errors pause the whole run with a global back-off (5 minutes) instead of burning
  per-card attempts.
- Model errors mark the card `FAILED` with the message and retry up to 3 times with back-off.
  After that the card stays `FAILED` until the document changes or a user requests a full
  rebuild.
- A failed dossier build keeps the previous dossier and records the error on `case_memory`.

### D5: Card format
The card is plain text with a fixed skeleton that the prompt enforces:

```
KURZFASSUNG: <1–2 sentences>
DOKUMENTART: …
URHEBER/ABSENDER: …
DATUM: …
KERNAUSSAGEN:
- <attributed statement, e.g. "Beklagte bestreitet …">
BETRÄGE: …
FRISTEN/TERMINE (im Dokument genannt): …
```

- The header (`[D:<id>] <name> · <document date> · <folder>`) is prepended by j-lawyer.
- `KURZFASSUNG` is parsed into a separate `abstract` column. If the line is missing, the first
  300 characters are used.
- Plain text instead of JSON, because malformed LLM output degrades gracefully instead of
  failing parsing.
- Input text is capped at `card.maxInputChars` (default 120000). Larger documents are marked
  `truncated`, and the dossier and context package state this.
- Documents with empty or whitespace-only text get a `NO_TEXT` card (header only) without an LLM
  call.

### D6: Dossier input budget and citation validation
**Input budget:**
- Input is the full cards in document-date order, the rendered master data and the lawyer's
  notes.
- If the full cards exceed `dossier.maxInputChars` (default 200000), the oldest cards are
  replaced by their abstracts until the input fits. This fallback is deterministic and noted in
  the build metadata.

**Output:**
- Fixed Markdown sections, in German: Sachstand, Beteiligte und Positionen, Chronologie,
  Streitpunkte, Beweismittel, Offene Fragen, Widersprüche.
- The prompt requires `[D:<id>]` on every factual statement and attribution of party
  statements. It also says the notes are the lawyer's own assessment: they take precedence but
  must not be copied into the dossier.
- After generation, every `[D:<id>]` that does not reference a card in the input is removed and
  counted as `invalidCitations` in the build metadata (a hallucinated-source detector).
- Citations are stored by id and rendered to current document names at display time, so renames
  never require a rebuild.

### D7: Master data is rendered live, not generated
The "Stammdaten" block of the context package is built from the database on every read:
- file number, name, reason, subject field, claim value, lawyer
- parties with party type
- open `case_events` (follow-ups, deadlines, appointments)

The LLM receives this block as input for the dossier build so it can relate statements to the
parties, but consumers never see LLM-restated versions of these facts. Changes to parties or
deadlines do not trigger a rebuild.

### D8: Storage in dedicated tables, not as a case document
- `case_memory`:
  - identity: `case_id` (PK, FK → `cases.id` ON DELETE CASCADE)
  - `enabled`, `enabled_by`, `enabled_date`
  - dossier: `dossier` MEDIUMTEXT, `dossier_built`, `dossier_meta` (TEXT, JSON: card counts,
    fallback used, invalid citations, model)
  - notes: `notes` MEDIUMTEXT, `notes_changed_by`, `notes_changed`
  - state: `dirty_since`, `status`, `last_error`, `prompt_version`
- `case_memory_cards`: one row per **included** document (see D11)
  - identity: `document_id` (PK, FK → `case_documents.id` ON DELETE CASCADE), `case_id`
  - inclusion: `included_by`, `included_date`
  - content: `abstract` TEXT, `card` MEDIUMTEXT
  - change detection: `document_version`, `text_sha256`
  - state: `status` (OK/NO_TEXT/FAILED/PENDING), `truncated`, `attempts`, `last_error`
  - provenance: `model`, `prompt_version`, `created`

**Why not a document in the case:** a dossier stored as a document would trigger its own
reprocessing (an update loop), clutter the document list, show up in full-text search and
invite editing. The tables cascade cleanly with case and document deletion.

**Card follows its document:** when the client "moves" a document to another case, the new copy
gets a new id and is not included in the target case unless the user opts in within the move
dialog. The source document goes to the recycle bin, so its card stops contributing to the
source dossier and is removed with the document when the bin is purged.

### D9: Authorization
| Operation | Roles | Additionally |
|---|---|---|
| Read status, context package, cards (EJB, REST) | `readArchiveFileRole` | `checkGroupsForCase` |
| Edit notes | `writeArchiveFileRole` | group check |
| Enable, disable, include/exclude documents, request rebuild | `writeArchiveFileRole` + `aiAgentRole` | group check, global switch on (enable/include) |
| Server-wide configuration | `adminRole` | existing `SystemManagement.setSetting` |

- The timer uses facades and `*Unrestricted` reads, and only ever touches enabled cases.
- Enabling, disabling, every include/exclude action (one entry per action, naming the documents,
  capped at 20 names plus "und N weitere") and every notes change write a case history entry.
  Dossier rebuilds do not.

### D10: Prompt versioning
- Prompts live in `j-lawyer-server-ejb/src/main/resources/casememory/` (card system prompt,
  dossier system prompt), together with a `PROMPT_VERSION` constant.
- Cards and dossiers store the version they were built with.
- A version bump does **not** trigger automatic regeneration, because that would be a
  server-wide cost spike. Stale cards are flagged in the status, and users or admins regenerate
  on demand.

### D11: Two-level opt-in: per case, then per document
**What:**
- Enabling a case memory includes **no** documents. Each document is included by an explicit
  user action. No switch, rule or server-side path includes documents automatically.
- **Storage:** "included" ⇔ a row in `case_memory_cards` exists. Including a document creates a
  `PENDING` row with `included_by`/`included_date`. Excluding it deletes the row, and with it the
  generated card. Disabling the case deletes all rows.
- **One click at assignment:** every client dialog that assigns documents to a case gets a
  checkbox "Zum Aktenwissen hinzufügen". It is unchecked by default, not remembered, and
  offered only when the target case has an enabled memory and the user holds `aiAgentRole` and
  `writeArchiveFileRole`.
- **After assignment:** the dialog first stores the documents through the existing
  `addDocument*` calls and then calls `CaseMemoryServiceRemote.includeDocuments(caseId,
  documentIds)`.
- **Afterwards:** multi-select actions in the case document list and a "nicht aufgenommen" list
  in the case memory tab cover documents that were filed without a dialog (mailbox scanner,
  Dropscan, AI tools, REST) or without ticking the box.

**Why:**
- The opt-in per document gives the firm control over exactly which content leaves for the AI
  service, for example to exclude privileged correspondence or third-party health data.
- An unchecked default that is not remembered keeps every inclusion an explicit act. The
  checkbox sits in the dialogs the user is already in, so it stays a single click.

**Rows instead of a column on `case_documents`:**
- `ArchiveFileDocumentsBean` is a core entity shared with the client over EJB remoting and used
  by many code paths. `add-document-encryption-at-rest` also adds columns to it.
- The row approach needs no change to the entity. It keeps inclusion and generated content in
  one place (excluding deletes both), and cascades with the document.
- The client loads the included ids of a case with one extra call
  (`getIncludedDocumentIds(caseId)`), and only when the case memory is enabled.

**Why the store and include calls are separate:**
- Extending all `addDocument*` signatures on `ArchiveFileServiceRemote` would ripple through
  every caller.
- The two calls are not atomic. If the include call fails, the document stays stored and the
  user is told, so they can include it from the document list. This matches the "nothing is
  sent without a decision" principle: a failure errs on the side of not including.

**A reusable checkbox:** a small component, `CaseMemoryOptInPanel`, usable from the NetBeans GUI
Builder palette as a custom component, encapsulates:
- availability for a given case id (status cached per case for a short time),
- the role checks,
- resetting to unchecked when the target case changes,
- the include call after storing.

Each assignment dialog only places the component and hands it the stored document ids.

**Coverage through shared dialogs.** The client has about 35 paths that store a document into a
case, but almost all of them go through a few shared dialogs. Placing the component in these
covers the vast majority:

| Shared dialog | Paths covered |
|---|---|
| `BulkSaveDialog` (dialog-wide checkbox; collects the ids from `addDocument` and from `assignObservedFile`, which already returns the id) | e-mail inbox (4 variants + sidebar), beA inbox, scans, Dropscan |
| `NewFilenameOptionPanel` via `FileUtils.getNewFileName(...)` (new overload returning name + opt-in; the checkbox is shown only when `selectedCase` has an enabled memory) | e-mail/beA attachments, eEB response, copy/move to another case, duplicate, convert to PDF, split/stamp/anonymise/reorder, PDF export merge, E-POST letter, fax/E-POST reports, e-invoice XML, AI results |
| `AddDocumentFromTemplateDialog` | templates, invoice/timesheet documents, calculations, form plugins, AI generation; the inclusion also applies to the PDF created by its "convert to PDF" option |
| `SendEmailFrame`, `SendBeaMessageFrame` (next to `chkSaveAsDocument`; passed through `SendAction` / `SendEncryptedAction` / `SendBeaMessageAction` / `SaveBeaMessageAction`) | sending with "als Dokument speichern", including the server-based e-mail path, where the naming prompt appears only on a name conflict |
| `AddNoteFrame` | notes as documents |

`UploadDocumentsAction` (file chooser, drag and drop, Outlook drops, drop onto a folder) has no
dialog. It gets a non-blocking notice after the upload: "N Dokumente hochgeladen – zum Aktenwissen
hinzufügen?". A modal question on every drag and drop would train users to click it away.

**Deliberately not covered** (documents stay not included; they can be included later from the
list or the tab):
- `SendEmailFrame` draft autosave,
- server-generated claim statement and dunning documents (`ClaimStatementDialog`,
  `DunningExportDialog`, `ClaimLedgerDunningPanel`),
- `FormInstancePanel` form data, and saving the case history as a document,
- `AddVoiceMemoDialog` (audio has no text, so it would only ever get a `NO_TEXT` card),
- all AI tools and all server-side paths (mailbox scanner, REST, `DocUtilityService`).

**Alternatives considered:**
- A per-case switch "include new documents automatically": rejected, because the opt-in is to
  be per document without exception.
- A "include all existing documents" option in the enable dialog: not needed, because
  multi-select in the document list covers it explicitly.
- Including by folder or tag: out of scope, see Non-Goals.

## Risks / Trade-offs

- **Incomplete memory because opt-ins were forgotten.**
  → The context package and the chat notice state how many documents are not included, so AI
  consumers and users see that the memory is partial. The case memory tab lists these documents
  with a bulk include action.

- **Confidentiality (§ 203 StGB, § 43e BRAO).** An included document goes to the configured AI
  service in the background, and again whenever its text changes, without a per-request
  decision.
  → Two-level opt-in (D11); a confirmation dialog naming service and model and showing the
  `local` flag; case history entries for enabling and for every include/exclude action; global
  switch off by default.
- **Concentrated sensitive data.** The dossier is the essence of the case, stored in plain text
  in the database, outside the scope of `add-document-encryption-at-rest`.
  → The same access checks as the case; documented as a residual risk alongside the Lucene index.
- **Hallucination and misattribution.**
  → Mandatory citations with server-side validation, an attribution rule in the prompt, hard
  facts rendered live, the "not incorporated" list, and the notes as the human correction
  channel.
- **Cost.** Including 500 documents at once triggers 500 card calls.
  → Opt-in, `maxCallsPerRun`, progress shown ("Aufbau läuft: 120/500"), and no automatic
  regeneration on prompt version bumps.
- **Staleness.** The dossier trails changes by the debounce interval plus processing time.
  → The context package always lists documents that are not yet incorporated, by id, so AI
  consumers can read them directly.
- **Poor text extraction** (scans without OCR).
  → `NO_TEXT` cards are visible; the user can run the existing OCR, which changes the
  fingerprint and triggers regeneration automatically.
- **Polling load.** One query per enabled case per minute; acceptable for opt-in volumes.
  → If needed later, add CDI-event wake-ups and lower the timer frequency.

## Migration Plan

- One Flyway migration creates both tables. No data migration, no backfill.
- Ships with the global switch off, so behaviour is unchanged until an admin configures and
  enables the feature.
- Rollback: disable the global switch. The tables can remain, or be dropped in a later
  migration.

## Open Questions

- Does `j-lawyer-ai` enforce an action's `usage-types`? The shipped `summarize` action is
  `interactive` only. If it is enforced, the admin must pick an action that allows `automated`
  use (for example `generate`), or dedicated `case-memory-card` / `case-memory-dossier` actions
  (`usage-types="automated"`, generous `max-tokens`) should be added to `j-lawyer-ai.xml`. The
  latter is listed as an optional task.
- Should the enable dialog **refuse** non-local models, or only warn? Current proposal: warn and
  show the `local` flag; the decision stays with the firm.
- Default debounce (10 min) and budgets (card 120k chars, dossier 200k chars) should be checked
  against the context windows of the models actually used.
