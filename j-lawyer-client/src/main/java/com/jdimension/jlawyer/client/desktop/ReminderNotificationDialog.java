/*
 *                     GNU AFFERO GENERAL PUBLIC LICENSE
 *                        Version 3, 19 November 2007
 *
 *  Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
 *  Everyone is permitted to copy and distribute verbatim copies
 *  of this license document, but changing it is not allowed.
 */
package com.jdimension.jlawyer.client.desktop;

import com.jdimension.jlawyer.persistence.ArchiveFileReviewsBean;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import org.apache.log4j.Logger;

/**
 * Singleton non-modal dialog that displays calendar event reminders.
 * Supports multiple reminders with individual dismiss/action buttons.
 */
public class ReminderNotificationDialog extends JDialog {

    private static final Logger log = Logger.getLogger(ReminderNotificationDialog.class.getName());

    private static ReminderNotificationDialog instance;

    private final JPanel entriesPanel;
    private final JLabel lblHeader;
    // Ordered map: review ID -> entry panel (preserves insertion order)
    private final Map<String, ReminderEntryPanel> entryPanels = new LinkedHashMap<>();

    private ReminderNotificationDialog() {
        super((java.awt.Frame) null, "Terminerinnerungen", false);
        setAlwaysOnTop(true);
        setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);

        try {
            setIconImage(new ImageIcon(getClass().getResource("/icons/windowicon.png")).getImage());
        } catch (Exception ex) {
            log.warn("Could not load window icon", ex);
        }

        setSize(550, 400);
        setMinimumSize(new Dimension(350, 200));
        getContentPane().setBackground(Color.WHITE);

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(new EmptyBorder(10, 12, 8, 12));

        ImageIcon bellIcon = loadIcon("/icons16/material/notifications_active.png");
        lblHeader = new JLabel("Terminerinnerungen", bellIcon, JLabel.LEFT);
        lblHeader.setFont(lblHeader.getFont().deriveFont(java.awt.Font.BOLD, lblHeader.getFont().getSize() + 4f));
        headerPanel.add(lblHeader, BorderLayout.CENTER);

        add(headerPanel, BorderLayout.NORTH);

        // Content: scrollable entries
        entriesPanel = new JPanel();
        entriesPanel.setLayout(new BoxLayout(entriesPanel, BoxLayout.Y_AXIS));
        entriesPanel.setBackground(Color.WHITE);
        entriesPanel.setBorder(new EmptyBorder(4, 8, 4, 8));

        JScrollPane scrollPane = new JScrollPane(entriesPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // Footer with "Alle bestätigen" button
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        footerPanel.setBackground(Color.WHITE);

        JButton btnDismissAll = new JButton("Alle bestätigen");
        btnDismissAll.setIcon(loadIcon("/icons16/material/baseline_event_available_green_36dp.png"));
        btnDismissAll.addActionListener(e -> dismissAll());
        footerPanel.add(btnDismissAll);

        add(footerPanel, BorderLayout.SOUTH);
    }

    /**
     * Returns the singleton instance of the reminder notification dialog.
     *
     * @return the singleton dialog instance
     */
    public static synchronized ReminderNotificationDialog getInstance() {
        if (instance == null) {
            instance = new ReminderNotificationDialog();
        }
        return instance;
    }

    /**
     * Adds a reminder to the dialog and makes it visible.
     *
     * @param review the calendar event to show as a reminder
     */
    public void addReminder(ArchiveFileReviewsBean review) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> addReminder(review));
            return;
        }

        String reviewId = review.getId();
        if (reviewId == null || entryPanels.containsKey(reviewId)) {
            return;
        }

        ReminderEntryPanel entry = new ReminderEntryPanel(review, () -> dismissEntry(reviewId));
        entryPanels.put(reviewId, entry);

        rebuildEntriesPanel();

        if (!isVisible()) {
            positionAtBottomRight();
            setVisible(true);
        }

        toFront();
    }

    private void dismissEntry(String reviewId) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> dismissEntry(reviewId));
            return;
        }

        entryPanels.remove(reviewId);
        rebuildEntriesPanel();

        if (entryPanels.isEmpty()) {
            setVisible(false);
        }
    }

    private void dismissAll() {
        entryPanels.clear();
        rebuildEntriesPanel();
        setVisible(false);
    }

    private void rebuildEntriesPanel() {
        entriesPanel.removeAll();
        boolean first = true;
        for (ReminderEntryPanel panel : entryPanels.values()) {
            if (!first) {
                entriesPanel.add(Box.createVerticalStrut(6));
            }
            panel.setBackground(Color.WHITE);
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
                    BorderFactory.createEmptyBorder(2, 0, 2, 0)));
            entriesPanel.add(panel);
            first = false;
        }
        updateHeader();
        entriesPanel.revalidate();
        entriesPanel.repaint();
    }

    private void updateHeader() {
        int count = entryPanels.size();
        if (count > 0) {
            lblHeader.setText("Terminerinnerungen (" + count + ")");
        } else {
            lblHeader.setText("Terminerinnerungen");
        }
    }

    private void positionAtBottomRight() {
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gd.getDefaultConfiguration();
            Rectangle screenBounds = gc.getBounds();
            Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(gc);

            int x = screenBounds.x + screenBounds.width - screenInsets.right - getWidth() - 12;
            int y = screenBounds.y + screenBounds.height - screenInsets.bottom - getHeight() - 12;
            setLocation(x, y);
        } catch (Exception ex) {
            log.warn("Could not position dialog at bottom-right", ex);
            setLocationRelativeTo(null);
        }
    }

    private ImageIcon loadIcon(String path) {
        try {
            java.net.URL url = getClass().getResource(path);
            if (url != null) {
                return new ImageIcon(url);
            }
        } catch (Exception ex) {
            log.warn("Could not load icon: " + path, ex);
        }
        return null;
    }
}
