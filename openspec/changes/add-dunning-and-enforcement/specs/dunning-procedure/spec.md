## ADDED Requirements

### Requirement: Pre-Court Reminder Stages

The system SHALL support a pre-court reminder cycle (außergerichtliches Mahnwesen) on a claim
ledger with configurable stages (e.g. Zahlungserinnerung, 1. Mahnung, 2. Mahnung, letzte Mahnung
vor gerichtlichen Schritten). Each stage SHALL define a document template, a payment period, an
optional reminder charge and whether it puts the debtor in default (Verzug). Sending a stage SHALL
generate the reminder document from the ledger data, record the stage with its date and deadline
on the ledger, create a follow-up for the deadline, and — where a charge is configured — book that
charge into the ledger as a pre-court cost.

#### Scenario: Escalating to the next stage

- **WHEN** the payment period of stage "1. Mahnung" expires without a payment covering the open
  amount
- **THEN** the ledger's dunning status shows the expired stage as overdue
- **AND** the user is offered the next configured stage with the document pre-filled

#### Scenario: Reminder charge booked

- **WHEN** a stage with a reminder charge of 5.00 EUR is sent
- **THEN** 5.00 EUR is booked into the ledger as a pre-court cost of the creditor
- **AND** the booking is linked to that reminder stage

### Requirement: Default Interest and Late Payment Charges

For a claim in default the system SHALL propose default interest under § 288 BGB, using the base
rate stored in `interest_base` plus 5 percentage points for consumer transactions and plus 9
percentage points where no consumer is involved, with the debtor category taken from the ledger
and freely overridable. Where the creditor is entitled to it, the system SHALL offer the lump sum
of § 288 Abs. 5 BGB as a pre-court cost position, SHALL NOT propose it when the debtor is a
consumer, and SHALL note that the lump sum is set off against costs of legal prosecution.

#### Scenario: B2B default interest proposed

- **WHEN** an interest rule is created for a main claim of a ledger whose debtor is not a consumer
- **THEN** the proposed rule is "base rate + 9 percentage points" from the date of default
- **AND** the user can change type, margin and start date before saving

#### Scenario: Lump sum not offered against consumers

- **WHEN** the debtor is flagged as a consumer
- **THEN** the § 288 Abs. 5 BGB lump sum is not proposed
- **AND** the reason is shown to the user

### Requirement: Court Dunning Case Record

The system SHALL record a court dunning procedure (gerichtliches Mahnverfahren) as an object
attached to a claim ledger, holding: the competent dunning court, the court's file number
(Geschäftsnummer), the applicant's identification number (Kennziffer) used for the application,
the applicant and defendant derived from the ledger parties, the amounts applied for as a snapshot
at application time, and a status with its date. The status SHALL be one of: prepared,
Mahnbescheid applied for, Mahnbescheid issued, Mahnbescheid served, Widerspruch filed (full or
partial), Vollstreckungsbescheid applied for, Vollstreckungsbescheid issued,
Vollstreckungsbescheid served, Einspruch filed, referred to the litigation court (Abgabe),
withdrawn, or completed with a final title. Status changes SHALL be journalled with date, user
and source (manual or imported court message).

#### Scenario: Status change journalled

- **WHEN** the user records that the Mahnbescheid was served on 2026-04-08
- **THEN** the dunning case status becomes "Mahnbescheid served" with that date
- **AND** the previous status, the new status, the user and the source are recorded in the history

#### Scenario: Partial objection

- **WHEN** a Widerspruch against part of the claim is recorded with the contested amount
- **THEN** the dunning case shows the contested and the uncontested amount
- **AND** an application for a Vollstreckungsbescheid over the uncontested amount remains possible

### Requirement: Competent Dunning Court Determination

The system SHALL determine the competent central dunning court (zentrales Mahngericht) from a
maintainable table that maps the relevant federal state to the court, following § 689 Abs. 2 and
Abs. 3 ZPO — by default the court for the applicant's general venue, and for applicants without a
general venue in Germany or in the constellations covered by the table the court designated there.
The determined court SHALL always be overridable by the user, and the table SHALL be editable by
an administrator without a software update.

#### Scenario: Court proposed from the creditor's seat

- **WHEN** a dunning case is created for a creditor whose seat is in Bavaria
- **THEN** the dunning court configured for Bavaria is proposed
- **AND** the user can select a different court from the table

#### Scenario: Table maintained by an administrator

- **WHEN** an administrator changes the court assigned to a federal state
- **THEN** subsequently created dunning cases use the changed assignment
- **AND** existing dunning cases keep the court they were created with

### Requirement: Statutory Deadline Automation

Recording a procedural date on a dunning case SHALL create the resulting statutory deadlines and
follow-ups as case events, with configurable lead times and assignees, marked so they can be
identified as generated by the dunning case. At least: on service of the Mahnbescheid a
two-week objection period (§ 692 Abs. 1 Nr. 3 ZPO) and a follow-up to apply for the
Vollstreckungsbescheid after it expires; a follow-up before the Mahnbescheid lapses six months
after service (§ 701 ZPO); on service of the Vollstreckungsbescheid a two-week Einspruch period
(§ 700 Abs. 1, § 339 ZPO); on a Widerspruch a follow-up for the statement of claim or the referral
to the litigation court. When the underlying date is corrected or the status changes, the
generated events SHALL be updated or closed accordingly.

