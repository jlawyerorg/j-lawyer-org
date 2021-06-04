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

import de.costache.calendar.JCalendar;
import de.costache.calendar.ui.ContentPanel;
import de.costache.calendar.ui.DayPanel;
import de.costache.calendar.util.CalendarUtil;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @author theodorcostache
 */
class MonthDisplayStrategy implements DisplayStrategy {

    private final ContentPanel parent;
    private final JCalendar calendar;
    private final SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy");
    private final DayPanel[] days = new DayPanel[35];
    private JPanel displayPanel;

    /**
     * Creates a new instance of MonthDisplayStrategy
     *
     * @param parent
     */
    public MonthDisplayStrategy(final ContentPanel parent) {
        this.parent = parent;
        this.calendar = parent.getOwner();
        init();
    }

    @Override
    public void init() {

        Calendar start = CalendarUtil.getCalendar(
                new Date(), true);
        start.set(Calendar.DAY_OF_MONTH, 1);
        Calendar end = CalendarUtil.getCalendar(start.getTime(), true);
        end.add(Calendar.MONTH, 1);

        calendar.getConfig().setIntervalStart(start);
        calendar.getConfig().setIntervalEnd(end);

        displayPanel = new JPanel(true);
        displayPanel.setOpaque(false);
        displayPanel.setLayout(new GridLayout(5, 7));
        final Calendar c = CalendarUtil.copyCalendar(start, true);
        c.set(Calendar.DAY_OF_WEEK,c.getFirstDayOfWeek());
        for (int i = 0; i < 35; i++) {
            days[i] = new DayPanel(parent.getOwner(), c.getTime(), 0.1f);
            days[i].setEnabled(CalendarUtil.isSameMonth(start, c));
            displayPanel.add(days[i].layout());
            c.add(Calendar.DATE, 1);
        }

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
        Calendar start = CalendarUtil.copyCalendar(calendar.getConfig().getIntervalStart(), true);

        start.add(Calendar.MONTH, -1);
        start.set(Calendar.DAY_OF_MONTH, 1);

        Calendar end = CalendarUtil.getCalendar(start.getTime(), true);
        end.add(Calendar.MONTH, 1);

        calendar.getConfig().setIntervalStart(start);
        calendar.getConfig().setIntervalEnd(end);

        Calendar c = CalendarUtil.copyCalendar(start, true);
        c.set(Calendar.DAY_OF_WEEK,c.getFirstDayOfWeek());
        for (int i = 0; i < 35; i++) {
            days[i].setDate(c.getTime());
            days[i].setEnabled(CalendarUtil.isSameMonth(start, c));
            c.add(Calendar.DATE, 1);
        }

        parent.validate();
        parent.repaint();
    }

    @Override
    public void moveIntervalRight() {
        Calendar start = CalendarUtil.copyCalendar(calendar.getConfig().getIntervalStart(), true);

        start.add(Calendar.MONTH, 1);
        start.set(Calendar.DAY_OF_MONTH, 1);

        Calendar end = CalendarUtil.getCalendar(start.getTime(), true);
        end.add(Calendar.MONTH, 1);

        calendar.getConfig().setIntervalStart(start);
        calendar.getConfig().setIntervalEnd(end);

        Calendar c = CalendarUtil.copyCalendar(start, true);
        c.set(Calendar.DAY_OF_WEEK,c.getFirstDayOfWeek());
        for (int i = 0; i < 35; i++) {
            days[i].setDate(c.getTime());
            days[i].setEnabled(CalendarUtil.isSameMonth(start, c));
            c.add(Calendar.DATE, 1);
        }

        parent.validate();
        parent.repaint();
    }

    /*
     * (non-Javadoc)
     *
     * @see de.mediamarkt.calendar.strategy.DisplayStrategy#getDisplayInterval()
     */
    @Override
    public String getDisplayInterval() {
        Calendar c = CalendarUtil.copyCalendar(calendar.getConfig().getIntervalStart(), true);
        return sdf.format(c.getTime());
    }

    @Override
    public void setIntervalStart(Date date) {
        Calendar start = CalendarUtil.getCalendar(date, true);
        start.set(Calendar.DAY_OF_MONTH, 1);

        Calendar end = CalendarUtil.getCalendar(start.getTime(), true);
        end.add(Calendar.MONTH, 1);

        calendar.getConfig().setIntervalStart(start);
        calendar.getConfig().setIntervalEnd(end);

        Calendar c = CalendarUtil.copyCalendar(start, true);
        c.set(Calendar.DAY_OF_WEEK,c.getFirstDayOfWeek());
        for (int i = 0; i < 35; i++) {
            days[i].setDate(c.getTime());
            days[i].setEnabled(CalendarUtil.isSameMonth(start, c));
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
        return Type.MONTH;
    }

}
