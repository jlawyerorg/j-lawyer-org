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
- [x] 0.4 The official ZVFV forms are in, and their field names are recorded. All eight annexes of
      the ZVFV 2022, version of 01.09.2024, obtained from the BMJ — they were not findable under
      justiz.de — and kept under `j-lawyer-server/j-lawyer-server-ejb/src/test/resources/zvfv/`
      together with three Hinweisblätter, which carry no fields. As annexes of a Rechtsverordnung
      they are amtliche Werke under § 5 Abs. 1 UrhG, so keeping them in the repository is
      unproblematic.
      `felder/` holds one index per form — page, technical name, kind, admissible values, label —
      read with the same PDFBox the server uses. 1894 fields across the eight.
      *Two findings that shape 4.2 and 4.3.* The technical names carry no meaning whatsoever:
      `Textfeld 353`, `Kontrollkästchen 3036`. What a field is stands only in its tooltip — and
      every field of all eight forms has one, which is what makes a mapping profile writable at all
      rather than a search on the printed form. And every one of the 663 check boxes uses the same
      on-state, `Ja`. The filler will still read it instead of assuming it: a later version may
      change it, and a wrong on-state produces a form that prints as ticked and reads as empty.
      *Also worth knowing:* Anlage 4 is the application and Anlage 5 the draft order the court
      adopts, which is why the draft has five times the fields (411 against 84). Both are needed;
      the court expects the draft with the application.
      `ZvfvFormFieldIndexTest` binds the index to the files: a replaced form whose index was not
      regenerated would send the mapping after fields that no longer exist. It also holds the two
      properties above, so that if a future version drops a tooltip or changes an on-state, that
      shows up here rather than in a form filed with a court.
- [ ] 0.5 Register for the EDA (Kennziffer) and agree a test exchange with the dunning court.
      Organisational and not doable from here, but on the critical path: phase 3 cannot be verified
      end to end without a Kennziffer of the firm's own.
      *Correction to the earlier wording of this task:* there is no formal approval procedure for the
      software. What is binding is the registration; the test is recommended, not required.
      Section 7.4 of the EDA-Konditionen: "Zur Vermeidung von Massenfehlern wird empfohlen,
      Mahnbescheidsanträge vor der erstmaligen Einreichung mit dem Mahngericht zu testen. Zum Test
      müssen bereits alle Voraussetzungen für den Echtbetrieb erfüllt sein (Kennziffer, Signatur,
      Datenbezeichnung, etc.) ... Ein Test wird insbesondere dann empfohlen, wenn eine eigene
      Schnittstellensoftware eingesetzt wird. Die im Handel verfügbaren Schnittstellen sind in der
      Regel bereits geprüft. Die Vorgehensweise für das Testverfahren bei DFÜ ist mit dem jeweiligen
      Mahngericht abzustimmen." A test can return messages from the test processing on request
      (Kosten-/Erlassnachricht MB, Monierung) — which is exactly what the import path of 3.10 needs
      to be exercised against something real. It presupposes the firm's own Kennziffer; one borrowed
      from another firm is of no use here, since it names that firm as the Prozessbevollmächtigter
      and the test result attaches to their registration.

      **Where this is written down.** *EDA-Konditionen Format 4.X*, the binding document
      (https://www.mahngerichte.de/wp-content/uploads/EDA-Konditionen.pdf, valid since 01.02.2014,
      last changed 22.09.2017, OLG Stuttgart – IuK-Fachzentrum Justiz), linked from
      https://www.mahngerichte.de/publikationen/eda-konditionen/ . Chapter 6 covers participation and
      the Kennziffer, 7.2 the Ausbaugrad, 7.4 the test. Section 2.3: "Die in der Schnittstelle
      beschriebenen Standards (einschließlich der Datenformate) sind für das Verfahren verbindlich!"

      **Contacts** (cover page of the Konditionen): technical questions, development and *test* —
      Oberlandesgericht Stuttgart, IuK-Fachzentrum Justiz, Referat Mahnverfahren,
      mahn@iuk-fz.justiz.bwl.de . Productive exchange — Amtsgericht Stuttgart, Mahnabteilung,
      70154 Stuttgart, 0711/921-3308.

      **What the registration asks for** (6.1.1, applied for at the Mahnabteilung of the competent
      Amtsgericht; forms at https://www.mahngerichte.de/verfahrenshilfen/kennziffernantraege-der-mahngerichte/
      and .../verfahrenshilfen/allgemeiner-kennziffernantrag/): the EDA-Art (currently DFÜ only), the
      Ausbaugrad, which Kennziffer participates — "soweit ein Prozessbevollmächtigter beteiligt ist,
      ist dieser immer auch EDA-Teilnehmer" — and acceptance of the Konditionen. A Kennziffer is an
      eight-digit numeric key into the court's own database holding designation, address,
      representation, bank details, EDA parameters and the SEPA mandate (6.2); those for a
      Prozessbevollmächtigter are exchanged between the federal states and work at almost all dunning
      courts (6.3).

      **Two points that bear on what is built here.** The transmission route (2.4) is exclusively
      DFÜ over defined internet paths, for lawyers Web-DFÜ-beA — which is the route the export of
      3.17 assumes. And the Ausbaugrad (7.2) is a parameter of the *Kennziffer*, not of the software:
      it decides which court messages come back as EDA records at all, so with an Ausbaugrad of zero
      the import of 3.10 would have nothing to work on. The same section notes that a monition always
      comes on paper in parallel, and that for Widerspruchs- and Abgabenachrichten "die entscheidende
      Nachricht ist hier immer die Papiernachricht (insbesondere wegen Beifügungen)" — one more
      reason the import proposes rather than books

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
      list view itself belongs with the other cross-case lists in the frame of 5.3 rather than in the
      ledger tab of 3.10, since the question it answers is not about one case; starting the VB
      application from it additionally needs the export step of 3.17
- [x] 3.9 `DunningServiceRemote` (English JavaDoc) and the REST endpoint — note the endpoint is
      `DunningEndpointV7`, see 3.15. The interface covers the procedures themselves (list, create,
      update, record a status, read the journal) alongside validation, export and message import.
      *Gap found and closed while doing this:* status changes were not being journalled at all,
      although `dunning_case_events` existed for it and the spec requires date, user and source to be
      recorded. Every move now writes an entry, and the source distinguishes what a person entered
      from what a court message reported — which is the first thing to look at when a procedure turns
      out to have been recorded wrongly
