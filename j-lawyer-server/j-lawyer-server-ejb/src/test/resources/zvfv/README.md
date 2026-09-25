# Amtliche Formulare der Zwangsvollstreckung (ZVFV)

Hier liegen die ausfüllbaren PDF-Formulare der *Verordnung über Formulare für die
Zwangsvollstreckung* (ZVFV 2022). Sie sind die Grundlage für zweierlei: die Feldzuordnung der
Formularerzeugung (4.2, 4.3) und die Tests, die prüfen, dass jedes Feld getroffen wird (4.10).

**Warum echte Dateien und keine Annahmen.** Ein AcroForm-Feld wird über seinen Namen angesprochen,
und die Namen stehen nirgends außer in der Datei selbst. Ankreuzfelder haben zusätzlich einen
*On-State* — den Wert, der „angekreuzt" bedeutet —, der je Formular und je Feld anders heißen kann
(`/Ja`, `/1`, `/On`, …). Wer ihn rät, erzeugt ein Formular, das gedruckt richtig aussieht und
maschinell leer ist. Dasselbe Argument wie bei den EDA-Referenzdateien unter
`src/test/resources/eda/reference`.

## Woher

Die Formulare sind Bestandteil einer Rechtsverordnung und damit amtliche Werke nach § 5 Abs. 1
UrhG — frei verwendbar und hier ablegbar.

Die Verordnung samt Anlagen: <https://www.gesetze-im-internet.de/zvfv_2022/>
Die ausfüllbaren Fassungen stellt das Bundesministerium der Justiz bereit (bmjv.de). Unter
justiz.de waren sie nicht auffindbar.

**Wichtig: die ausfüllbare Fassung, nicht der Abdruck aus dem Bundesgesetzblatt.** Der Abdruck ist
ein Bild des Formulars und trägt keine Formularfelder; für uns ist er wertlos.

## Was hier liegt

Die Dateien tragen die Namen, unter denen das BMJ sie veröffentlicht; das Präfix `20240901` ist der
Stand, ab dem die Fassung gilt. Drei `Hinweisblatt`-Dateien sind Merkblätter zum Ausfüllen und
tragen keine Formularfelder.

Unter `felder/` liegt zu jedem Formular ein Verzeichnis seiner AcroForm-Felder, erzeugt mit der
PDFBox-Fassung, die auch der Server benutzt. Spalten: Seite, technischer Name, Art, zulässige Werte,
Bezeichnung.

**Warum das Verzeichnis gebraucht wird.** Die technischen Namen sind nichtssagend — `Textfeld 353`,
`Kontrollkästchen 3036`. Was ein Feld bedeutet, steht allein in seinem Tooltip, und der ist die
letzte Spalte. Erfreulicherweise trägt **jedes** Feld aller acht Formulare einen.

**On-States.** Alle 663 Ankreuzfelder der acht Formulare haben denselben On-State `Ja`. Der Filler
liest ihn trotzdem aus der Datei, statt ihn festzuschreiben: eine spätere Fassung darf ihn ändern,
und ein falscher On-State erzeugt ein Formular, das gedruckt angekreuzt aussieht und maschinell
leer ist.

## Die Zuordnung zu den Anlagen der ZVFV

| Datei | Anlage | Felder | Was es ist |
|---|---|---|---|
| `20240901_Vollstreckungsauftrag-Gerichtsvollzieher.pdf` | 1 | 350 | Vollstreckungsauftrag an Gerichtsvollzieher — mit den Optionen des § 802a Abs. 2 ZPO (Sachpfändung, gütliche Erledigung, Vermögensauskunft, Haftbefehl, Zustellung) |
| `20240901_Antrag_Durchsuchungsanordnung.pdf` | 2 | 47 | Antrag auf richterliche Durchsuchungsanordnung, auch für Nachtzeit sowie Sonn- und Feiertage |
| `20240901_Entwurf_Durchsuchungsanordnung.pdf` | 3 | 243 | Entwurf der Anordnung, den das Gericht übernimmt |
| `20240901_Antrag_Pfaendungsbeschluss.pdf` | 4 | 84 | Antrag auf Pfändungsbeschluss und Pfändungs- und Überweisungsbeschluss |
| `20240901_Entwurf_Pfaendungsbeschluss.pdf` | 5 | 411 | Entwurf des Beschlusses |
| `20240901_Forderungsaufstellung_Gerichtsvollzieher.pdf` | 6 | 254 | Forderungsaufstellung zum Vollstreckungsauftrag |
| `20240901_ForderungsaufstK_eUnterhaltsansprueche.pdf` | 7 | 222 | Forderungsaufstellung zum PfÜB, ohne gesetzliche Unterhaltsansprüche |
| `20240901_Forderungsaufstellg_Unterhaltsansprueche.pdf` | 8 | 283 | Forderungsaufstellung zum PfÜB bei gesetzlichen Unterhaltsansprüchen |

Anlage 4 ist der Antrag, Anlage 5 der Entwurf, den das Gericht als Beschluss übernimmt — deshalb hat
der Entwurf fünfmal so viele Felder wie der Antrag. Beide werden gebraucht; das Gericht erwartet den
Entwurf mitgeliefert.

Die Anlagen **1, 4, 5, 6 und 7** sind die dringendsten: damit lassen sich Gerichtsvollzieherauftrag
und Forderungspfändung vollständig bauen, und das sind die Maßnahmen, die eine Kanzlei täglich
braucht.

## Wenn eine Fassung abgelöst wird

Bitte die alte Datei nicht ersetzen, sondern die neue danebenlegen — das Präfix trägt den Stand
ohnehin. Eine Maßnahme, die unter der alten Fassung erzeugt wurde, war nicht falsch, sie war
aktuell, und muss nachvollziehbar bleiben. Das Feldverzeichnis unter `felder/` wird dann für die
neue Datei neu erzeugt; ein Vergleich der beiden Textdateien zeigt sofort, was sich geändert hat.
