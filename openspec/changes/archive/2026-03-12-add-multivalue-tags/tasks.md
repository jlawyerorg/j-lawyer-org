## 1. Database Migration & Entity Changes
- [x] 1.1 Create SQL migration script adding `tag_value VARCHAR(250) DEFAULT NULL` to `case_tags`, `document_tags`, and `contact_tags`
- [x] 1.2 Add `tagValue` field with `@Column(name = "tag_value")` to `ArchiveFileTagsBean`
- [x] 1.3 Add `tagValue` field with `@Column(name = "tag_value")` to `DocumentTagsBean`
- [x] 1.4 Add `tagValue` field with `@Column(name = "tag_value")` to `AddressTagsBean`

## 2. Configuration Constants
- [x] 2.1 Add `OPTIONGROUP_ARCHIVEFILETAGS_MV_PREFIX = "archiveFile.tags.mv."` to `OptionConstants`
- [x] 2.2 Add `OPTIONGROUP_DOCUMENTTAGS_MV_PREFIX = "document.tags.mv."` to `OptionConstants`
- [x] 2.3 Add `OPTIONGROUP_ADDRESSTAGS_MV_PREFIX = "address.tags.mv."` to `OptionConstants`

## 3. EJB Service Changes
- [x] 3.1 Update `ArchiveFileService.setTag()`: when `active=true` and tag row already exists, update `tag_value` on the existing row instead of skipping; persist `tagValue` for new rows
- [x] 3.2 Update `ArchiveFileService.setDocumentTagImpl()`: same update-on-exists logic for `tag_value`
- [x] 3.3 Update `AddressService.setTag()`: same update-on-exists logic for `tag_value`
- [x] 3.4 Update `ArchiveFileServiceRemote` JavaDoc for changed tag methods
- [x] 3.5 Update `AddressServiceRemote` JavaDoc for changed tag methods

## 4. Webhook Event Enrichment
- [x] 4.1 Add `tagValue` field to `CaseTagChangedEvent`, `DocumentTagChangedEvent`, and `AddressTagChangedEvent`, include in JSON serialization
- [x] 4.2 Set `tagValue` on event when firing from `setTag` / `setDocumentTagImpl` / `AddressService.setTag`
- [x] 4.3 Single event per value change: `active=true` with new value (no deactivate+activate pair)

## 5. Backward Compatibility Verification
- [x] 5.1 Verify REST responses only add `tagValue` field, no existing fields removed or renamed
- [x] 5.2 Verify REST requests without `tagValue` create boolean tags (`tag_value=NULL`)
- [x] 5.3 Verify webhook JSON payloads retain all existing fields and only add `tagValue`
- [x] 5.4 Verify EJB `setTag()` with `tagValue=null` on tag bean behaves as boolean tag (no regression)
- [x] 5.5 Verify `/bytag/{tag}` without `value` query parameter returns all items with that tag

## 6. REST API Extensions
- [x] 6.1 Add nullable `tagValue` field to shared `RestfulTagV1` POJO
- [x] 6.2 Update `CasesEndpointV1` tag population code to set `tagValue` from entity bean
- [x] 6.3 Update `CasesEndpointV5` tag population code to set `tagValue` from entity bean
- [x] 6.4 Update `ContactsEndpointV5` tag population code to set `tagValue` from entity bean
- [x] 6.5 Update REST v7 case tag endpoints to accept and return `tagValue`
- [x] 6.6 Update REST v7 document tag endpoints to accept and return `tagValue`
- [x] 6.7 Update REST v7 contact tag endpoints to accept and return `tagValue`

## 7. Client Configuration Dialog
- [x] 7.1 Create new `MultiValueTagConfigurationDialog` with master-detail layout (left: tag names list, right: values list for selected tag)
- [x] 7.2 Left side: add/rename/delete controls for multi-value tag names; creating a tag auto-selects it
- [x] 7.3 Right side: add/rename/delete controls for values within the selected tag; disabled when no tag selected
- [x] 7.4 Validate that multi-value tag names do not collide with boolean tag names
- [x] 7.5 On tag rename: prompt user whether to update `tagName` in existing tag rows; rename option group
- [x] 7.6 On tag delete: prompt user whether to remove existing tag rows with that `tagName`; remove option group
- [x] 7.7 On value rename: prompt user whether to update `tag_value` in existing tag rows
- [x] 7.8 On value delete: prompt user whether to remove existing tag rows with that `tag_value`
- [x] 7.9 Add menu items in `JKanzleiGUI` to open `MultiValueTagConfigurationDialog` for case, document, and address multi-value tags

