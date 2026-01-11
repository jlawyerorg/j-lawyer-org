# Project Context

## Purpose
j-lawyer.org is a comprehensive legal practice management system designed for law firms and legal professionals. The system provides:
- Case/matter management (Archivefile management)
- Document management with versioning
- Contact/address management
- Calendar and appointment scheduling
- Time tracking and invoicing
- Integration with VoIP/fax services
- Cloud storage integration (Nextcloud/ownCloud)
- Mobile app support via REST API

## Tech Stack

### Core Technologies
- **Java EE/Jakarta EE**: Enterprise application framework
- **WildFly (JBoss)**: Application server
- **MySQL**: Relational database (accessed via JNDI: `java:/jlawyerdb`)
- **Hibernate/JPA 2.1**: ORM layer (persistence unit: `j-lawyer-server-ejbPU`)

### Backend
- **EJB 3.x**: Business logic layer (Stateless and Singleton session beans)
- **JAX-RS (RESTEasy)**: REST API implementation
- **Apache Lucene 4.7.0**: Full-text search engine
- **JBoss Remoting**: Client-server communication protocol

### Frontend
- **Java Swing**: Rich desktop client UI framework
- **FlatLaf**: Modern look and feel library
- **JavaFX**: Backup manager application

### Build Tools
- **Apache Ant**: Primary build system for most modules
- **Maven**: Build system for j-lawyer-cloud, j-lawyer-backupmgr, j-lawyer-invoicing

### Specialized Libraries
- **Mustang Project**: ZUGFeRD/XRechnung invoice generation (German e-invoicing standards)
- **Sipgate API**: VoIP/fax integration
- **WebDAV/CalDAV/CardDAV**: Cloud integration protocols

## Project Conventions

### Code Style
- **JavaDoc**: Always add JavaDoc comments when changing EJB remote interfaces (classes in j-lawyer-server-api ending with "Remote"). Use English for JavaDoc comments.
- **Package Naming**:
  - Server EJBs: `com.jdimension.jlawyer.server.ejb`
  - Server entities: `com.jdimension.jlawyer.persistence`
  - REST API: `org.jlawyer.io.rest.v{N}` (versioned)
  - Client UI: `com.jdimension.jlawyer.client.*`
  - Common utilities: `com.jdimension.jlawyer.services.*`
- **Test Classes**: End with `Test.java`
- **Form Files**: When modifying UI classes with corresponding `.form` files, consistently update the `.form` file to maintain NetBeans GUI Builder compatibility

### Architecture Patterns

#### Service Interface Pattern
Each EJB service implements two interfaces:
- `ServiceNameRemote`: For client access (defined in j-lawyer-server-api)
- `ServiceNameLocal`: For server-side inter-bean calls (defined in j-lawyer-server-ejb)

#### Key Services
- `ArchiveFileService`: Case/matter management
- `AddressService`: Contact management
- `CalendarService`: Scheduling and appointments
- `SecurityService`: Authentication and authorization
- `SearchService`: Full-text search
- `SingletonService`: Application lifecycle
- `ScheduledTasksService`: Background tasks

#### Client-Server Communication
- **Desktop Client → Server**: EJB remote invocation via JBoss Remoting
  - JNDI lookup pattern: `ejb:j-lawyer-server/j-lawyer-server-ejb//ServiceName!interface`
  - Service locator: `JLawyerServiceLocator` class
- **External Systems → Server**: REST API at `/j-lawyer-io/rest/v{1-7}/...`
  - HTTP Basic Authentication
  - JSON responses

#### REST API Versioning
- New endpoints go in the latest version (currently v7)
- Never break existing API versions
- Create new version directory if breaking changes are needed
- All endpoints are `@Stateless` EJBs using local EJB services via JNDI lookup
- Swagger UI available at `/j-lawyer-io/swagger-ui/`

#### Persistence Strategy
- **Transaction Type**: JTA (container-managed transactions)
- **Schema Strategy**: Validate only - DDL changes via manual SQL scripts, not auto-generated
- **Core Domain Entities**: `ArchiveFileBean` (cases), `AddressBean` (contacts), `ArchiveFileDocumentsBean`, `AppUserBean`

