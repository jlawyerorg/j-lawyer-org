## ADDED Requirements

### Requirement: Readable Placeholder Labels
The system SHALL provide a readable label for every placeholder offered to users. Party
placeholders SHALL be labelled with the party type's name and the field label (e.g.
"Mandant: Vorname" for `{{MANDANT_VORNAME}}`). A placeholder without a maintained label SHALL be
shown with its key.

#### Scenario: Party placeholder label
- **WHEN** a party type "Mandant" with placeholder prefix `MANDANT` exists
- **THEN** `{{MANDANT_VORNAME}}` SHALL be shown as "Mandant: Vorname"

#### Scenario: Fixed placeholder label
- **WHEN** the placeholder `{{AKTE_ZEICHEN}}` is offered
- **THEN** it SHALL be shown as "Akte: Aktenzeichen"

#### Scenario: Missing label
- **WHEN** a placeholder has no maintained label
- **THEN** it SHALL be shown with its key and remain selectable

### Requirement: Placeholder Categories
The system SHALL group placeholders into categories (Akte, Beteiligte, Falldaten, Benutzer,
Kanzlei, Datum und Dokument, Rechnung, Rechnungsposition, Zeiterfassung, Ingo, E-Mail). Party
placeholders SHALL be grouped by party type and form placeholders by form below their category.

#### Scenario: Party placeholders grouped by party type
- **WHEN** the party types "Mandant" and "Gegner" exist
- **THEN** the category "Beteiligte" SHALL contain the sub-categories "Mandant" and "Gegner",
  each with that party type's placeholders

### Requirement: Placeholders Offered For E-Mails
In the post template editor and the text block editor the system SHALL offer only placeholders
that can be resolved when composing e-mails and beA messages, including `{{CURSOR}}` and
`{{CLOUD_LINK}}`, and SHALL NOT offer invoice, invoice position, time sheet and table
placeholders. Alias placeholders SHALL NOT be offered but SHALL continue to be resolved.

#### Scenario: Invoice placeholders hidden
- **WHEN** the user opens the placeholder picker in the post template editor
- **THEN** no placeholder of the categories Rechnung, Rechnungsposition and Zeiterfassung SHALL
  be offered

#### Scenario: E-mail placeholders offered
- **WHEN** the user opens the placeholder picker in the text block editor
- **THEN** `{{CURSOR}}` and `{{CLOUD_LINK}}` SHALL be offered in the category "E-Mail"

#### Scenario: Alias still resolved
- **WHEN** an existing post template contains `{{MANDANT_FIRMA}}`
- **THEN** the placeholder SHALL be resolved as before, although it is not offered in the picker

### Requirement: Cloud Link Not Available In beA Messages
The beA composer SHALL replace `{{CLOUD_LINK}}` in post templates and text blocks with an empty
text, and the placeholder SHALL be labelled as available in e-mails only.

#### Scenario: Text block with cloud link in beA message
- **WHEN** the user inserts a text block containing `{{CLOUD_LINK}}` into a beA message
- **THEN** the inserted text SHALL NOT contain `{{CLOUD_LINK}}`

### Requirement: Placeholder Picker
The post template editor and the text block editor SHALL provide a placeholder picker with a
search field and a category tree. The search SHALL match label, combined label and key,
case-insensitively. Selecting placeholders and confirming (double click, Enter or insert button)
SHALL insert their keys at the cursor position of the target field.

#### Scenario: Search by label
- **WHEN** the user types "vorname" into the search field
- **THEN** the picker SHALL list the first name placeholders of all party types and of the user,
  each with its combined label

#### Scenario: Search by key
- **WHEN** the user types "AKTE_ZEI" into the search field
- **THEN** the picker SHALL list `{{AKTE_ZEICHEN}}`

#### Scenario: Insert by double click
- **WHEN** the user double-clicks the entry "Mandant: Vorname"
- **THEN** `{{MANDANT_VORNAME}}` SHALL be inserted at the cursor position of the target field
