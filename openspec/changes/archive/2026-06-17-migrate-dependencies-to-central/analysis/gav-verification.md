# Task 1.3 — Verified GAV Mapping (jlawyer.thirdparty:* → Maven Central)

Verification method: for each unique runtime artifact in `scripts/lib-gav-map.txt`, the local committed jar's SHA-1 was compared against the Maven Central release of the same (or sha-matched) coordinate via `repo1.maven.org`. Source/javadoc jars were excluded. 223 unique runtime artifacts.

Full machine-readable result: [`analysis/lib-gav-map-central.tsv`](lib-gav-map-central.tsv) (and `scripts/lib-gav-map-central.tsv`).

## Summary

| status | count | meaning | Phase 2 action |
|---|---|---|---|
| **CENTRAL_OK** | 169 | local jar SHA-1 byte-identical to a Central release | migrate version-for-version to the real GAV |
| **SHA_MISMATCH** | 11 | same coordinate exists on Central but the committed jar is a divergent/repackaged build | keep local (D1 'no behavior change'); investigate individually |
| **NOT_ON_CENTRAL** | 43 | no Central release matches the committed binary | residual local repo (D3) |
| **total** | 223 | | |

## CENTRAL_OK — version recovery for no-version (0.0.0) jars

These committed jars had no version in their filename; the true Central version was recovered by SHA-1 match:

- `ecs` → `ecs:ecs:1.4.2`
- `jboss-cli-client` → `org.wildfly.core:wildfly-cli:18.1.2.Final:client`
- `jersey-client` → `org.glassfish.jersey.core:jersey-client:2.5.1`
- `jersey-common` → `org.glassfish.jersey.core:jersey-common:2.5.1`
- `jersey-container-servlet` → `org.glassfish.jersey.containers:jersey-container-servlet:2.5.1`
- `jersey-container-servlet-core` → `org.glassfish.jersey.containers:jersey-container-servlet-core:2.5.1`
- `jersey-server` → `org.glassfish.jersey.core:jersey-server:2.5.1`
- `TableLayout` → `tablelayout:TableLayout:20020517`

## SHA_MISMATCH (11) — committed jar diverges from Central

Coordinate exists on Central at the same version, but the bytes differ → these are repackaged/recompiled builds. Per design D1 they must **stay local** unless a spot-check proves the canonical Central jar is a safe substitute. (4 spot-checked directly against repo1 and confirmed divergent: log4j-core, jsch, json-simple, flyway-core.)

- `jlawyer.thirdparty:commons-beanutils:1.8.0` vs `commons-beanutils:commons-beanutils:1.8.0` — Version exists on Central but local sha differs (0c651d5...); no sha-1 match found anywhere; not the -core variant either.
- `jlawyer.thirdparty:flyway-core:11.17.2` vs `org.flywaydb:flyway-core:11.17.2` — Central 11.17.2 jar exists but sha differs from local; local jar likely repackaged/rebuilt.
- `jlawyer.thirdparty:flyway-mysql:11.17.2` vs `org.flywaydb:flyway-mysql:11.17.2` — Central 11.17.2 jar exists but sha differs from local; local jar likely repackaged/rebuilt.
- `jlawyer.thirdparty:jasperreports:4.5.1` vs `net.sf.jasperreports:jasperreports:4.5.1` — Only groupId net.sf.jasperreports for 4.5.1; central sha 383828fb... differs from local 31432ac2... (likely repackaged local jar).
- `jlawyer.thirdparty:jsch:0.1.54` vs `com.jcraft:jsch:0.1.54` — Only groupId on Central is com.jcraft; its sha da35843 differs from local. Likely repackaged.
- `jlawyer.thirdparty:json-simple:2.3.0` vs `com.github.cliftonlabs:json-simple:2.3.0` — Only groupId on Central is cliftonlabs; its sha f371933 differs from local. Likely repackaged.
- `jlawyer.thirdparty:log4j-api:2.17.1` vs `org.apache.logging.log4j:log4j-api:2.17.1` — Version exists on Central but local sha differs from Apache canonical d771af8 (and lucee 39fe14e); likely repackaged/recompiled.
- `jlawyer.thirdparty:log4j-core:2.17.1` vs `org.apache.logging.log4j:log4j-core:2.17.1` — Version on Central but local sha differs from Apache canonical 779f60f (and lucee 511f797); likely repackaged.
- `jlawyer.thirdparty:log4j-slf4j-impl:2.17.1` vs `org.apache.logging.log4j:log4j-slf4j-impl:2.17.1` — Only Apache groupId on Central; its sha 84692d4 differs from local. Likely repackaged.
- `jlawyer.thirdparty:tika-app:1.22` vs `org.apache.tika:tika-app:1.22` — Only org.apache.tika publishes 1.22; central sha b0f63b7 differs from local 5a245fb.
- `jlawyer.thirdparty:zip4j_1.3.2:0.0.0` vs `net.lingala.zip4j:zip4j:1.3.2` — Filename implies v1.3.2. net.lingala.zip4j:zip4j:1.3.2 exists but sha1 differs; local jar is a divergent/repackaged build.

