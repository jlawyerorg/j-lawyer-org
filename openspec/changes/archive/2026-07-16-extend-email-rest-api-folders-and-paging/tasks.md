# Tasks

## 1. Spec
- [x] 1.1 Write delta spec for `email-rest-api` (ADDED Folder Visibility Management; MODIFIED Folder Listing and Message Listing)
- [x] 1.2 `openspec validate extend-email-rest-api-folders-and-paging --strict` passes

## 2. Implementation (already shipped & deployed)
- [x] 2.1 `includeHidden` / `includeCounts` query parameters on folder listing (both default `true`)
- [x] 2.2 Count-unknown (`-1`) sentinel + deferred per-folder counts endpoint `GET .../folders/{id}/counts`
- [x] 2.3 Folder hide/unhide (`PUT .../folders/{id}/hidden`) + hidden-folders listing (`GET .../hidden-folders`), incl. descendant cascade and desktop interop
- [x] 2.4 `offset` honored in the filtered message-listing branch (windowed from newest; `offset=0` unchanged)
- [x] 2.5 New POJOs `RestfulFolderCountsV7`, `RestfulSetHiddenRequestV7`
- [x] 2.6 Web client consumes the endpoints (lazy counts, hide UI, infinite scroll)
- [x] 2.7 Built and deployed to WildFly; verified via REST (admin:a) on a real IMAP mailbox

## 3. Archive (after review)
- [x] 3.1 `openspec archive extend-email-rest-api-folders-and-paging` to fold the delta into `specs/email-rest-api`
