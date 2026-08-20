# Change: Encryption at Rest for Case and Contact Documents and Previews

## Why

Documents are the most sensitive data the system holds (attorney-client
privileged material, German DSGVO Art. 9 special-category data, etc.) — both case
documents and the documents stored directly on a contact (powers of attorney, ID
copies, mandate/fee agreements). Today the contents of the `archivefiles/` /
`archivefiles-preview/` (case documents + previews) and `addressfiles/` /
`addressfiles-preview/` (contact documents + previews) folders under the data
directory (`jlawyer.server.basedirectory`, default `/opt/jboss/j-lawyer-data/`) are
stored as **plaintext** on disk. Anyone with access to the filesystem, a stolen disk, a
backup archive, a snapshot, or the underlying Samba/SFTP/FTP storage backend can
read every document without authenticating to the application.

Encryption at rest closes that gap: the bytes on the storage medium become
unreadable without the firm's key, supporting DSGVO Art. 32 ("state of the art"
technical measures) and common law-firm/insurer security requirements — while
keeping the application's behaviour (search, preview, OCR, REST/EJB access)
unchanged for authenticated users.

## What Changes

- **NEW capability `document-encryption`**: transparent, authenticated encryption
  of all bytes written to and read from the document-storage folders —
  `archivefiles/` and `archivefiles-preview/` (case documents) **and** `addressfiles/`
  and `addressfiles-preview/` (contact documents) — applied at the document-storage
  layer so that business logic, REST endpoints, search indexing and preview generation
  are unaffected. Case and address documents already share the same storage and
  preview code paths (`ServerFileUtils`, `PreviewGenerator`), so the same scoped
  injection covers both.
- A new **per-file AES-256-GCM** crypto primitive with a **random IV/nonce per
  file** and a self-describing, versioned **on-disk file header** (magic bytes,
  format version, algorithm id, IV, wrapped key, auth tag). The existing
  `Crypto` class is **not** reused for file content because it pins a fixed salt
  *and* a fixed GCM nonce (catastrophic for multi-file reuse).
- **Envelope key management**: documents are encrypted with data-encryption keys
  protected by a master key-encryption key (KEK) that is unlocked at server
  startup. The KEK source (key file / passphrase / env / external KMS) is a
  decision captured in `design.md` and surfaced to the operator.
- **Mixed-state coexistence**: encrypted and plaintext files may live side by
  side; reads auto-detect via the header so the feature can be switched on
  without a flag day.
- **Migration tooling** (admin-triggered, resumable, progress-reporting):
  - forward migration unencrypted → encrypted,
  - reverse migration / decrypt-all (rollback and "turn the feature off"),
  - key rotation by re-wrapping data keys (no bulk re-encryption).
- **Keystore provisioning + export via the client**: a sysAdmin can **download** the
  keystore (at activation/rotation and on demand) to create the required off-server
  backup, and **upload** it to unlock a migrated/rebuilt server missing its key — without
  filesystem/shell access (TLS-secured, sysAdminRole-only, upload validated + no silent
  overwrite).
- **Operational safeguards**: fail-closed when the key is missing/invalid,
  mandatory key-backup/escrow guidance, and audit logging of bulk
  encrypt/decrypt/rotate operations.
- **Configuration**: a server setting enables/disables the feature and selects
  the key source; defaults chosen to avoid breaking existing installs.
- **Admin surfaces**: all operations (enable/disable, key-backup confirmation, keystore
  upload, forward/reverse migration, key rotation, status) are triggered from the client
  admin console **and** exposed over the REST API (v8) so server-only/Docker
  installations can be administered headless; both surfaces call the same server
  operations.

Scope is defined by storage path, not content type: **all** documents under
`archivefiles/` and `addressfiles/` are covered regardless of kind — including
dictation/audio and saved e-mails, which are ordinary documents on those paths (no
separate dictation or e-mail store exists). Out of scope (documented as residual risk in `design.md`): the Lucene
`searchindex/` (extracted plaintext text), the MySQL database contents, the template
folders (`emailtemplates/`/`mastertemplates/`/`letterheads/`), the transient
`faxqueue/`, and transport encryption (already handled by TLS).

## Impact

- Affected specs: `document-encryption` (new).
- Affected code:
  - `j-lawyer-server-common`: new `com.jdimension.jlawyer.security.*` file-crypto
    utility + key provider; `ServerFileUtils` (`readFile`/`writeFile`/`createFile`,
    `ServerFileUtils.java:814,850,857`) — encryption must be scoped to the four
    document folders (`archivefiles/`, `archivefiles-preview/`, `addressfiles/`,
    `addressfiles-preview/`), **not** applied to all `ServerFileUtils` callers.
  - `j-lawyer-server-ejb`: `ArchiveFileService` document I/O
    (`addDocumentImpl`, `setDocumentContent`, `getDocumentContentImpl`,
    `getDocumentContentBucket`, preview read/write), `AddressDocumentService` document
    I/O (`addDocument`, `setDocumentContent`, `getDocumentContent`,
    `addDocumentFromTemplate`), and `PreviewGenerator` (shared by both).
  - Admin/config surface for enablement, key source, and the migration/rotation
    operations (EJB service + likely a client admin panel).
  - `j-lawyer-backupmgr`: backups will contain ciphertext; restore requires the
    key — key backup/restore semantics must be documented.
- Affected operators: key management is now a hard operational dependency —
  **losing the key means losing all documents.** Migration of large existing
  data directories is a one-time, IO/CPU-intensive batch job.
