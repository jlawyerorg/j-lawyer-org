## ADDED Requirements

### Requirement: Bidirectional Full Calendar Sync
The scheduled full calendar sync SHALL propagate changes in both directions: it SHALL push
local calendar entries to the configured CalDAV calendar and SHALL pull remote entries from
the same calendar. Both directions SHALL run in the same scheduled invocation and SHALL be
gated by the existing full-sync toggle.

#### Scenario: Push and pull share the scheduled run
- **WHEN** the scheduled full calendar sync fires
- **THEN** local changes are pushed to the CalDAV calendar
- **AND** remote changes are pulled from the CalDAV calendar
- **AND** both directions are skipped together if the full-sync toggle is disabled

#### Scenario: Pull consumes existing read APIs
- **WHEN** the pull direction executes
- **THEN** it retrieves all events of the configured calendar via the existing CalDAV read
  path
- **AND** it matches remote events to local entries by their calendar UID

### Requirement: Persisted Last-Observed CalDAV State
The system SHALL persist, per local calendar entry, the CalDAV state observed at the last
successful sync, so that changes on either side since that point are detectable.

#### Scenario: Cloud metadata written after every apply
- **WHEN** a local entry is pushed to CalDAV or updated from a pulled CalDAV event
- **THEN** the entry's stored CalDAV `ETag`, remote `LAST-MODIFIED` (in UTC) and local
  sync timestamp are updated in the same transaction as the entry itself

#### Scenario: Legacy entries without cloud metadata
- **WHEN** the first sync after upgrade encounters a local entry that has been pushed
  before but has no stored cloud metadata
- **THEN** the sync initialises the stored `ETag`, remote `LAST-MODIFIED` and local sync
  timestamp from the current remote state
- **AND** does not treat the entry as changed on either side

### Requirement: Last-Write-Wins Conflict Resolution
The sync SHALL resolve conflicts by *last write wins*: when both the local entry and its CalDAV counterpart have changed since the last successful sync, the side with the newer modification time wins and its state is applied to the other side.

#### Scenario: Only the remote side changed
- **WHEN** a remote event's `LAST-MODIFIED` is newer than the stored value and the local
  entry has not been modified since the stored sync timestamp
- **THEN** the local entry is updated from the remote event
- **AND** no push back to CalDAV is issued for this change

#### Scenario: Only the local side changed
- **WHEN** the local entry has been modified since the stored sync timestamp and the
  remote event's `LAST-MODIFIED` matches the stored value
- **THEN** the local entry is pushed to CalDAV
- **AND** the stored cloud metadata is refreshed to the new remote state

#### Scenario: Both sides changed, remote is newer
- **WHEN** both sides changed and the remote `LAST-MODIFIED` is later than the local
  modification time
- **THEN** the remote state is applied locally
- **AND** the previous local state is discarded

#### Scenario: Both sides changed, local is newer
- **WHEN** both sides changed and the local modification time is later than the remote
  `LAST-MODIFIED`
- **THEN** the local state is pushed to CalDAV
- **AND** the previous remote state is overwritten

#### Scenario: Both sides changed at the same instant
- **WHEN** both modification times are equal
- **THEN** the remote side wins (deterministic tie-break)

### Requirement: Import Of Foreign CalDAV Entries With File-Number Matching
The pull direction SHALL import CalDAV events whose UID is unknown locally. Before falling back to a default case, the sync SHALL scan the event's summary and description for the file number of any existing, non-archived case (matching the pattern used by `MailboxScannerTask` for incoming email: preload all file numbers and match by case-insensitive substring); if a match is found, the imported entry SHALL be attached to that case.

#### Scenario: File number found in summary
- **WHEN** an unknown remote event's summary contains the file number of an existing,
  non-archived case
- **THEN** a new local calendar entry is created and attached to that case

#### Scenario: File number found in description
- **WHEN** an unknown remote event's description contains the file number of an existing,
  non-archived case
- **THEN** a new local calendar entry is created and attached to that case

#### Scenario: File number belongs to an archived case
- **WHEN** the extracted file number resolves to an existing case that is archived
- **THEN** the file-number attempt is treated as unsuccessful
- **AND** the default-case fallback is applied

#### Scenario: No file number match
- **WHEN** no file number of any existing case is found in the event's summary or
  description
- **THEN** the default-case fallback is applied

### Requirement: Import Type Derived From Event Shape
The pull direction SHALL derive the j-lawyer calendar entry type for imported CalDAV events from the event's date/time shape: all-day events SHALL become `Wiedervorlage` (followup); events with a time-of-day start and end SHALL become `Termin` (event). Deadlines (`Frist`) SHALL NOT be created automatically.

#### Scenario: All-day event becomes a followup
- **WHEN** an unknown remote event has an all-day `DTSTART` (VALUE=DATE) and no time-of-day
- **THEN** a new local `Wiedervorlage` is created

#### Scenario: Time-ranged event becomes an appointment
- **WHEN** an unknown remote event has a `DTSTART`/`DTEND` with time-of-day
- **THEN** a new local `Termin` is created

#### Scenario: Frist is never auto-created
- **WHEN** the pull direction imports events
- **THEN** no entry of type `Frist` is created for any imported remote event

### Requirement: Target Calendar And Default Case Selection
The target `CalendarSetup` for an imported entry SHALL be the first `CalendarSetup` of the derived type (Wiedervorlage or Termin) whose `defaultArchiveFileKey` is set, ordered deterministically by id ascending. The selected `CalendarSetup`'s `defaultArchiveFileKey` SHALL be used as the target case when no file number match was found. The calendar-setup dialog SHALL let the user pick this default case, and the persistence layer SHALL store it per `CalendarSetup`.

