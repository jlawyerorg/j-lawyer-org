# Änderung: Maximierungszustand des Desktop-Hauptfensters wiederherstellen

## Ausgangslage
Der Desktopclient speichert Fensterposition und -größe. Ein maximiert
geschlossenes Hauptfenster kann beim Neustart dennoch im Fenstermodus erscheinen.
Das wurde unter Windows mit einem Monitor reproduziert. Die Tickets #2257 und
#1480 beschreiben das Verhalten und das Risiko beim Wechsel der Bildschirme.

## Vorgeschlagene Änderungen
- Beim bestätigten Beenden den Maximierungszustand in den lokalen
  Client-Einstellungen speichern. Die letzten normalen Fenstermaße bleiben separat.
- Beim Start nach den Startabfragen den gespeicherten Zustand wiederherstellen.
  Bei geänderter Bildschirmkonfiguration muss das Fenster erreichbar bleiben.
- Bestehende Profile mit gespeicherten Fenstermaßen weiterhin unterstützen.

## Geltungsbereich und Auswirkungen
- Geltungsbereich: gemeinsamer Java-Swing-Desktopclient für Windows, Linux und
  macOS. Die Änderung soll auf allen drei Systemen greifen.
- Teststand: Das Ausgangsproblem ist für Windows bestätigt. Verhalten und
  Wirksamkeit unter Linux und macOS müssen separat geprüft werden.
- Betroffen sind der Startablauf, das Hauptfenster und lokale Client-Einstellungen.
  Server, Nebenfenster und Trennbalken liegen außerhalb des Vorschlags.
