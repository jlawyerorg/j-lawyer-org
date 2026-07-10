import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { catchError, forkJoin, map, Observable, of, shareReplay, switchMap } from 'rxjs';
import { API_ROOT } from '../core/api';
import {
  ContactCase, ContactDetail, ContactDocument, ContactField, ContactKV, ContactSection,
  ContactFilter, ContactOverview, ContactType,
} from './contact.models';

const CONTACTS_V1 = `${API_ROOT}/v1/contacts`;
const CONTACTS_V2 = `${API_ROOT}/v2/contacts`;
const CONTACTS_V5 = `${API_ROOT}/v5/contacts`;
const CONTACTS_V7 = `${API_ROOT}/v7/contacts`;
const CONTACTS_V8 = `${API_ROOT}/v8/contacts`;
const CASES_PARTIES = (caseId: string) => `${API_ROOT}/v1/cases/${caseId}/parties`;
const PAGE_SIZE = 50;

/** Wire shapes returned by j-lawyer-io (only the fields we consume). */
interface ContactListDto {
  id: string; title: string; salutation: string; firstName: string; name: string;
  company: string; department: string; street: string; streetNumber: string;
  zipCode: string; city: string; country: string; phone: string; mobile: string;
  email: string; website: string;
}
interface ContactPageDto { total: number; offset: number; limit: number; items: ContactListDto[]; }
// RestfulContactV2 (GET /v2/contacts/{id}). Jackson omits empty fields, so all are optional.
interface ContactDto {
  id: string;
  title?: string; salutation?: string; firstName?: string; firstName2?: string; name?: string;
  company?: string; department?: string;
  street?: string; streetNumber?: string; zipCode?: string; city?: string; country?: string;
  state?: string; district?: string; adjunct?: string;
  phone?: string; mobile?: string; fax?: string;
  email?: string; emailHome?: string; emailMisc?: string; website?: string; beaSafeId?: string;
  birthDate?: string; birthName?: string; placeOfBirth?: string; dateOfDeath?: string;
  nationality?: string; profession?: string; role?: string; initials?: string;
  degreePrefix?: string; degreeSuffix?: string; titleInAddress?: string;
  legalForm?: string; companyRegistrationNumber?: string; companyRegistrationCourt?: string;
  vatId?: string; tin?: string;
  bankName?: string; bankCode?: string; bankAccount?: string; bankAccountOwner?: string;
  sepaReference?: string; sepaSince?: string; leitwegId?: string;
  legalProtection?: number; insuranceName?: string; insuranceNumber?: string; insurant?: string;
  trafficLegalProtection?: number; trafficInsuranceName?: string; trafficInsuranceNumber?: string; trafficInsurant?: string;
  motorLegalProtection?: number; motorInsuranceName?: string; motorInsuranceNumber?: string; motorInsurant?: string;
  taxDeduction?: number;
  complimentaryClose?: string; notice?: string;
  custom1?: string; custom2?: string; custom3?: string;
  externalId?: string; externalId2?: string; externalId3?: string; externalId4?: string; externalId5?: string;
}
interface AddressDocDto { id: string; name: string; size: number; creationDate: string; changeDate: string; }
interface ContactCaseDto { id: string; fileNumber: string; name: string; reason: string; dateChanged: string; }
interface CasePartyDto { addressId?: string; involvementType?: string; }
interface PartyTypeDto { name?: string; color?: number; }

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

  /** Loads a full contact (the richer v2 detail); null on error. */
  loadDetail(id: string): Observable<ContactDetail | null> {
    return this.http.get<ContactDto>(`${CONTACTS_V2}/${id}`).pipe(
      map((dto) => toDetail(dto)),
      catchError(() => of(null)),
    );
  }

  /**
   * Cases the contact is involved in (GET /v5/contacts/{id}/cases), newest change first; [] on
   * error. The case-overview DTO carries no role, so for each case we read its parties
   * (/v1/cases/{id}/parties), match the party whose addressId is this contact and take its
   * involvement type as the role, coloured via the configured party types (like the case view).
   */
  cases(id: string): Observable<ContactCase[]> {
    return forkJoin({
      list: this.http.get<ContactCaseDto[]>(`${CONTACTS_V5}/${id}/cases`).pipe(catchError(() => of([]))),
      colors: this.partyTypeColors(),
    }).pipe(
      switchMap(({ list, colors }) => {
        const rows = list ?? [];
        if (!rows.length) { return of([] as ContactCase[]); }
        return forkJoin(rows.map((c) =>
          this.http.get<CasePartyDto[]>(`${CASES_PARTIES(c.id)}`).pipe(
            catchError(() => of([] as CasePartyDto[])),
            map((parties) => {
              const role = (parties ?? []).find((p) => p.addressId === id)?.involvementType ?? '';
              return {
                id: c.id,
                fileNumber: c.fileNumber ?? '',
                name: c.name ?? '',
                reason: c.reason ?? '',
                dateChanged: isoDate(c.dateChanged),
                role,
                roleColor: colors.get(role) ?? '',
              } satisfies ContactCase;
            }),
          )));
      }),
      map((rows) => rows.sort((a, b) => (a.dateChanged < b.dateChanged ? 1 : a.dateChanged > b.dateChanged ? -1 : 0))),
      catchError(() => of([])),
    );
  }

  private partyTypeColors$?: Observable<Map<string, string>>;

  /** Party-type name -> CSS hex colour, from the configured party types. Cached app-wide. */
  private partyTypeColors(): Observable<Map<string, string>> {
    this.partyTypeColors$ ??= this.http.get<PartyTypeDto[]>(`${API_ROOT}/v1/cases/party/types`).pipe(
      map((list) => new Map((list ?? []).filter((t) => t.name).map((t) => [t.name!, rgbHex(t.color)] as const))),
      catchError(() => of(new Map<string, string>())),
      shareReplay(1),
    );
    return this.partyTypeColors$;
  }

  /** Documents attached directly to the contact (GET /v7/contacts/{id}/documents); [] on error. */
  documents(id: string): Observable<ContactDocument[]> {
    return this.http.get<AddressDocDto[]>(`${CONTACTS_V7}/${id}/documents`).pipe(
      map((rows) => (rows ?? []).map(toContactDocument)),
      catchError(() => of([])),
    );
  }
}

