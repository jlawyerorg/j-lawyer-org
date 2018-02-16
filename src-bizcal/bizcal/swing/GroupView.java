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

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import bizcal.common.CalendarModel;
import bizcal.common.CalendarViewConfig;
import bizcal.common.Event;
import bizcal.swing.util.FrameArea;
import bizcal.util.BizcalException;
import bizcal.util.DateInterval;
import bizcal.util.DateUtil;
import bizcal.util.LocaleBroker;
import bizcal.util.TextUtil;
import bizcal.util.TimeOfDay;

/**
 * @author Fredrik Bertilsson
 */
public class GroupView
	extends CalendarView
{
	private static final int LABEL_COL_WIDTH = 70;
	public static final int HOUR_RESOLUTION = 2;
	public static final int PREFERRED_HOUR_WIDTH = 10;
	public static final int PREFERRED_ROW_HEIGHT = 40;
	
	private List<List<FrameArea>> frameAreaRows = new ArrayList<List<FrameArea>>();
	private List eventRows = new ArrayList();
	private Map vLines = new HashMap();
	private List hLines = new ArrayList();
	private JLayeredPane calPanel;
	private JScrollPane scrollPane;
	private DaysHoursHeaderPanel columnHeader;
	private CalendarRowHeader rowHeader;
	private List calBackgrounds = new ArrayList();
	private int dayCount;
	
	public GroupView(CalendarViewConfig config, CalendarModel model) throws Exception	
    {
		super(config);
		setModel(model);
		font = new Font("Verdana", Font.PLAIN, 10);	 
		calPanel = new JLayeredPane();
		calPanel.setLayout(new Layout());
		ThisMouseListener mouseListener = new ThisMouseListener();
        calPanel.addMouseListener(mouseListener);
        calPanel.addMouseMotionListener(mouseListener);
        scrollPane = 
        	new JScrollPane(calPanel,
        			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
					JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setCursor(Cursor.getDefaultCursor());
		scrollPane.getVerticalScrollBar().setUnitIncrement(15);
        scrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, createCorner(true, true));
        scrollPane.setCorner(JScrollPane.LOWER_LEFT_CORNER, createCorner(true, false));
        scrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, createCorner(false, true));
		columnHeader = new DaysHoursHeaderPanel(config, model);	
        scrollPane.setColumnHeaderView(columnHeader.getComponent());
		rowHeader = new CalendarRowHeader(model, config);
		rowHeader.setFooterHeight(0);
        scrollPane.setRowHeaderView(rowHeader.getComponent());
		
    }
	
	public void refresh0() throws Exception
    {		
		calPanel.removeAll();
        calPanel.setBackground(Color.WHITE);
               
        frameAreaRows.clear();
        eventRows.clear();
        hLines.clear();
        vLines.clear();
        calBackgrounds.clear();

		dayCount = DateUtil.getDateDiff(getModel().getInterval().getEndDate(),
				getModel().getInterval().getStartDate());
        
        addDraggingComponents(calPanel);

        JLabel hLine = new JLabel();
        hLine.setBackground(getDescriptor().getLineColor());
        hLine.setOpaque(true);
        calPanel.add(hLine, Integer.valueOf(1));
        hLines.add(hLine);
        
        Iterator i = getModel().getSelectedCalendars().iterator();
        while (i.hasNext()) {
        	bizcal.common.Calendar cal = (bizcal.common.Calendar) i.next();
        	Object calId = cal.getId();
        	String calHeader = cal.getSummary();
        	calHeader = StringLengthFormater.formatNameString(calHeader, font, LABEL_COL_WIDTH-5);
                    	            	            
            hLine = new JLabel();
            hLine.setBackground(getDescriptor().getLineColor());
            hLine.setOpaque(true);
            calPanel.add(hLine, Integer.valueOf(1));
            hLines.add(hLine);

            List<FrameArea> frameAreas = new ArrayList<FrameArea>();
            frameAreaRows.add(frameAreas);

            List events = getModel().getEvents(calId);
            Collections.sort(events);
             
            eventRows.add(events);
            Iterator j = events.iterator();
            while (j.hasNext()) 
            {
                Event event = (Event) j.next();
                FrameArea area = createFrameArea(calId, event);
                frameAreas.add(area);
               	calPanel.add(area, Integer.valueOf(event.getLevel()));
            }           
        }		

        Calendar cal = Calendar.getInstance(LocaleBroker.getLocale());
        cal.setTime(getInterval().getStartDate());
        while (cal.getTime().getTime() < getInterval().getEndDate().getTime()) {
            Date date = cal.getTime();

            // Day line
            JLabel line = new JLabel();
            line.setBackground(getDescriptor().getLineColor2());
            line.setOpaque(true);
            calPanel.add(line, Integer.valueOf(2));
            vLines.put(date, line);
            
            if (dayCount <= 7) {
	            TimeOfDay startTime = getDescriptor().getStartView();
	            cal.set(Calendar.HOUR_OF_DAY, startTime.getHour());
	            cal.set(Calendar.MINUTE, startTime.getMinute());
	            cal.set(Calendar.SECOND, 0);
	            cal.set(Calendar.MILLISECOND, 0);
	            TimeOfDay endTime = getDescriptor().getEndView();
	            while (true) {
	            	TimeOfDay timeOfDay = new TimeOfDay(cal.getTime());
	            	if (timeOfDay.getValue() >= endTime.getValue())
	            		break;

	            	line = new JLabel();
	                line.setBackground(getDescriptor().getLineColor());
	                line.setOpaque(true);
	                calPanel.add(line, Integer.valueOf(2));
	                vLines.put(cal.getTime(), line);
	            	cal.add(Calendar.HOUR, +1 * HOUR_RESOLUTION);
	            }
            }
            
            cal.add(Calendar.DAY_OF_YEAR, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            //pos += 24 * 3600 * 1000;
            
        }
        
		i = getSelectedCalendars().iterator();
		while (i.hasNext()) {
			bizcal.common.Calendar calendar = (bizcal.common.Calendar) i.next();
			JPanel calBackground = new JPanel();
			calBackground.setBackground(calendar.getColor());
			calBackgrounds.add(calBackground);
			calPanel.add(calBackground, Integer.valueOf(1));
		}        
       
        calPanel.validate();
        calPanel.repaint();

		columnHeader.setModel(getModel());
		columnHeader.refresh(); 
		rowHeader.refresh();
		
    }
	
	private int getWidth()
	{
		return calPanel.getWidth();
	}
	
	private int getHeight()
	{
		return calPanel.getHeight();
	}
	
	protected int getCaptionRowHeight()
	{
		//return CAPTION_ROW_HEIGHT0 * 2;
		return 0;
	}	
	
	protected int getXOffset()
	{
		//return LABEL_COL_WIDTH;
		return 0;
	}
	
	private int getTimeHeight()
	{
		return getHeight() - getCaptionRowHeight();
	}
	
	private int getTimeWidth()
	{
		return getWidth() - getXOffset();
	}
	
	private int getRowHeight()
		throws Exception
	{
		//return getTimeHeight() / getModel().getSelectedCalendars().size();
		return PREFERRED_ROW_HEIGHT;
	}
	
	private int getXPos(Date date)
		throws Exception
	{
		TimeOfDay time = new TimeOfDay(date);		
		long x = time.getValue() - getDescriptor().getStartView().getValue();
		if (x < 0)
			x = 0;
		long dayViewDuration = getDescriptor().getEndView().getValue() -
			getDescriptor().getStartView().getValue();
		double ratio = ((double) x) / ((double) dayViewDuration);
		double dayWidth = getDayWidth();
		int datediff = DateUtil.getDateDiff(date, getInterval().getStartDate());
		return (int) (getXOffset() + datediff*dayWidth + ratio * dayWidth);
	}
	
	private double getDayWidth()
		throws Exception
	{
		long duration = getInterval().getDuration();
		duration = duration / 24 / 3600 / 1000;
		return ((double) getTimeWidth() / (double) duration);
	}
	
	protected LayoutManager getLayout()
	{
		return new Layout();
	}
	
	private class Layout implements LayoutManager
    {
        public void addLayoutComponent(String name, Component comp)
        {
        }

        public void removeLayoutComponent(Component comp)
        {
        }

        public Dimension preferredLayoutSize(Container parent)
        {
        	try {
	        	DateInterval interval = getModel().getInterval();
	        	int dayCount = 
	        		DateUtil.getDateDiff(interval.getEndDate(), interval.getStartDate());
	        	int width = dayCount * getHourCount() * PREFERRED_HOUR_WIDTH; 
	        	//int height = getModel().getSelectedCalendars().size() * PREFERRED_ROW_HEIGHT;
	        	int height = 10 * PREFERRED_ROW_HEIGHT;
	            return new Dimension(width, height);
        	} catch (Exception e) {
        		throw BizcalException.create(e);
        	}
        }

        public Dimension minimumLayoutSize(Container parent)
        {
            return new Dimension(50, 100);
        }

        public void layoutContainer(Container parent0)
        {
            try {
                int width = getWidth();
                int height = getHeight();
                int yoffset = getCaptionRowHeight();
                int rowHeight = getRowHeight();                
                
            	JLabel hLine = (JLabel) hLines.get(0);
            	hLine.setBounds(0, 0, width, 1);
                
                int yPos = yoffset;
                for (int i = 0; i < eventRows.size(); i++) {
                	List areas = (List) frameAreaRows.get(i);
                	List events = (List) eventRows.get(i);
                	
    				FrameArea prevArea = null;
    				int overlapCol = 0;
    				int overlapColCount = 0;
    				int overlapCols[] = new int[events.size()];
                	
                	for (int j=0; j < areas.size(); j++) {
                		FrameArea area = (FrameArea) areas.get(j);
                		Event event = (Event) events.get(j);
                		int x1 = getXPos(event.getStart());
                		int x2 = getXPos(event.getEnd());
                		area.setBounds(x1, 
                				yPos, 
								x2-x1, 
								rowHeight);  
                		
						// Overlap logic
						if (!event.isBackground()) {
							if (prevArea != null) {
								Rectangle r = prevArea.getBounds();
								int prevX2 = r.x + r.width;
								if (prevX2 > x1) {
									// Previous event overlap
									overlapCol++;
									if (prevX2 < x2) {
										// This events finish later than previous
										prevArea = area;
									}
								} else {
									overlapCol = 0;
									prevArea = area;
								}
							}  else
								prevArea = area;
							overlapCols[j] = overlapCol;
							if (overlapCol > overlapColCount)
								overlapColCount = overlapCol;
						} else
							overlapCols[j] = 0;						                		
                	}
					// Overlap logic. Loop the events/frameareas a second 
					// time and set the xpos and widths
					if (overlapColCount > 0) {
						int slotHeight = rowHeight / (overlapColCount+1);
						for (int j = 0; j < areas.size(); j++) {
							Event event = (Event) events.get(j);
							if (event.isBackground())
								continue;
							FrameArea area = (FrameArea) areas.get(j);
							int index = overlapCols[j];
							Rectangle r = area.getBounds();
							area.setBounds(r.x, r.y+index*slotHeight, r.width, slotHeight);
						}
					}
                	
                	
                	hLine = (JLabel) hLines.get(i+1);
                	hLine.setBounds(0, yPos + rowHeight, width, 1);
                	
					yPos += rowHeight;
                }
                
                int captionHeight = getCaptionRowHeight() / 2;
                //long pos = getInterval().getStartDate().getTime();
                Calendar cal = Calendar.getInstance(LocaleBroker.getLocale());
                cal.setTime(getInterval().getStartDate());
                while (cal.getTime().getTime() < getInterval().getEndDate().getTime()) {
                    Date date = cal.getTime();
                    int xpos = getXPos(date);
                    
                    JLabel line = (JLabel) vLines.get(date);
                    line.setBounds(xpos, 0, 1, height);

                    if (dayCount <= 7) {
	                    TimeOfDay startTime = getDescriptor().getStartView();
	                    cal.set(Calendar.HOUR_OF_DAY, startTime.getHour());
	                    cal.set(Calendar.MINUTE, startTime.getMinute());
	                    cal.set(Calendar.SECOND, 0);
	                    cal.set(Calendar.MILLISECOND, 0);
	                    TimeOfDay endTime = getDescriptor().getEndView();
	                    while (true) {
	                    	TimeOfDay timeOfDay = new TimeOfDay(cal.getTime());
	                    	if (timeOfDay.getValue() >= endTime.getValue())
	                    		break;

	                    	xpos = getXPos(cal.getTime());
	                        line = (JLabel) vLines.get(cal.getTime());
	                        line.setBounds(xpos, captionHeight, 1, height-captionHeight);	                    	
	                    	cal.add(Calendar.HOUR, +1 * HOUR_RESOLUTION);
	                    }
                    }
                    
                    cal.add(Calendar.DAY_OF_MONTH, 1);
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                }
                
                yPos = yoffset;
				for (int iCal = 0; iCal < calBackgrounds.size(); iCal++) {
					int x1 = getXOffset();
					int x2 = getWidth();
					JPanel calBackground = (JPanel) calBackgrounds.get(iCal);
					calBackground.setBounds(x1, yPos, x2 - x1,
							rowHeight);
					yPos += rowHeight;
				}
                
            } catch (Exception e) {
                throw BizcalException.create(e);
            }
        }
    }
	
	public JComponent getComponent()
	{
		return scrollPane;
	}
	
	protected Date getDate(int xPos, int yPos)
		throws Exception
	{
		double daywidth = getDayWidth();
		xPos -= getXOffset();
		int dayno = (int) (xPos / daywidth);
		xPos -= dayno*daywidth;		
		double ratio = (double) xPos / (double) daywidth;
		Date date = getInterval().getStartDate();
		date = DateUtil.getDiffDay(date, dayno);
		long dayViewDuration = getDescriptor().getEndView().getValue() -
			getDescriptor().getStartView().getValue();
		long startTime = getDescriptor().getStartView().getValue();
		long passedTime = (long) (ratio*dayViewDuration);
		TimeOfDay timeOfDay = new TimeOfDay(startTime+passedTime);
		date = timeOfDay.getDate(date);
		return date;
	}
	
	protected Object getCalendarId(int x, int y) 
		throws Exception 
	{
		int pos = y / getRowHeight();
		if (pos >= getSelectedCalendars().size())
			return null;
		bizcal.common.Calendar cal = 
			(bizcal.common.Calendar) getSelectedCalendars().get(pos);
		return cal.getId();
	}
	
	protected String getHeaderText()
		throws Exception
	{		
		Date from = getInterval().getStartDate();
		Calendar date = Calendar.getInstance(LocaleBroker.getLocale());
		date.setTime(getInterval().getEndDate());
		date.add(Calendar.DATE, -1);
		DateFormat format = new SimpleDateFormat("MMMM yyyy", LocaleBroker.getLocale());
		return TextUtil.formatCase(format.format(from));
	}
	
	private int getHourCount()
		throws Exception
	{
		return getDescriptor().getEndView().getHour() - 
			getDescriptor().getStartView().getHour();
	}
		
}
