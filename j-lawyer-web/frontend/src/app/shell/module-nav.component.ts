import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { MODULES, ModuleLink } from './modules';

/**
 * Navy module bar (left). Collapses to an icon rail on tablet widths (labels hidden via
 * CSS). Active state comes from the router. Footer items (settings) are pinned to the
 * bottom. Hidden on phones — the bottom-nav takes over.
 */
@Component({
  selector: 'jl-module-nav',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, RouterLinkActive, IconComponent, TranslocoModule],
  template: `
    <nav [attr.aria-label]="'nav.modules' | transloco">
      @for (m of primary; track m.path) {
        <a [routerLink]="m.path" routerLinkActive="active" [title]="m.labelKey | transloco">
          <jl-icon [name]="m.icon" />
          <span class="txt">{{ m.labelKey | transloco }}</span>
          @if (m.badge) { <span class="nav-badge">{{ m.badge }}</span> }
        </a>
      }
      <span class="grow"></span>
      @for (m of footer; track m.path) {
        <a [routerLink]="m.path" routerLinkActive="active" [title]="m.labelKey | transloco">
          <jl-icon [name]="m.icon" />
          <span class="txt">{{ m.labelKey | transloco }}</span>
        </a>
      }
    </nav>
  `,
  styles: [`
    :host { display: block; height: 100%; background: var(--jl-navy); border-right: 1px solid var(--jl-navy-line); }
    nav { display: flex; flex-direction: column; height: 100%; padding: 10px; gap: 2px; }
    a {
      position: relative; display: flex; align-items: center; gap: 11px; padding: 9px 11px;
      border-radius: 8px; color: #b9c9d6; text-decoration: none; font-size: .9rem; font-weight: 500;
      border: 1px solid transparent; white-space: nowrap; transition: background .13s ease, color .13s ease;
    }
    a:hover { background: #ffffff10; color: #eef4f9; }
    a.active { background: #ffffff16; color: #fff; border-color: #ffffff1a; }
    a.active::before {
      content: ""; position: absolute; left: -10px; width: 3px; height: 22px;
      border-radius: 3px; background: var(--jl-blue);
    }
    .nav-badge {
      margin-left: auto; background: var(--jl-red); color: #fff; font-size: .62rem;
      font-weight: 700; border-radius: 8px; padding: 1px 6px;
    }
    .grow { flex: 1; }
    /* Tablet: icon rail (labels + badges hidden) */
    @media (max-width: 1024px) {
      a .txt, .nav-badge { display: none; }
      a { justify-content: center; padding: 11px 0; }
      a.active::before { display: none; }
    }
  `],
})
export class ModuleNavComponent {
  protected readonly primary: ModuleLink[] = MODULES.filter((m) => !m.footer);
  protected readonly footer: ModuleLink[] = MODULES.filter((m) => m.footer);
}
