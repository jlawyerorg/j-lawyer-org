import { HttpClient, HttpParams } from '@angular/common/http';
import { effect, inject, Injectable, signal } from '@angular/core';
import { catchError, from, map, mergeMap, Observable, of } from 'rxjs';
import { API_ROOT } from '../core/api';
import { AuthService } from '../core/auth/auth.service';
import { MailAttachment, Mailbox, MailFolder, MailMessage, SendMailRequest } from './email.models';

const EMAIL_V7 = `${API_ROOT}/v7/email`;
/** Sentinel for a not-yet-loaded folder count (server returns -1 when includeCounts=false). */
const COUNT_UNKNOWN = -1;
/** Concurrency for the background per-folder count fetches (one IMAP SELECT each). */
const COUNTS_CONCURRENCY = 4;

interface MailboxDto { id: string; displayName?: string; emailAddress?: string; type?: string; }
interface FolderDto {
  folderId: string; parentFolderId?: string; displayName?: string; wellKnownName?: string;
  unreadCount?: number; totalCount?: number;
}
interface FolderCountsDto { folderId: string; unreadCount?: number; totalCount?: number; }
interface AttachmentDto {
  attachmentId: string; name?: string; contentType?: string; size?: number;
  inline?: boolean; contentId?: string; contentBase64?: string | null;
}
interface MessageDto {
  messageRef: string; messageId?: string; subject?: string; from?: string; to?: string[]; cc?: string[];
  date?: number | string | null; read?: boolean; hasAttachments?: boolean; body?: string;
  bodyContentType?: string; inReplyTo?: string; references?: string; readReceiptRequested?: boolean;
  attachments?: AttachmentDto[];
}

/**
 * E-Mail data access against the v7 REST API (/v7/email/**). All calls are Basic-auth-restricted
 * server-side (loginRole) and additionally ownership-checked per mailbox; the Bearer token is
 * attached by authInterceptor. Read & triage surface only (list/read/mark/move/delete/download).
 */
