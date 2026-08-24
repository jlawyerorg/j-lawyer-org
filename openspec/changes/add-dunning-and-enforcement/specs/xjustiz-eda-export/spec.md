## ADDED Requirements

### Requirement: XJustiz EDA Export of the Dunning Application

The system SHALL export the application data set of a court dunning case as an XJustiz-compliant
EDA file conforming to the XJustiz "Mahn" schema shipped with the server
(`j-lawyer-server-common/xjustiz`, currently `xjustiz_0600_mahn_3_3.xsd` of XJustiz 3.5.1). The
export SHALL be available from the desktop client on a claim ledger with a dunning case and from
the REST API, SHALL be generated from the stored ledger, party, title and dunning-case data
without re-entering it, and SHALL only produce a file after validation and schema validation have
passed.

#### Scenario: Export from the desktop client

- **GIVEN** a claim ledger with at least one main claim, a debtor with a complete address and a
  dunning case with court and Kennziffer
- **WHEN** the user triggers the XJustiz export in the ledger workspace
- **THEN** the system generates the EDA file, validates it against the schema and stores it in the
  case
- **AND** offers to save a copy to disk and reports where the file was stored

#### Scenario: Export through REST

- **WHEN** a client requests the XJustiz export for a dunning case through the REST API
- **THEN** the response carries the generated XML with content type `application/xml` and a
  filename that identifies case and date
- **AND** the same file is stored as a case document, as in the desktop client

### Requirement: Pre-Export Validation of the Application Data

Before generating an EDA file the system SHALL validate the application data and SHALL report
every problem in one list rather than failing on the first one. The validation SHALL cover at
least: at least one main claim with an amount greater than zero, a debtor with a complete postal
address, a creditor with a complete postal address, a determined dunning court, the Kennziffer of
the acting lawyer, and the consistency of interest rules that start on service. Validation SHALL
also be callable on its own, from the client and through REST, so a user can check readiness
without producing a file.

#### Scenario: Incomplete data reported in one list

- **WHEN** an export is validated for a ledger whose debtor has no postal code and whose dunning
  case has no Kennziffer
- **THEN** both problems are reported together with the field they refer to
- **AND** no file is generated

#### Scenario: Readiness check without export

- **WHEN** the user or a REST client requests validation only
- **THEN** the system returns the list of open issues, empty if the data set is complete
- **AND** nothing is stored or changed

### Requirement: Mapping of Ledger Data to the Dunning Schema

The export SHALL map the claim ledger to the schema's claim structures: each main claim with its
amount, its designation and its due date; interest per main claim with its type (fixed rate or
base rate plus margin), its rate, its start date and, where interest starts on service, the
schema's representation for interest from service of the dunning order; interest-bearing and
non-interest-bearing costs and pre-court costs of the creditor as the corresponding ancillary
claims; and payments already received as a reduction of the amounts applied for. Monetary amounts
SHALL be exported with two decimal places and an explicit currency, and dates in the schema's date
format. The mapping SHALL be documented in the code and covered by tests that assert the produced
document against the shipped XSD; element and attribute names SHALL be taken from the schema, not
from assumptions.

#### Scenario: Main claim with base-rate interest

- **GIVEN** a main claim of 5,000.00 EUR designated "Kaufpreis Warenlieferung" with interest of 5
  percentage points above the base rate from 2026-01-01
- **WHEN** the EDA file is generated
- **THEN** the claim appears with its amount, designation and due date
- **AND** the interest appears with its type, margin and start date as the schema defines them

#### Scenario: Interest from service

- **GIVEN** a main claim whose interest starts on service of the dunning order
- **WHEN** the EDA file is generated
- **THEN** the interest is expressed as interest from service, not with a fixed start date

#### Scenario: Costs and ancillary claims

- **GIVEN** a ledger holding pre-court costs of the creditor and assessed costs
- **WHEN** the EDA file is generated
- **THEN** each cost position appears as an ancillary claim with its designation and amount
- **AND** the sum of the exported positions equals the ledger's cost totals at the export date

### Requirement: XJustiz Message Metadata

The generated document SHALL carry the message metadata the schema requires: message type, a
unique message identifier, the creation timestamp, the sending firm with its own file number, the
addressed dunning court, the applicant (creditor) with address, the applicant's legal
representative with the Kennziffer used, and the defendant (debtor) with address and legal form,
distinguishing natural persons from organisations. Firm data SHALL be taken from the server
configuration, party data from the ledger parties, and court and Kennziffer from the dunning case.

#### Scenario: Header identifies sender and court

- **WHEN** an EDA file is generated for a case with file number `2026/1234`
- **THEN** the message metadata contains the firm's own file number, a unique message identifier
  and the creation timestamp
- **AND** the addressed court is the dunning court stored on the dunning case

#### Scenario: Organisation as debtor

