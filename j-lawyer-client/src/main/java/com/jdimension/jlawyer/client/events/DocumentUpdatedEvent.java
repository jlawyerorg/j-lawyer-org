package com.jdimension.jlawyer.client.events;

import com.jdimension.jlawyer.persistence.ArchiveFileDocumentsBean;

/**
 *
 * @author jens
 */
public class DocumentUpdatedEvent extends Event {

    private ArchiveFileDocumentsBean document;

    public DocumentUpdatedEvent(ArchiveFileDocumentsBean doc) {
        super(Event.TYPE_DOCUMENTUPDATED);
        this.document=doc;

    }

    @Override
    public boolean isUiUpdateTrigger() {
        return true;
    }

    /**
     * @return the document
     */
    public ArchiveFileDocumentsBean getDocument() {
        return document;
    }

    /**
     * @param document the document to set
     */
    public void setDocument(ArchiveFileDocumentsBean document) {
        this.document = document;
    }

}
