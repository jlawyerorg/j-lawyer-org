import {
  ChangeDetectionStrategy, Component, computed, effect, inject, input, signal,
} from '@angular/core';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { AuthService } from '../core/auth/auth.service';
import { OptionAdminService, OptionValue } from './option-admin.service';

/**
 * Generic editor for one configuration option group ("Wertevorrat"): lists the values and lets the
 * user add, rename and delete them. Reused across the ~20 dictionary sections (salutations,
 * countries, tags, tax rates, …) — the desktop equivalent is the reused OptionGroupConfigurationDialog.
 *
 * Editing is gated by the real server roles: add/rename need `createOptionGroupRole`, delete needs
 * `deleteOptionGroupRole`. Without them the list is read-only and a hint is shown; the server also
 * rejects the write, so this is purely to avoid dead buttons.
 */
@Component({
  selector: 'jl-option-list-editor',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent],
  template: `
    <div class="ole">
      @if (!canEdit()) {
        <p class="ole-hint">{{ 'settings.readOnlyHint' | transloco }}</p>
      }

      @if (canEdit()) {
        <form class="ole-add" (submit)="add($event)">
          <input type="text" [value]="newValue()" (input)="newValue.set($any($event.target).value)"
                 [placeholder]="'settings.newValue' | transloco" [disabled]="saving()" />
          <button type="submit" class="btn-primary" [disabled]="saving() || !newValue().trim()">
            <jl-icon name="plus" [size]="15" /><span>{{ 'settings.add' | transloco }}</span>
          </button>
        </form>
      }

      @if (error()) { <p class="ole-error">{{ error() }}</p> }

      @if (loading()) {
        <p class="ole-muted">{{ 'settings.loading' | transloco }}</p>
      } @else if (items().length === 0) {
        <p class="ole-muted">{{ 'settings.empty' | transloco }}</p>
      } @else {
        <ul class="ole-list">
          @for (it of items(); track it.id) {
            <li class="ole-row">
              @if (editingId() === it.id) {
                <input class="ole-edit" type="text" [value]="editValue()"
                       (input)="editValue.set($any($event.target).value)"
                       (keydown.enter)="commitRename(it)" (keydown.escape)="cancelEdit()" />
                <button type="button" class="icon-btn" [disabled]="saving() || !editValue().trim()"
                        (click)="commitRename(it)" [attr.aria-label]="'settings.save' | transloco">
                  <jl-icon name="check" [size]="16" />
                </button>
                <button type="button" class="icon-btn" (click)="cancelEdit()"
                        [attr.aria-label]="'settings.cancel' | transloco">
                  <jl-icon name="close" [size]="16" />
                </button>
              } @else {
                <span class="ole-value">{{ it.value }}</span>
                @if (canEdit()) {
                  <button type="button" class="icon-btn" (click)="startEdit(it)"
                          [attr.aria-label]="'settings.rename' | transloco">
                    <jl-icon name="edit" [size]="15" />
                  </button>
                }
                @if (canDelete()) {
                  <button type="button" class="icon-btn danger" [disabled]="saving()" (click)="remove(it)"
                          [attr.aria-label]="'settings.delete' | transloco">
                    <jl-icon name="trash" [size]="15" />
                  </button>
                }
              }
            </li>
          }
        </ul>
      }
    </div>
  `,
  styles: [`
    :host { display: block; }
    .ole { max-width: 620px; }
    .ole-hint { margin: 0 0 12px; font-size: .82rem; color: #b26a00; }
    .ole-add { display: flex; gap: 8px; margin-bottom: 14px; }
    .ole-add input { flex: 1 1 auto; font: inherit; font-size: .9rem; padding: 8px 11px;
      border: 1px solid var(--jl-line-strong); border-radius: 8px; background: var(--jl-surface); color: var(--jl-ink); }
    .ole-add input:focus { outline: none; border-color: var(--jl-blue); }
    .btn-primary { display: inline-flex; align-items: center; gap: 6px; font: inherit; font-size: .86rem;
      font-weight: 600; padding: 8px 14px; border: 0; border-radius: 8px; background: var(--jl-blue); color: #fff; cursor: pointer; }
    .btn-primary:hover:not(:disabled) { filter: brightness(1.06); }
    .btn-primary:disabled { opacity: .55; cursor: default; }
    .ole-error { margin: 0 0 12px; color: var(--jl-red); font-size: .84rem; }
    .ole-muted { color: var(--jl-ink-soft); font-size: .88rem; }
    .ole-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 4px; }
    .ole-row { display: flex; align-items: center; gap: 6px; padding: 6px 8px; border: 1px solid var(--jl-line);
      border-radius: 8px; background: var(--jl-surface); }
    .ole-row:hover { border-color: var(--jl-line-strong); }
    .ole-value { flex: 1 1 auto; font-size: .9rem; color: var(--jl-ink); min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .ole-edit { flex: 1 1 auto; font: inherit; font-size: .9rem; padding: 5px 8px; border: 1px solid var(--jl-blue);
      border-radius: 6px; background: var(--jl-surface); color: var(--jl-ink); }
    .ole-edit:focus { outline: none; }
    .icon-btn { display: inline-flex; align-items: center; justify-content: center; width: 30px; height: 30px;
      border: 1px solid transparent; border-radius: 7px; background: transparent; color: var(--jl-ink-soft); cursor: pointer; }
    .icon-btn:hover:not(:disabled) { background: var(--jl-surface-alt); color: var(--jl-ink); }
    .icon-btn.danger:hover:not(:disabled) { color: var(--jl-red); }
    .icon-btn:disabled { opacity: .5; cursor: default; }
  `],
})
export class OptionListEditorComponent {
  private readonly api = inject(OptionAdminService);
  private readonly auth = inject(AuthService);

