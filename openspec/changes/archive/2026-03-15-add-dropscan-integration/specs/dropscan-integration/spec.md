## ADDED Requirements

### Requirement: Dropscan-Konfiguration
The system SHALL allow each user to configure their Dropscan connection by storing an API token on the user profile. The API token MUST be stored encrypted. All scanboxes associated with the token's Dropscan account SHALL be used automatically without requiring manual scanbox selection. The polling interval SHALL be stored globally in server settings with a default of 15 minutes and does not require UI configuration.

#### Scenario: Benutzer konfiguriert Dropscan-Token
- **WHEN** an administrator opens the UserAdministrationDialog for a user
- **THEN** a `pwdDropscanToken` field (JPasswordField) is available for entering the API token

#### Scenario: Verbindung testen
- **WHEN** an administrator clicks `cmdDropscanTest` and a valid API token is entered in `pwdDropscanToken`
- **THEN** all scanboxes of the Dropscan account are retrieved and displayed as confirmation

#### Scenario: Ungültiges Token
- **WHEN** an administrator clicks `cmdDropscanTest` and an invalid API token is entered
- **THEN** an error message is displayed indicating the invalid token

#### Scenario: Mehrere Benutzer mit eigenem Dropscan-Account
- **WHEN** multiple users have configured their own Dropscan API tokens
- **THEN** each user sees only the mailings from their own account's scanboxes

### Requirement: Dropscan-Posteingang anzeigen
The system SHALL provide an overview of all mailings (postal items) across all scanboxes of the current user's Dropscan account within the `pnlDropscan` panel in `ScannerPanel.java`. The overview MUST include scanbox identifier, received date, status, recipient, and an envelope preview. The user SHALL be able to filter by scanbox.

#### Scenario: Posteingang auflisten
- **WHEN** a user navigates to the `pnlDropscan` tab in ScannerPanel and has a configured Dropscan API token
- **THEN** mailings from all scanboxes of the user's account are displayed in a table, sorted by received date

#### Scenario: Manueller Refresh
- **WHEN** a user clicks the refresh button in `pnlDropscan`
- **THEN** the mailing list is reloaded from the Dropscan API

#### Scenario: Automatischer Refresh nach Zuordnung
- **WHEN** a mailing has been successfully imported to a case or a destruction request has been sent
- **THEN** the mailing list in `pnlDropscan` is automatically refreshed

#### Scenario: Nach Scanbox filtern
- **WHEN** a user selects a specific scanbox from the scanbox filter
- **THEN** only mailings from the selected scanbox are displayed

#### Scenario: Alle Scanboxen anzeigen
- **WHEN** a user selects "All" in the scanbox filter
- **THEN** mailings from all scanboxes are displayed

#### Scenario: Posteingang nach Status filtern
- **WHEN** a user selects a status filter (e.g. "received", "scanned")
- **THEN** only mailings with the selected status are displayed

#### Scenario: Umschlag-Vorschau anzeigen
- **WHEN** a user selects a mailing in the list
- **THEN** the envelope image (JPEG) is displayed as a preview

#### Scenario: Keine Dropscan-Credentials konfiguriert
- **WHEN** a user navigates to the `pnlDropscan` tab and has not configured a Dropscan API token
- **THEN** a notice is displayed indicating that a Dropscan API token must be configured in the UserAdministrationDialog

### Requirement: Mailing als Aktendokument importieren
The system SHALL allow importing a scanned mailing as individual documents into a j-lawyer case via the existing BulkSaveDialog. The import MUST download the ZIP from Dropscan (containing individual PDFs per document), extract it, and pass the individual files to BulkSaveDialog which handles duplicate detection, document naming (original filenames from ZIP), and case assignment. No ZIP files SHALL be stored in the case. Case assignment SHALL be manual, assisted by a suggestion list built from OCR data, following the existing ScannerPanel pattern. A checkbox "nach Zuordnung vernichten" in the pnlDropscan header SHALL allow automatic destruction of the physical mailing after successful import.

#### Scenario: Akten-Vorschlagsliste aus OCR-Daten
- **WHEN** a user selects a scanned mailing in the inbox
- **THEN** the OCR plaintext is retrieved from the Dropscan API, file numbers (own and referenced) are matched against the text, and matching cases are displayed as a suggestion list using SaveScanToCasePanel components

#### Scenario: Fallback auf zuletzt bearbeitete Akten
- **WHEN** a user selects a scanned mailing and no file numbers are found in the OCR text or no OCR text is available
- **THEN** the user's recently changed cases are displayed as fallback suggestions

#### Scenario: Einzeldokumente in Akte importieren
- **WHEN** a user selects a suggested case or manually chooses a target case
- **THEN** the ZIP is downloaded from Dropscan, extracted, the individual PDFs (with original filenames from ZIP) are passed to BulkSaveDialog for import into the case, and the mailing's `received_at` timestamp is set as the document date via `ArchiveFileService.setDocumentDate`

#### Scenario: Duplikaterkennung
- **WHEN** a user imports a mailing that contains documents with names already existing in the target case
- **THEN** the BulkSaveDialog's existing duplicate detection handles the conflict

#### Scenario: Nach Zuordnung vernichten
- **WHEN** the checkbox "nach Zuordnung vernichten" is enabled and a mailing is successfully imported
- **THEN** a destruction request is automatically sent to the Dropscan API for that mailing

#### Scenario: Nach Zuordnung nicht vernichten
- **WHEN** the checkbox "nach Zuordnung vernichten" is disabled and a mailing is successfully imported
- **THEN** no destruction request is sent and the physical mailing remains at Dropscan

#### Scenario: Import nicht möglich ohne Scan
- **WHEN** a user selects a mailing with status "received" (not yet scanned)
- **THEN** no suggestion list is shown and a notice is displayed indicating that the mailing must be scanned first

### Requirement: Aktionen auf Mailings ausführen
The system SHALL allow triggering actions on Dropscan mailings from the j-lawyer UI using the current user's credentials: request scan, request forwarding, and request destruction.

#### Scenario: Scan anfordern
- **WHEN** a user selects "Scan" for a mailing with status "received"
- **THEN** a scan request is sent to the Dropscan API and the mailing status is updated

#### Scenario: Vernichtung anfordern
- **WHEN** a user selects "Destroy" for a scanned mailing and confirms the action
- **THEN** a destruction request is sent to the Dropscan API

#### Scenario: Vernichtung ohne Bestätigung
- **WHEN** a user selects "Destroy" for a mailing
- **THEN** a confirmation dialog is displayed before the request is sent

#### Scenario: Weiterleitung anfordern
- **WHEN** a user selects "Forward" for a mailing, provides a forwarding address and a date
- **THEN** a forwarding request is sent to the Dropscan API

### Requirement: Periodisches Polling neuer Mailings
The system SHALL periodically retrieve new mailings from the Dropscan API for all users who have configured a Dropscan API token. For each user, mailings from all scanboxes of the account SHALL be retrieved. The polling interval SHALL use a global default of 15 minutes stored in server settings.

#### Scenario: Automatischer Abruf fuer alle konfigurierten User
- **WHEN** the polling interval has elapsed
- **THEN** new mailings are retrieved from the Dropscan API for each user who has a configured API token, across all their scanboxes

#### Scenario: Benachrichtigung bei neuen Mailings
- **WHEN** new mailings are detected during periodic retrieval for a specific user
- **THEN** that user is notified via an in-app notification

#### Scenario: Kein Polling ohne konfigurierte User
- **WHEN** no users have configured a Dropscan API token
- **THEN** no polling requests are sent to the Dropscan API
