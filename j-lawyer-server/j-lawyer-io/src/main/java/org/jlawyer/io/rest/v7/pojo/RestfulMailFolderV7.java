/*
 * Copyright (C) j-lawyer.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package org.jlawyer.io.rest.v7.pojo;

import com.jdimension.jlawyer.services.MailFolderDTO;
import java.io.Serializable;

public class RestfulMailFolderV7 implements Serializable {

    protected String folderId;
    protected String parentFolderId;
    protected String displayName;
    protected String wellKnownName;
    protected int unreadCount;
    protected int totalCount;

    public static RestfulMailFolderV7 fromMailFolderDTO(MailFolderDTO dto) {
        RestfulMailFolderV7 r = new RestfulMailFolderV7();
        r.setFolderId(dto.getFolderId());
        r.setParentFolderId(dto.getParentFolderId());
        r.setDisplayName(dto.getDisplayName());
        r.setWellKnownName(dto.getWellKnownName());
        r.setUnreadCount(dto.getUnreadCount());
        r.setTotalCount(dto.getTotalCount());
        return r;
    }

    public String getFolderId() {
        return folderId;
    }

    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }

    public String getParentFolderId() {
        return parentFolderId;
    }

    public void setParentFolderId(String parentFolderId) {
        this.parentFolderId = parentFolderId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getWellKnownName() {
        return wellKnownName;
    }

    public void setWellKnownName(String wellKnownName) {
        this.wellKnownName = wellKnownName;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
}
