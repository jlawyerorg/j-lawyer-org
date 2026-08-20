import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_ROOT } from '../core/api';

const PARTY_TYPES_V7 = `${API_ROOT}/v7/configuration/party-types`;

/** A party type (Beteiligtentyp, RestfulPartyTypeV7): a role a party can take in a case. */
export interface PartyType {
  id?: string;
  name: string;
  /** Document placeholder token, e.g. used when generating documents. */
  placeHolder: string;
  /** ARGB/RGB color as a packed integer (as stored by the desktop). */
  color: number;
  /** Sort sequence within the list. */
  sequenceNumber: number;
}

/**
 * Party-type master data (Beteiligtentypen) over the v7 configuration endpoint. Listing is open to
 * any authenticated user; create/update/delete require `adminRole` (enforced server-side). Deleting a
 * type still referenced by a party fails with HTTP 409 and a message.
 */
@Injectable({ providedIn: 'root' })
export class PartyTypeService {
  private readonly http = inject(HttpClient);

  list(): Observable<PartyType[]> {
    return this.http.get<PartyType[]>(PARTY_TYPES_V7);
  }

  create(t: PartyType): Observable<PartyType> {
    return this.http.put<PartyType>(PARTY_TYPES_V7, t);
  }

  update(t: PartyType): Observable<PartyType> {
    return this.http.post<PartyType>(PARTY_TYPES_V7, t);
  }

  delete(t: PartyType): Observable<unknown> {
    return this.http.request('delete', PARTY_TYPES_V7, { body: t });
  }
}
