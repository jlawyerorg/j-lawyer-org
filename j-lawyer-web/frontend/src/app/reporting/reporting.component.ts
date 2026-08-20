import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { ReportingService } from './reporting.service';
import { BarChart, ReportGroup, ReportMeta, ReportResult, ReportTable } from './reporting.models';

/** A bar chart transformed for CSS rendering. */
interface ChartVM {
  name: string;
  xTitle: string;
  yTitle: string;
  legend: { name: string; color: string }[];
  columns: { label: string; bars: { heightPct: number; color: string; value: string; neg: boolean }[] }[];
}

/** A table transformed for rendering (case-id column hidden, sum row flagged). */
interface TableVM {
  name: string;
  columns: string[];
  rows: { caseId: string | null; cells: string[]; isSum: boolean }[];
}

const PALETTE = [
  'var(--jl-blue)', 'var(--jl-red)', 'var(--jl-green)', 'var(--jl-warning)', '#0a5b90',
  '#8e44ad', '#16a085', '#d35400', '#2c3e50', '#c0392b', '#27ae60', '#2980b9',
];

/**
 * Auswertungen (reporting) module — master-detail list of the server-defined reports
 * (GET /v7/reports/list) grouped by category; selecting one runs it (POST /v7/reports/invoke)
 * and renders the resulting tables and bar charts. Reports carrying a date-selection label get
 * a from/to range. Table rows that reference a case deep-link to it. ACL-restricted server-side.
 */
