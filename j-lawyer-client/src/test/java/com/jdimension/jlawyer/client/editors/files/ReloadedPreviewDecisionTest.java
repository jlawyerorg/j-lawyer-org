package com.jdimension.jlawyer.client.editors.files;

import com.jdimension.jlawyer.persistence.ArchiveFileDocumentsBean;
import java.util.Date;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * After the documents of a case have been loaded again, the preview of the shown document is
 * outdated only if that document got new content.
 */
public class ReloadedPreviewDecisionTest {

    private static ArchiveFileDocumentsBean doc(String id, int version, long size, long changed) {
        ArchiveFileDocumentsBean d = new ArchiveFileDocumentsBean();
        d.setId(id);
        d.setVersion(version);
        d.setSize(size);
        d.setChangeDate(new Date(changed));
        return d;
    }

    @Test
    public void newVersionSizeOrChangeDate() {
        assertTrue(ArchiveFilePanel.hasNewContent(doc("a", 1, 100, 1000), doc("a", 2, 100, 1000)));
        assertTrue(ArchiveFilePanel.hasNewContent(doc("a", 1, 100, 1000), doc("a", 1, 120, 1000)));
        assertTrue(ArchiveFilePanel.hasNewContent(doc("a", 1, 100, 1000), doc("a", 1, 100, 2000)));
    }

    @Test
    public void unchangedDocument() {
        assertFalse(ArchiveFilePanel.hasNewContent(doc("a", 1, 100, 1000), doc("a", 1, 100, 1000)));
    }

    @Test
    public void documentGone() {
        assertFalse(ArchiveFilePanel.hasNewContent(doc("a", 1, 100, 1000), null));
    }
}
