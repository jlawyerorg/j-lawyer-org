# Change: Add court dunning (Mahnverfahren) and enforcement (Zwangsvollstreckung) support

## Why

j-lawyer already carries the financial core of a debt-collection matter: the claim ledger
(`ClaimLedger` / `ClaimComponent` / `ClaimLedgerEntry`, "Forderungskonto") stores main claims
and costs, interest rules (fixed or base-rate related, fed by the `interest_base` table) and
payments with a §§ 366, 367 BGB payment split (`PaymentSplitCalculator`). What is missing is
everything that happens *around* that ledger in a German law firm:

- the pre-court reminder cycle (außergerichtliches Mahnwesen) with default interest, reminder
  charges and escalation,
- the automated court dunning procedure (gerichtliches Mahnverfahren): application for a
  Mahnbescheid, the two-week Widerspruch deadline, the Vollstreckungsbescheid application, the
  six-month cut-off of § 701 ZPO, the fees of Nr. 3305/3308 VV RVG and Nr. 1100 KV GKG,
- and enforcement (Zwangsvollstreckung) out of the resulting title: the bailiff order, the
  attachment and transfer order (PfÜB), asset disclosure (Vermögensauskunft), third-party
  debtors, the § 788 ZPO cost cascade and the deadline/follow-up monitoring that keeps a
  portfolio of titles alive for up to 30 years (§ 197 Abs. 1 Nr. 3 BGB).

Every established German practice-management suite treats this as one connected module built on
top of the claim ledger — RA-MICRO ("Forderungskonto" plus the Zwangsvollstreckung module),
Advolux/Haufe ("Forderungseinzug und Zwangsvollstreckung"), Kleos, LawFirm. Firms doing recovery
work cannot use j-lawyer for it today; they keep a second system. Since 1 October 2025 the
revised official ZVFV forms are mandatory and enforcement communication is moving to fully
electronic transmission, so hand-typed forms are no longer a viable workaround.

The pending change `implement-xjustiz-dunning-export` already covers one building block — the
XJustiz/EDA data record for the dunning application. This change supplies the workflow, the
procedural state, the deadlines, the costs and the enforcement side around it, and feeds that
exporter instead of duplicating it.

## What Changes

**Claim ledger (extends the existing Forderungskonto)**
- Ledger master data: creditors and debtors taken from the case parties, number of creditors
  (relevant for the Nr. 1008 VV RVG increase), joint-and-several vs. single-debtor bookings
  (§ 788 Abs. 1 S. 2 ZPO), debtor's representative.
- Title data (Titelverwaltung): title type, issuing court/notary, file number, date, service
  date, enforceable copy/clause flags, plus 30-year limitation monitoring.
- Sub-ledgers (Unterkonten) for separately titled or separately calculated claims.
- Additional component types: pre-court costs of the creditor (as "andere Nebenforderungen" of
  the dunning application), assessed costs (festgesetzte Kosten, interest-bearing per § 104
  Abs. 1 S. 2 ZPO), fixed interest arrears, and recurring monthly main claims (maintenance).
- Interest starting on service of the dunning order ("Zinsen ab Zustellung"), resolved when the
  service date arrives.
- Additional payment allocation modes: § 497 Abs. 3 BGB (consumer loans), allocation to a
  single debtor, manual allocation across several main claims.
- Claim statement document (Forderungsaufstellung) to a key date, firm-wide balance list
  (Saldenliste), installment plan (Tilgungsplan), and a service API that books procedural fees
  and disbursements into the ledger (and optionally the case account).

**Dunning (new capability `dunning-procedure`)**
- Pre-court reminder stages per ledger with templates, default interest per § 288 BGB from the
  stored base rate, reminder charges and the § 288 Abs. 5 BGB lump sum, deadline follow-ups.
- A court dunning case record with an explicit status model (beantragt → erlassen → zugestellt →
  Widerspruch / Vollstreckungsbescheid → Einspruch → rechtskräftig / Abgabe an das Streitgericht),
  court file number and the applicant's Kennziffer.
- Determination of the competent central dunning court from a maintainable table (§ 689 Abs. 2,
  3 ZPO) with manual override.
- Automatic statutory deadlines and follow-ups: two-week Widerspruch period, VB application
  window, the six-month lapse of § 701 ZPO, two-week Einspruch period.
- Fee/cost booking for the applications: Nr. 3305 and Nr. 3308 VV RVG including the Nr. 1008
  increase and the Vorbem. 3 Abs. 4 VV RVG credit, Nr. 1100 KV GKG court fee.
- Hand-off of the assembled application data to the XJustiz/EDA exporter, and **import of the
  court's response messages** (service, objection, costs, referral) which updates the status,
  the dates and the ledger (MB-Zustellung booking).
- A dunning worklist across all cases.

**Enforcement (new capability `enforcement-proceedings`)**
- Enforcement measures (Vollstreckungsmaßnahmen) attached to a ledger/title, from a catalog:
  enforcement warning, bailiff order (Sachpfändung, gütliche Erledigung § 802b, Vermögensauskunft
  § 802c, arrest), judicial search order, PfÜB for monetary and for maintenance claims, wage and
  bank attachment, compulsory mortgage, referral to forced sale/administration, register and
  residents' register inquiries.
- Generation of the **official ZVFV forms** (Anlagen 1–8) by filling the published AcroForm PDFs
  through the existing server-side `PdfFormsAccess`, with admin-maintainable form templates,
  field mapping profiles, form-version validity and mandatory-field validation.
