## ADDED Requirements

### Requirement: Multi-Value Tag Definition
The system SHALL support defining multi-value (dropdown) tags via dynamically named `server_options` option groups. Each multi-value tag is represented as an option group named `{prefix}.tags.mv.{TagName}` where each row in that group stores one selectable value. The tag name is derived from the option group suffix after `*.tags.mv.`. This avoids delimiter/escaping issues since values are stored one per row. Multi-value tag names MUST NOT collide with boolean tag names within the same entity type. The prefixes are `archiveFile` (cases), `document` (documents), and `address` (contacts).

#### Scenario: Define a case multi-value tag
- **WHEN** an administrator creates option group `archiveFile.tags.mv.Verfahrensstand` with rows `Beratung`, `Klage eingereicht`, `Verhandlung`, `Abgeschlossen`
- **THEN** the system SHALL store these as four rows in `server_options`
- **AND** the multi-value tag "Verfahrensstand" SHALL be available for cases with four selectable values

#### Scenario: Define a document multi-value tag
- **WHEN** an administrator creates option group `document.tags.mv.Dokumentstatus` with rows `Entwurf`, `Geprüft`, `Final`
- **THEN** the multi-value tag "Dokumentstatus" SHALL be available for documents with three selectable values

#### Scenario: Define a contact multi-value tag
- **WHEN** an administrator creates option group `address.tags.mv.Mandantenstatus` with rows `Interessent`, `Aktiv`, `Ehemalig`
- **THEN** the multi-value tag "Mandantenstatus" SHALL be available for contacts with three selectable values

#### Scenario: Tag value containing special characters
- **WHEN** an administrator adds a value `Klage, Berufung` (containing a comma) to option group `archiveFile.tags.mv.Verfahrensstand`
- **THEN** the system SHALL store it as a single row with value `Klage, Berufung`
- **AND** the value SHALL appear as one selectable entry in the dropdown

#### Scenario: Reject duplicate tag name across boolean and multi-value
- **WHEN** an administrator attempts to create a multi-value tag with a name that already exists as a boolean tag in the same entity type
- **THEN** the system SHALL reject the definition with a validation error

### Requirement: Multi-Value Tag Assignment for Cases
The system SHALL allow assigning a multi-value tag to a case by storing a row in `case_tags` with `tagName` set to the multi-value tag label and `tag_value` set to the selected dropdown value. Selecting an empty value SHALL remove the tag row. Changing the dropdown value SHALL update the existing row's `tag_value`.

#### Scenario: Assign multi-value tag to case
- **WHEN** a user selects "Klage eingereicht" from the "Verfahrensstand" dropdown on a case
- **THEN** the system SHALL create a row in `case_tags` with `tagName="Verfahrensstand"` and `tag_value="Klage eingereicht"`

#### Scenario: Change multi-value tag value on case
- **WHEN** a user changes the "Verfahrensstand" dropdown from "Klage eingereicht" to "Verhandlung" on a case
- **THEN** the system SHALL update the existing row's `tag_value` to "Verhandlung"

#### Scenario: Clear multi-value tag from case
- **WHEN** a user selects the empty/blank entry from the "Verfahrensstand" dropdown on a case
- **THEN** the system SHALL remove the tag row from `case_tags`

#### Scenario: Boolean tags unaffected
- **WHEN** a user toggles a boolean tag on a case
- **THEN** the tag row SHALL have `tag_value=NULL` as before

### Requirement: Multi-Value Tag Assignment for Documents
The system SHALL allow assigning a multi-value tag to a document by storing a row in `document_tags` with `tagName` and `tag_value`. The same assignment semantics as case tags apply.

#### Scenario: Assign multi-value tag to document
- **WHEN** a user selects "Final" from the "Dokumentstatus" dropdown on a document
- **THEN** the system SHALL create a row in `document_tags` with `tagName="Dokumentstatus"` and `tag_value="Final"`

