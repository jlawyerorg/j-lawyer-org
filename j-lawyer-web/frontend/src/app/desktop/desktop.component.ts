import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe, DecimalPipe, NgTemplateOutlet } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Subject, debounceTime } from 'rxjs';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { AuthService } from '../core/auth/auth.service';
import { EmailService } from '../communication/email.service';
import { BeaService } from '../bea/bea.service';
import { DashboardService } from './dashboard.service';
import {
  ALL_WIDGETS, DashboardConfig, DashStats, DashWidgetId, DEFAULT_DASHBOARD_CONFIG, DUE_FUTURE_OPTIONS, DUE_PAST_OPTIONS,
  DueItem, DueType, InvoiceSummary, MessageItem, RecentCase, TaggedItem, UserOption,
} from './desktop.models';

type UserWidget = 'recent' | 'due' | 'tagged';
interface TagTab { key: string; label: string; kind: 'case' | 'doc'; }

/**
 * "Mein Desktop" — a read-only dashboard mirroring the Swing DesktopPanel. Widgets are assembled
 * from existing REST endpoints and deep-link into the modules. Visibility and per-widget config
 * (multi-user filters, due window/type tabs, subscribed labels, limits) are persisted per user
 * (GET/PUT /v8/profile/dashboard). OpenSpec: add-web-client.
 */
