# Change: AI tools for case links and contact relationships

## Why

Case links (`case-linking`, entity `CaseLink`) and contact relationships
(`add-contact-relationships`, entity `ContactRelation`) form two graphs that the desktop client
can display, but Ingo cannot read either of them. Questions such as "Which cases are related to
2026/0815?", "Who belongs to the client's circle?", "Does anyone close to the opponent appear
in our other cases?" or "How are Müller and Schmidt connected?" are therefore out of reach for
the assistant.

## What Changes

- Seven new **read-only** tools in the client-side `ToolRegistry`, all `RISK_LOW` (no approval
  dialog):
  - Basic reads, one server call each: `get_case_links`, `get_contact_relations`,
    `get_cases_for_contact`.
  - Analysis tools that combine several reads on the client: `get_case_network`,
    `get_contact_network`, `find_party_connections`, `find_connection`.
- The analysis tools are bounded by a per-call budget of server calls and nodes, do not expand
  hub contacts with very many cases, and report `truncated` with a reason when a bound was hit.
- A new helper class `NetworkToolSupport` holds the JSON rendering and the graph traversal,
  behind a small data-source interface so it can be unit tested without a server.
- No server, REST or schema change: the tools only use existing remote reads
  (`getCaseLinks`, `getRelations`, `getArchiveFileAddressesForAddress`,
  `getInvolvementDetailsForCase`), which already apply the caller's permissions.
- Writing tools (link cases, add a relationship) are out of scope.

## Impact

- Affected specs: `ai-assistant-integration`
- Affected code:
  - `j-lawyer-client/.../client/assistant/ToolRegistry.java`
  - `j-lawyer-client/.../client/assistant/NetworkToolSupport.java` (new)
  - `j-lawyer-client/src/test/java/.../client/assistant/NetworkToolSupportTest.java` (new)
