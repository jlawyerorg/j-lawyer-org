package org.jlawyer.io.rest.v8;

import com.jdimension.jlawyer.persistence.Timesheet;
import com.jdimension.jlawyer.persistence.TimesheetPosition;
import com.jdimension.jlawyer.persistence.TimesheetPositionTemplate;
import com.jdimension.jlawyer.services.ArchiveFileServiceLocal;
import com.jdimension.jlawyer.services.TimesheetServiceLocal;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.jboss.logging.Logger;
import org.jlawyer.io.rest.v8.pojo.RestfulTimesheetPositionTemplateV8;
import org.jlawyer.io.rest.v8.pojo.RestfulTimesheetPositionV8;
import org.jlawyer.io.rest.v8.pojo.RestfulTimesheetV8;

@Stateless
@Path("/v8/timesheets")
@Consumes({"application/json"})
@Produces({"application/json"})
public class TimesheetsEndpointV8 implements TimesheetsEndpointLocalV8 {

    private static final Logger log = Logger.getLogger(TimesheetsEndpointV8.class.getName());
    private static final String LOOKUP_CASES = "java:global/j-lawyer-server/j-lawyer-server-ejb/ArchiveFileService!com.jdimension.jlawyer.services.ArchiveFileServiceLocal";
    private static final String LOOKUP_TIMESHEETS = "java:global/j-lawyer-server/j-lawyer-server-ejb/TimesheetService!com.jdimension.jlawyer.services.TimesheetServiceLocal";

    @Context
    private SecurityContext securityContext;

