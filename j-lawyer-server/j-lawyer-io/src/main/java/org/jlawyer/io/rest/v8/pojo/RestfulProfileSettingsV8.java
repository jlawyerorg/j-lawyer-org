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
 * The editable per-user profile settings of the currently authenticated user — the web equivalent
 * of the desktop {@code UserProfileDialog}'s "Benachrichtigungen", "Sicherheit" and "Neue Akten"
 * tabs. These map onto the per-user settings blob ({@code UserSettingsKeys}) and are shared with the
 * desktop client, so both clients read and write the same values.
 */
public class RestfulProfileSettingsV8 {

    // Benachrichtigungen (notifications)
    private boolean notifyCalendarEntry = true;
    private boolean notifyCalendarEntryAuthored = false;
    private boolean notifyCalendarEntryReminder = true;
    private boolean notifyInstantMessageMention = true;
    private boolean notifyInstantMessageMentionDone = true;
    private boolean notifyInvoiceDue = true;
    private boolean notifyScheduledDailyAgenda = true;
    private boolean notifyScheduledWeeklyDigest = true;

    // Sicherheit (security)
    private boolean warnUnknownSenders = true;

    // Neue Akten (defaults for new cases)
    /** Id of the default owner group ('' = none). */
    private String defaultOwnerGroup = "";
    /** Ids of the groups granted access to new cases by default. */
    private List<String> defaultAllowedGroups = new ArrayList<>();

    public RestfulProfileSettingsV8() {
    }

    public boolean isNotifyCalendarEntry() {
        return notifyCalendarEntry;
    }

    public void setNotifyCalendarEntry(boolean notifyCalendarEntry) {
        this.notifyCalendarEntry = notifyCalendarEntry;
    }

    public boolean isNotifyCalendarEntryAuthored() {
        return notifyCalendarEntryAuthored;
    }

    public void setNotifyCalendarEntryAuthored(boolean notifyCalendarEntryAuthored) {
        this.notifyCalendarEntryAuthored = notifyCalendarEntryAuthored;
    }

    public boolean isNotifyCalendarEntryReminder() {
        return notifyCalendarEntryReminder;
    }

    public void setNotifyCalendarEntryReminder(boolean notifyCalendarEntryReminder) {
        this.notifyCalendarEntryReminder = notifyCalendarEntryReminder;
    }

    public boolean isNotifyInstantMessageMention() {
        return notifyInstantMessageMention;
    }

    public void setNotifyInstantMessageMention(boolean notifyInstantMessageMention) {
        this.notifyInstantMessageMention = notifyInstantMessageMention;
    }

    public boolean isNotifyInstantMessageMentionDone() {
        return notifyInstantMessageMentionDone;
    }

    public void setNotifyInstantMessageMentionDone(boolean notifyInstantMessageMentionDone) {
        this.notifyInstantMessageMentionDone = notifyInstantMessageMentionDone;
    }

    public boolean isNotifyInvoiceDue() {
        return notifyInvoiceDue;
    }

    public void setNotifyInvoiceDue(boolean notifyInvoiceDue) {
        this.notifyInvoiceDue = notifyInvoiceDue;
    }

    public boolean isNotifyScheduledDailyAgenda() {
        return notifyScheduledDailyAgenda;
    }

    public void setNotifyScheduledDailyAgenda(boolean notifyScheduledDailyAgenda) {
        this.notifyScheduledDailyAgenda = notifyScheduledDailyAgenda;
    }

    public boolean isNotifyScheduledWeeklyDigest() {
        return notifyScheduledWeeklyDigest;
    }

    public void setNotifyScheduledWeeklyDigest(boolean notifyScheduledWeeklyDigest) {
        this.notifyScheduledWeeklyDigest = notifyScheduledWeeklyDigest;
    }

    public boolean isWarnUnknownSenders() {
        return warnUnknownSenders;
    }

    public void setWarnUnknownSenders(boolean warnUnknownSenders) {
        this.warnUnknownSenders = warnUnknownSenders;
    }

    public String getDefaultOwnerGroup() {
        return defaultOwnerGroup;
    }

    public void setDefaultOwnerGroup(String defaultOwnerGroup) {
        this.defaultOwnerGroup = defaultOwnerGroup;
    }

    public List<String> getDefaultAllowedGroups() {
        return defaultAllowedGroups;
    }

    public void setDefaultAllowedGroups(List<String> defaultAllowedGroups) {
        this.defaultAllowedGroups = defaultAllowedGroups;
    }

}
