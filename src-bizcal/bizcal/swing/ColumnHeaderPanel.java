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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.LayoutManager;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import bizcal.common.CalendarModel;
import bizcal.common.CalendarViewConfig;
import bizcal.swing.util.ErrorHandler;
import bizcal.swing.util.GradientArea;
import bizcal.swing.util.ResourceIcon;
import bizcal.swing.util.TrueGridLayout;
import bizcal.util.BizcalException;
import bizcal.util.DateUtil;
import bizcal.util.LocaleBroker;
import bizcal.util.TextUtil;

public class ColumnHeaderPanel
{
	public static final Color GRADIENT_COLOR = new Color(230, 230, 230);
	private PopupMenuCallback popupMenuCallback;
	private JPanel panel;
	private List calHeaders = new ArrayList();
	private List dateHeaders = new ArrayList();
	private List dateHeaders2 = new ArrayList();
	private List dateList = new ArrayList();
	private List dateLines = new ArrayList();
	private GradientArea gradientArea;
	private JLabel refLabel = new JLabel("AAA");
	private int rowCount;
	private int dayCount;
	private CalendarModel model;
	private Color lineColor = Color.LIGHT_GRAY;
	private int fixedDayCount = -1;
	private CalendarListener listener;
	private boolean showExtraDateHeaders = false;
	private CalendarViewConfig config;
	private boolean isMonthView = false;

//	// formater for month view
//	DateFormat monthDateFormat = new SimpleDateFormat("EEEE",
//			LocaleBroker.getLocale());
//	// formater for week view
//	DateFormat weekDateFormat = new SimpleDateFormat("EE - dd.MM.",
//			LocaleBroker.getLocale());
//	// formater for day view
//	DateFormat dayFormat = new SimpleDateFormat("EEEE dd.MM.yyyy",
//			LocaleBroker.getLocale());


	public ColumnHeaderPanel(CalendarViewConfig config)
	{
		this.config = config;
		panel = new JPanel();
		panel.setLayout(new Layout());
		gradientArea = new GradientArea(
				GradientArea.TOP_BOTTOM, Color.WHITE, GRADIENT_COLOR);
		gradientArea.setBorder(false);
	}

	public ColumnHeaderPanel(CalendarViewConfig config, int fixedDayCount)
	{
		this(config);
		this.fixedDayCount = fixedDayCount;
	}

	@SuppressWarnings("unchecked")
	public void refresh()
		throws Exception
	{
		calHeaders.clear();
		dateHeaders.clear();
		dateHeaders2.clear();
		dateList.clear();
		dateLines.clear();
		panel.removeAll();

		Calendar calendar = DateUtil.newCalendar();
		dayCount = DateUtil.getDateDiff(model.getInterval().getEndDate(),
				model.getInterval().getStartDate());
		if (fixedDayCount > 0)
			dayCount = fixedDayCount;

		int calCount = model.getSelectedCalendars().size();
		if (dayCount >= 1 || calCount > 1) {
			if (dayCount > 1 && calCount > 1)
				rowCount = 2;
			else
				rowCount = 1;
			DateFormat toolTipFormat = new SimpleDateFormat("EEEE d MMMM",
					LocaleBroker.getLocale());

//			DateFormat dateFormat = new SimpleDateFormat("EE - d MMMM yyyy",
//					LocaleBroker.getLocale());
//			DateFormat longDateFormat = new SimpleDateFormat("EEEE - d MMMM yyyy",
//					LocaleBroker.getLocale());
//
//			DateFormat shortDateFormat =
//				DateFormat.getDateInstance(DateFormat.SHORT, LocaleBroker.getLocale());

			// TODO ??????
//			if (dayCount == 5 || dayCount == 7) {
//			}
			for (int j = 0; j < calCount; j++) {
				bizcal.common.Calendar cal = (bizcal.common.Calendar) model
						.getSelectedCalendars().get(j);
				if (calCount >= 1) {
					JLabel headerLabel = new JLabel(cal.getSummary(), JLabel.CENTER);
					headerLabel.addMouseListener(new CalHeaderMouseListener(cal
							.getId()));
					headerLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
					JComponent header = headerLabel;
					if (cal.isCloseable()) {
						JPanel panel = new JPanel();
						panel.setOpaque(false);
						panel.setLayout(new BorderLayout());
						panel.add(headerLabel, BorderLayout.CENTER);
						JLabel iconLabel = new JLabel(new ResourceIcon("/bizcal/res/cancel.gif"));
						iconLabel.addMouseListener(new CloseListener(cal.getId()));
						panel.add(iconLabel, BorderLayout.EAST);
						header = panel;
					}
					calHeaders.add(header);
					panel.add(header);
				}
				JPanel dateHeaderPanel = new JPanel();
				dateHeaderPanel.setLayout(new TrueGridLayout(1, dayCount));
				dateHeaderPanel.setOpaque(false);
				Date date = model.getInterval().getStartDate();
				if (fixedDayCount > 0)
					date = DateUtil.round2Week(date);
				for (int i = 0; i < dayCount; i++) {
					/* ------------------------------------------------------- */
					// ==========================================================
					// Bullshit here.
					// the final text of the column is set in the method
					// resizeDates(int width)  !!!!!!!!!!!!!
					// ===========================================================
					//
					String dateStr = "";
					if (dayCount == 1)
						dateStr = TextUtil.formatCase(config.getDayFormat().format(date));
					else if (isMonthView)
						dateStr = config.getMonthDateFormat().format(date);
					else
						dateStr = config.getWeekDateFormat().format(date);
					/* ------------------------------------------------------- */
					JLabel header = new JLabel(dateStr, JLabel.CENTER);
					header.setAlignmentY(2);
					//header.setFont(font);
					header.setToolTipText(toolTipFormat.format(date));

					if (model.isRedDay(date))
						header.setForeground(Color.RED);
					dateHeaders.add(header);
					panel.add(header);
					if (showExtraDateHeaders) {
						JLabel header2 = new JLabel(model.getDateHeader(cal.getId(), date), JLabel.CENTER);
						dateHeaders2.add(header2);
						panel.add(header2);
					}
					dateList.add(date);
					if (i > 0 || j > 0) {
						JLabel line = new JLabel();
						line.setBackground(lineColor);
						line.setOpaque(true);
						line.setBackground(lineColor);
						if (DateUtil.getDayOfWeek(date) == calendar.getFirstDayOfWeek())
							line.setBackground(config.getLineColor2());
						if (model.getSelectedCalendars().size() > 1 && i == 0)
							line.setBackground(config.getLineColor3());

						panel.add(line);
						dateLines.add(line);
					}
					date = DateUtil.getDiffDay(date, +1);
				}
			}
		} else
			rowCount = 0;

		if (showExtraDateHeaders)
			rowCount++;

		panel.add(gradientArea);
		panel.updateUI();
	}

