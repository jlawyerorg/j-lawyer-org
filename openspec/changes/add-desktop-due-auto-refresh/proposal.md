# Change: Automatic refresh of the desktop due list

## Why

The due list on the desktop ("Fällig": follow-ups, deadlines and appointments) reloads every 15 minutes only while the desktop is *not* the active view, plus immediately after changes made in this client. Entries that colleagues, integrations using the REST API or the jlawyer CLI create or close stay invisible for as long as the user keeps the desktop open - which is where follow-ups are usually worked off.

The chronological calendar overview solved the same problem with the calendar version (`SingletonServiceRemote.getCalendarVersion()`, change `add-calendar-overview-auto-refresh`). The desktop due list can reuse it without any server change.

## What Changes

- Add `DueEntriesRefreshTimerTask` to the client. Every 60 seconds, and only while the desktop is the active view, it decides whether to reload the due list.
- The calendar version is only the fast change signal: the list reloads when the version differs from the one the displayed list was loaded with.
- Because the version cannot observe everything, the list also reloads when the day has changed since the last load (its date windows move on without any write), and once the last load is 15 minutes old, at the next check, so changes to case data, permissions and the like are picked up while the desktop stays in front.
- `ReviewsDueTimerTask` numbers each load, records its start time and reads the calendar version before it queries. It hands all three to `DesktopPanel` together with the loaded entries, for every load path (start-up, background reload, manual refresh, reload after filter changes and after this client's own changes). `DesktopPanel` drops the result of a load that started before the displayed one and keeps the entries for the search in one place.
- A reload on the same day and with unchanged compact view that returns the same entries as displayed, including every stored field of their calendar entries, does not rebuild the list, so its scroll position and an ongoing interaction with an entry are not disturbed. After a day change the list is always rebuilt, so that overdue entries are marked relative to the new day. The original reason why the background reload skips while the desktop is in front was not to interrupt user interactions.
- An automatic reload keeps the search text: the list shows the reloaded entries that match it. The selected event type tab is kept, as before.
- Unchanged: the 15-minute background reload while the desktop is not the active view, the immediate reload after the user changes the user filter, the date ranges or the compact view, and the reload after changes made in this client.

Not breaking. No server change.

## Impact

- Affected specs: `desktop-panel`
- Affected code:
  - `j-lawyer-client`: `DesktopPanel`, `ReviewsDueTimerTask`, `ReviewDueEntry`, `DueEntriesRefreshTimerTask` (new)
  - Tests: `DueEntriesRefreshTimerTaskTest`, `ReviewDueEntryTest` (client), `CalendarVersionBumpTest` (server EJB)
