## 1. Phase 1 — Claim ledger foundation

- [ ] 1.1 Add ledger party model (creditors/debtors, sequence, representatives, effective creditor
      count, consumer flag) with migration and entity classes
- [ ] 1.2 Add sub-ledger reference, allocation mode, surplus handling and consumer-loan flag to
      `ClaimLedger`
- [ ] 1.3 Add `EnforcementTitle` entity with prerequisites (Titel/Klausel/Zustellung) and 30-year
      limitation computation plus follow-up creation
- [ ] 1.4 Extend `ClaimComponentType` (pre-court costs, assessed costs, interest arrears, recurring
      monthly claims) and add the interest start mode "on service"
- [ ] 1.5 Extend `PaymentSplitCalculator` with § 497 Abs. 3 BGB, target-directed, per-debtor and
      manual allocation; persist the mode and the deviation warning
- [ ] 1.6 Extend `ClaimLedgerTotals` with per-debtor totals and continuing-interest data
- [ ] 1.7 Implement the conversion of non-interest-bearing costs into assessed costs
- [ ] 1.8 Implement `bookProceduralCost` (ledger booking + optional `CaseAccountEntry` + origin
      reference + reversal)
- [ ] 1.9 Move ledger operations from `ArchiveFileServiceRemote` to `ClaimLedgerServiceRemote`
      (English JavaDoc) keeping the old methods delegating for compatibility
- [ ] 1.10 Claim statement document (PDF/editable, key date, storable in the case, CSV/JSON export)
- [ ] 1.11 Balance list over all accessible ledgers with filters, sums and CSV export
- [ ] 1.12 Desktop UI: restructure `ClaimLedgerDialog` (+ `.form`) into the tabbed ledger workspace
      and implement the tabs `Stammdaten`, `Titel` and `Buchungen` (parties, title, new component
      types, allocation modes)
- [ ] 1.13 Desktop UI: status badges on the `ClaimLedgerEntryPanel` cards in
      `Finanzen → Forderungskonto` (open total, next deadline) and the claim statement dialog
      (+ `.form`)
- [ ] 1.14 Unit tests for interest, allocation modes, per-debtor totals and statement/itemisation
      consistency

## 2. Phase 2 — Pre-court dunning

- [ ] 2.1 Reminder stage configuration (template, period, charge, default-triggering) with admin UI
- [ ] 2.2 Stage execution: generate document, record stage and deadline, create follow-up, book the
      charge
- [ ] 2.3 § 288 BGB default interest proposal (5/9 percentage points from `interest_base`) and the
      § 288 Abs. 5 BGB lump sum incl. the consumer exclusion
- [ ] 2.4 Ledger view showing the dunning stage state and the next escalation
- [ ] 2.5 Tests for stage escalation, charge bookings and interest proposals

## 3. Phase 3 — Court dunning procedure

- [ ] 3.1 `DunningCase` entity, status model and history with migration
- [ ] 3.2 Administrable dunning court table incl. default data and § 689 Abs. 2, 3 ZPO derivation
- [ ] 3.3 Deadline engine: rules for Widerspruch (2 weeks), VB application, § 701 ZPO six-month
      lapse, Einspruch (2 weeks); creation, recalculation and closing of case events
- [ ] 3.4 Fee/cost proposal and booking for MB/VB (Nr. 3305, 3308, 1008 VV RVG, Vorbem. 3 Abs. 4
      credit, Nr. 1100 KV GKG) with data-driven fee tables
- [ ] 3.5 Assembly and validation of the application data set; hand-off to the XJustiz/EDA exporter;
      link the produced file to the dunning case
- [ ] 3.6 Import of court response messages with matching, status/date update, ledger service
      booking and an inbox for unmatched messages
- [ ] 3.7 Dunning worklist (filters, CSV, navigation, start VB application from the list)
- [ ] 3.8 `DunningServiceRemote` (English JavaDoc) and `DunningEndpointV7`
- [ ] 3.9 Desktop UI: `Mahnverfahren` tab of the ledger workspace (reminder stages, dunning case,
      status timeline, message import) (+ `.form` files)
- [ ] 3.10 Tests: status transitions, deadline generation/recalculation, fee credit, message import

## 4. Phase 4 — Enforcement

- [ ] 4.1 `EnforcementMeasure` entity, catalog of measure types and their configuration
- [ ] 4.2 Form template management (PDF + field mapping + validity) with admin UI and import of a
      default package
- [ ] 4.3 Form generation through `PdfFormsAccess` incl. mandatory-field validation, storage in the
      case, recording of the form version
- [ ] 4.4 Claim itemisation for ZVFV Anlagen 6–8 from the ledger, sharing the statement calculation
- [ ] 4.5 Third-party debtors incl. § 840 ZPO declaration deadline and payment booking
- [ ] 4.6 Enforcement cost proposal and booking (§ 788 ZPO, Nr. 3309/3310 VV RVG, GvKostG, court
      fees), joint or single debtor, advanced-by-firm handling
- [ ] 4.7 Measure follow-ups incl. outcome-driven closing and § 802d ZPO re-attempt scheduling
- [ ] 4.8 `EnforcementServiceRemote` (English JavaDoc) and `EnforcementEndpointV7`
- [ ] 4.9 Desktop UI: `Zwangsvollstreckung` tab (measures, third-party debtors, form generation) and
      `Fristen & Dokumente` tab of the ledger workspace (+ `.form` files)
- [ ] 4.10 Tests: form field mapping per annex, itemisation vs. statement equality, cost bookings,
      follow-up lifecycle

## 5. Phase 5 — Plans, portfolio, polish

- [ ] 5.1 Installment plan computation (from amount / from count) incl. continuing interest, plan
      document, due-date follow-ups and missed-installment detection
- [ ] 5.2 Payment agreement (§ 802b ZPO) with stay of measures and breach handling
- [ ] 5.3 Cross-case frame next to `editors/finance/ManagePaymentsFrame` (+ `.form`) holding the
      balance list, the dunning worklist and the enforcement/title portfolio (limitation, last
      measure, open amount), plus a desktop widget for overdue recovery deadlines
- [ ] 5.4 Ledger REST endpoints (totals, payment booking, statement)
- [ ] 5.5 Documentation: user-facing description of the workflow, admin guide for court table, fee
      tables, reminder stages and form templates
- [ ] 5.6 End-to-end test: claim → reminder → Mahnbescheid → Vollstreckungsbescheid → bailiff order
      → PfÜB → payments → statement, verifying bookings, deadlines and documents
