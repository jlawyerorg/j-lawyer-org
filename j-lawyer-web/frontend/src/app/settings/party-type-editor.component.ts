import {
  ChangeDetectionStrategy, Component, computed, input, OnInit, output, signal,
} from '@angular/core';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { PartyType } from './party-type.service';

/** Modal editor for a party type (Beteiligtentyp): name, placeholder token, color, sort sequence. */
@Component({
  selector: 'jl-party-type-editor',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent],
  template: `
    <div class="ed-backdrop" (click)="close.emit()"></div>
    <div class="ed-dialog" role="dialog" aria-modal="true">
      <header class="ed-head">
        <h2>{{ (isNew() ? 'settings.party.create' : 'settings.party.edit') | transloco }}</h2>
        <button type="button" class="icon-btn" (click)="close.emit()" [attr.aria-label]="'settings.cancel' | transloco">
          <jl-icon name="close" [size]="18" />
        </button>
      </header>

      <div class="ed-body">
        <label class="ed-field">
          <span>{{ 'settings.party.name' | transloco }}</span>
          <input type="text" [value]="draft().name" (input)="patch('name', $any($event.target).value)" />
        </label>
        <label class="ed-field">
          <span>{{ 'settings.party.placeHolder' | transloco }}</span>
          <input type="text" [value]="draft().placeHolder" (input)="patch('placeHolder', $any($event.target).value)" />
        </label>
        <label class="ed-field">
          <span>{{ 'settings.party.color' | transloco }}</span>
          <input type="color" [value]="hexColor()" (input)="patchColor($any($event.target).value)" />
        </label>
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
export class PartyTypeEditorComponent implements OnInit {
  readonly partyType = input<PartyType | null>(null);
  readonly saving = input<boolean>(false);
  readonly error = input<string | null>(null);
  readonly save = output<PartyType>();
  readonly close = output<void>();

  protected readonly draft = signal<PartyType>({
    name: '', placeHolder: '', color: 0, sequenceNumber: 1,
  });
  protected readonly isNew = computed(() => !this.partyType());
  protected readonly canSave = computed(() => !!this.draft().name.trim() && !!this.draft().placeHolder.trim());
  /** Packed color int → #rrggbb for the native color input (low 24 bits, ignoring any alpha byte). */
  protected readonly hexColor = computed(() => '#' + ((this.draft().color & 0xffffff) >>> 0).toString(16).padStart(6, '0'));

  ngOnInit(): void {
    const t = this.partyType();
    if (t) { this.draft.set({ ...t }); }
  }

  protected patch<K extends keyof PartyType>(key: K, value: PartyType[K]): void {
    this.draft.update((d) => ({ ...d, [key]: value }));
  }

  protected patchColor(hex: string): void {
    this.patch('color', parseInt(hex.replace('#', ''), 16) || 0);
  }

  protected submit(): void {
    if (this.canSave() && !this.saving()) { this.save.emit(this.draft()); }
  }
}
