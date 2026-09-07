## ADDED Requirements

### Requirement: Live printer discovery
The desktop client SHALL obtain the printers currently reported by the client operating
system whenever printer choices are presented or an explicit printer target is resolved.
Persisted preference data MUST NOT be treated as evidence that a printer is currently
available.

#### Scenario: Printer becomes available after a location change
- **WHEN** the operating system reports a quick-access printer that was unavailable when
  the document context menu was previously opened
- **THEN** reopening the context menu shows that printer without requiring a client
  restart or a settings change

#### Scenario: Printer becomes unavailable
- **WHEN** the operating system no longer reports a saved quick-access printer
- **THEN** reopening the document context menu no longer shows an actionable entry for
  that printer

### Requirement: Unchanged default-printer direct action
The desktop client SHALL retain the existing `drucken (Standarddrucker)` action and SHALL
continue to resolve the operating-system default printer when that action is invoked.

#### Scenario: Direct print with no quick-access configuration
- **WHEN** no quick-access printer names are stored and the user invokes `drucken
  (Standarddrucker)` for supported selected documents
- **THEN** the documents follow the existing default-printer print path

#### Scenario: Default printer is also a favourite
- **WHEN** the current default printer's name is stored in the quick-access list
- **THEN** the context menu keeps the standard-printer action and does not add a duplicate
  named quick action for that printer

### Requirement: Device-local quick-access printer names
The desktop client SHALL allow the user to select printer names for quick access from the
printers currently reported by the operating system. It SHALL store only those names in
local client settings and MUST NOT store printer services, driver configuration,
capabilities, or availability on the server or in local settings.

#### Scenario: Configure an available quick-access printer
- **WHEN** the user selects a currently available printer in the quick-access settings
  and saves the dialog
- **THEN** the printer name is stored on that client and appears as a direct print action
  while the same name is currently available

#### Scenario: Different client installations
- **WHEN** the same j-lawyer user uses two client installations with different local
  quick-access selections
- **THEN** each installation shows its own quick-access entries and neither selection is
  synchronized through the server

#### Scenario: Previously selected printer is currently unavailable
- **WHEN** a saved quick-access name is absent from the current operating-system printer
  list and the user opens the quick-access settings
- **THEN** the name is marked as currently unavailable, remains saved unless the user
  removes it, and cannot be selected for printing

### Requirement: Bounded dynamic document context menu
The desktop client SHALL add a direct print action only for each currently available
quick-access printer and SHALL provide a separate on-demand action for all other current
printers. It MUST NOT place the complete operating-system printer list directly in the
document context menu.

#### Scenario: Many installed printers
- **WHEN** the operating system reports many printers but only two non-default printers
  are selected and currently available for quick access
- **THEN** the document context menu contains direct named actions for those two printers
  and the separate all-printers chooser action, but no direct actions for the remaining
  printers

#### Scenario: Context menu is reopened repeatedly
- **WHEN** the user opens and closes the same document context menu multiple times
- **THEN** the dynamic print actions reflect the latest printer list and do not accumulate
  duplicate menu entries or action handlers

### Requirement: On-demand selection from current printers
The desktop client SHALL provide `drucken (anderen Drucker auswählen …)` to display a
single-selection chooser containing all printers currently reported by the operating
system in deterministic, case-insensitive name order.

#### Scenario: Select another printer
- **WHEN** the user opens the chooser, selects a current printer, and confirms
- **THEN** the selected supported documents are sent through the explicit named-printer
  path for that printer

#### Scenario: Cancel printer selection
- **WHEN** the user cancels the chooser
- **THEN** no document is retrieved for printing and no print job is started

#### Scenario: No current printer
- **WHEN** the operating system reports no available printer
- **THEN** the client disables the chooser action or displays a clear no-printer message
  and does not start a print job

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

