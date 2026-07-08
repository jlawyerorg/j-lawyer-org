## 1. Entscheidungsvorbereitung (Voraussetzung für Freigabe)

- [ ] 1.0 Design vorab exakt ausarbeiten (interaktiver Prototyp/Mockup via Claude-Artifacts) auf Basis Referenz-Screenshot + Logo-Farben; abnehmen lassen
- [ ] 1.1 Design-System-Ansatz festlegen (Tokens-Quelle, Komponentenbibliothek, Governance) — siehe `design.md` Decision 1
- [~] 1.1a Farb-Tokens festlegen: **Logo-Farben führend** (`#0e72b5`/`#de313b`/`#97bf0d`/`#666666` aus `ServerColorTheme`) als Primärtoken + Neutralfarben aus dem Screenshot (Navy `#0b1b2c`, Off-White, Creme) → in `frontend/src/styles/tokens.css` implementiert (hell/dunkel). Offen: finale Tints/Shades + WCAG-Kontrastprüfung; Layout NICHT aus dem Mockup ableiten
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
- [x] 1.7 REST-Gap-Analyse: 24 EJB-Remote-Interfaces vs. REST v1–v8; fehlende Endpunkte je Modul dokumentiert → siehe `gap-analysis.md`
- [ ] 1.8 Offene Fragen aus `design.md` beantworten und Entscheidungen dort festhalten

## 2. Phase 0 — Fundament

