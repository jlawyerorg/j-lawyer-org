# email-rest-api delta

## ADDED Requirements

### Requirement: Email Case Suggestions
The REST API SHALL provide an endpoint that, given an email's subject, body, and sender,
returns a consolidated set of suggested cases, matching sender contacts, and extracted phone
numbers, so clients do not have to download the full set of file numbers or issue one request
per candidate. Suggested cases SHALL be limited to those the authenticated user may access.

#### Scenario: Suggestions from sender, subject, and body
- **WHEN** a POST request is made to `/rest/v7/email/mailboxes/{mailboxId}/case-suggestions` with `{ "subject": "...", "body": "...", "from": "Name <a@b.de>" }`
- **THEN** the response SHALL contain `suggestedCases`, `contacts`, `phoneNumbers`, `senderName`, and `senderEmail`
- **AND** `suggestedCases` SHALL include cases whose file number appears in the subject or body, cases referenced by a foreign file number found in the subject or body, and cases linked to contacts matching the sender address
- **AND** each suggested case SHALL carry `id`, `fileNumber`, `name`, `reason`, `archived`, and a `source` of `subjectBody`, `reference`, or `sender`

#### Scenario: Suggested cases are access-controlled and de-duplicated
- **WHEN** the heuristic matches a case the authenticated user is not allowed to see
- **THEN** that case SHALL be omitted from `suggestedCases`
- **AND** a case matched by more than one signal SHALL appear only once

#### Scenario: Sender has no matching contact
- **WHEN** the sender address matches no contact in the address book
- **THEN** `contacts` SHALL be an empty array
- **AND** `senderName` and `senderEmail` SHALL still be returned (so the client can offer to create a contact)

#### Scenario: Phone numbers extracted from the body
- **WHEN** the body contains phone numbers
- **THEN** `phoneNumbers` SHALL contain the distinct phone numbers detected in the body
