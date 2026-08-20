import { ChangeDetectionStrategy, Component, inject, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslocoModule } from '@jsverse/transloco';
import { DecimalPipe } from '@angular/common';
import { IconComponent } from '../shared/icon.component';
import { CasesService } from './cases.service';
import { CaseTimesheet, PositionTemplate, TimesheetWrite } from './case.models';

/** The project editor's payload: the timesheet data plus the chosen allowed position templates. */
export interface TimesheetSaveResult {
  data: TimesheetWrite;
  /** The allowed positions / hourly rates for the project (empty = no restriction). */
  templates: PositionTemplate[];
}

/**
 * Modal editor for a timesheet (project). Create (no input) or edit an existing one: name,
 * description, rounding interval, an optional net budget cap and the open/closed status. Emits the
 * payload on save — the parent does the REST call. Rendered only while open (mounted via @if).
 */
@Component({
  selector: 'jl-timesheet-editor',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, TranslocoModule, IconComponent, DecimalPipe],
  template: `
    <div class="backdrop" (click)="close.emit()"></div>
    <div class="dialog" role="dialog" aria-modal="true">
      <header class="dh">
        <h2>{{ (sheet() ? 'akten.zeiten.editProject' : 'akten.zeiten.newProject') | transloco }}</h2>
        <button type="button" class="x" (click)="close.emit()" [attr.aria-label]="'akten.editor.cancel' | transloco">
          <jl-icon name="close" [size]="18" />
        </button>
      </header>

      <div class="db">
        <label class="fld">
          <span class="lbl">{{ 'akten.zeiten.field.name' | transloco }}</span>
          <input type="text" [ngModel]="f().name" (ngModelChange)="upd('name', $event)" />
        </label>
        <label class="fld">
          <span class="lbl">{{ 'akten.zeiten.field.description' | transloco }}</span>
          <textarea rows="2" [ngModel]="f().description" (ngModelChange)="upd('description', $event)"></textarea>
        </label>
        <div class="row">
          <label class="fld">
            <span class="lbl">{{ 'akten.zeiten.field.interval' | transloco }}</span>
            <select [ngModel]="f().interval" (ngModelChange)="upd('interval', +$event)">
              @for (i of intervals(); track i) { <option [value]="i">{{ i }} min</option> }
            </select>
          </label>
          <label class="fld">
            <span class="lbl">{{ 'akten.zeiten.field.status' | transloco }}</span>
            <select [ngModel]="f().status" (ngModelChange)="upd('status', +$event)">
              <option [value]="10">{{ 'akten.zeiten.open' | transloco }}</option>
              <option [value]="20">{{ 'akten.zeiten.closed' | transloco }}</option>
            </select>
          </label>
        </div>
        <label class="chk">
          <input type="checkbox" [ngModel]="f().limited" (ngModelChange)="upd('limited', $event)" />
          {{ 'akten.zeiten.field.limited' | transloco }}
        </label>
        @if (f().limited) {
          <label class="fld">
            <span class="lbl">{{ 'akten.zeiten.field.limit' | transloco }}</span>
            <input type="number" step="0.01" [ngModel]="f().limit" (ngModelChange)="upd('limit', toNum($event))" />
          </label>
        }

        <!-- Allowed positions / hourly rates: restrict which position templates (and thus rates) can be booked. -->
        <div class="fld">
          <span class="lbl">{{ 'akten.zeiten.field.allowedPositions' | transloco }}</span>
          @if (allTemplates() === null) {
            <span class="muted small">{{ 'akten.loading' | transloco }}</span>
          } @else if (!allTemplates()?.length) {
            <span class="muted small">{{ 'akten.zeiten.tpl.none' | transloco }}</span>
          } @else {
            <div class="tpl-head">
              <span class="hint">{{ 'akten.zeiten.tpl.hint' | transloco }}</span>
              <span class="spacer"></span>
              <button type="button" class="tpl-bulk" (click)="selectAll()">{{ 'akten.zeiten.tpl.selectAll' | transloco }}</button>
              <button type="button" class="tpl-bulk" (click)="selectNone()">{{ 'akten.zeiten.tpl.selectNone' | transloco }}</button>
            </div>
            <div class="tpl-list">
              @for (t of allTemplates(); track t.id) {
                <label class="tpl-row" [class.on]="selected().has(t.id)">
                  <input type="checkbox" [checked]="selected().has(t.id)" (change)="toggle(t.id)" />
                  <span class="tpl-name">{{ t.name || '—' }}</span>
                  <span class="tpl-rate">{{ t.unitPrice | number: '1.2-2' }} €/h · {{ t.taxRate | number: '1.0-2' }} %</span>
                </label>
              }
            </div>
            <span class="hint">{{ 'akten.zeiten.tpl.selected' | transloco: { n: selected().size } }}</span>
          }
        </div>
      </div>

      <footer class="df">
        <span class="spacer"></span>
        <button type="button" class="btn" (click)="close.emit()">{{ 'akten.editor.cancel' | transloco }}</button>
        <button type="button" class="btn primary" [disabled]="!f().name.trim()" (click)="submit()">{{ 'akten.editor.save' | transloco }}</button>
      </footer>
    </div>
  `,
  styles: [`
    :host { position: fixed; inset: 0; z-index: 60; display: grid; place-items: center; }
    .backdrop { position: absolute; inset: 0; background: rgba(6, 14, 22, .5); }
    .dialog { position: relative; width: min(520px, 95vw); max-height: 92dvh; display: flex; flex-direction: column;
      background: var(--jl-surface); border: 1px solid var(--jl-line); border-radius: 14px; box-shadow: 0 24px 60px rgba(0,0,0,.32); overflow: hidden; }
    .dh { flex: none; display: flex; align-items: center; gap: 12px; padding: 14px 18px; border-bottom: 1px solid var(--jl-line); }
    .dh h2 { margin: 0; font-size: 1.05rem; font-weight: 800; flex: 1; }
    .x { display: inline-grid; place-items: center; width: 32px; height: 32px; border-radius: 8px; border: 0; background: transparent; color: var(--jl-ink-soft); cursor: pointer; }
    .x:hover { background: var(--jl-surface-alt); }
    .db { flex: 1 1 auto; min-height: 0; overflow-y: auto; padding: 14px 18px; display: flex; flex-direction: column; gap: 12px; }
    .row { display: flex; gap: 12px; }
    .fld { display: flex; flex-direction: column; gap: 5px; flex: 1; min-width: 0; }
    .lbl { font-size: .72rem; font-weight: 700; color: var(--jl-ink-faint); text-transform: uppercase; letter-spacing: .03em; }
    input, select, textarea { font: inherit; font-size: .88rem; padding: 8px 10px; border: 1px solid var(--jl-line-strong); border-radius: 8px; background: var(--jl-surface); color: var(--jl-ink); width: 100%; }
    input:focus, select:focus, textarea:focus { outline: none; border-color: var(--jl-blue); }
    textarea { resize: vertical; }
    .chk { display: flex; align-items: center; gap: 8px; font-size: .86rem; font-weight: 600; }
    .chk input { width: auto; }
    .muted { color: var(--jl-ink-faint); }
    .small { font-size: .82rem; }
    .hint { font-size: .74rem; color: var(--jl-ink-faint); }
    .tpl-head { display: flex; align-items: center; gap: 8px; }
    .tpl-head .spacer { flex: 1; }
    .tpl-bulk { font: inherit; font-size: .74rem; font-weight: 650; padding: 3px 8px; border-radius: 6px;
      border: 1px solid var(--jl-line-strong); background: var(--jl-surface); color: var(--jl-ink-soft); cursor: pointer; }
    .tpl-bulk:hover { border-color: var(--jl-blue); color: var(--jl-blue); background: color-mix(in srgb, var(--jl-blue) 8%, transparent); }
    .tpl-list { display: flex; flex-direction: column; gap: 4px; max-height: 200px; overflow-y: auto;
      border: 1px solid var(--jl-line); border-radius: 8px; padding: 6px; }
    .tpl-row { display: flex; align-items: center; gap: 10px; padding: 6px 8px; border-radius: 6px; cursor: pointer; }
    .tpl-row:hover { background: var(--jl-surface-alt); }
    .tpl-row.on { background: color-mix(in srgb, var(--jl-blue) 8%, transparent); }
    .tpl-row input { width: auto; flex: none; }
    .tpl-name { flex: 1; min-width: 0; font-size: .86rem; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .tpl-rate { font-size: .78rem; color: var(--jl-ink-soft); font-variant-numeric: tabular-nums; white-space: nowrap; }
    .df { flex: none; display: flex; align-items: center; gap: 10px; padding: 12px 18px; border-top: 1px solid var(--jl-line); }
    .spacer { flex: 1; }
    .btn { font: inherit; font-size: .85rem; font-weight: 650; padding: 8px 16px; border-radius: 8px; border: 1px solid var(--jl-line-strong); background: var(--jl-surface); color: var(--jl-ink); cursor: pointer; }
    .btn:hover:not(:disabled) { border-color: var(--jl-blue); }
    .btn:disabled { opacity: .5; cursor: default; }
    .btn.primary { background: var(--jl-blue); border-color: var(--jl-blue); color: #fff; }
  `],
})
export class TimesheetEditorComponent implements OnInit {
  /** The timesheet to edit, or null to create a new one. */
  readonly sheet = input<CaseTimesheet | null>(null);

