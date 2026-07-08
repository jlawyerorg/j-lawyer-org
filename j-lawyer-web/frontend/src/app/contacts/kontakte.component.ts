import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { ContactsService } from './contacts.service';
import { ContactDetail, ContactFilter } from './contact.models';

type ContactTab = 'overview' | 'cases' | 'documents' | 'history';

/**
 * Kontakte (Adressen) module — responsive master-detail mirroring the Akten module: a
 * server-paginated, filtered and searched contact list (infinite scroll) plus a lazily
 * loaded contact detail (contact channels, address, further fields). Data comes from the
 * real REST API via ContactsService.
 */
@Component({
  selector: 'jl-kontakte',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent],
  template: `
    <div class="master-detail" [class.show-detail]="selectedId()">
      <!-- Kontaktliste -->
      <section class="list">
        <header class="list-head">
          <h1>{{ 'kontakte.title' | transloco }}</h1>
          <span class="count">{{ cases.total() }}</span>
          <button type="button" class="btn-primary">
            <jl-icon name="plus" [size]="14" />{{ 'kontakte.new' | transloco }}
          </button>
        </header>

        <div class="filters" role="tablist">
          @for (f of filters; track f) {
            <button type="button" class="chip" [class.on]="filter() === f" (click)="setFilter(f)">
              {{ 'kontakte.filter.' + f | transloco }}
            </button>
          }
        </div>

        <label class="search">
          <jl-icon name="search" [size]="14" />
          <input type="search" [placeholder]="'kontakte.searchContacts' | transloco"
                 [value]="search()" (input)="onSearch($any($event.target).value)" />
        </label>

        <div class="rows" (scroll)="onScroll($event)">
          @if (cases.listLoading() && cases.overviews().length === 0) {
            <p class="muted loading">{{ 'kontakte.loading' | transloco }}</p>
          } @else if (cases.listError() && cases.overviews().length === 0) {
            <p class="empty">
              {{ 'kontakte.error' | transloco }}
              <button type="button" class="btn-retry" (click)="cases.reload()">{{ 'kontakte.retry' | transloco }}</button>
            </p>
          } @else {
            @for (c of cases.overviews(); track c.id) {
              <button type="button" class="row" [class.sel]="c.id === selectedId()" (click)="select(c.id)">
                <span class="av" [class]="c.type">{{ c.initials }}</span>
                <span class="name">{{ c.displayName }}</span>
                <span class="sub">{{ c.subtitle || '—' }}</span>
                <span class="r-right">
                  <span class="pill" [class]="c.type">{{ 'kontakte.type.' + c.type | transloco }}</span>
                </span>
              </button>
            } @empty {
              <p class="empty">{{ 'kontakte.empty' | transloco }}</p>
            }
            @if (cases.listLoading() && cases.overviews().length > 0) {
              <p class="muted loading more">{{ 'kontakte.loadingMore' | transloco }}</p>
            }
          }
        </div>
      </section>

      <!-- Kontaktdetail -->
      <section class="detail">
        @if (detailLoading()) {
          <p class="muted detail-empty">{{ 'kontakte.loading' | transloco }}</p>
        } @else {
          @if (selected(); as c) {
          <div class="detail-head">
            <button type="button" class="back" (click)="clearSelection()">‹ {{ 'kontakte.back' | transloco }}</button>
            <div class="crumbs">{{ 'kontakte.title' | transloco }} › <b>{{ c.displayName }}</b></div>
            <div class="title-row">
              <span class="av lg" [class]="c.type">{{ initials(c.displayName) }}</span>
              <h2>{{ c.displayName }}</h2>
              <span class="pill" [class]="c.type">{{ 'kontakte.type.' + c.type | transloco }}</span>
            </div>
            <div class="meta">
              @if (c.honorific) { <span><span class="k">{{ 'kontakte.field.honorific' | transloco }}</span> <b>{{ c.honorific }}</b></span> }
              @if (c.company) { <span><span class="k">{{ 'kontakte.field.company' | transloco }}</span> <b>{{ c.company }}</b></span> }
              @if (c.department) { <span><span class="k">{{ 'kontakte.field.department' | transloco }}</span> <b>{{ c.department }}</b></span> }
            </div>
            <div class="tabs" role="tablist">
              @for (t of tabs; track t) {
                <button type="button" class="tab" [class.on]="activeTab() === t" (click)="activeTab.set(t)">
                  {{ 'kontakte.tabs.' + t | transloco }}
                </button>
              }
            </div>
          </div>

          <div class="detail-body">
            @if (activeTab() === 'overview') {
              <div class="grid">
                <!-- Kontaktkanäle -->
                <div class="card">
                  <div class="card-h"><h3>{{ 'kontakte.channels' | transloco }}</h3></div>
                  <div class="card-b">
                    @for (f of c.contactFields; track f.key) {
                      <div class="field">
                        <jl-icon [name]="f.icon || 'more'" [size]="15" />
                        <span class="fx">
                          <span class="fk">{{ 'kontakte.field.' + f.key | transloco }}</span>
                          @if (f.href) {
                            <a class="fv" [href]="f.href">{{ f.value }}</a>
                          } @else {
                            <span class="fv">{{ f.value }}</span>
                          }
                        </span>
                      </div>
                    } @empty {
                      <p class="muted">{{ 'kontakte.noChannels' | transloco }}</p>
                    }
                  </div>
                </div>

                <!-- Anschrift -->
                <div class="card">
                  <div class="card-h"><h3>{{ 'kontakte.address' | transloco }}</h3></div>
                  <div class="card-b">
                    @if (c.addressLines.length) {
                      <div class="field">
                        <jl-icon name="pin" [size]="15" />
                        <span class="fx addr">
                          @for (line of c.addressLines; track $index) {
                            <span class="fv">{{ line }}</span>
                          }
                        </span>
                      </div>
                    } @else {
                      <p class="muted">{{ 'kontakte.noAddress' | transloco }}</p>
                    }
                  </div>
                </div>

                <!-- Weiteres -->
                @if (c.moreFields.length) {
                  <div class="card full">
                    <div class="card-h"><h3>{{ 'kontakte.more' | transloco }}</h3></div>
                    <div class="card-b">
                      @for (f of c.moreFields; track f.key) {
                        <div class="field">
                          <jl-icon name="more" [size]="15" />
                          <span class="fx">
                            <span class="fk">{{ 'kontakte.field.' + f.key | transloco }}</span>
                            <span class="fv">{{ f.value }}</span>
                          </span>
                        </div>
                      }
                    </div>
                  </div>
                }
              </div>
            } @else {
              <p class="muted tab-todo">{{ 'kontakte.tabTodo' | transloco }}</p>
            }
          </div>
          } @else {
            <p class="empty detail-empty">{{ 'kontakte.selectHint' | transloco }}</p>
          }
        }
      </section>
    </div>
  `,
  styleUrl: './kontakte.component.css',
})
export class KontakteComponent {
  protected readonly cases = inject(ContactsService);

