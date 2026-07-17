import { ChangeDetectionStrategy, Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { AuthService } from '../core/auth/auth.service';
import { CasesService } from './cases.service';
import { TimesheetTrackingService } from './timesheet-tracking.service';
import { CaseUserRef, PositionTemplate, TimesheetPosition } from './case.models';

export type PositionMode = 'start' | 'manual' | 'edit';

/** What the editor emits; the parent converts the local datetimes and calls the right endpoint. */
export interface PositionEditResult {
  mode: PositionMode;
  positionId?: string;
  name: string;
  description: string;
  /** datetime-local strings ("yyyy-MM-ddTHH:mm"); empty in 'start' mode. */
  startLocal: string;
  stopLocal: string;
  unitPrice: number;
  taxRate: number;
  /** Login name of the user the time is logged for. */
  principal: string;
}

/**
 * Modal editor for a time entry (position). Three modes:
 *  - 'start'  → begin a stopwatch (name/description/rate; the server timestamps the start),
 *  - 'manual' → add an entry with explicit start/stop times,
 *  - 'edit'   → change an existing entry.
 * Predefined templates (per timesheet) can pre-fill name/description/rate. Emits {@link PositionEditResult}.
 */
@Component({
  selector: 'jl-position-editor',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, TranslocoModule, IconComponent],
  template: `
    <div class="backdrop" (click)="close.emit()"></div>
    <div class="dialog" role="dialog" aria-modal="true">
      <header class="dh">
        <h2>{{ ('akten.zeiten.pos.' + mode() + 'Title') | transloco }}</h2>
        <button type="button" class="x" (click)="close.emit()" [attr.aria-label]="'akten.editor.cancel' | transloco">
          <jl-icon name="close" [size]="18" />
        </button>
      </header>

      <div class="db">
        @if (mode() === 'start' && parallelWarning()) {
          <p class="pe-warn">{{ 'akten.zeiten.parallelWarning' | transloco }}</p>
        }
        @if (templates().length) {
          <label class="fld">
            <span class="lbl">{{ 'akten.zeiten.pos.template' | transloco }}</span>
            <select #tpl (change)="applyTemplate(tpl.value); tpl.value=''">
              <option value="">—</option>
              @for (t of templates(); track t.id) { <option [value]="t.id">{{ t.name }}</option> }
            </select>
          </label>
        }

        <label class="fld">
          <span class="lbl">{{ 'akten.zeiten.pos.name' | transloco }}</span>
          <input type="text" [ngModel]="name()" (ngModelChange)="name.set($event)" />
        </label>
        <label class="fld">
          <span class="lbl">{{ 'akten.zeiten.pos.description' | transloco }}</span>
          <textarea rows="2" [ngModel]="description()" (ngModelChange)="description.set($event)"></textarea>
        </label>

        <label class="fld">
          <span class="lbl">{{ 'akten.zeiten.pos.user' | transloco }}</span>
          <select [ngModel]="principal()" (ngModelChange)="principal.set($event)">
            @if (!userInList()) { <option [value]="principal()">{{ principal() }}</option> }
            @for (u of users(); track u.principalId) {
              <option [value]="u.principalId">{{ u.displayName }}</option>
            }
          </select>
        </label>

        @if (mode() === 'manual') {
          <label class="fld">
            <span class="lbl">{{ 'akten.zeiten.pos.duration' | transloco }}</span>
            <input type="text" inputmode="decimal" [ngModel]="durationRaw()" (ngModelChange)="durationRaw.set($event)"
                   (change)="applyDuration()" (keydown.enter)="applyDuration()"
                   [placeholder]="'akten.zeiten.pos.durationHint' | transloco" />
          </label>
          @if (durationError()) {
            <p class="pe-warn">{{ 'akten.zeiten.pos.durationError' | transloco }}</p>
          }
        }

        @if (mode() !== 'start') {
          <div class="row">
            <label class="fld">
              <span class="lbl">{{ 'akten.zeiten.pos.start' | transloco }}</span>
              <input type="datetime-local" [ngModel]="startLocal()" (ngModelChange)="startLocal.set($event)" />
            </label>
            <label class="fld">
              <span class="lbl">{{ 'akten.zeiten.pos.stop' | transloco }}</span>
              <input type="datetime-local" [ngModel]="stopLocal()" (ngModelChange)="stopLocal.set($event)" />
            </label>
          </div>
        }

        <div class="row">
          <label class="fld">
            <span class="lbl">{{ 'akten.zeiten.pos.rate' | transloco }}</span>
            <input type="number" step="0.01" [ngModel]="unitPrice()" (ngModelChange)="unitPrice.set(toNum($event))" />
          </label>
          <label class="fld">
            <span class="lbl">{{ 'akten.zeiten.pos.tax' | transloco }}</span>
            <input type="number" step="0.1" [ngModel]="taxRate()" (ngModelChange)="taxRate.set(toNum($event))" />
          </label>
        </div>
      </div>

      <footer class="df">
        <span class="spacer"></span>
        <button type="button" class="btn" (click)="close.emit()">{{ 'akten.editor.cancel' | transloco }}</button>
        <button type="button" class="btn primary" [disabled]="!canSave()" (click)="submit()">
          {{ (mode() === 'start' ? 'akten.zeiten.pos.startBtn' : 'akten.editor.save') | transloco }}
        </button>
      </footer>
    </div>
  `,
  styles: [`
    :host { position: fixed; inset: 0; z-index: 60; display: grid; place-items: center; }
    .backdrop { position: absolute; inset: 0; background: rgba(6, 14, 22, .5); }
    .dialog { position: relative; width: min(560px, 95vw); max-height: 92dvh; display: flex; flex-direction: column;
      background: var(--jl-surface); border: 1px solid var(--jl-line); border-radius: 14px; box-shadow: 0 24px 60px rgba(0,0,0,.32); overflow: hidden; }
    .dh { flex: none; display: flex; align-items: center; gap: 12px; padding: 14px 18px; border-bottom: 1px solid var(--jl-line); }
    .dh h2 { margin: 0; font-size: 1.05rem; font-weight: 800; flex: 1; }
    .x { display: inline-grid; place-items: center; width: 32px; height: 32px; border-radius: 8px; border: 0; background: transparent; color: var(--jl-ink-soft); cursor: pointer; }
    .x:hover { background: var(--jl-surface-alt); }
    .db { flex: 1 1 auto; min-height: 0; overflow-y: auto; padding: 14px 18px; display: flex; flex-direction: column; gap: 12px; }
    .pe-warn { margin: 0; font-size: .82rem; color: #8a5300; background: #fff4e0; border: 1px solid #f0c67a; border-radius: 8px; padding: 8px 11px; }
    @media (prefers-color-scheme: dark) { .pe-warn { color: #f0c67a; background: #3a2c10; border-color: #6b5320; } }
    .row { display: flex; gap: 12px; }
    .fld { display: flex; flex-direction: column; gap: 5px; flex: 1; min-width: 0; }
    .lbl { font-size: .72rem; font-weight: 700; color: var(--jl-ink-faint); text-transform: uppercase; letter-spacing: .03em; }
    input, select, textarea { font: inherit; font-size: .88rem; padding: 8px 10px; border: 1px solid var(--jl-line-strong); border-radius: 8px; background: var(--jl-surface); color: var(--jl-ink); width: 100%; }
    input:focus, select:focus, textarea:focus { outline: none; border-color: var(--jl-blue); }
    textarea { resize: vertical; }
    .df { flex: none; display: flex; align-items: center; gap: 10px; padding: 12px 18px; border-top: 1px solid var(--jl-line); }
    .spacer { flex: 1; }
    .btn { font: inherit; font-size: .85rem; font-weight: 650; padding: 8px 16px; border-radius: 8px; border: 1px solid var(--jl-line-strong); background: var(--jl-surface); color: var(--jl-ink); cursor: pointer; }
    .btn:hover:not(:disabled) { border-color: var(--jl-blue); }
    .btn:disabled { opacity: .5; cursor: default; }
    .btn.primary { background: var(--jl-blue); border-color: var(--jl-blue); color: #fff; }
    @media (max-width: 520px) { .row { flex-direction: column; } }
  `],
})
export class PositionEditorComponent implements OnInit {
  readonly mode = input.required<PositionMode>();
  readonly timesheetId = input.required<string>();
  /** The position to edit (edit mode only). */
  readonly position = input<TimesheetPosition | null>(null);
  /** In 'start' mode: warn that another timer is already running (server setting parallellogswarning). */
  readonly parallelWarning = input(false);

