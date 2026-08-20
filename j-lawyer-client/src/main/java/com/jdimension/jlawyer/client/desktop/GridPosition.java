/*
 *                     GNU AFFERO GENERAL PUBLIC LICENSE
 *                        Version 3, 19 November 2007
 *
 *  Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
 *  Everyone is permitted to copy and distribute verbatim copies
 *  of this license document, but changing it is not allowed.
 */
package com.jdimension.jlawyer.client.desktop;

import org.json.simple.JSONObject;

/**
 * Represents a panel's position within the desktop grid layout.
 * Stores row, column, spanning information and visibility state.
 *
 * @author jens
 */
public class GridPosition {

    private int row;
    private int col;
    private int rowSpan;
    private int colSpan;
    private boolean visible;

    /**
     * Creates a new grid position with default spanning (1x1) and visible.
     *
     * @param row the row index (0-based)
     * @param col the column index (0-based)
     */
    public GridPosition(int row, int col) {
        this(row, col, 1, 1, true);
    }

    /**
     * Creates a new grid position with specified spanning and visible.
     *
     * @param row the row index (0-based)
     * @param col the column index (0-based)
     * @param rowSpan number of rows to span (minimum 1)
     * @param colSpan number of columns to span (minimum 1)
     */
    public GridPosition(int row, int col, int rowSpan, int colSpan) {
        this(row, col, rowSpan, colSpan, true);
    }

    /**
     * Creates a new grid position with all parameters specified.
     *
     * @param row the row index (0-based)
     * @param col the column index (0-based)
     * @param rowSpan number of rows to span (minimum 1)
     * @param colSpan number of columns to span (minimum 1)
     * @param visible whether the panel is visible
     */
    public GridPosition(int row, int col, int rowSpan, int colSpan, boolean visible) {
        this.row = row;
        this.col = col;
        this.rowSpan = Math.max(1, rowSpan);
        this.colSpan = Math.max(1, colSpan);
        this.visible = visible;
    }

    /**
     * Creates a copy of this grid position.
     *
     * @return a new GridPosition with the same values
     */
    public GridPosition copy() {
        return new GridPosition(row, col, rowSpan, colSpan, visible);
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getCol() {
        return col;
    }

    public void setCol(int col) {
        this.col = col;
    }

    public int getRowSpan() {
        return rowSpan;
    }

    public void setRowSpan(int rowSpan) {
        this.rowSpan = Math.max(1, rowSpan);
    }

    public int getColSpan() {
        return colSpan;
    }

    public void setColSpan(int colSpan) {
        this.colSpan = Math.max(1, colSpan);
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * Checks if this position occupies the given cell (considering spanning).
     *
     * @param checkRow the row to check
     * @param checkCol the column to check
     * @return true if this position occupies the cell
     */
    public boolean occupiesCell(int checkRow, int checkCol) {
        return checkRow >= row && checkRow < row + rowSpan
                && checkCol >= col && checkCol < col + colSpan;
    }

    /**
     * Converts this position to a JSON object for persistence.
     *
     * @return JSON representation
     */
    @SuppressWarnings("unchecked")
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("row", row);
        json.put("col", col);
        json.put("rowSpan", rowSpan);
        json.put("colSpan", colSpan);
        json.put("visible", visible);
        return json;
    }

    /**
     * Creates a GridPosition from a JSON object.
     *
     * @param json the JSON object
     * @return the GridPosition, or null if parsing fails
     */
    public static GridPosition fromJson(JSONObject json) {
        if (json == null) {
            return null;
        }
        try {
            int row = ((Number) json.get("row")).intValue();
            int col = ((Number) json.get("col")).intValue();
            int rowSpan = json.containsKey("rowSpan") ? ((Number) json.get("rowSpan")).intValue() : 1;
            int colSpan = json.containsKey("colSpan") ? ((Number) json.get("colSpan")).intValue() : 1;
            boolean visible = json.containsKey("visible") ? (Boolean) json.get("visible") : true;
            return new GridPosition(row, col, rowSpan, colSpan, visible);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return "GridPosition[row=" + row + ", col=" + col + ", rowSpan=" + rowSpan
                + ", colSpan=" + colSpan + ", visible=" + visible + "]";
    }
}