- [x] 3.10 Desktop UI: `Mahnverfahren` tab of the ledger workspace (reminder stages, dunning case,
      status timeline, message import) (+ `.form` files). Split into two tabs rather than one,
      because the pre-court reminders and the gerichtliches Mahnverfahren are two different
      procedures and only the second has procedural dates that deadlines run from: the reminder
      stages keep their panel under the name `Mahnungen` — which is what they are — and
      `Mahnverfahren` now holds the new `ClaimLedgerCourtDunningPanel` with the procedure, the
      court, the references and the journal of its course. The journal is a table and not a status
      field on purpose: what matters is the sequence of dated events, and each row says whether a
      person entered it or a court message reported it.
      `DunningStatusDialog` records a status and says, per status, what the date is taken to mean —
      the day of service, for instance, is where the objection period of § 692 Abs. 1 Nr. 3 ZPO and
      the six months of § 701 ZPO start — and refuses an entry without a valid date, since a status
      without its date carries no deadline. Determining the court asks the *creditor's* address
      (§ 689 Abs. 2 ZPO) and leaves an ambiguous OLG division to the user rather than guessing. Where
      nothing can be determined — which in practice usually means the applicant's federal state is
      simply not recorded in the address — every configured responsibility is offered instead, led by
      its federal state, with the reason for the failure above the list: the choice is the user's in
      any case, and a dead end helps nobody.
      The Kennziffer is chosen by *person*, not typed: it is granted to one lawyer by the court, and
      in a firm with several lawyers each has their own, so the selector offers the lawyers who have
      one (`AppUserBean.lawyer` with a `dunning_kennziffer` from 3.14) and takes the number from
      them. Typing eight digits by hand is a way of filing under a colleague's number without
      noticing. A new procedure starts with the Kennziffer of whoever creates it, and a number that
      belongs to no current user — a lawyer who has left — is shown as it stands rather than dropped,
      since losing it silently would file the next application under a different one.
      `DunningMessageImportDialog` is the document-driven flow: pick the EDA file the beA inbox
      already filed to the case, analyse it, assign each position on its own — a message file is
      collective — and book. There is no inbox to keep in step with the case.
      Also closed here, because it is what makes a recorded service date worth anything: a
      Mahnbescheid served now starts the interest of the components awarded interest *ab
      Zustellung*. Until then such a rule carried no start and nothing would ever have given it one,
      so interest the court awarded would quietly never have been claimed. The decision which rules
      move sits in `ServiceDateInterestPlanner`, apart from the storing: a rule with a start of its
      own is left alone, but one carrying exactly the service date recorded before moves along with
      a correction. Both paths do it — the manual entry and the court message.
      The cross-case worklist *view* announced in 3.8 is not here: it belongs with the other
      cross-case lists in the frame of 5.3, and starting a VB application from it needs the export
      step of 3.17
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
      mapping documented in the code. Complete: key record, applicant and defendant with their
      statutory representatives (C05/C06 and C17/C18, which the format attaches to the party
      immediately preceding them — position is the only link, so the builder emits them there),
      the filing lawyer (C07–C11 via `EdaRepresentativeMapper`), catalogued and free-text claims
      with the additions their catalogue number demands, running interest (C26), already computed
      interest (C19), assignment (C25), consumer-credit data (C27), and the trailer sums.
      One rule of the format shaped the lawyer's area: a Kennziffer for the Prozessbevollmächtigter
      closes C07–C09 — a firm filing under its own Kennziffer has told the court who it is once, and
      repeating it is rejected — while C10 stays open because its fee declarations are made per
      application and cannot be stored in a Kennziffer. C10 is therefore where the day of
      instruction belongs, which decides the applicable RVG version under § 60 Abs. 1 S. 1 RVG;
      the ledger holds no such date, so it is asked for in the export step of 3.17 rather than
      guessed here. The seven ancillary-claim areas C28–C34 are mapped: the ledger's own cost types
      correspond to them one for one, which makes the mapping a lookup rather than a judgement — and
      a necessary one, since a court decides differently on a reminder charge than on collection
      costs. No record area of Satzart 01 is left unmapped
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
- [x] 3.10a Delete a dunning procedure that has not left the firm, from the `Mahnverfahren` tab
      (`removeDunningCase` on the service and the interface, "Entfernen" in the panel + `.form`).
      The need is real because "Neue Mahnsache" creates at once, without a dialog: a mis-click
      otherwise leaves a procedure that can only be dragged along through the status field.
      Where the line runs, and why it runs there: a procedure still in preparation - no court file
      number, no procedural date recorded - is an entry somebody made, and deleting it removes a
      mistake. Once the application has gone to the court it is the firm's record of a court matter,
      and deleting would not undo the procedure at the court; it would only destroy what the firm
      knows about it - the journal with its dates, users and sources, the deadlines computed from the
      procedural dates, and the service date from which the interest of the components awarded
      interest *ab Zustellung* runs. Those procedures are ended with a status instead, which the
      model already has: `WITHDRAWN` and `COMPLETED`. `WITHDRAWN` is explicitly no route back to
      deletion - it is the answer to "this should not have been started", not a way to unsay it.
      The rule sits on `DunningCase` itself as `getRemovalRefusal()` / `isRemovable()`, next to the
      data it judges and following `EnforcementTitle.getMissingPrerequisites()`. That way the server
      enforces it and the panel can say *why* beforehand rather than greying a button out for reasons
      the user cannot see - and neither side can drift from the other.
      One cleanup the database does not do: `dunning_case_events` and `dunning_case_deadlines` hang
      off the procedure with `ON DELETE CASCADE`, but the follow-ups those deadlines created do not -
      `DunningCaseDeadline.reviewId` points at an `ArchiveFileReviewsBean` with no cascade behind it.
      They would have stayed in the calendar pointing at a procedure that no longer exists, so the
      deletion takes them with it. Note the difference from a *lapsed* deadline, which marks its
      follow-up done rather than deleting it: there something happened in the case, here nothing did
