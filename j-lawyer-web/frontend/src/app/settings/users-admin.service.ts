import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_ROOT } from '../core/api';

const SECURITY_V6 = `${API_ROOT}/v6/security`;
const DROPSCAN_V8 = `${API_ROOT}/v8/dropscan`;

/** A Dropscan scanbox as returned by discovery (RestfulDropscanScanboxV8). */
export interface DropscanScanbox {
  id: number;
  number: string;
}

/**
 * A user (RestfulUserV6). The full shape is round-tripped: the server's update applies the whole
 * body onto the existing record, so unedited fields must be sent back unchanged to avoid clearing
 * them. `principalId` is the login name and the identity key (immutable once created).
 */
export interface AdminUser {
  principalId?: string;
  displayName?: string;
  firstName?: string;
  name?: string;
  email?: string;
  lawyer?: boolean;
  company?: string;
  abbreviation?: string;
  phone?: string;
  mobile?: string;
  fax?: string;
  website?: string;
  street?: string;
  zipCode?: string;
  city?: string;
  externalId?: string;
  countryCode?: string;
  areaCode?: string;
  role?: string;
  adjunct?: string;
  countryCodeInvoicing?: string;
  bankName?: string;
  bankBic?: string;
  bankIban?: string;
  taxNr?: string;
  taxVatId?: string;
  autoLockDocuments?: boolean;
  /** Id of the user's primary group ('' = none). */
  primaryGroupId?: string;
  /** Nextcloud connection. The password is write-only (never returned; only applied when non-empty). */
  cloudHost?: string;
  cloudPort?: number;
  cloudSsl?: boolean;
  cloudUser?: string;
  cloudPassword?: string;
  cloudPath?: string;
  /** Read-only: whether a beA certificate is stored for the user. */
  beaCertificatePresent?: boolean;
  /** Dropscan API token — write-only (never returned; only applied when non-empty). */
  dropscanApiToken?: string;
  /** Read-only: whether a Dropscan API token is stored. */
  dropscanApiTokenSet?: boolean;
  /** Optional comma-separated Dropscan scanbox IDs the user is restricted to ('' = all). */
  dropscanScanboxes?: string;
}

/** A minimal id + display-name option (calendar / mailbox / invoice pool). */
export interface IdName {
  id: string;
  name: string;
}

/** A system role with its human description (RestfulRoleV6). */
export interface AdminRole {
  role: string;
  description?: string;
}

/** A security group (RestfulGroupV6). */
export interface AdminGroup {
  id?: string;
  name?: string;
  abbreviation?: string;
}

/**
 * User, role and group administration over the v6 security REST endpoints. Listing is open to any
 * authenticated user; create/update and role assignment require `adminRole` (enforced server-side).
 *
 * Limitation (server contract): creating a user assigns a dummy password and grants no login
 * permission — the web cannot set passwords or enable login; that stays a desktop/admin task. Role
 * assignment and master-data editing of existing users are fully supported. Groups support full CRUD
 * plus membership editing.
 */
@Injectable({ providedIn: 'root' })
export class UsersAdminService {
  private readonly http = inject(HttpClient);

  listUsers(): Observable<AdminUser[]> {
    return this.http.get<AdminUser[]>(`${SECURITY_V6}/users/list`);
  }

  createUser(user: AdminUser): Observable<AdminUser> {
    return this.http.put<AdminUser>(`${SECURITY_V6}/users/create`, user);
  }

  updateUser(user: AdminUser): Observable<AdminUser> {
    return this.http.post<AdminUser>(`${SECURITY_V6}/users/update`, user);
  }

  listRoles(): Observable<AdminRole[]> {
    return this.http.get<AdminRole[]>(`${SECURITY_V6}/roles`);
  }

  getUserRoles(principalId: string): Observable<AdminRole[]> {
    return this.http.get<AdminRole[]>(`${SECURITY_V6}/roles/${encodeURIComponent(principalId)}`);
  }

  /** Replaces the user's role set with the given roles. */
  setUserRoles(principalId: string, roles: AdminRole[]): Observable<unknown> {
    return this.http.post(`${SECURITY_V6}/roles/${encodeURIComponent(principalId)}`, roles);
  }

  listGroups(): Observable<AdminGroup[]> {
    return this.http.get<AdminGroup[]>(`${SECURITY_V6}/groups`);
  }

  createGroup(group: AdminGroup): Observable<AdminGroup> {
    return this.http.put<AdminGroup>(`${SECURITY_V6}/groups/create`, group);
  }

  updateGroup(group: AdminGroup): Observable<AdminGroup> {
    return this.http.post<AdminGroup>(`${SECURITY_V6}/groups/update`, group);
  }

