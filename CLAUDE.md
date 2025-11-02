# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

j-lawyer.org is a legal practice management system built on Java EE architecture. It consists of a rich desktop client (Swing), an application server (WildFly/JBoss), and a MySQL database. The system includes both traditional EJB remote interfaces and modern REST APIs for integration.

## Build Commands

### Full Build
```bash
./build.sh
```
Builds all modules in dependency order with tests. Requires:
- Java 11+ (for most modules)
- Java 8 (for j-lawyer-backupmgr)
- openjfx package
- Sipgate credentials (optional, for j-lawyer-fax tests)
- FTP/SFTP credentials (optional, for j-lawyer-server-common tests)

### Fast Build (Skip Tests)
```bash
./build-fast.sh
```
Builds all modules without running tests. Uses specific Java versions configured in the script.

### Clean Build
```bash
./clean.sh
```
Cleans all build artifacts from all modules.

### Individual Module Build
```bash
# Server modules (require -Dj2ee.server.home=/path/to/jboss)
ant -Dj2ee.server.home=/home/travis -buildfile j-lawyer-server-entities/build.xml default
ant -buildfile j-lawyer-server-api/build.xml default
ant -Dj2ee.server.home=/home/travis -buildfile j-lawyer-server/build.xml default test
ant -buildfile j-lawyer-server-common/build.xml default

# Client and common modules
ant -buildfile j-lawyer-client/build.xml default
ant -buildfile j-lawyer-io-common/build.xml default
ant -buildfile j-lawyer-fax/build.xml default

# Maven modules
mvn -f j-lawyer-cloud/pom.xml install
mvn -f j-lawyer-backupmgr/pom.xml clean package test
mvn -f j-lawyer-invoicing/pom.xml install
```

### Running Tests
```bash
# Server EJB tests
ant -Dj2ee.server.home=/home/travis -buildfile j-lawyer-server/build.xml test

# Individual module tests
ant -buildfile j-lawyer-fax/build.xml default
ant -buildfile j-lawyer-server-common/build.xml default
```

Tests are located in `test/` directories within each module. Test classes follow the pattern `*Test.java`.

### Deployment
```bash
./deploy.sh  # Copies j-lawyer-server.ear to WildFly deployments directory
```

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
- **Apache Lucene 4.7.0**: Full-text search
- **JAX-RS (RESTEasy)**: REST API implementation
- **Java Swing**: Desktop UI framework
- **FlatLaf**: Modern look and feel
- **Build Tools**: Apache Ant (primary), Maven (select modules)

### Build Order

Modules must be built in this order due to dependencies:
1. j-lawyer-cloud (Maven)
2. j-lawyer-fax (Ant)
3. j-lawyer-server-common (Ant)
4. j-lawyer-server-entities (Ant)
5. j-lawyer-server-api (Ant)
6. j-lawyer-server (Ant) - produces j-lawyer-server.ear
7. j-lawyer-io-common (Ant)
8. j-lawyer-client (Ant)
9. j-lawyer-backupmgr (Maven)

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

This codebase is developed using NetBeans IDE. Each module contains:
- `build.xml`: Custom Ant build file
- `nbproject/`: NetBeans project configuration
- `nbproject/build-impl.xml`: Generated build implementation (imported by build.xml)

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