## 1. Datenmodell & Migration
- [ ] 1.1 Flyway-Migration `V3_6_0_<n>__CaseDocumentsMetadata.sql` (nächste freie Nummer zum Umsetzungszeitpunkt; Kollision mit `add-bidirectional-nextcloud-calendar-sync` prüfen): Spalten `title`, `keywords`, `received_date`, `correspondent_id`, `correspondent_name`, `correspondent_direction`, `parent_id` (design D1), Indizes, FKs (`ON DELETE SET NULL`), Bump `jlawyer.server.database.version`
- [ ] 1.2 `ArchiveFileDocumentsBean`: Felder + Getter/Setter, Konstanten für Richtung (`CORRESPONDENT_NONE/IN/OUT`), `getDisplayTitle()` (Bezeichnung, sonst Dateiname), Schlagwort-Normalisierung (Hilfsklasse in `j-lawyer-server-common`, mit Unit-Test)
- [ ] 1.3 DTOs `DocumentMetadata` und `DocumentMetadataPatch` in `j-lawyer-server-api`

## 2. Server-Services (EJB)
- [ ] 2.1 `ArchiveFileServiceRemote`/`Local`/`ArchiveFileService`: `updateDocumentMetadata`, `updateDocumentsMetadata` (Patch), `setDocumentParent` (gleiche Akte, Zyklusprüfung), `getDocumentKeywordsForCase` – mit englischer JavaDoc, ACL, Historieneintrag, ohne Versions-/`changeDate`-Änderung
- [ ] 2.2 Hierarchie-Regeln beim Löschen (Anzeige eigenständig, `parent_id` bis endgültige Löschung behalten, Wiederherstellen stellt Beziehung wieder her)
- [ ] 2.2a `addDocument(…, DocumentMetadata)` (Remote + Local, JavaDoc): Anlegen mit Metadaten in einer Transaktion, einmalige Indizierung inkl. Metadaten; bestehende Signatur delegiert (design D12)
- [ ] 2.2b `copyDocumentsToCase` / `moveDocumentsToCase` (Remote + Local, JavaDoc, optionale Namens-Map): Inhalt, Etiketten, alle Metadaten übernehmen, `parent_id` innerhalb der Menge auf neue IDs umhängen, sonst weglassen; Verschieben = Quelle in Papierkorb; Historie je Akte (design D11)
- [ ] 2.3 `MessagingServiceRemote`/`Local`/`MessagingService`: `getInstantMessageCountsForCase(caseId)` (eine Abfrage, GROUP BY), `getInstantMessagesForDocument(docId)`
- [ ] 2.4 Kontaktauflösung für Von/An (Beteiligte zuerst, dann global; E-Mail, beA-SafeId, Faxnummer) als wiederverwendbarer Service-Helper
- [ ] 2.5 Unit-Tests: Schlagwort-Normalisierung, Zyklusprüfung, Patch-Semantik, ID-Umhängung beim Kopieren/Verschieben (Parent+Kind, nur Kind, nur Parent)

## 3. Suche (Lucene)
- [ ] 3.1 `SearchAPI`: `title`/`keywords` analysiert indizieren, Keyword-Felder für `bezeichnung`, `schlagwort` (je Schlagwort ein Term), `von`
- [ ] 3.2 `SearchQueryBuilder`: Feldpräfixe `bezeichnung:`, `schlagwort:`, `von:`; Standardsuche zusätzlich über Titel/Schlagworte; Tests in `SearchQueryBuilderTest` ergänzen
- [ ] 3.3 Index-Update bei Metadatenänderung ohne Textextraktion (gespeichertes `FIELD_TEXT` wiederverwenden) über `SearchIndexProcessor`

## 4. Automatische Befüllung
- [ ] 4.0 Alle Befüllungspfade legen Dokumente über `addDocument(…, DocumentMetadata)` an (kein nachträgliches Setzen)
- [ ] 4.1 E-Mail in Akte speichern (`mail/SaveToCaseExecutor`, `MailContentUI`, Sidebar `SaveToCasePanel`): Eingangsdatum, Von, Bezeichnung = Betreff, Hierarchie Nachricht → Anlagen
- [ ] 4.2 Serverseitig automatisiertes Speichern von E-Mails (Mailbox-Automatik, `IntegrationService`) analog 4.1
- [ ] 4.3 beA speichern (`bea/SaveBeaMessageAction`, `BeaMessageContentUI`, beA-REST v8 Speichern-Pfade): Von per SafeId, Eingangsdatum, Bezeichnung, Hierarchie
- [ ] 4.4 Versand E-Mail (`SendAction`, `SendEncryptedAction`, `SendEmailFrame`), beA (`SendBeaMessageAction`), Fax (`VoipService`), ePost (`EpostLetterSendStatus`): An setzen falls leer; abgelegte gesendete Nachricht mit An/Bezeichnung; „ +N“ bei mehreren Empfängern
- [ ] 4.5 REST v7 E-Mail-Senden / v1-Upload-Pfade des Web-Clients prüfen und dieselbe Befüllung anwenden, wo Dokumente entstehen

