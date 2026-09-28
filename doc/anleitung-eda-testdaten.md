# EDA-Testdaten für das Mahngericht von Hand erzeugen

Das Gericht bietet an, Mahnbescheid-Anträge im EDA-Format zu prüfen und Kostennachricht und
Monierung zurückzuliefern. Diese Anleitung erzeugt einen Satz Dateien, der die Stellen trifft, an
denen unsere Umsetzung Entscheidungen trifft — nicht zehnmal denselben Normalfall.

## Vor dem ersten Antrag

**Kennziffer.** Ohne Prozessbevollmächtigten-Kennziffer (PVKEZI) kein konventionelles EDA, auch im
Test nicht. Sie ersetzt im Datensatz die Adressangaben des Bevollmächtigten; fehlt sie, schreibt
j-lawyer stattdessen die Anschrift aus — beides sollte einmal geprüft werden, aber die Kennziffer
ist der Regelfall.

**Beim Gericht klären:** welches Mahngericht für den Test, ob eine Test-Kennziffer zu verwenden ist,
ob der Dateiname einer Konvention folgt (unser Feld ist sechsstellig) und auf welchem Weg
eingereicht wird.

**Nur erfundene Personen.** Ein Test bei Gericht ist kein Anlass, echte Schuldnerdaten in ein fremdes
System zu geben. Sprechende Namen helfen später beim Zuordnen der Rückläufe: *Albert Antrag*,
*Bertha Beklagt*, *Cäsar Consumer GmbH*.

**Eine Akte je Konstellation.** Das hält die Dateien auseinander und die Rückläufe zuordenbar.

---

## Der Weg durch die Oberfläche (einmal ausführlich)

**1. Forderungskonto anlegen.** Akte → Reiter **Finanzen** → **Forderungskonto** → neues Konto.
Name so vergeben, dass er die Konstellation nennt: `K1 Grundfall`.

**2. Reiter *Stammdaten*.** **Beteiligten hinzufügen** für Gläubiger und Schuldner. Der Kontakt
kommt aus den Beteiligten der Akte — wer dort fehlt, muss zuerst in die Akte. Je Beteiligtem:
- **Rolle** (Gläubiger / Schuldner) und die Reihenfolge, in der sie im Antrag stehen sollen,
- bei juristischen Personen den **Vertreter** mit seiner **Stellung/Funktion**; die Liste stammt aus
  dem Verzeichnis der Gerichte, und was dort steht, akzeptiert das Mahngericht.

Unten am Panel: **Tilgungsreihenfolge**, **Anzahl Gläubiger (Nr. 1008 VV RVG)** und das Häkchen
**Verbraucherdarlehen (§ 497 Abs. 3 BGB)**.

**3. Reiter *Positionen*.** Je Forderung eine Position. Der Dialog fragt:
- **Typ** — Hauptforderung, laufende monatliche Hauptforderung, Kosten, Zinsrückstand, und die
  vorgerichtlichen Positionen (Mahnkosten, Auskunftskosten, Inkassokosten, Auslagen,
  Bankrücklastkosten, vorgerichtliche Anwaltsvergütung Nr. 2300 VV RVG),
- **Betrag**,
- **Anspruchsart** — die Katalognummer des Mahnverfahrens (11 Kaufvertrag, 19 Miete Wohnraum,
  28 Schadenersatz aus Vertrag …). Einige verlangen eine **Zusatzangabe**, der Dialog sagt welche,
- **Anspruchsbegründung** (Rechnung, Mahnung, Vertrag, Kontoauszug …) und **Nummer/Beleg**,
- **Zinsregeln**: *gültig ab* und das Zinsmodell — **fester Zinssatz** oder
  **Basiszinssatz + Aufschlag**.

**4. Reiter *Mahnverfahren*.** **Neue Mahnsache** → **Gericht ermitteln** (aus dem Wohnsitz des
Antragstellers bzw. der Zuordnungstabelle) → **Eigenes Zeichen** und **Kennziffer** eintragen →
**Mahnsache speichern**.

**5. Antrag erzeugen.** Knopf **Antrag erzeugen**. Der Dialog fragt nach:
- **Streitwert** (die Hauptforderung; Nebenforderungen bleiben nach § 43 GKG außer Betracht),
- **Anrechnung Nr. 2300/2302 VV RVG** und **Beauftragung**,
- **Gegenleistung** — drei Möglichkeiten, von denen genau eine zutrifft,
- ob im Falle eines Widerspruchs das **streitige Verfahren** beantragt wird,
- die **Vergütung des Prozessbevollmächtigten** — RVG in voller Höhe, vereinbarte Vergütung mit
  Betrag, oder vollständiger Verzicht,
- den **Dateinamen** (sechs Zeichen).

Erst **Antrag prüfen** — der Bericht nennt jede Beanstandung, auch die nicht blockierenden. Dann
**EDA-Datei erzeugen**. Erzeugt wird nur, was die Prüfung besteht *und* die Strukturprüfung
durchläuft; die Datei liegt danach als Dokument in der Akte, und die Mahnsache steht auf
„Mahnbescheid beantragt".

**6. Gegenlesen, bevor die Datei das Haus verlässt.** Das Dokument in der Akte öffnen → Reiter
**Antragsdaten**. Dort steht der Antrag so, wie das Gericht ihn liest. Was dort falsch aussieht, ist
falsch — kein Test beweist, dass ein Feld bedeutet, was sein Name sagt.

---

## Die Konstellationen

Für jede gilt: **nur die genannte Abweichung** gegenüber K1, alles andere bleibt gleich. So sagt ein
Monierungsgrund etwas darüber aus, woran es lag.

