import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { AuthService } from '../core/auth/auth.service';
import { IconComponent } from '../shared/icon.component';
import { Webhook, WebhooksService } from './webhooks.service';

const EMPTY: Webhook = { name: '', url: '', hookType: '', authUser: '', authPassword: '', connectionTimeout: 30, readTimeout: 60 };

/**
 * "Web Hooks" section: outbound webhook configuration (Administration). Full CRUD requires
 * `adminRole` (enforced server-side). The HTTP basic-auth password is write-only. The event type is
 * chosen from the server's hook types.
 */
@Component({
  selector: 'jl-webhooks',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent],
  template: `
    <div class="fl">
      @if (!canEdit()) { <p class="fl-muted">{{ 'settings.readOnlyHint' | transloco }}</p> }
      <div class="fl-bar">
        @if (canEdit()) {
          <button type="button" class="btn-primary" (click)="openNew()"><jl-icon name="plus" [size]="15" /><span>{{ 'settings.webhook.create' | transloco }}</span></button>
        }
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
          @for (h of items(); track h.name) {
            <li class="fl-row">
              <div class="fl-main" (click)="openEdit(h)">
                <span class="fl-name">{{ h.name }}</span>
                <span class="fl-sub">{{ h.url }}</span>
              </div>
              <span class="fl-badge">{{ h.hookType }}</span>
              @if (canEdit()) {
                <button type="button" class="icon-btn" (click)="openEdit(h)" [attr.aria-label]="'settings.rename' | transloco"><jl-icon name="edit" [size]="15" /></button>
                <button type="button" class="icon-btn danger" [disabled]="busy()" (click)="remove(h)" [attr.aria-label]="'settings.delete' | transloco"><jl-icon name="trash" [size]="15" /></button>
              }
            </li>
          }
        </ul>
      }
    </div>

    @if (editorOpen()) {
      <div class="ed-backdrop" (click)="editorOpen.set(false)"></div>
      <div class="ed-dialog" role="dialog" aria-modal="true">
        <header class="ed-head"><h2>{{ (editItem() ? 'settings.webhook.edit' : 'settings.webhook.create') | transloco }}</h2></header>
        <div class="ed-body">
          <label class="ed-field"><span>{{ 'settings.webhook.name' | transloco }}</span><input type="text" [value]="draft().name" [disabled]="!!editItem()" (input)="patch('name', $any($event.target).value)" /></label>
          <label class="ed-field"><span>{{ 'settings.webhook.url' | transloco }}</span><input type="text" [value]="draft().url" (input)="patch('url', $any($event.target).value)" /></label>
          <label class="ed-field">
            <span>{{ 'settings.webhook.type' | transloco }}</span>
            <select [value]="draft().hookType" (change)="patch('hookType', $any($event.target).value)">
              <option value=""></option>
              @for (t of types(); track t) { <option [value]="t">{{ t }}</option> }
            </select>
          </label>
          <div class="ed-note">{{ 'settings.webhook.authNote' | transloco }}</div>
          <div class="ed-grid">
            <label class="ed-field"><span>{{ 'settings.webhook.user' | transloco }}</span><input type="text" autocomplete="off" [value]="draft().authUser" (input)="patch('authUser', $any($event.target).value)" /></label>
            <label class="ed-field"><span>{{ 'settings.webhook.password' | transloco }}</span><input type="password" autocomplete="new-password" [value]="draft().authPassword ?? ''" [attr.placeholder]="editItem()?.authPasswordSet ? ('settings.webhook.passwordKeep' | transloco) : ''" (input)="patch('authPassword', $any($event.target).value)" /></label>
          </div>
          <div class="ed-grid">
            <label class="ed-field"><span>{{ 'settings.webhook.connectTimeout' | transloco }}</span><input type="number" [value]="draft().connectionTimeout" (input)="patchNum('connectionTimeout', $any($event.target).value)" /></label>
            <label class="ed-field"><span>{{ 'settings.webhook.readTimeout' | transloco }}</span><input type="number" [value]="draft().readTimeout" (input)="patchNum('readTimeout', $any($event.target).value)" /></label>
          </div>
          @if (saveError()) { <p class="ed-error">{{ saveError() }}</p> }
        </div>
        <footer class="ed-foot">
          <button type="button" class="btn-ghost" [disabled]="saving()" (click)="editorOpen.set(false)">{{ 'settings.cancel' | transloco }}</button>
          <button type="button" class="btn-primary" [disabled]="saving() || !draft().name.trim()" (click)="submit()">{{ (saving() ? 'settings.saving' : 'settings.save') | transloco }}</button>
        </footer>
      </div>
    }
  `,
  styleUrls: ['./finance-list.css', './finance-editor.css'],
  styles: [`.ed-note { font-size: .78rem; color: var(--jl-ink-faint); }`],
})
export class WebhooksComponent implements OnInit {
  private readonly api = inject(WebhooksService);
  private readonly auth = inject(AuthService);
  private readonly transloco = inject(TranslocoService);

