import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { CasesService } from './cases.service';
import { CaseDetail } from './case.models';

type CaseTab = 'overview' | 'documents' | 'parties' | 'deadlines' | 'finance' | 'history';

/**
 * Akten (cases) module — responsive master-detail (design-mockup.html): searchable case
 * list + case detail (overview with parties, deadlines, recent documents, note). Data comes
 * from the real REST API via CasesService (list eagerly, detail lazily per selection).
 */
@Component({
  selector: 'jl-akten',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent, DatePipe, DecimalPipe],
  template: `
    <div class="master-detail" [class.show-detail]="selectedId()">
      <!-- Aktenliste -->
      <section class="list">
        <header class="list-head">
          <h1>{{ 'akten.title' | transloco }}</h1>
          <span class="count">{{ filtered().length }}</span>
          <button type="button" class="btn-primary">
            <jl-icon name="plus" [size]="14" />{{ 'akten.new' | transloco }}
          </button>
        </header>

        <label class="search">
          <jl-icon name="search" [size]="14" />
          <input type="search" [placeholder]="'akten.searchCases' | transloco"
                 [value]="search()" (input)="search.set($any($event.target).value)" />
        </label>

        <div class="rows">
          @if (cases.listLoading()) {
            <p class="muted loading">{{ 'akten.loading' | transloco }}</p>
          } @else if (cases.listError()) {
            <p class="empty">
              {{ 'akten.error' | transloco }}
              <button type="button" class="btn-retry" (click)="cases.loadList()">{{ 'akten.retry' | transloco }}</button>
            </p>
          } @else {
            @for (c of filtered(); track c.id) {
              <button type="button" class="row" [class.sel]="c.id === selectedId()" (click)="select(c.id)">
                <span class="az">{{ c.fileNumber }}</span>
                <span class="name">{{ c.name }}</span>
                <span class="sub">{{ c.reason || '—' }}</span>
                <span class="r-right">
                  <span class="date">{{ c.lastChanged | date: 'dd.MM.yy' }}</span>
                </span>
              </button>
            } @empty {
              <p class="empty">{{ 'akten.empty' | transloco }}</p>
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
                <button type="button" class="tab" [class.on]="activeTab() === t" (click)="activeTab.set(t)">
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
            } @else {
              <p class="muted tab-todo">{{ 'akten.tabTodo' | transloco }}</p>
            }
          </div>
          } @else {
            <p class="empty detail-empty">{{ 'akten.selectHint' | transloco }}</p>
          }
        }
      </section>
    </div>
  `,
  styleUrl: './akten.component.css',
})
export class AktenComponent {
  protected readonly cases = inject(CasesService);

  protected readonly tabs: CaseTab[] = ['overview', 'documents', 'parties', 'deadlines', 'finance', 'history'];

  protected readonly search = signal('');
  protected readonly activeTab = signal<CaseTab>('overview');
  protected readonly selectedId = signal<string | null>(null);
  protected readonly selected = signal<CaseDetail | null>(null);
  protected readonly detailLoading = signal(false);

  private autoSelected = false;

  protected readonly filtered = computed(() => {
    const term = this.search().trim().toLowerCase();
    const rows = this.cases.overviews();
    if (!term) {
      return rows;
    }
    return rows.filter((c) => (c.name + ' ' + c.fileNumber + ' ' + c.reason).toLowerCase().includes(term));
  });

  constructor() {
    this.cases.loadList();
    // On wide screens, open the first case once the list arrives (once only).
    effect(() => {
      const rows = this.cases.overviews();
      if (!this.autoSelected && rows.length && this.selectedId() === null && window.innerWidth > 680) {
        this.autoSelected = true;
        this.select(rows[0].id);
      }
    });
  }

  protected select(id: string): void {
    this.selectedId.set(id);
    this.activeTab.set('overview');
    this.detailLoading.set(true);
    this.selected.set(null);
    this.cases.loadDetail(id).subscribe((detail) => {
      // ignore a stale response if the user already picked another case
      if (this.selectedId() === id) {
        this.selected.set(detail);
        this.detailLoading.set(false);
      }
    });
  }

  protected clearSelection(): void {
    this.selectedId.set(null);
    this.selected.set(null);
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
