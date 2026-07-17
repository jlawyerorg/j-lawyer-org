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

import java.util.ArrayList;
import java.util.List;

/**
 * The document recycle bin overview: the retention period (days after which entries are purged
 * automatically), the total size of all soft-deleted documents, and the documents themselves.
 */
public class RestfulDocumentBinV8 {

    private int retentionDays;
    private long totalBytes;
    private List<RestfulBinDocumentV8> documents = new ArrayList<>();

    public RestfulDocumentBinV8() {
    }

    public int getRetentionDays() { return retentionDays; }
    public void setRetentionDays(int retentionDays) { this.retentionDays = retentionDays; }
    public long getTotalBytes() { return totalBytes; }
    public void setTotalBytes(long totalBytes) { this.totalBytes = totalBytes; }
    public List<RestfulBinDocumentV8> getDocuments() { return documents; }
    public void setDocuments(List<RestfulBinDocumentV8> documents) { this.documents = documents; }

}
