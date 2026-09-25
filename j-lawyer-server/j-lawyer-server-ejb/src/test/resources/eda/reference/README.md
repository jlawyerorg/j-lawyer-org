# Referenzdateien aus dem Online-Mahnantrag

Echte, von den Mahngerichten erzeugte EDA-Dateien als Prüfstein für unseren Generator und Parser.
Erzeugt über <https://www.online-mahnantrag.de>, Antragsart **EDA-Download**.

**Es wird nichts eingereicht.** mahngerichte.de zur Download-Variante: *„Der Versand erfolgt in
diesem Fall über einen vom Einreicher betriebenen sicheren Übermittlungsweg (§ 130a ZPO); ein
Versand über den Online-Mahnantrag findet dabei nicht statt."* Die Datei landet im
Download-Ordner, sonst passiert nichts.

---

## Teil A — Der Weg durch den Assistenten

Die Feldbezeichnungen unten sind die des Assistenten. Wo eine Auswahlliste gemeint ist, steht der
Eintrag **fett**.

### A.1 Einstieg

1. <https://www.online-mahnantrag.de> aufrufen, Cookie-Hinweis bestätigen.
2. **Bundesland:** `Sachsen` → *Weiter*.
3. **Antragsart:** `EDA-Download` — steht bereits vorausgewählt, Alternativen wären Barcode und
   eID. → *Weiter*.
4. **Verfahrensart:** `normales Mahnverfahren` — ebenfalls vorausgewählt. (Die anderen sind
   Urkunden-, Scheck- und Wechselmahnverfahren; die brauchen wir nicht.) → *Weiter*.

### A.2 Die Navigationsleiste

Ab hier ist die Leiste am Rand das Wichtigste. Sie springt **frei** zwischen:

> Bundeslandauswahl · Prozessbevollmächtigter · Antragsteller · Antragsgegner ·
> **Hauptforderung/Zinsen** · Auslagen/Nebenforderungen · Allgemeine Angaben ·
> Datenübersicht · **EDA-Download**

Daraus folgt der eigentliche Trick: **Variante 01 einmal vollständig erfassen und herunterladen,
danach je Variante nur den genannten Abschnitt anspringen, ändern und erneut herunterladen.** Die
übrigen Angaben bleiben stehen. Zwölf Dateien kosten so keine zwölf Durchläufe.

### A.3 Beteiligte erfassen

Antragsteller und Antragsgegner haben je vier Reiter:

| Reiter | wofür |
|---|---|
| **Natürliche Person** | Anrede (*Herr/Frau*), Vorname, Name, Straße, PLZ, Ort, Land |
| **Firma** | Rechtsform aus Liste, Name 1–4 (lange Namen laufen über mehrere Felder), Anschrift |
| **Kennziffer** | Beteiligter ist beim Gericht unter einer Kennziffer hinterlegt |
| **Sonstige** | Parteien kraft Amtes u. ä. |

### A.4 Hauptforderung und Zinsen

Der Abschnitt beginnt mit dem **katalogisierten Anspruch**:

| Feld | Inhalt |
|---|---|
| Katalogart | Auswahlliste, z. B. `Kaufvertrag(11)`, `Miete für Wohnraum …(19)` |
| Anspruchsgrund | Auswahlliste: *Schreiben, Rechnung, Mahnung, Kontoauszug, Aufstellung, Vertrag, Stromrechnung, Gasrechnung, andere* |
| dessen Nummer | z. B. die Rechnungsnummer |
| von / bis | Zeitraum des Anspruchs |
| Betrag | Hauptforderung |
| Verbraucherkredit | Haken, nur bei §§ 491 ff. BGB |

Über Reiter erreichbar: **Sonstiger Anspruch** (Freitextbegründung) und **Ausgerechnete Zinsen**.

Nach dem Speichern kommt die Verzweigung **„Sie können jetzt …"** mit vier Möglichkeiten:

