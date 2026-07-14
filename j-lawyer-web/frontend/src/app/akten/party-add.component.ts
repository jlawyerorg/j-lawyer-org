import { ChangeDetectionStrategy, Component, computed, inject, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { CasesService } from './cases.service';
import { ContactRef, PartyTypeOption } from './case.models';

/**
 * Modal dialog to add a party (Beteiligter) to a case: search an existing contact (address book)
 * and pick an involvement type (from the configured party types). Emits { addressId, involvementType }
 * on confirm — the parent does the REST call and reloads. Rendered only while open (mounted via @if).
 */
@Component({
  selector: 'jl-party-add',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, TranslocoModule, IconComponent],
  template: `
    <div class="backdrop" (click)="close.emit()"></div>
    <div class="dialog" role="dialog" aria-modal="true">
      <header class="dh">
        <h2>{{ 'akten.party.addTitle' | transloco }}</h2>
        <button type="button" class="x" (click)="close.emit()" [attr.aria-label]="'akten.editor.cancel' | transloco">
          <jl-icon name="close" [size]="18" />
        </button>
      </header>

      <div class="db">
        <label class="fld">
          <span class="lbl">{{ 'akten.party.involvementType' | transloco }}</span>
          <select [ngModel]="involvementType()" (ngModelChange)="involvementType.set($event)">
            <option value="">—</option>
            @for (t of partyTypes(); track t.name) {
              <option [value]="t.name">{{ t.name }}</option>
            }
          </select>
        </label>

        <label class="fld">
          <span class="lbl">{{ 'akten.party.contact' | transloco }}</span>
          <div class="search">
            <jl-icon name="search" [size]="14" />
            <input type="search" [ngModel]="query()" (ngModelChange)="onSearch($event)"
                   [placeholder]="'akten.party.searchPlaceholder' | transloco" autocomplete="off" />
          </div>
        </label>

        <div class="results">
          @if (selected(); as s) {
            <div class="chosen">
              <jl-icon name="contacts" [size]="15" />
              <span class="cl">{{ s.label }}</span>
              <button type="button" class="clear" (click)="clearSelection()" [attr.aria-label]="'akten.party.clear' | transloco">✕</button>
            </div>
          } @else if (searching()) {
            <p class="muted">{{ 'akten.loading' | transloco }}</p>
          } @else if (query().trim().length < 2) {
            <p class="muted">{{ 'akten.party.typeToSearch' | transloco }}</p>
          } @else {
            @for (r of results(); track r.id) {
              <button type="button" class="hit" (click)="pick(r)">
                <jl-icon name="contacts" [size]="15" />
                <span class="cl">{{ r.label }}</span>
              </button>
            } @empty {
              <p class="muted">{{ 'akten.party.noResults' | transloco }}</p>
            }
          }
        </div>
      </div>

      <footer class="df">
        <span class="spacer"></span>
        <button type="button" class="btn" (click)="close.emit()">{{ 'akten.editor.cancel' | transloco }}</button>
        <button type="button" class="btn primary" [disabled]="!canSave()" (click)="submit()">
          {{ 'akten.party.add' | transloco }}
        </button>
      </footer>
    </div>
  `,
  styles: [`
    :host { position: fixed; inset: 0; z-index: 60; display: grid; place-items: center; }
    .backdrop { position: absolute; inset: 0; background: rgba(6, 14, 22, .5); }
    .dialog {
      position: relative; width: min(520px, 95vw); max-height: 92dvh; display: flex; flex-direction: column;
      background: var(--jl-surface); border: 1px solid var(--jl-line); border-radius: 14px;
      box-shadow: 0 24px 60px rgba(0,0,0,.32); overflow: hidden;
    }
    .dh { flex: none; display: flex; align-items: center; gap: 12px; padding: 14px 18px; border-bottom: 1px solid var(--jl-line); }
    .dh h2 { margin: 0; font-size: 1.05rem; font-weight: 800; flex: 1; }
    .x { display: inline-grid; place-items: center; width: 32px; height: 32px; border-radius: 8px; border: 0; background: transparent; color: var(--jl-ink-soft); cursor: pointer; }
    .x:hover { background: var(--jl-surface-alt); }
    .db { flex: 1 1 auto; min-height: 0; overflow-y: auto; padding: 14px 18px; display: flex; flex-direction: column; gap: 14px; }
    .fld { display: flex; flex-direction: column; gap: 5px; }
    .lbl { font-size: .72rem; font-weight: 700; color: var(--jl-ink-faint); text-transform: uppercase; letter-spacing: .03em; }
    select, input { font: inherit; font-size: .88rem; padding: 8px 10px; border: 1px solid var(--jl-line-strong); border-radius: 8px; background: var(--jl-surface); color: var(--jl-ink); width: 100%; }
    select:focus, input:focus { outline: none; border-color: var(--jl-blue); }
    .search { display: flex; align-items: center; gap: 8px; border: 1px solid var(--jl-line-strong); border-radius: 8px; padding: 0 10px; color: var(--jl-ink-faint); }
    .search:focus-within { border-color: var(--jl-blue); }
    .search input { border: 0; padding: 8px 0; }
    .results { display: flex; flex-direction: column; gap: 4px; min-height: 80px; max-height: 260px; overflow-y: auto; }
    .hit, .chosen { display: flex; align-items: center; gap: 10px; padding: 9px 11px; border-radius: 8px; font-size: .86rem; text-align: left; }
    .hit { background: var(--jl-surface-alt); border: 1px solid transparent; cursor: pointer; color: var(--jl-ink); font: inherit; }
    .hit:hover { border-color: var(--jl-blue); }
    .chosen { background: color-mix(in srgb, var(--jl-blue) 10%, transparent); border: 1px solid var(--jl-blue); }
    .cl { flex: 1; min-width: 0; }
    .clear { border: 0; background: none; color: var(--jl-ink-soft); cursor: pointer; font-size: .9rem; }
    .muted { color: var(--jl-ink-faint); font-size: .82rem; padding: 8px 2px; }
    .df { flex: none; display: flex; align-items: center; gap: 10px; padding: 12px 18px; border-top: 1px solid var(--jl-line); }
    .spacer { flex: 1; }
    .btn { font: inherit; font-size: .85rem; font-weight: 650; padding: 8px 16px; border-radius: 8px; border: 1px solid var(--jl-line-strong); background: var(--jl-surface); color: var(--jl-ink); cursor: pointer; }
    .btn:hover:not(:disabled) { border-color: var(--jl-blue); }
    .btn:disabled { opacity: .5; cursor: default; }
    .btn.primary { background: var(--jl-blue); border-color: var(--jl-blue); color: #fff; }
  `],
})
export class PartyAddComponent implements OnInit {
  readonly save = output<{ addressId: string; involvementType: string }>();
  readonly close = output<void>();

