## Context

The existing calendar notification logic lives in
`com.jdimension.jlawyer.services.CalendarService` (j-lawyer-server-ejb). Mail is dispatched
asynchronously: the service builds an `OutgoingMailRequest` and publishes it to a JMS queue,
where `OutgoingMailProcessor` (MDB) renders the HTML template and sends via SMTP. User
preferences are stored as a serialized blob on `AppUserBean` and read with
`getSettingAsBoolean(key, default)`.

Two facts drive this design:

1. `ArchiveFileReviewsBean` (table `case_events`) stores `assignee` but has **no creator
   column**. Existing columns: `id, eventType, summary, description, location, beginDate,
   endDate, done, archiveFileKey, assignee, calendar_setup, reminder_minutes`.
2. Both edit entry points already converge on one notification helper:
   - `updateReview(...)` → `publishUpdatedEventMail(review, aFile)` (line ~1294)
   - `markReviewDone(reviewId, done)` → `publishUpdatedEventMail(currentReview, aFile)` (line ~1226)
   This is the single place to add creator notifications.

## Goals / Non-Goals

- Goals:
  - Notify the original creator when someone else modifies or completes their entry.
  - Reuse the existing JMS + template + preference machinery; no new infrastructure.
  - No duplicate mails when creator and assignee are the same person.
- Non-Goals:
  - Backfilling a creator for pre-existing entries (legacy rows stay `NULL` → no creator mail).
  - Notifying on entry deletion (`removeReview`) — out of scope.
  - In-app / push notifications — mail only, matching the existing feature.
  - Notifying the creator on calendar migration bulk-edits (`migrateCalendar`, line ~1458) —
    that is an administrative move, not a user edit.

## Decisions

### 1. Track the creator on the entity

Add `createdBy` (`@Column(name = "created_by")`, `String`, nullable) to
`ArchiveFileReviewsBean`. Set it to `context.getCallerPrincipal().getName()` at every
review-creation site:
- `CalendarService.addReview` (primary path)
- `ArchiveFileService` review-creation sites (lines ~1286, ~1730) for entries created from
  forms/templates, so those entries also carry a creator.

Nullable + default `NULL` keeps the migration trivial and backward compatible. The notify
logic treats `NULL`/empty creator as "unknown" and skips.

The creator is **immutable after creation and server-authoritative**. `updateReview` receives
a detached, client-built bean and passes it straight to `edit()` (JPA merge), so a client that
does not round-trip `createdBy` would otherwise null the column. Some clients do exactly that:
`EditOrDuplicateEventDialog` constructs a fresh `ArchiveFileReviewsBean` and copies only
selected fields (not `createdBy`). Therefore `updateReview` reloads the persisted entity and
forces `review.setCreatedBy(oldReview.getCreatedBy())` before the merge. This one server-side
guard protects every update path (edit dialog, table toggle, AI tool, REST) without requiring
each client to preserve the field. The `markReviewDone` and `migrateCalendar` paths load and
mutate the managed entity in place, so they preserve `createdBy` inherently.

### 2. Detect "completion" vs generic "modification"

The creator message differs for completion ("erledigt") vs other edits ("geändert"). A
completion is a transition of `done` from `false` to `true`:
- `markReviewDone`: it early-returns if the value is unchanged, so a call with `done == true`
  is a completion; `done == false` is a re-open (treated as a modification).
- `updateReview`: compare the loaded `oldReview.isDone()` against the incoming
  `review.isDone()`; `false → true` is a completion, anything else is a modification.

Pass a `boolean completed` flag into the creator-notification helper so the message text can
branch. (A re-open, `true → false`, is reported as a modification.)

### 3. Single helper, suppression rules

Add `private void publishAuthorNotification(ArchiveFileReviewsBean review, ArchiveFileBean aFile, boolean completed)`
called from both `updateReview` and `markReviewDone` alongside the existing
`publishUpdatedEventMail`. It sends mail to the creator only when **all** hold:

- `createdBy` is non-empty (known creator), AND
- `createdBy != caller principal` (someone else made the change), AND
- `createdBy != assignee` — when the creator is also the assignee, the existing
  `publishUpdatedEventMail` already mails them; skip to avoid a duplicate, AND
- the creator `AppUserBean` exists and has a non-null email, AND
- the creator's `NOTIFICATION_EVENT_CALENDARENTRY_AUTHORED` preference is on. This preference
  defaults to **`false`** (opt-in), so `getSettingAsBoolean(..., false)` is used — unlike the
  existing assignee notification, which defaults to `true`.

The two notifications are independent recipients (assignee vs creator) and can both fire for
a single edit when they are different people — that is correct and intended.

### 4. New preference key + UI

Add to `UserSettingsKeys`:
`NOTIFICATION_EVENT_CALENDARENTRY_AUTHORED = "notify.event.calendarentry.authored"`.

The checkbox `chkEventCalendarEdited` already exists in `UserProfileDialog` (and its `.form`),
labelled "ein von mir erstellter Kalendereintrag durch andere verändert / erledigt wurde".
It still needs to be wired: loaded from / saved to the new key next to the existing
`chkEventCalendarEntry`, default **deselected** (`getSettingAsBoolean(..., false)`), since the
preference is opt-in.

### 5. Expose the creator to REST clients (read-only)

REST clients consume calendar entries as `RestfulDueDate` POJOs (`j-lawyer-io`), with versions
`V1`, `V4`, and `V6` (`V6 extends V4`). The newest is `V6`. Mapping lives in the cases
endpoints (`CasesEndpointV1/V4/V6`) and `CalendarEndpointV4`.

- Add a `createdBy` field (getter/setter) to `RestfulDueDateV6` only. Because `V6` is the
  latest calendar POJO and older versions must not change, `V1`/`V4` stay byte-identical.
- Populate it in the **entity→POJO** mapping at the `V6` read sites
  (`CasesEndpointV6`, `dd.setCreatedBy(rev.getCreatedBy())` next to `setAssignee`).
- Do **not** read it from the inbound **POJO→entity** mapping (`addReview`/create path): the
  creator is server-authoritative and set by the EJB from the caller principal. Accepting it
  from the client would let a caller spoof the creator. The field is effectively read-only.

This keeps the existing versioning rule ("never break existing API versions; new work in the
latest version") intact — the change is a single additive, read-only field on the newest
calendar POJO. If v1/v4 clients later need the creator, it can be added there additively
without breaking anything.

## Risks / Trade-offs

- **Legacy entries have no creator** → no creator notification until the entry is next saved.
  Acceptable; the feature is forward-looking. Documented as a non-goal.
- **Extra mail volume**: bounded by the suppression rules (modifier≠creator, creator≠assignee,
  preference on, email present); comparable to the existing assignee notification.
- **`markReviewDone` re-open path**: re-opening (`true → false`) sends a "modified"
  notification rather than a dedicated message — acceptable, keeps the message set small.

## Migration Plan

Flyway script `V3_6_0_4__CaseEventsAddCreatedBy.sql`. The column holds a principal id, so it
matches `security_users.principalId` (`VARCHAR(50) BINARY`) rather than the wider `assignee`
column:
```sql
ALTER TABLE case_events ADD `created_by` VARCHAR(50) BINARY DEFAULT NULL;
insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.4') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.4';
commit;
```
No data backfill. Rollback is dropping the column; the code tolerates a `NULL` creator, so a
partial deploy degrades gracefully (no creator mails) rather than failing.

## Open Questions

- The creator preference defaults to **off** (opt-in), whereas the existing assignee
  preference defaults to **on**. This asymmetry is intentional: the feature is new and
  should not silently start mailing creators until they opt in.
