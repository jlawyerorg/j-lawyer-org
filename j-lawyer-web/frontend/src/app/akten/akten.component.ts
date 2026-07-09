import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { DocumentPreviewComponent } from '../shared/document-preview.component';
import { DocumentContentService } from '../shared/document-content.service';
import { PreviewDoc, previewKindOf } from '../shared/document-preview.models';
import { forkJoin } from 'rxjs';
import { CaseFilter, CasesService } from './cases.service';
import {
  AccountEntry, CaseDetail, CaseDocument, CaseHistoryEntry, CaseInvoice, CasePayment,
} from './case.models';

type CaseTab = 'overview' | 'documents' | 'parties' | 'deadlines' | 'finance' | 'history';
/** Sub-view within the finance tab (invoices, payments and the case account are too much for one screen). */
type FinanceView = 'invoices' | 'payments' | 'account';

/**
 * Akten (cases) module — responsive master-detail (design-mockup.html): searchable case
 * list + case detail (overview with parties, deadlines, recent documents, note). Data comes
 * from the real REST API via CasesService. The list is filtered/searched and paginated
 * server-side (infinite scroll via {@link CasesService.loadMore}); the detail is fetched
 * lazily per selection.
 */
@Component({
  selector: 'jl-akten',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent, DatePipe, DecimalPipe, DocumentPreviewComponent],
  template: `
    <div class="master-detail" [class.show-detail]="selectedId()">
      <!-- Aktenliste -->
      <section class="list">
        <header class="list-head">
          <h1>{{ 'akten.title' | transloco }}</h1>
          <span class="count">{{ cases.total() }}</span>
          <button type="button" class="btn-primary">
            <jl-icon name="plus" [size]="14" />{{ 'akten.new' | transloco }}
          </button>
        </header>

        <div class="filters" role="tablist">
          @for (f of filters; track f) {
            <button type="button" class="chip" [class.on]="filter() === f" (click)="setFilter(f)">
              {{ 'akten.filter.' + f | transloco }}
            </button>
          }
        </div>

        <label class="search">
          <jl-icon name="search" [size]="14" />
          <input type="search" [placeholder]="'akten.searchCases' | transloco"
                 [value]="search()" (input)="onSearch($any($event.target).value)" />
        </label>

        <div class="rows" (scroll)="onScroll($event)">
          @if (cases.listLoading() && cases.overviews().length === 0) {
            <p class="muted loading">{{ 'akten.loading' | transloco }}</p>
          } @else if (cases.listError() && cases.overviews().length === 0) {
            <p class="empty">
              {{ 'akten.error' | transloco }}
              <button type="button" class="btn-retry" (click)="cases.reload()">{{ 'akten.retry' | transloco }}</button>
            </p>
          } @else {
            @for (c of cases.overviews(); track c.id) {
              <button type="button" class="row" [class.sel]="c.id === selectedId()" (click)="open(c.id)">
                <span class="az">{{ c.fileNumber }}</span>
                <span class="name">{{ c.name }}</span>
                <span class="sub">{{ c.subjectField || c.reason || '—' }}{{ c.lawyer ? ' · ' + c.lawyer : '' }}</span>
                <span class="r-right">
                  <span class="pill" [class]="c.status">{{ 'akten.status.' + c.status | transloco }}</span>
                  <span class="date">{{ c.lastChanged | date: 'dd.MM.yy' }}</span>
                </span>
              </button>
            } @empty {
              <p class="empty">{{ 'akten.empty' | transloco }}</p>
            }
            @if (cases.listLoading() && cases.overviews().length > 0) {
              <p class="muted loading more">{{ 'akten.loadingMore' | transloco }}</p>
            }
          }
        </div>
      </section>

      <!-- Aktendetail -->
      <section class="detail">
        @if (detailLoading()) {
          <p class="muted detail-empty">{{ 'akten.loading' | transloco }}</p>
        } @else {
          @if (selected(); as c) {
          <div class="detail-head">
            <button type="button" class="back" (click)="clearSelection()">‹ {{ 'akten.back' | transloco }}</button>
            <div class="crumbs">{{ 'akten.title' | transloco }} › <b>{{ c.fileNumber }}</b></div>
            <div class="title-row">
              <h2>{{ c.name }}</h2>
              <span class="pill" [class]="c.status">{{ 'akten.status.' + c.status | transloco }}</span>
            </div>
            <div class="meta">
              <span><span class="k">{{ 'akten.meta.fileNumber' | transloco }}</span> <b>{{ c.fileNumber }}</b></span>
              <span><span class="k">{{ 'akten.meta.subjectField' | transloco }}</span> <b>{{ c.subjectField || '—' }}</b></span>
              <span><span class="k">{{ 'akten.meta.lawyer' | transloco }}</span> <b>{{ c.lawyer || '—' }}</b></span>
              <span><span class="k">{{ 'akten.meta.claimValue' | transloco }}</span> <b>{{ c.claimValue | number: '1.2-2' }} €</b></span>
            </div>
            <div class="tabs" role="tablist">
              @for (t of tabs; track t) {
                <button type="button" class="tab" [class.on]="activeTab() === t" (click)="selectTab(t)">
                  {{ 'akten.tabs.' + t | transloco }}
                </button>
              }
            </div>
          </div>

          <div class="detail-body">
            @if (activeTab() === 'overview') {
              <div class="grid">
                <!-- Beteiligte -->
                <div class="card">
                  <div class="card-h"><h3>{{ 'akten.parties' | transloco }}</h3></div>
                  <div class="card-b">
                    @for (p of c.parties; track p.id) {
                      <div class="party">
                        <span class="pa">{{ initials(p.contact || p.involvementType) }}</span>
                        <span>
                          <span class="role">{{ p.involvementType || '—' }}</span>
                          <span class="nm">{{ p.contact || '—' }}</span>
                        </span>
                      </div>
                    } @empty {
                      <p class="muted">{{ 'akten.noParties' | transloco }}</p>
                    }
                  </div>
                </div>

                <!-- Fristen -->
                <div class="card">
                  <div class="card-h"><h3>{{ 'akten.deadlines' | transloco }}</h3></div>
                  <div class="card-b">
                    @for (d of c.dueDates; track d.id) {
                      <div class="frist">
                        <span class="bar" [class.deadline]="d.type === 'deadline'"></span>
                        <span class="fdate">{{ d.dueDate | date: 'dd.MM.' }}</span>
                        <span class="fx">
                          <span class="ft">{{ d.reason }}</span>
                          <span class="fs">{{ d.assignee }} · {{ 'akten.dueType.' + d.type | transloco }}</span>
                        </span>
                      </div>
                    } @empty {
                      <p class="muted">{{ 'akten.noDeadlines' | transloco }}</p>
                    }
                  </div>
                </div>

                <!-- Dokumente -->
                <div class="card full">
                  <div class="card-h"><h3>{{ 'akten.recentDocs' | transloco }}</h3></div>
                  <div class="card-b">
                    @for (doc of c.documents; track doc.id) {
                      <div class="doc">
                        <span class="ext">{{ doc.ext }}</span>
                        <span>
                          <span class="dn">{{ doc.name }}</span>
                          <span class="dmeta">{{ doc.date | date: 'dd.MM.yyyy' }}</span>
                        </span>
                        <span class="dsz">{{ doc.size }}</span>
                      </div>
                    } @empty {
                      <p class="muted">{{ 'akten.noDocs' | transloco }}</p>
                    }
                  </div>
                </div>

                <!-- Notiz -->
                @if (c.notice) {
                  <div class="card full">
                    <div class="card-h"><h3>{{ 'akten.note' | transloco }}</h3></div>
                    <div class="card-b"><p class="note">{{ c.notice }}</p></div>
                  </div>
                }
              </div>
            } @else if (activeTab() === 'documents') {
              <div class="card full">
                <div class="card-h">
                  <h3>{{ 'akten.documents' | transloco }}</h3>
                  <span class="card-count">{{ c.documents.length }}</span>
                </div>
                <div class="card-b">
                  @for (doc of c.documents; track doc.id) {
                    <div class="doc doc-row" [class.previewable]="canPreview(doc)"
                         (click)="canPreview(doc) ? preview(doc) : download(doc)">
                      <span class="ext">{{ doc.ext || '—' }}</span>
                      <span class="doc-main">
                        <span class="dn">{{ doc.name }}</span>
                        <span class="dmeta">{{ doc.date | date: 'dd.MM.yyyy' }} · {{ doc.size }}</span>
                      </span>
                      <span class="doc-actions">
                        @if (canPreview(doc)) {
                          <button type="button" class="doc-btn" (click)="$event.stopPropagation(); preview(doc)">
                            {{ 'akten.docPreview' | transloco }}
                          </button>
                        }
                        <button type="button" class="doc-btn primary" (click)="$event.stopPropagation(); download(doc)">
                          <jl-icon name="download" [size]="14" />{{ 'akten.docDownload' | transloco }}
                        </button>
                      </span>
                    </div>
                  } @empty {
                    <p class="muted">{{ 'akten.noDocs' | transloco }}</p>
                  }
                </div>
              </div>
            } @else if (activeTab() === 'parties') {
              <div class="card full">
                <div class="card-h">
                  <h3>{{ 'akten.parties' | transloco }}</h3>
                  <span class="card-count">{{ c.parties.length }}</span>
                </div>
                <div class="card-b">
                  @for (p of c.parties; track p.id) {
                    <div class="party">
                      <span class="pa">{{ initials(p.contact || p.involvementType) }}</span>
                      <span>
                        <span class="role">{{ p.involvementType || '—' }}</span>
                        <span class="nm">{{ p.contact || '—' }}</span>
                      </span>
                    </div>
                  } @empty {
                    <p class="muted">{{ 'akten.noParties' | transloco }}</p>
                  }
                </div>
              </div>
            } @else if (activeTab() === 'deadlines') {
              <div class="card full">
                <div class="card-h">
                  <h3>{{ 'akten.deadlines' | transloco }}</h3>
                  <span class="card-count">{{ c.dueDates.length }}</span>
                </div>
                <div class="card-b">
                  @for (d of c.dueDates; track d.id) {
                    <div class="frist" [class.done]="d.done">
                      <span class="bar" [class.deadline]="d.type === 'deadline'"></span>
                      <span class="fdate">{{ d.dueDate | date: 'dd.MM.yyyy' }}</span>
                      <span class="fx">
                        <span class="ft">{{ d.reason }}</span>
                        <span class="fs">{{ d.assignee }} · {{ 'akten.dueType.' + d.type | transloco }}</span>
                      </span>
                      @if (d.done) { <jl-icon name="check" [size]="14" /> }
                    </div>
                  } @empty {
                    <p class="muted">{{ 'akten.noDeadlines' | transloco }}</p>
                  }
                </div>
              </div>
            } @else if (activeTab() === 'finance') {
              @if (financeLoading()) {
                <p class="muted tab-todo">{{ 'akten.loading' | transloco }}</p>
              } @else if (financeError()) {
                <p class="tab-todo">
                  {{ 'akten.error' | transloco }}
                  <button type="button" class="btn-retry" (click)="retryTab()">{{ 'akten.retry' | transloco }}</button>
                </p>
              } @else {
                <div class="grid">
                  <div class="card full finance-summary">
                    <div class="fin-kpi">
                      <span class="fk-label">{{ 'akten.finance.claimValue' | transloco }}</span>
                      <span class="fk-value">{{ c.claimValue | number: '1.2-2' }} €</span>
                    </div>
                    <div class="fin-kpi">
                      <span class="fk-label">{{ 'akten.finance.invoiced' | transloco }}</span>
                      <span class="fk-value">{{ sumGross(invoices()) | number: '1.2-2' }} €</span>
                    </div>
                    <div class="fin-kpi">
                      <span class="fk-label">{{ 'akten.finance.paid' | transloco }}</span>
                      <span class="fk-value">{{ sumPayments(payments()) | number: '1.2-2' }} €</span>
                    </div>
                    <div class="fin-kpi">
                      <span class="fk-label">{{ 'akten.finance.balance' | transloco }}</span>
                      <span class="fk-value" [class.neg]="accountBalance(accountEntries()) < 0">
                        {{ accountBalance(accountEntries()) | number: '1.2-2' }} €
                      </span>
                    </div>
                  </div>

                  <div class="fin-nav" role="tablist">
                    @for (v of financeViews; track v) {
                      <button type="button" class="fin-nav-btn" [class.on]="financeView() === v" (click)="financeView.set(v)">
                        {{ 'akten.finance.view.' + v | transloco }}
                        <span class="fin-nav-count">{{ financeCount(v) }}</span>
                      </button>
                    }
                  </div>

                  @switch (financeView()) {
                    @case ('invoices') {
                      <div class="card full">
                        <div class="card-b">
                          @for (inv of invoices(); track inv.id) {
                            <div class="fin-row">
                              <span class="fin-main">
                                <span class="dn">{{ inv.invoiceNumber || inv.name || '—' }}</span>
                                <span class="dmeta">
                                  {{ inv.status }}
                                  @if (inv.dueDate) { · {{ 'akten.finance.due' | transloco }} {{ inv.dueDate | date: 'dd.MM.yyyy' }} }
                                </span>
                              </span>
                              <span class="fin-amount">{{ inv.totalGross | number: '1.2-2' }} {{ inv.currency }}</span>
                            </div>
                          } @empty {
                            <p class="muted">{{ 'akten.finance.noInvoices' | transloco }}</p>
                          }
                        </div>
                      </div>
                    }
                    @case ('payments') {
                      <div class="card full">
                        <div class="card-b">
                          @for (pay of payments(); track pay.id) {
                            <div class="fin-row">
                              <span class="fin-main">
                                <span class="dn">{{ pay.name || pay.paymentNumber || pay.reason || '—' }}</span>
                                <span class="dmeta">
                                  {{ pay.status }}
                                  @if (pay.targetDate) { · {{ pay.targetDate | date: 'dd.MM.yyyy' }} }
                                </span>
                              </span>
                              <span class="fin-amount">{{ pay.total | number: '1.2-2' }} {{ pay.currency }}</span>
                            </div>
                          } @empty {
                            <p class="muted">{{ 'akten.finance.noPayments' | transloco }}</p>
                          }
                        </div>
                      </div>
                    }
                    @case ('account') {
                      <div class="card full">
                        <div class="card-b">
                          @if (accountRows().length) {
                            <div class="acct-scroll">
                              <table class="acct-table">
                                <thead>
                                  <tr>
                                    <th class="c-date">{{ 'akten.finance.acct.date' | transloco }}</th>
                                    <th class="c-desc">{{ 'akten.finance.acct.description' | transloco }}</th>
                                    <th class="c-contact">{{ 'akten.finance.acct.contact' | transloco }}</th>
                                    <th class="num">{{ 'akten.finance.acct.earnings' | transloco }}</th>
                                    <th class="num">{{ 'akten.finance.acct.spendings' | transloco }}</th>
                                    <th class="num">{{ 'akten.finance.acct.escrow' | transloco }}</th>
                                    <th class="num">{{ 'akten.finance.acct.expenditures' | transloco }}</th>
                                    <th class="num">{{ 'akten.finance.acct.balance' | transloco }}</th>
                                  </tr>
                                </thead>
                                <tbody>
                                  @for (row of accountRows(); track row.entry.id) {
                                    <tr>
                                      <td class="c-date">{{ row.entry.date | date: 'dd.MM.yyyy' }}</td>
                                      <td class="c-desc">{{ row.entry.description || '—' }}</td>
                                      <td class="c-contact">{{ row.entry.contact || '—' }}</td>
                                      <td class="num">{{ row.entry.earnings ? (row.entry.earnings | number: '1.2-2') : '—' }}</td>
                                      <td class="num">{{ row.entry.spendings ? (row.entry.spendings | number: '1.2-2') : '—' }}</td>
                                      <td class="num">{{ (row.entry.escrowIn - row.entry.escrowOut) ? ((row.entry.escrowIn - row.entry.escrowOut) | number: '1.2-2') : '—' }}</td>
                                      <td class="num">{{ (row.entry.expendituresIn - row.entry.expendituresOut) ? ((row.entry.expendituresIn - row.entry.expendituresOut) | number: '1.2-2') : '—' }}</td>
                                      <td class="num bal" [class.neg]="row.balance < 0">{{ row.balance | number: '1.2-2' }}</td>
                                    </tr>
                                  }
                                </tbody>
                              </table>
                            </div>
                          } @else {
                            <p class="muted">{{ 'akten.finance.noAccountEntries' | transloco }}</p>
                          }
                        </div>
                      </div>
                    }
                  }
                </div>
              }
            } @else if (activeTab() === 'history') {
              <div class="card full">
                <div class="card-h">
                  <h3>{{ 'akten.tabs.history' | transloco }}</h3>
                  @if (history()) { <span class="card-count">{{ history()?.length ?? 0 }}</span> }
                </div>
                <div class="card-b">
                  @if (historyLoading()) {
                    <p class="muted">{{ 'akten.loading' | transloco }}</p>
                  } @else if (historyError()) {
                    <p>
                      {{ 'akten.error' | transloco }}
                      <button type="button" class="btn-retry" (click)="retryTab()">{{ 'akten.retry' | transloco }}</button>
                    </p>
                  } @else {
                    @for (h of history(); track h.id) {
                      <div class="hist">
                        <span class="hist-dot"></span>
                        <span class="hist-body">
                          <span class="hist-desc">{{ h.changeDescription }}</span>
                          <span class="hist-meta">{{ h.changeDate | date: 'dd.MM.yyyy HH:mm' }} · {{ h.principal }}</span>
                        </span>
                      </div>
                    } @empty {
                      <p class="muted">{{ 'akten.noHistory' | transloco }}</p>
                    }
                  }
                </div>
              </div>
            }
          </div>
          } @else {
            <p class="empty detail-empty">{{ 'akten.selectHint' | transloco }}</p>
          }
        }
      </section>
    </div>

    <jl-document-preview [doc]="previewDoc()" (closed)="previewDoc.set(null)" />
  `,
  styleUrl: './akten.component.css',
})
export class AktenComponent {
  protected readonly cases = inject(CasesService);
  private readonly documents = inject(DocumentContentService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly tabs: CaseTab[] = ['overview', 'documents', 'parties', 'deadlines', 'finance', 'history'];
  protected readonly filters: CaseFilter[] = ['all', 'open', 'closed'];

  protected readonly filter = signal<CaseFilter>('all');
  protected readonly search = signal('');
  protected readonly activeTab = signal<CaseTab>('overview');
  protected readonly selectedId = signal<string | null>(null);
  protected readonly selected = signal<CaseDetail | null>(null);
  protected readonly detailLoading = signal(false);

  // Document preview — handed to the shared <jl-document-preview> overlay.
  protected readonly previewDoc = signal<PreviewDoc | null>(null);

  // History tab state (lazy-loaded per case)
  protected readonly history = signal<CaseHistoryEntry[] | null>(null);
  protected readonly historyLoading = signal(false);
  protected readonly historyError = signal(false);

  // Finance tab state (invoices + payments + case account, lazy-loaded per case)
  protected readonly invoices = signal<CaseInvoice[] | null>(null);
  protected readonly payments = signal<CasePayment[] | null>(null);
  protected readonly accountEntries = signal<AccountEntry[] | null>(null);
  protected readonly financeLoading = signal(false);
  protected readonly financeError = signal(false);
  /** Active sub-view of the finance tab. */
  protected readonly financeView = signal<FinanceView>('invoices');
  protected readonly financeViews: FinanceView[] = ['invoices', 'payments', 'account'];

  private autoSelected = false;
  private searchDebounce: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    this.cases.reload();
    // The route param (/cases/:id) is the source of truth for the selected case, so
    // deep links, browser back/forward and in-app navigation all stay in sync.
    this.route.paramMap.pipe(takeUntilDestroyed()).subscribe((params) => {
      const id = params.get('id');
      if (id) {
        this.autoSelected = true;
        if (id !== this.selectedId()) {
          this.select(id);
        }
      } else if (this.selectedId() !== null) {
        this.selectedId.set(null);
        this.selected.set(null);
      }
    });
    // On wide screens with no deep link, open the first case once the list arrives (once).
    effect(() => {
      const rows = this.cases.overviews();
      if (!this.autoSelected && rows.length && this.selectedId() === null && window.innerWidth > 680) {
        this.autoSelected = true;
        this.router.navigate(['/cases', rows[0].id], { replaceUrl: true });
      }
    });
  }