### K1 — Grundfall
Natürliche Person gegen natürliche Person. Eine Hauptforderung, Katalog **11 Kaufvertrag**,
Anspruchsbegründung **Rechnung** mit Nummer, Zinsen **Basiszinssatz + 5** ab Rechnungsdatum, dazu
**Mahnkosten** als Nebenforderung. Vergütung: RVG in voller Höhe.
*Prüft:* den Normalfall — Parteien, Katalogfall, Zinsangabe, eine Nebenforderung, Kennziffer.

### K2 — Firma mit gesetzlichem Vertreter
Gläubiger ist eine **GmbH**, vertreten durch ihren Geschäftsführer (Stellung aus der Liste).
*Prüft:* ob die Firma als solche und ihr Vertreter getrennt und in den richtigen Sätzen stehen — der
häufigste Monierungsgrund überhaupt.

### K3 — Zwei Antragsgegner
Zwei Schuldner, gesamtschuldnerisch.
*Prüft:* die Wiederholung des Antragsgegner-Blocks und die Zählung im Kopfsatz.

### K4 — Zwei Antragsteller
Zwei Gläubiger, **Anzahl Gläubiger (Nr. 1008 VV RVG)** entsprechend gesetzt.
*Prüft:* mehrere Antragsteller und die Erhöhungsgebühr.

### K5 — Katalogfall mit Zusatzangabe
Katalog **19 Miete für Wohnraum** — verlangt **PLZ und Ort der Wohnung**. Alternativ **28
Schadenersatz aus Vertrag**, das die **Vertragsart** verlangt.
*Prüft:* die Zusatzangabe, die nur bei bestimmten Katalognummern existiert und im Vordruck an einer
eigenen Stelle steht.

### K6 — Laufende Forderung
Typ **laufende monatliche Hauptforderung** mit Zeitraum, z. B. Miete von 03/2026 bis 08/2026.
*Prüft:* die Zeitraumangabe statt eines Einzelbetrags.

### K7 — Verbraucherdarlehen mit hohem Zins
Häkchen **Verbraucherdarlehen (§ 497 Abs. 3 BGB)**, Katalog **4 Darlehensrückzahlung**, Zinsen
**über** fünf Punkten über dem Basiszinssatz.
*Prüft:* zwei Angaben, die beim Gericht eine zusätzliche Prüfung auslösen — hier ist eine Monierung
die interessantere Antwort als ein Durchlauf.

### K8 — Ohne laufende Zinsen, dafür Zinsrückstand
Keine Zinsregel an der Hauptforderung, stattdessen eine Position vom Typ **Zinsrückstand**.
*Prüft:* den Fall, in dem Zinsen als bezifferte Nebenforderung geltend gemacht werden.

### K9 — Breite Nebenforderungen
**Auslagen des Gläubigers**, **Auskunftskosten**, **Bankrücklastkosten**, **Inkassokosten** und
**vorgerichtliche Anwaltsvergütung (Nr. 2300 VV RVG)** mit gesetzter **Anrechnung**.
*Prüft:* die Zuordnung jeder Nebenforderungsart zu ihrem Feld und die Anrechnung.

### K10 — Vergütungsvarianten
Zweimal dieselbe Sache: einmal **vereinbarte Vergütung** mit Betrag, einmal **vollständiger
Verzicht**.
*Prüft:* die dritte Gruppe im Antrag, die sonst immer gleich aussieht.

### K11 — Gegenleistung
Einmal **hängt von einer Gegenleistung ab, diese ist erbracht**, einmal **hängt nicht davon ab**.
*Prüft:* eine Pflichtangabe, deren falsche Belegung erst bei Gericht auffällt.

### K12 — Ohne Kennziffer
Dieselbe Sache wie K1, aber die **Kennziffer leer** lassen.
*Prüft:* den Zweig, in dem die Anschrift des Bevollmächtigten ausgeschrieben wird statt durch die
Kennziffer ersetzt zu werden. Bitte vorher fragen, ob das Gericht das im Test annimmt.

---

## Was die Oberfläche nicht kann

**Sammeldateien.** Erzeugt wird eine Datei je Mahnsache. Eine Datei mit mehreren Anträgen — die den
Zähler im Nachlaufsatz prüfen würde — entsteht über die Oberfläche nicht.

**Vollstreckungsbescheid.** Das Gericht testet nur Mahnbescheid-Anträge; der VB-Antrag bleibt außen
vor.

---

## Einreichen und auswerten

Zu jeder Datei eine Zeile mitschicken — das ersetzt Rückfragen:

| Datei | Konstellation | Besonderheit |
|---|---|---|
| `TST001` | K1 | Grundfall, Kaufvertrag, Zinsen über Basiszins |
| `TST002` | K2 | Gläubiger GmbH mit Geschäftsführer |
| … | | |

**Die Rückläufe sind die zweite Hälfte des Tests.** Bitten Sie um Kostennachricht und Monierung im
EDA-Format zu **jeder** eingereichten Datei — auch zu den beanstandungsfreien. Diese Dateien in der
Akte ablegen und über **Gerichtsnachricht einlesen** verarbeiten: unser Lesepfad ist bisher nur
gegen die Beispieldateien von online-mahnantrag.de geprüft, also gegen unsere eigene Quelle. Echte
Gerichtsnachrichten sind der erste unabhängige Beleg, dass er stimmt.

Was dabei auffällt, gehört in `openspec/changes/add-dunning-and-enforcement/tasks.md` unter 0.5 —
mit der Konstellation, nicht nur mit dem Fehlertext.
