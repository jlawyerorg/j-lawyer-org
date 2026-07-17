import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { TranslocoModule } from '@jsverse/transloco';
import { AuthService } from '../core/auth/auth.service';
import { BackupSettings, SystemSettingsService } from './system-settings.service';

type DayKey = 'monday' | 'tuesday' | 'wednesday' | 'thursday' | 'friday' | 'saturday' | 'sunday';

const EMPTY: BackupSettings = {
  enabled: false, hour: 22,
  monday: false, tuesday: false, wednesday: false, thursday: false, friday: false, saturday: false, sunday: false,
  dbHost: 'localhost', dbPort: 3306, dbName: 'jlawyerdb', dbUser: 'root', dbPassword: '', dbPasswordSet: false,
  encryptionPassword: '', encryptionPasswordSet: false, syncTarget: '', exportTarget: '',
};

/**
 * Editor for the server data-backup (Datensicherung) configuration: whether scheduled backups run,
 * the weekday/hour schedule, the database connection, the encryption password and sync/export
 * targets. Passwords are write-only (left blank keeps the stored value). Needs `sysAdminRole`.
 */
@Component({
  selector: 'jl-backup-settings',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule],
  template: `
    <div class="bk">
      @if (!canEdit()) { <p class="bk-hint">{{ 'settings.readOnlyHint' | transloco }}</p> }
      @if (loading()) {
        <p class="bk-muted">{{ 'settings.loading' | transloco }}</p>
      } @else if (loadError()) {
        <p class="bk-error">{{ 'settings.loadError' | transloco }}</p>
      } @else {
        <fieldset class="bk-sec">
          <legend>{{ 'settings.backup.schedule' | transloco }}</legend>
          <label class="bk-check">
            <input type="checkbox" [checked]="draft().enabled" [disabled]="!canEdit()"
                   (change)="patch('enabled', $any($event.target).checked)" />
            <span>{{ 'settings.backup.enabled' | transloco }}</span>
          </label>
          <div class="bk-days">
            @for (d of days; track d.key) {
              <label class="bk-day">
                <input type="checkbox" [checked]="$any(draft())[d.key]" [disabled]="!canEdit()"
                       (change)="patch(d.key, $any($event.target).checked)" />
                <span>{{ d.labelKey | transloco }}</span>
              </label>
            }
          </div>
          <label class="bk-field bk-hour">
            <span>{{ 'settings.backup.hour' | transloco }}</span>
            <select [value]="draft().hour" [disabled]="!canEdit()" (change)="patchNum('hour', $any($event.target).value)">
              @for (h of hours; track h) { <option [value]="h">{{ h }}:00</option> }
            </select>
          </label>
        </fieldset>

        <fieldset class="bk-sec">
          <legend>{{ 'settings.backup.database' | transloco }}</legend>
          <div class="bk-grid">
            <label class="bk-field">
              <span>{{ 'settings.backup.dbHost' | transloco }}</span>
              <input type="text" [value]="draft().dbHost" [disabled]="!canEdit()" (input)="patch('dbHost', $any($event.target).value)" />
            </label>
            <label class="bk-field">
              <span>{{ 'settings.backup.dbPort' | transloco }}</span>
              <input type="number" [value]="draft().dbPort" [disabled]="!canEdit()" (input)="patchNum('dbPort', $any($event.target).value)" />
            </label>
            <label class="bk-field">
              <span>{{ 'settings.backup.dbName' | transloco }}</span>
              <input type="text" [value]="draft().dbName" [disabled]="!canEdit()" (input)="patch('dbName', $any($event.target).value)" />
            </label>
            <label class="bk-field">
              <span>{{ 'settings.backup.dbUser' | transloco }}</span>
              <input type="text" [value]="draft().dbUser" [disabled]="!canEdit()" (input)="patch('dbUser', $any($event.target).value)" />
            </label>
            <label class="bk-field">
              <span>{{ 'settings.backup.dbPassword' | transloco }}</span>
              <input type="password" autocomplete="new-password" [value]="draft().dbPassword" [disabled]="!canEdit()"
                     [placeholder]="(draft().dbPasswordSet ? 'settings.backup.pwdKeep' : 'settings.backup.pwdNone') | transloco"
                     (input)="patch('dbPassword', $any($event.target).value)" />
            </label>
          </div>
        </fieldset>

        <fieldset class="bk-sec">
          <legend>{{ 'settings.backup.encryption' | transloco }}</legend>
          <label class="bk-field">
            <span>{{ 'settings.backup.encryptionPassword' | transloco }}</span>
            <input type="password" autocomplete="new-password" [value]="draft().encryptionPassword" [disabled]="!canEdit()"
                   [placeholder]="(draft().encryptionPasswordSet ? 'settings.backup.pwdKeep' : 'settings.backup.pwdNone') | transloco"
                   (input)="patch('encryptionPassword', $any($event.target).value)" />
          </label>
          <p class="bk-note">{{ 'settings.backup.encryptionHint' | transloco }}</p>
        </fieldset>

        <fieldset class="bk-sec">
          <legend>{{ 'settings.backup.targets' | transloco }}</legend>
          <div class="bk-grid">
            <div class="bk-field">
              <span>{{ 'settings.backup.syncTarget' | transloco }}</span>
              <div class="bk-with-btn">
                <input type="text" [value]="draft().syncTarget" [disabled]="!canEdit()" (input)="patch('syncTarget', $any($event.target).value)" />
                <button type="button" class="bk-helper" [disabled]="!canEdit()" (click)="openHelper()">{{ 'settings.backup.helper' | transloco }}</button>
              </div>
            </div>
            <label class="bk-field">
              <span>{{ 'settings.backup.exportTarget' | transloco }}</span>
              <input type="text" [value]="draft().exportTarget" [disabled]="!canEdit()" (input)="patch('exportTarget', $any($event.target).value)" />
            </label>
          </div>
        </fieldset>

        <div class="bk-foot">
          @if (savedOk()) { <span class="bk-ok">{{ 'settings.savedOk' | transloco }}</span> }
          @if (saveError()) { <span class="bk-error">{{ 'settings.saveError' | transloco }}</span> }
          <button type="button" class="btn-primary" [disabled]="!canEdit() || saving()" (click)="submit()">
            {{ (saving() ? 'settings.saving' : 'settings.save') | transloco }}
          </button>
        </div>
      }
    </div>

    @if (helperOpen()) {
      <div class="hl-backdrop" (click)="helperOpen.set(false)"></div>
      <div class="hl-dialog" role="dialog" aria-modal="true">
        <header class="hl-head"><h2>{{ 'settings.backup.helperTitle' | transloco }}</h2></header>
        <div class="hl-body">
          <label class="bk-field">
            <span>{{ 'settings.backup.hlType' | transloco }}</span>
            <select [value]="hlType()" (change)="hlType.set($any($event.target).value)">
              <option value="sftp">{{ 'settings.backup.hlSftp' | transloco }}</option>
              <option value="ftp">{{ 'settings.backup.hlFtp' | transloco }}</option>
              <option value="smb">{{ 'settings.backup.hlSmb' | transloco }}</option>
              <option value="file">{{ 'settings.backup.hlLocal' | transloco }}</option>
            </select>
          </label>
          @if (hlType() !== 'file') {
            <div class="bk-grid">
              <label class="bk-field">
                <span>{{ 'settings.backup.hlHost' | transloco }}</span>
                <input type="text" [value]="hlHost()" (input)="hlHost.set($any($event.target).value)" />
              </label>
              <label class="bk-field">
                <span>{{ 'settings.backup.hlUser' | transloco }}</span>
                <input type="text" [value]="hlUser()" (input)="hlUser.set($any($event.target).value)" />
              </label>
              <label class="bk-field">
                <span>{{ 'settings.backup.hlPassword' | transloco }}</span>
                <input type="password" autocomplete="new-password" [value]="hlPassword()" (input)="hlPassword.set($any($event.target).value)" />
              </label>
            </div>
          }
          <label class="bk-field">
            <span>{{ 'settings.backup.hlFolder' | transloco }}</span>
            <input type="text" [value]="hlFolder()" (input)="hlFolder.set($any($event.target).value)" />
          </label>
          <div class="hl-preview">
            <span class="hl-preview-label">{{ 'settings.backup.hlPreview' | transloco }}</span>
            <code>{{ assembled() || '—' }}</code>
          </div>
          @if (hlError()) { <p class="bk-error">{{ hlError() }}</p> }
        </div>
        <footer class="hl-foot">
          <button type="button" class="btn-ghost" [disabled]="hlValidating()" (click)="helperOpen.set(false)">{{ 'settings.cancel' | transloco }}</button>
          <button type="button" class="btn-primary" [disabled]="hlValidating() || !assembled()" (click)="applyHelper()">
            {{ (hlValidating() ? 'settings.backup.hlChecking' : 'settings.backup.hlApply') | transloco }}
          </button>
        </footer>
      </div>
    }
  `,
  styles: [`
    :host { display: block; }
    .bk { max-width: 680px; }
    .bk-hint { margin: 0 0 12px; font-size: .82rem; color: #b26a00; }
    .bk-muted { color: var(--jl-ink-soft); font-size: .88rem; }
    .bk-error { color: var(--jl-red); font-size: .84rem; }
    .bk-sec { border: 1px solid var(--jl-line); border-radius: 10px; padding: 12px 14px 14px; margin: 0 0 14px;
      display: flex; flex-direction: column; gap: 12px; }
    .bk-sec legend { font-size: .8rem; font-weight: 700; color: var(--jl-ink-soft); padding: 0 6px; text-transform: uppercase; letter-spacing: .04em; }
    .bk-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    .bk-field { display: flex; flex-direction: column; gap: 5px; font-size: .8rem; color: var(--jl-ink-faint); }
    .bk-field input, .bk-field select { font: inherit; font-size: .9rem; color: var(--jl-ink); padding: 8px 10px;
      border: 1px solid var(--jl-line-strong); border-radius: 8px; background: var(--jl-surface); outline: none; }
    .bk-field input:focus, .bk-field select:focus { border-color: var(--jl-blue); }
    .bk-field input:disabled, .bk-field select:disabled { opacity: .6; }
    .bk-hour { max-width: 160px; }
    .bk-check { display: flex; align-items: center; gap: 8px; font-size: .9rem; color: var(--jl-ink); }
    .bk-check input { width: 16px; height: 16px; }
    .bk-days { display: flex; flex-wrap: wrap; gap: 10px 16px; }
    .bk-day { display: flex; align-items: center; gap: 6px; font-size: .86rem; color: var(--jl-ink); }
    .bk-day input { width: 15px; height: 15px; }
    .bk-note { margin: 0; font-size: .78rem; color: var(--jl-ink-faint); }
    .bk-foot { display: flex; align-items: center; justify-content: flex-end; gap: 12px; }
    .bk-ok { color: var(--jl-green, #1a7f37); font-size: .84rem; }
    .btn-primary { font: inherit; font-size: .86rem; font-weight: 600; padding: 9px 18px; border: 0; border-radius: 8px;
      background: var(--jl-blue); color: #fff; cursor: pointer; }
    .btn-primary:disabled { opacity: .55; cursor: default; }
    .bk-with-btn { display: flex; gap: 8px; }
    .bk-with-btn input { flex: 1 1 auto; }
    .bk-helper { flex: 0 0 auto; font: inherit; font-size: .82rem; padding: 0 12px; border: 1px solid var(--jl-line-strong);
      border-radius: 8px; background: var(--jl-surface); color: var(--jl-ink); cursor: pointer; }
    .bk-helper:hover:not(:disabled) { border-color: var(--jl-blue); color: var(--jl-blue); }
    .bk-helper:disabled { opacity: .5; cursor: default; }
    .hl-backdrop { position: fixed; inset: 0; background: rgba(4, 12, 20, .45); z-index: 44; }
    .hl-dialog { position: fixed; z-index: 45; left: 50%; top: 50%; transform: translate(-50%, -50%);
      width: min(560px, calc(100vw - 32px)); max-height: calc(100vh - 48px); display: flex; flex-direction: column;
      background: var(--jl-surface); color: var(--jl-ink); border: 1px solid var(--jl-line-strong); border-radius: 14px;
      box-shadow: 0 24px 60px rgba(4, 12, 20, .4); }
    .hl-head { padding: 14px 16px; border-bottom: 1px solid var(--jl-line); }
    .hl-head h2 { margin: 0; font-size: 1rem; font-weight: 700; }
    .hl-body { flex: 1 1 auto; overflow-y: auto; padding: 16px; display: flex; flex-direction: column; gap: 12px; }
    .hl-preview { display: flex; flex-direction: column; gap: 4px; padding: 8px 10px; background: var(--jl-surface-alt); border-radius: 8px; }
    .hl-preview-label { font-size: .74rem; color: var(--jl-ink-faint); text-transform: uppercase; letter-spacing: .03em; }
    .hl-preview code { font-family: ui-monospace, Menlo, Consolas, monospace; font-size: .85rem; color: var(--jl-ink); word-break: break-all; }
    .hl-foot { display: flex; justify-content: flex-end; gap: 8px; padding: 12px 16px; border-top: 1px solid var(--jl-line); }
    .btn-ghost { font: inherit; font-size: .86rem; padding: 8px 14px; border: 1px solid var(--jl-line-strong);
      border-radius: 8px; background: var(--jl-surface); color: var(--jl-ink); cursor: pointer; }
    .btn-ghost:disabled { opacity: .5; cursor: default; }
    @media (max-width: 560px) { .bk-grid { grid-template-columns: 1fr; } }
  `],
})
export class BackupSettingsComponent implements OnInit {
  private readonly api = inject(SystemSettingsService);
  private readonly auth = inject(AuthService);

