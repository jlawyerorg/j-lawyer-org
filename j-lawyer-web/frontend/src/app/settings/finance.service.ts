import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_ROOT } from '../core/api';

const INVOICES_V7 = `${API_ROOT}/v7/invoices`;

/** An invoice type / document category (Belegart, RestfulInvoiceTypeV7). */
export interface InvoiceType {
  id?: string;
  displayName: string;
  description: string;
  /** Whether documents of this type count toward turnover (umsatzwirksam). */
  turnOver: boolean;
}

/** A reusable invoice line-item template (Belegposition-Vorlage, RestfulInvoicePositionTemplateV7). */
export interface InvoicePositionTemplate {
  id?: string;
  name: string;
  description: string;
  taxRate: number;
  units: number;
  unitPrice: number;
}

/** Global finance settings (RestfulFinanceSettingsV7). */
export interface FinanceSettings {
  /** GiroCode (EPC-QR) image edge length in pixels. */
  giroCodePx: number;
}

/** An invoice numbering pool (Belegnummernkreis, RestfulInvoicePoolV7). */
export interface InvoicePool {
  id?: string;
  displayName: string;
  /** Numbering pattern, e.g. with placeholders for year and running index. */
  pattern: string;
  /** Whether the running index may be adjusted manually. */
  manualAdjust: boolean;
  /** Small-business scheme (§19 UStG — no VAT shown). */
  smallBusiness: boolean;
  startIndex: number;
  /** Current running index (last number issued); edited only when manualAdjust is set. */
  lastIndex: number;
  /** Default payment term in days. */
  paymentTerm: number;
}

/**
 * Invoice master-data configuration: types (Belegarten) and numbering pools (Belegnummernkreise),
 * over the v7 invoices endpoints. Listing is open to any authenticated user; create/update/delete
 * require `adminRole` (enforced server-side; the hosting screen is admin-gated). `preview` returns
 * the next numbers a pool configuration would produce, without saving it.
 */
@Injectable({ providedIn: 'root' })
export class FinanceService {
  private readonly http = inject(HttpClient);

  listTypes(): Observable<InvoiceType[]> {
    return this.http.get<InvoiceType[]>(`${INVOICES_V7}/types`);
  }

  createType(t: InvoiceType): Observable<InvoiceType> {
    return this.http.put<InvoiceType>(`${INVOICES_V7}/types`, t);
  }

  updateType(t: InvoiceType): Observable<InvoiceType> {
    return this.http.post<InvoiceType>(`${INVOICES_V7}/types`, t);
  }

  deleteType(t: InvoiceType): Observable<unknown> {
    return this.http.request('delete', `${INVOICES_V7}/types`, { body: t });
  }

  listPools(): Observable<InvoicePool[]> {
    return this.http.get<InvoicePool[]>(`${INVOICES_V7}/pools`);
  }

  createPool(p: InvoicePool): Observable<InvoicePool> {
    return this.http.put<InvoicePool>(`${INVOICES_V7}/pools`, p);
  }

  updatePool(p: InvoicePool): Observable<InvoicePool> {
    return this.http.post<InvoicePool>(`${INVOICES_V7}/pools`, p);
  }

  deletePool(p: InvoicePool): Observable<unknown> {
    return this.http.request('delete', `${INVOICES_V7}/pools`, { body: p });
  }

  /** Next invoice numbers the given pool configuration would produce (not persisted). */
  previewPool(p: InvoicePool): Observable<string[]> {
    return this.http.post<string[]>(`${INVOICES_V7}/pools/preview`, p);
  }

  listPositionTemplates(): Observable<InvoicePositionTemplate[]> {
    return this.http.get<InvoicePositionTemplate[]>(`${INVOICES_V7}/position-templates`);
  }

  createPositionTemplate(t: InvoicePositionTemplate): Observable<InvoicePositionTemplate> {
    return this.http.put<InvoicePositionTemplate>(`${INVOICES_V7}/position-templates`, t);
  }

  updatePositionTemplate(t: InvoicePositionTemplate): Observable<InvoicePositionTemplate> {
    return this.http.post<InvoicePositionTemplate>(`${INVOICES_V7}/position-templates`, t);
  }

  deletePositionTemplate(t: InvoicePositionTemplate): Observable<unknown> {
    return this.http.request('delete', `${INVOICES_V7}/position-templates`, { body: t });
  }

  getFinanceSettings(): Observable<FinanceSettings> {
    return this.http.get<FinanceSettings>(`${API_ROOT}/v7/configuration/finance-settings`);
  }

  saveFinanceSettings(s: FinanceSettings): Observable<FinanceSettings> {
    return this.http.put<FinanceSettings>(`${API_ROOT}/v7/configuration/finance-settings`, s);
  }
}
