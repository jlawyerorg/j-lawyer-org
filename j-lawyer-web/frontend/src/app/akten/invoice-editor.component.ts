import { ChangeDetectionStrategy, Component, computed, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { CaseInvoice, InvoicePool, InvoiceType, InvoiceWrite } from './case.models';
import { ContactOption } from './account-entry-editor.component';

/** The invoice statuses (raw values sent to the server, which maps them to its status codes). */
const INVOICE_STATUSES = [
  'Entwurf', 'offen', 'offen - 1. Mahnstufe', 'offen - 2. Mahnstufe',
  'offen - 3. Mahnstufe', 'offen - nicht vollstreckbar', 'bezahlt', 'storniert',
];
const PAYMENT_TYPES = ['BANKTRANSFER', 'DIRECTDEBIT', 'OTHER'];

/**
 * Modal editor for an invoice's master data ("Beleg"-Stammdaten), mirroring the desktop InvoiceDialog
 * (without the plugin-based auto-fill): name, type, number-range pool, recipient, dates, currency,
 * small-business flag, payment type and status. Line items are managed in a separate positions
 * dialog. Emits an {@link InvoiceWrite}; the parent performs the REST call.
 */
@Component({
  selector: 'jl-invoice-editor',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, TranslocoModule, IconComponent],
  template: `
    <div class="backdrop" (click)="close.emit()"></div>
    <div class="dialog" role="dialog" aria-modal="true">
      <header class="dh">
        <h2>{{ (entry() ? 'akten.inv.edit' : 'akten.inv.add') | transloco }}</h2>
        @if (entry()?.invoiceNumber) { <span class="inv-no">{{ entry()?.invoiceNumber }}</span> }
        <button type="button" class="x" (click)="close.emit()" [attr.aria-label]="'akten.editor.cancel' | transloco">
          <jl-icon name="close" [size]="18" />
        </button>
      </header>

      <div class="db">
        <label class="fld">
          <span class="lbl">{{ 'akten.inv.name' | transloco }}</span>
          <input type="text" [ngModel]="name()" (ngModelChange)="name.set($event)" />
        </label>

        <div class="row">
          <label class="fld">
            <span class="lbl">{{ 'akten.inv.type' | transloco }}</span>
            <select [ngModel]="invoiceType()" (ngModelChange)="invoiceType.set($event)">
              <option value="">—</option>
              @for (t of types(); track t.id) { <option [value]="t.id">{{ t.displayName }}</option> }
            </select>
          </label>
          <label class="fld">
            <span class="lbl">{{ 'akten.inv.pool' | transloco }}</span>
            <select [ngModel]="poolId()" (ngModelChange)="onPoolChange($event)">
              <option value="">—</option>
              @for (p of pools(); track p.id) { <option [value]="p.id">{{ p.displayName }}</option> }
            </select>
          </label>
        </div>

        <div class="row">
          <label class="fld">
            <span class="lbl">{{ 'akten.inv.recipient' | transloco }}</span>
            <select [ngModel]="contactId()" (ngModelChange)="contactId.set($event)">
              <option value="">—</option>
              @if (contactMissing()) { <option [value]="contactId()">{{ 'akten.inv.linkedContact' | transloco }}</option> }
              @for (c of contacts(); track c.id) { <option [value]="c.id">{{ c.name }}</option> }
            </select>
          </label>
          <label class="fld">
            <span class="lbl">{{ 'akten.inv.status' | transloco }}</span>
            <select [ngModel]="status()" (ngModelChange)="status.set($event)">
              @for (s of statuses; track s) { <option [value]="s">{{ s }}</option> }
            </select>
          </label>
        </div>

        <div class="row">
          <label class="fld">
            <span class="lbl">{{ 'akten.inv.created' | transloco }}</span>
            <input type="date" [ngModel]="created()" (ngModelChange)="created.set($event)" />
          </label>
          <label class="fld">
            <span class="lbl">{{ 'akten.inv.due' | transloco }}</span>
            <input type="date" [ngModel]="due()" (ngModelChange)="due.set($event)" />
          </label>
        </div>

        <div class="row">
          <label class="fld">
            <span class="lbl">{{ 'akten.inv.payment' | transloco }}</span>
            <select [ngModel]="paymentType()" (ngModelChange)="paymentType.set($event)">
              @for (p of paymentTypes; track p) { <option [value]="p">{{ 'akten.inv.pay.' + p | transloco }}</option> }
            </select>
          </label>
          <label class="fld">
            <span class="lbl">{{ 'akten.inv.currency' | transloco }}</span>
            <input type="text" maxlength="3" [ngModel]="currency()" (ngModelChange)="currency.set($event)" />
          </label>
        </div>

        <label class="chk">
          <input type="checkbox" [ngModel]="smallBusiness()" (ngModelChange)="smallBusiness.set($event)" />
          {{ 'akten.inv.smallBusiness' | transloco }}
        </label>

        <label class="fld">
          <span class="lbl">{{ 'akten.inv.description' | transloco }}</span>
          <textarea rows="2" [ngModel]="description()" (ngModelChange)="description.set($event)"></textarea>
        </label>
      </div>

      <footer class="df">
        <span class="spacer"></span>
        <button type="button" class="btn" (click)="close.emit()">{{ 'akten.editor.cancel' | transloco }}</button>
        <button type="button" class="btn primary" [disabled]="!canSave()" (click)="submit()">{{ 'akten.editor.save' | transloco }}</button>
      </footer>
    </div>
  `,
  styles: [`
    :host { position: fixed; inset: 0; z-index: 60; display: grid; place-items: center; }
    .backdrop { position: absolute; inset: 0; background: rgba(6, 14, 22, .5); }
    .dialog { position: relative; width: min(600px, 95vw); max-height: 92dvh; display: flex; flex-direction: column;
      background: var(--jl-surface); border: 1px solid var(--jl-line); border-radius: 14px; box-shadow: 0 24px 60px rgba(0,0,0,.32); overflow: hidden; }
    .dh { flex: none; display: flex; align-items: center; gap: 10px; padding: 14px 18px; border-bottom: 1px solid var(--jl-line); }
    .dh h2 { margin: 0; font-size: 1.05rem; font-weight: 800; }
    .inv-no { font-size: .78rem; font-weight: 700; color: var(--jl-blue); background: color-mix(in srgb, var(--jl-blue) 12%, transparent); padding: 2px 9px; border-radius: 9px; }
    .x { margin-left: auto; display: inline-grid; place-items: center; width: 32px; height: 32px; border-radius: 8px; border: 0; background: transparent; color: var(--jl-ink-soft); cursor: pointer; }
    .x:hover { background: var(--jl-surface-alt); }
    .db { flex: 1 1 auto; min-height: 0; overflow-y: auto; padding: 14px 18px; display: flex; flex-direction: column; gap: 12px; }
    .row { display: flex; gap: 12px; }
    .fld { display: flex; flex-direction: column; gap: 5px; flex: 1; min-width: 0; }
    .lbl { font-size: .72rem; font-weight: 700; color: var(--jl-ink-faint); text-transform: uppercase; letter-spacing: .03em; }
    input, select, textarea { font: inherit; font-size: .88rem; padding: 8px 10px; border: 1px solid var(--jl-line-strong); border-radius: 8px; background: var(--jl-surface); color: var(--jl-ink); width: 100%; }
    input:focus, select:focus, textarea:focus { outline: none; border-color: var(--jl-blue); }
    textarea { resize: vertical; }
    .chk { display: flex; align-items: center; gap: 8px; font-size: .86rem; font-weight: 600; }
    .chk input { width: auto; }
    .df { flex: none; display: flex; align-items: center; gap: 10px; padding: 12px 18px; border-top: 1px solid var(--jl-line); }
    .spacer { flex: 1; }
    .btn { font: inherit; font-size: .85rem; font-weight: 650; padding: 8px 16px; border-radius: 8px; border: 1px solid var(--jl-line-strong); background: var(--jl-surface); color: var(--jl-ink); cursor: pointer; }
    .btn:hover:not(:disabled) { border-color: var(--jl-blue); }
    .btn:disabled { opacity: .5; cursor: default; }
    .btn.primary { background: var(--jl-blue); border-color: var(--jl-blue); color: #fff; }
    @media (max-width: 520px) { .row { flex-direction: column; } }
  `],
})
export class InvoiceEditorComponent implements OnInit {
  readonly entry = input<CaseInvoice | null>(null);
  readonly types = input<InvoiceType[]>([]);
  readonly pools = input<InvoicePool[]>([]);
  readonly contacts = input<ContactOption[]>([]);
  readonly caseId = input.required<string>();