## 8. Client Tag Display
- [x] 8.1 Update `TagUtils` to discover multi-value tags by querying option groups matching `*.tags.mv.*` prefix and extracting tag names from group suffixes
- [x] 8.2 Render multi-value tags as `MultiValueTag` components in the case tag panel (`ArchiveFilePanel`)
- [x] 8.3 Render multi-value tags as `MultiValueTag` components in the contact tag panel (`AddressPanel`)
- [x] 8.4 Render multi-value tags as `MultiValueTag` components in the document tag area
- [x] 8.5 Wire `MultiValueTag` combo box action listener to call `setTag` / `setDocumentTag` with `tagValue`
- [x] 8.6 Populate `MultiValueTag` combo box selection from existing tag row's `tagValue` when loading entity

## 9. Server-Side Rename/Remove Support
- [x] 9.1 Add server-side method to rename `tagName` in tag rows across `case_tags`, `document_tags`, `contact_tags`
- [x] 9.2 Add server-side method to rename `tag_value` in tag rows matching a given `tagName`
- [x] 9.3 Add server-side method to delete tag rows matching a given `tagName` and `tag_value`
- [x] 9.4 Add server-side method to delete tag rows matching a given `tagName` (for full tag deletion)

## 10. Server-Side Tag Value Filtering
- [x] 10.1 Extend `ArchiveFileService.searchEnhanced()` to accept optional tag-value map and add `tag_value IN (...)` clause when values are provided
- [x] 10.2 Extend `ArchiveFileService.searchTagsEnhanced()` to support tag-value filtering
- [x] 10.3 Extend `ArchiveFileService.getTagged()` to support tag-value filtering
- [x] 10.4 Extend `AddressService.searchEnhanced()` to accept optional tag-value map
- [x] 10.5 Extend `AddressService.searchTagsEnhanced()` to support tag-value filtering
- [x] 10.6 Update `ArchiveFileServiceRemote` and `AddressServiceRemote` JavaDoc for extended search signatures

## 11. Client Tag Filter Popup Menus
- [x] 11.1 Update `TagUtils.populateTags()` to render multi-value tag values as flat `JCheckBoxMenuItem` entries with display text `"TagName: Value"` and client properties `tagName`/`tagValue` for metadata
- [x] 11.2 Update `TagUtils.getSelectedTags()` to read `tagName`/`tagValue` client properties from menu items (never parse display label) and return a structure that conveys value selections
- [x] 11.3 Update `QuickArchiveFileSearchPanel` to pass tag-value selections to search (case tag + document tag filters)
- [x] 11.4 Update `QuickAddressSearchPanel` to pass tag-value selections to search (address tag filter)
- [x] 11.5 Update `SearchAndAssignDialog` to pass tag-value selections to search (case + document tag filters)
- [x] 11.6 Update `ArchiveFilePanel` document tag filter to support multi-value tag value filtering
- [x] 11.7 Update `AddBeaRecipientSearchDialog` to pass tag-value selections to search
- [x] 11.8 Update `AddRecipientSearchDialog` to pass tag-value selections to search
- [x] 11.9 Update `AddAddressSearchDialog` to pass tag-value selections to search
- [x] 11.10 Update `MultiAddressSearchDialog` to pass tag-value selections to search
- [x] 11.11 Update `DesktopPanel` + `TaggedTimerTask` to pass tag-value selections to `getTagged()` and persist value selections in user settings

## 12. REST API Tag Value Filtering
- [x] 12.1 Extend `CasesEndpointV7.getCasesByTag()` (`/bytag/{tag}`) with optional `value` query parameter
- [x] 12.2 Extend `CasesEndpointV7.getDocumentsByTag()` (`/documents/bytag/{tag}`) with optional `value` query parameter
- [x] 12.3 Extend `ContactsEndpointV2.getContactsByTag()` (`/bytag/{tag}`) with optional `value` query parameter

## 13. Case History Text
- [x] 13.1 Update `ArchiveFileService.setTag()` history text: include tag value for multi-value tags (`"Akten-Etikett gesetzt: TagName = Value"`)
- [x] 13.2 Update `ArchiveFileService.setTag()` history text: show old→new value on value change (`"Akten-Etikett geändert: TagName = OldValue → NewValue"`)
- [x] 13.3 Update `ArchiveFileService.setDocumentTagImpl()` history text: include tag value for multi-value document tags (`"Dokument-Etikett gesetzt: TagName = Value"`)
- [x] 13.4 Update `AddressService.setTag()` history text: include tag value for multi-value contact tags (`"Adress-Etikett gesetzt: TagName = Value"`)
- [x] 13.5 Boolean tag history text remains unchanged (no value suffix)