## NOT_ON_CENTRAL (43) — residual artifacts

### javafx (use org.openjfx) (7)
- `javafx.base:0.0.0`
- `javafx.controls:0.0.0`
- `javafx.fxml:0.0.0`
- `javafx.graphics:0.0.0`
- `javafx.media:0.0.0`
- `javafx.swing:0.0.0`
- `javafx.web:0.0.0`

### other / truly non-central (14)
- `bizcal:0.0.6`
- `bizcalDemoApplication:0.0.0`
- `eclipselink-jpa-modelgen:2.3.0`
- `eclipselink:2.3.0`
- `forms:1.0.6`
- `ical4j:1.0-beta3`
- `j-lawyer-invoicing:0.0.0`
- `jai_codec:0.0.0`
- `jai_core:1.1.3`
- `jai_imageio:0.0.0`
- `jcifs:1.3.18`
- `jortho:0.0.0`
- `libintl:0.0.0`
- `swing-layout:1.0.4`

### proprietary (7)
- `config_schulung:0.0.0`
- `j-lawyer-bea-wrapper:0.0.0`
- `j-lawyer-cloud:0.0.0`
- `java-sepa-xml:1.0.1`
- `javaee-doc-api:0.0.0`
- `jboss-client:0.0.0`
- `lu.tudor.santec.i18n:0.0.0`

### repackaged WildFly/spec jar (15)
- `concurrent:0.0.0`
- `dsn:0.0.0`
- `flatlaf:3.5.4-no-natives`
- `javax.annotation-api:0.0.0`
- `javax.annotation:0.0.0`
- `javax.xml.soap-api:0.0.0`
- `jaxb-api-osgi:0.0.0`
- `jaxws-api:0.0.0`
- `jsr181-api:0.0.0`
- `org.eclipse.persistence.jpa.jpql_1.0.0:0.0.0`
- `pdftest:9.0.0`
- `pop3:0.0.0`
- `sam:0.0.0`
- `swingx:0.0.0`
- `webservices-api-osgi:0.0.0`

## Open Questions resolved

- *Which committed jars are repackaged/patched (≠ Central)?* → the 11 SHA_MISMATCH + the 'repackaged WildFly/spec jar' bucket above.
- *Residual set form* → still open (D3): in-project file repo vs private/Nexus. The residual set is now concretely the SHA_MISMATCH + NOT_ON_CENTRAL artifacts.
- *swingx/beansbinding/etc.* → see their rows in the TSV (most resolve to NOT_ON_CENTRAL / divergent).

## Task 1.1 — Root dependencyManagement scaffolding

Root `pom.xml` `<dependencyManagement>` now pins **145** of the 169 CENTRAL_OK artifacts (one per `groupId:artifactId`). These are inert until Phase 2 (no module references the real coordinates yet). The remaining 11 `groupId:artifactId` are vendored at multiple versions across modules and are deliberately **excluded** (a single root pin would upgrade the minority module — a Non-Goal). They are versioned per-module in Phase 2:

These `groupId:artifactId` are vendored at >1 version across modules. A single root pin would force an upgrade on the minority module (a Non-Goal). Each module must declare its version explicitly in Phase 2. Lifting them to one version each (with any required code changes) is the dedicated follow-up change **`consolidate-duplicate-dependency-versions`**.

