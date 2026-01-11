# Tasks: Implement XJustiz Dunning Export

## Implementation Tasks

Tasks are ordered to deliver value incrementally and enable early testing.

---

### 1. Create XJustiz JAXB module with Ant

**Description:** Set up new Ant module to generate Java classes from XJustiz schemas using xjc (JAXB compiler).

**Deliverables:**
- New Ant module: `j-lawyer-xjustiz/`
- Module structure:
  - `xsd/` - XJustiz XSD schema files (copied from j-lawyer-server-common/xjustiz/)
  - `src/generated/` - JAXB-generated Java classes (git-ignored)
  - `src/java/` - Utility classes for XML operations
  - `build/` - Compiled classes
  - `dist/j-lawyer-xjustiz.jar` - Output JAR
- `build.xml` with xjc task:
  ```xml
  <target name="generate-jaxb">
      <taskdef name="xjc" classname="com.sun.tools.xjc.XJCTask"/>
      <xjc destdir="src/generated"
           schema="xsd/xjustiz_0600_mahn_3_3.xsd"
           package="org.xjustiz.mahn.v3">
          <arg value="-extension"/>
      </xjc>
  </target>
  <target name="compile" depends="generate-jaxb">
      <javac srcdir="src/generated:src/java"
             destdir="build/classes"
             source="11" target="11"/>
  </target>
  <target name="jar" depends="compile">
      <jar destfile="dist/j-lawyer-xjustiz.jar"
           basedir="build/classes"/>
  </target>
  ```
- `.gitignore` entry for `src/generated/`
- Unit tests verifying JAXB marshalling/unmarshalling
- Integration into main build scripts (build.sh, build-fast.sh)

**Dependencies:** None (xjc included with JDK 8-10, or requires separate JAXB library for JDK 11+)

**Validation:**
- `ant -buildfile j-lawyer-xjustiz/build.xml default` succeeds
- Generated classes compile without errors
- `j-lawyer-xjustiz.jar` is created
- Can marshal simple XJustiz object to XML
- Can unmarshal sample XJustiz XML to objects
- JAR usable from other Ant modules

**Notes:**
- For JDK 11+, may need to include JAXB dependencies in classpath
- Consider checking in minimal generated code for CI/build servers
- XSD schema files should be bundled in module or copied from j-lawyer-server-common

**Estimated effort:** 5 hours

---

### 2. Extend AppUserBean with lawyer identification number

**Description:** Add new field to AppUserBean for storing lawyer identification number (Kennziffer).

**Deliverables:**
- Add `lawyerIdentificationNumber` field to AppUserBean:
  ```java
  @Column(name = "lawyer_identification_number", length = 50)
  private String lawyerIdentificationNumber;
  ```
- Add getter and setter methods
- Update entity serialVersionUID
- Database migration SQL script:
  ```sql
  ALTER TABLE users ADD COLUMN lawyer_identification_number VARCHAR(50);
  ```

**Location:** `j-lawyer-server-entities/src/java/com/jdimension/jlawyer/persistence/AppUserBean.java`

**Dependencies:** None

**Validation:**
- Field is persisted correctly
- Can store and retrieve identification number
- Null values allowed (optional field)
- Migration runs successfully on existing database
- No data loss during migration

**Estimated effort:** 2 hours

---

### 3. Add lawyer identification field to user management UI

**Description:** Extend user management dialog to allow editing lawyer identification number.

**Deliverables:**
- Add "Kennziffer Prozessbevollmächtigter" text field to user edit dialog
- Field visibility logic:
  - Show field only when "Anwalt" checkbox is checked
  - Hide/disable when lawyerAttribute = false
- Field validation:
  - Max length 50 characters
  - Alphanumeric input accepted
- Update `.form` file for NetBeans GUI Builder compatibility
- Save/load identification number with user profile

**Location:** `j-lawyer-client/src/com/jdimension/jlawyer/client/settings/` (user management dialog)

**Dependencies:** Task 2 (AppUserBean extension)

**Validation:**
- Field appears in user edit dialog for lawyers
- Field is hidden for non-lawyers
- Value saves correctly
- Value loads and displays correctly
- NetBeans GUI Builder can edit form
- No regressions in existing user management functionality

