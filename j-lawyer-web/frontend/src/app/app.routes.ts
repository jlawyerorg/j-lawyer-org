import { Route, Routes } from '@angular/router';
import { MODULES } from './shell/modules';
import { authGuard } from './core/auth/auth.guard';

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
  cases: () => import('./akten/akten.component').then((c) => c.AktenComponent),
  contacts: () => import('./contacts/kontakte.component').then((c) => c.KontakteComponent),
};

const moduleRoutes: Routes = MODULES.map((m) => ({
  path: m.path,
  loadComponent: IMPLEMENTED[m.path] ?? placeholderLoader,
  data: { labelKey: m.labelKey },
  title: 'j-lawyer',
}));

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
      { path: '', pathMatch: 'full', redirectTo: 'cases' },
      ...moduleRoutes,
      { path: '**', redirectTo: 'cases' },
    ],
  },
];
