package com.jdimension.jlawyer.client.assistant;

import com.jdimension.jlawyer.ai.AiCapability;
import com.jdimension.jlawyer.ai.AiRequestStatus;
import com.jdimension.jlawyer.ai.AiResponse;
import com.jdimension.jlawyer.ai.ConfigurationData;
import com.jdimension.jlawyer.ai.ConfigurationUtils;
import com.jdimension.jlawyer.ai.InputData;
import com.jdimension.jlawyer.ai.Message;
import com.jdimension.jlawyer.ai.OutputData;
import com.jdimension.jlawyer.ai.Parameter;
import com.jdimension.jlawyer.ai.ParameterData;
import com.jdimension.jlawyer.client.settings.ClientSettings;
import com.jdimension.jlawyer.client.utils.AttachmentListCellRenderer;
import com.jdimension.jlawyer.client.utils.AudioUtils;
import com.jdimension.jlawyer.client.utils.ComponentUtils;
import com.jdimension.jlawyer.client.utils.DesktopUtils;
import com.jdimension.jlawyer.client.utils.FrameUtils;
import com.jdimension.jlawyer.client.utils.TemplatesUtil;
import com.jdimension.jlawyer.client.utils.ThreadUtils;
import com.jdimension.jlawyer.persistence.AppUserBean;
import com.jdimension.jlawyer.persistence.ArchiveFileAddressesBean;
import com.jdimension.jlawyer.persistence.ArchiveFileBean;
import com.jdimension.jlawyer.persistence.AssistantConfig;
import com.jdimension.jlawyer.persistence.PartyTypeBean;
import com.jdimension.jlawyer.pojo.PartiesTriplet;
import com.jdimension.jlawyer.services.JLawyerServiceLocator;
import com.formdev.flatlaf.ui.FlatLineBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.event.MouseEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicReference;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import org.apache.log4j.Logger;
import themes.colors.DefaultColorTheme;

/**
 * Modern panel-style dialog for generic (single-turn) AI assistant interactions.
 * Replaces the older AssistantGenericDialog with a layout consistent with AssistantChatPanel:
 * messages in the center, prompt input at the bottom, collapsible context area.
 *
 * @author jens
 */
public class AssistantGenericPanel extends JDialog {

    private static final Logger log = Logger.getLogger(AssistantGenericPanel.class.getName());

    private AssistantConfig config = null;
    private AiCapability capability = null;
    private AssistantInputAdapter inputAdapter = null;

    private AiRequestStatus result = null;

    private boolean interrupted = false;

    // variables for transcription
    private boolean isRecording = false;
    private AiCapability transcribeCapability = null;
    private AssistantConfig transcribeConfig = null;
    private TargetDataLine targetDataLine;
    private ByteArrayOutputStream byteArrayOutputStream;
    private Color cmdTranscribeOriginalBackground = null;

    // variables used for placeholder support in prompts
    private List<PartyTypeBean> allPartyTypes = null;
    private Collection<String> formPlaceHolders = null;
    private HashMap<String, String> formPlaceHolderValues = null;
    private AppUserBean caseLawyer = null;
    private AppUserBean caseAssistant = null;
    private List<PartiesTriplet> parties = new ArrayList<>();
    private ArchiveFileBean selectedCase = null;

    // Gray-tone bubble colors (consistent with AssistantChatPanel)
    private static final Color ASSISTANT_BUBBLE_BG = new Color(245, 245, 245);
    private static final Color BUBBLE_BORDER_COLOR = new Color(215, 215, 215);

    // UI components
    private JPanel pnlTitle;
    private JLabel lblRequestType;
    private JComboBox<String> cmbDevices;
    private JLabel lblContextToggle;
    private JPanel pnlContextContent;
    private JTabbedPane tabInputs;
    private JList<String> lstInputFiles;
    private JTextArea taInputString;
    private JPanel pnlParameters;
    private JScrollPane scrollMessages;
    private JPanel pnlMessages;
    private JProgressBar progress;
    private JTextArea taPrompt;
    private JButton cmdSubmit;
    private JButton cmdInterrupt;
    private JButton cmdTranscribe;
    private JButton cmdClose;
    private JButton cmdProcessOutput;
    private JButton cmdCopy;
    private JPanel bottomPanel;
    private JPopupMenu popInputText;

    // Output files
    private JLabel lblOutputToggle;
    private JPanel pnlOutputContent;
    private JList<String> lstOutputFiles;

    public AssistantGenericPanel(ArchiveFileBean selectedCase, AssistantConfig config, AiCapability c, AssistantInputAdapter inputAdapter, boolean autoExecute, JFrame parent, boolean modal) {
        super(parent, modal);
        this.initialize(selectedCase, config, c, inputAdapter, autoExecute);
    }

    public AssistantGenericPanel(ArchiveFileBean selectedCase, AssistantConfig config, AiCapability c, AssistantInputAdapter inputAdapter, boolean autoExecute, JDialog parent, boolean modal) {
        super(parent, modal);
        this.initialize(selectedCase, config, c, inputAdapter, autoExecute);
    }

