package com.jdimension.jlawyer.services;

import com.jdimension.jlawyer.persistence.TimesheetPositionTemplate;
import java.util.List;
import java.util.Map;
import javax.ejb.Local;

@Local
public interface TimesheetServiceLocal {

    List<TimesheetPositionTemplate> getPositionTemplatesForTimesheet(String timesheetId) throws Exception;
    Map<String,List<TimesheetPositionTemplate>> getPositionTemplatesForTimesheets(List<String> timesheetIds) throws Exception;

}
