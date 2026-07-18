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
 * The web dashboard ("Mein Desktop") configuration of the current user: which widgets are visible and
 * their per-widget settings. The server treats {@code config} as an opaque JSON string — it is stored
 * and returned verbatim; the schema is owned by the web client. Empty means "no config stored yet"
 * (the client applies its defaults). Mirrors how the desktop persists its grid layout as JSON.
 *
 * @author jens
 */
public class RestfulDashboardConfigV8 {

    private String config = "";

    public RestfulDashboardConfigV8() {
    }

    public RestfulDashboardConfigV8(String config) {
        this.config = config;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }
}
