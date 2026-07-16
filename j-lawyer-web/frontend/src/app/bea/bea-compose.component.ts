import {
  ChangeDetectionStrategy, Component, computed, inject, input, OnInit, output, signal,
} from '@angular/core';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { TranslocoModule, TranslocoService } from '@jsverse/transloco';
import { IconComponent } from '../shared/icon.component';
import { BeaService } from './bea.service';
import { BeaComposeMode, BeaIdentity, BeaListItem, BeaMessage, BeaSendRequest } from './bea.models';

/** A recipient chosen for the message: a Safe-ID plus a readable label. */
interface Recipient { safeId: string; label: string; }
/** An attachment to send: name + Base64 content + size. */
interface OutAttachment { name: string; content: string; size: number; }

/**
 * Modal composer for writing and sending a beA message (new / reply / forward). One recipient per
 * message (beA sends to a single Safe-ID); the recipient is picked from a SAFE directory search or,
 * for a reply, prefilled from the original sender. Bodies are plain text. Sending goes through
 * {@link BeaService.sendMessage}; a forward carries the original's document attachments.
 */
@Component({
  selector: 'jl-bea-compose',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoModule, IconComponent],
  template: `
    <div class="cmp-backdrop" (click)="tryClose()"></div>
    <div class="cmp-dialog" role="dialog" aria-modal="true">
      <header class="cmp-head">
        <h2>{{ titleKey() | transloco }}</h2>
        <button type="button" class="icon-btn" (click)="tryClose()" [attr.aria-label]="'beaCompose.close' | transloco">
          <jl-icon name="close" [size]="18" />
        </button>
      </header>

      <div class="cmp-body">
        <div class="cmp-row">
          <label>{{ 'beaCompose.from' | transloco }}</label>
          <span class="cmp-static"><jl-icon name="shield" [size]="13" /> {{ senderLabel() }}</span>
        </div>

        <!-- Recipient -->
        <div class="cmp-row cmp-recip">
          <label>{{ 'beaCompose.to' | transloco }}</label>
          @if (recipient(); as r) {
            <span class="cmp-chip">
              <jl-icon name="shield" [size]="12" />
              <span class="cmp-chip-label">{{ r.label }}</span>
              <button type="button" class="cmp-chip-x" [disabled]="sending()" (click)="clearRecipient()"
                      [attr.aria-label]="'beaCompose.changeRecipient' | transloco">✕</button>
            </span>
          } @else {
            <div class="cmp-search">
              <div class="cmp-search-fields">
                <input type="text" [value]="qName()" (input)="qName.set($any($event.target).value)"
                       (keydown.enter)="searchRecipients()" [placeholder]="'beaCompose.searchName' | transloco" />
                <input type="text" [value]="qCity()" (input)="qCity.set($any($event.target).value)"
                       (keydown.enter)="searchRecipients()" [placeholder]="'beaCompose.searchCity' | transloco" />
                <input type="text" [value]="qZip()" (input)="qZip.set($any($event.target).value)"
                       (keydown.enter)="searchRecipients()" [placeholder]="'beaCompose.searchZip' | transloco" />
                <button type="button" class="cmp-search-btn" [disabled]="searching() || !canSearch()"
                        (click)="searchRecipients()">
                  <jl-icon name="search" [size]="14" /> {{ 'beaCompose.search' | transloco }}
                </button>
              </div>
              @if (searching()) { <p class="muted">{{ 'beaCompose.searching' | transloco }}</p> }
              @else if (searched() && !results().length) { <p class="muted">{{ 'beaCompose.noResults' | transloco }}</p> }
              @if (results().length) {
                <ul class="cmp-results">
                  @for (id of results(); track id.safeId) {
                    <li (click)="pickRecipient(id)">
                      <span class="cmp-res-name">{{ id.displayName }}</span>
                      <span class="cmp-res-id">{{ id.safeId }}</span>
                    </li>
                  }
                </ul>
              }
            </div>
          }
        </div>

        <div class="cmp-row">
          <label>{{ 'beaCompose.subject' | transloco }}</label>
          <input type="text" [value]="subject()" (input)="subject.set($any($event.target).value)" autocomplete="off" />
        </div>

        <div class="cmp-row cmp-refs">
          <label>{{ 'beaCompose.references' | transloco }}</label>
          <input type="text" [value]="referenceNumber()" (input)="referenceNumber.set($any($event.target).value)"
                 [placeholder]="'beaCompose.refSender' | transloco" autocomplete="off" />
          <input type="text" [value]="referenceJustice()" (input)="referenceJustice.set($any($event.target).value)"
                 [placeholder]="'beaCompose.refCourt' | transloco" autocomplete="off" />
        </div>

        <div class="cmp-row">
          <label>{{ 'beaCompose.priority' | transloco }}</label>
          <select [value]="priorityCode()" (change)="priorityCode.set($any($event.target).value)">
            <option value="">—</option>
            @for (p of priorities(); track p.code) { <option [value]="p.code">{{ p.name }}</option> }
          </select>
        </div>

        <div class="cmp-row cmp-flags">
          <label class="cmp-check">
            <input type="checkbox" [checked]="eebRequested()" (change)="eebRequested.set($any($event.target).checked)" />
            {{ 'beaCompose.eeb' | transloco }}
          </label>
          <label class="cmp-check">
            <input type="checkbox" [checked]="confidential()" (change)="confidential.set($any($event.target).checked)" />
            {{ 'beaCompose.confidential' | transloco }}
          </label>
        </div>

        <textarea class="cmp-text" [value]="body()" (input)="body.set($any($event.target).value)"
                  [placeholder]="'beaCompose.bodyPlaceholder' | transloco"></textarea>

        @if (attachments().length || loadingAtt()) {
          <div class="cmp-atts">
            @if (loadingAtt()) { <span class="muted">{{ 'beaCompose.loadingAtt' | transloco }}</span> }
            @for (a of attachments(); track $index) {
              <span class="cmp-att">
                <jl-icon name="paperclip" [size]="13" />
                <span class="cmp-att-name">{{ a.name }}</span>
                <span class="cmp-att-size">{{ formatSize(a.size) }}</span>
                <button type="button" class="cmp-att-x" (click)="removeAttachment($index)" aria-label="remove">✕</button>
              </span>
            }
          </div>
        }

        @if (error()) { <p class="cmp-error">{{ error() }}</p> }
      </div>

      <footer class="cmp-foot">
        <label class="cmp-attach-btn" [class.disabled]="sending()">
          <jl-icon name="paperclip" [size]="15" />
          <span>{{ 'beaCompose.attach' | transloco }}</span>
          <input type="file" multiple hidden [disabled]="sending()" (change)="onFiles($event)" />
        </label>
        <span class="cmp-spacer"></span>
        <button type="button" class="btn-ghost" [disabled]="sending()" (click)="tryClose()">
          {{ 'beaCompose.cancel' | transloco }}
        </button>
        <button type="button" class="btn-primary" [disabled]="sending() || !canSend()" (click)="send()">
          <jl-icon name="send" [size]="15" />
          <span>{{ (sending() ? 'beaCompose.sending' : 'beaCompose.send') | transloco }}</span>
        </button>
      </footer>
    </div>
  `,
  styleUrl: './bea-compose.component.css',
})
export class BeaComposeComponent implements OnInit {
  private readonly api = inject(BeaService);
  private readonly transloco = inject(TranslocoService);

