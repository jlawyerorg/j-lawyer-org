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
 * Request body for the multi-value tag ("Listenetiketten") operations. Which fields are required
 * depends on the operation:
 * <ul>
 *   <li>add value: {@code tagName} + {@code value}</li>
 *   <li>remove value: {@code tagName} + {@code value}</li>
 *   <li>rename value: {@code tagName} + {@code value} + {@code newValue}</li>
 *   <li>rename tag: {@code tagName} + {@code newTagName}</li>
 *   <li>remove tag: {@code tagName}</li>
 * </ul>
 * Operations cascade to tags already attached to cases/addresses/documents so definitions and
 * assignments stay consistent.
 */
public class RestfulMultiValueTagOpV7 {

    private String tagName;
    private String value;
    private String newValue;
    private String newTagName;

    public RestfulMultiValueTagOpV7() {
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public String getNewTagName() {
        return newTagName;
    }

    public void setNewTagName(String newTagName) {
        this.newTagName = newTagName;
    }

}
