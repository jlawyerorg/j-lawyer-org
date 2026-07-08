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
  /** True for appointments (Termin), which carry a clock time; deadlines/follow-ups are all-day. */
  timed: boolean;
  caseId: string;
  caseFileNumber: string;
  caseName: string;
}

/** Events belonging to one calendar day, for the agenda grouping. */
export interface CalendarDay {
  /** Local ISO day key (yyyy-MM-dd) used as the group id. */
  key: string;
  date: Date;
  isToday: boolean;
  events: CalendarEvent[];
}
