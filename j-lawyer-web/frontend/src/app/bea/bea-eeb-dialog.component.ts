import {
  ChangeDetectionStrategy, Component, computed, inject, input, OnInit, output, signal,
} from '@angular/core';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { BeaService } from './bea.service';
import { BeaListItem, BeaMessage } from './bea.models';

/** Which eEB reply this dialog sends. */
export type EebMode = 'confirm' | 'reject';

/**
 * Modal for replying to an eEB request (elektronisches Empfangsbekenntnis) on a received beA
 * message: confirm ("abgeben") with an acceptance date, or reject ("zurückweisen") with a reason and
 * an optional comment. The confirming postbox is `safeId`; the eEB is addressed back to the original
 * sender (`recipientSafeId`). If the incoming message has no eEB id the reply cannot be sent from
 * here and the user is pointed to the beA web portal — matching the desktop client.
 */
@Component({
  selector: 'jl-bea-eeb-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent],
  template: `
    <div class="eeb-backdrop" (click)="tryClose()"></div>
    <div class="eeb-dialog" role="dialog" aria-modal="true">
      <header class="eeb-head">
        <h2>{{ (mode() === 'confirm' ? 'beaEeb.titleConfirm' : 'beaEeb.titleReject') | transloco }}</h2>
        <button type="button" class="icon-btn" (click)="tryClose()" [attr.aria-label]="'beaEeb.close' | transloco">
          <jl-icon name="close" [size]="18" />
        </button>
      </header>

      <div class="eeb-body">
        @if (!eebId()) {
          <p class="eeb-warn">{{ 'beaEeb.noEebId' | transloco }}</p>
        } @else if (mode() === 'confirm') {
          <p class="eeb-info">{{ 'beaEeb.confirmInfo' | transloco }}</p>
          <label class="eeb-field">
            <span>{{ 'beaEeb.abgabeDate' | transloco }}</span>
            <input type="date" [value]="date()" [min]="minDate()" [max]="maxDate()"
                   (input)="date.set($any($event.target).value)" />
          </label>
        } @else {
          <p class="eeb-info">{{ 'beaEeb.rejectInfo' | transloco }}</p>
          <label class="eeb-field">
            <span>{{ 'beaEeb.reason' | transloco }}</span>
            <select [value]="reasonCode()" (change)="reasonCode.set($any($event.target).value)">
              <option value="">{{ 'beaEeb.reasonPlaceholder' | transloco }}</option>
              @for (r of reasons(); track r.code) { <option [value]="r.code">{{ r.name }}</option> }
            </select>
          </label>
          <label class="eeb-field">
            <span>{{ 'beaEeb.comment' | transloco }}</span>
            <textarea [value]="comment()" (input)="comment.set($any($event.target).value)"
                      [placeholder]="'beaEeb.commentPlaceholder' | transloco"></textarea>
          </label>
        }

        @if (error()) { <p class="eeb-error">{{ error() }}</p> }
      </div>

      <footer class="eeb-foot">
        <button type="button" class="btn-ghost" [disabled]="sending()" (click)="tryClose()">
          {{ 'beaEeb.cancel' | transloco }}
        </button>
        <button type="button" class="btn-primary" [disabled]="sending() || !canSubmit()" (click)="submit()">
          <jl-icon [name]="mode() === 'confirm' ? 'check' : 'close'" [size]="15" />
          <span>{{ (sending() ? 'beaEeb.sending'
                  : (mode() === 'confirm' ? 'beaEeb.confirm' : 'beaEeb.reject')) | transloco }}</span>
        </button>
      </footer>
    </div>
  `,
  styles: [`
    :host { display: contents; }
    .eeb-backdrop { position: fixed; inset: 0; background: rgba(4, 12, 20, .45); z-index: 42; }
    .eeb-dialog {
      position: fixed; z-index: 43; left: 50%; top: 50%; transform: translate(-50%, -50%);
      width: min(460px, calc(100vw - 32px)); max-height: calc(100vh - 48px); display: flex; flex-direction: column;
      background: var(--jl-surface); color: var(--jl-ink); border: 1px solid var(--jl-line-strong);
      border-radius: 14px; box-shadow: 0 24px 60px rgba(4, 12, 20, .4);
    }
    .eeb-head { display: flex; align-items: center; justify-content: space-between; gap: 8px; padding: 14px 16px; border-bottom: 1px solid var(--jl-line); }
    .eeb-head h2 { margin: 0; font-size: 1rem; font-weight: 700; }
    .eeb-body { flex: 1 1 auto; overflow-y: auto; padding: 14px 16px; display: flex; flex-direction: column; gap: 12px; }
    .eeb-info { margin: 0; font-size: .86rem; color: var(--jl-ink-soft); }
    .eeb-warn { margin: 0; font-size: .86rem; color: #b26a00; }
    .eeb-field { display: flex; flex-direction: column; gap: 5px; font-size: .82rem; color: var(--jl-ink-faint); }
    .eeb-field input, .eeb-field select, .eeb-field textarea {
      font: inherit; font-size: .88rem; color: var(--jl-ink); padding: 8px 10px; border: 1px solid var(--jl-line-strong);
      border-radius: 8px; background: var(--jl-surface); outline: none;
    }
    .eeb-field input:focus, .eeb-field select:focus, .eeb-field textarea:focus { border-color: var(--jl-blue); }
    .eeb-field textarea { min-height: 90px; resize: vertical; }
    .eeb-error { margin: 0; color: var(--jl-red); font-size: .84rem; }
    .eeb-foot { display: flex; justify-content: flex-end; gap: 8px; padding: 12px 16px; border-top: 1px solid var(--jl-line); }
    .btn-ghost { font: inherit; font-size: .86rem; padding: 8px 14px; border: 1px solid var(--jl-line-strong); border-radius: 8px; background: var(--jl-surface); color: var(--jl-ink); cursor: pointer; }
    .btn-ghost:hover:not(:disabled) { background: var(--jl-surface-alt); }
    .btn-primary { display: inline-flex; align-items: center; gap: 7px; font: inherit; font-size: .86rem; font-weight: 600; padding: 8px 16px; border: 0; border-radius: 8px; background: var(--jl-blue); color: #fff; cursor: pointer; }
    .btn-primary:hover:not(:disabled) { filter: brightness(1.06); }
    .btn-primary:disabled, .btn-ghost:disabled { opacity: .55; cursor: default; }
  `],
})
export class BeaEebDialogComponent implements OnInit {
  private readonly api = inject(BeaService);
  private readonly transloco = inject(TranslocoService);

