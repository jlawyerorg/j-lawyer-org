/**
 * Domain models for the E-Mail module. These mirror the v7 REST DTOs served under
 * /j-lawyer-io/rest/v7/email (RestfulMailboxV7, RestfulMailFolderV7, RestfulMailMessageV7,
 * RestfulMailAttachmentV7), reshaped for the UI. Read & triage only — compose/send is a
 * later phase. See email.service.ts for the endpoint mapping.
 */

/** A configured mailbox (IMAP/SMTP or Microsoft Exchange), owned by the logged-in user. */
export interface Mailbox {
  id: string;
  displayName: string;
  emailAddress: string;
  /** 'exchange' | 'imap' — informational only. */
  type: string;
}

/** A single mail folder as returned by the server (flat; parentFolderId builds the tree). */
export interface MailFolder {
  folderId: string;
  parentFolderId: string | null;
  displayName: string;
  /** 'inbox' | 'sentitems' | 'deleteditems' | 'drafts' | '' — well-known role, if any. */
  wellKnownName: string;
  unreadCount: number;
  totalCount: number;
}

/** A folder plus its children, ordered for display (well-known roles first). */
export interface FolderNode extends MailFolder {
  children: FolderNode[];
  depth: number;
}

/** A message list row / full message. `body`/`attachments` are populated by the single-message GET. */
export interface MailMessage {
  messageRef: string;
  messageId: string;
  subject: string;
  from: string;
  to: string[];
  cc: string[];
  date: string | null;
  read: boolean;
  hasAttachments: boolean;
  body: string;
  /** 'text/plain' | 'text/html'. */
  bodyContentType: string;
  inReplyTo: string;
  references: string;
  readReceiptRequested: boolean;
  attachments: MailAttachment[];
}

/** Attachment metadata; `contentBase64` is only present for inline parts and lazy fetches. */
export interface MailAttachment {
  attachmentId: string;
  name: string;
  contentType: string;
  size: number;
  inline: boolean;
  contentId: string;
  contentBase64: string | null;
}

/** Message-list scope toggle: all messages, or unread only (mirrors the desktop "unread" filter). */
export type MailScope = 'all' | 'unread';

/**
 * Optional "how far back" filter for the message list. `sinceDays` null = no limit. Replaces the
 * old count-capped presets ("Letzte 20/50/…") now that the list pages in via infinite scroll.
 */
export interface TimeRange {
  id: string;
  labelKey: string;
  sinceDays: number | null;
}

export const TIME_RANGES: TimeRange[] = [
  { id: 'any', labelKey: 'email.range.any', sinceDays: null },
  { id: 'week', labelKey: 'email.range.week', sinceDays: 7 },
  { id: 'twoWeeks', labelKey: 'email.range.twoWeeks', sinceDays: 14 },
  { id: 'month', labelKey: 'email.range.month', sinceDays: 31 },
  { id: 'threeMonths', labelKey: 'email.range.threeMonths', sinceDays: 93 },
  { id: 'year', labelKey: 'email.range.year', sinceDays: 366 },
];

/** Messages fetched per infinite-scroll page (server `top`; `offset` advances by this each page). */
export const PAGE_SIZE = 50;

/** How the composer was opened, driving prefill (recipients, subject, quoted body, threading). */
export type ComposeMode = 'new' | 'reply' | 'replyAll' | 'forward';

/** An attachment to send: raw bytes as Base64 (no `data:` prefix), mirroring RestfulMailAttachmentV7. */
export interface ComposeAttachment {
  name: string;
  contentType: string;
  size: number;
  contentBase64: string;
  inline: boolean;
  contentId: string;
}

/** A case suggested for an opened email; `source` says why it was suggested. */
export interface CaseSuggestion {
  id: string;
  fileNumber: string;
  name: string;
  reason: string;
  archived: boolean;
  /** 'subjectBody' | 'reference' | 'sender'. */
  source: string;
}

/** A contact matching the sender of an opened email. */
export interface SuggestedContact {
  id: string;
  displayName: string;
  email: string;
}

/** Consolidated case suggestions returned by POST /v7/email/mailboxes/{id}/case-suggestions. */
export interface CaseSuggestions {
  suggestedCases: CaseSuggestion[];
  contacts: SuggestedContact[];
  phoneNumbers: string[];
  senderName: string;
  senderEmail: string;
}

/** Payload for POST /v7/email/mailboxes/{id}/send. `to`/`cc`/`bcc` are comma-separated address lists. */
export interface SendMailRequest {
  to: string;
  cc: string;
  bcc: string;
  subject: string;
  body: string;
  /** 'text/plain' | 'text/html'. */
  contentType: string;
  attachments: ComposeAttachment[];
  /** '' | 'high' | 'low'. */
  priority: string;
  readReceipt: boolean;
  inReplyTo: string;
  references: string;
}

/** Order well-known folders first (inbox, drafts, sent), trash last, others alphabetical. */
export function wellKnownOrder(wellKnownName: string): number {
  switch (wellKnownName) {
    case 'inbox': return 0;
    case 'drafts': return 1;
    case 'sentitems': return 2;
    case 'deleteditems': return 9;
    default: return 5;
  }
}
