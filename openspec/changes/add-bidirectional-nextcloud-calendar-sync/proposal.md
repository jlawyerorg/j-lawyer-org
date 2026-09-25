# Change: Add bidirectional Nextcloud calendar sync

## Why
Die Nextcloud-Kalendersynchronisation läuft heute nur in eine Richtung: `CalendarSyncService`
schiebt lokale Wiedervorlagen, Fristen und Termine als CalDAV-Events zu Nextcloud, liest aber
nichts zurück. Änderungen, die Nutzer direkt im Nextcloud-Web oder in daran angebundenen
Clients (iOS/Android/Thunderbird) vornehmen, gehen in j-lawyer verloren; Nextcloud ist damit
faktisch nur ein Anzeigemedium. Ziel ist eine echte bidirektionale Synchronisation, damit
externe Änderungen zurückfließen und neu in Nextcloud angelegte Einträge in j-lawyer sichtbar
werden.

## What Changes
- **Rückwärts-Sync im vorhandenen Voll-Sync:** `CalendarSyncService.fullCalendarSync()` liest
  zusätzlich alle Events des CalDAV-Kalenders und gleicht sie gegen `ArchiveFileReviewsBean`
  ab. Der bereits vorhandene, aber ungenutzte `NextcloudCalendarConnector.getAllEvents()`
  wird konsumiert.
- **Konfliktauflösung nach *last write wins*:** Bei zeitgleich geänderten Einträgen gewinnt
  die Seite mit der neueren Änderungszeit; Grundlage ist ein pro Eintrag persistierter,
  zuletzt beobachteter CalDAV-Zustand (ETag + Remote-LastModified).
- **Import fremder Nextcloud-Einträge:** Für unbekannte Events wird aus Titel und
  Beschreibung ein Aktenzeichen gesucht (analog `MailboxScannerTask` ab Zeile 1306: alle
  Aktenzeichen einmalig laden und in den Strings suchen; archivierte Akten überspringen).
  Wird ein Aktenzeichen gefunden, hängt der neue Eintrag an dieser Akte, andernfalls an
  einer Standardakte.
- **Typableitung aus Event-Form:** Ganztägige Remote-Events werden zu Wiedervorlagen,
  zeitgebundene (von–bis) zu Terminen. Für Fristen findet kein automatischer Import statt.
- **Standardakte am Kalender:** `CalendarSetup` erhält ein neues Feld
  `defaultArchiveFileKey`; im Kalendereinstellungs-Dialog wird ein Aktenauswähler ergänzt.
  Zielkalender für importierte Einträge ist der erste `CalendarSetup` des passenden Typs
  (Wiedervorlage bzw. Termin), der eine Standardakte gesetzt hat.
- **Remote-Löschungen als „erledigt":** Verschwindet ein zuvor synchronisierter Eintrag in
  Nextcloud und wurde er lokal seither nicht geändert, wird er in j-lawyer **nicht**
  gelöscht, sondern auf `done=1` gesetzt. Damit bleibt die Historie erhalten.
- **Historieneinträge für Remote-Änderungen:** Jede aus dem Remote-Sync resultierende
  Änderung (Import, Update, „erledigt"-Setzen) erzeugt in der jeweiligen Akte einen
  Historieneintrag, kenntlich als vom Cloud-Sync ausgelöst.
- **Datenmodell (additiv):** neue Spalten
  - `CalendarSetup.default_archive_file_key`
  - `ArchiveFileReviewsBean.cloud_etag`, `ArchiveFileReviewsBean.cloud_last_modified`,
    `ArchiveFileReviewsBean.cloud_last_synced`
  via SQL-Migrationsskript; JPA bleibt bei `hbm2ddl=validate`.
- **Sync-Loop-Schutz:** Ein Update, das aus der Pull-Richtung stammt, löst im selben Zyklus
  keinen erneuten Push aus.
- **Bestehendes Verhalten bleibt:** `CalendarSetup.deleteDone`, die planmäßigen Sync-Zeiten
  und die ereignisgetriebenen Push-Aufrufe (`eventAdded/Updated/Deleted`) bleiben
  unverändert.

## Impact
- **Neue Spec:** `calendar-cloud-sync` (bislang keine Spec zu CalDAV-Sync vorhanden)
- **Betroffene Module/Dateien:**
  - `j-lawyer-server/j-lawyer-server-ejb/.../services/CalendarSyncService.java` — Pull-Pfad,
    Konfliktentscheidung, Loop-Schutz
  - `j-lawyer-cloud/.../NextcloudCalendarConnector.java` — bestehende Read-Methoden werden
    konsumiert; ggf. Ergänzung um ETag/LastModified in der Rückgabe
  - `j-lawyer-server-entities/.../CalendarSetup.java` — neues Feld
    `defaultArchiveFileKey`
  - `j-lawyer-server-entities/.../ArchiveFileReviewsBean.java` — neue Felder für den
    zuletzt beobachteten CalDAV-Zustand
  - `j-lawyer-client/.../CalendarSetupDialog` (+ `.form`) — Standardakte-Picker
  - Flyway-Migration in `j-lawyer-server-entities/src/main/resources/db/migration/`
    (nächster Slot z. B. `V3_6_0_49__NextcloudBidirectionalSync.sql`)
  - Historien-Schreibpfad für Remote-ausgelöste Änderungen (`CaseHistoryService` bzw.
    passende bestehende API)
- **Betroffene Nachbar-Specs (nur zur Kenntnis, keine Delta):**
  - `calendar-overview` — die Change-Version wird durch Pull-Writes ebenfalls hochgezählt,
    das existierende Contract-Verhalten passt bereits.
  - `calendar-notifications` — Import-Einträge werden ohne `createdBy` angelegt, wodurch
    keine Ersteller-Benachrichtigung ausgelöst wird (bestehende Suppression greift).
