## Context

Case documents are implemented by `ArchiveFileDocumentsBean` (table `case_documents`) plus
a large set of methods on `ArchiveFileService`. Metadata lives in the database; the actual
byte content lives on disk under
`${jlawyer.server.basedirectory}/archivefiles/{caseId}/{fileName}` with previews under
`archivefiles-preview/{caseId}/`. The case-document model also carries folders
(`CaseFolder`), tags (`DocumentTagsBean`), versioning, locking, favorites, highlights and
an external id.

For address documents we want the same storage discipline (DB metadata + on-disk content,
soft-delete recycle bin, OCR, PDF preview, create-from-template) but a deliberately
**simpler model**: a flat list per address with no folders, tags, versioning, locking,
favorites, highlights, or external-id lookup.

## Goals / Non-Goals

- Goals
  - A self-contained `AddressDocumentService` that does not overload `ArchiveFileService`.
  - Reuse the existing content-storage, OCR, preview/PDF-conversion and template helpers
    rather than duplicating them.
  - REST v7 + client UI parity with the confirmed feature scope.
- Non-Goals
  - Folders, tags, versioning, locking, favorite, highlights, external id.
  - Migrating or moving documents between cases and addresses (no cross-linking).
  - Changing any existing case-document behavior.

## Decisions

- **Decision: New entity `AddressDocumentsBean` (table `address_documents`).**
  Fields kept from `ArchiveFileDocumentsBean`: `id` (String PK), `name`, `size`,
  `dictateSign`, `creationDate`, `changeDate`, and the soft-delete trio
  `deleted` / `deletionDate` / `deletedBy`. The case link `archiveFileKey` is replaced by
  `addressKey` (ManyToOne → `AddressBean`, column `contact_id`). Fields dropped: `folder`,
  `favorite`, `version`, `highlight1`, `highlight2`, `externalId`, `lockedBy`,
  `lockedDate`, `documentType`. Content is **not** stored in the entity; it lives on disk (consistent with
  how case content is read/written through the VFS, not the BLOB column).
  - Alternative considered: reuse `ArchiveFileDocumentsBean` with a nullable address FK.
    Rejected — it would entangle the simpler address model with folders/tags/versioning and
    complicate every existing case query.

- **Decision: New EJB `AddressDocumentService` (`@Stateless`), Remote + Local.**
  Methods mirror the confirmed subset of `ArchiveFileService` document methods, renamed
  around addresses. A new `AddressDocumentsBeanFacade extends AbstractFacade<...>` provides
  persistence, following `ArchiveFileDocumentsBeanFacade`.
  - Alternative considered: add the methods to the existing `AddressService`. Rejected to
    keep `AddressService` focused on contact data and to allow `@RolesAllowed` scoping of
    document operations independently.

- **Decision: On-disk layout `addressfiles/{addressId}/` and
  `addressfiles-preview/{addressId}/`** under `jlawyer.server.basedirectory`, derived the
  same way as the case path logic in `ArchiveFileService`. Factor the path-building and
  content read/write into a shared helper so case and address code do not diverge.

- **Decision: REST under `/v7/contacts`** (new `ContactsEndpointV7`), exposing the document
  sub-resources analogous to the case document endpoints (list, get metadata, get/set
  content, create, rename, delete, trash list, restore, permanent delete, to-pdf, perform
  OCR, create-from-template). Reuse `RestfulDocumentV1` / `RestfulDocumentContentV1` shapes
  where possible; add address-specific request POJOs only where the case POJO carries a
  `caseId` that must become a `contactId`.

- **Decision: Client UI** adds a documents panel/tab to `EditAddressPanel`, reusing
  `DocumentViewerFactory` and the document import/preview infrastructure used by
  `ArchiveFilePanel`. The `.form` file is updated alongside the `.java` so the NetBeans GUI
  builder stays authoritative.

## Risks / Trade-offs

- **Code duplication vs. coupling.** Mirroring case-document logic risks divergence over
  time → mitigate by extracting shared storage/OCR/preview/template helpers instead of
  copy-paste.
- **Schema migration.** New table only (no change to existing tables) → low risk; ship a
  forward-only SQL script consistent with the project's `validate` schema strategy.
- **Permissions.** Address documents must honor the same authentication/authorization model
  as other address operations → guard service methods with `@RolesAllowed` consistent with
  `AddressService`.

## Migration Plan

1. Ship `address_documents` table creation script (forward-only).
2. Deploy entity + facade + service + REST + client together (single EAR/client release).
3. No data backfill required; feature is additive. Rollback = drop the new table and remove
   the new endpoints/UI; existing case documents are untouched.

## Open Questions

- None outstanding. (Document-type classification and template re-generation references are
  explicitly out of scope: address documents have no `documentType` field and do not retain
  a link to an originating template.)