@Component({
  selector: 'jl-desktop',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent, RouterLink, DatePipe, DecimalPipe, NgTemplateOutlet],
  template: `
    <!-- reusable multi-user filter (button + checkbox popover), parameterized by widget -->
    <ng-template #userFilter let-w="w">
      <div class="uf">
        <button type="button" class="chip-btn" [class.active]="userCount(w) > 0" (click)="userPop.set(userPop() === w ? '' : w)" [title]="'desktop.cfg.user' | transloco">
          <jl-icon name="contacts" [size]="14" />
          @if (userCount(w)) { <span>{{ userCount(w) }}</span> }
        </button>
        @if (userPop() === w) {
          <div class="cfg-pop">
            <h3>{{ 'desktop.cfg.user' | transloco }}</h3>
            @for (u of users(); track u.id) {
              <label class="cfg-row"><input type="checkbox" [checked]="userChecked(w, u.id)" (change)="toggleUser(w, u.id)" /><span>{{ u.name }}</span></label>
            }
            @if (!users().length) { <p class="muted sm">—</p> }
          </div>
        }
      </div>
    </ng-template>

    <section class="dash">
      <header class="dash-head">
        <div class="dh-title">
          <h1>{{ 'desktop.title' | transloco }}</h1>
          <p class="greeting">{{ 'desktop.greeting' | transloco: { name: userName() } }}</p>
        </div>
        <div class="dh-config">
          <button type="button" class="icon-btn" (click)="configOpen.set(!configOpen())" [attr.aria-label]="'desktop.configure' | transloco" [title]="'desktop.configure' | transloco">
            <jl-icon name="gear" [size]="18" />
          </button>
          @if (configOpen()) {
            <div class="cfg-pop">
              <h3>{{ 'desktop.cfg.widgets' | transloco }}</h3>
              @for (wid of allWidgets; track wid) {
                <label class="cfg-row"><input type="checkbox" [checked]="visible(wid)" (change)="toggleWidget(wid)" /><span>{{ 'desktop.widget.' + wid | transloco }}</span></label>
              }
            </div>
          }
        </div>
      </header>

      @if (visible('stats')) {
        <div class="strip">
          @if (statsLoading()) { <p class="muted pad">{{ 'desktop.loading' | transloco }}</p> }
          @else {
            @if (stats(); as s) {
              <div class="kpis wide">
                <div class="kpi"><span class="k-value">{{ s.casesTotal | number }}</span><span class="k-label">{{ 'desktop.stats.cases' | transloco }}</span></div>
                <div class="kpi"><span class="k-value">{{ s.casesOpen | number }}</span><span class="k-label">{{ 'desktop.stats.open' | transloco }}</span></div>
                <div class="kpi"><span class="k-value">{{ s.casesArchived | number }}</span><span class="k-label">{{ 'desktop.stats.archived' | transloco }}</span></div>
                <div class="kpi"><span class="k-value">{{ s.contacts | number }}</span><span class="k-label">{{ 'desktop.stats.contacts' | transloco }}</span></div>
              </div>
            } @else {
              <p class="pad">{{ 'desktop.error' | transloco }} <button type="button" class="btn-retry" (click)="loadStats()">{{ 'desktop.retry' | transloco }}</button></p>
            }
          }
        </div>
      }

      @if (visible('status')) {
        <div class="strip chips">
          @if (mailUnread() > 0) { <a class="chip" routerLink="/communication"><jl-icon name="mail" [size]="15" /><span class="c-val">{{ mailUnread() }}</span><span class="c-lbl">{{ 'desktop.status.mail' | transloco }}</span></a> }
          @if (scansCount() && scansCount()! > 0) { <a class="chip" routerLink="/scans"><jl-icon name="inbox" [size]="15" /><span class="c-val">{{ scansCount() }}</span><span class="c-lbl">{{ 'desktop.status.scans' | transloco }}</span></a> }
          @if (beaUnread() > 0) { <a class="chip" routerLink="/bea"><jl-icon name="shield" [size]="15" /><span class="c-val">{{ beaUnread() }}</span><span class="c-lbl">{{ 'desktop.status.bea' | transloco }}</span></a> }
          @if (mailUnread() === 0 && (!scansCount() || scansCount() === 0) && beaUnread() === 0) { <p class="muted pad">{{ 'desktop.status.nothing' | transloco }}</p> }
        </div>
      }

      <div class="grid">
        <!-- Zuletzt geändert -->
        @if (visible('recent')) {
          <article class="widget">
            <div class="w-head">
              <jl-icon name="cases" [size]="16" />
              <h2>{{ 'desktop.recent.title' | transloco }}</h2>
              <span class="w-spacer"></span>
              <ng-container *ngTemplateOutlet="userFilter; context: { w: 'recent' }"></ng-container>
              <a class="w-more" routerLink="/cases">{{ 'desktop.all' | transloco }} ›</a>
            </div>
            <div class="w-body">
              @if (recentLoading()) { <p class="muted pad">{{ 'desktop.loading' | transloco }}</p> }
              @else if (recentError()) { <p class="pad">{{ 'desktop.error' | transloco }} <button type="button" class="btn-retry" (click)="loadRecent()">{{ 'desktop.retry' | transloco }}</button></p> }
              @else {
                @for (c of recent(); track c.id) {
                  <a class="row case-row" [routerLink]="['/cases', c.id]">
                    <span class="az">{{ c.fileNumber }}</span>
                    <span class="rmain"><span class="rname">{{ c.name }}</span><span class="rsub">{{ c.subjectField || '—' }}{{ c.lawyer ? ' · ' + c.lawyer : '' }}</span></span>
                    <span class="rdate">{{ c.lastChanged | date: 'dd.MM.yy' }}</span>
                  </a>
                } @empty { <p class="muted pad">{{ 'desktop.recent.empty' | transloco }}</p> }
              }
            </div>
          </article>
        }

        <!-- Fällig -->
        @if (visible('due')) {
          <article class="widget">
            <div class="w-head">
              <jl-icon name="calendar" [size]="16" />
              <h2>{{ 'desktop.due.title' | transloco }}</h2>
              <span class="w-spacer"></span>
              <span class="due-range">
                <jl-icon name="clock" [size]="14" />
                <select [title]="'desktop.due.past' | transloco" [value]="cfg().dueSinceDays" (change)="setDueDays('since', $any($event.target).value)">
                  @for (o of pastOptions; track o) { <option [value]="o">{{ o }}</option> }
                </select>
                <span class="rsep">…</span>
                <select [title]="'desktop.due.future' | transloco" [value]="cfg().dueInDays" (change)="setDueDays('in', $any($event.target).value)">
                  @for (o of futureOptions; track o) { <option [value]="o">{{ o }}</option> }
                </select>
              </span>
              <ng-container *ngTemplateOutlet="userFilter; context: { w: 'due' }"></ng-container>
            </div>
            <div class="tabs">
              @for (t of dueTabs; track t) {
                <button type="button" [class.on]="cfg().dueType === t" (click)="setDueType(t)">
                  {{ (t === 'all' ? 'desktop.due.type.all' : 'desktop.due.type.' + t) | transloco }}
                  <span class="tab-count">{{ dueCounts()[t] }}</span>
                </button>
              }
            </div>
            <div class="w-body">
              @if (dueLoading()) { <p class="muted pad">{{ 'desktop.loading' | transloco }}</p> }
              @else if (dueError()) { <p class="pad">{{ 'desktop.error' | transloco }} <button type="button" class="btn-retry" (click)="loadDue()">{{ 'desktop.retry' | transloco }}</button></p> }
              @else {
                @for (g of dueGroups(); track g.key) {
                  <div class="due-group">
                    <div class="dg-head" [class.od]="g.overdue">
                      <span class="dg-date">{{ g.day | date: 'EEE dd.MM.yyyy' }}</span>
                      @if (g.caseFileNumber) { <a class="dg-case" [routerLink]="['/cases', g.caseId]">{{ g.caseFileNumber }} · {{ g.caseName }}</a> }
                    </div>
                    @for (d of g.items; track d.id) {
                      <div class="row due-row" [class.overdue]="d.overdue">
                        <span class="bar" [class]="d.type"></span>
                        <span class="rmain">
                          <span class="rname">{{ d.summary || ('desktop.due.noSummary' | transloco) }}</span>
                          <span class="rsub">{{ ('desktop.due.type.' + d.type) | transloco }}@if (d.type === 'event') { · {{ d.due | date: 'HH:mm' }} }@if (d.assignee) { · {{ d.assignee }} }</span>
                        </span>
                        <label class="btn-date" [title]="'desktop.due.reschedule' | transloco">
                          <jl-icon name="calendar" [size]="15" />
                          <input type="date" [value]="ymdInput(d.due)" (change)="onReschedule(d, $any($event.target).value)" />
                        </label>
                        <button type="button" class="btn-done" [title]="'desktop.due.markDone' | transloco" (click)="setDone(d)"><jl-icon name="check" [size]="15" /></button>
                      </div>
                    }
                  </div>
                } @empty { <p class="muted pad">{{ 'desktop.due.empty' | transloco }}</p> }
              }
            </div>
          </article>
        }

        <!-- Offene Rechnungen -->
        @if (visible('invoices')) {
          <article class="widget">
            <div class="w-head">
              <jl-icon name="euro" [size]="16" />
              <h2>{{ 'desktop.invoices.title' | transloco }}</h2>
              <a class="w-more" routerLink="/finance">{{ 'desktop.all' | transloco }} ›</a>
            </div>
            <div class="w-body">
              @if (invLoading()) { <p class="muted pad">{{ 'desktop.loading' | transloco }}</p> }
              @else if (invError()) { <p class="pad">{{ 'desktop.error' | transloco }} <button type="button" class="btn-retry" (click)="loadInvoices()">{{ 'desktop.retry' | transloco }}</button></p> }
              @else {
                @if (invoices(); as s) {
                  <div class="kpis">
                    <div class="kpi"><span class="k-value">{{ s.count }}</span><span class="k-label">{{ 'desktop.invoices.open' | transloco }}</span></div>
                    <div class="kpi"><span class="k-value">{{ s.totalGross | number: '1.2-2' }} {{ s.currency }}</span><span class="k-label">{{ 'desktop.invoices.sum' | transloco }}</span></div>
                  </div>
                  @for (i of s.top; track i.id) {
                    <a class="row inv-row" [routerLink]="i.caseId ? ['/cases', i.caseId] : null">
                      <span class="rmain"><span class="rname">{{ i.invoiceNumber || '—' }}</span><span class="rsub">{{ i.status }}</span></span>
                      <span class="inv-amount">{{ i.totalGross | number: '1.2-2' }} {{ i.currency }}</span>
                    </a>
                  } @empty { <p class="muted pad">{{ 'desktop.invoices.empty' | transloco }}</p> }
                }
              }
            </div>
          </article>
        }

        <!-- Nach Etikett -->
        @if (visible('tagged')) {
          <article class="widget">
            <div class="w-head">
              <jl-icon name="tag" [size]="16" />
              <h2>{{ 'desktop.tagged.title' | transloco }}</h2>
              <span class="w-spacer"></span>
              <ng-container *ngTemplateOutlet="userFilter; context: { w: 'tagged' }"></ng-container>
              <button type="button" class="icon-btn sm" (click)="subscribeOpen.set(!subscribeOpen())" [title]="'desktop.tagged.subscribe' | transloco"><jl-icon name="tag" [size]="15" /></button>
            </div>
            @if (subscribeOpen()) {
              <div class="w-cfg subscribe">
                <div class="sub-col">
                  <h4>{{ 'desktop.tagged.caseTags' | transloco }}</h4>
                  @for (t of allCaseTags(); track t) { <label class="cfg-row"><input type="checkbox" [checked]="cfg().caseTags.includes(t)" (change)="toggleTag('case', t)" /><span>{{ t }}</span></label> }
                  @if (!allCaseTags().length) { <p class="muted sm">{{ 'desktop.tagged.none' | transloco }}</p> }
                </div>
                <div class="sub-col">
                  <h4>{{ 'desktop.tagged.docTags' | transloco }}</h4>
                  @for (t of allDocTags(); track t) { <label class="cfg-row"><input type="checkbox" [checked]="cfg().docTags.includes(t)" (change)="toggleTag('doc', t)" /><span>{{ t }}</span></label> }
                  @if (!allDocTags().length) { <p class="muted sm">{{ 'desktop.tagged.none' | transloco }}</p> }
                </div>
              </div>
            }
            @if (hasSubscriptions()) {
              <div class="tabs wrap">
                <button type="button" [class.on]="activeTagKey() === ''" (click)="setActiveTag('')">{{ 'desktop.tagged.allTab' | transloco }}</button>
                @for (tab of tagTabs(); track tab.key) {
                  <button type="button" [class.on]="activeTagKey() === tab.key" (click)="setActiveTag(tab.key)">
                    <jl-icon [name]="tab.kind === 'doc' ? 'doc' : 'cases'" [size]="12" />
                    {{ tab.label }} <span class="tab-count">{{ tagCount(tab.key) }}</span>
                  </button>
                }
              </div>
            }
            <div class="w-body">
              @if (!hasSubscriptions()) { <p class="muted pad">{{ 'desktop.tagged.pick' | transloco }}</p> }
              @else if (taggedLoading()) { <p class="muted pad">{{ 'desktop.loading' | transloco }}</p> }
              @else {
                @for (it of taggedView(); track it.kind + it.id) {
                  <a class="row tag-row" [routerLink]="['/cases', it.caseId]" [queryParams]="it.kind === 'doc' ? { doc: it.id } : {}">
                    <jl-icon [name]="it.kind === 'doc' ? 'doc' : 'cases'" [size]="14" />
                    <span class="rmain">
                      <span class="rname">{{ it.primary }}</span>
                      @if (it.secondary) { <span class="rsub">{{ it.secondary }}</span> }
                      @if (it.tags.length) { <span class="tag-chips">@for (t of it.tags; track t) { <span class="tag-chip">{{ t }}</span> }</span> }
                    </span>
                  </a>
                } @empty { <p class="muted pad">{{ 'desktop.tagged.empty' | transloco }}</p> }
              }
            </div>
          </article>
        }

        <!-- Nachrichten an mich -->
        @if (visible('messages')) {
          <article class="widget">
            <div class="w-head"><jl-icon name="bell" [size]="16" /><h2>{{ 'desktop.messages.title' | transloco }}</h2></div>
            <div class="w-body">
              @if (msgLoading()) { <p class="muted pad">{{ 'desktop.loading' | transloco }}</p> }
              @else if (msgError()) { <p class="pad">{{ 'desktop.error' | transloco }} <button type="button" class="btn-retry" (click)="loadMessages()">{{ 'desktop.retry' | transloco }}</button></p> }
              @else {
                @for (m of messages(); track m.mentionId) {
                  <div class="row msg-row">
                    <span class="rmain">
                      <span class="rname">{{ m.sender }} <span class="msg-date">{{ m.sent | date: 'dd.MM. HH:mm' }}</span></span>
                      <span class="msg-text">{{ m.content }}</span>
                      @if (m.caseFileNumber) { <a class="msg-case" [routerLink]="['/cases', m.caseId]">{{ m.caseFileNumber }} · {{ m.caseName }}</a> }
                    </span>
                    <button type="button" class="btn-done" [title]="'desktop.messages.done' | transloco" (click)="markDone(m)"><jl-icon name="check" [size]="15" /></button>
                  </div>
                } @empty { <p class="muted pad">{{ 'desktop.messages.empty' | transloco }}</p> }
              }
            </div>
          </article>
        }
      </div>
    </section>
  `,
  styleUrl: './desktop.component.css',
})
export class DesktopComponent {
  private readonly dashboard = inject(DashboardService);
  private readonly auth = inject(AuthService);
  private readonly email = inject(EmailService);
  private readonly bea = inject(BeaService);

