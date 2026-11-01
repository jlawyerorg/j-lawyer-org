## ADDED Requirements

### Requirement: Automatic Refresh Of The Due List
The due list on the desktop SHALL refresh itself while the desktop is the active view. It SHALL use the calendar version as the fast change signal, and it SHALL additionally reload on a day change and after a maximum age, because the calendar version does not reflect every change that affects the list. It SHALL perform no periodic change checks while the desktop is not the active view.

#### Scenario: Entry changed outside this client
- **WHEN** a follow-up, deadline or appointment within the list's date windows is created, changed or closed outside this client, for example by a colleague or through the REST API, while the desktop is the active view
- **THEN** the due list shows the change within about one minute without the user pressing refresh

#### Scenario: Version read before the change is committed
- **WHEN** the calendar version is read after a write increased it but before the writing transaction is committed
- **THEN** the list may still load the old entries together with the new version
- **AND** the change appears with the reload after the maximum age, unless another change of the calendar version triggers an earlier reload

#### Scenario: Nothing changed
- **WHEN** the desktop stays the active view, no calendar entry changes and the list was loaded less than 15 minutes ago on the same day
- **THEN** each interval only reads the calendar version, and the list is not reloaded

#### Scenario: Day change
- **WHEN** the date has changed since the load the list shows was started
- **THEN** the list reloads without comparing the calendar version, because its date windows have moved

#### Scenario: Maximum age
- **WHEN** the load the list shows was started 15 minutes ago or longer while the desktop is the active view
- **THEN** the list reloads at the next interval without comparing the calendar version, so that changes the version does not reflect, such as case data or permissions, are picked up

#### Scenario: Desktop not active
- **WHEN** the desktop is not the active view
- **THEN** no periodic change checks are made
- **AND** the existing background reload every 15 minutes continues as before

#### Scenario: Filter changed
- **WHEN** the user changes the user filter, the date ranges or the compact view of the due list
- **THEN** the list reloads immediately, as before

#### Scenario: Refresh fails
- **WHEN** reading the calendar version or reloading the list fails, for example because the server is unreachable
- **THEN** the previously displayed entries remain visible
- **AND** the next interval tries again

#### Scenario: Unchanged entries are not rendered again
- **WHEN** an automatic reload on the same day and with unchanged compact view returns the same entries as the list already shows, including every stored field of their calendar entries
- **THEN** the list is not rebuilt, so that its scroll position and an ongoing interaction with an entry are not disturbed

#### Scenario: Other loads always render
- **WHEN** the list is loaded for any other reason, for example after the user postponed an entry in this client, after a filter change, on a manual refresh or by the background reload
- **THEN** the list is rebuilt as before, even if the loaded entries equal the displayed ones, because the displayed entries may have been changed in place

#### Scenario: Overdue marking after a day change
- **WHEN** a reload after a day change returns the same entries
- **THEN** the list is rebuilt anyway, so that entries are marked as overdue relative to the new day

#### Scenario: Automatic reload keeps search text and tab
- **WHEN** the list reloads automatically while a search text is entered or an event type tab is selected
- **THEN** the list shows the reloaded entries that match the search text
- **AND** the selected tab stays selected as long as it still has entries

#### Scenario: Overlapping loads
- **WHEN** two loads of the list overlap, for example an automatic reload and a reload after a filter change
- **THEN** the list shows the result of the load that started last

#### Scenario: Client shutdown
- **WHEN** the client is closed
- **THEN** the refresh timer is stopped and cancelled
