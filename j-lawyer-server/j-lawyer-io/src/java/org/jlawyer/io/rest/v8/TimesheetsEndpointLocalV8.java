package org.jlawyer.io.rest.v8;

import javax.ejb.Local;
import javax.ws.rs.core.Response;
import org.jlawyer.io.rest.v8.pojo.RestfulTimesheetPositionV8;

@Local
public interface TimesheetsEndpointLocalV8 {

    Response getOpenTimesheets();

    Response getTimesheet(String timesheetId);

    Response getTimesheetPositions(String timesheetId);

    Response getTimesheetTemplates(String timesheetId);

    Response getRunningPositions();

    Response getRunningPositionsCount();

    Response getLastPositionsForCase(String caseId);

    Response startPosition(String timesheetId, RestfulTimesheetPositionV8 position);

    Response stopPosition(String timesheetId, String positionId, RestfulTimesheetPositionV8 position);

    Response addPosition(String timesheetId, RestfulTimesheetPositionV8 position);

    Response updatePosition(String timesheetId, String positionId, RestfulTimesheetPositionV8 position);

}