  protected readonly allWidgets = ALL_WIDGETS;
  protected readonly pastOptions = DUE_PAST_OPTIONS;
  protected readonly futureOptions = DUE_FUTURE_OPTIONS;
  protected readonly dueTabs: DueType[] = ['followup', 'respite', 'event', 'all'];
  protected readonly userName = computed(() => this.auth.user()?.displayName ?? '');

  protected readonly cfg = signal<DashboardConfig>(DEFAULT_DASHBOARD_CONFIG);
  protected readonly configOpen = signal(false);
  protected readonly subscribeOpen = signal(false);
  /** Which widget's user-filter popover is open ('' = none). */
  protected readonly userPop = signal<'' | UserWidget>('');
  private readonly persist$ = new Subject<void>();

  protected readonly users = signal<UserOption[]>([]);

  protected readonly recent = signal<RecentCase[]>([]);
  protected readonly recentLoading = signal(true);
  protected readonly recentError = signal(false);

  private readonly dueAll = signal<DueItem[]>([]);
  protected readonly dueLoading = signal(true);
  protected readonly dueError = signal(false);

  protected readonly invoices = signal<InvoiceSummary | null>(null);
  protected readonly invLoading = signal(true);
  protected readonly invError = signal(false);

  protected readonly allCaseTags = signal<string[]>([]);
  protected readonly allDocTags = signal<string[]>([]);
  protected readonly taggedData = signal<Record<string, TaggedItem[]>>({});
  protected readonly taggedLoading = signal(false);

