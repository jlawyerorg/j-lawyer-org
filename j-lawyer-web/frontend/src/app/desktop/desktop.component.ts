import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { AuthService } from '../core/auth/auth.service';
import { DashboardService } from './dashboard.service';
import { DueItem, InvoiceSummary, RecentCase } from './desktop.models';

/**
 * "Mein Desktop" — a read-only dashboard overview mirroring the Swing client's DesktopPanel
 * widgets: recently changed cases, open deadlines/follow-ups (due) and open invoices. Each
 * widget is assembled from an existing REST endpoint (no dedicated backend) and links into
 * the relevant module via the case deep link (/cases/:id). OpenSpec change {@code add-web-client}.
 */
@Component({
  selector: 'jl-desktop',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent, RouterLink, DatePipe, DecimalPipe],
  template: `
    <section class="dash">
      <header class="dash-head">
        <h1>{{ 'desktop.title' | transloco }}</h1>
        <p class="greeting">{{ 'desktop.greeting' | transloco: { name: userName() } }}</p>
      </header>

      <div class="grid">
        <!-- Zuletzt geändert -->
        <article class="widget">
          <div class="w-head">
            <jl-icon name="cases" [size]="16" />
            <h2>{{ 'desktop.recent.title' | transloco }}</h2>
            <a class="w-more" routerLink="/cases">{{ 'desktop.all' | transloco }} ›</a>
          </div>
          <div class="w-body">
            @if (recentLoading()) {
              <p class="muted pad">{{ 'desktop.loading' | transloco }}</p>
            } @else if (recentError()) {
              <p class="pad">{{ 'desktop.error' | transloco }}
                <button type="button" class="btn-retry" (click)="loadRecent()">{{ 'desktop.retry' | transloco }}</button>
              </p>
            } @else {
              @for (c of recent(); track c.id) {
                <a class="row case-row" [routerLink]="['/cases', c.id]">
                  <span class="az">{{ c.fileNumber }}</span>
                  <span class="rmain">
                    <span class="rname">{{ c.name }}</span>
                    <span class="rsub">{{ c.subjectField || '—' }}{{ c.lawyer ? ' · ' + c.lawyer : '' }}</span>
                  </span>
                  <span class="rdate">{{ c.lastChanged | date: 'dd.MM.yy' }}</span>
                </a>
              } @empty {
                <p class="muted pad">{{ 'desktop.recent.empty' | transloco }}</p>
              }
            }
          </div>
        </article>

        <!-- Fällig -->
        <article class="widget">
          <div class="w-head">
            <jl-icon name="calendar" [size]="16" />
            <h2>{{ 'desktop.due.title' | transloco }}</h2>
            <a class="w-more" routerLink="/calendar">{{ 'desktop.all' | transloco }} ›</a>
          </div>
          <div class="w-body">
            @if (dueLoading()) {
              <p class="muted pad">{{ 'desktop.loading' | transloco }}</p>
            } @else if (dueError()) {
              <p class="pad">{{ 'desktop.error' | transloco }}
                <button type="button" class="btn-retry" (click)="loadDue()">{{ 'desktop.retry' | transloco }}</button>
              </p>
            } @else {
              @for (d of due(); track d.id) {
                <a class="row due-row" [class.overdue]="d.overdue"
                   [routerLink]="d.caseId ? ['/cases', d.caseId] : null">
                  <span class="ddate" [class.od]="d.overdue">{{ d.due | date: 'dd.MM.' }}</span>
                  <span class="bar" [class]="d.type"></span>
                  <span class="rmain">
                    <span class="rname">{{ d.summary || ('desktop.due.noSummary' | transloco) }}</span>
                    <span class="rsub">
                      @if (d.caseFileNumber) { {{ d.caseFileNumber }} · {{ d.caseName }} }
                      @if (d.assignee) { · {{ d.assignee }} }
                    </span>
                  </span>
                  <span class="pill" [class]="d.type">{{ 'desktop.due.type.' + d.type | transloco }}</span>
                </a>
              } @empty {
                <p class="muted pad">{{ 'desktop.due.empty' | transloco }}</p>
              }
            }
          </div>
        </article>

        <!-- Offene Rechnungen -->
        <article class="widget">
          <div class="w-head">
            <jl-icon name="euro" [size]="16" />
            <h2>{{ 'desktop.invoices.title' | transloco }}</h2>
            <a class="w-more" routerLink="/finance">{{ 'desktop.all' | transloco }} ›</a>
          </div>
          <div class="w-body">
            @if (invLoading()) {
              <p class="muted pad">{{ 'desktop.loading' | transloco }}</p>
            } @else if (invError()) {
              <p class="pad">{{ 'desktop.error' | transloco }}
                <button type="button" class="btn-retry" (click)="loadInvoices()">{{ 'desktop.retry' | transloco }}</button>
              </p>
            } @else {
              @if (invoices(); as s) {
                <div class="kpis">
                  <div class="kpi">
                    <span class="k-value">{{ s.count }}</span>
                    <span class="k-label">{{ 'desktop.invoices.open' | transloco }}</span>
                  </div>
                  <div class="kpi">
                    <span class="k-value">{{ s.totalGross | number: '1.2-2' }} {{ s.currency }}</span>
                    <span class="k-label">{{ 'desktop.invoices.sum' | transloco }}</span>
                  </div>
                </div>
                @for (i of s.top; track i.id) {
                  <a class="row inv-row" [routerLink]="i.caseId ? ['/cases', i.caseId] : null">
                    <span class="rmain">
                      <span class="rname">{{ i.invoiceNumber || '—' }}</span>
                      <span class="rsub">{{ i.status }}</span>
                    </span>
                    <span class="inv-amount">{{ i.totalGross | number: '1.2-2' }} {{ i.currency }}</span>
                  </a>
                } @empty {
                  <p class="muted pad">{{ 'desktop.invoices.empty' | transloco }}</p>
                }
              }
            }
          </div>
        </article>
      </div>
    </section>
  `,
  styleUrl: './desktop.component.css',
})
export class DesktopComponent {
  private readonly dashboard = inject(DashboardService);
  private readonly auth = inject(AuthService);

