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
package de.costache.calendar.format;

import de.costache.calendar.model.CalendarEvent;

import java.util.Collection;

/**
 * To provide a custom tooltip formatter implement this interfaces and use
 * the method setTooltipFormater(...) from the {@link de.costache.calendar.JCalendar} class
 */
public interface CalendarEventFormat {

	public String format(CalendarEvent calendarEvent);

    public String format(Collection<CalendarEvent> holidays);
}