#### File Storage
Pluggable backends via `VirtualFile` abstraction:
- Local file system (default: `/opt/jboss/j-lawyer-data/`)
- Samba/CIFS shares
- SSH/SFTP remote storage
- FTP remote storage

### Testing Strategy
- **Test Locations**:
  - Ant modules: `test/` directory
  - Maven modules: `src/test/` directory
- **Naming**: Test classes end with `Test.java`
- **EJB Tests**: Located in `j-lawyer-server/j-lawyer-server-ejb/test/`
- **External Dependencies**: Some tests require credentials:
  - Sipgate (for j-lawyer-fax tests)
  - FTP/SFTP (for j-lawyer-server-common tests)
  - Set via environment variables in build scripts
- **Test Execution**:
  - Full tests: `./build.sh`
  - Skip tests: `./build-fast.sh`
  - Individual module: `ant -buildfile module/build.xml test`

### Git Workflow
- **Main Branch**: `master`
- **Build Trigger**: Never invoke a build after a code change - this is done manually
- **Commit Convention**: Standard git commit messages

## Domain Context

### Legal Practice Management Domain
- **Case/Matter (ArchiveFile)**: Central entity representing a legal case or matter
- **Documents**: Versioned files attached to cases, stored in pluggable file system
- **Contacts (Addresses)**: Clients, opposing parties, courts, insurance companies
- **Calendar**: Appointments, hearings, deadlines with reminder functionality
- **Invoicing**: Time tracking, expense tracking, invoice generation with ZUGFeRD support
- **Dictation**: Voice recording integration for legal dictation
- **Forms/Templates**: Document generation from templates with placeholders

### German Legal Context
- ZUGFeRD/XRechnung: German e-invoicing standards
- beA (besonderes elektronisches Anwaltspostfach): German attorney mailbox integration
- German court calendar and holiday system

## Important Constraints

### Build Order Dependencies
Modules must be built in this specific order:
1. j-lawyer-cloud (Maven)
2. j-lawyer-fax (Ant)
3. j-lawyer-server-common (Ant)
4. j-lawyer-server-entities (Ant)
5. j-lawyer-server-api (Ant)
6. j-lawyer-server (Ant) - produces j-lawyer-server.ear
7. j-lawyer-io-common (Ant)
8. j-lawyer-client (Ant)
9. j-lawyer-backupmgr (Maven)

### Java Version Requirements
- **Most modules**: Java 11+
- **j-lawyer-backupmgr**: Java 8
- **Additional**: openjfx package required

### NetBeans IDE Integration
- Developed using NetBeans IDE
- Each module contains `build.xml` and `nbproject/` configuration
- `nbproject/build-impl.xml` is generated by NetBeans (imported by build.xml)
- **Critical**: Maintain `.form` files for GUI Builder compatibility

### Server Configuration
- Server modules require `-Dj2ee.server.home=/path/to/jboss` property
- Persistence unit: `j-lawyer-server-ejbPU`
- Data source: `java:/jlawyerdb`

### API Compatibility
- Never break existing REST API versions
- Maintain backward compatibility for EJB remote interfaces
- Desktop clients may be running older versions

## External Dependencies

### Required Services
- **WildFly/JBoss**: Application server (tested with specific versions)
- **MySQL**: Database server (JNDI name: `java:/jlawyerdb`)

### Optional Integrations
- **Sipgate**: VoIP and fax service integration
- **Nextcloud/ownCloud**: Cloud storage integration via WebDAV/CalDAV/CardDAV
- **Samba/CIFS**: Network file share integration
- **SFTP/FTP**: Remote file storage
- **beA**: German attorney mailbox system

### Development/Testing
- **Docker**: Development environment available via docker-compose
  - Port: 8000
  - Default credentials: admin/a
  - Database: jlawyerdb (user: jlawyer/jlawyer)
  - Volumes: `/var/docker_data/j-lawyer-data/`, `/var/docker_data/j-lawyer-db/`

### Build Dependencies
- Apache Ant
- Maven
- Java 8 and Java 11+ JDKs
- openjfx package
