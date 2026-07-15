import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { catchError, defer, finalize, forkJoin, map, Observable, of, shareReplay, switchMap } from 'rxjs';
import { API_ROOT } from '../core/api';
import {
  AccountEntry, AccountEntryWrite, CaseDetail, CaseDocument, CaseGroup, CaseHistoryEntry, CaseInvoice, CaseMessage, CaseOverview,
  CasePayment, CaseStatus, CaseTag, CaseTimesheet, CaseUserRef, CaseWrite, ContactRef, DocDateMode, DocFolder,
  DocSortKey, DueDate, InvoicePool, InvoicePositionItem, InvoicePositionWrite, InvoiceType, InvoiceWrite,
  MultiValueTagDef, Party, PartyTypeOption, PartyUpdate, PartyWrite, PositionTemplate,
  PositionWrite, RunningPosition, SortDir, TimesheetPosition, TimesheetWrite,
} from './case.models';

const CASES_BASE = `${API_ROOT}/v1/cases`;
const CASES_V2 = `${API_ROOT}/v2/cases`;
const CASES_V3 = `${API_ROOT}/v3/cases`;
const CASES_V4 = `${API_ROOT}/v4/cases`;
const CASES_V5 = `${API_ROOT}/v5/cases`;
const CASES_V7 = `${API_ROOT}/v7/cases`;
const INVOICES_V7 = `${API_ROOT}/v7/invoices`;
const CASES_V8 = `${API_ROOT}/v8/cases`;
const TIMESHEETS_V8 = `${API_ROOT}/v8/timesheets`;
const CONTACTS_V8 = `${API_ROOT}/v8/contacts`;
const USERS_V6 = `${API_ROOT}/v6/security/users`;
const GROUPS_V6 = `${API_ROOT}/v6/security/groups`;
const MESSAGES_V7 = `${API_ROOT}/v7/messages`;
const TAG_TEMPLATES_V7 = `${API_ROOT}/v7/configuration/optiongroups/archiveFile.tags`;
const CALENDARS_V4 = `${API_ROOT}/v4/calendars/list`;
const PARTY_TYPES = `${API_ROOT}/v1/cases/party/types`;
const PAGE_SIZE = 50;

export type CaseFilter = 'all' | 'open' | 'closed';

/** Wire shapes returned by j-lawyer-io (only the fields we consume). */
interface CaseListDto {
  id: string; fileNumber: string; name: string; reason: string; dateChanged: string;
  subjectField: string; lawyer: string; archived: boolean;
}
interface CasePageDto { total: number; offset: number; limit: number; items: CaseListDto[]; }
interface CaseDto {
  id: string; fileNumber: string; name: string; reason: string; subjectField: string;
  lawyer: string; assistant: string; claimNumber: string; claimValue: number; notice: string; archived: number;
  // Extra writable fields kept so the editor can round-trip them unchanged (not shown in the form).
  custom1?: string; custom2?: string; custom3?: string; group?: string; externalId?: string;
}
interface PartyDto {
  id: string; involvementType: string; contact?: string; contactName?: string; reference?: string;
  custom1?: string; custom2?: string; custom3?: string; addressId?: string;
}
// GET /v8/contacts/page item (only the fields the party picker needs).
interface ContactHitDto {
  id: string; firstName?: string; name?: string; company?: string; city?: string; zipCode?: string;
}
interface ContactPageHitDto { items?: ContactHitDto[]; }
// GET /v6/security/users
interface UserDto { principalId: string; displayName?: string; }
// v1 and v4 differ: v1 has `reason`/`dueDate`; v4 renames them to `summary`/`beginDate` and adds
// `calendar` (the calendar-setup id). We read whichever the server provides.
interface DueDateDto {
  id: string; done: boolean; assignee: string; type: string;
  reason?: string; dueDate?: string; summary?: string; beginDate?: string; calendar?: string;
  description?: string; location?: string; endDate?: string; reminderMinutes?: number;
}
// GET /v4/calendars — configured calendars; `background` is a packed-RGB int colour.
interface CalendarDto { id: string; displayName?: string; background?: number; }
// GET /v1/cases/party/types — configured party types; `color` is a packed-RGB int colour.
interface PartyTypeDto { id: string; name?: string; color?: number; }
interface DocDto {
  id: string; name: string; size: number; creationDate: string; changeDate?: string;
  favorite?: boolean; folderId?: string; version?: number; highlight1?: number; highlight2?: number;
  tags?: { name?: string }[];
}
// GET /v3/cases/{id}/folders — the nested root folder node (children hold sub-folders).
interface FolderDto { id: string; name: string; parentId?: string; children?: FolderDto[]; }
interface HistoryDto { id: string; principal: string; changeDate: number; changeDescription: string; }
interface InvoiceDto {
  id: string; invoiceNumber: string; name: string; status: string;
  total: number; totalGross: number; currency: string; dueDate: string; creationDate: string;
  description?: string; invoiceType?: string; invoiceTypeId?: string; lastPoolId?: string;
  contactId?: string; sender?: string; paymentType?: string; smallBusiness?: boolean;
  periodFrom?: string; periodTo?: string;
}
interface InvoicePositionDto {
  id: string; name?: string; description?: string; position?: number;
  taxRate?: number; units?: number; unitPrice?: number; total?: number; invoiceId?: string;
}
interface InvoiceTypeDto { id: string; displayName?: string; description?: string; turnOver?: boolean; }
interface InvoicePoolDto { id: string; displayName?: string; smallBusiness?: boolean; paymentTerm?: number; }
interface PaymentDto {
  id: string; paymentNumber: string; name: string; reason: string; status: string;
  total: number; currency: string; targetDate: string; creationDate: string;
}
interface AccountEntryDto {
  id: string; entryDate: string; description: string; contactName?: string; contactId?: string; invoiceId?: string;
  earnings: number; spendings: number; escrowIn: number; escrowOut: number;
  expendituresIn: number; expendituresOut: number;
}
interface TimesheetDto {
  id: string; name: string; description: string; interval: number;
  limited: boolean; limit: number; percentageDone: number; status: number;
  caseId?: string; caseFileNumber?: string; caseName?: string;
}
interface TimesheetPositionDto {
  id: string; name: string; description: string; principal: string; started: string; stopped: string;
  unitPrice: number; taxRate: number; total: number; timesheetId: string; invoiceId?: string; running: boolean;
}
interface PositionTemplateDto { id: string; name?: string; description?: string; unitPrice?: number; taxRate?: number; }
interface TagDto { id: string; name: string; tagValue?: string; }
interface MultiValueTagDto { tagName?: string; values?: string[]; }
interface GroupDto { id: string; name: string; abbreviation?: string; }
interface MessageDto { id: string; sent: number | string; sender: string; content: string; }