#### Scenario: Clear multi-value tag from document
- **WHEN** a user selects the empty/blank entry from the "Dokumentstatus" dropdown on a document
- **THEN** the system SHALL remove the tag row from `document_tags`

### Requirement: Multi-Value Tag Assignment for Contacts
The system SHALL allow assigning a multi-value tag to a contact by storing a row in `contact_tags` with `tagName` and `tag_value`. The same assignment semantics as case tags apply.

#### Scenario: Assign multi-value tag to contact
- **WHEN** a user selects "Aktiv" from the "Mandantenstatus" dropdown on a contact
- **THEN** the system SHALL create a row in `contact_tags` with `tagName="Mandantenstatus"` and `tag_value="Aktiv"`

#### Scenario: Clear multi-value tag from contact
- **WHEN** a user selects the empty/blank entry from the "Mandantenstatus" dropdown on a contact
- **THEN** the system SHALL remove the tag row from `contact_tags`

### Requirement: Multi-Value Tag Configuration UI
A new `MultiValueTagConfigurationDialog` SHALL provide a master-detail layout for managing multi-value tags. The existing `OptionGroupConfigurationDialog` SHALL NOT be modified. The left side (master) shows all multi-value tag names for the current entity type with add/rename/delete controls. The right side (detail) shows the values for the selected tag with add/rename/delete controls. Multi-value tag names MUST NOT collide with boolean tag names within the same entity type. The dialog SHALL be accessible via new menu items in the administration menu for case tags, document tags, and address tags.

#### Scenario: Create new multi-value tag and immediately add values
- **WHEN** an administrator clicks "add" on the left side and enters tag name "Priorität"
- **THEN** the system SHALL create an empty option group `archiveFile.tags.mv.Priorität`
- **AND** the tag SHALL appear in the left list and be auto-selected
- **AND** the right side SHALL be enabled so the user can immediately add values

#### Scenario: Add value to selected multi-value tag
- **WHEN** an administrator selects "Priorität" on the left and adds the value "Hoch" on the right
- **THEN** the system SHALL add a row to option group `archiveFile.tags.mv.Priorität` with value "Hoch"

#### Scenario: Rename multi-value tag name with user confirmation
- **WHEN** an administrator renames a multi-value tag from "Status" to "Verfahrensstand" on the left side
- **THEN** the system SHALL prompt "Sollen vorhandene Werte an Akten/Adressen/Dokumenten ebenfalls umbenannt werden?"
- **AND** if the user confirms, the system SHALL update `tagName` in all existing tag rows that reference the old name
- **AND** the option group SHALL be renamed from `*.tags.mv.Status` to `*.tags.mv.Verfahrensstand`

#### Scenario: Delete multi-value tag with user confirmation
- **WHEN** an administrator deletes the multi-value tag "Priorität" on the left side
- **THEN** the system SHALL prompt whether to remove existing tag rows with `tagName="Priorität"`
- **AND** if confirmed, the system SHALL delete those tag rows
- **AND** the option group `*.tags.mv.Priorität` SHALL be removed

#### Scenario: Rename value with user confirmation
- **WHEN** an administrator renames the value "Klage" to "Klage eingereicht" on the right side
- **THEN** the system SHALL prompt whether to update existing assignments
- **AND** if confirmed, the system SHALL update `tag_value` from "Klage" to "Klage eingereicht" in all matching tag rows

#### Scenario: Remove value with user confirmation
- **WHEN** an administrator removes the value "Niedrig" on the right side
- **THEN** the system SHALL prompt whether to remove existing tag rows with `tag_value="Niedrig"`
- **AND** if confirmed, the system SHALL delete those tag rows
- **AND** if declined, the tag rows SHALL remain with the orphaned value

#### Scenario: Add value preserves existing assignments
- **WHEN** an administrator adds a new value "Eilig" to the existing multi-value tag "Priorität"
- **THEN** the system SHALL add a row to option group `archiveFile.tags.mv.Priorität`
- **AND** existing assignments SHALL remain unchanged

