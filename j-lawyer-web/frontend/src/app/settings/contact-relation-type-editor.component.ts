import {
  ChangeDetectionStrategy, Component, computed, input, OnInit, output, signal,
} from '@angular/core';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { ContactRelationType } from '../contacts/contact.models';

/**
 * Modal editor for a relationship type: catalogue name, the label per direction, the symmetry flag,
 * the category the pickers group by, colour and the active flag. A symmetric type reads the same both
 * ways, so its second label is disabled and mirrored from the first. Turning an existing type
 * symmetric is warned about: already recorded relationships keep their stored direction.
 */
@Component({
  selector: 'jl-contact-relation-type-editor',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent],
  template: `
    <div class="ed-backdrop" (click)="close.emit()"></div>
    <div class="ed-dialog" role="dialog" aria-modal="true">
      <header class="ed-head">
        <h2>{{ (isNew() ? 'settings.relationType.create' : 'settings.relationType.edit') | transloco }}</h2>
        <button type="button" class="icon-btn" (click)="close.emit()" [attr.aria-label]="'settings.cancel' | transloco">
          <jl-icon name="close" [size]="18" />
        </button>
      </header>

      <div class="ed-body">
        <label class="ed-field">
          <span>{{ 'settings.relationType.name' | transloco }}</span>
          <input type="text" [value]="draft().name" (input)="patch('name', $any($event.target).value)" />
        </label>
        <label class="ed-field">
          <span>{{ 'settings.relationType.labelFrom' | transloco }}</span>
          <input type="text" [value]="draft().labelFrom" (input)="patchLabelFrom($any($event.target).value)" />
        </label>
        <label class="ed-field">
          <span>{{ 'settings.relationType.labelTo' | transloco }}</span>
          <input type="text" [value]="draft().labelTo" [disabled]="draft().symmetric"
                 (input)="patch('labelTo', $any($event.target).value)" />
        </label>
        <label class="ed-check">
          <input type="checkbox" [checked]="draft().symmetric" (change)="patchSymmetric($any($event.target).checked)" />
          {{ 'settings.relationType.symmetric' | transloco }}
        </label>
        <p class="ed-hint">{{ 'settings.relationType.symmetricHint' | transloco }}</p>
        @if (symmetryWarning()) { <p class="ed-error">{{ 'settings.relationType.symmetricWarning' | transloco }}</p> }
        <div class="ed-grid">
          <label class="ed-field">
            <span>{{ 'settings.relationType.category' | transloco }}</span>
            <input type="text" list="crt-categories" autocomplete="off" [value]="draft().category"
                   (input)="patch('category', $any($event.target).value)" />
            <datalist id="crt-categories">
              @for (c of categories(); track c) { <option [value]="c"></option> }
            </datalist>
          </label>
          <label class="ed-field">
            <span>{{ 'settings.relationType.color' | transloco }}</span>
            <input type="color" [value]="hexColor()" (input)="patchColor($any($event.target).value)" />
          </label>
        </div>
        <label class="ed-check">
          <input type="checkbox" [checked]="draft().active" (change)="patch('active', $any($event.target).checked)" />
          {{ 'settings.relationType.active' | transloco }}
        </label>
        <p class="ed-hint">{{ 'settings.relationType.activeHint' | transloco }}</p>
        @if (error()) { <p class="ed-error">{{ error() }}</p> }
      </div>

      <footer class="ed-foot">
        <button type="button" class="btn-ghost" [disabled]="saving()" (click)="close.emit()">{{ 'settings.cancel' | transloco }}</button>
        <button type="button" class="btn-primary" [disabled]="saving() || !canSave()" (click)="submit()">
          {{ (saving() ? 'settings.saving' : 'settings.save') | transloco }}
        </button>
      </footer>
    </div>
  `,
  styleUrl: './finance-editor.css',
})
export class ContactRelationTypeEditorComponent implements OnInit {
  readonly relationType = input<ContactRelationType | null>(null);
  /** The categories already in use, offered as suggestions so the grouping stays consistent. */
  readonly categories = input<string[]>([]);
  readonly saving = input<boolean>(false);
  readonly error = input<string | null>(null);
  readonly save = output<ContactRelationType>();
  readonly close = output<void>();

  protected readonly draft = signal<ContactRelationType>({
    name: '', labelFrom: '', labelTo: '', symmetric: false, category: '', color: 0, sequenceNumber: 1, active: true,
  });
  protected readonly isNew = computed(() => !this.relationType());
  protected readonly canSave = computed(() => {
    const d = this.draft();
    return !!d.name.trim() && !!d.labelFrom.trim() && (d.symmetric || !!d.labelTo.trim());
  });
  /** Shown when an existing, directed type is being turned symmetric. */
  protected readonly symmetryWarning = computed(() => !this.isNew() && this.draft().symmetric && !this.wasSymmetric);
  /** Packed color int → #rrggbb for the native color input (low 24 bits, ignoring any alpha byte). */
  protected readonly hexColor = computed(() => '#' + ((this.draft().color & 0xffffff) >>> 0).toString(16).padStart(6, '0'));

  private wasSymmetric = false;

  ngOnInit(): void {
    const t = this.relationType();
    if (t) {
      this.draft.set({ ...t });
      this.wasSymmetric = t.symmetric;
    }
  }

  protected patch<K extends keyof ContactRelationType>(key: K, value: ContactRelationType[K]): void {
    this.draft.update((d) => ({ ...d, [key]: value }));
  }

  /** A symmetric type reads the same both ways, so the second label follows the first. */
  protected patchLabelFrom(value: string): void {
    this.draft.update((d) => ({ ...d, labelFrom: value, labelTo: d.symmetric ? value : d.labelTo }));
  }

  protected patchSymmetric(symmetric: boolean): void {
    this.draft.update((d) => ({ ...d, symmetric, labelTo: symmetric ? d.labelFrom : d.labelTo }));
  }

  protected patchColor(hex: string): void {
    this.patch('color', parseInt(hex.replace('#', ''), 16) || 0);
  }

  protected submit(): void {
    if (this.canSave() && !this.saving()) { this.save.emit(this.draft()); }
  }
}
