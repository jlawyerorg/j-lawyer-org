/*
 *                     GNU AFFERO GENERAL PUBLIC LICENSE
 *                        Version 3, 19 November 2007
 *
 *  Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
 *  Everyone is permitted to copy and distribute verbatim copies
 *  of this license document, but changing it is not allowed.
 */
package com.jdimension.jlawyer.client.desktop;

import com.jdimension.jlawyer.client.ui.theme.SectionCardPanel;
import com.jdimension.jlawyer.client.ui.theme.UiColors;
import com.jdimension.jlawyer.client.utils.FrameUtils;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

/**
 * Dialog for configuring the DesktopPanel grid layout.
 * Allows users to select presets, adjust grid dimensions, and assign panels to cells.
 *
 * @author jens
 */
public class DesktopGridLayoutDialog extends JDialog {

    private static final String EMPTY_CELL = "(Leer)";
    private static final String PANEL_LASTCHANGED_LABEL = "Zuletzt geändert";
    private static final String PANEL_DUE_LABEL = "Fällig";
    private static final String PANEL_TAGGED_LABEL = "Nach Etikett";
    private static final String PANEL_MESSAGES_TO_ME_LABEL = "Nachrichten an mich";
    private static final String PANEL_MESSAGES_TO_OTHERS_LABEL = "Nachrichten an andere";
    private static final String PANEL_INVOICES_LABEL = "Offene Rechnungen";

    private static final Color PAGE_BACKGROUND = new Color(236, 241, 246);
    private static final Color MUTED_TEXT = new Color(100, 116, 139);
    private static final Color CELL_EMPTY = new Color(248, 250, 252);
    private static final Color CELL_ASSIGNED = new Color(234, 243, 252);
    private static final Color CELL_ASSIGNED_BORDER = new Color(14, 114, 181);

    private final DesktopLayoutManager layoutManager;
    private final Runnable onLayoutChanged;
    private DesktopGridConfiguration originalConfiguration;

    // Panel ID to Label mapping
    private final Map<String, String> panelIdToLabel = new HashMap<>();
    private final Map<String, String> labelToPanelId = new HashMap<>();

    // UI Components
    private JComboBox<DesktopLayoutPreset> cmbPreset;
    private JSpinner spinRows;
    private JSpinner spinCols;
    private JPanel pnlGridAssignment;
    private List<JComboBox<String>> cellComboBoxes;
    private boolean updatingComboBoxes = false;

    private boolean dialogResult = false;

    /**
     * Creates a new layout configuration dialog.
     *
     * @param parent the parent frame
     * @param layoutManager the layout manager to configure
     * @param onLayoutChanged callback to invoke when layout changes
     */
    public DesktopGridLayoutDialog(JFrame parent, DesktopLayoutManager layoutManager, Runnable onLayoutChanged) {
        super(parent, "Desktop Layout anpassen", true);
        this.layoutManager = layoutManager;
        this.onLayoutChanged = onLayoutChanged;
        this.originalConfiguration = layoutManager.getConfiguration().copy();
        this.cellComboBoxes = new ArrayList<>();

        // Initialize mappings
        panelIdToLabel.put(DesktopLayoutPreset.PANEL_LASTCHANGED, PANEL_LASTCHANGED_LABEL);
        panelIdToLabel.put(DesktopLayoutPreset.PANEL_DUE, PANEL_DUE_LABEL);
        panelIdToLabel.put(DesktopLayoutPreset.PANEL_TAGGED, PANEL_TAGGED_LABEL);
        panelIdToLabel.put(DesktopLayoutPreset.PANEL_MESSAGES_TO_ME, PANEL_MESSAGES_TO_ME_LABEL);
        panelIdToLabel.put(DesktopLayoutPreset.PANEL_MESSAGES_TO_OTHERS, PANEL_MESSAGES_TO_OTHERS_LABEL);
        panelIdToLabel.put(DesktopLayoutPreset.PANEL_INVOICES, PANEL_INVOICES_LABEL);
        labelToPanelId.put(PANEL_LASTCHANGED_LABEL, DesktopLayoutPreset.PANEL_LASTCHANGED);
        labelToPanelId.put(PANEL_DUE_LABEL, DesktopLayoutPreset.PANEL_DUE);
        labelToPanelId.put(PANEL_TAGGED_LABEL, DesktopLayoutPreset.PANEL_TAGGED);
        labelToPanelId.put(PANEL_MESSAGES_TO_ME_LABEL, DesktopLayoutPreset.PANEL_MESSAGES_TO_ME);
        labelToPanelId.put(PANEL_MESSAGES_TO_OTHERS_LABEL, DesktopLayoutPreset.PANEL_MESSAGES_TO_OTHERS);
        labelToPanelId.put(PANEL_INVOICES_LABEL, DesktopLayoutPreset.PANEL_INVOICES);

        initComponents();
        loadCurrentSettings();

        setSize(700, 450);
        setMinimumSize(new Dimension(580, 350));
        FrameUtils.centerDialog(this, parent);
    }

