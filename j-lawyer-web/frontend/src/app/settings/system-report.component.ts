import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { TranslocoModule } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { AuthService } from '../core/auth/auth.service';
import { SystemReport, SystemSettingsService } from './system-settings.service';

/**
 * Read-only system report — the web equivalent of the desktop "Systemreport" dialog: server
 * version/host, the JVM system properties and a tail of the server log. Needs `sysAdminRole`.
 */
@Component({
  selector: 'jl-system-report',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent],
  template: `
    <div class="sr">
      @if (loading()) {
        <p class="sr-muted">{{ 'settings.loading' | transloco }}</p>
      } @else if (loadError()) {
        <p class="sr-error">{{ 'settings.loadError' | transloco }}</p>
      } @else if (report()) {
        @if (report()!; as r) {
        <div class="sr-bar">
          <button type="button" class="btn-ghost" (click)="load()"><jl-icon name="refresh" [size]="15" /><span>{{ 'settings.report.refresh' | transloco }}</span></button>
        </div>

        <fieldset class="sr-sec">
          <legend>{{ 'settings.report.info' | transloco }}</legend>
          <div class="sr-info"><span>{{ 'settings.report.version' | transloco }}</span><b>{{ r.serverVersion }}</b></div>
          <div class="sr-info"><span>{{ 'settings.report.host' | transloco }}</span><b>{{ r.hostName }}</b></div>
          <div class="sr-info"><span>{{ 'settings.report.ip' | transloco }}</span><b>{{ r.ipAddress }}</b></div>
        </fieldset>

        <fieldset class="sr-sec">
          <legend>{{ 'settings.report.properties' | transloco }}</legend>
          <div class="sr-scroll">
            <table class="sr-props">
              <tbody>
                @for (p of r.properties; track p.key) {
                  <tr><td class="k">{{ p.key }}</td><td class="v">{{ p.value }}</td></tr>
                }
              </tbody>
            </table>
          </div>
        </fieldset>

        <fieldset class="sr-sec">
          <legend>{{ 'settings.report.log' | transloco }}</legend>
          <div class="sr-loglines">
            <label>{{ 'settings.report.lines' | transloco }}</label>
            <select [value]="lines()" (change)="setLines($any($event.target).value)">
              @for (n of lineOptions; track n) { <option [value]="n">{{ n }}</option> }
            </select>
            <button type="button" class="btn-ghost" [disabled]="!r.serverLog" (click)="downloadLog(r.serverLog)">
              <jl-icon name="download" [size]="15" /><span>{{ 'settings.report.download' | transloco }}</span>
            </button>
          </div>
          <pre class="sr-log">{{ r.serverLog }}</pre>
        </fieldset>
        }
      }
    </div>
  `,
  styles: [`
    :host { display: block; }
    .sr { display: flex; flex-direction: column; gap: 14px; }
    .sr-muted { color: var(--jl-ink-soft); font-size: .88rem; }
    .sr-error { color: var(--jl-red); font-size: .84rem; }
    .sr-bar { display: flex; }
    .sr-sec { border: 1px solid var(--jl-line); border-radius: 10px; padding: 12px 14px; margin: 0; display: flex; flex-direction: column; gap: 8px; }
    .sr-sec legend { font-size: .8rem; font-weight: 700; color: var(--jl-ink-soft); padding: 0 6px; text-transform: uppercase; letter-spacing: .04em; }
    .sr-info { display: flex; gap: 10px; font-size: .88rem; }
    .sr-info span { flex: 0 0 160px; color: var(--jl-ink-faint); }
    .sr-scroll { max-height: 260px; overflow: auto; border: 1px solid var(--jl-line); border-radius: 8px; }
    table.sr-props { width: 100%; border-collapse: collapse; font-size: .8rem; font-family: ui-monospace, SFMono-Regular, Menlo, monospace; }
    table.sr-props td { padding: 4px 8px; border-bottom: 1px solid var(--jl-line); vertical-align: top; }
    table.sr-props .k { color: var(--jl-ink-soft); white-space: nowrap; }
    table.sr-props .v { color: var(--jl-ink); word-break: break-all; }
    .sr-loglines { display: flex; align-items: center; gap: 8px; font-size: .82rem; color: var(--jl-ink-faint); }
    .sr-loglines select { font: inherit; padding: 4px 8px; border: 1px solid var(--jl-line-strong); border-radius: 7px; background: var(--jl-surface); color: var(--jl-ink); }
    .sr-log { max-height: 360px; overflow: auto; background: var(--jl-surface-alt); border-radius: 8px; padding: 10px 12px; margin: 0;
      font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: .76rem; color: var(--jl-ink); white-space: pre; }
    .btn-ghost { display: inline-flex; align-items: center; gap: 6px; font: inherit; font-size: .84rem; padding: 7px 12px; border: 1px solid var(--jl-line-strong); border-radius: 8px; background: var(--jl-surface); color: var(--jl-ink); cursor: pointer; }
  `],
})
export class SystemReportComponent implements OnInit {
  private readonly api = inject(SystemSettingsService);
  private readonly auth = inject(AuthService);

  protected readonly lineOptions = [200, 500, 1000, 2000, 5000];
  protected readonly lines = signal(500);
  protected readonly report = signal<SystemReport | null>(null);
  protected readonly loading = signal(false);
  protected readonly loadError = signal(false);
  protected readonly canView = computed(() => this.auth.hasRole('sysAdminRole'));

  ngOnInit(): void { this.load(); }

  protected setLines(v: string): void { const n = parseInt(v, 10); this.lines.set(Number.isFinite(n) ? n : 500); this.load(); }

  protected load(): void {
    this.loading.set(true); this.loadError.set(false);
    this.api.getSystemReport(this.lines()).subscribe({
      next: (r) => { this.report.set(r); this.loading.set(false); },
      error: () => { this.loadError.set(true); this.loading.set(false); },
    });
  }

  /** Downloads the server log as a plain-text file (client-side blob; no external host). */
  protected downloadLog(logText: string): void {
    const host = this.report()?.hostName || 'server';
    const blob = new Blob([logText ?? ''], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `j-lawyer-${host}-server.log`;
    a.click();
    URL.revokeObjectURL(url);
  }
}
