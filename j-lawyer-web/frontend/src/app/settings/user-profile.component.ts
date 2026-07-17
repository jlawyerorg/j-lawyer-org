import {
  ChangeDetectionStrategy, Component, computed, inject, OnInit, output, signal,
} from '@angular/core';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { Profile, ProfileService, ProfileSettings } from './profile.service';

type ProfileTab = 'profile' | 'notifications' | 'security' | 'newCases';

/** A notification toggle: its settings key plus whether the feature is live yet (disabled if not). */
interface NotifyToggle { key: keyof ProfileSettings; labelKey: string; enabled: boolean; }

/** The 8 notification toggles, in the desktop dialog's order. Three are not live yet (disabled there too). */
const NOTIFY_TOGGLES: NotifyToggle[] = [
  { key: 'notifyInstantMessageMention', labelKey: 'profile.notify.imMention', enabled: true },
  { key: 'notifyInstantMessageMentionDone', labelKey: 'profile.notify.imMentionDone', enabled: true },
  { key: 'notifyCalendarEntry', labelKey: 'profile.notify.calendarEntry', enabled: true },
  { key: 'notifyCalendarEntryAuthored', labelKey: 'profile.notify.calendarEntryAuthored', enabled: true },
  { key: 'notifyCalendarEntryReminder', labelKey: 'profile.notify.calendarEntryReminder', enabled: false },
  { key: 'notifyInvoiceDue', labelKey: 'profile.notify.invoiceDue', enabled: true },
  { key: 'notifyScheduledDailyAgenda', labelKey: 'profile.notify.dailyAgenda', enabled: false },
  { key: 'notifyScheduledWeeklyDigest', labelKey: 'profile.notify.weeklyDigest', enabled: false },
];

/**
 * Self-service profile dialog for the logged-in user — the web equivalent of the desktop
 * {@code UserProfileDialog}, opened from the header avatar. Shows the read-only identity block
 * (Kürzel / primäre Gruppe / E-Mail) with a password change, and edits the per-user settings
 * (Benachrichtigungen / Sicherheit / Neue Akten) via the caller-scoped `/v8/profile` endpoint (needs
 * only `loginRole`). The avatar and the "Formatierungen" tab of the desktop dialog are intentionally
 * omitted, as is the desktop-only "KI-Werkzeuge" tab (no web AI assistant).
 */
