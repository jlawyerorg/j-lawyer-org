/**
 * Contact (Adressen) domain models, aligned with the j-lawyer REST DTOs returned by
 * j-lawyer-io:
 *  - list:   GET /rest/v8/contacts/page -> RestfulContactPageV8 { total, offset, limit, items }
 *  - detail: GET /rest/v1/contacts/{id} -> RestfulContactV1
 *
 * The address book is a single global list (not per-user restricted). A contact is treated
 * as a company when it carries a company name, otherwise as a natural person.
 */

/** Distinguishes natural persons from companies (derived from the `company` field). */
export type ContactType = 'person' | 'company';

/** Server-side filter for the contact list. */
export type ContactFilter = 'all' | 'people' | 'companies';

/** List row — the fields the v8 list endpoint provides. */
/** A single recipient suggestion for the mail composer's address-book typeahead. */
export interface RecipientSuggestion {
  /** Contact display name shown in the dropdown. */
  label: string;
  /** The e-mail address inserted into the recipient field. */
  email: string;
}

export interface ContactOverview {
  id: string;
  type: ContactType;
  /** Human-readable display name (company, or "name, firstName"). */
  displayName: string;
  /** Secondary line: "zip city" (falls back to e-mail). */
  subtitle: string;
  city: string;
  zipCode: string;
  email: string;
  phone: string;
  /** Two-letter avatar initials derived from the display name. */
  initials: string;
}

/** An actionable contact channel (phone/e-mail/web), only rendered when the value is non-empty. */
export interface ContactField {
  /** i18n key suffix under `kontakte.field.*`. */
  key: string;
  value: string;
  /** Optional icon name (IconComponent) and href scheme for actionable values. */
  icon?: string;
  href?: string;
}

/** A label/value pair inside a detail section (label via `kontakte.field.<labelKey>`). */
export interface ContactKV {
  labelKey: string;
  value: string;
}

/** A grouped section of the contact detail (title via `kontakte.section.<key>`), non-empty only. */
export interface ContactSection {
  key: string;
  fields: ContactKV[];
}

/** A case the contact is involved in (GET /v5/contacts/{id}/cases -> RestfulCaseOverviewV1). */
export interface ContactCase {
  id: string;
  fileNumber: string;
  name: string;
  reason: string;
  /** ISO date (sanitized). */
  dateChanged: string;
  /** The contact's role in this case (party type name, e.g. "Mandant"); empty if unknown. */
  role: string;
  /** The party type's colour as a CSS hex string, or '' when none/unmatched. */
  roleColor: string;
}

/**
 * A document attached directly to a contact (GET /v7/contacts/{id}/documents ->
 * RestfulAddressDocumentV7). Unlike case documents these have no folder/favourite/tags — the
 * desktop AddressPanel likewise shows only name, date and size.
 */
export interface ContactDocument {
  id: string;
  name: string;
  /** Creation date, ISO (sanitized). */
  date: string;
  /** Last-change date, ISO (sanitized). */
  changeDate: string;
  /** Human-readable size (e.g. "34 KB"). */
  size: string;
  /** Raw size in bytes (for sorting). */
  sizeBytes: number;
  /** Upper-case file extension derived from the name. */
  ext: string;
}

/** Sort criteria for the contact documents tab. */
export type ContactDocSortKey = 'name' | 'date' | 'size';

/**
 * A relationship of a contact to another contact (GET /v8/contacts/{id}/relations ->
 * RestfulContactRelationV8). `label` is already the direction-correct wording as seen from the
 * contact the relationships were read for — it is administrable master data with a single wording
 * and therefore never translated.
 */
export interface ContactRelation {
  id: string;
  /** Direction-correct label of the relationship type, e.g. "ist Mutter von". */
  label: string;
  typeId: string;
  /** Catalogue name of the type, e.g. "Mutter – Kind". */
  typeName: string;
  /** True when the type reads the same in both directions. */
  symmetric: boolean;
  /** The type's colour as a CSS hex string ('' when none). */
  color: string;
  /** Optional free-text note; '' when none. */
  note: string;
  /** ISO timestamp (sanitized); '' when unset. */
  creationDate: string;
  createdBy: string;
  otherContactId: string;
  /** Display name of the other contact. */
  otherContactName: string;
  otherContactCompany: string;
  otherContactCity: string;
}

/**
 * A relationship type of the administrable catalogue (GET/POST/PUT/DELETE
 * /v8/configuration/contact-relation-types -> RestfulContactRelationTypeV8). A symmetric type reads
 * the same both ways, so its `labelTo` is kept equal to `labelFrom`.
 */
