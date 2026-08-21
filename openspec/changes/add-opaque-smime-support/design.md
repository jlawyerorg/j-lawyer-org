## Context

S/MIME has three structural forms on the wire:

1. **Clear-signed** (`multipart/signed; protocol="application/pkcs7-signature"`) — original MIME parts are sent in cleartext alongside a detached `smime.p7s` signature. Already supported (commit `3494dcf68`) — though only incidentally: that commit merely taught the walkers to descend into an inline part whose content is a `Multipart`. Nothing in the codebase inspects `multipart/signed` or the `protocol=` parameter.
2. **Opaque-signed** (`application/pkcs7-mime; smime-type=signed-data`) — entire signed MIME content packed into a PKCS#7 SignedData blob, delivered as a single `smime.p7m` body. Not currently supported.
3. **Encrypted** (`application/pkcs7-mime; smime-type=enveloped-data`) — content encrypted with recipient certificate; requires private key to decrypt. Out of scope.

Independent MIME-walker code paths exist in the codebase, none of which traverse opaque-signed envelopes:

- `j-lawyer-server-common/src/main/java/com/jdimension/jlawyer/email/CommonMailUtils.java` — `getAttachmentInfo` (1006), `getAttachmentNames` (993, delegates), `getAttachmentBytes` (1068), `getAttachmentPart` (1088), `recursiveFindPart` (1183). Used by `MailboxScannerTask` (background mailbox archival) and, by inheritance, by the client.
- `j-lawyer-server/j-lawyer-server-ejb/src/main/java/com/jdimension/jlawyer/services/EmailService.java` — `extractAttachments` (2158), `extractAttachmentsMetadata` (2268, a near-verbatim clone), `extractBody` (3567) and `hasAttachments` (3534, the paperclip indicator). This is the path used by server-based mailboxes in the client *and* by the REST API.
- `j-lawyer-client/src/main/java/com/jdimension/jlawyer/client/mail/EmailUtils.java` — `EmailUtils extends CommonMailUtils` (710), so `getAttachmentNames`, `getAttachmentInfo` and `recursiveFindPart` are **inherited, not redefined**. Only `getAttachmentPart(String, Object, Folder)` (954), `getAttachmentBytes(String, MessageContainer)` (1065), `checkForAttachments` (1104), `hasAttachment` (833) and `removeAttachmentsFromMessage` (1796) are client-specific.
- `j-lawyer-server/j-lawyer-server-war/src/main/java/com/jdimension/jlawyer/timer/MailboxScannerTask.java` — holds no walker logic of its own but calls `CommonMailUtils` at 1337/1356 (body) and 1642/1662 (attachments), and guards the body block with `isMimeType("multipart/*")` at 1335.
- Exchange/Graph mailboxes bypass JavaMail entirely: `EmailService.graphGetMessage` (2867), `graphGetAttachments` (2919), `graphGetAttachmentsMetadata` (2949) read attachment lists from the Graph REST API.

## Goals / Non-Goals

- **Goal**: Make opaque-signed S/MIME messages behave indistinguishably from unsigned messages from the user's perspective (body visible, attachments enumerated and savable) on every access path: background scanner, client inbox (server-based and legacy direct-IMAP), archived `.eml` in the viewer, REST API, and Exchange/Graph.
- **Goal**: Single helper, used identically everywhere, so future walker additions inherit the fix.
- **Goal**: Graceful degradation — if BC parsing fails or the blob is malformed, fall back to current behavior, never throw.
- **Non-Goal**: Decrypt enveloped-data S/MIME — that requires per-mailbox private key storage, certificate management UI, and keystore infrastructure (separate, much larger change).
- **Non-Goal**: Verify the signature — we trust the transport and only extract the signed payload. Cryptographic verification would require certificate-chain validation infrastructure.
- **Non-Goal**: Retroactively repair mails that were already archived. Their case documents stay as they are; the attachments become reachable by opening the archived `.eml` in the client viewer.

## Decisions

### Decision: Unwrap at the call site, not only at the walker entry point

The obvious design — call `unwrapSMIME(part)` on the first line of each walker — **does not work** for the `CommonMailUtils` walkers, and would leave the very path the ticket is about (background auto-import) broken.

