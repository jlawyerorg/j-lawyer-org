## 1. Datenmodell und Migration
- [ ] 1.1 Tabellennamen für `ArchiveFileReviewsBean` und `CalendarSetup` in der Datenbank verifizieren (Namen aus den JPA-Beans in `j-lawyer-server-entities` ableiten)
- [ ] 1.2 Flyway-Migration in `j-lawyer-server-entities/src/main/resources/db/migration/` anlegen: nächster freier Slot im Schema `V3_6_0_XX__NextcloudBidirectionalSync.sql` (Nummer beim Umsetzen anhand des Verzeichnisses festlegen), neue Spalten `cloud_etag VARCHAR(255) NULL`, `cloud_last_modified DATETIME NULL`, `cloud_last_synced DATETIME NULL` auf der Reviews-Tabelle sowie `default_archive_file_key VARCHAR(...) NULL` auf der CalendarSetup-Tabelle
- [ ] 1.3 `ArchiveFileReviewsBean` um die drei neuen Felder inkl. Getter/Setter und JPA-Mapping erweitern
- [ ] 1.4 `CalendarSetup` um `defaultArchiveFileKey` (FK bzw. String-Key auf `ArchiveFileBean`) erweitern
- [ ] 1.5 Falls das Persistence-Model die Spalten zwingend braucht: `persistence.xml` / Mapping-Anmerkungen aktualisieren

## 2. CalDAV-Read-Pfad ergänzen
- [ ] 2.1 `NextcloudCalendarConnector.getAllEvents()` so ergänzen, dass pro Event `href`, `etag`, `LAST-MODIFIED` (UTC) und der geparste VEvent verfügbar sind
- [ ] 2.2 Für den Push-Pfad denselben Rückgabewert nach einem erfolgreichen PUT erzeugen, damit `cloud_etag`/`cloud_last_modified` gesetzt werden können
- [ ] 2.3 Vorhandene Reads (`getEventByUid`) an dieselbe Ergebnisstruktur angleichen

## 3. Aktenzeichen-Erkennung
- [ ] 3.1 Wiederverwendbaren Helper extrahieren, der dem Muster in `MailboxScannerTask.java:1308 ff.` folgt: alle nicht archivierten Aktenzeichen einmalig laden und in einem gegebenen String per `contains(fn.toLowerCase())` matchen; archivierte Akten überspringen
- [ ] 3.2 Nutzung des Helpers in Mail- und Kalender-Sync konsolidieren (falls sinnvoll und ohne Regressionen im Mail-Pfad)
- [ ] 3.3 Tests mit typischen und kantigen Aktenzeichen-Formaten sowie mit archivierten Zielakten (Skip)

## 4. Server-Sync: Rückrichtung, Typableitung, Konflikt
- [ ] 4.1 In `CalendarSyncService.fullCalendarSync()` nach dem bestehenden Push-Block einen Pull-Block ergänzen: alle CalDAV-Events pro `CalendarSetup` lesen und mit den lokal bekannten Reviews (per UID) abgleichen
- [ ] 4.2 Konfliktentscheidung nach *last write wins* implementieren (Vergleich Remote-`LAST-MODIFIED` gegen `cloud_last_modified`; lokales `updatedAt`/`changeDate` gegen `cloud_last_synced`; bei Gleichstand gewinnt Nextcloud)
- [ ] 4.3 Für unbekannte UIDs: Typ aus Event-Form ableiten (ganztägig → Wiedervorlage, zeitgebunden → Termin; Frist nicht auto-erzeugen)
- [ ] 4.4 Zielkalender auswählen: erste `CalendarSetup`-Instanz des abgeleiteten Typs mit gesetztem `defaultArchiveFileKey`, deterministisch nach `id` sortiert
- [ ] 4.5 Aktenzuordnung: erst Aktenzeichensuche (siehe 3.1) in Titel/Beschreibung; bei Treffer diese Akte, sonst `defaultArchiveFileKey` der in 4.4 gewählten `CalendarSetup`
- [ ] 4.6 Existiert kein `CalendarSetup` des abgeleiteten Typs mit Standardakte: Import überspringen und `WARN` mit UID, Typ und `SUMMARY` loggen
- [ ] 4.7 Remote-Löschung: fehlt eine zuvor synchronisierte UID auf Remote-Seite und wurde der lokale Eintrag seit `cloud_last_synced` nicht geändert → lokal `done=1` setzen (mit `doneDate`, `doneBy` leer); wurde er lokal geändert → beim Push in Nextcloud neu anlegen
- [ ] 4.8 Loop-Schutz: Pull-Pfad persistiert ohne den `CalendarService`-Push-Hook aufzurufen; nach jedem Apply werden `cloud_etag`, `cloud_last_modified` und `cloud_last_synced` in derselben Transaktion aktualisiert
- [ ] 4.9 `createdBy` importierter Einträge bleibt leer (bestehende Suppression in `calendar-notifications` greift)
- [ ] 4.10 Erster Sync-Lauf initialisiert Cloud-Metadaten für bereits gepushte Einträge, ohne Inhalte zu ändern
- [ ] 4.11 Feature-Guard: der bestehende Toggle `SERVERCONF_CLOUDSYNC_CALENDAR_FULLSYNC` schaltet auch die Pull-Richtung

