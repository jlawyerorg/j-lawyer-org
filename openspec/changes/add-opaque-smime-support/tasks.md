## 1. Dependency setup (Maven)

- [ ] 1.1 Root `pom.xml`: add `org.bouncycastle:bcprov-jdk18on`, `bcpkix-jdk18on`, `bcutil-jdk18on` at version `1.78` to `<dependencyManagement>` (hardcoded version — the project uses no version properties)
- [ ] 1.2 `j-lawyer-server-common/pom.xml`: add the three dependencies version-less, default (compile) scope
- [ ] 1.3 `j-lawyer-server/j-lawyer-server-ejb/pom.xml`: add them version-less with `<scope>provided</scope>` (every dependency in that module is provided — skinny EAR)
- [ ] 1.4 `j-lawyer-server/j-lawyer-server-ear/pom.xml`: add them version-less **without** scope, so `maven-ear-plugin` bundles them into `EAR/lib/` (`defaultLibBundleDir`, `skinnyModules`)
- [ ] 1.5 `j-lawyer-client`: no entry required — BC arrives transitively via `j-lawyer-server-common` (and already via the iText BC adapters). Confirm rather than add.
- [ ] 1.6 Compile-only check of the reactor (no functional change yet)

## 2. Shared unwrap helpers

- [ ] 2.1 Add `public static Part unwrapSMIME(Part part)` to `CommonMailUtils`, returning the inner `MimeBodyPart` or `null`
- [ ] 2.2 Pre-filter on `part.isMimeType("application/pkcs7-mime")` / `"application/x-pkcs7-mime"`, then decide on the CMS ContentInfo OID: `1.2.840.113549.1.7.2` (signedData) → unwrap, `1.2.840.113549.1.7.3` (envelopedData) → WARN + `null`
- [ ] 2.3 For signedData: `new CMSSignedData(part.getInputStream())`, null-check `getSignedContent()` (null for detached signatures), `write(...)` into a buffer, re-parse as `MimeBodyPart`, return it
- [ ] 2.4 Wrap all BouncyCastle calls in try/catch; on any failure log WARN with the cause and return `null`
- [ ] 2.5 Add `public static Object unwrapContent(Part part)` returning the inner `Part` when `unwrapSMIME` succeeds, otherwise `part.getContent()` — this is the drop-in replacement for walker-call-site `getContent()` calls
- [ ] 2.6 JavaDoc on both: return contract, the "graceful degradation" guarantee, and why `unwrapContent` returns the `Part` rather than its content

## 3. Replace `getContent()` at walker call sites

This is the step the previous revision of this change was missing: `CommonMailUtils`'
walkers receive `msg.getContent()`, which for an opaque-signed mail is an `InputStream`,
so an entry-point unwrap inside the walker can never fire.

- [ ] 3.1 `MailboxScannerTask:1642` — `getAttachmentInfo(CommonMailUtils.unwrapContent(msg))`
- [ ] 3.2 `MailboxScannerTask:1337` / `:1356` — same for both `recursiveFindPart` calls
- [ ] 3.3 `MailboxScannerTask:1335` — widen the `copiedMsg.isMimeType("multipart/*")` guard so an opaque-signed message does not fall into the non-multipart branch and end up with an empty body (breaks case-number matching)
- [ ] 3.4 `CommonMailUtils.getAttachmentBytes(String, Message):1070` — unwrap internally (it holds the `Message`)
- [ ] 3.5 Client: `MailContentUI:1419/1427/1467`, `ViewEmailFrame:1135/1321`, `EmailInboxPanel:2812/4010/4048`, `EmailToPDFConverter:694/720`, `EmailUtils:1070`
- [ ] 3.6 Re-grep for any remaining `getAttachmentNames(`/`getAttachmentInfo(`/`recursiveFindPart(`/`getAttachmentPart(` call that still passes `.getContent()`

## 4. Walker entry-point hooks

These walkers receive the `Message` itself as a `Part`, so an entry-point unwrap suffices.

- [ ] 4.1 `EmailService.extractAttachments:2158`
- [ ] 4.2 `EmailService.extractAttachmentsMetadata:2268` (clone of 4.1 — if only one is hooked, attachment list and attachment metadata disagree)
- [ ] 4.3 `EmailService.extractBody:3567`
- [ ] 4.4 `EmailService.hasAttachments:3534` (paperclip indicator; shallow one-level check today)
- [ ] 4.5 `EmailUtils.getAttachmentPart(String, Object, Folder):954` — keep `closeIfIMAP(folder)` correct on every return path introduced by the unwrap
- [ ] 4.6 `EmailUtils.checkForAttachments:1104` and `EmailUtils.hasAttachment:833`
- [ ] 4.7 `EmailUtils.removeAttachmentsFromMessage:1796` / `removeAttachmentsFromMultipart:1836` — "save to case without attachments" currently strips nothing from an opaque envelope
- [ ] 4.8 In the `Part.ATTACHMENT` disposition branch of `getAttachmentInfo` / `getAttachmentPart`, unwrap a **nested** `smime.p7m` and recurse instead of listing the blob (gateway-wrapped variant)
- [ ] 4.9 Note in the code that no hook is needed in `j-lawyer-server/j-lawyer-io` — `EmailEndpointV7` delegates exclusively to `EmailServiceLocal`

