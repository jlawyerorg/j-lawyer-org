## Context

The claim ledger already exists and is used: `claimledgers`, `claimcomponents`, `interest_rules`,
`claimledger_entries` (migrations `V3_4_0_7`, `V3_4_0_9`, `V3_4_0_19`) and `interest_base`
(migration `V3_4_0_12`), served by `ClaimLedgerService`/`PaymentSplitCalculator` and exposed
through `ArchiveFileServiceRemote`
(`getClaimLedgers`, `addClaimComponent`, `createPaymentSplit`, `calculateClaimLedgerTotals`, …),
with `ClaimLedgerDialog` as the desktop UI. `ClaimLedgerServiceRemote` is an empty marker
interface today.

Two facts about that existing code shape this change. First, interest is computed twice: the real
engine in `ArchiveFileService.calculateAccruedInterest` splits the period at base-rate and
principal changes and resolves the rate from `interest_base`, while `PaymentSplitCalculator` keeps
a second, simplified copy whose `getBaseInterestRate()` returns a hard-coded `3.62`. Second,
`ClaimLedgerEntryFacade.findByComponentAndType` runs the named query
`ClaimLedgerEntry.findByComponent` while setting a `type` parameter that query does not declare,
which fails at runtime; the correct
query exists and is simply unused. Both are cleaned up in phase 1, before statements and
itemisations are built on top.

Everything this change adds hangs off that ledger. The procedural objects (dunning case,
enforcement measures) are *not* new financial containers — they are workflow objects that read the
ledger and write bookings back into it through one service operation.

Related infrastructure that is reused rather than rebuilt:

- PDFBox is already on the server and `com.jdimension.jlawyer.documents.PdfFormsAccess` already
  opens AcroForm PDFs — but it substitutes placeholders *inside field values* and has no
  name-based fill, so the ZVFV forms need a new filler beside it (see Decisions).
- `ArchiveFileReviewsBean` with `EVENTTYPE_FOLLOWUP` / `EVENTTYPE_RESPITE` is the existing deadline
  and follow-up mechanism, including the calendar/notification stack.
- `CaseAccountEntry` (Aktenkonto) already models outlays and receipts of the firm.
- Document templates, document naming rules and the dispatch channels (beA via beAstie, E-Post,
  print) already exist and are the exit path for every document produced here.
- Calculation plugins (Groovy, `org.jlawyer.plugins.calculation`) already compute RVG fees for
  invoices.

## Goals / Non-Goals

- Goals: cover the German recovery workflow end to end on top of the existing ledger; keep every
  legal parameter (fee tables, base rate, court table, official forms) as maintainable data;
  produce documents through the existing document and dispatch stack; make procedural dates create
  the deadlines that keep a title alive.
- Non-Goals: new transport protocols (no direct EGVP/court/bailiff transmission), the barcode
  application of online-mahnantrag.de, the European Order for Payment, foreign enforcement,
  insolvency, deep Zwangsversteigerung/-verwaltung workflows, Inkasso (RDG) fee schedules, and the
  Angular web UI (REST only).

## Decisions

- **Decision: extend the ledger, do not fork it.** Title data, ledger parties and the new component
  types become part of the ledger model, so totals, allocation and statements keep one
  implementation. Alternative considered: a separate "enforcement account" — rejected, it would
  duplicate interest and allocation logic that is already correct.
- **Decision: two new workflow entities, one shared cost path.** `DunningCase` and
  `EnforcementMeasure` both book money exclusively through one ledger service operation
  (`bookProceduralCost`), which also owns the optional `CaseAccountEntry` and the reversal
  semantics. That keeps § 788 ZPO costs, MB/VB fees and reminder charges consistent and reversible.
- **Decision: a general court directory, not a dunning-specific court table.** j-lawyer has no
  court entity today (cases carry no court field, courts appear as parties or free text, and
  `server_options` is a plain key/value list that cannot hold identifiers and addresses). This
  change alone needs court data in four roles — dunning court, enforcement court, the Amtsgericht
  for a search order, and the litigation court after referral under § 696 ZPO — so a dunning-only
  table would need a sibling immediately. `courts` plus `court_scopes` therefore hold the master
  data, and each feature adds only its own rules. Determining competence is deliberately kept out
  of the directory: for dunning it is a static assignment (`dunning_court_rules`), for enforcement
  it derives from the debtor's residence (§ 828 Abs. 2 ZPO), which needs a postal-code to district
  mapping — that mapping is not part of this change, the user picks the enforcement court and the
  directory supplies its address and identifiers. The directory is seeded with the central dunning
  courts only; a later import from a court register stays possible because the XJustiz identifier
  is the matching key. That identifier is a stable key, not the identifier every interface expects
  — the EDA dunning application addresses the court by postal code and place — and its code list
  (`gds.gerichte`) is of type 3, so its values live in the XRepository and not in the schema files
  shipped with the server; the seed is therefore maintained as project data.
