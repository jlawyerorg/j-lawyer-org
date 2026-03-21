## Context
sipgate neo (rolled out since September 2025) replaces the concept of "phonelines" with "channels". Existing accounts are being migrated gradually. The `/{userId}/phonelines` endpoint already returns HTTP 403 for neo accounts. The `POST /sessions/calls` endpoint still works but does not support channel-based routing.

This change affects the `j-lawyer-fax` module (SipgateAPI) and indirectly the client UI dialogs that consume SipUri objects for voice calls.

## Goals / Non-Goals
- Goals:
  - Restore voice call functionality for sipgate neo accounts
  - Use `GET /channels` to enumerate available voice endpoints
  - Use `POST /calls` with `channelId` to initiate calls
  - Maintain backward-compatible SipUri interface so client UI code requires minimal changes
- Non-Goals:
  - Migrating fax, SMS, or other endpoints (these still work)
  - Supporting both old phonelines and new channels simultaneously (phonelines are deprecated)
  - Adding new UI features around channels

## Decisions

### Decision 1: Map channels to existing SipUri model
- **What**: Parse `ChannelResponse` into the same `SipUri` objects used today, mapping `name` to `description` and channel UUID to `uri`
- **Why**: The client UI (PlaceCallDialog, SendFaxDialog, SendSmsDialog) consumes `List<SipUri>` — changing this interface would require changes across all three dialogs and the server EJB layer
- **Alternatives considered**: Creating a new `ChannelUri` class — rejected because the existing SipUri model is flexible enough (uri is already a String, description is a String)

### Decision 2: Replace /sessions/calls with /calls
- **What**: Use `POST /calls` with `deviceId`, `targetNumber`, `callerId`, `channelId` instead of `POST /sessions/calls` with `caller`, `callee`, `callerId`
- **Why**: The new endpoint natively supports channels and is the forward-looking API
- **Alternatives considered**: Keep `/sessions/calls` — still works but does not support channel routing and may be deprecated next

### Decision 3: Device ID resolution for calls
- **What**: The new `POST /calls` requires a `deviceId` (specific SIP device) rather than a phoneline ID. Channel responses include `users[].deviceIds[]` — we need to pick the appropriate device.
- **Approach**: Store the first available `deviceId` from the channel's user list in `SipUri.uri`, and the channel UUID separately (e.g. via `SipUri.outgoingNumber` or a new field). Alternatively, pass `channelId` and let sipgate resolve the device.
- **Open**: Need to verify whether `POST /calls` works with only `channelId` and no `deviceId`, or both are required.

## Risks / Trade-offs
- **ID format change**: Stored preferences for last-used voice line (`CONF_VOIP_LASTSIPVOICE`) will contain old `p0`-style IDs after migration. The UI already handles "not found in list" gracefully (no preselection), so this is a minor UX issue.
- **Device resolution**: If `POST /calls` requires a `deviceId` and channels have multiple users/devices, we need a strategy to pick the correct device for the authenticated user.
- **No fallback**: Once migrated, old phoneline-based accounts would break if they haven't been migrated to neo yet. However, sipgate has confirmed phonelines return 403 on neo accounts, so mixed support is not viable.

## Open Questions
- Does `POST /calls` work with only `channelId` (no `deviceId`)?
- Should `outgoingNumber` for voice URIs still come from `/{userId}/numbers`, or is there a channel-level number assignment?
