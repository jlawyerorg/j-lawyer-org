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
package de.costache.calendar.events;

import java.io.Serializable;

import de.costache.calendar.model.CalendarEvent;

/**
 * 
 * @author theodorcostache
 * 
 */
public class SelectionChangedEvent implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	CalendarEvent calendarEvent;

	public SelectionChangedEvent() {

	}

	public SelectionChangedEvent(CalendarEvent calendarEvent) {
		this.calendarEvent = calendarEvent;
	}

	public CalendarEvent getCalendarEvent() {
		return calendarEvent;
	}

	public void setCalendarEvent(CalendarEvent calendarEvent) {
		this.calendarEvent = calendarEvent;
	}
}
