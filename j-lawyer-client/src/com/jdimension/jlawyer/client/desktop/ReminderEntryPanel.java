/*
 *                     GNU AFFERO GENERAL PUBLIC LICENSE
 *                        Version 3, 19 November 2007
 *
 *  Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
 *  Everyone is permitted to copy and distribute verbatim copies
 *  of this license document, but changing it is not allowed.
 */
package com.jdimension.jlawyer.client.desktop;

import com.jdimension.jlawyer.client.configuration.PopulateOptionsEditor;
import com.jdimension.jlawyer.client.editors.EditorsRegistry;
import com.jdimension.jlawyer.client.editors.ThemeableEditor;
import com.jdimension.jlawyer.client.editors.files.ArchiveFilePanel;
import com.jdimension.jlawyer.client.editors.files.EditArchiveFileDetailsPanel;
import com.jdimension.jlawyer.client.editors.files.ViewArchiveFileDetailsPanel;
import com.jdimension.jlawyer.client.settings.ClientSettings;
import com.jdimension.jlawyer.client.settings.UserSettings;
import com.jdimension.jlawyer.persistence.ArchiveFileBean;
import com.jdimension.jlawyer.persistence.ArchiveFileReviewsBean;
import com.jdimension.jlawyer.services.ArchiveFileServiceRemote;
import com.jdimension.jlawyer.services.JLawyerServiceLocator;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.datatransfer.StringSelection;
import java.awt.Toolkit;
import java.text.SimpleDateFormat;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import org.apache.log4j.Logger;

/**
 * A panel representing a single reminder entry in the notification dialog.
 * Shows event summary, date/time, location, case name, and action buttons.
 */
public class ReminderEntryPanel extends JPanel {

    private static final Logger log = Logger.getLogger(ReminderEntryPanel.class.getName());
    private static final Color COLOR_LOGO_GREEN = new Color(151, 191, 13);

    private final ArchiveFileReviewsBean review;
    private final Runnable onDismiss;

    public ReminderEntryPanel(ArchiveFileReviewsBean review, Runnable onDismiss) {
        this.review = review;
        this.onDismiss = onDismiss;
        initComponents();
    }

    private Color getCalendarColor() {
        if (review.getCalendarSetup() != null) {
            return new Color(review.getCalendarSetup().getBackground());
        }
        return COLOR_LOGO_GREEN;
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setOpaque(true);

        // Left accent bar in the calendar's color
        final Color accentColor = getCalendarColor();
        JPanel accentBar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(accentColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
            }
        };
        accentBar.setPreferredSize(new Dimension(5, 0));
        accentBar.setOpaque(false);
        add(accentBar, BorderLayout.WEST);

        // Center: info area
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        infoPanel.setBorder(new EmptyBorder(8, 10, 8, 6));

        // Summary (bold, larger font)
        String summary = review.getSummary() != null ? review.getSummary() : "";
        JLabel lblSummary = new JLabel(summary);
        lblSummary.setFont(lblSummary.getFont().deriveFont(Font.BOLD, lblSummary.getFont().getSize() + 2f));
        lblSummary.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(lblSummary);
        infoPanel.add(Box.createVerticalStrut(3));

        // Date/time line
        SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        String dateStr = review.getBeginDate() != null ? df.format(review.getBeginDate()) : "";
        JLabel lblDate = new JLabel("Beginn: " + dateStr);
        lblDate.setIcon(loadIcon("/icons16/material/baseline_event_available_blue_36dp.png"));
        lblDate.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(lblDate);

        // Location line (only if location is non-empty)
        String location = review.getLocation();
        if (location != null && !location.isEmpty()) {
            infoPanel.add(Box.createVerticalStrut(2));
            JLabel lblLocation = new JLabel("Ort: " + location);
            lblLocation.setAlignmentX(Component.LEFT_ALIGNMENT);
            infoPanel.add(lblLocation);
        }

