# Change: Automatic refresh for the cross-case calendar overview

## Why

The "chronologisch" navigation entry (`ArchiveFileReviewsOverviewPanel`) shows follow-ups, deadlines and appointments across all cases, as a list and as a calendar sheet. It only ever refreshes when the user presses the refresh button, so entries created or changed by colleagues stay invisible. Users have asked for an automatic refresh.

Enabling a timer on the current data path would be prohibitively expensive:

- `CalendarService.getAllOpenReviews()` has no date filter and no limit, runs two `EntityManager.find()` calls per row, and calls `SecurityUtils.getAllowedCasesForUser(...)`, which UNION-scans the whole `cases` table on every invocation and matches with `List.contains()`.
- `ArchiveFileReviewsBean` has EAGER `@ManyToOne` associations to both `ArchiveFileBean` and `CalendarSetup`. `ArchiveFileBean` eagerly loads `Group` and `rootFolder`, and `CaseFolder.children` is EAGER and recursive. Every calendar row therefore ships a whole case, its owner group, its entire folder tree and the CalDAV credentials (`cloudUser`/`cloudPassword`, in clear text) to every Swing client.
- `case_events` has no index on `beginDate` or `done`.

That is roughly 3.7 KB per row where the UI displays about ten scalars — an estimated 2.9 MB per refresh for 800 open entries, 17 MB for 5000. A naive 60-second timer would move 174 MB/h to 1 GB/h per client.

The refresh becomes affordable through three independent factors, none of which suffices alone: a projection DTO shrinks the payload, a version token suppresses re-fetching when nothing changed, and polling only runs while the panel is the active editor.

## What Changes

- Add `CalendarEntryDTO` and `CalendarEntriesResponse` to `j-lawyer-server-api`, projecting only the columns the overview renders.
- Add a JPQL constructor-expression query to `ArchiveFileReviewsBeanFacade` that resolves case visibility inside the query instead of scanning the whole `cases` table.
- Add `CalendarServiceRemote.getCalendarEntries(...)`, which short-circuits and returns no entries when the caller's known version still matches the server's.
- Add an in-memory calendar version counter to `SingletonService`, bumped from `ArchiveFileReviewsBeanFacade.create/edit/remove` — the single choke point through which all five writing services pass.
- Add indexes on `case_events(done, beginDate)` and `case_events(beginDate)`.
- Make `ArchiveFileReviewsOverviewPanel` implement `PopulateOptionsEditor` so navigating to the module reloads it, and add a version-guarded 60-second timer that only polls while the panel is the active editor, plus a ~10-minute unconditional heartbeat.
- Load the calendar sheet for the visible interval ±1 month instead of loading every open entry; the list keeps its unbounded "all open entries" semantics.
- Subscribe the panel to `TYPE_REVIEWADDED` / `TYPE_REVIEWUPDATED` so this client's own changes appear immediately.
- Fix two latent NPEs in `CalendarPanel.addCalendarEvent` and remove the `Thread.sleep(750)` from `ArchiveFileReviewsSearchThread`.

Not breaking: `getAllOpenReviews()` and `searchReviews(...)` keep their current signatures and behaviour for their other callers.

## Impact

- Affected specs: `calendar-overview` (new capability)
- Affected code:
  - `j-lawyer-server-api`: `CalendarEntryDTO` (new), `CalendarEntriesResponse` (new), `CalendarServiceRemote`, `SingletonServiceRemote`
  - `j-lawyer-server-ejb`: `ArchiveFileReviewsBeanFacade` (+ `Local`), `CalendarService`, `SingletonService` (+ `Local`)
  - `j-lawyer-server-entities`: `ArchiveFileReviewsBean` (static helpers), `db/migration/V3_6_0_31__CaseEventsDateIndexes.sql` (new)
  - `j-lawyer-client`: `ArchiveFileReviewsOverviewPanel`, `ArchiveFileReviewsSearchThread`, `CalendarEntryRowIdentifier` (new), `CalendarOverviewRefreshTimerTask` (new), `de/costache/calendar/CalendarPanel`, `de/costache/calendar/model/CalendarEvent`, `ThreadUtils`
