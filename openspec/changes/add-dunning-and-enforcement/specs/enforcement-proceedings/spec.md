## ADDED Requirements

### Requirement: Enforcement Measure Record

The system SHALL record enforcement measures (Zwangsvollstreckungsmaßnahmen) attached to a claim
ledger and to one of its titles. A measure SHALL hold its type, the debtors it is directed
against, the addressee (bailiff, enforcement court, third-party debtor, land registry, other) —
courts and registries taken from the court directory by their scope — the date it was ordered, the
date it was dispatched, the outcome with its date, free-text notes, the documents generated for
it, the deadlines it created and the ledger bookings it caused. Outcomes
SHALL at least distinguish pending, successful, partially successful, unsuccessful (fruchtlos),
withdrawn and stayed.

#### Scenario: Measure created from the ledger

- **WHEN** a user starts a measure from a claim ledger with a complete title
- **THEN** parties, title data and amounts are taken from the ledger without re-entry
- **AND** the measure is listed in the case with its status "pending"

#### Scenario: Unsuccessful outcome recorded

- **WHEN** the bailiff reports an unsuccessful attachment
- **THEN** the measure outcome is recorded as unsuccessful with its date
- **AND** the system proposes the follow-up measures configured for that outcome

### Requirement: Enforcement Measure Catalog

