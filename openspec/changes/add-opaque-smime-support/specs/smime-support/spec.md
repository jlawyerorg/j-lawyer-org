## ADDED Requirements

### Requirement: Opaque-Signed S/MIME Unwrapping

The system SHALL transparently unwrap opaque-signed S/MIME envelopes (`application/pkcs7-mime; smime-type=signed-data`) so that the inner MIME content's body and attachments are accessible to all existing email-processing paths.

#### Scenario: User opens an opaque-signed mail in the inbox

- **GIVEN** an IMAP message with top-level `Content-Type: application/pkcs7-mime; smime-type=signed-data`
- **AND** the message is from a known mail server (e.g., Outlook, beA, DATEV-Mail)
- **WHEN** the user selects the message in the mail inbox UI
- **THEN** the message body SHALL be rendered using the original inner MIME content
- **AND** the attachment list SHALL display the original attachment file names, not `smime.p7m`
- **AND** the "has attachments" indicator SHALL be set

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

#### Scenario: Nested opaque-signed part

- **GIVEN** a `multipart/mixed` message that contains an `application/pkcs7-mime; smime-type=signed-data` part (gateway-wrapped variant)
- **WHEN** any walker enumerates attachments
- **THEN** the inner attachments SHALL be listed
- **AND** `smime.p7m` SHALL NOT appear in the attachment list

### Requirement: Opaque-Signed Envelope as the Top-Level Message

The system SHALL correctly handle an opaque-signed envelope that is the top-level content type of the message, where `Message.getContent()` yields a raw `InputStream` rather than a `Part` or `Multipart`. Attachment enumeration SHALL NOT return an empty list and MIME traversal SHALL NOT raise a `ClassCastException` for such messages.

#### Scenario: Attachment enumeration for a top-level opaque envelope

- **GIVEN** a message whose top-level `Content-Type` is `application/pkcs7-mime; smime-type=signed-data`
- **WHEN** attachment enumeration runs on that message
- **THEN** the inner attachments SHALL be returned
- **AND** no `ClassCastException` SHALL be raised

#### Scenario: Case-number detection in the body during auto-import

- **GIVEN** the background scanner processes a top-level opaque-signed message whose inner body contains a case number
- **WHEN** the scanner extracts the body to match a case
- **THEN** the unwrapped body text SHALL be used for matching
- **AND** the message SHALL be assigned to the matching case as it would be for an unsigned mail

#### Scenario: Behaviour for non-S/MIME mail is unchanged

- **GIVEN** any message that is not an S/MIME container
- **WHEN** a walker obtains the content to traverse
- **THEN** it SHALL receive exactly the value that `Part.getContent()` returned before this change

### Requirement: Coverage Across All Access Paths

The system SHALL present the same unwrapped body and attachment list for a given opaque-signed message regardless of the access path: background auto-import, the desktop client for server-based mailboxes, the desktop client's legacy direct-IMAP path, an archived `.eml` opened in the client viewer, and the REST API.

#### Scenario: REST API returns the original attachments

- **GIVEN** an opaque-signed message in a mailbox reachable through the REST API
- **WHEN** a client calls `GET /j-lawyer-io/rest/v7/email/{mailboxId}/messages/{messageRef}/attachments`
- **THEN** the response SHALL list the original attachment names, not `smime.p7m`
- **AND** fetching a single attachment SHALL return the original attachment content

#### Scenario: Archived mail opened in the viewer

- **GIVEN** a case document that is an `.eml` of an opaque-signed message archived before this change
- **WHEN** the user opens that document in the client's mail viewer
- **THEN** the body and the original attachments SHALL be displayed
- **AND** the existing case documents SHALL NOT be modified or migrated

#### Scenario: Exchange/Graph mailbox

- **GIVEN** an opaque-signed message in a mailbox served through the Microsoft Graph API, where attachments are returned as JSON rather than traversed as MIME
- **WHEN** the attachment list is requested
- **THEN** the original attachment names SHALL be returned rather than `smime.p7m`

### Requirement: Clear-Signed S/MIME Compatibility

The system SHALL continue to handle clear-signed S/MIME (`multipart/signed; protocol="application/pkcs7-signature"`) exactly as before opaque-signed support was added.

#### Scenario: Clear-signed mail with detached signature

- **GIVEN** a message with `Content-Type: multipart/signed; protocol="application/pkcs7-signature"`
- **WHEN** any walker processes the message
- **THEN** the inner multipart's body and attachments SHALL be visible as today
- **AND** the detached `smime.p7s` signature SHALL appear in the attachment list as today

