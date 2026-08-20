import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { AuthService } from '../core/auth/auth.service';
import { MonitoringSettings, MonitoringSnapshot, SystemSettingsService } from './system-settings.service';

const EMPTY: MonitoringSettings = {
  cpuWarn: 80, cpuError: 90, memWarn: 80, memError: 90, diskWarn: 80, diskError: 90, vmWarn: 75, vmError: 85,
  monitorCpu: true, monitorRam: true, monitorDisk: true, monitorJava: true,
  notify: false, notifyBackupSuccess: true, notifyBackupFailure: true,
};

interface Bar { labelKey: string; percent: number; detail: string; }

/**
 * Server monitoring — the web equivalent of the desktop "Servermonitoring" dialog: a live snapshot
 * (CPU/RAM/Java-VM/disk usage + last status) plus the threshold / monitored-resource / notification
 * configuration. Needs `sysAdminRole` (enforced server-side).
 */
@Component({
  selector: 'jl-server-monitoring',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent],
  template: `
    <div class="ss">
      @if (!canEdit()) { <p class="ss-hint">{{ 'settings.readOnlyHint' | transloco }}</p> }

      <!-- live snapshot -->
      <fieldset class="ss-sec">
        <legend>{{ 'settings.monitor.snapshot' | transloco }}</legend>
        @if (snapLoading()) {
          <p class="ss-muted">{{ 'settings.loading' | transloco }}</p>
        } @else if (snapError()) {
          <p class="ss-error">{{ 'settings.loadError' | transloco }}</p>
        } @else {
          @for (b of bars(); track b.labelKey) {
            <div class="mon-row">
              <span class="mon-label">{{ b.labelKey | transloco }}</span>
              <span class="mon-bar"><span class="mon-fill" [class.warn]="b.percent >= 75" [class.err]="b.percent >= 90" [style.width.%]="b.percent"></span></span>
              <span class="mon-val">{{ b.percent }}% · {{ b.detail }}</span>
            </div>
          }
          @if (snapshot()?.lastStatus) { <p class="ss-note">{{ 'settings.monitor.lastStatus' | transloco }}: {{ snapshot()?.lastStatus }}</p> }
          <button type="button" class="btn-ghost" (click)="loadSnapshot()"><jl-icon name="refresh" [size]="15" /><span>{{ 'settings.monitor.refresh' | transloco }}</span></button>
        }
      </fieldset>

      @if (loading()) {
        <p class="ss-muted">{{ 'settings.loading' | transloco }}</p>
      } @else if (loadError()) {
        <p class="ss-error">{{ 'settings.loadError' | transloco }}</p>
      } @else {
        <fieldset class="ss-sec">
          <legend>{{ 'settings.monitor.thresholds' | transloco }}</legend>
          <table class="thr">
            <thead><tr><th></th><th>{{ 'settings.monitor.monitored' | transloco }}</th><th>{{ 'settings.monitor.warn' | transloco }}</th><th>{{ 'settings.monitor.error' | transloco }}</th></tr></thead>
            <tbody>
              @for (m of metrics; track m.key) {
                <tr>
                  <td>{{ m.labelKey | transloco }}</td>
                  <td><input type="checkbox" [checked]="boolVal(m.enabledKey)" [disabled]="!canEdit()" (change)="patch(m.enabledKey, $any($event.target).checked)" /></td>
                  <td><input type="number" min="0" max="100" [value]="numVal(m.warnKey)" [disabled]="!canEdit()" (input)="patchNum(m.warnKey, $any($event.target).value)" /></td>
                  <td><input type="number" min="0" max="100" [value]="numVal(m.errorKey)" [disabled]="!canEdit()" (input)="patchNum(m.errorKey, $any($event.target).value)" /></td>
                </tr>
              }
            </tbody>
          </table>
        </fieldset>

        <fieldset class="ss-sec">
          <legend>{{ 'settings.monitor.notifications' | transloco }}</legend>
          <label class="ss-check"><input type="checkbox" [checked]="draft().notify" [disabled]="!canEdit()" (change)="patch('notify', $any($event.target).checked)" /><span>{{ 'settings.monitor.notify' | transloco }}</span></label>
          <label class="ss-check"><input type="checkbox" [checked]="draft().notifyBackupSuccess" [disabled]="!canEdit()" (change)="patch('notifyBackupSuccess', $any($event.target).checked)" /><span>{{ 'settings.monitor.notifyBackupSuccess' | transloco }}</span></label>
          <label class="ss-check"><input type="checkbox" [checked]="draft().notifyBackupFailure" [disabled]="!canEdit()" (change)="patch('notifyBackupFailure', $any($event.target).checked)" /><span>{{ 'settings.monitor.notifyBackupFailure' | transloco }}</span></label>
          <p class="ss-note">{{ 'settings.monitor.notifyHint' | transloco }}</p>
        </fieldset>

        <div class="ss-foot">
          @if (savedOk()) { <span class="ss-ok">{{ 'settings.savedOk' | transloco }}</span> }
          @if (saveError()) { <span class="ss-error">{{ 'settings.saveError' | transloco }}</span> }
          <button type="button" class="btn-primary" [disabled]="!canEdit() || saving()" (click)="submit()">{{ (saving() ? 'settings.saving' : 'settings.save') | transloco }}</button>
        </div>
      }
    </div>
  `,
  styles: [`
    :host { display: block; }
    .ss { max-width: 720px; display: flex; flex-direction: column; gap: 14px; }
    .ss-hint { margin: 0; font-size: .82rem; color: #b26a00; }
    .ss-muted { color: var(--jl-ink-soft); font-size: .88rem; }
    .ss-error { color: var(--jl-red); font-size: .84rem; }
    .ss-note { margin: 4px 0 0; font-size: .78rem; color: var(--jl-ink-faint); }
    .ss-sec { border: 1px solid var(--jl-line); border-radius: 10px; padding: 12px 14px; margin: 0; display: flex; flex-direction: column; gap: 8px; }
    .ss-sec legend { font-size: .8rem; font-weight: 700; color: var(--jl-ink-soft); padding: 0 6px; text-transform: uppercase; letter-spacing: .04em; }
    .ss-check { display: flex; align-items: center; gap: 8px; font-size: .9rem; color: var(--jl-ink); }
    .ss-check input { width: 16px; height: 16px; }
    .mon-row { display: grid; grid-template-columns: 90px 1fr auto; align-items: center; gap: 10px; font-size: .84rem; }
    .mon-label { color: var(--jl-ink-soft); }
    .mon-bar { height: 10px; border-radius: 6px; background: var(--jl-surface-alt); overflow: hidden; }
    .mon-fill { display: block; height: 100%; background: var(--jl-green, #1a7f37); }
    .mon-fill.warn { background: #d9840a; }
    .mon-fill.err { background: var(--jl-red); }
    .mon-val { color: var(--jl-ink-faint); font-variant-numeric: tabular-nums; white-space: nowrap; }
    table.thr { width: 100%; border-collapse: collapse; font-size: .84rem; }
    table.thr th { text-align: left; font-size: .72rem; text-transform: uppercase; letter-spacing: .04em; color: var(--jl-ink-faint); padding: 4px 8px; }
    table.thr td { padding: 4px 8px; border-top: 1px solid var(--jl-line); }
    table.thr input[type=number] { width: 72px; font: inherit; font-size: .88rem; padding: 5px 7px; border: 1px solid var(--jl-line-strong); border-radius: 7px; background: var(--jl-surface); color: var(--jl-ink); }
    .ss-foot { display: flex; align-items: center; justify-content: flex-end; gap: 12px; }
    .ss-ok { color: var(--jl-green, #1a7f37); font-size: .84rem; }
    .btn-ghost { align-self: flex-start; display: inline-flex; align-items: center; gap: 6px; font: inherit; font-size: .84rem; padding: 7px 12px; border: 1px solid var(--jl-line-strong); border-radius: 8px; background: var(--jl-surface); color: var(--jl-ink); cursor: pointer; }
    .btn-primary { font: inherit; font-size: .86rem; font-weight: 600; padding: 9px 18px; border: 0; border-radius: 8px; background: var(--jl-blue); color: #fff; cursor: pointer; }
    .btn-primary:disabled { opacity: .55; cursor: default; }
  `],
})
export class ServerMonitoringComponent implements OnInit {
  private readonly api = inject(SystemSettingsService);
  private readonly auth = inject(AuthService);

