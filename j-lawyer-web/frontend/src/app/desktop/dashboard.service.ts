import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { forkJoin, map, Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { API_ROOT } from '../core/api';
import {
  DashboardConfig, DashStats, DueItem, InvoiceSummary, MessageItem,
  normalizeConfig, OpenInvoice, RecentCase, TaggedItem, UserOption,
} from './desktop.models';

const CASES_V7 = `${API_ROOT}/v7/cases`;
const CASES_V8 = `${API_ROOT}/v8/cases`;
const CONTACTS_V8 = `${API_ROOT}/v8/contacts`;
const CALENDAR_V8 = `${API_ROOT}/v8/calendar`;
const DUEDATE_V6 = `${API_ROOT}/v6/cases/duedate`;
const CONFIG_V7 = `${API_ROOT}/v7/configuration`;
const MESSAGES_V7 = `${API_ROOT}/v7/messages`;
const PROFILE_V8 = `${API_ROOT}/v8/profile`;
const SECURITY_V6 = `${API_ROOT}/v6/security`;

/** Wire shapes (only the fields the dashboard consumes). */
interface CaseOverviewDto { id: string; fileNumber: string; name: string; subjectField: string; lawyer: string; assistant: string; dateChanged: string; }
interface CasePageDto { total: number; items: CaseOverviewDto[]; }
interface ContactPageDto { total: number; }
interface EventDto {
  id: string; type: string; summary: string; begin: number; end: number | null; done: boolean; assignee: string;
  caseId: string; caseFileNumber: string; caseName: string;
  description?: string; location?: string; reminderMinutes?: number; calendar?: string;
}
interface InvoiceDto { id: string; invoiceNumber: string; status: string; totalGross: number; currency: string; caseId: string; dueDate: string; }
interface CaseByTagDto { id: string; fileNumber: string; name: string; lawyer: string; assistant: string; tags: string[]; }
interface OptionDto { id: string; optionGroup: string; value: string; }
interface DocByTagDto { id: string; caseId: string; name: string; tags: string[]; }
interface MentionDto { id: string; principal: string; done: boolean; }
interface MessageDto { id: string; sent: number | string; sender: string; content: string; caseContext: string; caseFileNumber: string; caseName: string; mentions: MentionDto[]; }
interface UserDto { principalId: string; displayName: string; }
interface DashboardConfigDto { config: string; }

const MESSAGE_WINDOW_DAYS = 30;

/**
 * Read-only data access for the "Mein Desktop" dashboard. Reuses existing endpoints; the only
 * dedicated backend is the per-user config blob (GET/PUT /v8/profile/dashboard). ACL is enforced
 * server-side.
 */
@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);

  // --- per-user dashboard config (opaque JSON blob, server-persisted) ---

  getConfig(): Observable<DashboardConfig> {
    return this.http.get<DashboardConfigDto>(`${PROFILE_V8}/dashboard`).pipe(
      map((dto) => normalizeConfig(parseConfig(dto?.config))),
    );
  }

  saveConfig(cfg: DashboardConfig): Observable<unknown> {
    return this.http.put(`${PROFILE_V8}/dashboard`, { config: JSON.stringify(cfg) });
  }

  /** All users, for the widget user filters (principalId + display name). */
  listUsers(): Observable<UserOption[]> {
    return this.http.get<UserDto[]>(`${SECURITY_V6}/users/list`).pipe(
      map((rows) => (rows ?? []).map((u) => ({ id: u.principalId, name: u.displayName || u.principalId } satisfies UserOption))
        .sort((a, b) => a.name.localeCompare(b.name))),
      catchError(() => of([])),
    );
  }

  // --- widgets ---

  /**
   * The most recently changed cases. When {@link users} is non-empty, a larger page is fetched and
   * filtered to cases where a selected user is the responsible lawyer or assistant.
   */
  recentCases(limit: number, users: string[]): Observable<RecentCase[]> {
    const set = new Set(users);
    const fetch = set.size ? Math.max(60, limit * 8) : limit;
    const params = new HttpParams().set('offset', '0').set('limit', String(fetch)).set('filter', 'all');
    return this.http.get<CasePageDto>(`${CASES_V8}/page`, { params }).pipe(
      map((page) => {
        let items = (page.items ?? []);
        if (set.size) { items = items.filter((c) => set.has(c.lawyer) || set.has(c.assistant)); }
        return items.slice(0, limit).map((c) => ({
          id: c.id, fileNumber: c.fileNumber, name: c.name, subjectField: c.subjectField ?? '',
          lawyer: c.lawyer ?? '', assistant: c.assistant ?? '', lastChanged: isoDate(c.dateChanged),
        } satisfies RecentCase));
      }),
    );
  }

  /**
   * All open deadlines/follow-ups/appointments in the window ({@link sinceDays} into the past +
   * {@link inDays} into the future), unfiltered by type/user — the widget filters those client-side
   * so the tabs switch instantly. Sorted soonest-first; overdue flagged.
   */
  dueWindow(sinceDays: number, inDays: number): Observable<DueItem[]> {
    const now = new Date();
    const from = new Date(now.getTime() - sinceDays * 86_400_000);
    const to = new Date(now.getTime() + inDays * 86_400_000);
    const params = new HttpParams()
      .set('status', 'open').set('type', 'all')
      .set('from', ymd(from)).set('to', ymd(to)).set('limit', '300');
    return this.http.get<EventDto[]>(`${CALENDAR_V8}/events`, { params }).pipe(
      map((rows) => (rows ?? [])
        .filter((e) => !e.done)
        .map((e) => {
          const due = new Date(e.begin);
          return {
            id: e.id, type: toType(e.type), summary: e.summary ?? '', due,
            end: e.end != null ? new Date(e.end) : null,
            overdue: due.getTime() < now.getTime(), done: !!e.done, assignee: e.assignee ?? '',
            caseId: e.caseId ?? '', caseFileNumber: e.caseFileNumber ?? '', caseName: e.caseName ?? '',
            description: e.description ?? '', location: e.location ?? '',
            reminderMinutes: e.reminderMinutes ?? -1, calendarId: e.calendar ?? '',
          } satisfies DueItem;
        })
        .sort((a, b) => a.due.getTime() - b.due.getTime())),
    );
  }

  /** Marks a due entry done/undone via the v6 duedate update (no dedicated endpoint), round-tripping all fields. */
  setDueDone(item: DueItem, done: boolean): Observable<unknown> {
    return this.http.put(`${DUEDATE_V6}/update`, dueUpdatePayload(item, { done }));
  }

  /** Moves a due entry to a new begin/end (reschedule via the date picker). */
  rescheduleDue(item: DueItem, beginMs: number, endMs: number): Observable<unknown> {
    return this.http.put(`${DUEDATE_V6}/update`, dueUpdatePayload(item, { beginMs, endMs }));
  }

  openInvoices(topN = 6): Observable<InvoiceSummary> {
    return this.http.get<InvoiceDto[]>(`${CASES_V7}/invoices`).pipe(
      map((rows) => {
        const invoices: OpenInvoice[] = (rows ?? []).map((i) => ({
          id: i.id, invoiceNumber: i.invoiceNumber ?? '', status: i.status ?? '',
          totalGross: i.totalGross ?? 0, currency: i.currency ?? '€', caseId: i.caseId ?? '', dueDate: isoDate(i.dueDate),
        }));
        const totalGross = invoices.reduce((acc, i) => acc + i.totalGross, 0);
        return { count: invoices.length, totalGross, currency: invoices[0]?.currency ?? '€', top: invoices.slice(0, topN) } satisfies InvoiceSummary;
      }),
    );
  }

  /** The defined case-tag names (Wertevorrat archiveFile.tags). */
  caseTags(): Observable<string[]> { return this.optionValues('archiveFile.tags'); }

  /** The defined document-tag names (Wertevorrat document.tags). */
  docTags(): Observable<string[]> { return this.optionValues('document.tags'); }

  private optionValues(group: string): Observable<string[]> {
    return this.http.get<OptionDto[]>(`${CONFIG_V7}/optiongroups/${group}`).pipe(
      map((opts) => (opts ?? []).map((o) => o.value).filter((v): v is string => !!v)),
      catchError(() => of([])),
    );
  }

  /** Cases carrying a tag (v8 overview with lawyer/assistant + all tags). */
  casesByTag(tag: string): Observable<TaggedItem[]> {
    return this.http.get<CaseByTagDto[]>(`${CASES_V8}/bytag/${encodeURIComponent(tag)}`).pipe(
      map((rows) => (rows ?? []).map((c) => ({
        kind: 'case' as const, id: c.id, caseId: c.id, primary: c.fileNumber ?? '', secondary: c.name ?? '',
        tags: c.tags ?? [], lawyer: c.lawyer ?? '', assistant: c.assistant ?? '',
      } satisfies TaggedItem))),
      catchError(() => of([])),
    );
  }

  /** Documents carrying a tag (v8, with case id + all tags; navigate to the case w/ the doc preselected). */
  documentsByTag(tag: string): Observable<TaggedItem[]> {
    return this.http.get<DocByTagDto[]>(`${CASES_V8}/documents/bytag/${encodeURIComponent(tag)}`).pipe(
      map((rows) => (rows ?? []).map((d) => ({
        kind: 'doc' as const, id: d.id, caseId: d.caseId ?? '', primary: d.name ?? '', secondary: '', tags: d.tags ?? [],
      } satisfies TaggedItem))),
      catchError(() => of([])),
    );
  }

  /** Open messenger mentions addressed to the current user within the last 30 days, newest first. */
  messagesToMe(username: string, limit: number): Observable<MessageItem[]> {
    const seconds = MESSAGE_WINDOW_DAYS * 86_400;
    return this.http.get<MessageDto[]>(`${MESSAGES_V7}/since/${seconds}`).pipe(
      map((rows) => (rows ?? [])
        .map((m) => {
          const mine = (m.mentions ?? []).find((x) => x.principal === username && !x.done);
          return mine ? { message: m, mentionId: mine.id } : null;
        })
        .filter((x): x is { message: MessageDto; mentionId: string } => x !== null)
        .map(({ message, mentionId }) => ({
          id: message.id, mentionId, sender: message.sender ?? '', content: message.content ?? '',
          sent: new Date(typeof message.sent === 'number' ? message.sent : Date.parse(isoDate(message.sent))),
          caseId: message.caseContext ?? '', caseFileNumber: message.caseFileNumber ?? '', caseName: message.caseName ?? '',
        } satisfies MessageItem))
        .sort((a, b) => b.sent.getTime() - a.sent.getTime())
        .slice(0, limit)),
    );
  }

  markMentionDone(mentionId: string): Observable<unknown> {
    return this.http.put(`${MESSAGES_V7}/mentions/${encodeURIComponent(mentionId)}/done`, {});
  }

  /** Case (total/open/archived) + contact counts via the page endpoints' totals. */
  stats(): Observable<DashStats> {
    const total = (filter: string) => this.http
      .get<CasePageDto>(`${CASES_V8}/page`, { params: new HttpParams().set('offset', '0').set('limit', '1').set('filter', filter) })
      .pipe(map((p) => p.total ?? 0), catchError(() => of(0)));
    const contacts = this.http
      .get<ContactPageDto>(`${CONTACTS_V8}/page`, { params: new HttpParams().set('offset', '0').set('limit', '1') })
      .pipe(map((p) => p.total ?? 0), catchError(() => of(0)));
    // the page endpoint's archived token is "closed" (all | open | closed)
    return forkJoin({ all: total('all'), open: total('open'), archived: total('closed'), contacts }).pipe(
      map((r) => ({ casesTotal: r.all, casesOpen: r.open, casesArchived: r.archived, contacts: r.contacts } satisfies DashStats)),
    );
  }

  /** New (received) Dropscan mailings count; null when Dropscan is unavailable. */
  newScansCount(): Observable<number | null> {
    return this.http.get<unknown[]>(`${API_ROOT}/v8/dropscan/mailings`, { params: new HttpParams().set('status', 'received') }).pipe(
      map((rows) => (rows ?? []).length),
      catchError(() => of(null)),
    );
  }
}

function toType(t: string): DueItem['type'] {
  return t === 'respite' || t === 'event' ? t : 'followup';
}

/** Builds the RestfulDueDateV6 update body from a due item, applying done/reschedule changes. */
function dueUpdatePayload(item: DueItem, changes: { done?: boolean; beginMs?: number; endMs?: number }): Record<string, unknown> {
  const begin = changes.beginMs ?? item.due.getTime();
  const end = changes.endMs ?? (item.end ? item.end.getTime() : begin);
  return {
    id: item.id,
    caseId: item.caseId,
    calendar: item.calendarId,
    type: item.type.toUpperCase(),
    summary: item.summary,
    description: item.description,
    location: item.location,
    assignee: item.assignee,
    beginDate: new Date(begin).toISOString(),
    endDate: new Date(end).toISOString(),
    reminderMinutes: item.reminderMinutes,
    done: changes.done ?? item.done,
  };
}

function parseConfig(raw: string | null | undefined): Partial<DashboardConfig> | null {
  if (!raw) { return null; }
  try { return JSON.parse(raw) as Partial<DashboardConfig>; } catch { return null; }
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
