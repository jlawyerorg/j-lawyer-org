<!-- OPENSPEC:START -->
# OpenSpec Instructions

These instructions are for AI assistants working in this project.

Always open `@/openspec/AGENTS.md` when the request:
- Mentions planning or proposals (words like proposal, spec, change, plan)
- Introduces new capabilities, breaking changes, architecture shifts, or big performance/security work
- Sounds ambiguous and you need the authoritative spec before coding

Use `@/openspec/AGENTS.md` to learn:
- How to create and apply change proposals
- Spec format and conventions
- Project structure and guidelines

Keep this managed block so 'openspec update' can refresh the instructions.

<!-- OPENSPEC:END -->

# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

j-lawyer.org is a legal practice management system built on Java EE architecture. It consists of a rich desktop client (Swing), an application server (WildFly/JBoss), and a MySQL database. The system includes both traditional EJB remote interfaces and modern REST APIs for integration.

## Build Commands

The whole project is a single **Maven reactor** built with **Java 17**
(`/home/jens/bin/jdk-17.0.9-full/`). Dependencies that are not on Maven Central are
resolved from an in-project file repository (`maven-repo/`, git-ignored) that is seeded
on first build from the committed `lib/` jars by `scripts/seed-maven-repo.sh`. The
server targets **WildFly 26.1.3** (`/home/jens/bin/wildfly-26.1.3.Final/`).

### Full Build (with tests)
```bash
./build.sh        # mvn clean install  (seeds maven-repo on first run)
```
Optional integration-test credentials (Sipgate, FTP/SFTP) can be exported beforehand;
they are skipped if unset.

### Fast Build (skip tests)
```bash
./build-fast.sh   # mvn -Dmaven.test.skip=true clean install
```

### Clean
```bash
./clean.sh        # mvn clean   (keeps the in-project maven-repo)
```

### Individual Module Build
```bash
export JAVA_HOME=/home/jens/bin/jdk-17.0.9-full
mvn -pl j-lawyer-server/j-lawyer-server-ejb -am install        # a module + its deps
mvn -pl j-lawyer-client install                                # the desktop client
mvn -pl j-lawyer-server/j-lawyer-server-ear -am install        # the deployable EAR
```

### Running Tests
```bash
mvn test                       # whole reactor
mvn -pl j-lawyer-fax test      # a single module
```
Test classes follow the pattern `*Test.java` (under `src/test/java`).

### REST API spec (swagger.json)
The `j-lawyer-io` swagger.json is generated from swagger-core annotations on **every
build** and packaged into the war as a build artifact — it is no longer committed or
hand-maintained. The `swagger-maven-plugin` (`compile` phase) writes
`target/swagger-gen/swagger.json`; `SwaggerFinalizer` (`process-classes` phase) adds the
security definition + uniform error envelope and writes `target/swagger-final/swagger.json`,
which the `maven-war-plugin` overlays into `swagger-ui/`. No manual regeneration step and
no golden baseline; just change the REST annotations and rebuild.

### Deployment
```bash
./deploy.sh  # copies j-lawyer-server/j-lawyer-server-ear/target/j-lawyer-server.ear to WildFly
```

### NetBeans
Open the root `pom.xml` as a Maven project. The Swing GUI Builder still works on the
`.form` files (now under `src/main/java`). The former Ant `build.xml`/`nbproject/`
files have been removed; if NetBeans recreates them it still has the project open as Ant
— close it and reopen the `pom.xml`.

## Architecture

### Module Structure

**j-lawyer-server** (Enterprise Archive - EAR)
- **j-lawyer-server-ejb**: Business logic implemented as EJB session beans
  - Stateless session beans: `ArchiveFileService`, `AddressService`, `CalendarService`, `SecurityService`, etc.
  - Singleton session beans: `SingletonService`, `ContainerLifecycleBean`, `ScheduledTasksService`
  - Each service has Remote and Local interfaces for client access and server-side calls
- **j-lawyer-server-io**: REST API endpoints (WAR deployed as j-lawyer-io)
  - Versioned API: v1 through v7
  - Swagger UI available at `/j-lawyer-io/swagger-ui/`
  - Authentication: HTTP Basic Auth
- **j-lawyer-server-war**: Additional web components

**j-lawyer-server-entities**
- JPA entity beans representing the data model
- Core entities: `ArchiveFileBean` (cases), `AddressBean` (contacts), `ArchiveFileDocumentsBean`, `AppUserBean`
- Shared by both EJB and REST layers

