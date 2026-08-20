import { ChangeDetectionStrategy, Component, inject, OnInit, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { interval } from 'rxjs';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { CasesService } from './cases.service';
import { TimesheetTrackingService } from './timesheet-tracking.service';
import { CaseOverview, CaseTimesheet, PositionWrite, RunningPosition } from './case.models';
import { PositionEditorComponent, PositionEditResult, PositionMode } from './position-editor.component';
import { runningElapsed, toServerDateTime } from './timesheet.util';

/**
 * Cross-case time-tracking dialog opened from the header stopwatch. Two parts: a live list of the
 * current user's running timers (with stop buttons), and a "new entry" flow — search a case, pick
 * one of its open projects, then start a stopwatch or log time manually (via the shared position
 * editor, which carries the user dropdown). Mirrors the desktop TimesheetLogDialog in spirit.
 */
@Component({
  selector: 'jl-timesheet-log',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent, PositionEditorComponent],
  template: `
    <div class="backdrop" (click)="close.emit()"></div>
    <div class="dialog" role="dialog" aria-modal="true">
      <header class="dh">
        <jl-icon name="clock" [size]="18" />
        <h2>{{ 'timelog.title' | transloco }}</h2>
        <span class="spacer"></span>
        <button type="button" class="x" (click)="close.emit()" [attr.aria-label]="'akten.editor.cancel' | transloco">
          <jl-icon name="close" [size]="18" />
        </button>
      </header>

      <div class="db">
        <!-- Running timers -->
        <section class="blk">
          <h3 class="blk-h">
            {{ 'timelog.running' | transloco }}
            @if (running().length) { <span class="rc">{{ running().length }}</span> }
          </h3>
          @if (loadingRunning()) {
            <p class="muted">{{ 'akten.loading' | transloco }}</p>
          } @else {
            @for (r of running(); track r.position.id) {
              <div class="run-row">
                <span class="run-main">
                  <span class="run-title">{{ r.position.name || r.position.description || '—' }}</span>
                  <span class="run-sub">
                    {{ r.timesheet.caseFileNumber || '—' }}<span class="dot">·</span>{{ r.timesheet.name || '—' }}<span class="dot">·</span>{{ r.position.principal }}
                  </span>
                </span>
                <span class="run-time">{{ elapsed(r) }}</span>
                <button type="button" class="stop-btn" [disabled]="busyId() === r.position.id"
                        (click)="stop(r)" [title]="'akten.zeiten.stopTimer' | transloco">
                  <jl-icon name="stop" [size]="15" />
                </button>
              </div>
            } @empty {
              <p class="muted">{{ 'timelog.noRunning' | transloco }}</p>
            }
          }
        </section>

        <!-- New entry -->
        <section class="blk">
          <h3 class="blk-h">{{ 'timelog.newEntry' | transloco }}</h3>
          <label class="fld">
            <span class="lbl">{{ 'timelog.case' | transloco }}</span>
            <input type="search" [value]="caseTerm()" (input)="onCaseSearch($any($event.target).value)"
                   [placeholder]="'timelog.casePlaceholder' | transloco" />
          </label>
          @if (caseResults().length && !selectedCase()) {
            <div class="results">
              @for (c of caseResults(); track c.id) {
                <button type="button" class="res" (click)="selectCase(c)">
                  <span class="res-az">{{ c.fileNumber }}</span>
                  <span class="res-nm">{{ c.name }}</span>
                </button>
              }
            </div>
          }
          @if (selectedCase(); as sc) {
            <div class="chosen">
              <span class="res-az">{{ sc.fileNumber }}</span>
              <span class="res-nm">{{ sc.name }}</span>
              <button type="button" class="unpick" (click)="clearCase()" [title]="'timelog.changeCase' | transloco">✕</button>
            </div>
            <label class="fld">
              <span class="lbl">{{ 'timelog.project' | transloco }}</span>
              @if (loadingProjects()) {
                <span class="muted small">{{ 'akten.loading' | transloco }}</span>
              } @else if (projects().length) {
                <select [value]="selectedProjectId()" (change)="selectedProjectId.set($any($event.target).value)">
                  <option value="">—</option>
                  @for (p of projects(); track p.id) { <option [value]="p.id">{{ p.name }}</option> }
                </select>
              } @else {
                <span class="muted small">{{ 'timelog.noProjects' | transloco }}</span>
              }
            </label>
            <div class="cap-tools">
              <button type="button" class="add-btn" [disabled]="!selectedProjectId()" (click)="openCapture('start')">
                <jl-icon name="clock" [size]="14" /> {{ 'akten.zeiten.startTimer' | transloco }}
              </button>
              <button type="button" class="add-btn ghost" [disabled]="!selectedProjectId()" (click)="openCapture('manual')">
                <jl-icon name="plus" [size]="14" /> {{ 'akten.zeiten.addManual' | transloco }}
              </button>
            </div>
          }
        </section>
      </div>
    </div>

    @if (capture(); as cap) {
      <jl-position-editor [mode]="cap.mode" [timesheetId]="cap.timesheetId" [position]="null"
                          [parallelWarning]="tracking.warnBeforeParallelStart()"
                          (save)="onCapture($event)" (close)="capture.set(null)" />
    }
  `,
  styles: [`
    :host { position: fixed; inset: 0; z-index: 58; display: grid; place-items: center; }
    .backdrop { position: absolute; inset: 0; background: rgba(6, 14, 22, .5); }
    .dialog { position: relative; width: min(560px, 95vw); max-height: 92dvh; display: flex; flex-direction: column;
      background: var(--jl-surface); border: 1px solid var(--jl-line); border-radius: 14px; box-shadow: 0 24px 60px rgba(0,0,0,.32); overflow: hidden; }
    .dh { flex: none; display: flex; align-items: center; gap: 10px; padding: 14px 18px; border-bottom: 1px solid var(--jl-line); color: var(--jl-ink); }
    .dh h2 { margin: 0; font-size: 1.05rem; font-weight: 800; }
    .dh .spacer { flex: 1; }
    .x { display: inline-grid; place-items: center; width: 32px; height: 32px; border-radius: 8px; border: 0; background: transparent; color: var(--jl-ink-soft); cursor: pointer; }
    .x:hover { background: var(--jl-surface-alt); }
    .db { flex: 1 1 auto; min-height: 0; overflow-y: auto; padding: 14px 18px; display: flex; flex-direction: column; gap: 20px; }
    .blk-h { margin: 0 0 10px; font-size: .82rem; font-weight: 800; text-transform: uppercase; letter-spacing: .03em; color: var(--jl-ink-faint); display: flex; align-items: center; gap: 8px; }
    .rc { background: var(--jl-blue); color: #fff; font-size: .68rem; font-weight: 700; padding: 1px 8px; border-radius: 9px; }
    .run-row { display: flex; align-items: center; gap: 10px; padding: 8px 10px; border: 1px solid var(--jl-line); border-radius: 9px; margin-bottom: 6px;
      background: color-mix(in srgb, var(--jl-blue) 6%, transparent); }
    .run-main { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
    .run-title { font-weight: 650; font-size: .9rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .run-sub { font-size: .76rem; color: var(--jl-ink-soft); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .run-sub .dot { margin: 0 5px; opacity: .6; }
    .run-time { font-variant-numeric: tabular-nums; font-weight: 800; color: var(--jl-blue); font-size: .95rem; }
    .stop-btn { display: inline-grid; place-items: center; width: 30px; height: 30px; border-radius: 8px; border: 1px solid var(--jl-red);
      background: var(--jl-surface); color: var(--jl-red); cursor: pointer; }
    .stop-btn:hover:not(:disabled) { background: color-mix(in srgb, var(--jl-red) 12%, transparent); }
    .stop-btn:disabled { opacity: .4; cursor: default; }
    .fld { display: flex; flex-direction: column; gap: 5px; margin-bottom: 10px; }
    .lbl { font-size: .72rem; font-weight: 700; color: var(--jl-ink-faint); text-transform: uppercase; letter-spacing: .03em; }
    input, select { font: inherit; font-size: .88rem; padding: 8px 10px; border: 1px solid var(--jl-line-strong); border-radius: 8px; background: var(--jl-surface); color: var(--jl-ink); width: 100%; }
    input:focus, select:focus { outline: none; border-color: var(--jl-blue); }
    .results { border: 1px solid var(--jl-line); border-radius: 9px; overflow: hidden; margin-bottom: 10px; max-height: 220px; overflow-y: auto; }
    .res { display: flex; gap: 10px; align-items: baseline; width: 100%; text-align: left; padding: 8px 12px; background: var(--jl-surface); border: 0; border-bottom: 1px solid var(--jl-line); cursor: pointer; }
    .res:last-child { border-bottom: 0; }
    .res:hover { background: var(--jl-surface-alt); }
    .res-az { font-weight: 700; font-size: .82rem; color: var(--jl-blue); white-space: nowrap; }
    .res-nm { font-size: .84rem; color: var(--jl-ink); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .chosen { display: flex; align-items: center; gap: 10px; padding: 8px 12px; border: 1px solid var(--jl-line-strong); border-radius: 9px; margin-bottom: 10px; }
    .chosen .res-nm { flex: 1; }
    .unpick { margin-left: auto; border: 0; background: transparent; color: var(--jl-ink-soft); cursor: pointer; font-size: .9rem; }
    .cap-tools { display: flex; flex-wrap: wrap; gap: 8px; }
    .add-btn { display: inline-flex; align-items: center; gap: 6px; font: inherit; font-size: .82rem; font-weight: 650;
      padding: 8px 14px; border-radius: 8px; border: 1px solid transparent; cursor: pointer;
      background: color-mix(in srgb, var(--jl-blue) 12%, transparent); color: var(--jl-blue); }
    .add-btn:hover:not(:disabled) { background: color-mix(in srgb, var(--jl-blue) 18%, transparent); }
    .add-btn:disabled { opacity: .45; cursor: default; }
    .add-btn.ghost { background: transparent; border-color: var(--jl-line-strong); color: var(--jl-ink-soft); }
    .add-btn.ghost:hover:not(:disabled) { border-color: var(--jl-blue); color: var(--jl-blue); background: color-mix(in srgb, var(--jl-blue) 8%, transparent); }
    .muted { color: var(--jl-ink-faint); }
    .small { font-size: .82rem; }
  `],
})
export class TimesheetLogComponent implements OnInit {
  readonly close = output<void>();