- `eine Abtretung … erfassen` → für Variante 09
- `Zinsangaben zum Anspruch erfassen` → **laufende Zinsen**, hier auch die Staffel
- `einen weiteren Anspruch oder ausgerechnete Zinsen erfassen`
- `keine weiteren Angaben` → weiter

> Diese Verzweigung ist leicht zu übersehen. Ohne den Punkt *Zinsangaben* enthält die Datei keine
> laufenden Zinsen.

### A.5 Prozessbevollmächtigter

| Feld | Wert (in **allen** Läufen gleich) |
|---|---|
| Anrede | **Rechtsanwalt** |
| Kennziffer | `07774512` |
| Name | `Jens Kutschke` |
| Straße | `Am Waldacker 4` |
| PLZ / Ort | `01689` / `Niederau` |
| Datum der Beauftragung | `02.03.2026` |
| Betrag, Mehrwertsteuersatz | leer lassen |
| Erklärung Vorsteuer, umsatzsteuerbefreit | Haken nicht ändern |

### A.6 Allgemeine Angaben — Pflicht

Ohne diesen Abschnitt kommt man nicht zum Download. Drei Erklärungen, die im Kennsatz landen:

| Erklärung im Assistenten | Feld | Bedeutung |
|---|---|---|
| *Anspruch hängt von einer Gegenleistung ab, diese ist aber bereits erbracht* | `VGLM1` | X gesetzt |
| *Anspruch hängt nicht von einer Gegenleistung ab* | `VGLM2` | X gesetzt |
| *Im Falle eines Widerspruchs beantrage ich die Durchführung des streitigen Verfahrens* | `ASTRVM` | X = Abgabe beantragt, leer = nicht beantragt |

Die ersten beiden gehören zusammen: § 688 Abs. 2 Nr. 2 ZPO lässt den Mahnbescheid nicht zu, wenn
der Anspruch von einer **noch nicht erbrachten** Gegenleistung abhängt — deshalb muss erklärt
werden, welcher der beiden Fälle vorliegt. Bei mehreren Ansprüchen dürfen laut Satzbeschreibung
**beide** Felder belegt sein. Die dritte ist der Antrag nach § 696 Abs. 1 ZPO.

**Im Grundfall:** die erste Erklärung ankreuzen (Kaufvertrag, Ware geliefert), die dritte **nicht**.
Die übrigen Belegungen verteilen sich auf die Varianten 05 und 06.

### A.6a Auslagen und Nebenforderungen

Der Abschnitt führt je Kostenart einen Betrag und — das ist leicht zu übersehen — **eigene
Zinsangaben**: Zinssatz, ein Haken *über Basiszinssatz* und ein Zeitraum von/bis. Die Kostenarten:

- Porto/Auslagen des Vordrucks, sonstiger Betrag mit eigener Bezeichnung
- Mahnkosten
- Auskunftskosten
- Bankrücklastkosten
- Inkassokosten
- vorgerichtliche Anwaltsvergütung

Bei der vorgerichtlichen Anwaltsvergütung kommen zwei Felder dazu, die in unseren Satz C10 gehören:
ein **abweichender Streitwert** für diese Vergütung und der Haken **besonders umfangreich /
schwierig**.

### A.7 Vergütung des Prozessbevollmächtigten — Pflicht

Ohne diese Angabe meldet die Datenübersicht *„Es wurde keine Vergütung für den
Prozessbevollmächtigten ermittelt."* Die Vergütung im Mahnverfahren ist zum 01.06.2025 neu
geregelt; die Gerichte verlangen die Angabe seither ausdrücklich. Drei Möglichkeiten, die im
Format **ein** Feld belegen (`IKUBET`):

| Auswahl | Feldinhalt |
|---|---|
| gesetzliche Vergütung nach dem RVG in voller Höhe | Feld bleibt **leer** |
| vereinbarte Vergütung, Gesamtbetrag einschl. Auslagen und ggf. USt. | **Betrag** größer null |
| vollständiger Verzicht | **0,00** |