  protected readonly metrics: { key: string; labelKey: string; enabledKey: keyof MonitoringSettings; warnKey: keyof MonitoringSettings; errorKey: keyof MonitoringSettings }[] = [
    { key: 'cpu', labelKey: 'settings.monitor.cpu', enabledKey: 'monitorCpu', warnKey: 'cpuWarn', errorKey: 'cpuError' },
    { key: 'ram', labelKey: 'settings.monitor.ram', enabledKey: 'monitorRam', warnKey: 'memWarn', errorKey: 'memError' },
    { key: 'vm', labelKey: 'settings.monitor.vm', enabledKey: 'monitorJava', warnKey: 'vmWarn', errorKey: 'vmError' },
    { key: 'disk', labelKey: 'settings.monitor.disk', enabledKey: 'monitorDisk', warnKey: 'diskWarn', errorKey: 'diskError' },
  ];

  protected readonly draft = signal<MonitoringSettings>({ ...EMPTY });
  protected readonly loading = signal(false);
  protected readonly loadError = signal(false);
  protected readonly saving = signal(false);
  protected readonly saveError = signal(false);
  protected readonly savedOk = signal(false);
  protected readonly canEdit = computed(() => this.auth.hasRole('sysAdminRole'));

  protected readonly snapshot = signal<MonitoringSnapshot | null>(null);
  protected readonly snapLoading = signal(false);
  protected readonly snapError = signal(false);

