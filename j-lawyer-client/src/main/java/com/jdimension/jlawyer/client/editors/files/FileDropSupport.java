/*
 *                     GNU AFFERO GENERAL PUBLIC LICENSE
 *                        Version 3, 19 November 2007
 *
 *  Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
 *  Everyone is permitted to copy and distribute verbatim copies
 *  of this license document, but changing it is not allowed.
 */
package com.jdimension.jlawyer.client.editors.files;

import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.apache.log4j.Logger;

/**
 * Shared logic for dropping files from the operating system (file manager,
 * classic Outlook) onto client components, so that every drop target accepts
 * the same flavors and extracts files the same way.
 */
public class FileDropSupport {

    private static final Logger log = Logger.getLogger(FileDropSupport.class.getName());

    private FileDropSupport() {
    }

    /**
     * Checks whether the offered flavors represent files that can be uploaded
     * as new documents. Internal document drags (moving existing documents)
     * are not considered file transfers.
     *
     * @param flavors the flavors offered by the drag source
     * @return true if the drop contains files or Outlook items
     */
    public static boolean isFileTransfer(DataFlavor[] flavors) {
        if (flavors == null) {
            return false;
        }
        for (DataFlavor f : flavors) {
            if (DocumentsTransferable.DOCS_FLAVOR.equals(f)) {
                return false;
            }
        }
        if (OutlookDropHelper.isOutlookDrop(flavors)) {
            return true;
        }
        for (DataFlavor f : flavors) {
            if (DataFlavor.javaFileListFlavor.equals(f)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extracts the dropped files. Outlook items are written to temporary
     * files. Must be called from within drop(), after the drop has been
     * accepted. Problems are logged; for sources that announce files without
     * delivering any data (New Outlook, OWA, RDP) a hint dialog is shown
     * asynchronously, so the native drag operation is not blocked.
     *
     * @param transferable the drop transferable
     * @param dialogParent parent component for the hint dialog
     * @return the dropped files, empty if nothing could be extracted
     */
    public static List<File> getDroppedFiles(Transferable transferable, Component dialogParent) {
        List<File> files = new ArrayList<>();

        if (OutlookDropHelper.isOutlookDrop(transferable.getTransferDataFlavors())) {
            try {
                files.addAll(OutlookDropHelper.extractOutlookFiles(transferable));
                if (files.isEmpty()) {
                    log.error("Outlook drop: no files could be extracted");
                }
            } catch (Exception ex) {
                log.error("Outlook drop error", ex);
            }
            return files;
        }

        if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            try {
                List transferData = (List) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                if (transferData == null || transferData.isEmpty()) {
                    log.error("transfer data is empty");
                    return files;
                }
                for (Object fo : transferData) {
                    if (fo instanceof File) {
                        files.add((File) fo);
                    } else {
                        log.error("transfer data: " + fo.getClass().getName());
                    }
                }
            } catch (Exception ex) {
                if (isVirtualFileDropFailure(ex, transferable)) {
                    log.warn("Drop aborted: source advertised javaFileListFlavor but did not deliver any native data (typical for New Outlook for Windows, Outlook Web App, RDP/Citrix sessions, or drags from email reading pane)");
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(dialogParent,
                            "Die Quellanwendung hat einen Drag & Drop signalisiert, aber keine verwertbaren Datei-Inhalte mitgesendet.\n\n"
                            + "Mögliche Ursachen:\n"
                            + "    • \"Neues Outlook für Windows\" (statt klassischem Outlook)\n"
                            + "    • Outlook im Webbrowser (Outlook Web App)\n"
                            + "    • Outlook über RDP / Citrix / Terminalserver\n"
                            + "    • Drag aus dem Lesebereich statt aus der Nachrichtenliste\n\n"
                            + "Workaround: Bitte ziehen Sie die E-Mail oder Datei zunächst auf den Desktop oder in einen Ordner\n"
                            + "und von dort nach j-lawyer.",
                            com.jdimension.jlawyer.client.utils.DesktopUtils.POPUP_TITLE_HINT,
                            JOptionPane.WARNING_MESSAGE));
                } else {
                    log.error("file drop error", ex);
                    logTransferFlavors("file drop error - offered flavors:", transferable);
                }
            }
            return files;
        }

        logTransferFlavors("drop not supported - offered flavors:", transferable);
        return files;
    }

    private static boolean isVirtualFileDropFailure(Exception ex, Transferable transferable) {
        if (!(ex instanceof IOException)) {
            return false;
        }
        String msg = ex.getMessage();
        if (msg == null || !msg.toLowerCase().contains("no native data")) {
            return false;
        }
        DataFlavor[] flavors = transferable.getTransferDataFlavors();
        if (flavors == null) {
            return false;
        }
        if (OutlookDropHelper.isOutlookDrop(flavors)) {
            return false;
        }
        for (DataFlavor f : flavors) {
            String name = f.getHumanPresentableName();
            if (name != null && name.toLowerCase().contains("file-list")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Logs all flavors offered by a transferable, to diagnose unsupported drag
     * sources.
     *
     * @param message log message prefix
     * @param transferable the transferable to inspect
     */
    public static void logTransferFlavors(String message, Transferable transferable) {
        try {
            DataFlavor[] flavors = transferable.getTransferDataFlavors();
            log.error(message + " count=" + (flavors == null ? 0 : flavors.length));
            if (flavors != null) {
                for (int i = 0; i < flavors.length; i++) {
                    DataFlavor df = flavors[i];
                    log.error("  [" + i + "] name='" + df.getHumanPresentableName()
                            + "' repr=" + df.getDefaultRepresentationClassAsString()
                            + " mime=" + df.getMimeType());
                }
            }
        } catch (Throwable th) {
            log.error("Error logging transferable flavors", th);
        }
    }
}
