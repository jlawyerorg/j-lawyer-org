import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { TranslocoModule } from '@jsverse/transloco';
import { AuthService } from '../core/auth/auth.service';
import { CardDavSyncSettings, CloudAddressBook, SystemSettingsService } from './system-settings.service';

const EMPTY: CardDavSyncSettings = {
  enabled: false, birthdaySync: true, host: '', port: 443, ssl: true, path: '', user: '',
  password: '', passwordSet: false, href: '',
};

/**
 * Editor for the address-book → Nextcloud/CardDAV synchronization (global server config): connection,
 * target address book, birthday sync, plus an on-demand "sync now" trigger. Only contacts linked to a
 * case are synced. The password is write-only. Needs `sysAdminRole` (also enforced server-side).
 */
@Component({
  selector: 'jl-carddav-sync',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule],
  template: `
    <div class="cs">
      @if (!canEdit()) { <p class="cs-hint">{{ 'settings.readOnlyHint' | transloco }}</p> }
      @if (loading()) {
        <p class="cs-muted">{{ 'settings.loading' | transloco }}</p>
      } @else if (loadError()) {
        <p class="cs-error">{{ 'settings.loadError' | transloco }}</p>
      } @else {
        <fieldset class="cs-sec">
          <legend>{{ 'settings.carddav.general' | transloco }}</legend>
          <label class="cs-check">
            <input type="checkbox" [checked]="draft().enabled" [disabled]="!canEdit()" (change)="patch('enabled', $any($event.target).checked)" />
            <span>{{ 'settings.carddav.enabled' | transloco }}</span>
          </label>
          <label class="cs-check">
            <input type="checkbox" [checked]="draft().birthdaySync" [disabled]="!canEdit()" (change)="patch('birthdaySync', $any($event.target).checked)" />
            <span>{{ 'settings.carddav.birthdaySync' | transloco }}</span>
          </label>
          <p class="cs-note">{{ 'settings.carddav.scopeHint' | transloco }}</p>
        </fieldset>

        <fieldset class="cs-sec">
          <legend>{{ 'settings.carddav.connection' | transloco }}</legend>
          <div class="cs-grid">
            <label class="cs-field">
              <span>{{ 'settings.carddav.host' | transloco }}</span>
              <input type="text" [value]="draft().host" [disabled]="!canEdit()" (input)="patch('host', $any($event.target).value)" />
            </label>
            <label class="cs-field">
              <span>{{ 'settings.carddav.port' | transloco }}</span>
              <input type="number" [value]="draft().port" [disabled]="!canEdit()" (input)="patchPort($any($event.target).value)" />
            </label>
            <label class="cs-field">
              <span>{{ 'settings.carddav.user' | transloco }}</span>
              <input type="text" [value]="draft().user" [disabled]="!canEdit()" (input)="patch('user', $any($event.target).value)" />
            </label>
            <label class="cs-field">
              <span>{{ 'settings.carddav.password' | transloco }}</span>
              <input type="password" autocomplete="new-password" [value]="draft().password" [disabled]="!canEdit()"
                     [placeholder]="(draft().passwordSet ? 'settings.carddav.pwdKeep' : 'settings.carddav.pwdNone') | transloco"
                     (input)="patch('password', $any($event.target).value)" />
            </label>
            <label class="cs-field">
              <span>{{ 'settings.carddav.path' | transloco }}</span>
              <input type="text" [value]="draft().path" [disabled]="!canEdit()" (input)="patch('path', $any($event.target).value)" />
            </label>
          </div>
          <label class="cs-check">
            <input type="checkbox" [checked]="draft().ssl" [disabled]="!canEdit()" (change)="patch('ssl', $any($event.target).checked)" />
            <span>{{ 'settings.carddav.ssl' | transloco }}</span>
          </label>
        </fieldset>

        <fieldset class="cs-sec">
          <legend>{{ 'settings.carddav.target' | transloco }}</legend>
          <div class="cs-row">
            <label class="cs-field cs-grow">
              <span>{{ 'settings.carddav.addressBook' | transloco }}</span>
              <select [value]="draft().href" [disabled]="!canEdit()" (change)="patch('href', $any($event.target).value)">
                <option value="">—</option>
                @for (ab of addressBookOptions(); track ab.href) {
                  <option [value]="ab.href">{{ ab.displayName || ab.href }}</option>
                }
              </select>
            </label>
            <button type="button" class="btn-sm" [disabled]="!canEdit() || fetching()" (click)="fetchBooks()">
              {{ (fetching() ? 'settings.carddav.fetching' : 'settings.carddav.fetch') | transloco }}
            </button>
          </div>
          @if (fetchError()) { <p class="cs-error">{{ fetchError() }}</p> }
        </fieldset>

        <div class="cs-foot">
          @if (savedOk()) { <span class="cs-ok">{{ 'settings.savedOk' | transloco }}</span> }
          @if (saveError()) { <span class="cs-error">{{ 'settings.saveError' | transloco }}</span> }
          <button type="button" class="btn-primary" [disabled]="!canEdit() || saving()" (click)="submit()">
            {{ (saving() ? 'settings.saving' : 'settings.save') | transloco }}
          </button>
        </div>

        <fieldset class="cs-sec">
          <legend>{{ 'settings.carddav.runTitle' | transloco }}</legend>
          <p class="cs-note">{{ 'settings.carddav.runHint' | transloco }}</p>
          <div class="cs-runrow">
            <button type="button" class="btn-ghost" [disabled]="!canEdit() || syncing()" (click)="runSync()">
              {{ (syncing() ? 'settings.carddav.syncing' : 'settings.carddav.runNow') | transloco }}
            </button>
            @if (syncStarted()) { <span class="cs-ok">{{ 'settings.carddav.syncStarted' | transloco }}</span> }
            @if (syncError()) { <span class="cs-error">{{ 'settings.saveError' | transloco }}</span> }
          </div>
        </fieldset>
      }
    </div>
  `,
  styles: [`
    :host { display: block; }
    .cs { max-width: 680px; }
    .cs-hint { margin: 0 0 12px; font-size: .82rem; color: #b26a00; }
    .cs-muted { color: var(--jl-ink-soft); font-size: .88rem; }
    .cs-error { color: var(--jl-red); font-size: .84rem; }
    .cs-ok { color: var(--jl-green, #1a7f37); font-size: .84rem; }
    .cs-sec { border: 1px solid var(--jl-line); border-radius: 10px; padding: 12px 14px 14px; margin: 0 0 14px;
      display: flex; flex-direction: column; gap: 12px; }
    .cs-sec legend { font-size: .8rem; font-weight: 700; color: var(--jl-ink-soft); padding: 0 6px; text-transform: uppercase; letter-spacing: .04em; }
    .cs-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    .cs-field { display: flex; flex-direction: column; gap: 5px; font-size: .8rem; color: var(--jl-ink-faint); }
    .cs-field input, .cs-field select { font: inherit; font-size: .9rem; color: var(--jl-ink); padding: 8px 10px;
      border: 1px solid var(--jl-line-strong); border-radius: 8px; background: var(--jl-surface); outline: none; }
    .cs-field input:focus, .cs-field select:focus { border-color: var(--jl-blue); }
    .cs-field input:disabled, .cs-field select:disabled { opacity: .6; }
    .cs-row { display: flex; align-items: flex-end; gap: 10px; }
    .cs-grow { flex: 1 1 auto; }
    .cs-runrow { display: flex; align-items: center; gap: 12px; }
    .cs-check { display: flex; align-items: center; gap: 8px; font-size: .9rem; color: var(--jl-ink); }
    .cs-check input { width: 16px; height: 16px; }
    .cs-note { margin: 0; font-size: .78rem; color: var(--jl-ink-faint); }
    .cs-foot { display: flex; align-items: center; justify-content: flex-end; gap: 12px; margin-bottom: 14px; }
    .btn-primary { font: inherit; font-size: .86rem; font-weight: 600; padding: 9px 18px; border: 0; border-radius: 8px;
      background: var(--jl-blue); color: #fff; cursor: pointer; }
    .btn-primary:disabled { opacity: .55; cursor: default; }
    .btn-sm, .btn-ghost { font: inherit; font-size: .84rem; padding: 8px 14px; border: 1px solid var(--jl-line-strong);
      border-radius: 8px; background: var(--jl-surface); color: var(--jl-ink); cursor: pointer; flex: 0 0 auto; }
    .btn-sm:hover:not(:disabled), .btn-ghost:hover:not(:disabled) { border-color: var(--jl-blue); color: var(--jl-blue); }
    .btn-sm:disabled, .btn-ghost:disabled { opacity: .5; cursor: default; }
    @media (max-width: 560px) { .cs-grid { grid-template-columns: 1fr; } }
  `],
})
export class CardDavSyncComponent implements OnInit {
  private readonly api = inject(SystemSettingsService);
  private readonly auth = inject(AuthService);