  /** True when the document can be previewed inline / in a new tab (vs. download-only). */
  protected canPreview(doc: CaseDocument): boolean {
    return previewKindOf(doc.ext) !== 'none';
  }

  /** Opens the shared preview overlay for a document (download-only kinds just download). */
  protected preview(doc: CaseDocument): void {
    if (previewKindOf(doc.ext) === 'none') {
      this.download(doc);
      return;
    }
    this.previewDoc.set({ id: doc.id, name: doc.name, ext: doc.ext });
  }

  /** Fetches a document's bytes and triggers a browser download with its file name. */
  protected download(doc: CaseDocument): void {
    this.documents.download({ id: doc.id, name: doc.name, ext: doc.ext });
  }

  /** Switches the server-side filter and reloads the first page. */
  protected setFilter(f: CaseFilter): void {
    if (this.filter() === f) {
      return;
    }
    this.filter.set(f);
    this.cases.setFilter(f);
  }

  /** Debounces keystrokes into a server-side search (250ms). */
  protected onSearch(value: string): void {
    this.search.set(value);
    if (this.searchDebounce) {
      clearTimeout(this.searchDebounce);
    }
    this.searchDebounce = setTimeout(() => this.cases.setSearch(value), 250);
  }

  /** Loads the next page when the list is scrolled near the bottom. */
  protected onScroll(event: Event): void {
    const el = event.target as HTMLElement;
    if (el.scrollHeight - el.scrollTop - el.clientHeight < 240) {
      this.cases.loadMore();
    }
  }

