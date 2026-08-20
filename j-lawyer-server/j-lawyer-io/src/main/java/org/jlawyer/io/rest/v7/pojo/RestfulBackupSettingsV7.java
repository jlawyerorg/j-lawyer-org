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
 * Server-side data-backup (Datensicherung) configuration: whether scheduled backups run, the weekday
 * schedule and hour, the database connection to dump, the encryption password, and the sync/export
 * targets. Both passwords are write-only: they are never returned (a {@code *Set} flag indicates
 * whether a value is stored) and are only (re)applied when a non-empty value is submitted.
 */
public class RestfulBackupSettingsV7 {

    private boolean enabled = false;
    private int hour = 22;
    private boolean monday = false;
    private boolean tuesday = false;
    private boolean wednesday = false;
    private boolean thursday = false;
    private boolean friday = false;
    private boolean saturday = false;
    private boolean sunday = false;

    private String dbHost = "localhost";
    private int dbPort = 3306;
    private String dbName = "jlawyerdb";
    private String dbUser = "root";
    private String dbPassword = "";
    private boolean dbPasswordSet = false;

    private String encryptionPassword = "";
    private boolean encryptionPasswordSet = false;

    private String syncTarget = "";
    private String exportTarget = "";

    public RestfulBackupSettingsV7() {
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getHour() { return hour; }
    public void setHour(int hour) { this.hour = hour; }

    public boolean isMonday() { return monday; }
    public void setMonday(boolean monday) { this.monday = monday; }
    public boolean isTuesday() { return tuesday; }
    public void setTuesday(boolean tuesday) { this.tuesday = tuesday; }
    public boolean isWednesday() { return wednesday; }
    public void setWednesday(boolean wednesday) { this.wednesday = wednesday; }
    public boolean isThursday() { return thursday; }
    public void setThursday(boolean thursday) { this.thursday = thursday; }
    public boolean isFriday() { return friday; }
    public void setFriday(boolean friday) { this.friday = friday; }
    public boolean isSaturday() { return saturday; }
    public void setSaturday(boolean saturday) { this.saturday = saturday; }
    public boolean isSunday() { return sunday; }
    public void setSunday(boolean sunday) { this.sunday = sunday; }

    public String getDbHost() { return dbHost; }
    public void setDbHost(String dbHost) { this.dbHost = dbHost; }
    public int getDbPort() { return dbPort; }
    public void setDbPort(int dbPort) { this.dbPort = dbPort; }
    public String getDbName() { return dbName; }
    public void setDbName(String dbName) { this.dbName = dbName; }
    public String getDbUser() { return dbUser; }
    public void setDbUser(String dbUser) { this.dbUser = dbUser; }
    public String getDbPassword() { return dbPassword; }
    public void setDbPassword(String dbPassword) { this.dbPassword = dbPassword; }
    public boolean isDbPasswordSet() { return dbPasswordSet; }
    public void setDbPasswordSet(boolean dbPasswordSet) { this.dbPasswordSet = dbPasswordSet; }

    public String getEncryptionPassword() { return encryptionPassword; }
    public void setEncryptionPassword(String encryptionPassword) { this.encryptionPassword = encryptionPassword; }
    public boolean isEncryptionPasswordSet() { return encryptionPasswordSet; }
    public void setEncryptionPasswordSet(boolean encryptionPasswordSet) { this.encryptionPasswordSet = encryptionPasswordSet; }

    public String getSyncTarget() { return syncTarget; }
    public void setSyncTarget(String syncTarget) { this.syncTarget = syncTarget; }
    public String getExportTarget() { return exportTarget; }
    public void setExportTarget(String exportTarget) { this.exportTarget = exportTarget; }

}
