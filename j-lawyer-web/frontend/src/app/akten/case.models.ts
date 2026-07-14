/**
 * Case domain models, aligned with the j-lawyer REST DTOs actually returned by
 * j-lawyer-io (verified against a live server):
 *  - list:      GET /rest/v1/cases/list/active  -> { id, fileNumber, name, reason, dateChanged }
 *  - detail:    GET /rest/v1/cases/{id}          -> RestfulCaseV1
 *  - parties:   GET /rest/v1/cases/{id}/parties  -> RestfulPartyV1[]
 *  - duedates:  GET /rest/v1/cases/{id}/duedates -> RestfulDueDateV1[]
 *  - documents: GET /rest/v1/cases/{id}/documents
 */

/** Display status derived on the client (the list DTO carries no status). */
export type CaseStatus = 'open' | 'dueToday' | 'closed';

/** List row — the fields the v8 list endpoint provides (richer than v1). */
export interface CaseOverview {
  id: string;
  /** Aktenzeichen */
  fileNumber: string;
  name: string;
  /** Betreff/Grund */
  reason: string;
  /** Rechtsgebiet */
  subjectField: string;
  lawyer: string;
  archived: boolean;
  /** Derived from `archived` (the list has no due-date data, so no dueToday here). */
  status: CaseStatus;
  /** ISO date of last change (sanitized, no [UTC] suffix). */
  lastChanged: string;
}

/** Involved party. `involvementType` is a free German label from the server (e.g. "Mandant"). */
export interface Party {
  id: string;
  involvementType: string;
  /** Resolved contact name; may be empty if the server did not include it. */
  contact: string;
  /** The party's own reference/file mark ("Zeichen"), e.g. their case number; empty when none. */
  reference: string;
  // Editable detail fields (mirroring the desktop InvolvedPartyEntryPanel).
  /** Resolved name of the linked address (read-only display in the editor). */
  contactName: string;
  /** Free-text contact person ("Ansprechpartner"). */
  contactPerson: string;
  custom1: string;
  custom2: string;
  custom3: string;
  /** Id of the linked address/contact (round-tripped on update). */
  addressId: string;
  /**
   * The party type's colour as a CSS hex string (e.g. "#c62828"), resolved from the
   * configured party types by matching `involvementType` (= party-type name). Empty when the
   * party type has no colour or could not be matched.
   */
  color: string;
}

/** Deadline / follow-up. `type` is derived from the server type (RESPITE -> deadline). */
export interface DueDate {
  id: string;
  reason: string;
  /** ISO date (sanitized). */
  dueDate: string;
  done: boolean;
  assignee: string;
  type: 'deadline' | 'followup';
  /**
   * Colour of the calendar this entry belongs to, as a CSS hex string, resolved from the
   * configured calendars via the entry's calendar id. Empty when unknown; the view then falls
   * back to a type-based colour.
   */
  calendarColor: string;
  // Extra fields captured so the entry can be opened in the calendar editor for editing.
  description: string;
  location: string;
  /** ISO end date/time (sanitized); empty when none. */
  endDate: string;
  reminderMinutes: number;
  /** Calendar setup id this entry belongs to; needed to edit. */
  calendarId: string;
  /** Raw server type token: 'EVENT' | 'RESPITE' | 'FOLLOWUP'. */
  restType: string;
}

/** A case label ("Etikett", GET /v1/cases/{id}/tags). */
export interface CaseTag {
  id: string;
  name: string;
  /** Selected value for a list/multi-value tag; empty for a plain single-value label. */
  value: string;
}

/** A configured list-tag definition (GET /v7/configuration/tags/multivalue/case): a name + its value set. */
export interface MultiValueTagDef {
  tagName: string;
  values: string[];
}

/** A user group (all groups: GET /v6/security/groups; case's allowed groups: GET /v7/cases/{id}/groups). */
export interface CaseGroup {
  id: string;
  name: string;
  /** Short code; needed when PUTting the full authorized-group set back. */
  abbreviation: string;
}

/** An instant message linked to the case (GET /v7/cases/{id}/messages). */
export interface CaseMessage {
  id: string;
  /** ISO timestamp (sanitized). */
  sent: string;
  /** Author username. */
  sender: string;
  content: string;
}

