import { HttpClient } from '@angular/common/http';
import { effect, inject, Injectable, signal } from '@angular/core';
import { catchError, map, Observable, of, shareReplay } from 'rxjs';
import { API_ROOT } from '../core/api';
import { AuthService } from '../core/auth/auth.service';
import { CaseSuggestions } from '../communication/email.models';
import {
  BeaAttachment, BeaExport, BeaFolder, BeaIdentity, BeaIdentitySearchCriteria, BeaJournalEntry,
  BeaListItem, BeaMessage, BeaMessageHeader, BeaRestriction, BeaSendRequest, BeaVerificationResult,
  Postbox,
} from './bea.models';

const BEA_V8 = `${API_ROOT}/v8/bea`;

interface PostboxDto {
  safeId: string; userName?: string; displayName?: string; firstName?: string; surName?: string;
  officeName?: string; organization?: string; city?: string; chamber?: string;
}
interface FolderDto {
  id: number; parentId?: number; name?: string; type?: string; unreadMessageCount?: number; safeId?: string;
}
interface HeaderDto {
  id: string; subject?: string; sender?: string; receptionTime?: number | string | null;
  sentTime?: number | string | null; referenceJustice?: string; referenceNumber?: string;
  confidential?: boolean; signed?: boolean; read?: boolean; recipientName?: string;
  recipientSafeId?: string; folderId?: number; postBoxSafeId?: string;
}
interface AttachmentDto {
  name?: string; alias?: string; type?: number; size?: number; content?: string | null; technicalAttachment?: boolean;
}
interface JournalDto {
  journalType?: string; eventType?: string; timestamp?: number | string | null;
  fromSurnameFirstname?: string; fromUsername?: string; toSurnameFirstname?: string; attachmentReference?: string;
}
interface MessageDto {
  id: string; subject?: string; body?: string; receptionTime?: number | string | null;
  createdTime?: number | string | null; senderSafeId?: string; senderName?: string;
  referenceJustice?: string; referenceNumber?: string; confidential?: boolean; signed?: boolean;
  read?: boolean; eebRequested?: boolean; eebId?: string; messageType?: string;
  recipients?: { name?: string; safeId?: string }[]; attachments?: AttachmentDto[];
  vhnAttachments?: AttachmentDto[]; journal?: JournalDto[];
  verificationHtml?: string; verificationXml?: string; verificationStatus?: string;
}
interface LoginResultDto { postboxes?: PostboxDto[]; }
interface VerificationDto { html?: string; xml?: string; status?: string; }
interface IdentityDto {
  safeId?: string; firstName?: string; surName?: string; userName?: string; city?: string;
  zipCode?: string; officeName?: string; organization?: string; type?: string; displayName?: string;
}
interface ListItemDto { code?: string; name?: string; }
interface ExportDto { fileName?: string; contentBase64?: string; }

/**
 * beA data access against the v8 REST API (/v8/bea/**). All calls are Basic-auth-restricted
 * (loginRole); the Bearer token is attached by authInterceptor. Server-side, the endpoint
 * delegates to the external beAstie service using the user's stored certificate — the browser
 * never handles the beA certificate or PIN. Read & triage surface only.
 */
