import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { TranslocoModule } from '@jsverse/transloco';
import { AuthService } from '../core/auth/auth.service';
import { BeaSettings, SystemSettingsService } from './system-settings.service';

/**
 * Editor for the beA integration: whether it is enabled and the endpoint URL of the beA connector
 * service. Needs `sysAdminRole` (also enforced server-side).
 */
@Component({
  selector: 'jl-bea-settings',
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
        <label class="ss-check">
          <input type="checkbox" [checked]="draft().enabled" [disabled]="!canEdit()"
                 (change)="patchEnabled($any($event.target).checked)" />
          <span>{{ 'settings.bea.enabled' | transloco }}</span>
        </label>
        <label class="ss-field">
          <span>{{ 'settings.bea.endpoint' | transloco }}</span>
          <input type="text" [value]="draft().endpoint" [disabled]="!canEdit() || saving()"
                 placeholder="http://localhost:7080"
                 (input)="patchEndpoint($any($event.target).value)" />
        </label>
        <p class="ss-note">{{ 'settings.bea.hint' | transloco }}</p>
        <div class="ss-foot">
          @if (savedOk()) { <span class="ss-ok">{{ 'settings.savedOk' | transloco }}</span> }
          @if (saveError()) { <span class="ss-error">{{ 'settings.saveError' | transloco }}</span> }
          <button type="button" class="btn-primary" [disabled]="!canEdit() || saving()" (click)="submit()">
            {{ (saving() ? 'settings.saving' : 'settings.save') | transloco }}
          </button>
        </div>
      }
    </div>
  `,
  styles: [`
    :host { display: block; }
    .ss { max-width: 640px; display: flex; flex-direction: column; gap: 12px; }
    .ss-hint { margin: 0; font-size: .82rem; color: #b26a00; }
    .ss-muted { color: var(--jl-ink-soft); font-size: .88rem; }
    .ss-error { color: var(--jl-red); font-size: .84rem; }
    .ss-field { display: flex; flex-direction: column; gap: 5px; font-size: .8rem; color: var(--jl-ink-faint); }
    .ss-field input { font: inherit; font-size: .9rem; color: var(--jl-ink); padding: 8px 10px;
      border: 1px solid var(--jl-line-strong); border-radius: 8px; background: var(--jl-surface); outline: none; }
    .ss-field input:focus { border-color: var(--jl-blue); }
    .ss-field input:disabled { opacity: .6; }
    .ss-check { display: flex; align-items: center; gap: 8px; font-size: .9rem; color: var(--jl-ink); }
    .ss-check input { width: 16px; height: 16px; }
    .ss-note { margin: 0; font-size: .78rem; color: var(--jl-ink-faint); }
    .ss-foot { display: flex; align-items: center; justify-content: flex-end; gap: 12px; }
    .ss-ok { color: var(--jl-green, #1a7f37); font-size: .84rem; }
    .btn-primary { font: inherit; font-size: .86rem; font-weight: 600; padding: 9px 18px; border: 0; border-radius: 8px;
      background: var(--jl-blue); color: #fff; cursor: pointer; }
    .btn-primary:disabled { opacity: .55; cursor: default; }
  `],
})
export class BeaSettingsComponent implements OnInit {
  private readonly api = inject(SystemSettingsService);
  private readonly auth = inject(AuthService);

  protected readonly draft = signal<BeaSettings>({ enabled: true, endpoint: '' });
  protected readonly loading = signal(false);
  protected readonly loadError = signal(false);
  protected readonly saving = signal(false);
  protected readonly saveError = signal(false);
  protected readonly savedOk = signal(false);
  protected readonly canEdit = computed(() => this.auth.hasRole('sysAdminRole'));

  ngOnInit(): void {
    this.loading.set(true);
    this.api.getBea().subscribe({
      next: (s) => { this.draft.set({ ...s }); this.loading.set(false); },
      error: () => { this.loadError.set(true); this.loading.set(false); },
    });
  }

  protected patchEnabled(value: boolean): void {
    this.draft.update((d) => ({ ...d, enabled: value }));
    this.savedOk.set(false);
  }

  protected patchEndpoint(value: string): void {
    this.draft.update((d) => ({ ...d, endpoint: value }));
    this.savedOk.set(false);
  }

  protected submit(): void {
    if (!this.canEdit() || this.saving()) { return; }
    this.saving.set(true);
    this.saveError.set(false);
    this.savedOk.set(false);
    this.api.saveBea(this.draft()).subscribe({
      next: () => { this.saving.set(false); this.savedOk.set(true); },
      error: () => { this.saving.set(false); this.saveError.set(true); },
    });
  }
}
