# Tasks

## Server
- [x] Add `getDocumentNameTemplates()` to `SystemManagementLocal`
- [x] Add 3 v7 POJOs: `RestfulDocumentNameTemplateV7`, `RestfulDocumentNameRequestV7`, `RestfulDocumentNameV7`
- [x] `GET /v7/configuration/document-name-templates` in `ConfigurationEndpointV7` (+ Local)
- [x] `POST /v7/cases/{id}/document-name` in `CasesEndpointV7` (+ Local), ACL via `getArchiveFile(id)`
- [x] Build EAR, deploy to WildFly
- [x] Verify `GET` returns templates incl. default flag
- [x] Verify `POST` resolves placeholders, uses default when template empty, 404 for unknown case

## Web
- [x] `CasesService`: `documentNameTemplates()`, `computeDocumentName(caseId, fileName, date, template)`
- [x] Consume in the bulk-save dialog (name template selector + "auf alle anwenden")

## Docs
- [x] OpenSpec delta for the new `document-naming` capability
