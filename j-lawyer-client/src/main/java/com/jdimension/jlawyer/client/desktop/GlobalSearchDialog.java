package com.jdimension.jlawyer.client.desktop;

import com.jdimension.jlawyer.client.configuration.PopulateOptionsEditor;
import com.jdimension.jlawyer.client.editors.EditorsRegistry;
import com.jdimension.jlawyer.client.editors.SaveableEditor;
import com.jdimension.jlawyer.client.editors.ThemeableEditor;
import com.jdimension.jlawyer.client.editors.addresses.AddressPanel;
import com.jdimension.jlawyer.client.editors.addresses.EditAddressDetailsPanel;
import com.jdimension.jlawyer.client.editors.addresses.ViewAddressDetailsPanel;
import com.jdimension.jlawyer.client.editors.files.ArchiveFilePanel;
import com.jdimension.jlawyer.client.editors.files.EditArchiveFileDetailsPanel;
import com.jdimension.jlawyer.client.editors.files.ViewArchiveFileDetailsPanel;
import com.jdimension.jlawyer.client.processing.GlassPane;
import com.jdimension.jlawyer.client.settings.ClientSettings;
import com.jdimension.jlawyer.client.settings.UserSettings;
import com.jdimension.jlawyer.persistence.AddressBean;
import com.jdimension.jlawyer.persistence.ArchiveFileBean;
import com.jdimension.jlawyer.persistence.ArchiveFileReviewsBean;
import com.jdimension.jlawyer.persistence.Invoice;
import com.jdimension.jlawyer.services.AddressServiceRemote;
import com.jdimension.jlawyer.services.ArchiveFileServiceRemote;
import com.jdimension.jlawyer.services.CalendarServiceRemote;
import com.jdimension.jlawyer.services.InvoiceServiceRemote;
import com.jdimension.jlawyer.services.JLawyerServiceLocator;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import org.apache.log4j.Logger;
import themes.colors.DefaultColorTheme;

/**
 * Modal dialog for global search across cases, addresses, calendar entries and invoices.
 */
public class GlobalSearchDialog extends JDialog {

    private static final Logger log = Logger.getLogger(GlobalSearchDialog.class.getName());
    private static final Color ARCHIVED_COLOR = DefaultColorTheme.COLOR_DARK_GREY;

    private static final Color TEXT_DARK = new Color(30, 41, 59);
    private static final Color MUTED_TEXT = new Color(100, 116, 139);
    private static final Color ROW_HOVER = new Color(241, 245, 249);
    private static final Color ROW_SELECTED = new Color(234, 243, 252);
    private static final Color ROW_SELECTED_BORDER = new Color(191, 219, 243);
    private static final String HOVER_INDEX = "jlawyer.globalSearch.hoverIndex";
    // corner radius of the card (and of the window shape)
    private static final int CARD_ARC = 20;

    // UserSettings keys for filter button states
    private static final String SETTING_FILTER_CASES = "globalsearch.filter.cases";
    private static final String SETTING_FILTER_CALENDAR = "globalsearch.filter.calendar";
    private static final String SETTING_FILTER_ADDRESSES = "globalsearch.filter.addresses";
    private static final String SETTING_FILTER_ARCHIVED = "globalsearch.filter.archived";
    private static final String SETTING_FILTER_INVOICES = "globalsearch.filter.invoices";

    private final JFrame parentFrame;
    private GlassPane glass;

    private JTextField txtSearch;
    private JToggleButton btnCases;
    private JToggleButton btnCalendar;
    private JToggleButton btnAddresses;
    private JToggleButton btnArchivedCases;
    private JToggleButton btnInvoices;
    private JList<GlobalSearchResultItem> resultList;
    private DefaultListModel<GlobalSearchResultItem> listModel;
    private JLabel lblStatus;
    private JProgressBar progressBar;
    private JButton cmdCancel;

    // Separate lists for each category
    private List<GlobalSearchResultItem> activeCaseResults = new ArrayList<>();
    private List<GlobalSearchResultItem> archivedCaseResults = new ArrayList<>();
    private List<GlobalSearchResultItem> calendarResults = new ArrayList<>();
    private List<GlobalSearchResultItem> addressResults = new ArrayList<>();
    private List<GlobalSearchResultItem> invoiceResults = new ArrayList<>();

