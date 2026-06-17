# Change: Migrate dependencies from the in-project file repository to Maven Central

## Why

The Ant→Maven migration (`migrate-ant-to-maven`, archived) deliberately deferred all
library work: third-party jars stay committed under module `lib/` folders and are
resolved through an in-project file repository (`maven-repo/`) under synthetic flat
coordinates `jlawyer.thirdparty:<artifact>:<version>`. This works and is offline-autark,
but it has two structural drawbacks:

1. **No security visibility.** GitHub Dependabot (and the dependency graph) match
   `groupId:artifactId:version` against the advisory database. The synthetic
   `jlawyer.thirdparty:*` coordinates match nothing, so Dependabot is **blind** to the
   ~233 vendored libraries — e.g. a Log4j/commons CVE would raise no alert. Only the few
   real-coordinate deps (cloud, invoicing, backupmgr, jackson, junit, …) are covered.
2. **Repo bloat & manual upkeep.** ~450 binary jars live in git history; upgrades mean
   sourcing jars by hand; there is no real transitive dependency tree, BOM, or tooling.

Moving the bulk of dependencies to Maven Central coordinates fixes both and is the
natural foundation for the later modernization (version consolidation, removing shaded
fat jars).

## What Changes

- Map the vendored `jlawyer.thirdparty:*` artifacts to their **real Maven Central
  coordinates**, version-for-version (no upgrades — behavior must not change).
- Introduce a parent `<dependencyManagement>` / BOM to pin versions in one place.
- Keep a small **residual local repository** (or a private/Nexus repo) only for the
  artifacts that are genuinely not on Central (proprietary or repackaged).
- Remove the committed `lib/` jars and the seeding script once each artifact is sourced
  from Central or the residual repo.
- Re-introduce the real transitive graph: add `<dependencyManagement>`/`<exclusions>`
  where needed to keep the conflict isolation the flat jars implicitly provided.
- Enable **Dependabot** (and optionally a binary CVE scanner in the interim) so CVEs in
  the now-coordinated dependencies are surfaced.
- **BREAKING (build sourcing only, no runtime change intended):** first build now
  requires network access to Maven Central; the offline-autark file repo is retired
  (except the residual artifacts).

## Impact

- Affected specs: `build-system` (MODIFIED dependency-resolution requirement),
  `dependency-security` (new capability)
- Affected code/config:
  - every module `pom.xml` (dependency coordinates), root `pom.xml`
    (`dependencyManagement`/BOM, repository config)
  - `scripts/seed-maven-repo.sh`, `scripts/lib-gav-map.txt`, `maven-repo/` and the
    committed `**/lib/*.jar` (removed/reduced)
  - `.github/dependabot.yml` (new), CI workflows (drop the seed step, optional scanner)
- Not changed: runtime behavior, library versions, the EAR/WildFly target, the shaded
  jars' internal contents.
- Sequencing: implement on a **separate branch after `migrate-ant-to-maven` is merged**;
  this proposal is committed now only to preserve the analysis as the implementation basis.
