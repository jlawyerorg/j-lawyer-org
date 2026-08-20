# Tasks

## Server
- [x] Add `RestfulBeaExportV8` and `RestfulBeaCaseSuggestionRequestV8` POJOs
- [x] `GET /v8/bea/postboxes/{safeId}/messages/{messageId}/export` in `BeaEndpointV8` (+ Local)
- [x] `POST /v8/bea/case-suggestions` in `BeaEndpointV8` (+ Local), ACL via the case service
- [x] Build EAR, deploy to WildFly
- [x] Verify `case-suggestions` matches a case by its reference number
- [ ] Verify `export` against a live beA message (requires a configured beA session)

## Web
- [x] `BeaService`: `exportMessage`, `caseSuggestions` (+ compose/identity helpers)
- [x] beA reader action bar + compose dialog
- [x] beA save-to-case dialog + suggestion panel

## Docs
- [x] OpenSpec delta for the new `bea-rest-api` capability
