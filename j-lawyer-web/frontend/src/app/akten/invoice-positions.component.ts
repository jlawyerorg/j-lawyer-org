import { ChangeDetectionStrategy, Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { CasesService } from './cases.service';
import { InvoicePositionItem, InvoicePositionWrite } from './case.models';

interface Draft {
  id?: string;
  name: string;
  description: string;
  units: number;
  unitPrice: number;
  taxRate: number;
}

/**
 * Modal that manages an invoice's line items ("Positionen"): lists them and lets the user add, edit
 * and delete positions through a single form (Bezeichnung, Menge, Einzelpreis, MwSt → Summe). Each
 * write recalculates the invoice totals server-side; the parent is notified via (changed) so it can
 * refresh the invoice list. Requires an already-persisted invoice (positions reference its id).
 */
@Component({
  selector: 'jl-invoice-positions',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, TranslocoModule, IconComponent, DecimalPipe],
  template: `
    <div class="backdrop" (click)="close.emit()"></div>
    <div class="dialog" role="dialog" aria-modal="true">
      <header class="dh">
        <h2>{{ 'akten.inv.positions' | transloco }}</h2>
        <span class="sub">{{ invoiceLabel() }}</span>
        <span class="spacer"></span>
        <button type="button" class="x" (click)="close.emit()" [attr.aria-label]="'akten.editor.cancel' | transloco">
          <jl-icon name="close" [size]="18" />
        </button>
      </header>

      <div class="db">
        @if (loading()) {
          <p class="muted">{{ 'akten.loading' | transloco }}</p>
        } @else {
          <div class="pos-scroll">
            <table class="pos-table">
              <thead>
                <tr>
                  <th>{{ 'akten.inv.col.name' | transloco }}</th>
                  <th class="num">{{ 'akten.inv.col.units' | transloco }}</th>
                  <th class="num">{{ 'akten.inv.col.price' | transloco }}</th>
                  <th class="num">{{ 'akten.inv.col.tax' | transloco }}</th>
                  <th class="num">{{ 'akten.inv.col.total' | transloco }}</th>
                  <th class="c-act"></th>
                </tr>
              </thead>
              <tbody>
                @for (p of positions(); track p.id) {
                  <tr [class.editing]="draft().id === p.id">
                    <td>
                      <span class="pn">{{ p.name || '—' }}</span>
                      @if (p.description) { <span class="pd">{{ p.description }}</span> }
                    </td>
                    <td class="num">{{ p.units | number: '1.0-2' }}</td>
                    <td class="num">{{ p.unitPrice | number: '1.2-2' }}</td>
                    <td class="num">{{ p.taxRate | number: '1.0-2' }} %</td>
                    <td class="num">{{ p.total | number: '1.2-2' }}</td>
                    <td class="c-act">
                      <span class="row-actions">
                        <button type="button" class="row-edit" (click)="edit(p)" [title]="'akten.inv.pos.edit' | transloco">
                          <jl-icon name="edit" [size]="15" />
                        </button>
                        <button type="button" class="row-del" [disabled]="busyId() === p.id"
                                (click)="remove(p)" [title]="'akten.inv.pos.delete' | transloco">
                          <jl-icon name="trash" [size]="15" />
                        </button>
                      </span>
                    </td>
                  </tr>
                } @empty {
                  <tr><td colspan="6" class="muted pad">{{ 'akten.inv.noPositions' | transloco }}</td></tr>
                }
              </tbody>
              <tfoot>
                <tr>
                  <td colspan="4">{{ 'akten.inv.netTotal' | transloco }}</td>
                  <td class="num">{{ netTotal() | number: '1.2-2' }}</td>
                  <td></td>
                </tr>
              </tfoot>
            </table>
          </div>

          <!-- Add / edit form -->
          <div class="pos-form">
            <div class="pf-title">{{ (draft().id ? 'akten.inv.pos.edit' : 'akten.inv.pos.add') | transloco }}</div>
            <label class="fld">
              <span class="lbl">{{ 'akten.inv.col.name' | transloco }}</span>
              <input type="text" [ngModel]="draft().name" (ngModelChange)="upd('name', $event)" />
            </label>
            <label class="fld">
              <span class="lbl">{{ 'akten.inv.pos.description' | transloco }}</span>
              <textarea rows="2" [ngModel]="draft().description" (ngModelChange)="upd('description', $event)"></textarea>
            </label>
            <div class="row">
              <label class="fld">
                <span class="lbl">{{ 'akten.inv.col.units' | transloco }}</span>
                <input type="number" step="0.01" [ngModel]="draft().units" (ngModelChange)="upd('units', toNum($event))" />
              </label>
              <label class="fld">
                <span class="lbl">{{ 'akten.inv.col.price' | transloco }}</span>
                <input type="number" step="0.01" [ngModel]="draft().unitPrice" (ngModelChange)="upd('unitPrice', toNum($event))" />
              </label>
              <label class="fld">
                <span class="lbl">{{ 'akten.inv.col.tax' | transloco }}</span>
                <input type="number" step="0.1" [ngModel]="draft().taxRate" (ngModelChange)="upd('taxRate', toNum($event))" />
              </label>
              <div class="fld">
                <span class="lbl">{{ 'akten.inv.col.total' | transloco }}</span>
                <span class="line-total">{{ draftTotal() | number: '1.2-2' }}</span>
              </div>
            </div>
            <div class="pf-actions">
              @if (draft().id) {
                <button type="button" class="btn" (click)="resetDraft()">{{ 'akten.inv.pos.newBtn' | transloco }}</button>
              }
              <span class="spacer"></span>
              <button type="button" class="btn primary" [disabled]="!draft().name.trim() || saving()" (click)="saveDraft()">
                {{ (draft().id ? 'akten.editor.save' : 'akten.inv.pos.addBtn') | transloco }}
              </button>
            </div>
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    :host { position: fixed; inset: 0; z-index: 60; display: grid; place-items: center; }
    .backdrop { position: absolute; inset: 0; background: rgba(6, 14, 22, .5); }
    .dialog { position: relative; width: min(760px, 96vw); max-height: 92dvh; display: flex; flex-direction: column;
      background: var(--jl-surface); border: 1px solid var(--jl-line); border-radius: 14px; box-shadow: 0 24px 60px rgba(0,0,0,.32); overflow: hidden; }
    .dh { flex: none; display: flex; align-items: center; gap: 10px; padding: 14px 18px; border-bottom: 1px solid var(--jl-line); }
    .dh h2 { margin: 0; font-size: 1.05rem; font-weight: 800; }
    .dh .sub { font-size: .82rem; color: var(--jl-ink-soft); }
    .dh .spacer { flex: 1; }
    .x { display: inline-grid; place-items: center; width: 32px; height: 32px; border-radius: 8px; border: 0; background: transparent; color: var(--jl-ink-soft); cursor: pointer; }
    .x:hover { background: var(--jl-surface-alt); }
    .db { flex: 1 1 auto; min-height: 0; overflow-y: auto; padding: 14px 18px; display: flex; flex-direction: column; gap: 14px; }
    .pos-scroll { overflow-x: auto; max-width: 100%; }
    .pos-table { width: 100%; border-collapse: collapse; font-size: .84rem; }
    .pos-table th, .pos-table td { padding: 7px 10px; text-align: left; border-bottom: 1px solid var(--jl-line); vertical-align: top; }
    .pos-table thead th { font-size: .66rem; text-transform: uppercase; letter-spacing: .05em; color: var(--jl-ink-faint); font-weight: 700; }
    .pos-table .num { text-align: right; font-variant-numeric: tabular-nums; white-space: nowrap; }
    .pos-table .c-act { text-align: right; width: 1%; white-space: nowrap; }
    .pos-table tbody tr.editing { background: color-mix(in srgb, var(--jl-blue) 8%, transparent); }
    .pos-table tfoot td { border-top: 2px solid var(--jl-line-strong); border-bottom: 0; font-weight: 800; }
    .pn { display: block; font-weight: 600; }
    .pd { display: block; font-size: .78rem; color: var(--jl-ink-soft); white-space: pre-wrap; }
    .row-actions { display: inline-flex; gap: 4px; justify-content: flex-end; }
    .row-edit, .row-del { display: inline-grid; place-items: center; width: 28px; height: 28px; border-radius: 7px;
      border: 1px solid var(--jl-line-strong); background: var(--jl-surface); color: var(--jl-ink-soft); cursor: pointer; }
    .row-edit:hover { border-color: var(--jl-blue); color: var(--jl-blue); background: color-mix(in srgb, var(--jl-blue) 10%, transparent); }
    .row-del:hover { border-color: var(--jl-red); color: var(--jl-red); background: color-mix(in srgb, var(--jl-red) 10%, transparent); }
    .row-del:disabled { opacity: .4; cursor: default; }
    .muted { color: var(--jl-ink-faint); }
    .pad { padding: 16px 10px; text-align: center; }
    .pos-form { border: 1px solid var(--jl-line); border-radius: 10px; padding: 12px; display: flex; flex-direction: column; gap: 10px; background: var(--jl-surface-alt); }
    .pf-title { font-size: .74rem; font-weight: 800; text-transform: uppercase; letter-spacing: .03em; color: var(--jl-ink-soft); }
    .row { display: flex; gap: 10px; }
    .fld { display: flex; flex-direction: column; gap: 5px; flex: 1; min-width: 0; }
    .lbl { font-size: .7rem; font-weight: 700; color: var(--jl-ink-faint); text-transform: uppercase; letter-spacing: .03em; }
    input, textarea { font: inherit; font-size: .88rem; padding: 8px 10px; border: 1px solid var(--jl-line-strong); border-radius: 8px; background: var(--jl-surface); color: var(--jl-ink); width: 100%; }
    input:focus, textarea:focus { outline: none; border-color: var(--jl-blue); }
    textarea { resize: vertical; }
    .line-total { font-size: .92rem; font-weight: 800; font-variant-numeric: tabular-nums; padding: 8px 0; }
    .pf-actions { display: flex; align-items: center; gap: 10px; }
    .pf-actions .spacer { flex: 1; }
    .btn { font: inherit; font-size: .85rem; font-weight: 650; padding: 8px 16px; border-radius: 8px; border: 1px solid var(--jl-line-strong); background: var(--jl-surface); color: var(--jl-ink); cursor: pointer; }
    .btn:hover:not(:disabled) { border-color: var(--jl-blue); }
    .btn:disabled { opacity: .5; cursor: default; }
    .btn.primary { background: var(--jl-blue); border-color: var(--jl-blue); color: #fff; }
    @media (max-width: 560px) { .row { flex-wrap: wrap; } }
  `],
})
export class InvoicePositionsComponent implements OnInit {
  readonly invoiceId = input.required<string>();
  readonly invoiceLabel = input<string>('');

