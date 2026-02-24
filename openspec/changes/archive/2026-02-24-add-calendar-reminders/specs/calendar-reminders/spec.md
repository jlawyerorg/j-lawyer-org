## ADDED Requirements

### Requirement: Reminder Attribute on Events
The system SHALL support a configurable reminder lead time (`reminderMinutes`, column `reminder_minutes`) on calendar events (eventType=EVENT, 30). The value semantics are: -1 = no reminder (default), 0 = reminder at event start, >0 = reminder N minutes before event start. This is consistent with iCalendar/CalDAV where TRIGGER:PT0M is a valid alarm at event start (RFC 5545).

#### Scenario: Create event with reminder
- **WHEN** a user creates a new event (eventType=EVENT) and selects a reminder of 15 minutes
- **THEN** the system SHALL persist `reminderMinutes=15` on the event

#### Scenario: Create event with reminder at start
- **WHEN** a user creates a new event and selects "Bei Beginn" (at start)
- **THEN** the system SHALL persist `reminderMinutes=0` on the event

#### Scenario: Create event without reminder
- **WHEN** a user creates a new event and does not activate a reminder
- **THEN** the system SHALL persist `reminderMinutes=-1` on the event

#### Scenario: Edit event reminder
- **WHEN** a user edits an existing event and changes the reminder from 15 to 30 minutes
- **THEN** the system SHALL update `reminderMinutes=30` on the event

#### Scenario: Deactivate reminder
- **WHEN** a user edits an existing event and deactivates the reminder
- **THEN** the system SHALL update `reminderMinutes=-1` on the event

#### Scenario: Reminder not available for follow-ups and respites
- **WHEN** a user creates or edits a follow-up (eventType=10) or respite (eventType=20)
- **THEN** the reminder bell button SHALL be disabled in the UI
- **AND** the `reminderMinutes` value SHALL remain -1

### Requirement: Reminder Bell Button in Event Creation
The event creation panel (`NewEventPanel`) SHALL provide a bell icon button for configuring the reminder. A blue bell indicates no reminder (-1). Clicking the bell opens a popup menu with predefined values: Bei Beginn (0), 5 Min., 10 Min., 15 Min., 30 Min., 1 Std., 2 Std., 1 Tag. On selection, the bell turns green. The button SHALL only be enabled when the event type is EVENT (30).

#### Scenario: Bell button enabled for events
- **WHEN** a user selects the event type "Termin" (EVENT)
- **THEN** the bell button SHALL be enabled and clickable

#### Scenario: Bell button disabled for non-events
- **WHEN** a user selects "Wiedervorlage" (FOLLOWUP) or "Frist" (RESPITE)
- **THEN** the bell button SHALL be disabled and display the blue (inactive) icon

#### Scenario: Activate reminder via bell
- **WHEN** a user clicks the blue bell and selects "15 Min." from the popup
- **THEN** the bell icon SHALL change to green
- **AND** `reminderMinutes` SHALL be set to 15

#### Scenario: Deactivate reminder via bell
- **WHEN** a user clicks the green bell and selects a deactivation option
- **THEN** the bell icon SHALL change back to blue
- **AND** `reminderMinutes` SHALL be reset to -1

### Requirement: Reminder Bell Button in Event Editing
The event editing dialog (`EditOrDuplicateEventDialog`) SHALL display the same bell icon button. When editing an existing event, the bell state SHALL reflect the current `reminderMinutes` value (green if >= 0, blue if -1).

#### Scenario: Edit dialog shows active reminder
- **WHEN** a user opens the edit dialog for an event with `reminderMinutes=30`
- **THEN** the bell icon SHALL be green

#### Scenario: Edit dialog shows inactive reminder
- **WHEN** a user opens the edit dialog for an event with `reminderMinutes=-1`
- **THEN** the bell icon SHALL be blue

#### Scenario: Duplicate event preserves reminder
- **WHEN** a user duplicates an event with `reminderMinutes=15`
- **THEN** the new event's bell SHALL be green with 15 minutes pre-selected

### Requirement: Nextcloud VALARM Synchronization
When synchronizing events to Nextcloud via CalDAV, the system SHALL include a VALARM component in the VEVENT if `reminderMinutes >= 0`. The VALARM SHALL use ACTION:DISPLAY. For `reminderMinutes > 0`, the TRIGGER SHALL be `-PT{reminderMinutes}M`. For `reminderMinutes == 0`, the TRIGGER SHALL be `PT0M`. No VALARM SHALL be generated when `reminderMinutes == -1`.

