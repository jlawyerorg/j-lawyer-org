package com.jdimension.jlawyer.client.assistant;

import com.jdimension.jlawyer.ai.AiCapability;
import com.jdimension.jlawyer.ai.AiModel;
import com.jdimension.jlawyer.ai.AiRequestStatus;
import com.jdimension.jlawyer.ai.AiResponse;
import com.jdimension.jlawyer.ai.ConfigurationData;
import com.jdimension.jlawyer.ai.ConfigurationUtils;
import com.jdimension.jlawyer.ai.InputData;
import com.jdimension.jlawyer.ai.Message;
import com.jdimension.jlawyer.ai.OutputData;
import com.jdimension.jlawyer.ai.Parameter;
import com.jdimension.jlawyer.ai.ParameterData;
import com.jdimension.jlawyer.ai.ToolCall;
import com.jdimension.jlawyer.ai.ToolDefinition;
import com.jdimension.jlawyer.persistence.AssistantPrompt;
import com.jdimension.jlawyer.client.editors.files.ArchiveFilePanel;
import com.jdimension.jlawyer.client.settings.ClientSettings;
import com.jdimension.jlawyer.client.settings.UserSettings;
import com.jdimension.jlawyer.client.utils.AudioUtils;
import com.jdimension.jlawyer.client.utils.ComponentUtils;
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
import com.jdimension.jlawyer.client.utils.DesktopUtils;
import com.jdimension.jlawyer.client.utils.SelectAttachmentDialog;
import com.jdimension.jlawyer.documents.DocumentPreview;
import com.jdimension.jlawyer.persistence.ArchiveFileDocumentsBean;
import com.jdimension.jlawyer.services.ArchiveFileServiceRemote;
import com.jdimension.jlawyer.services.IntegrationServiceRemote;
import com.formdev.flatlaf.ui.FlatLineBorder;
import java.io.File;
import java.nio.file.Files;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
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
 * Modern chat-style dialog for AI assistant interactions.
 * Replaces the older AssistantChatDialog with a layout similar to ChatGPT/Claude:
 * messages in the center, input at the bottom.
 *
 * @author jens
 */
public class AssistantChatPanel extends JDialog {

    private static final Logger log = Logger.getLogger(AssistantChatPanel.class.getName());

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

    // used for calling document creation dialog
    private ArchiveFilePanel caseView = null;

    // cache incoming and outgoing messages
    private List<Message> messages = new ArrayList<>();

    // tool calling support
    private final ToolRegistry toolRegistry = new ToolRegistry();
    private boolean modelSupportsTools = false;

    // static cache for assistant models to avoid repeated server calls
    private static Map<AssistantConfig, List<AiModel>> cachedModels = null;

    private String initialPrompt = "";

    // tracks whether the first message has been sent (to include input data)
    private boolean isFirstMessage = true;

    private static final int USER_MSG_TRUNCATE_LENGTH = 300;

    // Gray-tone bubble colors (no blue)
    private static final Color USER_BUBBLE_BG = new Color(232, 232, 232);
    private static final Color ASSISTANT_BUBBLE_BG = new Color(245, 245, 245);
    private static final Color USER_ICON_BG = new Color(150, 150, 150);
    private static final Color BUBBLE_BORDER_COLOR = new Color(215, 215, 215);

    // UI components
    private JPanel pnlTitle;
    private JLabel lblRequestType;
    private JButton cmdPrompt;
    private JButton cmdResetChat;
    private JLabel lblContextToggle;
    private JPanel pnlContextContent;
    private JTextArea taInputString;
    private JPanel pnlParameters;
    private JScrollPane scrollMessages;
    private JPanel pnlMessages;
    private JProgressBar progress;
    private JTextArea taPrompt;
    private JButton cmdSubmit;
    private JButton cmdInterrupt;
    private JButton cmdAttach;
    private JButton cmdTranscribe;
    private JComboBox<String> cmbDevices;
    private JButton cmdClose;
    private JButton cmdProcessOutput;
    private JButton cmdNewDocument;
    private JButton cmdCopy;
    private JLabel lblSupportsTools;
    private JPanel pnlPlaceholder;
    private JPopupMenu popAssistant;
    private JPanel bottomPanel;

    public AssistantChatPanel(ArchiveFileBean selectedCase, AssistantConfig config, AiCapability c, AssistantInputAdapter inputAdapter, JDialog parent, boolean modal) {
        super(parent, modal);
        this.initialize(selectedCase, config, c, inputAdapter);
    }

    public AssistantChatPanel(ArchiveFileBean selectedCase, AssistantConfig config, AiCapability c, AssistantInputAdapter inputAdapter, JFrame parent, boolean modal) {
        super(parent, modal);
        this.initialize(selectedCase, config, c, inputAdapter);
    }

