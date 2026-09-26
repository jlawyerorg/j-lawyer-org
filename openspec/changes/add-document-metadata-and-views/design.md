# Design: Dokument-Metadaten und Dokumentenansichten

## Context
- `ArchiveFileDocumentsBean` (`case_documents`) trägt heute Name, Größe, Diktatzeichen,
  Erstell-/Änderungsdatum, Favorit, Ordner, Markierungen, Typ, Sperre, externe ID.
- Etiketten liegen separat in `DocumentTagsBean` (boolesch und mehrwertig, kontrolliertes
  Vokabular über `server_options`).
- `InstantMessage` hat bereits `document_id` → `case_documents.id`; die UI zeigt das nicht.
- Die Dokumentenliste ist `CaseFolderPanel` mit je einem `DocumentEntryPanel` pro Dokument
  in einem `pnlDocumentEntries`-Container plus `SortButton`s; die Vorschau liegt im
  `ArchiveFilePanel` (`splitDocumentsMain`/`splitDocuments`).
- SwingX (`JXTreeTable`) ist im Client verfügbar.
- REST: neueste Version ist v8 (JWT, Web-Client). Der Web-Client lädt Dokumente heute über
  `GET /v1/cases/{id}/documents/with-tags`.

## Goals / Non-Goals
- Goals: fünf neue Metadatenfelder, Sichtbarkeit von Nachrichten am Dokument, zwei
  gleichwertige, jederzeit umschaltbare Ansichten im Desktop-Client, Parität im Web-Client,
  Such- und REST-Anbindung.
- Non-Goals: mehrere Von/An-Einträge je Dokument; Ablösung der Etiketten; Metadaten an
  Kontakt-Dokumenten; Änderung bestehender REST-Endpunkte; Datenmigration/Backfill für
  Bestandsdokumente; MCP/Ingo-Tool-Ausgabe (Folge-Change); KI-Schlagwortvorschläge im
  Web-Client und KI-Ausführung über REST.

## Decisions

### D1 – Spalten und Typen
| Spalte | Typ | Bemerkung |
|---|---|---|
| `title` | `VARCHAR(500) NULL` | leer → UI zeigt Dateinamen |
| `keywords` | `VARCHAR(2000) NULL` | normalisiert: getrimmt, dedupliziert (case-insensitive), getrennt durch `, ` |
| `received_date` | `DATETIME NULL` | Datum + Uhrzeit (Mail-/beA-Eingang hat Uhrzeit); UI zeigt Datum, Tooltip mit Uhrzeit |
| `correspondent_id` | `VARCHAR(50) NULL` | Kontakt-ID (`contacts.id`), FK `ON DELETE SET NULL` |
| `correspondent_name` | `VARCHAR(500) NULL` | Anzeigename/Freitext; bei Kontaktverweis zum Zeitpunkt der Zuordnung denormalisiert |
| `correspondent_direction` | `INT NOT NULL DEFAULT 0` | 0 = keine, 1 = eingehend (Von), 2 = ausgehend (An) |
| `parent_id` | `VARCHAR(50) NULL` | FK `case_documents.id ON DELETE SET NULL` |

Indizes auf `parent_id`, `correspondent_id`, `received_date`. Migration
`V3_6_0_<n>__CaseDocumentsMetadata.sql` mit der **nächsten freien Nummer zum Zeitpunkt der
Umsetzung** (die offene Change `add-bidirectional-nextcloud-calendar-sync` plant ebenfalls
eine `V3_6_0_49`); Muster wie bestehende Migrationen inkl. Bump von
`jlawyer.server.database.version`.

**Warum ein Schlagwortfeld statt Tabelle:** Anforderung „ein Feld für beste
Durchsuchbarkeit“; die eigentliche Suche läuft über Lucene (D6), Autovervollständigung über
eine aggregierende Abfrage je Akte. Eine n:m-Tabelle bringt hier keinen Mehrwert, und das
kontrollierte Vokabular decken bereits die Etiketten ab.