  readonly save = output<TimesheetSaveResult>();
  readonly close = output<void>();

  private readonly cases = inject(CasesService);

  protected readonly intervals = signal<number[]>([1, 5, 10, 15, 30, 60]);
  protected readonly f = signal<TimesheetWrite>({ name: '', description: '', interval: 15, limited: false, limit: 0, status: 10 });
  /** The global template pool (null while loading). */
  protected readonly allTemplates = signal<PositionTemplate[] | null>(null);
  /** Ids of the templates allowed for this project. */
  protected readonly selected = signal<Set<string>>(new Set());

  ngOnInit(): void {
    const s = this.sheet();
    if (s) {
      this.f.set({ id: s.id, name: s.name, description: s.description, interval: s.interval || 15, limited: s.limited, limit: s.limit, status: s.status || 10 });
      // Pre-check the project's currently allowed positions (the server returns all when unrestricted).
      this.cases.positionTemplates(s.id).subscribe((tpls) => this.selected.set(new Set(tpls.map((t) => t.id))));
    }
    this.cases.allTimesheetTemplates().subscribe((t) => {
      this.allTemplates.set(t);
      // A new project defaults to all positions allowed (matches the desktop), so pre-check the pool.
      if (!this.sheet()) {
        this.selected.set(new Set(t.map((x) => x.id)));
      }
    });
    this.cases.timesheetIntervals().subscribe((ivs) => {
      this.intervals.set(ivs);
      // Ensure the current interval is offered even if it's not in the configured set.
      if (!ivs.includes(this.f().interval)) {
        this.intervals.update((cur) => [...cur, this.f().interval].sort((a, b) => a - b));
      }
    });
  }

  protected toNum(v: unknown): number {
    const n = Number(v);
    return Number.isFinite(n) ? n : 0;
  }

  protected upd<K extends keyof TimesheetWrite>(key: K, value: TimesheetWrite[K]): void {
    this.f.update((cur) => ({ ...cur, [key]: value }));
  }

  /** Toggles a template in/out of the project's allowed set. */
  protected toggle(id: string): void {
    this.selected.update((cur) => {
      const next = new Set(cur);
      if (next.has(id)) { next.delete(id); } else { next.add(id); }
      return next;
    });
  }

  /** Selects every template in the pool. */
  protected selectAll(): void {
    this.selected.set(new Set((this.allTemplates() ?? []).map((t) => t.id)));
  }

  /** Clears the whole selection. */
  protected selectNone(): void {
    this.selected.set(new Set());
  }

  protected submit(): void {
    if (!this.f().name.trim()) {
      return;
    }
    const templates = (this.allTemplates() ?? []).filter((t) => this.selected().has(t.id));
    this.save.emit({ data: this.f(), templates });
  }
}
