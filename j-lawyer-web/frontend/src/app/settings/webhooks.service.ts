import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_ROOT } from '../core/api';

const WEBHOOKS_V7 = `${API_ROOT}/v7/webhooks`;

/** An outbound webhook (RestfulIntegrationHookV7). `name` is the identity. Password write-only. */
export interface Webhook {
  name: string;
  url: string;
  hookType: string;
  authUser: string;
  authPassword?: string;
  authPasswordSet?: boolean;
  connectionTimeout: number;
  readTimeout: number;
}

/** Overview of one webhook execution (RestfulIntegrationHookLogOverview). */
export interface WebhookLogOverview {
  failed: boolean;
  hookId: string;
  hookType: string;
  requestDate: string;
}

/**
 * Webhook configuration + execution logs over the v7 webhooks endpoint. Config CRUD requires
 * `adminRole`; the event-type list and the execution logs require `loginRole` (enforced server-side).
 * The HTTP basic-auth password is write-only.
 */
@Injectable({ providedIn: 'root' })
export class WebhooksService {
  private readonly http = inject(HttpClient);

  listTypes(): Observable<string[]> { return this.http.get<string[]>(`${WEBHOOKS_V7}/types`); }
  list(): Observable<Webhook[]> { return this.http.get<Webhook[]>(`${WEBHOOKS_V7}/hooks`); }
  create(h: Webhook): Observable<Webhook> { return this.http.put<Webhook>(`${WEBHOOKS_V7}/hooks`, h); }
  update(h: Webhook): Observable<Webhook> { return this.http.post<Webhook>(`${WEBHOOKS_V7}/hooks`, h); }
  delete(name: string): Observable<unknown> { return this.http.delete(`${WEBHOOKS_V7}/hooks/${encodeURIComponent(name)}`); }

  /** Recent webhook execution log entries (most-recent-first is up to the caller). */
  listLogs(): Observable<WebhookLogOverview[]> { return this.http.get<WebhookLogOverview[]>(`${WEBHOOKS_V7}/list`); }
}
