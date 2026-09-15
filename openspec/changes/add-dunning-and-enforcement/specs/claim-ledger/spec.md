## ADDED Requirements

### Requirement: Claim Ledger Parties

A claim ledger SHALL store its own creditor (Gläubiger) and debtor (Schuldner) lists. Each entry
SHALL reference the address book contact (`AddressBean`) that identifies the party, following the
reference the invoicing model already uses, and deleting that contact SHALL NOT delete the party or
the ledger. A party entry MAY additionally reference the case party record
(`ArchiveFileAddressesBean`) it was derived from, so the party can be proposed from the case and
the role it holds there stays visible; that reference SHALL be optional, because a ledger party
need not be a party of the case, and losing it SHALL NOT change who the ledger runs against.

Because an enforcement title stays enforceable for 30 years, a party entry SHALL additionally hold
a snapshot of the designation and postal address as they were used towards the court. The snapshot
SHALL be written when the party is first used in a dunning application, a title or an enforcement
document, SHALL NOT be overwritten by later changes to the contact, and SHALL be what documents
reproducing an earlier state resolve to. Current work SHALL use the referenced contact, so an
address correction takes effect without touching the history.

Each party entry SHALL carry a sequence number, an optional legal
representative and an optional authorised representative (Bevollmächtigter). The ledger SHALL
store the effective number of creditors separately from the number of creditor entries, because
one contact may represent several creditors (e.g. spouses under one address), and fee increases
under Nr. 1008 VV RVG SHALL be derived from that number. Debtors SHALL be liable jointly and
severally by default; a booking MAY be assigned to exactly one debtor and SHALL then be marked as
a single-debtor position.

#### Scenario: Ledger inherits case parties

- **WHEN** a claim ledger is created for a case that has a client and an opponent
- **THEN** the client is proposed as creditor and the opponent as debtor
- **AND** the proposed number of creditors equals the number of creditor entries

#### Scenario: Party outlives its contact

- **WHEN** the address book contact referenced by a debtor is deleted
- **THEN** the ledger, its bookings and the debtor entry remain
- **AND** the debtor stays identifiable by the snapshot taken when the title was obtained

#### Scenario: Address correction does not rewrite the title

- **GIVEN** a debtor against whom a Vollstreckungsbescheid was obtained under a former address
- **WHEN** the contact's address is corrected after the debtor has moved
- **THEN** new enforcement documents use the corrected address
- **AND** the designation and address recorded with the title stay as they were

#### Scenario: Single-debtor cost booking

- **WHEN** an enforcement cost of 25.00 EUR is booked against debtor 2 only
- **THEN** the ledger entry is marked as attributable to debtor 2
- **AND** the ledger totals show the joint total and, per debtor, the amount that debtor owes

#### Scenario: Fee increase for several creditors

- **WHEN** the ledger records 3 creditors and a Nr. 3305 VV RVG fee is calculated
- **THEN** the calculation applies the Nr. 1008 VV RVG increase for two additional creditors
- **AND** the increase is shown as a separate, editable part of the proposed fee

### Requirement: Enforcement Title Data

A claim ledger SHALL be able to store one or more enforcement titles (Titel). A title SHALL
record its type (Vollstreckungsbescheid, Urteil, Kostenfestsetzungsbeschluss, Prozessvergleich,
notarielle Urkunde, Anerkenntnis, other), the issuing body and its file number, the date of
issue, the date the enforceable copy (vollstreckbare Ausfertigung) with clause was obtained, the
date of service on the debtor, the debtors it runs against, and a free-text description of the
subject matter used as the short designation in enforcement documents. The system SHALL derive
from the title whether the three formal prerequisites of enforcement (Titel, Klausel, Zustellung)
are complete and SHALL refuse to generate enforcement documents from an incomplete title unless
the user explicitly overrides.

#### Scenario: Enforcement blocked without service

- **WHEN** a user starts an enforcement measure from a title whose service date is empty
- **THEN** the system reports the missing prerequisite (Zustellung)
- **AND** offers to continue only after an explicit confirmation, which is recorded on the measure

#### Scenario: Title limitation monitoring

