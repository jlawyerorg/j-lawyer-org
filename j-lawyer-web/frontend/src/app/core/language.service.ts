import { inject, Injectable, signal } from '@angular/core';
import { TranslocoService } from '@jsverse/transloco';
import { APP_LANGUAGES, AppLanguage, DEFAULT_LANGUAGE, LANGUAGE_STORAGE_KEY } from './i18n';

/**
 * UI language state. Wraps TranslocoService with the app's language list, persistence
 * (localStorage) and initial-language resolution (stored > browser > default).
 * Instantiated at startup (injected by the header) so the active language is set
 * before the first render.
 */
@Injectable({ providedIn: 'root' })
export class LanguageService {
  private readonly transloco = inject(TranslocoService);
  readonly languages: AppLanguage[] = APP_LANGUAGES;

  private readonly _current = signal<string>(DEFAULT_LANGUAGE);
  readonly current = this._current.asReadonly();

  constructor() {
    this.use(this.resolveInitial());
  }

  use(code: string): void {
    this.transloco.setActiveLang(code);
    this._current.set(code);
    localStorage.setItem(LANGUAGE_STORAGE_KEY, code);
  }

  private resolveInitial(): string {
    const stored = localStorage.getItem(LANGUAGE_STORAGE_KEY);
    if (stored && this.isSupported(stored)) {
      return stored;
    }
    const browser = navigator.language?.slice(0, 2);
    return browser && this.isSupported(browser) ? browser : DEFAULT_LANGUAGE;
  }

  private isSupported(code: string): boolean {
    return this.languages.some((l) => l.code === code);
  }
}
