## ADDED Requirements

### Requirement: Transaction List Review
After a bank statement CSV has been loaded with a selected CSV configuration, the client SHALL
show all transactions at once in a scrollable list, one entry per transaction. Each entry
SHALL show the booking date, counterparty name and IBAN, amount (colored by sign), purpose,
the matched case including the reason for the match, the processing status, and a summary of
the proposed actions. Each entry SHALL be expandable to edit the case and the actions.

#### Scenario: Statement loaded
- **WHEN** the user loads a CSV file with 40 valid transactions
- **THEN** the list SHALL contain 40 entries without requiring step-by-step navigation
- **AND** the window SHALL show the counts of all, included, already booked and unmatched transactions

#### Scenario: Large statement stays responsive
- **WHEN** the user loads a statement with more than 100 transactions
- **THEN** matching SHALL run in the background with visible progress
- **AND** entries SHALL be added to the list as they are matched

#### Scenario: Filtering the list
- **WHEN** the user selects the filter "ohne Zuordnung"
- **THEN** only entries without a matched case SHALL be visible

### Requirement: Automatic Action Proposals
For every transaction the client SHALL determine a case and propose actions using the existing
matching rules, in this order: invoice or payment number contained in the purpose, fuzzy
(Jaro-Winkler > 0.9) invoice or payment number, case file number contained in the purpose,
and a unique active case of the contact owning the sender IBAN. When a case is matched, a case
account entry SHALL be proposed, typed as earnings for incoming and spendings for outgoing
amounts, with the recipient taken from the matched invoice or payment. Marking an invoice as
paid or a payment as executed SHALL be offered but not pre-selected. Entries with at least one
proposed action SHALL be included by default.

#### Scenario: Invoice number in purpose
- **WHEN** the purpose of an incoming transaction contains the number of an open invoice
- **THEN** the entry SHALL be assigned to the invoice's case
- **AND** a case account entry of type earnings referencing that invoice SHALL be proposed
- **AND** the entry SHALL be included

#### Scenario: No match
- **WHEN** no rule matches a transaction
- **THEN** the entry SHALL show the status "keine Zuordnung", propose no actions and not be included
- **AND** the user SHALL be able to assign a case manually, after which actions SHALL be proposed

### Requirement: Deferred Changes During Review
The client SHALL keep all edits of entries (case, actions, account entry details, recipient,
description, case tags) as pending proposals only. No data SHALL be written to the server
before the user confirms the batch execution.

#### Scenario: Tag marked during review
- **WHEN** the user marks a case tag in an entry and closes the window without executing
- **THEN** the tag SHALL NOT have been set on the case

#### Scenario: Closing with pending actions
- **WHEN** the user closes the window while included entries with unexecuted actions exist
- **THEN** the client SHALL ask for confirmation before discarding them

### Requirement: Batch Execution
The client SHALL provide one action that executes the actions of all included entries. Before
execution it SHALL warn about invalid entries (account entry without case) and conflicts (the
same invoice or payment selected in more than one entry), and it SHALL show a single
confirmation with the number of actions per type. Entries SHALL be processed one by one; a
failing entry SHALL NOT stop the processing of the remaining entries. For each entry with at
least one executed action, the transaction SHALL be recorded in the transaction log (expiry 550
days). After the run the client SHALL show a report of succeeded and failed entries.

#### Scenario: All entries succeed
- **WHEN** the user confirms the execution of 25 included entries and all succeed
- **THEN** all 25 entries SHALL show the status "erledigt" and no longer be editable
- **AND** the report SHALL state 25 successful entries

#### Scenario: One entry fails
- **WHEN** one of 25 included entries fails during execution
- **THEN** the other 24 entries SHALL be executed
- **AND** the failed entry SHALL show the status "Fehler" with the error message and remain included
- **AND** the user SHALL be able to execute it again

#### Scenario: Same invoice selected twice
- **WHEN** two included entries both mark the same invoice as paid
- **THEN** the client SHALL warn about the conflict before the confirmation

### Requirement: Duplicate Protection
Transactions already recorded in the transaction log SHALL be shown as already booked,
including the user and the date of processing. They SHALL NOT be included or editable.

#### Scenario: Statement loaded again after execution
- **WHEN** the user loads the same statement again after a batch execution
- **THEN** every previously executed transaction SHALL be shown as "bereits gebucht" and excluded
