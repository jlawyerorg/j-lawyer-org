## 1. Implementation

- [x] 1.1 Add `NetworkToolSupport` with the data-source interface, the JSON rendering of links,
      relationships and involvements, and the bounded traversals for case network, contact
      network, party connections and shortest connection
- [x] 1.2 Register the seven tools in `ToolRegistry` (definitions, `execute` dispatch, call
      summaries) with a remote-backed data source
- [x] 1.3 Unit test `NetworkToolSupportTest` for traversal, role comparison, hub handling,
      budget truncation and path finding

## 2. Verification

- [ ] 2.1 Manual check in the Ingo chat against Docker (admin:a) with two linked cases, a
      mother/child relationship and a contact that is party in two cases with different roles
- [ ] 2.2 Manual check that a linked case the user may not see appears neither in the links nor
      in a connection path
