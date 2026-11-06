package com.jdimension.jlawyer.ui.folders;

import com.jdimension.jlawyer.persistence.CaseFolder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * A folder dialog keeps the folder it was opened for. If the folders have been loaded again
 * meanwhile, its change has to go to the folder with the same id among the folders shown now.
 */
public class FolderLookupAfterReloadTest {

    private static CaseFolder folder(String id, String name, CaseFolder parent) {
        CaseFolder f = new CaseFolder();
        f.setId(id);
        f.setName(name);
        if (parent != null) {
            f.setParentId(parent.getId());
            parent.getChildren().add(f);
        }
        return f;
    }

    @Test
    public void staleFolderResolvesToTheShownOne() {
        // the tree the dialog was opened in
        CaseFolder oldRoot = folder("root", "Dokumente", null);
        CaseFolder oldLetters = folder("letters", "Schriftverkehr", oldRoot);
        CaseFolder oldCourt = folder("court", "Gericht", oldLetters);

        // the same folders, loaded again
        CaseFolder shownRoot = folder("root", "Dokumente", null);
        CaseFolder shownLetters = folder("letters", "Schriftverkehr", shownRoot);
        CaseFolder shownCourt = folder("court", "Gericht", shownLetters);

        assertSame(shownRoot, FoldersListPanel.currentFolder(shownRoot, oldRoot));
        assertSame(shownLetters, FoldersListPanel.currentFolder(shownRoot, oldLetters));
        assertSame(shownCourt, FoldersListPanel.currentFolder(shownRoot, oldCourt));
    }

    @Test
    public void folderNotShownStaysAsGiven() {
        CaseFolder shownRoot = folder("root", "Dokumente", null);
        folder("letters", "Schriftverkehr", shownRoot);
        CaseFolder gone = folder("gone", "Gelöscht", null);
        assertSame(gone, FoldersListPanel.currentFolder(shownRoot, gone));
    }

    @Test
    public void newFolderIsAddedToTheGivenAndTheShownParent() {
        // an upload of a directory tree keeps the parent it started with and looks up the
        // folders it created there, while the folders shown have been loaded again
        CaseFolder oldRoot = folder("root", "Dokumente", null);
        CaseFolder shownRoot = folder("root", "Dokumente", null);
        CaseFolder docs = folder("docs", "docs", null);
        docs.setParentId("root");

        FoldersListPanel.addFolder(shownRoot, oldRoot, docs);
        assertTrue(oldRoot.hasChild("docs"));
        assertTrue(shownRoot.hasChild("docs"));

        // the same folder again, e.g. the reload already brought it
        FoldersListPanel.addFolder(shownRoot, oldRoot, docs);
        assertEquals(1, oldRoot.getChildren().size());
        assertEquals(1, shownRoot.getChildren().size());
    }

    @Test
    public void newFolderIsAddedOnceWithoutReload() {
        CaseFolder root = folder("root", "Dokumente", null);
        CaseFolder docs = folder("docs", "docs", null);
        docs.setParentId("root");
        FoldersListPanel.addFolder(root, root, docs);
        assertEquals(1, root.getChildren().size());
        assertSame(docs, root.getChildren().get(0));
    }

    @Test
    public void nothingShownYet() {
        CaseFolder f = folder("letters", "Schriftverkehr", null);
        assertSame(f, FoldersListPanel.currentFolder(null, f));
        assertNull(FoldersListPanel.currentFolder(f, null));
    }
}