  private readonly cases = inject(CasesService);

  protected readonly partyTypes = signal<PartyTypeOption[]>([]);
  protected readonly involvementType = signal('');
  protected readonly query = signal('');
  protected readonly results = signal<ContactRef[]>([]);
  protected readonly selected = signal<ContactRef | null>(null);
  protected readonly searching = signal(false);

  protected readonly canSave = computed(() => !!this.selected() && !!this.involvementType().trim());

  private searchTimer: ReturnType<typeof setTimeout> | null = null;
  private searchSeq = 0;

  ngOnInit(): void {
    this.cases.partyTypes().subscribe((types) => {
      this.partyTypes.set(types);
      // Preselect the first non-placeholder type (usually "Mandant") for convenience.
      const first = types.find((t) => !t.placeHolder) ?? types[0];
      if (first && !this.involvementType()) {
        this.involvementType.set(first.name);
      }
    });
  }

  protected onSearch(value: string): void {
    this.query.set(value);
    this.selected.set(null);
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
      this.cases.searchContacts(value.trim()).subscribe((hits) => {
        if (seq === this.searchSeq) {
          this.results.set(hits);
          this.searching.set(false);
        }
      });
    }, 250);
  }

  protected pick(r: ContactRef): void {
    this.selected.set(r);
  }

  protected clearSelection(): void {
    this.selected.set(null);
  }

  protected submit(): void {
    const s = this.selected();
    if (s && this.involvementType().trim()) {
      this.save.emit({ addressId: s.id, involvementType: this.involvementType().trim() });
    }
  }
}
