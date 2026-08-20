# rest-api-documentation Specification

## Purpose
TBD - created by archiving change migrate-ant-to-maven. Update Purpose after archive.
## Requirements
### Requirement: OpenAPI Generation via Standard Libraries
The `j-lawyer-io` REST API documentation (`swagger.json`) SHALL be generated using standard OpenAPI/Swagger annotation libraries and a Maven build plugin, replacing the `jaxrs-analyzer` bytecode-scanning tool and the custom `org.jlawyer.io.rest.tools` post-processors.

#### Scenario: JSON generated during Maven build
- **WHEN** the `j-lawyer-io` module is built by Maven
- **THEN** `swagger.json` is produced by the OpenAPI library/plugin and bundled into the `j-lawyer-io.war` `swagger-ui/` directory, with no dependency on `tools/jaxrs-analyzer.jar`

#### Scenario: Swagger UI keeps loading the static JSON
- **WHEN** a user opens the Swagger UI at `/j-lawyer-io/swagger-ui/`
- **THEN** the UI loads the relative `swagger.json` exactly as before (no UI wiring change)

### Requirement: Swagger JSON Semantic Compatibility
The generated `swagger.json` SHALL remain semantically identical to the previous `jaxrs-analyzer` output so that all existing REST API consumers continue to work. Formatting and key/array ordering MAY differ; the API contract MUST NOT.

#### Scenario: Metadata preserved
- **WHEN** the new `swagger.json` is generated
- **THEN** it declares title `j-lawyer-io`, version `8`, basePath `/j-lawyer-io/rest`, and schemes `http,https`

#### Scenario: Security definitions preserved
- **WHEN** the new `swagger.json` is generated
- **THEN** it contains the same HTTP Basic `securityDefinitions` and the same global `security` requirement previously injected by `AddBasicAuthToJson`

#### Scenario: Tags preserved
- **WHEN** the new `swagger.json` is generated
- **THEN** it contains the same tag set and the same operation-to-tag assignments previously produced by `AddSwaggerTagsToJson`

#### Scenario: Paths and schemas preserved
- **WHEN** the new `swagger.json` is compared against the prior output
- **THEN** the set of paths, HTTP methods, parameters, request/response media types, and model definitions are equivalent, with no added, removed, or renamed entries

### Requirement: Swagger JSON Equivalence Gate
The build SHALL enforce the semantic compatibility above by comparing the freshly generated `swagger.json` against a captured golden reference of the prior output and failing on any structural difference.

#### Scenario: Build fails on contract drift
- **WHEN** a change causes the generated `swagger.json` to add, remove, rename, or alter a path, method, parameter, schema, security definition, or tag relative to the golden reference
- **THEN** the equivalence check fails the build

#### Scenario: Build passes on formatting-only differences
- **WHEN** the generated `swagger.json` differs from the golden reference only in whitespace or key/array ordering
- **THEN** the equivalence check passes after canonical normalization

