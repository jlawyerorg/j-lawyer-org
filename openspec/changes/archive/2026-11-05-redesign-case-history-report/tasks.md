## 1. Server
- [x] 1.1 Add `searchHistory` to `ArchiveFileServiceRemote` (with JavaDoc)
- [x] 1.2 Add dynamic `search` query (incl. case visibility predicate) to `ArchiveFileHistoryBeanFacade(Local)`
- [x] 1.3 Implement `searchHistory` in `ArchiveFileService` (permission check in the query, before the limit)
- [x] 1.4 Add Flyway migration with indexes on `case_history`

## 2. Client
- [x] 2.1 Add `HistorySearchTableModel`
- [x] 2.2 Replace `HistorySearchThread` with `HistorySearchWorker` (SwingWorker, discard stale results)
- [x] 2.3 Rebuild `HistoryPanel` (.java + .form): filter bar, table, detail pane, actions, status line
- [x] 2.4 Persist the last preset, user and limit in the client settings
- [x] 2.5 Remove the obsolete tile panels and the now unused `RangeSlider`, `RangeSliderUI`, `FlexibleGridLayout`

## 3. Verification
- [x] 3.1 Manual test of filters, sorting, opening a case, export, limit hint, permissions, and light/dark theme

## 4. Visual polish
- [x] 4.1 Categorize entries (`HistoryCategory`) and show colored badges (`CategoryBadgeIcon`)
- [x] 4.2 Timeline table (`HistoryTable`, `HistoryTimelineRenderer`): day separators, two-line rows, avatars and display names, zebra stripes, sorting via the model
- [x] 4.3 Highlight search terms in the table and the detail card
- [x] 4.4 Key figures and activity chart (`ActivityChart`), category filter chips
- [x] 4.5 Translucent cards (`GlassCardPanel`), polished filter fields and search button, detail card
- [x] 4.6 Export without day separators (`HistoryExportTableModel`)
- [x] 4.7 Manual test in light/dark theme on the background image
