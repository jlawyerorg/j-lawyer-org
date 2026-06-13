## Context

`improve-server-notification-mail-deliverability` introduced `ServerMailUtils.prepareNotificationMessage(session, fromAddress, ...)` and switched all three server-side notification send paths (`OutgoingMailProcessor.sendMail`, `SystemManagement.statusMail`, `SystemMonitorTask.statusMail`) to call it with `smtpuser` as `fromAddress`. That fix handles charset, MIME structure, headers, and Message-ID, but it cannot improve DMARC alignment when `smtpuser` is not a deliverable email address on the operator's domain.

This change adds the missing knob: a separate operator-configurable From-address. It is intentionally split out so the deliverability core can be shipped without scheduling UI work.

## Goals / Non-Goals

**Goals**
- Allow operators to align the visible `From:` with the relay's SPF/DKIM/DMARC domain without changing the SMTP authentication credentials.
- Fix the long-standing label ambiguity in `SystemMailboxDialog` where the SMTP authentication user is incorrectly labeled "Absenderadresse".
- Preserve full backward compatibility: empty setting = no behavior change.

**Non-Goals**
- The deliverability fixes themselves — already done in `improve-server-notification-mail-deliverability`.
- DNS / DKIM / SPF / DMARC setup on the operator side.
- Migration of existing operator configurations: empty new setting is the correct default for upgrades.

## Decisions

### Decision: Resolution rule `smtpfrom OR smtpuser`

A single, easy-to-explain rule:

```java
String fromAddress = (smtpFromS != null && smtpFromS.getSettingValue() != null && !smtpFromS.getSettingValue().trim().isEmpty())
                     ? smtpFromS.getSettingValue().trim()
                     : smtpUser;
```

Applied identically in all three server call sites. The resolved `fromAddress` is what gets passed to `ServerMailUtils.prepareNotificationMessage`.

**Alternatives considered**
- *Always derive a From address heuristically from the SMTP host.* Rejected: unreliable across providers, hides the operator's intent, can produce addresses the operator does not own.
- *Hard-code `noreply@<smtphost-domain>`.* Rejected: relays often refuse mails whose `From:` is not whitelisted on the account.

### Decision: Only override `mail.smtp.from` when `smtpfrom` is explicitly set

The SMTP envelope sender (`mail.smtp.from` JavaMail property) is set to `fromAddress` only when `smtpfrom` was explicitly configured. When the From falls back to `smtpuser`, the envelope is left to JavaMail's default (which mirrors the From-header). This avoids regressing relays that disallow envelope-overriding for the authenticated user.

### Decision: Settings dialog gets a new field, existing label is corrected

A new labeled text field "Absenderadresse (From):" appears above the existing "Absendername" field. The existing `jLabel7` ("Absenderadresse:") on the SMTP-user field is relabeled to "Anmeldename (SMTP-Benutzer):" so the two roles are visually distinct. The `.form` file is updated in lockstep per project convention.

The test-mail handler `cmdTestMailActionPerformed` chooses the From in this order:
1. The new "Absenderadresse (From)" field if non-empty.
2. Today's fallback (the recipient input `txtRecipient.getText()`, preserved verbatim).

This minimises behavior change for operators who do not configure the new field.

**Alternatives considered**
- *Make the field mandatory.* Rejected: would break upgrades for operators who currently rely on the empty state.
- *Auto-derive a sensible default from `smtpuser`.* Rejected: heuristics introduce more confusion than they save; we prefer an explicit, optional field with a tooltip.

## Risks / Trade-offs

- **Risk:** Setting `mail.smtp.from` (envelope) different from the auth user can be rejected by strict relays. **Mitigation:** Only set the property when `smtpfrom` is explicitly configured.
- **Risk:** Operators who misconfigure `smtpfrom` to a domain they don't own may make deliverability worse (DMARC rejects with no fallback). **Mitigation:** Tooltip on the field explicitly states "Sollte eine gültige E-Mail-Adresse mit passenden SPF-/DKIM-Records auf der Domain sein." Test-mail button gives immediate feedback.
- **Risk:** Without `improve-server-notification-mail-deliverability` already applied, the wiring tasks need more code edits. **Mitigation:** Apply the main change first (this change's `## Impact` calls it out as a prerequisite).

## Migration Plan

1. Add the setting key constant in server-common.
2. Update the three server call sites to read the new setting and resolve the From.
3. Add the client-side constant and the UI field; relabel the existing label.
4. Update the test-mail handler.
5. Communicate the new field in release notes — empty value remains the safe default.

Rollback: remove the four edits (constant + three call-site reads + UI). The setting in the database becomes dormant data.

## Open Questions

- None.
