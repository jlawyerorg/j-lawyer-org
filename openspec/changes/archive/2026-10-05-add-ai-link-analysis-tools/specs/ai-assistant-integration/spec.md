## ADDED Requirements

### Requirement: Case Link and Contact Relationship Tools
The client-side `ToolRegistry` SHALL provide read-only tools that let the AI assistant read and analyse case links and contact relationships: `get_case_links`, `get_contact_relations`, `get_cases_for_contact`, `get_case_network`, `get_contact_network`, `find_party_connections` and `find_connection`. They SHALL be registered with risk level `RISK_LOW` and therefore SHALL NOT require an approval dialog. They SHALL only use existing server reads that apply the caller's permissions, so that a case the user may not see is never disclosed.

#### Scenario: Read the links of a case
- **WHEN** the LLM calls `get_case_links` with a case id
- **THEN** the client SHALL return every link of the case oriented towards the other case, with link id, description, the other case's id, file number, name, reason and archived flag, and creation date and user

#### Scenario: Read the relationships of a contact
- **WHEN** the LLM calls `get_contact_relations` with a contact id
- **THEN** the client SHALL return every relationship with the label as seen from the given contact, the type name, the symmetric flag, the note and the other contact's id, display name, company and city

#### Scenario: Read the cases of a contact
- **WHEN** the LLM calls `get_cases_for_contact` with a contact id
- **THEN** the client SHALL return every case the contact is a party in, with the party type and the party's reference
- **AND** archived cases SHALL be omitted when `includeArchived` is false

#### Scenario: Restricted cases stay hidden
- **WHEN** a linked case or a case a contact is involved in belongs to a group the user is not a member of
- **THEN** that case SHALL NOT appear in the result of any of these tools, including connection paths

#### Scenario: Case network
- **WHEN** the LLM calls `get_case_network` with a case id and a depth between 1 and 3
- **THEN** the client SHALL return every case reachable over case links within that depth, each with its depth and the case and link description it was reached through
- **AND** SHALL list the contacts that are a party in more than one of these cases together with their role per case

#### Scenario: Contact network
- **WHEN** the LLM calls `get_contact_network` with a contact id and a depth between 1 and 2
- **THEN** the client SHALL return the contacts reachable over relationships within that depth and the relationships between them
- **AND** SHALL add each contact's cases with role when `includeCases` is true

#### Scenario: Party connections of a case
- **WHEN** the LLM calls `find_party_connections` with a case id
- **THEN** the client SHALL return, for every party of the case, the other cases the same contact is a party in with its role there, flagging `roleDiffers` when that role differs from the role in the given case
- **AND** SHALL return the directly related contacts of every party that are a party in other cases or in the given case
- **AND** the tool description SHALL state that the result is a hint and not a complete conflict-of-interest check

#### Scenario: Connection between two contacts
- **WHEN** the LLM calls `find_connection` with two contact ids and a depth between 1 and 4
- **THEN** the client SHALL search a shortest path over relationships, case involvements and case links and return it as a list of readable statements
- **AND** SHALL return `found: false` when no path exists within the depth

#### Scenario: Bounded traversal
- **WHEN** an analysis tool would exceed 60 server calls or 100 nodes
- **THEN** the client SHALL stop the traversal, return what it found and set `truncated` with a reason
- **AND** a contact that is a party in more than 25 cases SHALL NOT be expanded over its cases, except as the start or target of `find_connection`, and SHALL be marked as `hub`
