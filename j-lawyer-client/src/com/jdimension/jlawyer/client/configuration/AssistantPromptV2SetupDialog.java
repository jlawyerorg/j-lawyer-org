/*
 *                     GNU AFFERO GENERAL PUBLIC LICENSE
 *                        Version 3, 19 November 2007
 *
 *  Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
 *  Everyone is permitted to copy and distribute verbatim copies
 *  of this license document, but changing it is not allowed.
 */
package com.jdimension.jlawyer.client.configuration;

import com.jdimension.jlawyer.ai.AiCapability;
import com.jdimension.jlawyer.ai.AiModel;
import com.jdimension.jlawyer.ai.Configuration;
import com.jdimension.jlawyer.ai.ConfigurationData;
import com.jdimension.jlawyer.ai.ConfigurationUtils;
import com.jdimension.jlawyer.client.assistant.AssistantAccess;
import com.jdimension.jlawyer.client.settings.ClientSettings;
import com.jdimension.jlawyer.client.utils.CaseInsensitiveStringComparator;
import com.jdimension.jlawyer.client.utils.ComponentUtils;
import com.jdimension.jlawyer.client.utils.FrameUtils;
import com.jdimension.jlawyer.persistence.AssistantConfig;
import com.jdimension.jlawyer.persistence.AssistantPrompt;
import com.jdimension.jlawyer.services.JLawyerServiceLocator;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import org.apache.log4j.Logger;
import themes.colors.DefaultColorTheme;

/**
 * V2 dialog for managing assistant prompts with model selection and
 * per-model configuration parameters.
 *
 * @author jens
 */
public class AssistantPromptV2SetupDialog extends javax.swing.JDialog {

    private static final Logger log = Logger.getLogger(AssistantPromptV2SetupDialog.class.getName());

    private static final String MODEL_DEFAULT = "(Standard)";

    private JTable tblPrompts;
    private JTextField txtName;
    private JComboBox<String> cmbRequestType;
    private JComboBox<String> cmbModel;
    private JTextArea taPrompt;
    private JTextArea taSystemPrompt;
    private JPanel pnlConfig;
    private JLabel lblDeductTokens;
    private JButton cmdAdd;
    private JButton cmdRemove;
    private JButton cmdSave;
    private JButton cmdClose;

    private List<AiModel> allModels = new ArrayList<>();
    private Map<String, JTextField> configTextFields = new LinkedHashMap<>();

    private boolean updatingUI = false;

    public AssistantPromptV2SetupDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();

        this.resetDetails();

        this.cmbRequestType.removeAllItems();
        for (String c : AiCapability.capabilities()) {
            this.cmbRequestType.addItem(c);
        }

        this.tblPrompts.setSelectionForeground(DefaultColorTheme.COLOR_LOGO_BLUE);

        ClientSettings settings = ClientSettings.getInstance();
        try {
            JLawyerServiceLocator locator = JLawyerServiceLocator.getInstance(settings.getLookupProperties());

            // Load models from all assistant configs
            Map<AssistantConfig, List<AiModel>> modelsMap = locator.lookupIntegrationServiceRemote().getAssistantModels();
            Map<String, AiModel> deduped = new LinkedHashMap<>();
            for (List<AiModel> models : modelsMap.values()) {
                for (AiModel m : models) {
                    deduped.put(m.getName(), m);
                }
            }
            allModels = new ArrayList<>(deduped.values());

            // Load prompts
            List<AssistantPrompt> assistants = locator.lookupIntegrationServiceRemote().getAllAssistantPrompts();

            this.tblPrompts.setDefaultRenderer(Object.class, new AssistantPromptTableCellRenderer());

            for (AssistantPrompt ap : assistants) {
                ((DefaultTableModel) this.tblPrompts.getModel()).addRow(new Object[]{ap, ap.getRequestType()});
            }
            TableRowSorter<TableModel> sorter = new TableRowSorter<>(this.tblPrompts.getModel());
            sorter.setComparator(0, new Comparator<AssistantPrompt>() {
                @Override
                public int compare(AssistantPrompt o1, AssistantPrompt o2) {
                    if (o1 == null && o2 == null) {
                        return 0;
                    }
                    if (o1 == null) {
                        return 1;
                    }
                    if (o2 == null) {
                        return -1;
                    }

                    String name1 = o1.getName() != null ? o1.getName() : "";
                    String name2 = o2.getName() != null ? o2.getName() : "";

                    return name1.toLowerCase().compareTo(name2.toLowerCase());
                }
            });
            sorter.setComparator(1, new CaseInsensitiveStringComparator());
            this.tblPrompts.setRowSorter(sorter);
            this.tblPrompts.getRowSorter().toggleSortOrder(0);

        } catch (Exception ex) {
            log.error("Error connecting to server", ex);
            JOptionPane.showMessageDialog(this, ex.getMessage(), com.jdimension.jlawyer.client.utils.DesktopUtils.POPUP_TITLE_ERROR, JOptionPane.ERROR_MESSAGE);
            return;
        }

