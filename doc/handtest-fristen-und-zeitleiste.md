# Handtest: Drittschuldner, Fristen und Zeitleiste

Drei neue Masken und eine Änderung, die über sie hinausreicht. Was noch nicht geht, steht am Ende.

## Vorbereitung

**Bauen und deployen.** Neue Migrationen sind diesmal **nicht** dabei — `V3_6_0_47`
(Drittschuldner) und `V3_6_0_48` (Fristen, Maßnahmedokumente, Merkmal *Vermögensauskunft*) sind auf
Ihrer Installation bereits gelaufen. Es geht allein um neuen Code in EAR und Client.

**Ein Forderungskonto mit Titel und mindestens einer Maßnahme** wird gebraucht. Die vier Maßnahmen
aus dem letzten Durchgang genügen; drei davon tragen schon ein Ergebnis.

---

## Teil A — Drittschuldner (§ 840 ZPO)

Forderungskonto → Reiter **Zwangsvollstreckung** → Maßnahme markieren → **Drittschuldner**.

**A0 Die Spalte in der Maßnahmenliste.** Nach dem Anlegen eines Drittschuldners (A1) steht in der
Maßnahmentabelle in der Spalte **Drittschuldner** ein Zeichen. Ohne es müsste man jede Zeile
einzeln öffnen, um zu sehen, ob eine Bank oder ein Arbeitgeber dahintersteht.

**A1 Anlegen ohne Kontakt.** „Neu", Bezeichnung *Musterbank AG*, Anschrift zweizeilig, Gepfändet
*Kontoguthaben*, Detail eine IBAN. Speichern.
→ Zeile in der Liste, Spalte *Erklärung am* leer.

**A2 Anlegen mit Kontakt.** „Neu", im Feld *Kontakt* einen Beteiligten der Akte wählen.
→ Bezeichnung und Anschrift werden **einmalig** gefüllt. Danach die Bezeichnung ändern, einen
anderen Kontakt wählen: die geänderte Bezeichnung darf **nicht** überschrieben werden.

**A3 Zustellung und Frist.** Bei A1 *Zustellung* auf heute setzen, speichern.
→ Spalte *Erklärung bis* zeigt **heute + 14 Tage**, Stand „offen"; neben dem Feld steht „Erklärung
fällig bis …". In der Akte, Reiter **Kalender**, steht eine neue Wiedervorlage zu diesem Datum — und
zwar **ohne die Akte neu zu laden** (siehe Teil D).

**A4 Überfällig.** Bei A2 *Zustellung* auf ein Datum vor drei Wochen setzen, speichern.
→ neben dem Feld „… **überfällig** (§ 840 Abs. 2 S. 2 ZPO)".

**A5 Erklärung erfassen.** Zeile A3 markieren → **Erklärung erfassen**, Datum bestätigen, einen
Text eingeben.
→ Spalte *Erklärung am* gefüllt, der eingegebene Text steht im Feld **Erklärung** unter dem Datum;
die Wiedervorlage in der Akte ist **erledigt** — und zwar **sofort**, ohne die Akte neu zu laden.

**A5b Erklärung nachtragen.** Das Feld *Erklärung* bei einer anderen Zeile füllen und speichern,
dann die Zeile wechseln und zurückkommen.
→ Der Text steht noch da. (Er wird auch beim Knopf *Erklärung erfassen* als Vorschlag angeboten.)

**A6 Datum direkt eintragen.** Bei A4 das Feld *Erklärung am* füllen und speichern.
→ derselbe Effekt wie A5. Danach das Feld leeren und erneut speichern: es erscheint der Hinweis,
dass der Eingang bestehen bleibt — und er bleibt bestehen.

**A7 Zahlung buchen.** Zeile markieren → **Zahlung buchen**, Betrag *250,00*, Datum heute.
→ Hinweis „Gebucht: 250,00 EUR von … verteilt auf *n* Buchung(en)". Im Reiter **Buchungen** stehen
die Teilbuchungen mit dem Namen des Drittschuldners; **Summen** und der Kopf sind kleiner geworden.
*Das ist der Punkt:* eine Zahlung des Drittschuldners läuft durch dieselbe Tilgungsreihenfolge wie
jede andere (im gesetzlichen Fall §§ 366, 367 BGB).

