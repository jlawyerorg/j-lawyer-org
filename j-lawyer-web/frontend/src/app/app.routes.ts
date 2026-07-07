import { Routes } from '@angular/router';
import { MODULES } from './shell/modules';

/**
 * Routes are derived from the module list so navigation and routing stay in sync.
 * Every module currently resolves to a placeholder; real module components replace
 * these entries per migration phase (OpenSpec change add-web-client, tasks.md).
 */
const moduleRoutes: Routes = MODULES.map((m) => ({
  path: m.path,
  loadComponent: () =>
    import('./placeholder/module-placeholder.component').then(
      (c) => c.ModulePlaceholderComponent,
    ),
  data: { label: m.label },
  title: `${m.label} – j-lawyer`,
}));

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'akten' },
  ...moduleRoutes,
  { path: '**', redirectTo: 'akten' },
];
