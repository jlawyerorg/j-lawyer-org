## Context

`MultiCalDialog` erhält erst beim Öffnen irgendein `JTextField` und ist kein globales Kennzeichen für Datumsfelder. Kontakte nutzen einfache Textfelder, `CaseAccountEntryPanel.txtDate` ein `JFormattedTextField` mit `DateFormatter`; manche Rechnungs- und Zahlungsfelder sind nicht direkt bearbeitbar. Datum-Uhrzeit-Felder haben abweichende Formate.

## Goals / Non-Goals

- Goal: Achtstellige Eingaben in allen direkt bearbeitbaren reinen Datumsfeldern des installierten Clients, ohne Freitext umzudeuten.
- Non-Goals: Browserclient, Server-/REST-/Importvalidierung, Datenmigration oder neue Tastatureingabe in bislang kalendergebundenen Feldern.

## Decisions

- Vor Implementierung alle geeigneten Felder inventarisieren und gemeinsame Normalisierung nur ausdrücklich daran anschließen. Weder Feldnamen noch Kalenderaufrufe genügen als automatische Erkennung.
- Kalendergültigkeit und Format allgemein prüfen; Zukunft und Geburts-/Sterbereihenfolge nur für persönliche Daten. Bisherige spezifische Regeln sonstiger Felder wahren.
- Erst neue Eingaben bei Übernahme normalisieren, gespeicherte Altwerte beim Laden nicht verändern. Bei formatierten Swing-Feldern Eingabe und Commit gesondert integrieren.

## Risks / Trade-offs

- Ausgelassene Felder erzeugen inkonsistente Eingabe: Inventar mit implementierten Feldern und manuellen Tests abgleichen.
- Globale Texterkennung könnte Notizen ändern: keine globalen Listener für alle Textfelder.

## Open Questions

- Jens: Soll #1687 zusätzlich die separate Browseroberfläche `j-lawyer-web` umfassen? Dieser Vorschlag umfasst sie ausdrücklich nicht.
