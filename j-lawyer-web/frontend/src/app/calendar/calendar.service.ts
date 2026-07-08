import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { API_ROOT } from '../core/api';
import { CalendarEvent, CalendarEventType, CalendarFilter } from './calendar.models';

const CALENDAR_V8 = `${API_ROOT}/v8/calendar`;

/** Wire shape returned by j-lawyer-io (only the fields we consume). */
interface CalendarEventDto {
  id: string; type: CalendarEventType; summary: string; description: string; location: string;
  begin: number; end: number | null; done: boolean; assignee: string;
  caseId: string; caseFileNumber: string; caseName: string;
}

/**
 * Calendar data access against the real REST API (GET /v8/calendar/events). Holds the current
 * view state (anchor month, type filter, open/all status) and refetches whenever it changes.
 * The Bearer token is attached by authInterceptor.
 */
@Injectable({ providedIn: 'root' })
export class CalendarService {
  private readonly http = inject(HttpClient);

  readonly events = signal<CalendarEvent[]>([]);
  readonly loading = signal(false);
  readonly error = signal(false);
  /** First day of the currently displayed month (local). */
  readonly month = signal<Date>(startOfMonth(new Date()));
  readonly filter = signal<CalendarFilter>('all');
  /** When true, only open (not-done) entries are requested. */
  readonly openOnly = signal(false);

  private requestSeq = 0;

  /** Loads the events for the current month/filter/status (also used for retry). */
  reload(): void {
    const anchor = this.month();
    const from = isoDay(anchor);
    const to = isoDay(endOfMonth(anchor));
    this.loading.set(true);
    this.error.set(false);
    const seq = ++this.requestSeq;

    let params = new HttpParams()
      .set('from', from)
      .set('to', to)
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

  /** Moves the displayed month by the given offset (e.g. -1 / +1) and reloads. */
  shiftMonth(offset: number): void {
    const cur = this.month();
    this.month.set(new Date(cur.getFullYear(), cur.getMonth() + offset, 1));
    this.reload();
  }

  /** Jumps to the current month and reloads. */
  goToday(): void {
    this.month.set(startOfMonth(new Date()));
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

function startOfMonth(d: Date): Date {
  return new Date(d.getFullYear(), d.getMonth(), 1);
}

function endOfMonth(d: Date): Date {
  return new Date(d.getFullYear(), d.getMonth() + 1, 0);
}

/** Formats a Date as a local yyyy-MM-dd string (no timezone shift). */
function isoDay(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
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
