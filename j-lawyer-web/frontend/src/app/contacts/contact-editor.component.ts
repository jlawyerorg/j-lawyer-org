import { ChangeDetectionStrategy, Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { ContactData } from './contact.models';
import { OptionGroupsService } from './option-groups.service';
import { CustomFieldsService } from '../settings/custom-fields.service';

type FieldType = 'text' | 'textarea' | 'flag' | 'select';
interface SelectOption { value: string; labelKey: string; }
interface FieldDef {
  key: keyof ContactData;
  type?: FieldType;
  /** Narrow field (short values like Nr./PLZ/dates). */
  sm?: boolean;
  /** Configurable value list (option group) to offer as datalist suggestions (free text stays allowed). */
  group?: string;
  /** Fixed, non-editable choices for a `select` field. */
  select?: SelectOption[];
  /** Preselected value for a `select` field when the contact has none. */
  defaultValue?: string;
  /** Explicit label text (overrides the `kontakte.field.<key>` translation), e.g. a configured custom-field label. */
  labelText?: string;
}
/** A titled cluster of fields within a tab (mirrors the desktop AddressPanel's TitledBorder groups). */
interface GroupDef { titleKey?: string; fields: FieldDef[]; }
interface TabDef { id: string; groups: GroupDef[]; }

/**
 * Gender is a discrete, forced choice (not a free-text option group). Values are the server-side
 * AddressBean.GENDER_* constants; a missing/unknown value maps to UNDEFINED (as in the desktop).
 */
const GENDER_OPTIONS: SelectOption[] = [
  { value: 'FEMALE', labelKey: 'kontakte.editor.gender.FEMALE' },
  { value: 'MALE', labelKey: 'kontakte.editor.gender.MALE' },
  { value: 'OTHER', labelKey: 'kontakte.editor.gender.OTHER' },
  { value: 'LEGALENTITY', labelKey: 'kontakte.editor.gender.LEGALENTITY' },
  { value: 'UNDEFINED', labelKey: 'kontakte.editor.gender.UNDEFINED' },
];

/**
 * Editor tabs, oriented on the desktop AddressPanel (Person / Organisation / Kontakt /
 * Bank & Versicherung / Textbausteine / Eigene), each split into titled field groups so the ~50
 * fields stay scannable. External ids are intentionally omitted (integration keys) but still
 * round-trip because the working copy is a full clone. Field labels resolve via
 * {@code kontakte.field.<key>}, group titles via {@code kontakte.editor.group.<key>}, tab titles
 * via {@code kontakte.editor.tab.<id>}.
 */
const TABS: TabDef[] = [
  {
    id: 'person',
    groups: [
      {
        titleKey: 'kontakte.editor.group.name',
        fields: [
          { key: 'title', sm: true, group: 'address.title' }, { key: 'titleInAddress', sm: true, group: 'address.titleinaddress' },
          { key: 'firstName' }, { key: 'firstName2' }, { key: 'name' }, { key: 'birthName' },
          { key: 'gender', type: 'select', select: GENDER_OPTIONS, defaultValue: 'UNDEFINED' }, { key: 'initials', sm: true },
        ],
      },
      {
        titleKey: 'kontakte.editor.group.degree',
        fields: [{ key: 'degreePrefix', group: 'address.degreeprefix' }, { key: 'degreeSuffix', group: 'address.degreesuffix' }],
      },
      {
        titleKey: 'kontakte.editor.group.personal',
        fields: [
          { key: 'nationality', group: 'address.nationality' }, { key: 'profession', group: 'address.profession' }, { key: 'role', group: 'address.role' },
          { key: 'birthDate', sm: true }, { key: 'placeOfBirth' }, { key: 'dateOfDeath', sm: true },
        ],
      },
    ],
  },
  {
    id: 'org',
    groups: [
      {
        titleKey: 'kontakte.editor.group.legalPerson',
        fields: [
          { key: 'company' }, { key: 'department' }, { key: 'legalForm', group: 'address.legalform' },
          { key: 'companyRegistrationNumber' }, { key: 'companyRegistrationCourt' },
        ],
      },
      { titleKey: 'kontakte.editor.group.einvoice', fields: [{ key: 'leitwegId' }] },
    ],
  },
  {
    id: 'contact',
    groups: [
      {
        titleKey: 'kontakte.editor.group.address',
        fields: [
          { key: 'street' }, { key: 'streetNumber', sm: true }, { key: 'adjunct' },
          { key: 'zipCode', sm: true }, { key: 'city' }, { key: 'district' },
          { key: 'country', sm: true, group: 'address.country' }, { key: 'state', group: 'address.state' },
        ],
      },
      {
        titleKey: 'kontakte.editor.group.channels',
        fields: [
          { key: 'phone' }, { key: 'mobile' }, { key: 'fax' },
          { key: 'email' }, { key: 'emailHome' }, { key: 'emailMisc' }, { key: 'website' },
        ],
      },
      { titleKey: 'kontakte.editor.group.bea', fields: [{ key: 'beaSafeId' }] },
    ],
  },
  {
    id: 'bank',
    groups: [
      {
        titleKey: 'kontakte.editor.group.bank',
        fields: [
          { key: 'bankName' }, { key: 'bankCode', sm: true }, { key: 'bankAccount' }, { key: 'bankAccountOwner' },
          { key: 'sepaReference' }, { key: 'sepaSince', sm: true },
        ],
      },
      {
        titleKey: 'kontakte.editor.group.legalProtection',
        fields: [{ key: 'legalProtection', type: 'flag' }, { key: 'insuranceName' }, { key: 'insuranceNumber' }, { key: 'insurant' }],
      },
      {
        titleKey: 'kontakte.editor.group.trafficProtection',
        fields: [{ key: 'trafficLegalProtection', type: 'flag' }, { key: 'trafficInsuranceName' }, { key: 'trafficInsuranceNumber' }, { key: 'trafficInsurant' }],
      },
      {
        titleKey: 'kontakte.editor.group.motorProtection',
        fields: [{ key: 'motorLegalProtection', type: 'flag' }, { key: 'motorInsuranceName' }, { key: 'motorInsuranceNumber' }, { key: 'motorInsurant' }],
      },
      {
        titleKey: 'kontakte.editor.group.tax',
        fields: [{ key: 'vatId' }, { key: 'tin' }, { key: 'taxDeduction', type: 'flag' }],
      },
    ],
  },
  {
    id: 'letter',
    groups: [{ fields: [{ key: 'salutation', group: 'address.salutation' }, { key: 'complimentaryClose', group: 'address.complimentaryclose' }] }],
  },
  {
    id: 'misc',
    groups: [
      { titleKey: 'kontakte.editor.group.note', fields: [{ key: 'notice', type: 'textarea' }] },
      { titleKey: 'kontakte.editor.group.custom', fields: [{ key: 'custom1' }, { key: 'custom2' }, { key: 'custom3' }] },
    ],
  },
];

/**
 * Modal editor for a contact (create or edit), split into tabs of titled field groups (see
 * {@link TABS}). Works on a clone of the full contact DTO, so fields on tabs the user never opens
 * round-trip unchanged on save. The dialog keeps a fixed height (only the body scrolls), so the tab
 * bar and buttons stay put when switching tabs. Emits the working copy on save, the id on delete,
 * or nothing on cancel — the parent does the REST call. Rendered only while open (mounted via @if).
 */
@Component({
  selector: 'jl-contact-editor',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, TranslocoModule, IconComponent],
  template: `
    <div class="backdrop" (click)="close.emit()"></div>
    <div class="dialog" role="dialog" aria-modal="true">
      <header class="dh">
        <h2>{{ (contact() ? 'kontakte.editor.editTitle' : 'kontakte.editor.newTitle') | transloco }}</h2>
        <button type="button" class="x" (click)="close.emit()" [attr.aria-label]="'kontakte.editor.cancel' | transloco">
          <jl-icon name="close" [size]="18" />
        </button>
      </header>

      <div class="tabbar" role="tablist">
        @for (t of tabs; track t.id) {
          <button type="button" class="tab" [class.on]="activeTab() === t.id" (click)="activeTab.set(t.id)">
            {{ 'kontakte.editor.tab.' + t.id | transloco }}
          </button>
        }
      </div>

      <div class="db">
        @for (g of activeGroups(); track $index) {
          @if (g.titleKey) { <h4 class="grp">{{ g.titleKey | transloco }}</h4> }
          <div class="grid">
            @for (fd of g.fields; track fd.key) {
              @if (fd.type === 'flag') {
                <label class="chk">
                  <input type="checkbox" [ngModel]="flagVal(fd.key)" (ngModelChange)="upd(fd.key, $event ? 1 : 0)" />
                  {{ fd.labelText ? fd.labelText : ('kontakte.field.' + fd.key | transloco) }}
                </label>
              } @else if (fd.type === 'textarea') {
                <label class="fld wide">
                  <span class="lbl">{{ fd.labelText ? fd.labelText : ('kontakte.field.' + fd.key | transloco) }}</span>
                  <textarea rows="3" [ngModel]="strVal(fd.key)" (ngModelChange)="upd(fd.key, $event)"></textarea>
                </label>
              } @else if (fd.type === 'select') {
                <label class="fld" [class.sm]="fd.sm">
                  <span class="lbl">{{ fd.labelText ? fd.labelText : ('kontakte.field.' + fd.key | transloco) }}</span>
                  <select [ngModel]="strVal(fd.key) || fd.defaultValue" (ngModelChange)="upd(fd.key, $event)">
                    @for (o of fd.select; track o.value) {
                      <option [value]="o.value">{{ o.labelKey | transloco }}</option>
                    }
                  </select>
                </label>
              } @else if (fd.group) {
                <label class="fld" [class.sm]="fd.sm">
                  <span class="lbl">{{ fd.labelText ? fd.labelText : ('kontakte.field.' + fd.key | transloco) }}</span>
                  <input type="text" [attr.list]="'dl-' + fd.key" autocomplete="off"
                         [ngModel]="strVal(fd.key)" (ngModelChange)="upd(fd.key, $event)" />
                  <datalist [id]="'dl-' + fd.key">
                    @for (opt of options(fd.group); track opt) { <option [value]="opt"></option> }
                  </datalist>
                </label>
              } @else {
                <label class="fld" [class.sm]="fd.sm">
                  <span class="lbl">{{ fd.labelText ? fd.labelText : ('kontakte.field.' + fd.key | transloco) }}</span>
                  <input type="text" [ngModel]="strVal(fd.key)" (ngModelChange)="upd(fd.key, $event)" />
                </label>
              }
            }
          </div>
        }
      </div>

      <footer class="df">
        @if (contact()?.id) {
          <button type="button" class="btn danger" (click)="remove.emit(contact()!.id!)">
            <jl-icon name="trash" [size]="15" /> {{ 'kontakte.editor.delete' | transloco }}
          </button>
        }
        <span class="spacer"></span>
        <button type="button" class="btn" (click)="close.emit()">{{ 'kontakte.editor.cancel' | transloco }}</button>
        <button type="button" class="btn primary" [disabled]="!canSave()" (click)="submit()">
          {{ 'kontakte.editor.save' | transloco }}
        </button>
      </footer>
    </div>
  `,
  styles: [`
    :host { position: fixed; inset: 0; z-index: 60; display: grid; place-items: center; }
    .backdrop { position: absolute; inset: 0; background: rgba(6, 14, 22, .5); }
    .dialog {
      position: relative; width: min(820px, 95vw); height: min(680px, 92dvh); display: flex; flex-direction: column;
      background: var(--jl-surface); border: 1px solid var(--jl-line); border-radius: 14px;
      box-shadow: 0 24px 60px rgba(0,0,0,.32); overflow: hidden;
    }
    .dh { flex: none; display: flex; align-items: center; gap: 12px; padding: 14px 18px; border-bottom: 1px solid var(--jl-line); }
    .dh h2 { margin: 0; font-size: 1.05rem; font-weight: 800; flex: 1; }
    .x { display: inline-grid; place-items: center; width: 32px; height: 32px; border-radius: 8px; border: 0; background: transparent; color: var(--jl-ink-soft); cursor: pointer; }
    .x:hover { background: var(--jl-surface-alt); }
    .tabbar { flex: none; display: flex; flex-wrap: wrap; gap: 2px 4px; padding: 6px 12px; border-bottom: 1px solid var(--jl-line); }
    .tab { flex: none; padding: 8px 13px; font: inherit; font-size: .8rem; font-weight: 650; color: var(--jl-ink-soft); background: none; border: 0; border-radius: 7px; cursor: pointer; white-space: nowrap; }
    .tab:hover { color: var(--jl-ink); background: var(--jl-surface-alt); }
    .tab.on { color: var(--jl-blue); background: color-mix(in srgb, var(--jl-blue) 12%, transparent); }
    .db { flex: 1 1 auto; min-height: 0; overflow-y: auto; padding: 4px 18px 18px; }
    .grp { margin: 16px 0 8px; font-size: .72rem; font-weight: 800; text-transform: uppercase; letter-spacing: .05em; color: var(--jl-ink-faint); border-bottom: 1px solid var(--jl-line); padding-bottom: 5px; }
    .db > .grp:first-child { margin-top: 10px; }
    .grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
    .fld { display: flex; flex-direction: column; gap: 5px; min-width: 0; }
    .fld.wide, .chk { grid-column: 1 / -1; }
    .lbl { font-size: .72rem; font-weight: 700; color: var(--jl-ink-faint); text-transform: uppercase; letter-spacing: .03em; }
    input, textarea, select {
      font: inherit; font-size: .88rem; padding: 8px 10px; border: 1px solid var(--jl-line-strong);
      border-radius: 8px; background: var(--jl-surface); color: var(--jl-ink); width: 100%;
    }
    input:focus, textarea:focus, select:focus { outline: none; border-color: var(--jl-blue); }
    textarea { resize: vertical; }
    .chk { display: flex; align-items: center; gap: 8px; font-size: .86rem; font-weight: 600; }
    .chk input { width: auto; }
    .df { flex: none; display: flex; align-items: center; gap: 10px; padding: 12px 18px; border-top: 1px solid var(--jl-line); }
    .spacer { flex: 1; }
    .btn { font: inherit; font-size: .85rem; font-weight: 650; padding: 8px 16px; border-radius: 8px; border: 1px solid var(--jl-line-strong); background: var(--jl-surface); color: var(--jl-ink); cursor: pointer; display: inline-flex; align-items: center; gap: 6px; }
    .btn:hover:not(:disabled) { border-color: var(--jl-blue); }
    .btn:disabled { opacity: .5; cursor: default; }
    .btn.primary { background: var(--jl-blue); border-color: var(--jl-blue); color: #fff; }
    .btn.danger { border-color: transparent; color: var(--jl-red); }
    .btn.danger:hover { background: color-mix(in srgb, var(--jl-red) 12%, transparent); border-color: var(--jl-red); }
    @media (max-width: 560px) { .grid { grid-template-columns: 1fr; } }
  `],
})
export class ContactEditorComponent implements OnInit {
  /** The contact to edit (raw DTO), or null to create a new one. */
  readonly contact = input<ContactData | null>(null);