	public JComponent getComponent()
	{
		return panel;
	}

	protected class CalHeaderMouseListener extends MouseAdapter {
		private Object calId;

		public CalHeaderMouseListener(Object calId) {
			this.calId = calId;
		}

		public void mousePressed(MouseEvent e) {
			maybeShowPopup(e);
		}

		public void mouseReleased(MouseEvent e) {
			maybeShowPopup(e);
		}

		private void maybeShowPopup(MouseEvent e) {
			try {
				if (e.isPopupTrigger()) {
					JPopupMenu popup = popupMenuCallback
							.getCalendarPopupMenu(calId);
					if (popup == null)
						return;
					popup.show(e.getComponent(), e.getX(), e.getY());
				}
			} catch (Exception exc) {
				throw BizcalException.create(exc);
			}
		}

		public void mouseEntered(MouseEvent e) {
			//rootPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
		}

		public void mouseExited(MouseEvent e) {
			//rootPanel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		}
	}

	private class Layout implements LayoutManager {
		public void addLayoutComponent(String name, Component comp) {
		}

		public void removeLayoutComponent(Component comp) {
		}

		public Dimension preferredLayoutSize(Container parent) {
			try {
				int height = refLabel.getPreferredSize().height;
				height = rowCount * height;
				int calenderSize = 1;
				if (model != null && model.getSelectedCalendars() != null)
					calenderSize = model.getSelectedCalendars().size();
				/* ---------------------------------------------------- */
				int width = dayCount * calenderSize * DayView.PREFERRED_DAY_WIDTH;
				return new Dimension(width, height);
			} catch (Exception e) {
				throw BizcalException.create(e);
			}
		}

		public Dimension minimumLayoutSize(Container parent) {
			return new Dimension(50, 100);
		}

