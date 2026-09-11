## ADDED Requirements

### Requirement: Live printer discovery
The desktop client SHALL obtain the printers currently reported by the client operating
system whenever printer choices are presented or an explicit printer target is resolved.
Persisted preference data MUST NOT be treated as evidence that a printer is currently
available.

For context-menu presentation, "available" means reported by the operating system in the
latest completed printer discovery result. It does not guarantee that the printer is
physically connected, online, reachable, or able to accept a job.

#### Scenario: Printer becomes available after a location change
- **WHEN** the operating system reports a favourite printer that was unavailable when
  the document context menu was previously opened
- **THEN** reopening the context menu shows that printer without requiring a client
  restart or a settings change

#### Scenario: Printer becomes unavailable
- **WHEN** the operating system no longer reports a saved favourite printer
- **THEN** reopening the document context menu no longer shows an actionable entry for
  that printer

### Requirement: Responsive printer discovery
The desktop client SHALL NOT block opening the archive-file context menu on a synchronous
operating-system printer discovery call. Printer discovery for menu presentation SHALL
use the latest completed printer snapshot or another non-blocking implementation, and
slow discovery MUST NOT run on the Swing Event Dispatch Thread.

#### Scenario: Printer lookup is slow while opening the context menu
- **WHEN** operating-system printer discovery is slow, blocked, or waiting on a network
  printer
- **THEN** the archive-file context menu opens without waiting for that discovery call
  to finish

#### Scenario: No completed printer snapshot exists yet
- **WHEN** the archive-file context menu is opened before any printer discovery result is
  available
- **THEN** the client keeps the existing default-printer menu behaviour for that opening
  and may show favourite entries on a subsequent opening after discovery completes

#### Scenario: Snapshot is refreshed asynchronously
- **WHEN** a background printer discovery completes after the context menu was previously
  opened
- **THEN** the next context-menu opening uses the refreshed printer list without requiring
  a client restart

### Requirement: Unchanged default-printer direct action
The desktop client SHALL retain the existing `drucken (Standarddrucker)` action and SHALL
continue to resolve the operating-system default printer when that action is invoked.

#### Scenario: Direct print with no favourite configuration
- **WHEN** no favourite printer names are stored and the user invokes `drucken
  (Standarddrucker)` for supported selected documents
- **THEN** the documents follow the existing default-printer print path

#### Scenario: Default printer is also a favourite
- **WHEN** the current default printer's name is stored in the favourite list
- **THEN** the context menu keeps the standard-printer action and does not add a duplicate
  named favourite action for that printer

### Requirement: Device-local favourite printer names
The desktop client SHALL allow the user to select printer names as favourites from the
printers currently reported by the operating system and MAY allow an optional local
display label for each selected printer. It SHALL store only those names and display
labels in local client settings and MUST NOT store printer services, driver
configuration, capabilities, or availability on the server or in local settings.

#### Scenario: Configure an available favourite printer
- **WHEN** the user selects a currently available printer in the favourites settings
  and saves the dialog
- **THEN** the printer name is stored on that client and appears as a direct print action
  while the same name is currently available

#### Scenario: Configure a display label
- **WHEN** the user gives a selected favourite printer the display label `Faxdrucker`
- **THEN** the document context menu uses `Faxdrucker` as the visible text while print
  dispatch still resolves the target by the stored operating-system printer name

#### Scenario: Different client installations
- **WHEN** the same j-lawyer user uses two client installations with different local
  favourite selections
- **THEN** each installation shows its own favourite entries and neither selection is
  synchronized through the server

#### Scenario: Previously selected printer is currently unavailable
- **WHEN** a saved favourite name is absent from the current operating-system printer
  list and the user opens the favourites settings
- **THEN** the name is shown as a disabled currently-unavailable entry, remains saved
  unless the user removes it, and cannot be selected for printing

### Requirement: Ordinary users may configure local printer favourites
The desktop client SHALL allow ordinary users to configure printer favourites for their
local client installation. Access to the printer favourites dialog MUST NOT require
`adminRole` or `sysAdminRole`, because the setting is not shared server configuration.

#### Scenario: Ordinary user configures favourites
- **WHEN** a user without `adminRole` or `sysAdminRole` opens the printer favourites
  settings dialog
- **THEN** the dialog is available and the user can save local favourite printer names
  and optional display labels

### Requirement: Bounded opt-in document context menu
The desktop client SHALL keep the current single default-printer menu entry while no
currently available non-default favourite printer exists. It SHALL show a `Drucken`
submenu only when at least one currently available non-default favourite printer
exists. It MUST NOT place the complete operating-system printer list directly in the
document context menu.

#### Scenario: No favourites configured
- **WHEN** the operating system reports multiple printers and no favourite printer
  names are stored
- **THEN** the document context menu keeps the existing `drucken (Standarddrucker)` action
  and does not add a printer submenu or any named printer actions

#### Scenario: No available non-default favourite
- **WHEN** the only stored favourite printer is currently unavailable or is the current
  default printer
- **THEN** the document context menu keeps the existing `drucken (Standarddrucker)` action
  and does not add a printer submenu or any named printer actions

#### Scenario: Many installed printers
- **WHEN** the operating system reports many printers but only two non-default printers
  are selected and currently available as favourites
- **THEN** the document context menu contains a `Drucken` submenu with the standard
  printer action first and direct named actions for those two printers, but no direct
  actions for the remaining printers

#### Scenario: Context menu is reopened repeatedly
- **WHEN** the user opens and closes the same document context menu multiple times
- **THEN** the dynamic print actions reflect the latest printer list and do not accumulate
  duplicate menu entries or action handlers

### Requirement: Explicit named-printer dispatch without fallback
The desktop client SHALL resolve an explicitly selected printer by its exact live service
name before dispatch and SHALL use the resolved target for every selected supported
document. It MUST NOT silently redirect an explicit print request to the default printer
or another printer.

#### Scenario: Named target handles mixed supported documents
- **WHEN** the user selects supported PDF and LibreOffice-printable documents and chooses
  a named printer
- **THEN** each document is dispatched to that named target through its existing
  format-appropriate print path

#### Scenario: Named target disappears before dispatch
- **WHEN** the selected printer can no longer be resolved immediately before the print
  batch starts
- **THEN** the client reports that the named printer is unavailable and does not
  intentionally dispatch the batch to another printer

#### Scenario: Named print fails during a batch
- **WHEN** a named print job fails after one or more documents may already have been
  dispatched
- **THEN** the client identifies the selected target and failure, does not retry on the
  default printer, and does not claim that physical printing was atomic

### Requirement: Existing document eligibility and cleanup behaviour
Adding printer selection SHALL NOT broaden the set of document types considered directly
printable and SHALL preserve the existing temporary-file cleanup and user-visible error
handling except where target-specific errors require clearer wording.

#### Scenario: Unsupported selected document
- **WHEN** the selection contains a document type that the current direct-print feature
  does not support
- **THEN** the document remains unsupported under the same eligibility rules and printer
  selection does not bypass those rules

#### Scenario: Named print completes or fails
- **WHEN** a named print attempt finishes successfully or with an error
- **THEN** temporary files are cleaned up according to the existing direct-print lifecycle
  and no document content is persisted in the printer preference
