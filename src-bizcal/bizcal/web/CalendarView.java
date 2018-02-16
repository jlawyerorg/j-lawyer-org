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
package bizcal.web;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.ecs.Element;
import org.apache.ecs.html.Font;
import org.apache.ecs.html.Script;
import org.apache.ecs.html.TD;
import org.apache.ecs.html.TH;
import org.apache.ecs.html.TR;

import bizcal.common.Calendar;
import bizcal.common.CalendarModel;
import bizcal.common.CalendarViewConfig;
import bizcal.common.Event;
import bizcal.util.DateInterval;

/**
 * @author Fredrik Bertilsson
 */
public abstract class CalendarView
{
	private CalendarViewConfig config;
    private CalendarModel _callback;
    private DateInterval interval;
    private WebCalendarCallback webCallback;
    
	public DateInterval getInterval() {
		return interval;
	}
	public void setInterval(DateInterval interval) {
		this.interval = interval;
	}
	
	public void setDescriptor(CalendarViewConfig desc) 
	{
		CalendarViewConfig calD = (CalendarViewConfig) desc;
		setCalendarCallback(calD.getCallback());
	}	
    
    public void setCalendarCallback(CalendarModel callback)
    {
    	_callback = callback;
    }

    protected CalendarModel getCalendarCallback()
    {
    	return _callback;
    }
    
    public abstract Element getContent()
    	throws Exception;
    	
    /*protected Table createColorDesc() throws Exception
    {
        Table table = new Table();
        TR tr = new TR();
        table.addElement(tr);
        TD td = new TD(new I(getHelp()));
        tr.addElement(td);
        Iterator i = getCalendarCallback().getColorDescriptions().iterator();
        if (!i.hasNext())
        	return null;
        while (i.hasNext()) {
        	ColorDescription desc = (ColorDescription) i.next();
            tr.addElement(new TD("&nbsp;"));
            Div div = new Div("&nbsp;" + desc.getDescription() + "&nbsp;");
            Border border = new Border(div);
            border.setBgColor(toHTML(desc.getColor()));
            border.setPadding(2);
            td = new TD(border);
            tr.addElement(td);        	
        }        
        return table;
    }*/
    
    protected String fixLength(String str, int len)
    {
    	if (str.length() > len)
    		return str.substring(0, len);
    	for (int i=str.length(); i < len; i++)
    		str += "&nbsp;";
    	return str;
    }

    public TD createCell(Event event,
            DateInterval interval,
            Calendar cal,
            Date time)
    	throws Exception
    {
        TD td = new TD();
        Font font = new Font();
        td.addElement(font);
        font.addElement(event.getSummary());
        td.setBgColor(toHTML(event.getColor()));
        if (event.isEditable()) {
	        td.setOnClick("openOld('eventid="
	                + event.getId() + "')");
	        td.setOnMouseOver("this.style.cursor='pointer'");
        } else if (event.isBackground()) {
        	td.setOnClick(getOpenNewStatement(cal, time));
        }
        DateInterval eventInterv = new DateInterval(event.getStart(), event.getEnd());
        if (interval.getStartDate().after(eventInterv.getStartDate()))
        	eventInterv.setStartDate(interval.getStartDate());
        if (interval.getEndDate().before(eventInterv.getEndDate()))
        	eventInterv.setEndDate(interval.getEndDate());
        if (!event.isBackground()) {
	        int span = (int) (eventInterv.getDuration() / getResolution());
	        td.setRowSpan(span);
        }
        return td;
    }
    
    public TD createFreeCell(Calendar cal, Date time)
    	throws Exception
    {
        TD td = new TD();
        td.setBgColor(toHTML(cal.getColor()));
        if (cal.isBlankIsAvailible()) {
        	td.setOnClick(getOpenNewStatement(cal, time));
        	td.setOnMouseOver("this.style.cursor='pointer'");
        }
        return td;
    }
    
    private String getOpenNewStatement(Calendar cal, Date time)
    	throws Exception
    {    	
    	String startParam = getWebCallback().getStarttimeParamName();
    	String calParam = getWebCallback().getCalendarParamName();
        String href_new = calParam + "=" + cal.getId();
        href_new += "&" + startParam;
        href_new += "=\\\""
                 + time.getTime()
                 + "\\\"";
    	return "openNew('" + href_new + "')";
    }
        
           
    public Script getScript()
    	throws Exception
    {
    	Script script = new Script();
    	script.setLanguage("JavaScript");
    	
    	String eventFormId = "newEvent";
    	String url = getWebCallback().getDetailURL();
    	StringBuffer str = new StringBuffer();
    	str.append("function openNew(keys)\n");
    	str.append("{\n");
    	str.append("\thref = '" + url + "?detail=true&popup=true&' + keys;\n");
    	str.append("\twindow.open(href, 'detail');\n");
    	str.append("}\n");
    	str.append("function openOld(keys)\n");
    	str.append("{\n");
    	str.append("\thref = '" + url + "?detail=true&popup=true&' + keys;\n");
    	str.append("\twindow.open(href, 'detail');\n");
    	str.append("}\n");
    	
    	script.addElement(str.toString());
    	return script;   	
    }
    	
	protected List createSlots(List events, DateInterval interval)
		throws Exception
	{
		int noOfSlots = (int) (interval.getDuration() / getResolution());
		List slots = new ArrayList(noOfSlots);
		for (int i=0; i<noOfSlots; i++)
			slots.add(null);
		Iterator j = events.iterator();
		while (j.hasNext()) {
			Event event = (Event) j.next();
			long time = event.getStart().getTime();
			int slotno = getSlotNo(time, interval);					
			while (time < event.getEnd().getTime()) {
				if (slotno >= 0 && slotno < noOfSlots) {
					Event slotEvent = (Event) slots.get(slotno);
					if (slotEvent != null) {
						if (slotEvent.getLevel() < event.getLevel())
							slots.set(slotno, event);							
					} else
						slots.set(slotno, event);
				}
				time += getResolution();
				slotno++;
			}
		}
		return slots;
	}
	
	public int getSlotNo(long date, DateInterval interval)
		throws Exception
	{
		long time = date - interval.getStartDate().getTime();
		return (int) (time / getResolution());
	}
	
	protected long getResolution()
		throws Exception
	{
		return getCalendarCallback().getResolution();
	}
	
	private String toHTML(Color color)
	{
		StringBuffer str = new StringBuffer();
		str.append("#");
		str.append(Integer.toString(color.getRed(), 16));
		str.append(Integer.toString(color.getGreen(), 16));
		str.append(Integer.toString(color.getBlue(), 16));
		return str.toString();
	}
	
	public void setStartDate(Date date)
		throws Exception
	{
		setInterval(new DateInterval(date, java.util.Calendar.DAY_OF_YEAR));
	}
		
    protected TR createHeaderRow() throws Exception {
		TR tr = new TR();
		return tr;
	}

	protected TH createHeaderCell(String str) throws Exception {
		TH th = new TH();
		Font font = new Font();
		font.addElement(str);
		th.addElement(font);
		return th;
	}
	
    
	public void setWebCallback(WebCalendarCallback webCallback) {
		this.webCallback = webCallback;
	}
	
	protected WebCalendarCallback getWebCallback()
	{
		return webCallback;
	}
	
	protected CalendarViewConfig getConfig()
	{
		return config;
	}
	
	protected CalendarModel getModel()
	{
		return _callback;
	}
}
