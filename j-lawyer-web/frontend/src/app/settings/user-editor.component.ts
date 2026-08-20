import {
  ChangeDetectionStrategy, Component, computed, inject, input, OnInit, output, signal,
} from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { AdminGroup, AdminRole, AdminUser, IdName, UsersAdminService } from './users-admin.service';

/** Payload emitted on save: the full (round-tripped) user plus the selected roles, groups and resource ids. */
export interface UserEditResult {
  user: AdminUser;
  roles: string[];
  groups: string[];
  calendars: string[];
  mailboxes: string[];
  invoicePools: string[];
}

interface UserField { key: keyof AdminUser; labelKey: string; type?: 'text' | 'email'; }
interface UserSection { titleKey: string; fields: UserField[]; }

/** Grouped master-data fields (all round-tripped by RestfulUserV6), mirroring the desktop dialog. */
const SECTIONS: UserSection[] = [
  { titleKey: 'settings.users.sec.person', fields: [
    { key: 'firstName', labelKey: 'settings.users.firstName' },
    { key: 'name', labelKey: 'settings.users.name' },
    { key: 'company', labelKey: 'settings.users.company' },
    { key: 'role', labelKey: 'settings.users.function' },
    { key: 'abbreviation', labelKey: 'settings.users.abbreviation' },
  ] },
  { titleKey: 'settings.users.sec.contact', fields: [
    { key: 'email', labelKey: 'settings.users.email', type: 'email' },
    { key: 'phone', labelKey: 'settings.users.phone' },
    { key: 'mobile', labelKey: 'settings.users.mobile' },
    { key: 'fax', labelKey: 'settings.users.fax' },
    { key: 'website', labelKey: 'settings.users.website' },
  ] },
  { titleKey: 'settings.users.sec.address', fields: [
    { key: 'street', labelKey: 'settings.users.street' },
    { key: 'adjunct', labelKey: 'settings.users.adjunct' },
    { key: 'zipCode', labelKey: 'settings.users.zipCode' },
    { key: 'city', labelKey: 'settings.users.city' },
    { key: 'countryCode', labelKey: 'settings.users.countryCode' },
  ] },
  { titleKey: 'settings.users.sec.banking', fields: [
    { key: 'bankName', labelKey: 'settings.users.bankName' },
    { key: 'bankBic', labelKey: 'settings.users.bankBic' },
    { key: 'bankIban', labelKey: 'settings.users.bankIban' },
    { key: 'taxNr', labelKey: 'settings.users.taxNr' },
    { key: 'taxVatId', labelKey: 'settings.users.taxVatId' },
    { key: 'countryCodeInvoicing', labelKey: 'settings.users.countryCodeInvoicing' },
  ] },
];

type ResKind = 'calendars' | 'mailboxes' | 'invoicePools';
type EditorTab = 'master' | 'permissions' | 'access' | 'integrations';

/**
 * Modal editor for a user's master data, permissions (roles), group membership and access to
 * calendars, mailboxes and invoice number pools, plus the Nextcloud connection and beA certificate.
 * Roles are shown by their human-readable description (never the technical name). The full original
 * record is round-tripped so fields not surfaced are preserved. Resource assignments are diffed by
 * the parent on save; the beA certificate is uploaded directly (only for existing users).
 */
