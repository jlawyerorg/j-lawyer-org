# Design: XJustiz Dunning Export

## Architecture Overview

The XJustiz dunning export feature follows the existing architecture pattern:

```
Desktop Client (Swing UI)
    ↓
EJB Service Layer (ArchiveFileService)
    ↓
XJustiz Export Utility (Server-Common)
    ↓
ClaimLedger Data Model + XJustiz Schema
    ↓
Generated XML File
```

## Component Design

### 1. XJustiz Export Utility (New)

**Location:** `j-lawyer-server-common/src/com/jdimension/jlawyer/xjustiz/`

**Purpose:** Core XML generation logic, reusable across server and client

**Key Classes:**
- `XJustizDunningExporter` - Main export coordinator
- `XJustizDunningMapper` - Maps ClaimLedger model to XJustiz schema
- `XJustizValidator` - Validates generated XML against XSD

**Rationale:** Placing in `server-common` allows reuse from both EJB services and potential future command-line tools.

### 2. EJB Service Extension

**Location:** `j-lawyer-server/j-lawyer-server-ejb/src/com/jdimension/jlawyer/services/`

**Modified:** `ArchiveFileService` (Local and Remote interfaces)

**New Methods:**
```java
/**
 * Exports a ClaimLedger as XJustiz court dunning application XML.
 *
 * @param ledgerId The ID of the ClaimLedger to export
 * @param exportConfig Configuration for export (court, case details, etc.)
 * @return Byte array containing the generated XML
 * @throws Exception if export fails or validation errors occur
 */
byte[] exportClaimLedgerAsXJustizDunning(String ledgerId, XJustizDunningConfig exportConfig) throws Exception;

/**
 * Validates whether a ClaimLedger has sufficient data for XJustiz export.
 *
 * @param ledgerId The ID of the ClaimLedger to validate
 * @return List of validation issues (empty if valid)
 */
List<String> validateClaimLedgerForXJustizExport(String ledgerId);
```

**Rationale:** ArchiveFileService already manages ClaimLedger operations, adding export methods maintains cohesion.

### 3. REST API Endpoint (New)

**Location:** `j-lawyer-server/j-lawyer-io/src/org/jlawyer/io/rest/v7/`

**New Class:** `DunningEndpointV7`

**Endpoints:**
- `POST /v7/dunning/export/xjustiz` - Export ClaimLedger as XJustiz XML
- `GET /v7/dunning/validate/{ledgerId}` - Validate ClaimLedger for export

**Rationale:** Separate endpoint for dunning operations keeps API organized, enables mobile app support.

### 4. Desktop Client UI (Enhanced)

**Location:** `j-lawyer-client/src/com/jdimension/jlawyer/client/editors/files/`

**Modified:** `ClaimLedgerDialog` (and corresponding `.form` file)

**New UI Elements:**
- Export button labeled "XJustiz Mahnantrag exportieren"
- Export configuration dialog for court selection and case details
- Validation feedback panel showing export readiness

**Export Configuration Dialog Fields:**
- **Schuldner (Dropdown)**: Populated from case participants (ArchiveFileBean.getInvolvedParties())
  - Displays: Name and role of each participant
  - Pre-selects first participant if only one exists
  - Required field
- **Gericht (Dropdown)**: Common German courts with free-text override option
  - Pre-populated list: AG München, LG München, etc.
  - Free-text field for unlisted courts
  - Required field
- **Prozessbevollmächtigter (Dropdown)**: Populated from system users with attribute "Anwalt"
  - Query: AppUserBean where lawyerAttribute = true
  - Displays: Full name of lawyer
  - Pre-selects current user if they have "Anwalt" attribute
  - Required field
- **Zustellungsadresse**: Pre-filled from firm settings, editable
- **Notizen** (Optional): Free-text field for additional notes

**Data Sources:**
- Case participants: Retrieved via ArchiveFileService.getInvolvedParties()
- Lawyers: Retrieved via SecurityService.getAllUsers() filtered by lawyerAttribute = true
- Firm settings: Retrieved from application configuration

**XJustizDunningConfig Structure:**
```java
class XJustizDunningConfig {
    AddressBean debtor;              // Selected from case participants
    String courtCode;                 // Selected from dropdown or free-text
    AppUserBean lawyerRepresentative; // Selected from lawyer users
    Address serviceAddress;           // Pre-filled, editable
    String notes;                     // Optional
}
```

**Rationale:**
- Using case participants ensures Schuldner is already in the system with complete address
- Lawyer selection from users ensures proper representative attribution
- Pre-filling reduces user effort for common scenarios
- AddressBean reference provides full participant information including role

### 5. EDA File Viewer Component (New)

**Location:** `j-lawyer-client/src/com/jdimension/jlawyer/client/editors/files/`

