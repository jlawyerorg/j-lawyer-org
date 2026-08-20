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

import com.jdimension.jlawyer.persistence.DocumentFolder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A folder node within a document-folder template tree. Mirrors {@link DocumentFolder}: a node has an
 * id, the id of its parent (null for the root) and its child nodes, sorted by name for stable display.
 */
public class RestfulFolderNodeV7 {

    private String id = null;
    private String parentId = null;
    private String name = null;
    private List<RestfulFolderNodeV7> children = new ArrayList<>();

    public RestfulFolderNodeV7() {
    }

    /** Recursively maps a persistent folder (and its children) to its REST representation. */
    public static RestfulFolderNodeV7 fromEntity(DocumentFolder f) {
        RestfulFolderNodeV7 n = new RestfulFolderNodeV7();
        if (f == null) {
            return n;
        }
        n.setId(f.getId());
        n.setParentId(f.getParentId());
        n.setName(f.getName());
        List<RestfulFolderNodeV7> kids = new ArrayList<>();
        if (f.getChildren() != null) {
            for (DocumentFolder child : f.getChildren()) {
                kids.add(fromEntity(child));
            }
        }
        kids.sort(Comparator.comparing(c -> c.getName() == null ? "" : c.getName().toLowerCase()));
        n.setChildren(kids);
        return n;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public List<RestfulFolderNodeV7> getChildren() {
        return children;
    }

    public void setChildren(List<RestfulFolderNodeV7> children) {
        this.children = children;
    }

}
