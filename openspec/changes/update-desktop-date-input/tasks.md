## 1. Inventur vor Umsetzung

- [ ] 1.1 Die bereits gefundene Liste in `date-field-inventory.md` gegen sämtliche Java- und `.form`-Dateien sowie Tabellenzellen und dynamisch erzeugte Eingaben abgleichen und ergänzen: Kontakte, Buchhaltung, Rechnungen, Fristen, Termine, Berichte, Einstellungen und weitere Masken. Kalenderaufrufe, Formatter und Namen einzeln prüfen; keiner dieser Hinweise ist allein vollständig.
- [ ] 1.2 Je Kandidat in der Feldübersicht direkte Editierbarkeit, Datentyp, Parser, Speicherweg und fachliche Datumsgrenzen bestätigen; die zwei `JDateChooser` samt internem Texteditor und die editierbare Datumsspalte gezielt zur Laufzeit prüfen. Ausnahmen (Freitext, Anzeige, Datum-Uhrzeit, nur Kalender) begründen.

## 2. Implementierung nach Review/Freigabe

- [ ] 2.1 Gemeinsame strikte Prüfung echter Daten und Normalisierung von genau `TTMMJJJJ` auf `TT.MM.JJJJ` schaffen; punktiertes Datum und vierstelliges Jahr zulassen, keine Jahrhundertvermutung oder allgemeine Zukunftsgrenze.
- [ ] 2.2 Gemeinsame Logik an jedes bestätigte Feld aus 1.1 und dessen Speicherpfad anschließen, auch `JFormattedTextField` gesondert berücksichtigen; verständliche Fehler, sichtbarer Formathinweis und konsistente `.form`-Dateien.
- [ ] 2.3 Persönliche Daten in Hauptmaske und Schnellerfassung zusätzlich auf Zukunft und bei neuen Widersprüchen Tod vor Geburt prüfen; Leerwerte, unveränderte Altwerte und Altersanzeige berücksichtigen.
- [ ] 2.4 Mit fiktiven Daten automatisiert und manuell Punkt-/Zifferneingabe, ungültige Daten, Schaltjahr, Zukunftsregeln, Altwerte, Kontakte, Buchhaltung, Fälligkeiten, Fristen, Kalenderauswahl, Datumstabellenzellen und die zwei `JDateChooser`-Texteditoren prüfen.
- [ ] 2.5 Abgleich Feldinventar/Implementierung, `git diff --check`, Client-Build und passende Tests; keine Web-/Server-/DB-Änderungen oder echten Mandantendaten.
