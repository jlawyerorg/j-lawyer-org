## Context

The claim ledger already exists and is used: `claimledgers`, `claimcomponents`, `interest_rules`,
`claimledger_entries`, `interest_base` (migrations `V3_4_0_7`, `V3_4_0_9`, `V3_4_0_19`), served by
`ClaimLedgerService`/`PaymentSplitCalculator` and exposed through `ArchiveFileServiceRemote`
(`getClaimLedgers`, `addClaimComponent`, `createPaymentSplit`, `calculateClaimLedgerTotals`, …),
with `ClaimLedgerDialog` as the desktop UI. `ClaimLedgerServiceRemote` is an empty marker
interface today.

Everything this change adds hangs off that ledger. The procedural objects (dunning case,
enforcement measures) are *not* new financial containers — they are workflow objects that read the
ledger and write bookings back into it through one service operation.

Related infrastructure that is reused rather than rebuilt:

- `com.jdimension.jlawyer.documents.PdfFormsAccess` (server, PDFBox) already fills AcroForm PDFs —
  this is what the ZVFV forms need.
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
- **Decision: legal parameters as data.**
  - Fee tables (RVG value table, Nr. 1100 KV GKG, GvKostG positions) live in configuration/lookup
    tables so a statutory change is an update, not a release. Where a firm already uses a Groovy
    calculation plugin for RVG, the same table is the source.
  - The dunning court assignment (§ 689 Abs. 2, 3 ZPO) is an administrable table, because the
    central courts and their assignments change.
  - Official forms are administrable templates (PDF + field mapping + validity range), because the
    ZVFV forms changed in 2022, 2024 and again with mandatory use from 1 October 2025. The
    published PDFs are not committed to the repository; a default set can be delivered as an
    importable package.
- **Decision: deadlines are ordinary case events.** Generated deadlines and follow-ups are
  `ArchiveFileReviewsBean` rows tagged with their origin (dunning case / measure and rule id) so
  they appear in the existing calendar, reminders and follow-up views, and can be recalculated when
  the underlying date changes. Alternative considered: a private deadline table — rejected, it
  would be invisible to the firm's normal work organisation.
- **Decision: the EDA record stays in `implement-xjustiz-dunning-export`.** The dunning case
  assembles and validates the application data and calls that exporter. This change adds only the
  *inbound* direction (court response messages), which that proposal explicitly does not cover.
- **Decision: German domain wording in the UI, English identifiers in code and JavaDoc**, matching
  the existing code base and the JavaDoc rule for `*Remote` interfaces.

## Data Model Sketch

New tables (Flyway, next free numbers after `V3_6_0_6`):

- `claimledger_parties` — ledger id, role (CREDITOR/DEBTOR), sequence, contact reference,
  representative, authorised representative, consumer flag.
- `claimledgers` additions — parent ledger id (sub-ledger), effective creditor count, allocation
  mode, surplus handling, consumer-loan flag, VAT handling, dunning stage state.
- `enforcement_titles` — ledger id, type, issuing body, file number, dates (issue, clause,
  service), debtors covered, subject designation, limitation date.
- `claimcomponents` additions — extended type enum, interest start mode ("fixed date" /
  "on service"), recurrence (monthly claims: start month, end month), origin reference.
- `dunning_cases` — ledger id, court, court file number, Kennziffer, status, status date, amounts
  snapshot, contested amount, export reference.
- `dunning_case_history` — status transitions with user, date and source.
- `dunning_court_table` — federal state / criterion → court, editable by administrators.
- `dunning_stages` (configuration) and `dunning_stage_events` (per ledger: stage, date, deadline,
  document, charge booking).
- `enforcement_measures` — ledger id, title id, type, debtors, addressee, dates, outcome, notes.
- `enforcement_third_party_debtors` — measure id, contact, attached claim type, § 840 declaration
  state and date.
- `enforcement_form_templates` — name, form key (ZVFV annex), validity from/to, PDF reference,
  mapping profile.
- `payment_plans` / `payment_plan_installments` — ledger id, mode, start, interval, amounts,
  paid/missed state, optional link to an `enforcement_measures` row (§ 802b agreement).
- Ledger entries gain an origin reference (dunning case or measure) so bookings can be traced and
  reversed.

Services: `ClaimLedgerServiceRemote` (currently empty) takes the ledger operations that
`ArchiveFileServiceRemote` accumulated plus the new ones; `DunningServiceRemote` and
`EnforcementServiceRemote` are new. REST: `DunningEndpointV7`, `EnforcementEndpointV7`, and ledger
endpoints; swagger is generated by the build.

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
- **Overlap with `implement-xjustiz-dunning-export`.** Mitigation: that change owns the export and
  the viewer; this one owns the workflow that feeds it. If the exporter lands later, the dunning
  case still works and stores its validated data set.

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
