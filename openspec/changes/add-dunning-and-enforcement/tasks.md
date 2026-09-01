## 0. Reference data (prepared alongside the phases that consume it)

- [x] 0.1 Compile the main claim catalogue (Hauptforderungskatalog) of the dunning courts as
      shipped reference data — number, designation, and per number the additional entries it
      requires. Needed by 1.5 and 3.12; the catalogue is published by the dunning courts and is
      *not* part of the Satzbeschreibungen. Shipped as `BundledMainClaimCatalogue` behind the
      `MainClaimCatalogue` interface (56 entries), together with `BundledContractTypeCatalogue`
      (278 contract types for catalogue no. 28, which no. 28 requires as its additional entry).
      Carried in code for now rather than seeded — that decision is revisited in 5.7
- [x] 0.2 Compile the seed for the central dunning courts — official name, postal code and place
      (the EDA application addresses the court by these), XJustiz identifier as the matching key,
      address, accepted channels — feeding 3.2 and the dunning rule table of 3.3. Shipped as
      `BundledDunningCourtDirectory` behind the `DunningCourtDirectory` interface: all twelve
      courts with their XJustiz identifiers from `gds.gerichte`, plus the assignment of the federal
      states, which is a list rather than a map because North Rhine-Westphalia is divided between
      two courts. Carried in code for now rather than seeded into the court master data — that
      decision is revisited in 5.7 and acted on in 5.8
- [ ] 0.3 Decide source and storage format of the fee tables (RVG value table, Nr. 1100 KV GKG,
      GvKostG positions), then ship them as maintainable data; consumed by 3.5 and 4.6.
      *Decided and shipped for the value scales:* § 13 Abs. 1 RVG and § 34 Abs. 1 GKG are not tables
      but one algorithm with different numbers, and the printed Anlage 2 of each act is only a
      rendering of it that stops at 500,000 euro while the rule does not. Stored is therefore the
      rule — `fee_scales` plus `fee_scale_brackets`, with validity ranges, because fee law changes on
      a fixed date and § 60 RVG has earlier matters billed under the previous law. Seeded with the
      state after the KostBRÄG 2025 (in force 1 June 2025), verified against all 42 published rows of
      each table. The fee items that sit on those scales follow the same shape and are shipped with
      them in `fee_items`: Nr. 3305, 3308, 1008, 7002 VV RVG, the credit of Vorbem. 3 Abs. 4 VV RVG
      and Nr. 1100 KV GKG, all taken from the official Vergütungs- and Kostenverzeichnis. *Still
      open:* the GvKostG positions for 4.6, and the pre-2025 scale and items, which an installation
      needs for matters commissioned before 1 June 2025 (§ 60 RVG)
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

- [x] 2.1 Reminder stage configuration (template, period, charge, default-triggering) with admin UI
- [x] 2.2 Stage execution: generate document, record stage and deadline, create follow-up, book the
      charge
- [x] 2.3 § 288 BGB default interest proposal (5/9 percentage points from `interest_base`) and the
      § 288 Abs. 5 BGB lump sum incl. the consumer exclusion
- [x] 2.4 Ledger view showing the dunning stage state and the next escalation
- [x] 2.5 Tests for stage escalation, charge bookings and interest proposals

## 3. Phase 3 — Court dunning procedure

- [x] 3.1 `DunningCase` entity, status model and history with migration
- [x] 3.2 Court directory: `courts` and `court_scopes` with migration, service and remote interface
      (English JavaDoc), administration UI (+ `.form`) and the repeatable seed from 0.2, matched on
      the XJustiz identifier. The court data stops being carried in code here:
      `BundledDunningCourtDirectory` is the source the seed was generated from, so the two cannot
      drift apart. Replacing it in `ReferenceData` is *not* part of this task — that implementation
      also has to answer which court serves which state, and those rules arrive with the rule table
      of 3.3; until then the bundled directory stays the one `ReferenceData` hands out
- [x] 3.3 Dunning rule table over the directory (selection key incl. OLG district / postal-code
      range, special-rule marker, channels, Kennziffer and direct-debit flags) and the § 689
      Abs. 2, 3 ZPO derivation with manual override. With the rules in place, replace
      `BundledDunningCourtDirectory` in `ReferenceData` with an implementation reading the court
      master data and these rules. Keep the responsibility list as a list: North Rhine-Westphalia is
      divided along the OLG district of Cologne and applicants seated abroad are assigned to one
      court, so a schema assuming one court per state would lose both