  protected readonly items = signal<Webhook[]>([]);
  protected readonly types = signal<string[]>([]);
  protected readonly loading = signal(false);
  protected readonly loadError = signal(false);
  protected readonly busy = signal(false);
  protected readonly opError = signal<string | null>(null);
  protected readonly editorOpen = signal(false);
  protected readonly editItem = signal<Webhook | null>(null);
  protected readonly draft = signal<Webhook>({ ...EMPTY });
  protected readonly saving = signal(false);
  protected readonly saveError = signal<string | null>(null);
  protected readonly canEdit = computed(() => this.auth.hasRole('adminRole'));

  ngOnInit(): void {
    this.reload();
    this.api.listTypes().subscribe({ next: (t) => this.types.set(t ?? []), error: () => { /* ignore */ } });
  }

  private reload(): void {
    this.loading.set(true); this.loadError.set(false);
    this.api.list().subscribe({
      next: (l) => { this.items.set([...l].sort((a, b) => a.name.localeCompare(b.name))); this.loading.set(false); },
      error: () => { this.loadError.set(true); this.loading.set(false); },
    });
  }

  protected openNew(): void { this.editItem.set(null); this.draft.set({ ...EMPTY }); this.saveError.set(null); this.editorOpen.set(true); }
  protected openEdit(h: Webhook): void { this.editItem.set(h); this.draft.set({ ...h, authPassword: '' }); this.saveError.set(null); this.editorOpen.set(true); }

  protected patch<K extends keyof Webhook>(key: K, value: Webhook[K]): void { this.draft.update((d) => ({ ...d, [key]: value })); }
  protected patchNum(key: 'connectionTimeout' | 'readTimeout', value: string): void { const n = parseInt(value, 10); this.patch(key, Number.isFinite(n) ? n : 0); }

  protected submit(): void {
    const d = this.draft();
    if (!d.name.trim() || this.saving()) { return; }
    this.saving.set(true); this.saveError.set(null); this.opError.set(null);
    const req = this.editItem() ? this.api.update(d) : this.api.create(d);
    req.subscribe({
      next: () => { this.saving.set(false); this.editorOpen.set(false); this.reload(); },
      error: (e: HttpErrorResponse) => { this.saving.set(false); this.saveError.set(this.msg(e)); },
    });
  }

  protected remove(h: Webhook): void {
    if (this.busy() || !h.name) { return; }
    this.busy.set(true); this.opError.set(null);
    this.api.delete(h.name).subscribe({
      next: () => { this.busy.set(false); this.reload(); },
      error: (e: HttpErrorResponse) => { this.busy.set(false); this.opError.set(this.msg(e)); },
    });
  }

  private msg(e: HttpErrorResponse): string {
    const body = e?.error;
    if (typeof body === 'string' && body.trim()) { return body.trim(); }
    return this.transloco.translate('settings.saveError');
  }
}
