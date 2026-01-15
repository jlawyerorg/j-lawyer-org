/*
 *                     GNU AFFERO GENERAL PUBLIC LICENSE
 *                        Version 3, 19 November 2007
 *
 *  Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
 *  Everyone is permitted to copy and distribute verbatim copies
 *  of this license document, but changing it is not allowed.
 */
package com.jdimension.jlawyer.client.desktop;

import java.util.HashMap;
import java.util.Map;

/**
 * Predefined layout presets for the DesktopPanel grid configuration.
 * Each preset defines grid dimensions and panel positions.
 *
 * @author jens
 */
public enum DesktopLayoutPreset {

    /**
     * Classic layout (2x2): Last Changed top-left, Tagged bottom-left, Due spans right column.
     * This matches the original DesktopPanel layout.
     */
    CLASSIC("Klassisch", 2, 2) {
        @Override
        public Map<String, GridPosition> getPanelPositions() {
            Map<String, GridPosition> positions = new HashMap<>();
            positions.put(PANEL_LASTCHANGED, new GridPosition(0, 0, 1, 1));
            positions.put(PANEL_TAGGED, new GridPosition(1, 0, 1, 1));
            positions.put(PANEL_DUE, new GridPosition(0, 1, 2, 1)); // spans 2 rows
            return positions;
        }
    },

    /**
     * Horizontal layout (1x3): All three panels side by side.
     */
    HORIZONTAL("Horizontal", 1, 3) {
        @Override
        public Map<String, GridPosition> getPanelPositions() {
            Map<String, GridPosition> positions = new HashMap<>();
            positions.put(PANEL_LASTCHANGED, new GridPosition(0, 0));
            positions.put(PANEL_DUE, new GridPosition(0, 1));
            positions.put(PANEL_TAGGED, new GridPosition(0, 2));
            return positions;
        }
    },

    /**
     * Vertical layout (3x1): All three panels stacked vertically.
     */
    VERTICAL("Vertikal", 3, 1) {
        @Override
        public Map<String, GridPosition> getPanelPositions() {
            Map<String, GridPosition> positions = new HashMap<>();
            positions.put(PANEL_LASTCHANGED, new GridPosition(0, 0));
            positions.put(PANEL_DUE, new GridPosition(1, 0));
            positions.put(PANEL_TAGGED, new GridPosition(2, 0));
            return positions;
        }
    },

    /**
     * Compact layout (1x1): All panels displayed as tabs.
     */
    COMPACT("Kompakt (Tabs)", 1, 1) {
        @Override
        public Map<String, GridPosition> getPanelPositions() {
            Map<String, GridPosition> positions = new HashMap<>();
            // All panels at position 0,0 - will be displayed as tabs
            positions.put(PANEL_LASTCHANGED, new GridPosition(0, 0));
            positions.put(PANEL_DUE, new GridPosition(0, 0));
            positions.put(PANEL_TAGGED, new GridPosition(0, 0));
            return positions;
        }
    };

    // Panel identifiers
    public static final String PANEL_LASTCHANGED = "lastchanged";
    public static final String PANEL_DUE = "due";
    public static final String PANEL_TAGGED = "tagged";

    private final String displayName;
    private final int rows;
    private final int cols;

    DesktopLayoutPreset(String displayName, int rows, int cols) {
        this.displayName = displayName;
        this.rows = rows;
        this.cols = cols;
    }

    /**
     * Returns the localized display name for this preset.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the number of rows for this preset.
     *
     * @return number of rows (1-3)
     */
    public int getRows() {
        return rows;
    }

    /**
     * Returns the number of columns for this preset.
     *
     * @return number of columns (1-3)
     */
    public int getCols() {
        return cols;
    }

    /**
     * Returns the panel positions for this preset.
     * Each entry maps a panel ID to its GridPosition.
     *
     * @return map of panel IDs to positions
     */
    public abstract Map<String, GridPosition> getPanelPositions();

    /**
     * Creates a complete grid configuration for this preset.
     *
     * @return the grid configuration
     */
    public DesktopGridConfiguration toConfiguration() {
        DesktopGridConfiguration config = new DesktopGridConfiguration(rows, cols);
        for (Map.Entry<String, GridPosition> entry : getPanelPositions().entrySet()) {
            config.setPanelPosition(entry.getKey(), entry.getValue().copy());
        }
        return config;
    }

    @Override
    public String toString() {
        return displayName;
    }

    /**
     * Finds a preset by its display name.
     *
     * @param displayName the display name to search for
     * @return the matching preset, or null if not found
     */
    public static DesktopLayoutPreset findByDisplayName(String displayName) {
        for (DesktopLayoutPreset preset : values()) {
            if (preset.getDisplayName().equals(displayName)) {
                return preset;
            }
        }
        return null;
    }
}
