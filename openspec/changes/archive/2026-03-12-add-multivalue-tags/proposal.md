# Change: Add Multi-Value / Dropdown Tags

## Why
Status-like information (e.g. "Verfahrensstand: Klage eingereicht", "Mandatsstatus: aktiv") currently requires many individual boolean tags with numeric prefixes to simulate ordering. This clutters the tag panel and forces users to manage mutually exclusive states manually. Multi-value (dropdown) tags allow a single named label with selectable values, reducing tag count and providing a cleaner workflow for status-driven categorization across cases, documents, and contacts.

## What Changes
- Add nullable `tag_value` column (VARCHAR) to all three tag tables (`case_tags`, `document_tags`, `contact_tags`) to store the selected dropdown value
- Introduce new `server_options` option groups (`archiveFile.tags.mv.{TagName}`, `document.tags.mv.{TagName}`, `address.tags.mv.{TagName}`) for multi-value tag definitions, one row per selectable value
- Add constants for the new option groups to `OptionConstants`
- Extend EJB service `setTag` methods to accept and persist `tagValue`
- Extend REST API v7 tag endpoints with nullable `tagValue` field (backward-compatible)
- Enrich webhook event payloads with `tagValue` when present
- Integrate the existing `MultiValueTag` Swing component into the tag panel alongside regular `TagToggleButton` components
- Add new `MultiValueTagConfigurationDialog` with master-detail layout for managing multi-value tags
- Support renaming multi-value tag definitions (propagate name changes to existing tag rows)
- Extend all tag filter popup menus to show multi-value tags as flat `"TagName: Value"` entries
- Extend server-side search/filter methods to support filtering by tag value
- Extend REST API tag filter endpoints to support optional value parameter
- Include tag values in case history text entries (set, change, clear)
- Update `TagUtils` display methods (`getTagList`, `getTagListTooltip`, `getDocumentTagsOverviewAsHtml`) for multi-value tag display
- Add `getOptionGroupsByPrefix()` to `SystemManagementRemote` for efficient client startup caching of multi-value tag definitions
- Add REST API endpoint to discover multi-value tag definitions and available values
- Extend `DocumentTagRule` / `TagRulesDialog` to support multi-value tags with specific values

## Impact
- Affected specs: new spec `tagging` (new capability)
- Affected code:
  - `j-lawyer-server-entities`: `ArchiveFileTagsBean`, `DocumentTagsBean`, `AddressTagsBean` (new `tagValue` field)
  - `j-lawyer-server-common`: `OptionConstants` (new option group constants)
  - `j-lawyer-server-api`: `ArchiveFileServiceRemote`, `AddressServiceRemote` (updated JavaDoc, extended search signatures)
  - `j-lawyer-server/j-lawyer-server-ejb`: `ArchiveFileService`, `AddressService` (persist tagValue, filter by tagValue), `AppOptionGroupBeanFacade`
  - `j-lawyer-server-io`: REST v7 tag endpoints (include tagValue in request/response, filter by value)
  - `j-lawyer-client` tag filter popups (8 dialogs + desktop panel):
    - `QuickArchiveFileSearchPanel` (case search: case tag + document tag filters)
    - `QuickAddressSearchPanel` (contact search: address tag filter)
    - `SearchAndAssignDialog` (document assignment: case + document tag filters)
    - `ArchiveFilePanel` (document tag filter within case editor)
    - `AddBeaRecipientSearchDialog` (BEA recipient: address tag filter)
    - `AddRecipientSearchDialog` (email recipient: address tag filter)
    - `AddAddressSearchDialog` (contact-to-case linking: address tag filter)
    - `MultiAddressSearchDialog` (bulk mail: address tag filter)
    - `DesktopPanel` + `TaggedTimerTask` (tagged items tab: case + document tag filters)
  - `j-lawyer-client`: `TagUtils` (popup building, selection extraction, tag list display, tooltips, document tag overview), `TagPanel`, `ArchiveFilePanel`, `AddressPanel`, action listeners, `SplashThread` (startup caching)
  - `j-lawyer-client`: `TagRulesDialog` (multi-value tag rule configuration)
  - `j-lawyer-server-api`: `SystemManagementRemote` (new `getOptionGroupsByPrefix()` method)
  - Database: DDL migration adding `tag_value` column to three tables
