## Context

The AI assistant "Ingo" (j-lawyer-ai) currently processes requests in a single LLM call without access to application data. Modern LLM providers (Anthropic, OpenAI, Mistral, Gemini) support tool/function calling, where the model can request execution of predefined functions and receive their results within a conversation.

j-lawyer-ai knows nothing about the j-lawyer EJB layer. The client defines and executes tools, and sends the tool definitions to j-lawyer-ai as part of the request. j-lawyer-ai simply forwards them to the LLM provider.

Two projects are affected:
- `/home/jens/dev/j-lawyer-ai/` -- the Ingo AI backend (Spring Boot)
- `/home/jens/dev/j-lawyer-org/` -- j-lawyer backend (WildFly) and desktop client (Swing)

## Goals / Non-Goals

**Goals:**
- Enable LLMs to call tools that query j-lawyer data during chat conversations
- Client-driven tool execution: the user can approve or deny each tool call before execution
- Tool execution via existing EJB services in the client (not via REST API)
- Tools are defined and owned by the client; j-lawyer-ai is tool-agnostic
- `supportsTools` is a model-level property (not action-level)
- Four-level approval: allow, deny, always allow (persisted), allow for session (in-memory)

**Non-Goals:**
- Write/mutate tools (no creating cases, no modifying contacts) -- read-only tools only for safety
- Server-side tool execution in j-lawyer-ai
- Tool definitions in j-lawyer-ai.xml (j-lawyer-ai receives tools from the client)
- Custom user-defined tools (developer-defined in the client code only)

## Decisions

### Decision 1: Client-driven tool execution with user approval
**What:** When the LLM requests a tool call, j-lawyer-ai returns the tool call in the response (status `TOOL_CALL_PENDING`) without executing it. The desktop client displays the tool call to the user, who can approve or deny it. If approved, the client executes the tool via existing EJB services and sends the result back to j-lawyer-ai as a follow-up request.

**Why:**
- Users retain control over what data the AI accesses -- critical for legal confidentiality
- Tool execution uses the existing EJB layer with proper authentication, authorization, and business logic
- j-lawyer-ai remains tool-agnostic -- it just forwards definitions to the LLM and tool_use responses to the client

**Flow:**
```
1. Client: builds tool definitions from ToolRegistry
2. Client -> IntegrationService -> AssistantAPI -> j-lawyer-ai: chat request with tools[]
3. j-lawyer-ai -> LLM: request with tool definitions (provider-specific format)
4. LLM -> j-lawyer-ai: tool_use response (e.g. search_cases with query "Müller")
5. j-lawyer-ai -> Client: response with status TOOL_CALL_PENDING, toolCalls=[...], messages=[...]
6. Client: displays tool call, user chooses: allow / deny / always allow / allow for session
7a. On allow: Client executes tool via EJB, sends follow-up with tool result message
7b. On deny: Client sends follow-up with denial message
8. j-lawyer-ai -> LLM: conversation with tool result (or denial)
9. LLM -> j-lawyer-ai: final text response (or another tool call -> repeat from step 5)
10. j-lawyer-ai -> Client: final text response
```

### Decision 2: Tools defined in the client, sent to j-lawyer-ai per request
**What:** The client owns all tool definitions in a central `ToolRegistry`. Tool definitions (id, description, parameters) are serialized and sent to j-lawyer-ai as part of the v2 request body. j-lawyer-ai forwards them to the LLM in the provider-specific format.

**Why:**
- j-lawyer-ai knows nothing about the j-lawyer EJB layer and should not define domain-specific tools
- The client knows which EJB services are available and what data they return
- Adding new tools only requires changes in the client
- j-lawyer-ai just needs generic tool forwarding support, making it reusable for other tool sets

### Decision 3: Client-side ToolRegistry architecture
**What:** A central `ToolRegistry` class in the client that:
1. Defines all available tools with their metadata (id, description, parameters)
2. Provides a `getToolDefinitions()` method that returns serializable DTOs for the request
3. Contains a `execute(toolId, arguments)` method that dispatches to the appropriate EJB calls
4. Each tool implementation can call one or more EJB services and combine results

**Why:** Centralizes tool logic in one place. Each tool is self-contained and maps naturally to the EJB layer.

**Location:** `j-lawyer-client/src/com/jdimension/jlawyer/client/assistant/ToolRegistry.java`

