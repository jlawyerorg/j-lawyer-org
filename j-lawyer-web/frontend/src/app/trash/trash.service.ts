import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_ROOT } from '../core/api';

const BIN_V8 = `${API_ROOT}/v8/documents/bin`;

/** A soft-deleted document in the recycle bin (RestfulBinDocumentV8). */
export interface BinDocument {
  type: 'case' | 'address';
  id: string;
  name: string;
  /** Epoch milliseconds. */
  deletionDate: number;
  deletedBy: string;
  size: number;
  ownerId: string;
  ownerLabel: string;
  ownerReference: string;
}

/** The recycle-bin overview (RestfulDocumentBinV8). */
export interface DocumentBin {
  retentionDays: number;
  totalBytes: number;
  documents: BinDocument[];
}

/**
 * The document recycle bin ("Papierkorb"), backed by `/v8/documents/bin`. Reading needs `loginRole`
 * (partial results if a read role is missing); restore/delete require the matching write role;
 * changing the retention period requires `adminRole` (all enforced server-side).
 */
@Injectable({ providedIn: 'root' })
export class TrashService {
  private readonly http = inject(HttpClient);

  getBin(): Observable<DocumentBin> { return this.http.get<DocumentBin>(BIN_V8); }

  restore(type: 'case' | 'address', id: string): Observable<unknown> {
    return this.http.post(`${BIN_V8}/${type}/${encodeURIComponent(id)}/restore`, {});
  }

  remove(type: 'case' | 'address', id: string): Observable<unknown> {
    return this.http.delete(`${BIN_V8}/${type}/${encodeURIComponent(id)}`);
  }

  setRetention(retentionDays: number): Observable<unknown> {
    return this.http.put(`${BIN_V8}/retention`, { retentionDays });
  }
}
