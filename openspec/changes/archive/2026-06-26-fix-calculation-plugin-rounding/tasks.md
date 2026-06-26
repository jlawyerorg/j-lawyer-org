# Tasks

> Cross-repository change. Section A = `j-lawyer-org` (client 3.6). Section B =
> `j-lawyer-calculations` (new plugin versions for client `3.6.0.0`). Both must ship together.

## A. j-lawyer-org client (release 3.6)

- [x] A1 In `CalculationPlugin.java`, change both value-bearing `getUi(...)` signatures to take
  `java.math.BigDecimal claimValue` and bind `claimvalue` as that `BigDecimal`.
- [x] A2 Convert at the call sites: where the value is sourced from the `float`
  `ArchiveFileBean.getClaimValue()`, pass
  `new BigDecimal(Float.toString(claimValue)).setScale(2, RoundingMode.HALF_UP)`.
- [x] A3 Update call site `InvoiceDialog.java:879` to the new `BigDecimal` signature + conversion.
- [x] A4 Update the 2-arg `getUi(...)` call sites. (The actual callers are
  `ArchiveFilePanel.java:1136` and `JKanzleiGUI.java:867`, **not** `ClaimLedgerDialog`, which
  imports `BigDecimal` but never calls `getUi`.) `ArchiveFilePanel` converts the parsed
  `claimValueF`; `JKanzleiGUI` passes `BigDecimal.ZERO.setScale(2)` (was `0f`).
- [x] A5 Checked `CalculationPluginUtil.java` and all `getUi(...)` callers — the only
  `CalculationPlugin.getUi` callers are A3/A4. (`FormInstancePanel.getUi()` is a different
  `FormPlugin` type, left unchanged.)
- [x] A6 Harden manual entry: `InvoicePositionEntryPanel.java:770` now applies
  `.setScale(2, RoundingMode.HALF_UP)` to `taxRate`.
- [x] A7 JavaDoc added to both `CalculationPlugin.getUi(...)` methods describing the
  `BigDecimal` `claimvalue` binding contract.

## B. j-lawyer-calculations plugins (new versions for `for="3.6.0.0"`)

- [x] B1 **Republished money-plugin set:** `rvg`, `sozialrecht`, `honorar`, `strafrecht_owig`,
  `gnotkg`, `tableproperties`. These build `InvoicePosition`s / read `claimvalue`. `gnotkg`
  (experimental, last shipped for `3.4.0.4`) is brought forward into `3.6.0.0` per request — it
  shares the fixed `InvoiceUtils.groovy`; its call sites use the `(String,…)` overloads.
  `pflichtverteidiger`/`strafrecht` exist in `src/2.3.0.0` but are not in the XML (not deployed),
  so they are out of scope. Non-monetary plugins (`rvgtables`, `pkhtables`, `gkgtables`,
  `rechnungsnr`, `archivnr`) are carried forward unchanged.
- [x] B2 Created source directory `src/3.6.0.0/`.
- [x] B3 Rewrote `InvoiceUtils.groovy` (→ `src/3.6.0.0/`): `invoicePosition(...)` overloads take
  `Number` and convert losslessly to `BigDecimal` (via `toString()`); all monetary fields
  `setScale(2, HALF_UP)`; `unitPrice = total.divide(units, 2, HALF_UP)` (replaces
  `(float)(total/units)`) with a zero-units guard; `normalizeForElectronicInvoice` uses
  `signum()`/`negate()`. Verified by executing the methods against a stub `InvoicePosition`
  (13 assertions: scale, division, zero-units, e-invoice sign flip — all pass).
- [x] B4 Rewrote `TaxPropertiesUtils.groovy` (→ `src/3.6.0.0/`): `runden(...)` via
  `BigDecimal.setScale(.., HALF_UP)` (dropped `Math.round(value*d)/d`); `getUst*Factor()` via
  `BigDecimal.divide`; `getUst*PercentageString()` and `setUst*Percentage(...)` no longer use
  `float`.
- [x] B8 Bumped `version` (and `updated` date) in each republished `_meta.groovy`: rvg 3.4.3,
  sozialrecht 3.3.1, honorar 3.2.1, strafrecht_owig 2.2.2, gnotkg 2.0.1, tableproperties 1.4.1.
