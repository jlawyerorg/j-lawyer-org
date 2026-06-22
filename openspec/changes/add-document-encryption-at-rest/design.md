# Design: Encryption at Rest for Case Documents and Previews

## Context

Document bytes for cases are stored under the data directory
(`jlawyer.server.basedirectory`, default `/opt/jboss/j-lawyer-data/`):

```
<basedirectory>/
├── archivefiles/<archiveFileId>/<documentId>          # originals + versions (raw bytes)
├── archivefiles-preview/<archiveFileId>/<documentId>.text   # extracted text for search
│                                       /<documentId>.pdf    # rendered PDF preview
├── searchindex/                                       # Lucene index (extracted plaintext)
├── templates/ emailtemplates/ mastertemplates/        # templates
```

All document content I/O funnels through three static methods in
`com.jdimension.jlawyer.server.utils.ServerFileUtils`
(`readFile(File)`:814, `writeFile(File,byte[])`:850, `createFile(String,byte[])`:857),
invoked from `ArchiveFileService` (`addDocumentImpl`:~1805, `setDocumentContent`:~2127,
`getDocumentContentImpl`:~2321) and `PreviewGenerator`. The storage location may be a
local disk or a `VirtualFile` backend (Samba/SFTP/FTP) — encryption is applied to the
byte payload **above** the storage layer, so it protects remote backends too.

### Scope Boundary (by storage path, not content type) — Q5

Encryption scope is defined by **where bytes are written**, not by content type. Every
document under `archivefiles/` is in scope regardless of kind, because they all flow
through the same `addDocumentImpl`/`getDocumentContent` byte[] path:
- **Dictation/audio** are ordinary documents — `dictateSign` is only a metadata field on
  `ArchiveFileDocumentsBean` (`ArchiveFileService.java:1811`); the audio bytes are stored
  at `archivefiles/<id>/<docId>` like any other document. **No special handling.**
- **Saved e-mails** (`.eml`) are likewise saved as documents under `archivefiles/`; there
  is no separate on-disk e-mail store (live mail stays on the IMAP server).

Sibling folders under `basedirectory` and their disposition:
- `archivefiles/`, `archivefiles-preview/` → **in scope** (C2).
- `searchindex/` (Lucene) → out of scope, accepted residual risk (C2).
- `emailtemplates/`, `mastertemplates/`, `letterheads/` → templates, out of scope.
- `faxqueue/` → transient outbound-fax spool; may briefly hold document content as
  plaintext. Out of scope, noted as a minor transient residual (not one of the two
  named folders).
- `databucket/` temp transfers live under `java.io.tmpdir` and are handled by C5.

An existing `com.jdimension.jlawyer.security.Crypto` performs AES-256-GCM but with a
**hard-coded salt and a fixed nonce** (`Crypto.java:685-699`) and a string-only API.
A fixed GCM nonce reused across many files breaks confidentiality and integrity, so
`Crypto` is unsuitable for file content and a new primitive is required.

## Goals / Non-Goals

**Goals**
- Confidentiality + integrity of `archivefiles/` and `archivefiles-preview/` content
  on the storage medium (defends against stolen disk/backup/snapshot and read access
  to the storage backend).
- Transparent to all readers/writers: REST (v1–v7), EJB clients, search indexing,
  preview/OCR/PDF generation behave identically for authenticated users.
- Switchable on without a flag day (mixed encrypted/plaintext coexistence).
- Safe, resumable migration both directions; cheap key rotation.

**Non-Goals**
- Protecting against a fully compromised running server that already holds the
  unlocked key in memory (live-memory attacker). At-rest ≠ in-use protection.
- Per-user / per-case cryptographic access control (server must decrypt for
  search/preview, so a single firm-wide key domain is assumed unless decided otherwise).
- Encrypting the Lucene index, the database, or templates (residual risk; see below).
- End-to-end / client-side encryption.

## Decisions

Confirmed with the product owner (2026-06-22):
- **C1 Key source:** firm-wide master **key file on the server**, stored outside the
  data directory with restrictive file permissions; unlocked automatically at startup
  (unattended restarts supported). Threat model: protects against stolen
  disk/backup/snapshot and read access to the storage backend — **not** against an
  attacker who already has live access to the running server's memory.
- **C2 Scope:** encrypt **only** `archivefiles/` and `archivefiles-preview/`. The
  Lucene `searchindex/` stays plaintext — an **accepted residual risk** (extracted
  text recoverable with disk access). DB and templates are out of scope.
- **C3 Key domain:** a **single firm-wide key domain** (one master key + per-file
  data keys via envelope). The server can decrypt for search/preview/OCR.
