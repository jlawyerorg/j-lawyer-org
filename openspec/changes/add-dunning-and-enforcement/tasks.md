## 0. Reference data (prepared alongside the phases that consume it)

- [ ] 0.1 Compile the main claim catalogue (Hauptforderungskatalog) of the dunning courts as
      shipped reference data — number, designation, and per number the additional entries it
      requires — with a repeatable, non-duplicating seed. Needed by 1.5 and 3.12; the catalogue is
      published by the dunning courts and is *not* part of the Satzbeschreibungen
- [ ] 0.2 Compile the seed for the central dunning courts — official name, postal code and place
      (the EDA application addresses the court by these), XJustiz identifier as the matching key,
      address, accepted channels — feeding 3.2 and the dunning rule table of 3.3
- [ ] 0.3 Decide source and storage format of the fee tables (RVG value table, Nr. 1100 KV GKG,
      GvKostG positions), then ship them as maintainable data; consumed by 3.5 and 4.6
- [ ] 0.4 Obtain at least one official ZVFV form PDF per annex in scope and record its AcroForm
      field names, so the mapping profiles of 4.2/4.3 and the tests of 4.10 can be written against
      the real forms rather than assumptions
- [ ] 0.5 Apply for the Kennziffer at the dunning court and start the EDA test and approval
      procedure with the OLG Stuttgart – IuK-Fachzentrum Justiz. Organisational, but on the
      critical path: phase 3 cannot be verified end to end without it, and acceptance of a
      structurally valid file is not guaranteed until the test exchange has run

## 1. Phase 1 — Claim ledger foundation

- [x] 1.1 Add ledger party model (creditors/debtors, sequence, representatives, effective creditor
      count, consumer flag) with migration and entity classes: `AddressBean` reference as the
      party's identity with `ON DELETE SET NULL` like `invoices.contact_id`, an optional
      `ArchiveFileAddressesBean` reference for the case role it was derived from, and the
      designation/address snapshot written on first use towards a court
- [x] 1.2 Add sub-ledger reference, allocation mode, surplus handling and consumer-loan flag to
      `ClaimLedger`
- [x] 1.3 Add `EnforcementTitle` entity with prerequisites (Titel/Klausel/Zustellung) and 30-year
      limitation computation plus follow-up creation
- [x] 1.4 Extend `ClaimComponentType` (the EDA pre-court cost categories Auslagen, Mahnkosten,
      Auskunftskosten, Bankrücklastkosten, Inkassokosten, Nr. 2300 VV RVG, andere Nebenforderungen;
      assessed costs; interest arrears; recurring monthly claims) and add the interest start mode
      "on service"
- [x] 1.5 Add the main claim classification to `ClaimComponent`: catalogue number or free-text
      marker, plus the additional entries certain numbers require (property postal code/place for
      17/19/20/90, contract designation for 28, and the account, meter or service details of the
      remaining special numbers), with migration
- [x] 1.6 Consolidate the two interest implementations: make `PaymentSplitCalculator` use the
      period-splitting engine of `ArchiveFileService` (removing its hard-coded `3.62` base rate),
      and fix `ClaimLedgerEntryFacade.findByComponentAndType`, which runs the wrong named query
      with an undeclared parameter
- [x] 1.7 Extend `PaymentSplitCalculator` with § 497 Abs. 3 BGB, target-directed, per-debtor and
      manual allocation; persist the mode and the deviation warning
- [x] 1.8 Extend `ClaimLedgerTotals` with per-debtor totals and continuing-interest data
- [x] 1.9 Implement the conversion of non-interest-bearing costs into assessed costs
- [x] 1.10 Implement `bookProceduralCost` (ledger booking + optional `CaseAccountEntry` + origin
      reference + reversal)
- [x] 1.11 Move ledger operations from `ArchiveFileServiceRemote` to `ClaimLedgerServiceRemote`
      (English JavaDoc) keeping the old methods delegating for compatibility
- [x] 1.12 Claim statement document (PDF/editable, key date, storable in the case, CSV/JSON export)
- [x] 1.13 Balance list over all accessible ledgers with filters, sums and CSV export
- [x] 1.14 Desktop UI: restructure `ClaimLedgerDialog` (+ `.form`) into the tabbed ledger workspace
      and implement the tabs `Stammdaten`, `Titel` and `Buchungen` (parties, title, new component
      types, allocation modes, catalogue classification of main claims incl. the additional entries
      it demands)
- [x] 1.15 Desktop UI: status badges on the `ClaimLedgerEntryPanel` cards in
      `Finanzen → Forderungskonto` (open total, next deadline) and the claim statement dialog
      (+ `.form`)
- [x] 1.16 Unit tests for interest (incl. base-rate changes inside a period), allocation modes,
      per-debtor totals and statement/itemisation consistency, asserting that the payment split and
      the ledger totals report the same interest

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
- [ ] 3.2 Court directory: `courts` and `court_scopes` with migration, service and remote interface
      (English JavaDoc), administration UI (+ `.form`) and the repeatable seed from 0.2, matched on
      the XJustiz identifier
