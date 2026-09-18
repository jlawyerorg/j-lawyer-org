/*
 *                     GNU AFFERO GENERAL PUBLIC LICENSE
 *                        Version 3, 19 November 2007
 *
 *  Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
 *  Everyone is permitted to copy and distribute verbatim copies
 *  of this license document, but changing it is not allowed.
 */
package com.jdimension.jlawyer.client.desktop;

import com.jdimension.jlawyer.client.editors.EditorsRegistry;
import com.jdimension.jlawyer.client.editors.files.FileDropSupport;
import com.jdimension.jlawyer.client.editors.files.UploadDocumentsAction;
import com.jdimension.jlawyer.client.processing.ProgressIndicator;
import com.jdimension.jlawyer.client.settings.ClientSettings;
import com.jdimension.jlawyer.client.settings.UserSettings;
import com.jdimension.jlawyer.client.utils.ThreadUtils;
import com.jdimension.jlawyer.persistence.ArchiveFileBean;
import com.jdimension.jlawyer.services.ArchiveFileServiceRemote;
import com.jdimension.jlawyer.services.JLawyerServiceLocator;
import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.File;
import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.apache.log4j.Logger;

/**
 * Accepts files dropped onto a desktop entry referring to a case and uploads
 * them into the root folder of that case. While files are dragged over the
 * entry, its drop indicator is shown.
 */
public class CaseEntryDropHandler implements DropTargetListener {

    private static final Logger log = Logger.getLogger(CaseEntryDropHandler.class.getName());
    private static final String BUNDLE = "com/jdimension/jlawyer/client/desktop/CaseEntryDropHandler";

    private final Component component;
    private final CaseDropTarget target;
    private boolean indicatorVisible = false;

    private <T extends Component & CaseDropTarget> CaseEntryDropHandler(T entry) {
        this.component = entry;
        this.target = entry;
    }

    /**
     * Makes the given entry accept file drops. Child components must not have
     * drop targets of their own, so that all drag events reach the entry.
     *
     * @param <T> entry type
     * @param entry the desktop entry
     */
    public static <T extends Component & CaseDropTarget> void install(T entry) {
        new DropTarget(entry, DnDConstants.ACTION_COPY, new CaseEntryDropHandler(entry), true);
    }

    private boolean canImport(DataFlavor[] flavors) {
        return target.getDropCaseId() != null
                && target.isDropAllowed()
                && UserSettings.getInstance().isCurrentUserInRole(UserSettings.ROLE_WRITECASE)
                && FileDropSupport.isFileTransfer(flavors);
    }

    private void processDrag(DropTargetDragEvent dtde) {
        if (canImport(dtde.getCurrentDataFlavors())) {
            dtde.acceptDrag(DnDConstants.ACTION_COPY);
            showIndicator(true);
        } else {
            dtde.rejectDrag();
            showIndicator(false);
        }
    }

    private void showIndicator(boolean visible) {
        if (visible != this.indicatorVisible) {
            this.indicatorVisible = visible;
            this.target.setDropIndicatorVisible(visible);
        }
    }

    @Override
    public void dragEnter(DropTargetDragEvent dtde) {
        processDrag(dtde);
    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {
        processDrag(dtde);
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {
        processDrag(dtde);
    }

    @Override
    public void dragExit(DropTargetEvent dte) {
        showIndicator(false);
    }

    @Override
    public void drop(DropTargetDropEvent dtde) {
        showIndicator(false);

        final String caseId = target.getDropCaseId();
        if (!canImport(dtde.getCurrentDataFlavors())) {
            dtde.rejectDrop();
            return;
        }

        dtde.acceptDrop(DnDConstants.ACTION_COPY);
        List<File> files = FileDropSupport.getDroppedFiles(dtde.getTransferable(), component);
        // complete the native drag operation before any dialog is shown
        dtde.dropComplete(!files.isEmpty());
        if (!files.isEmpty()) {
            SwingUtilities.invokeLater(() -> upload(caseId, files));
        }
    }

    private void upload(String caseId, List<File> files) {
        ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE);
        ArchiveFileBean aFile = null;
        try {
            JLawyerServiceLocator locator = JLawyerServiceLocator.getInstance(ClientSettings.getInstance().getLookupProperties());
            ArchiveFileServiceRemote fileService = locator.lookupArchiveFileServiceRemote();
            aFile = fileService.getArchiveFile(caseId);
        } catch (Exception ex) {
            log.error("Error loading archive file " + caseId + " for document upload", ex);
            JOptionPane.showMessageDialog(EditorsRegistry.getInstance().getMainWindow(), MessageFormat.format(bundle.getString("error.loadingcase"), ex.getMessage()), bundle.getString("dialog.error"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (aFile == null) {
            JOptionPane.showMessageDialog(EditorsRegistry.getInstance().getMainWindow(), bundle.getString("error.casenotfound"), bundle.getString("dialog.error"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (aFile.isArchived()) {
            JOptionPane.showMessageDialog(EditorsRegistry.getInstance().getMainWindow(), MessageFormat.format(bundle.getString("hint.archived"), aFile.getFileNumber()), bundle.getString("dialog.hint"), JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Component owner = EditorsRegistry.getInstance().getMainEditorsPane();
        ThreadUtils.setWaitCursor(owner);
        ProgressIndicator pi = new ProgressIndicator(EditorsRegistry.getInstance().getMainWindow(), true);
        pi.setShowCancelButton(true);
        UploadDocumentsAction a = new UploadDocumentsAction(pi, owner, aFile, null, files, null, null);
        final String caseCaption = aFile.getFileNumber() + " " + (aFile.getName() == null ? "" : aFile.getName());
        a.setCallback(() -> {
            if (a.getUploadedCount() > 0) {
                JOptionPane.showMessageDialog(EditorsRegistry.getInstance().getMainWindow(), MessageFormat.format(bundle.getString("msg.uploaded"), a.getUploadedCount(), caseCaption.trim()), bundle.getString("dialog.uploaded"), JOptionPane.INFORMATION_MESSAGE);
            }
        });
        a.start();
    }
}
