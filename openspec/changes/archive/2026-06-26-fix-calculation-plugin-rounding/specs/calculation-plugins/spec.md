## ADDED Requirements

### Requirement: Decimal claim value binding

The client SHALL pass the case claim value into a calculation plugin as a
`java.math.BigDecimal` bound to the Groovy variable `claimvalue`. The conversion from the
stored `float` claim value SHALL NOT introduce binary floating-point artifacts (i.e. it SHALL
go through the decimal string representation, not `new BigDecimal(float)`), and the result
SHALL be scaled to 2 decimal places using `RoundingMode.HALF_UP`.

#### Scenario: Claim value reaches the plugin without float artifacts

- **WHEN** a case with claim value `12500.55` opens a calculation plugin from the invoice
  dialog
- **THEN** the plugin's `claimvalue` binding is a `BigDecimal` equal to `12500.55` at scale 2
- **AND** no value such as `12500.5499999…` (a `new BigDecimal(float)` artifact) is observed

#### Scenario: All value-bearing plugin entry points use the decimal binding

- **WHEN** any `CalculationPlugin.getUi(...)` overload that carries a claim value is invoked
- **THEN** the `claimvalue` it binds is a `BigDecimal`, not a `float`

### Requirement: Decimal invoice position construction

Calculation plugins SHALL construct `InvoicePosition` monetary fields (`taxRate`, `units`,
`unitPrice`, `total`) using `BigDecimal` arithmetic at scale 2 with `RoundingMode.HALF_UP`. The
unit price SHALL be derived as `total.divide(units, 2, RoundingMode.HALF_UP)` and SHALL NOT use
`float`/`double` division.

#### Scenario: Unit price derived by decimal division

- **WHEN** a plugin creates a position with `total = 100.00` split over `units = 3`
- **THEN** `unitPrice` is `33.33` (BigDecimal divide, scale 2, HALF_UP)
- **AND** the field is a scale-2 `BigDecimal`, not a converted `float`

#### Scenario: No float division in position helpers

- **WHEN** the shared `InvoiceUtils.invoicePosition(...)` helpers build a position
- **THEN** every monetary field is set from a scale-2 `BigDecimal`
- **AND** no `(float)(total / units)` style computation is performed

### Requirement: Decimal fee calculation

Fee and statutory-table calculations SHALL compute the product of table value and factor
using `BigDecimal` with an explicit scale and `RoundingMode.HALF_UP`. This applies to RVG, GKG,
PKH, GNotKG, Honorar, Strafrecht and Pflichtverteidiger calculations. Factor values SHALL be constructed from
their exact decimal representation (e.g. `new BigDecimal("1.3")`), and no intermediate
`float`/`double` money arithmetic SHALL occur before rounding.

#### Scenario: Fee computed entirely in decimal

- **WHEN** a fee is computed as table value multiplied by factor `1.3`
- **THEN** the intermediate product and the rounded result are `BigDecimal`
- **AND** the result equals the published fee table value to the cent

### Requirement: Position totals reconcile to the cent

For positions produced by a calculation plugin, the sum of `position.total` SHALL equal the
invoice net total to the cent, and for each position `unitPrice * units` SHALL equal `total` at
scale 2.

#### Scenario: Sum invariance

- **WHEN** a plugin transfers several positions into an invoice
- **THEN** `Σ position.total` equals the invoice net total with no cent deviation
- **AND** for each position, `unitPrice.multiply(units).setScale(2, HALF_UP)` equals `total`

### Requirement: Version-gated plugin republication

Affected plugins SHALL be republished as new versions targeting client version `3.6.0.0` by
adding `<calculation … for="3.6.0.0" …>` entries (and a corresponding source directory) without
modifying any plugin version targeting an earlier client version. The client SHALL load only
the plugin entries whose `for` attribute equals its own full version string.

#### Scenario: 3.6.0.0 client gets the fixed plugins

- **WHEN** a client reporting full version `3.6.0.0` synchronizes plugins
- **THEN** it downloads the `for="3.6.0.0"` entries (the BigDecimal-based plugins)

#### Scenario: Older client is unaffected

- **WHEN** a client reporting full version `3.5.0.3` synchronizes plugins
- **THEN** it resolves only its existing `for="3.5.0.3"` entries
- **AND** the newly added `for="3.6.0.0"` entries are ignored, leaving its behavior unchanged

### Requirement: Electronic-invoice sign normalization preserved

The decimal reimplementation of position normalization SHALL keep `InvoicePosition` compliant
with EN 16931 / BR-27 (no negative unit price): when the line amount is negative the unit price
SHALL be made non-negative and the units sign flipped, leaving `total` unchanged.

#### Scenario: Negative line normalized without changing total

- **WHEN** a position has a negative `unitPrice`
- **THEN** after normalization `unitPrice` is non-negative, `units` is negated, and `total` is
  unchanged at scale 2