## 5. Exchange / Microsoft Graph mailboxes

Graph mailboxes (`MailboxSetup.isMsExchange()`) never build a JavaMail `Part` when reading, so sections 3 and 4 do not reach them.

- [ ] 5.1 `EmailService.graphGetAttachments:2919` — if the Graph response carries a single `smime.p7m` attachment, feed its `contentBytes` through a `MimeBodyPart` + `unwrapSMIME` and return the inner attachments instead of the blob
- [ ] 5.2 `EmailService.graphGetAttachmentsMetadata:2949` — same, metadata only
- [ ] 5.3 `EmailService.graphGetMessage:2867` — same for the body
- [ ] 5.4 If 5.1–5.3 are deferred, record Exchange/Graph as an explicit non-goal in the spec rather than leaving a silent gap

## 6. Automated tests

`j-lawyer-server-common` has `src/test/java` and junit 4.12 as a test dependency — this is newly possible since the Maven migration.

- [ ] 6.1 Add fixtures under `j-lawyer-server-common/src/test/resources/`: one opaque-signed `.eml`, one clear-signed (recover the deleted fixture with `git show 3494dcf68:signed.eml`), one truncated `smime.p7m`
- [ ] 6.2 `src/test/java/com/jdimension/jlawyer/test/email/CommonMailUtilsSMimeTest.java`: opaque-signed → attachment names and bytes match the inner originals
- [ ] 6.3 Clear-signed regression: attachment list unchanged from before this change
- [ ] 6.4 Malformed blob → `unwrapSMIME` returns `null`, no exception escapes; walker output equals pre-change output
- [ ] 6.5 envelopedData → `null` plus WARN, `smime.p7m` still listed
- [ ] 6.6 Non-S/MIME message → `unwrapContent(msg)` returns exactly what `msg.getContent()` returns (behaviour-preservation guard)

## 7. Field diagnostics

- [ ] 7.1 Log content type and CMS ContentInfo OID once per S/MIME message, so signed-vs-encrypted can be confirmed on the affected installation (no sample `.eml` was available when this change was written)

## 8. Manual verification

- [ ] 8.1 `./build-fast.sh`
- [ ] 8.2 `unzip -l j-lawyer-server/j-lawyer-server-ear/target/j-lawyer-server.ear | grep -i bouncycastle` shows the three jars in `lib/`
- [ ] 8.3 `ls j-lawyer-client/target/lib/ | grep bc` shows them client-side; confirm which `bcpkix` variant wins the split package and that the unwrap works in the built client
- [ ] 8.4 Open an opaque-signed mail in a server-based mailbox: body rendered, attachments listed by original name, paperclip indicator set
- [ ] 8.5 "In Akte speichern" with "Anhänge separat": case receives `.eml` + original attachments, no `smime.p7m`; and with "ohne Anhänge": attachments actually stripped
- [ ] 8.6 Same mail in a legacy direct-IMAP (non-server-based) mailbox
- [ ] 8.7 REST: `GET /j-lawyer-io/rest/v7/email/{mailboxId}/messages/{ref}/attachments` lists the original names; fetching one returns the PDF bytes
- [ ] 8.8 `MailboxScannerTask` run over a mailbox holding an opaque-signed mail: case-number matching finds the number in the body, archived documents contain the unwrapped attachments
- [ ] 8.9 Lucene search for a phrase from inside an opaque-signed mail body finds the mail
- [ ] 8.10 Open an already-archived `.eml` from a case in the viewer: original attachments visible (no data migration involved)
- [ ] 8.11 Clear-signed regression check against the recovered `signed.eml`
- [ ] 8.12 Exchange/Graph mailbox with an opaque-signed mail (if 5.1–5.3 implemented)

## 9. Documentation

- [ ] 9.1 Add `.p7m` to the extension table in `ServerFileUtils.java:727-730` (it lists `.pkcs7` and `.p7s` only)
- [ ] 9.2 Note in `CLAUDE.md` / `openspec/project.md` that BouncyCastle is now a server-side runtime dependency and why
- [ ] 9.3 Update `openspec/specs/email-client/spec.md` Purpose if it still says "TBD" (optional cleanup, unrelated to this change)
