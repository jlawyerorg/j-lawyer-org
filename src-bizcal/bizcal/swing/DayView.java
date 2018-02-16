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
import java.awt.Point;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import bizcal.common.DayViewConfig;
import bizcal.common.Event;
import bizcal.swing.util.FrameArea;
import bizcal.util.BizcalException;
import bizcal.util.DateInterval;
import bizcal.util.DateUtil;
import bizcal.util.Interval;
import bizcal.util.TimeOfDay;
import bizcal.util.Tuple;


public class DayView extends CalendarView {
	public static int PIXELS_PER_HOUR = 80;

	private static final int CAPTION_ROW_HEIGHT0 = 20;

	public static final int PREFERRED_DAY_WIDTH = 10;

	public static final Integer GRID_LEVEL = Integer.valueOf(1);

	private List<List<FrameArea>> frameAreaCols = new ArrayList<List<FrameArea>>();

	private List<List<Event>> eventColList = new ArrayList<List<Event>>();

	private List<Date> _dateList = new ArrayList<Date>();

	private Map<Tuple, JLabel> timeLines = new HashMap<Tuple, JLabel>();
	
	private HashMap<Date, Integer> linePositionMap = new HashMap<Date, Integer>();
	
	private Map<Integer, Date> minuteMapping = Collections.synchronizedMap(new HashMap<Integer, Date>());

	private Map hourLabels = new HashMap();

	private Map minuteLabels = new HashMap();

	private List<JLabel> vLines = new ArrayList<JLabel>();

	private List<JPanel> calBackgrounds = new ArrayList<JPanel>();

	private ColumnHeaderPanel columnHeader;

	private TimeLabelPanel rowHeader;

	private int dayCount;

	private JScrollPane scrollPane;

	private JLayeredPane calPanel;

	private boolean firstRefresh = true;

	private DayViewConfig config;

	private List<JLabel> dateFooters = new ArrayList<JLabel>();



	/**
	 * @param desc
	 * @throws Exception
	 */
	public DayView(DayViewConfig desc) throws Exception  {
		this(desc, null);
	}


	/**
	 * @param desc
	 * @param upperLeftCornerComponent component that is displayed in the upper left corner of the scrollpaine
	 * @throws Exception
	 */
	public DayView(DayViewConfig desc, Component upperLeftCornerComponent) throws Exception {
		/* ================================================== */
		super(desc);
		this.config = desc;
		calPanel = new JLayeredPane();
		calPanel.setLayout(new Layout());
		ThisMouseListener mouseListener = new ThisMouseListener();
		ThisKeyListener keyListener = new ThisKeyListener();
		calPanel.addMouseListener(mouseListener);
		calPanel.addMouseMotionListener(mouseListener);
		calPanel.addKeyListener(keyListener);
		// calPanel.setPreferredSize(new
		// Dimension(calPanel.getPreferredSize().width,
		// calPanel.getPreferredSize().height+200));
		scrollPane = new JScrollPane(calPanel,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setCursor(Cursor.getDefaultCursor());
		scrollPane.getVerticalScrollBar().setUnitIncrement(15);

		/* ------------------------------------------------------- */
		if (upperLeftCornerComponent == null) {
			/* ------------------------------------------------------- */
			scrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, createCorner(true,
					true));
			/* ------------------------------------------------------- */
		} else {
			/* ------------------------------------------------------- */
			scrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, upperLeftCornerComponent);
			/* ------------------------------------------------------- */
		}

		scrollPane.setCorner(JScrollPane.LOWER_LEFT_CORNER, createCorner(true,
				false));
		scrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, createCorner(
				false, true));
		columnHeader = new ColumnHeaderPanel(desc);
		columnHeader.setShowExtraDateHeaders(desc.isShowExtraDateHeaders());
		scrollPane.setColumnHeaderView(columnHeader.getComponent());
		/* ------------------------------------------------------- */
		// set the time label at the left side
		rowHeader = new TimeLabelPanel(desc, new TimeOfDay(this.config.getDayStartHour(), 0),
				new TimeOfDay(this.config.getDayEndHour(), 0));
		/* ------------------------------------------------------- */
		rowHeader.setFooterHeight(getFooterHeight());
		scrollPane.setRowHeaderView(rowHeader.getComponent());

		// scrollPane.setPreferredSize(new Dimension(scrollPane.getWidth(),
		// scrollPane.getHeight()+400));

