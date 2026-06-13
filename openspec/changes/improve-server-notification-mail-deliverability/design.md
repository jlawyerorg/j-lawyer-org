## Context

The server-side notification send path exists in three places that share almost identical SMTP setup, authentication, and `MimeMessage` construction code:

- `OutgoingMailProcessor.sendMail(...)` — the MDB that consumes `OutgoingMailRequest` from the JMS queue; used for all event-triggered notifications, in particular the mention-in-message mail (`MessagingService.publishOutgoingMailRequest`, lines 789–803 / 1093–1107 of `MessagingService.java`).
- `SystemManagement.statusMail(String subject, String body)` — the EJB facade used by callers like `IterativeBackupTask` to report job results.
- `SystemMonitorTask.statusMail(ServerSettingsBeanFacadeLocal, String, String)` — a private duplicate inside the WAR module, invoked from the monitoring scheduler.

All three use the system-wide SMTP credentials stored under `jlawyer.server.monitor.smtp*`. None of them participate in the per-user mailbox flow (`EmailService.smtpSendMail`), which is already correct and out of scope.

The user-mailbox path lives in `j-lawyer-server-ejb`. The server-common module is on the classpath of all three call sites and already hosts shared mail utilities (`CommonMailUtils`). Putting the new helper there keeps the dependency direction clean.

## Goals / Non-Goals

**Goals**
- Stop the regular spam-folder / reject behavior of server-originated notification mails.
- Eliminate duplicated SMTP code across the three call sites by centralizing message preparation in one helper.
- Keep the fix fully backward-compatible: operators who do nothing after upgrading must still receive mails.

**Non-Goals**
- DNS / DKIM / SPF / DMARC setup for the operator.
- `List-Unsubscribe` / `List-Unsubscribe-Post` headers. Excluded by request.
- A new operator setting to decouple the From-address from the SMTP authentication user — deferred to the optional follow-up `add-configurable-notification-from-address`. That change also owns all client-UI work.
- Any change to per-user mail sending (`EmailService.smtpSendMail`).

## Decisions

### Decision: Centralize message preparation in `ServerMailUtils.prepareNotificationMessage`

One helper builds the `MimeMessage` with all headers, charset, multipart structure, and (optionally) the CID-embedded logo, leaving SMTP transport (`Session`, `Transport`, `Authenticator`) to the caller.

Helper signature:

```java
public static MimeMessage prepareNotificationMessage(
    Session session,
    String fromAddress,        // resolved From, today always smtpuser
    String senderName,         // display name for From
    String to,
    String subject,
    String plainTextBody,      // raw text; required (always)
    String htmlBody,           // already rendered HTML; null = plain-only mail
    boolean embedLogoAsCid,    // attach /templates/email/logo.png as cid:logo if present
    String mailerTag           // value for X-Mailer
) throws Exception;
```

Behavior:

- Generates `Message-ID: <UUID.timestamp@<fromDomain>>` via an anonymous subclass overriding `MimeMessage.updateMessageID()`. This is the only way to guarantee the auto-generated default cannot win during `saveChanges()`.
- Sets `From`, `Reply-To` (= From), `To`, `Subject` (UTF-8), `Sent-Date`.
- Adds `Auto-Submitted: auto-generated` and `X-Mailer: <mailerTag>`.
- Builds the body as `multipart/alternative` of `text/plain; charset=UTF-8` + `text/html; charset=UTF-8` when `htmlBody != null`; otherwise just `text/plain; charset=UTF-8`. If logo embedding is requested and the resource is present, wraps the alternative in `multipart/related` with the PNG as an inline part with `Content-ID: <logo>`.
- Calls `saveChanges()` and returns the message.

The caller is responsible for SMTP `Properties`, opening the `Transport`, and calling `bus.send(msg)`. The caller keeps the existing `props.put("mail.from", smtpUser)` line (no semantic change vs. today).

The signature is stable: the optional follow-up change `add-configurable-notification-from-address` only changes what value the callers pass for `fromAddress`; the helper itself does not change.

