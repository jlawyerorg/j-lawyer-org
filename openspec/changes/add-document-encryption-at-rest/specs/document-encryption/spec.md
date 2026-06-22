## ADDED Requirements

### Requirement: Transparent Encryption of Document Content at Rest

When encryption at rest is enabled, the system SHALL encrypt all byte content
written to the `archivefiles/` folder (document originals and versions) before it is
persisted to the storage backend, and SHALL decrypt it transparently on read, so that
EJB clients, REST API consumers (v1–v7), search indexing, and preview generation
observe the same plaintext content as before.

#### Scenario: New document is stored encrypted
- **WHEN** encryption is enabled and a document is added or its content is updated
- **THEN** the bytes persisted under `archivefiles/<archiveFileId>/<documentId>` are
  ciphertext and cannot be interpreted without the firm's key
- **AND** a subsequent authenticated read returns the original plaintext bytes

#### Scenario: Reads are transparent to callers
- **WHEN** an authenticated caller retrieves document content via EJB or REST
- **THEN** the returned bytes are identical to the bytes originally stored
- **AND** no caller code outside the storage layer needs to change

#### Scenario: Chunked DataBucket transfer serves decrypted content
- **WHEN** an encrypted document is retrieved via the chunked `DataBucket` transfer path
- **THEN** the server decrypts the document into the local bucket file before chunking,
  without loading the entire document into memory at once
- **AND** the client receives the same plaintext chunks it would have received before
  encryption was enabled
- **AND** the temporary decrypted bucket file is removed after the transfer completes

### Requirement: Transparent Encryption of Document Previews

When encryption at rest is enabled, the system SHALL encrypt the extracted-text
(`<documentId>.text`) and rendered-PDF (`<documentId>.pdf`) preview files written to
the `archivefiles-preview/` folder, and SHALL decrypt them transparently wherever
previews are consumed.

#### Scenario: Preview generation writes ciphertext
- **WHEN** a text or PDF preview is generated for a document
- **THEN** the preview file persisted under `archivefiles-preview/` is ciphertext
- **AND** displaying the preview to an authenticated user yields the original content

### Requirement: Authenticated Per-File Encryption

The system SHALL encrypt each file with AES-256 in an authenticated mode (GCM) using a
cryptographically random initialization vector generated independently for every file
and every rewrite. The system SHALL NOT reuse a fixed nonce/IV across files, and SHALL
NOT reuse the legacy `Crypto` fixed-salt/fixed-nonce primitive for file content. On
read, the system SHALL verify the authentication tag and SHALL refuse to return content
that fails integrity verification.

#### Scenario: Each file uses a unique IV
- **WHEN** two documents (or two versions of one document) are encrypted
- **THEN** each is encrypted with its own distinct random IV

#### Scenario: Tampered ciphertext is rejected
- **WHEN** an encrypted file's bytes have been modified on disk
- **THEN** the read fails with an integrity error and no plaintext is returned

### Requirement: Self-Describing Versioned File Format

Encrypted files SHALL begin with a self-describing header containing a magic marker, a
format version, an algorithm identifier, the wrapped data key, and the IV, such that the
system can (a) detect whether any given file is encrypted or plaintext and (b) evolve
the format in future versions without ambiguity.

#### Scenario: Encrypted file is detected by header
- **WHEN** the system opens a file that carries the encryption magic marker
- **THEN** it decrypts the file using the parameters declared in the header

#### Scenario: Format version is recorded
- **WHEN** an encrypted file is written
- **THEN** its header records the format version and algorithm used to produce it

#### Scenario: Crypto material is self-contained in the file
- **WHEN** an encrypted file is written
- **THEN** the wrapped data key, IV, algorithm, and key version are stored in the file
  header itself, so the file can be decrypted with only the master key and without any
  database record
- **AND** preview files under `archivefiles-preview/` (which have no database row) are
  decryptable from their header alone

### Requirement: Logical Size Reported Independently of Ciphertext

The system SHALL continue to report the logical plaintext size of a document to callers,
not the larger on-disk size that results from the encryption header and authentication
tag.

#### Scenario: Plaintext size is preserved
- **WHEN** a client queries the size of an encrypted document
- **THEN** the size returned equals the original plaintext byte length
- **AND** not the larger size of the encrypted file on disk

### Requirement: Envelope Key Management with a Master Key

