# Design: Placeholder catalog

## Context

Placeholders are string constants in `PlaceHolders` (j-lawyer-server-common). The list offered
in the editors is built by `PlaceHolders.getAllPlaceHolders(partyTypePlaceHolders,
formPlaceHolders)`:
- fixed placeholders (`PROFIL_*`, `AKTE_*`, `USER_*`, `BEL_*`, `BELP_*`, `KURZDATUM`, ...),
- party placeholders generated per party type from generic templates (`{{###_VORNAME}}` with
  `###` replaced by `PartyTypeBean.getPlaceHolder()`, e.g. `MANDANT`); the party type also has a
  readable `name` (e.g. "Mandant"),
- form placeholders of the case (`FormsServiceRemote.getPlaceHoldersForCase`), whose form
  (`ArchiveFileFormsBean`) has a `description`.

There are no labels and no grouping. The same list is used for documents and e-mails, although
invoice and time sheet placeholders only resolve when generating invoice/time sheet documents.

## Goals / Non-Goals

Goals:
- A readable label and a category for every placeholder offered in the e-mail editors.
- Fast picking: grouping plus search.
- One place to maintain labels, reusable by other pickers later.

Non-Goals:
- Changing placeholder keys, resolution or document generation.
- Using labels inside templates (templates keep raw keys).
- Pickers for document templates, the web client or REST (can use the catalog later).
- User-defined labels.

## Decisions

### Labels as annotations on the constants
Labels and categories are maintained directly in `PlaceHolders.java`, one annotation above each
constant. Properties files were rejected: as long as the client is not consistently
internationalised, a second file is harder to maintain than a line next to the constant.

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface PlaceHolderInfo {
    PlaceHolderCategory category();
    String label();
    String aliasOf() default "";   // set: alias of another placeholder, not offered
}

@PlaceHolderInfo(category = PlaceHolderCategory.AKTE, label = "Aktenzeichen")
public static final String AKTE_ZEICHEN = "{{AKTE_ZEICHEN}}";

@PlaceHolderInfo(category = PlaceHolderCategory.BETEILIGTE, label = "Vorname")
public static final String _VORNAME = "{{###_VORNAME}}";

@PlaceHolderInfo(category = PlaceHolderCategory.BETEILIGTE, label = "Firma", aliasOf = _UNTERNEHMEN)
public static final String _FIRMA_ALIAS = "{{###_FIRMA}}";
```

- The constants remain `String`s. `PlaceHolderServerUtils` (which extends `PlaceHolders`) and all
  other usages are unaffected. Replacing the constants by an enum was rejected for this reason.
- Labels are short field names; the category / sub-category supplies the context. The combined
  form "Mandant: Vorname" is built for search results and tooltips.
- Labels are German. If the client is internationalised later, `label` can become a message key
  without changing the structure.
- The category is explicit per constant, so no prefix rules are needed and special cases
  (`KURZDATUM`, `DOK_DZ`, `INGO_TEXT`, `TABELLE_1`) are handled like any other constant.
- Placeholders without annotation fall back to their key in the picker. A unit test iterates
  over all `public static final String` fields of `PlaceHolders` whose value is a placeholder
  (`{{...}}`) and fails for a field without `@PlaceHolderInfo`, and for an `aliasOf` that does
  not name another placeholder.

### Catalog API (j-lawyer-server-common)

```java
public enum PlaceHolderCategory { AKTE, BETEILIGTE, FALLDATEN, BENUTZER, KANZLEI,
    DATUM_DOKUMENT, EMAIL, INGO, RECHNUNG, RECHNUNGSPOSITION, ZEITERFASSUNG,
    SONSTIGE;                       // fallback for constants without annotation
    String getDisplayName(); }      // "Akte", "Beteiligte", ...

public enum PlaceHolderContext { EMAIL, DOCUMENT }

public class PlaceHolderDescriptor implements Serializable {
    String key;          // {{MANDANT_VORNAME}}
    PlaceHolderCategory category;
    String subCategory;  // "Mandant" (party type) / form description, may be null
    String label;        // "Vorname"
    String getDisplayName();  // "Mandant: Vorname"
}

