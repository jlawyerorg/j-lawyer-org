import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { catchError, forkJoin, map, Observable, of, shareReplay } from 'rxjs';
import { API_ROOT } from '../core/api';
import {
  AccountEntry, CaseDetail, CaseDocument, CaseGroup, CaseHistoryEntry, CaseInvoice, CaseMessage, CaseOverview,
  CasePayment, CaseStatus, CaseTag, CaseTimesheet, DocDateMode, DocFolder, DocSortKey, DueDate,
  Party, SortDir, TimesheetPosition,
} from './case.models';

const CASES_BASE = `${API_ROOT}/v1/cases`;
const CASES_V3 = `${API_ROOT}/v3/cases`;
const CASES_V4 = `${API_ROOT}/v4/cases`;
const CASES_V7 = `${API_ROOT}/v7/cases`;
const CASES_V8 = `${API_ROOT}/v8/cases`;
const TIMESHEETS_V8 = `${API_ROOT}/v8/timesheets`;
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
}
interface PartyDto { id: string; involvementType: string; contact?: string; contactName?: string; }
// v1 and v4 differ: v1 has `reason`/`dueDate`; v4 renames them to `summary`/`beginDate` and adds
// `calendar` (the calendar-setup id). We read whichever the server provides.
interface DueDateDto {
  id: string; done: boolean; assignee: string; type: string;
  reason?: string; dueDate?: string; summary?: string; beginDate?: string; calendar?: string;
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
}
interface PaymentDto {
  id: string; paymentNumber: string; name: string; reason: string; status: string;
  total: number; currency: string; targetDate: string; creationDate: string;
}
interface AccountEntryDto {
  id: string; entryDate: string; description: string; contactName?: string;
  earnings: number; spendings: number; escrowIn: number; escrowOut: number;
  expendituresIn: number; expendituresOut: number;
}
interface TimesheetDto {
  id: string; name: string; description: string; interval: number;
  limited: boolean; limit: number; percentageDone: number; status: number;
}
interface TimesheetPositionDto {
  id: string; name: string; description: string; principal: string; started: string; stopped: string;
  unitPrice: number; taxRate: number; total: number; timesheetId: string; invoiceId?: string; running: boolean;
}
interface TagDto { id: string; name: string; }
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

  /** Loads the case's labels ("Etiketten", GET /v1/cases/{id}/tags); [] on error. */
  tags(id: string): Observable<CaseTag[]> {
    return this.http.get<TagDto[]>(`${CASES_BASE}/${id}/tags`).pipe(
      map((rows) => (rows ?? []).map((t) => ({ id: t.id, name: t.name ?? '' }))),
      catchError(() => of([])),
    );
  }

  /** Loads the user groups allowed to access the case ("Berechtigungen", GET /v7/cases/{id}/groups); [] on error. */
  allowedGroups(id: string): Observable<CaseGroup[]> {
    return this.http.get<GroupDto[]>(`${CASES_V7}/${id}/groups`).pipe(
      map((rows) => (rows ?? []).map((g) => ({ id: g.id, name: g.name || g.abbreviation || '' }))),
      catchError(() => of([])),
    );
  }

  /** Loads the case's instant messages (GET /v7/cases/{id}/messages), most recent first; [] on error. */
  messages(id: string): Observable<CaseMessage[]> {
    return this.http.get<MessageDto[]>(`${CASES_V7}/${id}/messages`).pipe(
      map((rows) => (rows ?? []).map(toMessage).sort((a, b) => (b.sent > a.sent ? 1 : b.sent < a.sent ? -1 : 0))),
      catchError(() => of([])),
    );
  }

  /** Loads a full case with its parties, due dates and documents; null on error. */
  loadDetail(id: string): Observable<CaseDetail | null> {
    return forkJoin({
      caseDto: this.http.get<CaseDto>(`${CASES_BASE}/${id}`),
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
      catchError(() => of(null)),
    );
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
    earnings,
    spendings,
    escrowIn,
    escrowOut,
    expendituresIn,
    expendituresOut,
    total: earnings + escrowIn + expendituresIn - spendings - escrowOut - expendituresOut,
  };
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
