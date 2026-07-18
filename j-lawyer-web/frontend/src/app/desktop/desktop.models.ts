/**
 * "Mein Desktop" (dashboard) view models. The dashboard is a read-only overview assembled
 * from existing REST endpoints (recently changed cases, due deadlines/follow-ups, open invoices,
 * tagged cases/documents, messenger mentions, KPI/status strips) — mirroring the Swing DesktopPanel.
 * Which widgets are shown and their per-widget config are persisted per user (GET/PUT /v8/profile/dashboard).
 */

/** A recently changed case (from GET /v8/cases/page, sorted by dateChanged desc). */
export interface RecentCase {
  id: string;
  fileNumber: string;
  name: string;
  subjectField: string;
  lawyer: string;
  /** Responsible assistant (used for the user filter). */
  assistant: string;
  /** ISO date (sanitized). */
  lastChanged: string;
}

/** An open deadline / follow-up / appointment that is due (from GET /v8/calendar/events). */
export interface DueItem {
  id: string;
  type: 'event' | 'respite' | 'followup';
  summary: string;
  /** Begin moment (the due date/time). */
  due: Date;
  /** End moment, or null when the entry has no distinct end. */
  end: Date | null;
  overdue: boolean;
  done: boolean;
  assignee: string;
  caseId: string;
  caseFileNumber: string;
  caseName: string;
  // extra fields carried so a done/reschedule update can round-trip the whole entry
  description: string;
  location: string;
  reminderMinutes: number;
  calendarId: string;
}

/** An open invoice (from GET /v7/cases/invoices). */
export interface OpenInvoice {
  id: string;
  invoiceNumber: string;
  status: string;
  totalGross: number;
  currency: string;
  caseId: string;
  dueDate: string;
}

/** Aggregated open-invoice figures for the KPI row. */
export interface InvoiceSummary {
  count: number;
  totalGross: number;
  currency: string;
  top: OpenInvoice[];
}

/**
 * A tagged item shown in the "Nach Etikett" widget — either a case (the tag is on the case) or a
 * document (the tag is on a document). Navigation differs: a case opens /cases/:caseId, a document
 * opens its case with the document preselected (/cases/:caseId?doc=:id).
 */
export interface TaggedItem {
  kind: 'case' | 'doc';
  /** For a case: the case id. For a document: the document id. */
  id: string;
  /** The containing case id (equals id for kind==='case'). */
  caseId: string;
  primary: string;
  secondary: string;
  /** All of the item's tag (label) names, shown small on the row. */
  tags: string[];
  /** Responsible users (cases only; used for the user filter). */
  lawyer?: string;
  assistant?: string;
}

/** An open messenger mention addressed to the current user (from GET /v7/messages/since/{sec}). */
export interface MessageItem {
  id: string;
  mentionId: string;
  sender: string;
  content: string;
  sent: Date;
  caseId: string;
  caseFileNumber: string;
  caseName: string;
}

/** Case/contact counts for the KPI strip. */
export interface DashStats {
  casesTotal: number;
  casesOpen: number;
  casesArchived: number;
  contacts: number;
}

/** Quick-access unread/new counts; a value is null when unknown/unconfigured (chip hidden). */
export interface StatusCounts {
  mail: number | null;
  scans: number | null;
  bea: number | null;
}

/** A selectable user for the widget user filters (principalId + display name). */
export interface UserOption {
  id: string;
  name: string;
}

/** The dashboard widget ids (also the persisted visibility keys). */
export type DashWidgetId = 'stats' | 'status' | 'recent' | 'due' | 'invoices' | 'tagged' | 'messages';

/** Due event types (also the tab keys, plus 'all'). */
export type DueType = 'all' | 'event' | 'respite' | 'followup';

/** All widgets in display order (strips first, then grid cards). */
export const ALL_WIDGETS: DashWidgetId[] = ['stats', 'status', 'recent', 'due', 'invoices', 'tagged', 'messages'];

