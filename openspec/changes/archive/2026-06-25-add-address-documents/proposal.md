# Change: Store documents directly on an address (contact)

## Why

Documents can currently only be stored on a case (`ArchiveFileBean`). Many documents
belong to a contact rather than a single case — e.g. a client's ID copy, a power of
attorney, insurance policies, or correspondence that is not (yet) tied to a matter.
Today users have to create a placeholder case or keep such files outside the system.
Addresses (`AddressBean`, table `contacts`) should be able to hold documents directly.

## What Changes

- Introduce a new entity `AddressDocumentsBean` (table `address_documents`) modeled after
  `ArchiveFileDocumentsBean`, but **without folders and without tags**.
- Add a new EJB service `AddressDocumentService` (Remote + Local) providing the document
  operations on addresses. Physical content is stored on disk under a per-address
  directory, mirroring the case-document storage mechanism.
- Supported capabilities (confirmed scope):
  - **Core CRUD**: add document, list documents, get metadata, get/set content,
    rename, set document date, delete.
  - **Recycle bin / soft-delete**: deleted documents go to a per-address trash; restore,
    permanently delete, and list trashed documents.
  - **OCR**: run text recognition on an address document.
  - **PDF preview / conversion**: text/PDF preview and convert-to-PDF for office formats.
  - **Create from template**: generate a document from a template with placeholder values.
- Add a **REST v7** full CRUD endpoint set for address documents under `/v7/contacts`,
  mirroring the case-document sub-resources.
- Add a **Swing client** documents area/tab in the address editor (`EditAddressPanel`)
  with add / open / rename / delete and recycle-bin actions, reusing the existing
  document viewer infrastructure.

### Out of scope (explicitly NOT included)

- **Folders** (no `CaseFolder` analogue) — documents are a flat list per address.
- **Tags** (no `DocumentTagsBean` analogue).
- **Versioning / history**, **document locking**, **favorite flag**, **external-id**
  lookup, **highlights**, and **document-type classification** — none of these are carried
  over to address documents.

## Impact

- Affected specs: `address-documents` (new capability).
- Affected code:
  - `j-lawyer-server-entities`: new `AddressDocumentsBean` + DB migration script.
  - `j-lawyer-server/j-lawyer-server-ejb`: new `AddressDocumentService` impl +
    `AddressDocumentsBeanFacade` + `AddressDocumentsBeanFacadeLocal`.
  - `j-lawyer-server-api`: new `AddressDocumentServiceRemote` interface.
  - `j-lawyer-server/j-lawyer-io`: new REST endpoints under `/v7/contacts` + payload POJOs.
  - `j-lawyer-client`: `JLawyerServiceLocator` lookup + documents panel in the address
    editor (`.java` + `.form`).
- Storage: new on-disk directories
  `${jlawyer.server.basedirectory}/addressfiles/{addressId}/` (content) and
  `${jlawyer.server.basedirectory}/addressfiles-preview/{addressId}/` (previews).
