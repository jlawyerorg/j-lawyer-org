import { ChangeDetectionStrategy, Component, DestroyRef, effect, inject, input, output, signal } from '@angular/core';
import { DomSanitizer, SafeResourceUrl, SafeUrl } from '@angular/platform-browser';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from './icon.component';
import { DocumentContentService } from './document-content.service';
import {
  base64ToBytes, bytesToText, DocPreviewKind, mimeOf, previewKindOf, PreviewDoc,
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
  imports: [TranslocoModule, IconComponent],
  template: `
    @if (doc(); as pd) {
      <div class="viewer" (click)="close()">
        <div class="viewer-box" (click)="$event.stopPropagation()">
          <header class="viewer-head">
            <span class="ext">{{ pd.ext || '—' }}</span>
            <span class="viewer-name">{{ pd.name }}</span>
            @if (kind() === 'pdf' && pdfSafeUrl()) {
              <button type="button" class="doc-btn" (click)="openPdfTab()">{{ 'preview.openTab' | transloco }}</button>
            }
            <button type="button" class="doc-btn" (click)="triggerDownload()">
              <jl-icon name="download" [size]="14" />{{ 'preview.download' | transloco }}
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
            } @else if (kind() === 'pdf' && pdfSafeUrl()) {
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
  `],
})
export class DocumentPreviewComponent {
  readonly doc = input<PreviewDoc | null>(null);
  readonly closed = output<void>();

  private readonly contentService = inject(DocumentContentService);
  private readonly sanitizer = inject(DomSanitizer);

  protected readonly kind = signal<DocPreviewKind>('none');
  protected readonly loading = signal(false);
  protected readonly error = signal(false);
  protected readonly image = signal<SafeUrl | null>(null);
  protected readonly text = signal('');
  protected readonly pdfSafeUrl = signal<SafeResourceUrl | null>(null);

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
    if (d) {
      this.contentService.download(d);
    }
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
    this.revokePdfUrl();
    this.pdfSafeUrl.set(null);
    this.image.set(null);
    this.text.set('');
    this.error.set(false);
    this.kind.set(kind);
    if (kind === 'none') {
      this.loading.set(false);
      return;
    }
    this.loading.set(true);

    this.contentService.content(d.id).subscribe({
      next: (dto) => {
        if (seq !== this.seq) {
          return;
        }
        const b64 = (dto.base64content ?? '').replace(/\s/g, '');
        const mime = mimeOf(d.ext);
        if (kind === 'image') {
          this.image.set(this.sanitizer.bypassSecurityTrustUrl(`data:${mime};base64,${b64}`));
        } else if (kind === 'text') {
          this.text.set(bytesToText(base64ToBytes(b64)));
        } else if (kind === 'pdf') {
          this.pdfBlobUrl = URL.createObjectURL(new Blob([base64ToBytes(b64)], { type: mime }));
          // iframe [src] is a RESOURCE_URL context; the blob is our own same-origin data.
          // CSP frame-src 'self' blob: permits embedding it (see index.html).
          this.pdfSafeUrl.set(this.sanitizer.bypassSecurityTrustResourceUrl(this.pdfBlobUrl));
        }
        this.loading.set(false);
      },
      error: () => {
        if (seq !== this.seq) {
          return;
        }
        this.error.set(true);
        this.loading.set(false);
      },
    });
  }

  private reset(): void {
    this.seq++;
    this.image.set(null);
    this.text.set('');
    this.pdfSafeUrl.set(null);
    this.loading.set(false);
    this.error.set(false);
    this.revokePdfUrl();
  }

  private revokePdfUrl(): void {
    if (this.pdfBlobUrl) {
      URL.revokeObjectURL(this.pdfBlobUrl);
      this.pdfBlobUrl = null;
    }
  }
}