**Alternatives considered**
- *Inject as `@EJB`.* Rejected: helper is stateless utility code with no transactional or security concerns; an EJB adds complexity without benefit.
- *Put it directly into `OutgoingMailProcessor` and have the others delegate via the JMS queue.* Rejected: the JMS path is async and unsuitable for status/monitor mails whose timing is observable in operator workflows.

### Decision: Derive Message-ID domain from the From-address (with hostname fallback)

The single strongest spam signal in today's mails is the `Message-ID` right-hand side coming from `InetAddress.getLocalHost()` instead of the From-domain. The helper derives the domain by splitting `fromAddress` on `@` and using the part after the `@`. If `fromAddress` contains no `@` (the current setup at sites where `smtpuser` is a non-email login like `user1234`), the helper falls back to the local hostname and logs a single WARN. This matches today's behavior in the worst case (still better than today because at least we control the Message-ID format and avoid the `JavaMail.<user>@<host>` shape), and a follow-up change can introduce a configurable From to break out of this fallback.

### Decision: CID-embedded logo, falling back to no logo

The current `template.html` embeds the logo as a Base64 `data:` URI. This is itself a moderate spam signal. Switching to a `cid:` inline part is a known-good pattern.

Behavior:
- The PNG previously inlined in the template ships as `j-lawyer-server-ejb/src/java/templates/email/logo.png`.
- `prepareNotificationMessage(..., embedLogoAsCid=true, ...)` loads `/templates/email/logo.png` via `getResourceAsStream`. If the stream is `null`, the helper logs once at INFO and produces the mail without the related-wrap; the `<img src="cid:logo">` in the HTML simply does not render in the receiving client. No exception, no failed send.

**Alternatives considered**
- *Drop the logo entirely.* Less branding but simpler. Available as a fallback if maintaining the PNG resource turns out to be a bother.
- *Host the logo externally and reference an `https://` URL.* Rejected: external image references in mails are themselves a spam signal and create a runtime dependency on a web host.

### Decision: Explicit UTF-8 everywhere, no reliance on JVM default encoding

`msg.setSubject(subject, "UTF-8")` and `bodyPart.setContent(body, "text/html; charset=UTF-8")` / `bodyPart.setText(text, "UTF-8")` are used throughout. The current code's reliance on the JVM default encoding produces mangled umlauts on WildFly instances without `-Dfile.encoding=UTF-8`.

### Decision: Do NOT add `List-Unsubscribe`

Excluded by explicit request. For one-to-one mention notifications and operator status mails the unsubscribe semantics are unclear. The remaining fixes capture the bulk of the deliverability gain.

## Risks / Trade-offs

- **Risk:** Existing `smtpuser` values without `@` continue to produce a Message-ID with the server's local hostname and a malformed `From:` header. **Mitigation:** The WARN log explains it; the follow-up change adds the proper fix.
- **Risk:** Overriding `MimeMessage.updateMessageID()` might silently no-op on certain `javax.mail` versions. **Mitigation:** After `saveChanges()`, read back the `Message-ID` header; if it does not match the intended value, re-set the header and call `saveChanges()` once more.
- **Risk:** Missing logo PNG in older WAR builds during partial deploys. **Mitigation:** Helper skips the related-wrap silently.
- **Risk:** Subtle behavior change in mail clients that previously rendered the HTML-only structure but mis-handle `multipart/alternative`. **Mitigation:** `multipart/alternative` is the standard since RFC 2046; modern clients have handled it correctly for two decades.

## Migration Plan

1. Land server-common changes first: `ServerMailUtils` helper. No behavior change yet.
2. Switch `OutgoingMailProcessor.sendMail` to the helper; ship logo resource and the updated template. Verify by triggering a mention.
3. Switch `SystemManagement.statusMail` to the helper. Verify by triggering a backup job.
4. Switch `SystemMonitorTask.statusMail` (WAR module copy) to the helper.
5. Apply minimal header improvements to `SystemManagement.testSendMail` so the "Einstellungen testen" button verifies the new header set.

Rollback: revert the helper call in each of the three call sites — the surrounding SMTP code paths remain intact; the helper introduction is the only structural change.

## Open Questions

- None blocking. The helper API and header set are locally decidable.
