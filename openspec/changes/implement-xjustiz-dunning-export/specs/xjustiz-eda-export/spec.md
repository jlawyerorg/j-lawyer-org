# Spec: XJustiz EDA Export

## Capability: XJustiz EDA Export

Enable export of ClaimLedger (Forderungskonto) data as XJustiz-compliant court dunning application XML files.

---

## ADDED Requirements

### Requirement: Export ClaimLedger as XJustiz XML

The system SHALL enable users to export a ClaimLedger as a valid XJustiz court dunning application XML file that MUST comply with the XJustiz 3.5.1 Mahn schema (xjustiz_0600_mahn_3_3.xsd).

#### Scenario: Export from desktop client

**Given** a user has a case with an associated ClaimLedger containing:
- At least one ClaimComponent with a non-zero amount
- A valid debtor (contact) with complete address information
- At least one ClaimLedgerEntry

**When** the user opens the ClaimLedger dialog and clicks "XJustiz Mahnantrag exportieren"

**And** provides required configuration:
- Target court (from XJustiz court code list)
- Claimant representative details (lawyer/firm)
- Service address for court communications

**Then** the system generates an XJustiz XML file

**And** validates the file against xjustiz_0600_mahn_3_3.xsd

**And** saves the file as an ArchiveFileDocumentsBean with tag "XJustiz Mahnantrag"

**And** offers to save the file to disk

**And** displays success confirmation with file location

#### Scenario: Export via REST API

**Given** an authenticated REST API client

**And** a ClaimLedger ID with valid export data

**When** the client sends `POST /v7/dunning/export/xjustiz` with:
```json
{
  "ledgerId": "<claim-ledger-id>",
  "courtCode": "AG München",
  "claimantRepresentative": {
    "name": "Rechtsanwalt Max Mustermann",
    "address": {...}
  },
  "serviceAddress": {...}
}
```

**Then** the API returns HTTP 200 with XML content

**And** Content-Type header is `application/xml`

**And** Content-Disposition header suggests filename `mahnantrag_<file-number>_<date>.xml`

---

### Requirement: Validate ClaimLedger before export

The system MUST validate that a ClaimLedger contains sufficient data for XJustiz export and SHALL provide clear guidance on missing or invalid data.

#### Scenario: Successful validation

**Given** a ClaimLedger with:
- Valid debtor contact with name, street, postal code, city
- At least one ClaimComponent (type: Hauptforderung) with amount > 0
- At least one ClaimLedgerEntry of type CLAIM

**When** user initiates export or calls validation endpoint

**Then** validation passes with no errors

**And** export proceeds normally

#### Scenario: Missing debtor information

**Given** a ClaimLedger where the debtor (contact) has incomplete address:
- Missing postal code or city

**When** user initiates export

**Then** system displays validation error: "Schuldneradresse unvollständig: PLZ und Ort sind erforderlich"

**And** export is blocked until data is corrected

**And** provides link to edit the contact

#### Scenario: No claim components

**Given** a ClaimLedger with no ClaimComponents

**When** user initiates export

**Then** system displays validation error: "Keine Forderungsbestandteile vorhanden. Mindestens eine Hauptforderung ist erforderlich."

**And** suggests creating a Hauptforderung component

#### Scenario: Validation via API

**Given** an authenticated REST API client

**When** the client sends `GET /v7/dunning/validate/<ledger-id>`

**Then** the API returns HTTP 200 with validation result:
```json
{
  "valid": false,
  "errors": [
    {
      "field": "debtor.postalCode",
      "message": "Postal code is required for debtor address"
    }
  ]
}
```

---

### Requirement: Map ClaimLedger components to XJustiz elements

The export MUST correctly map ClaimLedger domain model to XJustiz schema elements according to specified mapping rules.

#### Scenario: Map Hauptforderung (main claim)

**Given** a ClaimComponent with:
- Type: "Hauptforderung"
- Description: "Kaufpreis für Warenlieferung"
- Total amount (from ClaimLedgerEntries): EUR 5000.00