/**
 * Case data access against the real REST API (GET /rest/v1/cases/…). The Bearer token is
 * attached by authInterceptor. The list is loaded once into a signal; the detail (case +
 * parties + due dates + documents) is fetched lazily per selection.
 */
@Injectable({ providedIn: 'root' })
export class CasesService {
  private readonly http = inject(HttpClient);

  /** Accumulated rows across the pages loaded so far (server-side paginated). */
  readonly overviews = signal<CaseOverview[]>([]);
  readonly total = signal(0);
  readonly listLoading = signal(false);
  readonly listError = signal(false);
  /** The raw (writable) DTO of the case loaded last, so the editor can clone it for edits. */
  readonly rawSelected = signal<CaseWrite | null>(null);
  /** True while a create/update/delete/upload write is in flight. */
  readonly saving = signal(false);

  // Documents-tab view state, kept here so it survives case navigation for the session.
  readonly docSort = signal<{ key: DocSortKey; dir: SortDir }>({ key: 'name', dir: 'asc' });
  readonly docDateMode = signal<DocDateMode>('change');

  private currentFilter: CaseFilter = 'all';
  private currentSearch = '';
  private requestSeq = 0;

  get filter(): CaseFilter {
    return this.currentFilter;
  }

  /** True while more pages are available for the current filter/search. */
  get hasMore(): boolean {
    return this.overviews().length < this.total();
  }

  /** (Re)loads the first page with the given filter (server-side). */
  setFilter(filter: CaseFilter): void {
    this.currentFilter = filter;
    this.reload();
  }

  /** (Re)loads the first page with the given search term (server-side). */
  setSearch(search: string): void {
    this.currentSearch = search;
    this.reload();
  }

  /** Loads the first page for the current filter/search (also used for retry). */
  reload(): void {
    this.fetchPage(0, true);
  }

  /** Appends the next page (infinite scroll). No-op while loading or when exhausted. */
  loadMore(): void {
    if (this.listLoading() || !this.hasMore) {
      return;
    }
    this.fetchPage(this.overviews().length, false);
  }

  private fetchPage(offset: number, reset: boolean): void {
    this.listLoading.set(true);
    this.listError.set(false);
    const seq = ++this.requestSeq;

    let params = new HttpParams()
      .set('offset', String(offset))
      .set('limit', String(PAGE_SIZE))
      .set('filter', this.currentFilter);
    if (this.currentSearch.trim()) {
      params = params.set('q', this.currentSearch.trim());
    }

    this.http.get<CasePageDto>(`${CASES_V8}/page`, { params }).subscribe({
      next: (page) => {
        if (seq !== this.requestSeq) {
          return; // a newer request superseded this one
        }
        const rows = (page.items ?? []).map(toOverview);
        this.overviews.update((cur) => (reset ? rows : [...cur, ...rows]));
        this.total.set(page.total ?? rows.length);
        this.listLoading.set(false);
      },
      error: () => {
        if (seq !== this.requestSeq) {
          return;
        }
        this.listError.set(true);
        this.listLoading.set(false);
      },
    });
  }

  /** Loads a case's change history, most recent first (GET /v8/cases/{id}/history); [] on error. */
  history(id: string): Observable<CaseHistoryEntry[]> {
    return this.http.get<HistoryDto[]>(`${CASES_V8}/${id}/history`).pipe(
      map((rows) => (rows ?? []).map(toHistory)),
    );
  }

  /** Loads the case's invoices (GET /v7/cases/{id}/invoices); [] on error. */
  invoices(id: string): Observable<CaseInvoice[]> {
    return this.http.get<InvoiceDto[]>(`${CASES_V7}/${id}/invoices`).pipe(
      map((rows) => (rows ?? []).map(toInvoice)),
      catchError(() => of([])),
    );
  }

  private invoiceTypes$?: Observable<InvoiceType[]>;
  private invoicePools$?: Observable<InvoicePool[]>;

  /** The configured invoice types ("Rechnungsarten", GET /v7/invoices/types). Cached. */
  invoiceTypes(): Observable<InvoiceType[]> {
    this.invoiceTypes$ ??= this.http.get<InvoiceTypeDto[]>(`${INVOICES_V7}/types`).pipe(
      map((rows) => (rows ?? []).map((t) => ({
        id: t.id, displayName: t.displayName ?? '', description: t.description ?? '', turnOver: !!t.turnOver,
      }))),
      catchError(() => of([])),
      shareReplay(1),
    );
    return this.invoiceTypes$;
  }

  /** The configured invoice number-range pools ("Nummernkreise", GET /v7/invoices/pools). Cached. */
  invoicePools(): Observable<InvoicePool[]> {
    this.invoicePools$ ??= this.http.get<InvoicePoolDto[]>(`${INVOICES_V7}/pools`).pipe(
      map((rows) => (rows ?? []).map((p) => ({
        id: p.id, displayName: p.displayName ?? '', smallBusiness: !!p.smallBusiness, paymentTerm: p.paymentTerm ?? 14,
      }))),
      catchError(() => of([])),
      shareReplay(1),
    );
    return this.invoicePools$;
  }

  /** Loads an invoice's line items (GET /v7/cases/invoices/{id}/positions), by sort order; [] on error. */
  invoicePositions(invoiceId: string): Observable<InvoicePositionItem[]> {
    return this.http.get<InvoicePositionDto[]>(`${CASES_V7}/invoices/${encodeURIComponent(invoiceId)}/positions`).pipe(
      map((rows) => (rows ?? []).map(toInvoicePosition).sort((a, b) => a.position - b.position)),
      catchError(() => of([])),
    );
  }

