import { computed, inject, Injectable, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { interval } from 'rxjs';
import { CasesService } from './cases.service';
import { TimesheetConfigService } from '../settings/timesheet-config.service';

/**
 * App-wide time-tracking state for the header stopwatch: keeps a live count of the current user's
 * running timers (polled + refreshed on demand) and owns the open/closed state of the cross-case
 * log dialog. Any component that starts/stops a timer should call {@link refresh} so the header
 * indicator updates immediately rather than waiting for the next poll.
 */
@Injectable({ providedIn: 'root' })
export class TimesheetTrackingService {
  private readonly cases = inject(CasesService);
  private readonly config = inject(TimesheetConfigService);

  /** Number of the current user's running timers (drives the blinking header indicator). */
  readonly runningCount = signal(0);
  /** Whether the cross-case log dialog is open. */
  readonly dialogOpen = signal(false);
  /** Server setting: warn before starting a parallel recording (jlawyer.server.timesheets.parallellogswarning). */
  private readonly parallelWarnEnabled = signal(false);
  /** Whether a "parallel recording" warning should be shown when starting another timer now. */
  readonly warnBeforeParallelStart = computed(() => this.parallelWarnEnabled() && this.runningCount() > 0);
  /** Server setting: how a bare number (no unit) is interpreted in manual duration entry. */
  readonly numericInput = signal<'minutes' | 'hours' | 'reject'>('minutes');

  constructor() {
    this.refresh();
    this.config.getSettings().subscribe({
      next: (s) => { this.parallelWarnEnabled.set(s.parallelLogsWarning); this.numericInput.set(s.numericInput); },
      error: () => { /* keep defaults */ },
    });
    // Fallback poll so the indicator recovers even if a refresh() call is missed (e.g. another device).
    interval(30_000).pipe(takeUntilDestroyed()).subscribe(() => this.refresh());
  }

  /** Re-reads the running-timer count from the server. */
  refresh(): void {
    this.cases.runningCount().subscribe((n) => this.runningCount.set(n));
  }

  open(): void {
    this.dialogOpen.set(true);
    this.refresh();
  }

  close(): void {
    this.dialogOpen.set(false);
  }
}
