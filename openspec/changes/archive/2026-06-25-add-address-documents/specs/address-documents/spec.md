## ADDED Requirements

### Requirement: Address Document Entity

The system SHALL persist documents that belong directly to an address (contact) using a
dedicated entity `AddressDocumentsBean` backed by the `address_documents` table. Each
address document SHALL reference exactly one address and SHALL store metadata (id, name,
size, dictate sign, creation date, change date) in the database and its binary content on
the configured file storage. Address documents SHALL NOT support folders, tags,
versioning, locking, favorite marking, highlights, external-id lookup, or document-type
classification.

#### Scenario: Document is linked to an address
- **WHEN** a document is added to an address
- **THEN** the persisted `AddressDocumentsBean` references that address
- **AND** the document appears in the address's document list and in no case's list

#### Scenario: No folder or tag attributes
- **WHEN** an address document is created
- **THEN** the system stores it as a flat entry under the address with no folder
- **AND** the system provides no operation to assign folders or tags to it

### Requirement: Add Address Document

The system SHALL allow adding a new document to an address by supplying the address id, a
file name, and the binary content. The system SHALL store the content under a per-address
storage location and SHALL reject adding a document whose name already exists (non-deleted)
for that address.

#### Scenario: Add a document
- **WHEN** a caller adds a document with a unique name to an existing address
- **THEN** the content is written to the address's storage location
- **AND** a metadata record is created and returned with a generated id

#### Scenario: Duplicate name rejected
- **WHEN** a caller adds a document whose name matches an existing non-deleted document on the same address
- **THEN** the system rejects the operation and reports a name conflict

### Requirement: List and Read Address Documents

The system SHALL allow listing all non-deleted documents of an address, retrieving a single
document's metadata by id, and retrieving a document's binary content by id.

#### Scenario: List documents
- **WHEN** a caller requests the documents of an address
- **THEN** the system returns all non-deleted documents for that address

#### Scenario: Read content
- **WHEN** a caller requests the content of an existing address document
- **THEN** the system returns the stored binary content

### Requirement: Update Address Document Content and Metadata

The system SHALL allow updating an address document's binary content, renaming it, and
setting its document date. Renaming SHALL reject a name that collides with another
non-deleted document on the same address.

#### Scenario: Update content
- **WHEN** a caller updates the content of an existing address document
- **THEN** the stored content is replaced and the change date is updated

#### Scenario: Rename document
- **WHEN** a caller renames a document to a name not used by another non-deleted document on the address
- **THEN** the document's name is updated

#### Scenario: Set document date
- **WHEN** a caller sets the date of an address document
- **THEN** the document's date is updated

### Requirement: Recycle Bin for Address Documents

The system SHALL soft-delete address documents into a per-address recycle bin rather than
removing them immediately. The system SHALL allow listing recycle-bin documents, restoring
a document from the recycle bin, and permanently deleting a document from the recycle bin.
A restored document SHALL reappear in the address's active document list.

#### Scenario: Soft-delete to recycle bin
- **WHEN** a caller deletes an address document
- **THEN** the document is marked deleted with deletion date and deleting user
- **AND** it no longer appears in the active document list but appears in the recycle bin

#### Scenario: Restore from recycle bin
- **WHEN** a caller restores a deleted address document
- **THEN** the document reappears in the active document list and is removed from the recycle bin

#### Scenario: Permanent deletion
- **WHEN** a caller permanently deletes a document from the recycle bin
- **THEN** the metadata record and its stored content are removed and cannot be restored

### Requirement: OCR for Address Documents

The system SHALL allow running optical character recognition on an address document to
extract its text.

#### Scenario: Run OCR
- **WHEN** a caller requests OCR for an address document of a supported type
- **THEN** the system performs text recognition and returns or stores the recognized text

### Requirement: Preview and PDF Conversion for Address Documents

The system SHALL provide a text or PDF preview for an address document and SHALL allow
converting a supported office-format document to PDF.

#### Scenario: Get preview
- **WHEN** a caller requests a preview of an address document of a supported format
- **THEN** the system returns a text or PDF preview

#### Scenario: Convert to PDF
- **WHEN** a caller requests PDF conversion of a supported office-format address document
- **THEN** the system returns or stores a PDF rendering of the document

### Requirement: Create Address Document from Template

The system SHALL allow creating a new address document from a template by supplying the
address id, a target file name, the template, and placeholder values to be substituted.

#### Scenario: Create from template
- **WHEN** a caller creates an address document from a template with placeholder values
- **THEN** the system generates the document with placeholders substituted and stores it on the address

### Requirement: REST API for Address Documents

The system SHALL expose REST v7 endpoints under `/v7/contacts` providing full CRUD over
address documents: list an address's documents, get a document's metadata, get and set its
content, create a document, rename it, delete it, list the recycle bin, restore, permanently
delete, convert to PDF, perform OCR, and create from template. All endpoints SHALL require
authentication consistent with the existing REST API.

#### Scenario: Create a document via REST
- **WHEN** an authenticated client POSTs a document with content to an address's documents endpoint
- **THEN** the system stores the document and returns its metadata

#### Scenario: Retrieve documents via REST
- **WHEN** an authenticated client requests an address's documents
- **THEN** the system returns the list of non-deleted documents for that address

#### Scenario: Unauthenticated request rejected
- **WHEN** an unauthenticated client calls an address documents endpoint
- **THEN** the system rejects the request

### Requirement: Desktop Client Address Documents View

The desktop client SHALL provide a documents area within the address editor that lists an
address's documents and allows adding, opening, renaming, and deleting documents as well as
restoring documents from the recycle bin, reusing the existing document viewer
infrastructure.

#### Scenario: Manage documents in the address editor
- **WHEN** a user opens an address in the client and selects the documents area
- **THEN** the user sees the address's documents and can add, open, rename, and delete them

#### Scenario: Restore in the client
- **WHEN** a user opens the recycle bin in the address documents area and restores a document
- **THEN** the document reappears in the address's active document list