Der Unterschied zwischen leer und 0,00 ist der teure: eine Null verzichtet auf die Vergütung.

**Im Grundfall:** `die gesetzliche Vergütung nach dem RVG in voller Höhe` wählen. Für Variante 10
zusätzlich eine Datei `10b-vereinbarte-verguetung.eda` mit der vereinbarten Vergütung `280,00` und
eine Datei `10c-verzicht.eda` mit dem Verzicht — damit liegen alle drei Belegungen des Feldes vor.

### A.8 Abschluss je Datei

1. **Datenübersicht** aufrufen und als PDF sichern — unter demselben Namen wie die EDA-Datei.
   Sie ist später die einzige Möglichkeit, eine Abweichung nachzuvollziehen.
2. **EDA-Download** aufrufen und die Datei herunterladen.
3. Datei nach `NN-kurzbeschreibung.eda` umbenennen, den **Originalnamen** (enthält die Antrags-ID)
   in der Tabelle unten notieren.

### A.9 Nach einem Session-Timeout

Die Sitzung läuft ab, und dann ist **alles** weg. Zwei Konsequenzen:

- **Nach jeder Variante sofort herunterladen**, nicht erst am Ende. Eine verlorene Sitzung kostet
  dann höchstens eine Variante.
- Zum Wiedereinstieg den Grundzustand in dieser Reihenfolge neu erfassen:

| Schritt | Eingabe |
|---|---|
| Bundesland | `Sachsen` |
| Antragsart | `EDA-Download` |
| Verfahrensart | `normales Mahnverfahren` |
| Anspruch | Katalogart `Kaufvertrag(11)` · Anspruchsgrund `Rechnung` · Nummer `R-2025-0815` · von `15.09.2025` · Betrag `5.000,00` |
| danach in der Auswahl | *Zinsangaben zum Anspruch erfassen* → `9,00 %` ab `15.10.2025`, dann *keine weiteren Angaben* |
| Antragsteller (nat. Person) | Frau · `Erika` · `Gläubiger` · `Hauptstr. 1` · `01067 Dresden` |
| Antragsgegner (nat. Person) | Herr · `Max` · `Schuldner` · `Bahnhofstr. 12` · `04109 Leipzig` |
| Prozessbevollmächtigter | `Rechtsanwalt` · Kennziffer `07774512` · `Jens Kutschke` · `Am Waldacker 4` · `01689 Niederau` · Beauftragung `02.03.2026` |
| Allgemeine Angaben | ☑ *Gegenleistung erbracht* · ☐ streitiges Verfahren |
| Vergütung | `gesetzliche Vergütung nach dem RVG in voller Höhe` |

Das ist zugleich Variante 01. Jede weitere Variante setzt darauf auf und ändert nur den bei ihr
genannten Abschnitt.

---

## Teil B — Variante 01, der Grundfall

*Natürliche Person gegen natürliche Person, ein katalogisierter Anspruch, laufende Zinsen.*
Prüft Kennsatz, beide Parteiblöcke, katalogisierten Anspruch und laufende Zinsen.

**Antragsteller** — Reiter *Natürliche Person*

| Feld | Wert |
|---|---|
| Anrede | Frau |
| Vorname | `Erika` |
| Name | `Gläubiger` |
| Straße | `Hauptstr. 1` |
| PLZ / Ort | `01067` / `Dresden` |

**Antragsgegner** — Reiter *Natürliche Person*

| Feld | Wert |
|---|---|
| Anrede | Herr |
| Vorname | `Max` |
| Name | `Schuldner` |
| Straße | `Bahnhofstr. 12` |
| PLZ / Ort | `04109` / `Leipzig` |

**Hauptforderung**

| Feld | Wert |
|---|---|
| Katalogart | `Kaufvertrag(11)` |
| Anspruchsgrund | `Rechnung` |
| Nummer | `R-2025-0815` |
| von | `15.09.2025` |
| Betrag | `5.000,00` |