  protected readonly bars = computed<Bar[]>(() => {
    const s = this.snapshot();
    if (!s) { return []; }
    return [
      { labelKey: 'settings.monitor.cpu', percent: s.cpuPercent, detail: '' },
      { labelKey: 'settings.monitor.ram', percent: this.pct(s.memoryUse, s.memoryMax), detail: `${this.mb(s.memoryUse)} / ${this.mb(s.memoryMax)} MB` },
      { labelKey: 'settings.monitor.vm', percent: this.pct(s.vmMemoryUse, s.vmMemoryMax), detail: `${this.mb(s.vmMemoryUse)} / ${this.mb(s.vmMemoryMax)} MB` },
      { labelKey: 'settings.monitor.disk', percent: this.pct(s.diskUse, s.diskMax), detail: `${this.gb(s.diskUse)} / ${this.gb(s.diskMax)} GB` },
    ];
  });

  ngOnInit(): void {
    this.loading.set(true);
    this.api.getMonitoring().subscribe({
      next: (s) => { this.draft.set({ ...s }); this.loading.set(false); },
      error: () => { this.loadError.set(true); this.loading.set(false); },
    });
    this.loadSnapshot();
  }

  protected loadSnapshot(): void {
    this.snapLoading.set(true); this.snapError.set(false);
    this.api.getMonitoringSnapshot().subscribe({
      next: (s) => { this.snapshot.set(s ?? null); this.snapLoading.set(false); },
      error: () => { this.snapError.set(true); this.snapLoading.set(false); },
    });
  }

  protected boolVal(key: keyof MonitoringSettings): boolean { return this.draft()[key] === true; }
  protected numVal(key: keyof MonitoringSettings): number { return Number(this.draft()[key] ?? 0); }
  protected patch<K extends keyof MonitoringSettings>(key: K, value: MonitoringSettings[K]): void { this.draft.update((d) => ({ ...d, [key]: value })); this.savedOk.set(false); }
  protected patchNum(key: keyof MonitoringSettings, value: string): void { const n = parseInt(value, 10); this.patch(key, (Number.isFinite(n) ? n : 0) as MonitoringSettings[typeof key]); }

  private pct(use: number, max: number): number { return max > 0 ? Math.min(100, Math.round((use / max) * 100)) : 0; }
  private mb(bytes: number): number { return Math.round(bytes / (1024 * 1024)); }
  private gb(bytes: number): number { return Math.round(bytes / (1024 * 1024 * 1024)); }

  protected submit(): void {
    if (!this.canEdit() || this.saving()) { return; }
    this.saving.set(true); this.saveError.set(false); this.savedOk.set(false);
    this.api.saveMonitoring(this.draft()).subscribe({
      next: () => { this.saving.set(false); this.savedOk.set(true); },
      error: () => { this.saving.set(false); this.saveError.set(true); },
    });
  }
}
