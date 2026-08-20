import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { AuthService } from '../core/auth/auth.service';
import { IconComponent } from '../shared/icon.component';
import { AdminGroup, AdminUser, UsersAdminService } from './users-admin.service';

interface GroupDraft { id?: string; name: string; abbreviation: string; }

/**
 * "Gruppen" section: full group administration — create / rename / delete security groups and edit
 * their membership by ticking users. All mutations need `adminRole` (also enforced server-side);
 * business errors surface inline. Membership toggles are applied immediately (optimistic).
 */
@Component({
  selector: 'jl-settings-groups',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent],
  template: `
    <div class="sg">
      @if (!canEdit()) { <p class="sg-note">{{ 'settings.readOnlyHint' | transloco }}</p> }
      @if (opError()) { <p class="sg-error">{{ opError() }}</p> }

      @if (loading()) {
        <p class="sg-muted">{{ 'settings.loading' | transloco }}</p>
      } @else if (loadError()) {
        <p class="sg-error">{{ 'settings.loadError' | transloco }}</p>
      } @else {
        <div class="sg-grid">
          <!-- Group list -->
          <div class="sg-col">
            @if (canEdit()) {
              <button type="button" class="btn-primary sg-new" (click)="openNewGroup()">
                <jl-icon name="plus" [size]="15" /><span>{{ 'settings.groups.create' | transloco }}</span>
              </button>
            }
            @if (groups().length === 0) {
              <p class="sg-muted">{{ 'settings.empty' | transloco }}</p>
            } @else {
              <ul class="sg-list">
                @for (g of groups(); track g.id) {
                  <li class="sg-row" [class.sel]="selectedGroupId() === g.id" (click)="selectGroup(g)">
                    <span class="sg-name">{{ g.name }}</span>
                    @if (g.abbreviation) { <span class="sg-abbr">{{ g.abbreviation }}</span> }
                    @if (canEdit()) {
                      <button type="button" class="icon-btn" (click)="openEditGroup(g); $event.stopPropagation()" [attr.aria-label]="'settings.rename' | transloco">
                        <jl-icon name="edit" [size]="14" />
                      </button>
                      <button type="button" class="icon-btn danger" [disabled]="busy()" (click)="askDeleteGroup(g); $event.stopPropagation()" [attr.aria-label]="'settings.delete' | transloco">
                        <jl-icon name="trash" [size]="14" />
                      </button>
                    }
                  </li>
                }
              </ul>
            }
          </div>

          <!-- Membership of the selected group -->
          <div class="sg-col">
            @if (selectedGroup(); as g) {
              <h3 class="sg-mh">{{ 'settings.groups.members' | transloco }} — {{ g.name }}</h3>
              @if (membersLoading()) {
                <p class="sg-muted">{{ 'settings.loading' | transloco }}</p>
              } @else if (users().length === 0) {
                <p class="sg-muted">{{ 'settings.groups.noUsers' | transloco }}</p>
              } @else {
                <ul class="sg-members">
                  @for (u of users(); track u.principalId) {
                    <li class="sg-mrow">
                      <label class="sg-check">
                        <input type="checkbox" [checked]="isMember(u.principalId!)" [disabled]="!canEdit() || busy()"
                               (change)="toggleMember(u.principalId!, $any($event.target).checked)" />
                        <span>{{ userLabel(u) }}</span>
                      </label>
                    </li>
                  }
                </ul>
              }
            } @else {
              <p class="sg-muted">{{ 'settings.groups.selectGroup' | transloco }}</p>
            }
          </div>
        </div>
      }
    </div>

    @if (groupModal(); as m) {
      <div class="ed-backdrop" (click)="closeModal()"></div>
      <div class="ed-dialog" role="dialog" aria-modal="true">
        <header class="ed-head"><h2>{{ (m === 'new' ? 'settings.groups.create' : 'settings.groups.edit') | transloco }}</h2></header>
        <div class="ed-body">
          <label class="ed-field">
            <span>{{ 'settings.groups.name' | transloco }}</span>
            <input type="text" [value]="draft().name" (input)="patch('name', $any($event.target).value)" (keydown.enter)="submitGroup()" />
          </label>
          <label class="ed-field">
            <span>{{ 'settings.groups.abbreviation' | transloco }}</span>
            <input type="text" [value]="draft().abbreviation" (input)="patch('abbreviation', $any($event.target).value)" (keydown.enter)="submitGroup()" />
          </label>
          @if (modalError()) { <p class="ed-error">{{ modalError() }}</p> }
        </div>
        <footer class="ed-foot">
          <button type="button" class="btn-ghost" [disabled]="saving()" (click)="closeModal()">{{ 'settings.cancel' | transloco }}</button>
          <button type="button" class="btn-primary" [disabled]="saving() || !draft().name.trim()" (click)="submitGroup()">
            {{ (saving() ? 'settings.saving' : 'settings.save') | transloco }}
          </button>
        </footer>
      </div>
    }
  `,
  styles: [`
    :host { display: block; }
    .sg { max-width: 760px; }
    .sg-note { margin: 0 0 12px; font-size: .82rem; color: #b26a00; }
    .sg-muted { color: var(--jl-ink-soft); font-size: .88rem; }
    .sg-error { color: var(--jl-red); font-size: .84rem; margin: 0 0 10px; }
    .sg-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
    .sg-new { margin-bottom: 10px; }
    .sg-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 4px; }
    .sg-row { display: flex; align-items: center; gap: 10px; padding: 8px 11px; border: 1px solid var(--jl-line);
      border-radius: 8px; background: var(--jl-surface); cursor: pointer; }
    .sg-row:hover { border-color: var(--jl-line-strong); }
    .sg-row.sel { border-color: var(--jl-blue); background: color-mix(in srgb, var(--jl-blue) 8%, transparent); }
    .sg-name { flex: 1 1 auto; font-size: .9rem; color: var(--jl-ink); }
    .sg-abbr { font-size: .76rem; color: var(--jl-ink-faint); font-variant-numeric: tabular-nums; }
    .sg-mh { margin: 0 0 10px; font-size: .9rem; font-weight: 700; color: var(--jl-ink); }
    .sg-members { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 2px; max-height: 360px; overflow-y: auto; }
    .sg-check { display: flex; align-items: center; gap: 9px; font-size: .88rem; color: var(--jl-ink); padding: 6px 4px; cursor: pointer; }
    .sg-check input { width: 16px; height: 16px; }
    .icon-btn.danger { color: var(--jl-red); }
    @media (max-width: 620px) { .sg-grid { grid-template-columns: 1fr; } }
  `],
  styleUrls: ['./finance-editor.css'],
})
export class SettingsGroupsComponent implements OnInit {
  private readonly api = inject(UsersAdminService);
  private readonly auth = inject(AuthService);
  private readonly transloco = inject(TranslocoService);

