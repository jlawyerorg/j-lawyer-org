## 1. Database Migration & Entity Changes
- [ ] 1.1 Create SQL migration script adding `tag_value VARCHAR(250) DEFAULT NULL` to `case_tags`, `document_tags`, and `contact_tags`
- [ ] 1.2 Add `tagValue` field with `@Column(name = "tag_value")` to `ArchiveFileTagsBean`
- [ ] 1.3 Add `tagValue` field with `@Column(name = "tag_value")` to `DocumentTagsBean`
- [ ] 1.4 Add `tagValue` field with `@Column(name = "tag_value")` to `AddressTagsBean`

## 2. Configuration Constants
- [ ] 2.1 Add `OPTIONGROUP_ARCHIVEFILETAGS_MV_PREFIX = "archiveFile.tags.mv."` to `OptionConstants`
- [ ] 2.2 Add `OPTIONGROUP_DOCUMENTTAGS_MV_PREFIX = "document.tags.mv."` to `OptionConstants`
- [ ] 2.3 Add `OPTIONGROUP_ADDRESSTAGS_MV_PREFIX = "address.tags.mv."` to `OptionConstants`

## 3. EJB Service Changes
- [ ] 3.1 Update `ArchiveFileService.setTag()`: when `active=true` and tag row already exists, update `tag_value` on the existing row instead of skipping; persist `tagValue` for new rows
- [ ] 3.2 Update `ArchiveFileService.setDocumentTagImpl()`: same update-on-exists logic for `tag_value`
- [ ] 3.3 Update `AddressService.setTag()`: same update-on-exists logic for `tag_value`
- [ ] 3.4 Update `ArchiveFileServiceRemote` JavaDoc for changed tag methods
- [ ] 3.5 Update `AddressServiceRemote` JavaDoc for changed tag methods

## 4. Webhook Event Enrichment
- [ ] 4.1 Add `tagValue` field to `CaseTagChangedEvent`, `DocumentTagChangedEvent`, and `AddressTagChangedEvent`, include in JSON serialization
- [ ] 4.2 Set `tagValue` on event when firing from `setTag` / `setDocumentTagImpl` / `AddressService.setTag`
- [ ] 4.3 Single event per value change: `active=true` with new value (no deactivate+activate pair)

## 5. Backward Compatibility Verification
- [ ] 5.1 Verify REST responses only add `tagValue` field, no existing fields removed or renamed
- [ ] 5.2 Verify REST requests without `tagValue` create boolean tags (`tag_value=NULL`)
- [ ] 5.3 Verify webhook JSON payloads retain all existing fields and only add `tagValue`
- [ ] 5.4 Verify EJB `setTag()` with `tagValue=null` on tag bean behaves as boolean tag (no regression)
- [ ] 5.5 Verify `/bytag/{tag}` without `value` query parameter returns all items with that tag

## 6. REST API Extensions
- [ ] 6.1 Add nullable `tagValue` field to shared `RestfulTagV1` POJO
- [ ] 6.2 Update `CasesEndpointV1` tag population code to set `tagValue` from entity bean
- [ ] 6.3 Update `CasesEndpointV5` tag population code to set `tagValue` from entity bean
- [ ] 6.4 Update `ContactsEndpointV5` tag population code to set `tagValue` from entity bean
- [ ] 6.5 Update REST v7 case tag endpoints to accept and return `tagValue`
- [ ] 6.6 Update REST v7 document tag endpoints to accept and return `tagValue`
- [ ] 6.7 Update REST v7 contact tag endpoints to accept and return `tagValue`

