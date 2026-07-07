# Change: Web UI mit Funktionsumfang des j-lawyer-client

## Status

**Freigegeben am 2026-07-07** (jens@office-42.de) — Umsetzung freigegeben. Fixierte
Entscheidungen: Angular · statisches Bundle als eigenständiger WAR (`j-lawyer-web`) auf
WildFly · kostenfreie Komponentenbasis · Logo-Farben führend + responsives Master-Detail.
Die Detail-/Folge-Entscheidungen (Bestätigungs-Spike, REST-Gap-Analyse, Auth-/Push-Modell,
WOPI-Office-Strategie, exakte Produktionsschrift) bleiben wie in `tasks.md`/`design.md`
markiert offen und werden im Zuge der Umsetzung getroffen.

## Why

Der bestehende `j-lawyer-client` ist eine reichhaltige Swing-Desktop-Anwendung, die
pro Arbeitsplatz installiert, aktualisiert und mit einer lokalen Java-/JavaFX-Laufzeit
sowie einer lokal installierten Office-Suite betrieben werden muss. Das erschwert
Onboarding, Betrieb auf Nicht-Windows-Geräten, mobiles/Tablet-Arbeiten und
Zero-Install-Szenarien (Terminalserver, Homeoffice, BYOD). Eine Web-UI, die den
Funktionsumfang des Desktop-Clients erreicht, macht die Kanzleisoftware ohne lokale
Installation über den Browser nutzbar und senkt die Betriebskosten.

Dieses Proposal ist bewusst **entscheidungsvorbereitend**: Bevor Implementierung
beginnt, müssen drei strategische Fragen geklärt und dokumentiert werden — (1) wie ein
einheitliches Design sichergestellt wird, (2) welche Technologie inkl.
Deployment-Modell gewählt wird, und (3) welche Client-Funktionen im Web besonders
herausfordernd sind (allen voran die Office-Suite-Integration). Die technischen
Entscheidungen und ihre Begründung stehen in `design.md`.

## What Changes

- **Neue Fähigkeit `web-client`**: browserbasierte, **responsive** UI (Desktop, Tablet,
  Smartphone — eine einzige Codebasis, mobile-first), die schrittweise Funktionsparität
  mit dem Swing-Client erreicht (Fälle, Dokumente, Kontakte, Kalender,
  Finanzen/Rechnungen, Zeiterfassung, Suche, E-Mail, beA, Messenger, Reporting,
  Administration/Einstellungen).
- **Architekturentscheidung**: Web-UI kommuniziert **ausschließlich über die REST-API**
  (`/j-lawyer-io/rest/v{n}`) — nicht über EJB-Remoting. Fehlende Endpunkte werden in
  einer neuen API-Version additiv ergänzt, ohne bestehende Versionen zu brechen.
- **Design-System**: verbindliches, dokumentiertes UI-Design-System (Tokens,
  Komponentenbibliothek, Interaktionsmuster) als Voraussetzung für einheitliches Design.
- **Technologie- und Deployment-Auswahl**: dokumentierte Auswahlkriterien und
  Entscheidung. Bevorzugtes Deployment als WAR auf demselben WildFly (charmant, nicht
  zwingend); Alternativen werden mit Begründung bewertet.
- **Behandlung der Desktop-only-Funktionen**: explizite Strategie für die im Web nicht
  1:1 abbildbaren Funktionen (natives Office-Bearbeiten via `launcher/`-Paket,
  Scanner/TWAIN, lokale Dateisystem-Watches, Diktat/Hardware).
- **Phasenplan**: MVP → Kernmodule → Vollparität, mit klaren Meilensteinen.

Dieses Proposal liefert **keine** Implementierung. Es definiert Anforderungen und die
Architektur-/Technologieentscheidung. Implementierung erfolgt nach Freigabe in
Folge-Changes pro Modul.

## Impact

- **Affected specs**: neue Capability `web-client` (dieses Change). Nachgelagert
  betroffen (additive Erweiterungen in Folge-Changes): `email-rest-api`,
  `calendar-api`, `rest-api-documentation`, `full-text-search`.
- **Affected code**:
  - Neues Frontend-Modul/Repo-Bereich (SPA-Assets).
  - Neues Maven-Modul `j-lawyer-web` als **eigenständiger WAR** (statisches
    Angular-Bundle), separat auf derselben WildFly deployt; `deploy.sh` kopiert künftig
    EAR + WAR (siehe `design.md` Decision 2a).
  - Additive REST-Endpunkte in `j-lawyer-server/j-lawyer-io/src/main/java/org/jlawyer/io/rest/v{n}`
    für Lücken gegenüber den 24 EJB-Remote-Interfaces in `j-lawyer-server-api`.
  - Authentifizierung: heute HTTP Basic Auth; Web-UI erfordert eine session-/
    tokenbasierte Variante (siehe Zusammenhang mit `add-two-factor-auth`).
- **Nicht betroffen**: bestehende EJB-Remote-Interfaces und der Swing-Client bleiben
  unverändert und lauffähig (Koexistenz während der Migration).
