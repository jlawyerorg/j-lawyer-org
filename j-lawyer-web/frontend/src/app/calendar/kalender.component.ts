import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { LanguageService } from '../core/language.service';
import { CalendarService } from './calendar.service';
import { CalendarEvent, CalendarFilter, CalendarView } from './calendar.models';
import { addDays, sameDay, startOfDay, startOfMonth, startOfWeekMon } from './calendar.dates';

/** One agenda day with preformatted labels (recomputed on language change). */
interface AgendaDay {
  key: string;
  label: string;
  isToday: boolean;
  rows: { ev: CalendarEvent; time: string }[];
}

/** A timed event positioned inside a day/week time-grid column. */
interface GridEvent {
  ev: CalendarEvent;
  time: string;
  top: number;
  height: number;
  lane: number;
  lanes: number;
}

/** One column of the day/week time grid. */
interface GridColumn {
  key: string;
  date: Date;
  weekday: string;
  dayLabel: string;
  isToday: boolean;
  allDay: CalendarEvent[];
  timed: GridEvent[];
}

/** One cell of the month grid. */
interface MonthCell {
  key: string;
  dayNum: number;
  inMonth: boolean;
  isToday: boolean;
  events: { ev: CalendarEvent; time: string }[];
}

const DAY_START = 7; // first hour shown in the time grid
const DAY_END = 21; // last hour boundary shown (exclusive slot end)
const HOUR_PX = 46;

/**
 * Kalender module — the caller's appointments (Termine), deadlines (Fristen) and follow-ups
 * (Wiedervorlagen), ACL-restricted (GET /v8/calendar/events). Beside the agenda list it offers
 * day / week / month sheet layouts. The active view drives the fetched date range in
 * CalendarService; type filter chips and an "open only" toggle apply to every layout.
 */
