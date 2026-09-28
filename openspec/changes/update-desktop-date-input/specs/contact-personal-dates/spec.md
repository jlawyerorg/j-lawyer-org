## ADDED Requirements

### Requirement: Explicit personal dates in desktop contacts
Birth and death fields in the desktop contact editor MUST follow the desktop date-only input requirement and show `TTMMJJJJ` as a gray, non-value hint in empty fields. Quick contact creation MUST follow the same rule for birth dates. Empty optional dates MUST remain valid.

#### Scenario: Birth date without dots
- **WHEN** a user enters `28061979` in the contact editor or quick creation
- **THEN** the client displays and saves `28.06.1979`

#### Scenario: No century guess
- **WHEN** a user enters `28.06.79` as a birth or death date
- **THEN** the client requests a four-digit year

### Requirement: Plausible newly entered personal dates
The desktop client MUST reject new or changed birth or death dates after today and newly introduced cases in which death precedes birth. Newly entered invalid dates MUST NOT produce a misleading age display.

#### Scenario: Date after today
- **WHEN** a user enters a birth or death date in the future
- **THEN** the client reports an error and leaves the input for correction

#### Scenario: Death before birth
- **WHEN** the new death date precedes a comparable valid birth date
- **THEN** the client refuses to save the new inconsistency

### Requirement: Unchanged legacy contact dates
The desktop client MUST NOT rewrite an unchanged stored malformed personal date or block unrelated contact edits solely because it is already stored.

#### Scenario: Update name while old date stays untouched
- **WHEN** a contact has an unchanged malformed date and the user edits only its name
- **THEN** the name can be saved and the stored date remains unchanged
