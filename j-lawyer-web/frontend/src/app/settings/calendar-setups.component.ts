import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { AuthService } from '../core/auth/auth.service';
import { IconComponent } from '../shared/icon.component';
import { CAL_EVENT_TYPES, CalendarConfigService, CalendarSetup, CloudCalendar } from './calendar-config.service';

const EMPTY: CalendarSetup = {
  displayName: '', href: '', eventType: 10, background: 0, deleteDone: false,
  cloudHost: '', cloudPort: 443, cloudSsl: true, cloudPath: '', cloudUser: '', cloudPassword: '', passwordSet: false,
};

/**
 * "Kalender und Synchronisation" section: lists calendar definitions (CalDAV) and hosts a create/edit
 * modal (connection, target calendar, event type, colour), plus a "sync now" trigger. Writes need
 * `adminRole`. The cloud password is write-only.
 */
@Component({
  selector: 'jl-calendar-setups',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent],
  template: `
    <div class="fl">
      @if (!canEdit()) { <p class="fl-muted">{{ 'settings.readOnlyHint' | transloco }}</p> }
      <div class="fl-bar">
        @if (canEdit()) {
          <button type="button" class="btn-primary" (click)="openNew()"><jl-icon name="plus" [size]="15" /><span>{{ 'settings.calendar.create' | transloco }}</span></button>
          <button type="button" class="btn-ghost" [disabled]="syncing()" (click)="runSync()">{{ (syncing() ? 'settings.calendar.syncing' : 'settings.calendar.runNow') | transloco }}</button>
        }
        @if (syncStarted()) { <span class="fl-ok">{{ 'settings.calendar.syncStarted' | transloco }}</span> }
      </div>
      @if (opError()) { <p class="fl-error">{{ opError() }}</p> }

      @if (loading()) {
        <p class="fl-muted">{{ 'settings.loading' | transloco }}</p>
      } @else if (loadError()) {
        <p class="fl-error">{{ 'settings.loadError' | transloco }}</p>
      } @else if (items().length === 0) {
        <p class="fl-muted">{{ 'settings.empty' | transloco }}</p>
      } @else {
        <ul class="fl-list">
          @for (c of items(); track c.id) {
            <li class="fl-row">
              <span class="fl-swatch" [style.background]="swatch(c)"></span>
              <div class="fl-main" (click)="openEdit(c)">
                <span class="fl-name">{{ c.displayName }}</span>
                <span class="fl-sub">{{ typeLabel(c.eventType) | transloco }}{{ c.cloudHost ? ' · ' + c.cloudHost : '' }}</span>
              </div>
              @if (canEdit()) {
                <button type="button" class="icon-btn" (click)="openEdit(c)" [attr.aria-label]="'settings.rename' | transloco"><jl-icon name="edit" [size]="15" /></button>
                <button type="button" class="icon-btn danger" [disabled]="busy()" (click)="remove(c)" [attr.aria-label]="'settings.delete' | transloco"><jl-icon name="trash" [size]="15" /></button>
              }
            </li>
          }
        </ul>
      }
    </div>

    @if (editorOpen()) {
      <div class="ed-backdrop" (click)="editorOpen.set(false)"></div>
      <div class="ed-dialog cs-dialog" role="dialog" aria-modal="true">
        <header class="ed-head"><h2>{{ (editItem() ? 'settings.calendar.edit' : 'settings.calendar.create') | transloco }}</h2></header>
        <div class="ed-body">
          <div class="ed-grid">
            <label class="ed-field">
              <span>{{ 'settings.calendar.displayName' | transloco }}</span>
              <input type="text" [value]="draft().displayName" (input)="patch('displayName', $any($event.target).value)" />
            </label>
            <label class="ed-field">
              <span>{{ 'settings.calendar.eventType' | transloco }}</span>
              <select [value]="draft().eventType" (change)="patchNum('eventType', $any($event.target).value)">
                @for (t of eventTypes; track t.value) { <option [value]="t.value">{{ t.labelKey | transloco }}</option> }
              </select>
            </label>
            <label class="ed-field">
              <span>{{ 'settings.calendar.color' | transloco }}</span>
              <input type="color" [value]="hexColor()" (input)="patchColor($any($event.target).value)" />
            </label>
            <label class="ed-check">
              <input type="checkbox" [checked]="draft().deleteDone" (change)="patch('deleteDone', $any($event.target).checked)" />
              <span>{{ 'settings.calendar.deleteDone' | transloco }}</span>
            </label>
          </div>

          <fieldset class="ed-sec">
            <legend>{{ 'settings.calendar.connection' | transloco }}</legend>
            <div class="ed-grid">
              <label class="ed-field"><span>{{ 'settings.calendar.host' | transloco }}</span><input type="text" [value]="draft().cloudHost" (input)="patch('cloudHost', $any($event.target).value)" /></label>
              <label class="ed-field"><span>{{ 'settings.calendar.port' | transloco }}</span><input type="number" [value]="draft().cloudPort" (input)="patchNum('cloudPort', $any($event.target).value)" /></label>
              <label class="ed-field"><span>{{ 'settings.calendar.user' | transloco }}</span><input type="text" [value]="draft().cloudUser" (input)="patch('cloudUser', $any($event.target).value)" /></label>
              <label class="ed-field"><span>{{ 'settings.calendar.password' | transloco }}</span>
                <input type="password" autocomplete="new-password" [value]="draft().cloudPassword"
                       [placeholder]="(draft().passwordSet ? 'settings.calendar.pwdKeep' : 'settings.calendar.pwdNone') | transloco"
                       (input)="patch('cloudPassword', $any($event.target).value)" /></label>
              <label class="ed-field"><span>{{ 'settings.calendar.path' | transloco }}</span><input type="text" [value]="draft().cloudPath" (input)="patch('cloudPath', $any($event.target).value)" /></label>
            </div>
            <label class="ed-check"><input type="checkbox" [checked]="draft().cloudSsl" (change)="patch('cloudSsl', $any($event.target).checked)" /><span>{{ 'settings.calendar.ssl' | transloco }}</span></label>
          </fieldset>

          <fieldset class="ed-sec">
            <legend>{{ 'settings.calendar.target' | transloco }}</legend>
            <div class="cs-row">
              <label class="ed-field cs-grow">
                <span>{{ 'settings.calendar.calendar' | transloco }}</span>
                <select [value]="draft().href" (change)="patch('href', $any($event.target).value)">
                  <option value="">—</option>
                  @for (cal of calendarOptions(); track cal.href) { <option [value]="cal.href">{{ cal.displayName || cal.href }}</option> }
                </select>
              </label>
              <button type="button" class="btn-sm" [disabled]="fetching()" (click)="fetch()">{{ (fetching() ? 'settings.calendar.fetching' : 'settings.calendar.fetch') | transloco }}</button>
            </div>
            @if (fetchError()) { <p class="ed-error">{{ fetchError() }}</p> }
          </fieldset>

          @if (saveError()) { <p class="ed-error">{{ saveError() }}</p> }
        </div>
        <footer class="ed-foot">
          <button type="button" class="btn-ghost" [disabled]="saving()" (click)="editorOpen.set(false)">{{ 'settings.cancel' | transloco }}</button>
          <button type="button" class="btn-primary" [disabled]="saving() || !draft().displayName.trim()" (click)="submit()">{{ (saving() ? 'settings.saving' : 'settings.save') | transloco }}</button>
        </footer>
      </div>
    }
  `,
  styles: [`
    .fl-swatch { flex: 0 0 auto; width: 14px; height: 14px; border-radius: 4px; border: 1px solid var(--jl-line-strong); }
    .fl-bar { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; }
    .fl-ok { color: var(--jl-green, #1a7f37); font-size: .84rem; }
    .cs-dialog { width: min(620px, calc(100vw - 32px)); }
    .ed-sec { border: 1px solid var(--jl-line); border-radius: 10px; padding: 10px 12px 12px; margin: 4px 0 0; display: flex; flex-direction: column; gap: 10px; }
    .ed-sec legend { font-size: .76rem; font-weight: 700; color: var(--jl-ink-soft); padding: 0 6px; text-transform: uppercase; letter-spacing: .03em; }
    .cs-row { display: flex; align-items: flex-end; gap: 10px; }
    .cs-grow { flex: 1 1 auto; }
  `],
  styleUrls: ['./finance-list.css', './finance-editor.css'],
})
export class CalendarSetupsComponent implements OnInit {
  private readonly api = inject(CalendarConfigService);
  private readonly auth = inject(AuthService);
  private readonly transloco = inject(TranslocoService);

