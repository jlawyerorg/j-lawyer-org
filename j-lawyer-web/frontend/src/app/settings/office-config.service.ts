import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { catchError, Observable, of } from 'rxjs';
import { API_ROOT } from '../core/api';

const OFFICE_V8 = `${API_ROOT}/v8/office`;

/** Document-server connection settings (RestfulOfficeSettingsV8). `secret` is write-only. */
export interface OfficeSettings {
  provider: string;
  baseUrl: string;
  wopiPublicUrl: string;
  secret: string;
  secretSet: boolean;
}

/** Everything needed to embed the editor for one document (RestfulEditorConfigV8). */
export interface EditorConfig {
  provider: string;
  urlsrc: string;
  wopiSrc: string;
  accessToken: string;
  accessTokenTtl: number;
  fileName: string;
}

/**
 * Access to in-browser Office editing (OpenSpec add-web-client, Decision 6): the admin connection
 * settings, the "is it enabled" flag that gates the document-menu action, and the per-document editor
 * config (which mints the WOPI access token server-side after an ACL check).
 */
@Injectable({ providedIn: 'root' })
export class OfficeConfigService {
  private readonly http = inject(HttpClient);

  /** Whether a document server is configured — drives the "edit in browser" action. Loaded once. */
  readonly enabled = signal(false);
  private statusLoaded = false;

  /** Fetches the enabled flag once (idempotent); safe to call from every document menu. */
  ensureStatus(): void {
    if (this.statusLoaded) {
      return;
    }
    this.statusLoaded = true;
    this.http.get<{ enabled: boolean }>(`${OFFICE_V8}/status`).pipe(
      catchError(() => of({ enabled: false })),
    ).subscribe((s) => this.enabled.set(!!s?.enabled));
  }

  getSettings(): Observable<OfficeSettings> {
    return this.http.get<OfficeSettings>(`${OFFICE_V8}/settings`);
  }

  saveSettings(s: OfficeSettings): Observable<OfficeSettings> {
    return this.http.put<OfficeSettings>(`${OFFICE_V8}/settings`, s);
  }

  editorConfig(documentId: string): Observable<EditorConfig> {
    return this.http.get<EditorConfig>(`${OFFICE_V8}/editor-config/${encodeURIComponent(documentId)}`);
  }
}