**Zinsen** — in der Verzweigung *Zinsangaben zum Anspruch erfassen*:
fester Zinssatz **9,00 %** ab **15.10.2025**.

**Auslagen/Nebenforderungen:** keine.

**Allgemeine Angaben:** nur *„Anspruch hängt von einer Gegenleistung ab, diese ist aber bereits
erbracht"* ankreuzen. Das streitige Verfahren **nicht** beantragen.

---

## Teil C — Varianten

Jeweils **nur** die genannte Abweichung zum Grundfall; alles andere bleibt stehen.

### 02 — Antragsteller ist eine GmbH mit gesetzlichem Vertreter
*Prüft: Rechtsform statt Anredeschlüssel, Sätze des gesetzlichen Vertreters.*

Antragsteller auf Reiter **Firma**: Rechtsform `GmbH`, Name `Beispiel Handels GmbH`,
`Industriestr. 7`, `01067 Dresden`. Gesetzlichen Vertreter erfassen:
`Geschäftsführer`, `Max Muster`, gleiche Anschrift.

### 03 / 03b — GmbH & Co. KG gegen AG & Co. KG
*Der Fall, der uns schon einen echten Fehler gezeigt hat: die eine bekommt Anredeschlüssel 4 bei
leerem Rechtsformfeld, die andere gar keinen Schlüssel und trägt ihre Rechtsform.*
**Bitte zwei Dateien.**

Antragsgegner auf Reiter **Firma**:

| | `03` | `03b` |
|---|---|---|
| Rechtsform | `GmbH & Co KG` | `AG & Co KG` |
| Name | `Muster Transport GmbH & Co. KG` | `Muster Logistik AG & Co. KG` |
| Anschrift | `Industriestr. 7`, `04109 Leipzig` | dieselbe |
| **1. gesetzliche Vertreterin** | `Muster Verwaltungs-GmbH` | `Muster Verwaltungs-AG` |
| **deren Vertretung** | Geschäftsführer `Max Muster` | Vorstand `Erika Muster` |
| Anschriften der Vertreter | **beide leer lassen** | **beide leer lassen** |

**Es ist eine Kette, keine einzelne Person.** Die KG wird von ihrer persönlich haftenden
Gesellschafterin vertreten (§ 161 Abs. 2 i. V. m. § 125 HGB) — bei dieser Rechtsform die
Komplementär-GmbH. Die GmbH wiederum handelt durch ihren Geschäftsführer. Der Assistent fragt
deshalb erst nach dem *Namen der GmbH* und dann nach deren Vertretung. Bei der AG & Co. KG
entsprechend: Komplementär-AG, vertreten durch den Vorstand.

Das Format trägt das: *„Zu jedem Antragsteller können maximal 6 gesetzliche Vertreter
(ASGV_01/ASGV_02) eingetragen werden! Gesetzliche Vertreter werden immer dem unmittelbar
vorausgegangenen Antragsteller zugeordnet!"* Die Kette entsteht also aus mehreren aufeinander
folgenden Vertretersätzen — genau der Fall, den unser Datenmodell derzeit noch nicht abbildet
(siehe 5.3b).

Falls der Assistent zusätzlich eine **Funktion** abfragt: sie ist nicht frei wählbar. Die
Mahngerichte führen dazu zwei Verzeichnisse — *Liste der Rechtsformen* (Stand 26.02.2024) und
*Liste der Funktionen der gesetzlichen Vertreter* (Stand 06.10.2015), beide unter
<https://www.mahngerichte.de/verzeichnisse/rechtsformen-und-gesetzliche-vertreter/>. Die
Rechtsform verweist über einen Schlüssel auf die zulässigen Funktionen; für `GMBH & CO KG` ist das
Schlüssel 31 mit `Geschäftsführende Gesellschafterin`, `Geschäftsführer` und `Direktor`.