#### Scenario: Right side disabled when no tag selected
- **WHEN** no multi-value tag is selected on the left side
- **THEN** the value list and controls on the right side SHALL be disabled

### Requirement: Multi-Value Tag REST API
The shared `RestfulTagV1` POJO (used across REST API v1, v5, and later versions) SHALL include an optional `tagValue` field. All REST endpoints that convert entity beans to `RestfulTagV1` SHALL populate the `tagValue` field. For boolean tags, `tagValue` SHALL be null or absent. For multi-value tags, `tagValue` SHALL contain the selected value string. This applies to `CasesEndpointV1`, `CasesEndpointV5`, `ContactsEndpointV5`, and all v7 tag endpoints.

#### Scenario: GET returns tagValue for multi-value tags
- **WHEN** a client fetches tags for a case via the REST API
- **AND** the case has a multi-value tag "Verfahrensstand" with value "Klage eingereicht"
- **THEN** the response SHALL include `{"tagName": "Verfahrensstand", "tagValue": "Klage eingereicht"}`

#### Scenario: GET returns null tagValue for boolean tags
- **WHEN** a client fetches tags for a case via the REST API
- **AND** the case has a boolean tag "Dringend"
- **THEN** the response SHALL include `{"tagName": "Dringend", "tagValue": null}`

#### Scenario: POST/PUT accepts tagValue
- **WHEN** a client sets a tag via the REST API with `tagName="Verfahrensstand"` and `tagValue="Verhandlung"`
- **THEN** the system SHALL persist the tag with the specified value

#### Scenario: Backward compatibility for requests without tagValue
- **WHEN** a client sets a tag via the REST API without providing `tagValue`
- **THEN** the system SHALL treat it as a boolean tag (`tagValue=NULL`)

### Requirement: Multi-Value Tag Filter in Search Dialogs
All tag filter popup menus in the client SHALL display each multi-value tag value as a flat `JCheckBoxMenuItem` with display text `"TagName: Value"` (e.g. `"Verfahrensstand: Klage eingereicht"`). The tag name and value SHALL be stored as client properties on the menu item (`putClientProperty("tagName", ...)` and `putClientProperty("tagValue", ...)`), so selection extraction reads metadata from properties and never parses the display label. Boolean tags SHALL remain as flat `JCheckBoxMenuItem` entries with only `tagName` set. Users can check none, one, or multiple values of the same multi-value tag. This applies to the following dialogs: `QuickArchiveFileSearchPanel` (case search), `QuickAddressSearchPanel` (contact search), `SearchAndAssignDialog` (document assignment), `ArchiveFilePanel` (document tag filter), `AddBeaRecipientSearchDialog`, `AddRecipientSearchDialog`, `AddAddressSearchDialog`, `MultiAddressSearchDialog`, and `DesktopPanel` tagged items tab.

#### Scenario: Multi-value tag values appear as flat entries in filter popup
- **WHEN** a user opens the tag filter popup in a search dialog
- **AND** a multi-value tag "Verfahrensstand" is defined with values "Beratung", "Klage eingereicht", "Verhandlung", "Abgeschlossen"
- **THEN** the popup SHALL show four flat checkable entries: "Verfahrensstand: Beratung", "Verfahrensstand: Klage eingereicht", "Verfahrensstand: Verhandlung", "Verfahrensstand: Abgeschlossen"

#### Scenario: Tag name and value stored as metadata not parsed from label
- **WHEN** a `JCheckBoxMenuItem` for multi-value tag value "Verfahrensstand: Klage eingereicht" is created
- **THEN** the item SHALL have client property `tagName="Verfahrensstand"` and `tagValue="Klage eingereicht"`
- **AND** `TagUtils.getSelectedTags()` SHALL read these properties to determine the selection

