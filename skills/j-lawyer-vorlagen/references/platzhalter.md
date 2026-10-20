# Platzhalter in j-lawyer-Dokumentvorlagen

Platzhalter haben die Form `{{NAME}}`: GROSSBUCHSTABEN, Ziffern und Unterstriche, ohne Leerzeichen
innerhalb der Klammern. Beim Erstellen eines Dokuments aus einer Akte ersetzt der Server sie durch
die Werte aus Akte, Beteiligten, Kanzleiprofil, Benutzer, Beleg oder Falldatenblatt.

Für eine gezielte Suche ist `python3 scripts/jlvorlage.py placeholders --suche <text>` sparsamer als
diese Datei vollständig zu lesen.

## Beteiligte

- Die Platzhalter für Beteiligte haben die Form `{{<PRÄFIX>_<FELD>}}`, z.B. `{{MANDANT_NAME}}`,
  `{{GEGNER_STRASSE}}`, `{{DRITTE_PLZ}}`.
- Standard-Beteiligtentypen: **MANDANT**, **GEGNER**, **DRITTE**. Kanzleien legen oft weitere an
  (z.B. `VERSICHERUNG`, `GERICHT`, `GEGNERANWALT`); den genauen Präfix muss die Kanzlei nennen
  (Einstellungen → Beteiligtentypen).
- Eine Vorlage mit `MANDANT_…` kann beim Erstellen für jeden Beteiligten genutzt werden – man muss
  nicht pro Beteiligtentyp eine Vorlage bauen. Pro Typ wird genau ein Beteiligter eingesetzt.
