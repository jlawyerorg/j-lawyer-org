## ADDED Requirements

### Requirement: EDA File Format

The system SHALL read and write files in the record format of the automated court dunning
procedure ("Elektronischer Datenaustausch", EDA), as specified by the Oberlandesgericht Stuttgart
– IuK-Fachzentrum Justiz – in the EDA-Konditionen "Format 4 (Version 4.0.00)" and the associated
Satzbeschreibungen. The implementation SHALL observe the format's invariants: records of a fixed
length of 128 bytes with no record separator and no EOF character; the character set CP-850, with
umlauts and `ß` encoded as such and never transliterated to `ae`/`oe`/`ue`/`ss`; unused fields —
including numerically defined ones — filled with blanks; and every record beginning with a
nine-byte key of record type (2), section marker (5) and sequence number (2) followed by 119 bytes
of data. A logical file SHALL begin with a file header (record type `AA`), contain only records of
one record type, and end with a file trailer (record type `BB`); a physical file SHALL consist of
up to 220 logical files and SHALL NOT be split across transmissions.

#### Scenario: Record layout respected

- **WHEN** an EDA file is generated
- **THEN** every record is exactly 128 bytes long and carries no carriage return, line feed or EOF
  character
- **AND** unused fields are blank-filled rather than zero-filled

#### Scenario: Umlauts encoded in CP-850

- **GIVEN** a debtor named "Jürgen Müller-Straßer"
- **WHEN** the EDA file is generated
- **THEN** the name is written in CP-850 with the umlauts and `ß` encoded as single characters
- **AND** no transliteration to `ue`/`ss` occurs

#### Scenario: Logical file framing

- **WHEN** a file containing dunning applications is generated
- **THEN** it starts with an `AA` record naming the record type, the format version `4000` and the
  participant's Kennziffer, and ends with a `BB` record carrying the number of applications
- **AND** the data area between them holds records of that record type only

### Requirement: Export of the Dunning Application

The system SHALL export the application for a Mahnbescheid as an EDA file of record type `01`
(format 4.0.00) and the application for a Vollstreckungsbescheid as an EDA file of record type
`08` (format 4.1.00), generated from the stored ledger, party, title and dunning-case data without
re-entering it. The export SHALL be available from the desktop client on a claim ledger with a
dunning case and from the REST API, and SHALL produce a file only after the application data has
passed validation. The record type actually written SHALL be the one valid at the time of export
according to the format table published by the dunning courts, and the version used SHALL be
recorded on the dunning case.

#### Scenario: Export from the desktop client

- **GIVEN** a claim ledger with at least one main claim, a debtor with a complete address and a
  dunning case with court and Kennziffer
- **WHEN** the user triggers the EDA export in the ledger workspace
- **THEN** the system generates the record type `01` file and stores it in the case
- **AND** offers to save a copy to disk and reports where the file was stored

#### Scenario: Export through REST

- **WHEN** a client requests the EDA export for a dunning case through the REST API
- **THEN** the response carries the generated file as a binary octet stream with a filename that
  identifies case and date
- **AND** the same file is stored as a case document, as in the desktop client

#### Scenario: Vollstreckungsbescheid application

- **WHEN** the user applies for a Vollstreckungsbescheid on a dunning case whose Mahnbescheid was
  served and whose objection period expired
- **THEN** a record type `08` file is generated carrying the court's Gerichtsnummer from the
  dunning case
- **AND** the file is stored and linked like the Mahnbescheid application

### Requirement: Mapping of Ledger Data to the EDA Application Records

The export SHALL map the claim ledger onto the record areas the dunning application defines,
using the field names and value domains of the Satzbeschreibung rather than assumptions:

- the key record (`KS`) with the participant's own reference (`TGZ`), the postal code and place of
  the dunning court, the Gesamtschuldner marker when several debtors are jointly and severally
  liable, and the court's Geschäftsnummer only when an application is repeated;
- creditors (`AS`) and debtors (`AG`) with their legal representatives (`ASGV`, `AGGV`), each
  representative following the party it belongs to, and the acting lawyer (`ASPV`);
- main claims either as catalogued claims (`ASPK`) carrying a number of the AUGEMA main claim
  catalogue, or as free-text claims (`ASPS`, "sonstiger Anspruch");
- interest per claim as running interest (`ZINS`) with the rate, the rate marker `F` (fixed) or
  `B` (above the ECB base rate), the period marker (annual, monthly, daily) and the optional
  start and end dates, or as already computed interest (`ZIAUS`), which may occur only once per
  application and must precede the first main claim;
