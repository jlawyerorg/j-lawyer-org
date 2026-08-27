import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_ROOT } from '../core/api';
import { base64ToBytes, BeaPreview, DocumentContentDto, EmailPreview, mimeOf, OfficePreview, PreviewDoc } from './document-preview.models';

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

  /**
   * Base64 PDF rendering of a case document for preview — Office formats are converted server-side
   * (StirlingPDF). Case documents only (GET /v8/cases/document/{id}/preview-pdf).
   */
  previewPdf(id: string): Observable<OfficePreview> {
    return this.http.get<OfficePreview>(`${API_ROOT}/v8/cases/document/${id}/preview-pdf`);
  }

  /**
   * Replaces a case document's content with the given Base64 bytes (PUT
   * /v8/cases/document/{id}/content) — used by the HTML rich-text editor's auto-save. Requires
   * writeArchiveFileRole; the server writes a document-history entry. Case documents only.
   */
  updateContent(id: string, base64: string): Observable<void> {
    return this.http.put<void>(`${API_ROOT}/v8/cases/document/${id}/content`, { base64content: base64 });
  }

  /** Base64-parsed EML preview (GET /v8/cases/document/{id}/eml) — case documents only. */
  emlPreview(id: string): Observable<EmailPreview> {
    return this.http.get<EmailPreview>(`${API_ROOT}/v8/cases/document/${id}/eml`);
  }

  /** Base64-parsed beA message preview (GET /v8/cases/document/{id}/bea) — case documents only. */
  beaPreview(id: string): Observable<BeaPreview> {
    return this.http.get<BeaPreview>(`${API_ROOT}/v8/cases/document/${id}/bea`);
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
