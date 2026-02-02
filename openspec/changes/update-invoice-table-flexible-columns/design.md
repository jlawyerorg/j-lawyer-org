## Context

The template system replaces placeholders in Word (.docx) and LibreOffice (.odt) documents. For table placeholders like `{{BEL_TABELLE}}`, the entire table is generated programmatically with a fixed column structure. Template designers have no control over which columns appear or how cell content is composed.

This change introduces a second, complementary approach: the template designer builds the table layout themselves, using per-position placeholders that the server resolves row-by-row.

### Stakeholders
- Template designers (law firm staff)
- j-lawyer.org developers maintaining LibreOfficeAccess / MicrosoftOfficeAccess

## Goals / Non-Goals

**Goals:**
- Allow template designers to choose which invoice columns appear
- Allow combining multiple position fields in a single table cell
- Allow flexible tax summary rows (one row per tax rate)
- Maintain full backward compatibility with `{{BEL_TABELLE}}`
- Work in both LibreOffice (ODT) and Microsoft Office (DOCX) templates

**Non-Goals:**
- Changing the existing `{{BEL_TABELLE}}` behavior
- Adding new invoice data fields beyond what `InvoicePosition` already provides
- Modifying the `StyledCalculationTable` class or its styling capabilities
- Server-side column configuration (the template defines the layout)

## Decisions

### Decision 1: Template-defined table layout with per-position placeholders

The template designer creates a table in their .docx/.odt file with:
- **Row 0 (Header):** Free text column headers (no placeholders needed)
- **Row 1 (Template Row):** Cells containing `{{BELP_*}}` placeholders, optionally mixed with static text
- **Row 2+ (Optional Footer Rows):** Static summary content or scalar placeholders like `{{BEL_TOTAL}}`

The server detects a table as "flexible" when any cell contains a `{{BELP_*}}` pattern. It then:
1. Identifies the template row (the row containing `{{BELP_*}}` placeholders)
2. For each invoice position, clones the template row and resolves all `{{BELP_*}}` placeholders against that position's data
3. Removes the original template row
4. Leaves header and footer rows untouched (footer rows may contain regular scalar placeholders like `{{BEL_TOTAL}}` which are resolved separately)
5. Recalculates column widths based on actual resolved content (not placeholder text)

**Alternatives considered:**
- *New dedicated placeholder like `{{BEL_TABELLE_FLEX}}`*: Rejected — auto-detection from `{{BELP_*}}` presence is simpler and more intuitive. No need for an extra marker.
- *Server settings for column visibility*: Rejected — moves layout decisions away from the template designer, less flexible, harder to maintain.

### Decision 2: Tax rate row duplication

For tax summary rows, a row containing `{{BEL_UST_SATZ}}` or `{{BEL_UST_BETRAG}}` is duplicated for each distinct tax rate used in the invoice, similar to how position rows are duplicated.

Example template table:
```
| Nr.           | Leistung                                  | Betrag                         |
| {{BELP_NR}}   | {{BELP_NAME}}: {{BELP_BESCHR}}            | {{BELP_NETTO}} {{BEL_WHRG}}   |
|               | Netto                                     | {{BEL_SUM_NETTO}} {{BEL_WHRG}}  |
|               | USt. {{BEL_UST_SATZ}}%                    | {{BEL_UST_BETRAG}} {{BEL_WHRG}} |
|               | Zahlbetrag                                | {{BEL_TOTAL}} {{BEL_WHRG}}    |
```

Result (2 positions, 2 tax rates):
```
| Nr. | Leistung                          | Betrag         |
| 1   | Beratung: Erstberatung Mietrecht  | 190,00 EUR     |
| 2   | Kopien: 50 Seiten                 | 25,00 EUR      |
|     | Netto                             | 215,00 EUR     |
|     | USt. 7%                           | 1,75 EUR       |
|     | USt. 19%                          | 36,10 EUR      |
|     | Zahlbetrag                        | 252,85 EUR     |
```

### Decision 3: Per-position placeholder names

