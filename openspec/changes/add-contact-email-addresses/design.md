## Context
Contacts store one e-mail address (`AddressBean.email`). Users need to record more than
one. Contacts are pushed to the cloud via CardDAV, where vCard `EMAIL` is a repeatable,
typed property — so the feature should map cleanly onto that standard. The contact search
is native SQL over flat columns of the `contacts` table and returns all matches unpaged.

## Goals / Non-Goals
- Goals:
  - Store up to three e-mail addresses per contact (primary / home / misc).
  - Keep the address search a single-table scan (no join-induced regression).
  - Stay close to vCard: emit each address as its own typed `EMAIL` property, and read
    multiple `EMAIL` properties back on vCard *file* import.
  - Additive REST changes; no broken API versions.
- Non-Goals:
  - Unlimited / dynamic number of addresses.
  - A normalised `@OneToMany` e-mail child entity.
  - A vCard import mapping in the CardDAV *cloud* connector (it is push-only today; only the
    desktop vCard *file* import is in scope).

## Decisions
- **Decision: three flat columns, not a child table.** vCard models `EMAIL` as a
  repeatable list, which suggests a `@OneToMany`. But the existing data model is
  deliberately wide (`phone`, `mobile`, `fax`, `email` are all flat columns) and the search
  is native SQL: `select id from contacts where … or ucase(email) like ? …`, returning all
  rows unpaged. A child table would force a join + `DISTINCT` into six search statements and
  multiply rows before they are all materialised. Three flat columns add zero joins and fit
  the existing pattern. Trade-off: a hard cap of three addresses and a theoretically lossy
  round-trip if an external client ever stored 4+ — accepted as covering the common cases.
- **Decision: keep `email` as the primary slot.** The existing column and most of its
  references (named query, REST `email`, display helper) stay as-is. The two new columns are
  `email_home` and `email_misc` (mapped to the `AddressBean.emailHome`/`emailMisc`
  properties). No data migration of existing values is needed.
- **Decision: vCard type mapping reflects the real semantics of each slot.**
  - primary (`email`) → `EMAIL;TYPE=WORK;PREF=1` — the firm's main/business address, marked
    preferred.
  - home (`emailHome`) → `EMAIL;TYPE=HOME`
  - misc (`emailMisc`) → `EMAIL` (no `HOME`/`WORK` type)
  - This re-types the primary address from the current `TYPE=HOME` to `TYPE=WORK`. Trade-off:
    existing synced contacts get their primary address re-categorised (HOME → WORK) in client
    UIs on the next sync. Accepted, because `WORK` is the correct semantics for a contact's
    main address in a law-firm context and it avoids two `TYPE=HOME` entries. The address
    value is never changed, only its type parameter.
  - Alternative considered: keeping the primary at `TYPE=HOME;PREF=1` for byte-level
    backward compatibility — rejected in favour of correct typing.
  - The desktop vCard *file* import (`ImportContactsDialog`) uses the inverse mapping when
    reading: an `EMAIL` with `PREF` or `TYPE=WORK` → primary, `TYPE=HOME` → home, anything
    else → misc; if a slot is already taken, overflow goes to the next free slot, ending in
    misc; addresses beyond three are dropped (consistent with the three-slot model). This is
    a separate code path from the CardDAV cloud connector, which remains push-only.
- **Decision: extend REST v1 + v2 only.** Mirrors the precedent set by
  `add-contact-insurant-fields`; these are the versions backed by the `RestfulContact`
  POJOs. Additive, optional fields keep older clients working unchanged.
- **Decision: the recipient picker offers every address; single-recipient call sites fall
  back.** Composing an e-mail in the client reads a contact's address in two shapes:
  - The party-based recipient menu `SendEmailFrame.addRecipientCandidate(AddressBean, …)`
    is the funnel for every party-driven flow (`addParty`, used by the case party panel,
    cloud-share mail, etc.). It builds one To/CC/BCC menu item per party from
    `ab.getEmail()`. It SHALL instead iterate over every populated address
    (`email`, `emailHome`, `emailMisc`) and create a menu entry per address, labelled so the
    user can tell them apart when a contact has more than one; a contact with a single
    address behaves exactly as before. This is the primary fix.
  - Direct single-recipient call sites that today do `dlg.setTo(x.getEmail())`
    (`InvolvedPartyEntryPanel`, `ShareDocumentsToCloudAction`,
    `AddRecipientSearchDialog.useSelection`) SHALL use a "primary, else first populated
    alternative" fallback so the To field is not left empty when only an alternative address
    exists, and the "no e-mail address recorded" guard SHALL consider all three fields.
  - A small shared helper on the contact/address side (e.g. `getAnyEmail()` returning the
    first non-empty of `email`/`emailHome`/`emailMisc`, and/or `getAllEmails()`) keeps the
    fallback logic in one place. Recipient search results / encryption indicators
    (`QuickEmailSearchThread`) and the AI-assistant contact JSON (`ToolRegistry`) likewise
    consider all three addresses rather than `email` only.
  - Out of scope / read-only (no change): pure display labels and tooltips
    (`IdentityPanel`, party tooltip), import previews (`ImportContactsDialog`), print stub
    generation, and all *system-user* / BEA / court-import call sites — those read user or
    institution e-mails, not contact `AddressBean` addresses.

## Risks / Trade-offs
- **Search parameter-index drift** → each of the six native-SQL statements binds parameters
  positionally; adding two `?` per statement requires matching `setString` calls in the
  right order. Mitigation: task 3.3 verifies `?`-count vs. binding-count, and the two
  subselect variants (which omit `zipCode`) are called out explicitly.
- **`.form` desync** → the GUI Builder breaks if `AddressPanel.java` and `AddressPanel.form`
  drift. Mitigation: edit both consistently (task 6.1).
- **Three-address cap** → external contacts with more addresses are truncated on a
  hypothetical future import. Accepted; documented in the spec.

## Migration Plan
- Flyway `V3_6_0_2__ContactsAddEmailAddresses.sql` adds two nullable `VARCHAR(250) BINARY`
  columns (typed exactly like the existing `contacts.email` column), plus a single-column
  index per field (`IDX_CONTACTS_EMAIL_HOME`, `IDX_CONTACTS_EMAIL_MISC`) mirroring the
  existing `IDX_EMAIL`. Note: these indexes (like `IDX_EMAIL` itself) do not accelerate the
  contact quick-search, which uses `ucase(col) LIKE '%term%'` — a leading-wildcard LIKE over
  a function-wrapped column cannot use a B-tree index. They are added for symmetry with the
  primary `email` column and the per-column index convention, and to serve exact-match
  lookups (e.g. a future dedup over alternative addresses)
  and bumps the DB version to `3.6.0.2`. Existing rows get `NULL` for both — no backfill.
- Rollback: drop the two columns; no other state changes.

## Open Questions
- None.
