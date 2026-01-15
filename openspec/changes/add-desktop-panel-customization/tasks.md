# Implementation Tasks

## 1. Add UserSettings Keys
- [ ] 1.1 Add new keys to `UserSettingsKeys.java`:
  - `CONF_DESKTOP_GRID_ROWS` (int, 1-2)
  - `CONF_DESKTOP_GRID_COLS` (int, 1-3)
  - `CONF_DESKTOP_PANEL_LAYOUT` (JSON string with panel positions)
  - `CONF_DESKTOP_SECTION_LASTCHANGED_VISIBLE` (boolean)
  - `CONF_DESKTOP_SECTION_DUE_VISIBLE` (boolean)
  - `CONF_DESKTOP_SECTION_TAGGED_VISIBLE` (boolean)

## 2. Create Layout Preset Enum
- [ ] 2.1 Create `DesktopLayoutPreset.java` with predefined layouts:
  - CLASSIC (2x2, current layout)
  - HORIZONTAL (1x3, three columns)
  - VERTICAL (3x1, three rows)
  - FOCUS_DUE (2x2, Due panel spans 2 rows)
  - FOCUS_LASTCHANGED (2x2, LastChanged spans 2 rows)
  - COMPACT (1x1, tabbed view)
- [ ] 2.2 Each preset defines: grid dimensions, panel positions, spanning

## 3. Create Grid Position Model
- [ ] 3.1 Create `GridPosition` class or inner class:
  - `row`, `col` (0-based start position)
  - `rowSpan`, `colSpan` (default 1)
  - `visible` (boolean)
- [ ] 3.2 Add JSON serialization/deserialization for UserSettings storage

## 4. Update ComponentUtils for Percentage-Based Layout
- [ ] 4.1 Add `persistSplitPaneRatio(JSplitPane, Class, String)` method
- [ ] 4.2 Add `restoreSplitPaneRatio(JSplitPane, Class, String)` method
- [ ] 4.3 Store ratio as double (0.0-1.0) calculated from divider position / total size
- [ ] 4.4 Handle edge case: restore when component not yet sized (use ComponentListener)

## 5. Implement Dynamic SplitPane Tree Generation
- [ ] 5.1 Create `buildSplitPaneTree()` method in DesktopPanel:
  - Input: 2D array of panels based on grid positions
  - Output: nested JSplitPane structure
  - Algorithm: recursively split horizontally then vertically
- [ ] 5.2 Handle empty cells with placeholder panels
- [ ] 5.3 Handle spanning (panel occupies multiple cells)
- [ ] 5.4 Register all generated SplitPanes for ratio persistence

## 6. Refactor DesktopPanel
- [ ] 6.1 Extract panel content creation into separate methods:
  - `createLastChangedContent()` returns JPanel with scrollpane
  - `createDueContent()` returns JPanel with tabpane
  - `createTaggedContent()` returns JPanel with tabpane
- [ ] 6.2 Add `rebuildLayout()` method that:
  - Reads grid configuration from UserSettings
  - Builds panel position map
  - Calls `buildSplitPaneTree()`
  - Restores SplitPane ratios
- [ ] 6.3 Update existing timer tasks to use new panel references
- [ ] 6.4 Update DesktopPanel.form for NetBeans compatibility

## 7. Create Configuration Dialog
- [ ] 7.1 Create `DesktopGridLayoutDialog.java`:
  - Preset dropdown (quick selection)
  - Grid size spinners (rows: 1-2, cols: 1-3)
  - Visual grid preview (clickable cells)
  - Panel assignment controls (checkboxes, cell selection, span selection)
- [ ] 7.2 Add "Apply" button for live preview without closing dialog
- [ ] 7.3 Add "OK" (apply + close), "Cancel" (revert), "Reset to Default" buttons
- [ ] 7.4 Store pre-dialog state for cancel/revert functionality
- [ ] 7.5 Create corresponding .form file for NetBeans GUI Builder

## 8. Add Context Menu with Submenus
- [ ] 8.1 Create context menu on DesktopPanel (right-click on header/background)
- [ ] 8.2 Add menu item "Layout anpassen..." → opens DesktopGridLayoutDialog
- [ ] 8.3 Add submenu "Layout-Vorlage" with all presets:
  - Classic, Horizontal, Vertical, Focus Due, Focus Last Changed, Compact
  - Selecting preset applies immediately without dialog
- [ ] 8.4 Add submenu "Bereiche anzeigen" with visibility toggles:
  - Checkable items: "Zuletzt geändert", "Fällig", "Nach Etikett"
  - Toggling applies immediately without dialog
- [ ] 8.5 Prevent unchecking last visible section (show warning)

## 9. Implement Section Visibility Logic
- [ ] 9.1 When section hidden: exclude from grid, rebuild layout
- [ ] 9.2 Handle edge case: prevent hiding all sections
- [ ] 9.3 Handle 1x1 grid with hidden sections: show remaining as tabs
- [ ] 9.4 Update visibility state in UserSettings on toggle

## 10. Handle Special Cases
- [ ] 10.1 Grid 1x1: Display all visible panels as tabs in JTabbedPane
- [ ] 10.2 Single visible panel: No SplitPane needed, show panel directly
- [ ] 10.3 Two visible panels: Single SplitPane (horizontal or vertical based on grid)
- [ ] 10.4 Empty cells in grid: Show subtle placeholder or collapse

## 11. Testing
- [ ] 11.1 Test all preset layouts
- [ ] 11.2 Test custom grid configurations (all valid combinations)
- [ ] 11.3 Test panel visibility toggles
- [ ] 11.4 Test spanning configurations
- [ ] 11.5 Test resize persistence across sessions
- [ ] 11.6 Test layout sync across devices (same user, different machines)
- [ ] 11.7 Test edge cases: window resize, minimize/restore
