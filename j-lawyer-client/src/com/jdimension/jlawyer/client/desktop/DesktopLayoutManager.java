/*
 *                     GNU AFFERO GENERAL PUBLIC LICENSE
 *                        Version 3, 19 November 2007
 *
 *  Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
 *  Everyone is permitted to copy and distribute verbatim copies
 *  of this license document, but changing it is not allowed.
 */
package com.jdimension.jlawyer.client.desktop;

import com.jdimension.jlawyer.client.settings.ClientSettings;
import com.jdimension.jlawyer.client.settings.UserSettings;
import com.jdimension.jlawyer.client.utils.ComponentUtils;
import com.jdimension.jlawyer.server.services.settings.UserSettingsKeys;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import org.apache.log4j.Logger;

/**
 * Manages the dynamic layout of the DesktopPanel.
 * Handles grid configuration, SplitPane tree generation, and layout persistence.
 *
 * @author jens
 */
public class DesktopLayoutManager {

    private static final Logger log = Logger.getLogger(DesktopLayoutManager.class.getName());

    private final Class<?> containerClass;
    private DesktopGridConfiguration configuration;
    private final Map<String, JComponent> panelContents;
    private final Map<String, String> panelTitles;
    private final List<JSplitPane> generatedSplitPanes;
    private JComponent currentLayout;

    /**
     * Creates a new layout manager.
     *
     * @param containerClass the class to use for settings namespace
     */
    public DesktopLayoutManager(Class<?> containerClass) {
        this.containerClass = containerClass;
        this.panelContents = new HashMap<>();
        this.panelTitles = new HashMap<>();
        this.generatedSplitPanes = new ArrayList<>();
        this.configuration = loadConfiguration();
    }

    /**
     * Registers a panel with its content component.
     *
     * @param panelId the panel identifier (use DesktopLayoutPreset constants)
     * @param title the display title for the panel
     * @param content the panel's content component
     */
    public void registerPanel(String panelId, String title, JComponent content) {
        panelContents.put(panelId, content);
        panelTitles.put(panelId, title);
    }

    /**
     * Gets the current grid configuration.
     *
     * @return the configuration
     */
    public DesktopGridConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Sets a new grid configuration and rebuilds the layout.
     *
     * @param configuration the new configuration
     */
    public void setConfiguration(DesktopGridConfiguration configuration) {
        this.configuration = configuration;
        saveConfiguration();
    }

    /**
     * Applies a preset and saves the configuration.
     *
     * @param preset the preset to apply
     */
    public void applyPreset(DesktopLayoutPreset preset) {
        this.configuration = preset.toConfiguration();
        // Preserve current visibility settings
        applyVisibilityFromSettings();
        saveConfiguration();
    }

    /**
     * Toggles the visibility of a panel.
     *
     * @param panelId the panel identifier
     * @return true if the toggle was successful, false if it would hide the last panel
     */
    public boolean togglePanelVisibility(String panelId) {
        GridPosition pos = configuration.getPanelPosition(panelId);
        if (pos == null) {
            return false;
        }

        // Check if this would hide the last visible panel
        if (pos.isVisible() && configuration.getVisiblePanelCount() <= 1) {
            return false;
        }

        pos.setVisible(!pos.isVisible());
        saveConfiguration();
        saveVisibilitySettings();
        return true;
    }

    /**
     * Sets the visibility of a specific panel.
     *
     * @param panelId the panel identifier
     * @param visible the new visibility
     * @return true if successful, false if it would hide the last panel
     */
    public boolean setPanelVisibility(String panelId, boolean visible) {
        GridPosition pos = configuration.getPanelPosition(panelId);
        if (pos == null) {
            return false;
        }

        // Check if this would hide the last visible panel
        if (!visible && pos.isVisible() && configuration.getVisiblePanelCount() <= 1) {
            return false;
        }

        pos.setVisible(visible);
        saveConfiguration();
        saveVisibilitySettings();
        return true;
    }

