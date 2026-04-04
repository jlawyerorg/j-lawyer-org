## 1. Database Migration
- [x] 1.1 Create Flyway script `V3_5_0_15__AddAiAgentRole.sql` that inserts `aiAgentRole` for every existing user in `security_roles`

## 2. User Administration Dialog
- [x] 2.1 Add `aiAgentRole` handling in `getRolesFromUI()` — map `chkIntegrationsIngo` checkbox to role name `aiAgentRole`
- [x] 2.2 Add `aiAgentRole` handling in `setRoles()` — set `chkIntegrationsIngo` selected state based on loaded roles
- [x] 2.3 Reset `chkIntegrationsIngo` to `false` at the start of `setRoles()`

## 3. ToolRegistry Access Control
- [x] 3.1 Add role check method `hasAiAgentRole()` that reads current user's roles via `UserSettings.getInstance().getUserRoles()`
- [x] 3.2 Gate `getToolDefinitions()` — return empty list when user lacks `aiAgentRole`
- [x] 3.3 Gate `execute()` — return error JSON when user lacks `aiAgentRole`

## 4. Assistant Panel UI Feedback (AssistantChatPanel + AssistantGenericPanel)
- [x] 4.1 In `AssistantChatPanel`: check `aiAgentRole` before displaying placeholder text; show red permission-denied message when role is missing
- [x] 4.2 In `AssistantGenericPanel`: apply the same role check and red permission-denied message
