import {
  ChangeDetectionStrategy, Component, computed, input, OnInit, output, signal,
} from '@angular/core';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { InvoicePositionTemplate } from './finance.service';

/** Modal editor for an invoice position template: name, description, tax rate, quantity, unit price. */
@Component({
  selector: 'jl-invoice-position-editor',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent],
  template: `
    <div class="ed-backdrop" (click)="close.emit()"></div>
    <div class="ed-dialog" role="dialog" aria-modal="true">
      <header class="ed-head">
        <h2>{{ (isNew() ? 'settings.finance.positionCreate' : 'settings.finance.positionEdit') | transloco }}</h2>
        <button type="button" class="icon-btn" (click)="close.emit()" [attr.aria-label]="'settings.cancel' | transloco">
          <jl-icon name="close" [size]="18" />
        </button>
      </header>

      <div class="ed-body">
        <label class="ed-field">
          <span>{{ 'settings.finance.positionName' | transloco }}</span>
          <input type="text" [value]="draft().name" (input)="patch('name', $any($event.target).value)" />
        </label>
        <label class="ed-field">
          <span>{{ 'settings.finance.description' | transloco }}</span>
          <textarea rows="2" [value]="draft().description" (input)="patch('description', $any($event.target).value)"></textarea>
        </label>
        <div class="ed-grid">
          <label class="ed-field">
            <span>{{ 'settings.finance.units' | transloco }}</span>
            <input type="number" step="0.01" [value]="draft().units" (input)="patchNum('units', $any($event.target).value)" />
          </label>
          <label class="ed-field">
            <span>{{ 'settings.finance.unitPrice' | transloco }}</span>
            <input type="number" step="0.01" [value]="draft().unitPrice" (input)="patchNum('unitPrice', $any($event.target).value)" />
          </label>
          <label class="ed-field">
            <span>{{ 'settings.finance.taxRate' | transloco }}</span>
            <input type="number" step="0.1" [value]="draft().taxRate" (input)="patchNum('taxRate', $any($event.target).value)" />
          </label>
        </div>
        @if (error()) { <p class="ed-error">{{ error() }}</p> }
      </div>

      <footer class="ed-foot">
        <button type="button" class="btn-ghost" [disabled]="saving()" (click)="close.emit()">{{ 'settings.cancel' | transloco }}</button>
        <button type="button" class="btn-primary" [disabled]="saving() || !canSave()" (click)="submit()">
          {{ (saving() ? 'settings.saving' : 'settings.save') | transloco }}
        </button>
      </footer>
    </div>
  `,
  styleUrl: './finance-editor.css',
})
export class InvoicePositionEditorComponent implements OnInit {
  readonly template = input<InvoicePositionTemplate | null>(null);
  readonly saving = input<boolean>(false);
  readonly error = input<string | null>(null);
  readonly save = output<InvoicePositionTemplate>();
  readonly close = output<void>();

  protected readonly draft = signal<InvoicePositionTemplate>({
    name: '', description: '', taxRate: 19, units: 1, unitPrice: 0,
  });
  protected readonly isNew = computed(() => !this.template());
  protected readonly canSave = computed(() => !!this.draft().name.trim());

  ngOnInit(): void {
    const t = this.template();
    if (t) { this.draft.set({ ...t }); }
  }

  protected patch<K extends keyof InvoicePositionTemplate>(key: K, value: InvoicePositionTemplate[K]): void {
    this.draft.update((d) => ({ ...d, [key]: value }));
  }

  protected patchNum(key: 'taxRate' | 'units' | 'unitPrice', value: string): void {
    const n = parseFloat(value);
    this.patch(key, Number.isFinite(n) ? n : 0);
  }

  protected submit(): void {
    if (this.canSave() && !this.saving()) { this.save.emit(this.draft()); }
  }
}