@Component({
  selector: 'jl-kalender',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent, RouterLink],
  template: `
    <section class="cal">
      <header class="cal-head">
        <div class="nav">
          <button type="button" class="ico" (click)="cal.shift(-1)" [attr.aria-label]="'kalender.prev' | transloco">‹</button>
          <h1>{{ rangeLabel() }}</h1>
          <button type="button" class="ico" (click)="cal.shift(1)" [attr.aria-label]="'kalender.next' | transloco">›</button>
          <button type="button" class="today" (click)="cal.goToday()">{{ 'kalender.today' | transloco }}</button>
        </div>
        <div class="head-right">
          <div class="views" role="tablist">
            @for (v of views; track v) {
              <button type="button" class="vbtn" [class.on]="cal.view() === v" (click)="cal.setView(v)">
                {{ 'kalender.view.' + v | transloco }}
              </button>
            }
          </div>
          <label class="toggle">
            <input type="checkbox" [checked]="cal.openOnly()" (change)="cal.setOpenOnly($any($event.target).checked)" />
            {{ 'kalender.openOnly' | transloco }}
          </label>
          <span class="count">{{ countLabel() }}</span>
        </div>
      </header>

      <div class="filters" role="tablist">
        @for (f of filters; track f) {
          <button type="button" class="chip" [class.on]="cal.filter() === f" (click)="cal.setFilter(f)">
            {{ 'kalender.filter.' + f | transloco }}
          </button>
        }
      </div>

      @if (cal.loading()) {
        <p class="muted pad">{{ 'kalender.loading' | transloco }}</p>
      } @else if (cal.error()) {
        <p class="pad">
          {{ 'kalender.error' | transloco }}
          <button type="button" class="btn-retry" (click)="cal.reload()">{{ 'kalender.retry' | transloco }}</button>
        </p>
      } @else {
        @switch (cal.view()) {
          @case ('agenda') {
            <div class="agenda">
              @for (d of days(); track d.key) {
                <div class="day">
                  <div class="day-h" [class.today]="d.isToday">
                    <span class="dl">{{ d.label }}</span>
                    @if (d.isToday) { <span class="badge-today">{{ 'kalender.todayTag' | transloco }}</span> }
                  </div>
                  @for (r of d.rows; track r.ev.id) {
                    <div class="ev" [class.done]="r.ev.done">
                      <span class="etime">
                        @if (r.time) { {{ r.time }} } @else { <span class="allday">{{ 'kalender.allDay' | transloco }}</span> }
                      </span>
                      <span class="bar" [class]="r.ev.type" [style.background]="r.ev.color || null"
                            [title]="r.ev.calendarName || null"></span>
                      <span class="ebody">
                        <span class="etop">
                          <span class="pill" [class]="r.ev.type">{{ 'kalender.type.' + r.ev.type | transloco }}</span>
                          <span class="esum">{{ r.ev.summary || ('kalender.noSummary' | transloco) }}</span>
                          @if (r.ev.done) { <jl-icon name="check" [size]="14" /> }
                        </span>
                        <span class="emeta">
                          @if (r.ev.caseFileNumber) {
                            @if (r.ev.caseId) {
                              <a class="ecase link" [routerLink]="['/cases', r.ev.caseId]">{{ r.ev.caseFileNumber }} · {{ r.ev.caseName }}</a>
                            } @else {
                              <span class="ecase">{{ r.ev.caseFileNumber }} · {{ r.ev.caseName }}</span>
                            }
                          }
                          @if (r.ev.location) { <span class="eloc"><jl-icon name="pin" [size]="12" /> {{ r.ev.location }}</span> }
                          @if (r.ev.assignee) { <span class="eass">{{ r.ev.assignee }}</span> }
                        </span>
                      </span>
                    </div>
                  }
                </div>
              } @empty {
                <p class="muted pad">{{ 'kalender.empty' | transloco }}</p>
              }
            </div>
          }

          @case ('month') {
            <div class="month">
              <div class="wk-head">
                @for (w of weekdayHeaders(); track w) { <span>{{ w }}</span> }
              </div>
              <div class="month-grid">
                @for (cell of monthCells(); track cell.key) {
                  <div class="mcell" [class.out]="!cell.inMonth" [class.today]="cell.isToday">
                    <span class="mday">{{ cell.dayNum }}</span>
                    @for (r of cell.events.slice(0, 3); track r.ev.id) {
                      @if (r.ev.caseId) {
                        <a class="mchip" [class]="r.ev.type" [class.done]="r.ev.done"
                           [style.background]="r.ev.color ? tint(r.ev.color) : null"
                           [routerLink]="['/cases', r.ev.caseId]" [title]="r.ev.calendarName || r.ev.summary">
                          <span class="mdot" [style.background]="r.ev.color || null"></span>{{ r.time }}{{ r.ev.summary || ('kalender.noSummary' | transloco) }}
                        </a>
                      } @else {
                        <span class="mchip" [class]="r.ev.type" [class.done]="r.ev.done"
                              [style.background]="r.ev.color ? tint(r.ev.color) : null" [title]="r.ev.calendarName || r.ev.summary">
                          <span class="mdot" [style.background]="r.ev.color || null"></span>{{ r.time }}{{ r.ev.summary || ('kalender.noSummary' | transloco) }}
                        </span>
                      }
                    }
                    @if (cell.events.length > 3) {
                      <span class="mmore">{{ 'kalender.more' | transloco: { n: cell.events.length - 3 } }}</span>
                    }
                  </div>
                }
              </div>
            </div>
          }

          @default {
            <div class="timegrid" [class.week]="cal.view() === 'week'">
              <div class="tg-headrow">
                <span class="tg-gutter"></span>
                <div class="tg-cols">
                  @for (col of gridDays(); track col.key) {
                    <div class="tg-colhead" [class.today]="col.isToday">
                      <span class="ch-wd">{{ col.weekday }}</span>
                      <span class="ch-d">{{ col.dayLabel }}</span>
                    </div>
                  }
                </div>
              </div>
              <div class="tg-alldayrow">
                <span class="tg-gutter">{{ 'kalender.allDay' | transloco }}</span>
                <div class="tg-cols">
                  @for (col of gridDays(); track col.key) {
                    <div class="tg-allday">
                      @for (ev of col.allDay; track ev.id) {
                        @if (ev.caseId) {
                          <a class="mchip" [class]="ev.type" [class.done]="ev.done"
                             [style.background]="ev.color ? tint(ev.color) : null"
                             [routerLink]="['/cases', ev.caseId]" [title]="ev.calendarName || ev.summary">
                            <span class="mdot" [style.background]="ev.color || null"></span>{{ ev.summary || ('kalender.noSummary' | transloco) }}
                          </a>
                        } @else {
                          <span class="mchip" [class]="ev.type" [class.done]="ev.done"
                                [style.background]="ev.color ? tint(ev.color) : null" [title]="ev.calendarName || ev.summary">
                            <span class="mdot" [style.background]="ev.color || null"></span>{{ ev.summary || ('kalender.noSummary' | transloco) }}
                          </span>
                        }
                      }
                    </div>
                  }
                </div>
              </div>
              <div class="tg-scroll">
                <div class="tg-body">
                  <div class="tg-hours">
                    @for (h of hours; track h) {
                      <div class="tg-hour" [style.height.px]="hourPx"><span>{{ hourLabel(h) }}</span></div>
                    }
                  </div>
                  <div class="tg-cols">
                    @for (col of gridDays(); track col.key) {
                      <div class="tg-col">
                        @for (h of hours; track h) { <div class="tg-hline" [style.height.px]="hourPx"></div> }
                        @for (ge of col.timed; track ge.ev.id) {
                          @if (ge.ev.caseId) {
                            <a class="tev" [class]="ge.ev.type" [class.done]="ge.ev.done"
                               [routerLink]="['/cases', ge.ev.caseId]" [title]="ge.ev.calendarName || ge.ev.summary"
                               [style.background]="ge.ev.color || null"
                               [style.border-color]="ge.ev.color ? darken(ge.ev.color) : null"
                               [style.color]="ge.ev.color ? contrastOn(ge.ev.color) : null"
                               [style.top.px]="ge.top" [style.height.px]="ge.height"
                               [style.left.%]="ge.lane * (100 / ge.lanes)" [style.width.%]="100 / ge.lanes">
                              <span class="tev-t">{{ ge.time }}</span>
                              <span class="tev-s">{{ ge.ev.summary || ('kalender.noSummary' | transloco) }}</span>
                            </a>
                          } @else {
                            <span class="tev" [class]="ge.ev.type" [class.done]="ge.ev.done" [title]="ge.ev.calendarName || ge.ev.summary"
                                  [style.background]="ge.ev.color || null"
                                  [style.border-color]="ge.ev.color ? darken(ge.ev.color) : null"
                                  [style.color]="ge.ev.color ? contrastOn(ge.ev.color) : null"
                                  [style.top.px]="ge.top" [style.height.px]="ge.height"
                                  [style.left.%]="ge.lane * (100 / ge.lanes)" [style.width.%]="100 / ge.lanes">
                              <span class="tev-t">{{ ge.time }}</span>
                              <span class="tev-s">{{ ge.ev.summary || ('kalender.noSummary' | transloco) }}</span>
                            </span>
                          }
                        }
                      </div>
                    }
                  </div>
                </div>
              </div>
            </div>
          }
        }
      }
    </section>
  `,
  styleUrl: './kalender.component.css',
})
export class KalenderComponent {
  protected readonly cal = inject(CalendarService);
  private readonly lang = inject(LanguageService).current;