**Estimated effort:** 3 hours

---

### 4. Create XJustiz export configuration model

**Description:** Define POJOs for export configuration and mapping utilities.

**Deliverables:**
- `XJustizDunningConfig` class with fields:
  - `AddressBean debtor` (selected case participant)
  - `String courtCode` (selected court)
  - `AppUserBean lawyerRepresentative` (selected lawyer user)
  - `Address serviceAddress` (pre-filled, editable)
  - `String notes` (optional)
- `ValidationResult` class (success flag, error messages)
- Configuration serialization for user preferences
- Validation methods to check required fields are present

**Location:** `j-lawyer-server-entities/src/com/jdimension/jlawyer/xjustiz/`

**Dependencies:** None (uses existing AddressBean and AppUserBean)

**Validation:**
- Configuration objects serialize/deserialize correctly
- Required field validation works (debtor, court, lawyer not null)
- Can store/retrieve configuration from user preferences
- References to AddressBean and AppUserBean work correctly

**Estimated effort:** 3 hours

---

### 5. Implement ClaimLedger to XJustiz mapper

**Description:** Core mapping logic from ClaimLedger domain model to XJustiz JAXB objects.

**Deliverables:**
- `XJustizDunningMapper` class with mapping methods
- Maps ClaimComponent types to XJustiz forderung elements
- Aggregates ClaimLedgerEntries to calculate totals
- Maps ArchiveFileBean contact to schuldner element
- Generates message header with metadata

**Location:** `j-lawyer-server-common/src/com/jdimension/jlawyer/xjustiz/`

**Dependencies:** Task 1 (JAXB classes), Task 4 (config model)

**Validation:**
- Unit tests with mock ClaimLedger data
- All ClaimComponent types map correctly
- Payment calculations are accurate
- Interest calculations follow XJustiz format

**Estimated effort:** 8 hours

---

### 6. Implement XML validation utility

**Description:** Validate generated XJustiz XML against XSD schema.

**Deliverables:**
- `XJustizValidator` class
- Schema loading from `j-lawyer-server-common/xjustiz/`
- Validation with detailed error reporting
- Schema cache to avoid repeated parsing

**Location:** `j-lawyer-server-common/src/com/jdimension/jlawyer/xjustiz/`

**Dependencies:** Task 1 (JAXB classes)

**Validation:**
- Valid XJustiz XML passes validation
- Invalid XML fails with specific error messages
- Schema files load correctly
- Performance: validates 100KB XML in <100ms

**Estimated effort:** 3 hours

---

### 7. Implement ClaimLedger pre-export validation

**Description:** Validate ClaimLedger completeness before attempting export.

**Deliverables:**
- `ClaimLedgerValidator` class
- Checks debtor address completeness
- Verifies at least one Hauptforderung component exists
- Validates amounts are non-zero
- Validates selected lawyer has lawyerIdentificationNumber
- Returns user-friendly error messages

**Location:** `j-lawyer-server-common/src/com/jdimension/jlawyer/xjustiz/`

**Dependencies:** Task 2 (AppUserBean with lawyer ID field)

**Validation:**
- Unit tests for each validation rule
- German error messages are clear
- Provides actionable guidance
- Lawyer ID validation works correctly

**Estimated effort:** 3 hours

---

### 8. Add export methods to ArchiveFileService EJB

**Description:** Extend ArchiveFileService with XJustiz export methods.

**Deliverables:**
- New methods in `ArchiveFileServiceLocal` and `ArchiveFileServiceRemote`:
  - `exportClaimLedgerAsXJustizDunning(String ledgerId, XJustizDunningConfig config)`
  - `validateClaimLedgerForXJustizExport(String ledgerId)`
- Implementation in `ArchiveFileService` EJB
- JavaDoc comments (English) on Remote interface
- Transaction management
- Permission checks (same as ClaimLedger view)

**Location:** `j-lawyer-server-api/` and `j-lawyer-server/j-lawyer-server-ejb/`

**Dependencies:** Tasks 4-7 (mapper, validator, config)

**Validation:**
- EJB deploys successfully
- Methods callable via JNDI lookup
- Transactions commit/rollback correctly
- Permission checks enforce access control
- Integration test with real database

