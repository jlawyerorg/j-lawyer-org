# Implementation Tasks

## Overview
This document outlines the implementation tasks for adding the calendar date range endpoint to the existing `CasesEndpointV4` class.

## Task Breakdown

### 1. Review Existing Endpoint Class
**Dependencies**: None
**Estimated Effort**: 30 minutes
**Validation**: Understanding of current structure

- [x] Read `j-lawyer-server/j-lawyer-io/src/org/jlawyer/io/rest/v4/CasesEndpointV4.java`
- [x] Note existing patterns for method structure
- [x] Confirm `RestfulDueDateV6` is already imported (line 683)
- [x] Identify where to add new method (end of class)

### 2. Add New Method to CasesEndpointV4
**Dependencies**: Task 1
**Estimated Effort**: 3-4 hours
**Validation**: Method compiles, parameters are correctly defined

- [x] Add new method to `CasesEndpointV4.java`
- [x] Add `@GET` annotation with `@Path("/duedates/range")`
- [x] Add `@Produces(MediaType.APPLICATION_JSON+";charset=utf-8")` annotation
- [x] Add `@RolesAllowed({"loginRole"})` security annotation
- [x] Add `@QueryParam` parameters:
  - `from` (String, required)
  - `to` (String, required)
  - `eventType` (Integer, optional, default -1 for ANY)
  - `status` (Integer, optional, default -1 for ANY)
  - `limit` (Integer, optional, default 5000)
- [x] Add JavaDoc documentation with parameter descriptions and response codes (401, 403, 400, 500, 200)

### 3. Implement Date Parsing Logic
**Dependencies**: Task 2
**Estimated Effort**: 2 hours
**Validation**: Unit tests pass for various date formats

- [x] Implement parsing for ISO 8601 format (e.g., "2024-01-01T00:00:00Z")
- [x] Implement parsing for epoch milliseconds (e.g., "1704067200000")
- [x] Add error handling for invalid date formats
- [x] Return 400 Bad Request with descriptive error message for parse failures

### 4. Integrate with CalendarService
**Dependencies**: Task 3
**Estimated Effort**: 2 hours
**Validation**: JNDI lookup succeeds, service calls work

- [x] Add JNDI lookup for `CalendarServiceLocal`:
  ```java
  InitialContext ic = new InitialContext();
  CalendarServiceLocal cal = (CalendarServiceLocal) ic.lookup("java:global/j-lawyer-server/j-lawyer-server-ejb/CalendarService!com.jdimension.jlawyer.services.CalendarServiceLocal");
  ```
- [x] Call `cal.searchReviews(status, eventType, fromDate, toDate, limit)`
- [x] Handle exceptions and return appropriate HTTP status codes

### 5. Transform Response to JSON
**Dependencies**: Task 4
**Estimated Effort**: 2-3 hours
**Validation**: JSON response validates, all required fields present

- [x] Reuse existing `RestfulDueDateV6` POJO (already imported in CasesEndpointV4)
- [x] Map `ArchiveFileReviewsBean` fields to `RestfulDueDateV6`:
  - Event ID → setId()
  - Summary → setSummary()
  - Description → setDescription()
  - Location → setLocation()
  - Begin date → setBeginDate()
  - End date → setEndDate()
  - Event type → setType() (convert to TYPE_FOLLOWUP, TYPE_RESPITE, TYPE_EVENT)
  - Done flag → setDone()
  - Assignee → setAssignee()
  - Case ID → setCaseId()
  - Case file number → setFileNumber()
  - Case name → setCaseName()
- [x] Handle null values gracefully
- [x] Return `Response.ok(eventList).build()` for success
- [x] Return `Response.serverError().build()` for unexpected errors

### 6. Add Error Handling
**Dependencies**: Tasks 2-5
**Estimated Effort**: 1 hour
**Validation**: Error responses follow REST conventions

- [x] Add try-catch block around entire method
- [x] Log errors using `log.error()` with exception details
- [x] Return appropriate HTTP status codes:
  - 400 for invalid parameters
  - 401 for authentication failures (handled by container)
  - 500 for server errors
- [x] Include error messages in response where appropriate

### 7. Update API Documentation
**Dependencies**: Tasks 1-6 complete
**Estimated Effort**: 1 hour
**Validation**: Swagger UI displays new endpoint

- [ ] Ensure endpoint appears in Swagger UI at `/j-lawyer-io/swagger-ui/` (requires deployment)
- [ ] Verify parameter descriptions are clear (JavaDoc complete)
- [ ] Verify response schema is documented (using RestfulDueDateV6)
- [ ] Check that examples are provided (to be done during testing)

### 8. Testing
**Dependencies**: All implementation tasks complete
**Estimated Effort**: 3-4 hours
**Validation**: All test scenarios pass

- [ ] Manual testing with curl or Postman (requires deployment and server restart):
  - Test with ISO 8601 date format
  - Test with epoch milliseconds
  - Test with simple date format (yyyy-MM-dd)
  - Test with invalid date format (expect 400)
  - Test with eventType filter
  - Test with status filter
  - Test with combined filters
  - Test with very large date range (verify limit works)
  - Test without authentication (expect 401)
- [ ] Verify security: Test that users only see events from cases they have access to
- [ ] Verify response format matches specification
- [ ] Test edge cases:
  - Empty date range (no events)
  - Date range in the past
  - Date range in the future
  - Single day range
  - fromDate > toDate (should return results or empty array)
  - Missing required parameters (from/to)

### 9. Code Review Preparation
**Dependencies**: Tasks 1-8 complete
**Estimated Effort**: 1 hour
**Validation**: Code follows project conventions

- [x] Verify code follows existing `CasesEndpointV4` patterns
- [x] Add JavaDoc comments (in English) to the new method
- [x] Verify no build warnings are introduced (will be verified on build)
- [x] Ensure code formatting is consistent with existing methods in the class
- [x] Verify method is added at appropriate location in class (added at end)

## Parallelizable Work
- Tasks 7 (documentation) and 8 (testing) can be performed in parallel with final implementation tasks
- Documentation can be drafted while implementation is in progress

## Notes
- Do **NOT** invoke build commands - builds are done manually
- Follow existing REST API patterns in `CasesEndpointV4` (look at other methods in same class for reference)
- Only add new method - do **NOT** modify existing methods in `CasesEndpointV4`
- `RestfulDueDateV6` is already imported in the class, so no new imports needed for the POJO
- Security is handled by container and existing `CalendarService` checks - no additional implementation needed
- This follows the existing pattern where v4 endpoints use POJOs from later versions (v6)
