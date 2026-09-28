# Manuelle Tests 11.3 – Dokument-Metadaten und Dokumentenansichten (Desktop)

Diese Anleitung deckt die Punkte aus Task 11.3 ab, die nur im Desktop-Client geprüft werden
können. Serverseitiges Verhalten ist bereits automatisiert geprüft (`verify_rest.py`, 45
Prüfungen, sowie die Unit-Tests aus 2.5). Die Abschnitte bauen aufeinander auf; Dauer insgesamt
etwa 60–90 Minuten.

Ergebnis je Schritt abhaken. Bei einer Abweichung bitte notieren: Schritt-Nummer, was passiert
ist, ggf. Screenshot und die Uhrzeit (für `server.log`).

---

## 0. Vorbereitung

**Stand:**
- [ ] Server mit dem aktuellen EAR deployt (enthält `DocumentHierarchy` aus 2.5), Client aus
      demselben Stand gestartet.
- [ ] Web-Client mit `-Pweb` gebaut und deployt (nur für Abschnitt 12).

**Testdaten** (einmalig anlegen, am Ende wieder löschen):
- Kontakt **„Vera Verifikation“** mit E-Mail-Adresse `vera@example.org`.
- Akte **„ZZ Test Metadaten A“** mit Vera als Beteiligte (Typ z. B. „Gegner“).
- Akte **„ZZ Test Metadaten B“** (leer, Ziel für Kopieren/Verschieben).
- In Akte A fünf Dokumente per Drag & Drop hochladen, z. B.:
  `Mail.pdf`, `Anlage1.pdf`, `Anlage2.pdf`, `Vertrag.docx` (mehrere Seiten Text),
  `Scan.pdf` (gescanntes PDF ohne Textebene).
- Mindestens eine **Dokument-Etikette** (boolesch, z. B. „Wichtig“) und eine **Listen-
  etikette** (z. B. „Dokumentstatus“ mit Werten „Offen“, „Geprüft“) konfiguriert.
- **Ingo/Assistent:** mindestens ein eigener Prompt vom Typ **extract** (z. B.
  „Schlagworte vorschlagen“: *„Nenne 5 Schlagworte zu diesem Dokument, durch Komma
  getrennt.“*) und einer vom Typ **chat**; zusätzlich ein Prompt eines anderen Typs (z. B.
  Zusammenfassung) als Gegenprobe.
- Für Abschnitt 1: ein **zweiter Arbeitsplatz** (zweiter Rechner oder zweite Client-
  Installation mit eigenem lokalen Profil), derselbe Benutzer.

---

## 1. Ansichten umschalten und Einstellungen am zweiten Arbeitsplatz

| # | Schritt | Erwartet |
|---|---------|----------|
| 1.1 | Akte A öffnen, Reiter Dokumente. | Listenansicht mit Ordnern links, Liste in der Mitte, rechts Reiter **Vorschau / Details / Nachrichten**. Kein separates Panel „Dokument-Etiketten“ unter der Liste. |
| 1.2 | Drei Dokumente markieren (Strg+Klick), Ordnerauswahl ändern (nur Wurzel), Schnellfilter „Anlage“ eingeben. Dann auf **Tabellenansicht** umschalten. | Dieselben Dokumente sind markiert, Ordnerauswahl und Filter unverändert. Der Reiterbereich rechts ist ausgeblendet, die Tabelle nutzt die volle Breite. |
| 1.3 | Zurück zur **Listenansicht**. | Auswahl und Filter unverändert; der Teiler zur Vorschau steht an derselben Position wie vor 1.2 (springt **nicht** nach ganz rechts). |
| 1.4 | Tabellenansicht: per Rechtsklick auf den Spaltenkopf die Spalte **„Größe“** ausblenden, **„Von/An“** an die erste Stelle ziehen, Breite von **„Bezeichnung“** ändern. | Änderungen sofort sichtbar. |
| 1.5 | Akte schließen, eine andere Akte öffnen. | Tabellenansicht bleibt aktiv, Spalten wie in 1.4. |
| 1.6 | Client beenden, am **zweiten Arbeitsplatz** mit demselben Benutzer anmelden, eine Akte öffnen. | Tabellenansicht aktiv, „Größe“ ausgeblendet, „Von/An“ vorn, Breite übernommen (Einstellung liegt serverseitig). |
| 1.7 | Am zweiten Arbeitsplatz auf Listenansicht zurückschalten, am ersten Arbeitsplatz neu anmelden. | Listenansicht aktiv. |

