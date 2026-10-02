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

Die Dateien tragen die Namen, unter denen das BMJ sie veröffentlicht. Drei `Hinweisblatt`-Dateien
sind Merkblätter zum Ausfüllen und tragen keine Formularfelder.

**Die Fassung steht im Verzeichnisnamen, nicht im Dateinamen.** Unter
`src/main/resources/zvfv/` liegt je Fassung ein Verzeichnis — `2024-09-01/`, `2026-10-01/` — mit den
PDFs und dem Unterverzeichnis `mapping/`; die Feldverzeichnisse hier unter `felder/` sind genauso
gegliedert.

Das war nicht die erste Absicht. Der ursprüngliche Plan war, das Präfix `20240901` des Herausgebers
als Fassungsangabe zu lesen. Zum 01.10.2026 hat das BMJ die geänderten Formulare aber unter **genau
denselben Dateinamen** veröffentlicht, weiterhin datiert auf 20240901. Der Name sagt seither nichts
über die Fassung, und wer sie daraus ableitet, liefert das falsche Formular aus. Belegen lässt sich
die Ablösung an den Dokumenten selbst: unveränderter Titel und unverändertes Erstellungsdatum, aber
Änderungsdatum **09.06.2026**.

Unter `felder/` liegt zu jedem Formular ein Verzeichnis seiner AcroForm-Felder, erzeugt mit der
PDFBox-Fassung, die auch der Server benutzt. Spalten: Seite, technischer Name, Art, zulässige Werte,
Bezeichnung.

**Warum das Verzeichnis gebraucht wird.** Die technischen Namen sind nichtssagend — `Textfeld 353`,
`Kontrollkästchen 3036`. Was ein Feld bedeutet, steht allein in seinem Tooltip, und der ist die
letzte Spalte. Erfreulicherweise trägt **jedes** Feld aller acht Formulare einen.

**On-States.** In der Fassung 2024-09-01 hatten alle 663 Ankreuzfelder denselben On-State `Ja`. Der
Filler liest ihn trotzdem aus der Datei, statt ihn festzuschreiben — und das hat sich bereits
ausgezahlt: die Fassung 2026-10-01 führt 660 Felder mit `Ja` und **fünf mit `Yes`**, nämlich die
fünf, die im Vollstreckungsauftrag neu hinzugekommen sind. Wäre `Ja` festgeschrieben, blieben diese
fünf Kästchen leer, während das gedruckte Formular angekreuzt aussieht.

## Die Zuordnung zu den Anlagen der ZVFV

Die Feldzahl ist für 2024-09-01 angegeben; wo die Fassung 2026-10-01 abweicht, steht sie dahinter.

| Datei | Anlage | Felder | Was es ist |
|---|---|---|---|
| `20240901_Vollstreckungsauftrag-Gerichtsvollzieher.pdf` | 1 | 350 / 352 | Vollstreckungsauftrag an Gerichtsvollzieher — mit den Optionen des § 802a Abs. 2 ZPO (Sachpfändung, gütliche Erledigung, Vermögensauskunft, Haftbefehl, Zustellung) |
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

Bitte die alte Fassung nicht ersetzen, sondern die neue in ein **neues Verzeichnis** danebenlegen.
Eine Maßnahme, die unter der alten Fassung erzeugt wurde, war nicht falsch, sie war aktuell, und
muss nachvollziehbar bleiben.

1. Die **ausfüllbaren** PDFs beim BMJ holen (nicht den Abdruck aus dem Bundesgesetzblatt — der ist
   ein Bild und trägt keine Felder) und nach `src/main/resources/zvfv/<JJJJ-MM-TT>/` legen, `mapping/`
   dazu. Das Datum ist das **Inkrafttreten**, nicht das Dateidatum des Herausgebers.
