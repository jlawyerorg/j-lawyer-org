## Context
Der bestehende `CalendarSyncService` (Singleton, `@Schedule` Mo–Fr 12:21 und 23:21) und die
ereignisgetriebenen Push-Hooks in `CalendarService` (`eventAdded/Updated/Deleted`) senden
lokale Änderungen an einen CalDAV-Kalender. `NextcloudCalendarConnector.getAllEvents()` und
`getEventByUid()` liegen ungenutzt vor. `ArchiveFileReviewsBean.id` wird bereits als CalDAV
UID verwendet, ein `X-ALT-JLAWYERORG=1` markiert von j-lawyer erzeugte Events. ETag wird
heute nur ad-hoc vor jedem PUT nachgeladen und nicht gespeichert.

Nutzer erwarten Änderungen aus dem Nextcloud-Web bzw. angebundenen mobilen Clients zurück in
j-lawyer und wollen dort auch neu angelegte Termine wiederfinden.

## Goals / Non-Goals
- **Goals**
  - Rückwärts-Sync als Teil des vorhandenen Voll-Sync-Laufs.
  - Deterministische Konfliktauflösung nach *last write wins*.
  - Import fremder Nextcloud-Einträge mit Aktenzuordnung über Aktenzeichenerkennung und
    Standardakte-Fallback.
  - Keine Sync-Schleifen (Pull darf nicht sofort einen Push desselben Zustands auslösen).
- **Non-Goals**
  - Kein neuer, kürzerer Poll-Intervall. Die bestehende `@Schedule`-Kadenz reicht; ein
    schnellerer Puls kann später nachgeschoben werden.
  - Kein Merge auf Feldebene. Bei einem Konflikt gewinnt eine der beiden Seiten vollständig.
  - Keine Migration bestehender, in Nextcloud manuell angelegter Alt-Einträge in einem
    einmaligen Backfill – der reguläre Pull greift automatisch beim nächsten Lauf.
  - Kein UI, um im Nachhinein einem importierten Eintrag eine andere Akte zuzuordnen; die
    normale Bearbeitung eines Wiedervorlage-Eintrags deckt das ab.

## Decisions

### Persistenz des zuletzt beobachteten CalDAV-Zustands
- **Entscheidung:** `ArchiveFileReviewsBean` bekommt drei neue Spalten:
  - `cloud_etag VARCHAR(255) NULL`
  - `cloud_last_modified DATETIME NULL` (Remote-`LAST-MODIFIED` bzw. `DTSTAMP` als UTC)
  - `cloud_last_synced DATETIME NULL` (lokaler Zeitpunkt des erfolgreichen letzten Syncs)
- **Warum:** Ohne persistierten Referenzzustand kann *last write wins* nicht zuverlässig
  entscheiden, welche Seite sich seit dem letzten Sync verändert hat. `cloud_etag` allein
  reicht als Änderungserkennung, `cloud_last_modified` und `cloud_last_synced` sind für die
  Konfliktentscheidung erforderlich.
- **Alternativen:** ETag ad-hoc pro Sync-Lauf neu holen und mit `updatedAt` vergleichen —
  scheitert daran, dass wir das Delta seit letztem Sync nicht kennen.

