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
import bizcal.swing.util.GradientArea;
import bizcal.swing.util.TrueGridLayout;
import bizcal.util.BizcalException;
import bizcal.util.DateUtil;
import bizcal.util.LocaleBroker;
import bizcal.util.TextUtil;
import bizcal.util.TimeOfDay;

public class DaysHoursHeaderPanel 
{
	public static final Color GRADIENT_COLOR = new Color(230, 230, 230);	
	private PopupMenuCallback popupMenuCallback;
	private JPanel panel;
	private List dateHeaders = new ArrayList();
	private List dateHeaders2 = new ArrayList();
	private List hourHeaders = new ArrayList();
	private List dateList = new ArrayList();
	private List dateLines = new ArrayList();
	private GradientArea gradientArea;
	private JLabel refLabel = new JLabel("AAA");
	private int rowCount;
	private int dayCount;
	private int hourCount;
	private CalendarModel model;
	private int fixedDayCount = -1;
	private boolean showExtraDateHeaders = false;
	private CalendarViewConfig config;
	
	public DaysHoursHeaderPanel(CalendarViewConfig config, CalendarModel model)
	{
		this.config = config;
		this.model = model;
		panel = new JPanel();
		panel.setLayout(new Layout());
		gradientArea = new GradientArea(
				GradientArea.TOP_BOTTOM, Color.WHITE, GRADIENT_COLOR);
		gradientArea.setBorder(false);
	}
		
	public void refresh()
		throws Exception
	{
		dateHeaders.clear();
		dateHeaders2.clear();
		hourHeaders.clear();
		dateList.clear();
		dateLines.clear();
		panel.removeAll();
		
		dayCount = DateUtil.getDateDiff(model.getInterval().getEndDate(),
				model.getInterval().getStartDate());
		if (fixedDayCount > 0)
			dayCount = fixedDayCount;
		
		if (dayCount > 1) {
			rowCount = 1;
			if (dayCount <= 7)
				rowCount++;
			DateFormat toolTipFormat = new SimpleDateFormat("EEEE d MMMM",
					LocaleBroker.getLocale());
			DateFormat dateFormat = 
				DateFormat.getDateInstance(DateFormat.SHORT, LocaleBroker.getLocale());
			DateFormat hourFormat = new SimpleDateFormat("HH"); 
//			if (dayCount == 5 || dayCount == 7) {
//			}
			JPanel dateHeaderPanel = new JPanel();
			dateHeaderPanel.setLayout(new TrueGridLayout(1, dayCount));
			dateHeaderPanel.setOpaque(false);
			Date date = model.getInterval().getStartDate();
			if (fixedDayCount > 0)
				date = DateUtil.round2Week(date);
			for (int i = 0; i < dayCount; i++) {
				String dateStr = dateFormat.format(date);
				JLabel header = new JLabel(dateStr, JLabel.CENTER);
				header.setToolTipText(toolTipFormat.format(date));
				if (model.isRedDay(date))
					header.setForeground(Color.RED);
				dateHeaders.add(header);
				panel.add(header);
				/*if (showExtraDateHeaders) {
					header = new JLabel(model.getDateHeader(cal.getId(), date), JLabel.CENTER);
					dateHeaders2.add(header);
					panel.add(header);
				}*/
				dateList.add(date);
				JLabel line = new JLabel();
				line.setOpaque(true);
				line.setBackground(config.getLineColor2());
				panel.add(line);
				dateLines.add(line);
				if (dayCount <= 7) {
					hourCount = 0;
					long time = config.getStartView().getValue();
					while (time < config.getEndView().getValue()) {					
						dateStr = hourFormat.format(new TimeOfDay(time).getDate(date));
						header = new JLabel(dateStr, JLabel.CENTER);
						dateHeaders2.add(header);
						panel.add(header);
						if (time > config.getStartView().getValue()) {
							line = new JLabel();
							line.setBackground(config.getLineColor());
							line.setOpaque(true);
							panel.add(line);
							dateLines.add(line);
						}					
						time += 3600*1000*GroupView.HOUR_RESOLUTION;
						hourCount += GroupView.HOUR_RESOLUTION;
					}
				}
				date = DateUtil.getDiffDay(date, +1);
			}
		}
		
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
				//int width = dayCount * model.getSelectedCalendars().size() * DayView.PREFERRED_DAY_WIDTH;
				//int width = dayCount * DayView.PREFERRED_DAY_WIDTH;
	        	int width = dayCount * getHourCount() * GroupView.PREFERRED_HOUR_WIDTH; 
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
				double hourWidth = dateColWidth / hourCount;
				double rowHeight = parent.getHeight() / rowCount;
				int dateI = 0;
				int dateI2 = 0;
				int dateLineI = 0;
				int dayRowCount = showExtraDateHeaders ? 2 : 1;
				for (int j=0; j < dayCount; j++) {
					JLabel dateLabel = (JLabel) dateHeaders.get(dateI);
					int xpos = (int) (dateI*dateColWidth);
					dateLabel.setBounds(xpos,
							(int) 0,
							(int) dateColWidth, 
							(int) rowHeight);
					/*if (showExtraDateHeaders) {
						dateLabel = (JLabel) dateHeaders2.get(dateI);
						dateLabel.setBounds(xpos,
								(int) (dateYPos + rowHeight),
								(int) dateColWidth, 
								(int) rowHeight);
					}*/
					JLabel line = (JLabel) dateLines.get(dateLineI);
					int height = (int) rowHeight * dayRowCount;
					int ypos = (int) rowHeight;
					line.setBounds(xpos, 
							0,
							1,
							height*rowCount);
					dateLineI++;						
					if (dayCount <= 7) {
						int hourI = 0;
						long time = config.getStartView().getValue();
						while (time < config.getEndView().getValue()) {					
							dateLabel = (JLabel) dateHeaders2.get(dateI2);
							xpos = (int) (dateI*dateColWidth + hourI*hourWidth);
							dateLabel.setBounds(xpos,
									(int) rowHeight,
									(int) hourWidth*GroupView.HOUR_RESOLUTION, 
									(int) rowHeight);	
							if (time > config.getStartView().getValue()) {
								line = (JLabel) dateLines.get(dateLineI);
								height = (int) rowHeight * dayRowCount;
								ypos = (int) rowHeight;
								line.setBounds(xpos, 
										ypos,
										1,
										height);
								dateLineI++;	
							}
							time += 3600*1000 * GroupView.HOUR_RESOLUTION;
							hourI += GroupView.HOUR_RESOLUTION;
							dateI2++;
						}		
					}
					dateI++;					
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
		DateFormat format = new SimpleDateFormat("EEEEE");
		for (int i=0; i < dateHeaders.size(); i++) {
			JLabel label = (JLabel) dateHeaders.get(i);
			Date date = (Date) dateList.get(i);
			String str = format.format(date);
			if (str.length() > charCount)
				str = str.substring(0, charCount);
			str = TextUtil.formatCase(str);
			if (today.equals(DateUtil.round2Day(date)))
				str = "<html><b>" + str + "</b></html>";
			label.setText(str);
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
	}
	
	public void setShowExtraDateHeaders(boolean showExtraDateHeaders) {
		this.showExtraDateHeaders = showExtraDateHeaders;
	}
	
	private int getHourCount() throws Exception {
		return config.getEndView().getHour()
				- config.getStartView().getHour();
	}
	
	
}
