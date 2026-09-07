## 0. Approval and implementation baseline

- [ ] 0.1 Obtain maintainer approval for the proposal, especially the distinction between
  live printer discovery and device-local persistence of favourite printer names plus
  optional display labels.
- [ ] 0.2 Confirm the final labels and placement of the printer favourites settings in
  the existing `Einstellungen` menu before changing Swing forms.
- [ ] 0.3 Create a clean feature branch from the then-current upstream `master`; do not
  implement from an outdated fork branch.

## 1. Live printer discovery and favourite model

- [ ] 1.1 Add a small client-side printer utility/model that obtains the current printer
  services and default printer from `PrintServiceLookup` on demand.
- [ ] 1.2 Normalize the live view without changing printer names: discard null/blank
  names, collapse exact duplicates, and sort display entries case-insensitively.
- [ ] 1.3 Add a `ClientSettings` key for device-local favourite printer names and optional
  display labels, using an existing local settings mechanism with an empty default.
- [ ] 1.4 Implement the intersection of live printers and favourite names, excluding the
  current default printer from duplicate favourite actions.
- [ ] 1.5 Keep unavailable saved names in the settings model, mark them unavailable, and
  never return them as actionable document-menu targets.

## 2. Explicit printer support in the print launcher

- [ ] 2.1 Refactor `LauncherFactory.directPrint(...)` only as far as needed to preserve
  the current default-printer entry point and add an explicit named-printer entry point.
- [ ] 2.2 Resolve a named printer against a fresh live printer list before dispatch; if it
  is absent, return/report a clear error and do not invoke the default-printer path.
- [ ] 2.3 For PDF documents, set the resolved `PrintService` on the existing Java/PDFBox
  print job.
- [ ] 2.4 For LibreOffice-supported documents, use LibreOffice's named-printer invocation
  (`--pt` with the printer name) while retaining the current default invocation for the
  existing action.
- [ ] 2.5 Pass executable paths, options, printer names, and document paths as separate
  process arguments without a shell or string interpolation.
- [ ] 2.6 Preserve existing document-type filtering, temporary-file cleanup, and error
  reporting; include the target printer name in named-print failures without exposing
  document contents or private paths in public test artefacts.

## 3. Printer favourites settings UI

- [ ] 3.1 Add a compact modal printer favourites settings dialog and matching NetBeans
  `.form` file.
- [ ] 3.2 Populate the dialog on every open with the union of live printers and saved
  favourite names; visually mark saved names that are currently unavailable.
- [ ] 3.3 Permit selecting current printers and removing unavailable saved names, but do
  not permit arbitrary free-text printer target entries.
- [ ] 3.4 Permit an optional display label for each selected favourite and use it only for
  UI display, never for printer resolution.
- [ ] 3.5 Save only the selected names and optional display labels to local
  `ClientSettings`; do not call a server service or persist driver/capability/
  availability data.
- [ ] 3.6 Add the dialog entry to the agreed location under `Einstellungen`, updating
  `JKanzleiGUI.java` and `JKanzleiGUI.form` together if those are the confirmed files.

## 4. Dynamic archive-file context menu

- [ ] 4.1 Preserve the existing static `drucken (Standarddrucker)` item and handler.
- [ ] 4.2 Keep the existing static `drucken (Standarddrucker)` item unchanged when there is
  no currently available non-default favourite printer.
- [ ] 4.3 When at least one currently available non-default favourite exists, present a
  `Drucken` submenu with `Standarddrucker` first and one entry per available favourite.
- [ ] 4.4 Rebuild the favourite actions whenever the document context menu is opened.
- [ ] 4.5 Ensure repeated menu openings do not accumulate duplicate components or action
  listeners and do not disturb unrelated context-menu entries.
- [ ] 4.6 Route a favourite action through the explicit named-printer path for all selected
  supported documents.
- [ ] 4.7 Keep `ArchiveFilePanel.java` and `ArchiveFilePanel.form` compatible with the
  NetBeans GUI Builder.

## 5. Automated verification

- [ ] 5.1 Add focused tests for live/favourite intersection, deterministic sorting,
  duplicate suppression, default-printer suppression, and unavailable saved names.
- [ ] 5.2 Add tests for named-target resolution and the no-fallback rule when a target is
  absent.
- [ ] 5.3 Add tests around process-argument construction for printer names and document
  paths containing spaces and non-ASCII characters; verify that no shell command string
  is constructed.
- [ ] 5.4 Add UI/model-level tests where feasible for rebuilding the dynamic menu twice
  without duplicate entries or listeners.
- [ ] 5.5 Add tests that optional display labels affect menu text only and never printer
  resolution.

## 6. Manual verification in an isolated client environment

- [ ] 6.1 Confirm the existing default-printer action behaves as before with an empty
  favourite list and with no currently available non-default favourite.
- [ ] 6.2 Add two generic test printers to favourites and verify that only currently
  available, non-default selections appear as direct actions.
- [ ] 6.3 Verify named printing of a PDF and a LibreOffice-supported document to each test
  target, including printer names, display labels, and file names containing spaces.
- [ ] 6.4 Select multiple supported documents and verify that all are intentionally sent
  to the same selected target.
- [ ] 6.5 Make a saved favourite printer unavailable, reopen the menu without restarting
  the client, and verify that it disappears from the context menu, remains marked in
  settings, and is never replaced by the default printer.
- [ ] 6.6 Make the printer available again and verify that it reappears after reopening the
  menu without changing settings.
- [ ] 6.7 Verify the no-printer and printer-disappears-before-dispatch error paths and
  confirm that no silent fallback occurs.
- [ ] 6.8 Inspect the context menu on a system with many installed printers and verify that
  only selected, currently available favourites appear there.

## 7. Review gates

- [ ] 7.1 Run the project-prescribed manual client build/test procedure; do not alter the
  production installation or production data.
- [ ] 7.2 Review every changed `.java`/`.form` pair, the complete diff, and the changed-file
  list.
- [ ] 7.3 Perform secret/privacy scans on code, tests, logs, screenshots, commits, and PR
  text; use only generic printer and document names in public artefacts.
- [ ] 7.4 Run `openspec validate add-dynamic-printer-selection --strict` and resolve all
  findings before requesting implementation review.
- [ ] 7.5 Submit any implementation as a small draft pull request with transparent AI-use
  disclosure and explicit manual-test results.
