import { ChangeDetectionStrategy, Component, computed, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { AccountEntry, AccountEntryWrite } from './case.models';

/** A selectable contact ("von / an") — a case party's linked address. */
export interface ContactOption {
  id: string;
  name: string;
}

/** A selectable invoice ("Beleg"). */
export interface InvoiceOption {
  id: string;
  label: string;
}

/**
 * Modal editor for a case account entry ("Buchung" im Aktenkonto). Mirrors the desktop
 * CaseAccountEntryPanel: date, contact ("von/an"), linked invoice ("Beleg"), a comment, and the
 * three amount categories — Einnahmen/Ausgaben, Fremdgeld, Auslagen — each with an in/out value.
 * Emits an {@link AccountEntryWrite}; the parent performs the REST call.
 */
@Component({
  selector: 'jl-account-entry-editor',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, TranslocoModule, IconComponent],
  template: `
    <div class="backdrop" (click)="close.emit()"></div>
    <div class="dialog" role="dialog" aria-modal="true">
      <header class="dh">
        <h2>{{ (entry() ? 'akten.acct.edit' : 'akten.acct.add') | transloco }}</h2>
        <button type="button" class="x" (click)="close.emit()" [attr.aria-label]="'akten.editor.cancel' | transloco">
          <jl-icon name="close" [size]="18" />
        </button>
      </header>

      <div class="db">
        <div class="row">
          <label class="fld">
            <span class="lbl">{{ 'akten.acct.date' | transloco }}</span>
            <input type="date" [ngModel]="date()" (ngModelChange)="date.set($event)" />
          </label>
          <label class="fld">
            <span class="lbl">{{ 'akten.acct.contact' | transloco }}</span>
            <select [ngModel]="contactId()" (ngModelChange)="contactId.set($event)">
              <option value="">—</option>
              @if (contactMissing()) { <option [value]="contactId()">{{ entry()?.contact }}</option> }
              @for (c of contacts(); track c.id) { <option [value]="c.id">{{ c.name }}</option> }
            </select>
          </label>
        </div>

        <label class="fld">
          <span class="lbl">{{ 'akten.acct.invoice' | transloco }}</span>
          <select [ngModel]="invoiceId()" (ngModelChange)="invoiceId.set($event)">
            <option value="">—</option>
            @for (i of invoices(); track i.id) { <option [value]="i.id">{{ i.label }}</option> }
          </select>
        </label>

        <label class="fld">
          <span class="lbl">{{ 'akten.acct.comment' | transloco }}</span>
          <textarea rows="2" [ngModel]="description()" (ngModelChange)="description.set($event)"></textarea>
        </label>

        <div class="amounts">
          @for (g of groups; track g.key) {
            <div class="amt-group">
              <span class="amt-title">{{ 'akten.finance.acct.cat.' + g.key | transloco }}</span>
              <div class="amt-row">
                <label class="amt-fld">
                  <span class="amt-lbl in">{{ 'akten.acct.in' | transloco }}</span>
                  <input type="number" step="0.01" [ngModel]="f()[g.in]" (ngModelChange)="setAmt(g.in, $event)" />
                </label>
                <label class="amt-fld">
                  <span class="amt-lbl out">{{ 'akten.acct.out' | transloco }}</span>
                  <input type="number" step="0.01" [ngModel]="f()[g.out]" (ngModelChange)="setAmt(g.out, $event)" />
                </label>
              </div>
            </div>
          }
        </div>
      </div>

      <footer class="df">
        <span class="net" [class.neg]="net() < 0">{{ 'akten.acct.net' | transloco }}: {{ net().toFixed(2) }} €</span>
        <span class="spacer"></span>
        <button type="button" class="btn" (click)="close.emit()">{{ 'akten.editor.cancel' | transloco }}</button>
        <button type="button" class="btn primary" [disabled]="!canSave()" (click)="submit()">{{ 'akten.editor.save' | transloco }}</button>
      </footer>
    </div>
  `,
  styles: [`
    :host { position: fixed; inset: 0; z-index: 60; display: grid; place-items: center; }
    .backdrop { position: absolute; inset: 0; background: rgba(6, 14, 22, .5); }
    .dialog { position: relative; width: min(560px, 95vw); max-height: 92dvh; display: flex; flex-direction: column;
      background: var(--jl-surface); border: 1px solid var(--jl-line); border-radius: 14px; box-shadow: 0 24px 60px rgba(0,0,0,.32); overflow: hidden; }
    .dh { flex: none; display: flex; align-items: center; gap: 12px; padding: 14px 18px; border-bottom: 1px solid var(--jl-line); }
    .dh h2 { margin: 0; font-size: 1.05rem; font-weight: 800; flex: 1; }
    .x { display: inline-grid; place-items: center; width: 32px; height: 32px; border-radius: 8px; border: 0; background: transparent; color: var(--jl-ink-soft); cursor: pointer; }
    .x:hover { background: var(--jl-surface-alt); }
    .db { flex: 1 1 auto; min-height: 0; overflow-y: auto; padding: 14px 18px; display: flex; flex-direction: column; gap: 12px; }
    .row { display: flex; gap: 12px; }
    .fld { display: flex; flex-direction: column; gap: 5px; flex: 1; min-width: 0; }
    .lbl { font-size: .72rem; font-weight: 700; color: var(--jl-ink-faint); text-transform: uppercase; letter-spacing: .03em; }
    input, select, textarea { font: inherit; font-size: .88rem; padding: 8px 10px; border: 1px solid var(--jl-line-strong); border-radius: 8px; background: var(--jl-surface); color: var(--jl-ink); width: 100%; }
    input:focus, select:focus, textarea:focus { outline: none; border-color: var(--jl-blue); }
    textarea { resize: vertical; }
    .amounts { display: flex; flex-direction: column; gap: 10px; }
    .amt-group { border: 1px solid var(--jl-line); border-radius: 10px; padding: 10px 12px; }
    .amt-title { font-size: .74rem; font-weight: 800; text-transform: uppercase; letter-spacing: .03em; color: var(--jl-ink-soft); }
    .amt-row { display: flex; gap: 12px; margin-top: 8px; }
    .amt-fld { display: flex; flex-direction: column; gap: 4px; flex: 1; }
    .amt-lbl { font-size: .68rem; font-weight: 700; text-transform: uppercase; letter-spacing: .04em; }
    .amt-lbl.in { color: var(--jl-green, #1f9d55); }
    .amt-lbl.out { color: var(--jl-red); }
    .df { flex: none; display: flex; align-items: center; gap: 10px; padding: 12px 18px; border-top: 1px solid var(--jl-line); }
    .net { font-size: .82rem; font-weight: 800; font-variant-numeric: tabular-nums; color: var(--jl-green, #1f9d55); }
    .net.neg { color: var(--jl-red); }
    .spacer { flex: 1; }
    .btn { font: inherit; font-size: .85rem; font-weight: 650; padding: 8px 16px; border-radius: 8px; border: 1px solid var(--jl-line-strong); background: var(--jl-surface); color: var(--jl-ink); cursor: pointer; }
    .btn:hover:not(:disabled) { border-color: var(--jl-blue); }
    .btn:disabled { opacity: .5; cursor: default; }
    .btn.primary { background: var(--jl-blue); border-color: var(--jl-blue); color: #fff; }
    @media (max-width: 520px) { .row, .amt-row { flex-direction: column; } }
  `],
})
export class AccountEntryEditorComponent implements OnInit {
  /** The entry to edit; null to create. For "duplicate" pass a copy with id undefined. */
  readonly entry = input<AccountEntry | null>(null);
  readonly contacts = input<ContactOption[]>([]);
  readonly invoices = input<InvoiceOption[]>([]);

