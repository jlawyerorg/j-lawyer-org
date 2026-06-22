## ADDED Requirements

### Requirement: Policy holder for insurance blocks
The contact data model SHALL store the policy holder (Versicherungsnehmer) for each of
the three insurance blocks — general legal protection, traffic legal protection and motor
insurance — independently of the contact's own identity, because the contact is not
necessarily the insured party.

#### Scenario: Persisting a distinct policy holder
- **WHEN** a contact is saved with a policy holder name entered for general legal
  protection, traffic legal protection and/or motor insurance
- **THEN** each policy holder value is persisted on the contact and returned unchanged
  when the contact is reloaded

#### Scenario: Empty policy holder
- **WHEN** a contact is saved without a policy holder for an insurance block
- **THEN** the corresponding policy holder value is stored as null and no error occurs

### Requirement: Motor insurance availability flag
The contact data model SHALL store whether a motor insurance (Kraftfahrtversicherung) is
available as a boolean flag, mirroring the existing availability flags for general legal
protection and traffic legal protection.

#### Scenario: Toggling motor insurance availability
- **WHEN** the motor-insurance-available flag is set on a contact and the contact is saved
- **THEN** the flag is persisted and returned with the same value on reload

#### Scenario: Default for existing contacts
- **WHEN** the migration runs against contacts created before this change
- **THEN** their motor-insurance-available flag defaults to false (not available)

### Requirement: REST exposure of policy holder and motor availability fields
The contacts REST API (v1 and v2) SHALL expose the three policy holder fields and the
motor-insurance-available flag as additive, optional fields without changing or breaking
existing fields.

#### Scenario: Round-trip via REST v2
- **WHEN** a client creates or updates a contact via the v2 contacts endpoint with the
  new fields populated
- **THEN** the response and a subsequent GET return the same policy holder values and
  motor-insurance-available flag

#### Scenario: Backward compatibility
- **WHEN** an existing client that does not send the new fields creates or updates a
  contact
- **THEN** the request succeeds and the new fields are treated as null/false
