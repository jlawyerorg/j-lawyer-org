/*
 *                     GNU AFFERO GENERAL PUBLIC LICENSE
 *                        Version 3, 19 November 2007
 *
 *  Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
 *  Everyone is permitted to copy and distribute verbatim copies
 *  of this license document, but changing it is not allowed.
 */
package com.jdimension.jlawyer.client.desktop;

import com.formdev.flatlaf.FlatClientProperties;
import com.jdimension.jlawyer.client.configuration.PopulateOptionsEditor;
import com.jdimension.jlawyer.client.editors.EditorsRegistry;
import com.jdimension.jlawyer.client.editors.ThemeableEditor;
import com.jdimension.jlawyer.client.editors.files.ArchiveFilePanel;
import com.jdimension.jlawyer.client.editors.files.EditArchiveFileDetailsPanel;
import com.jdimension.jlawyer.client.editors.files.ViewArchiveFileDetailsPanel;
import com.jdimension.jlawyer.client.settings.ClientSettings;
import com.jdimension.jlawyer.client.settings.UserSettings;
import com.jdimension.jlawyer.server.services.settings.UserSettingsKeys;
import com.jdimension.jlawyer.persistence.ArchiveFileBean;
import com.jdimension.jlawyer.services.ArchiveFileServiceRemote;
import com.jdimension.jlawyer.services.JLawyerServiceLocator;
import com.jdimension.jlawyer.ui.tagging.TagUtils;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Image;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import javax.swing.JOptionPane;
import org.apache.log4j.Logger;
import themes.colors.DefaultColorTheme;

/**
 * Header panel for grouping calendar entries by case and day.
 * Displays the date, case number, case name, case reason and tags for a group of related entries.
 *
 * @author jens
 */
public class CaseGroupHeaderPanel extends javax.swing.JPanel {

    private static final Logger log = Logger.getLogger(CaseGroupHeaderPanel.class.getName());

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy");

    // Same background color as entry panels
    private static final Color HEADER_COLOR = new Color(
            DefaultColorTheme.COLOR_DARK_GREY.getRed(),
            DefaultColorTheme.COLOR_DARK_GREY.getGreen(),
            DefaultColorTheme.COLOR_DARK_GREY.getBlue(),
            190).darker().darker();

    private String archiveFileId = null;

    /**
     * Creates new form CaseGroupHeaderPanel
     */
    public CaseGroupHeaderPanel() {
        initComponents();

        this.jPanel1.setBackground(HEADER_COLOR);
        this.jPanel1.putClientProperty(FlatClientProperties.COMPONENT_ROUND_RECT, true);

        this.setOpaque(false);
        this.lblCaseInfo.setOpaque(false);
        this.lblCaseInfo.setForeground(Color.WHITE);
        this.lblCaseInfo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        this.lblTags.setOpaque(false);
        this.lblTags.setForeground(DefaultColorTheme.COLOR_LIGHT_GREY);

        // Use smaller icon in compact view mode
        boolean compactView = UserSettings.getInstance().getSettingAsBoolean(
            UserSettingsKeys.CONF_DESKTOP_DUE_COMPACT_VIEW, true);
        if (compactView) {
            this.lblIcon.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons16/material/baseline_folder_blue_36dp.png")));
        }

        ClientSettings settings = ClientSettings.getInstance();
        String fontSizeOffset = settings.getConfiguration(ClientSettings.CONF_UI_FONTSIZEOFFSET, "0");
        try {
            int offset = Integer.parseInt(fontSizeOffset);
            Font currentFont = this.lblCaseInfo.getFont();
            this.lblCaseInfo.setFont(currentFont.deriveFont((float) currentFont.getSize() + (float) offset));
            Font tagsFont = this.lblTags.getFont();
            this.lblTags.setFont(tagsFont.deriveFont((float) tagsFont.getSize() + (float) offset));
        } catch (Throwable t) {
            log.error("Could not set font size", t);
        }
    }