### D2 – Referenzen als String-IDs statt `@ManyToOne`
`correspondentId` und `parentId` werden als `String` gemappt, nicht als Entity-Beziehung.
Grund: Dokumentlisten werden in großen Mengen über EJB-Remote serialisiert; eine
`@ManyToOne` auf `AddressBean` bzw. rekursiv auf `ArchiveFileDocumentsBean` würde
Kontakte/Eltern mitladen und mitserialisieren. Der Anzeigename liegt denormalisiert in
`correspondent_name`, sodass die Liste ohne Zusatzabfrage auskommt. Der Kontakt wird nur
beim Öffnen (Klick auf Von/An) nachgeladen.

### D3 – Hierarchie-Regeln
- Eltern- und Kind-Dokument MÜSSEN derselben Akte angehören; Zyklen werden serverseitig
  abgewiesen. Die Tiefe ist nicht begrenzt (praktisch 1–2 Ebenen).
- Kinder dürfen in einem anderen Ordner liegen als das Eltern-Dokument. Anzeige: Ein Kind
  wird unter seinem Eltern-Dokument angezeigt, wenn dieses in der aktuellen Ordnerauswahl
  sichtbar ist; sonst erscheint es auf oberster Ebene mit Hinweis „Anlage zu …“.
- **Löschen** (Papierkorb) des Eltern-Dokuments: Kinder werden in der Anzeige sofort
  eigenständig. `parent_id` bleibt bis zur endgültigen Löschung erhalten, damit ein
  Wiederherstellen die Beziehung wiederherstellt; bei endgültiger Löschung setzt die FK
  (`ON DELETE SET NULL`) bzw. der Service `parent_id` auf `NULL`.
- **Verschieben in einen anderen Ordner** derselben Akte ändert keine Beziehung.
- **Kopieren und Verschieben in eine andere Akte** – siehe D11.

### D4 – Von/An-Auflösung und automatische Befüllung
- Kontaktauflösung: zuerst unter den Beteiligten der Akte, danach global; Schlüssel je
  Kanal: E-Mail-Adresse, beA-SafeId, Faxnummer. Kein Treffer → nur `correspondent_name`
  (Freitext, z. B. „Max Mustermann <max@example.org>“).
- **Eingang** (E-Mail oder beA in Akte speichern, auch serverseitig automatisiert):
  `received_date` = Empfangszeitpunkt, Von = Absender, Richtung eingehend, `title` = Betreff
  der Nachricht (nur am Nachrichtendokument), Anlagen erhalten `parent_id` = Nachrichten-
  dokument, dieselbe Richtung, denselben Absender und dasselbe Eingangsdatum. Wird „nur
  Anlagen“ gespeichert, entsteht keine Hierarchie.
- **Versand** (E-Mail, beA, Fax, ePost): Für jedes versendete Aktendokument wird An = erster
  Empfänger gesetzt, **sofern noch kein Von/An gesetzt ist** – ein weitergeleitetes
  eingegangenes Dokument behält also seinen Absender. Die als Dokument abgelegte gesendete
  Nachricht (z. B. `.eml`, beA-Export) erhält An = erster Empfänger, Richtung ausgehend,
  Bezeichnung = Betreff, und die mitversendeten Aktendokumente werden **nicht** umgehängt
  (sie existieren bereits in der Akte). Bei mehreren Empfängern enthält
  `correspondent_name` den ersten Empfänger plus „ +N“.
- Auch Dokumente **ausgehender** Nachrichten (gesendete E-Mail/beA aus eigenem Postfach, beim
  Versand abgelegte Nachricht, ePost) erhalten das Datum der Nachricht als Eingangs-/Datums-
  feld, damit jede Nachricht zeitlich eingeordnet ist.
- Manuelles Setzen überschreibt immer.

### D5 – Metadatenänderungen, Versionierung, Historie
Metadatenänderungen erzeugen **keine** neue Dokumentversion und ändern `changeDate` nicht
(„Geändert“ bleibt inhaltsbezogen). Sie erzeugen einen Eintrag in der Aktenhistorie
(einen Sammeleintrag bei Stapelbearbeitung). Gesperrte Dokumente (`locked`) dürfen in
ihren Metadaten geändert werden, da der Inhalt unberührt bleibt.

