import { ChangeDetectionStrategy, Component, DestroyRef, effect, inject, input, output, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { trustBlobResourceUrl } from './safe-url.util';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from './icon.component';
import { HtmlEditorComponent } from './html-editor.component';
import { DocumentContentService } from './document-content.service';
import {
  base64ToBytes, BeaPreview, bytesToText, DocPreviewKind, EmailPreview, mimeOf, previewKindOf, PreviewDoc,
} from './document-preview.models';

/**
 * Reusable document-preview overlay: give it a {@link PreviewDoc} via the `doc` input and it
 * fetches the content and renders it inline — images and text directly, PDFs in a sandboxed
 * same-origin blob iframe (CSP frame-src 'self' blob:), everything else as download-only.
 * Emits `closed` when dismissed. Used by the case detail and the fulltext-search results.
 */
@Component({
  selector: 'jl-document-preview',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent, HtmlEditorComponent],
  template: `
    @if (doc(); as pd) {
      <div class="viewer" [class.embedded]="embedded()" (click)="embedded() ? null : close()">
        <div class="viewer-box" (click)="$event.stopPropagation()">
          <header class="viewer-head">
            <span class="ext">{{ pd.ext || '—' }}</span>
            <span class="viewer-name">{{ pd.name }}</span>
            @if (kind() === 'html') {
              <button type="button" class="doc-btn" (click)="popOutHtml()" [title]="'preview.editSeparate' | transloco"><jl-icon name="external" [size]="14" /></button>
            }
            @if (pdfSafeUrl()) {
              <button type="button" class="doc-btn" (click)="openPdfTab()" [title]="'preview.openTab' | transloco"><jl-icon name="external" [size]="14" /></button>
            }
            <button type="button" class="doc-btn" (click)="triggerDownload()" [title]="'preview.download' | transloco">
              <jl-icon name="download" [size]="14" />
            </button>
            <button type="button" class="viewer-close" (click)="close()" [attr.aria-label]="'preview.close' | transloco">✕</button>
          </header>
          <div class="viewer-body">
            @if (loading()) {
              <p class="muted pad">{{ 'preview.loading' | transloco }}</p>
            } @else if (error()) {
              <p class="pad">{{ 'preview.error' | transloco }}</p>
            } @else if (kind() === 'image' && image()) {
              <img class="viewer-img" [src]="image()" [alt]="pd.name" />
            } @else if (kind() === 'text') {
              <pre class="viewer-text">{{ text() }}</pre>
            } @else if (kind() === 'html') {
              <jl-html-editor [documentId]="pd.id" [embedded]="embedded()" />
            } @else if (kind() === 'eml' && eml(); as em) {
              <div class="mailview">
                <dl class="mail-head">
                  <dt>{{ 'preview.mail.subject' | transloco }}</dt><dd>{{ em.subject || '—' }}</dd>
                  <dt>{{ 'preview.mail.from' | transloco }}</dt><dd>{{ em.from || '—' }}</dd>
                  <dt>{{ 'preview.mail.to' | transloco }}</dt><dd>{{ em.to || '—' }}</dd>
                  @if (em.cc) { <dt>{{ 'preview.mail.cc' | transloco }}</dt><dd>{{ em.cc }}</dd> }
                  @if (em.date) { <dt>{{ 'preview.mail.date' | transloco }}</dt><dd>{{ em.date }}</dd> }
                </dl>
                @if (em.attachments.length) {
                  <div class="mail-atts">
                    <jl-icon name="paperclip" [size]="13" />
                    @for (a of em.attachments; track $index) {
                      <span class="att-chip">{{ a.name }}@if (a.size > 0) { <em>({{ fmtSize(a.size) }})</em> }</span>
                    }
                  </div>
                }
                @if (em.htmlBody) {
                  <div class="mail-body" [innerHTML]="em.htmlBody"></div>
                } @else {
                  <pre class="mail-body-text">{{ em.textBody }}</pre>
                }
              </div>
            } @else if (kind() === 'bea' && bea(); as bm) {
              <div class="mailview">
                <dl class="mail-head">
                  <dt>{{ 'preview.mail.subject' | transloco }}</dt><dd>{{ bm.subject || '—' }}</dd>
                  <dt>{{ 'preview.mail.from' | transloco }}</dt><dd>{{ bm.from || '—' }}</dd>
                  <dt>{{ 'preview.mail.to' | transloco }}</dt><dd>{{ bm.to || '—' }}</dd>
                  @if (bm.sent) { <dt>{{ 'preview.mail.sent' | transloco }}</dt><dd>{{ bm.sent }}</dd> }
                  @if (bm.caseNumber) { <dt>{{ 'preview.mail.caseNumber' | transloco }}</dt><dd>{{ bm.caseNumber }}</dd> }
                  @if (bm.justizReference) { <dt>{{ 'preview.mail.justizReference' | transloco }}</dt><dd>{{ bm.justizReference }}</dd> }
                </dl>
                @if (bm.attachments.length) {
                  <div class="mail-atts">
                    <jl-icon name="paperclip" [size]="13" />
                    @for (a of bm.attachments; track $index) {
                      <span class="att-chip">{{ a.name }}@if (a.size > 0) { <em>({{ fmtSize(a.size) }})</em> }</span>
                    }
                  </div>
                }
                <pre class="mail-body-text">{{ bm.body }}</pre>
              </div>
            } @else if (pdfSafeUrl()) {
              <iframe class="viewer-frame" [src]="pdfSafeUrl()" [title]="pd.name"></iframe>
            } @else {
              <p class="pad">{{ 'preview.noPreview' | transloco }}</p>
            }
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    .viewer { position: fixed; inset: 0; z-index: 50; display: grid; place-items: center; padding: 24px;
      background: color-mix(in srgb, #0b1b2c 62%, transparent); }
    .viewer-box { display: flex; flex-direction: column; width: min(1000px, 100%); height: min(88vh, 100%);
      background: var(--jl-surface); border: 1px solid var(--jl-line); border-radius: 12px; overflow: hidden;
      box-shadow: 0 12px 48px rgba(11,27,44,.32); }
    .viewer-head { display: flex; align-items: center; gap: 10px; padding: 11px 14px; border-bottom: 1px solid var(--jl-line); }
    .ext { width: 34px; height: 40px; border-radius: 5px; display: grid; place-items: center; color: #fff;
      font-size: .58rem; font-weight: 800; flex: none; background: var(--jl-blue); }
    .viewer-name { flex: 1; min-width: 0; font-weight: 650; font-size: .92rem; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .doc-btn {
      display: inline-flex; align-items: center; gap: 5px; font: inherit; font-size: .78rem; font-weight: 600;
      cursor: pointer; color: var(--jl-ink-soft); background: var(--jl-surface);
      border: 1px solid var(--jl-line-strong); border-radius: 7px; padding: 5px 10px; white-space: nowrap;
    }
    .doc-btn:hover { border-color: var(--jl-blue); color: var(--jl-blue); }
    .viewer-close { width: 30px; height: 30px; display: grid; place-items: center; font-size: 1rem; cursor: pointer;
      color: var(--jl-ink-soft); background: transparent; border: 1px solid var(--jl-line-strong); border-radius: 7px; }
    .viewer-close:hover { border-color: var(--jl-red); color: var(--jl-red); }
    .viewer-body { flex: 1 1 auto; min-height: 0; overflow: auto; background: var(--jl-surface-alt); display: flex; }
    .viewer-body .pad { padding: 28px; margin: auto; }
    .muted { color: var(--jl-ink-soft); }
    .viewer-img { max-width: 100%; max-height: 100%; margin: auto; object-fit: contain; }
    .viewer-text { margin: 0; padding: 16px 18px; width: 100%; font-family: ui-monospace, "SF Mono", Menlo, Consolas, monospace;
      font-size: .82rem; line-height: 1.5; color: var(--jl-ink); white-space: pre-wrap; word-break: break-word; }
    .viewer-frame { width: 100%; height: 100%; border: 0; background: #fff; }
    /* Structured mail / beA preview */
    .mailview { width: 100%; display: flex; flex-direction: column; background: var(--jl-surface); }
    .mail-head { margin: 0; padding: 14px 18px; display: grid; grid-template-columns: auto 1fr; gap: 4px 14px;
      border-bottom: 1px solid var(--jl-line); font-size: .84rem; }
    .mail-head dt { color: var(--jl-ink-soft); font-weight: 650; white-space: nowrap; }
    .mail-head dd { margin: 0; color: var(--jl-ink); min-width: 0; word-break: break-word; }
    .mail-atts { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; padding: 10px 18px;
      border-bottom: 1px solid var(--jl-line); color: var(--jl-ink-soft); }
    .att-chip { display: inline-flex; align-items: center; gap: 5px; font-size: .78rem; color: var(--jl-ink);
      background: var(--jl-surface-alt); border: 1px solid var(--jl-line); border-radius: 6px; padding: 3px 8px; }
    .att-chip em { color: var(--jl-ink-soft); font-style: normal; }
    .mail-body { padding: 16px 18px; overflow: auto; font-size: .88rem; line-height: 1.5; color: var(--jl-ink);
      background: #fff; }
    .mail-body img { max-width: 100%; height: auto; }
    .mail-body-text { margin: 0; padding: 16px 18px; font-family: ui-monospace, "SF Mono", Menlo, Consolas, monospace;
      font-size: .82rem; line-height: 1.5; color: var(--jl-ink); white-space: pre-wrap; word-break: break-word; }
    /* Embedded (side-panel) mode: fill the host, no overlay/backdrop/shadow. */
    .viewer.embedded { position: static; inset: auto; z-index: auto; padding: 0; background: transparent; height: 100%; display: block; }
    .viewer.embedded .viewer-box { width: 100%; height: 100%; border: 0; border-radius: 0; box-shadow: none; }
  `],
})
export class DocumentPreviewComponent {
  readonly doc = input<PreviewDoc | null>(null);
  /**
   * Optional pre-fetched Base64 content. When provided, the overlay renders (and downloads)
   * these bytes directly instead of fetching via the content service — for callers whose bytes
   * come from a non-document source (e.g. beA message attachments).
   */
  readonly inlineContent = input<string | null>(null);
  /** When true, renders in-flow filling the host (a side panel) instead of a fixed overlay. */
  readonly embedded = input(false);
  readonly closed = output<void>();