**Example structure:**
```java
public class ToolRegistry {
    private static final List<ToolDefinition> TOOLS = List.of(
        new ToolDefinition("search_cases", "Sucht nach Akten anhand eines Suchbegriffs",
            List.of(new ToolParameter("query", "string", "Suchbegriff", true))),
        new ToolDefinition("get_case", "Ruft Details einer Akte ab",
            List.of(new ToolParameter("fileNumber", "string", "Aktenzeichen", true))),
        // ...
    );

    public List<ToolDefinition> getToolDefinitions() { return TOOLS; }

    public String execute(String toolId, String argumentsJson) {
        switch (toolId) {
            case "search_cases": return executeSearchCases(argumentsJson);
            case "get_case": return executeGetCase(argumentsJson);
            // ...
        }
    }

    private String executeSearchCases(String argumentsJson) {
        // Parse arguments, call ArchiveFileServiceRemote.searchEnhanced(...),
        // possibly call additional EJBs, serialize result to JSON
    }
}
```

### Decision 4: supportsTools as a model property
**What:** `supportsTools` is a boolean property on the model configuration in `j-lawyer-ai.xml` (alongside `local` and `deductTokens`). It is exposed via `GET /v2/models` and mapped to `AiModel.supportsTools` in the client.

**Why:** Tool calling is a model capability, not an action property. Claude and GPT-4 support tools; older Ollama models do not. The client needs this information to decide whether to include tool definitions in a request.

**Display:** Shown in `AssistantPromptV2SetupDialog` alongside `deductTokens` (e.g. "unterstützt Werkzeuge" / "keine Werkzeugunterstützung").

### Decision 5: Forced synchronous execution for tool requests
**What:** When a request contains tools, j-lawyer-ai SHALL process it synchronously regardless of the action's `async-recommended` setting.

**Why:** The client drives the tool calling loop with multiple synchronous round trips. Async processing with polling would add unnecessary complexity -- the client needs to receive the `TOOL_CALL_PENDING` response immediately to show the approval dialog.

### Decision 6: Four-level tool approval
**What:** When the client displays a tool call for approval, the user has four options:

| Option | Behavior | Persistence |
|--------|----------|-------------|
| **Allow** | Execute this tool call once | None |
| **Deny** | Reject this tool call | None |
| **Always allow** | Execute and remember permanently | Stored in `UserSettings` (server-side key-value via `SystemManagementRemote`) |
| **Allow for session** | Execute and remember until client exits | In-memory `Set<String>` in `ToolRegistry` or `AssistantChatPanel` |

