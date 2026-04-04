## ADDED Requirements

### Requirement: AI Agent Role Authorization
The system SHALL enforce a dedicated `aiAgentRole` permission for all AI tool access. Only users who have been granted this role in `security_roles` SHALL be able to discover or execute AI tools via ToolRegistry.

#### Scenario: User with aiAgentRole accesses tools
- **WHEN** a user with the `aiAgentRole` role requests tool definitions or executes a tool
- **THEN** the system SHALL return the full tool list and execute tools as before

#### Scenario: User without aiAgentRole is denied tool access
- **WHEN** a user without the `aiAgentRole` role requests tool definitions
- **THEN** the system SHALL return an empty tool list
- **AND** any attempt to execute a tool SHALL return an error response

#### Scenario: Assistant panels show permission denied for unauthorized user
- **WHEN** a user without the `aiAgentRole` role opens the `AssistantChatPanel` or `AssistantGenericPanel`
- **THEN** the system SHALL display a red-colored text indicating that permissions for executing AI agents are missing
- **AND** the default "Ingo erledigt Routinearbeiten" placeholder SHALL NOT be shown

#### Scenario: Assistant panels show normal placeholder for authorized user
- **WHEN** a user with the `aiAgentRole` role opens the `AssistantChatPanel` or `AssistantGenericPanel`
- **THEN** the system SHALL display the standard "Ingo erledigt Routinearbeiten" placeholder text

### Requirement: AI Agent Role Default Assignment
The system SHALL grant the `aiAgentRole` to all existing users via a Flyway database migration, so that the role is enabled by default and must be explicitly revoked by an administrator.

#### Scenario: Migration grants role to existing users
- **WHEN** the Flyway migration V3_5_0_15 is applied
- **THEN** every user present in `security_roles` SHALL receive an `aiAgentRole` entry with `roleGroup` = `Roles`

### Requirement: AI Agent Role Administration
The system SHALL allow administrators to manage the `aiAgentRole` via the existing `chkIntegrationsIngo` checkbox in the User Administration Dialog. The role SHALL be persisted to and loaded from the `security_roles` table following the same pattern as all other roles.

#### Scenario: Administrator grants AI agent role
- **WHEN** an administrator selects the `chkIntegrationsIngo` checkbox and saves the user
- **THEN** the system SHALL persist an `aiAgentRole` entry in `security_roles` for that user

#### Scenario: Administrator revokes AI agent role
- **WHEN** an administrator deselects the `chkIntegrationsIngo` checkbox and saves the user
- **THEN** the system SHALL remove the `aiAgentRole` entry from `security_roles` for that user

#### Scenario: Loading user roles reflects AI agent role state
- **WHEN** a user's roles are loaded into the User Administration Dialog
- **THEN** the `chkIntegrationsIngo` checkbox SHALL be selected if the user has `aiAgentRole`, and deselected otherwise
