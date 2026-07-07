import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/**
 * Root shell. Scaffold-level only — the responsive App-Shell (Navy chrome, Modulleiste,
 * Master-Detail) is built per the approved design reference (design-mockup.html) in a
 * later phase. Standalone component, Signals-first (design.md Decision 2b).
 */
@Component({
  selector: 'jl-root',
  standalone: true,
  imports: [RouterOutlet],
  template: `
    <header class="app-head">
      <span class="wordmark"><b>j-lawyer</b></span>
    </header>
    <main class="app-main">
      <router-outlet />
    </main>
  `,
  styles: [`
    .app-head {
      height: 56px; display: flex; align-items: center; padding: 0 16px;
      background: var(--jl-navy); color: #eaf1f7; border-bottom: 1px solid var(--jl-navy-line);
    }
    .wordmark b { font-weight: 800; letter-spacing: -.02em; }
    .app-main { padding: 24px; }
  `],
})
export class AppComponent {}
