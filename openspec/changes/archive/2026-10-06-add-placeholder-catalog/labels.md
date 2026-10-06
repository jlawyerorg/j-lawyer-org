# Bezeichnungen und Kategorien der Platzhalter

Fachlich abgestimmt (2026-10-07). Jede Zeile wird bei der Umsetzung zu einer Annotation
`@PlaceHolderInfo(category = ..., label = "...")` an der Konstante in `PlaceHolders.java`.
Die Bezeichnungen orientieren sich an den Feldbeschriftungen der Oberfläche (Kontakt, Akte,
Profil). Angezeigt wird im Baum die Bezeichnung, in Suche und Tooltip
"Kategorie bzw. Unterkategorie: Bezeichnung".

Legende Spalte "E-Mail": ✓ = im E-Mail-Kontext angeboten, – = ausgeblendet.

## Akte (`AKTE`)

| Konstante | Platzhalter | Bezeichnung | E-Mail | Anmerkung |
|---|---|---|---|---|
| AKTE_ZEICHEN | `{{AKTE_ZEICHEN}}` | Aktenzeichen | ✓ | |
| AKTE_NR | `{{AKTE_NR}}` | Aktennummer | ✓ | wird wie AKTE_ZEICHEN befüllt, bleibt aber eigenständig angeboten |
| AKTE_KURZRUBRUM | `{{AKTE_KURZRUBRUM}}` | Kurzrubrum | ✓ | |
| AKTE_WEGEN | `{{AKTE_WEGEN}}` | wegen | ✓ | |
| AKTE_SACHGEBIET | `{{AKTE_SACHGEBIET}}` | Sachgebiet | ✓ | |
| AKTE_GEGENSTANDSWERT | `{{AKTE_GEGENSTANDSWERT}}` | Gegenstandswert | ✓ | |
| AKTE_SCHADENNR | `{{AKTE_SCHADENNR}}` | Schadennummer | ✓ | |
| AKTE_NOTIZ | `{{AKTE_NOTIZ}}` | Notiz | ✓ | |
| AKTE_ERSTELLT | `{{AKTE_ERSTELLT}}` | angelegt am | ✓ | |
| AKTE_EIGENE1 | `{{AKTE_EIGENE1}}` | Eigenes Feld 1 | ✓ | konfigurierte Feldnamen anzuzeigen ist eine spätere Erweiterung |
| AKTE_EIGENE2 | `{{AKTE_EIGENE2}}` | Eigenes Feld 2 | ✓ | |
| AKTE_EIGENE3 | `{{AKTE_EIGENE3}}` | Eigenes Feld 3 | ✓ | |
| AKTE_ANWALT_AN | `{{AKTE_ANWALT_AN}}` | Anwalt (Anzeigename) | ✓ | |
| AKTE_ANWALT | `{{AKTE_ANWALT}}` | Anwalt (Benutzername) | ✓ | |
| AKTE_ANWALT_KRZ | `{{AKTE_ANWALT_KRZ}}` | Anwalt (Kürzel) | ✓ | |
| AKTE_SACHBEARBEITER_AN | `{{AKTE_SACHBEARBEITER_AN}}` | Sachbearbeiter (Anzeigename) | ✓ | |
| AKTE_SACHBEARBEITER | `{{AKTE_SACHBEARBEITER}}` | Sachbearbeiter (Benutzername) | ✓ | |
| AKTE_SACHBEARBEITER_KRZ | `{{AKTE_SACHBEARBEITER_KRZ}}` | Sachbearbeiter (Kürzel) | ✓ | |

## Beteiligte (`BETEILIGTE`, Unterkategorie = Name des Beteiligtentyps)

Generische Konstanten; `###` wird je Beteiligtentyp ersetzt, z.B. `{{MANDANT_VORNAME}}` →
"Mandant: Vorname".

### Person

