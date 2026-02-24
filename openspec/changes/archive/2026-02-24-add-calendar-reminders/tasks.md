## 1. Data Model & Persistence
- [x] 1.1 Add `reminderMinutes` field (int, default -1) to `ArchiveFileReviewsBean` entity with JPA `@Column(name = "reminder_minutes")`
- [x] 1.2 Create SQL migration script: `ALTER TABLE case_events ADD COLUMN reminder_minutes INT NOT NULL DEFAULT -1`
- [x] 1.3 Update `RestfulDueDateV6` POJO to include `reminderMinutes` field

## 2. Server-Side Logic
- [x] 2.1 Update `CalendarService.addReview()` and `updateReview()` to persist reminderMinutes
- [x] 2.2 Update `CalendarServiceRemote` JavaDoc for changed methods
- [x] 2.3 Extend `CalendarSyncService` to pass reminderMinutes to Nextcloud connector

## 3. Nextcloud CalDAV Sync
- [x] 3.1 Extend `NextcloudCalendarConnector.putEvent()` to accept reminderMinutes parameter
- [x] 3.2 Generate VALARM with ACTION:DISPLAY and TRIGGER:-PT{minutes}M when reminderMinutes >= 0
- [x] 3.3 For reminderMinutes=0, use TRIGGER:PT0M (alarm at event start)
- [x] 3.4 Omit VALARM entirely when reminderMinutes == -1
- [x] 3.5 Ensure VALARM is included in both create and update operations

## 4. Client UI — Event Creation (NewEventPanel)
- [x] 4.1 Add bell icon JButton (`cmdReminder`) to NewEventPanel Java code
- [x] 4.2 Create JPopupMenu with reminder options: Bei Beginn (0), 5 Min. (5), 10 Min. (10), 15 Min. (15), 30 Min. (30), 1 Std. (60), 2 Std. (120), 1 Tag (1440)
- [x] 4.3 Implement icon toggle: blue bell (inactive, -1) ↔ green bell (active, ≥0)
- [x] 4.4 Enable bell button only when eventType=EVENT is selected; reset to -1 on type change
- [x] 4.5 Pass reminderMinutes to server when saving event
- [x] 4.6 **MANUAL**: Developer added JButton `cmdReminder` to `NewEventPanel.form` in NetBeans GUI Builder

## 5. Client UI — Event Editing (EditOrDuplicateEventDialog)
- [x] 5.1 Add bell icon JButton (`cmdReminder`) to EditOrDuplicateEventDialog Java code
- [x] 5.2 Reuse same JPopupMenu and icon toggle logic
- [x] 5.3 Populate bell state from existing event's reminderMinutes value
- [x] 5.4 Enable bell button only for eventType=EVENT
- [x] 5.5 **MANUAL**: Developer added JButton `cmdReminder` to `EditOrDuplicateEventDialog.form` in NetBeans GUI Builder

## 6. Client In-App Notification
- [x] 6.1 Create `ReminderNotificationTimerTask` background timer (60s interval) in client
- [x] 6.2 Query open events with reminderMinutes >= 0 where (beginDate - reminderMinutes) is within notification window; filter to events where assignee equals logged-in user or assignee is empty/null
- [x] 6.3 Display non-modal notification popup for triggered reminders
- [x] 6.4 Track shown reminders in client memory (HashSet of event IDs) to avoid duplicate notifications; state is lost on restart
- [x] 6.5 Integrate service startup into client application lifecycle (DesktopPanel)

## 7. REST API
- [x] 7.1 Verify `reminderMinutes` is serialized/deserialized in v4 and v6 calendar endpoints
- [x] 7.2 Ensure backward compatibility — default value -1 means existing API consumers are unaffected