Neue Remote-Methoden (Auszug, mit englischer JavaDoc):
- `updateDocumentMetadata(String docId, DocumentMetadata md)` – setzt alle Felder
- `updateDocumentsMetadata(List<String> docIds, DocumentMetadataPatch patch)` – Stapel;
  Patch mit Operationen je Feld (setzen / leeren / unverändert; bei Schlagworten zusätzlich
  hinzufügen / entfernen)
- `setDocumentParent(String docId, String parentId)` (null = lösen)
- `getDocumentKeywordsForCase(String caseId)` – distinct, für Autovervollständigung
- `getInstantMessageCountsForCase(String caseId)` → `Map<String,Integer>` (docId → Anzahl)
- `getInstantMessagesForDocument(String docId)`

- `addDocument(…, DocumentMetadata metadata)` (D12), `copyDocumentsToCase`,
  `moveDocumentsToCase` (D11)

`DocumentMetadata`/`DocumentMetadataPatch` sind serialisierbare DTOs in
`j-lawyer-server-api`. Die Anlagenanzahl wird client-/serverseitig aus der Dokumentliste
(`parent_id`) abgeleitet.

### D6 – Suche
`SearchAPI` indiziert zusätzlich `title` und `keywords` als analysierte Felder (Treffer der
Standard-Volltextsuche) sowie Keyword-Felder für `bezeichnung:`, `schlagwort:`, `von:`
analog zu `dateiname:`. Bei Metadatenänderung wird der Indexeintrag des Dokuments neu
geschrieben; der Text wird dabei aus dem gespeicherten `FIELD_TEXT` übernommen, damit keine
erneute Textextraktion nötig ist. Bestandsdokumente werden über den bestehenden Re-Index
erfasst. Der Schnellfilter im Client filtert lokal über Bezeichnung, Dateiname,
Schlagworte und Von/An-Namen.

### D7 – Desktop-Architektur der zwei Ansichten
- Gemeinsames Modell `CaseDocumentsViewModel` (Dokumente, Ordnerfilter, Suchfilter,
  Sortierung, Auswahl, Nachrichtenzähler, Rechnungsverknüpfungen, Hierarchie). Beide
  Ansichten sind reine Darstellungen dieses Modells; Aktionen (Kontextmenü, Drag & Drop,
  Tastenkürzel) nutzen dieselben Action-Klassen, sodass sie in beiden Modi identisch sind.
- `CaseFolderPanel` erhält einen `CardLayout`-Container mit `pnlListView` (bestehende
  `DocumentEntryPanel`-Liste, erweitert) und `pnlTableView` (neue Klasse
  `CaseDocumentsTreeTable` auf Basis `JXTreeTable` mit eigenem `TreeTableModel` und
  Renderern). Beide Container und der Segment-Schalter werden im `.form` gepflegt; die
  Tabelle selbst wird als Custom-Creation-Code eingebunden, damit der GUI-Builder nutzbar
  bleibt.
- Die bestehenden `SortButton`s steuern im Listenmodus die Sortierung; im Tabellenmodus
  übernehmen die Spaltenköpfe diese Rolle. Beide schreiben in dasselbe Sortierfeld des
  Modells.
- Im Tabellenmodus wird der Vorschaubereich von `ArchiveFilePanel` ausgeblendet (Divider
  gespeichert und beim Zurückschalten wiederhergestellt).