@Component({
  selector: 'jl-reporting',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent],
  template: `
    <div class="master-detail" [class.show-detail]="selectedId()">
      <section class="list">
        <header class="list-head">
          <h1>{{ 'reporting.title' | transloco }}</h1>
          <span class="count">{{ reports()?.length ?? 0 }}</span>
        </header>
        <div class="rows">
          @if (listLoading()) {
            <p class="muted pad">{{ 'reporting.loading' | transloco }}</p>
          } @else if (listError()) {
            <p class="pad">
              {{ 'reporting.error' | transloco }}
              <button type="button" class="btn-retry" (click)="loadList()">{{ 'reporting.retry' | transloco }}</button>
            </p>
          } @else {
            @for (g of groups(); track g.category) {
              <div class="grp">
                <div class="grp-h">{{ g.category }}</div>
                @for (r of g.reports; track r.reportId) {
                  <button type="button" class="row" [class.sel]="r.reportId === selectedId()" (click)="select(r)">
                    <span class="rn">{{ r.name }}</span>
                    @if (r.description) { <span class="rd">{{ r.description }}</span> }
                    <span class="rtypes">
                      @if (r.typeTable) { <jl-icon name="doc" [size]="13" /> }
                      @if (r.typeChart) { <jl-icon name="chart" [size]="13" /> }
                    </span>
                  </button>
                }
              </div>
            } @empty {
              <p class="muted pad">{{ 'reporting.empty' | transloco }}</p>
            }
          }
        </div>
      </section>

      <section class="detail">
        @if (selected(); as m) {
          <div class="detail-head">
            <button type="button" class="back" (click)="clearSelection()">‹ {{ 'reporting.back' | transloco }}</button>
            <div class="title-row">
              <h2>{{ m.name }}</h2>
              @if (m.securityType === 'CONFIDENTIAL') {
                <span class="sec">{{ 'reporting.confidential' | transloco }}</span>
              }
            </div>
            @if (m.description) { <p class="desc">{{ m.description }}</p> }

            <div class="controls">
              @if (m.dateSelectionLabel) {
                <label class="df">
                  <span>{{ 'reporting.from' | transloco }}</span>
                  <input type="date" [value]="fromDate()" (change)="fromDate.set($any($event.target).value)" />
                </label>
                <label class="df">
                  <span>{{ 'reporting.to' | transloco }}</span>
                  <input type="date" [value]="toDate()" (change)="toDate.set($any($event.target).value)" />
                </label>
                <span class="dsl">{{ m.dateSelectionLabel }}</span>
              }
              <button type="button" class="btn-primary" (click)="run()" [disabled]="runLoading()">
                {{ 'reporting.run' | transloco }}
              </button>
            </div>
          </div>

          <div class="detail-body">
            @if (runLoading()) {
              <p class="muted pad">{{ 'reporting.running' | transloco }}</p>
            } @else if (runError()) {
              <p class="pad">
                {{ 'reporting.runError' | transloco }}
                <button type="button" class="btn-retry" (click)="run()">{{ 'reporting.retry' | transloco }}</button>
              </p>
            } @else if (result()) {
              @for (c of charts(); track c.name) {
                <div class="card">
                  <div class="card-h"><h3>{{ c.name || m.name }}</h3></div>
                  <div class="card-b">
                    @if (c.legend.length) {
                      <div class="legend">
                        @for (l of c.legend; track l.name) {
                          <span class="lg"><span class="lg-dot" [style.background]="l.color"></span>{{ l.name }}</span>
                        }
                      </div>
                    }
                    <div class="chart" [attr.aria-label]="c.name">
                      @for (col of c.columns; track $index) {
                        <div class="bar-col">
                          <div class="bar-stack">
                            @for (b of col.bars; track $index) {
                              <span class="bar" [class.neg]="b.neg" [style.height.%]="b.heightPct"
                                    [style.background]="b.color" [title]="b.value"></span>
                            }
                          </div>
                          <span class="bar-x" [title]="col.label">{{ col.label }}</span>
                        </div>
                      }
                    </div>
                    @if (c.yTitle || c.xTitle) {
                      <p class="axis">{{ c.yTitle }}@if (c.yTitle && c.xTitle) { · }{{ c.xTitle }}</p>
                    }
                  </div>
                </div>
              }

              @for (t of tables(); track t.name) {
                <div class="card">
                  <div class="card-h">
                    <h3>{{ t.name || m.name }}</h3>
                    <span class="card-count">{{ t.rows.length }}</span>
                  </div>
                  <div class="card-b">
                    @if (t.rows.length) {
                      <div class="rpt-scroll">
                        <table class="rpt-table">
                          <thead>
                            <tr>@for (col of t.columns; track $index) { <th>{{ col }}</th> }</tr>
                          </thead>
                          <tbody>
                            @for (row of t.rows; track $index) {
                              <tr [class.sum]="row.isSum" [class.link]="row.caseId"
                                  (click)="row.caseId && openCase(row.caseId)">
                                @for (cell of row.cells; track $index) { <td>{{ cell }}</td> }
                              </tr>
                            }
                          </tbody>
                        </table>
                      </div>
                    } @else {
                      <p class="muted">{{ 'reporting.noRows' | transloco }}</p>
                    }
                  </div>
                </div>
              }

              @if (!charts().length && !tables().length) {
                <p class="muted pad">{{ 'reporting.noData' | transloco }}</p>
              }
            }
          </div>
        } @else {
          <p class="empty detail-empty">{{ 'reporting.selectHint' | transloco }}</p>
        }
      </section>
    </div>
  `,
  styleUrl: './reporting.component.css',
})
export class ReportingComponent {
  private readonly reportsApi = inject(ReportingService);
  private readonly router = inject(Router);

  protected readonly reports = signal<ReportMeta[] | null>(null);
  protected readonly listLoading = signal(false);
  protected readonly listError = signal(false);

  protected readonly selectedId = signal<string | null>(null);
  protected readonly selected = computed(() => this.reports()?.find((r) => r.reportId === this.selectedId()) ?? null);

  protected readonly fromDate = signal(startOfYearIso());
  protected readonly toDate = signal(endOfYearIso());

  protected readonly result = signal<ReportResult | null>(null);
  protected readonly runLoading = signal(false);
  protected readonly runError = signal(false);

  private runSeq = 0;

  /** Reports grouped by category, categories and reports each in stable order. */
  protected readonly groups = computed<ReportGroup[]>(() => {
    const byCat = new Map<string, ReportMeta[]>();
    for (const r of this.reports() ?? []) {
      (byCat.get(r.category) ?? byCat.set(r.category, []).get(r.category)!).push(r);
    }
    return [...byCat.entries()]
      .map(([category, reports]) => ({ category, reports: [...reports].sort((a, b) => a.sequence - b.sequence || a.name.localeCompare(b.name)) }))
      .sort((a, b) => a.category.localeCompare(b.category));
  });

  protected readonly charts = computed<ChartVM[]>(() => (this.result()?.barCharts ?? []).map(toChartVM));
  protected readonly tables = computed<TableVM[]>(() => (this.result()?.tables ?? []).map(toTableVM));

