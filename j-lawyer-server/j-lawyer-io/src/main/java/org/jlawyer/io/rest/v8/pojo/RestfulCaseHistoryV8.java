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

import com.jdimension.jlawyer.persistence.ArchiveFileHistoryBean;

/**
 * A single case history (audit trail) entry for the v8 case-detail history tab — OpenSpec
 * change {@code add-web-client}. The change date is epoch milliseconds (UTC) to avoid
 * timezone/format ambiguity on the client.
 *
 * @author jens
 */
public class RestfulCaseHistoryV8 {

    private String id;
    /** Login name of the user who caused the change. */
    private String principal;
    /** When the change happened, as epoch milliseconds (UTC). */
    private long changeDate;
    private String changeDescription;

    public RestfulCaseHistoryV8() {
    }

    /** Maps a history bean to the REST DTO. */
    public static RestfulCaseHistoryV8 fromBean(ArchiveFileHistoryBean h) {
        RestfulCaseHistoryV8 e = new RestfulCaseHistoryV8();
        e.setId(h.getId());
        e.setPrincipal(h.getPrincipal());
        e.setChangeDate(h.getChangeDate() != null ? h.getChangeDate().getTime() : 0L);
        e.setChangeDescription(h.getChangeDescription());
        return e;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPrincipal() {
        return principal;
    }

    public void setPrincipal(String principal) {
        this.principal = principal;
    }

    public long getChangeDate() {
        return changeDate;
    }

    public void setChangeDate(long changeDate) {
        this.changeDate = changeDate;
    }

    public String getChangeDescription() {
        return changeDescription;
    }

    public void setChangeDescription(String changeDescription) {
        this.changeDescription = changeDescription;
    }
}
