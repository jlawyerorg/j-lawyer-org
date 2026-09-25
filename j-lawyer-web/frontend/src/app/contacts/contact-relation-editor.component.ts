import { ChangeDetectionStrategy, Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { ContactsService } from './contacts.service';
import {
  ContactOverview, ContactRelation, ContactRelationType, ContactRelationWrite, RelationLabelOption,
} from './contact.models';

/** The label options of one category, as offered by the picker. */
interface LabelGroup {
  category: string;
  options: RelationLabelOption[];
}

/**
 * Modal dialog to record a relationship between the open contact and another one, and to edit an
 * existing relationship's note. Picking a **label** (grouped by the type's category) fixes the
 * relationship type and its direction: the type's `labelTo` is the reverse direction, so the parent
 * stores the two contacts swapped. The labels are master data and are shown as configured — they are
 * never translated. Emits the write on confirm; the parent does the REST call. Rendered only while
 * open (mounted via @if).
 */
@Component({
  selector: 'jl-contact-relation-editor',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, TranslocoModule, IconComponent],
  template: `
    <div class="backdrop" (click)="close.emit()"></div>
    <div class="dialog" role="dialog" aria-modal="true">
      <header class="dh">
        <h2>{{ (relation() ? 'kontakte.relations.editTitle' : 'kontakte.relations.addTitle') | transloco }}</h2>
        <button type="button" class="x" (click)="close.emit()" [attr.aria-label]="'kontakte.editor.cancel' | transloco">
          <jl-icon name="close" [size]="18" />
        </button>
      </header>

      <div class="db">
        @if (relation(); as r) {
          <p class="hint"><b>{{ r.label }}</b> {{ r.otherContactName }}</p>
        } @else {
          <label class="fld">
            <span class="lbl">{{ 'kontakte.relations.label' | transloco }}</span>
            <select [value]="optionKey()" (change)="pickLabel($any($event.target).value)">
              <option value="">{{ 'kontakte.relations.labelPlaceholder' | transloco }}</option>
              @for (g of labelGroups(); track g.category) {
                <optgroup [label]="g.category || ('kontakte.relations.noCategory' | transloco)">
                  @for (o of g.options; track key(o)) {
                    <option [value]="key(o)">{{ o.label }}</option>
                  }
                </optgroup>
              }
            </select>
          </label>
          @if (typesLoaded() && !labelGroups().length) {
            <p class="hint">{{ 'kontakte.relations.noTypes' | transloco }}</p>
          }

          <label class="fld">
            <span class="lbl">{{ 'kontakte.relations.contact' | transloco }}</span>
            <input type="search" [value]="query()" (input)="onSearch($any($event.target).value)"
                   [placeholder]="'kontakte.relations.searchPlaceholder' | transloco" autocomplete="off" />
          </label>
          <div class="results">
            @if (target(); as t) {
              <div class="chosen">
                <jl-icon name="contacts" [size]="15" />
                <span class="cl">{{ t.displayName }}{{ t.subtitle ? ' · ' + t.subtitle : '' }}</span>
                <button type="button" class="clear" (click)="target.set(null)"
                        [attr.aria-label]="'kontakte.relations.clear' | transloco">✕</button>
              </div>
            } @else if (searching()) {
              <p class="hint">{{ 'kontakte.loading' | transloco }}</p>
            } @else if (query().trim().length < 2) {
              <p class="hint">{{ 'kontakte.relations.typeToSearch' | transloco }}</p>
            } @else {
              @for (c of results(); track c.id) {
                <button type="button" class="hit" (click)="target.set(c)">
                  <jl-icon name="contacts" [size]="15" />
                  <span class="cl">{{ c.displayName }}{{ c.subtitle ? ' · ' + c.subtitle : '' }}</span>
                </button>
              } @empty {
                <p class="hint">{{ 'kontakte.relations.noResults' | transloco }}</p>
              }
            }
          </div>
        }

        <label class="fld">
          <span class="lbl">{{ 'kontakte.relations.note' | transloco }}</span>
          <input type="text" [ngModel]="note()" (ngModelChange)="note.set($event)"
                 [placeholder]="'kontakte.relations.noteHint' | transloco" />
        </label>
        @if (error()) { <p class="err">{{ error() }}</p> }
      </div>

      <footer class="df">
        <span class="spacer"></span>
        <button type="button" class="btn" [disabled]="saving()" (click)="close.emit()">
          {{ 'kontakte.editor.cancel' | transloco }}
        </button>
        <button type="button" class="btn primary" [disabled]="saving() || !canSave()" (click)="submit()">
          {{ (relation() ? 'kontakte.relations.saveBtn' : 'kontakte.relations.addBtn') | transloco }}
        </button>
      </footer>
    </div>
  `,
  styles: [`
    :host { position: fixed; inset: 0; z-index: 60; display: grid; place-items: center; }
    .backdrop { position: absolute; inset: 0; background: rgba(6, 14, 22, .5); }
    .dialog {
      position: relative; width: min(560px, 95vw); max-height: 92dvh; display: flex; flex-direction: column;
      background: var(--jl-surface); border: 1px solid var(--jl-line); border-radius: 14px;
      box-shadow: 0 24px 60px rgba(0,0,0,.32); overflow: hidden;
    }
    .dh { flex: none; display: flex; align-items: center; gap: 12px; padding: 14px 18px; border-bottom: 1px solid var(--jl-line); }
    .dh h2 { margin: 0; font-size: 1.05rem; font-weight: 800; flex: 1; }
    .x { display: inline-grid; place-items: center; width: 32px; height: 32px; border-radius: 8px; border: 0; background: transparent; color: var(--jl-ink-soft); cursor: pointer; }
    .x:hover { background: var(--jl-surface-alt); }
    .db { flex: 1 1 auto; min-height: 0; overflow-y: auto; padding: 14px 18px; display: flex; flex-direction: column; gap: 14px; }
    .fld { display: flex; flex-direction: column; gap: 5px; min-width: 0; }
    .lbl { font-size: .72rem; font-weight: 700; color: var(--jl-ink-faint); text-transform: uppercase; letter-spacing: .03em; }
    input, select {
      font: inherit; font-size: .88rem; padding: 8px 10px; border: 1px solid var(--jl-line-strong);
      border-radius: 8px; background: var(--jl-surface); color: var(--jl-ink); width: 100%;
    }
    input:focus, select:focus { outline: none; border-color: var(--jl-blue); }
    .results { display: flex; flex-direction: column; gap: 4px; max-height: 240px; overflow-y: auto; }
    .hit, .chosen { display: flex; align-items: center; gap: 10px; padding: 9px 11px; border-radius: 8px; font-size: .86rem; text-align: left; }
    .hit { background: var(--jl-surface-alt); border: 1px solid transparent; cursor: pointer; color: var(--jl-ink); font: inherit; }
    .hit:hover { border-color: var(--jl-blue); }
    .chosen { background: color-mix(in srgb, var(--jl-blue) 10%, transparent); border: 1px solid var(--jl-blue); }
    .cl { flex: 1; min-width: 0; }
    .clear { border: 0; background: none; color: var(--jl-ink-soft); cursor: pointer; font-size: .9rem; }
    .hint { margin: 0; color: var(--jl-ink-soft); font-size: .82rem; }
    .err { margin: 0; color: var(--jl-red); font-size: .82rem; font-weight: 600; }
    .df { flex: none; display: flex; align-items: center; gap: 10px; padding: 12px 18px; border-top: 1px solid var(--jl-line); }
    .spacer { flex: 1; }
    .btn { font: inherit; font-size: .85rem; font-weight: 650; padding: 8px 16px; border-radius: 8px; border: 1px solid var(--jl-line-strong); background: var(--jl-surface); color: var(--jl-ink); cursor: pointer; }
    .btn:hover:not(:disabled) { border-color: var(--jl-blue); }
    .btn:disabled { opacity: .5; cursor: default; }
    .btn.primary { background: var(--jl-blue); border-color: var(--jl-blue); color: #fff; }
  `],
})
export class ContactRelationEditorComponent implements OnInit {
  /** The relationship whose note is edited; null when a new relationship is recorded. */
  readonly relation = input<ContactRelation | null>(null);
  readonly saving = input<boolean>(false);
  /** The server's rejection message, shown instead of a generic failure. */
  readonly error = input<string | null>(null);
  readonly save = output<ContactRelationWrite>();
  readonly close = output<void>();

