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

/**
 * A soft-deleted document in the recycle bin (the desktop "Papierkorb"). {@code type} is
 * {@code "case"} or {@code "address"}; {@code ownerId} links to the owning case/contact so the
 * client can deep-link, {@code ownerLabel} is its display name and {@code ownerReference} the case
 * file number (empty for contacts). {@code deletionDate} is epoch milliseconds.
 */
public class RestfulBinDocumentV8 {

    private String type;
    private String id;
    private String name;
    private long deletionDate;
    private String deletedBy;
    private long size;
    private String ownerId;
    private String ownerLabel;
    private String ownerReference;

    public RestfulBinDocumentV8() {
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public long getDeletionDate() { return deletionDate; }
    public void setDeletionDate(long deletionDate) { this.deletionDate = deletionDate; }
    public String getDeletedBy() { return deletedBy; }
    public void setDeletedBy(String deletedBy) { this.deletedBy = deletedBy; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getOwnerLabel() { return ownerLabel; }
    public void setOwnerLabel(String ownerLabel) { this.ownerLabel = ownerLabel; }
    public String getOwnerReference() { return ownerReference; }
    public void setOwnerReference(String ownerReference) { this.ownerReference = ownerReference; }

}
