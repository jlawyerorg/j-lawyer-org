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

/**
 * How much of a folder to download — mirrors the Swing client's LoadFolderRestriction combo.
 * `top` caps the count; `sinceDays` limits how far back (null = no limit); `unreadOnly` filters.
 */
export interface LoadRestriction {
  id: string;
  labelKey: string;
  top: number;
  sinceDays: number | null;
  unreadOnly: boolean;
}

export const LOAD_RESTRICTIONS: LoadRestriction[] = [
  { id: 'last50', labelKey: 'email.restriction.last50', top: 50, sinceDays: null, unreadOnly: false },
  { id: 'last20', labelKey: 'email.restriction.last20', top: 20, sinceDays: null, unreadOnly: false },
  { id: 'last100', labelKey: 'email.restriction.last100', top: 100, sinceDays: null, unreadOnly: false },
  { id: 'last500', labelKey: 'email.restriction.last500', top: 500, sinceDays: null, unreadOnly: false },
  { id: 'unread', labelKey: 'email.restriction.unread', top: 200, sinceDays: null, unreadOnly: true },
  { id: 'week', labelKey: 'email.restriction.week', top: 500, sinceDays: 7, unreadOnly: false },
  { id: 'twoWeeks', labelKey: 'email.restriction.twoWeeks', top: 500, sinceDays: 14, unreadOnly: false },
  { id: 'month', labelKey: 'email.restriction.month', top: 500, sinceDays: 31, unreadOnly: false },
  { id: 'threeMonths', labelKey: 'email.restriction.threeMonths', top: 500, sinceDays: 93, unreadOnly: false },
  { id: 'year', labelKey: 'email.restriction.year', top: 500, sinceDays: 366, unreadOnly: false },
];

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