  protected readonly filters: CalendarFilter[] = ['all', 'event', 'respite', 'followup'];
  protected readonly views: CalendarView[] = ['agenda', 'day', 'week', 'month'];
  private readonly todayKey = signal(isoDayKey(new Date()));

  protected readonly hours = Array.from({ length: DAY_END - DAY_START }, (_, i) => DAY_START + i);
  protected readonly hourPx = HOUR_PX;

  protected readonly countLabel = computed(() => String(this.cal.events().length));

  /** The header label, adapted to the active view (month, week range or full day). */
  protected readonly rangeLabel = computed(() => {
    const locale = this.lang();
    const a = this.cal.anchor();
    switch (this.cal.view()) {
      case 'day':
        return new Intl.DateTimeFormat(locale, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }).format(a);
      case 'week': {
        const s = startOfWeekMon(a);
        const e = addDays(s, 6);
        const dm = new Intl.DateTimeFormat(locale, { day: 'numeric', month: 'short' });
        const dmy = new Intl.DateTimeFormat(locale, { day: 'numeric', month: 'short', year: 'numeric' });
        return `${dm.format(s)} – ${dmy.format(e)}`;
      }
      default: // agenda + month
        return new Intl.DateTimeFormat(locale, { month: 'long', year: 'numeric' }).format(a);
    }
  });

  /** Localized Mon–Sun short weekday headers for the month grid. */
  protected readonly weekdayHeaders = computed(() => {
    const fmt = new Intl.DateTimeFormat(this.lang(), { weekday: 'short' });
    const monday = startOfWeekMon(new Date(2024, 0, 1)); // any reference week
    return Array.from({ length: 7 }, (_, i) => fmt.format(addDays(monday, i)));
  });

  /** Agenda: groups the events into days with locale-formatted labels. */
  protected readonly days = computed<AgendaDay[]>(() => {
    const locale = this.lang();
    const today = this.todayKey();
    const dayFmt = new Intl.DateTimeFormat(locale, { weekday: 'long', day: '2-digit', month: 'long' });
    const timeFmt = new Intl.DateTimeFormat(locale, { hour: '2-digit', minute: '2-digit' });

    const groups = new Map<string, AgendaDay>();
    for (const ev of this.cal.events()) {
      const key = isoDayKey(ev.begin);
      let group = groups.get(key);
      if (!group) {
        group = { key, label: dayFmt.format(ev.begin), isToday: key === today, rows: [] };
        groups.set(key, group);
      }
      group.rows.push({ ev, time: ev.timed ? timeFmt.format(ev.begin) : '' });
    }
    return [...groups.values()];
  });

  /** Day/week time-grid columns (1 for day, 7 for week), each with all-day + positioned timed events. */
  protected readonly gridDays = computed<GridColumn[]>(() => {
    const locale = this.lang();
    const today = new Date();
    const wdFmt = new Intl.DateTimeFormat(locale, { weekday: 'short' });
    const dFmt = new Intl.DateTimeFormat(locale, { day: 'numeric', month: 'short' });
    const timeFmt = new Intl.DateTimeFormat(locale, { hour: '2-digit', minute: '2-digit' });

    const anchor = this.cal.anchor();
    const dates = this.cal.view() === 'day'
      ? [startOfDay(anchor)]
      : Array.from({ length: 7 }, (_, i) => addDays(startOfWeekMon(anchor), i));

    return dates.map((date) => {
      const dayEvents = this.cal.events().filter((e) => sameDay(e.begin, date));
      const allDay = dayEvents.filter((e) => !e.timed);
      const timed = layoutTimed(dayEvents.filter((e) => e.timed), timeFmt);
      return {
        key: isoDayKey(date),
        date,
        weekday: wdFmt.format(date),
        dayLabel: dFmt.format(date),
        isToday: sameDay(date, today),
        allDay,
        timed,
      };
    });
  });

  /** Month grid cells (6 weeks × 7 days from the Monday on/before the 1st). */
  protected readonly monthCells = computed<MonthCell[]>(() => {
    const locale = this.lang();
    const timeFmt = new Intl.DateTimeFormat(locale, { hour: '2-digit', minute: '2-digit' });
    const today = new Date();
    const anchorMonth = startOfMonth(this.cal.anchor()).getMonth();
    const start = startOfWeekMon(startOfMonth(this.cal.anchor()));

    // Bucket events by local day for O(1) lookup per cell.
    const byDay = new Map<string, CalendarEvent[]>();
    for (const ev of this.cal.events()) {
      const key = isoDayKey(ev.begin);
      (byDay.get(key) ?? byDay.set(key, []).get(key)!).push(ev);
    }

    return Array.from({ length: 42 }, (_, i) => {
      const date = addDays(start, i);
      const key = isoDayKey(date);
      const events = (byDay.get(key) ?? []).map((ev) => ({ ev, time: ev.timed ? timeFmt.format(ev.begin) + ' ' : '' }));
      return {
        key,
        dayNum: date.getDate(),
        inMonth: date.getMonth() === anchorMonth,
        isToday: sameDay(date, today),
        events,
      };
    });
  });

  protected hourLabel(h: number): string {
    return `${String(h).padStart(2, '0')}:00`;
  }

  /** A faint tint of the calendar colour for chip backgrounds (month/all-day). */
  protected tint(hex: string): string {
    return `color-mix(in srgb, ${hex} 16%, transparent)`;
  }

  /** A darker shade of the calendar colour for the time-grid block border. */
  protected darken(hex: string): string {
    return `color-mix(in srgb, ${hex} 65%, #000)`;
  }

  /**
   * Readable foreground (black or white) for a given "#rrggbb" background, by perceived
   * luminance, so a light calendar colour never renders unreadable white text on the time grid.
   */
  protected contrastOn(hex: string): string {
    const m = /^#?([0-9a-f]{6})$/i.exec(hex);
    if (!m) { return '#fff'; }
    const n = parseInt(m[1], 16);
    const r = (n >> 16) & 0xff, g = (n >> 8) & 0xff, b = n & 0xff;
    return 0.299 * r + 0.587 * g + 0.114 * b > 150 ? '#16232e' : '#fff';
  }

  constructor() {
    this.cal.reload();
  }
}

