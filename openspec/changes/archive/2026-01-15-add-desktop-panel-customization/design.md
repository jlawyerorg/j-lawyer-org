# Design: Desktop Panel Grid Layout Customization

## Context

The DesktopPanel (`j-lawyer-client/src/com/jdimension/jlawyer/client/desktop/DesktopPanel.java`) currently displays three sections using two hardcoded JSplitPanes:
- `jSplitPane1` (horizontal): splits "Due" section (right) from `jSplitPane2` (left)
- `jSplitPane2` (vertical): splits "Last Changed" (top) from "Tagged" (bottom)

This fixed structure limits user customization. Users want to:
1. Rearrange panels based on their workflow
2. Hide unused sections
3. Have consistent settings across devices

## Goals
- Configurable grid layout (1x1 to 2x3)
- Panels assignable to grid cells with spanning
- Mouse-resizable cells
- Section visibility toggle
- Server-side settings sync (UserSettings)
- Device-specific layout proportions (ClientSettings)

## Non-Goals
- Free-form docking (too complex, requires external library)
- More than 3 rows or columns (diminishing returns, complexity)
- Drag-and-drop panel rearrangement (configuration dialog is sufficient)
- Floating/detached panels

## Decisions

### Decision 1: Dynamic SplitPane Tree Generation
**Chosen**: Generate nested JSplitPane structure dynamically based on grid configuration

**Rationale**:
- Reuses existing Swing component (JSplitPane) with native resize support
- No external dependencies
- Consistent with existing codebase patterns
- Automatic resize handles between all cells

**Algorithm**:
```
buildSplitPaneTree(grid, startRow, startCol, numRows, numCols):
    if single cell:
        return panel or placeholder
    if numCols > 1:
        split horizontally at midpoint
        left = buildSplitPaneTree(left half)
        right = buildSplitPaneTree(right half)
        return new JSplitPane(HORIZONTAL, left, right)
    else if numRows > 1:
        split vertically at midpoint
        top = buildSplitPaneTree(top half)
        bottom = buildSplitPaneTree(bottom half)
        return new JSplitPane(VERTICAL, top, bottom)
```

**Generated structures**:
```
1x2: HSplit(P1, P2)
1x3: HSplit(P1, HSplit(P2, P3))
2x1: VSplit(P1, P2)
2x2: HSplit(VSplit(P1, P3), VSplit(P2, P4))
2x3: HSplit(VSplit(P1, P4), HSplit(VSplit(P2, P5), VSplit(P3, P6)))
```

**Alternatives considered**:
- GridBagLayout: No native resize support
- MigLayout: External dependency (~100KB)
- Docking Framework: Heavy dependency (~1.5MB), overkill for this use case

### Decision 2: Storage Strategy
**Chosen**: Split between UserSettings and ClientSettings

| Setting | Storage | Reason |
|---------|---------|--------|
| Grid dimensions | UserSettings | Sync across devices |
| Panel positions | UserSettings | Sync across devices |
| Panel visibility | UserSettings | Sync across devices |
| SplitPane ratios | ClientSettings | Device-specific (screen sizes vary) |

**UserSettings key**: `CONF_DESKTOP_PANEL_LAYOUT` stores JSON:
```json
{
  "rows": 2,
  "cols": 2,
  "panels": {
    "lastchanged": {"row": 0, "col": 0, "rowSpan": 1, "colSpan": 1, "visible": true},
    "due":         {"row": 0, "col": 1, "rowSpan": 2, "colSpan": 1, "visible": true},
    "tagged":      {"row": 1, "col": 0, "rowSpan": 1, "colSpan": 1, "visible": true}
  }
}
```

**ClientSettings keys**: `split.ratio.<classname>.<id>` stores double (0.0-1.0)

**Alternative considered**: All in UserSettings - rejected because optimal proportions depend on screen size

### Decision 3: Spanning Implementation
**Chosen**: Limited spanning - panels can span multiple cells but only in contiguous rectangular areas

**Constraints**:
- Spanning must not create overlapping assignments
- Spanning must fit within grid bounds
- When spanning, panel is placed in tree at the position of its top-left cell

**Implementation**:
- During tree generation, check if cell is part of a spanning panel
- If yes and not the origin cell, skip (placeholder already covered)
- If yes and is origin cell, create panel with combined weight

**Alternative considered**: Full arbitrary spanning - rejected due to complexity in SplitPane tree generation

### Decision 4: Configuration UI
**Chosen**: Modal dialog with visual grid editor and live preview

