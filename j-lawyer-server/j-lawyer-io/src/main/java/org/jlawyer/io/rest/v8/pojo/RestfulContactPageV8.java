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

import java.util.List;

/**
 * One page of contact overviews plus the total number of matching contacts, so a client can
 * do server-side pagination / infinite scroll (OpenSpec change {@code add-web-client}).
 *
 * @author jens
 */
public class RestfulContactPageV8 {

    private long total;
    private int offset;
    private int limit;
    private List<RestfulContactOverviewV8> items;

    public RestfulContactPageV8() {
    }

    public RestfulContactPageV8(long total, int offset, int limit, List<RestfulContactOverviewV8> items) {
        this.total = total;
        this.offset = offset;
        this.limit = limit;
        this.items = items;
    }

    /** Total number of contacts matching the filter (across all pages). */
    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public List<RestfulContactOverviewV8> getItems() {
        return items;
    }

    public void setItems(List<RestfulContactOverviewV8> items) {
        this.items = items;
    }
}
