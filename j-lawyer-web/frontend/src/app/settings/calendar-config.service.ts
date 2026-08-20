import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_ROOT } from '../core/api';

const CONFIG_V7 = `${API_ROOT}/v7/configuration`;

/** eventType values of a calendar (mirrors EventTypes on the server). */
export const CAL_EVENT_TYPES = [
  { value: 10, labelKey: 'settings.calendar.type.followup' },
  { value: 20, labelKey: 'settings.calendar.type.respite' },
  { value: 30, labelKey: 'settings.calendar.type.event' },
];

/** A calendar definition (RestfulCalendarSetupV7). cloudPassword is write-only. */
export interface CalendarSetup {
  id?: string;
  displayName: string;
  href: string;
  eventType: number;
  background: number;
  deleteDone: boolean;
  cloudHost: string;
  cloudPort: number;
  cloudSsl: boolean;
  cloudPath: string;
  cloudUser: string;
  cloudPassword: string;
  passwordSet: boolean;
}

/** A CalDAV calendar (RestfulCloudCalendarV7). */
export interface CloudCalendar { href: string; displayName: string; }

/** A calendar entry template (RestfulCalendarEntryTemplateV7). */
export interface CalendarEntryTemplate {
  id?: string;
  name: string;
  description: string;
  related: boolean;
  relatedName: string;
  relatedDescription: string;
  relatedOffsetDays: number;
}

/**
 * Calendar configuration over the v7 configuration endpoint: calendar definitions (CalDAV) and entry
 * templates. All operations require `adminRole` (enforced server-side). Calendar passwords are
 * write-only. `testConnection` lists the CalDAV calendars for a connection (falls back to the stored
 * password when left empty); `runSync` triggers a full calendar sync.
 */
@Injectable({ providedIn: 'root' })
export class CalendarConfigService {
  private readonly http = inject(HttpClient);

  listSetups(): Observable<CalendarSetup[]> { return this.http.get<CalendarSetup[]>(`${CONFIG_V7}/calendar-setups`); }
  createSetup(s: CalendarSetup): Observable<CalendarSetup> { return this.http.put<CalendarSetup>(`${CONFIG_V7}/calendar-setups`, s); }
  updateSetup(s: CalendarSetup): Observable<CalendarSetup> { return this.http.post<CalendarSetup>(`${CONFIG_V7}/calendar-setups`, s); }
  deleteSetup(s: CalendarSetup): Observable<unknown> { return this.http.request('delete', `${CONFIG_V7}/calendar-setups`, { body: s }); }
  testConnection(s: CalendarSetup): Observable<CloudCalendar[]> { return this.http.post<CloudCalendar[]>(`${CONFIG_V7}/calendar-setups/test-connection`, s); }
  runSync(): Observable<unknown> { return this.http.post(`${CONFIG_V7}/calendar-setups/sync`, {}); }

  listTemplates(): Observable<CalendarEntryTemplate[]> { return this.http.get<CalendarEntryTemplate[]>(`${CONFIG_V7}/calendar-entry-templates`); }
  createTemplate(t: CalendarEntryTemplate): Observable<CalendarEntryTemplate> { return this.http.put<CalendarEntryTemplate>(`${CONFIG_V7}/calendar-entry-templates`, t); }
  updateTemplate(t: CalendarEntryTemplate): Observable<CalendarEntryTemplate> { return this.http.post<CalendarEntryTemplate>(`${CONFIG_V7}/calendar-entry-templates`, t); }
  deleteTemplate(t: CalendarEntryTemplate): Observable<unknown> { return this.http.request('delete', `${CONFIG_V7}/calendar-entry-templates`, { body: t }); }
}
