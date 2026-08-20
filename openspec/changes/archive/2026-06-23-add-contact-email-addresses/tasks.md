## 1. Data model

- [x] 1.1 Add two properties to `AddressBean` (`j-lawyer-server-entities/.../persistence/AddressBean.java`),
  next to the existing `email` field (~line 814):
  - `String emailHome` → `@Column(name = "email_home")`
  - `String emailMisc` → `@Column(name = "email_misc")`
- [x] 1.2 Add getters/setters: `getEmailHome`/`setEmailHome`, `getEmailMisc`/`setEmailMisc`.
- [x] 1.3 Add convenience helpers on `AddressBean` for the client recipient logic (§7):
  - `String getAnyEmail()` — the first non-empty of `email`, `emailHome`, `emailMisc` (or
    null/empty if none).
  - `List<String> getAllEmails()` — all non-empty addresses in order primary → home → misc.
- [x] 1.4 Leave the `AddressBean.findByEmail` named query (line 703) and the display helper
  (~line 2001) untouched — they intentionally use the primary `email` only.

## 2. Flyway migration

- [x] 2.1 Create `j-lawyer-server-entities/src/main/resources/db/migration/V3_6_0_2__ContactsAddEmailAddresses.sql`.
  The two new columns MUST be typed exactly like the existing `contacts.email` column,
  which is `VARCHAR(250) BINARY` (table charset utf8 → `utf8_bin`) — not the `VARCHAR(255)`
  default the insurant migration used:
  - `ALTER TABLE contacts ADD email_home VARCHAR(250) BINARY DEFAULT NULL;`
  - `ALTER TABLE contacts ADD email_misc VARCHAR(250) BINARY DEFAULT NULL;`
  - add a single-column index per new field, mirroring the existing `IDX_EMAIL` on `email`
    and the per-column index convention for all searchable contact columns:
    - `alter table contacts add index IDX_CONTACTS_EMAIL_HOME (email_home);`
    - `alter table contacts add index IDX_CONTACTS_EMAIL_MISC (email_misc);`
  - bump `jlawyer.server.database.version` to `3.6.0.2` + `commit;`

## 3. Contact search

- [x] 3.1 In `AddressService` (`j-lawyer-server/j-lawyer-server-ejb/.../services/AddressService.java`),
  extend every search query string that contains `ucase(email) like ?` with
  `or ucase(email_home) like ? or ucase(email_misc) like ?` (lines ~728, ~858, ~1165, ~1296,
  ~1480, ~1565).
- [x] 3.2 For each of those statements add the two extra positional `setString` bindings
  (same search term as the existing `email` binding); keep all subsequent parameter
  indexes correct. The two `addressKey in (subselect …)` queries (~1296, ~1565) bind the
  same term set — update their bindings too.