    private void initialize(ArchiveFileBean selectedCase, AssistantConfig config, AiCapability c, AssistantInputAdapter inputAdapter, boolean autoExecute) {
        buildUI();

        this.scrollMessages.getVerticalScrollBar().setUnitIncrement(32);

        this.config = config;
        this.capability = c;
        this.inputAdapter = inputAdapter;

        this.selectedCase = selectedCase;
        if (this.selectedCase != null) {
            try {
                ClientSettings settings = ClientSettings.getInstance();
                JLawyerServiceLocator locator = JLawyerServiceLocator.getInstance(settings.getLookupProperties());

                this.allPartyTypes = locator.lookupSystemManagementRemote().getPartyTypes();
                try {
                    this.caseAssistant = locator.lookupSystemManagementRemote().getUser(this.selectedCase.getAssistant());
                } catch (Exception ex) {
                }
                try {
                    this.caseLawyer = locator.lookupSystemManagementRemote().getUser(this.selectedCase.getLawyer());
                } catch (Exception ex) {
                }
                this.formPlaceHolders = locator.lookupFormsServiceRemote().getPlaceHoldersForCase(this.selectedCase.getId());
                this.formPlaceHolderValues = locator.lookupFormsServiceRemote().getPlaceHolderValuesForCase(this.selectedCase.getId());

                try {
                    List<ArchiveFileAddressesBean> involved = locator.lookupArchiveFileServiceRemote().getInvolvementDetailsForCase(this.selectedCase.getId(), false);
                    for (ArchiveFileAddressesBean aab : involved) {
                        parties.add(new PartiesTriplet(aab.getAddressKey(), aab.getReferenceType(), aab));
                    }
                } catch (Exception ex) {
                    log.error("Could not load involvements for case " + this.selectedCase.getId(), ex);
                }

            } catch (Exception ex) {
                log.error("Error getting data for case " + selectedCase.getId(), ex);
                ThreadUtils.showErrorDialog(this, "Fehler beim Laden der Akteninformationen", "Akteninformationen laden");
            }
        }

        if (c.getDefaultPrompt() != null && c.getDefaultPrompt().getDefaultPrompt() != null) {
            if (!c.getDefaultPrompt().getDefaultPrompt().contains("{{")) {
                this.taPrompt.setText(c.getDefaultPrompt().getDefaultPrompt());
            } else {
                HashMap<String, Object> placeHolders = TemplatesUtil.getPlaceHolderValues(c.getDefaultPrompt().getDefaultPrompt(), selectedCase, this.parties, null, null, this.allPartyTypes, this.formPlaceHolders, this.formPlaceHolderValues, this.caseLawyer, this.caseAssistant);
                String promptWithValues = TemplatesUtil.replacePlaceHolders(c.getDefaultPrompt().getDefaultPrompt(), placeHolders);
                this.taPrompt.setText(promptWithValues);
            }
        }

        this.pnlParameters.setLayout(new GridLayout(this.capability.getParameters().size(), 2, 6, 6));
        for (Parameter p : this.capability.getParameters()) {
            this.pnlParameters.add(new JLabel(p.getName()));
            if (p.getList() != null && p.getList().length() > 0) {
                Vector<String> v = new Vector<>(Arrays.asList(p.getList().split(",")));
                JComboBox<String> combo = new JComboBox<>(v);
                combo.setEditable(false);
                combo.setSelectedItem(p.getDefaultValue());
                this.pnlParameters.add(combo);
            } else {
                JTextField tf = new JTextField();
                tf.setText(p.getDefaultValue());
                this.pnlParameters.add(tf);
            }
        }

        this.lblRequestType.setText(c.getName() + " (" + c.getDescription() + ")");

        this.lstInputFiles.setCellRenderer(new AttachmentListCellRenderer());
        this.lstInputFiles.setModel(new DefaultListModel());

        this.lstOutputFiles.setCellRenderer(new AttachmentListCellRenderer());
        this.lstOutputFiles.setModel(new DefaultListModel());

        ((DefaultListModel) this.lstInputFiles.getModel()).removeAllElements();
        ((DefaultListModel) this.lstOutputFiles.getModel()).removeAllElements();

        this.taInputString.setText("");

        boolean hasInputFiles = false;
        boolean hasInputText = false;
        for (InputData i : inputAdapter.getInputs(c)) {
            if (InputData.TYPE_STRING.equalsIgnoreCase(i.getType())) {
                this.taInputString.append(i.getStringData());
                this.taInputString.append(System.lineSeparator());
                hasInputText = true;
            } else {
                ((DefaultListModel) lstInputFiles.getModel()).addElement(i.getFileName());
                hasInputFiles = true;
            }
        }

        this.tabInputs.setEnabledAt(0, hasInputFiles);
        this.tabInputs.setEnabledAt(1, hasInputText);

        if (hasInputText) {
            this.tabInputs.setSelectedIndex(1);
        } else {
            this.tabInputs.setSelectedIndex(0);
        }

        // Hide context toggle if there is no input data at all
        if (!hasInputFiles && !hasInputText) {
            this.lblContextToggle.setVisible(false);
            this.pnlContextContent.setVisible(false);
        }

        // Enter sends request, Shift+Enter inserts newline
        this.taPrompt.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ENTER"), "enterKeyAction");
        this.taPrompt.getActionMap().put("enterKeyAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!((e.getModifiers() & ActionEvent.SHIFT_MASK) == ActionEvent.SHIFT_MASK)) {
                    cmdSubmitActionPerformed(null);
                }
            }
        });

        this.taPrompt.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("shift ENTER"), "shiftEnterKeyAction");
        this.taPrompt.getActionMap().put("shiftEnterKeyAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                taPrompt.append("\n");
            }
        });

        ComponentUtils.restoreDialogSize(this);

        // Initialize transcription capabilities
        AssistantAccess ingo = AssistantAccess.getInstance();
        try {
            java.util.Map<AssistantConfig, List<AiCapability>> capabilities = ingo.filterCapabilities(AiCapability.REQUESTTYPE_TRANSCRIBE, AiCapability.INPUTTYPE_FILE, AiCapability.USAGETYPE_AUTOMATED);

            if (!capabilities.isEmpty()) {
                this.transcribeCapability = capabilities.get(capabilities.keySet().iterator().next()).get(0);
                this.transcribeConfig = capabilities.keySet().iterator().next();
                this.cmdTranscribe.setEnabled(true);
            }
        } catch (Exception ex) {
            log.error("Error initializing transcription capabilities", ex);
        }

        this.cmdTranscribeOriginalBackground = this.cmdTranscribe.getBackground();

        this.cmbDevices.removeAllItems();
        AudioUtils.populateMicrophoneDevices(this.cmbDevices);

        // autoExecute parameter is accepted for API compatibility but not used;
        // the user always triggers execution manually via the submit button
    }

    private void buildUI() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setTitle("Assistent Ingo");
        getContentPane().setLayout(new BorderLayout());

        // Context menu for input text
        popInputText = new JPopupMenu();
        JMenuItem mnuPromptAll = new JMenuItem("in Prompt \u00fcbernehmen");
        mnuPromptAll.addActionListener(e -> {
            this.taPrompt.insert(this.taInputString.getText(), this.taPrompt.getCaretPosition());
        });
        popInputText.add(mnuPromptAll);

        JMenuItem mnuPromptSelection = new JMenuItem("Auswahl in Prompt \u00fcbernehmen");
        mnuPromptSelection.addActionListener(e -> {
            String sel = this.taInputString.getSelectedText();
            if (sel != null) {
                this.taPrompt.insert(sel, this.taPrompt.getCaretPosition());
            }
        });
        popInputText.add(mnuPromptSelection);

        // ========== NORTH: headerPanel ==========
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        // --- Title bar ---
        pnlTitle = new JPanel(new BorderLayout(6, 0));
        pnlTitle.setBackground(DefaultColorTheme.COLOR_DARK_GREY);
        pnlTitle.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JLabel lblIcon = new JLabel(new ImageIcon(getClass().getResource("/icons32/material/j-lawyer-ai.png")));
        pnlTitle.add(lblIcon, BorderLayout.WEST);

        lblRequestType = new JLabel("Transkribieren");
        lblRequestType.setFont(lblRequestType.getFont().deriveFont(lblRequestType.getFont().getStyle() | java.awt.Font.BOLD, lblRequestType.getFont().getSize() + 2));
        lblRequestType.setForeground(Color.WHITE);
        lblRequestType.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));
        pnlTitle.add(lblRequestType, BorderLayout.CENTER);

        JPanel pnlTitleButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        pnlTitleButtons.setOpaque(false);

        // Microphone device dropdown in header
        cmbDevices = new JComboBox<>();
        pnlTitleButtons.add(cmbDevices);

        pnlTitle.add(pnlTitleButtons, BorderLayout.EAST);
        pnlTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerPanel.add(pnlTitle);

        // --- Context toggle (for input files + text) ---
        lblContextToggle = new JLabel("\u25b6 Kontext anzeigen");
        lblContextToggle.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        lblContextToggle.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        lblContextToggle.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblContextToggle.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                boolean visible = !pnlContextContent.isVisible();
                pnlContextContent.setVisible(visible);
                lblContextToggle.setText(visible ? "\u25bc Kontext verbergen" : "\u25b6 Kontext anzeigen");
                headerPanel.revalidate();
            }
        });
        headerPanel.add(lblContextToggle);

        // --- Context content: tabbed pane for files + text (collapsed by default) ---
        pnlContextContent = new JPanel(new BorderLayout());
        pnlContextContent.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlContextContent.setVisible(false);
        pnlContextContent.setBorder(BorderFactory.createEmptyBorder(0, 8, 4, 8));

        tabInputs = new JTabbedPane();

        lstInputFiles = new JList<>();
        lstInputFiles.setLayoutOrientation(JList.VERTICAL_WRAP);
        lstInputFiles.setVisibleRowCount(3);
        JScrollPane scrollInputFiles = new JScrollPane(lstInputFiles);
        tabInputs.addTab("Dateien", scrollInputFiles);

        taInputString = new JTextArea();
        taInputString.setColumns(20);
        taInputString.setRows(5);
        taInputString.setLineWrap(true);
        taInputString.setWrapStyleWord(true);
        taInputString.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 1 && evt.getButton() == MouseEvent.BUTTON3) {
                    popInputText.show(taInputString, evt.getX(), evt.getY());
                }
            }
        });
        JScrollPane scrollInputText = new JScrollPane(taInputString);
        tabInputs.addTab("Text", scrollInputText);

        pnlContextContent.add(tabInputs, BorderLayout.CENTER);
        pnlContextContent.setPreferredSize(new Dimension(0, 160));
        pnlContextContent.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));
        headerPanel.add(pnlContextContent);

        // --- Parameter panel ---
        pnlParameters = new JPanel();
        pnlParameters.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlParameters.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        headerPanel.add(pnlParameters);

        getContentPane().add(headerPanel, BorderLayout.NORTH);

        // ========== CENTER: Messages ==========
        pnlMessages = new JPanel();
        pnlMessages.setLayout(new BoxLayout(pnlMessages, BoxLayout.Y_AXIS));
        pnlMessages.setBackground(Color.WHITE);
        pnlMessages.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        scrollMessages = new JScrollPane(pnlMessages);
        scrollMessages.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollMessages.setBorder(BorderFactory.createEmptyBorder());
        scrollMessages.getViewport().setBackground(Color.WHITE);

        getContentPane().add(scrollMessages, BorderLayout.CENTER);

        // ========== SOUTH: bottomPanel ==========
        bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(218, 218, 218)));

        // --- Output files toggle (collapsed by default) ---
        lblOutputToggle = new JLabel("\u25b6 Ausgabedateien anzeigen");
        lblOutputToggle.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        lblOutputToggle.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        lblOutputToggle.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblOutputToggle.setVisible(false);
        lblOutputToggle.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                boolean visible = !pnlOutputContent.isVisible();
                pnlOutputContent.setVisible(visible);
                lblOutputToggle.setText(visible ? "\u25bc Ausgabedateien verbergen" : "\u25b6 Ausgabedateien anzeigen");
                bottomPanel.revalidate();
            }
        });
        bottomPanel.add(lblOutputToggle);

        pnlOutputContent = new JPanel(new BorderLayout());
        pnlOutputContent.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlOutputContent.setVisible(false);
        pnlOutputContent.setBorder(BorderFactory.createEmptyBorder(0, 8, 4, 8));
        lstOutputFiles = new JList<>();
        lstOutputFiles.setLayoutOrientation(JList.VERTICAL_WRAP);
        lstOutputFiles.setVisibleRowCount(3);
        JScrollPane scrollOutputFiles = new JScrollPane(lstOutputFiles);
        scrollOutputFiles.setPreferredSize(new Dimension(0, 80));
        pnlOutputContent.add(scrollOutputFiles, BorderLayout.CENTER);
        pnlOutputContent.setPreferredSize(new Dimension(0, 80));
        pnlOutputContent.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        bottomPanel.add(pnlOutputContent);

        // --- Progress bar (3px, hidden by default) ---
        progress = new JProgressBar();
        progress.setIndeterminate(false);
        progress.setForeground(DefaultColorTheme.COLOR_LOGO_GREEN);
        progress.setPreferredSize(new Dimension(0, 3));
        progress.setMaximumSize(new Dimension(Integer.MAX_VALUE, 3));
        progress.setMinimumSize(new Dimension(0, 3));
        progress.setVisible(false);
        progress.setBorderPainted(false);
        bottomPanel.add(progress);

        // --- Input area: centered at ~80% width with buttons right of text field ---
        JPanel inputWrapper = new JPanel(new GridBagLayout());
        inputWrapper.setBorder(BorderFactory.createEmptyBorder(8, 16, 4, 16));
        GridBagConstraints gbc = new GridBagConstraints();

        // Left spacer (~10%)
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        inputWrapper.add(Box.createHorizontalGlue(), gbc);

        // Center: text field + buttons row (~80%)
        JPanel inputRow = new JPanel(new BorderLayout(6, 0));
        inputRow.setOpaque(false);

        // Prompt text area with rounded border
        taPrompt = new JTextArea();
        taPrompt.setLineWrap(true);
        taPrompt.setWrapStyleWord(true);
        taPrompt.setRows(1);
        JScrollPane scrollPrompt = new JScrollPane(taPrompt);
        scrollPrompt.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPrompt.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPrompt.setBorder(new FlatLineBorder(new Insets(6, 10, 6, 10), new Color(190, 190, 190), 1, 16));

        taPrompt.getDocument().addDocumentListener(new DocumentListener() {
            private void adjustRows() {
                // Defer to allow layout to complete first (getLineCount needs valid width)
                SwingUtilities.invokeLater(() -> {
                    int newRows = Math.max(1, Math.min(taPrompt.getLineCount(), 5));
                    if (taPrompt.getRows() != newRows) {
                        taPrompt.setRows(newRows);
                        bottomPanel.revalidate();
                        getContentPane().revalidate();
                        getContentPane().repaint();
                    }
                });
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                adjustRows();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                adjustRows();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                adjustRows();
            }
        });

        inputRow.add(scrollPrompt, BorderLayout.CENTER);

        // Buttons right of text field: dictate + send/stop
        // FlowLayout keeps buttons at their preferred size (no vertical stretching)
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        btnPanel.setOpaque(false);

        cmdTranscribe = new JButton(new ImageIcon(getClass().getResource("/icons16/material/baseline_mic_black_48dp.png")));
        cmdTranscribe.setToolTipText("KI-Anfrage diktieren");
        cmdTranscribe.setEnabled(false);
        cmdTranscribe.putClientProperty("JButton.buttonType", "roundRect");
        cmdTranscribe.addActionListener(e -> cmdTranscribeActionPerformed(e));
        btnPanel.add(cmdTranscribe);

        cmdSubmit = new JButton(new ImageIcon(getClass().getResource("/icons16/material/baseline_slideshow_black_48dp.png")));
        cmdSubmit.setToolTipText("Anfrage an Assistent Ingo senden");
        cmdSubmit.putClientProperty("JButton.buttonType", "roundRect");
        cmdSubmit.addActionListener(e -> cmdSubmitActionPerformed(e));
        btnPanel.add(cmdSubmit);

        cmdInterrupt = new JButton(new ImageIcon(getClass().getResource("/icons16/material/stop_circle_24dp_0E72B5.png")));
        cmdInterrupt.setToolTipText("Laufende Anfrage unterbrechen");
        cmdInterrupt.putClientProperty("JButton.buttonType", "roundRect");
        cmdInterrupt.setVisible(false);
        cmdInterrupt.addActionListener(e -> cmdInterruptActionPerformed(e));
        btnPanel.add(cmdInterrupt);

        // Wrap in a panel that top-aligns the buttons when inputRow stretches
        JPanel btnAlignPanel = new JPanel(new BorderLayout());
        btnAlignPanel.setOpaque(false);
        btnAlignPanel.add(btnPanel, BorderLayout.NORTH);
        inputRow.add(btnAlignPanel, BorderLayout.EAST);

        gbc.gridx = 1;
        gbc.weightx = 0.8;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        inputWrapper.add(inputRow, gbc);

        // Right spacer (~10%)
        gbc.gridx = 2;
        gbc.weightx = 0.1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        inputWrapper.add(Box.createHorizontalGlue(), gbc);

        bottomPanel.add(inputWrapper);

        // --- Action bar ---
        JPanel actionBar = new JPanel(new BorderLayout());
        actionBar.setBorder(BorderFactory.createEmptyBorder(4, 8, 6, 8));

        cmdClose = new JButton("Schliessen");
        cmdClose.setIcon(new ImageIcon(getClass().getResource("/icons/cancel.png")));
        cmdClose.addActionListener(e -> cmdCloseActionPerformed(e));
        actionBar.add(cmdClose, BorderLayout.WEST);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));

        cmdProcessOutput = new JButton("\u00dcbernehmen");
        cmdProcessOutput.setIcon(new ImageIcon(getClass().getResource("/icons/agt_action_success.png")));
        cmdProcessOutput.addActionListener(e -> cmdProcessOutputActionPerformed(e));
        rightActions.add(cmdProcessOutput);

        cmdCopy = new JButton("Kopieren");
        cmdCopy.setIcon(new ImageIcon(getClass().getResource("/icons16/editpaste.png")));
        cmdCopy.setToolTipText("Text in Zwischenablage kopieren");
        cmdCopy.addActionListener(e -> cmdCopyActionPerformed(e));
        rightActions.add(cmdCopy);

        actionBar.add(rightActions, BorderLayout.EAST);

        bottomPanel.add(actionBar);

        getContentPane().add(bottomPanel, BorderLayout.SOUTH);

        // --- Dialog listeners ---
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent evt) {
                ComponentUtils.storeDialogSize(AssistantGenericPanel.this);
            }

            @Override
            public void componentShown(java.awt.event.ComponentEvent evt) {
                FrameUtils.centerDialogOnParentMonitor(AssistantGenericPanel.this, AssistantGenericPanel.this.getOwner().getLocation());
            }
        });

        pack();
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            SwingUtilities.invokeLater(() -> {
                // Recalculate rows now that the text field has a valid width
                int newRows = Math.max(1, Math.min(taPrompt.getLineCount(), 5));
                if (taPrompt.getRows() != newRows) {
                    taPrompt.setRows(newRows);
                    bottomPanel.revalidate();
                    getContentPane().revalidate();
                    getContentPane().repaint();
                }
                this.taPrompt.requestFocusInWindow();
            });
        }
    }

    // ==================== Business Logic (ported from AssistantGenericDialog) ====================

    private void cmdSubmitActionPerformed(java.awt.event.ActionEvent evt) {
        SwingUtilities.invokeLater(this::startBackgroundTask);
    }

    private void startBackgroundTask() {

        this.pnlMessages.removeAll();
        this.interrupted = false;

        this.cmdSubmit.setVisible(false);
        this.cmdInterrupt.setVisible(true);

        ((DefaultListModel) this.lstOutputFiles.getModel()).removeAllElements();

        List<ParameterData> params = new ArrayList<>();
        if (capability.getParameters() != null && !capability.getParameters().isEmpty()) {
            params = getParameters();
        }

        final List<ParameterData> fParams = params;

        this.progress.setIndeterminate(true);
        this.progress.setVisible(true);

        AtomicReference<AiRequestStatus> resultRef = new AtomicReference<>();
        AtomicReference<AiChatMessageMarkdownPanel> incomingMessageRef = new AtomicReference<>();

        JDialog owner = this;

        SwingWorker<Void, Void> worker = new SwingWorker<>() {

            @Override
            protected Void doInBackground() throws Exception {
                List<InputData> inputs = inputAdapter.getInputs(capability);
                for (InputData i : inputs) {
                    if (InputData.TYPE_STRING.equalsIgnoreCase(i.getType())) {
                        i.setStringData(taInputString.getText());
                    }
                }

                ClientSettings settings = ClientSettings.getInstance();
                Message incomingMsg = new Message();
                incomingMsg.setRole(Message.ROLE_ASSISTANT);
                incomingMsg.setContent("...");
                AiChatMessageMarkdownPanel incomingMsgPanel = new AiChatMessageMarkdownPanel(incomingMsg, owner);
                styleMessageBubble(incomingMsgPanel);
                try {
                    JLawyerServiceLocator locator = JLawyerServiceLocator.getInstance(settings.getLookupProperties());

                    SwingUtilities.invokeAndWait(() -> {
                        Dimension maxSize = incomingMsgPanel.getPreferredSize();
                        maxSize.setSize(pnlMessages.getWidth(), maxSize.getHeight());

                        incomingMsgPanel.setPreferredSize(maxSize);
                        incomingMsgPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
                        pnlMessages.add(incomingMsgPanel);
                        incomingMsgPanel.revalidate();
                        incomingMsgPanel.repaint();

                        SwingUtilities.invokeLater(() -> {
                            JScrollBar bar = scrollMessages.getVerticalScrollBar();
                            bar.setValue(bar.getMaximum());
                        });
                    });

                    List<ConfigurationData> promptConfigs = null;
                    if (capability.getConfigurationValues() != null && !capability.getConfigurationValues().isEmpty()) {
                        promptConfigs = ConfigurationUtils.fromProperties(capability.getConfigurationValues());
                    }
                    AiRequestStatus status = locator.lookupIntegrationServiceRemote().submitAssistantRequest(config, capability.getRequestType(), capability.getActionId(), capability.getModelRef(), taPrompt.getText(), capability.getSystemPrompt(), capability.isAsyncRecommended(), fParams, inputs, null, promptConfigs, null);
                    if (status.isAsync()) {
                        Thread.sleep(1000);
                        AiResponse res = locator.lookupIntegrationServiceRemote().getAssistantRequestStatus(config, status.getRequestId());
                        while (res.getStatus().equals(AiResponse.STATUS_EXECUTING)) {
                            Thread.sleep(1000);
                            res = locator.lookupIntegrationServiceRemote().getAssistantRequestStatus(config, status.getRequestId());
                            StringBuilder resultString = new StringBuilder();
                            for (OutputData o : res.getOutputData()) {
                                if (o.getType().equalsIgnoreCase(OutputData.TYPE_STRING)) {
                                    resultString.append(o.getStringData()).append(System.lineSeparator()).append(System.lineSeparator());
                                }
                            }

                            SwingUtilities.invokeAndWait(() -> {
                                JScrollBar verticalBar = scrollMessages.getVerticalScrollBar();
                                int currentValue = verticalBar.getValue();
                                int maximum = verticalBar.getMaximum();
                                int extent = verticalBar.getVisibleAmount();
                                boolean wasAtBottom = (currentValue + extent >= maximum - 50);
                                int savedScrollPosition = currentValue;

                                incomingMsgPanel.getMessage().setContent(resultString.toString());
                                incomingMsgPanel.setMessage(incomingMsgPanel.getMessage(), owner);
                                incomingMsgPanel.repaint();
                                incomingMsgPanel.updateUI();
                                styleMessageBubble(incomingMsgPanel);

                                SwingUtilities.invokeLater(() -> {
                                    JScrollBar bar = scrollMessages.getVerticalScrollBar();
                                    if (wasAtBottom) {
                                        bar.setValue(bar.getMaximum());
                                    } else {
                                        bar.setValue(savedScrollPosition);
                                    }
                                });
                            });

                            if (interrupted) {
                                break;
                            }
                        }
                        status.setStatus(res.getStatus());
                        status.setStatusDetails(res.getStatusMessage());
                        status.setResponse(res);
                        resultRef.set(status);
                        incomingMessageRef.set(incomingMsgPanel);
                    } else {
                        resultRef.set(status);
                        incomingMessageRef.set(incomingMsgPanel);
                    }

                } catch (Throwable t) {
                    log.error("Error processing AI request", t);
                    AiRequestStatus status = new AiRequestStatus();
                    status.setStatus("ERROR");
                    status.setStatusDetails(t.getMessage());
                    resultRef.set(status);
                    incomingMessageRef.set(incomingMsgPanel);
                }
                return null;
            }

            @Override
            protected void done() {
                AiRequestStatus status = resultRef.get();
                result = status;
                if (status != null) {
                    if (status.isError()) {
                        JScrollBar verticalBar = scrollMessages.getVerticalScrollBar();
                        int currentValue = verticalBar.getValue();
                        int maximum = verticalBar.getMaximum();
                        int extent = verticalBar.getVisibleAmount();
                        boolean wasAtBottom = (currentValue + extent >= maximum - 50);
                        int savedScrollPosition = currentValue;

                        AiChatMessageMarkdownPanel incomingMsgPanel = incomingMessageRef.get();
                        incomingMsgPanel.getMessage().setContent(status.getStatus() + ": " + status.getStatusDetails());
                        incomingMsgPanel.setMessage(incomingMsgPanel.getMessage(), owner);
                        incomingMsgPanel.repaint();
                        incomingMsgPanel.updateUI();
                        styleMessageBubble(incomingMsgPanel);

                        SwingUtilities.invokeLater(() -> {
                            JScrollBar bar = scrollMessages.getVerticalScrollBar();
                            if (wasAtBottom) {
                                bar.setValue(bar.getMaximum());
                            } else {
                                bar.setValue(savedScrollPosition);
                            }
                        });
                    } else {
                        JScrollBar verticalBar = scrollMessages.getVerticalScrollBar();
                        int currentValue = verticalBar.getValue();
                        int maximum = verticalBar.getMaximum();
                        int extent = verticalBar.getVisibleAmount();
                        boolean wasAtBottom = (currentValue + extent >= maximum - 50);
                        int savedScrollPosition = currentValue;

                        StringBuilder resultString = new StringBuilder();
                        for (OutputData o : status.getResponse().getOutputData()) {
                            if (o.getType().equalsIgnoreCase(OutputData.TYPE_STRING)) {
                                resultString.append(o.getStringData()).append(System.lineSeparator()).append(System.lineSeparator());
                            }
                        }
                        AiChatMessageMarkdownPanel msgPanel = incomingMessageRef.get();
                        msgPanel.getMessage().setContent(resultString.toString());
                        msgPanel.setMessage(msgPanel.getMessage(), owner);
                        msgPanel.repaint();
                        msgPanel.updateUI();
                        styleMessageBubble(msgPanel);

                        SwingUtilities.invokeLater(() -> {
                            JScrollBar bar = scrollMessages.getVerticalScrollBar();
                            if (wasAtBottom) {
                                bar.setValue(bar.getMaximum());
                            } else {
                                bar.setValue(savedScrollPosition);
                            }
                        });
                    }
                }

                progress.setIndeterminate(false);
                progress.setVisible(false);
                taPrompt.setText("");

                cmdSubmit.setVisible(true);
                cmdInterrupt.setVisible(false);
            }
        };

        worker.execute();
    }

    private void cmdCloseActionPerformed(java.awt.event.ActionEvent evt) {
        this.setVisible(false);
        this.dispose();
    }

    private void cmdCopyActionPerformed(java.awt.event.ActionEvent evt) {
        AiChatMessageMarkdownPanel p = findLastMessagePanel();
        if (p != null) {
            String htmlText = p.getAsHtml();
            if (htmlText == null) {
                htmlText = p.getAsText();
            }
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(new AiChatMessageMarkdownPanel.HTMLTransferable(htmlText, p.getAsText()), null);
        }
    }

    private void cmdProcessOutputActionPerformed(java.awt.event.ActionEvent evt) {
        if (this.inputAdapter instanceof AssistantFlowAdapter && this.result != null) {

            AiChatMessageMarkdownPanel p = findLastMessagePanel();
            if (p != null) {
                String resultText = p.getSelectedAsText();
                if (resultText == null) {
                    resultText = p.getMessage().getContent();
                }

                for (OutputData o : this.result.getResponse().getOutputData()) {
                    if (o.getType().equalsIgnoreCase(OutputData.TYPE_STRING)) {
                        o.setStringData(resultText);
                    }
                }
            }

            ((AssistantFlowAdapter) this.inputAdapter).processOutput(capability, this.result);
        }
        this.setVisible(false);
        this.dispose();
    }

    private void cmdInterruptActionPerformed(java.awt.event.ActionEvent evt) {
        this.interrupted = true;
    }

    /**
     * Applies rounded FlatLineBorder and gray-tone colors to an assistant message bubble.
     */
    private void styleMessageBubble(AiChatMessageMarkdownPanel panel) {
        panel.setOpaque(true);
        panel.setBackground(ASSISTANT_BUBBLE_BG);
        panel.setBorder(new FlatLineBorder(new Insets(8, 8, 8, 8), BUBBLE_BORDER_COLOR, 1, 12));
        applyBubbleColors(panel, ASSISTANT_BUBBLE_BG, DefaultColorTheme.COLOR_DARK_GREY);
    }

    private void applyBubbleColors(java.awt.Container container, Color bg, Color iconBg) {
        for (Component c : container.getComponents()) {
            if (c instanceof JLabel && ((JLabel) c).getIcon() != null) {
                c.setBackground(iconBg);
            } else {
                c.setBackground(bg);
            }
            if (c instanceof java.awt.Container) {
                applyBubbleColors((java.awt.Container) c, bg, iconBg);
            }
        }
    }

    /**
     * Finds the last AiChatMessageMarkdownPanel in pnlMessages.
     */
    private AiChatMessageMarkdownPanel findLastMessagePanel() {
        for (int i = pnlMessages.getComponentCount() - 1; i >= 0; i--) {
            Component comp = pnlMessages.getComponent(i);
            if (comp instanceof AiChatMessageMarkdownPanel) {
                return (AiChatMessageMarkdownPanel) comp;
            }
        }
        return null;
    }

    private List<ParameterData> getParameters() {
        List<ParameterData> parameters = new ArrayList<>();
        for (int i = 0; i < this.capability.getParameters().size(); i++) {
            ParameterData d = new ParameterData();
            d.setId(this.capability.getParameters().get(i).getId());
            Component component = this.pnlParameters.getComponent(2 * i + 1);
            if (component instanceof JTextField) {
                String value = ((JTextField) component).getText();
                d.setValue(value);
            } else if (component instanceof JComboBox) {
                String value = ((JComboBox) component).getSelectedItem().toString();
                d.setValue(value);
            }
            parameters.add(d);
        }
        return parameters;
    }

    // ==================== Transcription ====================

    private void cmdTranscribeActionPerformed(java.awt.event.ActionEvent evt) {
        if (!isRecording) {
            startRecording();
            ClientSettings.getInstance().setConfiguration(ClientSettings.CONF_SOUND_LASTRECORDINGDEVICE, (String) this.cmbDevices.getSelectedItem());
        } else {
            try {
                stopRecording();
            } catch (IOException ex) {
                log.error("Error stopping recording", ex);
                ThreadUtils.showErrorDialog(this, "Fehler beim Stoppen der Aufnahme: " + ex.getMessage(), DesktopUtils.POPUP_TITLE_ERROR);
            }
        }
    }

    private List<InputData> getTranscribeInputs(byte[] wavContent) {
        ArrayList<InputData> inputs = new ArrayList<>();
        InputData i = new InputData();
        i.setFileName("Sounddatei.wav");
        i.setType("file");
        i.setBase64(true);
        i.setData(wavContent);
        inputs.add(i);
        return inputs;
    }

    private void startRecording() {
        try {
            AudioFormat audioFormat = AudioUtils.getAudioFormat();

            String selectedDeviceName = (String) this.cmbDevices.getSelectedItem();

            Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
            Mixer.Info selectedMixerInfo = null;
            for (Mixer.Info mixerInfo : mixerInfos) {
                if (mixerInfo.getName().equals(selectedDeviceName)) {
                    selectedMixerInfo = mixerInfo;
                    break;
                }
            }

            if (selectedMixerInfo == null) {
                log.error("Selected mixer device not found.");
                return;
            }

            Mixer mixer = AudioSystem.getMixer(selectedMixerInfo);

            DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
            targetDataLine = (TargetDataLine) mixer.getLine(dataLineInfo);
            targetDataLine.open(audioFormat);
            targetDataLine.start();

            byteArrayOutputStream = new ByteArrayOutputStream();

            isRecording = true;
            cmdTranscribe.setOpaque(true);
            cmdTranscribe.setBackground(DefaultColorTheme.COLOR_LOGO_GREEN);

            new Thread(() -> {
                try {
                    byte[] buffer = new byte[4096];
                    while (isRecording) {
                        int bytesRead = targetDataLine.read(buffer, 0, buffer.length);
                        byteArrayOutputStream.write(buffer, 0, bytesRead);
                    }
                } catch (Exception e) {
                    log.error("Unable to read microphone audio stream", e);
                    ThreadUtils.showErrorDialog(this, "Aufnahmefehler: " + e.getMessage(), DesktopUtils.POPUP_TITLE_ERROR);
                }
            }).start();
        } catch (Exception ex) {
            log.error("Unable to start recording microphone audio stream", ex);
            ThreadUtils.showErrorDialog(this, "Aufnahme konnte nicht gestartet werden: " + ex.getMessage(), DesktopUtils.POPUP_TITLE_ERROR);
        }
    }

    private void stopRecording() throws IOException {
        isRecording = false;

        if (targetDataLine != null) {
            targetDataLine.stop();
            targetDataLine.close();
        }
        byteArrayOutputStream.flush();
        byte[] dictatePart = byteArrayOutputStream.toByteArray();

        this.cmdTranscribe.setIcon(new ImageIcon(getClass().getResource("/icons16/material/baseline_hourglass_top_black_48dp.png")));
        this.cmdTranscribe.setOpaque(true);
        this.cmdTranscribe.setBackground(Color.ORANGE);
        try {

            List<ParameterData> params = new ArrayList<>();
            if (transcribeCapability.getParameters() != null && !transcribeCapability.getParameters().isEmpty()) {
                params = getParameters();
            }

            final List<ParameterData> fParams = params;

            AtomicReference<AiRequestStatus> resultRef = new AtomicReference<>();

            SwingWorker<Void, Void> worker = new SwingWorker<>() {

                @Override
                protected Void doInBackground() throws Exception {
                    cmdTranscribe.setIcon(new ImageIcon(getClass().getResource("/icons16/material/baseline_hourglass_top_black_48dp.png")));
                    cmdTranscribe.setOpaque(true);
                    cmdTranscribe.setBackground(Color.ORANGE);

                    List<InputData> inputs = getTranscribeInputs(AudioUtils.generateWAV(dictatePart));

                    ClientSettings settings = ClientSettings.getInstance();
                    try {
                        JLawyerServiceLocator locator = JLawyerServiceLocator.getInstance(settings.getLookupProperties());

                        AiRequestStatus status = locator.lookupIntegrationServiceRemote().submitAssistantRequest(
                            transcribeConfig,
                            transcribeCapability.getRequestType(),
                            transcribeCapability.getActionId(),
                            transcribeCapability.getModelRef(),
                            null,
                            null,
                            transcribeCapability.isAsyncRecommended(),
                            fParams,
                            inputs,
                            null,
                            null,
                            null
                        );

                        resultRef.set(status);

                    } catch (Throwable t) {
                        log.error("Error processing AI transcription request", t);
                        AiRequestStatus status = new AiRequestStatus();
                        status.setStatus("failed");
                        status.setStatusDetails(t.getMessage());
                        resultRef.set(status);
                    }
                    return null;
                }

                @Override
                protected void done() {
                    cmdTranscribe.setIcon(new ImageIcon(getClass().getResource("/icons16/material/baseline_mic_black_48dp.png")));
                    cmdTranscribe.setOpaque(true);
                    cmdTranscribe.setBackground(cmdTranscribeOriginalBackground);

                    AiRequestStatus status = resultRef.get();
                    if (status != null) {
                        String resultText = "";
                        if (status.isError()) {
                            resultText = status.getStatus() + ": " + status.getStatusDetails();
                            log.error("Transcription failed: " + status.getStatusDetails());
                            ThreadUtils.showErrorDialog(AssistantGenericPanel.this, "Transkription fehlgeschlagen: " + status.getStatusDetails(), DesktopUtils.POPUP_TITLE_ERROR);
                        } else {
                            StringBuilder resultString = new StringBuilder();
                            for (OutputData o : status.getResponse().getOutputData()) {
                                if (o.getType().equalsIgnoreCase(OutputData.TYPE_STRING)) {
                                    resultString.append(o.getStringData());
                                }
                            }
                            resultText = resultString.toString();
                        }

                        if (!resultText.isEmpty() && !status.isError()) {
                            String currentText = taPrompt.getText();
                            if (!currentText.isEmpty() && !currentText.endsWith(" ") && !currentText.endsWith("\n")) {
                                currentText += " ";
                            }
                            taPrompt.setText(currentText + resultText);
                            taPrompt.requestFocusInWindow();
                            taPrompt.setCaretPosition(taPrompt.getText().length());
                        }
                    }
                }
            };

            worker.execute();

        } catch (Exception ex) {
            log.error("Error during transcription", ex);
            ThreadUtils.showErrorDialog(this, "Transkriptionsfehler: " + ex.getMessage(), DesktopUtils.POPUP_TITLE_ERROR);
        }
    }
}