  /** Creates an invoice (PUT /v7/cases/invoices/create); returns the created invoice (incl. its id). */
  createInvoice(data: InvoiceWrite): Observable<CaseInvoice> {
    return this.write(this.http.put<InvoiceDto>(`${CASES_V7}/invoices/create`, data)).pipe(map((dto) => toInvoice(dto as InvoiceDto)));
  }

  /** Updates an invoice's Stammdaten (POST /v7/cases/invoices/{id}/update). */
  updateInvoice(invoiceId: string, data: InvoiceWrite): Observable<unknown> {
    return this.write(this.http.post(`${CASES_V7}/invoices/${encodeURIComponent(invoiceId)}/update`, data));
  }

  /** Deletes an invoice (DELETE /v7/cases/invoices/{id}). */
  deleteInvoice(invoiceId: string): Observable<unknown> {
    return this.write(this.http.delete(`${CASES_V7}/invoices/${encodeURIComponent(invoiceId)}`));
  }

  /** Duplicates an invoice into the same case (PUT /v7/cases/invoices/{id}/duplicate). */
  duplicateInvoice(invoiceId: string, caseId: string, poolId: string): Observable<unknown> {
    return this.write(this.http.put(`${CASES_V7}/invoices/${encodeURIComponent(invoiceId)}/duplicate`,
      { toCaseId: caseId, invoicePoolId: poolId, asCredit: false, markAsCopy: true }));
  }

  /** Adds a line item to an invoice (PUT /v7/cases/invoices/{id}/positions/create); recalculates totals. */
  createInvoicePosition(invoiceId: string, pos: InvoicePositionWrite): Observable<unknown> {
    return this.write(this.http.put(`${CASES_V7}/invoices/${encodeURIComponent(invoiceId)}/positions/create`, pos));
  }

  /** Updates a line item (POST /v7/cases/invoices/positions/{positionId}/update); recalculates totals. */
  updateInvoicePosition(positionId: string, pos: InvoicePositionWrite): Observable<unknown> {
    return this.write(this.http.post(`${CASES_V7}/invoices/positions/${encodeURIComponent(positionId)}/update`, pos));
  }

  /** Deletes a line item (DELETE /v7/cases/invoices/positions/{positionId}); recalculates totals. */
  deleteInvoicePosition(positionId: string): Observable<unknown> {
    return this.write(this.http.delete(`${CASES_V7}/invoices/positions/${encodeURIComponent(positionId)}`));
  }

  /** Loads the case's payments (GET /v8/cases/{id}/payments); [] on error. */
  payments(id: string): Observable<CasePayment[]> {
    return this.http.get<PaymentDto[]>(`${CASES_V8}/${id}/payments`).pipe(
      map((rows) => (rows ?? []).map(toPayment)),
      catchError(() => of([])),
    );
  }

  /** Loads the case account entries ("Aktenkonto", GET /v7/cases/{id}/accountentries); [] on error. */
  accountEntries(id: string): Observable<AccountEntry[]> {
    return this.http.get<AccountEntryDto[]>(`${CASES_V7}/${id}/accountentries`).pipe(
      map((rows) => (rows ?? []).map(toAccountEntry)),
      catchError(() => of([])),
    );
  }

  /** Creates a case account entry / "Buchung" (PUT /v7/cases/{id}/accountentries/create). */
  createAccountEntry(caseId: string, data: AccountEntryWrite): Observable<unknown> {
    return this.write(this.http.put(`${CASES_V7}/${encodeURIComponent(caseId)}/accountentries/create`, data));
  }

  /** Updates a case account entry (POST /v7/cases/accountentries/{id}/update). */
  updateAccountEntry(entryId: string, data: AccountEntryWrite): Observable<unknown> {
    return this.write(this.http.post(`${CASES_V7}/accountentries/${encodeURIComponent(entryId)}/update`, data));
  }

  /** Deletes a case account entry (DELETE /v7/cases/accountentries/{id}). */
  deleteAccountEntry(entryId: string): Observable<unknown> {
    return this.write(this.http.delete(`${CASES_V7}/accountentries/${encodeURIComponent(entryId)}`));
  }

  /** Wraps a write call with the shared `saving` flag. */
  private write(call: Observable<unknown>): Observable<unknown> {
    return defer(() => {
      this.saving.set(true);
      return call;
    }).pipe(finalize(() => this.saving.set(false)));
  }

  /**
   * Loads the case's timesheets ("Zeiten") including closed ones (GET /v8/timesheets/cases/{id}).
   * Falls back to the open-only v7 endpoint (GET /v7/cases/{id}/timesheets) when the v8 endpoint
   * is unavailable (e.g. an older server that predates it); [] if both fail.
   */
  timesheets(id: string): Observable<CaseTimesheet[]> {
    return this.http.get<TimesheetDto[]>(`${TIMESHEETS_V8}/cases/${id}`).pipe(
      catchError(() => this.http.get<TimesheetDto[]>(`${CASES_V7}/${id}/timesheets`)),
      map((rows) => (rows ?? []).map(toTimesheet)),
      catchError(() => of([])),
    );
  }

  /** Loads a timesheet's positions/time entries (GET /v8/timesheets/{id}/positions); [] on error. */
  timesheetPositions(timesheetId: string): Observable<TimesheetPosition[]> {
    return this.http.get<TimesheetPositionDto[]>(`${TIMESHEETS_V8}/${timesheetId}/positions`).pipe(
      map((rows) => (rows ?? []).map(toTimesheetPosition)),
      catchError(() => of([])),
    );
  }

  /** Loads a single timesheet by id, incl. its case (GET /v8/timesheets/{id}); null on error. */
  getTimesheet(timesheetId: string): Observable<CaseTimesheet | null> {
    return this.http.get<TimesheetDto>(`${TIMESHEETS_V8}/${encodeURIComponent(timesheetId)}`).pipe(
      map((dto) => (dto ? toTimesheet(dto) : null)),
      catchError(() => of(null)),
    );
  }

  /** Number of the authenticated user's currently running timers (GET .../positions/running/count); 0 on error. */
  runningCount(): Observable<number> {
    return this.http.get<number>(`${TIMESHEETS_V8}/positions/running/count`).pipe(
      map((n) => Number(n) || 0),
      catchError(() => of(0)),
    );
  }

