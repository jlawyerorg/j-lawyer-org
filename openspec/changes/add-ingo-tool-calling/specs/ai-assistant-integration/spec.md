## ADDED Requirements

### Requirement: Tool Calling Support
The system SHALL support LLM tool calling within AI assistant conversations. Tool definitions are owned by the j-lawyer client and sent to j-lawyer-ai as part of the request. j-lawyer-ai SHALL forward tool definitions to the LLM provider in the provider-specific format. When the LLM responds with a tool call, j-lawyer-ai SHALL NOT execute it but instead return the tool call request to the client with status `TOOL_CALL_PENDING`.

The client SHALL display the requested tool call to the user for approval. If approved, the client SHALL execute the tool via existing EJB services and send the result back to j-lawyer-ai as a follow-up request with a tool-role message. The loop SHALL continue until the LLM produces a final text response.

#### Scenario: LLM calls a single tool during chat
- **WHEN** a user sends a chat message and the model supports tools
- **THEN** the client SHALL include tool definitions from the `ToolRegistry` in the request
- **AND** j-lawyer-ai SHALL forward them to the LLM
- **AND** if the LLM responds with a tool call, j-lawyer-ai SHALL return status `TOOL_CALL_PENDING` with the requested `toolCalls` and the current conversation `messages`
- **AND** the client SHALL display the tool call to the user for approval
- **AND** if approved, the client SHALL execute the tool via EJB and send a follow-up request with the tool result
- **AND** j-lawyer-ai SHALL feed the tool result to the LLM and return the final text response

#### Scenario: User denies a tool call
- **WHEN** the client displays a tool call and the user denies it
- **THEN** the client SHALL send a follow-up request with a tool-role message indicating denial
- **AND** the LLM SHALL produce an answer without the tool result

#### Scenario: LLM calls multiple tools in sequence
- **WHEN** the LLM responds with a tool call and, after receiving the result, responds with another tool call
- **THEN** the client SHALL repeat the approval/execution loop for each tool call
- **AND** continue until the LLM produces a final text response

#### Scenario: LLM requests multiple tool calls in a single response
- **WHEN** the LLM responds with multiple tool calls in a single response (parallel tool calls)
- **THEN** j-lawyer-ai SHALL return all tool calls in the `toolCalls` list
- **AND** the client SHALL present each tool call for approval one by one
- **AND** send all tool results (approved or denied) back in a single follow-up request

#### Scenario: Model does not support tools
- **WHEN** the selected model has `supportsTools == false`
- **THEN** the client SHALL NOT include tool definitions in the request
- **AND** the request SHALL be processed as a standard chat without tool calling

### Requirement: Client-Side Tool Registry
The client SHALL maintain a central `ToolRegistry` that defines all available tools with their metadata (id, description, parameters) and execution logic. Each tool implementation SHALL dispatch to one or more EJB service methods via `JLawyerServiceLocator` and return the result as a JSON string.

#### Scenario: ToolRegistry provides definitions for request
- **WHEN** the client prepares a chat request for a model that supports tools
- **THEN** it SHALL call `ToolRegistry.getToolDefinitions()` to obtain the list of `ToolDefinition` DTOs
- **AND** include them in the request sent to j-lawyer-ai

#### Scenario: ToolRegistry executes a tool via EJB
- **WHEN** the user approves a tool call
- **THEN** the client SHALL call `ToolRegistry.execute(toolId, argumentsJson)`
- **AND** the registry SHALL dispatch to the appropriate EJB service methods
- **AND** a single tool execution MAY call multiple EJB methods across different services
- **AND** the combined result SHALL be serialized to JSON

#### Scenario: Tool execution fails with an exception
- **WHEN** an EJB call throws an exception during tool execution
- **THEN** the client SHALL catch the error
- **AND** send the error message as the tool result (role `tool`, content describing the error)
- **AND** the LLM SHALL receive the error and produce a response that accounts for the failure

### Requirement: Tool Definitions Sent by Client
j-lawyer-ai SHALL accept an optional `tools` array in the v2 request body containing generic tool definitions (id, description, parameters). j-lawyer-ai SHALL NOT define or hardcode any tools. The tool definitions SHALL be mapped to the provider-specific format by each LLM processor.

#### Scenario: j-lawyer-ai receives and forwards tools
- **WHEN** j-lawyer-ai receives a v2 request with a `tools` array
- **THEN** it SHALL map the tool definitions to the LLM provider's format (Anthropic `input_schema`, OpenAI `function.parameters`, Gemini `function_declarations`)
- **AND** include them in the API call to the LLM

#### Scenario: Request without tools
- **WHEN** j-lawyer-ai receives a v2 request without a `tools` array
- **THEN** it SHALL process the request exactly as before, with no tool calling behavior

### Requirement: Forced Synchronous Execution for Tool Requests
When a request contains tools, j-lawyer-ai SHALL process it synchronously regardless of the action's `async-recommended` setting. The client drives the tool calling loop with multiple synchronous round trips.

#### Scenario: Async override for tool requests
- **WHEN** a request contains a non-empty `tools` array and the action has `async-recommended="true"`
- **THEN** j-lawyer-ai SHALL ignore the async recommendation and process the request synchronously

### Requirement: Tool Call User Approval
The client SHALL display each tool call to the user before execution, showing the tool name and its arguments. The user SHALL have four approval options: Allow, Deny, Always Allow, and Allow for Session.

#### Scenario: User selects Allow
- **WHEN** the user selects "Allow" for a tool call
- **THEN** the client SHALL execute the tool once
- **AND** no approval preference SHALL be persisted