- **C4 Migration:** **administrator-triggered, resumable, online batch** migration,
  **with** reverse migration (decrypt-all) and key rotation supported.
- **C6 Keystore provisioning + export via the client (and REST):** symmetric upload/
  download of the existing keystore.
  - **Upload:** a server missing its key (e.g. after migration to a new host — the key is
    intentionally outside `basedirectory` and the backup, per C1) is unlocked by a
    `sysAdminRole` **uploading the backed-up keystore**; the server writes it to the
    configured location (`chmod 600`) and unlocks the document subsystem.
  - **Download/export:** a `sysAdminRole` can **download the keystore** — this is the
    mechanism that produces the off-server backup the Q3 gate requires. Offered when new
    key material is created (initial activation and rotation) and on demand thereafter.
    Note: after rotation the old backup no longer decrypts the re-wrapped data, so the new
    key must be downloaded and its backup confirmed as part of completing rotation.
  - Both directions are only a transport for the existing key — no new key-derivation
    scheme. Hard conditions: `sysAdminRole` only, audited, **TLS/HTTPS required** (raw key
    bytes on the wire), key bytes never logged; upload additionally **validates the key
    matches the data** (fingerprint / test-unwrap) and performs **no silent overwrite**
    (explicit replace required). A passphrase-based recovery keyslot was considered and
    **rejected** — it would add an offline brute-force surface and, if the wrapper
    travelled in the backup, weaken the C1 stolen-backup guarantee.
- **C5 DataBucket path:** the `DataBucket` streaming path
  (`getDocumentContentBucket`, `ArchiveFileService.java:5875`) **must** be supported.
  Today it `Files.copy`s the source file into a temp bucket file under
  `java.io.tmpdir/databucket/<id>` and `DataBucketUtils.fillBucket`/`nextBucket` then
  serve chunks from it via `RandomAccessFile`. With encryption on, the server decrypts
  the source file **server-side into the temp bucket file** (streaming GCM decrypt so a
  large file is not held wholly in memory), and the existing chunked bucket reads serve
  the resulting **plaintext** unchanged. Consequence: the temp bucket file is plaintext
  on the server's temp dir for the lifetime of the transfer — same exposure as today
  (the temp copy is already plaintext) — and must be reliably removed
  (`removeLocalFile`/`removeStaleBuckets`) after transfer. A symmetric path applies if
  buckets are ever used for uploads (decrypt-on-read / encrypt-on-assemble).

## Design Notes (recommended technical defaults)

- **D1 Algorithm:** AES-256-GCM, **fresh random 96-bit IV per file**, 128-bit auth
  tag. AES-NI/ARMv8-crypto makes this multi-GB/s; negligible for typical KB–MB docs.
- **D2 Envelope encryption:** each file gets a random data key (DEK); the DEK is wrapped
  by a firm-wide master key (KEK). Stored as a per-file header so key rotation only
  re-wraps DEKs (no bulk re-encryption of content).
- **D3 On-disk format:** `magic | version | algoId | wrappedDEK | IV | ciphertext+tag`.
  The magic prefix lets `read` auto-detect encrypted vs. plaintext files → enables
  mixed-state coexistence and idempotent migration.
- **D4 Injection point:** a dedicated document-storage helper (or a scoped wrapper),
  **not** blanket encryption inside `ServerFileUtils` — only paths under
  `archivefiles/` and `archivefiles-preview/` are encrypted, leaving templates and
  other `ServerFileUtils` callers untouched.
- **D5 Fail-closed:** if the feature is enabled but the key is missing/invalid at
  startup, document read/write fails loudly rather than silently serving/over-writing.
- **D6 Key derivation:** KEK derived with PBKDF2WithHmacSHA512 (or Argon2 if a lib is
  acceptable) from the configured secret with a **random per-install salt** stored
  alongside the wrapped master key — never the hard-coded salt from `Crypto`.

## Risks / Trade-offs

- **Key loss = total data loss.** No key escrow → unrecoverable documents. Mitigation:
  mandatory documented key backup, refuse to start migration until the operator
  confirms a key backup exists.
- **Large-file memory pressure.** The plain `readFile` loads whole files into a `byte[]`
  (2 GB cap); GCM verifies the tag over the full content anyway, so big scans (100s of
  MB) double memory transiently on that path. The `DataBucket` path avoids this by
  streaming-decrypting to a temp file (C5). Note: streaming GCM only surfaces verified
  plaintext after the tag is checked at stream end — acceptable here because the temp
  bucket file is server-local and removed after transfer.
