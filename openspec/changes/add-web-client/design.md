## Context

Der `j-lawyer-client` (Package `com.jdimension.jlawyer.client`) ist eine umfangreiche
Swing-Anwendung. Die Modulinventur (aus `editors/` und den Top-Level-Paketen) ergibt
folgende funktionale Bereiche, die eine Web-UI abdecken müsste:

| Bereich | Client-Paket(e) | Kurzbeschreibung |
|---|---|---|
| Fälle / Akten | `editors/files` | Akten anlegen/suchen, Beteiligte, Fristen/Wiedervorlagen (Reviews), Historie |
| Dokumente | `editors/documents`, `launcher/` | Dokumentenverwaltung, Viewer, Scannen, Office-Bearbeitung |
| Kontakte | `editors/addresses` | Adressen/Beteiligte, Schnellsuche |
| Kalender | `calendar/`, `editors/files` (Events) | Termine, Fristen, Erinnerungen |
| Finanzen | `editors/finance` | Rechnungen, Zahlungen, Kontoauszug-Import, ZUGFeRD/XRechnung |
| Zeiterfassung | `editors/files` (Timesheet) | Zeiterfassung, Abrechnung |
| Suche | `editors/search` | Volltext-/Dokumentensuche (Lucene) |
| E-Mail | `mail/`, `massmail/` | E-Mail-Client, Serienmails |
| beA | `bea/` | beA-Postfach (serverseitig via beAstie) |
| Messenger | `messenger/` | Interner Kanzlei-Chat |
| Reporting | `editors/reporting` | Auswertungen/Berichte |
| Recherche | `editors/research` | Rechercheintegrationen |
| VoIP/Fax | `voip/` | Telefonie/Fax (Sipgate) |
| Vorlagen | `templates/` | Dokumentvorlagen mit Platzhaltern |
| Assistent | `assistant/` | KI-Assistent (Ingo) |
| Admin/Settings | `configuration/`, `settings/`, `security/` | Nutzer, Rollen, Systemkonfiguration |
| Monitoring | `monitoring/` | Systemüberwachung |

Server-seitig existieren **24 EJB-Remote-Interfaces** (`j-lawyer-server-api/*Remote.java`)
und eine **REST-API v1–v8** (`j-lawyer-io`) mit bereits guter, aber unvollständiger
Abdeckung: Cases, Contacts, Calendar, Invoices, Payments, Timesheets, Email, Messaging,
Reports, Search, beA, Templates, Forms, Security, Administration, Configuration,
WebHooks, DataBucket. Der Desktop-Client nutzt heute **EJB-Remoting** (JBoss Remoting)
über `JLawyerServiceLocator`, **nicht** die REST-API.

**Autoren-/Wartungsmodell (wichtige Randbedingung):** Ein Großteil der Web-Anwendung
wird von **Claude Code generiert**; die Entwickler müssen den Code primär **lesen,
verstehen und reviewen** können — nicht überwiegend selbst von Grund auf schreiben.
Das verschiebt die Technologiekriterien: **Lesbarkeit/Verständlichkeit** und
**Konsistenz des generierten Codes** wiegen schwerer als die Autoren-Lernkurve; ein
großer, hochwertiger AI-Trainingskorpus des Frameworks erhöht die Qualität des
generierten Codes.

## Goals / Non-Goals

**Goals**
- Funktionale Parität mit dem Swing-Client als Zielbild (schrittweise, priorisiert).
- Einheitliches, wartbares Design über alle Module.
- Zero-Install-Zugriff über den Browser; **responsiv nutzbar auf Desktop, Tablet und
  Smartphone** (ein einziges responsives UI, keine getrennten Codebasen pro Gerät).
- Koexistenz: Swing-Client und Web-UI laufen parallel gegen denselben Server.
- Wiederverwendung der bestehenden Server-Logik über die REST-API.

**Non-Goals**
- Kein Ersatz des Servers oder der Datenbank; keine Änderung der EJB-Geschäftslogik.
- Keine sofortige 1:1-Parität — Desktop-spezifische Funktionen (natives Office-Editieren,
  TWAIN-Scannen) werden bewusst anders gelöst.
- Kein Abschalten des Swing-Clients im Rahmen dieses Changes.
- Keine **native** Mobile-App — stattdessen ein responsives Web-UI, das auch auf dem
  Smartphone läuft (die REST-API bedient bestehende native Apps weiterhin separat).
- Keine vollständige Desktop-Dichte auf dem Smartphone-Formfaktor — dichte Bulk-/
  Konfigurations-Workflows bleiben Desktop-optimiert (funktional aber erreichbar).

