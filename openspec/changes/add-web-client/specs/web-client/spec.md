## ADDED Requirements

### Requirement: Browserbasierter Zugriff ohne lokale Installation
Das System SHALL eine browserbasierte Benutzeroberfläche bereitstellen, die ohne
lokale Installation einer Java-Laufzeit oder des Desktop-Clients auf einem aktuellen
Desktop-, Tablet- oder Smartphone-Browser nutzbar ist.

#### Scenario: Zugriff über den Browser
- **WHEN** eine authentifizierte Anwenderin die Web-UI in einem unterstützten Browser öffnet
- **THEN** wird die Anwendung ohne lokale Installation geladen und die Hauptnavigation angezeigt

#### Scenario: Koexistenz mit dem Desktop-Client
- **WHEN** die Web-UI in Betrieb ist
- **THEN** bleibt der bestehende Swing-Desktop-Client gegen denselben Server unverändert lauffähig

### Requirement: Responsives UI für Desktop, Tablet und Smartphone
Das System SHALL als **eine einzige responsive** UI-Codebasis (mobile-first)
Desktop-, Tablet- und Smartphone-Formfaktoren bedienen und MUST NOT getrennte
Codebasen pro Gerätegröße erfordern. Das Layout SHALL sich an definierten Breakpoints
adaptieren, ohne horizontales Scrollen der Seite, und SHALL sowohl Zeiger- als auch
Touch-Bedienung gleichwertig unterstützen. Auf kleinen Formfaktoren SHALL das
mehrspaltige Master-Detail-Layout zu einem navigationsgetriebenen Single-Pane-Flow und
dichte Datentabellen zu Karten-/Listenansichten adaptieren.

#### Scenario: Desktop-Formfaktor
- **WHEN** die Web-UI auf einem breiten Bildschirm (≥ ~1024px) geöffnet wird
- **THEN** werden die persistente Modulleiste und ein mehrspaltiges Master-Detail-Layout mit vollen Datentabellen angezeigt

#### Scenario: Smartphone-Formfaktor
- **WHEN** die Web-UI auf einem schmalen Bildschirm (< ~600px) geöffnet wird
- **THEN** wird ein navigationsgetriebener Single-Pane-Flow angezeigt, Datentabellen erscheinen als Karten-/Listenansichten, und die Seite scrollt nicht horizontal

#### Scenario: Touch-Bedienung
- **WHEN** die Web-UI auf einem Touch-Gerät bedient wird
- **THEN** sind interaktive Elemente mit ausreichend großen Touch-Zielen und touch-tauglichen Interaktionsmustern nutzbar

### Requirement: Kommunikation ausschließlich über die REST-API
Die Web-UI SHALL ausschließlich über die versionierte REST-API (`/j-lawyer-io/rest/v{n}`)
mit dem Server kommunizieren und MUST NOT EJB-Remoting verwenden. Fehlende Funktionen
SHALL additiv in einer neuen API-Version ergänzt werden, ohne bestehende Versionen zu
brechen.

#### Scenario: Datenzugriff über REST
- **WHEN** die Web-UI Daten liest oder schreibt
- **THEN** erfolgt dies über REST-Endpunkte und nicht über JBoss-EJB-Remoting

#### Scenario: Fehlende Endpunkte werden additiv ergänzt
- **WHEN** eine für die Web-UI benötigte Serverfunktion noch keinen REST-Endpunkt hat
- **THEN** wird ein neuer Endpunkt in der aktuellen API-Version hinzugefügt, ohne bestehende Versionen zu verändern

### Requirement: Einheitliches Design-System
Das System SHALL ein verbindliches Design-System bereitstellen (Design-Tokens, eine
einzige Komponentenbibliothek, definierte responsive Breakpoints und dokumentierte
Interaktionsmuster für Zeiger- und Touch-Bedienung), das für alle Module gilt und
dessen Einhaltung durch Governance (Komponentenkatalog, PR-Prüfung, automatisierte
Gates über alle Breakpoints) sichergestellt wird. Die Marken-Tokens SHALL die
Logo-Farben aus `ServerColorTheme` nachnutzen; das Design SHALL sich nicht am
FlatLaf-Look des Desktop-Clients orientieren müssen.

#### Scenario: Konsistente Komponenten über Module hinweg
- **WHEN** ein neues Modul der Web-UI entwickelt wird
- **THEN** verwendet es ausschließlich Komponenten und Tokens des Design-Systems statt Ad-hoc-Styles

#### Scenario: Wiedererkennungswert über Logo-Farben
- **WHEN** ein Anwender die Web-UI nutzt
- **THEN** basieren die Marken-/Akzentfarben auf der Logo-Palette (`COLOR_LOGO_RED`/`GREEN`/`BLUE` aus `ServerColorTheme`), sodass ein markenkonformer Wiedererkennungswert entsteht