## 5. Historien-Einträge für Remote-Änderungen
- [ ] 5.1 Bestehenden Historien-Schreibpfad identifizieren (`CaseHistoryService` bzw. äquivalente EJB-API in `j-lawyer-server-ejb`) und für den Sync-Kontext nutzbar machen
- [ ] 5.2 Beim Import einer neuen UID: Historieneintrag „Kalendereintrag aus Nextcloud importiert (<SUMMARY>)" auf der Zielakte, in derselben Transaktion
- [ ] 5.3 Bei einer aus Remote übernommenen Änderung: Historieneintrag „Kalendereintrag aus Nextcloud aktualisiert (<SUMMARY>)"
- [ ] 5.4 Bei „erledigt"-Setzen wegen Remote-Löschung: Historieneintrag „Kalendereintrag als erledigt gesetzt (Remote-Löschung in Nextcloud)"
- [ ] 5.5 Historien-Autor auf einen deterministischen System-Marker („Cloud-Sync") setzen, nicht auf einen realen Benutzer

## 6. Client-UI
- [ ] 6.1 Im Kalendereinstellungs-Dialog (`j-lawyer-client/.../CalendarSetupDialog`) einen Aktenauswähler „Standardakte für externe Einträge" ergänzen, konsistent auch in der `.form`-Datei
- [ ] 6.2 Persistenzpfad im Client: `defaultArchiveFileKey` bei Speichern und Laden übernehmen
- [ ] 6.3 Hilfetext/Tooltip: Erklärung, dass Aktenzeichen-Erkennung Vorrang hat und dass der Kalendertyp die Zielauswahl bestimmt

## 7. Tests
- [ ] 7.1 EJB-Test: Push, Pull, Konfliktentscheidung und Loop-Schutz mit einem Mock-`NextcloudCalendarConnector`
- [ ] 7.2 EJB-Test: Import mit Aktenzeichen-Erkennung, mit Standardakte, ohne beides (Skip + WARN)
- [ ] 7.3 EJB-Test: Typableitung ganztägig → Wiedervorlage, zeitgebunden → Termin; Zielauswahl anhand der ersten passenden `CalendarSetup` mit Standardakte
- [ ] 7.4 EJB-Test: Remote-Löschung setzt `done=1` (kein Löschen), inklusive Verhalten bei paralleler lokaler Änderung
- [ ] 7.5 EJB-Test: Historieneinträge werden für Import, Update und Erledigung geschrieben, Autor ist der System-Marker
- [ ] 7.6 Manuelle Verifikation gegen eine Nextcloud-Testinstanz (Sync in beide Richtungen, iOS-/Web-Client)

## 8. Doku
- [ ] 8.1 CHANGELOG-/Release-Notes-Eintrag
- [ ] 8.2 Kurzabschnitt in der Benutzerdoku: Standardakte pro Kalender, Typableitung (ganztägig/zeitgebunden), Verhalten bei Remote-Löschung
