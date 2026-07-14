import { ChangeDetectionStrategy, Component, computed, effect, inject, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { CalendarService } from './calendar.service';
import { CalendarEvent, CaseRef, EventDraft } from './calendar.models';

/** Reminder options mirroring the desktop client (NewEventPanel): value in minutes, -1 = none. */
const REMINDER_OPTIONS: { value: number; key: string }[] = [
  { value: -1, key: 'kalender.editor.rem.none' },
  { value: 0, key: 'kalender.editor.rem.atStart' },
  { value: 5, key: 'kalender.editor.rem.m5' },
  { value: 10, key: 'kalender.editor.rem.m10' },
  { value: 15, key: 'kalender.editor.rem.m15' },
  { value: 30, key: 'kalender.editor.rem.m30' },
  { value: 60, key: 'kalender.editor.rem.h1' },
  { value: 120, key: 'kalender.editor.rem.h2' },
  { value: 1440, key: 'kalender.editor.rem.d1' },
];

/**
 * Modal editor for a calendar entry (create or edit). The entry kind (appointment / deadline /
 * follow-up) is derived from the chosen calendar's own type — appointments are timed, the others
 * are all-day. On create the owning case is picked via a debounced search; on edit it is fixed.
 * Emits the assembled {@link EventDraft} on save, the id on delete, or nothing on cancel — the
 * parent performs the REST call. Rendered only while open (mounted via {@code @if}).
 */
@Component({
  selector: 'jl-event-editor',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, TranslocoModule, IconComponent],
  template: `
    <div class="backdrop" (click)="close.emit()"></div>
    <div class="dialog" role="dialog" aria-modal="true">
      <header class="dh">
        <h2>{{ (event() ? 'kalender.editor.editTitle' : 'kalender.editor.newTitle') | transloco }}</h2>
        <button type="button" class="x" (click)="close.emit()" [attr.aria-label]="'kalender.editor.cancel' | transloco">
          <jl-icon name="close" [size]="18" />
        </button>
      </header>

      <div class="db">
        <!-- Case -->
        <label class="fld">
          <span class="lbl">{{ 'kalender.editor.case' | transloco }}</span>
          @if (event()) {
            <span class="fixed">{{ caseLabel() }}</span>
          } @else if (presetCase()) {
            <span class="fixed">{{ presetCase()!.fileNumber }} · {{ presetCase()!.name }}</span>
          } @else {
            @if (selectedCase(); as c) {
              <span class="picked">
                {{ c.fileNumber }} · {{ c.name }}
                <button type="button" class="unpick" (click)="clearCase()" [attr.aria-label]="'kalender.editor.cancel' | transloco">
                  <jl-icon name="close" [size]="13" />
                </button>
              </span>
            } @else {
              <input type="text" [ngModel]="caseQuery()" (ngModelChange)="onCaseQuery($event)"
                     [placeholder]="'kalender.editor.casePlaceholder' | transloco" autocomplete="off" />
              @if (caseResults().length) {
                <div class="results">
                  @for (r of caseResults(); track r.id) {
                    <button type="button" class="res" (click)="pickCase(r)">
                      <b>{{ r.fileNumber }}</b> · {{ r.name }}
                    </button>
                  }
                </div>
              }
            }
          }
        </label>

        <!-- Calendar (drives type + timed) -->
        <label class="fld">
          <span class="lbl">{{ 'kalender.editor.calendar' | transloco }}</span>
          <select [ngModel]="calendarId()" (ngModelChange)="calendarId.set($event)">
            @for (c of cal.calendars(); track c.id) {
              <option [value]="c.id">{{ c.displayName }} ({{ 'kalender.type.' + c.eventType | transloco }})</option>
            }
          </select>
        </label>

        <!-- Title -->
        <label class="fld">
          <span class="lbl">{{ 'kalender.editor.summary' | transloco }}</span>
          <input type="text" [ngModel]="summary()" (ngModelChange)="summary.set($event)" />
        </label>

        <!-- Date + time(s). Appointments carry begin+end date/time (multi-day possible); the
             other kinds are all-day on a single date. -->
        @if (timed()) {
          <div class="row">
            <label class="fld">
              <span class="lbl">{{ 'kalender.editor.start' | transloco }}</span>
              <input type="date" [ngModel]="beginDateStr()" (ngModelChange)="onBeginDate($event)" />
            </label>
            <label class="fld sm">
              <span class="lbl">{{ 'kalender.editor.time' | transloco }}</span>
              <input type="time" [ngModel]="startTime()" (ngModelChange)="startTime.set($event)" />
            </label>
          </div>
          <div class="row">
            <label class="fld">
              <span class="lbl">{{ 'kalender.editor.end' | transloco }}</span>
              <input type="date" [ngModel]="endDateStr()" (ngModelChange)="endDateStr.set($event)" [min]="beginDateStr()" />
            </label>
            <label class="fld sm">
              <span class="lbl">{{ 'kalender.editor.time' | transloco }}</span>
              <input type="time" [ngModel]="endTime()" (ngModelChange)="endTime.set($event)" />
            </label>
          </div>
        } @else {
          <label class="fld">
            <span class="lbl">{{ 'kalender.editor.date' | transloco }}</span>
            <input type="date" [ngModel]="beginDateStr()" (ngModelChange)="onBeginDate($event)" />
          </label>
        }

        <!-- Assignee + reminder -->
        <div class="row">
          <label class="fld">
            <span class="lbl">{{ 'kalender.editor.assignee' | transloco }}</span>
            <select [ngModel]="assignee()" (ngModelChange)="assignee.set($event)">
              <option value="">{{ 'kalender.editor.assigneeNone' | transloco }}</option>
              @for (u of assigneeOptions(); track u.principalId) {
                <option [value]="u.principalId">{{ u.displayName }}</option>
              }
            </select>
          </label>
          <label class="fld" [class.disabled]="!timed()">
            <span class="lbl">{{ 'kalender.editor.reminder' | transloco }}</span>
            <select [ngModel]="timed() ? reminderMinutes() : -1" (ngModelChange)="reminderMinutes.set(+$event)"
                    [disabled]="!timed()"
                    [title]="timed() ? null : ('kalender.editor.reminderEventOnly' | transloco)">
              @for (o of reminderOptions; track o.value) {
                <option [value]="o.value">{{ o.key | transloco }}</option>
              }
            </select>
          </label>
        </div>

        <!-- Location -->
        <label class="fld">
          <span class="lbl">{{ 'kalender.editor.location' | transloco }}</span>
          <input type="text" [ngModel]="location()" (ngModelChange)="location.set($event)" />
        </label>

        <!-- Description -->
        <label class="fld">
          <span class="lbl">{{ 'kalender.editor.description' | transloco }}</span>
          <textarea rows="3" [ngModel]="description()" (ngModelChange)="description.set($event)"></textarea>
        </label>

        <label class="chk">
          <input type="checkbox" [ngModel]="done()" (ngModelChange)="done.set($event)" />
          {{ 'kalender.editor.done' | transloco }}
        </label>
      </div>

      <footer class="df">
        @if (event()) {
          <button type="button" class="btn danger" [disabled]="cal.saving()" (click)="remove.emit(event()!.id)">
            <jl-icon name="trash" [size]="15" /> {{ 'kalender.editor.delete' | transloco }}
          </button>
        }
        @if (caseIdResolved()) {
          <button type="button" class="btn" (click)="openCase.emit(caseIdResolved())">
            <jl-icon name="cases" [size]="15" /> {{ 'kalender.editor.toCase' | transloco }}
          </button>
        }
        <span class="spacer"></span>
        <button type="button" class="btn" (click)="close.emit()">{{ 'kalender.editor.cancel' | transloco }}</button>
        <button type="button" class="btn primary" [disabled]="!canSave() || cal.saving()" (click)="submit()">
          {{ 'kalender.editor.save' | transloco }}
        </button>
      </footer>
    </div>
  `,
  styles: [`
    :host { position: fixed; inset: 0; z-index: 60; display: grid; place-items: center; }
    .backdrop { position: absolute; inset: 0; background: rgba(6, 14, 22, .5); }
    .dialog {
      position: relative; width: min(560px, 94vw); max-height: 92dvh; display: flex; flex-direction: column;
      background: var(--jl-surface); border: 1px solid var(--jl-line); border-radius: 14px;
      box-shadow: 0 24px 60px rgba(0,0,0,.32); overflow: hidden;
    }
    .dh { display: flex; align-items: center; gap: 12px; padding: 14px 18px; border-bottom: 1px solid var(--jl-line); }
    .dh h2 { margin: 0; font-size: 1.05rem; font-weight: 800; flex: 1; }
    .x { display: inline-grid; place-items: center; width: 32px; height: 32px; border-radius: 8px; border: 0; background: transparent; color: var(--jl-ink-soft); cursor: pointer; }
    .x:hover { background: var(--jl-surface-alt); }
    .db { padding: 16px 18px; overflow-y: auto; display: flex; flex-direction: column; gap: 12px; }
    .row { display: flex; gap: 12px; }
    .fld { display: flex; flex-direction: column; gap: 5px; flex: 1; min-width: 0; position: relative; }
    .fld.sm { flex: 0 0 130px; }
    .lbl { font-size: .74rem; font-weight: 700; color: var(--jl-ink-faint); text-transform: uppercase; letter-spacing: .03em; }
    input, select, textarea {
      font: inherit; font-size: .88rem; padding: 8px 10px; border: 1px solid var(--jl-line-strong);
      border-radius: 8px; background: var(--jl-surface); color: var(--jl-ink); width: 100%;
    }
    input:focus, select:focus, textarea:focus { outline: none; border-color: var(--jl-blue); }
    select:disabled { opacity: .5; cursor: not-allowed; }
    .fld.disabled .lbl { opacity: .5; }
    textarea { resize: vertical; }
    .fixed, .picked { font-size: .88rem; font-weight: 600; padding: 8px 10px; border: 1px dashed var(--jl-line-strong); border-radius: 8px; display: flex; align-items: center; gap: 8px; }
    .unpick { margin-left: auto; display: inline-grid; place-items: center; width: 20px; height: 20px; border: 0; border-radius: 5px; background: transparent; color: var(--jl-ink-faint); cursor: pointer; }
    .unpick:hover { background: var(--jl-surface-alt); color: var(--jl-red); }
    .results { position: absolute; top: 100%; left: 0; right: 0; z-index: 5; margin-top: 4px; background: var(--jl-surface); border: 1px solid var(--jl-line-strong); border-radius: 8px; box-shadow: 0 12px 30px rgba(0,0,0,.2); max-height: 220px; overflow-y: auto; }
    .res { display: block; width: 100%; text-align: left; padding: 8px 10px; border: 0; background: transparent; color: var(--jl-ink); font: inherit; font-size: .84rem; cursor: pointer; }
    .res:hover { background: var(--jl-surface-alt); }
    .chk { display: flex; align-items: center; gap: 8px; font-size: .88rem; font-weight: 600; }
    .chk input { width: auto; }
    .df { display: flex; align-items: center; gap: 10px; padding: 12px 18px; border-top: 1px solid var(--jl-line); }
    .spacer { flex: 1; }
    .btn { font: inherit; font-size: .85rem; font-weight: 650; padding: 8px 16px; border-radius: 8px; border: 1px solid var(--jl-line-strong); background: var(--jl-surface); color: var(--jl-ink); cursor: pointer; display: inline-flex; align-items: center; gap: 6px; }
    .btn:hover:not(:disabled) { border-color: var(--jl-blue); }
    .btn:disabled { opacity: .5; cursor: default; }
    .btn.primary { background: var(--jl-blue); border-color: var(--jl-blue); color: #fff; }
    .btn.danger { border-color: transparent; color: var(--jl-red); }
    .btn.danger:hover:not(:disabled) { background: color-mix(in srgb, var(--jl-red) 12%, transparent); border-color: var(--jl-red); }
  `],
})
export class EventEditorComponent implements OnInit {
  protected readonly cal = inject(CalendarService);

