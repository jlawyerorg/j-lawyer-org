/* ************************************************************************ *
 * Copyright 2011 costache for MSD                                            *
 * ************************************************************************ */
package de.costache.calendar.util;

import java.util.*;

import de.costache.calendar.JCalendar;
import de.costache.calendar.events.IntervalSelectionEvent;
import de.costache.calendar.events.IntervalSelectionListener;

/**
 * @author costache
 * 
 */
public class EventRepository {

	private final Map<JCalendar, List<IntervalSelectionListener>> intervalSelectionListeners = new HashMap<JCalendar, List<IntervalSelectionListener>>();

	private static final EventRepository instance = new EventRepository();

	/**
	 * 
	 */
	private EventRepository() {

	}

	public static EventRepository get() {
		return instance;
	}

	public void addIntervalSelectionListener(final JCalendar owner,
			final IntervalSelectionListener intervalSelectionListener) {
		if (!intervalSelectionListeners.containsKey(owner)) {
			intervalSelectionListeners.put(owner, new ArrayList<IntervalSelectionListener>());
		}
		this.intervalSelectionListeners.get(owner).add(intervalSelectionListener);
	}

	public void removeIntervalSelectionListener(final JCalendar owner,
			final IntervalSelectionListener intervalSelectionListener) {
		if (intervalSelectionListeners.containsKey(owner))
			this.intervalSelectionListeners.get(owner).remove(intervalSelectionListener);
	}

	public void triggerIntervalSelection(final JCalendar owner, final Date start, final Date end) {
		if (!intervalSelectionListeners.containsKey(owner))
			return;
		final IntervalSelectionEvent selectionEvent = new IntervalSelectionEvent(owner, start, end);
		for (final IntervalSelectionListener listener : intervalSelectionListeners.get(owner)) {
			listener.intervalSelected(selectionEvent);
		}
	}
}
