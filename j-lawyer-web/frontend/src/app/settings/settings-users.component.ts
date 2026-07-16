import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { switchMap } from 'rxjs';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { AdminRole, AdminUser, UsersAdminService } from './users-admin.service';
import { UserEditorComponent, UserEditResult } from './user-editor.component';

/**
 * "Nutzer" section: lists users and hosts the {@link UserEditorComponent} modal to create a user or
 * edit an existing one's master data and role assignment (v6 security REST). The Administration
 * screen already gates this behind `adminRole`, so the write endpoints are reachable.
 */
@Component({
  selector: 'jl-settings-users',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent, UserEditorComponent],
  template: `
    <div class="su">
      <div class="su-bar">
        <button type="button" class="btn-primary" (click)="openNew()">
          <jl-icon name="plus" [size]="15" /><span>{{ 'settings.users.create' | transloco }}</span>
        </button>
      </div>

      @if (loading()) {
        <p class="su-muted">{{ 'settings.loading' | transloco }}</p>
      } @else if (loadError()) {
        <p class="su-error">{{ 'settings.loadError' | transloco }}</p>
      } @else {
        <ul class="su-list">
          @for (u of users(); track u.principalId) {
            <li class="su-row" (click)="openEdit(u)">
              <div class="su-main">
                <span class="su-name">{{ u.displayName || u.principalId }}</span>
                <span class="su-login">{{ u.principalId }}</span>
              </div>
              @if (u.lawyer) { <span class="su-badge">{{ 'settings.users.lawyer' | transloco }}</span> }
              <jl-icon name="edit" [size]="15" />
            </li>
          }
        </ul>
      }
    </div>

    @if (editorOpen()) {
      <jl-user-editor [user]="editUser()" [allRoles]="allRoles()" [assignedRoles]="assignedRoles()"
                      [saving]="saving()" [error]="saveError()"
                      (save)="onSave($event)" (close)="editorOpen.set(false)" />
    }
  `,
  styles: [`
    :host { display: block; }
    .su { max-width: 640px; }
    .su-bar { margin-bottom: 14px; }
    .btn-primary { display: inline-flex; align-items: center; gap: 6px; font: inherit; font-size: .86rem;
      font-weight: 600; padding: 8px 14px; border: 0; border-radius: 8px; background: var(--jl-blue); color: #fff; cursor: pointer; }
    .btn-primary:hover { filter: brightness(1.06); }
    .su-muted { color: var(--jl-ink-soft); font-size: .88rem; }
    .su-error { color: var(--jl-red); font-size: .84rem; }
    .su-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 4px; }
    .su-row { display: flex; align-items: center; gap: 10px; padding: 9px 11px; border: 1px solid var(--jl-line);
      border-radius: 9px; background: var(--jl-surface); cursor: pointer; color: var(--jl-ink-soft); }
    .su-row:hover { border-color: var(--jl-blue); }
    .su-main { flex: 1 1 auto; min-width: 0; display: flex; flex-direction: column; gap: 1px; }
    .su-name { font-size: .9rem; font-weight: 600; color: var(--jl-ink); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .su-login { font-size: .76rem; color: var(--jl-ink-faint); }
    .su-badge { font-size: .72rem; font-weight: 700; color: var(--jl-blue); background: var(--jl-surface-alt);
      border-radius: 999px; padding: 2px 9px; }
  `],
})
export class SettingsUsersComponent implements OnInit {
  private readonly api = inject(UsersAdminService);

  protected readonly users = signal<AdminUser[]>([]);
  protected readonly allRoles = signal<AdminRole[]>([]);
  protected readonly loading = signal(false);
  protected readonly loadError = signal(false);

  protected readonly editorOpen = signal(false);
  protected readonly editUser = signal<AdminUser | null>(null);
  protected readonly assignedRoles = signal<string[]>([]);
  protected readonly saving = signal(false);
  protected readonly saveError = signal<string | null>(null);

  ngOnInit(): void {
    this.api.listRoles().subscribe({ next: (r) => this.allRoles.set(r), error: () => {} });
    this.reload();
  }

  private reload(): void {
    this.loading.set(true);
    this.loadError.set(false);
    this.api.listUsers().subscribe({
      next: (u) => { this.users.set(u); this.loading.set(false); },
      error: () => { this.loadError.set(true); this.loading.set(false); },
    });
  }

  protected openNew(): void {
    this.editUser.set(null);
    this.assignedRoles.set([]);
    this.saveError.set(null);
    this.editorOpen.set(true);
  }

  protected openEdit(u: AdminUser): void {
    this.editUser.set(u);
    this.assignedRoles.set([]);
    this.saveError.set(null);
    if (u.principalId) {
      this.api.getUserRoles(u.principalId).subscribe({
        next: (roles) => this.assignedRoles.set(roles.map((r) => r.role)),
        error: () => {},
      });
    }
    this.editorOpen.set(true);
  }

  protected onSave(result: UserEditResult): void {
    const pid = result.user.principalId;
    if (!pid) { return; }
    this.saving.set(true);
    this.saveError.set(null);
    const isNew = this.editUser() === null;
    const write$ = isNew ? this.api.createUser(result.user) : this.api.updateUser(result.user);
    const roleObjs: AdminRole[] = result.roles.map((role) => ({ role }));
    write$.pipe(switchMap(() => this.api.setUserRoles(pid, roleObjs))).subscribe({
      next: () => { this.saving.set(false); this.editorOpen.set(false); this.reload(); },
      error: () => { this.saving.set(false); this.saveError.set('save'); },
    });
  }
}