  /** The entry to edit, or null to create a new one. */
  readonly event = input<CalendarEvent | null>(null);
  /** Start instant to preselect when creating (date, and time when it came from a grid drag). */
  readonly presetBegin = input<Date | null>(null);
  /** End instant to preselect when creating (from a grid drag); defaults to begin + 1h. */
  readonly presetEnd = input<Date | null>(null);
  /** When true (grid drag) prefer an appointment calendar so the time fields show. */
  readonly presetTimed = input<boolean>(false);
  /** A fixed case to file a new entry against (used when creating from a case detail); locks the picker. */
  readonly presetCase = input<CaseRef | null>(null);

  readonly save = output<EventDraft>();
  readonly remove = output<string>();
  readonly close = output<void>();
  /** Emits the owning case id when the user asks to jump to the case (parent navigates + closes). */
  readonly openCase = output<string>();

  protected readonly reminderOptions = REMINDER_OPTIONS;

  protected readonly calendarId = signal('');
  protected readonly selectedCase = signal<CaseRef | null>(null);
  protected readonly caseQuery = signal('');
  protected readonly caseResults = signal<CaseRef[]>([]);
  protected readonly summary = signal('');
  protected readonly description = signal('');
  protected readonly location = signal('');
  protected readonly assignee = signal('');
  protected readonly beginDateStr = signal('');
  protected readonly endDateStr = signal('');
  protected readonly startTime = signal('09:00');
  protected readonly endTime = signal('10:00');
  protected readonly reminderMinutes = signal(-1);
  protected readonly done = signal(false);

