## Context

After `migrate-ant-to-maven` (archived), dependencies are sourced like this:
- ~233 unique third-party jars are committed under module `lib/` folders (≈450 files
  incl. duplicates) and installed into an in-project file repo `maven-repo/` (git-ignored)
  by `scripts/seed-maven-repo.sh`, under **flat synthetic coordinates**
  `jlawyer.thirdparty:<artifact>:<version>` (version parsed from the file name;
  no-version jars get `0.0.0`). The GAV mapping is recorded in `scripts/lib-gav-map.txt`.
- A few modules already use real Central coordinates: `j-lawyer-cloud`,
  `j-lawyer-invoicing`, `j-lawyer-backupmgr`, plus `com.fasterxml.jackson.core:*`,
  `junit:junit`, `io.swagger:swagger-annotations`, `org.openjfx:javafx-*` (backupmgr),
  `org.mustangproject:validator` (inside invoicing), `org.aarboard.nextcloud:nextcloud-api`,
  `com.github.caldav4j:caldav4j`, `org.apache.jackrabbit:jackrabbit-webdav`,
  `com.googlecode.ez-vcard:ez-vcard`.

This change re-sources the vendored jars from Maven Central so they carry real
coordinates (enabling Dependabot/CVE scanning and proper dependency management), while
keeping the genuinely-non-Central artifacts local.

## Goals / Non-Goals

**Goals**
- Real Central coordinates for every dependency that exists on Central, **version-for-version**.
- Central `dependencyManagement`/BOM; reproducible, network-resolvable build.
- Dependabot coverage + CVE alerts for the coordinated dependencies.
- Remove committed `lib/` jars and the seeding machinery for the migrated artifacts.

**Non-Goals**
- Library **upgrades** or behavior changes (those belong to the separate modernization).
- Removing the shaded fat jars (cloud/invoicing) — that is modernization, not sourcing.
- `javax`→`jakarta` namespace change; WildFly 26.1.3 target stays.

## Decisions

### D1 — Version-for-version mapping, verified against the committed jars
Each `jlawyer.thirdparty:<a>:<v>` maps to its canonical Central GAV at the **same version**.
The committed jar may be a patched/repackaged build, so for each artifact verify the
Central release matches (compare by SHA-1, or by class/manifest spot-check) before
switching. If a committed jar diverges from any Central release, treat it as non-Central
(keep it local, D3) rather than silently substituting — this preserves the
"no behavior change" guarantee. `scripts/lib-gav-map.txt` is the worklist.

### D2 — Central `dependencyManagement` / BOM
Pin all versions in the root `pom.xml` `<dependencyManagement>` (or a dedicated BOM
module) so module poms reference `groupId:artifactId` without versions. This also lets us
resolve the transitive conflicts that the flat jars hid (see D5).

### D3 — Residual repository for non-Central artifacts
Some artifacts are not on Central and must stay local (in the in-project file repo, or a
private/Nexus repo). Known non-Central set from the migration analysis:
- `jboss-client` / `jboss-cli-client` — ship with WildFly (use `${wildfly}/bin/client`),
  not Central; mark `provided`.
- `javafx.*` (the old JDK-8-era extracted jars) — replace with `org.openjfx:javafx-*`
  (Central) for the client, mirroring what backupmgr already does; verify the client's
  JavaFX usage still works on these.
- proprietary **beA wrapper** (`j-lawyer-proprietary/libs/j-lawyer-bea-wrapper.jar`) and
  its bundled jars under `j-lawyer-client/lib/bea/` — proprietary, stay local.
- `config_schulung.jar`, `jortho.jar`, `sam.jar`, `swingx.jar`, NetBeans
  `swing-layout`/`beans-binding`, and any repackaged/no-version jars — verify per item;
  several have Central equivalents (e.g. `org.swinglabs:swingx`, `org.jdesktop:beansbinding`,
  `net.java.dev.swing-layout:swing-layout`) but at possibly different coordinates/versions
  → only switch if version matches, else keep local.
- `bea.bak/` is an unused backup folder (excluded from the build already) — delete, do not migrate.

### D4 — Retire committed jars + seeding once migrated
After an artifact is sourced from Central (or the residual repo), remove its committed
`lib/` copy. When `lib/` is empty of migrated jars, `scripts/seed-maven-repo.sh` and
`scripts/lib-gav-map.txt` become obsolete (keep only if a residual local repo remains,
seeded from the few remaining jars). Update `.gitignore` accordingly. This is the main
repo-size win.

