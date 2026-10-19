# Claude-Skills für j-lawyer.org

| Skill | Zweck |
|---|---|
| `j-lawyer-vorlagen` | Dokumentvorlagen (DOCX/ODT) neu erstellen, aus einem Beispieldokument ableiten (konkrete Werte → Platzhalter) und bestehende Vorlagen bearbeiten und prüfen |

## Installation

### claude.ai (Browser) und Claude Desktop

1. Archiv bauen: `skills/build-skills.sh` → `skills/dist/j-lawyer-vorlagen.zip`
2. In Claude unter **Einstellungen → Funktionen** die Code-Ausführung bzw. das Erstellen von Dateien aktivieren.
3. Unter **Einstellungen → Funktionen → Skills** auf „Skill hochladen“ klicken und die ZIP-Datei wählen.
4. In einem Chat eine Vorlage oder ein Beispieldokument hochladen und z.B. schreiben:
   „Mach daraus eine j-lawyer-Vorlage“.

Ist der j-lawyer-Connector (MCP) verbunden, nutzt der Skill ihn optional: Er fragt Falldatenblatt-Präfixe
ab und erzeugt Testdokumente in einer Testakte. Vorlagen hochladen kann der Connector nicht.

### Claude Code

- **In diesem Repository** ist der Skill über `.claude/skills/j-lawyer-vorlagen` (Symlink) automatisch verfügbar.
- **Überall:** Symlink im Benutzerverzeichnis anlegen:
  ```bash
  ln -s "$(pwd)/skills/j-lawyer-vorlagen" ~/.claude/skills/j-lawyer-vorlagen
  ```

## Pflege

- Der Platzhalterkatalog (`j-lawyer-vorlagen/scripts/catalog.json` und der generierte Abschnitt in
  `references/platzhalter.md`) wird aus `PlaceHolders.java` und `smarttemplate.groovy` erzeugt:
  `python3 skills/tools/generate_catalog.py`. `build-skills.sh` ruft das automatisch auf.
- Neue Platzhalter brauchen deshalb nur `@PlaceHolderInfo` in `PlaceHolders.java`. Strukturregeln
  für Tabellenplatzhalter stehen in `generate_catalog.py` (`TABLE_PLACEHOLDERS`).
- Tests: `python3 -m unittest discover -s skills/j-lawyer-vorlagen/tests` (nutzt die Testvorlagen
  aus `j-lawyer-server-ejb/src/test/resources/data`).
- Das Werkzeug `jlvorlage.py` verwendet nur die Python-Standardbibliothek, damit es in der
  Code-Ausführung von claude.ai ohne Paketinstallation läuft.
