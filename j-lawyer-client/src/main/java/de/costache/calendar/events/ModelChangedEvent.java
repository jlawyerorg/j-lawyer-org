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

import de.costache.calendar.JCalendar;
import de.costache.calendar.model.CalendarEvent;

/**
 * 
 * @author theodorcostache
 * 
 */
public class ModelChangedEvent {

	private JCalendar source;
	private CalendarEvent calendarEvent;

	/**
	 * Creates a new instance of {@link ModelChangedEvent}
	 */
	public ModelChangedEvent() {
	}

	/**
	 * Creates a new instance of {@link ModelChangedEvent}
	 * 
	 * @param source
	 * @param calendarEvent
	 */
	public ModelChangedEvent(JCalendar source, CalendarEvent calendarEvent) {
		super();
		this.source = source;
		this.calendarEvent = calendarEvent;
	}

	/**
	 * gets the source
	 * 
	 * @return
	 */
	public JCalendar getSource() {
		return source;
	}

	/**
	 * sets the source
	 * 
	 * @param source
	 */
	public void setSource(JCalendar source) {
		this.source = source;
	}

	/**
	 * 
	 * @return
	 */
	public CalendarEvent getCalendarEvent() {
		return calendarEvent;
	}

	/**
	 * 
	 * @param calendarEvent
	 */
	public void setCalendarEvent(CalendarEvent calendarEvent) {
		this.calendarEvent = calendarEvent;
	}
}
