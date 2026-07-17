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

import java.util.ArrayList;
import java.util.List;

/**
 * A read-only system report (the desktop "Systemreport" dialog): server version/host, the JVM system
 * properties as sorted key/value pairs, and a tail of the server log. Used to render an overview and
 * to assemble a downloadable report client-side.
 */
public class RestfulSystemReportV7 {

    private String serverVersion;
    private String hostName;
    private String ipAddress;
    private List<Property> properties = new ArrayList<>();
    private String serverLog;

    public RestfulSystemReportV7() {
    }

    /** One system property key/value pair. */
    public static class Property {
        private String key;
        private String value;

        public Property() {
        }

        public Property(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }

    public String getServerVersion() { return serverVersion; }
    public void setServerVersion(String serverVersion) { this.serverVersion = serverVersion; }
    public String getHostName() { return hostName; }
    public void setHostName(String hostName) { this.hostName = hostName; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public List<Property> getProperties() { return properties; }
    public void setProperties(List<Property> properties) { this.properties = properties; }
    public String getServerLog() { return serverLog; }
    public void setServerLog(String serverLog) { this.serverLog = serverLog; }

}