@Component({
  selector: 'jl-user-editor',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent, NgTemplateOutlet],
  template: `
    <div class="ue-backdrop" (click)="close.emit()"></div>
    <div class="ue-dialog" role="dialog" aria-modal="true">
      <header class="ue-head">
        <h2>{{ (isNew() ? 'settings.users.create' : 'settings.users.edit') | transloco }}</h2>
        <button type="button" class="icon-btn" (click)="close.emit()" [attr.aria-label]="'settings.cancel' | transloco">
          <jl-icon name="close" [size]="18" />
        </button>
      </header>

      <nav class="ue-tabs">
        @for (t of tabs; track t.key) {
          <button type="button" class="ue-tab" [class.on]="activeTab() === t.key" (click)="activeTab.set(t.key)">
            {{ t.labelKey | transloco }}
          </button>
        }
      </nav>

      <div class="ue-body">
        @switch (activeTab()) {
          @case ('master') {
            <fieldset class="ue-sec">
              <legend>{{ 'settings.users.sec.account' | transloco }}</legend>
              <div class="ue-grid">
                <label class="ue-field">
                  <span>{{ 'settings.users.principalId' | transloco }}</span>
                  <input type="text" [value]="draft().principalId ?? ''" [disabled]="!isNew()"
                         (input)="patchText('principalId', $any($event.target).value)" />
                </label>
                <label class="ue-field">
                  <span>{{ 'settings.users.displayName' | transloco }}</span>
                  <input type="text" [value]="draft().displayName ?? ''"
                         (input)="patchText('displayName', $any($event.target).value)" />
                </label>
              </div>
              <label class="ue-check">
                <input type="checkbox" [checked]="draft().lawyer ?? false"
                       (change)="patch('lawyer', $any($event.target).checked)" />
                <span>{{ 'settings.users.lawyer' | transloco }}</span>
              </label>
              <label class="ue-check">
                <input type="checkbox" [checked]="draft().autoLockDocuments ?? false"
                       (change)="patch('autoLockDocuments', $any($event.target).checked)" />
                <span>{{ 'settings.users.autoLockDocuments' | transloco }}</span>
              </label>
            </fieldset>

            @for (sec of sections; track sec.titleKey) {
              <fieldset class="ue-sec">
                <legend>{{ sec.titleKey | transloco }}</legend>
                <div class="ue-grid">
                  @for (f of sec.fields; track f.key) {
                    <label class="ue-field">
                      <span>{{ f.labelKey | transloco }}</span>
                      <input [type]="f.type || 'text'" [value]="strVal(f.key)"
                             (input)="patchText(f.key, $any($event.target).value)" />
                    </label>
                  }
                </div>
              </fieldset>
            }

            <p class="ue-note">{{ 'settings.users.passwordNote' | transloco }}</p>
          }

          @case ('permissions') {
            <fieldset class="ue-roles">
              <legend>{{ 'settings.users.roles' | transloco }}</legend>
              @for (r of allRoles(); track r.role) {
                <label class="ue-role">
                  <input type="checkbox" [checked]="selectedRoles().has(r.role)" (change)="toggleRole(r.role)" />
                  <span class="ue-role-name">{{ r.description || r.role }}</span>
                </label>
              }
            </fieldset>

            @if (allGroups().length) {
              <fieldset class="ue-sec">
                <legend>{{ 'settings.users.primaryGroup' | transloco }}</legend>
                <label class="ue-field" style="max-width: 280px;">
                  <select [value]="draft().primaryGroupId ?? ''" (change)="patchText('primaryGroupId', $any($event.target).value)">
                    <option value="">{{ 'settings.users.noGroup' | transloco }}</option>
                    @for (g of allGroups(); track g.id) { <option [value]="g.id">{{ g.name }}</option> }
                  </select>
                </label>
              </fieldset>
              <fieldset class="ue-roles">
                <legend>{{ 'settings.users.groups' | transloco }}</legend>
                @if (isNew()) { <p class="ue-hint">{{ 'settings.users.groupsNewHint' | transloco }}</p> }
                @for (g of allGroups(); track g.id) {
                  <label class="ue-role">
                    <input type="checkbox" [checked]="selectedGroups().has(g.id!)" (change)="toggleGroup(g.id!)" />
                    <span class="ue-role-name">{{ g.name }}</span>
                    @if (g.abbreviation) { <span class="ue-role-desc">{{ g.abbreviation }}</span> }
                  </label>
                }
              </fieldset>
            }
          }

          @case ('access') {
            <ng-container *ngTemplateOutlet="resTpl; context: { titleKey: 'settings.users.sec.calendars', items: allCalendars(), kind: 'calendars' }" />
            <ng-container *ngTemplateOutlet="resTpl; context: { titleKey: 'settings.users.sec.mailboxes', items: allMailboxes(), kind: 'mailboxes' }" />
            <ng-container *ngTemplateOutlet="resTpl; context: { titleKey: 'settings.users.sec.invoicePools', items: allInvoicePools(), kind: 'invoicePools' }" />
          }

          @case ('integrations') {
            <fieldset class="ue-sec">
              <legend>{{ 'settings.users.sec.nextcloud' | transloco }}</legend>
              <div class="ue-grid">
                <label class="ue-field">
                  <span>{{ 'settings.users.cloudHost' | transloco }}</span>
                  <input type="text" [value]="draft().cloudHost ?? ''" (input)="patchText('cloudHost', $any($event.target).value)" />
                </label>
                <label class="ue-field">
                  <span>{{ 'settings.users.cloudPath' | transloco }}</span>
                  <input type="text" [value]="draft().cloudPath ?? ''" (input)="patchText('cloudPath', $any($event.target).value)" />
                </label>
                <label class="ue-field">
                  <span>{{ 'settings.users.cloudUser' | transloco }}</span>
                  <input type="text" [value]="draft().cloudUser ?? ''" (input)="patchText('cloudUser', $any($event.target).value)" />
                </label>
                <label class="ue-field">
                  <span>{{ 'settings.users.cloudPassword' | transloco }}</span>
                  <input type="password" [value]="draft().cloudPassword ?? ''" autocomplete="new-password"
                         [placeholder]="'settings.users.cloudPasswordHint' | transloco"
                         (input)="patchText('cloudPassword', $any($event.target).value)" />
                </label>
                <label class="ue-field">
                  <span>{{ 'settings.users.cloudPort' | transloco }}</span>
                  <input type="number" [value]="draft().cloudPort ?? 443" (input)="patchPort($any($event.target).value)" />
                </label>
              </div>
              <label class="ue-check">
                <input type="checkbox" [checked]="draft().cloudSsl ?? true" (change)="patch('cloudSsl', $any($event.target).checked)" />
                <span>{{ 'settings.users.cloudSsl' | transloco }}</span>
              </label>
            </fieldset>

            <fieldset class="ue-sec">
              <legend>{{ 'settings.users.sec.dropscan' | transloco }}</legend>
              <div class="ue-grid">
                <label class="ue-field">
                  <span>{{ 'settings.users.dropscanToken' | transloco }}</span>
                  <input type="password" autocomplete="new-password" [value]="draft().dropscanApiToken ?? ''"
                         [placeholder]="(draft().dropscanApiTokenSet ? 'settings.users.dropscanTokenKeep' : 'settings.users.dropscanTokenHint') | transloco"
                         (input)="patchText('dropscanApiToken', $any($event.target).value)" />
                </label>
                <label class="ue-field">
                  <span>{{ 'settings.users.dropscanScanboxes' | transloco }}</span>
                  <input type="text" [value]="draft().dropscanScanboxes ?? ''"
                         [placeholder]="'settings.users.dropscanScanboxesHint' | transloco"
                         (input)="patchText('dropscanScanboxes', $any($event.target).value)" />
                </label>
              </div>
              <div class="ue-bea-actions">
                <button type="button" class="btn-ghost" [disabled]="scanboxBusy()" (click)="discoverScanboxes()">
                  {{ (scanboxBusy() ? 'settings.users.dropscanTesting' : 'settings.users.dropscanTest') | transloco }}
                </button>
                @if (scanboxStatus() === 'ok') { <span class="ue-ok">{{ 'settings.users.dropscanTestOk' | transloco: { count: scanboxMsg() } }}</span> }
                @if (scanboxStatus() === 'error') { <span class="ue-error">{{ scanboxMsg() | transloco }}</span> }
              </div>
              <p class="ue-note">{{ 'settings.users.dropscanNote' | transloco }}</p>
            </fieldset>

            <fieldset class="ue-sec">
              <legend>{{ 'settings.users.sec.bea' | transloco }}</legend>
              @if (isNew()) {
                <p class="ue-hint">{{ 'settings.users.beaNewHint' | transloco }}</p>
              } @else {
                <p class="ue-bea-status">
                  {{ (draft().beaCertificatePresent ? 'settings.users.beaPresent' : 'settings.users.beaAbsent') | transloco }}
                </p>
                <div class="ue-grid">
                  <label class="ue-field">
                    <span>{{ 'settings.users.beaFile' | transloco }}</span>
                    <input type="file" accept=".p12,.pfx,.jks,application/x-pkcs12" (change)="onCertFile($event)" />
                  </label>
                  <label class="ue-field">
                    <span>{{ 'settings.users.beaPassword' | transloco }}</span>
                    <input type="password" autocomplete="new-password" [value]="certPassword()" (input)="certPassword.set($any($event.target).value)" />
                  </label>
                </div>
                <div class="ue-bea-actions">
                  <button type="button" class="btn-ghost" [disabled]="!certBase64() || certBusy()" (click)="uploadCert()">
                    {{ (certBusy() ? 'settings.saving' : 'settings.users.beaUpload') | transloco }}
                  </button>
                  @if (draft().beaCertificatePresent) {
                    <button type="button" class="btn-ghost danger" [disabled]="certBusy()" (click)="removeCert()">
                      {{ 'settings.users.beaRemove' | transloco }}
                    </button>
                  }
                  @if (certStatus() === 'ok') { <span class="ue-ok">{{ 'settings.savedOk' | transloco }}</span> }
                  @if (certStatus() === 'error') { <span class="ue-error">{{ 'settings.saveError' | transloco }}</span> }
                </div>
              }
            </fieldset>
          }
        }

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

    <ng-template #resTpl let-titleKey="titleKey" let-items="items" let-kind="kind">
      <fieldset class="ue-roles">
        <legend>{{ titleKey | transloco }}</legend>
        @if (isNew()) {
          <p class="ue-hint">{{ 'settings.users.resNewHint' | transloco }}</p>
        } @else if (!items.length) {
          <p class="ue-hint">{{ 'settings.empty' | transloco }}</p>
        } @else {
          @for (it of items; track it.id) {
            <label class="ue-role">
              <input type="checkbox" [checked]="isRes(kind, it.id)" (change)="toggleRes(kind, it.id)" />
              <span class="ue-role-name">{{ it.name }}</span>
            </label>
          }
        }
      </fieldset>
    </ng-template>
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
    .ue-tabs { display: flex; gap: 2px; padding: 0 12px; border-bottom: 1px solid var(--jl-line); overflow-x: auto; }
    .ue-tab { flex: 0 0 auto; font: inherit; font-size: .85rem; font-weight: 600; padding: 11px 14px; border: 0;
      border-bottom: 2px solid transparent; background: transparent; color: var(--jl-ink-soft); cursor: pointer; white-space: nowrap; }
    .ue-tab:hover { color: var(--jl-ink); }
    .ue-tab.on { color: var(--jl-blue); border-bottom-color: var(--jl-blue); }
    .ue-body { flex: 1 1 auto; overflow-y: auto; padding: 16px; display: flex; flex-direction: column; gap: 16px; min-height: 260px; }
    .ue-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    .ue-field { display: flex; flex-direction: column; gap: 5px; font-size: .8rem; color: var(--jl-ink-faint); }
    .ue-field input, .ue-field select { font: inherit; font-size: .9rem; color: var(--jl-ink);
      padding: 8px 10px; border: 1px solid var(--jl-line-strong); border-radius: 8px; background: var(--jl-surface); outline: none; }
    .ue-field input:focus, .ue-field select:focus { border-color: var(--jl-blue); }
    .ue-field input:disabled { opacity: .6; }
    .ue-check { display: flex; flex-direction: row; align-items: center; gap: 8px; color: var(--jl-ink); font-size: .9rem; }
    .ue-check input { width: 16px; height: 16px; }
    .ue-sec { border: 1px solid var(--jl-line); border-radius: 10px; padding: 12px 14px; margin: 0;
      display: flex; flex-direction: column; gap: 12px; }
    .ue-sec legend { font-size: .8rem; font-weight: 700; color: var(--jl-ink-soft); padding: 0 6px;
      text-transform: uppercase; letter-spacing: .03em; }
    .ue-roles { border: 1px solid var(--jl-line); border-radius: 10px; padding: 12px 14px; margin: 0;
      display: grid; grid-template-columns: 1fr 1fr; gap: 6px 16px; }
    .ue-roles legend { font-size: .8rem; font-weight: 700; color: var(--jl-ink-soft); padding: 0 6px;
      text-transform: uppercase; letter-spacing: .03em; }
    .ue-role { display: flex; align-items: baseline; gap: 7px; font-size: .86rem; color: var(--jl-ink); }
    .ue-role input { width: 15px; height: 15px; }
    .ue-role-name { font-weight: 600; }
    .ue-role-desc { color: var(--jl-ink-faint); font-size: .78rem; }
    .ue-note { margin: 0; font-size: .8rem; color: var(--jl-ink-soft); background: var(--jl-surface-alt);
      border-radius: 8px; padding: 9px 11px; }
    .ue-error { margin: 0; color: var(--jl-red); font-size: .84rem; }
    .ue-ok { color: var(--jl-green, #1a7f37); font-size: .84rem; }
    .ue-hint { grid-column: 1 / -1; margin: 0 0 4px; font-size: .78rem; color: var(--jl-ink-faint); }
    .ue-bea-status { margin: 0; font-size: .84rem; color: var(--jl-ink); }
    .ue-bea-actions { display: flex; align-items: center; gap: 12px; }
    .ue-foot { display: flex; justify-content: flex-end; gap: 8px; padding: 12px 16px; border-top: 1px solid var(--jl-line); }
    .icon-btn { display: inline-grid; place-items: center; width: 32px; height: 32px; border: 0; border-radius: 8px;
      background: transparent; color: var(--jl-ink-soft); cursor: pointer; }
    .btn-ghost { font: inherit; font-size: .86rem; padding: 8px 14px; border: 1px solid var(--jl-line-strong);
      border-radius: 8px; background: var(--jl-surface); color: var(--jl-ink); cursor: pointer; }
    .btn-ghost:disabled { opacity: .5; cursor: default; }
    .btn-ghost.danger { color: var(--jl-red); border-color: color-mix(in srgb, var(--jl-red) 40%, var(--jl-line-strong)); }
    .btn-primary { font: inherit; font-size: .86rem; font-weight: 600; padding: 8px 16px; border: 0; border-radius: 8px;
      background: var(--jl-blue); color: #fff; cursor: pointer; }
    .btn-primary:disabled { opacity: .55; cursor: default; }
    @media (max-width: 560px) { .ue-grid, .ue-roles { grid-template-columns: 1fr; } }
  `],
})
export class UserEditorComponent implements OnInit {
  private readonly api = inject(UsersAdminService);