export interface ContactRelationType {
  id?: string;
  /** Catalogue name, unique, e.g. "Mutter – Kind". */
  name: string;
  /** Wording for the contact on the `from` side, e.g. "ist Mutter von". */
  labelFrom: string;
  /** Wording for the contact on the `to` side, e.g. "ist Kind von". */
  labelTo: string;
  symmetric: boolean;
  /** Grouping for the pickers (Familie, Vertretung, …); '' when none. */
  category: string;
  /** RGB colour as a packed integer (as stored by the desktop). */
  color: number;
  sequenceNumber: number;
  active: boolean;
}

/**
 * One entry of the relationship label picker. Picking a label fixes the type **and** the direction:
 * `reverse` marks the type's `labelTo`, which is stored with the two contacts swapped.
 */
export interface RelationLabelOption {
  typeId: string;
  label: string;
  category: string;
  reverse: boolean;
}

/** What the relationship dialog returns (on edit only `note` is relevant). */
export interface ContactRelationWrite {
  typeId: string;
  /** True when the picked label was the type's `labelTo` (the two contacts are stored swapped). */
  reverse: boolean;
  otherContactId: string;
  note: string;
}

/**
 * Fills the fields Jackson omits when they are empty, so pickers and administration always see plain
 * values. `active` defaults to true (an omitted flag means the server sent `active = true`).
 */
export function toContactRelationType(dto: Partial<ContactRelationType>): ContactRelationType {
  return {
    id: dto.id,
    name: dto.name ?? '',
    labelFrom: dto.labelFrom ?? '',
    labelTo: dto.labelTo ?? '',
    symmetric: !!dto.symmetric,
    category: dto.category ?? '',
    color: dto.color ?? 0,
    sequenceNumber: dto.sequenceNumber ?? 0,
    active: dto.active !== false,
  };
}

/**
 * The writable contact shape (RestfulContactV2), all fields optional. Used verbatim as the request
 * body for create/update and as the editor's working copy — on edit it is cloned from the loaded
 * contact so unedited fields (insurance, external ids, …) round-trip unchanged. Short 0/1 flags are
 * kept as numbers; everything else is a string.
 */
export interface ContactData {
  id?: string;
  title?: string; salutation?: string; gender?: string;
  firstName?: string; firstName2?: string; name?: string; birthName?: string; initials?: string;
  company?: string; department?: string; legalForm?: string;
  companyRegistrationNumber?: string; companyRegistrationCourt?: string;
  street?: string; streetNumber?: string; zipCode?: string; city?: string;
  country?: string; state?: string; district?: string; adjunct?: string;
  phone?: string; mobile?: string; fax?: string;
  email?: string; emailHome?: string; emailMisc?: string; website?: string; beaSafeId?: string;
  birthDate?: string; placeOfBirth?: string; dateOfDeath?: string; nationality?: string;
  profession?: string; role?: string; degreePrefix?: string; degreeSuffix?: string; titleInAddress?: string;
  vatId?: string; tin?: string;
  bankName?: string; bankCode?: string; bankAccount?: string; bankAccountOwner?: string;
  sepaReference?: string; sepaSince?: string; leitwegId?: string;
  legalProtection?: number; insuranceName?: string; insuranceNumber?: string; insurant?: string;
  trafficLegalProtection?: number; trafficInsuranceName?: string; trafficInsuranceNumber?: string; trafficInsurant?: string;
  motorLegalProtection?: number; motorInsuranceName?: string; motorInsuranceNumber?: string; motorInsurant?: string;
  taxDeduction?: number;
  complimentaryClose?: string; notice?: string;
  custom1?: string; custom2?: string; custom3?: string;
  externalId?: string; externalId2?: string; externalId3?: string; externalId4?: string; externalId5?: string;
}

/** Full contact (RestfulContactV2) shaped for the detail view. */
export interface ContactDetail {
  id: string;
  type: ContactType;
  displayName: string;
  /** Salutation + title line (e.g. "Herr Dr."), may be empty. */
  honorific: string;
  company: string;
  department: string;
  /** Actionable contact channels (phone/e-mail/web). */
  channels: ContactField[];
  /** Formatted postal address lines. */
  addressLines: string[];
  /** Grouped detail sections (person, organisation, bank, insurance, …), non-empty only. */
  sections: ContactSection[];
  /** Free-text note (notice), shown prominently. */
  notice: string;
}