| Konstante | Platzhalter | Bezeichnung | E-Mail | Anmerkung |
|---|---|---|---|---|
| _ANREDE1 | `{{###_ANREDE1}}` | Anrede | ✓ | Feld "Anrede" (z.B. "Herr") |
| _TITEL_ALIAS | `{{###_TITEL}}` | Anrede | – | Alias von _ANREDE1 |
| _BEGRUESSUNG | `{{###_BEGRUESSUNG}}` | Begrüßung | ✓ | Feld "Begrüßung" (z.B. "Sehr geehrter Herr Müller,") |
| _ANREDE_ALIAS | `{{###_ANREDE}}` | Begrüßung | – | Alias von _BEGRUESSUNG |
| _NACHTEXT | `{{###_NACHTEXT}}` | Nachtext / Grußformel | ✓ | |
| _ANREDE2 | `{{###_ANREDE2}}` | Anrede Briefkopf | ✓ | |
| _AGRAD1 | `{{###_AGRAD1}}` | akad. Grad vor dem Namen | ✓ | |
| _AGRAD2 | `{{###_AGRAD2}}` | akad. Grad nach dem Namen | ✓ | |
| _VORNAME | `{{###_VORNAME}}` | Vorname | ✓ | |
| _VORNAME2 | `{{###_VORNAME2}}` | weitere Vornamen | ✓ |  |
| _NAME | `{{###_NAME}}` | Name | ✓ | |
| _INITIAL | `{{###_INITIAL}}` | Initialen | ✓ | |
| _GEBNAME | `{{###_GEBNAME}}` | Geburtsname | ✓ | |
| _GEB | `{{###_GEB}}` | Geburtsdatum | ✓ | |
| _GEBORT | `{{###_GEBORT}}` | Geburtsort | ✓ | |
| _GEST | `{{###_GEST}}` | Sterbedatum | ✓ | |
| _ALTER | `{{###_ALTER}}` | Alter | ✓ | |
| _GESCHLECHT | `{{###_GESCHLECHT}}` | Geschlecht | ✓ | |
| _STA | `{{###_STA}}` | Staatsangehörigkeit | ✓ | |
| _BERUF | `{{###_BERUF}}` | Beruf | ✓ | |
| _FKT | `{{###_FKT}}` | Funktion | ✓ | |

### Unternehmen

| Konstante | Platzhalter | Bezeichnung | E-Mail | Anmerkung |
|---|---|---|---|---|
| _UNTERNEHMEN | `{{###_UNTERNEHMEN}}` | Unternehmen | ✓ | |
| _FIRMA_ALIAS | `{{###_FIRMA}}` | Unternehmen | – | Alias von _UNTERNEHMEN |
| _ABTLG | `{{###_ABTLG}}` | Abteilung | ✓ | |
| _RFORM | `{{###_RFORM}}` | Rechtsform | ✓ | |
| _REGNR | `{{###_REGNR}}` | Registernummer | ✓ | |
| _REGGERICHT | `{{###_REGGERICHT}}` | Registergericht | ✓ | |
| _USTIDNR | `{{###_USTIDNR}}` | USt-ID | ✓ | |
| _STEUERNR | `{{###_STEUERNR}}` | Steuernummer | ✓ | |

### Adresse und Kommunikation

| Konstante | Platzhalter | Bezeichnung | E-Mail | Anmerkung |
|---|---|---|---|---|
| _STRASSE | `{{###_STRASSE}}` | Straße | ✓ | |
| _HAUSNR | `{{###_HAUSNR}}` | Hausnummer | ✓ | |
| _ZUSATZ | `{{###_ZUSATZ}}` | Adresszusatz | ✓ | |
| _PLZ | `{{###_PLZ}}` | PLZ | ✓ | |
| _ORT | `{{###_ORT}}` | Ort | ✓ | |
| _ORTSTEIL | `{{###_ORTSTEIL}}` | Ortsteil | ✓ | |
| _BLAND | `{{###_BLAND}}` | Bundesland | ✓ | |
| _LAND | `{{###_LAND}}` | Land | ✓ | |
| _TEL | `{{###_TEL}}` | Telefon | ✓ | |
| _MOBIL | `{{###_MOBIL}}` | Mobil | ✓ | |
| _FAX | `{{###_FAX}}` | Fax | ✓ | |
| _EMAIL | `{{###_EMAIL}}` | E-Mail | ✓ | erste vorhandene Adresse (`getAnyEmail()`) |
| _WWW | `{{###_WWW}}` | Homepage | ✓ | |

### Bank und Versicherung

