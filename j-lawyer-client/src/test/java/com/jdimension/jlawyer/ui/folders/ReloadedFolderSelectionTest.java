package com.jdimension.jlawyer.ui.folders;

import com.jdimension.jlawyer.persistence.CaseFolderSettings;
import java.util.Arrays;
import java.util.HashMap;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * After the folders of a case have been loaded again, the folders already shown keep their
 * selection and new folders follow their saved settings.
 */
public class ReloadedFolderSelectionTest {

    private static CaseFolderSettings hidden(boolean hidden) {
        CaseFolderSettings s = new CaseFolderSettings();
        s.setHidden(hidden);
        return s;
    }

    @Test
    public void shownFoldersKeepTheirSelection() {
        // f1 was unselected in this client and f2 selected again, while the loaded settings still say otherwise
        HashMap<String, CaseFolderSettings> saved = new HashMap<>();
        saved.put("f1", hidden(false));
        saved.put("f2", hidden(true));
        assertEquals(Arrays.asList("f1"), CaseFolderPanel.unselectedFolderIdsAfterReload(Arrays.asList("root", "f1", "f2"), Arrays.asList("f1"), saved));
    }

    @Test
    public void newFoldersFollowTheSavedSettings() {
        HashMap<String, CaseFolderSettings> saved = new HashMap<>();
        saved.put("new-hidden", hidden(true));
        saved.put("new-shown", hidden(false));
        assertEquals(Arrays.asList("new-hidden"), CaseFolderPanel.unselectedFolderIdsAfterReload(Arrays.asList("root"), Arrays.asList(), saved));
    }

    @Test
    public void newFolderWithoutSettingsIsSelected() {
        assertEquals(Arrays.asList(), CaseFolderPanel.unselectedFolderIdsAfterReload(Arrays.asList("root"), Arrays.asList(), new HashMap<>()));
    }
}
