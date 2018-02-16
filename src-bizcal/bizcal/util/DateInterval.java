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
package bizcal.util;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * A DateInterval is a child of Interval. It represents a peace of time.
 * 
 * 
 * @author Fredrik Bertilsson
 * 
 * 
 * 
 */
public class DateInterval extends Interval
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private long _duration;

    /**
     * @param interval
     * @throws Exception
     */
    public DateInterval(Interval interval) throws Exception {
    /* ================================================== */
        setStart(interval.getStart());
        setEndDate((Date) interval.getEnd());
        setIncludeStart(interval.isIncludeStart());
        setIncludeEnd(interval.isIncludeEnd());
        /* ================================================== */
    }

    public DateInterval()
    {
    }

    /**
     * @param start
     * @param end
     * @throws Exception
     */
    public DateInterval(Date start, Date end) throws Exception {
    	/* ================================================== */
    	setStartDate(start);
    	setEndDate(end);
    	/* ================================================== */
    }

    public DateInterval(Date day)
    	throws Exception
    {
    	Date start = DateUtil.round2Day(day);
    	setStartDate(start);
    	Date end = DateUtil.getDiffDay(start, +1);
    	setEndDate(end);
    }

    public DateInterval(Date day, int type) throws Exception {
		setStartDate(day);
		Calendar cal =
			Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault());
		cal.setTime(day);
		cal.add(type, +1);
		setEndDate(cal.getTime());
	}

    public DateInterval(Date day, int type, int count) throws Exception {
		setStartDate(day);
		Calendar cal =
			Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault());
		cal.setTime(day);
		cal.add(type, count);
		setEndDate(cal.getTime());
	}

    public DateInterval(Date start, long duration) throws Exception {
		this(start, new Date(start.getTime() + duration));
	}

    /**
     * Startdate of the interval
     * @return
     * @throws Exception
     */
    public Date getStartDate() throws Exception {
		return (Date) getStart();
	}

	public void setStartDate(Date start) throws Exception {
		setStart(start);
	}

	public Date getEndDate() throws Exception {
		return (Date) getEnd();
	}

	public void setEndDate(Date end) throws Exception {
		setEnd(end);
		if (getStartDate() == null)
			return;
		if (end == null)
		    return;
		long diff = end.getTime() - getStartDate().getTime();
		_duration = (int) diff;
	}

	public void setDuration(long duration)
		throws Exception
	{
		_duration = duration;
		setEnd(new Date(getStartDate().getTime() + duration));
	}

	/**
	 * Duration of the interval
	 * @return duration in milisec
	 */
	public long getDuration()
	{
		return _duration;
	}

	public DateInterval intersection(DateInterval interval)
		throws Exception
	{
		Interval result = intersection((Interval) interval);
		return new DateInterval((Date) result.getStart(), (Date) result.getEnd());
	}

}