The system SHALL protect each file's data-encryption key by wrapping it with a
firm-wide master key (key-encryption key) that is unlocked at server startup from a
configured key source. The master key SHALL be derived/stored using a random per-install
salt (never a hard-coded salt). The plaintext master key SHALL only exist in server
memory while the server is running.

#### Scenario: Master key unlocked at startup
- **WHEN** the server starts with encryption enabled and a valid key source
- **THEN** the master key is unlocked and document encryption/decryption is available

#### Scenario: Data keys are wrapped, not stored in clear
- **WHEN** a file is encrypted
- **THEN** its data-encryption key is stored only in wrapped (encrypted) form

### Requirement: Mixed-State Coexistence

The system SHALL support a data directory that contains both encrypted and plaintext
document/preview files simultaneously, choosing decrypt-or-passthrough per file based on
the file header, so that enabling encryption does not require a synchronized flag-day
conversion of all existing files.

#### Scenario: Plaintext legacy file read after enabling encryption
- **WHEN** encryption is enabled but a given legacy file has not yet been migrated
- **THEN** the system detects it is plaintext and returns it unchanged

#### Scenario: Newly written file is encrypted while legacy files remain plaintext
- **WHEN** encryption is enabled and an existing case gets a new document
- **THEN** the new file is encrypted while untouched legacy files remain readable

### Requirement: Migration from Unencrypted to Encrypted

The system SHALL provide an administrator-triggered migration that encrypts existing
plaintext files under `archivefiles/` and `archivefiles-preview/` in place. The
migration SHALL be idempotent (skipping already-encrypted files via header detection),
resumable after interruption, performable while the system is online, and SHALL report
progress and completion.

#### Scenario: Forward migration encrypts legacy files
- **WHEN** an administrator starts the forward migration
- **THEN** every plaintext document and preview file becomes encrypted
- **AND** already-encrypted files are skipped

#### Scenario: Migration resumes after interruption
- **WHEN** the migration is interrupted and restarted
- **THEN** it continues without re-processing already-encrypted files and without
  corrupting partially processed files

### Requirement: Reverse Migration and Key Rotation

The system SHALL provide an administrator-triggered reverse migration that decrypts all
files back to plaintext (to disable the feature or recover), and SHALL provide key
rotation that re-wraps existing data keys under a new master key without re-encrypting
file content.

#### Scenario: Reverse migration decrypts all files
- **WHEN** an administrator runs the reverse migration with the correct key
- **THEN** all encrypted files are rewritten as plaintext and the feature can be disabled

#### Scenario: Key rotation re-wraps without bulk re-encryption
- **WHEN** an administrator rotates the master key
- **THEN** each file's wrapped data key is re-wrapped under the new master key
- **AND** the file content ciphertext is not rewritten

### Requirement: Scoped Fail-Closed on Missing or Invalid Key

The system SHALL fail closed when encryption is enabled but the master key is
unavailable or invalid: it SHALL refuse document read and write operations with a clear
error rather than serving ciphertext as if it were plaintext or overwriting encrypted
files with unencrypted data. The failure SHALL be **scoped to the document subsystem** —
the rest of the application (calendar, contacts, case metadata, invoicing, etc.) SHALL
continue to operate. The system SHALL NOT fall back to writing plaintext or returning
raw ciphertext under any circumstances.

At startup, when encryption is enabled but no valid master key is available, the system
SHALL surface a loud error/health warning and keep the document functions disabled
(failing closed) rather than starting as if document storage were healthy.

#### Scenario: Server with wrong key does not corrupt data
- **WHEN** encryption is enabled but the configured key is wrong or missing
- **THEN** document read and write operations fail with an explicit error
- **AND** no encrypted file is overwritten or served as raw ciphertext

#### Scenario: Rest of the application stays available
- **WHEN** the master key is unavailable at runtime
- **THEN** document operations fail closed
- **AND** non-document functions (calendar, contacts, case metadata, invoicing) remain
  usable

#### Scenario: Startup with enabled encryption but no valid key
- **WHEN** the server starts with encryption enabled but cannot load a valid master key
- **THEN** it raises a visible error/health warning and leaves document functions
  disabled
- **AND** it does not serve or write document content as plaintext

### Requirement: Key Backup Safeguard at Activation

The system SHALL require a confirmed key backup before the first moment any document
becomes dependent on the master key — that is, before encrypted writes are activated
**or** before a forward migration starts, whichever occurs first — and SHALL document
that loss of the key makes all encrypted documents permanently unrecoverable. The
confirmation SHALL NOT be a simple acknowledgement: the administrator SHALL be required
to enter a key fingerprint (or recovery code) displayed from the actual key material, so
that the confirmation proves possession of the backed-up key rather than a reflexive
click.