**A8 Entfernen.** Einen Drittschuldner entfernen.
→ Rückfrage nennt, dass die Wiedervorlage mitgeht und gebuchte Zahlungen bleiben. Danach: Zahlung
noch da, Wiedervorlage in der Akte erledigt.

---

## Teil B — Fristen der Maßnahmen und der Reiter „Fristen & Dokumente"

**B1 Das Absendedatum.** Reiter **Zwangsvollstreckung** → eine **offene** Maßnahme (die
Vollstreckungsandrohung oder die Einwohnermeldeamtsanfrage) → **Ändern**.
→ Es gibt das neue Feld **Absendung**. Solange es leer ist, steht **rechts daneben** „ohne
Absendedatum keine Wiedervorlage zum Sachstand". Datum eintragen: dort steht dann „Wiedervorlage
*n* Tage danach" (Vollstreckungsandrohung 14, Auskünfte 21, PfÜB 30, Gerichtsvollzieher 42).
Speichern.

**B2 Der neue Reiter.** Forderungskonto → **Fristen & Dokumente**.
→ Eine Zeile *Sachstand der Maßnahme* mit dem errechneten Termin, Stand **offen**. Fällt der Termin
auf Samstag oder Sonntag, steht der Montag da.

**B3 Rückwirkend für den Bestand.** Knopf **Fristen neu berechnen**.
→ Der Hinweis unten sagt, was angelegt wurde — oder dass alles auf dem Stand war. Maßnahmen ohne
Absendedatum bekommen **nichts**, und Maßnahmen mit Ergebnis ebenfalls nicht: es ist nichts mehr zu
erfragen.

**B4 Das Ergebnis schließt die Frist.** Bei der Maßnahme aus B1 → **Ergebnis** → *fruchtlos*, Datum
heute.
→ Im Reiter *Fristen & Dokumente* steht die Zeile jetzt als **erledigt – Das Ergebnis der Maßnahme
ist erfasst: fruchtlos am …**. Die Wiedervorlage in der Akte ist ebenfalls erledigt.

**B5 § 802d ZPO — der eigentliche Gewinn.** Neue Maßnahme der Art **Abnahme der Vermögensauskunft
(§ 802c ZPO)** anlegen, Absendung heute, dann **Ergebnis** → *fruchtlos*, Datum heute.
→ Es entsteht **automatisch** eine zweite Zeile: *Erneute Vermögensauskunft möglich*, Termin
**heute + 2 Jahre**. Diese Frist ist der Grund, warum eine scheinbar tote Akte in zwei Jahren wieder
aufgemacht wird.

**B6 Zurückgenommen sperrt nicht.** Dieselbe Maßnahme → **Ergebnis** → *zurückgenommen*.
→ Nach **Fristen neu berechnen** ist die § 802d-Zeile **entfallen**: abgegeben wurde nichts, also
sperrt auch nichts.

**B7 Frist von Hand erledigen.** Eine offene Zeile markieren → **Erledigt**, Grund eingeben.
→ Stand „erledigt – <Ihr Grund>", Wiedervorlage geschlossen.

**B8 Dokumente.** Bei einer Maßnahme mit Formular **Formulare erzeugen**.
→ Die untere Tabelle zeigt Datum, Dokumentname, Formular mit Fassung und die Maßnahme — **ohne**
dass „Neu laden" gedrückt werden muss. **Vorher** erzeugte Dokumente erscheinen dort nicht; die
Verknüpfung entsteht erst beim Erzeugen.

---

## Teil C — Zeitleiste im Kopf

Sie steht im Kopf des Forderungskontos unter *Beschreibung* und wird bei jedem Öffnen neu ermittelt.

**C1 Sechs Stationen.** Offen · Gemahnt · Mahnbescheid · Tituliert · Vollstreckung · Erledigt.
Erreichte Punkte sind grün gefüllt, der aktuelle trägt einen weißen Ring, kommende sind blass.
Darunter das Datum, dann eine Zeile zum Stand und eine zur nächsten Frist.

**C2 Rückschluss.** Bei einem Konto mit Titel, aber ohne erfasste Mahnung: *Gemahnt* und
*Mahnbescheid* stehen trotzdem als erreicht da. Ein Titel beweist den Weg dorthin.

**C3 Nächste Frist.** Nach B1 muss unten „nächste Frist: … – Sachstand der Maßnahme" stehen. Liegt
ein Termin in der Vergangenheit, steht dort **„überfällig seit …"** in Rot.