@Injectable({ providedIn: 'root' })
export class BeaService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);

  // Cached session state, held for the whole app session so re-opening the module is instant
  // (and the slow beAstie login runs once, not on every view open). The message list is fetched
  // fresh each time. State: 'loading' before/while connecting, 'ok', or 'error'.
  private readonly _sessionState = signal<'loading' | 'ok' | 'error'>('loading');
  private readonly _postboxes = signal<Postbox[]>([]);
  private readonly _folders = signal<Record<string, BeaFolder[]>>({});
  private sessionLoaded = false;
  private loadingInFlight = false;

  readonly sessionState = this._sessionState.asReadonly();
  readonly postboxes = this._postboxes.asReadonly();
  /** Flat folder lists keyed by postbox safe-id (the component builds the tree). */
  readonly folders = this._folders.asReadonly();

  constructor() {
    // Drop the cache when the signed-in user changes (incl. logout) — beA is per-user.
    let lastUser: string | null = null;
    effect(() => {
      const u = this.auth.user()?.username ?? null;
      if (u !== lastUser) { lastUser = u; this.clearCache(); }
    });
  }

  /**
   * Establishes the beA session once (login → postboxes → folders) and caches it. No-op if
   * already established (or connecting) unless `force` is set — the manual refresh passes force.
   */
  ensureSession(force = false): void {
    if (!force && (this.sessionLoaded || this.loadingInFlight)) { return; }
    this.loadingInFlight = true;
    this._sessionState.set('loading');
    this.login().subscribe({
      next: (boxes) => {
        this._postboxes.set(boxes);
        this._sessionState.set('ok');
        if (!boxes.length) {
          this._folders.set({});
          this.sessionLoaded = true;
          this.loadingInFlight = false;
          return;
        }
        const acc: Record<string, BeaFolder[]> = {};
        let pending = boxes.length;
        const done = () => {
          if (--pending === 0) { this._folders.set(acc); this.sessionLoaded = true; this.loadingInFlight = false; }
        };
        for (const pb of boxes) {
          this.listFolders(pb.safeId).subscribe({
            next: (flat) => { acc[pb.safeId] = flat; done(); },
            error: () => { acc[pb.safeId] = []; done(); },
          });
        }
      },
      error: () => { this._sessionState.set('error'); this.loadingInFlight = false; },
    });
  }

  /** Optimistic unread adjustment on a cached folder, so badges survive a view re-open. */
  adjustUnread(safeId: string, folderId: number, delta: number): void {
    this._folders.update((map) => {
      const list = map[safeId];
      if (!list) { return map; }
      return {
        ...map,
        [safeId]: list.map((f) => (f.id === folderId
          ? { ...f, unreadMessageCount: Math.max(0, f.unreadMessageCount + delta) } : f)),
      };
    });
  }

  private clearCache(): void {
    this._postboxes.set([]);
    this._folders.set({});
    this._sessionState.set('loading');
    this.sessionLoaded = false;
    this.loadingInFlight = false;
  }

  /**
   * Establishes the beA session (server reads the caller's stored certificate) and returns the
   * accessible postboxes. Fails (500) if beA is disabled or no certificate is configured.
   */
  login(): Observable<Postbox[]> {
    return this.http.post<LoginResultDto>(`${BEA_V8}/login`, null).pipe(
      map((r) => (r?.postboxes ?? []).map(toPostbox)),
    );
  }

  /** Folders of a postbox (flat; the component builds the tree). */
  listFolders(safeId: string): Observable<BeaFolder[]> {
    return this.http.get<FolderDto[]>(`${BEA_V8}/postboxes/${enc(safeId)}/folders`).pipe(
      map((rows) => (rows ?? []).map(toFolder)),
    );
  }

  /** Lists a folder's messages via the filter derived from the download restriction + search. */
  searchMessages(
    safeId: string, folderId: number, r: BeaRestriction, senderNameContains?: string,
  ): Observable<BeaMessageHeader[]> {
    const filter: Record<string, unknown> = {
      onlyNew: r.onlyNew,
      limit: r.limit,
      offset: 0,
      sortCriterion: 'RECEIVED',
      sortDirection: 'DESC',
    };
    if (r.sinceDays != null) {
      filter['receivedFrom'] = Date.now() - r.sinceDays * 86_400_000;
    }
    if (senderNameContains) {
      filter['senderNameContains'] = senderNameContains;
    }
    return this.http
      .post<HeaderDto[]>(`${BEA_V8}/postboxes/${enc(safeId)}/folders/${folderId}/messages/search`, filter)
      .pipe(map((rows) => (rows ?? []).map(toHeader)));
  }

  /** Full message including body, attachments (metadata + inline content) and journal. */
  getMessage(safeId: string, messageId: string, includeAttachments = true): Observable<BeaMessage> {
    return this.http
      .get<MessageDto>(`${BEA_V8}/postboxes/${enc(safeId)}/messages/${enc(messageId)}`, {
        params: { includeAttachments: String(includeAttachments) },
      })
      .pipe(map(toMessage));
  }

  /** Fetches one attachment's bytes (Base64) on demand. */
  getAttachment(safeId: string, messageId: string, attachmentName: string): Observable<BeaAttachment> {
    return this.http
      .get<AttachmentDto>(
        `${BEA_V8}/postboxes/${enc(safeId)}/messages/${enc(messageId)}/attachments/${enc(attachmentName)}`,
      )
      .pipe(map(toAttachment));
  }

  markRead(safeId: string, messageId: string): Observable<unknown> {
    return this.http.put(`${BEA_V8}/postboxes/${enc(safeId)}/messages/${enc(messageId)}/read`, null);
  }

  /** Moves the message to trash. */
  deleteMessage(safeId: string, messageId: string): Observable<unknown> {
    return this.http.delete(`${BEA_V8}/postboxes/${enc(safeId)}/messages/${enc(messageId)}`);
  }

  moveMessage(safeId: string, messageId: string, targetFolderId: number): Observable<unknown> {
    return this.http.put(
      `${BEA_V8}/postboxes/${enc(safeId)}/messages/${enc(messageId)}/move`, { targetFolderId },
    );
  }

  /** Runs signature verification for the message. */
  verify(safeId: string, messageId: string): Observable<BeaVerificationResult> {
    return this.http
      .get<VerificationDto>(`${BEA_V8}/postboxes/${enc(safeId)}/messages/${enc(messageId)}/verify`)
      .pipe(map((v) => ({ html: v?.html ?? '', xml: v?.xml ?? '', status: v?.status ?? '' })));
  }

  /** Exports the whole message as a `.bea` file (XML) for storing it in a case. */
  exportMessage(safeId: string, messageId: string): Observable<BeaExport> {
    return this.http
      .get<ExportDto>(`${BEA_V8}/postboxes/${enc(safeId)}/messages/${enc(messageId)}/export`)
      .pipe(map((e) => ({ fileName: e?.fileName ?? 'message.bea', contentBase64: e?.contentBase64 ?? '' })));
  }

  /** Server-computed case suggestions for an opened beA message; empty result on error. */
  caseSuggestions(req: {
    subject: string; body: string; referenceNumber: string; referenceJustice: string; senderName: string;
  }): Observable<CaseSuggestions> {
    return this.http.post<CaseSuggestions>(`${BEA_V8}/case-suggestions`, req).pipe(
      catchError(() => of({ suggestedCases: [], contacts: [], phoneNumbers: [], senderName: '', senderEmail: '' })),
    );
  }

  /** Searches the SAFE directory for recipient identities; [] on error. */
  searchIdentity(criteria: BeaIdentitySearchCriteria): Observable<BeaIdentity[]> {
    return this.http.post<IdentityDto[]>(`${BEA_V8}/identities/search`, criteria).pipe(
      map((rows) => (rows ?? []).map(toIdentity)),
      catchError(() => of([])),
    );
  }

  /** Looks up a single identity by Safe-ID (optionally constrained by ZIP). */
  getIdentity(safeId: string, zipCode?: string): Observable<BeaIdentity | null> {
    const url = `${BEA_V8}/identities/${enc(safeId)}${zipCode ? `?zipCode=${enc(zipCode)}` : ''}`;
    return this.http.get<IdentityDto>(url).pipe(map(toIdentity), catchError(() => of(null)));
  }

  private priorities$?: Observable<BeaListItem[]>;

  /** Message priorities (Nachrichtenpriorität); cached. */
  messagePriorities(): Observable<BeaListItem[]> {
    this.priorities$ ??= this.http.get<ListItemDto[]>(`${BEA_V8}/message-priorities`).pipe(
      map((rows) => (rows ?? []).map((r) => ({ code: r.code ?? '', name: r.name ?? '' }))),
      catchError(() => of([])),
      shareReplay(1),
    );
    return this.priorities$;
  }

  private legalAuthorities$?: Observable<BeaListItem[]>;

  /** Legal authorities (Gerichte/Justizbehörden) for the send form; cached. */
  legalAuthorities(): Observable<BeaListItem[]> {
    this.legalAuthorities$ ??= this.http.get<ListItemDto[]>(`${BEA_V8}/legal-authorities`).pipe(
      map((rows) => (rows ?? []).map((r) => ({ code: r.code ?? '', name: r.name ?? '' }))),
      catchError(() => of([])),
      shareReplay(1),
    );
    return this.legalAuthorities$;
  }

  /** Sends a beA message from the given postbox (one recipient per message). */
  sendMessage(safeId: string, req: BeaSendRequest): Observable<BeaMessage> {
    const body = {
      recipientSafeId: req.recipientSafeId,
      subject: req.subject,
      body: req.body,
      referenceNumber: req.referenceNumber,
      referenceJustice: req.referenceJustice,
      attachments: req.attachments.map((a) => ({ name: a.name, content: a.content })),
      legalAuthorityCode: req.legalAuthorityCode,
      priorityCode: req.priorityCode,
      eebRequested: req.eebRequested,
      confidential: req.confidential,
    };
    return this.http.post<MessageDto>(`${BEA_V8}/postboxes/${enc(safeId)}/messages`, body).pipe(map(toMessage));
  }
}