  private readonly cases = inject(CasesService);
  protected readonly tracking = inject(TimesheetTrackingService);

  protected readonly running = signal<RunningPosition[]>([]);
  protected readonly loadingRunning = signal(false);
  protected readonly busyId = signal<string | null>(null);
  protected readonly now = signal(Date.now());

  protected readonly caseTerm = signal('');
  protected readonly caseResults = signal<CaseOverview[]>([]);
  protected readonly selectedCase = signal<CaseOverview | null>(null);
  protected readonly projects = signal<CaseTimesheet[]>([]);
  protected readonly loadingProjects = signal(false);
  protected readonly selectedProjectId = signal('');
  protected readonly capture = signal<{ mode: PositionMode; timesheetId: string } | null>(null);

  private searchDebounce: ReturnType<typeof setTimeout> | null = null;

  ngOnInit(): void {
    this.loadRunning();
    interval(1000).pipe(takeUntilDestroyed()).subscribe(() => {
      if (this.running().some((r) => r.position.running)) {
        this.now.set(Date.now());
      }
    });
  }

  private loadRunning(): void {
    this.loadingRunning.set(true);
    this.cases.runningPositions().subscribe((rows) => {
      this.running.set(rows);
      this.loadingRunning.set(false);
      this.tracking.refresh();
    });
  }

