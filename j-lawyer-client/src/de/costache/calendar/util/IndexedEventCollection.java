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

import de.costache.calendar.JCalendar;
import de.costache.calendar.events.ModelChangedEvent;
import de.costache.calendar.events.ModelChangedListener;
import de.costache.calendar.events.SelectionChangedEvent;
import de.costache.calendar.events.SelectionChangedListener;
import de.costache.calendar.model.CalendarEvent;
import de.costache.calendar.model.CalendarEvent.Property;
import org.apache.commons.collections.MultiHashMap;

import java.util.*;

/**
 * @author theodorcostache
 */
class IndexedEventCollection extends Observable implements Observer, EventCollection {

    private final MultiHashMap indexedEvents;
    private final List<ModelChangedListener> collectionChangedListeners;
    private final List<SelectionChangedListener> selectionChangedListeners;
    private final Set<CalendarEvent> selectedEvents;
    private final JCalendar parent;

    /**
     * Creates a new instance of {@link IndexedEventCollection}
     */
    public IndexedEventCollection(final JCalendar parent) {
        this.parent = parent;
        this.indexedEvents = new MultiHashMap();
        this.collectionChangedListeners = new ArrayList<ModelChangedListener>();
        this.selectionChangedListeners = new ArrayList<SelectionChangedListener>();
        this.selectedEvents = new HashSet<CalendarEvent>();
    }

    @Override
    public void add(final CalendarEvent calendarEvent) {
        calendarEvent.addObserver(this);
        final Collection<Date> dates = CalendarUtil.getDates(calendarEvent.getStart(), calendarEvent.getEnd());
        for (final Date date : dates) {

            if (!indexedEvents.containsValue(calendarEvent)) {
                indexedEvents.put(date, calendarEvent);
            }
        }
        notifyObservers();

        final ModelChangedEvent event = new ModelChangedEvent(parent, calendarEvent);
        for (final ModelChangedListener listener : collectionChangedListeners) {
            listener.eventAdded(event);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void remove(final CalendarEvent calendarEvent) {
        calendarEvent.deleteObserver(this);
        selectedEvents.remove(calendarEvent);

        for (final Object key : new HashSet<Object>(indexedEvents.keySet())) {
            final Collection<CalendarEvent> events = indexedEvents.getCollection(key);
            if (events.contains(calendarEvent))
                indexedEvents.remove(key, calendarEvent);
        }

        notifyObservers();

        final ModelChangedEvent event = new ModelChangedEvent(parent, calendarEvent);
        for (final ModelChangedListener listener : collectionChangedListeners) {
            listener.eventRemoved(event);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void removeAll(final List<CalendarEvent> calendarEvents) {
        for (CalendarEvent calendarEvent : calendarEvents) {
            remove(calendarEvent);
        }
    }

    @Override
    public void addSelected(final CalendarEvent calendarEvent) {
        selectedEvents.add(calendarEvent);
        final SelectionChangedEvent event = new SelectionChangedEvent(calendarEvent);
        for (final SelectionChangedListener listener : selectionChangedListeners) {
            listener.selectionChanged(event);
        }
    }

    @Override
    public void removeSelected(final CalendarEvent calendarEvent) {
        final boolean remove = selectedEvents.remove(calendarEvent);
        if (remove) {
            final SelectionChangedEvent event = new SelectionChangedEvent(calendarEvent);
            for (final SelectionChangedListener listener : selectionChangedListeners) {
                listener.selectionChanged(event);
            }
        }

    }

    @Override
    public void clearSelected(final CalendarEvent toIgnore, final boolean notifyListeners) {
        for (final CalendarEvent event : selectedEvents) {
            if (event != toIgnore) {
                event.setSelected(false);
            }
        }
        selectedEvents.clear();

        if (notifyListeners) {
            final SelectionChangedEvent event = new SelectionChangedEvent(null);
            for (final SelectionChangedListener listener : selectionChangedListeners) {
                listener.selectionChanged(event);
            }
        }
    }

    @Override
    public Collection<CalendarEvent> getSelectedEvents() {
        return Collections.unmodifiableSet(new HashSet<CalendarEvent>(selectedEvents));
    }

    @Override
    public Collection<CalendarEvent> getEvents(final Date date) {
        @SuppressWarnings("rawtypes")
        final Collection events = indexedEvents.getCollection(CalendarUtil.stripTime(date));
        if (events == null)
            return new ArrayList<CalendarEvent>();
        @SuppressWarnings("unchecked")
        final List<CalendarEvent> result = new ArrayList<CalendarEvent>(events);
        Collections.sort(result);
        return result;
    }

    @Override
    public Collection<CalendarEvent> getAllEvents() {

        Collection<CalendarEvent> values = indexedEvents.values();
        Set<CalendarEvent> result = new HashSet<CalendarEvent>();

        for (CalendarEvent event : values) {
            result.add(event);
        }
        return result;
    }

    @Override
    public List<CalendarEvent> getHolidayEvents(Date date) {
        Collection<CalendarEvent> events = getEvents(date);

        List<CalendarEvent> result = new ArrayList<CalendarEvent>();
        for(CalendarEvent event : events){
            if(event.isHoliday()) {
                result.add(event);
            }
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void update(final Observable o, final Object arg) {
        if (o instanceof CalendarEvent) {
            final CalendarEvent calendarEvent = (CalendarEvent) o;
            final Property property = (Property) arg;
            switch (property) {
                case START:
                case END:

                    for (final Object key : new HashSet<Object>(indexedEvents.keySet())) {
                        indexedEvents.remove(key, calendarEvent);
                    }

                    final Collection<Date> dates = CalendarUtil.getDates(calendarEvent.getStart(), calendarEvent.getEnd());
                    for (final Date date : dates) {
                        indexedEvents.put(date, calendarEvent);
                    }

                    notifyObservers(calendarEvent);

                    final ModelChangedEvent event = new ModelChangedEvent(parent, calendarEvent);
                    for (final ModelChangedListener listener : collectionChangedListeners) {
                        listener.eventChanged(event);
                    }

                default:
                    parent.invalidate();
                    parent.repaint();
                    break;
            }
        }
    }

    @Override
    public void addCollectionChangedListener(final ModelChangedListener listener) {
        this.collectionChangedListeners.add(listener);
    }

    @Override
    public void removeCollectionChangedListener(final ModelChangedListener listener) {
        this.collectionChangedListeners.remove(listener);
    }

    @Override
    public void addSelectionChangedListener(final SelectionChangedListener listener) {
        this.selectionChangedListeners.add(listener);
    }

    @Override
    public void removeSelectionChangedListener(final SelectionChangedListener listener) {
        this.selectionChangedListeners.remove(listener);
    }

    public int size() {
        return indexedEvents.size();
    }
}