#### Scenario: Activation blocked until key backup is proven
- **WHEN** an administrator attempts to enable encrypted writes or start a forward
  migration and the key backup has not yet been confirmed
- **THEN** the operation does not proceed and the operator is warned about irreversible
  data loss on key loss

#### Scenario: Confirmation requires the key fingerprint, not just a checkbox
- **WHEN** the administrator confirms the key backup
- **THEN** the confirmation is accepted only if the entered key fingerprint (or recovery
  code) matches the value derived from the current master key
- **AND** an incorrect or empty value is rejected and activation remains blocked

#### Scenario: Gate triggers on encrypted writes even without migration
- **WHEN** an administrator enables encryption so that new documents are written
  encrypted, without ever starting the forward migration
- **THEN** the key-backup confirmation is still required before the first encrypted write

### Requirement: Keystore Provisioning via the Client

The system SHALL allow an administrator to provide the master key to a server that is
missing it by uploading the previously backed-up keystore through the client, so that a
migrated or rebuilt server can be unlocked without filesystem or shell access. On
receipt, the server SHALL write the keystore to the configured key location with
restrictive permissions and SHALL unlock the document subsystem. This is a transport
mechanism for the existing key only; it does not replace the key-backup obligation and
introduces no alternative key-derivation scheme.

The upload SHALL be restricted to the `sysAdminRole`, SHALL be audited, and SHALL be
transmitted only over a transport-encrypted (TLS) channel; the key bytes SHALL NOT be
logged. Before accepting the key, the server SHALL validate that it matches the existing
encrypted data (for example via a key fingerprint or a test unwrap) and SHALL reject a
non-matching key. The server SHALL NOT silently overwrite an existing key; replacing a
present key SHALL require an explicit replace action.

#### Scenario: Locked server is unlocked by uploading the keystore
- **WHEN** a server has encryption enabled but no master key, and an administrator
  uploads the correct backed-up keystore through the client over a TLS-secured channel
- **THEN** the server stores the keystore at the configured location with restrictive
  permissions
- **AND** validates that it matches the encrypted data
- **AND** unlocks the document subsystem so documents become accessible again

#### Scenario: Non-matching or unauthorized upload is rejected
- **WHEN** the uploaded keystore does not match the encrypted data, or the caller does
  not hold `sysAdminRole`, or the channel is not transport-encrypted
- **THEN** the server rejects the upload and the document subsystem remains locked

#### Scenario: Existing key is not silently overwritten
- **WHEN** an administrator uploads a keystore while a key already exists at the
  configured location
- **THEN** the server does not overwrite it unless an explicit replace action is taken

### Requirement: Keystore Export (Download) via the Client

The system SHALL allow a `sysAdminRole` to download the current keystore through the
client (and REST), so the administrator can create the off-server backup that the
key-backup safeguard requires. The download SHALL be offered at the moments new key
material is created — initial key initialization/activation and key rotation — and SHALL
also be available on demand thereafter. The export SHALL be restricted to the
`sysAdminRole`, SHALL be audited, SHALL be transmitted only over a transport-encrypted
(TLS/HTTPS) channel, and the key bytes SHALL NOT be logged.

Because key rotation re-wraps data keys under the new master key, after a rotation the
previous key backup no longer suffices; the system SHALL make the new keystore available
for download and SHALL require its backup to be confirmed (via the key-backup safeguard)
as part of completing rotation.

#### Scenario: Download the keystore at activation to back it up
- **WHEN** a `sysAdminRole` initializes/activates encryption
- **THEN** the freshly generated keystore can be downloaded through the client over a
  TLS-secured channel
- **AND** the downloaded keystore is the backup whose possession is later proven at the
  key-backup gate

#### Scenario: Download the new keystore after rotation
- **WHEN** a `sysAdminRole` rotates the master key
- **THEN** the new keystore can be downloaded
- **AND** rotation requires the new key's backup to be confirmed, since the previous key
  backup can no longer decrypt the re-wrapped data

#### Scenario: Export is denied without privilege or transport security
- **WHEN** a keystore download is attempted by a caller without `sysAdminRole`, or over a
  non-TLS channel
- **THEN** the request is rejected and no key material is returned

### Requirement: Configurable Enablement and Key Source

