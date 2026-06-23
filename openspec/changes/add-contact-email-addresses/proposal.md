# Change: Add additional e-mail addresses to contacts (primary / home / misc)

## Why
A contact can currently store only a single e-mail address (`AddressBean.email`), but in
practice a client, court or counterparty often has more than one (e.g. a business address
and a private one). Because contacts are synchronised to the cloud via CardDAV/vCard —
where `EMAIL` is a repeatable property — we want to support more than one address while
staying close to the vCard standard.

Rather than a normalised `@OneToMany` child table, we keep the existing wide, denormalised
`contacts` table design: the address search is native SQL that scans flat columns
(`ucase(email) like ?` alongside `phone`, `mobile`, `fax`, …) and returns **all** matches
unpaged. A child table would force a join + `DISTINCT` into that query and is not justified
here. Two additional flat columns cover the common cases without any join.

## What Changes
- Add two flat columns to the `contacts` table via a Flyway migration:
  - `email_home` — the "home/private" e-mail address
  - `email_misc` — a "misc/other" e-mail address
  - the existing `email` column keeps its role as the **primary** address (unchanged).
- Extend the `AddressBean` JPA entity with two `String` properties `emailHome`/`emailMisc`
  (mapped to columns `email_home`/`email_misc`) + getters/setters.
- Include the two new columns in the contact search so a query term matches any of the
  three addresses. The native-SQL search strings in `AddressService` that today contain
  `ucase(email) like ?` are extended with `or ucase(email_home) like ? or ucase(email_misc)
  like ?`, and the corresponding positional `setString` bindings are added.
- Extend the REST contact POJOs `RestfulContactV1` and `RestfulContactV2` (fields,
  getters/setters, `toAddressBean`/`fromAddressBean`) and the PUT/update field-copy block
  in `ContactsEndpointV1` and `ContactsEndpointV2`. Additive only — no existing API
  version changes behaviour or breaks.
- Extend the CardDAV push path so all populated addresses are written as separate vCard
  `EMAIL` properties:
  - primary (`email`) → `EMAIL;TYPE=WORK;PREF=1` (the firm's main/business address, marked
    as the preferred one)
  - home (`emailHome`) → `EMAIL;TYPE=HOME`
  - misc (`emailMisc`) → `EMAIL` (no `HOME`/`WORK` type)
  - This **re-types the primary address from `HOME` to `WORK`** compared to the current
    single-address sync; existing synced contacts will show their primary address as a
    business address after the next sync. The address value itself is unchanged.
  - The CardDAV *cloud sync* is **push-only** today (`getVCard`); it has no vCard import
    path, so no read/parse mapping is in scope there.
- Fix the desktop vCard *file* import (`ImportContactsDialog`) to distribute multiple vCard
  `EMAIL` properties across `email`/`emailHome`/`emailMisc`. Today it loops over all
  addresses but calls `ab.setEmail(...)` each time, so only the last address survives and
  the rest are silently lost. The new mapping reads the `EMAIL` `TYPE`/`PREF` parameters
  (the inverse of the write mapping: `PREF`/`WORK` → primary, `HOME` → home, the rest →
  misc, overflow into misc).
- Wire the two new addresses into the desktop `AddressPanel` (new text fields + labels,
  load, clear, change-detection, write-back, read-only handling) and keep the `.form`
  file in sync for the NetBeans GUI Builder.
- Make the desktop e-mail composition use all three addresses. The recipient picker
  `SendEmailFrame.addRecipientCandidate` — the funnel for every party-based flow — offers a
  To/CC/BCC entry per populated address instead of only the primary; the direct
  `setTo(getEmail())` call sites (`InvolvedPartyEntryPanel`, `ShareDocumentsToCloudAction`,
  `AddRecipientSearchDialog`) fall back to the first populated address; recipient-search
  results / encryption indicators and the AI-assistant contact JSON consider all three. Two
  convenience helpers `AddressBean.getAnyEmail()`/`getAllEmails()` centralise the logic.
  Pure display/read-only and system-user/BEA call sites are left unchanged.
- Make the existing party `_EMAIL` document placeholder priority-aware: instead of always
  using the primary `email`, it resolves to the primary address, or the home address if the
  primary is empty, or the misc address if both are empty (via `AddressBean.getAnyEmail()`).
  **No new placeholder is introduced.**
- No swagger step: `swagger.json` is auto-generated from the annotations on every build
  (see change `update-swagger-autogeneration`), so the new REST fields are picked up
  automatically.

## Impact
- Affected specs: `contact-emails` (new capability)
- Affected code:
  - `j-lawyer-server-entities/.../persistence/AddressBean.java`
  - `j-lawyer-server-entities/src/main/resources/db/migration/V3_6_0_2__ContactsAddEmailAddresses.sql` (new)
  - `j-lawyer-server/j-lawyer-server-ejb/.../services/AddressService.java` (6 search query
    strings + their parameter bindings)
  - `j-lawyer-server/j-lawyer-io/.../rest/v1/pojo/RestfulContactV1.java`, `.../rest/v1/ContactsEndpointV1.java`
  - `j-lawyer-server/j-lawyer-io/.../rest/v2/pojo/RestfulContactV2.java`, `.../rest/v2/ContactsEndpointV2.java`
  - `j-lawyer-cloud/.../NextcloudContactsConnector.java` (`getVCard`, ~line 986)
  - `j-lawyer-cloud/.../contacts/CloudContact.java` (two new fields)
  - `j-lawyer-server/j-lawyer-server-ejb/.../services/ContactSyncService.java` (`getCloudContact`, ~line 870)
  - `j-lawyer-client/.../editors/addresses/AddressPanel.java` + `AddressPanel.form`
  - `j-lawyer-client/.../mail/SendEmailFrame.java` (`addRecipientCandidate`, ~line 1687)
  - `j-lawyer-client/.../mail/AddRecipientSearchDialog.java` (~line 869),
    `.../mail/QuickEmailSearchThread.java`, `.../editors/addresses/QuickAddressSearchThread.java`
  - `j-lawyer-client/.../editors/files/InvolvedPartyEntryPanel.java` (~line 1310)
  - `j-lawyer-client/.../cloud/ShareDocumentsToCloudAction.java` (~line 817)
  - `j-lawyer-client/.../assistant/ToolRegistry.java` (contact JSON, ~lines 2218, 4466)
  - `j-lawyer-client/.../configuration/ImportContactsDialog.java` (vCard file import, ~line 1002)
  - `j-lawyer-server/j-lawyer-server-ejb/.../org/jlawyer/utils/PlaceHolderServerUtils.java`
    (party `_EMAIL` placeholder, ~line 859)
  - `AddressBean.getAnyEmail()` / `getAllEmails()` convenience helpers
- No EJB `AddressService.createAddress`/`updateAddress` change needed: they merge the whole
  `AddressBean` (`addressFacade.create/edit`); no per-field mapping exists.
- `AddressBean.findByEmail` named query and the `toString`/display helper keep referencing
  the primary `email` only — unchanged.
