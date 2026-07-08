import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { catchError, forkJoin, map, Observable, of } from 'rxjs';
import { API_ROOT } from '../core/api';
import { CaseDetail, CaseDocument, CaseOverview, CaseStatus, DueDate, Party } from './case.models';

const CASES_BASE = `${API_ROOT}/v1/cases`;
const CASES_V8 = `${API_ROOT}/v8/cases`;
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
interface PartyDto { id: string; involvementType: string; contact?: string; }
interface DueDateDto { id: string; reason: string; dueDate: string; done: boolean; assignee: string; type: string; }
interface DocDto { id: string; name: string; size: number; creationDate: string; }
interface DocumentContentDto { id: string; fileName: string; caseId: string; base64content: string; }

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

  /** Loads a single document's Base64-encoded content (GET /v1/cases/document/{id}/content). */
  documentContent(id: string): Observable<DocumentContentDto> {
    return this.http.get<DocumentContentDto>(`${CASES_BASE}/document/${id}/content`);
  }

  /** Loads a full case with its parties, due dates and documents; null on error. */
  loadDetail(id: string): Observable<CaseDetail | null> {
    return forkJoin({
      caseDto: this.http.get<CaseDto>(`${CASES_BASE}/${id}`),
      parties: this.http.get<PartyDto[]>(`${CASES_BASE}/${id}/parties`).pipe(catchError(() => of([]))),
      dueDates: this.http.get<DueDateDto[]>(`${CASES_BASE}/${id}/duedates`).pipe(catchError(() => of([]))),
      documents: this.http.get<DocDto[]>(`${CASES_BASE}/${id}/documents`).pipe(catchError(() => of([]))),
    }).pipe(
      map(({ caseDto, parties, dueDates, documents }) => toDetail(caseDto, parties, dueDates, documents)),
      catchError(() => of(null)),
    );
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

function toDetail(dto: CaseDto, parties: PartyDto[], dueDates: DueDateDto[], documents: DocDto[]): CaseDetail {
  const mappedDue = (dueDates ?? []).map(toDueDate);
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
      contact: p.contact ?? '',
    } satisfies Party)),
    dueDates: mappedDue,
    documents: (documents ?? []).map(toDocument),
  };
}

function toDueDate(dto: DueDateDto): DueDate {
  return {
    id: dto.id,
    reason: dto.reason ?? '',
    dueDate: isoDate(dto.dueDate),
    done: !!dto.done,
    assignee: dto.assignee ?? '',
    type: dto.type === 'RESPITE' ? 'deadline' : 'followup',
  };
}

function toDocument(dto: DocDto): CaseDocument {
  return {
    id: dto.id,
    name: dto.name,
    date: isoDate(dto.creationDate),
    size: formatBytes(dto.size),
    ext: extensionOf(dto.name),
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
