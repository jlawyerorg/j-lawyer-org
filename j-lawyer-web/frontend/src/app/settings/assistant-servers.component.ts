import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { AuthService } from '../core/auth/auth.service';
import { IconComponent } from '../shared/icon.component';
import { DatePipe } from '@angular/common';
import { AiRequestLogEntry, AssistantConfig, AssistantConfigService, AssistantStatus } from './assistant-config.service';

const EMPTY: AssistantConfig = { name: '', url: '', userName: '', password: '', connectionTimeout: 30, readTimeout: 60, configuration: '' };

/**
 * "Ingo-Server" section: the AI assistant server connections (Assistent Ingo). Full CRUD requires
 * `adminRole` (enforced server-side). The HTTP basic-auth password is write-only. A "test" action
 * queries live status (reachability + models) and annotates each connection.
 */
@Component({
  selector: 'jl-assistant-servers',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent, DatePipe],
  template: `
    <div class="fl">
      @if (!canEdit()) { <p class="fl-muted">{{ 'settings.readOnlyHint' | transloco }}</p> }
      <div class="fl-bar">
        @if (canEdit()) {
          <button type="button" class="btn-primary" (click)="openNew()"><jl-icon name="plus" [size]="15" /><span>{{ 'settings.aiServer.create' | transloco }}</span></button>
        }
        <button type="button" class="btn-ghost" [disabled]="testing()" (click)="test()"><jl-icon name="refresh" [size]="15" /><span>{{ (testing() ? 'settings.aiServer.testing' : 'settings.aiServer.test') | transloco }}</span></button>
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
              <div class="fl-main" (click)="openEdit(c)">
                <span class="fl-name">{{ c.name }}</span>
                <span class="fl-sub">{{ c.url }}</span>
                @if (statusFor(c.id); as st) {
                  @if (st.reachable) {
                    <span class="fl-sub ok">{{ 'settings.aiServer.reachable' | transloco }}@if (st.models.length) { · {{ 'settings.aiServer.models' | transloco: { n: st.models.length } }} }@if (st.userLabel) { · {{ st.userLabel }} }</span>
                    @if (st.tokens != null && st.tokens >= 0) {
                      <span class="fl-badge">{{ 'settings.aiServer.quota' | transloco: { n: st.tokens } }}</span>
                    }
                  }
                } @else if (tested()) {
                  <span class="fl-sub bad">{{ 'settings.aiServer.unreachable' | transloco }}</span>
                }
              </div>
              <button type="button" class="icon-btn" (click)="openHistory(c)" [title]="'settings.aiServer.history' | transloco" [attr.aria-label]="'settings.aiServer.history' | transloco"><jl-icon name="clock" [size]="15" /></button>
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
      <div class="ed-dialog" role="dialog" aria-modal="true">
        <header class="ed-head"><h2>{{ (editItem() ? 'settings.aiServer.edit' : 'settings.aiServer.create') | transloco }}</h2></header>
        <div class="ed-body">
          <label class="ed-field"><span>{{ 'settings.aiServer.name' | transloco }}</span><input type="text" [value]="draft().name" (input)="patch('name', $any($event.target).value)" /></label>
          <label class="ed-field"><span>{{ 'settings.aiServer.url' | transloco }}</span><input type="text" [value]="draft().url" (input)="patch('url', $any($event.target).value)" /></label>
          <div class="ed-note">{{ 'settings.aiServer.authNote' | transloco }}</div>
          <div class="ed-grid">
            <label class="ed-field"><span>{{ 'settings.aiServer.userName' | transloco }}</span><input type="text" autocomplete="off" [value]="draft().userName" (input)="patch('userName', $any($event.target).value)" /></label>
            <label class="ed-field"><span>{{ 'settings.aiServer.password' | transloco }}</span><input type="password" autocomplete="new-password" [value]="draft().password ?? ''" [attr.placeholder]="passwordPlaceholder()" (input)="patch('password', $any($event.target).value)" /></label>
          </div>
          <div class="ed-grid">
            <label class="ed-field"><span>{{ 'settings.aiServer.connectTimeout' | transloco }}</span><input type="number" [value]="draft().connectionTimeout" (input)="patchNum('connectionTimeout', $any($event.target).value)" /></label>
            <label class="ed-field"><span>{{ 'settings.aiServer.readTimeout' | transloco }}</span><input type="number" [value]="draft().readTimeout" (input)="patchNum('readTimeout', $any($event.target).value)" /></label>
          </div>
          <label class="ed-field"><span>{{ 'settings.aiServer.configuration' | transloco }}</span><textarea rows="3" class="mono" [value]="draft().configuration" (input)="patch('configuration', $any($event.target).value)"></textarea></label>
          @if (saveError()) { <p class="ed-error">{{ saveError() }}</p> }
        </div>
        <footer class="ed-foot">
          <button type="button" class="btn-ghost" [disabled]="saving()" (click)="editorOpen.set(false)">{{ 'settings.cancel' | transloco }}</button>
          <button type="button" class="btn-primary" [disabled]="saving() || !draft().name.trim()" (click)="submit()">{{ (saving() ? 'settings.saving' : 'settings.save') | transloco }}</button>
        </footer>
      </div>
    }

    @if (historyOpen()) {
      <div class="ed-backdrop" (click)="historyOpen.set(false)"></div>
      <div class="ed-dialog wide" role="dialog" aria-modal="true">
        <header class="ed-head"><h2>{{ 'settings.aiServer.historyTitle' | transloco: { name: historyName() } }}</h2></header>
        <div class="ed-body">
          @if (historyLoading()) {
            <p class="fl-muted">{{ 'settings.loading' | transloco }}</p>
          } @else if (historyError()) {
            <p class="fl-error">{{ historyError() }}</p>
          } @else if (history().length === 0) {
            <p class="fl-muted">{{ 'settings.empty' | transloco }}</p>
          } @else {
            @if (historyStats(); as s) {
              <p class="hist-summary">{{ 'settings.aiServer.histMonth' | transloco: { count: s.count, total: s.total, perDay: s.perDay } }}</p>
            }
            <table class="hist">
              <thead><tr><th>{{ 'settings.aiServer.histTime' | transloco }}</th><th>{{ 'settings.aiServer.histType' | transloco }}</th><th class="num">{{ 'settings.aiServer.histTokens' | transloco }}</th></tr></thead>
              <tbody>
                @for (e of history(); track $index) {
                  <tr>
                    <td>{{ e.timestamp ? (e.timestamp | date: 'dd.MM.yyyy HH:mm') : '—' }}</td>
                    <td>{{ e.requestType }}</td>
                    <td class="num">{{ e.tokensUsed }}</td>
                  </tr>
                }
              </tbody>
            </table>
          }
        </div>
        <footer class="ed-foot">
          <button type="button" class="btn-ghost" (click)="historyOpen.set(false)">{{ 'settings.close' | transloco }}</button>
        </footer>
      </div>
    }
  `,
  styleUrls: ['./finance-list.css', './finance-editor.css'],
  styles: [`
    .fl-bar { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; }
    .ed-note { font-size: .78rem; color: var(--jl-ink-faint); }
    textarea.mono { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: .82rem; }
    .fl-sub.ok { color: var(--jl-green, #1a7f37); }
    .fl-sub.bad { color: var(--jl-red); }
    .ed-dialog.wide { width: min(640px, calc(100vw - 32px)); }
    table.hist { width: 100%; border-collapse: collapse; font-size: .84rem; }
    table.hist th, table.hist td { text-align: left; padding: 7px 10px; border-bottom: 1px solid var(--jl-line); }
    table.hist th { font-size: .72rem; text-transform: uppercase; letter-spacing: .04em; color: var(--jl-ink-faint); }
    table.hist .num { text-align: right; }
    .hist-summary { margin: 0 0 12px; font-size: .84rem; color: var(--jl-ink); background: var(--jl-surface-alt); border-radius: 8px; padding: 9px 11px; }
  `],
})
export class AssistantServersComponent implements OnInit {
  private readonly api = inject(AssistantConfigService);
  private readonly auth = inject(AuthService);
  private readonly transloco = inject(TranslocoService);

