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

import com.jdimension.jlawyer.persistence.AssistantConfig;

/**
 * A connection to an AI assistant ("Ingo") server (RestfulAssistantConfigV8) — the web equivalent of
 * the desktop "Ingo-Server" dialog. The HTTP basic-auth password is <b>write-only</b>: it is never
 * returned (only {@link #passwordSet} indicates whether one is stored) and is applied on write only
 * when non-empty, so an unchanged edit keeps the existing password.
 */
public class RestfulAssistantConfigV8 {

    private String id;
    private String name;
    private String url;
    private String userName;
    /** Write-only: the new HTTP basic-auth password; never populated on read. */
    private String password;
    /** Read-only: whether a password is stored. */
    private boolean passwordSet;
    private int connectionTimeout;
    private int readTimeout;
    /** Free-form assistant configuration (opaque JSON / text, round-tripped verbatim). */
    private String configuration;

    public RestfulAssistantConfigV8() {
    }

    /** Maps an entity to the DTO, masking the password (only {@link #passwordSet} is exposed). */
    public static RestfulAssistantConfigV8 fromEntity(AssistantConfig ac) {
        RestfulAssistantConfigV8 dto = new RestfulAssistantConfigV8();
        dto.id = ac.getId();
        dto.name = ac.getName();
        dto.url = ac.getUrl();
        dto.userName = ac.getUserName();
        dto.passwordSet = ac.getPassword() != null && !ac.getPassword().isEmpty();
        dto.connectionTimeout = ac.getConnectionTimeout();
        dto.readTimeout = ac.getReadTimeout();
        dto.configuration = ac.getConfiguration();
        return dto;
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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isPasswordSet() {
        return passwordSet;
    }

    public void setPasswordSet(boolean passwordSet) {
        this.passwordSet = passwordSet;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

}