The system SHALL provide a maintainable catalog of measure types covering at least: enforcement
warning (Vollstreckungsandrohung), bailiff order under the official form including its options
(Sachpfändung, gütliche Erledigung under § 802b ZPO with payment agreement, taking of the asset
disclosure under § 802c ZPO, arrest warrant application, service of the title), application for a
judicial search order, attachment and transfer order (PfÜB) for monetary claims and the separate
form for maintenance claims, wage attachment, bank account attachment, compulsory mortgage
(Zwangssicherungshypothek), referral to forced sale or forced administration, and inquiries
(residents' register, register of debtors, other information sources). Each catalog entry SHALL
define which official form it uses, which cost positions it proposes and which follow-ups it
creates.

#### Scenario: Catalog drives the measure

- **WHEN** the user selects the measure type "PfÜB (Geldforderung)"
- **THEN** the corresponding official form, cost proposal and follow-ups configured for that type
  are used
- **AND** measure types the firm does not use can be hidden by an administrator

### Requirement: Official ZVFV Form Generation

The system SHALL generate the officially prescribed enforcement forms (ZVFV, Anlagen 1 to 8) by
filling the published fillable PDF with data from the ledger, the parties, the title and the
measure. Filling SHALL address each form field by its name in the PDF's AcroForm, because the
official forms carry ordinary field names and empty default values; the placeholder substitution
used for the firm's own document templates, which matches on the content of a field's value, is
not applicable to them. The filling SHALL handle the widget types the forms actually use,
including checkboxes and radio groups, by setting the on-state value the field defines rather
than an arbitrary string, since the enforcement forms are largely tick-box forms. Mandatory fields
SHALL be validated before the form is produced and every missing entry SHALL be reported in one
list. The generated form SHALL be stored in the case, linked to the measure, and made available to
the existing dispatch channels (beA, E-Post, print). The system SHALL record which form version
was used.

#### Scenario: Bailiff order produced

- **WHEN** a bailiff order is generated for a ledger with a complete title
- **THEN** the filled form contains creditor, debtor, title data and the itemised claims
- **AND** the file is stored in the case and linked to the measure
- **AND** the form version used is recorded on the measure

#### Scenario: Tick boxes set from the measure options

- **WHEN** a bailiff order is generated with the options "gütliche Erledigung" and "Vermögens-
  auskunft" selected
- **THEN** exactly the corresponding check boxes carry their on-state value in the produced PDF
- **AND** the boxes for options that were not selected stay unset

#### Scenario: Missing mandatory entry

- **WHEN** a PfÜB is generated without a third-party debtor
- **THEN** generation is refused with the missing entries listed
- **AND** no document is stored

### Requirement: Form Template Versioning

Official forms SHALL be managed as administrable form templates with a name, the form's issue or
validity date, the PDF itself and a field mapping profile that maps the PDF's AcroForm field names
to data of the ledger, parties, title and measure, including the on-state values of check boxes
and radio groups. The system SHALL be able to list the field names an uploaded PDF contains, so a
mapping can be built against the actual form rather than against assumptions. An administrator
SHALL be able to upload a new form version and its mapping without a software update, and the
system SHALL use the version valid at the date the measure is created, warning the user when a
form is used outside its validity period.

#### Scenario: Field names read from the uploaded form

- **WHEN** an administrator uploads a form PDF
- **THEN** the system lists the AcroForm field names it contains with their type
- **AND** the mapping profile is built by assigning data to those names

#### Scenario: New form version activated

- **WHEN** an administrator uploads a form version valid from 2026-10-01 with its mapping
- **THEN** measures created from that date use the new version
- **AND** measures created earlier keep the version they were generated with

#### Scenario: Outdated form warning

- **WHEN** a measure would be generated from a form version whose validity has ended
- **THEN** the user is warned that the form is no longer prescribed
- **AND** generation continues only after an explicit confirmation

### Requirement: Claim Itemisation for Enforcement Forms

The system SHALL generate the claim itemisation required as an annex to the enforcement forms
(Anlagen 6 to 8) from the claim ledger, distinguishing the categories the forms require: titled
main claims with their interest basis and interest period, titled costs, further enforcement
costs, payments received, and the continuing interest note. Amounts SHALL be computed to the date
the measure is created and SHALL match the claim statement of the same ledger for the same date.

#### Scenario: Itemisation matches the statement

- **WHEN** the itemisation for a bailiff order dated 2026-09-15 is produced
- **THEN** its totals equal the claim statement of the same ledger for 2026-09-15
- **AND** the continuing interest is stated with its basis, rate and start date

#### Scenario: Maintenance form uses the maintenance itemisation

- **WHEN** a PfÜB for statutory maintenance claims is generated
- **THEN** the itemisation for maintenance claims is used
- **AND** current and arrears maintenance are shown separately

### Requirement: Third-Party Debtor Handling

The system SHALL record third-party debtors (Drittschuldner) for attachment measures with their
contact data, the attached claim (employment income, bank account, other), and the state of the
declaration under § 840 ZPO. Serving an attachment order on a third-party debtor SHALL create the
two-week deadline for the declaration and a follow-up when it does not arrive. Payments received
from a third-party debtor SHALL be bookable into the ledger as payments with the third-party
debtor recorded as the payer.

#### Scenario: Declaration deadline monitored

- **WHEN** service of the attachment order on the third-party debtor is recorded
- **THEN** a deadline for the § 840 ZPO declaration two weeks later is created
- **AND** a follow-up is created when the declaration has not been recorded by then

#### Scenario: Third-party payment booked

- **WHEN** the employer transfers 320.00 EUR on the attached wage claim
- **THEN** the amount is booked as a payment and allocated by the ledger's allocation mode
- **AND** the booking records the third-party debtor as payer

### Requirement: Enforcement Cost Handling

Costs caused by an enforcement measure SHALL be bookable into the claim ledger as costs of
enforcement recoverable under § 788 ZPO: bailiff costs under the GvKostG, court fees, the 0.3
procedural fee of Nr. 3309 VV RVG and the 0.3 fee of Nr. 3310 VV RVG including the flat-rate
expenses and VAT treatment of the ledger, and other outlays. The proposal SHALL be computed from
the ledger's value at the date of the measure and SHALL remain editable. Costs SHALL be marked as
owed jointly by all debtors by default and MAY be assigned to a single debtor, and amounts
advanced by the firm SHALL optionally create a matching case account entry.

#### Scenario: Fees proposed for a measure

- **WHEN** a bailiff order is created for a ledger with an open total of 4,200.00 EUR
- **THEN** the Nr. 3309 VV RVG fee for that value plus expenses and VAT is proposed together with
  the expected bailiff costs
- **AND** the user can adjust every position before booking

#### Scenario: Single-debtor enforcement cost

- **WHEN** a measure is directed against debtor 2 only and the user marks its costs accordingly
- **THEN** the costs are booked as owed by debtor 2 alone
- **AND** the per-debtor totals of the ledger reflect that

### Requirement: Measure Deadlines and Re-Attempts

Enforcement measures SHALL create the follow-ups their type defines — for example a follow-up for
the bailiff's report, for the service of the attachment order, for the third-party debtor's
declaration, or for a renewed asset disclosure once the statutory waiting period of § 802d ZPO has
passed. Follow-ups SHALL be created as case events assigned to a configurable user, SHALL be
closed when the awaited event is recorded, and SHALL be visible both on the measure and in the
firm's normal follow-up views.

#### Scenario: Report follow-up closed by the outcome

- **WHEN** the outcome of a bailiff order is recorded
- **THEN** the follow-up awaiting the bailiff's report is closed
- **AND** the closing is traceable to the measure

#### Scenario: Renewed asset disclosure scheduled

- **WHEN** an asset disclosure was taken on 2026-05-04
- **THEN** a follow-up for a renewed disclosure is created for the date on which a new one may be
  demanded under § 802d ZPO
- **AND** the follow-up names the reason

### Requirement: Payment Agreement and Suspension of Enforcement

The system SHALL record a payment agreement with the debtor (Zahlungsvereinbarung, in particular
under § 802b ZPO) with its installments, its start and its conditions, SHALL link it to the
installment plan of the ledger, and SHALL mark the affected measures as stayed for its duration.
When an installment is missed, the system SHALL report the breach on the ledger and the measures
and SHALL offer to resume enforcement.

#### Scenario: Enforcement stayed

- **WHEN** a payment agreement over 12 monthly installments is recorded
- **THEN** the running measures are marked as stayed with the agreement as the reason
- **AND** the installment due dates are monitored

#### Scenario: Breach resumes enforcement

- **WHEN** an installment is missed beyond the grace period
- **THEN** the breach is reported on the ledger and on the stayed measures
- **AND** the user is offered to end the stay and continue enforcement

### Requirement: Enforcement Portfolio Views

The system SHALL provide a list of enforcement measures across all cases the user may access,
showing case, debtor, title, measure type, date, status, outcome and open amount, filterable at
least by measure type, status, outcome, responsible user and date range, with CSV export and
navigation into case, ledger and measure. It SHALL also provide a title overview showing every
title with its open amount, its last measure and its limitation date.

#### Scenario: Titles without recent activity

- **WHEN** a user filters titles for those without a measure in the last 12 months and an open
  amount above zero
- **THEN** exactly those titles are listed with their last measure date
- **AND** a measure can be started for a listed title directly

### Requirement: Enforcement REST API

The REST API (current version) SHALL expose enforcement measures and titles of a case, SHALL
allow creating a measure with its type, debtors and addressee, recording its outcome, and
requesting the generation of its official form. Endpoints SHALL follow the existing authentication
and error envelope conventions and SHALL appear in the generated swagger specification.

#### Scenario: Measure created through REST

- **WHEN** a client posts a bailiff order for a ledger with a complete title
- **THEN** the measure is created with the same validations and cost proposals as in the desktop
  client
- **AND** the response contains the measure including the identifiers of the documents produced
