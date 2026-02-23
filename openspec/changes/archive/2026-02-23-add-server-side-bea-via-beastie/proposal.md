# Change: Move beA access from client to server via beAstie REST API

## Why
The j-lawyer desktop client currently communicates directly with the beA (besonderes elektronisches Anwaltspostfach) API using the BeaWrapper library. This thick-client approach requires the BeaWrapper library and beA certificates on every client machine, makes centralized session management impossible, and prevents future mobile/web clients from accessing beA. By moving beA operations to the server-side `BeaService` EJB, which delegates to the beAstie Spring Boot application's REST API, we centralize beA access, simplify the client, and enable multi-client beA access through a single service layer.

## What Changes

### Server-Side (j-lawyer-server)
- **BeaServiceRemote** (j-lawyer-server-api): Define remote interface with methods mirroring beA operations (login, logout, list postboxes, folder operations, message CRUD, identity lookup, eEB operations)
- **BeaServiceLocal** (j-lawyer-server-ejb): Define local interface for server-side inter-bean calls
- **BeaService** (j-lawyer-server-ejb): Implement EJB that calls beAstie REST API (`http://<beastie-host>:7080/api/v1/...`) using JAX-RS client or `java.net.http.HttpClient`
- **New ServerSettings keys**: `jlawyer.server.bea.beastieurl` for the beAstie base URL
- **DTO classes** in j-lawyer-server-api or j-lawyer-server-common: Serializable transfer objects for postboxes, messages, folders, identities, attachments, eEB data (replacing the `org.jlawyer.bea.model.*` classes that are not available on server classpath)

### Client-Side (j-lawyer-client)
- **BeaAccess**: Refactor from directly using `BeaWrapper` to calling `BeaServiceRemote` via `JLawyerServiceLocator`
- **Remove local BeaWrapper dependency** from client (no more direct beA API calls)
- **Remove local EHCache** for beA data (caching moves to server or beAstie)
- **BeaLoginDialog / BeaLoginPanel**: Adapt to call `BeaServiceRemote.login()` (no credentials needed - server reads them from DB via EJB principal)
- **All 29 beA UI classes** continue to exist but delegate to server instead of local BeaAccess singleton

### Shared (j-lawyer-server-api / j-lawyer-server-common)
- New serializable DTO classes shared between client and server, mapping to beAstie REST API DTOs:
  - **Core:** `BeaPostbox`, `BeaFolder`, `BeaMessageHeader`, `BeaMessage`, `BeaAttachment`, `BeaRecipient`
  - **Identity:** `BeaIdentity`, `BeaIdentitySearchRequest`
  - **Journal/Export:** `BeaMessageJournalEntry`, `BeaMessageExport`, `BeaProcessCard`, `BeaProcessCardEntry`
  - **Verification:** `BeaVerificationResult`
  - **eEB:** `BeaEebRequestAttributes`, `BeaEebResponseAttributes`
  - **Requests:** `BeaSendMessageRequest` (with legalAuthorityCode, priorityCode, eebRequested, confidential), `BeaSaveDraftRequest`, `BeaMessageFilter`, `BeaMoveMessageRequest`, `BeaCertificateInfoRequest`
  - **Misc:** `BeaListItem`, `BeaLoginResult` (postboxes)

### Configuration
- Repurpose existing server setting `jlawyer.server.bea.beaendpoint` (currently the direct beA API URL) to hold the beAstie base URL instead (e.g., `http://localhost:7080`)
- **BeaConfigurationDialog** (`j-lawyer-client/src/.../configuration/BeaConfigurationDialog.java`): The existing `txtEndpoint` text field is reused to configure the beAstie URL (label updated accordingly)
- Existing setting `jlawyer.server.bea.beamode` (on/off toggle) remains unchanged
- beAstie's `allowed-clients` config must include the j-lawyer-server's client ID
- No fallback to direct BeaWrapper in client - beAstie is the sole beA access path

## Impact
- Affected specs: none (new capability)
- Affected code:
  - `j-lawyer-server-api/src/com/jdimension/jlawyer/services/BeaServiceRemote.java`
  - `j-lawyer-server/j-lawyer-server-ejb/src/java/com/jdimension/jlawyer/services/BeaService.java`
  - `j-lawyer-server/j-lawyer-server-ejb/src/java/com/jdimension/jlawyer/services/BeaServiceLocal.java`
  - `j-lawyer-client/src/com/jdimension/jlawyer/client/bea/BeaAccess.java` (major refactor)
  - `j-lawyer-client/src/com/jdimension/jlawyer/client/bea/*.java` (29 classes, adapt to use remote service)
  - `j-lawyer-server-common/src/com/jdimension/jlawyer/server/services/settings/ServerSettingsKeys.java`
  - New DTO classes in `j-lawyer-server-api` or `j-lawyer-server-common`
- **BREAKING**: Client versions < 3.5.0 will not have the `BeaServiceRemote` lookup; requires coordinated server+client update
- External dependency: beAstie Spring Boot app must be running and reachable from j-lawyer-server
