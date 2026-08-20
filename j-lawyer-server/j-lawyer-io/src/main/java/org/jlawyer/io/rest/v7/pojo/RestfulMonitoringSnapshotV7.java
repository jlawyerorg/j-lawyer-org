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

import com.jdimension.jlawyer.server.services.MonitoringSnapshot;

/**
 * A point-in-time server health reading (the read-only tab of the desktop "Servermonitoring"
 * dialog). CPU is a percentage (10-minute average); disk/memory/VM are raw byte use/max so the
 * client can render both a percentage and a human-readable size.
 */
public class RestfulMonitoringSnapshotV7 {

    private long taken;
    private int cpuPercent;
    private long diskUse;
    private long diskMax;
    private long memoryUse;
    private long memoryMax;
    private long vmMemoryUse;
    private long vmMemoryMax;
    private String lastStatus;

    public RestfulMonitoringSnapshotV7() {
    }

    public static RestfulMonitoringSnapshotV7 fromSnapshot(MonitoringSnapshot s) {
        RestfulMonitoringSnapshotV7 dto = new RestfulMonitoringSnapshotV7();
        dto.taken = s.getTaken() != null ? s.getTaken().getTime() : 0L;
        dto.cpuPercent = (int) s.getCpuAverage();
        dto.diskUse = s.getDiskUse();
        dto.diskMax = s.getDiskMax();
        dto.memoryUse = s.getMemoryUse();
        dto.memoryMax = s.getMemoryMax();
        dto.vmMemoryUse = s.getVmMemoryUse();
        dto.vmMemoryMax = s.getVmMemoryMax();
        dto.lastStatus = s.getLastStatus();
        return dto;
    }

    public long getTaken() { return taken; }
    public void setTaken(long taken) { this.taken = taken; }
    public int getCpuPercent() { return cpuPercent; }
    public void setCpuPercent(int cpuPercent) { this.cpuPercent = cpuPercent; }
    public long getDiskUse() { return diskUse; }
    public void setDiskUse(long diskUse) { this.diskUse = diskUse; }
    public long getDiskMax() { return diskMax; }
    public void setDiskMax(long diskMax) { this.diskMax = diskMax; }
    public long getMemoryUse() { return memoryUse; }
    public void setMemoryUse(long memoryUse) { this.memoryUse = memoryUse; }
    public long getMemoryMax() { return memoryMax; }
    public void setMemoryMax(long memoryMax) { this.memoryMax = memoryMax; }
    public long getVmMemoryUse() { return vmMemoryUse; }
    public void setVmMemoryUse(long vmMemoryUse) { this.vmMemoryUse = vmMemoryUse; }
    public long getVmMemoryMax() { return vmMemoryMax; }
    public void setVmMemoryMax(long vmMemoryMax) { this.vmMemoryMax = vmMemoryMax; }
    public String getLastStatus() { return lastStatus; }
    public void setLastStatus(String lastStatus) { this.lastStatus = lastStatus; }

}
