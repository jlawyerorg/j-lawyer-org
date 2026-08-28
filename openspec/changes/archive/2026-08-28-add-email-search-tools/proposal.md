# Change: Add e-mail mailbox search tools for the Ingo AI assistant

## Why
The client-side `ToolRegistry` exposes 62 tools covering cases, documents, contacts, calendar, invoices, timesheets and instant messages -- but none for e-mail. The assistant cannot answer questions such as "has the client already replied on case 25/0042?", although the server already offers a complete IMAP/Graph abstraction including full-text search through `EmailServiceRemote`.

## What Changes
- Add three read-only tools to the client-side `ToolRegistry`:
  - `list_mailboxes` -- lists the mailboxes the logged-in user has access to (id, display name, e-mail address)
  - `search_emails` -- searches subject, sender, recipients and body across the accessible mailboxes; returns hit metadata without message bodies
  - `get_email` -- loads a single message with its plain-text body and attachment metadata
- All three are `RISK_LOW` (no approval dialog), consistent with every other read-only tool. Access remains bounded by `aiAgentRole` and the per-user mailbox ACL (`security_mailbox_access`), which is enforced implicitly by resolving mailboxes through `UserSettings.getMailboxes(principalId)`.
- Default search scope is the inbox of every accessible mailbox; `folder` selects a named folder and `scope="all"` searches all folders except trash, capped at 15 folders per mailbox. IMAP can only search one folder per call, so broader scopes cost one server round trip per folder.
- A failing folder or mailbox is reported in a `warnings` array instead of aborting the whole search.
- The tool description states explicitly that the search term is matched literally: `EmailService.imapListMessages` passes it unchanged into JavaMail's `SubjectTerm`/`FromStringTerm`/`RecipientStringTerm`/`BodyTerm`, which do a case-insensitive substring comparison. Boolean operators such as `OR` would be searched for literally and silently return no hits, so the model is told to issue one call per synonym. (The MS Graph backend maps the term to `$search`, which does understand KQL operators; the description follows the more restrictive IMAP behaviour so it holds for both backends.)

## Impact
- Affected specs: `ai-assistant-integration`
- Affected code: `j-lawyer-client/src/main/java/com/jdimension/jlawyer/client/assistant/ToolRegistry.java` (only file)
- Reuses existing remote methods `EmailServiceRemote.listFolders`, `listMessages(..., searchTerm)` and `getMessage(..., includeAttachmentMetadata)` -- no server change, no new remote interface method, no database migration
- No breaking changes; existing tools are untouched
