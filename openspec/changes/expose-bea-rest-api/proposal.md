# Change: Expose BeaService as REST API in j-lawyer-io

## Why
The BeaService EJB is currently only accessible via EJB remote invocation from the desktop client. Exposing it as a REST API under `/rest/v8/bea/` enables mobile apps, web clients, and third-party integrations to access beA (besonderes elektronisches Anwaltspostfach) functionality through standard HTTP/JSON, consistent with how other services (cases, calendar, messaging, etc.) are already exposed.

## What Changes
- **New `BeaEndpointV8`** REST endpoint class in `j-lawyer-server/j-lawyer-io` exposing all BeaService operations under `/rest/v8/bea/`
- **New `BeaEndpointLocalV8`** local interface for the endpoint
- **Register** the new endpoint in `EndpointServiceLocator.getClasses()`
- **Reuse existing DTOs** from `com.jdimension.jlawyer.services.bea.rest.*` as JSON request/response objects (no new DTOs needed)
- The REST endpoint delegates to `BeaServiceLocal` via JNDI lookup, following the established pattern

## Impact
- Affected specs: `bea-integration` (extends with REST layer)
- Affected code:
  - `j-lawyer-server/j-lawyer-io/src/java/org/jlawyer/io/rest/v8/BeaEndpointV8.java` (new)
  - `j-lawyer-server/j-lawyer-io/src/java/org/jlawyer/io/rest/v8/BeaEndpointLocalV8.java` (new)
  - `j-lawyer-server/j-lawyer-io/src/java/org/jlawyer/io/rest/v1/EndpointServiceLocator.java` (add registration)
- No breaking changes to existing APIs
- Depends on: `add-server-side-bea-via-beastie` (BeaService EJB must exist)
