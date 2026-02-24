/*
 *                     GNU AFFERO GENERAL PUBLIC LICENSE
 *                        Version 3, 19 November 2007
 *
 *  Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
 *  Everyone is permitted to copy and distribute verbatim copies
 *  of this license document, but changing it is not allowed.
 */
package com.jdimension.jlawyer.client.desktop;

import com.jdimension.jlawyer.client.settings.ClientSettings;
import com.jdimension.jlawyer.client.settings.UserSettings;
import com.jdimension.jlawyer.persistence.ArchiveFileReviewsBean;
import com.jdimension.jlawyer.server.constants.ArchiveFileConstants;

import com.jdimension.jlawyer.services.CalendarServiceRemote;
import com.jdimension.jlawyer.services.JLawyerServiceLocator;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.swing.SwingUtilities;
import org.apache.log4j.Logger;

/**
 * Timer task that checks for upcoming calendar event reminders and shows
 * non-modal notification dialogs. Only events of type EVENT (Termin) with
 * reminderMinutes >= 0 are considered. Reminders are shown only if the
 * logged-in user is the assignee or the assignee is empty/null.
 * Dismissed reminders are tracked in client memory only.
 */
public class ReminderNotificationTimerTask extends java.util.TimerTask {

    private static final Logger log = Logger.getLogger(ReminderNotificationTimerTask.class.getName());

    private final Set<String> dismissedEventIds = new HashSet<>();
    private volatile boolean stopped = false;

    public void stop() {
        stopped = true;
    }

    @Override
    public void run() {
        if (stopped) {
            return;
        }

        try {
            ClientSettings settings = ClientSettings.getInstance();
            JLawyerServiceLocator locator = JLawyerServiceLocator.getInstance(settings.getLookupProperties());

            if (stopped) return;

            CalendarServiceRemote calService = locator.lookupCalendarServiceRemote();

            // query only open events (Termine) starting within the next 24 hours
            // 1440 minutes is the maximum reminder lead time
            Calendar fromDate = Calendar.getInstance();
            Calendar toDate = Calendar.getInstance();
            toDate.add(Calendar.DAY_OF_MONTH, 1);

            Collection<ArchiveFileReviewsBean> openReviews = calService.searchReviews(
                    ArchiveFileConstants.REVIEWSTATUS_OPEN,
                    ArchiveFileConstants.REVIEWTYPE_EVENT,
                    fromDate.getTime(),
                    toDate.getTime());

            if (stopped || openReviews == null) return;

            String currentUser = UserSettings.getInstance().getCurrentUser().getPrincipalId();
            long now = System.currentTimeMillis();

            for (ArchiveFileReviewsBean rev : openReviews) {
                if (stopped) return;

                try {
                    // only events with a reminder configured
                    if (rev.getReminderMinutes() < 0) {
                        continue;
                    }

                    // only if the logged-in user is the assignee or assignee is empty
                    String assignee = rev.getAssignee();
                    if (assignee != null && !assignee.isEmpty() && !assignee.equals(currentUser)) {
                        continue;
                    }

                    // already dismissed in this session
                    if (dismissedEventIds.contains(rev.getId())) {
                        continue;
                    }

                    // check if now is within the reminder window
                    Date beginDate = rev.getBeginDate();
                    if (beginDate == null) {
                        continue;
                    }

                    long reminderTime = beginDate.getTime() - (rev.getReminderMinutes() * 60L * 1000L);
                    // grace period of 2 minutes past begin to account for timer interval
                    long gracePeriodMs = 2L * 60L * 1000L;
                    if (now >= reminderTime && now < beginDate.getTime() + gracePeriodMs) {
                        // reminder is due - show notification
                        dismissedEventIds.add(rev.getId());

                        final ArchiveFileReviewsBean reminder = rev;
                        SwingUtilities.invokeLater(() -> {
                            ReminderNotificationDialog.getInstance().addReminder(reminder);
                        });
                    }
                } catch (Exception ex) {
                    log.error("Error processing reminder for event " + rev.getId(), ex);
                }
            }
        } catch (Exception ex) {
            log.error("Error checking for event reminders", ex);
        }
    }
}