| Konstante | Platzhalter | Bezeichnung | E-Mail | Anmerkung |
|---|---|---|---|---|
| _BANK | `{{###_BANK}}` | Bank | ✓ | |
| _BLZ | `{{###_BLZ}}` | BIC | ✓ | Feld heißt in der Oberfläche "BIC" |
| _KONTONR | `{{###_KONTONR}}` | IBAN | ✓ | Feld heißt in der Oberfläche "IBAN" |
| _SEPAREF | `{{###_SEPAREF}}` | SEPA-Mandatsreferenz | ✓ | |
| _SEPASEIT | `{{###_SEPASEIT}}` | SEPA-Mandat vom | ✓ | |
| _RECHTSSCHUTZ | `{{###_RECHTSSCHUTZ}}` | Rechtsschutz: Versicherungsschein | ✓ | |
| _VRECHTSSCHUTZ | `{{###_VRECHTSSCHUTZ}}` | Verkehrsrechtsschutz: Versicherungsschein | ✓ | |
| _KFZVERS | `{{###_KFZVERS}}` | Kfz-Versicherung: Versicherungsschein | ✓ | |

### Sonstiges

| Konstante | Platzhalter | Bezeichnung | E-Mail | Anmerkung |
|---|---|---|---|---|
| _NOTIZ | `{{###_NOTIZ}}` | Notiz | ✓ | |
| _EIGENE1 | `{{###_EIGENE1}}` | Eigenes Feld 1 | ✓ | |
| _EIGENE2 | `{{###_EIGENE2}}` | Eigenes Feld 2 | ✓ | |
| _EIGENE3 | `{{###_EIGENE3}}` | Eigenes Feld 3 | ✓ | |

### Beteiligung an der Akte

| Konstante | Platzhalter | Bezeichnung | E-Mail | Anmerkung |
|---|---|---|---|---|
| _AKTE_ZEICHEN | `{{###_AKTE_ZEICHEN}}` | Zeichen des Beteiligten | ✓ | Zeichen aus der Beteiligung |
| _AKTE_KONTAKT | `{{###_AKTE_KONTAKT}}` | Kontakt / Ansprechpartner | ✓ |  |
| _AKTE_EIGENE1 | `{{###_AKTE_EIGENE1}}` | Beteiligung: Eigenes Feld 1 | ✓ | |
| _AKTE_EIGENE2 | `{{###_AKTE_EIGENE2}}` | Beteiligung: Eigenes Feld 2 | ✓ | |
| _AKTE_EIGENE3 | `{{###_AKTE_EIGENE3}}` | Beteiligung: Eigenes Feld 3 | ✓ | |

Die Zwischenüberschriften (Person, Unternehmen, ...) dienen nur der Durchsicht. Im Baum stehen
die Platzhalter eines Beteiligtentyps alphabetisch ohne weitere Ebene.

## Benutzer (`BENUTZER`, der angemeldete Autor)

| Konstante | Platzhalter | Bezeichnung | E-Mail |
|---|---|---|---|
| USER_AN | `{{USER_AN}}` | Anzeigename | ✓ |
| USER_VORNAME | `{{USER_VORNAME}}` | Vorname | ✓ |
| USER_NAME | `{{USER_NAME}}` | Name | ✓ |
| USER_KRZ | `{{USER_KRZ}}` | Kürzel | ✓ |
| USER_LOGIN | `{{USER_LOGIN}}` | Benutzername | ✓ |
| USER_FKT | `{{USER_FKT}}` | Funktion | ✓ |
| USER_UNTERNEHMEN | `{{USER_UNTERNEHMEN}}` | Unternehmen | ✓ |
| USER_STRASSE | `{{USER_STRASSE}}` | Straße | ✓ |
| USER_ZUSATZ | `{{USER_ZUSATZ}}` | Adresszusatz | ✓ |
| USER_PLZ | `{{USER_PLZ}}` | PLZ | ✓ |
| USER_ORT | `{{USER_ORT}}` | Ort | ✓ |
| USER_LAND | `{{USER_LAND}}` | Land | ✓ |
| USER_TEL | `{{USER_TEL}}` | Telefon | ✓ |
| USER_FAX | `{{USER_FAX}}` | Fax | ✓ |
| USER_MOBIL | `{{USER_MOBIL}}` | Mobil | ✓ |
| USER_EMAIL | `{{USER_EMAIL}}` | E-Mail | ✓ |
| USER_WWW | `{{USER_WWW}}` | Homepage | ✓ |
| USER_BANK | `{{USER_BANK}}` | Bank | ✓ |
| USER_BIC | `{{USER_BIC}}` | BIC | ✓ |
| USER_IBAN | `{{USER_IBAN}}` | IBAN | ✓ |
| USER_STEUERNR | `{{USER_STEUERNR}}` | Steuernummer | ✓ |
| USER_USTIDNR | `{{USER_USTIDNR}}` | USt-IdNr. | ✓ |