/** Local yyyy-MM-dd key used to group and match "today". */
function isoDayKey(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

/**
 * Positions timed events in a day column and splits overlapping ones into side-by-side lanes
 * (greedy interval partitioning). Times outside the visible window are clamped into it.
 */
function layoutTimed(events: CalendarEvent[], timeFmt: Intl.DateTimeFormat): GridEvent[] {
  const sorted = [...events].sort((a, b) => a.begin.getTime() - b.begin.getTime());
  const laneEnds: number[] = []; // end epoch per lane
  const withLane = sorted.map((ev) => {
    const s = ev.begin.getTime();
    const e = ev.end ? ev.end.getTime() : s + 3_600_000;
    let lane = laneEnds.findIndex((end) => end <= s);
    if (lane === -1) {
      lane = laneEnds.length;
      laneEnds.push(e);
    } else {
      laneEnds[lane] = e;
    }
    return { ev, lane };
  });
  const lanes = Math.max(1, laneEnds.length);

  const winStart = DAY_START * 60;
  const winEnd = DAY_END * 60;
  return withLane.map(({ ev, lane }) => {
    const startMin = ev.begin.getHours() * 60 + ev.begin.getMinutes();
    const endMin = ev.end ? ev.end.getHours() * 60 + ev.end.getMinutes() : startMin + 60;
    const top = Math.max(startMin, winStart);
    const bottom = Math.min(Math.max(endMin, top + 30), winEnd);
    return {
      ev,
      time: timeFmt.format(ev.begin),
      top: ((top - winStart) / 60) * HOUR_PX,
      height: ((bottom - top) / 60) * HOUR_PX,
      lane,
      lanes,
    };
  });
}