  readonly save = output<InvoiceWrite>();
  readonly close = output<void>();

  protected readonly statuses = INVOICE_STATUSES;
  protected readonly paymentTypes = PAYMENT_TYPES;

  protected readonly name = signal('');
  protected readonly invoiceType = signal('');
  protected readonly poolId = signal('');
  protected readonly contactId = signal('');
  protected readonly status = signal('Entwurf');
  protected readonly created = signal('');
  protected readonly due = signal('');
  protected readonly paymentType = signal('BANKTRANSFER');
  protected readonly currency = signal('EUR');
  protected readonly smallBusiness = signal(false);
  protected readonly description = signal('');

  protected readonly canSave = computed(() => !!this.invoiceType() && !!this.poolId() && !!this.paymentType() && !!this.currency().trim());

  protected readonly contactMissing = computed(() => {
    const id = this.contactId();
    return !!id && !this.contacts().some((c) => c.id === id);
  });

  ngOnInit(): void {
    const e = this.entry();
    if (e) {
      this.name.set(e.name);
      this.invoiceType.set(e.invoiceTypeId);
      this.poolId.set(e.lastPoolId);
      this.contactId.set(e.contactId);
      this.status.set(e.status || 'Entwurf');
      this.created.set(toLocalDate(e.creationDate));
      this.due.set(toLocalDate(e.dueDate));
      this.paymentType.set(e.paymentType || 'BANKTRANSFER');
      this.currency.set(e.currency || 'EUR');
      this.smallBusiness.set(e.smallBusiness);
      this.description.set(e.description);
    } else {
      this.created.set(toLocalDate(new Date().toISOString()));
    }
  }