  readonly save = output<ContactData>();
  readonly remove = output<string>();
  readonly close = output<void>();

  private readonly optionGroups = inject(OptionGroupsService);
  private readonly customFieldsSvc = inject(CustomFieldsService);

  protected readonly tabs = TABS;
  protected readonly activeTab = signal<string>(TABS[0].id);
  /** Working copy — a clone of the input contact (edit) or empty (create). */
  protected readonly f = signal<ContactData>({});
  /** Configured (non-empty) custom-field labels for addresses, keyed by slot. */
  private readonly customLabels = signal<Record<string, string>>({});

  protected readonly activeGroups = computed(() => {
    const groups = TABS.find((t) => t.id === this.activeTab())?.groups ?? [];
    const labels = this.customLabels();
    // Inject the configured labels onto custom1/2/3 and drop custom fields with no configured label.
    return groups
      .map((g) => ({
        ...g,
        fields: g.fields
          .filter((fd) => !this.isCustom(fd.key) || labels[fd.key])
          .map((fd) => (this.isCustom(fd.key) ? { ...fd, labelText: labels[fd.key] } : fd)),
      }))
      .filter((g) => g.fields.length > 0);
  });

  private isCustom(key: string): boolean {
    return key === 'custom1' || key === 'custom2' || key === 'custom3';
  }

