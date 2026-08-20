## ADDED Requirements

### Requirement: Opaque-Signed S/MIME Unwrapping

The system SHALL transparently unwrap opaque-signed S/MIME envelopes (`application/pkcs7-mime; smime-type=signed-data`) so that the inner MIME content's body and attachments are accessible to all existing email-processing paths.

#### Scenario: User opens an opaque-signed mail in the inbox

- **GIVEN** an IMAP message with top-level `Content-Type: application/pkcs7-mime; smime-type=signed-data`
- **AND** the message is from a known mail server (e.g., Outlook, beA, DATEV-Mail)
- **WHEN** the user selects the message in the mail inbox UI
- **THEN** the message body SHALL be rendered using the original inner MIME content
- **AND** the attachment list SHALL display the original attachment file names, not `smime.p7m`

#### Scenario: User saves an opaque-signed mail to a case with separate attachments

- **GIVEN** an opaque-signed message has been selected in the inbox
- **WHEN** the user invokes "in Akte speichern" with the "Anhänge separat" option
- **THEN** the case SHALL receive the `.eml` file plus one document per original attachment
- **AND** no document named `smime.p7m` SHALL be created

#### Scenario: Background mailbox scan archives opaque-signed mail

- **GIVEN** a mailbox configured for automatic archival via `MailboxScannerTask`
- **WHEN** the scanner processes an opaque-signed message
- **THEN** the archived case documents SHALL contain the unwrapped attachments
- **AND** the Lucene index SHALL contain the unwrapped body text for full-text search

### Requirement: Clear-Signed S/MIME Compatibility

The system SHALL continue to handle clear-signed S/MIME (`multipart/signed; protocol="application/pkcs7-signature"`) exactly as before opaque-signed support was added.

#### Scenario: Clear-signed mail with detached signature

- **GIVEN** a message with `Content-Type: multipart/signed; protocol="application/pkcs7-signature"`
- **WHEN** any walker processes the message
- **THEN** the inner multipart's body and attachments SHALL be visible as today
- **AND** the detached `smime.p7s` signature SHALL appear in the attachment list as today

### Requirement: Encrypted S/MIME Detection

The system SHALL detect encrypted S/MIME (`application/pkcs7-mime; smime-type=enveloped-data`) and log a WARN-level diagnostic, while preserving the current user-visible behavior (the `smime.p7m` blob remains visible as an opaque attachment).

#### Scenario: Encrypted mail received

- **GIVEN** an IMAP message with `Content-Type: application/pkcs7-mime; smime-type=enveloped-data`
- **WHEN** any walker processes the message
- **THEN** a single WARN log entry SHALL be emitted noting the message subject and sender
- **AND** the attachment list SHALL show `smime.p7m` as today (no decryption is attempted)
- **AND** no exception SHALL be thrown

### Requirement: Malformed S/MIME Graceful Degradation

The system SHALL never propagate exceptions from S/MIME parsing into the calling walker. If the PKCS#7 blob cannot be parsed, the walker SHALL continue with the original (unwrapped) `Part`.

#### Scenario: Truncated or corrupted smime.p7m

- **GIVEN** a message with `Content-Type: application/pkcs7-mime` whose body cannot be parsed by BouncyCastle
- **WHEN** the unwrap helper is invoked
- **THEN** the helper SHALL log a WARN entry with the parse failure cause
- **AND** the helper SHALL return `null`
- **AND** the calling walker SHALL proceed with the original `Part`, producing the same output as before this change

### Requirement: Shared Unwrap Helper

The system SHALL provide exactly one implementation of the S/MIME unwrap logic, located in `CommonMailUtils` within `j-lawyer-server-common`, callable from the client, the server EJB, and the background scanner without code duplication.

#### Scenario: All walkers use the same helper

- **GIVEN** the walkers in `CommonMailUtils`, `EmailService` (server EJB), and `EmailUtils` (client)
- **WHEN** any of them encounters an `application/pkcs7-mime` part
- **THEN** all three SHALL delegate to `CommonMailUtils.unwrapSMIME(Part)` for the unwrap decision
- **AND** no walker SHALL re-implement PKCS#7 parsing locally

### Requirement: BouncyCastle Runtime Dependency

The build SHALL include BouncyCastle 1.74 (`bcprov-jdk18on`, `bcpkix-jdk18on`, `bcutil-jdk18on`) on the classpath of `j-lawyer-server-common`, `j-lawyer-server-ejb`, and `j-lawyer-client`.

#### Scenario: Client distribution contains BouncyCastle JARs

- **WHEN** the client is built via `ant -buildfile j-lawyer-client/build.xml default`
- **THEN** `j-lawyer-client/dist/lib/` SHALL contain `bcprov-jdk18on-1.74.jar`, `bcpkix-jdk18on-1.74.jar`, and `bcutil-jdk18on-1.74.jar`

#### Scenario: Server EAR contains BouncyCastle JARs

- **WHEN** the server is built via `ant -Dj2ee.server.home=... -buildfile j-lawyer-server/build.xml default`
- **THEN** the resulting `j-lawyer-server.ear` SHALL contain the three BC JARs in its `lib/` directory
