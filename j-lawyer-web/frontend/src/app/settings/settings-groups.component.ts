import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { TranslocoModule } from '@jsverse/transloco';
import { AdminGroup, UsersAdminService } from './users-admin.service';

/**
 * "Gruppen" section: read-only list of security groups (v6 security has no create/update endpoint
 * yet — group management lands in a later wave).
 */
@Component({
  selector: 'jl-settings-groups',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule],
  template: `
    <div class="sg">
      <p class="sg-note">{{ 'settings.groups.readOnly' | transloco }}</p>
      @if (loading()) {
        <p class="sg-muted">{{ 'settings.loading' | transloco }}</p>
      } @else if (loadError()) {
        <p class="sg-error">{{ 'settings.loadError' | transloco }}</p>
      } @else if (groups().length === 0) {
        <p class="sg-muted">{{ 'settings.empty' | transloco }}</p>
      } @else {
        <ul class="sg-list">
          @for (g of groups(); track g.id) {
            <li class="sg-row">
              <span class="sg-name">{{ g.name }}</span>
              @if (g.abbreviation) { <span class="sg-abbr">{{ g.abbreviation }}</span> }
            </li>
          }
        </ul>
      }
    </div>
  `,
  styles: [`
    :host { display: block; }
    .sg { max-width: 560px; }
    .sg-note { margin: 0 0 12px; font-size: .8rem; color: var(--jl-ink-soft); background: var(--jl-surface-alt);
      border-radius: 8px; padding: 9px 11px; }
    .sg-muted { color: var(--jl-ink-soft); font-size: .88rem; }
    .sg-error { color: var(--jl-red); font-size: .84rem; }
    .sg-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 4px; }
    .sg-row { display: flex; align-items: center; gap: 10px; padding: 8px 11px; border: 1px solid var(--jl-line);
      border-radius: 8px; background: var(--jl-surface); }
    .sg-name { flex: 1 1 auto; font-size: .9rem; color: var(--jl-ink); }
    .sg-abbr { font-size: .76rem; color: var(--jl-ink-faint); font-variant-numeric: tabular-nums; }
  `],
})
export class SettingsGroupsComponent implements OnInit {
  private readonly api = inject(UsersAdminService);

  protected readonly groups = signal<AdminGroup[]>([]);
  protected readonly loading = signal(false);
  protected readonly loadError = signal(false);

  ngOnInit(): void {
    this.loading.set(true);
    this.api.listGroups().subscribe({
      next: (g) => { this.groups.set(g); this.loading.set(false); },
      error: () => { this.loadError.set(true); this.loading.set(false); },
    });
  }
}