- [x] 3.17 Export confirmation step in the ledger workspace, pre-filled from the dunning case, with
      write-back of changed values (+ `.form`). `DunningExportDialog`, opened from "Antrag erzeugen"
      in the `Mahnverfahren` tab. A confirmation step and not one button, because the export is not
      undoable in any useful sense: it writes the file into the case, moves the procedure to
      "Mahnbescheid beantragt", starts the deadlines, and from then on the procedure can only be
      ended by a status (3.10a), not deleted.
      Three things are asked for that the ledger does not know. *Which positions go in* — a claim
      ledger is not an application, a position may be disputed, paid or deliberately left out, so
      every one is listed and ticked individually, with its amount, invoice number and interest-to
      date editable; the value in dispute follows the ticks. *The day of instruction*, which under
      § 60 Abs. 1 S. 1 RVG decides which version of the RVG the court applies to the fees it sets.
      *The offsetting* of Vorbem. 3 Abs. 4 VV RVG — only the part to be set off against Nr. 3305 VV
      RVG, not the whole pre-court fee — together with the declaration of unusual extent or
      difficulty (VV2300M).
      Those three are stored on the procedure by `V3_6_0_24` (`order_date`, `offset_amount`,
      `special_effort`, plus `file_name` for the six-character EDAID of the last export) and written
      back before the file is built, so a repeated export starts from what was applied for rather
      than from a blank form. They belong to the application and not to the Kennziffer — the
      Satzbeschreibung says so in as many words.
      The rest of the C07–C11 area needs no second entry: the lawyer is found by the Kennziffer of
      the procedure, and their bank details from the user profile become the account the defendant is
      told to pay into (C11). C07–C09 stay out, since the Kennziffer closes that area.
      *Found while writing the instructions for the reference files (see
      `src/test/resources/eda/reference/README.md`) and fixed here:* the key record never carried the
      general declarations at all. `VGLM1`/`VGLM2` say whether the claim depends on a
      counter-performance and, where it does, that it has been rendered — § 688 Abs. 2 Nr. 2 ZPO
      makes a Mahnbescheid inadmissible where it depends on one still owed, so an application
      without either declaration is monited. The Online-Mahnantrag refuses to reach its download
      without it; our files went out without it and would have come back. `ASTRVM` is the request
      under § 696 Abs. 1 ZPO to refer the matter on an objection.
      Added by `V3_6_0_33` as three flags on the procedure, written into the key record, asked for in
      the export step, and demanded by the validator: at least one counter-performance declaration,
      or the application is not ready. Deliberately two flags rather than one choice — the
      Satzbeschreibung says "Bei mehreren Ansprüchen können auch beide Felder belegt sein!", so they
      do not exclude each other and the validator must not read both as a contradiction.
      Two further defects fixed on the way. `toClaims` passed a null amount straight through, which would
      have applied for a position with an empty amount field; an input without an amount now applies
      for the position as it stands.
      And a bug class the first real export exposed: `java.util.Date.toInstant()` was called on
      values read back from JPA. A field mapped `@Temporal(DATE)` returns a `java.sql.Date`, whose
      `toInstant()` throws `UnsupportedOperationException` by contract — only `java.sql.Timestamp`
      converts. It surfaced in `EdaValues.date` on the new `order_date`, but the same idiom sat in
      four more places reading DATE-mapped fields: `DunningDeadlineCalculator.toLocalDate` (every
      procedural date), `DunningStagePlanner.dueDate` (`DunningStageEvent.sentDate`), and two follow-
      up computations in `ClaimLedgerService` (the reminder deadline and `EnforcementTitle`'s
      limitation date). Two of those sit inside `try`/`catch` blocks that log and continue, so the
      failure would have been silent — no deadlines, no follow-up, no error. All now convert through
      `Instant.ofEpochMilli(date.getTime())`, which is right for all three date classes. The
      remaining conversions in `ClaimInterestCalculator` read TIMESTAMP-mapped fields and are sound;
      its caller-supplied key date is hardened all the same, since a stored date can be handed in.
      A third one the first export exposed: `EdaFieldLengthException` and `EdaEncodingException` live
      in the EJB module and are not on the desktop client's classpath, so letting one escape a remote
      method turned a precise sentence — which value, which field, how much too long — into
      "Failed to read response" with a `ClassNotFoundException` underneath. `exportApplication` now
      translates both into a plain `Exception` carrying their message, and the dialog walks the chain
      of causes rather than showing a wrapper's empty message. No EDA type appears in any remote
      signature; these two escaped as thrown exceptions only.
      And the document tab does not poll: a file created from a dialog only appears once the case is
      reopened unless a `DocumentAddedEvent` is published. The export does that now — and so does
      `ClaimStatementDialog`, which stored the claim statement without one since it was written
- [x] 3.18 EDA viewer: formatted view resolving record types, section markers and fields, raw
      record view, tabbed panel and integration into the document viewer of `ArchiveFilePanel`
      (+ `.form` files). `EdaPanel` implements `PreviewPanel` and is registered in
      `DocumentViewerFactory` for `.eda`, so an exchange file opens in the case's document preview
      like any other document, without a route of its own.
      Both views earn their place. The formatted one resolves every record against its
      Satzbeschreibung and names the fields, because 128 bytes with nothing between the fields means
      reading by counting columns. The raw one keeps the file exactly as it stands, because when a
      court complains about a file the question is what is actually in it, byte for byte.
      The heading answers the direction first — an application the firm sent or a message the court
      sent back — and derives it from the record type in the file header rather than from where the
      document sits in the case, which is what 3.10's message import needed too.
      The resolving happens server-side, where the Satzbeschreibungen live: `EdaDocumentDescriber`
      turns a file into the serialisable `EdaDocumentView`, reached through
      `describeEdaDocument(documentId)` with the case's access check. The client draws it and does
      the raw view from the bytes it already holds. A record no description covers is still shown
      with its raw line and no fields, so a file from a newer format version stays partly readable
      instead of appearing empty; filler is left out, since reserved space would bury the fields that
      do say something. Tests write a file with the builder and read it back with the describer, so
      what is verified is that writer and reader agree — a description that had drifted from the
      writer would be worse than none, because it would be believed.
      The fields are named too, not only the records: `EdaFieldLabels` carries the "Feldname lang"
      column of the Satzbeschreibungen for all 267 field names, so the tree reads
      "Anspruchsbetrag [ASPBET]" rather than "ASPBET". The short name stays beside the long one on
      purpose — it is what a monition of the court will name. The table is deliberately apart from
      the layouts: a layout says where a field sits and how long it is, which is what writing and
      parsing need and must not depend on a label; a field with no entry is shown under its short
      name rather than suppressed. A test walks all three layout registries and fails on any field
      the table does not name, since drift would leave raw short names exactly where the file is
      least self-explanatory.
      One thing the first stored file showed: the document was named after the own reference, which
      is normally the case file number — and a German one carries a slash. That is a path separator
      to every program that saves the file locally afterwards. All three document names the feature
      produces now go through `ServerFileUtils.sanitizeFileName`: the exchange file, the claim
      statement (where the name can be supplied by the caller) and the reminder letter (whose name
      comes from the freely configured stage name, "1./2. Mahnung" being a plausible one)
- [x] 3.19 Tests: status transitions, deadline generation/recalculation, fee credit, message import.
      Three of the four were already covered by the classes that carry the decisions:
      `DunningDeadlineCalculatorTest` (18) for generation and recalculation including the shift to
      the next working day under § 222 Abs. 2 ZPO, `DunningFeeCalculatorTest` (16) for the fee
      credit, and `EdaMessageReaderTest` (11) plus `EdaMessageInterpreterTest` (12) for the import —
      which message moves a procedure and which only reports.
      What was missing sat inside `DunningService` where no test could reach it, and two pieces of it
      are decisions rather than plumbing, so they moved out. `DunningStatusPolicy` holds the
      regression guard and the procedural dates: the guard is what makes re-reading a court file safe
      at all, and since the import works from a document of the case rather than an inbox, re-reading
      one is the normal case and not an accident. `DunningWorklistSelector` holds the filtering, the
      "objection period run" question — computed from the service date rather than read from the
      deadline records, so it answers even where the calendar entries were never created — the
      earliest open deadline, and the CSV.
      Two things the new tests pinned down that the code got right but nothing was holding: on the
      last day of the objection period nothing has expired yet, because the period ends with the
      expiry of that day (§ 188 Abs. 2 BGB); and a semicolon in a party name is quoted, since
      otherwise it shifts every following column of that row into the wrong one
