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
/**
 * @author Martin Heinemann martin.heinemann@tudor.lu
 *
 *
 *
 * @version
 * <br>$Log: CalendarManagementListener.java,v $
 * <br>Revision 1.2  2007/09/20 07:23:16  heine_
 * <br>new version commit
 * <br>
 * <br>Revision 1.2  2007/06/20 12:08:24  heinemann
 * <br>*** empty log message ***
 * <br>
 * <br>Revision 1.1  2007/05/25 13:43:59  heinemann
 * <br>pres-weekend checkin
 * <br>
 *
 */
package lu.tudor.santec.bizcal.listeners;

import lu.tudor.santec.bizcal.NamedCalendar;

/**
 * Every class thath implement this interface and is registered at
 * the CalendarPanel will be informed when ever a calender should be
 * created, modified or deleted.
 *
 *
 * @author martin.heinemann@tudor.lu
 * 24.05.2007
 * 13:52:43
 *
 *
 * @version
 * <br>$Log: CalendarManagementListener.java,v $
 * <br>Revision 1.2  2007/09/20 07:23:16  heine_
 * <br>new version commit
 * <br>
 * <br>Revision 1.2  2007/06/20 12:08:24  heinemann
 * <br>*** empty log message ***
 * <br>
 * <br>Revision 1.1  2007/05/25 13:43:59  heinemann
 * <br>pres-weekend checkin
 * <br>
 *
 */
public interface CalendarManagementListener {


	/**
	 * create a new calendar
	 */
	public void newCalendar();

	/**
	 * modify a calendar
	 *
	 * @param calendar
	 */
	public void modifyCalendar(NamedCalendar calendar);


	/**
	 * Delete a calendar
	 * @param calendar
	 */
	public void deleteCalendar(NamedCalendar calendar);


}