  /**
   * The authenticated user's running timers, each enriched with its timesheet + case
   * (GET .../positions/running, then one getTimesheet per entry). [] on error.
   */
  runningPositions(): Observable<RunningPosition[]> {
    return this.http.get<TimesheetPositionDto[]>(`${TIMESHEETS_V8}/positions/running`).pipe(
      map((rows) => (rows ?? []).map(toTimesheetPosition)),
      switchMap((positions) => positions.length
        ? forkJoin(positions.map((p) => this.getTimesheet(p.timesheetId).pipe(
            map((ts) => ({ position: p, timesheet: ts ?? emptyTimesheet(p.timesheetId) })),
          )))
        : of([] as RunningPosition[])),
      catchError(() => of([] as RunningPosition[])),
    );
  }

  /** One-shot case search for pickers (GET /v8/cases/page?q=); up to `limit` overviews, [] on error. */
  searchCases(term: string, limit = 20): Observable<CaseOverview[]> {
    let params = new HttpParams().set('offset', '0').set('limit', String(limit)).set('filter', 'all');
    if (term.trim()) {
      params = params.set('q', term.trim());
    }
    return this.http.get<CasePageDto>(`${CASES_V8}/page`, { params }).pipe(
      map((page) => (page.items ?? []).map(toOverview)),
      catchError(() => of([])),
    );
  }

  /** Predefined position templates for a timesheet (GET /v8/timesheets/{id}/templates); [] on error. */
  positionTemplates(timesheetId: string): Observable<PositionTemplate[]> {
    return this.http.get<PositionTemplateDto[]>(`${TIMESHEETS_V8}/${timesheetId}/templates`).pipe(
      map((rows) => (rows ?? []).map((t) => ({
        id: t.id, name: t.name ?? '', description: t.description ?? '',
        unitPrice: t.unitPrice ?? 0, taxRate: t.taxRate ?? 0,
      }))),
      catchError(() => of([])),
    );
  }

  private allTemplates$?: Observable<PositionTemplate[]>;

  /** The global pool of position templates (GET /v8/timesheets/templates), incl. hourly rate. Cached. */
  allTimesheetTemplates(): Observable<PositionTemplate[]> {
    this.allTemplates$ ??= this.http.get<PositionTemplateDto[]>(`${TIMESHEETS_V8}/templates`).pipe(
      map((rows) => (rows ?? []).map((t) => ({
        id: t.id, name: t.name ?? '', description: t.description ?? '',
        unitPrice: t.unitPrice ?? 0, taxRate: t.taxRate ?? 0,
      }))),
      catchError(() => of([])),
      shareReplay(1),
    );
    return this.allTemplates$;
  }

  private intervals$?: Observable<number[]>;

  /** Configured timesheet rounding intervals in minutes (GET /v7 option group). Cached; falls back to a default set. */
  timesheetIntervals(): Observable<number[]> {
    this.intervals$ ??= this.http.get<{ value?: string }[]>(`${API_ROOT}/v7/configuration/optiongroups/timesheet.intervalminutes`).pipe(
      map((rows) => {
        const vals = (rows ?? []).map((o) => parseInt((o.value ?? '').trim(), 10)).filter((n) => Number.isFinite(n) && n > 0);
        return vals.length ? Array.from(new Set(vals)).sort((a, b) => a - b) : [1, 5, 10, 15, 30, 60];
      }),
      catchError(() => of([1, 5, 10, 15, 30, 60])),
      shareReplay(1),
    );
    return this.intervals$;
  }

  // ----- Timesheet (project) CRUD (v8) -----

  /** Creates a timesheet/project for a case (POST /v8/timesheets/cases/{caseId}). */
  createTimesheet(caseId: string, data: TimesheetWrite): Observable<unknown> {
    return this.timesheetWrite(this.http.post(`${TIMESHEETS_V8}/cases/${encodeURIComponent(caseId)}`, data));
  }

  /** Updates a timesheet/project (PUT /v8/timesheets/{id}). */
  updateTimesheet(timesheetId: string, data: TimesheetWrite): Observable<unknown> {
    return this.timesheetWrite(this.http.put(`${TIMESHEETS_V8}/${encodeURIComponent(timesheetId)}`, data));
  }

  /** Deletes a timesheet/project including its positions (DELETE /v8/timesheets/{id}). */
  deleteTimesheet(timesheetId: string): Observable<unknown> {
    return this.timesheetWrite(this.http.delete(`${TIMESHEETS_V8}/${encodeURIComponent(timesheetId)}`));
  }

  /**
   * Creates or updates a project, then sets its allowed position templates (project positions /
   * hourly rates) in one flow — the allowed set needs the (possibly just created) timesheet id.
   */
  saveTimesheetWithTemplates(caseId: string, data: TimesheetWrite, templates: PositionTemplate[]): Observable<unknown> {
    const id$ = data.id
      ? this.http.put(`${TIMESHEETS_V8}/${encodeURIComponent(data.id)}`, data).pipe(map(() => data.id as string))
      : this.http.post<{ id: string }>(`${TIMESHEETS_V8}/cases/${encodeURIComponent(caseId)}`, data).pipe(map((r) => r.id));
    return this.timesheetWrite(id$.pipe(
      switchMap((id) => this.http.put(`${TIMESHEETS_V8}/${encodeURIComponent(id)}/templates`, templates).pipe(map(() => id))),
    ));
  }

  /** Sets a project's allowed position templates (PUT /v8/timesheets/{id}/templates). */
  setTimesheetTemplates(timesheetId: string, templates: PositionTemplate[]): Observable<unknown> {
    return this.timesheetWrite(this.http.put(`${TIMESHEETS_V8}/${encodeURIComponent(timesheetId)}/templates`, templates));
  }

  // ----- Position (time entry) CRUD + stopwatch (v8) -----

  /** Adds a manual time entry with explicit start/stop (POST /v8/timesheets/{id}/positions). */
  addPosition(timesheetId: string, pos: PositionWrite): Observable<unknown> {
    return this.timesheetWrite(this.http.post(`${TIMESHEETS_V8}/${encodeURIComponent(timesheetId)}/positions`, pos));
  }

