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

import com.jdimension.jlawyer.persistence.ArchiveFileBean;
import java.util.Date;

/**
 * Richer case list row than {@code RestfulCaseOverviewV1}: adds the fields a case list UI
 * needs to render and filter without a per-row detail fetch — subject field, lawyer,
 * assistant, claim value and the archived flag (OpenSpec change {@code add-web-client},
 * task 2.4). Purely additive; the v1 overview is unchanged.
 *
 * @author jens
 */
public class RestfulCaseOverviewV8 {

    private String id;
    private String externalId;
    private String name;
    private String fileNumber;
    private String reason;
    private String subjectField;
    private String lawyer;
    private String assistant;
    private double claimValue;
    private boolean archived;
    private Date dateChanged;

    public RestfulCaseOverviewV8() {
    }

    /** Maps the persistent case bean to the list-overview DTO. */
    public static RestfulCaseOverviewV8 fromArchiveFile(ArchiveFileBean afb) {
        if (afb == null) {
            throw new IllegalArgumentException("Archive file is required");
        }
        RestfulCaseOverviewV8 o = new RestfulCaseOverviewV8();
        o.setId(afb.getId());
        o.setExternalId(afb.getExternalId());
        o.setName(afb.getName());
        o.setFileNumber(afb.getFileNumber());
        o.setReason(afb.getReason());
        o.setSubjectField(afb.getSubjectField());
        o.setLawyer(afb.getLawyer());
        o.setAssistant(afb.getAssistant());
        o.setClaimValue(afb.getClaimValue());
        o.setArchived(afb.isArchived());
        o.setDateChanged(afb.getDateChanged());
        return o;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFileNumber() {
        return fileNumber;
    }

    public void setFileNumber(String fileNumber) {
        this.fileNumber = fileNumber;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getSubjectField() {
        return subjectField;
    }

    public void setSubjectField(String subjectField) {
        this.subjectField = subjectField;
    }

    public String getLawyer() {
        return lawyer;
    }

    public void setLawyer(String lawyer) {
        this.lawyer = lawyer;
    }

    public String getAssistant() {
        return assistant;
    }

    public void setAssistant(String assistant) {
        this.assistant = assistant;
    }

    public double getClaimValue() {
        return claimValue;
    }

    public void setClaimValue(double claimValue) {
        this.claimValue = claimValue;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public Date getDateChanged() {
        return dateChanged;
    }

    public void setDateChanged(Date dateChanged) {
        this.dateChanged = dateChanged;
    }
}
