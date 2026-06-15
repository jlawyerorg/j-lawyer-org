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
package de.costache.calendar.util;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import de.costache.calendar.events.ModelChangedListener;
import de.costache.calendar.events.SelectionChangedListener;
import de.costache.calendar.model.CalendarEvent;

/**
 * 
 * @author theodorcostache
 * 
 */
public interface EventCollection {

	void add(CalendarEvent event);

	void remove(CalendarEvent event);

    void removeAll(List<CalendarEvent> calendarEventList);

	void addSelected(CalendarEvent event);

	void removeSelected(CalendarEvent event);

	void clearSelected(CalendarEvent event, boolean b);

    List<CalendarEvent> getHolidayEvents(Date date);

	Collection<CalendarEvent> getSelectedEvents();

	Collection<CalendarEvent> getEvents(Date date);

    Collection<CalendarEvent> getAllEvents();

	void addCollectionChangedListener(ModelChangedListener listener);

	void removeCollectionChangedListener(ModelChangedListener listener);

	void addSelectionChangedListener(SelectionChangedListener selectionChangedListener);

	void removeSelectionChangedListener(SelectionChangedListener selectionChangedListener);
}
