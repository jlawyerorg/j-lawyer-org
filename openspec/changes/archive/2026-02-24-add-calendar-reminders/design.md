## Context
j-lawyer.org supports calendar events (Termine, Wiedervorlagen, Fristen) but has no reminder/alarm infrastructure. Events synced to Nextcloud via CalDAV contain no VALARM components. Users rely entirely on manual checking or Nextcloud's own defaults. This change adds first-class reminder support scoped to events (eventType=30), with bidirectional attribute sync and in-app notification.

## Goals / Non-Goals
- Goals:
  - Persist a configurable reminder lead time per event (minutes before start)
  - Display reminder configuration in event creation and editing UI
  - Emit VALARM components in CalDAV sync to Nextcloud
  - Show in-app popup notifications when a reminder fires
  - Expose reminder data via REST API
- Non-Goals:
  - Multiple reminders per event (single reminder is sufficient for v1)
  - Custom reminder actions (email, SMS) — only in-app display notification
  - Reminders for Wiedervorlagen (follow-ups) or Fristen (respites) — events only
  - Importing/parsing VALARM from Nextcloud back into j-lawyer (one-way sync remains)
  - Server-side push notifications to the client
  - Global default reminder setting per user — default is always "no reminder" (-1)
  - Server-side persistence of dismissed reminders — tracked in client memory only

## Decisions

### Data Model
- **Decision**: Add `reminderMinutes` (int, default -1) to `ArchiveFileReviewsBean`, mapped to column `reminder_minutes`
  - -1 = no reminder (default)
  - 0 = reminder at event start (valid per RFC 5545: `TRIGGER:PT0M`)
  - \>0 = reminder N minutes before event start
- **Alternatives considered**:
  - Separate reminder entity with foreign key → over-engineered for single reminder
  - Store as ISO 8601 duration string → less query-friendly, unnecessary flexibility
  - 0 as "no reminder" → inconsistent with CalDAV where PT0M is a valid trigger at event start
- **Rationale**: A single integer column is simple, queryable, and maps directly to VALARM TRIGGER duration. Using -1 as sentinel allows 0 to be a valid reminder time (at start), consistent with iCalendar/CalDAV semantics.

### Reminder Values
- **Decision**: Predefined popup values: Bei Beginn (0), 5 Min., 10 Min., 15 Min., 30 Min., 1 Std., 2 Std., 1 Tag (1440)
- **Rationale**: Matches common calendar app patterns; avoids freeform input errors

### VALARM Serialization
- **Decision**: Generate `VALARM` with `ACTION:DISPLAY` and `TRIGGER:-PT{x}M` in CalDAV VEVENT when `reminderMinutes >= 0`; omit VALARM when `reminderMinutes == -1`
- **Edge case**: `reminderMinutes=0` → `TRIGGER:PT0M` (alarm at event start, valid per RFC 5545)
- **Rationale**: DISPLAY alarms are universally supported by CalDAV clients; TRIGGER as negative duration before DTSTART is the iCalendar standard (RFC 5545)

### In-App Notification
- **Decision**: Client-side polling timer (60-second interval) checks for events with reminder within the next window; shows non-modal notification panel
- **Visibility rule**: Reminders are only shown if the logged-in user is the event's assignee, or the event has no assignee (empty/null)
- **Dismissed state**: Tracked in client memory (HashSet of event IDs); lost on restart — no server-side persistence
- **Alternatives considered**:
  - Server-side push via JMS/WebSocket → requires infrastructure changes, client doesn't support WebSocket
  - System tray notification → platform-dependent, not all OS support it reliably
  - Persist dismissed state server-side → unnecessary complexity, restart re-showing is acceptable
- **Rationale**: Client already has a background thread pattern (e.g. `DesktopPanel` refresh); polling open events with reminder is lightweight and consistent with existing architecture

### UI Design
- **Decision**: Bell icon button (JButton with icon) instead of dropdown
  - Blue bell icon = no reminder active (reminderMinutes == -1)
  - Click opens JPopupMenu with predefined minute options
  - On selection: icon changes to green bell, reminderMinutes is set
  - Click on green bell: option to deactivate (reset to -1) or change value
  - Only enabled for eventType=EVENT (30)
- **Alternatives considered**:
  - JComboBox dropdown → takes more horizontal space, less visually distinctive
- **Rationale**: Icon-only button is space-efficient, visual state (blue/green) provides instant feedback
- **.form files**: Changes to `NewEventPanel.form` and `EditOrDuplicateEventDialog.form` are done manually by the developer; the implementation only provides the Java code additions

### UI Scope
- **Decision**: Bell icon button only enabled for eventType=EVENT (30)
- **Rationale**: Follow-ups and respites are typically all-day markers without specific times; reminders are most useful for timed appointments

## Risks / Trade-offs
- **Client must be running for in-app reminders** → Mitigation: VALARM sync to Nextcloud provides mobile/desktop notifications independent of j-lawyer client
- **Polling interval of 60s may miss exact reminder time** → Mitigation: Check window covers reminderMinutes ± 2 minutes; acceptable for appointment reminders
- **Dismissed reminders re-trigger on restart** → Accepted: client-memory-only tracking; re-showing after restart is acceptable behavior

## Migration Plan
1. SQL DDL: `ALTER TABLE case_events ADD COLUMN reminder_minutes INT NOT NULL DEFAULT -1;`
2. Existing events get reminder_minutes=-1 (no reminder) — no behavioral change
3. Rollback: `ALTER TABLE case_events DROP COLUMN reminder_minutes;`

## Open Questions
None — all questions resolved.