    private void initComponents() {
        // design kit: light cards on a soft page background
        JPanel page = new JPanel(new BorderLayout(0, 0));
        page.setBackground(PAGE_BACKGROUND);
        setContentPane(page);

        JPanel contentPanel = new JPanel();
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(14, 14, 6, 14));

        // Preset selection and grid size side by side
        JPanel topRow = new JPanel(new GridLayout(1, 2, 12, 0));
        topRow.setOpaque(false);

        SectionCardPanel presetCard = new SectionCardPanel();
        presetCard.setTitle("Vorlage");
        presetCard.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        cmbPreset = new JComboBox<>();
        cmbPreset.setModel(new DefaultComboBoxModel<>(DesktopLayoutPreset.values()));
        cmbPreset.addActionListener(e -> applyPresetSelection());
        presetCard.add(cmbPreset);
        topRow.add(presetCard);

        SectionCardPanel gridCard = new SectionCardPanel();
        gridCard.setTitle("Raster");
        gridCard.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        gridCard.add(mutedLabel("Zeilen"));
        gridCard.add(Box.createHorizontalStrut(8));
        spinRows = new JSpinner(new SpinnerNumberModel(2, 1, 3, 1));
        spinRows.addChangeListener(e -> onGridSizeChanged());
        gridCard.add(spinRows);
        gridCard.add(Box.createHorizontalStrut(20));
        gridCard.add(mutedLabel("Spalten"));
        gridCard.add(Box.createHorizontalStrut(8));
        spinCols = new JSpinner(new SpinnerNumberModel(2, 1, 3, 1));
        spinCols.addChangeListener(e -> onGridSizeChanged());
        gridCard.add(spinCols);
        topRow.add(gridCard);

        topRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, topRow.getPreferredSize().height));
        contentPanel.add(topRow);
        contentPanel.add(Box.createVerticalStrut(12));

        // Grid cell assignment
        SectionCardPanel assignmentCard = new SectionCardPanel();
        assignmentCard.setTitle("Bereiche zuweisen");
        assignmentCard.setLayout(new BorderLayout());
        pnlGridAssignment = new JPanel();
        pnlGridAssignment.setOpaque(false);
        pnlGridAssignment.setPreferredSize(new Dimension(400, 150));
        assignmentCard.add(pnlGridAssignment, BorderLayout.CENTER);
        contentPanel.add(assignmentCard);

        page.add(contentPanel, BorderLayout.CENTER);

        // Button panel with BorderLayout: Reset on left, other buttons on right
        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(6, 14, 14, 14));

        JButton btnReset = new JButton("Zurücksetzen");
        btnReset.setIcon(new ImageIcon(getClass().getResource("/icons/reload.png")));
        btnReset.addActionListener(e -> resetToDefault());
        buttonPanel.add(btnReset, BorderLayout.WEST);

        JPanel rightButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightButtonPanel.setOpaque(false);
        JButton btnApply = new JButton("Anwenden");
        btnApply.setIcon(new ImageIcon(getClass().getResource("/icons/agt_update_misc.png")));
        btnApply.addActionListener(e -> applyChanges());
        rightButtonPanel.add(btnApply);

        JButton btnCancel = new JButton("Abbrechen");
        btnCancel.setIcon(new ImageIcon(getClass().getResource("/icons/cancel.png")));
        btnCancel.addActionListener(e -> cancelChanges());
        rightButtonPanel.add(btnCancel);

        JButton btnOk = new JButton("OK");
        btnOk.setIcon(new ImageIcon(getClass().getResource("/icons/agt_action_success.png")));
        btnOk.addActionListener(e -> confirmChanges());
        rightButtonPanel.add(btnOk);
        // the primary action
        getRootPane().setDefaultButton(btnOk);

        for (JButton b : new JButton[]{btnReset, btnApply, btnCancel, btnOk}) {
            b.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "arc: 10; margin: 4,12,4,12");
        }

        buttonPanel.add(rightButtonPanel, BorderLayout.EAST);

        page.add(buttonPanel, BorderLayout.SOUTH);
    }

    private static JLabel mutedLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(MUTED_TEXT);
        return l;
    }

    /**
     * One cell of the grid: a light rounded tile, outlined in blue while a section is assigned.
     */
    private static class CellTile extends JPanel {

        private boolean assigned = false;

        CellTile() {
            super(new BorderLayout(0, 4));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(8, 10, 10, 10));
        }

        void setAssigned(boolean assigned) {
            if (this.assigned != assigned) {
                this.assigned = assigned;
                repaint();
            }
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(this.assigned ? CELL_ASSIGNED : CELL_EMPTY);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            g2.setColor(this.assigned ? CELL_ASSIGNED_BORDER : UiColors.SEPARATOR);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private void loadCurrentSettings() {
        DesktopGridConfiguration config = layoutManager.getConfiguration();

        spinRows.setValue(config.getRows());
        spinCols.setValue(config.getCols());

        // Try to find matching preset
        cmbPreset.setSelectedItem(null);
        for (DesktopLayoutPreset preset : DesktopLayoutPreset.values()) {
            if (preset.getRows() == config.getRows() && preset.getCols() == config.getCols()) {
                cmbPreset.setSelectedItem(preset);
                break;
            }
        }

        rebuildGridAssignment();
        loadPanelAssignmentsFromConfig(config);
    }

    private void applyPresetSelection() {
        DesktopLayoutPreset preset = (DesktopLayoutPreset) cmbPreset.getSelectedItem();
        if (preset != null) {
            spinRows.setValue(preset.getRows());
            spinCols.setValue(preset.getCols());
            rebuildGridAssignment();
            // Apply preset's panel positions
            DesktopGridConfiguration presetConfig = preset.toConfiguration();
            loadPanelAssignmentsFromConfig(presetConfig);
        }
    }

    private void onGridSizeChanged() {
        rebuildGridAssignment();
    }

    private void rebuildGridAssignment() {
        pnlGridAssignment.removeAll();
        cellComboBoxes.clear();

        int rows = (Integer) spinRows.getValue();
        int cols = (Integer) spinCols.getValue();

        pnlGridAssignment.setLayout(new GridLayout(rows, cols, 10, 10));

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                CellTile cellPanel = new CellTile();

                JLabel posLabel = mutedLabel("Zeile " + (r + 1) + " · Spalte " + (c + 1));
                posLabel.setFont(posLabel.getFont().deriveFont(posLabel.getFont().getSize2D() - 1f));
                // label and selection stay at the top of the tile instead of stretching with it
                JPanel cellContent = new JPanel(new BorderLayout(0, 4));
                cellContent.setOpaque(false);
                cellContent.add(posLabel, BorderLayout.NORTH);
                cellPanel.add(cellContent, BorderLayout.NORTH);

                JComboBox<String> cmbPanel = new JComboBox<>();
                cmbPanel.addItem(EMPTY_CELL);
                cmbPanel.addItem(PANEL_LASTCHANGED_LABEL);
                cmbPanel.addItem(PANEL_DUE_LABEL);
                cmbPanel.addItem(PANEL_TAGGED_LABEL);
                cmbPanel.addItem(PANEL_MESSAGES_TO_ME_LABEL);
                cmbPanel.addItem(PANEL_MESSAGES_TO_OTHERS_LABEL);
                cmbPanel.addItem(PANEL_INVOICES_LABEL);
                cmbPanel.setSelectedItem(EMPTY_CELL);
                cmbPanel.addActionListener(e -> {
                    if (!updatingComboBoxes) {
                        onCellAssignmentChanged(cmbPanel);
                    }
                    updateCellTiles();
                });
                cellContent.add(cmbPanel, BorderLayout.CENTER);

                cellComboBoxes.add(cmbPanel);
                pnlGridAssignment.add(cellPanel);
            }
        }

        pnlGridAssignment.revalidate();
        pnlGridAssignment.repaint();
    }

    /**
     * Outlines the cells that have a section assigned.
     */
    private void updateCellTiles() {
        for (JComboBox<String> combo : cellComboBoxes) {
            CellTile tile = (CellTile) javax.swing.SwingUtilities.getAncestorOfClass(CellTile.class, combo);
            if (tile != null) {
                tile.setAssigned(combo.getSelectedItem() != null && !EMPTY_CELL.equals(combo.getSelectedItem()));
            }
        }
    }

    private void loadPanelAssignmentsFromConfig(DesktopGridConfiguration config) {
        updatingComboBoxes = true;
        try {
            int cols = (Integer) spinCols.getValue();

            // Reset all to empty
            for (JComboBox<String> combo : cellComboBoxes) {
                combo.setSelectedItem(EMPTY_CELL);
            }

            // Set panels according to config
            String[] panelIds = {
                    DesktopLayoutPreset.PANEL_LASTCHANGED,
                    DesktopLayoutPreset.PANEL_DUE,
                    DesktopLayoutPreset.PANEL_TAGGED,
                    DesktopLayoutPreset.PANEL_MESSAGES_TO_ME,
                    DesktopLayoutPreset.PANEL_MESSAGES_TO_OTHERS,
                    DesktopLayoutPreset.PANEL_INVOICES
            };

            for (String panelId : panelIds) {
                GridPosition pos = config.getPanelPosition(panelId);
                if (pos != null && pos.isVisible()) {
                    int index = pos.getRow() * cols + pos.getCol();
                    if (index >= 0 && index < cellComboBoxes.size()) {
                        String label = panelIdToLabel.get(panelId);
                        cellComboBoxes.get(index).setSelectedItem(label);
                    }
                }
            }
        } finally {
            updatingComboBoxes = false;
        }
        updateCellTiles();
    }

    private void onCellAssignmentChanged(JComboBox<String> changedCombo) {
        String selectedLabel = (String) changedCombo.getSelectedItem();

        // If a panel was selected (not empty), remove it from other cells
        if (selectedLabel != null && !EMPTY_CELL.equals(selectedLabel)) {
            updatingComboBoxes = true;
            try {
                for (JComboBox<String> combo : cellComboBoxes) {
                    if (combo != changedCombo && selectedLabel.equals(combo.getSelectedItem())) {
                        combo.setSelectedItem(EMPTY_CELL);
                    }
                }
            } finally {
                updatingComboBoxes = false;
            }
        }
    }

    private boolean validateAssignment() {
        // Check that at least one panel is assigned
        int assignedCount = 0;
        for (JComboBox<String> combo : cellComboBoxes) {
            String selected = (String) combo.getSelectedItem();
            if (selected != null && !EMPTY_CELL.equals(selected)) {
                assignedCount++;
            }
        }

        if (assignedCount == 0) {
            JOptionPane.showMessageDialog(this,
                    "Mindestens ein Panel muss einer Zelle zugewiesen sein.",
                    "Hinweis",
                    JOptionPane.WARNING_MESSAGE);
            return false;
        }

        return true;
    }

    private void applyChanges() {
        if (!validateAssignment()) {
            return;
        }
        applyCurrentSettings();
        if (onLayoutChanged != null) {
            onLayoutChanged.run();
        }
    }

    private void applyCurrentSettings() {
        int rows = (Integer) spinRows.getValue();
        int cols = (Integer) spinCols.getValue();

        DesktopGridConfiguration config = new DesktopGridConfiguration(rows, cols);

        // Read panel assignments from combo boxes
        for (int i = 0; i < cellComboBoxes.size(); i++) {
            String selectedLabel = (String) cellComboBoxes.get(i).getSelectedItem();
            if (selectedLabel != null && !EMPTY_CELL.equals(selectedLabel)) {
                String panelId = labelToPanelId.get(selectedLabel);
                if (panelId != null) {
                    int row = i / cols;
                    int col = i % cols;
                    config.setPanelPosition(panelId, new GridPosition(row, col, 1, 1, true));
                }
            }
        }

        // Mark unassigned panels as not visible
        String[] allPanelIds = {
                DesktopLayoutPreset.PANEL_LASTCHANGED,
                DesktopLayoutPreset.PANEL_DUE,
                DesktopLayoutPreset.PANEL_TAGGED,
                DesktopLayoutPreset.PANEL_MESSAGES_TO_ME,
                DesktopLayoutPreset.PANEL_MESSAGES_TO_OTHERS,
                DesktopLayoutPreset.PANEL_INVOICES
        };
        for (String panelId : allPanelIds) {
            if (config.getPanelPosition(panelId) == null) {
                config.setPanelPosition(panelId, new GridPosition(0, 0, 1, 1, false));
            }
        }

        layoutManager.setConfiguration(config);
    }

    private void resetToDefault() {
        layoutManager.applyPreset(DesktopLayoutPreset.CLASSIC);
        loadCurrentSettings();
        if (onLayoutChanged != null) {
            onLayoutChanged.run();
        }
    }

    private void cancelChanges() {
        // Restore original configuration
        layoutManager.setConfiguration(originalConfiguration);
        if (onLayoutChanged != null) {
            onLayoutChanged.run();
        }
        dialogResult = false;
        dispose();
    }

    private void confirmChanges() {
        if (!validateAssignment()) {
            return;
        }
        applyCurrentSettings();
        if (onLayoutChanged != null) {
            onLayoutChanged.run();
        }
        dialogResult = true;
        dispose();
    }

    /**
     * Shows the dialog and returns whether changes were confirmed.
     *
     * @return true if OK was clicked, false if cancelled
     */
    public boolean showDialog() {
        setVisible(true);
        return dialogResult;
    }
}
