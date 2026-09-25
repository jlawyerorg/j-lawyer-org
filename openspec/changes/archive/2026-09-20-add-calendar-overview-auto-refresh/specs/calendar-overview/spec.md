## ADDED Requirements

### Requirement: Projected Calendar Entry Payload
The cross-case calendar query SHALL return a projection containing only the fields the overview renders, and SHALL NOT return persistence entities that carry the associated case, its owner group, its folder tree or calendar credentials.

#### Scenario: Entry is returned without the case object graph
- **WHEN** a client requests calendar entries across cases
- **THEN** each returned entry carries the event's own fields plus the case id, file number, short name, subject and lawyer, and the calendar id, name and colour
- **AND** no `ArchiveFileBean`, `Group`, `CaseFolder` or `CalendarSetup` instance is part of the response

#### Scenario: Calendar credentials are not disclosed
- **WHEN** a calendar entry belongs to a cloud-synchronised calendar
- **THEN** the response contains the calendar's display name and colour
- **AND** the response contains neither the calendar's user name nor its password

#### Scenario: Entry without an assigned calendar
- **WHEN** an entry has no calendar assigned
- **THEN** the entry is returned with empty calendar fields and no error occurs

### Requirement: Case Visibility Parity
The projected query SHALL return exactly the set of entries the caller is permitted to see under the existing case visibility rules, and SHALL resolve visibility without enumerating all cases of the installation.

#### Scenario: Same result set as the unprojected query
- **WHEN** the projected query is executed for a user with open entries across several cases
- **THEN** it returns the same set of event ids as the existing unprojected cross-case query for that user

#### Scenario: User without any group
- **WHEN** the caller belongs to no group
- **THEN** the query executes without error and applies the same visibility rules as before

#### Scenario: Case with an owner group but no access entries
- **WHEN** a case has an owner group the caller does not belong to and carries no case-group access rows
- **THEN** its entries remain visible to the caller, as they are today

#### Scenario: Entry without a case reference
- **WHEN** an entry has no case reference
- **THEN** it is excluded from the result, as it is today

### Requirement: Calendar Change Version
The server SHALL expose an opaque version value that changes whenever any calendar entry is created, updated or deleted, regardless of which service performed the write.

#### Scenario: Version changes on a write through any service
- **WHEN** a calendar entry is created, updated or deleted through any server-side service, including dunning deadlines and claim-ledger follow-ups
- **THEN** the calendar version value differs from the value read before the write

#### Scenario: Version after a server restart
- **WHEN** the server is restarted
- **THEN** the version value differs from the value clients last observed
- **AND** clients treat the difference as a change and reload rather than serving stale data

#### Scenario: Version is comparable only for equality
- **WHEN** a client compares a stored version with a freshly read one
- **THEN** equality means nothing changed, and any difference means the client reloads

### Requirement: Conditional Calendar Entry Fetch
The cross-case calendar query SHALL accept the version the caller last observed and SHALL skip both the database query and the entry payload when that version still matches the current one.

#### Scenario: Nothing changed since the last fetch
- **WHEN** a client requests entries passing the version it last observed and nothing has changed since
- **THEN** the response reports that it is unchanged, carries no entries, and no database query for entries is executed

#### Scenario: Something changed since the last fetch
- **WHEN** a client passes a version that no longer matches the current one
- **THEN** the response carries the current entries together with the current version

#### Scenario: First fetch of a session
- **WHEN** a client has no previously observed version
- **THEN** it passes a value that forces a fetch and receives entries together with the current version

#### Scenario: Caller-supplied date arguments are not modified
- **WHEN** a caller passes date arguments to the query
- **THEN** the objects the caller passed are unchanged after the call returns

### Requirement: Automatic Refresh Of The Chronological Overview
The chronological calendar overview SHALL refresh itself without user interaction, and SHALL perform no server calls while it is not the active view.

#### Scenario: View is opened
- **WHEN** the user navigates to the chronological calendar overview
- **THEN** the calendar sheet loads the entries for its visible interval
- **AND** the list reloads only if the calendar version has changed since it was last loaded

