/*
 * Copyright (C) j-lawyer.org
 *
 * Licensed under the GNU Affero General Public License, version 3.
 * See the LICENSE file distributed with this project.
 */
package org.jlawyer.io.rest.v7.pojo;

import java.io.Serializable;

/**
 * A single suggested case for an opened email. {@code source} indicates why it was suggested:
 * {@code subjectBody} (own file number found in subject/body), {@code reference} (foreign file
 * number found in subject/body) or {@code sender} (linked to a contact matching the sender).
 */
public class RestfulSuggestedCaseV7 implements Serializable {

    private String id;
    private String fileNumber;
    private String name;
    private String reason;
    private boolean archived;
    private String source;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFileNumber() {
        return fileNumber;
    }

    public void setFileNumber(String fileNumber) {
        this.fileNumber = fileNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