  protected readonly draft = signal<CardDavSyncSettings>({ ...EMPTY });
  protected readonly addressBooks = signal<CloudAddressBook[]>([]);
  protected readonly loading = signal(false);
  protected readonly loadError = signal(false);
  protected readonly saving = signal(false);
  protected readonly saveError = signal(false);
  protected readonly savedOk = signal(false);
  protected readonly fetching = signal(false);
  protected readonly fetchError = signal<string | null>(null);
  protected readonly syncing = signal(false);
  protected readonly syncStarted = signal(false);
  protected readonly syncError = signal(false);
  protected readonly canEdit = computed(() => this.auth.hasRole('sysAdminRole'));

  /** Fetched address books, always including the currently-selected href so it never disappears. */
  protected readonly addressBookOptions = computed<CloudAddressBook[]>(() => {
    const books = this.addressBooks();
    const href = this.draft().href;
    if (href && !books.some((b) => b.href === href)) {
      return [{ href, displayName: href }, ...books];
    }
    return books;
  });

  ngOnInit(): void {
    this.loading.set(true);
    this.api.getCardDavSync().subscribe({
      next: (s) => { this.draft.set({ ...EMPTY, ...s, password: '' }); this.loading.set(false); },
      error: () => { this.loadError.set(true); this.loading.set(false); },
    });
  }

