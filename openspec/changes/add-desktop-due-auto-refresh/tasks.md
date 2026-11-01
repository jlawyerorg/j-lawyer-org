## 1. Load bookkeeping

- [x] 1.1 In `ReviewsDueTimerTask`, number each load and record its start time, and read the calendar version before querying, falling back to `DesktopPanel.DUE_VERSION_UNKNOWN` when it cannot be read
- [x] 1.2 Add `DesktopPanel.dueEntriesLoaded(entries, compactView, calendarVersion, loadStartedAt, loadSequence)`. `ReviewsDueTimerTask` calls it instead of `renderDueEntries`
- [x] 1.3 In `dueEntriesLoaded`, drop the result of a load that started before the displayed one, record version and start time, and keep the entries for the search for every load path, instead of in individual callbacks
- [x] 1.4 Render only when the entries, the compact view or the day differ from what is displayed. Entries are compared by `ReviewDueEntry.getDisplaySignature()`, which covers every stored field of the calendar entry, because that bean is written back when the user acts on an entry
- [x] 1.5 Render the entries matching the current search text, sharing `getDueEntriesMatchingSearch` with `filterDueEntries`
- [x] 1.6 Let `filterDueEntries` determine the entries inside its EDT runnable, so a queued search cannot reinstall an older snapshot

## 2. Refresh task

- [x] 2.1 Add `DueEntriesRefreshTimerTask` (`volatile stopped`, `stop()`), modelled on `CalendarOverviewRefreshTimerTask`
- [x] 2.2 Skip the tick unless the desktop is the active editor - the inverse of `ReviewsDueTimerTask`'s guard
- [x] 2.3 Reload without comparing the version when the list was never loaded, its load started 15 minutes ago or longer, or on another day
- [x] 2.4 Otherwise read the calendar version and reload only when it differs from the loaded one. On a read failure keep the list and retry on the next tick
- [x] 2.5 Catch everything in `run()`, so that an exception cannot end the shared timer
- [x] 2.6 Reload through `DesktopPanel.reloadDueEntries()`, which runs a `ReviewsDueTimerTask` in the calling thread
- [x] 2.7 Schedule the task at 60 s on the existing `reviewsTimer`, so it does not overlap with the background and event-triggered loads, and stop it from the shutdown hook

## 3. Tests

- [x] 3.1 `DueEntriesRefreshTimerTaskTest`: inactive desktop, unchanged and changed version, unreachable server, day change, maximum age, never loaded, stopped task
- [x] 3.2 `ReviewDueEntryTest`: the display signature changes with shown fields and with stored fields of the review, such as description and reminder
- [x] 3.3 `CalendarVersionBumpTest`: `ArchiveFileReviewsBeanFacade.create`, `edit` and `remove` each change the calendar version
