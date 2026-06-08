## ADDED Requirements

### Requirement: Notification Mails Carry a Properly Formed Message-ID

Every server-originated notification mail SHALL include a `Message-ID` header whose right-hand side is the domain of the `From:` address. JavaMail's default auto-generated `Message-ID` (of the form `JavaMail.<user>@<hostname>`) SHALL NOT be used.

#### Scenario: Message-ID domain matches From-domain

- **GIVEN** the From-address (currently `jlawyer.server.monitor.smtpuser`) is `notifications@kanzlei.example`
- **WHEN** the server sends a mention notification
- **THEN** the mail's `Message-ID` header SHALL have the form `<UUID.timestamp@kanzlei.example>`

#### Scenario: Fallback when From-address has no domain

- **GIVEN** the From-address contains no `@` (e.g. `smtpuser` is `user1234`)
- **WHEN** the server sends a notification mail
- **THEN** the `Message-ID` SHALL use the server's local hostname as the right-hand side
- **AND** a single WARN SHALL be logged for the send attempt noting that the From-address is malformed
- **AND** the send SHALL proceed (no behavior regression versus today)

### Requirement: Notification Mail Body Uses multipart/alternative with UTF-8

Mention-style notification mails (those routed through `OutgoingMailProcessor.sendMail`) SHALL be delivered as `multipart/alternative` with both a `text/plain; charset=UTF-8` part derived from the raw caption / sub-caption / body content, and a `text/html; charset=UTF-8` part rendered from the email template.

#### Scenario: Mention notification has both alternative parts

- **WHEN** the server sends a mention notification
- **THEN** the top-level `Content-Type` SHALL be `multipart/alternative` (or `multipart/related` enclosing a `multipart/alternative` when the logo is embedded)
- **AND** the alternative SHALL contain one `text/plain; charset=UTF-8` part with the readable text version
- **AND** the alternative SHALL contain one `text/html; charset=UTF-8` part with the templated HTML version

#### Scenario: Status and monitor mails stay plain text with explicit charset

- **WHEN** the server sends a status or monitor mail (via `SystemManagement.statusMail` or `SystemMonitorTask.statusMail`)
- **THEN** the body SHALL be a single `text/plain; charset=UTF-8` part
- **AND** the `Subject` header SHALL be set with explicit UTF-8 charset so German umlauts are RFC 2047 encoded-words

### Requirement: Notification Mails Carry Deliverability Headers

Every server-originated notification mail SHALL include the headers `Reply-To`, `Auto-Submitted`, and `X-Mailer`.

#### Scenario: Headers are present on every send

- **WHEN** the server sends any notification mail
- **THEN** the mail SHALL contain header `Reply-To:` set to the resolved From-address
- **AND** the mail SHALL contain header `Auto-Submitted: auto-generated`
- **AND** the mail SHALL contain header `X-Mailer: j-lawyer.org Server`

### Requirement: Logo Embedded via CID Inline Part

The HTML notification template SHALL reference its logo via a `cid:` Content-ID inline part, not via a `data:` URI. The PNG resource ships as `/templates/email/logo.png` on the classpath.

#### Scenario: Logo is delivered as an inline related part

- **GIVEN** the resource `/templates/email/logo.png` is present on the classpath
- **WHEN** the server sends a mention notification
- **THEN** the MIME structure SHALL be `multipart/related` enclosing a `multipart/alternative` and a `MimeBodyPart` for the logo
- **AND** the logo part SHALL have `Content-Disposition: inline`, `Content-ID: <logo>`, and a PNG content type
- **AND** the HTML alternative SHALL reference `<img src="cid:logo">`

#### Scenario: Missing logo resource degrades silently

- **GIVEN** the resource `/templates/email/logo.png` cannot be found on the classpath
- **WHEN** the server sends a mention notification
- **THEN** the mail SHALL still be sent successfully (no exception, no aborted send)
- **AND** the MIME structure SHALL fall back to `multipart/alternative` without the related wrap
- **AND** an INFO entry SHALL be logged at most once per process noting the missing resource

### Requirement: Test-Mail Path Uses the Same Header Set

The "Einstellungen testen" path (`SystemManagement.testSendMail`) SHALL produce a plain-text test mail that carries the same `Message-ID`, `From`, `Reply-To`, `Auto-Submitted`, `X-Mailer`, and UTF-8 subject behavior as production notifications, so operators can verify the header set without changing other settings.

#### Scenario: Test mail includes the new headers

- **GIVEN** an operator clicks "Einstellungen testen" in `SystemMailboxDialog`
- **WHEN** the test mail is received
- **THEN** the raw headers SHALL contain `Message-ID` with the domain of the entered recipient address
- **AND** the raw headers SHALL contain `Reply-To`, `Auto-Submitted: auto-generated`, `X-Mailer: j-lawyer.org Server`
- **AND** the body SHALL be a single `text/plain; charset=UTF-8` part
