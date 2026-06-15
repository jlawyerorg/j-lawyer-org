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
package de.costache.calendar.format;

import de.costache.calendar.model.CalendarEvent;

import java.text.SimpleDateFormat;
import java.util.Collection;

/**
 * The default formatter for the the tooltip text
 */
public class DefaultCalendarEventFormat implements CalendarEventFormat {

    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    @Override
    public String format(CalendarEvent calendarEvent) {
        StringBuilder formatted = new StringBuilder();
        formatted.append("<html>");
        formatted.append("<table>");

        formatted.append("<tr>");
        formatted.append("<td><b>Kalender: </b></td>");
        formatted.append("<td>").append(calendarEvent.getType().getName()).append("</td>");
        formatted.append("</tr>");
        
        formatted.append("<tr>");
        formatted.append("<td><b>Akte: </b></td>");
        formatted.append("<td>").append(calendarEvent.getCaseNumber()).append("</td>");
        formatted.append("</tr>");
        
        formatted.append("<tr>");
        formatted.append("<td></td>");
        formatted.append("<td>").append(calendarEvent.getCaseName()).append("</td>");
        formatted.append("</tr>");
        formatted.append("<tr>");
        formatted.append("<td></td>");
        formatted.append("<td>").append(calendarEvent.getCaseReason()).append("</td>");
        formatted.append("</tr>");
        formatted.append("<tr>");
        formatted.append("<td></td>");
        formatted.append("<td>").append("Anwalt: " + calendarEvent.getCaseLawyer()).append("</td>");
        formatted.append("</tr>");
        
        formatted.append("<tr>");
        formatted.append("<td><b>Grund: </b></td>");
        formatted.append("<td>").append(calendarEvent.getSummary()).append("</td>");
        formatted.append("</tr>");
        formatted.append("<tr>");
        formatted.append("<td><b>verantwortlich: </b></td>");
        formatted.append("<td>").append(calendarEvent.getAssignee()).append("</td>");
        formatted.append("</tr>");

        if (calendarEvent.getDescription() != null) {
            formatted.append("<tr>");
            formatted.append("<td><b>Beschreibung: </b></td>");
            formatted.append("<td>").append(calendarEvent.getDescription()).append("</td>");
            formatted.append("</tr>");
        }

        if (calendarEvent.getLocation() != null) {
            formatted.append("<tr>");
            formatted.append("<td><b>Ort: </b></td>");
            formatted.append("<td>").append(calendarEvent.getLocation()).append("</td>");
            formatted.append("</tr>");
        }

        formatted.append("<tr>");
        formatted.append("<td><b>Beginn: </b></td>");
        formatted.append("<td>").append(sdf.format(calendarEvent.getStart())).append("</td>");
        formatted.append("</tr>");

        formatted.append("<tr>");
        formatted.append("<td><b>Ende: </b></td>");
        formatted.append("<td>").append(sdf.format(calendarEvent.getEnd())).append("</td>");
        formatted.append("</tr>");

//        formatted.append("<tr>");
//        formatted.append("<td><b>Prioritt: </b></td>");
//        formatted.append("<td>").append(calendarEvent.getPriority()).append("</td>");
//        formatted.append("</tr>");

        formatted.append("</table>");
        formatted.append("</html>");
        return formatted.toString();
    }

    @Override
    public String format(Collection<CalendarEvent> holidays) {
        if (holidays.isEmpty()) {
            return null;
        }
        StringBuilder formatted = new StringBuilder();
        formatted.append("<html>");
        for (CalendarEvent event : holidays) {
            formatted.append("<b>").append(event.getSummary()).append("</b><br/>");
        }

        return formatted.toString();
    }


}
