# Tasks

## 1. Tool registration
- [x] 1.1 Add `MAX_DOCUMENT_NAME_CHARS` and `MAX_NAME_CLASH_RETRIES` constants
- [x] 1.2 Register `save_email_to_case` with `RISK_MEDIUM` in the static `TOOLS` initializer
- [x] 1.3 Add the `case` branch to `execute()`
- [x] 1.4 Add the `case` branch to `formatToolCallSummary()`

## 2. Implementation
- [x] 2.1 Implement `executeSaveEmailToCase` (validation, mailbox ACL, archived-case guard, up-front tag validation, EML retrieval, clash resolution, addDocument, folder, tags, `DocumentAddedEvent`)
- [x] 2.2 Implement `buildEmlDocumentName` applying the document name template and placeholders
- [x] 2.3 Add the helpers `resolveUserQuietly`, `limitDocumentNameLength` and `appendNameIndex`

## 3. Assistant chat context
- [x] 3.1 Prefix the chat context in `MailContentUI.getInputs` with a header block (`buildMailChatContext`), including `mailboxId` and `messageRef` for server-based mailboxes
- [x] 3.2 Restrict the header block to the chat request type so the other AI actions keep the plain body
- [x] 3.3 Mention the chat context as a source for `mailboxId`/`messageRef` in the tool and parameter descriptions

## 4. Verification
- [x] 4.1 Build `j-lawyer-client`
- [x] 4.2 File a mail into a case from the assistant chat; check name, approval dialog, "open document" button and automatic case refresh
- [x] 4.3 Verify the optional parameters `fileName`, `folderId` and `tags`
- [x] 4.4 Verify edge cases: duplicate filing yields ` (2)`, name only in the recycle bin gives a readable error, mail without subject, very long subject, archived case, inaccessible mailbox, unknown tag
- [x] 4.5 Start a chat from an open mail and verify the context panel shows the header block; ask to file the mail without a prior search