- Persistenz als **User Settings** (serverseitig über `UserSettings` gespeichert, folgen
  dem Benutzer an jeden Arbeitsplatz), Schlüssel als Konstanten in `UserSettingsKeys`:
  - `CONF_DOCUMENTS_VIEWMODE = "client.documents.viewmode"` (`list`|`table`)
  - `CONF_DOCUMENTS_TABLE_COLUMNS = "client.documents.table.columns"` – Spalten-ID,
    Reihenfolge, Sichtbarkeit und Breite als kompakter String, z. B.
    `title:1:320,correspondent:1:160,received:1:90,size:0:70,…`; unbekannte/neue Spalten
    werden mit Default ergänzt, damit spätere Spalten keine Migration brauchen
  - die bestehende `CONF_DOCUMENTS_LASTSORTMODE` wird für beide Modi genutzt und um die
    neuen Sortierschlüssel (Bezeichnung, Eingang, Von/An) erweitert
  Gespeichert wird bei jeder Änderung (Umschalten, Spalte verschieben/ein-/ausblenden,
  Breite geändert – letzteres entprellt).
- **Optik der Tabelle:** gleiche Schriften, Icons und Zeilenhöhe wie die Liste, keine
  Gitterlinien, dezente horizontale Trennlinie, Hover-Hervorhebung, Markierungsfarben
  (`highlight1/2`) als Zeilenhintergrund, Favorit/Sperre/Rechnung/Nachrichten als Icons.

### D8 – UI-Layout (Referenz)
Modus 1 – Liste + Vorschau:
```
┌Ordner──────┬───────────────────────────────────────────────┬─[Vorschau|Details|Nachrichten(2)]─┐
│ ▾ Akte     │☐ ★ 📄 Klageerwiderung Beklagter         🔒 💬2 € │ Bezeichnung / Schlagworte /        │
│   Schrift- │      ↘ RA Müller · Eing. 12.09.26 · Frist Kosten │ Eingang / Von-An (editierbar)      │
│   sätze    │      klageerwiderung_2026-09-12.pdf · 1,2 MB     │ ─────────────────────────────────  │
│   Korresp. │☐   ✉ ▾ AW: Vergleichsangebot             📎3      │  [Vorschau]                        │
│            │   ├ 📄 Anlage K1 – Rechnung                      │                                    │
└────────────┴───────────────────────────────────────────────┴────────────────────────────────────┘
```
Modus 2 – Tabelle:
```
┌Ordner──────┬☐┬★┬Typ┬Bezeichnung ▲─────────────┬Von/An───────┬Eingang─┬Geändert┬Schlagworte──┬Ordner──┬Größe┬Status──┐
│ ▾ Akte     │☐│★│📄 │Klageerwiderung Beklagter │↘ RA Müller  │12.09.26│12.09.26│Frist, Kosten│Schrifts│1,2MB│🔒 💬2 € │
│            │☐│ │✉  │▾ AW: Vergleichsangebot   │↘ Meier      │10.09.26│10.09.26│             │Korresp.│ 80KB│📎3      │
│            │☐│ │📄 │   Anlage K1 – Rechnung   │↘ Meier      │10.09.26│10.09.26│             │Korresp.│0,4MB│         │
└────────────┴─┴─┴───┴──────────────────────────┴─────────────┴────────┴────────┴─────────────┴────────┴─────┴─────────┘
```
Weitere Spalten: Etiketten (standardmäßig sichtbar, aktive Etiketten als Chips); optional
Dateiname, Erstellt, Diktatzeichen, Rechnung. In der Listenansicht erscheinen aktive
Etiketten als Chips in der zweiten Zeile (D13).

### D9 – REST v8 und Web-Client
- **Kompatibilität:** Alle bestehenden REST-Endpunkte (v1–v8) funktionieren unverändert
  weiter; Request- und Response-Formate bestehender Endpunkte werden nicht verändert.
  Bestehende schreibende Endpunkte (z. B. Dokument umbenennen, Ordner/Datum/Favorit setzen,
  Inhalt ersetzen, Dokument anlegen) DÜRFEN die neuen Metadaten nicht zurücksetzen; wo
  ein Endpunkt heute ein Bean aus einem POJO neu aufbaut, werden die neuen Felder aus dem
  gespeicherten Dokument übernommen.
- Die neuen Attribute werden additiv über v8 verfügbar gemacht:
- `GET /v8/cases/{id}/documents` → `RestfulDocumentV8` (alle bisherigen Felder aus
  `with-tags` + `title`, `keywords[]`, `receivedDate`, `correspondentId`,
  `correspondentName`, `correspondentDirection`, `parentId`, `messageCount`).