#### Scenario: Change made by another user while the view is open
- **WHEN** another user creates or changes a calendar entry while the overview is the active view
- **THEN** the overview shows that change within one refresh interval without the user pressing refresh

#### Scenario: Nothing changes while the view is open
- **WHEN** the overview stays open and no calendar entry changes
- **THEN** each refresh interval transfers only the version check, and no entry payload

#### Scenario: View is not active
- **WHEN** the overview is not the active view
- **THEN** no version checks and no entry fetches are performed

#### Scenario: Periodic unconditional refresh
- **WHEN** the overview has been the active view for about ten minutes without the version changing
- **THEN** it reloads once unconditionally, so that changes the version cannot observe are picked up

#### Scenario: Refresh fails
- **WHEN** a refresh fails, for example because the server is unreachable
- **THEN** the previously displayed entries remain visible
- **AND** the next interval retries rather than treating the data as current

#### Scenario: Client shutdown
- **WHEN** the client is closed
- **THEN** the refresh timer is cancelled and does not prevent the process from exiting

### Requirement: Refresh Preserves View State
An automatic refresh SHALL NOT disturb what the user is currently looking at.

#### Scenario: A row is selected
- **WHEN** an automatic refresh replaces the list contents while a row is selected
- **THEN** the same calendar entry is selected afterwards, identified by its entry id rather than by its row position

#### Scenario: The list is scrolled
- **WHEN** an automatic refresh replaces the list contents while the list is scrolled
- **THEN** the scroll position is preserved

#### Scenario: The calendar sheet shows another month
- **WHEN** an automatic refresh occurs while the calendar sheet displays a month other than the current one
- **THEN** the displayed interval does not change

#### Scenario: Refresh is visually quiet
- **WHEN** an automatic refresh occurs
- **THEN** the list is not shown empty in the meantime

### Requirement: Scope Of Loaded Entries
The list SHALL keep showing all open entries irrespective of their date, while the calendar sheet SHALL load only the interval it can display.

#### Scenario: Long overdue entry
- **WHEN** an open entry lies far in the past
- **THEN** it is still shown in the list

#### Scenario: Entry far in the future
- **WHEN** an open entry lies years in the future
- **THEN** it is still shown in the list

#### Scenario: Result set hits the safety limit
- **WHEN** the number of open entries exceeds the configured limit
- **THEN** the entries nearest in time are kept
- **AND** a visible indicator in the view states that the list was truncated, without requiring the user to hover or open a dialog

#### Scenario: Calendar sheet interval changes
- **WHEN** the user pages the calendar sheet to another interval
- **THEN** the entries for the newly visible interval are loaded

#### Scenario: Appointment spanning the interval boundary
- **WHEN** an appointment starts before the visible interval and ends inside it
- **THEN** it is included in the entries loaded for that interval

### Requirement: Data Currency Is Visible
The overview SHALL show the user how current the displayed data is, so that a quiet view cannot be mistaken for a stale one.

#### Scenario: Data was loaded successfully
- **WHEN** the overview has loaded entries
- **THEN** the view states when that data was last loaded

#### Scenario: Refresh failed
- **WHEN** a refresh fails and the previously loaded entries remain on screen
- **THEN** the view indicates that the data could not be updated and continues to show when it was last loaded successfully

#### Scenario: Manual refresh remains available
- **WHEN** automatic refresh is active
- **THEN** the user can still trigger an immediate refresh themselves

### Requirement: Immediate Reflection Of Local Changes
Calendar changes made in the running client SHALL appear in the chronological overview without waiting for the next refresh interval.

#### Scenario: Entry created elsewhere in the same client
- **WHEN** the user creates or changes a calendar entry in another part of the client
- **THEN** the chronological overview reflects it promptly

#### Scenario: Burst of changes
- **WHEN** several calendar changes are published in quick succession
- **THEN** they are coalesced into a single reload rather than one reload per change