  protected readonly tabs: ContactTab[] = ['overview', 'cases', 'documents', 'history'];
  protected readonly filters: ContactFilter[] = ['all', 'people', 'companies'];

  protected readonly filter = signal<ContactFilter>('all');
  protected readonly search = signal('');
  protected readonly activeTab = signal<ContactTab>('overview');
  protected readonly selectedId = signal<string | null>(null);
  protected readonly selected = signal<ContactDetail | null>(null);
  protected readonly detailLoading = signal(false);

  private autoSelected = false;
  private searchDebounce: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    this.cases.reload();
    // On wide screens, open the first contact once the list arrives (once only).
    effect(() => {
      const rows = this.cases.overviews();
      if (!this.autoSelected && rows.length && this.selectedId() === null && window.innerWidth > 680) {
        this.autoSelected = true;
        this.select(rows[0].id);
      }
    });
  }

  /** Switches the server-side filter and reloads the first page. */
  protected setFilter(f: ContactFilter): void {
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

  protected select(id: string): void {
    this.selectedId.set(id);
    this.activeTab.set('overview');
    this.detailLoading.set(true);
    this.selected.set(null);
    this.cases.loadDetail(id).subscribe((detail) => {
      // ignore a stale response if the user already picked another contact
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
      .replace(/[,]/g, ' ')
      .split(/\s+/)
      .map((w) => w.match(/[a-z0-9]/i)?.[0] ?? '')
      .filter(Boolean)
      .slice(0, 2)
      .join('')
      .toUpperCase();
  }
}