- `PUT /v8/cases/documents/{id}/metadata` (einzeln), `PUT /v8/cases/documents/metadata`
  (Stapel mit Patch-Semantik wie D5), `GET /v8/cases/{id}/documents/keywords`,
  `GET /v8/cases/documents/{id}/messages`, `GET /v8/cases/documents/{id}` (Einzeldokument
  mit Metadaten), `PUT /v8/cases/documents/{id}/parent`. Rollen: lesen `readArchiveFileRole`, schreiben
  `writeArchiveFileRole`, jeweils mit Akten-ACL-Prüfung.
- Web-Client wechselt im Dokumente-Reiter auf den v8-Endpunkt, zeigt Bezeichnung (Fallback
  Dateiname), Von/An, Eingang, Schlagwort-Chips, 💬/📎-Indikatoren, aufklappbare
  Hierarchie; Bearbeitung in `document-actions` und `document-bulk-bar`.

### D10 – KI-Unterstützung für Schlagworte
Überall, wo im **Desktop-Client** Schlagworte bearbeitet werden können (Reiter *Details*,
Inline-Editor der Tabelle, Eigenschaften-Dialog einzeln und im Stapel), gibt es einen
**KI-Button** (Ingo-Icon wie an anderen Stellen).
- Er bietet **alle eigenen Prompts der Anfragetypen `chat` und `extract`** an
  (`AssistantAccess.populatePromptMenu(menu, AiCapability.REQUESTTYPE_CHAT, …)` und
  `…REQUESTTYPE_EXTRACT…`, getrennt durch Separator; Untermenüs gemäß Prompt-
  Konfiguration). Sind keine Assistenten/Prompts konfiguriert, ist der Button deaktiviert
  (Tooltip mit Hinweis).
- Eingabe: der extrahierte Text des Dokuments (bestehende Textextraktion wie bei Ingo
  „Dokument als Kontext“), begrenzt auf die ersten **1.500 Wörter** (etwa drei A4-Seiten),
  plus Dateiname, Bezeichnung und vorhandene Schlagworte; der Prompt-Text
  kommt aus der Prompt-Konfiguration. Die Anfrage läuft asynchron mit Fortschritts-
  anzeige und ist abbrechbar.
- Ausgabe: der Antworttext wird an Kommas, Semikolons und Zeilenumbrüchen zerlegt,
  Aufzählungszeichen/Anführungszeichen entfernt, normalisiert (D1) und als **Vorschlags-
  liste mit Checkboxen** angezeigt (vorhandene Schlagworte ausgegraut). Übernommene
  Vorschläge werden dem Eingabefeld **hinzugefügt**, nicht gespeichert – Speichern bleibt
  eine explizite Nutzeraktion.
- **Bezeichnung:** Derselbe Button steht hinter dem Feld „Bezeichnung“ (Reiter *Details*,
  Eigenschaften-Dialog für ein Dokument). Gleiche Prompts, gleicher Kontext, der Dokumenttext
  aber nur bis 500 Wörter (etwa eine A4-Seite); aus der Antwort
  wird die erste nicht-leere Zeile ohne Aufzählungszeichen, Anführungszeichen, Markdown und
  ein vorangestelltes „Bezeichnung:“ genommen, zur Prüfung angeboten und nur ins Feld
  übernommen (`MetadataSuggester.suggestTitle`, `TitleEditor`).
- Stapelbearbeitung: der Prompt wird je ausgewähltem Dokument ausgeführt (sequenziell,
  Fortschritt „3/7“); die Vorschläge erscheinen je Dokument und werden beim Speichern je
  Dokument hinzugefügt.
- **Web-Client: vorerst ohne KI-Button.** REST bietet heute nur die Prompt-*Konfiguration*
  (`/v8/assistant/prompts`, Admin), keine Ausführung von Prompts; eine KI-Ausführung über
  REST wird in dieser Change bewusst nicht eingeführt. Die Schlagwort-Eingaben im Web
  bieten Autovervollständigung aus der Akte, aber keine KI-Vorschläge.

