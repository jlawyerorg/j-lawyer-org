## MODIFIED Requirements

### Requirement: OpenAPI Generation via Standard Libraries
The `j-lawyer-io` REST API documentation (`swagger.json`) SHALL be generated using standard OpenAPI/Swagger annotation libraries and a Maven build plugin on every build, and SHALL be packaged into the war as a build artifact rather than committed to the repository or hand-maintained.

#### Scenario: JSON generated during Maven build
- **WHEN** the `j-lawyer-io` module is built by Maven
- **THEN** `swagger.json` is produced by the OpenAPI library/plugin and bundled into the `j-lawyer-io.war` `swagger-ui/` directory, with no dependency on `tools/jaxrs-analyzer.jar`

#### Scenario: Generation is unconditional
- **WHEN** the module is built without any special profile or flag
- **THEN** `swagger.json` is regenerated from the current annotations (there is no opt-in `swagger-regen` profile and no committed copy that can go stale)

#### Scenario: Swagger UI keeps loading the static JSON
- **WHEN** a user opens the Swagger UI at `/j-lawyer-io/swagger-ui/`
- **THEN** the UI loads the relative `swagger.json` exactly as before (no UI wiring change)

#### Scenario: Security and tags applied during finalization
- **WHEN** the generated spec is finalized for packaging
- **THEN** `SwaggerFinalizer` injects the HTTP Basic `securityDefinitions`, the global `security` requirement, and the uniform error envelope before the spec is overlaid into `swagger-ui/`

## REMOVED Requirements

### Requirement: Swagger JSON Equivalence Gate
**Reason**: `swagger.json` is now regenerated on every build and packaged as a build artifact; there is no committed baseline to compare against, so the golden reference (`src/test/resources/swagger.golden.json`) and `SwaggerEquivalenceTest` are removed.
**Migration**: API contract changes are reviewed through the normal source/annotation diff. Consumers that relied on a committed `swagger.json` should fetch it from a built `j-lawyer-io.war` (or the running Swagger UI) instead of the repository.