### Requirement: Vorab abgenommene Design-Ausarbeitung
Das System SHALL vor Implementierungsbeginn eine exakt ausgearbeitete, abgenommene
Zielgestaltung enthalten (interaktiver Prototyp/Mockup, z. B. via Claude-Artifacts),
die die Logo-Farbpalette sowie den bereitgestellten Referenz-Screenshot
(`20260707_website-mockup.png`) — Letzteren **nur für Farbgebung und Typografie, nicht
als Layout-Vorlage** — berücksichtigt. Design-Tokens und Komponentenkatalog SHALL aus
dieser Ausarbeitung abgeleitet werden.

#### Scenario: Design vor Implementierung abgenommen
- **WHEN** die Implementierung eines Web-UI-Moduls beginnt
- **THEN** liegt eine abgenommene Design-Ausarbeitung vor, aus der Tokens und Komponenten abgeleitet sind

#### Scenario: Referenz-Screenshot nur für Farbe und Typografie
- **WHEN** die Design-Ausarbeitung erstellt wird
- **THEN** übernimmt sie Farbgebung und Typografie in Anlehnung an den Referenz-Screenshot und die Logo-Farbpalette, während das Layout dem responsiven Master-Detail-Muster (nicht dem Marketing-Seitenaufbau) folgt

### Requirement: Dokumentierte Technologie- und Deployment-Entscheidung
Das System SHALL vor Implementierungsbeginn eine dokumentierte Technologie- und
Deployment-Entscheidung anhand gewichteter Auswahlkriterien (Deployment-Fit,
Teamkompetenz/Wartbarkeit, Reife/Ökosystem, Barrierefreiheit/i18n, Lizenz,
Build-Integration) enthalten. Das Deployment SHALL ein statisch gebautes
Frontend-Bundle sein, das als **eigenständiger WAR** (`j-lawyer-web`) separat auf
demselben WildFly betrieben wird und keinen zusätzlichen Laufzeitprozess in Produktion
erfordert.

#### Scenario: Deployment als eigenständiger WAR auf WildFly
- **WHEN** die Web-UI ausgeliefert wird
- **THEN** wird sie als eigenständiger WAR separat auf dem bestehenden WildFly deployt (nicht in das EAR eingebettet), ohne einen zusätzlichen Produktions-Laufzeitprozess zu erfordern

#### Scenario: Build-Integration in den Maven-Reactor
- **WHEN** das Projekt gebaut wird
- **THEN** wird das Frontend-Bundle innerhalb des bestehenden Maven-Builds erzeugt und verpackt, und der Node-Toolchain-Bedarf ist auf die Buildmaschine beschränkt

### Requirement: Kostenfreie und anpassbare Komponentenbasis
Das gewählte Framework und die genutzten UI-Komponenten MUST NOT wiederkehrende
Lizenzkosten verursachen (permissive Lizenzen wie MIT/Apache). Das Framework SHALL das
Entwickeln eigener Komponenten ermöglichen, falls die Bibliothek nichts Passendes
bietet, und SHALL keine regelmäßigen, nicht automatisiert migrierbaren Brüche großer
Teile seiner APIs erfordern.

#### Scenario: Keine laufenden Lizenzkosten
- **WHEN** eine UI-Komponentenbibliothek oder ein Grid ausgewählt wird
- **THEN** wird eine kostenfrei lizenzierte Variante verwendet (z. B. Community/MIT), und kostenpflichtige Enterprise-Editionen werden vermieden

#### Scenario: Eigene Komponenten möglich
- **WHEN** für einen Anwendungsfall keine passende fertige Komponente existiert
- **THEN** kann eine eigene Komponente auf Basis der Framework-Primitive (z. B. CDK) entwickelt werden

#### Scenario: Beherrschbare API-Änderungen
- **WHEN** eine neue Major-Version des Frameworks erscheint
- **THEN** sind Breaking Changes angekündigt und über automatisierte Migrationen beherrschbar, ohne große Teile der Anwendung neu schreiben zu müssen

### Requirement: Lesbarer und konsistenter, AI-generierbarer Code
Die Codebasis SHALL explizit, strukturiert und über alle Module konsistent sein, da der
Großteil der Web-Anwendung von einem KI-Coding-Assistenten generiert und von den
Entwicklern primär gelesen, verstanden und reviewt wird. Das Projekt SHALL verbindliche
Coding-Standards (Lint-/Format-Regeln, dokumentierter Styleguide für den Assistenten)
durchsetzen, sodass generierter Code gleichförmig und ohne den Autor verständlich bleibt.

