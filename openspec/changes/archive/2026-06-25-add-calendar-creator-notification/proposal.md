# Change: Notify Calendar Entry Creator on Modification or Completion

## Why

Today, calendar-entry mail notifications only flow to the **assignee** ("verantwortlich"):
when person A creates or edits an entry assigned to person B, B is notified
(`CalendarService.addReview` / `publishUpdatedEventMail`). There is no notification in the
other direction. When person A modifies or completes ("erledigt") an entry that person B
originally created, B finds out nothing — even though B has a legitimate interest in knowing
their entry was changed or closed out.

Implementing this is blocked by a data gap: `ArchiveFileReviewsBean` (table `case_events`)
records the `assignee` but **not who created the entry**, so there is currently no way to
identify the recipient for a creator-facing notification.

## What Changes

- Persist the creator of every calendar entry: add a `created_by` column to `case_events`
  and a `createdBy` field on `ArchiveFileReviewsBean`, set to the caller principal whenever
  an entry is created.
- Send a mail notification to the **creator** when an entry they authored is modified or
  completed by a different user, covering both edit paths
  (`CalendarService.updateReview` and `CalendarService.markReviewDone`).
- Distinguish "geändert" (modified) from "erledigt" (completed) in the notification subject
  and body.
- Suppress the creator notification when it would be redundant or unwanted: when the modifier
  is the creator, when the creator is also the assignee (already covered by the existing
  assignee notification), when the creator has no email, when the creator's preference is off,
  or when the creator is unknown (legacy entries with `created_by = NULL`).
- Add a per-user preference `notify.event.calendarentry.authored` (default **off** / opt-in)
  with a checkbox in the user profile dialog, mirroring the existing
  `notify.event.calendarentry` toggle.
- Expose the creator to REST clients: add a read-only `createdBy` field to the latest
  calendar REST POJO (`RestfulDueDateV6`) and populate it in the entity→POJO mapping. The
  field is server-populated and therefore ignored on inbound (POJO→entity) mapping; existing
  v1/v4 representations are left unchanged.

## Impact

- Affected specs: `calendar-notifications` (new capability), `calendar-api` (REST creator field)
- Affected code:
  - `j-lawyer-server-entities` — `ArchiveFileReviewsBean` (+ new field), Flyway migration
    `V3_6_0_4__CaseEventsAddCreatedBy.sql`
  - `j-lawyer-server-ejb` — `CalendarService.addReview`, `updateReview`, `markReviewDone`,
    `publishUpdatedEventMail` (+ creator-notification helper); creator also set at the two
    `ArchiveFileService` review-creation sites
  - `j-lawyer-server-common` — `UserSettingsKeys` (new key)
  - `j-lawyer-client` — `UserProfileDialog` (+ `.form`): new preference checkbox
  - `j-lawyer-io` — `RestfulDueDateV6` (+ read-only `createdBy` field), entity→POJO mapping
    in `CasesEndpointV6`
- No EJB remote-interface signature changes; the REST change is additive (new read-only
  field on the latest calendar POJO only).
