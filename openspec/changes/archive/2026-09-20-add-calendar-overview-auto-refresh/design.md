## Context

The cross-case calendar overview is a cached singleton editor: `EditorsRegistry` keeps editor instances in a `HashMap`, so `ArchiveFileReviewsOverviewPanel`'s constructor runs once per client session and re-navigating to the module triggers nothing at all today. The only refresh path is `cmdRefreshActionPerformed`, which starts `ArchiveFileReviewsSearchThread` (calling `getAllOpenReviews()`) and `calendarPanel1.reloadDoneEvents()`.

The payload measurements that motivate this change are estimates derived from field counts and the jboss-marshalling River format, not measurements. They must be confirmed by packet capture before being quoted (see tasks 7.1-7.3).

## Goals / Non-Goals

Goals:
- Automatic refresh of both tabs without user interaction.
- Steady-state network cost near zero when nothing changes.
- No regression in which entries a user sees.

Non-Goals:
- Server push (WebSocket/SSE). The client is pull-only throughout; introducing a push channel is out of proportion here.
- A cluster-safe change signal. j-lawyer runs single-node; the `@Singleton` counter is per-JVM and the heartbeat covers its blind spots.
- Reworking `getAllOpenReviews()` / `searchReviews(...)`. Other callers (`GlobalSearchDialog`, `ToolRegistry`, `ReviewsDueTimerTask`, `ReminderNotificationTimerTask`) depend on them and are out of scope, though they would benefit from the same projection later.

## Decisions

**Decision: project to a DTO rather than tune fetch modes.** Changing `ArchiveFileReviewsBean.archiveFileKey` to `FetchType.LAZY` would affect every existing caller and would break detached-entity serialization for the Swing client. A projection query leaves the entity untouched. The repo already established this pattern for the same reason: `CaseLinkFacade.findDTOsByCase` + `CaseLinkDTO`, whose JavaDoc states the folder-tree problem explicitly. `RestfulCalendarEventV8` is a field-level blueprint but lives in `j-lawyer-io`, which is not on the client classpath.

**Decision: resolve case visibility inside the JPQL query.** `SecurityUtils.getAllowedCasesForUser` scans all cases; `SecurityUtils.filterAllowedCases` builds `4 x N` bind parameters and its own JavaDoc scopes it to "a handful of candidates". The four-branch visibility rule translates directly into an EXISTS/NOT EXISTS predicate over `ArchiveFileGroupsBean`, giving one round trip and no IN list.
- Alternative considered: keep `getAllowedCasesForUser` and accept the scan. Rejected — it is the dominant server-side cost and would run on every poll.

**Decision: bump the version counter in `ArchiveFileReviewsBeanFacade`, not in `CalendarService`.** Five services write to the reviews facade across 17 call sites: `CalendarService`, `ClaimLedgerService`, `DunningDeadlineService`, `DunningService` and `ArchiveFileService`. Bumping only in `CalendarService`'s four mutators would silently miss dunning deadlines and claim-ledger follow-ups. The facade is the one choke point they all pass through.

**Decision: conditional fetch modelled on `MessagingService.getMessagesSince`.** The client sends the version it last saw; the server compares and returns `unchanged=true` without querying. This is atomic — it has no TOCTOU window between reading the version and reading the data, unlike "poll a counter, then fetch". A bare `getCalendarVersion()` on `SingletonServiceRemote` additionally makes the steady-state poll a single 8-byte call.

**Decision: `AtomicLong` with `@Lock(LockType.READ)` on both accessors.** The existing `latestInstantMessage*` setters in `SingletonService` run under the default `@Lock(WRITE)`. Copying that shape would serialize every calendar write through the singleton's write lock. Seeded with `System.currentTimeMillis()` and compared with `!=`, never `<`, so a server restart reliably forces a refresh rather than leaving clients stale.

**Decision: the list stays unbounded, the calendar sheet is windowed.** This is the deadline view of a legal practice-management system; a Frist 400 days overdue and still open is exactly the row a user must not stop seeing, and Verjährungsfristen are routinely set years out. The window is also not what makes this cheap — the conditional fetch is. The list is protected by a `limit` with a visible truncation hint and `ORDER BY beginDate ASC`, so truncation can only drop the far future. The calendar sheet can only render the visible interval anyway, so windowing it to ±1 month is a functional improvement.
- The window predicate uses `beginDate <= :to AND COALESCE(endDate, beginDate) >= :from`, which also fixes an existing defect: `searchReviews` filters on `beginDate` only, so a multi-day appointment starting before the window currently disappears from it.