  /** Updates a time entry (PUT /v8/timesheets/{id}/positions/{positionId}). */
  updatePosition(timesheetId: string, positionId: string, pos: PositionWrite): Observable<unknown> {
    return this.timesheetWrite(this.http.put(`${TIMESHEETS_V8}/${encodeURIComponent(timesheetId)}/positions/${encodeURIComponent(positionId)}`, pos));
  }

  /** Deletes a time entry (DELETE /v8/timesheets/{id}/positions/{positionId}). */
  deletePosition(timesheetId: string, positionId: string): Observable<unknown> {
    return this.timesheetWrite(this.http.delete(`${TIMESHEETS_V8}/${encodeURIComponent(timesheetId)}/positions/${encodeURIComponent(positionId)}`));
  }

  /** Starts a running timer (POST /v8/timesheets/{id}/positions/start); the server timestamps the start. */
  startPosition(timesheetId: string, pos: PositionWrite): Observable<unknown> {
    return this.timesheetWrite(this.http.post(`${TIMESHEETS_V8}/${encodeURIComponent(timesheetId)}/positions/start`, pos));
  }

  /** Stops a running timer (PUT /v8/timesheets/{id}/positions/{positionId}/stop); the server timestamps the stop. */
  stopPosition(timesheetId: string, positionId: string, pos: PositionWrite): Observable<unknown> {
    return this.timesheetWrite(this.http.put(`${TIMESHEETS_V8}/${encodeURIComponent(timesheetId)}/positions/${encodeURIComponent(positionId)}/stop`, pos));
  }

  /** Wraps a timesheet write with the shared `saving` flag. */
  private timesheetWrite(call: Observable<unknown>): Observable<unknown> {
    return defer(() => {
      this.saving.set(true);
      return call;
    }).pipe(finalize(() => this.saving.set(false)));
  }

  /** Loads the case's labels ("Etiketten", GET /v1/cases/{id}/tags); [] on error. */
  tags(id: string): Observable<CaseTag[]> {
    return this.http.get<TagDto[]>(`${CASES_BASE}/${id}/tags`).pipe(
      map((rows) => (rows ?? []).map((t) => ({ id: t.id, name: t.name ?? '', value: t.tagValue ?? '' }))),
      catchError(() => of([])),
    );
  }

  private multiValueTags$?: Observable<MultiValueTagDef[]>;

  /** The configured list-tag ("multi-value") definitions for cases (name + value set). Cached. */
  multiValueTags(): Observable<MultiValueTagDef[]> {
    this.multiValueTags$ ??= this.http.get<MultiValueTagDto[]>(`${API_ROOT}/v7/configuration/tags/multivalue/case`).pipe(
      map((rows) => (rows ?? [])
        .filter((t) => t.tagName)
        .map((t) => ({ tagName: t.tagName!, values: (t.values ?? []).filter(Boolean) }))),
      catchError(() => of([])),
      shareReplay(1),
    );
    return this.multiValueTags$;
  }

  /** Loads the user groups allowed to access the case ("Berechtigungen", GET /v7/cases/{id}/groups); [] on error. */
  allowedGroups(id: string): Observable<CaseGroup[]> {
    return this.http.get<GroupDto[]>(`${CASES_V7}/${id}/groups`).pipe(
      map((rows) => (rows ?? []).map(toGroup)),
      catchError(() => of([])),
    );
  }

  private allGroups$?: Observable<CaseGroup[]>;

  /** All configured user groups (GET /v6/security/groups), for the owner/authorized-group pickers. Cached. */
  allGroups(): Observable<CaseGroup[]> {
    this.allGroups$ ??= this.http.get<GroupDto[]>(GROUPS_V6).pipe(
      map((rows) => (rows ?? []).map(toGroup).sort((a, b) => a.name.localeCompare(b.name))),
      catchError(() => of([])),
      shareReplay(1),
    );
    return this.allGroups$;
  }

  /**
   * Overwrites a case's authorized groups with the given full set (PUT /v7/cases/{id}/groups — a
   * bulk overwrite; there is no discrete add/remove endpoint, so the caller sends the desired list).
   */
  setAllowedGroups(caseId: string, groups: CaseGroup[]): Observable<unknown> {
    return defer(() => {
      this.saving.set(true);
      return this.http.put(`${CASES_V7}/${encodeURIComponent(caseId)}/groups`,
        groups.map((g) => ({ id: g.id, name: g.name, abbreviation: g.abbreviation })));
    }).pipe(finalize(() => this.saving.set(false)));
  }

  private tagTemplates$?: Observable<string[]>;

  /** The configured case-tag templates (GET /v7/configuration/optiongroups/archiveFile.tags). Cached. */
  tagTemplates(): Observable<string[]> {
    this.tagTemplates$ ??= this.http.get<{ value?: string }[]>(TAG_TEMPLATES_V7).pipe(
      map((rows) => (rows ?? []).map((o) => (o.value ?? '').trim()).filter(Boolean)),
      catchError(() => of([])),
      shareReplay(1),
    );
    return this.tagTemplates$;
  }

  /**
   * Adds/sets a label/tag on a case (PUT /v5/cases/{id}/tags). For a list (multi-value) tag pass
   * `tagValue`; the server updates the value in place if the tag is already set (setTag by name).
   */
  addTag(caseId: string, name: string, tagValue?: string): Observable<unknown> {
    const body: Record<string, string> = { name };
    if (tagValue != null) {
      body['tagValue'] = tagValue;
    }
    return defer(() => {
      this.saving.set(true);
      return this.http.put(`${CASES_V5}/${encodeURIComponent(caseId)}/tags`, body);
    }).pipe(finalize(() => this.saving.set(false)));
  }

  /** Removes a label/tag from a case (DELETE /v5/cases/tags/{tagId}). */
  removeTag(tagId: string): Observable<unknown> {
    return defer(() => {
      this.saving.set(true);
      return this.http.delete(`${CASES_V5}/tags/${encodeURIComponent(tagId)}`);
    }).pipe(finalize(() => this.saving.set(false)));
  }

  /** Posts a new instant message to a case (PUT /v7/messages/submit); mentions are auto-extracted server-side. */
  sendMessage(caseId: string, sender: string, content: string): Observable<unknown> {
    return defer(() => {
      this.saving.set(true);
      return this.http.put(`${MESSAGES_V7}/submit`, { sender, content, caseContext: caseId });
    }).pipe(finalize(() => this.saving.set(false)));
  }