  protected readonly groups = signal<AdminGroup[]>([]);
  protected readonly users = signal<AdminUser[]>([]);
  protected readonly loading = signal(false);
  protected readonly loadError = signal(false);
  protected readonly busy = signal(false);
  protected readonly opError = signal<string | null>(null);

  protected readonly selectedGroupId = signal<string | null>(null);
  protected readonly selectedGroup = computed(() => this.groups().find((g) => g.id === this.selectedGroupId()) ?? null);
  protected readonly memberSet = signal<Set<string>>(new Set());
  protected readonly membersLoading = signal(false);

  protected readonly groupModal = signal<'new' | 'edit' | null>(null);
  protected readonly draft = signal<GroupDraft>({ name: '', abbreviation: '' });
  protected readonly saving = signal(false);
  protected readonly modalError = signal<string | null>(null);

  protected readonly canEdit = computed(() => this.auth.hasRole('adminRole'));

  ngOnInit(): void {
    this.loading.set(true);
    this.api.listUsers().subscribe({
      next: (u) => this.users.set([...u].sort((a, b) => this.userLabel(a).localeCompare(this.userLabel(b)))),
      error: () => undefined,
    });
    this.reload();
  }

  private reload(keepId?: string): void {
    this.loadError.set(false);
    this.api.listGroups().subscribe({
      next: (g) => {
        this.groups.set([...g].sort((a, b) => (a.name ?? '').localeCompare(b.name ?? '')));
        this.loading.set(false);
        const want = keepId ?? this.selectedGroupId();
        if (want && this.groups().some((x) => x.id === want)) { this.selectedGroupId.set(want); }
      },
      error: () => { this.loadError.set(true); this.loading.set(false); },
    });
  }

