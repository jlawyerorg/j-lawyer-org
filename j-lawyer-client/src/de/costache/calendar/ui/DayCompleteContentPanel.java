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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collection;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import de.costache.calendar.ui.strategy.Config;
import de.costache.calendar.JCalendar;
import de.costache.calendar.model.CalendarEvent;
import de.costache.calendar.util.EventCollection;
import de.costache.calendar.util.EventCollectionRepository;
import de.costache.calendar.util.GraphicsUtil;

/**
 * 
 * @author theodorcostache
 * 
 */
public class DayCompleteContentPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final DayPanel owner;

	public DayCompleteContentPanel(DayPanel owner) {
		this.owner = owner;
		setOpaque(false);
		setBorder(BorderFactory.createLineBorder(owner.getOwner().getConfig().getLineColor()));

		this.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(final MouseEvent e) {
				for (MouseListener ml : DayCompleteContentPanel.this.owner.getOwner().getMouseListeners()) {
					ml.mouseClicked(e);
				}
			}

			@Override
			public void mouseReleased(final MouseEvent e) {
				for (MouseListener ml : DayCompleteContentPanel.this.owner.getOwner().getMouseListeners()) {
					ml.mouseReleased(e);
				}
			}

			@Override
			public void mousePressed(final MouseEvent e) {

				final JCalendar calendar = DayCompleteContentPanel.this.owner.getOwner();
				final CalendarEvent event = getEvent(e.getX(), e.getY());

				if (e.getClickCount() == 1) {

					final EventCollection events = EventCollectionRepository.get(calendar);

					if (!e.isControlDown()) {
						events.clearSelected(event, true);
					}
					if (event != null) {
						event.setSelected(true);
						if (event.isSelected()) {
							events.addSelected(event);
						} else {
							events.removeSelected(event);
						}
					}

					calendar.validate();
					calendar.repaint();

				}
				if (e.isPopupTrigger() && calendar.getPopupMenu() != null) {
					calendar.getPopupMenu().show(DayCompleteContentPanel.this, e.getX(), e.getY());
				}
				for (MouseListener ml : DayCompleteContentPanel.this.owner.getOwner().getMouseListeners()) {
					ml.mousePressed(e);
				}
			}

		});

		addMouseMotionListener(new MouseAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				super.mouseMoved(e);
				final JCalendar calendar = DayCompleteContentPanel.this.owner.getOwner();
				final CalendarEvent event = getEvent(e.getX(), e.getY());
				if (event != null) {
					setToolTipText(calendar.getTooltipFormater().format(event));
				} else {
					setToolTipText(null);
				}
			}
		});

	}

	public DayPanel getOwner() {
		return owner;
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		drawFullDayEvents((Graphics2D) g);
	}

	private void drawFullDayEvents(final Graphics2D graphics2d) {

		final EventCollection eventsCollection = EventCollectionRepository.get(owner.getOwner());
		final Collection<CalendarEvent> events = eventsCollection.getEvents(owner.getDate());
		int pos = 2;
		if (events.size() > 0) {

			final Config config = owner.getOwner().getConfig();

			for (final CalendarEvent event : events) {
				if (!event.isAllDay())
					continue;
				Color bgColor = event.getType().getBackgroundColor();
				bgColor = bgColor == null ? config.getEventDefaultBackgroundColor() : bgColor;
				Color fgColor = event.getType().getForegroundColor();
				fgColor = fgColor == null ? config.getEventDefaultForegroundColor() : fgColor;
				graphics2d.setColor(!event.isSelected() ? bgColor : bgColor.darker().darker());
				graphics2d.fillRect(2, pos, getWidth() - 4, 15);

				final String eventString = event.getSummary();
				int fontSize = Math.round(getHeight() * 0.5f);
				fontSize = fontSize > 9 ? 9 : fontSize;

				final Font font = new Font("Verdana", Font.BOLD, fontSize);
				final FontMetrics metrics = graphics2d.getFontMetrics(font);
				graphics2d.setFont(font);

				graphics2d.setColor(!event.isSelected() ? fgColor : Color.white);
				GraphicsUtil.drawTrimmedString(graphics2d, eventString, 6,
						pos + (13 / 2 + metrics.getHeight() / 2) - 2, getWidth());

				pos += 17;
			}
			setPreferredSize(new Dimension(0, pos));
			revalidate();
		}
	}

	private CalendarEvent getEvent(final int x, final int y) {

		final EventCollection eventsCollection = EventCollectionRepository.get(owner.getOwner());
		final Collection<CalendarEvent> events = eventsCollection.getEvents(owner.getDate());

		int pos = 2;
		if (events.size() > 0) {
			for (final CalendarEvent event : events) {
				if (!event.isAllDay())
					continue;
				final int rectXStart = 2;
				final int rectYStart = pos;

				final int rectWidth = getWidth() - 4;

				final int rectHeight = 15;

				final Rectangle r = new Rectangle(rectXStart, rectYStart, rectWidth, rectHeight);
				if (r.contains(x, y)) {
					return event;
				}

				pos += 17;

			}
		}
		return null;
	}
}
