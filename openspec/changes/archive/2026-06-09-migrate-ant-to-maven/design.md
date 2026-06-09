## Context

j-lawyer.org is a Java EE / WildFly application with a Swing desktop client. The build is today a mix of:
- **Ant** (NetBeans `build.xml` importing generated `nbproject/build-impl.xml`): `j-lawyer-fax`, `j-lawyer-server-common`, `j-lawyer-server-entities`, `j-lawyer-server-api`, `j-lawyer-io-common`, `j-lawyer-client`, and the EAR `j-lawyer-server` (sub-modules `j-lawyer-server-ejb`, `j-lawyer-server-io`, `j-lawyer-server-war`, `j-lawyer-io`).
- **Maven**: `j-lawyer-cloud`, `j-lawyer-invoicing`, `j-lawyer-backupmgr` (and the `j-lawyer-ide` dev tool).
- **Shell glue**: `build-fast.sh` switches `JAVA_HOME` between JDK 8 / 11 / 17 and invokes each module in a fixed order.

Key constraints confirmed with the owner:
1. **Dependencies**: ~452 jars are committed under `lib/` folders; many are not on Maven Central (`jboss-client.jar`, `javafx.*`, `config_schulung.jar`, `jortho.jar`, `sam.jar`, `swingx.jar`, `dsn.jar`, `concurrent.jar`, the beA wrapper, NetBeans `swing-layout`/`beans-binding`, etc.). → Use an **in-project file-based Maven repository**; keep exact versions; **no upgrades**.
2. **OpenAPI**: replace jaxrs-analyzer with OpenAPI libraries, output must be **semantically identical** (clients keep working); formatting/ordering may differ; verified by a golden-file test.
3. **backupmgr**: migrate to **Java 17 + OpenJFX** (drop `javafxpackager`).
4. **Layout**: move to **standard Maven layout** so the NetBeans GUI Builder (`.form`) keeps working in the client.

## Goals / Non-Goals

**Goals**
- One reactor: `mvn -T1C install` from repo root builds every deliverable in dependency order.
- All modules compile and run on Java 17; target server WildFly 26.1.3 (`javax.*`, Jakarta EE 8).
- Byte-identical *deliverables in substance*: same EAR module set / context-roots, same client classpath, same REST API + semantically identical `swagger.json`.
- NetBeans remains usable, including the GUI Builder for `.form` classes.

**Non-Goals**
- Upgrading any library version (log4j 2.17.1/2.17.2 split, Lucene 4.7.0, POI, Hibernate 5.3, etc. stay as-is).
- Jakarta namespace migration (`javax.*` → `jakarta.*`). Stays `javax.*` on WildFly 26.
- Changing runtime behavior, REST contract, EJB remote interfaces, or DB schema.
- Migrating installers (`install4j`) or the nightly release pipeline beyond pointing them at Maven outputs.

## Decisions

### D1 — Reactor structure: root parent + aggregator POM
Create `pom.xml` at repo root as both parent (shared `<properties>`, `<dependencyManagement>`, `<pluginManagement>`, `maven.compiler.release=17`) and aggregator (`<modules>`). Establish build/reactor order via real `<dependency>` edges, replacing the hand-ordered shell script:

```
j-lawyer-fax
j-lawyer-server-common
j-lawyer-io-common
j-lawyer-cloud
j-lawyer-invoicing
j-lawyer-server-entities      (depends: server-common)
j-lawyer-server-api           (depends: fax, server-common, server-entities)
j-lawyer-server (EAR aggregator)
  ├─ j-lawyer-server-ejb       (depends: entities, api, server-common, fax, cloud)
  ├─ j-lawyer-server-war       (depends: ejb, entities, api, server-common, fax)
  ├─ j-lawyer-server-io        (depends: ejb, entities, api, server-common, io-common)
  ├─ j-lawyer-io               (depends: ejb? entities, api, server-common) — REST + OpenAPI
  └─ ear                        (assembles the WARs + ejb-jar + entities)
j-lawyer-client               (depends: server-common, entities, api, fax, cloud, invoicing, bea-wrapper)
j-lawyer-backupmgr            (Java 17 + OpenJFX; standalone)
```
`j-lawyer-ide` and any nascent `j-lawyer-mcpserver` are **out of scope**: they are not part of the reactor and keep their existing standalone builds. `j-lawyer-invoicing` is currently only consumed by the client (its server copy is already commented out) — it is included in the reactor and wired as a client dependency.

GAV convention: `org.jlawyer:<module>:<version>`, version inherited from parent. The already-Mavenized modules keep their existing artifactIds where sensible.