        ComponentUtils.autoSizeColumns(tblPrompts);

        this.cmbRequestType.addActionListener(e -> {
            if (!updatingUI) {
                updateModelComboBox();
            }
        });

        this.cmbModel.addActionListener(e -> {
            if (!updatingUI) {
                updateConfigPanel();
            }
        });
    }

    private void resetDetails() {
        updatingUI = true;
        this.txtName.setText("");
        if (this.cmbRequestType.getItemCount() > 0) {
            this.cmbRequestType.setSelectedIndex(0);
        }
        this.cmbModel.removeAllItems();
        this.cmbModel.addItem(MODEL_DEFAULT);
        this.taPrompt.setText("");
        this.taSystemPrompt.setText("");
        clearConfigPanel();
        updateDeductTokensLabel();
        updatingUI = false;
    }

    private void clearConfigPanel() {
        pnlConfig.removeAll();
        configTextFields.clear();
        pnlConfig.revalidate();
        pnlConfig.repaint();
    }

    private void updateModelComboBox() {
        updatingUI = true;
        String selectedModel = (String) cmbModel.getSelectedItem();
        cmbModel.removeAllItems();
        cmbModel.addItem(MODEL_DEFAULT);

        String selectedRequestType = (String) cmbRequestType.getSelectedItem();
        if (selectedRequestType != null) {
            for (AiModel m : allModels) {
                if (m.getSupportedRequestTypes() != null && m.getSupportedRequestTypes().contains(selectedRequestType)) {
                    cmbModel.addItem(m.getName());
                }
            }
        }

        // Restore selection if still valid
        if (selectedModel != null) {
            for (int i = 0; i < cmbModel.getItemCount(); i++) {
                if (selectedModel.equals(cmbModel.getItemAt(i))) {
                    cmbModel.setSelectedIndex(i);
                    break;
                }
            }
        }
        updatingUI = false;
        updateConfigPanel();
    }

    private void updateDeductTokensLabel() {
        String selectedModelName = (String) cmbModel.getSelectedItem();
        if (selectedModelName == null || MODEL_DEFAULT.equals(selectedModelName)) {
            lblDeductTokens.setText(" ");
            return;
        }
        for (AiModel m : allModels) {
            if (m.getName().equals(selectedModelName)) {
                StringBuilder info = new StringBuilder();
                if (m.isDeductTokens()) {
                    info.append("Nutzung wird vom Guthaben abgezogen");
                } else {
                    info.append("keine Guthabenabbuchung");
                }
                info.append(" | ");
                if (m.isSupportsTools()) {
                    info.append("unterstützt Werkzeuge");
                } else {
                    info.append("keine Werkzeugunterstützung");
                }
                lblDeductTokens.setText(info.toString());
                return;
            }
        }
        lblDeductTokens.setText(" ");
    }

    private void updateConfigPanel() {
        clearConfigPanel();
        updateDeductTokensLabel();

        String selectedModelName = (String) cmbModel.getSelectedItem();
        if (selectedModelName == null || MODEL_DEFAULT.equals(selectedModelName)) {
            return;
        }

        AiModel selectedModel = null;
        for (AiModel m : allModels) {
            if (m.getName().equals(selectedModelName)) {
                selectedModel = m;
                break;
            }
        }

        if (selectedModel == null || selectedModel.getConfigurations() == null || selectedModel.getConfigurations().isEmpty()) {
            return;
        }

        // Parse existing config values from the currently selected prompt
        Map<String, String> existingValues = new HashMap<>();
        int row = tblPrompts.getSelectedRow();
        if (row >= 0) {
            AssistantPrompt ap = (AssistantPrompt) tblPrompts.getValueAt(row, 0);
            if (ap.getConfiguration() != null && !ap.getConfiguration().isEmpty()) {
                List<ConfigurationData> cfgData = ConfigurationUtils.fromProperties(ap.getConfiguration());
                for (ConfigurationData cd : cfgData) {
                    existingValues.put(cd.getId(), cd.getValue());
                }
            }
        }

        pnlConfig.setLayout(new GridBagLayout());
        int gridRow = 0;
        for (Configuration cfg : selectedModel.getConfigurations()) {
            String labelText = cfg.getDescription() != null && !cfg.getDescription().isEmpty()
                    ? cfg.getDescription() : cfg.getId();
            JLabel lbl = new JLabel(labelText + ":");

            GridBagConstraints lblGbc = new GridBagConstraints();
            lblGbc.gridx = 0;
            lblGbc.gridy = gridRow;
            lblGbc.anchor = GridBagConstraints.WEST;
            lblGbc.insets = new Insets(2, 4, 2, 4);
            pnlConfig.add(lbl, lblGbc);

            JTextField tf = new JTextField(20);
            String existing = existingValues.get(cfg.getId());
            if (existing != null) {
                tf.setText(existing);
            }

            GridBagConstraints tfGbc = new GridBagConstraints();
            tfGbc.gridx = 1;
            tfGbc.gridy = gridRow;
            tfGbc.fill = GridBagConstraints.HORIZONTAL;
            tfGbc.weightx = 1.0;
            tfGbc.insets = new Insets(2, 4, 2, 4);
            pnlConfig.add(tf, tfGbc);

            configTextFields.put(cfg.getId(), tf);
            gridRow++;
        }

        // Filler at bottom
        GridBagConstraints fillerGbc = new GridBagConstraints();
        fillerGbc.gridx = 0;
        fillerGbc.gridy = gridRow;
        fillerGbc.weighty = 1.0;
        pnlConfig.add(Box.createVerticalGlue(), fillerGbc);

        pnlConfig.revalidate();
        pnlConfig.repaint();
    }

    private void updatedUI(AssistantPrompt ap) {
        updatingUI = true;
        this.txtName.setText(ap.getName());
        this.cmbRequestType.setSelectedItem(ap.getRequestType());
        this.taPrompt.setText(ap.getPrompt());
        this.taSystemPrompt.setText(ap.getSystemPrompt() != null ? ap.getSystemPrompt() : "");
        updatingUI = false;

        // Update model combo for this request type
        updateModelComboBox();

        // Set the model selection
        updatingUI = true;
        if (ap.getModelRef() != null && !ap.getModelRef().isEmpty()) {
            boolean found = false;
            for (int i = 0; i < cmbModel.getItemCount(); i++) {
                if (ap.getModelRef().equals(cmbModel.getItemAt(i))) {
                    cmbModel.setSelectedIndex(i);
                    found = true;
                    break;
                }
            }
            if (!found) {
                cmbModel.setSelectedItem(MODEL_DEFAULT);
            }
        } else {
            cmbModel.setSelectedItem(MODEL_DEFAULT);
        }
        updatingUI = false;

        // Update config panel with values from prompt
        updateConfigPanel();
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {

        // === Table Panel ===
        JPanel jPanel1 = new JPanel();
        jPanel1.setBorder(BorderFactory.createTitledBorder("Prompts"));

        tblPrompts = new JTable();
        tblPrompts.setModel(new DefaultTableModel(
                new Object[][]{},
                new String[]{"Name", "Funktion"}
        ) {
            boolean[] canEdit = new boolean[]{false, false};

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        tblPrompts.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblPrompts.getTableHeader().setReorderingAllowed(false);
        tblPrompts.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tblPromptsMouseClicked(evt);
            }
        });
        tblPrompts.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent evt) {
                tblPromptsKeyReleased(evt);
            }
        });
        JScrollPane jScrollPane1 = new JScrollPane();
        jScrollPane1.setViewportView(tblPrompts);

        cmdAdd = new JButton();
        cmdAdd.setIcon(new ImageIcon(getClass().getResource("/icons/edit_add.png")));
        cmdAdd.addActionListener(e -> cmdAddActionPerformed(e));

        cmdRemove = new JButton();
        cmdRemove.setIcon(new ImageIcon(getClass().getResource("/icons/trashcan_full.png")));
        cmdRemove.addActionListener(e -> cmdRemoveActionPerformed(e));

        GroupLayout jPanel1Layout = new GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 441, Short.MAX_VALUE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(cmdAdd)
                                        .addComponent(cmdRemove))
                                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 528, Short.MAX_VALUE)
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addComponent(cmdAdd)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(cmdRemove)
                                                .addGap(0, 0, Short.MAX_VALUE)))
                                .addContainerGap())
        );

        // === Right side detail widgets ===
        cmdClose = new JButton();
        cmdClose.setIcon(new ImageIcon(getClass().getResource("/icons/cancel.png")));
        cmdClose.setText("Schliessen");
        cmdClose.addActionListener(e -> cmdCloseActionPerformed(e));

        JLabel jLabel1 = new JLabel("Name:");
        txtName = new JTextField();

        cmdSave = new JButton();
        cmdSave.setIcon(new ImageIcon(getClass().getResource("/icons/agt_action_success.png")));
        cmdSave.setText("\u00dcbernehmen");
        cmdSave.addActionListener(e -> cmdSaveActionPerformed(e));

        JLabel jLabel5 = new JLabel("Prompt - Konfiguration");
        jLabel5.setFont(jLabel5.getFont().deriveFont(jLabel5.getFont().getStyle() | java.awt.Font.BOLD, jLabel5.getFont().getSize() - 2));
        jLabel5.setForeground(new java.awt.Color(153, 153, 153));

        JLabel jLabel3 = new JLabel("Funktion:");
        JLabel jLabelModel = new JLabel("Modell:");
        JLabel jLabel11 = new JLabel("Prompt:");

        JLabel jLabel2 = new JLabel();
        jLabel2.setIcon(new ImageIcon(getClass().getResource("/icons/info.png")));
        jLabel2.setToolTipText("Es werden die Platzhalter des Vorlagensystems unterst\u00fctzt.\nAktenbezogene Platzhalter (bspw. {{AKTE_ZEICHEN}}) werden nur dann ersetzt,\nwenn der Prompt aus einer Akte heraus verwendet wird.");

        taPrompt = new JTextArea();
        taPrompt.setColumns(20);
        taPrompt.setLineWrap(true);
        taPrompt.setRows(5);
        taPrompt.setWrapStyleWord(true);
        JScrollPane jScrollPane2 = new JScrollPane();
        jScrollPane2.setViewportView(taPrompt);

        JLabel jLabelSystemPrompt = new JLabel("System Prompt:");
        JLabel jLabelSystemPromptInfo = new JLabel();
        jLabelSystemPromptInfo.setIcon(new ImageIcon(getClass().getResource("/icons/info.png")));
        jLabelSystemPromptInfo.setToolTipText("Optionaler System Prompt, der als Instruktion an das Modell gesendet wird.\nWird nur \u00fcbertragen, wenn ein Wert eingetragen ist.");

        taSystemPrompt = new JTextArea();
        taSystemPrompt.setColumns(20);
        taSystemPrompt.setLineWrap(true);
        taSystemPrompt.setRows(3);
        taSystemPrompt.setWrapStyleWord(true);
        JScrollPane jScrollPane3 = new JScrollPane();
        jScrollPane3.setViewportView(taSystemPrompt);

        cmbRequestType = new JComboBox<>();
        cmbRequestType.setModel(new DefaultComboBoxModel<>(new String[]{"Item 1", "Item 2", "Item 3", "Item 4"}));

        cmbModel = new JComboBox<>();
        cmbModel.addItem(MODEL_DEFAULT);

        lblDeductTokens = new JLabel();
        lblDeductTokens.setFont(lblDeductTokens.getFont().deriveFont(lblDeductTokens.getFont().getSize() - 2f));
        lblDeductTokens.setForeground(new java.awt.Color(153, 153, 153));

        // === Config panel ===
        pnlConfig = new JPanel();
        pnlConfig.setLayout(new GridBagLayout());
        JScrollPane configScrollPane = new JScrollPane(pnlConfig);
        configScrollPane.setBorder(BorderFactory.createTitledBorder("Modell-Konfiguration"));

        // === Main Layout ===
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Assistent Ingo - eigene Prompts");

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jPanel1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup()
                                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                        .addComponent(jLabel1)
                                                        .addComponent(jLabel3)
                                                        .addComponent(jLabelModel)
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addComponent(jLabel11)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(jLabel2))
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addComponent(jLabelSystemPrompt)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(jLabelSystemPromptInfo)))
                                                .addGap(10, 10, 10)
                                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                        .addComponent(txtName)
                                                        .addComponent(jScrollPane2)
                                                        .addComponent(jScrollPane3)
                                                        .addComponent(cmbRequestType, GroupLayout.Alignment.TRAILING, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(cmbModel, GroupLayout.Alignment.TRAILING, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(lblDeductTokens)))
                                        .addComponent(configScrollPane)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(jLabel5)
                                                .addGap(0, 421, Short.MAX_VALUE))
                                        .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                                .addGap(0, 0, Short.MAX_VALUE)
                                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                        .addComponent(cmdClose, GroupLayout.Alignment.TRAILING)
                                                        .addComponent(cmdSave, GroupLayout.Alignment.TRAILING))))
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(jPanel1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(jLabel5)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                        .addComponent(txtName, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(jLabel1))
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                        .addComponent(jLabel3)
                                                        .addComponent(cmbRequestType, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                        .addComponent(jLabelModel)
                                                        .addComponent(cmbModel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(lblDeductTokens)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                        .addComponent(jScrollPane2, GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                                        .addComponent(jLabel11)
                                                                        .addComponent(jLabel2))
                                                                .addGap(0, 0, Short.MAX_VALUE)))
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                        .addComponent(jScrollPane3, GroupLayout.DEFAULT_SIZE, 80, Short.MAX_VALUE)
                                                        .addGroup(layout.createSequentialGroup()
                                                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                                        .addComponent(jLabelSystemPrompt)
                                                                        .addComponent(jLabelSystemPromptInfo))
                                                                .addGap(0, 0, Short.MAX_VALUE)))
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(configScrollPane, GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(cmdSave)
                                                .addGap(18, 18, 18)
                                                .addComponent(cmdClose)))
                                .addContainerGap())
        );

        pack();
    }

    private void cmdCloseActionPerformed(java.awt.event.ActionEvent evt) {
        this.setVisible(false);
        this.dispose();
    }

    private void tblPromptsMouseClicked(java.awt.event.MouseEvent evt) {
        if (evt.getClickCount() == 1 && !evt.isConsumed()) {
            int row = this.tblPrompts.getSelectedRow();
            if (row < 0) {
                this.resetDetails();
            } else {
                AssistantPrompt ap = (AssistantPrompt) this.tblPrompts.getValueAt(row, 0);
                this.updatedUI(ap);
            }
        }
    }

    private void cmdAddActionPerformed(java.awt.event.ActionEvent evt) {
        Object newNameObject = JOptionPane.showInputDialog(this, "Anzeigename: ", "Neuen Prompt anlegen", JOptionPane.QUESTION_MESSAGE, null, null, "neuer Prompt");
        if (newNameObject == null) {
            return;
        }

        ClientSettings settings = ClientSettings.getInstance();
        try {
            JLawyerServiceLocator locator = JLawyerServiceLocator.getInstance(settings.getLookupProperties());

            AssistantPrompt ap = new AssistantPrompt();
            ap.setName(newNameObject.toString());
            ap.setRequestType(this.cmbRequestType.getSelectedItem().toString());
            ap.setPrompt(this.taPrompt.getText());

            // System Prompt
            String sp = taSystemPrompt.getText().trim();
            ap.setSystemPrompt(sp.isEmpty() ? null : sp);

            // Model and config
            String selectedModel = (String) cmbModel.getSelectedItem();
            if (selectedModel != null && !MODEL_DEFAULT.equals(selectedModel)) {
                ap.setModelRef(selectedModel);
            }
            ap.setConfiguration(collectConfigValues());

            AssistantPrompt savedPrompt = locator.lookupIntegrationServiceRemote().addAssistantPrompt(ap);
            AssistantAccess.getInstance().flushCustomPrompts();

            ((DefaultTableModel) this.tblPrompts.getModel()).addRow(new Object[]{savedPrompt, savedPrompt.getRequestType()});
            this.tblPrompts.getSelectionModel().setSelectionInterval(this.tblPrompts.getRowCount() - 1, this.tblPrompts.getRowCount() - 1);

        } catch (Exception ex) {
            log.error("Error creating new prompt", ex);
            JOptionPane.showMessageDialog(this, ex.getMessage(), com.jdimension.jlawyer.client.utils.DesktopUtils.POPUP_TITLE_ERROR, JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cmdSaveActionPerformed(java.awt.event.ActionEvent evt) {
        int row = this.tblPrompts.getSelectedRow();

        if (row >= 0) {
            AssistantPrompt ap = (AssistantPrompt) this.tblPrompts.getValueAt(row, 0);
            ap.setName(this.txtName.getText());
            ap.setRequestType(this.cmbRequestType.getSelectedItem().toString());
            ap.setPrompt(this.taPrompt.getText());

            // System Prompt
            String sp = taSystemPrompt.getText().trim();
            ap.setSystemPrompt(sp.isEmpty() ? null : sp);

            // Model
            String selectedModel = (String) cmbModel.getSelectedItem();
            if (selectedModel != null && !MODEL_DEFAULT.equals(selectedModel)) {
                ap.setModelRef(selectedModel);
            } else {
                ap.setModelRef(null);
            }

            // Configuration
            String configStr = collectConfigValues();
            ap.setConfiguration(configStr.isEmpty() ? null : configStr);

            ClientSettings settings = ClientSettings.getInstance();
            try {
                JLawyerServiceLocator locator = JLawyerServiceLocator.getInstance(settings.getLookupProperties());

                AssistantPrompt savedPrompt = locator.lookupIntegrationServiceRemote().updateAssistantPrompt(ap);
                AssistantAccess.getInstance().flushCustomPrompts();
                row = this.tblPrompts.convertRowIndexToModel(row);
                ((DefaultTableModel) this.tblPrompts.getModel()).setValueAt(savedPrompt, row, 0);
                ((DefaultTableModel) this.tblPrompts.getModel()).setValueAt(savedPrompt.getRequestType(), row, 1);

            } catch (Exception ex) {
                log.error("Error updating prompt", ex);
                JOptionPane.showMessageDialog(this, ex.getMessage(), com.jdimension.jlawyer.client.utils.DesktopUtils.POPUP_TITLE_ERROR, JOptionPane.ERROR_MESSAGE);
            }
            AssistantAccess.getInstance().resetCapabilities();
        }
    }

    private void cmdRemoveActionPerformed(java.awt.event.ActionEvent evt) {
        int row = this.tblPrompts.getSelectedRow();

        if (row >= 0) {
            AssistantPrompt ap = (AssistantPrompt) this.tblPrompts.getValueAt(row, 0);
            ClientSettings settings = ClientSettings.getInstance();
            try {
                JLawyerServiceLocator locator = JLawyerServiceLocator.getInstance(settings.getLookupProperties());

                locator.lookupIntegrationServiceRemote().removeAssistantPrompt(ap);
                AssistantAccess.getInstance().flushCustomPrompts();
                row = this.tblPrompts.convertRowIndexToModel(row);
                ((DefaultTableModel) this.tblPrompts.getModel()).removeRow(row);

                this.resetDetails();
            } catch (Exception ex) {
                log.error("Error removing prompt", ex);
                JOptionPane.showMessageDialog(this, ex.getMessage(), com.jdimension.jlawyer.client.utils.DesktopUtils.POPUP_TITLE_ERROR, JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void tblPromptsKeyReleased(java.awt.event.KeyEvent evt) {
        int row = this.tblPrompts.getSelectedRow();
        if (row < 0) {
            this.resetDetails();
        } else {
            AssistantPrompt ap = (AssistantPrompt) this.tblPrompts.getValueAt(row, 0);
            this.updatedUI(ap);
        }
    }

    private String collectConfigValues() {
        if (configTextFields.isEmpty()) {
            return "";
        }
        List<ConfigurationData> configs = new ArrayList<>();
        for (Map.Entry<String, JTextField> entry : configTextFields.entrySet()) {
            String val = entry.getValue().getText();
            if (val != null && !val.isEmpty()) {
                ConfigurationData cd = new ConfigurationData();
                cd.setId(entry.getKey());
                cd.setValue(val);
                configs.add(cd);
            }
        }
        return ConfigurationUtils.toProperties(configs);
    }
}
