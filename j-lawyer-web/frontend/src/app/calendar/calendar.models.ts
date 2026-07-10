/**
 * Calendar (Kalender / Fristen & Wiedervorlagen) domain models, aligned with the v8 REST DTO:
 *  - list: GET /rest/v8/calendar/events?from&to&type&status -> RestfulCalendarEventV8[]
 *
 * Entries are the case events stored server-side (ArchiveFileReviewsBean): appointments
 * (Termin), deadlines (Frist) and follow-ups (Wiedervorlage). Dates arrive as epoch millis.
 */

/** Entry kind — matches the REST `type` string. */
export type CalendarEventType = 'event' | 'respite' | 'followup';

/** Type filter for the agenda (adds `all`). */
export type CalendarFilter = 'all' | 'event' | 'respite' | 'followup';

/** How the calendar is laid out: the agenda list or a day/week/month sheet. */
export type CalendarView = 'agenda' | 'day' | 'week' | 'month';

/** A single calendar entry shaped for display. */
export interface CalendarEvent {
  id: string;
  type: CalendarEventType;
  summary: string;
  description: string;
  location: string;
  /** Start as a JS Date (parsed from epoch millis). */
  begin: Date;
  /** End as a JS Date, or null when the entry has no end time. */
  end: Date | null;
  done: boolean;
  assignee: string;
  /** Reminder lead time in minutes; -1 = no reminder (matches the desktop default). */
  reminderMinutes: number;
  /** True for appointments (Termin), which carry a clock time; deadlines/follow-ups are all-day. */
  timed: boolean;
  caseId: string;
  caseFileNumber: string;
  caseName: string;
  /**
   * Colour of the calendar this entry belongs to, as a CSS hex string (e.g. "#de303b"),
   * converted from the server's packed-RGB int. Empty when the entry has no calendar; the view
   * then falls back to a type-based colour.
   */
  color: string;
  /** Display name of that calendar; empty when none. Used as a tooltip. */
  calendarName: string;
  /** Id of the calendar this entry belongs to; empty when none. Needed to edit/update the entry. */
  calendarId: string;
}

/**
 * A calendar the user can file entries into (GET /v4/calendars/list). Each calendar is typed:
 * its {@link eventType} determines the kind of entry it holds (and whether entries are timed).
 */
export interface CalendarSetup {
  id: string;
  displayName: string;
  /** The single entry kind this calendar holds — drives the new entry's type. */
  eventType: CalendarEventType;
  /** CSS hex colour derived from the server's packed-RGB int; '' when unset. */
  color: string;
}

/** A minimal case reference for the create dialog's case picker. */
export interface CaseRef {
  id: string;
  fileNumber: string;
  name: string;
}

/** A user that can be assigned responsibility for an entry (login-enabled users). */
export interface UserRef {
  /** Login/principal id — stored as the entry's assignee. */
  principalId: string;
  displayName: string;
}

/**
 * The editable shape of a calendar entry, sent to the write endpoints
 * (PUT /v6/cases/duedate/{create,update}). {@link id} is absent when creating.
 */
export interface EventDraft {
  id?: string;
  caseId: string;
  /** Calendar setup id the entry is filed into. */
  calendar: string;
  /** REST type token — 'event' | 'respite' | 'followup' (uppercased on the wire). */
  type: CalendarEventType;
  summary: string;
  description: string;
  location: string;
  assignee: string;
  /** Start as epoch millis. */
  begin: number;
  /** End as epoch millis (equals begin for all-day entries). */
  end: number;
  reminderMinutes: number;
  done: boolean;
}

/** Events belonging to one calendar day, for the agenda grouping. */
export interface CalendarDay {
  /** Local ISO day key (yyyy-MM-dd) used as the group id. */
  key: string;
  date: Date;
  isToday: boolean;
  events: CalendarEvent[];
}
