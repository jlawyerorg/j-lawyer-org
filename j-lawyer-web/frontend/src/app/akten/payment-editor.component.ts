import { ChangeDetectionStrategy, Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { CasePayment, CaseUserRef, PaymentWrite } from './case.models';
import { ContactOption } from './account-entry-editor.component';
import { CasesService } from './cases.service';

/** The payment statuses (raw German values the server maps to its status codes). */
const PAYMENT_STATUSES = ['Entwurf', 'freigegeben', 'veranlasst', 'ausgeführt', 'fehlgeschlagen', 'storniert'];
const PAYMENT_TYPES = ['SEPATRANSFER', 'OTHER'];

/**
 * Modal editor for a payment ("Zahlung"), mirroring the desktop PaymentDialog: name, purpose/reason,
 * recipient, paying user ("Absender"), status, target date, payment type, amount and currency. The
 * payment number is generated server-side and never edited here. Emits a {@link PaymentWrite}; the
 * parent performs the REST call.
 */
@Component({
  selector: 'jl-payment-editor',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, TranslocoModule, IconComponent],
  template: `
    <div class="backdrop" (click)="close.emit()"></div>
    <div class="dialog" role="dialog" aria-modal="true">
      <header class="dh">
        <h2>{{ (entry() ? 'akten.pay.edit' : 'akten.pay.add') | transloco }}</h2>
        @if (entry()?.paymentNumber) { <span class="pay-no">{{ entry()?.paymentNumber }}</span> }
        <button type="button" class="x" (click)="close.emit()" [attr.aria-label]="'akten.editor.cancel' | transloco">
          <jl-icon name="close" [size]="18" />
        </button>
      </header>

      <div class="db">
        <label class="fld">
          <span class="lbl">{{ 'akten.pay.name' | transloco }}</span>
          <input type="text" [ngModel]="name()" (ngModelChange)="name.set($event)" />
        </label>

        <label class="fld">
          <span class="lbl">{{ 'akten.pay.reason' | transloco }}</span>
          <input type="text" [ngModel]="reason()" (ngModelChange)="reason.set($event)" />
        </label>

        <div class="row">
          <label class="fld">
            <span class="lbl">{{ 'akten.pay.recipient' | transloco }}</span>
            <select [ngModel]="contactId()" (ngModelChange)="contactId.set($event)">
              <option value="">—</option>
              @if (contactMissing()) { <option [value]="contactId()">{{ 'akten.pay.linkedContact' | transloco }}</option> }
              @for (c of contacts(); track c.id) { <option [value]="c.id">{{ c.name }}</option> }
            </select>
          </label>
          <label class="fld">
            <span class="lbl">{{ 'akten.pay.sender' | transloco }}</span>
            <select [ngModel]="sender()" (ngModelChange)="sender.set($event)">
              <option value="">—</option>
              @if (senderMissing()) { <option [value]="sender()">{{ sender() }}</option> }
              @for (u of users(); track u.principalId) { <option [value]="u.principalId">{{ u.displayName }}</option> }
            </select>
          </label>
        </div>

        <div class="row">
          <label class="fld">
            <span class="lbl">{{ 'akten.pay.status' | transloco }}</span>
            <select [ngModel]="status()" (ngModelChange)="status.set($event)">
              @for (s of statuses; track s) { <option [value]="s">{{ s }}</option> }
            </select>
          </label>
          <label class="fld">
            <span class="lbl">{{ 'akten.pay.target' | transloco }}</span>
            <input type="date" [ngModel]="target()" (ngModelChange)="target.set($event)" />
          </label>
        </div>

        <div class="row">
          <label class="fld">
            <span class="lbl">{{ 'akten.pay.type' | transloco }}</span>
            <select [ngModel]="paymentType()" (ngModelChange)="paymentType.set($event)">
              @for (p of paymentTypes; track p) { <option [value]="p">{{ 'akten.pay.pay.' + p | transloco }}</option> }
            </select>
          </label>
          <label class="fld">
            <span class="lbl">{{ 'akten.pay.amount' | transloco }}</span>
            <input type="number" step="0.01" [ngModel]="total()" (ngModelChange)="total.set($event)" />
          </label>
          <label class="fld cur">
            <span class="lbl">{{ 'akten.pay.currency' | transloco }}</span>
            <input type="text" maxlength="3" [ngModel]="currency()" (ngModelChange)="currency.set($event)" />
          </label>
        </div>

        <label class="fld">
          <span class="lbl">{{ 'akten.pay.description' | transloco }}</span>
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
    .pay-no { font-size: .78rem; font-weight: 700; color: var(--jl-blue); background: color-mix(in srgb, var(--jl-blue) 12%, transparent); padding: 2px 9px; border-radius: 9px; }
    .x { margin-left: auto; display: inline-grid; place-items: center; width: 32px; height: 32px; border-radius: 8px; border: 0; background: transparent; color: var(--jl-ink-soft); cursor: pointer; }
    .x:hover { background: var(--jl-surface-alt); }
    .db { flex: 1 1 auto; min-height: 0; overflow-y: auto; padding: 14px 18px; display: flex; flex-direction: column; gap: 12px; }
    .row { display: flex; gap: 12px; }
    .fld { display: flex; flex-direction: column; gap: 5px; flex: 1; min-width: 0; }
    .fld.cur { flex: 0 0 84px; }
    .lbl { font-size: .72rem; font-weight: 700; color: var(--jl-ink-faint); text-transform: uppercase; letter-spacing: .03em; }
    input, select, textarea { font: inherit; font-size: .88rem; padding: 8px 10px; border: 1px solid var(--jl-line-strong); border-radius: 8px; background: var(--jl-surface); color: var(--jl-ink); width: 100%; }
    input:focus, select:focus, textarea:focus { outline: none; border-color: var(--jl-blue); }
    textarea { resize: vertical; }
    .df { flex: none; display: flex; align-items: center; gap: 10px; padding: 12px 18px; border-top: 1px solid var(--jl-line); }
    .spacer { flex: 1; }
    .btn { font: inherit; font-size: .85rem; font-weight: 650; padding: 8px 16px; border-radius: 8px; border: 1px solid var(--jl-line-strong); background: var(--jl-surface); color: var(--jl-ink); cursor: pointer; }
    .btn:hover:not(:disabled) { border-color: var(--jl-blue); }
    .btn:disabled { opacity: .5; cursor: default; }
    .btn.primary { background: var(--jl-blue); border-color: var(--jl-blue); color: #fff; }
    @media (max-width: 520px) { .row { flex-direction: column; } .fld.cur { flex: 1; } }
  `],
})
export class PaymentEditorComponent implements OnInit {
  readonly entry = input<CasePayment | null>(null);
  readonly contacts = input<ContactOption[]>([]);
  readonly caseId = input.required<string>();
  /** Login id of the currently signed-in user, pre-selected as sender on a new payment. */
  readonly currentUser = input<string>('');