    /**
     * Returns all open timesheets across all cases
     *
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/open")
    @RolesAllowed({"readArchiveFileRole"})
    public Response getOpenTimesheets() {
        try {
            InitialContext ic = new InitialContext();
            ArchiveFileServiceLocal cases = (ArchiveFileServiceLocal) ic.lookup(LOOKUP_CASES);
            List<Timesheet> timesheets = cases.getOpenTimesheets();

            ArrayList<RestfulTimesheetV8> resultList = new ArrayList<>();
            for (Timesheet ts : timesheets) {
                resultList.add(RestfulTimesheetV8.fromTimesheet(ts));
            }

            return Response.ok(resultList).build();
        } catch (Exception ex) {
            log.error("can not get open timesheets", ex);
            return Response.serverError().build();
        }
    }

    /**
     * Returns a single timesheet by ID
     *
     * @param timesheetId timesheet ID
     * @response 401 User not authorized
     * @response 403 User not authenticated
     * @response 404 Timesheet not found
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/{timesheetId}")
    @RolesAllowed({"readArchiveFileRole"})
    public Response getTimesheet(@PathParam("timesheetId") String timesheetId) {
        try {
            InitialContext ic = new InitialContext();
            ArchiveFileServiceLocal cases = (ArchiveFileServiceLocal) ic.lookup(LOOKUP_CASES);
            Timesheet ts = cases.getTimesheet(timesheetId);
            if (ts == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            return Response.ok(RestfulTimesheetV8.fromTimesheet(ts)).build();
        } catch (Exception ex) {
            log.error("can not get timesheet " + timesheetId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Returns all positions for a given timesheet
     *
     * @param timesheetId timesheet ID
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/{timesheetId}/positions")
    @RolesAllowed({"readArchiveFileRole"})
    public Response getTimesheetPositions(@PathParam("timesheetId") String timesheetId) {
        try {
            InitialContext ic = new InitialContext();
            ArchiveFileServiceLocal cases = (ArchiveFileServiceLocal) ic.lookup(LOOKUP_CASES);
            List<TimesheetPosition> positions = cases.getTimesheetPositions(timesheetId);

            ArrayList<RestfulTimesheetPositionV8> resultList = new ArrayList<>();
            for (TimesheetPosition pos : positions) {
                resultList.add(RestfulTimesheetPositionV8.fromTimesheetPosition(pos));
            }

            return Response.ok(resultList).build();
        } catch (Exception ex) {
            log.error("can not get positions for timesheet " + timesheetId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Returns the allowed position templates for a given timesheet
     *
     * @param timesheetId timesheet ID
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/{timesheetId}/templates")
    @RolesAllowed({"readArchiveFileRole"})
    public Response getTimesheetTemplates(@PathParam("timesheetId") String timesheetId) {
        try {
            InitialContext ic = new InitialContext();
            TimesheetServiceLocal tsService = (TimesheetServiceLocal) ic.lookup(LOOKUP_TIMESHEETS);
            List<TimesheetPositionTemplate> templates = tsService.getPositionTemplatesForTimesheet(timesheetId);

            ArrayList<RestfulTimesheetPositionTemplateV8> resultList = new ArrayList<>();
            for (TimesheetPositionTemplate tpl : templates) {
                resultList.add(RestfulTimesheetPositionTemplateV8.fromTemplate(tpl));
            }

            return Response.ok(resultList).build();
        } catch (Exception ex) {
            log.error("can not get templates for timesheet " + timesheetId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Returns the currently running timesheet positions for the authenticated user
     *
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/positions/running")
    @RolesAllowed({"readArchiveFileRole"})
    public Response getRunningPositions() {
        try {
            String principal = securityContext.getUserPrincipal().getName();
            InitialContext ic = new InitialContext();
            ArchiveFileServiceLocal cases = (ArchiveFileServiceLocal) ic.lookup(LOOKUP_CASES);
            List<TimesheetPosition> positions = cases.getOpenTimesheetPositions(principal);

            ArrayList<RestfulTimesheetPositionV8> resultList = new ArrayList<>();
            for (TimesheetPosition pos : positions) {
                resultList.add(RestfulTimesheetPositionV8.fromTimesheetPosition(pos));
            }

            return Response.ok(resultList).build();
        } catch (Exception ex) {
            log.error("can not get running timesheet positions", ex);
            return Response.serverError().build();
        }
    }

    /**
     * Returns the number of currently running timesheet positions for the authenticated user
     *
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/positions/running/count")
    @RolesAllowed({"readArchiveFileRole"})
    public Response getRunningPositionsCount() {
        try {
            String principal = securityContext.getUserPrincipal().getName();
            InitialContext ic = new InitialContext();
            ArchiveFileServiceLocal cases = (ArchiveFileServiceLocal) ic.lookup(LOOKUP_CASES);
            int count = cases.hasOpenTimesheetPositions(principal);

            return Response.ok(count).build();
        } catch (Exception ex) {
            log.error("can not get running timesheet positions count", ex);
            return Response.serverError().build();
        }
    }

    /**
     * Returns the last timesheet positions of the authenticated user for a given case
     *
     * @param caseId case ID
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/cases/{caseId}/positions/last")
    @RolesAllowed({"readArchiveFileRole"})
    public Response getLastPositionsForCase(@PathParam("caseId") String caseId) {
        try {
            String principal = securityContext.getUserPrincipal().getName();
            InitialContext ic = new InitialContext();
            ArchiveFileServiceLocal cases = (ArchiveFileServiceLocal) ic.lookup(LOOKUP_CASES);
            List<TimesheetPosition> positions = cases.getLastTimesheetPositions(caseId, principal);

            ArrayList<RestfulTimesheetPositionV8> resultList = new ArrayList<>();
            for (TimesheetPosition pos : positions) {
                resultList.add(RestfulTimesheetPositionV8.fromTimesheetPosition(pos));
            }

            return Response.ok(resultList).build();
        } catch (Exception ex) {
            log.error("can not get last timesheet positions for case " + caseId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Starts a new timesheet position (stopwatch). The position will be created
     * with the current time as start time. The server automatically sets id,
     * started, stopped, principal and invoice. Only the following JSON attributes
     * are required in the request body: name, description, unitPrice, taxRate.
     *
     * @param timesheetId timesheet ID
     * @param position position data (required fields: name, description, unitPrice, taxRate)
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @POST
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{timesheetId}/positions/start")
    @RolesAllowed({"writeArchiveFileRole"})
    public Response startPosition(@PathParam("timesheetId") String timesheetId, RestfulTimesheetPositionV8 position) {
        try {
            InitialContext ic = new InitialContext();
            ArchiveFileServiceLocal cases = (ArchiveFileServiceLocal) ic.lookup(LOOKUP_CASES);
            Timesheet ts = cases.getTimesheet(timesheetId);
            if (ts == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            TimesheetPosition pos = position.toTimesheetPosition(ts);
            TimesheetPosition result = cases.timesheetPositionStart(timesheetId, pos);

            return Response.ok(RestfulTimesheetPositionV8.fromTimesheetPosition(result)).build();
        } catch (Exception ex) {
            log.error("can not start timesheet position for timesheet " + timesheetId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Stops a currently running timesheet position. The stop time is set
     * automatically by the server. Only the following JSON attributes are
     * required in the request body: name, description, unitPrice, taxRate.
     * The position ID is taken from the URL path parameter.
     *
     * @param timesheetId timesheet ID
     * @param positionId position ID
     * @param position position data (required fields: name, description, unitPrice, taxRate)
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @PUT
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{timesheetId}/positions/{positionId}/stop")
    @RolesAllowed({"writeArchiveFileRole"})
    public Response stopPosition(@PathParam("timesheetId") String timesheetId, @PathParam("positionId") String positionId, RestfulTimesheetPositionV8 position) {
        try {
            InitialContext ic = new InitialContext();
            ArchiveFileServiceLocal cases = (ArchiveFileServiceLocal) ic.lookup(LOOKUP_CASES);
            Timesheet ts = cases.getTimesheet(timesheetId);
            if (ts == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            TimesheetPosition pos = position.toTimesheetPosition(ts);
            pos.setId(positionId);
            TimesheetPosition result = cases.timesheetPositionStop(timesheetId, pos);

            return Response.ok(RestfulTimesheetPositionV8.fromTimesheetPosition(result)).build();
        } catch (Exception ex) {
            log.error("can not stop timesheet position " + positionId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Adds a manual timesheet position with explicit start and stop times
     *
     * @param timesheetId timesheet ID
     * @param position position data
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @POST
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{timesheetId}/positions")
    @RolesAllowed({"writeArchiveFileRole"})
    public Response addPosition(@PathParam("timesheetId") String timesheetId, RestfulTimesheetPositionV8 position) {
        try {
            InitialContext ic = new InitialContext();
            ArchiveFileServiceLocal cases = (ArchiveFileServiceLocal) ic.lookup(LOOKUP_CASES);
            Timesheet ts = cases.getTimesheet(timesheetId);
            if (ts == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            TimesheetPosition pos = position.toTimesheetPosition(ts);
            TimesheetPosition result = cases.timesheetPositionAdd(timesheetId, pos);

            return Response.ok(RestfulTimesheetPositionV8.fromTimesheetPosition(result)).build();
        } catch (Exception ex) {
            log.error("can not add timesheet position for timesheet " + timesheetId, ex);
            return Response.serverError().build();
        }
    }

    /**
     * Updates an existing timesheet position
     *
     * @param timesheetId timesheet ID
     * @param positionId position ID
     * @param position updated position data
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @PUT
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{timesheetId}/positions/{positionId}")
    @RolesAllowed({"writeArchiveFileRole"})
    public Response updatePosition(@PathParam("timesheetId") String timesheetId, @PathParam("positionId") String positionId, RestfulTimesheetPositionV8 position) {
        try {
            InitialContext ic = new InitialContext();
            ArchiveFileServiceLocal cases = (ArchiveFileServiceLocal) ic.lookup(LOOKUP_CASES);
            Timesheet ts = cases.getTimesheet(timesheetId);
            if (ts == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            TimesheetPosition pos = position.toTimesheetPosition(ts);
            pos.setId(positionId);
            TimesheetPosition result = cases.timesheetPositionSave(timesheetId, pos);

            return Response.ok(RestfulTimesheetPositionV8.fromTimesheetPosition(result)).build();
        } catch (Exception ex) {
            log.error("can not update timesheet position " + positionId, ex);
            return Response.serverError().build();
        }
    }

}
