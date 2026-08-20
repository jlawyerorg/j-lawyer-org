# Change: Upgrade Apache Lucene from 4.7.0 to 9.x

## Why

The full-text search engine is pinned to **Apache Lucene 4.7.0** (released 2014), which
is long end-of-life: it receives no security fixes and is invisible to most CVE advisory
tooling. Upgrading to the latest **Lucene 9.x** line restores supported, patched
dependencies while staying within the project's **Java 17** toolchain (Lucene 10 requires
Java 21, so 9.x is the highest line we can adopt without a JDK migration).

## What Changes

- Bump all six Lucene artifacts from `4.7.0` to the latest stable **9.x** (currently
  `9.12.x`) and pin the version centrally in the root `pom.xml` `dependencyManagement`.
- Rename the analyzers artifact `lucene-analyzers-common` → `lucene-analysis-common`
  (renamed in Lucene 9) across all module POMs that declare it.
- **BREAKING (internal API):** Rewrite `SearchAPI.java` for the Lucene 5→9 API changes
  (removal of the `Version` argument, `Path`-based `FSDirectory.open`, `IndexOptions`
  relocation, removal of per-document analyzer overloads, `SearcherManager`/
  `DirectoryReader` signature changes, removal of `IndexWriter.unlock` /
  `setWriteLockTimeout`, highlighter token-source API). This is internal only — no EJB
  remote interface or REST API signature changes.
- **BREAKING (on-disk index format):** A Lucene 4 index cannot be read by Lucene 9
  (`IndexFormatTooOldException`). On first start after the upgrade the existing
  `searchindex` directory is auto-cleared and a re-index instruction is logged; the index
  is then rebuilt from the source documents using the existing re-index mechanism
  (`SearchService.reIndexAll()` → `SearchIndexProcessor`). The rebuild is **always
  admin-initiated** — the server never re-indexes automatically. No data is lost — the
  index is derived from documents already stored in the case archive.
- Expose the existing `reIndexAll()` operation over the REST API under the **"Search"**
  category: add `POST /v8/search/reindex` to `SearchEndpointV8` (admin-only) so an
  administrator can start the rebuild without the desktop client. This requires adding
  `reIndexAll()` to `SearchServiceLocal` (it currently exists only on
  `SearchServiceRemote`). Purely additive to the v8 API.
- Migrate highlighting to the **`UnifiedHighlighter`** and drop the separate `text-tv`
  term-vector field: `FIELD_TEXT` is indexed with postings offsets and the highlighter
  builds snippets from those. This removes the largest index-size contributor (term vectors
  with offsets) and the deprecated `TokenSources`/`getTermVectors` APIs (eases a later
  Lucene 10 move). Snippet markup may differ slightly (`<b>…</b>`, sentence-based passages).
  Needs the same admin-initiated re-index.
- Add **fielded search** for document metadata: a `field:value` prefix
  (`dateiname`, `akte`, `az`) now performs a case-insensitive, wildcard-capable match
  against non-analyzed keyword index fields, while any non-fielded query keeps the
  previous robust literal full-text behavior (special characters escaped). Previously the
  blanket `QueryParser.escape()` neutralized the `:` so `field:value` never worked. Needs a
  full re-index for the new keyword fields to populate (admin-initiated, as above).

## Impact

- Affected specs: `full-text-search` (new capability spec — Lucene version, index-format
  incompatibility handling, preserved search behavior).
- Affected code:
  - `j-lawyer-server/j-lawyer-server-ejb/.../org/jlawyer/search/SearchAPI.java` (rewrite)
  - Root `pom.xml` (version pin) and the six module POMs declaring Lucene:
    `j-lawyer-server-ejb`, `j-lawyer-server-war`, `j-lawyer-server-io`, `j-lawyer-io`,
    `j-lawyer-server-ear` (and the root `dependencyManagement`).
  - REST re-index endpoint: `SearchEndpointV8` + `SearchEndpointLocalV8`
    (`j-lawyer-io/.../rest/v8/`) and `SearchServiceLocal`
    (`j-lawyer-server-ejb/.../services/SearchServiceLocal.java`).
  - Fielded search + keyword index fields: `SearchAPI.java` (`buildQuery`, keyword fields
    `dateiname-kw`/`akte-kw`/`az-kw`).
- Operational impact: one-time full re-index after deployment; search is degraded/empty
  until the rebuild completes.
- Relation to `consolidate-duplicate-dependency-versions`: Lucene is single-version today,
  so no conflict — this change simply moves it to a newer single version.