2. Das Feldverzeichnis erzeugen und gegen die alte Fassung stellen:

   ```
   ZvfvFieldIndexTool write 2026-10-01
   ZvfvFieldIndexTool diff  2024-09-01 2026-10-01
   ```

   Der Vergleich entscheidet über den ganzen weiteren Aufwand. Er nennt getrennt, was **entfallen**,
   was **neu** und was **umbenannt** ist — Letzteres sind die Felder, die ihren Namen behalten und
   ihre Bedeutung geändert haben, und nur die machen Arbeit.
3. `EnforcementFormPackage`: Konstante für die Fassung, `getVersions()`, `getCurrentVersion()`,
   `getValidFrom`/`getValidTo` fortschreiben — die abgelöste Fassung endet am Tag vor dem Beginn der
   neuen.
4. Migration: `valid_to` der abgelösten Fassung setzen, damit der Fassungswähler sie nicht weiterhin
   für die geltende hält (Muster: `V3_6_0_52__ZvfvFormVersion20261001.sql`).
5. Das Zuordnungsprofil übernehmen. Im Normalfall genügt Kopieren; wo der Vergleich etwas gemeldet
   hat, sind genau diese Felder einzeln anzusehen. `Anlage1MappingTest` prüft anschließend jede
   Bezeichnung gegen das Formular.
6. Ein Muster füllen und **ansehen**. Kein Test beweist, dass ein Feld bedeutet, was sein Tooltip
   sagt.

### Und in einer Kanzlei

Dort steht dasselbe im Verwaltungsdialog zur Verfügung, ohne auf eine Auslieferung zu warten:
*Einstellungen → Zwangsvollstreckung → Formulare* → Vorlage der neuen Fassung anlegen →
**„Von Fassung übernehmen"**. Die Aktion kopiert die Zuordnung der gewählten älteren Fassung und
lässt dabei stehen, was nicht zu übernehmen ist: Felder, die es nicht mehr gibt, und Felder, deren
Bedeutung sich geändert hat — jedes mit Namen und mit beiden Bezeichnungen. Was übrigbleibt, trägt
die Kanzlei selbst nach.

### Worauf der Vergleich zu achten ist

Der Übergang 2024-09-01 → 2026-10-01 ist das Lehrstück dafür, dass es nicht genügt, Feldnamen zu
vergleichen. **Die Namen blieben, die Bedeutungen wanderten.** Im Antrag auf PfÜB:

| Feld | 2024-09-01 | 2026-10-01 |
|---|---|---|
| `Kontrollkästchen 46` | Versand Titel erst nach Mitteilung des Az. | Elektronische Dokumente sind beigefügt |
| `Kontrollkästchen 45` | Versand gleichzeitig mit Titel | Versand nach Mitteilung des Az. |
| `Kontrollkästchen 44` | Versand als elektronisches Dokument | Gleichzeitige Übersendung auf dem Postweg |

Im Vollstreckungsauftrag dasselbe Muster (`Kontrollkästchen 474` Vermögensverzeichnis → bedingter
Pfändungsauftrag, `486` Rentenversicherung → Anschrift/Aufenthaltsort), dazu drei entfallene und
fünf neue Felder (350 → 352) und korrigierte Zitate (§ 753a S. 1 → § 752a Abs. 1, § 754a Abs. 1 S. 1
Nr. 4 → § 754a Abs. 3 Nr. 1 und 2).

Deshalb führt jedes Zuordnungsprofil die **Bezeichnung** jedes Feldes mit, und `Anlage1MappingTest`
vergleicht sie gegen die des Formulars. Ein Profil, das auf dieselben Namen zeigt, aber die alten
Bedeutungen meint, fällt dadurch im Build auf und nicht erst bei einer Einreichung.

Beim Übergang 2024-09-01 → 2026-10-01 trugen die Zuordnungen von Anlage 1 (47 Felder) und Anlage 6
(131 Felder) unverändert: kein zugeordnetes Feld war verschwunden, keine Bezeichnung hatte sich
geändert. Alle Verschiebungen lagen in Feldern, die wir nicht füllen.
