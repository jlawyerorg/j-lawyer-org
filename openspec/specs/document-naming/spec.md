# document-naming Specification

## Purpose
Expose document name templates (Benennungsschemata) and server-side document-name computation over
the REST API, so non-desktop clients can name documents exactly as the desktop client does —
resolving case placeholders, sanitizing, and preserving the source file extension — without pulling
case parties or form fields to the client.
## Requirements
### Requirement: Document Name Template Listing
The REST API SHALL provide an endpoint to list all document name templates
(Benennungsschemata) configured in the system, so clients can offer the same naming choices as
the desktop client.

#### Scenario: User lists document name templates
- **WHEN** a GET request is made to `/rest/v7/configuration/document-name-templates`
- **THEN** the response SHALL contain a JSON array of template objects
- **AND** each template SHALL include `id`, `displayName`, and `defaultTemplate`
- **AND** exactly the template configured as the system default SHALL have `defaultTemplate` set to `true`

### Requirement: Server-side Document Naming
The REST API SHALL provide an endpoint that computes a document file name for a case by applying
a document name template, so clients do not have to resolve case placeholders (file number, date,
parties, form fields) themselves. The case SHALL be limited to those the authenticated user may
access.

#### Scenario: Compute a name from a template
- **WHEN** a POST request is made to `/rest/v7/cases/{id}/document-name` with `{ "fileName": "Vertrag.pdf", "date": 0, "template": "<templateId>" }`
- **THEN** the response SHALL contain `{ "name": "<computed name>" }`
- **AND** the name SHALL have the template's placeholders resolved against the case, be sanitized for use as a file name, and preserve the extension of `fileName`
- **AND** a `date` of `0` SHALL be treated as the current date, otherwise `date` is interpreted as epoch milliseconds

#### Scenario: Empty or missing template uses the default
- **WHEN** the request omits `template` or sends an empty value
- **THEN** the system default document name template SHALL be applied
- **AND** when no document name template exists at all, the sanitized original `fileName` SHALL be returned

#### Scenario: Unknown case
- **WHEN** a POST request targets a case id the authenticated user cannot access or that does not exist
- **THEN** the response SHALL return HTTP 404