#### Scenario: Sync event with reminder before start
- **WHEN** an event with `reminderMinutes=15` is created or updated
- **AND** the event is assigned to a CalendarSetup with Nextcloud sync enabled
- **THEN** the CalDAV VEVENT SHALL contain a VALARM component with `TRIGGER:-PT15M`

#### Scenario: Sync event with reminder at start
- **WHEN** an event with `reminderMinutes=0` is created or updated
- **AND** the event is assigned to a CalendarSetup with Nextcloud sync enabled
- **THEN** the CalDAV VEVENT SHALL contain a VALARM component with `TRIGGER:PT0M`

#### Scenario: Sync event without reminder
- **WHEN** an event with `reminderMinutes=-1` is created or updated
- **THEN** the CalDAV VEVENT SHALL NOT contain a VALARM component

#### Scenario: Update reminder triggers Nextcloud sync
- **WHEN** an event's `reminderMinutes` is changed from -1 to 30
- **THEN** the system SHALL update the Nextcloud event to include the VALARM component

### Requirement: In-App Reminder Notification
The desktop client SHALL run a background service that periodically checks for upcoming events with active reminders (`reminderMinutes >= 0`) and displays non-modal notification popups when a reminder is due. Reminders SHALL only be shown if the logged-in user is the event's assignee, or if the event has no assignee (empty/null). Dismissed reminder state is tracked in client memory only and lost on restart.

#### Scenario: Reminder fires at configured time
- **GIVEN** an open event with `beginDate=10:00`, `reminderMinutes=15`, and the logged-in user is the assignee
- **WHEN** the current time reaches 09:45 (±2 minutes tolerance)
- **THEN** the client SHALL display a non-modal notification showing the event summary, time, and location

#### Scenario: Reminder fires at event start
- **GIVEN** an open event with `beginDate=10:00`, `reminderMinutes=0`, and the event has no assignee
- **WHEN** the current time reaches 10:00 (±2 minutes tolerance)
- **THEN** the client SHALL display a non-modal notification

#### Scenario: Reminder not shown for other user's events
- **GIVEN** an open event with `reminderMinutes=15` and `assignee=otherUser`
- **WHEN** the logged-in user is NOT `otherUser`
- **THEN** no notification SHALL be displayed for this event

#### Scenario: Reminder shown for unassigned events
- **GIVEN** an open event with `reminderMinutes=15` and `assignee` is null or empty
- **WHEN** the reminder time arrives
- **THEN** the client SHALL display a notification regardless of which user is logged in

#### Scenario: Reminder not shown for completed events
- **GIVEN** an event with `done=true` and `reminderMinutes=15`
- **WHEN** the reminder time arrives
- **THEN** no notification SHALL be displayed

#### Scenario: Reminder not shown twice
- **GIVEN** a reminder notification has been displayed for an event
- **WHEN** the next polling cycle runs
- **THEN** the same reminder SHALL NOT be displayed again during the current client session

#### Scenario: Dismissed state lost on restart
- **GIVEN** a reminder was dismissed in a previous client session
- **WHEN** the client is restarted and the event is still upcoming
- **THEN** the reminder MAY be shown again

#### Scenario: Client not running at reminder time
- **GIVEN** the j-lawyer client is not running at the reminder time
- **WHEN** the client is started after the event's begin time
- **THEN** no belated reminder SHALL be displayed for past events

### Requirement: REST API Reminder Support
The REST API SHALL include the `reminderMinutes` field in calendar event responses and accept it in create/update requests. The field uses the same semantics: -1 = no reminder, 0 = at start, >0 = minutes before.

#### Scenario: GET returns reminderMinutes
- **WHEN** a client fetches calendar events via the REST API
- **THEN** each event object SHALL include the `reminderMinutes` field (integer)

#### Scenario: POST/PUT accepts reminderMinutes
- **WHEN** a client creates or updates an event via the REST API with `reminderMinutes=30`
- **THEN** the system SHALL persist the reminder value

#### Scenario: Backward compatibility
- **WHEN** a client using an older API version fetches events
- **THEN** the response SHALL NOT cause errors (field is simply present as additional data)