/** Document shown in the case. */
export interface CaseDocument {
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
  /** Upper-case file extension derived from the name (e.g. "PDF"). */
  ext: string;
  /**
   * Id of the folder the document lives in; empty when it belongs to the root ("Dokumente").
   * Note: root documents may carry either no folder id or the root folder's own id.
   */
  folderId: string;
  favorite: boolean;
  /** Document version (>= 1). */
  version: number;
  /** First label colour as CSS hex, or '' when unset. */
  highlight1: string;
  /** Second label colour as CSS hex, or '' when unset. */
  highlight2: string;
  /** Tag/label names attached to the document. */
  tags: string[];
}

/**
 * A case document folder (GET /v3/cases/{id}/folders). The endpoint returns the nested root
 * node directly (its `children` hold the sub-folders); `id` of the root may equal the case id.
 */
export interface DocFolder {
  id: string;
  name: string;
  children: DocFolder[];
}

/** Document sort criteria offered in the documents tab (mirrors the desktop sort toggles). */
export type DocSortKey = 'name' | 'date' | 'size' | 'type' | 'favorite' | 'folder';
export type SortDir = 'asc' | 'desc';
/** Which date the documents view shows/sorts by (mirrors the desktop DATE_DISPLAY_MODE). */
export type DocDateMode = 'change' | 'creation';

// Preview primitives (DocPreviewKind, previewKindOf, mimeOf) now live in
// shared/document-preview.models.ts so the case detail and the fulltext search share one
// preview implementation. Re-exported here for existing importers.
export type { DocPreviewKind } from '../shared/document-preview.models';
export { previewKindOf, mimeOf } from '../shared/document-preview.models';

/** A case change-history (audit trail) entry (GET /v8/cases/{id}/history). */
export interface CaseHistoryEntry {
  id: string;
  /** Login name of the user who caused the change. */
  principal: string;
  /** Epoch milliseconds (UTC). */
  changeDate: number;
  changeDescription: string;
}

/** An invoice attached to the case (GET /v7/cases/{id}/invoices). */
export interface CaseInvoice {
  id: string;
  invoiceNumber: string;
  name: string;
  status: string;
  /** Net total. */
  total: number;
  /** Gross total (incl. VAT). */
  totalGross: number;
  currency: string;
  /** ISO date (sanitized, no [UTC] suffix); empty if unset. */
  dueDate: string;
  creationDate: string;
}

/** A payment attached to the case (GET /v8/cases/{id}/payments). */
export interface CasePayment {
  id: string;
  paymentNumber: string;
  name: string;
  reason: string;
  status: string;
  total: number;
  currency: string;
  /** ISO date (sanitized, no [UTC] suffix); empty if unset. */
  targetDate: string;
  creationDate: string;
}

/**
 * An entry of the case account ("Aktenkonto", GET /v7/cases/{id}/accountentries). Each entry
 * has up to three debit/credit pairs: earnings/spendings (fees), escrow in/out (Fremdgeld)
 * and expenditures in/out (Auslagen). `total` is the entry's net effect on the balance.
 */
export interface AccountEntry {
  id: string;
  /** ISO date (sanitized, no [UTC] suffix); empty if unset. */
  date: string;
  description: string;
  /** Resolved display name of the linked contact; empty if none. */
  contact: string;
  /** Einnahmen (credit). */
  earnings: number;
  /** Ausgaben (debit). */
  spendings: number;
  /** Fremdgeld-Eingang (credit). */
  escrowIn: number;
  /** Fremdgeld-Ausgang (debit). */
  escrowOut: number;
  /** Auslagen-Eingang (credit). */
  expendituresIn: number;
  /** Auslagen-Ausgang (debit). */
  expendituresOut: number;
  /** Net effect on the balance: (earnings+escrowIn+expendituresIn) - (spendings+escrowOut+expendituresOut). */
  total: number;
}

/**
 * A timesheet ("Zeitwerk"/project) of the case (GET /v7/cases/{id}/timesheets — open ones only).
 * The web view fills `positions` with a follow-up call per timesheet.
 */
export interface CaseTimesheet {
  id: string;
  name: string;
  description: string;
  /** Rounding granularity in minutes for its positions. */
  interval: number;
  /** True when a net budget cap applies. */
  limited: boolean;
  /** Net budget cap (only meaningful when limited). */
  limit: number;
  /** Percentage of the budget consumed (server-computed). */
  percentageDone: number;
  /** 10 = open ("offen"), 20 = closed ("geschlossen"). */
  status: number;
  /** The owning case (present on GET; used by the cross-case running view). */
  caseId?: string;
  caseFileNumber?: string;
  caseName?: string;
}

