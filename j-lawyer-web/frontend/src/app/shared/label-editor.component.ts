import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { TranslocoModule } from '@jsverse/transloco';
import { MultiValueTagDef } from '../akten/case.models';

/** One applied label: a plain label has an empty `value`; a list (multi-value) label carries one. */
export interface LabelValue { name: string; value: string; }

/**
 * Reusable label ("Etikett") editor rendered exactly like the case detail's Etiketten card:
 * list (multi-value) labels as an inline name + dropdown chip, plain labels as removable chips, and
 * an "+ hinzufügen" dropdown for the remaining plain-label templates. Fully controlled — it never
 * mutates state itself, it emits the new label set via {@link changed} and the host persists it.
 */
@Component({
  selector: 'jl-label-editor',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule],
  template: `
    <div class="le-chips">
      @for (def of listDefs(); track def.tagName) {
        <span class="le-chip le-list">
          <span class="le-name">{{ def.tagName }}</span>
          <select #sel class="le-select" [disabled]="disabled()" (change)="setList(def.tagName, sel.value)">
            <option value="" [selected]="!listValue(def.tagName)">—</option>
            @for (v of def.values; track v) {
              <option [value]="v" [selected]="listValue(def.tagName) === v">{{ v }}</option>
            }
          </select>
        </span>
      }
      @for (name of singleNames(); track name) {
        <span class="le-chip le-removable">{{ name }}
          <button type="button" class="le-x" [disabled]="disabled()" (click)="removeSingle(name)"
                  [title]="'labelEditor.remove' | transloco">✕</button>
        </span>
      }
      @if (!singleNames().length && !listDefs().length) {
        <span class="le-muted">{{ 'labelEditor.none' | transloco }}</span>
      }
    </div>
    @if (available().length) {
      <select class="le-add" #add [disabled]="disabled()" (change)="addSingle(add.value); add.value=''">
        <option value="">+ {{ 'labelEditor.add' | transloco }}</option>
        @for (name of available(); track name) { <option [value]="name">{{ name }}</option> }
      </select>
    }
  `,
  styles: [`
    :host { display: block; }
    .le-chips { display: flex; flex-wrap: wrap; gap: 6px; }
    .le-chip {
      font-size: .78rem; font-weight: 600; color: var(--jl-ink-soft);
      background: var(--jl-surface-alt); border: 1px solid var(--jl-line); border-radius: 12px; padding: 3px 10px;
    }
    .le-removable { display: inline-flex; align-items: center; gap: 5px; padding-right: 5px; }
    .le-x { display: inline-grid; place-items: center; width: 16px; height: 16px; border: 0; border-radius: 50%;
      background: transparent; color: var(--jl-ink-faint); font-size: .7rem; cursor: pointer; }
    .le-x:hover:not(:disabled) { background: color-mix(in srgb, var(--jl-red) 16%, transparent); color: var(--jl-red); }
    .le-x:disabled { opacity: .5; cursor: default; }
    .le-list { display: inline-flex; align-items: center; gap: 5px; padding: 2px 6px 2px 10px; }
    .le-list .le-name { color: var(--jl-ink-soft); }
    .le-list .le-name::after { content: ":"; }
    .le-select { font: inherit; font-size: .78rem; font-weight: 600; color: var(--jl-ink);
      border: 0; background: transparent; padding: 1px 2px; margin: 0; cursor: pointer; max-width: 170px; }
    .le-select:focus { outline: none; }
    .le-muted { color: var(--jl-ink-faint); font-size: .78rem; }
    .le-add {
      margin-top: 8px; font: inherit; font-size: .78rem; font-weight: 600; color: var(--jl-blue);
      background: var(--jl-surface); border: 1px dashed var(--jl-line-strong); border-radius: 8px; padding: 5px 8px; cursor: pointer;
    }
    .le-add:hover { border-color: var(--jl-blue); }
  `],
})
export class LabelEditorComponent {
  /** Currently applied labels. */
  readonly labels = input<LabelValue[]>([]);
  /** Plain-label template names available to add. */
  readonly singleDict = input<string[]>([]);
  /** List (multi-value) label definitions. */
  readonly listDefs = input<MultiValueTagDef[]>([]);
  readonly disabled = input<boolean>(false);

  readonly changed = output<LabelValue[]>();

  private readonly listNames = computed(() => new Set(this.listDefs().map((d) => d.tagName)));

  /** Names of the plain (non-list) labels currently applied. */
  protected readonly singleNames = computed(() =>
    this.labels().filter((l) => !this.listNames().has(l.name)).map((l) => l.name));

  /** Plain-label templates not yet applied (and not list-label names). */
  protected readonly available = computed(() => {
    const present = new Set(this.labels().map((l) => l.name));
    const list = this.listNames();
    return this.singleDict().filter((n) => !present.has(n) && !list.has(n));
  });

  protected listValue(name: string): string {
    return this.labels().find((l) => l.name === name)?.value ?? '';
  }

  protected setList(name: string, value: string): void {
    const rest = this.labels().filter((l) => l.name !== name);
    this.changed.emit(value ? [...rest, { name, value }] : rest);
  }

  protected addSingle(name: string): void {
    if (!name || this.labels().some((l) => l.name === name)) { return; }
    this.changed.emit([...this.labels(), { name, value: '' }]);
  }

  protected removeSingle(name: string): void {
    this.changed.emit(this.labels().filter((l) => l.name !== name));
  }
}