  protected readonly eventTypes = CAL_EVENT_TYPES;
  protected readonly items = signal<CalendarSetup[]>([]);
  protected readonly loading = signal(false);
  protected readonly loadError = signal(false);
  protected readonly busy = signal(false);
  protected readonly opError = signal<string | null>(null);
  protected readonly syncing = signal(false);
  protected readonly syncStarted = signal(false);

  protected readonly editorOpen = signal(false);
  protected readonly editItem = signal<CalendarSetup | null>(null);
  protected readonly draft = signal<CalendarSetup>({ ...EMPTY });
  protected readonly saving = signal(false);
  protected readonly saveError = signal<string | null>(null);
  protected readonly calendars = signal<CloudCalendar[]>([]);
  protected readonly fetching = signal(false);
  protected readonly fetchError = signal<string | null>(null);
  protected readonly canEdit = computed(() => this.auth.hasRole('adminRole'));

  protected readonly hexColor = computed(() => '#' + ((this.draft().background & 0xffffff) >>> 0).toString(16).padStart(6, '0'));
  protected readonly calendarOptions = computed<CloudCalendar[]>(() => {
    const list = this.calendars();
    const href = this.draft().href;
    if (href && !list.some((c) => c.href === href)) { return [{ href, displayName: href }, ...list]; }
    return list;
  });