- [x] 3.3 Verify the parameter count matches the `?` count for each statement (the subselect
  queries omit `zipCode` — do not add a binding that isn't in the SQL).

## 4. REST API (additive, v1 + v2)

- [x] 4.1 `RestfulContactV1`: add `emailHome`, `emailMisc` (`String`) fields + getters/setters,
  map both directions in `toAddressBean()` and `fromAddressBean()` (mirror `email`).
- [x] 4.2 `ContactsEndpointV1`: add `setEmailHome`/`setEmailMisc` in the PUT/update
  field-copy block (next to the existing `setEmail`).
- [x] 4.3 `RestfulContactV2`: same as 4.1.
- [x] 4.4 `ContactsEndpointV2`: same as 4.2.

## 5. CardDAV / vCard push

- [x] 5.1 In `NextcloudContactsConnector.getVCard(...)` (~line 986) replace the single
  `addEmail` block so all populated addresses are emitted:
  - primary: `Email pref = v.addEmail(c.getEmail(), EmailType.WORK); pref.setPref(1);`
  - home: `if (c.getEmailHome()!=null) v.addEmail(c.getEmailHome(), EmailType.HOME);`
  - misc: `if (c.getEmailMisc()!=null) v.addEmail(c.getEmailMisc());`
  - add the `ezvcard.property.Email` import (`EmailType` is already imported).
- [x] 5.2 `CloudContact` (`j-lawyer-cloud/.../contacts/CloudContact.java`): add `emailHome`,
  `emailMisc` (`String`) fields + getters/setters (mirror `email`, ~line 696/815).
- [x] 5.3 `ContactSyncService.getCloudContact(...)` (~line 870): set
  `c.setEmailHome(contact.getEmailHome())` and `c.setEmailMisc(contact.getEmailMisc())`.

## 6. Desktop client wiring

- [x] 6.1 Add `txtEmailHome`/`txtEmailMisc` text fields, the `jLabel60`/`jLabel61` labels
  ("E-Mail (privat)" / "E-Mail (sonstige)") and the `cmdSendEmailHome`/`cmdSendEmailMisc`
  send buttons to `AddressPanel` (`j-lawyer-client/.../editors/addresses/AddressPanel.java`),
  keeping `AddressPanel.form` in sync (NetBeans GUI Builder); the primary field label is
  "E-Mail (primär)". (Done in the working tree.)
- [x] 6.2 Send-button behaviour: extract a shared `sendEmailTo(String recipient)` helper and
  route `cmdSendEmail`/`cmdSendEmailHome`/`cmdSendEmailMisc` through it; extend
  `enableEmailButton()` to enable/disable all three buttons by field content; add
  `keyPressed` listeners on the two new fields (sharing `txtEmailKeyPressed`) in both
  `.java` and `.form`. (Done in the working tree.)
- [x] 6.3 `setAddressDTO(...)` (~line 985): load the two new fields from the bean
  (`getEmailHome`/`getEmailMisc`) and call `enableEmailButton()`. **Blocked on §1** (entity
  getters do not exist yet).
- [x] 6.4 `clear()`/reset (~line 1352): clear the two new fields and call `enableEmailButton()`.
- [x] 6.5 change-detection (`isChanged`): compare the two new fields vs. the bean.
- [x] 6.6 write-back: copy the two new fields into the `AddressBean`
  (`setEmailHome`/`setEmailMisc`). **Blocked on §1.**
- [x] 6.7 `setReadOnly(...)` (~line 892): include the two new text fields.

## 7. Desktop client — e-mail recipient usage

All sites read a contact's address from an `AddressBean` (or the `AddressBean` behind an
`ArchiveFileAddressesBean`); they must consider `emailHome`/`emailMisc` too. **Blocked on §1**
(the `getAnyEmail`/`getAllEmails` helpers and the new getters).

- [x] 7.1 `SendEmailFrame.addRecipientCandidate(AddressBean ab, PartyTypeBean ptb)`
  (`com.jdimension.jlawyer.client.mail`, ~line 1687): instead of one To/CC/BCC menu item
  built from `ab.getEmail()`, iterate over `ab.getAllEmails()` and add one To, one CC and
  one BCC `JCheckBoxMenuItem` per populated address. When the contact has more than one
  address, include the address (or its slot label primär/privat/sonstige) in the menu text
  so entries are distinguishable; keep the existing colour and encryption-lock icon logic
  per item. A single-address contact renders exactly as today.
- [x] 7.2 `AddRecipientSearchDialog.useSelection()` (~line 869): replace `ab.getEmail()`
  with `ab.getAnyEmail()` so a contact whose only address is home/misc can still be added
  to the To field.
- [x] 7.3 `InvolvedPartyEntryPanel.mnuSendEmailActionPerformed(...)` (~line 1310): base the
  "no e-mail address recorded" guard on `getAnyEmail()`/`getAllEmails()` and set the To
  field from `getAnyEmail()` (the party list still flows through `addParty` → §7.1 for the
  full picker).