### D11 – Kopieren und Verschieben serverseitig und mengenbasiert
**Ist-Zustand:** „In andere Akte verschieben/kopieren“ ist heute im Client orchestriert
(`ArchiveFilePanel`, `mnuMoveDocumentToOtherCase…`/`mnuCopyDocumentToOtherCase…`): je
Dokument `getDocumentContent` → `addDocument` in der Zielakte → Etiketten einzeln
übertragen → beim Verschieben `removeDocument` (Papierkorb). Das Ingo-Tool
`move_document_to_case` (`ToolRegistry`) macht dasselbe. Die Kopie erhält eine **neue ID**;
Metadaten außer Diktatzeichen, Ordner und Etiketten gehen verloren.

**Neu:** zwei Remote-/Local-Methoden in `ArchiveFileService`, die in einer Transaktion
arbeiten:
- `copyDocumentsToCase(Collection<String> docIds, String targetCaseId, String targetFolderId)`
- `moveDocumentsToCase(Collection<String> docIds, String targetCaseId, String targetFolderId)`
  (= Kopie + Quelle in den Papierkorb, wie bisher)

Beide liefern eine Zuordnung alte ID → neues Dokument und
- kopieren Inhalt, Dateiname, Diktatzeichen, Etiketten und **alle neuen Metadaten**
  (Bezeichnung, Schlagworte, Eingangsdatum, Von/An inkl. Kontaktverweis) sowie Favorit,
  Markierungen und Dokumenttyp,
- übernehmen **Eltern-Kind-Beziehungen innerhalb der übergebenen Menge**, indem
  `parent_id` der Kopien auf die neuen IDs umgehängt wird (gilt für Kopieren und
  Verschieben, in eine andere oder dieselbe Akte),
- lassen Beziehungen zu Dokumenten außerhalb der Menge weg (Kopie ohne `parent_id`); beim
  Verschieben werden in der Quellakte zurückbleibende Kinder eines verschobenen Eltern-
  Dokuments eigenständig (folgt aus D3, da der Parent im Papierkorb liegt),
- bestimmen den Zielnamen bei Namenskonflikten wie bisher (Client übergibt ggf. neue Namen
  als optionale Map `docId → newName`),
- indizieren die Kopien einmalig mit Metadaten (D12) und schreiben je Akte einen
  Historieneintrag.

Client (Kontextmenü, Drag & Drop auf andere Akte), Ingo-Tool `move_document_to_case` und
vorhandene REST-Pfade, die Dokumente zwischen Akten bewegen, werden auf diese Methoden
umgestellt; die bisherige Client-Schleife entfällt. Bestehende REST-Signaturen bleiben
unverändert.

### D12 – Anlegen mit Metadaten in einem Schritt
Neue Variante `addDocument(String caseId, String fileName, byte[] data, String dictateSign,
String externalId, DocumentMetadata metadata)` (Remote + Local). Das Dokument wird mit allen
Metadaten in einer Transaktion angelegt und **einmal** mit Metadaten indiziert; die
automatische Befüllung (D4) sowie Kopieren/Verschieben (D11) nutzen ausschließlich diese
Variante. Für E-Mail/beA mit Anlagen legt der Aufrufer zuerst das Nachrichtendokument an und
übergibt dessen ID als `parentId` im `DocumentMetadata` der Anlagen. Die bestehende
Signatur bleibt erhalten und delegiert mit `metadata = null`.

### D13 – Etiketten: Anzeige in der Liste, Bearbeitung zusammen mit den Metadaten
Das bisherige Panel „Dokument-Etiketten“ unterhalb der Dokumentenliste (`jPanel4` /
`documentTagPanel` in `splitDocuments` des `ArchiveFilePanel`) kostet viel Höhe und
**entfällt**. Stattdessen:
- **Anzeige:** Die *aktiven* Etiketten eines Dokuments werden direkt am Eintrag angezeigt –
  in der Listenansicht als kleine farbige Chips in der zweiten Zeile (boolesche Etiketten
  mit Name, mehrwertige als „Name: Wert“; bei Platzmangel „+N“ mit Tooltip), in der
  Tabellenansicht in der Spalte „Etiketten“ (standardmäßig sichtbar). Inaktive Etiketten
  werden nicht angezeigt.
