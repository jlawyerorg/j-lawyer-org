/**
 * Small, timezone-safe date helpers shared by the calendar service (fetch range) and the
 * calendar component (agenda grouping + day/week/month sheet grids). All operate on local
 * time and never mutate their input. Weeks start on Monday (German convention).
 */

/** Midnight (local) of the given date. */
export function startOfDay(d: Date): Date {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate());
}

/** A new date `n` days after `d` (n may be negative), at midnight. */
export function addDays(d: Date, n: number): Date {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate() + n);
}

/** Monday (local, midnight) of the week containing `d`. */
export function startOfWeekMon(d: Date): Date {
  const day = d.getDay(); // 0=Sun..6=Sat
  const back = (day + 6) % 7; // days since Monday
  return addDays(startOfDay(d), -back);
}

/** First day of `d`'s month (midnight). */
export function startOfMonth(d: Date): Date {
  return new Date(d.getFullYear(), d.getMonth(), 1);
}

/** Last day of `d`'s month (midnight). */
export function endOfMonth(d: Date): Date {
  return new Date(d.getFullYear(), d.getMonth() + 1, 0);
}

/** Local yyyy-MM-dd (no timezone shift), used for range params and day grouping/matching. */
export function isoDay(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

/** True when both dates fall on the same local calendar day. */
export function sameDay(a: Date, b: Date): boolean {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
}
