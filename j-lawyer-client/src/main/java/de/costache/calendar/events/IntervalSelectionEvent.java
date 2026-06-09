/* ************************************************************************ *
 * Copyright 2011 costache for MSD                                            *
 * ************************************************************************ */
package de.costache.calendar.events;

import java.util.Date;

import de.costache.calendar.JCalendar;

/**
 * @author costache
 * 
 */
public class IntervalSelectionEvent {

	private JCalendar owner;
	private Date intervalStart;
	private Date intervalEnd;

	/**
	 * Creates a new instance of {@link IntervalSelectionEvent}
	 */
	public IntervalSelectionEvent() {
	}

	/**
	 * Creates a new instance of {@link IntervalSelectionEvent}
	 * 
	 * @param intervalStart
	 *           the interval start
	 * @param intervalEnd
	 *           the interval end
	 * 
	 */
	public IntervalSelectionEvent(final JCalendar owner, final Date intervalStart, final Date intervalEnd) {
		super();
		this.owner = owner;
		this.intervalStart = intervalStart;
		this.intervalEnd = intervalEnd;
	}

	/**
	 * @return the intervalStart
	 */
	public Date getIntervalStart() {
		return intervalStart;
	}

	/**
	 * @param intervalStart
	 *           the intervalStart to set
	 */
	public void setIntervalStart(final Date intervalStart) {
		this.intervalStart = intervalStart;
	}

	/**
	 * @return the intervalEnd
	 */
	public Date getIntervalEnd() {
		return intervalEnd;
	}

	/**
	 * @param intervalEnd
	 *           the intervalEnd to set
	 */
	public void setIntervalEnd(final Date intervalEnd) {
		this.intervalEnd = intervalEnd;
	}

	/**
	 * @return the owner
	 */
	public JCalendar getOwner() {
		return owner;
	}

	/**
	 * @param owner
	 *           the owner to set
	 */
	public void setOwner(final JCalendar owner) {
		this.owner = owner;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "IntervalSelectionEvent [owner=" + owner + ", intervalStart=" + intervalStart + ", intervalEnd="
				+ intervalEnd + "]";
	}

}