**C4 Abweichung Widerspruch.** Bei einer Mahnsache mit Status *Widerspruch eingelegt*:
→ statt der Standzeile steht in Rot „streitig: Widerspruch eingelegt – das Mahnverfahren wird nur
auf Antrag fortgeführt (§ 696 Abs. 1 ZPO)".

**C5 Abweichung Wartezeit.** Nach B5:
→ „wartet bis <heute+2 Jahre>: eine erneute Vermögensauskunft ist erst dann zu verlangen
(§ 802d ZPO)".

**C6 Erledigt.** Ein Konto ohne offenen Betrag steht auf *Erledigt*.

**C7 Mauszeiger auf die Leiste.** Der Tooltip zählt alle sechs Stationen mit Datum und Erläuterung
auf — für den Fall, dass die Punkte allein nicht genügen.

---

## Teil D — Der Akten-Reiter „Kalender" (reicht über die Vollstreckung hinaus)

Das ist die einzige Änderung, die **jede** Akte betrifft: die Tabelle im Reiter *Kalender* zeigt
Änderungen jetzt sofort, statt erst nach dem Schließen und Öffnen der Akte. Eine Zeile wird dabei
**aktualisiert** und nicht verdoppelt — und eine **gelöschte** darf nicht zurückkehren.

Zu prüfen ist an diesen Stellen, jeweils mit **offener** Akte im Hintergrund:

| Nr. | Wo | Was | Erwartung in der Akte |
|---|---|---|---|
| D1 | Akte → Kalender, Kontextmenü | Wiedervorlage **auf erledigt setzen** | Häkchen erscheint sofort, **keine** zweite Zeile |
| D2 | dito | **auf offen setzen** | Häkchen verschwindet |
| D3 | dito | Klick auf das Häkchen in der Tabelle | wie D1 |
| D4 | dito | **vertagen** (verschieben) | neues Datum in derselben Zeile |
| D5 | dito | **löschen** | Zeile weg — und sie darf **nicht wieder auftauchen** |
| D6 | dito | **in andere Akte verschieben** | hier weg; ist die Zielakte offen, erscheint sie dort |
| D7 | Kalenderansicht | Termin mit der Maus verschieben | neues Datum in der Akte |
| D8 | Kalender | Wiedervorlage **bearbeiten / duplizieren** | Änderung bzw. neue Zeile |
| D9 | Desktop | „heute fällig" → Eintrag erledigen | Häkchen in der Akte |
| D10 | Akte archivieren | „offene Kalendereinträge schließen" bestätigen | Einträge stehen als erledigt |
| D11 | Assistent | Wiedervorlage anlegen und ändern lassen | Zeile erscheint / ändert sich |
| D12 | Forderungskonto → Titel | Verjährungs-Wiedervorlage setzen | Zeile erscheint |
| D13 | Forderungskonto → Mahnungen | Mahnfrist erzeugen | Zeile erscheint |
| D14 | Vollstreckung | alles aus Teil A und B | Zeilen erscheinen und schließen sich |

**D5 ist der wichtigste Punkt.** Das Löschen meldet dem Desktop eine *Änderung* — damit er seine
Liste der heute fälligen neu lädt —, und genau diese Meldung darf die gelöschte Zeile nicht wieder
in die Akte schreiben. Der Fall trifft nur Wiedervorlagen, deren Zeitraum **heute** enthält; zum
Prüfen also eine auf heute datierte löschen.

---

## Was noch nicht geht

- **Keine Stammdatenmaske für Maßnahmearten.** Wiedervorlagetage und das Merkmal
  *Vermögensauskunft* sind nur über die Datenbank änderbar.
- **Fristen nicht über REST.** `EnforcementEndpointV8` kennt Maßnahmen, aber keine Fristen.
- **Dokumente rückwirkend.** Vor diesem Stand erzeugte Formulare lassen sich der Maßnahme nicht
  mehr zuordnen; eine Zuordnung über den Dateinamen wäre geraten, und eine falsch zugeordnete
  Urkunde ist schlimmer als eine fehlende.
- **Ereignisse nur im eigenen Client.** Legt ein Kollege eine Frist an, sehen Sie sie erst beim
  Öffnen der Akte.
- **Offene Frage 4.6a** (Gegenstandswert der Vollstreckungsgebühr) ist unverändert offen.
