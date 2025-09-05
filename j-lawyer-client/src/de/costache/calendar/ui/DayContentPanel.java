/**
 * Copyright 2013 Theodor Costache
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package de.costache.calendar.ui;

import de.costache.calendar.ui.strategy.Config;
import de.costache.calendar.JCalendar;
import de.costache.calendar.model.CalendarEvent;
import de.costache.calendar.ui.strategy.DisplayStrategy.Type;
import de.costache.calendar.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * @author theodorcostache
 */
public class DayContentPanel extends JPanel {
    
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
    private final DayPanel owner;
    private Point startSelection;
    private Point endSelection;
    private Date startDate;
    private Date endDate;
    private int monthScrollOffset = 0;
    private boolean hoverMonthCell = false;
    private boolean overAddButton = false;
    private static final int ADD_BTN_SIZE = 18;
    // Tooltip timing control during drag
    private boolean dragging = false;
    private int origInitialDelay = -1;
    private int origReshowDelay = -1;
    private int origDismissDelay = -1;
    // Custom popup tooltip during drag
    private Popup dragPopup = null;
    private JLabel dragTipLabel = null;

    /**
     * Creates a new instance of {@link DayContentPanel}
     * @param owner
     */
    public DayContentPanel(final DayPanel owner) {
        super(true);
        setOpaque(false);
        this.owner = owner;
        addListeners();
    }

