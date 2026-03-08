# Change: Update Ingo AI assistant integration to REST API v2

## Why
The j-lawyer-ai backend has been upgraded to REST API v2, introducing type-specific endpoints (e.g. `/v2/summarize`, `/v2/chat`), a new action/model separation concept, and multi-provider support (Anthropic, OpenAI, Mistral, Gemini, Ollama). The j-lawyer-org client still calls the legacy v1 endpoints (`/v1/request-submit`, `/v1/capabilities`). The client needs to be updated to use the v2 API to take advantage of the new architecture.

## What Changes
- **AssistantAPI.java** (`j-lawyer-fax`): Replace v1 endpoint calls with v2 type-specific endpoints
  - `submitRequest()` calls `/v2/{requestType}` or `/v2/{requestType}-async` instead of `/v1/request-submit`
  - `getCapabilities()` calls `/v2/actions` instead of `/v1/capabilities`
  - `getRequestStatus()` calls `/v2/request-status/{id}` instead of `/v1/request-status/{id}`
  - Request body uses `actionId` + `model` instead of `requestType` + `modelType`
  - **Replace manual `StringBuilder` JSON construction with `JsonObject`/`JsonArray`** from json-simple 2.3.0 (already on classpath, already used for response parsing in the same class)
- **AiCapability.java** (`j-lawyer-fax`): Add new fields to map v2 action properties (`actionId`, `asyncRecommended`, `modelRef`)
- **New AiModel.java** (`j-lawyer-fax`): Add model discovery DTO for `/v2/models` endpoint
- **AssistantAccess.java** (`j-lawyer-client`): Update capability/action discovery and menu population to use v2 data model
- **Assistant dialog classes** (`j-lawyer-client`): Update request submission calls to pass `actionId` instead of capability name
- **v1 fallback endpoints** (`getUserInformation`, `getUserRequestLog`): Keep on v1 as these are not part of v2

## Impact
- Affected specs: ai-assistant-integration (new)
- Affected code:
  - `j-lawyer-fax/src/com/jdimension/jlawyer/ai/AssistantAPI.java` - REST client
  - `j-lawyer-fax/src/com/jdimension/jlawyer/ai/AiCapability.java` - Capability/Action DTO
  - `j-lawyer-fax/src/com/jdimension/jlawyer/ai/AiModel.java` - New model DTO
  - `j-lawyer-client/src/com/jdimension/jlawyer/client/assistant/AssistantAccess.java` - Client-side access layer
  - `j-lawyer-client/src/com/jdimension/jlawyer/client/assistant/AssistantFlowAdapter.java` - Flow adapter
  - Various assistant dialog classes in `j-lawyer-client`