- **Bearbeitung:** zusammen mit den übrigen Metadaten im Reiter *Details* (Listenmodus) und
  im Eigenschaften-Dialog (beide Modi, einzeln und im Stapel). Boolesche Etiketten als
  Schalter/Checkboxen, mehrwertige als Auswahlfelder – dieselbe Logik wie heute
  (`DocumentTagActionListener`, `MultiDocumentTagActionListener`, `setDocumentTag`),
  inklusive Stapelbearbeitung mehrerer Dokumente. Die Etiketten-Einträge im Kontextmenü
  bleiben als Schnellzugriff erhalten.
- Die Etiketten werden für die Liste in einem Aufruf je Akte geladen (vorhandenes
  `getDocumentTagsForCase`), nicht je Dokument.
- `splitDocuments` wird aufgelöst bzw. enthält nur noch das `CaseFolderPanel`; der
  Etiketten-Filter-Button (`cmdDocumentTagFilter`) bleibt unverändert.

### D14 – Umsetzungsentscheidungen Desktop (nachgetragen)
- **Gemeinsames Modell in `CaseFolderPanel`** statt eigener Klasse `CaseDocumentsViewModel`:
  `CaseFolderPanel` hält Dokumente, Filter, Sortierschlüssel, Zusatzdaten (Nachrichten-
  zähler, Etiketten) und baut die Zeilen (`DocumentRow`, `getVisibleRows()`). Liste und
  Tabelle sind Darstellungen davon; `getSelectedDocuments()` liefert die Auswahl der aktiven
  Ansicht, dadurch arbeiten Kontextmenü, Aktionen und Drag & Drop in beiden Ansichten
  unverändert.
- **Tabelle als `JTable`** (`CaseDocumentsTable`) statt `JXTreeTable`: Die Zeilen kommen
  bereits in Hierarchie-Reihenfolge aus dem gemeinsamen Modell; Einrückung und Auf-/Zuklappen
  zeichnet der Bezeichnungs-Renderer. Sortierung über Spaltenköpfe setzt den gemeinsamen
  Sortierschlüssel (`CaseFolderPanel.setSort`), damit bleibt die Hierarchie erhalten.
- Inline-Bearbeitung startet nur mit F2 (Doppelklick öffnet das Dokument wie in der Liste).
- Spalte „Vorschau“ (Augen-Symbol) hinter dem Dateityp: öffnet `DocumentPreviewDialog`, der
  `LoadDocumentPreviewThread` und damit dieselben Viewer (`DocumentViewerFactory`) wie der
  Vorschaubereich der Liste verwendet - interner Viewer per Klick, externe Anwendung per
  Doppelklick auf die Zeile.
- Die Sperranzeige wird wie in der bestehenden Liste bewusst **nicht** dargestellt (dort
  auskommentiert, weil ein nicht als gesperrt angezeigtes Dokument inzwischen gesperrt sein
  kann).
- Im Tabellenmodus wird der Reiter-Bereich (*Vorschau/Details/Nachrichten*) unsichtbar
  geschaltet; die gespeicherte Teilerposition bleibt dadurch unverändert. Die
  Sortierknöpfe der Liste werden im Tabellenmodus ausgeblendet.
- Neue Komponenten ohne `.form` (handgeschrieben): `DocumentDetailsPanel`,
  `DocumentMessagesPanel`, `KeywordsEditor`, `CorrespondentEditor`,
  `KeywordSuggestionsDialog`, `DocumentPropertiesDialog`, `CaseDocumentsTable`. Die
  bestehenden Form-Klassen (`ArchiveFilePanel`, `CaseFolderPanel`, `DocumentEntryPanel`)
  sind mit ihren `.form`-Dateien synchron.

