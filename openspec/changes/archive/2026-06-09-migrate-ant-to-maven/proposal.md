# Change: Migrate build from Ant to Maven (Java 17)

## Why

The project currently mixes Apache Ant (NetBeans-generated `build.xml`) for most modules with Maven for a few, glued together by hand-maintained shell scripts (`build-fast.sh`, `build.sh`, `clean.sh`) that switch `JAVA_HOME` between JDK 8 and JDK 11 per module. This is fragile, hard to onboard, IDE-coupled (NetBeans `nbproject/build-impl.xml`), and blocks moving to a single, current JDK. Migrating to a unified Maven reactor on Java 17 gives one reproducible build (`mvn install`), a single dependency model, and a foundation for the later library modernization (explicitly out of scope here).

## What Changes

- **BREAKING (build only, no runtime behavior change):** Replace all Ant `build.xml`/`nbproject` builds with Maven `pom.xml` modules under one root aggregator/parent POM. `mvn install` from the root builds the whole project in dependency order.
- Standardize on **Java 17** (`/home/jens/bin/jdk-17.0.9-full/`) for all modules, including `j-lawyer-backupmgr` (migrated off the Java 8 `javafxpackager` to `org.openjfx` dependencies + shade/assembly packaging).
- Target runtime stays **WildFly 26.1.3** (`/home/jens/bin/wildfly-26.1.3.Final/`), Jakarta EE 8 / `javax.*` namespace — unchanged.
- Resolve the ~452 currently git-committed `lib/*.jar` dependencies via an **in-project file-based Maven repository** (exact same artifacts/versions; no Maven Central remapping, no upgrades).
- Convert the EAR (`j-lawyer-server`) and its sub-WARs/EJB-jar to `maven-ear-plugin` / `maven-war-plugin` / `maven-ejb-plugin`, preserving `application.xml` module set, context-roots, `jboss-app.xml`, `jboss-deployment-structure.xml`, and EAR `MANIFEST.MF` Class-Path.
- Move every module to **standard Maven layout** (`src/main/java`, `src/main/resources`, `src/test/java`) so the **NetBeans GUI Builder (`.form`)** keeps working in `j-lawyer-client`.
- Replace **jaxrs-analyzer** (bytecode-scanning Swagger 2.0 generation + the two custom post-processors `AddBasicAuthToJson`, `AddSwaggerTagsToJson`) with standard **OpenAPI annotation libraries**, while guaranteeing the produced `swagger.json` stays **semantically identical** (same paths, params, schemas, responses, securityDefinitions, tags, version `8`) — enforced by a golden-file equivalence test. Swagger UI keeps serving the JSON as a static file.
- Replace the Maven `antrun` copy steps in `j-lawyer-cloud` / `j-lawyer-invoicing` (which copy jars into other modules' `lib/`) with proper reactor dependencies.
- Provide a single root build (`mvn install`) and update/retire `build-fast.sh`, `build.sh`, `clean.sh`, `deploy.sh` accordingly.

## Impact

- Affected specs: `build-system` (new), `rest-api-documentation` (new)
- Affected code (all modules):
  - Maven-already (into reactor): `j-lawyer-cloud`, `j-lawyer-invoicing`, `j-lawyer-backupmgr`
  - Out of scope (stay standalone): `j-lawyer-ide`, `j-lawyer-mcpserver`
  - Ant→Maven: `j-lawyer-fax`, `j-lawyer-server-common`, `j-lawyer-server-entities`, `j-lawyer-server-api`, `j-lawyer-io-common`, `j-lawyer-client`, and the EAR `j-lawyer-server` with sub-modules `j-lawyer-server-ejb`, `j-lawyer-server-io`, `j-lawyer-server-war`, `j-lawyer-io`
  - REST/OpenAPI: `j-lawyer-server/j-lawyer-io` (resource classes `org.jlawyer.io.rest.v1..v8`, `EndpointServiceLocator`, `tools/jaxrs-analyzer.jar`, `web/swagger-ui/`)
  - Proprietary: `j-lawyer-proprietary/libs/j-lawyer-bea-wrapper.jar` (stays an external/file-repo artifact)
  - Build scripts at repo root; `.gitignore`
- Not changed: runtime behavior, REST API contract, EJB remote interfaces, DB schema, dependency versions (modernization is a follow-up).
