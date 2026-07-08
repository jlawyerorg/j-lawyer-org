/**
 * "Mein Desktop" (dashboard) view models. The dashboard is a read-only overview assembled
 * from existing REST endpoints (no dedicated backend): recently changed cases, open
 * deadlines/follow-ups and open invoices — mirroring the Swing client's DesktopPanel widgets.
 */

/** A recently changed case (from GET /v8/cases/page, sorted by dateChanged desc). */
export interface RecentCase {
  id: string;
  fileNumber: string;
  name: string;
  subjectField: string;
  lawyer: string;
  /** ISO date (sanitized). */
  lastChanged: string;
}

/** An open deadline / follow-up / appointment that is due (from GET /v8/calendar/events). */
export interface DueItem {
  id: string;
  type: 'event' | 'respite' | 'followup';
  summary: string;
  /** Due moment as a JS Date (parsed from epoch millis). */
  due: Date;
  /** True when the due date is in the past (overdue). */
  overdue: boolean;
  assignee: string;
  caseId: string;
  caseFileNumber: string;
  caseName: string;
}

/** An open invoice (from GET /v7/cases/invoices). */
export interface OpenInvoice {
  id: string;
  invoiceNumber: string;
  status: string;
  totalGross: number;
  currency: string;
  caseId: string;
  /** ISO date (sanitized); empty if unset. */
  dueDate: string;
}

/** Aggregated open-invoice figures for the KPI row. */
export interface InvoiceSummary {
  count: number;
  totalGross: number;
  /** Currency of the first invoice (the practice's default); "€" fallback. */
  currency: string;
  /** The most-relevant few, for the preview list. */
  top: OpenInvoice[];
}
