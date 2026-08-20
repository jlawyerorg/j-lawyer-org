import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Subject } from 'rxjs';
import { debounceTime, switchMap } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { AuthService } from '../core/auth/auth.service';
import { CaseNumbering, CaseNumberingService } from './case-numbering.service';

const PRESETS: { label: string; pattern: string }[] = [
  { label: 'nnnnn/YY', pattern: 'nnnnn/YY' },
  { label: 'NNNNN/YYYY', pattern: 'NNNNN/YYYY' },
  { label: 'YYMMDD/RRRRR', pattern: 'YYMMDD/RRRRR' },
  { label: 'CCCCC', pattern: 'CCCCC' },
];

const EMPTY: CaseNumbering = {
  pattern: 'nnnnn/YY', startFrom: 1, increment: 1,
  extensionEnabled: false, dividerMain: '', dividerExt: '',
  prefixEnabled: false, prefix: '', suffixEnabled: false, suffix: '',
  lawyerAbbrevEnabled: false, groupAbbrevEnabled: false,
};

/**
 * Editor for the server-wide case-number (Aktenzeichen) configuration: numbering pattern (with
 * presets), start/increment, and an optional extension (dividers, fixed prefix/suffix, lawyer and
 * group abbreviations). A debounced live preview shows a sample number or the pattern's validation
 * error. Saving needs `adminRole` (also enforced server-side).
 */