  protected readonly messages = signal<MessageItem[]>([]);
  protected readonly msgLoading = signal(true);
  protected readonly msgError = signal(false);

  protected readonly stats = signal<DashStats | null>(null);
  protected readonly statsLoading = signal(true);
  protected readonly scansCount = signal<number | null>(null);

  protected readonly mailUnread = computed(() => {
    let sum = 0;
    for (const list of Object.values(this.email.folders())) { for (const f of list) { if (f.unreadCount > 0) { sum += f.unreadCount; } } }
    return sum;
  });
  protected readonly beaUnread = computed(() => {
    let sum = 0;
    for (const list of Object.values(this.bea.folders())) { for (const f of list) { if ((f.unreadMessageCount ?? 0) > 0) { sum += f.unreadMessageCount; } } }
    return sum;
  });

  protected readonly dueView = computed(() => {
    const c = this.cfg();
    const set = new Set(c.dueUsers);
    // NO display cap here: deadlines must never be hidden. The set is bounded by the date window.
    return this.dueAll()
      .filter((d) => (c.dueType === 'all' || d.type === c.dueType) && (!set.size || set.has(d.assignee)));
  });
  protected readonly dueCounts = computed(() => {
    const set = new Set(this.cfg().dueUsers);
    const base = this.dueAll().filter((d) => !set.size || set.has(d.assignee));
    return {
      all: base.length,
      followup: base.filter((d) => d.type === 'followup').length,
      respite: base.filter((d) => d.type === 'respite').length,
      event: base.filter((d) => d.type === 'event').length,
    } as Record<DueType, number>;
  });

