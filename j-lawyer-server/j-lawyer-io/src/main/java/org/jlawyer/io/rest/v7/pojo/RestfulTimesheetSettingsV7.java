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
 * Global time-tracking settings: whether the user is prompted when starting a time recording while
 * another is running in the same case ({@code parallelLogsWarning}), and how a bare number entered
 * during manual booking is interpreted ({@code numericInput}: "minutes", "hours" or "reject").
 */
public class RestfulTimesheetSettingsV7 {

    private boolean parallelLogsWarning = false;
    private String numericInput = "minutes";

    public RestfulTimesheetSettingsV7() {
    }

    public boolean isParallelLogsWarning() {
        return parallelLogsWarning;
    }

    public void setParallelLogsWarning(boolean parallelLogsWarning) {
        this.parallelLogsWarning = parallelLogsWarning;
    }

    public String getNumericInput() {
        return numericInput;
    }

    public void setNumericInput(String numericInput) {
        this.numericInput = numericInput;
    }

}
