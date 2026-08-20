## 1. Setting key constants

- [ ] 1.1 Add constant `SERVERCONF_MONITOR_SMTPFROM = "jlawyer.server.monitor.smtpfrom"` to `j-lawyer-server-common/src/com/jdimension/jlawyer/server/services/settings/ServerSettingsKeys.java`
- [ ] 1.2 Add the same constant to `j-lawyer-client/src/com/jdimension/jlawyer/client/settings/ServerSettings.java` (mirroring the server-common constant)

## 2. OutgoingMailProcessor wiring (j-lawyer-server-ejb)

- [ ] 2.1 In `OutgoingMailProcessor.sendMail(...)`: read the new setting alongside the other ones: `ServerSettingsBean smtpFromS = this.settingsFacade.find(ServerSettingsKeys.SERVERCONF_MONITOR_SMTPFROM);`
- [ ] 2.2 Compute the resolved address: `String fromAddress = (smtpFromS != null && smtpFromS.getSettingValue() != null && !smtpFromS.getSettingValue().trim().isEmpty()) ? smtpFromS.getSettingValue().trim() : smtpUser;`
- [ ] 2.3 Replace `props.put("mail.from", smtpUser)` with `props.put("mail.from", fromAddress)`
- [ ] 2.4 If `smtpFromS` was explicitly configured (the truthy branch above), also set `props.put("mail.smtp.from", fromAddress)`
- [ ] 2.5 Change the existing `ServerMailUtils.prepareNotificationMessage(...)` call to pass `fromAddress` instead of `smtpUser`

## 3. SystemManagement.statusMail wiring (j-lawyer-server-ejb)

- [ ] 3.1 Apply the same four-line edit as task 2 (read setting, resolve `fromAddress`, override `mail.from` and conditionally `mail.smtp.from`, pass `fromAddress` to the helper) to `SystemManagement.statusMail(String, String)`

## 4. SystemMonitorTask.statusMail wiring (j-lawyer-server-war)

- [ ] 4.1 Apply the same four-line edit to `SystemMonitorTask.statusMail(ServerSettingsBeanFacadeLocal, String, String)`

## 5. Client UI: new From-address field (j-lawyer-client)

- [ ] 5.1 In `SystemMailboxDialog.java` + `.form`: add `JTextField txtFromAddress` and `JLabel jLabelFromAddress` (label "Absenderadresse (From):"). Position above the existing "Absendername:" row. Both `.java` and `.form` must stay in sync per project convention.
- [ ] 5.2 In the same form: rename the existing label `jLabel7` from "Absenderadresse:" to "Anmeldename (SMTP-Benutzer):"
- [ ] 5.3 On dialog load: `txtFromAddress.setText(set.getSetting(ServerSettings.SERVERCONF_MONITOR_SMTPFROM, ""));`
- [ ] 5.4 On `cmdSaveActionPerformed`: `set.setSetting(ServerSettings.SERVERCONF_MONITOR_SMTPFROM, this.txtFromAddress.getText().trim());`
- [ ] 5.5 In `cmdTestMailActionPerformed`: pick `String from = this.txtFromAddress.getText().trim(); if (from.isEmpty()) from = this.txtRecipient.getText().trim();` and pass `from` (not `txtRecipient`) as the `mailAddress` argument to `testSendMail`
- [ ] 5.6 Add tooltip on `txtFromAddress`: "Optional. Wenn leer, wird der SMTP-Benutzer als Absender genutzt. Sollte eine gültige E-Mail-Adresse mit passenden SPF-/DKIM-Records auf der Domain sein."

## 6. Verification

- [ ] 6.1 With `smtpfrom` empty: trigger a mention notification. Raw headers must be identical to those produced by the main change (`From:` = `smtpuser`, Message-ID right-hand side derived from `smtpuser`-domain).
- [ ] 6.2 With `smtpfrom` configured to a DNS-aligned address (e.g. `noreply@kanzlei.example`): trigger a mention notification. Raw headers must show `From:` = `noreply@kanzlei.example`, `Message-ID:` right-hand side = `kanzlei.example`, `Reply-To:` = `noreply@kanzlei.example`. The SMTP envelope sender (visible in `Received:` headers or by checking the relay's logs) must also be `noreply@kanzlei.example`.
- [ ] 6.3 Configure `smtpfrom` to a domain with proper SPF/DKIM/DMARC and send a sample mention to `mail-tester.com`. Confirm a score >= 9/10.
- [ ] 6.4 Send a sample mention to a Gmail account. In "Show original" verify SPF + DKIM + DMARC "PASS" and the mail is in Inbox.
- [ ] 6.5 In `SystemMailboxDialog`: verify the new field is present above "Absendername", the previously-mislabeled "Absenderadresse:" field is now labeled "Anmeldename (SMTP-Benutzer):", and the test-mail button respects the new field.
- [ ] 6.6 Run `openspec validate add-configurable-notification-from-address --strict`