  /** The user to edit; null creates a new one. */
  readonly user = input<AdminUser | null>(null);
  readonly allRoles = input<AdminRole[]>([]);
  readonly assignedRoles = input<string[]>([]);
  readonly allGroups = input<AdminGroup[]>([]);
  readonly assignedGroups = input<string[]>([]);
  readonly allCalendars = input<IdName[]>([]);
  readonly assignedCalendars = input<string[]>([]);
  readonly allMailboxes = input<IdName[]>([]);
  readonly assignedMailboxes = input<string[]>([]);
  readonly allInvoicePools = input<IdName[]>([]);
  readonly assignedInvoicePools = input<string[]>([]);
  readonly saving = input<boolean>(false);
  readonly error = input<string | null>(null);

  readonly save = output<UserEditResult>();
  readonly close = output<void>();

  protected readonly sections = SECTIONS;
  protected readonly tabs: { key: EditorTab; labelKey: string }[] = [
    { key: 'master', labelKey: 'settings.users.tab.master' },
    { key: 'permissions', labelKey: 'settings.users.tab.permissions' },
    { key: 'access', labelKey: 'settings.users.tab.access' },
    { key: 'integrations', labelKey: 'settings.users.tab.integrations' },
  ];
  protected readonly activeTab = signal<EditorTab>('master');
  protected readonly draft = signal<AdminUser>({});
  protected readonly selectedRoles = signal<Set<string>>(new Set());
  protected readonly selectedGroups = signal<Set<string>>(new Set());
  protected readonly selectedCalendars = signal<Set<string>>(new Set());
  protected readonly selectedMailboxes = signal<Set<string>>(new Set());
  protected readonly selectedInvoicePools = signal<Set<string>>(new Set());