  readonly save = output<PositionEditResult>();
  readonly close = output<void>();

  private readonly cases = inject(CasesService);
  private readonly auth = inject(AuthService);
  private readonly tracking = inject(TimesheetTrackingService);

  /** Manual quick-entry: a duration; when applied, start/stop are back-calculated from now. */
  protected readonly durationRaw = signal('');
  protected readonly durationError = signal(false);

  protected readonly templates = signal<PositionTemplate[]>([]);
  protected readonly users = signal<CaseUserRef[]>([]);
  protected readonly name = signal('');
  protected readonly description = signal('');
  protected readonly startLocal = signal('');
  protected readonly stopLocal = signal('');
  protected readonly unitPrice = signal(0);
  protected readonly taxRate = signal(19);
  /** Login name of the user the time is logged for (defaults to the current user). */
  protected readonly principal = signal('');

  protected readonly canSave = computed(() =>
    !!this.name().trim() && !!this.principal() && (this.mode() === 'start' || (!!this.startLocal() && !!this.stopLocal())));

  /** True once the selected principal is present in the loaded user list (else we keep a stand-in option). */
  protected readonly userInList = computed(() => this.users().some((u) => u.principalId === this.principal()));

  ngOnInit(): void {
    const p = this.position();
    if (p) {
      this.name.set(p.name);
      this.description.set(p.description);
      this.startLocal.set(toLocalInput(p.started));
      this.stopLocal.set(toLocalInput(p.stopped));
      this.unitPrice.set(p.unitPrice);
      this.taxRate.set(p.taxRate);
      this.principal.set(p.principal || this.auth.user()?.username || '');
    } else {
      this.principal.set(this.auth.user()?.username || '');
      if (this.mode() === 'manual') {
        const now = toLocalInput(new Date().toISOString());
        this.startLocal.set(now);
        this.stopLocal.set(now);
      }
    }
    this.cases.positionTemplates(this.timesheetId()).subscribe((t) => this.templates.set(t));
    this.cases.users().subscribe((u) => this.users.set(u));
  }

