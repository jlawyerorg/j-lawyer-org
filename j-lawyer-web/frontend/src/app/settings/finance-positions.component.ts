import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { FinanceService, InvoicePositionTemplate } from './finance.service';
import { InvoicePositionEditorComponent } from './invoice-position-editor.component';

/** "Belegposition-Vorlagen" section: lists invoice position templates and hosts the create/edit modal. */
@Component({
  selector: 'jl-finance-positions',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent, InvoicePositionEditorComponent],
  template: `
    <div class="fl">
      <div class="fl-bar">
        <button type="button" class="btn-primary" (click)="openNew()">
          <jl-icon name="plus" [size]="15" /><span>{{ 'settings.finance.positionCreate' | transloco }}</span>
        </button>
      </div>

      @if (loading()) {
        <p class="fl-muted">{{ 'settings.loading' | transloco }}</p>
      } @else if (loadError()) {
        <p class="fl-error">{{ 'settings.loadError' | transloco }}</p>
      } @else if (items().length === 0) {
        <p class="fl-muted">{{ 'settings.empty' | transloco }}</p>
      } @else {
        <ul class="fl-list">
          @for (t of items(); track t.id) {
            <li class="fl-row">
              <div class="fl-main" (click)="openEdit(t)">
                <span class="fl-name">{{ t.name }}</span>
                @if (t.description) { <span class="fl-sub">{{ t.description }}</span> }
              </div>
              <span class="fl-badge">{{ t.taxRate }} %</span>
              <button type="button" class="icon-btn" (click)="openEdit(t)" [attr.aria-label]="'settings.rename' | transloco">
                <jl-icon name="edit" [size]="15" />
              </button>
              <button type="button" class="icon-btn danger" [disabled]="busy()" (click)="remove(t)" [attr.aria-label]="'settings.delete' | transloco">
                <jl-icon name="trash" [size]="15" />
              </button>
            </li>
          }
        </ul>
      }
    </div>

    @if (editorOpen()) {
      <jl-invoice-position-editor [template]="editItem()" [saving]="saving()" [error]="saveError()"
                                  (save)="onSave($event)" (close)="editorOpen.set(false)" />
    }
  `,
  styleUrl: './finance-list.css',
})
export class FinancePositionsComponent implements OnInit {
  private readonly api = inject(FinanceService);

  protected readonly items = signal<InvoicePositionTemplate[]>([]);
  protected readonly loading = signal(false);
  protected readonly loadError = signal(false);
  protected readonly busy = signal(false);

  protected readonly editorOpen = signal(false);
  protected readonly editItem = signal<InvoicePositionTemplate | null>(null);
  protected readonly saving = signal(false);
  protected readonly saveError = signal<string | null>(null);

  ngOnInit(): void { this.reload(); }

  private reload(): void {
    this.loading.set(true);
    this.loadError.set(false);
    this.api.listPositionTemplates().subscribe({
      next: (t) => { this.items.set(t); this.loading.set(false); },
      error: () => { this.loadError.set(true); this.loading.set(false); },
    });
  }

  protected openNew(): void { this.editItem.set(null); this.saveError.set(null); this.editorOpen.set(true); }
  protected openEdit(t: InvoicePositionTemplate): void { this.editItem.set(t); this.saveError.set(null); this.editorOpen.set(true); }

  protected onSave(t: InvoicePositionTemplate): void {
    this.saving.set(true);
    this.saveError.set(null);
    const req = this.editItem() ? this.api.updatePositionTemplate(t) : this.api.createPositionTemplate(t);
    req.subscribe({
      next: () => { this.saving.set(false); this.editorOpen.set(false); this.reload(); },
      error: () => { this.saving.set(false); this.saveError.set('save'); },
    });
  }

  protected remove(t: InvoicePositionTemplate): void {
    if (this.busy()) { return; }
    this.busy.set(true);
    this.api.deletePositionTemplate(t).subscribe({
      next: () => { this.busy.set(false); this.reload(); },
      error: () => { this.busy.set(false); },
    });
  }
}
