import { Routes } from '@angular/router';
import { MODULES } from './shell/modules';

/**
 * Routes are derived from the module list so navigation and routing stay in sync.
 * Every module currently resolves to a placeholder; real module components replace
 * these entries per migration phase (OpenSpec change add-web-client, tasks.md).
 * The module's i18n key is passed via route data for the placeholder heading.
 */
const moduleRoutes: Routes = MODULES.map((m) => ({
  path: m.path,
  loadComponent: () =>
    import('./placeholder/module-placeholder.component').then(
      (c) => c.ModulePlaceholderComponent,
    ),
  data: { labelKey: m.labelKey },
  title: 'j-lawyer',
}));

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'akten' },
  ...moduleRoutes,
  { path: '**', redirectTo: 'akten' },
];