		public void layoutContainer(Container parent) {
			try {
				if (rowCount == 0)
					return;
				double totWidth = parent.getWidth();
				double dateColWidth = totWidth / dateHeaders.size();
				double calColWidth = totWidth / calHeaders.size();
				double rowHeight = parent.getHeight() / rowCount;
				double dateYPos = 0;
				if (calHeaders.size() > 0)
					dateYPos = rowHeight;
				int dateI = 0;
				int dateLineI = 0;
				int dayRowCount = showExtraDateHeaders ? 2 : 1;
				for (int i=0; i < model.getSelectedCalendars().size(); i++) {
					if (calHeaders.size() > 0) {
						JComponent label = (JComponent) calHeaders.get(i);
						label.setBounds((int) (i*calColWidth),
								0,
								(int) calColWidth,
								(int) rowHeight);
					}
					if (dayCount >= 1) {
						for (int j=0; j < dayCount; j++) {
							JLabel dateLabel = (JLabel) dateHeaders.get(dateI);
							int xpos = (int) (dateI*dateColWidth);
							dateLabel.setBounds(xpos,
									(int) dateYPos,
									(int) dateColWidth,
									(int) rowHeight);
							if (showExtraDateHeaders) {
								dateLabel = (JLabel) dateHeaders2.get(dateI);
								dateLabel.setBounds(xpos,
										(int) (dateYPos + rowHeight),
										(int) dateColWidth,
										(int) rowHeight);
							}
							if (j > 0 || i > 0) {
								JLabel line = (JLabel) dateLines.get(dateLineI);
								int ypos = (int) dateYPos;
								int height = (int) rowHeight * dayRowCount;
								if (j == 0) {
									ypos = 0;
									height = (int) (rowHeight*(dayRowCount+1));
								}
								line.setBounds(xpos,
										ypos,
										1,
										height);
								dateLineI++;
							}
							dateI++;
						}
					}
				}
				gradientArea.setBounds(0, 0, parent.getWidth(), parent.getHeight());
				resizeDates((int) dateColWidth);
			} catch (Exception e) {
				throw BizcalException.create(e);
			}
		}
	}

	private void resizeDates(int width)
		throws Exception
	{
		if (dayCount != 5 && dayCount != 7)
			return;

		Date today = DateUtil.round2Day(new Date());

		FontMetrics metrics = refLabel.getFontMetrics(refLabel.getFont());
		int charCount = 10;
		if (maxWidth(charCount, metrics) > width) {
			charCount = 3;
			if (maxWidth(charCount, metrics) > width) {
				charCount = 2;
				if (maxWidth(charCount, metrics) > width) {
					charCount = 1;
				}
			}
		}

		for (int i=0; i < dateHeaders.size(); i++) {
			/* ------------------------------------------------------- */
			JLabel label = (JLabel) dateHeaders.get(i);
			Date date = (Date) dateList.get(i);
			String str = "";
			/* ------------------------------------------------------- */
			if (isMonthView)
				str = config.getMonthDateFormat().format(date);
			else
				str = config.getWeekDateFormat().format(date);
			/* ------------------------------------------------------- */
			if (str.length() > charCount)
				str = str.substring(0, charCount);
			/* ------------------------------------------------------- */
			str = TextUtil.formatCase(str);
			/* ------------------------------------------------------- */
			if (today.equals(DateUtil.round2Day(date)))
				str = "<html><b>" + str + "</b> </html>";
			/* ------------------------------------------------------- */
			label.setText(str);
			/* ------------------------------------------------------- */
		}
	}

	private int maxWidth(int charCount, FontMetrics metrics)
		throws Exception
	{
		DateFormat format = new SimpleDateFormat("EEEEE", LocaleBroker.getLocale());
		Calendar cal = DateUtil.newCalendar();
		cal.set(Calendar.DAY_OF_WEEK, 1);
		int maxWidth = 0;
		for (int i=0; i < 7; i++) {
			String str = format.format(cal.getTime());
			if (str.length() > charCount)
				str = str.substring(0, charCount);
			int width = metrics.stringWidth(str);
			if (width > maxWidth)
				maxWidth = width;
			cal.add(Calendar.DAY_OF_WEEK, +1);
		}
		return maxWidth;
	}

	public void setModel(CalendarModel model) {
		this.model = model;
	}
	public void setPopupMenuCallback(PopupMenuCallback popupMenuCallback) {
		this.popupMenuCallback = popupMenuCallback;
	}

	public void addCalendarListener(CalendarListener listener)
	{
		this.listener = listener;
	}

	private class CloseListener
		extends MouseAdapter
	{
		private Object calId;

		public CloseListener(Object calId)
		{
			this.calId = calId;
		}

		public void mouseClicked(MouseEvent event)
		{
			try {
				listener.closeCalendar(calId);
			} catch (Exception e) {
				ErrorHandler.handleError(e);
			}
		}
	}

	public void setShowExtraDateHeaders(boolean showExtraDateHeaders) {
		this.showExtraDateHeaders = showExtraDateHeaders;
	}

	/**
	 * @return the isMonthView
	 */
	public boolean isMonthView() {
		return isMonthView;
	}

	/**
	 * @param isMonthView the isMonthView to set
	 */
	public void setMonthView(boolean isMonthView) {
		this.isMonthView = isMonthView;
	}

}
