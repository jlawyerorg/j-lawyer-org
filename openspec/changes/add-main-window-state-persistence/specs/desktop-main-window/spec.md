## ADDED Requirements

### Requirement: Normale Fenstermaße und Maximierung getrennt speichern
Der gemeinsame Desktopclient für Windows, Linux und macOS SHALL Position und
Größe des Hauptfensters nur im Zustand `NORMAL` als normale Fenstermaße
speichern. Der Maximierungszustand SHALL getrennt davon in den lokalen
Einstellungen gespeichert werden, nachdem alle Abbruchmöglichkeiten
durchlaufen sind und unmittelbar bevor die Einstellungen geschrieben werden.

#### Scenario: Beenden wird abgebrochen
- **WHEN** der Nutzer das Beenden abbricht
- **THEN** der Client bleibt geöffnet und speichert keinen neuen Maximierungszustand als Einstellung für den nächsten Start

#### Scenario: Fensterzustand verändert sich
- **GIVEN** normale Fenstermaße sind gespeichert
- **WHEN** das Fenster maximiert oder minimiert wird
- **THEN** die zuletzt gespeicherten normalen Maße bleiben erhalten

### Requirement: Zielbildschirm und sichtbare Fenstermaße bestimmen
Der Client SHALL Breite und Höhe auf die nutzbare Arbeitsfläche des
Zielbildschirms begrenzen. Die Titelleiste muss dort über ihre volle Höhe und
auf mindestens 64 zusammenhängenden logischen Pixeln Breite sichtbar sein.
Bei mehreren geeigneten Bildschirmen gilt die größte Überlappung mit dem
Fenster. Ohne geeigneten Bildschirm SHALL der Client maximiert auf dem
Hauptbildschirm öffnen.

#### Scenario: Vorher genutzter Bildschirm fehlt
- **GIVEN** die Titelleiste erfüllt auf keinem verfügbaren Bildschirm das Sichtbarkeitskriterium
- **WHEN** der Client das Hauptfenster wiederherstellt
- **THEN** das Hauptfenster öffnet maximiert auf dem Hauptbildschirm

#### Scenario: Bestehendes Profil mit Fenstermaßen
- **GIVEN** das Profil enthält gespeicherte Fenstermaße und keinen gespeicherten Maximierungszustand
- **WHEN** der Client startet
- **THEN** der Client verwendet die vorhandenen Maße im Fenstermodus, wenn sie die Prüfung bestehen, andernfalls maximiert er auf dem Hauptbildschirm

### Requirement: Gespeicherten Fensterzustand beim Start anwenden
Nach den Startabfragen SHALL der Client zuerst die normalen Fenstermaße auf
dem Zielbildschirm setzen und dann den gespeicherten Zustand anwenden. Eine
vollständige Maximierung bleibt beim Beenden aus minimiertem Zustand erhalten.
Minimierung und Teilmaximierung werden beim Start nicht wiederhergestellt.

#### Scenario: Neustart nach maximiertem Beenden
- **GIVEN** das Hauptfenster ist auf einem verfügbaren Bildschirm maximiert
- **WHEN** der Nutzer den Client über X oder Datei → Beenden schließt und neu startet
- **THEN** das Hauptfenster ist nach den Startabfragen wieder maximiert

#### Scenario: Neustart nach Beenden im Fenstermodus
- **GIVEN** das Hauptfenster ist im Fenstermodus und vollständig nutzbar
- **WHEN** der Nutzer den Client beendet und mit demselben Monitoraufbau neu startet
- **THEN** das Hauptfenster erscheint im Fenstermodus mit der gespeicherten Position und Größe

#### Scenario: Maximiert auf dem Zweitbildschirm beendet
- **GIVEN** das Hauptfenster wurde auf einen zweiten Bildschirm verschoben und dort maximiert beendet
- **WHEN** der Client mit weiterhin verfügbarem Zweitbildschirm neu startet
- **THEN** das Hauptfenster erscheint wieder maximiert auf demselben Bildschirm

#### Scenario: Minimiert beendet nach vorheriger Maximierung
- **GIVEN** das Hauptfenster war vor dem Minimieren vollständig maximiert
- **WHEN** der Client minimiert beendet und neu gestartet wird
- **THEN** das Hauptfenster erscheint wieder maximiert, sofern die gespeicherten Maße nutzbar sind

#### Scenario: Minimiert beendet aus dem Fenstermodus
- **GIVEN** das Hauptfenster war vor dem Minimieren im Fenstermodus und hat nutzbare gespeicherte Maße
- **WHEN** der Client minimiert beendet und neu gestartet wird
- **THEN** das Hauptfenster erscheint im Fenstermodus mit den zuletzt gespeicherten normalen Maßen
