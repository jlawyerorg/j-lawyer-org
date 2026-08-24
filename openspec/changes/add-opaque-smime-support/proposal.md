# Change: Add support for opaque-signed S/MIME emails

## Status: deferred — pending a real opaque-signed sample

Investigating #3387 produced a sample mail that shows the reported symptom (only
`smime.p7m` visible, PDF attachment inaccessible) but is **not** opaque-signed: a sending
gateway had wrapped an ordinary clear-signed message as a single body part with
`Content-Type: multipart/signed`, `Content-Disposition: attachment; filename=smime.p7m`.
There is no PKCS#7 blob, the content is in the clear, and the cause was a missing
recursion in the `ATTACHMENT` disposition branch of the `CommonMailUtils` walkers. That is
fixed separately under #3566 and needs no BouncyCastle.

This change remains valid for **genuine** opaque-signed S/MIME
(`application/pkcs7-mime; smime-type=signed-data` with a CMS SignedData blob), which does
occur in the wild — but no such sample has been observed in this installation so far, and
the #3566 finding makes it less likely that #3387 was ever about this variant. Implement
only once a real `application/pkcs7-mime` message is available to verify against;
otherwise the BouncyCastle dependency would be introduced on an unverified premise.

## Why

When the user receives an S/MIME-signed email, only a single attachment `smime.p7m` is
visible (or, in the background scanner, no attachment at all) — the original attachments
and the body are missing. Commit `3494dcf68` (close #3190) added support for *clear-signed*
S/MIME (`multipart/signed`), but not for **opaque-signed** S/MIME
(`application/pkcs7-mime; smime-type=signed-data`), where the entire signed MIME content
(body + attachments + signature) is wrapped in a single PKCS#7 SignedData blob delivered
as `smime.p7m`. Outlook (default setting), beA-Versandservice, DATEV-Mail, and many
government mail services produce this opaque variant.

Opaque-signed S/MIME can be unwrapped **without any private key** — the signature only
wraps cleartext MIME. Once unwrapped, all existing walker logic for body extraction,
attachment enumeration, and "save to case" works as for normal mails.

## What Changes

- Two new helpers in `CommonMailUtils` (`j-lawyer-server-common`):
  - `unwrapSMIME(Part) : Part` — detects a PKCS#7 SignedData envelope via the CMS
    ContentInfo OID and returns the inner `MimeBodyPart`, or `null` if the part is not an
    opaque-signed S/MIME container.
  - `unwrapContent(Part) : Object` — the value that walkers should be fed. Returns the
    unwrapped inner `Part` for opaque-signed mails and plain `part.getContent()` otherwise,
    so behaviour for every other message is bit-identical.
- **Walker call sites are changed, not only walker entry points.** The `CommonMailUtils`
  walkers are invoked with `msg.getContent()`, not with the `Message`. For an opaque-signed
  mail `getContent()` yields an `InputStream` (JavaMail has no DataContentHandler for
  `application/pkcs7-mime`), so no `Part` ever reaches the walker and an entry-point-only
  unwrap would have no effect. Today this produces an empty attachment list in
  `getAttachmentInfo` and a `ClassCastException` in `getAttachmentPart` /
  `recursiveFindPart`. Every such call site passes through `unwrapContent` instead.
- Walkers that *do* receive the `Message` as a `Part` get an entry-point unwrap:
  `EmailService.extractAttachments`, `extractAttachmentsMetadata`, `extractBody`,
  `hasAttachments`, plus the client's folder-aware `EmailUtils.getAttachmentPart(String,
  Object, Folder)`, `checkForAttachments` and `hasAttachment`.
- A nested `smime.p7m` (gateway-wrapped variant) is unwrapped in the `ATTACHMENT`
  disposition branch instead of being listed as an attachment.
- Exchange/Graph mailboxes (`MailboxSetup.isMsExchange()`) do not walk MIME at all —
  attachments arrive as JSON from the Graph API. `graphGetAttachments` /
  `graphGetAttachmentsMetadata` / `graphGetMessage` unwrap the `smime.p7m` attachment
  bytes explicitly, so Exchange mailboxes are not silently left behind.
- Encrypted S/MIME (`smime-type=enveloped-data`) is detected via its OID and logged with a
  WARN entry; behaviour is unchanged from today (user still sees `smime.p7m`). Decryption
  is explicitly out of scope.
- **New runtime dependency**: BouncyCastle **1.78** (`bcprov-jdk18on`, `bcpkix-jdk18on`,
  `bcutil-jdk18on`) from Maven Central. It is already on the client classpath transitively
  via the iText 9.0.0 BouncyCastle adapters; it is new for the server side.

## Impact

- Affected specs: new capability `smime-support` (added)
- Affected code:
  - `j-lawyer-server-common/src/main/java/com/jdimension/jlawyer/email/CommonMailUtils.java`
  - `j-lawyer-server/j-lawyer-server-ejb/src/main/java/com/jdimension/jlawyer/services/EmailService.java`
  - `j-lawyer-server/j-lawyer-server-war/src/main/java/com/jdimension/jlawyer/timer/MailboxScannerTask.java`
    (direct change — the `isMimeType("multipart/*")` guard and three `getContent()` call sites)
  - `j-lawyer-client/src/main/java/com/jdimension/jlawyer/client/mail/EmailUtils.java`,
    `MailContentUI.java`, `ViewEmailFrame.java`, `EmailInboxPanel.java`,
    `j-lawyer-client/src/main/java/com/jdimension/jlawyer/client/utils/EmailToPDFConverter.java`
  - `j-lawyer-server-common/src/main/java/com/jdimension/jlawyer/server/utils/ServerFileUtils.java`
    (`.p7m` missing from the extension table)
  - `pom.xml` (root `dependencyManagement`), `j-lawyer-server-common/pom.xml`,
    `j-lawyer-server/j-lawyer-server-ejb/pom.xml`, `j-lawyer-server/j-lawyer-server-ear/pom.xml`
- No change needed in `j-lawyer-server/j-lawyer-io` — `EmailEndpointV7` delegates
  exclusively to `EmailServiceLocal` and holds no MIME logic of its own; the REST API
  inherits the fix.
- User-visible behavior: opaque-signed mails now show body and original attachments;
  "save to case" with separate attachments saves the real files instead of `smime.p7m`;
  the background scanner can match case numbers against the body and archives the real
  attachments; the Lucene index can index signed mail bodies for full-text search.
- Not in scope: retroactive repair of already-archived mails. Existing case documents keep
  their `.eml` (and any `smime.p7m`); the original attachments become reachable by opening
  the archived `.eml` in the client viewer.
- No DB schema changes, no REST API changes, no breaking changes for clear-signed S/MIME
  (regression path).
