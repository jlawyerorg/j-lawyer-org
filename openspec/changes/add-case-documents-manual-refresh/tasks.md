## 1. Reload

- [x] 1.1 Add `cmdReloadDocuments` to the documents toolbar in `ArchiveFilePanel` and its `.form`, and bind F5 to it while the focus is in the documents tab (`WHEN_ANCESTOR_OF_FOCUSED_COMPONENT`)
- [x] 1.2 Load the root folder (`getArchiveFile`), `getCaseFolderSettings`, `getDocuments`, `getInvoices`, `getDocumentTagsForCase` and, for an active full-text search, the search hits in a `SwingWorker`, and disable the button meanwhile
- [x] 1.3 Drop the result if the case has been opened again or another case has been opened (a counter increased by `setArchiveFileDTO`). Load again (at most three attempts) if `CaseFolderPanel.getChangeCount()` changed, a search was started or is still running. The change count is increased by every change of documents, folders or document tags in `CaseFolderPanel`, including favorite and highlights set in `DocumentEntryPanel` (`documentChanged`), not by sorting, filtering or folder selection
- [x] 1.4 Replace the folders with `CaseFolderPanel.replaceRootFolder`, which keeps the selection and the collapsed state of the folders already shown and applies the saved settings to new folders
- [x] 1.5 Replace the documents with `CaseFolderPanel.replaceDocuments`, which keeps the selection, the sort order, the collapsed attachments and the scroll position, then apply the last search with its terms
- [x] 1.6 Leave details and preview of the selected documents as they are, update both like a click if the selection has changed, and compare the document actually shown in the preview (`getShownPreviewDocumentId`): if it has new content (`hasNewContent`), reload the preview in the list view when it still targets that document, and in the table view, which shows no preview, drop the hidden preview so that it is loaded when shown again. Confirm an edit in the table view before rebuilding (`CaseFolderPanel.stopDocumentEditing`)
- [x] 1.7 In `FoldersListPanel.setRootFolder`, restore unselected folders without saving the setting again
- [x] 1.8 Let a search worker that has been replaced skip applying its filter after loading the tags
- [x] 1.9 Let `FoldersListPanel.folderAdded`, `folderRemoved` and `folderUpdated` apply the change to the folder with the same id among the folders shown now (`currentFolder`), as a defensive measure in case a folder object stems from folders shown before a reload. `folderAdded` adds the new folder once to the shown parent and to the given one (`addFolder`), because an upload of a directory tree keeps using the given parent to find the folders it created
- [x] 1.10 Apply a loaded result only while no modal dialog is showing (`isModalDialogShowing`), otherwise check again every 300 ms with a `javax.swing.Timer`, then run the case and change checks as usual. Drop the result also when the case view is no longer displayable, e.g. after switching to another module
- [x] 1.11 Let `CaseFolderPanel.addDocument` replace a document that is already shown by the added instance (`replaceDocument`) and rebuild the list keeping the scroll position, instead of adding it twice

## 2. Tests

- [x] 2.1 `ReloadedFolderSelectionTest`: folders already shown keep their selection, new folders follow their saved settings and are selected without settings
- [x] 2.2 `FolderLookupAfterReloadTest`: a folder from the folders shown before a reload resolves to the shown folder with the same id, a folder that is not shown stays as given, and a new folder is added once to the given and to the shown parent
- [x] 2.3 `ReloadedPreviewDecisionTest`: new version, size or change date of the shown document make its preview outdated, an unchanged or vanished document does not (`ArchiveFilePanel.hasNewContent`)
- [x] 2.4 `DocumentReplaceTest`: a document with the id of a shown one replaces that instance, a new one does not
