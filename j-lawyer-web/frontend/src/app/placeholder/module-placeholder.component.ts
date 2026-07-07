import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

/**
 * Generic per-module landing until the real module is built (OpenSpec add-web-client,
 * phased rollout). Reads its label from the route data so one component serves all
 * not-yet-implemented modules.
 */
@Component({
  selector: 'jl-module-placeholder',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="card">
      <p class="label">Modul</p>
      <h1>{{ label() }}</h1>
      <p class="muted">
        Dieses Modul wird phasenweise umgesetzt. Shell, Design-System und Routing stehen;
        die Fachinhalte folgen (OpenSpec-Change <code>add-web-client</code>).
      </p>
      <div class="swatches" aria-hidden="true">
        <span style="background:var(--jl-blue)"></span>
        <span style="background:var(--jl-red)"></span>
        <span style="background:var(--jl-green)"></span>
        <span style="background:var(--jl-navy)"></span>
      </div>
    </section>
  `,
  styles: [`
    .card {
      max-width: 640px; background: var(--jl-surface); color: var(--jl-ink);
      border: 1px solid var(--jl-line); border-radius: 12px; padding: 24px 28px;
      box-shadow: 0 1px 2px rgba(11,27,44,.06), 0 6px 20px rgba(11,27,44,.07);
    }
    h1 { margin: 4px 0 8px; font-size: 1.6rem; font-weight: 800; letter-spacing: -.02em; }
    .label {
      margin: 0; font-size: .7rem; text-transform: uppercase; letter-spacing: .09em;
      font-weight: 600; color: var(--jl-ink-faint);
    }
    .muted { color: var(--jl-ink-soft); }
    code { font-family: ui-monospace, "SF Mono", Menlo, Consolas, monospace; }
    .swatches { display: flex; gap: 8px; margin-top: 18px; }
    .swatches span { width: 40px; height: 40px; border-radius: 8px; }
  `],
})
export class ModulePlaceholderComponent {
  private readonly route = inject(ActivatedRoute);
  protected readonly label = signal<string>(this.route.snapshot.data['label'] ?? 'Modul');
}
