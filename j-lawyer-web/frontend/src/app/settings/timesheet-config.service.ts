import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_ROOT } from '../core/api';

const TS_V8 = `${API_ROOT}/v8/timesheets/templates`;
const TS_SETTINGS_V7 = `${API_ROOT}/v7/configuration/timesheet-settings`;

/** A timesheet position template (Zeiterfassungsposition, RestfulTimesheetPositionTemplateV8). */
export interface TimesheetPositionTemplate {
  id?: string;
  name: string;
  description: string;
  unitPrice: number;
  taxRate: number;
}

/** Global time-tracking settings (RestfulTimesheetSettingsV7). */
export interface TimesheetSettings {
  parallelLogsWarning: boolean;
  numericInput: 'minutes' | 'hours' | 'reject';
}

/**
 * Time-tracking configuration: the global pool of position templates (v8 timesheets endpoint) and the
 * global settings (v7 configuration). Listing/reading is open to authenticated users; all mutations
 * require `adminRole` (enforced server-side).
 */
@Injectable({ providedIn: 'root' })
export class TimesheetConfigService {
  private readonly http = inject(HttpClient);

  listTemplates(): Observable<TimesheetPositionTemplate[]> { return this.http.get<TimesheetPositionTemplate[]>(TS_V8); }
  createTemplate(t: TimesheetPositionTemplate): Observable<TimesheetPositionTemplate> { return this.http.put<TimesheetPositionTemplate>(TS_V8, t); }
  updateTemplate(t: TimesheetPositionTemplate): Observable<TimesheetPositionTemplate> { return this.http.post<TimesheetPositionTemplate>(TS_V8, t); }
  deleteTemplate(id: string): Observable<unknown> { return this.http.delete(`${TS_V8}/${encodeURIComponent(id)}`); }

  getSettings(): Observable<TimesheetSettings> { return this.http.get<TimesheetSettings>(TS_SETTINGS_V7); }
  saveSettings(s: TimesheetSettings): Observable<TimesheetSettings> { return this.http.put<TimesheetSettings>(TS_SETTINGS_V7, s); }
}