  /** Loads the case's instant messages (GET /v7/cases/{id}/messages), most recent first; [] on error. */
  messages(id: string): Observable<CaseMessage[]> {
    return this.http.get<MessageDto[]>(`${CASES_V7}/${id}/messages`).pipe(
      map((rows) => (rows ?? []).map(toMessage).sort((a, b) => (b.sent > a.sent ? 1 : b.sent < a.sent ? -1 : 0))),
      catchError(() => of([])),
    );
  }

  /**
   * Loads a full case with its parties, due dates and documents; null on error. The case itself is
   * read from v2 (a superset of v1) so its raw writable DTO can be stashed for the editor.
   */
  loadDetail(id: string): Observable<CaseDetail | null> {
    return forkJoin({
      caseDto: this.http.get<CaseDto>(`${CASES_V2}/${id}`).pipe(map((c) => { this.rawSelected.set(c); return c; })),
      parties: this.http.get<PartyDto[]>(`${CASES_BASE}/${id}/parties`).pipe(catchError(() => of([]))),
      // v4 duedates carry the calendar id (v1 does not); fall back to v1 if v4 is unavailable.
      dueDates: this.http.get<DueDateDto[]>(`${CASES_V4}/${id}/duedates`).pipe(
        catchError(() => this.http.get<DueDateDto[]>(`${CASES_BASE}/${id}/duedates`).pipe(catchError(() => of([])))),
      ),
      documents: this.http.get<DocDto[]>(`${CASES_BASE}/${id}/documents`).pipe(catchError(() => of([]))),
      // The document folder tree; 204 (no root folder) yields null.
      rootFolder: this.http.get<FolderDto | null>(`${CASES_V3}/${id}/folders`).pipe(catchError(() => of(null))),
      // Colour lookups (calendar-id -> colour, party-type-name -> colour); cached app-wide.
      calendarColors: this.calendarColors(),
      partyTypeColors: this.partyTypeColors(),
    }).pipe(
      map(({ caseDto, parties, dueDates, documents, rootFolder, calendarColors, partyTypeColors }) =>
        toDetail(caseDto, parties, dueDates, documents, rootFolder, calendarColors, partyTypeColors)),
      catchError(() => { this.rawSelected.set(null); return of(null); }),
    );
  }

  /**
   * Creates (no id) or updates (with id) a case's master data via the v2 endpoints, returning the
   * saved DTO. The caller sends the full working copy (cloned from {@link rawSelected}) so unedited
   * fields round-trip unchanged.
   */
  saveCase(data: CaseWrite): Observable<CaseWrite> {
    const url = data.id ? `${CASES_V2}/update` : `${CASES_V2}/create`;
    return defer(() => {
      this.saving.set(true);
      return this.http.put<CaseWrite>(url, data);
    }).pipe(finalize(() => this.saving.set(false)));
  }

  private partyTypes$?: Observable<PartyTypeOption[]>;

  /** The configured party/involvement types for the add-party dropdown. Cached app-wide. */
  partyTypes(): Observable<PartyTypeOption[]> {
    this.partyTypes$ ??= this.http.get<PartyTypeDto[]>(PARTY_TYPES).pipe(
      map((list) => (list ?? [])
        .filter((t) => t.name)
        .map((t) => ({ name: t.name!, color: rgbHex(t.color), placeHolder: !!(t as { placeHolder?: boolean }).placeHolder }))),
      catchError(() => of([])),
      shareReplay(1),
    );
    return this.partyTypes$;
  }

  /** Links a contact to a case as a party (PUT /v1/cases/party/create). */
  addParty(party: PartyWrite): Observable<unknown> {
    return defer(() => {
      this.saving.set(true);
      return this.http.put(`${CASES_BASE}/party/create`, party);
    }).pipe(finalize(() => this.saving.set(false)));
  }

  /** Updates an existing party's editable fields (PUT /v1/cases/party/update). */
  updateParty(party: PartyUpdate): Observable<unknown> {
    return defer(() => {
      this.saving.set(true);
      return this.http.put(`${CASES_BASE}/party/update`, party);
    }).pipe(finalize(() => this.saving.set(false)));
  }

  /** Removes a party from a case (DELETE /v1/cases/party/{id}/delete). */
  removeParty(partyId: string): Observable<unknown> {
    return defer(() => {
      this.saving.set(true);
      return this.http.delete(`${CASES_BASE}/party/${encodeURIComponent(partyId)}/delete`);
    }).pipe(finalize(() => this.saving.set(false)));
  }

  /**
   * Uploads a new document to a case (PUT /v1/cases/document/create). The content is sent
   * base64-encoded; `folderId` files it into a sub-folder (empty = the case root).
   */
  uploadDocument(caseId: string, fileName: string, base64content: string, folderId?: string): Observable<unknown> {
    const body: Record<string, string> = { caseId, fileName, base64content };
    if (folderId) {
      body['folderId'] = folderId;
    }
    return this.http.put(`${CASES_BASE}/document/create`, body);
  }

  /** Deletes a case document (DELETE /v1/cases/document/{id}/delete). */
  deleteDocument(documentId: string): Observable<unknown> {
    return this.http.delete(`${CASES_BASE}/document/${encodeURIComponent(documentId)}/delete`);
  }

  /** Contact search for the add-party picker (by name / file number / …); [] on error. */
  searchContacts(query: string): Observable<ContactRef[]> {
    const params = new HttpParams().set('offset', '0').set('limit', '20').set('filter', 'all').set('q', query);
    return this.http.get<ContactPageHitDto>(`${CONTACTS_V8}/page`, { params }).pipe(
      map((page) => (page.items ?? []).map(toContactRef)),
      catchError(() => of([])),
    );
  }

  private users$?: Observable<CaseUserRef[]>;

  /** Login-enabled users for the Anwalt/Sachbearbeiter dropdowns (GET /v6/security/users). Cached. */
  users(): Observable<CaseUserRef[]> {
    this.users$ ??= this.http.get<UserDto[]>(USERS_V6).pipe(
      map((rows) => (rows ?? [])
        .map((u) => ({ principalId: u.principalId, displayName: u.displayName || u.principalId }))
        .sort((a, b) => a.displayName.localeCompare(b.displayName))),
      catchError(() => of([])),
      shareReplay(1),
    );
    return this.users$;
  }