  readonly senderSafeId = input.required<string>();
  readonly senderLabel = input.required<string>();
  readonly mode = input.required<BeaComposeMode>();
  readonly seed = input<BeaMessage | null>(null);

  readonly sent = output<void>();
  readonly closed = output<void>();

  protected readonly recipient = signal<Recipient | null>(null);
  protected readonly subject = signal('');
  protected readonly body = signal('');
  protected readonly referenceNumber = signal('');
  protected readonly referenceJustice = signal('');
  protected readonly priorityCode = signal('');
  protected readonly eebRequested = signal(false);
  protected readonly confidential = signal(false);
  protected readonly attachments = signal<OutAttachment[]>([]);
  protected readonly priorities = signal<BeaListItem[]>([]);

  protected readonly sending = signal(false);
  protected readonly loadingAtt = signal(false);
  protected readonly error = signal<string | null>(null);

  // recipient search
  protected readonly qName = signal('');
  protected readonly qCity = signal('');
  protected readonly qZip = signal('');
  protected readonly results = signal<BeaIdentity[]>([]);
  protected readonly searching = signal(false);
  protected readonly searched = signal(false);

  protected readonly titleKey = computed(() => {
    switch (this.mode()) {
      case 'reply': return 'beaCompose.titleReply';
      case 'forward': return 'beaCompose.titleForward';
      default: return 'beaCompose.titleNew';
    }
  });

  protected readonly canSearch = computed(() =>
    !!(this.qName().trim() || this.qCity().trim() || this.qZip().trim()));
  protected readonly canSend = computed(() => !!this.recipient() && !!this.subject().trim());

  ngOnInit(): void {
    this.api.messagePriorities().subscribe((p) => this.priorities.set(p));

    const seed = this.seed();
    const mode = this.mode();
    if (!seed || mode === 'new') { return; }

    // Carry the case references over to the reply/forward.
    this.referenceNumber.set(seed.referenceNumber);
    this.referenceJustice.set(seed.referenceJustice);

    if (mode === 'reply') {
      if (seed.senderSafeId) {
        this.recipient.set({ safeId: seed.senderSafeId, label: seed.senderName || seed.senderSafeId });
      }
      this.subject.set(ensurePrefix('Re: ', seed.subject));
      this.body.set('\n\n' + quoteReply(seed, this.transloco));
    } else if (mode === 'forward') {
      this.subject.set(ensurePrefix('Fwd: ', seed.subject));
      this.body.set('\n\n' + quoteForward(seed, this.transloco));
      this.loadForwardedAttachments(seed);
    }
  }