---

## 2. Anzeige in Liste und Tabelle

Setzt Metadaten aus Abschnitt 3 voraus – ggf. nach Abschnitt 3 ausführen.

| # | Schritt | Erwartet |
|---|---------|----------|
| 2.1 | Listenansicht, Dokument mit Bezeichnung. | Erste Zeile zeigt die **Bezeichnung** statt des Dateinamens; zweite Zeile (grau) Von/An mit ↘ (eingehend) bzw. ↗ (ausgehend), Eingang, Dateiname, Größe. |
| 2.2 | Dokument ohne Bezeichnung. | Erste Zeile zeigt den Dateinamen. |
| 2.3 | Dokument mit aktiver Etikette „Wichtig“, „Dokumentstatus = Geprüft“ und Schlagworten. | Chips **„Wichtig“** und **„Dokumentstatus: Geprüft“** vor den Schlagwort-Chips; inaktive Etiketten erscheinen nicht. |
| 2.4 | Tabellenansicht: Spalten durchsehen. | U. a. Markierung 1/2, Favorit, Typ, **Vorschau** (Auge), Bezeichnung, Dateiname, Von/An, Eingang, Geändert, Erstellt, Schlagworte, **Etiketten** (standardmäßig sichtbar), Ordner, Größe, Diktatzeichen, Rechnung, Status. |
| 2.5 | Klick auf Spaltenkopf **„Eingang“**, zweiter Klick. | Sortierung nach Eingang, dann umgekehrt; Anlagen bleiben unter ihrem Eltern-Dokument. |
| 2.6 | Klick auf das **Auge** in der Spalte Vorschau. | Modales Fenster mit derselben Vorschau wie im Vorschau-Reiter der Liste. Doppelklick auf die Zeile öffnet dagegen die externe Anwendung. |
| 2.7 | Dokument mit zwei Markierungsfarben. | Beide Farben in den Spalten 1 und 2 sichtbar (nicht nur eine). |

---

## 3. Details-Reiter, Eigenschaften-Dialog und KI

| # | Schritt | Erwartet |
|---|---------|----------|
| 3.1 | Listenansicht, `Vertrag.docx` wählen, Reiter **Details**. | Felder Bezeichnung, Schlagworte, Eingang, Von/An, übergeordnetes Dokument, Überschrift **„Etiketten“** (fett, ohne Rahmen, Etiketten umbrechen). Der Reiter ist nicht breiter als der Bereich (kein horizontales Scrollen). |
| 3.2 | Bezeichnung „Kaufvertrag Müller“, Schlagworte „ Frist , frist, Kaufpreis“, Eingang heute, Von/An = Kontakt Vera (eingehend), Etikette „Wichtig“ aktivieren. Speichern. | Liste zeigt „Kaufvertrag Müller“, ↘ Vera, Schlagworte **„Frist, Kaufpreis“** (Duplikat entfernt), Chip „Wichtig“. |
| 3.3 | Von/An auf Freitext „RA Schulze“ (ausgehend) ändern, speichern. | ↗ RA Schulze; kein Kontaktbezug mehr. |
| 3.4 | **KI-Button** neben Schlagworte anklicken. | Menü enthält die chat- und extract-Prompts (durch Trenner getrennt), **nicht** den Zusammenfassungs-Prompt. |
| 3.5 | Extract-Prompt wählen. | Fortschrittsanzeige, danach Dialog mit Vorschlägen als Checkboxen; vorhandene Schlagworte ausgegraut; Knöpfe „alle“/„keine“ vorhanden. Abbrechen möglich. |
| 3.6 | Zwei Vorschläge übernehmen. | Sie stehen im Eingabefeld, sind aber **noch nicht gespeichert** (Akte neu laden → nicht vorhanden). Erst nach Speichern dauerhaft. |
| 3.7 | **KI-Button** hinter Bezeichnung, Prompt wählen. | Ein Titelvorschlag wird zur Prüfung angezeigt; nach Übernahme steht er im Feld, ohne zu speichern. |
| 3.8 | Gegenprobe: alle chat/extract-Prompts deaktivieren (oder Benutzer ohne Assistent). | KI-Buttons deaktiviert, Tooltip erklärt warum. Danach Prompts wieder aktivieren. |
| 3.9 | Tabellenansicht, Zeile wählen, **F2** in der Zelle Schlagworte, „Termin“ ergänzen, Enter. | Gespeichert und normalisiert; der KI-Button steht auch im Inline-Editor zur Verfügung. Doppelklick startet keine Bearbeitung (öffnet das Dokument). |
| 3.10 | F2 in der Zelle Bezeichnung, ändern, Enter. | Gespeichert. |
| 3.11 | Rechtsklick auf ein Dokument → **„Eigenschaften...“**. | Dialog mit denselben Feldern wie Details inkl. Etiketten und KI-Buttons. |

