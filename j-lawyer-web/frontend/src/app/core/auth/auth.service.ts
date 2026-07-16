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

  /**
   * Whether the signed-in user holds a given server role (e.g. 'adminRole', 'sysAdminRole',
   * 'createOptionGroupRole'). Roles come from the login/refresh response. This gates the UI only —
   * the server independently enforces every write, so a missing role also yields a 403.
   */
  hasRole(role: string): boolean {
    return this.user()?.roles?.includes(role) ?? false;
  }

  /** Whether the user holds at least one of the given roles (empty list → true, i.e. no gate). */
  hasAnyRole(roles: string[]): boolean {
    if (roles.length === 0) {
      return true;
    }
    const owned = this.user()?.roles ?? [];
    return roles.some((r) => owned.includes(r));
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