//		calPanel.addComponentListener(new ComponentAdapter() {
//			@Override
//			public void componentResized(ComponentEvent e) {
//				/* ====================================================== */
//				try {
//					// DayView.this.refresh();
//					// DayView.this.refresh0();
//				} catch (Exception e1) {
//					e1.printStackTrace();
//				}
//				/* ====================================================== */
//			}
//		});
		/* ================================================== */
	}

	public void refresh0() throws Exception {
		/* ================================================== */
//		System.out.println("DayView::refresh0");
//		System.out.println("----");
		if (calPanel == null || this.getModel() == null)
			return;
		/* ------------------------------------------------------- */
		// remove nealry everything from the panel
		/* ------------------------------------------------------- */
		dayCount = (int) (getModel().getInterval().getDuration() / (24 * 3600 * 1000));
		calPanel.removeAll();
		calPanel.setBackground(Color.WHITE);
		rowHeader.setStartEnd(new TimeOfDay(this.config.getDayStartHour(), 0),
				new TimeOfDay(this.config.getDayEndHour(), 0));
		rowHeader.setFooterHeight(getFooterHeight());
		rowHeader.getComponent().revalidate();


		frameAreaCols.clear();
		eventColList.clear();
		timeLines.clear();
		linePositionMap.clear();
		minuteMapping.clear();
		hourLabels.clear();
		minuteLabels.clear();
		calBackgrounds.clear();
		vLines.clear();
		dateFooters.clear();

		addDraggingComponents(calPanel);

		Font hourFont = getDayViewConfig().getFont().deriveFont((float) 12);
		hourFont = hourFont.deriveFont(Font.BOLD);
		/* ------------------------------------------------------- */
		// create a color for the lines
		/* ------------------------------------------------------- */
		Color color = getDayViewConfig().getLineColor();
		Color hlineColor = new Color(color.getRed(), color.getGreen(), color.getBlue(),getDayViewConfig().getGridAlpha());
		/* ------------------------------------------------------- */
		// Steps through the time axis and adds hour labels, minute labels
		// and timelines in different maps.
		// key: date, value: label
		/* ------------------------------------------------------- */
		long pos = getFirstInterval().getStartDate().getTime();
		while (pos < getFirstInterval().getEndDate().getTime()) {
			/* ------------------------------------------------------- */
			// create a date object for the current hour
			/* ------------------------------------------------------- */
			Date currentHour = new Date(pos);
			/* ------------------------------------------------------- */
			// load the number of timeslots per hour from the config
			/* ------------------------------------------------------- */
			int timeSlots = this.config.getNumberOfTimeSlots();
			// do not print more than 6 minute time slots (every 10'')
//			if (PIXELS_PER_HOUR > 120)
//				timeSlots = 6;
			/* ------------------------------------------------------- */
			// keep a maximum of timeslots per hour.
			/* ------------------------------------------------------- */
//			if (timeSlots > 10)
//				timeSlots = 10;
			/* ------------------------------------------------------- */
			// create a horizontal line for each time slot
			/* ------------------------------------------------------- */
			for (int i = 1; i <= timeSlots; i++) {
				/* ------------------------------------------------------- */
				// create a new JLabel for each line
				/* ------------------------------------------------------- */
				JLabel line = new JLabel();
				line.setOpaque(true);
				line.setBackground(hlineColor);
				/* ------------------------------------------------------- */
				// add the label to the panel. Layout will be done later in the layout manager
				/* ------------------------------------------------------- */
				calPanel.add(line, GRID_LEVEL);
				/* ------------------------------------------------------- */
				// put a tuple of the current day and the minute that the line is representing
				/* ------------------------------------------------------- */
				timeLines.put(new Tuple(currentHour, ""+(60/timeSlots)*i), line);
				addHorizontalLine(line);
				/* ------------------------------------------------------- */
			}
			/* ------------------------------------------------------- */
			// increase the position by one hour
			/* ------------------------------------------------------- */
			pos += DateUtil.MILLIS_HOUR;
			/* ------------------------------------------------------- */
		}
		if (config.isShowDateFooter()) {
			JLabel line = new JLabel();
			line.setBackground(getDayViewConfig().getLineColor());
			line.setOpaque(true);
			calPanel.add(line, GRID_LEVEL);
			timeLines.put(new Tuple(new Date(pos), "00"), line);
		}
		/* ------------------------------------------------------- */
		// create the columns for each day
		/* ------------------------------------------------------- */
		createColumns();
		/* ------------------------------------------------------- */
		// set the background color for each calendar
		/* ------------------------------------------------------- */
		for (Object obj : getSelectedCalendars()) {
			/* ------------------------------------------------------- */
			bizcal.common.Calendar cal = (bizcal.common.Calendar) obj;
			JPanel calBackground = new JPanel();
			calBackground.setBackground(cal.getColor());
			calBackgrounds.add(calBackground);
			calPanel.add(calBackground);
			/* ------------------------------------------------------- */
		}
		
		columnHeader.setModel(getModel());
		columnHeader.setPopupMenuCallback(popupMenuCallback);
		columnHeader.refresh();
		/* ------------------------------------------------------- */
		// if this is the first refresh, we must initialize the scrollpane
		/* ------------------------------------------------------- */
		if (firstRefresh)
			initScroll();
		firstRefresh = false;

		/* ------------------------------------------------------- */
		// do the refresh
		/* ------------------------------------------------------- */
		calPanel.validate();
		calPanel.repaint();
		/* ------------------------------------------------------- */
		// put the timelines in the background
		/* ------------------------------------------------------- */
		for (JLabel l : timeLines.values()) {
			try {
				/* --------------------------------------------- */
				calPanel.setComponentZOrder(l, calPanel.getComponents().length-2);
				/* --------------------------------------------- */
			} catch (Exception e) {
				/* --------------------------------------------- */
				e.printStackTrace();
				/* --------------------------------------------- */
			}
		}
		/* ------------------------------------------------------- */
		scrollPane.validate();
		scrollPane.repaint();

		rowHeader.getComponent().updateUI();
		// Hack to make to init scroll work
		// JScrollBar scrollBar = scrollPane.getVerticalScrollBar();
		// scrollBar.setValue(scrollBar.getValue()-1);

		/* ================================================== */
	}

	/**
	 * Returns the number of columns that are to display.
	 * As bizcal can display multiple calendars in parallel, it
	 * multiplies the number of days with the number of displayed calendars.
	 * 
	 * @return
	 * @throws Exception
	 */
	private int getColCount() throws Exception {
		/* ================================================== */
		return dayCount * getSelectedCalendars().size();
		/* ================================================== */
	}

	/**
	 * Returns the first interval to show. Start day plus one.
	 *
	 * @return
	 * @throws Exception
	 */
	private DateInterval getFirstInterval() throws Exception {
		/* ================================================== */
		Date start = getInterval().getStartDate();
//		Date end = DateUtil.getDiffDay(start, +1);

		return new DateInterval(
				DateUtil.round2Hour(start, this.config.getDayStartHour()),
				DateUtil.round2Hour(start, this.config.getDayEndHour()));
		/* ================================================== */
	}

	/**
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private void createColumns() throws Exception {
		DateInterval interval = getFirstInterval();
		int cols = getColCount();


		frameAreaHash.clear();
		List<Event> events = null;
		DateInterval interval2 = null;
		/* ------------------------------------------------------- */
		// iterate over all columns
		/* ------------------------------------------------------- */
		for (int it = 0; it < cols; it++) {
			/* ------------------------------------------------------- */
			int iCal = it / dayCount;
			bizcal.common.Calendar cal = (bizcal.common.Calendar) getSelectedCalendars()
					.get(iCal);
			Object calId = cal.getId();
			// obtain all events for the calendar
			events = broker.getEvents(calId);
			Collections.sort(events);

			if (it % dayCount == 0)
				interval2 = new DateInterval(interval);
			if (interval2 != null)
				_dateList.add(interval2.getStartDate());

			Calendar startdate = DateUtil.newCalendar();
			startdate.setTime(interval2.getStartDate());
			/* ------------------------------------------------------- */
			// create vertical lines
			Color vlColor = getDayViewConfig().getLineColor();
			int vlAlpha = getDayViewConfig().getGridAlpha()+50;
			if (vlAlpha > 255)
				vlAlpha = 255;
			/* ------------------------------------------------------- */
			Color vlAlphaColor = new Color(vlColor.getRed(), vlColor.getGreen(), vlColor.getBlue(), vlAlpha);
			/* ------------------------------------------------------- */
			if (it > 0) {
				/* ------------------------------------------------------- */
				JLabel verticalLine = new JLabel();
				verticalLine.setOpaque(true);
				verticalLine.setBackground(vlAlphaColor);
//				verticalLine.setBackground(getDesc().getLineColor());

				if (startdate.get(Calendar.DAY_OF_WEEK) == startdate
						.getFirstDayOfWeek())
					verticalLine.setBackground(getDescriptor().getLineColor2());
				if (getSelectedCalendars().size() > 1 && it % dayCount == 0)
					verticalLine.setBackground(getDescriptor().getLineColor3());
				calPanel.add(verticalLine, GRID_LEVEL);
				vLines.add(verticalLine);
				/* ------------------------------------------------------- */
			}
			/* ------------------------------------------------------- */
			List<FrameArea> frameAreas = new ArrayList<FrameArea>();
			// l�gger till en framearea-lista f�r varje dag
			frameAreaCols.add(frameAreas);
			// f�r alla event f�r personen inom intervallet
			if (calId == null)
				continue;
			Interval currDayInterval = getInterval(it % dayCount);
			List<Event> colEvents = new ArrayList<Event>();
			eventColList.add(colEvents);
			/* ------------------------------------------------------- */
			int iEvent = 0;
			if (events == null)
				events = new ArrayList();
			
			for (Event event : events) {
				/* ------------------------------------------------------- */
				DateInterval eventInterv = new DateInterval(event.getStart(),
						event.getEnd());
				if (!currDayInterval.overlap(eventInterv))
					continue;

				// if there are overlapping events
				FrameArea area = createFrameArea(calId, event);

				area.setBackground(config.getPrimaryColor());

				frameAreas.add(area);
				colEvents.add(event);
			
				calPanel.add(area, Integer.valueOf(event.getLevel()));
				iEvent++;
				
				
				
				/* ------------------------------------------------------- */
				if (!frameAreaHash.containsKey(event))
					frameAreaHash.put(event, area);
				else {
					frameAreaHash.get(event).addChild(area);

				}

			}

			if (config.isShowDateFooter()) {
				JLabel footer = new JLabel(broker.getDateFooter(cal.getId(),
						interval2.getStartDate(), colEvents));
				footer.setHorizontalAlignment(JLabel.CENTER);
				dateFooters.add(footer);
				calPanel.add(footer);
			}

			if (dayCount > 1)
				interval2 = incDay(interval2);
		}

	}

	// F�r in ett events start- eller slutdatum, h�jden p� f�nstret samt
	// intervallet som positionen ska ber�knas utifr�n
	/**
	 * Returns the y position for the date and the day
	 * 
	 * @param aDate
	 * @param dayNo
	 * @return
	 * @throws Exception
	 */
	private int getYPos(Date aDate, int dayNo) throws Exception {
		long time = aDate.getTime();
		return getYPos(time, dayNo);
	}

	/**
	 * @param time
	 * @param dayNo
	 * @return
	 * @throws Exception
	 */
	private int getYPos(long time, int dayNo) throws Exception {
		/* ================================================== */
		DateInterval interval = getInterval(dayNo);
		time -= interval.getStartDate().getTime();

		double viewPortHeight = getHeight() - getCaptionRowHeight()
				- getFooterHeight();
		// double timeSpan = (double) getTimeSpan();
//		double timeSpan = 24 * 3600 * 1000;
		double timeSpan = this.config.getHours() * 3600 * 1000;

		double dblTime = time;
		int ypos = (int) (dblTime / timeSpan * viewPortHeight);
		ypos += getCaptionRowHeight();
		return ypos;
		/* ================================================== */
	}

	/*
	 * private long getTimeSpan() throws Exception { return
	 * getDesc().getViewEndTime().getValue() -
	 * getDesc().getViewStartTime().getValue(); }
	 */
	 
	/***
	 * Try to get a date fitting to the given position
	 * 
	 */
	protected synchronized Date getDate(int xPos, int yPos) throws Exception {
		/* ================================================== */
		
		int colNo = getColumn(xPos);
		int dayNo = 0;
		/* ------------------------------------------------------- */
		// try to find the day in which the xPointer is located
		/* ------------------------------------------------------- */
		if (dayCount != 0)
			dayNo = colNo % dayCount;
		/* ------------------------------------------------------- */
		// get the DateInterval for the day
		/* ------------------------------------------------------- */
		DateInterval interval = getInterval(dayNo);
		/* ------------------------------------------------------- */
		// now we have the day. Next step is to find the time
		/* ------------------------------------------------------- */
		yPos -= getCaptionRowHeight();
		
//		BigDecimal bg_Pos = new BigDecimal (yPos);
//		BigDecimal bg_timeHeight = new BigDecimal (getTimeHeight());
//		BigDecimal bg_ratio = bg_Pos.divide (bg_timeHeight,5,RoundingMode.DOWN);
//		BigDecimal bg_time = new BigDecimal (interval.getDuration()).multiply(bg_ratio);
//		long time = bg_time.longValue();
		
//		Date duration = DateUtil.round2Minute(new Date(interval.getDuration()));
		
//		double ratio = ((double) yPos) / ((double) getTimeHeight());
		
//		long time = (long) Math.round((double)(interval.getDuration()/60000  * yPos) / (double)getTimeHeight());
//		
//		BigDecimal b = new BigDecimal(interval.getDuration());
//		
//		b.round(new MathContext(60000));
//		time *= 60000;
//		time += interval.getStartDate().getTime();
		/* ------------------------------------------------------- */
		Date foundDate = null;
		while (foundDate == null) {
			/* ------------------------------------------------------- */
			foundDate = minuteMapping.get(yPos);
			yPos++;
			if (yPos < 0 )
				break;
			if (yPos >= getHeight()) 
				yPos = getHeight();
//				break;
			/* ------------------------------------------------------- */
		}
		/* ------------------------------------------------------- */
//		return new Date(time);
		if (foundDate != null) {
			TimeOfDay td = DateUtil.getTimeOfDay(foundDate);
			Date d = td.getDate(interval.getStartDate());
			return d;
		}
		return null;
//		return foundDate;
		
		
		
		/* ================================================== */
	}
	
	
