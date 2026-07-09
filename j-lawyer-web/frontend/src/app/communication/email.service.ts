import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { API_ROOT } from '../core/api';
import { MailAttachment, Mailbox, MailFolder, MailMessage } from './email.models';

const EMAIL_V7 = `${API_ROOT}/v7/email`;

interface MailboxDto { id: string; displayName?: string; emailAddress?: string; type?: string; }
interface FolderDto {
  folderId: string; parentFolderId?: string; displayName?: string; wellKnownName?: string;
  unreadCount?: number; totalCount?: number;
}
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

  /** Mailboxes the caller may access. */
  listMailboxes(): Observable<Mailbox[]> {
    return this.http.get<MailboxDto[]>(`${EMAIL_V7}/mailboxes`).pipe(
      map((rows) => (rows ?? []).map(toMailbox)),
    );
  }

  /** Folders of a mailbox (flat; the component builds the tree). */
  listFolders(mailboxId: string): Observable<MailFolder[]> {
    return this.http.get<FolderDto[]>(`${EMAIL_V7}/mailboxes/${enc(mailboxId)}/folders`).pipe(
      map((rows) => (rows ?? []).map(toFolder)),
    );
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
