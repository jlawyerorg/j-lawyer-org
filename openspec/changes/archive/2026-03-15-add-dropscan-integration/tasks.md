## 1. Datenmodell & Konfiguration
- [x] 1.1 `AppUserBean` um Feld `dropscanApiToken` (varchar, nullable) erweitern
- [x] 1.2 SQL-Migrationsskript für neue Spalte in der User-Tabelle
- [x] 1.3 API-Token verschlüsselt speichern (Crypto-Klasse nutzen)
- [x] 1.4 ServerSettings-Schlüssel `dropscan.pollinginterval` mit Default 15 (Minuten) anlegen

## 2. Dropscan API Client (Server-Common)
- [x] 2.1 HTTP-Client-Klasse `DropscanApiClient` in `j-lawyer-server-common` erstellen
- [x] 2.2 Methode für Scanbox-Abfrage (`GET /scanboxes`) → Liste aller Scanboxen des Accounts
- [x] 2.3 Methoden für Mailing-Abfrage (`GET /scanboxes/{id}/mailings`) mit Pagination, pro Scanbox
- [x] 2.4 Methoden für Mailing-Details (`GET /scanboxes/{id}/mailings/{uuid}`)
- [x] 2.5 Methode für Umschlag-Bild (`GET .../envelope`) → byte[]
- [x] 2.6 Methode für PDF-Download (`GET .../pdf`) → byte[]
- [x] 2.7 Methode für Plaintext/OCR-Abruf (`GET .../plaintext`) → String
- [x] 2.8 Methode für ZIP-Download (`GET .../zip`) → byte[]
- [x] 2.9 Methode für Scan-Anforderung (`POST .../action_requests` mit action_type=scan)
- [x] 2.10 Methode für Vernichtungs-Anforderung (`POST .../action_requests` mit action_type=destroy)
- [x] 2.11 Methode für Weiterleitungs-Anforderung (`POST .../action_requests` mit action_type=forward)
- [x] 2.12 Methode zum Abbrechen einer Aktion (`DELETE .../action_requests/{id}`)
- [x] 2.13 DTO-Klassen: `DropscanMailing`, `DropscanScanbox`, `DropscanActionRequest`

## 3. EJB Service
- [x] 3.1 Remote-Interface `DropscanServiceRemote` in `j-lawyer-server-api` definieren
- [x] 3.2 Local-Interface `DropscanServiceLocal` in `j-lawyer-server-ejb` definieren
- [x] 3.3 Implementierung `DropscanService` in `j-lawyer-server-ejb`
- [x] 3.4 Methode: `getScanboxes(String principalId)` – Alle Scanboxen des User-Accounts abrufen
- [x] 3.5 Methode: `getMailings(String principalId, String scanboxId, String status)` – Mailings einer oder aller Scanboxen abfragen
- [x] 3.6 Methode: `getMailingDetails(String principalId, String scanboxId, String uuid)` – Einzelnes Mailing
- [x] 3.7 Methode: `getEnvelopeImage(String principalId, String scanboxId, String uuid)` – Umschlagbild
- [x] 3.8 Methode: `getMailingPlaintext(String principalId, String scanboxId, String uuid)` – OCR-Text abrufen
- [x] 3.9 Methode: `importMailingToCase(String principalId, String scanboxId, String uuid, String caseId)` – ZIP herunterladen, entpacken, jedes enthaltene PDF als einzelnes Dokument über BulkSaveDialog an Akte anhängen und `received_at` als Dokumentdatum setzen (`ArchiveFileService.setDocumentDate`)
- [x] 3.10 Methode: `requestScan(String principalId, String scanboxId, String uuid)` – Scan anfordern
- [x] 3.11 Methode: `requestDestroy(String principalId, String scanboxId, String uuid)` – Vernichtung anfordern
- [x] 3.12 Methode: `requestForward(String principalId, String scanboxId, String uuid, String addressId, String date)` – Weiterleitung anfordern

## 4. Scheduled Polling
- [~] 4.1 Timer-Task in `ScheduledTasksService` für periodischen Abruf neuer Mailings über alle User mit konfiguriertem Dropscan-Token, iteriert über alle Scanboxen pro User — Timer-Konfiguration vorhanden, aber `pollAllUsers()` ist auskommentiert
- [ ] 4.2 Benachrichtigung (Instant-Message oder ähnlich) bei neuen Mailings pro User

## 5. Client UI (in ScannerPanel.java / pnlDropscan)
- [x] 5.1 Dropscan-Posteingang in bestehendem `pnlDropscan`-Panel in `ScannerPanel.java` implementieren
- [x] 5.2 Tabelle mit Spalten: Scanbox, Datum, Status, Zustellweg
- [x] 5.3 Scanbox-Filter (Dropdown/Combobox zur Eingrenzung auf eine bestimmte Scanbox oder "Alle")
- [x] 5.4 Umschlag-Vorschau-Anzeige (JPEG, zoombar via GifJpegPngImagePanel)
- [~] 5.5 Kontextmenü/Buttons: Scannen, Weiterleiten, Vernichten, In Akte importieren — Scannen und Vernichten vorhanden, Weiterleiten fehlt als Button
- [x] 5.6 Checkbox "nach Zuordnung vernichten" im pnlDropscan-Header (analog "nach Zuordnung löschen")
- [x] 5.7 Akten-Vorschlagsliste bei Mailing-Auswahl (analog pnlActions-Pattern):
  - [x] 5.7.1 OCR-Plaintext vom Dropscan API abrufen (asynchron)
  - [x] 5.7.2 Aktenzeichen-Matching: eigene (`getAllArchiveFileNumbers`) und Fremdaktenzeichen (`getAllReferencedFileNumbers`) gegen OCR-Text matchen
  - [x] 5.7.3 Fallback auf zuletzt bearbeitete Akten (`getLastChanged`) wenn keine Treffer
  - [x] 5.7.4 Vorschläge als `SaveScanToCasePanel`-Komponenten anzeigen (Wiederverwendung bestehender Komponente)
  - [x] 5.7.5 `SaveToCaseExecutor`-Interface implementieren für Import-Callback
- [x] 5.8 Import über BulkSaveDialog: ZIP entpacken, Einzeldokumente mit Original-Dateinamen übergeben
- [x] 5.9 Nach Import: wenn "nach Zuordnung vernichten" aktiv, automatisch `requestDestroy` aufrufen
- [x] 5.10 Statusfilter (received, scanned, etc.)
- [x] 5.11 Refresh-Button für manuelles Aktualisieren der Mailing-Liste
- [x] 5.12 Automatischer Refresh nach Zuordnung oder Vernichtung

## 6. User-Einstellungen UI (manuell durch Entwickler, bereits umgesetzt)
- [x] 6.1 `pwdDropscanToken` (JPasswordField) im UserAdministrationDialog
- [x] 6.2 `cmdDropscanTest` (Button) zum Testen der Verbindung
- [x] 6.3 Event-Handler für `cmdDropscanTest`: ruft `DropscanServiceRemote.getScanboxes()` auf und zeigt gefundene Scanboxen an
- [x] 6.4 Speichern/Laden des Tokens über `AppUserBean.dropscanApiToken`

## 7. Tests
- [ ] 7.1 Unit-Tests für `DropscanApiClient` (mit Mock-Responses)
- [ ] 7.2 Unit-Tests für DTO-Serialisierung/Deserialisierung
- [ ] 7.3 Integrationstests für `DropscanService` (optional, erfordert API-Token)
