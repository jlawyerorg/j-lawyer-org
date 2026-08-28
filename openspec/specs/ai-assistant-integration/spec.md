# ai-assistant-integration Specification

## Purpose
TBD - created by archiving change update-ingo-api-v2. Update Purpose after archive.
## Requirements
### Requirement: v2 Type-Specific Request Submission
The system SHALL submit AI requests to type-specific v2 endpoints (`/v2/{requestType}` for synchronous, `/v2/{requestType}-async` for asynchronous) instead of the single v1 endpoint (`/v1/request-submit`).

The v2 request body SHALL contain `actionId` (referencing a preconfigured server-side action) and optionally `model` (to override the action's default model), along with `prompt`, `inputData`, `parameterData`, `configurationData`, and `messages`.

The request body SHALL be constructed using `JsonObject`/`JsonArray` from json-simple (already on classpath) instead of manual `StringBuilder` string concatenation, to ensure correct escaping and maintainability.

#### Scenario: Synchronous summarize request
- **WHEN** the user triggers a summarize action
- **THEN** the client SHALL POST to `/v2/summarize` with `actionId` and input data
- **AND** receive an `AiRequestStatus` response with status `FINISHED` and nested `AiResponse`

#### Scenario: Asynchronous chat request
- **WHEN** the user triggers a chat action that is marked as `asyncRecommended`
- **THEN** the client SHALL POST to `/v2/chat-async` with `actionId`, messages, and optional model override
- **AND** receive an `AiRequestStatus` response with status `EXECUTING` and a `requestId` for polling

### Requirement: v2 Action Discovery
The system SHALL discover available AI actions by calling `GET /v2/actions` instead of `GET /v1/capabilities`. Each action SHALL be mapped to an `AiCapability` object, populating the new `actionId`, `asyncRecommended`, and `modelRef` fields.

#### Scenario: Populate assistant menu from v2 actions
- **WHEN** the client initializes the assistant menu
- **THEN** it SHALL call `GET /v2/actions` and parse the response into `AiCapability` objects
- **AND** populate the UI menu with available actions grouped by request type

### Requirement: v2 Model Discovery
The system SHALL provide a method to discover available AI models by calling `GET /v2/models`. The response SHALL be parsed into `AiModel` objects containing provider information, supported request types, and configuration metadata.

#### Scenario: Retrieve available models
- **WHEN** the client queries for available models
- **THEN** it SHALL call `GET /v2/models`
- **AND** return a list of `AiModel` objects with name, description, provider, supportedRequestTypes, and local flag

### Requirement: v2 Request Status Polling
The system SHALL poll for async request status using `GET /v2/request-status/{requestId}` instead of `GET /v1/request-status/{requestId}`.

#### Scenario: Poll for completed async request
- **WHEN** the client polls for a previously submitted async request
- **THEN** it SHALL call `GET /v2/request-status/{requestId}`
- **AND** parse the `AiResponse` with status, outputData, and executionMillis

### Requirement: AI Agent Role Authorization
The system SHALL enforce a dedicated `aiAgentRole` permission for all AI tool access. Only users who have been granted this role in `security_roles` SHALL be able to discover or execute AI tools via ToolRegistry.

#### Scenario: User with aiAgentRole accesses tools
- **WHEN** a user with the `aiAgentRole` role requests tool definitions or executes a tool
- **THEN** the system SHALL return the full tool list and execute tools as before

#### Scenario: User without aiAgentRole is denied tool access
- **WHEN** a user without the `aiAgentRole` role requests tool definitions
- **THEN** the system SHALL return an empty tool list
- **AND** any attempt to execute a tool SHALL return an error response

#### Scenario: Assistant panels show permission denied for unauthorized user
- **WHEN** a user without the `aiAgentRole` role opens the `AssistantChatPanel` or `AssistantGenericPanel`
- **THEN** the system SHALL display a red-colored text indicating that permissions for executing AI agents are missing
- **AND** the default "Ingo erledigt Routinearbeiten" placeholder SHALL NOT be shown

#### Scenario: Assistant panels show normal placeholder for authorized user
- **WHEN** a user with the `aiAgentRole` role opens the `AssistantChatPanel` or `AssistantGenericPanel`
- **THEN** the system SHALL display the standard "Ingo erledigt Routinearbeiten" placeholder text

### Requirement: AI Agent Role Default Assignment
The system SHALL grant the `aiAgentRole` to all existing users via a Flyway database migration, so that the role is enabled by default and must be explicitly revoked by an administrator.

#### Scenario: Migration grants role to existing users
- **WHEN** the Flyway migration V3_5_0_15 is applied
- **THEN** every user present in `security_roles` SHALL receive an `aiAgentRole` entry with `roleGroup` = `Roles`

### Requirement: AI Agent Role Administration
The system SHALL allow administrators to manage the `aiAgentRole` via the existing `chkIntegrationsIngo` checkbox in the User Administration Dialog. The role SHALL be persisted to and loaded from the `security_roles` table following the same pattern as all other roles.

#### Scenario: Administrator grants AI agent role
- **WHEN** an administrator selects the `chkIntegrationsIngo` checkbox and saves the user
- **THEN** the system SHALL persist an `aiAgentRole` entry in `security_roles` for that user

#### Scenario: Administrator revokes AI agent role
- **WHEN** an administrator deselects the `chkIntegrationsIngo` checkbox and saves the user
- **THEN** the system SHALL remove the `aiAgentRole` entry from `security_roles` for that user

#### Scenario: Loading user roles reflects AI agent role state
- **WHEN** a user's roles are loaded into the User Administration Dialog
- **THEN** the `chkIntegrationsIngo` checkbox SHALL be selected if the user has `aiAgentRole`, and deselected otherwise

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

### Requirement: File E-Mail Into Case Tool
The client-side `ToolRegistry` SHALL provide a writing tool that stores a mailbox message as an `.eml` document in a case. It SHALL be registered with risk level `RISK_MEDIUM` and therefore SHALL require user approval. The mailbox SHALL be resolved through the mailbox list of the logged-in user, so that the per-user mailbox ACL applies as it does for the read-only e-mail tools.

#### Scenario: File a message into a case
- **WHEN** the LLM calls `save_email_to_case` with a mailbox id, a message reference and a case id
- **THEN** the client SHALL retrieve the message as `.eml` and store it as a document in that case
- **AND** publish a document-added event so the open case refreshes without user interaction
- **AND** return the new document id together with its name, so that the chat panel offers an "open document" button

#### Scenario: Document name follows the configured template
- **WHEN** a message is filed and a default document name template is configured
- **THEN** the name SHALL be built from the subject with an `.eml` extension and then run through that template including placeholder replacement, matching the manual filing dialog
- **AND** when no template is configured, the sanitized subject SHALL be used
- **AND** when the message has no subject, `E-Mail ohne Betreff` SHALL be used
- **AND** the resulting name SHALL be capped at 250 characters while keeping the `.eml` extension

#### Scenario: A document of that name already exists
- **WHEN** the target case already contains a document with the computed name
- **THEN** the client SHALL append ` (2)` through ` (5)` until the name is free
- **AND** report an error naming the conflict if all attempts are taken
- **AND** report a readable error instead of a raw exception when the name is taken by a document in the recycle bin, which the existence check does not cover

#### Scenario: Optional folder and tags
- **WHEN** the call carries a folder id
- **THEN** the stored document SHALL be moved into that folder, otherwise it stays in the case root
- **AND** when the call carries a comma-separated tag list, every tag SHALL be validated against the configured document tags before the document is written, and an invalid tag SHALL abort the call without storing anything

#### Scenario: Case or mailbox not usable
- **WHEN** the case does not exist, is archived, or the mailbox is not accessible to the logged-in user
- **THEN** the client SHALL return an error explaining which precondition failed
- **AND** SHALL NOT store a document

### Requirement: Mail Context For The Assistant Chat
When an assistant chat is started from an opened e-mail, the client SHALL pass the message header data as part of the chat context, and for server-based mailboxes SHALL include the mailbox id and the message reference. Without those identifiers the assistant cannot address the opened message with its e-mail tools. AI actions other than chat SHALL keep receiving the plain message text.

#### Scenario: Chat started from an opened message
- **WHEN** the user starts a chat action from an opened e-mail
- **THEN** the chat context SHALL contain sender, recipients, copy recipients, date, subject and attachment names
- **AND** for a server-based mailbox it SHALL also contain the mailbox id and the message reference, labelled so that they map onto the `mailboxId` and `messageRef` tool parameters
- **AND** the assistant SHALL be able to file that message into a case without searching for it again

#### Scenario: Non-chat AI action from an opened message
- **WHEN** the user starts a translate, summarize, explain or generate action from an opened e-mail
- **THEN** the input SHALL remain the selected text or message body without the header block

