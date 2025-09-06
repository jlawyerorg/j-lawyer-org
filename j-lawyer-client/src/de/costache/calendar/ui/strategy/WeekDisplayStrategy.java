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
import com.jdimension.jlawyer.client.settings.ClientSettings;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Cursor;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

/**
 *
 * @author theodorcostache
 *
 */
class WeekDisplayStrategy implements DisplayStrategy {

    private final ContentPanel parent;
    private final JCalendar calendar;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd. MMMM yyyy");
    private final DayPanel[] days = new DayPanel[7];

    private JSplitPane split = null;
    private boolean workWeek = false;

    private int getActiveDayCount() {
        return workWeek ? 5 : 7;
    }

    public WeekDisplayStrategy(final ContentPanel parent) {
        this.parent = parent;
        this.calendar = parent.getOwner();

        init();
    }

    @Override
    public void init() {

        Calendar start = CalendarUtil.getCalendar(new Date(), true);
        start.set(Calendar.DAY_OF_WEEK, start.getFirstDayOfWeek());

        Calendar end = CalendarUtil.getCalendar(start.getTime(), true);
        // load persisted preference
        ClientSettings cs = ClientSettings.getInstance();
        this.workWeek = "true".equalsIgnoreCase(cs.getConfiguration(ClientSettings.CONF_CALENDAR_WEEKVIEW_WORKWEEK, "false"));
        int dayCount = getActiveDayCount();
        end.add(Calendar.DATE, dayCount);

        calendar.getConfig().setIntervalStart(start);
        calendar.getConfig().setIntervalEnd(end);

        JPanel headersPanel = new JPanel(true);
        headersPanel.setLayout(new BorderLayout());
        headersPanel.setOpaque(false);
        // Allow the header to shrink with the viewport width; fix height only
        headersPanel.setMinimumSize(new Dimension(0, 60));
        headersPanel.setPreferredSize(new Dimension(0, 60));

        // Viewport panel: contains only the 7 day content columns (no hours column)
        // Use GridBagLayout so preferred heights of day panels (e.g., 1440) are honored for scrolling
        JPanel contentsPanel = new JPanel(true);
        contentsPanel.setLayout(new GridBagLayout());
        contentsPanel.setOpaque(false);

        JPanel allDayPanel = new JPanel(true);
        allDayPanel.setLayout(new GridLayout());
        allDayPanel.setOpaque(false);

        JPanel displayPanel = new JPanel(true);
        displayPanel.setOpaque(false);
        displayPanel.setLayout(new BorderLayout());

        // Use a single HoursPanel instance as the row header of the scroll pane
        HoursPanel hoursPanel = new HoursPanel(parent.getOwner());

        final Calendar c = CalendarUtil.copyCalendar(start, true);
        // Create containers for day headers (top) and day contents (center)
        JPanel dayHeaders = new JPanel(new GridLayout(1, dayCount));
        dayHeaders.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        for (int i = 0; i < dayCount; i++) {
            days[i] = new DayPanel(parent.getOwner(), c.getTime(), 0.02f);
            dayHeaders.add(days[i].getHeaderPanel());
            // Single-click on header switches to day view
            days[i].getHeaderPanel().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            final DayPanel panelRef = days[i];
            days[i].getHeaderPanel().addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        Date current = panelRef.getDate();
                        parent.getOwner().setSelectedDay(current);
                        parent.getOwner().setDisplayStrategy(Type.DAY, current);
                        e.consume();
                    }
                }
            });
            days[i].getContentPanel().setPreferredSize(new Dimension(30, 1440));
            gbc.gridx = i;
            gbc.weightx = 1;
            gbc.weighty = 1;
            contentsPanel.add(days[i].getContentPanel(), gbc);
            allDayPanel.add(days[i].getCompleteDayPanel());
            c.add(Calendar.DATE, 1);
        }

        // Header now only holds the 7 day headers (no spacer needed)
        headersPanel.add(dayHeaders, BorderLayout.CENTER);

        // Scroll on header switches weeks (left/right). Content area keeps vertical scroll.
        headersPanel.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (getType() != Type.WEEK) return;
                if (e.getWheelRotation() < 0) {
                    moveIntervalLeft();
                } else {
                    moveIntervalRight();
                }
                parent.getOwner().getHeaderPanel().getIntervalLabel().setText(getDisplayInterval());
                e.consume();
            }
        });

        // Ensure vertical scroll range by setting a preferred height for the week grid
        contentsPanel.setPreferredSize(new Dimension(0, 1440));

        JScrollPane content = new JScrollPane(contentsPanel);
        content.setWheelScrollingEnabled(true);
        content.setOpaque(false);
        content.getViewport().setOpaque(false);
        content.setBorder(new EmptyBorder(0, 0, 0, 0));
        content.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        content.getViewport().setViewPosition(new Point(0, 500));
        content.getVerticalScrollBar().setUnitIncrement(16);

        // Add 5-day workweek toggle in the upper-left corner (week view only)
        javax.swing.JToggleButton togWorkweek = new javax.swing.JToggleButton("5T");
        togWorkweek.setSelected(this.workWeek);
        togWorkweek.setToolTipText("5-Tage-Woche anzeigen");
        togWorkweek.setMargin(new java.awt.Insets(0, 2, 0, 2));
        togWorkweek.addActionListener(e -> {
            this.workWeek = togWorkweek.isSelected();
            try {
                ClientSettings.getInstance().setConfiguration(ClientSettings.CONF_CALENDAR_WEEKVIEW_WORKWEEK, Boolean.toString(this.workWeek));
                ClientSettings.getInstance().saveConfiguration();
            } catch (Exception ex) {
                // ignore save failure, preference still applied for current session
            }
            // Re-init with new day count and redraw
            init();
            display();
            parent.getOwner().getHeaderPanel().getIntervalLabel().setText(getDisplayInterval());
        });
        content.setCorner(ScrollPaneConstants.UPPER_LEFT_CORNER, togWorkweek);

        // Do not intercept mouse wheel here: allow vertical scrolling in the viewport.
        // Week navigation via wheel remains handled by ContentPanel when events are not consumed
        // (e.g., when hovering non-scrollable areas like the header).

        // Put headers as column header view so it shares viewport width (accounts for scrollbar)
        content.setColumnHeaderView(headersPanel);
        // Put hours panel as row header so it's outside the viewport width
        content.setRowHeaderView(hoursPanel);

        displayPanel.add(content, BorderLayout.CENTER);

        JScrollPane contentAllDay = new JScrollPane(allDayPanel);
        contentAllDay.setOpaque(false);
        contentAllDay.getViewport().setOpaque(false);
        contentAllDay.setBorder(BorderFactory.createLineBorder(parent.getOwner().getConfig().getLineColor()));
        contentAllDay.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        contentAllDay.getVerticalScrollBar().setUnitIncrement(16);
        // Align all-day columns with timed grid by adding a left spacer equal to HoursPanel width
        JPanel allDayRowHeaderSpacer = new JPanel(true);
        allDayRowHeaderSpacer.setOpaque(false);
        allDayRowHeaderSpacer.setPreferredSize(new Dimension(hoursPanel.getPreferredSize().width, 1));
        contentAllDay.setRowHeaderView(allDayRowHeaderSpacer);
        
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
        start.setTime(CalendarUtil.createInDays(start.getTime(), -7));
        start.set(Calendar.DAY_OF_WEEK, start.getFirstDayOfWeek());
        Calendar end = CalendarUtil.getCalendar(start.getTime(), true);
        end.add(Calendar.DATE, 7);

        calendar.getConfig().setIntervalStart(start);
        calendar.getConfig().setIntervalEnd(end);

        final Calendar c = CalendarUtil.copyCalendar(start, true);
        int dayCount = getActiveDayCount();
        for (int i = 0; i < dayCount; i++) {
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
        end.add(Calendar.DATE, getActiveDayCount());

        calendar.getConfig().setIntervalStart(start);
        calendar.getConfig().setIntervalEnd(end);
        final Calendar c = CalendarUtil.copyCalendar(start, true);
        int dayCount = getActiveDayCount();
        for (int i = 0; i < dayCount; i++) {
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
        end.add(Calendar.DATE, getActiveDayCount());

        calendar.getConfig().setIntervalStart(start);
        calendar.getConfig().setIntervalEnd(end);

        final Calendar c = CalendarUtil.copyCalendar(start, true);
        int dayCount = getActiveDayCount();
        for (int i = 0; i < dayCount; i++) {
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
