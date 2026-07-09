import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { DocumentPreviewComponent } from '../shared/document-preview.component';
import { DocumentContentService } from '../shared/document-content.service';
import { PreviewDoc, previewKindOf } from '../shared/document-preview.models';
import { forkJoin, map } from 'rxjs';
import { CaseFilter, CasesService } from './cases.service';
import {
  AccountEntry, CaseDetail, CaseDocument, CaseHistoryEntry, CaseInvoice, CasePayment,
  CaseTimesheet, TimesheetPosition,
} from './case.models';

type CaseTab = 'overview' | 'documents' | 'parties' | 'deadlines' | 'finance' | 'zeiten' | 'history';
/** Sub-view within the finance tab (invoices, payments and the case account are too much for one screen). */
type FinanceView = 'invoices' | 'payments' | 'account';
/** Status filter for the Zeiten tab (show all timesheets, only open, or only closed ones). */
type ZeitenFilter = 'all' | 'open' | 'closed';
/** A timesheet with its loaded positions (the "Zeiten" tab lists positions per timesheet). */
interface TimesheetView extends CaseTimesheet {
  positions: TimesheetPosition[];
}

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
                      @if (accountEntries()?.length) {
                        @let sum = accountSummary();
                        <!-- Kategorie-Salden (Einnahmen/Ausgaben, Fremdgeld, Auslagen), analog ArchiveFilePanel -->
                        <div class="acct-sums">
                          @for (g of sum; track g.key) {
                            <div class="acct-sum">
                              <span class="acct-sum-title">{{ 'akten.finance.acct.cat.' + g.key | transloco }}</span>
                              <div class="acct-sum-row">
                                <span>{{ 'akten.finance.acct.in' | transloco }}</span>
                                <span class="num">{{ g.in | number: '1.2-2' }} €</span>
                              </div>
                              <div class="acct-sum-row">
                                <span>{{ 'akten.finance.acct.out' | transloco }}</span>
                                <span class="num">{{ g.out | number: '1.2-2' }} €</span>
                              </div>
                              <div class="acct-sum-row net">
                                <span>{{ 'akten.finance.acct.balance' | transloco }}</span>
                                <span class="num" [class.neg]="g.net < 0">{{ g.net | number: '1.2-2' }} €</span>
                              </div>
                            </div>
                          }
                        </div>

                        <div class="card full">
                          <div class="card-b">
                            <div class="acct-scroll">
                              <table class="acct-table">
                                <thead>
                                  <tr>
                                    <th class="c-date">{{ 'akten.finance.acct.date' | transloco }}</th>
                                    <th class="c-contact">{{ 'akten.finance.acct.contact' | transloco }}</th>
                                    <th class="c-desc">{{ 'akten.finance.acct.description' | transloco }}</th>
                                    <th class="num">{{ 'akten.finance.acct.earnings' | transloco }}</th>
                                    <th class="num">{{ 'akten.finance.acct.spendings' | transloco }}</th>
                                    <th class="num">{{ 'akten.finance.acct.escrowIn' | transloco }}</th>
                                    <th class="num">{{ 'akten.finance.acct.escrowOut' | transloco }}</th>
                                    <th class="num">{{ 'akten.finance.acct.expendituresIn' | transloco }}</th>
                                    <th class="num">{{ 'akten.finance.acct.expendituresOut' | transloco }}</th>
                                  </tr>
                                </thead>
                                <tbody>
                                  @for (e of accountEntries(); track e.id) {
                                    <tr>
                                      <td class="c-date">{{ e.date | date: 'dd.MM.yyyy' }}</td>
                                      <td class="c-contact">{{ e.contact || '—' }}</td>
                                      <td class="c-desc">{{ e.description || '—' }}</td>
                                      <td class="num">{{ e.earnings ? (e.earnings | number: '1.2-2') : '—' }}</td>
                                      <td class="num">{{ e.spendings ? (e.spendings | number: '1.2-2') : '—' }}</td>
                                      <td class="num">{{ e.escrowIn ? (e.escrowIn | number: '1.2-2') : '—' }}</td>
                                      <td class="num">{{ e.escrowOut ? (e.escrowOut | number: '1.2-2') : '—' }}</td>
                                      <td class="num">{{ e.expendituresIn ? (e.expendituresIn | number: '1.2-2') : '—' }}</td>
                                      <td class="num">{{ e.expendituresOut ? (e.expendituresOut | number: '1.2-2') : '—' }}</td>
                                    </tr>
                                  }
                                </tbody>
                                <tfoot>
                                  <tr>
                                    <td class="c-date"></td>
                                    <td class="c-contact"></td>
                                    <td class="c-desc">{{ 'akten.finance.acct.totals' | transloco }}</td>
                                    <td class="num">{{ sum[0].in | number: '1.2-2' }}</td>
                                    <td class="num">{{ sum[0].out | number: '1.2-2' }}</td>
                                    <td class="num">{{ sum[1].in | number: '1.2-2' }}</td>
                                    <td class="num">{{ sum[1].out | number: '1.2-2' }}</td>
                                    <td class="num">{{ sum[2].in | number: '1.2-2' }}</td>
                                    <td class="num">{{ sum[2].out | number: '1.2-2' }}</td>
                                  </tr>
                                </tfoot>
                              </table>
                            </div>
                          </div>
                        </div>
                      } @else {
                        <div class="card full"><div class="card-b">
                          <p class="muted">{{ 'akten.finance.noAccountEntries' | transloco }}</p>
                        </div></div>
                      }
                    }
                  }
                </div>
              }
            } @else if (activeTab() === 'zeiten') {
              @if (timesheetsLoading()) {
                <p class="muted tab-todo">{{ 'akten.loading' | transloco }}</p>
              } @else if (timesheetsError()) {
                <p class="tab-todo">
                  {{ 'akten.error' | transloco }}
                  <button type="button" class="btn-retry" (click)="retryTab()">{{ 'akten.retry' | transloco }}</button>
                </p>
              } @else {
                <div class="grid">
                  @if (timesheets()?.length) {
                    <div class="fin-nav" role="tablist">
                      @for (f of zeitenFilters; track f) {
                        <button type="button" class="fin-nav-btn" [class.on]="zeitenFilter() === f" (click)="zeitenFilter.set(f)">
                          {{ 'akten.zeiten.filter.' + f | transloco }}
                          <span class="fin-nav-count">{{ zeitenCount(f) }}</span>
                        </button>
                      }
                    </div>
                  }
                  @for (ts of visibleTimesheets(); track ts.id) {
                    <div class="card full">
                      <div class="card-h">
                        <h3>{{ ts.name || '—' }}</h3>
                        <span class="ts-status" [class.closed]="ts.status === 20">
                          {{ (ts.status === 20 ? 'akten.zeiten.closed' : 'akten.zeiten.open') | transloco }}
                        </span>
                        <span class="card-count">{{ ts.positions.length }}</span>
                      </div>
                      <div class="card-b">
                        @if (ts.description) { <p class="ts-desc">{{ ts.description }}</p> }
                        <div class="ts-kpis">
                          <div class="fin-kpi">
                            <span class="fk-label">{{ 'akten.zeiten.duration' | transloco }}</span>
                            <span class="fk-value">{{ tsDuration(ts.positions) }} h</span>
                          </div>
                          <div class="fin-kpi">
                            <span class="fk-label">{{ 'akten.zeiten.net' | transloco }}</span>
                            <span class="fk-value">{{ tsNet(ts.positions) | number: '1.2-2' }} €</span>
                          </div>
                          <div class="fin-kpi">
                            <span class="fk-label">{{ 'akten.zeiten.invoiceable' | transloco }}</span>
                            <span class="fk-value">{{ tsInvoiceable(ts.positions) | number: '1.2-2' }} €</span>
                          </div>
                          @if (ts.limited) {
                            <div class="fin-kpi">
                              <span class="fk-label">{{ 'akten.zeiten.limit' | transloco }}</span>
                              <span class="fk-value">{{ ts.limit | number: '1.2-2' }} € · {{ ts.percentageDone | number: '1.0-0' }} %</span>
                            </div>
                          }
                        </div>

                        @if (ts.positions.length) {
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
                                </tr>
                              </thead>
                              <tbody>
                                @for (p of ts.positions; track p.id) {
                                  <tr>
                                    <td>{{ p.name || p.description || '—' }}</td>
                                    <td>{{ p.principal || '—' }}</td>
                                    <td>{{ p.started ? (p.started | date: 'dd.MM.yyyy HH:mm') : '—' }}</td>
                                    <td class="num">
                                      @if (p.running) {
                                        <span class="ts-run">{{ 'akten.zeiten.running' | transloco }}</span>
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
                                  </tr>
                                }
                              </tbody>
                            </table>
                          </div>
                        } @else {
                          <p class="muted">{{ 'akten.zeiten.noPositions' | transloco }}</p>
                        }
                      </div>
                    </div>
                  } @empty {
                    <div class="card full"><div class="card-b">
                      <p class="muted">{{ 'akten.zeiten.noTimesheets' | transloco }}</p>
                    </div></div>
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

  protected readonly tabs: CaseTab[] = ['overview', 'documents', 'parties', 'deadlines', 'finance', 'zeiten', 'history'];
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

  // Zeiten (time tracking) tab state — the case's timesheets (open + closed) with their positions.
  protected readonly timesheets = signal<TimesheetView[] | null>(null);
  protected readonly timesheetsLoading = signal(false);
  protected readonly timesheetsError = signal(false);
  /** Show all timesheets, only open or only closed. */
  protected readonly zeitenFilter = signal<ZeitenFilter>('all');
  protected readonly zeitenFilters: ZeitenFilter[] = ['all', 'open', 'closed'];

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
    this.timesheets.set(null);
    this.timesheetsError.set(false);
    this.zeitenFilter.set('all');
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
    } else if (tab === 'zeiten' && this.timesheets() === null && !this.timesheetsLoading()) {
      this.loadTimesheets();
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

  /**
   * Loads the case's (open) timesheets, then their positions in parallel. Mirrors the desktop
   * "Zeiten" tab: a list of timesheet projects, each with its time entries. Read-only.
   */
  private loadTimesheets(): void {
    const id = this.selectedId();
    if (!id) {
      return;
    }
    this.timesheetsLoading.set(true);
    this.timesheetsError.set(false);
    this.cases.timesheets(id).subscribe({
      next: (sheets) => {
        if (this.selectedId() !== id) {
          return;
        }
        if (!sheets.length) {
          this.timesheets.set([]);
          this.timesheetsLoading.set(false);
          return;
        }
        forkJoin(
          sheets.map((s) => this.cases.timesheetPositions(s.id).pipe(
            map((positions) => ({ ...s, positions })),
          )),
        ).subscribe({
          next: (views) => {
            if (this.selectedId() !== id) {
              return;
            }
            this.timesheets.set(views);
            this.timesheetsLoading.set(false);
          },
          error: () => {
            if (this.selectedId() !== id) {
              return;
            }
            this.timesheetsError.set(true);
            this.timesheetsLoading.set(false);
          },
        });
      },
      error: () => {
        if (this.selectedId() !== id) {
          return;
        }
        this.timesheetsError.set(true);
        this.timesheetsLoading.set(false);
      },
    });
  }

  /** Retries the currently-active tab's lazy load (history/finance/zeiten). */
  protected retryTab(): void {
    if (this.activeTab() === 'history') {
      this.loadHistory();
    } else if (this.activeTab() === 'finance') {
      this.loadFinance();
    } else if (this.activeTab() === 'zeiten') {
      this.loadTimesheets();
    }
  }

  /** Timesheets matching the active status filter (status 20 = closed). */
  protected visibleTimesheets(): TimesheetView[] {
    const all = this.timesheets() ?? [];
    const f = this.zeitenFilter();
    if (f === 'open') {
      return all.filter((t) => t.status !== 20);
    }
    if (f === 'closed') {
      return all.filter((t) => t.status === 20);
    }
    return all;
  }

  /** Count of timesheets for a given filter (shown as a badge on the filter buttons). */
  protected zeitenCount(f: ZeitenFilter): number {
    const all = this.timesheets() ?? [];
    if (f === 'open') {
      return all.filter((t) => t.status !== 20).length;
    }
    if (f === 'closed') {
      return all.filter((t) => t.status === 20).length;
    }
    return all.length;
  }

  /** Net sum of a timesheet's positions. */
  protected tsNet(positions: TimesheetPosition[]): number {
    return positions.reduce((acc, p) => acc + (p.total || 0), 0);
  }

  /** Sum of the still-unbilled (no invoice) positions — the invoiceable amount. */
  protected tsInvoiceable(positions: TimesheetPosition[]): number {
    return positions.reduce((acc, p) => acc + (p.invoiceId ? 0 : (p.total || 0)), 0);
  }

  /** Total tracked duration of a timesheet's completed positions, formatted "h:mm". */
  protected tsDuration(positions: TimesheetPosition[]): string {
    const ms = positions.reduce((acc, p) => acc + positionMillis(p), 0);
    return formatDurationMs(ms);
  }

  /** A single position's duration, formatted "h:mm" (empty while running or unstarted). */
  protected durationOf(p: TimesheetPosition): string {
    const ms = positionMillis(p);
    return ms > 0 ? formatDurationMs(ms) : '—';
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

  /**
   * Per-category account totals (mirrors ArchiveFilePanel.updateAccountTotals): for each of
   * the three categories — earnings/spendings, escrow, expenditures — the sum of credits
   * (`in`), the sum of debits (`out`) and the net (`in - out`). Returned in display order.
   */
  protected accountSummary(): { key: 'earnings' | 'escrow' | 'expenditures'; in: number; out: number; net: number }[] {
    const list = this.accountEntries() ?? [];
    const s = (pick: (e: AccountEntry) => number) => list.reduce((acc, e) => acc + (pick(e) || 0), 0);
    const earningsIn = s((e) => e.earnings);
    const earningsOut = s((e) => e.spendings);
    const escrowIn = s((e) => e.escrowIn);
    const escrowOut = s((e) => e.escrowOut);
    const expIn = s((e) => e.expendituresIn);
    const expOut = s((e) => e.expendituresOut);
    return [
      { key: 'earnings', in: earningsIn, out: earningsOut, net: earningsIn - earningsOut },
      { key: 'escrow', in: escrowIn, out: escrowOut, net: escrowIn - escrowOut },
      { key: 'expenditures', in: expIn, out: expOut, net: expIn - expOut },
    ];
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

/** Elapsed milliseconds of a completed position (0 while running or unstarted). */
function positionMillis(p: TimesheetPosition): number {
  if (!p.started || !p.stopped) {
    return 0;
  }
  const start = new Date(p.started).getTime();
  const stop = new Date(p.stopped).getTime();
  return stop > start ? stop - start : 0;
}

/** Formats a duration in milliseconds as "h:mm" (e.g. 125 min -> "2:05"). */
function formatDurationMs(ms: number): string {
  const totalMinutes = Math.round(ms / 60000);
  const h = Math.floor(totalMinutes / 60);
  const m = totalMinutes % 60;
  return `${h}:${String(m).padStart(2, '0')}`;
}
