import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { LanguageService } from '../core/language.service';
import { CalendarService } from './calendar.service';
import { CalendarEvent, CalendarFilter } from './calendar.models';

/** One agenda day with preformatted labels (recomputed on language change). */
interface AgendaDay {
  key: string;
  label: string;
  isToday: boolean;
  rows: { ev: CalendarEvent; time: string }[];
}

/**
 * Kalender module — a read-first agenda of the caller's appointments (Termine), deadlines
 * (Fristen) and follow-ups (Wiedervorlagen) for a month, grouped by day. Month navigation,
 * type filter chips and an "open only" toggle drive a server-side query (GET /v8/calendar/events,
 * ACL-restricted). Locale-aware date labels via the active UI language.
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
          <button type="button" class="ico" (click)="cal.shiftMonth(-1)" [attr.aria-label]="'kalender.prev' | transloco">‹</button>
          <h1>{{ monthLabel() }}</h1>
          <button type="button" class="ico" (click)="cal.shiftMonth(1)" [attr.aria-label]="'kalender.next' | transloco">›</button>
          <button type="button" class="today" (click)="cal.goToday()">{{ 'kalender.today' | transloco }}</button>
        </div>
        <div class="head-right">
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

      <div class="agenda">
        @if (cal.loading()) {
          <p class="muted pad">{{ 'kalender.loading' | transloco }}</p>
        } @else if (cal.error()) {
          <p class="pad">
            {{ 'kalender.error' | transloco }}
            <button type="button" class="btn-retry" (click)="cal.reload()">{{ 'kalender.retry' | transloco }}</button>
          </p>
        } @else {
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
                  <span class="bar" [class]="r.ev.type"></span>
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
        }
      </div>
    </section>
  `,
  styleUrl: './kalender.component.css',
})
export class KalenderComponent {
  protected readonly cal = inject(CalendarService);
  private readonly lang = inject(LanguageService).current;

  protected readonly filters: CalendarFilter[] = ['all', 'event', 'respite', 'followup'];
  private readonly todayKey = signal(isoDay(new Date()));

  protected readonly monthLabel = computed(() =>
    new Intl.DateTimeFormat(this.lang(), { month: 'long', year: 'numeric' }).format(this.cal.month()),
  );

  protected readonly countLabel = computed(() => String(this.cal.events().length));

  /** Groups the events into days with locale-formatted labels (recomputes on language change). */
  protected readonly days = computed<AgendaDay[]>(() => {
    const locale = this.lang();
    const today = this.todayKey();
    const dayFmt = new Intl.DateTimeFormat(locale, { weekday: 'long', day: '2-digit', month: 'long' });
    const timeFmt = new Intl.DateTimeFormat(locale, { hour: '2-digit', minute: '2-digit' });

    const groups = new Map<string, AgendaDay>();
    for (const ev of this.cal.events()) {
      const key = isoDay(ev.begin);
      let group = groups.get(key);
      if (!group) {
        group = { key, label: dayFmt.format(ev.begin), isToday: key === today, rows: [] };
        groups.set(key, group);
      }
      group.rows.push({ ev, time: ev.timed ? timeFmt.format(ev.begin) : '' });
    }
    return [...groups.values()];
  });

  constructor() {
    this.cal.reload();
  }
}

/** Local yyyy-MM-dd (no timezone shift), used to group and match "today". */
function isoDay(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}