- **Migration window.** Encrypting an existing multi-GB data directory is IO/CPU-heavy
  and must be resumable, idempotent (header detection), and online (no downtime).
- **Backups become ciphertext.** Good for at-rest, but `j-lawyer-backupmgr` restores
  are useless without the key → backup/restore docs must cover key handling.
- **Residual plaintext leaks outside scope:** the Lucene index and `.text` previews
  contain extracted document text; the DB holds metadata. If the index stays plaintext,
  an attacker with disk access can still recover substantial text. Must be an explicit,
  accepted decision.
- **Performance:** symmetric AES-GCM cost is small, but **per-key PBKDF2 must run once
  at startup**, never per file. Preview/OCR/PDF still need plaintext in memory.
- **Nextcloud share leaves the encryption boundary (Q14).** Sharing a document to
  Nextcloud transfers it **unencrypted** — by design. This is only safe if the cloud
  sync obtains content through the decrypting service layer
  (`ArchiveFileService.getDocumentContent`), **not** by reading raw files off disk. If
  any cloud/external path reads the data directory directly, it would now get
  ciphertext and break. Action: verify the cloud-sync read path and document that the
  Nextcloud copy is plaintext, governed by Nextcloud's own at-rest protection.

## Migration Sizing (Q9)

Typical data directory: **20–150 GB**. Forward migration reads every file once and
rewrites it once, so it is **I/O-bound, not CPU-bound**: AES-256-GCM with AES-NI runs at
multiple GB/s, negligible next to disk throughput. Rough total I/O ≈ 2× data size
(40–300 GB read+write).

- On HDD-class storage (~100 MB/s effective) that is ~7–50 min of pure transfer; on
  SSD/NVMe, minutes. Real runtime is usually dominated by **IOPS / per-file overhead**
  (legal data is many small PDFs/text previews), so plan for longer than raw-throughput
  math suggests and report progress + ETA rather than promising a fixed window.
- Implications baked into the design:
  - **Online & resumable** (already required) — no maintenance window for 150 GB.
  - **Crash-safe in-place rewrite**: encrypt to a temp file in the same directory,
    fsync, then atomic rename over the original, so an interrupted file is never left
    half-written; header detection makes re-runs idempotent.
  - **Throttling**: allow the operator to bound migration I/O (e.g. run off-hours /
    limited rate) so a 150 GB run does not starve live document access.
  - **No extra steady-state cost**: per-document crypto overhead at runtime is marginal;
    expected overhead budget is < ~5% added latency per op (AES-NI assumed, see Q10).

## Migration Plan

1. Ship the crypto + storage layer first, **disabled by default**; reads auto-detect
   so deploying the code changes nothing until enabled.
2. Operator initializes the key, backs it up, and **proves the backup at activation** by
   entering the key fingerprint/recovery code (Variant B). This gate fires before the
   first key-dependent operation — enabling encrypted writes or starting migration,
   whichever comes first — not only before migration.
3. Admin triggers forward migration: walk `archivefiles/` + `archivefiles-preview/`,
   encrypt-in-place any plaintext file (skip already-encrypted via header), resumable
   with progress + audit log. New writes are encrypted immediately once enabled.
4. Rollback: reverse migration decrypts in place and the feature can be disabled.
5. Key rotation: re-wrap DEKs from old KEK to new KEK (fast, content untouched).

## Key Storage, Document Metadata & Initialization

Answers to the architecture follow-ups (these refine C1; they do not change the
confirmed decisions).

### Where the key / keystore (password) lives
With C1 (auto-unlock, key on the server) two concrete shapes are viable:
- **(a) Raw key file** — a 256-bit KEK in a file **outside `basedirectory`** (e.g.
  `/opt/jboss/j-lawyer-keys/` or WildFly `standalone/configuration/`), `chmod 600`,
  owned by the WildFly OS user. No separate password; the protection *is* the
  filesystem ACL.
- **(b) Java KeyStore (PKCS12)** holding the KEK, protected by a keystore password. The
  password must then be readable at startup — a WildFly **system property / env var**
  (e.g. `jlawyer.encryption.keystore.password` set in `standalone.conf` / a Docker or
  systemd secret), itself permission-protected.

Honest crux for unattended restart: on a single host the keystore password cannot be
guarded by another *independent* secret — it bottoms out at OS file permissions either
way. So (a) and (b) are roughly equal on one host; (b) only adds value if the password
is injected at boot from somewhere a disk thief does not also get. **Recommendation:**
option (a) (or (b) with the password in an env var), key located **outside the data
directory** and **excluded from document backups**. Threat model restated precisely:
this defends **stolen backups of the data directory** (key not in the backup) and
**detached/remote storage backends** (Samba/SFTP/FTP hold only ciphertext, key stays on
the app server). It does **not** defend a full-host compromise or a full-disk theft that
captures both the data dir *and* the key location.

