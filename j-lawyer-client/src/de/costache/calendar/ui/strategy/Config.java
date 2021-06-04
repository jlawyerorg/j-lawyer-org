/**
 * Copyright 2013 Theodor Costache
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License. 
 */
package de.costache.calendar.ui.strategy;

import de.costache.calendar.model.CalendarEvent;
import de.costache.calendar.util.CalendarUtil;

import java.awt.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author theodorcostache
 */
public class Config {

    private Color lineColor;
    private Color middleLineColor;
    private Color dayHeaderForegroundColor;
    private Color dayHeaderBackgroundColor;
    private Color todayHeaderForegroundColor;
    private Color todayHeaderBackgroundColor;
    private Color eventDefaultBackgroundColor;
    private Color eventDefaultForegroundColor;
    private int workingHoursStart;
    private int workingHoursEnd;
    private Color outsideWorkingHoursColor;
    private Color dayDisabledBackgroundColor;
    private boolean allDayPanelVisible;
    private Color holidayBgColor;
    private Calendar intervalStart;
    private Calendar intervalEnd;

    public Config() {
        lineColor = new Color(220, 220, 220);
        middleLineColor = new Color(240, 240, 240);

        todayHeaderBackgroundColor = new Color(240, 230, 140, 128);
        dayHeaderBackgroundColor = new Color(173, 216, 230, 200);

        todayHeaderForegroundColor = Color.black;
        dayHeaderForegroundColor = Color.black;

        eventDefaultBackgroundColor = new Color(135, 184, 217, 128);
        eventDefaultForegroundColor = Color.DARK_GRAY;

        workingHoursStart = 8;
        workingHoursEnd = 17;

        outsideWorkingHoursColor = new Color(148, 197, 217, 40);
        dayDisabledBackgroundColor = new Color(148, 197, 217, 128);

        allDayPanelVisible = true;

        holidayBgColor = new Color(220, 220, 220);
    }

    public Color getLineColor() {
        return lineColor;
    }

    public void setLineColor(Color lineColor) {
        this.lineColor = lineColor;
    }

    public Color getMiddleLineColor() {
        return middleLineColor;
    }

    public void setMiddleLineColor(Color middleLineColor) {
        this.middleLineColor = middleLineColor;
    }

    public Color getDayHeaderBackgroundColor() {
        return dayHeaderBackgroundColor;
    }

    public void setDayHeaderBackgroundColor(Color dayHeaderBackgroundColor) {
        this.dayHeaderBackgroundColor = dayHeaderBackgroundColor;
    }

    public Color getDayHeaderForegroundColor() {
        return dayHeaderForegroundColor;
    }

    public void setDayHeaderForegroundColor(Color dayHeaderForegroundColor) {
        this.dayHeaderForegroundColor = dayHeaderForegroundColor;
    }

    public Color getTodayHeaderBackgroundColor() {
        return todayHeaderBackgroundColor;
    }

    public void setTodayHeaderBackgroundColor(Color todayHeaderBackgroundColor) {
        this.todayHeaderBackgroundColor = todayHeaderBackgroundColor;
    }

    public Color getTodayHeaderForegroundColor() {
        return todayHeaderForegroundColor;
    }

    public void setTodayHeaderForegroundColor(Color todayHeaderForegroundColor) {
        this.todayHeaderForegroundColor = todayHeaderForegroundColor;
    }

    public Color getEventDefaultBackgroundColor() {
        return eventDefaultBackgroundColor;
    }

    public void setEventDefaultBackgroundColor(Color eventDefaultBackgroundColor) {
        this.eventDefaultBackgroundColor = eventDefaultBackgroundColor;
    }

    public Color getEventDefaultForegroundColor() {
        return eventDefaultForegroundColor;
    }

    public void setEventDefaultForegroundColor(Color eventDefaultForegroundColor) {
        this.eventDefaultForegroundColor = eventDefaultForegroundColor;
    }

    public int getWorkingHoursEnd() {
        return workingHoursEnd;
    }

    public void setWorkingHoursEnd(int workingHoursEnd) {
        this.workingHoursEnd = workingHoursEnd;
    }

    public int getWorkingHoursStart() {
        return workingHoursStart;
    }

    public void setWorkingHoursStart(int workingHoursStart) {
        this.workingHoursStart = workingHoursStart;
    }

    public Color getOutsideWorkingHoursColor() {
        return outsideWorkingHoursColor;
    }

    public void setOutsideWorkingHoursColor(Color outsideWorkingHoursColor) {
        this.outsideWorkingHoursColor = outsideWorkingHoursColor;
    }

    public Color getDayDisabledBackgroundColor() {
        return dayDisabledBackgroundColor;
    }

    public void setDayDisabledBackgroundColor(Color dayDisabledBackgroundColor) {
        this.dayDisabledBackgroundColor = dayDisabledBackgroundColor;
    }

    public boolean isAllDayPanelVisible() {
        return allDayPanelVisible;
    }

    public void setAllDayPanelVisible(boolean allDayPanelVisible) {
        this.allDayPanelVisible = allDayPanelVisible;
    }

    public Color getHolidayBgColor() {
        return holidayBgColor;
    }

    public void setHolidayBgColor(Color holidayBgColor) {
        this.holidayBgColor = holidayBgColor;
    }

    public Calendar getIntervalStart() {
        return intervalStart;
    }

    void setIntervalStart(Calendar intervalStart) {
        this.intervalStart = intervalStart;
    }

    public Calendar getIntervalEnd() {
        return intervalEnd;
    }

    void setIntervalEnd(Calendar intervalEnd) {
        this.intervalEnd = intervalEnd;
    }
}