- Anrede-Felder (häufig verwechselt):
  - `_ANREDE1` = Anrede („Herr", „Frau")
  - `_ANREDE2` = Anrede in der Anschrift („Herrn")
  - `_BEGRUESSUNG` = Briefanrede aus den Kontaktdaten **ohne Namen** („Sehr geehrter Herr",
    „Sehr geehrte Frau Kollegin"). Briefanrede daher immer als
    `{{MANDANT_BEGRUESSUNG}} {{MANDANT_NAME}},` schreiben.
  - `_NACHTEXT` = Grußformel („Mit freundlichen Grüßen")
  - Die Aliasse `_ANREDE` (= `_BEGRUESSUNG`), `_TITEL` (= `_ANREDE1`) und `_FIRMA` (= `_UNTERNEHMEN`)
    funktionieren noch, werden aber nicht mehr verwendet.
- `_GESCHLECHT` liefert „männlich", „weiblich", „divers", „juristische Person" oder „undefiniert"
  und ist der typische Parameter für `MWDJU` und `GENDERN` (siehe skripte.md).

## Falldatenblätter

- Platzhalter aus Falldatenblättern haben die Form `{{<PRÄFIX>_<FELD>}}`, z.B. `{{KSCHUTZ_ZUGANGK}}`.
- Präfix und Feldnamen hängen vom Falldatenblatt-Plugin ab und stehen **nicht** in diesem Katalog. Die
  Kanzlei sieht sie in j-lawyer in der Akte unter „Falldaten" bzw. beim Einfügen von Platzhaltern;
  mit dem j-lawyer-Connector liefert `list_form_types` die Präfixe.
- Beim Prüfen mit `validate --formulare PRÄFIX1,PRÄFIX2` angeben, sonst gelten sie als unbekannt.

## Tabellenplatzhalter

Diese Platzhalter werden durch ganze Tabellen bzw. Grafiken ersetzt und müssen **allein in einer
Tabellenzelle** stehen:

| Platzhalter | Tabelle in der Vorlage |
|---|---|
| `{{TABELLE_1}}` | 1×1-Tabelle (Ergebnis aus Berechnungs-Plugins, z.B. RVG) |
| `{{BEL_GIROCODE}}` | 1×1-Tabelle (GiroCode-Grafik) |
| `{{BEL_TABELLE}}` | 1×1-Tabelle oder Tabelle mit exakt 6 Spalten (Spaltenbreiten bleiben erhalten; zusätzliche Kopfzeile mit Beschriftung erlaubt) |
| `{{ZE_TABELLE}}` | 1×1-Tabelle oder exakt 6 Spalten (Person, Datum, Dauer, Stundensatz, Total, Beschreibung) |
| `{{ZE_SUMMEN}}` | 1×1-Tabelle oder exakt 3 Spalten (Person, Dauer, Total) |

In DOCX werden sie nur in Tabellen des Haupttextes gefunden – nicht in Kopf-/Fußzeilen und nicht in
verschachtelten Tabellen.

### Flexible Belegtabelle

- Eine Tabellenzeile mit `{{BELP_…}}`-Platzhaltern wird pro Belegposition dupliziert. Spalten,
  Reihenfolge und Formatierung legt die Vorlage fest; die Kopfzeile ist frei gestaltbar.
- Eine Zeile mit `{{BEL_UST_SATZ}}`/`{{BEL_UST_BETRAG}}` wird pro Steuersatz dupliziert.
- `{{BEL_SUM_NETTO}}`, `{{BEL_TOTAL}}`, `{{BEL_WHRG}}` können in festen Zeilen (Summenzeilen) stehen.
- Mehrere Platzhalter in einer Zelle sind erlaubt, z.B. `{{BELP_NAME}}: {{BELP_BESCHR}}`.

Beispiel (4 Spalten):

| Pos. | Leistung | USt. | Netto |
|---|---|---|---|
| `{{BELP_NR}}` | `{{BELP_NAME}}: {{BELP_BESCHR}}` | `{{BELP_UST}}` | `{{BELP_NETTO}}` `{{BEL_WHRG}}` |
| | Summe netto | | `{{BEL_SUM_NETTO}}` `{{BEL_WHRG}}` |
| | USt. `{{BEL_UST_SATZ}}` | | `{{BEL_UST_BETRAG}}` `{{BEL_WHRG}}` |
| | **Gesamt** | | **`{{BEL_TOTAL}}` `{{BEL_WHRG}}`** |

## Vollständiger Katalog

Der folgende Abschnitt wird aus dem Quellcode generiert (`PlaceHolders.java`) und ist maßgeblich,
auch wo er vom Benutzerhandbuch abweicht.

<!-- BEGIN GENERATED: generate_catalog.py -->

### Kanzlei

| Platzhalter | Bedeutung |
|---|---|
| `{{PROFIL_FIRMA}}` | Name der Kanzlei |
| `{{PROFIL_STRASSE}}` | Straße |
| `{{PROFIL_STRASSE2}}` | Adresszusatz |
| `{{PROFIL_PLZ}}` | PLZ |
| `{{PROFIL_ORT}}` | Ort |
| `{{PROFIL_LAND}}` | Land |
| `{{PROFIL_TEL}}` | Telefon |
| `{{PROFIL_FAX}}` | Fax |
| `{{PROFIL_MOBIL}}` | Mobil |
| `{{PROFIL_EMAIL}}` | E-Mail |
| `{{PROFIL_WWW}}` | Homepage |
| `{{PROFIL_STNR}}` | Steuernummer |
| `{{PROFIL_USTIDNR}}` | USt-IdNr. |
| `{{PROFIL_BANK}}` | Bank |
| `{{PROFIL_KONTONR}}` | IBAN |
| `{{PROFIL_BLZ}}` | BIC |
| `{{PROFIL_BANK_AK}}` | Anderkonto: Bank |
| `{{PROFIL_KONTONR_AK}}` | Anderkonto: IBAN |
| `{{PROFIL_BLZ_AK}}` | Anderkonto: BIC |

### Datum und Dokument

| Platzhalter | Bedeutung |
|---|---|
| `{{KURZDATUM}}` | heutiges Datum (TT.MM.JJJJ) |
| `{{LANGDATUM}}` | heutiges Datum mit Wochentag |
| `{{DOK_DZ}}` | Diktatzeichen |
| `{{TABELLE_1}}` | Berechnungstabelle |

### Akte

| Platzhalter | Bedeutung |
|---|---|
| `{{AKTE_NR}}` | Aktennummer |
| `{{AKTE_ZEICHEN}}` | Aktenzeichen |
| `{{AKTE_KURZRUBRUM}}` | Kurzrubrum |
| `{{AKTE_NOTIZ}}` | Notiz |
| `{{AKTE_SCHADENNR}}` | Schadennummer |
| `{{AKTE_GEGENSTANDSWERT}}` | Gegenstandswert |
| `{{AKTE_WEGEN}}` | wegen |
| `{{AKTE_SACHGEBIET}}` | Sachgebiet |
| `{{AKTE_ERSTELLT}}` | angelegt am |
| `{{AKTE_ANWALT_AN}}` | Anwalt (Anzeigename) |
| `{{AKTE_SACHBEARBEITER_AN}}` | Sachbearbeiter (Anzeigename) |
| `{{AKTE_SACHBEARBEITER}}` | Sachbearbeiter (Benutzername) |
| `{{AKTE_ANWALT}}` | Anwalt (Benutzername) |
| `{{AKTE_SACHBEARBEITER_KRZ}}` | Sachbearbeiter (Kürzel) |
| `{{AKTE_ANWALT_KRZ}}` | Anwalt (Kürzel) |
| `{{AKTE_EIGENE1}}` | Eigenes Feld 1 |
| `{{AKTE_EIGENE2}}` | Eigenes Feld 2 |
| `{{AKTE_EIGENE3}}` | Eigenes Feld 3 |

### Benutzer

| Platzhalter | Bedeutung |
|---|---|
| `{{USER_AN}}` | Anzeigename |
| `{{USER_EMAIL}}` | E-Mail |
| `{{USER_KRZ}}` | Kürzel |
| `{{USER_VORNAME}}` | Vorname |
| `{{USER_NAME}}` | Name |
| `{{USER_UNTERNEHMEN}}` | Unternehmen |
| `{{USER_FKT}}` | Funktion |
| `{{USER_STRASSE}}` | Straße |
| `{{USER_ZUSATZ}}` | Adresszusatz |
| `{{USER_PLZ}}` | PLZ |
| `{{USER_ORT}}` | Ort |
| `{{USER_LAND}}` | Land |
| `{{USER_TEL}}` | Telefon |
| `{{USER_FAX}}` | Fax |
| `{{USER_MOBIL}}` | Mobil |
| `{{USER_WWW}}` | Homepage |
| `{{USER_BANK}}` | Bank |
| `{{USER_BIC}}` | BIC |
| `{{USER_IBAN}}` | IBAN |
| `{{USER_STEUERNR}}` | Steuernummer |
| `{{USER_USTIDNR}}` | USt-IdNr. |
| `{{USER_LOGIN}}` | Benutzername |

### Rechnung

| Platzhalter | Bedeutung |
|---|---|
| `{{BEL_NR}}` | Belegnummer |
| `{{BEL_TYP}}` | Belegart |
| `{{BEL_NAME}}` | Bezeichnung |
| `{{BEL_BESCHR}}` | Beschreibung |
| `{{BEL_DTFAELLIG}}` | fällig am |
| `{{BEL_DTLZVON}}` | Leistungszeitraum von |
| `{{BEL_DTLZBIS}}` | Leistungszeitraum bis |
| `{{BEL_DTERSTELLT}}` | Belegdatum |
| `{{BEL_TOTAL}}` | Gesamtbetrag (brutto) |
| `{{BEL_WHRG}}` | Währung |
| `{{BEL_TABELLE}}` | Positionstabelle |
| `{{BEL_GIROCODE}}` | GiroCode |
| `{{BEL_ABSAN}}` | Absender: Anzeigename |
| `{{BEL_ABSEMAIL}}` | Absender: E-Mail |
| `{{BEL_ABSKRZ}}` | Absender: Kürzel |
| `{{BEL_ABSVORNAME}}` | Absender: Vorname |
| `{{BEL_ABSNAME}}` | Absender: Name |
| `{{BEL_ABSUNTERNEHMEN}}` | Absender: Unternehmen |
| `{{BEL_ABSFKT}}` | Absender: Funktion |
| `{{BEL_ABSSTRASSE}}` | Absender: Straße |
| `{{BEL_ABSZUSATZ}}` | Absender: Adresszusatz |
| `{{BEL_ABSPLZ}}` | Absender: PLZ |
| `{{BEL_ABSORT}}` | Absender: Ort |
| `{{BEL_ABSLAND}}` | Absender: Land |
| `{{BEL_ABSTEL}}` | Absender: Telefon |
| `{{BEL_ABSFAX}}` | Absender: Fax |
| `{{BEL_ABSMOBIL}}` | Absender: Mobil |
| `{{BEL_ABSWWW}}` | Absender: Homepage |
| `{{BEL_ABSBANK}}` | Absender: Bank |
| `{{BEL_ABSBIC}}` | Absender: BIC |
| `{{BEL_ABSIBAN}}` | Absender: IBAN |
| `{{BEL_ABSSTEUERNR}}` | Absender: Steuernummer |
| `{{BEL_ABSUSTIDNR}}` | Absender: USt-IdNr. |
| `{{BEL_SUM_NETTO}}` | Summe netto |
| `{{BEL_UST_SATZ}}` | USt-Satz |
| `{{BEL_UST_BETRAG}}` | USt-Betrag |

### Rechnungsposition

| Platzhalter | Bedeutung |
|---|---|
| `{{BELP_NR}}` | Positionsnummer |
| `{{BELP_NAME}}` | Bezeichnung |
| `{{BELP_BESCHR}}` | Beschreibung |
| `{{BELP_MENGE}}` | Menge |
| `{{BELP_EINZEL}}` | Einzelpreis |
| `{{BELP_UST}}` | USt-Satz |
| `{{BELP_NETTO}}` | Gesamt netto |

### Zeiterfassung

| Platzhalter | Bedeutung |
|---|---|
| `{{ZE_TABELLE}}` | Tabelle der Zeiteinträge |
| `{{ZE_SUMMEN}}` | Summentabelle |

### Ingo

| Platzhalter | Bedeutung |
|---|---|
| `{{INGO_TEXT}}` | von Ingo erzeugter Text |

### Beteiligte (Präfix `###` = Platzhalter des Beteiligtentyps, z.B. MANDANT, GEGNER, DRITTE)

| Platzhalter | Bedeutung |
|---|---|
| `{{###_NAME}}` | Name |
| `{{###_VORNAME}}` | Vorname |
| `{{###_VORNAME2}}` | weitere Vornamen |
| `{{###_UNTERNEHMEN}}` | Unternehmen |
| `{{###_FIRMA}}` | Unternehmen – Alias von `_UNTERNEHMEN`, nicht verwenden |
| `{{###_ABTLG}}` | Abteilung |
| `{{###_ANREDE1}}` | Anrede |
| `{{###_TITEL}}` | Anrede – Alias von `_ANREDE1`, nicht verwenden |
| `{{###_BEGRUESSUNG}}` | Begrüßung |
| `{{###_ANREDE}}` | Begrüßung – Alias von `_BEGRUESSUNG`, nicht verwenden |
| `{{###_NACHTEXT}}` | Nachtext / Grußformel |
| `{{###_STRASSE}}` | Straße |
| `{{###_ORT}}` | Ort |
| `{{###_PLZ}}` | PLZ |
| `{{###_LAND}}` | Land |
| `{{###_BLAND}}` | Bundesland |
| `{{###_TEL}}` | Telefon |
| `{{###_MOBIL}}` | Mobil |
| `{{###_FAX}}` | Fax |
| `{{###_EMAIL}}` | E-Mail |
| `{{###_WWW}}` | Homepage |
| `{{###_BANK}}` | Bank |
| `{{###_BLZ}}` | BIC |
| `{{###_KONTONR}}` | IBAN |
| `{{###_SEPAREF}}` | SEPA-Mandatsreferenz |
| `{{###_SEPASEIT}}` | SEPA-Mandat vom |
| `{{###_RECHTSSCHUTZ}}` | Rechtsschutz: Versicherungsschein |
| `{{###_VRECHTSSCHUTZ}}` | Verkehrsrechtsschutz: Versicherungsschein |
| `{{###_KFZVERS}}` | Kfz-Versicherung: Versicherungsschein |
| `{{###_EIGENE1}}` | Eigenes Feld 1 |
| `{{###_EIGENE2}}` | Eigenes Feld 2 |
| `{{###_EIGENE3}}` | Eigenes Feld 3 |
| `{{###_GEB}}` | Geburtsdatum |
| `{{###_AKTE_ZEICHEN}}` | Zeichen des Beteiligten |
| `{{###_AKTE_KONTAKT}}` | Kontakt / Ansprechpartner |
| `{{###_AKTE_EIGENE1}}` | Beteiligung: Eigenes Feld 1 |
| `{{###_AKTE_EIGENE2}}` | Beteiligung: Eigenes Feld 2 |
| `{{###_AKTE_EIGENE3}}` | Beteiligung: Eigenes Feld 3 |
| `{{###_ORTSTEIL}}` | Ortsteil |
| `{{###_NOTIZ}}` | Notiz |
| `{{###_STA}}` | Staatsangehörigkeit |
| `{{###_GEBNAME}}` | Geburtsname |
| `{{###_GEBORT}}` | Geburtsort |
| `{{###_GEST}}` | Sterbedatum |
| `{{###_USTIDNR}}` | USt-ID |
| `{{###_STEUERNR}}` | Steuernummer |
| `{{###_RFORM}}` | Rechtsform |
| `{{###_REGNR}}` | Registernummer |
| `{{###_REGGERICHT}}` | Registergericht |
| `{{###_GESCHLECHT}}` | Geschlecht |
| `{{###_HAUSNR}}` | Hausnummer |
| `{{###_INITIAL}}` | Initialen |
| `{{###_AGRAD1}}` | akad. Grad vor dem Namen |
| `{{###_AGRAD2}}` | akad. Grad nach dem Namen |
| `{{###_BERUF}}` | Beruf |
| `{{###_FKT}}` | Funktion |
| `{{###_ZUSATZ}}` | Adresszusatz |
| `{{###_ANREDE2}}` | Anrede Briefkopf |
| `{{###_ALTER}}` | Alter |

### Nicht in Dokumentvorlagen nutzbar

- `{{CURSOR}}` – Cursorposition nach dem Einfügen (nur E-Mail-/beA-Textbausteine)
- `{{CLOUD_LINK}}` – Cloud-Link (nur E-Mail) (nur E-Mail-/beA-Textbausteine)

- `{{AUTOR_AN}}` – im Handbuch genannt, wird vom Server aber nicht ersetzt
- `{{AUTOR_KRZ}}` – im Handbuch genannt, wird vom Server aber nicht ersetzt
- `{{AUTOR_EMAIL}}` – im Handbuch genannt, wird vom Server aber nicht ersetzt

### Skriptfunktionen (Signaturen)

- `DATUMZEIT(format)` – Parameteranzahl: 1
- `FRIST(fromDate, addDays, format)` – Parameteranzahl: 2, 3
- `FRISTBANKTAG(fromDate, addDays, format)` – Parameteranzahl: 2, 3
- `GENDERN(word, gender, inCase)` – Parameteranzahl: 3
- `GROSS(s1)` – Parameteranzahl: 1
- `KLEIN(s1)` – Parameteranzahl: 1
- `LETIKETTWERT(etikett)` – Parameteranzahl: 1
- `MWDJU(gender, male, female, other, org, undef)` – Parameteranzahl: 6
- `TEXT(s1)` – Parameteranzahl: 1
- `WENNENTHAELT(s1, s2, then, otherwise)` – Parameteranzahl: 3, 4
- `WENNETIKETT(etikett, then, otherwise)` – Parameteranzahl: 2, 3
- `WENNFALLDATEN(s1, then, otherwise)` – Parameteranzahl: 2, 3
- `WENNGLEICH(s1, s2, then, otherwise)` – Parameteranzahl: 3, 4
- `WENNGROESSER(s1, s2, then, otherwise)` – Parameteranzahl: 3, 4
- `WENNLEER(s1, then, otherwise)` – Parameteranzahl: 2, 3
- `WENNLETIKETT(etikett, wert, then, otherwise)` – Parameteranzahl: 3, 4
- `ZUORDNEN(table, key1, key2, key3)` – Parameteranzahl: 2, 3, 4

<!-- END GENERATED -->
