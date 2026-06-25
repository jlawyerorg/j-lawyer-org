## 0. Decisions
- [x] 0.1 Master-key source: key file on server, auto-unlock at startup (C1)
- [x] 0.2 Single firm-wide key domain + per-file envelope keys (C3)
- [x] 0.3 Scope: only the document folders archivefiles/ + archivefiles-preview/ + addressfiles/ + addressfiles-preview/; Lucene index plaintext = accepted residual risk (C2)
- [x] 0.4 Migration: admin-triggered resumable online batch + reverse + rotation (C4)
- [x] 0.5a DataBucket path supported via server-side streaming decrypt to temp (C5/Q8)
- [x] 0.5b Data directory 20–150 GB → I/O-bound, online/resumable/throttled migration (Q9)
- [x] 0.5c ~5% steady-state overhead acceptable; AES-NI may be assumed (Q9, Q10)
- [x] 0.6a Default OFF on new installs and upgrades (Q11); compliance: DSGVO + BRAO/StGB (Q13)
- [x] 0.6b Scope is by storage path: dictation/audio + saved e-mails are documents under archivefiles/, auto-covered; contact documents live under addressfiles/ and share the same storage/preview code, auto-covered; no separate store (Q5)
- [x] 0.6c Key-backup gate at activation; Variant B (typed fingerprint/recovery code) (Q3)
- [x] 0.6 Option A scoped fail-closed + startup health check on invalid key (Q12)
- [ ] 0.6 Confirm default-on-vs-off, key-backup UX, compliance drivers (Q3, Q11, Q13)

## 1. Crypto primitive (j-lawyer-server-common)
- [ ] 1.1 Add `FileCrypto` AES-256-GCM utility: random per-file IV, 128-bit tag,
      encrypt/decrypt over byte[] (and streaming variant if 1.4 required)
- [ ] 1.2 Define self-describing file header (magic, version, algoId, wrapped DEK, IV)
- [ ] 1.3 Implement header detection (isEncrypted) for mixed-state reads
- [ ] 1.4 Streaming GCM decrypt/encrypt (required for the DataBucket path, C5)
- [ ] 1.5 Unit tests: round-trip, tamper detection, wrong key, header detection,
      plaintext passthrough

## 2. Key management
- [ ] 2.1 Implement master-key (KEK) provider: key file / PKCS12 keystore outside
      basedirectory, chmod 600; optional keystore password via system property/env var
- [ ] 2.2 Per-install random salt + key metadata stored alongside the key (NOT in DB);
      KDF run once at startup, key cached in memory
- [ ] 2.3 Envelope: generate/wrap/unwrap per-file data keys; crypto material in file header
- [ ] 2.4 Key rotation: re-wrap data keys under a new master key (bump key version)
- [ ] 2.5 Scoped fail-closed on missing/invalid key (document subsystem only; app keeps
      running) + startup health check that disables document functions and warns loudly
- [ ] 2.6 Explicit "Initialize encryption" action (`sysAdminRole`): SecureRandom KEK, write
      key + metadata, refuse overwrite, gate on backup confirmation (NOT a Flyway/startup step)
- [ ] 2.7 Tests for wrap/unwrap, rotation, fail-closed, init-refuses-overwrite
- [ ] 2.8 Document that key location is excluded from document backups

## 3. Document storage integration (j-lawyer-server-ejb)
- [ ] 3.0a Flyway SQL migration `V<x>__AddDocumentEncryptionColumns.sql`: add
      `encryption_state` + `key_version` to `case_documents` (schema only, inert when off)
- [ ] 3.0b Add the two bookkeeping fields to `ArchiveFileDocumentsBean` (hints, header is
      authoritative); keep `size` = logical plaintext length
- [ ] 3.0c Same bookkeeping columns/fields on `address_documents` / `AddressDocumentsBean`
      (Flyway + entity); header remains authoritative, `size` = logical plaintext length
- [ ] 3.1 Introduce a document-storage layer scoped to `archivefiles/`,
      `archivefiles-preview/`, `addressfiles/` and `addressfiles-preview/` (scope predicate
      keys on the storage-area path segment; do NOT encrypt all `ServerFileUtils` callers)
- [ ] 3.2 Wire encryption into `ArchiveFileService` write paths
      (`addDocumentImpl`, `setDocumentContent`)
- [ ] 3.3 Wire decryption into read paths (`getDocumentContentImpl`, unrestricted reads)
- [ ] 3.3a `getDocumentContentBucket`: replace `Files.copy(src→temp)` with streaming
      decrypt into the temp bucket file; ensure temp file cleanup (C5)
