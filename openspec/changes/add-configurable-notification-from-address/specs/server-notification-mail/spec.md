## ADDED Requirements

### Requirement: Configurable From-Address for Server Notifications

The server SHALL support an operator-configurable From-address for server-originated notification mails, decoupled from the SMTP authentication user, via a new server setting `jlawyer.server.monitor.smtpfrom`. When unset or empty the From-address SHALL fall back to `jlawyer.server.monitor.smtpuser`.

#### Scenario: Operator configures a deliverable From-address

- **GIVEN** an operator has set `jlawyer.server.monitor.smtpfrom` to `noreply@kanzlei.example`
- **WHEN** the server sends any notification mail (mention notification, status mail, monitor mail)
- **THEN** the mail's `From:` header SHALL contain `noreply@kanzlei.example`
- **AND** the mail's `Reply-To:` header SHALL contain `noreply@kanzlei.example`
- **AND** the mail's `Message-ID:` right-hand side SHALL be `kanzlei.example`
- **AND** the SMTP envelope sender (`mail.smtp.from` property) SHALL be set to `noreply@kanzlei.example`

#### Scenario: Empty setting preserves the smtpuser fallback

- **GIVEN** `jlawyer.server.monitor.smtpfrom` is empty or unset
- **WHEN** the server sends any notification mail
- **THEN** the mail's `From:` header SHALL contain the value of `jlawyer.server.monitor.smtpuser`
- **AND** the SMTP envelope sender SHALL NOT be explicitly overridden (JavaMail default applies)

### Requirement: Settings Dialog Surfaces the New From-Address Field

The desktop client's `SystemMailboxDialog` SHALL expose the new `smtpfrom` setting as a separate text field and SHALL relabel the existing SMTP authentication-user field to remove the ambiguity with the From-address.

#### Scenario: New field is present in the dialog

- **WHEN** an operator opens the system mailbox configuration dialog
- **THEN** a labeled text field "Absenderadresse (From):" SHALL be visible above the existing "Absendername" field
- **AND** the field SHALL be populated from the current value of `jlawyer.server.monitor.smtpfrom`
- **AND** a tooltip SHALL explain that the field is optional and that the value should be a DNS-aligned email address

#### Scenario: Existing auth-user field is correctly labeled

- **WHEN** an operator opens the system mailbox configuration dialog
- **THEN** the field that holds the SMTP authentication user (previously labeled "Absenderadresse:") SHALL be labeled "Anmeldename (SMTP-Benutzer):"

#### Scenario: Test-mail button prefers the configured From-address

- **GIVEN** the operator has entered a value into the new "Absenderadresse (From)" field
- **WHEN** the operator clicks "Einstellungen testen"
- **THEN** the test mail SHALL be sent using the entered value as the From-address
- **AND** if the From-address field is empty, the existing fallback (the recipient input) SHALL be used so behavior matches today
