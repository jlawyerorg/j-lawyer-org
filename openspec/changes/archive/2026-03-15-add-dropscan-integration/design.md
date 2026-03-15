## Context
Dropscan (https://api.dropscan.de/v1) ist ein Cloud-basierter Scan-Service aus Berlin. Kanzleien können dort virtuelle Postfächer (Scanboxen) einrichten, an die physische Post gesendet wird. Dropscan digitalisiert die Post und stellt sie über eine REST API bereit. Die Integration soll den Workflow von der Postannahme bis zur Aktenzuordnung in j-lawyer abbilden.

### Dropscan API Eckdaten
- **Base URL**: `https://api.dropscan.de/v1`
- **Auth**: Bearer Token oder OAuth2 (Scopes: `read`, `scan`, `destroy`)
- **Kernressourcen**: Scanboxes, Recipients, Mailings, ActionRequests, ForwardingAddresses, Shipments
- **Mailing-Status**: received → scan_requested → scanned → destroy_requested/destroyed bzw. forward_requested/forwarded
- **Dateiformate**: PDF (einzeln oder als ZIP), JPEG (Umschlag), Plaintext (OCR)
- **Pagination**: Max 100 Mailings pro Request, `older_than` UUID-basiert
- **Webhooks**: 9 Event-Typen (mailing_received, mailing_scanned, etc.)

## Goals / Non-Goals
- **Goals**:
  - Dropscan API-Token pro Benutzer am User-Objekt verwalten
  - Alle Scanboxen eines Dropscan-Accounts automatisch nutzen (keine manuelle Scanbox-Auswahl)
  - Posteingang zeigt Mailings aller Scanboxen, filterbar nach einzelner Scanbox
  - Globales Polling-Intervall mit Default in ServerSettings (ohne UI-Konfiguration)
  - Posteingang (Mailings) im Client anzeigen mit Umschlag-Vorschau und Status
  - Gescannte PDFs als Dokumente in j-lawyer-Akten importieren (manuelle Zuordnung mit Vorschlagsliste)
  - Aktenvorschläge basierend auf OCR-Daten/Plaintext des Mailings (Aktenzeichen-Matching analog ScannerPanel)
  - Aktionen (Scan anfordern, Weiterleiten, Vernichten) aus j-lawyer heraus auslösen
  - Periodisches Polling neuer Mailings über ScheduledTasksService für alle User mit konfiguriertem Account
- **Non-Goals**:
  - Webhook-Empfang (erfordert öffentlich erreichbaren Endpunkt, den viele Kanzlei-Server nicht haben)
  - Scanbox-Verwaltung (Anlegen/Löschen) – dies erfolgt über das Dropscan-Portal
  - Empfänger-Verwaltung über j-lawyer – dies erfolgt über das Dropscan-Portal
  - Forwarding-Adressen-Verwaltung über j-lawyer

## Decisions

### 1. Polling statt Webhooks
- **Decision**: Periodisches Polling über `ScheduledTasksService` statt Webhook-Empfang
- **Why**: Kanzlei-Server sind typischerweise hinter NAT/Firewall und nicht öffentlich erreichbar. Polling ist zuverlässiger und erfordert keine Netzwerkkonfiguration.
- **Alternatives**: Webhooks wären effizienter, aber die meisten j-lawyer-Installationen sind nicht von außen erreichbar.

### 2. Bearer Token Authentifizierung
- **Decision**: Bearer Token statt OAuth2-Flow
- **Why**: Einfachere Konfiguration für Kanzleien. Ein Token wird einmalig im Dropscan-Portal generiert und in j-lawyer hinterlegt.
- **Alternatives**: OAuth2 wäre flexibler, erfordert aber einen Redirect-Endpunkt und ist für Server-zu-Server-Kommunikation unnötig komplex.

### 3. API-Token pro Benutzer, alle Scanboxen automatisch, Polling-Intervall global
- **Decision**: Nur das Dropscan API-Token wird pro Benutzer in `AppUserBean` gespeichert. Alle Scanboxen des Accounts werden automatisch via `GET /scanboxes` ermittelt und genutzt — keine Scanbox-ID-Konfiguration nötig. Das Polling-Intervall wird global in ServerSettings mit einem Default (15 Minuten) hinterlegt und ist nicht über die UI konfigurierbar.
- **Why**: Ein Dropscan-Account kann mehrere Scanboxen haben. Statt den User eine einzelne auswählen zu lassen, werden alle Scanboxen im Posteingang zusammengeführt und sind per Filter eingrenzbar. Das ist flexibler und erfordert weniger Konfiguration.
- **Alternatives**: Scanbox-ID manuell konfigurieren – unnötige Einschränkung und zusätzlicher Konfigurationsaufwand.

### 4. Dropscan-Service als eigener EJB
- **Decision**: Neuer `DropscanService` EJB mit Remote- und Local-Interface
- **Why**: Saubere Trennung der Dropscan-Logik. Konsistent mit dem bestehenden Service-Pattern (vgl. SipgateService-Muster über Fax-Integration).
- **Alternatives**: Erweiterung eines bestehenden Services – widerspricht Single-Responsibility.

### 5. Dropscan-UI in ScannerPanel integriert
- **Decision**: Die Dropscan-Posteingangsoberfläche wird als `pnlDropscan`-Panel innerhalb des bestehenden `ScannerPanel.java` implementiert — kein separates `DropscanInboxPanel`. Bei Auswahl eines Mailings wird eine Vorschlagsliste passender Akten angezeigt, analog zum bestehenden Pattern in `pnlActions`. Die Dropscan API liefert OCR-Plaintext via `GET .../plaintext`; daraus werden Aktenzeichen extrahiert und gegen eigene sowie Fremdaktenzeichen gematcht. Fallback: zuletzt bearbeitete Akten. Anzeige über `SaveScanToCasePanel`-Komponenten.
- **Why**: Dropscan ist konzeptionell ein weiterer Posteingangs-Kanal neben dem lokalen Scanner. Integration in ScannerPanel bündelt alle Posteingangs-Workflows an einem Ort. Bewährtes Pattern und direkte Wiederverwendung der bestehenden Matching-Logik und UI-Komponenten.
- **Alternatives**: Eigenes Panel — fragmentiert die Posteingangs-Workflows unnötig.

### 6. Import über BulkSaveDialog mit Einzeldokumenten
- **Decision**: Beim Import wird das ZIP (`GET .../zip`) heruntergeladen und entpackt. Die Einzeldokumente (mit Original-Dateinamen aus dem ZIP) werden an den bestehenden `BulkSaveDialog` übergeben, der Duplikaterkennung, Benennung und Aktenzuordnung übernimmt. Kein ZIP in der Akte.
- **Why**: Wiederverwendung des bewährten BulkSaveDialog mit vorhandener Duplikaterkennung. Direkter Zugriff auf jedes Einzeldokument. Konsistent mit dem Scanner-Import-Workflow.
- **Alternatives**: Eigener Import-Dialog — dupliziert vorhandene Logik unnötig.

### 7. Automatische Vernichtung nach Zuordnung
- **Decision**: Eine Checkbox "nach Zuordnung vernichten" im `pnlDropscan`-Header ermöglicht die automatische Vernichtung des physischen Mailings bei Dropscan nach erfolgreichem Import (analog "nach Zuordnung löschen" für lokale Scans).
- **Why**: Konsistentes UX-Pattern mit dem bestehenden Scanner-Workflow. Vermeidet manuellen Zusatzschritt.
- **Alternatives**: Vernichtung immer manuell — funktioniert, aber umständlich bei hohem Postaufkommen.

### 8. HTTP-Client
- **Decision**: `java.net.HttpURLConnection` für Dropscan API-Aufrufe
- **Why**: Bereits im Projekt verwendet, keine zusätzliche Abhängigkeit notwendig. Für die wenigen REST-Aufrufe ausreichend.
- **Alternatives**: Apache HttpClient oder JAX-RS Client – erfordert zusätzliche Dependencies.

## Risks / Trade-offs
- **API-Rate-Limits**: Dropscan API dokumentiert keine expliziten Rate-Limits, aber aggressives Polling sollte vermieden werden → Konfigurierbares Polling-Intervall (Standard: 15 Minuten)
- **Token-Sicherheit**: Bearer Token muss verschlüsselt in der Datenbank gespeichert werden (AppUserBean-Feld) → Nutzung der bestehenden `Crypto`-Klasse für Verschlüsselung
- **API-Änderungen**: Dropscan API v1 könnte sich ändern → Version im Code pinnen, Fehlerbehandlung für unerwartete Responses
- **Große PDFs**: Mailing-PDFs könnten groß sein → Download nur on-demand, nicht beim Polling

## Migration Plan
- Neue Spalte in `AppUserBean`/User-Tabelle: `dropscanApiToken` (varchar, nullable)
- Neuer ServerSettings-Eintrag `dropscan.pollinginterval` mit Default 15 (Minuten)
- SQL-Migrationsskript für die neuen Spalten
- Feature ist standardmäßig deaktiviert (Felder leer) bis ein User seine Credentials konfiguriert
- Kein Breaking Change für bestehende Installationen

## Resolved Questions
- **Mailing-Metadaten**: `received_at` wird beim Import als Dokumentdatum gesetzt (`ArchiveFileService.setDocumentDate`). Weitere Attribute (Status, Timestamps, UUID etc.) werden in der Dropscan-UI angezeigt, aber nicht in die Akte übernommen.
- **Duplikaterkennung**: Wird durch den bestehenden BulkSaveDialog abgedeckt.
- **Dokumentbenennung**: Original-Dateinamen aus dem Dropscan-ZIP werden übernommen.
- **Vernichtung nach Import**: Optionale Checkbox "nach Zuordnung vernichten" analog "nach Zuordnung löschen".
- **Refresh**: Manueller Refresh-Button + automatischer Refresh nach Zuordnung/Vernichtung.
- **Berechtigungen**: Kein separates Rollenkonzept — jeder User mit konfiguriertem Token hat Zugriff.