**Estimated effort:** 4 hours

---

### 9. Implement REST API endpoint

**Description:** Create REST endpoint for XJustiz export accessible to mobile/external clients.

**Deliverables:**
- `DunningEndpointV7` class
- `POST /v7/dunning/export/xjustiz` endpoint
- `GET /v7/dunning/validate/{ledgerId}` endpoint
- Request/response POJOs (`RestfulXJustizExportRequestV7`, etc.)
- Swagger annotations

**Location:** `j-lawyer-server/j-lawyer-io/src/org/jlawyer/io/rest/v7/`

**Dependencies:** Task 8 (EJB service methods)

**Validation:**
- Swagger UI documents endpoints correctly
- Authentication required (HTTP Basic Auth)
- Returns XML with correct Content-Type header
- Error responses include helpful messages
- curl/Postman test requests succeed

**Estimated effort:** 3 hours

---

### 10. Create export configuration dialog UI

**Description:** Swing dialog for collecting export configuration from user with dynamic dropdowns.

**Deliverables:**
- `XJustizExportConfigDialog` class and `.form` file
- **Schuldner dropdown** (JComboBox):
  - Populated from case participants via ArchiveFileService.getInvolvedParties()
  - Display format: "Name (Role)" e.g., "Schmidt GmbH (Gegner)"
  - Pre-select first entry if only one exists
  - Sort alphabetically by name
  - Handle empty list with placeholder and warning
- **Gericht dropdown** (JComboBox):
  - Pre-populated with common German courts (AG München, LG München, etc.)
  - Free-text override option
- **Prozessbevollmächtigter dropdown** (JComboBox):
  - Populated from SecurityService.getAllUsers() filtered by lawyerAttribute = true
  - Display format: "FirstName LastName (KennzifferValue)" or "FirstName LastName (Kennziffer fehlt)"
  - Pre-select current user if they have lawyerAttribute = true
  - Sort alphabetically
  - Visually mark entries with missing lawyerIdentificationNumber
  - Handle empty list with placeholder and warning
  - Validate selected lawyer has identification number before export
- Service address fields (pre-filled, editable)
- Optional notes field
- "Als Vorlage speichern" checkbox
- Input validation with real-time feedback
- "Exportieren" button (disabled until all required fields filled)
- "Beteiligte bearbeiten" button (shown when no participants available)

**Location:** `j-lawyer-client/src/com/jdimension/jlawyer/client/editors/files/`

**Dependencies:** Task 4 (config model), Task 8 (EJB methods for data access)

**Validation:**
- Dialog displays correctly with all dropdowns
- Schuldner dropdown populates from case participants correctly
- Prozessbevollmächtigter dropdown shows only lawyer users
- Pre-selection logic works (first entry, current user)
- Empty dropdown scenarios handled gracefully
- Input validation prevents invalid submissions
- All required fields validated before enabling export
- Configuration saves to preferences
- NetBeans GUI Builder can edit `.form` file

**Estimated effort:** 7 hours

---

### 11. Integrate export button into ClaimLedgerDialog

**Description:** Add XJustiz export functionality to existing ClaimLedger UI.

**Deliverables:**
- New "XJustiz Mahnantrag exportieren" button in `ClaimLedgerDialog`
- Click handler:
  - Runs pre-export validation
  - Shows validation errors if any
  - Opens `XJustizExportConfigDialog` if valid
  - Calls EJB export method with configuration
  - Saves generated XML as ArchiveFileDocumentsBean
  - Offers to save XML file to disk (file chooser)
  - Displays success/error message
- Update `.form` file consistently

**Location:** `j-lawyer-client/src/com/jdimension/jlawyer/client/editors/files/`

**Dependencies:** Tasks 8 (EJB), 10 (config dialog)

**Validation:**
- Button appears in ClaimLedgerDialog
- Validation feedback is clear
- Export workflow completes end-to-end
- XML file saves to disk at user-selected location
- Document appears in case document list
- NetBeans GUI Builder maintains `.form` compatibility

**Estimated effort:** 4 hours

---

### 12. Add document tag for XJustiz exports

**Description:** Ensure exported files are tagged for easy filtering.

