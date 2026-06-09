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
import java.util.Date;

import de.costache.calendar.JCalendar;
import de.costache.calendar.ui.strategy.DisplayStrategy.Type;

/**
 * 
 * @author theodorcostache
 * 
 */
public class IntervalChangedEvent implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private JCalendar source;

	private Type selectedStrategy;

	private Date intervalStart;

	private Date intervalEnd;

	/**
	 * Creates a new instance of {@link IntervalChangedEvent}
	 */
	public IntervalChangedEvent() {

	}

	/**
	 * Creates a new instance of {@link IntervalChangedEvent}
	 * 
	 * @param source
	 * @param selectedStrategy
	 * @param intervalStart
	 * @param intervalEnd
	 */
	public IntervalChangedEvent(final JCalendar source, final Type selectedStrategy, final Date intervalStart,
			final Date intervalEnd) {
		super();
		this.source = source;
		this.selectedStrategy = selectedStrategy;
		this.intervalStart = intervalStart;
		this.intervalEnd = intervalEnd;
	}

	/**
	 * @return the source
	 */
	public JCalendar getSource() {
		return source;
	}

	/**
	 * @param source
	 *            the source to set
	 */
	public void setSource(final JCalendar source) {
		this.source = source;
	}

	/**
	 * @return the selectedStrategy
	 */
	public Type getSelectedStrategy() {
		return selectedStrategy;
	}

	/**
	 * @param selectedStrategy
	 *            the selectedStrategy to set
	 */
	public void setSelectedStrategy(final Type selectedStrategy) {
		this.selectedStrategy = selectedStrategy;
	}

	/**
	 * @return the intervalStart
	 */
	public Date getIntervalStart() {
		return intervalStart;
	}

	/**
	 * @param intervalStart
	 *            the intervalStart to set
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
	 *            the intervalEnd to set
	 */
	public void setIntervalEnd(final Date intervalEnd) {
		this.intervalEnd = intervalEnd;
	}

}
