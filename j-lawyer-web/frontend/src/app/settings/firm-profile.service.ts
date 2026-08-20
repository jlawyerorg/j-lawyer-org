import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_ROOT } from '../core/api';

const CONFIG_V7 = `${API_ROOT}/v7/configuration`;

/**
 * Firm master data ("Kanzleidaten", RestfulFirmProfileV7): company address, contact details, tax
 * identifiers and bank accounts. All fields are strings; empty means unset.
 */
export interface FirmProfile {
  companyName: string;
  street: string;
  street2: string;
  zipCode: string;
  city: string;
  country: string;
  phone: string;
  fax: string;
  mobile: string;
  email: string;
  website: string;
  taxId: string;
  vatId: string;
  bank: string;
  bic: string;
  iban: string;
  escrowBank: string;
  escrowBic: string;
  escrowIban: string;
}

/**
 * Reads and writes the firm master data over the v7 configuration endpoint. Reading is open to any
 * authenticated user; the write requires `adminRole` (enforced server-side, and the settings screen
 * that hosts the editor is admin-gated).
 */
@Injectable({ providedIn: 'root' })
export class FirmProfileService {
  private readonly http = inject(HttpClient);

  get(): Observable<FirmProfile> {
    return this.http.get<FirmProfile>(`${CONFIG_V7}/firm-profile`);
  }

  save(profile: FirmProfile): Observable<FirmProfile> {
    return this.http.put<FirmProfile>(`${CONFIG_V7}/firm-profile`, profile);
  }
}
