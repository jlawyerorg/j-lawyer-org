## 1. Entity helpers and DTOs

- [x] 1.1 Extract `public static String formatCaption(int eventType, Date begin, Date end, SimpleDateFormat dateTime, SimpleDateFormat date, SimpleDateFormat time)` from `ArchiveFileReviewsBean.toString(...)`; have the instance method delegate
- [x] 1.2 Extract `public static String eventTypeName(int eventType)` from `ArchiveFileReviewsBean.getEventTypeName()`; have the instance method delegate
- [x] 1.3 Add `CalendarEntryDTO` to `j-lawyer-server-api/.../services/`, following `CaseLinkDTO` conventions (Serializable, `serialVersionUID = 1L`, no-arg ctor plus documented projection ctor, `ArchiveFileBean.composeFileNumber` in the ctor)
- [x] 1.4 Give `CalendarEntryDTO` id-based `equals`/`hashCode` matching the entity's contract
- [x] 1.5 Add `getCaption()`, `getEventTypeName()` and `hasEndDateAndTime()` to `CalendarEntryDTO`, delegating to the helpers from 1.1/1.2
- [x] 1.6 Add `static CalendarEntryDTO fromEntity(ArchiveFileReviewsBean)` to `CalendarEntryDTO`
- [x] 1.7 Add `CalendarEntriesResponse` (`version`, `unchanged`, `entries`, `truncated`) to the same package

## 2. Server query and service

- [x] 2.1 Add `findCalendarEntries(List<String> groupIds, boolean openOnly, Date fromDate, Date toDate, int limit)` to `ArchiveFileReviewsBeanFacade` and its Local interface, as a `SELECT NEW` projection in the style of `CaseLinkFacade`
- [x] 2.2 Implement the four-branch visibility predicate in JPQL over `ArchiveFileGroupsBean`; guard the empty-group case so no `IN ()` is generated
- [x] 2.3 Keep `JOIN e.archiveFileKey c` an INNER join so entries without a case stay excluded
- [x] 2.4 Use `e.beginDate <= :to AND COALESCE(e.endDate, e.beginDate) >= :from` for the windowed variant
- [x] 2.5 Add `getCalendarEntries(Date fromDate, Date toDate, boolean openOnly, int limit, long knownVersion)` to `CalendarService`, `CalendarServiceLocal` and `CalendarServiceRemote`, with English JavaDoc on the remote interface
- [x] 2.6 Read the version before querying, return it in the response, and short-circuit on a match as `MessagingService.getMessagesSince` does
- [x] 2.7 Copy the date arguments defensively instead of mutating them
- [x] 2.8 Intern repeated `calendarId`, `calendarName`, `caseLawyer` and `assignee` strings before returning
- [x] 2.9 Leave `getAllOpenReviews()` and `searchReviews(...)` unchanged

## 3. Change version

- [x] 3.1 Add an `AtomicLong` calendar version to `SingletonService`, seeded with `System.currentTimeMillis()`
- [x] 3.2 Add `getCalendarVersion()` and `bumpCalendarVersion()` with `@Lock(LockType.READ)` on both
- [x] 3.3 Declare both on `SingletonServiceLocal`, and `getCalendarVersion()` on `SingletonServiceRemote` with JavaDoc stating that it is opaque, compared for equality only, per-JVM, and reset on restart
- [x] 3.4 Override `create`, `edit` and `remove` in `ArchiveFileReviewsBeanFacade` to bump the version

## 4. Database

- [x] 4.1 Add `V3_6_0_31__CaseEventsDateIndexes.sql` with indexes on `case_events(done, beginDate)` and `case_events(beginDate)`, bumping `jlawyer.server.database.version` to `3.6.0.31`

## 5. Client render path

- [x] 5.1 Add `caseId`, `caseNumber`, `caseName`, `caseReason` and `caseLawyer` fields to `CalendarEvent`; existing getters prefer `caseDto` when set, else the fields
- [x] 5.2 Retype `cachedEvents`, `doneEvents`, `removeById` and `findById` in `CalendarPanel` to `CalendarEntryDTO`
- [x] 5.3 Add `setData(Collection<CalendarEntryDTO>)` and `addCalendarEvent(CalendarEntryDTO)`, keeping the entity-typed overloads as converting wrappers so `ConflictingEventsDialog` and the `NewEventEntryCallbacks` implementors stay untouched
- [x] 5.4 Fix the null-`cachedEvents` and null-`calendarSetup` NPEs in `addCalendarEvent`
- [x] 5.5 In the "Akte bearbeiten" and edit-dialog actions, load the case via `ArchiveFileServiceRemote.getArchiveFile(ce.getCaseId())` when `getCaseDto()` is null
- [x] 5.6 Add a `setCalendarItems(CalendarPanel, Collection<CalendarEntryDTO>)` overload to `ThreadUtils`
- [x] 5.7 Extend the `IntervalChangedEvent` handler so it reloads open entries for the new interval plus one month on each side, not only done entries

