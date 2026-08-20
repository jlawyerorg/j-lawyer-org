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
 * Request body of the Dropscan scanbox-discovery endpoint
 * ({@code POST /v8/dropscan/discover-scanboxes}) — the REST equivalent of the desktop user
 * administration's "Test / Scanboxen ermitteln" button. Used while configuring a user's Dropscan
 * integration to verify the API token and list the available scanboxes.
 *
 * <p>If {@code apiToken} is set (the admin just typed a token), it is tested directly. Otherwise the
 * stored token of the user identified by {@code principalId} is used (the token is write-only in the
 * web UI, so an already-saved token is never sent back to the browser). Supplying neither is an
 * error.</p>
 *
 * @author jens
 */
public class RestfulDropscanDiscoverRequestV8 {

    private String apiToken;
    private String principalId;

    public RestfulDropscanDiscoverRequestV8() {
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public String getPrincipalId() {
        return principalId;
    }

    public void setPrincipalId(String principalId) {
        this.principalId = principalId;
    }
}