  protected userLabel(u: AdminUser): string {
    return (u.displayName?.trim())
      || [u.firstName, u.name].filter(Boolean).join(' ').trim()
      || u.principalId
      || '';
  }

  protected selectGroup(g: AdminGroup): void {
    if (!g.id) { return; }
    this.selectedGroupId.set(g.id);
    this.membersLoading.set(true);
    this.opError.set(null);
    this.api.groupMembers(g.id).subscribe({
      next: (ids) => { this.memberSet.set(new Set(ids)); this.membersLoading.set(false); },
      error: () => { this.memberSet.set(new Set()); this.membersLoading.set(false); },
    });
  }

  protected isMember(principalId: string): boolean {
    return this.memberSet().has(principalId);
  }

  protected toggleMember(principalId: string, checked: boolean): void {
    const gid = this.selectedGroupId();
    if (!gid || this.busy()) { return; }
    this.busy.set(true);
    this.opError.set(null);
    const req = checked ? this.api.addGroupMember(gid, principalId) : this.api.removeGroupMember(gid, principalId);
    req.subscribe({
      next: () => {
        const next = new Set(this.memberSet());
        if (checked) { next.add(principalId); } else { next.delete(principalId); }
        this.memberSet.set(next);
        this.busy.set(false);
      },
      error: (e: HttpErrorResponse) => { this.busy.set(false); this.opError.set(this.msg(e)); },
    });
  }

  // --- group CRUD ---

  protected openNewGroup(): void {
    this.draft.set({ name: '', abbreviation: '' });
    this.modalError.set(null);
    this.groupModal.set('new');
  }

  protected openEditGroup(g: AdminGroup): void {
    this.draft.set({ id: g.id, name: g.name ?? '', abbreviation: g.abbreviation ?? '' });
    this.modalError.set(null);
    this.groupModal.set('edit');
  }

  protected patch<K extends keyof GroupDraft>(key: K, value: GroupDraft[K]): void {
    this.draft.update((d) => ({ ...d, [key]: value }));
  }

  protected submitGroup(): void {
    const d = this.draft();
    if (!d.name.trim() || this.saving()) { return; }
    this.saving.set(true);
    this.modalError.set(null);
    this.opError.set(null);
    const payload: AdminGroup = { id: d.id, name: d.name.trim(), abbreviation: d.abbreviation.trim() };
    const req = this.groupModal() === 'edit' ? this.api.updateGroup(payload) : this.api.createGroup(payload);
    req.subscribe({
      next: (g) => { this.saving.set(false); this.groupModal.set(null); this.reload(g.id); },
      error: (e: HttpErrorResponse) => { this.saving.set(false); this.modalError.set(this.msg(e)); },
    });
  }

  protected askDeleteGroup(g: AdminGroup): void {
    if (!g.id || this.busy()) { return; }
    if (!confirm(this.transloco.translate('settings.groups.confirmDelete', { name: g.name }))) { return; }
    this.busy.set(true);
    this.opError.set(null);
    this.api.deleteGroup(g.id).subscribe({
      next: () => { this.busy.set(false); if (this.selectedGroupId() === g.id) { this.selectedGroupId.set(null); } this.reload(); },
      error: (e: HttpErrorResponse) => { this.busy.set(false); this.opError.set(this.msg(e)); },
    });
  }

  protected closeModal(): void { if (!this.saving()) { this.groupModal.set(null); } }

  private msg(e: HttpErrorResponse): string {
    const body = e?.error;
    if (typeof body === 'string' && body.trim()) { return body.trim(); }
    return this.transloco.translate('settings.saveError');
  }
}
