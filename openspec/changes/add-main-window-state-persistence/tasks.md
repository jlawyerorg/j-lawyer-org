## 1. Umsetzung
- [ ] 1.1 Letzte normale Fensterposition und -größe getrennt vom Maximierungszustand halten.
- [ ] 1.2 Zustand beim bestätigten Beenden über den gemeinsamen Schließpfad unmittelbar vor dem Speichern der Einstellungen erfassen.
- [ ] 1.3 Nach den Startabfragen zuerst die normalen Maße auf dem Zielbildschirm setzen, dann den Zustand anwenden. Prüfen, ob der frühe Wiederherstellungsaufruf erforderlich bleibt.
- [ ] 1.4 Neue Java-Dateien einschließlich Tests mit dem vollständigen AGPLv3-Vorspann des Projekts versehen.

## 2. Prüfung
- [ ] 2.1 Desktopclient mit dem Maven-Setup des Projekts und JDK 17 bauen.
- [ ] 2.2 Unter Windows mit isoliertem Profil normal und maximiert beenden, jeweils über X und Datei → Beenden. Danach erneut starten.
- [ ] 2.3 Bestehendes Profil, minimiertes Beenden und abgebrochenes Beenden prüfen.
- [ ] 2.4 Unter Windows die Wiederherstellung auf einem zweiten Bildschirm und den Rückfall nach dessen Entfernen prüfen. Virtuellen und physischen Monitoraufbau im Testbericht getrennt ausweisen.
- [ ] 2.5 Unter SteamOS prüfen. macOS im PR als offen ausweisen, solange kein Testgerät verfügbar ist.
- [ ] 2.6 Die Geometrieprüfung ohne Oberfläche automatisiert testen: anderer und fehlender Zielbildschirm, zu große Maße, unzureichend sichtbare Titelleiste und Rückfall.
