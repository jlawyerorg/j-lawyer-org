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

import com.jdimension.jlawyer.persistence.IntegrationHook;

/**
 * An outbound webhook (RestfulIntegrationHookV7) — the web equivalent of the desktop "Web Hooks"
 * dialog. The hook {@link #name} is the identity (primary key). The HTTP basic-auth password is
 * write-only: never returned (only {@link #authPasswordSet}) and applied on write only when
 * non-empty. {@link #hookType} is one of the server's hook types.
 */
public class RestfulIntegrationHookV7 {

    private String name;
    private String url;
    private String hookType;
    private String authUser;
    private String authPassword;
    private boolean authPasswordSet;
    private long connectionTimeout;
    private long readTimeout;

    public RestfulIntegrationHookV7() {
    }

    public static RestfulIntegrationHookV7 fromEntity(IntegrationHook h) {
        RestfulIntegrationHookV7 dto = new RestfulIntegrationHookV7();
        dto.name = h.getName();
        dto.url = h.getUrl();
        dto.hookType = h.getHookType();
        dto.authUser = h.getAuthenticationUser();
        dto.authPasswordSet = h.getAuthenticationPwd() != null && !h.getAuthenticationPwd().isEmpty();
        dto.connectionTimeout = h.getConnectionTimeout();
        dto.readTimeout = h.getReadTimeout();
        return dto;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getHookType() { return hookType; }
    public void setHookType(String hookType) { this.hookType = hookType; }
    public String getAuthUser() { return authUser; }
    public void setAuthUser(String authUser) { this.authUser = authUser; }
    public String getAuthPassword() { return authPassword; }
    public void setAuthPassword(String authPassword) { this.authPassword = authPassword; }
    public boolean isAuthPasswordSet() { return authPasswordSet; }
    public void setAuthPasswordSet(boolean authPasswordSet) { this.authPasswordSet = authPasswordSet; }
    public long getConnectionTimeout() { return connectionTimeout; }
    public void setConnectionTimeout(long connectionTimeout) { this.connectionTimeout = connectionTimeout; }
    public long getReadTimeout() { return readTimeout; }
    public void setReadTimeout(long readTimeout) { this.readTimeout = readTimeout; }

}