  private calendarColors$?: Observable<Map<string, string>>;
  private partyTypeColors$?: Observable<Map<string, string>>;

  /** Calendar id -> CSS hex colour, from the configured calendars. Cached for the app lifetime. */
  private calendarColors(): Observable<Map<string, string>> {
    this.calendarColors$ ??= this.http.get<CalendarDto[]>(CALENDARS_V4).pipe(
      map((list) => new Map((list ?? []).map((c) => [c.id, rgbHex(c.background)] as const))),
      catchError(() => of(new Map<string, string>())),
      shareReplay(1),
    );
    return this.calendarColors$;
  }

  /** Party-type name -> CSS hex colour, from the configured party types. Cached for the app lifetime. */
  private partyTypeColors(): Observable<Map<string, string>> {
    this.partyTypeColors$ ??= this.http.get<PartyTypeDto[]>(PARTY_TYPES).pipe(
      map((list) => new Map((list ?? []).filter((t) => t.name).map((t) => [t.name!, rgbHex(t.color)] as const))),
      catchError(() => of(new Map<string, string>())),
      shareReplay(1),
    );
    return this.partyTypeColors$;
  }
}

function toOverview(dto: CaseListDto): CaseOverview {
  return {
    id: dto.id,
    fileNumber: dto.fileNumber,
    name: dto.name,
    reason: dto.reason ?? '',
    subjectField: dto.subjectField ?? '',
    lawyer: dto.lawyer ?? '',
    archived: !!dto.archived,
    status: dto.archived ? 'closed' : 'open',
    lastChanged: isoDate(dto.dateChanged),
  };
}

function toGroup(dto: GroupDto): CaseGroup {
  return { id: dto.id, name: dto.name || dto.abbreviation || '', abbreviation: dto.abbreviation || '' };
}

/** A contact page hit to an add-party picker option ("Nachname, Vorname" or company, + place). */
function toContactRef(dto: ContactHitDto): ContactRef {
  const person = [dto.name, dto.firstName].map((s) => (s ?? '').trim()).filter(Boolean).join(', ');
  const display = (dto.company ?? '').trim() || person || '—';
  const place = [dto.zipCode, dto.city].map((s) => (s ?? '').trim()).filter(Boolean).join(' ');
  return { id: dto.id, label: place ? `${display} · ${place}` : display };
}

function toDetail(
  dto: CaseDto, parties: PartyDto[], dueDates: DueDateDto[], documents: DocDto[], rootFolder: FolderDto | null,
  calendarColors: Map<string, string>, partyTypeColors: Map<string, string>,
): CaseDetail {
  const mappedDue = (dueDates ?? []).map((d) => toDueDate(d, calendarColors));
  const archived = !!dto.archived;
  return {
    id: dto.id,
    fileNumber: dto.fileNumber,
    name: dto.name,
    reason: dto.reason ?? '',
    subjectField: dto.subjectField ?? '',
    lawyer: dto.lawyer ?? '',
    assistant: dto.assistant ?? '',
    claimNumber: dto.claimNumber ?? '',
    claimValue: dto.claimValue ?? 0,
    notice: dto.notice ?? '',
    archived,
    status: deriveStatus(archived, mappedDue),
    parties: (parties ?? []).map((p) => ({
      id: p.id,
      involvementType: p.involvementType ?? '',
      // Prefer the resolved contact display name (linked address); fall back to the
      // free-text contact person, then leave empty for the template's "—" placeholder.
      contact: p.contactName || p.contact || '',
      reference: p.reference ?? '',
      contactName: p.contactName ?? '',
      contactPerson: p.contact ?? '',
      custom1: p.custom1 ?? '',
      custom2: p.custom2 ?? '',
      custom3: p.custom3 ?? '',
      addressId: p.addressId ?? '',
      // The party type's colour, matched by its name (= involvementType).
      color: partyTypeColors.get(p.involvementType ?? '') ?? '',
    } satisfies Party)),
    dueDates: mappedDue,
    documents: (documents ?? []).map(toDocument),
    rootFolder: rootFolder ? toFolder(rootFolder) : null,
  };
}

function toFolder(dto: FolderDto): DocFolder {
  return {
    id: dto.id,
    name: dto.name ?? '',
    children: (dto.children ?? []).map(toFolder),
  };
}

function toDueDate(dto: DueDateDto, calendarColors: Map<string, string>): DueDate {
  return {
    id: dto.id,
    // v1 -> reason/dueDate, v4 -> summary/beginDate.
    reason: dto.reason || dto.summary || '',
    dueDate: isoDate(dto.dueDate || dto.beginDate || ''),
    done: !!dto.done,
    assignee: dto.assignee ?? '',
    type: dto.type === 'RESPITE' ? 'deadline' : 'followup',
    calendarColor: (dto.calendar && calendarColors.get(dto.calendar)) || '',
    description: dto.description ?? '',
    location: dto.location ?? '',
    endDate: isoDate(dto.endDate || ''),
    reminderMinutes: dto.reminderMinutes ?? -1,
    calendarId: dto.calendar ?? '',
    restType: (dto.type ?? 'FOLLOWUP').toUpperCase(),
  };
}

/**
 * Converts a packed-RGB int colour (as stored server-side, the argument to java.awt.Color(int))
 * to a CSS hex string. Only the low 24 bits are used (any alpha byte is ignored), matching the
 * desktop client's `new Color(int)`. Returns '' for null/undefined.
 */
function rgbHex(value: number | undefined | null): string {
  if (value == null) { return ''; }
  return '#' + (value & 0xffffff).toString(16).padStart(6, '0');
}

function toDocument(dto: DocDto): CaseDocument {
  return {
    id: dto.id,
    name: dto.name,
    date: isoDate(dto.creationDate),
    changeDate: isoDate(dto.changeDate),
    size: formatBytes(dto.size),
    sizeBytes: dto.size ?? 0,
    ext: extensionOf(dto.name),
    folderId: dto.folderId ?? '',
    favorite: !!dto.favorite,
    version: dto.version ?? 1,
    highlight1: highlightHex(dto.highlight1),
    highlight2: highlightHex(dto.highlight2),
    tags: (dto.tags ?? []).map((t) => t.name ?? '').filter(Boolean),
  };
}