  constructor() {
    this.loadList();
  }

  protected loadList(): void {
    this.listLoading.set(true);
    this.listError.set(false);
    this.reportsApi.list().subscribe({
      next: (rows) => {
        this.reports.set(rows);
        this.listLoading.set(false);
      },
      error: () => {
        this.listError.set(true);
        this.listLoading.set(false);
      },
    });
  }

  /** Selects a report and runs it immediately (with the current/default date range). */
  protected select(meta: ReportMeta): void {
    this.selectedId.set(meta.reportId);
    this.result.set(null);
    this.run();
  }

  /** Runs the selected report; date range is sent only for reports that take one. */
  protected run(): void {
    const m = this.selected();
    if (!m) {
      return;
    }
    // The server needs a date range for every report; reports without a user-facing date
    // selection (dateSelectionLabel empty) are run over a wide fixed range so they include all
    // data (mirrors the desktop client, which always supplies defaults). Empty dates -> 500.
    const useDates = !!m.dateSelectionLabel;
    const from = useDates ? this.fromDate() : '1970-01-01';
    const to = useDates ? this.toDate() : '2099-12-31';
    const seq = ++this.runSeq;
    this.runLoading.set(true);
    this.runError.set(false);
    this.reportsApi.invoke(m.reportId, from, to).subscribe({
      next: (res) => {
        if (seq !== this.runSeq) {
          return;
        }
        this.result.set(res);
        this.runLoading.set(false);
      },
      error: () => {
        if (seq !== this.runSeq) {
          return;
        }
        this.runError.set(true);
        this.runLoading.set(false);
      },
    });
  }

  protected openCase(caseId: string): void {
    this.router.navigate(['/cases', caseId]);
  }

  protected clearSelection(): void {
    this.selectedId.set(null);
    this.result.set(null);
  }
}

function toChartVM(chart: BarChart): ChartVM {
  const series = chart.series ?? [];
  const labels = series[0]?.xData ?? [];
  // Bars are stacked per x value, so the scale is the largest per-column sum (a year's total),
  // which keeps many-series charts (e.g. "cases per year by subject area") readable.
  const colValues = labels.map((_, i) => series.map((s) => Number(s.yData?.[i] ?? 0) || 0));
  const max = Math.max(1, ...colValues.map((col) => col.reduce((sum, v) => sum + Math.abs(v), 0)));
  const columns = labels.map((label, i) => ({
    label,
    bars: series.map((s, si) => {
      const value = colValues[i][si];
      return {
        heightPct: (Math.abs(value) / max) * 100,
        color: colorOf(si, s.fillColor),
        value: `${s.name ? s.name + ': ' : ''}${value}`,
        neg: value < 0,
      };
    }).filter((b) => b.heightPct > 0),
  }));
  return {
    name: chart.chartName ?? '',
    xTitle: chart.xAxisTitle ?? '',
    yTitle: chart.yAxisTitle ?? '',
    legend: series.length > 1 ? series.map((s, si) => ({ name: s.name ?? '', color: colorOf(si, s.fillColor) })) : [],
    columns,
  };
}

function toTableVM(table: ReportTable): TableVM {
  const hasCaseId = !!table.hasCaseIdColumn;
  const columns = hasCaseId ? (table.columnNames ?? []).slice(1) : (table.columnNames ?? []);
  const src = table.rows ?? [];
  const rows = src.map((r, idx) => ({
    caseId: hasCaseId ? (r[0] ?? null) : null,
    cells: hasCaseId ? r.slice(1) : r,
    isSum: !!table.hasSumRows && idx === src.length - 1,
  }));
  return { name: table.tableName ?? '', columns, rows };
}

function colorOf(index: number, fill: string | null): string {
  if (fill && fill.startsWith('#')) {
    return fill;
  }
  return PALETTE[index % PALETTE.length];
}

/** Local yyyy-MM-dd for Jan 1st of the current year (sensible default range start). */
function startOfYearIso(): string {
  return toIso(new Date(new Date().getFullYear(), 0, 1));
}

/** Local yyyy-MM-dd for Dec 31st of the current year (default range end; covers future-due entries). */
function endOfYearIso(): string {
  return toIso(new Date(new Date().getFullYear(), 11, 31));
}

function toIso(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}
