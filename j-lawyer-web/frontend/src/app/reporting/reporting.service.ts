import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { API_ROOT } from '../core/api';
import { BarChart, ReportMeta, ReportResult, ReportTable } from './reporting.models';

const REPORTS_V7 = `${API_ROOT}/v7/reports`;

interface ReportMetaDto {
  reportId: string; name: string; description: string; category: string; sequence: number;
  typeChart: boolean; typeTable: boolean; securityType: string; dateSelectionLabel: string;
}
interface ReportResultDto {
  tables?: ReportTable[];
  barCharts?: BarChart[];
}

/**
 * Reporting data access against the real REST API (GET /v7/reports/list, POST /v7/reports/invoke,
 * both ACL-restricted via the commonReportRole). The Bearer token is attached by authInterceptor.
 */
@Injectable({ providedIn: 'root' })
export class ReportingService {
  private readonly http = inject(HttpClient);

  /** Loads the metadata of all reports the caller may run. */
  list(): Observable<ReportMeta[]> {
    return this.http.get<ReportMetaDto[]>(`${REPORTS_V7}/list`).pipe(
      map((rows) => (rows ?? []).map(toMeta)),
    );
  }

  /**
   * Runs a report. `fromDate`/`toDate` are ISO days (yyyy-MM-dd) or empty for reports that
   * take no date range.
   */
  invoke(reportId: string, fromDate: string, toDate: string): Observable<ReportResult> {
    return this.http.post<ReportResultDto>(`${REPORTS_V7}/invoke`, { reportId, fromDate, toDate }).pipe(
      map((r) => ({ tables: r?.tables ?? [], barCharts: r?.barCharts ?? [] })),
    );
  }
}

function toMeta(dto: ReportMetaDto): ReportMeta {
  return {
    reportId: dto.reportId,
    name: dto.name ?? '',
    description: dto.description ?? '',
    category: dto.category ?? '',
    sequence: dto.sequence ?? 0,
    typeChart: !!dto.typeChart,
    typeTable: !!dto.typeTable,
    securityType: dto.securityType ?? '',
    dateSelectionLabel: dto.dateSelectionLabel ?? '',
  };
}