  /** Due items grouped by case + day (Swing DesktopPanel style); the case is shown once per group. */
  protected readonly dueGroups = computed(() => {
    const groups: { key: string; day: Date; caseId: string; caseFileNumber: string; caseName: string; overdue: boolean; items: DueItem[] }[] = [];
    const idx = new Map<string, number>();
    for (const d of this.dueView()) {
      const day = new Date(d.due.getFullYear(), d.due.getMonth(), d.due.getDate());
      const key = (d.caseId || '-') + '|' + day.getTime();
      let gi = idx.get(key);
      if (gi === undefined) {
        gi = groups.length; idx.set(key, gi);
        groups.push({ key, day, caseId: d.caseId, caseFileNumber: d.caseFileNumber, caseName: d.caseName, overdue: false, items: [] });
      }
      groups[gi].items.push(d);
      if (d.overdue) { groups[gi].overdue = true; }
    }
    return groups;
  });

  protected readonly hasSubscriptions = computed(() => this.cfg().caseTags.length + this.cfg().docTags.length > 0);
  /** Subscribed tag tabs that currently have (user-filtered) items. */
  protected readonly tagTabs = computed<TagTab[]>(() => {
    const c = this.cfg();
    const all: TagTab[] = [
      ...c.caseTags.map((t) => ({ key: 'case:' + t, label: t, kind: 'case' as const })),
      ...c.docTags.map((t) => ({ key: 'doc:' + t, label: t, kind: 'doc' as const })),
    ];
    return all.filter((tab) => this.tagCount(tab.key) > 0);
  });
  /** '' means the "Alle" (union) tab; else a concrete tab key that still has items. */
  protected readonly activeTagKey = computed(() => {
    const active = this.cfg().activeTag;
    return active && this.tagTabs().some((t) => t.key === active) ? active : '';
  });
  protected readonly taggedView = computed(() => {
    const c = this.cfg();
    const key = this.activeTagKey();
    let items = key ? (this.taggedData()[key] ?? []) : this.unionItems();
    const set = new Set(c.taggedUsers);
    if (set.size) { items = items.filter((i) => i.kind !== 'case' || set.has(i.lawyer ?? '') || set.has(i.assistant ?? '')); }
    return items.slice(0, c.limits.tagged);
  });

