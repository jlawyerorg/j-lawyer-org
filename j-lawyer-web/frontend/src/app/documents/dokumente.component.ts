import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DecimalPipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { DocumentPreviewComponent } from '../shared/document-preview.component';
import { DocumentContentService } from '../shared/document-content.service';
import { PreviewDoc, previewKindOf } from '../shared/document-preview.models';
import { DocumentsService } from './documents.service';
import { SearchHit } from './search.models';

/** One highlighted segment of a snippet (matched terms are marked). */
interface Segment {
  text: string;
  mark: boolean;
}

const MAX_DOCS = 50;

/**
 * Dokumente module — a global, Lucene-backed fulltext search across all case documents
 * (GET /v8/search/fulltext, ACL-restricted server-side). Hits can be previewed inline via
 * the shared {@link DocumentPreviewComponent}, downloaded, or opened in their case (deep
 * link /cases/:id). An optional filename-only toggle narrows the hits to those whose file
 * name matches, client-side. The query and toggle are mirrored to the URL (?q=, ?fn=) so
 * searches are shareable. OpenSpec change {@code add-web-client}.
 */
@Component({
  selector: 'jl-dokumente',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent, RouterLink, DecimalPipe, DocumentPreviewComponent],
  template: `
    <section class="docs">
      <header class="docs-head">
        <h1>{{ 'dokumente.title' | transloco }}</h1>
        @if (hits(); as h) {
          <span class="count">{{ displayHits().length }}{{ h.length >= maxDocs ? '+' : '' }}</span>
        }
      </header>

      <label class="search">
        <jl-icon name="search" [size]="16" />
        <input type="search" [placeholder]="'dokumente.searchPlaceholder' | transloco"
               [value]="query()" (input)="onSearch($any($event.target).value)" autofocus />
      </label>

      <label class="fn-toggle">
        <input type="checkbox" [checked]="fileNameOnly()" (change)="toggleFileNameOnly($any($event.target).checked)" />
        {{ 'dokumente.fileNameOnly' | transloco }}
      </label>

      <div class="results">
        @if (loading()) {
          <p class="muted pad">{{ 'dokumente.searching' | transloco }}</p>
        } @else if (error()) {
          <p class="pad">
            {{ 'dokumente.error' | transloco }}
            <button type="button" class="btn-retry" (click)="runSearch(query())">{{ 'dokumente.retry' | transloco }}</button>
          </p>
        } @else if (hits() === null) {
          <p class="muted pad hint">{{ 'dokumente.hint' | transloco }}</p>
        } @else if (displayHits().length === 0) {
          <p class="muted pad">{{ fileNameOnly() ? ('dokumente.emptyFileName' | transloco) : ('dokumente.empty' | transloco) }}</p>
        } @else {
          @if ((hits()?.length ?? 0) >= maxDocs) {
            <p class="cap">{{ 'dokumente.capped' | transloco: { max: maxDocs } }}</p>
          }
          @for (h of displayHits(); track h.id) {
            <article class="hit" [class.previewable]="canPreview(h)"
                     (click)="canPreview(h) ? preview(h) : download(h)">
              <span class="ext">{{ h.ext || '—' }}</span>
              <div class="hit-body">
                <div class="hit-top">
                  <span class="hit-name">{{ h.fileName }}</span>
                  <span class="score" [title]="'dokumente.score' | transloco">{{ h.score | number: '1.1-1' }}</span>
                </div>
                @if (h.snippet && !fileNameOnly()) {
                  <p class="snippet">
                    @for (seg of highlight(h.snippet); track $index) {
                      @if (seg.mark) { <mark>{{ seg.text }}</mark> } @else { {{ seg.text }} }
                    }…
                  </p>
                }
                <div class="hit-foot" (click)="$event.stopPropagation()">
                  @if (h.archiveFileId) {
                    <a class="case-link" [routerLink]="['/cases', h.archiveFileId]">
                      <jl-icon name="cases" [size]="13" />{{ h.archiveFileNumber }} · {{ h.archiveFileName }}
                    </a>
                  }
                  @if (canPreview(h)) {
                    <button type="button" class="hit-btn" (click)="preview(h)">
                      <jl-icon name="search" [size]="13" />{{ 'dokumente.preview' | transloco }}
                    </button>
                  }
                  <button type="button" class="hit-btn" (click)="download(h)">
                    <jl-icon name="download" [size]="13" />{{ 'dokumente.download' | transloco }}
                  </button>
                </div>
              </div>
            </article>
          }
        }
      </div>
    </section>

    <jl-document-preview [doc]="previewDoc()" (closed)="previewDoc.set(null)" />
  `,
  styleUrl: './dokumente.component.css',
})
export class DokumenteComponent {
  private readonly docs = inject(DocumentsService);
  private readonly documents = inject(DocumentContentService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly maxDocs = MAX_DOCS;
  protected readonly query = signal('');
  /** null = no search run yet; [] = ran with no hits. */
  protected readonly hits = signal<SearchHit[] | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal(false);
  /** When on, results are narrowed client-side to hits whose file name matches the terms. */
  protected readonly fileNameOnly = signal(false);
  /** The document currently shown in the shared preview overlay. */
  protected readonly previewDoc = signal<PreviewDoc | null>(null);

  /** The hits actually shown: all of them, or (in filename-only mode) those matching by name. */
  protected readonly displayHits = computed(() => {
    const rows = this.hits() ?? [];
    if (!this.fileNameOnly()) {
      return rows;
    }
    const terms = this.terms();
    if (!terms.length) {
      return rows;
    }
    return rows.filter((h) => {
      const name = h.fileName.toLowerCase();
      return terms.every((t) => name.includes(t));
    });
  });

  private searchDebounce: ReturnType<typeof setTimeout> | null = null;
  private reqSeq = 0;
  /** The term the current results belong to; guards against re-running the same search. */
  private lastSearched = '';

  constructor() {
    // The ?q= query param is the source of truth, so searches are shareable/bookmarkable
    // and browser back/forward replays them. The URL — not the input — drives the search.
    this.route.queryParamMap.pipe(takeUntilDestroyed()).subscribe((params) => {
      const q = (params.get('q') ?? '').trim();
      this.fileNameOnly.set(params.get('fn') === '1');
      this.query.set(q); // keep the input in sync (e.g. on a deep-linked load)
      if (!q) {
        this.lastSearched = '';
        this.hits.set(null);
      } else if (q !== this.lastSearched) {
        this.runSearch(q);
      }
    });
  }

  /** Debounces keystrokes, then reflects the term into the URL (?q=), which triggers the search. */
  protected onSearch(value: string): void {
    this.query.set(value);
    if (this.searchDebounce) {
      clearTimeout(this.searchDebounce);
    }
    this.searchDebounce = setTimeout(() => this.pushUrl(value.trim(), this.fileNameOnly()), 300);
  }

  /** Flips the filename-only refinement (mirrored to the URL as ?fn=1). */
  protected toggleFileNameOnly(on: boolean): void {
    this.fileNameOnly.set(on);
    this.pushUrl(this.query().trim(), on);
  }

  private pushUrl(q: string, fileNameOnly: boolean): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { q: q || null, fn: fileNameOnly ? '1' : null },
      replaceUrl: true,
    });
  }

  /** Runs the fulltext search for a term (also used for retry). */
  protected runSearch(term: string): void {
    const q = term.trim();
    if (!q) {
      this.hits.set(null);
      return;
    }
    const seq = ++this.reqSeq;
    this.lastSearched = q;
    this.loading.set(true);
    this.error.set(false);
    this.docs.searchFulltext(q, MAX_DOCS).subscribe({
      next: (rows) => {
        if (seq !== this.reqSeq) {
          return;
        }
        this.hits.set(rows);
        this.loading.set(false);
      },
      error: () => {
        if (seq !== this.reqSeq) {
          return;
        }
        this.error.set(true);
        this.loading.set(false);
      },
    });
  }

  /** The query split into lower-cased terms of length >= 2 (for highlighting + filename filter). */
  private terms(): string[] {
    return this.query().trim().toLowerCase().split(/\s+/).filter((t) => t.length >= 2);
  }

  /** Splits a snippet into plain/marked segments matching the query terms (CSP-safe, no innerHTML). */
  protected highlight(snippet: string): Segment[] {
    const terms = this.terms();
    if (!terms.length) {
      return [{ text: snippet, mark: false }];
    }
    const escaped = terms.map((t) => t.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'));
    const re = new RegExp(`(${escaped.join('|')})`, 'gi');
    // Splitting on a capturing group yields the matched terms as their own segments,
    // so a segment is a match iff it equals one of the query terms (case-insensitively).
    return snippet
      .split(re)
      .filter((s) => s.length > 0)
      .map((s) => ({ text: s, mark: terms.includes(s.toLowerCase()) }));
  }

  /** True when the document can be previewed inline (vs. download-only). */
  protected canPreview(h: SearchHit): boolean {
    return previewKindOf(h.ext) !== 'none';
  }

  /** Opens the shared preview overlay for a hit (download-only kinds just download). */
  protected preview(h: SearchHit): void {
    if (previewKindOf(h.ext) === 'none') {
      this.download(h);
      return;
    }
    this.previewDoc.set({ id: h.id, name: h.fileName, ext: h.ext });
  }

  /** Fetches a document's bytes and triggers a browser download with its file name. */
  protected download(h: SearchHit): void {
    this.documents.download({ id: h.id, name: h.fileName, ext: h.ext });
  }
}