    private void initialize(ArchiveFileBean selectedCase, AssistantConfig config, AiCapability c, AssistantInputAdapter inputAdapter) {
        buildUI();

        this.scrollMessages.getVerticalScrollBar().setUnitIncrement(32);

        if (inputAdapter != null && inputAdapter instanceof ArchiveFilePanel) {
            this.caseView = (ArchiveFilePanel) inputAdapter;
        }
        this.cmdNewDocument.setEnabled(this.caseView != null);

        this.config = config;
        this.capability = c;
        this.inputAdapter = inputAdapter;

        // Check if selected model supports tool calling
        if (AiCapability.REQUESTTYPE_CHAT.equals(c.getRequestType()) && c.getModelRef() != null) {
            try {
                if (cachedModels == null) {
                    ClientSettings cs = ClientSettings.getInstance();
                    JLawyerServiceLocator loc = JLawyerServiceLocator.getInstance(cs.getLookupProperties());
                    cachedModels = loc.lookupIntegrationServiceRemote().getAssistantModels();
                }
                for (List<AiModel> models : cachedModels.values()) {
                    for (AiModel m : models) {
                        if (m.getName().equals(c.getModelRef()) && m.isSupportsTools()) {
                            this.modelSupportsTools = true;
                            break;
                        }
                    }
                    if (this.modelSupportsTools) {
                        break;
                    }
                }
            } catch (Exception ex) {
                log.warn("Could not check model tool support", ex);
            }
        }

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

        this.taPrompt.setEnabled(false);
        if (c.getDefaultPrompt() != null && c.getDefaultPrompt().getDefaultPrompt() != null) {
            if (!c.getDefaultPrompt().getDefaultPrompt().contains("{{")) {
                this.taPrompt.setText(c.getDefaultPrompt().getDefaultPrompt());
            } else {
                HashMap<String, Object> placeHolders = TemplatesUtil.getPlaceHolderValues(c.getDefaultPrompt().getDefaultPrompt(), selectedCase, this.parties, null, null, this.allPartyTypes, this.formPlaceHolders, this.formPlaceHolderValues, this.caseLawyer, this.caseAssistant);
                String promptWithValues = TemplatesUtil.replacePlaceHolders(c.getDefaultPrompt().getDefaultPrompt(), placeHolders);
                this.taPrompt.setText(promptWithValues);
            }
            this.initialPrompt = this.taPrompt.getText();
            this.taPrompt.setEnabled(true);
        } else if (c.isCustomPrompts()) {
            this.taPrompt.setEnabled(true);
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

        this.lblSupportsTools.setIcon(new ImageIcon(getClass().getResource("/icons16/material/smart_toy_20dp_DE313B_FILL0_wght400_GRAD0_opsz20.png")));
        this.lblSupportsTools.setText("nicht agentenfähig");
        this.lblSupportsTools.setToolTipText("keine Werkzeugunterstützung");
        
        if (AiCapability.REQUESTTYPE_CHAT.equals(c.getRequestType()) && c.getModelRef() != null) {
            if (this.modelSupportsTools) {
                this.lblSupportsTools.setIcon(new ImageIcon(getClass().getResource("/icons16/material/smart_toy_20dp_97BF0D_FILL0_wght400_GRAD0_opsz20.png")));
                this.lblSupportsTools.setText("agentenfähig");
                this.lblSupportsTools.setToolTipText("unterstützt Werkzeuge");
            }
            
        }
        this.lblSupportsTools.revalidate();
        this.lblSupportsTools.repaint();

        if (this.modelSupportsTools) {
            pnlPlaceholder = new JPanel(new GridBagLayout());
            pnlPlaceholder.setBackground(Color.WHITE);
            pnlPlaceholder.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            pnlPlaceholder.setAlignmentX(0.5f);
            JLabel lblPlaceholder = new JLabel(
                "<html><center>"
                + "<span style='font-size:24pt;color:#787878;'>"
                + "Ingo erledigt Routinearbeiten - einfach ausprobieren:<br>"
                + "<i>\"Welche Funktionen kannst Du aufrufen?\"</i>"
                + "<br><br>"
                + "Beispiel:<br>"
                + "<i>\"Ermittle die heutigen offenen Termine und Fristen<br>"
                + "und erstelle mir eine Agenda.<br>"
                + "Priorisiere anhand der Formulierungen.\"</i>"
                + "</span>"
                + "</center></html>"
            );
            lblPlaceholder.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            pnlPlaceholder.add(lblPlaceholder, new GridBagConstraints());
            pnlMessages.add(Box.createVerticalGlue());
            pnlMessages.add(pnlPlaceholder);
            pnlMessages.add(Box.createVerticalGlue());
        }

        this.taInputString.setText("");
        boolean hasInputData = false;
        for (InputData i : inputAdapter.getInputs(c)) {
            if (InputData.TYPE_STRING.equalsIgnoreCase(i.getType())) {
                this.taInputString.append(i.getStringData());
                this.taInputString.append(System.lineSeparator());
                hasInputData = true;
            }
        }

        // Hide context toggle if there is no input data
        if (!hasInputData) {
            this.lblContextToggle.setVisible(false);
            this.pnlContextContent.setVisible(false);
        } else {
            updateContextToggleLabel(false);
        }

        // Enter sends message, Shift+Enter inserts newline
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
    }

    private void buildUI() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setTitle("Assistent Ingo");
        getContentPane().setLayout(new BorderLayout());

        popAssistant = new JPopupMenu();

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

        lblSupportsTools = new JLabel();
        lblSupportsTools.setFont(lblSupportsTools.getFont().deriveFont(lblSupportsTools.getFont().getSize() - 2f));
        lblSupportsTools.setForeground(new Color(153, 153, 153));
        lblSupportsTools.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));

        JPanel pnlCenterTitle = new JPanel();
        pnlCenterTitle.setOpaque(false);
        pnlCenterTitle.setLayout(new BoxLayout(pnlCenterTitle, BoxLayout.X_AXIS));
        pnlCenterTitle.add(lblRequestType);
        pnlCenterTitle.add(lblSupportsTools);
        pnlCenterTitle.add(Box.createHorizontalGlue());
        pnlTitle.add(pnlCenterTitle, BorderLayout.CENTER);

        JPanel pnlTitleButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        pnlTitleButtons.setOpaque(false);

        // Microphone device dropdown in header
        cmbDevices = new JComboBox<>();
        pnlTitleButtons.add(cmbDevices);

        cmdPrompt = new JButton(new ImageIcon(getClass().getResource("/icons16/material/j-lawyer-ai.png")));
        cmdPrompt.setToolTipText("Eigene Prompts ausw\u00e4hlen");
        cmdPrompt.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                cmdPromptMouseReleased(evt);
            }
        });
        pnlTitleButtons.add(cmdPrompt);

        cmdResetChat = new JButton(new ImageIcon(getClass().getResource("/icons16/material/restart_alt_24dp_0E72B5.png")));
        cmdResetChat.setToolTipText("Chat-Historie zur\u00fccksetzen und neuen Chat beginnen");
        cmdResetChat.addActionListener(e -> cmdResetChatActionPerformed(e));
        pnlTitleButtons.add(cmdResetChat);

        pnlTitle.add(pnlTitleButtons, BorderLayout.EAST);
        pnlTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerPanel.add(pnlTitle);

        // --- Context toggle ---
        lblContextToggle = new JLabel("\u25b6 Kontext anzeigen");
        lblContextToggle.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        lblContextToggle.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        lblContextToggle.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblContextToggle.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                boolean visible = !pnlContextContent.isVisible();
                pnlContextContent.setVisible(visible);
                updateContextToggleLabel(visible);
                headerPanel.revalidate();
            }
        });
        headerPanel.add(lblContextToggle);

        // --- Context content (collapsed by default) ---
        pnlContextContent = new JPanel(new BorderLayout());
        pnlContextContent.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlContextContent.setVisible(false);
        taInputString = new JTextArea();
        taInputString.setColumns(20);
        taInputString.setRows(5);
        taInputString.setLineWrap(true);
        taInputString.setWrapStyleWord(true);
        taInputString.setEditable(false);
        JScrollPane scrollContext = new JScrollPane(taInputString);
        scrollContext.setPreferredSize(new Dimension(0, 120));
        scrollContext.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        pnlContextContent.add(scrollContext, BorderLayout.CENTER);
        pnlContextContent.setBorder(BorderFactory.createEmptyBorder(0, 8, 4, 8));
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

        cmdAttach = new JButton(new ImageIcon(getClass().getResource("/icons/attach.png")));
        cmdAttach.setToolTipText("Dokument als Kontext anh\u00e4ngen");
        cmdAttach.putClientProperty("JButton.buttonType", "roundRect");
        cmdAttach.addActionListener(e -> cmdAttachActionPerformed(e));
        btnPanel.add(cmdAttach);

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

        cmdNewDocument = new JButton("neues Dokument");
        cmdNewDocument.setIcon(new ImageIcon(getClass().getResource("/icons/editcopy.png")));
        cmdNewDocument.addActionListener(e -> cmdNewDocumentActionPerformed(e));
        rightActions.add(cmdNewDocument);

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
                ComponentUtils.storeDialogSize(AssistantChatPanel.this);
            }
        });

        pack();
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            // Center after restoreDialogSize has set the final dimensions
            if (getOwner() != null) {
                FrameUtils.centerDialogOnParentMonitor(this, getOwner().getLocation());
            } else {
                FrameUtils.centerDialog(this, null);
            }
        }
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

    // ==================== Business Logic (ported 1:1 from AssistantChatDialog) ====================

    private void cmdSubmitActionPerformed(java.awt.event.ActionEvent evt) {
        SwingUtilities.invokeLater(this::startBackgroundTask);
    }

    private void startBackgroundTask() {

        this.interrupted = false;

        this.cmdSubmit.setVisible(false);
        this.cmdInterrupt.setVisible(true);

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
                ClientSettings settings = ClientSettings.getInstance();

                Message incomingMsg = new Message();
                incomingMsg.setRole(Message.ROLE_ASSISTANT);
                incomingMsg.setContent("...");
                AiChatMessageMarkdownPanel incomingMsgPanel = createStyledMessagePanel(incomingMsg, owner);
                try {
                    JLawyerServiceLocator locator = JLawyerServiceLocator.getInstance(settings.getLookupProperties());

                    String fullPrompt = taPrompt.getText();
                    if (isFirstMessage) {
                        // First message: include full input (or selected portion if user selected something)
                        String inputText = taInputString.getSelectedText();
                        if (inputText == null || inputText.isEmpty()) {
                            inputText = taInputString.getText();
                        }
                        if (inputText != null && !inputText.trim().isEmpty()) {
                            fullPrompt = fullPrompt + System.lineSeparator() + System.lineSeparator() + inputText;
                        }
                        isFirstMessage = false;
                    } else if (taInputString.getSelectedText() != null) {
                        // Subsequent messages: only include manually selected text
                        fullPrompt = fullPrompt + System.lineSeparator() + System.lineSeparator() + taInputString.getSelectedText();
                    }

                    Message outgoingMessage = new Message();
                    outgoingMessage.setRole(Message.ROLE_USER);
                    outgoingMessage.setContent(fullPrompt);
                    messages.add(outgoingMessage);
                    SwingUtilities.invokeAndWait(() -> {
                        AiChatMessageMarkdownPanel outGoingMsgPanel = createStyledMessagePanel(outgoingMessage, owner);
                        Dimension maxSize = outGoingMsgPanel.getPreferredSize();
                        maxSize.setSize(pnlMessages.getWidth(), maxSize.getHeight());
                        outGoingMsgPanel.setPreferredSize(maxSize);

                        // Remove placeholder hint on first message
                        removePlaceholder();
                        // Add spacing before this message pair
                        if (pnlMessages.getComponentCount() > 0) {
                            pnlMessages.add(Box.createRigidArea(new Dimension(0, 12)));
                        }

                        // Wrap user message with truncation if content is long
                        JPanel userWrapper = wrapUserMessage(outGoingMsgPanel, outgoingMessage.getContent(), owner);
                        if (userWrapper != null) {
                            userWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
                            pnlMessages.add(userWrapper);
                        } else {
                            outGoingMsgPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
                            pnlMessages.add(outGoingMsgPanel);
                        }

                        pnlMessages.add(Box.createRigidArea(new Dimension(0, 12)));

                        incomingMsgPanel.setPreferredSize(maxSize);
                        incomingMsgPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
                        pnlMessages.add(incomingMsgPanel);
                        pnlMessages.revalidate();
                        pnlMessages.repaint();

                        SwingUtilities.invokeLater(() -> {
                            JScrollBar bar = scrollMessages.getVerticalScrollBar();
                            bar.setValue(bar.getMaximum());
                        });
                    });

                    List<ConfigurationData> promptConfigs = null;
                    if (capability.getConfigurationValues() != null && !capability.getConfigurationValues().isEmpty()) {
                        promptConfigs = ConfigurationUtils.fromProperties(capability.getConfigurationValues());
                    }
                    List<ToolDefinition> tools = null;
                    if (AiCapability.REQUESTTYPE_CHAT.equals(capability.getRequestType())) {
                        tools = toolRegistry.getToolDefinitions();
                    }
                    AiRequestStatus status = locator.lookupIntegrationServiceRemote().submitAssistantRequest(config, capability.getRequestType(), capability.getActionId(), capability.getModelRef(), fullPrompt, capability.getSystemPrompt(), capability.isAsyncRecommended(), fParams, null, messages, promptConfigs, tools);
                    taInputString.setCaretPosition(0);

                    // Tool calling loop: handle TOOL_CALL_PENDING responses
                    final List<ConfigurationData> fPromptConfigs = promptConfigs;
                    final List<ToolDefinition> fTools = tools;
                    while (status.getResponse() != null
                            && AiResponse.STATUS_TOOL_CALL_PENDING.equals(status.getStatus())
                            && status.getResponse().getToolCalls() != null
                            && !status.getResponse().getToolCalls().isEmpty()
                            && !interrupted) {

                        AiResponse toolResponse = status.getResponse();

                        // Display any text the assistant said before the tool call
                        StringBuilder textBeforeTools = new StringBuilder();
                        for (OutputData o : toolResponse.getOutputData()) {
                            if (o.getType().equalsIgnoreCase(OutputData.TYPE_STRING) && o.getStringData() != null && !o.getStringData().trim().isEmpty()) {
                                textBeforeTools.append(o.getStringData());
                            }
                        }
                        if (textBeforeTools.length() > 0) {
                            final String assistantText = textBeforeTools.toString();
                            SwingUtilities.invokeAndWait(() -> {
                                incomingMsgPanel.getMessage().setContent(assistantText);
                                incomingMsgPanel.setMessage(incomingMsgPanel.getMessage(), owner);
                                incomingMsgPanel.repaint();
                                incomingMsgPanel.updateUI();
                                restyleMessagePanel(incomingMsgPanel);
                                scrollToBottom();
                            });
                        }

                        // Update messages from response (full conversation state)
                        if (toolResponse.getMessages() != null && !toolResponse.getMessages().isEmpty()) {
                            messages = new ArrayList<>(toolResponse.getMessages());
                        }

                        // Phase 1: Create panels and handle approvals for all tool calls
                        List<ToolCall> toolCalls = toolResponse.getToolCalls();
                        List<String> toolSummaries = new ArrayList<>();
                        List<String> riskLevels = new ArrayList<>();
                        List<String> paramTooltips = new ArrayList<>();
                        List<Boolean> approvals = new ArrayList<>();
                        List<JPanel> toolPanels = new ArrayList<>();

                        for (ToolCall tc : toolCalls) {
                            String toolSummary = toolRegistry.formatToolCallSummary(tc);
                            final String riskLevel = toolRegistry.getRiskLevel(tc.getToolName());
                            final String paramTooltip = formatParamTooltip(tc.getArguments());
                            toolSummaries.add(toolSummary);
                            riskLevels.add(riskLevel);
                            paramTooltips.add(paramTooltip);

                            boolean approved;
                            String displayMsg;
                            if (!toolRegistry.requiresApproval(tc.getToolName())) {
                                approved = true;
                                displayMsg = toolSummary;
                            } else if (toolRegistry.isApproved(tc.getToolName())) {
                                approved = true;
                                displayMsg = toolSummary + " (automatisch genehmigt)";
                            } else {
                                // Show approval dialog on EDT
                                final AtomicReference<String> approvalResult = new AtomicReference<>();
                                final String dialogTitle = toolSummary;
                                SwingUtilities.invokeAndWait(() -> {
                                    ToolDefinition toolDef = toolRegistry.getToolDefinition(tc.getToolName());
                                    ToolApprovalDialog approvalDialog = new ToolApprovalDialog(
                                            owner, dialogTitle, tc.getToolName(), riskLevel, toolDef, tc.getArguments());
                                    approvalDialog.setVisible(true);
                                    approvalResult.set(approvalDialog.getApprovalResult());
                                });

                                String decision = approvalResult.get();
                                if ("always".equals(decision)) {
                                    toolRegistry.approveAlways(tc.getToolName());
                                    approved = true;
                                } else if ("session".equals(decision)) {
                                    toolRegistry.approveForSession(tc.getToolName());
                                    approved = true;
                                } else if ("allow".equals(decision)) {
                                    approved = true;
                                } else {
                                    approved = false;
                                }
                                displayMsg = toolSummary;
                            }
                            approvals.add(approved);

                            // Create panel for this tool call
                            final String fDisplayMsg = displayMsg;
                            final String fRiskLevel = riskLevel;
                            final String fParamTooltip = paramTooltip;
                            final AtomicReference<JPanel> panelRef = new AtomicReference<>();
                            SwingUtilities.invokeAndWait(() -> {
                                panelRef.set(addToolStatusMessage(fDisplayMsg, fRiskLevel, fParamTooltip));
                            });
                            toolPanels.add(panelRef.get());
                        }

                        // Phase 2: Execute tools and update panels with results
                        List<Message> toolResultMessages = new ArrayList<>();
                        for (int i = 0; i < toolCalls.size(); i++) {
                            ToolCall tc = toolCalls.get(i);
                            boolean approved = approvals.get(i);
                            String toolSummary = toolSummaries.get(i);
                            JPanel toolPanel = toolPanels.get(i);

                            Message toolResultMsg = new Message();
                            toolResultMsg.setRole(Message.ROLE_TOOL);
                            toolResultMsg.setToolCallId(tc.getId());
                            toolResultMsg.setToolName(tc.getToolName());

                            if (approved) {
                                // Execute the tool
                                String toolResult;
                                try {
                                    toolResult = toolRegistry.execute(tc.getToolName(), tc.getArguments());
                                } catch (Exception ex) {
                                    toolResult = "{\"error\": \"" + ex.getMessage() + "\"}";
                                }
                                toolResultMsg.setContent(toolResult);

                                final String displayResult = toolSummary + " — Ergebnis erhalten";
                                final JPanel tp = toolPanel;
                                SwingUtilities.invokeAndWait(() -> {
                                    updateToolStatusMessage(tp, displayResult);
                                });
                            } else {
                                toolResultMsg.setContent("{\"denied\": true, \"message\": \"Der Benutzer hat diesen Werkzeugaufruf abgelehnt.\"}");

                                final String displayDenied = toolSummary + " — abgelehnt";
                                final JPanel tp = toolPanel;
                                SwingUtilities.invokeAndWait(() -> {
                                    updateToolStatusMessage(tp, displayDenied);
                                });
                            }
                            toolResultMessages.add(toolResultMsg);
                        }

                        // Add all tool result messages and send follow-up
                        messages.addAll(toolResultMessages);

                        // Send follow-up request with tool results
                        status = locator.lookupIntegrationServiceRemote().submitAssistantRequest(config, capability.getRequestType(), capability.getActionId(), capability.getModelRef(), null, capability.getSystemPrompt(), capability.isAsyncRecommended(), fParams, null, messages, fPromptConfigs, fTools);
                    }

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
                                restyleMessagePanel(incomingMsgPanel);

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

                        Message errorMsg = new Message();
                        errorMsg.setContent(status.getStatus() + ": " + status.getStatusDetails());
                        errorMsg.setRole(Message.ROLE_ASSISTANT);
                        AiChatMessageMarkdownPanel msgPanel = createStyledMessagePanel(errorMsg, owner);
                        Dimension maxSize = msgPanel.getPreferredSize();
                        maxSize.setSize(pnlMessages.getWidth(), maxSize.getHeight());
                        msgPanel.setPreferredSize(maxSize);
                        msgPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
                        removePlaceholder();
                        if (pnlMessages.getComponentCount() > 0) {
                            pnlMessages.add(Box.createRigidArea(new Dimension(0, 12)));
                        }
                        pnlMessages.add(msgPanel);

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
                        messages.add(msgPanel.getMessage());
                        msgPanel.setMessage(msgPanel.getMessage(), owner);
                        msgPanel.repaint();
                        msgPanel.updateUI();
                        restyleMessagePanel(msgPanel);

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

    private void updateContextToggleLabel(boolean visible) {
        int charCount = taInputString.getText().length();
        String charInfo = " (" + charCount + " Zeichen)";
        lblContextToggle.setText(visible ? "\u25bc Kontext verbergen" + charInfo : "\u25b6 Kontext anzeigen" + charInfo);
    }

    private void cmdAttachActionPerformed(ActionEvent evt) {
        String caseId = (this.selectedCase != null) ? this.selectedCase.getId() : null;
        SelectAttachmentDialog dlg = new SelectAttachmentDialog(this, true, caseId);
        FrameUtils.centerDialog(dlg, this);
        dlg.setVisible(true);

        try {
            ClientSettings settings = ClientSettings.getInstance();
            JLawyerServiceLocator locator = JLawyerServiceLocator.getInstance(settings.getLookupProperties());
            StringBuilder sb = new StringBuilder();

            ArchiveFileDocumentsBean[] docs = dlg.getSelectedDocuments();
            if (docs != null) {
                ArchiveFileServiceRemote afs = locator.lookupArchiveFileServiceRemote();
                for (ArchiveFileDocumentsBean doc : docs) {
                    DocumentPreview preview = afs.getDocumentPreview(doc.getId(), DocumentPreview.TYPE_TEXT);
                    sb.append("--- ").append(doc.getName()).append(" ---").append(System.lineSeparator());
                    sb.append(preview.getText()).append(System.lineSeparator()).append(System.lineSeparator());
                }
            }

            File[] files = dlg.getSelectedFiles();
            if (files != null) {
                IntegrationServiceRemote is = locator.lookupIntegrationServiceRemote();
                for (File f : files) {
                    byte[] data = Files.readAllBytes(f.toPath());
                    String text = is.extractText(data, f.getName(), -1);
                    sb.append("--- ").append(f.getName()).append(" ---").append(System.lineSeparator());
                    sb.append(text).append(System.lineSeparator()).append(System.lineSeparator());
                }
            }

            if (sb.length() > 0) {
                this.taInputString.append(sb.toString());
                this.lblContextToggle.setVisible(true);
                this.pnlContextContent.setVisible(true);
                updateContextToggleLabel(true);
                this.pnlContextContent.getParent().revalidate();
            }
        } catch (Exception ex) {
            log.error("Error attaching document context", ex);
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cmdCloseActionPerformed(java.awt.event.ActionEvent evt) {
        this.setVisible(false);
        this.dispose();
    }

    private void cmdCopyActionPerformed(java.awt.event.ActionEvent evt) {
        AiChatMessageMarkdownPanel p = findLastMessagePanel();
        if (p != null) {
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            Clipboard clipboard = toolkit.getSystemClipboard();
            StringSelection strSel = new StringSelection(p.getMessage().getContent());
            clipboard.setContents(strSel, null);
        }
    }

    private void cmdProcessOutputActionPerformed(java.awt.event.ActionEvent evt) {
        if (this.inputAdapter instanceof AssistantFlowAdapter && this.result != null) {

            if (result.getResponse() != null && result.getResponse().getOutputData() != null) {
                OutputData intro = new OutputData();
                intro.setType(OutputData.TYPE_STRING);
                intro.setStringData("## Chat-Verlauf: " + System.lineSeparator() + System.lineSeparator());
                result.getResponse().getOutputData().add(intro);
                for (Message msg : this.messages) {
                    OutputData od = new OutputData();
                    od.setType(OutputData.TYPE_STRING);

                    String role = msg.getRole();
                    String displayRole;
                    String iconBase64 = null;

                    if ("user".equalsIgnoreCase(role)) {
                        displayRole = UserSettings.getInstance().getCurrentUser().getPrincipalId();
                        ImageIcon icon = UserSettings.getInstance().getUserSmallIcon(displayRole);
                        iconBase64 = encodeImageIconToBase64(icon);
                    } else {
                        displayRole = "Assistent Ingo";
                        ImageIcon icon = new ImageIcon(getClass().getResource("/icons16/material/j-lawyer-ai.png"));
                        iconBase64 = encodeImageIconToBase64(icon);
                    }

                    StringBuilder md = new StringBuilder();

                    if (iconBase64 != null) {
                        md.append("![icon](data:image/png;base64,")
                                .append(iconBase64)
                                .append(") ");
                    }
                    md.append(" **").append(displayRole).append("**\n\n");

                    String[] lines = msg.getContent().split("\\r?\\n");
                    for (String line : lines) {
                        md.append("> ").append(line).append("\n");
                    }
                    md.append("\n");

                    od.setStringData(md.toString());
                    result.getResponse().getOutputData().add(od);
                }
            }

            ((AssistantFlowAdapter) this.inputAdapter).processOutput(capability, this.result);
        }
        this.setVisible(false);
        this.dispose();
    }

    private static String encodeImageIconToBase64(ImageIcon icon) {
        if (icon == null) {
            return null;
        }
        try {
            java.awt.Image img = icon.getImage();
            BufferedImage bImg = new BufferedImage(
                    img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = bImg.createGraphics();
            g2d.drawImage(img, 0, 0, null);
            g2d.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bImg, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            log.error("Error rendering icon to base 64", e);
            return null;
        }
    }

    private void cmdInterruptActionPerformed(java.awt.event.ActionEvent evt) {
        this.interrupted = true;
    }

    private void cmdResetChatActionPerformed(java.awt.event.ActionEvent evt) {
        this.messages.clear();
        this.pnlMessages.removeAll();
        this.pnlMessages.revalidate();
        this.pnlMessages.repaint();
        this.taPrompt.setText(this.initialPrompt);
        this.isFirstMessage = true;
    }

    private void cmdNewDocumentActionPerformed(java.awt.event.ActionEvent evt) {
        if (this.caseView != null) {
            AiChatMessageMarkdownPanel p = findLastMessagePanel();
            if (p != null) {
                this.caseView.newDocumentDialog(null, null, null, null, null, null, null, p.getMessage().getContent());
            }
        }
    }

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

    private void cmdPromptMouseReleased(java.awt.event.MouseEvent evt) {
        try {
            AssistantAccess ingo = AssistantAccess.getInstance();
            this.popAssistant.removeAll();
            List<AssistantPrompt> customPrompts = ingo.getCustomPrompts(AiCapability.REQUESTTYPE_CHAT);
            for (AssistantPrompt p : customPrompts) {
                JMenuItem mi = new JMenuItem();
                mi.setText(p.getName());
                mi.addActionListener((ActionEvent e) -> {
                    if (!p.getPrompt().contains("{{")) {
                        this.taPrompt.setText(p.getPrompt());
                    } else {
                        HashMap<String, Object> placeHolders = TemplatesUtil.getPlaceHolderValues(p.getPrompt(), selectedCase, this.parties, null, null, this.allPartyTypes, this.formPlaceHolders, this.formPlaceHolderValues, this.caseLawyer, this.caseAssistant);
                        String promptWithValues = TemplatesUtil.replacePlaceHolders(p.getPrompt(), placeHolders);
                        this.taPrompt.setText(promptWithValues);
                    }
                });
                popAssistant.add(mi);
            }

            this.popAssistant.show(this.cmdPrompt, evt.getX(), evt.getY());
        } catch (Exception ex) {
            log.error("Error loading custom prompts", ex);
            ThreadUtils.showErrorDialog(this, "Fehler beim Laden der eigenen Prompts: " + ex.getMessage(), DesktopUtils.POPUP_TITLE_ERROR);
        }
    }

    /**
     * Creates and styles an AiChatMessageMarkdownPanel with gray-tone
     * rounded bubble appearance.
     */
    private AiChatMessageMarkdownPanel createStyledMessagePanel(Message msg, JDialog owner) {
        AiChatMessageMarkdownPanel panel = new AiChatMessageMarkdownPanel(msg, owner);
        styleMessageBubble(panel, Message.ROLE_USER.equals(msg.getRole()));
        return panel;
    }

    /**
     * Re-applies gray bubble styling after a setMessage call has reset colors.
     */
    private void restyleMessagePanel(AiChatMessageMarkdownPanel panel) {
        styleMessageBubble(panel, Message.ROLE_USER.equals(panel.getMessage().getRole()));
    }

    private void removePlaceholder() {
        if (pnlPlaceholder != null) {
            pnlMessages.removeAll();
            pnlPlaceholder = null;
        }
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = scrollMessages.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    private JPanel addToolStatusMessage(String text, String riskLevel, String tooltip) {
        Color bgColor;
        Color borderColor;
        Color indicatorColor;
        if (ToolDefinition.RISK_HIGH.equals(riskLevel)) {
            bgColor = new Color(255, 240, 240);
            borderColor = new Color(220, 150, 150);
            indicatorColor = new Color(244, 67, 54);
        } else if (ToolDefinition.RISK_MEDIUM.equals(riskLevel)) {
            bgColor = new Color(255, 248, 220);
            borderColor = new Color(220, 180, 100);
            indicatorColor = new Color(255, 152, 0);
        } else {
            bgColor = new Color(240, 255, 240);
            borderColor = new Color(150, 200, 150);
            indicatorColor = new Color(76, 175, 80);
        }

        JPanel toolPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        toolPanel.setOpaque(true);
        toolPanel.setBackground(bgColor);
        toolPanel.setBorder(new FlatLineBorder(new Insets(4, 8, 4, 8), borderColor, 1, 8));

        JLabel riskIndicator = new JLabel("\u25CF");
        riskIndicator.setFont(riskIndicator.getFont().deriveFont(10f));
        riskIndicator.setForeground(indicatorColor);
        toolPanel.add(riskIndicator);

        JLabel lbl = new JLabel(text);
        lbl.setFont(lbl.getFont().deriveFont(lbl.getFont().getSize2D() - 1f));
        lbl.setName("toolStatusLabel");
        toolPanel.add(lbl);

        if (tooltip != null && !tooltip.isEmpty()) {
            toolPanel.setToolTipText(tooltip);
        }

        toolPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        toolPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, toolPanel.getPreferredSize().height + 8));
        removePlaceholder();
        pnlMessages.add(toolPanel);
        pnlMessages.revalidate();
        pnlMessages.repaint();
        scrollToBottom();
        return toolPanel;
    }

    private void updateToolStatusMessage(JPanel toolPanel, String newText) {
        for (Component comp : toolPanel.getComponents()) {
            if (comp instanceof JLabel && "toolStatusLabel".equals(comp.getName())) {
                ((JLabel) comp).setText(newText);
                toolPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, toolPanel.getPreferredSize().height + 8));
                toolPanel.revalidate();
                toolPanel.repaint();
                break;
            }
        }
    }

    private String formatParamTooltip(String argumentsJson) {
        try {
            org.json.simple.JsonObject args = (org.json.simple.JsonObject) org.json.simple.Jsoner.deserialize(argumentsJson);
            StringBuilder sb = new StringBuilder("<html>");
            for (Map.Entry<String, Object> entry : args.entrySet()) {
                sb.append("<b>").append(entry.getKey()).append("</b>: ")
                  .append(entry.getValue()).append("<br>");
            }
            sb.append("</html>");
            return sb.toString();
        } catch (Exception ex) {
            return argumentsJson;
        }
    }

    /**
     * Applies rounded FlatLineBorder and gray-tone colors to a message bubble.
     */
    private void styleMessageBubble(AiChatMessageMarkdownPanel panel, boolean isUser) {
        Color bg = isUser ? USER_BUBBLE_BG : ASSISTANT_BUBBLE_BG;
        Color iconBg = isUser ? USER_ICON_BG : DefaultColorTheme.COLOR_DARK_GREY;

        panel.setOpaque(true);
        panel.setBackground(bg);
        panel.setBorder(new FlatLineBorder(new Insets(8, 8, 8, 8), BUBBLE_BORDER_COLOR, 1, 12));

        applyBubbleColors(panel, bg, iconBg);
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
     * Finds the last AiChatMessageMarkdownPanel in pnlMessages,
     * skipping rigid areas and wrapper panels.
     */
    private AiChatMessageMarkdownPanel findLastMessagePanel() {
        for (int i = pnlMessages.getComponentCount() - 1; i >= 0; i--) {
            Component comp = pnlMessages.getComponent(i);
            if (comp instanceof AiChatMessageMarkdownPanel) {
                return (AiChatMessageMarkdownPanel) comp;
            }
            // check inside wrapper panels (e.g. truncated user message wrappers)
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                for (int j = panel.getComponentCount() - 1; j >= 0; j--) {
                    if (panel.getComponent(j) instanceof AiChatMessageMarkdownPanel) {
                        return (AiChatMessageMarkdownPanel) panel.getComponent(j);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Wraps a long user message in a collapsible panel that shows a truncated
     * preview with a "mehr anzeigen" link. Returns null if the message is short
     * enough to display in full.
     */
    private JPanel wrapUserMessage(AiChatMessageMarkdownPanel fullPanel, String content, JDialog owner) {
        if (content.length() <= USER_MSG_TRUNCATE_LENGTH) {
            return null;
        }

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        // Create truncated preview panel
        String preview = content.substring(0, USER_MSG_TRUNCATE_LENGTH) + " ...";
        Message previewMsg = new Message();
        previewMsg.setRole(Message.ROLE_USER);
        previewMsg.setContent(preview);
        AiChatMessageMarkdownPanel previewPanel = createStyledMessagePanel(previewMsg, owner);
        Dimension maxSize = previewPanel.getPreferredSize();
        maxSize.setSize(pnlMessages.getWidth(), maxSize.getHeight());
        previewPanel.setPreferredSize(maxSize);

        JLabel lblMore = new JLabel("mehr anzeigen \u25bc");
        lblMore.setForeground(DefaultColorTheme.COLOR_DARK_GREY);
        lblMore.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        lblMore.setBorder(BorderFactory.createEmptyBorder(2, 42, 4, 0));

        wrapper.add(previewPanel, BorderLayout.CENTER);
        wrapper.add(lblMore, BorderLayout.SOUTH);

        lblMore.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                wrapper.removeAll();
                wrapper.add(fullPanel, BorderLayout.CENTER);
                Dimension size = fullPanel.getPreferredSize();
                size.setSize(pnlMessages.getWidth(), size.getHeight());
                fullPanel.setPreferredSize(size);
                wrapper.revalidate();
                wrapper.repaint();
                pnlMessages.revalidate();
            }
        });

        return wrapper;
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
                String value = ((JComboBox) component).getEditor().getItem().toString();
                d.setValue(value);
            }
            parameters.add(d);
        }
        return parameters;
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
                            ThreadUtils.showErrorDialog(AssistantChatPanel.this, "Transkription fehlgeschlagen: " + status.getStatusDetails(), DesktopUtils.POPUP_TITLE_ERROR);
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
