/*
                    GNU AFFERO GENERAL PUBLIC LICENSE
                       Version 3, 19 November 2007

 Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>
 Everyone is permitted to copy and distribute verbatim copies
 of this license document, but changing it is not allowed.
*/
package com.jdimension.jlawyer.client.editors.files;

import com.jdimension.jlawyer.persistence.CalendarEntryTemplate;
import com.jdimension.jlawyer.persistence.CalendarSetup;
import java.util.Date;

/**
 * Backward-compatible extension of NewEventPanelListener to support
 * optional document context (documentId) for events.
 */
public interface NewEventPanelListenerV2 extends NewEventPanelListener {

    void addReview(CalendarEntryTemplate template,
                   int eventType,
                   String reason,
                   String description,
                   Date beginDate,
                   Date endDate,
                   String assignee,
                   String location,
                   CalendarSetup calSetup,
                   String documentId) throws Exception;
}
