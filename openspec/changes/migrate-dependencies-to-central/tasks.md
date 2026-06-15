## 1. Foundation
- [ ] 1.1 Add root `<dependencyManagement>` / BOM scaffolding (single place to pin versions)
- [ ] 1.2 (Optional, immediate) Add a binary CVE scanner to CI (OWASP Dependency-Check / Trivy) covering the committed jars
- [ ] 1.3 Build the verified mapping `jlawyer.thirdparty:<a>:<v>` → Central GAV from `scripts/lib-gav-map.txt`; flag artifacts whose committed jar diverges from the Central release (SHA-1 / spot-check)

## 2. Per-module coordinate migration (version-for-version, no upgrades)
- [ ] 2.1 j-lawyer-fax
- [ ] 2.2 j-lawyer-server-common
- [ ] 2.3 j-lawyer-io-common
- [ ] 2.4 j-lawyer-server-entities
- [ ] 2.5 j-lawyer-server-api
- [ ] 2.6 j-lawyer-server-ejb / -war / -io / j-lawyer-io (+ EAR bundled libs)
- [ ] 2.7 j-lawyer-client (largest set)
- [ ] After each module: remove migrated `lib/` jars, `mvn clean install`, smoke test

## 3. Transitive management (keep behavior identical)
- [ ] 3.1 Pin shared libs in `<dependencyManagement>` (pdfbox, groovy, ical4j, httpclient, jackson, commons-*)
- [ ] 3.2 Keep cloud/invoicing as shaded jars consumed via `:shaded` classifier + `<exclusions>*:*</exclusions>`
- [ ] 3.3 Preserve cloud's http relocation / jackson pin (beA conflict)
- [ ] 3.4 Diff the resolved dependency tree against the pre-migration set — versions must match

## 4. Non-Central residual artifacts
- [ ] 4.1 Decide residual repo form (in-project file repo vs private/Nexus)
- [ ] 4.2 Keep local: beA wrapper + `lib/bea/*`, `jboss-client`/`jboss-cli-client` (provided), any repackaged/no-version jars confirmed ≠ Central
- [ ] 4.3 Move client `javafx.*` → `org.openjfx:javafx-*` (verify UI) — or defer to JavaFX modernization
- [ ] 4.4 Evaluate `swingx`/`beansbinding`/`swing-layout`/`jortho`/`sam`/`config_schulung` for version-matching Central artifacts; else keep local
- [ ] 4.5 Delete `bea.bak/` (unused backup)

## 5. Cleanup
- [ ] 5.1 Remove obsolete committed `lib/*.jar`; update `.gitignore`
- [ ] 5.2 Retire `scripts/seed-maven-repo.sh` + `scripts/lib-gav-map.txt` (or scope them to the residual set)
- [ ] 5.3 CI: drop the maven-repo seed step (keep only if a residual local repo remains)

## 6. Dependency security
- [ ] 6.1 Add `.github/dependabot.yml` (maven ecosystem, all module poms)
- [ ] 6.2 Confirm Dependabot alerts/PRs appear for a known-vulnerable test bump
- [ ] 6.3 Document the residual-artifact coverage gap (no advisory data) + the binary scanner as compensating control

## 7. Verification
- [ ] 7.1 Full `mvn clean install` (Java 17) green
- [ ] 7.2 EAR deploys to WildFly 26.1.3; client launches (incl. JavaFX); REST/Swagger OK
- [ ] 7.3 Dependency-tree diff confirms no version changes vs the pre-migration baseline
