import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_ROOT } from '../core/api';
import { base64ToBytes, DocumentContentDto, mimeOf, PreviewDoc } from './document-preview.models';

/**
 * Fetches a document's bytes (GET /v1/cases/document/{id}/content) and triggers a browser
 * download. Shared by the case detail and the fulltext-search results so document access is
 * defined once. The Bearer token is attached by authInterceptor.
 */
@Injectable({ providedIn: 'root' })
export class DocumentContentService {
  private readonly http = inject(HttpClient);

  content(id: string): Observable<DocumentContentDto> {
    return this.http.get<DocumentContentDto>(`${API_ROOT}/v1/cases/document/${id}/content`);
  }

  /** Fetches the document and triggers a browser download with its file name. */
  download(doc: PreviewDoc): void {
    this.content(doc.id).subscribe({
      next: (dto) => {
        const bytes = base64ToBytes((dto.base64content ?? '').replace(/\s/g, ''));
        const url = URL.createObjectURL(new Blob([bytes], { type: mimeOf(doc.ext) }));
        const a = document.createElement('a');
        a.href = url;
        a.download = dto.fileName || doc.name;
        document.body.appendChild(a);
        a.click();
        a.remove();
        setTimeout(() => URL.revokeObjectURL(url), 10_000);
      },
    });
  }
}