- **Decision: legal parameters as data.**
  - Fee tables (RVG value table, Nr. 1100 KV GKG, GvKostG positions) live in configuration/lookup
    tables so a statutory change is an update, not a release. Where a firm already uses a Groovy
    calculation plugin for RVG, the same table is the source.
  - The dunning court assignment (§ 689 Abs. 2, 3 ZPO) is an administrable rule table over the
    court directory, because the central courts and their assignments change.
  - Official forms are administrable templates (PDF + field mapping + validity range), because the
    ZVFV forms changed in 2022, 2024 and again with mandatory use from 1 October 2025. The
    published PDFs are not committed to the repository; a default set can be delivered as an
    importable package.
- **Decision: a new, name-based AcroForm filler for the official forms.** `PdfFormsAccess` fills
  the firm's own templates by finding a placeholder *inside a field's value*; its fallback branch
  writes a value into any field that merely carries a `TU` entry, once per key, so the field keeps
  whatever key came last. The ZVFV PDFs have ordinary field names, empty values and are largely
  tick-box forms, so they need a filler that addresses fields by name and sets the declared
  on-state of check boxes and radio groups. That filler is new code beside `PdfFormsAccess`, which
  keeps serving the template path unchanged. Alternative considered: extending `PdfFormsAccess` in
  place — rejected, its placeholder semantics are relied on by existing templates.
- **Decision: deadlines are ordinary case events.** Generated deadlines and follow-ups are
  `ArchiveFileReviewsBean` rows tagged with their origin (dunning case / measure and rule id) so
  they appear in the existing calendar, reminders and follow-up views, and can be recalculated when
  the underlying date changes. Alternative considered: a private deadline table — rejected, it
  would be invisible to the firm's normal work organisation.
