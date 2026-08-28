# Tasks

## 1. Tool registration
- [x] 1.1 Add `MAX_MAIL_RESULTS`, `MAX_SEARCH_FOLDERS` and `MAX_MAIL_BODY_CHARS` constants to `ToolRegistry`
- [x] 1.2 Register `list_mailboxes`, `search_emails` and `get_email` in the static `TOOLS` initializer with `RISK_LOW`
- [x] 1.3 Add the three `case` branches to `execute()`
- [x] 1.4 Add the three `case` branches to `formatToolCallSummary()`

## 2. Tool implementations
- [x] 2.1 Implement `executeListMailboxes` on top of `UserSettings.getMailboxes(principalId)`
- [x] 2.2 Implement `executeSearchEmails` (mailbox/folder selection, per-folder `listMessages` with search term, date-descending sort, result cap, warnings)
- [x] 2.3 Implement `executeGetEmail` (message lookup, HTML-to-text via `CommonMailUtils.html2Text`, body cap, attachment metadata)
- [x] 2.4 Add the helpers `getAccessibleMailboxes`, `findAccessibleMailbox`, `selectSearchFolders`, `appendStringArrayJson` and the `MailHit` holder

## 3. Verification
- [ ] 3.1 Build `j-lawyer-client`
- [ ] 3.2 Verify in the assistant chat: list mailboxes, search a known mail, read it, search with `scope="all"`
- [ ] 3.3 Verify the three tools do NOT appear in the permission table of `UserProfileDialog` (RISK_LOW)
- [ ] 3.4 Verify edge cases: no hits, unknown `mailboxId`, user without `aiAgentRole`, unreachable mailbox reported in `warnings`
