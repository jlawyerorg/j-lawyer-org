/*
 *                     GNU AFFERO GENERAL PUBLIC LICENSE
 *                        Version 3, 19 November 2007
 *
 *  Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
 *  Everyone is permitted to copy and distribute verbatim copies
 *  of this license document, but changing it is not allowed.
 */
package com.jdimension.jlawyer.events;

import java.io.Serializable;

/**
 * Request object for asynchronous PDF preview generation.
 * TEXT previews remain synchronous (for search index), but PDF previews
 * are generated asynchronously to avoid blocking the client when
 * Stirling-PDF is slow or unavailable.
 *
 * @author jens
 */
public class PdfPreviewGenerationRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String archiveFileId;
    private String documentId;
    private String fileName;
    private boolean update;

    public PdfPreviewGenerationRequest() {
    }

    public PdfPreviewGenerationRequest(String archiveFileId, String documentId, String fileName, boolean update) {
        this.archiveFileId = archiveFileId;
        this.documentId = documentId;
        this.fileName = fileName;
        this.update = update;
    }

    /**
     * @return the archiveFileId
     */
    public String getArchiveFileId() {
        return archiveFileId;
    }

    /**
     * @param archiveFileId the archiveFileId to set
     */
    public void setArchiveFileId(String archiveFileId) {
        this.archiveFileId = archiveFileId;
    }

    /**
     * @return the documentId
     */
    public String getDocumentId() {
        return documentId;
    }

    /**
     * @param documentId the documentId to set
     */
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    /**
     * @return the fileName
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @param fileName the fileName to set
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * @return true if this is an update request, false for create
     */
    public boolean isUpdate() {
        return update;
    }

    /**
     * @param update true for updatePreview, false for createPreview
     */
    public void setUpdate(boolean update) {
        this.update = update;
    }
}
