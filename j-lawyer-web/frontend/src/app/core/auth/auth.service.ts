import { computed, inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, Observable, of, tap } from 'rxjs';
import { AuthBackend } from './auth-backend';
import { AuthSession, Credentials } from './auth.models';

/**
 * Authentication state. Holds the session (access token in memory only — design.md
 * Decision 5) as a signal; components read `user()`/`isAuthenticated()`. Delegates the
 * actual credential exchange to AuthBackend (the real `/v8/auth/*` REST endpoints).
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly backend = inject(AuthBackend);
  private readonly router = inject(Router);

  private readonly _session = signal<AuthSession | null>(null);
  readonly user = computed(() => this._session()?.user ?? null);
  readonly isAuthenticated = computed(() => this._session() !== null);

  /** Access token for the Authorization header; null when signed out. */
  get token(): string | null {
    return this._session()?.token ?? null;
  }

  login(credentials: Credentials): Observable<AuthSession> {
    return this.backend.login(credentials).pipe(tap((s) => this._session.set(s)));
  }

  logout(): void {
    this.backend.logout().subscribe();
    this._session.set(null);
    void this.router.navigateByUrl('/login');
  }

  /**
   * Attempt silent session restore via the refresh cookie (run at app startup, before
   * routing — see app.config). Never throws: resolves to null when there is no session.
   */
  restore(): Observable<AuthSession | null> {
    return this.backend.refresh().pipe(
      tap((s) => this._session.set(s)),
      catchError(() => of(null)),
    );
  }
}