  private readonly contacts = inject(ContactsService);

  protected readonly labelGroups = signal<LabelGroup[]>([]);
  protected readonly typesLoaded = signal(false);
  protected readonly option = signal<RelationLabelOption | null>(null);
  protected readonly query = signal('');
  protected readonly results = signal<ContactOverview[]>([]);
  protected readonly target = signal<ContactOverview | null>(null);
  protected readonly searching = signal(false);
  protected readonly note = signal('');

  protected readonly optionKey = computed(() => {
    const o = this.option();
    return o ? this.key(o) : '';
  });
  protected readonly canSave = computed(() => !!this.relation() || (!!this.option() && !!this.target()));

  private searchTimer: ReturnType<typeof setTimeout> | null = null;
  private searchSeq = 0;

  ngOnInit(): void {
    const r = this.relation();
    if (r) {
      this.note.set(r.note);
      return; // editing a note needs neither the catalogue nor the contact picker
    }
    this.contacts.relationTypes(true).subscribe((types) => {
      this.labelGroups.set(groupLabels(types));
      this.typesLoaded.set(true);
    });
  }

  /** Stable key of a label option (type + direction), used as the select's option value. */
  protected key(o: RelationLabelOption): string {
    return `${o.typeId}|${o.reverse ? 'r' : 'f'}`;
  }