- [x] 3.20 Tests: record layouts against the worked examples of the Satzbeschreibungen, mapping per
      claim and ancillary-claim type incl. catalogued claims with their required additional record,
      interest from service written with an empty start date, CP-850 round-trip incl. umlauts,
      validation and verification failure paths, export document storage and re-export history.
      *Covered so far:* interest from service with an empty start date, the CP-850 round trip with
      umlauts, the validation and verification failure paths, catalogued claims with their required
      additional record, the seven ancillary-claim types each into its own area, and the party
      records against the official "Eintragungsbeispiele" — which
      caught a real error: a GmbH & Co. KG takes salutation key 4 with an empty legal-form field,
      while an AG & Co. KG, which looks like its near relative, takes no key and carries its legal
      form; the mapper had treated both as ordinary legal persons.
      The last open item — export document storage and re-export history — turned up two defects
      rather than only missing tests, so `DunningExportPolicy` now carries the decisions and is
      tested against them.
      A repeat export is a normal thing: a monition has to be answered with a corrected application.
      But it was allowed at any point, and on a procedure the court had already decided it silently
      reset the status to "beantragt" and overwrote the application date with today. The policy now
      refuses an export once the court has issued, served or otherwise decided — after that, another
      application is a new procedure, not a repeat of this one — and the day the application first
      went out is kept, since a corrected application does not start the procedure again and § 167
      ZPO ties the effect of service back to the day it was applied for.
      Second, every export produced a document of the same name, so two of them could not be told
      apart. The name now carries the EDA file name of that export, and the export step generates a
      fresh one for a repeat rather than reusing the last. That is not cosmetic: the receipt the
      court sends back (Satzart 90) names the file and carries no procedure reference at all, so the
      file name is the only thread from a receipt to what was sent. The journal says which export was
      a repeat, because two identical entries would leave the reader to work out which one the court
      answered

## 4. Phase 4 — Enforcement

- [x] 4.1 `EnforcementMeasure` entity, catalog of measure types and their configuration.
      `EnforcementMeasure` (V3_6_0_37) records what was done, to whom, when and what came of it —
      a workflow object, not a second place where money lives: what is recovered is booked into the
      ledger. It names its debtors explicitly rather than taking all of the ledger's, because
      enforcement runs against the debtor named in the title and can be pursued against one joint
      debtor and not another. The addressee is frozen in the wording used towards them, like a party
      designation, so a measure filed years ago stays reconstructable after a bailiff's district has
      been redrawn.
      `EnforcementMeasureOutcome` distinguishes the six the specification asks for, and the
      distinction that matters is `isClosed()`: a stayed measure — the debtor paying by instalments
      under § 802b Abs. 2 ZPO — has *not* run its course. If the instalments fail it goes on from
      where it stood rather than being started again.
      The kinds are a table (V3_6_0_38, fifteen seeded entries), not an enum. Firms differ in what
      they use and a practice that never files a compulsory mortgage should not have to look at one;
      and what a kind entails — which ZVFV annex, which addressee, which follow-up period, whether
      it needs a title at all — is configuration. `form_key` names the annex rather than a template,
      because templates carry validity periods and the one in force when the measure is created is
      the one to use.
      *Corrected in V3_6_0_39:* the annex numbers of the seed were given from memory and several
      were wrong. The ZVFV 2022 has eight annexes, and the two mistakes mattered: the attachment
      order is Anlage 4, not 2 — Anlage 2 is the application for a judicial search order, which I
      had recorded as having no form at all — and there is only **one** attachment application
      whether the claim is maintenance or not. What differs is the itemisation attached to it,
      Anlage 7 against Anlage 8, so the type gained a second key, `itemisation_form_key`, and the
      bailiff order takes Anlage 6. The correction updates only rows still carrying the wrong value,
      so a firm that fixed it first keeps its own.
      `EnforcementMeasurePlanner` answers which kinds can be taken today and, for the rest, what is
      missing — each of the three conditions of § 750 Abs. 1 ZPO named separately, because "the
      title is incomplete" does not tell a firm what to go and fetch. It also holds the 750-euro
      floor of § 866 Abs. 3 ZPO for the compulsory mortgage. Kinds that cannot be taken are returned
      with their reason rather than dropped: a dialog that silently omits the bailiff order leaves
      the user wondering.
      `MigrationConventionsTest` gained two further checks, both paid for by a failed deployment.
      Hibernate writes an `@Enumerated(STRING)` as the name of the constant, so a typo in a seed
      settles into the database without complaint and surfaces much later as an
      `IllegalArgumentException` far from its cause; every uppercase literal of these migrations now
      has to be an addressee type, an outcome or a form key.
      And V3_6_0_38 failed on deployment with "Duplicate column name '1'": only the first of the
      fifteen seed blocks aliased its columns, and a derived table names an unaliased column after
      the text of its expression — so three columns holding the literal 1 were three columns called
      "1". The test now derives the column names of every `FROM (SELECT ...)` the way MySQL does and
      requires them to be distinct. Removing the aliases again makes it fail with the offending
      name.
