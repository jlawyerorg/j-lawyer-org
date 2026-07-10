import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { defer, finalize, map, Observable, tap } from 'rxjs';
import { API_ROOT } from '../core/api';
import { CalendarEvent, CalendarEventType, CalendarFilter, CalendarSetup, CalendarView, CaseRef, EventDraft, UserRef } from './calendar.models';
import { addDays, endOfMonth, isoDay, startOfDay, startOfMonth, startOfWeekMon } from './calendar.dates';

const CALENDAR_V8 = `${API_ROOT}/v8/calendar`;
const CALENDARS_V4 = `${API_ROOT}/v4/calendars`;
const DUEDATE_V6 = `${API_ROOT}/v6/cases/duedate`;
const CASES_V8 = `${API_ROOT}/v8/cases`;
const USERS_V6 = `${API_ROOT}/v6/security/users`;

/** Wire shape returned by j-lawyer-io (only the fields we consume). */
interface CalendarEventDto {
  id: string; type: CalendarEventType; summary: string; description: string; location: string;
  begin: number; end: number | null; done: boolean; assignee: string; reminderMinutes?: number;
  caseId: string; caseFileNumber: string; caseName: string;
  calendar?: string; calendarColor?: number; calendarName?: string;
}

/** Wire shape of GET /v4/calendars/list. */
interface CalendarSetupDto {
  id: string; displayName: string; eventType: string; background: number;
}

/** Wire shape of GET /v8/cases/page (only the fields the picker needs). */
interface CasePageDto {
  items: { id: string; fileNumber: string; name: string }[];
}

/** Wire shape of GET /v6/security/users (only the fields the assignee picker needs). */
interface UserDto {
  principalId: string; displayName: string;
}

/**
 * Calendar data access against the real REST API (GET /v8/calendar/events). Holds the current
 * view state (layout, anchor date, type filter, open/all status) and refetches whenever it
 * changes. The fetched date range follows the layout: the whole month grid for agenda/month,
 * the Mon–Sun week for week, a single day for day. The Bearer token is attached by authInterceptor.
 */
@Injectable({ providedIn: 'root' })
export class CalendarService {
  private readonly http = inject(HttpClient);

  readonly events = signal<CalendarEvent[]>([]);
  readonly loading = signal(false);
  readonly error = signal(false);
  /** The layout the calendar is currently shown in. */
  readonly view = signal<CalendarView>('agenda');
  /** Reference day the current view is centred on (local, midnight). */
  readonly anchor = signal<Date>(startOfDay(new Date()));
  readonly filter = signal<CalendarFilter>('all');
  /** When true, only open (not-done) entries are requested. */
  readonly openOnly = signal(false);

  /** Calendars the user can file entries into (loaded once, lazily). */
  readonly calendars = signal<CalendarSetup[]>([]);
  /** Login-enabled users offered as the entry's responsible person (loaded once, lazily). */
  readonly users = signal<UserRef[]>([]);
  /** True while a create/update/delete/toggle write is in flight. */
  readonly saving = signal(false);

  private requestSeq = 0;

  /** The [from, to] date range fetched for the current view + anchor. */
  range(): { from: Date; to: Date } {
    const a = this.anchor();
    switch (this.view()) {
      case 'day':
        return { from: startOfDay(a), to: startOfDay(a) };
      case 'week': {
        const from = startOfWeekMon(a);
        return { from, to: addDays(from, 6) };
      }
      case 'month': {
        // The month sheet shows a 6-week grid, so fetch the whole visible grid (incl. the
        // adjacent-month days) rather than just the 1st–last of the month.
        const from = startOfWeekMon(startOfMonth(a));
        return { from, to: addDays(from, 41) };
      }
      case 'agenda':
      default:
        return { from: startOfMonth(a), to: endOfMonth(a) };
    }
  }

  /** Loads the events for the current view/filter/status (also used for retry). */
  reload(): void {
    const { from, to } = this.range();
    this.loading.set(true);
    this.error.set(false);
    const seq = ++this.requestSeq;

    const params = new HttpParams()
      .set('from', isoDay(from))
      .set('to', isoDay(to))
      .set('type', this.filter())
      .set('status', this.openOnly() ? 'open' : 'all');

    this.http.get<CalendarEventDto[]>(`${CALENDAR_V8}/events`, { params }).subscribe({
      next: (rows) => {
        if (seq !== this.requestSeq) {
          return; // superseded
        }
        this.events.set((rows ?? []).map(toEvent).sort((a, b) => a.begin.getTime() - b.begin.getTime()));
        this.loading.set(false);
      },
      error: () => {
        if (seq !== this.requestSeq) {
          return;
        }
        this.error.set(true);
        this.loading.set(false);
      },
    });
  }

  /** Switches the layout (agenda/day/week/month) and refetches the matching range. */
  setView(view: CalendarView): void {
    if (this.view() === view) {
      return;
    }
    this.view.set(view);
    this.reload();
  }

  /** Moves the anchor by one unit of the current view (day/week/month) and reloads. */
  shift(direction: number): void {
    const a = this.anchor();
    switch (this.view()) {
      case 'day':
        this.anchor.set(addDays(a, direction));
        break;
      case 'week':
        this.anchor.set(addDays(a, direction * 7));
        break;
      default: // month + agenda step by whole months
        this.anchor.set(new Date(a.getFullYear(), a.getMonth() + direction, 1));
        break;
    }
    this.reload();
  }

  /** Jumps to today and reloads. */
  goToday(): void {
    this.anchor.set(startOfDay(new Date()));
    this.reload();
  }

  setFilter(filter: CalendarFilter): void {
    this.filter.set(filter);
    this.reload();
  }

  setOpenOnly(openOnly: boolean): void {
    this.openOnly.set(openOnly);
    this.reload();
  }

