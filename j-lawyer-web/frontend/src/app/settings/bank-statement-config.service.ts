import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_ROOT } from '../core/api';

const BSC_V7 = `${API_ROOT}/v7/configuration/bankstatement-csv-configs`;

/** A CSV bank-statement import profile (RestfulBankStatementCsvConfigV7). Column values are 0-based indices. */
export interface BankStatementCsvConfig {
  id?: string;
  configurationName: string;
  delimiter: string;
  decimalFormat: string;
  decimalSeparator: string;
  decimalGroupingCharacter: string;
  decimalGrouping: boolean;
  locale: string;
  headerLines: number;
  footerLines: number;
  columnDate: number;
  columnName: number;
  columnBookingType: number;
  columnIban: number;
  columnPurpose: number;
  columnAmount: number;
  columnCurrency: number;
}

/**
 * CSV bank-statement import profiles ("Kontoauszug-Import") over the v7 configuration endpoint. All
 * operations require `adminRole` (enforced server-side). A profile describes how a bank's CSV export
 * is parsed (delimiter, decimals, header/footer lines, per-field column index).
 */
@Injectable({ providedIn: 'root' })
export class BankStatementConfigService {
  private readonly http = inject(HttpClient);

  list(): Observable<BankStatementCsvConfig[]> { return this.http.get<BankStatementCsvConfig[]>(BSC_V7); }
  create(c: BankStatementCsvConfig): Observable<BankStatementCsvConfig> { return this.http.put<BankStatementCsvConfig>(BSC_V7, c); }
  update(c: BankStatementCsvConfig): Observable<BankStatementCsvConfig> { return this.http.post<BankStatementCsvConfig>(BSC_V7, c); }
  delete(c: BankStatementCsvConfig): Observable<unknown> { return this.http.request('delete', BSC_V7, { body: c }); }
}