### D5 — Re-introduce transitive management (the conflict caveat)
The flat jars carry **no declared transitives**, which silently avoided conflicts. Real
Central coordinates bring real transitive graphs, which can reintroduce the exact clashes
seen during the Ant→Maven migration:
- **pdfbox 2.x vs 3.x**: the client uses pdfbox 2.0.24 (`MemoryUsageSetting`,
  `PDDocument.load`); mustang (inside invoicing) needs pdfbox 3.x. They cannot coexist
  as `org.apache.pdfbox` — this is why invoicing is a shaded fat jar with a relocated
  `shaded.org.apache.pdfbox`, consumed via the `:shaded` classifier with all transitives
  excluded. Keep that arrangement until the client is moved to pdfbox 3.x (modernization).
- **groovy 2.4.21 vs leaked groovy 2.3.2**: cloud → caldav4j → ical4j 2.0.0 → groovy
  2.3.2 leaked transitively and clashed with the client's groovy-all 2.4.21
  (`InvokerHelper.<clinit>` failed). Also ical4j 2.0.0 (transitive) vs the client's
  ical4j 1.0-beta3.
- Mitigation pattern proven in the migration: shaded fat jars (cloud, invoicing) are
  self-contained; consume them with `<exclusions>*:*</exclusions>` (and a `:shaded`
  classifier where the IDE would otherwise resolve to thin `target/classes`). When moving
  to Central, keep these exclusions and add `<dependencyManagement>` to force single
  versions of shared libs (pdfbox, groovy, ical4j, httpclient, jackson, commons-*).
- cloud relocates `org.apache.http` specifically to avoid a clash with the beA wrapper's
  bundled httpclient; jackson is pinned to 2.16.1 for the same reason. Preserve these.

### D6 — Dependabot + interim binary scanning
- Add `.github/dependabot.yml` (ecosystem: maven, all module poms) once coordinates are
  real. Dependabot reads pom declarations and matches the advisory DB; it does **not**
  scan jar binaries, which is exactly why the synthetic coordinates are invisible today.
- The residual non-Central artifacts (beA wrapper etc.) have no advisory data → not
  covered by Dependabot regardless; acceptable (small set).
- Interim/option: a binary CVE scanner (OWASP Dependency-Check, Trivy, Grype/Syft) as a
  CI step analyzes the actual jars (by hash / embedded `pom.properties`) and can cover the
  gap immediately, independent of coordinates. This could even land before the full
  Central migration since it changes nothing in the build.

## Risks / Trade-offs

- **Silent behavior change** if a committed jar ≠ the Central release of the same version
  → mitigated by D1 (verify per artifact; keep divergent ones local).
- **Reintroduced transitive conflicts** → mitigated by D5 (`dependencyManagement`,
  `exclusions`, keep shaded jars).
- **First build needs network** (Central) → acceptable; CI already has it. Optionally keep
  a fallback/private mirror.
- **Effort**: ~233 artifacts to verify/map; do it incrementally, module by module, with a
  full `mvn clean install` + smoke test (client launch, EAR deploy) after each batch.

## Migration Plan

1. Add root `<dependencyManagement>`/BOM scaffolding; enable a binary CVE scanner in CI
   (optional, immediate visibility).
2. Per module, replace `jlawyer.thirdparty:*` deps with verified Central coordinates;
   remove the corresponding committed `lib/` jars; `mvn clean install` + smoke test.
3. Move the client's `javafx.*` to `org.openjfx:javafx-*`; verify JavaFX UI.
4. Reduce the file repo to the residual non-Central set (or move it to a private repo);
   delete obsolete jars, seeding script, and gav map if fully retired.
5. Add `.github/dependabot.yml`; confirm alerts/PRs appear.
6. Final full build + WildFly deploy + client run; confirm versions unchanged
   (diff dependency tree against the pre-migration set).

Rollback: per-module commits; if a Central-sourced artifact regresses, revert that module
to the committed jar / file-repo entry.

## Open Questions

- Which committed jars are repackaged/patched (≠ Central) and must stay local? (resolve via D1 SHA check)
- Residual repo form: keep the in-project file repo, or stand up a private/Nexus mirror?
- Do `swingx`/`beansbinding`/`swing-layout`/`jortho`/`sam`/`config_schulung` have
  version-matching Central artifacts, or do they stay local?
- Replace the client's bundled `javafx.*` with `org.openjfx` now, or keep until the JavaFX modernization?