**Purpose:** Display XJustiz EDA files in human-readable format within the ArchiveFilePanel document viewer

**Key Classes:**
- `XJustizEdaViewerPanel` - Main viewer panel with tabbed interface
- `XJustizEdaFormattedView` - Formatted, human-readable display of EDA content
- `XJustizEdaRawXmlView` - Syntax-highlighted raw XML display
- `XJustizEdaParser` - Parses EDA XML and extracts key fields for display

**UI Design:**
```
+------------------------------------------+
| [Formatierte Ansicht] [XML Rohdaten]    |
+------------------------------------------+
| Mahnantrag                               |
| ---------------------------------------- |
| Gläubiger:     [Name, Adresse]          |
| Schuldner:     [Name, Adresse]          |
| Gericht:       [Gericht]                 |
| Aktenzeichen:  [AZ]                      |
|                                          |
| Forderungen:                             |
|   Hauptforderung:  5.000,00 EUR         |
|   Zinsen:            150,00 EUR         |
|   Kosten:            550,00 EUR         |
|   --------------------------------       |
|   Gesamt:          5.700,00 EUR         |
|                                          |
| Erstellt am: [Datum]                     |
+------------------------------------------+
```

**Integration:**
- Registers as viewer for files with tag "XJustiz Mahnantrag"
- Implements existing document viewer interface
- Automatically activated when EDA file selected in document list

**Rationale:**
- Users can review generated dunning applications without external tools
- Formatted view improves accessibility for non-technical users
- Raw XML view enables technical verification and troubleshooting

## Data Mapping Strategy

### ClaimLedger → XJustiz Mapping

| ClaimLedger Concept | XJustiz Element | Notes |
|---------------------|-----------------|-------|
| ClaimLedger | `mahnantrag.nachricht` | Root dunning application message |
| ClaimComponent (Hauptforderung) | `forderung.hauptforderung` | Main claim amount |
| ClaimComponent (Kosten) | `forderung.kosten` | Costs |
| ClaimComponent (Zinsen) | `forderung.zinsen` | Interest |
| ClaimLedgerEntry (CLAIM) | Contributes to component totals | Aggregated |
| ClaimLedgerEntry (PAYMENT) | Reduces outstanding amount | Calculated balance |
| **Selected case participant** | `schuldner` | Debtor from dropdown selection |
| Current user/firm | `glaeubiger` | Claimant information |
| **Selected lawyer user** | `vertreter` | Legal representative from dropdown |
| ArchiveFileBean.fileNumber | `aktenzeichen` | Case reference |
| Configuration court | `gericht` | Court from dropdown or free-text |

### Required Configuration Fields

Not all required XJustiz fields exist in ClaimLedger, so export requires configuration:

- **Schuldner** (`schuldner`) - Selected from case participants dropdown
  - Source: ArchiveFileBean.getInvolvedParties()
  - Includes complete address information
  - Role displayed for identification (e.g., "Gegner", "Beklagter")
- **Court** (`gericht`) - Selected from dropdown or entered as free-text
  - Dropdown: Common German courts (AG München, LG München, etc.)
  - Free-text: For unlisted courts
- **Prozessbevollmächtigter** (`vertreter`) - Selected from lawyer users dropdown
  - Source: SecurityService.getAllUsers() filtered by lawyerAttribute = true
  - Displays full name
  - Pre-selects current user if they are a lawyer
  - Includes lawyer identification number (kennziffer) from AppUserBean
- **Document type** (`nachrichtentyp`) - Defaults to "Mahnantrag" (not configurable)
- **Service address** (`zustellungsadresse`) - Pre-filled from firm settings, editable

## User Management Extension

### AppUserBean Extension

To support the lawyer identification number (Kennziffer), the AppUserBean entity must be extended:

**New Field:**
```java
@Column(name = "lawyer_identification_number", length = 50)
private String lawyerIdentificationNumber;
```

**Purpose:**
- Stores the official identification number for lawyers (e.g., Anwaltsnummer, Kammernummer)
- Required by XJustiz schema for legal representative identification
- Used in dunning application XML as part of `<vertreter>` element

**Storage:**
- Database: New column in `users` table
- Optional field (can be null for non-lawyer users)
- Visible and editable in user management UI when lawyerAttribute = true

**Validation:**
- Max length: 50 characters
- No specific format validation (different courts/chambers may have different formats)
- Export validation: Must be present for selected lawyer user, otherwise export is blocked

**User Management UI Changes:**
- Add "Kennziffer Prozessbevollmächtigter" text field
- Only visible/enabled when "Anwalt" checkbox is checked
- Displayed in user edit dialog
- Saved with user profile

## Schema Generation Approach

