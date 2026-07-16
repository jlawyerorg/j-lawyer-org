/**
 * Domain models for the beA module (besonderes elektronisches Anwaltspostfach). These mirror the
 * shared server DTOs in com.jdimension.jlawyer.services.bea.rest, served under
 * /j-lawyer-io/rest/v8/bea (BeaPostbox, BeaFolder, BeaMessageHeader, BeaMessage, BeaAttachment,
 * BeaMessageJournalEntry, BeaVerificationResult), reshaped for the UI. Read & triage scope — send,
 * eEB confirm/reject and save-to-case are a later phase. See bea.service.ts for the endpoint map.
 */

/** The logged-in user's own postbox (a beA identity keyed by safe-id). */
export interface Postbox {
  safeId: string;
  label: string;
  userName: string;
  chamber: string;
}

/** A folder within a postbox. `type` is INBOX | SENT | DRAFT | OUTBOX | TRASH | CUSTOM. */
export interface BeaFolder {
  id: number;
  parentId: number;
  name: string;
  type: string;
  unreadMessageCount: number;
  safeId: string;
}

/** A folder plus its children, ordered for display (well-known roles first). */
export interface BeaFolderNode extends BeaFolder {
  children: BeaFolderNode[];
  depth: number;
}

/** A message list row (header only). */
export interface BeaMessageHeader {
  id: string;
  subject: string;
  sender: string;
  receptionTime: string | null;
  sentTime: string | null;
  referenceJustice: string;
  referenceNumber: string;
  confidential: boolean;
  signed: boolean;
  read: boolean;
  recipientName: string;
  recipientSafeId: string;
  folderId: number;
  postBoxSafeId: string;
}

/** A full message (body, attachments, journal, verification), from the single-message GET. */
export interface BeaMessage {
  id: string;
  subject: string;
  body: string;
  receptionTime: string | null;
  createdTime: string | null;
  senderSafeId: string;
  senderName: string;
  referenceJustice: string;
  referenceNumber: string;
  confidential: boolean;
  signed: boolean;
  read: boolean;
  eebRequested: boolean;
  eebId: string;
  messageType: string;
  recipients: { name: string; safeId: string }[];
  attachments: BeaAttachment[];
  vhnAttachments: BeaAttachment[];
  journal: BeaJournalEntry[];
  verificationHtml: string;
  verificationXml: string;
  verificationStatus: string;
}

/** An attachment; `content` is Base64 (Jackson serializes byte[] so) and populated on demand. */
export interface BeaAttachment {
  name: string;
  alias: string;
  type: number;
  size: number;
  content: string | null;
  technicalAttachment: boolean;
}

/** One entry of the message journal (Nachrichtenjournal). */
export interface BeaJournalEntry {
  journalType: string;
  eventType: string;
  timestamp: string | null;
  fromSurnameFirstname: string;
  fromUsername: string;
  toSurnameFirstname: string;
  attachmentReference: string;
}

/** Signature-verification outcome (STATUS_SUCCESS | STATUS_PARTIAL | STATUS_FAILED). */
export interface BeaVerificationResult {
  html: string;
  xml: string;
  status: string;
}

/**
 * How much of a folder to load — mirrors the Swing client's CONF_BEA_DOWNLOADRESTRICTION combo.
 * Translated into a BeaMessageFilter: `limit` caps the count; `sinceDays` sets receivedFrom;
 * `onlyNew` filters to unread. Sort is always RECEIVED / DESC server-side.
 */
export interface BeaRestriction {
  id: string;
  labelKey: string;
  limit: number;
  sinceDays: number | null;
  onlyNew: boolean;
}

export const BEA_RESTRICTIONS: BeaRestriction[] = [
  { id: 'last50', labelKey: 'bea.restriction.last50', limit: 50, sinceDays: null, onlyNew: false },
  { id: 'last20', labelKey: 'bea.restriction.last20', limit: 20, sinceDays: null, onlyNew: false },
  { id: 'last100', labelKey: 'bea.restriction.last100', limit: 100, sinceDays: null, onlyNew: false },
  { id: 'last500', labelKey: 'bea.restriction.last500', limit: 500, sinceDays: null, onlyNew: false },
  { id: 'unread', labelKey: 'bea.restriction.unread', limit: 200, sinceDays: null, onlyNew: true },
  { id: 'week', labelKey: 'bea.restriction.week', limit: 500, sinceDays: 7, onlyNew: false },
  { id: 'twoWeeks', labelKey: 'bea.restriction.twoWeeks', limit: 500, sinceDays: 14, onlyNew: false },
  { id: 'month', labelKey: 'bea.restriction.month', limit: 500, sinceDays: 31, onlyNew: false },
  { id: 'threeMonths', labelKey: 'bea.restriction.threeMonths', limit: 500, sinceDays: 93, onlyNew: false },
  { id: 'year', labelKey: 'bea.restriction.year', limit: 500, sinceDays: 366, onlyNew: false },
];

/** A beA identity (recipient) from the SAFE directory search or a Safe-ID lookup. */
export interface BeaIdentity {
  safeId: string;
  firstName: string;
  surName: string;
  userName: string;
  city: string;
  zipCode: string;
  officeName: string;
  organization: string;
  type: string;
  /** A readable one-line label derived from the available fields. */
  displayName: string;
}

/** Criteria for the SAFE directory search (all optional; at least one should be set). */
export interface BeaIdentitySearchCriteria {
  firstName?: string;
  surName?: string;
  userName?: string;
  city?: string;
  zipCode?: string;
  officeName?: string;
}

/** A code/name pair (message priority, legal authority). */
export interface BeaListItem {
  code: string;
  name: string;
}

/** A beA message exported as a `.bea` file (whole message serialized to XML). */
export interface BeaExport {
  fileName: string;
  contentBase64: string;
}

/** Payload to send a beA message (one recipient per message). */
export interface BeaSendRequest {
  recipientSafeId: string;
  subject: string;
  body: string;
  referenceNumber: string;
  referenceJustice: string;
  attachments: { name: string; content: string }[];
  legalAuthorityCode: string;
  priorityCode: string;
  eebRequested: boolean;
  confidential: boolean;
}

/** Composer mode: a new message, a reply to the sender, or a forward carrying the attachments. */
export type BeaComposeMode = 'new' | 'reply' | 'forward';

/** Order well-known folders first (inbox, drafts, outbox, sent), trash last, custom in between. */
export function beaFolderOrder(type: string): number {
  switch (type) {
    case 'INBOX': return 0;
    case 'DRAFT': return 1;
    case 'OUTBOX': return 2;
    case 'SENT': return 3;
    case 'TRASH': return 9;
    default: return 5;
  }
}
