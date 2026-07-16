import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { TranslocoModule } from '@jsverse/transloco';
import { AuthService } from '../core/auth/auth.service';
import { FinanceService, FinanceSettings } from './finance.service';

/**
 * Editor for global finance settings: currently the GiroCode (EPC-QR) image edge length in pixels,
 * loaded from and saved to the v7 finance-settings endpoint. Editing needs `adminRole` (also enforced
 * server-side); the hosting Administration screen is already admin-gated.
 */
@Component({
  selector: 'jl-finance-settings',
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
        <fieldset class="fp-group">
          <legend>{{ 'settings.finance.girocode' | transloco }}</legend>
          <label class="fp-field">
            <span>{{ 'settings.finance.giroCodePx' | transloco }}</span>
            <input type="number" min="0" step="1" [value]="draft().giroCodePx" [disabled]="!canEdit() || saving()"
                   (input)="patch($any($event.target).value)" />
          </label>
          <p class="fp-note">{{ 'settings.finance.giroCodePxHint' | transloco }}</p>
        </fieldset>

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
    .fp { max-width: 520px; }
    .fp-hint { margin: 0 0 12px; font-size: .82rem; color: #b26a00; }
    .fp-muted { color: var(--jl-ink-soft); font-size: .88rem; }
    .fp-error { color: var(--jl-red); font-size: .84rem; }
    .fp-group { border: 1px solid var(--jl-line); border-radius: 10px; padding: 12px 14px 14px; margin: 0 0 14px; }
    .fp-group legend { font-size: .8rem; font-weight: 700; color: var(--jl-ink-soft); padding: 0 6px; text-transform: uppercase; letter-spacing: .04em; }
    .fp-field { display: flex; flex-direction: column; gap: 5px; font-size: .8rem; color: var(--jl-ink-faint); max-width: 220px; }
    .fp-field input { font: inherit; font-size: .9rem; color: var(--jl-ink); padding: 8px 10px;
      border: 1px solid var(--jl-line-strong); border-radius: 8px; background: var(--jl-surface); outline: none; }
    .fp-field input:focus { border-color: var(--jl-blue); }
    .fp-field input:disabled { opacity: .6; }
    .fp-note { margin: 10px 0 0; font-size: .78rem; color: var(--jl-ink-faint); }
    .fp-foot { display: flex; align-items: center; justify-content: flex-end; gap: 12px; margin-top: 4px; }
    .fp-ok { color: var(--jl-green, #1a7f37); font-size: .84rem; }
    .btn-primary { font: inherit; font-size: .86rem; font-weight: 600; padding: 9px 18px; border: 0; border-radius: 8px;
      background: var(--jl-blue); color: #fff; cursor: pointer; }
    .btn-primary:hover:not(:disabled) { filter: brightness(1.06); }
    .btn-primary:disabled { opacity: .55; cursor: default; }
  `],
})
export class FinanceSettingsComponent implements OnInit {
  private readonly api = inject(FinanceService);
  private readonly auth = inject(AuthService);

  protected readonly draft = signal<FinanceSettings>({ giroCodePx: 150 });
  protected readonly loading = signal(false);
  protected readonly loadError = signal(false);
  protected readonly saving = signal(false);
  protected readonly saveError = signal(false);
  protected readonly savedOk = signal(false);

  protected readonly canEdit = computed(() => this.auth.hasRole('adminRole'));

  ngOnInit(): void {
    this.loading.set(true);
    this.loadError.set(false);
    this.api.getFinanceSettings().subscribe({
      next: (s) => { this.draft.set({ ...s }); this.loading.set(false); },
      error: () => { this.loadError.set(true); this.loading.set(false); },
    });
  }

  protected patch(value: string): void {
    const n = parseInt(value, 10);
    this.draft.set({ giroCodePx: Number.isFinite(n) ? n : 0 });
    this.savedOk.set(false);
  }

  protected submit(): void {
    if (!this.canEdit() || this.saving()) { return; }
    this.saving.set(true);
    this.saveError.set(false);
    this.savedOk.set(false);
    this.api.saveFinanceSettings(this.draft()).subscribe({
      next: () => { this.saving.set(false); this.savedOk.set(true); },
      error: () => { this.saving.set(false); this.saveError.set(true); },
    });
  }
}
