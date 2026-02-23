## Context
The BeaService EJB (51 methods) provides full beA mailbox access via beAstie. Currently only the desktop client can use it through EJB remoting. This change adds a REST facade in j-lawyer-io to make the same functionality available over HTTP/JSON.

## Goals / Non-Goals
- Goals: Expose all BeaService operations as RESTful HTTP endpoints; follow existing j-lawyer-io patterns exactly
- Non-Goals: New business logic (pure delegation); new DTOs (reuse existing Bea* classes); mobile/web client implementation

## Decisions

### Single endpoint class
- Decision: One `BeaEndpointV8` class with all beA methods, path prefix `/v8/bea`
- Alternatives: Split into multiple endpoints (BeaAuthEndpointV8, BeaPostboxEndpointV8, etc.)
- Rationale: Existing pattern uses one class per domain (CasesEndpointV7, MessagingEndpointV7). beA is one cohesive domain. New version v8 since this is a new API surface.

### Reuse existing DTOs directly
- Decision: Use `com.jdimension.jlawyer.services.bea.rest.*` classes as JSON request/response objects
- Alternatives: Create REST-specific POJOs with mapping
- Rationale: The Bea DTOs are already simple POJOs (getters/setters, no JPA annotations). They serialize to JSON via JAX-RS/Jackson without modification. Avoids duplication.

### Security context propagation
- Decision: REST endpoint looks up `BeaServiceLocal` via JNDI; the EJB container propagates the HTTP Basic Auth principal to BeaService's `SessionContext.getCallerPrincipal()`
- Rationale: Standard Java EE behavior - EJB-to-EJB local calls propagate security context. No additional auth handling needed.

### URL structure
- Decision: Resource-oriented paths mirroring the beA domain hierarchy
  - `/v8/bea/login`, `/v8/bea/logout` (session)
  - `/v8/bea/postboxes/{safeId}/folders/{folderId}/messages` (nested resources)
  - `/v8/bea/identities/{safeId}` (identity lookup)
  - `/v8/bea/eeb/*` (eEB operations)
- Rationale: RESTful naming that's intuitive for API consumers

### Authorization
- Decision: All endpoints require `loginRole` (same as other endpoints)
- Rationale: beA access is already gated by per-user certificate configuration in AppUserBean. The loginRole ensures the user is authenticated; BeaService handles beA-specific authorization via the user's certificate.

## Risks / Trade-offs
- Large attachment payloads over REST (base64-encoded in JSON) may be slower than EJB binary serialization. Acceptable for mobile/web clients.
- safeId path parameters may contain special characters (URL encoding needed). JAX-RS handles this automatically for `@PathParam`.

## Open Questions
- None - this is a straightforward REST facade over an existing EJB.