- **GIVEN** the debtor is an organisation
- **WHEN** the EDA file is generated
- **THEN** the debtor is exported as an organisation with its designation and address
- **AND** a natural person debtor is exported with first name, surname and address instead

### Requirement: Schema Validation of the Generated File

Every generated EDA file SHALL be validated against the shipped XJustiz XSD before it is stored or
returned. When validation fails, the system SHALL report the schema violations with their location
and SHALL NOT store or return the file. When the schema files are missing or unreadable, the system
SHALL report that as a configuration error and SHALL NOT fall back to an unvalidated export.

#### Scenario: Validation failure surfaces the violations

- **WHEN** the generated document violates the schema
- **THEN** the user is shown the violations including element and position
- **AND** no document is stored in the case

#### Scenario: Missing schema is a configuration error

- **WHEN** the XSD files cannot be read
- **THEN** the export fails with a message naming the missing schema
- **AND** no unvalidated file is produced

### Requirement: Export Confirmation Step

Before the export runs, the system SHALL show the data it will use — court, Kennziffer and acting
lawyer, creditor, debtor, and the amounts applied for — pre-filled from the dunning case and the
ledger, with the values the user may still change for this export. Fields that are required SHALL
be marked, an incomplete confirmation SHALL not start the export, and where exactly one candidate
exists (one debtor, one lawyer) it SHALL be preselected. Values changed here SHALL be written back
to the dunning case so the next export and the procedural record stay consistent.

#### Scenario: Pre-filled confirmation

- **WHEN** the export is started for a ledger with one debtor and one acting lawyer
- **THEN** debtor, lawyer, Kennziffer and court are preselected from the stored data
- **AND** the amounts applied for are shown as calculated for the export date

#### Scenario: Changed court written back

- **WHEN** the user selects a different dunning court in the confirmation step and exports
- **THEN** the file is generated for the selected court
- **AND** the dunning case records that court

### Requirement: Exported Files Stored as Case Documents

Every generated EDA file SHALL be stored as a document of the case, named through the document
naming rules, tagged so exports can be found by tag, and linked to the dunning case that produced
it. Re-exporting SHALL create an additional document rather than overwriting the previous one, and
all exports of a case SHALL remain listable with their creation date and the user who created
them.

#### Scenario: Export appears in the document list

- **WHEN** an export succeeds
- **THEN** the file appears in the case document list with its tag and creation date
- **AND** the dunning case links to that document

#### Scenario: Re-export keeps the history

- **WHEN** the user exports a second time after correcting an amount
- **THEN** both files exist in the case, distinguishable by date
- **AND** the dunning case links to the most recent one while the earlier one stays retrievable

### Requirement: EDA File Viewer

The document viewer SHALL display XJustiz EDA files in a readable form instead of raw markup only,
with a formatted view showing the parties, the court, the file number, the claims with their
amounts, interest and costs, and the totals, and a second view showing the raw XML. The viewer
SHALL open for EDA documents in the existing document viewer of the case panel, SHALL keep the
last selected view within a session, and SHALL show a malformed or non-EDA XML file as raw content
with an explanatory notice instead of failing.

#### Scenario: Formatted view of an export

- **WHEN** the user selects a stored EDA document
- **THEN** the viewer shows creditor, debtor, court, claims, interest, costs and totals in readable
  form
- **AND** the user can switch to the raw XML view

#### Scenario: Malformed file

- **WHEN** the selected file is not a parseable EDA document
- **THEN** the viewer shows the raw content with a notice that it could not be interpreted
- **AND** the document viewer does not fail

### Requirement: Lawyer Identification Number

A user account SHALL be able to store the identification number (Kennziffer) that identifies the
acting lawyer towards the dunning courts: one number used by default and, where a court issues and
requires its own, additional court-specific numbers. The fields SHALL be offered in user
administration for users flagged as lawyers and SHALL not be shown for other users. The export
SHALL use the court-specific number when the selected court is flagged as requiring its own, and
the default number otherwise; the dunning case SHALL record the number actually used. An export
SHALL be refused when no usable number exists for the selected court.

#### Scenario: Kennziffer maintained for a lawyer user

- **WHEN** an administrator edits a user flagged as a lawyer
- **THEN** the Kennziffer field is available, editable and persisted with the user
- **AND** the field is absent for users not flagged as lawyers

#### Scenario: Court requiring its own Kennziffer

- **GIVEN** the selected dunning court is flagged as requiring its own Kennziffer and the acting
  lawyer has one stored for that court
- **WHEN** the EDA file is generated
- **THEN** the court-specific number is used, not the default one
- **AND** the dunning case records which number was used

#### Scenario: Export refused without Kennziffer

- **WHEN** an export is started for a dunning case whose acting lawyer has no Kennziffer
- **THEN** the validation reports the missing Kennziffer
- **AND** no file is generated
