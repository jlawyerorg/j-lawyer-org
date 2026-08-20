import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { FinanceService, InvoicePool } from './finance.service';
import { InvoicePoolEditorComponent } from './invoice-pool-editor.component';

/** "Belegnummernkreise" section: lists invoice numbering pools and hosts the create/edit modal. */
@Component({
  selector: 'jl-finance-pools',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent, InvoicePoolEditorComponent],
  template: `
    <div class="fl">
      <div class="fl-bar">
        <button type="button" class="btn-primary" (click)="openNew()">
          <jl-icon name="plus" [size]="15" /><span>{{ 'settings.finance.poolCreate' | transloco }}</span>
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
          @for (p of items(); track p.id) {
            <li class="fl-row">
              <div class="fl-main" (click)="openEdit(p)">
                <span class="fl-name">{{ p.displayName }}</span>
                <span class="fl-sub fl-mono">{{ p.pattern }}</span>
              </div>
              <button type="button" class="icon-btn" (click)="openEdit(p)" [attr.aria-label]="'settings.rename' | transloco">
                <jl-icon name="edit" [size]="15" />
              </button>
              <button type="button" class="icon-btn danger" [disabled]="busy()" (click)="remove(p)" [attr.aria-label]="'settings.delete' | transloco">
                <jl-icon name="trash" [size]="15" />
              </button>
            </li>
          }
        </ul>
      }
    </div>

    @if (editorOpen()) {
      <jl-invoice-pool-editor [pool]="editItem()" [saving]="saving()" [error]="saveError()"
                              (save)="onSave($event)" (close)="editorOpen.set(false)" />
    }
  `,
  styleUrl: './finance-list.css',
})
export class FinancePoolsComponent implements OnInit {
  private readonly api = inject(FinanceService);

  protected readonly items = signal<InvoicePool[]>([]);
  protected readonly loading = signal(false);
  protected readonly loadError = signal(false);
  protected readonly busy = signal(false);

  protected readonly editorOpen = signal(false);
  protected readonly editItem = signal<InvoicePool | null>(null);
  protected readonly saving = signal(false);
  protected readonly saveError = signal<string | null>(null);

  ngOnInit(): void { this.reload(); }

  private reload(): void {
    this.loading.set(true);
    this.loadError.set(false);
    this.api.listPools().subscribe({
      next: (p) => { this.items.set(p); this.loading.set(false); },
      error: () => { this.loadError.set(true); this.loading.set(false); },
    });
  }

  protected openNew(): void { this.editItem.set(null); this.saveError.set(null); this.editorOpen.set(true); }
  protected openEdit(p: InvoicePool): void { this.editItem.set(p); this.saveError.set(null); this.editorOpen.set(true); }

  protected onSave(p: InvoicePool): void {
    this.saving.set(true);
    this.saveError.set(null);
    const req = this.editItem() ? this.api.updatePool(p) : this.api.createPool(p);
    req.subscribe({
      next: () => { this.saving.set(false); this.editorOpen.set(false); this.reload(); },
      error: () => { this.saving.set(false); this.saveError.set('save'); },
    });
  }

  protected remove(p: InvoicePool): void {
    if (this.busy()) { return; }
    this.busy.set(true);
    this.api.deletePool(p).subscribe({
      next: () => { this.busy.set(false); this.reload(); },
      error: () => { this.busy.set(false); },
    });
  }
}
