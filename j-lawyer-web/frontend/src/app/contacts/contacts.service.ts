import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { catchError, map, Observable, of } from 'rxjs';
import { API_ROOT } from '../core/api';
import { ContactDetail, ContactField, ContactFilter, ContactOverview, ContactType } from './contact.models';

const CONTACTS_V1 = `${API_ROOT}/v1/contacts`;
const CONTACTS_V8 = `${API_ROOT}/v8/contacts`;
const PAGE_SIZE = 50;

/** Wire shapes returned by j-lawyer-io (only the fields we consume). */
interface ContactListDto {
  id: string; title: string; salutation: string; firstName: string; name: string;
  company: string; department: string; street: string; streetNumber: string;
  zipCode: string; city: string; country: string; phone: string; mobile: string;
  email: string; website: string;
}
interface ContactPageDto { total: number; offset: number; limit: number; items: ContactListDto[]; }
interface ContactDto {
  id: string; title: string; salutation: string; firstName: string; name: string;
  company: string; department: string; street: string; streetNumber: string;
  zipCode: string; city: string; country: string; phone: string; mobile: string;
  fax: string; email: string; emailHome: string; emailMisc: string; website: string;
  birthDate: string; custom1: string; custom2: string; custom3: string;
}

/**
 * Contact data access against the real REST API. The list is filtered/searched and paginated
 * server-side (GET /v8/contacts/page, infinite scroll via {@link loadMore}); the full contact
 * detail is fetched lazily per selection (GET /v1/contacts/{id}). The Bearer token is attached
 * by authInterceptor.
 */
@Injectable({ providedIn: 'root' })
export class ContactsService {
  private readonly http = inject(HttpClient);

  /** Accumulated rows across the pages loaded so far (server-side paginated). */
  readonly overviews = signal<ContactOverview[]>([]);
  readonly total = signal(0);
  readonly listLoading = signal(false);
  readonly listError = signal(false);

  private currentFilter: ContactFilter = 'all';
  private currentSearch = '';
  private requestSeq = 0;

  get filter(): ContactFilter {
    return this.currentFilter;
  }

  /** True while more pages are available for the current filter/search. */
  get hasMore(): boolean {
    return this.overviews().length < this.total();
  }

  /** (Re)loads the first page with the given filter (server-side). */
  setFilter(filter: ContactFilter): void {
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

    this.http.get<ContactPageDto>(`${CONTACTS_V8}/page`, { params }).subscribe({
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

  /** Loads a full contact; null on error. */
  loadDetail(id: string): Observable<ContactDetail | null> {
    return this.http.get<ContactDto>(`${CONTACTS_V1}/${id}`).pipe(
      map((dto) => toDetail(dto)),
      catchError(() => of(null)),
    );
  }
}

function contactType(company: string | null | undefined): ContactType {
  return (company ?? '').trim() ? 'company' : 'person';
}

function personName(firstName: string, name: string): string {
  const first = (firstName ?? '').trim();
  const last = (name ?? '').trim();
  if (first && last) {
    return `${last}, ${first}`;
  }
  return last || first;
}

function displayNameOf(company: string, firstName: string, name: string): string {
  const c = (company ?? '').trim();
  return c || personName(firstName, name) || '—';
}

function initialsOf(display: string): string {
  return display
    .replace(/[,]/g, ' ')
    .split(/\s+/)
    .map((w) => w.match(/[a-z0-9]/i)?.[0] ?? '')
    .filter(Boolean)
    .slice(0, 2)
    .join('')
    .toUpperCase();
}

function cityLine(zipCode: string, city: string): string {
  return [zipCode, city].map((s) => (s ?? '').trim()).filter(Boolean).join(' ');
}

function toOverview(dto: ContactListDto): ContactOverview {
  const displayName = displayNameOf(dto.company, dto.firstName, dto.name);
  const place = cityLine(dto.zipCode, dto.city);
  return {
    id: dto.id,
    type: contactType(dto.company),
    displayName,
    subtitle: place || (dto.email ?? '').trim() || (dto.phone ?? '').trim(),
    city: dto.city ?? '',
    zipCode: dto.zipCode ?? '',
    email: dto.email ?? '',
    phone: dto.phone ?? '',
    initials: initialsOf(displayName) || '#',
  };
}

function field(key: string, value: string | null | undefined, icon?: string, href?: string): ContactField | null {
  const v = (value ?? '').trim();
  if (!v) {
    return null;
  }
  return href ? { key, value: v, icon, href: href + v } : { key, value: v, icon };
}

function toDetail(dto: ContactDto): ContactDetail {
  const type = contactType(dto.company);
  const honorific = [dto.salutation, dto.title].map((s) => (s ?? '').trim()).filter(Boolean).join(' ');

  const contactFields = [
    field('phone', dto.phone, 'phone', 'tel:'),
    field('mobile', dto.mobile, 'phone', 'tel:'),
    field('fax', dto.fax, 'phone'),
    field('email', dto.email, 'mail', 'mailto:'),
    field('emailHome', dto.emailHome, 'mail', 'mailto:'),
    field('emailMisc', dto.emailMisc, 'mail', 'mailto:'),
    field('website', dto.website, 'globe'),
  ].filter((f): f is ContactField => f !== null);

  const streetLine = [dto.street, dto.streetNumber].map((s) => (s ?? '').trim()).filter(Boolean).join(' ');
  const addressLines = [streetLine, cityLine(dto.zipCode, dto.city), (dto.country ?? '').trim()]
    .filter(Boolean);

  const moreFields = [
    field('department', dto.department),
    field('birthDate', dto.birthDate),
    field('custom1', dto.custom1),
    field('custom2', dto.custom2),
    field('custom3', dto.custom3),
  ].filter((f): f is ContactField => f !== null);

  return {
    id: dto.id,
    type,
    displayName: displayNameOf(dto.company, dto.firstName, dto.name),
    honorific,
    company: (dto.company ?? '').trim(),
    department: (dto.department ?? '').trim(),
    contactFields,
    addressLines,
    moreFields,
  };
}
