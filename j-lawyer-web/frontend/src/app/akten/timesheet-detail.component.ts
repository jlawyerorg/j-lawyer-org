import { ChangeDetectionStrategy, Component, computed, input, OnInit, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe, DecimalPipe } from '@angular/common';
import { interval } from 'rxjs';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { CaseTimesheet, TimesheetPosition } from './case.models';
import { formatDurationMs, positionMillis, runningElapsed, sumDuration, sumInvoiceable, sumNet } from './timesheet.util';

/**
 * Full-list detail dialog for one timesheet (project). Because a project can hold hundreds of
 * entries, the Zeiten tab only shows KPIs + running timers; this modal renders the complete,
 * searchable positions table with per-row edit/delete/stop actions. It is presentational: all
 * writes are emitted to the parent (which owns the position editor + REST calls).
 */
@Component({
  selector: 'jl-timesheet-detail',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent, DatePipe, DecimalPipe],
  template: `
    <div class="backdrop" (click)="close.emit()"></div>
    <div class="dialog" role="dialog" aria-modal="true">
      <header class="dh">
        <h2>{{ sheet().name || '—' }}</h2>
        <span class="ts-status" [class.closed]="sheet().status === 20">
          {{ (sheet().status === 20 ? 'akten.zeiten.closed' : 'akten.zeiten.open') | transloco }}
        </span>
        <span class="spacer"></span>
        <button type="button" class="x" (click)="close.emit()" [attr.aria-label]="'akten.editor.cancel' | transloco">
          <jl-icon name="close" [size]="18" />
        </button>
      </header>

      <div class="db">
        <div class="ts-kpis">
          <div class="fin-kpi">
            <span class="fk-label">{{ 'akten.zeiten.duration' | transloco }}</span>
            <span class="fk-value">{{ duration() }} h</span>
          </div>
          <div class="fin-kpi">
            <span class="fk-label">{{ 'akten.zeiten.net' | transloco }}</span>
            <span class="fk-value">{{ net() | number: '1.2-2' }} €</span>
          </div>
          <div class="fin-kpi">
            <span class="fk-label">{{ 'akten.zeiten.invoiceable' | transloco }}</span>
            <span class="fk-value">{{ invoiceable() | number: '1.2-2' }} €</span>
          </div>
          @if (sheet().limited) {
            <div class="fin-kpi">
              <span class="fk-label">{{ 'akten.zeiten.limit' | transloco }}</span>
              <span class="fk-value">{{ sheet().limit | number: '1.2-2' }} € · {{ sheet().percentageDone | number: '1.0-0' }} %</span>
            </div>
          }
        </div>

        <div class="detail-tools">
          @if (sheet().status !== 20) {
            <button type="button" class="add-btn" (click)="startTimer.emit()">
              <jl-icon name="clock" [size]="14" /> {{ 'akten.zeiten.startTimer' | transloco }}
            </button>
            <button type="button" class="add-btn ghost" (click)="addManual.emit()">
              <jl-icon name="plus" [size]="14" /> {{ 'akten.zeiten.addManual' | transloco }}
            </button>
          }
          <span class="spacer"></span>
          <label class="detail-search">
            <jl-icon name="search" [size]="14" />
            <input type="search" [value]="term()" (input)="term.set($any($event.target).value)"
                   [placeholder]="'akten.zeiten.searchEntries' | transloco" />
          </label>
        </div>

        <div class="ts-scroll">
          <table class="ts-table">
            <thead>
              <tr>
                <th>{{ 'akten.zeiten.col.description' | transloco }}</th>
                <th>{{ 'akten.zeiten.col.user' | transloco }}</th>
                <th>{{ 'akten.zeiten.col.started' | transloco }}</th>
                <th class="num">{{ 'akten.zeiten.col.duration' | transloco }}</th>
                <th class="num">{{ 'akten.zeiten.col.rate' | transloco }}</th>
                <th class="num">{{ 'akten.zeiten.col.total' | transloco }}</th>
                <th>{{ 'akten.zeiten.col.billing' | transloco }}</th>
                <th class="ts-act-col"></th>
              </tr>
            </thead>
            <tbody>
              @for (p of visible(); track p.id) {
                <tr [class.running]="p.running">
                  <td>{{ p.name || p.description || '—' }}</td>
                  <td>{{ p.principal || '—' }}</td>
                  <td>{{ p.started ? (p.started | date: 'dd.MM.yyyy HH:mm') : '—' }}</td>
                  <td class="num">
                    @if (p.running) {
                      <span class="ts-run">{{ elapsed(p) }}</span>
                    } @else { {{ durationOf(p) }} }
                  </td>
                  <td class="num">{{ p.unitPrice | number: '1.2-2' }} €</td>
                  <td class="num">{{ p.total | number: '1.2-2' }} €</td>
                  <td>
                    @if (p.invoiceId) {
                      <span class="ts-badge billed">{{ 'akten.zeiten.billed' | transloco }}</span>
                    } @else {
                      <span class="ts-badge open">{{ 'akten.zeiten.unbilled' | transloco }}</span>
                    }
                  </td>
                  <td class="ts-act-col">
                    <span class="row-actions">
                      @if (p.running) {
                        <button type="button" class="row-edit stop" [disabled]="busyId() === p.id"
                                (click)="stopPosition.emit(p)" [title]="'akten.zeiten.stopTimer' | transloco">
                          <jl-icon name="stop" [size]="15" />
                        </button>
                      } @else if (!p.invoiceId) {
                        <button type="button" class="row-edit" (click)="editPosition.emit(p)" [title]="'akten.zeiten.pos.edit' | transloco">
                          <jl-icon name="edit" [size]="15" />
                        </button>
                        <button type="button" class="row-del" [disabled]="busyId() === p.id"
                                (click)="deletePosition.emit(p)" [title]="'akten.zeiten.pos.delete' | transloco">
                          <jl-icon name="trash" [size]="15" />
                        </button>
                      }
                    </span>
                  </td>
                </tr>
              } @empty {
                <tr><td colspan="8" class="muted pad">{{ (term() ? 'akten.zeiten.noEntryMatch' : 'akten.zeiten.noPositions') | transloco }}</td></tr>
              }
            </tbody>
          </table>
        </div>
        <p class="detail-count">{{ 'akten.zeiten.entryCount' | transloco: { shown: visible().length, total: positions().length } }}</p>
      </div>
    </div>
  `,
  styles: [`
    :host { position: fixed; inset: 0; z-index: 55; display: grid; place-items: center; }
    .backdrop { position: absolute; inset: 0; background: rgba(6, 14, 22, .5); }
    .dialog { position: relative; width: min(1000px, 96vw); max-height: 92dvh; display: flex; flex-direction: column;
      background: var(--jl-surface); border: 1px solid var(--jl-line); border-radius: 14px; box-shadow: 0 24px 60px rgba(0,0,0,.32); overflow: hidden; }
    .dh { flex: none; display: flex; align-items: center; gap: 12px; padding: 14px 18px; border-bottom: 1px solid var(--jl-line); }
    .dh h2 { margin: 0; font-size: 1.05rem; font-weight: 800; }
    .dh .spacer { flex: 1; }
    .ts-status { font-size: .66rem; font-weight: 700; text-transform: uppercase; letter-spacing: .04em; padding: 2px 9px;
      border-radius: 9px; color: var(--jl-green, #1f9d55); background: color-mix(in srgb, var(--jl-green, #1f9d55) 14%, transparent); }
    .ts-status.closed { color: var(--jl-ink-soft); background: var(--jl-surface-alt); }
    .x { display: inline-grid; place-items: center; width: 32px; height: 32px; border-radius: 8px; border: 0; background: transparent; color: var(--jl-ink-soft); cursor: pointer; }
    .x:hover { background: var(--jl-surface-alt); }
    .db { flex: 1 1 auto; min-height: 0; overflow-y: auto; padding: 14px 18px; display: flex; flex-direction: column; gap: 12px; }
    .ts-kpis { display: flex; flex-wrap: wrap; gap: 10px 28px; }
    .fin-kpi { display: flex; flex-direction: column; gap: 2px; }
    .fk-label { font-size: .68rem; text-transform: uppercase; letter-spacing: .04em; color: var(--jl-ink-faint); font-weight: 700; }
    .fk-value { font-size: 1.02rem; font-weight: 800; font-variant-numeric: tabular-nums; }
    .detail-tools { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; }
    .detail-tools .spacer { flex: 1; }
    .add-btn { display: inline-flex; align-items: center; gap: 6px; font: inherit; font-size: .82rem; font-weight: 650;
      padding: 7px 12px; border-radius: 8px; border: 1px solid transparent; cursor: pointer;
      background: color-mix(in srgb, var(--jl-blue) 12%, transparent); color: var(--jl-blue); }
    .add-btn:hover { background: color-mix(in srgb, var(--jl-blue) 18%, transparent); }
    .add-btn.ghost { background: transparent; border-color: var(--jl-line-strong); color: var(--jl-ink-soft); }
    .add-btn.ghost:hover { border-color: var(--jl-blue); color: var(--jl-blue); background: color-mix(in srgb, var(--jl-blue) 8%, transparent); }
    .detail-search { display: flex; align-items: center; gap: 8px; background: var(--jl-surface-alt); border: 1px solid var(--jl-line);
      border-radius: 8px; padding: 6px 10px; color: var(--jl-ink-soft); min-width: 220px; }
    .detail-search input { all: unset; flex: 1; color: var(--jl-ink); min-width: 0; font: inherit; font-size: .84rem; }
    .ts-scroll { overflow-x: auto; max-width: 100%; }
    .ts-table { width: 100%; border-collapse: collapse; font-size: .84rem; white-space: nowrap; }
    .ts-table th, .ts-table td { padding: 7px 10px; text-align: left; border-bottom: 1px solid var(--jl-line); }
    .ts-table thead th { position: sticky; top: 0; background: var(--jl-surface); font-size: .66rem; text-transform: uppercase;
      letter-spacing: .05em; color: var(--jl-ink-faint); font-weight: 700; }
    .ts-table .num { text-align: right; font-variant-numeric: tabular-nums; }
    .ts-table .ts-act-col { text-align: right; width: 1%; white-space: nowrap; }
    .ts-table tbody tr:hover { background: var(--jl-surface-alt); }
    .ts-table tbody tr.running { background: color-mix(in srgb, var(--jl-blue) 7%, transparent); }
    .ts-run { color: var(--jl-blue); font-weight: 700; }
    .ts-badge { font-size: .66rem; font-weight: 700; text-transform: uppercase; letter-spacing: .04em; padding: 1px 8px; border-radius: 9px; }
    .ts-badge.billed { color: var(--jl-blue); background: color-mix(in srgb, var(--jl-blue) 12%, transparent); }
    .ts-badge.open { color: var(--jl-ink-soft); background: var(--jl-surface-alt); }
    .row-actions { display: inline-flex; align-items: center; gap: 4px; justify-content: flex-end; }
    .row-edit, .row-del { display: inline-grid; place-items: center; width: 28px; height: 28px; border-radius: 7px;
      border: 1px solid var(--jl-line-strong); background: var(--jl-surface); color: var(--jl-ink-soft); cursor: pointer; }
    .row-edit:hover { border-color: var(--jl-blue); color: var(--jl-blue); background: color-mix(in srgb, var(--jl-blue) 10%, transparent); }
    .row-del:hover { border-color: var(--jl-red); color: var(--jl-red); background: color-mix(in srgb, var(--jl-red) 10%, transparent); }
    .row-edit.stop { border-color: var(--jl-red); color: var(--jl-red); }
    .row-edit.stop:hover { background: color-mix(in srgb, var(--jl-red) 10%, transparent); }
    .row-del:disabled, .row-edit.stop:disabled { opacity: .4; cursor: default; }
    .muted { color: var(--jl-ink-faint); }
    .pad { padding: 16px 10px; text-align: center; }
    .detail-count { margin: 0; font-size: .74rem; color: var(--jl-ink-faint); }
  `],
})
export class TimesheetDetailComponent implements OnInit {
  readonly sheet = input.required<CaseTimesheet>();
  readonly positions = input.required<TimesheetPosition[]>();
  /** Id of a position currently being written (disables its buttons). */
  readonly busyId = input<string | null>(null);