#### Scenario: Target calendar and default case both used
- **WHEN** an unknown remote event has no matchable file number and at least one
  `CalendarSetup` of the derived type has a default case configured
- **THEN** the entry is created in the first such `CalendarSetup` (by id ascending)
- **AND** the entry is attached to that `CalendarSetup`'s default case

#### Scenario: Target calendar used even when file number matched
- **WHEN** an unknown remote event's file number matched an existing case and at least one
  `CalendarSetup` of the derived type has a default case configured
- **THEN** the entry is created in the first such `CalendarSetup` (by id ascending)
- **AND** the entry is attached to the file-number-matched case, not to the default case

#### Scenario: No CalendarSetup of the derived type has a default case
- **WHEN** an unknown remote event's derived type has no `CalendarSetup` with a default
  case configured
- **THEN** the entry is not imported
- **AND** the sync run logs a warning identifying the remote UID, the event summary and
  the derived type

#### Scenario: Default case selectable in the setup dialog
- **WHEN** the user opens the calendar-setup dialog
- **THEN** a picker for the default case is available
- **AND** the selected case is persisted with the `CalendarSetup` configuration

### Requirement: Remote Deletion Marks Local Entry As Done
The sync SHALL treat a disappeared CalDAV event as a completion signal: when a CalDAV event that was previously synced is missing on the remote side and the corresponding local entry has not been modified locally since the last sync, the local entry SHALL be marked as done (`done=1`) rather than deleted. When the local entry has been modified locally in the meantime, the local side wins and the entry is re-pushed to CalDAV.

#### Scenario: Remote deletion with no local change
- **WHEN** a UID that was previously synced is missing on the CalDAV side
- **AND** the corresponding local entry has not been modified since the stored sync
  timestamp
- **THEN** the local entry's `done` flag is set to true (with a completion timestamp)
- **AND** the local entry is not deleted

#### Scenario: Remote deletion with a newer local change
- **WHEN** a UID that was previously synced is missing on the CalDAV side
- **AND** the corresponding local entry has been modified since the stored sync timestamp
- **THEN** the local entry is preserved and remains not done
- **AND** it is pushed to CalDAV so that the remote calendar is re-populated

#### Scenario: Done entries are not re-imported by later syncs
- **WHEN** a subsequent full sync runs after a local entry has been marked done because of a
  remote deletion
- **THEN** the entry is not re-created on CalDAV and is not re-processed as a foreign import

### Requirement: Sync Loop Prevention
Applying a pulled change to a local entry SHALL NOT trigger a push of that same change back
to CalDAV within the same sync cycle.

#### Scenario: Pulled update does not re-push
- **WHEN** the pull direction applies a remote change to a local entry
- **THEN** no event-driven push (`eventUpdated`) is emitted for that apply
- **AND** the entry's stored cloud metadata reflects the remote state that was just applied

#### Scenario: Independent local edit after a pull is still pushed
- **WHEN** the user edits a locally imported entry after the pull has completed
- **THEN** the normal event-driven push runs as before

### Requirement: Imported Entries Have No Creator
Calendar entries imported from CalDAV SHALL be created without a recorded creator, so that
the existing creator-notification suppression in `calendar-notifications` prevents notifying
a wrongly-identified user.

#### Scenario: Imported entry has no creator
- **WHEN** the pull direction creates a new local entry from a remote event
- **THEN** the entry's creator is empty
- **AND** subsequent modifications by other users trigger no creator notification for that
  entry

### Requirement: Case History For Remote-Originated Changes
Every calendar-entry change produced by the pull direction SHALL create a case-history entry on the associated case in the same transaction, so that users can trace which changes originated from Nextcloud and when.

#### Scenario: Import creates a history entry
- **WHEN** the pull direction creates a new local entry from a foreign remote event
- **THEN** a case-history entry is created on the target case that identifies the change
  as an import from Nextcloud and names the entry's summary
- **AND** the history entry is written in the same transaction as the imported entry

#### Scenario: Remote update creates a history entry
- **WHEN** the pull direction applies a remote change to an existing local entry
- **THEN** a case-history entry is created on the entry's case that identifies the change
  as a Nextcloud update
- **AND** the history entry is written in the same transaction as the applied update

#### Scenario: Remote deletion creates a history entry
- **WHEN** the pull direction marks a local entry as done because its CalDAV counterpart
  disappeared
- **THEN** a case-history entry is created on the entry's case that identifies the change
  as a completion triggered by a remote deletion in Nextcloud
- **AND** the history entry is written in the same transaction as the completion

#### Scenario: History author identifies the sync
- **WHEN** any of the above history entries is created
- **THEN** the history entry's author is recorded as a deterministic system marker for
  the cloud sync rather than an actual j-lawyer user

### Requirement: Full-Sync Toggle Governs Pull As Well
The existing server setting for the scheduled full calendar sync SHALL govern the pull direction identically to the push direction.

#### Scenario: Full-sync disabled
- **WHEN** the server setting for scheduled full calendar sync is disabled
- **THEN** neither push nor pull is executed by the scheduler

#### Scenario: Event-driven push unaffected
- **WHEN** the scheduled full sync is disabled but a user creates, updates or deletes a
  calendar entry locally
- **THEN** the event-driven push to CalDAV still runs as before
- **AND** no pull is executed
