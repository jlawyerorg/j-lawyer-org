## Context
The j-lawyer-ai backend has been migrated to REST API v2. The j-lawyer-org desktop client (`j-lawyer-client`) and the shared AI library (`j-lawyer-fax`) need to be updated to call the new v2 endpoints. The v2 API introduces a separation of "models" (AI providers) and "actions" (preconfigured operations), replacing the v1 "capabilities" concept.

Key stakeholders: desktop client users, j-lawyer.CLOUD users.

## Goals / Non-Goals
- Goals:
  - Update `AssistantAPI` REST client to call v2 endpoints
  - Map the v2 action/model concept to existing client data structures
  - Enable model discovery via `/v2/models` endpoint
  - Maintain backward compatibility for users who haven't upgraded their j-lawyer-ai instance yet
- Non-Goals:
  - Adding new UI for model selection (future change)
  - Adding configuration UI for external provider API keys (managed in j-lawyer-ai XML config)
  - Changing the async polling mechanism (works the same in v2)

## Decisions

### Decision 1: Reuse `AiCapability` for v2 actions
The v2 "action" concept is structurally similar to v1 "capability". Instead of creating a parallel class hierarchy, we extend `AiCapability` with new fields (`actionId`, `asyncRecommended`, `modelRef`) and populate them from the `/v2/actions` endpoint. This minimizes changes in all consuming code (dialogs, menus, adapters).

Alternatives considered:
- Create new `AiAction` class: Would require updating all dialog classes, adapters, and menu builders. Too much churn for equivalent functionality.
- Use a wrapper/adapter pattern: Adds unnecessary indirection.

### Decision 2: Add `AiModel` class for model discovery
A new `AiModel` DTO class captures the `/v2/models` response. This is genuinely new data (provider info, supported request types per model, configurations) that has no v1 equivalent.

### Decision 3: Type-specific endpoint routing in `submitRequest()`
The v2 API uses type-specific endpoints (`/v2/summarize`, `/v2/chat`, etc.) instead of a single `/v1/request-submit`. The `submitRequest()` method will route to the correct endpoint based on the `requestType` parameter already passed by callers. For async requests, append `-async` to the path.

### Decision 4: v1 fallback for user-info endpoints
The `/v1/user-info` and `/v1/user-info/history` endpoints have no v2 equivalent. These will remain on v1 paths. This is safe because the j-lawyer-ai server serves both v1 and v2 simultaneously.

### Decision 5: Replace StringBuilder JSON construction with JsonObject/JsonArray
The current `submitRequest()` method manually builds JSON via `StringBuilder` with string concatenation and manual escaping (~100 lines). This is error-prone (missing commas, unescaped strings, trailing comma issues) and hard to maintain. The `json-simple 2.3.0` library (`org.json.simple.JsonObject`, `org.json.simple.JsonArray`) is already on the classpath and already used in the same class for response parsing. The request body will be built using `JsonObject.put()` / `JsonArray.add()` and serialized via `Jsoner.serialize()`.

Alternatives considered:
- Jersey/MOXy auto-serialization with POJOs: Available on classpath (jersey-media-moxy), but would require creating dedicated request DTO classes and configuring the Jersey client for JSON serialization. More infrastructure than needed for a single method.
- Keep StringBuilder: Fragile, already the source of duplicated/commented-out code blocks (see lines 773-787 in current `AssistantAPI.java`). Not acceptable going forward.

### Decision 6: No v1 fallback for core request flow
We will NOT implement a v1 fallback for `submitRequest()` or `getCapabilities()`. If the j-lawyer-ai server doesn't support v2, the client will show an error. Rationale: both client and server are typically deployed together, and maintaining dual-version logic adds complexity without clear benefit.

## API Mapping

### v1 → v2 Endpoint Mapping
| v1 Endpoint | v2 Endpoint | Notes |
|---|---|---|
| `POST /v1/request-submit` | `POST /v2/{requestType}` or `POST /v2/{requestType}-async` | Type-specific, sync/async split |
| `GET /v1/capabilities` | `GET /v2/actions` | Actions replace capabilities |
| `GET /v1/request-status/{id}` | `GET /v2/request-status/{id}` | Same concept, v2 returns `AiResponse` directly |
| `GET /v1/user-info` | `GET /v1/user-info` | No v2 equivalent, stays on v1 |
| `GET /v1/user-info/history` | `GET /v1/user-info/history` | No v2 equivalent, stays on v1 |

### v1 → v2 Request Body Mapping
| v1 Field | v2 Field | Notes |
|---|---|---|
| `requestType` | (encoded in URL path) | No longer in request body |
| `modelType` | `model` | Optional, overrides action's default model |
| `prompt` | `prompt` | Unchanged |
| `inputData` | `inputData` | Unchanged |
| `parameterData` | `parameterData` | Unchanged |
| `configurationData` | `configurationData` | Unchanged |
| `messages` | `messages` | Unchanged |
| (none) | `actionId` | New: references preconfigured action |

### v1 → v2 Capability/Action Field Mapping
| v1 `AiCapability` Field | v2 Action JSON Field | Notes |
|---|---|---|
| `name` | `name` | Display name |
| (none) | `actionId` | New: unique action identifier |
| `requestType` | `requestType` | Unchanged |
| `modelType` | `model` | Model reference name |
| `async` | `asyncRecommended` | Renamed, semantic shift |
| `customPrompts` | `customPrompts` | Unchanged |
| `description` | `description` | Unchanged |
| `input` | `input` | Unchanged |
| `output` | `output` | Unchanged |
| `parameters` | `parameters` | Unchanged |
| `configurations` | `configurations` | Unchanged |
| `usageTypes` | `usageTypes` | Unchanged |
| `defaultPrompt` | (none) | Prompt now part of action server-side |

## Risks / Trade-offs
- **Breaking change for older j-lawyer-ai servers**: If the j-lawyer-ai server hasn't been updated to v2, the client will fail to discover actions and submit requests. Mitigation: document minimum j-lawyer-ai version in release notes.
- **Response format differences in status endpoint**: v2 `GET /request-status/{id}` returns `AiResponse` directly instead of wrapped in `AiRequestStatus`. The current `getRequestStatus()` method already returns `AiResponse`, so this is compatible.

## Open Questions
- Should `defaultPrompt` in `AiCapability` be removed or kept for backward compatibility? Recommendation: keep it but don't populate from v2 (prompts are server-side in v2).
