# invoice-templates Specification

## Purpose
TBD - created by archiving change update-invoice-table-flexible-columns. Update Purpose after archive.
## Requirements
### Requirement: Flexible Invoice Table Columns
The template system SHALL support per-position placeholders (`{{BELP_NR}}`, `{{BELP_NAME}}`, `{{BELP_BESCHR}}`, `{{BELP_MENGE}}`, `{{BELP_EINZEL}}`, `{{BELP_UST}}`, `{{BELP_NETTO}}`) that can be placed inside table cells in Word (.docx) and LibreOffice (.odt) templates. When a table contains cells with `{{BELP_*}}` placeholders, the server SHALL duplicate the template row once per invoice position and resolve each placeholder against that position's data.

#### Scenario: Template with subset of columns
- **WHEN** a template table contains a row with cells `{{BELP_NR}}` and `{{BELP_NAME}}` and `{{BELP_NETTO}} {{BEL_WHRG}}` (3 columns, omitting quantity, unit price, and tax rate)
- **AND** the invoice has 2 positions
- **THEN** the generated document SHALL contain a table with 2 data rows (one per position), each having 3 columns with the resolved values

#### Scenario: Combined placeholders in single cell
- **WHEN** a template table cell contains `{{BELP_NR}}. {{BELP_NAME}}: {{BELP_BESCHR}}`
- **AND** position 1 has name "Beratung" and description "Erstberatung Mietrecht"
- **THEN** the generated cell content SHALL be `1. Beratung: Erstberatung Mietrecht`

#### Scenario: Backward compatibility with BEL_TABELLE
- **WHEN** a template uses the existing `{{BEL_TABELLE}}` placeholder
- **THEN** the server SHALL generate the fixed 6-column table with programmatic styling, identical to current behavior

### Requirement: Tax Rate Row Duplication
The template system SHALL support tax summary placeholders (`{{BEL_UST_SATZ}}`, `{{BEL_UST_BETRAG}}`). When a table row contains these placeholders, the server SHALL duplicate that row once per distinct tax rate used in the invoice and resolve each placeholder with the corresponding rate and amount.

#### Scenario: Multiple tax rates
- **WHEN** a template table contains a row with cells `USt. {{BEL_UST_SATZ}}%` and `{{BEL_UST_BETRAG}} {{BEL_WHRG}}`
- **AND** the invoice has positions with tax rates 7% and 19%
- **THEN** the generated document SHALL contain two tax summary rows: one for 7% and one for 19%, each showing the corresponding tax amount

#### Scenario: Single tax rate
- **WHEN** all invoice positions use the same tax rate (e.g. 19%)
- **THEN** only one tax summary row SHALL be generated

### Requirement: Net Total Summary Placeholder
The template system SHALL support a `{{BEL_SUM_NETTO}}` placeholder that resolves to the sum of all position net amounts (quantity x unit price). This placeholder can be used in table footer rows or anywhere in the document.

#### Scenario: Net total in table footer
- **WHEN** a template table contains a footer row with `{{BEL_SUM_NETTO}} {{BEL_WHRG}}`
- **AND** the invoice has positions with net amounts 190.00 and 25.00
- **THEN** the cell SHALL display `215,00 EUR` (formatted per invoice language)

### Requirement: Automatic Column Width Adjustment
After all placeholder rows in a flexible table are expanded and resolved, the server SHALL recalculate column widths based on the actual cell content. Each column width SHALL be set to the maximum text width found across all rows in that column, plus padding. This ensures columns are sized to fit the resolved values, not the placeholder text from the template.

#### Scenario: Narrow column with short values
- **WHEN** a template column contains `{{BELP_NR}}` (13 characters wide in the template)
- **AND** the resolved values are "1", "2", "3" (1 character each)
- **THEN** the column width in the generated document SHALL be based on the actual content width, not the placeholder width

#### Scenario: Wide column with long text
- **WHEN** a template column contains `{{BELP_NAME}}: {{BELP_BESCHR}}`
- **AND** the resolved values include long descriptions
- **THEN** the column width SHALL accommodate the longest resolved text

### Requirement: Language-Aware Number Formatting
All per-position monetary placeholders (`{{BELP_EINZEL}}`, `{{BELP_NETTO}}`, `{{BELP_MENGE}}`, `{{BEL_UST_BETRAG}}`, `{{BEL_SUM_NETTO}}`) SHALL be formatted according to the invoice language setting (DE, EN, FR, NL), consistent with the formatting used by `{{BEL_TABELLE}}`.

#### Scenario: German formatting
- **WHEN** the invoice language is DE
- **AND** a position has a net amount of 1234.50
- **THEN** `{{BELP_NETTO}}` SHALL resolve to `1.234,50`

#### Scenario: English formatting
- **WHEN** the invoice language is EN
- **AND** a position has a net amount of 1234.50
- **THEN** `{{BELP_NETTO}}` SHALL resolve to `1,234.50`