---

## 4. Stapelbearbeitung

| # | Schritt | Erwartet |
|---|---------|----------|
| 4.1 | Vier Dokumente markieren → **„Eigenschaften...“**. | Stapeldialog: jedes Feld hat eine Aktivierungs-Checkbox; Schlagworte mit hinzufügen/entfernen/ersetzen. |
| 4.2 | Nur **Von/An** aktivieren: Kontakt Vera, eingehend. Speichern. | Alle vier zeigen ↘ Vera; Bezeichnungen, Schlagworte und Eingang unverändert. |
| 4.3 | Schlagworte **hinzufügen** „Beweis“. | Alle vier tragen zusätzlich „Beweis“, bestehende bleiben. |
| 4.4 | Schlagworte **entfernen** „beweis“ (klein geschrieben). | Bei allen entfernt (Groß-/Kleinschreibung egal). |
| 4.5 | Etikette „Wichtig“ im Stapeldialog setzen. | Alle vier tragen den Chip „Wichtig“. |
| 4.6 | KI-Button im Stapeldialog, extract-Prompt. | Prompt läuft je Dokument (Fortschritt „n/4“), Vorschläge je Dokument; übernommene werden beim Speichern je Dokument ergänzt. |

---

## 5. Hierarchie per Drag & Drop, Löschen und Wiederherstellen

| # | Schritt | Erwartet |
|---|---------|----------|
| 5.1 | `Anlage1.pdf` und `Anlage2.pdf` markieren und auf `Mail.pdf` ziehen. | Beim Ziehen ist das Zieldokument blau umrandet. Danach stehen beide eingerückt unter Mail; Mail zeigt ein **grünes Badge „2“** vor der Bezeichnung. Die Einrückung beginnt dort, wo der Text des Eltern-Dokuments beginnt. |
| 5.2 | Klick auf das Badge bzw. den Aufklapp-Pfeil. | Anlagen klappen zu und wieder auf. Gleiches Verhalten in der Tabellenansicht. |
| 5.3 | `Mail.pdf` auf `Anlage1.pdf` ziehen. | Abgelehnt mit Hinweis (Zyklus), nichts geändert. |
| 5.4 | Gegenprobe: Dokumente auf einen **Ordner** ziehen; ein Dokument in den Datei-Manager ziehen; eine Datei vom Desktop auf ein **Dokument** ziehen. | Verschieben in den Ordner, Export als Datei bzw. Upload funktionieren wie bisher. |
| 5.5 | `Anlage1.pdf` in Ordner „Beweise“ verschieben, nur diesen Ordner anzeigen. | Anlage1 steht oben mit Hinweis „Anlage zu <Bezeichnung von Mail>“. |
| 5.6 | Alle Ordner anzeigen, `Mail.pdf` löschen (Papierkorb). | Anlagen bleiben sichtbar, jetzt als eigenständige Dokumente ohne Einrückung. |
| 5.7 | `Mail.pdf` aus dem Papierkorb wiederherstellen. | Anlagen erscheinen wieder eingerückt darunter. |
| 5.8 | Details von Anlage2: übergeordnetes Dokument auf „keins“ setzen, speichern. | Anlage2 wird eigenständig; das Badge an Mail zeigt 1. |

---

## 6. Kopieren und Verschieben in eine andere Akte

Vorher: Anlage2 wieder an Mail hängen; Mail und Anlagen haben Bezeichnung, Schlagworte,
Eingang, Von/An und eine Etikette.

