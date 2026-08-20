import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_ROOT } from '../core/api';

const CASE_NUMBERING_V7 = `${API_ROOT}/v7/configuration/case-numbering`;

/** Server-wide case-number (Aktenzeichen) configuration (RestfulCaseNumberingV7). */
export interface CaseNumbering {
  /** Numbering pattern with year/month/day, running-index and random tokens. */
  pattern: string;
  startFrom: number;
  /** Step between running numbers (1..10). */
  increment: number;
  extensionEnabled: boolean;
  dividerMain: string;
  dividerExt: string;
  prefixEnabled: boolean;
  prefix: string;
  suffixEnabled: boolean;
  suffix: string;
  /** Append the creating lawyer's abbreviation (Anwaltskürzel). */
  lawyerAbbrevEnabled: boolean;
  /** Append the case group's abbreviation (Gruppenkürzel). */
  groupAbbrevEnabled: boolean;
}

/**
 * Case-number configuration over the v7 configuration endpoint. Reading is open to any authenticated
 * user; saving requires `adminRole` (enforced server-side). `preview` returns a sample case number
 * for a candidate configuration without storing it; an invalid pattern yields HTTP 409 with a message.
 */
@Injectable({ providedIn: 'root' })
export class CaseNumberingService {
  private readonly http = inject(HttpClient);

  get(): Observable<CaseNumbering> {
    return this.http.get<CaseNumbering>(CASE_NUMBERING_V7);
  }

  save(c: CaseNumbering): Observable<CaseNumbering> {
    return this.http.put<CaseNumbering>(CASE_NUMBERING_V7, c);
  }

  preview(c: CaseNumbering): Observable<string[]> {
    return this.http.post<string[]>(`${CASE_NUMBERING_V7}/preview`, c);
  }
}
