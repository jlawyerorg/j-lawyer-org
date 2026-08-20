# j-lawyer-web

Standalone Angular web UI, packaged as its own WAR and deployed separately on the same
WildFly as the EAR (OpenSpec change `add-web-client`, `design.md` Decision 2a).

- **Framework:** Angular 19 (standalone components, Signals-first).
- **Layout:** `frontend/` = npm/Angular project (npm runs here); module root = Maven WAR
  assembly (`pom.xml`, `src/main/webapp/WEB-INF/web.xml`).
- **Supply chain:** see `SUPPLY-CHAIN.md`. npm is hardened via `frontend/.npmrc`
  (`ignore-scripts`, `save-exact`, `package-lock`, `audit` as a CI gate).
- **Runtime security:** no remote code — self-hosted assets + strict CSP (baseline in
  `frontend/src/index.html`, authoritative headers at the WildFly/Undertow layer).

## Status

Scaffold authored **without** running any install/build (builds are manual in this
project). Before the first build, the npm dependencies must be seeded once (Phase 1).

## Build

The module is **opt-in** so it does not affect the default reactor build:

```bash
# Phase 1 — one-time, on a trusted machine: produce package-lock.json + vendored tarballs
cd j-lawyer-web/frontend
npm install --ignore-scripts        # online, scripts disabled; review the result
#   -> commit package-lock.json; vendor tarballs into frontend/vendor-npm/ (see SUPPLY-CHAIN.md)

# Phase 2 — reproducible build via Maven (offline npm ci + ng build + WAR packaging)
export JAVA_HOME=/home/jens/bin/jdk-17.0.9-full
mvn -Pweb -pl j-lawyer-web -am install
#   For the very first Maven build (before vendoring), override the install args:
#   mvn -Pweb -pl j-lawyer-web -am install -Dnpm.install.args="ci --ignore-scripts"
```

The WAR is written to `target/j-lawyer-web.war`. Deploy it alongside the EAR
(`deploy.sh` to be extended for the second artifact).

## Dev server

```bash
cd frontend && npm run start   # http://localhost:4200 (proxy the REST API as needed)
```