## 7. Client Configuration Dialog
- [ ] 7.1 Create new `MultiValueTagConfigurationDialog` with master-detail layout (left: tag names list, right: values list for selected tag)
- [ ] 7.2 Left side: add/rename/delete controls for multi-value tag names; creating a tag auto-selects it
- [ ] 7.3 Right side: add/rename/delete controls for values within the selected tag; disabled when no tag selected
- [ ] 7.4 Validate that multi-value tag names do not collide with boolean tag names
- [ ] 7.5 On tag rename: prompt user whether to update `tagName` in existing tag rows; rename option group
- [ ] 7.6 On tag delete: prompt user whether to remove existing tag rows with that `tagName`; remove option group
- [ ] 7.7 On value rename: prompt user whether to update `tag_value` in existing tag rows
- [ ] 7.8 On value delete: prompt user whether to remove existing tag rows with that `tag_value`
- [ ] 7.9 Add menu items in `JKanzleiGUI` to open `MultiValueTagConfigurationDialog` for case, document, and address multi-value tags

## 8. Client Tag Display
- [ ] 8.1 Update `TagUtils` to discover multi-value tags by querying option groups matching `*.tags.mv.*` prefix and extracting tag names from group suffixes
- [ ] 8.2 Render multi-value tags as `MultiValueTag` components in the case tag panel (`ArchiveFilePanel`)
- [ ] 8.3 Render multi-value tags as `MultiValueTag` components in the contact tag panel (`AddressPanel`)
- [ ] 8.4 Render multi-value tags as `MultiValueTag` components in the document tag area
- [ ] 8.5 Wire `MultiValueTag` combo box action listener to call `setTag` / `setDocumentTag` with `tagValue`
- [ ] 8.6 Populate `MultiValueTag` combo box selection from existing tag row's `tagValue` when loading entity

## 9. Server-Side Rename/Remove Support
- [ ] 9.1 Add server-side method to rename `tagName` in tag rows across `case_tags`, `document_tags`, `contact_tags`
- [ ] 9.2 Add server-side method to rename `tag_value` in tag rows matching a given `tagName`
- [ ] 9.3 Add server-side method to delete tag rows matching a given `tagName` and `tag_value`
- [ ] 9.4 Add server-side method to delete tag rows matching a given `tagName` (for full tag deletion)

## 10. Server-Side Tag Value Filtering
- [ ] 10.1 Extend `ArchiveFileService.searchEnhanced()` to accept optional tag-value map and add `tag_value IN (...)` clause when values are provided
- [ ] 10.2 Extend `ArchiveFileService.searchTagsEnhanced()` to support tag-value filtering
- [ ] 10.3 Extend `ArchiveFileService.getTagged()` to support tag-value filtering
- [ ] 10.4 Extend `AddressService.searchEnhanced()` to accept optional tag-value map
- [ ] 10.5 Extend `AddressService.searchTagsEnhanced()` to support tag-value filtering
- [ ] 10.6 Update `ArchiveFileServiceRemote` and `AddressServiceRemote` JavaDoc for extended search signatures

## 11. Client Tag Filter Popup Menus
- [ ] 11.1 Update `TagUtils.populateTags()` to render multi-value tag values as flat `JCheckBoxMenuItem` entries with display text `"TagName: Value"` and client properties `tagName`/`tagValue` for metadata
- [ ] 11.2 Update `TagUtils.getSelectedTags()` to read `tagName`/`tagValue` client properties from menu items (never parse display label) and return a structure that conveys value selections
- [ ] 11.3 Update `QuickArchiveFileSearchPanel` to pass tag-value selections to search (case tag + document tag filters)
- [ ] 11.4 Update `QuickAddressSearchPanel` to pass tag-value selections to search (address tag filter)
- [ ] 11.5 Update `SearchAndAssignDialog` to pass tag-value selections to search (case + document tag filters)
- [ ] 11.6 Update `ArchiveFilePanel` document tag filter to support multi-value tag value filtering
- [ ] 11.7 Update `AddBeaRecipientSearchDialog` to pass tag-value selections to search
- [ ] 11.8 Update `AddRecipientSearchDialog` to pass tag-value selections to search
- [ ] 11.9 Update `AddAddressSearchDialog` to pass tag-value selections to search
- [ ] 11.10 Update `MultiAddressSearchDialog` to pass tag-value selections to search
- [ ] 11.11 Update `DesktopPanel` + `TaggedTimerTask` to pass tag-value selections to `getTagged()` and persist value selections in user settings

