import { HttpClient } from '@angular/common/http';
import { effect, inject, Injectable, signal } from '@angular/core';
import { catchError, of, tap } from 'rxjs';
import { API_ROOT } from '../core/api';
import { AuthService } from '../core/auth/auth.service';

const PROFILE_V8 = `${API_ROOT}/v8/profile`;

/** The per-user order of mailboxes and beA postboxes (RestfulInboxOrderV8). */
export interface InboxOrder {
  /** MailboxSetup ids, in the order chosen by the user. */
  mailboxOrder: string[];
  /** beA safe ids, in the order chosen by the user. */
  beaPostboxOrder: string[];
}

/**
 * The order in which the current user wants their mailboxes and beA postboxes presented.
 *
 * Backed by `/v8/profile/inbox-order`, which reads and writes the very same per-user settings
 * keys as the desktop client — so an order arranged here shows up in the desktop client and vice
 * versa. Shared by the e-mail and beA modules so the order is only fetched once per session.
 */
@Injectable({ providedIn: 'root' })
export class InboxOrderService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);

  private readonly _mailboxOrder = signal<string[]>([]);
  private readonly _beaPostboxOrder = signal<string[]>([]);
  private loaded = false;
  private loading = false;

  readonly mailboxOrder = this._mailboxOrder.asReadonly();
  readonly beaPostboxOrder = this._beaPostboxOrder.asReadonly();

  constructor() {
    // Drop the order when the signed-in user changes (incl. logout) — it is per user.
    let lastUser: string | null = null;
    effect(() => {
      const u = this.auth.user()?.username ?? null;
      if (u !== lastUser) {
        lastUser = u;
        this.loaded = false;
        this._mailboxOrder.set([]);
        this._beaPostboxOrder.set([]);
      }
    });
  }

  /** Loads the order once per session. No-op if already loaded (or loading) unless forced. */
  ensureLoaded(force = false): void {
    if ((this.loaded || this.loading) && !force) return;
    this.loading = true;
    this.http.get<InboxOrder>(`${PROFILE_V8}/inbox-order`).pipe(
      // a missing order must never keep the module from rendering
      catchError(() => of<InboxOrder>({ mailboxOrder: [], beaPostboxOrder: [] })),
    ).subscribe((order) => {
      this._mailboxOrder.set(order?.mailboxOrder ?? []);
      this._beaPostboxOrder.set(order?.beaPostboxOrder ?? []);
      this.loading = false;
      this.loaded = true;
    });
  }

  /** Stores a new mailbox order, applying it optimistically and rolling back on failure. */
  saveMailboxOrder(ids: string[]): void {
    const previous = this._mailboxOrder();
    this._mailboxOrder.set(ids);
    this.persist({ mailboxOrder: ids, beaPostboxOrder: this._beaPostboxOrder() },
      () => this._mailboxOrder.set(previous));
  }

  /** Stores a new beA postbox order, applying it optimistically and rolling back on failure. */
  saveBeaPostboxOrder(ids: string[]): void {
    const previous = this._beaPostboxOrder();
    this._beaPostboxOrder.set(ids);
    this.persist({ mailboxOrder: this._mailboxOrder(), beaPostboxOrder: ids },
      () => this._beaPostboxOrder.set(previous));
  }

  private persist(order: InboxOrder, rollback: () => void): void {
    this.http.put<InboxOrder>(`${PROFILE_V8}/inbox-order`, order).pipe(
      tap({ error: () => rollback() }),
      catchError(() => of(null)),
    ).subscribe();
  }
}