| # | Schritt | Erwartet |
|---|---------|----------|
| 6.1 | Mail und beide Anlagen markieren → Kontextmenü **in andere Akte kopieren** → Akte B. | In Akte B alle drei vorhanden, Anlagen unter Mail; Bezeichnung, Schlagworte, Eingang, Von/An (inkl. Kontaktbezug), Etiketten, Favorit, Markierungen übernommen. Akte A unverändert. |
| 6.2 | In Akte A nur **Anlage1** in Akte B kopieren. | Kopie in B ist eigenständig (kein Parent), Metadaten übernommen. |
| 6.3 | In Akte A Mail und Anlagen **in andere Akte verschieben** → Akte B. | In B Hierarchie und Metadaten erhalten. In A liegen die drei Quellen im Papierkorb. |
| 6.4 | In Akte B nur die **Mail** zurück nach A verschieben. | In A Mail ohne Anlagen. In B bleiben die Anlagen als eigenständige Dokumente. |
| 6.5 | Gesperrtes Dokument (von einem anderen Benutzer geöffnet) verschieben. | Abgelehnt mit Hinweis, es wird nichts kopiert. |
| 6.6 | **Ingo**: im Chat „Verschiebe das Dokument <Bezeichnung> in die Akte ZZ Test Metadaten A“. | Ingo nutzt „Dokument verschieben“; das Dokument landet mit Metadaten in Akte A, die Quelle im Papierkorb. |

---

## 7. Nachrichten zu Dokumenten

| # | Schritt | Erwartet |
|---|---------|----------|
| 7.1 | Dokument wählen, Reiter **Nachrichten**. | Eingabefeld **oben**, darunter die Nachrichten, neueste zuerst. Kein separater Knopf „Nachricht zu diesem Dokument“. |
| 7.2 | Nachricht „Bitte prüfen“ senden. | Erscheint oben in der Liste; in der Listenansicht zeigt das Dokument ein Nachrichtensymbol mit **1** (zweite Zeile, zwischen Größe und Ordner). |
| 7.3 | In der Liste auf das Nachrichtensymbol klicken. | Dokument wird gewählt, der Reiter Nachrichten öffnet sich. |
| 7.4 | Bei einer Nachricht auf **Antworten** klicken. | Das Eingabefeld oben wird vorbelegt (statt Popup). |
| 7.5 | Tabellenansicht, Klick auf den Nachrichtenzähler. | Popup mit den Nachrichten des Dokuments. |

---

## 8. Automatische Befüllung beim Speichern (E-Mail, beA, Fax)

| # | Schritt | Erwartet |
|---|---------|----------|
| 8.1 | E-Mail von `vera@example.org` mit zwei Anhängen im Postfach → **in Akte A speichern**. | Speicherdialog zeigt je Eintrag Metadaten: E-Mail mit Bezeichnung = Betreff, Eingang = **Zeitstempel der E-Mail** (nicht leer), Von = Vera; Anhänge mit „als Anlage zu … speichern“ angehakt. |
| 8.2 | Speichern. | Anhänge eingerückt unter der E-Mail; Von/An mit **Kontaktbezug zu Vera** (Beteiligte der Akte). |
| 8.3 | Wie 8.1, bei einem Anhang „als Anlage“ abwählen. | Der Anhang wird eigenständig gespeichert. |
| 8.4 | E-Mail aus Akte A an Vera **versenden** (mit Dokument aus der Akte als Anhang). | Gespeicherte Ausgangs-Mail: ↗ Vera; das angehängte Akten-Dokument erhält „An Vera“, sofern es noch kein Von/An hatte. |
| 8.5 | E-Mail an drei Empfänger versenden. | Von/An „<erster Empfänger> +2“. |
| 8.6 | **beA**-Nachricht aus Akte A versenden. | Gespeicherte Nachricht: ↗ Empfänger (Kontaktbezug über SafeId, falls vorhanden). |
| 8.7 | Eingegangene beA-Nachricht mit Anlagen in Akte A speichern. | Von = Absender, Eingang = Empfangszeit, Anlagen unter der Nachricht. |
| 8.8 | Fax aus Akte A senden, Faxbericht abwarten. | Faxbericht mit ↗ Empfänger. |

---

## 9. Speicherdialog: Metadaten und KI vor dem Speichern