- [x] 7.4 `ShareDocumentsToCloudAction` (~line 817): `dlg.setTo(recipient.getAnyEmail())`.
- [x] 7.5 Recipient search results / encryption indicator
  (`QuickEmailSearchThread` ~line 731, `QuickAddressSearchThread` ~line 738): show the
  contact when any of the three addresses is present, and base the crypto-lock decision on
  `getAnyEmail()` rather than `getEmail()`.
- [x] 7.6 AI-assistant contact serialization (`ToolRegistry` ~lines 2218 and 4466): include
  `emailHome`/`emailMisc` in the contact/address JSON alongside `email`.
- [x] 7.7 vCard *file* import `ImportContactsDialog.cmdChooseVCardActionPerformed(...)`
  (~lines 1002-1008): the loop `for (Email e : v.getEmails()) ab.setEmail(e.getValue())`
  currently keeps only the last address. Replace it so multiple `EMAIL` properties are
  distributed across `email`/`emailHome`/`emailMisc` using the inverse type mapping
  (`PREF`/`WORK` → primary, `HOME` → home, else → misc; overflow into the next free slot,
  ending in misc; 4th+ addresses dropped). Update the preview row accordingly.

## 8. Document placeholder replacement (server)

- [x] 8.1 `PlaceHolderServerUtils` (`org.jlawyer.utils`, ~line 859-860): the party `_EMAIL`
  placeholder is filled from `val(selected.getEmail())`. Change it to
  `val(selected.getAnyEmail())` so the placeholder resolves to the primary address, or — if
  the primary is empty — the home address, or — if both are empty — the misc address. **No
  new placeholder is introduced**; only the fill logic of the existing `_EMAIL` placeholder
  becomes priority-aware. **Blocked on §1** (`AddressBean.getAnyEmail`).
- [x] 8.2 Leave the unrelated `PROFIL_EMAIL` (company profile) and `USER_EMAIL` (document
  author) placeholders untouched — they resolve to user/profile e-mails, not contact
  addresses.
- Note: `_EMAIL` resolution is centralised server-side. The client performs no e-mail
  placeholder substitution of its own — mass mail (`GenerateMassMailDocumentsDialog`),
  "add document from template" (`AddDocumentFromTemplateDialog`) and the REST template
  endpoints (`TemplatesEndpointV6`) all delegate to `SystemManagement.getPlaceHolderValues`
  → `PlaceHolderServerUtils`. The single change in §8.1 therefore covers all of them; no
  client-side placeholder task is needed. (`AddDocumentFromTemplateDialog` only substitutes
  placeholders in the document file *name*, not e-mail content.)

## 9. API spec

- [x] 9.1 No manual step: `swagger.json` is regenerated from the annotations on every build
  and packaged into the war (see change `update-swagger-autogeneration`). The new REST
  fields appear automatically.

## 10. Verification

- [ ] 10.1 Build the reactor (`./build-fast.sh`).
- [ ] 10.2 Verify a search term matching only the home/misc address returns the contact.
- [ ] 10.3 Verify create/read/update of a contact via REST v2 round-trips all three addresses.
- [ ] 10.4 Verify the desktop form loads, edits and saves the three addresses.
- [ ] 10.5 Verify a CardDAV-synced contact shows all populated addresses as separate
  `EMAIL` lines with the primary marked `PREF`.
- [ ] 10.6 Verify the recipient picker (`SendEmailFrame`) offers a separate To/CC/BCC entry
  for each populated address of a multi-address party.
- [ ] 10.7 Verify "send e-mail to party" / cloud-share mail prefills the To field from an
  alternative address when the primary is empty.
- [ ] 10.8 Verify importing a vCard file with two or three `EMAIL` properties creates a
  contact with all of them distributed across primary/home/misc (no longer only the last).
- [ ] 10.9 Verify the `_EMAIL` document placeholder resolves to the home (or misc) address
  for a party whose primary address is empty.