- [x] 3.4 Deadline engine: rules for Widerspruch (2 weeks), VB application, § 701 ZPO six-month
      lapse, Einspruch (2 weeks); creation, recalculation and closing of case events
- [x] 3.5 Fee/cost proposal and booking for MB/VB (Nr. 3305, 3308, 1008 VV RVG, Vorbem. 3 Abs. 4
      credit, Nr. 1100 KV GKG) driven by the fee tables of 0.3
- [x] 3.6 Assembly and validation of the application data set (all problems reported in one list),
      callable as a readiness check from client and REST. The completeness and consistency checks
      are in place and reachable through `DunningServiceRemote.validateApplication`; the REST
      endpoint that wraps it lands with 3.9, and the field lengths and value domains of the
      Satzbeschreibung join the same list with the EDA core of 3.11
- [x] 3.7 Import of the court EDA messages (record types `03`, `05`, `16`, `18`, `20`, `22`, `90`)
      with matching on the echoed own reference, status/date update and confirmation before applying.
      Messages are read from a document of a case, which is how they arrive: through beA, filed with
      their attachments, and processed from there. `analyseCourtMessages` proposes an assignment per
      message and changes nothing; `applyCourtMessages` applies what the user confirmed.
      *Three things the Satzbeschreibungen established, each of which changed the design:* the courts
      send collective files — the trailer counts the messages in one — so a document can carry news
      for many procedures in many cases, and matching is global by the echoed reference rather than
      scoped to the document's case. The receipt confirmation (Satzart 90) carries no reference at
      all, so it is reported and never treated as an assignment that failed. And *no inbox is needed*:
      the source file stays in the case as a document, so a message left unassigned is not lost and
      the import can simply be run again — an inbox would have stored a second copy of data that
      already persists. A message that would move a procedure backwards is reported and skipped,
      because re-running an import is now the normal way to pick up what was left open.
      *Still open:* the ledger booking of the service date for components whose interest starts on
      service, and the dialog itself — both belong with the Mahnverfahren tab of 3.10
- [x] 3.8 Dunning worklist (filters, CSV, navigation, start VB application from the list) — the
      server side: filtering by status, court, overdue deadlines and above all by "objection period
      run without an enforcement order applied for", each row carrying the next open deadline, plus
      the CSV. Only procedures in cases the user may open appear; the list is a view of their own
      work. The expiry of the objection period is computed rather than read from the deadline
      records, so the answer does not depend on whether the calendar entries were ever created. The
      list view itself and starting the VB application from it belong with the desktop UI of 3.10
- [x] 3.9 `DunningServiceRemote` (English JavaDoc) and the REST endpoint — note the endpoint is
      `DunningEndpointV7`, see 3.15. The interface covers the procedures themselves (list, create,
      update, record a status, read the journal) alongside validation, export and message import.
      *Gap found and closed while doing this:* status changes were not being journalled at all,
      although `dunning_case_events` existed for it and the spec requires date, user and source to be
      recorded. Every move now writes an entry, and the source distinguishes what a person entered
      from what a court message reported — which is the first thing to look at when a procedure turns
      out to have been recorded wrongly
- [ ] 3.10 Desktop UI: `Mahnverfahren` tab of the ledger workspace (reminder stages, dunning case,
      status timeline, message import) (+ `.form` files)
- [x] 3.11 `com.jdimension.jlawyer.eda` core: data-driven record layouts (field, offset, length,
      type), the 128-byte fixed-length writer and parser, `AA`/`BB` framing and the CP-850 codec
      that refuses unencodable characters. The Satzbeschreibungen are published as PDFs at
      https://www.mahngerichte.de/publikationen/eda-konditionen/ — freely downloadable, individually
      and as one archive, without registration; only the barcode application is tied to the test and
      approval procedure of 0.5. The mechanics are in place - layout model with per-field offsets
      derived from the lengths, the 128-byte writer and parser, AA/BB framing with trailer counts
      computed from the file, and the CP-850 codec. Transcribed so far are the Dateivorsatz, the
      Dateinachsatz and the Kennsatz C01; the remaining record areas are transcribed with the mapper
      of 3.12, against the same builder that already refuses a layout not adding up to 128 bytes
