## ADDED Requirements

### Requirement: Calendar Entry Creator Field

The REST representation of a calendar entry (`RestfulDueDateV6`) SHALL include a `createdBy`
field identifying the user who created the entry. The field SHALL be read-only and
server-authoritative: it is populated from the persisted creator on outbound responses and
SHALL NOT be settable by REST clients. On creation, the server SHALL record the authenticated
caller as the creator regardless of any `createdBy` value supplied in the request body.

#### Scenario: Creator returned in response

- **WHEN** a REST client retrieves a calendar entry as `RestfulDueDateV6`
- **THEN** the response includes a `createdBy` field holding the creator's principal id
  (empty or absent when the entry predates creator tracking)

#### Scenario: Creator is not client-settable

- **WHEN** a REST client creates a calendar entry and supplies a `createdBy` value in the
  request
- **THEN** the supplied value is ignored
- **AND** the entry's creator is recorded as the authenticated caller

#### Scenario: Older POJO versions unchanged

- **WHEN** a REST client uses the v1 or v4 calendar representation
- **THEN** those representations are unchanged and do not include the `createdBy` field
