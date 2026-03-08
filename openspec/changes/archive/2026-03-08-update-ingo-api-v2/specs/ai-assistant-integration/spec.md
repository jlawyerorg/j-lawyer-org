## ADDED Requirements

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

## ADDED Requirements (continued)

### Requirement: AI Capability Data Model
The `AiCapability` class SHALL include additional fields to support the v2 action concept:
- `actionId` (String): Unique identifier for the action on the server
- `asyncRecommended` (boolean): Whether async execution is recommended (replaces the v1 `async` flag semantically)
- `modelRef` (String): Reference to the default model name for this action

The existing `async` field SHALL be populated from the v2 `asyncRecommended` field to maintain backward compatibility with existing UI code.

#### Scenario: AiCapability populated from v2 action
- **WHEN** the client parses a v2 action response
- **THEN** it SHALL set `actionId` to the action's unique identifier
- **AND** set `async` to the value of `asyncRecommended`
- **AND** set `modelRef` to the action's model reference
- **AND** preserve all other fields (name, description, requestType, input, output, parameters, configurations, usageTypes)