  /** Navigates to a case's deep link; the route subscription performs the actual select. */
  protected open(id: string): void {
    this.router.navigate(['/cases', id]);
  }

  private select(id: string): void {
    this.selectedId.set(id);
    this.activeTab.set('overview');
    this.detailLoading.set(true);
    this.selected.set(null);
    // Reset the lazily-loaded tab data for the new case.
    this.history.set(null);
    this.historyError.set(false);
    this.invoices.set(null);
    this.payments.set(null);
    this.accountEntries.set(null);
    this.financeError.set(false);
    this.financeView.set('invoices');
    this.cases.loadDetail(id).subscribe((detail) => {
      // ignore a stale response if the user already picked another case
      if (this.selectedId() === id) {
        this.selected.set(detail);
        this.detailLoading.set(false);
      }
    });
  }

  /** Switches the active detail tab and lazily loads its data on first open. */
  protected selectTab(tab: CaseTab): void {
    this.activeTab.set(tab);
    if (tab === 'history' && this.history() === null && !this.historyLoading()) {
      this.loadHistory();
    } else if (tab === 'finance' && this.invoices() === null && !this.financeLoading()) {
      this.loadFinance();
    }
  }

  private loadHistory(): void {
    const id = this.selectedId();
    if (!id) {
      return;
    }
    this.historyLoading.set(true);
    this.historyError.set(false);
    this.cases.history(id).subscribe({
      next: (rows) => {
        if (this.selectedId() !== id) {
          return;
        }
        this.history.set(rows);
        this.historyLoading.set(false);
      },
      error: () => {
        if (this.selectedId() !== id) {
          return;
        }
        this.historyError.set(true);
        this.historyLoading.set(false);
      },
    });
  }

