## Context

The desktop client's archive-file context menu contains the existing action `drucken
(Standarddrucker)`. `ArchiveFilePanel` prepares the selected supported documents and
delegates to `LauncherFactory.directPrint(...)`. The current PDF path resolves the
default `PrintService`; the LibreOffice path invokes printing to the default printer.

The target machines are ordinary Windows and other desktop installations. Their set of
installed printers may change when a laptop moves between locations, and it may also
contain many stale entries. A solution must therefore avoid treating a saved printer as
proof that the printer is currently available, while still keeping the frequent choices
short enough for one-click use.

## Goals / Non-Goals

**Goals**
- Keep default-printer direct printing as the fastest and unchanged path.
- Offer one-click entries for a small user-controlled set of currently available
  printers.
- Keep the document context menu unchanged unless the user has configured at least one
  currently available non-default favourite printer.
- Keep opening the archive-file context menu responsive even when printer discovery is
  slow or blocked by the operating system, printer drivers, or network printers.
- Allow user-friendly local display labels for favourite printers.
- Keep all configuration and implementation client-side.
- Fail visibly instead of redirecting a named print job to an unintended printer.
- Keep the change small, reviewable, and independently testable.

**Non-Goals**
- Defining or persisting a j-lawyer-specific default printer.
- Persisting printer driver details, capabilities, print services, or printer
  availability.
- Configuring duplex, trays, paper size, resolution, or other printer properties.
- Changing the separate `als Fax senden` action or the VoIP/fax integration.
- Adding a server setting, database field, REST endpoint, background service, network
  listener, or external dependency.
- Redesigning the document context menu outside the print entries.
- Adding a general `print to any installed printer` chooser to the archive-file context
  menu.

## Decisions

### Decision: Live printer discovery is authoritative

Printer services are obtained from the operating system and treated as the only authority
for whether a printer can be offered or resolved. However, printer discovery can be
delayed by operating-system printer stacks, drivers, or network printers, so the
archive-file context menu must not synchronously wait for a printer lookup on the Swing
Event Dispatch Thread.

The implementation should maintain an in-memory snapshot of the last completed printer
discovery and refresh it asynchronously when printer choices may be needed, including
client start, opening the printer favourites settings dialog, and opening the archive-file
context menu. The context menu is built from the last completed snapshot. If no snapshot
is available yet, the menu remains in the safe default-only state and a refresh may update
subsequent openings.

An explicit named print target is still resolved against a fresh live printer lookup
before dispatch. That resolution may happen as part of the existing print workflow, but
it must not freeze the context menu while it is opening.

No cached `PrintService`, driver data, capability data, or availability flag is written
to client settings.

This proposal uses "available" to mean "reported by the operating system in the latest
completed discovery result". It does not promise that a printer is physically connected,
online, reachable, or able to accept a job. Print-time failures are handled explicitly
without falling back to the default printer.

### Decision: Store only device-local favourites

The optional favourite preference is an allow-list of printer service names with optional
display labels, stored by `ClientSettings` on the individual client. This is a local UI
preference, not a printer configuration. It is not sent to the server and is not shared
between client installations.

The favourite entries shown in the document context menu are the intersection of:

1. printer names currently returned by the operating system; and
2. names selected as local favourites.

The current default printer is not duplicated as a favourite entry even if its name is
selected. If this leaves no currently available non-default favourite, the archive-file
context menu remains in its current shape with only `drucken (Standarddrucker)`.
Duplicate printer names are collapsed, and entries are sorted case-insensitively for
predictable display.

Optional display labels are used only for the menu text and settings UI. The actual print
target is always resolved by the stored operating-system printer service name. Duplicate
or blank labels are handled by falling back to the real printer name or otherwise
disambiguating according to the existing UI conventions.

**Alternatives considered**
- *Show every installed printer directly in the context menu.* Rejected because stale
  and rarely used Windows printer entries can make the context menu too long.
- *Show only a fixed maximum number of printers.* Rejected because the required printer
  could be omitted arbitrarily.
- *Persist one or more configured printers and use them without live resolution.*
  Rejected because a laptop may see different printers at different locations.
- *Always offer an on-demand chooser for every current printer.* Rejected for the first
  implementation because it changes the context menu even for users who have not opted
  into favourite printers and can turn a one-click workflow into a search task on systems
  with many stale printer entries.

### Decision: Unavailable favourites remain manageable but never actionable

The settings dialog shows the union of currently discovered printers and previously
selected favourite names. A previously selected name that is not in the current live list
is displayed as a disabled `derzeit nicht verfügbar` entry; the user can remove it, but
it remains selected by default so that it reappears automatically when the laptop returns
to that location.

The settings dialog does not permit arbitrary free-text printer target names. A new
favourite can only be selected from printers returned by the operating system at that
time. The optional display label is free text, but it never participates in printer
resolution.

### Decision: Printer favourites are user-level client preferences

The printer favourites settings dialog is available to ordinary users. It must not be
restricted to users with `adminRole` or `sysAdminRole`, because it only modifies
device-local client preferences and does not change shared server configuration.

### Decision: Default item first, submenu only after opt-in

When no currently available non-default favourite exists, the existing context-menu action
remains unchanged:

- `drucken (Standarddrucker)`

When at least one currently available non-default favourite exists, this entry becomes a
`Drucken` submenu containing:

1. `Standarddrucker`; and
2. one entry for each currently available non-default favourite, using its display label
   when present and its real printer name otherwise.

Printers that are merely installed in the operating system are not shown in the document
context menu unless they were selected as favourites. There is no all-printer chooser in
this first change.

### Decision: Resolve the target before dispatch and never silently fall back

The chosen printer name is resolved against a fresh live printer list before a print
batch starts. The resolved target applies to all selected supported documents. If it
cannot be resolved, the batch is not intentionally sent to another printer and the user
receives a clear error.

For PDFs, the existing PDFBox/Java printing path uses the selected `PrintService`. For
documents printed through LibreOffice, the invocation uses LibreOffice's named-printer
argument instead of its default-printer argument. Printer names are passed as structured
process arguments, never interpolated into a shell command.

The default-printer action continues to call the existing default path. The explicit
target overload is additive so unrelated callers retain their current semantics.

### Decision: Keep Swing/NetBeans form compatibility

Any changed Swing component with a matching `.form` file is updated in lockstep. Dynamic
menu entries are rebuilt in code without replacing or serializing the static existing
default-printer menu item.

## Risks / Trade-offs

- **Risk:** Java and LibreOffice may expose or interpret a printer name differently on a
  particular operating system. **Mitigation:** use the exact live service name, keep PDF
  and LibreOffice tests separate, and treat a rejected target as an error without
  fallback.
- **Risk:** Querying printers can be slow or block due to operating-system, driver, or
  network-printer behaviour. **Mitigation:** do not query printers synchronously while
  opening the archive-file context menu; build the menu from the latest completed
  snapshot and refresh discovery asynchronously.
- **Risk:** A printer can disappear after live resolution or a multi-document batch can
  fail partway through. **Mitigation:** resolve before dispatch, report the failing target
  and document, and never retry on the default printer. The UI must not claim atomicity
  for physical printing.
- **Risk:** A long printer name or label can make the context menu wide. **Mitigation:**
  only user-selected favourites are direct entries; the implementation may apply the
  project's existing UI truncation convention while preserving the full target name in a
  tooltip.
- **Risk:** A stale favourite remains in settings. **Mitigation:** mark it unavailable
  and allow removal; never show it as an active document-menu action.
- **Risk:** Editing generated Swing code without its form metadata breaks the NetBeans
  GUI Builder. **Mitigation:** change each `.java`/`.form` pair together and inspect both
  in review.

## Migration Plan

1. Add the new client-local setting with an empty default.
2. Add live printer discovery and named-target resolution behind a small client utility.
3. Add asynchronous printer discovery and snapshot handling so context-menu opening does
   not wait for printer lookup.
4. Extend the print launcher with an additive explicit-target path.
5. Add favourite settings and dynamic document-menu entries.
6. Verify unchanged default printing without favourites, named PDF printing, named
   LibreOffice printing, stale favourites, location changes, and error handling in an
   isolated client test environment.

Rollback consists of removing the new client UI and explicit-target path. A leftover
client setting is inert and can be ignored; no server or data migration is required.

## Resolved Review Choices

- Device-local favourite printer names plus optional display labels are acceptable,
  provided every visible entry and every print dispatch is resolved against the current
  operating-system printer list.
- Unavailable favourite names are displayed in the settings dialog as disabled rows.
