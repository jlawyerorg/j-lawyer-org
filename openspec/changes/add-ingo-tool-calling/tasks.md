## 1. j-lawyer-ai: Data Model Updates

- [x] 1.1 Create `ToolDefinition` model class (id, description, list of parameters) for receiving tool definitions from the client
- [x] 1.2 Create `ToolParameter` model class (name, type, description, required)
- [x] 1.3 Create `ToolCall` model class (id, toolName, arguments as JSON string)
- [x] 1.4 Extend `AiRequestV2` with `List<ToolDefinition> tools` field
- [x] 1.5 Extend internal `AiRequest` with `List<ToolDefinition> tools` field and update `AiRestEndpointV2.toAiRequest()` to copy tools from `AiRequestV2` to `AiRequest`
- [x] 1.6 Extend `AiResponse` with `List<ToolCall> toolCalls` field and `List<Message> messages` field (for returning conversation state)
- [x] 1.7 Add `STATUS_TOOL_CALL_PENDING = "TOOL_CALL_PENDING"` constant to `AiResponse`
- [x] 1.8 Extend `Message` with `toolCallId` and `toolName` fields for tool-role messages
- [x] 1.9 Add `supports-tools` boolean attribute to `<model>` element in XML config and expose via `GET /v2/models`
- [x] 1.10 Update `j-lawyer-ai.xml` to set `supports-tools="true"` on models that support tool calling (e.g. Claude, GPT-4) and `supports-tools="false"` on others

## 2. j-lawyer-ai: LLM Processor Tool Support

- [x] 2.1 Modify `AnthropicProcessor` to include `tools` in API request body (from `AiRequest.tools`) and detect `tool_use` content blocks in responses -- return `TOOL_CALL_PENDING` with toolCalls and conversation messages
- [x] 2.2 Modify `OpenAiProcessor` to include `tools` in API request body and detect `tool_calls` in responses -- same pattern
- [x] 2.3 Modify `MistralProcessor` to include `tools` in API request body and detect tool call responses
- [x] 2.4 Modify `GeminiProcessor` to include `function_declarations` in API request body and detect `functionCall` parts
- [x] 2.5 Modify `OllamaProcessor` to include `tools` in API request body (OpenAI-compatible format) and detect `tool_calls` in responses
- [x] 2.5 When a follow-up request arrives with tool-role messages in the message history, reconstruct the provider-specific format from the normalized JSON in `Message.content` and pass the full conversation to the LLM
- [x] 2.6 Normalize provider-specific tool_use responses into a provider-agnostic JSON structure in `Message.content` (with `type: "tool_use_request"`, optional `text`, and `toolCalls` array) before including in `AiResponse.messages`
- [x] 2.7 Handle parallel tool calls: when the LLM returns multiple tool calls in one response, include all of them in `AiResponse.toolCalls`
- [x] 2.8 Update `V2PipelineRunner` to pass tool definitions from the request to the processors
- [x] 2.9 When the response status is `TOOL_CALL_PENDING`, skip post-processors (e.g. token deduction) since the request is not yet complete
- [x] 2.10 Force synchronous execution when the request contains tools (ignore `async-recommended` on the action)
- [x] 2.11 Update `AiRestEndpointV2.handleSync()` to propagate `AiResponse.status` to `AiRequestStatus.status` instead of hardcoding `"FINISHED"`

## 3. j-lawyer-org: Shared Model Updates (j-lawyer-fax)

- [x] 3.1 Create `ToolDefinition` DTO class in `com.jdimension.jlawyer.ai` (id, description, list of parameters)
- [x] 3.2 Create `ToolParameter` DTO class in `com.jdimension.jlawyer.ai` (name, type, description, required)
- [x] 3.3 Create `ToolCall` DTO class in `com.jdimension.jlawyer.ai` (id, toolName, arguments)
- [x] 3.4 Extend `AiResponse` with `List<ToolCall> toolCalls` field and `List<Message> messages` field
- [x] 3.5 Add `STATUS_TOOL_CALL_PENDING = "TOOL_CALL_PENDING"` constant to `AiResponse`
- [x] 3.6 Extend `AiModel` with `supportsTools` boolean field
- [x] 3.7 Extend `Message` with `toolCallId` and `toolName` fields
- [x] 3.8 Update `AssistantAPI.submitRequest()` to serialize `tools` (List<ToolDefinition>) in the request JSON and deserialize `toolCalls` and `messages` from response JSON
- [x] 3.9 Update `AssistantAPI.getModels()` to deserialize `supportsTools` from models response
- [x] 3.10 Update `AssistantAPI.getRequestStatus()` to deserialize `toolCalls` from status response JSON