  private readonly contentService = inject(DocumentContentService);
  private readonly sanitizer = inject(DomSanitizer);

  protected readonly kind = signal<DocPreviewKind>('none');
  protected readonly loading = signal(false);
  protected readonly error = signal(false);
  protected readonly image = signal<string | null>(null);
  protected readonly text = signal('');
  protected readonly pdfSafeUrl = signal<SafeResourceUrl | null>(null);
  protected readonly eml = signal<EmailPreview | null>(null);
  protected readonly bea = signal<BeaPreview | null>(null);

  private pdfBlobUrl: string | null = null;
  private seq = 0;

  constructor() {
    effect(() => {
      const d = this.doc();
      if (d) {
        this.load(d);
      } else {
        this.reset();
      }
    });
    inject(DestroyRef).onDestroy(() => this.revokePdfUrl());
  }

  protected close(): void {
    this.reset();
    this.closed.emit();
  }

  protected triggerDownload(): void {
    const d = this.doc();
    if (!d) {
      return;
    }
    const inline = this.inlineContent();
    if (inline != null) {
      const bytes = base64ToBytes(inline.replace(/\s/g, ''));
      const url = URL.createObjectURL(new Blob([bytes], { type: mimeOf(d.ext) }));
      const a = document.createElement('a');
      a.href = url;
      a.download = d.name || 'download';
      document.body.appendChild(a);
      a.click();
      a.remove();
      setTimeout(() => URL.revokeObjectURL(url), 10_000);
      return;
    }
    this.contentService.download(d);
  }

