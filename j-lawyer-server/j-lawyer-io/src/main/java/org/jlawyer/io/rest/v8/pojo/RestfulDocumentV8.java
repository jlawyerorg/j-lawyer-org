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
import com.jdimension.jlawyer.persistence.ArchiveFileDocumentsBean;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A case document with all of its metadata as returned by the v8 document endpoints. Dates are
 * epoch milliseconds (null if not set). {@code correspondentDirection} is 0 (none), 1 (incoming,
 * "Von") or 2 (outgoing, "An").
 *
 * @author jens
 */
public class RestfulDocumentV8 {

    private String id;
    private String caseId;
    private String externalId;
    private String name;
    private String title;
    private List<String> keywords = new ArrayList<>();
    private Long creationDate;
    private Long changeDate;
    private Long receivedDate;
    private String correspondentId;
    private String correspondentName;
    private int correspondentDirection;
    private String parentId;
    private int messageCount;
    private long size;
    private long version;
    private boolean favorite;
    private String folderId;
    private int highlight1;
    private int highlight2;
    private String dictateSign;
    private int documentType;
    private boolean locked;
    private String lockedBy;
    private List<RestfulDocumentTagV8> tags = new ArrayList<>();

    public RestfulDocumentV8() {
    }

    public static RestfulDocumentV8 fromDocumentsBean(ArchiveFileDocumentsBean d) {
        RestfulDocumentV8 r = new RestfulDocumentV8();
        r.setId(d.getId());
        r.setCaseId(d.getArchiveFileKey() == null ? null : d.getArchiveFileKey().getId());
        r.setExternalId(d.getExternalId());
        r.setName(d.getName());
        r.setTitle(d.getTitle());
        r.setKeywords(DocumentKeywords.split(d.getKeywords()));
        r.setCreationDate(toMillis(d.getCreationDate()));
        r.setChangeDate(toMillis(d.getChangeDate()));
        r.setReceivedDate(toMillis(d.getReceivedDate()));
        r.setCorrespondentId(d.getCorrespondentId());
        r.setCorrespondentName(d.getCorrespondentName());
        r.setCorrespondentDirection(d.getCorrespondentDirection());
        r.setParentId(d.getParentId());
        r.setSize(d.getSize());
        r.setVersion(d.getVersion());
        r.setFavorite(d.isFavorite());
        r.setFolderId(d.getFolder() == null ? null : d.getFolder().getId());
        r.setHighlight1(d.getHighlight1());
        r.setHighlight2(d.getHighlight2());
        r.setDictateSign(d.getDictateSign());
        r.setDocumentType(d.getDocumentType());
        r.setLocked(d.isLocked());
        r.setLockedBy(d.getLockedBy());
        return r;
    }

    static Long toMillis(Date d) {
        return d == null ? null : d.getTime();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }

    public Long getCreationDate() { return creationDate; }
    public void setCreationDate(Long creationDate) { this.creationDate = creationDate; }

    public Long getChangeDate() { return changeDate; }
    public void setChangeDate(Long changeDate) { this.changeDate = changeDate; }

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

    public int getMessageCount() { return messageCount; }
    public void setMessageCount(int messageCount) { this.messageCount = messageCount; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }

    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }

    public String getFolderId() { return folderId; }
    public void setFolderId(String folderId) { this.folderId = folderId; }

    public int getHighlight1() { return highlight1; }
    public void setHighlight1(int highlight1) { this.highlight1 = highlight1; }

    public int getHighlight2() { return highlight2; }
    public void setHighlight2(int highlight2) { this.highlight2 = highlight2; }

    public String getDictateSign() { return dictateSign; }
    public void setDictateSign(String dictateSign) { this.dictateSign = dictateSign; }

    public int getDocumentType() { return documentType; }
    public void setDocumentType(int documentType) { this.documentType = documentType; }

    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    public String getLockedBy() { return lockedBy; }
    public void setLockedBy(String lockedBy) { this.lockedBy = lockedBy; }

    public List<RestfulDocumentTagV8> getTags() { return tags; }
    public void setTags(List<RestfulDocumentTagV8> tags) { this.tags = tags; }
}