  protected readonly days: { key: DayKey; labelKey: string }[] = [
    { key: 'monday', labelKey: 'settings.backup.mon' },
    { key: 'tuesday', labelKey: 'settings.backup.tue' },
    { key: 'wednesday', labelKey: 'settings.backup.wed' },
    { key: 'thursday', labelKey: 'settings.backup.thu' },
    { key: 'friday', labelKey: 'settings.backup.fri' },
    { key: 'saturday', labelKey: 'settings.backup.sat' },
    { key: 'sunday', labelKey: 'settings.backup.sun' },
  ];
  protected readonly hours = Array.from({ length: 24 }, (_, i) => i);

  protected readonly draft = signal<BackupSettings>({ ...EMPTY });
  protected readonly loading = signal(false);
  protected readonly loadError = signal(false);
  protected readonly saving = signal(false);
  protected readonly saveError = signal(false);
  protected readonly savedOk = signal(false);
  protected readonly canEdit = computed(() => this.auth.hasRole('sysAdminRole'));

  // sync-target helper dialog
  protected readonly helperOpen = signal(false);
  protected readonly hlType = signal<'sftp' | 'ftp' | 'smb' | 'file'>('sftp');
  protected readonly hlHost = signal('');
  protected readonly hlUser = signal('');
  protected readonly hlPassword = signal('');
  protected readonly hlFolder = signal('');
  protected readonly hlValidating = signal(false);
  protected readonly hlError = signal<string | null>(null);

