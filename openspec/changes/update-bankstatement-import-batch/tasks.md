## 1. Model and logic (no UI)

- [x] 1.1 Create `BankTransactionProposal`: transaction, included flag, duplicate log entry,
      matched case + match reason, invoice/payment actions, account entry (type enum, invoice,
      recipient, description), tags to set/remove, lazily loaded case details, status
      (OPEN/NO_MATCH/DUPLICATE/DONE/FAILED) + error message, `hasActions()`, `toSummary()`
- [x] 1.2 Create `BankTransactionMatcher` by moving the matching logic out of
      `ImportBankStatementFrame.loadTransaction()` (incl. `getUniqueCase`) without behaviour
      change; pre-fill proposals like `enableActions()`/`loadInvoicesForCase()` did; cache
      file-number and IBAN lookups per run; add `loadCaseDetails(proposal)`
- [x] 1.3 Create `BankTransactionProcessor` by moving `processActions()` (without dialogs),
      plus applying marked tags; write the transaction log when at least one action ran

## 2. UI

- [x] 2.1 Create `BankTransactionEntryPanel` (`.java` + `.form`): header row, purpose/case
      row, action summary, expandable detail area; changes are written to the proposal only
- [x] 2.2 Rebuild `ImportBankStatementFrame` (`.java` + `.form`): remove the wizard
      components; add filter, select all/none, a scrollable list (width-tracking `Scrollable`
      panel), counters and the "Alle Buchungen ausführen" button
- [x] 2.3 Run the matching in a `SwingWorker` with progress after the CSV has been parsed;
      add panels incrementally
- [x] 2.4 Batch execution: validation (account entry without case, invoice/payment selected
      more than once), confirmation with counts, `SwingWorker` execution with per-panel status,
      final report
- [x] 2.5 Ask before closing the window when included, unexecuted actions exist

## 3. Verification

- [ ] 3.1 Manual test with a mixed CSV (invoice no., payment no., file no., known IBAN,
      unmatched, already booked) and comparison with the previous matching results
- [ ] 3.2 Verify that reviewing/editing writes nothing (incl. tags) and that a reload after
      execution marks all executed transactions as already booked
- [ ] 3.3 Verify error handling (one failing transaction, others succeed) and responsiveness
      with 100+ transactions
- [ ] 3.4 Open both `.form` files in the NetBeans GUI builder
