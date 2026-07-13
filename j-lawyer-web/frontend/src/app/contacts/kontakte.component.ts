import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { DocumentPreviewComponent } from '../shared/document-preview.component';
import { DocumentContentService } from '../shared/document-content.service';
import { fileKind, fileKindIcon, PreviewDoc, previewKindOf } from '../shared/document-preview.models';
import { PinsService } from '../shell/pins.service';
import { ContactsService } from './contacts.service';
import { ContactEditorComponent } from './contact-editor.component';
import { ContactCase, ContactData, ContactDetail, ContactDocSortKey, ContactDocument, ContactFilter } from './contact.models';

type ContactTab = 'overview' | 'cases' | 'documents';

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
  imports: [TranslocoModule, IconComponent, DatePipe, RouterLink, DocumentPreviewComponent, ContactEditorComponent],
  template: `
    <div class="master-detail" [class.show-detail]="selectedId()">
      <!-- Kontaktliste -->
      <section class="list">
        <header class="list-head">
          <h1>{{ 'kontakte.title' | transloco }}</h1>
          <span class="count">{{ cases.total() }}</span>
          <button type="button" class="btn-primary" (click)="openCreate()">
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
              <button type="button" class="pin-toggle" [class.on]="pins.isPinned('contact', c.id)"
                      (click)="togglePin(c)"
                      [title]="(pins.isPinned('contact', c.id) ? 'pins.unpinItem' : 'pins.pinItem') | transloco">
                <jl-icon name="pushpin" [size]="16" />
              </button>
              <span class="head-actions">
                <button type="button" class="hbtn" (click)="openEdit()" [title]="'kontakte.editContact' | transloco">
                  <jl-icon name="edit" [size]="15" /> {{ 'kontakte.edit' | transloco }}
                </button>
                <button type="button" class="hbtn danger" (click)="confirmDelete(c)" [title]="'kontakte.deleteContact' | transloco">
                  <jl-icon name="trash" [size]="15" />
                </button>
              </span>
            </div>
            <div class="meta">
              @if (c.company) { <span><span class="k">{{ 'kontakte.field.company' | transloco }}</span> <b>{{ c.company }}</b></span> }
              @if (c.department) { <span><span class="k">{{ 'kontakte.field.department' | transloco }}</span> <b>{{ c.department }}</b></span> }
            </div>
            <div class="tabs" role="tablist">
              @for (t of tabs; track t) {
                <button type="button" class="tab" [class.on]="activeTab() === t" (click)="selectTab(t)">
                  {{ 'kontakte.tabs.' + t | transloco }}
                </button>
              }
            </div>
          </div>

          <div class="detail-body">
            @if (activeTab() === 'overview') {
              <div class="grid">
                <!-- Notiz (meist wichtig, daher oben) -->
                @if (c.notice) {
                  <div class="card full">
                    <div class="card-h"><h3>{{ 'kontakte.note' | transloco }}</h3></div>
                    <div class="card-b"><p class="note">{{ c.notice }}</p></div>
                  </div>
                }

                <!-- Kontaktkanäle -->
                @if (c.channels.length) {
                  <div class="card">
                    <div class="card-h"><h3>{{ 'kontakte.channels' | transloco }}</h3></div>
                    <div class="card-b">
                      @for (f of c.channels; track f.key) {
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
                      }
                    </div>
                  </div>
                }

                <!-- Anschrift -->
                @if (c.addressLines.length) {
                  <div class="card">
                    <div class="card-h"><h3>{{ 'kontakte.address' | transloco }}</h3></div>
                    <div class="card-b">
                      <div class="field">
                        <jl-icon name="pin" [size]="15" />
                        <span class="fx addr">
                          @for (line of c.addressLines; track $index) {
                            <span class="fv">{{ line }}</span>
                          }
                        </span>
                      </div>
                    </div>
                  </div>
                }

                <!-- Gruppierte Detailabschnitte (Person, Organisation, Bank, Versicherung, …) -->
                @for (s of c.sections; track s.key) {
                  <div class="card">
                    <div class="card-h"><h3>{{ 'kontakte.section.' + s.key | transloco }}</h3></div>
                    <div class="card-b">
                      <dl class="kv">
                        @for (f of s.fields; track f.labelKey) {
                          <dt>{{ 'kontakte.field.' + f.labelKey | transloco }}</dt><dd>{{ f.value }}</dd>
                        }
                      </dl>
                    </div>
                  </div>
                }

                @if (!c.channels.length && !c.addressLines.length && !c.sections.length && !c.notice) {
                  <p class="muted">{{ 'kontakte.noDetails' | transloco }}</p>
                }
              </div>
            } @else if (activeTab() === 'cases') {
              <div class="card full">
                <div class="card-h">
                  <h3>{{ 'kontakte.cases.title' | transloco }}</h3>
                  @if (contactCases()?.length) { <span class="card-count">{{ contactCases()!.length }}</span> }
                </div>
                <div class="card-b">
                  @if (casesLoading()) {
                    <p class="muted">{{ 'kontakte.loading' | transloco }}</p>
                  } @else if (casesError()) {
                    <p class="muted">
                      {{ 'kontakte.error' | transloco }}
                      <button type="button" class="btn-retry" (click)="loadCases()">{{ 'kontakte.retry' | transloco }}</button>
                    </p>
                  } @else {
                    @for (cs of contactCases()!; track cs.id) {
                      <a class="case-row" [routerLink]="['/cases', cs.id]">
                        <jl-icon name="cases" [size]="16" />
                        <span class="cx">
                          <span class="cn">{{ cs.name }}</span>
                          <span class="cmeta">{{ cs.fileNumber }}{{ cs.reason ? ' · ' + cs.reason : '' }}</span>
                        </span>
                        @if (cs.role) {
                          <span class="role-badge" [style.background]="cs.roleColor || null"
                                [style.color]="cs.roleColor ? contrastOn(cs.roleColor) : null">{{ cs.role }}</span>
                        }
                        <span class="cdate">{{ cs.dateChanged | date: 'dd.MM.yyyy' }}</span>
                      </a>
                    } @empty {
                      <p class="muted">{{ 'kontakte.cases.empty' | transloco }}</p>
                    }
                  }
                </div>
              </div>
            } @else if (activeTab() === 'documents') {
              <div class="card full docs-card">
                <div class="card-h">
                  <h3>{{ 'kontakte.documents' | transloco }}</h3>
                  @if (contactDocs()?.length) { <span class="card-count">{{ contactDocs()!.length }}</span> }
                </div>
                <div class="doc-tools">
                  <div class="doc-search">
                    <jl-icon name="search" [size]="14" />
                    <input type="search" [value]="docSearch()" [placeholder]="'kontakte.docs.searchPlaceholder' | transloco"
                           (input)="docSearch.set($any($event.target).value)" />
                    @if (docSearch()) { <button type="button" class="clear" (click)="docSearch.set('')" aria-label="clear">✕</button> }
                  </div>
                  <div class="doc-sort">
                    <span class="sort-label">{{ 'kontakte.docs.sortBy' | transloco }}</span>
                    @for (s of docSortKeys; track s) {
                      <button type="button" class="sort-btn" [class.on]="docSort().key === s" (click)="toggleSort(s)">
                        {{ 'kontakte.docs.sort.' + s | transloco }}{{ sortArrow(s) }}
                      </button>
                    }
                  </div>
                </div>
                <div class="card-b">
                  @if (docsLoading()) {
                    <p class="muted">{{ 'kontakte.loading' | transloco }}</p>
                  } @else if (docsError()) {
                    <p class="muted">
                      {{ 'kontakte.error' | transloco }}
                      <button type="button" class="btn-retry" (click)="loadDocuments()">{{ 'kontakte.retry' | transloco }}</button>
                    </p>
                  } @else {
                    @for (doc of visibleDocs(); track doc.id) {
                      <div class="doc doc-row" [class.previewable]="canPreview(doc)"
                           (click)="canPreview(doc) ? preview(doc) : download(doc)">
                        <span class="ftype" [class]="'kind-' + docKind(doc)" [title]="doc.ext || ''">
                          <jl-icon [name]="docIcon(doc)" [size]="16" />
                          <span class="ext-lbl">{{ doc.ext || '—' }}</span>
                        </span>
                        <span class="doc-main">
                          <span class="dn">{{ doc.name }}</span>
                          <span class="dmeta">{{ doc.date | date: 'dd.MM.yyyy' }} · {{ doc.size }}</span>
                        </span>
                        <span class="doc-actions">
                          @if (canPreview(doc)) {
                            <button type="button" class="doc-btn" (click)="$event.stopPropagation(); preview(doc)">
                              {{ 'kontakte.docs.preview' | transloco }}
                            </button>
                          }
                          <button type="button" class="doc-btn primary" (click)="$event.stopPropagation(); download(doc)">
                            <jl-icon name="download" [size]="14" />{{ 'kontakte.docs.download' | transloco }}
                          </button>
                        </span>
                      </div>
                    } @empty {
                      <p class="muted">{{ (docSearch() ? 'kontakte.docs.noResults' : 'kontakte.docs.empty') | transloco }}</p>
                    }
                  }
                </div>
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

    <jl-document-preview [doc]="previewDoc()" (closed)="previewDoc.set(null)" />

    @if (editing(); as ed) {
      <jl-contact-editor [contact]="ed.contact"
                         (save)="onSave($event)" (remove)="onDelete($event)" (close)="closeEditor()" />
    }
  `,
  styleUrl: './kontakte.component.css',
})
export class KontakteComponent {
  protected readonly cases = inject(ContactsService);
  protected readonly pins = inject(PinsService);
  private readonly content = inject(DocumentContentService);
  private readonly route = inject(ActivatedRoute);
  private readonly transloco = inject(TranslocoService);

