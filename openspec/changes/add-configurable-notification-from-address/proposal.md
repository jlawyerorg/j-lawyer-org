# Change: Add a configurable From-address for server notification mails

## Why

`improve-server-notification-mail-deliverability` fixes the most damaging deliverability problems of server-originated notification mails (Message-ID, charset, alternative parts, CID logo, Reply-To / Auto-Submitted / X-Mailer headers). One root cause it explicitly leaves untouched is the `From:` header being forced to the SMTP authentication user (`jlawyer.server.monitor.smtpuser`). For many providers the SMTP login name is not a deliverable email address (e.g. `user1234`, numeric account IDs) or lives on a different domain than the one the relay is allowed to send from. The result is a malformed `From:` header or a DMARC-misaligned mail.

This optional follow-up introduces a separate, operator-configurable From-address so the visible sender can be aligned with the relay's SPF / DKIM / DMARC domain. It is scoped purely as an additive setting plus the UI to surface it — no behavior change for operators who leave the new setting empty.

This change is independent from the main deliverability work and can be applied at any later time without coordination.

## What Changes

- **New server setting** `jlawyer.server.monitor.smtpfrom` (constant `ServerSettingsKeys.SERVERCONF_MONITOR_SMTPFROM`) — operator-configurable From-address, decoupled from `smtpuser`. Empty value preserves current behavior (From-address = `smtpuser`).
- **Three server call sites updated** to resolve the From-address as `smtpfrom.isEmpty() ? smtpuser : smtpfrom` and pass it to `ServerMailUtils.prepareNotificationMessage` instead of the hard-coded `smtpuser`:
  - `OutgoingMailProcessor.sendMail`
  - `SystemManagement.statusMail`
  - `SystemMonitorTask.statusMail`
- **SMTP envelope sender** (`mail.smtp.from` property) set to the resolved From-address only when `smtpfrom` is explicitly configured (avoids regressions on strict relays).
- **Client UI** (`SystemMailboxDialog`): new "Absenderadresse (From):" text field for the new setting; the existing `txtUser` label is relabeled from "Absenderadresse:" to "Anmeldename (SMTP-Benutzer):" to remove the long-standing ambiguity. The test-mail handler (`cmdTestMailActionPerformed`) uses the new field as the test sender if filled, otherwise the current fallback (the recipient input).

## Impact

- **Affected specs:** `server-notification-mail` (additive — adds two new requirements; does not modify any requirement introduced by `improve-server-notification-mail-deliverability`).
- **Affected code:**
  - `j-lawyer-server-common/src/com/jdimension/jlawyer/server/services/settings/ServerSettingsKeys.java` (new constant)
  - `j-lawyer-server/j-lawyer-server-ejb/src/java/org/jlawyer/async/OutgoingMailProcessor.java` (read setting, resolve From, pass to helper)
  - `j-lawyer-server/j-lawyer-server-ejb/src/java/com/jdimension/jlawyer/services/SystemManagement.java` (same in `statusMail`)
  - `j-lawyer-server/j-lawyer-server-war/src/java/com/jdimension/jlawyer/timer/SystemMonitorTask.java` (same in private `statusMail`)
  - `j-lawyer-client/src/com/jdimension/jlawyer/client/settings/ServerSettings.java` (new constant)
  - `j-lawyer-client/src/com/jdimension/jlawyer/client/configuration/SystemMailboxDialog.java` + `.form` (new field, label correction, test-mail handler update)
- **Backward compatibility:** empty `smtpfrom` produces bit-for-bit identical behavior to the state after `improve-server-notification-mail-deliverability` is applied.
- **Prerequisite:** `improve-server-notification-mail-deliverability` should be archived first so that `ServerMailUtils.prepareNotificationMessage` exists and the three call sites already delegate to it. If applied before, the wiring tasks 2.x / 3.x / 4.x in this change become structural refactors instead of one-line changes.
- **No DB schema changes**, no REST API changes.
