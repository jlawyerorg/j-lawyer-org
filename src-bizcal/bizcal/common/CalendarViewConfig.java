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

import java.awt.Color;
import java.awt.Font;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import bizcal.util.LocaleBroker;
import bizcal.util.TimeOfDay;

public class CalendarViewConfig
{
	private CalendarModel callback;
	private boolean showTopHeader = true;
	private Font font = new Font("Verdana", Font.PLAIN, 10);
//	private Color primaryColor = new Color(182,202,184);
	private Color primaryColor = new Color(100,100,245);
	private Color secondaryColor = new Color(255,255,255);
	private String caption;
	private Color lineColor = new Color(200, 200, 200);
	private Color lineColor2 = new Color(150, 150, 150);
	private Color lineColor3 = new Color(100, 100, 100);
	private TimeOfDay startView;
	private TimeOfDay endView;
	
	
	//	 formater for month view
	private DateFormat monthDateFormat = new SimpleDateFormat("EEEE",
			LocaleBroker.getLocale());
	// formater for week view
	private DateFormat weekDateFormat = new SimpleDateFormat("EE - dd.MM.",
			LocaleBroker.getLocale());
	// formater for day view
	private DateFormat dayFormat = new SimpleDateFormat("EEEE dd.MM.yyyy",
			LocaleBroker.getLocale());
	
	

	public CalendarViewConfig()
	{
		startView = new TimeOfDay(2, 0);
		endView = new TimeOfDay(18, 0);
	}

	public String getCaption() {
		return caption;
	}
	public void setCaption(String caption) {
		this.caption = caption;
	}
	public CalendarModel getCallback() {
		return callback;
	}
	public void setCallback(CalendarModel callback) {
		this.callback = callback;
	}

	public boolean isShowTopHeader() {
		return showTopHeader;
	}
	public void setShowTopHeader(boolean showTopHeader) {
		this.showTopHeader = showTopHeader;
	}
	public Font getFont() {
		return font;
	}
	public void setFont(Font font) {
		this.font = font;
	}

	public void copy(CalendarViewConfig other)
	{
		this.callback = other.callback;
		this.font = other.font;
		this.showTopHeader = other.showTopHeader;
		this.startView = other.startView;
		this.endView = other.endView;
	}

	public Color getPrimaryColor() {
		return primaryColor;
	}
	public void setPrimaryColor(Color primaryColor) {
		this.primaryColor = primaryColor;
	}
	public Color getSecondaryColor() {
		return secondaryColor;
	}
	public void setSecondaryColor(Color secondaryColor) {
		this.secondaryColor = secondaryColor;
	}
	public Color getLineColor() {
		return lineColor;
	}
	public void setLineColor(Color lineColor) {
		this.lineColor = lineColor;
	}
	public TimeOfDay getEndView() {
		return endView;
	}
	public void setEndView(TimeOfDay endView) {
		this.endView = endView;
	}
	public TimeOfDay getStartView() {
		return startView;
	}
	public void setStartView(TimeOfDay startView) {
		this.startView = startView;
	}

	public Color getLineColor2() {
		return lineColor2;
	}

	public void setLineColor2(Color lineColor2) {
		this.lineColor2 = lineColor2;
	}

	public Color getLineColor3() {
		return lineColor3;
	}

	public void setLineColor3(Color lineColor3) {
		this.lineColor3 = lineColor3;
	}

	public int getMinimumTimeSlotHeight() {
		/* ====================================================== */
		return 20;
		/* ====================================================== */
	}

	/**
	 * @return the dayFormat
	 */
	public DateFormat getDayFormat() {
		return dayFormat;
	}

	/**
	 * @param dayFormat the dayFormat to set
	 */
	public void setDayFormat(DateFormat dayFormat) {
		this.dayFormat = dayFormat;
	}

	/**
	 * @return the monthDateFormat
	 */
	public DateFormat getMonthDateFormat() {
		return monthDateFormat;
	}

	/**
	 * @param monthDateFormat the monthDateFormat to set
	 */
	public void setMonthDateFormat(DateFormat monthDateFormat) {
		this.monthDateFormat = monthDateFormat;
	}

	/**
	 * @return the weekDateFormat
	 */
	public DateFormat getWeekDateFormat() {
		return weekDateFormat;
	}

	/**
	 * @param weekDateFormat the weekDateFormat to set
	 */
	public void setWeekDateFormat(DateFormat weekDateFormat) {
		this.weekDateFormat = weekDateFormat;
	}
	
	
	
	
	
}
