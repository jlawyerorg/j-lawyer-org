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
