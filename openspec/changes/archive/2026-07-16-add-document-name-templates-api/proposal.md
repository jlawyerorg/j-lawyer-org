# Change: Document name templates and server-side document naming

## Why
The desktop client's "bulk save to case" dialog (`BulkSaveDialog`) derives each document's
file name from a **document name template** (Benennungsschema): a pattern whose placeholders
(file number, date, party fields, form fields, …) are resolved against the case, then sanitized,
with the source file's extension preserved. This logic lives entirely in the EJB layer
(`ArchiveFileServiceLocal.getNewDocumentName`, `ServerTemplatesUtil`) and the template list is
not exposed over REST at all.

The web client needs the same naming behaviour for its "In Akte speichern" dialog. Reproducing
placeholder resolution in the browser is infeasible (it would require exposing parties, form
fields, lawyer/assistant, and party types to the web), so naming must run server-side behind a
single request per document, mirroring `BulkSaveEntry.setNameTemplate`.

## What Changes
- Add `GET /v7/configuration/document-name-templates` returning all configured document name
  templates as `{ id, displayName, defaultTemplate }`, with the default template flagged.
- Add `POST /v7/cases/{id}/document-name` accepting `{ fileName, date, template }` and returning
  `{ name }`: the placeholder-resolved, sanitized document file name with the original extension
  preserved. When `template` is empty the default template is used; when no template exists the
  sanitized original name is returned. The case is ACL-gated via `getArchiveFile(id)`.
- Expose `getDocumentNameTemplates()` on the `SystemManagementLocal` interface (already
  implemented for the remote interface) so the configuration endpoint can call it locally.

## Impact
- Affected specs: document-naming (new capability)
- Affected code:
  - `j-lawyer-server-ejb/.../services/SystemManagementLocal.java` (1 method signature)
  - `j-lawyer-io/.../rest/v7/ConfigurationEndpointV7.java` (+ `ConfigurationEndpointLocalV7.java`)
  - `j-lawyer-io/.../rest/v7/CasesEndpointV7.java` (+ `CasesEndpointLocalV7.java`)
  - new POJOs: `RestfulDocumentNameTemplateV7`, `RestfulDocumentNameRequestV7`, `RestfulDocumentNameV7`
  - Web: `akten/cases.service.ts`, `communication/email-bulk-save.component.ts`
- Backward compatibility: purely additive; no existing endpoint or EJB signature changes.
