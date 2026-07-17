import { ChangeDetectionStrategy, Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { CasesService } from './cases.service';
import { CaseGroup, CaseUserRef, CaseWrite } from './case.models';
import { CustomField, CustomFieldsService } from '../settings/custom-fields.service';

/**
 * Modal editor for a case's master data (Stammdaten). Works on a clone of the full case DTO (from
 * GET /v2/cases/{id}), so fields not shown in the form (custom1-3, group, externalId) round-trip
 * unchanged on save. Anwalt/Sachbearbeiter are user dropdowns (their stored value is the login
 * principal id). Emits the working copy on save, or nothing on cancel — the parent does the REST
 * call. Rendered only while open (mounted via @if).
 */
@Component({
  selector: 'jl-case-editor',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, TranslocoModule, IconComponent],
  template: `
    <div class="backdrop" (click)="close.emit()"></div>
    <div class="dialog" role="dialog" aria-modal="true">
      <header class="dh">
        <h2>{{ (caseData()?.id ? 'akten.editor.editTitle' : 'akten.editor.newTitle') | transloco }}</h2>
        <button type="button" class="x" (click)="close.emit()" [attr.aria-label]="'akten.editor.cancel' | transloco">
          <jl-icon name="close" [size]="18" />
        </button>
      </header>

      <div class="db">
        <h4 class="grp">{{ 'akten.editor.group.base' | transloco }}</h4>
        <div class="grid">
          <label class="fld sm">
            <span class="lbl">{{ 'akten.meta.fileNumber' | transloco }}</span>
            <input type="text" [ngModel]="strVal('fileNumber')" (ngModelChange)="upd('fileNumber', $event)" />
          </label>
          <label class="fld">
            <span class="lbl">{{ 'akten.editor.field.name' | transloco }}</span>
            <input type="text" [ngModel]="strVal('name')" (ngModelChange)="upd('name', $event)" />
          </label>
          <label class="fld wide">
            <span class="lbl">{{ 'akten.meta.reason' | transloco }}</span>
            <input type="text" [ngModel]="strVal('reason')" (ngModelChange)="upd('reason', $event)" />
          </label>
          <label class="fld wide">
            <span class="lbl">{{ 'akten.meta.subjectField' | transloco }}</span>
            <input type="text" [ngModel]="strVal('subjectField')" (ngModelChange)="upd('subjectField', $event)" />
          </label>
        </div>

        <h4 class="grp">{{ 'akten.editor.group.responsible' | transloco }}</h4>
        <div class="grid">
          <label class="fld">
            <span class="lbl">{{ 'akten.meta.lawyer' | transloco }}</span>
            <select [ngModel]="strVal('lawyer')" (ngModelChange)="upd('lawyer', $event)">
              <option value="">—</option>
              @for (u of userOptions('lawyer'); track u.principalId) {
                <option [value]="u.principalId">{{ u.displayName }}</option>
              }
            </select>
          </label>
          <label class="fld">
            <span class="lbl">{{ 'akten.meta.assistant' | transloco }}</span>
            <select [ngModel]="strVal('assistant')" (ngModelChange)="upd('assistant', $event)">
              <option value="">—</option>
              @for (u of userOptions('assistant'); track u.principalId) {
                <option [value]="u.principalId">{{ u.displayName }}</option>
              }
            </select>
          </label>
          <label class="fld">
            <span class="lbl">{{ 'akten.editor.field.ownerGroup' | transloco }}</span>
            <select [ngModel]="strVal('group')" (ngModelChange)="upd('group', $event)">
              <option value="">—</option>
              @for (g of groupOptions(); track g.id) {
                <option [value]="g.name">{{ g.name }}</option>
              }
            </select>
          </label>
        </div>

        <h4 class="grp">{{ 'akten.editor.group.claim' | transloco }}</h4>
        <div class="grid">
          <label class="fld">
            <span class="lbl">{{ 'akten.editor.field.claimNumber' | transloco }}</span>
            <input type="text" [ngModel]="strVal('claimNumber')" (ngModelChange)="upd('claimNumber', $event)" />
          </label>
          <label class="fld sm">
            <span class="lbl">{{ 'akten.meta.claimValue' | transloco }}</span>
            <input type="number" step="0.01" [ngModel]="f().claimValue ?? null" (ngModelChange)="upd('claimValue', toNum($event))" />
          </label>
          <label class="chk">
            <input type="checkbox" [ngModel]="f().archived === 1" (ngModelChange)="upd('archived', $event ? 1 : 0)" />
            {{ 'akten.editor.field.archived' | transloco }}
          </label>
        </div>

        @if (customFields().length) {
          <h4 class="grp">{{ 'akten.editor.group.custom' | transloco }}</h4>
          <div class="grid">
            @for (cf of customFields(); track cf.key) {
              <label class="fld">
                <span class="lbl">{{ cf.label }}</span>
                <input type="text" [ngModel]="strVal(cf.key)" (ngModelChange)="upd(cf.key, $event)" />
              </label>
            }
          </div>
        }

        <h4 class="grp">{{ 'akten.note' | transloco }}</h4>
        <div class="grid">
          <label class="fld wide">
            <span class="lbl">{{ 'akten.note' | transloco }}</span>
            <textarea rows="4" [ngModel]="strVal('notice')" (ngModelChange)="upd('notice', $event)"></textarea>
          </label>
        </div>
      </div>

      <footer class="df">
        <span class="spacer"></span>
        <button type="button" class="btn" (click)="close.emit()">{{ 'akten.editor.cancel' | transloco }}</button>
        <button type="button" class="btn primary" [disabled]="!canSave()" (click)="submit()">
          {{ 'akten.editor.save' | transloco }}
        </button>
      </footer>
    </div>
  `,
  styles: [`
    :host { position: fixed; inset: 0; z-index: 60; display: grid; place-items: center; }
    .backdrop { position: absolute; inset: 0; background: rgba(6, 14, 22, .5); }
    .dialog {
      position: relative; width: min(680px, 95vw); max-height: 92dvh; display: flex; flex-direction: column;
      background: var(--jl-surface); border: 1px solid var(--jl-line); border-radius: 14px;
      box-shadow: 0 24px 60px rgba(0,0,0,.32); overflow: hidden;
    }
    .dh { flex: none; display: flex; align-items: center; gap: 12px; padding: 14px 18px; border-bottom: 1px solid var(--jl-line); }
    .dh h2 { margin: 0; font-size: 1.05rem; font-weight: 800; flex: 1; }
    .x { display: inline-grid; place-items: center; width: 32px; height: 32px; border-radius: 8px; border: 0; background: transparent; color: var(--jl-ink-soft); cursor: pointer; }
    .x:hover { background: var(--jl-surface-alt); }
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
    @media (max-width: 560px) { .grid { grid-template-columns: 1fr; } }
  `],
})
export class CaseEditorComponent implements OnInit {
  /** The case to edit (raw DTO), or null to create a new one. */
  readonly caseData = input<CaseWrite | null>(null);