  private searchTimer: ReturnType<typeof setTimeout> | null = null;
  private searchSeq = 0;

  constructor() {
    this.cal.loadUsers();
    // When creating, default to a calendar as soon as the list arrives — an appointment calendar
    // for grid drags (so time fields show), otherwise the first calendar.
    effect(() => {
      const list = this.cal.calendars();
      if (!this.event() && list.length && !this.calendarId()) {
        const preferred = this.presetTimed() ? list.find((c) => c.eventType === 'event') : undefined;
        this.calendarId.set((preferred ?? list[0]).id);
      }
    });
  }

  /** True when the selected calendar holds appointments (Termine), which carry a clock time. */
  protected readonly timed = computed(() => {
    const c = this.cal.calendars().find((x) => x.id === this.calendarId());
    return c?.eventType === 'event';
  });

  protected readonly caseLabel = computed(() => {
    const ev = this.event();
    return ev ? `${ev.caseFileNumber} · ${ev.caseName}` : '';
  });

  /**
   * The assignee dropdown options — the login-enabled users, plus the entry's current assignee if
   * it is no longer among them (e.g. a since-disabled user), so editing never drops the value.
   */
  protected readonly assigneeOptions = computed(() => {
    const users = this.cal.users();
    const current = this.event()?.assignee ?? '';
    if (current && !users.some((u) => u.principalId === current)) {
      return [{ principalId: current, displayName: current }, ...users];
    }
    return users;
  });