## Kanzlei (`KANZLEI`, Profil)

| Konstante | Platzhalter | Bezeichnung | E-Mail | Anmerkung |
|---|---|---|---|---|
| PROFIL_FIRMA | `{{PROFIL_FIRMA}}` | Name der Kanzlei | ✓ | |
| PROFIL_STRASSE | `{{PROFIL_STRASSE}}` | Straße | ✓ | |
| PROFIL_STRASSE2 | `{{PROFIL_STRASSE2}}` | Adresszusatz | ✓ | |
| PROFIL_PLZ | `{{PROFIL_PLZ}}` | PLZ | ✓ | |
| PROFIL_ORT | `{{PROFIL_ORT}}` | Ort | ✓ | |
| PROFIL_LAND | `{{PROFIL_LAND}}` | Land | ✓ | |
| PROFIL_TEL | `{{PROFIL_TEL}}` | Telefon | ✓ | |
| PROFIL_FAX | `{{PROFIL_FAX}}` | Fax | ✓ | |
| PROFIL_MOBIL | `{{PROFIL_MOBIL}}` | Mobil | ✓ | |
| PROFIL_EMAIL | `{{PROFIL_EMAIL}}` | E-Mail | ✓ | |
| PROFIL_WWW | `{{PROFIL_WWW}}` | Homepage | ✓ | |
| PROFIL_STEUERNR | `{{PROFIL_STNR}}` | Steuernummer | ✓ | |
| PROFIL_USTIDNR | `{{PROFIL_USTIDNR}}` | USt-IdNr. | ✓ | |
| PROFIL_BANK | `{{PROFIL_BANK}}` | Bank | ✓ | |
| PROFIL_BLZ | `{{PROFIL_BLZ}}` | BIC | ✓ | |
| PROFIL_KONTONR | `{{PROFIL_KONTONR}}` | IBAN | ✓ | |
| PROFIL_BANK_AK | `{{PROFIL_BANK_AK}}` | Anderkonto: Bank | ✓ | |
| PROFIL_BLZ_AK | `{{PROFIL_BLZ_AK}}` | Anderkonto: BIC | ✓ | |
| PROFIL_KONTONR_AK | `{{PROFIL_KONTONR_AK}}` | Anderkonto: IBAN | ✓ | |

## Datum und Dokument (`DATUM_DOKUMENT`)

| Konstante | Platzhalter | Bezeichnung | E-Mail | Anmerkung |
|---|---|---|---|---|
| KURZDATUM | `{{KURZDATUM}}` | heutiges Datum (TT.MM.JJJJ) | ✓ | |
| LANGDATUM | `{{LANGDATUM}}` | heutiges Datum mit Wochentag | ✓ | Format "Mi, 07.10.2026" |
| DOK_DZ | `{{DOK_DZ}}` | Diktatzeichen | ✓ | |
| TABELLE_1 | `{{TABELLE_1}}` | Berechnungstabelle | – | Tabellenplatzhalter |

## Ingo (`INGO`)

| Konstante | Platzhalter | Bezeichnung | E-Mail | Anmerkung |
|---|---|---|---|---|
| INGO_TEXT | `{{INGO_TEXT}}` | von Ingo erzeugter Text | ✓ | wird beim E-Mail-Versand nicht befüllt, bleibt aber angeboten |

## E-Mail (`EMAIL`, neue Konstanten)

| Konstante | Platzhalter | Bezeichnung | E-Mail | Anmerkung |
|---|---|---|---|---|
| CURSOR | `{{CURSOR}}` | Cursorposition nach dem Einfügen | ✓ | |
| CLOUD_LINK | `{{CLOUD_LINK}}` | Cloud-Link (nur E-Mail) | ✓ | im beA-Fenster leer ersetzt |

## Rechnung (`RECHNUNG`) – im E-Mail-Kontext ausgeblendet