**When** exporting to XJustiz

**Then** generates XML element:
```xml
<forderung>
  <hauptforderung>
    <betrag waehrung="EUR">5000.00</betrag>
    <beschreibung>Kaufpreis für Warenlieferung</beschreibung>
  </hauptforderung>
</forderung>
```

#### Scenario: Map interest calculation

**Given** a ClaimComponent with:
- Type: "Zinsen"
- Interest rule: 5% p.a. from 2025-01-01
- Calculated interest amount: EUR 150.00

**When** exporting to XJustiz

**Then** generates XML element:
```xml
<zinsen>
  <betrag waehrung="EUR">150.00</betrag>
  <zinssatz>5.0</zinssatz>
  <verzinsungszeitraum>
    <beginn>2025-01-01</beginn>
  </verzinsungszeitraum>
</zinsen>
```

#### Scenario: Map costs

**Given** ClaimComponents with:
- Type: "Kosten", Description: "Anwaltsgebühren", Amount: EUR 500.00
- Type: "Kosten", Description: "Mahnkosten", Amount: EUR 50.00

**When** exporting to XJustiz

**Then** generates XML with separate cost elements:
```xml
<kosten>
  <position>
    <beschreibung>Anwaltsgebühren</beschreibung>
    <betrag waehrung="EUR">500.00</betrag>
  </position>
  <position>
    <beschreibung>Mahnkosten</beschreibung>
    <betrag waehrung="EUR">50.00</betrag>
  </position>
</kosten>
```

---

### Requirement: Include required XJustiz metadata

Export MUST include all mandatory XJustiz message envelope and metadata elements.

#### Scenario: Generate message header

**Given** an export request at 2025-11-11 14:30:00

**And** case file number "2025/1234"

**When** generating XJustiz XML

**Then** includes message header:
```xml
<nachrichtenkopf>
  <nachrichtentyp>Mahnantrag</nachrichtentyp>
  <nachrichtennummer>generated-uuid</nachrichtennummer>
  <erstellungszeitpunkt>2025-11-11T14:30:00+01:00</erstellungszeitpunkt>
  <absender>
    <aktenzeichen>2025/1234</aktenzeichen>
  </absender>
</nachrichtenkopf>
```

#### Scenario: Include claimant information

**Given** current law firm configured as:
- Name: "Kanzlei Mustermann"
- Address: "Hauptstraße 1, 80331 München"

**When** exporting to XJustiz

**Then** includes claimant:
```xml
<glaeubiger>
  <name>Kanzlei Mustermann</name>
  <anschrift>
    <strasse>Hauptstraße 1</strasse>
    <postleitzahl>80331</postleitzahl>
    <ort>München</ort>
  </anschrift>
</glaeubiger>
```

#### Scenario: Include debtor information from selected participant

**Given** user selected case participant in export dialog:
- Name: "Schmidt GmbH"
- Role: "Gegner"
- Address: "Berliner Str. 42, 10115 Berlin"

**When** exporting to XJustiz

**Then** includes debtor from selected participant:
```xml
<schuldner>
  <organisation>
    <bezeichnung>Schmidt GmbH</bezeichnung>
  </organisation>
  <anschrift>
    <strasse>Berliner Str. 42</strasse>
    <postleitzahl>10115</postleitzahl>
    <ort>Berlin</ort>
  </anschrift>
</schuldner>
```

**And** uses complete address data from AddressBean

#### Scenario: Include lawyer representative from selected user

**Given** user selected lawyer in export dialog:
- Name: "Rechtsanwalt Max Mustermann"
- Firm: "Kanzlei Mustermann"
- Address: "Hauptstraße 1, 80331 München"

**When** exporting to XJustiz

