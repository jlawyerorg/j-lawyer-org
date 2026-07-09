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
}

/** Document shown in the case. */
export interface CaseDocument {
  id: string;
  name: string;
  /** ISO date (sanitized). */
  date: string;
  /** Human-readable size (e.g. "34 KB"). */
  size: string;
  /** Upper-case file extension derived from the name (e.g. "PDF"). */
  ext: string;
}

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
}