  // beA certificate upload (existing users only)
  protected readonly certBase64 = signal('');
  protected readonly certPassword = signal('');
  protected readonly certBusy = signal(false);
  protected readonly certStatus = signal<'ok' | 'error' | null>(null);

  // Dropscan "Test / Scanboxen ermitteln"
  protected readonly scanboxBusy = signal(false);
  protected readonly scanboxStatus = signal<'ok' | 'error' | null>(null);
  protected readonly scanboxMsg = signal('');

  protected readonly isNew = computed(() => !this.user());
  protected readonly canSave = computed(() => !!(this.draft().principalId ?? '').trim());

  ngOnInit(): void {
    this.draft.set({ ...(this.user() ?? {}) });
    this.selectedRoles.set(new Set(this.assignedRoles()));
    this.selectedGroups.set(new Set(this.assignedGroups()));
    this.selectedCalendars.set(new Set(this.assignedCalendars()));
    this.selectedMailboxes.set(new Set(this.assignedMailboxes()));
    this.selectedInvoicePools.set(new Set(this.assignedInvoicePools()));
  }

  protected patch<K extends keyof AdminUser>(key: K, value: AdminUser[K]): void {
    this.draft.update((d) => ({ ...d, [key]: value }));
  }

  protected strVal(key: keyof AdminUser): string {
    const v = this.draft()[key];
    return v == null ? '' : String(v);
  }

