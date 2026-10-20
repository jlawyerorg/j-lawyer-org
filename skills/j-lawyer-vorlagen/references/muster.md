# Neue Vorlagen: Beschreibungsformat für `build` und Bausteine

## Format

```
---
schrift: Arial              # Schriftart (Standard Arial)
groesse: 11                 # Schriftgröße in pt
raender: 2.0,2.0,2.0,2.5    # oben,rechts,unten,links in cm (Standard)
absatzabstand: 6            # Abstand nach Absätzen in pt
zeilenabstand: 1.0          # 1.0 = einfach, 1.5 = anderthalbzeilig
kopfzeile: Text | zweite Zeile        # optional, " | " trennt Absätze
fusszeile: {{PROFIL_FIRMA}} · {{PROFIL_STRASSE}} · {{PROFIL_PLZ}}{{PROFIL_ORT}}
---
# Überschrift 1 / ## Überschrift 2 / ### Überschrift 3

Ein Absatz. Aufeinanderfolgende Zeilen werden zu einem Absatz verbunden,
eine Leerzeile beginnt einen neuen Absatz.
Zeile mit Zeilenumbruch (Umschalt+Enter) endet mit Backslash\
nächste Zeile im selben Absatz

**fett**, *kursiv*, __unterstrichen__ (auch kombiniert)

[rechts] rechtsbündiger Absatz      ([links], [zentriert], [block] ebenso)

\leer                 (leerer Absatz, z.B. als Abstand)
\seitenumbruch

\spalten 10,50,20,20  (optional vor einer Tabelle: relative Spaltenbreiten)
| Kopf 1 | Kopf 2 |
|---|---|               (Trennzeile -> erste Zeile wird fett und als Kopfzeile wiederholt)
| Zelle | {{PLATZHALTER}} |
```

- Platzhalter und Skripte werden nie durch Formatierungszeichen zerteilt und stehen immer in einem Run.
- Eine Tabelle mit genau einer Zelle `| {{TABELLE_1}} |` ergibt die 1×1-Tabelle für Tabellenplatzhalter.
- Ergebnis: A4 hochformat, Tabellen mit Gitterlinien.
- Für aufwendige Gestaltung (DIN-5008-Anschriftenfeld mit festen Positionen, Logos): Vorlage
  erzeugen und den Nutzer in Word/LibreOffice nacharbeiten lassen, oder ein vorhandenes Dokument
  der Kanzlei als Basis nehmen (Workflow B/C) – das bewahrt deren Layout.

## Bausteine

### Anschriftenfeld (Empfänger = Mandant)

```
{{MANDANT_UNTERNEHMEN}}\
{{MANDANT_ANREDE2}}{{MANDANT_AGRAD1}}{{MANDANT_VORNAME}}{{MANDANT_NAME}}\
{{MANDANT_ZUSATZ}}\
{{MANDANT_STRASSE}}{{MANDANT_HAUSNR}}\
{{MANDANT_PLZ}}{{MANDANT_ORT}}
```
- `_ANREDE2` liefert „Herrn“ / „Frau“ für die Anschrift.
- Leere Zeilen (z.B. ohne Unternehmen oder Zusatz) bleiben in DOCX als Leerzeile stehen. Wenn
  das stört, für diese Zeilen jeweils einen eigenen Absatz mit `[[SCRIPT:TEXT(MANDANT_ZUSATZ);]]`
  verwenden; ein Absatz, der nur aus einem leeren Skript besteht, wird entfernt.
- Viele Kanzleien pflegen die Hausnummer im Feld Straße. Dann `{{MANDANT_HAUSNR}}` weglassen und
  vorher fragen.

### Absenderzeile (nur ohne Briefkopf)

```
{{PROFIL_FIRMA}} · {{PROFIL_STRASSE}} · {{PROFIL_PLZ}}{{PROFIL_ORT}}
```

### Bezugszeichen / Ort, Datum

```
\spalten 35,35,30
| Ihr Zeichen | Unser Zeichen | Datum |
| {{GEGNER_AKTE_ZEICHEN}} | {{AKTE_ZEICHEN}} ({{DOK_DZ}}) | {{PROFIL_ORT}}, {{KURZDATUM}} |
```
oder als Zeile: `[rechts] {{PROFIL_ORT}}, {{KURZDATUM}}`

### Betreff