### Keystore location per OS (server host)
The keystore lives on the **server** (WildFly), not the client (the client only saves a
*downloaded* backup wherever the admin chooses). The path is configurable via the
server setting (task 4.1); recommended defaults, always **outside `basedirectory`**, with
restrictive permissions, and **excluded from backups**:
- **Linux:** `/etc/j-lawyer-server/keystore/keystore.p12` (alt. `/opt/jboss/j-lawyer-keys/`);
  dir `700`, file `600`, owned by the WildFly service user.
- **Windows:** `C:\ProgramData\j-lawyer\keystore\keystore.p12`; ACL limited to the service
  account + Administrators.
- **macOS:** `/Library/Application Support/j-lawyer/keystore/keystore.p12`; `600`, service
  user. (The macOS Keychain is user-scoped and unsuitable for a headless daemon.)
- **Docker:** a dedicated mounted volume or Docker secret, **not** under the data volume
  (`/var/docker_data/j-lawyer-data/` → `/opt/jboss/j-lawyer-data/`, which is created
  `chmod -R 777`). Must persist across container recreation and stay out of the data
  backup.

### Source of truth for "is this file encrypted?"
The **per-file header is authoritative** (D3): encryption state is determined by reading
the file's magic bytes, which is what makes mixed-state coexistence and idempotent,
resumable migration work — and it is robust if the DB and files ever diverge (e.g.
restore from mismatched backups). Crucially, `archivefiles-preview/` files have **no DB
row**, so they can only rely on the header. Therefore **all crypto material lives in the
file header**, never in the DB:
`magic | formatVersion | algoId(AES-256-GCM) | keyVersion | wrappedDEK | iv | ciphertext+tag`.

### Per-document DB attributes (case_documents)
The header is sufficient for correctness, so the DB needs only **lightweight bookkeeping
hints** (denormalised, kept in sync on write/migrate, never the source of truth) to power
migration progress and "how many docs still plaintext?" queries without scanning files:
- `encryption_state` (e.g. `NONE` / `ENCRYPTED`) — migration bookkeeping.
- `key_version` (INT) — which KEK generation wrapped this file (rotation reporting;
  authoritative copy is in the header).
- **`size` semantics unchanged**: keeps meaning the **logical plaintext length** clients
  expect, *not* the larger on-disk ciphertext (header + tag). Implementation must report
  plaintext size even though the file on disk is bigger.
- No new columns for the `.text`/`.pdf` previews (they have no row) — header only.

Caveat: the legacy `content MEDIUMBLOB` column can still hold document bytes for
not-yet-`migrateDocument`- d records. Bytes living in the DB blob are **out of scope** for
file-at-rest encryption (that is DB-level encryption); ensure content is file-based, or
document the residual.

### Where the "encryption is active" state lives — three separate stores
1. **Config / intent** — the `encryption enabled` flag and key-source config live in the
   server-wide `server_settings` table (the existing key/value store accessed via
   `SystemManagement.getSetting/setSetting` + `ServerSettingsKeys`, e.g.
   `jlawyer.server.encryption.enabled`; absent/false ⇒ off). It drives whether new writes
   encrypt and whether fail-closed applies, and it travels with the DB backup.
2. **Actual per-file data state** — authoritative in the **file header**; the
   `encryption_state`/`key_version` columns on `case_documents` are only denormalised
   hints. The "enabled" flag does **not** imply every file is encrypted (mixed-state /
   partial migration); the header is the truth per file.
3. **Key + key metadata** — the KEK, key version, KDF salt and fingerprint live in the
   keystore file **outside `basedirectory`**, not in the DB, excluded from backups.

This separation is exactly what produces the migrated/locked case: after a move the DB
flag says "enabled" but the key file is absent ⇒ scoped fail-closed (Q12) ⇒ offer the
keystore upload (C6).

### Initial key/keystore creation — NOT a Flyway migration
Key generation must **not** run inside a Flyway migration, because Flyway runs
automatically at deploy for *every* install while the feature default is **off** —
silently generating keys or enabling crypto would be wrong, and key material is
filesystem/server state, not schema state. Split it:
- **Flyway SQL migration** (`V<maj>_<min>_<patch>_<seq>__AddDocumentEncryptionColumns.sql`)
  adds only the two bookkeeping columns to `case_documents`. Pure schema, safe and inert
  when the feature is off; runs everywhere.
