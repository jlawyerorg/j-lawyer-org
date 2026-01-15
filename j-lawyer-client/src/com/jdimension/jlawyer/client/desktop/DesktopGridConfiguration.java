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
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Holds the complete grid configuration for the DesktopPanel.
 * Includes grid dimensions and panel positions.
 *
 * @author jens
 */
public class DesktopGridConfiguration {

    private int rows;
    private int cols;
    private Map<String, GridPosition> panelPositions;

    /**
     * Creates a new configuration with default 2x2 grid.
     */
    public DesktopGridConfiguration() {
        this(2, 2);
    }

    /**
     * Creates a new configuration with specified dimensions.
     *
     * @param rows number of rows (1-3)
     * @param cols number of columns (1-3)
     */
    public DesktopGridConfiguration(int rows, int cols) {
        this.rows = Math.max(1, Math.min(3, rows));
        this.cols = Math.max(1, Math.min(3, cols));
        this.panelPositions = new HashMap<>();
    }

    /**
     * Creates a deep copy of this configuration.
     *
     * @return a new configuration with copied values
     */
    public DesktopGridConfiguration copy() {
        DesktopGridConfiguration copy = new DesktopGridConfiguration(rows, cols);
        for (Map.Entry<String, GridPosition> entry : panelPositions.entrySet()) {
            copy.panelPositions.put(entry.getKey(), entry.getValue().copy());
        }
        return copy;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = Math.max(1, Math.min(3, rows));
        // Adjust panel positions if they exceed new bounds
        adjustPanelPositions();
    }

    public int getCols() {
        return cols;
    }

    public void setCols(int cols) {
        this.cols = Math.max(1, Math.min(3, cols));
        // Adjust panel positions if they exceed new bounds
        adjustPanelPositions();
    }

    /**
     * Gets the position for a specific panel.
     *
     * @param panelId the panel identifier
     * @return the position, or null if not set
     */
    public GridPosition getPanelPosition(String panelId) {
        return panelPositions.get(panelId);
    }

    /**
     * Sets the position for a specific panel.
     *
     * @param panelId the panel identifier
     * @param position the position
     */
    public void setPanelPosition(String panelId, GridPosition position) {
        panelPositions.put(panelId, position);
    }

    /**
     * Gets all panel positions.
     *
     * @return map of panel IDs to positions
     */
    public Map<String, GridPosition> getPanelPositions() {
        return panelPositions;
    }

    /**
     * Checks if this is a compact (1x1) layout where tabs should be used.
     *
     * @return true if layout is 1x1
     */
    public boolean isCompactLayout() {
        return rows == 1 && cols == 1;
    }

    /**
     * Gets the number of visible panels.
     *
     * @return count of visible panels
     */
    public int getVisiblePanelCount() {
        int count = 0;
        for (GridPosition pos : panelPositions.values()) {
            if (pos.isVisible()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Adjusts panel positions to fit within current grid bounds.
     */
    private void adjustPanelPositions() {
        for (GridPosition pos : panelPositions.values()) {
            // Ensure position is within bounds
            if (pos.getRow() >= rows) {
                pos.setRow(rows - 1);
            }
            if (pos.getCol() >= cols) {
                pos.setCol(cols - 1);
            }
            // Adjust spanning to fit
            if (pos.getRow() + pos.getRowSpan() > rows) {
                pos.setRowSpan(rows - pos.getRow());
            }
            if (pos.getCol() + pos.getColSpan() > cols) {
                pos.setColSpan(cols - pos.getCol());
            }
        }
    }

    /**
     * Converts this configuration to a JSON string for persistence.
     *
     * @return JSON string representation
     */
    @SuppressWarnings("unchecked")
    public String toJsonString() {
        JSONObject json = new JSONObject();
        json.put("rows", rows);
        json.put("cols", cols);

        JSONObject panels = new JSONObject();
        for (Map.Entry<String, GridPosition> entry : panelPositions.entrySet()) {
            panels.put(entry.getKey(), entry.getValue().toJson());
        }
        json.put("panels", panels);

        return json.toJSONString();
    }

    /**
     * Creates a configuration from a JSON string.
     *
     * @param jsonString the JSON string
     * @return the configuration, or default if parsing fails
     */
    public static DesktopGridConfiguration fromJsonString(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) {
            return DesktopLayoutPreset.CLASSIC.toConfiguration();
        }

        try {
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(jsonString);

            int rows = ((Number) json.get("rows")).intValue();
            int cols = ((Number) json.get("cols")).intValue();

            DesktopGridConfiguration config = new DesktopGridConfiguration(rows, cols);

            JSONObject panels = (JSONObject) json.get("panels");
            if (panels != null) {
                for (Object key : panels.keySet()) {
                    String panelId = (String) key;
                    JSONObject posJson = (JSONObject) panels.get(panelId);
                    GridPosition pos = GridPosition.fromJson(posJson);
                    if (pos != null) {
                        config.setPanelPosition(panelId, pos);
                    }
                }
            }

            return config;
        } catch (Exception e) {
            // Return default configuration on parse error
            return DesktopLayoutPreset.CLASSIC.toConfiguration();
        }
    }

    /**
     * Applies visibility settings to this configuration.
     *
     * @param lastChangedVisible visibility for last changed panel
     * @param dueVisible visibility for due panel
     * @param taggedVisible visibility for tagged panel
     */
    public void applyVisibility(boolean lastChangedVisible, boolean dueVisible, boolean taggedVisible) {
        GridPosition pos = panelPositions.get(DesktopLayoutPreset.PANEL_LASTCHANGED);
        if (pos != null) {
            pos.setVisible(lastChangedVisible);
        }
        pos = panelPositions.get(DesktopLayoutPreset.PANEL_DUE);
        if (pos != null) {
            pos.setVisible(dueVisible);
        }
        pos = panelPositions.get(DesktopLayoutPreset.PANEL_TAGGED);
        if (pos != null) {
            pos.setVisible(taggedVisible);
        }
    }

    @Override
    public String toString() {
        return "DesktopGridConfiguration[rows=" + rows + ", cols=" + cols
                + ", panels=" + panelPositions.size() + "]";
    }
}
