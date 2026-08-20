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
 * Full-text (Lucene) search-index status: how many documents are indexed vs. how many documents
 * exist in total.
 */
public class RestfulSearchIndexV7 {

    private int indexedDocuments = 0;
    private int totalDocuments = 0;

    public RestfulSearchIndexV7() {
    }

    public int getIndexedDocuments() {
        return indexedDocuments;
    }

    public void setIndexedDocuments(int indexedDocuments) {
        this.indexedDocuments = indexedDocuments;
    }

    public int getTotalDocuments() {
        return totalDocuments;
    }

    public void setTotalDocuments(int totalDocuments) {
        this.totalDocuments = totalDocuments;
    }

}
