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
package de.costache.calendar.model;

import java.util.Date;
import java.util.Observable;

/**
 * @author theodorcostache
 */
public class CalendarEvent extends Observable implements Comparable<CalendarEvent> {


    private String summary;
    private String description;
    private String location;
    private Date start;
    private Date end;
    private EventType type;
    private boolean selected;
    private boolean allDay;
    private int priority;
    private boolean holiday;

    /**
     *
     */
    public CalendarEvent() {
        type = new EventType();
        type.setName("default");
    }

    public CalendarEvent(final Date start, final Date end) {
        this();
        this.start = start;
        this.end = end;
    }

    public CalendarEvent(final String summary, final Date start, final Date end) {
        this(start, end);
        this.summary = summary;
        this.start = start;
        this.end = end;
    }

    public CalendarEvent(final Date start, final Date end, final EventType type) {
        this(start, end);
        this.type = type;
    }

    public CalendarEvent(final String sumamry, final Date start, final Date end, final EventType type) {
        this(sumamry, start, end);
        this.type = type;
    }

    /**
     * @return the summary
     */
    public String getSummary() {
        return summary;
    }

    /**
     * @param summary the summary to set
     */
    public void setSummary(final String summary) {
        this.summary = summary;
        setChanged();
        notifyObservers(Property.SUMMARY);
        clearChanged();
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(final String description) {
        this.description = description;
        setChanged();
        notifyObservers(Property.DESCRIPTION);
        clearChanged();
    }

    /**
     * @return the location
     */
    public String getLocation() {
        return location;
    }

    /**
     * @param location the location to set
     */
    public void setLocation(final String location) {
        this.location = location;
        setChanged();
        notifyObservers(Property.LOCATION);
        clearChanged();
    }

    /**
     * @return the start
     */
    public Date getStart() {
        return start;
    }

    /**
     * @param start the start to set
     */
    public void setStart(final Date start) {
        this.start = start;
        setChanged();
        notifyObservers(Property.START);
        clearChanged();
    }

    /**
     * @return the end
     */
    public Date getEnd() {
        return end;
    }

    /**
     * @param end the end to set
     */
    public void setEnd(final Date end) {
        this.end = end;
        setChanged();
        notifyObservers(Property.END);
        clearChanged();
    }

    /**
     * @return the type
     */
    public EventType getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(final EventType type) {
        this.type = type;
        setChanged();
        notifyObservers(Property.TYPE);
        clearChanged();
    }

    /**
     * @return
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * @param selected
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /**
     * @return the type
     */
    public boolean isAllDay() {
        return allDay;
    }

    /**
     * @param type the type to set
     */
    public void setAllDay(final boolean value) {
        this.allDay = value;
        setChanged();
        notifyObservers(Property.ALLDAY);
        clearChanged();
    }

    /**
     * @return the priority
     */
    public int getPriority() {
        return priority;
    }

    /**
     * @param priority the priority to set
     */
    public void setPriority(final int value) {
        this.priority = value;
        setChanged();
        notifyObservers(Property.PRIORITY);
        clearChanged();
    }

    public boolean isHoliday() {
        return holiday;
    }

    public void setHoliday(boolean holiday) {
        this.holiday = holiday;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((end == null) ? 0 : end.hashCode());
        result = prime * result + ((start == null) ? 0 : start.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final CalendarEvent other = (CalendarEvent) obj;
        if (end == null) {
            if (other.end != null)
                return false;
        } else if (!end.equals(other.end))
            return false;
        if (start == null) {
            if (other.start != null)
                return false;
        } else if (!start.equals(other.start))
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        return true;
    }

    @Override
    public int compareTo(final CalendarEvent o) {
        final int comp = start.compareTo(o.getStart());
        if (comp == 0)
            return end.compareTo(o.getEnd());
        return comp;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "CalendarEvent [start=" + start + ", end=" + end + ", type=" + type + "]";
    }

    public enum Property {
        SUMMARY, DESCRIPTION, LOCATION, START, END, TYPE, ALLDAY, PRIORITY
    }

}
