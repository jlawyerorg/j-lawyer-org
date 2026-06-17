# Change: Consolidate duplicated third-party dependency versions to a single version

## Why

During `migrate-dependencies-to-central` (Task 1.1), the SHA-1 verification of the
vendored jars surfaced **11 `groupId:artifactId` coordinates that are present at more than
one version** across modules — something the flat `jlawyer.thirdparty:*` jars previously
hid. The pattern is consistent: the desktop **client** carries the newer version while
**server**/**fax** carry an older one (e.g. `org.apache.poi:poi` 5.2.3 vs 4.1.2,
`com.google.guava:guava` 32.1.1-jre vs 14.0.1, `org.apache.commons:commons-lang3` 3.12.0
vs 3.5, `org.slf4j:slf4j-api` 1.7.36 vs 1.7.30).

`migrate-dependencies-to-central` deliberately treats version consolidation as a
**Non-Goal** ("version-for-version, no upgrades — behavior must not change"), so those 11
coordinates are intentionally left out of the root `dependencyManagement` and are pinned
per-module instead. This change captures the deferred follow-up: lift each of those 11
coordinates to a **single agreed version** (normally the highest already in use) so they
can be centrally managed in one place — accepting that some lifts are real upgrades that
may need **code changes** (notably POI 4→5 and Guava 14→32, which have breaking APIs).

## What Changes

- For each of the 11 multi-version coordinates, pick one target version (default: the
  highest version already vendored) and move every module that uses it to that version.
- Move the now-single-version coordinate into the root `dependencyManagement` so module
  poms reference `groupId:artifactId` without a version (closing the per-module exception
  left by `migrate-dependencies-to-central`).
- Where the lift is a real upgrade, review and adjust affected code for API changes
  (POI `org.apache.poi.*`, Guava, jaxb-api `javax.xml.bind`).
- Verify behavior is unchanged at runtime (full build, EAR deploy, client launch, the
  relevant features: document/spreadsheet generation for POI, etc.).

## Impact

- **Depends on:** `migrate-dependencies-to-central` (the per-module pins it introduces are
  the starting point; this change removes those exceptions).
- Affected specs: `build-system` (ADDED requirement: single version per third-party
  coordinate).
- Affected code/config: root `pom.xml` (`dependencyManagement` gains the 11 coordinates),
  module poms (drop per-module versions), and any client/server code that touches the
  upgraded APIs (POI, Guava, jaxb).
- **Not a blocker** for `migrate-dependencies-to-central`: that change ships with the 11
  coordinates pinned per-module; this change is a clean, independent follow-up.
