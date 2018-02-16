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

import java.util.Calendar;

import bizcal.util.DateUtil;
import bizcal.util.TimeOfDay;

/**
 * 
 * The DayViewConfig class defines the explicit layout and behaviour
 * of the DayView.<br>
 * There are some default values that can be changed during usage of the config.
 * 
 * @author martin.heinemann@tudor.lu
 * 26.06.2007
 * 11:57:07
 *
 *
 * @version
 * <br>$Log: DayViewConfig.java,v $
 * <br>Revision 1.11  2008/06/12 13:04:18  heine_
 * <br>*** empty log message ***
 * <br>
 * <br>Revision 1.10  2008/03/28 08:45:12  heine_
 * <br>*** empty log message ***
 * <br>
 * <br>Revision 1.9  2008/01/21 14:12:52  heine_
 * <br>*** empty log message ***
 * <br>
 * <br>Revision 1.13  2007/07/09 07:16:47  heinemann
 * <br>*** empty log message ***
 * <br>
 * <br>Revision 1.12  2007/06/27 14:59:55  heinemann
 * <br>*** empty log message ***
 * <br>
 * <br>Revision 1.11  2007/06/26 13:10:51  heinemann
 * <br>*** empty log message ***
 * <br>
 *   
 */
public class DayViewConfig
	extends CalendarViewConfig
{
//	private int dayCount = 1;
	/**
	 * Switched off -> nothing will be printed on top of a column!!!
	 */
	private boolean showExtraDateHeaders = true;
	private boolean showDateFooter = false;
//	private TimeOfDay startView;
	private TimeOfDay endView;

	
	
	/**
	 * The alpha for the grid lines
	 */
	private int gridAlpha = 50;
	
	
	/**
	 * Timeslots per hour
	 */
	private int numberOfTimeSlots = 3;

	/**
	 * The default start time of the day view
	 */
	public  int DAY_START_DEFAULT = 7;
	
	/**
	 * The default end time of the day view
	 */
	public  int DAY_END_DEFAULT   = 18;


	/**
	 * The working start time of the day view
	 * Can be changed to switch to half day view
	 */
	private int dayStartHour = DAY_START_DEFAULT;
	
	/**
	 * The working end time of the day view
	 * Can be changed to switch to half day view
	 */ 
	private int dayEndHour   = DAY_END_DEFAULT;
	
	/**
	 * the day break time
	 */
	private int breakHour = 12;
	
	// this is only for the initial position of the scrollpaine
	private int dayViewStart = 7;
	private int dayViewEnd   = 12;

	private int weekStart = Calendar.MONDAY;
	private int weekEnd   = Calendar.SUNDAY;


	public DayViewConfig()
	{
//		startView = new TimeOfDay(dayViewStart, 0);
		endView = new TimeOfDay(dayViewEnd, 0);

		setCaption("Calendar");
	}

	public DayViewConfig(CalendarViewConfig calViewD)
	{
		copy(calViewD);
	}

	/**
	 * @return
	 */
	public int getDayCount() {
		return DateUtil.getDiffDay(weekStart, weekEnd);
	}
	/**
	 * @param dayCount
	 */
	@Deprecated
	public void setDayCount(int dayCount) {
//		this.dayCount = dayCount;
	}

	public boolean isShowExtraDateHeaders() {
		return showExtraDateHeaders;
	}

	public void setShowExtraDateHeaders(boolean showExtraDateHeaders) {
		this.showExtraDateHeaders = showExtraDateHeaders;
	}

	public void setShowDateFooter(boolean showDateFooter) {
		this.showDateFooter = showDateFooter;
	}

	public boolean isShowDateFooter() {
		return showDateFooter;
	}

	@Override
	public TimeOfDay getEndView() {
		return endView;
	}
	@Override
	public void setEndView(TimeOfDay endView) {
		this.endView = endView;
	}

	/**
	 * @return the numberOfTimeSlots
	 */
	public int getNumberOfTimeSlots() {
		return numberOfTimeSlots;
	}

	/**
	 * @param numberOfTimeSlots the numberOfTimeSlots to set
	 */
	public void setNumberOfTimeSlots(int numberOfTimeSlots) {
		this.numberOfTimeSlots = numberOfTimeSlots;
	}

	// =============================================================================
	// Default day start end hour
	// =============================================================================
	
	/**
	 * Get the default day end hour
	 * 
	 * @return
	 */
	public int getDefaultDayEndHour() {
		/* ================================================== */
		return this.DAY_END_DEFAULT;
		/* ================================================== */
	}
	
	/**
	 * get the default day start hour
	 * 
	 * @return
	 */
	public int getDefaultDayStartHour() {
		/* ================================================== */
		return this.DAY_START_DEFAULT;
		/* ================================================== */
	}
	
	
	public void setDefaultDayEndHour(int hour) {
		/* ================================================== */
		this.DAY_END_DEFAULT = hour;
		/* ================================================== */
	}
	
	public void setDefaultDayStartHour(int hour) {
		/* ================================================== */
		this.DAY_START_DEFAULT = hour;
		/* ================================================== */
	}
	
	
	// =============================================================================	
	
	
	/**
	 * @return the dayEndHour
	 */
	public int getDayEndHour() {
		return dayEndHour;
	}

	/**
	 * @param dayEndHour the dayEndHour to set
	 */
	public void setDayEndHour(int dayEndHour) {
		this.dayEndHour = dayEndHour;
	}

	/**
	 * @return the dayStartHour
	 */
	public int getDayStartHour() {
		return dayStartHour;
	}

	/**
	 * @param dayStartHour the dayStartHour to set
	 */
	public void setDayStartHour(int dayStartHour) {
		this.dayStartHour = dayStartHour;
	}

	/**
	 * Returns the amount of hours to display
	 *
	 * @return
	 */
	public int getHours() {
		/* ================================================== */
		return this.getDayEndHour() - this.getDayStartHour();
		/* ================================================== */
	}

	@Override
	public int getMinimumTimeSlotHeight() {
		/* ====================================================== */
		// TODO Auto-generated method stub
		return super.getMinimumTimeSlotHeight();
		/* ====================================================== */
	}

	/**
	 * @return the dayViewEnd
	 */
	public int getDayViewEnd() {
		/* ================================================== */
		return dayViewEnd;
		/* ================================================== */
	}

	/**
	 * @param dayViewEnd the dayViewEnd to set
	 */
	public void setDayViewEnd(int dayViewEnd) {
		this.dayViewEnd = dayViewEnd;
		
		endView = new TimeOfDay(dayViewEnd, 0);
	}

	/**
	 * @return the dayViewStart
	 */
	public int getDayViewStart() {
		return dayViewStart;
	}

	/**
	 * @param dayViewStart the dayViewStart to set
	 */
	public void setDayViewStart(int dayViewStart) {
		this.dayViewStart = dayViewStart;
//		startView = new TimeOfDay(dayViewStart, 0);
	}

	
	// ==============================================================
	// week start / week end
	// ==============================================================	
	/**
	 * @return the weekEnd
	 */
	public int getWeekEnd() {
		return weekEnd;
	}

	/**
	 * @param weekEnd the weekEnd to set
	 */
	public void setWeekStop(int weekEnd) {
		this.weekEnd = weekEnd;
	}

	/**
	 * @return the weekStart
	 */
	public int getWeekStart() {
		return weekStart;
	}

	/**
	 * @param weekStart the weekStart to set
	 */
	public void setWeekStart(int weekStart) {
		this.weekStart = weekStart;
	}

	/**
	 * Set the day break hour
	 * 
	 * @param breakHour
	 */
	public void setDayBreak(int breakHour) {
		/* ====================================================== */
		this.breakHour = breakHour;
		/* ====================================================== */
	}

	/**
	 * Get the day break hour
	 * @return
	 */
	public int getDayBreak() {
		/* ====================================================== */
		return this.breakHour;
		/* ====================================================== */
	}

	/**
	 * @return the gridAlpha
	 */
	public int getGridAlpha() {
		return gridAlpha;
	}

	/**
	 * @param gridAlpha the gridAlpha to set
	 */
	public void setGridAlpha(int gridAlpha) {
		this.gridAlpha = gridAlpha;
	}

	


}