**Components**:
1. Preset dropdown (quick selection)
2. Grid size spinners (rows: 1-2, cols: 1-3)
3. Visual grid preview showing current assignment
4. Panel list with visibility checkboxes and position/span controls
5. "Apply" button for live preview without closing dialog
6. "OK" to apply and close, "Cancel" to revert changes

**Entry points**:
- Right-click context menu on DesktopPanel:
  - "Layout anpassen..." → opens configuration dialog
  - "Layout-Vorlage" → submenu with preset selection (direct apply)
  - "Bereiche anzeigen" → submenu with visibility toggles
- Quick preset/visibility changes apply immediately without dialog

**Alternative considered**: Inline toolbar - rejected due to space constraints

### Decision 5: Grid 1x1 Handling
**Chosen**: Use JTabbedPane when grid is 1x1

When user selects 1x1 grid, all visible panels are displayed as tabs:
```
┌─────────────────────────────────┐
│ [Zuletzt] [Fällig] [Etiketten] │
├─────────────────────────────────┤
│                                 │
│     Active Tab Content          │
│                                 │
└─────────────────────────────────┘
```

**Implementation**: Special case in `rebuildLayout()` - if rows=1 and cols=1, create JTabbedPane instead of SplitPane tree.

### Decision 6: Preset Layouts
**Chosen**: Provide 6 predefined presets for common use cases

| Preset | Grid | Description | Layout |
|--------|------|-------------|--------|
| CLASSIC | 2x2 | Current default layout | LC spans 2 rows left, Due top-right, Tagged bottom-right |
| HORIZONTAL | 1x3 | Three columns | LC, Due, Tagged side by side |
| VERTICAL | 3x1 | Three rows | LC, Due, Tagged stacked |
| FOCUS_DUE | 2x2 | Emphasize due items | Due spans 2 rows right, LC top-left, Tagged bottom-left |
| FOCUS_LASTCHANGED | 2x2 | Emphasize recent | LC spans 2 cols top, Due bottom-left, Tagged bottom-right |
| COMPACT | 1x1 | Tabbed view | All panels as tabs |

**Implementation**: Enum with factory method returning grid config

### Decision 7: Empty Grid Cells
**Chosen**: Leave empty cells blank (no placeholder)

**Rationale**:
- Cleaner visual appearance
- Empty cells naturally collapse when adjacent SplitPane dividers are dragged
- No visual clutter for unused grid positions
- Users who want to use all cells will assign panels; empty cells indicate intentional unused space

**Implementation**:
- Empty cells receive a transparent JPanel with no content
- No border, no background, no label
- SplitPane dividers remain draggable to resize adjacent panels

**Alternative considered**: Visual placeholder with "Drop panel here" text - rejected as too cluttered

## Risks / Trade-offs

### Risk: Complex SplitPane nesting affects performance
Large grids (2x3) generate up to 5 nested SplitPanes.
- **Mitigation**: Limit max grid size to 2x3, lazy-load panel content

### Risk: NetBeans GUI Builder compatibility
Dynamic layout generation may conflict with .form file.
- **Mitigation**: Keep static elements (header, widgets) in .form, generate only the grid area dynamically

### Risk: Layout breaks on very small screens
Deeply nested SplitPanes may have minimum size issues.
- **Mitigation**: Set reasonable minimum sizes, suggest COMPACT preset for small screens

### Trade-off: Spanning complexity vs. flexibility
Full spanning support is complex; limited spanning is less flexible.
- **Accepted**: Limited spanning covers most use cases, keeps implementation manageable

## Migration Plan

1. **Phase 1**: Add new classes (GridPosition, DesktopLayoutPreset, DesktopGridLayoutDialog)
2. **Phase 2**: Add ComponentUtils percentage methods
3. **Phase 3**: Refactor DesktopPanel to use dynamic layout
4. **Phase 4**: Add configuration UI and context menu
5. **Phase 5**: First run migration:
   - If no UserSettings exist: Apply CLASSIC preset
   - Existing pixel-based SplitPane settings: Convert to ratios on first load

## Resolved Questions

1. ~~Should there be an "Apply" button in the dialog for live preview, or only "OK" to apply?~~
   **Resolved**: Yes, include "Apply" button for live preview (see Decision 4)

2. ~~Should empty grid cells show a visual placeholder or remain blank?~~
   **Resolved**: Leave blank, no placeholder (see Decision 7)

3. ~~Should the preset selection be available directly in the context menu (without opening dialog)?~~
   **Resolved**: Yes, presets available as submenu "Layout-Vorlage" in context menu (see Decision 4)
