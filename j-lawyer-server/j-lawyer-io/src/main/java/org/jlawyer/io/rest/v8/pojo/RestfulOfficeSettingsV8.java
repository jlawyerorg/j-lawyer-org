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

/**
 * Connection settings for the external Office document server used by in-browser Office editing
 * (OpenSpec add-web-client, Decision 6). {@code provider} is {@code none|collabora|onlyoffice};
 * {@code baseUrl} is the document server the browser loads the editor from and the server fetches
 * discovery from; {@code wopiPublicUrl} is the j-lawyer base URL reachable BY the document server
 * (server-to-server WOPI, relevant for distributed deployments). The {@code secret} is write-only:
 * it is accepted on PUT but never returned on GET; {@code secretSet} indicates whether one is stored.
 *
 * @author jens
 */
public class RestfulOfficeSettingsV8 {

    private String provider = "none";
    private String baseUrl = "";
    private String wopiPublicUrl = "";
    private String secret = "";
    private boolean secretSet = false;

    public RestfulOfficeSettingsV8() {
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getWopiPublicUrl() {
        return wopiPublicUrl;
    }

    public void setWopiPublicUrl(String wopiPublicUrl) {
        this.wopiPublicUrl = wopiPublicUrl;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public boolean isSecretSet() {
        return secretSet;
    }

    public void setSecretSet(boolean secretSet) {
        this.secretSet = secretSet;
    }

}