  protected readonly tabs: ContactTab[] = ['overview', 'cases', 'documents'];
  protected readonly filters: ContactFilter[] = ['all', 'people', 'companies'];

  protected readonly filter = signal<ContactFilter>('all');
  protected readonly search = signal('');
  protected readonly activeTab = signal<ContactTab>('overview');
  protected readonly selectedId = signal<string | null>(null);
  protected readonly selected = signal<ContactDetail | null>(null);
  protected readonly detailLoading = signal(false);

  // Cases tab (lazy-loaded per contact): the cases the contact is involved in.
  protected readonly contactCases = signal<ContactCase[] | null>(null);
  protected readonly casesLoading = signal(false);
  protected readonly casesError = signal(false);

  // Documents tab (lazy-loaded per contact): the contact's own attached documents + view state.
  protected readonly contactDocs = signal<ContactDocument[] | null>(null);
  protected readonly docsLoading = signal(false);
  protected readonly docsError = signal(false);
  protected readonly docSearch = signal('');
  protected readonly docSortKeys: ContactDocSortKey[] = ['name', 'date', 'size'];
  protected readonly docSort = signal<{ key: ContactDocSortKey; dir: 'asc' | 'desc' }>({ key: 'name', dir: 'asc' });
  protected readonly previewDoc = signal<PreviewDoc | null>(null);

