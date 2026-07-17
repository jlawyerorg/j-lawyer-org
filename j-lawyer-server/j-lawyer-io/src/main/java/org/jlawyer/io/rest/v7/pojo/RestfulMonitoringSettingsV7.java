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
 * Server monitoring configuration — the web equivalent of the desktop "Servermonitoring" dialog's
 * settings: warn/error thresholds (%) per resource, which resources are monitored, and whether
 * notifications are sent (incl. backup success/failure). Backed by the {@code MONITOR_*} settings.
 */
public class RestfulMonitoringSettingsV7 {

    private int cpuWarn = 80;
    private int cpuError = 90;
    private int memWarn = 80;
    private int memError = 90;
    private int diskWarn = 80;
    private int diskError = 90;
    private int vmWarn = 75;
    private int vmError = 85;

    private boolean monitorCpu = true;
    private boolean monitorRam = true;
    private boolean monitorDisk = true;
    private boolean monitorJava = true;

    private boolean notify = false;
    private boolean notifyBackupSuccess = true;
    private boolean notifyBackupFailure = true;

    public RestfulMonitoringSettingsV7() {
    }

    public int getCpuWarn() { return cpuWarn; }
    public void setCpuWarn(int cpuWarn) { this.cpuWarn = cpuWarn; }
    public int getCpuError() { return cpuError; }
    public void setCpuError(int cpuError) { this.cpuError = cpuError; }
    public int getMemWarn() { return memWarn; }
    public void setMemWarn(int memWarn) { this.memWarn = memWarn; }
    public int getMemError() { return memError; }
    public void setMemError(int memError) { this.memError = memError; }
    public int getDiskWarn() { return diskWarn; }
    public void setDiskWarn(int diskWarn) { this.diskWarn = diskWarn; }
    public int getDiskError() { return diskError; }
    public void setDiskError(int diskError) { this.diskError = diskError; }
    public int getVmWarn() { return vmWarn; }
    public void setVmWarn(int vmWarn) { this.vmWarn = vmWarn; }
    public int getVmError() { return vmError; }
    public void setVmError(int vmError) { this.vmError = vmError; }

    public boolean isMonitorCpu() { return monitorCpu; }
    public void setMonitorCpu(boolean monitorCpu) { this.monitorCpu = monitorCpu; }
    public boolean isMonitorRam() { return monitorRam; }
    public void setMonitorRam(boolean monitorRam) { this.monitorRam = monitorRam; }
    public boolean isMonitorDisk() { return monitorDisk; }
    public void setMonitorDisk(boolean monitorDisk) { this.monitorDisk = monitorDisk; }
    public boolean isMonitorJava() { return monitorJava; }
    public void setMonitorJava(boolean monitorJava) { this.monitorJava = monitorJava; }

    public boolean isNotify() { return notify; }
    public void setNotify(boolean notify) { this.notify = notify; }
    public boolean isNotifyBackupSuccess() { return notifyBackupSuccess; }
    public void setNotifyBackupSuccess(boolean notifyBackupSuccess) { this.notifyBackupSuccess = notifyBackupSuccess; }
    public boolean isNotifyBackupFailure() { return notifyBackupFailure; }
    public void setNotifyBackupFailure(boolean notifyBackupFailure) { this.notifyBackupFailure = notifyBackupFailure; }

}
