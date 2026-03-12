## Context
j-lawyer.org supports boolean tags for cases, documents, and contacts via three tables (`case_tags`, `document_tags`, `contact_tags`) and corresponding entity beans. Tag definitions are stored in the `server_options` table under option groups `archiveFile.tags`, `document.tags`, and `address.tags`. The client already has a `MultiValueTag` Swing component (label + JComboBox) but it is not yet wired into the tag system. This change extends the existing tag infrastructure to support dropdown-style tags with selectable values.

## Goals / Non-Goals
- Goals:
  - Allow administrators to define multi-value tags with a named label and a list of selectable values
  - Store the selected value alongside the tag name in existing tag tables
  - Display multi-value tags as dropdown widgets in the tag panel (using existing `MultiValueTag` component)
  - Expose tag values through REST API and webhook events
  - Support all three entity types: cases, documents, contacts
- Non-Goals:
  - Global vs. case-specific tag scoping (all tags remain global)
  - Value transition rules or workflow enforcement (any value can be selected at any time)
  - Tag value history or audit trail beyond the existing `date_set` timestamp
  - Hierarchical or nested tag values

## Decisions

### Data Model: Extend Existing Tables
- **Decision**: Add nullable `tag_value` VARCHAR(250) column to `case_tags`, `document_tags`, and `contact_tags`
- **Alternatives considered**:
  - Separate `case_tag_values` junction table → unnecessary complexity for a single optional attribute
  - New entity/table for multi-value tag definitions → over-engineered; `server_options` already handles tag definitions
- **Rationale**: A single nullable column is the simplest approach. Existing boolean tags have `tag_value=NULL`; multi-value tags have `tag_value` set to the selected dropdown value (or NULL/empty if no value selected). This is fully backward-compatible — existing code that ignores `tag_value` continues to work.

### Multi-Value Tag Definitions via server_options
- **Decision**: Use dynamically named option groups in `server_options` table, one row per selectable value:
  - `archiveFile.tags.mv.{TagName}` for case multi-value tags
  - `document.tags.mv.{TagName}` for document multi-value tags
  - `address.tags.mv.{TagName}` for contact multi-value tags
  - Each row's `value` column stores a single selectable value
  - Example: option group `archiveFile.tags.mv.Verfahrensstand` with rows: `Beratung`, `Klage eingereicht`, `Verhandlung`, `Abgeschlossen`
  - The multi-value tag name is derived from the option group suffix after `*.tags.mv.`
- **Alternatives considered**:
  - Single row with comma-separated values (`TagName:Value1,Value2,Value3`) → breaks when a value contains a comma; escaping adds complexity
  - Single row with JSON value list → harder to read/edit manually, overkill for flat list
  - Different delimiter (pipe, semicolon) → same delimiter collision problem, just deferred
  - Separate definition table → adds schema complexity; option groups are the established pattern
- **Rationale**: Follows the existing `AppOptionGroupBean` pattern where each row is one value within an option group. Avoids all delimiter/escaping issues since values are stored one per row. The tag name is encoded in the option group name itself, which is clean and unambiguous.

### Tag Assignment Semantics
- **Decision**: When a multi-value tag is assigned, a row is inserted into the tag table with `tagName` set to the multi-value tag's label and `tagValue` set to the selected dropdown value. Selecting an empty/blank value removes the tag row (same as deactivating a boolean tag). Changing the dropdown value updates the existing row's `tagValue`.
- **Rationale**: Consistent with how boolean tags work — presence of a row means "tag is active". The `tagValue` simply adds which value is selected.

### setTag Value Change Logic
- **Decision**: When `setTag(active=true)` is called for a multi-value tag whose `tagName` already has a row, the existing row's `tag_value` SHALL be updated (rather than skipping with "war bereits gesetzt"). The current code's `findByArchiveFileKeyAndTagName` check finds the row, but today the `active=true` + `!check.isEmpty()` branch is a no-op. For multi-value tags, this branch must update `tag_value` on the existing row.
- **Rationale**: A value change is semantically "still active, different value" — one call, one event.