  // ----- recipient search -----

  protected searchRecipients(): void {
    if (!this.canSearch() || this.searching()) { return; }
    this.searching.set(true);
    this.searched.set(true);
    this.api.searchIdentity({
      surName: this.qName().trim() || undefined,
      city: this.qCity().trim() || undefined,
      zipCode: this.qZip().trim() || undefined,
    }).subscribe({
      next: (rows) => { this.results.set(rows); this.searching.set(false); },
      error: () => { this.results.set([]); this.searching.set(false); },
    });
  }

  protected pickRecipient(id: BeaIdentity): void {
    this.recipient.set({ safeId: id.safeId, label: id.displayName || id.safeId });
    this.results.set([]);
    this.searched.set(false);
  }

  protected clearRecipient(): void {
    if (this.sending()) { return; }
    this.recipient.set(null);
  }

  // ----- attachments -----

  private loadForwardedAttachments(seed: BeaMessage): void {
    const atts = (seed.attachments ?? []).filter((a) => !a.technicalAttachment);
    if (!atts.length) { return; }
    this.loadingAtt.set(true);
    forkJoin(atts.map((a) => a.content
      ? of({ name: a.alias || a.name, content: a.content, size: a.size })
      : this.api.getAttachment(this.senderSafeId(), seed.id, a.name).pipe(
        catchError(() => of(null))))).subscribe((results) => {
      const fetched: OutAttachment[] = [];
      for (const r of results) {
        if (r && r.content) { fetched.push({ name: (r as { alias?: string; name: string }).alias || r.name, content: r.content, size: r.size }); }
      }
      this.attachments.update((list) => [...list, ...fetched]);
      this.loadingAtt.set(false);
    });
  }

  protected onFiles(ev: Event): void {
    const input = ev.target as HTMLInputElement;
    const files = Array.from(input.files ?? []);
    input.value = '';
    for (const f of files) {
      const reader = new FileReader();
      reader.onload = () => {
        const dataUrl = String(reader.result ?? '');
        const base64 = dataUrl.slice(dataUrl.indexOf(',') + 1);
        this.attachments.update((list) => [...list, { name: f.name, content: base64, size: f.size }]);
      };
      reader.readAsDataURL(f);
    }
  }

  protected removeAttachment(i: number): void {
    this.attachments.update((list) => list.filter((_, idx) => idx !== i));
  }

  // ----- send -----

  protected send(): void {
    const r = this.recipient();
    if (this.sending() || !r || !this.canSend()) { return; }
    this.error.set(null);
    this.sending.set(true);
    const req: BeaSendRequest = {
      recipientSafeId: r.safeId,
      subject: this.subject().trim(),
      body: this.body(),
      referenceNumber: this.referenceNumber().trim(),
      referenceJustice: this.referenceJustice().trim(),
      attachments: this.attachments().map((a) => ({ name: a.name, content: a.content })),
      legalAuthorityCode: '',
      priorityCode: this.priorityCode(),
      eebRequested: this.eebRequested(),
      confidential: this.confidential(),
    };
    this.api.sendMessage(this.senderSafeId(), req).subscribe({
      next: () => { this.sending.set(false); this.sent.emit(); },
      error: () => { this.sending.set(false); this.error.set(this.transloco.translate('beaCompose.error')); },
    });
  }

  protected tryClose(): void {
    if (this.sending()) { return; }
    this.closed.emit();
  }

  protected formatSize(bytes: number): string {
    if (!bytes) { return ''; }
    if (bytes < 1024) { return `${bytes} B`; }
    if (bytes < 1024 * 1024) { return `${Math.round(bytes / 1024)} KB`; }
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }
}

// ---------- module-scope helpers ----------

function ensurePrefix(prefix: string, subject: string): string {
  const s = subject ?? '';
  return s.toLowerCase().startsWith(prefix.toLowerCase()) ? s : prefix + s;
}

function quoteReply(msg: BeaMessage, t: TranslocoService): string {
  const header = t.translate('beaCompose.quoteHeader', { from: msg.senderName || msg.senderSafeId });
  const quoted = (msg.body ?? '').split('\n').map((l) => '> ' + l).join('\n');
  return `${header}\n${quoted}`;
}

function quoteForward(msg: BeaMessage, t: TranslocoService): string {
  const lines = [
    '---------- ' + t.translate('beaCompose.forwardHeader') + ' ----------',
    `${t.translate('bea.from')}: ${msg.senderName || msg.senderSafeId}`,
    `${t.translate('beaCompose.subject')}: ${msg.subject}`,
    '',
    msg.body ?? '',
  ];
  return lines.join('\n');
}