- **Key/keystore creation is an explicit, audited admin action** ("Initialize
  encryption"): generate a random 256-bit KEK via `SecureRandom`, write the key
  file/keystore to the configured location with restrictive permissions, persist key
  metadata (key version, per-install KDF salt if a passphrase-derived KEK is used)
  **alongside the key — outside the DB and outside backups**, refuse to overwrite an
  existing key, and require the operator's key-backup confirmation (Q3) before any
  forward migration may start.

## Admin Control Surface

Authorization: every encryption administration operation is restricted to the
**`sysAdminRole`** (not the ordinary `adminRole`) — `@RolesAllowed({"sysAdminRole"})` on
the EJB operations, the same role `SystemManagement` already uses for sensitive actions.
This holds for both the client and REST surfaces.

All administrator operations — enabling/disabling encryption, the key-backup
confirmation (Q3), keystore upload (C6), forward migration, reverse migration, and key
rotation — are **triggered from the client** and **executed on the server**. This follows
the existing long-running-admin-op pattern: the "Suchindex neu erfassen" button in
`SearchIndexOptionsDialog` (`j-lawyer-client/.../configuration/SearchIndexOptionsDialog.java`)
which calls a server service and lets the work proceed server-side.

- A new admin dialog (under `AdminConsoleFrame`, alongside `ServerSettings` /
  `SearchIndexOptionsDialog`) hosts the controls; `.form` files kept in sync for the GUI
  builder.
- The client is only the control/monitoring UI. Migration/rotation/decryption run on the
  server (it holds both the data and the key) as background jobs that **survive client
  disconnect** and report progress/status the client can poll — consistent with the
  resumable/online migration requirement.
- **Headless availability via REST (Q15 → yes):** the same operations are also exposed
  over the REST API (latest version **v8**) so server-only/Docker installations can be
  administered without the desktop client. Both surfaces call the same server operations.
  Model: `AdministrationEndpointV7` already provides the right template — a `/backup`
  POST plus async `/jobs/{jobId}/status` and `/jobs/list` endpoints via
  `SingletonService`; the encryption endpoints reuse that kick-off-then-poll job pattern.
  Admin-only (HTTP Basic), **HTTPS required** for the keystore-upload endpoint (raw key
  bytes on the wire), never logged. New endpoints only — existing versions unchanged; the
  served `swagger.json` is regenerated and re-baselined.

## Compliance Drivers (Q13)

Confirmed drivers: **DSGVO** and the German attorney confidentiality regime
(**BRAO / StGB**).
- **DSGVO Art. 32** ("state of the art" technical/organisational measures) and Art. 5(2)
  (accountability) — satisfied by AES-256-GCM at rest plus audit logging of bulk crypto
  operations. Art. 33/34 breach exposure is reduced because a stolen disk/backup yields
  only ciphertext.
- **Attorney confidentiality**: StGB §203 (Verletzung von Privatgeheimnissen) and the
  BRAO/BORA confidentiality duties make unauthorised disclosure of client documents a
  professional and criminal-law risk; encryption at rest is a proportionate safeguard.
- No specific cipher/key length is mandated by these; **AES-256-GCM with per-file random
  IV is comfortably within state of the art**, so no algorithm change is forced.

## Open Questions

Resolved: Q1, Q2, Q4, Q6, Q7 → C1–C4; Q8 → C5; Q9 (size 20–150 GB, ~5% overhead OK) →
Migration Sizing; Q10 (AES-NI may be assumed) → yes; Q11 (default **off** for new
installs and upgrades) → see config requirement; Q13 (DSGVO + BRAO/StGB) → Compliance
Drivers; Q14 (Nextcloud share sends plaintext) → see Risks + task 6.4; Q5 → see Scope
Boundary (no separate dictation/audio or e-mail store exists).

Resolved: Q3 → the key-backup gate fires at **activation** (first dependency on the key:
enabling encrypted writes **or** starting migration, whichever first), and confirmation
uses **Variant B** — the admin must type the key fingerprint/recovery code derived from
the actual key material (not a mere checkbox).

Resolved: Q12 → **Option A, scoped fail-closed**. On missing/invalid key the document
subsystem fails closed (read+write refused, never plaintext-fallback or raw-ciphertext),
while the rest of the application keeps running; a startup check surfaces a loud
error/health warning and keeps document functions disabled rather than appearing healthy.

Resolved: Q15 → **yes**, the admin operations are also exposed over the REST API (v8),
backed by the same server operations as the client (see Admin Control Surface).

All design questions are now resolved; the proposal is ready for review.
