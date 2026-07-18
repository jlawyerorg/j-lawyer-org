# Change: Add UNO-backed LibreOffice document monitoring

## Why
LibreOffice documents can remain marked as open for several seconds after the user closes them because j-lawyer must debounce lock-file disappearance to avoid the historic data-loss bug #232. Java UNO can provide semantic save and close events for LibreOffice documents opened by j-lawyer, allowing near-immediate close detection when those events agree with lock-file release.

## What Changes
- Add optional Java UNO monitoring for LibreOffice/OpenOffice documents launched by the desktop client.
- Treat a document as closed immediately when both a UNO close/unload event and LibreOffice lock-file removal have been observed.
- Preserve the existing lock-file grace period when UNO is unavailable, fails, disconnects, or has not confirmed close.
- Keep current lock-file monitoring as the compatibility fallback.
- Add focused tests/harness coverage for UNO event handling and lock-file fallback behavior.

## Impact
- Affected specs: document-monitoring
- Affected code:
  - `j-lawyer-client/src/com/jdimension/jlawyer/client/launcher/*Office*`
  - `j-lawyer-client/src/com/jdimension/jlawyer/client/launcher/ObservedOfficeDocument.java`
  - `j-lawyer-client/src/com/jdimension/jlawyer/client/editors/files/WaitForDocumentAction.java`
- External dependency consideration: Java UNO runtime jars (`juh`, `jurt`, `ridl`, `unoil`, `java_uno`/LibreOffice Java support) must be discovered or bundled/declared for the client runtime; fallback remains available when absent.
