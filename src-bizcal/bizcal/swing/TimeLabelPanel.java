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
import java.awt.Font;
import java.awt.LayoutManager;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import bizcal.common.CalendarViewConfig;
import bizcal.swing.util.GradientArea;
import bizcal.util.BizcalException;
import bizcal.util.TimeOfDay;

/**
 * 
 * Class to paint the time labels on the border of the calendar grid.
 * 
 * @author martin.heinemann@tudor.lu
 * 27.03.2008
 * 10:45:06
 *
 *
 * @version
 * <br>$Log: TimeLabelPanel.java,v $
 * <br>Revision 1.7  2008/06/12 13:04:18  heine_
 * <br>*** empty log message ***
 * <br>
 * <br>Revision 1.6  2008/03/28 08:45:11  heine_
 * <br>*** empty log message ***
 * <br>
 *   
 */
public class TimeLabelPanel
{
	private JPanel panel;
	private List<JLabel> hourLabels = new ArrayList<JLabel>();
	private List<JLabel> minuteLabels = new ArrayList<JLabel>();
	private List<JLabel> hourLines = new ArrayList<JLabel>();
	private List<JLabel> minuteLines = new ArrayList<JLabel>();
	private Font font = new Font("Verdana", Font.PLAIN, 11);
	private GradientArea gradientArea;
	private int width = 40;
	private int hourCount;
	private int footerHeight = 0;
	private CalendarViewConfig config;
	private TimeOfDay start;
	private TimeOfDay end;
	private SimpleDateFormat hourFormat;
	private Font hourFont;

	public TimeLabelPanel(CalendarViewConfig config, TimeOfDay start, TimeOfDay end) throws Exception {
		/* ================================================== */
		this.config = config;
		this.start = start;
		this.end = end;
		/* ------------------------------------------------------- */
		panel = new JPanel();
		panel.setLayout(new Layout());

		this.hourFormat = new SimpleDateFormat("HH");
		hourFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

		this.hourFont = font.deriveFont((float) 12);
		hourFont = hourFont.deriveFont(Font.BOLD);
		/* ------------------------------------------------------- */
		refresh();
		/* ================================================== */
	}


	public void refresh() {
		/* ================================================== */
		try {
			hourCount = end.getHour() - start.getHour();

			if (hourCount == 0)
				hourCount = 24;
			/* ------------------------------------------------------- */
			// clear arrays
			hourLabels.clear();
			minuteLabels.clear();
			hourLines.clear();
			minuteLines.clear();
			/* ------------------------------------------------------- */
			// remove all elemtns from panel
			panel.removeAll();
			/* ------------------------------------------------------- */
			long pos = start.getValue();
	 		while (pos < end.getValue()) {
				Date date = new Date(pos);
				String timeTxt = hourFormat.format(date);
				JLabel timeLabel = new JLabel(timeTxt);
				timeLabel.setVerticalTextPosition(JLabel.CENTER);
				timeLabel.setFont(hourFont);
				panel.add(timeLabel);
				hourLabels.add(timeLabel);
				JLabel line = new JLabel();
				line.setBackground(this.config.getLineColor());
				line.setOpaque(true);
				hourLines.add(line);
				panel.add(line);
				

				timeTxt = "15";
				timeLabel = new JLabel(timeTxt);
				timeLabel.setFont(font);
				panel.add(timeLabel);
				minuteLabels.add(timeLabel);
//				line = new JLabel();
//				line.setBackground(this.config.getLineColor());
//				line.setOpaque(true);
//				minuteLines.add(line);
//				panel.add(line);
				createMinuteLine();
				createMinuteLine();

				timeTxt = "45";
				timeLabel = new JLabel(timeTxt);
				timeLabel.setFont(font);
				panel.add(timeLabel);
				minuteLabels.add(timeLabel);
//				line = new JLabel();
////				line.setBackground(this.config.getLineColor());
//				line.setBackground(Color.RED);
//				line.setOpaque(true);
//				minuteLines.add(line);
//				panel.add(line);
				createMinuteLine();
				createMinuteLine();

				pos += 3600 * 1000;
			}
	        gradientArea = new GradientArea(GradientArea.LEFT_RIGHT, Color.WHITE,
	        		ColumnHeaderPanel.GRADIENT_COLOR);
	        gradientArea.setOpaque(true);
			gradientArea.setBorder(false);
			panel.add(gradientArea);


			panel.validate();
			panel.updateUI();

		} catch (Exception e) {
			e.printStackTrace();
		}
		/* ================================================== */
	}

	
	/**
	 * Creates a new JLabel for a line and adds it to the panel
	 */
	private void createMinuteLine() {
		/* ================================================== */
		JLabel line = new JLabel();
		line.setBackground(this.config.getLineColor());
		line.setOpaque(true);
		minuteLines.add(line);
		panel.add(line);
		/* ================================================== */
	}
	
	
	/**
	 * Sets the start end end interval.
	 * A refresh is made automatically
	 *
	 * @param start
	 * @param end
	 */
	public void setStartEnd(TimeOfDay start, TimeOfDay end) {
		/* ================================================== */
		this.start = start;
		this.end = end;
		refresh();
		/* ================================================== */
	}

