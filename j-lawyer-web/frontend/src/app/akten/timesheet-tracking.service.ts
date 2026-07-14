import { inject, Injectable, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { interval } from 'rxjs';
import { CasesService } from './cases.service';

/**
 * App-wide time-tracking state for the header stopwatch: keeps a live count of the current user's
 * running timers (polled + refreshed on demand) and owns the open/closed state of the cross-case
 * log dialog. Any component that starts/stops a timer should call {@link refresh} so the header
 * indicator updates immediately rather than waiting for the next poll.
 */
@Injectable({ providedIn: 'root' })
export class TimesheetTrackingService {
  private readonly cases = inject(CasesService);

  /** Number of the current user's running timers (drives the blinking header indicator). */
  readonly runningCount = signal(0);
  /** Whether the cross-case log dialog is open. */
  readonly dialogOpen = signal(false);

  constructor() {
    this.refresh();
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