  constructor() {
    this.persist$.pipe(debounceTime(600), takeUntilDestroyed()).subscribe(() => {
      this.dashboard.saveConfig(this.cfg()).subscribe({ error: () => { /* best-effort */ } });
    });
    this.dashboard.getConfig().subscribe({
      next: (cfg) => { this.cfg.set(cfg); this.afterConfig(); },
      error: () => { this.cfg.set({ ...DEFAULT_DASHBOARD_CONFIG }); this.afterConfig(); },
    });
  }

  protected visible(id: DashWidgetId): boolean { return this.cfg().widgets.includes(id); }

  /** The (user-filtered for case tabs) item count of a tag tab. */
  protected tagCount(key: string): number {
    let items = this.taggedData()[key] ?? [];
    if (key.startsWith('case:')) {
      const set = new Set(this.cfg().taggedUsers);
      if (set.size) { items = items.filter((i) => set.has(i.lawyer ?? '') || set.has(i.assistant ?? '')); }
    }
    return items.length;
  }

  /** The union of all subscribed tags' items, de-duplicated by kind+id. */
  private unionItems(): TaggedItem[] {
    const seen = new Set<string>();
    const out: TaggedItem[] = [];
    for (const tab of this.tagTabs()) {
      for (const it of this.taggedData()[tab.key] ?? []) {
        const k = it.kind + ':' + it.id;
        if (!seen.has(k)) { seen.add(k); out.push(it); }
      }
    }
    return out;
  }

