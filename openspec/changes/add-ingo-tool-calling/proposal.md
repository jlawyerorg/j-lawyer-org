# Change: Add tool calling support for Ingo AI assistant

## Why
The AI assistant Ingo currently operates without access to application data -- it can only process user-provided text and files. Adding tool calling enables Ingo to query j-lawyer data (cases, contacts, documents, calendar) during a conversation, producing context-aware answers and significantly increasing its usefulness for legal practitioners.

## What Changes

### j-lawyer-ai (Ingo Backend - /home/jens/dev/j-lawyer-ai/)
- Extend the v2 REST endpoints to accept an optional `tools` array in the request body (list of tool definitions with id, description, parameters)
- Forward tool definitions to the LLM providers (Anthropic, OpenAI, Mistral, Gemini, Ollama) in their provider-specific format
- When the LLM responds with a tool call, return status `TOOL_CALL_PENDING` with the requested `toolCalls` and the full conversation `messages` -- do NOT execute the tool
- When a follow-up request arrives with tool-role messages, pass the full conversation to the LLM and continue
- Extend `AiResponse` with `toolCalls` and `messages` fields, add `TOOL_CALL_PENDING` status
- Extend `Message` with `toolCallId` and `toolName` fields
- Normalize provider-specific tool_use messages into a provider-agnostic JSON format in `Message.content` for stateless round trips; reconstruct provider-specific format on follow-up requests
- Handle parallel tool calls (multiple tools in one LLM response) by returning all in `toolCalls`
- Add `supportsTools` boolean to the model configuration in `j-lawyer-ai.xml` and expose it via `GET /v2/models`
- Force synchronous execution when the request contains tools (ignore `async-recommended`)

### j-lawyer-org (j-lawyer Backend + Client - /home/jens/dev/j-lawyer-org/)
- Create a client-side `ToolRegistry` that defines all available tools (id, description, parameters, execution logic)
- Each tool implementation lives in the client and can call one or more EJB services via `JLawyerServiceLocator`
- The client sends tool definitions from the `ToolRegistry` to j-lawyer-ai through the existing integration chain (Client → IntegrationService → AssistantAPI → j-lawyer-ai)
- Extend shared DTOs (`AiResponse`, `Message`, `AiModel`) in j-lawyer-fax with new fields
- Update `AssistantAPI` to serialize tool definitions in requests and deserialize `toolCalls`/`messages` from responses
- Update `IntegrationServiceRemote`/`IntegrationService` to pass tool definitions and forward tool-related response data
- Update `AssistantChatPanel` to implement the client-driven tool calling loop with four user approval options:
  - **Allow** -- execute this tool call (no persistence)
  - **Deny** -- reject this tool call (no persistence)
  - **Always allow** -- execute and persist this tool as allowed via `UserSettings` (survives restarts)
  - **Allow for this session** -- execute and remember in memory until j-lawyer-client terminates
- Handle parallel tool calls (multiple tools in one LLM response) by processing each one by one and sending all results in a single follow-up
- If an EJB call fails during tool execution, send the error message as the tool result so the LLM can handle it gracefully
- Tool calling is restricted to the `chat` request type (processed by `AssistantChatPanel`)
- Display `supportsTools` in `AssistantPromptV2SetupDialog` alongside `deductTokens`

## Impact
- Affected specs: `ai-assistant-integration`
- Affected code (j-lawyer-ai): `AiRestEndpointV2`, `V2PipelineRunner`, `AnthropicProcessor`, `OpenAiProcessor`, `MistralProcessor`, `GeminiProcessor`, `OllamaProcessor`, `AiRequestV2`, `AiResponse`, `Message`, `Model` (XML config), `j-lawyer-ai.xml`
- Affected code (j-lawyer-org): `AssistantAPI`, `AiResponse`, `AiModel`, `Message`, `IntegrationService`, `IntegrationServiceRemote`, `AssistantChatPanel`, `AssistantPromptV2SetupDialog`, `UserSettings`/`UserSettingsKeys`
- No database schema changes required (UserSettings uses existing key-value storage)
- No breaking API changes -- all new fields are optional and backward compatible
