## 1. Entity & Database

- [x] 1.1 Remove `refreshToken` field from `MailboxSetup.java` entity
- [x] 1.2 Add `clientSecretExpiry` (Date) field to `MailboxSetup.java` entity
- [x] 1.3 Create SQL migration script `V3_5_0_13__AddMailboxClientSecretExpiry.sql`
- [ ] 1.4 Create SQL migration script to drop `token_refresh` column from `mailbox_setup`

## 2. Data Transfer Objects

- [x] 2.1 Create `MailFolderDTO` in `j-lawyer-server-api`
- [x] 2.2 Create `MailMessageDTO` in `j-lawyer-server-api`
- [x] 2.3 Create `MailAttachmentDTO` in `j-lawyer-server-api`

## 3. Unified Mail Service Interface

- [x] 3.1–3.12 All methods added to `EmailServiceRemote` and `EmailServiceLocal`, Device Code Flow methods removed

## 4. Server-Side IMAP/SMTP Backend

- [x] 4.1–4.9 All IMAP/SMTP backend methods implemented in `EmailService.java`

## 5. Server-Side Graph API Backend

- [x] 5.1–5.12 All Graph API backend methods implemented, Client Credentials Flow replaces Device Code Flow

## 6. Server-Side Backend Dispatch

- [x] 6.1 Dispatch logic in `EmailService`: delegates to IMAP or Graph API based on `msExchange` flag
- [x] 6.2 `OutgoingMailProcessor` — not relevant (uses system-level SMTP, not per-user mailbox)

## 7. Mailbox Scanner Migration

- [x] 7.1 `MailboxScannerTask` — migrated to `processMailboxViaService()` using unified EmailService (listMessages, getMessage, getMessageAsEml, getAttachments, moveMessage, createFolder). Works for both IMAP and O365. Legacy `processMailbox()` retained as reference.

## 8. Client-Side Migration

- [x] 8.1 `MessageContainer` — backward-compatible: supports both DTO (`isServerBased()`) and legacy `Message`
- [x] 8.2 `LoadFolderThread` — server-based path via `runServerBased()` using `listMessages()` EJB
- [x] 8.3 `LoadFolderAction` — server-based path via `executeServerBased()` using `listMessages()` EJB
- [x] 8.4 `EmailInboxPanel` delete — server-based path using `deleteMessage()` EJB
- [x] 8.5 `EmailInboxPanel` mark-as-read — `MessageContainer.setRead()` handles server path internally
- [x] 8.6 `MoveAction` — server-based path using `moveMessage()` EJB
- [x] 8.7 `SendAction` — server-based path via `executeServerBased()` for `msExchange` mailboxes
- [x] 8.8 `EmailUtils` — DTO-based `reply()` overload with threading headers (`inReplyTo`, `references`); `SendEmailFrame` extended with `setThreadingHeaders()`/`getThreadingHeaders()`
- [x] 8.9 `MailContentUI` — server-based `setMessageServerBased()` populates CID cache from `MailAttachmentDTO` and displays message body
- [x] 8.10 `FolderContainer` — backward-compatible: supports both DTO and legacy `Folder`
- [x] 8.11 Client-side polling — `serverPollingTimer` in `EmailInboxPanel` calls `hasNewMessages()` every 30 seconds, repaints folder tree on new messages
- [ ] 8.12 Remove direct IMAP/SMTP connection code — **DEFERRED (legacy paths retained for non-server-based fallback; will be removed when all callers are migrated)**
- [x] 8.13 `O365OAuthCouplingDialog` — button action replaced with info message
- [x] 8.14 Mailbox config panel — `txtSecretExpiry` + `cmdSecretExpiry` with `MultiCalDialog`, load/save/reset/duplicate logic

## 12. Remaining Client-Side Direct Mail Access

- [x] 12.1 Migrate `SendEncryptedAction` — SMTP send retained (client-side PDF encryption), but Copy-to-Sent now uses `appendToFolder()` EJB instead of direct IMAP
- [x] 12.2 Migrate `SendEmailFrame` "Copy to Sent" — handled via `appendToFolder()` EJB (new server method)
- [x] 12.3 Migrate `SendEmailFrame` Draft saving — now uses `appendToFolder()` EJB to save drafts to server-side Drafts folder instead of direct IMAP
- [x] 12.4 Migrate `EmailUtils.sendReceipt()` — replaced 100+ lines of direct SMTP code with single `sendMail()` EJB call
- [x] 12.5 Remove dead code: `connect()` and `traverseFolders()` removed from `EmailInboxPanel`. `reconnectSelectedFolder()` retained — still referenced in legacy else-block
- [ ] 12.6 Remove dead code: Legacy IMAP paths in `LoadFolderThread.run()`, `LoadFolderAction.execute()`, `LoadEmailAction` — **DEFERRED (coupled with 12.7, requires coordinated removal)**
- [ ] 12.7 Remove dead code: Legacy `getMessage()`/`getFolder()` in `MessageContainer`/`FolderContainer` — **DEFERRED (still referenced at 50+ call sites in legacy else-blocks; requires big-bang removal of all else-blocks)**
- [x] 12.8 `MailSettingsTestAction` — confirmed no longer instantiated, can be deleted
- [ ] 12.9 Clean up `javax.mail` imports — **DEFERRED (all imports still needed by legacy else-blocks; removable only after 12.6+12.7)**

**Note:** Tasks 12.6, 12.7, 12.9 form a dependency chain — legacy else-blocks, deprecated methods, and javax.mail imports can only be removed together in a single coordinated pass after the server-based path is confirmed stable in production.