**j-lawyer-server-api**
- Remote interfaces for EJB services
- Defines contracts like `ArchiveFileServiceRemote`, `SecurityServiceRemote`, `SearchServiceRemote`
- Shared between server and client for remote invocation

**j-lawyer-server-common**
- Shared utilities for both client and server
- Virtual file system abstractions (Local, Samba, SSH/FTP/SFTP)
- Calendar/holiday calculations
- Security utilities (encryption, password hashing)

**j-lawyer-client**
- Rich Swing-based desktop application
- Communicates with server via EJB remote invocation
- Service locator pattern: `JLawyerServiceLocator` class
- Main UI: case management, document management, calendar, contacts

**j-lawyer-cloud**
- Nextcloud/ownCloud integration
- WebDAV file operations, CalDAV calendar sync, CardDAV contact sync

**j-lawyer-invoicing**
- ZUGFeRD/XRechnung invoice generation (German e-invoicing standards)
- Based on Mustang Project library

**j-lawyer-fax**
- VoIP/fax integration (Sipgate API)
- Fax queue management

**j-lawyer-backupmgr**
- Standalone JavaFX application for backup/restore
- Can run independently of main application

**j-lawyer-io-common**
- Common I/O utilities
- System monitoring capabilities

### Technology Stack

- **Java EE/Jakarta EE**: Enterprise framework
- **WildFly (JBoss)**: Application server
- **MySQL**: Database (accessed via JNDI: `java:/jlawyerdb`)
- **Hibernate/JPA 2.1**: ORM (persistence unit: `j-lawyer-server-ejbPU`)
- **Apache Lucene 9.12.0**: Full-text search
- **JAX-RS (RESTEasy)**: REST API implementation
- **Java Swing**: Desktop UI framework
- **FlatLaf**: Modern look and feel
- **Build Tools**: Apache Maven (single reactor, Java 17)

### Build Order

All modules build in one Maven reactor; the order is derived automatically from the
declared inter-module dependencies (no manual ordering). The rough dependency order is:
1. j-lawyer-fax, j-lawyer-server-common, j-lawyer-io-common (leaf libraries)
2. j-lawyer-cloud, j-lawyer-invoicing (shaded Maven jars)
3. j-lawyer-server-entities → j-lawyer-server-api
4. j-lawyer-server (EAR aggregator): j-lawyer-server-ejb, j-lawyer-server-war,
   j-lawyer-server-io, j-lawyer-io → j-lawyer-server-ear (produces j-lawyer-server.ear)
5. j-lawyer-client
6. j-lawyer-backupmgr (Java 17 + OpenJFX)

### Client-Server Communication

**Desktop Client → Server**
- Uses EJB remote invocation (JBoss Remoting Protocol)
- Client looks up EJB references via JNDI: `ejb:j-lawyer-server/j-lawyer-server-ejb//ServiceName!interface`
- `JLawyerServiceLocator` manages service lookups

**External Systems → Server**
- REST API at `/j-lawyer-io/rest/v{1-7}/...`
- HTTP Basic Authentication
- Returns JSON responses

### Persistence Layer

- **Persistence Unit**: `j-lawyer-server-ejbPU`
- **Transaction Type**: JTA (container-managed)
- **Data Source**: `java:/jlawyerdb`
- **Schema Strategy**: validate (DDL changes via SQL scripts, not auto-generated)
- **Core Domain**: Cases (`ArchiveFileBean`), Documents (`ArchiveFileDocumentsBean`), Contacts (`AddressBean`), Calendar, Users/Roles, Invoices, Timesheets

### File Storage

Pluggable backends via `VirtualFile` abstraction:
- Local file system (default: `/opt/jboss/j-lawyer-data/`)
- Samba/CIFS shares
- SSH/SFTP remote storage
- FTP remote storage

### Docker Environment

For testing/development:
```bash
wget https://raw.githubusercontent.com/jlawyerorg/j-lawyer-org/master/docker/docker-compose.yaml
wget https://raw.githubusercontent.com/jlawyerorg/j-lawyer-org/master/docker/run.sh
sh run.sh
```

