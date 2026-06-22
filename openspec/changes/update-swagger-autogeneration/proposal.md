# Change: Generate swagger.json on every build as a build artifact

## Why
The `j-lawyer-io` `swagger.json` was an opt-in (`-Pswagger-regen`) regeneration whose
output was committed and guarded by a golden-baseline equivalence test. In practice the
served spec silently went stale whenever REST endpoints changed but the manual
regen+re-baseline step was forgotten, and the committed 11k-line file was a recurring
source of merge noise. The team prefers `swagger.json` to always reflect the annotations
automatically.

## What Changes
- Generation runs on **every** build (moved out of the `swagger-regen` profile into the
  main `j-lawyer-io` build): `swagger-maven-plugin` (`compile`) →
  `target/swagger-gen/swagger.json`, then `SwaggerFinalizer` (`process-classes`) →
  `target/swagger-final/swagger.json`, overlaid into the war's `swagger-ui/` by
  `maven-war-plugin` `webResources`.
- `swagger.json` becomes a **build artifact**: the committed
  `src/main/webapp/swagger-ui/swagger.json` is removed and git-ignored.
- **BREAKING (build governance):** the golden baseline
  (`src/test/resources/swagger.golden.json`) and `SwaggerEquivalenceTest` are removed —
  there is no longer a committed baseline to drift against, so the drift gate no longer
  applies. API changes are reviewed via the normal code/annotation diff.
- The `swagger-regen` profile is deleted (generation is unconditional now).

## Impact
- Affected specs: `rest-api-documentation`
- Affected code:
  - `j-lawyer-server/j-lawyer-io/pom.xml` (build plugins, war webResources, profile removed)
  - removed: `j-lawyer-server/j-lawyer-io/src/main/webapp/swagger-ui/swagger.json`
  - removed: `j-lawyer-server/j-lawyer-io/src/test/resources/swagger.golden.json`
  - removed: `j-lawyer-server/j-lawyer-io/src/test/java/org/jlawyer/io/rest/tools/SwaggerEquivalenceTest.java`
  - kept: `SwaggerFinalizer` (now writes to `target/swagger-final/`)
  - `.gitignore`, `CLAUDE.md` (docs)