@Injectable({ providedIn: 'root' })
export class EmailService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);

  // Cached mailbox/folder structure, held for the whole app session so re-opening the module
  // is instant. Loaded once (or on manual refresh); the message list is always fetched fresh.
  private readonly _mailboxes = signal<Mailbox[] | null>(null);
  private readonly _folders = signal<Record<string, MailFolder[]>>({});
  private readonly _structureLoading = signal(false);
  private readonly _structureError = signal(false);
  private structureLoaded = false;

  readonly mailboxes = this._mailboxes.asReadonly();
  /** Flat folder lists keyed by mailbox id (the component builds the tree). */
  readonly folders = this._folders.asReadonly();
  readonly structureLoading = this._structureLoading.asReadonly();
  readonly structureError = this._structureError.asReadonly();

  constructor() {
    // Drop the cache when the signed-in user changes (incl. logout), so a different user never
    // sees the previous user's mailboxes.
    let lastUser: string | null = null;
    effect(() => {
      const u = this.auth.user()?.username ?? null;
      if (u !== lastUser) { lastUser = u; this.clearCache(); }
    });
  }

  /**
   * Loads mailboxes + all their folders once and caches them. No-op if already cached (or
   * loading) unless `force` is set — the manual refresh passes force=true.
   */
  ensureStructure(force = false): void {
    if ((this.structureLoaded || this._structureLoading()) && !force) { return; }
    this._structureLoading.set(true);
    this._structureError.set(false);
    this.listMailboxes().subscribe({
      next: (boxes) => {
        this._mailboxes.set(boxes);
        if (!boxes.length) {
          this._folders.set({});
          this._structureLoading.set(false);
          this.structureLoaded = true;
          return;
        }
        const acc: Record<string, MailFolder[]> = {};
        let pending = boxes.length;
        const done = () => {
          if (--pending === 0) {
            this._folders.set(acc);
            this._structureLoading.set(false);
            this.structureLoaded = true;
          }
        };
        for (const mb of boxes) {
          this.listFolders(mb.id).subscribe({
            next: (flat) => { acc[mb.id] = flat; done(); this.loadCounts(mb.id, flat); },
            error: () => { acc[mb.id] = []; done(); },
          });
        }
      },
      error: () => { this._structureError.set(true); this._structureLoading.set(false); },
    });
  }

  /** Optimistic unread adjustment on a cached folder, so badges survive a view re-open. */
  adjustUnread(mailboxId: string, folderId: string, delta: number): void {
    this._folders.update((map) => {
      const list = map[mailboxId];
      if (!list) { return map; }
      return {
        ...map,
        [mailboxId]: list.map((f) => (f.folderId === folderId
          ? { ...f, unreadCount: Math.max(0, f.unreadCount + delta) } : f)),
      };
    });
  }

  private clearCache(): void {
    this._mailboxes.set(null);
    this._folders.set({});
    this._structureError.set(false);
    this.structureLoaded = false;
  }

  /** Mailboxes the caller may access. */
  listMailboxes(): Observable<Mailbox[]> {
    return this.http.get<MailboxDto[]>(`${EMAIL_V7}/mailboxes`).pipe(
      map((rows) => (rows ?? []).map(toMailbox)),
    );
  }

  /**
   * Folders of a mailbox (flat; the component builds the tree). Loads WITHOUT hidden folders and
   * WITHOUT counts — the countless call avoids one IMAP SELECT per folder, so the tree renders fast;
   * counts are then filled in lazily by {@link loadCounts}.
   */
  listFolders(mailboxId: string): Observable<MailFolder[]> {
    const params = new HttpParams().set('includeHidden', 'false').set('includeCounts', 'false');
    return this.http.get<FolderDto[]>(`${EMAIL_V7}/mailboxes/${enc(mailboxId)}/folders`, { params }).pipe(
      map((rows) => (rows ?? []).map(toFolder)),
    );
  }

  /** Background-fetches per-folder unread/total counts (bounded concurrency) and patches the cache. */
  private loadCounts(mailboxId: string, folders: MailFolder[]): void {
    const pending = folders.filter((f) => f.unreadCount === COUNT_UNKNOWN || f.totalCount === COUNT_UNKNOWN);
    if (!pending.length) { return; }
    from(pending).pipe(
      mergeMap((f) => this.http
        .get<FolderCountsDto>(`${EMAIL_V7}/mailboxes/${enc(mailboxId)}/folders/${enc(f.folderId)}/counts`)
        .pipe(catchError(() => of(null))), COUNTS_CONCURRENCY),
    ).subscribe((c) => {
      if (c) { this.patchCounts(mailboxId, c.folderId, c.unreadCount ?? 0, c.totalCount ?? 0); }
    });
  }

  private patchCounts(mailboxId: string, folderId: string, unread: number, total: number): void {
    this._folders.update((map) => {
      const list = map[mailboxId];
      if (!list) { return map; }
      return {
        ...map,
        [mailboxId]: list.map((f) => (f.folderId === folderId ? { ...f, unreadCount: unread, totalCount: total } : f)),
      };
    });
  }

  /** The folders the user hid for a mailbox (for an "unhide" management UI). */
  hiddenFolders(mailboxId: string): Observable<MailFolder[]> {
    return this.http.get<FolderDto[]>(`${EMAIL_V7}/mailboxes/${enc(mailboxId)}/hidden-folders`).pipe(
      map((rows) => (rows ?? []).map(toFolder)),
    );
  }

  /** Hides/unhides a folder, then reloads that mailbox's folder list (+counts) to reflect it. */
  setFolderHidden(mailboxId: string, folderId: string, hidden: boolean): Observable<unknown> {
    return this.http.put(`${EMAIL_V7}/mailboxes/${enc(mailboxId)}/folders/${enc(folderId)}/hidden`, { hidden });
  }

  /** Re-fetches one mailbox's folder list (used after hide/unhide). */
  refreshFolders(mailboxId: string): void {
    this.listFolders(mailboxId).subscribe({
      next: (flat) => {
        this._folders.update((map) => ({ ...map, [mailboxId]: flat }));
        this.loadCounts(mailboxId, flat);
      },
    });
  }

  /**
   * Lists messages in a folder. `sinceDate` is an ISO day (yyyy-MM-dd) or empty; `search` matches
   * subject/from/to/body server-side. Bodies/attachments are not populated here (see getMessage).
   */
  listMessages(
    mailboxId: string, folderId: string,
    opts: { top: number; offset?: number; sinceDate?: string; unreadOnly?: boolean; search?: string },
  ): Observable<MailMessage[]> {
    let params = new HttpParams()
      .set('top', String(opts.top))
      .set('offset', String(opts.offset ?? 0))
      .set('unreadOnly', String(!!opts.unreadOnly));
    if (opts.sinceDate) {
      params = params.set('sinceDate', opts.sinceDate);
    }
    if (opts.search) {
      params = params.set('search', opts.search);
    }
    return this.http
      .get<MessageDto[]>(`${EMAIL_V7}/mailboxes/${enc(mailboxId)}/folders/${enc(folderId)}/messages`, { params })
      .pipe(map((rows) => (rows ?? []).map(toMessage)));
  }

  /** Full message including body and (optionally) attachment metadata + inline content. */
  getMessage(mailboxId: string, messageRef: string, includeAttachments = true): Observable<MailMessage> {
    const params = new HttpParams().set('includeAttachments', String(includeAttachments));
    return this.http
      .get<MessageDto>(`${EMAIL_V7}/mailboxes/${enc(mailboxId)}/messages/${enc(messageRef)}`, { params })
      .pipe(map(toMessage));
  }

  /** Fetches a single attachment's bytes (Base64) on demand. */
  getAttachment(mailboxId: string, messageRef: string, attachmentId: string): Observable<MailAttachment> {
    return this.http
      .get<AttachmentDto>(
        `${EMAIL_V7}/mailboxes/${enc(mailboxId)}/messages/${enc(messageRef)}/attachments/${enc(attachmentId)}`,
      )
      .pipe(map(toAttachment));
  }

  /** Downloads the full message as an RFC-822 .eml (binary). */
  getEml(mailboxId: string, messageRef: string): Observable<Blob> {
    return this.http.get(`${EMAIL_V7}/mailboxes/${enc(mailboxId)}/messages/${enc(messageRef)}/eml`, {
      responseType: 'blob',
    });
  }

  markAsRead(mailboxId: string, messageRef: string, read: boolean): Observable<unknown> {
    const params = new HttpParams().set('read', String(read));
    return this.http.put(
      `${EMAIL_V7}/mailboxes/${enc(mailboxId)}/messages/${enc(messageRef)}/read`, null, { params },
    );
  }

  moveMessage(mailboxId: string, messageRef: string, targetFolderId: string): Observable<unknown> {
    const params = new HttpParams().set('targetFolderId', targetFolderId);
    return this.http.put(
      `${EMAIL_V7}/mailboxes/${enc(mailboxId)}/messages/${enc(messageRef)}/move`, null, { params },
    );
  }

  deleteMessage(mailboxId: string, messageRef: string): Observable<unknown> {
    return this.http.delete(`${EMAIL_V7}/mailboxes/${enc(mailboxId)}/messages/${enc(messageRef)}`);
  }

  /**
   * Sends a message through the mailbox's configured backend (SMTP or Graph). The server also
   * copies the sent message into the mailbox's Sent folder, so no separate append is needed.
   */
  sendMail(mailboxId: string, req: SendMailRequest): Observable<unknown> {
    return this.http.post(`${EMAIL_V7}/mailboxes/${enc(mailboxId)}/send`, req);
  }
}

