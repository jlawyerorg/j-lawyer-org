## Context

The invoice calculation feature is split across two repositories:

- `j-lawyer-org` — the desktop client. It discovers, downloads and runs calculation plugins.
  Discovery (`SplashThread.java:1285-1379`) fetches
  `https://www.j-lawyer.org/downloads/j-lawyer-calculations.xml`, and downloads only the
  `<calculation>` entries whose `for` attribute **exactly equals** the running client version
  string `VersionUtils.getFullClientVersion()` (`SplashThread.java:1330-1344`). For the 3.6
  release that string is `"3.6.0.0"` (`VersionUtils.getClientVersion()` = `"3.6"`, patch `"0"`,
  build `"0"` → `getFullClientVersion()` = `"3.6.0.0"`). Plugins are cached per client version
  under `…/calculations/<fullClientVersion>/`.
- `j-lawyer-calculations` — the plugin repository. Plugins are Groovy scripts grouped by a
  minimum-client-version source directory (`src/2.3.0.0/`, `src/2.6.0.0/`, …) and listed in
  `j-lawyer-calculations.xml`. Pushing to this repo publishes the files to the download URL.

At runtime the client instantiates each plugin via `CalculationPlugin.getUi(...)`
(`CalculationPlugin.java:704-728`), which runs `<id>_ui.groovy` in a Groovy `Binding`. The
binding carries `callback` (a `GenericCalculationCallback`) and, for the value-based dialogs,
`claimvalue`. Plugins return invoice positions by calling
`callback.processResultToInvoice(ArrayList<InvoicePosition>)`
(`GenericCalculationCallback.java:782-794`), which republishes them as
`InvoicePositionAddedEvent`s consumed by `InvoiceDialog`.

The persisted model is decimal: `InvoicePosition.taxRate/units/unitPrice/total` and
`Invoice.total/totalGross` are all `BigDecimal(precision=10, scale=2)`
(`InvoicePosition.java:698-707`, `Invoice.java:766-769`). Everything *upstream* of those
setters is float/double, which is the defect.

## Goals / Non-Goals

Goals:
- No binary floating-point arithmetic touches money anywhere on the InvoiceDialog ←→ plugin
  path. All monetary values are `BigDecimal` with an explicit scale (2) and rounding mode
  (`HALF_UP`) from the plugin input through `InvoicePosition` construction.
- Position totals reconcile exactly: `Σ position.total == invoice.total` to the cent, and
  `unitPrice * units == total` to scale 2.
- Zero behavioral impact on clients older than 3.6.0.0.

Non-Goals:
- Migrating the `ArchiveFileBean.claimValue` DB column / entity / REST contract from `float`
  to `BigDecimal`. That is a server + schema + API-versioning change of much larger blast
  radius. Here the float claim value is converted to `BigDecimal` losslessly *at the plugin
  boundary* (`new BigDecimal(Float.toString(v))`), which is sufficient because the displayed
  claim value already only carries 2 decimal places.
- Changing the document-output path (`processResultToDocument`) numeric formatting beyond what
  the BigDecimal binding already gives it.
- Rewriting plugins that perform no money math (pure UI/table-property helpers) unless the
  audit shows they feed money values.

## Decisions

### D1 — Convert the claim value to BigDecimal at the boundary, not in the DB
The float claim value is converted to `BigDecimal` via
`new BigDecimal(Float.toString(claimValue)).setScale(2, RoundingMode.HALF_UP)`.

- `Float.toString()` yields the shortest decimal that round-trips the float (e.g. `4000.0`,
  `12500.55`), so the BigDecimal reflects what the user sees, avoiding the
  `new BigDecimal(float)` artifact (`new BigDecimal(0.1f)` → `0.100000001490…`).
- The `getUi(...)` signatures are widened to accept `BigDecimal claimValue` (see D2), and the
  float→BigDecimal conversion above is performed by the callers
  (`InvoiceDialog`, `ClaimLedgerDialog`, and any other caller that sources the value from the
  `float` `ArchiveFileBean.getClaimValue()`). This keeps the plugin-facing contract purely
  decimal and confines the legacy `float` to the call site that reads the entity.