#### Scenario: User selects Deny
- **WHEN** the user selects "Deny" for a tool call
- **THEN** the client SHALL send a denial message to the LLM
- **AND** no approval preference SHALL be persisted

#### Scenario: User selects Always Allow
- **WHEN** the user selects "Always Allow" for a tool call
- **THEN** the client SHALL execute the tool
- **AND** persist the tool ID as permanently approved via `UserSettings` (key: `assistant.tool.alwaysAllow.<toolId>`)
- **AND** future calls to this tool SHALL be auto-executed without prompting

#### Scenario: User selects Allow for Session
- **WHEN** the user selects "Allow for Session" for a tool call
- **THEN** the client SHALL execute the tool
- **AND** remember the tool ID in memory for the current session
- **AND** future calls to this tool SHALL be auto-executed without prompting until j-lawyer-client terminates

#### Scenario: Previously approved tool is auto-executed
- **WHEN** a tool call arrives for a tool that was previously approved (via "Always Allow" or "Allow for Session")
- **THEN** the client SHALL auto-execute the tool without showing the approval dialog

### Requirement: Tool Call Result Display in Chat
After a tool call is processed (executed, failed, or denied), the client SHALL display a concise status message in the chat conversation history so the user understands what data the LLM based its answer on.

#### Scenario: Successful tool execution displayed in chat
- **WHEN** a tool call is executed successfully
- **THEN** the chat SHALL display a status message showing the tool name, arguments, and a brief summary of the result (e.g. "Aktensuche: 'Müller' — 3 Treffer")

#### Scenario: Failed tool execution displayed in chat
- **WHEN** a tool call execution fails with an error
- **THEN** the chat SHALL display a status message showing the tool name and the error (e.g. "Aktensuche: 'Müller' — Fehler: Verbindung fehlgeschlagen")

#### Scenario: LLM returns text alongside a tool call
- **WHEN** the LLM response contains both text content and a tool call
- **THEN** the client SHALL display the text as a regular assistant message in the chat
- **AND** then show the tool call approval dialog

#### Scenario: Denied tool call displayed in chat
- **WHEN** the user denies a tool call
- **THEN** the chat SHALL display a status message indicating the denial (e.g. "Aktensuche: 'Müller' — abgelehnt")

### Requirement: TOOL_CALL_PENDING Status Propagation
When a tool call is pending, both `AiResponse.status` and `AiRequestStatus.status` SHALL be set to `TOOL_CALL_PENDING`. The `handleSync()` method in `AiRestEndpointV2` SHALL propagate the response status to `AiRequestStatus` instead of hardcoding `"FINISHED"`.

#### Scenario: Status propagation through response chain
- **WHEN** a LLM processor returns an `AiResponse` with status `TOOL_CALL_PENDING`
- **THEN** `AiRestEndpointV2.handleSync()` SHALL set `AiRequestStatus.status` to the value of `AiResponse.status`
- **AND** the client SHALL receive `TOOL_CALL_PENDING` in `AiRequestStatus.status`

### Requirement: Structured Message Content for Tool Calling
The `Message.content` field SHALL remain a `String` but MAY contain a JSON string for structured assistant messages that include tool_use requests. j-lawyer-ai SHALL normalize provider-specific tool_use formats into a provider-agnostic JSON structure stored in `content`. When processing a follow-up request, j-lawyer-ai SHALL reconstruct the provider-specific format from the normalized JSON. The client SHALL pass these messages back unchanged without needing to understand their internal structure.

#### Scenario: Assistant tool_use message round trip
- **WHEN** j-lawyer-ai receives a tool_use response from the LLM
- **THEN** it SHALL normalize it into a JSON structure in `Message.content` (with `type`, optional `text`, and `toolCalls`)
- **AND** include it in `AiResponse.messages`
- **AND** when the client sends the follow-up request with these messages, j-lawyer-ai SHALL reconstruct the provider-specific format before calling the LLM

### Requirement: Tool Calling Restricted to Chat
Tool calling SHALL only be supported for the `chat` request type, which is processed by `AssistantChatPanel`. Other request types (summarize, explain, generate, extract, vision, transcribe, translate) SHALL NOT include tool definitions.

#### Scenario: Non-chat request type ignores tools
- **WHEN** a request uses a non-chat request type
- **THEN** the client SHALL NOT include tool definitions in the request regardless of model capability

### Requirement: Conversation State in Response
When j-lawyer-ai returns a `TOOL_CALL_PENDING` response, it SHALL include the full conversation message history (including the assistant's tool_use message) in `AiResponse.messages`. The client SHALL send this message history back in the follow-up request along with the new tool-role message, since j-lawyer-ai is stateless.

#### Scenario: Stateless round trip
- **WHEN** j-lawyer-ai returns a `TOOL_CALL_PENDING` response
- **THEN** the response SHALL contain a `messages` list with the full conversation so far
- **AND** the client SHALL include these messages plus the tool result message in the follow-up request

### Requirement: Model-Level Tool Support Flag
The `supportsTools` boolean SHALL be a property of the model configuration in `j-lawyer-ai.xml`, exposed via `GET /v2/models` and mapped to `AiModel.supportsTools`. The client SHALL use this flag to decide whether to include tool definitions in requests. The flag SHALL be displayed in `AssistantPromptV2SetupDialog` alongside `deductTokens`.

#### Scenario: Display supportsTools in setup dialog
- **WHEN** the user selects a model in `AssistantPromptV2SetupDialog`
- **THEN** the dialog SHALL display whether the model supports tools (e.g. "unterstützt Werkzeuge" / "keine Werkzeugunterstützung") in the same manner as the `deductTokens` indicator
