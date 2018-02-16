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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import bizcal.common.CalendarViewConfig;
import bizcal.common.Event;
import bizcal.swing.util.ErrorHandler;
import bizcal.swing.util.FrameArea;
import bizcal.swing.util.TableLayoutPanel;
import bizcal.swing.util.TableLayoutPanel.Row;
import bizcal.util.BizcalException;
import bizcal.util.DateUtil;
import bizcal.util.TextUtil;

/**
 *
 * @version
 * <br>$Log: MonthView.java,v $
 * <br>Revision 1.19  2008/06/19 12:20:00  heine_
 * <br>*** empty log message ***
 * <br>
 *   
 */
public class MonthView extends CalendarView
{
	private ColumnHeaderPanel columnHeader;
	private List<List> cells = new ArrayList<List>();
	private List hLines = new ArrayList();
	private List vLines = new ArrayList();
	private JScrollPane scrollPane;
	private JPanel calPanel;

	public MonthView(CalendarViewConfig desc) throws Exception	{
		/* ================================================== */
		super(desc);
		calPanel = new JPanel();
		calPanel.setLayout(new Layout());

        scrollPane =
        	new JScrollPane(calPanel,
        			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
					JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setCursor(Cursor.getDefaultCursor());
		scrollPane.getVerticalScrollBar().setUnitIncrement(15);
        scrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, createCorner(true, true));
        scrollPane.setCorner(JScrollPane.LOWER_LEFT_CORNER, createCorner(true, false));
        scrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, createCorner(false, true));
		columnHeader = new ColumnHeaderPanel(desc, 7);