  readonly close = output<void>();
  /** Emitted after any write so the parent can refresh the invoice list (totals changed). */
  readonly changed = output<void>();

  private readonly cases = inject(CasesService);

  protected readonly positions = signal<InvoicePositionItem[]>([]);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly busyId = signal<string | null>(null);
  protected readonly draft = signal<Draft>(emptyDraft());

  protected readonly draftTotal = computed(() => round2(this.draft().units * this.draft().unitPrice));
  protected readonly netTotal = computed(() => round2(this.positions().reduce((acc, p) => acc + (p.total || 0), 0)));

  ngOnInit(): void {
    this.reload();
  }

  private reload(): void {
    this.loading.set(true);
    this.cases.invoicePositions(this.invoiceId()).subscribe((rows) => {
      this.positions.set(rows);
      this.loading.set(false);
    });
  }

  protected toNum(v: unknown): number {
    const n = Number(v);
    return Number.isFinite(n) ? n : 0;
  }

  protected upd<K extends keyof Draft>(key: K, value: Draft[K]): void {
    this.draft.update((cur) => ({ ...cur, [key]: value }));
  }

  protected edit(p: InvoicePositionItem): void {
    this.draft.set({ id: p.id, name: p.name, description: p.description, units: p.units, unitPrice: p.unitPrice, taxRate: p.taxRate });
  }

