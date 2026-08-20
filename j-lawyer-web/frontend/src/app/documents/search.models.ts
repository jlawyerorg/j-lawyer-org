/**
 * Global document (fulltext) search models, aligned with the REST DTO returned by
 * j-lawyer-io: GET /rest/v8/search/fulltext?query=&maxDocs= -> RestfulSearchHitV8[].
 */

/** A single fulltext search hit: one document, with the case it belongs to. */
export interface SearchHit {
  /** Document id (usable with /v1/cases/document/{id}/content). */
  id: string;
  fileName: string;
  /** Owning case id — used to deep-link to /cases/:archiveFileId. */
  archiveFileId: string;
  archiveFileName: string;
  archiveFileNumber: string;
  /** Context snippet around the match (may be empty). */
  snippet: string;
  /** Lucene relevance score. */
  score: number;
  /** Upper-case file extension derived from the file name (e.g. "PDF"). */
  ext: string;
}
