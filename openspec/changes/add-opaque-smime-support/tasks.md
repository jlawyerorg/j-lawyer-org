## 1. Dependency setup (manual, by user)

- [ ] 1.1 Copy `bcprov-jdk18on-1.74.jar`, `bcpkix-jdk18on-1.74.jar`, `bcutil-jdk18on-1.74.jar` from `j-lawyer-client/lib/bea.bak/` to `j-lawyer-server-common/lib/`
- [ ] 1.2 Copy the same three JARs to `j-lawyer-server/j-lawyer-server-ejb/lib/`
- [ ] 1.3 Move the same three JARs out of `bea.bak/` into a regular `j-lawyer-client/lib/` location (e.g., new subdir `lib/bc/`)
- [ ] 1.4 In `j-lawyer-server-common/nbproject/project.properties`: add 3 `file.reference.<jar>=lib/<jar>` entries and 3 corresponding entries in `javac.classpath` (pattern: see `${file.reference.log4j-api-2.17.1.jar}` at line 62)
- [ ] 1.5 In `j-lawyer-server/j-lawyer-server-ejb/nbproject/project.properties`: add 3 `file.reference` + 3 `javac.classpath` entries (pattern: see log4j-api entries at lines 107/127/214)
- [ ] 1.6 In `j-lawyer-client/nbproject/project.properties`: add 3 `file.reference` + 3 `javac.classpath` entries (pattern: see `outlook-message-parser-1.9.6.jar` at lines 51/264)
- [ ] 1.7 Confirm clean builds of all three modules (compile only, no functional change yet)

## 2. Shared unwrap helper

- [ ] 2.1 Add public static `CommonMailUtils.unwrapSMIME(Part part)` returning `Part` or `null`
- [ ] 2.2 Implement detection: `part.isMimeType("application/pkcs7-mime")` or `"application/x-pkcs7-mime"` plus `smime-type` parameter check
- [ ] 2.3 For `smime-type=signed-data`: parse via `new CMSSignedData(part.getInputStream())`, extract signed content via `getSignedContent().write(...)`, re-parse bytes as `MimeBodyPart`, return it
- [ ] 2.4 For `smime-type=enveloped-data`: emit one WARN log entry (include subject/from if available) and return `null`
- [ ] 2.5 Wrap all BouncyCastle calls in try/catch; on any failure log WARN and return `null`
- [ ] 2.6 Add JavaDoc explaining return contract and the "graceful degradation" guarantee

## 3. Walker integration

- [ ] 3.1 In `CommonMailUtils.getAttachmentInfo(Object)` (line 967): call `unwrapSMIME` at entry and recurse on the unwrapped Part if non-null
- [ ] 3.2 In `CommonMailUtils.getAttachmentBytes(...)` / `getAttachmentPart(...)`: same pattern
- [ ] 3.3 In `CommonMailUtils.recursiveFindPart(...)`: same pattern (covers `MailboxScannerTask` body extraction)
- [ ] 3.4 In `EmailService.extractAttachments(Part, List<MailAttachmentDTO>)` (line 1806): same pattern
- [ ] 3.5 In `EmailService.extractBody(Part)` (line 3213): same pattern
- [ ] 3.6 In `EmailUtils.getAttachmentPart(...)` (line 954): same pattern
- [ ] 3.7 In `EmailUtils.getAttachmentNames(...)` / `getAttachmentBytes(...)`: same pattern
- [ ] 3.8 In `EmailUtils.recursiveFindPart(...)` (used by `MailContentUI` line 1427/1467): same pattern

## 4. Manual verification

- [ ] 4.1 Build all modules clean (`./build-fast.sh`)
- [ ] 4.2 Confirm EAR contains 3 BC JARs (`unzip -l j-lawyer-server.ear | grep bc-`)
- [ ] 4.3 Confirm client `dist/lib/` contains 3 BC JARs
- [ ] 4.4 Open one opaque-signed test mail in a server-based mailbox: verify body is rendered and attachments are listed by their original names
- [ ] 4.5 "In Akte speichern" with "Anhänge separat" on the opaque-signed mail: verify case receives `.eml` + original attachments, no `smime.p7m`
- [ ] 4.6 Open the existing clear-signed test `signed.eml` (from commit `3494dcf68`): regression check — must still work
- [ ] 4.7 Receive or synthesize one encrypted (`smime-type=enveloped-data`) mail: verify server log contains the WARN line and `smime.p7m` is still shown as today
- [ ] 4.8 Restart `MailboxScannerTask` (or trigger one) on a mailbox containing an opaque-signed mail: verify the archived case documents include the unwrapped attachments
- [ ] 4.9 Lucene search for a unique phrase from inside an opaque-signed mail body: verify the mail is found

## 5. Documentation

- [ ] 5.1 Add a short note to the relevant developer doc (or `CLAUDE.md`) that BouncyCastle is now a build dependency in three modules and why
- [ ] 5.2 Update `openspec/specs/email-client/spec.md` Purpose if it still says "TBD" (optional cleanup, unrelated to this change)
