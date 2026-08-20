import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { API_ROOT } from '../core/api';
import { SearchHit } from './search.models';

const SEARCH_BASE = `${API_ROOT}/v8/search`;

/** Wire shape returned by the fulltext endpoint (only the fields we consume). */
interface SearchHitDto {
  id: string;
  fileName: string;
  archiveFileId: string;
  archiveFileName: string;
  archiveFileNumber: string;
  snippet: string;
  score: number;
}

/**
 * Global document search against the real REST API (GET /v8/search/fulltext, Lucene-backed,
 * ACL-restricted server-side). The Bearer token is attached by authInterceptor.
 */
@Injectable({ providedIn: 'root' })
export class DocumentsService {
  private readonly http = inject(HttpClient);

  /** Runs a fulltext search over the document index; returns the ranked hits. */
  searchFulltext(query: string, maxDocs = 50): Observable<SearchHit[]> {
    const params = new HttpParams().set('query', query).set('maxDocs', String(maxDocs));
    return this.http.get<SearchHitDto[]>(`${SEARCH_BASE}/fulltext`, { params }).pipe(
      map((rows) => (rows ?? []).map(toHit)),
    );
  }
}

function toHit(dto: SearchHitDto): SearchHit {
  return {
    id: dto.id,
    fileName: dto.fileName ?? '',
    archiveFileId: dto.archiveFileId ?? '',
    archiveFileName: dto.archiveFileName ?? '',
    archiveFileNumber: dto.archiveFileNumber ?? '',
    snippet: dto.snippet ?? '',
    score: dto.score ?? 0,
    ext: extensionOf(dto.fileName ?? ''),
  };
}

function extensionOf(name: string): string {
  const dot = name.lastIndexOf('.');
  return dot > -1 && dot < name.length - 1 ? name.slice(dot + 1).toUpperCase() : '';
}