function toIdentity(dto: IdentityDto): BeaIdentity {
  const person = [dto.firstName, dto.surName].filter(Boolean).join(' ').trim();
  const place = [dto.zipCode, dto.city].filter(Boolean).join(' ').trim();
  const label = dto.displayName
    || [dto.officeName || dto.organization || person, place].filter(Boolean).join(', ')
    || dto.userName || dto.safeId || '';
  return {
    safeId: dto.safeId ?? '',
    firstName: dto.firstName ?? '',
    surName: dto.surName ?? '',
    userName: dto.userName ?? '',
    city: dto.city ?? '',
    zipCode: dto.zipCode ?? '',
    officeName: dto.officeName ?? '',
    organization: dto.organization ?? '',
    type: dto.type ?? '',
    displayName: label,
  };
}

function enc(seg: string): string {
  return encodeURIComponent(seg);
}

function toPostbox(dto: PostboxDto): Postbox {
  const name = [dto.surName, dto.firstName].filter(Boolean).join(', ');
  return {
    safeId: dto.safeId,
    label: dto.displayName || dto.officeName || name || dto.organization || dto.userName || dto.safeId,
    userName: dto.userName ?? '',
    chamber: dto.chamber ?? '',
  };
}

function toFolder(dto: FolderDto): BeaFolder {
  return {
    id: dto.id,
    parentId: dto.parentId ?? 0,
    name: dto.name ?? '',
    type: dto.type ?? 'CUSTOM',
    unreadMessageCount: dto.unreadMessageCount ?? 0,
    safeId: dto.safeId ?? '',
  };
}

