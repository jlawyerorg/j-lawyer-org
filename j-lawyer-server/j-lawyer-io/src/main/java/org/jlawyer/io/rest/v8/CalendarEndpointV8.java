/*
 * Copyright (C) 2026 Jens Kutschke
 *
 * This file is part of j-lawyer.org.
 *
 * j-lawyer.org is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * j-lawyer.org is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with j-lawyer.org.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.jlawyer.io.rest.v8;
import org.jlawyer.io.rest.tools.RestErrorResponses;

import com.jdimension.jlawyer.persistence.ArchiveFileBean;
import com.jdimension.jlawyer.persistence.ArchiveFileReviewsBean;
import com.jdimension.jlawyer.server.constants.ArchiveFileConstants;
import com.jdimension.jlawyer.services.ArchiveFileServiceLocal;
import com.jdimension.jlawyer.services.CalendarServiceLocal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jlawyer.io.rest.v8.pojo.RestfulCalendarEventV8;

/**
 * v8 calendar view endpoint: returns the caller's follow-ups, deadlines and appointments
 * (ACL-restricted to accessible cases) within a date range, so a web agenda/calendar can be
 * rendered without a per-case fetch — OpenSpec change {@code add-web-client}, task 3.4.
 *
 * @author jens
 */
@Stateless
@Path("/v8/calendar")
@Consumes({"application/json"})
@Produces({"application/json"})
@io.swagger.annotations.Api(tags = {"Calendar"})
public class CalendarEndpointV8 implements CalendarEndpointLocalV8 {

    private static final Logger log = Logger.getLogger(CalendarEndpointV8.class.getName());
    private static final String LOOKUP_CALENDAR = "java:global/j-lawyer-server/j-lawyer-server-ejb/CalendarService!com.jdimension.jlawyer.services.CalendarServiceLocal";
    private static final String LOOKUP_CASES = "java:global/j-lawyer-server/j-lawyer-server-ejb/ArchiveFileService!com.jdimension.jlawyer.services.ArchiveFileServiceLocal";

    /**
     * Lists calendar entries (follow-ups, deadlines, appointments) for the caller within a range.
     *
     * @param from   inclusive start date as {@code yyyy-MM-dd} (optional; unbounded past if blank)
     * @param to     inclusive end date as {@code yyyy-MM-dd} (optional; unbounded future if blank)
     * @param type   one of {@code all} | {@code followup} | {@code respite} | {@code event}
     * @param status one of {@code all} | {@code open} | {@code done}
     * @param limit  maximum number of entries (default 2000, clamped 1..5000)
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/events")
    @RolesAllowed({"readArchiveFileRole"})
    @io.swagger.annotations.ApiOperation(value = "Lists calendar entries within a date range", response = RestfulCalendarEventV8.class, responseContainer = "List")
    public Response listEvents(
            @QueryParam("from") String from,
            @QueryParam("to") String to,
            @QueryParam("type") @DefaultValue("all") String type,
            @QueryParam("status") @DefaultValue("all") String status,
            @QueryParam("limit") @DefaultValue("2000") int limit) {
        try {
            int typeCode = ArchiveFileConstants.REVIEWTYPE_ANY;
            if ("followup".equalsIgnoreCase(type)) {
                typeCode = ArchiveFileConstants.REVIEWTYPE_FOLLOWUP;
            } else if ("respite".equalsIgnoreCase(type)) {
                typeCode = ArchiveFileConstants.REVIEWTYPE_RESPITE;
            } else if ("event".equalsIgnoreCase(type)) {
                typeCode = ArchiveFileConstants.REVIEWTYPE_EVENT;
            }

            int statusCode = ArchiveFileConstants.REVIEWSTATUS_ANY;
            if ("open".equalsIgnoreCase(status)) {
                statusCode = ArchiveFileConstants.REVIEWSTATUS_OPEN;
            } else if ("done".equalsIgnoreCase(status)) {
                statusCode = ArchiveFileConstants.REVIEWSTATUS_DONE;
            }

            Date fromDate = parseDate(from);
            Date toDate = parseDate(to);
            int effectiveLimit = limit <= 0 ? 2000 : Math.min(limit, 5000);

            InitialContext ic = new InitialContext();
            CalendarServiceLocal cal = (CalendarServiceLocal) ic.lookup(LOOKUP_CALENDAR);
            Collection<ArchiveFileReviewsBean> reviews = cal.searchReviews(statusCode, typeCode, fromDate, toDate, effectiveLimit);

            ArrayList<RestfulCalendarEventV8> items = new ArrayList<>();
            for (ArchiveFileReviewsBean rev : reviews) {
                items.add(RestfulCalendarEventV8.fromReview(rev));
            }
            return Response.ok(items).build();
        } catch (Exception ex) {
            log.error("Can not list calendar events", ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    /**
     * Deletes a calendar entry (follow-up, deadline or appointment) by its id. Access is checked
     * against the case the entry belongs to: the caller must be able to read that case, otherwise
     * the entry is treated as non-existent (404). Completes the calendar CRUD surface for the web
     * client — OpenSpec change {@code add-web-client}. Creation/update live at {@code /v6/cases/duedate/*}.
     *
     * @param id the calendar entry (review) id
     * @response 204 Entry deleted
     * @response 404 No such entry, or the caller cannot access the owning case
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @DELETE
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/events/{id}")
    @RolesAllowed({"writeArchiveFileRole"})
    @io.swagger.annotations.ApiOperation(value = "Deletes a calendar entry (follow-up, deadline or appointment) by id")
    public Response deleteEvent(@PathParam("id") String id) {
        try {
            if (id == null || id.trim().isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            InitialContext ic = new InitialContext();
            CalendarServiceLocal cal = (CalendarServiceLocal) ic.lookup(LOOKUP_CALENDAR);
            ArchiveFileServiceLocal cases = (ArchiveFileServiceLocal) ic.lookup(LOOKUP_CASES);

            ArchiveFileReviewsBean review = cal.getReview(id);
            if (review == null || review.getArchiveFileKey() == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            // ACL: getArchiveFile is restricted to cases the caller may access; null => no access.
            ArchiveFileBean owningCase = cases.getArchiveFile(review.getArchiveFileKey().getId());
            if (owningCase == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            cal.removeReview(id);
            return Response.noContent().build();
        } catch (Exception ex) {
            log.error("Can not delete calendar entry " + id, ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    /** Parses a {@code yyyy-MM-dd} date in the server's default time zone; null/blank -&gt; null. */
    private static Date parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
            fmt.setLenient(false);
            return fmt.parse(value.trim());
        } catch (Exception ex) {
            log.warn("Ignoring unparseable calendar date '" + value + "'");
            return null;
        }
    }
}
