## Context

Authentication in j-lawyer.org is performed by **WildFly Elytron**, not by application
code. The EJB remoting connector (`http-remoting-connector`, `standalone.xml:531`) uses
`jlawyer-sasl-authentication-factory` (PLAIN SASL, `:369`) bound to
`jlawyer-security-domain` → `jlawyer-jdbc-realm` (`:288`). That realm runs:

```sql
select password from security_users where principalId=?   -- SHA-256 digest-mapper
```

The desktop client passes username/password through JBoss Remoting; the
`SecurityService.login()` EJB is a stub — the container has already authenticated before
any EJB method executes. The same realm also backs the REST war
(`jlawyer-application-security-domain`) via HTTP Basic Auth.

Two hard constraints shaped the design:

1. The client is **open source and modifiable**, so a second factor checked only in
   client code is not a real control.
2. We will **not ship a custom Elytron Java module** (custom `LoginModule` /
   credential-validator), because it is brittle across WildFly upgrades/migrations.

## Goals / Non-Goals

- **Goals**
  - Enforce a second factor for the **desktop client** such that a modified client
    cannot bypass it — the enforcement point is the SASL handshake itself.
  - Keep all factor-verification logic in portable application code (no Elytron Java).
  - Backward compatible: users without 2FA enabled are unaffected.
  - TOTP (RFC 6238) as primary factor; **email OTP** as fallback/recovery.
  - Opt-in per user; admin can reset/disable for a user.
- **Non-Goals**
  - 2FA for the REST API (mobile app / integrations) — REST stays on HTTP Basic Auth
    with the ungated realm. (Revisitable later via app-specific tokens.)
  - Upgrading the password hash (still SHA-256; out of scope, tracked separately).
  - WebAuthn / hardware keys (impractical in Swing today).
  - Backup codes and admin-enforced/mandatory 2FA (deferred; see Open Questions).

## Decisions

### Decision 1: Enforce via the Elytron `principal-query` SQL ("timestamp gate")

Add a gated `principal-query` that returns the password row only when the user either
has 2FA disabled or has a fresh second-factor timestamp:

```sql
select password from security_users
where principalId = ?
  and (totp_enabled = false or totp_valid_until > NOW())
```

If 2FA is enabled but no fresh verification exists, the query returns **no row** →
credential mapping fails → the EJB/SASL handshake fails. This is enforced by the server,
independent of the client binary. The change is **pure configuration** (SQL text in
`standalone.xml`); nothing is compiled against WildFly internals.

- Alternative — custom Elytron credential-validator / SASL factory that validates
  password+OTP in one step: stronger single-step binding, but requires a custom WildFly
  module → rejected per constraint 2.
- Alternative — password+OTP concatenation into the PLAIN credential: also needs a
  custom evidence decoder, and breaks "save password" / REST Basic Auth → rejected.

### Decision 2: Two-realm split — gate EJB only, leave REST ungated

Introduce a second realm `jlawyer-jdbc-realm-2fa` (gated query above), a security-domain
`jlawyer-security-domain-2fa`, and a SASL factory bound to it; repoint the EJB
`http-remoting-connector` to that gated factory. The existing `jlawyer-jdbc-realm`
(ungated) continues to back the REST war.

This achieves three things at once:
- Scopes 2FA to the desktop client (the decided scope).
- Leaves REST/mobile/integrations untouched.
- Resolves the bootstrap chicken-and-egg: the pre-auth verification endpoint lives in
  the REST war on the **ungated** realm, so it remains reachable to write the timestamp
  that later unlocks the gated EJB login.

### Decision 3: Pre-authentication verification endpoint (REST)

New endpoint, e.g. `POST /j-lawyer-io/rest/v8/security/2fa/verify`, secured by HTTP Basic
(ungated realm, so password is validated by Elytron even when the user has 2FA enabled).
Body: `{ "code": "123456", "channel": "totp|email" }`. The endpoint:

1. Confirms the authenticated principal matches the request.
2. Verifies the code against the stored TOTP secret (or the pending email OTP).
3. On success, sets the validity window using the **database clock** to avoid app/DB
   skew: `update security_users set totp_valid_until = DATE_ADD(NOW(), INTERVAL :window SECOND) where principalId = ?`.

A companion `POST .../2fa/request-email` sends an email OTP via the existing
`EmailService` when the user chooses the fallback channel.

Login flow in the client (`LoginDialog`):
collect user + password + factor → call `2fa/verify` over HTTPS → on success perform the
normal EJB remoting login (which now passes the gate) → unlock UI. Users with
`totp_enabled = false` skip the second-factor step entirely.

