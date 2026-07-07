import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AppHeaderComponent } from './app-header.component';
import { ModuleNavComponent } from './module-nav.component';
import { BottomNavComponent } from './bottom-nav.component';

/**
 * Responsive application shell (design-mockup.html): header + module bar + routed
 * content, with a mobile bottom-nav. Grid areas adapt per breakpoint —
 * desktop: full nav + content; tablet: icon rail; phone: single pane + bottom-nav.
 * Module content renders in <router-outlet>.
 */
@Component({
  selector: 'jl-shell',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, AppHeaderComponent, ModuleNavComponent, BottomNavComponent],
  template: `
    <jl-app-header class="head" />
    <jl-module-nav class="nav" />
    <main class="main"><router-outlet /></main>
    <jl-bottom-nav class="botnav" />
  `,
  styles: [`
    :host {
      display: grid;
      grid-template-areas: "nav head" "nav main";
      grid-template-columns: 232px 1fr;
      grid-template-rows: 56px 1fr;
      height: 100dvh;
      background: var(--jl-ground);
    }
    .head { grid-area: head; }
    .nav { grid-area: nav; }
    .main { grid-area: main; overflow: auto; min-height: 0; padding: 24px; }
    .botnav { display: none; }

    /* Tablet: narrow icon rail */
    @media (max-width: 1024px) {
      :host { grid-template-columns: 64px 1fr; }
    }
    /* Phone: single pane + bottom-nav, module bar hidden */
    @media (max-width: 680px) {
      :host {
        grid-template-areas: "head" "main" "botnav";
        grid-template-columns: 1fr;
        grid-template-rows: 52px 1fr auto;
      }
      .nav { display: none; }
      .botnav { display: block; grid-area: botnav; position: sticky; bottom: 0; }
      .main { padding: 16px; }
    }
  `],
})
export class ShellComponent {}
