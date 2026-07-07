import { Injectable, effect, signal } from '@angular/core';

type Theme = 'light' | 'dark';

/**
 * Light/dark theme state. Stamps `data-theme` on the document root so the token
 * overrides in styles/tokens.css take effect (design.md Decision 1 / design-mockup.html).
 * Initialised from the OS preference; toggled by the user in the header.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly _theme = signal<Theme>(this.preferredTheme());
  readonly theme = this._theme.asReadonly();

  constructor() {
    effect(() => document.documentElement.setAttribute('data-theme', this._theme()));
  }

  toggle(): void {
    this._theme.update((t) => (t === 'dark' ? 'light' : 'dark'));
  }

  private preferredTheme(): Theme {
    return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  }
}
