## Context

Currently, the j-lawyer desktop client (Swing) communicates directly with the beA API via the BeaWrapper library. Each client instance manages its own beA session, certificate handling, and message caching (EHCache). The beAstie Spring Boot application already provides a complete REST API wrapping BeaWrapper with session management, making it an ideal backend for server-side beA access.

### Architecture Before (Thick Client)
```
┌──────────────────────┐
│  j-lawyer Client     │
│  ┌────────────────┐  │
│  │  BeaAccess      │  │    direct
│  │  (Singleton)    │──┼──────────> beA API (BRAK)
│  │  + BeaWrapper   │  │
│  │  + EHCache      │  │
│  └────────────────┘  │
└──────────────────────┘
```

### Architecture After (Server-Side via beAstie)
```
┌──────────────────────┐     EJB Remote    ┌──────────────────┐    REST/HTTP    ┌────────────┐
│  j-lawyer Client     │ ───────────────>  │  j-lawyer Server │ ─────────────> │  beAstie   │
│  ┌────────────────┐  │                   │  ┌────────────┐  │                │  (Spring)  │
│  │  BeaAccess      │  │                   │  │ BeaService  │  │                │  port 7080 │
│  │  (thin proxy)   │  │                   │  │ (EJB)      │  │                │            │
│  └────────────────┘  │                   │  └────────────┘  │                │  BeaWrapper│
└──────────────────────┘                   └──────────────────┘                │  + beA API │
                                                                               └────────────┘
```

## Goals / Non-Goals

**Goals:**
- Move all beA API communication from client to server
- Reuse beAstie REST API as the backend (no direct BeaWrapper usage in j-lawyer-server)
- Keep the existing client UI classes functional with minimal changes
- BeaService manages beAstie sessions (login/logout/token lifecycle)
- Serializable DTOs shared between client and server (in j-lawyer-server-api)

**Non-Goals:**
- Changing the beAstie application itself (it remains as-is)
- Exposing beA operations through j-lawyer's own REST API (v7) at this time
- Implementing server-side caching (beAstie handles its own caching if needed)
- Multi-user session pooling (each user has their own beAstie session via their certificate)

## Decisions

### 1. HTTP Client for beAstie Communication
- **Decision**: Use `java.net.http.HttpClient` (Java 11+) in BeaService for calling beAstie REST API
- **Alternatives considered**:
  - JAX-RS Client (RESTEasy): Would add dependency complexity in EJB context
  - Apache HttpClient: Extra dependency, not needed
- **Rationale**: java.net.http.HttpClient is lightweight, built into Java 11+, no extra dependencies

### 2. DTO Location
- **Decision**: Place serializable DTO classes in `j-lawyer-server-api` module under `com.jdimension.jlawyer.services.bea.rest`
- **Alternatives considered**:
  - j-lawyer-server-common: Would work but server-api is the standard location for client-server contracts
  - Reuse beAstie DTOs directly: Not possible since beAstie is a separate codebase
- **Rationale**: j-lawyer-server-api is already shared between client and server, contains all remote interfaces

### 3. Session Management Strategy
- **Decision**: BeaService identifies the calling user via `SessionContext.getCallerPrincipal().getName()`, looks up the user's beA certificate and password from `AppUserBean` (via JPA), and manages the beAstie bearer token entirely server-side. The client simply calls `login()` with no parameters - the server resolves everything from the authenticated principal.
- **Token registry**: A `@Singleton` companion bean (`BeaSessionRegistry`) maps j-lawyer principal names to beAstie bearer tokens. The `@Stateless` BeaService checks this registry before every call.
- **Session expiry handling**: Transparent auto-re-login. When a beAstie call returns 401 (session expired), BeaService automatically re-authenticates using the user's certificate/password from the database and retries the original request. The client never sees session expiry errors.
- **Alternatives considered**:
  - Client sends certificate + password to server: Unnecessary transfer since credentials are already stored server-side in `AppUserBean`
  - Pass beAstie token to client and have client send it with every call: Adds complexity to all 20+ remote methods
  - Create a `@Stateful` session bean: Adds lifecycle complexity, resource consumption
- **Rationale**: Server-managed sessions with principal-based credential lookup keep the client extremely simple (no credentials to pass). Follows the existing pattern used by `CalendarService`, `MessagingService`, `ReportService` etc. which all use `context.getCallerPrincipal().getName()` to identify the user.

### 4. Certificate Handling
- **Decision**: BeaService reads the beA certificate (`byte[]`) and password from `AppUserBean` via the EJB principal name. The certificate is base64-encoded and sent to beAstie `/api/v1/auth/login`. The client never transfers certificate data.
- **Password decryption**: `AppUserBean.beaCertificatePassword` is stored encrypted via `Crypto.encrypt()`. BeaService must decrypt it using `Crypto.decrypt()` before sending to beAstie.
- **Rationale**: Certificates and passwords are already stored on the server (DB). No need to transfer them from client - the server has everything needed to authenticate with beAstie.

### 5. JSON Serialization in BeaService
- **Decision**: Use Jackson (already available in WildFly/JBoss) or javax.json for JSON parsing in BeaService
- **Rationale**: WildFly ships with Jackson modules, no extra dependencies needed

## Risks / Trade-offs

- **Network latency**: Adding a hop (client -> server -> beAstie -> beA) increases latency
  - Mitigation: beAstie runs on the same host or local network as j-lawyer-server
- **beAstie availability**: New external runtime dependency
  - Mitigation: BeaService returns clear error messages when beAstie is unreachable
- **Session timeout**: beAstie sessions expire after 9 minutes
  - Mitigation: BeaService transparently re-authenticates on 401 response and retries the request
- **Large attachments**: beA messages can have large attachments (base64 encoded)
  - Mitigation: Attachments are already transferred over EJB remoting; size limits remain the same
- **Breaking change**: Older clients won't find BeaServiceRemote
  - Mitigation: Coordinate client+server release; beA feature already requires specific client version (controlled via `jlawyer.global.bea.enabledversions`)

## Migration Plan

1. **Phase 1**: Create DTOs and BeaService interface (no client changes yet)
2. **Phase 2**: Implement BeaService EJB with beAstie REST calls
3. **Phase 3**: Refactor BeaAccess in client to delegate to BeaServiceRemote
4. **Phase 4**: Remove BeaWrapper and EHCache dependencies from client
5. **Phase 5**: Test end-to-end with beAstie running alongside j-lawyer-server

Rollback: Not planned. Direct BeaWrapper code will be removed from the client entirely.

## Resolved Questions

1. **beAstie URL configuration**: Configurable via `BeaConfigurationDialog.java`. The existing `txtEndpoint` text field (backed by `ServerSettings.SERVERCONF_BEAENDPOINT`) is repurposed to hold the beAstie base URL instead of the direct beA API endpoint. The label will be updated accordingly.
2. **Fallback to direct BeaWrapper**: No fallback. BeaWrapper and EHCache dependencies are fully removed from the client. beAstie is the sole access path.
3. **Session expiry handling**: Transparent auto-re-login. BeaService stores the user's certificate and password in the session registry, and automatically re-authenticates against beAstie when a 401 response is received, then retries the original request.