### Requirement: Encrypted S/MIME Detection

The system SHALL detect encrypted S/MIME (CMS ContentInfo OID `1.2.840.113549.1.7.3`, `smime-type=enveloped-data`) and log a WARN-level diagnostic, while preserving the current user-visible behavior (the `smime.p7m` blob remains visible as an opaque attachment).

#### Scenario: Encrypted mail received

- **GIVEN** an IMAP message with `Content-Type: application/pkcs7-mime; smime-type=enveloped-data`
- **WHEN** any walker processes the message
- **THEN** a single WARN log entry SHALL be emitted noting the message subject and sender
- **AND** the attachment list SHALL show `smime.p7m` as today (no decryption is attempted)
- **AND** no exception SHALL be thrown

#### Scenario: Detection does not rely on the MIME parameter alone

- **GIVEN** a message declaring `application/x-pkcs7-mime` without an `smime-type` parameter
- **WHEN** the unwrap helper inspects it
- **THEN** the decision between signed-data and enveloped-data SHALL be taken from the CMS ContentInfo content-type OID

### Requirement: Malformed S/MIME Graceful Degradation

The system SHALL never propagate exceptions from S/MIME parsing into the calling walker. If the PKCS#7 blob cannot be parsed, the walker SHALL continue with the original (unwrapped) content.

#### Scenario: Truncated or corrupted smime.p7m

- **GIVEN** a message with `Content-Type: application/pkcs7-mime` whose body cannot be parsed by BouncyCastle
- **WHEN** the unwrap helper is invoked
- **THEN** the helper SHALL log a WARN entry with the parse failure cause
- **AND** the helper SHALL return `null`
- **AND** the calling walker SHALL proceed with the original content, producing the same output as before this change

#### Scenario: Detached signature with no embedded content

- **GIVEN** a PKCS#7 SignedData structure whose signed content is absent (detached signature)
- **WHEN** the unwrap helper is invoked
- **THEN** the helper SHALL return `null` without raising an exception

### Requirement: Shared Unwrap Helper

The system SHALL provide exactly one implementation of the S/MIME unwrap logic, located in `CommonMailUtils` within `j-lawyer-server-common`, callable from the client, the server EJB, and the background scanner without code duplication. It SHALL expose both the unwrap decision (`unwrapSMIME(Part)`) and the content accessor used by walker call sites (`unwrapContent(Part)`).

#### Scenario: All paths use the same helper

- **GIVEN** the walkers in `CommonMailUtils`, `EmailService` (server EJB), the client's `EmailUtils`, and the Graph-based code paths
- **WHEN** any of them encounters an `application/pkcs7-mime` container
- **THEN** all of them SHALL delegate to `CommonMailUtils.unwrapSMIME(Part)` for the unwrap decision
- **AND** no caller SHALL re-implement PKCS#7 parsing locally

#### Scenario: Call sites obtain walker input through the helper

- **GIVEN** a code path that previously passed `message.getContent()` into a MIME walker
- **WHEN** that path is invoked
- **THEN** it SHALL obtain the walker input from `CommonMailUtils.unwrapContent(message)` instead

### Requirement: BouncyCastle Runtime Dependency

The build SHALL include BouncyCastle 1.78 (`org.bouncycastle:bcprov-jdk18on`, `bcpkix-jdk18on`, `bcutil-jdk18on`) on the classpath of `j-lawyer-server-common`, `j-lawyer-server-ejb`, the assembled EAR, and `j-lawyer-client`. The version SHALL be pinned once in the root `pom.xml` `dependencyManagement` and SHALL match the version already resolved transitively by the iText BouncyCastle adapters, so no conflicting BouncyCastle version is introduced.

#### Scenario: Client distribution contains BouncyCastle

- **WHEN** the reactor is built with `mvn install` from the repository root
- **THEN** `j-lawyer-client/target/lib/` SHALL contain `bcprov-jdk18on`, `bcpkix-jdk18on` and `bcutil-jdk18on` at version 1.78

#### Scenario: Server EAR contains BouncyCastle

- **WHEN** the reactor is built with `mvn install` from the repository root
- **THEN** the resulting `j-lawyer-server.ear` SHALL contain the three BouncyCastle jars in its `lib/` directory

#### Scenario: Unwrap works despite the duplicate CMS package on the client

- **GIVEN** the client classpath contains both `bcpkix-jdk18on` and `bcpkix-fips`, which both provide `org.bouncycastle.cms.CMSSignedData`
- **WHEN** the client unwraps an opaque-signed message
- **THEN** the unwrap SHALL succeed without requiring a JCE provider to be registered