  /** When the pool changes on a new invoice, adopt its small-business default + payment term. */
  protected onPoolChange(id: string): void {
    this.poolId.set(id);
    if (this.entry()) {
      return;
    }
    const pool = this.pools().find((p) => p.id === id);
    if (pool) {
      this.smallBusiness.set(pool.smallBusiness);
      const d = new Date();
      d.setDate(d.getDate() + (pool.paymentTerm || 14));
      this.due.set(toLocalDate(d.toISOString()));
    }
  }

  protected submit(): void {
    if (!this.canSave()) {
      return;
    }
    const e = this.entry();
    this.save.emit({
      id: e?.id,
      caseId: this.caseId(),
      name: this.name().trim(),
      description: this.description().trim(),
      invoiceType: this.invoiceType(),
      lastPoolId: this.poolId(),
      contactId: this.contactId() || undefined,
      sender: e?.sender || undefined,
      status: this.status(),
      paymentType: this.paymentType(),
      smallBusiness: this.smallBusiness(),
      currency: this.currency().trim(),
      creationDate: toServerDate(this.created()),
      dueDate: toServerDate(this.due()),
      total: e?.total,
      totalGross: e?.totalGross,
    });
  }
}

/** ISO timestamp → "yyyy-MM-dd" for a date input (local); '' when empty/invalid. */
function toLocalDate(iso: string): string {
  if (!iso) {
    return '';
  }
  const d = new Date(iso);
  if (isNaN(d.getTime())) {
    return '';
  }
  const p = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
}

/** "yyyy-MM-dd" → an ISO instant the server's JSON-B accepts (UTC 'Z'); '' when empty. */
function toServerDate(local: string): string {
  if (!local) {
    return '';
  }
  const d = new Date(`${local}T00:00:00`);
  return isNaN(d.getTime()) ? '' : d.toISOString();
}
