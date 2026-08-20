/**
 * Geometry for the case view's horizontal timeline (Zeitstrahl): maps a case's due dates onto a
 * non-linear time axis where near dates are stretched and distant ones compressed, so upcoming
 * Fristen / Wiedervorlagen / Termine are readable at a glance without a range selector.
 *
 * Pure functions, no Angular — the scale is the part most likely to be re-tuned, and this way it
 * can be asserted with a plain node script.
 */

/** The three server-side entry kinds: Frist, Wiedervorlage, Termin. */
export type DueKind = 'respite' | 'followup' | 'event';

/**
 * Derives the entry kind from the raw server token (`DueDate.restType`).
 *
 * Do NOT use `DueDate.type` for this — it collapses EVENT into 'followup' and only distinguishes
 * two of the three kinds. The returned token doubles as the `kalender.type.*` i18n key suffix and
 * as the marker's CSS class.
 */
export function dueKind(restType: string): DueKind {
  const t = (restType || '').toUpperCase();
  return t === 'RESPITE' ? 'respite' : t === 'EVENT' ? 'event' : 'followup';
}

/**
 * Whole calendar days from today to `iso`; negative for the past, null when unparseable.
 *
 * Both dates are projected onto UTC midnight before subtracting, which keeps the result stable
 * across DST switches and independent of the time of day.
 */
export function daysFromToday(iso: string, now: Date): number | null {
  if (!iso) {
    return null;
  }
  const t = new Date(iso);
  if (Number.isNaN(t.getTime())) {
    return null;
  }
  const a = Date.UTC(t.getFullYear(), t.getMonth(), t.getDate());
  const b = Date.UTC(now.getFullYear(), now.getMonth(), now.getDate());
  return Math.round((a - b) / 86_400_000);
}

/** Knee of the logarithm, in days — roughly "one week is the unit of near". */
const K = 7;
/** Days mapped to X_FAR (one year). */
const D_REF = 365;
/** Days mapped to 1.0; beyond this the axis saturates. */
const D_MAX = 365 * 5;
/** Days of overdue history the left zone resolves before it saturates. */
const D_OVER = 90;

/** Left edge of the overdue zone. */
export const X_PAST = 0.02;
/** Position of the "heute" gridline — the reader's anchor. */
export const X_TODAY = 0.14;
/** Position of the "1 Jahr" gridline. */
const X_FAR = 0.93;

const L_REF = Math.log(1 + D_REF / K);
const L_MAX = Math.log(1 + D_MAX / K);
const L_OVER = Math.log(1 + D_OVER / K);

/**
 * Maps days-from-today to a fraction of the track in [0,1]. Monotonically non-decreasing and
 * continuous at both branch boundaries (d = 0 and d = D_REF).
 *
 * The resulting layout: the next week occupies 14% of the width, the next month 33%, the next
 * quarter 52%, and everything beyond a year the last 7%.
 */
export function timelineX(d: number): number {
  if (Number.isNaN(d)) {
    return X_TODAY;
  }
  if (d <= 0) {
    // Overdue: compressed zone left of "heute", saturating at D_OVER days.
    const u = Math.min(1, Math.log(1 + -d / K) / L_OVER);
    return X_TODAY - (X_TODAY - X_PAST) * u;
  }
  if (d <= D_REF) {
    // Main band: today .. one year.
    return X_TODAY + (X_FAR - X_TODAY) * Math.log(1 + d / K) / L_REF;
  }
  if (d <= D_MAX) {
    // Tail: one .. five years. Same log, so the slope change at one year is mild.
    return X_FAR + (1 - X_FAR) * (Math.log(1 + d / K) - L_REF) / (L_MAX - L_REF);
  }
  return 1;
}

/** A gridline on the axis. `key` is the `akten.timeline.*` i18n key suffix. */
export interface TimelineTick {
  key: string;
  /** Days from today this tick marks. */
  d: number;
  /** Fraction of the track, derived from {@link timelineX} so gridline and marker never drift. */
  x: number;
  /** Container-width tier: 0 always shown, 1-3 progressively revealed on wider containers. */
  tier: number;
}

/** The gridlines, coarsest first by tier. */
export const TIMELINE_TICKS: TimelineTick[] = [
  { key: 'today', d: 0, tier: 0 },
  { key: 'week1', d: 7, tier: 1 },
  { key: 'month1', d: 30, tier: 0 },
  { key: 'month3', d: 90, tier: 2 },
  { key: 'month6', d: 182, tier: 3 },
  { key: 'year1', d: 365, tier: 0 },
].map((t) => ({ ...t, x: timelineX(t.d) }));

/** Minimum horizontal gap between two markers in the same lane, as a fraction of the track. */
const MIN_GAP = 0.018;
/**
 * Lanes available for stacking; beyond this markers may overlap (see assignLanes).
 * Kept at 3 because the strip lives in the sticky case header, where every row costs height.
 */
export const MAX_LANES = 3;
/** Vertical distance between two lanes, in px. Mirrored by the component's marker CSS. */
export const LANE_STEP = 14;

/**
 * Packs markers into stacked lanes so near-simultaneous entries stay individually visible.
 *
 * Works in fraction space, so the result is independent of the rendered width and needs no
 * measurement. `xs` must be sorted ascending. When all lanes are occupied the marker goes into
 * the lane with the largest gap — it may then overlap, which is preferred over a cluster chip:
 * every marker stays an individually clickable and focusable element.
 */
export function assignLanes(xs: number[]): number[] {
  const lastX: number[] = [];
  return xs.map((x) => {
    let lane = lastX.findIndex((lx) => x - lx >= MIN_GAP);
    if (lane === -1) {
      lane = lastX.length < MAX_LANES ? lastX.length : lastX.indexOf(Math.min(...lastX));
    }
    lastX[lane] = x;
    return lane;
  });
}
