## ADDED Requirements

### Requirement: TOTP Enrollment

The system SHALL allow each user to enroll a time-based one-time password (TOTP, RFC 6238)
second factor from the desktop client. Enrollment SHALL generate a per-user secret,
present it as a standard `otpauth://totp/...` provisioning URI (and/or QR code), store the
secret encrypted at rest, and activate the second factor only after the user confirms a
first valid code. 2FA SHALL be opt-in and disabled by default for every user.

#### Scenario: User enrolls TOTP successfully

- **WHEN** a user starts TOTP enrollment and enters a code that matches the newly
  generated secret
- **THEN** the secret is stored encrypted, `totp_enabled` is set to true for that user,
  and the second factor is required at the next login

#### Scenario: Enrollment rejected on wrong confirmation code

- **WHEN** a user starts TOTP enrollment but enters a code that does not match the
  generated secret
- **THEN** `totp_enabled` remains false and the secret is not activated

#### Scenario: Secret never stored in clear text

- **WHEN** a TOTP secret is persisted to `security_users.totp_secret`
- **THEN** it is encrypted via the existing Crypto provider and is never written in clear
  text

### Requirement: Protocol-Level Second-Factor Login Gate

For the desktop client (EJB remoting), the system SHALL enforce the second factor at the
authentication layer so that it cannot be bypassed by a modified client. When a user has
2FA enabled, the EJB/SASL authentication SHALL succeed only while a fresh second-factor
verification window is present; otherwise the handshake SHALL fail. Users with 2FA
disabled SHALL authenticate unchanged.

#### Scenario: 2FA-enabled user without fresh verification is rejected

- **WHEN** a user with `totp_enabled = true` attempts the EJB remoting login and no fresh
  `totp_valid_until` window exists
- **THEN** the Elytron credential query returns no row and the login handshake fails

#### Scenario: 2FA-enabled user with fresh verification succeeds

- **WHEN** a user with `totp_enabled = true` has just completed second-factor
  verification and `totp_valid_until` is in the future
- **THEN** the EJB remoting login succeeds and the client session is established

#### Scenario: Modified client cannot bypass the gate

- **WHEN** a client is altered to skip the second-factor step and attempts the EJB login
  for a 2FA-enabled user with only username and password
- **THEN** the server-side gate causes the handshake to fail because no fresh window was
  written

#### Scenario: Non-2FA user unaffected

- **WHEN** a user with `totp_enabled = false` logs in with username and password
- **THEN** authentication behaves exactly as before this change

### Requirement: Pre-Authentication Verification Endpoint

The system SHALL provide a REST endpoint, reachable on the ungated (HTTP Basic) realm,
that verifies a user's password and second factor and writes a short-lived verification
window using the database clock. The endpoint SHALL only write the window when both the
password (validated by the container) and the supplied second-factor code are valid for
the authenticated principal.

#### Scenario: Valid TOTP opens the window

- **WHEN** the endpoint is called with valid HTTP Basic credentials and a valid TOTP code
- **THEN** `totp_valid_until` is set to a near-future time computed from the database
  clock and a success response is returned

#### Scenario: Invalid second factor does not open the window

- **WHEN** the endpoint is called with valid HTTP Basic credentials but an invalid code
- **THEN** `totp_valid_until` is not updated and an authentication error is returned

#### Scenario: Window scoped to the authenticated user

- **WHEN** the endpoint is called for a principal that differs from the authenticated
  HTTP Basic identity
- **THEN** the request is rejected and no window is written

### Requirement: Email OTP Fallback

The system SHALL offer an email one-time password as a fallback second factor for users
who cannot use their authenticator. On request, the system SHALL generate a short-lived
numeric code, deliver it to the user via the existing email service, and accept it at the
verification endpoint as an alternative to TOTP.

#### Scenario: User requests and uses an email OTP

- **WHEN** a 2FA-enabled user requests an email OTP and then submits the received code at
  the verification endpoint within its validity period
- **THEN** the verification succeeds and the login window is opened

#### Scenario: Expired email OTP is rejected

- **WHEN** a user submits an email OTP after it has expired
- **THEN** verification fails and no login window is opened

### Requirement: Administrative Reset

The system SHALL allow an administrator to disable/reset 2FA for a user who has lost
access to their second factor, clearing the stored secret and setting the user back to a
non-2FA state.

#### Scenario: Admin resets a locked-out user

- **WHEN** an administrator resets 2FA for a user
- **THEN** `totp_enabled` is set to false, the stored secret is cleared, and the user can
  log in with username and password until they re-enroll

#### Scenario: Reset restricted to administrators

- **WHEN** a non-administrator attempts to reset another user's 2FA
- **THEN** the operation is denied
