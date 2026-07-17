import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { TranslocoModule } from '@jsverse/transloco';
import { AuthService } from '../core/auth/auth.service';
import { SystemMailbox, SystemSettingsService } from './system-settings.service';

const EMPTY: SystemMailbox = {
  smtpServer: '', smtpPort: '', smtpUser: '', password: '', passwordSet: false,
  senderEmail: '', senderName: '', recipient: '', ssl: false, startTls: false,
};

/**
 * Editor for the system mailbox (outbound SMTP) used by the server to send administrative and
 * business notifications — the web equivalent of the desktop "Systempostfach" dialog. Needs
 * `sysAdminRole`. The password is write-only (never returned; only applied when re-entered).
 */
@Component({
  selector: 'jl-system-mailbox',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule],
  template: `
    <div class="ss">
      @if (!canEdit()) { <p class="ss-hint">{{ 'settings.readOnlyHint' | transloco }}</p> }
      @if (loading()) {
        <p class="ss-muted">{{ 'settings.loading' | transloco }}</p>
      } @else if (loadError()) {
        <p class="ss-error">{{ 'settings.loadError' | transloco }}</p>
      } @else {
        <p class="ss-note">{{ 'settings.mailbox.hint' | transloco }}</p>
        <div class="ss-grid">
          <label class="ss-field"><span>{{ 'settings.mailbox.server' | transloco }}</span><input type="text" [value]="draft().smtpServer" [disabled]="!canEdit()" (input)="patch('smtpServer', $any($event.target).value)" /></label>
          <label class="ss-field"><span>{{ 'settings.mailbox.port' | transloco }}</span><input type="text" [value]="draft().smtpPort" [disabled]="!canEdit()" (input)="patch('smtpPort', $any($event.target).value)" /></label>
          <label class="ss-field"><span>{{ 'settings.mailbox.user' | transloco }}</span><input type="text" autocomplete="off" [value]="draft().smtpUser" [disabled]="!canEdit()" (input)="patch('smtpUser', $any($event.target).value)" /></label>
          <label class="ss-field"><span>{{ 'settings.mailbox.password' | transloco }}</span><input type="password" autocomplete="new-password" [value]="draft().password" [disabled]="!canEdit()" [attr.placeholder]="draft().passwordSet ? ('settings.mailbox.passwordKeep' | transloco) : ''" (input)="patch('password', $any($event.target).value)" /></label>
          <label class="ss-field"><span>{{ 'settings.mailbox.senderEmail' | transloco }}</span><input type="text" [value]="draft().senderEmail" [disabled]="!canEdit()" (input)="patch('senderEmail', $any($event.target).value)" /></label>
          <label class="ss-field"><span>{{ 'settings.mailbox.senderName' | transloco }}</span><input type="text" [value]="draft().senderName" [disabled]="!canEdit()" (input)="patch('senderName', $any($event.target).value)" /></label>
          <label class="ss-field"><span>{{ 'settings.mailbox.recipient' | transloco }}</span><input type="text" [value]="draft().recipient" [disabled]="!canEdit()" (input)="patch('recipient', $any($event.target).value)" /></label>
        </div>
        <label class="ss-check"><input type="checkbox" [checked]="draft().ssl" [disabled]="!canEdit()" (change)="patch('ssl', $any($event.target).checked)" /><span>SSL</span></label>
        <label class="ss-check"><input type="checkbox" [checked]="draft().startTls" [disabled]="!canEdit()" (change)="patch('startTls', $any($event.target).checked)" /><span>StartTLS</span></label>
        <div class="ss-foot">
          @if (testStatus() === 'ok') { <span class="ss-ok">{{ 'settings.mailbox.testOk' | transloco }}</span> }
          @if (testStatus() === 'error') { <span class="ss-error">{{ testError() || ('settings.mailbox.testError' | transloco) }}</span> }
          @if (savedOk()) { <span class="ss-ok">{{ 'settings.savedOk' | transloco }}</span> }
          @if (saveError()) { <span class="ss-error">{{ 'settings.saveError' | transloco }}</span> }
          <button type="button" class="btn-ghost" [disabled]="!canEdit() || testing() || !draft().recipient" (click)="test()">{{ (testing() ? 'settings.mailbox.testing' : 'settings.mailbox.test') | transloco }}</button>
          <button type="button" class="btn-primary" [disabled]="!canEdit() || saving()" (click)="submit()">{{ (saving() ? 'settings.saving' : 'settings.save') | transloco }}</button>
        </div>
      }
    </div>
  `,
  styles: [`
    :host { display: block; }
    .ss { max-width: 720px; display: flex; flex-direction: column; gap: 12px; }
    .ss-hint { margin: 0; font-size: .82rem; color: #b26a00; }
    .ss-muted { color: var(--jl-ink-soft); font-size: .88rem; }
    .ss-error { color: var(--jl-red); font-size: .84rem; }
    .ss-note { margin: 0; font-size: .82rem; color: var(--jl-ink-soft); }
    .ss-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    .ss-field { display: flex; flex-direction: column; gap: 5px; font-size: .8rem; color: var(--jl-ink-faint); }
    .ss-field input { font: inherit; font-size: .9rem; color: var(--jl-ink); padding: 8px 10px; border: 1px solid var(--jl-line-strong); border-radius: 8px; background: var(--jl-surface); outline: none; }
    .ss-field input:focus { border-color: var(--jl-blue); }
    .ss-field input:disabled { opacity: .6; }
    .ss-check { display: flex; align-items: center; gap: 8px; font-size: .9rem; color: var(--jl-ink); }
    .ss-check input { width: 16px; height: 16px; }
    .ss-foot { display: flex; align-items: center; justify-content: flex-end; gap: 12px; flex-wrap: wrap; }
    .ss-ok { color: var(--jl-green, #1a7f37); font-size: .84rem; }
    .btn-ghost { font: inherit; font-size: .86rem; padding: 9px 16px; border: 1px solid var(--jl-line-strong); border-radius: 8px; background: var(--jl-surface); color: var(--jl-ink); cursor: pointer; }
    .btn-ghost:disabled { opacity: .5; cursor: default; }
    .btn-primary { font: inherit; font-size: .86rem; font-weight: 600; padding: 9px 18px; border: 0; border-radius: 8px; background: var(--jl-blue); color: #fff; cursor: pointer; }
    .btn-primary:disabled { opacity: .55; cursor: default; }
    @media (max-width: 560px) { .ss-grid { grid-template-columns: 1fr; } }
  `],
})
export class SystemMailboxComponent implements OnInit {
  private readonly api = inject(SystemSettingsService);
  private readonly auth = inject(AuthService);