#### Scenario: Filter cases by specific multi-value tag value
- **WHEN** a user checks "Verfahrensstand: Klage eingereicht" in the case search tag filter
- **AND** executes a search
- **THEN** the results SHALL only include cases that have the tag "Verfahrensstand" with `tag_value="Klage eingereicht"`

#### Scenario: Filter by multiple values of same multi-value tag
- **WHEN** a user checks both "Verfahrensstand: Klage eingereicht" and "Verfahrensstand: Verhandlung"
- **AND** executes a search
- **THEN** the results SHALL include cases with "Verfahrensstand" set to either "Klage eingereicht" OR "Verhandlung"

#### Scenario: Boolean tags in filter unaffected
- **WHEN** a user checks a boolean tag "Dringend" in the tag filter popup
- **AND** executes a search
- **THEN** the results SHALL include cases that have the tag "Dringend" (no value filtering)

#### Scenario: Combined boolean and multi-value tag filter
- **WHEN** a user checks the boolean tag "Dringend" AND checks "Verfahrensstand: Klage eingereicht"
- **AND** executes a search
- **THEN** the results SHALL include cases that have the tag "Dringend" OR have "Verfahrensstand" with value "Klage eingereicht"

### Requirement: Desktop Panel Multi-Value Tag Tabs
On the `DesktopPanel` tagged items tabbed pane, selected multi-value tag values SHALL appear as tab labels in the format `"TagName: Value"` (e.g. `"Verfahrensstand: Klage eingereicht"`). Each checked value gets its own tab. Users can display none, one, or multiple values of the same multi-value tag simultaneously. Selected value filters SHALL be persisted in user settings across sessions.

#### Scenario: Desktop tagged items tab shows multi-value tag value
- **WHEN** a user checks "Verfahrensstand: Beratung" in the desktop tagged items tag filter
- **THEN** a tab labeled "Verfahrensstand: Beratung" SHALL appear in the tabbed pane
- **AND** the tab SHALL show only cases with "Verfahrensstand" set to "Beratung"

#### Scenario: Multiple values of same tag as separate tabs
- **WHEN** a user checks both "Verfahrensstand: Beratung" and "Verfahrensstand: Klage eingereicht"
- **THEN** two separate tabs SHALL appear, one for each value

#### Scenario: Desktop tag value filter persisted across sessions
- **WHEN** a user selects multi-value tag value filters on the desktop tagged items tab
- **AND** restarts the client
- **THEN** the previously selected value filters SHALL be restored

### Requirement: Server-Side Tag Value Filtering
The server-side search methods (`searchEnhanced`, `searchTagsEnhanced`, `getTagged`) SHALL support an optional tag-value map parameter alongside the existing tag name array. When values are provided for a multi-value tag, the query SHALL add a `tag_value IN (...)` clause. When no values are specified, the existing tag name matching behavior SHALL be preserved (backward-compatible).

#### Scenario: Search with tag value filter
- **WHEN** a search request includes tag name "Verfahrensstand" with values ["Klage eingereicht", "Verhandlung"]
- **THEN** the server SHALL return only items where `tagName="Verfahrensstand"` AND `tag_value IN ("Klage eingereicht", "Verhandlung")`

#### Scenario: Search with tag name only (no value filter)
- **WHEN** a search request includes tag name "Dringend" without any value filter
- **THEN** the server SHALL return all items where `tagName="Dringend"` regardless of `tag_value`

#### Scenario: Mixed boolean and multi-value tag filter on server
- **WHEN** a search request includes boolean tag "Dringend" (no values) and multi-value tag "Verfahrensstand" with values ["Beratung"]
- **THEN** the server SHALL return items matching either filter (OR logic between tags)

### Requirement: REST API Tag Value Filtering
The REST API v7 `/bytag/{tag}` endpoints SHALL accept an optional `value` query parameter to filter by specific tag value. Omitting the parameter SHALL return all items with that tag regardless of value (backward-compatible).