/** messageRef/folderId can contain slashes and other unsafe chars — encode per path segment. */
function enc(seg: string): string {
  return encodeURIComponent(seg);
}

function toMailbox(dto: MailboxDto): Mailbox {
  return {
    id: dto.id,
    displayName: dto.displayName ?? dto.emailAddress ?? '',
    emailAddress: dto.emailAddress ?? '',
    type: dto.type ?? 'imap',
  };
}

function toFolder(dto: FolderDto): MailFolder {
  return {
    folderId: dto.folderId,
    parentFolderId: dto.parentFolderId || null,
    displayName: dto.displayName ?? '',
    wellKnownName: dto.wellKnownName ?? '',
    unreadCount: dto.unreadCount ?? 0,
    totalCount: dto.totalCount ?? 0,
  };
}

function toMessage(dto: MessageDto): MailMessage {
  return {
    messageRef: dto.messageRef,
    messageId: dto.messageId ?? '',
    subject: dto.subject ?? '',
    from: dto.from ?? '',
    to: dto.to ?? [],
    cc: dto.cc ?? [],
    date: normalizeDate(dto.date),
    read: !!dto.read,
    hasAttachments: !!dto.hasAttachments,
    body: dto.body ?? '',
    bodyContentType: dto.bodyContentType ?? 'text/plain',
    inReplyTo: dto.inReplyTo ?? '',
    references: dto.references ?? '',
    readReceiptRequested: !!dto.readReceiptRequested,
    attachments: (dto.attachments ?? []).map(toAttachment),
  };
}

function toAttachment(dto: AttachmentDto): MailAttachment {
  return {
    attachmentId: dto.attachmentId,
    name: dto.name ?? '',
    contentType: dto.contentType ?? 'application/octet-stream',
    size: dto.size ?? 0,
    inline: !!dto.inline,
    contentId: dto.contentId ?? '',
    contentBase64: dto.contentBase64 ?? null,
  };
}

/** JAX-RS serializes java.util.Date as an epoch-millis number; keep it as an ISO string for the UI. */
function normalizeDate(d: number | string | null | undefined): string | null {
  if (d == null || d === '') {
    return null;
  }
  if (typeof d === 'number') {
    return new Date(d).toISOString();
  }
  // Strip a Java ZonedDateTime zone suffix like "…[UTC]" if present.
  return String(d).replace(/\[.*\]$/, '');
}
