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

import com.jdimension.jlawyer.persistence.DocumentFolderTemplate;

/**
 * A document-folder template (Aktenstruktur-Vorlage): a named, reusable folder tree that can be
 * applied to a case. Carries the template id/name and the root folder of its tree.
 */
public class RestfulFolderTemplateV7 {

    private String id = null;
    private String name = null;
    private RestfulFolderNodeV7 rootFolder = null;

    public RestfulFolderTemplateV7() {
    }

    /** Maps a persistent folder template (including its folder tree) to its REST representation. */
    public static RestfulFolderTemplateV7 fromEntity(DocumentFolderTemplate t) {
        RestfulFolderTemplateV7 r = new RestfulFolderTemplateV7();
        r.setId(t.getId());
        r.setName(t.getName());
        r.setRootFolder(RestfulFolderNodeV7.fromEntity(t.getRootFolder()));
        return r;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RestfulFolderNodeV7 getRootFolder() {
        return rootFolder;
    }

    public void setRootFolder(RestfulFolderNodeV7 rootFolder) {
        this.rootFolder = rootFolder;
    }

}
