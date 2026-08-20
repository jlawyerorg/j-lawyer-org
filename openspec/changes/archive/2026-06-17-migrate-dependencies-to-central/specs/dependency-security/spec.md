## ADDED Requirements

### Requirement: Dependency Vulnerability Monitoring
The project SHALL surface known vulnerabilities (CVEs) in its third-party dependencies.
Because GitHub Dependabot matches declared `groupId:artifactId:version` coordinates
against the advisory database (and does not scan jar binaries), dependencies SHALL use
real coordinates so they are visible to Dependabot, which SHALL be enabled for the Maven
ecosystem across the module POMs.

#### Scenario: CVE in a coordinated dependency is reported
- **WHEN** a dependency declared with real Maven coordinates has a known CVE
- **THEN** Dependabot raises an alert (and, where configured, an update pull request)

#### Scenario: Synthetic coordinates would hide vulnerabilities
- **WHEN** a dependency is declared only under a synthetic `jlawyer.thirdparty:*` coordinate
- **THEN** it is NOT matched by the advisory database and MUST therefore be migrated to real coordinates (or covered by a compensating binary scan)

### Requirement: Coverage of Non-Advisory Artifacts
The project SHALL provide a documented compensating control for artifacts without
advisory-database coverage (proprietary or repackaged jars kept in the residual local
repository), because Dependabot cannot assess them.

#### Scenario: Residual artifacts are covered or documented
- **WHEN** an artifact is not resolvable to advisory-tracked coordinates (e.g. the beA wrapper)
- **THEN** either a binary CVE scanner (e.g. OWASP Dependency-Check / Trivy) assesses it, or the coverage gap is explicitly documented as accepted risk
