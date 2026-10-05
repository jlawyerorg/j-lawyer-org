# Change: Bank statement import as a reviewable list with batch execution

## Why

The bank statement import (`Finanzen → Kontoauszug importieren`, `ImportBankStatementFrame`)
is a wizard with one step per CSV transaction. Navigating to the next or previous step
executes the actions of the current transaction immediately, each time after a yes/no
confirmation. Case tags are even written to the case the moment they are clicked. For a
statement with 50+ transactions this means 50+ steps and 50+ confirmations, no overview of
what was matched, and no way to correct a decision once the user has moved on.

## What Changes

- Replace the step-by-step wizard with a **scrollable list containing one panel per
  transaction**. Each panel shows the date, counterparty/IBAN, amount, purpose, matched case
  (and why it was matched), the processing status, and a one-line summary of the proposed
  actions. The panel can be expanded to edit the case, the actions, the account entry details
  and the case tags.
- The automatic matching (invoice/payment number exact and fuzzy, file number in purpose,
  unique case by IBAN, duplicate check via transaction log) is unchanged in behaviour. It runs
  for all transactions in the background after loading the CSV, and the panels appear as
  results arrive.
- **Nothing is written to the server while reviewing**. This also applies to case tags, which
  are now marked and applied on confirmation.
- A single **"Alle Buchungen ausführen"** action validates all included transactions, shows a
  confirmation with counts per action type, and then executes all of them. Each transaction is
  processed independently: a failure does not stop the run. A final report lists successes
  and failures, and failed transactions stay included so they can be run again.
- Filtering (all / with actions / unmatched / already booked / failed) and bulk
  select/deselect.
- The account entry type is chosen with a combo box (filtered by the sign of the amount)
  instead of six radio buttons.

No server, REST API or database changes are needed. The existing remote methods are reused.

## Impact

- Affected specs: new capability `bank-statement-import`
- Affected code (client only, package `com.jdimension.jlawyer.client.editors.finance`):
  - `ImportBankStatementFrame.java` / `.form` (rebuilt)
  - new: `BankTransactionProposal`, `BankTransactionMatcher`, `BankTransactionProcessor`,
    `BankTransactionEntryPanel` (`.java` + `.form`), `BankTransactionListPanel`