- [ ] 3.4 Wire `PreviewGenerator` text/PDF preview read+write (shared by case + address;
      covers `archivefiles-preview/` and `addressfiles-preview/`)
- [ ] 3.4b Wire `AddressDocumentService` write/read paths (`addDocument`,
      `setDocumentContent`, `getDocumentContent`, `addDocumentFromTemplate`), or confirm they
      are already covered because they go through the scoped storage layer from 3.1
- [ ] 3.5 Verify search indexing receives decrypted text; confirm whether the
      Lucene index is in/out of scope per 0.3

## 4. Configuration & admin surface
- [ ] 4.1 Server config in `server_settings` (via ServerSettingsKeys): enable flag
      (`jlawyer.server.encryption.enabled`, default off) + key-source/path selection
- [ ] 4.2 EJB operations: start/stop/status for forward migration, reverse migration,
      key rotation — `@RolesAllowed({"sysAdminRole"})` (NOT adminRole); run server-side as
      background jobs that survive client disconnect and report progress/status
- [ ] 4.3 Key-backup gate at ACTIVATION (before encrypted writes OR migration, whichever
      first); Variant B: require typed key fingerprint/recovery code matching the key
- [ ] 4.4 Client admin dialog under `AdminConsoleFrame` (+ `.form`) for enablement,
      migration, rollback, rotation, key-backup gate and keystore upload — following the
      `SearchIndexOptionsDialog` "reindex" pattern (trigger + status, work runs on server)
- [ ] 4.5 Audit logging for migration/rotation start+completion
- [ ] 4.6 Keystore upload: client upload dialog (+ `.form`) + `sysAdminRole` EJB op that
      writes the keystore to the configured path (chmod 600), validates key-matches-data before
      accepting, refuses silent overwrite, requires TLS channel, never logs key bytes,
      audited (C6)
- [ ] 4.6a Keystore download/export: client download (+ `.form`) + `sysAdminRole` EJB op;
      offered at activation and rotation and on demand; TLS, audited, never logs key bytes;
      rotation requires new-key backup confirmation before completing (C6)
- [ ] 4.7 Expose locked-state ("encryption enabled, key missing") to the client so the
      upload flow can be offered
- [ ] 4.8 REST API (v8): encryption admin endpoints (enable/disable, key-backup confirm,
      keystore upload + download, forward/reverse migration, rotation, status) backed by
      the same EJB ops; `sysAdminRole` only, HTTPS required for key material, async + status
      modelled on `AdministrationEndpointV7` (`/jobs/{id}/status`); key bytes never logged
- [ ] 4.9 Regenerate + re-baseline `swagger.json` (`mvn -Pswagger-regen`), keep
      `SwaggerEquivalenceTest` green

## 5. Migration tooling (sized for 20–150 GB, I/O-bound)
- [ ] 5.1 Idempotent, resumable forward migration over all four document folders
      (archivefiles/, archivefiles-preview/, addressfiles/, addressfiles-preview/) with progress + ETA
- [ ] 5.2 Reverse migration (decrypt-all) with progress
- [ ] 5.3 Online-safe behaviour (handle concurrent reads/writes during migration)
- [ ] 5.4 Crash-safe in-place rewrite: temp file in same dir → fsync → atomic rename
- [ ] 5.5 Throttling control to bound migration I/O (off-hours / rate limit)
- [ ] 5.6 Migration tests on a mixed-state directory (incl. interrupt/resume + crash mid-file)

## 6. Operational / docs
- [ ] 6.1 Document key backup/restore, server migration via client keystore upload (C6),
      and interaction with `j-lawyer-backupmgr`
- [ ] 6.1a Document per-OS keystore locations (Linux/Windows/macOS/Docker), perms/ACL, and
      Docker: dedicated volume/secret outside the (777) data volume
- [ ] 6.2 Document residual risks (Lucene index, DB, templates) per 0.3
- [ ] 6.3 Document CPU/AES-NI expectations and migration runtime guidance
- [ ] 6.4 Verify Nextcloud/cloud sync reads via `getDocumentContent` (decrypted), not
      raw disk; document that the shared copy is plaintext (Q14)

## 7. Validation
- [ ] 7.1 `openspec validate add-document-encryption-at-rest --strict`
- [ ] 7.2 End-to-end: enable → write → read → search → preview → migrate → rotate →
      reverse-migrate on a test data directory
