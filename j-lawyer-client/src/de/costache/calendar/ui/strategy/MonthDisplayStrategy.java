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
import de.costache.calendar.ui.DayContentPanel;
import de.costache.calendar.util.CalendarUtil;
import de.costache.calendar.util.EventRepository;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseWheelEvent;
import java.util.HashSet;
import java.util.Set;
import themes.colors.DefaultColorTheme;

/**
 * Month display strategy for the calendar UI.
 * 
 * Author: theodorcostache
 */
class MonthDisplayStrategy implements DisplayStrategy {

    private final ContentPanel parent;
    private final JCalendar calendar;
    private final SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy");
    private final DayPanel[] days = new DayPanel[35];
    private JPanel displayPanel;
    // Anchor date for month scrolling; preserves the originally selected date across scrolls
    private Calendar monthAnchor = null;
    
    // Selection state for multi-day drag
    private boolean selectionActive = false;
    private int selectionStartIndex = -1;
    private Set<Integer> highlighted = new HashSet<>();
    private java.awt.Point pressPoint = null;
    private static final int H_DRAG_THRESHOLD = 6; // pixels
    private Date selectionStartDate = null; // anchor date across month scroll while dragging

    /**
     * Creates a new instance of MonthDisplayStrategy
     *
     * @param parent the parent content panel
     */
    public MonthDisplayStrategy(final ContentPanel parent) {
        this.parent = parent;
        this.calendar = parent.getOwner();
        init();
    }