  protected readonly draft = signal<SystemMailbox>({ ...EMPTY });
  protected readonly loading = signal(false);
  protected readonly loadError = signal(false);
  protected readonly saving = signal(false);
  protected readonly saveError = signal(false);
  protected readonly savedOk = signal(false);
  protected readonly testing = signal(false);
  protected readonly testStatus = signal<'ok' | 'error' | null>(null);
  protected readonly testError = signal<string | null>(null);
  protected readonly canEdit = computed(() => this.auth.hasRole('sysAdminRole'));

  ngOnInit(): void {
    this.loading.set(true);
    this.api.getSystemMailbox().subscribe({
      next: (s) => { this.draft.set({ ...s, password: '' }); this.loading.set(false); },
      error: () => { this.loadError.set(true); this.loading.set(false); },
    });
  }

  protected patch<K extends keyof SystemMailbox>(key: K, value: SystemMailbox[K]): void {
    this.draft.update((d) => ({ ...d, [key]: value }));
    this.savedOk.set(false);
    this.testStatus.set(null);
  }

  protected test(): void {
    if (!this.canEdit() || this.testing()) { return; }
    this.testing.set(true); this.testStatus.set(null); this.testError.set(null);
    this.api.testSystemMailbox(this.draft()).subscribe({
      next: () => { this.testing.set(false); this.testStatus.set('ok'); },
      error: (e: HttpErrorResponse) => {
        this.testing.set(false); this.testStatus.set('error');
        const msg = e?.error?.error;
        this.testError.set(typeof msg === 'string' ? msg : null);
      },
    });
  }

  protected submit(): void {
    if (!this.canEdit() || this.saving()) { return; }
    this.saving.set(true); this.saveError.set(false); this.savedOk.set(false);
    this.api.saveSystemMailbox(this.draft()).subscribe({
      next: (s) => { this.draft.set({ ...s, password: '' }); this.saving.set(false); this.savedOk.set(true); },
      error: () => { this.saving.set(false); this.saveError.set(true); },
    });
  }
}
