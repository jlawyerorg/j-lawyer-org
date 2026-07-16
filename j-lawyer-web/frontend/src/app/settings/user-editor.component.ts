import {
  ChangeDetectionStrategy, Component, computed, inject, input, OnInit, output, signal,
} from '@angular/core';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { AdminRole, AdminUser } from './users-admin.service';

/** Payload emitted on save: the full (round-tripped) user plus the selected role names. */
export interface UserEditResult {
  user: AdminUser;
  roles: string[];
}

/**
 * Modal editor for a user's master data and role assignment. For a new user `principalId` (login
 * name) is editable and required; on edit it is fixed. The full original record is round-tripped so
 * fields the form does not surface are preserved (the server update overwrites the whole record).
 *
 * Note surfaced to the user: creating a user does not set a password or grant login — that stays a
 * desktop/admin task (server contract). Role assignment and editing existing users work fully.
 */
@Component({
  selector: 'jl-user-editor',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent],
  template: `
    <div class="ue-backdrop" (click)="close.emit()"></div>
    <div class="ue-dialog" role="dialog" aria-modal="true">
      <header class="ue-head">
        <h2>{{ (isNew() ? 'settings.users.create' : 'settings.users.edit') | transloco }}</h2>
        <button type="button" class="icon-btn" (click)="close.emit()" [attr.aria-label]="'settings.cancel' | transloco">
          <jl-icon name="close" [size]="18" />
        </button>
      </header>

      <div class="ue-body">
        <div class="ue-grid">
          <label class="ue-field">
            <span>{{ 'settings.users.principalId' | transloco }}</span>
            <input type="text" [value]="draft().principalId ?? ''" [disabled]="!isNew()"
                   (input)="patch('principalId', $any($event.target).value)" />
          </label>
          <label class="ue-field">
            <span>{{ 'settings.users.displayName' | transloco }}</span>
            <input type="text" [value]="draft().displayName ?? ''"
                   (input)="patch('displayName', $any($event.target).value)" />
          </label>
          <label class="ue-field">
            <span>{{ 'settings.users.firstName' | transloco }}</span>
            <input type="text" [value]="draft().firstName ?? ''"
                   (input)="patch('firstName', $any($event.target).value)" />
          </label>
          <label class="ue-field">
            <span>{{ 'settings.users.name' | transloco }}</span>
            <input type="text" [value]="draft().name ?? ''"
                   (input)="patch('name', $any($event.target).value)" />
          </label>
          <label class="ue-field">
            <span>{{ 'settings.users.email' | transloco }}</span>
            <input type="email" [value]="draft().email ?? ''"
                   (input)="patch('email', $any($event.target).value)" />
          </label>
          <label class="ue-field">
            <span>{{ 'settings.users.phone' | transloco }}</span>
            <input type="text" [value]="draft().phone ?? ''"
                   (input)="patch('phone', $any($event.target).value)" />
          </label>
          <label class="ue-field ue-check">
            <input type="checkbox" [checked]="draft().lawyer ?? false"
                   (change)="patch('lawyer', $any($event.target).checked)" />
            <span>{{ 'settings.users.lawyer' | transloco }}</span>
          </label>
        </div>

        <fieldset class="ue-roles">
          <legend>{{ 'settings.users.roles' | transloco }}</legend>
          @for (r of allRoles(); track r.role) {
            <label class="ue-role">
              <input type="checkbox" [checked]="selected().has(r.role)" (change)="toggleRole(r.role)" />
              <span class="ue-role-name">{{ r.role }}</span>
              @if (r.description) { <span class="ue-role-desc">{{ r.description }}</span> }
            </label>
          }
        </fieldset>

        <p class="ue-note">{{ 'settings.users.passwordNote' | transloco }}</p>
        @if (error()) { <p class="ue-error">{{ error() }}</p> }
      </div>

      <footer class="ue-foot">
        <button type="button" class="btn-ghost" [disabled]="saving()" (click)="close.emit()">
          {{ 'settings.cancel' | transloco }}
        </button>
        <button type="button" class="btn-primary" [disabled]="saving() || !canSave()" (click)="submit()">
          {{ (saving() ? 'settings.saving' : 'settings.save') | transloco }}
        </button>
      </footer>
    </div>
  `,
  styles: [`
    :host { display: contents; }
    .ue-backdrop { position: fixed; inset: 0; background: rgba(4, 12, 20, .45); z-index: 42; }
    .ue-dialog { position: fixed; z-index: 43; left: 50%; top: 50%; transform: translate(-50%, -50%);
      width: min(620px, calc(100vw - 32px)); max-height: calc(100vh - 48px); display: flex; flex-direction: column;
      background: var(--jl-surface); color: var(--jl-ink); border: 1px solid var(--jl-line-strong);
      border-radius: 14px; box-shadow: 0 24px 60px rgba(4, 12, 20, .4); }
    .ue-head { display: flex; align-items: center; justify-content: space-between; gap: 8px; padding: 14px 16px; border-bottom: 1px solid var(--jl-line); }
    .ue-head h2 { margin: 0; font-size: 1rem; font-weight: 700; }
    .ue-body { flex: 1 1 auto; overflow-y: auto; padding: 16px; display: flex; flex-direction: column; gap: 16px; }
    .ue-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    .ue-field { display: flex; flex-direction: column; gap: 5px; font-size: .8rem; color: var(--jl-ink-faint); }
    .ue-field input[type=text], .ue-field input[type=email] { font: inherit; font-size: .9rem; color: var(--jl-ink);
      padding: 8px 10px; border: 1px solid var(--jl-line-strong); border-radius: 8px; background: var(--jl-surface); outline: none; }
    .ue-field input:focus { border-color: var(--jl-blue); }
    .ue-field input:disabled { opacity: .6; }
    .ue-check { flex-direction: row; align-items: center; gap: 8px; grid-column: 1 / -1; color: var(--jl-ink); font-size: .9rem; }
    .ue-check input { width: 16px; height: 16px; }
    .ue-roles { border: 1px solid var(--jl-line); border-radius: 10px; padding: 12px 14px; margin: 0;
      display: grid; grid-template-columns: 1fr 1fr; gap: 6px 16px; }
    .ue-roles legend { font-size: .8rem; font-weight: 700; color: var(--jl-ink-soft); padding: 0 6px; }
    .ue-role { display: flex; align-items: baseline; gap: 7px; font-size: .86rem; color: var(--jl-ink); }
    .ue-role input { width: 15px; height: 15px; }
    .ue-role-name { font-weight: 600; }
    .ue-role-desc { color: var(--jl-ink-faint); font-size: .78rem; }
    .ue-note { margin: 0; font-size: .8rem; color: var(--jl-ink-soft); background: var(--jl-surface-alt);
      border-radius: 8px; padding: 9px 11px; }
    .ue-error { margin: 0; color: var(--jl-red); font-size: .84rem; }
    .ue-foot { display: flex; justify-content: flex-end; gap: 8px; padding: 12px 16px; border-top: 1px solid var(--jl-line); }
    .btn-ghost { font: inherit; font-size: .86rem; padding: 8px 14px; border: 1px solid var(--jl-line-strong);
      border-radius: 8px; background: var(--jl-surface); color: var(--jl-ink); cursor: pointer; }
    .btn-ghost:hover:not(:disabled) { background: var(--jl-surface-alt); }
    .btn-primary { font: inherit; font-size: .86rem; font-weight: 600; padding: 8px 16px; border: 0; border-radius: 8px;
      background: var(--jl-blue); color: #fff; cursor: pointer; }
    .btn-primary:hover:not(:disabled) { filter: brightness(1.06); }
    .btn-primary:disabled, .btn-ghost:disabled { opacity: .55; cursor: default; }
    .icon-btn { display: inline-flex; align-items: center; justify-content: center; width: 32px; height: 32px;
      border: 1px solid transparent; border-radius: 8px; background: transparent; color: var(--jl-ink-soft); cursor: pointer; }
    .icon-btn:hover { background: var(--jl-surface-alt); color: var(--jl-ink); }
    @media (max-width: 560px) { .ue-grid, .ue-roles { grid-template-columns: 1fr; } }
  `],
})
export class UserEditorComponent implements OnInit {
  /** The user to edit; null creates a new one. */
  readonly user = input<AdminUser | null>(null);
  readonly allRoles = input<AdminRole[]>([]);
  /** Role names currently assigned to the user. */
  readonly assignedRoles = input<string[]>([]);
  readonly saving = input<boolean>(false);
  readonly error = input<string | null>(null);

  readonly save = output<UserEditResult>();
  readonly close = output<void>();

  protected readonly draft = signal<AdminUser>({});
  protected readonly selected = signal<Set<string>>(new Set());

  protected readonly isNew = computed(() => !this.user());
  protected readonly canSave = computed(() => !!(this.draft().principalId ?? '').trim());

  ngOnInit(): void {
    this.draft.set({ ...(this.user() ?? {}) });
    this.selected.set(new Set(this.assignedRoles()));
  }

  protected patch<K extends keyof AdminUser>(key: K, value: AdminUser[K]): void {
    this.draft.update((d) => ({ ...d, [key]: value }));
  }

  protected toggleRole(role: string): void {
    this.selected.update((set) => {
      const next = new Set(set);
      if (next.has(role)) { next.delete(role); } else { next.add(role); }
      return next;
    });
  }

  protected submit(): void {
    if (!this.canSave() || this.saving()) { return; }
    this.save.emit({ user: this.draft(), roles: [...this.selected()] });
  }
}
