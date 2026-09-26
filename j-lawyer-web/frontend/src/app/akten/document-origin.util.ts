import { catchError, Observable, of, shareReplay, switchMap } from 'rxjs';
import { CORRESPONDENT_IN, CORRESPONDENT_OUT, DocCorrespondent, DocMetadataWrite } from './case.models';
import { CasesService } from './cases.service';

/**
 * Where a document saved from a message comes from — the web counterpart of the desktop
 * DocumentOrigin. Used to pre-fill the extended metadata (title, received date, correspondent,
 * parent) of documents created from an e-mail or a beA message.
 */
export interface DocumentOrigin {
  /** Title for the message document itself (e.g. the subject); attachments get no title. */
  title: string;
  /** Timestamp of the message (received or sent) as epoch ms; null when unknown. */
  date: number | null;
  /** Kind of the correspondent key, for the server-side contact lookup. */
  keyType: 'email' | 'safeid' | 'fax';
  /** E-mail address, SafeId or fax number of the other party; '' when unknown. */
  key: string;
  /** Display name of the other party as given by the message. */
  name: string;
  /** {@link CORRESPONDENT_IN} or {@link CORRESPONDENT_OUT}. */
  direction: number;
}

/** Splits an address like `"Max Muster" <max@example.org>` into address and display name. */
export function parseAddress(value: string | null | undefined): { address: string; name: string } {
  const s = (value ?? '').trim();
  const m = /^(.*?)\s*<([^>]+)>\s*$/.exec(s);
  if (m) {
    return { address: m[2].trim(), name: m[1].trim().replace(/^"(.*)"$/, '$1').trim() };
  }
  return { address: s, name: '' };
}

/**
 * The origin of an e-mail: outgoing (to the first recipient) when the sender is one of the own
 * addresses, else incoming (from the sender) — like the desktop MailDocumentOrigins.
 */
export function emailOrigin(subject: string, date: string | null, from: string, to: string[], ownAddresses: string[]): DocumentOrigin {
  const sender = parseAddress(from);
  const own = new Set(ownAddresses.map((a) => a.trim().toLowerCase()).filter(Boolean));
  const millis = date ? new Date(date).getTime() : NaN;
  const base = { title: subject ?? '', date: isNaN(millis) ? null : millis, keyType: 'email' as const };
  if (sender.address && own.has(sender.address.toLowerCase()) && to.length) {
    const rcpt = parseAddress(to[0]);
    const more = to.length - 1;
    const name = rcpt.name || rcpt.address;
    return { ...base, key: rcpt.address, name: more > 0 ? `${name} (+${more})` : name, direction: CORRESPONDENT_OUT };
  }
  return { ...base, key: sender.address, name: sender.name, direction: CORRESPONDENT_IN };
}

/**
 * The origin of a beA message: outgoing (to the first recipient) when it was sent from the own
 * postbox, else incoming (from the sender) — like the desktop beA inbox.
 */
export function beaOrigin(subject: string, receptionTime: string | null, senderSafeId: string, senderName: string,
                          recipients: { name: string; safeId: string }[], ownSafeId: string): DocumentOrigin {
  const millis = receptionTime ? new Date(receptionTime).getTime() : NaN;
  const base = { title: subject ?? '', date: isNaN(millis) ? null : millis, keyType: 'safeid' as const };
  if (ownSafeId && senderSafeId === ownSafeId && recipients.length) {
    const first = recipients[0];
    const more = recipients.length - 1;
    const name = first.name || first.safeId;
    return { ...base, key: first.safeId ?? '', name: more > 0 ? `${name} (+${more})` : name, direction: CORRESPONDENT_OUT };
  }
  return { ...base, key: senderSafeId ?? '', name: senderName ?? '', direction: CORRESPONDENT_IN };
}

/**
 * Resolves the correspondent of an origin once per case (case parties first, then all contacts)
 * and returns a function that writes the metadata of one saved document. The message document
 * gets the title; attachments get none but point to the message document as their parent.
 * Write errors are swallowed (e.g. servers without the v8 metadata endpoints).
 */
export function originWriter(cases: CasesService, caseId: string, origin: DocumentOrigin):
    (documentId: string, opts: { withTitle: boolean; parentId?: string | null }) => Observable<unknown> {
  const resolved$: Observable<DocCorrespondent> = (origin.key || origin.name)
    ? cases.resolveCorrespondent(caseId, origin.keyType, origin.key, origin.name, origin.direction).pipe(shareReplay(1))
    : of({ correspondentId: '', correspondentName: '', correspondentDirection: 0 });
  return (documentId, opts) => resolved$.pipe(
    switchMap((c) => {
      const data: DocMetadataWrite = {
        title: opts.withTitle && origin.title.trim() ? origin.title.trim() : null,
        keywords: [],
        // The message timestamp, also for outgoing messages (like the desktop client).
        receivedDate: origin.date,
        correspondentId: c.correspondentId || null,
        correspondentName: c.correspondentName || null,
        correspondentDirection: c.correspondentName ? (c.correspondentDirection || origin.direction) : 0,
        parentId: opts.parentId || null,
      };
      return cases.updateDocumentMetadataV8(documentId, data);
    }),
    // The document itself is saved; missing metadata must not turn the save into a failure.
    catchError(() => of(null)),
  );
}
