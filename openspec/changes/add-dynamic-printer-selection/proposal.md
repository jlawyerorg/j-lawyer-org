# Change: Add dynamic printer selection to direct document printing

## Why

Direct printing from an archive file currently always uses the operating system's
default printer. This makes occasional printing to another target, such as a fax
printer, unnecessarily cumbersome because the document has to be opened in another
application first.

Showing every printer installed on the client directly in the document context menu
would solve the selection problem but would make the menu unwieldy on systems that have
accumulated many unused or location-specific printers. The proposed change therefore
combines live discovery of the printers currently reported by the operating system with
an optional, device-local quick-access list and an on-demand chooser for all other
currently available printers.

## What Changes

- Preserve the existing one-click action `drucken (Standarddrucker)` and its current
  behaviour.
- Discover available printers from the client operating system whenever the document
  context menu or printer settings are opened; no printer object, driver configuration,
  or assumed availability is persisted.
- Add direct context-menu entries for user-selected quick-access printers that are
  currently available. Quick-access printers that are not currently available are not
  shown in the document context menu.
- Add `drucken (anderen Drucker auswählen …)` to open a compact chooser containing all
  printers currently reported by the operating system.
- Add a client setting for managing the quick-access list. The setting stores only the
  selected printer names on the local client so that office and home installations can
  have different lists; it is not stored on or synchronized by the server.
- Print every selected supported document to the explicitly chosen printer. If that
  printer cannot be resolved at print time, show an error and do not silently fall back
  to the default printer.
- Keep the existing handling of supported and unsupported document types and extend both
  the PDF and LibreOffice print paths to accept an explicit printer target.

## Impact

- **Affected specs:** `document-printing` (new capability)
- **Affected issues:**
  - Resolves the direct-printer-selection use case in #1227.
  - Enables the fax-printer workflow described in #1170 through normal direct printing,
    but does not change the separate `als Fax senden` action and therefore does not by
    itself fully resolve #1170.
  - Does not implement printer properties or a j-lawyer-specific default printer from
    #413; those remain out of scope.
- **Likely affected client code:**
  - `j-lawyer-client/src/main/java/com/jdimension/jlawyer/client/editors/files/ArchiveFilePanel.java`
    and `ArchiveFilePanel.form`
  - `j-lawyer-client/src/main/java/com/jdimension/jlawyer/client/launcher/LauncherFactory.java`
  - `j-lawyer-client/src/main/java/com/jdimension/jlawyer/client/settings/ClientSettings.java`
  - `j-lawyer-client/src/main/java/com/jdimension/jlawyer/client/JKanzleiGUI.java` and
    `JKanzleiGUI.form`
  - a small printer quick-access settings dialog (`.java` and matching `.form`)
- **No server, database, EJB, or REST API changes.**
- **Backward compatibility:** existing direct printing to the operating-system default
  remains available and unchanged. With no quick-access configuration, no printer-
  specific direct entries are shown.
- **Approval point:** the maintainer should explicitly confirm whether persisting only
  device-local quick-access printer names is acceptable. Actual printer availability is
  always determined live, as requested.