The project is primarily Ant-based, so JAXB code generation must integrate with Ant build system.

### Option 1: Ant with xjc Task (Recommended)

Generate Java classes using Ant's `xjc` task (part of JAXB, included with JDK):

**Module Structure:**
- Create new Ant module: `j-lawyer-xjustiz/`
- Contains XSD schemas (from `j-lawyer-server-common/xjustiz/`)
- Generated Java classes in `src/generated/`
- Additional utility classes in `src/java/`

**Ant Build Script:**
```xml
<target name="generate-jaxb" description="Generate JAXB classes from XJustiz XSD">
    <taskdef name="xjc" classname="com.sun.tools.xjc.XJCTask">
        <classpath>
            <fileset dir="${jaxb.home}/lib" includes="*.jar"/>
        </classpath>
    </taskdef>

    <xjc destdir="src/generated"
         schema="xsd/xjustiz_0600_mahn_3_3.xsd"
         package="org.xjustiz.mahn.v3">
        <arg value="-extension"/>
        <produces dir="src/generated" includes="**/*.java"/>
    </xjc>

    <javac srcdir="src/generated" destdir="build/classes"
           includeantruntime="false" source="11" target="11"/>
</target>

<target name="default" depends="generate-jaxb, compile, jar"/>
```

**Integration:**
- Built as JAR: `j-lawyer-xjustiz.jar`
- Included in `j-lawyer-server-common` classpath
- Generated once during build, compiled to classes
- Build order: Early in chain (before server-common)

**Pros:**
- Consistent with existing Ant-based architecture
- xjc available in JDK, no additional tools needed
- Type-safe XML generation
- Automatic validation during marshalling

**Cons:**
- Initial build complexity
- Large generated codebase
- Must regenerate if XSD schema changes

### Option 2: Maven Module (Alternative)

Create Maven module similar to `j-lawyer-invoicing`:

**Precedent in Project:**
- `j-lawyer-invoicing` (Maven) - ZUGFeRD/XRechnung generation
- `j-lawyer-cloud` (Maven) - Nextcloud integration
- `j-lawyer-backupmgr` (Maven) - JavaFX application

**Approach:**
```xml
<plugin>
    <groupId>org.jvnet.jaxb2.maven2</groupId>
    <artifactId>maven-jaxb2-plugin</artifactId>
    <executions>
        <execution>
            <goals><goal>generate</goal></goals>
        </execution>
    </executions>
</plugin>
```

**Integration:**
- `mvn install` produces `j-lawyer-xjustiz.jar`
- Ant modules depend on Maven artifact
- Hybrid build: Maven first, then Ant

**Pros:**
- Maven plugins handle JAXB generation well
- Follows precedent of other Maven modules in project

**Cons:**
- Introduces build tool inconsistency in core module
- Requires Maven in build chain
- More complex for developers unfamiliar with Maven

### Option 3: Generate Once, Commit to Repo

Generate JAXB classes once, commit them to repository:

**Approach:**
- Run xjc manually or via script once
- Commit generated classes to `j-lawyer-server-common/src/`
- No build-time generation

**Pros:**
- Simplest build process
- No tool dependencies
- Faster builds

**Cons:**
- Less maintainable (hard to update if schema changes)
- Large diffs in version control
- Generated code mixed with hand-written code
- Not recommended for evolving schemas

### Decision: Option 1 (Ant with xjc)

**Rationale:**
1. **Consistency:** Matches existing Ant-based architecture
2. **Availability:** xjc ships with JDK, no additional dependencies
3. **Maintainability:** Regenerate classes when XSD updates
4. **Isolation:** Separate `j-lawyer-xjustiz` module keeps generated code isolated
5. **Type Safety:** Full JAXB benefits (marshalling, validation, IDE support)

**Implementation:**
- Create `j-lawyer-xjustiz/` as new Ant module
- Bundle XSD schemas in `j-lawyer-xjustiz/xsd/` directory
- Generate classes during `ant default` target
- Produce `j-lawyer-xjustiz.jar` for use by other modules

**Build Order Integration:**

Update build scripts to include `j-lawyer-xjustiz` early in the chain:

```bash
# build.sh / build-fast.sh
# Current order:
# 1. j-lawyer-cloud (Maven)
# 2. j-lawyer-fax (Ant)
# 3. j-lawyer-server-common (Ant)
# ...

# New order:
# 1. j-lawyer-cloud (Maven)
# 2. j-lawyer-xjustiz (Ant) ← NEW
# 3. j-lawyer-fax (Ant)
# 4. j-lawyer-server-common (Ant) - add j-lawyer-xjustiz.jar to classpath
# ...
```

**Classpath Configuration:**

