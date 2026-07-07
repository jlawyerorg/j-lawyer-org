# REST-Gap-Analyse (Task 1.7)

**Ziel:** Für die Web-UI (die ausschließlich über REST kommuniziert, siehe `design.md`)
ermitteln, welche der vom Desktop-Client genutzten Server-Fähigkeiten bereits über die
REST-API v1–v8 verfügbar sind und welche additiv ergänzt werden müssen.

**Methode:** Abgleich der **24 EJB-Remote-Interfaces** (`j-lawyer-server-api/.../services/*Remote.java`,
= vollständige vom Swing-Client genutzte Fähigkeitsfläche) gegen die **REST-Endpunkte**
(`j-lawyer-io/.../rest/v1..v8`). Stand: Branch `feature/add-web-client`.

**Nicht-Bruch-Regel:** Alle Ergänzungen erfolgen additiv in der jeweils aktuellen
API-Version (derzeit v8) bzw. einer neuen Version; bestehende Versionen bleiben
unverändert (siehe `project.md`, „REST API Versioning").

---

## 1. Fähigkeitsfläche (Remote-Interfaces nach Methodenumfang)

| Remote-Interface | Methoden | REST vorhanden? |
|---|---:|---|
| ArchiveFileServiceRemote (Akten) | 177 | ✅ v1–v7 (umfangreich) |
| SystemManagementRemote | 86 | ⚠️ teilweise (v7 administration) |
| BeaServiceRemote | 45 | ✅ v8 (umfangreich) |
| IntegrationServiceRemote | 44 | ❌ fehlt |
| SecurityServiceRemote | 32 | ⚠️ teilweise (v1/v6) |
| CalendarServiceRemote | 29 | ⚠️ teilweise (v4: nur Listen + Fristen) |
| AddressServiceRemote (Kontakte) | 25 | ✅ v1/v2/v5/v7 |
| VoipServiceRemote | 24 | ❌ fehlt |
| EmailServiceRemote | 24 | ✅ v7 (umfangreich) |
| InvoiceServiceRemote | 21 | ⚠️ Kern vorhanden (v7), Erweiterungen fehlen |
| FormsServiceRemote | 18 | ✅ v1 |
| AddressDocumentServiceRemote | 17 | ✅ v7 (contacts/documents) |
| MessagingServiceRemote | 15 | ✅ v7 (Basis) |
| SingletonServiceRemote | 13 | ⚠️ teilweise |
| DropscanServiceRemote | 11 | ❌ fehlt |
| TimesheetServiceRemote | 7 | ✅ v8 (vollständig) |
| CustomerRelationsServiceRemote | 7 | ❌ fehlt |
| SearchServiceRemote | 5 | ✅ v8 |
| ReportServiceRemote | 3 | ✅ v7 |
| PaymentServiceRemote | 1 | ✅ v8 |
| DataBucketLoaderRemote | 1 | ✅ v6 |
| DocUtilityServiceRemote | 0¹ | — |
| ContainerLifecycleBeanRemote | 0¹ | — (interner Lifecycle, nicht UI-relevant) |
| ClaimLedgerServiceRemote | 0¹ | — (Interface leer; Forderungskonto liegt in Kontoauszügen/account entries, v7 teils vorhanden) |

¹ 0 = keine eigenen Methoden. `DocUtility`/`ContainerLifecycleBean`/`ClaimLedger` sind leer bzw. rein intern — keine eigene UI-Fähigkeitsfläche.

---

## 2. Abdeckung nach Web-UI-Modul

| Modul | REST-Stand | Bewertung |
|---|---|---|
| **Akten** | v1–v7: CRUD, Dokumente (CRUD/Content/to-PDF/OCR/Tags/Papierkorb), Beteiligte, Fristen (v6 CRUD), Ordner (v3), Historie, Suche, Formulare | ✅ **abgedeckt** |
| **Kontakte** | v1/v2/v5/v7: CRUD, Tags, Fälle, Dokumente, externe IDs | ✅ **abgedeckt** |
| **Dokumente** | via Akten/Kontakte + Vorlagen (v6), Volltextsuche (v8) | ✅ **abgedeckt** (Office-Editing = eigenes Thema, `design.md` Decision 3) |
| **Zeiterfassung** | v8: Timesheets/Positionen, Start/Stop, laufende Positionen | ✅ **abgedeckt** |
| **Volltextsuche** | v8: fulltext + reindex | ✅ **abgedeckt** |
| **E-Mail** | v7: Postfächer, Ordner, Nachrichten CRUD, senden, EML, Anhänge | ✅ **abgedeckt** |
| **beA** | v8: Postfächer, Ordner, Nachrichten, eEB, Identitäten, Journal | ✅ **abgedeckt** |
| **Reporting** | v7: list + invoke | ✅ **abgedeckt** |
| **Rechnungen/Zahlungen** | v7 (Rechnungen/Positionen/Kontoauszüge/Typen/Pools) + v8 (Payments) | ⚠️ **teilweise** — Kern da; ZUGFeRD/XRechnung-Export, Rechnungs-Belegerzeugung, Bankauszug-Import, Girocode/EPC-QR, Mahnwesen prüfen/ergänzen |
| **Kalender/Fristen** | v4: nur `list calendars` + Fristen (duedates); Fristen-CRUD via v6 cases | ⚠️ **teilweise** — **Termin-/Event-CRUD, Erinnerungen, Kalender-Setups (CalDAV), Terminvorlagen, Feiertage, Terminkonflikte, Wiedervorlagen-Verwaltung** fehlen als eigenständige Endpunkte |
| **Messenger** | v7: submit, delete, since, mentions/done | ⚠️ **Basis** — reicht funktional; Echtzeit erfordert Push (siehe Abschnitt 4) |
| **Administration/Einstellungen** | v6 (users/groups/roles), v7 (administration: backup/jobs; configuration: options), v7 webhooks | ⚠️ **teilweise** — großer Teil von `SystemManagement` (86) und `Security` (32) fehlt: Systemüberwachung/Monitoring, Nutzerprofil/Passwort, detaillierte Konfiguration |
| **Kommunikation: VoIP/Fax/E-POST** | — | ❌ **fehlt vollständig** (`VoipServiceRemote`, 24): `initiateCall/Fax/Sms`, `sendLetter`, `sendRegisteredLetter`, E-POST-Queue, `getLetterStatus`, `getBalance` |
| **KI-Assistent (Ingo)** | — | ❌ **fehlt vollständig** (`IntegrationService`): `submitAssistantRequest`, Assistant-Configs/Prompts/Replacements/Models/Capabilities, `getAssistantRequestLog/Status` |
| **E-Mail-Vorlagen** | v6 (lesen: email templates) | ⚠️ **teilweise** — CRUD in `IntegrationService` (`saveEmailTemplate`, `deleteEmailTemplate`, `renameEmailTemplate`, `duplicateEmailTemplate`) fehlt |
| **Serienschreiben / CRM** | — | ❌ **fehlt vollständig** (`CustomerRelationsService`, 7): Kampagnen (`createCampaign`, `addToCampaign`, `listAddressesForCampaign`, `getDocumentForAddress`) |
| **Scan-Eingang / DropScan** | — | ❌ **fehlt** (`DropscanService` 11; `IntegrationService` observed files: `getObservedDirectoryContent`, `addObservedFile`, `assignObservedFile`, `getObservedFilePreview/Text`, `performOcrForObservedFile`) |
| **Integration-Hooks/Webhooks-Verwaltung** | v7 (webhooks lesen) | ⚠️ **teilweise** — Hook-CRUD (`addIntegrationHook`/`update`/`remove`, `getHookTypes`) fehlt |

---

## 3. Ergänzungsbedarf, priorisiert nach Migrationsphasen

Ausgerichtet am Phasenplan in `design.md` / `tasks.md`.

### Phase 0/1 (Fundament + MVP) — Voraussetzung
- **Auth-Login-Endpunkt** (session-/tokenbasiert) — existiert nicht; nur Basic Auth + `security/metadata`. Blockiert jede Anmeldung. Abstimmen mit `add-two-factor-auth`.
- **Kalender-Endpunkt (neue Version)**: Termin-/Event-CRUD, Erinnerungen, `getConflictingEvents`, Kalender-Setups, Feiertage. Größte inhaltliche Lücke der Kernmodule.

### Phase 2/3 (Kernmodule + Finanzen/Kommunikation)
- **Rechnungswesen-Erweiterungen**: ZUGFeRD/XRechnung-Export/-Beleg, Bankauszug-Import, Girocode/EPC-QR; Forderungskonto/Mahnwesen baut auf den Kontoauszügen/account entries (v7) auf.
- **E-Mail-Vorlagen-CRUD** (aus `IntegrationService`).
- **Messenger-Echtzeit** über Push (siehe Abschnitt 4).

### Phase 4 (Parität & Komfort)
- **VoIP/Fax/E-POST-Endpunkte** (`VoipService`, komplett).
- **KI-Assistent-Endpunkte** (`IntegrationService` Assistant-Teil, komplett).
- **Serienschreiben/CRM-Endpunkte** (`CustomerRelationsService`, komplett).
- **Scan-Eingang/DropScan + observed files** (Ersatzweg für den Desktop-Scanner-Workflow, vgl. `design.md` Decision 3).
- **Administration/Monitoring-Erweiterungen** (Teile von `SystemManagement`), Nutzerprofil/Passwort.
- **Integration-Hooks-CRUD**.

---

## 4. Übergreifende Feststellungen (nicht durch REST-CRUD allein lösbar)

- **Authentifizierung**: Browser-tauglicher Login-Flow fehlt (heute Basic Auth). Siehe
  `design.md` „Authentifizierung" + `add-two-factor-auth`.
- **Echtzeit/Push**: Messenger, Kalender-/Wiedervorlage-Erinnerungen und Status-Badges
  brauchen serverseitigen Push (WebSocket/SSE). REST-Polling ist Übergangslösung. Der
  Desktop-Client löst dies heute über clientseitiges Polling + IMAP-IDLE + In-Process-
  Event-Bus (nicht über REST).
- **Kein Remote-Code/Supply-Chain**: rein clientseitig/Build-seitig (CSP, self-host,
  gehärtete Build-Kette) — kein REST-Thema (siehe `design.md` Decision 2c).
- **Office-Bearbeitung** (Download→native App→Watch→Re-Upload): keine REST-Frage, sondern
  Architektur (WOPI vs. Download/Upload, `design.md` Decision 3).

---

## 5. Fazit

Die REST-API deckt die **Kernmodule** (Akten, Kontakte, Dokumente, Zeiterfassung, Suche,
E-Mail, beA, Reporting) bereits weitgehend ab — MVP und Phase 2 sind damit überwiegend
ohne neue Endpunkte realisierbar (Ausnahme: **Auth-Login** und **Kalender-Event-CRUD**).
Der additive Ergänzungsbedarf konzentriert sich auf **Kalender**, **Finanz-Erweiterungen**
sowie die Phase-4-Module **VoIP/Fax/E-POST**, **KI-Assistent**, **CRM/Serienschreiben**
und **Scan-Eingang** — allesamt heute ganz ohne REST. Keine der Ergänzungen bricht
bestehende Versionen.
