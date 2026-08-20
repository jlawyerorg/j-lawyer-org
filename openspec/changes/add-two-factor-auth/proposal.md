# Change: Add Two-Factor Authentication for the Desktop Client

## Why

The desktop client login is protected by a single factor (username + password),
validated entirely at the WildFly/Elytron layer (`jlawyer-jdbc-realm`, SHA-256) before
any application code runs. A stolen or guessed password grants full access to a firm's
case data. As a legal practice management system handling highly confidential client
data, j-lawyer.org needs a second authentication factor.

Because the client is open source and trivially modifiable, a second factor enforced
only in client UI code would be worthless. At the same time, customizing Elytron with
bespoke Java login modules is fragile across WildFly upgrades. This proposal threads
that needle: the second factor is enforced **at the protocol level** through a small
change to Elytron's `principal-query` SQL (configuration only, no custom module), while
all factor verification logic lives in **portable application code**.

## What Changes

- **TOTP enrollment** (RFC 6238) per user via QR code in the client settings, with an
  **email OTP fallback** for users who lose their authenticator.
- **Protocol-level login gate**: the Elytron `principal-query` for EJB remoting is
  extended so a user with 2FA enabled only authenticates when a fresh, short-lived
  "second factor verified" timestamp exists on their row. No fresh second factor → the
  EJB/SASL handshake itself fails. A modified client cannot bypass this.
- **Pre-authentication verification endpoint** (REST, behind HTTP Basic on the existing
  ungated realm): validates password + second factor in application code and writes the
  short-lived validity window via the database clock.
- **Two-realm split in Elytron** (configuration in `docker/wildfly/standalone.xml`): the
  EJB remoting connector uses a new *gated* realm; the REST war keeps the *ungated*
  realm. This scopes 2FA to the desktop client and keeps the REST API (mobile app,
  integrations) on unchanged HTTP Basic Auth, and lets the pre-auth endpoint bootstrap
  without a chicken-and-egg lockout.
- **Opt-in per user + admin reset**: 2FA is off by default (fully backward compatible);
  each user enables it for themselves; an administrator can reset/disable it for a user
  who has lost their device.
- **Schema migration**: add `totp_secret` (encrypted), `totp_enabled`,
  `totp_valid_until`, and email-OTP fields to `security_users`.

## Impact

- Affected specs: **two-factor-auth** (new capability)
- Affected code:
  - `docker/wildfly/standalone.xml` — gated `principal-query`, second realm + SASL factory
  - `j-lawyer-server-entities/.../persistence/AppUserBean.java` — new fields
  - `j-lawyer-server-entities/src/main/resources/db/migration/V3_6_0_5__AddTwoFactorAuth.sql`
  - `j-lawyer-server-common/.../security/` — RFC 6238 TOTP utility (self-contained), reuse of `Crypto` for secret encryption
  - `j-lawyer-server/j-lawyer-server-ejb/.../services/SecurityService.java` (+ Remote/Local) — enrollment, verification, admin reset; `EmailService` for email OTP
  - `j-lawyer-server/j-lawyer-io/.../rest/v8/` — pre-auth `2fa/verify` + email-OTP request endpoints
  - `j-lawyer-client/.../client/LoginDialog.java` (+ `.form`) — second-factor step in the login flow
  - `j-lawyer-client/.../client/` settings UI — TOTP enrollment; QR PNG generated
    server-side (reuse ZXing pattern from `InvoiceService.getGiroCode()`) and shown as an
    `ImageIcon` like `GirocodeDialog` — no new client dependency
  - `j-lawyer-client/.../configuration/UserAdministrationDialog.java` — admin reset action
- Non-breaking: users without 2FA enabled authenticate exactly as today.