  protected userCount(w: UserWidget): number { return this.userArr(w).length; }
  protected userChecked(w: UserWidget, id: string): boolean { return this.userArr(w).includes(id); }
  private userArr(w: UserWidget): string[] {
    const c = this.cfg();
    return w === 'recent' ? c.recentUsers : w === 'due' ? c.dueUsers : c.taggedUsers;
  }

  private afterConfig(): void {
    if (this.visible('recent') || this.visible('due') || this.visible('tagged')) {
      this.dashboard.listUsers().subscribe((u) => this.users.set(u));
    }
    if (this.visible('recent')) { this.loadRecent(); }
    if (this.visible('due')) { this.loadDue(); }
    if (this.visible('invoices')) { this.loadInvoices(); }
    if (this.visible('messages')) { this.loadMessages(); }
    if (this.visible('stats')) { this.loadStats(); }
    if (this.visible('tagged')) { this.loadTagLists(); this.loadTaggedData(); }
    if (this.visible('status')) { this.email.ensureStructure(); this.loadScans(); }
  }

  // --- config edits (persisted, debounced) ---

  private persist(): void { this.persist$.next(); }

  protected toggleWidget(id: DashWidgetId): void {
    const on = this.visible(id);
    this.cfg.update((c) => ({ ...c, widgets: on ? c.widgets.filter((w) => w !== id) : [...c.widgets, id] }));
    this.persist();
    if (!on) {
      if ((id === 'recent' || id === 'due' || id === 'tagged') && !this.users().length) {
        this.dashboard.listUsers().subscribe((u) => this.users.set(u));
      }
      switch (id) {
        case 'recent': this.loadRecent(); break;
        case 'due': this.loadDue(); break;
        case 'invoices': this.loadInvoices(); break;
        case 'messages': this.loadMessages(); break;
        case 'stats': this.loadStats(); break;
        case 'tagged': this.loadTagLists(); this.loadTaggedData(); break;
        case 'status': this.email.ensureStructure(); this.loadScans(); break;
      }
    }
  }

  protected toggleUser(w: UserWidget, id: string): void {
    const field = w === 'recent' ? 'recentUsers' : w === 'due' ? 'dueUsers' : 'taggedUsers';
    const has = this.userArr(w).includes(id);
    this.cfg.update((c) => ({ ...c, [field]: has ? c[field].filter((x) => x !== id) : [...c[field], id] }));
    this.persist();
    if (w === 'recent') { this.loadRecent(); } // due & tagged filter client-side
  }

  protected setDueDays(which: 'since' | 'in', value: string): void {
    const n = Math.max(0, parseInt(value, 10) || 0);
    this.cfg.update((c) => ({ ...c, dueSinceDays: which === 'since' ? n : c.dueSinceDays, dueInDays: which === 'in' ? n : c.dueInDays }));
    this.persist();
    this.loadDue();
  }

  protected setDueType(t: DueType): void {
    this.cfg.update((c) => ({ ...c, dueType: t }));
    this.persist();
  }

  protected toggleTag(kind: 'case' | 'doc', tag: string): void {
    const field = kind === 'case' ? 'caseTags' : 'docTags';
    const key = kind + ':' + tag;
    const has = this.cfg()[field].includes(tag);
    this.cfg.update((c) => ({ ...c, [field]: has ? c[field].filter((t) => t !== tag) : [...c[field], tag] }));
    this.persist();
    if (has) {
      this.taggedData.update((m) => { const n = { ...m }; delete n[key]; return n; });
    } else {
      this.fetchTag(kind, tag);
    }
  }

  protected setActiveTag(key: string): void {
    this.cfg.update((c) => ({ ...c, activeTag: key }));
    this.persist();
  }

  // --- loaders ---

  protected loadRecent(): void {
    this.recentLoading.set(true); this.recentError.set(false);
    this.dashboard.recentCases(this.cfg().limits.recent, this.cfg().recentUsers).subscribe({
      next: (rows) => { this.recent.set(rows); this.recentLoading.set(false); },
      error: () => { this.recentError.set(true); this.recentLoading.set(false); },
    });
  }