  protected readonly userName = computed(() => this.auth.user()?.displayName ?? '');

  protected readonly recent = signal<RecentCase[]>([]);
  protected readonly recentLoading = signal(true);
  protected readonly recentError = signal(false);

  protected readonly due = signal<DueItem[]>([]);
  protected readonly dueLoading = signal(true);
  protected readonly dueError = signal(false);

  protected readonly invoices = signal<InvoiceSummary | null>(null);
  protected readonly invLoading = signal(true);
  protected readonly invError = signal(false);

  constructor() {
    this.loadRecent();
    this.loadDue();
    this.loadInvoices();
  }

  protected loadRecent(): void {
    this.recentLoading.set(true);
    this.recentError.set(false);
    this.dashboard.recentCases(8).subscribe({
      next: (rows) => { this.recent.set(rows); this.recentLoading.set(false); },
      error: () => { this.recentError.set(true); this.recentLoading.set(false); },
    });
  }

  protected loadDue(): void {
    this.dueLoading.set(true);
    this.dueError.set(false);
    this.dashboard.dueItems().subscribe({
      next: (rows) => { this.due.set(rows); this.dueLoading.set(false); },
      error: () => { this.dueError.set(true); this.dueLoading.set(false); },
    });
  }

  protected loadInvoices(): void {
    this.invLoading.set(true);
    this.invError.set(false);
    this.dashboard.openInvoices(6).subscribe({
      next: (s) => { this.invoices.set(s); this.invLoading.set(false); },
      error: () => { this.invError.set(true); this.invLoading.set(false); },
    });
  }
}
