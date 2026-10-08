# Tasks

## 1. Tool definition
- [x] 1.1 Make `query` optional and add `toDate` to the `search_emails` definition; extend the description (listing without search term, date ranges)
- [x] 1.2 Add the `MAX_MAIL_PAGES` constant
- [x] 1.3 Adapt the `search_emails` branch of `formatToolCallSummary()` (listing / date range)

## 2. Implementation
- [x] 2.1 Accept a missing or blank `query` and pass `null` to `listMessages`
- [x] 2.2 Parse and validate `toDate` (end of day, not before `fromDate`)
- [x] 2.3 Send `fromDate` one day earlier and filter hits client-side against the exact range
- [x] 2.4 Page per folder via `offset` until enough hits, older than `fromDate`, folder exhausted or page cap; set `scanLimitReached`, warn when the backend does not page

## 3. Verification
- [x] 3.1 Build `j-lawyer-client`
- [x] 3.2 Assistant chat: "die neuesten 5 E-Mails im Postfach X" (IMAP and, if available, Graph)
- [x] 3.3 Assistant chat: "alle E-Mails, die im Monat M empfangen wurden" for a past month, including messages of the first day of the month
- [x] 3.4 Existing search with `query` still works; `toDate` before `fromDate` returns an error
