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

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JPanel;

import de.costache.calendar.ui.strategy.Config;
import de.costache.calendar.JCalendar;
import de.costache.calendar.util.CalendarUtil;

/**
 * 
 * @author theodorcostache
 * 
 */
public class DayHeaderPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String headerText;
	private final DayPanel owner;

	/**
	 * Creates a new instance of {@link DayHeaderPanel}
	 * 
	 * @param headerText
	 */
	public DayHeaderPanel(final DayPanel owner, final String headerText) {
		super(true);
		setOpaque(false);
		this.headerText = headerText;
		this.owner = owner;
	}

	/**
	 * returns the owner
	 * 
	 * @return
	 */
	public DayPanel getOwner() {
		return owner;
	}

	@Override
	public void paint(final Graphics g) {
		super.paint(g);
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
		final int height = getHeight();
		final int width = getWidth();

		JCalendar calendar = owner.getOwner();
		Config config = calendar.getConfig();

		final boolean isToday = CalendarUtil.isToday(owner.getDate());
		final Color bgColor = isToday ? config.getTodayHeaderBackgroundColor() : config.getDayHeaderBackgroundColor();
		final Color fgColor = isToday ? config.getTodayHeaderForegroundColor() : config.getDayHeaderForegroundColor();

		g2d.setColor(bgColor);
		g2d.fillRect(0, 0, width, height);
		g2d.setColor(bgColor);
		g2d.drawRect(0, 0, width, height);
		g2d.setColor(fgColor);
		int fontSize = Math.round(height * 0.5f);
		fontSize = fontSize > 12 ? 12 : fontSize;

		final Font font = new Font("Verdana", Font.PLAIN, fontSize);
		final FontMetrics metrics = g2d.getFontMetrics(font);
		g2d.setFont(font);

		g2d.drawString(headerText, 5, height / 2 + metrics.getHeight() / 2);
	}

	/**
	 * @return the headerText
	 */
	public String getHeaderText() {
		return headerText;
	}

	/**
	 * @param headerText
	 *            the headerText to set
	 */
	public void setHeaderText(final String headerText) {
		this.headerText = headerText;
	}
}
