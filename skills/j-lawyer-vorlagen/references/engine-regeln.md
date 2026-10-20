# So verarbeitet der j-lawyer-Server Vorlagen

Diese Regeln stammen aus dem Server-Code (DOCX: `MicrosoftOfficeAccess`, Apache POI; ODT:
`LibreOfficeAccess`, ODF Toolkit). Sie erklären, warum ein Platzhalter manchmal nicht ersetzt wird.
`validate` prüft die meisten automatisch, `normalize` behebt die meisten automatisch.

## Wo wird ersetzt?

| Bereich | DOCX | ODT |
|---|---|---|
| Haupttext, Tabellen (auch verschachtelt) | ja | ja |
| Kopf- und Fußzeilen | ja | ja |
| Textfelder / Textboxen / Rahmen | ja, aber nur wenn der Platzhalter vollständig in **einem** Run steht | ja |
| Hyperlinks, Inhaltssteuerelemente (`w:sdt`), nachverfolgte Änderungen (`w:ins`), Feldfunktionen | **nein** | Hyperlinks: unsicher |
| Fuß-/Endnoten, Kommentare | nein | – |
| Tabellenplatzhalter (`TABELLE_1`, `BEL_TABELLE`, `ZE_*`, `BEL_GIROCODE`) und `BELP_*`-Zeilen | nur Tabellen des Haupttextes, nicht verschachtelt, nicht Kopf-/Fußzeile | Haupttext |

## Runs (DOCX) und Spans (ODT)

- Word zerlegt Text in Runs mit eigener Formatierung. Ein Run wechselt z.B. bei Rechtschreibmarkierungen,
  Revisionen oder Formatwechseln. So kann `{{MANDANT_NAME}}` intern als `{{MAND` + `ANT_NAME}}` gespeichert sein.
- **DOCX, normaler Absatz:** Der Server findet auch über Runs verteilte Platzhalter. Das Ergebnis
  bekommt die Formatierung des **ersten** Runs.
- **DOCX, Ausnahmen:** In folgenden Fällen versagt die Ersetzung:
  - der Platzhalter ist in einer Textbox auf mehrere Runs verteilt
  - derselbe Platzhalter kommt zweimal verteilt im selben Absatz vor
  - ein Run enthält mehrere Textelemente

  Lösung: `normalize`.
- **ODT:** Über Spans verteilte Platzhalter werden gefunden.
- **Regel:** Ein Platzhalter wird einheitlich formatiert, also z.B. ganz fett oder gar nicht. `replace`
  und `build` legen jeden Platzhalter in genau einen Run.

## Leerzeichen-Automatik

- Nach jedem eingesetzten Wert fügt der Server ein Leerzeichen an. Das gilt nicht, wenn direkt danach
  eines dieser Zeichen folgt: `. , ; : ! ? ' "` Leerzeichen `/ ) ] -`
- `{{MANDANT_PLZ}}{{MANDANT_ORT}}` und `{{MANDANT_PLZ}} {{MANDANT_ORT}}` ergeben beide
  „12345 Berlin“: Folgt bereits ein Leerzeichen, hängt der Server keines an.
- **Doppeltes Leerzeichen** entsteht, wenn ein leerer Platzhalter **zwischen zwei Leerzeichen** steht:
  `{{MANDANT_ANREDE1}} {{MANDANT_AGRAD1}} {{MANDANT_NAME}}` ergibt ohne akademischen Grad
  „Herr  Müller“. Solche optionalen Platzhalter ohne Leerzeichen anhängen:
  `{{MANDANT_ANREDE1}} {{MANDANT_AGRAD1}}{{MANDANT_NAME}}`.
- Folgt direkt ein Buchstabe oder eine Ziffer (`{{MANDANT_NAME}}s`), wird ein Leerzeichen
  eingeschoben („Müller s“). Für solche Fälle ein Skript verwenden:
  `[[SCRIPT:MANDANT_NAME + "s";]]`