  readonly save = output<CaseWrite>();
  readonly close = output<void>();

  private readonly cases = inject(CasesService);
  private readonly customFieldsSvc = inject(CustomFieldsService);

  /** Working copy — a clone of the input case (edit) or empty (create). */
  protected readonly f = signal<CaseWrite>({});
  protected readonly users = signal<CaseUserRef[]>([]);
  protected readonly groups = signal<CaseGroup[]>([]);
  /** Configured (non-empty) custom fields for cases. */
  protected readonly customFields = signal<CustomField[]>([]);

  protected readonly canSave = computed(() => !!(this.f().name ?? '').trim());

  ngOnInit(): void {
    const c = this.caseData();
    if (c) {
      this.f.set({ ...c });
    }
    this.cases.users().subscribe((u) => this.users.set(u));
    this.cases.allGroups().subscribe((g) => this.groups.set(g));
    this.customFieldsSvc.labels('case').subscribe((l) => this.customFields.set(this.customFieldsSvc.configuredFields(l)));
  }

  /**
   * Owner-group dropdown options — all groups, plus the case's current owner group if it is no
   * longer among them (so editing never drops the value). The stored value is the group name.
   */
  protected groupOptions(): CaseGroup[] {
    const groups = this.groups();
    const current = (this.f().group ?? '').trim();
    if (current && !groups.some((g) => g.name === current)) {
      return [{ id: current, name: current, abbreviation: '' }, ...groups];
    }
    return groups;
  }

  /**
   * The user dropdown options for a field — the login-enabled users, plus the case's current value
   * if it is no longer among them (e.g. a since-disabled user), so editing never drops the value.
   */
  protected userOptions(key: 'lawyer' | 'assistant'): CaseUserRef[] {
    const users = this.users();
    const current = (this.f()[key] ?? '').trim();
    if (current && !users.some((u) => u.principalId === current)) {
      return [{ principalId: current, displayName: current }, ...users];
    }
    return users;
  }

  protected strVal(key: keyof CaseWrite): string {
    const v = this.f()[key];
    return v == null ? '' : String(v);
  }

  protected toNum(value: unknown): number {
    const n = Number(value);
    return Number.isFinite(n) ? n : 0;
  }

  protected upd<K extends keyof CaseWrite>(key: K, value: CaseWrite[K]): void {
    this.f.update((cur) => ({ ...cur, [key]: value }));
  }

  protected submit(): void {
    if (this.canSave()) {
      this.save.emit(this.f());
    }
  }
}