    /**
     * Checks if a panel is visible.
     *
     * @param panelId the panel identifier
     * @return true if visible
     */
    public boolean isPanelVisible(String panelId) {
        GridPosition pos = configuration.getPanelPosition(panelId);
        return pos != null && pos.isVisible();
    }

    /**
     * Builds the layout component based on the current configuration.
     *
     * @param splitPaneColor optional color for split pane dividers
     * @return the layout component (JSplitPane tree, JTabbedPane, or single panel)
     */
    public JComponent buildLayout(Color splitPaneColor) {
        // Clear previously generated split panes
        generatedSplitPanes.clear();

        // Collect visible panels
        List<String> visiblePanelIds = new ArrayList<>();
        for (String panelId : new String[]{
                DesktopLayoutPreset.PANEL_LASTCHANGED,
                DesktopLayoutPreset.PANEL_DUE,
                DesktopLayoutPreset.PANEL_TAGGED}) {
            if (isPanelVisible(panelId) && panelContents.containsKey(panelId)) {
                visiblePanelIds.add(panelId);
            }
        }

        if (visiblePanelIds.isEmpty()) {
            // Should not happen, but return empty panel
            currentLayout = new JPanel();
            return currentLayout;
        }

        if (visiblePanelIds.size() == 1) {
            // Single panel - no splits needed
            currentLayout = panelContents.get(visiblePanelIds.get(0));
            return currentLayout;
        }

        if (configuration.isCompactLayout()) {
            // 1x1 grid - use tabbed pane
            currentLayout = buildTabbedPane(visiblePanelIds);
            return currentLayout;
        }

        // Build SplitPane tree
        currentLayout = buildSplitPaneTree(visiblePanelIds, splitPaneColor);
        return currentLayout;
    }

    /**
     * Gets the list of generated SplitPanes for ratio persistence.
     *
     * @return list of SplitPanes
     */
    public List<JSplitPane> getGeneratedSplitPanes() {
        return generatedSplitPanes;
    }

    /**
     * Restores SplitPane ratios from settings.
     */
    public void restoreSplitPaneRatios() {
        int index = 0;
        for (JSplitPane splitPane : generatedSplitPanes) {
            ComponentUtils.restoreSplitPaneRatio(splitPane, containerClass, "grid_split_" + index);
            index++;
        }
    }

    /**
     * Sets up persistence for SplitPane ratios.
     */
    public void persistSplitPaneRatios() {
        int index = 0;
        for (JSplitPane splitPane : generatedSplitPanes) {
            ComponentUtils.persistSplitPaneRatio(splitPane, containerClass, "grid_split_" + index);
            index++;
        }
    }

    // ========== Private Methods ==========

    private DesktopGridConfiguration loadConfiguration() {
        try {
            UserSettings settings = UserSettings.getInstance();
            String layoutJson = settings.getSetting(UserSettingsKeys.CONF_DESKTOP_PANEL_LAYOUT, "");

            DesktopGridConfiguration config;
            if (layoutJson.isEmpty()) {
                // Use default CLASSIC layout
                config = DesktopLayoutPreset.CLASSIC.toConfiguration();
            } else {
                config = DesktopGridConfiguration.fromJsonString(layoutJson);
            }

            // Apply visibility from individual settings
            applyVisibilityFromSettings(config);

            return config;
        } catch (Exception e) {
            log.error("Error loading desktop layout configuration", e);
            return DesktopLayoutPreset.CLASSIC.toConfiguration();
        }
    }

    private void applyVisibilityFromSettings() {
        applyVisibilityFromSettings(this.configuration);
    }

