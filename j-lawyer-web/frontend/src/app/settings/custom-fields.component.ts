import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { TranslocoModule } from '@jsverse/transloco';
import { AuthService } from '../core/auth/auth.service';
import { CustomFieldEntity, CustomFieldLabels, CustomFieldsService } from './custom-fields.service';

interface Group { entityType: CustomFieldEntity; titleKey: string; }
const GROUPS: Group[] = [
  { entityType: 'address', titleKey: 'settings.customFields.address' },
  { entityType: 'case', titleKey: 'settings.customFields.case' },
  { entityType: 'party', titleKey: 'settings.customFields.party' },
];

const EMPTY: CustomFieldLabels = { label1: '', label2: '', label3: '' };

/**
 * Editor for the custom-field ("eigene Felder") labels of addresses, cases and involved parties.
 * Each entity type has three optional labels; an empty label hides that field in the views. Saving
 * needs `adminRole` (also enforced server-side).
 */
@Component({
  selector: 'jl-custom-fields',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule],
  template: `
    <div class="cf">
      @if (!canEdit()) { <p class="cf-hint">{{ 'settings.readOnlyHint' | transloco }}</p> }
      <p class="cf-note">{{ 'settings.customFields.hint' | transloco }}</p>

      @if (loading()) {
        <p class="cf-muted">{{ 'settings.loading' | transloco }}</p>
      } @else if (loadError()) {
        <p class="cf-error">{{ 'settings.loadError' | transloco }}</p>
      } @else {
        @for (g of groups; track g.entityType) {
          <fieldset class="cf-sec">
            <legend>{{ g.titleKey | transloco }}</legend>
            <div class="cf-grid">
              @for (n of [1, 2, 3]; track n) {
                <label class="cf-field">
                  <span>{{ 'settings.customFields.field' | transloco: { n: n } }}</span>
                  <input type="text" [value]="labelOf(g.entityType, n)" [disabled]="!canEdit() || saving()"
                         (input)="patch(g.entityType, n, $any($event.target).value)" />
                </label>
              }
            </div>
          </fieldset>
        }

        <div class="cf-foot">
          @if (savedOk()) { <span class="cf-ok">{{ 'settings.savedOk' | transloco }}</span> }
          @if (saveError()) { <span class="cf-error">{{ 'settings.saveError' | transloco }}</span> }
          <button type="button" class="btn-primary" [disabled]="!canEdit() || saving()" (click)="submit()">
            {{ (saving() ? 'settings.saving' : 'settings.save') | transloco }}
          </button>
        </div>
      }
    </div>
  `,
  styles: [`
    :host { display: block; }
    .cf { max-width: 640px; }
    .cf-hint { margin: 0 0 8px; font-size: .82rem; color: #b26a00; }
    .cf-note { margin: 0 0 14px; font-size: .8rem; color: var(--jl-ink-faint); }
    .cf-muted { color: var(--jl-ink-soft); font-size: .88rem; }
    .cf-error { color: var(--jl-red); font-size: .84rem; }
    .cf-sec { border: 1px solid var(--jl-line); border-radius: 10px; padding: 12px 14px 14px; margin: 0 0 14px; }
    .cf-sec legend { font-size: .8rem; font-weight: 700; color: var(--jl-ink-soft); padding: 0 6px; text-transform: uppercase; letter-spacing: .04em; }
    .cf-grid { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 12px; }
    .cf-field { display: flex; flex-direction: column; gap: 5px; font-size: .8rem; color: var(--jl-ink-faint); }
    .cf-field input { font: inherit; font-size: .9rem; color: var(--jl-ink); padding: 8px 10px;
      border: 1px solid var(--jl-line-strong); border-radius: 8px; background: var(--jl-surface); outline: none; }
    .cf-field input:focus { border-color: var(--jl-blue); }
    .cf-field input:disabled { opacity: .6; }
    .cf-foot { display: flex; align-items: center; justify-content: flex-end; gap: 12px; }
    .cf-ok { color: var(--jl-green, #1a7f37); font-size: .84rem; }
    .btn-primary { font: inherit; font-size: .86rem; font-weight: 600; padding: 9px 18px; border: 0; border-radius: 8px;
      background: var(--jl-blue); color: #fff; cursor: pointer; }
    .btn-primary:disabled { opacity: .55; cursor: default; }
    @media (max-width: 560px) { .cf-grid { grid-template-columns: 1fr; } }
  `],
})
export class CustomFieldsComponent implements OnInit {
  private readonly api = inject(CustomFieldsService);
  private readonly auth = inject(AuthService);

  protected readonly groups = GROUPS;
  protected readonly drafts = signal<Record<CustomFieldEntity, CustomFieldLabels>>({
    address: { ...EMPTY }, case: { ...EMPTY }, party: { ...EMPTY },
  });
  protected readonly loading = signal(false);
  protected readonly loadError = signal(false);
  protected readonly saving = signal(false);
  protected readonly saveError = signal(false);
  protected readonly savedOk = signal(false);
  protected readonly canEdit = computed(() => this.auth.hasRole('adminRole'));

  ngOnInit(): void {
    this.loading.set(true);
    this.loadError.set(false);
    forkJoin({
      address: this.api.labels('address'),
      case: this.api.labels('case'),
      party: this.api.labels('party'),
    }).subscribe({
      next: (r) => { this.drafts.set({ address: { ...r.address }, case: { ...r.case }, party: { ...r.party } }); this.loading.set(false); },
      error: () => { this.loadError.set(true); this.loading.set(false); },
    });
  }

  protected labelOf(entity: CustomFieldEntity, n: number): string {
    return (this.drafts()[entity] as unknown as Record<string, string>)['label' + n] ?? '';
  }

  protected patch(entity: CustomFieldEntity, n: number, value: string): void {
    this.drafts.update((d) => ({ ...d, [entity]: { ...d[entity], ['label' + n]: value } }));
    this.savedOk.set(false);
  }

  protected submit(): void {
    if (!this.canEdit() || this.saving()) { return; }
    this.saving.set(true);
    this.saveError.set(false);
    this.savedOk.set(false);
    const d = this.drafts();
    forkJoin([
      this.api.save('address', d.address),
      this.api.save('case', d.case),
      this.api.save('party', d.party),
    ]).subscribe({
      next: () => { this.saving.set(false); this.savedOk.set(true); },
      error: () => { this.saving.set(false); this.saveError.set(true); },
    });
  }
}
