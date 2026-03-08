## 1. AI Library Updates (j-lawyer-fax)
- [x] 1.1 Add `actionId`, `asyncRecommended`, and `modelRef` fields to `AiCapability.java`
- [x] 1.2 Create new `AiModel.java` DTO class with fields: `name`, `description`, `provider`, `supportedRequestTypes`, `local`, `configurations`
- [x] 1.3 Refactor `AssistantAPI.submitRequest()`: replace `StringBuilder` JSON construction with `JsonObject`/`JsonArray` (json-simple), and update to call v2 type-specific endpoints (`/v2/{requestType}` or `/v2/{requestType}-async`) with v2 request body format (`actionId`, `model` instead of `requestType`, `modelType`)
- [x] 1.4 Update `AssistantAPI.getCapabilities()` to call `/v2/actions` and parse action JSON into `AiCapability` objects
- [x] 1.5 Add new `AssistantAPI.getModels()` method to call `/v2/models` and return `List<AiModel>`
- [x] 1.6 Update `AssistantAPI.getRequestStatus()` to call `/v2/request-status/{id}` instead of `/v1/request-status/{id}`

## 2. Client-Side Updates (j-lawyer-client + j-lawyer-server)
- [x] 2.1 Update `IntegrationServiceRemote` interface: add `actionId` and `model` parameters to `submitAssistantRequest()`
- [x] 2.2 Update `IntegrationService` EJB implementation to pass `actionId` and `model` through to `AssistantAPI`
- [x] 2.3 Update `AssistantAccess.java` to use `actionId` and `modelRef` when submitting requests
- [x] 2.4 Update assistant dialog classes (`AssistantGenericDialog`, `AssistantChatDialog`, `AssistantGenerateDialog`, `AssistantExtractDialog`, `AssistantVisionDialog`) to pass `actionId`/`modelRef` instead of `modelType`
- [x] 2.5 Update integration call sites in `SendEmailFrame`, `HtmlPanel`, `AddNoteFrame`, `AddressFromClipboardDialog`, `CreateAddressStep`

## 3. Model Selection & Configuration Parameters
- [x] 3.1 DB migration: add `model_ref` and `configuration` columns to `assistant_prompts` table
- [x] 3.2 Extend `AssistantPrompt` entity with `modelRef` and `configuration` fields
- [x] 3.3 Add `getAssistantModels()` to `IntegrationServiceRemote` and `IntegrationService`
- [x] 3.4 Add `List<ConfigurationData> promptConfigurations` parameter to `submitAssistantRequest`
- [x] 3.5 Create `ConfigurationUtils` helper class (Properties ↔ ConfigurationData conversion)
- [x] 3.6 Add `configurationValues` field to `AiCapability` (+ clone support)
- [x] 3.7 Create `AssistantPromptV2SetupDialog` with model selection and config parameter editing
- [x] 3.8 Update `AssistantAccess.filterCapabilities()` to set modelRef/configurationValues from prompt
- [x] 3.9 Update `AssistantAccess.populateMenu()` (FlowAdapter) to pass config to submitAssistantRequest
- [x] 3.10 Update assistant dialogs to pass prompt-specific configs to submitAssistantRequest
- [x] 3.11 Redirect `JKanzleiGUI` menu entry to `AssistantPromptV2SetupDialog`
- [x] 3.12 Update all 14 `submitAssistantRequest` call sites with new parameter

## 4. Testing
- [ ] 4.1 Verify capability/action discovery against running j-lawyer-ai v2 instance
- [ ] 4.2 Verify sync and async request submission for each request type
- [ ] 4.3 Verify status polling for async requests
- [ ] 4.4 Verify model selection and configuration in prompt setup dialog
- [ ] 4.5 Verify prompt-specific configs are passed through to AI requests