Die **Anschrift** der Vertreterin bleibt in beiden Fällen leer. Die Satzbeschreibung verlangt sie
nur, wenn eine natürliche Person oder ein eingetragener Kaufmann vertreten wird, bei einer Partei
kraft Amtes (Insolvenzverwalter, Testamentsvollstrecker) oder beim ersten Vertreter einer WEG.

### 04 — Zwei Antragsgegner als Gesamtschuldner
*Prüft das Gesamtschuldner-Merkmal im Kennsatz.*

Zweiten Antragsgegner ergänzen: Frau `Erika Schuldner`, gleiche Anschrift. Die
Gesamtschuldnerschaft im Assistenten bestätigen.

### 05 — Sonstiger Anspruch mit langer Begründung
*Prüft die Aufteilung des Freitextes über zwei Sätze (93 + 70 Zeichen).*

Statt des katalogisierten Anspruchs den Reiter **Sonstiger Anspruch**, Begründung:

> `Restwerklohn aus dem Bauvorhaben Musterstraße 12 in Leipzig gemäß Schlussrechnung vom 15.09.2025, Restbetrag nach Teilzahlung`

Zusätzlich unter *Allgemeine Angaben* **den Antrag auf Durchführung des streitigen Verfahrens
ankreuzen** — damit liegt `ASTRVM` einmal gesetzt vor.

### 06 — Katalog Nr. 28 mit Vertragsart
*Prüft den Zusatzsatz, den Nr. 28 verlangt.*

Katalogart `Schadensersatz aus Vertrag(28)`. Nummer 28 verlangt zwingend einen **Anspruchszusatz**:
die **Vertragsart**. Fehlt sie, bricht der Export mit *„Zu mindestens einem katalogisierbaren
Anspruch fehlen die Angaben zum Anspruchszusatz"* ab.

Das Feld gehört zum Anspruch, nicht zu den allgemeinen Angaben, und erscheint in der Regel erst,
nachdem die Katalognummer gewählt und gespeichert ist (im Vordruck: Zeile 35, zweite Hälfte).

Die Vertragsart ist **nicht frei formulierbar**. Sie stammt aus dem Verzeichnis
<https://www.mahngerichte.de/verzeichnisse/vertragsarten-zu-katalog-nr28/>, das die Arten in
Großbuchstaben und ohne das Wort „Vertrag" führt. Für unseren Fall: **`KAUF`** — nicht
„Kaufvertrag". Weitere Einträge derselben Liste zur Orientierung: `BARKAUF`, `RATENKAUF`,
`KFZ-KAUF`, `MIETKAUF`, `DARLEHEN`, `DIENSTLEISTUNG`, `BAU`, `BÜRGSCHAFT`.

Unter *Allgemeine Angaben* hier die **zweite** Erklärung ankreuzen (*hängt nicht von einer
Gegenleistung ab*) statt der ersten — sachlich richtig für einen Schadensersatzanspruch, und damit
liegt `VGLM2` einmal gesetzt vor.

### 07 — Wohnraummiete mit Anschrift der Wohnung
*Prüft den Zusatzsatz mit der Anschrift des Mietobjekts.*

Katalogart `Miete für Wohnraum …(19)`, Mietobjekt `Bahnhofstr. 12, 04109 Leipzig`,
von `01.06.2025` bis `31.08.2025`, Betrag `2.400,00`.

### 08 — Zinsstaffel und ausgerechnete Zinsen nebeneinander
*Prüft mehrere laufende Zinssätze und den Satz für bereits ausgerechnete Zinsen.*

Eine Staffel ist **keine eigene Maske**, sondern mehrere Zinszeilen zum selben Anspruch. Der
amtliche Ausfüllhinweis: *„Wenn für eine Hauptforderung oder einen Teil davon unterschiedliche
Zinssätze geltend gemacht werden sollen, ist für jeden Zinssatz die Zeilennummer der betreffenden
Hauptforderung zu wiederholen. Die Datumsangaben dürfen sich nicht überschneiden."* Mehr als drei
Zinszeilen verlangen ein Ergänzungsblatt.

