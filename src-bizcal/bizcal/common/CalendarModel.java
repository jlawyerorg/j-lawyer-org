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
package bizcal.common;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import bizcal.util.BizcalException;
import bizcal.util.DateInterval;
import bizcal.util.DateUtil;
import bizcal.util.TimeOfDay;

public interface CalendarModel
{
	public List<Event> getEvents(Object calId)
		throws Exception;

	public List getCalendars()
		throws Exception;

	public List getSelectedCalendars()
		throws Exception;

	public void refresh()
		throws Exception;

	public void deleteCalendar(Object id)
		throws Exception;

	public long getResolution()
		throws Exception;

	public DateInterval getInterval()
		throws Exception;

	public List getColorDescriptions()
		throws Exception;

	public boolean isInsertable(Object id, Date date)
		throws Exception;

	public boolean isRedDay(Date date)
		throws Exception;

	public TimeOfDay getViewStart()
		throws Exception;

	public TimeOfDay getViewEnd()
		throws Exception;

	public String getDateHeader(Object calId, Date date)
		throws Exception;

	public String getDateFooter(Object calId, Date date, List events)
		throws Exception;

	public abstract class BaseImpl
		implements CalendarModel
	{
		private int sunday = Calendar.SUNDAY;
		private TimeOfDay viewStart;
		private TimeOfDay viewEnd;
		private DateInterval interval;

		public BaseImpl()
		{
			try {
				viewStart = new TimeOfDay(7, 0);
				viewEnd = new TimeOfDay(18, 0);
			} catch (Exception e) {
				throw BizcalException.create(e);
			}
		}

		public List getCalendars() throws Exception
		{
			return new ArrayList();
		}

		public List getSelectedCalendars()
			throws Exception
		{
			return new ArrayList();
		}


		public void refresh() throws Exception
		{
		}

		public void deleteCalendar(Object id) throws Exception
		{
		}

		public long getResolution()
		{
			return 15 * 60 * 1000;
		}

		public List getColorDescriptions() throws Exception

		{
			return new ArrayList();
		}

		public boolean isInsertable(Object id, Date date)
		throws Exception
		{
			return true;
		}

		public boolean isRedDay(Date date)
		throws Exception
		{
			return DateUtil.getDayOfWeek(date) == sunday;
		}

		public TimeOfDay getViewStart()
		throws Exception
		{
			return viewStart;
		}

		public TimeOfDay getViewEnd()
		throws Exception
		{
			return viewEnd;
		}

		public void setViewStart(TimeOfDay value)
		{
			viewStart = value;
		}

		public void setViewEnd(TimeOfDay value)
		{
			viewEnd = value;
		}

		public DateInterval getInterval() {
			return interval;
		}

		public void setInterval(DateInterval interval)
			throws Exception
		{
			this.interval = interval;
		}

		public String getDateHeader(Object calId, Date date)
		throws Exception
		{
			return "mops";
		}

		public String getDateFooter(Object calId, Date date, List events)
		throws Exception
		{
			return "Puuups";
		}


	}

}
