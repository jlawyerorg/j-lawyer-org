import { ChangeDetectionStrategy, Component, ElementRef, computed, input, output, signal, viewChildren } from '@angular/core';
import { DatePipe } from '@angular/common';
import { TranslocoModule } from '@jsverse/transloco';
import { DueDate } from './case.models';
import { DueKind, LANE_STEP, TIMELINE_TICKS, X_PAST, X_TODAY, assignLanes, daysFromToday, dueKind, timelineX } from './case-timeline.util';

/** One due date placed on the axis. */
interface TimelinePoint {
  due: DueDate;
  kind: DueKind;
  /** Whole days from today; negative for the past. */
  days: number;
  /** Fraction of the track in [0,1]. */
  x: number;
  /** Open and due today or earlier — mirrors AktenComponent.isOverdue. */
  overdue: boolean;
  /** The entry's calendar colour, or '' to fall back to the kind's token colour. */
  color: string;
  /** Stacking lane, 0 = closest to the axis. */
  lane: number;
}

/** A gridline plus the absolute date it stands for. */
interface RenderTick {
  key: string;
  x: number;
  tier: number;
  date: Date;
}

/**
 * Horizontal timeline for the case header: places the case's open Fristen, Wiedervorlagen and
 * Termine on a non-linear time axis so what is imminent, what is months out and what is already
 * overdue can be read at a glance.
 *
 * A pure projection of {@link DueDate}[] — no service, no request. The data is already loaded by
 * CasesService.loadDetail (GET /v4/cases/{id}/duedates).
 *
 * Renders as absolutely positioned buttons rather than SVG: percentage positions are intrinsically
 * fluid (no viewBox, no ResizeObserver) and a native <button> is focusable and Enter/Space-operable
 * without extra work.
 *
 * NOTE ON ENCODING: the three kinds are distinguished by SHAPE first and colour second, and that is
 * not redundant. The established triade (--jl-red / --jl-warning / --jl-blue, see
 * desktop.component.css and kalender.component.css) puts red and amber only ~ΔE 6 apart under
 * deuteranopia in the dark theme — too close to carry the distinction alone. Do not "simplify" the
 * three shapes into three coloured dots.
 */