	private int getPreferredHeight()
	{
		return DayView.PIXELS_PER_HOUR * hourCount + footerHeight;
	}

	private class Layout implements LayoutManager {
		public void addLayoutComponent(String name, Component comp) {
		}

		public void removeLayoutComponent(Component comp) {
		}

		public Dimension preferredLayoutSize(Container parent) {
			return new Dimension(width, getPreferredHeight());
		}

		public Dimension minimumLayoutSize(Container parent) {
			return new Dimension(50, 50);
		}

		public void layoutContainer(Container parent) {
			try {
				double totHeight = parent.getHeight() - footerHeight;
				double rowHeight = totHeight / hourCount;
				double minuteRowHeight = rowHeight / 2;
				int colWidth = width / 2;
				int iMinute = 0;
				int iLine  = 0;
				for (int i=0; i < hourLabels.size(); i++) {
					/* ------------------------------------------------------- */
					// layout the hour labels
					/* ------------------------------------------------------- */
					JLabel hourLabel = (JLabel) hourLabels.get(i);
					hourLabel.setBounds(0,
							(int) (i*rowHeight),
							colWidth,
							(int) rowHeight);
					/* ------------------------------------------------------- */
					// layout the hour lines
					/* ------------------------------------------------------- */
					JLabel hourLine = (JLabel) hourLines.get(i);
					hourLine.setBounds(0,
							(int) ((i+1)*rowHeight),
							width,
							1);
					/* ------------------------------------------------------- */
					// layout the first minute label
					/* ------------------------------------------------------- */
					JLabel minuteLabel = (JLabel) minuteLabels.get(iMinute);
					minuteLabel.setBounds(colWidth,
							(int) (i*rowHeight),
							colWidth,
							(int) (minuteRowHeight));
					iMinute++;
					/* ------------------------------------------------------- */
					// the minute line for the 30 min
					/* ------------------------------------------------------- */
					JLabel minuteLine = (JLabel) minuteLines.get(iLine);
//					
					minuteLine.setBounds(colWidth,
							(int) (i*rowHeight + minuteRowHeight),
							colWidth,
							1);
					iLine++;
					/* ------------------------------------------------------- */
					// line for 15
					/* ------------------------------------------------------- */
					JLabel minuteLine2 = (JLabel) minuteLines.get(iLine);
					
					minuteLine2.setBounds(colWidth*2-4,
							(int) (i*rowHeight + minuteRowHeight/2),
							colWidth,
							1);
					
					iLine++;
					/* ------------------------------------------------------- */
					// the minute label for 45 min
					/* ------------------------------------------------------- */
					minuteLabel = (JLabel) minuteLabels.get(iMinute);
					minuteLabel.setBounds(colWidth,
							(int) (i*rowHeight + minuteRowHeight),
							colWidth,
							(int) minuteRowHeight);
					iMinute++;
					/* ------------------------------------------------------- */
					// line for 45
					/* ------------------------------------------------------- */
					JLabel minuteLine3 = (JLabel) minuteLines.get(iLine);
					
					minuteLine3.setBounds(colWidth*2-4,
							(int) (i*rowHeight + minuteRowHeight + minuteRowHeight/2),
							colWidth,
							1);
					
					iLine++;
					/* ------------------------------------------------------- */
				}
				gradientArea.setBounds(0, 0, parent.getWidth(), parent.getHeight());
			} catch (Exception e) {
				e.printStackTrace();
//				throw BizcalException.create(e);
			}
		}
	}

	public JComponent getComponent()
	{
		return panel;
	}

	public void setFooterHeight(int footerHeight) {
		this.footerHeight = footerHeight;
	}
}