  protected resetDraft(): void {
    this.draft.set(emptyDraft());
  }

  protected saveDraft(): void {
    const d = this.draft();
    if (!d.name.trim() || this.saving()) {
      return;
    }
    this.saving.set(true);
    const write: InvoicePositionWrite = {
      id: d.id,
      name: d.name.trim(),
      description: d.description.trim(),
      position: d.id ? (this.positions().find((p) => p.id === d.id)?.position ?? 0) : this.nextPosition(),
      taxRate: d.taxRate,
      units: d.units,
      unitPrice: d.unitPrice,
      total: round2(d.units * d.unitPrice),
    };
    const call = d.id
      ? this.cases.updateInvoicePosition(d.id, write)
      : this.cases.createInvoicePosition(this.invoiceId(), write);
    call.subscribe({
      next: () => { this.saving.set(false); this.resetDraft(); this.reload(); this.changed.emit(); },
      error: () => this.saving.set(false),
    });
  }

  protected remove(p: InvoicePositionItem): void {
    if (this.busyId()) {
      return;
    }
    this.busyId.set(p.id);
    this.cases.deleteInvoicePosition(p.id).subscribe({
      next: () => {
        this.busyId.set(null);
        if (this.draft().id === p.id) { this.resetDraft(); }
        this.reload();
        this.changed.emit();
      },
      error: () => this.busyId.set(null),
    });
  }

  private nextPosition(): number {
    return this.positions().reduce((max, p) => Math.max(max, p.position), 0) + 1;
  }
}

function emptyDraft(): Draft {
  return { name: '', description: '', units: 1, unitPrice: 0, taxRate: 19 };
}

function round2(n: number): number {
  return Math.round((n + Number.EPSILON) * 100) / 100;
}