- **WHEN** a title is saved with a date of issue
- **THEN** the system computes the 30-year limitation date under § 197 Abs. 1 Nr. 3 BGB
- **AND** creates a follow-up (Wiedervorlage) on the case at a configurable lead time before it

### Requirement: Sub-Ledgers

A claim ledger SHALL optionally reference a parent claim ledger, forming a sub-ledger
(Unterkonto). Sub-ledgers SHALL be used for claims that are titled separately, that run against a
subset of the debtors, or whose interest must be calculated separately. Totals, statements and
measures SHALL always be computed for exactly one ledger; aggregation over a parent and its
sub-ledgers SHALL be available as an explicit option in statements and lists.

#### Scenario: Sub-ledger for a claim against one debtor only

- **WHEN** a user creates a sub-ledger of an existing ledger and selects only debtor 1
- **THEN** the sub-ledger keeps its own components, bookings and totals
- **AND** the parent ledger's totals are unchanged

#### Scenario: Aggregated statement

- **WHEN** a claim statement is requested for a parent ledger with "include sub-ledgers" enabled
- **THEN** the statement lists every ledger separately and adds a combined total

### Requirement: Extended Claim Component Types

The claim component types SHALL be extended beyond `MAIN_CLAIM`, `COST_INTEREST_BEARING` and
`COST_NON_INTEREST_BEARING` by: the pre-court cost categories that the EDA dunning application
distinguishes as separate record areas — the creditor's outlays (Auslagen), reminder costs
(Mahnkosten), information costs (Auskunftskosten), bank return costs (Bankrücklastkosten),
collection costs (Inkassokosten), the pre-court lawyer's fee under Nr. 2300 VV RVG, and other
ancillary claims (andere Nebenforderungen) — assessed costs (festgesetzte Kosten) which are
interest-bearing under § 104 Abs. 1 S. 2 ZPO, interest arrears booked as a fixed amount
(Zinsforderung/Zinsrückstand), and recurring monthly main claims (laufende monatliche
Hauptforderung, e.g. maintenance) with a start month, an optional end month and an amount that is
posted as a due item per month. Keeping these categories apart is a requirement of the dunning
application, which will not accept them merged into one position. Converting non-interest-bearing
costs into assessed interest-bearing costs after a cost assessment order SHALL be supported as a
single operation that removes or reduces the original positions and records the conversion.

#### Scenario: Monthly maintenance claim

- **WHEN** a recurring monthly main claim of 350.00 EUR starting 2026-01 without end month exists
- **THEN** the ledger shows a due item of 350.00 EUR for every month up to the key date
- **AND** the totals designate the claim as continuing monthly

#### Scenario: Pre-court costs kept in their categories

- **WHEN** reminder charges of 10.00 EUR and a pre-court Nr. 2300 VV RVG fee of 480.20 EUR are
  booked on one ledger
- **THEN** each is stored under its own component type
- **AND** the dunning application can place them in the record areas the EDA format requires

#### Scenario: Conversion after a cost assessment order

- **WHEN** the user converts previously booked non-interest-bearing costs of 402.50 EUR into
  assessed costs with interest of 5 percentage points above the base rate from 2026-03-15
- **THEN** the original cost positions are removed or reduced accordingly
- **AND** a single assessed-cost component of 402.50 EUR with the given interest rule exists
- **AND** the conversion is recorded in the ledger history

### Requirement: Main Claim Classification

A main claim SHALL be classifiable by a number of the main claim catalogue the dunning courts
publish (Hauptforderungskatalog), or SHALL be marked as a free-text claim where the catalogue holds
no fitting entry. The catalogue SHALL be shipped as maintainable reference data — number,
designation and the additional entries the number requires — so it can be updated without a
software release, and the classification SHALL be offered wherever a main claim is created or
edited, with the free-text designation remaining available.

Where a catalogue number requires further data, the ledger SHALL hold it with the claim: the postal
code and place of the property for residential and condominium claims (catalogue numbers 17, 19,
20 and 90), the contract designation for damages from contract (28), and the account, meter or
service details the catalogue demands for the numbers that require them. The classification and
these additions SHALL be validated when a dunning application is prepared, because the EDA format
rejects a catalogued claim whose required additional record is missing.

#### Scenario: Claim classified by catalogue number

