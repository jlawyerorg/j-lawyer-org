import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Translation, TranslocoLoader } from '@jsverse/transloco';

/**
 * Cache-busting token, fixed once per page load. The translation JSONs are fetched at a stable URL
 * (not content-hashed like the JS bundle), so without this a browser can serve a stale copy after a
 * redeploy — new UI strings then show up as raw keys. A per-load token forces a fresh fetch on each
 * full load while still allowing the file to be reused within the same session.
 */
const CACHE_BUST = Date.now();

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
    return this.http.get<Translation>(`i18n/${lang}.json?v=${CACHE_BUST}`);
  }
}
