## ADDED Requirements

### Requirement: E-Mail Mailbox Tools
The client-side `ToolRegistry` SHALL provide read-only tools that let the AI assistant list the user's e-mail mailboxes, search them, and read a single message. The tools SHALL resolve mailboxes exclusively through the mailbox list of the logged-in user, so that the per-user mailbox ACL is enforced without a separate permission check. They SHALL be registered with risk level `RISK_LOW` and therefore SHALL NOT require an approval dialog, consistent with all other read-only tools.

#### Scenario: List accessible mailboxes
- **WHEN** the LLM calls `list_mailboxes`
- **THEN** the client SHALL return the id, display name and e-mail address of every mailbox the logged-in user has access to
- **AND** an empty list SHALL be returned as an empty result, not as an error

#### Scenario: Search the inbox of all accessible mailboxes
- **WHEN** the LLM calls `search_emails` with a search term and no `mailboxId`, `folder` or `scope`
- **THEN** the client SHALL search the inbox of every accessible mailbox for the term in subject, sender, recipients and body
- **AND** return hit metadata (mailbox, folder, message reference, subject, sender, recipients, date, read flag, attachment flag) without message bodies
- **AND** sort the hits by date descending and cap them at 50, setting `truncated` when more hits exist

#### Scenario: Search terms are matched literally
- **WHEN** the LLM calls `search_emails`
- **THEN** the search term SHALL be matched as a literal, case-insensitive substring, because the IMAP backend passes it unchanged into `SubjectTerm`, `FromStringTerm`, `RecipientStringTerm` and `BodyTerm`
- **AND** the tool description SHALL state that boolean operators, quotes and wildcards are not supported and would be searched for literally
- **AND** the tool description SHALL instruct the model to issue one call per synonym or word variant instead of combining them into one term

#### Scenario: Search a named folder or all folders
- **WHEN** the LLM calls `search_emails` with `folder` set to a folder display name
- **THEN** only folders with that display name SHALL be searched
- **AND** when `scope` is set to `all` instead, all folders except trash SHALL be searched, capped at 15 folders per mailbox
- **AND** the inbox and the sent folder SHALL be searched first, so that the folder cap never drops them regardless of the order the mail server lists folders in
- **AND** `foldersTruncated` SHALL be set when the cap was applied

#### Scenario: A mailbox or folder cannot be reached
- **WHEN** listing folders or searching a folder fails for one mailbox
- **THEN** the search SHALL continue with the remaining mailboxes and folders
- **AND** the failure SHALL be reported in a `warnings` array in the result

#### Scenario: Read a single message
- **WHEN** the LLM calls `get_email` with a mailbox id and a message reference obtained from `search_emails`
- **THEN** the client SHALL return the message including its body and attachment metadata
- **AND** an HTML body SHALL be converted to plain text
- **AND** the body SHALL be capped at 30000 characters, setting `truncated` when it was shortened
- **AND** attachment contents SHALL NOT be loaded

#### Scenario: Mailbox is not accessible to the user
- **WHEN** the LLM calls `search_emails` or `get_email` with a `mailboxId` the logged-in user has no access to
- **THEN** the client SHALL return an error stating that the mailbox was not found or is not accessible
- **AND** SHALL NOT contact the mailbox
