import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_ROOT } from '../core/api';

const NT_V7 = `${API_ROOT}/v7/configuration/document-name-templates`;

/** A document name template (Benennungsschema, RestfulDocumentNameTemplateV7). */
export interface NameTemplate {
  id?: string;
  displayName: string;
  pattern: string;
  defaultTemplate: boolean;
}

/**
 * Document name templates (Benennungsschemata) over the v7 configuration endpoint. Listing is open
 * to any authenticated user; writes require `adminRole` (enforced server-side). Tokens in a pattern:
 * date tokens [yyyy][yy][mm][m][dd][d][HH][H][MM][M] and the literal DATEINAME (original file name).
 */
@Injectable({ providedIn: 'root' })
export class NameTemplateService {
  private readonly http = inject(HttpClient);

  list(): Observable<NameTemplate[]> {
    return this.http.get<NameTemplate[]>(NT_V7);
  }

  create(t: NameTemplate): Observable<NameTemplate> {
    return this.http.put<NameTemplate>(NT_V7, t);
  }

  update(t: NameTemplate): Observable<NameTemplate> {
    return this.http.post<NameTemplate>(NT_V7, t);
  }

  delete(t: NameTemplate): Observable<unknown> {
    return this.http.request('delete', NT_V7, { body: t });
  }

  preview(t: NameTemplate): Observable<string[]> {
    return this.http.post<string[]>(`${NT_V7}/preview`, t);
  }
}