### Webhook Semantics for Value Changes
- **Decision**: When a multi-value tag value is set or changed, fire a single `CaseTagChangedEvent` / `DocumentTagChangedEvent` / `AddressTagChangedEvent` with `active=true` and the new `tagValue`. When cleared (empty dropdown), fire a single event with `active=false`. No two-event sequence (deactivate old + activate new).
- **Alternatives considered**:
  - Two events per value change (active=false for old, active=true for new) → unnecessarily chatty, harder for consumers to correlate
- **Rationale**: The tag remains active, only the value changes. The `MultiValueTag` dropdown always has an empty first entry to represent removal, so clearing is always an explicit `active=false`. Consumers who need old-value tracking can maintain their own state.

### REST API Extension
- **Decision**: Add nullable `tagValue` field to the shared `RestfulTagV1` POJO (used across REST v1, v5, and later versions). Since `RestfulTagV1` is the common tag POJO shared by all API versions, adding the field there makes `tagValue` available across all versions that return tags. All REST endpoint code that converts entity beans to `RestfulTagV1` (in `CasesEndpointV1`, `CasesEndpointV5`, `ContactsEndpointV5`) must also populate `tagValue` from the entity bean. Existing endpoints continue to work — `tagValue` is simply null for boolean tags.
- **Rationale**: Backward-compatible. Adding a nullable field to a shared POJO is additive. Older clients that ignore unknown JSON fields or don't send `tagValue` continue to work.

### Client UI Integration
- **Decision**: In the tag panel (`TagPanel`), render multi-value tags using the existing `MultiValueTag` component (JPanel with JLabel + JComboBox) alongside regular `TagToggleButton` components. Multi-value tags appear after boolean tags, visually distinguished by their dropdown control. The `MultiValueTag` component's action listener triggers the same server call as boolean tags, but includes the selected `tagValue`.
- **Rationale**: Reuses existing component; maintains visual consistency with WrapLayout-based tag panel.

### Configuration UI
- **Decision**: Create a new `MultiValueTagConfigurationDialog` with a master-detail layout. The existing `OptionGroupConfigurationDialog` remains untouched — it is a generic dialog used for many non-tag option groups (titles, professions, currencies, etc.) and should not be burdened with tag-specific logic.
- **Layout**:
  - **Left side (master)**: List of all multi-value tag names for the current entity type, with add/rename/delete buttons below. Adding a tag prompts for the name, creates the option group `*.tags.mv.{name}`, adds it to the list, and selects it immediately so the user can start adding values.
  - **Right side (detail)**: List of values for the selected tag, with add/rename/delete buttons below. Only enabled when a tag is selected on the left.
- **Tag-level operations** (left side):
  - Create: prompt for tag name, validate no collision with boolean tags, create empty option group, auto-select
  - Rename: prompt for new name, ask user whether to update `tagName` in existing tag rows, rename option group
  - Delete: ask user whether to remove existing tag rows with that `tagName`, remove option group
- **Value-level operations** (right side):
  - Add: add row to the selected tag's option group
  - Rename: prompt for new value, ask user whether to update `tag_value` in existing tag rows
  - Delete: ask user whether to remove tag rows with that `tag_value`, or keep them orphaned
- **Invocation**: New menu items in `JKanzleiGUI` for multi-value case tags, document tags, and address tags, each opening `MultiValueTagConfigurationDialog` with the appropriate prefix (`archiveFile.tags.mv.`, `document.tags.mv.`, `address.tags.mv.`)
- **Alternatives considered**:
  - Extend existing `OptionGroupConfigurationDialog` with a dropdown → confusing UX for creating new tags (empty dropdown state); also pollutes a generic dialog with tag-specific logic
  - Comma-separated text field for values → cannot detect renames vs. add+remove; one-row-per-value makes change detection trivial
