import { ChangeDetectionStrategy, Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { CasesService } from './cases.service';
import { Party, PartyTypeOption, PartyUpdate } from './case.models';

/**
 * Modal editor for an existing party's detail fields (mirrors the desktop InvolvedPartyEntryPanel):
 * involvement type, reference ("Zeichen"), contact person ("Ansprechpartner") and the three custom
 * fields. The linked address is shown read-only. Emits the update payload on save — the parent does
 * the REST call. Rendered only while open (mounted via @if).
 */
@Component({
  selector: 'jl-party-editor',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, TranslocoModule, IconComponent],
  template: `
    <div class="backdrop" (click)="close.emit()"></div>
    <div class="dialog" role="dialog" aria-modal="true">
      <header class="dh">
        <h2>{{ 'akten.party.editTitle' | transloco }}</h2>
        <button type="button" class="x" (click)="close.emit()" [attr.aria-label]="'akten.editor.cancel' | transloco">
          <jl-icon name="close" [size]="18" />
        </button>
      </header>

      <div class="db">
        <div class="linked">
          <jl-icon name="contacts" [size]="16" />
          <span>{{ party().contactName || party().contact || '—' }}</span>
        </div>

        <label class="fld">
          <span class="lbl">{{ 'akten.party.involvementType' | transloco }}</span>
          <select [ngModel]="f().involvementType" (ngModelChange)="upd('involvementType', $event)">
            <option value="">—</option>
            @for (t of typeOptions(); track t.name) { <option [value]="t.name">{{ t.name }}</option> }
          </select>
        </label>

        <label class="fld">
          <span class="lbl">{{ 'akten.party.reference' | transloco }}</span>
          <input type="text" [ngModel]="f().reference" (ngModelChange)="upd('reference', $event)" />
        </label>

        <label class="fld">
          <span class="lbl">{{ 'akten.party.contactPerson' | transloco }}</span>
          <input type="text" [ngModel]="f().contact" (ngModelChange)="upd('contact', $event)" />
        </label>

        <div class="row">
          <label class="fld">
            <span class="lbl">{{ 'akten.party.custom1' | transloco }}</span>
            <input type="text" [ngModel]="f().custom1" (ngModelChange)="upd('custom1', $event)" />
          </label>
          <label class="fld">
            <span class="lbl">{{ 'akten.party.custom2' | transloco }}</span>
            <input type="text" [ngModel]="f().custom2" (ngModelChange)="upd('custom2', $event)" />
          </label>
          <label class="fld">
            <span class="lbl">{{ 'akten.party.custom3' | transloco }}</span>
            <input type="text" [ngModel]="f().custom3" (ngModelChange)="upd('custom3', $event)" />
          </label>
        </div>
      </div>

      <footer class="df">
        <span class="spacer"></span>
        <button type="button" class="btn" (click)="close.emit()">{{ 'akten.editor.cancel' | transloco }}</button>
        <button type="button" class="btn primary" (click)="submit()">{{ 'akten.editor.save' | transloco }}</button>
      </footer>
    </div>
  `,
  styles: [`
    :host { position: fixed; inset: 0; z-index: 60; display: grid; place-items: center; }
    .backdrop { position: absolute; inset: 0; background: rgba(6, 14, 22, .5); }
    .dialog {
      position: relative; width: min(560px, 95vw); max-height: 92dvh; display: flex; flex-direction: column;
      background: var(--jl-surface); border: 1px solid var(--jl-line); border-radius: 14px;
      box-shadow: 0 24px 60px rgba(0,0,0,.32); overflow: hidden;
    }
    .dh { flex: none; display: flex; align-items: center; gap: 12px; padding: 14px 18px; border-bottom: 1px solid var(--jl-line); }
    .dh h2 { margin: 0; font-size: 1.05rem; font-weight: 800; flex: 1; }
    .x { display: inline-grid; place-items: center; width: 32px; height: 32px; border-radius: 8px; border: 0; background: transparent; color: var(--jl-ink-soft); cursor: pointer; }
    .x:hover { background: var(--jl-surface-alt); }
    .db { flex: 1 1 auto; min-height: 0; overflow-y: auto; padding: 14px 18px; display: flex; flex-direction: column; gap: 12px; }
    .linked { display: flex; align-items: center; gap: 8px; font-size: .9rem; font-weight: 700; color: var(--jl-ink);
      background: var(--jl-surface-alt); border: 1px solid var(--jl-line); border-radius: 8px; padding: 8px 11px; }
    .row { display: flex; gap: 12px; }
    .fld { display: flex; flex-direction: column; gap: 5px; flex: 1; min-width: 0; }
    .lbl { font-size: .72rem; font-weight: 700; color: var(--jl-ink-faint); text-transform: uppercase; letter-spacing: .03em; }
    input, select { font: inherit; font-size: .88rem; padding: 8px 10px; border: 1px solid var(--jl-line-strong); border-radius: 8px; background: var(--jl-surface); color: var(--jl-ink); width: 100%; }
    input:focus, select:focus { outline: none; border-color: var(--jl-blue); }
    .df { flex: none; display: flex; align-items: center; gap: 10px; padding: 12px 18px; border-top: 1px solid var(--jl-line); }
    .spacer { flex: 1; }
    .btn { font: inherit; font-size: .85rem; font-weight: 650; padding: 8px 16px; border-radius: 8px; border: 1px solid var(--jl-line-strong); background: var(--jl-surface); color: var(--jl-ink); cursor: pointer; }
    .btn:hover { border-color: var(--jl-blue); }
    .btn.primary { background: var(--jl-blue); border-color: var(--jl-blue); color: #fff; }
    @media (max-width: 520px) { .row { flex-direction: column; } }
  `],
})
export class PartyEditorComponent implements OnInit {
  /** The party to edit (from the case detail). */
  readonly party = input.required<Party>();
  /** The owning case id (needed in the update payload). */
  readonly caseId = input.required<string>();

  readonly save = output<PartyUpdate>();
  readonly close = output<void>();

  private readonly cases = inject(CasesService);

  protected readonly partyTypes = signal<PartyTypeOption[]>([]);
  /** Working copy of the editable fields. */
  protected readonly f = signal<PartyUpdate>({
    id: '', caseId: '', addressId: '', involvementType: '', reference: '', contact: '',
    custom1: '', custom2: '', custom3: '',
  });

  ngOnInit(): void {
    const p = this.party();
    this.f.set({
      id: p.id,
      caseId: this.caseId(),
      addressId: p.addressId,
      involvementType: p.involvementType,
      reference: p.reference,
      // `contact` here is the free-text Ansprechpartner, not the resolved address name.
      contact: p.contactPerson,
      custom1: p.custom1,
      custom2: p.custom2,
      custom3: p.custom3,
    });
    this.cases.partyTypes().subscribe((types) => this.partyTypes.set(types));
  }

  /**
   * Involvement-type options — the configured types, plus the party's current type if it is no
   * longer among them (so editing never drops the value).
   */
  protected readonly typeOptions = computed(() => {
    const types = this.partyTypes();
    const current = this.f().involvementType;
    if (current && !types.some((t) => t.name === current)) {
      return [{ name: current, color: '', placeHolder: false }, ...types];
    }
    return types;
  });

  protected upd<K extends keyof PartyUpdate>(key: K, value: PartyUpdate[K]): void {
    this.f.update((cur) => ({ ...cur, [key]: value }));
  }

  protected submit(): void {
    this.save.emit(this.f());
  }
}