## 4. j-lawyer-org: Server-Side Updates (j-lawyer-server)

- [x] 4.1 Update `IntegrationServiceRemote.submitAssistantRequest()` signature to accept `List<ToolDefinition> tools` parameter
- [x] 4.2 Update `IntegrationService.submitAssistantRequest()` to pass tool definitions to `AssistantAPI` and forward `toolCalls`/`messages` from `AiResponse`
- [x] 4.3 Update `IntegrationServiceRemote` JavaDoc for changed methods

## 5. j-lawyer-org: Client ToolRegistry (j-lawyer-client)

- [x] 5.1 Create `ToolRegistry` class in `com.jdimension.jlawyer.client.assistant` that holds all tool definitions and execution logic
- [x] 5.2 Implement `getToolDefinitions()` returning `List<ToolDefinition>` for inclusion in requests
- [x] 5.3 Implement `execute(String toolId, String argumentsJson)` that dispatches to the appropriate EJB calls and returns a JSON result string
- [x] 5.4 Implement `search_cases` tool (calls `ArchiveFileServiceRemote` search methods, may combine multiple calls)
- [x] 5.5 Implement `get_case` tool (calls `ArchiveFileServiceRemote` to get case details)
- [x] 5.6 Implement `search_contacts` tool (calls `AddressServiceRemote` search methods)
- [x] 5.7 Implement `list_case_documents` tool (calls `ArchiveFileServiceRemote.getDocuments(...)`)
- [x] 5.8 Implement `get_document_text` tool (extracts text content from a document)
- [x] 5.9 Add session-level approval tracking: `Set<String> sessionApprovedTools` cleared on client exit
- [x] 5.10 Add permanent approval check: read `UserSettings` key `assistant.tool.alwaysAllow.<toolId>`

## 6. j-lawyer-org: Client UI Updates (j-lawyer-client)

- [x] 6.1 Update `AssistantChatPanel` to detect `TOOL_CALL_PENDING` status in responses
- [x] 6.2 Display requested tool calls with four approval options: Allow, Deny, Always Allow, Allow for Session -- process each tool call in the `toolCalls` list one by one
- [x] 6.3 On allow/always-allow/session-allow: execute the tool via `ToolRegistry.execute()`, catch exceptions and use error message as result, construct a tool-role `Message` with the result (set `toolCallId`, `toolName`, `role=tool`)
- [x] 6.4 On deny: construct a tool-role `Message` indicating denial
- [x] 6.5 After processing all tool calls in the batch: send a single follow-up request with all tool result messages plus the full conversation history from `AiResponse.messages`
- [x] 6.6 On "always allow": persist approval via `UserSettings` (`assistant.tool.alwaysAllow.<toolId>`)
- [x] 6.7 On "allow for session": add toolId to `ToolRegistry.sessionApprovedTools`
- [x] 6.8 Before showing approval dialog, check if tool is already approved (session set or UserSettings) and auto-execute if so
- [x] 6.9 Display tool call result in chat history after execution: success with summary, failure with error, or denial
- [x] 6.10 Repeat the loop until a `FINISHED` or `FAILED` response is received
- [x] 6.11 When a `TOOL_CALL_PENDING` response contains text alongside tool calls, display the text as a regular assistant message before showing the tool approval dialog
- [x] 6.12 Cache `AiModel` objects in `AssistantAccess` (alongside capabilities) so `AssistantChatPanel` can look up `supportsTools` by model name from `AiCapability.modelRef`
- [x] 6.13 Only send tool definitions in request when selected model has `supportsTools == true` and request type is `chat`
- [x] 6.14 Display `supportsTools` in `AssistantPromptV2SetupDialog` alongside `deductTokens` (e.g. "unterstützt Werkzeuge" / "keine Werkzeugunterstützung")

## 7. Testing

- [ ] 7.1 Test tool calling round trip with Anthropic API (Claude)
- [ ] 7.2 Test tool calling round trip with OpenAI-compatible API
- [ ] 7.3 Test that actions without tools continue to work unchanged (backward compatibility)
- [ ] 7.4 Test user denial of a tool call (LLM receives denial, produces answer without tool)
- [ ] 7.5 Test multiple sequential tool calls in one conversation
- [ ] 7.6 Test parallel tool calls (multiple tools in one LLM response)
- [ ] 7.7 Test tool execution error handling (EJB exception sent as tool result)
- [ ] 7.8 Test "always allow" persistence across client restarts
- [ ] 7.9 Test "allow for session" cleared after client restart
- [ ] 7.10 Test tool calling round trip with Ollama (tool-capable model)
- [ ] 7.11 Test that tools are not sent when model has `supportsTools == false`
- [ ] 7.12 Test that tools are not sent for non-chat request types
