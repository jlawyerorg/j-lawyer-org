## 1. Shared helper (j-lawyer-server-common)

- [ ] 1.1 Create `j-lawyer-server-common/src/com/jdimension/jlawyer/server/utils/ServerMailUtils.java` with one public static method `prepareNotificationMessage(Session, String fromAddress, String senderName, String to, String subject, String plainTextBody, String htmlBody, boolean embedLogoAsCid, String mailerTag) throws Exception` returning a `MimeMessage`
- [ ] 1.2 Inside `prepareNotificationMessage`: instantiate an anonymous subclass of `MimeMessage` that overrides `updateMessageID()` to set `Message-ID: <UUID.randomUUID()+"."+System.currentTimeMillis()+"@"+fromDomain+">"`; derive `fromDomain` by splitting `fromAddress` on `@`; if `fromAddress` has no `@`, fall back to `InetAddress.getLocalHost().getHostName()` and log a single WARN per send attempt
- [ ] 1.3 Set `From` via `new InternetAddress(fromAddress, senderName, "UTF-8")`; set `Reply-To` to the same address; set `To` via `InternetAddress.parse(to, false)`; set `Subject` via `msg.setSubject(subject, "UTF-8")`; set `SentDate`
- [ ] 1.4 Set headers `Auto-Submitted: auto-generated`, `X-Mailer: <mailerTag>`
- [ ] 1.5 Build body: if `htmlBody == null` → `msg.setText(plainTextBody, "UTF-8")`; else build `MimeMultipart("alternative")` with two parts (`text/plain; charset=UTF-8`, `text/html; charset=UTF-8`)
- [ ] 1.6 If `embedLogoAsCid` and `htmlBody != null`: try to load `/templates/email/logo.png` via `ServerMailUtils.class.getResourceAsStream`; if present, wrap the alternative in a `MimeMultipart("related")` with an inline `MimeBodyPart` (`setDisposition(Part.INLINE)`, `setContentID("<logo>")`, content via `ByteArrayDataSource`); if absent, log INFO once and skip the wrap
- [ ] 1.7 `msg.saveChanges()`; after that, read back `msg.getHeader("Message-ID")` and re-set if it does not match the intended value (defensive against javax.mail version differences); return the message

## 2. OutgoingMailProcessor refactor (j-lawyer-server-ejb)

- [ ] 2.1 Add a new private method `getPlainTextBody(String mainCaption, String subCaption, String bodyContent)` to `OutgoingMailProcessor` returning the raw text concatenation: `mainCaption + "\n\n" + subCaption + "\n\n" + bodyContent + "\n\n--\nBitte antworte nicht auf diese Nachricht.\nDu erhältst diese Mitteilung, weil für deinen Account eine E-Mail-Benachrichtigung aktiviert ist."`
- [ ] 2.2 In `OutgoingMailProcessor.sendMail(...)`: replace the manual `MimeMessage` / `MimeMultipart` construction (lines 829–862) with one call to `ServerMailUtils.prepareNotificationMessage(session, smtpUser, senderName, to, subject, plainBody, htmlBody, true, "j-lawyer.org Server")`
- [ ] 2.3 Keep the existing `Transport.connect / bus.send / bus.close` wrapping and the existing `props.put("mail.from", smtpUser)` line; only the `MimeMessage` creation is replaced

## 3. SystemManagement.statusMail refactor (j-lawyer-server-ejb)

- [ ] 3.1 In `SystemManagement.statusMail(String, String)` (line 1368 ff.): replace the manual `MimeMessage` construction (lines 1465–1482) with `ServerMailUtils.prepareNotificationMessage(session, smtpUser, senderName, smtpTo, subject, body, /*htmlBody*/ null, /*embedLogoAsCid*/ false, "j-lawyer.org Server")` — status mails stay plain text
- [ ] 3.2 Verify by calling the EJB facade from an `IterativeBackupTask` run in a dev container; raw headers must contain the new `Message-ID`, `Reply-To`, `Auto-Submitted`, `X-Mailer` and an explicit charset on Subject and body

## 4. SystemMonitorTask.statusMail refactor (j-lawyer-server-war)

- [ ] 4.1 In `SystemMonitorTask.statusMail(ServerSettingsBeanFacadeLocal, String, String)` (line 1039): apply the same change as in task 3 — replace the manual `MimeMessage` construction with `ServerMailUtils.prepareNotificationMessage(...)` using `smtpUser` as `fromAddress`
- [ ] 4.2 Confirm the WAR module has `j-lawyer-server-common` on its classpath (already used here); add the classpath entry if missing

## 5. testSendMail header improvements (j-lawyer-server-ejb)

- [ ] 5.1 In `SystemManagement.testSendMail(...)` (line 1496): build the `MimeMessage` via `ServerMailUtils.prepareNotificationMessage(session, mailAddress, senderName, mailAddress, subject, body, null, false, "j-lawyer.org Server")` — test mail stays simple plain-text; `mailAddress` is used both as From and recipient (mirrors the current behavior)
- [ ] 5.2 Verify by clicking "Einstellungen testen" in `SystemMailboxDialog` and inspecting raw headers: `Message-ID`, `From`, `Reply-To`, `Auto-Submitted`, `X-Mailer`, UTF-8 subject

## 6. Email template and logo resource (j-lawyer-server-ejb)

- [ ] 6.1 Add `j-lawyer-server/j-lawyer-server-ejb/src/java/templates/email/logo.png` — the same logo currently inlined as Base64 in `template.html` (extract once, commit the PNG)
- [ ] 6.2 In `template.html`: replace the `<img src="data:image/png;base64,...">` with `<img src="cid:logo" alt="j-lawyer.org" width="100" height="100" style="display:block;border:0;">`
- [ ] 6.3 Remove the empty `<h2></h2>` and `<h3></h3>` placeholders; add `<title>j-lawyer.org Benachrichtigung</title>` inside `<head>`
- [ ] 6.4 Confirm the EJB build packages `logo.png` into the JAR alongside `template.html` (both live under `src/java/templates/email/`; verify by listing JAR contents after a build)

## 7. Verification

- [ ] 7.1 Trigger a mention notification with the existing `smtpuser` configuration. Raw headers of the received mail must contain `Message-ID` with the domain of `smtpuser` (or the server hostname if `smtpuser` has no `@`, accompanied by a WARN), plus `Reply-To`, `Auto-Submitted: auto-generated`, `X-Mailer: j-lawyer.org Server`, `Content-Type: multipart/alternative` (or `multipart/related` enclosing it) with both a `text/plain; charset=UTF-8` and a `text/html; charset=UTF-8` part
- [ ] 7.2 Send a test through "Einstellungen testen" — verify the same header set on the received plain-text test mail
- [ ] 7.3 Send a sample mention notification to a fresh `mail-tester.com` address; confirm the score improves substantially over the baseline (>= 7/10 even with `smtpuser` as From; reaching 10/10 typically requires the optional follow-up change plus proper SPF/DKIM/DMARC)
- [ ] 7.4 With `logo.png` removed from the deployed EJB JAR: trigger a mention; the mail must still send and arrive (without the inline image, no exception, single INFO log line about the missing resource)
- [ ] 7.5 Run `openspec validate improve-server-notification-mail-deliverability --strict`
