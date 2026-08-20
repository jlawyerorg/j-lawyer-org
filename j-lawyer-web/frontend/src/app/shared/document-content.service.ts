import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_ROOT } from '../core/api';
import { base64ToBytes, DocumentContentDto, mimeOf, PreviewDoc } from './document-preview.models';

/**
 * Fetches a document's bytes and triggers a browser download. Shared by the case detail, the
 * fulltext-search results and the contact detail so document access is defined once. Case
 * documents live under /v1/cases/document/{id}/content, contact documents under
 * /v7/contacts/document/{id}/content. The Bearer token is attached by authInterceptor.
 */
@Injectable({ providedIn: 'root' })
export class DocumentContentService {
  private readonly http = inject(HttpClient);

  content(id: string, source: 'case' | 'contact' = 'case'): Observable<DocumentContentDto> {
    const url = source === 'contact'
      ? `${API_ROOT}/v7/contacts/document/${id}/content`
      : `${API_ROOT}/v1/cases/document/${id}/content`;
    return this.http.get<DocumentContentDto>(url);
  }

  /** Fetches the document and triggers a browser download with its file name. */
  download(doc: PreviewDoc): void {
    this.content(doc.id, doc.source).subscribe({
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