## 5. REST
- [ ] 5.0 Bestehende Endpunkte v1–v8 prüfen, die Dokumente anlegen/ändern (u. a. `CasesEndpointV1`…`V8`, `DocumentsBinEndpointV8`, Office/WOPI `setDocumentContent`): Pfade und Formate bleiben unverändert, neue Metadaten dürfen nicht zurückgesetzt werden (Merge statt Neuaufbau des Beans)
- [ ] 5.1 POJOs `RestfulDocumentV8`, `RestfulDocumentMetadataV8`, `RestfulDocumentMetadataPatchV8`
- [ ] 5.2 `CasesEndpointV8`/`CasesEndpointLocalV8`: `GET /v8/cases/{id}/documents`, `PUT /v8/cases/documents/{id}/metadata`, `PUT /v8/cases/documents/metadata`, `GET /v8/cases/{id}/documents/keywords`, `GET /v8/cases/documents/{id}/messages`, `GET /v8/cases/documents/{id}`, `PUT /v8/cases/documents/{id}/parent` (Rollen + ACL, swagger-Annotationen)

## 6. Desktop-Client: Grundlage
- [ ] 6.1 `CaseDocumentsViewModel` (Dokumente, Filter, Sortierung inkl. neuer Sortierschlüssel, Auswahl, Hierarchie, Nachrichtenzähler, Rechnungen); `CaseFolderPanel` auf das Modell umstellen, ohne sichtbare Änderung (Regressionstest manuell: DnD, Kontextmenü, Auswahl, Suche)
- [ ] 6.2 Gemeinsame Actions für Kontextmenü/DnD/Tastatur aus `CaseFolderPanel`/`ArchiveFilePanel` so extrahieren, dass beide Ansichten sie nutzen
- [ ] 6.2a Kopieren/Verschieben in andere Akte (Kontextmenü `mnuCopyDocumentToOtherCase`/`mnuMoveDocumentToOtherCase`, Drag & Drop auf Akte) auf `copyDocumentsToCase`/`moveDocumentsToCase` umstellen, Client-Schleife entfernen; Ingo-Tool `move_document_to_case` (`ToolRegistry`) ebenso; REST-Pfade, die Dokumente zwischen Akten bewegen, prüfen und umstellen (Signaturen unverändert)
- [ ] 6.3 Schnellfilter um Bezeichnung, Schlagworte, Von/An erweitern
- [ ] 6.4 `UserSettingsKeys`: `CONF_DOCUMENTS_VIEWMODE` und `CONF_DOCUMENTS_TABLE_COLUMNS`; Speichern/Laden über `UserSettings` (serverseitig), Spaltenformat tolerant gegenüber neuen/unbekannten Spalten; `CONF_DOCUMENTS_LASTSORTMODE` um neue Sortierschlüssel erweitern
- [ ] 6.5 Wiederverwendbare Komponente `KeywordsEditor` (Eingabefeld mit Autovervollständigung + KI-Button): Menü aus `AssistantAccess.populatePromptMenu` für `REQUESTTYPE_CHAT` und `REQUESTTYPE_EXTRACT`, asynchrone Ausführung mit Dokumenttext, Parser (Komma/Semikolon/Zeilenumbruch, Aufzählungszeichen), Vorschlagsliste mit Checkboxen, Übernahme ins Feld ohne Speichern; deaktiviert ohne Prompts; Unit-Test für den Parser

## 7. Desktop-Client: Listenansicht
- [ ] 7.1 `DocumentEntryPanel` (+ `.form`) zweizeilig: Bezeichnung, Status-Icons (💬 Anzahl, 📎 Anzahl), zweite Zeile mit Von/An, Eingang, Schlagwort-Chips, Chips der aktiven Etiketten („+N“ bei Überlauf), Dateiname, Größe
- [ ] 7.1a Panel „Dokument-Etiketten“ (`jPanel4`/`documentTagPanel` in `splitDocuments`) aus `ArchiveFilePanel` (+ `.form`) entfernen; Etiketten für die Liste einmal je Akte über `getDocumentTagsForCase` laden (design D13)
- [ ] 7.2 Hierarchie in der Liste: Einrückung, Auf-/Zuklappen, Hinweis „Anlage zu …“
- [ ] 7.3 `ArchiveFilePanel` (+ `.form`): Vorschaubereich mit Reitern *Vorschau*, *Details* (Metadaten-Editor mit Kontaktauswahl, `KeywordsEditor` inkl. KI-Button und Etiketten-Bearbeitung – boolesch als Schalter, mehrwertig als Auswahl, bestehende Tag-Listener wiederverwenden), *Nachrichten* (Liste + „Nachricht zu diesem Dokument“ über `SendInstantMessageDialog`)
- [ ] 7.4 Klick auf 💬 öffnet Reiter *Nachrichten*

