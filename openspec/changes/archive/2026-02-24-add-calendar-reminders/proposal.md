# Change: Add Calendar Reminders for Events

## Why
Calendar events (Termine, eventType=30) currently have no reminder/alarm functionality. Users must manually remember upcoming appointments. Adding reminders with configurable lead time enables proactive in-app notification, and synchronizing VALARM components to Nextcloud ensures reminders also trigger on mobile devices and desktop calendar clients.

## What Changes
- Add reminder attribute (`reminderMinutes` / column `reminder_minutes`, default -1 = no reminder, 0 = at start, >0 = minutes before) to the `ArchiveFileReviewsBean` entity and `case_events` database table
- Extend the event creation/editing UI (`NewEventPanel`, `EditOrDuplicateEventDialog`) with a bell icon toggle button (only for eventType=EVENT): blue bell = no reminder, click opens popup with minute options, green bell = reminder active
- Serialize VALARM components into iCalendar VEVENT objects during Nextcloud CalDAV sync
- Add a client-side background timer that checks for upcoming events and shows in-app reminder notifications
- Extend the REST API `RestfulDueDateV6` POJO and relevant endpoints to include reminder data

## Impact
- Affected specs: `calendar-api` (MODIFIED: response format includes reminder fields), new spec `calendar-reminders`
- Affected code:
  - `j-lawyer-server-entities`: `ArchiveFileReviewsBean.java` (new field)
  - `j-lawyer-server-api`: `CalendarServiceRemote.java` (updated JavaDoc)
  - `j-lawyer-server/j-lawyer-server-ejb`: `CalendarService.java`, `CalendarSyncService.java` (VALARM serialization)
  - `j-lawyer-cloud`: `NextcloudCalendarConnector.java` (VALARM in VEVENT)
  - `j-lawyer-client`: `NewEventPanel.java`, `EditOrDuplicateEventDialog.java`, new `ReminderNotificationService.java`
  - `j-lawyer-server-io`: REST POJO update for v7
  - Database: DDL migration script for `case_events` table
