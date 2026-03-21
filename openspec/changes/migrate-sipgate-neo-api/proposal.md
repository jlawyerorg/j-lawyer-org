# Change: Migrate Sipgate integration to neo API endpoints

## Why
The `/{userId}/phonelines` endpoint returns HTTP 403 on sipgate neo accounts and is being deprecated. sipgate neo replaces phonelines with "channels" — a new concept that groups call routing, devices, and users. The existing `POST /sessions/calls` endpoint lacks native channel support, while the new `POST /calls` endpoint explicitly accepts a `channelId`.

## What Changes
- **BREAKING**: Replace `GET /{userId}/phonelines` with `GET /channels` in `SipgateAPI.getOwnUris()` — response schema changes from `SimplePhonelineResponse` (id like `p0`, `alias`) to `ChannelResponse` (UUID `id`, `name`, `users[]`, `settings`)
- **BREAKING**: Replace `POST /sessions/calls` with `POST /calls` in `SipgateAPI.initiateCall()` — request body changes from `caller`/`callee`/`callerId` to `deviceId`/`targetNumber`/`callerId`/`channelId`, response changes from `sessionId` to `callSid`
- SipUri objects created from channels will carry UUID-based IDs instead of short IDs like `p0`
- Stored user preferences for last-used voice line (`CONF_VOIP_LASTSIPVOICE`) will be invalidated on migration since IDs change format

## Impact
- Affected specs: `sipgate-voip-integration` (new capability spec)
- Affected code:
  - `j-lawyer-fax/src/com/jdimension/jlawyer/fax/SipgateAPI.java` — `getOwnUris()` and `initiateCall()`
  - `j-lawyer-client/src/com/jdimension/jlawyer/client/voip/PlaceCallDialog.java` — consumes voice URIs and calls `initiateCall()`
  - No changes needed for fax (`/sessions/fax`), SMS (`/sessions/sms`), faxlines, SMS lines, numbers, balance, or history endpoints