- **Rationale**: Master-detail is the natural pattern for a two-level hierarchy (tags → values). Creating a new tag immediately selects it, so the user flows naturally into adding values. The generic `OptionGroupConfigurationDialog` stays clean for its many other uses. Since each value is a separate `AppOptionGroupBean` row with its own `id`, rename detection is trivial (same row id, different value).

### Tag Filter Popup Menus
- **Decision**: In all tag filter popup menus (used in 8 search dialogs + desktop panel), each multi-value tag value appears as a flat `JCheckBoxMenuItem` with display text `"TagName: Value"` (e.g. `"Verfahrensstand: Klage eingereicht"`). The tag name and value are stored as client properties on the menu item (`putClientProperty("tagName", ...)` and `putClientProperty("tagValue", ...)`), so `TagUtils.getSelectedTags()` reads metadata from properties — never by parsing the display label. This avoids ambiguity even if tag names or values contain colons. Boolean tags remain as flat `JCheckBoxMenuItem` entries with only `tagName` set and `tagValue=null`. Users can check none, one, or multiple values of the same multi-value tag.
- **Affected dialogs**:
  - `QuickArchiveFileSearchPanel` — case search (case tag + document tag filter popups)
  - `QuickAddressSearchPanel` — contact search (address tag filter popup)
  - `SearchAndAssignDialog` — document assignment (case + document tag filter popups)
  - `ArchiveFilePanel` — document tag filter within case editor
  - `AddBeaRecipientSearchDialog` — BEA recipient search (address tag filter popup)
  - `AddRecipientSearchDialog` — email recipient search (address tag filter popup)
  - `AddAddressSearchDialog` — contact-to-case linking (address tag filter popup)
  - `MultiAddressSearchDialog` — bulk mail (address tag filter popup)
  - `DesktopPanel` + `TaggedTimerTask` — tagged items tab (case + document tag filter popups)
- **Alternatives considered**:
  - Submenus per multi-value tag with values as sub-items → difficult to navigate in Swing popup menus, poor UX
  - Parsing tag name and value from the display string (e.g. splitting on ": ") → fragile if names or values contain ": "; rejected in favor of client properties
  - Tag name only without value filtering → loses the key benefit of multi-value tags in search workflows
- **Rationale**: Flat entries are easy to scan and click. Client properties cleanly separate display text from data, eliminating any parsing risk. The approach requires only changes to `TagUtils.populateTags()` and `TagUtils.getSelectedTags()`.

### Desktop Panel Tagged Items Tab Labels
- **Decision**: On the `DesktopPanel` tagged items tabbed pane, selected multi-value tag values appear as tab labels in the format `"TagName: Value"` (e.g. `"Verfahrensstand: Klage eingereicht"`), consistent with the filter popup display. Each checked value gets its own tab, just like boolean tags. Users can display none, one, or multiple values of the same multi-value tag simultaneously.
- **Rationale**: Consistent with the existing pattern where each selected boolean tag gets a tab. The `"TagName: Value"` format is unambiguous for display purposes; internally the tab stores tag name and value as separate data fields.

### Server-Side Tag Value Filtering
- **Decision**: Extend `searchEnhanced` / `searchTagsEnhanced` / `getTagged` methods to accept an optional map of tag-name-to-values alongside the existing `String[] tagName` parameter. When values are provided for a multi-value tag, the SQL query adds a `tag_value IN (...)` clause. When no values are specified (or for boolean tags), the existing `tagName` matching behavior is preserved.
- **Alternatives considered**:
  - Encode value in tag name string (e.g. "Verfahrensstand=Klage eingereicht") → fragile parsing, breaks existing API contracts
  - Separate endpoint/method for value filtering → duplicates logic unnecessarily
- **Rationale**: Extends existing search methods with minimal API surface change. A `Map<String, String[]>` parameter (tag name → selected values) is clean and backward-compatible — callers that pass null or empty map get the same behavior as before.