**Deliverables:**
- Document tag "XJustiz Mahnantrag" auto-applied to exports
- Tag visible in document list
- Users can filter document list by this tag

**Location:** Client and server document handling code

**Dependencies:** Task 9 (integration)

**Validation:**
- Tag appears on exported documents
- Filter by tag shows only XJustiz exports
- Tag persists across client restarts

**Estimated effort:** 1 hour

---

### 13. Implement EDA file parser

**Description:** Create parser to extract key fields from XJustiz EDA XML for formatted display.

**Deliverables:**
- `XJustizEdaParser` class
- Parse EDA XML using JAXB (reuse generated classes)
- Extract header: document type, creation date, message ID
- Extract parties: claimant and debtor with addresses
- Extract court information and case reference
- Extract all claim components with amounts
- Calculate totals
- Handle parsing errors gracefully

**Location:** `j-lawyer-server-common/src/com/jdimension/jlawyer/xjustiz/`

**Dependencies:** Task 1 (JAXB classes)

**Validation:**
- Unit tests with sample EDA files
- Correctly extracts all required fields
- Handles malformed XML without crashing
- Returns user-friendly error messages
- Performance: parses typical EDA file in <500ms

**Estimated effort:** 4 hours

---

### 14. Create formatted EDA view UI component

**Description:** Build Swing panel to display parsed EDA data in human-readable format.

**Deliverables:**
- `XJustizEdaFormattedView` class
- German labels for all fields
- Currency formatting for amounts
- Structured layout with sections (parties, court, claims)
- Visual separator for totals
- Error display panel for parsing failures
- Responsive layout that handles window resizing

**Location:** `j-lawyer-client/src/com/jdimension/jlawyer/client/editors/files/`

**Dependencies:** Task 11 (parser)

**Validation:**
- Displays all fields correctly
- German currency formatting (5.000,00 EUR)
- Layout is clear and readable
- Error messages display properly
- Works with different window sizes

**Estimated effort:** 5 hours

---

### 15. Create raw XML view UI component

**Description:** Build panel to display raw XML with syntax highlighting.

**Deliverables:**
- `XJustizEdaRawXmlView` class
- Load XML content from file
- Apply XML syntax highlighting
- Scrollable text area
- Enable text selection and copying
- Optional: line numbers
- Optional: search/find functionality

**Location:** `j-lawyer-client/src/com/jdimension/jlawyer/client/editors/files/`

**Dependencies:** None (independent of parser)

**Validation:**
- XML displays correctly
- Syntax highlighting works
- Scrolling is smooth
- Text selection and copy work
- Large files (>100KB) render acceptably

**Estimated effort:** 3 hours

---

### 16. Create EDA viewer panel with tabs

**Description:** Main viewer panel with tabbed interface combining formatted and raw views.

**Deliverables:**
- `XJustizEdaViewerPanel` class
- JTabbedPane with two tabs: "Formatierte Ansicht" and "XML Rohdaten"
- Load document from ArchiveFileDocumentsBean
- Initialize formatted view with parser
- Lazy-load raw XML view on first tab selection
- Remember last active tab per session
- Handle loading errors

**Location:** `j-lawyer-client/src/com/jdimension/jlawyer/client/editors/files/`

**Dependencies:** Tasks 11-13 (parser, formatted view, raw view)

**Validation:**
- Both tabs display correctly
- Tab switching works smoothly
- Lazy loading improves initial load time
- Tab preference persists during session
- Handles file loading errors gracefully

**Estimated effort:** 3 hours

---

### 17. Integrate EDA viewer into ArchiveFilePanel

**Description:** Register EDA viewer with document viewer framework in ArchiveFilePanel.

**Deliverables:**
- Viewer registration for "XJustiz Mahnantrag" tag
- Integration with existing document viewer selection logic
- Priority setting (higher than generic XML viewer)
- Viewer activation when EDA file selected
- Seamless switching between different document types

**Location:** `j-lawyer-client/src/com/jdimension/jlawyer/client/editors/files/`

**Dependencies:** Task 14 (EDA viewer panel)

