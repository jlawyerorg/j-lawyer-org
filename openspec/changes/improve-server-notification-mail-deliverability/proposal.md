# Change: Improve deliverability of server-originated notification mails

## Why

Notification mails sent by the server through the system-wide SMTP account (e.g. "Du wurdest in einer Sofortnachricht erwähnt", `SystemMonitorTask` status mails, `IterativeBackupTask` reports) are regularly classified as spam or outright rejected by receiving gateways (Microsoft 365, web.de/GMX, T-Online, Gmail). Root causes in the server-side send code path (`OutgoingMailProcessor.sendMail`, `SystemManagement.statusMail`, `SystemMonitorTask.statusMail`): the `Message-ID` does not match the `From:` domain (JavaMail auto-generates one with the local hostname), the body is HTML-only without a `text/plain` alternative, `Subject` and body have no explicit UTF-8 charset, the logo is embedded as a `data:` URI, and the headers `Reply-To`, `Auto-Submitted`, `X-Mailer` are missing.

The user-mailbox sending path (`EmailService.smtpSendMail`) is unaffected — it already does the right things.

## What Changes

- **New shared helper** `ServerMailUtils.prepareNotificationMessage(...)` in `j-lawyer-server-common`, used by all three server-side send call sites so the same headers, MIME structure, and Message-ID logic apply everywhere.
- **Proper Message-ID**: generated explicitly with the From-address domain on the right-hand side (`<UUID.timestamp@from-domain>`) via subclassing `MimeMessage.updateMessageID()` so JavaMail's auto-generated default cannot win. The From-address is the value of `jlawyer.server.monitor.smtpuser` (current behavior); if that value contains no `@`, the right-hand side falls back to the server's local hostname and a WARN is logged.
- **Explicit UTF-8** on `Subject:` and on every body part. Subjects with German umlauts ("erwähnt") become RFC 2047 encoded-words instead of being mangled by the JVM default encoding.
- **`multipart/alternative` with `text/plain` + `text/html`** instead of today's `multipart/mixed[text/html]`. For mention notifications the plaintext alternative is derived from the raw (pre-escape) `mainCaption`/`subCaption`/`bodyContent`. For status mails the body is already plain text and is sent as `text/plain; charset=UTF-8` without HTML wrapping.
- **Logo via `cid:` inline part** instead of base64 `data:` URI in the template. The PNG ships as a classpath resource `/templates/email/logo.png`; if it is missing the related-wrap is silently dropped.
- **Additional headers** on all server-originated notification mails:
  - `Reply-To:` — set to the From address (i.e. `smtpuser`).
  - `Auto-Submitted: auto-generated` (RFC 3834) — marks transactional automated mail and suppresses out-of-office bounces.
  - `X-Mailer: j-lawyer.org Server` — identifies the mailer.

Explicitly **out of scope**:
- `List-Unsubscribe` / `List-Unsubscribe-Post` headers.
- A separate configurable From-address decoupled from the SMTP authentication user — tracked in the optional follow-up change `add-configurable-notification-from-address` (which can be applied later without dependencies on operator UI work right now).
- SPF/DKIM/DMARC DNS configuration on the operator side.
- Changes to per-user mailbox sending in `EmailService.smtpSendMail`.

## Impact

- **Affected specs:** new capability `server-notification-mail` (added).
- **Affected code:**
  - `j-lawyer-server-common/src/com/jdimension/jlawyer/server/utils/ServerMailUtils.java` (new)
  - `j-lawyer-server/j-lawyer-server-ejb/src/java/org/jlawyer/async/OutgoingMailProcessor.java` (refactor `sendMail`)
  - `j-lawyer-server/j-lawyer-server-ejb/src/java/com/jdimension/jlawyer/services/SystemManagement.java` (refactor `statusMail`, light update to `testSendMail`)
  - `j-lawyer-server/j-lawyer-server-war/src/java/com/jdimension/jlawyer/timer/SystemMonitorTask.java` (refactor private `statusMail` at line 1039)
  - `j-lawyer-server/j-lawyer-server-ejb/src/java/templates/email/template.html` (remove `data:` URI, reference `cid:logo`)
  - `j-lawyer-server/j-lawyer-server-ejb/src/java/templates/email/logo.png` (new resource)
- **No client / UI changes in this change.** The existing settings dialog continues to work unchanged.
- **User-visible behavior:** notification mails are more likely to land in the inbox of receiving gateways because the Message-ID is well-formed and the message carries a plaintext alternative, proper charset declarations, and standard transactional headers. Maximum gain still requires the operator to ensure that the `smtpuser`-domain has SPF/DKIM/DMARC records; the optional follow-up change `add-configurable-notification-from-address` makes that easier by allowing a separate From-address.
- **No DB schema changes**, no REST API changes, no breaking changes for existing operators.
