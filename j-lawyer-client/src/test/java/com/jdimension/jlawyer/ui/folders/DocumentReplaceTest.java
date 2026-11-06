package com.jdimension.jlawyer.ui.folders;

import com.jdimension.jlawyer.persistence.ArchiveFileDocumentsBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * A document added to the list that is already shown, e.g. because a reload brought it after it
 * was saved, replaces the shown instance instead of being shown twice.
 */
public class DocumentReplaceTest {

    private static ArchiveFileDocumentsBean doc(String id) {
        ArchiveFileDocumentsBean d = new ArchiveFileDocumentsBean();
        d.setId(id);
        return d;
    }

    @Test
    public void sameIdIsReplacedByTheGivenInstance() {
        ArchiveFileDocumentsBean reloaded = doc("d2");
        List<ArchiveFileDocumentsBean> documents = new ArrayList<>(Arrays.asList(doc("d1"), reloaded, doc("d3")));
        ArchiveFileDocumentsBean saved = doc("d2");

        assertTrue(CaseFolderPanel.replaceDocument(documents, saved));
        assertEquals(3, documents.size());
        assertSame(saved, documents.get(1));
    }

    @Test
    public void newDocumentIsNotReplaced() {
        List<ArchiveFileDocumentsBean> documents = new ArrayList<>(Arrays.asList(doc("d1")));
        assertFalse(CaseFolderPanel.replaceDocument(documents, doc("d2")));
        assertEquals(1, documents.size());
    }
}
