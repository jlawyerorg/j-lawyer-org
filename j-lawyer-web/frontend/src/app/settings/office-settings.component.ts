import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslocoModule } from '@jsverse/transloco';
import { AuthService } from '../core/auth/auth.service';
import { OfficeConfigService, OfficeSettings } from './office-config.service';

/**
 * "In-browser Office editing" system settings (OpenSpec add-web-client, Decision 6): the connection
 * to the external document server (Collabora / OnlyOffice). Editing needs {@code sysAdminRole} (also
 * enforced server-side; the System screen is already role-gated). The secret is write-only.
 */
@Component({
  selector: 'jl-office-settings',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, TranslocoModule],
  template: `
    <section class="os">
      <h2>{{ 'settings.office.title' | transloco }}</h2>
      <p class="os-hint">{{ 'settings.office.hint' | transloco }}</p>

      @if (loading()) {
        <p class="os-muted">{{ 'settings.loading' | transloco }}</p>
      } @else if (loadError()) {
        <p class="os-error">{{ 'settings.loadError' | transloco }}</p>
      } @else {
        <label class="os-field">
          <span>{{ 'settings.office.provider' | transloco }}</span>
          <select [ngModel]="model().provider" (ngModelChange)="patch('provider', $event)" [disabled]="!canEdit()">
            <option value="none">{{ 'settings.office.providerNone' | transloco }}</option>
            <option value="collabora">Collabora Online</option>
            <option value="onlyoffice">OnlyOffice</option>
          </select>
        </label>

        <label class="os-field">
          <span>{{ 'settings.office.baseUrl' | transloco }}</span>
          <input type="url" placeholder="https://collabora.example.org" [ngModel]="model().baseUrl"
                 (ngModelChange)="patch('baseUrl', $event)" [disabled]="!canEdit()" />
          <small>{{ 'settings.office.baseUrlHint' | transloco }}</small>
        </label>

        <label class="os-field">
          <span>{{ 'settings.office.wopiPublicUrl' | transloco }}</span>
          <input type="url" placeholder="https://kanzlei.example.org" [ngModel]="model().wopiPublicUrl"
                 (ngModelChange)="patch('wopiPublicUrl', $event)" [disabled]="!canEdit()" />
          <small>{{ 'settings.office.wopiPublicUrlHint' | transloco }}</small>
        </label>

        <label class="os-field">
          <span>{{ 'settings.office.secret' | transloco }}</span>
          <input type="password" autocomplete="new-password"
                 [placeholder]="(model().secretSet ? 'settings.office.secretSet' : 'settings.office.secretNone') | transloco"
                 [ngModel]="model().secret" (ngModelChange)="patch('secret', $event)" [disabled]="!canEdit()" />
          <small>{{ 'settings.office.secretHint' | transloco }}</small>
        </label>

        <div class="os-actions">
          <button type="button" class="os-btn" [disabled]="!canEdit() || saving()" (click)="save()">
            {{ (saving() ? 'settings.saving' : 'settings.save') | transloco }}
          </button>
          @if (savedOk()) { <span class="os-ok">{{ 'settings.savedOk' | transloco }}</span> }
          @if (saveError()) { <span class="os-error">{{ 'settings.saveError' | transloco }}</span> }
        </div>
      }
    </section>
  `,
  styles: [`
    .os { max-width: 560px; display: flex; flex-direction: column; gap: 12px; }
    .os h2 { margin: 0; font-size: 1.05rem; font-weight: 800; }
    .os-hint { margin: 0; color: var(--jl-ink-soft); font-size: .86rem; }
    .os-muted { color: var(--jl-ink-faint); }
    .os-error { color: var(--jl-red); font-size: .84rem; }
    .os-ok { color: var(--jl-green, #1a7f37); font-size: .84rem; }
    .os-field { display: flex; flex-direction: column; gap: 4px; }
    .os-field > span { font-size: .74rem; font-weight: 700; text-transform: uppercase; letter-spacing: .03em; color: var(--jl-ink-faint); }
    .os-field small { color: var(--jl-ink-soft); font-size: .78rem; }
    input, select { font: inherit; font-size: .9rem; padding: 8px 10px; border: 1px solid var(--jl-line-strong); border-radius: 8px; background: var(--jl-surface); color: var(--jl-ink); }
    input:focus, select:focus { outline: none; border-color: var(--jl-blue); }
    .os-actions { display: flex; align-items: center; gap: 12px; margin-top: 4px; }
    .os-btn { font: inherit; font-size: .86rem; font-weight: 650; padding: 8px 16px; border-radius: 8px; border: 1px solid var(--jl-blue); background: var(--jl-blue); color: #fff; cursor: pointer; }
    .os-btn:disabled { opacity: .5; cursor: default; }
  `],
})
export class OfficeSettingsComponent implements OnInit {
  private readonly api = inject(OfficeConfigService);
  private readonly auth = inject(AuthService);

  protected readonly canEdit = computed(() => this.auth.hasRole('sysAdminRole'));
  protected readonly loading = signal(true);
  protected readonly loadError = signal(false);
  protected readonly saving = signal(false);
  protected readonly savedOk = signal(false);
  protected readonly saveError = signal(false);
  protected readonly model = signal<OfficeSettings>({ provider: 'none', baseUrl: '', wopiPublicUrl: '', secret: '', secretSet: false });

  ngOnInit(): void {
    this.api.getSettings().subscribe({
      next: (s) => { this.model.set({ ...s, secret: '' }); this.loading.set(false); },
      error: () => { this.loadError.set(true); this.loading.set(false); },
    });
  }

  protected patch<K extends keyof OfficeSettings>(key: K, value: OfficeSettings[K]): void {
    this.model.update((m) => ({ ...m, [key]: value }));
    this.savedOk.set(false);
  }

  protected save(): void {
    this.saving.set(true);
    this.savedOk.set(false);
    this.saveError.set(false);
    this.api.saveSettings(this.model()).subscribe({
      next: (s) => { this.model.set({ ...s, secret: '' }); this.saving.set(false); this.savedOk.set(true); },
      error: () => { this.saving.set(false); this.saveError.set(true); },
    });
  }
}
