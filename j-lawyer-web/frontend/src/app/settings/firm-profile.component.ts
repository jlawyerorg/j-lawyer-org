import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { TranslocoModule } from '@jsverse/transloco';
import { AuthService } from '../core/auth/auth.service';
import { FirmProfile, FirmProfileService } from './firm-profile.service';

interface FieldDef { key: keyof FirmProfile; labelKey: string; }
interface FieldGroup { titleKey: string; fields: FieldDef[]; }

const EMPTY: FirmProfile = {
  companyName: '', street: '', street2: '', zipCode: '', city: '', country: '',
  phone: '', fax: '', mobile: '', email: '', website: '',
  taxId: '', vatId: '', bank: '', bic: '', iban: '',
  escrowBank: '', escrowBic: '', escrowIban: '',
};

const GROUPS: FieldGroup[] = [
  { titleKey: 'settings.firm.company', fields: [
    { key: 'companyName', labelKey: 'settings.firm.field.companyName' },
    { key: 'street', labelKey: 'settings.firm.field.street' },
    { key: 'street2', labelKey: 'settings.firm.field.street2' },
    { key: 'zipCode', labelKey: 'settings.firm.field.zipCode' },
    { key: 'city', labelKey: 'settings.firm.field.city' },
    { key: 'country', labelKey: 'settings.firm.field.country' },
  ] },
  { titleKey: 'settings.firm.contact', fields: [
    { key: 'phone', labelKey: 'settings.firm.field.phone' },
    { key: 'fax', labelKey: 'settings.firm.field.fax' },
    { key: 'mobile', labelKey: 'settings.firm.field.mobile' },
    { key: 'email', labelKey: 'settings.firm.field.email' },
    { key: 'website', labelKey: 'settings.firm.field.website' },
  ] },
  { titleKey: 'settings.firm.tax', fields: [
    { key: 'taxId', labelKey: 'settings.firm.field.taxId' },
    { key: 'vatId', labelKey: 'settings.firm.field.vatId' },
  ] },
  { titleKey: 'settings.firm.bank', fields: [
    { key: 'bank', labelKey: 'settings.firm.field.bank' },
    { key: 'bic', labelKey: 'settings.firm.field.bic' },
    { key: 'iban', labelKey: 'settings.firm.field.iban' },
  ] },
  { titleKey: 'settings.firm.escrow', fields: [
    { key: 'escrowBank', labelKey: 'settings.firm.field.bank' },
    { key: 'escrowBic', labelKey: 'settings.firm.field.bic' },
    { key: 'escrowIban', labelKey: 'settings.firm.field.iban' },
  ] },
];

/**
 * Editor for the firm master data ("Kanzleidaten"): a grouped form loaded from and saved to the v7
 * firm-profile endpoint. Editing needs `adminRole` (also enforced server-side); the hosting
 * Administration screen is already admin-gated.
 */
@Component({
  selector: 'jl-firm-profile',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule],
  template: `
    <div class="fp">
      @if (!canEdit()) { <p class="fp-hint">{{ 'settings.readOnlyHint' | transloco }}</p> }

      @if (loading()) {
        <p class="fp-muted">{{ 'settings.loading' | transloco }}</p>
      } @else if (loadError()) {
        <p class="fp-error">{{ 'settings.loadError' | transloco }}</p>
      } @else {
        @for (g of groups; track g.titleKey) {
          <fieldset class="fp-group">
            <legend>{{ g.titleKey | transloco }}</legend>
            <div class="fp-grid">
              @for (f of g.fields; track f.key) {
                <label class="fp-field">
                  <span>{{ f.labelKey | transloco }}</span>
                  <input type="text" [value]="draft()[f.key]" [disabled]="!canEdit() || saving()"
                         (input)="patch(f.key, $any($event.target).value)" />
                </label>
              }
            </div>
          </fieldset>
        }

        <div class="fp-foot">
          @if (savedOk()) { <span class="fp-ok">{{ 'settings.savedOk' | transloco }}</span> }
          @if (saveError()) { <span class="fp-error">{{ 'settings.saveError' | transloco }}</span> }
          <button type="button" class="btn-primary" [disabled]="!canEdit() || saving()" (click)="submit()">
            {{ (saving() ? 'settings.saving' : 'settings.save') | transloco }}
          </button>
        </div>
      }
    </div>
  `,
  styles: [`
    :host { display: block; }
    .fp { max-width: 720px; }
    .fp-hint { margin: 0 0 12px; font-size: .82rem; color: #b26a00; }
    .fp-muted { color: var(--jl-ink-soft); font-size: .88rem; }
    .fp-error { color: var(--jl-red); font-size: .84rem; }
    .fp-group { border: 1px solid var(--jl-line); border-radius: 10px; padding: 12px 14px 14px; margin: 0 0 14px; }
    .fp-group legend { font-size: .8rem; font-weight: 700; color: var(--jl-ink-soft); padding: 0 6px; text-transform: uppercase; letter-spacing: .04em; }
    .fp-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    .fp-field { display: flex; flex-direction: column; gap: 5px; font-size: .8rem; color: var(--jl-ink-faint); }
    .fp-field input { font: inherit; font-size: .9rem; color: var(--jl-ink); padding: 8px 10px;
      border: 1px solid var(--jl-line-strong); border-radius: 8px; background: var(--jl-surface); outline: none; }
    .fp-field input:focus { border-color: var(--jl-blue); }
    .fp-field input:disabled { opacity: .6; }
    .fp-foot { display: flex; align-items: center; justify-content: flex-end; gap: 12px; margin-top: 4px; }
    .fp-ok { color: var(--jl-green, #1a7f37); font-size: .84rem; }
    .btn-primary { font: inherit; font-size: .86rem; font-weight: 600; padding: 9px 18px; border: 0; border-radius: 8px;
      background: var(--jl-blue); color: #fff; cursor: pointer; }
    .btn-primary:hover:not(:disabled) { filter: brightness(1.06); }
    .btn-primary:disabled { opacity: .55; cursor: default; }
    @media (max-width: 560px) { .fp-grid { grid-template-columns: 1fr; } }
  `],
})
export class FirmProfileComponent implements OnInit {
  private readonly api = inject(FirmProfileService);
  private readonly auth = inject(AuthService);

  protected readonly groups = GROUPS;
  protected readonly draft = signal<FirmProfile>({ ...EMPTY });
  protected readonly loading = signal(false);
  protected readonly loadError = signal(false);
  protected readonly saving = signal(false);
  protected readonly saveError = signal(false);
  protected readonly savedOk = signal(false);

  protected readonly canEdit = computed(() => this.auth.hasRole('adminRole'));

  ngOnInit(): void {
    this.loading.set(true);
    this.loadError.set(false);
    this.api.get().subscribe({
      next: (p) => { this.draft.set({ ...EMPTY, ...p }); this.loading.set(false); },
      error: () => { this.loadError.set(true); this.loading.set(false); },
    });
  }

  protected patch(key: keyof FirmProfile, value: string): void {
    this.draft.update((d) => ({ ...d, [key]: value }));
    this.savedOk.set(false);
  }

  protected submit(): void {
    if (!this.canEdit() || this.saving()) { return; }
    this.saving.set(true);
    this.saveError.set(false);
    this.savedOk.set(false);
    this.api.save(this.draft()).subscribe({
      next: () => { this.saving.set(false); this.savedOk.set(true); },
      error: () => { this.saving.set(false); this.saveError.set(true); },
    });
  }
}
