# calendar-api Specification

## Purpose
Defines the REST API endpoint for retrieving calendar entries (reviews/events) within a specified date range. This capability enables external systems and mobile applications to efficiently query calendar data for specific time periods, supporting calendar view displays, offline synchronization, and dashboard integrations.
## Requirements
### Requirement: Date Range Query Endpoint
The REST API SHALL provide an endpoint to retrieve calendar entries within a specified date range.

#### Scenario: Retrieve open events for a date range
**Given** an authenticated user with access to one or more cases
**When** the client sends a GET request to `/j-lawyer-io/rest/v4/cases/duedates/range?from={fromDate}&to={toDate}`
**Then** the API SHALL return a JSON array of calendar entries where `beginDate >= fromDate AND beginDate <= toDate`
**And** the results SHALL only include entries for cases the user has permission to access
**And** the response status SHALL be 200 OK

#### Scenario: Invalid date format
**Given** an authenticated user
**When** the client sends a GET request with invalid date format
**Then** the API SHALL return status 400 Bad Request
**And** the response SHALL include an error message describing the expected date format

### Requirement: Date Format Support
The endpoint SHALL accept dates in ISO 8601 format or as epoch milliseconds.

#### Scenario: Accept ISO 8601 date format
**Given** an authenticated user
**When** the client sends `from=2024-01-01T00:00:00Z&to=2024-01-31T23:59:59Z`
**Then** the API SHALL parse the dates correctly
**And** return events within January 2024

#### Scenario: Accept epoch milliseconds
**Given** an authenticated user
**When** the client sends `from=1704067200000&to=1706745599000` (January 2024 in milliseconds)
**Then** the API SHALL parse the dates correctly
**And** return events within January 2024

### Requirement: Optional Filtering Parameters
The endpoint SHALL support optional filtering by event type and completion status.

#### Scenario: Filter by event type
**Given** an authenticated user
**When** the client includes `eventType=10` (followup) in the query parameters
**Then** the API SHALL return only followup entries within the date range
**And** exclude respite (20) and event (30) types

#### Scenario: Filter by completion status
**Given** an authenticated user
**When** the client includes `status=0` (open) in the query parameters
**Then** the API SHALL return only open (not done) entries
**And** exclude completed entries

#### Scenario: Combine multiple filters
**Given** an authenticated user
**When** the client sends `from=2024-01-01&to=2024-01-31&eventType=30&status=0`
**Then** the API SHALL return only open events (not followups or respites) in January 2024

### Requirement: Response Format
The endpoint SHALL return calendar entries using the existing RestfulDueDateV6 POJO format.

#### Scenario: Response includes all event fields
**Given** a successful query
**When** the API returns calendar entries
**Then** each entry SHALL be formatted as RestfulDueDateV6 object including:
- Event ID
- Summary
- Description
- Location
- Begin date/time
- End date/time
- Event type (followup, respite, event)
- Completion status (done flag)
- Assignee
- Case ID
- All other fields defined in RestfulDueDateV6

### Requirement: Security and Authorization
The endpoint SHALL enforce case-level access controls consistent with existing calendar operations.

#### Scenario: User receives only accessible events
**Given** a user with access to cases A and B but not case C
**When** the user queries a date range containing events from all three cases
**Then** the API SHALL return events from cases A and B only
**And** SHALL NOT return events from case C
**And** SHALL NOT indicate the existence of case C events

#### Scenario: Unauthorized access
**Given** an unauthenticated request
**When** the client attempts to access the endpoint without credentials
**Then** the API SHALL return status 401 Unauthorized

### Requirement: Default Behavior
The endpoint SHALL provide sensible defaults when optional parameters are omitted.

#### Scenario: Default status filter
**Given** the `status` parameter is omitted
**When** the client queries a date range
**Then** the API SHALL return events with any status (both open and done)

#### Scenario: Default event type filter
**Given** the `eventType` parameter is omitted
**When** the client queries a date range
**Then** the API SHALL return events of all types (followup, respite, and event)

#### Scenario: Limit protection
**Given** a date range query that could return an excessive number of results
**When** the query is executed
**Then** the API SHALL apply a reasonable limit (default: 5000 entries)
**And** SHALL return the results ordered by begin date ascending

