## 1. Reactor foundation
- [x] 1.1 Create root `pom.xml` (parent + aggregator): `maven.compiler.release=17`, shared `<dependencyManagement>`/`<pluginManagement>`, encoding UTF-8
- [x] 1.2 Configure JDK 17 (`/home/jens/bin/jdk-17.0.9-full/`); WildFly 26.1.3 documented as target
- [x] 1.3 Create in-project file repository (`./maven-repo`) and declare it as a `<repository>`
- [x] 1.4 GAV scheme for non-Central jars + scripted seeding from existing `lib/` jars (`scripts/seed-maven-repo.sh`, no version changes). 233 artifacts seeded.

## 2. Leaf library modules
- [x] 2.1 `j-lawyer-fax` → Maven jar (Main-Class `com.jdimension.jlawyer.fax.FaxClient`)
- [x] 2.2 `j-lawyer-server-common` → Maven jar
- [x] 2.3 `j-lawyer-io-common` → Maven jar

## 3. Core server libs
- [x] 3.1 `j-lawyer-server-entities` → Maven jar; META-INF + Flyway migrations as resources. NOTE: JPA static-metamodel annotation processing dropped — generated `Entity_` classes are unused in the codebase (verified). Runtime unchanged.
- [x] 3.2 `j-lawyer-server-api` → Maven jar

## 4. Existing Maven modules into reactor
- [x] 4.1 `j-lawyer-cloud` → inherits parent; shade kept; antrun copy-to-lib removed (reactor dependency now)
- [x] 4.2 `j-lawyer-invoicing` → inherits parent; shade kept; antrun copy removed; wired as client dependency
- [x] 4.3 jackson pin / bea-wrapper conflict handling retained in cloud

## 5. EAR and sub-modules (`j-lawyer-server`)
- [x] 5.1 `j-lawyer-server-ejb` → Maven ejb-jar
- [x] 5.2 `j-lawyer-server-war` → Maven war (context-root `/j-lawyer-server-war`)
- [x] 5.3 `j-lawyer-server-io` → Maven war (context-root `/j-lawyer.io`)
- [x] 5.4 `j-lawyer-io` → Maven war (context-root `/j-lawyer-io`)
- [x] 5.5 `j-lawyer-server-ear` → maven-ear-plugin: module set, context-roots, `defaultLibBundleDir=lib`, skinnyModules
- [x] 5.6 `application.xml` (generated, v8), `META-INF/jboss-app.xml`, `META-INF/jboss-deployment-structure.xml` carried over
- [x] 5.7 EAR verified: deploys to WildFly 26.1.3 without errors (modules, context-roots, descriptors, and provided/compile lib scoping all correct at runtime)

## 6. OpenAPI generation (replace jaxrs-analyzer)
- [x] 6.1 Captured current `swagger.json` golden (`src/test/resources/swagger.golden.json`; 2.0, v8, 197 paths, 88 defs, 16 tags)
- [x] 6.2 swagger-core toolchain added: `swagger-annotations` dep + kongchen `swagger-maven-plugin` under opt-in profile `swagger-regen` (jaxrs-analyzer cannot read Java 17 bytecode — confirmed)
- [x] 6.3 Plugin reproduces title/version/basePath/schemes
- [x] 6.4 securityDefinition basicAuth configured (global `security` + description still to port — see remainder)
- [x] 6.5 Class-level `@Api(tags=…)` added to all 25 endpoint classes
- [x] 6.6 Served `swagger.json` = committed golden in the WAR (API contract guaranteed unchanged); UI loads it unchanged
- [x] 6.7 Semantic-equivalence gate tool (`scripts/swagger-equivalence-check.py`) — measures gap
- [ ] 6.8 **REMAINING (large, iterative):** annotate all ~208 operations with `@ApiOperation(response=…, responseContainer=…)` + per-op `@ApiResponses` (400/404 where applicable) so the generated spec matches the golden; add global-security + basicAuth description post-step; then retire `tools/jaxrs-analyzer.jar` and the `org.jlawyer.io.rest.tools` post-processors and switch the served file to the generated one. Current gap measured by the gate: securityDefinitions/global-security + 208 operations + 88 definitions.

## 7. Desktop client
- [x] 7.1 Standard Maven layout (`src/main/java`), `.java`+`.form` co-located (292 forms excluded from jar)
- [x] 7.2 All deps wired (file repo + reactor + bea-wrapper). pdfbox 2.0.24 ordered before invoicing (which leaks an unrelocated pdfbox 3.x) so the 2.x API wins.
- [x] 7.3 Runnable jar (Main-Class `com.jdimension.jlawyer.client.Main`) + `target/lib/` layout
- [ ] 7.4 Verify NetBeans GUI Builder opens/edits a `.form` class under Maven — layout is GUI-Builder-compatible; interactive verification pending (handoff)

## 8. Backup manager (Java 17 + OpenJFX)
- [x] 8.1 `org.openjfx:javafx-*:17.0.9` deps replace `jfxrt.jar`/`javafxpackager`
- [x] 8.2 Runnable fat jar via maven-shade (Main-Class `org.jlawyer.backupmgr.BackupManagerLauncher`)

## 9. Scripts, cleanup, verification
- [x] 9.1 `build-fast.sh`/`build.sh`/`clean.sh` rewritten to drive Maven (single root build; seeds maven-repo on first run)
- [x] 9.2 `deploy.sh` points at `j-lawyer-server/j-lawyer-server-ear/target/j-lawyer-server.ear`
- [ ] 9.3 Remove per-module `nbproject/`, `build.xml`, `build-impl.xml`, redundant `lib/` jars — deferred until after deploy verification (kept for now as a safety net / rollback reference)
- [x] 9.4 `.gitignore` updated (`**/target/`; maven-repo committed). CLAUDE.md / project.md build sections: pending doc refresh.
- [x] 9.5 Full `mvn clean install` green on Java 17 (all 13 modules; EAR + client + backupmgr produced). `build-fast.sh` ~71s.
- [x] 9.5b EAR deploys to WildFly 26.1.3 without errors; client launches (incl. JavaFX) via NetBeans/Maven Run; Swagger UI serves the (golden) spec. (Flyway stopper was a DB-state/out-of-order issue, resolved with a matching/fresh DB — not a build problem.)
- [x] 9.6 EJB remote interfaces compile unchanged; server deploys cleanly (remote bindings active)

## Pre-existing issue fixed (out of migration scope, required for clean compile)
- [x] `EmailService.java:1288` called a non-existent `getInboxUnreadCount(MailboxSetup)` (present on master; NetBeans never recompiled it cleanly). Replaced with `getInboxSnapshot(ms).unreadCount`, mirroring the identical pattern at line ~1495. Behavior preserved.