  protected pickLabel(key: string): void {
    const all = this.labelGroups().flatMap((g) => g.options);
    this.option.set(all.find((o) => this.key(o) === key) ?? null);
  }

  /** Debounces keystrokes into a contact search (250ms), like the other contact pickers. */
  protected onSearch(value: string): void {
    this.query.set(value);
    this.target.set(null);
    if (this.searchTimer) {
      clearTimeout(this.searchTimer);
    }
    if (value.trim().length < 2) {
      this.results.set([]);
      this.searching.set(false);
      return;
    }
    this.searching.set(true);
    this.searchTimer = setTimeout(() => {
      const seq = ++this.searchSeq;
      this.contacts.searchContacts(value.trim()).subscribe((hits) => {
        if (seq === this.searchSeq) {
          this.results.set(hits);
          this.searching.set(false);
        }
      });
    }, 250);
  }

  protected submit(): void {
    if (!this.canSave() || this.saving()) {
      return;
    }
    const o = this.option();
    const t = this.target();
    this.save.emit({
      typeId: o?.typeId ?? '',
      reverse: !!o?.reverse,
      otherContactId: t?.id ?? '',
      note: this.note().trim(),
    });
  }
}

/**
 * Both labels of every type as picker entries, grouped by category: `labelFrom` in the stored
 * direction and `labelTo` as its reverse. A symmetric type reads the same both ways and therefore
 * contributes one entry only.
 */
function groupLabels(types: ContactRelationType[]): LabelGroup[] {
  const groups: LabelGroup[] = [];
  for (const t of types) {
    if (!t.id) { continue; }
    const options: RelationLabelOption[] = [];
    if (t.labelFrom) {
      options.push({ typeId: t.id, label: t.labelFrom, category: t.category, reverse: false });
    }
    if (!t.symmetric && t.labelTo && t.labelTo !== t.labelFrom) {
      options.push({ typeId: t.id, label: t.labelTo, category: t.category, reverse: true });
    }
    if (!options.length) { continue; }
    let group = groups.find((g) => g.category === t.category);
    if (!group) {
      group = { category: t.category, options: [] };
      groups.push(group);
    }
    group.options.push(...options);
  }
  return groups;
}