| # | Schritt | Erwartet |
|---|---------|----------|
| 9.1 | E-Mail mit `Vertrag.docx`, `Scan.pdf` und einem Text-PDF speichern (Dialog wie 8.1). Nur zwei Einträge zum Speichern auswählen. | Zeile „für alle“ mit **Schlagworte für alle...**, **Von / An für alle...**, **Schlagworte eintragen** und **Bezeichnungen vorschlagen**. |
| 9.2 | **Schlagworte eintragen** (Prompt wählen). | Ohne Rückfrage-Dialoge werden Schlagworte direkt ergänzt – **nur bei den zum Speichern ausgewählten** Einträgen; Fortschritt im Statustext. |
| 9.3 | **Bezeichnungen vorschlagen**. | Bezeichnungen der ausgewählten Einträge werden direkt ersetzt, ohne Popups. |
| 9.4 | Statustext bei `Scan.pdf` (ohne Textebene). | Als übersprungen gemeldet; der Tooltip nennt den Grund. |
| 9.5 | Einstellungen → Assistent: Grenze für die Textextraktion vor dem Speichern auf 1 MB setzen; eine DOCX über 1 MB speichern und eine KI-Aktion ausführen. | DOCX wird übersprungen (zu groß); nach Zurücksetzen auf 3 MB wird sie verarbeitet. Die Einstellung bleibt nach einem Neustart erhalten (Server-Einstellung). |
| 9.6 | Einzelnen Eintrag bearbeiten (Bezeichnung, Von/An) und speichern. | Werte wie eingegeben gespeichert; ein unverändertes Von/An wird beim Speichern auf den Kontakt aufgelöst. |

---

## 10. Suche

| # | Schritt | Erwartet |
|---|---------|----------|
| 10.1 | Volltextsuche: `schlagwort:kaufpreis`. | Findet „Kaufvertrag Müller“, keine Dokumente ohne dieses Schlagwort. |
| 10.2 | `bezeichnung:kaufvertrag` und `von:vera`. | Finden das jeweilige Dokument. |
| 10.3 | Ohne Feldpräfix: `Kaufpreis`. | Findet das Dokument auch über die Metadaten. |
| 10.4 | Schnellfilter in der Dokumentliste: „Frist“, dann „Vera“. | Treffer über Schlagworte bzw. Von/An in Liste und Tabelle. |

---

## 11. Rechte (optional, falls ein Testbenutzer verfügbar ist)

| # | Schritt | Erwartet |
|---|---------|----------|
| 11.1 | Benutzer ohne Schreibrecht auf Akten öffnet Akte A. | Metadaten sichtbar; Details, Eigenschaften, F2 und Drag & Drop auf ein Dokument sind nicht speicherbar (Fehlermeldung oder deaktiviert). |
| 11.2 | Benutzer ohne Zugriff auf eine gruppenbeschränkte Akte. | Akte und Dokumente nicht erreichbar (serverseitig bereits per `verify_rest.py` bestätigt). |

---

## 12. Web-Client (Kurzcheck, gehört zu 11.5)

`http://localhost:8080/j-lawyer-web/` → Akte A → Dokumente:
- [ ] Bezeichnung, ↘/↗ Von/An, Eingang, Etiketten-Chips vor Schlagwort-Chips.
- [ ] Grünes 📎-Badge klappt Anlagen auf/zu; Hinweis „Anlage zu …“ bei gefiltertem Ordner.
- [ ] 💬-Badge öffnet den Nachrichten-Dialog; Senden verknüpft die Nachricht mit dem Dokument.
- [ ] Menü **Eigenschaften…** einzeln (inkl. übergeordnetes Dokument) und über die
      Stapelleiste (Schlagwort hinzufügen bei mehreren Dokumenten).
- [ ] E-Mail mit Anhängen im Web in Akte A speichern → Hierarchie und Von/An wie in 8.1/8.2.
- [ ] Sortierung „Eingang“; die Suche findet Schlagworte.

---

## Aufräumen

- Akten „ZZ Test Metadaten A/B“ löschen (entfernt auch Dokumente und Nachrichten).
- Kontakt „Vera Verifikation“ löschen.
- Testweise geänderte Assistenten-Einstellungen und die Grenze für die Textextraktion
  zurücksetzen.
- Ansicht und Spalten ggf. auf die gewünschte Einstellung zurückstellen.

## Ergebnis

Nach erfolgreichem Durchlauf in `tasks.md` 11.3 (und 11.5, falls Abschnitt 12 ok) abhaken.
Abweichungen bitte mit Schritt-Nummer melden.
