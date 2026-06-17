## ADDED Requirements

### Requirement: Single Version per Third-Party Coordinate

Every third-party `groupId:artifactId` SHALL resolve to exactly one version across the
whole reactor, pinned centrally in the root `dependencyManagement`. The coordinates that
`migrate-dependencies-to-central` left pinned per-module because they were vendored at
multiple versions (e.g. `org.apache.poi:poi`, `com.google.guava:guava`,
`org.apache.commons:commons-lang3`, `org.slf4j:slf4j-api`) SHALL be consolidated to a
single agreed version — normally the highest version already in use — and any module code
that depended on the older API SHALL be adjusted so behavior is preserved.

#### Scenario: One version per coordinate in the reactor

- **WHEN** the resolved dependency tree of the full reactor is inspected
- **THEN** each third-party `groupId:artifactId` appears at exactly one version, and that
  version is declared once in the root `dependencyManagement` (no per-module `<version>`
  override remains for the previously-duplicated coordinates)

#### Scenario: Upgraded module keeps working

- **WHEN** a module is lifted from its older version to the consolidated version (e.g.
  `j-lawyer-server-ejb` POI 4.1.2 → 5.2.3, or `j-lawyer-fax` Guava 14.0.1 → 32.1.1-jre)
- **THEN** the module compiles against the new API, its affected feature is smoke-tested,
  and it behaves equivalently to before the lift

#### Scenario: Namespace and runtime target unchanged

- **WHEN** the jaxb coordinates (`javax.xml.bind:jaxb-api`, `com.sun.xml.bind:jaxb-impl`)
  are consolidated
- **THEN** they stay on the `javax.*` 2.3.x line, with no `javax`→`jakarta` change and the
  same WildFly 26.1.3 runtime target
