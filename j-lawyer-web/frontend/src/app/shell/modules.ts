/**
 * The top-level modules shown in the module bar (and a subset in the mobile bottom-nav).
 * Mirrors the Swing client's module tree (see openspec change add-web-client). Routes
 * are generated from this list in app.routes.ts, so nav and routing stay in sync.
 * Labels are i18n keys resolved via Transloco (see public/i18n/*.json).
 */
export interface ModuleLink {
  /** Route path segment (always English, e.g. 'cases'). */
  path: string;
  /** Transloco key for the display label (e.g. 'module.akten'). */
  labelKey: string;
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
  { path: 'desktop', labelKey: 'module.desktop', icon: 'desktop', mobile: true },
  { path: 'cases', labelKey: 'module.akten', icon: 'cases', mobile: true },
  { path: 'contacts', labelKey: 'module.adressen', icon: 'contacts' },
  { path: 'calendar', labelKey: 'module.kalender', icon: 'calendar', mobile: true },
  { path: 'communication', labelKey: 'module.kommunikation', icon: 'mail', mobile: true },
  { path: 'bea', labelKey: 'module.bea', icon: 'shield' },
  { path: 'documents', labelKey: 'module.dokumente', icon: 'doc' },
  { path: 'finance', labelKey: 'module.finanzen', icon: 'euro' },
  { path: 'reporting', labelKey: 'module.reporting', icon: 'chart' },
  { path: 'settings', labelKey: 'module.einstellungen', icon: 'gear', footer: true },
];
