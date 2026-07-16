import { Route, Routes } from '@angular/router';
import { MODULES } from './shell/modules';
import { authGuard } from './core/auth/auth.guard';
import { roleGuard } from './core/auth/role.guard';

type LoadComponent = NonNullable<Route['loadComponent']>;

/**
 * Routes are derived from the module list so navigation and routing stay in sync.
 * Every module currently resolves to a placeholder; real module components replace
 * these entries per migration phase (OpenSpec change add-web-client, tasks.md).
 * The module's i18n key is passed via route data for the placeholder heading.
 */
const placeholderLoader: LoadComponent = () =>
  import('./placeholder/module-placeholder.component').then((c) => c.ModulePlaceholderComponent);

/** Modules with a real implemented component (keyed by route path; others fall back to the placeholder). */
const IMPLEMENTED: Record<string, LoadComponent> = {
  desktop: () => import('./desktop/desktop.component').then((c) => c.DesktopComponent),
  cases: () => import('./akten/akten.component').then((c) => c.AktenComponent),
  contacts: () => import('./contacts/kontakte.component').then((c) => c.KontakteComponent),
  calendar: () => import('./calendar/kalender.component').then((c) => c.KalenderComponent),
  communication: () => import('./communication/email.component').then((c) => c.EmailComponent),
  bea: () => import('./bea/bea.component').then((c) => c.BeaComponent),
  documents: () => import('./documents/dokumente.component').then((c) => c.DokumenteComponent),
  reporting: () => import('./reporting/reporting.component').then((c) => c.ReportingComponent),
};

// 'settings' is not a flat placeholder route — it is a nested layout (tabs) with role-scoped
// child screens (see settingsRoute below), so it is excluded from the generated module routes.
const moduleRoutes: Routes = MODULES.filter((m) => m.path !== 'settings').map((m) => ({
  path: m.path,
  loadComponent: IMPLEMENTED[m.path] ?? placeholderLoader,
  data: { labelKey: m.labelKey },
  title: 'j-lawyer',
}));

/**
 * The consolidated Settings area: a tab layout with three role-scoped screens. Auth is enforced by
 * the shell's canActivateChild; the admin/system child routes additionally require a role via
 * roleGuard (`data.requiredRoles`). The default child is the always-accessible general screen.
 */
const settingsRoute: Route = {
  path: 'settings',
  loadComponent: () => import('./settings/settings.component').then((c) => c.SettingsComponent),
  data: { labelKey: 'module.einstellungen' },
  title: 'j-lawyer',
  children: [
    { path: '', pathMatch: 'full', redirectTo: 'general' },
    {
      path: 'general',
      loadComponent: () => import('./settings/general-settings.component').then((c) => c.GeneralSettingsComponent),
    },
    {
      path: 'administration',
      canActivate: [roleGuard],
      data: { requiredRoles: ['adminRole'] },
      loadComponent: () => import('./settings/administration-settings.component').then((c) => c.AdministrationSettingsComponent),
    },
    {
      path: 'system',
      canActivate: [roleGuard],
      data: { requiredRoles: ['sysAdminRole'] },
      loadComponent: () => import('./settings/system-settings.component').then((c) => c.SystemSettingsComponent),
    },
  ],
};

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./auth/login.component').then((c) => c.LoginComponent),
    title: 'j-lawyer',
  },
  {
    // Shell layout route: all module routes are children, guarded so unauthenticated
    // users are redirected to /login (auth.guard). Login lives outside this subtree.
    path: '',
    loadComponent: () => import('./shell/shell.component').then((c) => c.ShellComponent),
    canActivateChild: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'desktop' },
      // Deep link to a single case (shareable/bookmarkable); reuses the Akten component,
      // which reads the :id param to select the case. Must precede the module routes.
      {
        path: 'cases/:id',
        loadComponent: () => import('./akten/akten.component').then((c) => c.AktenComponent),
        data: { labelKey: 'module.akten' },
        title: 'j-lawyer',
      },
      // Deep link to a single contact (used by pinned shortcuts); reuses the contacts component,
      // which reads the :id param to select the contact. Must precede the module routes.
      {
        path: 'contacts/:id',
        loadComponent: () => import('./contacts/kontakte.component').then((c) => c.KontakteComponent),
        data: { labelKey: 'module.kontakte' },
        title: 'j-lawyer',
      },
      settingsRoute,
      ...moduleRoutes,
      { path: '**', redirectTo: 'desktop' },
    ],
  },
];
