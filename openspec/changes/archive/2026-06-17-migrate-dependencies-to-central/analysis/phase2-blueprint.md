# Phase 2 — per-module migration blueprint (established on j-lawyer-fax / Task 2.1)

Reproducible procedure for migrating each module's `jlawyer.thirdparty:*` dependencies to
real Maven Central coordinates, using the verified mapping in
[`lib-gav-map-central.tsv`](lib-gav-map-central.tsv).

## Per-dependency decision (keyed on the verified mapping by `artifactId:version`)

| mapping status | action in the module pom |
|---|---|
| **CENTRAL_OK**, coordinate NOT in the 11-conflict set | replace with real `groupId:artifactId`, **drop `<version>`** (resolved from root `dependencyManagement`) |
| **CENTRAL_OK**, coordinate IN the 11-conflict set (commons-lang3, guava, poi, poi-scratchpad, slf4j-api, junit, commons-net, commons-logging, activation, jaxb-api, jaxb-impl) | replace with real `groupId:artifactId`, **keep the explicit `<version>`** (per-module pin; consolidation deferred to `consolidate-duplicate-dependency-versions`) |
| **SHA_MISMATCH** or **NOT_ON_CENTRAL** | **leave the `jlawyer.thirdparty:*` block unchanged** (residual; served from the in-project file repo) |
| duplicate `<dependency>` block | remove the duplicate |

Preserve `<scope>` (e.g. `test`), `<classifier>`, and `<type>` from the original block.

## Invariants to check after editing a module pom

1. Every version-less (managed) dependency's `groupId:artifactId` exists in the root
   `<dependencyManagement>` — else the build fails with "version missing". (Verified for fax.)
2. The pom stays well-formed XML.
3. The residual `jlawyer.thirdparty:*` coordinates the module still references must remain
   seeded by the file repo (do not delete their backing `lib/` jars yet).

## Deferred to later phases (do NOT do per-module here)

- **Removing committed `lib/` jars** (Phase 5): the file repo is seeded from *all* modules'
  `lib/` jars, so a jar may still seed an unmigrated module's synthetic coordinate. Remove
  jars only once no `jlawyer.thirdparty:*` reference to that coordinate remains anywhere.
- **Transitive reconciliation** (Phase 3): real coordinates reintroduce real transitive
  graphs the flat jars hid. After migrating a module, run a dependency-tree diff
  (`mvn -pl <module> -am dependency:tree`) against the pre-migration tree and reconcile any
  new/changed transitive versions via `dependencyManagement`/`exclusions`.

## Verification gate (manual build — not run here per project rule)

After each module:
```
mvn -pl <module> -am clean install     # Java 17
mvn -pl <module> -am dependency:tree    # diff vs pre-migration baseline
```
Then smoke-test the module's feature. Only proceed to the next module once green.

## Pre-existing finding for Task 2.7 (j-lawyer-client) — conflicting duplicate declarations

Surfaced as build warnings during fax verification (these are in the client pom, untouched
by Task 2.1). The client pom declares the same synthetic coordinate twice at **different**
versions; Maven silently resolves last-declared-wins. When migrating the client, dedupe
each to the effective (last-wins) version:

| coordinate | decl. 1 | decl. 2 | effective (last-wins) → use this |
|---|---|---|---|
| `jlawyer.thirdparty:forms` | line 382: 9.0.0 | line 527: 1.0.6 | **1.0.6** |
| `jlawyer.thirdparty:ical4j` | line 522: 0.9.18 | line 557: 1.0-beta3 | **1.0-beta3** (matches design D5) |
| `jlawyer.thirdparty:ecs` | line 537: 1.4.2 | line 562: 0.0.0 | **0.0.0** |
| `jlawyer.thirdparty:hamcrest-core` | line 357: 1.3 | line 572: 1.3 | 1.3 (harmless dup) |

## Transitive reconciliation observed on fax (Phase 3 pattern)

`mvn -pl j-lawyer-fax dependency:tree` after the coordinate migration confirmed the real
coordinates reintroduce transitives the flat jars hid. Two were **conflicting** and were
excluded in the fax pom; the rest are benign and kept (real graph, per D5):

