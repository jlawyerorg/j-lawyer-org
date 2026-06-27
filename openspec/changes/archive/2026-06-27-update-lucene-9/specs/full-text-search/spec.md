## ADDED Requirements

### Requirement: Supported Lucene Search Engine Version
The full-text search engine SHALL use a supported Apache Lucene 9.x release, pinned
centrally in the root `pom.xml` `dependencyManagement` using the canonical
`org.apache.lucene:*` Maven Central coordinates so the version is visible to CVE advisory
tooling (Dependabot). The version SHALL stay within the project's Java 17 toolchain
(Lucene 10+, which requires Java 21, is out of scope).

#### Scenario: Lucene artifacts resolve to a single 9.x version
- **WHEN** the reactor resolves `org.apache.lucene:lucene-core` and the other Lucene artifacts
- **THEN** every Lucene artifact resolves to the same pinned 9.x version from `dependencyManagement`, and the analyzers artifact is `lucene-analysis-common`

#### Scenario: Search version is advisory-visible
- **WHEN** dependency CVE tooling scans the declared coordinates
- **THEN** the Lucene version is reported as `org.apache.lucene:*:9.x` (real coordinates), not a synthetic or end-of-life-hidden version

### Requirement: Incompatible Search Index Is Cleared and Rebuilt On Demand
Because Lucene 9 cannot read an index written by Lucene 4, the system SHALL detect an
incompatible on-disk search index at startup and recover by clearing it and recreating an
empty index, rather than failing permanently or leaving search unusable. The system SHALL
NOT re-index automatically; the rebuild from the source documents SHALL be initiated by an
administrator. No case data is lost because the index is derived from documents already
stored in the case archive.

#### Scenario: Old-format index detected on startup
- **WHEN** the search component opens a `searchindex` directory written by an older Lucene major version (`IndexFormatTooOldException`)
- **THEN** it logs a prominent message instructing an administrator to start a full re-index, clears the index directory, recreates an empty writer, and the server starts successfully instead of leaving search uninitialized

#### Scenario: No automatic re-index
- **WHEN** the index has been cleared due to format incompatibility
- **THEN** the server does NOT start a re-index on its own and waits for an administrator to trigger it

#### Scenario: Admin-initiated re-index restores searchability
- **WHEN** an administrator triggers a full re-index after the index was cleared
- **THEN** documents are re-indexed via the existing re-index mechanism and the indexed document count grows to match the stored documents

### Requirement: Fielded Search for Document Metadata
Full-text search SHALL support a `field:value` query prefix for the document metadata
fields filename (`dateiname`), case name (`akte`), and case number (`az`), in addition to
the default full-text search over document content. Metadata fielded matches SHALL be
case-insensitive and support `*`/`?` wildcards. Any query without a recognized field
prefix SHALL continue to be treated as literal full-text against the document content,
with query-syntax special characters escaped so arbitrary input cannot cause a parse
error. Metadata fielded search relies on non-analyzed keyword index fields, so it takes
effect only for documents indexed after the change (a full re-index is required).

#### Scenario: Search by filename
- **WHEN** a user searches for `dateiname:test.pdf`
- **THEN** documents whose filename equals `test.pdf` (case-insensitive) are returned

#### Scenario: Wildcard filename search
- **WHEN** a user searches for `dateiname:*.pdf`
- **THEN** documents whose filename ends with `.pdf` (case-insensitive) are returned

#### Scenario: Plain text search is unchanged and robust
- **WHEN** a user searches without a recognized field prefix (e.g. `Vertrag 2024` or text containing special characters like `:` or `(`)
- **THEN** the input is searched as literal full-text against the document content and does not raise a query parse error

### Requirement: Administrative Re-Index via REST API
The REST API SHALL provide an administrator-only endpoint, under the "Search" category, to
start a full re-index of the document search index, so the rebuild can be initiated
without the desktop client. The endpoint SHALL reuse the existing asynchronous re-index
mechanism and SHALL NOT block on completion.

#### Scenario: Admin triggers re-index over REST
- **WHEN** an authenticated user with the administrator role calls `POST /v8/search/reindex`
- **THEN** a full re-index is started asynchronously and the endpoint responds with an accepted status without waiting for the rebuild to finish

#### Scenario: Non-admin is rejected
- **WHEN** a user without the administrator role calls `POST /v8/search/reindex`
- **THEN** the request is rejected as unauthorized and no re-index is started

#### Scenario: Endpoint documented under "Search"
- **WHEN** the generated `swagger.json` is inspected
- **THEN** the re-index operation appears under the "Search" tag alongside the fulltext search operation

### Requirement: Search Behavior Preserved Across the Upgrade
The externally observable search behavior SHALL be preserved across the Lucene upgrade:
the `SearchAPI` operations (add, update, remove, search, delete-all, document count,
re-open) keep their method signatures and semantics, and full-text search continues to
return scored hits with highlighted text fragments. No EJB remote interface or REST API
signature changes as part of this upgrade.

#### Scenario: Query returns scored, highlighted hits
- **WHEN** a user performs a full-text search after the upgrade and re-index
- **THEN** matching documents are returned with a relevance score and an HTML-highlighted text fragment, as before

#### Scenario: Caller contracts unchanged
- **WHEN** `SearchService` and `SearchIndexProcessor` call the search component
- **THEN** they use the same `SearchAPI` method signatures as before the upgrade (the Lucene API changes are contained inside `SearchAPI`)
