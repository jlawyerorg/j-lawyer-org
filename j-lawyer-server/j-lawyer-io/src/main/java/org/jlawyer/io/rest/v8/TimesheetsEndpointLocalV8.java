package org.jlawyer.io.rest.v8;

import java.util.List;
import javax.ejb.Local;
import javax.ws.rs.core.Response;
import org.jlawyer.io.rest.v8.pojo.RestfulTimesheetPositionTemplateV8;
import org.jlawyer.io.rest.v8.pojo.RestfulTimesheetPositionV8;
import org.jlawyer.io.rest.v8.pojo.RestfulTimesheetV8;

@Local
public interface TimesheetsEndpointLocalV8 {

    Response getOpenTimesheets();

    Response getCaseTimesheets(String caseId);

    Response getTimesheet(String timesheetId);

    Response getTimesheetPositions(String timesheetId);

    Response getTimesheetTemplates(String timesheetId);

    Response createTemplate(org.jlawyer.io.rest.v8.pojo.RestfulTimesheetPositionTemplateV8 template);

    Response updateTemplate(org.jlawyer.io.rest.v8.pojo.RestfulTimesheetPositionTemplateV8 template);

    Response deleteTemplate(String templateId);

    Response getRunningPositions();

    Response getRunningPositionsCount();

    Response getLastPositionsForCase(String caseId);

    Response startPosition(String timesheetId, RestfulTimesheetPositionV8 position);

    Response stopPosition(String timesheetId, String positionId, RestfulTimesheetPositionV8 position);

    Response addPosition(String timesheetId, RestfulTimesheetPositionV8 position);

    Response updatePosition(String timesheetId, String positionId, RestfulTimesheetPositionV8 position);

    Response deletePosition(String timesheetId, String positionId);

    /**
     * Creates a new timesheet (project) for a case.
     *
     * @param caseId the case (archive file) ID the timesheet belongs to
     * @param timesheet the timesheet data (name is required)
     * @return HTTP 200 with the created timesheet, or a server error response
     */
    Response addTimesheet(String caseId, RestfulTimesheetV8 timesheet);

    /**
     * Updates an existing timesheet (project).
     *
     * @param timesheetId the timesheet ID to update
     * @param timesheet the updated timesheet data (name is required)
     * @return HTTP 200 with the updated timesheet, 404 if not found, or a server error response
     */
    Response updateTimesheet(String timesheetId, RestfulTimesheetV8 timesheet);

    /**
     * Deletes an existing timesheet (project) including its positions.
     *
     * @param timesheetId the timesheet ID to delete
     * @return HTTP 200 on success, or a server error response
     */
    Response deleteTimesheet(String timesheetId);

    /**
     * Returns the global pool of position templates that can be assigned as a timesheet's allowed positions.
     *
     * @return HTTP 200 with the list of templates, or a server error response
     */
    Response getAllTemplates();

    /**
     * Sets the allowed position templates (project positions / hourly rates) for a timesheet.
     *
     * @param timesheetId the timesheet ID
     * @param templates the allowed position templates (only their ids are read); an empty list clears the restriction
     * @return HTTP 200 on success, or a server error response
     */
    Response setTimesheetTemplates(String timesheetId, List<RestfulTimesheetPositionTemplateV8> templates);

}
