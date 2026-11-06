## ADDED Requirements

### Requirement: Cross-case history search
The system SHALL provide a report "Aktenhistorie" that searches history entries across all cases the current user may access. The search SHALL be filterable by date range, user, case (file number or name) and change text.

#### Scenario: Default view
- **WHEN** the user opens the report
- **THEN** the history entries of the current user from the last 7 days are listed, newest first

#### Scenario: Date range preset
- **WHEN** the user selects a preset such as "Letzter Monat"
- **THEN** the from/to dates are set accordingly and the search runs immediately

#### Scenario: Custom date range
- **WHEN** the user changes the from or to date manually
- **THEN** the preset switches to "Benutzerdefiniert", and the to date includes the whole day

#### Scenario: Text and case filter
- **WHEN** the user enters a case query and/or a text query and presses Enter
- **THEN** only entries whose case file number or name matches the case query, and whose change description contains the text query, are listed

### Requirement: Permission-aware result limit
The search SHALL apply the result limit after removing entries of cases the user is not allowed to see. The client SHALL indicate when the limit has been reached.

#### Scenario: Restricted cases
- **WHEN** some matching entries belong to group-protected cases the user cannot access
- **THEN** these entries are omitted, and the result is still filled up to the limit with permitted entries

#### Scenario: Limit reached
- **WHEN** more permitted entries match than the selected limit
- **THEN** exactly the limit is shown, together with a hint to narrow the date range or filters

### Requirement: Tabular result with actions
The report SHALL show the results in a sortable table with the columns time, user, file number, case name and change, plus a detail pane with the full change text of the selected entry.

#### Scenario: Open case
- **WHEN** the user double-clicks an entry, presses Enter or uses "Akte öffnen"
- **THEN** the case is opened in the case editor, and navigating back returns to the report

#### Scenario: Export
- **WHEN** the user clicks "Export"
- **THEN** the currently displayed rows are exported as a spreadsheet file and opened

#### Scenario: Stale results
- **WHEN** the user changes filters while a search is still running
- **THEN** only the result of the latest search is displayed

### Requirement: Timeline presentation
The report SHALL present the history as a timeline. Entries SHALL be grouped by day with separator rows when sorted by time, SHALL show a colored badge for the kind of change, and SHALL show the key figures of the displayed entries together with an activity chart.

#### Scenario: Day grouping
- **WHEN** the entries are sorted by time
- **THEN** a separator row such as "Heute · Mi, 05.11.2026 · 23 Einträge" precedes the entries of each day
- **AND** sorting by another column removes the separators

#### Scenario: Category filter
- **WHEN** the user selects one or more category chips
- **THEN** only entries of these categories are shown, and the key figures and the chart refer to them

#### Scenario: Search term highlighting
- **WHEN** a text or case query was used for the search
- **THEN** its occurrences are highlighted in the table and in the detail card

#### Scenario: Jump to a day
- **WHEN** the user clicks a bar of the activity chart
- **THEN** the table scrolls to the entries of that day
