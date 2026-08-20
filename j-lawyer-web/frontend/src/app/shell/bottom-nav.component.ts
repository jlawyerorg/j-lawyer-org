import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { MODULES, ModuleLink } from './modules';

/**
 * Mobile bottom navigation (phones only; hidden on larger screens via the shell layout).
 * Shows the mobile-flagged modules plus a "Mehr" button that opens a sheet with ALL modules —
 * otherwise the modules not in the bottom bar (contacts, documents, finance, reporting, beA …)
 * would be unreachable on a phone.
 */
@Component({
  selector: 'jl-bottom-nav',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, RouterLinkActive, IconComponent, TranslocoModule],
  template: `
    @if (menuOpen()) {
      <div class="sheet-backdrop" (click)="menuOpen.set(false)"></div>
      <div class="sheet" role="menu">
        <div class="sheet-grid">
          @for (m of allModules; track m.path) {
            <a [routerLink]="m.path" routerLinkActive="active" (click)="menuOpen.set(false)">
              <jl-icon [name]="m.icon" [size]="22" />
              <span>{{ m.labelKey | transloco }}</span>
            </a>
          }
        </div>
      </div>
    }
    <nav [attr.aria-label]="'nav.modulesMobile' | transloco">
      @for (m of items; track m.path) {
        <a [routerLink]="m.path" routerLinkActive="active" (click)="menuOpen.set(false)">
          <jl-icon [name]="m.icon" [size]="20" />
          <span>{{ m.labelKey | transloco }}</span>
        </a>
      }
      <button type="button" class="more-btn" [class.active]="menuOpen()"
              [attr.aria-expanded]="menuOpen()" (click)="menuOpen.set(!menuOpen())">
        <jl-icon name="more" [size]="20" /><span>{{ 'module.mehr' | transloco }}</span>
      </button>
    </nav>
  `,
  styles: [`
    :host { display: block; position: relative; background: var(--jl-navy); border-top: 1px solid var(--jl-navy-line); }
    nav { display: flex; justify-content: space-around; padding: 7px 4px 9px; }
    a, .more-btn {
      display: grid; justify-items: center; gap: 3px; color: #9db1c1;
      text-decoration: none; font-size: .6rem; text-align: center;
    }
    .more-btn { font: inherit; font-size: .6rem; background: transparent; border: 0; cursor: pointer; padding: 0; }
    a.active, .more-btn.active { color: #fff; }

    /* "Mehr" sheet: sits directly above the bottom bar; backdrop dismisses it. */
    .sheet-backdrop { position: fixed; inset: 0; z-index: 20; background: rgba(11,27,44,.45); }
    .sheet {
      position: absolute; bottom: 100%; left: 0; right: 0; z-index: 30;
      background: var(--jl-navy); border-top: 1px solid var(--jl-navy-line);
      box-shadow: 0 -8px 28px rgba(0,0,0,.3); max-height: 60vh; overflow-y: auto;
    }
    .sheet-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 4px; padding: 14px 10px; }
    .sheet-grid a {
      display: grid; justify-items: center; gap: 6px; padding: 12px 6px; border-radius: 10px;
      color: #cdd9e4; text-decoration: none; font-size: .72rem; text-align: center;
    }
    .sheet-grid a:hover, .sheet-grid a.active { background: rgba(255,255,255,.08); color: #fff; }
  `],
})
export class BottomNavComponent {
  protected readonly menuOpen = signal(false);
  protected readonly items: ModuleLink[] = MODULES.filter((m) => m.mobile);
  protected readonly allModules: ModuleLink[] = MODULES;
}