### `com.google.guava:guava`
- `32.1.1-jre` — synthetic `jlawyer.thirdparty:guava:32.1.1-jre` — modules: j-lawyer-client
- `14.0.1` — synthetic `jlawyer.thirdparty:guava:14.0.1` — modules: j-lawyer-fax

### `com.sun.xml.bind:jaxb-impl`
- `2.3.3` — synthetic `jlawyer.thirdparty:jaxb-impl:2.3.3` — modules: j-lawyer-client
- `2.3.2` — synthetic `jlawyer.thirdparty:jaxb-impl:2.3.2` — modules: j-lawyer-server/j-lawyer-server-ejb

### `commons-logging:commons-logging`
- `1.2` — synthetic `jlawyer.thirdparty:commons-logging:1.2` — modules: j-lawyer-client, j-lawyer-server, j-lawyer-server-common, j-lawyer-server/j-lawyer-server-ejb
- `1.1` — synthetic `jlawyer.thirdparty:commons-logging:1.1` — modules: j-lawyer-fax

### `commons-net:commons-net`
- `3.5` — synthetic `jlawyer.thirdparty:commons-net:3.5` — modules: j-lawyer-server, j-lawyer-server-common, j-lawyer-server/j-lawyer-server-ejb
- `3.1` — synthetic `jlawyer.thirdparty:commons-net:3.1` — modules: j-lawyer-client

### `javax.activation:activation`
- `1.1` — synthetic `jlawyer.thirdparty:activation:1.1` — modules: j-lawyer-fax
- `1.1.1` — synthetic `jlawyer.thirdparty:activation:1.1.1` — modules: j-lawyer-server/j-lawyer-server-ejb

### `javax.xml.bind:jaxb-api`
- `2.2.7` — synthetic `jlawyer.thirdparty:jaxb-api:2.2.7` — modules: j-lawyer-fax
- `2.3.1` — synthetic `jlawyer.thirdparty:jaxb-api:2.3.1` — modules: j-lawyer-server/j-lawyer-server-ejb

### `junit:junit`
- `4.12` — synthetic `jlawyer.thirdparty:junit:4.12` — modules: j-lawyer-server-common, j-lawyer-server-entities
- `3.8.2` — synthetic `jlawyer.thirdparty:junit:3.8.2` — modules: j-lawyer-client, j-lawyer-fax, j-lawyer-server
- `4.10` — synthetic `jlawyer.thirdparty:junit:4.10` — modules: j-lawyer-client, j-lawyer-fax, j-lawyer-server

### `org.apache.commons:commons-lang3`
- `3.5` — synthetic `jlawyer.thirdparty:commons-lang3:3.5` — modules: j-lawyer-server, j-lawyer-server-common, j-lawyer-server/j-lawyer-server-ejb
- `3.12.0` — synthetic `jlawyer.thirdparty:commons-lang3:3.12.0` — modules: j-lawyer-client

### `org.apache.poi:poi`
- `5.2.3` — synthetic `jlawyer.thirdparty:poi:5.2.3` — modules: j-lawyer-client
- `4.1.2` — synthetic `jlawyer.thirdparty:poi:4.1.2` — modules: j-lawyer-server/j-lawyer-server-ejb

### `org.apache.poi:poi-scratchpad`
- `5.2.3` — synthetic `jlawyer.thirdparty:poi-scratchpad:5.2.3` — modules: j-lawyer-client
- `4.1.2` — synthetic `jlawyer.thirdparty:poi-scratchpad:4.1.2` — modules: j-lawyer-server/j-lawyer-server-ejb

### `org.slf4j:slf4j-api`
- `1.7.30` — synthetic `jlawyer.thirdparty:slf4j-api:1.7.30` — modules: j-lawyer-fax, j-lawyer-server, j-lawyer-server-common, j-lawyer-server-entities, j-lawyer-server/j-lawyer-server-io
- `1.7.36` — synthetic `jlawyer.thirdparty:slf4j-api:1.7.36` — modules: j-lawyer-client

