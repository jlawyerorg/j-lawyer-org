/**
 * Case domain models. Field names mirror the REST DTOs (RestfulCaseV1 / RestfulPartyV1 /
 * RestfulDueDateV1 in j-lawyer-io) so swapping the mock CasesService for real REST calls
 * (GET /j-lawyer-io/rest/v7/cases/…) is a localized change. See gap-analysis.md.
 */

/** Display status derived for the list (mock); real data derives it from `archived` + due dates. */
export type CaseStatus = 'open' | 'dueToday' | 'waiting' | 'closed';

/** List row (aligns with RestfulCaseOverviewV1 + a few display fields). */
export interface CaseOverview {
  id: string;
  /** Aktenzeichen */
  fileNumber: string;
  name: string;
  /** Rechtsgebiet */
  subjectField: string;
  lawyer: string;
  status: CaseStatus;
  /** ISO date of last change (display). */
  lastChanged: string;
}

/** Involved party (RestfulPartyV1). `involvementType` is an i18n key suffix (akten.roles.*). */
export interface Party {
  id: string;
  involvementType: 'client' | 'opponent' | 'court' | 'insurer';
  contact: string;
}

/** Deadline / follow-up (RestfulDueDateV1). */
export interface DueDate {
  id: string;
  reason: string;
  /** ISO date. */
  dueDate: string;
  done: boolean;
  assignee: string;
  type: 'deadline' | 'followup';
}

/** Document shown in the case (subset; real data comes from GET …/{id}/documents). */
export interface CaseDocument {
  id: string;
  name: string;
  /** ISO date. */
  date: string;
  author: string;
  size: string;
  ext: string;
}

/** Full case (RestfulCaseV1 + related collections). */
export interface CaseDetail extends CaseOverview {
  claimNumber: string;
  claimValue: number;
  assistant: string;
  reason: string;
  notice: string;
  /** ISO date the case was created. */
  created: string;
  parties: Party[];
  dueDates: DueDate[];
  documents: CaseDocument[];
}
