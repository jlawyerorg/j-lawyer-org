/**
 * The top-level modules shown in the module bar (and a subset in the mobile bottom-nav).
 * Mirrors the Swing client's module tree (see openspec change add-web-client). Routes
 * are generated from this list in app.routes.ts, so nav and routing stay in sync.
 */
export interface ModuleLink {
  /** Route path segment. */
  path: string;
  label: string;
  /** Icon name resolved by IconComponent. */
  icon: string;
  /** Optional unread/notification count. */
  badge?: number;
  /** Rendered at the bottom of the module bar (e.g. settings). */
  footer?: boolean;
  /** Also shown in the mobile bottom-nav. */
  mobile?: boolean;
}

export const MODULES: ModuleLink[] = [
  { path: 'desktop', label: 'Mein Desktop', icon: 'desktop', mobile: true },
  { path: 'akten', label: 'Akten', icon: 'cases', mobile: true },
  { path: 'adressen', label: 'Adressen', icon: 'contacts' },
  { path: 'kalender', label: 'Kalender & Fristen', icon: 'calendar', mobile: true },
  { path: 'kommunikation', label: 'Kommunikation', icon: 'mail', badge: 3, mobile: true },
  { path: 'dokumente', label: 'Dokumente', icon: 'doc' },
  { path: 'finanzen', label: 'Finanzen', icon: 'euro' },
  { path: 'reporting', label: 'Reporting', icon: 'chart' },
  { path: 'einstellungen', label: 'Einstellungen', icon: 'gear', footer: true },
];
