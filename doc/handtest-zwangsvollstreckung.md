# Handtest: Zwangsvollstreckung

Was mit diesem Durchgang geprüft wird — und was noch nicht geht, steht am Ende.

## Vorbereitung

**Bauen und deployen.** Dazugekommen sind drei Migrationen — `V3_6_0_44` und `V3_6_0_45` (die
Anspruchsbegründung und ihre Nummer an der Forderungsposition) und `V3_6_0_46` (die Gebühren­
positionen Nr. 3309/3310 VV RVG und Nr. 2111 KV GKG). Sie laufen beim Start des Servers.

**Beide Feldzuordnungen übernehmen.** Die Zuordnung für *Anlage 1* wurde korrigiert, und
*Anlage 6* hat 16 Felder mehr bekommen (Abschnitt IV, die Kosten der Maßnahme). Der Import ergänzt
nur eine **fehlende** Zuordnung und rührt eine vorhandene nicht an — dafür gibt es jetzt den Knopf
**Zuordnung übernehmen**: Vorlage markieren, Knopf drücken, Rückfrage bestätigen. Erwartet:
„47 Felder zugeordnet" für Anlage 1 und **„131 Felder zugeordnet"** für Anlage 6. Löschen und neu
anlegen ist dafür nicht mehr nötig — das kostete auch die Datei, die Fassung und die Gültigkeiten.

**Standardpaket erneut importieren:** Finanzen → Vollstreckungsformulare → *Standardpaket
importieren*.

Es müsste achtmal „vorhanden, fehlende Datei ergänzt" melden — die PDFs waren durch den Fehler beim
Auflisten aus der Datenbank verschwunden — und bei Anlage 1 zusätzlich „40 Felder zugeordnet".
Danach beim Anklicken von *Anlage 1* unten **350 Felder**.

Der Import überschreibt nichts: er legt an, was fehlt, ergänzt eine fehlende Datei und eine
fehlende Feldzuordnung — Name, Fassung, Gültigkeiten und eine von Ihnen angepasste Zuordnung bleiben
unangetastet. Es muss also nichts vorher gelöscht werden.

Dieser Schritt ist nicht optional. Die Feldzuordnung entsteht **allein** beim Import — sie wird mit
dem Formularpaket ausgeliefert und nicht bei der Installation eingespielt. Meldet die Erzeugung in
Teil C, zu *Anlage 1* sei keine Zuordnung hinterlegt, dann ist der Import gegen den aktuellen Stand
noch nicht gelaufen. Der Bericht sagt seit dieser Fassung auch, wenn eine Vorlage ohne Zuordnung
bleibt, statt dazu zu schweigen.

## Testdaten

Eine Akte mit Forderungskonto. Im Forderungskonto:

**Reiter Stammdaten** — zwei Beteiligte, beide mit vollständiger Anschrift:

| | Gläubiger | Schuldner |
|---|---|---|
| Rolle | Gläubiger | Schuldner |
| Kontakt | eine Firma, z. B. `Beispiel Handels GmbH`, Rechtsform `GmbH` | eine natürliche Person, Feld **Anrede** = `Herr`, z. B. `Max Schuldner` |
| Anschrift | `Industriestr. 7`, `01067 Dresden` | `Bahnhofstr. 12`, `04109 Leipzig` |

