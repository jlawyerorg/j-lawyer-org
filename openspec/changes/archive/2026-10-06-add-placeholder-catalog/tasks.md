## 1. Catalog (j-lawyer-server-common)

- [x] 1.1 Create annotation `PlaceHolderInfo` (category, label, aliasOf; RUNTIME, FIELD) and
      enum `PlaceHolderCategory` with display names
- [x] 1.2 Review `labels.md` and resolve the ❓ items
- [x] 1.3 Annotate all placeholder constants in `PlaceHolders` (fixed and generic `###_`
      constants, aliases with `aliasOf`); add annotated constants `CURSOR` and `CLOUD_LINK`
      (category EMAIL, not part of `getAllPlaceHolders`); also `TemplatesEndpointV6` uses
      `PlaceHolders.CLOUD_LINK`
- [x] 1.4 Use the new constants: `EmailTemplate.PLACEHOLDER_CURSOR` refers to
      `PlaceHolders.CURSOR`; replace the private constant in `SendBeaMessageFrame` and the
      `{{CLOUD_LINK}}` literal in `SendEmailFrame`; `SendBeaMessageFrame` resolves
      `{{CLOUD_LINK}}` to an empty string for templates and text blocks
- [x] 1.5 Create `PlaceHolderContext`, `PlaceHolderDescriptor` and `PlaceHolderCatalog` (cached
      reflection over the annotations, party type and form sub-categories, alias exclusion,
      context filtering, sorting)
- [x] 1.6 Unit tests: every placeholder constant of `PlaceHolders` has `@PlaceHolderInfo`;
      every `aliasOf` names another placeholder;
      EMAIL context excludes invoice/time sheet placeholders and includes `{{CURSOR}}` /
      `{{CLOUD_LINK}}`; aliases excluded; party/form sub-categories

## 2. Client picker

- [x] 2.1 Create `PlaceHolderPickerPanel` (+ `.form`): search field, category tree, flat search
      result list, renderer with label + key, multi-selection, insert listener on double click
      / Enter
- [x] 2.2 `EmailTemplatesPanel` (+ `.form`): replace the placeholder list with the picker (EMAIL
      context), keep target combo and insert button
- [x] 2.3 `EmailTextBlocksPanel` (+ `.form`): replace the placeholder list with the picker
      (EMAIL context), keep insert button

## 3. Validation

- [x] 3.1 Both panels: categories and party type sub-categories shown; search "vorname" lists
      the first names of all party types and the user
- [x] 3.2 Double click / Enter / button insert the raw key at the cursor
- [x] 3.3 Invoice and time sheet placeholders are not offered; `{{CURSOR}}` and `{{CLOUD_LINK}}`
      are offered
- [x] 3.4 beA: a template / text block containing `{{CLOUD_LINK}}` leaves no placeholder in
      the message
- [x] 3.5 Existing templates with alias placeholders still resolve
- [x] 3.6 Run `openspec validate add-placeholder-catalog --strict`