- Port: 8000
- Database: `jlawyerdb` (user: jlawyer/jlawyer)
- Default credentials: admin/a
- Swagger UI: http://localhost:8000/j-lawyer-io/swagger-ui/
- Volumes:
  - `/var/docker_data/j-lawyer-data/`: Documents and templates
  - `/var/docker_data/j-lawyer-db/`: MySQL data

## Development Guidelines

### NetBeans Project Structure

This codebase is developed using NetBeans IDE as a **Maven** project (open the root
`pom.xml`). Each module is a standard Maven module:
- `pom.xml`: module build definition
- `src/main/java`, `src/main/resources`, `src/test/java`: standard layout
- Swing GUI Builder `.form` files live next to their `.java` under `src/main/java`
- NetBeans Run/Debug for the client is mapped via `j-lawyer-client/nbactions.xml`

The former Ant artifacts (`build.xml`, `nbproject/`) have been removed.

### Code Organization

**Package Naming Conventions**:
- Server EJBs: `com.jdimension.jlawyer.server.ejb`
- Server entities: `com.jdimension.jlawyer.persistence`
- REST API: `org.jlawyer.io.rest.v{N}`
- Client UI: `com.jdimension.jlawyer.client.*`
- Common utilities: `com.jdimension.jlawyer.services.*`

**Service Interface Pattern**:
Each EJB service has two interfaces:
- `ServiceNameRemote`: For client access (in j-lawyer-server-api)
- `ServiceNameLocal`: For server-side inter-bean calls (in j-lawyer-server-ejb)

### API Development

**REST API Versioning**:
- New endpoints go in the latest version (currently v7)
- Never break existing API versions
- Create new version directory if breaking changes are needed
- All endpoints are `@Stateless` EJBs using local EJB services via JNDI lookup

**EJB Development**:
- Mark services as `@Stateless` (most) or `@Singleton` (lifecycle/scheduled)
- Implement both Remote and Local interfaces
- Use `@RolesAllowed` for authorization
- Container-managed transactions (JTA) by default

### Working with Entities

- Entities are in j-lawyer-server-entities module
- Changes affect both EJB and REST layers
- Always use JPA annotations for persistence
- Schema changes require manual SQL migration scripts

### Testing

- Tests are in `test/` directories (Ant modules) or `src/test/` (Maven modules)
- Test classes end with `Test.java`
- Some tests require external credentials (Sipgate, FTP/SFTP) - set via environment variables in build scripts
- EJB tests are in `j-lawyer-server/j-lawyer-server-ejb/test/`

## Common Tasks

### Adding a New REST Endpoint

1. Create endpoint class in `j-lawyer-server/j-lawyer-server-io/src/org/jlawyer/io/rest/v7/`
2. Annotate with `@Stateless`, `@Path`, `@Consumes`, `@Produces`
3. Use JNDI to lookup required local EJB services
4. Return appropriate JSON responses

### Adding a New EJB Service

1. Create interface in `j-lawyer-server-api/src/`
2. Create implementation in `j-lawyer-server/j-lawyer-server-ejb/src/`
3. Annotate implementation with `@Stateless` (or `@Singleton`)
4. Define Remote and Local interfaces
5. Update client's `JLawyerServiceLocator` if client needs access

### Adding a New Entity

1. Create entity class in `j-lawyer-server-entities/src/com/jdimension/jlawyer/persistence/`
2. Add JPA annotations (`@Entity`, `@Table`, `@Id`, etc.)
3. Create corresponding database table via SQL script
4. Rebuild j-lawyer-server-entities module first
5. Update services that need to use the new entity

### Modifying the Client UI

1. UI code is in `j-lawyer-client/src/com/jdimension/jlawyer/client/`
2. Forms are typically designed with NetBeans GUI Builder (.form files)
3. Use `JLawyerServiceLocator` to obtain server references
4. Handle exceptions from remote calls appropriately

## Additional Resources

- Developer Quickstart: https://github.com/jlawyerorg/j-lawyer-developer-quickstart
- Project Website: https://www.j-lawyer.org/
- Contributing Guidelines: See CONTRIBUTING.md
- License: AGPLv3 (see LICENSE file)
- Always add JavaDoc comments when changing an EJB remote interface (class in j-lawyer-server-api, ending with "Remote" in the class name). Always use english for JavaDoc comments.
- Never invoke a build after a code change, this will be done manually.
- When making changes to UI classes that have a corresponding .form file, make sure to consistently update the .form file - I want to continue to use Netbeans GUI builder for the impacted classes.