@Component({
  selector: 'jl-case-numbering',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule],
  template: `
    <div class="cn">
      @if (!canEdit()) { <p class="cn-hint">{{ 'settings.readOnlyHint' | transloco }}</p> }

      @if (loading()) {
        <p class="cn-muted">{{ 'settings.loading' | transloco }}</p>
      } @else if (loadError()) {
        <p class="cn-error">{{ 'settings.loadError' | transloco }}</p>
      } @else {
        <fieldset class="cn-group">
          <legend>{{ 'settings.caseNumbering.scheme' | transloco }}</legend>

          <label class="cn-field">
            <span>{{ 'settings.caseNumbering.pattern' | transloco }}</span>
            <input type="text" [value]="draft().pattern" [disabled]="!canEdit()"
                   (input)="patch('pattern', $any($event.target).value)" />
          </label>
          <div class="cn-presets">
            @for (p of presets; track p.pattern) {
              <button type="button" class="cn-chip" [disabled]="!canEdit()" (click)="patch('pattern', p.pattern)">{{ p.label }}</button>
            }
          </div>
          <p class="cn-note">{{ 'settings.caseNumbering.patternHint' | transloco }}</p>

          <div class="cn-grid">
            <label class="cn-field">
              <span>{{ 'settings.caseNumbering.startFrom' | transloco }}</span>
              <input type="number" step="1" min="1" [value]="draft().startFrom" [disabled]="!canEdit()"
                     (input)="patchNum('startFrom', $any($event.target).value)" />
            </label>
            <label class="cn-field">
              <span>{{ 'settings.caseNumbering.increment' | transloco }}</span>
              <input type="number" step="1" min="1" max="10" [value]="draft().increment" [disabled]="!canEdit()"
                     (input)="patchNum('increment', $any($event.target).value)" />
            </label>
          </div>

          <div class="cn-preview">
            <span class="cn-preview-label">{{ 'settings.caseNumbering.preview' | transloco }}</span>
            @if (previewError()) {
              <span class="cn-preview-error">{{ previewError() }}</span>
            } @else if (previewItems().length === 0) {
              <span class="cn-preview-value">…</span>
            } @else {
              <ul class="cn-preview-list">
                @for (s of previewItems(); track $index) {
                  <li>{{ s }}</li>
                }
              </ul>
            }
          </div>
        </fieldset>

        <fieldset class="cn-group">
          <legend>{{ 'settings.caseNumbering.extension' | transloco }}</legend>
          <label class="cn-check">
            <input type="checkbox" [checked]="draft().extensionEnabled" [disabled]="!canEdit()"
                   (change)="patch('extensionEnabled', $any($event.target).checked)" />
            <span>{{ 'settings.caseNumbering.extensionEnabled' | transloco }}</span>
          </label>

          @if (draft().extensionEnabled) {
            <div class="cn-ext">
              <div class="cn-grid">
                <label class="cn-field">
                  <span>{{ 'settings.caseNumbering.dividerMain' | transloco }}</span>
                  <input type="text" [value]="draft().dividerMain" [disabled]="!canEdit()"
                         (input)="patch('dividerMain', $any($event.target).value)" />
                </label>
                <label class="cn-field">
                  <span>{{ 'settings.caseNumbering.dividerExt' | transloco }}</span>
                  <input type="text" [value]="draft().dividerExt" [disabled]="!canEdit()"
                         (input)="patch('dividerExt', $any($event.target).value)" />
                </label>
              </div>

              <div class="cn-row">
                <label class="cn-check">
                  <input type="checkbox" [checked]="draft().prefixEnabled" [disabled]="!canEdit()"
                         (change)="patch('prefixEnabled', $any($event.target).checked)" />
                  <span>{{ 'settings.caseNumbering.prefix' | transloco }}</span>
                </label>
                <input type="text" class="cn-inline" [value]="draft().prefix" [disabled]="!canEdit() || !draft().prefixEnabled"
                       (input)="patch('prefix', $any($event.target).value)" />
              </div>

              <div class="cn-row">
                <label class="cn-check">
                  <input type="checkbox" [checked]="draft().suffixEnabled" [disabled]="!canEdit()"
                         (change)="patch('suffixEnabled', $any($event.target).checked)" />
                  <span>{{ 'settings.caseNumbering.suffix' | transloco }}</span>
                </label>
                <input type="text" class="cn-inline" [value]="draft().suffix" [disabled]="!canEdit() || !draft().suffixEnabled"
                       (input)="patch('suffix', $any($event.target).value)" />
              </div>

              <label class="cn-check">
                <input type="checkbox" [checked]="draft().lawyerAbbrevEnabled" [disabled]="!canEdit()"
                       (change)="patch('lawyerAbbrevEnabled', $any($event.target).checked)" />
                <span>{{ 'settings.caseNumbering.lawyerAbbrev' | transloco }}</span>
              </label>
              <label class="cn-check">
                <input type="checkbox" [checked]="draft().groupAbbrevEnabled" [disabled]="!canEdit()"
                       (change)="patch('groupAbbrevEnabled', $any($event.target).checked)" />
                <span>{{ 'settings.caseNumbering.groupAbbrev' | transloco }}</span>
              </label>
            </div>
          }
        </fieldset>

        <div class="cn-foot">
          @if (savedOk()) { <span class="cn-ok">{{ 'settings.savedOk' | transloco }}</span> }
          @if (saveError()) { <span class="cn-error">{{ saveError() }}</span> }
          <button type="button" class="btn-primary" [disabled]="!canEdit() || saving()" (click)="submit()">
            {{ (saving() ? 'settings.saving' : 'settings.save') | transloco }}
          </button>
        </div>
      }
    </div>
  `,
  styles: [`
    :host { display: block; }
    .cn { max-width: 640px; }
    .cn-hint { margin: 0 0 12px; font-size: .82rem; color: #b26a00; }
    .cn-muted { color: var(--jl-ink-soft); font-size: .88rem; }
    .cn-error { color: var(--jl-red); font-size: .84rem; }
    .cn-group { border: 1px solid var(--jl-line); border-radius: 10px; padding: 12px 14px 14px; margin: 0 0 14px; }
    .cn-group legend { font-size: .8rem; font-weight: 700; color: var(--jl-ink-soft); padding: 0 6px; text-transform: uppercase; letter-spacing: .04em; }
    .cn-field { display: flex; flex-direction: column; gap: 5px; font-size: .8rem; color: var(--jl-ink-faint); }
    .cn-field input { font: inherit; font-size: .9rem; color: var(--jl-ink); padding: 8px 10px;
      border: 1px solid var(--jl-line-strong); border-radius: 8px; background: var(--jl-surface); outline: none; }
    .cn-field input:focus { border-color: var(--jl-blue); }
    .cn-field input:disabled { opacity: .6; }
    .cn-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-top: 10px; }
    .cn-presets { display: flex; flex-wrap: wrap; gap: 6px; margin: 8px 0 0; }
    .cn-chip { font: inherit; font-size: .78rem; padding: 4px 10px; border: 1px solid var(--jl-line-strong);
      border-radius: 999px; background: var(--jl-surface); color: var(--jl-ink); cursor: pointer; }
    .cn-chip:hover:not(:disabled) { border-color: var(--jl-blue); color: var(--jl-blue); }
    .cn-chip:disabled { opacity: .5; cursor: default; }
    .cn-note { margin: 8px 0 0; font-size: .78rem; color: var(--jl-ink-faint); }
    .cn-preview { display: flex; flex-direction: column; gap: 6px; margin-top: 12px; padding: 8px 10px;
      background: var(--jl-surface-alt); border-radius: 8px; }
    .cn-preview-label { font-size: .78rem; color: var(--jl-ink-faint); }
    .cn-preview-value { font-family: ui-monospace, Menlo, Consolas, monospace; font-size: .95rem; font-weight: 600; color: var(--jl-ink); }
    .cn-preview-list { list-style: none; margin: 0; padding: 0; display: grid;
      grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 2px 14px; }
    .cn-preview-list li { font-family: ui-monospace, Menlo, Consolas, monospace; font-size: .9rem; color: var(--jl-ink); }
    .cn-preview-error { font-size: .82rem; color: var(--jl-red); }
    .cn-check { display: flex; align-items: center; gap: 8px; font-size: .88rem; color: var(--jl-ink); margin: 8px 0 0; cursor: pointer; }
    .cn-check input { width: 16px; height: 16px; }
    .cn-ext { margin-top: 10px; padding-top: 10px; border-top: 1px solid var(--jl-line); }
    .cn-row { display: flex; align-items: center; gap: 12px; }
    .cn-inline { flex: 1 1 auto; font: inherit; font-size: .9rem; color: var(--jl-ink); padding: 7px 10px;
      border: 1px solid var(--jl-line-strong); border-radius: 8px; background: var(--jl-surface); outline: none; }
    .cn-inline:disabled { opacity: .5; }
    .cn-foot { display: flex; align-items: center; justify-content: flex-end; gap: 12px; margin-top: 4px; }
    .cn-ok { color: var(--jl-green, #1a7f37); font-size: .84rem; }
    .btn-primary { font: inherit; font-size: .86rem; font-weight: 600; padding: 9px 18px; border: 0; border-radius: 8px;
      background: var(--jl-blue); color: #fff; cursor: pointer; }
    .btn-primary:hover:not(:disabled) { filter: brightness(1.06); }
    .btn-primary:disabled { opacity: .55; cursor: default; }
    @media (max-width: 560px) { .cn-grid { grid-template-columns: 1fr; } }
  `],
})
export class CaseNumberingComponent implements OnInit {
  private readonly api = inject(CaseNumberingService);
  private readonly auth = inject(AuthService);
  private readonly transloco = inject(TranslocoService);

