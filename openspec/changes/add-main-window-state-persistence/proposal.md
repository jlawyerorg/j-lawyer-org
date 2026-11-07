# Änderung: Maximierungszustand des Desktop-Hauptfensters wiederherstellen

## Ausgangslage
Der Desktopclient speichert Fensterposition und -größe. Ein maximiert
geschlossenes Hauptfenster kann beim Neustart dennoch im Fenstermodus erscheinen.
Das wurde unter Windows mit einem Monitor reproduziert. Die Tickets #2257 und
#1480 beschreiben das Verhalten und das Risiko beim Wechsel der Bildschirme.

## Vorgeschlagene Änderungen
- Beim bestätigten Beenden den Maximierungszustand in den lokalen
  Client-Einstellungen speichern. Position und Größe werden nur im Zustand NORMAL erfasst.
- Beim Start nach den Startabfragen den gespeicherten Zustand wiederherstellen.
  Den Zielbildschirm anhand der normalen Fenstermaße ermitteln. Ist die
  Titelleiste dort nicht ausreichend erreichbar, maximiert auf dem Hauptbildschirm starten.
- Bestehende Profile mit gespeicherten Fenstermaßen weiterhin unterstützen.

## Geltungsbereich und Auswirkungen
- Geltungsbereich: gemeinsamer Java-Swing-Desktopclient für Windows, Linux und
  macOS. Die Änderung soll auf allen drei Systemen greifen.
- Testplan: Das Ausgangsproblem ist unter Windows bestätigt. Windows-Tests sind
  mit einem Einzelmonitor und einem erweiterten Desktop geplant. Linux wird
  unter SteamOS geprüft. Für macOS steht derzeit kein Testgerät zur Verfügung.
  Unter macOS ist der Zoomzustand gemeint. Der Vollbildmodus bleibt außerhalb
  des Vorschlags.
- Betroffen sind der Startablauf, das Hauptfenster und lokale Client-Einstellungen.
  Server, Nebenfenster und Trennbalken liegen außerhalb des Vorschlags.