function toHeader(dto: HeaderDto): BeaMessageHeader {
  return {
    id: dto.id,
    subject: dto.subject ?? '',
    sender: dto.sender ?? '',
    receptionTime: normalizeDate(dto.receptionTime),
    sentTime: normalizeDate(dto.sentTime),
    referenceJustice: dto.referenceJustice ?? '',
    referenceNumber: dto.referenceNumber ?? '',
    confidential: !!dto.confidential,
    signed: !!dto.signed,
    read: !!dto.read,
    recipientName: dto.recipientName ?? '',
    recipientSafeId: dto.recipientSafeId ?? '',
    folderId: dto.folderId ?? 0,
    postBoxSafeId: dto.postBoxSafeId ?? '',
  };
}

function toMessage(dto: MessageDto): BeaMessage {
  return {
    id: dto.id,
    subject: dto.subject ?? '',
    body: dto.body ?? '',
    receptionTime: normalizeDate(dto.receptionTime),
    createdTime: normalizeDate(dto.createdTime),
    senderSafeId: dto.senderSafeId ?? '',
    senderName: dto.senderName ?? '',
    referenceJustice: dto.referenceJustice ?? '',
    referenceNumber: dto.referenceNumber ?? '',
    confidential: !!dto.confidential,
    signed: !!dto.signed,
    read: !!dto.read,
    eebRequested: !!dto.eebRequested,
    eebId: dto.eebId ?? '',
    messageType: dto.messageType ?? '',
    recipients: (dto.recipients ?? []).map((r) => ({ name: r.name ?? '', safeId: r.safeId ?? '' })),
    attachments: (dto.attachments ?? []).map(toAttachment),
    vhnAttachments: (dto.vhnAttachments ?? []).map(toAttachment),
    journal: (dto.journal ?? []).map(toJournal),
    verificationHtml: dto.verificationHtml ?? '',
    verificationXml: dto.verificationXml ?? '',
    verificationStatus: dto.verificationStatus ?? '',
  };
}

function toAttachment(dto: AttachmentDto): BeaAttachment {
  return {
    name: dto.name ?? '',
    alias: dto.alias ?? '',
    type: dto.type ?? 10,
    size: dto.size ?? 0,
    content: dto.content ?? null,
    technicalAttachment: !!dto.technicalAttachment,
  };
}

function toJournal(dto: JournalDto): BeaJournalEntry {
  return {
    journalType: dto.journalType ?? '',
    eventType: dto.eventType ?? '',
    timestamp: normalizeDate(dto.timestamp),
    fromSurnameFirstname: dto.fromSurnameFirstname ?? '',
    fromUsername: dto.fromUsername ?? '',
    toSurnameFirstname: dto.toSurnameFirstname ?? '',
    attachmentReference: dto.attachmentReference ?? '',
  };
}

/** Jackson serializes java.util.Date as epoch millis; keep an ISO string (or null) for the UI. */
function normalizeDate(d: number | string | null | undefined): string | null {
  if (d == null || d === '') {
    return null;
  }
  if (typeof d === 'number') {
    return new Date(d).toISOString();
  }
  return String(d).replace(/\[.*\]$/, '');
}