### Konflikt-Entscheidung
- **Entscheidung:** Für jeden CalDAV-Event UID:
  1. Wenn Remote-`LAST-MODIFIED` > gespeichertes `cloud_last_modified` → Remote hat sich
     geändert.
  2. Wenn lokales `updatedAt` (bzw. `changeDate` je nach vorhandenem Feld) >
     `cloud_last_synced` → Lokal hat sich geändert.
  3. Beide geändert → jüngere absolute Zeit gewinnt. Bei Gleichstand gewinnt Nextcloud
     (deterministisches Tie-Breaking).
  4. Nur eine Seite geändert → diese Seite gewinnt (kein „Konflikt").
- **Warum:** *Last write wins* nach Wunsch, mit deterministischem Tie-Break.
- **Alternativen:** Merge auf Feldebene wäre komplex und fehleranfällig bei divergenten
  Wiederholungsregeln — verworfen.

### Import fremder Einträge: Typableitung, Zielkalender und Aktenzuordnung
- **Entscheidung:** Für jedes Remote-Event ohne bekannte UID:
  1. **Typ aus Event-Form ableiten:**
     - VEVENT ganztägig (`DTSTART` als `VALUE=DATE`, kein Zeitanteil) → **Wiedervorlage**.
     - VEVENT mit `DTSTART`/`DTEND` als Zeitstempel → **Termin**.
     - Fristen (Frist) werden nicht automatisch aus Remote-Events erzeugt.
  2. **Zielkalender wählen:** ersten `CalendarSetup` des abgeleiteten Typs bestimmen, bei
     dem `defaultArchiveFileKey` gesetzt ist. Sortierung deterministisch nach `id`
     aufsteigend.
  3. **Aktenzeichen suchen (analog `MailboxScannerTask.java:1308 ff.`):** alle nicht
     archivierten Aktenzeichen einmalig laden (`caseSvc.getArchiveFileByFileNumberUnrestricted`
     als Auflösung, `isArchived()` überspringen) und `summary.toLowerCase()` bzw.
     `description.toLowerCase()` per `contains(fn.toLowerCase())` prüfen. Der erste
     Treffer bindet die Zielakte.
  4. Ohne Aktenzeichentreffer wird `defaultArchiveFileKey` des in Schritt 2 gewählten
     `CalendarSetup` verwendet.
  5. Existiert kein `CalendarSetup` des passenden Typs mit gesetzter Standardakte, wird
     der Eintrag **nicht** importiert und der Sync-Lauf protokolliert eine Warnung mit UID,
     Typ und `SUMMARY`.
- **Warum:** Wunsch des Auftraggebers; verhindert „waisenlose" Kalendereinträge. Die
  Wiederverwendung des `MailboxScannerTask`-Musters hält Aktenzeichen-Erkennung im gesamten
  Server konsistent.
- **Alternativen:** Automatisch eine synthetische Akte anlegen — verworfen (verschmutzt den
  Aktenbestand). Aktenzeichenerkennung pro Sync-Lauf über eine Regex ohne Aktenpool —
  verworfen (formatabhängig, führt bei rufnummernartigen Strings zu Fehltreffern).

### Ersteller (`createdBy`) importierter Einträge
- **Entscheidung:** `createdBy` bleibt leer. Damit greift die bestehende Regel in
  `calendar-notifications`, wonach ohne Creator keine Ersteller-Benachrichtigung gesendet
  wird.
- **Warum:** Der tatsächliche Ersteller in Nextcloud ist nicht zuverlässig auf einen
  j-lawyer-Nutzer abbildbar.

### Remote-Löschung → lokal „erledigt"
- **Entscheidung:** Verschwindet eine UID auf CalDAV-Seite und wurde der lokale Eintrag
  seit `cloud_last_synced` nicht mehr geändert, wird er lokal **nicht gelöscht**, sondern
  auf `done=1` gesetzt (mit `doneDate` = aktueller Zeitpunkt, `doneBy` = leer). Wurde er
  lokal in der Zwischenzeit geändert, gewinnt die lokale Seite und der Eintrag wird beim
  anschließenden Push in Nextcloud neu angelegt.
- **Warum:** Historie und Nachvollziehbarkeit bleiben erhalten; die geplante Sync-Schleife
  ignoriert erledigte Einträge ohnehin, sodass es zu keinem Wiedereinspielen kommt.
  `CalendarSetup.deleteDone` bleibt orthogonal: es steuert weiterhin, ob eine lokal
  ausgelöste Erledigung Nextcloud-seitig gelöscht wird.
- **Alternativen:** Weiter hart löschen — verworfen (Datenverlust, keine Historie).

### Loop-Schutz
- **Entscheidung:** Der Pull-Pfad ruft die Persistenz nicht über
  `CalendarService.updateArchiveFileReview()` auf, sondern über einen internen Local-Call,
  der `CalendarSyncService.eventUpdated()` **nicht** triggert. Nach erfolgreichem
  Anwenden werden `cloud_etag`, `cloud_last_modified` und `cloud_last_synced` in derselben
  Transaktion aktualisiert.
- **Warum:** Ohne das würde jeder Import sofort einen Rück-Push samt Round-Trip auslösen.

### Historie für Remote-ausgelöste Änderungen
- **Entscheidung:** Jede aus dem Pull-Pfad resultierende Änderung an einem
  Kalendereintrag (Import, inhaltliche Änderung, „erledigt"-Setzen wegen Remote-Löschung)
  schreibt in derselben Transaktion einen Historieneintrag auf der zugehörigen Akte. Der
  Eintrag hat eine sprechende Beschreibung (z. B. „Kalendereintrag aus Nextcloud
  importiert", „Kalendereintrag aus Nextcloud aktualisiert", „Kalendereintrag als
  erledigt gesetzt (Remote-Löschung)"). Der Historien-Ersteller wird als System-Marker
  („Cloud-Sync") gesetzt, kein echter Benutzer.
- **Warum:** Nutzer sollen in der Akten-Historie nachvollziehen können, welche Kalender-
  Änderung von außen kam und wann.
- **Alternativen:** Nur Logfile-Eintrag — verworfen (in der Akte nicht sichtbar).

### Trigger-Kadenz
- **Entscheidung:** Kein neuer Scheduler. Der Pull-Pfad läuft im vorhandenen
  `fullCalendarSync()` (Mo–Fr 12:21 und 23:21). Der Full-Sync-Toggle
  (`SERVERCONF_CLOUDSYNC_CALENDAR_FULLSYNC`) schaltet weiterhin Push **und** neuen Pull
  gemeinsam.
- **Warum:** Minimalinvasiv; kürzere Kadenz kann bei Bedarf später separat spezifiziert
  werden.

## Risks / Trade-offs
- **iOS/macOS-Duplikate:** Der Kommentar im Code deutete früher auf Duplikat-Effekte
  intervallbasierter Pulls hin. Da wir die Pull-Frequenz nicht erhöhen und Events UID-basiert
  matchen, entsteht kein zusätzlicher Client-Duplikatspfad. → Bei UID-Kollisionen wird die
  vorhandene lokale Instanz aktualisiert, nicht dupliziert.
- **Zeitzonenschieflagen bei `LAST-MODIFIED`:** Wir normalisieren beide Seiten strikt auf
  UTC vor dem Vergleich.
- **Aktenzeichen-Fehltreffer:** Bewusst restriktive Erkennung (nur exakte Treffer im
  vorhandenen Format); im Zweifel greift die Standardakte.
- **Alte Einträge ohne `cloud_last_synced`:** Erster Sync-Lauf behandelt sie so, als wären
  sie beidseitig identisch (`cloud_etag` = aktueller Remote-ETag setzen, ohne Änderung).

## Migration Plan
1. Flyway-Migration in `j-lawyer-server-entities/src/main/resources/db/migration/` legt die
   drei Spalten auf der Reviews-Tabelle und die eine Spalte auf der CalendarSetup-Tabelle
   an. Konvention wie im Verzeichnis üblich: `V3_6_0_49__NextcloudBidirectionalSync.sql`
   (nächster freier Slot beim Umsetzen prüfen). Konkrete Tabellennamen aus den JPA-Beans
   im Task-Schritt verifizieren.
2. Deployment: JPA-Beans um die neuen Felder ergänzt.
3. Erster Sync-Lauf initialisiert `cloud_etag`, `cloud_last_modified` und
   `cloud_last_synced` opportunistisch für alle bereits gepushten Einträge, ohne Inhalte
   zu ändern.
4. Rollback: Die neuen Spalten sind nullable; bei Rückzug bleiben sie ungenutzt liegen. Die
   Pull-Logik ist an einen Feature-Guard (bestehender Full-Sync-Toggle) gekoppelt und lässt
   sich pro Instanz deaktivieren.

## Open Questions
- keine offenen Punkte mehr; Aktenzeichenerkennung folgt dem Muster in
  `MailboxScannerTask.java:1308 ff.`, Migrations-Namensschema folgt dem bestehenden
  `V3_6_0_XX__CamelCase.sql` in `j-lawyer-server-entities/src/main/resources/db/migration/`.
