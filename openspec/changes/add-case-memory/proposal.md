# Change: Add a case memory (per-document cards and a case dossier) as ready-made AI context

## Why

Cases and their documents are often very large. An AI request over a whole case today means
sending the extracted text of every document to the model: a 200-document case easily exceeds
200k tokens. This is slow, expensive, frequently beyond the model's context window, and it
repeats the same reading work on every request. The only case-wide mechanisms today are
selecting documents in `ArchiveFilePanel` (which concatenates their full text without any size
limit, `ArchiveFilePanel.getInputs`) and the Ingo chat tools, which read documents one by one
(`get_document_text`, capped at 30000 characters each).

Following the "LLM wiki" idea, this change does the reading work **once, when a document
arrives**, and keeps the result as compact, pre-synthesised knowledge next to the case. It has
two layers:

1. **Document cards** (map step): one short card per document, created in the background when a
   document is added or its content changes.
2. **Case dossier** (reduce step): a structured synthesis of the whole case. It is always
   rebuilt from the cards, never edited incrementally, so errors cannot accumulate and removed
   documents disappear from it.

For a typical large case this reduces the AI context from the raw text (≈200k+ tokens) to the
cards (≈40–60k) and the dossier (≈5–10k), with the original text still reachable per document.
The dossier is also useful to people directly, for example as a case cover sheet, for handing
a case over to a colleague, or to prepare a client meeting.

## What Changes

### New capability `case-memory` (server)
- **Two-level opt-in: per case, then per document.** An administrator enables the feature
  server-wide and configures the Ingo connection. A user with `aiAgentRole` then enables the case
  memory for an individual case. Enabling includes **no** documents: every document must be
  included by an explicit user action, and no path includes documents automatically (mailbox
  scanner, Dropscan, AI tools, REST, copies/moves). The opt-in stays a single click:
  - every client dialog that assigns documents to a case offers an unchecked checkbox
    "Zum Aktenwissen hinzufügen", shown only when the target case has an enabled memory,
  - the case document list marks included documents and offers include/exclude for multiple
    selected documents,
  - the case memory tab lists documents that are not included.

  Enabling, disabling and every include/exclude action are recorded in the case history.
  Excluding a document deletes its card. Disabling the case memory deletes all inclusions and
  all generated content of that case.
- **Document cards.** For every included, non-deleted document of an enabled case, the server
  creates a card from the existing text preview (`archivefiles-preview/<caseId>/<docId>.txt`). A card
  consists of a short abstract and a detail section with a fixed structure: document type,
  author/sender, date, core statements with attribution, amounts, deadlines mentioned. The
  document id, name, date and folder are written into the card header by j-lawyer, not by the
  LLM. Documents without extractable text get a text-less card and cause no LLM call.
- **Case dossier.** Generated from the cards, the case master data and the lawyer's notes. It
  has fixed sections: current status, parties and their positions, chronology, points in
  dispute, evidence, open questions, contradictions. Every statement cites its source document
  as a `[D:<documentId>]` token. After generation the server validates these tokens and removes
  citations of documents that were not in the input. Claims made by a party must stay
  attributed to that party instead of being stated as fact.
- **Lawyer's notes.** Each case gets a manually maintained notes section. The LLM reads it as
  context but never writes to it.
- **Reconciliation-driven background processing.** A `@Singleton` EJB timer compares the
  included documents with their cards for enabled cases, using the document version and a SHA-256 fingerprint of the text
  preview. It then:
  - creates missing cards and regenerates changed ones,
  - drops cards of permanently deleted documents via FK cascade,
  - excludes documents in the recycle bin,
  - rebuilds the dossier once the case has been quiet for a debounce interval (default
    10 minutes).

  Because the database state itself acts as the work queue, the processing survives restarts
  and needs no new JMS queue. Moves between cases, restores and OCR runs are covered without
  separate event handling.
- **LLM access through the existing Ingo backend.** The job calls `AssistantAPI` directly with
  an admin-selected `AssistantConfig` and one action/model each for cards and for the dossier.
  It submits asynchronously and polls. j-lawyer owns the prompts and ships them as versioned
  classpath resources.
- **Freshness and hard facts.** Every read of the case memory returns a *context package*:
  - the time of the last build and how many included documents are incorporated, pending,
    failed or without text,
  - how many case documents are not included (a count only, no ids or content),
  - which included documents are not yet incorporated, by id,
  - **master data rendered live from the database**: file number, name, reason, subject field,
    claim value, responsible lawyer, parties with roles, open follow-ups/deadlines/appointments,
  - the lawyer's notes, the dossier, and the card abstracts.

  Hard facts are therefore never taken from LLM output, and changes to parties or deadlines are
  current without triggering a rebuild.
