## 1. Entscheidungsvorbereitung (Voraussetzung für Freigabe)

- [ ] 1.0 Design vorab exakt ausarbeiten (interaktiver Prototyp/Mockup via Claude-Artifacts) auf Basis Referenz-Screenshot + Logo-Farben; abnehmen lassen
- [ ] 1.1 Design-System-Ansatz festlegen (Tokens-Quelle, Komponentenbibliothek, Governance) — siehe `design.md` Decision 1
- [ ] 1.1a Farb-Tokens festlegen: **Logo-Farben führend** (`#0e72b5`/`#de313b`/`#97bf0d`/`#666666` aus `ServerColorTheme`) als Primärtoken + Neutralfarben aus dem Screenshot (Navy `#0b1b2c`, Off-White, Creme) → Tints/Shades, WCAG-Kontraste; Layout NICHT aus dem Mockup ableiten
- [ ] 1.1a-2 Typografie festlegen: self-hostbare Open-Source-Grotesk-Sans (im WAR gebündelt); exakte `font-family` aus dem CSS der Live-Website bestätigen
- [ ] 1.1b Responsive Breakpoints + adaptive Layout-Muster definieren (Desktop/Tablet/Smartphone, Tabelle→Karte) — siehe `design.md` Decision 3a
- [x] 1.2 SPA-Framework per gewichteter Bewertung entscheiden → **Angular** (siehe `design.md` Decision 2b)
- [ ] 1.2a Bestätigungs-Spike Angular: ein Modul (Fallliste + Falldetail + Datentabelle) mit Logo-Tokens; Entwickler bewerten die **Lesbarkeit/Verständlichkeit** des von Claude generierten Codes
- [ ] 1.2d Frontend-Coding-Standards für AI-Generierung festlegen: ESLint + Prettier, CLI-Schematics, Signals-first (RxJS nur wo nötig), Frontend-Styleguide/AGENTS.md für den Assistenten
- [ ] 1.2b Kostenfreie Komponentenbasis festlegen: Angular Material + CDK (MIT) und/oder PrimeNG (MIT); Grid nur AG Grid Community (MIT), Enterprise/Kendo/Syncfusion/DevExtreme/Vaadin Pro ausschließen
- [ ] 1.2c Custom-Component-Machbarkeit via CDK verifizieren (Beispiel: eigene Komponente ohne Fertigbaustein) und `ng update`-Migrationspfad dokumentieren
- [x] 1.3 Deployment-Modell entschieden: **eigenständiger WAR `j-lawyer-web`, separat auf derselben WildFly** (siehe `design.md` Decision 2a)
- [ ] 1.4 Auth-Modell festlegen (session-/tokenbasiert) und mit `add-two-factor-auth` abstimmen
- [ ] 1.5 Push-Mechanismus wählen (WebSocket/SSE/Polling) für Messenger, Erinnerungen, Status-Badges
- [ ] 1.6 Office-Editing-Strategie entscheiden (WOPI via Collabora/OnlyOffice als Ziel; Download/Upload als MVP-Fallback)
- [ ] 1.7 REST-Gap-Analyse: 24 EJB-Remote-Interfaces vs. vorhandene REST v1–v8; fehlende Endpunkte je Modul dokumentieren
- [ ] 1.8 Offene Fragen aus `design.md` beantworten und Entscheidungen dort festhalten

## 2. Phase 0 — Fundament

- [ ] 2.1 Design-System-Paket: Tokens, responsive App-Shell (adaptive Modulleiste/Bottom-Nav, Auth, Fehler-/Ladezustände), Komponentenkatalog
- [ ] 2.2 Neues Maven-Modul `j-lawyer-web` (eigenständiger WAR): `frontend-maven-plugin` baut das Angular-Bundle, `maven-war-plugin` verpackt die statischen Assets + SPA-Fallback (unbekannte Pfade → `index.html`); `deploy.sh` um das zweite Artefakt erweitern
- [ ] 2.2a Laufzeit-Härtung: alle Assets self-hosten (keine CDNs), strikte CSP als WildFly-/`web.xml`-Header (`default-src 'self'` …), Nonce für Component-Styles (`ngCspNonce`), Trusted Types aktivieren
- [ ] 2.2b Build-Härtung gegen Supply-Chain: `npm ci --ignore-scripts`, gepinntes Lockfile mit Integrity-Hashes, gespiegelte/vendorte npm-Registry (analog `maven-repo/`), `npm audit`/Dependabot im CI
- [ ] 2.3 Login-/Auth-Flow implementieren (Same-Origin, sichere Session/Token)
- [ ] 2.4 Fehlende Basis-REST-Endpunkte additiv in neuer API-Version ergänzen (ohne bestehende Versionen zu brechen)

## 3. Phase 1 — MVP (Read-first)

- [ ] 3.1 Fallliste + Suche + Falldetail
- [ ] 3.2 Dokumentenliste + Download + Vorschau (PDF nativ; Office via Konvertierung/Viewer)
- [ ] 3.3 Kontakte (Liste/Detail/Suche)
- [ ] 3.4 Kalender-/Wiedervorlagen-Ansicht
- [ ] 3.5 Office-Bearbeitung via Download → lokal bearbeiten → Re-Upload (Fallback)

## 4. Phase 2 — Kernmodule (Write)

- [ ] 4.1 Fälle/Dokumente/Kontakte/Kalender voll editierbar (inkl. Beteiligte, Notizen, Fristen)
- [ ] 4.2 Zeiterfassung
- [ ] 4.3 Volltextsuche (Lucene) mit Trefferliste/Relevanz
- [ ] 4.4 E-Mail-Client (IMAP/SMTP) inkl. Vorlagen

## 5. Phase 3 — Finanzen & Kommunikation

- [ ] 5.1 Rechnungen/Zahlungen inkl. ZUGFeRD/XRechnung, Girocode/EPC-QR
- [ ] 5.2 Messenger mit serverseitigem Push
- [ ] 5.3 beA-Postfach
- [ ] 5.4 Reporting (Tabellen/Diagramme)

## 6. Phase 4 — Parität & Komfort

- [ ] 6.1 In-Browser-Office-Editing via WOPI (Collabora/OnlyOffice)
- [ ] 6.2 Serienschreiben (Mass Mail)
- [ ] 6.3 KI-Assistent (Ingo) inkl. Tool-Calling/Approval
- [ ] 6.4 Administration/Einstellungen (Nutzer, Rollen, Systemkonfiguration)
- [ ] 6.5 VoIP/Fax/E-POST (Click-to-Dial, Statusverfolgung)
- [ ] 6.6 Datei-Upload-Ersatz für Scanner-Workflow (inkl. Mobile-Kamera-Upload)

## 7. Übergreifend

- [ ] 7.1 Barrierefreiheit (WCAG/BITV) und i18n (DE primär) durchgängig prüfen
- [ ] 7.2 Visual-Regression-/Lint-Gates für Design-Konsistenz in CI — über alle Breakpoints (Desktop/Tablet/Smartphone)
- [ ] 7.4 Responsive/Touch-Tests auf realen Geräteklassen; optional PWA-Ausbaustufe evaluieren
- [ ] 7.3 Dokumentation: bewusst nicht abgebildete Desktop-Funktionen (Scanner/TWAIN, Diktierhardware, Ordner-Watch)