  readonly safeId = input.required<string>();
  readonly messageId = input.required<string>();
  readonly recipientSafeId = input.required<string>();
  readonly eebId = input.required<string>();
  readonly mode = input.required<EebMode>();

  /** Emitted with the sent eEB reply message after it was sent successfully. */
  readonly done = output<BeaMessage>();
  readonly closed = output<void>();

  protected readonly date = signal(today());
  protected readonly reasonCode = signal('');
  protected readonly comment = signal('');
  protected readonly reasons = signal<BeaListItem[]>([]);
  protected readonly sending = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly maxDate = computed(() => today());
  protected readonly minDate = computed(() => addDays(today(), -62));

  protected readonly canSubmit = computed(() => {
    if (!this.eebId()) { return false; }
    return this.mode() === 'confirm' ? !!this.date() : !!this.reasonCode();
  });

  ngOnInit(): void {
    if (this.mode() === 'reject') {
      this.api.eebRejectionReasons().subscribe((r) => this.reasons.set(r));
    }
  }

  protected submit(): void {
    if (this.sending() || !this.canSubmit()) { return; }
    this.error.set(null);
    this.sending.set(true);
    const done = (sent: BeaMessage) => { this.sending.set(false); this.done.emit(sent); };
    const fail = () => { this.sending.set(false); this.error.set(this.transloco.translate('beaEeb.error')); };

    if (this.mode() === 'confirm') {
      const iso = `${this.date()}T12:00:00Z`;
      this.api.sendEebConfirmation(this.safeId(), this.messageId(), this.recipientSafeId(), iso)
        .subscribe({ next: done, error: fail });
    } else {
      this.api.sendEebRejection(this.safeId(), this.messageId(), this.recipientSafeId(), this.reasonCode(), this.comment())
        .subscribe({ next: done, error: fail });
    }
  }

  protected tryClose(): void {
    if (this.sending()) { return; }
    this.closed.emit();
  }
}

// ---------- module-scope helpers ----------

/** Today as yyyy-MM-dd (local). */
function today(): string {
  return toDateStr(new Date());
}

function addDays(dateStr: string, days: number): string {
  const d = new Date(dateStr + 'T00:00:00');
  d.setDate(d.getDate() + days);
  return toDateStr(d);
}

function toDateStr(d: Date): string {
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${m}-${day}`;
}
