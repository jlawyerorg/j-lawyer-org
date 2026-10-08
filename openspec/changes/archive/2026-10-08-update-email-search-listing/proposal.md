# Change: Let search_emails list messages without a search term and by date range

## Why
`search_emails` requires a non-empty `query`. Requests such as "show me the 5 newest e-mails in mailbox X" or "all e-mails received in October 2026" have no meaningful search term, so the assistant either invents one (which is matched literally and finds nothing) or gives up -- although `EmailServiceRemote.listMessages(..., searchTerm)` already accepts `null` and then returns the newest messages of a folder.

Date ranges are not possible either: the tool only has `fromDate`, and the backend always returns the *newest* `top` messages. For a range in the past, those newest messages are typically all after the range end, so the messages inside the range are never returned.

In addition, the IMAP backend maps `sinceDate` to `SentDateTerm(GT, ...)`, which JavaMail translates into `SENTSINCE d NOT SENTON d` -- messages sent *on* `fromDate` itself are excluded. "From 2026-10-01" therefore silently drops all messages of October 1st on IMAP mailboxes (Graph uses `ge` and is not affected).

## What Changes
- `query` of `search_emails` becomes optional. Without it, the newest messages of the selected folders are listed (still sorted date-descending, still capped by `maxResults` / 50).
- New optional parameter `toDate` (`yyyy-MM-dd`, inclusive up to end of day), consistent with the other date-range tools. `toDate` before `fromDate` is rejected.
- Client-side date filtering: `fromDate` is sent to the server one day earlier and the hits are filtered exactly against `[fromDate 00:00, toDate 23:59:59]` on the client. This compensates for the IMAP day-exclusion and works identically for IMAP and Graph. Messages without a date are excluded when a date range is given.
- Paging per folder: the client pages backwards through a folder (page size 50, using the existing `offset` parameter) until `maxResults` matching messages were collected, a page contains messages older than `fromDate`, the folder is exhausted, or a cap of 10 pages (500 messages) per folder is reached. Reaching the cap sets `scanLimitReached` in the result. Where the backend cannot page (Graph `$search` ignores `$skip`), repeated message references end the paging and a warning is added.
- The tool description explains listing without a search term and how to express calendar ranges (e.g. October 2026 = `fromDate=2026-10-01`, `toDate=2026-10-31`). The call summary shown in the chat reflects listing and date range.

## Impact
- Affected specs: `ai-assistant-integration` (requirement "E-Mail Mailbox Tools")
- Affected code: `j-lawyer-client/src/main/java/com/jdimension/jlawyer/client/assistant/ToolRegistry.java` (only file)
- No server change, no remote interface change, no database migration
- Not breaking: existing calls with `query` behave as before, except that messages sent on `fromDate` are now included on IMAP mailboxes