    private void addListeners() {

        this.addMouseListener(new MouseAdapter() {

            final JCalendar calendar = DayContentPanel.this.owner
                    .getOwner();

            @Override
            public void mouseClicked(final MouseEvent e) {
                for (final MouseListener ml : DayContentPanel.this.owner
                        .getOwner().getMouseListeners()) {
                    ml.mouseClicked(e);
                }
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                    if (startSelection == null || endSelection == null)
                        return;
                    startDate = CalendarUtil.pixelToDate(owner.getDate(), (int) startSelection.getY(), getHeight());
                    endDate = CalendarUtil.pixelToDate(owner.getDate(), (int) endSelection.getY(), getHeight());

                    EventRepository.get().triggerIntervalSelection(calendar,
                            startDate, endDate);
                }
            }

            @Override
            public void mouseReleased(final MouseEvent e) {
                final boolean isSelectedStrategyMonthRelease = calendar.getDisplayStrategy() == Type.MONTH;
                if (isSelectedStrategyMonthRelease && overAddButton) {
                    // Create a new event at working hours start, duration 30 mins
                    Calendar cs = CalendarUtil.getCalendar(owner.getDate(), false);
                    int whStart = owner.getOwner().getConfig().getWorkingHoursStart();
                    cs.set(Calendar.HOUR_OF_DAY, whStart);
                    cs.set(Calendar.MINUTE, 0);
                    cs.set(Calendar.SECOND, 0);
                    cs.set(Calendar.MILLISECOND, 0);
                    Date s = cs.getTime();
                    Calendar ce = CalendarUtil.getCalendar(s, false);
                    ce.add(Calendar.MINUTE, 30);
                    Date en = ce.getTime();
                    // Ensure any drag tooltip is removed immediately BEFORE the dialog opens
                    hideDragTip();
                    setToolTipText(null);
                    startSelection = null;
                    endSelection = null;
                    if (dragging) {
                        javax.swing.ToolTipManager ttm = javax.swing.ToolTipManager.sharedInstance();
                        if (origInitialDelay >= 0) ttm.setInitialDelay(origInitialDelay);
                        if (origReshowDelay >= 0) ttm.setReshowDelay(origReshowDelay);
                        if (origDismissDelay >= 0) ttm.setDismissDelay(origDismissDelay);
                        dragging = false;
                    }
                    EventRepository.get().triggerIntervalSelection(calendar, s, en);
                    return;
                }
                if (e.isPopupTrigger() && calendar.getPopupMenu() != null) {
                    calendar.getPopupMenu().show(DayContentPanel.this,
                            e.getX(), e.getY());
                } else {
                    final boolean isSelectedStrategyMonth = calendar
                        .getDisplayStrategy() == Type.MONTH;
                    final CalendarEvent event = isSelectedStrategyMonth ? getEventForMonth(
                        e.getX(), e.getY()) : getNotMonthEvent(e.getX(),
                        e.getY());
                    
                    if(event==null) {
                    
                    if (startDate != null && endDate != null) {
                        // Hide tooltip BEFORE opening the dialog
                        hideDragTip();
                        setToolTipText(null);
                        EventRepository.get().triggerIntervalSelection(calendar, startDate, endDate);
                    }
                    
                    }
                }
                // Clear tooltip after finishing drag/selection
                setToolTipText(null);
                hideDragTip();
                // Clear selection rectangle after release
                startSelection = null;
                endSelection = null;
                // Restore tooltip timings if we were dragging
                if (dragging) {
                    javax.swing.ToolTipManager ttm = javax.swing.ToolTipManager.sharedInstance();
                    if (origInitialDelay >= 0) ttm.setInitialDelay(origInitialDelay);
                    if (origReshowDelay >= 0) ttm.setReshowDelay(origReshowDelay);
                    if (origDismissDelay >= 0) ttm.setDismissDelay(origDismissDelay);
                    dragging = false;
                }
//                for (final MouseListener ml : DayContentPanel.this.owner
//                        .getOwner().getMouseListeners()) {
//                    ml.mouseReleased(e);
//                }
            }

            @Override
            public void mousePressed(final MouseEvent e) {

                final boolean isSelectedStrategyMonth = calendar
                        .getDisplayStrategy() == Type.MONTH;
                final CalendarEvent event = isSelectedStrategyMonth ? getEventForMonth(
                        e.getX(), e.getY()) : getNotMonthEvent(e.getX(),
                        e.getY());

                if (e.getClickCount() == 1) {

                    final EventCollection events = EventCollectionRepository
                            .get(calendar);

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
                    calendar.getPopupMenu().show(DayContentPanel.this,
                            e.getX(), e.getY());
                } else {
//                for (final MouseListener ml : DayContentPanel.this.owner
//                        .getOwner().getMouseListeners()) {
//                    ml.mousePressed(e);
//                }
                    // Begin drag selection on empty space in DAY/WEEK
                    if (!isSelectedStrategyMonth && event == null && SwingUtilities.isLeftMouseButton(e)) {
                        // Initialize selection from press position
                        startDate = CalendarUtil.pixelToDate(owner.getDate(), e.getY(), getHeight());
                        startDate = CalendarUtil.roundDateToHalfAnHour(startDate, false);
                        endDate = CalendarUtil.roundDateToHalfAnHour(startDate, true);
                        startSelection = new Point(e.getX(), CalendarUtil.secondsToPixels(startDate, getHeight()));
                        endSelection = new Point(e.getX(), CalendarUtil.secondsToPixels(endDate, getHeight()));
                        // Prepare immediate tooltip behavior during drag
                        javax.swing.ToolTipManager ttm = javax.swing.ToolTipManager.sharedInstance();
                        if (!dragging) {
                            origInitialDelay = ttm.getInitialDelay();
                            origReshowDelay = ttm.getReshowDelay();
                            origDismissDelay = ttm.getDismissDelay();
                            ttm.setInitialDelay(0);
                            ttm.setReshowDelay(0);
                            ttm.setDismissDelay(Math.max(15000, origDismissDelay));
                        }
                        dragging = true;
                        String tipText = sdf.format(startDate) + " - " + sdf.format(endDate) + " Uhr";
                        setToolTipText(tipText);
                        showDragTip(e, tipText);
                        calendar.validate();
                        calendar.repaint();
                    }
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // Do not clear active drag selection when the pointer briefly exits due to overlays
                if (!dragging) {
                    startSelection = null;
                    endSelection = null;
                }
                hoverMonthCell = false;
                overAddButton = false;
                setCursor(Cursor.getDefaultCursor());
                hideDragTip();
                // Also restore tooltip timings if we leave while dragging
                if (dragging) {
                    javax.swing.ToolTipManager ttm = javax.swing.ToolTipManager.sharedInstance();
                    if (origInitialDelay >= 0) ttm.setInitialDelay(origInitialDelay);
                    if (origReshowDelay >= 0) ttm.setReshowDelay(origReshowDelay);
                    if (origDismissDelay >= 0) ttm.setDismissDelay(origDismissDelay);
                    dragging = false;
                }
                calendar.validate();
                calendar.repaint();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (calendar.getDisplayStrategy() == Type.MONTH) {
                    hoverMonthCell = true;
                    calendar.validate();
                    calendar.repaint();
                }
            }
        });

        addMouseMotionListener(new MouseAdapter() {

            final JCalendar calendar = DayContentPanel.this.owner
                    .getOwner();

            @Override
            public void mouseMoved(MouseEvent e) {
                // While actively dragging a selection, do not modify hover state
                if (dragging) return;
                final boolean isSelectedStrategyMonth = calendar
                        .getDisplayStrategy() == Type.MONTH;

                // Update add-button hover state for month view
                if (isSelectedStrategyMonth) {
                    hoverMonthCell = true;
                    Rectangle ab = getAddButtonBounds();
                    overAddButton = ab.contains(e.getPoint());
                    setCursor(overAddButton ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
                    // No tooltip for add button to keep UI minimal
                }

                final CalendarEvent event = isSelectedStrategyMonth ? getEventForMonth(
                        e.getX(), e.getY()) : getNotMonthEvent(e.getX(),
                        e.getY());

                if (event != null) {
                    startSelection = null;
                    endSelection = null;
                    calendar.validate();
                    calendar.repaint();
                    return;
                }
                startDate = CalendarUtil.pixelToDate(
                        owner.getDate(), e.getY(),
                        getHeight());
                startDate = CalendarUtil.roundDateToHalfAnHour(startDate, false);
                endDate = CalendarUtil.pixelToDate(owner.getDate(),
                        e.getY(), getHeight());
                endDate = CalendarUtil.roundDateToHalfAnHour(endDate, true);

                startSelection = new Point(e.getX(), CalendarUtil.secondsToPixels(startDate, getHeight()));

                checkEndSelectionAndRepaintCalendar(e, startDate, endDate);
            }

            /**
             * This method checks if the endDate is one day after the startDate
             * If true, then the full height is used for the endSelection to have the right results in the View
             * Else the calculated value would be 0 and therefore lead to a wrong painted selection
             * If false, then the normal calculated value is used
             * @param e the mouse event to get the clicked position
             * @param startDate the start date
             * @param endDate the end date
             */
            private void checkEndSelectionAndRepaintCalendar(MouseEvent e, Date startDate, Date endDate) {
                Calendar startCal = CalendarUtil.getCalendar(startDate, false);
                Calendar endCal = CalendarUtil.getCalendar(endDate, false);
                // If endDate's time is 00:00 -> date is theoretically on next day, then use full height
                if (endCal.get(Calendar.DAY_OF_MONTH) > startCal.get(Calendar.DAY_OF_MONTH)) {
                    endSelection = new Point(e.getX(), getHeight());
                } else {
                    endSelection = new Point(e.getX(), CalendarUtil.secondsToPixels(endDate, getHeight()));
                }

                calendar.validate();
                calendar.repaint();
            }

            @Override
            public void mouseDragged(final MouseEvent e) {
                if (startSelection == null)
                    return;
                if (e.getY() > startSelection.getY()) {
                    endDate = CalendarUtil.pixelToDate(owner.getDate(),
                            e.getY(), getHeight());
                    endDate = CalendarUtil.roundDateToHalfAnHour(endDate, true);

                    checkEndSelectionAndRepaintCalendar(e, startDate, endDate);

                    // Update tooltip live while dragging with current span
                    try {
                        // Ensure tooltip shows immediately during drag
                        if (!dragging) {
                            javax.swing.ToolTipManager ttm = javax.swing.ToolTipManager.sharedInstance();
                            origInitialDelay = ttm.getInitialDelay();
                            origReshowDelay = ttm.getReshowDelay();
                            origDismissDelay = ttm.getDismissDelay();
                            ttm.setInitialDelay(0);
                            ttm.setReshowDelay(0);
                            // keep dismiss delay generous so it stays visible while moving
                            ttm.setDismissDelay(Math.max(15000, origDismissDelay));
                            dragging = true;
                        }
                        String tipText = sdf.format(startDate) + " - " + sdf.format(endDate) + " Uhr";
                        setToolTipText(tipText);
                        showDragTip(e, tipText);
                    } catch (Exception ignore) {
                        // best effort; tooltip is non-critical
                    }
                }
            }
        });

        //Listener for tooltip
        addMouseMotionListener(new MouseAdapter() {

            final JCalendar calendar = DayContentPanel.this.owner
                    .getOwner();

            @Override
            public void mouseMoved(final MouseEvent e) {
                // Suppress separate hover tooltip updates during active drag
                if (dragging) return;
                super.mouseMoved(e);

                final boolean isSelectedStrategyMonth = calendar
                        .getDisplayStrategy() == Type.MONTH;
                final CalendarEvent event = isSelectedStrategyMonth ? getEventForMonth(
                        e.getX(), e.getY()) : getNotMonthEvent(e.getX(),
                        e.getY());

                if (event != null) {
                    setToolTipText(calendar.getTooltipFormater().format(event));
                } else {
                    List holidayEvents = EventCollectionRepository.get(calendar).getHolidayEvents(owner.getDate());
                    if (holidayEvents.isEmpty()) {
                        setToolTipText(sdf.format(startDate) + " - " + sdf.format(endDate));
                    } else {
                        setToolTipText(calendar.getTooltipFormater().format(holidayEvents));
                    }
                }

            }
        });

        // Wheel scroll within a month day cell when content overflows
        addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            @Override
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent e) {
                final JCalendar calendar = DayContentPanel.this.owner.getOwner();
                if (calendar.getDisplayStrategy() != Type.MONTH) {
                    // Forward wheel to enclosing scroll pane to allow vertical scrolling in DAY/WEEK
                    java.awt.Component anc = javax.swing.SwingUtilities.getAncestorOfClass(javax.swing.JScrollPane.class, DayContentPanel.this);
                    if (anc instanceof javax.swing.JScrollPane) {
                        javax.swing.JScrollPane sp = (javax.swing.JScrollPane) anc;
                        javax.swing.JScrollBar vbar = sp.getVerticalScrollBar();
                        int inc = Math.max(16, vbar.getUnitIncrement());
                        int delta = e.getWheelRotation() * inc * 3;
                        vbar.setValue(vbar.getValue() + delta);
                        e.consume();
                    }
                    return;
                }

                // Determine total content height for this day in month view
                final EventCollection eventsCollection = EventCollectionRepository.get(calendar);
                final Collection<CalendarEvent> events = eventsCollection.getEvents(owner.getDate());
                int rowHeight = 17; // step used in month drawing
                int marginTop = 2;
                int totalRows = 0;
                for (final CalendarEvent event : CalendarUtil.sortEvents(new ArrayList<>(events))) {
                    if (event.isHoliday()) continue;
                    totalRows++;
                }
                int totalHeight = marginTop + totalRows * rowHeight;
                int viewport = getHeight();
                int maxOffset = Math.max(0, totalHeight - viewport);
                if (maxOffset <= 0) {
                    // No overflow, let parent handle (month navigation)
                    return;
                }

                // Consume and apply local scrolling only when there is overflow
                int direction = e.getWheelRotation(); // 1 = down, -1 = up
                int step = rowHeight * 2; // scroll 2 rows per notch for speed
                if (e.isShiftDown()) step = rowHeight * 4; // accelerate
                if (e.isControlDown()) step = rowHeight; // precise
                monthScrollOffset = Math.max(0, Math.min(maxOffset, monthScrollOffset + direction * step));
                e.consume();
                calendar.validate();
                calendar.repaint();
            }
        });
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
        drawBackground((Graphics2D) g);
        if (owner.getOwner().getDisplayStrategy() != Type.MONTH) {
            drawCalendarEvents((Graphics2D) g);
        } else {
            drawCalendarEventsMonth((Graphics2D) g);
        }

        if (startSelection != null && endSelection != null) {
            g.setColor(new Color(173, 216, 230, 50));
            final int height = (int) (endSelection.getY() - startSelection
                    .getY());
            final int xStart = 0;
            final int width = getWidth();
            final int yStart = (int) (height > 0 ? startSelection.getY()
                    : endSelection.getY());
            g.fillRect(xStart, yStart, Math.abs(width), Math.abs(height));
        }
    }

    private void drawBackground(final Graphics2D graphics2d) {
        final int height = getHeight();
        final int width = getWidth();
        final JCalendar calendar = owner.getOwner();
        final Config config = calendar.getConfig();
        final Color outsideWorkingHoursColor = config
                .getOutsideWorkingHoursColor();
        final Color dayDisableBackgroundColor = config
                .getDayDisabledBackgroundColor();
        final int workingHoursRectHeight = config.getWorkingHoursStart() * 60;
        final int workingHoursEndRectYStart = config.getWorkingHoursEnd() * 60;
        final int workingHoursEndHeight = height - config.getWorkingHoursEnd()
                * 60;
        final boolean isSelectedStrategyMonth = calendar.getDisplayStrategy() == Type.MONTH;
        final List<CalendarEvent> holidays = EventCollectionRepository.get(calendar).getHolidayEvents(owner.getDate());

        if (isEnabled()) {
            if (!isSelectedStrategyMonth) {
                if (holidays.isEmpty()) {
                    graphics2d.setColor(outsideWorkingHoursColor);
                    graphics2d.fillRect(0, 0, width, workingHoursRectHeight);
                    graphics2d.fillRect(0, workingHoursEndRectYStart, width,
                            workingHoursEndHeight);
                }
            }

            if (holidays.size() > 0) {
                graphics2d.setColor(calendar.getConfig().getHolidayBgColor());
                graphics2d.fillRect(0, 0, width, height);

            }
        } else {
            if (isSelectedStrategyMonth) {
                graphics2d.setColor(dayDisableBackgroundColor);
                graphics2d.fillRect(0, 0, width, height);
            }
        }

        graphics2d.setColor(config.getLineColor());
        graphics2d.drawRect(0, 0, width, height);

        if (!isSelectedStrategyMonth) {
            int y = 0;

            for (int i = 0; i < 24; i++) {
                y += 60;
                graphics2d.setColor(config.getMiddleLineColor());
                graphics2d.drawLine(0, y - 30, getWidth(), y - 30);
                graphics2d.setColor(config.getLineColor());
                graphics2d.drawLine(0, y, getWidth(), y);
            }
        }

    }

    private void drawCalendarEvents(final Graphics2D graphics2d) {

        final EventCollection eventsCollection = EventCollectionRepository
                .get(owner.getOwner());
        final Collection<CalendarEvent> events = eventsCollection
                .getEvents(owner.getDate());

        final Map<CalendarEvent, List<CalendarEvent>> conflictingEvents = CalendarUtil
                .getConflicting(events);

        final Config config = owner.getOwner().getConfig();
        if (events.size() > 0) {
            
            JLabel dummyLabel = new JLabel();
            final Font font = new JLabel().getFont().deriveFont(dummyLabel.getFont().getStyle() & ~java.awt.Font.BOLD);
            
            for (final CalendarEvent event : CalendarUtil.sortEvents(new ArrayList<>(events))) {
                if (event.isAllDay() || event.isHoliday())
                    continue;
                Color bgColor = event.getType().getBackgroundColor();
                bgColor = bgColor == null ? config
                        .getEventDefaultBackgroundColor() : bgColor;
                Color fgColor = event.getType().getForegroundColor();
                fgColor = fgColor == null ? config
                        .getEventDefaultForegroundColor() : fgColor;

                graphics2d.setColor(!event.isSelected() ? bgColor : bgColor
                        .darker().darker());
                int eventStart = 0;

                final boolean isSameStartDay = CalendarUtil.isSameDay(
                        event.getStart(), owner.getDate());
                if (isSameStartDay) {
                    eventStart = CalendarUtil.secondsToPixels(event.getStart(),
                            getHeight());
                }

                int eventYEnd = getHeight();
                if (CalendarUtil.isSameDay(event.getEnd(), owner.getDate())) {
                    eventYEnd = CalendarUtil.secondsToPixels(event.getEnd(),
                            getHeight());
                }

                final int conflictIndex = conflictingEvents.get(event).indexOf(
                        event);
                int conflictingEventsSize = conflictingEvents.get(event)
                        .size();

                // start jens
                if (conflictingEventsSize == 0)
                    conflictingEventsSize = 1;
                // stop jens

                graphics2d.fillRoundRect(conflictIndex * (getWidth() - 4)
                                / conflictingEventsSize, eventStart, (getWidth() - 4)
                                / conflictingEventsSize - 2, eventYEnd - eventStart,
                        12, 12);
                final String eventString = sdf.format(event.getStart()) + " - "
                        + sdf.format(event.getEnd()) + " " + event.getSummary();

                graphics2d.setFont(font);
                graphics2d
                        .setColor(!event.isSelected() ? fgColor : Color.white);
                RenderingHints hints = new RenderingHints(
                RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics2d.setRenderingHints(hints);

                GraphicsUtil.drawString(graphics2d, eventString, conflictIndex
                                * (getWidth() - 4) / conflictingEventsSize + 3,
                        eventStart + 15, (getWidth() - 4)
                                / conflictingEventsSize - 3, eventYEnd
                                - eventStart);

            }
        }
    }

    private CalendarEvent getNotMonthEvent(final int x, final int y) {

        final EventCollection eventsCollection = EventCollectionRepository
                .get(owner.getOwner());
        final Collection<CalendarEvent> events = eventsCollection
                .getEvents(owner.getDate());

        final Map<CalendarEvent, List<CalendarEvent>> conflictingEvents = CalendarUtil
                .getConflicting(events);

        if (events.size() > 0) {
            for (final CalendarEvent event : CalendarUtil.sortEvents(new ArrayList<>(events))) {
                if (event.isAllDay() || event.isHoliday())
                    continue;

                int eventYStart = 0;
                final boolean isSameStartDay = CalendarUtil.isSameDay(
                        event.getStart(), owner.getDate());
                if (isSameStartDay) {
                    eventYStart = CalendarUtil.secondsToPixels(
                            event.getStart(), getHeight());
                }

                int eventYEnd = getHeight();
                if (CalendarUtil.isSameDay(event.getEnd(), owner.getDate())) {
                    eventYEnd = CalendarUtil.secondsToPixels(event.getEnd(),
                            getHeight());
                }

                final int conflictIndex = conflictingEvents.get(event).indexOf(
                        event);
                final int conflictingEventsSize = conflictingEvents.get(event)
                        .size();

                final int rectXStart = conflictIndex * (getWidth() - 4)
                        / conflictingEventsSize;
                final int rectYStart = eventYStart;

                final int rectWidth = (getWidth() - 4) / conflictingEventsSize
                        - 2;

                final int rectHeight = eventYEnd - eventYStart;

                final Rectangle r = new Rectangle(rectXStart, rectYStart,
                        rectWidth, rectHeight);
                if (r.contains(x, y)) {
                    return event;
                }
            }
        }
        return null;
    }

    private void drawCalendarEventsMonth(final Graphics2D graphics2d) {

        final EventCollection eventsCollection = EventCollectionRepository
                .get(owner.getOwner());
        final Collection<CalendarEvent> events = eventsCollection.getEvents(owner.getDate());
        // Collect non-holiday events in month view order
        java.util.List<CalendarEvent> monthEvents = new ArrayList<>();
        for (final CalendarEvent event : CalendarUtil.sortEvents(new ArrayList<>(events))) {
            if (!event.isHoliday()) monthEvents.add(event);
        }

        int rowHeight = 17;
        int marginTop = 2;
        int totalHeight = marginTop + monthEvents.size() * rowHeight;
        int viewport = getHeight();
        int maxOffset = Math.max(0, totalHeight - viewport);
        if (monthScrollOffset > maxOffset) monthScrollOffset = maxOffset;
        if (monthScrollOffset < 0) monthScrollOffset = 0;

        int pos = marginTop - monthScrollOffset;
        if (!monthEvents.isEmpty()) {
            final Config config = owner.getOwner().getConfig();
            for (final CalendarEvent event : monthEvents) {
                // Compute row Y with current offset
                int y = pos;
                int h = 15;
                // Only paint if visible (simple intersection check)
                if (y < viewport && (y + h) > 0) {
                    Color bgColor = event.getType().getBackgroundColor();
                    bgColor = bgColor == null ? config.getEventDefaultBackgroundColor() : bgColor;
                    Color fgColor = event.getType().getForegroundColor();
                    fgColor = fgColor == null ? config.getEventDefaultForegroundColor() : fgColor;

                    graphics2d.setColor(!event.isSelected() ? bgColor : bgColor.darker().darker());
                    graphics2d.fillRect(2, y, getWidth() - 4, h);

                    String eventString = event.isAllDay() ? event.getSummary()
                            : sdf.format(event.getStart()) + " " + sdf.format(event.getEnd()) + " " + event.getSummary();
                    int fontSize = Math.round(getHeight() * 0.5f);
                    fontSize = fontSize > 9 ? 9 : fontSize;

                    final Font font = new Font("Verdana", Font.BOLD, fontSize);
                    final FontMetrics metrics = graphics2d.getFontMetrics(font);
                    graphics2d.setFont(font);

                    graphics2d.setColor(!event.isSelected() ? fgColor : Color.white);
                    GraphicsUtil.drawTrimmedString(graphics2d, eventString, 6, y + (13 / 2 + metrics.getHeight() / 2) - 2, getWidth());
                }
                pos += rowHeight;
            }
        }

        // Visual hint overlays if there is more content
        if (maxOffset > 0) {
            Graphics2D g2 = (Graphics2D) graphics2d.create();
            // smoother gradients using LinearGradientPaint
            int fadeH = Math.min(18, viewport / 3);
            // top fade
            if (monthScrollOffset > 0) {
                java.awt.geom.Point2D p1 = new java.awt.geom.Point2D.Float(0, 0);
                java.awt.geom.Point2D p2 = new java.awt.geom.Point2D.Float(0, fadeH);
                float[] dist = {0f, 1f};
                Color[] cols = {new Color(255, 255, 255, 220), new Color(255, 255, 255, 0)};
                java.awt.LinearGradientPaint gp = new java.awt.LinearGradientPaint(p1, p2, dist, cols);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), fadeH);
            }
            // bottom fade
            if (monthScrollOffset < maxOffset) {
                int y0 = viewport - fadeH;
                java.awt.geom.Point2D p1b = new java.awt.geom.Point2D.Float(0, y0);
                java.awt.geom.Point2D p2b = new java.awt.geom.Point2D.Float(0, viewport);
                float[] distb = {0f, 1f};
                Color[] colsb = {new Color(255, 255, 255, 0), new Color(255, 255, 255, 220)};
                java.awt.LinearGradientPaint gp2 = new java.awt.LinearGradientPaint(p1b, p2b, distb, colsb);
                g2.setPaint(gp2);
                g2.fillRect(0, y0, getWidth(), fadeH);
            }
            g2.dispose();

            // Draw "+N" indicator bottom-right for offscreen events
            int hiddenTop = monthScrollOffset / rowHeight;
            int hiddenBottomPx = totalHeight - (monthScrollOffset + viewport);
            int hiddenBottom = hiddenBottomPx > 0 ? (int) Math.ceil(hiddenBottomPx / (double) rowHeight) : 0;
            int hiddenTotal = hiddenTop + hiddenBottom;
            if (hiddenTotal > 0) {
                String more = "+" + hiddenTotal;
                Font f = graphics2d.getFont().deriveFont(Font.PLAIN, 9f);
                FontMetrics fm = graphics2d.getFontMetrics(f);
                int tw = fm.stringWidth(more);
                int th = fm.getHeight();
                int pad = 3;
                int boxW = tw + pad * 2;
                int boxH = th;
                int x = getWidth() - boxW - 2;
                int y = viewport - boxH - 2;
                graphics2d.setColor(new Color(0, 0, 0, 90));
                graphics2d.fillRoundRect(x, y, boxW, boxH, 8, 8);
                graphics2d.setColor(Color.white);
                graphics2d.setFont(f);
                graphics2d.drawString(more, x + pad, y + th - fm.getDescent());
            }
        }

        // Add-button overlay (hover only) in top-right corner
        final JCalendar calendar = DayContentPanel.this.owner.getOwner();
        if (calendar.getDisplayStrategy() == Type.MONTH && hoverMonthCell) {
            Rectangle ab = getAddButtonBounds();
            Graphics2D g2 = (Graphics2D) graphics2d.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0, 0, 0, overAddButton ? 160 : 120));
            g2.fillRoundRect(ab.x, ab.y, ab.width, ab.height, 10, 10);
            g2.setColor(new Color(255, 255, 255, 230));
            int cx = ab.x + ab.width / 2;
            int cy = ab.y + ab.height / 2;
            int arm = Math.max(5, ab.width / 3);
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(cx - arm, cy, cx + arm, cy);
            g2.drawLine(cx, cy - arm, cx, cy + arm);
            g2.dispose();
        }
    }

    // Resets the month scroll offset; invoked by parent when date changes
    public void resetMonthScroll() {
        this.monthScrollOffset = 0;
    }

    private CalendarEvent getEventForMonth(final int x, final int y) {

        final EventCollection eventsCollection = EventCollectionRepository
                .get(owner.getOwner());
        final Collection<CalendarEvent> events = eventsCollection
                .getEvents(owner.getDate());

        int pos = 2;
        if (events.size() > 0) {
            for (final CalendarEvent event : CalendarUtil.sortEvents(new ArrayList<>(events))) {

                if (event.isHoliday())
                    continue;

                final int rectXStart = 2;
                final int rectYStart = pos - monthScrollOffset;

                final int rectWidth = getWidth() - 4;

                final int rectHeight = 15;

                final Rectangle r = new Rectangle(rectXStart, rectYStart,
                        rectWidth, rectHeight);
                if (r.contains(x, y)) {
                    return event;
                }

                pos += 17;

            }
        }
        return null;
    }

    private Rectangle getAddButtonBounds() {
        int pad = 3;
        int w = Math.min(ADD_BTN_SIZE, Math.max(14, getWidth() / 8));
        int h = w;
        int x = getWidth() - w - pad;
        int y = pad;
        return new Rectangle(x, y, w, h);
    }

    // Show a lightweight popup near the cursor with the current timespan
    private void showDragTip(java.awt.event.MouseEvent e, String text) {
        try {
            if (dragTipLabel == null) {
                dragTipLabel = new JLabel(text);
                dragTipLabel.setOpaque(true);
                Color bg = UIManager.getColor("ToolTip.background");
                Color fg = UIManager.getColor("ToolTip.foreground");
                if (bg == null) bg = new Color(255, 255, 220);
                if (fg == null) fg = Color.black;
                dragTipLabel.setBackground(bg);
                dragTipLabel.setForeground(fg);
                dragTipLabel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(120, 120, 120)),
                        BorderFactory.createEmptyBorder(3, 6, 3, 6)
                ));
            } else {
                dragTipLabel.setText(text);
            }

            if (dragPopup != null) {
                dragPopup.hide();
                dragPopup = null;
            }
            java.awt.Point screen = new java.awt.Point(e.getX() + 12, e.getY() + 12);
            javax.swing.SwingUtilities.convertPointToScreen(screen, this);
            dragPopup = javax.swing.PopupFactory.getSharedInstance().getPopup(this, dragTipLabel, screen.x, screen.y);
            dragPopup.show();
        } catch (Exception ignore) {
            // non-fatal
        }
    }

    private void hideDragTip() {
        try {
            if (dragPopup != null) {
                dragPopup.hide();
                dragPopup = null;
            }
        } catch (Exception ignore) {
        }
    }

}
