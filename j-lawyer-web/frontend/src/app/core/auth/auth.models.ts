/**
 * Auth domain models. Shapes mirror the REST auth contract (design.md Decision 5,
 * `TokenResponseV8`): login exchanges credentials for a short-lived access token; the
 * user's identity/roles come with it. `otp` carries the optional second factor (2FA).
 */
export interface Credentials {
  username: string;
  password: string;
  /** Optional TOTP second factor (enforced server-side later; see add-two-factor-auth). */
  otp?: string;
}

export interface AuthUser {
  username: string;
  displayName: string;
  initials: string;
  roles: string[];
}

export interface AuthSession {
  /** Short-lived access token — kept in memory only (design.md Decision 5). */
  token: string;
  user: AuthUser;
}
