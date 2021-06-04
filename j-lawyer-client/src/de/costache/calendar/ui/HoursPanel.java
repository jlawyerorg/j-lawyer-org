/**
 * Copyright 2013 Theodor Costache
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License. 
 */
package de.costache.calendar.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Date;

import javax.swing.JPanel;

import de.costache.calendar.JCalendar;
import de.costache.calendar.util.CalendarUtil;

/**
 * 
 * @author theodorcostache
 * 
 */
public class HoursPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final JCalendar owner;

	/**
	 * Creates a new instance of {@link HoursPanel}
	 */
	public HoursPanel(JCalendar owner) {
		super(true);
		setOpaque(false);
		setPreferredSize(new Dimension(40, 1440));
		this.owner = owner;
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);

		g.setColor(new Color(230, 230, 230));
		g.drawRect(0, 0, getWidth(), getHeight());
		int height = 60;
		int y = 0;

		int fontSize = 12;
		boolean fits = false;
		while (!fits) {
			final Font font = new Font("Verdana", Font.PLAIN, fontSize);
			final FontMetrics metrics = g.getFontMetrics(font);

			if (metrics.stringWidth("12 pm") <= (getWidth() - 5)) {
				fits = true;
			}
			fontSize -= 1;
		}
		fontSize = fontSize > 12 ? 12 : fontSize;
		final Font font = new Font("Verdana", Font.PLAIN, fontSize);
		g.setFont(font);
		final FontMetrics metrics = g.getFontMetrics(font);

		for (int i = 0; i < 24; i++) {
			y += height;
			String hour = i + 1 + ": 00";
			if (i == 11)
				hour = "12 pm";
			if (i == 23)
				hour = "12 am";
			int stringWidth = metrics.stringWidth(hour);
			g.setColor(Color.gray);

			g.drawString(hour, getWidth() - stringWidth - 2, y - 2);
			g.setColor(owner.getConfig().getLineColor());
			g.drawLine(0, y, getWidth(), y);
			g.setColor(owner.getConfig().getMiddleLineColor());
			g.drawLine(0, y - 30, getWidth(), y - 30);
		}

		((Graphics2D) g).setStroke(new BasicStroke(2f));
		g.setColor(owner.getConfig().getTodayHeaderBackgroundColor());
		int yNow = CalendarUtil.secondsToPixels(new Date(), 1440);
		g.drawLine(0, yNow, getWidth(), yNow);
	}
}