#### Scenario: Deadlines created on service

- **WHEN** the service date of the Mahnbescheid is recorded as 2026-04-08
- **THEN** a deadline for the end of the objection period on 2026-04-22 is created
- **AND** a follow-up for the Vollstreckungsbescheid application is created for the next working
  day after it
- **AND** a follow-up before the six-month lapse on 2026-10-08 is created

#### Scenario: Correction propagates

- **WHEN** the service date is corrected to 2026-04-10
- **THEN** the generated deadlines are recalculated from the corrected date
- **AND** deadlines already marked as done are not silently changed but reported to the user

### Requirement: Fee and Court Cost Booking for the Dunning Procedure

Applying for a Mahnbescheid or a Vollstreckungsbescheid SHALL propose the associated fees
calculated from the ledger's claim value at the application date and book them into the ledger
through the procedural cost booking service: the 1.0 procedural fee of Nr. 3305 VV RVG, the 0.5
fee of Nr. 3308 VV RVG, in each case with the Nr. 1008 VV RVG increase for several creditors and
with the flat-rate expenses and VAT treatment of the ledger, and the court fee of Nr. 1100 KV GKG.
Where a business fee under Nr. 2300 VV RVG was already booked, the credit under Vorbem. 3 Abs. 4
VV RVG SHALL be applied and shown. All proposed amounts SHALL be editable before booking, and the
fee tables SHALL be maintainable as data rather than hard-coded.

#### Scenario: Fees proposed for the dunning application

- **WHEN** the user applies for a Mahnbescheid over a claim value of 5,000.00 EUR
- **THEN** the Nr. 3305 VV RVG fee, the flat-rate expenses, VAT and the Nr. 1100 KV GKG court fee
  are proposed for that value
- **AND** each amount can be changed before it is booked

#### Scenario: Credit of the business fee

- **WHEN** a Nr. 2300 VV RVG fee was booked for the same matter before the application
- **THEN** the proposal shows the credit under Vorbem. 3 Abs. 4 VV RVG as a separate line
- **AND** the booked amount is reduced by the credit

### Requirement: Hand-Off of the Application Data to the EDA Export

The dunning case SHALL assemble the complete application data set — parties and their
representatives, Kennziffer, court, main claims with their designation, interest rules including
"ab Zustellung", other ancillary claims from the pre-court costs, and the requested costs — and
SHALL hand it to the XJustiz/EDA export capability instead of requiring re-entry. Before hand-off
the system SHALL validate the data set and report every missing or inconsistent element in one
list. The resulting export file SHALL be stored in the case, linked to the dunning case, and made
available to the existing dispatch channels (beA, E-Post, print); this change SHALL NOT implement
a transmission path of its own.

#### Scenario: Validation before export

- **WHEN** an export is requested while the debtor has no complete postal address
- **THEN** the export is refused and the missing address is listed as a validation issue
- **AND** no file is created

#### Scenario: Export linked to the dunning case

- **WHEN** the export succeeds
- **THEN** the generated file is stored in the case and linked to the dunning case
- **AND** the dunning case status changes to "Mahnbescheid applied for" with the export date

### Requirement: Import of Court Response Messages

The system SHALL import the electronic response messages of the dunning court (Nachrichten über
Antragserledigungen: costs, service or non-service, objection, response to an objection, referral)
from a file supplied by the user or received through the existing message channels. An imported
message SHALL be matched to its dunning case by court file number and Kennziffer, SHALL update the
status and the relevant dates, SHALL trigger the resulting deadlines, and SHALL book the service
date into the ledger where interest starts on service. Messages that cannot be matched SHALL be
listed for manual assignment and SHALL never be silently discarded.

#### Scenario: Service message processed

- **WHEN** a service message for a known court file number is imported
- **THEN** the dunning case status becomes "Mahnbescheid served" with the date from the message
- **AND** the objection deadline and the follow-ups are created
- **AND** the ledger records the service booking for components with interest "ab Zustellung"

#### Scenario: Unmatched message

- **WHEN** a message references a file number that no dunning case carries
- **THEN** the message is stored in an inbox list with its content readable
- **AND** the user can assign it to a dunning case manually

### Requirement: Dunning Worklist

The system SHALL provide a list of all dunning cases the user may access, showing case, parties,
court, file number, status, the date of the last status change, the next deadline and the amount,
filterable by status, court, responsible user and overdue deadlines, with CSV export and direct
navigation to the case, the ledger and the dunning case.

#### Scenario: Overdue applications

- **WHEN** a user filters the worklist for dunning cases whose objection period has expired and
  that have no Vollstreckungsbescheid application yet
- **THEN** exactly those dunning cases are listed, ordered by the expiry date
- **AND** the user can start the Vollstreckungsbescheid application from the list

### Requirement: Dunning REST API

The REST API (current version) SHALL expose dunning cases of a case or ledger with their status,
dates and amounts, SHALL allow recording a status change with its date, and SHALL allow triggering
the assembly and export of the application data set. Endpoints SHALL follow the existing
authentication and error envelope conventions and SHALL appear in the generated swagger
specification.

#### Scenario: Status recorded through REST

- **WHEN** a client posts the status "Mahnbescheid served" with date 2026-04-08 to a dunning case
- **THEN** the status is applied exactly as it would be in the desktop client, including deadlines
- **AND** the response returns the updated dunning case
