## 1. Build configuration

- [x] 1.1 Move `swagger-maven-plugin` (generate, `compile` phase) into the main
  `j-lawyer-io` `<build>`; output to `target/swagger-gen`.
- [x] 1.2 Move the `SwaggerFinalizer` exec (`process-classes`) into the main build; output
  to `target/swagger-final/swagger.json`.
- [x] 1.3 Add `maven-war-plugin` `<webResources>` overlaying `target/swagger-final/swagger.json`
  into `swagger-ui/`.
- [x] 1.4 Delete the `swagger-regen` profile.

## 2. Remove committed artifacts

- [x] 2.1 `git rm` `src/main/webapp/swagger-ui/swagger.json` and git-ignore the path.
- [x] 2.2 `git rm` `src/test/resources/swagger.golden.json`.
- [x] 2.3 `git rm` `src/test/java/org/jlawyer/io/rest/tools/SwaggerEquivalenceTest.java`.

## 3. Docs

- [x] 3.1 Update `CLAUDE.md` REST API spec section to describe always-on artifact generation.

## 4. Verification

- [ ] 4.1 Build `j-lawyer-io` and confirm `j-lawyer-io.war` contains a fresh
  `swagger-ui/swagger.json` reflecting current annotations (incl. the new contact fields).
- [ ] 4.2 Confirm a no-op rebuild leaves the git working tree clean (no committed spec to churn).