**Decision: keep the entity-typed overloads on `CalendarPanel`.** `setData(Collection<ArchiveFileReviewsBean>)` is also called by `ConflictingEventsDialog`, and `addCalendarEvent(ArchiveFileReviewsBean)` is reached through the `NewEventEntryCallbacks` interface from `BulkSaveDialog`, `TimesheetDialog` and `NewEventEntryDialog`. Making `CalendarEntryDTO` the internal render type while keeping thin converting overloads leaves all four callers untouched.

**Decision: fetch the full `ArchiveFileBean` lazily at navigation time.** `CalendarEvent.caseDto` is consumed in six places; four are tooltip fields that need only scalars, and two open a case or the edit dialog and need the real entity. Those two already trigger a full case load immediately afterwards, so one `getArchiveFile(caseId)` round trip there is free. This mirrors `CaseLinkDTO`'s documented doctrine of fetching the full case only when the user actually navigates.

## Risks / Trade-offs

- **Visibility regression** — the four-branch rule is security-relevant, and the `NOT EXISTS` branch (owner group set, no `case_groups` rows, still visible) is the one most easily lost in translation. Getting it wrong widens access. Mitigation: explicit parity test as a user with restricted groups and as a user with none.
- **Caption drift** — column 0 is sorted as a *string* by `DateStringComparator`. If the DTO's caption is not byte-identical to `ArchiveFileReviewsBean.toString()`, sorting breaks silently. Mitigation: extract a static formatter on the entity and delegate from both; assert equality in a test.
- **`equals`/`hashCode`** — `CalendarPanel` calls `cachedEvents.contains(rev)`. Without id-based equality on the DTO, every render duplicates every entry. Mitigation: replicate the entity's id-based contract; assert `cachedEvents.size()` is stable across two `renderEvents()` calls.
- **Counter blind spots** — ACL changes, DB restores and direct SQL do not bump the counter, and the counter resets on restart. Mitigation: the ~10-minute unconditional heartbeat heals all of these; the restart case is additionally covered by the `!=` comparison and the timestamp seed.
- **Global granularity** — any calendar write anywhere refreshes every open panel. Accepted: after the projection a refresh is at most a few hundred KB, and a real office produces a handful of calendar writes per hour.
- **Timer lifecycle** — the editor is cached forever and has no dispose hook, so the only teardown is a shutdown hook, as in `DesktopPanel`.

## Migration Plan

`V3_6_0_31__CaseEventsDateIndexes.sql` adds two indexes and bumps `jlawyer.server.database.version` to `3.6.0.31`. Index creation on `case_events` is an online DDL operation in MySQL/InnoDB; time it against a production-sized copy first. No data migration, no rollback beyond dropping the indexes. All server changes are additive, so a new server works with an old client and vice versa — an old client simply never calls the new method.

**Decision: a visible status label in the header, not a tooltip.** The header row already lays out `jPanel1` (buttons), `jLabel18` (icon) and `lblPanelTitle`, with the title stretching; a right-aligned `lblStatus` fits there without restructuring the layout. `ArchiveFileReviewsOverviewPanel.form` is updated together with the generated code so the panel stays editable in the NetBeans GUI builder.

The label carries two things, because automatic refresh creates both needs:
- **Currency.** Once the view refreshes on its own, the user can no longer tell whether what they see is current; a silent view and a stale view look identical. The label therefore shows when the data was last loaded, which is the question the refresh button used to answer implicitly.
- **Truncation.** When the safety limit cuts the list, the user must be told, and must be told what was dropped - entries are ordered by date ascending, so truncation removes the far future, never the overdue past. A tooltip would hide exactly the information the user needs in order to trust the list.

- Alternatives considered: a tooltip on the title (hides the truncation warning behind a hover, and gives no currency cue at all); a separate status bar at the bottom of the panel (more layout churn, further from the refresh button the label relates to); showing nothing and relying on the refresh interval (leaves a stale view indistinguishable from a quiet one when the connection drops).

## Open Questions

None.
