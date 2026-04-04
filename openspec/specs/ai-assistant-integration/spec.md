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

