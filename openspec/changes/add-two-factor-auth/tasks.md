## 1. Data model & migration

- [ ] 1.1 Add fields to `AppUserBean` (`security_users`): `totpSecret` (encrypted String), `totpEnabled` (boolean, default false), `totpValidUntil` (Date/DATETIME, nullable), `emailOtpHash` (String, nullable), `emailOtpValidUntil` (DATETIME, nullable)
- [ ] 1.2 Create `V3_6_0_5__AddTwoFactorAuth.sql` adding the columns with backward-compatible defaults (`totp_enabled` NOT NULL DEFAULT false)
- [ ] 1.3 Verify Hibernate `validate` strategy passes against the new schema

## 2. Server: TOTP & crypto utilities

- [ ] 2.1 Add self-contained RFC 6238 TOTP utility in `j-lawyer-server-common/.../security/` (HMAC-SHA1, 6 digits, 30s step, ±1 step skew, Base32 secret, `otpauth://` URI builder)
- [ ] 2.2 Add a TOTP unit test (`*Test.java`) covering known RFC test vectors and skew window
- [ ] 2.3 Encrypt/decrypt the TOTP secret using the existing `Crypto`/`CryptoProvider`
- [ ] 2.4 Generate the enrollment QR PNG server-side from the `otpauth://` URI, reusing the ZXing pattern from `InvoiceService.getGiroCode()` (`QRCodeWriter` → `MatrixToImageWriter`); return `byte[]`

## 3. Server: SecurityService enrollment, verification, reset

- [ ] 3.1 Add enrollment methods to `SecurityServiceLocal`/`SecurityServiceRemote` (generate secret + provisioning URI; confirm-and-activate with a first valid code) — JavaDoc on the Remote interface (English)
- [ ] 3.2 Add second-factor verification logic (TOTP and email OTP) and a method to write `totp_valid_until` via a database-clock UPDATE (`DATE_ADD(NOW(), INTERVAL :window SECOND)`)
- [ ] 3.3 Add email-OTP generation + delivery through the existing `EmailService`; store only a hash + expiry
- [ ] 3.4 Add admin reset method (`@RolesAllowed` admin) clearing secret and setting `totp_enabled=false`; JavaDoc on Remote
- [ ] 3.5 Make the validity-window length a server-side configurable value (default 60s)

## 4. Server: REST pre-auth endpoints (v8)

- [ ] 4.1 Add `POST /rest/v8/security/2fa/verify` (HTTP Basic on ungated realm): verify code, confirm principal matches, write the window
- [ ] 4.2 Add `POST /rest/v8/security/2fa/request-email`: generate + send an email OTP
- [ ] 4.3 Ensure these endpoints are reachable on the ungated realm (no chicken-and-egg lockout) and reject principal mismatch

## 5. WildFly / Elytron configuration

- [ ] 5.1 In `docker/wildfly/standalone.xml`, add gated realm `jlawyer-jdbc-realm-2fa` with `principal-query`: `select password from security_users where principalId=? and (totp_enabled=false or totp_valid_until>NOW())`
- [ ] 5.2 Add `jlawyer-security-domain-2fa` + a SASL authentication factory bound to it; repoint the EJB `http-remoting-connector` to the gated SASL factory
- [ ] 5.3 Confirm the REST war keeps the original ungated `jlawyer-security-domain`
- [ ] 5.4 Document the equivalent server-config change for existing (non-docker) deployments in the setup notes

## 6. Client: enrollment UI

- [ ] 6.1 Add a 2FA section in user settings: trigger enrollment, display the server-generated QR PNG as an `ImageIcon` (pattern from `GirocodeDialog.java:718-720`) plus the otpauth URI + Base32 secret for manual entry, confirm first code to activate
- [ ] 6.2 Allow a user to disable their own 2FA (re-enrollment path)

## 7. Client: login flow

- [ ] 7.1 In `LoginDialog`, after collecting credentials, detect 2FA and prompt for the second factor (TOTP code, with "send email code" fallback action)
- [ ] 7.2 Call the `2fa/verify` (and optionally `request-email`) REST endpoint over HTTPS before the EJB remoting login
- [ ] 7.3 Update `LoginDialog.form` consistently for NetBeans GUI Builder
- [ ] 7.4 Handle failure/expiry UX (clear messaging when the window expired or the code was wrong)

## 8. Client: admin reset

- [ ] 8.1 Add a "Reset 2FA" action for a selected user in `UserAdministrationDialog` calling the admin reset method
- [ ] 8.2 Update `UserAdministrationDialog.form` consistently

## 9. Client: re-authentication paths

- [ ] 9.1 Route admin privilege escalation (`getTemporaryInstanceFor`, `JKanzleiGUI.java:3072`) through the second-factor verification when the escalating user has 2FA enabled, so the new connection passes the gate
- [ ] 9.2 Detect a failed (re)connect for a 2FA user and surface a clear "please log in again (incl. second factor)" prompt instead of a raw error
- [ ] 9.3 Confirm/raise the WildFly Remoting connection idle timeout so routine idling does not silently drop the authenticated connection

## 10. Validation & docs

- [ ] 10.1 Manual end-to-end test: enroll → logout → login requires factor → wrong/expired code fails → correct code succeeds → admin reset → login without factor
- [ ] 10.2 Verify non-2FA users are completely unaffected
- [ ] 10.3 Verify token expiry mid-session does NOT disrupt an active connection
- [ ] 10.4 Document the TOCTOU window, the re-authentication paths, and the required server-config change in user/admin docs
