## 1. Channel endpoint integration
- [x] 1.1 In `SipgateAPI.getOwnUris()`: replace `API_BASE + internalUserId + "/phonelines"` with `API_BASE + "channels"` and parse `ChannelResponse` fields (`id` as UUID, `name` instead of `alias`, `users[].deviceIds[]`)
- [x] 1.2 Map each channel to a SipUri: set `uri` to channel UUID, `description` to channel `name`, add `TOS_VOICE` to typeOfService
- [x] 1.3 Resolve device IDs from `ChannelResponse.users[]` for the authenticated user — store appropriate `deviceId` for later use in call initiation (added `deviceId` field to `SipUri`)

## 2. Call endpoint migration
- [x] 2.1 In `SipgateAPI.initiateCall()`: replace `POST /sessions/calls` with `POST /calls`
- [x] 2.2 Adapt request body: use `deviceId`/`targetNumber`/`callerId`/`channelId` instead of `caller`/`callee`/`callerId`
- [x] 2.3 Adapt response parsing: read `callSid` instead of `sessionId`
- [x] 2.4 Update `initiateCall()` method signature across all layers: `SipgateAPI`, `SipgateInstance`, `VoipServiceRemote`, `VoipService`, `PlaceCallDialog`

## 3. Verification
- [ ] 3.1 Verify `getOwnUris()` returns voice URIs from channels on a sipgate neo account
- [ ] 3.2 Verify `initiateCall()` successfully initiates a call via `POST /calls` with channel ID
- [ ] 3.3 Verify fax, SMS, and other unaffected endpoints still work as before
- [ ] 3.4 Verify PlaceCallDialog correctly displays channel-based voice URIs and initiates calls
