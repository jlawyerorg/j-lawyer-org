import {
  ChangeDetectionStrategy, Component, computed, inject, input, OnInit, output, signal,
} from '@angular/core';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { FinanceService, InvoicePool } from './finance.service';

/**
 * Modal editor for an invoice numbering pool (Belegnummernkreis): name, numbering pattern, indices,
 * payment term and flags, with a live "preview" of the next numbers the pattern would produce. The
 * running index (lastIndex) can only be edited when "manual adjust" is enabled, mirroring the desktop.
 */
@Component({
  selector: 'jl-invoice-pool-editor',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent],
  template: `
    <div class="ed-backdrop" (click)="close.emit()"></div>
    <div class="ed-dialog" role="dialog" aria-modal="true">
      <header class="ed-head">
        <h2>{{ (isNew() ? 'settings.finance.poolCreate' : 'settings.finance.poolEdit') | transloco }}</h2>
        <button type="button" class="icon-btn" (click)="close.emit()" [attr.aria-label]="'settings.cancel' | transloco">
          <jl-icon name="close" [size]="18" />
        </button>
      </header>

      <div class="ed-body">
        <label class="ed-field">
          <span>{{ 'settings.finance.displayName' | transloco }}</span>
          <input type="text" [value]="draft().displayName" (input)="patch('displayName', $any($event.target).value)" />
        </label>
        <label class="ed-field">
          <span>{{ 'settings.finance.pattern' | transloco }}</span>
          <input type="text" [value]="draft().pattern" (input)="patch('pattern', $any($event.target).value)" />
          <span class="ed-hint">{{ 'settings.finance.patternHint' | transloco }}</span>
        </label>
        <div class="ed-grid">
          <label class="ed-field">
            <span>{{ 'settings.finance.startIndex' | transloco }}</span>
            <input type="number" [value]="draft().startIndex" (input)="patchNum('startIndex', $any($event.target).value)" />
          </label>
          <label class="ed-field">
            <span>{{ 'settings.finance.paymentTerm' | transloco }}</span>
            <input type="number" [value]="draft().paymentTerm" (input)="patchNum('paymentTerm', $any($event.target).value)" />
          </label>
        </div>
        <div class="ed-field">
          <span>{{ 'settings.finance.lastIndex' | transloco }}</span>
          <div class="ed-inline">
            <span class="ed-value">{{ draft().lastIndex }}</span>
            <button type="button" class="btn-ghost btn-sm" (click)="resetIndex()">{{ 'settings.finance.resetIndex' | transloco }}</button>
          </div>
        </div>
        <label class="ed-check">
          <input type="checkbox" [checked]="draft().manualAdjust" (change)="patch('manualAdjust', $any($event.target).checked)" />
          <span>{{ 'settings.finance.manualAdjust' | transloco }}</span>
        </label>
        <label class="ed-check">
          <input type="checkbox" [checked]="draft().smallBusiness" (change)="patch('smallBusiness', $any($event.target).checked)" />
          <span>{{ 'settings.finance.showVat' | transloco }}</span>
        </label>

        @if (preview().length) {
          <div class="ed-preview">
            <h3>{{ 'settings.finance.preview' | transloco }}</h3>
            <ul>@for (n of preview(); track $index) { <li>{{ n }}</li> }</ul>
          </div>
        }
        @if (error()) { <p class="ed-error">{{ error() }}</p> }
      </div>

      <footer class="ed-foot">
        <button type="button" class="btn-ghost" [disabled]="previewing()" (click)="runPreview()">
          {{ (previewing() ? 'settings.loading' : 'settings.finance.preview') | transloco }}
        </button>
        <span class="spacer"></span>
        <button type="button" class="btn-ghost" [disabled]="saving()" (click)="close.emit()">{{ 'settings.cancel' | transloco }}</button>
        <button type="button" class="btn-primary" [disabled]="saving() || !canSave()" (click)="submit()">
          {{ (saving() ? 'settings.saving' : 'settings.save') | transloco }}
        </button>
      </footer>
    </div>
  `,
  styleUrl: './finance-editor.css',
})
export class InvoicePoolEditorComponent implements OnInit {
  private readonly api = inject(FinanceService);

  readonly pool = input<InvoicePool | null>(null);
  readonly saving = input<boolean>(false);
  readonly error = input<string | null>(null);
  readonly save = output<InvoicePool>();
  readonly close = output<void>();

  protected readonly draft = signal<InvoicePool>({
    displayName: '', pattern: '', manualAdjust: true, smallBusiness: false,
    startIndex: 1, lastIndex: 0, paymentTerm: 14,
  });
  protected readonly preview = signal<string[]>([]);
  protected readonly previewing = signal(false);

  protected readonly isNew = computed(() => !this.pool());
  protected readonly canSave = computed(() => !!this.draft().displayName.trim() && !!this.draft().pattern.trim());

  ngOnInit(): void {
    const p = this.pool();
    if (p) { this.draft.set({ ...p }); }
  }

  protected patch<K extends keyof InvoicePool>(key: K, value: InvoicePool[K]): void {
    this.draft.update((d) => ({ ...d, [key]: value }));
    this.preview.set([]);
  }

  protected patchNum(key: 'startIndex' | 'paymentTerm', value: string): void {
    const n = parseInt(value, 10);
    this.patch(key, Number.isFinite(n) ? n : 0);
  }

  /** Resets the running counter to 0 (persisted on save), mirroring the desktop "Zurücksetzen". */
  protected resetIndex(): void {
    this.patch('lastIndex', 0);
  }

  protected runPreview(): void {
    if (this.previewing()) { return; }
    this.previewing.set(true);
    this.api.previewPool(this.draft()).subscribe({
      next: (rows) => { this.preview.set(rows ?? []); this.previewing.set(false); },
      error: () => { this.preview.set([]); this.previewing.set(false); },
    });
  }

  protected submit(): void {
    if (this.canSave() && !this.saving()) { this.save.emit(this.draft()); }
  }
}
