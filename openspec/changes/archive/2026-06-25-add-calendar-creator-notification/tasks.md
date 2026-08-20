## 1. Persist the creator

- [x] 1.1 Add `createdBy` field (`@Column(name = "created_by")`, nullable `String`) with
      getter/setter to `ArchiveFileReviewsBean` (also copied in `getClone()`)
- [x] 1.2 Add Flyway migration `V3_6_0_4__CaseEventsAddCreatedBy.sql`
      (`ALTER TABLE case_events ADD created_by VARCHAR(50) BINARY DEFAULT NULL;` + version bump);
      `VARCHAR(50)` matches `security_users.principalId`
- [x] 1.3 Set `createdBy = context.getCallerPrincipal().getName()` in
      `CalendarService.addReview`
- [x] 1.4 Set `createdBy` at the `ArchiveFileService` review-creation sites (lines ~1286, ~1730)
- [x] 1.5 In `CalendarService.updateReview`, preserve the persisted creator
      (`review.setCreatedBy(oldReview.getCreatedBy())` before `edit`) so clients that rebuild the
      bean (e.g. `EditOrDuplicateEventDialog`) cannot reset `created_by` to null on merge

## 2. Preference key and client UI

- [x] 2.1 Add `NOTIFICATION_EVENT_CALENDARENTRY_AUTHORED = "notify.event.calendarentry.authored"`
      to `UserSettingsKeys`
- [x] 2.2 Add `chkEventCalendarEdited` checkbox to `UserProfileDialog` and its `.form`
      file (keep NetBeans GUI Builder consistent) — already added
- [x] 2.3 Wire `chkEventCalendarEdited`: load from the setting (default `false` / opt-in) and
      save it on apply, next to the existing `chkEventCalendarEntry`

## 3. Creator notification logic

- [x] 3.1 Add `publishAuthorNotification(review, aFile, boolean completed)` to `CalendarService`
      that builds the `OutgoingMailRequest` (subject/body branch on `completed`) and publishes it
- [x] 3.2 Apply suppression rules: known creator, creator != caller, creator != assignee,
      creator has email, preference on
- [x] 3.3 Call it from `updateReview` with `completed = !wasDone && review.isDone()`, where
      `wasDone` is captured from `oldReview` BEFORE the `edit()` merge (the merge mutates the
      managed `oldReview` in place, so reading it afterwards would report the new state)
- [x] 3.4 Call it from `markReviewDone` with `completed = done`

## 4. Expose creator via REST

- [x] 4.1 Add read-only `createdBy` field (getter/setter) to `RestfulDueDateV6`
- [x] 4.2 Populate it in the entity→POJO mapping at the `CasesEndpointV6` read sites
      (`dd.setCreatedBy(rev.getCreatedBy())`, next to `setAssignee`)
- [x] 4.3 Do NOT map it from the inbound POJO→entity path; the EJB sets the creator from the
      caller principal (read-only field, no client spoofing). The REST update path loads the
      existing entity, so `created_by` is preserved on edit.

## 5. Verification

- [ ] 5.1 A modifies an entry created by B (B not assignee) → B receives a "geändert" mail
- [ ] 5.2 A completes an entry created by B → B receives an "erledigt" mail
- [ ] 5.3 Creator edits their own entry → no creator mail
- [ ] 5.4 Creator is also the assignee → exactly one mail (no duplicate)
- [ ] 5.5 Creator preference off (the default), or no email, or legacy `created_by = NULL`
      → no creator mail
- [ ] 5.6 Creator preference explicitly enabled → creator mail is sent
- [ ] 5.7 A `RestfulDueDateV6` response carries `createdBy`; creating an entry via REST ignores
      any client-supplied `createdBy` and stores the caller as creator
- [x] 5.8 `openspec validate add-calendar-creator-notification --strict` passes