//	private static long normalize(long time) {
//		/* ================================================== */
////		return DateUtil.round2Minute(new Date(time)).getTime();
////		int mod = 1000;
//		BigDecimal b = new BigDecimal(time);
////		BigDecimal rounded = b.round(new MathContext(60000, RoundingMode.HALF_UP));
//		BigDecimal rounded = b.setScale(60000, RoundingMode.HALF_DOWN);
//		
//		
//		System.out.println("Normalizing " + time + " to " + rounded.longValue());
//		
////		return rounded.longValue();
//		return time;
//		
////		return time/60000;
//		/* ================================================== */
//	}
	
	/**
	 * Returns the DateInterval object for the given day
	 * 
	 * @param dayNo
	 * @return
	 * @throws Exception
	 */
	private DateInterval getInterval(int dayNo) throws Exception {
		/* ================================================== */
		// get the first interval
		/* ------------------------------------------------------- */
		DateInterval interval = getFirstInterval();
		/* ------------------------------------------------------- */
		// cycle through the days until we have reached the desired one
		/* ------------------------------------------------------- */
		for (int i = 0; i < dayNo; i++)
			interval = incDay(interval);
		return interval;
		/* ================================================== */
	}

	private int getColumn(int xPos) throws Exception {
		xPos -= getXOffset();
		int width = getWidth() - getXOffset();
		double ratio = ((double) xPos) / ((double) width);
		return (int) (ratio * getColCount());
	}

	private Object getCalendarId(int colNo) throws Exception {
		int pos = 0;
		// dayCount = 1;
		if (dayCount != 0)
			pos = colNo / dayCount;
		bizcal.common.Calendar cal = (bizcal.common.Calendar) getSelectedCalendars()
				.get(pos);
		return cal.getId();
	}

	protected int getXOffset() {
		// return LABEL_COL_WIDTH;
		return 0;
	}

	private int getXPos(int colno) throws Exception {
		double x = getWidth();
		x = x - getXOffset();
		double ratio = ((double) colno) / ((double) getColCount());
		return ((int) (x * ratio)) + getXOffset();
		/*
		 * BigDecimal xPos = new BigDecimal((x * ratio) + getXOffset()); return
		 * xPos.setScale(0,BigDecimal.ROUND_CEILING).intValue();
		 */
	}

	private int getWidth() {
		return calPanel.getWidth();
	}

	private int getHeight() {
		return calPanel.getHeight();
	}

	private int getTimeHeight() throws Exception {
		return getHeight() - getCaptionRowHeight() - getFooterHeight();
	}

	private int getFooterHeight() {
		if (config.isShowDateFooter())
			return PIXELS_PER_HOUR / 2;
		return 0;
	}

	/**
	 *
	 * 05.06.2007 11:31:56
	 *
	 *
	 * @version <br>
	 *          $Log: DayView.java,v $
	 *          Revision 1.38  2008/12/12 16:20:11  heine_
	 *          *** empty log message ***
	 *
	 *          Revision 1.37  2008/08/12 12:47:27  heine_
	 *          fixed some bugs and made code improvements
	 *
	 *          Revision 1.36  2008/06/10 13:16:36  heine_
	 *          *** empty log message ***
	 *
	 *          Revision 1.35  2008/06/09 14:10:09  heine_
	 *          *** empty log message ***
	 *
	 *          Revision 1.34  2008/05/30 11:36:48  heine_
	 *          *** empty log message ***
	 *
	 *          Revision 1.33  2008/04/24 14:17:37  heine_
	 *          Improved timeslot search when clicking and moving
	 *
	 *          Revision 1.32  2008/04/08 13:17:53  heine_
	 *          *** empty log message ***
	 *
	 *          Revision 1.31  2008/03/28 08:45:11  heine_
	 *          *** empty log message ***
	 *
	 *          Revision 1.30  2008/03/21 15:02:35  heine_
	 *          fixed problem when selecting lasso area in a region that was in the bottom of the panel.
	 *
	 *          Removed all the evil getBounds() statements. Should run fast now and use lesser heap.
	 *
	 *          Revision 1.29  2008/01/21 14:13:55  heine_
	 *          fixed nullpointer problem when refreshing without a model.
	 *          The refresh method just returns in case of this
	 *
	 *          Revision 1.24  2008-01-21 14:06:11  heinemann
	 *          fixed nullpointer problem when refreshing without a model.
	 *          The refresh method just returns in case of this.
	 *
	 *          Revision 1.23  2007-09-18 12:39:57  heinemann
	 *          *** empty log message ***
	 *
	 *          Revision 1.22  2007/07/09 07:30:08  heinemann
	 *          *** empty log message ***
	 *
	 *          Revision 1.21  2007/07/09 07:16:47  heinemann
	 *          *** empty log message ***
	 *
	 *          Revision 1.20  2007/06/20 12:08:08  heinemann
	 *          *** empty log message ***
	 *
	 *          Revision 1.19  2007/06/18 11:41:32  heinemann
	 *          bug fixes and alpha optimations
	 *
	 *          Revision 1.18  2007/06/15 07:00:38  hermen
	 *          changed translatrix keys
	 *
	 *          Revision 1.17  2007/06/14 13:31:25  heinemann
	 *          *** empty log message ***
	 *
	 *          Revision 1.16  2007/06/12 11:58:03  heinemann
	 *          *** empty log message ***
	 *
	 *          Revision 1.15  2007/06/11 13:23:39  heinemann
	 *          *** empty log message ***
	 *
	 *          Revision 1.14  2007/06/08 12:21:10  heinemann
	 *          *** empty log message ***
	 *
	 *          Revision 1.13  2007/06/07 12:12:50  heinemann
	 *          Events that lasts longer than a day and have at least one overlapping, will now have the same width for all FrameAreas in the columns
	 * <br>
	 *          Revision 1.12 2007/06/06 11:23:01 heinemann <br>
	 *          *** empty log message *** <br>
	 *
	 */
	private class Layout implements LayoutManager {
		public void addLayoutComponent(String name, Component comp) {
		}

		public void removeLayoutComponent(Component comp) {
		}

		public Dimension preferredLayoutSize(Container parent) {
			try {
				int width = dayCount * getModel().getSelectedCalendars().size()
						* PREFERRED_DAY_WIDTH;
				return new Dimension(width, getPreferredHeight());
			} catch (Exception e) {
				throw BizcalException.create(e);
			}
		}

		public Dimension minimumLayoutSize(Container parent) {
			return new Dimension(50, 100);
		}

		@SuppressWarnings("unchecked")
		public void layoutContainer(Container parent0) {
			/* ================================================== */
			try {
				DayView.this.resetVerticalLines();
				int width 	= getWidth();
				int height 	= getHeight();
				DateInterval day = getFirstInterval();

				int numberOfCols = getColCount();
				if (numberOfCols == 0)
					numberOfCols = 1;

				/* ------------------------------------------------------- */
				// iterate over all columns (a column per day)
				/* ------------------------------------------------------- */
				for (int i = 0; i < eventColList.size(); i++) {
					/* ------------------------------------------------------- */
					int dayNo = i % dayCount;
					int xpos = getXPos(i);
					int captionYOffset = getCaptionRowHeight() - CAPTION_ROW_HEIGHT0;
					int colWidth = getXPos(i + 1) - getXPos(i);
					// Obs. tempor�r l�sning med korrigering med +2. L�gg till
					// korrigeringen p� r�tt st�lle
					// kan h�ra ihop synkning av tidsaxel och muslyssnare
					int vLineTop = captionYOffset + CAPTION_ROW_HEIGHT0 + 2;
					if (dayNo == 0 && (getSelectedCalendars().size() > 1)) {
						vLineTop = 0;
						day = getFirstInterval();
					}
					
//					Calendar startinterv = Calendar.getInstance(Locale.getDefault());
//					startinterv.setTime(day.getStartDate());
					
					/* ------------------------------------------------------- */
					//
					/* ------------------------------------------------------- */
					if (i > 0) {
						JLabel verticalLine = vLines.get(i-1);
						int vLineHeight = height - vLineTop;
						verticalLine.setBounds(xpos, vLineTop, 1, vLineHeight);
						// add the line position to the list
						addVerticalLine(verticalLine);
					}
					/* ------------------------------------------------------- */
					// show a footer. Haven't seen it working....
					/* ------------------------------------------------------- */
					if (config.isShowDateFooter()) {
						JLabel dayFooter = dateFooters.get(i);
						dayFooter.setBounds(xpos, getTimeHeight(), colWidth, getFooterHeight());
					}
					/* ------------------------------------------------------- */
					// get the date interval for the current day
					/* ------------------------------------------------------- */
					DateInterval currIntervall = getInterval(dayNo);
					FrameArea previousArea = null;
					/* ------------------------------------------------------- */
					// this indicates the position of the event inside the day-column
					// if it is overlapping with other events
					/* ------------------------------------------------------- */
					int overlapCol 		= 0;
					/* ------------------------------------------------------- */
					// this is the total amount of columns inside a day-column.
					// Overlapping events will be painted in columns inside the day-column
					/* ------------------------------------------------------- */
					int overlapColCount = 0;
					// ======================================================
					// eventColList contains a list of ArrayLists that holds the
					// events per day
					// the same with the frameAreaCols
					// =======================================================
					List<Event> events	  = eventColList.get(i);
					List<FrameArea> areas = frameAreaCols.get(i);
					/* ------------------------------------------------------- */
					int overlapCols[] = new int[events.size()];
					// for each event of the day
					for (int j = 0; j < events.size(); j++) {
						/* ------------------------------------------------------- */
						FrameArea area = areas.get(j);
						Event event    = events.get(j);
						// adapt the FrameArea according the appropriate event
						// data
						Date startTime = event.getStart();
						// if the starttime is before the displayable time, we take the first displayable time
						if (startTime.before(currIntervall.getStartDate()))
							startTime = currIntervall.getStartDate();
						/* ------------------------------------------------------- */
						Date endTime = event.getEnd();
						// if the events lasts longer than the current day, set
						// 23:59 as end
						if (endTime.after(currIntervall.getEndDate()))
							endTime = currIntervall.getEndDate();
						/* ------------------------------------------------------- */
						// compute the new bounds of the framearea
						/* ------------------------------------------------------- */
						// get the ypos for the start time
						int y1 = getYPos(startTime, dayNo);
						if (y1 < getCaptionRowHeight())
							y1 = getCaptionRowHeight();
						// get the y position for the end time
						int y2 = getYPos(endTime, dayNo);
						
						int dHeight = y2 - y1;
						int x1 = xpos;
						area.setBounds(x1, y1, colWidth, dHeight);
						/* ------------------------------------------------------- */
						// Overlap logic
						// 
						// overlapping works only for events that are not
						// in the background
						/* ------------------------------------------------------- */
						if (!event.isBackground()) {
							/* ------------------------------------------------------- */
							if (previousArea != null) {
								/* ------------------------------------------------------- */
								int previousY2 = previousArea.getY() + previousArea.getHeight();
								// if the previous ends after the current starts
								if (previousY2 > y1) {
									// Previous event overlap
									overlapCol++;
									if (previousY2 < y2) {
										/* ------------------------------------------------------- */
										// This events ends after the previous
										/* ------------------------------------------------------- */
										previousArea = area; 
										/* ------------------------------------------------------- */
									}
								} else {
									/* ------------------------------------------------------- */
									// set the overlap column to 0. this is the column in which the 
									// overlap event will be painted afterwards.
									/* ------------------------------------------------------- */
									overlapCol = 0;
									previousArea = area;
									/* ------------------------------------------------------- */
								}
								/* ------------------------------------------------------- */
							}  else {
								previousArea = area;
//								overlapCols[j] = 0;
							}
							// store the column position for the overlapping event
							overlapCols[j] = overlapCol;
							
							if (overlapCol > overlapColCount)
								overlapColCount = overlapCol;
							/* ------------------------------------------------------- */
						} 
						else
							overlapCols[j] = -1;
					}
					/* ------------------------------------------------------- */
						// Overlap logic. Loop the events/frameareas a second
						// time and set the xpos and widths
					/* ------------------------------------------------------- */
					if (overlapColCount > 0) {
						/* ------------------------------------------------------- */
						int currWidth = colWidth;
						for (int k = 0; k < areas.size(); k++) {
							/* ------------------------------------------------------- */
							Event event = events.get(k);
							/* ------------------------------------------------------- */
							if (event.isBackground())
								continue;
							/* ------------------------------------------------------- */
							FrameArea area = areas.get(k);
							int overlapIndex = overlapCols[k];
							if (overlapIndex == 0)
								currWidth = colWidth;
							/* ------------------------------------------------------- */
							try {
								/* ------------------------------------------------------- */
								int kOffset = 1;
								while (events.get(k+kOffset).isBackground())
									kOffset++;
								
								if ( overlapCols[k+kOffset] > 0){
									// find biggest in line
									int curr = overlapIndex;
									for (int a = k+1; a < areas.size();a++) {
										/* ------------------------------------------------------- */
										if (overlapCols[a] == 0)
//											break;
											continue;
										if (overlapCols[a] > curr)
											curr = overlapCols[a];
										/* ------------------------------------------------------- */
									}
									currWidth = colWidth / (curr+1);
								}
							} catch (Exception e) {}
							/* ------------------------------------------------------- */
							area.setBounds(area.getX() + overlapIndex*currWidth, area.getY(), currWidth, area.getHeight());
						}
					}
				}
				/* ------------------------------------------------------- */
				// Loop the frameareas a third time
				// and set areas that belong to an event to the same width
				/* ------------------------------------------------------- */
				for (List<FrameArea> fAreas : frameAreaCols) {
					/* ------------------------------------------------------- */
					if (fAreas != null)
						for (FrameArea fa : fAreas) {
							/* ------------------------------------------------------- */
							int sw = findSmallestFrameArea(fa);
							int baseFAWidth;
							try {
								baseFAWidth = getBaseFrameArea(fa.getEvent()).getWidth();
							} catch (Exception e) {
								continue;
							}
							if (sw > baseFAWidth) {
								sw = baseFAWidth;
							}
							fa.setBounds(fa.getX(), fa.getY(),
									sw,
									fa.getHeight());
							/* ------------------------------------------------------- */
							// ensure, that the background events are really painted in the background!
							/* ------------------------------------------------------- */
							try {
								/* --------------------------------------------- */
								if (fa.getEvent().isBackground())
									calPanel.setComponentZOrder(fa, calPanel.getComponents().length-5);
								/* --------------------------------------------- */
							} catch (Exception e) {
//								e.printStackTrace();
							}
							/* ------------------------------------------------------- */
						}
					/* ------------------------------------------------------- */
				}


					// old obsolete
// // Overlap logic. Loop the events/frameareas a second
// // time and set the xpos and widths
// if (overlapColCount > 0) {
// int slotWidth = colWidth / (overlapColCount+1);
// for (int j = 0; j < areas.size(); j++) {
// Event event = (Event) events.get(j);
// if (event.isBackground())
// continue;
// FrameArea area = (FrameArea) areas.get(j);
// int index = overlapCols[j];
// Rectangle r = area.getBounds();
// area.setBounds(r.x + index*slotWidth, r.y, slotWidth, r.height);
// }
// }
				/* ================================================== */
				// set up the line to minute mapping hashmap.
				// we create a hashmap of pixel to minute mapping to
				// have a fixed resource for resolving the explicit time 
				// for a position on the calendar panel
				/* ================================================== */
				if (dayCount > 1)
					day = incDay(day);

				/* ------------------------------------------------------- */
				// iterate through all time lines
				/* ------------------------------------------------------- */
				for (Tuple key : timeLines.keySet()) {
					/* ------------------------------------------------------- */
					// get the day of the line
					/* ------------------------------------------------------- */
					Date date = (Date) key.elementAt(0);
					/* ------------------------------------------------------- */
					// extract the minutes from the string
					/* ------------------------------------------------------- */
					int minutes = Integer.parseInt((String) key.elementAt(1));
					/* ------------------------------------------------------- */
					JLabel line = timeLines.get(key);
					Date date1 = new Date(date.getTime() + ((long) minutes) * 60 * 1000);
					
					int y1 = getYPos(date1, 0);
					
					linePositionMap.put(date1, y1);
					
					int x1 = 0;
					int lineheight = 1;
					if (minutes > 0) {
						// x1 = 25;
						lineheight = 1;
					}
					line.setBounds(x1, y1, width, lineheight);
					/* ------------------------------------------------------- */
				}
				/* ------------------------------------------------------- */
				// build up the hash for minute to pixel mapping
				/* ------------------------------------------------------- */
				// get the dates of the lines and sort them
				/* ------------------------------------------------------- */
				List<Date> lines = new ArrayList<Date>(linePositionMap.keySet());
				/* ------------------------------------------------------- */
				// add the first, there is no line!
				/* ------------------------------------------------------- */
//				minuteMapping.put(0, getFirstInterval().getStartDate());
				linePositionMap.put(getFirstInterval().getStartDate(), 0);
				Collections.sort(lines);
				/* ------------------------------------------------------- */
				int linesPerHour = config.getNumberOfTimeSlots();
				for (int i = 0; i < lines.size(); i++) {
					/* ------------------------------------------------------- */
					// get the date for the position
					/* ------------------------------------------------------- */
					Date currDate = lines.get(i);
					/* ------------------------------------------------------- */
					// get the position for that date
					/* ------------------------------------------------------- */
					int currPos = linePositionMap.get(currDate);
					/* ------------------------------------------------------- */
					// get the position of the next date
					/* ------------------------------------------------------- */
					int nextPos = 0;
					if (i+1 < lines.size()) {
						Date nextDate = lines.get(i+1);
						nextPos   = linePositionMap.get(nextDate);
					} else
						nextPos = getTimeHeight();
					/* ------------------------------------------------------- */
					// div the height of one timeslot
					/* ------------------------------------------------------- */
					int slotHeight = nextPos - currPos;
					int numberOfMinutesPerSlot = 60 / linesPerHour;
					/* ------------------------------------------------------- */
					// compute the number of pixels for one minute
					/* ------------------------------------------------------- */
					int pixelsPerMinute = slotHeight / numberOfMinutesPerSlot;
					/* ------------------------------------------------------- */
					// add the minute->pixel mapping
					/* ------------------------------------------------------- */
					minuteMapping.put(currPos, currDate);
					int startMinute = DateUtil.getMinuteOfHour(currDate);
					
					for (int k = 1; k < numberOfMinutesPerSlot; k++) {
						
						minuteMapping.put(currPos + k*pixelsPerMinute, DateUtil.round2Minute(currDate, startMinute + k));
					}
					/* ------------------------------------------------------- */
				}
				/* ------------------------------------------------------- */
				// DEBUG print minuteMapping
				/* ------------------------------------------------------- */
//				List<Integer> minList = new ArrayList<Integer>(minuteMapping.keySet());
//				Collections.sort(minList);
//				for (Integer in : minList)
//					System.out.println("Key: " + in + " => " + minuteMapping.get(in));
				
				/* ------------------------------------------------------- */
				for (int iCal = 0; iCal < calBackgrounds.size(); iCal++) {
					/* ------------------------------------------------------- */
					int x1 = getXPos(iCal * dayCount);
					int x2 = getXPos((iCal + 1) * dayCount);
					JPanel calBackground = calBackgrounds.get(iCal);
					calBackground.setBounds(x1, getCaptionRowHeight(), x2 - x1,	getHeight());
					/* ------------------------------------------------------- */
				}
			} catch (Exception e) {
				throw BizcalException.create(e);
			}
		}
		/* ================================================== */
	}

	
	
	private void layoutNew(List<Event> eventList, List<FrameArea> areaList) {
		/* ================================================== */
		//		currCol
		//		colsInRow
		//		currWidth=width/colsInRow
		/* ------------------------------------------------------- */
		// Map to store the column position for each event
		HashMap<Event, Integer> colPositionMap = new HashMap<Event, Integer>();
		// Map to store the amount of events, that are painted in a row
		// neccessary to get the right width of each event
		HashMap<Event, Integer> colsInRowMap   = new HashMap<Event, Integer>();
		/* ------------------------------------------------------- */
		
		
		/* ================================================== */	
	}
	
	
	
	/**
	 * Finds the smallest width of a framearea and its children
	 *
	 * @param fa
	 * @return
	 */
	private int findSmallestFrameArea(FrameArea fa) {
		/* ================================================== */
		if (fa.getChildren() == null || fa.getChildren().size() < 1)
			return fa.getWidth();
		
		int smallest = fa.getWidth();
		for (FrameArea child : fa.getChildren()) {
			if (child.getWidth() < smallest)
				smallest = child.getWidth();
		}
		return smallest;
		
		/* ================================================== */
	}

	protected Object getCalendarId(int x, int y) throws Exception {
		return getCalendarId(getColumn(x));
	}

	/**
	 * returns the day view config object.
	 * If none is specified, it will create a default.
	 * 
	 * @return
	 * @throws Exception
	 */
	private DayViewConfig getDayViewConfig() throws Exception {
		/* ================================================== */
		DayViewConfig result = (DayViewConfig) getDescriptor();
		if (result == null) {
			result = new DayViewConfig();
			setDescriptor(result);
		}
		return result;
		/* ================================================== */
	}

//	public DayViewConfig getDayViewConfig() throws Exception {
//		return getDesc();
//	}

	protected int getInitYPos() throws Exception {
		double viewStart = getModel().getViewStart().getValue();
		double ratio = viewStart / (24 * 3600 * 1000);
		return (int) (ratio * this.config.getHours() * PIXELS_PER_HOUR);

//		double viewStart = getModel().getViewStart().getValue();
//		double ratio = viewStart / (24 * 3600 * 1000);
//		return (int) (ratio * 24 * PIXELS_PER_HOUR);

	}

	private int getPreferredHeight() {

		return this.config.getHours() * PIXELS_PER_HOUR + getFooterHeight();
	}

	public JComponent getComponent() {
		return scrollPane;
	}

	public void initScroll() throws Exception {
		scrollPane.getViewport().setViewPosition(new Point(0, getInitYPos()));
	}

	public void addListener(CalendarListener listener) {
		super.addListener(listener);
		columnHeader.addCalendarListener(listener);
	}

}
