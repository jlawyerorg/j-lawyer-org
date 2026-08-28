# Change: Add an AI tool that files an e-mail into a case as .eml

## Why
Since `add-email-search-tools` the assistant can search and read mailboxes, but it cannot act on what it finds. Filing a message into a case is the obvious next step in daily practice ("save this mail into case 25/0042"). Today that is only possible manually through `EmailInboxPanel` → `BulkSaveDialog`, or automatically through the server-side `MailboxScannerTask`.

## What Changes
- Add one writing tool `save_email_to_case` (`RISK_MEDIUM`, so the approval dialog applies) to the client-side `ToolRegistry`.
- Required parameters `mailboxId`, `messageRef` (both from `search_emails`) and `caseId` (from `search_cases`); optional `fileName`, `folderId` and a comma-separated `tags` list.
- The document name is derived from the subject and then run through the configured document name template, reusing `ArchiveFileServiceRemote.getNewDocumentName` plus `TemplatesUtil.getPlaceHolderValues`/`replacePlaceHolders` exactly as `BulkSaveEntry.setNameTemplate` does, so an AI-filed mail is named like a manually filed one.
- Name clashes are resolved with the suffix ` (2)` … ` (5)` like `MailboxScannerTask`, then reported as an error.
- The name is capped at 250 characters (the width of `documents.name`). Neither the manual nor the scanner path does this today, so a very long subject makes their insert fail.
- Tags are validated against the configured option groups *before* the document is written, reusing the existing `validateTag` helper.
- The result JSON carries `name` next to `documentId`, which is what makes `AssistantChatPanel.extractDocumentReferences` render the "open document" button.
- Extend the context that `MailContentUI` hands to the assistant chat. It passed the body text only, so a chat started from an open message knew neither the header data nor `mailboxId`/`messageRef` -- the model could not address that very message and had to guess it back via `search_emails`. The chat context now carries a header block including both identifiers. Only the chat flow gets it; translate/summarize/generate keep the plain body, otherwise the headers would be translated and prepended to the reply mail.

## Impact
- Affected specs: `ai-assistant-integration`
- Affected code: `j-lawyer-client/src/main/java/com/jdimension/jlawyer/client/assistant/ToolRegistry.java`, `j-lawyer-client/src/main/java/com/jdimension/jlawyer/client/mail/MailContentUI.java`
- Reuses `EmailServiceRemote.getMessage`/`getMessageAsEml` and `ArchiveFileServiceRemote.addDocument`/`doesDocumentExist`/`moveDocumentsToFolder`/`setDocumentTag` -- no server change, no new remote interface method, no database migration
- No breaking changes; existing tools are untouched
