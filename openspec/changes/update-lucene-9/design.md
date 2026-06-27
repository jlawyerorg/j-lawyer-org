## Context

Lucene is used in exactly one place: `org.jlawyer.search.SearchAPI` (a singleton in
`j-lawyer-server-ejb`). It owns the `IndexWriter`, a `SearcherManager` with a background
refresh scheduler, and a highlighter-based search. The public surface of `SearchAPI`
(`addToIndex`, `updateInIndex`, `removeFromIndex`, `search`, `deleteAll`,
`getNumberOfDocs`, `reOpen`) is called by `SearchService` (EJB) and
`SearchIndexProcessor` (JMS consumer); none of these signatures change, so the upgrade is
contained behind `SearchAPI`.

The jump is across five major versions (4 → 5 → 6 → 7 → 8 → 9). The two material risks
are the on-disk index format (Lucene reads only its own + the previous major) and the
several breaking API changes introduced in Lucene 5 and 6.

## Goals / Non-Goals

- Goals: supported Lucene line on the existing Java 17 toolchain; identical search/index
  behavior from the caller's perspective; centrally pinned version visible to Dependabot.
- Non-Goals: no JDK upgrade (rules out Lucene 10); no change to search semantics, query
  syntax, scoring expectations, or the `SearchHit` contract; no online/stepwise migration
  of the old index (we rebuild instead); **no Apache Tika change** (see below).

### Apache Tika is unaffected (not a transitive dependency of Lucene)

A common assumption is that Lucene depends on Apache Tika; it does not. The dependency
direction is the reverse: **Apache Solr** (built on Lucene) bundles Tika for content
extraction, not Lucene itself.

- `lucene-core` (and the other artifacts this project uses — `lucene-analysis-common`,
  `lucene-highlighter`, `lucene-memory`, `lucene-queries`, `lucene-queryparser`) declare
  **no compile dependencies**, and none reference Tika.
