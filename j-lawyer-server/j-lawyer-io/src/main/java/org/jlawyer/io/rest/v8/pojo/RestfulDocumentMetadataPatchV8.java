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
import com.jdimension.jlawyer.services.DocumentMetadataPatch;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A bulk metadata change for {@code PUT /v8/cases/documents/metadata}. Only activated fields are
 * changed on each document: {@code changeTitle}, {@code changeReceivedDate},
 * {@code changeCorrespondent} and a {@code keywordOperation} other than {@code UNCHANGED}
 * ({@code SET}, {@code ADD} or {@code REMOVE}).
 *
 * @author jens
 */
public class RestfulDocumentMetadataPatchV8 {

    private List<String> documentIds = new ArrayList<>();
    private boolean changeTitle;
    private String title;
    private String keywordOperation = "UNCHANGED";
    private List<String> keywords = new ArrayList<>();
    private boolean changeReceivedDate;
    private Long receivedDate;
    private boolean changeCorrespondent;
    private String correspondentId;
    private String correspondentName;
    private int correspondentDirection;

    public RestfulDocumentMetadataPatchV8() {
    }

    /**
     * @return the service patch
     * @throws IllegalArgumentException for an unknown keyword operation
     */
    public DocumentMetadataPatch toPatch() {
        DocumentMetadataPatch p = new DocumentMetadataPatch();
        if (changeTitle) {
            p.setTitle(title);
        }
        if (keywordOperation != null && !keywordOperation.isBlank()) {
            DocumentMetadataPatch.KeywordOperation op = DocumentMetadataPatch.KeywordOperation.valueOf(keywordOperation.trim().toUpperCase());
            if (op != DocumentMetadataPatch.KeywordOperation.UNCHANGED) {
                p.setKeywords(op, keywords == null ? null : DocumentKeywords.join(keywords));
            }
        }
        if (changeReceivedDate) {
            p.setReceivedDate(receivedDate == null ? null : new Date(receivedDate));
        }
        if (changeCorrespondent) {
            p.setCorrespondent(correspondentId, correspondentName, correspondentDirection);
        }
        return p;
    }

    public List<String> getDocumentIds() { return documentIds; }
    public void setDocumentIds(List<String> documentIds) { this.documentIds = documentIds; }

    public boolean isChangeTitle() { return changeTitle; }
    public void setChangeTitle(boolean changeTitle) { this.changeTitle = changeTitle; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getKeywordOperation() { return keywordOperation; }
    public void setKeywordOperation(String keywordOperation) { this.keywordOperation = keywordOperation; }

    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }

    public boolean isChangeReceivedDate() { return changeReceivedDate; }
    public void setChangeReceivedDate(boolean changeReceivedDate) { this.changeReceivedDate = changeReceivedDate; }

    public Long getReceivedDate() { return receivedDate; }
    public void setReceivedDate(Long receivedDate) { this.receivedDate = receivedDate; }

    public boolean isChangeCorrespondent() { return changeCorrespondent; }
    public void setChangeCorrespondent(boolean changeCorrespondent) { this.changeCorrespondent = changeCorrespondent; }

    public String getCorrespondentId() { return correspondentId; }
    public void setCorrespondentId(String correspondentId) { this.correspondentId = correspondentId; }

    public String getCorrespondentName() { return correspondentName; }
    public void setCorrespondentName(String correspondentName) { this.correspondentName = correspondentName; }

    public int getCorrespondentDirection() { return correspondentDirection; }
    public void setCorrespondentDirection(int correspondentDirection) { this.correspondentDirection = correspondentDirection; }
}
