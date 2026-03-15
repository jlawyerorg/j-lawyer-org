package com.jdimension.jlawyer.client.editors.files;

import com.jdimension.jlawyer.client.settings.ClientSettings;
import com.jdimension.jlawyer.persistence.ArchiveFileBean;
import com.jdimension.jlawyer.services.ArchiveFileServiceRemote;
import com.jdimension.jlawyer.services.JLawyerServiceLocator;
import java.util.List;
import java.util.Map;

public class DropscanEntryProcessor extends BulkSaveEntryProcessor {

    private final Map<String, byte[]> extractedFiles;

    public DropscanEntryProcessor(Map<String, byte[]> extractedFiles) {
        this.extractedFiles = extractedFiles;
    }

    @Override
    public void preSave(BulkSaveEntry forEntry, List<BulkSaveEntry> allEntries) throws Exception {
        // intentionally left blank
    }

    @Override
    public boolean isPreSaveProcessor() {
        return false;
    }

    @Override
    public String save(BulkSaveEntry entry, ArchiveFileBean targetCase) throws Exception {
        // Not used - BulkSaveDialog handles saving via its own logic when isSaveProcessor() returns false
        return null;
    }

    @Override
    public boolean isSaveProcessor() {
        return false;
    }

    @Override
    public void postSave(BulkSaveEntry entry) throws Exception {
        // intentionally left blank
    }

    @Override
    public boolean isPostSaveProcessor() {
        return false;
    }

    @Override
    public byte[] getBytes(BulkSaveEntry entry) throws Exception {
        byte[] data = extractedFiles.get(entry.getDocumentFilename());
        if (data == null) {
            throw new Exception("No data found for file: " + entry.getDocumentFilename());
        }
        return data;
    }

    @Override
    public boolean isBytesProvider() {
        return true;
    }
}
