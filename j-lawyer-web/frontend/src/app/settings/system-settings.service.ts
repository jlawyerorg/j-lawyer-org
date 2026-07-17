import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_ROOT } from '../core/api';

const CONFIG_V7 = `${API_ROOT}/v7/configuration`;

/** Scan / OCR settings (RestfulScanSettingsV7). */
export interface ScanSettings {
  serverDirectory: string;
  ocrCommand: string;
}

/** Stirling-PDF integration settings (RestfulStirlingSettingsV7). */
export interface StirlingSettings {
  endpoint: string;
}

/** beA integration settings (RestfulBeaSettingsV7). */
export interface BeaSettings {
  enabled: boolean;
  endpoint: string;
}

/** Data-backup configuration (RestfulBackupSettingsV7). Passwords are write-only. */
export interface BackupSettings {
  enabled: boolean;
  hour: number;
  monday: boolean;
  tuesday: boolean;
  wednesday: boolean;
  thursday: boolean;
  friday: boolean;
  saturday: boolean;
  sunday: boolean;
  dbHost: string;
  dbPort: number;
  dbName: string;
  dbUser: string;
  dbPassword: string;
  dbPasswordSet: boolean;
  encryptionPassword: string;
  encryptionPasswordSet: boolean;
  syncTarget: string;
  exportTarget: string;
}

/**
 * System-level settings (scan/OCR, data backup) over the v7 configuration endpoint. All require
 * `sysAdminRole` (enforced server-side). Backup passwords are write-only: they are never returned
 * (a `*Set` flag says whether one is stored) and only applied when a non-empty value is sent.
 */
@Injectable({ providedIn: 'root' })
export class SystemSettingsService {
  private readonly http = inject(HttpClient);

  getScan(): Observable<ScanSettings> { return this.http.get<ScanSettings>(`${CONFIG_V7}/scan-settings`); }
  saveScan(s: ScanSettings): Observable<ScanSettings> { return this.http.put<ScanSettings>(`${CONFIG_V7}/scan-settings`, s); }

  getBackup(): Observable<BackupSettings> { return this.http.get<BackupSettings>(`${CONFIG_V7}/backup-settings`); }
  saveBackup(s: BackupSettings): Observable<BackupSettings> { return this.http.put<BackupSettings>(`${CONFIG_V7}/backup-settings`, s); }

  /** Validates an external storage location (backup sync target) server-side. */
  validateStorageLocation(location: string): Observable<{ location: string; valid: boolean; message: string }> {
    return this.http.post<{ location: string; valid: boolean; message: string }>(`${CONFIG_V7}/validate-storage-location`, { location });
  }

  getStirling(): Observable<StirlingSettings> { return this.http.get<StirlingSettings>(`${CONFIG_V7}/stirling-settings`); }
  saveStirling(s: StirlingSettings): Observable<StirlingSettings> { return this.http.put<StirlingSettings>(`${CONFIG_V7}/stirling-settings`, s); }

  getBea(): Observable<BeaSettings> { return this.http.get<BeaSettings>(`${CONFIG_V7}/bea-settings`); }
  saveBea(s: BeaSettings): Observable<BeaSettings> { return this.http.put<BeaSettings>(`${CONFIG_V7}/bea-settings`, s); }
}
