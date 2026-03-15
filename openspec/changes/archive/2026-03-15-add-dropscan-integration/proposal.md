# Change: Dropscan-Integration für digitalen Posteingang

## Why
Kanzleien erhalten täglich physische Post (Gerichtsschreiben, Mandantenkorrespondenz, Bescheide), die manuell geöffnet, gescannt und Akten zugeordnet werden muss. Dropscan bietet einen Cloud-basierten Scan-Service mit virtuellen Postfächern, der diesen Prozess digitalisiert. Eine Integration in j-lawyer ermöglicht es, eingehende Post automatisch zu empfangen, als Dokumente an Akten anzuhängen und Aktionen (Scannen, Weiterleiten, Vernichten) direkt aus der Anwendung heraus auszulösen.

## What Changes
- Dropscan API-Token wird pro Benutzer am User-Objekt gespeichert, da jeder User einen eigenen Dropscan-Account haben kann. Alle Scanboxen des Accounts werden automatisch genutzt (keine Scanbox-ID-Konfiguration nötig)
- Globales Polling-Intervall in ServerSettings mit sinnvollem Default (kein UI-Konfigurationsfeld nötig)
- Neuer EJB-Service `DropscanService` für die Kommunikation mit der Dropscan REST API
- Neues Client-Panel zur Anzeige und Verwaltung von Dropscan-Posteingang (Mailings)
- Möglichkeit, gescannte Dokumente (PDF) direkt als Aktendokument zu importieren
- Aktionssteuerung: Scan, Weiterleitung und Vernichtung von Post aus j-lawyer heraus
- Periodischer Hintergrund-Task zum Abruf neuer Mailings (Polling) über alle User mit konfiguriertem Dropscan-Account

## Impact
- Affected specs: Neue Capability `dropscan-integration`
- Affected code:
  - `j-lawyer-server-api`: Neues Remote-Interface `DropscanServiceRemote`
  - `j-lawyer-server/j-lawyer-server-ejb`: Neuer Service `DropscanService`, Erweiterung `ScheduledTasksService`
  - `j-lawyer-server-entities`: Erweiterung `AppUserBean` um Dropscan-Feld (API-Token)
  - `j-lawyer-client`: Dropscan-Posteingang als `pnlDropscan` in `ScannerPanel.java` integriert
  - `j-lawyer-server-common`: HTTP-Client-Utilities für Dropscan API-Aufrufe