Those walkers are not called with the `Message`; they are called with `msg.getContent()`. JavaMail has no `DataContentHandler` for `application/pkcs7-mime`, so for an opaque-signed mail `getContent()` returns a raw `InputStream`. That value is neither a `Multipart` nor a `Part`, so:

- `getAttachmentInfo(Object)` (1006) falls through both branches → **empty attachment list**
- `getAttachmentPart(String, Object)` (1106) casts `(Part) partObject` unconditionally → **ClassCastException**
- `recursiveFindPart(Object, …)` (1192) does the same → **ClassCastException**

By the time control reaches the walker, the type information needed for the unwrap decision is gone. The fix therefore introduces a second helper that replaces the `getContent()` calls themselves:

```java
/** Content to hand to a walker, transparently unwrapping opaque-signed S/MIME. */
public static Object unwrapContent(Part part) throws Exception {
    Part inner = unwrapSMIME(part);
    return (inner != null) ? inner : part.getContent();
}
```

Returning the inner **`Part`** (not `inner.getContent()`) is deliberate: the walkers already handle a `Part` whose disposition is `null` by fetching its content via `safeGetContent`, and a `Part` never triggers the unconditional casts above — whereas an inner content of `String` (a signed plain-text mail without attachments) would.

Call sites become `walker(CommonMailUtils.unwrapContent(msg))` instead of `walker(msg.getContent())`. For every non-S/MIME message the returned value is exactly what `getContent()` returned before, so the change is behaviour-preserving.

Walkers that already receive the `Message` as a `Part` — all four in `EmailService`, and the client's folder-aware `getAttachmentPart` — get the simple entry-point unwrap instead.

**Alternative considered**: normalise the message once at fetch time into a new `MimeMessage` merging the outer envelope headers with the inner content. Rejected — there is no single fetch choke point (IMAP fetch in `EmailService`, `MailboxScannerTask`, client folder listing), and the `.eml` written to the case must remain the unmodified original.

### Decision: Detect via the CMS ContentInfo OID, not only the MIME parameter

Senders are inconsistent: some emit `application/x-pkcs7-mime`, some omit the `smime-type` parameter entirely. Use the MIME type only as a cheap pre-filter (`isMimeType("application/pkcs7-mime")` or `"application/x-pkcs7-mime"`) and take the actual decision from the CMS ContentInfo content-type OID:

- `1.2.840.113549.1.7.2` — signedData → unwrap
- `1.2.840.113549.1.7.3` — envelopedData → WARN, return `null`

`CMSSignedData.getSignedContent()` returns `null` for a detached signature — null-check before writing, and return `null` in that case.

### Decision: BouncyCastle 1.78 via Maven Central coordinates

Use `org.bouncycastle:bcprov-jdk18on`, `bcpkix-jdk18on`, `bcutil-jdk18on` at **1.78**. That is the version the client already resolves transitively through the iText 9.0.0 BouncyCastle adapters; pinning the proposal's original 1.74 would conflict with or downgrade iText's BC.

Following the project convention established by `migrate-dependencies-to-central`: hardcode the version once in the root `pom.xml` `<dependencyManagement>` (the project uses no version properties) and declare version-less dependencies in the module poms. Scopes differ per module — see tasks.

Do **not** use `bcmail-jdk18on` — its `SMIMESignedParser` pulls in JavaMail integration we do not need; extract the signed bytes via `CMSSignedData.getSignedContent().write(out)` and re-parse as `MimeBodyPart` using the JavaMail API already in use. That path needs **no JCE provider registration**, which matters for the split-package risk below.

**Alternatives considered**:
- *WildFly's `org.bouncycastle.main` module* via `jboss-deployment-structure.xml`: rejected — version drift risk (depends on WildFly install), and only the provider is exported, not `bcpkix` (which contains `CMSSignedData`).
- *Java standard library `sun.security.pkcs.PKCS7`*: rejected — internal API, not portable across JDK vendors.
- *Hand-rolled ASN.1 DER walk to avoid the new dependency*: rejected — ~100 lines of security-sensitive parsing to avoid a library the client already ships.

### Decision: Exchange/Graph handled explicitly, not implicitly