/**
 * A document label colour to CSS hex, or '' when unset. The server uses Integer.MIN_VALUE (and
 * -1 on some DTOs) as the "no colour" sentinel; every other value is a packed RGB int.
 */
function highlightHex(value: number | undefined | null): string {
  if (value == null || value === -1 || value === -2147483648) { return ''; }
  return rgbHex(value);
}

function toHistory(dto: HistoryDto): CaseHistoryEntry {
  return {
    id: dto.id,
    principal: dto.principal ?? '',
    changeDate: dto.changeDate ?? 0,
    changeDescription: dto.changeDescription ?? '',
  };
}

function toInvoice(dto: InvoiceDto): CaseInvoice {
  return {
    id: dto.id,
    invoiceNumber: dto.invoiceNumber ?? '',
    name: dto.name ?? '',
    status: dto.status ?? '',
    total: dto.total ?? 0,
    totalGross: dto.totalGross ?? 0,
    currency: dto.currency ?? '€',
    dueDate: isoDate(dto.dueDate),
    creationDate: isoDate(dto.creationDate),
    description: dto.description ?? '',
    invoiceType: dto.invoiceType ?? '',
    invoiceTypeId: dto.invoiceTypeId ?? '',
    lastPoolId: dto.lastPoolId ?? '',
    contactId: dto.contactId ?? '',
    sender: dto.sender ?? '',
    paymentType: dto.paymentType ?? 'BANKTRANSFER',
    smallBusiness: !!dto.smallBusiness,
    periodFrom: isoDate(dto.periodFrom),
    periodTo: isoDate(dto.periodTo),
  };
}

function toInvoicePosition(dto: InvoicePositionDto): InvoicePositionItem {
  return {
    id: dto.id,
    name: dto.name ?? '',
    description: dto.description ?? '',
    position: dto.position ?? 0,
    taxRate: dto.taxRate ?? 0,
    units: dto.units ?? 0,
    unitPrice: dto.unitPrice ?? 0,
    total: dto.total ?? 0,
    invoiceId: dto.invoiceId ?? '',
  };
}

function toPayment(dto: PaymentDto): CasePayment {
  return {
    id: dto.id,
    paymentNumber: dto.paymentNumber ?? '',
    name: dto.name ?? '',
    reason: dto.reason ?? '',
    status: dto.status ?? '',
    total: dto.total ?? 0,
    currency: dto.currency ?? '€',
    targetDate: isoDate(dto.targetDate),
    creationDate: isoDate(dto.creationDate),
  };
}

function toAccountEntry(dto: AccountEntryDto): AccountEntry {
  const earnings = dto.earnings ?? 0;
  const spendings = dto.spendings ?? 0;
  const escrowIn = dto.escrowIn ?? 0;
  const escrowOut = dto.escrowOut ?? 0;
  const expendituresIn = dto.expendituresIn ?? 0;
  const expendituresOut = dto.expendituresOut ?? 0;
  return {
    id: dto.id,
    date: isoDate(dto.entryDate),
    description: dto.description ?? '',
    contact: dto.contactName ?? '',
    contactId: dto.contactId ?? '',
    invoiceId: dto.invoiceId ?? '',
    earnings,
    spendings,
    escrowIn,
    escrowOut,
    expendituresIn,
    expendituresOut,
    total: earnings + escrowIn + expendituresIn - spendings - escrowOut - expendituresOut,
  };
}

/** Placeholder timesheet when a running position's project can't be resolved (keeps the row usable). */
function emptyTimesheet(id: string): CaseTimesheet {
  return { id, name: '', description: '', interval: 0, limited: false, limit: 0, percentageDone: 0, status: 10 };
}

function toTimesheet(dto: TimesheetDto): CaseTimesheet {
  return {
    id: dto.id,
    name: dto.name ?? '',
    description: dto.description ?? '',
    interval: dto.interval ?? 0,
    limited: !!dto.limited,
    limit: dto.limit ?? 0,
    percentageDone: dto.percentageDone ?? 0,
    status: dto.status ?? 10,
    caseId: dto.caseId,
    caseFileNumber: dto.caseFileNumber,
    caseName: dto.caseName,
  };
}

function toTimesheetPosition(dto: TimesheetPositionDto): TimesheetPosition {
  return {
    id: dto.id,
    name: dto.name ?? '',
    description: dto.description ?? '',
    principal: dto.principal ?? '',
    started: isoDate(dto.started),
    stopped: isoDate(dto.stopped),
    unitPrice: dto.unitPrice ?? 0,
    taxRate: dto.taxRate ?? 0,
    total: dto.total ?? 0,
    timesheetId: dto.timesheetId ?? '',
    invoiceId: dto.invoiceId ?? '',
    running: !!dto.running,
  };
}

function toMessage(dto: MessageDto): CaseMessage {
  return {
    id: dto.id,
    sent: typeof dto.sent === 'number' ? new Date(dto.sent).toISOString() : isoDate(dto.sent),
    sender: dto.sender ?? '',
    content: dto.content ?? '',
  };
}

function deriveStatus(archived: boolean, dueDates: DueDate[]): CaseStatus {
  if (archived) {
    return 'closed';
  }
  const endOfToday = new Date();
  endOfToday.setHours(23, 59, 59, 999);
  const urgent = dueDates.some((d) => !d.done && d.dueDate && new Date(d.dueDate) <= endOfToday);
  return urgent ? 'dueToday' : 'open';
}

/** Strips the Java ZonedDateTime zone suffix ("2024-05-13T22:00:00Z[UTC]" -> parseable ISO). */
function isoDate(value: string | null | undefined): string {
  return value ? value.replace(/\[.*\]$/, '') : '';
}

function extensionOf(name: string): string {
  const dot = name.lastIndexOf('.');
  return dot > -1 && dot < name.length - 1 ? name.slice(dot + 1).toUpperCase() : '';
}

function formatBytes(bytes: number): string {
  if (!bytes || bytes < 0) {
    return '—';
  }
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  const kb = bytes / 1024;
  if (kb < 1024) {
    return `${Math.round(kb)} KB`;
  }
  return `${(kb / 1024).toFixed(1)} MB`;
}