  /** Loads the list of calendars for the entry editor (once; no-op if already loaded). */
  loadCalendars(): void {
    if (this.calendars().length) {
      return;
    }
    this.http.get<CalendarSetupDto[]>(`${CALENDARS_V4}/list`).subscribe({
      next: (rows) => this.calendars.set((rows ?? []).map(toCalendarSetup)),
      error: () => this.calendars.set([]),
    });
  }

  /** Loads the login-enabled users for the assignee picker (once; no-op if already loaded). */
  loadUsers(): void {
    if (this.users().length) {
      return;
    }
    this.http.get<UserDto[]>(USERS_V6).subscribe({
      next: (rows) => this.users.set(
        (rows ?? [])
          .map((u) => ({ principalId: u.principalId, displayName: u.displayName || u.principalId }))
          .sort((a, b) => a.displayName.localeCompare(b.displayName)),
      ),
      error: () => this.users.set([]),
    });
  }

  /** Case search for the create dialog's picker (by file number / name / …). */
  searchCases(query: string): Observable<CaseRef[]> {
    const params = new HttpParams().set('q', query).set('offset', 0).set('limit', 8);
    return this.http.get<CasePageDto>(`${CASES_V8}/page`, { params }).pipe(
      map((page) => (page?.items ?? []).map((i) => ({ id: i.id, fileNumber: i.fileNumber, name: i.name }))),
    );
  }

  /**
   * Creates (no id) or updates (with id) a calendar entry via the v6 duedate endpoints, reloading
   * the current view on success. Both endpoints require the owning case and target calendar.
   * Returns a cold observable — the caller subscribes exactly once (see {@link toggleDone} for the
   * fire-and-forget variant).
   */
  save(draft: EventDraft): Observable<unknown> {
    const url = draft.id ? `${DUEDATE_V6}/update` : `${DUEDATE_V6}/create`;
    return defer(() => {
      this.saving.set(true);
      return this.http.put(url, toDueDatePayload(draft));
    }).pipe(
      tap(() => this.reload()),
      finalize(() => this.saving.set(false)),
    );
  }

  /** Deletes a calendar entry (DELETE /v8/calendar/events/{id}), reloading on success. */
  remove(id: string): Observable<unknown> {
    return defer(() => {
      this.saving.set(true);
      return this.http.delete(`${CALENDAR_V8}/events/${encodeURIComponent(id)}`);
    }).pipe(
      tap(() => this.reload()),
      finalize(() => this.saving.set(false)),
    );
  }

  /**
   * Flips an entry's done flag via update (there is no dedicated mark-done REST endpoint).
   * Subscribes internally so it can be called fire-and-forget from a template click handler.
   */
  toggleDone(ev: CalendarEvent): void {
    this.save({
      id: ev.id,
      caseId: ev.caseId,
      calendar: ev.calendarId,
      type: ev.type,
      summary: ev.summary,
      description: ev.description,
      location: ev.location,
      assignee: ev.assignee,
      begin: ev.begin.getTime(),
      end: (ev.end ?? ev.begin).getTime(),
      reminderMinutes: ev.reminderMinutes,
      done: !ev.done,
    }).subscribe({ error: () => undefined });
  }
}

function toCalendarSetup(dto: CalendarSetupDto): CalendarSetup {
  const t = (dto.eventType ?? '').toUpperCase();
  const type: CalendarEventType = t === 'EVENT' ? 'event' : t === 'RESPITE' ? 'respite' : 'followup';
  return { id: dto.id, displayName: dto.displayName ?? '', eventType: type, color: rgbHex(dto.background) };
}

/** Maps our draft to the RestfulDueDateV6 JSON shape (ISO dates, uppercase type). */
function toDueDatePayload(draft: EventDraft): Record<string, unknown> {
  const payload: Record<string, unknown> = {
    caseId: draft.caseId,
    calendar: draft.calendar,
    type: draft.type.toUpperCase(),
    summary: draft.summary,
    description: draft.description,
    location: draft.location,
    assignee: draft.assignee,
    beginDate: new Date(draft.begin).toISOString(),
    endDate: new Date(draft.end).toISOString(),
    reminderMinutes: draft.reminderMinutes,
    done: draft.done,
  };
  if (draft.id) {
    payload['id'] = draft.id;
  }
  return payload;
}

function toEvent(dto: CalendarEventDto): CalendarEvent {
  const begin = new Date(dto.begin);
  return {
    id: dto.id,
    type: dto.type,
    summary: dto.summary ?? '',
    description: dto.description ?? '',
    location: dto.location ?? '',
    begin,
    end: dto.end != null ? new Date(dto.end) : null,
    done: !!dto.done,
    assignee: dto.assignee ?? '',
    reminderMinutes: dto.reminderMinutes ?? -1,
    // Only appointments (Termin) carry a meaningful clock time; the others are all-day.
    timed: dto.type === 'event',
    caseId: dto.caseId ?? '',
    caseFileNumber: dto.caseFileNumber ?? '',
    caseName: dto.caseName ?? '',
    // Only trust the colour when a calendar is actually assigned — the int defaults to 0
    // (which would otherwise read as black) for entries without a calendar.
    color: dto.calendar ? rgbHex(dto.calendarColor) : '',
    calendarName: dto.calendarName ?? '',
    calendarId: dto.calendar ?? '',
  };
}

/**
 * Converts a packed-RGB int colour (the server's java.awt.Color(int) argument) to a CSS hex
 * string; only the low 24 bits are used (any alpha byte is ignored). '' for null/undefined.
 */
function rgbHex(value: number | undefined | null): string {
  if (value == null) { return ''; }
  return '#' + (value & 0xffffff).toString(16).padStart(6, '0');
}