- consumer-credit data (`VKG`) where the claim arises from a consumer loan under §§ 491–504 BGB;
- ancillary claims in the distinct areas the format defines — the creditor's outlays (`AUSL`),
  reminder costs (`MAHNK`), information costs (`AUSK`), bank return costs (`BKRL`), collection
  costs (`INKB`), the pre-court lawyer's fee under Nr. 2300 VV RVG (`VV23`) and other ancillary
  claims (`ANF`) — each with its own interest fields.

The assignment of interest, assignment and consumer-credit records to individual claims SHALL use
the format's assignment markers. The mapping SHALL be documented in the code and covered by tests.

#### Scenario: Main claim with base-rate interest

- **GIVEN** a main claim of 5,000.00 EUR with interest of 5 percentage points above the base rate
  from 2026-01-01
- **WHEN** the EDA file is generated
- **THEN** the claim is written as a main claim record with its amount and its catalogue number or
  free-text designation
- **AND** a running-interest record follows carrying the rate 5, the rate marker `B`, the annual
  period marker and the start date `260101`

#### Scenario: Interest from service expressed by an empty start date

- **GIVEN** a main claim whose interest starts on service of the dunning order
- **WHEN** the EDA file is generated
- **THEN** the running-interest record is written with the start date field left blank, which the
  format defines as interest from service of the Mahnbescheid
- **AND** no substitute date is invented

#### Scenario: Ancillary claims mapped to their own areas

- **GIVEN** a ledger holding reminder charges of 10.00 EUR and a pre-court Nr. 2300 VV RVG fee of
  480.20 EUR
- **WHEN** the EDA file is generated
- **THEN** the reminder charges are written in the reminder-cost area and the fee in the Nr. 2300
  VV RVG area, not both as "other ancillary claims"
- **AND** each carries its own interest fields

#### Scenario: Record order enforced

- **WHEN** an application with two creditors, one of them with two legal representatives, and
  three claims is generated
- **THEN** the records appear in the order the format prescribes, each legal representative
  directly following the party it represents
- **AND** the per-area frequency limits of the format are respected

### Requirement: Pre-Export Validation of the Application Data

Before generating an EDA file the system SHALL validate the application data and SHALL report
every problem in one list rather than failing on the first one. The validation SHALL cover at
least: at least one main claim with an amount greater than zero, a debtor with a complete postal
address, a creditor with a complete postal address, a determined dunning court with postal code
and place, the Kennziffer of the acting lawyer, the field lengths and value domains of the
Satzbeschreibung, the format's per-area frequency limits, and the consistency of interest rules
that start on service. Validation SHALL also be callable on its own, from the client and through
REST, so a user can check readiness without producing a file. Where the data exceeds what the
format can carry, the system SHALL say so plainly, because such an application has to be filed on
paper instead.

#### Scenario: Incomplete data reported in one list

- **WHEN** an export is validated for a ledger whose debtor has no postal code and whose dunning
  case has no Kennziffer
- **THEN** both problems are reported together with the field they refer to
- **AND** no file is generated

#### Scenario: Readiness check without export

- **WHEN** the user or a REST client requests validation only
- **THEN** the system returns the list of open issues, empty if the data set is complete
- **AND** nothing is stored or changed

#### Scenario: Data exceeding the format

- **WHEN** a claim designation is longer than the field the format provides
- **THEN** the validation reports the field, its limit and the actual length
- **AND** the message states that an application exceeding the format has to be filed on paper

### Requirement: Structural Verification of the Generated File

Every generated EDA file SHALL be verified structurally before it is stored or returned: record
length, record framing by `AA` and `BB`, the record type and format version in the header, record
order and per-area frequency, the trailer's application count, and the encodability of every
character in CP-850. When the verification fails, the system SHALL report the violations with the
record number and field they occur in and SHALL NOT store or return the file. No unverified file
SHALL ever be produced.

#### Scenario: Verification failure surfaces the violations

- **WHEN** the generated file contains a record that is not 128 bytes long
- **THEN** the user is shown the violation including the record number
- **AND** no document is stored in the case

#### Scenario: Character outside the permitted set

- **WHEN** a party name contains a character that CP-850 cannot represent
- **THEN** the export fails naming the character and the field it occurs in
- **AND** the character is not silently replaced

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
- **THEN** the file is generated for the selected court's postal code and place
- **AND** the dunning case records that court

### Requirement: Import of Court Response Messages

