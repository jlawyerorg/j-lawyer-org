# Change: Placeholder catalog with categories and readable labels

## Why

The placeholder lists in the post template editor (`EmailTemplatesPanel`) and the text block
editor (`EmailTextBlocksPanel`) show a flat, alphabetically sorted list of several hundred raw
keys such as `{{MANDANT_VORNAME}}`, `{{BEL_ABSUSTIDNR}}` or `{{###_AGRAD1}}`-derived keys. Finding
the right one requires knowing the key. Placeholders have no readable label today, and the list
also offers placeholders that never resolve in an e-mail (invoice, invoice position and time
sheet placeholders).

## What Changes

- **Readable labels are established, directly in the code.** Every placeholder constant in
  `PlaceHolders` gets a runtime annotation `@PlaceHolderInfo(category = ..., label = "...")`, e.g.
  `{{MANDANT_VORNAME}}` → "Mandant: Vorname", `{{AKTE_ZEICHEN}}` → "Akte: Aktenzeichen",
  `{{USER_TEL}}` → "Benutzer: Telefon". Label and category are maintained in the line above the
  constant; the constants stay `String`s, so no existing code changes. Party placeholders are
  labelled once on their generic constant (`_VORNAME` → "Vorname") and combined with the party
  type's name. No properties files are introduced; labels are German only for now.
- **Categories.** A new enum `PlaceHolderCategory` (Akte, Beteiligte, Falldaten, Benutzer,
  Kanzlei, Datum und Dokument, Rechnung, Rechnungsposition, Zeiterfassung, Ingo, E-Mail) and a
  new `PlaceHolderCatalog` in j-lawyer-server-common that reads the annotations once via
  reflection and describes each placeholder with key, category, sub-category (party type / form)
  and label.
- **Context.** The catalog is requested for a usage context. The e-mail context (post templates,
  text blocks) leaves out categories that cannot be resolved there (Rechnung,
  Rechnungsposition, Zeiterfassung, table placeholders) and adds the e-mail-only placeholders
  `{{CURSOR}}` and `{{CLOUD_LINK}}`, which are not offered at all today.
- **Aliases.** Alias placeholders (`###_FIRMA`, `###_TITEL`, `###_ANREDE`) are marked with
  `aliasOf` in their annotation. They stay resolvable but are not offered in the picker; the
  canonical placeholder is shown instead.
- **E-mail placeholders become constants.** `{{CURSOR}}` (today `EmailTemplate.PLACEHOLDER_CURSOR`
  and a private constant in `SendBeaMessageFrame`) and `{{CLOUD_LINK}}` (today a string literal in
  `SendEmailFrame`) are declared as annotated constants in `PlaceHolders`; the existing usages
  refer to them.
- **Cloud link in beA messages.** beA messages have no cloud link. In `SendBeaMessageFrame`,
  `{{CLOUD_LINK}}` in a post template or text block is replaced by an empty string, so it never
  appears literally. Its label says "Cloud-Link (nur E-Mail)".
- **Label draft.** The proposed labels and categories of all placeholders are listed in
  `labels.md` for review before implementation.
- **Reusable picker.** A new Swing component `PlaceHolderPickerPanel` (+ `.form`) replaces the
  placeholder lists of both panels: search field (matches label and key), tree with categories
  and sub-categories, entries shown with their label and the key as secondary text/tooltip,
  insert by double click, Enter or the existing insert button. The inserted text is still the
  raw key.
- `PlaceHolders.getAllPlaceHolders` stays unchanged; document generation and placeholder
  resolution are not affected.

## Impact

- Affected specs: `placeholder-catalog` (new)
- New code:
  - j-lawyer-server-common: `PlaceHolderInfo` (annotation), `PlaceHolderCategory`,
    `PlaceHolderContext`, `PlaceHolderDescriptor`, `PlaceHolderCatalog`
  - j-lawyer-client: `PlaceHolderPickerPanel` (+ `.form`)
- Modified code: `PlaceHolders` (annotations on all constants, new `CURSOR` / `CLOUD_LINK`
  constants), `EmailTemplate`, `SendEmailFrame`, `SendBeaMessageFrame` (use the new constants),
  `EmailTemplatesPanel` (+ `.form`), `EmailTextBlocksPanel` (+ `.form`)
- No server API change, no database change, no breaking changes.