  /** Opens the HTML rich-text editor for this document in a separate tab/window (pop-out). */
  protected popOutHtml(): void {
    const d = this.doc();
    if (!d) {
      return;
    }
    const url = new URL(`htmledit/${encodeURIComponent(d.id)}?name=${encodeURIComponent(d.name)}`, document.baseURI).href;
    window.open(url, '_blank', 'noopener');
    this.close();
  }

  /** Opens the already-fetched PDF blob in a new tab (native viewer; CSP blocks embedding). */
  protected openPdfTab(): void {
    if (this.pdfBlobUrl) {
      window.open(this.pdfBlobUrl, '_blank', 'noopener');
    }
  }

  private load(d: PreviewDoc): void {
    const kind = previewKindOf(d.ext);
    const seq = ++this.seq;
    // Read the inline input up front so this effect always depends on it (doc + content are set together).
    const inline = this.inlineContent();
    this.revokePdfUrl();
    this.pdfSafeUrl.set(null);
    this.image.set(null);
    this.text.set('');
    this.eml.set(null);
    this.bea.set(null);
    this.error.set(false);
    this.kind.set(kind);
    if (kind === 'none') {
      this.loading.set(false);
      return;
    }
    // Office documents: render the server-produced PDF (StirlingPDF); case documents only.
    if (kind === 'office') {
      if (d.source === 'contact') {
        this.loading.set(false);
        return;
      }
      this.loading.set(true);
      this.contentService.previewPdf(d.id).subscribe({
        next: (dto) => {
          if (seq !== this.seq) {
            return;
          }
          if (dto.kind === 'pdf' && dto.base64content) {
            this.render(d, dto.base64content, 'pdf', seq);
          } else {
            // No PDF rendering available (StirlingPDF not configured) — show the extracted text.
            this.kind.set('text');
            this.text.set(dto.text ?? '');
            this.loading.set(false);
          }
        },
        error: () => {
          if (seq === this.seq) {
            this.error.set(true);
            this.loading.set(false);
          }
        },
      });
      return;
    }
    // HTML: editable rich-text (WYSIWYG) editor. Case documents only — contact documents have no
    // write endpoint, so they fall back to a read-only text view of the raw HTML.
    if (kind === 'html') {
      if (d.source === 'contact') {
        this.kind.set('text');
        this.loading.set(true);
        this.contentService.content(d.id, 'contact').subscribe({
          next: (dto) => this.render(d, dto.base64content ?? '', 'text', seq),
          error: () => {
            if (seq === this.seq) {
              this.error.set(true);
              this.loading.set(false);
            }
          },
        });
        return;
      }
      this.loading.set(false); // the embedded <jl-html-editor> loads and auto-saves itself
      return;
    }
    // EML / beA: server-parsed structured previews (headers, body, attachments); case documents only.
    if (kind === 'eml' || kind === 'bea') {
      if (d.source === 'contact') {
        this.loading.set(false);
        return;
      }
      this.loading.set(true);
      const req: Observable<EmailPreview | BeaPreview> = kind === 'eml'
        ? this.contentService.emlPreview(d.id)
        : this.contentService.beaPreview(d.id);
      req.subscribe({
        next: (dto) => {
          if (seq !== this.seq) {
            return;
          }
          if (kind === 'eml') {
            this.eml.set(dto as EmailPreview);
          } else {
            this.bea.set(dto as BeaPreview);
          }
          this.loading.set(false);
        },
        error: () => {
          if (seq === this.seq) {
            this.error.set(true);
            this.loading.set(false);
          }
        },
      });
      return;
    }
    if (inline != null) {
      this.render(d, inline, kind, seq);
      return;
    }
    this.loading.set(true);

    this.contentService.content(d.id, d.source).subscribe({
      next: (dto) => this.render(d, dto.base64content ?? '', kind, seq),
      error: () => {
        if (seq !== this.seq) {
          return;
        }
        this.error.set(true);
        this.loading.set(false);
      },
    });
  }

