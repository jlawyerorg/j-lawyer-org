## 1. AI Library Updates (j-lawyer-fax)
- [ ] 1.1 Add `actionId`, `asyncRecommended`, and `modelRef` fields to `AiCapability.java`
- [ ] 1.2 Create new `AiModel.java` DTO class with fields: `name`, `description`, `provider`, `supportedRequestTypes`, `local`, `configurations`
- [ ] 1.3 Refactor `AssistantAPI.submitRequest()`: replace `StringBuilder` JSON construction with `JsonObject`/`JsonArray` (json-simple), and update to call v2 type-specific endpoints (`/v2/{requestType}` or `/v2/{requestType}-async`) with v2 request body format (`actionId`, `model` instead of `requestType`, `modelType`)
- [ ] 1.4 Update `AssistantAPI.getCapabilities()` to call `/v2/actions` and parse action JSON into `AiCapability` objects
- [ ] 1.5 Add new `AssistantAPI.getModels()` method to call `/v2/models` and return `List<AiModel>`
- [ ] 1.6 Update `AssistantAPI.getRequestStatus()` to call `/v2/request-status/{id}` instead of `/v1/request-status/{id}`

## 2. Client-Side Updates (j-lawyer-client)
- [ ] 2.1 Update `AssistantAccess.java` to use `actionId` when submitting requests and to pass model information
- [ ] 2.2 Update `AssistantFlowAdapter.java` to map `actionId` and model references correctly
- [ ] 2.3 Update assistant dialog classes (`AssistantGenericDialog`, `AssistantChatDialog`, `AssistantGenerateDialog`, `AssistantExtractDialog`, `AssistantVisionDialog`) to pass `actionId` instead of capability name when calling `submitRequest()`
- [ ] 2.4 Update `AssistantAccess.populateMenu()` to use v2 action properties (`asyncRecommended` instead of `async`)

## 3. Testing
- [ ] 3.1 Verify capability/action discovery against running j-lawyer-ai v2 instance
- [ ] 3.2 Verify sync and async request submission for each request type
- [ ] 3.3 Verify status polling for async requests
