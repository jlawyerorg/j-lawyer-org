## 1. Dependency / POM changes
- [x] 1.1 Pick the exact target version (latest stable 9.x, e.g. `9.12.x`) and set it for every `org.apache.lucene:*` entry in the root `pom.xml` `dependencyManagement` — set to `9.12.0`
- [x] 1.2 Rename `lucene-analyzers-common` → `lucene-analysis-common` in the root `dependencyManagement`
- [x] 1.3 Update the analyzers artifactId to `lucene-analysis-common` in each module POM that declares it: `j-lawyer-server-ejb`, `j-lawyer-server-war`, `j-lawyer-server-io`, `j-lawyer-io`, `j-lawyer-server-ear`
- [x] 1.4 Confirm the remaining Lucene artifacts (`lucene-core`, `lucene-highlighter`, `lucene-memory`, `lucene-queries`, `lucene-queryparser`) still exist under the same coordinates in 9.x and inherit the version from `dependencyManagement`
- [ ] 1.5 Review the transitive graph the upgrade introduces; add `exclusions`/management entries if any shared library (per the build-system spec) would otherwise gain a conflicting version (needs a build to inspect the resolved graph)

## 2. Rewrite `SearchAPI.java` for the Lucene 9 API
- [x] 2.1 Remove all `org.apache.lucene.util.Version` usages (`LUCENE_47`, `LUCENE_CURRENT`): `GermanAnalyzer()`, `IndexWriterConfig(analyzer)`, `QueryParser(field, analyzer)`
- [x] 2.2 Change `FSDirectory.open(File)` to `FSDirectory.open(dstDir.toPath())`
- [x] 2.3 Replace `FieldType.setIndexed(true)` + `FieldInfo.IndexOptions.*` with `org.apache.lucene.index.IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS`
- [x] 2.4 Drop per-document analyzer args: `addDocument(doc)` and `updateDocument(term, doc)`
- [x] 2.5 Update `SearcherManager` construction to `new SearcherManager(writer, null)`
- [x] 2.6 Update `DirectoryReader.open(writer)` (remove the `applyAllDeletes` boolean) in `getNumberOfDocs()`
- [x] 2.7 Rework lock handling: removed `IndexWriter.unlock(...)`, `setWriteLockTimeout(...)`, and the `WRITE_LOCK_TIMEOUT_MS` constant. Lucene 9's default `NativeFSLockFactory` auto-releases stale locks from crashed JVMs, so a real `LockObtainFailedException` (live second JVM) now propagates into the existing `initError` + cooldown-retry path instead of being force-cleared
- [x] 2.8 Highlighter path updated for 9.x: replaced deprecated `TokenSources.getAnyTokenStream(...)` with `TokenSources.getTermVectorTokenStreamOrNull(field, tvFields, -1)` plus an `analyzer.tokenStream(...)` fallback; `Highlighter`/`QueryScorer`/`SimpleHTMLFormatter`/`IndexSearcher.doc(int)` retained
- [x] 2.9 Fix imports accordingly (removed `Version`, `LockObtainFailedException`; `IndexOptions`/`IndexFormatTooOldException` covered by `org.apache.lucene.index.*`)

## 3. Index-format incompatibility handling
- [x] 3.1 In `openWriterAndDirectory()`, catch `IndexFormatTooOldException` (and `IndexFormatTooNewException`): log a prominent message, clear the `searchindex` directory (`clearIndexDirectory()`), and recreate an empty writer so the server starts cleanly instead of staying uninitialized
- [x] 3.2 Do NOT auto-trigger a re-index after clearing; log a clear instruction that an administrator must start a full re-index (re-index stays admin-initiated)
- [x] 3.3 Ensure `deleteAll()` + rebuild via `SearchIndexProcessor` still works end-to-end with the new writer lifecycle (no API change in that path; `deleteAll()`/`addToIndex()` updated for 9.x)
- [x] 3.4 Fix shutdown `NoClassDefFoundError` (`IndexFileDeleter$CommitPoint`): remove the JVM shutdown hook from `SearchAPI`, add static `SearchAPI.shutdownInstance()`, and close the index from `ContainerLifecycleBean#terminate()` (`@PreDestroy`) so the final commit runs while the deployment classloader is still available

## 4. Expose re-index over REST API ("Search" category)
- [x] 4.1 Add `void reIndexAll();` to `SearchServiceLocal` (`j-lawyer-server-ejb/.../services/SearchServiceLocal.java`); the `SearchService` bean already implements it
- [x] 4.2 Add `Response reIndexAll();` to `SearchEndpointLocalV8`
- [x] 4.3 Implement `reIndexAll()` in `SearchEndpointV8`: `@POST @Path("/reindex")`, `@RolesAllowed({"adminRole"})`, `@ApiOperation` (tag "Search"), look up `SearchServiceLocal` via the existing JNDI name, call `reIndexAll()`, return `202 Accepted` (async rebuild) and `500` on failure
- [x] 4.4 Confirm the generated `swagger.json` lists `POST /v8/search/reindex` under the "Search" tag after build

