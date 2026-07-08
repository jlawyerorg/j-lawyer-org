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

/** A labelled contact detail entry, only rendered when the value is non-empty. */
export interface ContactField {
  /** i18n key suffix under `kontakte.field.*`. */
  key: string;
  value: string;
  /** Optional icon name (IconComponent) and href scheme for actionable values. */
  icon?: string;
  href?: string;
}

/** Full contact (RestfulContactV1) shaped for the detail view. */
export interface ContactDetail {
  id: string;
  type: ContactType;
  displayName: string;
  /** Salutation + title line (e.g. "Herr Dr."), may be empty. */
  honorific: string;
  company: string;
  department: string;
  /** Grouped, non-empty field sections for the overview tab. */
  contactFields: ContactField[];
  addressLines: string[];
  moreFields: ContactField[];
}
