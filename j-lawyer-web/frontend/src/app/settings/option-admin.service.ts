import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { API_ROOT } from '../core/api';

const CONFIG_V7 = `${API_ROOT}/v7/configuration`;

/** One editable value of an option group (its id is needed for rename/delete). */
export interface OptionValue {
  id: string;
  value: string;
}

interface OptionDto {
  id?: string;
  optionGroup?: string;
  value?: string;
}

/**
 * Reads and mutates a configuration option group ("Wertevorrat") over the v7 configuration REST
 * endpoints. Reading is open to any authenticated user; create/rename need `createOptionGroupRole`
 * and delete needs `deleteOptionGroupRole` (enforced server-side). Unlike the read-only
 * {@link ../contacts/option-groups.service OptionGroupsService} used by the contact editor, this
 * service returns each value's id and performs writes, so the settings editors can manage the lists.
 */
@Injectable({ providedIn: 'root' })
export class OptionAdminService {
  private readonly http = inject(HttpClient);

  /** All values of a group, sorted locale-aware by value. */
  list(group: string): Observable<OptionValue[]> {
    return this.http
      .get<OptionDto[]>(`${CONFIG_V7}/optiongroups/${encodeURIComponent(group)}`)
      .pipe(map((rows) => (rows ?? [])
        .map((r) => ({ id: r.id ?? '', value: (r.value ?? '').trim() }))
        .filter((o) => !!o.value)
        .sort((a, b) => a.value.localeCompare(b.value))));
  }

  /** Adds a value to the group (idempotent server-side); returns the created/existing value. */
  create(group: string, value: string): Observable<OptionValue> {
    return this.http
      .put<OptionDto>(`${CONFIG_V7}/options/create`, { optionGroup: group, value })
      .pipe(map((r) => ({ id: r.id ?? '', value: r.value ?? value })));
  }

  /** Renames an existing value (identified by id). */
  rename(group: string, id: string, value: string): Observable<OptionValue> {
    return this.http
      .put<OptionDto>(`${CONFIG_V7}/options/update`, { id, optionGroup: group, value })
      .pipe(map((r) => ({ id: r.id ?? id, value: r.value ?? value })));
  }

  /** Deletes a value (identified by id). Does not touch tags already attached to cases/etc. */
  delete(group: string, id: string): Observable<unknown> {
    return this.http.request('delete', `${CONFIG_V7}/options/delete`, {
      body: { id, optionGroup: group },
    });
  }
}