## 5. Fielded search for metadata
- [x] 5.1 Add non-analyzed lowercased keyword fields `dateiname-kw`, `akte-kw`, `az-kw` (`StringField`, `Store.NO`) in `addToIndex`/`updateInIndex`, alongside the existing stored display fields
- [x] 5.2 Replace the blanket `QueryParser.escape()` in `search()` with `buildQuery()`: recognized `dateiname:`/`akte:`/`az:` → `TermQuery`/`WildcardQuery` on the keyword field (lowercased); `text:` → analyzed full-text; anything else → escaped literal full-text on the default field (unchanged behavior)
- [x] 5.3 Keep non-fielded queries robust (special characters escaped, no `ParseException`)
- [x] 5.4 After re-index, verify: `dateiname:test.pdf` (exact, case-insensitive), `dateiname:*.pdf` (wildcard), `akte:`/`az:` queries, and that plain queries with special characters still work
- [x] 5.5 Client: wire the search help icon (`jLabel3`) in `DocumentSearchPanel` to an accurate syntax dialog (`showSearchSyntaxHelp()` + `SEARCH_SYNTAX_HELP` constant reflecting the real single-field behavior)
- [x] 5.6 Client: wire a field-selection dropdown — `applySelectedSearchField()` builds the `feld:wert` query from the selected `cmbSearchField` entry (index → `SEARCH_FIELD_PREFIXES`), used in the search action; "Inhalt"/empty selection sends the raw text so typed prefixes still work
- [x] 5.7 Client (.form/UI, done by maintainer): add a `javax.swing.JComboBox` named `cmbSearchField` before `cmbMaxDocs` with items in this exact order — `Inhalt`, `Dateiname`, `Aktenname`, `Aktenzeichen` (done)
- [x] 5.8 Client (.form/UI, done by maintainer): replace the misleading `jLabel3` tooltip (advertises `autor:`, AND/OR, phrases, exclusion — none supported) with an accurate short hint; optionally update the placeholder

## 6. Verification
- [x] 6.1 Full reactor build on Java 17 (`./build.sh`) compiles with no Lucene API errors
- [x] 6.2 Deploy the EAR to WildFly 26.1.3; server starts and the old 4.x index is cleared, not fatal
- [x] 6.3 Trigger re-index via the new `POST /v8/search/reindex` endpoint (admin credentials); confirm `getNumberOfDocs()` grows to match document count
- [x] 6.4 Smoke-test representative German full-text queries: hits returned, scoring sane, highlighting renders
- [x] 6.5 Verify the reindex endpoint rejects non-admin users (401/403)
- [ ] 6.6 Confirm Dependabot/CVE tooling sees `org.apache.lucene:*:9.x` (real coordinates, current version)
- [x] 6.7 Measure performance & index size before/after: record `searchindex/` directory size and `getNumberOfDocs()`, plus latency of a few representative queries, on the old (4.7) index and the rebuilt (9.x) index; confirm size is roughly equal-or-smaller and top-N query latency is not worse
- [x] 6.8 Verify clean WildFly shutdown no longer logs the `NoClassDefFoundError` (`IndexFileDeleter$CommitPoint`) from the search index close

## 7. Docs
- [x] 7.1 Update CLAUDE.md and `openspec/project.md` references from "Apache Lucene 4.7.0" to the new 9.x version (now `9.12.0`)
- [ ] 7.2 Add a release note documenting the mandatory one-time re-index after upgrade, the new `POST /v8/search/reindex` endpoint, and the new `field:value` search syntax (no release-notes file in repo yet; to be added with the release)

## 8. Migrate to UnifiedHighlighter (behavior-changing; bundled into this change by request)
- [x] 8.1 Replace the classic `Highlighter` + `QueryScorer` + `TokenSources` path in `SearchAPI.search()` with `org.apache.lucene.search.uhighlight.UnifiedHighlighter` (`UnifiedHighlighter.builder(searcher, analyzer).build()`, `highlight(FIELD_TEXT, query, hits, 6)`)
- [x] 8.2 Stop indexing the `text-tv` term-vector field (dropped `FIELD_TEXT_TERMVECTOR`); index `FIELD_TEXT` via a stored `TEXT_FIELD_TYPE` with `IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS` so the UnifiedHighlighter highlights from postings offsets
- [x] 8.3 Note: requires a full re-index because the field/term-vector layout changed (covered by the same admin-initiated re-index)
- [x] 8.4 Verify highlighting output is acceptable (HTML fragments wrapped in `<b>…</b>`) for representative German queries after re-index — confirmed working (per-passage `<br/>` lines, length-capped snippets via `LengthGoalBreakIterator`, no snippet for metadata-only hits, width-wrapped tooltip)
- [ ] 8.5 Re-measure index size (per task 6.7) to quantify the reduction from removing term vectors