- **WHEN** a main claim "Kaufpreis Warenlieferung" is created and the matching catalogue number is
  selected
- **THEN** the number is stored with the claim alongside its designation
- **AND** the dunning application can write it as a catalogued claim

#### Scenario: Claim without a fitting catalogue entry

- **WHEN** no catalogue number fits the claim
- **THEN** the claim is marked as a free-text claim with its designation
- **AND** the dunning application writes it as a "sonstiger Anspruch"

#### Scenario: Catalogue number demanding additional data

- **WHEN** a main claim is classified with a catalogue number for a residential property claim
- **THEN** the postal code and place of the property are requested and stored with the claim
- **AND** preparing a dunning application without them reports the missing entry

### Requirement: Interest Starting on Service

A claim component SHALL be able to define its interest start as "on service of the dunning order"
(ab Zustellung) instead of a fixed date. Until the service date is known, interest on that
component SHALL be zero and the component SHALL be displayed with the pending marker. When the
service date is recorded — manually or by importing a court message — interest SHALL be
calculated from that date onwards and a ledger entry documenting the service SHALL be created.

#### Scenario: Interest activated by the service date

- **GIVEN** a main claim with interest of 5 percentage points above the base rate "ab Zustellung"
- **WHEN** the dunning order's service date 2026-04-08 is recorded
- **THEN** interest on that component is calculated from 2026-04-08
- **AND** a ledger entry "MB-Zustellung 08.04.2026" documents the change

### Requirement: Payment Allocation Modes

In addition to the statutory allocation under §§ 366 Abs. 2, 367 BGB the payment split SHALL
support: allocation under § 497 Abs. 3 BGB for consumer loans (costs of legal prosecution first,
then principal, then default interest), allocation to a named target (interest on costs, costs,
interest on the main claim, main claim), allocation to a single debtor's positions, and free
manual distribution across several main claims. The ledger SHALL store per ledger whether a
remaining surplus is re-allocated under §§ 366, 367 BGB or carried to the next position, and
every stored allocation SHALL keep the mode used and a warning flag when the distribution
deviates from the statutory order.

#### Scenario: Consumer loan allocation

- **WHEN** the ledger is flagged as a consumer loan and a payment of 200.00 EUR is booked
- **THEN** the payment is allocated to the costs of legal prosecution first, then to the
  principal, then to default interest
- **AND** the allocation records `§ 497 Abs. 3 BGB` as the mode used

#### Scenario: Manual distribution with deviation warning

- **WHEN** the user manually allocates a payment entirely to one main claim although another main
  claim carries a higher interest rate
- **THEN** the proposal is accepted and stored
- **AND** it is marked as deviating from § 366 Abs. 2 BGB with an explanatory warning text

### Requirement: Single Interest and Balance Calculation Path

Interest accrual, component balances and ledger totals SHALL be computed by exactly one
implementation, shared by the ledger totals, the payment split, the claim statement, the
enforcement itemisation and every export. The system SHALL NOT keep a second, simplified
calculation beside it, and SHALL NOT use a hard-coded base rate anywhere: the base rate SHALL
always be resolved from `interest_base` for the period being computed, splitting the period at
every base-rate and principal change. Where two implementations exist today, they SHALL be
consolidated before the statement and the itemisation are built on them.

#### Scenario: Payment split and totals agree

- **WHEN** a payment is allocated and the ledger totals are computed for the same key date
- **THEN** the interest amounts both use are identical
- **AND** both were derived from the base rate stored for that period, not from a constant

#### Scenario: Base rate change inside the interest period

- **GIVEN** a component bearing interest of 5 percentage points above the base rate across a date
  on which the base rate changes
- **WHEN** interest is computed to a key date after that change
- **THEN** the period is split at the change date and each part uses the rate valid for it
- **AND** the statement, the itemisation and the totals report the same amount

### Requirement: Claim Statement Document

The system SHALL generate a claim statement (Forderungsaufstellung) for a claim ledger to a
freely chosen key date. The statement SHALL contain the parties, the title data, every component
with its interest rule, every booking in chronological order, the interest computed per component
up to the key date, all payments with their allocation, the resulting balance split into main
claims, interest, interest-bearing and non-interest-bearing costs, and the continuing-interest
note ("weitere Zinsen aus … seit …"). The statement SHALL be producible as PDF and as an editable
document, SHALL be storable in the case file with a name from the document naming rules, and SHALL
be exportable as structured data (CSV/JSON) for creditors who process it further.