## 9. Server-Side Polling & New-Message State

- [x] 9.1–9.4 All polling and new-message state implemented in `EmailService.java`

## 10. Test Connection & Warnings

- [x] 10.1 Test connection button calls `testConnection()` via EJB for O365 mailboxes
- [x] 10.2 Client Secret expiry warning — shows warning in `MailboxSetupDialog` when within 30 days or expired

## 11. Validation & Testing

### IMAP-Mailbox

- [x] 11.1 Ordnerbaum wird korrekt geladen (Inbox, Sent, Trash, Unterordner)
- [x] 11.2 Nachrichten in INBOX auflisten — Betreff, Absender, Datum korrekt
- [x] 11.3 Nachricht anklicken — Body (plain + HTML) wird angezeigt
- [x] 11.4 Nachricht mit Anhängen — Attachment-Liste wird angezeigt, Download funktioniert
- [x] 11.5 Nachricht mit Inline-Bildern (CID) — Bilder werden im HTML-Body angezeigt
- [x] 11.6 E-Mail senden (plain text) — kommt beim Empfänger an
- [x] 11.7 E-Mail senden (HTML + Anhänge) — Anhänge korrekt, Copy-to-Sent funktioniert
- [x] 11.8 Antworten / Weiterleiten — Threading-Header (`In-Reply-To`, `References`) korrekt
- [x] 11.9 Nachricht in anderen Ordner verschieben — verschwindet aus Quellordner, erscheint in Zielordner
- [x] 11.10 Nachricht löschen — wandert in Papierkorb
- [x] 11.11 Papierkorb leeren — Nachrichten werden endgültig gelöscht
- [x] 11.12 Als gelesen / ungelesen markieren — Status wird korrekt gesetzt und in Ordnerbaum aktualisiert
- [x] 11.13 Neuen Ordner anlegen — erscheint im Baum
- [x] 11.14 Ordner löschen — verschwindet aus dem Baum
- [x] 11.15 Polling: neue Nachricht im externen Client senden → erscheint nach ~30s in j-lawyer
- [x] 11.16 Entwurf speichern — landet im Drafts-Ordner
- [x] 11.17 Verbindungstest-Button — meldet Erfolg bei korrekten Einstellungen, Fehler bei falschen
- [x] 11.18 MailboxScanner: eingehende Mail wird automatisch der Akte zugeordnet (Absender-Match)

### O365 / Graph API Mailbox

- [x] 11.19 Ordnerbaum wird korrekt geladen (Inbox, Sent Items, Deleted Items, Unterordner)
- [x] 11.20 INBOX-Ordner wird korrekt als Inbox erkannt (opaque GUID, nicht "INBOX")
- [x] 11.21 Nachrichten in INBOX auflisten — Betreff, Absender, Datum korrekt
- [x] 11.22 Nachricht anklicken — Body (HTML) wird angezeigt
- [x] 11.23 Nachricht mit Anhängen — Attachment-Liste wird angezeigt, Download funktioniert
- [x] 11.24 Nachricht mit Inline-Bildern — Bilder werden im HTML-Body angezeigt
- [x] 11.25 E-Mail senden (HTML + Anhänge) — kommt beim Empfänger an
- [x] 11.26 Antworten / Weiterleiten — Threading korrekt
- [x] 11.27 Nachricht verschieben — Quell- und Zielordner korrekt aktualisiert
- [x] 11.28 Nachricht löschen — wandert in Deleted Items
- [x] 11.29 Papierkorb leeren
- [x] 11.30 Als gelesen / ungelesen markieren
- [x] 11.31 Neuen Ordner anlegen
- [x] 11.32 Ordner löschen
- [x] 11.33 Polling: neue Nachricht → erscheint nach ~30s
- [x] 11.34 Entwurf speichern — landet im Drafts-Ordner
- [x] 11.35 Verbindungstest-Button — meldet Erfolg / Fehler
- [x] 11.36 Client Credentials Token-Refresh funktioniert (Token läuft nach ~60min ab)
- [x] 11.37 Client Secret Expiry-Warnung wird in Mailbox-Konfiguration angezeigt (wenn < 30 Tage)
- [x] 11.38 MailboxScanner: eingehende Mail wird automatisch der Akte zugeordnet

### Pre-Fetch (IMAP + O365)

- [x] 11.39 INBOX öffnen → Server-Log zeigt Pre-Fetch der Top 5 Nachrichten
- [x] 11.40 Top-5-Nachricht anklicken → Body erscheint sofort (kein getMessage-Call im Log)
- [x] 11.41 Nachricht außerhalb Top 5 anklicken → normaler getMessage-Call (Fallback)
- [x] 11.42 Cache-Invalidierung: nach Nachricht löschen/verschieben wird Pre-Fetch bei nächstem INBOX-Load neu getriggert

### Übergreifend

- [x] 11.43 Mehrere Mailboxen (IMAP + O365 gemischt) — beide erscheinen im Ordnerbaum, unabhängig bedienbar
- [x] 11.44 Nachricht zur Akte speichern (Drag & Drop oder Button) — .eml wird korrekt in der Akte abgelegt
- [x] 11.45 Empfangsbestätigung (Read Receipt) senden — funktioniert über EJB
- [x] 11.46 Verschlüsselte E-Mail senden (`SendEncryptedAction`) — PDF-Verschlüsselung + Copy-to-Sent via EJB
- [x] 11.47 Ordner ausblenden (MailboxSettings) — ausgeblendete Ordner erscheinen nicht im Baum
- [x] 11.48 Suche in Ordner — Suchergebnisse werden korrekt angezeigt
