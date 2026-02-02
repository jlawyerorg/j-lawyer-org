## 1. New Placeholder Constants
- [ ] 1.1 Add per-position placeholder constants to `PlaceHolders.java` (`BELP_NR`, `BELP_NAME`, `BELP_BESCHR`, `BELP_MENGE`, `BELP_EINZEL`, `BELP_UST`, `BELP_NETTO`)
- [ ] 1.2 Add summary placeholder constants (`BEL_SUM_NETTO`, `BEL_UST_SATZ`, `BEL_UST_BETRAG`)
- [ ] 1.3 Add new placeholders to `ALLPLACEHOLDERS` list (but NOT to `ALLTABLEPLACEHOLDERS` — these are text-level placeholders resolved within table cells)

## 2. Per-Position Data Provisioning
- [ ] 2.1 In `InvoiceDialog.java`, build a list of per-position placeholder maps (one `HashMap<String, String>` per invoice position) alongside the existing `StyledCalculationTable`
- [ ] 2.2 Build summary data: net total, and a list of (tax rate, tax amount) pairs
- [ ] 2.3 Pass per-position data and summary data through the placeholder system to the server-side template processing (extend method signatures in `PlaceHolderServerUtils` or add a new data carrier)

## 3. LibreOffice Template Processing (ODT)
- [ ] 3.1 In `LibreOfficeAccess.java`, add detection logic: scan tables for cells containing `{{BELP_*}}` patterns
- [ ] 3.2 Implement template-row duplication: for each invoice position, clone the template row and resolve `{{BELP_*}}` placeholders against that position's data
- [ ] 3.3 Implement tax-rate-row duplication: for rows containing `{{BEL_UST_SATZ}}`/`{{BEL_UST_BETRAG}}`, duplicate per distinct tax rate
- [ ] 3.4 Resolve `{{BEL_SUM_NETTO}}` in footer rows
- [ ] 3.5 Remove original template rows after duplication
- [ ] 3.6 Ensure regular scalar placeholders (e.g. `{{BEL_TOTAL}}`, `{{BEL_WHRG}}`) in the same table are still resolved normally
- [ ] 3.7 Recalculate column widths after row expansion using `calculateTextWidth()` approach

## 4. Microsoft Office Template Processing (DOCX)
- [ ] 4.1 In `MicrosoftOfficeAccess.java`, add detection logic: scan tables for cells containing `{{BELP_*}}` patterns
- [ ] 4.2 Implement template-row duplication for DOCX (clone `XWPFTableRow`, resolve placeholders per position)
- [ ] 4.3 Implement tax-rate-row duplication for DOCX
- [ ] 4.4 Resolve `{{BEL_SUM_NETTO}}` in footer rows
- [ ] 4.5 Remove original template rows after duplication
- [ ] 4.6 Ensure regular scalar placeholders in the same table are still resolved normally
- [ ] 4.7 Implement auto column width adjustment for DOCX after row expansion

## 5. Number Formatting
- [ ] 5.1 Apply language-aware number formatting (DE/EN/FR/NL) to all monetary per-position placeholders, consistent with existing `{{BEL_TABELLE}}` formatting

## 6. Testing
- [ ] 6.1 Test with ODT template: flexible table with subset of columns
- [ ] 6.2 Test with ODT template: combined placeholders in single cell
- [ ] 6.3 Test with DOCX template: flexible table with subset of columns
- [ ] 6.4 Test with DOCX template: combined placeholders in single cell
- [ ] 6.5 Test tax-rate row duplication with multiple tax rates
- [ ] 6.6 Verify `{{BEL_TABELLE}}` still works unchanged (backward compatibility)
- [ ] 6.7 Test edge case: template with no positions (empty invoice)
- [ ] 6.8 Verify column widths are adjusted to actual content after placeholder resolution