- [~] 4.2 Form template management (PDF + field mapping + validity). **The model is built; the admin
      UI and the import of the default package are still open.**
      *Two decisions, both the user's.* The PDFs live in the database — the number of forms is small
      and the versions few, and a backup of the database is then a backup of everything needed to
      reproduce a filing. And the forms of a measure are a list with roles, not a column each.
      The second deserves its reasoning. `form_key` and the hastily added `itemisation_form_key`
      were really two entries of one list, and the list is longer than two: an attachment order is
      the application (Anlage 4), the draft order the court adopts as its own (Anlage 5) and the
      itemisation (Anlage 7 or 8). Three is the most any kind needs today — but the number is not
      the point, the **role** is, because it decides what fills the form: the application from the
      measure, parties and title, the draft from the same in the language of a decision, the
      itemisation from the ledger through 4.4. Three unnamed columns would leave the code to work
      that out from their order. V3_6_0_41 moves the two columns into
      `enforcement_measure_type_forms` and adds the draft orders, which had been missing entirely —
      the court expects the draft with the application.
      V3_6_0_42 adds the templates with their validity and the mapping profile. The profile belongs
      to the **version**, not the form: a new version may rename its fields, and a profile written
      for the old one would address fields that no longer exist. It is rows rather than a document
      in a column, because a profile is edited a field at a time, compared between versions and
      checked against the form — and a mapping naming a field the form no longer has should be a
      query, not a parse.
      `EnforcementFormTemplateSelector` picks the version in force on the day the measure is
      created. Where none is in force it **warns rather than refuses**: a firm that has to file
      today cannot wait for us to ship a template, and filing on last month's form beats not
      filing. Two versions valid at once is a master-data defect; the newest is used and said to be
      a guess.
      Ten tests, three mutations killed. A fourth survived at first and was worth the catch: the
      boundary test checked *which* version came back on the day a new one takes effect, and under
      an exclusive start date the same version came back through the "none valid" branch — right
      answer, wrong reason. It now asserts the selection is current as well.
      *The default package is imported, on a button.* The eight forms moved from the test resources
      to `src/main/resources/zvfv/` so they exist once in the repository and travel in the
      deployment; the field index stays behind as working material. `EnforcementFormPackage` holds
      what cannot be read off a file — which file is which annex, since the publisher names them
      "Antrag_Pfaendungsbeschluss" and not "Anlage 4" — and `EnforcementService.importDefaultForm​Package()`
      creates a template per form. It runs without harm a second time: a form already held under the
      same version is left as it is, adjustments included. The import adds what is missing and
      changes nothing that is there.
      Deliberately **not** on startup. `ContainerLifecycleBean` wires reference data and writes
      nothing to the database today, and giving it silent write access for this would be the wrong
      trade; the forms are master data of an installation and appear because somebody decided they
      should. An empty template store is visible in the enforcement tab instead.
      Considered and rejected: the PDFs as hex literals in a Flyway migration. 3.1 MB become 6.2 MB
      of hex, a single statement of that size runs into `max_allowed_packet`, the file is frozen by
      its checksum forever — every new form version another one — and the bytes would sit in the
      repository twice.
      Seven tests on the package, three mutations killed: naming a file that is not shipped, listing
      a Hinweisblatt (which looks like a form and carries no fields) as an annex, and giving two
      entries the same annex number.
      *The vocabulary a profile is written against.* `EnforcementFormDataSource` answers the keys a
      mapping may point at — `glaeubiger.name`, `schuldner.ist_herr`, `forderung.gesamt` — built
      from the measure, the parties, the title and the itemisation of 4.4, so the form and the
      firm's own statement cannot disagree about a sum.
      Deliberately a closed vocabulary and not an expression language. A profile is written by an
      administrator, not a programmer; a language would let them write something almost right, and
      the place that shows is a form already at a court. A closed set can be offered in a list and
      got wrong only by choosing the wrong key, which is visible when the form is read back.
      The tick marks are keys of their own for the same reason. The forms do not ask for a
      salutation, they offer four boxes — Herr, Frau, Unternehmen, Sonstige — so the vocabulary
      answers in that shape and the profile stays a plain assignment of field to key. A key plus a
      condition would be a small language again.
      Two properties carry it: **every key answers** whether or not the case has the data, so a
      profile pointing at something absent clears the field instead of leaving what the template
      held; and the designation used towards the court goes before the contact's current one, so a
      filing stays reconstructable after somebody tidies an address.
      Thirteen tests, four mutations killed — among them writing "Deutschland" into the field
      labelled "Land (wenn nicht Deutschland)", and ignoring the frozen designation. A fifth
      survived and was worth it: the test called "a company is a company whatever its salutation"
      gave its company no salutation, so a company carrying a stale "Herr" would have been ticked as
      a company *and* as a man. The fixture now carries one.
      *The profile for Anlage 1* (V3_6_0_43, 40 fields) assigns the bailiff order's fields to those
      keys: addressee, who is filing, creditor, debtor, reference, place and date — what an order
      needs at minimum. The options of § 802a Abs. 2 ZPO belong to the individual measure, not to
      the form, and arrive with the measure UI.
      `field_label` carries the form's own tooltip alongside each assignment. It is the only evidence
      that a mapping means what it claims: `Textfeld 220` says nothing, "Name/Firma" says something,
      and if a later version gives that name a different meaning the comparison against the field
      index shows it.
      `Anlage1MappingTest` holds the profile against both ends — every field it names exists in the
      form, every key it uses is one the vocabulary answers, every recorded label still matches, and
      no tick key sits in a text field or the reverse. Plus the through-test: fill the form from the
      profile and accept not one rejection.
      *What none of that proves* is that a field **means** what its label says — that the block on
      page 3 carrying a Geburtsdatum really is the debtor and not a second creditor. The labels
      repeat across blocks ("Name/Firma" appears for creditor, debtor and representative alike), so
      the assignment was read off the form's order. Settled the only way it can be: a filled sample
      was produced from the profile and checked against the form. Creditor, debtor and the filing
      firm each land where they belong.
      *Still open in 4.2:* the administration UI.
- [~] 4.3 New name-based AcroForm filler beside `PdfFormsAccess`. **The filler is built and tested
      against the real forms; storage in the case and recording of the form version wait for the
      service of 4.8.**
      Beside `PdfFormsAccess` rather than inside it because the two solve different problems. That
      one fills a firm's own templates, where a placeholder is written into a field's value or its
      tooltip and is found by searching for it. The ZVFV forms carry no placeholders: their fields
      have fixed names, meaningless in themselves, and a mapping profile says which holds what.
      Searching would find nothing.
      *Check boxes are where this earns its keep.* A box is not ticked by writing "true" into it;
      each carries an on-state of its own, and `AcroFormFiller` reads it from the field rather than
      assuming it. A value that means neither clearly ticked nor clearly empty is **refused**, not
      guessed at — ticking a box because the data said "vielleicht" would assert something nobody
      said, and nobody would notice before the court did. "nein" and "false" untick it.
      *Mandatory fields come from the profile, not the form.* None of the eight forms sets the
      Required flag on a single field, so there is nothing in the PDF to validate against; the
      profile names them and `AcroFormFillResult` reports which stayed empty. It separates three
      findings of different weight: a field the form does not have (a broken profile), a mandatory
      field left empty (missing data), and a value that could not be applied (the dangerous one).
      Twelve tests, every one against a real court form and every one **reading the saved file
      back**. Setting a value and believing it is how one produces a form that looks right in the
      code and is empty on the paper. Five mutations — hard-coding the on-state, accepting any
      non-empty value as ticked, swallowing unknown fields, letting whitespace pass as a mandatory
      entry, not flattening — all killed.
      *Noted for 4.9:* Anlage 4 is the application and Anlage 5 the draft order the court adopts, so
      a PfÜB measure produces **two** documents, not one. The measure type carries only `form_key`
      today; that needs a second one, or the draft has to be derived from the application.
- [x] 4.4 Claim itemisation for ZVFV Anlagen 6–8 from the ledger, sharing the statement calculation.
      **Derived from the claim statement, not computed a second time.** The specification requires
      the two to agree; deriving makes them agree by construction rather than by a test that has to
      be re-run whenever either side changes. Two calculations meant to produce the same figure
      eventually stop doing so, and the day they diverge is the day a bailiff collects a different
      sum from the one the firm's own statement shows.
      *A gap in the model had to be closed first.* Anlagen 6 to 8 give a titled claim a different
      line from a further one, and the bailiff acts on the difference — he enforces what the title
      carries and nothing else. But a title did not know which positions it covered: there was no
      link between `EnforcementTitle` and `ClaimComponent` and no date on a component to infer one
      from. V3_6_0_40 adds `enforcement_title_components`. Without it the classification would have
      been a guess, and a guess here either asks the bailiff to enforce without a basis or leaves
      out what the title awards.
      Where a title says nothing — one recorded before this was tracked — everything goes on the
      further line and the itemisation is **marked as not knowing**. That asks for less than the
      title allows rather than more, and it is visible rather than silent.
      Costs the title does not carry are not a further *claim*: the costs of the enforcement itself
      are recoverable under § 788 ZPO without a title of their own, and the form gives them their
      own line.
      The rate goes on the form, not the money — the bailiff works out the interest on the day he
      collects. So the row carries the rate, which of the form's two boxes applies ("Prozentpunkte
      über dem Basiszinssatz" or "Prozent") and from when it runs, all taken from the continuing
      interest the statement already worked out rather than derived from the interest rules again.
      Fourteen tests, five mutations killed: deducting a reversed payment twice, treating any
      booking as a payment, filing untitled costs as a further claim, declaring everything titled
      when the coverage is unknown, and writing the computed rate where the margin belongs.
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
      measure, open amount), plus a desktop widget for overdue recovery deadlines. The dunning
      worklist view moved here from 3.10 together with starting a VB application from a row, for
      which the server side of 3.8 is already in place
