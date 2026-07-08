import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DecimalPipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { DocumentsService } from './documents.service';
import { SearchHit } from './search.models';
import { CasesService } from '../akten/cases.service';
import { mimeOf } from '../akten/case.models';

/** One highlighted segment of a snippet (matched terms are marked). */
interface Segment {
  text: string;
  mark: boolean;
}

const MAX_DOCS = 50;

/**
 * Dokumente module — a global, Lucene-backed fulltext search across all case documents
 * (GET /v8/search/fulltext, ACL-restricted server-side). Each hit links to its case
 * (deep link /cases/:id, where the Dokumente tab offers the full preview) and can be
 * downloaded directly. The query is mirrored to the URL (?q=) so searches are shareable.
 * OpenSpec change {@code add-web-client}, task 3.2 follow-up / 4.3.
 */
@Component({
  selector: 'jl-dokumente',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent, RouterLink, DecimalPipe],
  template: `
    <section class="docs">
      <header class="docs-head">
        <h1>{{ 'dokumente.title' | transloco }}</h1>
        @if (hits(); as h) {
          <span class="count">{{ h.length }}{{ h.length >= maxDocs ? '+' : '' }}</span>
        }
      </header>

      <label class="search">
        <jl-icon name="search" [size]="16" />
        <input type="search" [placeholder]="'dokumente.searchPlaceholder' | transloco"
               [value]="query()" (input)="onSearch($any($event.target).value)" autofocus />
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
        } @else if (hits()?.length === 0) {
          <p class="muted pad">{{ 'dokumente.empty' | transloco }}</p>
        } @else {
          @if ((hits()?.length ?? 0) >= maxDocs) {
            <p class="cap">{{ 'dokumente.capped' | transloco: { max: maxDocs } }}</p>
          }
          @for (h of hits(); track h.id) {
            <article class="hit">
              <span class="ext">{{ h.ext || '—' }}</span>
              <div class="hit-body">
                <div class="hit-top">
                  <span class="hit-name">{{ h.fileName }}</span>
                  <span class="score" [title]="'dokumente.score' | transloco">{{ h.score | number: '1.1-1' }}</span>
                </div>
                @if (h.snippet) {
                  <p class="snippet">
                    @for (seg of highlight(h.snippet); track $index) {
                      @if (seg.mark) { <mark>{{ seg.text }}</mark> } @else { {{ seg.text }} }
                    }…
                  </p>
                }
                <div class="hit-foot">
                  @if (h.archiveFileId) {
                    <a class="case-link" [routerLink]="['/cases', h.archiveFileId]">
                      <jl-icon name="cases" [size]="13" />{{ h.archiveFileNumber }} · {{ h.archiveFileName }}
                    </a>
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
  `,
  styleUrl: './dokumente.component.css',
})
export class DokumenteComponent {
  private readonly docs = inject(DocumentsService);
  private readonly cases = inject(CasesService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly maxDocs = MAX_DOCS;
  protected readonly query = signal('');
  /** null = no search run yet; [] = ran with no hits. */
  protected readonly hits = signal<SearchHit[] | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal(false);

  private searchDebounce: ReturnType<typeof setTimeout> | null = null;
  private reqSeq = 0;
  /** The term the current results belong to; guards against re-running the same search. */
  private lastSearched = '';

  constructor() {
    // The ?q= query param is the source of truth, so searches are shareable/bookmarkable
    // and browser back/forward replays them. The URL — not the input — drives the search.
    this.route.queryParamMap.pipe(takeUntilDestroyed()).subscribe((params) => {
      const q = (params.get('q') ?? '').trim();
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
    this.searchDebounce = setTimeout(() => {
      this.router.navigate([], {
        relativeTo: this.route,
        queryParams: value.trim() ? { q: value.trim() } : {},
        replaceUrl: true,
      });
    }, 300);
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

  /** Splits a snippet into plain/marked segments matching the query terms (CSP-safe, no innerHTML). */
  protected highlight(snippet: string): Segment[] {
    const terms = this.query().trim().split(/\s+/).filter((t) => t.length >= 2);
    if (!terms.length) {
      return [{ text: snippet, mark: false }];
    }
    const escaped = terms.map((t) => t.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'));
    const re = new RegExp(`(${escaped.join('|')})`, 'gi');
    const lowered = terms.map((t) => t.toLowerCase());
    // Splitting on a capturing group yields the matched terms as their own segments,
    // so a segment is a match iff it equals one of the query terms (case-insensitively).
    return snippet
      .split(re)
      .filter((s) => s.length > 0)
      .map((s) => ({ text: s, mark: lowered.includes(s.toLowerCase()) }));
  }

  /** Fetches a document's bytes and triggers a browser download with its file name. */
  protected download(h: SearchHit): void {
    this.cases.documentContent(h.id).subscribe({
      next: (dto) => {
        const bytes = base64ToBytes((dto.base64content ?? '').replace(/\s/g, ''));
        const url = URL.createObjectURL(new Blob([bytes], { type: mimeOf(h.ext) }));
        const a = document.createElement('a');
        a.href = url;
        a.download = dto.fileName || h.fileName;
        document.body.appendChild(a);
        a.click();
        a.remove();
        setTimeout(() => URL.revokeObjectURL(url), 10_000);
      },
    });
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