### Decision 4: Self-contained TOTP, reuse `Crypto` for the secret

Implement RFC 6238 (HMAC-SHA1, 6 digits, 30s step, ±1 step skew) as a ~60-line utility in
`j-lawyer-server-common/.../security/` rather than adding a non-Central dependency (the
project resolves such deps through the awkward in-repo `maven-repo/`). The TOTP secret is
stored in `security_users.totp_secret` **encrypted** via the existing
`Crypto`/`CryptoProvider`, never in clear text.

Enrollment returns the standard `otpauth://totp/...` provisioning URI **and a ready-made
QR PNG generated server-side**, reusing the existing ZXing pattern already used for
invoice giro codes (`InvoiceService.getGiroCode()`, `j-lawyer-server-ejb`, ~line 1096:
`QRCodeWriter().encode(...)` → `MatrixToImageWriter.writeToStream(matrix, "PNG", ...)` →
`byte[]`). ZXing is already a dependency in both the EJB module and the client. The client
displays the returned PNG as an `ImageIcon` exactly like `GirocodeDialog`
(`GirocodeDialog.java:718-720`) — no new dependency and no client-side QR code. The
Base32 secret + URI are also shown for manual entry as a fallback.

### Decision 5: Validity window length

Default window: **60 seconds** — long enough to complete the EJB handshake, short enough
to minimize the race window (Risk 1). Configurable server-side.

## Risks / Trade-offs

- **TOCTOU race window** → Between writing `totp_valid_until` and the EJB handshake there
  is a window in which an attacker who *already knows the password* and is actively
  racing for that specific user could establish a session without the second factor.
  Mitigation: keep the window short (≈60s); the timestamp is per-user and only useful in
  that narrow interval. Documented as accepted residual risk for an interactive LOB app.
- **Session model & re-authentication** → `JLawyerServiceLocator` is an app-lifetime
  singleton holding ONE `InitialContext`/Remoting connection
  (`getInstance()`, `JLawyerServiceLocator.java:923`); SASL PLAIN runs ONCE at connection
  establishment, and all subsequent EJB calls reuse the authenticated connection — there
  is no per-request authentication. Therefore the gate is evaluated only at (re)connect
  time, and `totp_valid_until` expiring mid-session does **not** log the user out or break
  in-flight calls. Only two paths re-authenticate after login:
  1. **Admin privilege escalation** via `getTemporaryInstanceFor(principal, password, ...)`
     (`JKanzleiGUI.java:3072`) builds a NEW connection with re-entered credentials. For a
     2FA-enabled user this would hit the gate and fail unless it is also routed through
     the second-factor verification first. → Must be handled (see tasks).
  2. **Connection drop + transparent reconnect** (network blip, WildFly idle timeout). The
     client has no app-level reconnect logic today, so a dropped connection already breaks
     the session; with the gate, a silent reconnect for a 2FA user would fail and require a
     fresh login (incl. second factor). Mitigation: keep the WildFly Remoting connection
     idle timeout generous so routine idling does not drop the connection, and surface a
     failed reconnect as a clear "please log in again" prompt rather than a raw error.
- **Clock dependency** → The gate uses DB `NOW()`; the window must be written with the
  same DB clock (Decision 3) so app/DB skew cannot cause spurious failures or extensions.
- **Email OTP strength** → Weaker than TOTP (mailbox as attack surface) and depends on
  outbound mail working; positioned as fallback/recovery, not primary.
- **standalone.xml drift** → Only `docker/wildfly/standalone.xml` is in the repo; real
  deployments may use their own copy. The realm/SASL changes must be documented as a
  required server-config update (and included in setup/migration notes).

## Migration Plan

1. Ship `V3_6_0_5__AddTwoFactorAuth.sql`: add `totp_secret VARCHAR`, `totp_enabled
   BOOLEAN NOT NULL DEFAULT false`, `totp_valid_until DATETIME NULL`, and email-OTP
   columns to `security_users`. Defaults make every existing user pass the gate
   unchanged.
2. Update `docker/wildfly/standalone.xml` (gated realm + second security-domain + SASL
   factory; repoint EJB connector). Document the equivalent change for existing servers.
3. Deploy server (entities, EJB, REST). No user is affected until they opt in.
4. Roll out client with enrollment UI + login second-factor step.
- **Rollback**: revert the EJB connector to the ungated SASL factory; the extra columns
  are inert. Users keep logging in with password only.

## Open Questions

- Future: admin-enforced/mandatory 2FA per role and one-time backup codes — schema is
  designed to accommodate both without further migration, but they are out of scope here.
