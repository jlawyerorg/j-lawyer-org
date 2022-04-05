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

import com.jdimension.jlawyer.client.utils.ComponentUtils;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;

import de.costache.calendar.JCalendar;
import de.costache.calendar.ui.ContentPanel;
import de.costache.calendar.ui.DayPanel;
import de.costache.calendar.ui.HoursPanel;
import de.costache.calendar.util.CalendarUtil;
import javax.swing.JSplitPane;

/**
 *
 * @author theodorcostache
 *
 */
class DayDisplayStrategy implements DisplayStrategy {

    private final ContentPanel parent;
    private final JCalendar calendar;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd. MMMM yyyy");
    private DayPanel day;
    private JSplitPane split;

    public DayDisplayStrategy(final ContentPanel parent) {
        this.parent = parent;
        this.calendar = parent.getOwner();
        init();
    }

    @Override
    public void init() {

        Calendar start = CalendarUtil.getCalendar(new Date(), true);
        Calendar end = CalendarUtil.getCalendar(new Date(), true);
        end.add(Calendar.DATE, 1);

        calendar.getConfig().setIntervalStart(start);
        calendar.getConfig().setIntervalEnd(end);

        JPanel contentsPanel = new JPanel(true);
        contentsPanel.setLayout(new GridBagLayout());
        contentsPanel.setOpaque(false);

        JPanel displayPanel = new JPanel(true);
        displayPanel.setOpaque(false);
        displayPanel.setLayout(new BorderLayout());
        final GridBagConstraints gbc = new GridBagConstraints();

        final Calendar c = CalendarUtil.copyCalendar(start, true);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weighty = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.BOTH;
        contentsPanel.add(new HoursPanel(parent.getOwner()), gbc);

        day = new DayPanel(parent.getOwner(), c.getTime());
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weighty = 1;
        gbc.weightx = 1;
        day.getContentPanel().setPreferredSize(new Dimension(30, 1440));
        contentsPanel.add(day.getContentPanel(), gbc);
        c.add(Calendar.DATE, 1);

        day.getHeaderPanel().setPreferredSize(new Dimension(1440, 60));
        displayPanel.add(day.getHeaderPanel(), BorderLayout.NORTH);
        JScrollPane content = new JScrollPane(contentsPanel);
        content.setOpaque(false);
        content.getViewport().setOpaque(false);
        content.setBorder(new EmptyBorder(0, 0, 0, 0));
        content.getViewport().setViewPosition(new Point(0, 500));
        content.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        content.getVerticalScrollBar().setUnitIncrement(16);
        displayPanel.add(content, BorderLayout.CENTER);
        JScrollPane contentAllDay = new JScrollPane(day.getCompleteDayPanel());
        contentAllDay.setOpaque(false);
        contentAllDay.getViewport().setOpaque(false);
        contentAllDay.setBorder(BorderFactory.createLineBorder(parent.getOwner().getConfig().getLineColor()));
        contentAllDay.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        contentAllDay.getVerticalScrollBar().setUnitIncrement(16);

        this.split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, displayPanel, contentAllDay);
        ComponentUtils.decorateSplitPane(split);
        
        this.split.setDividerLocation(200);
        ComponentUtils.restoreSplitPane(this.split, this.getClass(), "split");
        ComponentUtils.persistSplitPane(this.split, this.getClass(), "split");

    }

    @Override
    public void display() {
        parent.removeAll();
        parent.setLayout(new BorderLayout());
        parent.add(split, BorderLayout.CENTER);
        parent.validate();
        parent.repaint();
    }

    @Override
    public void moveIntervalLeft() {
        Calendar start = calendar.getConfig().getIntervalStart();
        start.add(Calendar.DATE, -1);
        Calendar end = CalendarUtil.getCalendar(start.getTime(), true);
        end.add(Calendar.DATE, 1);

        calendar.getConfig().setIntervalStart(start);
        calendar.getConfig().setIntervalEnd(end);

        day.setDate(start.getTime());
        parent.validate();
        parent.repaint();
    }

    @Override
    public void moveIntervalRight() {
        Calendar start = calendar.getConfig().getIntervalStart();
        start.add(Calendar.DATE, 1);
        Calendar end = CalendarUtil.getCalendar(start.getTime(), true);
        end.add(Calendar.DATE, 1);

        calendar.getConfig().setIntervalStart(start);
        calendar.getConfig().setIntervalEnd(end);

        day.setDate(start.getTime());
        parent.validate();
        parent.repaint();
    }

    @Override
    public void setIntervalStart(Date date) {
        Calendar start = calendar.getConfig().getIntervalStart();
        start.setTime(date);
        Calendar end = CalendarUtil.getCalendar(start.getTime(), true);
        end.add(Calendar.DATE, 1);

        calendar.getConfig().setIntervalStart(start);
        calendar.getConfig().setIntervalEnd(end);

        day.setDate(start.getTime());
        parent.validate();
        parent.repaint();
    }

    @Override
    public String getDisplayInterval() {
        return sdf.format(calendar.getConfig().getIntervalStart().getTime());
    }

    /*
	 * (non-Javadoc)
	 * 
	 * @see de.mediamarkt.calendar.strategy.DisplayStrategy#getType()
     */
    @Override
    public Type getType() {
        return Type.DAY;
    }

}