- [ ] 3.3 Dunning rule table over the directory (selection key incl. OLG district / postal-code
      range, special-rule marker, channels, Kennziffer and direct-debit flags) and the § 689
      Abs. 2, 3 ZPO derivation with manual override
- [ ] 3.4 Deadline engine: rules for Widerspruch (2 weeks), VB application, § 701 ZPO six-month
      lapse, Einspruch (2 weeks); creation, recalculation and closing of case events
- [ ] 3.5 Fee/cost proposal and booking for MB/VB (Nr. 3305, 3308, 1008 VV RVG, Vorbem. 3 Abs. 4
      credit, Nr. 1100 KV GKG) driven by the fee tables of 0.3
- [ ] 3.6 Assembly and validation of the application data set (all problems reported in one list),
      callable as a readiness check from client and REST
- [ ] 3.7 Import of the court EDA messages (record types `03`, `05`, `16`, `18`, `20`, `22`, `90`)
      with matching on the echoed own reference, status/date update, ledger service booking and an
      inbox for unmatched messages
- [ ] 3.8 Dunning worklist (filters, CSV, navigation, start VB application from the list)
- [ ] 3.9 `DunningServiceRemote` (English JavaDoc) and `DunningEndpointV8`
- [ ] 3.10 Desktop UI: `Mahnverfahren` tab of the ledger workspace (reminder stages, dunning case,
      status timeline, message import) (+ `.form` files)
- [ ] 3.11 `com.jdimension.jlawyer.eda` core: data-driven record layouts (field, offset, length,
      type), the 128-byte fixed-length writer and parser, `AA`/`BB` framing and the CP-850 codec
      that refuses unencodable characters
- [ ] 3.12 EDA mapper for the Mahnbescheid application (record type `01`, format 4.0.00): key
      record, parties and their representatives, catalogued claims (number from 1.5, with the
      additional record each special number demands) and free-text claims, running and already
      computed interest, consumer-credit data, and the seven ancillary-claim areas — with the
      mapping documented in the code
- [ ] 3.13 Structural verifier (record length, framing, record order, per-area frequency, trailer
      counts, character set) with violations reported per record and field
- [ ] 3.14 `AppUserBean` lawyer identification number (Kennziffer) with migration and the user
      administration field shown only for lawyer users (+ `.form`)
- [ ] 3.15 Export operation in the EJB layer and the REST endpoint (export + validate), storing the
      file as a tagged case document linked to the dunning case
- [ ] 3.16 EDA mapper and export for the Vollstreckungsbescheid application (record type `08`,
      format 4.1.00), reusing the record core
- [ ] 3.17 Export confirmation step in the ledger workspace, pre-filled from the dunning case, with
      write-back of changed values (+ `.form`)
- [ ] 3.18 EDA viewer: formatted view resolving record types, section markers and fields, raw
      record view, tabbed panel and integration into the document viewer of `ArchiveFilePanel`
      (+ `.form` files)
- [ ] 3.19 Tests: status transitions, deadline generation/recalculation, fee credit, message import
- [ ] 3.20 Tests: record layouts against the worked examples of the Satzbeschreibungen, mapping per
      claim and ancillary-claim type incl. catalogued claims with their required additional record,
      interest from service written with an empty start date, CP-850 round-trip incl. umlauts,
      validation and verification failure paths, export document storage and re-export history

## 4. Phase 4 — Enforcement

- [ ] 4.1 `EnforcementMeasure` entity, catalog of measure types and their configuration
- [ ] 4.2 Form template management (PDF + field mapping + validity) with admin UI, listing of the
      AcroForm field names of an uploaded PDF, and import of a default package built from 0.4
- [ ] 4.3 New name-based AcroForm filler beside `PdfFormsAccess` (fields addressed by name,
      on-state values for check boxes and radio groups), form generation incl. mandatory-field
      validation, storage in the case, recording of the form version
- [ ] 4.4 Claim itemisation for ZVFV Anlagen 6–8 from the ledger, sharing the statement calculation
- [ ] 4.5 Third-party debtors incl. § 840 ZPO declaration deadline and payment booking
- [ ] 4.6 Enforcement cost proposal and booking (§ 788 ZPO, Nr. 3309/3310 VV RVG, GvKostG, court
      fees), joint or single debtor, advanced-by-firm handling
- [ ] 4.7 Measure follow-ups incl. outcome-driven closing and § 802d ZPO re-attempt scheduling
- [ ] 4.8 `EnforcementServiceRemote` (English JavaDoc) and `EnforcementEndpointV8`
- [ ] 4.9 Desktop UI: `Zwangsvollstreckung` tab (measures, third-party debtors, form generation) and
      `Fristen & Dokumente` tab of the ledger workspace (+ `.form` files)
- [ ] 4.10 Tests: form field mapping per annex incl. check-box on-states, itemisation vs. statement
      equality, cost bookings, follow-up lifecycle

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