| Konstante | Platzhalter | Bezeichnung |
|---|---|---|
| BEL_NR | `{{BEL_NR}}` | Belegnummer |
| BEL_TYP | `{{BEL_TYP}}` | Belegart |
| BEL_NAME | `{{BEL_NAME}}` | Bezeichnung |
| BEL_BESCHR | `{{BEL_BESCHR}}` | Beschreibung |
| BEL_DTERSTELLT | `{{BEL_DTERSTELLT}}` | Belegdatum |
| BEL_DTFAELLIG | `{{BEL_DTFAELLIG}}` | fällig am |
| BEL_DTLZVON | `{{BEL_DTLZVON}}` | Leistungszeitraum von |
| BEL_DTLZBIS | `{{BEL_DTLZBIS}}` | Leistungszeitraum bis |
| BEL_TOTAL | `{{BEL_TOTAL}}` | Gesamtbetrag (brutto) |
| BEL_WHRG | `{{BEL_WHRG}}` | Währung |
| BEL_SUM_NETTO | `{{BEL_SUM_NETTO}}` | Summe netto |
| BEL_UST_SATZ | `{{BEL_UST_SATZ}}` | USt-Satz |
| BEL_UST_BETRAG | `{{BEL_UST_BETRAG}}` | USt-Betrag |
| BEL_TABELLE | `{{BEL_TABELLE}}` | Positionstabelle |
| BEL_GIROCODE | `{{BEL_GIROCODE}}` | GiroCode |
| BEL_ABSAN | `{{BEL_ABSAN}}` | Absender: Anzeigename |
| BEL_ABSVORNAME | `{{BEL_ABSVORNAME}}` | Absender: Vorname |
| BEL_ABSNAME | `{{BEL_ABSNAME}}` | Absender: Name |
| BEL_ABSKRZ | `{{BEL_ABSKRZ}}` | Absender: Kürzel |
| BEL_ABSFKT | `{{BEL_ABSFKT}}` | Absender: Funktion |
| BEL_ABSUNTERNEHMEN | `{{BEL_ABSUNTERNEHMEN}}` | Absender: Unternehmen |
| BEL_ABSSTRASSE | `{{BEL_ABSSTRASSE}}` | Absender: Straße |
| BEL_ABSZUSATZ | `{{BEL_ABSZUSATZ}}` | Absender: Adresszusatz |
| BEL_ABSPLZ | `{{BEL_ABSPLZ}}` | Absender: PLZ |
| BEL_ABSORT | `{{BEL_ABSORT}}` | Absender: Ort |
| BEL_ABSLAND | `{{BEL_ABSLAND}}` | Absender: Land |
| BEL_ABSTEL | `{{BEL_ABSTEL}}` | Absender: Telefon |
| BEL_ABSFAX | `{{BEL_ABSFAX}}` | Absender: Fax |
| BEL_ABSMOBIL | `{{BEL_ABSMOBIL}}` | Absender: Mobil |
| BEL_ABSEMAIL | `{{BEL_ABSEMAIL}}` | Absender: E-Mail |
| BEL_ABSWWW | `{{BEL_ABSWWW}}` | Absender: Homepage |
| BEL_ABSBANK | `{{BEL_ABSBANK}}` | Absender: Bank |
| BEL_ABSBIC | `{{BEL_ABSBIC}}` | Absender: BIC |
| BEL_ABSIBAN | `{{BEL_ABSIBAN}}` | Absender: IBAN |
| BEL_ABSSTEUERNR | `{{BEL_ABSSTEUERNR}}` | Absender: Steuernummer |
| BEL_ABSUSTIDNR | `{{BEL_ABSUSTIDNR}}` | Absender: USt-IdNr. |

## Rechnungsposition (`RECHNUNGSPOSITION`) – im E-Mail-Kontext ausgeblendet

| Konstante | Platzhalter | Bezeichnung |
|---|---|---|
| BELP_NR | `{{BELP_NR}}` | Positionsnummer |
| BELP_NAME | `{{BELP_NAME}}` | Bezeichnung |
| BELP_BESCHR | `{{BELP_BESCHR}}` | Beschreibung |
| BELP_MENGE | `{{BELP_MENGE}}` | Menge |
| BELP_EINZEL | `{{BELP_EINZEL}}` | Einzelpreis |
| BELP_UST | `{{BELP_UST}}` | USt-Satz |
| BELP_NETTO | `{{BELP_NETTO}}` | Gesamt netto |

## Zeiterfassung (`ZEITERFASSUNG`) – im E-Mail-Kontext ausgeblendet

| Konstante | Platzhalter | Bezeichnung |
|---|---|---|
| ZE_TABELLE | `{{ZE_TABELLE}}` | Tabelle der Zeiteinträge |
| ZE_SUMMEN | `{{ZE_SUMMEN}}` | Summentabelle |