## Decision 1 — Einheitliches Design sicherstellen

**Entscheidung**: Ein verbindliches **Design-System** ist Voraussetzung und Teil des
MVP, nicht ein späterer Politur-Schritt. Das Design orientiert sich **nicht** am
bestehenden FlatLaf-Look; Wiedererkennungswert entsteht über die **Logo-Farben** und
eine vorab exakt ausgearbeitete Zielgestaltung.

**Design-Inputs (vor Implementierung festzulegen):**
- **Logo-Farbpalette** als Marken-/Akzentfarben, nachgenutzt aus
  `j-lawyer-server-common/src/main/java/org/jlawyer/themes/ServerColorTheme.java`:
  Rot `#de313b` (`COLOR_LOGO_RED`), Grün `#97bf0d` (`COLOR_LOGO_GREEN`),
  Blau `#0e72b5` (`COLOR_LOGO_BLUE`), Grau `#666666` (`COLOR_DARK_GREY`) plus abgeleitetes
  Hellgrau. Diese Farben bilden die Basis der Marken-Tokens (Primär/Sekundär/Akzent,
  Statusfarben) — ergänzt um systematisch abgeleitete Tints/Shades für Flächen,
  Zustände und Kontraste (WCAG-konform).
- **Referenz-Screenshot** `20260707_website-mockup.png` (in diesem Change-Ordner) — die
  j-lawyer.org-Marketing-Website. **Nur für Farbgebung und Typografie** heranziehen,
  **nicht** als Layout-Vorlage (das Web-UI-Layout folgt dem responsiven Master-Detail-
  Muster aus Decision 3a, nicht dem Marketing-Seitenaufbau).
- **Farbpalette** — **die Logo-Farben aus `ServerColorTheme` sind führend** (verbindliche
  Primärtoken); die aus dem Screenshot gesampelten Werte dienen nur als Referenz. Die
  Neutralfarben werden aus dem Screenshot übernommen, da das Logo dafür keine Töne liefert:

  | Rolle | Verbindlicher Token | Website-Referenz | Herkunft |
  |---|---|---|---|
  | Blau (Primär/Buttons) | **`#0e72b5`** | `#1469c0` | `COLOR_LOGO_BLUE` (führend) |
  | Rot (CTA/Warnung) | **`#de313b`** | `#c93535` | `COLOR_LOGO_RED` (führend) |
  | Grün (Akzent/Erfolg) | **`#97bf0d`** | `#97bf0d` | `COLOR_LOGO_GREEN` (führend, identisch) |
  | Neutralgrau | **`#666666`** (+ abgeleitetes Hellgrau) | — | `COLOR_DARK_GREY` (führend) |
  | Navy (dunkle Flächen/Header) | `#0b1b2c` | `#0b1b2c` | Screenshot (Neutralfarbe, kein Logo-Ton) |
  | Off-White (Flächen) | ~`#f4f4f2` | ~`#f4f4f2` | Screenshot (Neutralfarbe) |
  | Warmes Creme/Beige | ~`#f0ece4` / `#ddd9d3` | ~`#f0ece4` | Screenshot (Neutralfarbe) |

  Aus diesen Grundwerten werden systematisch Tints/Shades für Flächen, Zustände und
  Kontraste abgeleitet (WCAG-konform); exakte Hex-Werte der Ableitungen werden in der
  Design-Ausarbeitung finalisiert.
- **Typografie**: moderne, humanistische **Grotesk-Sans**, kräftige (semibold/bold)
  Überschriften, klarer Fließtext (Anmutung des Screenshots). Wegen „keine Lizenzkosten"
  + Self-Contained-Deployment (Schrift im WAR gebündelt, kein externes CDN) eine
  **self-hostbare Open-Source-Schrift** (z. B. Inter oder vergleichbar); die exakte
  `font-family` ist aus dem CSS der Live-Website zu bestätigen, bevor sie fixiert wird.