#### Scenario: GET cases by tag with value filter
- **WHEN** a client requests `GET /cases/bytag/Verfahrensstand?value=Klage+eingereicht`
- **THEN** the response SHALL include only cases with `tagName="Verfahrensstand"` and `tag_value="Klage eingereicht"`

#### Scenario: GET cases by tag without value filter
- **WHEN** a client requests `GET /cases/bytag/Verfahrensstand` (no value parameter)
- **THEN** the response SHALL include all cases with `tagName="Verfahrensstand"` regardless of `tag_value`

#### Scenario: GET documents by tag with value filter
- **WHEN** a client requests `GET /cases/documents/bytag/Dokumentstatus?value=Final`
- **THEN** the response SHALL include only documents with `tagName="Dokumentstatus"` and `tag_value="Final"`

#### Scenario: GET contacts by tag with value filter
- **WHEN** a client requests `GET /contacts/bytag/Mandantenstatus?value=Aktiv`
- **THEN** the response SHALL include only contacts with `tagName="Mandantenstatus"` and `tag_value="Aktiv"`

### Requirement: Tag Sort Order
Boolean tags and multi-value tags SHALL be sorted together in a single alphabetical list by tag name, then by tag value (if present). This applies to tag panels in entity editors and to tag filter popup menus. There SHALL be no separate grouping by tag type.

#### Scenario: Tag panel sorts boolean and multi-value tags together
- **GIVEN** boolean tags "Dringend", "Archiviert" and a multi-value tag "Aktentyp" with values "Beratung", "Klage"
- **WHEN** the tag panel is rendered on a case
- **THEN** the components SHALL appear in order: "Aktentyp" (dropdown), "Archiviert" (toggle), "Dringend" (toggle)

#### Scenario: Filter popup sorts entries alphabetically by display text
- **GIVEN** boolean tags "Dringend", "Archiviert" and a multi-value tag "Verfahrensstand" with values "Beratung", "Klage eingereicht"
- **WHEN** the tag filter popup is opened
- **THEN** the entries SHALL appear in order: "Archiviert", "Dringend", "Verfahrensstand: Beratung", "Verfahrensstand: Klage eingereicht"

#### Scenario: Multi-value tag values group naturally by shared prefix
- **GIVEN** a multi-value tag "Priorität" with values "Hoch", "Mittel", "Niedrig"
- **WHEN** the tag filter popup is opened
- **THEN** the three entries "Priorität: Hoch", "Priorität: Mittel", "Priorität: Niedrig" SHALL appear consecutively in the sorted list

### Requirement: Backward Compatibility
All changes to the REST API, webhook event payloads, and EJB remote interfaces MUST NOT break existing clients. Specifically: REST responses MAY include the new `tagValue` field (null for boolean tags) but MUST NOT remove or rename existing fields. REST requests without `tagValue` MUST be treated as boolean tag operations. Webhook event JSON payloads MAY include the new `tagValue` field but MUST retain all existing fields (`hookType`, `hookId`, `caseId`, `tagName`, `active`) unchanged. EJB remote method signatures MUST remain compatible — existing callers that do not set `tagValue` on tag beans MUST continue to work with `tagValue` defaulting to null.

#### Scenario: Existing REST client unaffected by new tagValue field in response
- **WHEN** an existing REST client fetches tags for a case
- **THEN** the response MAY include a `tagValue` field on each tag
- **AND** the client MUST NOT fail because an unknown field is present (JSON parsers typically ignore unknown fields)

#### Scenario: Existing REST client sends tag without tagValue
- **WHEN** an existing REST client creates a tag via POST without including `tagValue` in the request body
- **THEN** the system SHALL create a boolean tag with `tag_value=NULL`

#### Scenario: Existing webhook consumer receives new tagValue field
- **WHEN** an existing webhook consumer receives a `CaseTagChangedEvent`, `DocumentTagChangedEvent`, or `AddressTagChangedEvent` JSON payload
- **THEN** the payload SHALL contain all previously existing fields (`hookType`, `hookId`, entity ID, `tagName`, `active`)
- **AND** the new `tagValue` field SHALL be present (null for boolean tags)
- **AND** the consumer MUST NOT fail because an unknown field is present