### REST API Tag Value Filtering
- **Decision**: Extend REST v7 `/bytag/{tag}` endpoints with an optional `value` query parameter. `GET /bytag/{tag}?value=Klage+eingereicht` filters by specific value. Omitting the parameter returns all items with that tag regardless of value (backward-compatible).
- **Rationale**: Simple, RESTful extension. Existing clients that don't send the `value` parameter are unaffected.

### Case History Text
- **Decision**: When a multi-value tag is set or changed, the case history entry SHALL include the tag value. Format: `"Akten-Etikett gesetzt: TagName = Value"` (e.g. `"Akten-Etikett gesetzt: Verfahrensstand = Klage eingereicht"`). When a value changes, the history entry SHALL show old and new value: `"Akten-Etikett geändert: TagName = OldValue → NewValue"` (e.g. `"Akten-Etikett geändert: Verfahrensstand = Beratung → Klage eingereicht"`). When a multi-value tag is cleared, the history entry uses the existing format: `"Akten-Etikett entfernt: TagName"`. Boolean tag history text remains unchanged (`"Akten-Etikett gesetzt: TagName"` / `"Akten-Etikett entfernt: TagName"`). Document tags use the corresponding `"Dokument-Etikett ..."` prefix. Contact tags use `"Adress-Etikett ..."` prefix.
- **Rationale**: Including the value in history entries gives users a clear audit trail of status changes without needing a separate history mechanism.

### Tag Display in Lists and Tooltips
- **Decision**: `TagUtils.getTagList()` SHALL render multi-value tags as `"TagName: Value"` in the comma-separated tag string used in search result lists. `TagUtils.getTagListTooltip()` SHALL show multi-value tags as `"TagName: Value"` in the HTML tooltip with timestamps. `TagUtils.getDocumentTagsOverviewAsHtml()` SHALL show per-value counts for multi-value tags (e.g. `"Dokumentstatus: Final (3)"`). All three methods read from tag beans that already carry the `tagValue` field, so no additional server calls are needed.
- **Affected display locations**: `QuickArchiveFileSearchThread` result rows, `LastChangedEntryPanelTransparent`, `TaggedEntryPanelTransparent`, `CaseGroupHeaderPanel`, document tag overview tooltip in `ArchiveFilePanel`.
- **Rationale**: Consistent `"TagName: Value"` format across all display contexts. The tag list and tooltip methods already iterate over tag beans; they just need to check `tagValue != null` and append the value.

### Client Startup Caching of Multi-Value Tag Definitions
- **Decision**: During client startup, `SplashThread` SHALL load all multi-value tag definitions (option groups matching `*.tags.mv.*`) and cache them for use by tag panels and filter popups. A new method `getOptionGroupsByPrefix(String prefix)` SHALL be added to `SystemManagementRemote` / `SystemManagementLocal` to efficiently retrieve all option groups whose name starts with a given prefix (e.g. `"archiveFile.tags.mv."`). This avoids the need to call `getAllOptionGroups()` and filter client-side or to make N individual `getOptionGroup()` calls.
- **Alternatives considered**:
  - Call `getAllOptionGroups()` and filter client-side → returns all option groups (hundreds of unrelated entries), wasteful
  - Call `getOptionGroup(exactName)` per tag → requires knowing tag names in advance, which is the chicken-and-egg problem
- **Rationale**: A single prefix-based query is efficient and clean. The `SplashThread` already loads boolean tag definitions; multi-value tags follow the same pattern.

### REST API for Multi-Value Tag Definitions
- **Decision**: Add REST v7 endpoints to discover multi-value tag definitions and their available values. Endpoints: `GET /rest/v7/configuration/tags/multivalue/{entityType}` where `entityType` is `cases`, `documents`, or `contacts`. Returns a JSON array of tag definitions, each containing the tag name and its list of selectable values. This allows external applications (e.g. mobile apps, integrations) to render dropdowns for multi-value tags.
- **Rationale**: Without a definitions endpoint, external clients can only read/write tag values but cannot discover which tags are multi-value or what values are available. This endpoint completes the REST API story for multi-value tags.

