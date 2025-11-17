# Add Calendar Date Range Endpoint

## Summary
Add a new REST API endpoint to retrieve all open calendar entries (reviews/events) within a specified date range. This will enable external systems and mobile applications to efficiently query calendar data for a given time period.

## Motivation
Currently, the REST API provides endpoints to retrieve all open reviews (`getAllOpenReviews()`) or search with various filters (`searchReviews()`), but there is no simple way to retrieve all open calendar entries for a specific date range through the REST API. External integrations and mobile applications need a straightforward endpoint to:

- Fetch calendar entries for display in calendar views (weekly, monthly)
- Synchronize calendar data for offline access
- Build dashboards showing upcoming events within a specific timeframe
- Integrate with external calendar systems

The existing `searchReviews()` method in `CalendarService` already supports date range queries but is not exposed via REST API in recent versions (v5-v7).

## Scope
- **In Scope**:
  - New REST API method in `CasesEndpointV4`: `GET /v4/cases/duedates/range`
  - Query parameters for `from` and `to` date range
  - Optional query parameters for filtering by event type and status
  - Reuse existing `RestfulDueDateV6` POJO for response format
  - Security: Respect user permissions and case access controls
  - Return JSON array of calendar entries with full event details

- **Out of Scope**:
  - Creating new endpoint classes or POJOs
  - Modifications to existing calendar/review business logic
  - Changes to the database schema or entity model
  - Pagination (can be added in future if needed)
  - Modifications to desktop client
  - Changes to calendar synchronization with cloud services
  - Changes to other API versions (v5, v6, v7)

## Proposed Solution
Add a new method to the existing `CasesEndpointV4` class that:

1. Accepts date range parameters (`from`, `to`) as ISO 8601 formatted strings or epoch milliseconds
2. Optionally accepts `eventType` (followup=10, respite=20, event=30, or any=-1) and `status` (open=0, done=1, any=-1) parameters
3. Delegates to existing `CalendarService.searchReviews()` method
4. Returns filtered calendar entries as `RestfulDueDateV6` objects respecting user access permissions
5. Follows existing REST API patterns (HTTP Basic Auth, JSON responses)
6. Endpoint path: `/v4/cases/duedates/range`

## Dependencies
- Existing `CasesEndpointV4` class (will be modified)
- Existing `CalendarServiceLocal` EJB interface
- Existing `searchReviews()` business logic
- Existing `RestfulDueDateV6` POJO (already imported in CasesEndpointV4)
- No database schema changes required
- No new entity beans or POJOs required

## Risks and Mitigations
- **Risk**: Performance impact from large date ranges
  - **Mitigation**: Use existing limit parameter (default 5,000,000, can be made configurable)

- **Risk**: Security - unauthorized access to calendar data
  - **Mitigation**: Leverage existing security checks in `searchReviews()` which filters by allowed cases

- **Risk**: Modifying existing v4 endpoint class
  - **Mitigation**: Adding new method only, no changes to existing methods. This is a common pattern seen in the codebase (CasesEndpointV4 already imports RestfulDueDateV6 from v6)

## Success Criteria
- New endpoint successfully returns calendar entries for date ranges
- Security controls properly filter results based on user permissions
- API documentation (Swagger) includes new endpoint
- Response format is consistent with existing calendar/review endpoints
- Zero breaking changes to existing API versions
