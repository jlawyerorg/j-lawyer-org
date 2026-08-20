import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_ROOT } from '../core/api';

const ASSISTANT_V8 = `${API_ROOT}/v8/assistant`;

/** The request types an assistant prompt can target (AiCapability capabilities). */
export const ASSISTANT_REQUEST_TYPES = [
  'transcribe', 'translate', 'summarize', 'explain', 'chat', 'vision', 'generate', 'extract',
] as const;
export type AssistantRequestType = typeof ASSISTANT_REQUEST_TYPES[number];

/** An AI assistant ("Ingo") server connection (RestfulAssistantConfigV8). Password is write-only. */
export interface AssistantConfig {
  id?: string;
  name: string;
  url: string;
  userName: string;
  /** Write-only: only sent when changing the password; never returned. */
  password?: string;
  /** Read-only: whether a password is stored. */
  passwordSet?: boolean;
  connectionTimeout: number;
  readTimeout: number;
  /** Free-form assistant configuration (opaque JSON / text). */
  configuration: string;
}

/** A custom assistant prompt (RestfulAssistantPromptV8). */
export interface AssistantPrompt {
  id?: string;
  name: string;
  requestType: string;
  prompt: string;
  systemPrompt: string;
  modelRef: string;
  configuration: string;
}

/** An automatic transcription replacement (RestfulAssistantReplacementV8). */
export interface AssistantReplacement {
  id?: string;
  searchString: string;
  replaceWith: string;
  caseInsensitive: boolean;
}

/** A model offered by an assistant server (RestfulAssistantModelV8). */
export interface AssistantModel {
  name: string;
  provider?: string;
  description?: string;
  /** Runs locally on the assistant server (vs. an external provider). */
  local?: boolean;
  /** Using it deducts from the Ingo token budget ("Ingo-Tokens" vs. "Fremd-Tokens"). */
  deductTokens?: boolean;
  /** Agent-capable (supports tool calls). */
  supportsTools?: boolean;
  supportedRequestTypes: string[];
  /** Configurable parameters the model exposes (key + human description). */
  configurations?: { id: string; description: string }[];
}

/** One entry of an assistant server's request history (RestfulAiRequestLogV8). */
export interface AiRequestLogEntry {
  /** Epoch milliseconds. */
  timestamp: number;
  requestType: string;
  tokensUsed: number;
}

/** Live status of one assistant server (RestfulAssistantStatusV8). */
export interface AssistantStatus {
  configId: string;
  name: string;
  reachable: boolean;
  userLabel?: string;
  /** Available token budget reported by the server (-1 when unknown). */
  tokens?: number;
  tokensPerDay?: number;
  tokensPerMonth?: number;
  models: AssistantModel[];
  error?: string;
}

/**
 * Configuration of the AI assistant ("Ingo"), backed by `/v8/assistant` — the web equivalent of the
 * desktop "Assistent Ingo" menu: server connections, custom prompts and automatic replacements.
 * Server-connection CRUD requires `adminRole`; prompts/replacements create+update require
 * `loginRole` and delete requires `adminRole`; the status lookup requires `loginRole` (enforced
 * server-side). The interactive assistant runtime is out of scope.
 */
@Injectable({ providedIn: 'root' })
export class AssistantConfigService {
  private readonly http = inject(HttpClient);

  // servers
  listConfigs(): Observable<AssistantConfig[]> { return this.http.get<AssistantConfig[]>(`${ASSISTANT_V8}/configs`); }
  createConfig(c: AssistantConfig): Observable<AssistantConfig> { return this.http.put<AssistantConfig>(`${ASSISTANT_V8}/configs`, c); }
  updateConfig(c: AssistantConfig): Observable<AssistantConfig> { return this.http.post<AssistantConfig>(`${ASSISTANT_V8}/configs`, c); }
  deleteConfig(id: string): Observable<unknown> { return this.http.delete(`${ASSISTANT_V8}/configs/${encodeURIComponent(id)}`); }
  getStatus(): Observable<AssistantStatus[]> { return this.http.get<AssistantStatus[]>(`${ASSISTANT_V8}/status`); }
  getRequestLog(id: string): Observable<AiRequestLogEntry[]> { return this.http.get<AiRequestLogEntry[]>(`${ASSISTANT_V8}/configs/${encodeURIComponent(id)}/log`); }

  // prompts
  listPrompts(): Observable<AssistantPrompt[]> { return this.http.get<AssistantPrompt[]>(`${ASSISTANT_V8}/prompts`); }
  createPrompt(p: AssistantPrompt): Observable<AssistantPrompt> { return this.http.put<AssistantPrompt>(`${ASSISTANT_V8}/prompts`, p); }
  updatePrompt(p: AssistantPrompt): Observable<AssistantPrompt> { return this.http.post<AssistantPrompt>(`${ASSISTANT_V8}/prompts`, p); }
  deletePrompt(id: string): Observable<unknown> { return this.http.delete(`${ASSISTANT_V8}/prompts/${encodeURIComponent(id)}`); }

  // replacements
  listReplacements(): Observable<AssistantReplacement[]> { return this.http.get<AssistantReplacement[]>(`${ASSISTANT_V8}/replacements`); }
  createReplacement(r: AssistantReplacement): Observable<AssistantReplacement> { return this.http.put<AssistantReplacement>(`${ASSISTANT_V8}/replacements`, r); }
  updateReplacement(r: AssistantReplacement): Observable<AssistantReplacement> { return this.http.post<AssistantReplacement>(`${ASSISTANT_V8}/replacements`, r); }
  deleteReplacement(id: string): Observable<unknown> { return this.http.delete(`${ASSISTANT_V8}/replacements/${encodeURIComponent(id)}`); }
}
