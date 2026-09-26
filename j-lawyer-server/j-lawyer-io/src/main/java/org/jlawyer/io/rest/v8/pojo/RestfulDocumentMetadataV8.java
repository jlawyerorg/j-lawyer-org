/*
 * Copyright (C) 2026 Jens Kutschke
 *
 * This file is part of j-lawyer.org.
 *
 * j-lawyer.org is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * j-lawyer.org is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with j-lawyer.org.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.jlawyer.io.rest.v8.pojo;

import com.jdimension.jlawyer.documents.DocumentKeywords;
import com.jdimension.jlawyer.services.DocumentMetadata;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * The complete metadata of one case document for {@code PUT /v8/cases/documents/{id}/metadata}.
 * All fields are replaced; a field left empty is cleared. {@code receivedDate} is epoch
 * milliseconds, {@code correspondentDirection} 0 (none), 1 (incoming) or 2 (outgoing).
 *
 * @author jens
 */
public class RestfulDocumentMetadataV8 {

    private String title;
    private List<String> keywords = new ArrayList<>();
    private Long receivedDate;
    private String correspondentId;
    private String correspondentName;
    private int correspondentDirection;
    private String parentId;

    public RestfulDocumentMetadataV8() {
    }

    public DocumentMetadata toDocumentMetadata() {
        DocumentMetadata md = new DocumentMetadata();
        md.setTitle(title);
        md.setKeywords(keywords == null ? null : DocumentKeywords.join(keywords));
        md.setReceivedDate(receivedDate == null ? null : new Date(receivedDate));
        md.setCorrespondentId(correspondentId);
        md.setCorrespondentName(correspondentName);
        md.setCorrespondentDirection(correspondentDirection);
        md.setParentId(parentId);
        return md;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }

    public Long getReceivedDate() { return receivedDate; }
    public void setReceivedDate(Long receivedDate) { this.receivedDate = receivedDate; }

    public String getCorrespondentId() { return correspondentId; }
    public void setCorrespondentId(String correspondentId) { this.correspondentId = correspondentId; }

    public String getCorrespondentName() { return correspondentName; }
    public void setCorrespondentName(String correspondentName) { this.correspondentName = correspondentName; }

    public int getCorrespondentDirection() { return correspondentDirection; }
    public void setCorrespondentDirection(int correspondentDirection) { this.correspondentDirection = correspondentDirection; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }
}