  /** The option-group key to edit (e.g. 'address.salutation'). */
  readonly group = input.required<string>();

  protected readonly items = signal<OptionValue[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly saving = signal(false);
  protected readonly newValue = signal('');
  protected readonly editingId = signal<string | null>(null);
  protected readonly editValue = signal('');

  protected readonly canEdit = computed(() => this.auth.hasRole('createOptionGroupRole'));
  protected readonly canDelete = computed(() => this.auth.hasRole('deleteOptionGroupRole'));

  constructor() {
    // Reload whenever the selected group changes (the shared screen reuses this component instance).
    effect(() => { this.group(); this.reload(); });
  }

  private reload(): void {
    const g = this.group();
    this.editingId.set(null);
    this.error.set(null);
    this.loading.set(true);
    this.api.list(g).subscribe({
      next: (rows) => { this.items.set(rows); this.loading.set(false); },
      error: () => { this.error.set('load'); this.loading.set(false); },
    });
  }

  protected add(ev: Event): void {
    ev.preventDefault();
    const value = this.newValue().trim();
    if (!value || this.saving()) { return; }
    this.saving.set(true);
    this.error.set(null);
    this.api.create(this.group(), value).subscribe({
      next: (created) => {
        if (!this.items().some((i) => i.id === created.id)) {
          this.items.update((rows) => [...rows, created].sort((a, b) => a.value.localeCompare(b.value)));
        }
        this.newValue.set('');
        this.saving.set(false);
      },
      error: () => { this.error.set('save'); this.saving.set(false); },
    });
  }

  protected startEdit(it: OptionValue): void {
    this.editingId.set(it.id);
    this.editValue.set(it.value);
  }

  protected cancelEdit(): void {
    this.editingId.set(null);
  }

  protected commitRename(it: OptionValue): void {
    const value = this.editValue().trim();
    if (!value || this.saving()) { return; }
    if (value === it.value) { this.cancelEdit(); return; }
    this.saving.set(true);
    this.error.set(null);
    this.api.rename(this.group(), it.id, value).subscribe({
      next: (updated) => {
        // The server implements rename as delete-then-create, so the value's id CHANGES; adopt the
        // returned id, otherwise a later rename/delete would target a stale id and 404.
        this.items.update((rows) => rows
          .map((r) => (r.id === it.id ? { id: updated.id, value: updated.value } : r))
          .sort((a, b) => a.value.localeCompare(b.value)));
        this.editingId.set(null);
        this.saving.set(false);
      },
      error: () => { this.error.set('save'); this.saving.set(false); },
    });
  }

  protected remove(it: OptionValue): void {
    if (this.saving()) { return; }
    this.saving.set(true);
    this.error.set(null);
    this.api.delete(this.group(), it.id).subscribe({
      next: () => {
        this.items.update((rows) => rows.filter((r) => r.id !== it.id));
        this.saving.set(false);
      },
      error: () => { this.error.set('delete'); this.saving.set(false); },
    });
  }
}
