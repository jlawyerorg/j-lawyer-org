# Change: Extend the email REST API with folder visibility, deferred counts, and filtered pagination

## Why
The web client shipped folder hiding, fast (count-deferred) folder loading, and
infinite-scroll message paging. These added new `/v7/email` endpoints and query
parameters, and changed the paging semantics of the message-listing endpoint. None of
this is captured in the `email-rest-api` spec, so the spec no longer matches the deployed
API. This change backfills the spec to restore it as the source of truth.

## What Changes
- Document optional `includeHidden` and `includeCounts` query parameters on folder
  listing. Both default to `true` (unchanged behavior). `includeCounts=false` skips the
  per-folder IMAP SELECT and returns `unreadCount`/`totalCount` as `-1` (count-unknown)
  sentinels, so the tree renders fast and counts are filled in lazily.
- Add folder-visibility endpoints: `PUT .../folders/{folderId}/hidden`,
  `GET .../hidden-folders`, and per-folder `GET .../folders/{folderId}/counts`. Hidden
  state is stored per mailbox, cascades to descendants, and is interoperable with the
  desktop client.
- Clarify that `offset` pagination applies **within filtered results**
  (`search`/`unreadOnly`/`sinceDate`), enabling infinite scroll. For `offset=0` the result
  is byte-for-byte identical to prior behavior, so existing clients are unaffected.

## Impact
- Affected specs: email-rest-api
- Affected code (already implemented, deployed, and verified via REST):
  - `j-lawyer-server-ejb/.../services/EmailService.java`, `EmailServiceLocal.java`
  - `j-lawyer-io/.../rest/v7/EmailEndpointV7.java`, `EmailEndpointLocalV7.java`
  - `j-lawyer-io/.../rest/v7/pojo/RestfulFolderCountsV7.java`, `RestfulSetHiddenRequestV7.java`
  - Web client: `communication/email.service.ts`, `communication/email.component.ts`
- Backward compatibility: every new query parameter defaults to prior behavior, and no
  existing endpoint response changes for existing callers (desktop client always sends
  `offset=0`).