### Automated Document Tag Rules
- **Decision**: The existing `DocumentTagRule` mechanism (which auto-applies boolean tags to documents based on file name patterns) SHALL be extended to support multi-value tags. A `DocumentTagRule` can reference a multi-value tag by storing both `tagName` and `tagValue`. When the rule matches, the tag is applied with the specified value. The `TagRulesDialog` configuration UI SHALL be updated to allow selecting a multi-value tag and a specific value from its dropdown. If a multi-value tag is selected, the value dropdown becomes mandatory.
- **Alternatives considered**:
  - Defer to a later release → users expect tag rules to work consistently for all tag types; partial support would be confusing
- **Rationale**: Tag rules are a core automation feature. Extending them to support multi-value tags is a natural and expected behavior.

### Sort Order
- **Decision**: Boolean tags and multi-value tags are sorted together in a single alphabetical list — by tag name first, then by tag value (if present). This applies to both tag panels (case/document/contact editor) and filter popup menus. No separate grouping by tag type.
- **Tag panels**: `TagToggleButton` (boolean) and `MultiValueTag` (dropdown) components are interleaved alphabetically by tag name. The component type itself provides visual distinction.
- **Filter popup menus**: All `JCheckBoxMenuItem` entries sorted alphabetically by display text. Multi-value tag values naturally group together because they share the `"TagName: "` prefix.
- **Alternatives considered**:
  - Two separate groups (boolean first, multi-value second) → splits alphabetically adjacent tags apart, less intuitive
- **Rationale**: Single alphabetical sort is the simplest mental model. Users scan one sorted list rather than two sections.

### Backward Compatibility
- **Decision**: All API surfaces (REST, webhook JSON, EJB remote interfaces) MUST only add new fields, never remove or rename existing ones. The new `tagValue` field is always nullable and defaults to null. Existing clients that ignore unknown JSON fields or don't populate `tagValue` on tag beans continue to work without changes.
- **Webhook payloads**: `CaseTagChangedEvent`, `DocumentTagChangedEvent`, and `AddressTagChangedEvent` JSON gains a `tagValue` key. Existing fields (`hookType`, `hookId`, entity ID, `tagName`, `active`) remain unchanged. Consumers using lenient JSON parsing (the norm) are unaffected by the additional field.
- **REST API**: Responses gain `tagValue` on tag objects. Requests without `tagValue` default to null. The `/bytag/{tag}` filter endpoint gains an optional `value` query parameter; omitting it preserves existing behavior.
- **EJB remote interfaces**: Method signatures remain the same (`setTag(id, tagBean, active)`). The `tagValue` field is on the entity bean itself, so callers that don't set it get null by default.

## Risks / Trade-offs
- **Tag name collision**: A multi-value tag name could collide with a boolean tag name → Mitigation: Validate at configuration time that multi-value tag names don't overlap with boolean tag names within the same entity type
- **Value list changes**: If an administrator removes a value from the definition, existing tags with that value remain in the database → Accepted: the old value is still stored and displayed; user can re-select from updated list
- **Filter popup complexity**: Submenus add one level of nesting → Accepted: only multi-value tags get submenus; boolean tags remain flat; visual grouping is an improvement over many flat entries

## Migration Plan
1. SQL DDL:
   ```sql
   ALTER TABLE case_tags ADD COLUMN tag_value VARCHAR(250) DEFAULT NULL;
   ALTER TABLE document_tags ADD COLUMN tag_value VARCHAR(250) DEFAULT NULL;
   ALTER TABLE contact_tags ADD COLUMN tag_value VARCHAR(250) DEFAULT NULL;
   ```
2. Existing tag rows get `tag_value=NULL` — no behavioral change for boolean tags
3. Rollback:
   ```sql
   ALTER TABLE case_tags DROP COLUMN tag_value;
   ALTER TABLE document_tags DROP COLUMN tag_value;
   ALTER TABLE contact_tags DROP COLUMN tag_value;
   ```

## Open Questions
None — all questions resolved.
