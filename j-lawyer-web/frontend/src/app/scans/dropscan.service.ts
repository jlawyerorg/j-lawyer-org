import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_ROOT } from '../core/api';

const DROPSCAN = `${API_ROOT}/v8/dropscan`;

/** A Dropscan scanbox (DropscanScanbox). */
export interface DropscanScanbox {
  id: number;
  number: string;
  autoScan?: boolean;
  autoDestroy?: number;
  emailInboxAddress?: string;
}

/** A Dropscan mailing / postal item (DropscanMailing). Dates are epoch ms or ISO strings. */
export interface DropscanMailing {
  uuid: string;
  scanboxId: number;
  scanboxNumber: string;
  recipientId?: number;
  recipientName?: string;
  status: string;
  receivedVia?: string;
  receivedAt?: number | string;
  updatedAt?: number | string;
  scannedAt?: number | string;
  scanned?: boolean;
}

/** A suggested case for a mailing (RestfulSuggestedCaseV7). */
export interface SuggestedCase {
  id: string;
  fileNumber: string;
  name: string;
  reason?: string;
  archived?: boolean;
  source?: string;
}

/** Case-assignment suggestions for a mailing (RestfulCaseSuggestionsV7 subset). */
export interface CaseSuggestions {
  suggestedCases: SuggestedCase[];
  phoneNumbers: string[];
}

/**
 * Dropscan integration, backed by `/v8/dropscan` — scoped to the authenticated user's own Dropscan
 * account. Lists scanboxes and mailings, fetches the scanned PDF / envelope image, and requests a
 * scan or destruction. Importing a mailing into a case reuses `CasesService.uploadDocument`.
 */
@Injectable({ providedIn: 'root' })
export class DropscanService {
  private readonly http = inject(HttpClient);

  listScanboxes(): Observable<DropscanScanbox[]> {
    return this.http.get<DropscanScanbox[]>(`${DROPSCAN}/scanboxes`);
  }

  listMailings(scanboxId?: string, status?: string): Observable<DropscanMailing[]> {
    const params: Record<string, string> = {};
    if (scanboxId) { params['scanboxId'] = scanboxId; }
    if (status) { params['status'] = status; }
    return this.http.get<DropscanMailing[]>(`${DROPSCAN}/mailings`, { params });
  }

  /** The scanned PDF of a mailing as a Blob (preview + import source). */
  mailingPdf(scanboxId: string | number, uuid: string): Observable<Blob> {
    return this.http.get(`${DROPSCAN}/scanboxes/${encodeURIComponent(String(scanboxId))}/mailings/${encodeURIComponent(uuid)}/pdf`,
      { responseType: 'blob' });
  }

  /** The envelope image of a mailing as a Blob (preview before scanning). */
  envelopeImage(scanboxId: string | number, uuid: string): Observable<Blob> {
    return this.http.get(`${DROPSCAN}/scanboxes/${encodeURIComponent(String(scanboxId))}/mailings/${encodeURIComponent(uuid)}/envelope`,
      { responseType: 'blob' });
  }

  /** Case-assignment suggestions for a scanned mailing (matched from its OCR text). */
  caseSuggestions(scanboxId: string | number, uuid: string): Observable<CaseSuggestions> {
    return this.http.get<CaseSuggestions>(`${DROPSCAN}/scanboxes/${encodeURIComponent(String(scanboxId))}/mailings/${encodeURIComponent(uuid)}/case-suggestions`);
  }

  requestScan(scanboxId: string | number, uuid: string): Observable<unknown> {
    return this.http.post(`${DROPSCAN}/scanboxes/${encodeURIComponent(String(scanboxId))}/mailings/${encodeURIComponent(uuid)}/scan`, {});
  }

  requestDestroy(scanboxId: string | number, uuid: string): Observable<unknown> {
    return this.http.post(`${DROPSCAN}/scanboxes/${encodeURIComponent(String(scanboxId))}/mailings/${encodeURIComponent(uuid)}/destroy`, {});
  }
}