- **Decision: the dunning application is written in the EDA record format, not in XJustiz.** The
  schema set committed under `j-lawyer-server-common/xjustiz` cannot carry an application:
  `xjustiz_0600_mahn_3_3.xsd` declares exactly two messages, `aktenzeichenmitteilung.0600001`
  ("vom Prozessgericht an das Mahngericht") and `uebergabe.0600002` ("von einem Mahngericht an ein
  Prozessgericht"), both court-to-court, and its claim structure is a retrospective record of an
  already issued Mahnbescheid — no interest elements, `kosten` holding only a `kostenbefreiung`
  code rather than amounts, an `anspruchsnummer` the court assigns, and a required
  `antragseingangsdatum` only the court knows. A law firm neither sends nor receives either
  message. The applications and the court's replies travel in the EDA record format of the dunning
  courts instead, so this change targets that format and the XJustiz schema set plays no part in
  it. Alternative considered: emitting XJustiz anyway and hoping the courts accept it — rejected,
  the schema has nowhere to put the data.
- **Decision: the EDA exchange is part of this change.** The separate proposal
  `implement-xjustiz-dunning-export` was withdrawn and removed from `openspec/changes/` without
  being implemented; keeping the exporter separate would have split one workflow across two
  changes, since the exporter needs the dunning case (court, Kennziffer, parties, amounts at
  application time) and the inbound court messages need the same mapping. The codec lives in
  `j-lawyer-server-common` (`com.jdimension.jlawyer.eda`: record model, writer, parser, mapper,
  validator) so EJB, REST and future tools share it. It is hand-written: the format is a
  fixed-length record layout, so there is no schema to generate from and the JAXB build step the
  withdrawn proposal assumed falls away entirely. Validation is two-staged — application data
  first, then a structural verification of the produced file — and no unverified file is ever
  stored or returned.
- **Decision: exports are ordinary case documents.** Each export is stored through the normal
  document path (naming rules, tag, case document list) and linked to the dunning case; re-exports
  add a document instead of overwriting, so the procedural history stays reconstructable. The EDA
  viewer is a viewer panel in the existing document viewer, with a formatted view and the raw XML,
  degrading to raw content with a notice for files it cannot interpret.
- **Decision: the Kennziffer lives on the user, with per-court overrides.** The identification
  number belongs to the acting lawyer, so `AppUserBean` carries the nationwide one (shown in user
  administration only for users flagged as lawyers). Because the courts exchange Kennziffer data
  but not all of them accept a foreign one — Uelzen and Hamburg issue and require their own — a
  user may store additional court-specific numbers, and the export picks the court-specific number
  when the selected court is flagged as requiring it. The dunning case records the number actually
  used.
- **Decision: German domain wording in the UI, English identifiers in code and JavaDoc**, matching
  the existing code base and the JavaDoc rule for `*Remote` interfaces.

## Data Model Sketch

New tables (Flyway, next free numbers after the highest existing migration, currently `V3_6_0_8`):

- `claimledger_parties` — ledger id, role (CREDITOR/DEBTOR), sequence, contact reference,
  representative, authorised representative, consumer flag.
- `claimledgers` additions — parent ledger id (sub-ledger), effective creditor count, allocation
  mode, surplus handling, consumer-loan flag, VAT handling, dunning stage state.
- `enforcement_titles` — ledger id, type, issuing body, file number, dates (issue, clause,
  service), debtors covered, subject designation, limitation date.
- `claimcomponents` additions — extended type enum, interest start mode ("fixed date" /
  "on service"), recurrence (monthly claims: start month, end month), origin reference.
- `dunning_cases` — ledger id, court, own reference (Teilnehmergeschäftszeichen, the key the court
  echoes in every message), court file number (Gerichtsnummer), Kennziffer used, EDA format version
  used, status, status date, amounts snapshot, contested amount, export reference.
- `dunning_case_history` — status transitions with user, date and source.
- `courts` — central court master data: official name, additional designation, XJustiz identifier
  from the code list `urn:xoev-de:xjustiz:codeliste:gds.gerichte`, postal and Postfach address,
  phone/fax/web, electronic recipient (EGVP/beA SAFE-ID), optional bank details for court fees,
  validity range, active flag, note.
- `court_scopes` — n:m assignment of scopes (`DUNNING`, `ENFORCEMENT`, `LITIGATION`, `INSOLVENCY`,
  `LAND_REGISTRY`, `LABOUR`, …) driving which courts a selection field offers.
- `dunning_court_rules` — the dunning-specific layer on top: selection key (federal state and,
  where a state is split, OLG district or postal-code range), special-rule marker (applicant
  without a domestic general venue, § 703d ZPO, condominium claims), accepted channels, the flags
  `requires_own_kennziffer` (Uelzen, Hamburg) and `direct_debit_must_be_nationwide` (Hünfeld),
  validity range — each row referencing a court of `courts`.
- `dunning_stages` (configuration) and `dunning_stage_events` (per ledger: stage, date, deadline,
  document, charge booking).
- `enforcement_measures` — ledger id, title id, type, debtors, addressee, dates, outcome, notes.
- `enforcement_third_party_debtors` — measure id, contact, attached claim type, § 840 declaration
  state and date.
- `app_user` addition — lawyer identification number (Kennziffer) for users flagged as lawyers.
- `enforcement_form_templates` — name, form key (ZVFV annex), validity from/to, PDF reference,
  mapping profile.
- `payment_plans` / `payment_plan_installments` — ledger id, mode, start, interval, amounts,
  paid/missed state, optional link to an `enforcement_measures` row (§ 802b agreement).
- Ledger entries gain an origin reference (dunning case or measure) so bookings can be traced and
  reversed.

Exchange code: `com.jdimension.jlawyer.eda` in `j-lawyer-server-common` (record model, fixed-length
writer, parser used by the import and the viewer, mapper, validator, CP-850 codec), hand-written;
the client viewer panel sits next to the other document viewers.

Services: `ClaimLedgerServiceRemote` (currently empty) takes the ledger operations that
`ArchiveFileServiceRemote` accumulated plus the new ones; `DunningServiceRemote` and
`EnforcementServiceRemote` are new. REST: `DunningEndpointV8`, `EnforcementEndpointV8`, and ledger
endpoints in `org.jlawyer.io.rest.v8`, the current API version; swagger is generated by the build.

## Risks / Trade-offs

- **Legal correctness of money and deadlines.** Wrong interest, allocation or a missed two-week
  period is a liability event. Mitigation: every computed amount and date is a *proposal* the user
  can change, calculations are unit tested against worked examples from the literature, and the
  statement/itemisation share one calculation path so form and statement can never disagree.
- **Official forms change frequently.** Mitigation: forms and mappings are data with a validity
  range; using a form outside its validity warns the user; the version used is recorded.
- **Scope.** This is large. Mitigation: five phases, each shippable on its own; phase 1 alone
  already improves the existing ledger.
- **Multiple debtors.** Joint-and-several vs. single-debtor bookings complicate every total.
  Mitigation: model it in phase 1 (not retrofitted later), and keep per-debtor totals part of
  `ClaimLedgerTotals` from the start.
- **EDA format drift.** The courts publish a validity table per record type and retire old
  versions (only format 4 has been accepted since 1 November 2017); the Satzbeschreibungen are
  amended regularly — the Mahnbescheid application's most recent change is dated 24 June 2026.
  Mitigation: the record layouts are declared as data (field name, offset, length, type) rather
  than hard-coded offsets, the version written is recorded on the dunning case, and tests assert
  each layout against worked examples from the Satzbeschreibungen so an amendment surfaces as a
  failing test rather than as a rejected application.
- **Court acceptance.** A structurally valid file is not automatically an accepted application:
  the courts reject records that break field order, exceed the maximum data volumes or carry
  characters outside CP-850, and they answer with a monition (record type `20`) rather than a
  parser error. Mitigation: start from the minimum mandatory data set, keep the mapping documented
  next to the code, import and display monitions so a rejection is visible, and treat each
  rejection as a mapping bug with a test.

## Migration Plan

- Additive schema only; existing ledgers keep working. New columns are nullable or defaulted:
  existing ledgers get one creditor and one debtor derived from the case parties on first opening,
  allocation mode `LEGAL`, no title.
- Feature is usable without configuration except for the pieces that need firm data: dunning court
  table (shipped with a default), reminder stages (shipped with a default set), fee tables
  (shipped), official forms (imported by the firm).
- Rollback: the workflow objects can be removed without touching ledger data; bookings they created
  remain as ordinary ledger entries with a dangling origin reference, which is tolerated.

## Client Scope

Desktop client (Swing) and REST only. The Angular web client of `add-web-client` consumes the same
REST endpoints in a later change; no web UI work is part of this change.

## UI Structure (decided)

The recovery workflow gets more screens than a single modal dialog carries, but the case editor's
tab bar is already full (`Allgemeine Daten`, `Beteiligte`, `Dokumente`, `Finanzen`, `Falldaten`,
`Kalender`, `Historie`, …). Therefore:

1. **Case level stays where it is.** The ledger list keeps living in `Finanzen → Forderungskonto`
   (`pnlClaimLedgers` with `ClaimLedgerEntryPanel` cards). Each card is extended by the status the
   user needs at a glance: open total, dunning stage / dunning-case status, running enforcement
   measures, next deadline.
2. **The ledger detail becomes a workspace.** `ClaimLedgerDialog` — already the single detail view,
   opened from the card and from `ArchiveFilePanel` — is turned into a tabbed, resizable window:
   - `Stammdaten` (parties, creditor count, allocation mode, VAT, consumer flags)
   - `Titel` (titles with Klausel/Zustellung state and limitation date)
   - `Buchungen` (today's ledger table, components, payments, allocation)
   - `Mahnverfahren` (reminder stages and the court dunning case with its status timeline)
   - `Zwangsvollstreckung` (measures, third-party debtors, generated forms)
   - `Fristen & Dokumente` (deadlines and follow-ups created by the workflow, produced documents)
   No new case tab is introduced, and no existing entry point changes.
3. **Cross-case work happens outside the case**, following the existing pattern of
   `editors/finance/ManagePaymentsFrame`: one frame with the balance list, the dunning worklist and
   the enforcement/title portfolio, plus a desktop widget for overdue recovery deadlines that fits
   the configurable desktop grid.

Alternatives considered: a dedicated top-level case tab "Forderung & Vollstreckung" (rejected — the
tab bar is the scarcest UI resource in the case editor, and the content belongs to a ledger, not to
the case), and keeping a single flat dialog (rejected — six functional areas do not fit one form).

## Deferred (agreed scope cuts)

- Bulk/mass processing for high-volume creditors (claim import, batch applications, batch measures)
  is out of scope; the REST API keeps the door open for it.
- The § 850c ZPO attachment table is not shipped; attachable income remains the third-party
  debtor's calculation.
- Web UI: later change, see Client Scope.