- **DOCX:** Das Leerzeichen kommt auch bei leerem Wert und am Absatzende dazu.
- **ODT:** Am Absatzende und bei leerem Wert kommt kein Leerzeichen dazu.

## Leere Absätze

- **ODT:** Ein Absatz, der nach der Ersetzung leer ist, wird entfernt. Das ist praktisch für optionale
  Adresszeilen wie `{{MANDANT_ZUSATZ}}`.
- **DOCX:** Nur ein Absatz, der ausschließlich aus einem Skript mit leerem Ergebnis besteht, wird entfernt.
- **Optionale Zeilen in DOCX:** Für eine Zeile, die verschwinden soll, ein eigenes Skript als einzigen
  Absatzinhalt verwenden: `[[SCRIPT:TEXT(MANDANT_ZUSATZ);]]`

## Beteiligte und Falldatenblätter

- **Beteiligte:**
  - Präfixe kommen aus den Beteiligtentypen der Kanzlei; Standard sind MANDANT, GEGNER, DRITTE.
  - Beim Erstellen wählt die Nutzerin pro verwendetem Präfix einen Beteiligten aus. Eine Vorlage mit
    `MANDANT_…` lässt sich also auch für einen Gegner verwenden.
  - Pro Präfix gibt es genau einen Beteiligten. Für zwei Gegner werden zwei Typen gebraucht
    (z.B. GEGNER und DRITTE).
- **Falldatenblätter:**
  - Der Platzhalter besteht aus Präfix und Feldname des Plugins.
  - Fehlt das Falldatenblatt in der Akte, bleibt der Wert leer.
  - Mit `WENNFALLDATEN("PRÄFIX", …)` lässt sich abfragen, ob das Falldatenblatt vorhanden ist.

## Briefköpfe

- Wird beim Erstellen ein Briefkopf gewählt, dient die Briefkopf-Datei als Basis und der Inhalt der
  Vorlage wird angehängt. Danach werden die Platzhalter ersetzt, auch in Kopf- und Fußzeile des
  Briefkopfs.
- **DOCX:** Nur Absätze und Tabellen des Haupttextes der Vorlage werden übernommen; Kopf-/Fußzeilen,
  Textboxen und Abschnittseinstellungen der Vorlage gehen verloren.
- Vorlagen für die Nutzung mit Briefkopf enthalten deshalb nur den Brieftext (Anschriftenfeld
  und Bezugszeichen nur, wenn der Briefkopf sie nicht schon enthält).
- **Bild-Briefkopf (PNG/JPG):** Er wird als Seitenhintergrund der ersten Seite eingefügt. Die Ränder
  der Vorlage müssen dazu passen.

## Weitere Formate

- **ODS/XLSX:** Platzhalter in Zellen funktionieren, Skripte nicht.
- **PDF-Formulare:** Platzhalter stehen im Formularfeld-Inhalt, z.B. `{{MANDANT_VORNAME}} {{MANDANT_NAME}}`,
  oder der Feldname ist der Platzhalter ohne Klammern. Nur Textfelder werden unterstützt, keine Skripte.
- Diese Formate unterstützt `jlvorlage.py` nicht.

## Direkte XML-Bearbeitung (Workflow C, strukturelle Änderungen)

- **DOCX:** in `word/document.xml`
  - einen bestehenden Absatz `<w:p>` mit seinen `<w:pPr>` kopieren, damit die Formatierung gleich bleibt
  - Text in `<w:t xml:space="preserve">` schreiben
  - Tabellenzeilen `<w:tr>` kopieren
- **ODT:** in `content.xml`
  - `<text:p text:style-name="…">` mit vorhandenem Stil kopieren
  - mehrere Leerzeichen als `<text:s text:c="n"/>`, Zeilenumbruch `<text:line-break/>`
- **Danach immer:** `normalize`, dann `validate`, und die Datei in LibreOffice/Word öffnen lassen
  bzw. mit `soffice --headless --convert-to pdf` prüfen, falls verfügbar.