  protected readonly items = signal<AssistantConfig[]>([]);
  protected readonly loading = signal(false);
  protected readonly loadError = signal(false);
  protected readonly busy = signal(false);
  protected readonly opError = signal<string | null>(null);
  protected readonly editorOpen = signal(false);
  protected readonly editItem = signal<AssistantConfig | null>(null);
  protected readonly draft = signal<AssistantConfig>({ ...EMPTY });
  protected readonly saving = signal(false);
  protected readonly saveError = signal<string | null>(null);
  protected readonly canEdit = computed(() => this.auth.hasRole('adminRole'));

  protected readonly statuses = signal<AssistantStatus[]>([]);
  protected readonly tested = signal(false);
  protected readonly testing = signal(false);

  // request history
  protected readonly historyOpen = signal(false);
  protected readonly historyName = signal('');
  protected readonly history = signal<AiRequestLogEntry[]>([]);
  protected readonly historyLoading = signal(false);
  protected readonly historyError = signal<string | null>(null);
  /** Last-30-days summary: number of requests, total tokens and average tokens per day. */
  protected readonly historyStats = signal<{ count: number; total: number; perDay: number } | null>(null);

  ngOnInit(): void {
    this.reload();
    // auto-query live status so reachability + token quota show directly in the overview
    this.test();
  }