  protected elapsed(r: RunningPosition): string {
    return runningElapsed(r.position, this.now());
  }

  protected stop(r: RunningPosition): void {
    if (this.busyId()) {
      return;
    }
    this.busyId.set(r.position.id);
    const p = r.position;
    this.cases.stopPosition(r.timesheet.id, p.id, {
      name: p.name, description: p.description, unitPrice: p.unitPrice, taxRate: p.taxRate, principal: p.principal,
    }).subscribe({
      next: () => { this.busyId.set(null); this.loadRunning(); },
      error: () => this.busyId.set(null),
    });
  }

  protected onCaseSearch(term: string): void {
    this.caseTerm.set(term);
    this.selectedCase.set(null);
    if (this.searchDebounce) {
      clearTimeout(this.searchDebounce);
    }
    const q = term.trim();
    if (!q) {
      this.caseResults.set([]);
      return;
    }
    this.searchDebounce = setTimeout(() => {
      this.cases.searchCases(q).subscribe((rows) => this.caseResults.set(rows));
    }, 250);
  }

  protected selectCase(c: CaseOverview): void {
    this.selectedCase.set(c);
    this.caseResults.set([]);
    this.projects.set([]);
    this.selectedProjectId.set('');
    this.loadingProjects.set(true);
    this.cases.timesheets(c.id).subscribe((sheets) => {
      // Only open projects can receive new time.
      this.projects.set(sheets.filter((s) => s.status !== 20));
      this.loadingProjects.set(false);
    });
  }

  protected clearCase(): void {
    this.selectedCase.set(null);
    this.projects.set([]);
    this.selectedProjectId.set('');
    this.caseTerm.set('');
  }

  protected openCapture(mode: PositionMode): void {
    const id = this.selectedProjectId();
    if (id) {
      this.capture.set({ mode, timesheetId: id });
    }
  }

  protected onCapture(r: PositionEditResult): void {
    const ctx = this.capture();
    if (!ctx) {
      return;
    }
    const base: PositionWrite = {
      name: r.name, description: r.description, unitPrice: r.unitPrice, taxRate: r.taxRate, principal: r.principal,
    };
    const call = r.mode === 'start'
      ? this.cases.startPosition(ctx.timesheetId, base)
      : this.cases.addPosition(ctx.timesheetId, {
          ...base, started: toServerDateTime(r.startLocal), stopped: toServerDateTime(r.stopLocal),
        });
    call.subscribe({
      next: () => { this.capture.set(null); this.loadRunning(); },
      error: () => undefined,
    });
  }
}