    @Override
    public void init() {
        Calendar start = CalendarUtil.getCalendar(new Date(), true);
        start.set(Calendar.DAY_OF_MONTH, 1);
        Calendar end = CalendarUtil.getCalendar(start.getTime(), true);
        end.add(Calendar.MONTH, 1);

        calendar.getConfig().setIntervalStart(start);
        calendar.getConfig().setIntervalEnd(end);

        displayPanel = new JPanel(true);
        displayPanel.setOpaque(false);
        displayPanel.setLayout(new GridLayout(5, 7));
        // initialize anchor to the currently selected day (preserve across wheel scrolls)
        this.monthAnchor = CalendarUtil.getCalendar(calendar.getSelectedDay(), true);
        final Calendar c = CalendarUtil.copyCalendar(start, true);
        c.set(Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek());
        for (int i = 0; i < 35; i++) {
            days[i] = new DayPanel(parent.getOwner(), c.getTime(), 0.1f);
            days[i].setEnabled(CalendarUtil.isSameMonth(start, c));
            JPanel cell = days[i].layout();
            displayPanel.add(cell);
            // In month view: single-click on the day header switches to day view
            days[i].getHeaderPanel().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            final DayPanel panelRef = days[i];
            final javax.swing.JComponent headerComp = days[i].getHeaderPanel();
            headerComp.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1 && !selectionActive) {
                        Date current = panelRef.getDate();
                        parent.getOwner().setSelectedDay(current);
                        parent.getOwner().setDisplayStrategy(DisplayStrategy.Type.DAY, current);
                        e.consume();
                    }
                }
                @Override
                public void mousePressed(MouseEvent e) {
                    java.awt.Point p = SwingUtilities.convertPoint(headerComp, e.getPoint(), displayPanel);
                    handlePress(p);
                }
                @Override
                public void mouseReleased(MouseEvent e) {
                    java.awt.Point p = SwingUtilities.convertPoint(headerComp, e.getPoint(), displayPanel);
                    handleRelease(p);
                }
            });
            headerComp.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    java.awt.Point p = SwingUtilities.convertPoint(headerComp, e.getPoint(), displayPanel);
                    handleDrag(p);
                }
            });
            // Also listen on each cell to capture drag across cells
            cell.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    java.awt.Point p = SwingUtilities.convertPoint(cell, e.getPoint(), displayPanel);
                    handlePress(p);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    java.awt.Point p = SwingUtilities.convertPoint(cell, e.getPoint(), displayPanel);
                    handleRelease(p);
                }
            });
            cell.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    java.awt.Point p = SwingUtilities.convertPoint(cell, e.getPoint(), displayPanel);
                    handleDrag(p);
                }
            });
            c.add(Calendar.DATE, 1);
        }

        // Month navigation via wheel anywhere over the month grid
        displayPanel.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (getType() != Type.MONTH) return;
                if (e.getWheelRotation() < 0) {
                    moveIntervalLeft();
                } else {
                    moveIntervalRight();
                }
                parent.getOwner().getHeaderPanel().getIntervalLabel().setText(getDisplayInterval());
                e.consume();
            }
        });

        // Multi-day selection via horizontal drag
        displayPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) { handlePress(e.getPoint()); }
            @Override
            public void mouseReleased(MouseEvent e) { handleRelease(e.getPoint()); }
        });
        displayPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) { handleDrag(e.getPoint()); }
        });
    }

    private void handlePress(java.awt.Point pDisplay) {
        // If already dragging (anchor set), ignore spurious press events from new cells after month scroll
        if (selectionStartDate != null) return;
        pressPoint = pDisplay;
        selectionActive = false;
        selectionStartIndex = cellIndexAtPoint(pressPoint);
        if (selectionStartIndex >= 0 && selectionStartIndex < 35) {
            selectionStartDate = stripDate(days[selectionStartIndex].getDate());
        } else {
            selectionStartDate = null;
        }
    }

    private void handleRelease(java.awt.Point pDisplay) {
        if (selectionActive) {
            int endIdx = cellIndexAtPoint(pDisplay);
            if (selectionStartDate != null && endIdx >= 0) {
                Date startDate = startOfDay(selectionStartDate);
                Date endDate = endOfDay(days[endIdx].getDate());
                // normalize order
                if (startDate.after(endDate)) {
                    Date tmp = startDate; startDate = startOfDay(days[endIdx].getDate()); endDate = endOfDay(selectionStartDate);
                }
                EventRepository.get().triggerIntervalSelection(parent.getOwner(), startDate, endDate);
            }
            clearHighlight();
        }
        selectionActive = false;
        selectionStartIndex = -1;
        pressPoint = null;
        selectionStartDate = null;
    }

    private void handleDrag(java.awt.Point pDisplay) {
        // If drag started over a content panel (vertical day drawing), do not interfere
        if (!selectionActive && isOverDayContent(pressPoint)) {
            return;
        }
        if (pressPoint == null) return;
        if (!selectionActive) {
            if (Math.abs(pDisplay.x - pressPoint.x) < H_DRAG_THRESHOLD) {
                return; // wait until horizontal drag threshold
            }
            selectionActive = true;
        }
        int currIdx = cellIndexAtPoint(pDisplay);
        if (currIdx >= 0) {
            highlightByDates(selectionStartDate, days[currIdx].getDate());
        }
    }

    private void highlightByDates(Date d1, Date d2) {
        if (d1 == null || d2 == null) { clearHighlight(); return; }
        Date a = stripDate(d1);
        Date b = stripDate(d2);
        Date firstVisible = stripDate(days[0].getDate());
        Date lastVisible = stripDate(days[34].getDate());

        // Determine start and end indices within current grid
        int idxA = findIndexByDate(a);
        int idxB = findIndexByDate(b);
        if (idxA == -1) {
            // Anchor outside current grid: clamp to edges
            if (a.before(firstVisible)) idxA = 0; else if (a.after(lastVisible)) idxA = 34; else idxA = 0;
        }
        if (idxB == -1) {
            if (b.before(firstVisible)) idxB = 0; else if (b.after(lastVisible)) idxB = 34; else idxB = 34;
        }
        highlightRange(Math.min(idxA, idxB), Math.max(idxA, idxB));
    }

    private int findIndexByDate(Date d) {
        Date target = stripDate(d);
        for (int i = 0; i < 35; i++) {
            if (stripDate(days[i].getDate()).equals(target)) return i;
        }
        return -1;
    }

    private boolean isOverDayContent(java.awt.Point p) {
        if (p == null) return false;
        java.awt.Component deepest = SwingUtilities.getDeepestComponentAt(displayPanel, p.x, p.y);
        while (deepest != null && deepest != displayPanel) {
            if (deepest instanceof DayContentPanel) return true;
            deepest = deepest.getParent();
        }
        return false;
    }

    private int cellIndexAtPoint(java.awt.Point p) {
        if (p == null) return -1;
        java.awt.Component c = SwingUtilities.getDeepestComponentAt(displayPanel, p.x, p.y);
        if (c == null) return -1;
        // Walk up to direct child of displayPanel
        java.awt.Component child = c;
        while (child != null && child.getParent() != displayPanel) {
            child = child.getParent();
        }
        if (child == null) return -1;
        return displayPanel.getComponentZOrder(child);
    }

    private void highlightRange(int from, int to) {
        clearHighlight();
        Color base = DefaultColorTheme.COLOR_LOGO_BLUE;
        java.awt.Color selColor = new java.awt.Color(base.getRed(), base.getGreen(), base.getBlue());
        java.awt.Color fillColor = new java.awt.Color(base.getRed(), base.getGreen(), base.getBlue(), 40); // light translucent fill
        for (int i = from; i <= to; i++) {
            if (i < 0 || i >= displayPanel.getComponentCount()) continue;
            java.awt.Component child = displayPanel.getComponent(i);
            if (child instanceof javax.swing.JComponent) {
                javax.swing.JComponent jc = (javax.swing.JComponent) child;
                jc.setBorder(BorderFactory.createLineBorder(selColor, 2));
                // subtle background to make selection clearer
                jc.setOpaque(true);
                jc.setBackground(fillColor);
                highlighted.add(i);
            }
        }
        displayPanel.repaint();
    }

    private void clearHighlight() {
        for (Integer idx : highlighted) {
            if (idx >= 0 && idx < displayPanel.getComponentCount()) {
                java.awt.Component child = displayPanel.getComponent(idx);
                if (child instanceof javax.swing.JComponent) {
                    javax.swing.JComponent jc = (javax.swing.JComponent) child;
                    jc.setBorder(null);
                    jc.setOpaque(false);
                }
            }
        }
        highlighted.clear();
        displayPanel.repaint();
    }

    private static Date startOfDay(Date d) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private static Date endOfDay(Date d) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }

    private static Date stripDate(Date d) {
        return CalendarUtil.stripTime(d);
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
        // Use persistent anchor across scrolls
        if (monthAnchor == null) monthAnchor = CalendarUtil.getCalendar(calendar.getSelectedDay(), true);
        int dom = monthAnchor.get(Calendar.DAY_OF_MONTH);
        monthAnchor.add(Calendar.MONTH, -1);
        int maxDom = monthAnchor.getActualMaximum(Calendar.DAY_OF_MONTH);
        monthAnchor.set(Calendar.DAY_OF_MONTH, Math.min(dom, maxDom));
        calendar.setSelectedDay(monthAnchor.getTime());
    }

    @Override
    public void moveIntervalRight() {
        if (monthAnchor == null) monthAnchor = CalendarUtil.getCalendar(calendar.getSelectedDay(), true);
        int dom = monthAnchor.get(Calendar.DAY_OF_MONTH);
        monthAnchor.add(Calendar.MONTH, 1);
        int maxDom = monthAnchor.getActualMaximum(Calendar.DAY_OF_MONTH);
        monthAnchor.set(Calendar.DAY_OF_MONTH, Math.min(dom, maxDom));
        calendar.setSelectedDay(monthAnchor.getTime());
    }

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
        c.set(Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek());
        for (int i = 0; i < 35; i++) {
            days[i].setDate(c.getTime());
            days[i].setEnabled(CalendarUtil.isSameMonth(start, c));
            c.add(Calendar.DATE, 1);
        }

        parent.validate();
        parent.repaint();
    }

    @Override
    public Type getType() {
        return Type.MONTH;
    }
}

