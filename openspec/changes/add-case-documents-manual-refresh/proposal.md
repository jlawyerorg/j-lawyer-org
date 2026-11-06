# Change: Manual reload of the documents of an open case

## Why

The documents and folders of a case are loaded when the case is opened. Documents that colleagues, integrations using the REST API, the MCP server or other automation add later stay invisible until the user closes and reopens the case. Such documents are often needed right away, so periodic polling every 30 to 60 seconds would be too slow, and a server-driven refresh is planned for the web client (see issue #2384). The desktop client gets a manual reload for now.

## What Changes

- Add a reload button (F5 while the focus is in the documents tab) to the documents toolbar of a case in the desktop client.
- The reload runs in the background and loads the folders, the saved folder settings, the documents, the invoices linked to documents and the document tags with the same server calls as opening the case. The other case data is not touched.
- It keeps the selected documents, the sort order, the collapsed folders and attachments, the view (list or table) and the scroll position of the list. Folders already shown keep their selection, new folders follow their saved settings. An active search or tag filter is applied to the reloaded documents with the same terms, including the full-text search.
- The details and the preview of the selected documents are left as they are, so that unsaved input is kept. When the reload changes the selection, for example because a selected document is gone, preview and details are updated as after a click. When the document shown in the preview has new content (version, size or change date), the preview is loaded again in the list view if it still shows that document. In the table view, which shows no preview, the hidden preview is dropped, so that it is loaded when shown again. An edit in the table view is confirmed before the table is rebuilt.
- If this client changes documents, folders or document tags while the reload runs, or a search is started or still running, the result is discarded and loaded again, at most three times in total, then the user is asked to reload again. If the case is opened again, another case is opened or the case view is closed meanwhile, for example by switching to another module, the result is dropped.
- A running search that has been replaced by a newer one no longer applies its filter after loading the tags. Before, it only checked this before loading them.
- While a modal dialog is open, for example to create, rename or delete a folder, to pick a highlight color or for the progress of an upload, the loaded result is not applied yet. Such a dialog may still change the documents or folders it was opened for. The reload checks again every 300 ms and, once no modal dialog is open, goes through the change check above, so a change made in the dialog leads to loading again.
- Adding a document that is already shown, for example because the reload already brought it after it was saved, replaces the shown instance by the added one instead of adding it twice, because the caller may keep changing its instance.
- As a defensive measure, creating, renaming and deleting a folder apply the change to the folder with the same id among the folders shown now, in case the folder object stems from folders shown before a reload. A new folder is added once to that folder and to the given parent, because an upload of a directory tree keeps using the given parent.
- Building the folder tree no longer saves the selection of every unselected folder to the server again. The selection is taken from the saved settings or was saved when it was changed. This removes one server call per unselected folder when a case is opened and when a folder is created, renamed or deleted.
- No periodic server requests, no server change, no new remote method.

Not breaking.

## Impact

- Affected specs: `case-document-views`
- Affected code:
  - `j-lawyer-client`: `ArchiveFilePanel` (with `.form`), `CaseFolderPanel`, `FoldersListPanel`, `DocumentEntryPanel`
  - Tests: `ReloadedFolderSelectionTest`, `FolderLookupAfterReloadTest`, `ReloadedPreviewDecisionTest`, `DocumentReplaceTest` (client)