#### Scenario: Existing EJB client calls setTag without tagValue
- **WHEN** an existing desktop client calls `setTag()` with an `ArchiveFileTagsBean` that has `tagValue=null`
- **THEN** the system SHALL create or remove a boolean tag as before

#### Scenario: REST bytag endpoint without value parameter
- **WHEN** an existing REST client requests `GET /cases/bytag/{tag}` without a `value` query parameter
- **THEN** the system SHALL return all cases with that tag regardless of `tag_value` (same behavior as before)

### Requirement: Multi-Value Tag Webhook Events
Webhook event payloads (`CaseTagChangedEvent`, `DocumentTagChangedEvent`, `AddressTagChangedEvent`) SHALL include a `tagValue` field. When a multi-value tag value is set or changed, a single event SHALL be fired with `active=true` and `tagValue` set to the new value. When the multi-value tag is cleared (empty dropdown selection), a single event SHALL be fired with `active=false`. No two-event sequence (deactivate old + activate new) SHALL be used for value changes. Boolean tag events SHALL continue to work as before with `tagValue=null`.

#### Scenario: Webhook fires when multi-value tag value is set
- **WHEN** a user selects "Klage eingereicht" from the "Verfahrensstand" dropdown on a case
- **THEN** the system SHALL fire one `CaseTagChangedEvent` with `active=true`, `tagName="Verfahrensstand"`, `tagValue="Klage eingereicht"`

#### Scenario: Webhook fires when multi-value tag value is changed
- **WHEN** a user changes "Verfahrensstand" from "Klage eingereicht" to "Verhandlung" on a case
- **THEN** the system SHALL fire one `CaseTagChangedEvent` with `active=true`, `tagName="Verfahrensstand"`, `tagValue="Verhandlung"`
- **AND** no event with `active=false` SHALL be fired for the old value

#### Scenario: Webhook fires when multi-value tag is cleared
- **WHEN** a user selects the empty entry from the "Verfahrensstand" dropdown on a case
- **THEN** the system SHALL fire one `CaseTagChangedEvent` with `active=false`, `tagName="Verfahrensstand"`

#### Scenario: Webhook fires when multi-value tag is set on contact
- **WHEN** a user selects "Aktiv" from the "Mandantenstatus" dropdown on a contact
- **THEN** the system SHALL fire one `AddressTagChangedEvent` with `active=true`, `tagName="Mandantenstatus"`, `tagValue="Aktiv"`

#### Scenario: Webhook fires without tagValue on boolean tag change
- **WHEN** a boolean tag "Dringend" is activated on a case
- **THEN** the webhook payload SHALL include `tagName="Dringend"`, `active=true`, `tagValue=null`

### Requirement: Multi-Value Tag Case History Text
When a multi-value tag is set, changed, or cleared, the case history entry SHALL include the tag value. Setting a multi-value tag SHALL produce a history entry in the format `"Akten-Etikett gesetzt: TagName = Value"`. Changing the value SHALL produce `"Akten-Etikett geändert: TagName = OldValue → NewValue"`. Clearing the tag SHALL produce the existing format `"Akten-Etikett entfernt: TagName"`. Boolean tag history text SHALL remain unchanged. Document tags use the `"Dokument-Etikett ..."` prefix, contact tags use `"Adress-Etikett ..."`.

#### Scenario: History entry when multi-value tag is set on case
- **WHEN** a user selects "Klage eingereicht" from the "Verfahrensstand" dropdown on a case
- **THEN** the case history SHALL contain an entry `"Akten-Etikett gesetzt: Verfahrensstand = Klage eingereicht"`