#### Scenario: Entwickler versteht generierten Code
- **WHEN** ein Entwickler ein von der KI generiertes Modul liest, das er nicht selbst geschrieben hat
- **THEN** kann er dessen Struktur und Ablauf nachvollziehen, weil Muster, Dateistruktur und Reaktivität einheitlich und explizit sind

#### Scenario: Konsistenz über Module erzwungen
- **WHEN** neuer Code generiert und eingecheckt wird
- **THEN** entsprechen Struktur, Formatierung und Muster den verbindlichen Coding-Standards (automatisiert geprüft)

### Requirement: Browsergeeignete Authentifizierung
Das System SHALL für die Web-UI einen session- oder tokenbasierten Anmelde-Flow
bereitstellen (inklusive Abmeldung) und MUST NOT sich auf HTTP-Basic-Auth als
alleinigen Mechanismus für die interaktive Web-Anmeldung stützen. Der Flow SHALL mit
der Zwei-Faktor-Authentifizierung kompatibel sein.

#### Scenario: Anmeldung und Abmeldung
- **WHEN** ein Anwender sich in der Web-UI anmeldet
- **THEN** erhält er eine session- oder tokenbasierte Sitzung, die er explizit beenden (abmelden) kann

#### Scenario: Kompatibilität mit Zwei-Faktor-Authentifizierung
- **WHEN** für ein Konto Zwei-Faktor-Authentifizierung aktiviert ist
- **THEN** verlangt der Web-Anmelde-Flow den zweiten Faktor

### Requirement: Funktionsparität mit dem Desktop-Client als Zielbild
Das System SHALL schrittweise Funktionsparität mit dem Swing-Client anstreben und die
Kernmodule abdecken: Fälle, Dokumente, Kontakte, Kalender/Fristen, Finanzen/Rechnungen,
Zeiterfassung, Volltextsuche, E-Mail, beA, Messenger, Reporting sowie Administration/
Einstellungen. Die Auslieferung SHALL phasenweise erfolgen (MVP → Kernmodule →
Vollparität).

#### Scenario: Kernmodule verfügbar
- **WHEN** die Web-UI die Kernmodul-Phasen erreicht hat
- **THEN** können Anwender Fälle, Dokumente, Kontakte, Kalender/Fristen und Kontaktdaten im Browser einsehen und bearbeiten

#### Scenario: Phasenweise Auslieferung
- **WHEN** eine Phase ausgeliefert wird
- **THEN** ist der jeweils enthaltene Funktionsumfang eigenständig nutzbar, ohne dass der Desktop-Client abgeschaltet werden muss

### Requirement: Strategie für Desktop-gebundene Funktionen
Das System SHALL für Funktionen ohne direktes Web-Äquivalent eine explizite Strategie
festlegen und dokumentieren, insbesondere für die native Office-Bearbeitung (heute im
`launcher/`-Paket über Temp-Datei + `DocumentObserver`-Watch), Scanner/TWAIN-Zugriff,
lokale Ordner-Überwachung und Diktier-/Aufnahmehardware.

#### Scenario: Office-Dokument bearbeiten
- **WHEN** ein Anwender in der Web-UI ein Office-Dokument bearbeiten möchte
- **THEN** wird entweder browserbasiertes Editieren (WOPI) oder ein Download-/Re-Upload-Fallback angeboten, statt eine lokale Office-Suite über die Web-UI zu starten

#### Scenario: Nicht abbildbare Hardware-Funktion
- **WHEN** eine Funktion lokale Hardware erfordert (z. B. TWAIN-Scanner, Diktier-Fußschalter)
- **THEN** wird ein web-taugliches Ersatzverfahren (z. B. Datei-/Kamera-Upload, MediaRecorder-Aufnahme) angeboten und die bewusste Abweichung dokumentiert

### Requirement: Serverseitige Echtzeit-Benachrichtigungen
Das System SHALL Echtzeit-/Benachrichtigungsfunktionen (Messenger, Kalender-/
Wiedervorlage-Erinnerungen, Status-Badges) über serverseitigen Push (WebSocket oder
SSE) oder Polling bereitstellen, da das clientseitige EJB-Remoting mit Timer-/
Observer-Mechanismen im Browser nicht verfügbar ist.

#### Scenario: Neue Sofortnachricht
- **WHEN** eine neue Messenger-Nachricht für einen angemeldeten Web-Anwender eintrifft
- **THEN** wird sie ohne manuelles Neuladen der Seite angezeigt

#### Scenario: Fällige Erinnerung
- **WHEN** eine Kalender- oder Wiedervorlage-Erinnerung fällig wird
- **THEN** erhält der angemeldete Web-Anwender eine Benachrichtigung in der Web-UI