  protected readonly canSave = computed(() => !!this.calendarId() && !!this.resolveCaseId());

  /** The owning case id (fixed entry case, or the picked case when creating); '' when none. */
  protected readonly caseIdResolved = computed(() => this.event()?.caseId ?? this.presetCase()?.id ?? this.selectedCase()?.id ?? '');

  ngOnInit(): void {
    this.cal.loadCalendars();
    const ev = this.event();
    if (ev) {
      this.calendarId.set(ev.calendarId);
      this.summary.set(ev.summary);
      this.description.set(ev.description);
      this.location.set(ev.location);
      this.assignee.set(ev.assignee);
      this.reminderMinutes.set(ev.reminderMinutes);
      this.done.set(ev.done);
      const end = ev.end ?? new Date(ev.begin.getTime() + 3_600_000);
      this.beginDateStr.set(toDateStr(ev.begin));
      this.endDateStr.set(toDateStr(end));
      this.startTime.set(toTimeStr(ev.begin));
      this.endTime.set(toTimeStr(end));
    } else {
      const begin = this.presetBegin() ?? new Date();
      const end = this.presetEnd() ?? new Date(begin.getTime() + 3_600_000);
      this.beginDateStr.set(toDateStr(begin));
      this.endDateStr.set(toDateStr(end));
      // Only carry a preset clock time from a grid drag; the "New" button leaves the default.
      if (this.presetTimed()) {
        this.startTime.set(toTimeStr(begin));
        this.endTime.set(toTimeStr(end));
      }
      // The default calendar is seeded reactively by the constructor effect once the list loads.
    }
  }

