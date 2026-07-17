package org.jlawyer.io.rest.v8;
import org.jlawyer.io.rest.tools.RestErrorResponses;

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
import javax.ws.rs.DELETE;
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
@io.swagger.annotations.Api(tags={"Timesheets"})
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
    @io.swagger.annotations.ApiOperation(value="Returns all open timesheets across all cases", response=org.jlawyer.io.rest.v8.pojo.RestfulTimesheetV8.class, responseContainer="List")
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
            return RestErrorResponses.serverError(ex);
        }
    }

    /**
     * Returns all timesheets of a case, both open and closed (unlike the
     * v7 /cases/{id}/timesheets endpoint which returns only open ones).
     *
     * @param caseId case (archive file) ID
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/cases/{caseId}")
    @RolesAllowed({"readArchiveFileRole"})
    @io.swagger.annotations.ApiOperation(value="Returns all timesheets of a case, both open and closed", response=org.jlawyer.io.rest.v8.pojo.RestfulTimesheetV8.class, responseContainer="List")
    public Response getCaseTimesheets(@PathParam("caseId") String caseId) {
        try {
            InitialContext ic = new InitialContext();
            ArchiveFileServiceLocal cases = (ArchiveFileServiceLocal) ic.lookup(LOOKUP_CASES);
            List<Timesheet> timesheets = cases.getTimesheets(caseId);

            ArrayList<RestfulTimesheetV8> resultList = new ArrayList<>();
            for (Timesheet ts : timesheets) {
                resultList.add(RestfulTimesheetV8.fromTimesheet(ts));
            }

            return Response.ok(resultList).build();
        } catch (Exception ex) {
            log.error("can not get timesheets for case " + caseId, ex);
            return RestErrorResponses.serverError(ex);
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
    @io.swagger.annotations.ApiOperation(value="Returns a single timesheet by ID", response=org.jlawyer.io.rest.v8.pojo.RestfulTimesheetV8.class)
    @io.swagger.annotations.ApiResponses({@io.swagger.annotations.ApiResponse(code=404, message="Not Found")})
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
            return RestErrorResponses.serverError(ex);
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
    @io.swagger.annotations.ApiOperation(value="Returns all positions for a given timesheet", response=org.jlawyer.io.rest.v8.pojo.RestfulTimesheetPositionV8.class, responseContainer="List")
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
            return RestErrorResponses.serverError(ex);
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
    @io.swagger.annotations.ApiOperation(value="Returns the allowed position templates for a given timesheet", response=org.jlawyer.io.rest.v8.pojo.RestfulTimesheetPositionTemplateV8.class, responseContainer="List")
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
            return RestErrorResponses.serverError(ex);
        }
    }

    /**
     * Returns the global pool of position templates (name, description, hourly rate, tax rate) that
     * can be assigned as the allowed positions of a timesheet.
     *
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/templates")
    @RolesAllowed({"readArchiveFileRole"})
    @io.swagger.annotations.ApiOperation(value="Returns all configured position templates (the global pool)", response=org.jlawyer.io.rest.v8.pojo.RestfulTimesheetPositionTemplateV8.class, responseContainer="List")
    public Response getAllTemplates() {
        try {
            InitialContext ic = new InitialContext();
            TimesheetServiceLocal tsService = (TimesheetServiceLocal) ic.lookup(LOOKUP_TIMESHEETS);
            List<TimesheetPositionTemplate> templates = tsService.getAllTimesheetPositionTemplates();

            ArrayList<RestfulTimesheetPositionTemplateV8> resultList = new ArrayList<>();
            for (TimesheetPositionTemplate tpl : templates) {
                resultList.add(RestfulTimesheetPositionTemplateV8.fromTemplate(tpl));
            }

            return Response.ok(resultList).build();
        } catch (Exception ex) {
            log.error("can not get all position templates", ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    /**
     * Creates a global timesheet position template. Requires administrator permission.
     *
     * @param template the template to create
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @PUT
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/templates")
    @RolesAllowed({"adminRole"})
    @io.swagger.annotations.ApiOperation(value = "Creates a global timesheet position template. Requires administrator permission.", response = org.jlawyer.io.rest.v8.pojo.RestfulTimesheetPositionTemplateV8.class)
    public Response createTemplate(@io.swagger.annotations.ApiParam RestfulTimesheetPositionTemplateV8 template) {
        try {
            InitialContext ic = new InitialContext();
            TimesheetServiceLocal tsService = (TimesheetServiceLocal) ic.lookup(LOOKUP_TIMESHEETS);
            TimesheetPositionTemplate created = tsService.addTimesheetPositionTemplate(template.toTemplate());
            return Response.ok(RestfulTimesheetPositionTemplateV8.fromTemplate(created)).build();
        } catch (Exception ex) {
            log.error("can not create position template", ex);
            return Response.status(Response.Status.CONFLICT).entity(ex.getMessage()).build();
        }
    }

    /**
     * Updates a global timesheet position template. Requires administrator permission.
     *
     * @param template the template to update
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @POST
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/templates")
    @RolesAllowed({"adminRole"})
    @io.swagger.annotations.ApiOperation(value = "Updates a global timesheet position template. Requires administrator permission.", response = org.jlawyer.io.rest.v8.pojo.RestfulTimesheetPositionTemplateV8.class)
    public Response updateTemplate(@io.swagger.annotations.ApiParam RestfulTimesheetPositionTemplateV8 template) {
        try {
            InitialContext ic = new InitialContext();
            TimesheetServiceLocal tsService = (TimesheetServiceLocal) ic.lookup(LOOKUP_TIMESHEETS);
            TimesheetPositionTemplate updated = tsService.updateTimesheetPositionTemplate(template.toTemplate());
            return Response.ok(RestfulTimesheetPositionTemplateV8.fromTemplate(updated)).build();
        } catch (Exception ex) {
            log.error("can not update position template", ex);
            return Response.status(Response.Status.CONFLICT).entity(ex.getMessage()).build();
        }
    }

    /**
     * Deletes a global timesheet position template. Requires administrator permission.
     *
     * @param templateId the id of the template to delete
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @DELETE
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/templates/{templateId}")
    @RolesAllowed({"adminRole"})
    @io.swagger.annotations.ApiOperation(value = "Deletes a global timesheet position template. Requires administrator permission.")
    public Response deleteTemplate(@PathParam("templateId") String templateId) {
        try {
            InitialContext ic = new InitialContext();
            TimesheetServiceLocal tsService = (TimesheetServiceLocal) ic.lookup(LOOKUP_TIMESHEETS);
            RestfulTimesheetPositionTemplateV8 ref = new RestfulTimesheetPositionTemplateV8();
            ref.setId(templateId);
            tsService.removeTimesheetPositionTemplate(ref.toTemplate());
            return Response.ok().build();
        } catch (Exception ex) {
            log.error("can not delete position template", ex);
            return Response.status(Response.Status.CONFLICT).entity(ex.getMessage()).build();
        }
    }

    /**
     * Sets the allowed position templates for a timesheet (project), restricting which positions /
     * hourly rates can be booked. Only the template ids in the body are used. An empty list clears
     * the restriction.
     *
     * @param timesheetId timesheet ID
     * @param templates the allowed position templates (only their ids are read)
     * @response 401 User not authorized
     * @response 403 User not authenticated
     * @response 404 Timesheet not found
     */
    @Override
    @PUT
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{timesheetId}/templates")
    @RolesAllowed({"writeArchiveFileRole"})
    @io.swagger.annotations.ApiOperation(value="Sets the allowed position templates (project positions / hourly rates) for a timesheet")
    @io.swagger.annotations.ApiResponses({@io.swagger.annotations.ApiResponse(code=404, message="Not Found")})
    public Response setTimesheetTemplates(@PathParam("timesheetId") String timesheetId, @io.swagger.annotations.ApiParam List<RestfulTimesheetPositionTemplateV8> templates) {
        try {
            InitialContext ic = new InitialContext();
            TimesheetServiceLocal tsService = (TimesheetServiceLocal) ic.lookup(LOOKUP_TIMESHEETS);

            ArrayList<TimesheetPositionTemplate> list = new ArrayList<>();
            if (templates != null) {
                for (RestfulTimesheetPositionTemplateV8 t : templates) {
                    TimesheetPositionTemplate tpl = new TimesheetPositionTemplate();
                    tpl.setId(t.getId());
                    list.add(tpl);
                }
            }
            tsService.setPositionTemplatesForTimesheet(timesheetId, list);
            return Response.ok().build();
        } catch (Exception ex) {
            log.error("can not set allowed templates for timesheet " + timesheetId, ex);
            return RestErrorResponses.serverError(ex);
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
    @io.swagger.annotations.ApiOperation(value="Returns the currently running timesheet positions for the authenticated user", response=org.jlawyer.io.rest.v8.pojo.RestfulTimesheetPositionV8.class, responseContainer="List")
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
            return RestErrorResponses.serverError(ex);
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
    @io.swagger.annotations.ApiOperation(value="Returns the number of currently running timesheet positions for the authenticated user", response=Integer.class)
    public Response getRunningPositionsCount() {
        try {
            String principal = securityContext.getUserPrincipal().getName();
            InitialContext ic = new InitialContext();
            ArchiveFileServiceLocal cases = (ArchiveFileServiceLocal) ic.lookup(LOOKUP_CASES);
            int count = cases.hasOpenTimesheetPositions(principal);

            return Response.ok(count).build();
        } catch (Exception ex) {
            log.error("can not get running timesheet positions count", ex);
            return RestErrorResponses.serverError(ex);
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
    @io.swagger.annotations.ApiOperation(value="Returns the last timesheet positions of the authenticated user for a given case", response=org.jlawyer.io.rest.v8.pojo.RestfulTimesheetPositionV8.class, responseContainer="List")
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
            return RestErrorResponses.serverError(ex);
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
    @io.swagger.annotations.ApiOperation(value="Starts a new timesheet position (stopwatch). The position will be created with the current time as start time. The server automatically sets id, started, stopped, principal and invoice. Only the following JSON attributes are required in the request body: name, description, unitPrice, taxRate.", response=org.jlawyer.io.rest.v8.pojo.RestfulTimesheetPositionV8.class)
    @io.swagger.annotations.ApiResponses({@io.swagger.annotations.ApiResponse(code=404, message="Not Found")})
    public Response startPosition(@PathParam("timesheetId") String timesheetId, @io.swagger.annotations.ApiParam RestfulTimesheetPositionV8 position) {
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
            return RestErrorResponses.serverError(ex);
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
    @io.swagger.annotations.ApiOperation(value="Stops a currently running timesheet position. The stop time is set automatically by the server. Only the following JSON attributes are required in the request body: name, description, unitPrice, taxRate. The position ID is taken from the URL path parameter.", response=org.jlawyer.io.rest.v8.pojo.RestfulTimesheetPositionV8.class)
    @io.swagger.annotations.ApiResponses({@io.swagger.annotations.ApiResponse(code=404, message="Not Found")})
    public Response stopPosition(@PathParam("timesheetId") String timesheetId, @PathParam("positionId") String positionId, @io.swagger.annotations.ApiParam RestfulTimesheetPositionV8 position) {
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
            return RestErrorResponses.serverError(ex);
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
    @io.swagger.annotations.ApiOperation(value="Adds a manual timesheet position with explicit start and stop times", response=org.jlawyer.io.rest.v8.pojo.RestfulTimesheetPositionV8.class)
    @io.swagger.annotations.ApiResponses({@io.swagger.annotations.ApiResponse(code=404, message="Not Found")})
    public Response addPosition(@PathParam("timesheetId") String timesheetId, @io.swagger.annotations.ApiParam RestfulTimesheetPositionV8 position) {
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
            return RestErrorResponses.serverError(ex);
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
    @io.swagger.annotations.ApiOperation(value="Updates an existing timesheet position", response=org.jlawyer.io.rest.v8.pojo.RestfulTimesheetPositionV8.class)
    @io.swagger.annotations.ApiResponses({@io.swagger.annotations.ApiResponse(code=404, message="Not Found")})
    public Response updatePosition(@PathParam("timesheetId") String timesheetId, @PathParam("positionId") String positionId, @io.swagger.annotations.ApiParam RestfulTimesheetPositionV8 position) {
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
            return RestErrorResponses.serverError(ex);
        }
    }

    /**
     * Deletes an existing timesheet position
     *
     * @param timesheetId timesheet ID
     * @param positionId position ID
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @DELETE
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/{timesheetId}/positions/{positionId}")
    @RolesAllowed({"writeArchiveFileRole"})
    @io.swagger.annotations.ApiOperation(value="Deletes an existing timesheet position")
    @io.swagger.annotations.ApiResponses({@io.swagger.annotations.ApiResponse(code=404, message="Not Found")})
    public Response deletePosition(@PathParam("timesheetId") String timesheetId, @PathParam("positionId") String positionId) {
        try {
            InitialContext ic = new InitialContext();
            ArchiveFileServiceLocal cases = (ArchiveFileServiceLocal) ic.lookup(LOOKUP_CASES);
            Timesheet ts = cases.getTimesheet(timesheetId);
            if (ts == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            TimesheetPosition pos = new TimesheetPosition();
            pos.setId(positionId);
            cases.removeTimesheetPosition(timesheetId, pos);

            return Response.ok().build();
        } catch (Exception ex) {
            log.error("can not delete timesheet position " + positionId, ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    /**
     * Creates a new timesheet (project) for a case
     *
     * @param caseId case (archive file) ID
     * @param timesheet timesheet data (name required)
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @POST
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/cases/{caseId}")
    @RolesAllowed({"writeArchiveFileRole"})
    @io.swagger.annotations.ApiOperation(value="Creates a new timesheet for a case", response=org.jlawyer.io.rest.v8.pojo.RestfulTimesheetV8.class)
    public Response addTimesheet(@PathParam("caseId") String caseId, @io.swagger.annotations.ApiParam RestfulTimesheetV8 timesheet) {
        try {
            InitialContext ic = new InitialContext();
            ArchiveFileServiceLocal cases = (ArchiveFileServiceLocal) ic.lookup(LOOKUP_CASES);
            Timesheet result = cases.addTimesheet(caseId, timesheet.toTimesheet());
            return Response.ok(RestfulTimesheetV8.fromTimesheet(result)).build();
        } catch (Exception ex) {
            log.error("can not add timesheet for case " + caseId, ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    /**
     * Updates an existing timesheet (project)
     *
     * @param timesheetId timesheet ID
     * @param timesheet updated timesheet data (name required)
     * @response 401 User not authorized
     * @response 403 User not authenticated
     * @response 404 Timesheet not found
     */
    @Override
    @PUT
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{timesheetId}")
    @RolesAllowed({"writeArchiveFileRole"})
    @io.swagger.annotations.ApiOperation(value="Updates an existing timesheet", response=org.jlawyer.io.rest.v8.pojo.RestfulTimesheetV8.class)
    @io.swagger.annotations.ApiResponses({@io.swagger.annotations.ApiResponse(code=404, message="Not Found")})
    public Response updateTimesheet(@PathParam("timesheetId") String timesheetId, @io.swagger.annotations.ApiParam RestfulTimesheetV8 timesheet) {
        try {
            InitialContext ic = new InitialContext();
            ArchiveFileServiceLocal cases = (ArchiveFileServiceLocal) ic.lookup(LOOKUP_CASES);
            Timesheet existing = cases.getTimesheet(timesheetId);
            if (existing == null || existing.getArchiveFileKey() == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            Timesheet toSave = timesheet.toTimesheet();
            toSave.setId(timesheetId);
            Timesheet result = cases.updateTimesheet(existing.getArchiveFileKey().getId(), toSave);
            return Response.ok(RestfulTimesheetV8.fromTimesheet(result)).build();
        } catch (Exception ex) {
            log.error("can not update timesheet " + timesheetId, ex);
            return RestErrorResponses.serverError(ex);
        }
    }

    /**
     * Deletes an existing timesheet (project) including its positions
     *
     * @param timesheetId timesheet ID
     * @response 401 User not authorized
     * @response 403 User not authenticated
     */
    @Override
    @DELETE
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Path("/{timesheetId}")
    @RolesAllowed({"writeArchiveFileRole"})
    @io.swagger.annotations.ApiOperation(value="Deletes an existing timesheet including its positions")
    public Response deleteTimesheet(@PathParam("timesheetId") String timesheetId) {
        try {
            InitialContext ic = new InitialContext();
            ArchiveFileServiceLocal cases = (ArchiveFileServiceLocal) ic.lookup(LOOKUP_CASES);
            cases.removeTimesheet(timesheetId);
            return Response.ok().build();
        } catch (Exception ex) {
            log.error("can not delete timesheet " + timesheetId, ex);
            return RestErrorResponses.serverError(ex);
        }
    }

}
