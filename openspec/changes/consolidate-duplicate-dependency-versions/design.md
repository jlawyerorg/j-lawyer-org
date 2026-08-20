## Context

`migrate-dependencies-to-central` re-sources the vendored jars under their real Maven
Central coordinates, version-for-version. Its Task 1.1 SHA-1 verification produced the
authoritative inventory in
`openspec/changes/migrate-dependencies-to-central/analysis/gav-verification.md`, which
lists 11 `groupId:artifactId` that exist at more than one version across modules. Because
that change forbids upgrades, those 11 are pinned **per-module** rather than centrally.
This change lifts them to one version each so they can be managed centrally.

## The 11 coordinates and target versions

Default rule: consolidate **up** to the highest version already vendored (no module loses
functionality; only the lagging modules move forward).

| Coordinate | Versions in use (module) | Target | Risk |
|---|---|---|---|
| `org.slf4j:slf4j-api` | 1.7.36 (client) / 1.7.30 (fax, server, server-common, server-entities, server-io) | 1.7.36 | none (patch) |
| `commons-logging:commons-logging` | 1.2 (client, server, server-common, server-ejb) / 1.1 (fax) | 1.2 | none |
| `commons-net:commons-net` | 3.5 (server, server-common, server-ejb) / 3.1 (client) | 3.5 | low |
| `javax.activation:activation` | 1.1.1 (server-ejb) / 1.1 (fax) | 1.1.1 | low |
| `com.sun.xml.bind:jaxb-impl` | 2.3.3 (client) / 2.3.2 (server-ejb) | 2.3.3 | low |
| `junit:junit` | 4.12 (server-common, server-entities) / 4.10 + 3.8.2 (client, fax, server) | 4.12 | low (test only) |
| `javax.xml.bind:jaxb-api` | 2.3.1 (server-ejb) / 2.2.7 (fax) | 2.3.1 | medium |
| `org.apache.commons:commons-lang3` | 3.12.0 (client) / 3.5 (server, server-common, server-ejb) | 3.12.0 | medium |
| `com.google.guava:guava` | 32.1.1-jre (client) / 14.0.1 (fax) | 32.1.1-jre | **high (major 14→32)** |
| `org.apache.poi:poi` | 5.2.3 (client) / 4.1.2 (server-ejb) | 5.2.3 | **high (major 4→5)** |
| `org.apache.poi:poi-scratchpad` | 5.2.3 (client) / 4.1.2 (server-ejb) | 5.2.3 | **high (major 4→5)** |

## Decisions

### D1 — Consolidate upward, per coordinate, smallest-risk first
Order the work patch/minor lifts → API-affecting lifts → POI major. Each lift is an
independent, revertable step (one coordinate at a time), so a regression isolates to a
single library.

### D2 — POI 4→5 is the dominant risk
The client already runs POI 5.2.3; only `j-lawyer-server-ejb` lags at 4.1.2. POI 5 drops
`poi-ooxml-schemas` (folded into `poi-ooxml-full`/`poi-ooxml-lite`), changes several
enums to real Java enums, and removes deprecated APIs. The server's POI usage must be
read and adjusted, and POI-backed document/spreadsheet features smoke-tested.

### D3 — Guava 14→32 is a wide jump but narrow surface
Only `j-lawyer-fax` is on Guava 14. Review fax's `com.google.common.*` imports for
removed/beta APIs; the surface is small, so a targeted code review suffices.

### D4 — Keep `javax.*` namespace
This change does **not** touch the jakarta migration; jaxb-api/jaxb-impl stay on the
`javax.xml.bind` 2.3.x line (matches the WildFly 26.1.3 / Jakarta EE 8 target).

## Risks / Trade-offs

- A consolidated upgrade can change behavior in the lagging module → mitigated by D1
  (one coordinate at a time, build + smoke test after each) and by reading the upgraded
  APIs for the high-risk lifts (POI, Guava, commons-lang3, jaxb).
- This change is **not required** for `migrate-dependencies-to-central` to ship; it is a
  clean follow-up that removes the per-module version exceptions that change leaves behind.