    /**
     * Sets the group information to display.
     *
     * @param archiveFileId the case file ID for navigation
     * @param archiveFileNumber the case file number
     * @param archiveFileName the case name
     * @param archiveFileReason the case reason
     * @param dueDate the due date for this group
     * @param tags the tags associated with the case
     */
    public void setGroupInfo(String archiveFileId, String archiveFileNumber, String archiveFileName, String archiveFileReason, Date dueDate, ArrayList<String> tags) {
        this.archiveFileId = archiveFileId;

        String dateStr = "";
        if (dueDate != null) {
            synchronized (DATE_FORMAT) {
                dateStr = DATE_FORMAT.format(dueDate);
            }
        }

        String caseNumber = archiveFileNumber != null ? archiveFileNumber : "";
        String caseName = archiveFileName != null ? archiveFileName : "";
        String caseReason = archiveFileReason != null ? archiveFileReason : "";

        // Truncate long case names
        if (caseName.length() > 45) {
            caseName = caseName.substring(0, 45) + "...";
        }
        if (caseReason.length() > 53) {
            caseReason = caseReason.substring(0, 53) + "...";
        }

        boolean compactView = UserSettings.getInstance().getSettingAsBoolean(
            UserSettingsKeys.CONF_DESKTOP_DUE_COMPACT_VIEW, true);

        StringBuilder displayText = new StringBuilder();
        displayText.append("<html><b>").append(dateStr).append("</b> | ").append(caseNumber).append(" - ").append(caseName);
        if (!compactView && !caseReason.isEmpty()) {
            displayText.append("<br/>").append(caseReason);
        }
        displayText.append("</html>");

        this.lblCaseInfo.setText(displayText.toString());
        this.lblCaseInfo.setToolTipText(archiveFileNumber + " - " + archiveFileName + (archiveFileReason != null ? " - " + archiveFileReason : ""));

        // Set tags
        this.lblTags.setText("");
        this.lblTags.setIcon(null);
        if (tags != null && !tags.isEmpty()) {
            String tagList = TagUtils.getTagList(tags);
            String shortenedTagList = tagList;
            if (shortenedTagList.length() > 105) {
                shortenedTagList = shortenedTagList.substring(0, 105) + "...";
            }
            this.lblTags.setText(shortenedTagList);
            this.lblTags.setToolTipText(tagList);
            this.lblTags.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons16/material/baseline_label_white_36dp.png")));
        }
    }

    private void navigateToCase() {
        if (this.archiveFileId == null) {
            return;
        }

        try {
            Object editor = null;
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

            ArchiveFileBean aFile = null;
            try {
                JLawyerServiceLocator locator = JLawyerServiceLocator.getInstance(ClientSettings.getInstance().getLookupProperties());
                ArchiveFileServiceRemote fileService = locator.lookupArchiveFileServiceRemote();
                aFile = fileService.getArchiveFile(this.archiveFileId);
            } catch (Exception ex) {
                log.error("Error loading archive file from server", ex);
                JOptionPane.showMessageDialog(this,
                    java.text.MessageFormat.format(java.util.ResourceBundle.getBundle("com/jdimension/jlawyer/client/desktop/ReviewDueEntryPanel").getString("dialog.error.loadingcase"), new Object[]{ex.getMessage()}),
                    java.util.ResourceBundle.getBundle("com/jdimension/jlawyer/client/desktop/ReviewDueEntryPanel").getString("msg.error"),
                    JOptionPane.ERROR_MESSAGE);
            }

            if (aFile == null) {
                return;
            }

            ((ArchiveFilePanel) editor).setArchiveFileDTO(aFile);
            ((ArchiveFilePanel) editor).setOpenedFromEditorClass(DesktopPanel.class.getName());
            EditorsRegistry.getInstance().setMainEditorsPaneView((Component) editor);
        } catch (Exception ex) {
            log.error("Error creating editor from class " + this.getClass().getName(), ex);
            JOptionPane.showMessageDialog(this,
                java.text.MessageFormat.format(java.util.ResourceBundle.getBundle("com/jdimension/jlawyer/client/desktop/ReviewDueEntryPanel").getString("dialog.error.loadingeditor"), new Object[]{ex.getMessage()}),
                java.util.ResourceBundle.getBundle("com/jdimension/jlawyer/client/desktop/ReviewDueEntryPanel").getString("msg.error"),
                JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        lblIcon = new javax.swing.JLabel();
        lblCaseInfo = new javax.swing.JLabel();
        lblTags = new javax.swing.JLabel();

        setOpaque(false);

        lblIcon.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons32/material/sharp_folder_blue_36dp.png"))); // NOI18N

        lblCaseInfo.setFont(lblCaseInfo.getFont());
        lblCaseInfo.setForeground(new java.awt.Color(255, 255, 255));
        lblCaseInfo.setText("case info");
        lblCaseInfo.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        lblCaseInfo.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lblCaseInfoMouseClicked(evt);
            }
        });

        lblTags.setFont(lblTags.getFont().deriveFont(lblTags.getFont().getSize()-2f));
        lblTags.setText(" ");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblIcon)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblCaseInfo, javax.swing.GroupLayout.DEFAULT_SIZE, 338, Short.MAX_VALUE)
                    .addComponent(lblTags, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(2, 2, 2)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblIcon)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(lblCaseInfo)
                        .addGap(2, 2, 2)
                        .addComponent(lblTags)))
                .addGap(2, 2, 2))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void lblCaseInfoMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lblCaseInfoMouseClicked
        navigateToCase();
    }//GEN-LAST:event_lblCaseInfoMouseClicked


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jPanel1;
    private javax.swing.JLabel lblCaseInfo;
    private javax.swing.JLabel lblIcon;
    private javax.swing.JLabel lblTags;
    // End of variables declaration//GEN-END:variables
}
