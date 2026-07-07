# j-lawyer-web — Supply-Chain-Härtung

Guardrails für die npm-Build-Kette des Web-UI-Moduls. Umsetzung von `design.md`
Decision 2c des Change `add-web-client`. **Diese Guardrails werden vor dem
Angular-Scaffolding etabliert** („Guardrails first").

## Bedrohungsmodell

Zwei Vektoren (siehe Decision 2c):

1. **Laufzeit (Browser)** — gelöst durch self-host + strikte CSP; **keinerlei
   Remote-Ressourcen**. Kein npm-Thema (nur Deployment-Config des WAR).
2. **Build-Zeit (npm)** — der eigentliche Supply-Chain-Vektor: bösartige/kompromittierte
   Pakete (transitiv), `pre`/`postinstall`-Skripte, Typosquatting. **Dieses Dokument
   adressiert Vektor 2.**

## Controls

### 1. Gepinnte, integritätsgeprüfte Abhängigkeiten
- `package-lock.json` **committen**; Installation ausschließlich via `npm ci`
  (nie `npm install` in CI/Release — `npm ci` schlägt bei Lockfile-Drift fehl).
- `save-exact=true` → exakte Versionen, keine `^`/`~`-Ranges.
- Lockfile enthält `integrity`-Hashes (SHA-512) je Paket → Manipulation wird erkannt.

### 2. Install-Skripte neutralisieren
- `ignore-scripts=true` in `.npmrc` → `pre`/`postinstall` von Abhängigkeiten werden
  **nicht** ausgeführt (Haupt-Angriffsfläche für „install-time"-Schadcode).
- **Gotcha**: Einzelne legitime Pakete des Angular-Toolchains (v. a. `esbuild`) nutzten
  historisch `postinstall`, um ein Plattform-Binary zu holen. Moderne Versionen liefern
  Plattform-Binaries über `optionalDependencies` (kein `postinstall` nötig) — im
  Scaffold-Spike (Task 1.2a) verifizieren. Falls doch ein Skript nötig ist: **vetted
  Allowlist** statt globalem Freischalten, z. B. via `@lavamoat/allow-scripts`
  (`allow-scripts run`) — nur explizit geprüfte Pakete dürfen Skripte ausführen.

### 3. Kontrollierte Registry (Analog zu `maven-repo/`)
Das Projekt spiegelt Maven-Artefakte bereits in-project (`maven-repo/`, git-ignoriert,
aus committeten `lib/`-Jars via `scripts/seed-maven-repo.sh` geseedet). npm-Analog:

- **Option A — vendored Offline-Cache (empfohlen, faithful zum Maven-Muster):**
  Die geprüften Paket-Tarballs werden committet (`vendor-npm/*.tgz`, Rolle wie `lib/`);
  `scripts/seed-npm-cache.sh` befüllt daraus den npm-Cache; anschließend löst
  `npm ci --offline --ignore-scripts` **vollständig offline** aus dem Cache auf. Der
  generierte Cache/`node_modules` ist git-ignoriert (Rolle wie `maven-repo/`).
- **Option B — Proxy-Registry (Verdaccio/Nexus):** interne Registry cached Upstream;
  `.npmrc` zeigt per `registry=` darauf. Betrieb eines Dienstes nötig — weniger faithful
  zum bestehenden file-basierten Muster, aber komfortabler bei vielen Updates.

**Zwei-Phasen-Workflow** (unvermeidbar — der erste Bezug muss irgendwo herkommen):
1. **Einmaliges, kontrolliertes Seeding** auf einer vertrauenswürdigen Maschine:
   `npm ci --ignore-scripts` gegen die öffentliche Registry, Lockfile + Integrity
   erzeugen, Tarballs nach `vendor-npm/` vendoren (`npm pack` je Abhängigkeit bzw.
   Cache-Export). Ergebnis wird geprüft und committet.
2. **Reproduzierbare Installs** danach: offline aus `vendor-npm/`/Cache, ohne Netz.
   Updates durchlaufen erneut Phase 1 (bewusst, review-pflichtig).

### 4. Scanning & Monitoring
- `npm audit --audit-level=high` als **CI-Gate** (nicht bei jedem lokalen Install →
  `audit=false` in `.npmrc`, dediziert in CI).
- **Dependabot** (bereits aktiv) für npm aktivieren.
- Reproduzierbare Builds ausschließlich in CI/auf der Build-Maschine, nicht auf
  Entwickler-Maschinen (Node-Toolchain nur zur Build-Zeit — kein Node in Produktion).

### 5. Kleinerer Abhängigkeitsbaum
Angular als kuratierter First-Party-Monorepo (Router/Forms/HTTP/CLI/Material aus einer
Hand) minimiert Dritt-transitive Pakete — kleinere Angriffsfläche als ein
handzusammengestellter Stack (siehe Decision 2b).

## Dateien in diesem Modul
Das npm/Angular-Projekt liegt unter `frontend/`; die Maven-WAR-Assembly im Modul-Root
(`pom.xml`, `src/main/webapp/`). npm läuft ausschließlich in `frontend/`.
- `frontend/.npmrc` — erzwingt `ignore-scripts`, `save-exact`, `package-lock`,
  `engine-strict`, `fund=false`, `audit=false` (Audit dediziert in CI).
- `frontend/.gitignore` — `node_modules/`, `dist/`, `.angular/` und der generierte
  npm-Cache sind nicht committet (Rolle wie `maven-repo/`); `package-lock.json`
  **wird** committet.
- `frontend/scripts/seed-npm-cache.sh` — befüllt den npm-Cache aus
  `frontend/vendor-npm/*.tgz` (Skelett; wird nach dem ersten Lockfile/Seeding in
  Phase 1 scharf geschaltet).

## Status
Guardrail-Konfiguration und -Policy sind **vor** dem Scaffolding etabliert. Das
tatsächliche Seeding (`vendor-npm/`, Cache) sowie das CI-Audit-Gate werden mit dem
Angular-Scaffold (Task 2.2/2.2a) und der CI-Anbindung scharf geschaltet.