#### Scenario: Statement to a past key date

- **WHEN** a statement is requested with key date 2026-03-31 for a ledger holding bookings up to
  2026-08-01
- **THEN** only bookings up to 2026-03-31 are included and interest is computed to that date
- **AND** the balance shown equals the ledger totals for that date

#### Scenario: Statement filed in the case

- **WHEN** the user chooses to store the statement
- **THEN** the document is saved to the case and appears in the document list
- **AND** the ledger records that a statement was produced, with date and user

### Requirement: Balance List Across Ledgers

The system SHALL provide a balance list (Saldenliste) over all claim ledgers the user may access,
filterable at least by creditor, debtor, case, responsible lawyer, ledger status and open amount,
and by whether the case is archived. The list SHALL show per ledger the open main claim, accrued
interest, open costs and total, and SHALL support sorting, CSV export and opening the underlying
case or ledger.

#### Scenario: Portfolio view for one creditor

- **WHEN** a user filters the balance list by creditor "Muster GmbH"
- **THEN** all ledgers with that creditor are listed with their open totals as of today
- **AND** the list shows a sum row over the filtered rows

### Requirement: Procedural Cost Booking Service

The claim ledger SHALL expose a service operation used by the dunning and enforcement
capabilities to book a procedural fee or disbursement, taking the amount or the calculation
basis, the cost category (RVG fee, court fee, bailiff cost, other outlay), whether it is
interest-bearing, whether it is owed jointly or by a single debtor, the VAT treatment, and an
optional link to the originating dunning case or enforcement measure. The operation SHALL create
the ledger entry and MAY, on request, create a matching case account entry (Aktenkonto) for an
amount advanced by the firm. Bookings created this way SHALL be traceable back to their origin
and SHALL be removed or reversed when the originating measure is deleted or cancelled.

#### Scenario: Bailiff cost advanced by the firm

- **WHEN** a bailiff order books 32.50 EUR of bailiff costs with "advanced by the firm" set
- **THEN** the amount appears in the ledger as an enforcement cost owed jointly by all debtors
- **AND** a case account entry records the outlay
- **AND** the ledger entry references the enforcement measure that created it

#### Scenario: Reversal on measure cancellation

- **WHEN** an enforcement measure with booked costs is cancelled
- **THEN** the system offers to reverse the bookings it created
- **AND** on confirmation the reversal is recorded as an adjustment, not by deleting history

### Requirement: Installment Plan

The system SHALL compute and store an installment plan (Tilgungsplan) for a claim ledger, either
from a given installment amount (deriving the number of installments and the final installment)
or from a given number of installments (deriving the installment amount), taking continuing
interest into account and starting on a chosen date with a chosen interval. The plan SHALL be
producible as a document for the debtor, SHALL create follow-ups for the due dates, and SHALL
flag a missed installment when no matching payment was booked within a configurable grace period.

#### Scenario: Plan derived from the installment amount

- **WHEN** a plan of 100.00 EUR per month starting 2026-09-01 is computed for a ledger with an
  open total of 1,250.00 EUR and continuing interest
- **THEN** the plan lists every due date with its amount and the resulting remaining balance
- **AND** the final installment settles the remaining balance including interest accrued

#### Scenario: Missed installment detected

- **WHEN** an installment is due and no payment covering it was booked within the grace period
- **THEN** the plan marks that installment as missed
- **AND** a follow-up is created for the responsible user

### Requirement: Claim Ledger REST API

The REST API (current version) SHALL expose claim ledgers of a case, their components, bookings
and totals for a key date, and SHALL allow creating a payment booking with an allocation mode and
requesting a claim statement. Endpoints SHALL follow the existing authentication and error
envelope conventions and SHALL be covered by the generated swagger specification.

#### Scenario: Booking a payment through REST

- **WHEN** a client posts a payment of 150.00 EUR with allocation mode `LEGAL` to a ledger
- **THEN** the payment is booked and allocated as the desktop client would allocate it
- **AND** the response contains the resulting allocations and the new totals