  protected readonly canSave = computed(() => {
    const v = this.f();
    return !!((v.company ?? '').trim() || (v.name ?? '').trim() || (v.firstName ?? '').trim());
  });

  ngOnInit(): void {
    const c = this.contact();
    if (c) {
      this.f.set({ ...c });
    }
    this.customFieldsSvc.labels('address').subscribe((l) => {
      const map: Record<string, string> = {};
      for (const f of this.customFieldsSvc.configuredFields(l)) { map[f.key] = f.label; }
      this.customLabels.set(map);
    });
  }

  /** The configurable value list for a combo field's option group (datalist suggestions). */
  protected options(group: string): string[] {
    return this.optionGroups.get(group)();
  }

  /** Current string value of a field (empty string for unset / numeric fields). */
  protected strVal(key: keyof ContactData): string {
    const v = this.f()[key];
    return v == null ? '' : String(v);
  }

  /** Current boolean value of a 0/1 flag field. */
  protected flagVal(key: keyof ContactData): boolean {
    return this.f()[key] === 1;
  }

  /** Updates a single field in the working copy. */
  protected upd<K extends keyof ContactData>(key: K, value: ContactData[K]): void {
    this.f.update((cur) => ({ ...cur, [key]: value }));
  }

  protected submit(): void {
    if (this.canSave()) {
      this.save.emit(this.f());
    }
  }
}
