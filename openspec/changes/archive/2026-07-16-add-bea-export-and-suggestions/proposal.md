# Change: beA message export (.bea) and case suggestions

## Why
The web beA view is being brought to parity with the web e-mail view: a reader action bar
(reply/forward), a save-to-case dialog, and a case-suggestion list. Two capabilities the e-mail
side already has over REST are missing for beA:

- **Whole-message export.** The desktop client stores a beA message in a case as a single `.bea`
  file — the entire `BeaMessage` marshalled to XML (`BeaAccess.exportMessage`). This runs only in
  the desktop client (JAXB marshalling); there is no REST endpoint, so a web client cannot produce
  the `.bea` document.
- **Case suggestions.** The e-mail reader gets server-computed case suggestions
  (`POST /v7/email/mailboxes/{id}/case-suggestions`). beA has no equivalent, yet a beA message
  carries two Aktenzeichen (sender + court) that are strong case signals.

## What Changes
- Add `GET /v8/bea/postboxes/{safeId}/messages/{messageId}/export` returning the message as a
  `.bea` file: `{ fileName, contentBase64 }`, where the content is the full `BeaMessage` marshalled
  to XML by the server (JAXB, identical format to the desktop client) and the file name mirrors the
  desktop convention (`referenceNumber_subject_sender--recipient.bea`, sanitized).
- Add `POST /v8/bea/case-suggestions` accepting `{ subject, body, referenceNumber, referenceJustice,
  senderName }` and returning the same structure as the e-mail case suggestions
  (`suggestedCases`, `contacts`, `phoneNumbers`, `senderName`, `senderEmail`). The server matches
  the two reference numbers plus subject/body against known own and referenced file numbers, and the
  sender name against the address book. All case lookups are ACL-filtered.

## Impact
- Affected specs: bea-rest-api (new capability)
- Affected code:
  - `j-lawyer-io/.../rest/v8/BeaEndpointV8.java` (+ `BeaEndpointLocalV8.java`)
  - new POJOs: `RestfulBeaExportV8`, `RestfulBeaCaseSuggestionRequestV8`
  - reuses the v7 suggestion result POJOs so the JSON matches the e-mail suggestions
  - Web: `bea/bea.service.ts`, `bea/bea.component.ts`, new beA compose + save-to-case components
- Backward compatibility: purely additive; no existing endpoint changes.
