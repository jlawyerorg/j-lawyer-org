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
 * Server-wide configuration for synchronizing the address book into a Nextcloud/CardDAV address book.
 * Only contacts linked to at least one case are synced. The password is write-only: it is never
 * returned (a {@code passwordSet} flag says whether one is stored) and only applied when a non-empty
 * value is submitted.
 */
public class RestfulCardDavSyncV7 {

    private boolean enabled = false;
    private boolean birthdaySync = true;
    private String host = "";
    private int port = 443;
    private boolean ssl = true;
    private String path = "";
    private String user = "";
    private String password = "";
    private boolean passwordSet = false;
    /** CardDAV collection href of the target address book. */
    private String href = "";

    public RestfulCardDavSyncV7() {
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isBirthdaySync() { return birthdaySync; }
    public void setBirthdaySync(boolean birthdaySync) { this.birthdaySync = birthdaySync; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public boolean isSsl() { return ssl; }
    public void setSsl(boolean ssl) { this.ssl = ssl; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public boolean isPasswordSet() { return passwordSet; }
    public void setPasswordSet(boolean passwordSet) { this.passwordSet = passwordSet; }
    public String getHref() { return href; }
    public void setHref(String href) { this.href = href; }

}
