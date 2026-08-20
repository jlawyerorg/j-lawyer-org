/**
 * Reporting ("Auswertungen") domain models, aligned with the v7 REST DTOs:
 *  - list:   GET  /rest/v7/reports/list          -> RestfulReportMetadataV7[]
 *  - invoke: POST /rest/v7/reports/invoke {reportId, fromDate, toDate} -> RestfulReportResultV7
 *
 * Reports are defined server-side (ReportCatalog) and grouped by category. A report may
 * produce tables and/or bar charts; those carrying a `dateSelectionLabel` take a date range.
 */

/** Metadata describing one available report. */
export interface ReportMeta {
  reportId: string;
  name: string;
  description: string;
  category: string;
  /** Ordering hint within the category. */
  sequence: number;
  typeChart: boolean;
  typeTable: boolean;
  /** "COMMON" | "CONFIDENTIAL" — informational. */
  securityType: string;
  /** When non-empty, the report accepts a from/to date range; the label describes the date it filters on. */
  dateSelectionLabel: string;
}

/** A tabular report result. */
export interface ReportTable {
  tableName: string;
  columnNames: string[];
  rows: string[][];
  /** When true, column 0 holds the case id: it is hidden and the row links to that case. */
  hasCaseIdColumn: boolean;
  /** When true, the final row is a totals/sum row (rendered emphasised). */
  hasSumRows: boolean;
}

/** One data series of a bar chart (shares the chart's x labels). */
export interface BarSeries {
  name: string;
  xData: string[];
  yData: number[];
  /** Server-provided colour (may be null/empty; we fall back to a palette). */
  fillColor: string | null;
  renderStyle: string | null;
}

/** A bar-chart report result. */
export interface BarChart {
  chartName: string;
  xAxisTitle: string;
  yAxisTitle: string;
  series: BarSeries[];
}

/** The full result of invoking a report. */
export interface ReportResult {
  tables: ReportTable[];
  barCharts: BarChart[];
}

/** Reports of one category, for the grouped list. */
export interface ReportGroup {
  category: string;
  reports: ReportMeta[];
}
