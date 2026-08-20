# Change: Add support for opaque-signed S/MIME emails

## Why

When the user receives an S/MIME-signed email and saves it to a case, only a single attachment `smime.p7m` is visible — the original attachments and the body are missing. Commit `3494dcf68` (close #3190) added support for *clear-signed* S/MIME (`multipart/signed`), but not for **opaque-signed** S/MIME (`application/pkcs7-mime; smime-type=signed-data`), where the entire signed MIME content (body + attachments + signature) is wrapped in a single PKCS#7 SignedData blob delivered as `smime.p7m`. Outlook (default setting), beA-Versandservice, DATEV-Mail, and many government mail services produce this opaque variant.

Opaque-signed S/MIME can be unwrapped **without any private key** — the signature only wraps cleartext MIME. Once unwrapped, all existing walker logic for body extraction, attachment enumeration, and "save to case" works as for normal mails.

## What Changes

- New helper `CommonMailUtils.unwrapSMIME(Part)` in `j-lawyer-server-common` that detects and unwraps PKCS#7 SignedData envelopes using BouncyCastle's `CMSSignedData`, returning the inner `Part` (or `null` if the input is not an opaque-signed S/MIME container).
- All MIME-tree walkers (server EJB `EmailService.extractAttachments`/`extractBody`, server-common `CommonMailUtils.getAttachmentInfo`/`getAttachmentBytes`/`recursiveFindPart`, client `EmailUtils.getAttachmentPart`/`getAttachmentNames`/`getAttachmentBytes`/`recursiveFindPart`) invoke `unwrapSMIME` at their entry points before applying the existing recursion logic, so opaque S/MIME envelopes become transparent.
- Encrypted S/MIME (`smime-type=enveloped-data`) is detected and logged with a WARN entry; behavior is unchanged from today (user still sees `smime.p7m`). Decryption is explicitly out of scope.
- **New runtime dependency**: BouncyCastle 1.74 (`bcprov-jdk18on`, `bcpkix-jdk18on`, `bcutil-jdk18on`) added to the classpath of `j-lawyer-server-common`, `j-lawyer-server-ejb`, and `j-lawyer-client`. JARs already exist in the repo under `j-lawyer-client/lib/bea.bak/` and only need to be referenced.

## Impact

- Affected specs: new capability `smime-support` (added)
- Affected code:
  - `j-lawyer-server-common/src/com/jdimension/jlawyer/email/CommonMailUtils.java`
  - `j-lawyer-server/j-lawyer-server-ejb/src/java/com/jdimension/jlawyer/services/EmailService.java`
  - `j-lawyer-client/src/com/jdimension/jlawyer/client/mail/EmailUtils.java`
  - `j-lawyer-server/j-lawyer-server-war/src/java/com/jdimension/jlawyer/timer/MailboxScannerTask.java` (transitively via `CommonMailUtils`)
  - `nbproject/project.properties` of all three modules (BC classpath entries)
- User-visible behavior: opaque-signed mails now show body and original attachments; "save to case" with separate attachments saves the real files instead of `smime.p7m`; Lucene index can now index signed mail bodies for full-text search.
- No DB schema changes, no REST API changes, no breaking changes for clear-signed S/MIME (regression path).
