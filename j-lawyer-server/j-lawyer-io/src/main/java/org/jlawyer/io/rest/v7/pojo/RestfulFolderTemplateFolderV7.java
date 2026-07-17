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
package org.jlawyer.io.rest.v7.pojo;

/**
 * Request body for the folder-level operations on a document-folder template. Which fields are
 * required depends on the operation:
 * <ul>
 *   <li>add folder: {@code templateName} + {@code parentId} + {@code name}</li>
 *   <li>rename folder: {@code folderId} + {@code name}</li>
 *   <li>remove folder: {@code folderId}</li>
 *   <li>clone template: {@code templateName} (source) + {@code targetName}</li>
 * </ul>
 */
public class RestfulFolderTemplateFolderV7 {

    private String templateName = null;
    private String targetName = null;
    private String folderId = null;
    private String parentId = null;
    private String name = null;

    public RestfulFolderTemplateFolderV7() {
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public String getFolderId() {
        return folderId;
    }

    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