| new transitive | via | verdict |
|---|---|---|
| `javax.ws.rs:javax.ws.rs-api:2.0` | jersey stack | **kept as the single JAX-RS API**; the previously-direct `jboss-jaxrs-api_2.1_spec:2.0.2.Final` (a flat-model stand-in) was **removed** so only one JAX-RS jar remains. A per-dependency exclusion was rejected — the jar leaks via several jersey modules (whack-a-mole). |
| `log4j:log4j:1.2.12`, `logkit:logkit:1.0.1`, `avalon-framework:4.1.3`, `javax.servlet:servlet-api:2.3` | commons-logging:1.1 | **excluded** — legacy JCL optionals; conflict with log4j2 + servlet-api 3.0.1 |
| `org.eclipse.persistence:jakarta.persistence:2.2.3` | eclipselink-jpa | kept (needed by eclipselink) |
| `javax.validation:validation-api:1.1.0.Final` | jersey-server | kept (non-conflicting) |
| `org.apache.ws.commons.util` → `xml-apis:1.0.b2` | ws-commons-util | kept (non-conflicting) |
| `javax.inject:javax.inject:1` | hk2-api | kept (JSR-330 annotations, identical to hk2's repackage) |

Rule of thumb for the remaining modules: after the tree diff, exclude only transitives that
**duplicate a direct dependency or an already-present API** (two JAX-RS / two servlet-api /
log4j1+log4j2); keep non-conflicting net-new transitives.

### Server EAR group (Task 2.6) — two more reconciliations (applied to ejb/war/io/j-lawyer-io/ear)

| coordinate | leaked transitives | action |
|---|---|---|
| `org.apache.jena:jena-core:2.7.4` | `log4j:log4j:1.2.16`, `org.slf4j:slf4j-log4j12:1.6.4`, `xerces:xercesImpl:2.10.0` | **excluded** — log4j 1.x + a 2nd SLF4J binding (clashes with log4j2/log4j-slf4j-impl) + a shadowing XML parser; none were in the flat model |
| `org.wildfly.core:wildfly-cli:18.1.2.Final:client` (recovered from `jboss-cli-client`) | ~60 WildFly/Elytron/Remoting jars | **excluded `*:*`** — the `client` classifier is a self-contained uber jar (the old `jboss-cli-client.jar`); its declared transitives would duplicate the whole WildFly graph |

More reconciliations were found only at the **EAR bundling** stage. The EAR `lib/`
(skinny-modules → shared `lib/`) still contained `log4j-1.2.16`, `slf4j-log4j12-1.6.4`,
`xerces-2.9.1`, `junit-3.8.1`. A `dependency:tree -Dverbose` on the EAR pinpointed the
**actual** sources (the first guesses — jena-core/java-rdfa — were wrong; only the verbose
tree settles it):

| leaked jar | real source (EAR direct compile dep) | exclusion |
|---|---|---|
| `log4j:log4j:1.2.16` + `org.slf4j:slf4j-log4j12:1.6.4` | `org.apache.jena:jena-iri:0.9.4` | exclude both on jena-iri |
| `xerces:xercesImpl:2.9.1` | `org.apache.odftoolkit:odfdom-java` (+ `simple-odf`) | exclude xercesImpl on both |
| `junit:junit:3.8.1` | `org.apache.ws.commons.util:ws-commons-util:1.0.2` | exclude junit on ws-commons-util |

Applied to all five EAR-group modules. (The earlier jena-core / java-rdfa / wildfly-cli
exclusions are still correct defense for their own subtrees.)

**Lesson:** for a skinny-modules EAR, the assembled `lib/` listing is the real integration
check, and `dependency:tree -Dverbose -Dincludes=…` is the only reliable way to find which
direct dependency actually drags a jar in — exclusions work, but only on the dep that owns
the path Maven actually resolves (nearest-wins). Benign net-new transitives kept here: poi-excelant→ant (ExcelAnt genuinely
needs Ant), mysql-connector-j→protobuf-java, jollyday→threeten-extra/jaxb-runtime, the
jakarta CDI api graph — all `provided`.

> Note: consolidating commons-logging 1.1 → 1.2 (in `consolidate-duplicate-dependency-versions`)
> makes those JCL optionals `optional` upstream and removes the need for the exclusions.

## Status

- **2.1 j-lawyer-fax** — pom coordinates migrated (27 managed, 6 per-module pinned, 4
  residual synthetic kept, 1 duplicate removed) **+ transitive reconcile** (2 conflicting
  transitives excluded). **Build green**, dependency-tree reviewed. Pending: feature smoke
  test; lib-jar removal deferred to Phase 5.
