/*******************************************************************************
 * Bizcal is a component library for calendar widgets written in java using swing.
 * Copyright (C) 2007  Frederik Bertilsson 
 * Contributors:       Martin Heinemann martin.heinemann(at)tudor.lu
 * 
 * http://sourceforge.net/projects/bizcal/
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * 
 * [Java is a trademark or registered trademark of Sun Microsystems, Inc. 
 * in the United States and other countries.]
 * 
 *******************************************************************************/
package bizcal.swing;

import java.awt.event.MouseEvent;
import java.util.Date;
import java.util.List;

import bizcal.common.Event;
import bizcal.swing.util.FrameArea;
import bizcal.util.DateInterval;

/**
 * @author Fredrik Bertilsson
 */
public interface CalendarListener
{
	public void dateSelected(Date date) throws Exception;

	public void dateChanged(Date date) throws Exception;

	public void eventsSelected(List<Event> list) throws Exception;

	public void selectionReset() throws Exception;

	public void eventSelected(Object id, Event event) throws Exception;

	public void eventDoubleClick(Object id, Event event, MouseEvent mouseEvent);

	public void showEvent(Object id, Event event) throws Exception;

	public void newEvent(Object id, Date date) throws Exception;

	public void newEvent(Object id, DateInterval interval) throws Exception;

	public void copy(List<Event> list) throws Exception;

	public void paste(Object calId, Date date) throws Exception;

	public void moved(Event event, Object orgCalId, Date orgDate, Object newCalId, Date newDate)
		throws Exception;

	public void resized(Event event, Object orgCalId, Date orgEndDate, Date newEndDate)
		throws Exception;

	public void newCalendar()
		throws Exception;

	public void deleteEvent(Event event)
		throws Exception;

	public void deleteEvents(List<Event> events);

	public void closeCalendar(Object calId)
		throws Exception;

	public static class BaseImpl implements CalendarListener {
		public void dateSelected(Date date) throws Exception {
		}

		public void dateChanged(Date date) throws Exception {
		}


		public void selectionReset() throws Exception { }

		public void eventSelected(Object id, Event event) throws Exception { }

		public void showEvent(Object id, Event event) throws Exception
		{
		}

		public void newEvent(Object id, Date date) throws Exception {
		}

		public void newEvent(Object id, DateInterval interval) throws Exception {
		}

		public void selected(Object id, DateInterval interval) throws Exception
		{ }


		public void moved(Event event, Object orgCalId, Date orgDate,
				Object newCalId, Date newDate) throws Exception {
		}

		public void newCalendar()
			throws Exception
		{
		}

		public void deleteEvent(Event event)
		throws Exception
		{
		}

		public void paste(Object calId, Date date) throws Exception
		{
		}

		public void closeCalendar(Object calId)
			throws Exception
		{
		}

		public void resized(Event event, Object orgCalId, Date orgEndDate, Date newEndDate) throws Exception {
			/* ====================================================== */
			/* ====================================================== */
		}

		public void eventDoubleClick(Object id, Event event, MouseEvent mouseEvent) {
			/* ====================================================== */
			// TODO Auto-generated method stub
			/* ====================================================== */
		}

		public void eventClicked(Object id, Event _event, MouseEvent e) {
			/* ====================================================== */
			// TODO Auto-generated method stub
			/* ====================================================== */
		}

		public void eventClicked(Object id, Event _event, FrameArea area, MouseEvent e) {
			/* ====================================================== */
			// TODO Auto-generated method stub
			/* ====================================================== */
		}

		public void deleteEvents(List<Event> events) {
			/* ====================================================== */
			// TODO Auto-generated method stub
			/* ====================================================== */
		}

		public void eventsSelected(List<Event> list) throws Exception {
			/* ====================================================== */
			// TODO Auto-generated method stub
			/* ====================================================== */
		}

		public void copy(List<Event> list) throws Exception {
			/* ====================================================== */
			// TODO Auto-generated method stub
			/* ====================================================== */
		}

	}

	/**
	 * @param id
	 * @param _event
	 * @param area
	 * @param e
	 */
	public void eventClicked(Object id, Event _event, FrameArea area, MouseEvent e);

}
