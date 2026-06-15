# build-system Specification

## Purpose
TBD - created by archiving change migrate-ant-to-maven. Update Purpose after archive.
## Requirements
### Requirement: Unified Maven Reactor Build
The project SHALL be built by a single Maven reactor rooted at a parent/aggregator `pom.xml`, replacing all Ant (`build.xml`/`nbproject`) builds. A single command (`mvn install` from the repository root) SHALL build every module in dependency order.

#### Scenario: Full build from root
- **WHEN** a developer runs `mvn install` in the repository root with JDK 17 active
- **THEN** all modules compile, test, and package without invoking Ant or any per-module `JAVA_HOME` switching

#### Scenario: Reactor build order from dependencies
- **WHEN** the reactor resolves the module graph
- **THEN** build order is derived from declared inter-module `<dependency>` edges (not from a hand-ordered shell script)

#### Scenario: No Ant build artifacts remain
- **WHEN** the migration is complete
- **THEN** no module relies on `build.xml`, `nbproject/build-impl.xml`, or the shell scripts for its build, and produced deliverables are equivalent in substance to the previous Ant outputs

### Requirement: Java 17 Toolchain
All modules SHALL compile and run on Java 17 (`/home/jens/bin/jdk-17.0.9-full/`), including `j-lawyer-backupmgr`, which SHALL be migrated off the Java 8 `javafxpackager` to `org.openjfx` dependencies.

#### Scenario: Single JDK for the whole reactor
- **WHEN** the reactor is built
- **THEN** every module targets `release=17` and no module requires JDK 8 or JDK 11

#### Scenario: Backup manager packaging on Java 17
- **WHEN** `j-lawyer-backupmgr` is built
- **THEN** it uses `org.openjfx:javafx-*` dependencies and produces a runnable jar via shade/assembly with Main-Class `org.jlawyer.backupmgr.BackupManagerLauncher`

### Requirement: WildFly 26.1.3 / javax Namespace Preserved
The server deliverable SHALL continue to target WildFly 26.1.3 (`/home/jens/bin/wildfly-26.1.3.Final/`) using the Jakarta EE 8 `javax.*` namespace. The migration SHALL NOT change library versions or perform a `javax`→`jakarta` namespace migration.

#### Scenario: Unchanged runtime target
- **WHEN** the EAR built by Maven is deployed to WildFly 26.1.3
- **THEN** it deploys and runs with the same `javax.*` APIs and the same dependency versions as the prior Ant build

### Requirement: In-Project File Repository for Dependencies
Dependencies SHALL be resolved through an in-project file-based Maven repository seeded from the existing committed `lib/` jars, preserving exact artifact versions. Libraries not available on Maven Central (e.g. `jboss-client`, `javafx.*`, `config_schulung`, `jortho`, `sam`, `swingx`, NetBeans `swing-layout`/`beans-binding`, the beA wrapper) SHALL be provided from this repository with stable GAVs.

#### Scenario: Offline-resolvable proprietary jar
- **WHEN** the reactor resolves the beA wrapper or another non-Central jar
- **THEN** it is fetched from the in-project file repository with the same binary content as the current `lib/` jar

#### Scenario: No implicit upgrades
- **WHEN** dependencies are migrated to Maven coordinates
- **THEN** the version of every dependency matches the version embedded in the current `lib/` jar filename, with no upgrades

### Requirement: EAR Assembly Equivalence
The `j-lawyer-server` EAR SHALL be assembled by `maven-ear-plugin` and SHALL preserve the existing module set, web context-roots, EAR `lib/` bundle directory, EAR `MANIFEST.MF` Class-Path, and the hand-maintained descriptors `application.xml`, `META-INF/jboss-app.xml`, and `META-INF/jboss-deployment-structure.xml`.

#### Scenario: Context-roots unchanged
- **WHEN** the Maven EAR is assembled
- **THEN** `j-lawyer-io.war` is bound to `/j-lawyer-io`, `j-lawyer-server-io.war` to `/j-lawyer.io`, and `j-lawyer-server-war.war` to `/j-lawyer-server-war`

#### Scenario: Descriptors and shared libs preserved
- **WHEN** the assembled EAR is inspected
- **THEN** it contains `jboss-app.xml` (security-domain `jlawyer-application-security-domain`), `jboss-deployment-structure.xml`, the shared `lib/` jars, and a `MANIFEST.MF` Class-Path resolving the file-I/O jars

### Requirement: JPA Metamodel Generation Preserved
Modules that currently generate the JPA static metamodel via annotation processing (`j-lawyer-server-entities`, `j-lawyer-server-ejb`) SHALL continue to generate equivalent metamodel classes under Maven via an annotation processor configured on the compiler plugin.

#### Scenario: Metamodel available to dependents
- **WHEN** `j-lawyer-server-entities` is compiled by Maven
- **THEN** the generated `*_` metamodel classes are produced and available to dependent modules as before

### Requirement: NetBeans GUI Builder Compatibility
Modules with NetBeans GUI Builder forms (notably `j-lawyer-client`) SHALL use the standard Maven source layout (`src/main/java`) with `.java` and `.form` files co-located, so the GUI Builder remains usable.

#### Scenario: Editing a form under Maven
- **WHEN** a developer opens a `.form`-backed class in NetBeans for the Maven `j-lawyer-client` project
- **THEN** the GUI Builder loads and can edit the form, and the build keeps the `.java`/`.form` pair consistent

