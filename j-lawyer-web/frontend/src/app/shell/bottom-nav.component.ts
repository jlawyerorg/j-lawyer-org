import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { IconComponent } from '../shared/icon.component';
import { MODULES, ModuleLink } from './modules';

/**
 * Mobile bottom navigation (phones only; hidden on larger screens via the shell layout).
 * Shows the mobile-flagged modules plus a "Mehr" entry for the rest.
 */
@Component({
  selector: 'jl-bottom-nav',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, RouterLinkActive, IconComponent],
  template: `
    <nav aria-label="Module (mobil)">
      @for (m of items; track m.path) {
        <a [routerLink]="m.path" routerLinkActive="active">
          <jl-icon [name]="m.icon" [size]="20" />
          <span>{{ shortLabel(m) }}</span>
        </a>
      }
      <a [routerLink]="'einstellungen'" routerLinkActive="active">
        <jl-icon name="more" [size]="20" /><span>Mehr</span>
      </a>
    </nav>
  `,
  styles: [`
    :host { display: block; background: var(--jl-navy); border-top: 1px solid var(--jl-navy-line); }
    nav { display: flex; justify-content: space-around; padding: 7px 4px 9px; }
    a {
      display: grid; justify-items: center; gap: 3px; color: #9db1c1;
      text-decoration: none; font-size: .6rem;
    }
    a.active { color: #fff; }
  `],
})
export class BottomNavComponent {
  protected readonly items: ModuleLink[] = MODULES.filter((m) => m.mobile);

  /** First word of the label — bottom-nav has little room. */
  protected shortLabel(m: ModuleLink): string {
    return m.label.split(' ')[0];
  }
}