@Component({
  selector: 'jl-case-timeline',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, TranslocoModule],
  host: {
    '[class.hidden]': '!points().length',
  },
  template: `
    <div class="strip" role="group"
         [attr.aria-label]="('akten.timeline.aria' | transloco) + '. ' + ('akten.timeline.hint' | transloco)"
         (keydown)="onKeydown($event)">

      <!-- The distortion hint sits on the legend, not on the strip: a title covering the whole
           strip would fire a native tooltip over every marker and fight the custom one. -->
      <div class="head">
        @if (overdueCount(); as n) {
          <span class="od">{{ 'akten.timeline.overdueCount' | transloco: { n: n } }}</span>
        }
        <span class="legend" aria-hidden="true" [title]="'akten.timeline.hint' | transloco">
          @for (k of kinds(); track k) {
            <span class="lg"><i class="shape" [class]="k"></i>{{ 'kalender.type.' + k | transloco }}</span>
          }
        </span>
      </div>

      <div class="track">
        <div class="lay">
          @if (overdueCount()) {
            <span class="zone" aria-hidden="true"
                  [style.left.%]="pastPct" [style.width.%]="todayPct - pastPct"></span>
          }

          @for (t of ticks(); track t.key) {
            <span class="tick" aria-hidden="true" [attr.data-tier]="t.tier"
                  [class.today]="t.key === 'today'" [style.left.%]="t.x * 100">
              <i class="rule"></i>
              <em [title]="t.date | date: 'dd.MM.yyyy'">{{ 'akten.timeline.' + t.key | transloco }}</em>
            </span>
          }

          <span class="axis" aria-hidden="true"></span>

          @for (p of points(); track p.due.id; let i = $index) {
            <button #mk type="button" class="mk"
                    [style.left.%]="p.x * 100" [style.bottom.px]="p.lane * laneStep"
                    [attr.tabindex]="i === rovingIndex() ? 0 : -1"
                    [attr.aria-label]="('akten.timeline.entry' | transloco: {
                        date: (p.due.dueDate | date: 'dd.MM.yyyy'),
                        kind: ('kalender.type.' + p.kind | transloco),
                        reason: p.due.reason })
                      + (p.overdue ? ' — ' + ('akten.timeline.overdue' | transloco) : '')
                      + (p.due.done ? ' — ' + ('akten.timeline.done' | transloco) : '')"
                    (click)="pick.emit(p.due)"
                    (pointerenter)="active.set(p)" (pointerleave)="active.set(null)"
                    (focus)="focusIndex.set(i); active.set(p)" (blur)="active.set(null)">
              <span class="shape" [class]="p.kind" [class.done]="p.due.done" [class.overdue]="p.overdue"
                    [style.color]="p.color || null"></span>
            </button>
          }

          <!-- Inside .lay so its left:% shares the markers' coordinate system. -->
          @if (active(); as a) {
            <div class="tip" role="tooltip" [style.left.%]="a.x * 100"
                 [style.transform]="a.x < 0.18 ? 'translateX(0)' : a.x > 0.82 ? 'translateX(-100%)' : 'translateX(-50%)'">
              <span class="t-date">
                {{ a.due.dueDate | date: 'dd.MM.yyyy' }}@if (a.kind === 'event') { · {{ a.due.dueDate | date: 'HH:mm' }} }
              </span>
              <span class="t-reason">{{ a.due.reason }}</span>
              <span class="t-meta">
                <i class="shape" [class]="a.kind" [style.color]="a.color || null"></i>
                {{ 'kalender.type.' + a.kind | transloco }}@if (a.due.assignee) { · {{ a.due.assignee }} }
              </span>
              @if (a.overdue) { <span class="t-od">{{ 'akten.timeline.overdue' | transloco }}</span> }
              @else if (a.due.done) { <span class="t-done">{{ 'akten.timeline.done' | transloco }}</span> }
            </div>
          }
        </div>
      </div>
    </div>
  `,
  styles: [`
    :host { display: block; container-type: inline-size; margin: 0 0 6px; }
    :host(.hidden) { display: none; }

    /* padding-bottom reserves the row the tick labels occupy — they sit at top:100% of the track
       and would otherwise overlap the tab bar below. Total strip height ~78px. */
    .strip { position: relative; padding-bottom: 15px; }

    .head { display: flex; align-items: center; gap: 10px; height: 13px; margin-bottom: 2px; font-size: .68rem; line-height: 13px; }
    .od { font-weight: 800; color: var(--jl-red); }
    .legend { margin-left: auto; display: flex; align-items: center; gap: 9px; color: var(--jl-ink-faint); }
    .lg { display: inline-flex; align-items: center; gap: 4px; }

    /* 3 lanes + the axis. The inset layer keeps markers at 0%/100% from being clipped. */
    .track { position: relative; height: 43px; }
    .lay { position: absolute; inset: 0 12px; }
    .axis { position: absolute; left: 0; right: 0; bottom: 0; height: 1px; background: var(--jl-line-strong); }

    .zone { position: absolute; top: 0; bottom: 0; background: color-mix(in srgb, var(--jl-red) 8%, transparent);
      border-right: 1px solid color-mix(in srgb, var(--jl-red) 30%, transparent); border-radius: 3px 0 0 3px; }

    .tick { position: absolute; top: 0; bottom: 0; }
    .tick .rule { position: absolute; top: 0; bottom: 0; left: 0; width: 1px; background: var(--jl-line); }
    .tick.today .rule { width: 2px; background: var(--jl-accent); }
    .tick em { position: absolute; top: 100%; left: 0; transform: translateX(-50%); margin-top: 3px;
      font-style: normal; font-size: .64rem; line-height: 12px; white-space: nowrap; color: var(--jl-ink-faint); }
    .tick.today em { transform: translateX(-50%); font-weight: 800; color: var(--jl-accent); }
    /* Tick density follows the detail column, not the viewport — hence @container. */
    .tick[data-tier="1"], .tick[data-tier="2"], .tick[data-tier="3"] { display: none; }
    @container (min-width: 420px) { .tick[data-tier="1"] { display: block; } }
    @container (min-width: 560px) { .tick[data-tier="2"] { display: block; } }
    @container (min-width: 780px) { .tick[data-tier="3"] { display: block; } }

    .mk { position: absolute; width: 22px; height: 14px; margin-left: -11px; padding: 0; border: 0;
      background: transparent; cursor: pointer; display: grid; place-items: center; z-index: 2; }
    .mk:focus { outline: none; }

    .shape { width: 10px; height: 10px; background: currentColor; box-shadow: 0 0 0 2px var(--jl-surface); }
    .shape.respite { color: var(--jl-red); border-radius: 1px; transform: rotate(45deg); }
    .shape.followup { color: var(--jl-warning); border-radius: 2px; }
    .shape.event { color: var(--jl-blue); width: 11px; height: 11px; border-radius: 50%; }
    .shape.done { background: transparent; border: 2px solid currentColor; opacity: .5; }
    .shape.overdue { box-shadow: 0 0 0 2px var(--jl-surface), 0 0 0 4px var(--jl-red); }
    .mk:has(.overdue) { z-index: 3; }
    .mk:has(.done) { z-index: 1; }
    .mk:focus-visible .shape { box-shadow: 0 0 0 2px var(--jl-surface), 0 0 0 4px var(--jl-accent); }
    .legend .shape, .t-meta .shape { box-shadow: none; width: 8px; height: 8px; flex: none; }
    .legend .shape.event, .t-meta .shape.event { width: 9px; height: 9px; }

    /* top:100% = the axis; the extra offset clears the tick-label row. Floats over .tabs by design. */
    .tip { position: absolute; top: 100%; z-index: 20; margin-top: 18px; width: max-content; max-width: 260px;
      display: flex; flex-direction: column; gap: 1px; padding: 7px 10px; pointer-events: none;
      background: var(--jl-surface); border: 1px solid var(--jl-line-strong); border-radius: 9px;
      box-shadow: 0 10px 28px rgba(0,0,0,.18); }
    .t-date { font-size: .78rem; font-weight: 800; color: var(--jl-ink); font-variant-numeric: tabular-nums; }
    .t-reason { font-size: .78rem; color: var(--jl-ink); display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
    .t-meta { display: inline-flex; align-items: center; gap: 5px; font-size: .72rem; color: var(--jl-ink-soft); }
    .t-od { font-size: .72rem; font-weight: 800; color: var(--jl-red); }
    .t-done { font-size: .72rem; color: var(--jl-ink-faint); }
  `],
})
export class CaseTimelineComponent {
  /** The case's due dates, as loaded into CaseDetail.dueDates. */
  readonly dueDates = input.required<DueDate[]>();
  /** Injectable "now" so the placement can be checked deterministically; defaults to the clock. */
  readonly now = input<Date | null>(null);
  /** Emitted when a marker is activated — the parent opens the calendar editor. */
  readonly pick = output<DueDate>();

