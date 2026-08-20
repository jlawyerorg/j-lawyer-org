import {
  ChangeDetectionStrategy, Component, computed, inject, input, OnInit, output, signal,
} from '@angular/core';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { InvoiceType } from './finance.service';

/** Modal editor for an invoice type (Belegart): display name, description and turnover flag. */
@Component({
  selector: 'jl-invoice-type-editor',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent],
  template: `
    <div class="ed-backdrop" (click)="close.emit()"></div>
    <div class="ed-dialog" role="dialog" aria-modal="true">
      <header class="ed-head">
        <h2>{{ (isNew() ? 'settings.finance.typeCreate' : 'settings.finance.typeEdit') | transloco }}</h2>
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
          <span>{{ 'settings.finance.description' | transloco }}</span>
          <textarea rows="2" [value]="draft().description" (input)="patch('description', $any($event.target).value)"></textarea>
        </label>
        <label class="ed-check">
          <input type="checkbox" [checked]="draft().turnOver" (change)="patch('turnOver', $any($event.target).checked)" />
          <span>{{ 'settings.finance.turnOver' | transloco }}</span>
        </label>
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
export class InvoiceTypeEditorComponent implements OnInit {
  readonly type = input<InvoiceType | null>(null);
  readonly saving = input<boolean>(false);
  readonly error = input<string | null>(null);
  readonly save = output<InvoiceType>();
  readonly close = output<void>();

  protected readonly draft = signal<InvoiceType>({ displayName: '', description: '', turnOver: true });
  protected readonly isNew = computed(() => !this.type());
  protected readonly canSave = computed(() => !!this.draft().displayName.trim());

  ngOnInit(): void {
    const t = this.type();
    if (t) { this.draft.set({ ...t }); }
  }

  protected patch<K extends keyof InvoiceType>(key: K, value: InvoiceType[K]): void {
    this.draft.update((d) => ({ ...d, [key]: value }));
  }

  protected submit(): void {
    if (this.canSave() && !this.saving()) { this.save.emit(this.draft()); }
  }
}