  protected readonly presets = PRESETS;
  protected readonly draft = signal<CaseNumbering>({ ...EMPTY });
  protected readonly loading = signal(false);
  protected readonly loadError = signal(false);
  protected readonly saving = signal(false);
  protected readonly saveError = signal<string | null>(null);
  protected readonly savedOk = signal(false);
  protected readonly previewItems = signal<string[]>([]);
  protected readonly previewError = signal<string | null>(null);

  protected readonly canEdit = computed(() => this.auth.hasRole('adminRole'));

  private readonly previewInput = new Subject<CaseNumbering>();

  constructor() {
    this.previewInput.pipe(
      debounceTime(300),
      switchMap((c) => this.api.preview(c)),
      takeUntilDestroyed(),
    ).subscribe({
      next: (samples) => { this.previewItems.set(samples); this.previewError.set(null); },
      error: (e: HttpErrorResponse) => { this.previewItems.set([]); this.previewError.set(this.msg(e, 'settings.caseNumbering.invalidPattern')); },
    });
  }

  ngOnInit(): void {
    this.loading.set(true);
    this.loadError.set(false);
    this.api.get().subscribe({
      next: (c) => { this.draft.set({ ...EMPTY, ...c }); this.loading.set(false); this.refreshPreview(); },
      error: () => { this.loadError.set(true); this.loading.set(false); },
    });
  }

  protected patch<K extends keyof CaseNumbering>(key: K, value: CaseNumbering[K]): void {
    this.draft.update((d) => ({ ...d, [key]: value }));
    this.savedOk.set(false);
    this.refreshPreview();
  }

  protected patchNum(key: 'startFrom' | 'increment', value: string): void {
    const n = parseInt(value, 10);
    this.patch(key, Number.isFinite(n) ? n : 0);
  }

  private refreshPreview(): void {
    this.previewInput.next(this.draft());
  }

  protected submit(): void {
    if (!this.canEdit() || this.saving()) { return; }
    this.saving.set(true);
    this.saveError.set(null);
    this.savedOk.set(false);
    this.api.save(this.draft()).subscribe({
      next: () => { this.saving.set(false); this.savedOk.set(true); },
      error: (e: HttpErrorResponse) => { this.saving.set(false); this.saveError.set(this.msg(e, 'settings.saveError')); },
    });
  }

  /** Prefers the server's plain-text message (HTTP 409); otherwise the translated fallback key. */
  private msg(e: HttpErrorResponse, fallbackKey: string): string {
    const body = e?.error;
    if (typeof body === 'string' && body.trim()) { return body.trim(); }
    return this.transloco.translate(fallbackKey);
  }
}