- **Vorab-Design-Ausarbeitung mit Claude (Artifacts/„Claude design")**: das gewünschte
  Design wird vor Implementierungsbeginn als interaktiver Prototyp/Mockup exakt
  ausgearbeitet und abgenommen; daraus werden die Tokens und der Komponentenkatalog
  abgeleitet. Diese Ausarbeitung ist Voraussetzung für die Freigabe (siehe tasks.md).

Bausteine:
1. **Design Tokens** (Farben, Typografie, Abstände, Radien, Elevation, Zustände) als
   Single Source of Truth (z. B. CSS Custom Properties / Style-Dictionary), abgeleitet
   aus der oben genannten Logo-Palette und der abgenommenen Design-Ausarbeitung.
2. **Komponentenbibliothek**: Auswahl **einer** ausgereiften Bibliothek statt
   Eigenbau (z. B. Material-basiert oder Headless + Tokens). Kriterien:
   Barrierefreiheit (WCAG/BITV — für Kanzleisoftware relevant),
   Datentabellen/Grids (der Client ist tabellenlastig), Formulare/Validierung,
   Internationalisierung (DE primär), **touch-taugliche, responsive Komponenten
   (mobile-first, adaptive Datentabellen)**, Wartungsstatus, Lizenz (AGPL-kompatibel).
3. **Verbindliche Interaktionsmuster** dokumentiert: **responsives** Master-Detail-Layout
   wie im Client (Modulleiste → Liste → Editor), das je Breakpoint adaptiert (siehe
   Decision 3a), Formular-/Speicher-/Fehlerkonventionen, Ladezustände, Leerzustände,
   Bestätigungsdialoge — jeweils für Zeiger- **und** Touch-Bedienung.
4. **Governance**: Storybook/Komponentenkatalog als lebende Referenz; PR-Checkliste
   „nur Bibliothekskomponenten + Tokens, keine Ad-hoc-Styles"; automatisierte Lint-/
   Visual-Regression-Prüfung **über alle definierten Breakpoints**.
5. **Ein Frontend, viele Module**: eine einzige responsive SPA-Codebasis mit gemeinsamem
   App-Shell (Navigation, Auth, Fehlerbehandlung), damit Module nicht divergieren — **eine**
   Codebasis für alle Gerätegrößen statt getrennter Desktop-/Mobile-Fassungen.

## Decision 3a — Responsives Layout (Desktop / Tablet / Smartphone)

**Entscheidung**: Ein einziges **responsives** UI (mobile-first, fluide) statt getrennter
Codebasen. Definierte Breakpoints steuern eine adaptive App-Shell:

- **Desktop (≥ ~1024px)**: volle Dichte — persistente Modulleiste, mehrspaltiges
  Master-Detail (Liste + Editor nebeneinander), volle Datentabellen.
- **Tablet (~600–1024px)**: einklappbare Modulleiste, Master-Detail bei Portrait
  gestapelt/als Drawer, reduzierte Tabellenspalten.
- **Smartphone (< ~600px)**: navigations-getriebener Single-Pane-Flow (Liste →
  Detail als Push-Navigation), Modulleiste als Bottom-Nav/Hamburger, **Datentabellen
  werden zu Karten-/Listenansichten** (die tabellenlastigen Client-Ansichten lassen
  sich auf dem Telefon nicht 1:1 zeigen), Aktionen in Sheets/Kontextmenüs.

Grundsätze: mobile-first CSS, relative Einheiten, `max-width:100%` für Medien, kein
horizontales Scrollen der Seite (breite Inhalte scrollen in eigenem Container),
Touch-Ziele ≥ 44px, Tastatur-/Zeiger- und Touch-Bedienung gleichwertig. Auf dem
Smartphone liegt der Fokus auf den mobil sinnvollen Aufgaben (Fall-/Kontakt-/
Dokumentenzugriff, Kalender, Zeiterfassung, Messenger, Benachrichtigungen);
dichte Bulk-/Konfigurations-Workflows bleiben primär Desktop-optimiert, sind aber
funktional erreichbar.

## Decision 2 — Technologie- und Deployment-Auswahl

**Auswahlkriterien** (gewichtet, in Reihenfolge der Bedeutung):
0. **Lesbarkeit/Verständlichkeit & AI-Generierbarkeit** — der Code wird primär von
   Claude Code geschrieben und von den Entwicklern gelesen/reviewt: explizite,
   strukturierte, „magiearme" Codebasis; großer AI-Trainingskorpus für hochwertigen,
   idiomatischen Output; einheitliche Patterns für konsistentes Reviewen im Großen.
1. **Deployment-Fit** — läuft es als WAR auf demselben WildFly 26.1.3? Ein
   statisch gebautes SPA-Bundle in einer WAR (analog zur bestehenden `swagger-ui/`
   im `j-lawyer-io`-WAR) ist deploybar wie jedes andere Artefakt und braucht keinen
   zusätzlichen Node-Laufzeitprozess in Produktion.
2. **Wartbarkeit für ein kleines Team** — langfristige Wartung durch Lesen/Reviewen;
   Java-nahe, explizite Struktur erleichtert das Verstehen (Autoren-Lernkurve nachrangig,
   da AI-generiert).
3. **Reife & Ökosystem** — Datentabellen, Formulare, Kalender-Komponenten.
4. **API-Stabilität** — das Framework soll nicht regelmäßig große Teile seiner APIs
   signifikant brechen; Breaking Changes müssen angekündigt und automatisiert
   migrierbar sein.
5. **Customizing / eigene Komponenten** — es muss möglich sein, eigene Komponenten zu
   bauen, wenn die Bibliothek nichts Passendes bietet (headless Primitive erwünscht).
6. **Keine laufenden Lizenzkosten** — weder Framework noch benötigte
   UI-Komponenten dürfen wiederkehrende Lizenzkosten verursachen.
6a. **Supply-Chain-Angriffsfläche** — kleiner Dritt-Abhängigkeitsbaum; keine
   Remote-Ressourcen zur Laufzeit (self-host + strikte CSP durchsetzbar); Build-Zeit-
   Abhängigkeiten kontrollierbar (siehe Decision 2c).
7. **Responsive-/Mobile-Fähigkeit** — touch-taugliche, adaptive Komponenten und ein
   ausgereiftes Breakpoint-/Layout-Modell (siehe Decision 3a); optional PWA-fähig
   (Installierbarkeit/Offline-Shell) als spätere Ausbaustufe.
8. **Barrierefreiheit & i18n**.
9. **Lizenz** — AGPLv3-kompatibel (permissive Lizenz wie MIT/Apache).
10. **Build-Integration** — muss sich in den Maven-Reactor einfügen.

**Bewertete Optionen**:

| Option | Deployment | Für/Wider |
|---|---|---|
| **SPA (React/Angular/Vue/Svelte) als statisches Bundle in einer WAR** | ✅ WAR auf WildFly, kein Node in Prod | Sauberste Trennung Frontend/Backend; REST-API bereits vorhanden; großes Ökosystem für Tabellen/Formulare/Kalender. Node nur zur Build-Zeit (via `frontend-maven-plugin`). **Empfehlung.** |
| **Jakarta Faces (JSF/PrimeFaces) im WAR** | ✅ nativ WildFly | Serverseitig, „javaesk", eine Sprache. Aber: schwergewichtiger State, weniger fließende UX, schlechter für tablet-taugliche, hochinteraktive Oberflächen; Komponenten-Look schwer an FlatLaf anzugleichen. |
| **Vaadin (Java-only UI)** | ✅ WAR/Spring | Kein JS nötig, Java-Team-freundlich; aber lizenz-/kostensensibel für Pro-Komponenten, eigenes Programmiermodell, weniger REST-orientiert. |
| **Server-rendered (Thymeleaf + htmx)** | ✅ WAR | Leichtgewichtig, wenig JS; aber komplexe Master-Detail-/Kalender-/Editor-Interaktionen des Clients werden schnell aufwendig. |
| **Separates SPA hinter eigenem Webserver (nginx/Node)** | ⚠️ zusätzlicher Prozess | Flexibel, aber bricht das „ein Container"-Betriebsmodell und erhöht Ops-Aufwand. |

**Empfehlung**: **SPA als statisches Bundle, verpackt in einer WAR**, die auf demselben
WildFly deployt wird und dieselbe REST-API konsumiert. Das erfüllt das
Deployment-Kriterium („WAR-Deployment charmant") ohne einen zweiten Laufzeitprozess und
hält Frontend/Backend sauber getrennt.

### Decision 2a — Packaging: eigenständiger WAR (fixiert)

**Entscheidung**: Die Web-UI wird als **eigenständiger WAR** (`j-lawyer-web`) gebaut und
**separat** auf derselben WildFly-Instanz deployt — **nicht** in das
`j-lawyer-server.ear` eingebettet und **nicht** als Overlay in den bestehenden
`j-lawyer-io`-WAR gemischt.

Der WAR enthält nur das statische Angular-Bundle (keine Servlets/JAX-RS) plus eine
SPA-Fallback-Regel (unbekannte Pfade → `index.html`); WildFly serviert die Assets als
statischen Content.

Begründung:
- **Unabhängige, schnelle Redeploys**: Die UI iteriert deutlich schneller als der Server
  (Code großteils von Claude Code generiert). Ein wenige-MB-WAR neu zu deployen statt des
  ganzen EAR spart im Alltag spürbar Zeit.
- **Saubere Build-Isolation**: Die Node-Toolchain bleibt in einem eigenen Maven-Modul,
  statt in die EAR-Assembly verflochten zu werden.
- **Kein Auth-/CORS-Nachteil**: Origin = Schema+Host+Port; der Context-Pfad
  (`/j-lawyer-web` vs. `/j-lawyer-io`) zählt nicht. Auf derselben WildFly-Instanz ist
  alles same-origin — keine CORS-Sonderbehandlung, Cookies funktionieren sauber.
- **Versionsdisziplin bereits gelöst**: Die UI spricht die versionierte REST-API (vN),
  die genau für UI↔Server-Versionsversatz ausgelegt ist.

Kosten/Restrisiko:
- `deploy.sh` kopiert künftig **zwei** Artefakte (EAR + WAR); UI und Server müssen grob
  in Sync gehalten werden (durch REST-Versionierung entschärft).
- **Reversibel**: Da es nur statische Assets + Build-Verdrahtung sind, lässt sich der WAR
  bei geänderten Ops-Präferenzen später mit geringem Aufwand ins EAR falten.

### Decision 2b — SPA-Framework: Angular (empfohlen)

Entscheidungsgrundlage (Auftraggeber-Input): Team **primär Java, wenig JS/TS**,
**kleines dauerhaftes Inhouse-Team**, Priorität **reifes Enterprise-Ökosystem
„aus einer Hand"**; **keine Lizenzkosten**, **API-Stabilität**, **Customizing möglich**;
und — entscheidend — **der Code wird großteils von Claude Code geschrieben, die
Entwickler lesen/verstehen/reviewen ihn primär** (Autoren-Lernkurve daher nachrangig,
Lesbarkeit und Konsistenz vorrangig).

Gewichtete Bewertung (Score 1–5 × Gewicht, Maximum 500):

| Kriterium | Gewicht | Angular | React | Vue | Svelte |
|---|---:|---:|---:|---:|---:|
| Lesbarkeit/Verständlichkeit für Leser (Struktur, Explizitheit, wenig „Magie") | 18 | 4 | 3 | 4 | 5 |
| Enterprise-Ökosystem „aus einer Hand" | 18 | 5 | 4 | 3 | 2 |
| AI-Generierbarkeit & Konsistenz (großer Korpus, einheitliche Patterns) | 14 | 5 | 5 | 4 | 3 |
| Struktur/Konventionen & Konsistenz über Module | 12 | 5 | 3 | 4 | 3 |
| API-Stabilität (keine häufigen Breaking Changes) | 10 | 4 | 5 | 3 | 2 |
| Customizing / eigene Komponenten möglich | 8 | 5 | 5 | 4 | 4 |
| Supply-Chain-Angriffsfläche (kleiner Dritt-Abhängigkeitsbaum) | 6 | 5 | 3 | 4 | 4 |
| Keine laufenden Lizenzkosten (Framework + Komponenten) | 5 | 5 | 5 | 5 | 5 |
| Langzeit-Reife / Community | 5 | 5 | 5 | 4 | 3 |
| Responsive-/Mobile-Komponenten | 2 | 4 | 5 | 4 | 4 |
| Build-/WAR-Integration (statisches Bundle) | 2 | 5 | 5 | 5 | 5 |
| **Gewichtete Summe** | **100** | **470** | **410** | **379** | **338** |

**Ergebnis: Angular.** Begründung im Autoren-/Leser-Modell:
- **Konsistenz fürs Reviewen (Hauptvorteil bei AI-Autorenschaft)**: Wenn Claude Code den
  Großteil schreibt, ist Gleichförmigkeit über tausende Zeilen entscheidend. Angulars
  „einen Weg"-Ansatz (CLI-Schematics, DI, getrennte `.ts`/`.html`/`.css`, typisierte
  In-/Outputs) erzeugt vorhersehbaren, gleichförmig lesbaren Output. Reacts Flexibilität
  führt zu wechselnden Mustern → für reine Leser schwerer konsistent zu halten.
- **Lesbarkeit für Java-Leser**: explizite OO-Struktur (Klassen, DI, Typisierung, klare
  Dateistruktur) entspricht dem mentalen Modell des Teams; man findet sich zurecht, ohne
  den Code selbst geschrieben zu haben. **Leitlinie: Signals-first** (Angular Signals,
  stabil ab v17) statt RxJS-Pipelines, wo möglich — imperativ anmutende, gut lesbare
  Reaktivität; RxJS nur wo nötig (HTTP/Streams).
- **„Aus einer Hand"**: Routing, typisierte Reactive Forms, HTTP, DI, CLI ab Werk +
  offizielles **Angular Material/CDK**; **PrimeNG / AG Grid Community** fügen sich nahtlos
  ein.

**Weitere Kriterien:**
- **Keine laufenden Lizenzkosten** — auf Framework-Ebene neutral (alle MIT, keine
  Laufzeitkosten). Entscheidend ist die **Komponentenbibliothek**: eine vollständig
  kostenfreie Angular-Basis besteht aus **Angular Material + CDK (MIT)** und
  **PrimeNG (MIT)**. **Leitplanke**: **AG Grid nur Community (MIT)**, nicht Enterprise
  (kostenpflichtig); ebenso Kendo UI / Syncfusion / DevExtreme / Vaadin Pro meiden.
- **API-Stabilität** — hier ist **React am stärksten** (konservative Kern-API). Angular
  hat zweimal jährlich Major-Releases, aber mit **formaler Deprecation-Policy und
  automatisierten `ng update`-Migrationen** → verwalteter, angekündigter Wandel statt
  Überraschungs-Brüchen. Vue (2→3 harter Bruch) und Svelte (5 „runes") liegen zurück.
  Dies ist der einzige Punkt, an dem React klar besser abschneidet — ehrlich benannt.
- **Customizing** — Angular und React je 5. Für Angular ist das **CDK** ein echtes Pfund:
  headless Primitive (Overlay, Drag&Drop, A11y, Virtual Scroll), um eigene Komponenten
  zu bauen, wenn die Bibliothek nichts Passendes bietet — deckt das Kriterium direkt ab.

**Zweitwahl React** — unter der AI-Autorenschaft der stärkste Herausforderer: größter
AI-Trainingskorpus, sehr idiomatischer Claude-Output und beste API-Stabilität. Der
Ausschlag für Angular kommt aus **Lesbarkeit für Java-Leser + Konsistenz fürs Reviewen +
Enterprise-Ökosystem**. React würde vorn liegen, wenn **API-Stabilität** oder **maximale
AI-Korpusgröße** höher gewichtet würde als „aus einer Hand" und Java-Lesbarkeit.
**Svelte** gewinnt die reine Lesbarkeit, fällt aber wegen kleinem AI-Korpus, dünnem
Enterprise-Ökosystem und jungen APIs (v5 „runes") zurück; Vue liegt dazwischen.

**Konsistenz-Leitlinien für AI-generierten Code** (unabhängig vom Framework wichtig, mit
Angular gut durchsetzbar):
- Verbindliche Lint-/Format-Regeln (ESLint + Prettier) und CLI-Schematics, damit
  generierter Code gleichförmig und diff-arm bleibt.
- **Signals-first** statt RxJS, wo möglich, für imperativ lesbare Reaktivität.
- Ein dokumentierter Projekt-Styleguide/„AGENTS.md" fürs Frontend, den Claude Code
  befolgt, damit Struktur und Muster über Module hinweg identisch bleiben.

**Restrisiko/Mitigation**: RxJS-Lesbarkeit für Java-Leute; halbjährliche Major-Releases
erfordern Migrationsdisziplin (`ng update`). → kleiner **Bestätigungs-Spike** (ein Modul:
Fallliste + Falldetail + Datentabelle mit Logo-Tokens) mit kostenfreier Komponentenbasis,
bei dem **die Entwickler den von Claude generierten Angular-Code auf Lesbarkeit/
Verständlichkeit bewerten**, vor der finalen Festschreibung.

**Build-Integration**: `frontend-maven-plugin` baut das Bundle in der `generate-resources`-
Phase; `maven-war-plugin` verpackt die statischen Assets. Damit bleibt „ein
`mvn install`, ein EAR/WAR-Set" erhalten und der Node-Toolchain-Bedarf ist auf die
Buildmaschine beschränkt.

### Decision 2c — Supply-Chain-Härtung (keine Remote-Ressourcen zur Laufzeit)

Ziel (Auftraggeber): Supply-Chain-Angriffe minimieren; die deployte Anwendung soll
**keinerlei Code von remote laden**. Zwei getrennte Vektoren:

**A) Laufzeit (Browser) — garantierbar auf Null.** Die App wird als statisches Bundle
ausschließlich aus dem eigenen WAR (same-origin) geliefert:
- **Alles self-hosten**: JS/CSS (vom CLI gebündelt), Schriften (kein Font-CDN, siehe
  Decision 1), Icons als Inline-SVG/Font. Kein externer Aufruf außer der eigenen REST-API.
- **Strikte Content Security Policy** (per WildFly-/`web.xml`-Header) erzwingt das
  browserseitig, z. B. `default-src 'self'; script-src 'self'; connect-src 'self';
  font-src 'self'; img-src 'self' data:; object-src 'none'; base-uri 'self'`. Component-
  Styles über Nonce (`ngCspNonce`) statt `unsafe-inline`; zusätzlich Angulars eingebaute
  **Trusted-Types**-Unterstützung gegen DOM-XSS.
- **Kein Node in Produktion** (Decision 2a) → keine serverseitige npm-Laufzeit-Angriffsfläche.

**B) Build-Zeit (npm) — der eigentliche Supply-Chain-Vektor.** „Kein Remote zur Laufzeit"
verhindert nicht, dass Schadcode beim Bauen ins Bundle gelangt. Kontrollen:
- **Lockfile + `npm ci`** mit gepinnten, integritätsgehashten Versionen.
- **Install-Skripte deaktivieren** (`npm ci --ignore-scripts`) gegen `postinstall`-Angriffe.
- **Gespiegelte/vendorte Registry** analog zum bestehenden `maven-repo/`-Muster des
  Projekts (private Registry wie Verdaccio/Nexus oder committeter Offline-Cache) → Builds
  ziehen nichts Unkontrolliertes aus dem Netz.
- **SCA/Scanning**: `npm audit` + Dependabot (bereits aktiv) + reproduzierbare Builds in CI
  statt auf Dev-Maschinen.

Dieses Ziel begünstigt **Angular** zusätzlich: der kuratierte First-Party-Monorepo
(Router, Forms, HTTP, CLI, Material aus einer Hand) hat einen **deutlich kleineren
Dritt-Abhängigkeitsbaum** als ein handzusammengestellter React-Stack → kleinere
Build-Zeit-Angriffsfläche (siehe zusätzliches Kriterium in Decision 2b).

**Authentifizierung**: Die REST-API nutzt heute HTTP Basic Auth — für eine Browser-SPA
ungeeignet (kein Logout, kein 2FA, Credential-Handling). Es wird ein
session-/tokenbasierter Login-Flow benötigt (z. B. serverseitige Session-Cookie- oder
Bearer-Token-Ausgabe nach Login), abgestimmt mit `add-two-factor-auth`. Same-Origin-
Deployment (WAR auf demselben WildFly) vermeidet CORS und erleichtert sichere Cookies.

## Decision 3 — Besonders herausfordernde Funktionen

Diese Funktionen sind im Client tief an das lokale Betriebssystem/Desktop gekoppelt und
lassen sich nicht 1:1 in den Browser übertragen. Für jede wird eine Web-Strategie
festgelegt:

1. **Native Office-Bearbeitung (größte Herausforderung).**
   Der Client (Paket `launcher/`) lädt ein Dokument in eine lokale Temp-Datei, startet
   die **plattform-spezifische Office-Suite** (`LinuxOfficeLauncher`,
   `MacOfficeLauncher`, `MacMicrosoftOfficeLauncher`, `WindowsOfficeLauncher`,
   `WindowsMicrosoftOfficeLauncher`), überwacht die Datei mit einem
   `DocumentObserver`/WatchService und lädt sie nach dem Speichern automatisch zurück.
   Ein Browser kann Prozesse **nicht** starten und Dateisysteme nicht überwachen.
   Optionen: (a) **Browserbasiertes Editieren** via Collabora Online / OnlyOffice
   (WOPI-Integration) — echte In-Browser-Bearbeitung, erfordert zusätzlichen
   Dienst/Server; (b) **Download → lokal bearbeiten → manueller Re-Upload** (einfach,
   aber UX-Rückschritt); (c) optionaler **Browser-Helper/Companion** für den
   „edit-in-place"-Komfort. **Empfehlung**: WOPI-basiertes In-Browser-Editieren als
   Zielbild, Download/Upload als Fallback im MVP. Explizit außerhalb dieses Changes zu
   entscheiden, aber als Risiko benannt.

2. **Scannen (TWAIN/Scanner).** `editors/documents/ScannerPanel`, `ScannerUtils`
   greifen auf lokale Scannerhardware zu. Im Browser nicht möglich.
   Strategie: Upload von Dateien (Drag&Drop, Datei-Dialog, Mobile-Kamera-Upload);
   Scanner-Workflow bleibt Desktop-Client oder wird über einen separaten Upload-Ordner/
   Companion abgebildet.

3. **Lokale Dateisystem-Interaktion.** Drag&Drop aus dem Dateisystem, Temp-Dateien,
   automatischer Upload überwachter Ordner (`ScannerLocalDocumentsUploadTimerTask`).
   Ersatz: Browser-Datei-Uploads und -Downloads; „Ordner überwachen" entfällt oder
   Companion.

4. **Dokumentenanzeige.** Client nutzt eigene Viewer (`editors/documents/viewer`,
   PDF-Rendering). Im Web: PDF nativ im Browser; für Office-Formate Vorschau via
   serverseitiger Konvertierung oder WOPI-Viewer.

5. **Echtzeit-/Push-Funktionen.** Messenger (`messenger/`), Kalender-Erinnerungen,
   Wiedervorlage-Benachrichtigungen und Monitoring benötigen serverseitiges Push.
   EJB-Remoting entfällt; im Web via **WebSocket/SSE** oder Polling. Server-Push muss
   ergänzt werden (heute clientseitige Timer/Observer).

6. **VoIP/Fax & CTI.** `voip/` integriert Telefonie. Im Web über die
   `VoipServiceRemote`-Funktionalität als REST + ggf. WebRTC/Click-to-Dial.

7. **Diktat/Audio.** Aufnahme im Browser via MediaRecorder-API möglich, aber
   Diktiergeräte-Hardware (Fußschalter etc.) nicht — reiner Web-Workaround.

## Risks / Trade-offs

- **Scope-Risiko**: Vollparität ist sehr groß (16+ Module). → Phasenplan mit MVP und
  klar priorisierten Modulen; Koexistenz mit Swing-Client, kein Big-Bang.
- **REST-Lücken**: Nicht alle 24 EJB-Funktionen sind über REST verfügbar. → Lücken pro
  Modul additiv in neuer API-Version ergänzen; niemals bestehende Versionen brechen.
- **Office-Editieren**: Kernkomfort des Desktop-Clients schwer ersetzbar. → früher
  Spike zu WOPI (Collabora/OnlyOffice); Fallback Download/Upload akzeptiert.
- **Auth-Umstellung**: Basic Auth ungeeignet. → tokengestützter Login, mit
  `add-two-factor-auth` abstimmen.
- **Design-Drift**: Ohne Governance divergieren Module. → Design-System + Katalog +
  PR-Checkliste ab Tag 1.
- **Responsive-Aufwand**: Der Client ist stark tabellen-/dichtelastig; das auf das
  Smartphone zu bringen, ist nicht trivial. → mobile-first ab Tag 1, adaptive
  Tabellen→Karten-Muster im Design-System, Visual-Regression über alle Breakpoints;
  auf dem Telefon bewusst aufgabenfokussiert statt 1:1-Parität.
- **Node-Toolchain im Java-Build**: zusätzliche Build-Komplexität. → auf Buildzeit
  beschränkt, in Prod nur statische Assets im WAR.

## Migration Plan

Inkrementell, ohne den Swing-Client abzuschalten:
1. **Phase 0 — Fundament**: Design-System, App-Shell, Auth-Flow, Build-/WAR-Deployment,
   REST-Gap-Analyse.
2. **Phase 1 — MVP (Read-first)**: Login, Fallliste/-suche, Falldetail, Dokumentenliste
   + Download/Vorschau, Kontakte, Kalenderansicht. Office-Bearbeitung via
   Download/Upload-Fallback.
3. **Phase 2 — Kernmodule (Write)**: Fälle/Dokumente/Kontakte/Kalender voll editierbar,
   Zeiterfassung, Volltextsuche, E-Mail.
4. **Phase 3 — Finanzen & Kommunikation**: Rechnungen/Zahlungen (ZUGFeRD), Messenger
   (Push), beA, Reporting.
5. **Phase 4 — Parität & Komfort**: WOPI-Office-Editieren, Serienmails, Assistent,
   Administration/Settings, VoIP.

Rollback pro Phase: Web-UI ist additiv; bei Problemen weiter Swing-Client nutzen.

## Open Questions

- ~~Konkrete SPA-Framework-Wahl~~ — **entschieden: Angular** (siehe Decision 2b);
  offen nur noch der Bestätigungs-Spike vor finaler Festschreibung.
- WOPI-Editing: Collabora Online vs. OnlyOffice; als optionaler Dienst neben WildFly
  akzeptabel? Lizenz-/Betriebskosten?
- Auth-Modell: serverseitige Session-Cookies vs. Bearer-Token; Zusammenspiel mit 2FA.
- Push-Mechanismus: WebSocket vs. SSE vs. Polling für Messenger/Erinnerungen.
- Companion-App für Scanner/Edit-in-place: gewünscht oder bewusst verzichten?
- ~~Wird die Web-UI als eigenes WAR oder als Overlay ausgeliefert?~~ — **entschieden:
  eigenständiger WAR `j-lawyer-web`, separat auf derselben WildFly** (siehe Decision 2a).