- [x] B9 Added a full `for="3.6.0.0"` block to `j-lawyer-calculations.xml` (11 rows): money
  plugins (incl. `gnotkg`) point at `…/calculations/3.6.0.0`; non-monetary plugins keep their
  existing `…/2.3.0.0` URLs. No existing row modified. XML validated with `xmllint`.

### Deep fee-internal BigDecimal conversion (done)

- [x] B5 Rewrote `rvg_ui.groovy` fee functions (`berechneRvgWertGebuehr`/`berechnePkh…`/
  `berechneGkg…`) to `BigDecimal` via a shared `wertGebuehr(rl, streitWert, factor)` helper:
  `base.multiply(factor.setScale(2, HALF_UP)).max(0).setScale(2, HALF_UP)`. Rounding the factor
  to 2 decimals recovers the exact factor from float artifacts (the Mehrvertretungszuschlag
  `(n-1)*0.3+satz` produces e.g. `1.5999999` as a float; `faktorFormat` defines factors at 2
  decimals, so `1.5999999 → 1.60`). No `Math.max` on `BigDecimal` (that is ambiguous in Groovy).
- [x] B6 Same `BigDecimal` rewrite applied to `honorar_ui.groovy` (Rvg/Gkg `berechne*`). For the
  fixed-Betragsrahmen plugins `sozialrecht_ui`/`strafrecht_owig_ui`: `factor`, `urahmen`,
  `orahmen`, `schwellenG`, the Mehrvertretung formula, the `/2`+`*-1` Anrechnung/quote lines were
  moved to `BigDecimal` (`calchange()` already returned `BigDecimal`). `gnotkg`'s
  `GNotKgTablesRangeList.berechneGebuehr`/`getMappedValue` converted to `BigDecimal`.
- [x] B7 Converted the provider classes to `BigDecimal`: `RvgTablesRange`/`PkhTablesRange`/
  `GkgTablesRange`/`GNotKgTablesRange` (`low`/`high`/`mappedValue` → `BigDecimal`, `contains`
  takes `Number`) and the 10 `*RangeList*` `getMappedValue(...)` methods now take and return
  `BigDecimal` (dual `float`/`double` overloads collapsed to one). The display plugins'
  `berechne*` (called by `strafrecht` via `new rvgtables_ui()`) had their `Math.max(BigDecimal,0)`
  replaced with `.max(0g)`. Republished `rvgtables` (1.6.1), `pkhtables` (1.5.1), `gkgtables`
  (1.4.5) under `for="3.6.0.0"` so the `BigDecimal` table classes are actually downloaded.
  **Verification:** all 160 table lookups identical old-vs-new across all tables/years; the full
  `table → wertGebuehr → InvoiceUtils.invoicePosition` path verified (incl. artifact factor and
  `.5` GKG values) against the real `3.6.0.0` classes; every edited `.groovy` parse-checked.

## C. Verification

- [x] C1 Reconciliation proven for `InvoiceUtils` via stub execution: `unitPrice*units` and
  `total` consistent at scale 2; multi-unit split (100.00/3 → 33.33) handled.
- [ ] C2 RVG spot-check against the published table (manual; needs the running client).
- [x] C3 Compatibility (static): client matches `for` exactly to `getFullClientVersion()`
  (`SplashThread.java:1331`); `3.5.x` clients ignore the new `for="3.6.0.0"` rows; existing rows
  untouched. Runtime download path to be confirmed with a real 3.6.0.0 client.
- [x] C4 E-invoice normalization verified in the stub run (total unchanged, unitPrice
  non-negative, units negated).
- [ ] C5 Manual end-to-end: open `InvoiceDialog`, pull positions from each republished plugin,
  confirm no cent deviation and a clean ZUGFeRD/XRechnung export (needs the running client +
  published plugins).

## D. OpenSpec hygiene

- [x] D1 `openspec validate fix-calculation-plugin-rounding --strict` passes.
- [ ] D2 After deployment, archive the change and fold `calculation-plugins` into
  `openspec/specs/`.