  private autoSelected = false;
  private searchDebounce: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    this.cases.reload();
    // A :id route param (deep link / pinned shortcut) selects that contact.
    this.route.paramMap.pipe(takeUntilDestroyed()).subscribe((params) => {
      const id = params.get('id');
      if (id) {
        this.autoSelected = true;
        if (id !== this.selectedId()) {
          this.select(id);
        }
      }
    });
    // On wide screens with no deep link, open the first contact once the list arrives (once only).
    effect(() => {
      const rows = this.cases.overviews();
      if (!this.autoSelected && rows.length && this.selectedId() === null && window.innerWidth > 680) {
        this.autoSelected = true;
        this.select(rows[0].id);
      }
    });
  }

  /** Toggles the current contact as a pinned shortcut in the header pin bar. */
  protected togglePin(c: ContactDetail): void {
    this.pins.toggle({ kind: 'contact', id: c.id, label: c.displayName });
  }

  /** Editor state: null = closed; `contact` is the raw DTO to edit (null when creating). */
  protected readonly editing = signal<{ contact: ContactData | null } | null>(null);

  protected openCreate(): void {
    this.editing.set({ contact: null });
  }

  /** Opens the editor for the current contact using its raw (writable) DTO. */
  protected openEdit(): void {
    const raw = this.cases.rawSelected();
    if (raw) {
      this.editing.set({ contact: raw });
    }
  }

  protected closeEditor(): void {
    this.editing.set(null);
  }

  /** Persists the working copy (create or update), then refreshes the list and reopens the detail. */
  protected onSave(data: ContactData): void {
    this.cases.save(data).subscribe({
      next: (saved) => {
        this.closeEditor();
        this.cases.reload();
        if (saved?.id) {
          this.select(saved.id);
        }
      },
      error: () => undefined, // keep the dialog open on failure
    });
  }

  /** Confirms and deletes the current contact directly from the detail header. */
  protected confirmDelete(c: ContactDetail): void {
    if (confirm(this.deletePrompt(c.displayName))) {
      this.onDelete(c.id);
    }
  }

  private deletePrompt(name: string): string {
    return this.transloco.translate('kontakte.deleteConfirm', { name });
  }

  protected onDelete(id: string): void {
    this.cases.remove(id).subscribe({
      next: () => {
        this.closeEditor();
        this.selectedId.set(null);
        this.selected.set(null);
        this.cases.reload();
      },
      error: () => undefined,
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
    this.contactCases.set(null);
    this.casesError.set(false);
    this.contactDocs.set(null);
    this.docsError.set(false);
    this.docSearch.set('');
    this.previewDoc.set(null);
    this.cases.loadDetail(id).subscribe((detail) => {
      // ignore a stale response if the user already picked another contact
      if (this.selectedId() === id) {
        this.selected.set(detail);
        this.detailLoading.set(false);
      }
    });
  }

  /** Switches tabs and lazily loads the tab's data on first open. */
  protected selectTab(tab: ContactTab): void {
    this.activeTab.set(tab);
    if (tab === 'cases' && this.contactCases() === null && !this.casesLoading()) {
      this.loadCases();
    } else if (tab === 'documents' && this.contactDocs() === null && !this.docsLoading()) {
      this.loadDocuments();
    }
  }

  /** Loads the cases the contact is involved in (GET /v5/contacts/{id}/cases). */
  protected loadCases(): void {
    const id = this.selectedId();
    if (!id) { return; }
    this.casesLoading.set(true);
    this.casesError.set(false);
    this.cases.cases(id).subscribe({
      next: (rows) => { if (this.selectedId() === id) { this.contactCases.set(rows); this.casesLoading.set(false); } },
      error: () => { if (this.selectedId() === id) { this.casesError.set(true); this.casesLoading.set(false); } },
    });
  }

  /** Loads the contact's own documents (GET /v7/contacts/{id}/documents). */
  protected loadDocuments(): void {
    const id = this.selectedId();
    if (!id) { return; }
    this.docsLoading.set(true);
    this.docsError.set(false);
    this.cases.documents(id).subscribe({
      next: (docs) => { if (this.selectedId() === id) { this.contactDocs.set(docs); this.docsLoading.set(false); } },
      error: () => { if (this.selectedId() === id) { this.docsError.set(true); this.docsLoading.set(false); } },
    });
  }

  /** The documents to show: filtered by the search term, sorted by the active criterion. */
  protected visibleDocs(): ContactDocument[] {
    const all = this.contactDocs() ?? [];
    const term = this.docSearch().trim().toLowerCase();
    const docs = term ? all.filter((d) => d.name.toLowerCase().includes(term)) : [...all];
    const { key, dir } = this.docSort();
    const mul = dir === 'asc' ? 1 : -1;
    return docs.sort((a, b) => {
      let p = 0;
      switch (key) {
        case 'name': p = a.name.localeCompare(b.name, undefined, { sensitivity: 'base' }); break;
        case 'date': p = a.date < b.date ? -1 : a.date > b.date ? 1 : 0; break;
        case 'size': p = a.sizeBytes - b.sizeBytes; break;
      }
      p *= mul;
      return p !== 0 ? p : a.name.localeCompare(b.name, undefined, { sensitivity: 'base' });
    });
  }

  protected toggleSort(key: ContactDocSortKey): void {
    const cur = this.docSort();
    if (cur.key === key) {
      this.docSort.set({ key, dir: cur.dir === 'asc' ? 'desc' : 'asc' });
    } else {
      this.docSort.set({ key, dir: key === 'date' || key === 'size' ? 'desc' : 'asc' });
    }
  }

  protected sortArrow(key: ContactDocSortKey): string {
    const s = this.docSort();
    return s.key === key ? (s.dir === 'asc' ? ' ▲' : ' ▼') : '';
  }

  protected docKind(doc: ContactDocument): string {
    return fileKind(doc.ext);
  }

  protected docIcon(doc: ContactDocument): string {
    return fileKindIcon(doc.ext);
  }

  protected canPreview(doc: ContactDocument): boolean {
    return previewKindOf(doc.ext) !== 'none';
  }

  /** Opens the shared preview overlay (download-only kinds just download). Uses the contact source. */
  protected preview(doc: ContactDocument): void {
    if (previewKindOf(doc.ext) === 'none') {
      this.download(doc);
      return;
    }
    this.previewDoc.set({ id: doc.id, name: doc.name, ext: doc.ext, source: 'contact' });
  }

  protected download(doc: ContactDocument): void {
    this.content.download({ id: doc.id, name: doc.name, ext: doc.ext, source: 'contact' });
  }

  protected clearSelection(): void {
    this.selectedId.set(null);
    this.selected.set(null);
  }

  /** Readable foreground (black/white) for a "#rrggbb" background, by perceived luminance. */
  protected contrastOn(hex: string): string {
    const m = /^#?([0-9a-f]{6})$/i.exec(hex);
    if (!m) { return '#fff'; }
    const n = parseInt(m[1], 16);
    const r = (n >> 16) & 0xff, g = (n >> 8) & 0xff, b = n & 0xff;
    return 0.299 * r + 0.587 * g + 0.114 * b > 150 ? '#16232e' : '#fff';
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
