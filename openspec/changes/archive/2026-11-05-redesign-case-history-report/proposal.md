# Change: Redesign the cross-case history report ("Aktenhistorie")

## Why
The special report "Aktenhistorie" (Auswertungen → Historie) is hard to use. The time range can only be chosen with a range slider scaled over the whole history, entries are rendered as nested tiles that cannot be sorted, filtered, copied or exported, and there is no text, case or user filter. On the server, the result limit is applied before the permission check, so the report silently returns too few entries.

## What Changes
- New remote method `ArchiveFileServiceRemote.searchHistory(...)`. It filters by date range, users, case (file number / name) and change text. The permission check runs before the limit is applied, and results are sorted newest first.
- Flyway migration adding indexes on `case_history(changeDate)` and `case_history(principal, changeDate)`.
- `HistoryPanel` is rebuilt as a filter bar plus a sortable table plus a detail pane, with actions to open the case, copy the change text and export to CSV.
- The tile-based panels `DayHistoryPanel`, `DayHistoryDetailsPanel`, `DayCaseHistoryDetailsPanel` and `DayCaseUserHistoryDetailsPanel` are removed.
- The existing methods `getHistoryInterval`, `getHistoryByDateInterval` and `getHistoryByPrincipalAndDateInterval` stay unchanged and compatible.

## Impact
- Affected specs: case-history-report (new)
- Affected code: `j-lawyer-client/.../editors/history/*`, `ArchiveFileServiceRemote`, `ArchiveFileService`, `ArchiveFileHistoryBeanFacade(Local)`, a new Flyway migration in `j-lawyer-server-entities`