  /** Assembles the target URL from the helper fields, mirroring the desktop dialog exactly. */
  protected readonly assembled = computed(() => {
    const protocol = this.hlType() + '://';
    const local = this.hlType() === 'file';
    const server = local ? '' : this.hlHost();
    const user = this.hlUser();
    const pwd = this.hlPassword();
    const credentials = (!local && user.length > 0 && pwd.length > 0) ? `${user}:${pwd}@` : '';
    let folder = this.hlFolder().replace(/\\/g, '/');
    if (!folder.startsWith('/')) { folder = '/' + folder; }
    if (folder.length === 1) { folder = ''; }
    if (this.hlType() === 'smb' && folder.length > 0 && !folder.endsWith('/')) { folder = folder + '/'; }
    if (local) { return protocol + folder; }
    if (!server) { return ''; }
    return protocol + credentials + server + folder;
  });

  ngOnInit(): void {
    this.loading.set(true);
    this.api.getBackup().subscribe({
      next: (s) => { this.draft.set({ ...EMPTY, ...s, dbPassword: '', encryptionPassword: '' }); this.loading.set(false); },
      error: () => { this.loadError.set(true); this.loading.set(false); },
    });
  }

  protected patch<K extends keyof BackupSettings>(key: K, value: BackupSettings[K]): void {
    this.draft.update((d) => ({ ...d, [key]: value }));
    this.savedOk.set(false);
  }

