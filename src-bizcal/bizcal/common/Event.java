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
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.swing.ImageIcon;

import bizcal.util.DateUtil;
import bizcal.util.LocaleBroker;

/**
 * @author Fredrik Bertilsson
 */
public class Event
	implements Comparable
{
	
	// =============================================
	// constants to store parameters
	// in the properties hashmap
	// =============================================
	
	/**
	 * Parameter key for the distance of the diagonal lines in background mode
	 */
	public static final String LINE_DISTANCE = "linedistance";
	
	private Object id;
	private String summary;
	private String description;
	private Date start;
	private Date end;
	private int level 			  	= 0;
	private Color color 			= Color.LIGHT_GRAY;
	private boolean frame 			= true;
	private boolean roundedCorner 	= true;
	private boolean editable 		= true;
	private boolean showTime 		= true;
	private boolean showHeader 		= true;
	private String toolTip 			= null;
	private Map props 				= new HashMap();
	private boolean background 		= false;
	private boolean selectable 		= true;
	private ImageIcon icon 			= null;
	private Object orgEvent;

	private ImageIcon upperRightIcon;

	public Object getId() {
		return id;
	}
	public void setId(Object id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public Date getEnd() {
		return end;
	}
	public void setEnd(Date end) {
		this.end = end;
	}
	public Date getStart() {
		return start;
	}
	public void setStart(Date start) {
		this.start = start;
	}
	public String getSummary() {
		return summary;
	}
	public void setSummary(String summary) {
		this.summary = summary;
	}

	public Color getColor() {
		return color;
	}
	public void setColor(Color color) {
		this.color = color;
	}
	public boolean isFrame() {
		return frame;
	}
	public void setFrame(boolean frame) {
		this.frame = frame;
	}
	public int getLevel() {
		return level;
	}
	public void setLevel(int level) {
		this.level = level;
	}
	public boolean isRoundedCorner() {
		return roundedCorner;
	}
	public void setRoundedCorner(boolean roundedCorner) {
		this.roundedCorner = roundedCorner;
	}
	public boolean isEditable() {
		return editable;
	}
	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object event) {
		/* ================================================== */
		if (event instanceof Event && getId() != null) {
			return getId().equals(((Event) event).getId());
		}
		else
			return super.equals(event);
		/* ================================================== */
	}
	
	 /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode () {
		int	l_Code = 17;
		l_Code = (int) (37*l_Code + this.getStart().getTime()+this.getEnd().getTime());
		return l_Code;
	}
	
	
	@SuppressWarnings("unchecked")
	public void set(String property, Object value)
	{
		props.put(property, value);
	}

	public Object get(String property)
	{
		return props.get(property);
	}

	public boolean isShowTime() {
		return showTime;
	}
	public void setShowTime(boolean showTime) {
		this.showTime = showTime;
	}
	
	public boolean showHeader() {
		/* ================================================== */
		return this.showHeader;
		/* ================================================== */
	}
	
	public void setShowHeader(boolean b) {
		/* ================================================== */
		this.showHeader = b;
		/* ================================================== */
	}
	
	
	public String getToolTip()
	{
		if (toolTip != null)
			return toolTip;
		DateFormat format = DateFormat.getTimeInstance(DateFormat.SHORT, LocaleBroker.getLocale());
		return "<html>[" + format.format(getStart()) +
			"-" + format.format(getEnd()) + "] <b>" +
			summary+"</b><br><hr><br><table width=\"300\"><tr><td>"
			+description+"</td></tr></table>";
	}
	public void setToolTip(String toolTip) {
		this.toolTip = toolTip;
	}

	public boolean isBackground() {
		return background;
	}
	public void setBackground(boolean background) {
		this.background = background;
	}

	public boolean isSelectable() {
		return selectable;
	}
	public void setSelectable(boolean selectable) {
		this.selectable = selectable;
	}

	public String toString()
	{
		DateFormat format =
			DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, new Locale("sv"));
		format.setTimeZone(TimeZone.getTimeZone("CET"));
		return format.format(start) + " - " + format.format(end) + " : " + summary;
	}

	public ImageIcon getIcon() {
		return icon;
	}
	public void setIcon(ImageIcon icon) {
		this.icon = icon;
	}
	
	/**
	 * Get the icon that is painted in the upper right corner
	 * 
	 * @return
	 */
	public ImageIcon getUpperRightIcon() {
		return upperRightIcon;
	}

	/**
	 * Set an icon for the upper right corner.
	 * 
	 * @param upperRightIcon
	 */
	public void setUpperRightIcon(ImageIcon upperRightIcon) {
		this.upperRightIcon = upperRightIcon;
	}

	public Object getOrgEvent() {
		return orgEvent;
	}
	public void setOrgEvent(Object orgEvent) {
		this.orgEvent = orgEvent;
	}

	public int compareTo(Object other)
	{
		Event another = (Event) other;
		return start.compareTo(another.start);
	}


	/**
	 * Moves the Event to a new startdate.
	 *
	 * @param newStartDate
	 */
	public void move(Date newStartDate) {
		/* ================================================== */
		if (newStartDate != null) {
			setEnd(new Date(getEnd().getTime() + DateUtil.getDiffDay(getStart(), newStartDate)));
			setStart(newStartDate);
		}
		/* ================================================== */
	}


	/**
	 * Duplicate this object
	 *
	 * @return
	 */
	public Event copy() {
		/* ================================================== */
		Event e = new Event();

		e.setBackground(isBackground());
		e.setColor(getColor());
		e.setDescription(getDescription());

		e.setEditable(isEditable());
		e.setEnd(getEnd());
		e.setFrame(isFrame());

		e.setIcon(getIcon());
		e.setId(getId());
		e.setLevel(getLevel());

		e.setOrgEvent(getOrgEvent());
		e.setRoundedCorner(isRoundedCorner());
		e.setSelectable(isSelectable());

		e.setShowTime(isShowTime());
		e.setStart(getStart());
		e.setSummary(getSummary());

		e.setToolTip(getToolTip());

		return e;

		/* ================================================== */
	}



}
