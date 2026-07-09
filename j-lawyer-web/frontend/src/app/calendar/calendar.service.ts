import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { API_ROOT } from '../core/api';
import { CalendarEvent, CalendarEventType, CalendarFilter, CalendarView } from './calendar.models';
import { addDays, endOfMonth, isoDay, startOfDay, startOfMonth, startOfWeekMon } from './calendar.dates';

const CALENDAR_V8 = `${API_ROOT}/v8/calendar`;

/** Wire shape returned by j-lawyer-io (only the fields we consume). */
interface CalendarEventDto {
  id: string; type: CalendarEventType; summary: string; description: string; location: string;
  begin: number; end: number | null; done: boolean; assignee: string;
  caseId: string; caseFileNumber: string; caseName: string;
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
    // Only appointments (Termin) carry a meaningful clock time; the others are all-day.
    timed: dto.type === 'event',
    caseId: dto.caseId ?? '',
    caseFileNumber: dto.caseFileNumber ?? '',
    caseName: dto.caseName ?? '',
  };
}
