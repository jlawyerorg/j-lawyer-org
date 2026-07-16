# Tasks

## 1. Spec
- [x] 1.1 Delta spec for `email-rest-api` (ADDED: Email Case Suggestions)
- [x] 1.2 `openspec validate add-email-case-suggestions --strict` passes

## 2. Server
- [x] 2.1 Add `extractKeywordsFromText` and `getAllReferencedFileNumbers` to `ArchiveFileServiceLocal` (bean already implements both)
- [x] 2.2 New POJOs `RestfulCaseSuggestionRequestV7` (subject/body/from) and `RestfulCaseSuggestionsV7` (cases/contacts/phoneNumbers/sender)
- [x] 2.3 `POST /v7/email/case-suggestions` in `EmailEndpointV7` (+ `EmailEndpointLocalV7`): reproduce the heuristic (subject/body file numbers, foreign references, sender→contacts→cases, phone extraction), dedupe + ACL-filter
- [x] 2.4 Build EAR, deploy, verify via REST (admin:a): sender-based + file-number-in-subject cases returned; phone numbers extracted

## 3. Web client
- [x] 3.1 `EmailService.caseSuggestions(mailboxId?, subject, body, from)` → typed result
- [x] 3.2 Reader "matching cases" panel: suggested cases as deep-links to `/cases/:id`, sender contacts, phone numbers; loads lazily when a message is opened
- [x] 3.3 i18n (de/en); build web WAR, deploy, verify

## 4. Wrap-up
- [x] 4.1 Update `add-web-client` tasks.md (4.4 note)
- [x] 4.2 Archive change (fold delta into `specs/email-rest-api`)