function toContactDocument(dto: AddressDocDto): ContactDocument {
  return {
    id: dto.id,
    name: dto.name,
    date: isoDate(dto.creationDate),
    changeDate: isoDate(dto.changeDate),
    size: formatBytes(dto.size),
    sizeBytes: dto.size ?? 0,
    ext: extensionOf(dto.name),
  };
}

/** Strips a trailing "[Zone]" suffix from an ISO timestamp (e.g. "…Z[UTC]" -> "…Z"). */
function isoDate(value: string | null | undefined): string {
  return value ? value.replace(/\[.*\]$/, '') : '';
}

/** Packed-RGB int colour (java.awt.Color(int) argument) to CSS hex; '' for null/undefined. */
function rgbHex(value: number | undefined | null): string {
  if (value == null) { return ''; }
  return '#' + (value & 0xffffff).toString(16).padStart(6, '0');
}

function extensionOf(name: string): string {
  const dot = name.lastIndexOf('.');
  return dot > -1 && dot < name.length - 1 ? name.slice(dot + 1).toUpperCase() : '';
}

function formatBytes(bytes: number): string {
  if (!bytes) { return '0 B'; }
  if (bytes < 1024) { return `${bytes} B`; }
  if (bytes < 1024 * 1024) { return `${Math.round(bytes / 1024)} KB`; }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
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

/** A label/value pair, or null when the value is empty (filtered out of a section). */
function kv(labelKey: string, value: string | null | undefined): ContactKV | null {
  const v = (value ?? '').trim();
  return v ? { labelKey, value: v } : null;
}

/** Builds a section from label/value candidates, dropping empties; null when the section is empty. */
function section(key: string, fields: (ContactKV | null)[]): ContactSection | null {
  const kept = fields.filter((f): f is ContactKV => f !== null);
  return kept.length ? { key, fields: kept } : null;
}

/** Renders a 0/1 "short" flag as a localized-ish "ja", or null when unset. */
function flag(labelKey: string, value: number | undefined): ContactKV | null {
  return value === 1 ? { labelKey, value: 'ja' } : null;
}

function toDetail(dto: ContactDto): ContactDetail {
  const type = contactType(dto.company ?? '');
  const honorific = [dto.salutation, dto.title].map((s) => (s ?? '').trim()).filter(Boolean).join(' ');

  const channels = [
    field('phone', dto.phone, 'phone', 'tel:'),
    field('mobile', dto.mobile, 'phone', 'tel:'),
    field('fax', dto.fax, 'phone'),
    field('email', dto.email, 'mail', 'mailto:'),
    field('emailHome', dto.emailHome, 'mail', 'mailto:'),
    field('emailMisc', dto.emailMisc, 'mail', 'mailto:'),
    field('website', dto.website, 'globe'),
    field('beaSafeId', dto.beaSafeId, 'shield'),
  ].filter((f): f is ContactField => f !== null);

  const streetLine = [dto.street, dto.streetNumber].map((s) => (s ?? '').trim()).filter(Boolean).join(' ');
  const addressLines = [
    streetLine, (dto.adjunct ?? '').trim(), (dto.district ?? '').trim(),
    cityLine(dto.zipCode ?? '', dto.city ?? ''), (dto.state ?? '').trim(), (dto.country ?? '').trim(),
  ].filter(Boolean);

  // Grouped sections — each dropped entirely when it has no populated field.
  const sections = [
    section('person', [
      kv('firstName2', dto.firstName2),
      kv('birthName', dto.birthName),
      kv('birthDate', dto.birthDate),
      kv('placeOfBirth', dto.placeOfBirth),
      kv('dateOfDeath', dto.dateOfDeath),
      kv('nationality', dto.nationality),
      kv('profession', dto.profession),
      kv('role', dto.role),
      kv('degreePrefix', dto.degreePrefix),
      kv('degreeSuffix', dto.degreeSuffix),
      kv('initials', dto.initials),
    ]),
    section('organisation', [
      kv('department', dto.department),
      kv('legalForm', dto.legalForm),
      kv('companyRegistrationNumber', dto.companyRegistrationNumber),
      kv('companyRegistrationCourt', dto.companyRegistrationCourt),
      kv('vatId', dto.vatId),
      kv('tin', dto.tin),
    ]),
    section('bank', [
      kv('bankName', dto.bankName),
      kv('bankCode', dto.bankCode),
      kv('bankAccount', dto.bankAccount),
      kv('bankAccountOwner', dto.bankAccountOwner),
      kv('sepaReference', dto.sepaReference),
      kv('sepaSince', dto.sepaSince),
      kv('leitwegId', dto.leitwegId),
    ]),
    section('insurance', [
      flag('legalProtection', dto.legalProtection),
      kv('insuranceName', dto.insuranceName),
      kv('insuranceNumber', dto.insuranceNumber),
      kv('insurant', dto.insurant),
      flag('trafficLegalProtection', dto.trafficLegalProtection),
      kv('trafficInsuranceName', dto.trafficInsuranceName),
      kv('trafficInsuranceNumber', dto.trafficInsuranceNumber),
      kv('trafficInsurant', dto.trafficInsurant),
      flag('motorLegalProtection', dto.motorLegalProtection),
      kv('motorInsuranceName', dto.motorInsuranceName),
      kv('motorInsuranceNumber', dto.motorInsuranceNumber),
      kv('motorInsurant', dto.motorInsurant),
      flag('taxDeduction', dto.taxDeduction),
    ]),
    section('letter', [
      kv('salutationField', dto.salutation),
      kv('titleInAddress', dto.titleInAddress),
      kv('complimentaryClose', dto.complimentaryClose),
    ]),
    section('custom', [
      kv('custom1', dto.custom1),
      kv('custom2', dto.custom2),
      kv('custom3', dto.custom3),
    ]),
    section('externalIds', [
      kv('externalId', dto.externalId),
      kv('externalId2', dto.externalId2),
      kv('externalId3', dto.externalId3),
      kv('externalId4', dto.externalId4),
      kv('externalId5', dto.externalId5),
    ]),
  ].filter((s): s is ContactSection => s !== null);

  return {
    id: dto.id,
    type,
    displayName: displayNameOf(dto.company ?? '', dto.firstName ?? '', dto.name ?? ''),
    honorific,
    company: (dto.company ?? '').trim(),
    department: (dto.department ?? '').trim(),
    channels,
    addressLines,
    sections,
    notice: (dto.notice ?? '').trim(),
  };
}
