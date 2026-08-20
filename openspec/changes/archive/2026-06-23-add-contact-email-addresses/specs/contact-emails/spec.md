## ADDED Requirements

### Requirement: Multiple e-mail addresses per contact
The contact data model SHALL store up to three e-mail addresses per contact — a primary
address, a home address and a misc address — as independent fields. The existing primary
address field SHALL retain its current meaning and value; the home and misc addresses are
additional, optional fields.

#### Scenario: Persisting additional addresses
- **WHEN** a contact is saved with a home and/or misc e-mail address entered
- **THEN** each address is persisted on the contact and returned unchanged when the
  contact is reloaded

#### Scenario: Empty additional addresses
- **WHEN** a contact is saved without a home or misc e-mail address
- **THEN** the corresponding field is stored as null and no error occurs

#### Scenario: Existing contacts after migration
- **WHEN** the migration runs against contacts created before this change
- **THEN** their primary address is preserved and the home and misc addresses default to
  null

### Requirement: Search across all e-mail addresses
The contact search SHALL match a search term against any of the three e-mail addresses
(primary, home, misc), so that a contact is found regardless of which of its addresses the
term refers to.

#### Scenario: Match on a non-primary address
- **WHEN** a search is performed with a term that matches only the home or only the misc
  address of a contact
- **THEN** that contact is included in the search results

#### Scenario: Match on the primary address is unchanged
- **WHEN** a search is performed with a term that matches the primary address
- **THEN** that contact is still returned exactly as before this change

### Requirement: REST exposure of additional e-mail addresses
The contacts REST API (v1 and v2) SHALL expose the home and misc e-mail addresses as
additive, optional fields without changing or breaking existing fields, including the
existing primary `email` field.

#### Scenario: Round-trip via REST v2
- **WHEN** a client creates or updates a contact via the v2 contacts endpoint with the
  home and misc addresses populated
- **THEN** the response and a subsequent GET return the same primary, home and misc
  addresses

#### Scenario: Backward compatibility
- **WHEN** an existing client that does not send the new fields creates or updates a
  contact
- **THEN** the request succeeds, the primary address behaves as before and the new fields
  are treated as null

### Requirement: E-mail composition uses all addresses of a contact
When composing an e-mail in the desktop client, the system SHALL make every populated
e-mail address of a contact (primary, home, misc) usable as a recipient, not only the
primary address.

#### Scenario: Recipient picker offers every address
- **WHEN** a contact (party) that has more than one populated e-mail address is offered in
  the recipient picker
- **THEN** a distinguishable To/CC/BCC entry is available for each populated address

#### Scenario: Fallback when the primary address is empty
- **WHEN** a "send e-mail to contact" action is invoked for a contact that has no primary
  address but has a home or misc address
- **THEN** the recipient is prefilled from the first populated alternative address and no
  "no e-mail address recorded" error is shown

#### Scenario: Single address unchanged
- **WHEN** a contact has only the primary e-mail address
- **THEN** e-mail composition behaves exactly as before this change

### Requirement: Document e-mail placeholder resolves by priority
The existing party e-mail document placeholder (`_EMAIL`) SHALL resolve to the first
populated address of the party in the order primary, then home, then misc. No additional
placeholder is introduced for the home or misc addresses.

#### Scenario: Primary address present
- **WHEN** a document with the party `_EMAIL` placeholder is generated for a party that has
  a primary e-mail address
- **THEN** the placeholder is replaced with the primary address

#### Scenario: Falling back to an alternative address
- **WHEN** the placeholder is generated for a party whose primary address is empty but which
  has a home and/or misc address
- **THEN** the placeholder is replaced with the home address, or the misc address if the
  home address is also empty

#### Scenario: No address present
- **WHEN** the placeholder is generated for a party that has none of the three addresses
- **THEN** the placeholder is replaced with an empty value, as before

### Requirement: CardDAV publishing of all e-mail addresses
When a contact is pushed to the cloud via CardDAV, the system SHALL publish every
populated e-mail address as a separate vCard `EMAIL` property, with the primary address
marked as the preferred one.

#### Scenario: Multiple addresses in the vCard
- **WHEN** a contact with a primary, a home and a misc e-mail address is synchronised
- **THEN** the generated vCard contains three `EMAIL` properties and the primary address
  carries the `PREF` parameter

#### Scenario: Only the primary address present
- **WHEN** a contact with only the primary e-mail address is synchronised
- **THEN** the generated vCard contains a single `EMAIL` property for the primary address

### Requirement: vCard file import preserves multiple e-mail addresses
When a contact is imported from a vCard file, the system SHALL preserve up to three of the
vCard's `EMAIL` properties by distributing them across the primary, home and misc address
fields, rather than keeping only one.

#### Scenario: Multiple addresses in the imported vCard
- **WHEN** a vCard containing two or three `EMAIL` properties is imported
- **THEN** the created contact has those addresses distributed across its primary, home and
  misc fields (the preferred/work address as primary), and none of them is silently
  discarded

#### Scenario: Single address in the imported vCard
- **WHEN** a vCard containing a single `EMAIL` property is imported
- **THEN** the created contact has that address as its primary e-mail address