@Component({
  selector: 'jl-user-profile',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent],
  template: `
    <div class="up-backdrop" (click)="close.emit()"></div>
    <div class="up-dialog" role="dialog" aria-modal="true">
      <header class="up-head">
        <h2>{{ 'profile.title' | transloco }}</h2>
        <button type="button" class="icon-btn" (click)="close.emit()" [attr.aria-label]="'settings.cancel' | transloco">
          <jl-icon name="close" [size]="18" />
        </button>
      </header>

      <nav class="up-tabs">
        @for (t of tabs; track t.key) {
          <button type="button" class="up-tab" [class.on]="activeTab() === t.key" (click)="activeTab.set(t.key)">
            {{ t.labelKey | transloco }}
          </button>
        }
      </nav>

      <div class="up-body">
        @if (loading()) {
          <p class="up-muted">{{ 'settings.loading' | transloco }}</p>
        } @else if (loadError()) {
          <p class="up-error">{{ 'settings.loadError' | transloco }}</p>
        } @else if (draft()) {
          @if (draft()!; as d) {
          @switch (activeTab()) {

            @case ('profile') {
              <fieldset class="up-sec">
                <legend>{{ 'profile.sec.identity' | transloco }}</legend>
                <div class="up-info">
                  <span class="up-info-label">{{ 'profile.abbreviation' | transloco }}</span>
                  <span class="up-info-value">{{ profile()?.abbreviation || '—' }}</span>
                </div>
                <div class="up-info">
                  <span class="up-info-label">{{ 'profile.primaryGroup' | transloco }}</span>
                  <span class="up-info-value">{{ profile()?.primaryGroupName || '—' }}</span>
                </div>
                <div class="up-info">
                  <span class="up-info-label">{{ 'profile.email' | transloco }}</span>
                  @if (profile()?.email) {
                    <span class="up-info-value">{{ profile()?.email }}</span>
                  } @else {
                    <span class="up-info-value warn">{{ 'profile.emailMissing' | transloco }}</span>
                  }
                </div>
              </fieldset>

              <fieldset class="up-sec">
                <legend>{{ 'profile.sec.password' | transloco }}</legend>
                <p class="up-note">{{ 'profile.passwordHint' | transloco }}</p>
                <div class="up-grid">
                  <label class="up-field">
                    <span>{{ 'profile.newPassword' | transloco }}</span>
                    <input type="password" autocomplete="new-password" [value]="pw1()"
                           (input)="pw1.set($any($event.target).value); pwStatus.set(null)" />
                  </label>
                  <label class="up-field">
                    <span>{{ 'profile.newPasswordRepeat' | transloco }}</span>
                    <input type="password" autocomplete="new-password" [value]="pw2()"
                           (input)="pw2.set($any($event.target).value); pwStatus.set(null)" />
                  </label>
                </div>
                @if (profile()?.passwordComplexityRequired) {
                  <p class="up-note" [class.bad]="pw1() && !complexOk()">{{ 'profile.passwordComplexity' | transloco }}</p>
                }
                <div class="up-pw-actions">
                  <button type="button" class="btn-ghost" [disabled]="!canChangePw() || pwBusy()" (click)="submitPassword()">
                    {{ (pwBusy() ? 'settings.saving' : 'profile.changePassword') | transloco }}
                  </button>
                  @if (pwStatus() === 'ok') { <span class="up-ok">{{ 'profile.passwordChanged' | transloco }}</span> }
                  @if (pwStatus() === 'mismatch') { <span class="up-error">{{ 'profile.passwordMismatch' | transloco }}</span> }
                  @if (pwStatus() === 'complexity') { <span class="up-error">{{ 'profile.passwordComplexity' | transloco }}</span> }
                  @if (pwStatus() === 'error') { <span class="up-error">{{ 'settings.saveError' | transloco }}</span> }
                </div>
              </fieldset>
            }

            @case ('notifications') {
              <fieldset class="up-sec">
                <legend>{{ 'profile.sec.notifications' | transloco }}</legend>
                @if (profile()?.email) {
                  <p class="up-note">{{ 'profile.notifyTarget' | transloco: { email: profile()?.email } }}</p>
                } @else {
                  <p class="up-note bad">{{ 'profile.emailMissing' | transloco }}</p>
                }
                @for (t of toggles; track t.key) {
                  <label class="up-check" [class.off]="!t.enabled">
                    <input type="checkbox" [checked]="boolVal(t.key)" [disabled]="!t.enabled"
                           (change)="patchBool(t.key, $any($event.target).checked)" />
                    <span>{{ t.labelKey | transloco }}</span>
                    @if (!t.enabled) { <span class="up-soon">{{ 'profile.comingSoon' | transloco }}</span> }
                  </label>
                }
              </fieldset>
            }

            @case ('security') {
              <fieldset class="up-sec">
                <legend>{{ 'profile.sec.security' | transloco }}</legend>
                <label class="up-check">
                  <input type="checkbox" [checked]="d.warnUnknownSenders"
                         (change)="patchBool('warnUnknownSenders', $any($event.target).checked)" />
                  <span>{{ 'profile.warnUnknownSenders' | transloco }}</span>
                </label>
              </fieldset>
            }

            @case ('newCases') {
              <fieldset class="up-sec">
                <legend>{{ 'profile.sec.ownerGroup' | transloco }}</legend>
                <label class="up-field" style="max-width: 320px;">
                  <select [value]="d.defaultOwnerGroup" (change)="patchOwner($any($event.target).value)">
                    <option value="">{{ 'profile.noGroup' | transloco }}</option>
                    @for (g of profile()?.memberGroups ?? []; track g.id) {
                      <option [value]="g.id">{{ g.name }}</option>
                    }
                  </select>
                </label>
              </fieldset>

              <fieldset class="up-sec">
                <legend>{{ 'profile.sec.allowedGroups' | transloco }}</legend>
                @if (!(profile()?.allGroups ?? []).length) {
                  <p class="up-note">{{ 'settings.empty' | transloco }}</p>
                } @else {
                  @for (g of profile()?.allGroups ?? []; track g.id) {
                    <label class="up-check">
                      <input type="checkbox" [checked]="isAllowed(g.id)" (change)="toggleAllowed(g.id)" />
                      <span>{{ g.name }}</span>
                    </label>
                  }
                }
              </fieldset>
            }
          }
          }
        }
      </div>

      <footer class="up-foot">
        <button type="button" class="btn-ghost" [disabled]="saving()" (click)="close.emit()">
          {{ 'settings.cancel' | transloco }}
        </button>
        @if (savedOk()) { <span class="up-ok">{{ 'settings.savedOk' | transloco }}</span> }
        @if (saveError()) { <span class="up-error">{{ 'settings.saveError' | transloco }}</span> }
        <button type="button" class="btn-primary" [disabled]="saving() || loading() || !!loadError()" (click)="save()">
          {{ (saving() ? 'settings.saving' : 'settings.save') | transloco }}
        </button>
      </footer>
    </div>
  `,
  styles: [`
    :host { display: contents; }
    .up-backdrop { position: fixed; inset: 0; background: rgba(4, 12, 20, .45); z-index: 42; }
    .up-dialog { position: fixed; z-index: 43; left: 50%; top: 50%; transform: translate(-50%, -50%);
      width: min(560px, calc(100vw - 32px)); max-height: calc(100vh - 48px); display: flex; flex-direction: column;
      background: var(--jl-surface); color: var(--jl-ink); border: 1px solid var(--jl-line-strong);
      border-radius: 14px; box-shadow: 0 24px 60px rgba(4, 12, 20, .4); }
    .up-head { display: flex; align-items: center; justify-content: space-between; gap: 8px; padding: 14px 16px; border-bottom: 1px solid var(--jl-line); }
    .up-head h2 { margin: 0; font-size: 1rem; font-weight: 700; }
    .up-tabs { display: flex; gap: 2px; padding: 0 12px; border-bottom: 1px solid var(--jl-line); overflow-x: auto; }
    .up-tab { flex: 0 0 auto; font: inherit; font-size: .85rem; font-weight: 600; padding: 11px 14px; border: 0;
      border-bottom: 2px solid transparent; background: transparent; color: var(--jl-ink-soft); cursor: pointer; white-space: nowrap; }
    .up-tab:hover { color: var(--jl-ink); }
    .up-tab.on { color: var(--jl-blue); border-bottom-color: var(--jl-blue); }
    .up-body { flex: 1 1 auto; overflow-y: auto; padding: 16px; display: flex; flex-direction: column; gap: 16px; min-height: 220px; }
    .up-muted { color: var(--jl-ink-soft); font-size: .88rem; }
    .up-sec { border: 1px solid var(--jl-line); border-radius: 10px; padding: 12px 14px; margin: 0;
      display: flex; flex-direction: column; gap: 10px; }
    .up-sec legend { font-size: .8rem; font-weight: 700; color: var(--jl-ink-soft); padding: 0 6px;
      text-transform: uppercase; letter-spacing: .03em; }
    .up-info { display: flex; align-items: baseline; gap: 10px; font-size: .9rem; }
    .up-info-label { flex: 0 0 140px; color: var(--jl-ink-faint); font-size: .82rem; }
    .up-info-value { font-weight: 600; color: var(--jl-ink); }
    .up-info-value.warn { color: #b26a00; font-weight: 500; }
    .up-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    .up-field { display: flex; flex-direction: column; gap: 5px; font-size: .8rem; color: var(--jl-ink-faint); }
    .up-field input, .up-field select { font: inherit; font-size: .9rem; color: var(--jl-ink);
      padding: 8px 10px; border: 1px solid var(--jl-line-strong); border-radius: 8px; background: var(--jl-surface); outline: none; }
    .up-field input:focus, .up-field select:focus { border-color: var(--jl-blue); }
    .up-check { display: flex; align-items: center; gap: 8px; font-size: .9rem; color: var(--jl-ink); }
    .up-check.off { color: var(--jl-ink-faint); }
    .up-check input { width: 16px; height: 16px; }
    .up-soon { font-size: .72rem; color: var(--jl-ink-faint); background: var(--jl-surface-alt); border-radius: 6px; padding: 1px 6px; }
    .up-note { margin: 0; font-size: .78rem; color: var(--jl-ink-faint); }
    .up-note.bad { color: var(--jl-red); }
    .up-pw-actions { display: flex; align-items: center; gap: 12px; }
    .up-ok { color: var(--jl-green, #1a7f37); font-size: .84rem; }
    .up-error { color: var(--jl-red); font-size: .84rem; margin: 0; }
    .up-foot { display: flex; align-items: center; justify-content: flex-end; gap: 12px; padding: 12px 16px; border-top: 1px solid var(--jl-line); }
    .icon-btn { display: inline-grid; place-items: center; width: 32px; height: 32px; border: 0; border-radius: 8px;
      background: transparent; color: var(--jl-ink-soft); cursor: pointer; }
    .btn-ghost { font: inherit; font-size: .86rem; padding: 8px 14px; border: 1px solid var(--jl-line-strong);
      border-radius: 8px; background: var(--jl-surface); color: var(--jl-ink); cursor: pointer; }
    .btn-ghost:disabled { opacity: .5; cursor: default; }
    .btn-primary { font: inherit; font-size: .86rem; font-weight: 600; padding: 8px 16px; border: 0; border-radius: 8px;
      background: var(--jl-blue); color: #fff; cursor: pointer; }
    .btn-primary:disabled { opacity: .55; cursor: default; }
    @media (max-width: 520px) { .up-grid { grid-template-columns: 1fr; } }
  `],
})
export class UserProfileComponent implements OnInit {
  private readonly api = inject(ProfileService);

