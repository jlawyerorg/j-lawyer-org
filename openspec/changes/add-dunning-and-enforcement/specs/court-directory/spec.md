## ADDED Requirements

### Requirement: Court Master Data

The system SHALL maintain courts and comparable judicial bodies as central master data, so that
every feature referring to a court uses one record instead of free text. A court record SHALL hold
the official name in the spelling the judiciary uses, an optional additional designation (e.g.
"Zentrales Mahngericht"), the XJustiz court identifier from the code list
`urn:xoev-de:xjustiz:codeliste:gds.gerichte`, the postal address and, where used, the Postfach
address, phone, fax and web address, the electronic recipient identifier used for transmission
(EGVP/beA SAFE-ID), optional bank details for court fees, a validity range, an active flag and a
free-text note. The XJustiz identifier SHALL be unique among active courts and SHALL be the key
used whenever a court has to be identified towards the judiciary.

#### Scenario: Court used instead of free text

- **WHEN** a feature needs a court — a dunning application, an enforcement application, a document
  placeholder — 
- **THEN** it references a court record and takes name, address and identifiers from it
- **AND** no feature stores the court's address redundantly

#### Scenario: Renamed or merged court

- **WHEN** a court is superseded and its validity ends
- **THEN** it is no longer offered for new records
- **AND** records that already reference it keep resolving to it

### Requirement: Court Scopes

A court record SHALL carry one or more scopes describing what the court is used for, at least:
dunning court (Mahngericht), enforcement court (Vollstreckungsgericht), litigation court
(Prozessgericht), insolvency court, land registry, labour court. Scopes SHALL drive which courts a
selection field offers, and a court SHALL be selectable for a given purpose only if it carries the
matching scope, unless the user explicitly widens the selection.

#### Scenario: Selection limited by scope

- **WHEN** the user picks the court for an attachment and transfer order
- **THEN** only courts with the enforcement scope are offered by default
- **AND** the user can widen the selection to all courts if the required one is not tagged yet

#### Scenario: One court with several scopes

- **WHEN** an Amtsgericht acts both as an enforcement court and as a litigation court
- **THEN** the single court record carries both scopes
- **AND** it appears in both selection fields

### Requirement: Court Administration and Seed Data

Courts and their scopes SHALL be maintainable by an administrator — created, changed, deactivated —
without a software update. The system SHALL ship a seed data set containing the central dunning
courts with their scope, XJustiz identifier and address, and SHALL not require a complete
directory of all German courts; administrators SHALL be able to add the courts their firm works
with. Seeding SHALL be repeatable without duplicating existing records, matching on the XJustiz
identifier.

#### Scenario: Administrator adds a court

- **WHEN** an administrator creates a court with its identifier, address and scopes
- **THEN** it is immediately available in the matching selection fields
- **AND** no restart or software update is required

#### Scenario: Seed does not duplicate

- **WHEN** the seed data set is applied to an installation that already holds a seeded court
- **THEN** the existing record is kept or updated in place
- **AND** no second record with the same XJustiz identifier is created

### Requirement: Court References in Procedural Records

Procedural records SHALL reference courts from the directory rather than copying them: the dunning
case references its dunning court, an enforcement measure references the enforcement court or the
court addressed by an application, and a case may reference the litigation court together with its
file number. Where a document has already been produced, the record SHALL additionally keep the
court designation as it was used at that time, so an outgoing document stays reconstructable even
if the court record is later changed.

#### Scenario: Court change does not rewrite history

- **WHEN** a court's address is corrected after an application was produced for it
- **THEN** the produced document keeps the designation and address it was generated with
- **AND** new documents use the corrected data

#### Scenario: Case with litigation court

- **WHEN** a case records its litigation court and file number
- **THEN** the court is a reference into the directory
- **AND** document placeholders resolve the court's name and address from it