  deleteGroup(groupId: string): Observable<unknown> {
    return this.http.delete(`${SECURITY_V6}/groups/${encodeURIComponent(groupId)}`);
  }

  /** Login names of the users that are members of the group. */
  groupMembers(groupId: string): Observable<string[]> {
    return this.http.get<string[]>(`${SECURITY_V6}/groups/${encodeURIComponent(groupId)}/members`);
  }

  /** The groups a user is a member of. */
  userGroups(principalId: string): Observable<AdminGroup[]> {
    return this.http.get<AdminGroup[]>(`${SECURITY_V6}/users/${encodeURIComponent(principalId)}/groups`);
  }

  addGroupMember(groupId: string, principalId: string): Observable<unknown> {
    return this.http.put(`${SECURITY_V6}/groups/${encodeURIComponent(groupId)}/members/${encodeURIComponent(principalId)}`, {});
  }

  removeGroupMember(groupId: string, principalId: string): Observable<unknown> {
    return this.http.delete(`${SECURITY_V6}/groups/${encodeURIComponent(groupId)}/members/${encodeURIComponent(principalId)}`);
  }

  /**
   * Tests a Dropscan API token and lists its scanboxes ("Test / Scanboxen ermitteln"). Pass the
   * freshly typed `apiToken`; when it is empty the server falls back to the stored token of the user
   * identified by `principalId` (the token is write-only, so an already-saved token is never sent
   * back to the browser). Requires `adminRole` (enforced server-side).
   */
  discoverScanboxes(apiToken?: string, principalId?: string): Observable<DropscanScanbox[]> {
    return this.http.post<DropscanScanbox[]>(`${DROPSCAN_V8}/discover-scanboxes`, { apiToken: apiToken ?? '', principalId: principalId ?? '' });
  }

  // --- selectable resources (calendars, mailboxes, invoice pools) ---

  listCalendars(): Observable<IdName[]> { return this.http.get<IdName[]>(`${SECURITY_V6}/calendars`); }
  listMailboxes(): Observable<IdName[]> { return this.http.get<IdName[]>(`${SECURITY_V6}/mailboxes`); }
  listInvoicePools(): Observable<IdName[]> { return this.http.get<IdName[]>(`${SECURITY_V6}/invoice-pools`); }

  userCalendars(pid: string): Observable<string[]> { return this.http.get<string[]>(`${SECURITY_V6}/users/${encodeURIComponent(pid)}/calendars`); }
  userMailboxes(pid: string): Observable<string[]> { return this.http.get<string[]>(`${SECURITY_V6}/users/${encodeURIComponent(pid)}/mailboxes`); }
  userInvoicePools(pid: string): Observable<string[]> { return this.http.get<string[]>(`${SECURITY_V6}/users/${encodeURIComponent(pid)}/invoice-pools`); }

  addUserCalendar(pid: string, id: string): Observable<unknown> { return this.http.put(`${SECURITY_V6}/users/${encodeURIComponent(pid)}/calendars/${encodeURIComponent(id)}`, {}); }
  removeUserCalendar(pid: string, id: string): Observable<unknown> { return this.http.delete(`${SECURITY_V6}/users/${encodeURIComponent(pid)}/calendars/${encodeURIComponent(id)}`); }
  addUserMailbox(pid: string, id: string): Observable<unknown> { return this.http.put(`${SECURITY_V6}/users/${encodeURIComponent(pid)}/mailboxes/${encodeURIComponent(id)}`, {}); }
  removeUserMailbox(pid: string, id: string): Observable<unknown> { return this.http.delete(`${SECURITY_V6}/users/${encodeURIComponent(pid)}/mailboxes/${encodeURIComponent(id)}`); }
  addUserInvoicePool(pid: string, id: string): Observable<unknown> { return this.http.put(`${SECURITY_V6}/users/${encodeURIComponent(pid)}/invoice-pools/${encodeURIComponent(id)}`, {}); }
  removeUserInvoicePool(pid: string, id: string): Observable<unknown> { return this.http.delete(`${SECURITY_V6}/users/${encodeURIComponent(pid)}/invoice-pools/${encodeURIComponent(id)}`); }

  /** Uploads (replaces) a user's beA certificate. */
  uploadBeaCertificate(pid: string, contentBase64: string, password: string): Observable<unknown> {
    return this.http.post(`${SECURITY_V6}/users/${encodeURIComponent(pid)}/bea-certificate`, { contentBase64, password });
  }

  /** Removes a user's beA certificate. */
  removeBeaCertificate(pid: string): Observable<unknown> {
    return this.http.delete(`${SECURITY_V6}/users/${encodeURIComponent(pid)}/bea-certificate`);
  }
}