- Claim itemisation (Forderungsaufstellung, Anlagen 6–8) generated from the ledger, including
  the "further interest from …" continuation.
- Third-party debtors (Drittschuldner) with the § 840 ZPO declaration and its two-week deadline,
  and booking of amounts they pay.
- Enforcement cost handling per § 788 ZPO: bailiff costs (GvKostG), court fees and Nr. 3309/3310
  VV RVG fees booked into the ledger, joint or single debtor, tracked as advanced vs. recoverable.
- Per-measure deadlines/follow-ups, outcome recording (fruchtlos, Teilerfolg, erledigt),
  re-attempt scheduling (e.g. renewed Vermögensauskunft under § 802d ZPO), payment agreement with
  suspension of enforcement.
- Portfolio views/reports over all measures and titles, plus REST endpoints (v7) for both new
  capabilities so the web client and integrations can use them.

## Impact

- **Affected specs**: `claim-ledger` (new), `dunning-procedure` (new), `enforcement-proceedings`
  (new).
- **Depends on**: pending change `implement-xjustiz-dunning-export` (owns the XJustiz "Mahn"
  EDA record and its viewer). This change consumes it; if it is not implemented first, the EDA
  hand-off requirement here degrades to "export data assembled and stored", and the export itself
  stays with that change.
- **Affected code**
  - `j-lawyer-server-entities`: new entities next to `ClaimLedger`/`ClaimComponent`
    (`ClaimLedgerParty`, `EnforcementTitle`, `DunningCase`, `EnforcementMeasure`,
    `EnforcementFormTemplate`, `PaymentPlan`, …) plus Flyway migrations under
    `src/main/resources/db/migration/` (next free numbers after `V3_6_0_6`).
  - `j-lawyer-server/j-lawyer-server-ejb`: extend `ClaimLedgerService`/`ArchiveFileService`, new
    `DunningService` and `EnforcementService`, fee/interest calculation, reuse of
    `com.jdimension.jlawyer.documents.PdfFormsAccess` for AcroForm filling and of
    `ArchiveFileReviewsBean` for deadlines.
  - `j-lawyer-server-api`: new remote interfaces (JavaDoc in English, per project convention).
  - `j-lawyer-io`: new `DunningEndpointV7` / `EnforcementEndpointV7` (swagger is generated).
  - `j-lawyer-client`: extend `ClaimLedgerDialog` (+ `.form`), new dialogs/panels for title,
    dunning case, measures and forms; new worklist panel.
- **Not affected / explicit non-goals**
  - Direct transmission to courts and bailiffs: generated EDA files and PDFs are handed to the
    existing beA (beAstie), E-Post and print paths — no new transport is built here.
  - The Barcode application of `online-mahnantrag.de`, the European Order for Payment
    (EuMahnVO), enforcement abroad, insolvency proceedings, and full Zwangsversteigerungs-/
    Zwangsverwaltungs-workflows (only referral/measure recording).
  - Non-lawyer collection (Inkasso) fee schedules (RDG-licensed collectors).
  - Bulk/mass processing for high-volume creditors (claim import, batch applications, batch
    measures) — deferred to a later change; the REST API keeps that path open.
  - The § 850c ZPO attachment table (computation of attachable income) — not shipped.
  - Web-client UI: the Swing client and the REST API are in scope, the Angular UI of
    `add-web-client` follows in a later change.

## User Interface

No new case tab: the ledger list stays in `Finanzen → Forderungskonto` and gains status badges,
`ClaimLedgerDialog` becomes the tabbed ledger workspace (Stammdaten · Titel · Buchungen ·
Mahnverfahren · Zwangsvollstreckung · Fristen & Dokumente), and cross-case work (balance list,
dunning worklist, enforcement/title portfolio) gets its own frame next to the existing
`ManagePaymentsFrame`, plus a desktop widget for overdue recovery deadlines. See `design.md`.

## Rollout

Phased so that each phase is independently useful:

1. Ledger master data, title data, extended component types, statement document.
2. Pre-court dunning stages.
3. Court dunning case, deadlines, fees, EDA hand-off and response import.
4. Enforcement measures, ZVFV form generation, § 788 ZPO costs, deadlines.
5. Payment plans, portfolio views/reports, REST API.

## Sources consulted

- RA-MICRO, *Handbuch Forderungskonto* (Z1–Z10: master data, booking types, payment allocation,
  Tilgungsplan, Saldenliste, Kostenaufstellung, Vollstreckungsakte) and the RA-MICRO
  Zwangsvollstreckung module/wiki pages.
- Advolux/Haufe "Forderungseinzug und Zwangsvollstreckung", Kleos "Mahnungen und ZV",
  LawFirm enforcement module descriptions.
- mahngerichte.de: procedure overview, EDA (application types, channels, court response
  messages, Kennziffer).
- ZVFV 2022 (Anlagen 1–8) incl. the 2024 amendment mandatory since 1 October 2025;
  BMJ enforcement form downloads.
- ZPO (§§ 688 ff., 699, 701, 704 ff., 750, 788, 802b–802d, 829, 835, 840, 850c),
  BGB (§§ 247, 286, 288, 366, 367, 497, 197), RVG VV Nr. 1008, 2300, 3305, 3308, 3309, 3310,
  KV GKG Nr. 1100, GvKostG.