### D15 – Metadaten und KI-Vorschläge im Speicherdialog (`BulkSaveDialog`)
- Je Eintrag eine Metadatenzeile (`BulkSaveMetadataPanel`, in `BulkSaveEntry.form`):
  Bezeichnung und Schlagworte (je mit KI-Button), Eingang, Von/An, bei Anlagen „als Anlage zu
  … speichern“ (abwählbar). Vorbelegt aus `DocumentOrigin`; Von/An wird beim Speichern aus dem
  Ursprung auf den Kontakt aufgelöst, solange der Anwender es nicht ändert.
- Zeile „für alle“ (`BulkSaveMetadataAllPanel`, in `BulkSaveDialog.form`): Schlagworte
  hinzufügen, Von/An setzen, „Schlagworte eintragen“ und „Bezeichnungen vorschlagen“. Die beiden
  KI-Aktionen laufen Datei für Datei und tragen die Ergebnisse **ohne Rückfrage** direkt ein
  (Schlagworte werden ergänzt, die Bezeichnung ersetzt) - bei vielen Dateien wären Einzeldialoge
  zu viele; alles bleibt vor dem Speichern änderbar. Fortschritt und übersprungene Dateien
  (mit Grund im Tooltip) stehen im Statustext neben den Knöpfen.
- Text für Vorschläge vor dem Speichern (`PreSaveTextSource`, einmal je Eintrag):
  1. Nachricht selbst: Betreff und Text aus dem Client (`DocumentOrigin.messageText`),
  2. PDF: lokal per PDFBox (erste 15 Seiten), kein Upload,
  3. TXT/CSV/MD/XML/JSON/HTML: lokal,
  4. andere Formate: `IntegrationService.extractText` (Tika, nicht persistiert, max. 30.000
     Zeichen) nur bis zur Server-Einstellung `jlawyer.server.assistant.presave.maxmb`
     (Standard 3 MB, einstellbar im Assistenten-Setup),
  5. kein Text (gescanntes PDF) oder zu groß: kein Upload, Hinweis auf Vorschläge nach dem
     Speichern.
- `MetadataSuggester` arbeitet dafür mit einer Textquelle (`TextSource`) statt fest mit einem
  Akten-Dokument; Akten-Dokumente nutzen weiter die Text-Vorschau des Servers.

## Risks / Trade-offs
- **Umfang der Client-Änderung** (`CaseFolderPanel` ≈ 2100 Zeilen, `ArchiveFilePanel`
  ≈ 11 000 Zeilen): Risiko von Regressionen bei DnD, Auswahl, Kontextmenü. Mitigation:
  gemeinsames Modell + gemeinsame Actions, Listenmodus zuerst auf das Modell umstellen
  (ohne sichtbare Änderung), dann Tabelle ergänzen.
- **Performance** großer Akten (1000+ Dokumente): Nachrichtenzähler und Hierarchie werden
  in einem Aufruf geladen; die Tabelle ist leichter als die heutigen Panel-Zeilen.
- **Denormalisierter Kontaktname** kann veralten, wenn der Kontakt umbenannt wird –
  akzeptiert (Dokument hält fest, an wen es ging); Tooltip/Öffnen zeigt aktuellen Kontakt.
- **Lucene-Index**: neue Keyword-Felder greifen erst nach Re-Index für Bestandsdokumente.

## Migration Plan
1. Flyway-Migration fügt Spalten/Indizes/FKs hinzu (nullable, keine Datenänderung).
2. Server + Client werden gemeinsam ausgeliefert (EJB-Remote-DTO ändert sich).
3. Empfehlung in den Release-Notes: Suchindex neu aufbauen.
Rollback: Spalten bleiben ungenutzt; kein Datenverlust bestehender Felder.

## Resolved Questions
- Kein nachträgliches Befüllen von Bestandsdaten (auch nicht in einem Folge-Change).
- MCP-Server/Ingo sollen die neuen Felder ausgeben (`list_case_documents` u. ä.) – als
  **Folge-Change** nach diesem, nicht Teil dieses Scopes.