Short prefix `BELP_` (Beleg-Position) to keep placeholders compact in narrow template cells:

| Placeholder | Source | Description |
|---|---|---|
| `{{BELP_NR}}` | Running counter (1, 2, 3...) | Position number |
| `{{BELP_NAME}}` | `InvoicePosition.name` | Position name |
| `{{BELP_BESCHR}}` | `InvoicePosition.description` | Position description |
| `{{BELP_MENGE}}` | `InvoicePosition.units` | Quantity |
| `{{BELP_EINZEL}}` | `InvoicePosition.unitPrice` | Unit price |
| `{{BELP_UST}}` | `InvoicePosition.taxRate` | Tax rate (percentage) |
| `{{BELP_NETTO}}` | `units * unitPrice` | Net amount |

Summary placeholders:

| Placeholder | Description |
|---|---|
| `{{BEL_SUM_NETTO}}` | Sum of all net amounts |
| `{{BEL_UST_SATZ}}` | Tax rate (in a row duplicated per rate) |
| `{{BEL_UST_BETRAG}}` | Tax amount for that rate |

Note: `{{BEL_TOTAL}}` and `{{BEL_WHRG}}` already exist and can be used in footer rows.

### Decision 4: Styling

The flexible table inherits its visual formatting from the template document itself (fonts, borders, colors as defined in Word/LibreOffice). The server does not apply `StyledCalculationTable` styling to flexible tables — the template designer controls the appearance.

This contrasts with `{{BEL_TABELLE}}` where styling is applied programmatically from server settings.

### Decision 5: Number formatting

Per-position monetary and numeric values (`{{BELP_MENGE}}`, `{{BELP_EINZEL}}`, `{{BELP_NETTO}}`, `{{BEL_UST_BETRAG}}`, `{{BEL_SUM_NETTO}}`) are formatted according to the invoice language setting (DE/EN/FR/NL), consistent with how `{{BEL_TABELLE}}` formats its values today.

### Decision 6: Automatic column width adjustment

Template cells containing placeholders like `{{BELP_NR}}` are wider than the actual values (e.g. "1"). After all placeholder rows are expanded and resolved, the server SHALL recalculate column widths based on the actual cell content.

**ODT (LibreOffice):** Reuse the existing `calculateTextWidth()` approach from `LibreOfficeAccess.java:1744-1763` — iterate over all rows per column, measure the resolved text, and set each column to the maximum width found (plus padding).

**DOCX (MS Office):** Implement equivalent auto-width logic using Apache POI. Set `CTTblWidth` on each column to the calculated optimal width, or use `tbl:tblW` with `w:type="auto"` where supported.

This ensures that the final document has clean, tight column widths regardless of how wide the placeholder text was in the template.

### Decision 7: Empty-row setting does not apply

The server setting `plugins.global.tableproperties.table.emptyRows` does NOT apply to flexible tables. This setting is specific to the programmatic `{{BEL_TABELLE}}` generation. In flexible tables, the template designer controls the exact row structure — if they want empty separator rows, they add them explicitly in the template.

## Risks / Trade-offs

- **Template complexity**: Flexible tables require more template design effort than `{{BEL_TABELLE}}`. Mitigated by keeping `{{BEL_TABELLE}}` as the simple default and documenting the new approach.
- **Auto-detection edge case**: A table that accidentally contains text matching `{{BELP_*}}` would be misinterpreted. Risk is negligible in practice — these are deliberate placeholder patterns.
- **Styling responsibility shift**: With flexible tables, the template controls styling rather than server settings. This is intentional but means existing `plugins.global.tableproperties.*` settings do not apply. This should be documented.
- **Column width heuristics**: Auto-width calculation depends on font metrics and may not be pixel-perfect across all fonts. The existing `calculateTextWidth()` approach for `{{BEL_TABELLE}}` is already accepted as good enough.

## Migration Plan

- No migration needed: `{{BEL_TABELLE}}` continues to work unchanged
- New feature is opt-in: only activated when a template contains `{{BELP_*}}` placeholders
- Existing templates are unaffected