## 6. Client refresh

- [x] 6.1 Add `CalendarEntryRowIdentifier` (`caseId`, `reviewId`, `caption`) alongside the existing `ArchiveFileReviewsRowIdentifier`, which stays as is
- [x] 6.2 Switch `ArchiveFileReviewsSearchThread` to the new service method, remove the `Thread.sleep(750)`, and install the fully built table model once
- [x] 6.3 Replace `id.getArchiveFileDTO()` in the panel's open-case and double-click handlers with a lazy case fetch, keeping `selectEvent(reviewId)`
- [x] 6.4 Make `ArchiveFileReviewsOverviewPanel` implement `PopulateOptionsEditor`; `populateOptions()` loads the calendar sheet interval and reloads the list version-guarded
- [x] 6.5 Add `CalendarOverviewRefreshTimerTask` modelled on `ReviewsDueTimerTask` (`volatile stopped`, `stop()`, `knownVersion`, `forceRefresh()`, heartbeat counter)
- [x] 6.6 Skip the tick unless the panel is the active editor; note this is the inverse of `ReviewsDueTimerTask`'s guard
- [x] 6.7 Set `knownVersion` only after a successful load, so failures stay fail-open
- [x] 6.8 Reload unconditionally every tenth tick
- [x] 6.9 Preserve the selected entry by id and the scroll position across a reload
- [x] 6.10 Schedule the timer at 60 s in the constructor and cancel it from a shutdown hook, as `DesktopPanel` does
- [x] 6.11 Add a right-aligned `lblStatus` to the header row in `ArchiveFileReviewsOverviewPanel.form`, next to `lblPanelTitle`, and keep the generated `initComponents` code in sync so the panel stays editable in the NetBeans GUI builder
- [x] 6.12 Show the time of the last successful load in `lblStatus` after every load, automatic or manual
- [x] 6.13 Show a truncation notice in `lblStatus` when the result hit the limit, naming that the far future was dropped
- [x] 6.14 Indicate in `lblStatus` when a refresh failed, keeping the last successful load time visible
- [x] 6.15 Keep the manual refresh button working as a forced refresh that bypasses the version guard
- [x] 6.16 Subscribe the panel to `TYPE_REVIEWADDED` and `TYPE_REVIEWUPDATED`, coalescing bursts into one delayed reload without remote calls on the publishing thread

## 7. Verification

- [ ] 7.1 Capture one refresh with `tcpdump` before the change and record the byte count
- [ ] 7.2 Repeat the capture after phase A and after phase B, and correct the estimates in `proposal.md` with the measured values
- [ ] 7.3 Leave the panel open for ten idle minutes and confirm only version checks plus exactly one heartbeat appear
- [ ] 7.4 Assert the projected query returns the same event ids as `getAllOpenReviews()` for a user with restricted groups and for a user with none
- [ ] 7.5 Assert a case with an owner group the user is not in and no case-group rows stays visible
- [ ] 7.6 Assert `CalendarEntryDTO.getCaption()` equals `ArchiveFileReviewsBean.toString()` for all three event types, null end date, same-day and multi-day
- [ ] 7.7 Call `renderEvents()` twice and assert `cachedEvents.size()` is unchanged
- [ ] 7.8 Confirm an appointment spanning a month boundary renders in both months
- [ ] 7.9 Confirm no NPE for a null calendar setup and for a null case reference
- [ ] 7.10 Create an entry through each of the five writing services and confirm the version changes each time
- [ ] 7.11 Restart WildFly and confirm clients reload rather than going stale
- [ ] 7.12 With two clients, confirm a change in one appears in the other within one interval
- [ ] 7.13 Navigate away and confirm polling stops; navigate back and confirm the view is current
- [ ] 7.14 Switch editors about fifty times and confirm the timer thread count does not grow
- [ ] 7.15 Confirm selection by entry id and scroll position survive a refresh, and the calendar sheet does not jump to today
- [ ] 7.16 Open the panel in the NetBeans GUI builder after the `.form` change and confirm it loads without warnings and the header row renders as intended
- [ ] 7.17 Confirm `lblStatus` shows the load time after an automatic refresh, a truncation notice past the limit, and a failure notice with the last good time when the server is unreachable
- [ ] 7.18 `EXPLAIN` the generated SQL before and after the migration and confirm the new index is used
- [ ] 7.19 Apply `V3_6_0_31` to a production-sized copy and record the runtime
- [ ] 7.20 Build server, client and EAR, then deploy
