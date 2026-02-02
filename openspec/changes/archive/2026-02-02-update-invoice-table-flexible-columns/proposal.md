# Change: Flexible Column Structure for Invoice Table Placeholder

## Why
The `{{BEL_TABELLE}}` placeholder currently generates a fixed 6-column table (Index, Position, Quantity, Unit Price, Tax Rate, Amount). Template designers cannot remove unwanted columns (e.g. hide Tax Rate for tax-exempt invoices) or combine multiple data fields into a single column (e.g. "1. Service Name: Detailed description" in one cell). This limits the flexibility of invoice templates significantly.

## What Changes
- Introduce new per-position placeholders (`{{BELP_NR}}`, `{{BELP_NAME}}`, `{{BELP_BESCHR}}`, `{{BELP_MENGE}}`, `{{BELP_EINZEL}}`, `{{BELP_UST}}`, `{{BELP_NETTO}}`) that can be used inside a template-defined table
- Introduce per-tax-rate placeholders (`{{BEL_UST_SATZ}}`, `{{BEL_UST_BETRAG}}`) for flexible tax summary rows
- Template designers create their own table layout in the Word/LibreOffice template with a "template row" containing these placeholders — the server duplicates this row for each invoice position
- Placeholders can be freely combined with static text in a single cell (e.g. `{{BELP_NR}}. {{BELP_NAME}}: {{BELP_BESCHR}}`)
- Columns are made optional by simply not including them in the template
- After row expansion, column widths are automatically recalculated based on actual content to ensure optimal layout
- `{{BEL_TABELLE}}` continues to work unchanged for backward compatibility
- New placeholders are populated in `PlaceHolderServerUtils` and resolved in `LibreOfficeAccess` / `MicrosoftOfficeAccess`

## Impact
- Affected specs: invoice-templates (new capability spec)
- Affected code:
  - `j-lawyer-server-common`: `PlaceHolders.java` (new placeholder constants), `StyledCalculationTable.java` (row-level placeholder data)
  - `j-lawyer-server-ejb`: `LibreOfficeAccess.java` (new table expansion logic for ODT, auto column widths), `MicrosoftOfficeAccess.java` (new table expansion logic for DOCX, auto column widths), `PlaceHolderServerUtils.java` (populate new per-position placeholder values)
  - `j-lawyer-client`: `InvoiceDialog.java` (pass per-position data to placeholder system)
