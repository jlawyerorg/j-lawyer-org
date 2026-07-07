import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Translation, TranslocoLoader } from '@jsverse/transloco';

/**
 * Loads translation files from the app's own origin (same-origin, no CDN — CSP-safe,
 * design.md Decision 2c). The relative path resolves against <base href> so it works
 * under the /j-lawyer-web context path: -> /j-lawyer-web/i18n/<lang>.json.
 * Files live in public/i18n/ and are packaged into the WAR as static assets.
 */
@Injectable({ providedIn: 'root' })
export class TranslocoHttpLoader implements TranslocoLoader {
  private readonly http = inject(HttpClient);

  getTranslation(lang: string) {
    return this.http.get<Translation>(`i18n/${lang}.json`);
  }
}
