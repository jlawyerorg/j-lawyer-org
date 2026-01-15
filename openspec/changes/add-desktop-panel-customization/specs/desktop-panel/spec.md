# Desktop Panel Grid Layout Customization

## ADDED Requirements

### Requirement: Configurable Grid Layout
Users SHALL be able to configure the DesktopPanel layout as a grid with 1-2 rows and 1-3 columns (maximum 2x3).

#### Scenario: User selects grid dimensions
- **WHEN** user opens the layout configuration dialog
- **THEN** user can select the number of rows (1-2) and columns (1-3)
- **AND** the grid preview updates to show the selected dimensions

#### Scenario: Grid layout is applied
- **WHEN** user confirms the grid configuration
- **THEN** the DesktopPanel rebuilds with the new grid structure
- **AND** panels are arranged according to their assigned positions

### Requirement: Panel-to-Cell Assignment
Users SHALL be able to assign each of the three panels (Last Changed, Due, Tagged) to specific grid cells.

#### Scenario: User assigns panel to cell
- **WHEN** user selects a panel in the configuration dialog
- **AND** user selects a target cell in the grid
- **THEN** the panel is assigned to that cell
- **AND** the preview updates to show the new assignment

#### Scenario: Cell already occupied
- **WHEN** user assigns a panel to a cell that already contains another panel
- **THEN** the existing panel is displaced (unassigned or moved)
- **AND** user is informed of the change

### Requirement: Panel Spanning
Users SHALL be able to configure panels to span multiple contiguous cells (row span and/or column span).

#### Scenario: User configures row span
- **WHEN** user sets a panel's row span to 2 in a 2-row grid
- **THEN** the panel occupies both rows in its column
- **AND** other panels cannot be assigned to the spanned cells

#### Scenario: User configures column span
- **WHEN** user sets a panel's column span to 2 in a multi-column grid
- **THEN** the panel occupies multiple columns in its row
- **AND** the layout adjusts to accommodate the spanning

#### Scenario: Invalid spanning prevented
- **WHEN** user attempts to set spanning that exceeds grid bounds
- **THEN** the spanning is limited to available cells
- **AND** user is informed of the constraint

### Requirement: Mouse-Resizable Cells
Grid cells SHALL be resizable by dragging the borders between cells with the mouse.

#### Scenario: User resizes cells horizontally
- **WHEN** user drags the vertical divider between two columns
- **THEN** the columns resize proportionally
- **AND** the new proportions are persisted locally

#### Scenario: User resizes cells vertically
- **WHEN** user drags the horizontal divider between two rows
- **THEN** the rows resize proportionally
- **AND** the new proportions are persisted locally

#### Scenario: Resize proportions persist across sessions
- **WHEN** user closes and reopens the application
- **THEN** the cell proportions are restored to their last saved state

### Requirement: Section Visibility Toggle
Users SHALL be able to show or hide each of the three panels independently.

#### Scenario: User hides a section via context menu
- **WHEN** user right-clicks on the DesktopPanel
- **THEN** a context menu appears with visibility toggles for each section
- **AND** user can toggle visibility without opening the configuration dialog

#### Scenario: User hides a section via dialog
- **WHEN** user unchecks a panel's visibility in the configuration dialog
- **THEN** the panel is excluded from the grid layout
- **AND** remaining panels fill the available space

#### Scenario: Prevent hiding all sections
- **WHEN** user attempts to hide the last visible section
- **THEN** the action is prevented
- **AND** an informative message is displayed

#### Scenario: Hidden sections excluded from grid
- **WHEN** one or more sections are hidden
- **THEN** only visible sections are included in the grid
- **AND** the grid adjusts to show only the visible panels

### Requirement: Layout Presets
The system SHALL provide predefined layout presets for common configurations, accessible both from the configuration dialog and directly from the context menu.

#### Scenario: User selects a preset from dialog
- **WHEN** user selects a preset from the dropdown in the configuration dialog
- **THEN** the grid dimensions and panel positions are set according to the preset
- **AND** the preview updates immediately

