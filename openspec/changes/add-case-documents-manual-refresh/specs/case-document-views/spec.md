## ADDED Requirements

### Requirement: Manual Reload of Case Documents
The documents toolbar of a case in the desktop client SHALL offer a button that loads the documents and folders of the case again from the server, also available as F5 while the focus is in the documents tab. The reload SHALL run in the background and SHALL NOT query the server periodically. It SHALL keep the selected documents, the sort order, the collapsed folders and attachments, the chosen view and the selection of the folders already shown, SHALL apply an active search or tag filter to the reloaded documents, and SHALL NOT discard unsaved input in the document details as long as the selected documents stay the same. When the reload changes the selection, for example because a selected document is gone or now lies in an unselected folder, details and preview SHALL be updated as after a click.

#### Scenario: Document added outside this client
- **WHEN** a document is added to an open case outside this client, for example by a colleague or through the REST API, and the user presses the reload button
- **THEN** the document is shown in the case without closing and reopening it

#### Scenario: State kept
- **WHEN** a user has collapsed a folder, selected a document and sorted by name, and reloads the documents
- **THEN** the folder stays collapsed, the document stays selected and the list stays sorted by name

#### Scenario: Active filter
- **WHEN** a quick filter or tag filter is active and the reload brings new documents
- **THEN** only the new documents that match the filter are shown

#### Scenario: Previewed document changed
- **WHEN** the document shown in the preview got a new version outside this client and the user reloads the documents
- **THEN** the preview shows the new content
- **AND** unsaved input in the document details is kept

#### Scenario: Local change during the reload
- **WHEN** the user changes documents, folders or document tags in this client while the reload is running
- **THEN** the loaded result is discarded and the documents are loaded again, so that the change is not undone

#### Scenario: Dialog open when the reload finishes
- **WHEN** the reload finishes while a modal dialog is open, for example to create, rename or delete a folder, to pick a highlight color or for the progress of an upload
- **THEN** the loaded result is applied only after the dialog has been closed
- **AND** a change made in the dialog is kept, if necessary by loading again

#### Scenario: Document saved during the reload
- **WHEN** a document saved in this client is already contained in the reloaded documents when this client adds it to the list
- **THEN** the document is shown once
