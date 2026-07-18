## Context
j-lawyer currently launches LibreOffice/OpenOffice documents as external files and monitors LibreOffice's `.~lock.<file>#` sidecar file. A prior workaround for #232 intentionally waits after the lock disappears because some LibreOffice versions temporarily remove and recreate that file while the document is still open or saving.

Java UNO testing on this workstation showed LibreOffice can emit document modify, `OnSaveDone`, close query, close notify, view closed, and unload events when j-lawyer starts LibreOffice with a UNO accept socket and loads the document through UNO. A normal LibreOffice process started without `--accept=...` exposes no socket to attach to.

## Goals / Non-Goals
- Goals:
  - Reduce close-detection latency for LibreOffice documents opened by j-lawyer.
  - Preserve #232 data-loss protection for lock-file-only monitoring.
  - Keep behavior functional when UNO Java support is missing.
- Non-Goals:
  - Attach to arbitrary already-running LibreOffice instances not started with a UNO accept socket.
  - Replace all office launchers in one step.
  - Remove lock-file monitoring.

## Decisions
- Decision: Use hybrid confirmation.
  - If UNO close/unload has been observed and the lock file is gone, close immediately.
  - If the lock file is gone without UNO close/unload confirmation, use the existing grace-period fallback.
  - If UNO close/unload is observed while the lock file still exists, wait for lock removal without adding the grace period afterward.
- Decision: Make UNO optional.
  - The launcher attempts UNO only when Java UNO classes and a usable LibreOffice binary are available.
  - UNO discovery must support both legacy split jars such as `juh.jar` / `unoil.jar` and modern `libreoffice.jar` packaging.
  - Any UNO startup, connection, listener, or bridge failure falls back to the current process/lock-file monitoring path.
- Decision: Scope UNO session lifetime to the observed document.
  - The observed document keeps strong references to the UNO bridge objects, classloader, process handle, component, and listener proxies while monitoring is active.
  - The observer releases the UNO classloader when it processes the document as closed.
  - Normal close cleanup does not destroy the LibreOffice process, because without an isolated user profile it may be the user's shared LibreOffice instance.
- Decision: Keep lock-file writes authoritative for disk release.
  - UNO close events prove user/document lifecycle, while lock-file disappearance proves LibreOffice has released the file for safe import.

## Risks / Trade-offs
- Java UNO packaging differs by OS and LibreOffice installation.
  - Mitigation: discovery plus fallback to current launcher.
- Starting LibreOffice with a UNO socket changes launcher internals.
  - Mitigation: keep the change scoped to LibreOffice/OpenOffice launcher paths and preserve existing process launch fallback.
- Existing LibreOffice instances may ignore the requested accept socket.
  - Mitigation: if the launched process exits before the UNO socket accepts connections, fail fast and use the existing lock-file launcher path.
- UNO bridge disconnects or LibreOffice crashes can leave incomplete event state.
  - Mitigation: treat disconnect/crash as no UNO confirmation and use lock-file fallback.

## Migration Plan
1. Add UNO monitor classes behind optional runtime detection.
2. Wire LibreOffice launcher to use UNO loading when available.
3. Preserve and test lock-file fallback.
4. Consider packaging Java UNO jars or installer dependency changes after confirming target-platform availability.

## Open Questions
- Should the client bundle LibreOffice Java UNO jars or require the installed LibreOffice Java support package?
- Should the UNO accept socket use localhost TCP with a random port or a named pipe where supported?