## 14. Tag Display in Lists and Tooltips
- [x] 14.1 Update `TagUtils.getTagList()` to render multi-value tags as `"TagName: Value"` in comma-separated tag string
- [x] 14.2 Update `TagUtils.getTagListTooltip()` to show multi-value tags as `"TagName: Value"` in HTML tooltip with timestamps
- [x] 14.3 Update `TagUtils.getDocumentTagsOverviewAsHtml()` to show per-value counts for multi-value tags (e.g. `"Dokumentstatus: Final (3)"`)
- [x] 14.4 Verify display in `QuickArchiveFileSearchThread` result rows, `LastChangedEntryPanelTransparent`, `TaggedEntryPanelTransparent`, `CaseGroupHeaderPanel`

## 15. Client Startup Caching
- [x] 15.1 Add `getOptionGroupsByPrefix(String prefix)` method to `SystemManagementRemote` and `SystemManagementLocal`
- [x] 15.2 Implement `getOptionGroupsByPrefix()` in `SystemManagement` EJB (query `server_options` WHERE `optionGroup LIKE prefix%`)
- [x] 15.3 Update `SplashThread` to load multi-value tag definitions for all three entity types using `getOptionGroupsByPrefix()` and cache them
- [x] 15.4 Update tag panel rendering and filter popup building to use cached multi-value tag definitions instead of per-use server calls

## 16. REST API for Tag Definitions
- [x] 16.1 Add REST v7 endpoint `GET /rest/v7/configuration/tags/multivalue/{entityType}` returning multi-value tag definitions with available values
- [x] 16.2 Implement endpoint by querying option groups with appropriate prefix (`archiveFile.tags.mv.`, `document.tags.mv.`, `address.tags.mv.`)
- [x] 16.3 Return JSON array with tag name and values list per tag definition
- [x] 16.4 Update `SystemManagementRemote` JavaDoc for `getOptionGroupsByPrefix()`

## 17. Automated Document Tag Rules
- [x] 17.1 Add `tagValue` field to `DocumentTagRule` entity / configuration
- [x] 17.2 Update `TagRulesDialog` to allow selecting a multi-value tag and a specific value from its dropdown; value selection mandatory when multi-value tag is chosen
- [x] 17.3 Update tag rule matching logic to apply multi-value tag with the configured `tagValue` when the rule fires
- [x] 17.4 Verify boolean tag rules remain unchanged (no `tagValue` set)

## Testplan

### Konfiguration
- [x] Listenetikett für Akten anlegen (z.B. "Status" mit Werten "offen", "in Arbeit", "erledigt")
- [x] Listenetikett für Dokumente anlegen (z.B. "Dokumentstatus" mit Werten "Entwurf", "Final")
- [x] Listenetikett für Adressen anlegen (z.B. "Kategorie" mit Werten "Mandant", "Gegner", "Zeuge")
- [x] Doppelpunkt in Tag-Name wird abgelehnt
- [x] Doppelpunkt in Wert wird abgelehnt
- [x] Name, der bereits als Ja/Nein-Etikett existiert, wird abgelehnt
- [x] Doppelter Name wird abgelehnt
- [x] Doppelter Wert wird abgelehnt
- [x] Tag umbenennen (Kontextmenü): Option-Group wird umbenannt, optional bestehende Zuordnungen
- [x] Tag löschen (Kontextmenü): Option-Group wird entfernt, optional bestehende Zuordnungen
- [x] Wert umbenennen (Kontextmenü): Wert in Option-Group wird umbenannt, optional bestehende Zuordnungen
- [x] Wert löschen (Kontextmenü): Wert in Option-Group wird entfernt, optional bestehende Zuordnungen
- [x] Cache wird nach jeder Änderung aktualisiert (Änderungen sofort in Panels sichtbar ohne Neustart)

### Akten-Tags
- [x] Listenetikett-Dropdown erscheint im Akten-Tag-Panel (vor den Ja/Nein-Etiketten)
- [x] Wert auswählen setzt Tag mit tag_value auf dem Server
- [x] Wert auf leer zurücksetzen entfernt den Tag
- [x] Wert ändern aktualisiert tag_value (kein zweiter Eintrag)
- [x] Gespeicherter Wert wird beim Laden der Akte korrekt angezeigt
- [x] Historientext zeigt "Akten-Etikett gesetzt: TagName = Value"
- [x] Historientext zeigt "Akten-Etikett geändert: TagName = AltWert → NeuWert" bei Wertänderung

### Dokument-Tags
- [x] Listenetikett-Dropdown erscheint im Dokument-Tag-Panel (vor den Ja/Nein-Etiketten)
- [x] Wert auswählen/ändern/entfernen funktioniert wie bei Akten-Tags
- [x] Historientext zeigt "Dokument-Etikett gesetzt: TagName = Value"

