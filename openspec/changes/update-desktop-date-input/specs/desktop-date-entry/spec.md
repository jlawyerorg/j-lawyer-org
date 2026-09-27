## ADDED Requirements

### Requirement: Eight-digit dates in desktop date-only input
Every directly editable desktop field dedicated solely to a calendar date MUST accept exactly `DDMMYYYY` and `DD.MM.YYYY`, normalize the undotted form to `DD.MM.YYYY` before saving, and reject impossible dates with a useful error. The year MUST have four digits; the client MUST NOT guess a century.

#### Scenario: Accounting date without dots
- **WHEN** a user commits `28061979` in a directly editable accounting date field
- **THEN** the client accepts and displays `28.06.1979`

#### Scenario: Existing dotted format
- **WHEN** a user commits `28.06.1979` in a directly editable date-only field
- **THEN** the date remains `28.06.1979`

#### Scenario: Ambiguous or impossible date
- **WHEN** a user commits `280679` or `31022024` in such a field
- **THEN** the client reports the format or calendar error and does not save the invalid input

#### Scenario: Future due date
- **WHEN** a user enters a valid future due date with no conflicting existing field rule
- **THEN** the client permits the date

#### Scenario: Editable date in a table
- **WHEN** a user enters `28061979` into a selected date-only table cell and commits the table edit
- **THEN** the client stores `28.06.1979` as the date for that row

#### Scenario: Search range using a date chooser text editor
- **WHEN** a user types `28061979` into a directly editable date chooser used for a search range
- **THEN** the client applies the same calendar-date check and uses `28.06.1979` in the search

### Requirement: Date-only scope
The desktop client MUST apply undotted conversion only to expressly identified directly editable date-only fields. Notes, arbitrary free text, displays and date-time inputs MUST retain their current semantics. A calendar-only field MUST retain its existing interaction mode.

#### Scenario: Eight digits in a note
- **WHEN** a user types `28061979` in a note
- **THEN** the note contains those same eight digits

#### Scenario: Date-time field
- **WHEN** a user edits a date-time field
- **THEN** its existing time-aware input remains available

#### Scenario: Calendar-only field
- **WHEN** a user selects a date in a field that previously allowed only calendar selection
- **THEN** that interaction remains available without a new typing mode