  readonly save = output<PaymentWrite>();
  readonly close = output<void>();

  private readonly cases = inject(CasesService);
  /** Login-enabled users for the "Absender" dropdown (loaded from the cached service list). */
  protected readonly users = signal<CaseUserRef[]>([]);

  protected readonly statuses = PAYMENT_STATUSES;
  protected readonly paymentTypes = PAYMENT_TYPES;

  protected readonly name = signal('');
  protected readonly reason = signal('');
  protected readonly contactId = signal('');
  protected readonly sender = signal('');
  protected readonly status = signal('Entwurf');
  protected readonly target = signal('');
  protected readonly paymentType = signal('SEPATRANSFER');
  protected readonly total = signal(0);
  protected readonly currency = signal('EUR');
  protected readonly description = signal('');

  protected readonly canSave = computed(() =>
    !!this.paymentType() && !!this.currency().trim() && Number(this.total()) > 0);

  protected readonly contactMissing = computed(() => {
    const id = this.contactId();
    return !!id && !this.contacts().some((c) => c.id === id);
  });

  protected readonly senderMissing = computed(() => {
    const s = this.sender();
    return !!s && !this.users().some((u) => u.principalId === s);
  });

  ngOnInit(): void {
    this.cases.users().subscribe((u) => this.users.set(u));
    const e = this.entry();
    if (e) {
      this.name.set(e.name);
      this.reason.set(e.reason);
      this.contactId.set(e.contactId);
      this.sender.set(e.sender);
      this.status.set(e.status || 'Entwurf');
      this.target.set(toLocalDate(e.targetDate));
      this.paymentType.set(e.paymentType || 'SEPATRANSFER');
      this.total.set(e.total);
      this.currency.set(e.currency || 'EUR');
      this.description.set(e.description);
    } else {
      this.sender.set(this.currentUser());
      this.target.set(toLocalDate(new Date().toISOString()));
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
      reason: this.reason().trim(),
      status: this.status(),
      paymentType: this.paymentType(),
      contactId: this.contactId() || undefined,
      sender: this.sender() || undefined,
      total: Number(this.total()),
      currency: this.currency().trim(),
      targetDate: toServerDate(this.target()),
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

/** "yyyy-MM-dd" → an ISO instant the server's JSON-B accepts (UTC 'Z'); undefined when empty. */
function toServerDate(local: string): string | undefined {
  if (!local) {
    return undefined;
  }
  const d = new Date(`${local}T00:00:00`);
  return isNaN(d.getTime()) ? undefined : d.toISOString();
}
