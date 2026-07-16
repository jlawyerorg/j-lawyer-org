import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TranslocoModule } from '@jsverse/transloco';
import { AuthService } from '../core/auth/auth.service';

/**
 * Settings layout: a top tab bar switching between the three role-scoped screens (Allgemein /
 * Administration / System), plus the routed screen below. Tabs the user cannot enter are hidden
 * (the routes are also `roleGuard`-protected, and the server enforces every write). Reached from the
 * single gear entry in the module bar.
 */
@Component({
  selector: 'jl-settings',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, TranslocoModule],
  template: `
    <div class="se">
      <nav class="se-tabs">
        <a routerLink="general" routerLinkActive="active" class="se-tab">{{ 'settings.screen.general' | transloco }}</a>
        @if (canAdmin()) {
          <a routerLink="administration" routerLinkActive="active" class="se-tab">{{ 'settings.screen.administration' | transloco }}</a>
        }
        @if (canSystem()) {
          <a routerLink="system" routerLinkActive="active" class="se-tab">{{ 'settings.screen.system' | transloco }}</a>
        }
      </nav>
      <div class="se-body">
        <router-outlet />
      </div>
    </div>
  `,
  styles: [`
    :host { display: block; height: 100%; }
    .se { display: flex; flex-direction: column; height: 100%; min-height: 0; }
    .se-tabs { display: flex; gap: 2px; padding: 8px 14px 0; border-bottom: 1px solid var(--jl-line); flex: none; }
    .se-tab { font: inherit; font-size: .9rem; font-weight: 600; color: var(--jl-ink-soft); text-decoration: none;
      padding: 9px 15px; border-radius: 8px 8px 0 0; border-bottom: 2px solid transparent; }
    .se-tab:hover { color: var(--jl-ink); background: var(--jl-surface-alt); }
    .se-tab.active { color: var(--jl-blue); border-bottom-color: var(--jl-blue); }
    .se-body { flex: 1 1 auto; min-height: 0; }
  `],
})
export class SettingsComponent {
  private readonly auth = inject(AuthService);
  protected readonly canAdmin = computed(() => this.auth.hasRole('adminRole'));
  protected readonly canSystem = computed(() => this.auth.hasRole('sysAdminRole'));
}