/** A single time entry within a timesheet (GET /v8/timesheets/{id}/positions). */
export interface TimesheetPosition {
  id: string;
  name: string;
  description: string;
  /** User/principal who logged the entry. */
  principal: string;
  /** ISO timestamp (sanitized); empty if unset. */
  started: string;
  /** ISO timestamp (sanitized); empty while running. */
  stopped: string;
  /** Hourly rate (net). */
  unitPrice: number;
  taxRate: number;
  /** Net line total (server-computed with the timesheet's rounding interval). */
  total: number;
  timesheetId: string;
  /** Set once billed; empty/absent while unbilled. */
  invoiceId: string;
  /** True when started but not stopped (a live timer). */
  running: boolean;
}

/**
 * Writable case master data (RestfulCaseV2 shape, all optional). The editor clones the raw DTO
 * loaded from GET /v2/cases/{id} so fields not shown in the form (custom1-3, group, externalId)
 * round-trip unchanged on update. `archived` is the server's short flag (0/1).
 */
export interface CaseWrite {
  id?: string;
  fileNumber?: string;
  name?: string;
  reason?: string;
  subjectField?: string;
  lawyer?: string;
  assistant?: string;
  claimNumber?: string;
  claimValue?: number;
  notice?: string;
  archived?: number;
  custom1?: string;
  custom2?: string;
  custom3?: string;
  group?: string;
  externalId?: string;
}

/** A configured party/involvement type (GET /v1/cases/party/types) for the add-party dropdown. */
export interface PartyTypeOption {
  name: string;
  /** CSS hex colour ('' when none). */
  color: string;
  placeHolder: boolean;
}

/** A contact search hit for the add-party picker (GET /v8/contacts/page). */
export interface ContactRef {
  id: string;
  label: string;
}

/** A login-enabled user for the Anwalt/Sachbearbeiter dropdowns (GET /v6/security/users). */
export interface CaseUserRef {
  principalId: string;
  displayName: string;
}

/** Payload to link a contact to a case as a party (PUT /v1/cases/party/create). */
export interface PartyWrite {
  caseId: string;
  addressId: string;
  involvementType: string;
}

/** Editable party fields sent to PUT /v1/cases/party/update (mirrors RestfulPartyV1). */
export interface PartyUpdate {
  id: string;
  caseId: string;
  addressId: string;
  involvementType: string;
  reference: string;
  contact: string;
  custom1: string;
  custom2: string;
  custom3: string;
}

/** Writable timesheet (project) fields sent to the v8 create/update endpoints. */
export interface TimesheetWrite {
  id?: string;
  name: string;
  description: string;
  /** Rounding interval in minutes. */
  interval: number;
  limited: boolean;
  /** Net budget cap (only meaningful when limited). */
  limit: number;
  /** 10 = open, 20 = closed. */
  status: number;
}

/**
 * Writable time-entry (position) fields. `started`/`stopped` are server-format datetime strings
 * (yyyy-MM-ddTHH:mm:ss±HHMM); omitted for the stopwatch start/stop calls where the server timestamps.
 */
export interface PositionWrite {
  name: string;
  description: string;
  started?: string;
  stopped?: string;
  unitPrice: number;
  taxRate: number;
  /** Login name of the user the time is logged for (defaults server-side to the caller). */
  principal?: string;
}

/** A running time entry enriched with its timesheet/case, for the cross-case running view. */
export interface RunningPosition {
  position: TimesheetPosition;
  timesheet: CaseTimesheet;
}

/** A predefined position template (GET /v8/timesheets/{id}/templates) for pre-filling entries. */
export interface PositionTemplate {
  id: string;
  name: string;
  description: string;
  unitPrice: number;
  taxRate: number;
}

/** Full case (RestfulCaseV1 + related collections + derived status). */
export interface CaseDetail {
  id: string;
  fileNumber: string;
  name: string;
  reason: string;
  subjectField: string;
  lawyer: string;
  assistant: string;
  claimNumber: string;
  claimValue: number;
  notice: string;
  archived: boolean;
  status: CaseStatus;
  parties: Party[];
  dueDates: DueDate[];
  documents: CaseDocument[];
  /** Root of the case document folder tree; null when the case has no folder structure. */
  rootFolder: DocFolder | null;
}