The system SHALL import the EDA messages the dunning courts send — the cost and issue notice for
the Mahnbescheid (`03`), the service and non-service notice for Mahnbescheid and
Vollstreckungsbescheid (`05`), the referral notice (`16`), the objection notice (`18`), the
monition (`20`), the cost and issue notice for the Vollstreckungsbescheid (`22`) and the receipt
confirmation (`90`) — from a file supplied by the user or received through the existing message
channels. An imported message SHALL be matched to its dunning case primarily by the participant's
own reference the court echoes back and secondarily by the court's Gerichtsnummer, SHALL update
the status and the relevant dates, SHALL trigger the resulting deadlines, and SHALL book the
service date into the ledger where interest starts on service. Messages that cannot be matched
SHALL be listed for manual assignment and SHALL never be silently discarded.

#### Scenario: Service message processed

- **WHEN** a service notice of record type `05` with the message kind "service of the
  Mahnbescheid" and a service date is imported
- **THEN** the dunning case matching the echoed own reference reaches the status "Mahnbescheid
  served" with that date
- **AND** the objection deadline and the follow-ups are created
- **AND** the ledger records the service booking for components with interest from service

#### Scenario: Objection notice processed

- **WHEN** an objection notice of record type `18` is imported
- **THEN** the dunning case records the objection with its date and, for a partial objection, the
  contested amount
- **AND** the deadlines that the objection makes obsolete are closed

#### Scenario: Unmatched message

- **WHEN** a message carries an own reference that no dunning case holds
- **THEN** the message is stored in an inbox list with its content readable in the formatted view
- **AND** the user can assign it to a dunning case manually

### Requirement: Exchanged Files Stored as Case Documents

Every generated EDA file and every imported court message SHALL be stored as a document of the
case, named through the document naming rules, tagged so the exchange can be found by tag, and
linked to the dunning case it belongs to. Re-exporting SHALL create an additional document rather
than overwriting the previous one, and all exchanged files of a case SHALL remain listable with
their creation or import date and the user who created or imported them.

#### Scenario: Export appears in the document list

- **WHEN** an export succeeds
- **THEN** the file appears in the case document list with its tag and creation date
- **AND** the dunning case links to that document

#### Scenario: Re-export keeps the history

- **WHEN** the user exports a second time after correcting an amount
- **THEN** both files exist in the case, distinguishable by date
- **AND** the dunning case links to the most recent one while the earlier one stays retrievable

### Requirement: EDA File Viewer

The document viewer SHALL display EDA files in a readable form instead of the raw fixed-length
records, with a formatted view resolving record types, section markers and fields into their
documented meaning — parties, court, own reference, claims with their amounts, interest and
ancillary claims, and the totals — and a second view showing the raw records. The viewer SHALL
open for EDA documents in the existing document viewer of the case panel, SHALL keep the last
selected view within a session, and SHALL show a file it cannot interpret as raw content with an
explanatory notice instead of failing.

#### Scenario: Formatted view of an export

- **WHEN** the user selects a stored EDA document
- **THEN** the viewer shows creditor, debtor, court, claims, interest, ancillary claims and totals
  in readable form
- **AND** the user can switch to the raw record view

#### Scenario: Malformed file

- **WHEN** the selected file does not follow the EDA record structure
- **THEN** the viewer shows the raw content with a notice that it could not be interpreted
- **AND** the document viewer does not fail

### Requirement: Lawyer Identification Number

A user account SHALL be able to store the identification number (Kennziffer) that identifies the
EDA participant towards the dunning courts: one number used by default and, where a court issues
and requires its own, additional court-specific numbers. The fields SHALL be offered in user
administration for users flagged as lawyers and SHALL not be shown for other users. The export
SHALL use the court-specific number when the selected court is flagged as requiring its own, and
the default number otherwise; the dunning case SHALL record the number actually used. Where a
Kennziffer identifies the acting lawyer, the export SHALL omit the lawyer's own address records,
because the format forbids sending both. An export SHALL be refused when no usable number exists
for the selected court.

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

#### Scenario: Kennziffer replaces the address records

- **WHEN** an application is exported for an acting lawyer identified by a Kennziffer
- **THEN** the Kennziffer is written in the key record and the lawyer's address records are omitted
- **AND** only the application-specific lawyer data the format still permits is written

#### Scenario: Export refused without Kennziffer

- **WHEN** an export is started for a dunning case whose acting lawyer has no Kennziffer
- **THEN** the validation reports the missing Kennziffer
- **AND** no file is generated
