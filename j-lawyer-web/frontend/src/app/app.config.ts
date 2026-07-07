import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withFetch } from '@angular/common/http';

import { routes } from './app.routes';

/**
 * Root application providers. Kept minimal and explicit (readability for reviewers —
 * see design.md, "AI-generierter Code / Lesbarkeit"). HttpClient talks only to the
 * same-origin REST API (design.md Decision 2c).
 */
export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withFetch()),
  ],
};
