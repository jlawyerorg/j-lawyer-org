import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_ROOT } from '../core/api';

const PROFILE_V8 = `${API_ROOT}/v8/profile`;

/** An id + display-name option (a security group). */
export interface ProfileGroup {
  id: string;
  name: string;
}

/** The editable per-user settings (RestfulProfileSettingsV8), shared with the desktop client. */
export interface ProfileSettings {
  notifyCalendarEntry: boolean;
  notifyCalendarEntryAuthored: boolean;
  notifyCalendarEntryReminder: boolean;
  notifyInstantMessageMention: boolean;
  notifyInstantMessageMentionDone: boolean;
  notifyInvoiceDue: boolean;
  notifyScheduledDailyAgenda: boolean;
  notifyScheduledWeeklyDigest: boolean;
  warnUnknownSenders: boolean;
  defaultOwnerGroup: string;
  defaultAllowedGroups: string[];
}

/** The self-service profile of the current user (RestfulProfileV8). */
export interface Profile {
  principalId: string;
  displayName: string;
  firstName: string;
  name: string;
  email: string;
  abbreviation: string;
  primaryGroupId: string;
  primaryGroupName: string;
  passwordComplexityRequired: boolean;
  settings: ProfileSettings;
  allGroups: ProfileGroup[];
  memberGroups: ProfileGroup[];
}

/**
 * Self-service profile of the currently authenticated user (the web equivalent of the desktop
 * {@code UserProfileDialog}). Backed by `/v8/profile`, which is scoped to the caller and needs only
 * `loginRole` — so every logged-in user can read and edit their own profile without `adminRole`.
 */
@Injectable({ providedIn: 'root' })
export class ProfileService {
  private readonly http = inject(HttpClient);

  getProfile(): Observable<Profile> {
    return this.http.get<Profile>(PROFILE_V8);
  }

  saveSettings(settings: ProfileSettings): Observable<ProfileSettings> {
    return this.http.put<ProfileSettings>(`${PROFILE_V8}/settings`, settings);
  }

  /** Sets a new password for the current user (no current password required, matching the desktop). */
  changePassword(newPassword: string): Observable<unknown> {
    return this.http.put(`${PROFILE_V8}/password`, { newPassword });
  }
}
