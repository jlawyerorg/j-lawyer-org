## ADDED Requirements

### Requirement: Channel-based voice endpoint discovery
The system SHALL retrieve available voice channels from the sipgate API using `GET /channels` and present them as selectable voice endpoints in the UI.

Each channel SHALL be mapped to a SipUri with:
- `uri` set to the channel UUID
- `description` set to the channel `name`
- `typeOfService` containing `TOS_VOICE`

#### Scenario: Successful channel retrieval
- **WHEN** the system queries `GET /channels` with valid authentication
- **THEN** it SHALL return a list of SipUri objects, one per channel, each with `TOS_VOICE` in its typeOfService

#### Scenario: No channels available
- **WHEN** the system queries `GET /channels` and the response contains an empty `items` array
- **THEN** no voice SipUri objects SHALL be added to the result list

#### Scenario: API error during channel retrieval
- **WHEN** the system queries `GET /channels` and receives a non-200 response
- **THEN** it SHALL throw a SipgateException with the HTTP status code and response body

### Requirement: Channel-based call initiation
The system SHALL initiate voice calls using `POST /calls` with the channel-based request format.

The request body SHALL contain:
- `deviceId`: the device identifier for the calling device
- `targetNumber`: the remote phone number in E.164 format
- `callerId` (optional): the caller ID to display
- `channelId` (optional): the channel to route the call through

#### Scenario: Successful call initiation
- **WHEN** a user initiates a call with a valid channel URI and target number
- **THEN** the system SHALL send a POST request to `/calls` and return the `callSid` from the response

#### Scenario: Call initiation with caller ID
- **WHEN** a user initiates a call with a caller ID specified
- **THEN** the request body SHALL include the `callerId` field

#### Scenario: Call initiation failure
- **WHEN** the sipgate API returns a non-200 response to `POST /calls`
- **THEN** the system SHALL throw a SipgateException with the error details

## REMOVED Requirements

### Requirement: Phoneline-based voice endpoint discovery
The system previously retrieved voice endpoints from `GET /{userId}/phonelines`, parsing `SimplePhonelineResponse` objects with short IDs like `p0` and `alias` fields.

**Reason**: The `/{userId}/phonelines` endpoint returns HTTP 403 on sipgate neo accounts and is deprecated.
**Migration**: Replaced by "Channel-based voice endpoint discovery" above.

### Requirement: Session-based call initiation
The system previously initiated calls via `POST /sessions/calls` with `caller`, `callee`, and `callerId` fields, receiving a `sessionId` in response.

**Reason**: The `/sessions/calls` endpoint does not support channel-based routing used by sipgate neo.
**Migration**: Replaced by "Channel-based call initiation" above.