- [ ] 5.3a Decide how a sub-ledger is assigned, and act on it. `ClaimLedger.parentLedger`
      (`parent_ledger_id`) exists from 1.2 and carries a real purpose: a claim that is titled,
      calculated or enforced separately while belonging to the same matter — the titled principal
      running on while subsequently assessed costs form their own ledger with their own interest, or
      several debtors kept apart but reported together. The claim statement uses it: "Unterkonten
      einbeziehen" assembles a full statement per sub-ledger, recursively, keeping each ledger's
      positions, interest and payments apart rather than merging them. `updateClaimLedger` carries
      the field through.
      *What is missing:* no dialog in the client ever sets it. The relation can only be established
      directly in the database, so in practice a ledger has no sub-ledgers and the checkbox in the
      statement dialog changes nothing. It promises something the user interface cannot deliver.
      *Proposed:* add a field "übergeordnetes Konto" to the ledger master data offering the other
      ledgers of the same case, guarded against selecting itself and against cycles. The server side
      is complete; only the field is missing, and hiding the checkbox instead would leave working
      logic unreachable. Decide and then either build the field or take the checkbox out — an option
      without effect is worse than an absent one. The documentation in `doc/` says today that a
      ledger *can* be assigned to another without noting that this is not possible through the user
      interface; it is to be corrected either way.
