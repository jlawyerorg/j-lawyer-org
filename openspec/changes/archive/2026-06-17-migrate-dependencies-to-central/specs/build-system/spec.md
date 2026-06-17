## MODIFIED Requirements

### Requirement: In-Project File Repository for Dependencies
Dependencies that are available on Maven Central SHALL be resolved from Maven Central
using their canonical `groupId:artifactId:version` coordinates, version-for-version with
the previously vendored jars (no upgrades). Only artifacts that are genuinely not on
Maven Central (proprietary or repackaged — e.g. the beA wrapper and its bundled jars,
`jboss-client`/`jboss-cli-client` which ship with WildFly, and any confirmed-divergent
jars) SHALL be provided from a residual local repository (the in-project file repository
or a private/Nexus repository) with stable coordinates. Shared library versions SHALL be
pinned centrally (parent `dependencyManagement`/BOM) so the transitive graph resolves to
the same versions as before.

#### Scenario: Central-sourced dependency carries real coordinates
- **WHEN** a dependency exists on Maven Central
- **THEN** it is declared with its canonical Central coordinates at the same version as the previously committed jar, not a synthetic `jlawyer.thirdparty:*` coordinate

#### Scenario: Non-Central artifact stays local
- **WHEN** the reactor resolves the beA wrapper or another artifact not on Maven Central
- **THEN** it is provided from the residual local/private repository with the same binary content as before

#### Scenario: No implicit upgrades or behavior change
- **WHEN** a dependency is re-sourced from Maven Central
- **THEN** its version matches the previously vendored version exactly, and if the committed jar diverges from the Central release it is kept local rather than substituted

#### Scenario: Transitive conflicts remain controlled
- **WHEN** real Central coordinates reintroduce transitive dependencies
- **THEN** `dependencyManagement` and dependency `exclusions` (and the existing shaded fat jars consumed via their `:shaded` classifier) keep a single, conflict-free version of each shared library (e.g. pdfbox, groovy, ical4j, httpclient, jackson)
