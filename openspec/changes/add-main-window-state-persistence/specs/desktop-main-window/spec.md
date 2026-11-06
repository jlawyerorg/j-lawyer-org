## ADDED Requirements

### Requirement: Zustand des Desktop-Hauptfensters wiederherstellen
Der gemeinsame Desktopclient für Windows, Linux und macOS SHALL beim
bestätigten Beenden den Maximierungszustand des Hauptfensters getrennt von
seiner letzten normalen Position und Größe lokal speichern. Beim letzten
Wiederherstellen im Startablauf SHALL der Client die Fenstermaße gegen die
verfügbaren Bildschirme prüfen und den gespeicherten Zustand anwenden.

#### Scenario: Neustart nach maximiertem Beenden
- **GIVEN** das Hauptfenster ist auf einem verfügbaren Bildschirm maximiert
- **WHEN** der Nutzer den Client über X oder Datei → Beenden schließt und neu startet
- **THEN** das Hauptfenster ist nach den Startabfragen wieder maximiert

#### Scenario: Neustart nach Beenden im Fenstermodus
- **GIVEN** das Hauptfenster ist im Fenstermodus und vollständig nutzbar
- **WHEN** der Nutzer den Client beendet und mit demselben Monitoraufbau neu startet
- **THEN** das Hauptfenster erscheint im Fenstermodus mit der gespeicherten Position und Größe

#### Scenario: Vorher genutzter Bildschirm fehlt
- **GIVEN** die gespeicherte Fensterposition wäre mit den verfügbaren Bildschirmen nicht erreichbar
- **WHEN** der Client das Hauptfenster wiederherstellt
- **THEN** das Hauptfenster erscheint an einer nutzbaren Position auf einem verfügbaren Bildschirm

#### Scenario: Beenden im minimierten Zustand
- **GIVEN** das Hauptfenster ist beim bestätigten Beenden minimiert
- **WHEN** der Nutzer den Client neu startet
- **THEN** das Hauptfenster erscheint sichtbar und wird nicht minimiert wiederhergestellt

#### Scenario: Bestehendes Profil mit Fenstermaßen
- **GIVEN** das Profil enthält gespeicherte Fenstermaße und keinen gespeicherten Maximierungszustand
- **WHEN** der Client startet
- **THEN** die bisherige Wiederherstellung der Fenstermaße und ihr Rückfallverhalten gelten weiter

#### Scenario: Beenden wird abgebrochen
- **WHEN** der Nutzer das Beenden abbricht
- **THEN** der Client bleibt geöffnet und speichert keinen neuen Maximierungszustand als Einstellung für den nächsten Start
