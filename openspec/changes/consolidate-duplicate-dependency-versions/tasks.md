## 1. Low-risk lifts (patch/minor; no expected code changes)
- [ ] 1.1 `org.slf4j:slf4j-api` → **1.7.36** (client 1.7.36; fax/server/server-common/server-entities/server-io 1.7.30) — patch bump
- [ ] 1.2 `commons-logging:commons-logging` → **1.2** (client/server/server-common/server-ejb 1.2; fax 1.1)
- [ ] 1.3 `commons-net:commons-net` → **3.5** (server/server-common/server-ejb 3.5; client 3.1)
- [ ] 1.4 `javax.activation:activation` → **1.1.1** (server-ejb 1.1.1; fax 1.1)
- [ ] 1.5 `com.sun.xml.bind:jaxb-impl` → **2.3.3** (client 2.3.3; server-ejb 2.3.2)
- [ ] 1.6 `junit:junit` → **4.12** (test scope; server-common/server-entities 4.12; client/fax/server 4.10 + 3.8.2) — verify no test relies on JUnit 3 `TestCase`

## 2. Lifts that may need code changes (review APIs before/after)
- [ ] 2.1 `javax.xml.bind:jaxb-api` → **2.3.1** (server-ejb 2.3.1; fax 2.2.7) — check JAXB binding/runtime usage in fax
- [ ] 2.2 `org.apache.commons:commons-lang3` → **3.12.0** (client 3.12.0; server/server-common/server-ejb 3.5) — review removed/deprecated `org.apache.commons.lang3.*` usages in server modules
- [ ] 2.3 `com.google.guava:guava` → **32.1.1-jre** (client 32.1.1-jre; fax 14.0.1) — **major bump**; review fax's `com.google.common.*` usage for removed APIs (e.g. `Objects`, `Throwables`, beta APIs)

## 3. POI 4 → 5 (major; highest code-change risk)
- [ ] 3.1 `org.apache.poi:poi` → **5.2.3** (client 5.2.3; server-ejb 4.1.2)
- [ ] 3.2 `org.apache.poi:poi-scratchpad` → **5.2.3** (client 5.2.3; server-ejb 4.1.2)
- [ ] 3.3 Review server-ejb POI usage for 4→5 breaking changes (deprecated/removed `org.apache.poi.*` APIs, `poi-ooxml-schemas` → `poi-ooxml-full`, cell-type enums) and adjust code/tests
- [ ] 3.4 Confirm POI-backed features still work (spreadsheet/document generation on server and client)

## 4. Centralize + verify
- [ ] 4.1 Move all 11 now-single-version coordinates into root `pom.xml` `<dependencyManagement>`; remove the per-module `<version>` overrides left by `migrate-dependencies-to-central`
- [ ] 4.2 Full `mvn clean install` (Java 17) green; dependency-tree shows one version per coordinate
- [ ] 4.3 EAR deploys to WildFly 26.1.3; client launches; smoke-test the upgraded-API features (POI, Guava in fax, jaxb in fax)
