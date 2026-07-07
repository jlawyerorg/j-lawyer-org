import { Injectable } from '@angular/core';
import { delay, Observable, of, throwError } from 'rxjs';
import { AuthSession, AuthUser, Credentials } from './auth.models';

/**
 * Authentication backend.
 *
 * TEMPORARY MOCK (design.md Decision 5). Swap for real REST here without touching the
 * AuthService/components: replace each method with an HttpClient call to
 * POST /j-lawyer-io/rest/v{n}/auth/{login,refresh,logout}. The server issues the access
 * token and an httpOnly refresh cookie; here `sessionStorage` stands in for that cookie
 * so a page reload can "refresh" the session (the real refresh cookie is not JS-readable).
 *
 * Demo credentials: admin / a  (the docker default).
 */
@Injectable({ providedIn: 'root' })
export class AuthBackend {
  private static readonly REFRESH_MARKER = 'jl.mock.refresh';

  login(c: Credentials): Observable<AuthSession> {
    const ok = c.username.trim().toLowerCase() === 'admin' && c.password === 'a';
    if (!ok) {
      return throwError(() => new Error('invalid-credentials')).pipe(delay(300));
    }
    sessionStorage.setItem(AuthBackend.REFRESH_MARKER, c.username.trim());
    return of(this.session(c.username.trim())).pipe(delay(300));
  }

  /** Silent re-auth via the (mock) refresh cookie; errors if none. */
  refresh(): Observable<AuthSession> {
    const username = sessionStorage.getItem(AuthBackend.REFRESH_MARKER);
    return username
      ? of(this.session(username)).pipe(delay(150))
      : throwError(() => new Error('no-session'));
  }

  logout(): Observable<void> {
    sessionStorage.removeItem(AuthBackend.REFRESH_MARKER);
    return of(void 0);
  }

  private session(username: string): AuthSession {
    const user: AuthUser = {
      username,
      displayName: username === 'admin' ? 'Dr. Kunze' : username,
      initials: (username === 'admin' ? 'DK' : username.slice(0, 2)).toUpperCase(),
      roles: ['loginRole'],
    };
    return { token: `mock.${username}.${crypto.randomUUID()}`, user };
  }
}