Graph mailboxes never construct a JavaMail `Part` when reading, so no amount of MIME-walker work reaches them. `graphGetAttachments` / `graphGetAttachmentsMetadata` detect the single `smime.p7m` attachment in the Graph response and run its `contentBytes` through `unwrapSMIME` (wrapped in a `MimeBodyPart`), returning the inner attachments; `graphGetMessage` does the same for the body. Same helper, different entry.

### Decision: Encrypted S/MIME: detect, log, do nothing

When `unwrapSMIME` encounters envelopedData, log a single WARN line per message (with subject and sender) and return `null`. The caller falls back to the existing path, which surfaces the `smime.p7m` as an attachment — exactly the current behavior, just with a diagnostic log entry. No UI change in this proposal.

## Risks / Trade-offs

- **Risk**: Split-package collision on the client classpath. `j-lawyer-client/target/lib/` currently contains **both** `bcpkix-jdk18on-1.78.jar` and `bcpkix-fips-2.0.7.jar`, and both ship `org/bouncycastle/cms/CMSSignedData.class` (they arrive via `com.itextpdf:bouncy-castle-adapter` and `com.itextpdf:bouncy-castle-fips-adapter`, both declared in `j-lawyer-client/pom.xml`). Which one wins depends on manifest Class-Path order.
  - **Mitigation**: the unwrap uses only `new CMSSignedData(InputStream)` + `getSignedContent()`, which needs no JCE provider and exists identically in both variants. A verification step confirms the unwrap works in the built client. Removing the (probably unused) `bouncy-castle-fips-adapter` would clean this up but belongs in a separate change.
- **Risk**: BouncyCastle classloader conflicts in WildFly if a system-level BC module exists.
  - **Mitigation**: the jars are bundled in `EAR/lib/` by `maven-ear-plugin` (`defaultLibBundleDir`, `skinnyModules`); if conflicts surface, exclude `org.bouncycastle.*` in `jboss-deployment-structure.xml`.
- **Risk**: Repeated parsing — `getAttachmentNames` followed by one `getAttachmentBytes` per attachment re-parses the PKCS#7 blob each time.
  - **Mitigation**: accepted. The blob is already fully in memory in these paths; no asymptotic change. Revisit only if profiling shows it.
- **Risk**: Malformed `smime.p7m` blobs from broken senders could crash the walker.
  - **Mitigation**: `unwrapSMIME` catches all exceptions, logs WARN, returns `null`. The caller continues with the original content — degrades to today's behavior.
- **Risk**: The customer's mails might actually be `enveloped-data`, in which case nothing improves.
  - **Mitigation**: a diagnostic log line reporting content type and CMS OID makes this verifiable in the field before drawing conclusions from a failed rollout.
- **Trade-off**: We do not verify signatures. A user opening an opaque-signed mail sees content that *appears* to be the original without a cryptographic guarantee. This matches current behavior for clear-signed and what most mail clients display by default.

## Migration Plan

1. Add BC 1.78 to the root `pom.xml` `<dependencyManagement>`; add version-less dependencies to `j-lawyer-server-common` (compile), `j-lawyer-server-ejb` (`provided`, matching that module's convention) and `j-lawyer-server-ear` (unscoped, so they land in `EAR/lib/`). The client needs no entry — it inherits BC transitively.
2. Implement `unwrapSMIME` and `unwrapContent`.
3. Replace the `getContent()` call sites; add entry-point unwraps to the `Part`-receiving walkers; handle the Graph path.
4. Cover with unit tests in `j-lawyer-server-common/src/test/java` (junit is already a test dependency there).
5. Verify with one opaque-signed and one clear-signed test mail (regression).

No data migration. No schema changes. Rollback: revert the commit; the BC jars can stay on the classpath without harm.

## Open Questions

- Should we expose a UI hint for "this mail is signed (unverified)" — analogous to the existing HTML warning for mails from unknown senders? **Defer** — not in scope for this change.
- Are the mails reported in #3387 actually opaque-signed, or enveloped-data? No sample `.eml` is available; the change assumes opaque-signed (the Outlook default) and ships the diagnostic log line to confirm it in the field.
- Should `com.itextpdf:bouncy-castle-fips-adapter` be dropped from the client to remove the duplicate `org.bouncycastle.cms` package? **Out of scope** — track separately.