    private void applyVisibilityFromSettings(DesktopGridConfiguration config) {
        try {
            UserSettings settings = UserSettings.getInstance();

            boolean lastChangedVisible = !"false".equals(
                    settings.getSetting(UserSettingsKeys.CONF_DESKTOP_SECTION_LASTCHANGED_VISIBLE, "true"));
            boolean dueVisible = !"false".equals(
                    settings.getSetting(UserSettingsKeys.CONF_DESKTOP_SECTION_DUE_VISIBLE, "true"));
            boolean taggedVisible = !"false".equals(
                    settings.getSetting(UserSettingsKeys.CONF_DESKTOP_SECTION_TAGGED_VISIBLE, "true"));

            config.applyVisibility(lastChangedVisible, dueVisible, taggedVisible);
        } catch (Exception e) {
            log.error("Error loading visibility settings", e);
        }
    }

    private void saveConfiguration() {
        try {
            UserSettings settings = UserSettings.getInstance();
            settings.setSetting(UserSettingsKeys.CONF_DESKTOP_PANEL_LAYOUT, configuration.toJsonString());
        } catch (Exception e) {
            log.error("Error saving desktop layout configuration", e);
        }
    }

    private void saveVisibilitySettings() {
        try {
            UserSettings settings = UserSettings.getInstance();

            GridPosition pos = configuration.getPanelPosition(DesktopLayoutPreset.PANEL_LASTCHANGED);
            if (pos != null) {
                settings.setSetting(UserSettingsKeys.CONF_DESKTOP_SECTION_LASTCHANGED_VISIBLE,
                        String.valueOf(pos.isVisible()));
            }

            pos = configuration.getPanelPosition(DesktopLayoutPreset.PANEL_DUE);
            if (pos != null) {
                settings.setSetting(UserSettingsKeys.CONF_DESKTOP_SECTION_DUE_VISIBLE,
                        String.valueOf(pos.isVisible()));
            }

            pos = configuration.getPanelPosition(DesktopLayoutPreset.PANEL_TAGGED);
            if (pos != null) {
                settings.setSetting(UserSettingsKeys.CONF_DESKTOP_SECTION_TAGGED_VISIBLE,
                        String.valueOf(pos.isVisible()));
            }
        } catch (Exception e) {
            log.error("Error saving visibility settings", e);
        }
    }

    private JTabbedPane buildTabbedPane(List<String> visiblePanelIds) {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.TOP);

        for (String panelId : visiblePanelIds) {
            JComponent content = panelContents.get(panelId);
            String title = panelTitles.getOrDefault(panelId, panelId);
            if (content != null) {
                tabbedPane.addTab(title, content);
            }
        }