  protected applyTemplate(id: string): void {
    const t = this.templates().find((x) => x.id === id);
    if (!t) {
      return;
    }
    this.name.set(t.name);
    if (t.description) {
      this.description.set(t.description);
    }
    this.unitPrice.set(t.unitPrice);
    this.taxRate.set(t.taxRate);
  }

  protected toNum(v: unknown): number {
    const n = Number(v);
    return Number.isFinite(n) ? n : 0;
  }

  /**
   * Parses a manual duration into minutes. Accepts `h:mm`, or a number with an explicit unit
   * (`h`/`std` = hours, `m`/`min` = minutes); a bare number (no unit) is interpreted per the server
   * setting `numericInput` (minutes / hours / reject). Returns null on an invalid or rejected value.
   */
  private durationToMinutes(raw: string): number | null {
    const s = raw.trim().toLowerCase().replace(',', '.');
    if (!s) { return null; }
    // "h:mm"
    const hm = /^(\d+):([0-5]?\d)$/.exec(s);
    if (hm) { return (+hm[1]) * 60 + (+hm[2]); }
    // explicit units, hours and/or minutes combined: "2h", "15m", "2h15m", "2h 15m", "1.5h"
    const u = /^(?:(\d+(?:\.\d+)?)\s*(?:h|std))?\s*(?:(\d+(?:\.\d+)?)\s*(?:m|min))?$/.exec(s);
    if (u && (u[1] !== undefined || u[2] !== undefined)) {
      const mins = Math.round((u[1] ? parseFloat(u[1]) * 60 : 0) + (u[2] ? parseFloat(u[2]) : 0));
      return mins > 0 ? mins : null;
    }
    // a bare number without a unit -> interpret per server setting
    const bare = /^(\d+(?:\.\d+)?)$/.exec(s);
    if (bare) {
      const num = parseFloat(bare[1]);
      if (!Number.isFinite(num) || num <= 0) { return null; }
      const fmt = this.tracking.numericInput();
      if (fmt === 'hours') { return Math.round(num * 60); }
      if (fmt === 'minutes') { return Math.round(num); }
      return null; // 'reject'
    }
    return null;
  }

  /** Applies the quick-entry duration by back-calculating start (= now − duration) and stop (= now). */
  protected applyDuration(): void {
    const raw = this.durationRaw();
    if (!raw.trim()) { this.durationError.set(false); return; }
    const mins = this.durationToMinutes(raw);
    if (mins === null) { this.durationError.set(true); return; }
    this.durationError.set(false);
    const now = new Date();
    const start = new Date(now.getTime() - mins * 60000);
    this.stopLocal.set(toLocalInput(now.toISOString()));
    this.startLocal.set(toLocalInput(start.toISOString()));
  }

  protected submit(): void {
    if (!this.canSave()) {
      return;
    }
    this.save.emit({
      mode: this.mode(),
      positionId: this.position()?.id,
      name: this.name().trim(),
      description: this.description().trim(),
      startLocal: this.startLocal(),
      stopLocal: this.stopLocal(),
      unitPrice: this.unitPrice(),
      taxRate: this.taxRate(),
      principal: this.principal(),
    });
  }
}

/** ISO timestamp -> "yyyy-MM-ddTHH:mm" for a datetime-local input (local time); '' when empty. */
function toLocalInput(iso: string): string {
  if (!iso) {
    return '';
  }
  const d = new Date(iso);
  if (isNaN(d.getTime())) {
    return '';
  }
  const p = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}`;
}