  protected readonly laneStep = LANE_STEP;
  protected readonly pastPct = X_PAST * 100;
  protected readonly todayPct = X_TODAY * 100;

  protected readonly active = signal<TimelinePoint | null>(null);
  protected readonly focusIndex = signal(0);

  private readonly marks = viewChildren<ElementRef<HTMLButtonElement>>('mk');

  protected readonly points = computed<TimelinePoint[]>(() => {
    const now = this.now() ?? new Date();
    const placed = this.dueDates()
      .map((due) => {
        const days = daysFromToday(due.dueDate, now);
        return days === null ? null : { due, days };
      })
      // Open entries always show (however old); done ones only from today on. Done-and-past is
      // pure history, would pile up at the left edge and dilute what the strip exists to signal.
      .filter((e): e is { due: DueDate; days: number } => !!e && (!e.due.done || e.days >= 0))
      .map(({ due, days }) => ({
        due,
        days,
        kind: dueKind(due.restType),
        x: timelineX(days),
        overdue: !due.done && days <= 0,
        color: due.calendarColor,
      }))
      .sort((a, b) => a.x - b.x || a.due.id.localeCompare(b.due.id));

    const lanes = assignLanes(placed.map((p) => p.x));
    return placed.map((p, i) => ({ ...p, lane: lanes[i] }));
  });

  protected readonly ticks = computed<RenderTick[]>(() => {
    const now = this.now() ?? new Date();
    return TIMELINE_TICKS.map((t) => {
      const date = new Date(now.getFullYear(), now.getMonth(), now.getDate() + t.d);
      return { key: t.key, x: t.x, tier: t.tier, date };
    });
  });

  /**
   * The marker that currently holds the single tab stop. Clamped against the point count, so a
   * case switch or a deleted entry can never leave the strip without a reachable tab stop.
   */
  protected readonly rovingIndex = computed(() => Math.min(this.focusIndex(), Math.max(0, this.points().length - 1)));

  protected readonly overdueCount = computed(() => this.points().filter((p) => p.overdue).length);

  /** The kinds actually present in this case, in a stable order — drives the legend. */
  protected readonly kinds = computed<DueKind[]>(() => {
    const present = new Set(this.points().map((p) => p.kind));
    return (['respite', 'followup', 'event'] as DueKind[]).filter((k) => present.has(k));
  });

  /**
   * Roving tabindex: without it a case with 25 due dates would inject 25 tab stops between the
   * case meta and the tab bar. Arrow keys walk the markers, Escape dismisses the tooltip.
   */
  protected onKeydown(ev: KeyboardEvent): void {
    if (ev.key === 'Escape') {
      this.active.set(null);
      return;
    }
    const last = this.points().length - 1;
    if (last < 0) {
      return;
    }
    const current = this.rovingIndex();
    let next: number;
    switch (ev.key) {
      case 'ArrowRight': next = Math.min(last, current + 1); break;
      case 'ArrowLeft': next = Math.max(0, current - 1); break;
      case 'Home': next = 0; break;
      case 'End': next = last; break;
      default: return;
    }
    ev.preventDefault();
    this.focusIndex.set(next);
    this.marks()[next]?.nativeElement.focus();
  }
}