/** Day presets for the "Fällig" past/future dropdowns (match the Swing DesktopPanel). */
export const DUE_PAST_OPTIONS = [1, 3, 7, 14, 31, 180, 365];
export const DUE_FUTURE_OPTIONS = [0, 1, 3, 7, 14, 31];

/**
 * Per-user dashboard configuration, persisted server-side as an opaque JSON blob via
 * GET/PUT /v8/profile/dashboard. Owned by the web client; the server stores it verbatim.
 * The three user filters are multi-selects of principalIds; an empty list means "everyone".
 */
export interface DashboardConfig {
  widgets: DashWidgetId[];
  /** "Zuletzt geändert" user filter (principalIds; empty = all). */
  recentUsers: string[];
  /** "Fällig" window: days into the past / future. */
  dueSinceDays: number;
  dueInDays: number;
  /** "Fällig" active type tab. */
  dueType: DueType;
  /** "Fällig" user filter (assignee; principalIds; empty = all). */
  dueUsers: string[];
  /** Subscribed case tags shown in "Nach Etikett". */
  caseTags: string[];
  /** Subscribed document tags shown in "Nach Etikett". */
  docTags: string[];
  /** "Nach Etikett" user filter (applies to case items; principalIds; empty = all). */
  taggedUsers: string[];
  /** Currently shown tag tab: '' = all subscribed (union), else 'case:<tag>' | 'doc:<tag>'. */
  activeTag: string;
  /** Row limits per list widget. */
  limits: { recent: number; due: number; tagged: number; messages: number };
}

/** Defaults applied when the user has no stored config (all widgets visible, no tags subscribed). */
export const DEFAULT_DASHBOARD_CONFIG: DashboardConfig = {
  widgets: [...ALL_WIDGETS],
  recentUsers: [],
  dueSinceDays: 7,
  dueInDays: 14,
  dueType: 'all',
  dueUsers: [],
  caseTags: [],
  docTags: [],
  taggedUsers: [],
  activeTag: '',
  limits: { recent: 8, due: 10, tagged: 15, messages: 8 },
};

/** Merges a stored (possibly partial / older-schema) config onto the defaults. */
export function normalizeConfig(raw: Partial<DashboardConfig> | null | undefined): DashboardConfig {
  const d = DEFAULT_DASHBOARD_CONFIG;
  const r = raw ?? {};
  const widgets = Array.isArray(r.widgets) ? r.widgets.filter((w): w is DashWidgetId => ALL_WIDGETS.includes(w as DashWidgetId)) : d.widgets;
  const dueType: DueType = ['all', 'event', 'respite', 'followup'].includes(r.dueType as string) ? (r.dueType as DueType) : d.dueType;
  const strArr = (v: unknown): string[] => Array.isArray(v) ? v.filter((x): x is string => typeof x === 'string') : [];
  return {
    widgets: widgets.length ? widgets : [...d.widgets],
    recentUsers: strArr(r.recentUsers),
    dueSinceDays: numOrZero(r.dueSinceDays, d.dueSinceDays),
    dueInDays: numOrZero(r.dueInDays, d.dueInDays),
    dueType,
    dueUsers: strArr(r.dueUsers),
    caseTags: strArr(r.caseTags),
    docTags: strArr(r.docTags),
    taggedUsers: strArr(r.taggedUsers),
    activeTag: typeof r.activeTag === 'string' ? r.activeTag : d.activeTag,
    limits: {
      recent: numOr(r.limits?.recent, d.limits.recent),
      due: numOr(r.limits?.due, d.limits.due),
      tagged: numOr(r.limits?.tagged, d.limits.tagged),
      messages: numOr(r.limits?.messages, d.limits.messages),
    },
  };
}

function numOrZero(value: unknown, fallback: number): number {
  return typeof value === 'number' && Number.isFinite(value) && value >= 0 ? value : fallback;
}

function numOr(value: unknown, fallback: number): number {
  return typeof value === 'number' && Number.isFinite(value) && value > 0 ? value : fallback;
}