The system SHALL expose configuration to enable or disable encryption at rest and to
select the master-key source. Encryption SHALL be **disabled by default** on both new
installations and upgrades, so that the system stores and reads documents as plaintext
until an administrator explicitly opts in.

#### Scenario: Feature disabled by default on upgrade
- **WHEN** the new version is deployed to an existing installation without configuration
- **THEN** documents continue to be stored and read as plaintext until encryption is
  explicitly enabled

#### Scenario: Feature disabled by default on new install
- **WHEN** a fresh installation is set up without configuring encryption
- **THEN** documents are stored as plaintext until an administrator enables encryption

#### Scenario: Administrator enables encryption
- **WHEN** an administrator enables encryption and configures a valid key source
- **THEN** new writes are encrypted and the migration tooling becomes available

### Requirement: Encryption Administration Restricted to System Administrators

The system SHALL authorize encryption administration operations only for the
`sysAdminRole` — this covers enable/disable, key initialization, the key-backup
confirmation, keystore upload, forward migration, reverse migration, and key rotation.
The ordinary `adminRole` SHALL NOT be permitted to perform these operations. This applies
identically to the client and REST surfaces.

#### Scenario: System administrator may operate encryption
- **WHEN** a user holding `sysAdminRole` invokes an encryption administration operation
- **THEN** the operation is authorized

#### Scenario: Ordinary administrator is denied
- **WHEN** a user holding only `adminRole` (not `sysAdminRole`) invokes any encryption
  administration operation, via the client or REST
- **THEN** the operation is refused as unauthorized

### Requirement: Explicit Key Initialization

The master key SHALL be created only by an explicit, audited administrator action, and
SHALL NOT be generated automatically by database schema migration or by server startup.
Key creation SHALL generate a cryptographically random master key, store it (with its
metadata) outside the data directory, and SHALL refuse to overwrite an existing key.

#### Scenario: Schema migration does not create keys
- **WHEN** the new version is deployed and database migrations run
- **THEN** only bookkeeping schema changes are applied
- **AND** no master key is generated and encryption remains disabled until an
  administrator initializes it

#### Scenario: Existing key is not overwritten
- **WHEN** an administrator triggers key initialization while a key already exists
- **THEN** the operation refuses and the existing key is preserved

### Requirement: Bounded Performance Overhead

Encryption and decryption SHALL add only marginal overhead to per-document operations
for typical document sizes, and per-key derivation (PBKDF2/KDF) SHALL be performed once
at startup rather than per file, so that the master key is not re-derived on each
read/write.

#### Scenario: Key derivation runs once
- **WHEN** the server processes many document reads and writes after startup
- **THEN** the key-derivation function is not re-invoked per operation

#### Scenario: Throughput remains acceptable for typical documents
- **WHEN** authenticated users read and write documents of typical size
- **THEN** the added latency from encryption is marginal relative to existing I/O

### Requirement: REST Administration of Encryption

The system SHALL expose the encryption administration operations — enable/disable,
key-backup confirmation, keystore upload, forward migration, reverse migration, and key
rotation, plus status/progress retrieval — through the REST API so that a server-only or
headless installation can be administered without the desktop client. These endpoints
SHALL be backed by the same server operations as the client and SHALL be added in the
latest API version without changing existing versions.

The REST endpoints SHALL require the `sysAdminRole`, SHALL be served only over a
transport-encrypted (HTTPS) channel for any operation that carries key material
(notably keystore upload), SHALL NOT log key material, and long-running operations SHALL
start asynchronously and report progress via a status endpoint (consistent with the
existing administration job-status pattern).

#### Scenario: Headless server is administered via REST
- **WHEN** an administrator calls the REST encryption endpoints on a server with no
  desktop client
- **THEN** the administrator can enable encryption, upload the keystore, and start
  migration or rotation
- **AND** can poll operation status until completion

#### Scenario: REST keystore upload requires HTTPS and admin role
- **WHEN** a keystore upload is attempted over a non-HTTPS channel or without
  `sysAdminRole`
- **THEN** the request is rejected and no key material is accepted

### Requirement: Audit Logging of Bulk Cryptographic Operations

The system SHALL record an audit entry for the start and completion of forward
migration, reverse migration, and key rotation, including who initiated the operation
and its outcome.

#### Scenario: Migration is auditable
- **WHEN** an administrator runs a migration or key rotation
- **THEN** an audit record captures the initiator, operation type, and result