  private loadFinance(): void {
    const id = this.selectedId();
    if (!id) {
      return;
    }
    this.financeLoading.set(true);
    this.financeError.set(false);
    forkJoin({
      invoices: this.cases.invoices(id),
      payments: this.cases.payments(id),
      accountEntries: this.cases.accountEntries(id),
    }).subscribe({
      next: ({ invoices, payments, accountEntries }) => {
        if (this.selectedId() !== id) {
          return;
        }
        this.invoices.set(invoices);
        this.payments.set(payments);
        this.accountEntries.set(accountEntries);
        this.financeLoading.set(false);
      },
      error: () => {
        if (this.selectedId() !== id) {
          return;
        }
        this.financeError.set(true);
        this.financeLoading.set(false);
      },
    });
  }

  /** Retries the currently-active tab's lazy load (history/finance). */
  protected retryTab(): void {
    if (this.activeTab() === 'history') {
      this.loadHistory();
    } else if (this.activeTab() === 'finance') {
      this.loadFinance();
    }
  }

  /** Sum of an invoice list's gross totals (for the finance summary). */
  protected sumGross(list: CaseInvoice[] | null): number {
    return (list ?? []).reduce((acc, i) => acc + (i.totalGross || 0), 0);
  }

  /** Sum of a payment list's totals (for the finance summary). */
  protected sumPayments(list: CasePayment[] | null): number {
    return (list ?? []).reduce((acc, p) => acc + (p.total || 0), 0);
  }

  /** Item count shown as a badge on a finance sub-nav button. */
  protected financeCount(view: FinanceView): number {
    const list = view === 'invoices' ? this.invoices()
      : view === 'payments' ? this.payments()
        : this.accountEntries();
    return list?.length ?? 0;
  }

  /** Overall case-account balance: the sum of all entries' net effects. */
  protected accountBalance(list: AccountEntry[] | null): number {
    return (list ?? []).reduce((acc, e) => acc + (e.total || 0), 0);
  }

  /**
   * The account entries decorated with a running balance, in entry order (the endpoint
   * already returns them by ascending date).
   */
  protected accountRows(): { entry: AccountEntry; balance: number }[] {
    let running = 0;
    return (this.accountEntries() ?? []).map((entry) => {
      running += entry.total || 0;
      return { entry, balance: running };
    });
  }

  /** Mobile "back" from the detail: return to the plain list URL. */
  protected clearSelection(): void {
    this.router.navigate(['/cases']);
  }

  protected initials(name: string): string {
    return name
      .split(/\s+/)
      .filter(Boolean)
      .map((w) => w[0])
      .slice(0, 2)
      .join('')
      .toUpperCase();
  }
}
