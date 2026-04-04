# Change: Add dedicated permission role for AI agent tools

## Why
Currently all authenticated users have unrestricted access to AI agent tools (Ingo). A dedicated `aiAgentRole` permission is needed so administrators can control which users may execute AI tool calls, for compliance and cost management reasons.

## What Changes
- Wire existing `chkIntegrationsIngo` checkbox in `UserAdministrationDialog` to persist/load `aiAgentRole` in `security_roles`
- Add Flyway migration `V3_5_0_15` that grants `aiAgentRole` to all existing users (opt-out model)
- Add role check in `ToolRegistry.getToolDefinitions()` and `ToolRegistry.execute()` to deny tool access when the user lacks `aiAgentRole`
- Replace placeholder text in `AssistantChatPanel` and `AssistantGenericPanel` with a red permission-denied message when the user lacks `aiAgentRole`

## Impact
- Affected specs: ai-assistant-integration
- Affected code:
  - `j-lawyer-client/.../configuration/UserAdministrationDialog.java` (role save/load)
  - `j-lawyer-client/.../configuration/UserAdministrationDialog.form` (already contains checkbox)
  - `j-lawyer-client/.../assistant/ToolRegistry.java` (role gate)
  - `j-lawyer-client/.../assistant/AssistantChatPanel.java` (UI feedback)
  - `j-lawyer-client/.../assistant/AssistantGenericPanel.java` (UI feedback)
  - `j-lawyer-server-entities/src/java/db/migration/V3_5_0_15__AddAiAgentRole.sql` (migration)
