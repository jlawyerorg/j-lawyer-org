/*******************************************************************************
 * Copyright (c) 2007 by CRP Henri TUDOR - SANTEC LUXEMBOURG 
 * check http://www.santec.tudor.lu for more information
 *  
 * Contributor(s):
 * Johannes Hermen  johannes.hermen(at)tudor.lu                            
 * Martin Heinemann martin.heinemann(at)tudor.lu  
 *  
 * This library is free software; you can redistribute it and/or modify it  
 * under the terms of the GNU Lesser General Public License (version 2.1)
 * as published by the Free Software Foundation.
 * 
 * This software is distributed in the hope that it will be useful, but     
 * WITHOUT ANY WARRANTY; without even the implied warranty of               
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU        
 * Lesser General Public License for more details.                          
 * 
 * You should have received a copy of the GNU Lesser General Public         
 * License along with this library; if not, write to the Free Software      
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 *******************************************************************************/
package lu.tudor.santec.bizcal.export;

import java.io.File;
import java.io.FileOutputStream;
import java.net.SocketException;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import lu.tudor.santec.bizcal.NamedCalendar;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.util.UidGenerator;
import bizcal.common.Event;

public class ICALExporter {

	private Calendar calendar;
	private UidGenerator uidGenerator;
	private boolean showCalendarName = true;
	
	/**
	 * static logger for this class
	 */
	private static Logger logger = Logger.getLogger(ICALExporter.class
			.getName());

	public ICALExporter() {
		calendar = new Calendar();
		calendar.getProperties().add(new ProdId("-//Ben Fortuna//iCal4j 1.0//EN"));
		calendar.getProperties().add(Version.VERSION_2_0);
		calendar.getProperties().add(CalScale.GREGORIAN);
		
		try {
			uidGenerator = new UidGenerator("1");
		} catch (SocketException e) {
			logger.log(Level.WARNING, "uidGenerator failed", e);
		}
	}
	
	public void saveEvents(Collection<Event> events) {
		for (Iterator iter = events.iterator(); iter.hasNext();) {
			Event event = (Event) iter.next();
			
			String summary = "";
			if (showCalendarName && event.get(NamedCalendar.CALENDAR_NAME) != null) {
				summary += event.get(NamedCalendar.CALENDAR_NAME) + ": " + "\n";
			}
			summary += event.getSummary();
			
			VEvent vev = new VEvent(
					new DateTime(event.getStart()),
					new DateTime(event.getEnd()),
					summary
				);
			Description d = new Description(event.getDescription());
			vev.getProperties().add(d);
			Location l = new Location((String)event.get(NamedCalendar.CALENDAR_NAME));
			vev.getProperties().add(l);
			vev.getProperties().add(uidGenerator.generateUid());

			calendar.getComponents().add(vev);
		}
	}
	
	public void writeICSFile(File f) {
		FileOutputStream fout;
		try {
			fout = new FileOutputStream(f);
			CalendarOutputter outputter = new CalendarOutputter();
			outputter.output(calendar, fout);
		} catch (Exception e) {
			logger.log(Level.WARNING, "writing ICS file failed", e);
		}
	}

	/**
	 * @return the showCalendarName
	 */
	public boolean isShowCalendarName() {
		return showCalendarName;
	}

	/**
	 * @param showCalendarName the showCalendarName to set
	 */
	public void setShowCalendarName(boolean showCalendarName) {
		this.showCalendarName = showCalendarName;
	}
	
}
