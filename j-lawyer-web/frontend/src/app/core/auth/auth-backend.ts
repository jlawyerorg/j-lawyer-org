import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { AUTH_BASE } from '../api';
import { AuthSession, AuthUser, Credentials } from './auth.models';

/** Wire shape of the server's TokenResponseV8 (POST /v8/auth/login|refresh). */
interface TokenResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  username: string;
  roles: string[];
}

/**
 * Authentication backend talking to the real j-lawyer REST endpoints
 * (`/j-lawyer-io/rest/v8/auth/*`, see design.md Decision 5 and j-lawyer-io/AUTH-SETUP.md).
 *
 * The server returns the short-lived access token in the body (kept in memory by
 * {@link AuthService}) and the refresh token as an httpOnly cookie the browser stores and
 * replays automatically. Requests use `withCredentials` so that cookie is sent/stored even
 * if the API is ever served from a different origin.
 */
@Injectable({ providedIn: 'root' })
export class AuthBackend {
  private readonly http = inject(HttpClient);

  login(credentials: Credentials): Observable<AuthSession> {
    return this.http
      .post<TokenResponse>(`${AUTH_BASE}/login`, credentials, { withCredentials: true })
      .pipe(map((response) => toSession(response)));
  }

  /** Silent re-auth via the httpOnly refresh cookie; errors (401) when there is none. */
  refresh(): Observable<AuthSession> {
    return this.http
      .post<TokenResponse>(`${AUTH_BASE}/refresh`, null, { withCredentials: true })
      .pipe(map((response) => toSession(response)));
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${AUTH_BASE}/logout`, null, { withCredentials: true });
  }
}

function toSession(response: TokenResponse): AuthSession {
  return { token: response.accessToken, user: toUser(response.username, response.roles) };
}

function toUser(username: string, roles: string[]): AuthUser {
  return {
    username,
    displayName: username,
    initials: initialsOf(username),
    roles: roles ?? [],
  };
}

/** Up to two letters: first of each name part (jens.kutschke → JK), else first two chars. */
function initialsOf(username: string): string {
  const parts = username.split(/[.\-_@\s]+/).filter(Boolean);
  const letters =
    parts.length >= 2 ? parts[0][0] + parts[1][0] : username.slice(0, 2);
  return letters.toUpperCase();
}
