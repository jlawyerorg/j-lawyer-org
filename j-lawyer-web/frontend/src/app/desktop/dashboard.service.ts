import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { API_ROOT } from '../core/api';
import { DueItem, InvoiceSummary, OpenInvoice, RecentCase } from './desktop.models';

const CASES_V7 = `${API_ROOT}/v7/cases`;
const CASES_V8 = `${API_ROOT}/v8/cases`;
const CALENDAR_V8 = `${API_ROOT}/v8/calendar`;

/** Wire shapes (only the fields the dashboard consumes). */
interface CasePageDto { items: { id: string; fileNumber: string; name: string; subjectField: string; lawyer: string; dateChanged: string }[]; }
interface EventDto { id: string; type: string; summary: string; begin: number; done: boolean; assignee: string; caseId: string; caseFileNumber: string; caseName: string; }
interface InvoiceDto { id: string; invoiceNumber: string; status: string; totalGross: number; currency: string; caseId: string; dueDate: string; }

/**
 * Read-only data access for the "Mein Desktop" dashboard. Reuses existing endpoints — no
 * dedicated backend: recently changed cases (v8 cases page), due deadlines/follow-ups
 * (v8 calendar events, open only) and open invoices (v7). ACL is enforced server-side.
 */
@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);

  /** The {@link limit} most recently changed cases (the page endpoint sorts by dateChanged desc). */
  recentCases(limit = 8): Observable<RecentCase[]> {
    const params = new HttpParams().set('offset', '0').set('limit', String(limit)).set('filter', 'all');
    return this.http.get<CasePageDto>(`${CASES_V8}/page`, { params }).pipe(
      map((page) => (page.items ?? []).map((c) => ({
        id: c.id,
        fileNumber: c.fileNumber,
        name: c.name,
        subjectField: c.subjectField ?? '',
        lawyer: c.lawyer ?? '',
        lastChanged: isoDate(c.dateChanged),
      } satisfies RecentCase))),
    );
  }

  /**
   * Open deadlines/follow-ups/appointments in a window around today, soonest first. Overdue
   * items (due before now) are flagged so the UI can surface them.
   */
  dueItems(fromDaysBack = 14, toDaysAhead = 30, limit = 10): Observable<DueItem[]> {
    const now = new Date();
    const from = new Date(now.getTime() - fromDaysBack * 86_400_000);
    const to = new Date(now.getTime() + toDaysAhead * 86_400_000);
    const params = new HttpParams()
      .set('status', 'open')
      .set('type', 'all')
      .set('from', ymd(from))
      .set('to', ymd(to))
      .set('limit', '100');
    return this.http.get<EventDto[]>(`${CALENDAR_V8}/events`, { params }).pipe(
      map((rows) => (rows ?? [])
        .filter((e) => !e.done)
        .map((e) => {
          const due = new Date(e.begin);
          return {
            id: e.id,
            type: toType(e.type),
            summary: e.summary ?? '',
            due,
            overdue: due.getTime() < now.getTime(),
            assignee: e.assignee ?? '',
            caseId: e.caseId ?? '',
            caseFileNumber: e.caseFileNumber ?? '',
            caseName: e.caseName ?? '',
          } satisfies DueItem;
        })
        .sort((a, b) => a.due.getTime() - b.due.getTime())
        .slice(0, limit)),
    );
  }

  /** Open/draft invoices across all non-archived cases, aggregated into a KPI summary. */
  openInvoices(topN = 6): Observable<InvoiceSummary> {
    return this.http.get<InvoiceDto[]>(`${CASES_V7}/invoices`).pipe(
      map((rows) => {
        const invoices: OpenInvoice[] = (rows ?? []).map((i) => ({
          id: i.id,
          invoiceNumber: i.invoiceNumber ?? '',
          status: i.status ?? '',
          totalGross: i.totalGross ?? 0,
          currency: i.currency ?? '€',
          caseId: i.caseId ?? '',
          dueDate: isoDate(i.dueDate),
        }));
        const totalGross = invoices.reduce((acc, i) => acc + i.totalGross, 0);
        return {
          count: invoices.length,
          totalGross,
          currency: invoices[0]?.currency ?? '€',
          top: invoices.slice(0, topN),
        } satisfies InvoiceSummary;
      }),
    );
  }
}

function toType(t: string): DueItem['type'] {
  return t === 'respite' || t === 'event' ? t : 'followup';
}

/** Strips the Java ZonedDateTime zone suffix ("...Z[UTC]" -> parseable ISO). */
function isoDate(value: string | null | undefined): string {
  return value ? value.replace(/\[.*\]$/, '') : '';
}

/** Local yyyy-MM-dd (server expects a date in its own timezone). */
function ymd(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}
