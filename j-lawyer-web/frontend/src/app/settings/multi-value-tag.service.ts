import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_ROOT } from '../core/api';

const MV_V7 = `${API_ROOT}/v7/configuration/tags/multivalue`;

/** Entity a multi-value tag applies to. */
export type MvEntityType = 'case' | 'address' | 'document';

/** A multi-value tag definition ("Listenetikett", RestfulMultiValueTagDefinitionV7). */
export interface MvTagDefinition {
  tagName: string;
  values: string[];
}

/**
 * Multi-value tags (Listenetiketten) for cases/addresses/documents over the v7 configuration
 * endpoint. Listing is open to any authenticated user; adds/renames need `createOptionGroupRole`,
 * removals `deleteOptionGroupRole` (enforced server-side). All mutations cascade to tags already
 * attached to entities so definitions and assignments stay consistent.
 */
@Injectable({ providedIn: 'root' })
export class MultiValueTagService {
  private readonly http = inject(HttpClient);

  list(entityType: MvEntityType): Observable<MvTagDefinition[]> {
    return this.http.get<MvTagDefinition[]>(`${MV_V7}/${entityType}`);
  }

  addValue(entityType: MvEntityType, tagName: string, value: string): Observable<unknown> {
    return this.http.post(`${MV_V7}/${entityType}/values`, { tagName, value });
  }

  removeValue(entityType: MvEntityType, tagName: string, value: string): Observable<unknown> {
    return this.http.request('delete', `${MV_V7}/${entityType}/values`, { body: { tagName, value } });
  }

  renameValue(entityType: MvEntityType, tagName: string, value: string, newValue: string): Observable<unknown> {
    return this.http.post(`${MV_V7}/${entityType}/values/rename`, { tagName, value, newValue });
  }

  renameTag(entityType: MvEntityType, tagName: string, newTagName: string): Observable<unknown> {
    return this.http.post(`${MV_V7}/${entityType}/rename`, { tagName, newTagName });
  }

  removeTag(entityType: MvEntityType, tagName: string): Observable<unknown> {
    return this.http.request('delete', `${MV_V7}/${entityType}`, { body: { tagName } });
  }
}
