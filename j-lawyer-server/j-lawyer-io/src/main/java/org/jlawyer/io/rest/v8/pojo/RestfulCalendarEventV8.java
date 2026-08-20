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
import com.jdimension.jlawyer.persistence.ArchiveFileReviewsBean;
import com.jdimension.jlawyer.persistence.CalendarSetup;

/**
 * A single calendar entry (follow-up / deadline / appointment) for the v8 calendar view, with
 * enough case context to render an agenda without a per-row detail fetch — OpenSpec change
 * {@code add-web-client}, task 3.4. Dates are epoch milliseconds (UTC) to avoid timezone/format
 * ambiguity on the client.
 *
 * @author jens
 */
public class RestfulCalendarEventV8 {

    private String id;
    /** {@code followup} (Wiedervorlage), {@code respite} (Frist) or {@code event} (Termin). */
    private String type;
    private String summary;
    private String description;
    private String location;
    /** Start of the entry as epoch milliseconds (UTC). */
    private long begin;
    /** End as epoch milliseconds (UTC); null for follow-ups/deadlines without an end time. */
    private Long end;
    private boolean done;
    private String assignee;
    private int reminderMinutes;
    private String caseId;
    private String caseFileNumber;
    private String caseName;
    /** Id of the calendar this entry belongs to; empty when none is assigned. */
    private String calendar;
    /** Colour of that calendar as a packed RGB int (the argument to java.awt.Color(int)). */
    private int calendarColor;
    /** Display name of that calendar; empty when none is assigned. */
    private String calendarName;

    public RestfulCalendarEventV8() {
    }

    /** Maps event type constants to the stable string used by the REST API. */
    public static String typeName(int eventType) {
        switch (eventType) {
            case ArchiveFileReviewsBean.EVENTTYPE_RESPITE:
                return "respite";
            case ArchiveFileReviewsBean.EVENTTYPE_EVENT:
                return "event";
            case ArchiveFileReviewsBean.EVENTTYPE_FOLLOWUP:
            default:
                return "followup";
        }
    }

    /**
     * Maps a review/event to the calendar DTO. The case reference is only read via the basic
     * columns already loaded by the service (id/file number/name), never a lazy association.
     */
    public static RestfulCalendarEventV8 fromReview(ArchiveFileReviewsBean rev) {
        RestfulCalendarEventV8 e = new RestfulCalendarEventV8();
        e.setId(rev.getId());
        e.setType(typeName(rev.getEventType()));
        e.setSummary(rev.getSummary());
        e.setDescription(rev.getDescription());
        e.setLocation(rev.getLocation());
        e.setBegin(rev.getBeginDate() != null ? rev.getBeginDate().getTime() : 0L);
        e.setEnd(rev.getEndDate() != null ? rev.getEndDate().getTime() : null);
        e.setDone(rev.isDone());
        e.setAssignee(rev.getAssignee());
        e.setReminderMinutes(rev.getReminderMinutes());
        ArchiveFileBean afb = rev.getArchiveFileKey();
        if (afb != null) {
            e.setCaseId(afb.getId());
            e.setCaseFileNumber(afb.getFileNumber());
            e.setCaseName(afb.getName());
        }
        // The calendar the entry belongs to carries the colour shown in the client. The
        // calendarSetup is an eager @ManyToOne, so it is available on the (possibly detached) bean.
        CalendarSetup cs = rev.getCalendarSetup();
        if (cs != null) {
            e.setCalendar(cs.getId());
            e.setCalendarColor(cs.getBackground());
            e.setCalendarName(cs.getDisplayName());
        }
        return e;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public long getBegin() {
        return begin;
    }

    public void setBegin(long begin) {
        this.begin = begin;
    }

    public Long getEnd() {
        return end;
    }

    public void setEnd(Long end) {
        this.end = end;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public int getReminderMinutes() {
        return reminderMinutes;
    }

    public void setReminderMinutes(int reminderMinutes) {
        this.reminderMinutes = reminderMinutes;
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

    public String getCalendar() {
        return calendar;
    }

    public void setCalendar(String calendar) {
        this.calendar = calendar;
    }

    public int getCalendarColor() {
        return calendarColor;
    }

    public void setCalendarColor(int calendarColor) {
        this.calendarColor = calendarColor;
    }

    public String getCalendarName() {
        return calendarName;
    }

    public void setCalendarName(String calendarName) {
        this.calendarName = calendarName;
    }
}