  readonly save = output<AccountEntryWrite>();
  readonly close = output<void>();

  protected readonly groups: { key: string; in: AmountKey; out: AmountKey }[] = [
    { key: 'earnings', in: 'earnings', out: 'spendings' },
    { key: 'escrow', in: 'escrowIn', out: 'escrowOut' },
    { key: 'expenditures', in: 'expendituresIn', out: 'expendituresOut' },
  ];

  protected readonly date = signal('');
  protected readonly contactId = signal('');
  protected readonly invoiceId = signal('');
  protected readonly description = signal('');
  protected readonly f = signal<Record<AmountKey, number>>({
    earnings: 0, spendings: 0, escrowIn: 0, escrowOut: 0, expendituresIn: 0, expendituresOut: 0,
  });

  protected readonly canSave = computed(() => !!this.date());

  /** The saved contact isn't among the selectable parties (e.g. no longer a party) — keep it as an option. */
  protected readonly contactMissing = computed(() => {
    const id = this.contactId();
    return !!id && !this.contacts().some((c) => c.id === id);
  });

  protected readonly net = computed(() => {
    const a = this.f();
    return a.earnings + a.escrowIn + a.expendituresIn - a.spendings - a.escrowOut - a.expendituresOut;
  });

  ngOnInit(): void {
    const e = this.entry();
    if (e) {
      this.date.set(toLocalDate(e.date));
      this.contactId.set(e.contactId);
      this.invoiceId.set(e.invoiceId);
      this.description.set(e.description);
      this.f.set({
        earnings: e.earnings, spendings: e.spendings, escrowIn: e.escrowIn,
        escrowOut: e.escrowOut, expendituresIn: e.expendituresIn, expendituresOut: e.expendituresOut,
      });
    } else {
      this.date.set(toLocalDate(new Date().toISOString()));
    }
  }

  protected setAmt(key: AmountKey, value: unknown): void {
    const n = Number(value);
    this.f.update((cur) => ({ ...cur, [key]: Number.isFinite(n) ? n : 0 }));
  }

  protected submit(): void {
    if (!this.canSave()) {
      return;
    }
    const a = this.f();
    this.save.emit({
      id: this.entry()?.id,
      entryDate: toServerDate(this.date()),
      contactId: this.contactId() || undefined,
      invoiceId: this.invoiceId() || undefined,
      description: this.description().trim(),
      earnings: a.earnings, spendings: a.spendings, escrowIn: a.escrowIn,
      escrowOut: a.escrowOut, expendituresIn: a.expendituresIn, expendituresOut: a.expendituresOut,
    });
  }
}

type AmountKey = 'earnings' | 'spendings' | 'escrowIn' | 'escrowOut' | 'expendituresIn' | 'expendituresOut';

/** ISO timestamp → "yyyy-MM-dd" for a date input (local time); '' when empty/invalid. */
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

/** "yyyy-MM-dd" (local midnight) → an ISO instant the server's JSON-B accepts (UTC 'Z'). */
function toServerDate(local: string): string {
  const d = new Date(`${local}T00:00:00`);
  return isNaN(d.getTime()) ? '' : d.toISOString();
}