**Then** includes legal representative from selected lawyer user:
```xml
<vertreter>
  <natuerlichePerson>
    <name>
      <vorname>Max</vorname>
      <nachname>Mustermann</nachname>
    </name>
    <bezeichnung>Rechtsanwalt</bezeichnung>
  </natuerlichePerson>
  <kanzlei>Kanzlei Mustermann</kanzlei>
  <anschrift>
    <strasse>Hauptstraße 1</strasse>
    <postleitzahl>80331</postleitzahl>
    <ort>München</ort>
  </anschrift>
</vertreter>
```

**And** uses lawyer information from AppUserBean

---

### Requirement: Perform XML schema validation

All generated XJustiz XML MUST be validated against the XJustiz 3.5.1 Mahn schema before being saved or returned to the user.

#### Scenario: Schema validation passes

**Given** generated XJustiz XML with all required elements

**When** validating against xjustiz_0600_mahn_3_3.xsd

**Then** validation succeeds with no errors

**And** file is saved and export completes

#### Scenario: Schema validation fails

**Given** generated XJustiz XML missing a mandatory element

**When** validating against xjustiz_0600_mahn_3_3.xsd

**Then** validation fails with specific error message

**And** export is aborted

**And** error message indicates which element is missing or invalid

**And** provides guidance for correcting the issue

#### Scenario: Schema file missing

**Given** XJustiz XSD schema file is not found in expected location

**When** attempting export

**Then** system displays error: "XJustiz Schema-Dateien nicht gefunden. Bitte Installation überprüfen."

**And** logs detailed error with expected file path

**And** export is blocked until schemas are available

---

### Requirement: Provide export configuration dialog

Desktop client MUST provide a configuration dialog to collect required information not present in ClaimLedger data.

#### Scenario: Display configuration dialog

**Given** user clicks "XJustiz Mahnantrag exportieren" in ClaimLedgerDialog

**When** validation passes

**Then** displays "XJustiz Export Konfiguration" dialog with fields:
- **Schuldner** dropdown populated from case participants
- **Gericht** dropdown with common German courts
- Free-text court code field for unlisted courts
- **Prozessbevollmächtigter** dropdown populated from lawyer users
- Service address (pre-filled from firm settings, editable)
- Optional: Additional notes

**And** "Exportieren" button (enabled when all required fields filled)

**And** "Abbrechen" button

#### Scenario: Populate Schuldner dropdown from case participants

**Given** a case with 3 participants:
- "Schmidt GmbH" (Role: Gegner)
- "Müller AG" (Role: Beklagter)
- "Weber Versicherung" (Role: Drittschuldner)

**When** user opens export configuration dialog

**Then** Schuldner dropdown contains 3 entries:
- "Schmidt GmbH (Gegner)"
- "Müller AG (Beklagter)"
- "Weber Versicherung (Drittschuldner)"

**And** first entry is pre-selected if no previous selection exists

**And** entries are sorted alphabetically by name

#### Scenario: Populate Prozessbevollmächtigter dropdown from lawyer users

**Given** system has 5 users:
- "Max Mustermann" (lawyerAttribute = true)
- "Anna Schmidt" (lawyerAttribute = true)
- "Peter Müller" (lawyerAttribute = false, Secretary)
- "Lisa Weber" (lawyerAttribute = true)
- "Tom Klein" (lawyerAttribute = false, Assistant)

**When** user opens export configuration dialog

**Then** Prozessbevollmächtigter dropdown contains 3 entries:
- "Max Mustermann"
- "Anna Schmidt"
- "Lisa Weber"

**And** current user is pre-selected if they have lawyerAttribute = true

**And** entries are sorted alphabetically by name

#### Scenario: Auto-select when only one option available

**Given** case has exactly one participant

**And** system has exactly one lawyer user

**When** user opens export configuration dialog

**Then** Schuldner dropdown has one entry and is pre-selected

**And** Prozessbevollmächtigter dropdown has one entry and is pre-selected

**And** "Exportieren" button is enabled (assuming court also selected)

#### Scenario: No case participants available

**Given** case has no participants (getInvolvedParties() returns empty list)

**When** user opens export configuration dialog

**Then** Schuldner dropdown is empty with placeholder text "Keine Beteiligten vorhanden"

