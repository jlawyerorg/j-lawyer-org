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
import com.jdimension.jlawyer.services.AddressServiceRemote;
import com.jdimension.jlawyer.services.ArchiveFileServiceRemote;
import com.jdimension.jlawyer.services.CalendarServiceRemote;
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
 * Modal dialog for global search across cases, addresses and calendar entries.
 */
public class GlobalSearchDialog extends JDialog {

    private static final Logger log = Logger.getLogger(GlobalSearchDialog.class.getName());
    private static final Color ARCHIVED_COLOR = DefaultColorTheme.COLOR_DARK_GREY;

    // UserSettings keys for filter button states
    private static final String SETTING_FILTER_CASES = "globalsearch.filter.cases";
    private static final String SETTING_FILTER_CALENDAR = "globalsearch.filter.calendar";
    private static final String SETTING_FILTER_ADDRESSES = "globalsearch.filter.addresses";
    private static final String SETTING_FILTER_ARCHIVED = "globalsearch.filter.archived";

    private final JFrame parentFrame;
    private GlassPane glass;

    private JTextField txtSearch;
    private JToggleButton btnCases;
    private JToggleButton btnCalendar;
    private JToggleButton btnAddresses;
    private JToggleButton btnArchivedCases;
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

        JPanel mainPanel = new JPanel(new BorderLayout(0, 8));
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                new EmptyBorder(12, 12, 12, 12)));
        mainPanel.setBackground(Color.WHITE);

        // Search field - use default font like txtSearchDue in DesktopPanel
        txtSearch = new JTextField();
        txtSearch.putClientProperty("JTextField.placeholderText", "Suchen...");
        txtSearch.putClientProperty("JTextField.showClearButton", true);
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

        btnCases.addActionListener(e -> { saveFilterState(SETTING_FILTER_CASES, btnCases.isSelected()); applyFilter(); });
        btnCalendar.addActionListener(e -> { saveFilterState(SETTING_FILTER_CALENDAR, btnCalendar.isSelected()); applyFilter(); });
        btnAddresses.addActionListener(e -> { saveFilterState(SETTING_FILTER_ADDRESSES, btnAddresses.isSelected()); applyFilter(); });
        btnArchivedCases.addActionListener(e -> { saveFilterState(SETTING_FILTER_ARCHIVED, btnArchivedCases.isSelected()); applyFilter(); });

        filterPanel.add(btnCases);
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

        JScrollPane scrollPane = new JScrollPane(resultList);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));

        // Footer panel with status and cancel button
        JPanel footerPanel = new JPanel(new BorderLayout(8, 0));
        footerPanel.setOpaque(false);

        // Status label
        lblStatus = new JLabel(" ");
        lblStatus.setForeground(Color.GRAY);

        // Cancel button
        cmdCancel = new JButton("Abbrechen");
        cmdCancel.setIcon(new ImageIcon(getClass().getResource("/icons/cancel.png")));
        cmdCancel.addActionListener(e -> closeDialog());

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
        JToggleButton btn = new JToggleButton(text);
        btn.putClientProperty("JButton.buttonType", "roundRect");
        btn.putClientProperty("FlatLaf.style",
                "borderWidth: 2; pressedBackground: rgb(102, 102, 102); " +
                "selectedBackground: rgb(151, 191, 13); selectedForeground: rgb(0, 0, 0); " +
                "foreground: rgb(102, 102, 102)");
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

                    activeCaseResults = activeCases;
                    archivedCaseResults = archivedCases;
                    calendarResults = calendar;
                    addressResults = addresses;

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
        listModel.clear();
        lblStatus.setText(" ");
    }

    private void applyFilter() {
        listModel.clear();

        // Add results in order: active cases, calendar, addresses, archived cases
        // Only add if the corresponding filter button is selected
        if (btnCases.isSelected()) {
            for (GlobalSearchResultItem item : activeCaseResults) {
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
        lblStatus.setText(String.format("%d Akten, %d Kalendereinträge, %d Adressen, %d archivierte Akten",
                activeCaseResults.size(), calendarResults.size(), addressResults.size(), archivedCaseResults.size()));
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

    private void navigateToCase(JLawyerServiceLocator locator, String caseId, Image bgi) throws Exception {
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
    private static class GlobalSearchResultRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {

            JPanel panel = new JPanel(new BorderLayout(8, 2));
            panel.setBorder(new EmptyBorder(6, 8, 6, 8));

            if (isSelected) {
                panel.setBackground(list.getSelectionBackground());
                panel.setForeground(list.getSelectionForeground());
            } else {
                panel.setBackground(list.getBackground());
                panel.setForeground(list.getForeground());
            }

            if (value instanceof GlobalSearchResultItem) {
                GlobalSearchResultItem item = (GlobalSearchResultItem) value;

                JLabel iconLabel = new JLabel(item.getIcon());
                iconLabel.setVerticalAlignment(SwingConstants.TOP);

                JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 2));
                textPanel.setOpaque(false);

                JLabel primaryLabel = new JLabel(item.getDisplayText());
                primaryLabel.setFont(primaryLabel.getFont().deriveFont(Font.BOLD));

                // Set color based on selection and archived status
                if (isSelected) {
                    primaryLabel.setForeground(list.getSelectionForeground());
                } else if (item.isArchived()) {
                    primaryLabel.setForeground(ARCHIVED_COLOR);
                }

                // Secondary label - normal style (not italic)
                JLabel secondaryLabel = new JLabel(item.getSecondaryText());
                secondaryLabel.setForeground(isSelected ? list.getSelectionForeground() : Color.GRAY);

                textPanel.add(primaryLabel);
                textPanel.add(secondaryLabel);

                panel.add(iconLabel, BorderLayout.WEST);
                panel.add(textPanel, BorderLayout.CENTER);
            }

            return panel;
        }
    }
}
