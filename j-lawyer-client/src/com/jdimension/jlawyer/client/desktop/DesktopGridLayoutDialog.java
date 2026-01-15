/*
 *                     GNU AFFERO GENERAL PUBLIC LICENSE
 *                        Version 3, 19 November 2007
 *
 *  Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
 *  Everyone is permitted to copy and distribute verbatim copies
 *  of this license document, but changing it is not allowed.
 */
package com.jdimension.jlawyer.client.desktop;

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
        labelToPanelId.put(PANEL_LASTCHANGED_LABEL, DesktopLayoutPreset.PANEL_LASTCHANGED);
        labelToPanelId.put(PANEL_DUE_LABEL, DesktopLayoutPreset.PANEL_DUE);
        labelToPanelId.put(PANEL_TAGGED_LABEL, DesktopLayoutPreset.PANEL_TAGGED);

        initComponents();
        loadCurrentSettings();

        setSize(580, 400);
        setMinimumSize(new Dimension(580, 350));
        FrameUtils.centerDialog(this, parent);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        // Main content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));

        // Preset selection
        JPanel presetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        presetPanel.add(new JLabel("Vorlage:"));
        cmbPreset = new JComboBox<>();
        cmbPreset.setModel(new DefaultComboBoxModel<>(DesktopLayoutPreset.values()));
        cmbPreset.addActionListener(e -> applyPresetSelection());
        presetPanel.add(cmbPreset);
        contentPanel.add(presetPanel);

        contentPanel.add(Box.createVerticalStrut(10));

        // Grid dimensions
        JPanel gridPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        gridPanel.setBorder(BorderFactory.createTitledBorder("Grid-Größe"));
        gridPanel.add(new JLabel("Zeilen:"));
        spinRows = new JSpinner(new SpinnerNumberModel(2, 1, 3, 1));
        spinRows.addChangeListener(e -> onGridSizeChanged());
        gridPanel.add(spinRows);
        gridPanel.add(Box.createHorizontalStrut(20));
        gridPanel.add(new JLabel("Spalten:"));
        spinCols = new JSpinner(new SpinnerNumberModel(2, 1, 3, 1));
        spinCols.addChangeListener(e -> onGridSizeChanged());
        gridPanel.add(spinCols);
        contentPanel.add(gridPanel);

        contentPanel.add(Box.createVerticalStrut(10));

        // Grid cell assignment
        JPanel assignmentContainer = new JPanel(new BorderLayout());
        assignmentContainer.setBorder(BorderFactory.createTitledBorder("Panel-Zuweisung"));
        pnlGridAssignment = new JPanel();
        pnlGridAssignment.setPreferredSize(new Dimension(400, 150));
        assignmentContainer.add(pnlGridAssignment, BorderLayout.CENTER);
        contentPanel.add(assignmentContainer);

        add(contentPanel, BorderLayout.CENTER);

        // Button panel with BorderLayout: Reset on left, other buttons on right
        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 15, 15));

        JButton btnReset = new JButton("Zurücksetzen");
        btnReset.setIcon(new ImageIcon(getClass().getResource("/icons/reload.png")));
        btnReset.addActionListener(e -> resetToDefault());
        buttonPanel.add(btnReset, BorderLayout.WEST);

        JPanel rightButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
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

        buttonPanel.add(rightButtonPanel, BorderLayout.EAST);

        add(buttonPanel, BorderLayout.SOUTH);
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

        pnlGridAssignment.setLayout(new GridLayout(rows, cols, 5, 5));

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                JPanel cellPanel = new JPanel(new BorderLayout(2, 2));
                cellPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

                JLabel posLabel = new JLabel("Zelle " + (r + 1) + "," + (c + 1), JLabel.CENTER);
                posLabel.setFont(posLabel.getFont().deriveFont(9f));
                cellPanel.add(posLabel, BorderLayout.NORTH);

                JComboBox<String> cmbPanel = new JComboBox<>();
                cmbPanel.addItem(EMPTY_CELL);
                cmbPanel.addItem(PANEL_LASTCHANGED_LABEL);
                cmbPanel.addItem(PANEL_DUE_LABEL);
                cmbPanel.addItem(PANEL_TAGGED_LABEL);
                cmbPanel.setSelectedItem(EMPTY_CELL);
                cmbPanel.addActionListener(e -> {
                    if (!updatingComboBoxes) {
                        onCellAssignmentChanged(cmbPanel);
                    }
                });
                cellPanel.add(cmbPanel, BorderLayout.CENTER);

                cellComboBoxes.add(cmbPanel);
                pnlGridAssignment.add(cellPanel);
            }
        }

        pnlGridAssignment.revalidate();
        pnlGridAssignment.repaint();
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
                    DesktopLayoutPreset.PANEL_TAGGED
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
                DesktopLayoutPreset.PANEL_TAGGED
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