  /** Renders already-decoded Base64 bytes into the appropriate viewer (image/text/pdf). */
  private render(d: PreviewDoc, rawB64: string, kind: DocPreviewKind, seq: number): void {
    if (seq !== this.seq) {
      return;
    }
    const b64 = (rawB64 ?? '').replace(/\s/g, '');
    const mime = mimeOf(d.ext);
    if (kind === 'image') {
      // Bind the data: URL as a plain string. Angular's built-in URL sanitizer
      // (img [src] is a URL context) already allows raster data URLs, and mimeOf()
      // only yields image/{png,jpeg,gif,webp,bmp} for the 'image' kind (never SVG),
      // so no bypassSecurityTrust* is needed here.
      this.image.set(`data:${mime};base64,${b64}`);
    } else if (kind === 'text') {
      this.text.set(bytesToText(base64ToBytes(b64)));
    } else if (kind === 'pdf') {
      this.pdfBlobUrl = URL.createObjectURL(new Blob([base64ToBytes(b64)], { type: mime }));
      // iframe [src] is a RESOURCE_URL context; the blob is our own same-origin data.
      // CSP frame-src 'self' blob: permits embedding it (see index.html).
      this.pdfSafeUrl.set(trustBlobResourceUrl(this.sanitizer, this.pdfBlobUrl));
    }
    this.loading.set(false);
  }

  private reset(): void {
    this.seq++;
    this.image.set(null);
    this.text.set('');
    this.eml.set(null);
    this.bea.set(null);
    this.pdfSafeUrl.set(null);
    this.loading.set(false);
    this.error.set(false);
    this.revokePdfUrl();
  }

  /** Human-readable byte size for an attachment chip. */
  protected fmtSize(bytes: number): string {
    if (bytes < 1024) {
      return `${bytes} B`;
    }
    if (bytes < 1024 * 1024) {
      return `${Math.round(bytes / 1024)} KB`;
    }
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  private revokePdfUrl(): void {
    if (this.pdfBlobUrl) {
      URL.revokeObjectURL(this.pdfBlobUrl);
      this.pdfBlobUrl = null;
    }
  }
}
