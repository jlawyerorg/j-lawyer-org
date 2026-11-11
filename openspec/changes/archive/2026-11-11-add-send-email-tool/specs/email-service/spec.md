## ADDED Requirements

### Requirement: Send Mail Returning The Sent Message
`EmailServiceRemote` and `EmailServiceLocal` SHALL provide `sendMailReturningEml`, which takes the
same parameters as `sendMail`, is subject to the same role and mailbox access checks, sends the
message the same way and additionally returns the sent message as RFC 822 bytes, so that callers
can file what actually went out instead of a reconstruction. `sendMail` SHALL keep its behaviour.

#### Scenario: SMTP mailbox
- **WHEN** `sendMailReturningEml` is called for an IMAP/SMTP mailbox
- **THEN** the message SHALL be transported and copied to the Sent folder as with `sendMail`
- **AND** the returned bytes SHALL be the transported message, including its Message-ID, date and
  inline image structure, plus the `Bcc` header

#### Scenario: Microsoft Graph mailbox
- **WHEN** `sendMailReturningEml` is called for an Exchange mailbox
- **THEN** the message SHALL be sent through Graph and saved to Sent Items as with `sendMail`
- **AND** the returned bytes SHALL be a MIME message built from the same recipients, subject, body
  and attachments, including the `Bcc` header

#### Scenario: No access to the mailbox
- **WHEN** the caller has no access to the mailbox
- **THEN** the call SHALL fail as `sendMail` does and nothing SHALL be sent
