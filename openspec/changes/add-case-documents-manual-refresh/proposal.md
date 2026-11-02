# Change: Manual reload of the documents of an open case

## Why

The documents and folders of a case are loaded when the case is opened. Documents that colleagues, integrations using the REST API, the MCP server or other automation add later stay invisible until the user closes and reopens the case. Such documents are often needed right away, so periodic polling every 30 to 60 seconds would be too slow, and a server-driven refresh is planned for the web client (see issue #2384). The desktop client gets a manual reload for now.

## What Changes

- Add a reload button (F5 while the focus is in the documents tab) to the documents toolbar of a case in the desktop client.
- The reload runs in the background and loads the folders, the saved folder settings, the documents, the invoices linked to documents and the document tags with the same server calls as opening the case. The other case data is not touched.
- It keeps the selected documents, the sort order, the collapsed folders and attachments, the view (list or table) and the scroll position of the list. Folders already shown keep their selection, new folders follow their saved settings. An active search or tag filter is applied to the reloaded documents with the same terms, including the full-text search.
- The details and the preview of the selected documents are left as they are, so that unsaved input is kept. When the reload changes the selection, for example because a selected document is gone, preview and details are updated as after a click. When the previewed document has new content (version, size or change date), the preview is loaded again. An edit in the table view is confirmed before the table is rebuilt.
- If this client changes documents, folders or document tags while the reload runs, or a search is started or still running, the result is discarded and loaded again, at most three times in total, then the user is asked to reload again. If the case is opened again or another case is opened meanwhile, the result is dropped.
- A running search that has been replaced by a newer one no longer applies its filter after loading the tags. Before, it only checked this before loading them.
- Creating, renaming and deleting a folder apply the change to the folder with the same id among the folders shown now. The modal folder dialogs keep the folder they were opened for, and a reload finishing while such a dialog is open replaces the folders shown.
- Building the folder tree no longer saves the selection of every unselected folder to the server again. The selection is taken from the saved settings or was saved when it was changed. This removes one server call per unselected folder when a case is opened and when a folder is created, renamed or deleted.
- No polling, no server change, no new remote method.

Not breaking.

## Impact

- Affected specs: `case-document-views`
- Affected code:
  - `j-lawyer-client`: `ArchiveFilePanel` (with `.form`), `CaseFolderPanel`, `FoldersListPanel`, `DocumentEntryPanel`
  - Tests: `ReloadedFolderSelectionTest`, `FolderLookupAfterReloadTest` (client)