- [x] 5.3b Legal representatives: a chain, not a single person, and their function from the court's
      own directory. `ClaimLedgerParty.legalRepresentative` held **one** `AddressBean`, and
      `EdaPartyMapper.mapLegalRepresentative` wrote at most one pair of records from it. The format
      allows six: *"Zu jedem Antragsteller können maximal 6 gesetzliche Vertreter (ASGV_01/ASGV_02)
      eingetragen werden! Gesetzliche Vertreter werden immer dem unmittelbar vorausgegangenen
      Antragsteller zugeordnet!"*
      That is not an exotic case. A GmbH & Co. KG — one of the commonest German legal forms — is
      represented by its personally liable partner (§ 161 Abs. 2 i. V. m. § 125 HGB), which is the
      Komplementär-GmbH, and that GmbH acts through its Geschäftsführer. Two levels, and the
      Online-Mahnantrag asks for both. With one slot we could enter the GmbH or the Geschäftsführer,
      not the chain, and whichever was entered was wrong on its own.
      **Built.** `ClaimLedgerPartyRepresentative` (table `claimledger_party_representatives`,
      migration V3_6_0_36) holds one step of the chain: the party, the contact, the position and the
      function. `ClaimLedgerParty.representatives` replaces the single field — lazy, uncascaded,
      owned by `ClaimLedgerService`, which loads it for remote clients and rewrites it on save, the
      way `CourtService` handles court scopes. The migration copies existing single representatives
      in as the first step and drops the old column. `EdaPartyMapper.mapLegalRepresentatives` writes
      the chain in order; the party dialog edits it as a table with a chain preview ("X, vertreten
      durch Geschäftsführer Y").
      How an entry is written follows the courts' own files rather than our guess: a **company** as
      representative goes into the Stellung field by its name with the name field left empty, a
      **person** into both. The address record follows only where an address is recorded — the
      reference files omit it for a representative given by name alone. Both rules were read off
      `03-ag-gmbh-co-kg.eda` and `03b-ag-ag-co-kg.eda` and are now held by conformance tests.
      **Second half, also built.** The two directories are transcribed verbatim into
      `legal-representatives.txt` beside the other reference data (632 legal forms, 1130 functions,
      retrieved 2026-09-21) and read through `LegalRepresentativeDirectory` /
      `BundledLegalRepresentativeDirectory`, reachable as
      `ReferenceData.getLegalRepresentativeDirectory()`. The party dialog offers the admitted
      designations per chain step, and `DunningApplicationValidator` warns where one is outside the
      list. Which list applies depends on whom the step acts for: the first entry is measured against
      the party's legal form, every later one against its predecessor's.
      Deliberately a **warning, not a block**: the function list is Stand 06.10.2015 and the legal
      form it is keyed by is free text on the contact, so an unknown legal form yields no list and
      nothing can be said. Refusing an application over a gap in our reference data would be worse
      than the monition it is meant to prevent. The chain being longer than six *is* an error — the
      seventh entry would simply not be written.
      Three things the transcription turned up, recorded in the resource header so nobody reads them
      as our damage:
      - The list contains **no bare `GMBH`**. It has `GESELLSCHAFT MIT BESCHRÄNKTER HAFTU` and
        `GESELLSCHAFT MIT BESCHR. HAFTUNG` (key 10) and every combination like `GMBH & CO KG`, but
        not the abbreviation every contact record in practice carries. Resolved in 5.3e by an alias
        table of our own; the published data stays untouched.
      - `TREUHÄNDER ? 313 INSO` and `TREUHÄNDER GEM. ? 313 INSO` carry a literal question mark in the
        courts' PDF (0x3F in the text stream, where the neighbouring entries have 0xA7). Presumably
        old charset damage on their side. The list says which spellings are admitted, so ours says
        what theirs says.
      - Keys 52, 57 and 90 appear among the legal forms but have no function group, and that is
        correct: key 90 are the Parteien kraft Amtes — an Insolvenzverwalter *is* the representation
        and has none — and key 57 names the WEG's administrator in the legal form itself, so who acts
        for it follows from the next link of the chain. A test asserts it is exactly these three, so
        a botched re-import cannot pass as a property of the source.
- [x] 5.3d Migration V3_6_0_36 failed on deployment, and a test now stands where it failed.
      *Two defects in one script.* The table was written `CHARSET=utf8mb4` while the tables it points
      at are utf8, so MySQL refused both foreign keys with errno 150, "Foreign key constraint is
      incorrectly formed" — a message that names everything except the cause. And the copy of the
      existing single representatives built its key as `concat('rep-', p.id)`, which can exceed the
      50 characters of the column; the row id is now the party id itself, which is unique and cannot
      overrun.
      `MigrationConventionsTest` reads every migration, works out the character set of every table
      it creates and every foreign key it declares, and fails where one crosses a character set
      boundary. Deliberately not "every table must be utf8": several tables of this schema are
      utf8mb4 and are fine, because nothing points from them into the older part. utf8 and utf8mb3
      are treated as one, which they are. Planting the original defect again makes the test fail with
      the table names in the message.
      *To redeploy:* the failed attempt left a row in `flyway_schema_history`, and Flyway refuses to
      continue past it. It has to go before the next start:
      `delete from flyway_schema_history where version = '3.6.0.36' and success = 0;`
      The script itself is re-runnable — the CREATE TABLE never took effect, so nothing was left
      half-done.
- [x] 5.3e The function dropdown was empty, because our master data and the courts' list do not
      speak the same language. j-lawyer seeds nine legal forms — `AG, Einzelunternehmen, e.K, GbR,
      OHG, KG, GmbH, UG, eG` — and the courts' list spells every form out and carries no
      abbreviations at all: `AKTIENGESELLSCHAFT`, `GESELLSCHAFT MIT BESCHR. HAFTUNG`,
      `KOMMANDITGESELLSCHAFT`. Seven of the nine found nothing, so the field stayed empty for nearly
      every company a firm records, and the check that was supposed to prevent a monition never ran.
      Two things were missing, and they are of different kinds.
      *Punctuation is not part of the identity of a legal form.* `GmbH & Co. KG` — the case that
      started this whole task — did not reach the published `GMBH & CO KG` for want of a full stop,
      and `e.K` did not reach `EK`. Stops are now dropped before matching, not turned into a space,
      because the list writes both `E.KFM.` and `EK`.
      *Abbreviations need a table, and it is ours, not theirs.* It claims nothing about the practice
      of the courts — only that a GmbH is a Gesellschaft mit beschränkter Haftung, which is a fact
      about the legal form. It decides which list of designations is offered and checked against and
      never touches what goes into the application, where the legal form is written as the user
      recorded it.
      Lookup is exact, then relaxed, then resolved, stopping at the first that answers, so a
      published spelling is never overruled by a guess. Where a relaxed spelling would answer with
      two different keys it answers with none — a wrong list is worse than no list, because it warns
      the user off a designation that is in fact correct. The one such case is the courts' own:
      `WEG VERTRETEN DURCH VERWALTER-GMBH` stands in their PDF under both key 10 and key 57, and a
      test holds that it is still the only one.
      Where no list is found the dialog now says so instead of showing an empty field: the
      designation is then free text and is not checked.
      *Also learned:* key 03 (Aktiengesellschaft) admits `GESCHÄFTSFÜHRER`. Surprising, but it is
      what the list says — the test distinguishes AG from GmbH by `AUFSICHTSRAT` instead, which only
      the AG carries.
- [x] 5.3f Closed value sets where the format prescribes one, and the missing UI behind them.
      The question was whether every dropdown should be restricted to the values the EDA format
      admits, accepting that a firm might have to record dunning master data a second time. The
      answer differs per field, and the distinction is what the reference files show:
      *Closed, and nothing is duplicated.* The **function of a legal representative** and the
      **contract type for catalogue 28** exist nowhere else in j-lawyer — there is no second source
      to keep in step, only the choice between picking from the list and typing a word the court
      will monition. Both are now picked. The function field is a closed combo wherever a list
      exists and takes free text only where the legal form is one the directory does not know; a
      value recorded earlier survives even if it is off-list, so opening an old party does not
      silently drop what already went to a court.
      *Not closed: the legal form.* The courts' own Online-Mahnantrag writes values into `ASRF` that
      are not on its own list — `02` and `03` carry a literal `GmbH`, and `GMBH` does not appear in
      the *Liste der Rechtsformen*. Enforcing it would make us stricter than the court and would
      break conformance case 02. The list is the key index, not the admissible content of the field.
      Decided against an override per party; the contact's legal form stays the single source.
      *The UI that was missing.* `catalogueNumber` and `catalogueContractDesignation` were entity
      fields that the EDA mapper read and **nothing ever set** — the same shape of gap as the
      ancillary claim mapper. `ClaimComponentEditorDialog` now offers the catalogue as a closed
      list with "sonstiger Anspruch" as its first entry, and shows exactly the additional field the
      chosen number demands. `ContractTypeCatalogue`, implemented long ago and called from nowhere,
      is finally the source of that list.
      *A validator that was accepting the wrong answer.* It asked whether **any** of the four
      additional fields was filled, so a postcode satisfied catalogue 28, which wants the contract
      type. `CatalogueAddition` resolves a number to the one field that answers it — 19/20/90 the
      property location, 28 the contract type, 36/42/61/70 the reference detail — and the check now
      asks that field. A contract type outside the published list is refused rather than warned
      about: unlike the representative's designation, the monition is documented rather than
      suspected. Two tests hold the resolver against the catalogue in both directions, so a number
      that gains a requirement in a later Stand cannot pass unresolved.
      *Since answered by the courts' own wizard, without a file having to be produced:*
      `catalogueReferenceDetail` now reaches the record. Asked for catalogue 36 and again for 61, the
      Online-Mahnantrag refused the entry anywhere but one place — *"Bitte die Kontonummer im Feld
      Rechnungsnummer eintragen"*, *"Bitte die Art der Wahlleistung im Feld Rechnungsnummer
      eintragen"*. So **36, 42 and 61 all go into `ASPRNR` and displace the invoice number**: one
      column, not two.
      That corrected a guess. The catalogue points 36 at the "3. Spalte" and 61 at the "2. Spalte",
      and reading that prose I had put 61 into `ASPGR` and written a class, `EdaCatalogueDetailColumn`,
      to express the distinction. There is no distinction; whatever those columns count on the paper
      form, the file has one field. The class is gone and the decision lives in `CatalogueAddition`
      where the rest of it already was.
      Catalogue **70** turned out to demand no field at all: asked for a Kindertagesstättenbeitrag
      the wizard wants nothing further, because the *Zeitraum vom – bis* it names is the claim line's
      own from and to. It is a kind of its own, `CLAIM_PERIOD`, rather than `NONE` — the catalogue
      does state a requirement and a reader should find out where it is met, not conclude there is
      none. The validator no longer demands a field for it, which it had been doing.
      One consequence remains: giving both an invoice number and the further entry for 36, 42 or 61
      is refused at export rather than silently resolved. Which of the two is dropped is not ours to
      decide.
- [x] 5.3g The hint under the representative table was mostly invisible, and the dialog did not
      reflow when resized. Both had the same cause: the hints were `JLabel`s carrying HTML at a
      fixed height (60px and 34px) and a fixed width, and every other component in the dialog was
      laid out at `PREFERRED_SIZE`, so growing the window changed nothing and the surplus was
      swallowed by a trailing `addGap(0, 0, Short.MAX_VALUE)`.
      A label given HTML wraps at whatever width it is laid out with but keeps the height it was
      laid out with, so the rest is simply cut off — and the chain preview is as long as the chain
      is. The two hints are now non-editable, word-wrapping `JTextArea`s in borderless, transparent
      scroll panes: they report the height their content needs, they grow with the dialog, and a
      chain longer than the space can be scrolled instead of lost. The text became plain with line
      breaks; the HTML was only ever there for the `<br/>`.
      The dialog itself now resizes: the contact combo, the court's place, the representative table
      and both hints carry a range instead of one fixed size, and a minimum of 560x520 stops it
      being dragged below the point where the table shows no row.
      Measured rather than assumed, with a throwaway program that builds the dialog off-screen:
      packed 725x520 with 74px of hint for the 51px a two-step chain needs; enlarged to 1025x780 it
      gives the hint 204px. A six-step chain with a long designation needs 204px and scrolls at the
      packed size. `setSize(100, 100)` is clamped to the minimum.
      Not covered by a test: it would need a display, and a layout test that fails on a headless
      build would cost more than it catches.
- [x] 5.3h Two buttons saying "Speichern" in one tab, and a court that quietly went missing.
      Reported as a usability problem: after choosing a dunning court the tab says the assignment is
      taken over "mit Speichern", and there are two such buttons in view. Behind the ambiguity sat a
      data loss. The claim ledger dialog's own save stores the ledger and **closes the window**; the
      procedure in the "Mahnverfahren" tab is a separate record with its own button. Choosing a
      court wrote it into memory only, so the natural next click — the big Speichern at the bottom —
      saved the ledger, shut the dialog and dropped the court without a word.
      *The court is now written at once.* Choosing one is an answered question, not a draft, so it
      is stored there and then and the message says it is stored. That removes the sentence that
      caused the confusion instead of rewording it.
      *The buttons say what they save.* The tab's is "Mahnsache speichern" and carries a tooltip
      naming the other one. Same-named buttons doing different things is the defect; a clearer hint
      would only have described it.
      *And nothing leaves silently.* The tab reports unsaved entries, and the dialog asks before it
      closes — on Speichern and on Abbrechen alike — jumping back to the tab if the user wants to
      finish there. Switching to another procedure in the selector asks too, because it overwrites
      the same fields.
      Two things found while doing it. `loadCases()` reset the selector to the first procedure after
      every save, so anyone working on the third had to find it again each time; the shown procedure
      now survives a reload. And a newly created procedure was not the one shown afterwards — it is
      now.
- [ ] 5.3c The salutation key from the legal form directory — decide whether to use it. The
      *Liste der Rechtsformen* carries an `Anrede-MM` column, which is the key the format expects,
      and `EdaPartyMapper.salutationKey` currently guesses it from the wording of the legal form.
      Replacing the guess with the published value looks obvious but the reference files argue
      against it: for `Muster Transport GmbH & Co. KG` the directory says key 4, and the courts'
      own Online-Mahnantrag wrote **no key at all** and put the legal form in as text
      (`01AG   01 GmbH & Co KG`). Same for the AG & Co. KG in `03b`. So either the wizard does not
      apply this list to companies, or the key means something other than what the column name
      suggests. Our current behaviour matches the reference files and is therefore left alone.
- [x] 3.21 Conformance against real files of the dunning courts. Twelve applications were produced
      through the Online-Mahnantrag under a real Kennziffer and are kept in
      `src/test/resources/eda/reference` with the instructions that made them
      (`reference/README.md`). They are correct by construction, so whatever our code objects to in
      them is our fault — the sharpest check available short of the court's own test run.
      `EdaReferenceFileTest` reads every one of them, has the verifier pass judgement, and asserts
      that we hold a Satzbeschreibung for every record in them. What that turned up:
      *A real file has no record separators at all.* It is an unbroken stream of 128-byte records.
      That is not one court's habit: the character repertoire of the EDA-Konditionen (4.3.2) begins
      at X'20' and lists only printable characters, so a CR or LF between records is a character the
      format does not admit. We were writing `\r\n` after every record — our files carried
      inadmissible characters, and our own verifier could not see it because it split on exactly
      those characters before checking the repertoire. Worse in the other direction: verifier,
      describer, message reader and `EdaFile.parse` all split on line ends only, so a genuine court
      file would have been read as one 1408-byte record and rejected, and the message import would
      have found nothing in anything the courts sent. `EdaRecords.split` now serves all four: line
      ends where they exist (which keeps the diagnostics of a hand-made file readable), otherwise
      every 128 characters.
      *The record C16 was missing entirely.* Every reference file carries it once per defendant, and
      § 690 Abs. 1 Nr. 5 ZPO requires the application to name the court competent for the contested
      proceedings. Added by `V3_6_0_35` per party, not per procedure, because that is how the format
      holds it — two defendants whose general venue lies apart have different courts, and the
      courts' own file for two joint debtors carries two such records. `LitigationCourtType` names
      the five kinds rather than carrying the raw digit and derives the Amtsgericht/Landgericht
      split from the value in dispute (§ 23 Nr. 1, § 71 Abs. 1 GVG). The validator refuses an
      application whose defendant does not name one.
      *We wrote empty records.* A natural person got an empty second name record, a company likewise
      where its designation fitted the first two fields. No reference file contains a single one.
      An empty record is not free: the trailer counts it.
      *Confirmed correct*, which is the larger half: the offsetting lands exactly in `VV2300MBET` of
      C10 as we write it, likewise `VORSTM`; the ancillary claims hit their own areas (C28 `VPBET`,
      C29 `MAHNK`, C33 `VV2300BET`); catalogued claim, running interest, party records and
      salutation keys agree field for field — including the pair that had caught us out before, a
      GmbH & Co. KG against an AG & Co. KG.
      *Left open:* `ZIRGBET` of C26 carries the amount interest is charged on where it differs from
      the claim ("nur wenn nicht identisch mit vorausgehendem Anspruch"), which we cannot express at
      all — interest on part of a claim. And `TKEZI` of the file header can be alphanumeric, as the
      Online-Mahnantrag's own `SAH00003` shows, while our layout declares it numeric; that matters
      only for reading other people's files
- [x] 3.21a Conformance of what we *produce*, not only of what we read. `EdaReferenceFileTest`
      checks that we can read the courts' files; `EdaConformanceTest` builds the same cases from the
      facts in `reference/README.md` and compares the sequence of record areas against the file the
      court produced. Not byte for byte — the file name, the day of creation, the software that
      wrote it and the Kennziffer in the header differ legitimately. The sequence is where the
      defects were: a record missing, one too many, one in the wrong place. Seven cases are covered:
      01 (natural person, catalogue claim), 02 (applicant GmbH with representative), 03 and 03b (the
      two-step chain against a GmbH & Co. KG and an AG & Co. KG), 04 (two joint debtors, which pins
      the order party → its litigation court → its chain → next party), 05 (a claim the catalogue
      does not cover, written over two records) and 10 (ancillary claims).
      *What case 10 found:* `EdaAncillaryClaimMapper` was complete and **never called**. The builder
      passed every claim to `EdaClaimMapper`, so C28, C29 and C33 were missing from every application
      that had a postage item, a reminder charge or a pre-court lawyer's fee — which is most of them.
      The builder now separates them out and writes them after the main claims, sorted by record
      area, because the order the firm entered them in the ledger is not the order the court reads.
      Our own tests could not have found this: they encoded our own assumption that the claims list
      held main claims only.
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