        // Case name line (clickable, navigates to case)
        ArchiveFileBean caseFile = review.getArchiveFileKey();
        if (caseFile != null) {
            infoPanel.add(Box.createVerticalStrut(2));
            String caseName = caseFile.getFileNumber() != null ? caseFile.getFileNumber() : "";
            if (caseFile.getName() != null && !caseFile.getName().isEmpty()) {
                caseName += " " + caseFile.getName();
            }
            final String caseText = caseName.trim();
            JLabel lblCase = new JLabel(caseText);
            lblCase.setIcon(loadIcon("/icons16/material/baseline_folder_blue_36dp.png"));
            lblCase.setAlignmentX(Component.LEFT_ALIGNMENT);
            lblCase.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            lblCase.setForeground(new Color(14, 114, 181));
            lblCase.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent evt) {
                    navigateToCase();
                }
                @Override
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    lblCase.setText("<html><u>" + caseText + "</u></html>");
                }
                @Override
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    lblCase.setText(caseText);
                }
            });
            infoPanel.add(lblCase);
        }

        // Description as tooltip on the whole panel
        String description = review.getDescription();
        if (description != null && !description.isEmpty()) {
            setToolTipText(description);
        }

        add(infoPanel, BorderLayout.CENTER);

        // Right side: action buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(new EmptyBorder(6, 2, 6, 6));

        // Dismiss button
        JButton btnDismiss = createActionButton(
                loadIcon("/icons/baseline_done_black_48dp.png"),
                "Bestätigen");
        btnDismiss.addActionListener(e -> {
            if (onDismiss != null) {
                onDismiss.run();
            }
        });
        buttonPanel.add(btnDismiss);
        buttonPanel.add(Box.createVerticalStrut(4));

        // Copy location button (only if location is non-empty)
        if (location != null && !location.isEmpty()) {
            JButton btnCopyLocation = createActionButton(
                    loadIcon("/icons16/material/baseline_content_copy_lightgrey_48dp.png"),
                    "Ort kopieren");
            btnCopyLocation.addActionListener(e -> {
                StringSelection selection = new StringSelection(location);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
            });
            buttonPanel.add(btnCopyLocation);
        }

        buttonPanel.add(Box.createVerticalGlue());
        add(buttonPanel, BorderLayout.EAST);

        // Set max height so entries don't stretch
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
    }

    private JButton createActionButton(ImageIcon icon, String tooltip) {
        JButton btn = new JButton(icon);
        btn.setToolTipText(tooltip);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        Dimension size = new Dimension(28, 28);
        btn.setPreferredSize(size);
        btn.setMaximumSize(size);
        btn.setMinimumSize(size);
        return btn;
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

    private void navigateToCase() {
        try {
            ArchiveFileBean caseRef = review.getArchiveFileKey();
            if (caseRef == null) {
                return;
            }

            Object editor;
            if (UserSettings.getInstance().isCurrentUserInRole(UserSettings.ROLE_WRITECASE)) {
                editor = EditorsRegistry.getInstance().getEditor(EditArchiveFileDetailsPanel.class.getName());
            } else {
                editor = EditorsRegistry.getInstance().getEditor(ViewArchiveFileDetailsPanel.class.getName());
            }

            Object desktop = EditorsRegistry.getInstance().getEditor(DesktopPanel.class.getName());
            Image bgi = ((DesktopPanel) desktop).getBackgroundImage();

            if (editor instanceof ThemeableEditor) {
                ((ThemeableEditor) editor).setBackgroundImage(bgi);
            }

            if (editor instanceof PopulateOptionsEditor) {
                ((PopulateOptionsEditor) editor).populateOptions();
            }

            JLawyerServiceLocator locator = JLawyerServiceLocator.getInstance(ClientSettings.getInstance().getLookupProperties());
            ArchiveFileServiceRemote fileService = locator.lookupArchiveFileServiceRemote();
            ArchiveFileBean aFile = fileService.getArchiveFile(caseRef.getId());

            if (aFile == null) {
                return;
            }

            ((ArchiveFilePanel) editor).setArchiveFileDTO(aFile);
            ((ArchiveFilePanel) editor).setOpenedFromEditorClass(DesktopPanel.class.getName());
            EditorsRegistry.getInstance().setMainEditorsPaneView((Component) editor);

            // Dismiss this entry after navigating
            if (onDismiss != null) {
                onDismiss.run();
            }
        } catch (Exception ex) {
            log.error("Error navigating to case", ex);
            JOptionPane.showMessageDialog(this, "Fehler beim Öffnen der Akte: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }
}