**And** "Exportieren" button is disabled

**And** displays warning: "Bitte fügen Sie zunächst Beteiligte zur Akte hinzu"

**And** provides button "Beteiligte bearbeiten" to open participant management

#### Scenario: No lawyer users available

**Given** system has no users with lawyerAttribute = true

**When** user opens export configuration dialog

**Then** Prozessbevollmächtigter dropdown is empty with placeholder "Keine Anwälte im System"

**And** "Exportieren" button is disabled

**And** displays warning: "Kein Benutzer mit Anwalts-Attribut gefunden"

**And** suggests contacting administrator to configure lawyer users

#### Scenario: Save configuration for reuse

**Given** user completes export configuration dialog

**And** checks "Als Vorlage speichern"

**When** clicking "Exportieren"

**Then** saves configuration as user preference

**And** pre-fills same values on next export

**And** allows user to modify for each export

#### Scenario: Configuration validation for required fields

**Given** user enters configuration

**When** any required field is empty:
- Schuldner dropdown has no selection
- OR court code field is empty
- OR Prozessbevollmächtigter dropdown has no selection

**Then** "Exportieren" button is disabled

**And** displays hint for missing field:
- "Schuldner ist erforderlich"
- "Gericht ist erforderlich"
- "Prozessbevollmächtigter ist erforderlich"

**And** hints are displayed next to respective fields

---

### Requirement: Store generated XML as case document

Exported XJustiz files MUST be stored as case documents for audit trail and future reference.

#### Scenario: Save export as document

**Given** successful XJustiz export

**When** XML is generated and validated

**Then** creates ArchiveFileDocumentsBean with:
- Name: "XJustiz Mahnantrag - <date>"
- Favorite: false
- Dictionary: null
- Tags: ["XJustiz Mahnantrag"]
- File content: generated XML

**And** associates document with the case

**And** appears in case document list

#### Scenario: Download previous export

**Given** case has previously exported XJustiz file

**When** user views document list filtered by tag "XJustiz Mahnantrag"

**Then** displays all XJustiz exports with timestamps

**And** user can download, preview, or delete any export

**And** can re-export to generate updated version

---

### Requirement: Display EDA files in document viewer

The system MUST provide a specialized viewer for XJustiz EDA files in the ArchiveFilePanel document viewer that SHALL display content in both formatted and raw XML views.

#### Scenario: View EDA file in formatted view

**Given** a case with an exported XJustiz EDA file tagged "XJustiz Mahnantrag"

**When** user selects the file in the document list

**Then** the document viewer opens XJustizEdaViewerPanel

**And** displays two tabs: "Formatierte Ansicht" and "XML Rohdaten"

**And** "Formatierte Ansicht" tab is active by default

**And** formatted view shows:
- Document header with creation date
- Claimant (Gläubiger) name and address
- Debtor (Schuldner) name and address
- Court name and case reference
- Itemized claims (Hauptforderung, Zinsen, Kosten)
- Total amount with currency

**And** all amounts are formatted as German currency (e.g., "5.000,00 EUR")

**And** all labels are in German

#### Scenario: Switch to raw XML view

**Given** EDA file is open in formatted view

**When** user clicks "XML Rohdaten" tab

**Then** raw XML content is displayed

**And** XML is syntax-highlighted for readability

**And** XML content is scrollable

**And** user can select and copy text from XML view

#### Scenario: View malformed EDA file

**Given** an XML file tagged "XJustiz Mahnantrag" with invalid structure

**When** user opens the file in document viewer

**Then** formatted view displays error message: "Fehler beim Parsen der EDA-Datei. Bitte prüfen Sie die XML-Rohdaten."

**And** provides button to switch to "XML Rohdaten" tab

**And** raw XML view still displays the file content

**And** error is logged with parsing details

#### Scenario: EDA viewer integration with document list

**Given** user views document list in ArchiveFilePanel

**When** multiple documents exist including EDA files

**Then** EDA files are identifiable by "XJustiz Mahnantrag" tag

