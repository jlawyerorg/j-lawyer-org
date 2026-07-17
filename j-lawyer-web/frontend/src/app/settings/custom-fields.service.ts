import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, shareReplay, tap } from 'rxjs/operators';
import { API_ROOT } from '../core/api';

const CONFIG_V7 = `${API_ROOT}/v7/configuration`;

/** Entity a custom-field set belongs to. */
export type CustomFieldEntity = 'address' | 'case' | 'party';

/** The three custom-field labels (RestfulCustomFieldsV7); an empty label = field not in use. */
export interface CustomFieldLabels {
  label1: string;
  label2: string;
  label3: string;
}

/** A configured (non-empty) custom field: which slot it is and its label. */
export interface CustomField {
  key: 'custom1' | 'custom2' | 'custom3';
  label: string;
}

/**
 * Custom-field ("eigene Felder") labels for addresses, cases and involved parties, over the v7
 * configuration endpoint. Reading is open to any authenticated user (views need the labels); saving
 * requires `adminRole`. Labels are cached per entity type (they change rarely) and re-fetched after a
 * save. {@link configuredFields} turns the labels into the list of fields that are actually in use.
 */
@Injectable({ providedIn: 'root' })
export class CustomFieldsService {
  private readonly http = inject(HttpClient);
  private readonly cache = new Map<CustomFieldEntity, Observable<CustomFieldLabels>>();

  /** The three labels for an entity type (cached). Returns empty labels on error. */
  labels(entityType: CustomFieldEntity): Observable<CustomFieldLabels> {
    let obs = this.cache.get(entityType);
    if (!obs) {
      obs = this.http.get<CustomFieldLabels>(`${CONFIG_V7}/custom-fields/${entityType}`).pipe(
        catchError(() => of({ label1: '', label2: '', label3: '' })),
        shareReplay(1),
      );
      this.cache.set(entityType, obs);
    }
    return obs;
  }

  /** The custom fields that are actually configured (non-empty label), in slot order. */
  configuredFields(labels: CustomFieldLabels): CustomField[] {
    const fields: CustomField[] = [];
    if (labels.label1?.trim()) { fields.push({ key: 'custom1', label: labels.label1.trim() }); }
    if (labels.label2?.trim()) { fields.push({ key: 'custom2', label: labels.label2.trim() }); }
    if (labels.label3?.trim()) { fields.push({ key: 'custom3', label: labels.label3.trim() }); }
    return fields;
  }

  save(entityType: CustomFieldEntity, labels: CustomFieldLabels): Observable<CustomFieldLabels> {
    return this.http.put<CustomFieldLabels>(`${CONFIG_V7}/custom-fields/${entityType}`, labels).pipe(
      tap(() => this.cache.delete(entityType)),
    );
  }
}