## 8. Desktop-Client: Tabellenansicht
- [ ] 8.1 `CaseDocumentsTreeTable` (`JXTreeTable`) + `TreeTableModel` + Renderer (Titel mit Einrückung, Von/An mit Pfeil, Datum, Schlagwort-Chips, Etiketten-Chips, Status-Icons), Optik gemäß design D7
- [ ] 8.2 Spaltenkonfiguration (Header-Kontextmenü, Reihenfolge, Breite, Sichtbarkeit) persistieren
- [ ] 8.3 Sortierung über Spaltenköpfe ans gemeinsame Modell gebunden
- [ ] 8.4 Inline-Bearbeitung Bezeichnung/Schlagworte (F2/Doppelklick in Zelle; Schlagwort-Zelleneditor mit KI-Button aus 6.5), Doppelklick auf Zeile öffnet Dokument
- [ ] 8.5 Nachrichten-Popup bei Klick auf 💬
- [ ] 8.6 Kontextmenü, DnD, Mehrfachauswahl, Nur-Lesen-Modus, Sperranzeige wie in der Liste

## 9. Desktop-Client: Umschaltung & Dialog
- [ ] 9.1 Segment-Schalter in der Toolbar von `CaseFolderPanel` (+ `.form`), `CardLayout`-Wechsel, Erhalt von Ordner-/Dokumentauswahl, Sortierung, Filter; Vorschau im Tabellenmodus ausblenden und Divider wiederherstellen
- [ ] 9.2 `DocumentPropertiesDialog` (+ `.form`): Einzel- und Stapelbearbeitung (Feld-Aktivierung, Schlagworte setzen/hinzufügen/entfernen, Etiketten setzen/entfernen, KI-Button; im Stapel Ausführung je Dokument mit Fortschritt und Vorschlägen je Dokument), Eintrag „Eigenschaften…“ im Kontextmenü

## 10. Web-Client
- [ ] 10.1 `cases.service.ts`: Dokumentliste über `GET /v8/cases/{id}/documents`, Metadaten-Update einzeln/Stapel, Schlagwort-Vorschläge, Nachrichten je Dokument; Modelle in `case.models.ts`
- [ ] 10.2 Dokumente-Reiter (`akten.component.ts`): Bezeichnung mit Dateinamen-Fallback, Von/An, Eingang, Schlagwort-Chips, 💬/📎-Indikatoren, aufklappbare Hierarchie
- [ ] 10.3 `document-actions` / `document-bulk-bar`: Metadaten bearbeiten (einzeln/Stapel), Nachrichten anzeigen; i18n DE/EN

## 11. Verifikation
- [ ] 11.1 `openspec validate add-document-metadata-and-views --strict`
- [ ] 11.2 Build (manuell durch den Entwickler)
- [ ] 11.3 Manuelle Tests: Ansicht/Spalten an zweitem Arbeitsplatz wiederhergestellt; KI-Button (chat/extract) im Desktop einzeln und im Stapel; bestehende REST-Endpunkte (v1 Umbenennen/Liste) lassen neue Metadaten unverändert; E-Mail mit Anlagen speichern → Hierarchie/Metadaten; beA-Versand → An; Umschalten der Ansichten mit Auswahl; Stapelbearbeitung; Parent löschen/wiederherstellen; Parent+Kinder in andere Akte kopieren und verschieben (Beziehungen + Metadaten erhalten); Ingo „Dokument verschieben“; aktive Etiketten als Chips, Bearbeitung in Details/Dialog; Feldsuche `schlagwort:`
- [ ] 11.4 REST v8 per curl: Liste, Update einzeln/Stapel, ACL-Ablehnung
- [ ] 11.5 Web-Client: Dokumente-Reiter und Stapelleiste gegen laufenden Server

## 12. Folge-Change (nicht Teil dieser Change)
- [ ] 12.1 Eigene OpenSpec-Change anlegen: MCP-Server/Ingo-Tools (`list_case_documents` u. ä.) geben Bezeichnung, Schlagworte, Eingangsdatum, Von/An und Eltern-Dokument aus
