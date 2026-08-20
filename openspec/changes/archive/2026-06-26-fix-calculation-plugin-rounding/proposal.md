# Change: Fix rounding errors in invoice calculation plugins (BigDecimal interface)

## Why

The `InvoiceDialog` lets the user pull invoice positions from calculation plugins
(button `cmdCalculation`, `InvoiceDialog.java:879-880`). Those plugins live in a separate
repository, `/home/jens/dev/j-lawyer-calculations/`, and are loaded as Groovy scripts at
runtime via `CalculationPlugin.getUi(...)`.

The whole calculation "interface" — both the value handed *into* the plugins and the money
math *inside* them — is based on `float`/`double`, while the persisted invoice model is
`BigDecimal(precision=10, scale=2)`. Money is therefore computed in binary floating point
and only converted to `BigDecimal` at the very end, after precision has already been lost.
This produces the reported rounding errors (cent-level deviations, position totals that do
not add up to the invoice total, `unitPrice * units != total`).

Concrete float/double sources found:

- **Value into the plugin is float.** `ArchiveFileBean.getClaimValue()` is a `float`
  (`ArchiveFileBean.java:713,825`). `InvoiceDialog.java:879` passes it into
  `CalculationPlugin.getUi(Invoice, ArchiveFileBean, float claimValue)`, which binds it as
  the Groovy variable `claimvalue` as a `float`
  (`CalculationPlugin.java:712-728`).
- **Plugin helper does float division.** `InvoiceUtils.groovy` (in
  `j-lawyer-calculations/src/2.3.0.0/`) declares every overload with `float taxRate,
  float total, float units` and computes `pos.setUnitPrice((float)(total / units))`.
- **Fee math is float.** `rvg_ui.groovy` declares
  `float berechneRvgWertGebuehr(float streitWert, float factor, int rvgYear)` (and the GKG/PKH
  variants), keeps `float faktor`, and feeds parsed amounts via `.floatValue()`. `setScale(2,
  HALF_UP)` is applied only *after* the float arithmetic.
- **Tax helper is double/float.** `TaxPropertiesUtils.groovy` (in `src/2.6.0.0/`) uses
  `runden(double value, int decimalPoints)` via `Math.round(value*d)/d` and a `float`
  percentage.

The **return** boundary is already correct: `InvoicePosition.unitPrice/units/total/taxRate`
are all `BigDecimal(10,2)` (`InvoicePosition.java:698-707`). The fix is to make the *input*
and the *internal* math `BigDecimal` end-to-end, so no binary float ever touches money.

Because plugins are loaded by exact client-version match — the client only downloads
`<calculation>` entries whose `for` attribute equals `getFullClientVersion()` ("3.6.0.0",
`SplashThread.java:1331`) — the fixed plugins must be **republished as new versions targeting
client 3.6.0.0**. Clients < 3.6 keep matching their existing `for` entries and are completely
unaffected (no regression risk).

## What Changes

This is a **cross-repository** change spanning two repos that must ship together.

### Repo A — `j-lawyer-org` (desktop client, released as 3.6)
- **BREAKING (plugin contract):** Change the `CalculationPlugin.getUi(...)` signatures from
  `float claimValue` to `java.math.BigDecimal claimValue`, and inject the case claim value into
  calculation plugins as a `BigDecimal` `claimvalue` binding instead of `float`. Callers convert
  from the stored `float` via `new BigDecimal(Float.toString(claimValue))` so no binary-float
  artifact is introduced. Affects all `CalculationPlugin.getUi(...)` overloads
  (`CalculationPlugin.java:712,721`) and their call sites (`InvoiceDialog`, `ClaimLedgerDialog`,
  `CalculationPluginUtil`).
- Harden the manual entry path: `InvoicePositionEntryPanel` sets `taxRate` without
  `setScale(2)` (`InvoicePositionEntryPanel.java:770`) — align it with the other fields.

### Repo B — `j-lawyer-calculations` (Groovy plugins, new versions for client 3.6.0.0)
- Add a new plugin source directory `src/3.6.0.0/` containing BigDecimal-based versions of
  every affected plugin and its shared helpers (`InvoiceUtils.groovy`,
  `TaxPropertiesUtils.groovy`, the fee-table classes).
- Rewrite `InvoiceUtils.invoicePosition(...)` to take and operate on `BigDecimal`; compute
  `unitPrice = total.divide(units, 2, RoundingMode.HALF_UP)`; normalize all monetary fields to
  scale 2 / HALF_UP; keep the existing e-invoice sign normalization.
- Rewrite fee calculations (RVG/GKG/PKH/GNotKG/Honorar/Strafrecht/Pflichtverteidiger) to use
  `BigDecimal` for table value × factor with explicit scale and `HALF_UP` rounding; read
  `claimvalue` as `BigDecimal`.
- Rewrite `TaxPropertiesUtils` to use `BigDecimal` (percentage and rounding).
- Bump the `version` in each affected `_meta.groovy`.
- Add `<calculation … for="3.6.0.0" version="<new>" url="…/3.6.0.0" files="…"/>` entries to
  `j-lawyer-calculations.xml` for every affected plugin; leave all existing entries untouched.

### Compatibility
- Clients < 3.6.0.0 continue to match their existing `for` entries → unchanged float-based
  plugins → identical behavior. No older plugin version is modified.

## Impact

- **Affected specs:** `calculation-plugins` (new capability).
- **Affected code (repo A):**
  - `j-lawyer-client/.../plugins/calculation/CalculationPlugin.java` (binding type)
  - `j-lawyer-client/.../editors/files/InvoiceDialog.java:879` (call site)
  - `j-lawyer-client/.../editors/files/ClaimLedgerDialog.java` (call site)
  - `j-lawyer-client/.../plugins/calculation/CalculationPluginUtil.java` (call site, if any)
  - `j-lawyer-client/.../editors/files/InvoicePositionEntryPanel.java:770` (taxRate setScale)
- **Affected code (repo B, `j-lawyer-calculations`):**
  - `src/3.6.0.0/InvoiceUtils.groovy`, `TaxPropertiesUtils.groovy` (new)
  - `src/3.6.0.0/rvg_ui.groovy`, `honorar_ui.groovy`, `sozialrecht_ui.groovy`,
    `gnotkg_ui.groovy`, `pflichtverteidiger_ui.groovy`, `strafrecht_ui.groovy`,
    `strafrecht_owig_ui.groovy`, `tableproperties_ui.groovy` and the `*tables*`/`*meta*` files
    (new versions; exact list confirmed by the audit task)
  - `j-lawyer-calculations.xml` (new `for="3.6.0.0"` entries)
- **No database / server / REST change.** The claim value stays a `float` in the DB; it is
  converted to `BigDecimal` at the plugin boundary only. (Migrating `ArchiveFileBean.claimValue`
  to `BigDecimal` is a larger schema change and is an explicit non-goal here.)