**And** clicking an EDA file opens it in XJustizEdaViewerPanel

**And** clicking a non-EDA file opens it in appropriate viewer

**And** viewer switching is seamless without errors

#### Scenario: Display all claim components

**Given** an EDA file with multiple claim components:
- Hauptforderung: EUR 5,000.00
- Zinsen (5% from 2025-01-01): EUR 150.00
- Kosten - Anwaltsgebühren: EUR 500.00
- Kosten - Mahnkosten: EUR 50.00

**When** viewing in formatted view

**Then** displays structured claim breakdown:
```
Forderungen:
  Hauptforderung:           5.000,00 EUR
  Zinsen (5,0% ab 01.01.2025):
                              150,00 EUR
  Kosten:
    Anwaltsgebühren:          500,00 EUR
    Mahnkosten:                50,00 EUR
  ──────────────────────────────────────
  Gesamt:                   5.700,00 EUR
```

**And** total is correctly calculated

**And** interest rate and date are included if present

#### Scenario: Remember tab selection

**Given** user opens EDA file and switches to "XML Rohdaten" tab

**When** user closes and reopens the same EDA file in same session

**Then** "XML Rohdaten" tab is active (last used tab remembered)

**And** tab preference is session-scoped (resets on application restart)

---

### Requirement: Store and use lawyer identification number

The system MUST allow storing a lawyer identification number (Kennziffer) for users with lawyer attribute and SHALL use this number in XJustiz export.

#### Scenario: Configure lawyer identification number in user management

**Given** administrator edits a user with lawyerAttribute = true

**When** viewing user edit dialog

**Then** displays "Kennziffer Prozessbevollmächtigter" text field

**And** field is visible and editable

**And** field accepts alphanumeric input up to 50 characters

**And** field can be saved with user profile

**And** saved value persists and is retrievable

#### Scenario: Hide lawyer ID field for non-lawyer users

**Given** administrator edits a user with lawyerAttribute = false

**When** viewing user edit dialog

**Then** "Kennziffer Prozessbevollmächtigter" field is not visible

**And** field is not editable for non-lawyer users

#### Scenario: Include lawyer identification in XJustiz export

**Given** user selected lawyer with:
- Name: "Max Mustermann"
- lawyerIdentificationNumber: "A12345"

**When** exporting to XJustiz

**Then** includes lawyer identification in representative element:
```xml
<vertreter>
  <natuerlichePerson>
    <name>
      <vorname>Max</vorname>
      <nachname>Mustermann</nachname>
    </name>
    <bezeichnung>Rechtsanwalt</bezeichnung>
  </natuerlichePerson>
  <kennziffer>A12345</kennziffer>
  <kanzlei>Kanzlei Mustermann</kanzlei>
  <anschrift>...</anschrift>
</vertreter>
```

**And** kennziffer is populated from AppUserBean.lawyerIdentificationNumber

#### Scenario: Validate lawyer identification number before export

**Given** user selects lawyer without lawyerIdentificationNumber

**When** user attempts to export

**Then** validation fails with error message: "Der ausgewählte Prozessbevollmächtigte hat keine Kennziffer hinterlegt"

**And** export is blocked

**And** provides link to edit user profile

**And** suggests entering identification number in user management

#### Scenario: Display lawyer ID in configuration dropdown

**Given** lawyer users with identification numbers:
- "Max Mustermann" (ID: A12345)
- "Anna Schmidt" (ID: B67890)
- "Lisa Weber" (ID: missing)

**When** user opens export configuration dialog

**Then** Prozessbevollmächtigter dropdown displays:
- "Max Mustermann (A12345)"
- "Anna Schmidt (B67890)"
- "Lisa Weber (Kennziffer fehlt)"

**And** entries with missing ID are visually marked

**And** selecting entry with missing ID triggers validation warning

---

## MODIFIED Requirements

None - this is a new feature with no modifications to existing requirements.

---

## REMOVED Requirements

None - this is a new feature with no removals.