Also den Zinsschritt **zweimal** durchlaufen:

1. In der Auswahl nach dem Anspruch *Zinsangaben zum Anspruch erfassen* → `5,00 %` vom
   `15.10.2025` bis `31.12.2025`.
2. Zurück auf derselben Auswahl **erneut** *Zinsangaben zum Anspruch erfassen* → `9,00 %` ab
   `01.01.2026`.

Die Zeiträume dürfen sich nicht überschneiden — deshalb endet der erste am 31.12. und der zweite
beginnt am 01.01.

Anschließend über *einen weiteren Anspruch oder ausgerechnete Zinsen erfassen* den Reiter
**Ausgerechnete Zinsen**: Betrag `123,45`, Satz `5,00 %`, vom `01.01.2025` bis `30.06.2025`.

### 09 — Abtretung
*Prüft den Abtretungssatz.*

Zu finden an **derselben Auswahl wie die Zinsen**, die nach dem Speichern eines Anspruchs
erscheint — dort die *erste* Option: *„eine Abtretung oder einen Forderungsübergang zum Anspruch
erfassen."* Bist du daran vorbei, über die Navigationsleiste zurück auf *Hauptforderung/Zinsen*
und den Anspruch erneut öffnen.

Einzutragen: Datum `15.02.2026`, Abtretender `Ursprungsgläubiger GmbH`, `70173 Stuttgart`.

Die Abtretung hängt am **einzelnen Anspruch**, nicht am Antrag; bei mehreren Ansprüchen also beim
richtigen wählen. Und „oder einen Forderungsübergang": dasselbe Feld trägt auch den gesetzlichen
Übergang (§ 86 VVG, § 116 SGB X) — im Format ist beides der Satz C25.

### 10 — Nebenforderungen und Anrechnung
*Prüft die Nebenforderungsbereiche und die Anrechnung nach Vorbem. 3 Abs. 4 VV RVG.*

Unter *Auslagen/Nebenforderungen*:

| Feld | Betrag |
|---|---|
| Porto / Auslagen | `12,50` |
| Mahnkosten | `5,00` |
| vorgerichtliche Anwaltsvergütung | `334,75` |

Die Zinsfelder der Kostenarten leer lassen — jede Kostenart trägt eigene, und für diese Variante
brauchen wir sie nicht.

Zur **Anrechnung** von `162,50`: Ob der Assistent danach fragt oder sie selbst aus der
vorgerichtlichen Vergütung errechnet, ist nicht geprüft. Taucht ein Feld für einen Minderungs- oder
Anrechnungsbetrag auf, `162,50` eintragen; errechnet er ihn selbst, den Wert aus der Datenübersicht
notieren — für den Vergleich zählt, was in der Datei steht.

### 11 — Antragsgegner im Ausland
*Prüft das Auslandskennzeichen.*

Antragsgegner `Max Schuldner`, `Rue de la Paix 4`, `1000 Bruxelles`, Land `Belgien`.

### 12 — ohne Kennziffer, Prozessbevollmächtigter in Vollform
*Prüft den Bereich, den eine Kennziffer sonst schließt — bei uns der Zweig, der im Echtbetrieb
nie läuft und deshalb sonst ungeprüft bliebe.*

Kennziffer beim Prozessbevollmächtigten **leeren**, Name und Anschrift stehen lassen.

### 13 — Antragsteller über Kennziffer — **entfällt**

Ursprünglich geplant, um die Antragstellerkennziffer (`ASKEZI`) im Kennsatz zu prüfen. Nicht
durchführbar: der Reiter *Kennziffer* beim Antragsteller erwartet eine Nummer, unter der der
**Gläubiger** beim Gericht registriert ist. `07774512` ist die Kennziffer des einreichenden
Anwalts und gehört in den Satz des Prozessbevollmächtigten. Eine Antragstellerkennziffer lässt
sich nicht erfinden — sie wird vom Gericht vergeben.