        return tabbedPane;
    }

    private JComponent buildSplitPaneTree(List<String> visiblePanelIds, Color splitPaneColor) {
        int rows = configuration.getRows();
        int cols = configuration.getCols();

        // Build a grid array with panels placed at their positions
        JComponent[][] grid = new JComponent[rows][cols];
        boolean[][] occupied = new boolean[rows][cols];

        // Place panels in grid according to their positions
        for (String panelId : visiblePanelIds) {
            GridPosition pos = configuration.getPanelPosition(panelId);
            if (pos == null || !pos.isVisible()) {
                continue;
            }

            JComponent content = panelContents.get(panelId);
            if (content == null) {
                continue;
            }

            int row = Math.min(pos.getRow(), rows - 1);
            int col = Math.min(pos.getCol(), cols - 1);

            // Mark cells as occupied (for spanning)
            for (int r = row; r < Math.min(row + pos.getRowSpan(), rows); r++) {
                for (int c = col; c < Math.min(col + pos.getColSpan(), cols); c++) {
                    occupied[r][c] = true;
                }
            }

            grid[row][col] = content;
        }

        // Build the SplitPane tree recursively
        return buildSplitPaneTreeRecursive(grid, occupied, 0, 0, rows, cols, splitPaneColor);
    }

    private JComponent buildSplitPaneTreeRecursive(JComponent[][] grid, boolean[][] occupied,
            int startRow, int startCol, int numRows, int numCols, Color splitPaneColor) {

        // Base case: single cell
        if (numRows == 1 && numCols == 1) {
            JComponent content = grid[startRow][startCol];
            if (content != null) {
                return content;
            } else {
                // Empty cell - return transparent placeholder
                JPanel placeholder = new JPanel();
                placeholder.setOpaque(false);
                placeholder.setMinimumSize(new Dimension(0, 0));
                return placeholder;
            }
        }

        // Find the best content in this region
        JComponent singleContent = findSingleContentInRegion(grid, startRow, startCol, numRows, numCols);
        if (singleContent != null) {
            // Only one panel in this region - return it directly
            return singleContent;
        }

        // Split the region
        if (numCols > 1) {
            // Split horizontally
            int leftCols = numCols / 2;
            int rightCols = numCols - leftCols;

            JComponent left = buildSplitPaneTreeRecursive(grid, occupied,
                    startRow, startCol, numRows, leftCols, splitPaneColor);
            JComponent right = buildSplitPaneTreeRecursive(grid, occupied,
                    startRow, startCol + leftCols, numRows, rightCols, splitPaneColor);

            // Check if both sides have content
            if (isEmpty(left) && isEmpty(right)) {
                return createEmptyPlaceholder();
            } else if (isEmpty(left)) {
                return right;
            } else if (isEmpty(right)) {
                return left;
            }

            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
            splitPane.setResizeWeight(0.5);
            splitPane.setOpaque(false);
            splitPane.setBorder(null);
            splitPane.setMinimumSize(new Dimension(0, 0));
            if (splitPaneColor != null) {
                ComponentUtils.decorateSplitPane(splitPane, splitPaneColor);
            }
            generatedSplitPanes.add(splitPane);
            return splitPane;

        } else if (numRows > 1) {
            // Split vertically
            int topRows = numRows / 2;
            int bottomRows = numRows - topRows;

            JComponent top = buildSplitPaneTreeRecursive(grid, occupied,
                    startRow, startCol, topRows, numCols, splitPaneColor);
            JComponent bottom = buildSplitPaneTreeRecursive(grid, occupied,
                    startRow + topRows, startCol, bottomRows, numCols, splitPaneColor);

            // Check if both sides have content
            if (isEmpty(top) && isEmpty(bottom)) {
                return createEmptyPlaceholder();
            } else if (isEmpty(top)) {
                return bottom;
            } else if (isEmpty(bottom)) {
                return top;
            }

            JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom);
            splitPane.setResizeWeight(0.5);
            splitPane.setOpaque(false);
            splitPane.setBorder(null);
            splitPane.setMinimumSize(new Dimension(0, 0));
            if (splitPaneColor != null) {
                ComponentUtils.decorateSplitPane(splitPane, splitPaneColor);
            }
            generatedSplitPanes.add(splitPane);
            return splitPane;
        }

        return createEmptyPlaceholder();
    }

    private JComponent findSingleContentInRegion(JComponent[][] grid, int startRow, int startCol,
            int numRows, int numCols) {
        JComponent found = null;
        int count = 0;

        for (int r = startRow; r < startRow + numRows; r++) {
            for (int c = startCol; c < startCol + numCols; c++) {
                if (grid[r][c] != null) {
                    found = grid[r][c];
                    count++;
                    if (count > 1) {
                        return null; // More than one panel
                    }
                }
            }
        }

        return found;
    }

    private boolean isEmpty(JComponent component) {
        if (component == null) {
            return true;
        }
        // Check if it's an empty placeholder
        if (component instanceof JPanel) {
            JPanel panel = (JPanel) component;
            return panel.getComponentCount() == 0 && !panel.isOpaque();
        }
        return false;
    }

    private JPanel createEmptyPlaceholder() {
        JPanel placeholder = new JPanel();
        placeholder.setOpaque(false);
        placeholder.setMinimumSize(new Dimension(0, 0));
        return placeholder;
    }
}