  protected loadDue(): void {
    this.dueLoading.set(true); this.dueError.set(false);
    this.dashboard.dueWindow(this.cfg().dueSinceDays, this.cfg().dueInDays).subscribe({
      next: (rows) => { this.dueAll.set(rows); this.dueLoading.set(false); },
      error: () => { this.dueError.set(true); this.dueLoading.set(false); },
    });
  }

  /** yyyy-MM-dd for a native date input's value. */
  protected ymdInput(d: Date): string {
    const y = d.getFullYear(), m = String(d.getMonth() + 1).padStart(2, '0'), day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }

  /** Marks a due entry done, then reloads the window (done entries drop out — status=open). */
  protected setDone(d: DueItem): void {
    this.dashboard.setDueDone(d, true).subscribe({ next: () => this.loadDue(), error: () => { /* keep the row */ } });
  }

  /** Reschedules a due entry to the picked date, preserving its time-of-day and duration. */
  protected onReschedule(d: DueItem, dateStr: string): void {
    if (!dateStr) { return; }
    const [y, m, day] = dateStr.split('-').map(Number);
    if (!y || !m || !day) { return; }
    const nb = new Date(d.due);
    nb.setFullYear(y, m - 1, day);
    const dur = Math.max(0, (d.end ? d.end.getTime() : d.due.getTime()) - d.due.getTime());
    const begin = nb.getTime();
    this.dashboard.rescheduleDue(d, begin, begin + dur).subscribe({ next: () => this.loadDue(), error: () => { /* leave as-is */ } });
  }

  protected loadInvoices(): void {
    this.invLoading.set(true); this.invError.set(false);
    this.dashboard.openInvoices(6).subscribe({
      next: (s) => { this.invoices.set(s); this.invLoading.set(false); },
      error: () => { this.invError.set(true); this.invLoading.set(false); },
    });
  }

  private loadTagLists(): void {
    this.dashboard.caseTags().subscribe((t) => this.allCaseTags.set(t));
    this.dashboard.docTags().subscribe((t) => this.allDocTags.set(t));
  }

  private loadTaggedData(): void {
    const c = this.cfg();
    if (!c.caseTags.length && !c.docTags.length) { this.taggedData.set({}); return; }
    this.taggedLoading.set(true);
    let pending = c.caseTags.length + c.docTags.length;
    const done = () => { if (--pending <= 0) { this.taggedLoading.set(false); } };
    for (const t of c.caseTags) { this.fetchTag('case', t, done); }
    for (const t of c.docTags) { this.fetchTag('doc', t, done); }
  }

  private fetchTag(kind: 'case' | 'doc', tag: string, done?: () => void): void {
    const key = kind + ':' + tag;
    const req = kind === 'case' ? this.dashboard.casesByTag(tag) : this.dashboard.documentsByTag(tag);
    req.subscribe({
      next: (items) => { this.taggedData.update((m) => ({ ...m, [key]: items })); done?.(); },
      error: () => { this.taggedData.update((m) => ({ ...m, [key]: [] })); done?.(); },
    });
  }

  protected loadMessages(): void {
    this.msgLoading.set(true); this.msgError.set(false);
    this.dashboard.messagesToMe(this.auth.user()?.username ?? '', this.cfg().limits.messages).subscribe({
      next: (rows) => { this.messages.set(rows); this.msgLoading.set(false); },
      error: () => { this.msgError.set(true); this.msgLoading.set(false); },
    });
  }

  protected loadStats(): void {
    this.statsLoading.set(true);
    this.dashboard.stats().subscribe({
      next: (s) => { this.stats.set(s); this.statsLoading.set(false); },
      error: () => { this.stats.set(null); this.statsLoading.set(false); },
    });
  }

  private loadScans(): void {
    this.dashboard.newScansCount().subscribe({ next: (n) => this.scansCount.set(n), error: () => this.scansCount.set(null) });
  }

  protected markDone(m: MessageItem): void {
    this.dashboard.markMentionDone(m.mentionId).subscribe({
      next: () => this.messages.update((list) => list.filter((x) => x.mentionId !== m.mentionId)),
      error: () => { /* leave the row; the user can retry */ },
    });
  }
}
