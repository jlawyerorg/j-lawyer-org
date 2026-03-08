package org.jlawyer.io.rest.v8.pojo;

import com.jdimension.jlawyer.persistence.Timesheet;
import java.math.BigDecimal;

public class RestfulTimesheetV8 {

    private String id;
    private String name;
    private String description;
    private int interval;
    private boolean limited;
    private BigDecimal limit;
    private float percentageDone;
    private int status;
    private String caseId;
    private String caseFileNumber;
    private String caseName;

    public RestfulTimesheetV8() {
    }

    public static RestfulTimesheetV8 fromTimesheet(Timesheet ts) {
        RestfulTimesheetV8 r = new RestfulTimesheetV8();
        r.setId(ts.getId());
        r.setName(ts.getName());
        r.setDescription(ts.getDescription());
        r.setInterval(ts.getInterval());
        r.setLimited(ts.isLimited());
        r.setLimit(ts.getLimit());
        r.setPercentageDone(ts.getPercentageDone());
        r.setStatus(ts.getStatus());
        if (ts.getArchiveFileKey() != null) {
            r.setCaseId(ts.getArchiveFileKey().getId());
            r.setCaseFileNumber(ts.getArchiveFileKey().getFileNumber());
            r.setCaseName(ts.getArchiveFileKey().getName());
        }
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public boolean isLimited() {
        return limited;
    }

    public void setLimited(boolean limited) {
        this.limited = limited;
    }

    public BigDecimal getLimit() {
        return limit;
    }

    public void setLimit(BigDecimal limit) {
        this.limit = limit;
    }

    public float getPercentageDone() {
        return percentageDone;
    }

    public void setPercentageDone(float percentageDone) {
        this.percentageDone = percentageDone;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getCaseFileNumber() {
        return caseFileNumber;
    }

    public void setCaseFileNumber(String caseFileNumber) {
        this.caseFileNumber = caseFileNumber;
    }

    public String getCaseName() {
        return caseName;
    }

    public void setCaseName(String caseName) {
        this.caseName = caseName;
    }

}