  ngOnInit(): void { this.reload(); }

  private reload(): void {
    this.loading.set(true);
    this.loadError.set(false);
    this.api.listSetups().subscribe({
      next: (l) => { this.items.set([...l].sort((a, b) => a.displayName.localeCompare(b.displayName))); this.loading.set(false); },
      error: () => { this.loadError.set(true); this.loading.set(false); },
    });
  }

  protected swatch(c: CalendarSetup): string { return '#' + ((c.background & 0xffffff) >>> 0).toString(16).padStart(6, '0'); }
  protected typeLabel(t: number): string { return this.eventTypes.find((e) => e.value === t)?.labelKey ?? ''; }

  protected openNew(): void {
    this.editItem.set(null); this.draft.set({ ...EMPTY }); this.calendars.set([]);
    this.saveError.set(null); this.fetchError.set(null); this.editorOpen.set(true);
  }
  protected openEdit(c: CalendarSetup): void {
    this.editItem.set(c); this.draft.set({ ...c, cloudPassword: '' }); this.calendars.set([]);
    this.saveError.set(null); this.fetchError.set(null); this.editorOpen.set(true);
  }

  protected patch<K extends keyof CalendarSetup>(key: K, value: CalendarSetup[K]): void { this.draft.update((d) => ({ ...d, [key]: value })); }
  protected patchNum(key: 'eventType' | 'cloudPort', value: string): void {
    const n = parseInt(value, 10);
    this.patch(key, (Number.isFinite(n) ? n : 0) as CalendarSetup[typeof key]);
  }
  protected patchColor(hex: string): void { this.patch('background', parseInt(hex.replace('#', ''), 16) || 0); }

  protected fetch(): void {
    if (this.fetching()) { return; }
    this.fetching.set(true); this.fetchError.set(null);
    this.api.testConnection(this.draft()).subscribe({
      next: (cals) => { this.calendars.set(cals); this.fetching.set(false); },
      error: (e: HttpErrorResponse) => { this.fetching.set(false); this.fetchError.set(this.msg(e)); },
    });
  }

  protected submit(): void {
    const d = this.draft();
    if (!d.displayName.trim() || this.saving()) { return; }
    this.saving.set(true); this.saveError.set(null); this.opError.set(null);
    const req = this.editItem() ? this.api.updateSetup(d) : this.api.createSetup(d);
    req.subscribe({
      next: () => { this.saving.set(false); this.editorOpen.set(false); this.reload(); },
      error: (e: HttpErrorResponse) => { this.saving.set(false); this.saveError.set(this.msg(e)); },
    });
  }

  protected remove(c: CalendarSetup): void {
    if (this.busy()) { return; }
    if (!confirm(this.transloco.translate('settings.calendar.confirmDelete', { name: c.displayName }))) { return; }
    this.busy.set(true); this.opError.set(null);
    this.api.deleteSetup(c).subscribe({
      next: () => { this.busy.set(false); this.reload(); },
      error: (e: HttpErrorResponse) => { this.busy.set(false); this.opError.set(this.msg(e)); },
    });
  }

  protected runSync(): void {
    if (this.syncing()) { return; }
    this.syncing.set(true); this.syncStarted.set(false); this.opError.set(null);
    this.api.runSync().subscribe({
      next: () => { this.syncing.set(false); this.syncStarted.set(true); },
      error: (e: HttpErrorResponse) => { this.syncing.set(false); this.opError.set(this.msg(e)); },
    });
  }

  private msg(e: HttpErrorResponse): string {
    const body = e?.error;
    if (typeof body === 'string' && body.trim()) { return body.trim(); }
    return this.transloco.translate('settings.saveError');
  }
}
