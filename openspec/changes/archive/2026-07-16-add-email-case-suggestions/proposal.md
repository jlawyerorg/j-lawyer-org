# Change: Server-side case suggestions for an opened email

## Why
The Swing client, when a message is selected, builds a suggestion list of matching cases from
the sender, the subject/body (own and foreign file numbers), plus extracted phone numbers. It
does this by downloading **all** case file numbers and **all** referenced file numbers to the
client and matching locally, then issuing one lookup per hit. That is fine for the fat client
(warm EJB connection, local cache) but wrong for the web client: large payloads, N+1 HTTP
round-trips, no shared cache, and two of the primitives (`extractKeywordsFromText`,
`getAllReferencedFileNumbers`) are not exposed over REST at all.

Moving the whole heuristic behind a single REST endpoint keeps desktop and web consistent,
enforces ACL centrally, and turns dozens of round-trips into one.

## What Changes
- Add `POST /v7/email/case-suggestions` (v7 email capability) accepting `{ subject, body, from }`
  and returning a structured suggestion result: matching cases (tagged by source), the sender's
  matching contacts, extracted phone numbers, and the parsed sender name/email.
- The server reproduces the Swing heuristic using existing EJB logic (file-number matching in
  subject/body, foreign-reference matching, sender→contacts→cases) and ACL-filters the cases.
- Expose two EJB methods on the `ArchiveFileServiceLocal` interface that the bean already
  implements for the remote interface (`extractKeywordsFromText`, `getAllReferencedFileNumbers`),
  so the endpoint can call them locally.
- Web client: a "matching cases" panel in the mail reader that calls the endpoint and lets the
  user jump to a suggested case (deep-link). Save-to-case and create-case/contact actions remain
  out of scope for this change.

## Impact
- Affected specs: email-rest-api
- Affected code:
  - `j-lawyer-server-ejb/.../services/ArchiveFileServiceLocal.java` (2 method signatures)
  - `j-lawyer-io/.../rest/v7/EmailEndpointV7.java` (+ `EmailEndpointLocalV7.java`) — new endpoint
  - new POJOs: `RestfulCaseSuggestionsV7`, `RestfulCaseSuggestionRequestV7`
  - Web: `communication/email.service.ts`, `email.component.ts` (reader suggestion panel)
- Backward compatibility: purely additive; no existing endpoint or EJB signature changes.
