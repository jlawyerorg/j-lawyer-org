import {
  ApplicationConfig,
  inject,
  isDevMode,
  LOCALE_ID,
  provideAppInitializer,
  provideZoneChangeDetection,
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { provideTransloco } from '@jsverse/transloco';
import { firstValueFrom } from 'rxjs';

import { routes } from './app.routes';
import { TranslocoHttpLoader } from './core/transloco-loader';
import { APP_LANGUAGES, DEFAULT_LANGUAGE } from './core/i18n';
import { authInterceptor } from './core/auth/auth.interceptor';
import { AuthService } from './core/auth/auth.service';

/**
 * Root application providers. Kept minimal and explicit (readability for reviewers —
 * see design.md). HttpClient and Transloco talk only to the same-origin app
 * (design.md Decision 2c). Runtime i18n via Transloco (design.md Decision 4).
 */
export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    { provide: LOCALE_ID, useValue: 'de' },
    provideRouter(routes),
    provideHttpClient(withFetch(), withInterceptors([authInterceptor])),
    // Attempt a silent session restore (via the refresh cookie) before the first route
    // resolves, so a page reload keeps the user signed in instead of bouncing to /login.
    provideAppInitializer(() => firstValueFrom(inject(AuthService).restore())),
    provideTransloco({
      config: {
        availableLangs: APP_LANGUAGES.map((l) => l.code),
        defaultLang: DEFAULT_LANGUAGE,
        fallbackLang: DEFAULT_LANGUAGE,
        reRenderOnLangChange: true,
        prodMode: !isDevMode(),
      },
      loader: TranslocoHttpLoader,
    }),
  ],
};
