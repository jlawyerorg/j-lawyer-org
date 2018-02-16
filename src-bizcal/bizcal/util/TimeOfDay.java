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
 * @author Fredrik Bertilsson
 */
public class TimeOfDay
	implements Comparable
{
    private long _time;
    
    public TimeOfDay(long time)
    {
        _time = time;
    }
    
    public TimeOfDay(int hours, int minutes)
    {
    	_time = hours*3600*1000 + minutes*60*1000;	
    }
    
    public TimeOfDay(Date date) throws Exception {
		Calendar cal = Calendar.getInstance(LocaleBroker.getLocale());
		cal.setTimeZone(TimeZoneBroker.getTimeZone());
		cal.setTime(date);
		_time = cal.get(Calendar.HOUR_OF_DAY) * 60;
		_time += cal.get(Calendar.MINUTE);
		_time *= 60 * 1000;

	}
    
    public String toString()
    {
        return "" + _time;
    }
    
    public long getValue()
    {
        return _time;
    }
    
    public int getHour()
    	throws Exception
    {
    	return getCalendar().get(Calendar.HOUR_OF_DAY);
    }

    public int getMinute()
    	throws Exception
    {
    	return getCalendar().get(Calendar.MINUTE);
    }
    
    private Calendar getCalendar()
    	throws Exception
    {
    	Calendar cal = Calendar.getInstance(Locale.getDefault());
    	cal.setTimeZone(TimeZone.getTimeZone("GMT"));
    	cal.setTime(new Date(_time));
    	return cal;
    }
    
    public Date getDate(Date date)
    	throws Exception
    {
    	Calendar cal = Calendar.getInstance(Locale.getDefault());
    	cal.setTimeZone(TimeZone.getDefault());
    	cal.setTime(date);
    	cal.set(Calendar.HOUR_OF_DAY, getHour());
    	cal.set(Calendar.MINUTE, getMinute());
    	cal.set(Calendar.SECOND, 0);
    	cal.set(Calendar.MILLISECOND, 0);
    	return cal.getTime();
    }
    
    public int compareTo(Object other)
    {
        if (other == null)
            return -1;
    	TimeOfDay o = (TimeOfDay) other;    	
    	return (int) (_time - o.getValue());
    }
    
    public boolean equals(Object other)
    {
        return compareTo(other) == 0;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode () {
		int	l_Code = 17;
		l_Code = (int) (37*l_Code + this.getValue());
		return l_Code;
	}
    
}
