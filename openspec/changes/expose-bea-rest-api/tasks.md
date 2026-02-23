## 1. Implementation

- [x] 1.1 Create `BeaEndpointLocalV8.java` interface in `j-lawyer-server/j-lawyer-io/src/java/org/jlawyer/io/rest/v8/`
- [x] 1.2 Create `BeaEndpointV8.java` with all REST methods delegating to `BeaServiceLocal` via JNDI lookup
  - [x] 1.2.1 Auth & session endpoints (login, logout, version, certificate-info)
  - [x] 1.2.2 Postbox endpoints (list, get, outbox-empty, egvp-check)
  - [x] 1.2.3 Folder endpoints (list, get, create, delete)
  - [x] 1.2.4 Message endpoints (list, search, get, get-header, send, draft, delete, restore, move, mark-read, read-check)
  - [x] 1.2.5 Message journal & process card endpoints
  - [x] 1.2.6 Message verification endpoint
  - [x] 1.2.7 Identity endpoints (get, get-with-zip, search)
  - [x] 1.2.8 Reference data endpoints (legal-authorities, default-authority, message-priorities)
  - [x] 1.2.9 eEB endpoints (check-request, check-response, attributes, render-html, rejection-reasons, confirm, reject)
- [x] 1.3 Register `BeaEndpointV8.class` in `EndpointServiceLocator.getClasses()`

## 2. Verification
- [ ] 2.1 Verify the module compiles (no build invocation - manual step)
- [ ] 2.2 Verify Swagger UI picks up the new endpoints after deployment