  protected patchText(key: keyof AdminUser, value: string): void {
    this.patch(key, value as AdminUser[typeof key]);
  }

  /**
   * Tests the Dropscan API token and fills the scanbox field with the discovered ids — the web
   * equivalent of the desktop "Test / Scanboxen ermitteln". Sends the freshly typed token if any;
   * otherwise the server falls back to the user's stored token (write-only in the UI).
   */
  protected discoverScanboxes(): void {
    if (this.scanboxBusy()) { return; }
    const token = (this.draft().dropscanApiToken ?? '').trim();
    if (!token && !this.draft().dropscanApiTokenSet) {
      this.scanboxStatus.set('error');
      this.scanboxMsg.set('settings.users.dropscanNoToken');
      return;
    }
    this.scanboxBusy.set(true);
    this.scanboxStatus.set(null);
    this.scanboxMsg.set('');
    this.api.discoverScanboxes(token, this.draft().principalId).subscribe({
      next: (boxes) => {
        this.scanboxBusy.set(false);
        this.patch('dropscanScanboxes', (boxes ?? []).map((b) => b.id).join(','));
        this.scanboxStatus.set('ok');
        this.scanboxMsg.set(String((boxes ?? []).length));
      },
      error: () => {
        this.scanboxBusy.set(false);
        this.scanboxStatus.set('error');
        this.scanboxMsg.set('settings.users.dropscanTestFailed');
      },
    });
  }

