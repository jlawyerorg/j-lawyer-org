/*
 *   GNU Affero General Public License v3
 */
package com.jdimension.jlawyer.ui.tagging;

import com.jdimension.jlawyer.client.editors.EditorsRegistry;
import com.jdimension.jlawyer.client.editors.files.ArchiveFilePanel;
import com.jdimension.jlawyer.persistence.DocumentTagsBean;
import com.jdimension.jlawyer.services.ArchiveFileServiceRemote;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.JOptionPane;
import org.apache.log4j.Logger;
import com.jdimension.jlawyer.client.utils.DesktopUtils;

/**
 * Applies a tag toggle to multiple documents at once.
 */
public class MultiDocumentTagActionListener implements ActionListener {

    private static final Logger log = Logger.getLogger(MultiDocumentTagActionListener.class.getName());

    private final ArchiveFileServiceRemote fileService;
    private final List<String> documentIds;
    private final ArchiveFilePanel caller;

    public MultiDocumentTagActionListener(List<String> documentIds, ArchiveFileServiceRemote fileService, ArchiveFilePanel caller) {
        this.fileService = fileService;
        this.documentIds = documentIds;
        this.caller = caller;
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        try {
            if (this.documentIds == null || this.documentIds.isEmpty()) {
                return;
            }

            TagToggleButton tb = (TagToggleButton) ae.getSource();
            boolean active = tb.isSelected();
            String tagName = tb.getText();

            DocumentTagsBean tagBean = new DocumentTagsBean();
            tagBean.setTagName(tagName);

            try {
                this.fileService.setDocumentTags(this.documentIds, tagBean, active);
                this.caller.updateDocumentTagsOverview();
            } catch (Throwable t) {
                log.error("Error setting tag '" + tagName + "' for multiple documents", t);
                JOptionPane.showMessageDialog(
                        EditorsRegistry.getInstance().getMainWindow(),
                        "Fehler beim Setzen des Dokumentetiketts: " + t.getMessage(),
                        DesktopUtils.POPUP_TITLE_ERROR,
                        JOptionPane.ERROR_MESSAGE
                );
            }

        } catch (Throwable t) {
            log.error("Error applying tag to multiple documents", t);
            JOptionPane.showMessageDialog(EditorsRegistry.getInstance().getMainWindow(),
                    "Fehler beim Setzen des Dokumentetiketts: " + t.getMessage(),
                    DesktopUtils.POPUP_TITLE_ERROR,
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}