- [~] 2.1 Design-System-Paket / App-Shell — **responsive Shell implementiert & Build verifiziert**: `ShellComponent` (Grid Header/Modulleiste/Content/Bottom-Nav, Breakpoints Desktop→Icon-Rail→Bottom-Nav), `AppHeaderComponent`, `ModuleNavComponent` (Router-aktiv), `BottomNavComponent`, `ThemeService` (hell/dunkel via `data-theme`), CSP-sichere `IconComponent` (`@switch`, kein innerHTML), Routen aus Modul-Liste generiert, `ModulePlaceholderComponent`. Offen: Fehler-/Ladezustände, Komponentenkatalog (Storybook), echte Modulinhalte
- [~] 2.2 Neues Maven-Modul `j-lawyer-web` (eigenständiger WAR) — **erstellt & Build verifiziert**: Angular-19-Projekt in `frontend/` (standalone, Signals-first), `pom.xml` (`frontend-maven-plugin` + `maven-war-plugin`), `web.xml` SPA-Fallback, opt-in Profil `-Pweb`. `npm install` (848 Pakete, `package-lock.json` committet), `ng build` und `mvn -Pweb ... install` erzeugen `target/j-lawyer-web.war` erfolgreich; `deploy.sh` kopiert das WAR (wenn gebaut) nach WildFly → erreichbar unter `/j-lawyer-web`. Offen: Offline-Vendoring (`vendor-npm/` + `npm ci --offline`)
- [~] 2.2a Laufzeit-Härtung — **CSP-Baseline gesetzt**: alle Assets self-hosten (keine CDNs), CSP-`<meta>` in `index.html` (`default-src 'self'`; `script-src 'self'`). Offen: autoritative CSP-/Security-Header auf WildFly/Undertow (inkl. `frame-ancestors`), Nonce für Component-Styles (`ngCspNonce`), Trusted Types
- [x] 2.2b Build-Härtung — Guardrails-Config & Policy etabliert (vor dem Scaffold): `j-lawyer-web/.npmrc` (`ignore-scripts`, `save-exact`, `package-lock`, `engine-strict`, `audit=false`), `.gitignore`, `SUPPLY-CHAIN.md`, `scripts/seed-npm-cache.sh` (Analog zu `maven-repo/`). `esbuild`-`postinstall` verifiziert: Build gelingt mit `--ignore-scripts` (esbuild-Binary via optionalDependencies, kein Skript nötig). Offen: Offline-Vendoring (`vendor-npm/`), `npm audit`-CI-Gate + Dependabot scharf schalten
- [x] 2.2d i18n-Fundament (Transloco) — **implementiert & verifiziert**: DE/EN, Laufzeit-Umschaltung im Header, self-hosted JSON (`public/i18n/*.json`), `core/i18n.ts` als zentrale Sprachliste (neue Sprache = 1 JSON + 1 Eintrag), Persistenz via `localStorage`, Init aus gespeichert>Browser>Default. Alle Shell-Strings über Transloco-Keys. Verifiziert per Headless-Chromium (DE↔EN wechselt Labels/Suche/Überschrift). Offen: `TranslocoTitleStrategy` für übersetzte Seitentitel
- [ ] 2.3 Login-/Auth-Flow implementieren (siehe `design.md` Decision 5 — additiv, kompatibel):
  - [~] 2.3a Server: Elytron-Bearer-Mechanismus (`BEARER_TOKEN` + `token-realm`, RS256) **neben** BASIC — **umgesetzt in `docker/wildfly/standalone.xml`**: `key-store jlawyer-jwt-keystore` (PKCS12), `token-realm jlawyer-jwt-realm` (`sub`-Principal, `roles`-Claim), Realm in `jlawyer-security-domain` (Decoder `from-roles-attribute` → Identität propagiert zur EJB-Ebene), `http-authentication-factory jlawyer-http-authentication` (BASIC+BEARER_TOKEN), Undertow-`application-security-domain` → Factory mit `override-deployment-config`; EJB-Mapping bleibt `security-domain`. `/rest/v8/auth/*` in `web.xml` öffentlich; 401 ohne `WWW-Authenticate: Basic`. Setup dokumentiert in `j-lawyer-io/AUTH-SETUP.md`. **E2E am laufenden WildFly 26.1.3 verifiziert (2026-07-08):** Bearer-Zugriff auf `/v1/cases/list/active` (filtert intern per `getCallerPrincipal()`) → 200, d.h. JWT-Identität propagiert zur EJB-Ebene. *(Option A verworfen — s. Decision 5, revidiert 2026-07-08.)*
  - [~] 2.3b Server: `POST v8/auth/login|refresh|logout` — **implementiert**: `AuthenticationEndpointV8` (+ pojos `LoginRequestV8`/`TokenResponseV8`), RS256-JWT-Access-Token + httpOnly-Refresh-Cookie (`JLAWYER_REFRESH`, SameSite=Lax, Secure wenn HTTPS); Credential-Prüfung via neuer `@PermitAll`-Methode `SecurityService.authenticateAndGetRoles` (`findByPrincipalIdUnrestricted` + `PasswordsUtil`-Hash, konstante Zeit + `security_roles`); RS256-JWT-Util (JDK-only) `JwtService`/`JwtClaims`/`JwtException` in `j-lawyer-server-common` + Unit-Test (6/6 grün); `JwtKeyProvider` lädt Schlüssel aus PKCS12; in `EndpointServiceLocator` registriert; `otp`-Feld reserviert. 2FA nur am Login (mit `add-two-factor-auth` koordinieren). **E2E am laufenden Server verifiziert (2026-07-08):** Login `admin/a`→200+Cookie, ohne Token→401, mit Bearer→200, Refresh→200 (Token rotiert), Logout→204 dann Refresh→401, Falschpasswort→401 ohne `WWW-Authenticate`.
  - [~] 2.3c Client: Login-Route, `AuthService` (Signal-State), `HttpInterceptor` (Bearer + 401-Handling), Route-Guard; Access-Token nur im Speicher — **implementiert gegen Mock-Backend & verifiziert**: `LoginComponent` (Reactive-Form, i18n, hell/dunkel, Demo-Hinweis admin/a), `AuthBackend` (Mock, `sessionStorage`-Refresh-Marker als Platzhalter fürs httpOnly-Cookie), `AuthService` (Signal-Session, Token nur in-memory, `restore()`), funktionaler `authInterceptor` (Bearer nur an API-URLs, 401→Logout), `authGuard` (`canActivateChild`, Redirect `/login?returnUrl`), Shell als bewachte Layout-Route + `/login` außerhalb, `provideAppInitializer` für Silent-Restore, Logout + Nutzer-Initialen im Header. Verifiziert per Headless-Chromium: Unauth→Redirect, Falsch-Login→Fehler, admin/a→`/akten`, Reload→Session bleibt, Logout→`/login`. **Offen: Umstellung des `AuthBackend`-Mocks auf echte REST-Endpunkte (2.3b)**
  - [~] 2.3d Verifizieren: Basic-REST-Client und Swing-Client weiterhin unverändert funktionsfähig (Kompatibilität) — **Basic-REST verifiziert (2026-07-08):** `curl -u admin:a /v1/cases/list/active` → 200 unverändert neben aktivem Bearer. **Offen: Swing-Client (EJB-Remoting) gegengeprüft**
- [ ] 2.4 Fehlende Basis-REST-Endpunkte additiv in neuer API-Version ergänzen (ohne bestehende Versionen zu brechen)

## 3. Phase 1 — MVP (Read-first)

- [~] 3.1 Fallliste + Suche + Falldetail — **UI implementiert & verifiziert**: `AktenComponent` (responsives Master-Detail), filterbare/durchsuchbare Liste mit Status-Pills, Detail mit Reiter „Übersicht" (Beteiligte, Fristen, letzte Dokumente, Notiz), i18n (DE/EN), Logo-Tokens, hell/dunkel. `CasesService` mit REST-kompatiblen Modellen (`RestfulCaseV1`/`Party`/`DueDate`). Verifiziert per Headless-Chromium (Liste/Auswahl/Filter/Sprachwechsel). **Offen: Anbindung an echte REST-API — braucht Auth-Flow (2.3); aktuell Sample-Daten im `CasesService`**
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