Das Feld **Anrede** ist gemeint, nicht **Begrüßung**: die Begrüßung ist der Briefanredesatz („Sehr
geehrter Herr Schuldner,"), die Anrede ist das Wort, das auf dem Formular das Kreuz bei *Herr* oder
*Frau* setzt. Bleibt sie leer, kreuzt das Formular *Sonstige* an — richtig, denn geraten wird nicht.
Eine Firma braucht sie nicht, die wird an der Firmenbezeichnung erkannt.

Beim Schuldner zusätzlich das **Prozessgericht** ausfüllen (Amtsgericht, `04275`, `Leipzig`) — das
wird hier zwar nicht gebraucht, aber ohne gerät die Partei unvollständig ins System.

**Reiter Positionen** — eine Hauptforderung, z. B. `Kaufpreis`, `5.000,00 €`.

## Teil A — Die Voraussetzungen des § 750 Abs. 1 ZPO

Das ist der eigentliche Prüfpunkt: die Vollstreckung braucht Titel, Klausel **und** Zustellung, und
der Dialog soll **einzeln benennen**, was davon fehlt.

**A1 — ohne Titel.** Reiter *Zwangsvollstreckung* → **Maßnahme**. Im Feld *Titel* steht „- ohne
Titel -".

Erwartet: die meisten Maßnahmearten sind mit „— nicht möglich" gekennzeichnet, der Hinweis darunter
nennt § 750 Abs. 1 ZPO. Vorausgewählt ist die erste Art, die **doch** geht — das ist die
*Vollstreckungsandrohung*, denn die steht vor der Vollstreckung. Ebenfalls möglich sein müssen
*Einwohnermeldeamtsanfrage* und *Auskunft aus dem Schuldnerverzeichnis*.

Dialog abbrechen.

**A2 — Titel ohne Klausel.** Reiter *Titel* → neuen Titel anlegen:

| Feld | Wert |
|---|---|
| Titelart | Vollstreckungsbescheid |
| Erlassende Stelle | `Amtsgericht Leipzig` |
| Aktenzeichen | `12 C 345/26` |
| Erlassdatum | `10.03.2026` |
| Vollstreckbare Ausfertigung | **leer lassen** |
| Zustellung an Schuldner | **leer lassen** |

Zurück in *Zwangsvollstreckung* → **Maßnahme**, oben den Titel auswählen.

Erwartet: die Liste wird neu beantwortet, und der *Vollstreckungsauftrag an den Gerichtsvollzieher*
ist weiterhin nicht möglich — mit dem Hinweis auf die fehlende **Vollstreckungsklausel (§ 724 ZPO)**.

**A3 — Klausel ja, Zustellung nein.** Titel bearbeiten, *Vollstreckbare Ausfertigung* auf
`12.03.2026`. Dialog erneut öffnen.

Erwartet: jetzt nennt der Hinweis die fehlende **Zustellung (§ 750 Abs. 1 ZPO)**. Dass sich die
Meldung zwischen A2 und A3 ändert, ist der Punkt — „der Titel ist unvollständig" würde Ihnen nicht
sagen, was Sie besorgen müssen.

**A4 — Zustelldatum in der Zukunft.** *Zustellung an Schuldner* auf ein Datum **nach heute**
setzen, etwa `31.12.2026`.

Erwartet: immer noch nicht möglich. Ein Tippfehler im Zustelldatum darf die Vollstreckung nicht
freigeben.

**A5 — vollständig.** *Zustellung an Schuldner* auf `20.03.2026`.

Erwartet: der *Vollstreckungsauftrag an den Gerichtsvollzieher* ist möglich, und der Hinweis
darunter beschreibt ihn statt ein Hindernis zu nennen.

**Vorausgewählt ist er nicht** — vorausgewählt ist immer die *erste* Art der Liste, die möglich
ist, und das bleibt die *Vollstreckungsandrohung*, weil sie vor der Vollstreckung steht. Das ist
so gewollt und keine Fehlfunktion: die Reihenfolge der Liste ist der Ablauf. (Eine andere Vorgabe
wäre machbar — sagen Sie Bescheid, wenn Sie lieber die häufigste Maßnahme vorausgewählt hätten.)

**A6 — die Wertgrenze.** Die Hauptforderung auf `500,00 €` ändern und den Dialog erneut öffnen.

Erwartet: die *Zwangssicherungshypothek* ist nicht möglich, mit dem Hinweis auf § 866 Abs. 3 ZPO
(erst ab 750 Euro). Alles andere bleibt möglich. Danach wieder auf `5.000,00 €` setzen.

**Wichtig:** die Grenze misst am *Forderungskonto*, und das Konto rechnet aus **Buchungen**. Bis
zu dieser Fassung änderte das Betragsfeld an der Position nichts daran — die Position zeigte 500,
das Konto führte weiter 5.000, und die Hypothek blieb möglich. Jetzt folgt die Buchung dem Betrag,
solange auf der Position noch nichts weiter gebucht wurde. Ist bereits gezahlt oder verzinst
worden, lehnt das Speichern mit einer Begründung ab: die Änderung gehört dann als Korrekturbuchung
in den Reiter *Buchungen*.

## Teil B — Die Maßnahme

**B1 — anlegen.** Dialog mit vollständigem Titel, Art *Vollstreckungsauftrag an den
Gerichtsvollzieher*:

| Feld | Wert |
|---|---|
| Datum | heute |
| Empfänger | `Gerichtsvollzieherverteilungsstelle beim Amtsgericht Leipzig` |
| Anschrift (**zwei** Zeilen) | `Bernhard-Göring-Str. 64`<br>`04275 Leipzig` |
| Notizen | beliebig |

Erwartet: die Maßnahme erscheint in der Tabelle mit Datum, Art, Empfänger; Ergebnis und
Formularfassung bleiben leer.

**Der Empfänger ist Pflicht**, und wie er sich auf das Formular verteilt, ist festgelegt: die
Bezeichnung geht in die Empfängerzeile, von der Anschrift die **erste** Zeile in „Postfach oder
Straße und Hausnummer" und **alles Weitere** in „Postleitzahl und Ort". Eine einzeilige Anschrift
lässt deshalb das Feld für Postleitzahl und Ort leer, und die Formularerzeugung bricht ab — mit
einer Meldung, die jede fehlende Angabe beim Namen nennt und dazusagt, wo sie einzutragen ist:

> Diese Pflichtangaben fehlen:
>   - Postleitzahl und Ort (einzutragen unter: Empfänger der Maßnahme, weitere Zeilen der Anschrift)

**B1a — nachtragen.** Fehlt an einer bestehenden Maßnahme etwas, lässt sie sich jetzt ändern:
markieren → **Stift-Symbol**. Der Dialog öffnet sich mit ihren Werten; Titel, Art, Datum,
Empfänger und Notizen sind änderbar. Ergebnis und Formularfassung bleiben, wo sie sind — das eine
wird über *Ergebnis* festgehalten, das andere hält fest, was geschehen ist. Vorher musste eine
Maßnahme dafür gelöscht und neu angelegt werden.

**B2 — Ergebnis erfassen.** Maßnahme markieren → **Ergebnis** → `fruchtlos`, Datum bestätigen.

Erwartet: in der Tabelle steht „fruchtlos (Datum)", und **unten erscheint ein Hinweis**, dass damit
der Weg zur Vermögensauskunft nach § 802c ZPO und zum Schuldnerverzeichnis eröffnet ist. Fruchtlos
ist der Anfang des nächsten Schritts, keine Sackgasse — das soll die Oberfläche sagen.

**B3 — Ergebnis ändern** auf `einstweilen eingestellt`. Die Maßnahme bleibt in der Liste; sie ist
nicht erledigt, denn die Raten können platzen.

**B4 — entfernen.** Maßnahme markieren → Papierkorb-Symbol.

Erwartet: die Rückfrage weist darauf hin, dass erzeugte Dokumente in der Akte bleiben.

## Teil C — Formulare erzeugen

**C1.** Maßnahme *Vollstreckungsauftrag* markieren → **Formulare erzeugen** → *Ja* (festschreiben).

Der Knopf hängt **nicht** am Ergebnis, sondern allein daran, ob die Maßnahmeart überhaupt ein
amtliches Formular hat. Ist er grau, obwohl eine Maßnahme markiert ist, steht der Grund im
Hinweisfeld darunter — bei einer *Vollstreckungsandrohung* etwa, weil die ZVFV dafür kein Formular
vorsieht.

**Erwartet werden jetzt zwei Dokumente** im Reiter *Dokumente* der Akte: der Vollstreckungsauftrag
(Anlage 1) und die Forderungsaufstellung (Anlage 6). Der Auftrag besteht aus beiden; das Formular
allein wäre unvollständig.

**C2. Die Forderungsaufstellung prüfen.** Sie ist ein Positionsformular mit festen Plätzen, kein
Formular mit Zeilen. Zu prüfen ist deshalb nicht, ob etwas dasteht, sondern ob es an der richtigen
Stelle steht:

- **Abschnitt I, erster Block:** die Hauptforderung. Angekreuzt ist *Hauptforderung*, wenn nichts
  gezahlt wurde — sonst *Restforderung*, und dann steht links die ursprüngliche Höhe und rechts der
  Rest.
- **Die ausgerechneten Zinsen** in derselben Zeile: Satz, Betrag, von wann bis wann. „bis" ist der
  Stichtag der Maßnahme, nicht heute.
- **Die laufenden Zinsen** in der unteren Zinszeile desselben Blocks: Satz und Betrag, aber *ohne*
  Enddatum und *ohne* Zinsbetrag. Das ist richtig so — die rechnet der Gerichtsvollzieher am Tag
  der Beitreibung selbst aus, und die Summe unten zählt sie ausdrücklich nicht mit.
- **Abschnitt III:** die Kosten. „Festgesetzte Kosten" nur bei einem Kostenfestsetzungsbeschluss,
  „vorgerichtliche Kosten" bei Mahn-, Auskunfts- und Inkassokosten, „Kosten des Mahnverfahrens" nur
  dann, wenn der Titel ein **Vollstreckungsbescheid** ist. Bei einem Urteil stehen dieselben Kosten
  in der freien Zeile darunter — die Überschrift des Blocks wäre sonst eine Behauptung über einen
  Titel, den es nicht gibt.
- **Leer bleiben** (und das ist beabsichtigt): Abschnitt II (rückständiger Unterhalt — der gehört in
  die Anlage 8), der dritte Forderungsblock (Säumniszuschläge führt das Forderungskonto nicht), die
  jeweils zweite Zins- und laufende Zinszeile, und in Abschnitt IV die Rechtsanwaltskosten nach RVG
  für *diese* Maßnahme (§ 788 ZPO — Aufgabe 4.6, rechnet das System noch nicht).
- **Summe I. bis IV.** unten rechts: sie muss mit der Forderungsaufstellung im Reiter
  *Forderungskonto* zum selben Stichtag übereinstimmen.

**C3. Der Überlauf.** Legen Sie zum Ausprobieren eine **dritte** titulierte Hauptforderung an und
erzeugen Sie erneut. Erwartet wird eine Fehlermeldung, die die Position beim Namen nennt:

> Die Forderungsaufstellung hat mehr Positionen, als das amtliche Formular Plaetze hat. Ohne Platz
> blieben: …

Das amtliche Formular hat zwei Blöcke für titulierte Hauptforderungen, nicht mehr. Ein Formular,
das die dritte stillschweigend wegließe, sähe vollständig aus — und der Gerichtsvollzieher triebe
sie nie bei.

**Wichtig für die Prüfung:** nach einem Fehler darf im Reiter *Dokumente* **nichts** liegen. Der
Dienst prüft alle Formulare, bevor er das erste schreibt; andernfalls hielte die Akte einen halben
Antrag, den niemand als solchen erkennt.

## Teil D — Was in der Nacht dazugekommen ist

Vier Dinge, die noch niemand benutzt hat. Der Reihe nach, weil sie aufeinander aufbauen.

**D1 — die Buchungsart bleibt, was sie ist.** Reiter *Buchungen*, die initiale Buchung der
Hauptforderung markieren → bearbeiten → nur *OK* drücken, ohne etwas zu ändern.

Erwartet: die Art bleibt **Hauptforderung**. Vorher genügte dieses Öffnen-und-Bestätigen, um sie in
eine *Zinsbuchung* zu verwandeln — die Liste führt die Hauptforderung nicht, also stand dort der
erste Listeneintrag, und der wurde beim Speichern übernommen. Danach zählte die Forderung nicht mehr
zur Forderung, und weil Zinsen auf die Forderung laufen, waren auch die Zinsen weg.

Gegenprobe: im Reiter *Summen* muss die Forderung unverändert stehen.

**D2 — die Position merkt sich alles.** Reiter *Positionen* → Position bearbeiten:

| Feld | Wert |
|---|---|
| Anspruchsbegründung | `Rechnung` |
| Nummer/Beleg | `R-2025-0815` |
| Anspruchsart (Katalog) | `Kaufvertrag (11)` |

Speichern, Dialog schließen, Position **erneut öffnen**.

Erwartet: alle drei stehen noch da. Bis gestern speicherte das Bearbeiten nur Name, Kommentar,
Betrag und Art — Katalognummer, Zusatzangaben, Zinsbeginn und Wiederholung gingen verloren, ohne
dass etwas gesagt wurde.

Zweite Probe: wählen Sie testweise die Katalognummer **36** (Kontoüberziehung). Erwartet: die Zeile
*Nummer/Beleg* verschwindet, und an ihre Stelle rückt die Zusatzangabe des Katalogs mit **eigener
Beschriftung** („Konto-Nummer:"). Es sieht aus wie ein Feld, das seine Beschriftung wechselt, sind
aber zwei — bei 36, 42 und 61 belegt die Zusatzangabe dieselbe Spalte der Austauschdatei, und ein
Feld anzubieten, dessen Inhalt der Antrag wegwirft, lädt zu einer Eingabe ein, die verschwindet.
Danach wieder auf `Kaufvertrag (11)` zurück.

**D3 — die Begründung im Mahnantrag.** Reiter *Mahnverfahren* → EDA-Datei erzeugen. In der Tabelle
ist die Spalte *Rechnungsnummer* jetzt aus der Position **vorbelegt**; was dort steht, geht hinaus
und ändert die Position nicht.

Erzeugte Datei im Reiter *Dokumente* öffnen → Reiter **Antragsdaten**.

Erwartet: die gerenderte Seite zeigt unter *1. Katalogisierbarer Anspruch*

> Anspruch: Kaufvertrag
> (Katalog-Nr. 11)
> Mitteilungsform: **Rechnung**
> Rechnungsnummer: **R-2025-0815**

Vorher stand dort „Mitteilungsform: PKW" — der Name der Position im Feld für die Anspruchsbegründung.
Das druckt der Mahnbescheid: „aus Rechnung Nr. R-2025-0815 vom …".

Prüfen Sie im selben Zug den Kopf: **Anrede** des Schuldners (nicht mehr „Sonstige", sofern am
Kontakt gepflegt) und **Rechtsform** der Gläubigerin.

**D4 — die Kosten der Zwangsvollstreckung.** Reiter *Zwangsvollstreckung*, Maßnahme markieren →
**Kosten**.

Erwartet im Dialog:

- *Verfahrensgebühr Zwangsvollstreckung*, Nr. 3309 VV RVG, 0,3 aus dem Gegenstandswert. Bei 5.000 €
  offener Forderung sind das **106,35 €** (1,0-Gebühr 354,50 € nach der Tabelle seit 01.06.2025).
- *Post- und Telekommunikationspauschale*, Nr. 7002 VV RVG: 20 % davon = 21,27 €, **gedeckelt auf
  20,00 €**.
- *Umsatzsteuer*, Nr. 7008 VV RVG, 19 % auf beides.
- *Kosten des Gerichtsvollziehers* — **ohne Betrag und ohne Häkchen.** Das ist Absicht: sie hängen an
  seinen Amtshandlungen und am Wegegeld, nicht am Gegenstandswert. Eine Zahl vorzuschlagen hieße,
  sie zu erfinden. Tragen Sie testweise `33,00` ein und setzen Sie das Häkchen.
- Keine Gerichtsgebühr — die entsteht erst beim Pfändungs- und Überweisungsbeschluss (Nr. 2111 KV
  GKG, 20,00 €), nicht beim Gerichtsvollzieher.

Summe unten muss mitlaufen, sobald Sie einen Betrag ändern oder ein Häkchen setzen.

**Eine Frage an Sie dabei:** der Gegenstandswert ist zurzeit die *offene Forderung* des Kontos —
Hauptforderung, Kosten **und** aufgelaufene Zinsen. Nach § 25 Abs. 1 Nr. 1 RVG ist es der Betrag der
zu vollstreckenden Forderung; Zinsen als Nebenforderung erhöhen den Wert nach § 4 Abs. 1 ZPO
grundsätzlich nicht. Sehen Sie im Dialog nach, welcher Wert oben steht, und sagen Sie mir, ob die
Zinsen herausgerechnet werden sollen — das ist eine Frage, die ich nicht raten will.

**Buchen.** Danach im Reiter *Positionen*: für jede angesetzte Position eine neue Kostenposition,
deren Herkunft die Maßnahme nennt. Im Reiter *Summen* steigt die offene Forderung entsprechend —
§ 788 Abs. 1 ZPO lässt die Kosten mit der Hauptforderung beitreiben, deshalb stehen sie im
Forderungskonto und nicht auf einer Rechnung.

**D4a — ein Schuldner allein.** Nochmal *Kosten*, im Dialog unten den Schuldner einzeln wählen und
buchen. Erwartet: im Reiter *Buchungen* trägt die neue Zeile in der Spalte **Schuldner** dessen
Namen; bei allen übrigen steht „alle". Die Spalte gab es vorher nicht — eine Einzelschuld sah in
der Tabelle genauso aus wie eine gesamtschuldnerische.

**D4b — verauslagt.** Nochmal *Kosten*, Haken *von der Kanzlei verauslagt*. Erwartet: zusätzlich
eine Auslage im **Aktenkonto** der Akte. Das Geld ist aus der Kanzleikasse geflossen, lange bevor
der Schuldner zahlt; das Forderungskonto sagt nur, was der Schuldner schuldet.

**D5 — Abschnitt IV der Forderungsaufstellung.** Zuerst prüfen, dass die Zuordnung aktuell ist:
Finanzen → Vollstreckungsformulare → *Anlage 6* markieren → **Zuordnung übernehmen** (neuer Knopf).
Erwartet: „131 Felder zugeordnet". Ohne das bleibt Abschnitt IV leer, denn der Import ergänzt nur
eine fehlende Zuordnung und rührt eine vorhandene nicht an.

Dann Formulare der Maßnahme **erneut erzeugen** und die Anlage 6 öffnen, Seite 2, Abschnitt *IV. Kosten der Zwangsvollstreckung gemäß § 788 Absatz 1 ZPO*.

Erwartet: der erste RVG-Block trägt die Bezeichnung der Maßnahme, den Gegenstandswert, die
Verfahrensgebühr, die Pauschale, die Umsatzsteuer und die Zwischensumme — **die gebuchten Beträge,
nicht die vorgeschlagenen.** Probe darauf: buchen Sie die Umsatzsteuer bewusst nicht mit und
erzeugen Sie erneut; im Formular darf sie dann auch nicht stehen. Ein Formular, das die Rechnung
zeigt, während das Konto etwas anderes führt, ließe zwei Dokumente derselben Sache einander
widersprechen — und auffallen würde es dem Schuldner.

Leer bleiben, absichtlich: *Bisherige Vollstreckungskosten gemäß Aufstellung in weiterer Anlage*
(die Zeile verweist auf eine Anlage, die wir nicht beilegen; frühere Vollstreckungskosten stehen in
der freien Zeile desselben Abschnitts) und der zweite RVG-Block, solange keine Terminsgebühr
angefallen ist.

**D6 — die Reiterfolge.** Nur ein Blick: Stammdaten, Positionen, Buchungen, Summen, Aufstellung,
Mahnungen, Mahnverfahren, Titel, Zwangsvollstreckung, Basiszinsen. Und der Reiter *Aufstellung* muss
sich beim Anklicken **neu aufbauen** — bis gestern lud er sich beim Anklicken des Mahnverfahrens neu
und beim eigenen nie.

## Was noch nicht geht

- **Anlagen 2 bis 5, 7 und 8** haben noch kein Zuordnungsprofil. Zugeordnet sind Anlage 1 (der
  Auftrag) und Anlage 6 (seine Forderungsaufstellung); damit ist der Gerichtsvollzieherauftrag
  vollständig, der Pfändungs- und Überweisungsbeschluss noch nicht.
- **Drittschuldner** und die Erklärungsfrist des § 840 ZPO (Aufgabe 4.5).
- **Vollstreckungskosten** nach § 788 ZPO (4.6).
- **Wiedervorlagen** je Maßnahme und der Reiter *Fristen & Dokumente* (4.7).
- Die **Optionen des § 802a Abs. 2 ZPO** — Sachpfändung, gütliche Erledigung, Vermögensauskunft,
  Haftbefehl — gehören an die einzelne Maßnahme und fehlen noch im Dialog.

## Was ich gern zurückgemeldet hätte

Vor allem Teil A: ob die Meldungen zwischen A2, A3 und A4 tatsächlich wechseln und benennen, was
fehlt. Das ist die Stelle, an der eine Kanzlei sonst einen Auftrag hinausschickt, den der
Gerichtsvollzieher zurückgibt — nach Wochen und auf Kosten des Versuchs.
