# Change: Dokument-Metadaten erweitern und Dokumentenansicht der Akte überarbeiten

## Why
Ein Aktendokument ist heute im Wesentlichen durch seinen Dateinamen beschrieben. Es fehlen
fachliche Metadaten, die in der Kanzleipraxis ständig gebraucht werden: eine lesbare
Bezeichnung unabhängig vom Dateinamen, frei vergebbare Schlagworte, das Eingangsdatum, der
Absender bzw. Empfänger und der Zusammenhang zwischen Dokumenten (z. B. E-Mail und ihre
Anlagen). Außerdem existieren bereits Instant Messages mit Dokumentbezug
(`instantmessage.document_id`), die am Dokument selbst aber nirgends sichtbar sind.

Die heutige Dokumentenliste (`CaseFolderPanel` / `DocumentEntryPanel`) ist einzeilig und
auf die vorhandenen Felder zugeschnitten; zusätzliche Metadaten lassen sich darin nicht
sinnvoll unterbringen. Nutzer brauchen außerdem je nach Arbeitssituation entweder eine
Liste mit Vorschau (Sichten, Lesen) oder eine dichte Tabelle (Sortieren, Vergleichen,
Überblick über viele Dokumente).

## What Changes
- **Datenmodell `case_documents` / `ArchiveFileDocumentsBean`** (Flyway-Migration), neue Felder:
  - `title` – Bezeichnung (menschenlesbar, unabhängig vom Dateinamen)
  - `keywords` – Schlagworte (ein Feld, kommasepariert, normalisiert)
  - `received_date` – Eingangsdatum
  - `correspondent_id` / `correspondent_name` / `correspondent_direction` – Von/An:
    Verweis auf einen Kontakt plus Freitext-Fallback und Richtung (eingehend/ausgehend)
  - `parent_id` – Eltern-Dokument für Hierarchien (z. B. Anlagen einer E-Mail)
- **Services (EJB):** Metadaten einzeln und für mehrere Dokumente zugleich setzen,
  Eltern-Beziehungen setzen/lösen (gleiche Akte, keine Zyklen), Schlagwort-Vorschläge je
  Akte, Nachrichtenanzahl je Dokument in einem Aufruf (kein N+1). Kinder werden beim
  Löschen/Verschieben des Eltern-Dokuments eigenständig; werden Eltern-Dokument und Kinder
  gemeinsam verschoben, bleiben die Beziehungen erhalten.
- **Kopieren/Verschieben in andere Akten serverseitig** (`copyDocumentsToCase`,
  `moveDocumentsToCase`) statt Client-Schleife: alle Metadaten und Etiketten werden
  übernommen, Eltern-Kind-Beziehungen innerhalb der kopierten/verschobenen Menge bleiben
  erhalten (auf die neuen IDs umgehängt).
- **Anlegen mit Metadaten in einem Schritt** (neue `addDocument`-Variante): eine
  Transaktion, eine Indizierung.
- **Automatische Befüllung:** Beim Speichern von E-Mails/beA-Nachrichten in die Akte werden
  Eingangsdatum, Absender, Bezeichnung (Betreff) und die Hierarchie Nachricht → Anlagen
  gesetzt; beim Versand (E-Mail, beA, Fax, ePost) wird der Empfänger als „An“ gesetzt,
  sofern noch kein Von/An gesetzt ist.
- **Desktop-Client – zwei umschaltbare Ansichten** im Dokumente-Reiter der Akte:
  1. *Liste + Vorschau* (Weiterentwicklung der heutigen Ansicht): zweizeilige Einträge mit
     Bezeichnung, Von/An, Eingangsdatum, Schlagworten und Status-Icons (u. a. 💬 Nachrichten,
     📎 Anlagen); rechter Bereich mit Reitern *Vorschau*, *Details*, *Nachrichten*.
  2. *Tabelle*: `JXTreeTable`, eine Zeile je Dokument, Optik an die heutige Liste angelehnt,
     konfigurierbare Spalten, Sortierung über Spaltenköpfe, Inline-Bearbeitung.
  Umschaltung jederzeit über einen Segment-Schalter; Modus, Spalten und Sortierung werden
  pro Benutzer gespeichert; Ordner-, Dokumentauswahl und Filter bleiben beim Umschalten
  erhalten. Hierarchien sind in beiden Ansichten auf-/zuklappbar.
- **Eigenschaften-Dialog** für ein oder mehrere Dokumente (Mehrfachbearbeitung).
- **Etiketten:** Das Panel „Dokument-Etiketten“ unter der Liste entfällt; aktive Etiketten
  werden direkt am Dokument angezeigt (Chips bzw. Tabellenspalte) und zusammen mit den
  übrigen Metadaten im Reiter *Details* bzw. im Eigenschaften-Dialog bearbeitet.
