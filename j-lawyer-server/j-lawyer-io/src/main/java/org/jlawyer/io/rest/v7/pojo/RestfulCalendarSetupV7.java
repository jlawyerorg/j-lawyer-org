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
 * A calendar definition (CalendarSetup): a Nextcloud/CalDAV calendar the server synchronizes events
 * to. eventType is 10 = follow-up (Wiedervorlage), 20 = respite/deadline (Frist), 30 = event (Termin).
 * The cloud password is write-only (never returned; only applied when a non-empty value is submitted).
 */
public class RestfulCalendarSetupV7 {

    private String id = null;
    private String displayName = "";
    private String href = "";
    private int eventType = 10;
    private int background = 0;
    private boolean deleteDone = false;

    private String cloudHost = "";
    private int cloudPort = 443;
    private boolean cloudSsl = true;
    private String cloudPath = "";
    private String cloudUser = "";
    private String cloudPassword = "";
    private boolean passwordSet = false;

    public RestfulCalendarSetupV7() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getHref() { return href; }
    public void setHref(String href) { this.href = href; }
    public int getEventType() { return eventType; }
    public void setEventType(int eventType) { this.eventType = eventType; }
    public int getBackground() { return background; }
    public void setBackground(int background) { this.background = background; }
    public boolean isDeleteDone() { return deleteDone; }
    public void setDeleteDone(boolean deleteDone) { this.deleteDone = deleteDone; }
    public String getCloudHost() { return cloudHost; }
    public void setCloudHost(String cloudHost) { this.cloudHost = cloudHost; }
    public int getCloudPort() { return cloudPort; }
    public void setCloudPort(int cloudPort) { this.cloudPort = cloudPort; }
    public boolean isCloudSsl() { return cloudSsl; }
    public void setCloudSsl(boolean cloudSsl) { this.cloudSsl = cloudSsl; }
    public String getCloudPath() { return cloudPath; }
    public void setCloudPath(String cloudPath) { this.cloudPath = cloudPath; }
    public String getCloudUser() { return cloudUser; }
    public void setCloudUser(String cloudUser) { this.cloudUser = cloudUser; }
    public String getCloudPassword() { return cloudPassword; }
    public void setCloudPassword(String cloudPassword) { this.cloudPassword = cloudPassword; }
    public boolean isPasswordSet() { return passwordSet; }
    public void setPasswordSet(boolean passwordSet) { this.passwordSet = passwordSet; }

}
