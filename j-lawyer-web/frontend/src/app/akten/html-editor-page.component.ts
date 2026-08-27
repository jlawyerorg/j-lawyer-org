import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { HtmlEditorComponent } from '../shared/html-editor.component';

/**
 * Full-page host for the HTML rich-text editor, opened in a separate tab/window (route
 * {@code htmledit/:id?name=…}) so several documents can be edited at once. Shares the
 * authenticated session (same origin); auto-saves on edit and once more on close.
 */
@Component({
  selector: 'jl-html-editor-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent, HtmlEditorComponent],
  template: `
    <div class="hp">
      <header class="hp-head">
        <jl-icon name="edit" [size]="16" />
        <h1>{{ name() }}</h1>
        <span class="hp-spacer"></span>
        <button type="button" class="hp-x" (click)="closeWindow()">
          <jl-icon name="close" [size]="16" /> {{ 'akten.docs.close' | transloco }}
        </button>
      </header>
      <div class="hp-body">
        @if (id()) { <jl-html-editor [documentId]="id()" /> }
      </div>
    </div>
  `,
  styles: [`
    :host { display: block; position: fixed; inset: 0; }
    .hp { display: flex; flex-direction: column; height: 100%; background: var(--jl-surface); }
    .hp-head { display: flex; align-items: center; gap: 10px; padding: 9px 14px; border-bottom: 1px solid var(--jl-line); }
    .hp-head h1 { margin: 0; font-size: .98rem; font-weight: 800; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .hp-spacer { flex: 1; }
    .hp-x { display: inline-flex; align-items: center; gap: 6px; font: inherit; font-size: .82rem; font-weight: 600; padding: 6px 11px; border-radius: 8px; border: 1px solid var(--jl-line-strong); background: var(--jl-surface); color: var(--jl-ink-soft); cursor: pointer; }
    .hp-x:hover { border-color: var(--jl-red); color: var(--jl-red); }
    .hp-body { flex: 1; min-height: 0; }
  `],
})
export class HtmlEditorPageComponent {
  private readonly route = inject(ActivatedRoute);
  protected readonly id = signal(this.route.snapshot.paramMap.get('id') ?? '');
  protected readonly name = signal(this.route.snapshot.queryParamMap.get('name') ?? '');

  protected closeWindow(): void {
    window.close();
  }
}