### Adress-Tags
- [x] Listenetikett-Dropdown erscheint im Adress-Tag-Panel (vor den Ja/Nein-Etiketten)
- [x] Wert auswählen/ändern/entfernen funktioniert wie bei Akten-Tags
- [x] Historientext zeigt "Adress-Etikett gesetzt: TagName = Value"

### Suche & Filter
- [x] Aktensuche: Listenetikett-Werte erscheinen als "TagName: Value" im Filtermenü
- [x] Aktensuche: Filtern nach einem Wert liefert nur Akten mit diesem Wert
- [x] Dokumentsuche: Listenetikett-Werte erscheinen im Filtermenü
- [x] Dokumentsuche: Filtern nach einem Wert liefert nur passende Dokumente
- [x] Adresssuche: Listenetikett-Werte erscheinen im Filtermenü
- [x] Adresssuche: Filtern nach einem Wert liefert nur passende Adressen
- [x] SearchAndAssignDialog: Akten- und Dokument-Filter funktionieren mit Listenetiketten
- [x] AddBeaRecipientSearchDialog: Adress-Filter funktioniert mit Listenetiketten
- [x] AddRecipientSearchDialog: Adress-Filter funktioniert mit Listenetiketten
- [x] AddAddressSearchDialog: Adress-Filter funktioniert mit Listenetiketten
- [x] MultiAddressSearchDialog: Adress-Filter funktioniert mit Listenetiketten

### Desktop-Panel (Abonnierte Etiketten)
- [x] Listenetikett-Werte erscheinen als "TagName: Value" im Akten-Tag-Filtermenü
- [x] Listenetikett-Werte erscheinen als "TagName: Value" im Dokument-Tag-Filtermenü
- [x] Abonnierter Wert erzeugt Tab mit Titel "TagName: Value"
- [x] Tab enthält nur Akten/Dokumente mit exakt diesem Wert
- [x] Boolean-Tags erzeugen weiterhin Tabs mit einfachem Tag-Namen
- [x] Filter-Auswahl wird über Neustart hinweg gespeichert

### Anzeige in Listen und Tooltips
- [x] Tag-Liste in Suchergebnissen zeigt "TagName: Value" für Listenetiketten
- [x] Tooltip zeigt "TagName: Value" mit Zeitstempel
- [x] Dokument-Tags-Übersicht zeigt Wert-Anzahlen (z.B. "Dokumentstatus: Final (3)")
- [x] Zuletzt geänderte Akten: Tags korrekt angezeigt
- [x] Wiedervorlagen: Tags korrekt angezeigt

### Ja/Nein-Etiketten (Regressionstests)
- [x] Ja/Nein-Etiketten für Akten funktionieren weiterhin (setzen, entfernen)
- [x] Ja/Nein-Etiketten für Dokumente funktionieren weiterhin
- [x] Ja/Nein-Etiketten für Adressen funktionieren weiterhin
- [x] Historientext für Ja/Nein-Etiketten unverändert (kein Wert-Suffix)
- [x] Suche/Filter mit Ja/Nein-Etiketten funktioniert weiterhin

### REST API
- [x] GET `/rest/v7/configuration/tags/multivalue/case` liefert Akten-Listenetiketten mit Werten
- [x] GET `/rest/v7/configuration/tags/multivalue/document` liefert Dokument-Listenetiketten
- [x] GET `/rest/v7/configuration/tags/multivalue/address` liefert Adress-Listenetiketten
- [x] GET `/rest/v7/configuration/tags/multivalue/invalid` liefert 400 Bad Request
- [x] Tag-Endpunkte (v1, v5, v7) liefern `tagValue` in der Antwort
- [x] GET `/bytag/{tag}` ohne `value`-Parameter liefert alle Akten/Kontakte mit diesem Tag
- [x] GET `/bytag/{tag}?value=X` liefert nur Akten/Kontakte mit tag_value=X

### Webhooks
- [x] CaseTagChangedEvent enthält `tagValue` bei Listenetiketten
- [x] DocumentTagChangedEvent enthält `tagValue` bei Listenetiketten
- [x] AddressTagChangedEvent enthält `tagValue` bei Listenetiketten
- [x] Events für Ja/Nein-Etiketten enthalten kein/null `tagValue`

### Etiketten-Automatik (Dokument-Tag-Regeln)
- [x] Regel mit Ja/Nein-Etikett funktioniert wie bisher
- [x] Regel mit Listenetikett im Format "TagName=Wert" setzt Tag mit tag_value
- [x] Validierung: Listenetikett ohne "=Wert" wird beim Speichern abgelehnt
- [x] Regel mit gemischten Etiketten (boolean + multi-value) funktioniert