#### Scenario: History entry when multi-value tag value is changed on case
- **WHEN** a user changes the "Verfahrensstand" dropdown from "Beratung" to "Klage eingereicht" on a case
- **THEN** the case history SHALL contain an entry `"Akten-Etikett geändert: Verfahrensstand = Beratung → Klage eingereicht"`

#### Scenario: History entry when multi-value tag is cleared on case
- **WHEN** a user selects the empty entry from the "Verfahrensstand" dropdown on a case
- **THEN** the case history SHALL contain an entry `"Akten-Etikett entfernt: Verfahrensstand"`

#### Scenario: Boolean tag history unchanged
- **WHEN** a user activates the boolean tag "Dringend" on a case
- **THEN** the case history SHALL contain `"Akten-Etikett gesetzt: Dringend"` (no value suffix)

#### Scenario: History entry for multi-value document tag
- **WHEN** a user selects "Final" from the "Dokumentstatus" dropdown on a document
- **THEN** the case history SHALL contain an entry `"Dokument-Etikett gesetzt: Dokumentstatus = Final"`

### Requirement: Multi-Value Tag Display in Lists and Tooltips
Multi-value tags SHALL be displayed in the `"TagName: Value"` format across all tag list and tooltip display contexts. `TagUtils.getTagList()` SHALL render multi-value tags as `"TagName: Value"` entries in the comma-separated tag string shown in search result lists, entry panels, and group headers. `TagUtils.getTagListTooltip()` SHALL render multi-value tags as `"TagName: Value"` entries in the HTML tooltip with timestamps. `TagUtils.getDocumentTagsOverviewAsHtml()` SHALL show per-value counts for multi-value tags (e.g. `"Dokumentstatus: Final (3)"`). Boolean tags SHALL continue to display as tag name only.

#### Scenario: Multi-value tag in search result tag list
- **WHEN** a case has the multi-value tag "Verfahrensstand" with value "Klage eingereicht" and boolean tag "Dringend"
- **THEN** `TagUtils.getTagList()` SHALL return a string containing `"Dringend, Verfahrensstand: Klage eingereicht"`

#### Scenario: Multi-value tag in tooltip
- **WHEN** a user hovers over a case's tag display
- **AND** the case has the multi-value tag "Verfahrensstand" with value "Klage eingereicht" set on 2026-01-15
- **THEN** the tooltip SHALL show `"Verfahrensstand: Klage eingereicht"` with the timestamp

#### Scenario: Multi-value tag in document tag overview
- **GIVEN** a case with 5 documents, 3 of which have tag "Dokumentstatus" with value "Final" and 1 with value "Entwurf"
- **WHEN** the document tag overview tooltip is rendered
- **THEN** it SHALL show `"Dokumentstatus: Final (3)"` and `"Dokumentstatus: Entwurf (1)"`

#### Scenario: Boolean tag display unaffected
- **WHEN** a case has only boolean tags "Dringend" and "Archiviert"
- **THEN** `TagUtils.getTagList()` SHALL return `"Archiviert, Dringend"` (no value suffix)

### Requirement: Client Startup Caching of Multi-Value Tag Definitions
During client startup, the `SplashThread` SHALL load all multi-value tag definitions for all three entity types and cache them for use by tag panels and filter popup menus. A new method `getOptionGroupsByPrefix(String prefix)` SHALL be added to `SystemManagementRemote` and `SystemManagementLocal` to efficiently retrieve all option groups whose name starts with a given prefix. This avoids calling `getAllOptionGroups()` (which returns hundreds of unrelated entries) or making per-tag `getOptionGroup()` calls (which requires knowing tag names in advance).

#### Scenario: Client loads multi-value tag definitions at startup
- **WHEN** the client starts and `SplashThread` runs
- **THEN** the system SHALL call `getOptionGroupsByPrefix("archiveFile.tags.mv.")`, `getOptionGroupsByPrefix("document.tags.mv.")`, and `getOptionGroupsByPrefix("address.tags.mv.")`
- **AND** the results SHALL be cached for use throughout the client session

