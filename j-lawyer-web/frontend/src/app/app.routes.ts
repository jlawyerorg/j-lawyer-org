import { Routes } from '@angular/router';

/**
 * Application routes. Scaffold placeholder — module routes (Akten, Kontakte, Kalender …)
 * are added per migration phase (see openspec change add-web-client, tasks.md).
 */
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'akten' },
  {
    path: 'akten',
    loadComponent: () =>
      import('./home/home.component').then((m) => m.HomeComponent),
    title: 'Akten – j-lawyer',
  },
  { path: '**', redirectTo: 'akten' },
];