**Why:** Balances security (explicit approval) with convenience (don't ask repeatedly for trusted tools).

**"Always allow" persistence:** Uses the existing `UserSettings` mechanism (`UserSettingsKeys` + `SystemManagementRemote.setUserSetting()`). Key pattern: `assistant.tool.alwaysAllow.<toolId>` with value `true`.

**"Allow for session":** A `Set<String>` in `ToolRegistry` that holds tool IDs approved for the current session. Cleared on client exit.

### Decision 7: Provider-specific tool format mapping in j-lawyer-ai
**What:** j-lawyer-ai receives generic tool definitions from the client and maps them to provider-specific formats in each LLM processor.

**Format mapping:**
- **Anthropic:** `tools` array with `name`, `description`, `input_schema` (JSON Schema). Response has `type: "tool_use"` content blocks with `id`, `name`, `input`.
- **OpenAI/Mistral:** `tools` array with `type: "function"`, `function.name`, `function.description`, `function.parameters`. Response has `tool_calls` in message with `id`, `function.name`, `function.arguments`.
- **Gemini:** `tools` array with `function_declarations`. Response has `functionCall` parts.

### Decision 8: Parallel tool calls
**What:** When the LLM requests multiple tool calls in a single response (supported by OpenAI, Mistral, Gemini), j-lawyer-ai returns all of them in the `toolCalls` list. The client processes them one by one -- showing each for approval/denial, executing approved ones, and sending all results back in a single follow-up request.

**Why:** Some providers batch multiple tool calls in one response (e.g. "search cases for Müller AND search contacts for Müller"). Each must be individually approved, but all results are sent back together in one round trip to avoid an invalid conversation state (the LLM expects all tool results before continuing).

### Decision 9: Error handling during tool execution
**What:** If an EJB call throws an exception during tool execution, the client SHALL catch the error and send the error message as the tool result (role `tool`, content describing the error). The LLM can then gracefully handle the failure (e.g. inform the user or try a different approach).

**Why:** Tool execution failures should not crash the conversation. The LLM is designed to handle tool errors and can produce a useful response even without the tool result.

### Decision 10: Structured Message content for tool_use round trips
**What:** The `Message` model currently has `content` as a plain `String`. For tool calling round trips, assistant messages from the LLM contain structured content (Anthropic: content blocks with `type: "tool_use"`, `id`, `name`, `input`; OpenAI: `tool_calls` array alongside `content`). To support stateless round trips, j-lawyer-ai must return these assistant messages in `AiResponse.messages` and the client must send them back unchanged.

**Solution:** The `Message.content` field SHALL remain a `String` but MAY contain a JSON string for structured assistant messages. j-lawyer-ai normalizes the provider-specific tool_use format into a provider-agnostic JSON structure stored in `content`. When sending the follow-up request, j-lawyer-ai reconstructs the provider-specific format from the normalized JSON before calling the LLM.

**Normalized format for assistant tool_use messages:**
```json
{
  "type": "tool_use_request",
  "text": "optional text the assistant said before the tool call",
  "toolCalls": [
    {"id": "call_123", "name": "search_cases", "arguments": "{\"query\": \"Müller\"}"}
  ]
}
```

The `Message.role` remains `"assistant"` for these messages. The processors detect the JSON structure in `content` and reconstruct the provider-specific format.

**Why:** Keeps the `Message` DTO simple (no new fields for structured content) while supporting full round-trip fidelity. The client does not need to understand the internal structure -- it just passes the messages back unchanged.

### Decision 11: Tool call result display in chat UI
**What:** After a tool is executed (or denied/failed), the chat panel SHALL display a concise status message in the conversation, e.g.:
- Success: "Aktensuche: 'Müller' — 3 Treffer"
- Failure: "Aktensuche: 'Müller' — Fehler: Verbindung fehlgeschlagen"
- Denied: "Aktensuche: 'Müller' — abgelehnt"

**Why:** The user needs to understand what data the LLM based its answer on. Without visible tool results, the LLM's answer appears to come from nowhere.

### Decision 12: OllamaProcessor tool support
**What:** `OllamaProcessor` SHALL support tool calling using the Ollama API's OpenAI-compatible tool format (same `tools` / `tool_calls` JSON structure as OpenAI). Only models with `supports-tools="true"` will receive tool definitions.

**Why:** Many Ollama-hosted models (e.g. llama3, mistral) support tool calling. Excluding Ollama would limit tool calling to cloud-only providers.

### Decision 13: TOOL_CALL_PENDING status propagation
**What:** When a tool call is pending, both `AiResponse.status` and `AiRequestStatus.status` SHALL be set to `TOOL_CALL_PENDING`. On the j-lawyer-ai side, `V2PipelineRunner` sets `AiResponse.status = TOOL_CALL_PENDING` and the `handleSync()` method in `AiRestEndpointV2` copies it to `AiRequestStatus.status` (instead of hardcoding `"FINISHED"`). On the j-lawyer-org side, the client checks `AiRequestStatus.status` to detect tool calls.

**Why:** The existing code in `handleSync()` hardcodes `status.setStatus("FINISHED")`. This must be changed to propagate the response status, so `TOOL_CALL_PENDING` reaches the client. Both status objects must agree to avoid confusion.

**Change in `AiRestEndpointV2.handleSync()`:**
```java
// Before:
status.setStatus("FINISHED");

// After:
status.setStatus(response.getStatus());
```

### Decision 14: Tool calling restricted to chat request type
**What:** Tool calling is only supported for the `chat` request type, which is processed by `AssistantChatPanel`.

**Why:** The chat panel is the only UI component that supports a multi-turn conversation loop. Other request types (summarize, explain, generate, etc.) use `AssistantGenericPanel` which does a single request/response.

### Decision 12: Conversation state management
**What:** When j-lawyer-ai returns a `TOOL_CALL_PENDING` response, it includes the full conversation message history (including the assistant's tool_use message) in `AiResponse.messages`. The client sends this message history back in the follow-up request along with the new tool-role message.

**Why:** j-lawyer-ai is stateless -- it does not keep conversation state between requests. The client must send the full conversation each time.

## Risks / Trade-offs

- **Structured message complexity:** The normalized JSON format in `Message.content` adds a layer of serialization. Each LLM processor must handle both plain text content and structured JSON content when building the LLM API request.

- **Multiple round trips:** Each tool call requires a full synchronous round trip through the EJB layer. Acceptable because tool calls are infrequent within a conversation (typically 1-3).
- **User friction:** Requiring approval for every tool call may be tedious. Mitigated by "always allow" and "allow for session" options.
- **Token consumption:** Multiple LLM calls per conversation increase token usage. Each round trip re-sends the full conversation history.
- **Provider compatibility:** Not all models support tool calling. Mitigated by `supportsTools` flag on the model -- client only sends tools when the model supports them.
- **Request size:** Sending tool definitions in every request increases payload size. Acceptable because tool definitions are small (a few KB).

## Resolved Questions

- **Token deduction for denied tool calls:** No tokens are deducted for intermediate `TOOL_CALL_PENDING` rounds (post-processors are skipped). Tokens are deducted only once on the final `FINISHED` response. This applies regardless of whether the user approved or denied tool calls along the way.
