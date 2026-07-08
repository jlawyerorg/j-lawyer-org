import { ChangeDetectionStrategy, Component, DestroyRef, effect, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { DomSanitizer, SafeResourceUrl, SafeUrl } from '@angular/platform-browser';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { CaseFilter, CasesService } from './cases.service';
import { CaseDetail, CaseDocument, DocPreviewKind, mimeOf, previewKindOf } from './case.models';

type CaseTab = 'overview' | 'documents' | 'parties' | 'deadlines' | 'finance' | 'history';

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
  imports: [TranslocoModule, IconComponent, DatePipe, DecimalPipe],
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
              <button type="button" class="row" [class.sel]="c.id === selectedId()" (click)="select(c.id)">
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

    @if (previewDoc(); as pd) {
      <div class="viewer" (click)="closePreview()">
        <div class="viewer-box" (click)="$event.stopPropagation()">
          <header class="viewer-head">
            <span class="ext">{{ pd.ext || '—' }}</span>
            <span class="viewer-name">{{ pd.name }}</span>
            @if (previewKind() === 'pdf' && pdfSafeUrl()) {
              <button type="button" class="doc-btn" (click)="openPdfTab()">{{ 'akten.pdfOpenTab' | transloco }}</button>
            }
            <button type="button" class="doc-btn" (click)="download(pd)">
              <jl-icon name="download" [size]="14" />{{ 'akten.docDownload' | transloco }}
            </button>
            <button type="button" class="viewer-close" (click)="closePreview()" [attr.aria-label]="'akten.close' | transloco">✕</button>
          </header>
          <div class="viewer-body">
            @if (previewLoading()) {
              <p class="muted pad">{{ 'akten.docLoading' | transloco }}</p>
            } @else if (previewError()) {
              <p class="pad">{{ 'akten.docError' | transloco }}</p>
            } @else if (previewKind() === 'image' && previewImage()) {
              <img class="viewer-img" [src]="previewImage()" [alt]="pd.name" />
            } @else if (previewKind() === 'text') {
              <pre class="viewer-text">{{ previewText() }}</pre>
            } @else if (previewKind() === 'pdf' && pdfSafeUrl()) {
              <iframe class="viewer-frame" [src]="pdfSafeUrl()" [title]="pd.name"></iframe>
            }
          </div>
        </div>
      </div>
    }
  `,
  styleUrl: './akten.component.css',
})
export class AktenComponent {
  protected readonly cases = inject(CasesService);
  private readonly sanitizer = inject(DomSanitizer);

  protected readonly tabs: CaseTab[] = ['overview', 'documents', 'parties', 'deadlines', 'finance', 'history'];
  protected readonly filters: CaseFilter[] = ['all', 'open', 'closed'];

  protected readonly filter = signal<CaseFilter>('all');
  protected readonly search = signal('');
  protected readonly activeTab = signal<CaseTab>('overview');
  protected readonly selectedId = signal<string | null>(null);
  protected readonly selected = signal<CaseDetail | null>(null);
  protected readonly detailLoading = signal(false);

  // Document preview state
  protected readonly previewDoc = signal<CaseDocument | null>(null);
  protected readonly previewKind = signal<DocPreviewKind>('none');
  protected readonly previewLoading = signal(false);
  protected readonly previewError = signal(false);
  protected readonly previewImage = signal<SafeUrl | null>(null);
  protected readonly previewText = signal('');
  protected readonly pdfSafeUrl = signal<SafeResourceUrl | null>(null);
  private pdfBlobUrl: string | null = null;
  private previewSeq = 0;

  private autoSelected = false;
  private searchDebounce: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    this.cases.reload();
    // On wide screens, open the first case once the list arrives (once only).
    effect(() => {
      const rows = this.cases.overviews();
      if (!this.autoSelected && rows.length && this.selectedId() === null && window.innerWidth > 680) {
        this.autoSelected = true;
        this.select(rows[0].id);
      }
    });
    inject(DestroyRef).onDestroy(() => this.revokePdfUrl());
  }

  /** True when the document can be previewed inline / in a new tab (vs. download-only). */
  protected canPreview(doc: CaseDocument): boolean {
    return previewKindOf(doc.ext) !== 'none';
  }

  /** Opens the preview overlay for a document (image/text inline, PDF via a new tab). */
  protected preview(doc: CaseDocument): void {
    const kind = previewKindOf(doc.ext);
    if (kind === 'none') {
      this.download(doc);
      return;
    }
    const seq = ++this.previewSeq;
    this.revokePdfUrl();
    this.pdfSafeUrl.set(null);
    this.previewImage.set(null);
    this.previewText.set('');
    this.previewError.set(false);
    this.previewKind.set(kind);
    this.previewDoc.set(doc);
    this.previewLoading.set(true);

    this.cases.documentContent(doc.id).subscribe({
      next: (dto) => {
        if (seq !== this.previewSeq) {
          return;
        }
        const b64 = (dto.base64content ?? '').replace(/\s/g, '');
        const mime = mimeOf(doc.ext);
        if (kind === 'image') {
          this.previewImage.set(this.sanitizer.bypassSecurityTrustUrl(`data:${mime};base64,${b64}`));
        } else if (kind === 'text') {
          this.previewText.set(bytesToText(base64ToBytes(b64)));
        } else if (kind === 'pdf') {
          this.pdfBlobUrl = URL.createObjectURL(new Blob([base64ToBytes(b64)], { type: mime }));
          // iframe [src] is a RESOURCE_URL context; the blob is our own same-origin data.
          // CSP frame-src 'self' blob: permits embedding it (see index.html).
          this.pdfSafeUrl.set(this.sanitizer.bypassSecurityTrustResourceUrl(this.pdfBlobUrl));
        }
        this.previewLoading.set(false);
      },
      error: () => {
        if (seq !== this.previewSeq) {
          return;
        }
        this.previewError.set(true);
        this.previewLoading.set(false);
      },
    });
  }

  /** Opens the already-fetched PDF blob in a new tab (native viewer; CSP blocks embedding). */
  protected openPdfTab(): void {
    if (this.pdfBlobUrl) {
      window.open(this.pdfBlobUrl, '_blank', 'noopener');
    }
  }

  protected closePreview(): void {
    this.previewSeq++;
    this.previewDoc.set(null);
    this.previewImage.set(null);
    this.previewText.set('');
    this.pdfSafeUrl.set(null);
    this.previewLoading.set(false);
    this.previewError.set(false);
    this.revokePdfUrl();
  }

  /** Fetches a document's bytes and triggers a browser download with its file name. */
  protected download(doc: CaseDocument): void {
    this.cases.documentContent(doc.id).subscribe({
      next: (dto) => {
        const bytes = base64ToBytes((dto.base64content ?? '').replace(/\s/g, ''));
        const url = URL.createObjectURL(new Blob([bytes], { type: mimeOf(doc.ext) }));
        const a = document.createElement('a');
        a.href = url;
        a.download = dto.fileName || doc.name;
        document.body.appendChild(a);
        a.click();
        a.remove();
        setTimeout(() => URL.revokeObjectURL(url), 10_000);
      },
    });
  }

  private revokePdfUrl(): void {
    if (this.pdfBlobUrl) {
      URL.revokeObjectURL(this.pdfBlobUrl);
      this.pdfBlobUrl = null;
    }
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

/** Decodes a (whitespace-stripped) Base64 string into raw bytes. */
function base64ToBytes(b64: string): Uint8Array {
  const bin = atob(b64);
  const bytes = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) {
    bytes[i] = bin.charCodeAt(i);
  }
  return bytes;
}

/** Decodes UTF-8 bytes to a string for text preview. */
function bytesToText(bytes: Uint8Array): string {
  return new TextDecoder('utf-8').decode(bytes);
}
