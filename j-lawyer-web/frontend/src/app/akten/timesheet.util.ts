import { TimesheetPosition } from './case.models';

/** Elapsed milliseconds of a completed position (0 while running or unstarted). */
export function positionMillis(p: TimesheetPosition): number {
  if (!p.started || !p.stopped) {
    return 0;
  }
  const start = new Date(p.started).getTime();
  const stop = new Date(p.stopped).getTime();
  return stop > start ? stop - start : 0;
}

/** Formats a duration in milliseconds as "h:mm" (e.g. 125 min -> "2:05"). */
export function formatDurationMs(ms: number): string {
  const totalMinutes = Math.round(ms / 60000);
  const h = Math.floor(totalMinutes / 60);
  const m = totalMinutes % 60;
  return `${h}:${String(m).padStart(2, '0')}`;
}

/** Live elapsed time of a running position as "h:mm:ss", given a "now" timestamp (ms). */
export function runningElapsed(p: TimesheetPosition, now: number): string {
  if (!p.started) {
    return '0:00:00';
  }
  const start = new Date(p.started).getTime();
  const total = Math.floor(Math.max(0, now - start) / 1000);
  const h = Math.floor(total / 3600);
  const m = Math.floor((total % 3600) / 60);
  const s = total % 60;
  return `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
}

/**
 * Converts a datetime-local value ("yyyy-MM-ddTHH:mm", local time) to the server's expected
 * "yyyy-MM-dd'T'HH:mm:ss±HHMM" (RFC822 offset, not a literal 'Z'). Empty in → empty out.
 */
export function toServerDateTime(local: string): string {
  if (!local) {
    return '';
  }
  const d = new Date(local);
  if (isNaN(d.getTime())) {
    return '';
  }
  const p = (n: number) => String(n).padStart(2, '0');
  const off = -d.getTimezoneOffset(); // minutes east of UTC
  const sign = off >= 0 ? '+' : '-';
  const abs = Math.abs(off);
  const offset = `${sign}${p(Math.floor(abs / 60))}${p(abs % 60)}`;
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}${offset}`;
}

/** Net sum of a position list. */
export function sumNet(positions: TimesheetPosition[]): number {
  return positions.reduce((acc, p) => acc + (p.total || 0), 0);
}

/** Sum of the still-unbilled (no invoice) positions — the invoiceable amount. */
export function sumInvoiceable(positions: TimesheetPosition[]): number {
  return positions.reduce((acc, p) => acc + (p.invoiceId ? 0 : (p.total || 0)), 0);
}

/** Total tracked duration of completed positions, formatted "h:mm". */
export function sumDuration(positions: TimesheetPosition[]): string {
  return formatDurationMs(positions.reduce((acc, p) => acc + positionMillis(p), 0));
}
