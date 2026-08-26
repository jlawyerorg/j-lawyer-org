import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { OfficeEditorComponent } from './office-editor.component';

/**
 * Full-page host for the in-browser Office editor, opened in a separate tab/window (route
 * {@code office/:id?name=…}) so several documents can be edited at once (OpenSpec add-web-client,
 * Decision 6). Shares the authenticated session (same origin); the editor mints its own token.
 */
@Component({
  selector: 'jl-office-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent, OfficeEditorComponent],
  template: `
    <div class="op">
      <header class="op-head">
        <jl-icon name="edit" [size]="16" />
        <h1>{{ name() }}</h1>
        <span class="op-spacer"></span>
        <button type="button" class="op-x" (click)="closeWindow()">
          <jl-icon name="close" [size]="16" /> {{ 'akten.docs.close' | transloco }}
        </button>
      </header>
      <div class="op-body">
        @if (id()) { <jl-office-editor [documentId]="id()" /> }
      </div>
    </div>
  `,
  styles: [`
    :host { display: block; position: fixed; inset: 0; }
    .op { display: flex; flex-direction: column; height: 100%; background: var(--jl-surface); }
    .op-head { display: flex; align-items: center; gap: 10px; padding: 9px 14px; border-bottom: 1px solid var(--jl-line); }
    .op-head h1 { margin: 0; font-size: .98rem; font-weight: 800; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .op-spacer { flex: 1; }
    .op-x { display: inline-flex; align-items: center; gap: 6px; font: inherit; font-size: .82rem; font-weight: 600; padding: 6px 11px; border-radius: 8px; border: 1px solid var(--jl-line-strong); background: var(--jl-surface); color: var(--jl-ink-soft); cursor: pointer; }
    .op-x:hover { border-color: var(--jl-red); color: var(--jl-red); }
    .op-body { flex: 1; min-height: 0; }
  `],
})
export class OfficePageComponent {
  private readonly route = inject(ActivatedRoute);
  protected readonly id = signal(this.route.snapshot.paramMap.get('id') ?? '');
  protected readonly name = signal(this.route.snapshot.queryParamMap.get('name') ?? '');

  protected closeWindow(): void {
    window.close();
  }
}