  readonly startTimer = output<void>();
  readonly addManual = output<void>();
  readonly editPosition = output<TimesheetPosition>();
  readonly stopPosition = output<TimesheetPosition>();
  readonly deletePosition = output<TimesheetPosition>();
  readonly close = output<void>();

  protected readonly term = signal('');
  protected readonly now = signal(Date.now());

  protected readonly net = computed(() => sumNet(this.positions()));
  protected readonly invoiceable = computed(() => sumInvoiceable(this.positions()));
  protected readonly duration = computed(() => sumDuration(this.positions()));

  /** Positions filtered by the search term (name/description/user), newest first. */
  protected readonly visible = computed(() => {
    const t = this.term().trim().toLowerCase();
    const rows = this.positions().filter((p) => !t
      || p.name.toLowerCase().includes(t)
      || p.description.toLowerCase().includes(t)
      || p.principal.toLowerCase().includes(t));
    return [...rows].sort((a, b) => (a.started < b.started ? 1 : a.started > b.started ? -1 : 0));
  });

  ngOnInit(): void {
    // Live-update running timers once per second while the dialog is open.
    interval(1000).pipe(takeUntilDestroyed()).subscribe(() => {
      if (this.positions().some((p) => p.running)) {
        this.now.set(Date.now());
      }
    });
  }

  protected elapsed(p: TimesheetPosition): string {
    return runningElapsed(p, this.now());
  }

  protected durationOf(p: TimesheetPosition): string {
    const ms = positionMillis(p);
    return ms > 0 ? formatDurationMs(ms) : '—';
  }
}
