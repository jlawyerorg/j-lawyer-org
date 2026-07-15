/*
 * Copyright (C) j-lawyer.org
 *
 * Licensed under the GNU Affero General Public License, version 3.
 * See the LICENSE file distributed with this project.
 */
package org.jlawyer.io.rest.v7.pojo;

/**
 * Unread/total message counts for a single mail folder, returned by the per-folder counts endpoint
 * used to lazily fill in counts after a fast (countless) folder listing.
 */
public class RestfulFolderCountsV7 {

    private String folderId;
    private int unreadCount;
    private int totalCount;

    public String getFolderId() {
        return folderId;
    }

    public void setFolderId(String folderId) {
        this.folderId = folderId;
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
