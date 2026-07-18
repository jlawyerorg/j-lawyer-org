# Change: Normalize unusable LibreOffice document window geometry

## Why
LibreOffice persists per-document-type window geometry in its user profile
(`ooSetupFactoryWindowAttributes`). A known LibreOffice defect can save a tiny or
off-screen "restored" geometry after a maximized window is closed. When j-lawyer then
opens a document, LibreOffice recreates the window with that bad geometry, so the
document appears as a barely-visible sliver or off-screen and effectively invisible.
The user cannot interact with it, and j-lawyer's lock-file detection subsequently reports
the document as already open, breaking the workflow (issue #3483). LibreOffice provides
no CLI flag to override window geometry, so the only profile-preserving, reliably
cross-environment mitigation is to correct the live window through UNO.

## What Changes
- After a UNO-launched LibreOffice document loads, ensure its container window is visible.
- Detect an unusable window (too small, or too little of it intersects the desktop) and
  move/resize it to safe, centered bounds on the active display.
- Apply the correction with a short bounded retry, because LibreOffice restores the bad
  geometry asynchronously after the document loads.
- Keep the behavior non-fatal: any failure to inspect or adjust the window SHALL NOT
  prevent the document from opening or UNO close monitoring from running.
- This mitigation only applies on the UNO launch path; the non-UNO fallback launch is
  unchanged.

## Impact
- Affected specs: document-monitoring
- Affected code:
  - `j-lawyer-client/src/main/java/com/jdimension/jlawyer/client/launcher/JavaUnoOfficeMonitor.java`
- Depends on the UNO launch path introduced by `add-uno-document-monitoring`.
- Upstream: the root cause is a LibreOffice geometry-persistence defect; this change is a
  client-side workaround, not a fix of LibreOffice itself.
