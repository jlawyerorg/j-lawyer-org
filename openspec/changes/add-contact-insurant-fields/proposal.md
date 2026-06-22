# Change: Add policy-holder (Versicherungsnehmer) fields to contacts

## Why
Contacts currently store the insurance company and policy number for general legal
protection (Rechtsschutz), traffic legal protection (Verkehrsrechtsschutz) and motor
insurance (Kraftfahrtversicherung), but not who the policy holder (Versicherungsnehmer)
is — the contact is not always the insured party. Motor insurance also lacks an
"available yes/no" flag that the other two insurance blocks already have. The desktop
form has already been extended with the input components; the data model, REST API and a
Flyway migration must follow so the values are actually persisted and exposed.

## What Changes
- Add four columns to the `contacts` table via a Flyway migration:
  - `insurant` — policy holder for general legal protection
  - `trafficInsurant` — policy holder for traffic legal protection
  - `motorInsurant` — policy holder for motor insurance
  - `motorLegalProtection` — motor insurance available (yes/no), mirroring the existing
    `legalProtection` / `trafficLegalProtection` flags
- Extend the `AddressBean` JPA entity with the four properties (three `String`, one
  `boolean`).
- Extend the REST contact POJOs `RestfulContactV1` and `RestfulContactV2` (fields,
  getters/setters, `toAddressBean`/`fromAddressBean`) and the PUT/update mapping in
  `ContactsEndpointV1` and `ContactsEndpointV2`. Additive only — no existing API version
  changes behaviour or breaks.
- Wire the already-present desktop components in `AddressPanel` (load, clear,
  change-detection, write-back) and rename the placeholder field `jTextField1` to
  `txtInsurant`, keeping the `.form` file in sync.
- No swagger step: `swagger.json` is auto-generated from the annotations on every build
  (see change `update-swagger-autogeneration`), so the new REST fields are picked up
  automatically.

## Impact
- Affected specs: `contact-insurance` (new capability)
- Affected code:
  - `j-lawyer-server-entities/.../persistence/AddressBean.java`
  - `j-lawyer-server-entities/src/main/resources/db/migration/V3_6_0_1__ContactsAddInsurants.sql` (new)
  - `j-lawyer-server/j-lawyer-io/.../rest/v1/pojo/RestfulContactV1.java`, `.../rest/v1/ContactsEndpointV1.java`
  - `j-lawyer-server/j-lawyer-io/.../rest/v2/pojo/RestfulContactV2.java`, `.../rest/v2/ContactsEndpointV2.java`
  - `j-lawyer-client/.../editors/addresses/AddressPanel.java` + `AddressPanel.form`
- No EJB service change needed: `AddressService.createAddress`/`updateAddress` merge the
  whole `AddressBean` (`addressFacade.create/edit`); no per-field mapping exists.
- Cloud (CardDAV) contact sync does not map insurance fields today — out of scope.
