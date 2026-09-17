import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { API_ROOT } from '../core/api';
import { ContactRelationType, toContactRelationType } from '../contacts/contact.models';

const RELATION_TYPES_V8 = `${API_ROOT}/v8/configuration/contact-relation-types`;

/**
 * Relationship-type master data (Beziehungsarten zwischen Kontakten) over the v8 configuration
 * endpoint. Listing is open to any authenticated user (the pickers need it); create/update/delete
 * require `adminRole` (enforced server-side). Deleting a type that relationships still reference is
 * refused with a message saying how many use it.
 */
@Injectable({ providedIn: 'root' })
export class ContactRelationTypeService {
  private readonly http = inject(HttpClient);

  /** All types, inactive ones included (the administration shows them), in their configured order. */
  list(): Observable<ContactRelationType[]> {
    return this.http.get<Partial<ContactRelationType>[]>(RELATION_TYPES_V8).pipe(
      map((rows) => (rows ?? []).map(toContactRelationType)),
    );
  }

  /** Creates a type; the id the server generates comes back with it (POST, as v8 defines it). */
  create(t: ContactRelationType): Observable<ContactRelationType> {
    return this.http.post<ContactRelationType>(RELATION_TYPES_V8, t);
  }

  /** Updates the type identified by its id (PUT, as v8 defines it). */
  update(t: ContactRelationType): Observable<ContactRelationType> {
    return this.http.put<ContactRelationType>(RELATION_TYPES_V8, t);
  }

  delete(t: ContactRelationType): Observable<unknown> {
    return this.http.request('delete', RELATION_TYPES_V8, { body: t });
  }
}