`ASKEZI` bleibt damit ohne Referenzdatei. Verschmerzbar: unser Builder schreibt das Feld nie, weil
der Antragsteller immer in Vollform übermittelt wird.

---

### 14 / 15 / 16 — Katalognummern mit Zusatzangabe — **beantwortet, keine Dateien nötig**
*Die Weboberfläche hat die Frage in Worten beantwortet, bevor eine Datei entstehen musste.*

Der Katalog verlangt zu acht Nummern eine weitere Angabe. Für Wohnungsanschrift (19, 20, 90) und
Vertragsart (28) war klar, wohin sie geht: in die eigenen Sätze C21 und C22. Für die übrigen vier
verweist der Katalog auf *„Vordruck Zeile 32-34"* und nennt eine **Spalte** — bei Nr. 36 die dritte,
bei Nr. 61 die zweite. Das klang nach zwei verschiedenen Orten.

Es ist keiner. Der Assistent weist beide Eingaben mit derselben Anweisung zurück:

> *„Bitte die Kontonummer im Feld Rechnungsnummer eintragen."* (Nr. 36)
> *„Bitte die Art der Wahlleistung im Feld Rechnungsnummer eintragen."* (Nr. 61)

Also **36, 42 und 61 gehen alle in `ASPRNR` und verdrängen die Rechnungsnummer** — es gibt eine
Spalte, nicht zwei. Was die Spaltenzählung des Papiervordrucks bedeutet, ist für die Datei
unerheblich.

Zu **Nr. 70** hat der Assistent **überhaupt keine Zusatzangabe verlangt**. Der *Zeitraum vom – bis*,
den der Katalog nennt, ist das von/bis der Anspruchszeile selbst, das ohnehin gefüllt wird. Ein
eigenes Feld dafür gibt es nicht.

Damit ist der offene Punkt geschlossen; die Dateien 14 bis 16 werden nicht gebraucht.

## Ablage

| Nr. | Datei | Antrags-ID | erzeugt am |
|---|---|---|---|
| 01 | `01-natperson-katalog11.eda` | | |
| 02 | `02-as-gmbh-mit-vertreter.eda` | | |
| 03 | `03-ag-gmbh-co-kg.eda` | | |
| 03b | `03b-ag-ag-co-kg.eda` | | |
| 04 | `04-zwei-gesamtschuldner.eda` | | |
| 05 | `05-sonstiger-anspruch.eda` | | |
| 06 | `06-katalog28-vertragsart.eda` | | |
| 07 | `07-wohnraummiete-anschrift.eda` | | |
| 08 | `08-zinsstaffel-und-ausgerechnete.eda` | | |
| 09 | `09-abtretung.eda` | | |
| 10 | `10-nebenforderungen.eda` | | |
| 10b | `10b-vereinbarte-verguetung.eda` | | |
| 10c | `10c-verzicht.eda` | | |
| 11 | `11-ag-ausland.eda` | | |
| 12 | `12-ohne-kennziffer.eda` | | |

## Was damit gebaut wird

1. **Annahmetest:** jede Datei wird mit unserem Codec gelesen und muss von
   `EdaStructureVerifier` beanstandungsfrei angenommen werden. Beanstandet er eine nachweislich
   korrekte Datei, ist *er* zu streng.
2. **Konformitätstest:** dieselben Falldaten laufen durch `EdaMahnbescheidBuilder`, und das
   Ergebnis wird satzweise und feldweise gegen die Referenz gestellt — mit einem Bericht, welches
   Feld in welchem Satzbereich abweicht.

Punkt 2 ist der Zweck der Übung. Er nimmt vorweg, was sonst erst der Testlauf beim Mahngericht
zeigen würde.