		columnHeader.setShowExtraDateHeaders(true);
		columnHeader.setMonthView(true);
        scrollPane.setColumnHeaderView(columnHeader.getComponent());
        /* ================================================== */
	}

	/* (non-Javadoc)
	 * @see bizcal.swing.CalendarView#refresh0()
	 */
	@SuppressWarnings("unchecked")
	public void refresh0() throws Exception {
		/* ================================================== */
		// clear all containers
		/* ------------------------------------------------------- */
		calPanel.removeAll();
		cells.clear();
		hLines.clear();
		vLines.clear();
		/* ------------------------------------------------------- */
		Calendar cal = DateUtil.newCalendar();
		/* ------------------------------------------------------- */
		// get the current date constraint for the month
		/* ------------------------------------------------------- */
		// create a new calendar object for this month
		cal.setTime(getInterval().getStartDate());
		/* ------------------------------------------------------- */
		// add 15 days to the start of the month, to get nealry
		// the middle of the month
		/* ------------------------------------------------------- */
		cal.add(Calendar.DAY_OF_YEAR, 15);
		int month = cal.get(Calendar.MONTH);
		/* ------------------------------------------------------- */
		// do something for the start of the week
		/* ------------------------------------------------------- */
    	int lastDayOfWeek = cal.getFirstDayOfWeek();
    	lastDayOfWeek--;
    	if (lastDayOfWeek < 1)
    		lastDayOfWeek += 7;
    	
    	/* ------------------------------------------------------- */
    	// iterate over all selected calendars
    	/* ------------------------------------------------------- */
    	Iterator j = getModel().getSelectedCalendars().iterator();
    	while (j.hasNext()) {
    		/* ------------------------------------------------------- */
    		bizcal.common.Calendar calInfo = (bizcal.common.Calendar) j.next();
    		
    		cal.setTime(getInterval().getStartDate());
    		cal.set(    Calendar.DAY_OF_MONTH, 1);
    		cal.set(    Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
    		/* ------------------------------------------------------- */
    		// get the events of this calendar
    		/* ------------------------------------------------------- */
	    	Map<Date, List<Event>> eventMap = createEventsPerDay(calInfo.getId());
	    	
	    	int rowno = 0;
	    	
	        while (true) {
	        	/* ------------------------------------------------------- */
		    	List<JComponent> row;
		    	if (cells.size() <= rowno) {
		    		/* ------------------------------------------------------- */
		    		row = new ArrayList<JComponent>();
		    		cells.add(row);
		    		/* ------------------------------------------------------- */
		    	} else
		    		row = cells.get(rowno);
		    	
	        	JComponent cell = createDayCell(cal, eventMap, month, calInfo.getId());
	        	calPanel.add(cell);
	        	row.add(cell);
	            if (cal.get(Calendar.DAY_OF_WEEK) == lastDayOfWeek) {
	            	/* ------------------------------------------------------- */
	            	if (cal.get(Calendar.MONTH) != month)
	            		break;
	            	rowno++;
	            	/* ------------------------------------------------------- */
	            }
	            cal.add(Calendar.DAY_OF_MONTH, 1);
	        }
    	}

        int colCount = getModel().getSelectedCalendars().size()*7;
        for (int i=0; i < colCount-1; i++) {
			JLabel line = new JLabel();
			line.setBackground(Color.LIGHT_GRAY);
			line.setOpaque(true);
			if ((i+1) % 7 == 0)
				line.setBackground(getDescriptor().getLineColor3());
			calPanel.add(line);
			vLines.add(line);
        }

        int rowCount = cells.size()-1;
        for (int i=0; i < rowCount; i++) {
			JLabel line = new JLabel();
			line.setBackground(Color.LIGHT_GRAY);
			line.setOpaque(true);
			calPanel.add(line);
			hLines.add(line);
        }

		columnHeader.setModel(getModel());
		columnHeader.setPopupMenuCallback(popupMenuCallback);
		columnHeader.refresh();
		/* ================================================== */
	}


	/**
	 * @param cal
	 * @param eventMap
	 * @param month
	 * @param calId
	 * @return
	 * @throws Exception
	 */
	private JComponent createDayCell(Calendar cal, Map<Date, List<Event>> eventMap, int month, Object calId) throws Exception	{
		/* ================================================== */
		Font eventFont = this.font;
		TableLayoutPanel panel = new TableLayoutPanel();
		
		if (cal.get(Calendar.MONTH) == month) {
			panel.setBackground(Color.WHITE);
		} else
			panel.setBackground(new Color(230, 230, 230));
		panel.createColumn(TableLayoutPanel.FILL);
		//panel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
		//panel.setBorder(BasicBorders.getRadioButtonBorder());
		int dayno = cal.get(Calendar.DAY_OF_MONTH);
		String text = "" + dayno;
		Row row = panel.createRow();
		JLabel label = new JLabel(text);
		//label.setOpaque(true);
		//label.setBackground(getDescriptor().getPrimaryColor());
		//label.setForeground(Color.black);
		label.setFont(font.deriveFont(Font.BOLD));
		row.createCell(label);
		panel.createRow(TableLayoutPanel.FILL);

		DateFormat format = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault());
		List<Event> events = eventMap.get(DateUtil.round2Day(cal.getTime()));

		if(events != null) {
			/* ------------------------------------------------------- */
			for (Event event : events) {
				/* ------------------------------------------------------- */
				row = panel.createRow();
				String time = format.format(event.getStart());
				String summary = "";
				if (event.getSummary() != null)
					summary = event.getSummary();
				JLabel eventLabel = new JLabel(time + " " + summary);
				eventLabel.setFont(eventFont);
				time += "-" + format.format(event.getEnd());
				eventLabel.setToolTipText(time + " " + summary);
				eventLabel.setOpaque(true);
				eventLabel.setBackground(event.getColor());
				/* ------------------------------------------------------- */
				// set foreground color
				eventLabel.setForeground(FrameArea.computeForeground(event.getColor()));
				/* ------------------------------------------------------- */
				eventLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
				if (event.getIcon() != null)
					eventLabel.setIcon(event.getIcon());
				eventLabel.addMouseListener(new EventMouseListener(event, calId));
				row.createCell(eventLabel, TableLayoutPanel.TOP, TableLayoutPanel.FULL);
				/* ------------------------------------------------------- */
			}
			/* ------------------------------------------------------- */
		}
		panel.addMouseListener(new DayMouseListener(calId, cal.getTime()));
		JScrollPane scrollPanel =
			new JScrollPane(panel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPanel.setPreferredSize(new Dimension(100,100));
		//return scrollPanel;
		return panel;
		/* ================================================== */
	}

	/**
	 * @author martin.heinemann@tudor.lu
	 * 15.06.2007
	 * 15:41:10
	 *
	 *
	 * @version
	 * <br>$Log: MonthView.java,v $
	 * <br>Revision 1.19  2008/06/19 12:20:00  heine_
	 * <br>*** empty log message ***
	 * <br>
	 * <br>Revision 1.18  2008/01/21 14:13:26  heine_
	 * <br>*** empty log message ***
	 * <br>
	 * <br>Revision 1.9  2007/06/22 13:14:49  heinemann
	 * <br>*** empty log message ***
	 * <br>
	 * <br>Revision 1.8  2007/06/20 12:08:08  heinemann
	 * <br>*** empty log message ***
	 * <br>
	 * <br>Revision 1.7  2007/06/18 11:41:32  heinemann
	 * <br>bug fixes and alpha optimations
	 * <br>
	 *
	 */
	private class EventMouseListener extends MouseAdapter {

		private Event event;

		private Object calId;

		/**
		 * @param event
		 * @param calId
		 */
		public EventMouseListener(Event event, Object calId) {
			/* ================================================== */
			this.calId = calId;
			this.event = event;
			/* ================================================== */
		}

		public void mouseClicked(MouseEvent mevent) {
			/* ================================================== */
			try {
				if (mevent.getClickCount() == 2)
					listener.showEvent(calId, event);
			} catch (Exception e) {
				ErrorHandler.handleError(e);
			}
			/* ================================================== */
		}
	}

	private class DayMouseListener
	extends MouseAdapter
	{
		private Object calId;
		private Date date;

		public DayMouseListener(Object calId, Date date)
		{
			this.calId = calId;
			this.date = date;
		}

		public void mouseEntered(MouseEvent e)
		{
			JPanel label = (JPanel) e.getSource();
			label.setCursor(new Cursor(Cursor.HAND_CURSOR));
			label.setBackground(label.getBackground().darker());
			label.setForeground(Color.LIGHT_GRAY);
		}

		public void mouseExited(MouseEvent e)
		{
			JPanel label = (JPanel) e.getSource();
			label.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			label.setBackground(label.getBackground().brighter());
			label.setForeground(Color.BLACK);
		}

		public void mouseClicked(MouseEvent e)
		{
			try {
				/* ------------------------------------------------------- */
				if (listener == null)
					return;
				/* ------------------------------------------------------- */
				if (e.getClickCount() < 2) {
					listener.dateSelected(date);
					return;
				}
				/* ------------------------------------------------------- */
	    		if (!getModel().isInsertable(calId, date))
	    			return;
	    		listener.newEvent(calId, date);
	    		/* ------------------------------------------------------- */
			} catch (Exception exc) {
				ErrorHandler.handleError(exc);
			}
		}
	}

	protected Date getDate(int xPos, int yPos) throws Exception {
		/* ================================================== */
		return null;
		/* ================================================== */
	}

	/**
	 * @return
	 * @throws Exception
	 */
	public long getTimeInterval() throws Exception {
		/* ================================================== */
//		return 24*3600*1000*30;
		return DateUtil.MILLIS_DAY*30;
		/* ================================================== */
	}

	/**
	 * @return
	 * @throws Exception
	 */
	protected String getHeaderText() throws Exception {
		/* ================================================== */
		Calendar cal = DateUtil.newCalendar();
		cal.setTime(getInterval().getStartDate());
		DateFormat format = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
		return TextUtil.formatCase(format.format(cal.getTime()));
		/* ================================================== */
	}

//	protected JComponent createCalendarPanel()
//	throws Exception
//	{
//		calPanel = new JPanel();
//		calPanel.setLayout(new Layout());
//		calPanel.setBackground(Color.WHITE);
//		return calPanel;
//	}

	protected boolean supportsDrag()
	{
		return false;
	}


	/**
	 * @author martin.heinemann@tudor.lu
	 * 19.06.2008
	 * 10:34:43
	 *
	 *
	 * @version
	 * <br>$Log: MonthView.java,v $
	 * <br>Revision 1.19  2008/06/19 12:20:00  heine_
	 * <br>*** empty log message ***
	 * <br>
	 *   
	 */
	private class Layout implements LayoutManager {
		
		
		public void addLayoutComponent(String name, Component comp) {
		}

		public void removeLayoutComponent(Component comp) {
		}

		public Dimension preferredLayoutSize(Container parent) {
			try {
				int width = 7 * getModel().getSelectedCalendars().size() * DayView.PREFERRED_DAY_WIDTH;
				return new Dimension(width, getPreferredHeight());
			} catch (Exception e) {
				throw BizcalException.create(e);
			}
		}

		public Dimension minimumLayoutSize(Container parent) {
			return new Dimension(50, 100);
		}

		/* (non-Javadoc)
		 * @see java.awt.LayoutManager#layoutContainer(java.awt.Container)
		 */
		public void layoutContainer(Container parent) {
			/* ================================================== */
			try {
				double width = parent.getWidth();
				width = width / getModel().getSelectedCalendars().size();
				width = width / 7;
				double height = parent.getHeight();
				height = height / cells.size();
				for (int row=0; row < cells.size(); row++) {
					/* ------------------------------------------------------- */
					List rowList = cells.get(row);
					for (int col=0; col < rowList.size(); col++) {
						/* ------------------------------------------------------- */
						JComponent cell = (JComponent) rowList.get(col);
						cell.setBounds((int) (col*width+1),
								(int) (row*height+1),
								(int) width-1,
								(int) height-1);
						/* ------------------------------------------------------- */
					}
					/* ------------------------------------------------------- */
				}

		        int colCount = getModel().getSelectedCalendars().size()*7;
		        for (int i=0; i < colCount-1; i++) {
		        	/* ------------------------------------------------------- */
		        	try {
		        		/* ------------------------------------------------------- */
						JLabel line = (JLabel) vLines.get(i);
						line.setBounds((int) ((i+1)*width),
								0,
								1,
								parent.getHeight());
						/* ------------------------------------------------------- */
		        	} catch (Exception e) {
//		        		e.printStackTrace();
					}
		        	/* ------------------------------------------------------- */
		        }
		        int rowCount = cells.size()-1;
		        for (int i=0; i < rowCount; i++) {
		        	/* ------------------------------------------------------- */
		        	try {
					JLabel line = (JLabel) hLines.get(i);
					line.setBounds(0,
							(int) ((i+1)*height),
							parent.getWidth(),
							1);
		        	} catch (Exception e) {

					}
		        	/* ------------------------------------------------------- */
		        }

			} catch (Exception e) {
				throw BizcalException.create(e);
			}
		}
		/* ================================================== */
	}

	/**
	 * @return
	 */
	private int getPreferredHeight() {
		return cells.size() * 40;
	}

	public JComponent getComponent() {
		return scrollPane;
	}

	public void addListener(CalendarListener listener) {
		super.addListener(listener);
		columnHeader.addCalendarListener(listener);
	}


}
