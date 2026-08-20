## Context

S/MIME has three structural forms on the wire:

1. **Clear-signed** (`multipart/signed; protocol="application/pkcs7-signature"`) — original MIME parts are sent in cleartext alongside a detached `smime.p7s` signature. Already supported (commit `3494dcf68`).
2. **Opaque-signed** (`application/pkcs7-mime; smime-type=signed-data`) — entire signed MIME content packed into a PKCS#7 SignedData blob, delivered as a single `smime.p7m` body. Not currently supported.
3. **Encrypted** (`application/pkcs7-mime; smime-type=enveloped-data`) — content encrypted with recipient certificate; requires private key to decrypt. Out of scope.

Three independent MIME-walker code paths exist in the codebase, all of which currently fail to traverse opaque-signed envelopes:

- `j-lawyer-server-common/src/com/jdimension/jlawyer/email/CommonMailUtils.java` — used by `MailboxScannerTask` (background mailbox archival) and shared utilities.
- `j-lawyer-server/j-lawyer-server-ejb/src/java/com/jdimension/jlawyer/services/EmailService.java` — `extractAttachments` (line 1806) and `extractBody` (line 3213); invoked when the client calls `lookupEmailServiceRemote().getAttachments(...)` / `getMessage(...)` for server-based mailboxes (the primary "save to case" path).
- `j-lawyer-client/src/com/jdimension/jlawyer/client/mail/EmailUtils.java` — used by `MailContentUI` body rendering and the legacy direct-IMAP path in `EmailInboxPanel.saveToCaseCallback`.

## Goals / Non-Goals

- **Goal**: Make opaque-signed S/MIME messages behave indistinguishably from unsigned messages from the user's perspective (body visible, attachments enumerated and savable).
- **Goal**: Single helper, used identically by all three walkers, so future walker additions inherit the fix automatically.
- **Goal**: Graceful degradation — if BC parsing fails or the blob is malformed, fall back to current behavior (show `smime.p7m`), never throw.
- **Non-Goal**: Decrypt enveloped-data S/MIME — that requires per-mailbox private key storage, certificate management UI, and keystore infrastructure (separate, much larger change).
- **Non-Goal**: Verify the signature — we trust the transport and only extract the signed payload. Cryptographic verification would require certificate-chain validation infrastructure.

## Decisions

### Decision: BouncyCastle 1.74, bundled per module

Use `bcprov-jdk18on`, `bcpkix-jdk18on`, `bcutil-jdk18on` at version 1.74 (already present in `j-lawyer-client/lib/bea.bak/`). Do **not** use `bcmail-jdk18on` — its `SMIMESignedParser` would pull in JavaMail integration that we don't need; instead, extract signed bytes via `CMSSignedData.getSignedContent().write(out)` and re-parse as `MimeBodyPart` using the JavaMail API already in use.

**Alternatives considered**:
- *WildFly's `org.bouncycastle.main` module* via `jboss-deployment-structure.xml`: rejected — version drift risk (depends on WildFly install), and only the provider is exported, not `bcpkix` (which contains `CMSSignedData`). Bundling per module is consistent with how other libraries (log4j, POI, Tika) are handled.
- *Java standard library `sun.security.pkcs.PKCS7`*: rejected — internal API, not portable across JDK vendors.

### Decision: Shared helper in `CommonMailUtils`, called from all walkers

Place `unwrapSMIME(Part part)` as a public static method in `CommonMailUtils` (which lives in `j-lawyer-server-common`, already on the classpath of both client and EJB). Each walker calls it at its entry point:

```java
Part effectivePart = part;
Part unwrapped = CommonMailUtils.unwrapSMIME(part);
if (unwrapped != null) {
    effectivePart = unwrapped;
}
// existing walker logic continues with effectivePart
```

**Alternative considered**: duplicate the unwrap logic in each module. Rejected — three independent copies guarantee future drift; shared helper is the only sustainable shape.

### Decision: Opaque-signed becomes the top-level Part

Opaque-signed S/MIME has `application/pkcs7-mime` as the top-level content type (no surrounding `Multipart`). Therefore the unwrap check must run **before** any `instanceof Multipart` test, at the very first line of each walker entry point. Existing walkers' Multipart-only entry checks will silently miss it otherwise.

### Decision: Encrypted S/MIME: detect, log, do nothing

When `unwrapSMIME` encounters `smime-type=enveloped-data`, log a single WARN line per message (with subject and sender) and return `null`. Caller falls back to the existing path, which surfaces the `smime.p7m` as an attachment — exactly the current behavior, just with a diagnostic log entry. No UI change in this proposal.

## Risks / Trade-offs

- **Risk**: BouncyCastle classloader conflicts in WildFly if a system-level BC module exists.
  - **Mitigation**: bundle in module-private classpath; if conflicts surface, add `<exclude>` for `org.bouncycastle.*` in `jboss-deployment-structure.xml`.
- **Risk**: Memory / performance impact of reading the full PKCS#7 blob for large mails.
  - **Mitigation**: `CMSSignedData` streams from `InputStream`; we already read full mail content in walkers, so no asymptotic change.
- **Risk**: Malformed `smime.p7m` blobs from broken senders could crash the walker.
  - **Mitigation**: `unwrapSMIME` catches all exceptions, logs WARN, returns `null`. Walker continues with original part — degrades to today's behavior.
- **Trade-off**: We do not verify signatures. A user opening an opaque-signed mail will see content that *appears* to be the original, but we have no cryptographic guarantee. This matches current behavior for clear-signed (which also is not verified) and matches what most mail clients display by default.

## Migration Plan

1. User manually adds 3 BC JARs to the `lib/` directory of each of the 3 affected modules and updates each `nbproject/project.properties` (3 `file.reference` entries + 3 `javac.classpath` entries per module). JARs already live in `j-lawyer-client/lib/bea.bak/`.
2. Implement `unwrapSMIME` helper.
3. Wire it into the 3 walker entry points.
4. Verify with one opaque-signed and one clear-signed test mail (regression).

No data migration. No schema changes. Rollback: revert the commit; BC JARs can stay on the classpath without harm.

## Open Questions

- Should we expose a UI hint for "this mail is signed (unverified)" — analogous to how the existing HTML warning is shown for mails from unknown senders? **Defer** — not in scope for this change.
- Should the JAR additions be automated via the build script? **No** — j-lawyer's build is NetBeans-driven and the user prefers manual classpath edits to preserve `.nbproject/project.properties` control.
