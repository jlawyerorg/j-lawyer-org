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
package de.costache.calendar.ui.strategy;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import de.costache.calendar.JCalendar;
import de.costache.calendar.ui.ContentPanel;
import de.costache.calendar.ui.DayPanel;
import de.costache.calendar.ui.HoursPanel;
import de.costache.calendar.util.CalendarUtil;

/**
 * 
 * @author theodorcostache
 * 
 */
class WeekDisplayStrategy implements DisplayStrategy {

	private final ContentPanel parent;
    private final JCalendar calendar;
	private final SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy");
	private final DayPanel[] days = new DayPanel[7];

	private JPanel displayPanel;

	public WeekDisplayStrategy(final ContentPanel parent) {
		this.parent = parent;
        this.calendar = parent.getOwner();
		init();
	}

	@Override
	public void init() {

		UIDefaults uidef = UIManager.getDefaults();
		int swidth = Integer.parseInt(uidef.get("ScrollBar.width").toString());

		Calendar start = CalendarUtil.getCalendar(new Date(), true);
		start.set(Calendar.DAY_OF_WEEK, start.getFirstDayOfWeek());

        Calendar end = CalendarUtil.getCalendar(start.getTime(), true);
        end.add(Calendar.DATE,7);

        calendar.getConfig().setIntervalStart(start);
        calendar.getConfig().setIntervalEnd(end);

		JPanel headersPanel = new JPanel(true);
		headersPanel.setLayout(new GridLayout());
		headersPanel.setOpaque(false);

		JPanel contentsPanel = new JPanel(true);
		contentsPanel.setLayout(new GridBagLayout());
		contentsPanel.setOpaque(false);

		JPanel allDayPanel = new JPanel(true);
		allDayPanel.setLayout(new GridLayout());
		allDayPanel.setOpaque(false);

		displayPanel = new JPanel(true);
		displayPanel.setOpaque(false);
		displayPanel.setLayout(new GridBagLayout());
		final GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weighty = 1;
		gbc.weightx = 0;
		gbc.fill = GridBagConstraints.BOTH;
		contentsPanel.add(new HoursPanel(parent.getOwner()), gbc);

		final Calendar c = CalendarUtil.copyCalendar(start, true);
		for (int i = 0; i < 7; i++) {
			days[i] = new DayPanel(parent.getOwner(), c.getTime(), 0.02f);
			headersPanel.add(days[i].getHeaderPanel());
			gbc.gridx = i + 1;
			gbc.gridy = 0;
			gbc.weighty = 1;
			gbc.weightx = 1;
			days[i].getContentPanel().setPreferredSize(new Dimension(30, 1440));
			contentsPanel.add(days[i].getContentPanel(), gbc);
			allDayPanel.add(days[i].getCompleteDayPanel());
			c.add(Calendar.DATE, 1);
		}

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weighty = 0.02;
		gbc.weightx = 1;
		gbc.insets = new Insets(0, 41, 0, swidth);
		displayPanel.add(headersPanel, gbc);
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weighty = 0.88;
		gbc.insets = new Insets(0, 0, 0, 0);

		JScrollPane content = new JScrollPane(contentsPanel);
		content.setOpaque(false);
		content.getViewport().setOpaque(false);
		content.setBorder(new EmptyBorder(0, 0, 0, 0));
		content.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		content.getViewport().setViewPosition(new Point(0, 500));

		displayPanel.add(content, gbc);
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 2;
		gbc.weighty = 0.1;
		gbc.insets = new Insets(0, 41, 0, 0);

		JScrollPane contentAllDay = new JScrollPane(allDayPanel);
		contentAllDay.setOpaque(false);
		contentAllDay.getViewport().setOpaque(false);
		contentAllDay.setBorder(BorderFactory.createLineBorder(parent.getOwner().getConfig().getLineColor()));
		contentAllDay.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		displayPanel.add(contentAllDay, gbc);
	}

	@Override
	public void display() {
		parent.removeAll();
		parent.setLayout(new BorderLayout());
		parent.add(displayPanel, BorderLayout.CENTER);
		parent.validate();
		parent.repaint();

	}

	@Override
	public void moveIntervalLeft() {
		Calendar start = calendar.getConfig().getIntervalStart();
        start.setTime(CalendarUtil.createInDays(start.getTime(), -7));
		start.set(Calendar.DAY_OF_WEEK, start.getFirstDayOfWeek());
        Calendar end = CalendarUtil.getCalendar(start.getTime(), true);
        end.add(Calendar.DATE,7);

        calendar.getConfig().setIntervalStart(start);
        calendar.getConfig().setIntervalEnd(end);

		final Calendar c = CalendarUtil.copyCalendar(start, true);
		for (int i = 0; i < 7; i++) {
			days[i].setDate(c.getTime());
			c.add(Calendar.DATE, 1);
		}

		parent.validate();
		parent.repaint();
	}

	@Override
	public void moveIntervalRight() {
        Calendar start = calendar.getConfig().getIntervalStart();
        start.setTime(CalendarUtil.createInDays(start.getTime(), 7));
        start.set(Calendar.DAY_OF_WEEK, start.getFirstDayOfWeek());
        Calendar end = CalendarUtil.getCalendar(start.getTime(), true);
        end.add(Calendar.DATE,7);

        calendar.getConfig().setIntervalStart(start);
        calendar.getConfig().setIntervalEnd(end);
		final Calendar c = CalendarUtil.copyCalendar(start, true);
		for (int i = 0; i < 7; i++) {
			days[i].setDate(c.getTime());
			c.add(Calendar.DATE, 1);
		}

		parent.validate();
		parent.repaint();
	}

	@Override
	public String getDisplayInterval() {
		return sdf.format(calendar.getConfig().getIntervalStart().getTime()) + " - " + sdf.format(calendar.getConfig().getIntervalEnd().getTime());
	}

	@Override
	public void setIntervalStart(Date date) {
	    Calendar start = CalendarUtil.getCalendar(date, true);
		start.set(Calendar.DAY_OF_WEEK, start.getFirstDayOfWeek());
        Calendar end = CalendarUtil.getCalendar(start.getTime(), true);
        end.add(Calendar.DATE,7);

        calendar.getConfig().setIntervalStart(start);
        calendar.getConfig().setIntervalEnd(end);

		final Calendar c = CalendarUtil.copyCalendar(start, true);
		for (int i = 0; i < 7; i++) {
			days[i].setDate(c.getTime());
			c.add(Calendar.DATE, 1);
		}

		parent.validate();
		parent.repaint();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.mediamarkt.calendar.strategy.DisplayStrategy#getType()
	 */
	@Override
	public Type getType() {
		return Type.WEEK;
	}
}