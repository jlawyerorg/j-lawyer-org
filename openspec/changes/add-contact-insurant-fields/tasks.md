## 1. Data model

- [x] 1.1 Add four properties to `AddressBean` (`j-lawyer-server-entities/.../persistence/AddressBean.java`):
  - `String insurant` → `@Column(name = "insurant")`
  - `String trafficInsurant` → `@Column(name = "trafficInsurant")`
  - `String motorInsurant` → `@Column(name = "motorInsurant")`
  - `boolean motorLegalProtection` → `@Basic(optional = false) @Column(name = "motorLegalProtection")`
- [x] 1.2 Add getters/setters: `getInsurant`/`setInsurant`, `getTrafficInsurant`/`setTrafficInsurant`,
  `getMotorInsurant`/`setMotorInsurant`, `isMotorLegalProtection`/`setMotorLegalProtection`.

## 2. Flyway migration

- [x] 2.1 Create `j-lawyer-server-entities/src/main/resources/db/migration/V3_6_0_1__ContactsAddInsurants.sql`
  following the `V3_5_0_7__ContactsAddTaxDeduction.sql` pattern:
  - `ALTER TABLE contacts ADD insurant VARCHAR(255) DEFAULT NULL;`
  - `ALTER TABLE contacts ADD trafficInsurant VARCHAR(255) DEFAULT NULL;`
  - `ALTER TABLE contacts ADD motorInsurant VARCHAR(255) DEFAULT NULL;`
  - `ALTER TABLE contacts ADD motorLegalProtection BIT(1) DEFAULT 0;`
  - bump `jlawyer.server.database.version` to `3.6.0.1` + `commit;`

## 3. REST API (additive, v1 + v2)

- [x] 3.1 `RestfulContactV1`: add 4 fields (`insurant`, `trafficInsurant`, `motorInsurant` as
  `String`; `motorLegalProtection` as `short`), getters/setters, map both directions in
  `toAddressBean()` and `fromAddressBean()` (boolean ↔ short 0/1 like `legalProtection`).
- [x] 3.2 `ContactsEndpointV1`: add the 4 setters in the PUT/update field-copy block (~line 898).
- [x] 3.3 `RestfulContactV2`: same as 3.1.
- [x] 3.4 `ContactsEndpointV2`: same as 3.2 (~line 1016).

## 4. Desktop client wiring

- [x] 4.1 Rename `jTextField1` → `txtInsurant` in `AddressPanel.java` and `AddressPanel.form`
  (was already renamed to `txtInsurant` in the working tree).
- [x] 4.2 `setAddressDTO(...)`: set the 4 components from the bean.
- [x] 4.3 `clear()`/reset: clear the 4 components.
- [x] 4.4 change-detection (`isChanged`): compare the 4 components vs. the bean.
- [x] 4.5 write-back: copy the 4 components into the `AddressBean`.
- [x] 4.6 `setReadOnly(...)`: include the 3 new text fields and the new checkbox.

## 5. API spec

- [x] 5.1 No manual step: `swagger.json` is regenerated from the annotations on every build
  and packaged into the war (see change `update-swagger-autogeneration`). The new REST
  fields appear automatically.

## 6. Verification

- [ ] 6.1 Build the reactor (`./build-fast.sh`).
- [ ] 6.2 Manually verify create/read/update of a contact via REST v2 round-trips all 4 fields.
- [ ] 6.3 Manually verify the desktop form loads, edits and saves the 4 fields.