  protected openHistory(c: AssistantConfig): void {
    if (!c.id) { return; }
    this.historyName.set(c.name);
    this.history.set([]);
    this.historyStats.set(null);
    this.historyError.set(null);
    this.historyOpen.set(true);
    this.historyLoading.set(true);
    this.api.getRequestLog(c.id).subscribe({
      next: (l) => {
        const entries = [...(l ?? [])].sort((a, b) => b.timestamp - a.timestamp);
        this.history.set(entries);
        this.historyStats.set(this.lastMonthStats(entries));
        this.historyLoading.set(false);
      },
      error: (e: HttpErrorResponse) => { this.historyLoading.set(false); this.historyError.set(this.msg(e)); },
    });
  }

  /** Aggregates the entries of the last 30 days into count / total tokens / average tokens per day. */
  private lastMonthStats(entries: AiRequestLogEntry[]): { count: number; total: number; perDay: number } {
    const cutoff = Date.now() - 30 * 24 * 60 * 60 * 1000;
    const recent = entries.filter((e) => e.timestamp >= cutoff);
    const total = recent.reduce((sum, e) => sum + (e.tokensUsed || 0), 0);
    return { count: recent.length, total, perDay: Math.round(total / 30) };
  }

  private reload(): void {
    this.loading.set(true);
    this.loadError.set(false);
    this.api.listConfigs().subscribe({
      next: (l) => { this.items.set([...l].sort((a, b) => a.name.localeCompare(b.name))); this.loading.set(false); },
      error: () => { this.loadError.set(true); this.loading.set(false); },
    });
  }

  protected statusFor(id: string | undefined): AssistantStatus | undefined {
    return id ? this.statuses().find((s) => s.configId === id) : undefined;
  }

  protected test(): void {
    if (this.testing()) { return; }
    this.testing.set(true); this.opError.set(null);
    this.api.getStatus().subscribe({
      next: (l) => { this.statuses.set(l ?? []); this.tested.set(true); this.testing.set(false); },
      error: (e: HttpErrorResponse) => { this.testing.set(false); this.opError.set(this.msg(e)); },
    });
  }

  protected passwordPlaceholder(): string {
    return this.editItem()?.passwordSet ? this.transloco.translate('settings.aiServer.passwordKeep') : '';
  }

  protected openNew(): void { this.editItem.set(null); this.draft.set({ ...EMPTY }); this.saveError.set(null); this.editorOpen.set(true); }
  protected openEdit(c: AssistantConfig): void { this.editItem.set(c); this.draft.set({ ...c, password: '' }); this.saveError.set(null); this.editorOpen.set(true); }

  protected patch<K extends keyof AssistantConfig>(key: K, value: AssistantConfig[K]): void { this.draft.update((d) => ({ ...d, [key]: value })); }
  protected patchNum(key: 'connectionTimeout' | 'readTimeout', value: string): void { const n = parseInt(value, 10); this.patch(key, Number.isFinite(n) ? n : 0); }

  protected submit(): void {
    const d = this.draft();
    if (!d.name.trim() || this.saving()) { return; }
    this.saving.set(true); this.saveError.set(null); this.opError.set(null);
    const req = this.editItem() ? this.api.updateConfig(d) : this.api.createConfig(d);
    req.subscribe({
      next: () => { this.saving.set(false); this.editorOpen.set(false); this.reload(); },
      error: (e: HttpErrorResponse) => { this.saving.set(false); this.saveError.set(this.msg(e)); },
    });
  }

  protected remove(c: AssistantConfig): void {
    if (this.busy() || !c.id) { return; }
    this.busy.set(true); this.opError.set(null);
    this.api.deleteConfig(c.id).subscribe({
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