  protected patchNum(key: 'hour' | 'dbPort', value: string): void {
    const n = parseInt(value, 10);
    this.patch(key, (Number.isFinite(n) ? n : 0) as BackupSettings[typeof key]);
  }

  protected openHelper(): void {
    if (!this.canEdit()) { return; }
    this.hlType.set('sftp');
    this.hlHost.set(''); this.hlUser.set(''); this.hlPassword.set(''); this.hlFolder.set('');
    this.hlError.set(null);
    this.helperOpen.set(true);
  }

  /** Validates the assembled location server-side; on success fills the (editable) sync-target field. */
  protected applyHelper(): void {
    const target = this.assembled();
    if (!target || this.hlValidating()) { return; }
    this.hlValidating.set(true);
    this.hlError.set(null);
    this.api.validateStorageLocation(target).subscribe({
      next: (r) => {
        this.hlValidating.set(false);
        if (r.valid) {
          this.patch('syncTarget', target);
          this.helperOpen.set(false);
        } else {
          this.hlError.set(r.message || 'invalid');
        }
      },
      error: () => { this.hlValidating.set(false); this.hlError.set('error'); },
    });
  }

  protected submit(): void {
    if (!this.canEdit() || this.saving()) { return; }
    this.saving.set(true);
    this.saveError.set(false);
    this.savedOk.set(false);
    this.api.saveBackup(this.draft()).subscribe({
      next: (s) => { this.saving.set(false); this.savedOk.set(true); this.draft.set({ ...EMPTY, ...s, dbPassword: '', encryptionPassword: '' }); },
      error: () => { this.saving.set(false); this.saveError.set(true); },
    });
  }
}