  protected patchPort(value: string): void {
    const n = parseInt(value, 10);
    this.patch('cloudPort', Number.isFinite(n) ? n : 443);
  }

  protected toggleRole(role: string): void { this.toggle(this.selectedRoles, role); }
  protected toggleGroup(id: string): void { this.toggle(this.selectedGroups, id); }

  private setFor(kind: ResKind) {
    return kind === 'calendars' ? this.selectedCalendars
      : kind === 'mailboxes' ? this.selectedMailboxes : this.selectedInvoicePools;
  }
  protected isRes(kind: ResKind, id: string): boolean { return this.setFor(kind)().has(id); }
  protected toggleRes(kind: ResKind, id: string): void { this.toggle(this.setFor(kind), id); }

  private toggle(sig: { update: (fn: (s: Set<string>) => Set<string>) => void }, value: string): void {
    sig.update((set) => {
      const next = new Set(set);
      if (next.has(value)) { next.delete(value); } else { next.add(value); }
      return next;
    });
  }

  protected onCertFile(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    this.certStatus.set(null);
    if (!file) { this.certBase64.set(''); return; }
    const reader = new FileReader();
    reader.onload = () => {
      const result = String(reader.result);
      const comma = result.indexOf(',');
      this.certBase64.set(comma >= 0 ? result.slice(comma + 1) : result);
    };
    reader.readAsDataURL(file);
  }

  protected uploadCert(): void {
    const pid = this.user()?.principalId;
    if (!pid || !this.certBase64() || this.certBusy()) { return; }
    this.certBusy.set(true);
    this.certStatus.set(null);
    this.api.uploadBeaCertificate(pid, this.certBase64(), this.certPassword()).subscribe({
      next: () => { this.certBusy.set(false); this.certStatus.set('ok'); this.patch('beaCertificatePresent', true); this.certBase64.set(''); this.certPassword.set(''); },
      error: () => { this.certBusy.set(false); this.certStatus.set('error'); },
    });
  }

  protected removeCert(): void {
    const pid = this.user()?.principalId;
    if (!pid || this.certBusy()) { return; }
    this.certBusy.set(true);
    this.certStatus.set(null);
    this.api.removeBeaCertificate(pid).subscribe({
      next: () => { this.certBusy.set(false); this.patch('beaCertificatePresent', false); },
      error: () => { this.certBusy.set(false); this.certStatus.set('error'); },
    });
  }

  protected submit(): void {
    if (!this.canSave() || this.saving()) { return; }
    this.save.emit({
      user: this.draft(),
      roles: [...this.selectedRoles()],
      groups: [...this.selectedGroups()],
      calendars: [...this.selectedCalendars()],
      mailboxes: [...this.selectedMailboxes()],
      invoicePools: [...this.selectedInvoicePools()],
    });
  }
}