  /** Keeps the end date at/after the begin date (auto-follows while they stay in sync). */
  protected onBeginDate(value: string): void {
    const prev = this.beginDateStr();
    this.beginDateStr.set(value);
    if (!this.endDateStr() || this.endDateStr() === prev || this.endDateStr() < value) {
      this.endDateStr.set(value);
    }
  }

  protected onCaseQuery(q: string): void {
    this.caseQuery.set(q);
    if (this.searchTimer) {
      clearTimeout(this.searchTimer);
    }
    if (q.trim().length < 2) {
      this.caseResults.set([]);
      return;
    }
    const seq = ++this.searchSeq;
    this.searchTimer = setTimeout(() => {
      this.cal.searchCases(q.trim()).subscribe({
        next: (rows) => { if (seq === this.searchSeq) { this.caseResults.set(rows); } },
        error: () => { if (seq === this.searchSeq) { this.caseResults.set([]); } },
      });
    }, 250);
  }

  protected pickCase(c: CaseRef): void {
    this.selectedCase.set(c);
    this.caseResults.set([]);
    this.caseQuery.set('');
  }

  protected clearCase(): void {
    this.selectedCase.set(null);
  }

  private resolveCaseId(): string {
    return this.event()?.caseId ?? this.presetCase()?.id ?? this.selectedCase()?.id ?? '';
  }

  protected submit(): void {
    if (!this.canSave()) {
      return;
    }
    const timed = this.timed();
    const begin = combine(this.beginDateStr(), timed ? this.startTime() : '00:00');
    // All-day entries (Frist/Wiedervorlage) are single-day; appointments may span multiple days.
    const end = timed ? combine(this.endDateStr(), this.endTime()) : begin;
    const type = this.cal.calendars().find((c) => c.id === this.calendarId())?.eventType ?? 'followup';
    const ev = this.event();
    this.save.emit({
      id: ev?.id,
      caseId: this.resolveCaseId(),
      calendar: this.calendarId(),
      type,
      summary: this.summary().trim(),
      description: this.description().trim(),
      location: this.location().trim(),
      assignee: this.assignee().trim(),
      begin: begin.getTime(),
      end: Math.max(end.getTime(), begin.getTime()),
      // Reminders apply to appointments only; deadlines/follow-ups store "no reminder".
      reminderMinutes: timed ? this.reminderMinutes() : -1,
      done: this.done(),
    });
  }
}

function pad2(n: number): string {
  return String(n).padStart(2, '0');
}

/** Local yyyy-MM-dd for an <input type="date">. */
function toDateStr(d: Date): string {
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;
}

/** Local HH:mm for an <input type="time">. */
function toTimeStr(d: Date): string {
  return `${pad2(d.getHours())}:${pad2(d.getMinutes())}`;
}

/** Combines a yyyy-MM-dd and HH:mm into a local Date. */
function combine(dateStr: string, timeStr: string): Date {
  const [y, m, d] = dateStr.split('-').map(Number);
  const [hh, mm] = (timeStr || '00:00').split(':').map(Number);
  return new Date(y, (m || 1) - 1, d || 1, hh || 0, mm || 0);
}