#### Scenario: Tag panel uses cached definitions
- **WHEN** a user opens a case and the tag panel is rendered
- **THEN** multi-value tag definitions SHALL be read from the startup cache
- **AND** no additional server call SHALL be needed to discover multi-value tag names or values

#### Scenario: getOptionGroupsByPrefix returns matching groups
- **WHEN** `getOptionGroupsByPrefix("archiveFile.tags.mv.")` is called
- **AND** option groups `archiveFile.tags.mv.Verfahrensstand` and `archiveFile.tags.mv.Priorität` exist
- **THEN** the method SHALL return both groups with their values

### Requirement: REST API for Multi-Value Tag Definitions
The REST API v7 SHALL provide an endpoint to discover multi-value tag definitions and their available values. `GET /rest/v7/configuration/tags/multivalue/{entityType}` (where `entityType` is `cases`, `documents`, or `contacts`) SHALL return a JSON array of tag definitions, each containing the tag name and its list of selectable values. This allows external applications to discover which tags are multi-value and render appropriate UI controls.

#### Scenario: GET multi-value tag definitions for cases
- **WHEN** a client requests `GET /rest/v7/configuration/tags/multivalue/cases`
- **AND** multi-value tags "Verfahrensstand" (values: "Beratung", "Klage eingereicht") and "Priorität" (values: "Hoch", "Niedrig") are defined
- **THEN** the response SHALL include both tag definitions with their values

#### Scenario: GET multi-value tag definitions for documents
- **WHEN** a client requests `GET /rest/v7/configuration/tags/multivalue/documents`
- **THEN** the response SHALL include all document multi-value tag definitions with their values

#### Scenario: GET multi-value tag definitions for contacts
- **WHEN** a client requests `GET /rest/v7/configuration/tags/multivalue/contacts`
- **THEN** the response SHALL include all contact multi-value tag definitions with their values

#### Scenario: No multi-value tags defined
- **WHEN** a client requests `GET /rest/v7/configuration/tags/multivalue/cases`
- **AND** no case multi-value tags are defined
- **THEN** the response SHALL return an empty JSON array

### Requirement: Automated Document Tag Rules for Multi-Value Tags
The existing `DocumentTagRule` mechanism (which auto-applies tags to documents based on file name patterns) SHALL be extended to support multi-value tags. A `DocumentTagRule` MAY reference a multi-value tag by storing both `tagName` and `tagValue`. When the rule matches, the tag SHALL be applied with the configured value. The `TagRulesDialog` configuration UI SHALL allow selecting a multi-value tag and a specific value from its dropdown; the value selection SHALL be mandatory when a multi-value tag is chosen. Boolean tag rules SHALL remain unchanged.

#### Scenario: Tag rule applies multi-value tag to matching document
- **GIVEN** a document tag rule configured with tag name "Dokumentstatus", tag value "Entwurf", and file pattern "*.docx"
- **WHEN** a document "Schriftsatz.docx" is uploaded to a case
- **THEN** the system SHALL automatically apply the tag "Dokumentstatus" with `tag_value="Entwurf"` to the document

#### Scenario: Tag rule dialog shows value dropdown for multi-value tags
- **WHEN** an administrator selects a multi-value tag "Dokumentstatus" in the tag rules dialog
- **THEN** a value dropdown SHALL appear showing the available values ("Entwurf", "Geprüft", "Final")
- **AND** the user MUST select a value before saving the rule

#### Scenario: Tag rule dialog hides value dropdown for boolean tags
- **WHEN** an administrator selects a boolean tag "Dringend" in the tag rules dialog
- **THEN** no value dropdown SHALL appear

#### Scenario: Boolean tag rule unaffected
- **GIVEN** a document tag rule configured with boolean tag "Dringend" and file pattern "*.pdf"
- **WHEN** a document "Mahnung.pdf" is uploaded
- **THEN** the system SHALL apply the boolean tag "Dringend" with `tag_value=NULL` (same as before)
