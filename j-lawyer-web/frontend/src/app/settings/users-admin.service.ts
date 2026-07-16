import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_ROOT } from '../core/api';

const SECURITY_V6 = `${API_ROOT}/v6/security`;

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
 * assignment and master-data editing of existing users are fully supported. Groups are read-only
 * (no create/update endpoint yet).
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
}