### D2 — `getUi(...)` contract becomes BigDecimal (typed contract change, version-gated)
The `CalculationPlugin.getUi(...)` Java signatures are changed from `float claimValue` to
`BigDecimal claimValue`, and the `claimvalue` Groovy binding becomes a `BigDecimal`. Because a
3.6.0.0 client only ever loads 3.6.0.0-targeted plugins (D-context), and those are all
republished by this change, the binding type can change outright instead of adding a parallel
variable. `BigDecimal` is a `Number`, and Groovy promotes mixed `BigDecimal` arithmetic to
`BigDecimal`, so plugin code reads naturally (`claimvalue * faktor`). The republished plugins
are written to expect `BigDecimal`.

### D3 — Canonical money helper in `InvoiceUtils.groovy`
All position construction goes through BigDecimal overloads:

```groovy
import java.math.BigDecimal
import java.math.RoundingMode

static InvoicePosition invoicePosition(BigDecimal units, String name, String description,
                                       BigDecimal taxRate, BigDecimal total) {
    def pos = new InvoicePosition()
    pos.setName(name); pos.setDescription(description ?: "")
    pos.setTaxRate(taxRate.setScale(2, RoundingMode.HALF_UP))
    pos.setUnits(units.setScale(2, RoundingMode.HALF_UP))
    pos.setTotal(total.setScale(2, RoundingMode.HALF_UP))
    pos.setUnitPrice(total.divide(units, 2, RoundingMode.HALF_UP))   // was (float)(total/units)
    return normalizeForElectronicInvoice(pos)
}
```

Rounding policy: scale 2, `HALF_UP`, applied once at construction. `unitPrice` is derived by
`BigDecimal.divide` with explicit scale+mode (the old `(float)(total/units)` is the single
worst offender). The e-invoice sign normalization (EN 16931 / BR-27) is preserved but
rewritten with `BigDecimal.signum()` instead of `< 0f`.

### D4 — Decimal fee calculations
Fee functions become `BigDecimal berechne…WertGebuehr(BigDecimal streitWert, BigDecimal
factor, int year)`; table lookups return `BigDecimal`; `value.multiply(factor).setScale(2,
HALF_UP)`. `factor` values (1.3, 0.65, …) are constructed as `BigDecimal` literals
(`new BigDecimal("1.3")`, *not* `new BigDecimal(1.3d)`). UI spinners that currently produce
`float` factors are read via `new BigDecimal(spinner.value.toString())`.

### D5 — Republication & version matrix
For each affected plugin: bump `version` in `_meta.groovy`, place the new files in
`src/3.6.0.0/`, and add one XML row `for="3.6.0.0"` pointing at the `3.6.0.0` URL. Existing
rows are not edited. This is the explicit compatibility mechanism the request calls for.

## Risks / Trade-offs

- **Risk: incomplete plugin coverage** → an audit task (T-B1) enumerates every plugin that
  reads `claimvalue` or builds an `InvoicePosition`/fee and the list is pinned before rewrite.
- **Risk: Groovy dynamic typing hides a missed float** → unit-style reconciliation checks
  (`Σtotal == invoiceTotal`, `unitPrice*units == total`) on representative values (e.g. RVG
  factor 1.3 on several Streitwerte; a 3-way split of an odd total) added to the manual test
  checklist; spot-check against published RVG/GKG tables.
- **Risk: `claimvalue` type change breaks a not-yet-republished plugin** → mitigated by D5:
  the 3.6.0.0 client can only load 3.6.0.0 plugins, all of which are republished here.
- **Trade-off: claim value stays float in the DB** → accepted; the boundary conversion is
  lossless for 2-dp values and avoids a schema/API migration (Non-Goals).

## Migration Plan

1. Land repo A (client) changes on the 3.6 branch; build is manual.
2. Land repo B: add `src/3.6.0.0/` plugins + XML rows; push publishes them.
3. Order: publishing repo B before any 3.6.0.0 client exists in the wild is safe (old clients
   ignore `for="3.6.0.0"`). A freshly started 3.6.0.0 client downloads the new plugins on
   first run (`SplashThread` plugin sync).
4. Rollback: revert the client binding change and/or remove the `for="3.6.0.0"` XML rows; no
   data migration is involved.

## Open Questions

- None. The plugin set requiring republication is confirmed (see tasks B1), and the `getUi(...)`
  contract is confirmed to change to `BigDecimal` (D1/D2).
