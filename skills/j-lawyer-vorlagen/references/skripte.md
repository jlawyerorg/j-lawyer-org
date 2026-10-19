# Skripte in Vorlagen: `[[SCRIPT:…]]`

Skripte erzeugen Text abhängig von Daten aus Akte, Beteiligten, Falldatenblättern oder Etiketten.
Der Server wertet sie als **Groovy**-Ausdruck aus; das Ergebnis muss ein Text sein.

```
[[SCRIPT:WENNLEER(MANDANT_AGRAD1, "", MANDANT_AGRAD1 + " ");]]
```

## Syntax-Regeln (aus dem Server-Code)

- **Ein Absatz:** Das Skript beginnt mit `[[SCRIPT:` und endet mit `]]` im **selben Absatz**. Zeilen
  innerhalb eines Skripts nur mit Umschalt+Enter (Zeilenumbruch) trennen, nie mit Enter.
- **Kein `]`:** Ein Skript endet beim ersten `]`. Deshalb darf im Skript kein `]` vorkommen, auch
  nicht in Texten.
- **Platzhalter ohne Klammern:** Platzhalter stehen im Skript **ohne** `{{ }}`, z.B. `MANDANT_NAME`.
  - Vor der Auswertung ersetzt der Server jeden Namen durch `"Wert"`, also durch den Wert in
    Anführungszeichen. Dabei wird auch innerhalb von Texten ersetzt.
  - Deshalb darf ein Platzhaltername nie in einem Text in Anführungszeichen stehen. Er wird angehängt:
    `"Sehr geehrte Frau " + MANDANT_NAME + ","`.
- **Mehrzeilige Werte:** Werte aus mehrzeiligen Feldern (z.B. Notizen, mehrzeilige
  Falldatenblatt-Felder) in doppelte Anführungszeichen-Paare setzen: `""MANDANT_NOTIZ""`. Daraus
  wird ein Groovy-Text mit dreifachen Anführungszeichen.
- **Anführungszeichen:** Texte stehen in geraden Anführungszeichen `"…"`. Typografische „…“
  wandelt der Server um.
- **Verketten:** Texte werden mit `+` verkettet. `\n` erzeugt einen Zeilenumbruch, `\t` einen Tabulator.
- **Abschluss:** Ein abschließendes `;` ist üblich (`…;]]`), aber nicht nötig.
- **Leeres Ergebnis:** Liefert ein Skript, das allein in seinem Absatz steht, einen leeren Text, wird
  der ganze Absatz entfernt. So lassen sich optionale Absätze bauen.
- **Fehler:** Bei einem Fehler erscheint im Dokument `SCRIPTFEHLER: …`. Typische Ursachen:
  - unbekannter Platzhaltername (er wird nicht ersetzt und ist dann ein ungültiger Groovy-Name)
  - Platzhaltername innerhalb eines Textes
  - Klammern oder Anführungszeichen nicht geschlossen
  - falsche Anzahl Parameter
- **Was `validate` prüft:** alle oben genannten Fehlerursachen und dass das Skript in einem Absatz
  abgeschlossen ist.

## Funktionen

Alle Parameter und Rückgabewerte sind Texte. Vergleiche ignorieren Groß-/Kleinschreibung, wenn
nicht anders angegeben.

