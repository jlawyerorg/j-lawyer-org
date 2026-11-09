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
an optional, device-local favourite list that is the only source for additional
one-click menu entries.

## What Changes

- Preserve the existing one-click action `drucken (Standarddrucker)` and its current
  behaviour whenever no currently available non-default favourite printer is configured.
- Maintain a live view of printers reported by the client operating system without
  blocking the archive-file context menu while printer discovery is running; no printer
  object, driver configuration, or assumed availability is persisted.
- Add a `Drucken` submenu only when at least one currently available non-default
  favourite exists. The submenu contains the standard-printer action first and then the
  available favourites.
- Do not add direct menu entries, submenus, or all-printer choosers merely because the
  operating system reports multiple printers.
- Add a client setting for managing favourite printers. The setting stores only the
  selected printer names and optional display labels on the local client so that office
  and home installations can have different lists; it is not stored on or synchronized by
  the server.
- Allow ordinary users to manage their local printer favourites. The settings dialog is
  not restricted to `adminRole` or `sysAdminRole` because it only changes a local client
  preference.
- Show favourite printers in the document context menu by their optional display label
  (for example `Faxdrucker` or `Drucker Empfang`) while resolving the print target by the
  exact live operating-system printer name.
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
  - a small printer favourites settings dialog (`.java` and matching `.form`)
- **No server, database, EJB, or REST API changes.**
- **Backward compatibility:** existing direct printing to the operating-system default
  remains available and unchanged. With no currently available non-default favourites,
  the archive-file context menu keeps the current single default-printer entry.
- **Runtime constraint:** opening the archive-file context menu must remain responsive
  even if operating-system printer discovery is slow.
