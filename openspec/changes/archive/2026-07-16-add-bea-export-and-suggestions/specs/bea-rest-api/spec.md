# bea-rest-api delta

## ADDED Requirements

### Requirement: beA Message Export
The REST API SHALL provide an endpoint that exports a beA message as a `.bea` file — the whole
message (including its attachments) serialized to XML in the same format the desktop client
produces — so a client can store the complete message in a case as a single document.

#### Scenario: Export a message
- **WHEN** a GET request is made to `/rest/v8/bea/postboxes/{safeId}/messages/{messageId}/export`
- **THEN** the response SHALL contain `fileName` and `contentBase64`
- **AND** `contentBase64` SHALL be the Base64-encoded XML serialization of the whole message
- **AND** `fileName` SHALL end in `.bea` and be derived from the message's reference number, subject, sender and recipient

#### Scenario: Unknown message
- **WHEN** the message id does not exist in the postbox
- **THEN** the response SHALL return HTTP 404

### Requirement: beA Case Suggestions
The REST API SHALL provide an endpoint that, given an opened beA message's subject, body and the two
reference numbers (sender and court) plus the sender name, returns suggested cases, matching sender
contacts and extracted phone numbers, so clients do not have to download the full set of file
numbers. Suggested cases SHALL be limited to those the authenticated user may access.

#### Scenario: Suggestions from reference numbers, subject and body
- **WHEN** a POST request is made to `/rest/v8/bea/case-suggestions` with `{ "subject": "...", "body": "...", "referenceNumber": "...", "referenceJustice": "...", "senderName": "..." }`
- **THEN** the response SHALL contain `suggestedCases`, `contacts`, `phoneNumbers`, `senderName`, and `senderEmail`
- **AND** `suggestedCases` SHALL include cases whose own file number appears in the subject, body or either reference number, cases referenced by a foreign file number found there, and cases linked to contacts matching the sender name
- **AND** each suggested case SHALL carry `id`, `fileNumber`, `name`, `reason`, `archived`, and a `source` of `subjectBody`, `reference`, or `sender`

#### Scenario: Suggested cases are access-controlled and de-duplicated
- **WHEN** the heuristic matches a case the authenticated user is not allowed to see
- **THEN** that case SHALL be omitted from `suggestedCases`
- **AND** a case matched by more than one signal SHALL appear only once