  readonly close = output<void>();

  protected readonly toggles = NOTIFY_TOGGLES;
  protected readonly tabs: { key: ProfileTab; labelKey: string }[] = [
    { key: 'profile', labelKey: 'profile.tab.profile' },
    { key: 'notifications', labelKey: 'profile.tab.notifications' },
    { key: 'security', labelKey: 'profile.tab.security' },
    { key: 'newCases', labelKey: 'profile.tab.newCases' },
  ];
  protected readonly activeTab = signal<ProfileTab>('profile');

  protected readonly profile = signal<Profile | null>(null);
  protected readonly draft = signal<ProfileSettings | null>(null);
  protected readonly loading = signal(false);
  protected readonly loadError = signal(false);
  protected readonly saving = signal(false);
  protected readonly saveError = signal(false);
  protected readonly savedOk = signal(false);

  // password change (own sub-form / endpoint)
  protected readonly pw1 = signal('');
  protected readonly pw2 = signal('');
  protected readonly pwBusy = signal(false);
  protected readonly pwStatus = signal<'ok' | 'mismatch' | 'complexity' | 'error' | null>(null);

  // Mirrors PasswordsUtil.COMPLEXITY_STRONG: >=8 non-space chars with digit, lower, upper and a special char.
  private readonly strongRe = /^(?=\S*[0-9])(?=\S*[a-z])(?=\S*[A-Z])(?=\S*[-_@#$%^&+=])\S{8,}$/;

  protected readonly complexOk = computed(() =>
    !this.profile()?.passwordComplexityRequired || this.strongRe.test(this.pw1()));
  protected readonly canChangePw = computed(() =>
    this.pw1().length > 0 && this.pw2().length > 0);

  ngOnInit(): void {
    this.loading.set(true);
    this.api.getProfile().subscribe({
      next: (p) => { this.profile.set(p); this.draft.set({ ...p.settings }); this.loading.set(false); },
      error: () => { this.loadError.set(true); this.loading.set(false); },
    });
  }

  protected boolVal(key: keyof ProfileSettings): boolean {
    return this.draft()?.[key] === true;
  }

  protected patchBool(key: keyof ProfileSettings, value: boolean): void {
    this.draft.update((d) => (d ? { ...d, [key]: value } : d));
    this.savedOk.set(false);
  }

  protected patchOwner(value: string): void {
    this.draft.update((d) => (d ? { ...d, defaultOwnerGroup: value } : d));
    this.savedOk.set(false);
  }

  protected isAllowed(id: string): boolean {
    return this.draft()?.defaultAllowedGroups?.includes(id) ?? false;
  }

  protected toggleAllowed(id: string): void {
    this.draft.update((d) => {
      if (!d) { return d; }
      const set = new Set(d.defaultAllowedGroups ?? []);
      if (set.has(id)) { set.delete(id); } else { set.add(id); }
      return { ...d, defaultAllowedGroups: [...set] };
    });
    this.savedOk.set(false);
  }

  protected save(): void {
    const d = this.draft();
    if (!d || this.saving()) { return; }
    this.saving.set(true); this.saveError.set(false); this.savedOk.set(false);
    this.api.saveSettings(d).subscribe({
      next: () => { this.saving.set(false); this.savedOk.set(true); },
      error: () => { this.saving.set(false); this.saveError.set(true); },
    });
  }

  protected submitPassword(): void {
    if (!this.canChangePw() || this.pwBusy()) { return; }
    if (this.pw1() !== this.pw2()) { this.pwStatus.set('mismatch'); return; }
    if (!this.complexOk()) { this.pwStatus.set('complexity'); return; }
    this.pwBusy.set(true); this.pwStatus.set(null);
    this.api.changePassword(this.pw1()).subscribe({
      next: () => { this.pwBusy.set(false); this.pwStatus.set('ok'); this.pw1.set(''); this.pw2.set(''); },
      error: (e) => {
        this.pwBusy.set(false);
        this.pwStatus.set(e?.error?.error === 'password_complexity' ? 'complexity' : 'error');
      },
    });
  }
}