- **Instant Messages am Dokument sichtbar** (Indikator mit Anzahl, Nachrichtenliste,
  neue Nachricht mit Dokumentbezug direkt aus dem Dokument).
- **Volltextsuche (Lucene):** Bezeichnung, Schlagworte und Von/An werden indiziert und sind
  per Feldpräfix (`bezeichnung:`, `schlagwort:`, `von:`) durchsuchbar; der Schnellfilter
  der Dokumentenansicht berücksichtigt sie ebenfalls.
- **KI-Button an allen Schlagwort-Eingaben des Desktop-Clients**: bietet alle Ingo-Prompts
  der Typen `chat` und `extract` an, führt sie mit dem Dokumenttext aus und schlägt
  Schlagworte zur Übernahme vor.
- **Ansichtsvariante und Spaltenkonfiguration als User Settings** (serverseitig, folgen dem
  Benutzer an jeden Arbeitsplatz).
- **REST:** alle vorhandenen Endpunkte funktionieren unverändert weiter und setzen die neuen
  Metadaten nicht zurück.
- **REST v8 (additiv):** Dokumentliste einer Akte mit allen neuen Metadaten, Nachrichten-
  und Anlagenanzahl; Einzeldokument; Metadaten einzeln und als Stapel aktualisieren;
  Eltern-Dokument setzen; Schlagwort-Vorschläge der Akte; verlinkte Nachrichten.
- **Web-Client:** Dokumente-Reiter der Akte zeigt die neuen Metadaten, Hierarchie und
  Nachrichten-Indikator; Metadaten sind einzeln und im Stapel bearbeitbar.

## Impact
- Affected specs: `document-metadata` (neu), `case-document-views` (neu),
  `full-text-search` (MODIFIED: Feldsuche um Metadatenfelder erweitert)
- Affected code (Auszug):
  - `j-lawyer-server-entities/.../persistence/ArchiveFileDocumentsBean.java`
  - `j-lawyer-server-entities/src/main/resources/db/migration/V3_6_0_<n>__CaseDocumentsMetadata.sql` (neu, nächste freie Nummer)
  - `j-lawyer-server-api/.../services/ArchiveFileServiceRemote.java`, `MessagingServiceRemote.java`
  - `j-lawyer-server/j-lawyer-server-ejb/.../services/ArchiveFileService.java`,
    `ArchiveFileServiceLocal.java`, `MessagingService.java`
  - `j-lawyer-server/j-lawyer-server-ejb/.../org/jlawyer/search/SearchAPI.java`,
    `SearchQueryBuilder.java`, `SearchIndexProcessor.java`
  - `j-lawyer-server/j-lawyer-io/.../rest/v8/CasesEndpointV8.java` + neue POJOs
  - `j-lawyer-client/.../ui/folders/CaseFolderPanel.java/.form`,
    `DocumentEntryPanel.java/.form`, neue Tabellen-/Renderer-/Dialog-Klassen
  - `j-lawyer-client/.../editors/files/ArchiveFilePanel.java/.form` (Vorschau-Reiter)
  - `j-lawyer-client/.../mail/SaveToCaseExecutor.java`, `SendAction.java`,
    `SendEncryptedAction.java`, `.../bea/SaveBeaMessageAction.java`,
    `SendBeaMessageAction.java`, `.../voip/EpostLetterSendStatus.java`,
    `j-lawyer-server-ejb/.../VoipService.java`
  - `j-lawyer-web/frontend/src/app/akten/*` (Dokumente-Reiter, Aktionen, Stapelleiste)
  - `j-lawyer-client/.../assistant/AssistantAccess.java` (Prompt-Menü wiederverwenden),
    `.../assistant/ToolRegistry.java` (`move_document_to_case` auf Server-Methode umstellen)
  - `j-lawyer-server-common/.../settings/UserSettingsKeys.java`
- Keine Breaking Changes: alle neuen Spalten sind nullable, bestehende REST-Versionen
  bleiben unverändert, bestehende Dokumente behalten ihr Verhalten (Bezeichnung leer →
  Anzeige des Dateinamens).
- Etiketten (`DocumentTagsBean`) bleiben unverändert bestehen; Schlagworte ergänzen sie als
  freies Vokabular.
- Außerhalb des Scopes: Metadaten an Kontakt-Dokumenten (`AddressDocumentsBean`),
  mehrere Empfänger je Dokument, nachträgliches Befüllen von Bestandsdokumenten,
  KI-Schlagwortvorschläge im Web-Client (vorerst zurückgestellt).
- Folge-Change (bewusst getrennt): MCP-Server/Ingo-Tools (`list_case_documents` u. ä.)
  geben die neuen Felder aus.
