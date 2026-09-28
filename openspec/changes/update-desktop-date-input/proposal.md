# Change: Achtstellige Datumseingabe im installierten Client

## Why

Bei Kontakten können zweistellige Jahresangaben zu falschen Altersangaben führen (#1169). Zusätzlich behandelt #1687 die Eingabe von Datumsangaben ohne Punkte. Dieser Vorschlag verbindet eine eindeutige vierstellige Jahresangabe mit der vereinfachten Eingabe in den dafür vorgesehenen Feldern des installierten Clients.

## What Changes

- Alle direkt bearbeitbaren Felder für reine Kalenderdaten akzeptieren `TTMMJJJJ` und `TT.MM.JJJJ`. In leeren Eingabefeldern zeigt ein grauer, nicht gespeicherter Hinweis `TTMMJJJJ` die einfache Eingabeform. Gültige acht Ziffern ohne Punkte erscheinen spätestens beim Wechsel in ein anderes Feld als `TT.MM.JJJJ`. Ungültige Kalenderdaten und zweistellige Jahre bleiben zur Korrektur sichtbar und werden beim Übernehmen mit verständlicher Fehlermeldung abgewiesen.
- Bei neu eingegebenen/geänderten Geburts- und Sterbedaten zusätzlich Zukunftsdaten und einen neu entstandenen Widerspruch „Tod vor Geburt“ abweisen. Optionale Leerwerte sind erlaubt. Unveränderte fehlerhafte Altwerte bleiben erhalten und blockieren Änderungen anderer Kontaktdaten nicht.
- Sachliche Daten wie Fälligkeiten dürfen in der Zukunft liegen, soweit ihre eigenen fachlichen Regeln es erlauben. Achtstellige Zahlen in Notizen/Freitext, Datum-Uhrzeit-Felder, bloße Anzeigen und bislang nur per Kalender bedienbare Felder werden nicht in eine neue Tastatureingabe einbezogen.
- Die [Feldübersicht](date-field-inventory.md) benennt konkret die derzeit gefundenen editierbaren Datumsfelder und getrennt davon Kalender-/Anzeigefelder. Sie ist vor der Implementierung gegen alle Masken zu prüfen und bei weiteren Funden zu ergänzen.
- **Geltungsbereich zur Abstimmung:** Die separate Browseroberfläche `j-lawyer-web` ist nicht Teil dieses Vorschlags. Falls #1687 sie ebenfalls umfassen soll, bitte vor der Umsetzung den Geltungsbereich ergänzen.

## Impact

- **Affected specs:** neue Capabilities `desktop-date-entry` und `contact-personal-dates`.
- **Affected code:** `j-lawyer-client`, gemeinsame Datumslogik und die in der Feldübersicht aufgeführten direkt editierbaren reinen Datumsfelder einschließlich Kontakte und Buchhaltung; ergänzende Funde aus der abschließenden Inventur sowie nötige `.form`-Dateien.
- **Not affected:** Webclient, Server, REST, Importe, Datenbank, gespeicherte Altwerte, Notizen, Datum-Uhrzeit und Kalender-only-Felder; zusätzliche Trennzeichen aus #1247.
- **Rollback:** Client-Änderungen zurücknehmen; keine Datenmigration.
