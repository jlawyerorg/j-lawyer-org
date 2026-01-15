# Change: Desktop Panel Grid Layout Customization

## Why
Users have different workflows and screen sizes. The current DesktopPanel has a fixed layout with two nested SplitPanes. Users should be able to:
- Configure the grid structure (1x1 up to 2x3)
- Assign panels to grid cells with optional spanning
- Resize cells via mouse drag
- Hide sections they don't need
- Have settings synchronized across devices

## What Changes
- Replace fixed SplitPane structure with configurable grid layout (1-2 rows, 1-3 columns)
- Dynamically generate nested SplitPanes based on grid configuration for mouse-resizable cells
- Add panel-to-cell assignment with row/column spanning support
- Provide layout presets for common configurations (Classic, Horizontal, Vertical, etc.)
- Add configuration dialog for grid setup
- Store grid configuration and visibility in UserSettings (server-side, per user)
- Store SplitPane ratios as percentages in ClientSettings (local, device-specific)

## Impact
- Affected specs: New capability `desktop-panel`
- Affected code:
  - `j-lawyer-client/src/com/jdimension/jlawyer/client/desktop/DesktopPanel.java` (major refactoring)
  - `j-lawyer-client/src/com/jdimension/jlawyer/client/desktop/DesktopPanel.form` (update for new structure)
  - `j-lawyer-client/src/com/jdimension/jlawyer/client/desktop/DesktopGridLayoutDialog.java` (new)
  - `j-lawyer-client/src/com/jdimension/jlawyer/client/desktop/DesktopLayoutPreset.java` (new)
  - `j-lawyer-server-common/src/com/jdimension/jlawyer/server/services/settings/UserSettingsKeys.java`
  - `j-lawyer-client/src/com/jdimension/jlawyer/client/utils/ComponentUtils.java`
