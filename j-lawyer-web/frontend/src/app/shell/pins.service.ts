import { effect, inject, Injectable, signal } from '@angular/core';
import { AuthService } from '../core/auth/auth.service';

/** What can be pinned to the quick-access bar. */
export type PinKind = 'case' | 'contact';

/** A pinned shortcut shown in the pin bar; navigates to the case/contact detail on click. */
export interface PinnedItem {
  kind: PinKind;
  id: string;
  /** Primary label on the chip (case file number / contact display name). */
  label: string;
  /** Optional secondary text used as the chip's tooltip (e.g. the case name). */
  title?: string;
}

const STORE_PREFIX = 'jl.web.pins:';

/**
 * Manages the user's pinned cases/contacts shown in the header pin bar. State lives in a signal
 * and is persisted to localStorage, scoped per user so pins do not leak between accounts on a
 * shared browser. Frontend-only (no server sync) — this is a per-device convenience.
 */
@Injectable({ providedIn: 'root' })
export class PinsService {
  private readonly auth = inject(AuthService);

  readonly pins = signal<PinnedItem[]>([]);

  private currentUser: string | null = null;
  /** Guards the persist effect against writing back the freshly loaded set on a user switch. */
  private hydrating = false;

  constructor() {
    // (Re)load the pins whenever the logged-in user changes (also clears on logout).
    effect(() => {
      const user = this.auth.user()?.username ?? null;
      if (user !== this.currentUser) {
        this.currentUser = user;
        this.hydrating = true;
        this.pins.set(this.load(user));
        this.hydrating = false;
      }
    });
    // Persist on every change (except the initial hydration).
    effect(() => {
      const pins = this.pins();
      if (!this.hydrating) {
        this.save(this.currentUser, pins);
      }
    });
  }

  isPinned(kind: PinKind, id: string): boolean {
    return this.pins().some((p) => p.kind === kind && p.id === id);
  }

  /** Adds the item if not pinned, otherwise removes it. */
  toggle(item: PinnedItem): void {
    if (this.isPinned(item.kind, item.id)) {
      this.remove(item.kind, item.id);
    } else {
      this.pins.update((list) => [...list, item]);
    }
  }

  remove(kind: PinKind, id: string): void {
    this.pins.update((list) => list.filter((p) => !(p.kind === kind && p.id === id)));
  }

  private storageKey(user: string | null): string {
    return STORE_PREFIX + (user ?? 'anon');
  }

  private load(user: string | null): PinnedItem[] {
    try {
      const raw = localStorage.getItem(this.storageKey(user));
      const parsed = raw ? JSON.parse(raw) : [];
      return Array.isArray(parsed)
        ? parsed.filter((p): p is PinnedItem => p && (p.kind === 'case' || p.kind === 'contact') && !!p.id)
        : [];
    } catch {
      return [];
    }
  }

  private save(user: string | null, pins: PinnedItem[]): void {
    try {
      localStorage.setItem(this.storageKey(user), JSON.stringify(pins));
    } catch {
      // ignore quota/availability errors — pinning is a convenience, not critical state
    }
  }
}