```
**{{AKTE_KURZRUBRUM}} – {{AKTE_WEGEN}}**\
Unser Zeichen: {{AKTE_ZEICHEN}}
```

### Briefanrede und Grußformel

```
{{MANDANT_BEGRUESSUNG}} {{MANDANT_NAME}},
…
{{MANDANT_NACHTEXT}}

{{AKTE_ANWALT_AN}}\
Rechtsanwalt
```
- `{{MANDANT_BEGRUESSUNG}}` und `{{MANDANT_NACHTEXT}}` kommen aus den Kontaktdaten.
  `{{MANDANT_BEGRUESSUNG}}` enthält nur die Anrede ohne Namen („Sehr geehrter Herr“); Name und
  Komma ergänzt die Vorlage.
- Ist nicht sicher, dass diese gepflegt sind, die Anrede per `MWDJU` bilden (siehe skripte.md).
- Die Signatur kann auch `{{USER_AN}}` (angemeldete Person) verwenden. `{{AKTE_ANWALT_AN}}` ist
  der verantwortliche Anwalt.

### Fristsetzung

```
Wir fordern Sie auf, bis zum **[[SCRIPT:FRISTBANKTAG(KURZDATUM, "14");]]** …
```

### Rubrum (Schriftsatz)

```
[zentriert] **In dem Rechtsstreit**

{{MANDANT_VORNAME}}{{MANDANT_NAME}}, {{MANDANT_STRASSE}}{{MANDANT_HAUSNR}}, {{MANDANT_PLZ}}{{MANDANT_ORT}},

[rechts] – [[SCRIPT:GENDERN("Kläger", MANDANT_GESCHLECHT, "Nominativ");]] –

Prozessbevollmächtigte: {{PROFIL_FIRMA}}, {{PROFIL_STRASSE}}, {{PROFIL_PLZ}}{{PROFIL_ORT}}

[zentriert] gegen

{{GEGNER_UNTERNEHMEN}}{{GEGNER_VORNAME}}{{GEGNER_NAME}}, {{GEGNER_STRASSE}}{{GEGNER_HAUSNR}}, {{GEGNER_PLZ}}{{GEGNER_ORT}},

[rechts] – [[SCRIPT:GENDERN("Beklagter", GEGNER_GESCHLECHT, "Nominativ");]] –

wegen {{AKTE_WEGEN}}

Streitwert: {{AKTE_GEGENSTANDSWERT}} EUR
```
`GENDERN` liest die Zuordnungstabelle „GENDERN“ der Kanzlei (Begriff, Geschlecht, Fall → Wert).
Ist ein Begriff dort nicht gepflegt, erscheint „kein Zuordnungswert konfiguriert“. Im Zweifel mit
dem Nutzer klären oder `MWDJU` mit festen Texten verwenden.

### Rechnung (flexible Belegtabelle)

```
**{{BEL_TYP}} {{BEL_NR}}** vom {{BEL_DTERSTELLT}}\
Leistungszeitraum: {{BEL_DTLZVON}} – {{BEL_DTLZBIS}}

\spalten 8,52,12,28
| Pos. | Leistung | USt. | Betrag netto |
|---|---|---|---|
| {{BELP_NR}} | {{BELP_NAME}}: {{BELP_BESCHR}} | {{BELP_UST}} | {{BELP_NETTO}}{{BEL_WHRG}} |
| | Summe netto | | {{BEL_SUM_NETTO}}{{BEL_WHRG}} |
| | zzgl. USt. {{BEL_UST_SATZ}} | | {{BEL_UST_BETRAG}}{{BEL_WHRG}} |
| | **Gesamtbetrag** | | **{{BEL_TOTAL}}{{BEL_WHRG}}** |

Zahlbar bis {{BEL_DTFAELLIG}} auf das Konto {{PROFIL_KONTONR}} ({{PROFIL_BANK}}).

| {{BEL_GIROCODE}} |
```
- Einfache Variante statt der flexiblen Belegtabelle: eine 1×1-Tabelle `| {{BEL_TABELLE}} |`.
- Für Rechnungen mit abweichendem Absender gibt es die Platzhalter `{{BEL_ABS…}}`.

### Berechnungsergebnis (RVG-Plugin)

```
| {{TABELLE_1}} |
```

### Leistungsnachweis

```
| {{ZE_TABELLE}} |

| {{ZE_SUMMEN}} |
```
