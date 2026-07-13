import { HttpClient } from '@angular/common/http';
import { inject, Injectable, Signal, signal } from '@angular/core';
import { API_ROOT } from '../core/api';

const OPTIONGROUPS_V7 = `${API_ROOT}/v7/configuration/optiongroups`;

interface OptionDto { id?: string; optionGroup?: string; value?: string; }

/**
 * Loads and caches the configurable value lists ("option groups", e.g. address.title,
 * address.legalform) served by GET /v7/configuration/optiongroups/{group}. Each group is fetched
 * at most once and exposed as a signal, so the contact editor can back its combo fields (via native
 * <datalist>) with the same lists the desktop client offers. Values are de-duplicated, emptied
 * entries dropped, and sorted locale-aware. App-wide singleton — the lists rarely change per session.
 */
@Injectable({ providedIn: 'root' })
export class OptionGroupsService {
  private readonly http = inject(HttpClient);
  private readonly cache = new Map<string, ReturnType<typeof signal<string[]>>>();

  /** Returns a signal with the group's values (empty until the first fetch resolves). */
  get(group: string): Signal<string[]> {
    let sig = this.cache.get(group);
    if (!sig) {
      sig = signal<string[]>([]);
      this.cache.set(group, sig);
      this.http.get<OptionDto[]>(`${OPTIONGROUPS_V7}/${encodeURIComponent(group)}`).subscribe({
        next: (rows) => sig!.set(normalize(rows)),
        error: () => sig!.set([]),
      });
    }
    return sig;
  }
}

function normalize(rows: OptionDto[] | null): string[] {
  const seen = new Set<string>();
  for (const r of rows ?? []) {
    const v = (r.value ?? '').trim();
    if (v) {
      seen.add(v);
    }
  }
  return [...seen].sort((a, b) => a.localeCompare(b));
}
