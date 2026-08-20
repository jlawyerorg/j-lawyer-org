/**
 * Central i18n configuration. **To add a language:** add an entry here AND drop a
 * matching translation file at `public/i18n/<code>.json`. Everything else (available
 * langs, the header switcher) is derived from this list — no other code changes needed.
 */
export interface AppLanguage {
  /** ISO 639-1 code, also the translation file name (e.g. 'de' -> i18n/de.json). */
  code: string;
  /** Native display name shown in the language switcher. */
  label: string;
}

export const APP_LANGUAGES: AppLanguage[] = [
  { code: 'de', label: 'Deutsch' },
  { code: 'en', label: 'English' },
];

export const DEFAULT_LANGUAGE = 'de';

/** localStorage key for the user's chosen language. */
export const LANGUAGE_STORAGE_KEY = 'jl.lang';