| Funktion | Ergebnis |
|---|---|
| `WENNGLEICH(a, b, dann[, sonst])` | `dann`, wenn a = b (ohne Beachtung der Groß-/Kleinschreibung), sonst `sonst` bzw. leer |
| `WENNENTHAELT(text, suche, dann[, sonst])` | `dann`, wenn `suche` in `text` enthalten ist |
| `WENNLEER(wert, dannLeer[, sonstNichtLeer])` | `dannLeer`, wenn `wert` leer ist |
| `WENNGROESSER(wert, limit, dann[, sonst])` | `dann`, wenn Zahl `wert` > `limit` (deutsches Zahlformat: Punkt = Tausender, Komma = Dezimal) |
| `WENNFALLDATEN("PRÄFIX", dann[, sonst])` | `dann`, wenn die Akte ein Falldatenblatt mit diesem Präfix hat; das Präfix steht **in Anführungszeichen** |
| `WENNETIKETT("Etikett", dann[, sonst])` | `dann`, wenn die Akte das Etikett trägt |
| `WENNLETIKETT("Listenetikett", "Wert", dann[, sonst])` | `dann`, wenn das Listenetikett der Akte den Wert hat |
| `LETIKETTWERT("Listenetikett")` | Wert des Listenetiketts der Akte (leer, wenn nicht gesetzt) |
| `MWDJU(geschlecht, m, w, d, j, u)` | Text je nach Geschlecht: männlich / weiblich / divers / juristische Person / undefiniert. Fünf Varianten sind Pflicht. Bei anderen Werten erscheint „UNBEKANNTER WERT FUER GESCHLECHT“ |
| `GENDERN("Begriff", geschlecht, "Fall")` | geschlechts- und fallgerechte Form, z.B. `GENDERN("Kläger", MANDANT_GESCHLECHT, "Nominativ")`. Fall: Nominativ, Genitiv, Dativ, Akkusativ. Liest die Zuordnungstabelle „GENDERN“ der Kanzlei; ist der Begriff dort nicht gepflegt, erscheint „kein Zuordnungswert konfiguriert“ |
| `ZUORDNEN("Tabelle", schlüssel1[, schlüssel2[, schlüssel3]])` | Wert aus einer Zuordnungstabelle (Einstellungen → Dokumente → Zuordnungstabellen) |
| `FRIST(datum, "tage"[, "format"])` | Datum (TT.MM.JJJJ) plus Tage; ohne Format im Ergebnis TT.MM.JJJJ |
| `FRISTBANKTAG(datum, "tage"[, "format"])` | wie FRIST, fällt das Ergebnis auf Sa/So, wird es auf den Montag verschoben (Feiertage werden nicht berücksichtigt) |
| `DATUMZEIT("format")` | aktuelles Datum/Uhrzeit, Format nach Java `SimpleDateFormat`, z.B. `"EEEE, dd. MMMM yyyy"` |
| `GROSS(text)` | erster Buchstabe groß (z.B. für den Satzanfang) – **nicht** der ganze Text |
| `KLEIN(text)` | erster Buchstabe klein |
| `TEXT(text)` | gibt den Text unverändert zurück (leer statt null) |

Einige Hinweise zu den Funktionen:
- `FRIST`/`FRISTBANKTAG` liefern `??.??.????`, wenn das Datum leer oder ungültig ist.
- Zeichen für `DATUMZEIT` und das Format bei `FRIST`/`FRISTBANKTAG`:

  | Zeichen | Bedeutung |
  |---|---|
  | `dd` | Tag |
  | `MM` | Monat als Zahl |
  | `MMMM` | Monatsname |
  | `yyyy` | Jahr |
  | `EEE` / `EEEE` | Wochentag kurz / lang |
  | `HH:mm` | Uhrzeit |

### Bedingungen kombinieren (Groovy)

```
[[SCRIPT:(MANDANT_ORT.equals("Hamburg") && AKTE_SACHGEBIET.equals("Arbeitsrecht")) ? "Moin" : "Guten Tag";]]
[[SCRIPT:(MANDANT_NAME.equals("Müller") || MANDANT_NAME.equals("Meier")) ? "bekannt" : "neu";]]
```

### Funktionen schachteln

```
[[SCRIPT:WENNGLEICH(KSCHUTZ_BTRAT, "Betriebsrat vorhanden", MWDJU(GEGNER_GESCHLECHT, "Beim Beklagten ", "Bei der Beklagten ", "Bei der beklagten Partei ", "Bei der Beklagten ", "Bei der beklagten Partei ") + "ist ein Betriebsrat gebildet.", "Es ist kein Betriebsrat gebildet.");]]
```

## Bewährte Muster

Briefanrede abhängig vom Geschlecht (wenn `{{MANDANT_BEGRUESSUNG}}` in den Kontakten nicht gepflegt ist):
```
[[SCRIPT:MWDJU(MANDANT_GESCHLECHT, "Sehr geehrter Herr " + MANDANT_NAME + ",", "Sehr geehrte Frau " + MANDANT_NAME + ",", "Guten Tag " + MANDANT_VORNAME + " " + MANDANT_NAME + ",", "Sehr geehrte Damen und Herren,", "Sehr geehrte Damen und Herren,");]]
```

Optionaler Absatz (verschwindet, wenn leer):
```
[[SCRIPT:WENNLEER(MANDANT_RECHTSSCHUTZ, "", "Ihre Rechtsschutzversicherung (Schein-Nr. " + MANDANT_RECHTSSCHUTZ + ") haben wir informiert.");]]
```

Frist 14 Tage, auf Werktag verschoben, ausgeschrieben:
```
bis zum [[SCRIPT:FRISTBANKTAG(KURZDATUM, "14", "EEEE, dd. MMMM yyyy");]]
```

Parteibezeichnung gegendert:
```
[[SCRIPT:GROSS(GENDERN("Kläger", MANDANT_GESCHLECHT, "Nominativ"));]] beantragt …
```