  protected patch<K extends keyof CardDavSyncSettings>(key: K, value: CardDavSyncSettings[K]): void {
    this.draft.update((d) => ({ ...d, [key]: value }));
    this.savedOk.set(false);
  }

  protected patchPort(value: string): void {
    const n = parseInt(value, 10);
    this.patch('port', Number.isFinite(n) ? n : 443);
  }

  protected fetchBooks(): void {
    if (!this.canEdit() || this.fetching()) { return; }
    this.fetching.set(true);
    this.fetchError.set(null);
    this.api.listCloudAddressBooks(this.draft()).subscribe({
      next: (books) => { this.addressBooks.set(books); this.fetching.set(false); },
      error: (e: HttpErrorResponse) => { this.fetching.set(false); this.fetchError.set(typeof e?.error === 'string' && e.error.trim() ? e.error.trim() : 'error'); },
    });
  }

  protected submit(): void {
    if (!this.canEdit() || this.saving()) { return; }
    this.saving.set(true);
    this.saveError.set(false);
    this.savedOk.set(false);
    this.api.saveCardDavSync(this.draft()).subscribe({
      next: (s) => { this.saving.set(false); this.savedOk.set(true); this.draft.set({ ...EMPTY, ...s, password: '' }); },
      error: () => { this.saving.set(false); this.saveError.set(true); },
    });
  }

  protected runSync(): void {
    if (!this.canEdit() || this.syncing()) { return; }
    this.syncing.set(true);
    this.syncStarted.set(false);
    this.syncError.set(false);
    this.api.runCardDavSync().subscribe({
      next: () => { this.syncing.set(false); this.syncStarted.set(true); },
      error: () => { this.syncing.set(false); this.syncError.set(true); },
    });
  }
}