- The only place Tika appears under the Lucene group is the shared
  `lucene-solr-grandparent` POM, and only inside `<dependencyManagement>` (version hints
  for Solr's extraction contrib). `dependencyManagement` is not a transitive dependency —
  it sets a version only if a child declares the dependency, which the Lucene jars never
  do. In Lucene 9 this is moot anyway: Solr was split out into its own top-level project
  after Lucene 8, so Lucene 9 has no Solr grandparent and no Tika anywhere near it.

Consequently, this upgrade neither pulls nor changes any Tika version. Tika is declared
independently in this repo as `jlawyer.thirdparty:tika-app:1.22` (`provided` scope, a
shaded fat jar, so no class conflicts with Lucene regardless) and is used by code such as
`PreviewGenerator`, `TikaConfigurator`, `SystemManagement`, `IntegrationService`, and
`SearchIndexProcessor` (the last extracts the document text that is then handed to
Lucene). None of those call sites are touched by this change. A standalone Tika upgrade
(1.22 → 2.x has a breaking API) would be a separate change, independent of Lucene.

## Decisions

- **Decision: target the latest stable Lucene 9.x (9.12.x).** Highest line supported on
  Java 17; Lucene 10 requires Java 21.
- **Decision: rebuild the index rather than upgrade it in place.** A 4.x index is two+
  majors too old for Lucene 9, so `IndexUpgrader` chaining (4→5→6→7→8→9, every
  intermediate jar required) would be needed. The app already has a full re-index path
  (`reIndexAll()` publishing to the `searchIndexProcessorQueue`), so rebuilding is simpler
  and lower-risk.
  - Alternatives considered: stepwise `IndexUpgrader` chain — rejected (complex, brittle,
    needs all intermediate Lucene versions on the classpath).
- **Decision: detect the incompatible index on startup and recover by clearing only —
  the re-index is admin-initiated, never automatic.**
  `SearchAPI.openWriterAndDirectory()` must catch `IndexFormatTooOldException` (and
  `IndexFormatTooNewException`), clear the `searchindex` directory, recreate an empty
  writer, and log a prominent instruction that an administrator must start a full
  re-index — instead of leaving the index permanently uninitialized. The server does NOT
  auto-trigger `reIndexAll()`, to avoid surprise load on large installations.
- **Decision: expose `reIndexAll()` over the REST API under the existing "Search"
  category** so the admin can start the rebuild without the desktop client. The
  `SearchEndpointV8` class (`@Path("/v8/search")`, `@Api(tags={"Search"})`) gains a
  `POST /v8/search/reindex` operation guarded by `@RolesAllowed({"adminRole"})`. This
  reuses the existing v8 search endpoint rather than introducing a new API version
  (purely additive — no breaking change to v8).

- **Decision: close the index via the container lifecycle, not a JVM shutdown hook.**
  `SearchAPI` previously registered a `Runtime.getRuntime().addShutdownHook(...)` to close
  the writer. JVM shutdown hooks run only at JVM exit — after WildFly has undeployed the
  EAR and begun tearing down the deployment module classloader. `IndexWriter.close()`
  performs a final commit that lazily loads `IndexFileDeleter$CommitPoint`, which then
  fails with `NoClassDefFoundError`. This is hit reliably after the incompatible-index
  clear (the fresh empty index never committed during runtime, so the first commit is at
  close). Fix: remove the shutdown hook; expose a static `SearchAPI.shutdownInstance()`
  (closes only if the singleton was ever created) and call it from
  `ContainerLifecycleBean#terminate()` (`@PreDestroy`), which runs while the classloader is
  still available.

### Concrete API migration (Lucene 4.7 → 9.x) in `SearchAPI.java`

- Drop the `org.apache.lucene.util.Version` argument everywhere:
  - `new GermanAnalyzer(Version.LUCENE_47)` → `new GermanAnalyzer()`
  - `new IndexWriterConfig(Version.LUCENE_CURRENT, analyzer)` → `new IndexWriterConfig(analyzer)`
  - `new QueryParser(Version.LUCENE_CURRENT, FIELD_DEFAULT, analyzer)` → `new QueryParser(FIELD_DEFAULT, analyzer)`
- `FSDirectory.open(File)` → `FSDirectory.open(Path)` (use `dstDir.toPath()`).
- Field indexing: `FieldType.setIndexed(true)` is removed; indexing is implied by
  `setIndexOptions(...)`. Move the enum from `FieldInfo.IndexOptions.*` to
  `org.apache.lucene.index.IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS`.
- Per-document analyzer overloads removed: `writer.addDocument(doc, analyzer)` →
  `addDocument(doc)`; `writer.updateDocument(term, doc, analyzer)` →
  `updateDocument(term, doc)` (the analyzer comes from `IndexWriterConfig`).
- `SearcherManager(IndexWriter, boolean applyAllDeletes, SearcherFactory)` →
  `new SearcherManager(writer, null)`.
- `DirectoryReader.open(IndexWriter, boolean)` → `DirectoryReader.open(writer)`.
- Locking changed: `IndexWriter.unlock(Directory)` and
  `IndexWriterConfig.setWriteLockTimeout(...)` are gone. Lucene 9 uses
  `NativeFSLockFactory` by default, which auto-releases a stale lock left by a crashed JVM
  (the OS releases the underlying file lock), so a `LockObtainFailedException` now reliably
  means another live process holds the index. The old manual unlock/timeout/retry logic is
  therefore **removed entirely** — deleting `write.lock` under a native lock is both
  ineffective and unsafe (it would risk two concurrent writers). A genuine
  `LockObtainFailedException` propagates into the existing `initError` + cooldown-retry path
  (`ensureInitialized()`), which is the correct behavior.
- Highlighter: the `Highlighter`/`QueryScorer`/`SimpleHTMLFormatter` API is retained in
  9.x. The deprecated `TokenSources.getAnyTokenStream(...)` convenience is replaced with the
  stable, definitely-present `TokenSources.getTermVectorTokenStreamOrNull(field, tvFields,
  -1)` (the field stores term vectors with offsets), falling back to
  `analyzer.tokenStream(field, text)` when no term vectors are available.
  `IndexSearcher.doc(int)` still works (deprecated in 9 in favor of `storedFields()`); left
  as-is.

### REST re-index endpoint

The full re-index is already implemented end-to-end (`SearchService.reIndexAll()` →
`searchIndexProcessorQueue` → `SearchIndexProcessor`), but `reIndexAll()` is currently
declared only on `SearchServiceRemote` (used by the desktop client), not on
`SearchServiceLocal` (used by the REST layer). Exposing it via REST therefore needs:

- Add `void reIndexAll();` to `SearchServiceLocal`
  (`j-lawyer-server-ejb/.../services/SearchServiceLocal.java`). The bean already
  implements it, so no bean change is required.
- Add `Response reIndexAll();` to `SearchEndpointLocalV8` and implement it in
  `SearchEndpointV8`: `@POST @Path("/reindex") @RolesAllowed({"adminRole"})` with an
  `@ApiOperation` so it appears under the "Search" tag in Swagger. It looks up
  `SearchServiceLocal` via the existing JNDI name, calls `reIndexAll()`, and returns
  `202 Accepted` (the rebuild runs asynchronously via JMS), or `500` on lookup failure.
- The swagger.json is regenerated from these annotations on build (no manual step).

### POM changes

- Root `pom.xml` `dependencyManagement`: set every `org.apache.lucene:*` entry to the
  chosen 9.x version, and rename `lucene-analyzers-common` → `lucene-analysis-common`.
- Each module POM declaring Lucene (`j-lawyer-server-ejb`, `j-lawyer-server-war`,
  `j-lawyer-server-io`, `j-lawyer-io`, `j-lawyer-server-ear`): update the analyzers
  artifactId to `lucene-analysis-common`; versions inherit from `dependencyManagement`.

## Performance & index size impact

Directional only — exact figures depend on the corpus (German legal documents) and the
query mix, so they must be measured before/after (see tasks). The direction follows from
the Lucene 4.7 → 9.x evolution and from how `SearchAPI` indexes.

### Performance

- **Query latency — likely faster for top-N.** Lucene 8 made **Block-Max WAND** the
  default, which strongly speeds up disjunctive top-k queries. `SearchAPI.search()` only
  asks for the top `maxDocs` (`searcher.search(query, maxDocs)`), so it benefits directly.
  Caveat: the parser enables `setAllowLeadingWildcard(true)`; pure wildcard /
  leading-wildcard queries rewrite to `MultiTermQuery` and benefit little from BMW, so the
  gain depends on the actual query mix.
- **Indexing — comparable or better** (improved merge policy and flushing); no regression
  expected.
- **Heap — lower, with more reliance on the OS page cache.** Successive Lucene versions
  moved more structures (terms index/FST, norms, doc values) off-heap via
  `MMapDirectory`. This reduces JVM heap pressure on WildFly but assumes enough free RAM
  for the page cache.
- **Highlighting — unchanged.** The classic `Highlighter` + `TokenSources` path keeps its
  behavior and cost (see the optional `UnifiedHighlighter` migration below).
- **Analyzer — recall/precision, not speed.** `GermanAnalyzer` stemming/stopwords changed
  across majors, slightly altering tokenization and therefore hit sets; this is a
  correctness consideration, not a performance one.

### Index size on disk

- This index is **storage-dominated**: each document stores the full text **twice** —
  `FIELD_TEXT` (`TextField`, `Store.YES`) and `FIELD_TEXT_TERMVECTOR` (stored *and* with
  term vectors including offsets + positions). Stored fields and term vectors dominate
  size, not the postings.
- Stored-field and term-vector **compression improved markedly** (notably from Lucene 8.7:
  better LZ4, preset dictionaries, block dedup), at the same `BEST_SPEED` default as 4.7.
  Same content therefore tends to occupy **the same or less** space.
- The mandatory **full re-index** produces a fresh, well-merged index without accumulated
  tombstones/segment fragmentation, which usually **shrinks** the on-disk footprint versus
  a long-lived 4.7 index.
- **Net expectation:** roughly the same, more likely slightly smaller. A large increase is
  not expected.

### Optional future optimization (out of scope here)

Migrating from the classic `Highlighter` to the `UnifiedHighlighter` would allow dropping
the `FIELD_TEXT_TERMVECTOR` field (and its offsets), since the UnifiedHighlighter can
highlight from postings offsets or by re-analysis. That could reduce index size
substantially. It is intentionally kept out of this upgrade (separate change) to keep the
Lucene migration behavior-preserving; it is captured as an optional task.

## Risks / Trade-offs

- **Search unavailable during rebuild** → mitigate by documenting the one-time re-index in
  release notes; `reIndexAll()` runs asynchronously via JMS so the server stays up.
- **GermanAnalyzer stopword/stemming differences across majors** may slightly change
  tokenization and therefore hit sets → acceptable; queries and scoring are best-effort
  full-text, not exact contracts. Smoke-test representative German queries after rebuild.
- **Highlighter token-source API drift** → isolated to `search()`; verify highlighting
  output on a known document.
- **Transitive dependency changes** pulled in by Lucene 9 → keep them controlled via
  `dependencyManagement`/exclusions as the build-system spec already requires.

## Migration Plan

1. Update POMs (version + artifact rename) and rewrite `SearchAPI.java`.
2. Add startup detection of `IndexFormatTooOldException` → clear + recreate index dir.
3. Build the reactor, deploy the EAR.
4. On first start the old index is discarded (auto-cleared) and a re-index instruction is
   logged. An administrator then starts the full re-index — either via the existing
   `SearchIndexOptionsDialog` in the desktop client, or via the new
   `POST /v8/search/reindex` REST endpoint.
5. Verify: index document count grows, search returns hits, highlighting renders.
6. Rollback: redeploy the previous EAR; the old 4.x index directory must be restored from
   backup (or simply re-indexed again on the old version), since the rebuilt 9.x index is
   unreadable by Lucene 4.

## Open Questions

- None. The incompatible-index recovery auto-clears the directory and logs an
  instruction; the re-index is always admin-initiated (desktop dialog or the new
  `POST /v8/search/reindex` endpoint).