Modules using XJustiz (j-lawyer-server-common, j-lawyer-server-ejb) must include:
```xml
<path id="classpath">
    <pathelement location="../j-lawyer-xjustiz/dist/j-lawyer-xjustiz.jar"/>
    <!-- other dependencies -->
</path>
```

## File Storage and Retrieval

Generated XML files will be:
1. Saved as ArchiveFileDocumentsBean attached to the case
2. Marked with special document tag "XJustiz Mahnantrag"
3. Stored in standard VirtualFile system (local/Samba/SFTP)

This enables:
- Audit trail of generated dunning applications
- Re-download of previously generated files
- Integration with existing document management

## Validation Strategy

Three-level validation:

1. **Pre-export validation** - Check ClaimLedger completeness
   - Debtor address exists and is complete
   - At least one claim component with non-zero amount
   - All required configuration fields provided
   - Selected lawyer user has lawyerIdentificationNumber set
   - Lawyer identification number is not empty

2. **Schema validation** - XSD validation during generation
   - JAXB validation during marshalling
   - Fallback XSD validation if JAXB is not used

3. **Post-export validation** - Business rule checks
   - Total amounts match ClaimLedger calculations
   - All mandatory XJustiz elements present
   - Court code valid for jurisdiction

## Error Handling

Export errors categorized as:

- **Configuration errors** - Missing required fields (user-actionable)
- **Data errors** - Invalid ClaimLedger state (fix data first)
- **Technical errors** - XML generation/validation failures (log details)

Each category provides specific user guidance for resolution.

## Testing Strategy

1. **Unit tests** - Test mapping logic with mock ClaimLedger data
2. **Integration tests** - Test full export with real ClaimLedger entities
3. **Schema validation tests** - Verify generated XML passes XSD validation
4. **Court acceptance tests** - Manual submission to test court systems
5. **Viewer tests** - Test EDA parsing and display rendering
   - Parse valid EDA files correctly
   - Handle malformed XML gracefully
   - Formatted view displays all key fields
   - Tab switching works correctly
   - Integration with document viewer framework

## EDA Viewer Integration

### Document Viewer Architecture

The existing ArchiveFilePanel uses a plugin-based approach for document viewing:
- Each document type can register a custom viewer
- Viewers implement a common interface for integration
- Viewer selection based on file extension and/or document tags

### EDA Viewer Registration

The XJustizEdaViewerPanel registers as viewer for:
- **File extensions:** `.xml` (when tagged as XJustiz)
- **Document tags:** "XJustiz Mahnantrag"
- **Priority:** Higher than generic XML viewer

### Formatted View Implementation

**Display Strategy:**
1. Parse XML using JAXB (reuse generated classes from export)
2. Extract key fields: parties, amounts, court details
3. Format using Swing labels and panels with clear layout
4. Use German labels and currency formatting

**Fields to Display:**
- **Header:** Document type, creation date, message ID
- **Parties:** Claimant (Gläubiger) and Debtor (Schuldner) with addresses
- **Court:** Court name and case reference
- **Claims:** Itemized list of Hauptforderung, Zinsen, Kosten with totals
- **Interest:** Interest rate and calculation period if present
- **Representative:** Claimant's legal representative information

### Raw XML View Implementation

**Display Strategy:**
1. Load XML content as string
2. Apply syntax highlighting using existing XML highlighting library
3. Display in scrollable JTextArea or JEditorPane
4. Enable text selection and copy functionality
5. Line numbers for easier reference

**Optional Features:**
- XML pretty-printing for readability
- Search/find within XML
- Export raw XML to clipboard

### Tab Switching

**Implementation:**
- JTabbedPane with two tabs: "Formatierte Ansicht" and "XML Rohdaten"
- Lazy loading: Raw XML tab loads content only when first selected
- Tab state persists during session (remember last viewed tab)

### Error Handling

If EDA file is malformed:
- Show error message in formatted view
- Fall back to raw XML view to allow inspection
- Log parsing errors for debugging

## Performance Considerations

- Export is synchronous and expected to complete in <2 seconds
- XML generation is CPU-bound, minimal I/O
- No caching needed (export is infrequent operation)
- File size expected <100KB per dunning application
- EDA viewer parsing: <500ms for typical file
- Formatted view rendering: instant after parsing
- Raw XML view: lazy-loaded on first tab selection

## Security Considerations

- Export requires same permissions as ClaimLedger view access
- Generated XML contains sensitive financial data - standard file permissions apply
- No PII beyond what's already in case data
- REST API endpoint protected by HTTP Basic Auth

## Future Extensions

While out of scope for this change, the architecture supports:

- Additional XJustiz schema types (enforcement, insolvency)
- Batch export of multiple ClaimLedgers
- Template-based customization of export fields
- Direct beA submission integration