    public GlobalSearchDialog(JFrame parent, boolean modal) {
        super(parent, modal);
        this.parentFrame = parent;
        initComponents();
        setupGlassPane();
        setupKeyBindings();
    }

    private void setupGlassPane() {
        this.glass = new GlassPane();
        this.glass.setLayout(null);
        this.glass.setOpaque(false);
        this.glass.setSize(parentFrame.getSize());
        this.parentFrame.setGlassPane(this.glass);
    }

    private void initComponents() {
        setUndecorated(true);
        setAlwaysOnTop(true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        // design kit: a light card with rounded corners. The window stays opaque - a per-pixel
        // translucent window (needed for a soft shadow) is not double buffered on every platform,
        // the search field then flickers while typing. The corners come from the window shape.
        boolean shaped = false;
        try {
            shaped = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                    .isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSPARENT);
        } catch (Throwable t) {
            log.debug("could not determine window shape support", t);
        }
        if (shaped) {
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    setShape(new java.awt.geom.RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), CARD_ARC, CARD_ARC));
                }
            });
        }
        CardPanel mainPanel = new CardPanel(shaped);
        mainPanel.setLayout(new BorderLayout(0, 10));

        // Search field
        txtSearch = new JTextField();
        txtSearch.putClientProperty("JTextField.placeholderText", "Akten, Adressen, Termine, Belege suchen ...");
        txtSearch.putClientProperty("JTextField.showClearButton", true);
        txtSearch.putClientProperty("JTextField.leadingIcon", new com.formdev.flatlaf.icons.FlatSearchIcon());
        txtSearch.setFont(txtSearch.getFont().deriveFont(txtSearch.getFont().getSize2D() + 4f));
        txtSearch.putClientProperty("FlatLaf.style", "arc: 14; margin: 8,10,8,10; borderWidth: 1; focusWidth: 0; innerFocusWidth: 0;"
                + " background: #f8fafc; borderColor: #e2e8f0; focusedBorderColor: #0e72b5");
        txtSearch.addActionListener(e -> performSearch());

        // Filter buttons panel - no ButtonGroup, all are toggle buttons
        // Restore saved filter states (default: all enabled)
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        filterPanel.setOpaque(false);

        UserSettings userSettings = UserSettings.getInstance();
        btnCases = createFilterButton("Akten", userSettings.getSettingAsBoolean(SETTING_FILTER_CASES, true));
        btnCalendar = createFilterButton("Kalender", userSettings.getSettingAsBoolean(SETTING_FILTER_CALENDAR, true));
        btnAddresses = createFilterButton("Adressen", userSettings.getSettingAsBoolean(SETTING_FILTER_ADDRESSES, true));
        btnArchivedCases = createFilterButton("Archiviert", userSettings.getSettingAsBoolean(SETTING_FILTER_ARCHIVED, true));
        btnInvoices = createFilterButton("Belege", userSettings.getSettingAsBoolean(SETTING_FILTER_INVOICES, false));

        btnCases.addActionListener(e -> { saveFilterState(SETTING_FILTER_CASES, btnCases.isSelected()); applyFilter(); });
        btnCalendar.addActionListener(e -> { saveFilterState(SETTING_FILTER_CALENDAR, btnCalendar.isSelected()); applyFilter(); });
        btnAddresses.addActionListener(e -> { saveFilterState(SETTING_FILTER_ADDRESSES, btnAddresses.isSelected()); applyFilter(); });
        btnArchivedCases.addActionListener(e -> { saveFilterState(SETTING_FILTER_ARCHIVED, btnArchivedCases.isSelected()); applyFilter(); });
        btnInvoices.addActionListener(e -> { saveFilterState(SETTING_FILTER_INVOICES, btnInvoices.isSelected()); applyFilter(); });

        filterPanel.add(btnCases);
        filterPanel.add(btnInvoices);
        filterPanel.add(btnCalendar);
        filterPanel.add(btnAddresses);
        filterPanel.add(btnArchivedCases);

        // Progress bar (hidden by default)
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(0, 3));

        // Results list
        listModel = new DefaultListModel<>();
        resultList = new JList<>(listModel);
        resultList.setCellRenderer(new GlobalSearchResultRenderer());
        resultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    navigateToSelected();
                }
            }
        });

        // hover highlight: the renderer reads the row under the mouse from this client property
        resultList.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int i = resultList.locationToIndex(e.getPoint());
                if (i >= 0 && !resultList.getCellBounds(i, i).contains(e.getPoint())) {
                    i = -1;
                }
                setHoverIndex(i);
            }
        });
        resultList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                setHoverIndex(-1);
            }
        });
        resultList.setBackground(Color.WHITE);
        resultList.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JScrollPane scrollPane = new JScrollPane(resultList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);

        // Footer panel with status and cancel button
        JPanel footerPanel = new JPanel(new BorderLayout(8, 0));
        footerPanel.setOpaque(false);

        // Status label
        lblStatus = new JLabel(" ");
        lblStatus.setForeground(MUTED_TEXT);

        // Cancel button
        cmdCancel = new JButton("Abbrechen");
        cmdCancel.setIcon(new ImageIcon(getClass().getResource("/icons/cancel.png")));
        cmdCancel.addActionListener(e -> closeDialog());
        cmdCancel.putClientProperty("FlatLaf.style", "arc: 10; margin: 4,12,4,12");

        footerPanel.add(lblStatus, BorderLayout.CENTER);
        footerPanel.add(cmdCancel, BorderLayout.EAST);

        // Layout
        JPanel topPanel = new JPanel(new BorderLayout(0, 8));
        topPanel.setOpaque(false);
        topPanel.add(txtSearch, BorderLayout.NORTH);
        topPanel.add(filterPanel, BorderLayout.CENTER);
        topPanel.add(progressBar, BorderLayout.SOUTH);

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);

        // Size: 70% of DesktopPanel size
        try {
            Object desktop = EditorsRegistry.getInstance().getEditor(DesktopPanel.class.getName());
            if (desktop instanceof Component) {
                Dimension desktopSize = ((Component) desktop).getSize();
                int width = (int) (desktopSize.width * 0.7);
                int height = (int) (desktopSize.height * 0.7);
                setPreferredSize(new Dimension(width, height));
            }
        } catch (Exception ex) {
            setPreferredSize(new Dimension(600, 450));
        }

        pack();
        setLocationRelativeTo(parentFrame);
    }

    private JToggleButton createFilterButton(String text, boolean selected) {
        // filter chips: white with a fine border, blue tinted while active
        JToggleButton btn = new JToggleButton(text);
        btn.putClientProperty("JButton.buttonType", "roundRect");
        btn.putClientProperty("FlatLaf.style",
                "arc: 999; borderWidth: 1; focusWidth: 0; innerFocusWidth: 0; margin: 3,12,3,12;"
                + " background: #ffffff; foreground: #475569; borderColor: #e2e8f0; hoverBackground: #f1f5f9;"
                + " selectedBackground: #eaf3fc; selectedForeground: #0e72b5; pressedBackground: #dbeaf9");
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setSelected(selected);
        btn.setFocusable(false);
        return btn;
    }

    private void saveFilterState(String key, boolean value) {
        UserSettings.getInstance().setSettingAsBoolean(key, value);
    }

    private void setupKeyBindings() {
        // ESC to close
        getRootPane().registerKeyboardAction(
                e -> closeDialog(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        // Enter in list to navigate
        resultList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    navigateToSelected();
                }
            }
        });

        // Arrow down from search field to list
        txtSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN && listModel.size() > 0) {
                    resultList.setSelectedIndex(0);
                    resultList.requestFocusInWindow();
                }
            }
        });

        // Arrow up from list index 0 to search field
        resultList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_UP && resultList.getSelectedIndex() == 0) {
                    txtSearch.requestFocusInWindow();
                }
            }
        });
    }

    private void performSearch() {
        String query = txtSearch.getText().trim();
        if (query.isEmpty()) {
            clearResults();
            return;
        }

        lblStatus.setText("Suche...");
        listModel.clear();
        progressBar.setVisible(true);

        // Capture filter states for use in background thread
        final boolean searchCases = btnCases.isSelected();
        final boolean searchArchived = btnArchivedCases.isSelected();
        final boolean searchAddresses = btnAddresses.isSelected();
        final boolean searchCalendar = btnCalendar.isSelected();
        final boolean searchInvoices = btnInvoices.isSelected();

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    JLawyerServiceLocator locator = JLawyerServiceLocator.getInstance(
                            ClientSettings.getInstance().getLookupProperties());

                    List<GlobalSearchResultItem> activeCases = new ArrayList<>();
                    List<GlobalSearchResultItem> archivedCases = new ArrayList<>();
                    List<GlobalSearchResultItem> addresses = new ArrayList<>();
                    List<GlobalSearchResultItem> calendar = new ArrayList<>();
                    List<GlobalSearchResultItem> invoices = new ArrayList<>();

                    String lowerQuery = query.toLowerCase();
                    List<CompletableFuture<Void>> futures = new ArrayList<>();

                    // Only search cases if either active or archived cases filter is enabled
                    if (searchCases || searchArchived) {
                        CompletableFuture<Void> caseFuture = CompletableFuture.runAsync(() -> {
                            try {
                                ArchiveFileServiceRemote afService = locator.lookupArchiveFileServiceRemote();
                                ArchiveFileBean[] result = afService.searchSimple(query);
                                if (result != null) {
                                    for (ArchiveFileBean af : result) {
                                        GlobalSearchResultItem item = new GlobalSearchResultItem(af);
                                        if (af.isArchived()) {
                                            if (searchArchived) {
                                                archivedCases.add(item);
                                            }
                                        } else {
                                            if (searchCases) {
                                                activeCases.add(item);
                                            }
                                        }
                                    }
                                }
                            } catch (Exception ex) {
                                log.error("Error searching cases", ex);
                            }
                        });
                        futures.add(caseFuture);
                    }

                    // Only search addresses if address filter is enabled
                    if (searchAddresses) {
                        CompletableFuture<Void> addressFuture = CompletableFuture.runAsync(() -> {
                            try {
                                AddressServiceRemote adService = locator.lookupAddressServiceRemote();
                                AddressBean[] result = adService.searchSimple(query);
                                if (result != null) {
                                    for (AddressBean ab : result) {
                                        addresses.add(new GlobalSearchResultItem(ab));
                                    }
                                }
                            } catch (Exception ex) {
                                log.error("Error searching addresses", ex);
                            }
                        });
                        futures.add(addressFuture);
                    }

                    // Only search calendar if calendar filter is enabled
                    if (searchCalendar) {
                        CompletableFuture<Void> calendarFuture = CompletableFuture.runAsync(() -> {
                            try {
                                CalendarServiceRemote calService = locator.lookupCalendarServiceRemote();
                                Collection<ArchiveFileReviewsBean> reviews = calService.getAllOpenReviews();
                                if (reviews != null) {
                                    for (ArchiveFileReviewsBean rev : reviews) {
                                        boolean match = false;
                                        if (rev.getSummary() != null && rev.getSummary().toLowerCase().contains(lowerQuery)) {
                                            match = true;
                                        }
                                        if (!match && rev.getDescription() != null && rev.getDescription().toLowerCase().contains(lowerQuery)) {
                                            match = true;
                                        }
                                        if (!match && rev.getArchiveFileKey() != null) {
                                            ArchiveFileBean af = rev.getArchiveFileKey();
                                            if (af.getName() != null && af.getName().toLowerCase().contains(lowerQuery)) {
                                                match = true;
                                            }
                                            if (!match && af.getFileNumber() != null && af.getFileNumber().toLowerCase().contains(lowerQuery)) {
                                                match = true;
                                            }
                                        }
                                        if (match) {
                                            calendar.add(new GlobalSearchResultItem(rev));
                                        }
                                    }
                                }
                            } catch (Exception ex) {
                                log.error("Error searching calendar", ex);
                            }
                        });
                        futures.add(calendarFuture);
                    }

                    // Only search invoices if invoice filter is enabled
                    if (searchInvoices) {
                        CompletableFuture<Void> invoiceFuture = CompletableFuture.runAsync(() -> {
                            try {
                                InvoiceServiceRemote invService = locator.lookupInvoiceServiceRemote();
                                List<Invoice> result = invService.searchInvoices(query);
                                if (result != null) {
                                    for (Invoice inv : result) {
                                        if (inv.getArchiveFileKey() != null) {
                                            invoices.add(new GlobalSearchResultItem(inv));
                                        }
                                    }
                                }
                            } catch (Exception ex) {
                                log.error("Error searching invoices", ex);
                            }
                        });
                        futures.add(invoiceFuture);
                    }

                    // Wait for all active searches to complete
                    if (!futures.isEmpty()) {
                        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                    }

                    // Sort each list
                    // Active cases: by dateChanged descending
                    activeCases.sort(Comparator.comparing(
                            GlobalSearchResultItem::getDateChanged,
                            Comparator.nullsLast(Comparator.reverseOrder())));

                    // Archived cases: by dateChanged descending
                    archivedCases.sort(Comparator.comparing(
                            GlobalSearchResultItem::getDateChanged,
                            Comparator.nullsLast(Comparator.reverseOrder())));

                    // Calendar: by beginDate descending
                    calendar.sort(Comparator.comparing(
                            GlobalSearchResultItem::getBeginDate,
                            Comparator.nullsLast(Comparator.reverseOrder())));

                    // Addresses: by displayName ascending
                    addresses.sort(Comparator.comparing(
                            GlobalSearchResultItem::getDisplayText,
                            String.CASE_INSENSITIVE_ORDER));

                    // Invoices: by creation date descending
                    invoices.sort(Comparator.comparing(
                            GlobalSearchResultItem::getCreationDate,
                            Comparator.nullsLast(Comparator.reverseOrder())));

                    activeCaseResults = activeCases;
                    archivedCaseResults = archivedCases;
                    calendarResults = calendar;
                    addressResults = addresses;
                    invoiceResults = invoices;

                } catch (Exception ex) {
                    log.error("Error performing global search", ex);
                }
                return null;
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                applyFilter();
                updateStatus();
            }
        };
        worker.execute();
    }

    private void clearResults() {
        activeCaseResults.clear();
        archivedCaseResults.clear();
        calendarResults.clear();
        addressResults.clear();
        invoiceResults.clear();
        listModel.clear();
        lblStatus.setText(" ");
    }

    private void applyFilter() {
        listModel.clear();

        // Add results in order: active cases, invoices, calendar, addresses, archived cases
        // Only add if the corresponding filter button is selected
        if (btnCases.isSelected()) {
            for (GlobalSearchResultItem item : activeCaseResults) {
                listModel.addElement(item);
            }
        }

        if (btnInvoices.isSelected()) {
            for (GlobalSearchResultItem item : invoiceResults) {
                listModel.addElement(item);
            }
        }

        if (btnCalendar.isSelected()) {
            for (GlobalSearchResultItem item : calendarResults) {
                listModel.addElement(item);
            }
        }

        if (btnAddresses.isSelected()) {
            for (GlobalSearchResultItem item : addressResults) {
                listModel.addElement(item);
            }
        }

        if (btnArchivedCases.isSelected()) {
            for (GlobalSearchResultItem item : archivedCaseResults) {
                listModel.addElement(item);
            }
        }
    }

    private void updateStatus() {
        lblStatus.setText(String.format("%d Akten, %d Belege, %d Kalendereinträge, %d Adressen, %d archivierte Akten",
                activeCaseResults.size(), invoiceResults.size(), calendarResults.size(), addressResults.size(), archivedCaseResults.size()));
    }

    /**
     * Checks if the current editor has unsaved changes and prompts the user to save.
     * @return true if navigation should proceed, false if user cancelled
     */
    private boolean checkUnsavedChanges() {
        EditorsRegistry registry = EditorsRegistry.getInstance();
        JPanel mainPane = registry.getMainEditorsPane();
        if (mainPane != null && mainPane.getComponentCount() == 1) {
            Component currentEditor = mainPane.getComponent(0);
            if (currentEditor instanceof SaveableEditor) {
                SaveableEditor se = (SaveableEditor) currentEditor;
                if (se.isDirty()) {
                    int ret = JOptionPane.showConfirmDialog(
                            this,
                            java.util.ResourceBundle.getBundle("com/jdimension/jlawyer/client/editors/EditorsRegistry").getString("dialog.savebeforeexit"),
                            java.util.ResourceBundle.getBundle("com/jdimension/jlawyer/client/editors/EditorsRegistry").getString("dialog.savebeforeexit.title"),
                            JOptionPane.YES_NO_CANCEL_OPTION);
                    if (ret == JOptionPane.YES_OPTION) {
                        return se.save(); // Proceed only if save was successful
                    } else if (ret == JOptionPane.NO_OPTION) {
                        return true; // Proceed without saving
                    } else {
                        return false; // Cancel - stay in dialog
                    }
                }
            }
        }
        return true; // No unsaved changes, proceed
    }

    private void navigateToSelected() {
        GlobalSearchResultItem selected = resultList.getSelectedValue();
        if (selected == null) {
            return;
        }

        // Check for unsaved changes before closing the dialog
        if (!checkUnsavedChanges()) {
            return; // User cancelled, stay in search dialog
        }

        closeDialog();

        try {
            JLawyerServiceLocator locator = JLawyerServiceLocator.getInstance(
                    ClientSettings.getInstance().getLookupProperties());

            Object desktop = EditorsRegistry.getInstance().getEditor(DesktopPanel.class.getName());
            Image bgi = ((DesktopPanel) desktop).getBackgroundImage();

            switch (selected.getType()) {
                case CASE:
                case CALENDAR:
                    navigateToCase(locator, selected.getId(), bgi);
                    break;
                case INVOICE:
                    Object caseEditor = navigateToCase(locator, selected.getId(), bgi);
                    if (caseEditor instanceof ArchiveFilePanel) {
                        ((ArchiveFilePanel) caseEditor).selectFinance();
                    }
                    break;
                case ADDRESS:
                    navigateToAddress(locator, selected.getId(), bgi);
                    break;
            }
        } catch (Exception ex) {
            log.error("Error navigating to search result", ex);
            JOptionPane.showMessageDialog(parentFrame,
                    "Fehler beim Navigieren: " + ex.getMessage(),
                    "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Opens the given case in the main editor pane.
     *
     * @return the case editor that was opened, so callers can select a specific tab afterwards
     */
    private Object navigateToCase(JLawyerServiceLocator locator, String caseId, Image bgi) throws Exception {
        Object editor;
        if (UserSettings.getInstance().isCurrentUserInRole(UserSettings.ROLE_WRITECASE)) {
            editor = EditorsRegistry.getInstance().getEditor(EditArchiveFileDetailsPanel.class.getName());
        } else {
            editor = EditorsRegistry.getInstance().getEditor(ViewArchiveFileDetailsPanel.class.getName());
        }

        if (editor instanceof ThemeableEditor) {
            ((ThemeableEditor) editor).setBackgroundImage(bgi);
        }

        if (editor instanceof PopulateOptionsEditor) {
            ((PopulateOptionsEditor) editor).populateOptions();
        }

        ArchiveFileServiceRemote afService = locator.lookupArchiveFileServiceRemote();
        ArchiveFileBean fullCase = afService.getArchiveFile(caseId);

        ((ArchiveFilePanel) editor).setArchiveFileDTO(fullCase);
        ((ArchiveFilePanel) editor).setOpenedFromEditorClass(DesktopPanel.class.getName());
        EditorsRegistry.getInstance().setMainEditorsPaneView((Component) editor);

        return editor;
    }

    private void navigateToAddress(JLawyerServiceLocator locator, String addressId, Image bgi) throws Exception {
        Object editor;
        if (UserSettings.getInstance().isCurrentUserInRole(UserSettings.ROLE_WRITEADDRESS)) {
            editor = EditorsRegistry.getInstance().getEditor(EditAddressDetailsPanel.class.getName());
        } else {
            editor = EditorsRegistry.getInstance().getEditor(ViewAddressDetailsPanel.class.getName());
        }

        if (editor instanceof ThemeableEditor) {
            ((ThemeableEditor) editor).setBackgroundImage(bgi);
        }

        if (editor instanceof PopulateOptionsEditor) {
            ((PopulateOptionsEditor) editor).populateOptions();
        }

        AddressServiceRemote adService = locator.lookupAddressServiceRemote();
        AddressBean fullAddress = adService.getAddress(addressId);

        ((AddressPanel) editor).setAddressDTO(fullAddress);
        ((AddressPanel) editor).setOpenedFromEditorClass(DesktopPanel.class.getName());
        EditorsRegistry.getInstance().setMainEditorsPaneView((Component) editor);
    }

    private void closeDialog() {
        glass.setVisible(false);
        dispose();
    }

    @Override
    public void setVisible(boolean b) {
        if (b) {
            glass.setVisible(true);
            txtSearch.requestFocusInWindow();
        } else {
            glass.setVisible(false);
        }
        super.setVisible(b);
    }

    /**
     * Custom cell renderer for search results.
     */
    private void setHoverIndex(int index) {
        Object old = resultList.getClientProperty(HOVER_INDEX);
        if (old == null || (Integer) old != index) {
            resultList.putClientProperty(HOVER_INDEX, index);
            resultList.repaint();
        }
    }

    /**
     * The white card of the dialog with a fine outline - rounded if the window has a rounded
     * shape.
     */
    private static class CardPanel extends JPanel {

        private final boolean rounded;

        CardPanel(boolean rounded) {
            this.rounded = rounded;
            setOpaque(true);
            setBackground(Color.WHITE);
            setBorder(new EmptyBorder(16, 16, 14, 16));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(203, 213, 225));
            if (this.rounded) {
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, CARD_ARC, CARD_ARC);
            } else {
                g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            }
            g2.dispose();
        }
    }

    /**
     * A result row: rounded highlight on hover and selection, icon, bold title and a muted second
     * line. The first row of each kind of result carries a small heading.
     */
    private static class GlobalSearchResultRenderer extends DefaultListCellRenderer {

        private static String groupOf(GlobalSearchResultItem item) {
            switch (item.getType()) {
                case CASE:
                    return item.isArchived() ? "ARCHIVIERTE AKTEN" : "AKTEN";
                case INVOICE:
                    return "BELEGE";
                case CALENDAR:
                    return "KALENDER";
                case ADDRESS:
                    return "ADRESSEN";
                default:
                    return "";
            }
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {

            Object hover = list.getClientProperty(HOVER_INDEX);
            final boolean hovered = hover instanceof Integer && (Integer) hover == index;
            final boolean selected = isSelected;

            JPanel row = new JPanel(new BorderLayout(10, 2)) {
                @Override
                protected void paintComponent(Graphics g) {
                    if (selected || hovered) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(selected ? ROW_SELECTED : ROW_HOVER);
                        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                        if (selected) {
                            g2.setColor(ROW_SELECTED_BORDER);
                            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                        }
                        g2.dispose();
                    }
                    super.paintComponent(g);
                }
            };
            row.setOpaque(false);
            row.setBorder(new EmptyBorder(7, 10, 7, 10));

            JPanel panel = new JPanel(new BorderLayout(0, 4));
            panel.setBackground(Color.WHITE);
            panel.setBorder(new EmptyBorder(1, 2, 1, 6));

            if (value instanceof GlobalSearchResultItem) {
                GlobalSearchResultItem item = (GlobalSearchResultItem) value;

                String group = groupOf(item);
                boolean first = index == 0;
                if (!first) {
                    Object prev = list.getModel().getElementAt(index - 1);
                    first = !(prev instanceof GlobalSearchResultItem) || !group.equals(groupOf((GlobalSearchResultItem) prev));
                }
                if (first) {
                    JLabel heading = new JLabel(group);
                    heading.setFont(heading.getFont().deriveFont(Font.BOLD, heading.getFont().getSize2D() - 2f));
                    heading.setForeground(MUTED_TEXT);
                    heading.setBorder(new EmptyBorder(index == 0 ? 2 : 10, 10, 2, 0));
                    panel.add(heading, BorderLayout.NORTH);
                }

                JLabel iconLabel = new JLabel(item.getIcon());
                iconLabel.setVerticalAlignment(SwingConstants.CENTER);

                JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 2));
                textPanel.setOpaque(false);

                JLabel primaryLabel = new JLabel(item.getDisplayText());
                primaryLabel.setFont(primaryLabel.getFont().deriveFont(Font.BOLD));
                primaryLabel.setForeground(item.isArchived() ? ARCHIVED_COLOR : TEXT_DARK);

                JLabel secondaryLabel = new JLabel(item.getSecondaryText());
                secondaryLabel.setForeground(MUTED_TEXT);

                textPanel.add(primaryLabel);
                textPanel.add(secondaryLabel);

                row.add(iconLabel, BorderLayout.WEST);
                row.add(textPanel, BorderLayout.CENTER);
            }
            panel.add(row, BorderLayout.CENTER);

            return panel;
        }
    }
}