public class PlaceHolderCatalog {
    static List<PlaceHolderDescriptor> getPlaceHolders(PlaceHolderContext ctx,
            Map<String, String> partyTypes /* prefix -> name, in display order */,
            Map<String, String> formPlaceHolders /* key -> form name */);
}
```

`PlaceHolderCatalog` reads the annotations of `PlaceHolders` once via reflection
(`PlaceHolders.class.getFields()`) and caches them. Generic party constants (`###` in the value)
are expanded per party type with the party type's name as sub-category. Form placeholders get
category Falldaten with the form's description as sub-category and the field part of the key as
label. The result is sorted by category (enum order), sub-category (party types in the given
order, forms alphabetically) and label (German collation), since `getFields()` does not guarantee
declaration order.

Party types are passed as a map of placeholder prefix to name rather than as `PartyTypeBean`s,
because j-lawyer-server-common does not depend on j-lawyer-server-entities. A placeholder constant
without annotation is listed under "Sonstige" with its key as label.

The catalog is built on the client from data it already has (party types, form placeholders); no
new server call is needed.

### E-mail placeholders as constants
`{{CURSOR}}` and `{{CLOUD_LINK}}` are added to `PlaceHolders` with category EMAIL.
`EmailTemplate.PLACEHOLDER_CURSOR` refers to `PlaceHolders.CURSOR` (kept for compatibility),
the private constant in `SendBeaMessageFrame` and the literal in `SendEmailFrame` are replaced.
They are not added to `getAllPlaceHolders`, so document generation is unchanged.

### Context filtering

| Category           | EMAIL | DOCUMENT |
|--------------------|-------|----------|
| Akte, Beteiligte, Falldaten, Benutzer, Kanzlei, Datum und Dokument, Ingo | yes | yes |
| Rechnung, Rechnungsposition, Zeiterfassung, table placeholders | no | yes |
| E-Mail (`{{CURSOR}}`, `{{CLOUD_LINK}}`) | yes | no |

### Aliases
`###_FIRMA` (alias of `###_UNTERNEHMEN`), `###_TITEL` (alias of `###_ANREDE1`) and `###_ANREDE`
(alias of `###_BEGRUESSUNG`) are not offered; existing templates using them keep working.

### Picker component
`PlaceHolderPickerPanel` (client, `com.jdimension.jlawyer.client.editors` or `...mail`):
- search field on top; while empty, a `JTree` with categories → sub-categories → placeholders,
  categories collapsed except the last used one; while searching, a flat filtered list of
  matches showing "Mandant: Vorname" (match on label, display name and key, case-insensitive),
- a renderer showing the label, the key in a smaller grey font next to it and the key as tooltip,
- multi-selection like today,
- `getSelectedPlaceHolders()` returning the raw keys and an "insert requested" listener fired on
  double click / Enter, so the hosting panel decides where to insert (subject vs. body,
  text vs. HTML tab) with the existing insert logic.

Both panels replace `jScrollPane3`/`lstPlaceHolders` with the picker in their `.form`;
`EmailTemplatesPanel` keeps its target combo box ("in Betreff" / "in Inhalt").

## Risks / Trade-offs

- **Label maintenance.** About 170 annotations must be written once; afterwards a label is
  maintained in the line above its constant. The unit test and the key fallback limit the risk.
- **Reflection.** The catalog depends on field annotations at runtime; the annotation has
  `RUNTIME` retention and the result is cached, so the cost is a one-time scan of one class.
- **Context filtering hides placeholders.** A user who used an invoice placeholder in an e-mail
  template no longer finds it in the picker; it never resolved there, so nothing breaks.
- **Party type names are user-defined** and may be long; sub-categories use them as they are.

## Migration Plan

None; no data or schema changes.

### Cloud link in beA messages
Post templates and text blocks are shared by e-mail and beA, so the picker cannot hide
`{{CLOUD_LINK}}` depending on the later use. Instead `SendBeaMessageFrame` puts an empty value for
`PlaceHolders.CLOUD_LINK` into the resolved placeholder values (templates and text blocks), and
the label marks it as e-mail only.

## Open Questions

None. The labels and categories in `labels.md` are agreed; `AKTE_NR` and `INGO_TEXT` stay
offered, party placeholders are listed without an additional grouping level, and document
templates get the catalog in a separate change.