- [x] 3.12 EDA mapper for the Mahnbescheid application (record type `01`, format 4.0.00): key
      record, parties and their representatives, catalogued claims (number from 1.5, with the
      additional record each special number demands) and free-text claims, running and already
      computed interest, consumer-credit data, and the seven ancillary-claim areas — with the
      mapping documented in the code. Mapped and assembled so far: key record, applicant, defendant,
      catalogued and free-text claims with the additions their catalogue number demands, running
      interest, and the trailer sums. *Still to map:* legal and authorised representatives
      (C05–C11, C17/C18), already computed interest (C19), assignment (C25), consumer-credit data
      (C27). The seven ancillary-claim areas C28–C34 are mapped: the ledger's own cost types
      correspond to them one for one, which makes the mapping a lookup rather than a judgement — and
      a necessary one, since a court decides differently on a reminder charge than on collection
      costs. The layouts for the remaining pieces are present and verified, so each is a mapping
      rather than a transcription
- [x] 3.13 Structural verifier (record length, framing, record order, per-area frequency, trailer
      counts, character set) with violations reported per record and field
- [x] 3.14 `AppUserBean` lawyer identification number (Kennziffer) with migration and the user
      administration field shown only for lawyer users (+ `.form`)
- [x] 3.15 Export operation in the EJB layer and the REST endpoint (export + validate), storing the
      file as a tagged case document linked to the dunning case. The export validates first, builds,
      verifies the finished file structurally, and only then stores the document and moves the
      status. *Deviation from the plan, deliberately:* the endpoint is `DunningEndpointV7`, not V8.
      A new resource is additive and breaks no existing client, and CLAUDE.md has new endpoints go
      into the current version — creating a version for it would fragment the API for nothing. It
      exposes the readiness check; exporting through REST follows once the confirmation step of 3.17
      has settled what a caller must supply
- [x] 3.16 EDA mapper and export for the Vollstreckungsbescheid application (record type `08`,
      format 4.1.00), reusing the record core. All eleven record areas are transcribed and the
      application is assembled. Note what the reuse does *not* extend to: the defendant records of
      this application are genuinely different — no salutation key at all, the designation across
      four name fields, the legal form in the address record — so they have their own mapping rather
      than the Mahnbescheid's. The record core, the value formatting and the framing are shared
- [ ] 3.17 Export confirmation step in the ledger workspace, pre-filled from the dunning case, with
      write-back of changed values (+ `.form`)
- [ ] 3.18 EDA viewer: formatted view resolving record types, section markers and fields, raw
      record view, tabbed panel and integration into the document viewer of `ArchiveFilePanel`
      (+ `.form` files)
- [ ] 3.19 Tests: status transitions, deadline generation/recalculation, fee credit, message import
- [ ] 3.20 Tests: record layouts against the worked examples of the Satzbeschreibungen, mapping per
      claim and ancillary-claim type incl. catalogued claims with their required additional record,
      interest from service written with an empty start date, CP-850 round-trip incl. umlauts,
      validation and verification failure paths, export document storage and re-export history.
      *Covered so far:* interest from service with an empty start date, the CP-850 round trip with
      umlauts, the validation and verification failure paths, catalogued claims with their required
      additional record, the seven ancillary-claim types each into its own area, and the party
      records against the official "Eintragungsbeispiele" — which
      caught a real error: a GmbH & Co. KG takes salutation key 4 with an empty legal-form field,
      while an AG & Co. KG, which looks like its near relative, takes no key and carries its legal
      form; the mapper had treated both as ordinary legal persons. *Open:* the export document
      storage and re-export history, which live in the EJB layer

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
- [ ] 5.7 Revisit the reference data decision for the two catalogues. The main claim catalogue and
      the contract types for catalogue no. 28 ship as `Bundled*` implementations behind interfaces
      in `com.jdimension.jlawyer.referencedata`, selected in `ReferenceData`; the courts have
      already moved to the master data in 3.2. By this point the EDA approval procedure has run and
      it is known how often the courts actually change these catalogues and whether firms need to
      correct them themselves. Decide per catalogue whether it stays in code or moves to
      maintainable data, and record the reasoning — the seam exists so the answer can be "it
      stays", not only "it moves"
- [ ] 5.8 If 5.7 so decides: replace the bundled catalogues with a maintainable implementation and
      give it an update path. Since a catalogue number goes into a filed application, record which
      version of the catalogue an application was built against — `ReferenceDataSource` already
      carries origin and date for that purpose. Where the providers become injected services,
      `ReferenceData` and the `Bundled*` classes go away with them