#### Scenario: User selects a preset from context menu
- **WHEN** user right-clicks on the DesktopPanel
- **AND** user opens the "Layout-Vorlage" submenu
- **AND** user clicks on a preset
- **THEN** the preset is applied immediately without opening the dialog

#### Scenario: Available presets
- **WHEN** user opens the preset dropdown or context menu submenu
- **THEN** the following presets are available:
  - Classic (2x2, Last Changed spanning left column)
  - Horizontal (1x3, three columns side by side)
  - Vertical (3x1, three rows stacked)
  - Focus Due (2x2, Due spanning right column)
  - Focus Last Changed (2x2, Last Changed spanning top row)
  - Compact (1x1, tabbed view)

### Requirement: Configuration Dialog with Live Preview
The configuration dialog SHALL provide an "Apply" button for live preview of changes without closing the dialog.

#### Scenario: User previews changes with Apply
- **WHEN** user modifies settings in the configuration dialog
- **AND** user clicks the "Apply" button
- **THEN** the changes are applied to the DesktopPanel immediately
- **AND** the dialog remains open for further adjustments

#### Scenario: User cancels changes
- **WHEN** user clicks "Cancel" in the configuration dialog
- **THEN** all changes (including those applied via "Apply") are reverted
- **AND** the layout returns to its state before the dialog was opened

#### Scenario: User confirms changes
- **WHEN** user clicks "OK" in the configuration dialog
- **THEN** the current settings are saved to UserSettings
- **AND** the dialog closes

### Requirement: Empty Grid Cells
Empty grid cells (cells without assigned panels) SHALL be displayed as blank space without visual placeholders.

#### Scenario: Grid has unassigned cells
- **WHEN** the grid configuration has fewer panels than cells
- **THEN** unassigned cells appear as empty space
- **AND** adjacent SplitPane dividers remain draggable

### Requirement: Compact/Tabbed Mode
When grid is configured as 1x1, visible panels SHALL be displayed as tabs in a JTabbedPane.

#### Scenario: User selects 1x1 grid
- **WHEN** user configures grid as 1 row and 1 column
- **THEN** all visible panels are displayed as tabs
- **AND** user can switch between panels by clicking tabs

#### Scenario: Tab order matches panel order
- **WHEN** panels are displayed as tabs
- **THEN** tabs appear in order: Last Changed, Due, Tagged (if visible)

### Requirement: Configuration Persistence in UserSettings
Grid configuration (dimensions, panel positions, visibility) SHALL be stored in UserSettings (server-side) and synchronized across devices.

#### Scenario: Settings sync across devices
- **WHEN** user configures layout on device A
- **AND** user logs in on device B
- **THEN** the same grid configuration is applied on device B

#### Scenario: Settings persist across sessions
- **WHEN** user logs out and logs back in
- **THEN** the grid configuration is restored

### Requirement: Resize Proportions in ClientSettings
Cell resize proportions SHALL be stored as percentages (0.0-1.0) in ClientSettings (local, device-specific).

#### Scenario: Proportions adapt to screen size
- **WHEN** user sets a divider at 50% on a large screen
- **AND** user moves to a smaller screen
- **THEN** the divider remains at 50% (adapts to new screen size)

#### Scenario: Different devices can have different proportions
- **WHEN** user adjusts proportions on device A
- **AND** user logs in on device B
- **THEN** device B uses its own stored proportions (or defaults if none)

### Requirement: UserSettings Keys
The system SHALL provide the following UserSettings keys for DesktopPanel configuration:

- `client.desktop.grid.rows` (int, 1-2, default: 2)
- `client.desktop.grid.cols` (int, 1-3, default: 2)
- `client.desktop.panel.layout` (JSON string with panel positions)
- `client.desktop.section.lastchanged.visible` (boolean, default: true)
- `client.desktop.section.due.visible` (boolean, default: true)
- `client.desktop.section.tagged.visible` (boolean, default: true)

#### Scenario: Default configuration
- **WHEN** a user has no configuration stored
- **THEN** the CLASSIC preset is applied (2x2 grid, all panels visible)