### D2 — Dependency resolution via in-project file repository
Add a project-local repository (e.g. `./maven-repo/` declared as `<repository><url>file://${maven.multiModuleProjectDirectory}/maven-repo</url></repository>`). Install every non-Central jar there with deterministic GAVs derived from the existing filename+version (e.g. `jboss-client.jar` → `org.jboss:jboss-client:<wildfly-26.1.3>`, `config_schulung.jar` → `org.jlawyer.thirdparty:config-schulung:1`). A one-time, scripted `mvn install:install-file` pass (or a checked-in repo tree) seeds it from the current `lib/` jars so **versions never change**.

- The **beA wrapper** (`j-lawyer-proprietary/libs/j-lawyer-bea-wrapper.jar`) and other proprietary/no-Central jars live in this repo; the proprietary directory remains the source of truth.
- Per-module `lib/` folders and their committed jars are removed from the modules once each artifact exists in the file repo (the file repo becomes the single committed jar store).
- Rationale over alternatives: `system` scope breaks WAR/EAR packaging (system deps aren't bundled) and is deprecated; full Maven Central remapping risks subtly different artifacts and is an implicit upgrade — both rejected by the chosen strategy.

### D3 — EAR assembly with maven-ear-plugin
`j-lawyer-server` becomes `packaging=ear` using `maven-ear-plugin`, preserving exactly:
- Modules: `j-lawyer-server-ejb.jar` (ejbModule), `j-lawyer-server-entities.jar` (java/ejb module as today), `j-lawyer-server-war.war`, `j-lawyer-server-io.war`, `j-lawyer-io.war`.
- **Context roots** (must not change): `j-lawyer-io.war` → `/j-lawyer-io`, `j-lawyer-server-io.war` → `/j-lawyer.io`, `j-lawyer-server-war.war` → `/j-lawyer-server-war`.
- EAR `lib/` (the ~40 shared server jars) via `<defaultLibBundleDir>lib</defaultLibBundleDir>` / `bundleDir`, matching today's layout so the EAR `MANIFEST.MF` `Class-Path` (`lib/jcifs…`, `lib/jsch…`, `lib/commons-vfs2…`, `lib/commons-net…`) and the file I/O it enables keep resolving.
- Hand-maintained descriptors copied verbatim into the EAR: `application.xml` (today auto-generated by NetBeans → now an explicit, generated-equivalent descriptor or `generateApplicationXml=true` with matching module/context-root config), `META-INF/jboss-app.xml` (security-domain `jlawyer-application-security-domain`), `META-INF/jboss-deployment-structure.xml` (dom4j exclusions/dependencies).

### D4 — JPA metamodel / annotation processing
`j-lawyer-server-entities` and `j-lawyer-server-ejb` currently run annotation processing (`-Aeclipselink.canonicalmodel.use_static_factory=false`) for the JPA static metamodel. Preserve equivalent generation via the Hibernate JPA metamodel generator (`org.hibernate:hibernate-jpamodelgen`, matching the in-use Hibernate 5.3.28) configured on `maven-compiler-plugin` `annotationProcessorPaths`, so generated `*_` metamodel classes remain available downstream.

### D5 — OpenAPI generation replacing jaxrs-analyzer
Today `j-lawyer-io/build.xml` runs `com.sebastian_daschner.jaxrs_analyzer.Main` over compiled classes (`-n j-lawyer-io -v 8 -b swagger --swaggerSchemes http,https`), writes `build/web/swagger-ui/swagger.json`, then post-processes it with `AddBasicAuthToJson` (injects HTTP Basic `securityDefinitions` + global `security`) and `AddSwaggerTagsToJson` (derives tags from URL path segments via a hardcoded map). No OpenAPI annotations exist in the code yet.

Replacement approach:
- Adopt a Swagger 2.0 / OpenAPI annotation library compatible with the existing RESTEasy/JAX-RS stack (e.g. `swagger-core`/`swagger-jaxrs` 1.6.x for Swagger 2.0 output, the format jaxrs-analyzer emits). Generate `swagger.json` at build time via the corresponding `swagger-maven-plugin` (or equivalent), scanning `org.jlawyer.io.rest.*` + `EndpointServiceLocator`.
- Reproduce the post-processing as build configuration: title `j-lawyer-io`, version `8`, basePath `/j-lawyer-io/rest`, schemes `http,https`, the HTTP Basic `securityDefinitions` + global `security`, and the same tag set/assignment (port `AddSwaggerTagsToJson`'s mapping into config or a retained post-step).
- Place the generated `swagger.json` into the `j-lawyer-io.war` `swagger-ui/` directory; Swagger UI `index.html` keeps loading the relative `swagger.json` (unchanged).
- **Compatibility gate**: capture today's generated `swagger.json` as a golden file; add a build/test step that parses both old and new JSON and asserts **semantic equivalence** (deep-equal after canonical key sorting and array-order normalization for paths/params/definitions/tags). Differences in whitespace/ordering are tolerated; any structural delta (missing/renamed path, changed param, changed schema, changed security) fails the build.
- Retire `tools/jaxrs-analyzer.jar` and the two `org.jlawyer.io.rest.tools` post-processors once the gate passes.

### D6 — NetBeans GUI Builder compatibility (client)
Move `j-lawyer-client` sources to `src/main/java` (keeping `.java` + `.form` pairs together) and resources to `src/main/resources`. NetBeans recognizes Maven projects and the Matisse GUI Builder operates on `.form` files in the standard layout; the `swing-layout`/`beans-binding` jars (from the file repo) remain on the compile classpath. Client packaging produces the runnable jar + `dist/lib/` equivalent via `maven-jar-plugin` (Main-Class `com.jdimension.jlawyer.client.Main`, the existing JVM args stay in launcher/installer config) and `maven-dependency-plugin`/assembly to lay out dependency jars as the installer expects.

### D7 — backupmgr on Java 17 + OpenJFX
Replace JDK-8 `jfxrt.jar` bootclasspath + `javafxpackager -createjar` with `org.openjfx:javafx-*:17` dependencies and `maven-shade-plugin` (or `maven-assembly-plugin`) producing the runnable fat jar (Main-Class `org.jlawyer.backupmgr.BackupManagerLauncher`). Removes the last Java 8 requirement; whole reactor runs on one JDK.

## Risks / Trade-offs

- **OpenAPI byte-difference** → semantic golden-file gate (D5) is the contract; document that raw bytes may differ. If a consumer truly needs byte-identical output, fall back to wiring jaxrs-analyzer as a build step (rejected default).
- **File-repo GAV churn** → pick GAVs once, script the seeding, commit the repo tree; never re-derive ad hoc.
- **EAR descriptor drift** (auto-generated `application.xml` today) → pin an explicit application.xml and diff the assembled EAR contents against an Ant-built reference EAR before switching.
- **NetBeans Ant-project muscle memory** → `nbproject/`, `build.xml`, `build-impl.xml` are deleted per module; communicate the IDE change. `.form` workflow is preserved.
- **Big-bang vs incremental** → implement module-by-module in reactor order (fax → common → … → ear → client → backupmgr), keeping the shell scripts working against a hybrid state until the last module flips, to keep the project buildable throughout.

## Migration Plan

1. Add root parent/aggregator POM + seed the in-project file repository from current `lib/` jars (no upgrades).
2. Convert leaf libs first (`j-lawyer-fax`, `j-lawyer-server-common`, `j-lawyer-io-common`), then `entities`, `api`.
3. Fold existing Maven modules (`cloud`, `invoicing`) into the reactor; replace antrun copy-to-lib with reactor deps.
4. Convert EAR sub-modules and the EAR; diff assembled EAR vs Ant reference (modules, context-roots, lib/, descriptors, manifest).
5. Swap OpenAPI generation in `j-lawyer-io`; add golden-file equivalence gate; retire jaxrs-analyzer + post-processors.
6. Convert `j-lawyer-client` (standard layout, verify GUI Builder + runnable jar/installer layout).
7. Migrate `j-lawyer-backupmgr` to Java 17 + OpenJFX.
8. Replace `build-fast.sh`/`build.sh`/`clean.sh` with `mvn` invocations; update `deploy.sh`, `.gitignore`, docs; verify full `mvn install` + WildFly 26.1.3 deploy + client launch.

Rollback: changes are additive per module until scripts flip; the Ant builds remain in git history and can be restored module-by-module if a Maven module regresses.

## Open Questions

- Exact GAVs for the proprietary/no-Central jars — proposed scheme `org.jlawyer.thirdparty:<name>:<version-from-filename>`; confirm during implementation.
- Whether the installer (`install4j`) layout expectations require a specific `dist/lib/` structure beyond what `maven-dependency-plugin` produces.

## Out of Scope

- `j-lawyer-ide` and `j-lawyer-mcpserver` are **not** migrated and **not** joined to the reactor; they keep their existing standalone builds.