- **New EJB service `CaseMemoryService`** with Remote and Local interfaces (enable/disable,
  include/exclude documents, included document ids, status, context package, cards, notes,
  rebuild request).
- **Configuration** through server settings (`ServerSettingsKeys`): global switch, assistant
  config id, request type/action/model for cards and for the dossier, debounce interval, input
  size budgets, and the maximum number of LLM calls per timer run. The feature is off by
  default.
- **Database:** new tables `case_memory` and `case_memory_cards` (Flyway). They cascade from
  `cases` and `case_documents`.

### REST API v8
- `GET /v8/cases/{id}/memory` returns the context package.
- `GET /v8/cases/{id}/memory/cards` returns the document cards.
- Both are read-only and require `readArchiveFileRole` plus case group access. The swagger
  documentation is generated automatically.

### Desktop client
- New tab **"Aktengedächtnis"** in the case view, as a separate `CaseMemoryPanel` with its own
  `.form`, so `ArchiveFilePanel.form` only gains a tab. It contains:
  - a status line,
  - enable/disable with a confirmation dialog that names the AI service and model and says
    whether it is local,
  - the dossier rendered as Markdown, with citations shown as clickable document names,
  - the notes editor,
  - the card list,
  - the list of documents that are not included, with bulk include,
  - "Dossier neu aufbauen" and "Alle Karten neu erzeugen" buttons.
- **Case document list:** an inclusion mark on each document, and the context menu actions
  "Zum Aktenwissen hinzufügen" and "Aus dem Aktenwissen entfernen" for multiple selected
  documents.
- **Assignment dialogs:** a reusable opt-in component (`CaseMemoryOptInPanel`) in every dialog
  that assigns documents to a case, for example saving e-mails or beA messages, scans, uploads,
  documents from templates, and moving documents to another case. The full list is in
  `tasks.md`.
- New admin dialog **"Aktengedächtnis"** (with `.form`) for the server-wide settings.

### Ingo chat (`ai-assistant-integration`)
- New read-only tools `get_case_dossier` and `get_document_cards` in `ToolRegistry`
  (`RISK_LOW`).
- When a chat is started from a case whose memory is enabled, the chat context carries a notice
  telling the model to read the dossier before opening individual documents.
- AI tools never include documents. `save_email_to_case` stores documents without including
  them.

## Impact

- **Affected specs:** `case-memory` (new), `ai-assistant-integration` (ADDED requirements)
- **Affected code (new):**
  - `j-lawyer-server-entities`: `CaseMemoryBean`, `CaseMemoryCardBean`, Flyway migration
    (next free number at implementation time, currently `V3_6_0_25`; coordinate with
    `add-dunning-and-enforcement`)
  - `j-lawyer-server-api`: `CaseMemoryServiceRemote` (JavaDoc in English)
  - `j-lawyer-server-ejb`: `CaseMemoryService`/`CaseMemoryServiceLocal`, `CaseMemoryProcessor`
    (timer), facades, prompt resources, pure helper classes for parsing/validation
  - `j-lawyer-client`: `CaseMemoryPanel` (+ `.form`), `CaseMemorySetupDialog` (+ `.form`),
    `CaseMemoryOptInPanel` (+ `.form`)
- **Affected code (modified):**
  - `ServerSettingsKeys` (new keys)
  - `IntegrationService`: extract the assistant-config lookup and password decryption into a
    helper shared with the background job
  - `CasesEndpointV8` (two GET methods) and new `RestfulCaseMemoryV8`/`RestfulCaseMemoryCardV8`
    POJOs
  - `ArchiveFilePanel` (+ `.form`): add the tab, inclusion marks and context menu actions in the
    document list
  - all client dialogs that assign documents to a case (+ their `.form` files): place the
    opt-in component
  - `ToolRegistry`
  - the chat start from a case (chat context notice)
  - client configuration menu (admin dialog entry)
- **No changes to `j-lawyer-ai` are required.** The job uses existing request types with
  j-lawyer-supplied prompts. Dedicated "automated" actions in `j-lawyer-ai.xml` are optional
  (see `design.md`, Open Questions).
- **Operational:** background LLM calls consume the token balance of the configured Ingo
  account. Generated cards and dossiers are stored in the database. Like the Lucene index, they
  are not covered by `add-document-encryption-at-rest`.
- **No breaking changes.** The feature is additive and ships disabled.
