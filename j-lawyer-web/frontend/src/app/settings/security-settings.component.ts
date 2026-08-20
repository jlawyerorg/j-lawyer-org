import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { TranslocoModule } from '@jsverse/transloco';
import { AuthService } from '../core/auth/auth.service';
import { SecuritySettings, SystemSettingsService } from './system-settings.service';

/**
 * Server-wide security settings — the web equivalent of the desktop "Sicherheit" dialog. Currently a
 * single policy: whether complex passwords are enforced. Needs `adminRole` (enforced server-side).
 */
@Component({
  selector: 'jl-security-settings',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule],
  template: `
    <div class="ss">
      @if (!canEdit()) { <p class="ss-hint">{{ 'settings.readOnlyHint' | transloco }}</p> }
      @if (loading()) {
        <p class="ss-muted">{{ 'settings.loading' | transloco }}</p>
      } @else if (loadError()) {
        <p class="ss-error">{{ 'settings.loadError' | transloco }}</p>
      } @else {
        <fieldset class="ss-sec">
          <legend>{{ 'settings.security.passwordTitle' | transloco }}</legend>
          <label class="ss-radio"><input type="radio" name="pwc" [checked]="draft().forcePasswordComplexity" [disabled]="!canEdit()" (change)="patch(true)" /><span>{{ 'settings.security.forceComplex' | transloco }}</span></label>
          <label class="ss-radio"><input type="radio" name="pwc" [checked]="!draft().forcePasswordComplexity" [disabled]="!canEdit()" (change)="patch(false)" /><span>{{ 'settings.security.allowAny' | transloco }}</span></label>
          <p class="ss-note">{{ 'settings.security.hint' | transloco }}</p>
        </fieldset>
        <div class="ss-foot">
          @if (savedOk()) { <span class="ss-ok">{{ 'settings.savedOk' | transloco }}</span> }
          @if (saveError()) { <span class="ss-error">{{ 'settings.saveError' | transloco }}</span> }
          <button type="button" class="btn-primary" [disabled]="!canEdit() || saving()" (click)="submit()">{{ (saving() ? 'settings.saving' : 'settings.save') | transloco }}</button>
        </div>
      }
    </div>
  `,
  styles: [`
    :host { display: block; }
    .ss { max-width: 560px; display: flex; flex-direction: column; gap: 14px; }
    .ss-hint { margin: 0; font-size: .82rem; color: #b26a00; }
    .ss-muted { color: var(--jl-ink-soft); font-size: .88rem; }
    .ss-error { color: var(--jl-red); font-size: .84rem; }
    .ss-sec { border: 1px solid var(--jl-line); border-radius: 10px; padding: 12px 14px; margin: 0; display: flex; flex-direction: column; gap: 8px; }
    .ss-sec legend { font-size: .8rem; font-weight: 700; color: var(--jl-ink-soft); padding: 0 6px; text-transform: uppercase; letter-spacing: .04em; }
    .ss-radio { display: flex; align-items: center; gap: 8px; font-size: .9rem; color: var(--jl-ink); }
    .ss-radio input { width: 16px; height: 16px; }
    .ss-note { margin: 4px 0 0; font-size: .78rem; color: var(--jl-ink-faint); }
    .ss-foot { display: flex; align-items: center; justify-content: flex-end; gap: 12px; }
    .ss-ok { color: var(--jl-green, #1a7f37); font-size: .84rem; }
    .btn-primary { font: inherit; font-size: .86rem; font-weight: 600; padding: 9px 18px; border: 0; border-radius: 8px; background: var(--jl-blue); color: #fff; cursor: pointer; }
    .btn-primary:disabled { opacity: .55; cursor: default; }
  `],
})
export class SecuritySettingsComponent implements OnInit {
  private readonly api = inject(SystemSettingsService);
  private readonly auth = inject(AuthService);

  protected readonly draft = signal<SecuritySettings>({ forcePasswordComplexity: true });
  protected readonly loading = signal(false);
  protected readonly loadError = signal(false);
  protected readonly saving = signal(false);
  protected readonly saveError = signal(false);
  protected readonly savedOk = signal(false);
  protected readonly canEdit = computed(() => this.auth.hasRole('adminRole'));

  ngOnInit(): void {
    this.loading.set(true);
    this.api.getSecurity().subscribe({
      next: (s) => { this.draft.set({ ...s }); this.loading.set(false); },
      error: () => { this.loadError.set(true); this.loading.set(false); },
    });
  }

  protected patch(value: boolean): void {
    this.draft.set({ forcePasswordComplexity: value });
    this.savedOk.set(false);
  }

  protected submit(): void {
    if (!this.canEdit() || this.saving()) { return; }
    this.saving.set(true); this.saveError.set(false); this.savedOk.set(false);
    this.api.saveSecurity(this.draft()).subscribe({
      next: () => { this.saving.set(false); this.savedOk.set(true); },
      error: () => { this.saving.set(false); this.saveError.set(true); },
    });
  }
}