## 12. REST API Tag Value Filtering
- [ ] 12.1 Extend `CasesEndpointV7.getCasesByTag()` (`/bytag/{tag}`) with optional `value` query parameter
- [ ] 12.2 Extend `CasesEndpointV7.getDocumentsByTag()` (`/documents/bytag/{tag}`) with optional `value` query parameter
- [ ] 12.3 Extend `ContactsEndpointV2.getContactsByTag()` (`/bytag/{tag}`) with optional `value` query parameter

## 13. Case History Text
- [ ] 13.1 Update `ArchiveFileService.setTag()` history text: include tag value for multi-value tags (`"Akten-Etikett gesetzt: TagName = Value"`)
- [ ] 13.2 Update `ArchiveFileService.setTag()` history text: show old→new value on value change (`"Akten-Etikett geändert: TagName = OldValue → NewValue"`)
- [ ] 13.3 Update `ArchiveFileService.setDocumentTagImpl()` history text: include tag value for multi-value document tags (`"Dokument-Etikett gesetzt: TagName = Value"`)
- [ ] 13.4 Update `AddressService.setTag()` history text: include tag value for multi-value contact tags (`"Adress-Etikett gesetzt: TagName = Value"`)
- [ ] 13.5 Boolean tag history text remains unchanged (no value suffix)

## 14. Tag Display in Lists and Tooltips
- [ ] 14.1 Update `TagUtils.getTagList()` to render multi-value tags as `"TagName: Value"` in comma-separated tag string
- [ ] 14.2 Update `TagUtils.getTagListTooltip()` to show multi-value tags as `"TagName: Value"` in HTML tooltip with timestamps
- [ ] 14.3 Update `TagUtils.getDocumentTagsOverviewAsHtml()` to show per-value counts for multi-value tags (e.g. `"Dokumentstatus: Final (3)"`)
- [ ] 14.4 Verify display in `QuickArchiveFileSearchThread` result rows, `LastChangedEntryPanelTransparent`, `TaggedEntryPanelTransparent`, `CaseGroupHeaderPanel`

## 15. Client Startup Caching
- [ ] 15.1 Add `getOptionGroupsByPrefix(String prefix)` method to `SystemManagementRemote` and `SystemManagementLocal`
- [ ] 15.2 Implement `getOptionGroupsByPrefix()` in `SystemManagement` EJB (query `server_options` WHERE `optionGroup LIKE prefix%`)
- [ ] 15.3 Update `SplashThread` to load multi-value tag definitions for all three entity types using `getOptionGroupsByPrefix()` and cache them
- [ ] 15.4 Update tag panel rendering and filter popup building to use cached multi-value tag definitions instead of per-use server calls

## 16. REST API for Tag Definitions
- [ ] 16.1 Add REST v7 endpoint `GET /rest/v7/configuration/tags/multivalue/{entityType}` returning multi-value tag definitions with available values
- [ ] 16.2 Implement endpoint by querying option groups with appropriate prefix (`archiveFile.tags.mv.`, `document.tags.mv.`, `address.tags.mv.`)
- [ ] 16.3 Return JSON array with tag name and values list per tag definition
- [ ] 16.4 Update `SystemManagementRemote` JavaDoc for `getOptionGroupsByPrefix()`

## 17. Automated Document Tag Rules
- [ ] 17.1 Add `tagValue` field to `DocumentTagRule` entity / configuration
- [ ] 17.2 Update `TagRulesDialog` to allow selecting a multi-value tag and a specific value from its dropdown; value selection mandatory when multi-value tag is chosen
- [ ] 17.3 Update tag rule matching logic to apply multi-value tag with the configured `tagValue` when the rule fires
- [ ] 17.4 Verify boolean tag rules remain unchanged (no `tagValue` set)