**Validation:**
- EDA files automatically open in custom viewer
- Non-EDA files use appropriate viewers
- No regressions in existing document viewing
- Viewer integrates visually with ArchiveFilePanel
- Works with all document selection methods (click, keyboard)

**Estimated effort:** 3 hours

---

### 18. Write integration tests

**Description:** End-to-end tests covering full export workflow and viewer functionality.

**Deliverables:**
- `XJustizDunningExportTest` class
- Tests for successful export with various ClaimLedger configurations
- Tests for validation error scenarios
- Tests for XML schema compliance
- Tests with real database and ClaimLedger entities
- `XJustizEdaViewerTest` class
- Tests for EDA parsing with valid and malformed files
- Tests for formatted view rendering
- Tests for viewer integration with ArchiveFilePanel

**Location:** `j-lawyer-server/j-lawyer-server-ejb/test/` and `j-lawyer-client/test/`

**Dependencies:** All implementation tasks complete

**Validation:**
- All tests pass
- Tests cover happy path and error cases
- Generated XML validates against XJustiz schema
- Viewer displays content correctly
- No regressions in existing ClaimLedger or document viewing functionality

**Estimated effort:** 6 hours

---

### 19. Update user documentation

**Description:** Document XJustiz export and viewer features for end users.

**Deliverables:**
- Section in user manual: "XJustiz Mahnverfahren Export"
- Step-by-step export instructions with screenshots
- Configuration field explanations
- Troubleshooting common validation errors
- EDA viewer usage instructions
- Explanation of formatted vs. raw XML views
- German language documentation

**Location:** User manual (location depends on project docs structure)

**Dependencies:** Feature complete

**Validation:**
- Documentation is clear and actionable
- Screenshots match current UI
- Covers all user-facing features including viewer
- Viewer usage is well explained

**Estimated effort:** 4 hours

---

## Testing Milestones

### Milestone 0: User Management Extended (After Task 3)
- AppUserBean extended with lawyerIdentificationNumber
- User management UI allows editing lawyer identification
- Database migration applied successfully

### Milestone 1: Core mapping complete (After Task 7)
- Can map ClaimLedger to XJustiz JAXB objects
- Validation logic works including lawyer ID validation
- Unit tests pass

### Milestone 2: Server-side complete (After Task 9)
- EJB and REST API functional
- Can export via Postman/curl
- Integration tests pass

### Milestone 3: Export UI complete (After Task 12)
- Desktop client export works end-to-end
- Lawyer selection dropdown shows identification numbers
- Files save to case documents and disk
- User feedback is clear

### Milestone 4: Viewer complete (After Task 17)
- EDA files display in custom viewer
- Formatted and raw XML views work
- Viewer integrates with ArchiveFilePanel

### Milestone 5: Production-ready (After Task 19)
- All tests pass
- Documentation complete (including lawyer ID setup)
- Ready for release

---

## Parallelization Opportunities

Tasks that can be worked on in parallel:

- **Track 0 (Foundation):** Tasks 2-3 (user management extension - must be done first)
- **Track 1 (Core logic):** Tasks 1, 4-7 (JAXB, config, mapping and validation)
  - Task 1 (JAXB) can start immediately
  - Tasks 4-7 can start after Track 0 complete
- **Track 2 (Server APIs):** Tasks 8-9 (wait for Track 1)
- **Track 3 (Export UI):** Task 10 (can start after Task 4 and Track 0), then Tasks 11-12 (wait for Track 2)
- **Track 4 (Viewer):** Tasks 13-17 (can start after Task 1 JAXB, parallel to Track 3)
  - Task 13 (parser) requires Task 1
  - Tasks 14-15 (view components) can be parallel
  - Task 16 (panel) requires Tasks 13-15
  - Task 17 (integration) requires Task 16
- **Track 5 (QA):** Task 18 (wait for Tracks 3 & 4)
- **Track 6 (Docs):** Task 19 (can start early, finalize after all complete)

---

## Rollback Plan

If issues arise, rollback is straightforward:

1. Remove "XJustiz Mahnantrag exportieren" button from ClaimLedgerDialog
2. Do not deploy new EJB methods (old client won't call them)
3. Remove REST endpoint from deployment
4. No database rollback needed (uses existing document storage)

Feature can be disabled without affecting existing functionality.
